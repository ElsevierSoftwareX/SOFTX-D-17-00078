// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
/**
 * Manages the buffers, synths and settings concerning I/O.
 */

VRPControllerIO {
	var mData;
	var mTargetInput;
	var mTargetOutput;
	var mGroupMain;
	var mClock;
	var mCondition;

	// OSCFuncs
	var mOSCFuncEOF;
	var mOSCFuncClip;

	// Buffers
	var mBufferFile;
	var mBufferAudio;
	var mBufferCycleDetectionLog;
	var mBufferWritePoints;
	var mBufferWriteSampEn;
	var mBufferWriteFrequencyAmplitude;
	var mBufferWriteLog;
	var mBufferWriteGates;
	var mBufferRecordExtraChannels;

	// Synths
	var mSynthInput;
	var mSynthEcho;
	var mSynthWriteAudio;
	var mSynthWriteCycleDetection;
	var mSynthWritePoints;
	var mSynthWriteSampEn;
	var mSynthWriteFrequencyAmplitude;
	var mSynthWriteLog;
	var mSynthWriteGates;
	var mSynthRecordExtraChannels;

	// Constants
	classvar triggerIDEOF = 117;
	classvar triggerIDClip = 118;

	// Static Input/Output Buses
	classvar audioInputBusIndexVoice = 0; // First audio input channel
	classvar audioInputBusIndexEGG = 1; // Second audio input channel
	classvar audioOutputBusIndexSpeaker = 0; // First audio output channel

	// Settings
	var mAudioInputBusIndecesExtra;  // array of hardware input indeces for extra channels

	// If this duration is too short, the file might be closed before all writes have completed - resulting in a useless file, or potentially an erronous file.
	classvar diskWriteWaitTime = 2.0; // The duration in seconds to wait for all disk writes to finish.

	*new { | targetInput, targetOutput, mainGroup, data |
		^super.new.init(targetInput, targetOutput, mainGroup, data);
	}

	// Init given the target and output data structure
	// This function is called once only!
	init { | targetInput, targetOutput, mainGroup, data |
		mTargetInput = targetInput;
		mTargetOutput = targetOutput;
		mGroupMain = mainGroup;
		mData = data; // General data
		mCondition = Condition(true); // Syncing has no effect until we free the buffers.
	}

	// Tell the busManager what buses it requires.
	// This function is called before prepare.
	requires { | busManager |
		var d = mData;
		var s = d.settings;
		var n = s.cluster.nHarmonics - 1;
		var m = s.csdft.nHarmonics;

		busManager
		.requireAudio(\EchoVoice)
		.requireAudio(\EchoEGG)
		.requireAudio(\ConditionedMicrophone)
		.requireAudio(\ConditionedEGG)
		.requireAudio(\GateCycle)
		.requireAudio(\GateDelayedCycle)
		.requireAudio(\GateFilteredDFT)
		.requireAudio(\DeltaAmplitudeFirst, n)
		.requireAudio(\DeltaPhaseFirst, n)
		.requireAudio(\SampEn)
		.requireAudio(\ClusterNumber)
		.requireAudio(\Timestamp)
		.requireAudio(\DelayedFrequency)
		.requireAudio(\DelayedAmplitude)
		.requireAudio(\DelayedClarity)
		.requireAudio(\DelayedCrest)
		.requireAudio(\AmplitudeFirst, m)
		.requireAudio(\PhaseFirst, m);
	}

	// Prepare to start - init the SynthDefs and allocate Buffers.
	// This function is always called before start.
	prepare { | libname, server, busManager, clock |
		var d = mData;
		var iod = d.io;
		var s = d.settings;
		var ios = s.io;
		var cs = s.cluster;
		var cds = s.csdft;

		var timestamp = d.general.timestamp;
		var dir = s.general.output_directory;
		var base_path = dir +/+ timestamp;

		var oldname = (ios.filePathInput ? "").basename;
		var ix = (oldname.findBackwards("_Voice_EGG.wav", true) ? -1 ) - 1;

		if ((ios.keepInputName and: (ix >= 0) and: (ios.enabledWriteAudio.not)),
			{ base_path = dir +/+ (oldname[0..ix])}
		);

		if (File.type(dir) != \directory, { File.mkdir(dir); }); // Create the directory if necessary.

		VRPSDIO.compile(libname, triggerIDEOF, triggerIDClip, mGroupMain.nodeID,
			cds.nHarmonics, ios.enabledCalibrationTone, ios.writeLogFrameRate, ios.arrayRecordExtraInputs);

		// Allocate buffers if necessary
		if (ios.inputType == VRPSettingsIO.inputTypeFile(), {
			mBufferFile = Buffer.cueSoundFile(server, ios.filePathInput, 0, 2, 262144);
		});

		if (ios.enabledWriteAudio, {
			iod.filePathAudio = base_path ++ "_Voice_EGG.wav";
			File.open(iod.filePathAudio, "w").close; // Forcefully create the file
			mBufferAudio = Buffer.alloc(server, 262144, 2);
			mBufferAudio.write(iod.filePathAudio, "wav", "int16", 0, 0, true);
		});

		if (ios.enabledWriteCycleDetection, {
			iod.filePathCycleDetectionLog = base_path ++ "_CycleDetection.wav";
			File.open(iod.filePathCycleDetectionLog, "w").close; // Forcefully create the file
			mBufferCycleDetectionLog = Buffer.alloc(server, 262144, 2);
			mBufferCycleDetectionLog.write(iod.filePathCycleDetectionLog, "wav", "int16", 0, 0, true);
		});

		if (ios.enabledWriteOutputPoints, {
			iod.filePathOutputPoints = base_path ++ "_Points.aiff";
			File.open(iod.filePathOutputPoints, "w").close; // Forcefully create the file
			mBufferWritePoints = Buffer.alloc(server, cs.nDimensions * 1000, cs.nDimensions);
			mBufferWritePoints.write(iod.filePathOutputPoints, "aiff", "float", 0, 0, true);
		});

		if (ios.enabledWriteSampEn, {
			iod.filePathSampEn = base_path ++ "_SampEn.aiff";
			File.open(iod.filePathSampEn, "w").close; // Forcefully create the file
			mBufferWriteSampEn = Buffer.alloc(server, server.sampleRate, 1);
			mBufferWriteSampEn.write(iod.filePathSampEn, "aiff", "float", 0, 0, true);
		});

		if (ios.enabledWriteOutputPoints or: ios.enabledWriteSampEn, {
			iod.filePathFrequencyAmplitude = base_path ++ "_FreqAmp.aiff";
			File.open(iod.filePathFrequencyAmplitude, "w").close; // Forcefully create the file
			mBufferWriteFrequencyAmplitude = Buffer.alloc(server, server.sampleRate, 2);
			mBufferWriteFrequencyAmplitude.write(iod.filePathFrequencyAmplitude, "aiff", "float", 0, 0, true);
		});

		if ( ios.enabledWriteLog, {
			var channels = 7 + (2 * cds.nHarmonics);
			iod.filePathLog = base_path ++ "_Log.aiff";
			File.open(iod.filePathLog, "w").close; // Forcefully create the file
			mBufferWriteLog = Buffer.alloc(server, channels * 400, channels);
			mBufferWriteLog.write(iod.filePathLog, "aiff", "float", 0, 0, true);
		});

		if ( ios.enabledWriteGates and:
			((ios.writeLogFrameRate == 0) or: ios.enabledWriteOutputPoints or: ios.enabledWriteSampEn),
			{
				iod.filePathGates = base_path ++ "_Gates.wav";
				File.open(iod.filePathGates, "w").close; // Forcefully create the file
				mBufferWriteGates = Buffer.alloc(server, 262144, 5);
				mBufferWriteGates.write(iod.filePathGates, "wav", "int16", 0, 0, true);
		});

		if (ios.enabledWriteAudio and: ios.enabledRecordExtraChannels, {
			iod.filePathExtraChannels = base_path ++ "_Extra.wav";
			File.open(iod.filePathExtraChannels, "w").close; // Forcefully create the file
			mBufferRecordExtraChannels = Buffer.alloc(server, server.sampleRate, ios.arrayRecordExtraInputs.size);
			mBufferRecordExtraChannels.write(iod.filePathExtraChannels, "wav", "int16", 0, 0, true);
		});


	}

	// Start - Create the synths, and initiate fetching of data at the regular
	// interval given by the clock parameter.
	// This function is always called between prepare and stop.
	start { | server, busManager, clock |
		var bm = busManager;
		var d = mData;
		var iod = d.io;
		var s = d.settings;
		var ios = s.io;
		mClock = clock;

		// Instantiate the input synth
		switch (ios.inputType,
			VRPSettingsIO.inputTypeFile(), {
				mSynthInput = Synth(*VRPSDIO.diskInput(
					mBufferFile,
					bm.audio(\EchoVoice),
					bm.audio(\EchoEGG),
					bm.audio(\ConditionedMicrophone),
					bm.audio(\ConditionedEGG),
					mTargetInput,
					\addToTail)
				);

				// Setup the OSCFunc
				mOSCFuncEOF =
				OSCFunc({ iod.eof = true; },
					'/tr',
					server.addr,
					nil,
					[mSynthInput.nodeID, triggerIDEOF]
				);
			},

			VRPSettingsIO.inputTypeRecord(), {
				mSynthInput = Synth(*VRPSDIO.liveInput(
					audioInputBusIndexVoice,
					audioInputBusIndexEGG,
					bm.audio(\EchoVoice),
					bm.audio(\EchoEGG),
					bm.audio(\ConditionedMicrophone),
					bm.audio(\ConditionedEGG),
					mTargetInput,
					\addToTail)
				);

				// Setup the OSCFunc
				mOSCFuncClip =
				OSCFunc({ iod.clip = true; },
					'/tr',
					server.addr,
					nil,
					[mSynthInput.nodeID, triggerIDClip]
				);
			}
		);

		if (ios.enabledEcho, {
			mSynthEcho = Synth(*VRPSDIO.echoMicrophone(
				bm.audio(\EchoVoice),
				audioOutputBusIndexSpeaker,
				mTargetOutput,
				\addToTail)
			);
		});

		if (ios.enabledWriteAudio, {
			switch (ios.inputType,
				VRPSettingsIO.inputTypeRecord(), {
					mSynthWriteAudio = Synth(*VRPSDIO.writeAudio(
						bm.audio(\EchoVoice),
						bm.audio(\EchoEGG),
						mBufferAudio,
						mTargetOutput,
						\addToTail)
				)},
				VRPSettingsIO.inputTypeFile(), {
					mSynthWriteAudio = Synth(*VRPSDIO.writeAudio(
						bm.audio(\ConditionedMicrophone),
						bm.audio(\ConditionedEGG),
						mBufferAudio,
						mTargetOutput,
						\addToTail)
			)});
		});

		if (ios.enabledWriteCycleDetection, {
			mSynthWriteCycleDetection = Synth(*VRPSDIO.writeCycleDetectionLog(
				bm.audio(\ConditionedEGG),
				bm.audio(\GateCycle),
				mBufferCycleDetectionLog,
				mTargetOutput,
				\addToTail)
			);
		});

		if (ios.enabledWriteOutputPoints, {
			mSynthWritePoints = Synth(*VRPSDIO.writePoints(
				bm.audio(\GateFilteredDFT),
				bm.audio(\DeltaAmplitudeFirst),
				bm.audio(\DeltaPhaseFirst),
				mBufferWritePoints,
				mTargetOutput,
				\addToTail)
			);
		});

		if (ios.enabledWriteSampEn, {
			mSynthWriteSampEn = Synth(*VRPSDIO.writeSampEn(
				bm.audio(\GateFilteredDFT),
				bm.audio(\SampEn),
				mBufferWriteSampEn,
				mTargetOutput,
				\addToTail)
			);
		});

		if (ios.enabledWriteOutputPoints or: ios.enabledWriteSampEn, {
			mSynthWriteFrequencyAmplitude = Synth(*VRPSDIO.writeFrequencyAmplitude(
				bm.audio(\GateFilteredDFT),
				bm.audio(\DelayedFrequency),
				bm.audio(\DelayedAmplitude),
				mBufferWriteFrequencyAmplitude,
				mTargetOutput,
				\addToTail)
			);
		});

		if ( ios.enabledWriteLog, {
			mSynthWriteLog = Synth(*VRPSDIO.writeLog(
				bm.audio(\GateFilteredDFT),
				bm.audio(\Timestamp),
				bm.audio(\DelayedFrequency),
				bm.audio(\DelayedAmplitude),
				bm.audio(\DelayedClarity),
				bm.audio(\DelayedCrest),
				bm.audio(\ClusterNumber),
				bm.audio(\SampEn),
				bm.audio(\AmplitudeFirst),
				bm.audio(\PhaseFirst),
				mBufferWriteLog,
				mTargetOutput,
				\addToTail)
			);
		});

		if ( ios.enabledWriteGates and:
			(ios.enabledWriteLog or: ios.enabledWriteOutputPoints or: ios.enabledWriteSampEn), {
			mSynthWriteGates = Synth(*VRPSDIO.writeGates(
				bm.audio(\EchoEGG),
				bm.audio(\ConditionedEGG),
				bm.audio(\GateCycle),
				bm.audio(\GateDelayedCycle),
				bm.audio(\GateFilteredDFT),
				mBufferWriteGates,
				mTargetOutput,
				\addToTail)
			);
		});

			if (ios.enabledWriteAudio and: ios.enabledRecordExtraChannels, {
			mSynthRecordExtraChannels = Synth(*VRPSDIO.recordExtraChannels(
				mBufferRecordExtraChannels,
				mTargetOutput,
				\addToTail)
			);
		});

	}

	// Free the synths and buffers after finishing fetching data.
	// The synths are guaranteed to be paused at this point - so buffers should be stable.
	stop {
		// Free synths
		[
			mSynthInput,
			mSynthEcho,
			mSynthWriteAudio,
			mSynthWriteCycleDetection,
			mSynthWritePoints,
			mSynthWriteSampEn,
			mSynthWriteFrequencyAmplitude,
			mSynthWriteLog,
			mSynthWriteGates,
			mSynthRecordExtraChannels
		]
		do: { | synth | if (synth.notNil, { synth.free; }); };

		// ...and buffers
		mCondition.test = false; // Enable waiting on the condition variable.

		mClock.sched( mClock.tempo * diskWriteWaitTime, {
			[
				mBufferFile,
				mBufferAudio,
				mBufferCycleDetectionLog,
				mBufferWritePoints,
				mBufferWriteSampEn,
				mBufferWriteFrequencyAmplitude,
				mBufferWriteLog,
				mBufferWriteGates,
				mBufferRecordExtraChannels
			]
			do: { | buf |
				if (buf.notNil, {
					buf.close;
					buf.free;
				});
			};

			mCondition.test = true; // Ensure that no-one get stuck on wait after we exit here
			mCondition.signal; // Signal anyone who's stuck waiting
		});

		// ...and OSCFuncs
		if (mOSCFuncClip.notNil, {mOSCFuncClip.free});
		if (mOSCFuncEOF.notNil, {mOSCFuncEOF.free});
	}

	sync {
		mCondition.wait; // Wait for all writes to finish
	}
}