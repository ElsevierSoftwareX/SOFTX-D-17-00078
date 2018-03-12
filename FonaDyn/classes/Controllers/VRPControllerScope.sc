// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPControllerScope {
	var mData;
	var mTarget;
	var mSynthPrepare; // Prepare step for the BusListeners - generating timestamps/audio rate gate etc

	var mBufferMovingEGG;
	var mSynthMovingEGG;
	var mConditionMovingEGG;
	var mBufferRequesterMovingEGG;
	var mRequestMovingEGG;

	var mBusListenerGoodCycles; // Per cycle bus listener using only good cycles
	var mBusListenerCycleSeparation; // Per cycle bus listener using the raw cycles (unfiltered gate from the cycle separation)

	// States
	var mSampEnPointStart; // The freq/amp point where the SampEn measurement went from below to above the limit
	var mSampEnPrev; // The last read SampEn value
	var mRunning;

	*new { | target, data |
		^super.new.init(target, data);
	}

	// Init given the target and output data structure
	// This function is called once only!
	init { | target, data |
		mTarget = target;
		mData = data;
		mBusListenerGoodCycles = BusListener();
		mBusListenerCycleSeparation = BusListener();
	}

	// Tell the busManager what buses it requires.
	// This function is called before prepare.
	requires { | busManager |
		var d = mData;
		var s = d.settings;
		var n = s.cluster.nHarmonics - 1;

		busManager
		.requireAudio(\Timestamp)
		.requireAudio(\SampEn)
		.requireAudio(\GateCycle)
		.requireAudio(\GateFilteredDFT)
		.requireAudio(\ClusterNumber)
		.requireControl(\Frequency)
		.requireControl(\Amplitude)
		.requireControl(\Clarity)
		.requireControl(\Crest)
		.requireAudio(\DelayedFrequency)
		.requireAudio(\DelayedAmplitude)
		.requireAudio(\DelayedClarity);
	}

	// Prepare to start - init the SynthDefs and allocate Buffers.
	// This function is always called before start.
	prepare { | libname, server, busManager, clock |
		var bm = busManager;
		var d = mData;
		var sd = d.scope;
		var s = d.settings;
		var ss = s.scope;
		var vrpd = d.vrp;

		// Generate a base path for the output
		var dir = s.general.output_directory;
		var timestamp = d.general.timestamp;
		var base_path = dir +/+ timestamp;

		// Create 2 bus assignment tools for both rates
		var cycle_tool = BusListener.newBusAssignmentTool;
		var cycle_separation_tool = BusListener.newBusAssignmentTool;

		// Init the start/prev for sampen arrows
		mSampEnPointStart = nil;
		mSampEnPrev = 0;

		// Compile the synthdefs
		VRPSDScope.compile(libname, ss.movingEGGCount, ss.minSamples, ss.maxSamples, ss.normalize);

		// Create the directory if necessary.
		if (File.type(dir) != \directory, { File.mkdir(dir); });

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Add the handlers for the cycle rate bus listener
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		cycle_tool.assign(\SampEnScope,
			[
				bm.audio(\Timestamp),
				bm.audio(\SampEn)
			]
		);

		cycle_tool.assign(\VRPData,
			[
				bm.audio(\DelayedFrequency),
				bm.audio(\DelayedAmplitude),
				bm.audio(\DelayedClarity),
				bm.audio(\SampEn),
				bm.audio(\ClusterNumber)
			]
		);

		cycle_tool.assign(\SampEnArrows,
			[
				bm.audio(\DelayedFrequency),
				bm.audio(\DelayedAmplitude),
				bm.audio(\SampEn)
			]
		);

		mBusListenerGoodCycles

		// Add the handler to grab scope data for the SampEn scope
		.addHandler(\scopeSampEn, \scope,
			cycle_tool.indices(\SampEnScope),
			ss.duration,
			{
				| data |
				sd.sampen = data;
			}
		)

		// Add the handler to grab the VRPData
		.addHandler(\vrp, \custom, {
			| data |
			data.dataAsFrames do: { | frame |
				var freq;
				var amp;
				var clarity;
				var entropy;
				var clusterNumber;

				var idx_midi;
				var idx_spl;

				#freq, amp, clarity, entropy, clusterNumber = frame[ cycle_tool.indices(\VRPData) ];

				// There is a grid that is VRPDataVRP.vrpWidth x VRPDataVRP.vrpHeight in size,
				// that we need to map these values to
				idx_midi = VRPDataVRP.frequencyToIndex( freq );
				idx_spl = VRPDataVRP.amplitudeToIndex( amp );

				if ( clarity > VRPDataVRP.clarityThreshold, {
					var cluster_matrix = vrpd.clusters[clusterNumber];
					var best_count = 0;
					var best_cluster_idx = 0;
					var percent = 0;
					var cell_count;

					// Update the density of the cluster
					cluster_matrix.put(idx_spl, idx_midi, (cluster_matrix.at(idx_spl, idx_midi) ? 0) + 1); // nil = 0

					// Update the mostCommonCluster matrix
					s.cluster.nClusters.asInteger do: { | idx |
						var cluster_matrix = vrpd.clusters[idx];
						var count = cluster_matrix.at(idx_spl, idx_midi) ? 0;
						if ( count > best_count, {
							best_count = count;
							best_cluster_idx = idx;
						});
					};

					cell_count = vrpd.density.at(idx_spl, idx_midi) ? 1 ;
					percent = best_count * 100 * (cell_count).reciprocal ;
					vrpd.mostCommonCluster.put(idx_spl, idx_midi, [best_cluster_idx, percent]);

					// Update the entropy matrix
					if (entropy > (vrpd.entropy.at(idx_spl, idx_midi) ? 0 ) ,
					   { vrpd.entropy.put(idx_spl, idx_midi, entropy) }
					);

				}); // End if clarity > threshold
				vrpd.currentCluster = clusterNumber;
			}; // End do
		})

		// Add the handler to extract the SampEn arrows from the SampEn measurement
		.addHandler(\SampEnArrows, \custom, {
			| data |
			data.dataAsFrames do: { | frame |
				var freq;
				var amp;
				var sampen;
				var limit = s.sampen.limit;
				var abovelimit;
				var to;
				#freq, amp, sampen = frame[ cycle_tool.indices(\SampEnArrows) ];

				abovelimit = sampen > limit;
				to = freq@amp;
				if (abovelimit and: (mSampEnPrev > limit), {
					// Two measurements above the limit so generate an arrow
					var from = mSampEnPointStart;
					var v = (to - from) / if (from == to, 1, {from dist: to}); // Normalize
					var rot = atan2(v.y, v.x); // Get the angle in radians

					// Convert from into indices into the matrix
					// There is a grid that is VRPDataSampEn.vrpWidth x VRPDataSampEn.vrpHeight in size,
					// that we need to map these values to
					var idx_midi = VRPDataVRP.frequencyToIndex( from.x, VRPDataSampEn.vrpWidth );
					var idx_spl = VRPDataVRP.amplitudeToIndex( from.y, VRPDataSampEn.vrpHeight );

					d.sampen.sampenPoints.put(idx_spl, idx_midi, rot);
				});

				if ( abovelimit, {
					mSampEnPointStart = to;
				});

				mSampEnPrev = sampen;


			};
		});

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		cycle_separation_tool
		.assign(\vrp,
			[
				bm.control(\Frequency),
				bm.control(\Amplitude),
				bm.control(\Clarity),
				bm.control(\Crest),
			]
		);

		mBusListenerCycleSeparation

		.addHandler(\vrp, \custom, {
			| data |
			var freq, amp, clarity, crest;
			var indices = cycle_separation_tool.indices(\vrp);

			data.dataAsFrames do: { | frame |
				var idx_midi;
				var idx_spl;

				#freq, amp, clarity, crest = frame[ indices ];

				// There is a grid that is VRPDataVRP.vrpWidth x VRPDataVRP.vrpHeight in size,
				// to which we need to map these values
				idx_midi = VRPDataVRP.frequencyToIndex( freq );
				idx_spl = VRPDataVRP.amplitudeToIndex( amp );

				// Set the clarity measurement
				vrpd.clarity.put(idx_spl, idx_midi,
					max( clarity, vrpd.clarity.at(idx_spl, idx_midi) ? 0 )
				);

				if ( clarity > VRPDataVRP.clarityThreshold, {
					var density = vrpd.density.at(idx_spl, idx_midi) ? 0 ; // nil = 0
					var meanCrest = vrpd.crest.at(idx_spl, idx_midi) ? 0 ; // nil = 0
					var nm = density*meanCrest + crest;
					density = density + 1;
					meanCrest = nm/density;

					// Set the mean crest factor and density
					// vrpd.crest.put(idx_spl, idx_midi, crest);
					vrpd.crest.put(idx_spl, idx_midi, meanCrest);
					vrpd.density.put(idx_spl, idx_midi, density);

					// Update maxDensity
					if ( (vrpd.maxDensity ? 0) < density, {
						vrpd.maxDensity = density;
					});
				});
			};

			// Update the current Frequency and Amplitude
			vrpd.currentFrequency = freq;
			vrpd.currentAmplitude = amp;
			vrpd.currentClarity = clarity;
		});

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Prepare the bus listeners
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		mBusListenerGoodCycles.prepare(libname, server,
			cycle_tool.buses,
			bm.audio(\GateFilteredDFT),
			clock
		);

		mBusListenerCycleSeparation.prepare(libname, server,
			cycle_separation_tool.buses,
			bm.audio(\GateCycle),
			clock
		);

		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		// Prepare for the Moving EGG
		//////////////////////////////////////////////////////////////////////////////////////////////////////////////////
		mBufferMovingEGG = MovingEGG.allocBuffer(server, ss.movingEGGCount, ss.movingEGGSamples);
		mBufferRequesterMovingEGG = BufferRequester(mBufferMovingEGG);
		mConditionMovingEGG = Condition();

		value {
			var bufsize = mBufferMovingEGG.numFrames * mBufferMovingEGG.numChannels;
			var reqs = mBufferRequesterMovingEGG.asRequests(0, bufsize);
			if ( reqs.size != 1, { Error("Assertion failed!").postln; }); // Assert that we read the entire buffer
			mRequestMovingEGG = reqs[0];
		};
	}

	// Start - Create the synths, and initiate fetching of data at the regular
	// interval given by the clock parameter.
	// This function is always called between prepare and stop.
	start { | server, busManager, clock |
		var bm = busManager;

		// Start the preparation synth
		mSynthPrepare = Synth(*VRPSDScope.prepare(
			bm.audio(\Timestamp),
			mTarget,
			\addToTail)
		);

		// Start the bus listener
		mBusListenerGoodCycles.start(server, mTarget, clock);
		mBusListenerCycleSeparation.start(server, mTarget, clock);

		// Init movingEGG
		mSynthMovingEGG = Synth(*VRPSDScope.movingEGG(
			bm.audio(\GateCycle),
			bm.audio(\ConditionedEGG),
			mBufferMovingEGG,
			mTarget,
			\addToTail)
		);

		// Start fetching the buffer for the movingEGG.
		mRunning = true;
		clock.sched(1, {
			if ( mRunning, {
				// Request the contents of the buffer - no need to resend lost,
				// since we're only interested in the most fresh data
				mBufferRequesterMovingEGG.sendAll(mRequestMovingEGG.asArray);
			}, {
				// Resend the final request if it got lost
				if ( mConditionMovingEGG.test.not and: (mRequestMovingEGG.duration > mBufferRequesterMovingEGG.lostDuration), {
					mBufferRequesterMovingEGG.sendAll(mRequestMovingEGG.asArray);
				});
			});

			if ( mRequestMovingEGG.data.notNil and: mConditionMovingEGG.test.not, {
				// Dispatch request data
				mData.scope.movingEGGData = mRequestMovingEGG.data;
				mRequestMovingEGG.data = nil;

				if ( mRunning.not, {
					// Signal that the final request has been processed!
					mConditionMovingEGG.test = true; // Don't block on waits
					mConditionMovingEGG.signal; // Signal that stop can continue and free the buffer
				});
			});

			// Continue if:
			if (mRunning or: mConditionMovingEGG.test.not, 3, nil)
		});
	}

	// Free the synths and buffers after finishing fetching data.
	// The synths are guaranteed to be paused at this point - so buffers should be stable.
	stop {
		// Free the synths
		mSynthPrepare.free;
		mSynthMovingEGG.free;

		// Stop the bus listeners
		mBusListenerGoodCycles.stop;
		mBusListenerCycleSeparation.stop;

		// Signal that we want to wait on the condition
		mConditionMovingEGG.test = false;

		// Copy the request to ensure that we will make one final read after stop
		mRequestMovingEGG = mRequestMovingEGG.deepCopy;
		mRequestMovingEGG.data = nil;

		mBufferRequesterMovingEGG.sendAll(mRequestMovingEGG.asArray);

		// Tell the scheduled reads to stop
		mRunning = false;
	}

	sync {
		mBusListenerGoodCycles.sync;
		mBusListenerCycleSeparation.sync;

		mConditionMovingEGG.wait;
		// Ok to free the buffers now
		mBufferMovingEGG.free;
	}
}