// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //

VRPSDCluster {
	classvar nameNCluster = \sdNCluster;
	classvar nameNClusterNoReset = \sdNClusterNoReset;
	classvar nameGeneratePoints = \sdGeneratePoints;
	classvar nameSmoothedClusterCycle = \sdSmoothedClusterCycle;

	*compile { | libname, nClusters, nHarmonics, learn, minSamples, maxSamples, smoothFactor |

		nHarmonics = nHarmonics.asInteger;
		nClusters = nClusters.asInteger;
		learn = learn.asInteger;
		minSamples = minSamples.asInteger;
		maxSamples = maxSamples.asInteger;

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// SynthDef that generates all points
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameGeneratePoints,
			{ | aiBusAmplitudeFirst, // The first of nHarmonics consecutive buses with DFT amplitudes
				aiBusPhaseFirst, // The first of nHarmonics consecutive buses with DFT phases
				aoBusDeltaAmplitudeFirst, // The first of nHarmonics consecutive output buses for delta amplitudes
				aoBusDeltaPhaseFirst | // The first of nHarmonics consecutive output buses for delta phases

				var amps, phases, delta_amps, delta_phases;

				amps = In.ar(aiBusAmplitudeFirst, nHarmonics);
				phases = In.ar(aiBusPhaseFirst, nHarmonics);

				// Compute all level deltas relative to the 1st term
				delta_amps   = MulAdd(amps[0],  -1, amps[1..(nHarmonics-1)]);
				delta_phases = MulAdd(phases[0], -1, phases[1..(nHarmonics-1)]);

				Out.ar(aoBusDeltaAmplitudeFirst, delta_amps);
				Out.ar(aoBusDeltaPhaseFirst, delta_phases);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// N Cluster SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameNCluster,
			{ | iBufferKMeansRT, // The buffer used by KMeansRT
				aiBusDeltaAmplitudeFirst, // The first of nHarmonics consecutive output buses for delta amplitudes
				aiBusDeltaPhaseFirst, // The first of nHarmonics consecutive output buses for delta phases
				aiBusGate, // Gate for when new DFT data is available through aiBusDeltaAmplitudeFirst and aiBusDeltaPhaseFirst
				ciBusReset, // Reset once this becomes active (start over with learning)
				aoBusClusterNumber | // The output bus for the cluster number

				var delta_amps, delta_phases, scales, scaledCosines, scaledSines;
				var aIndex, gate, count;
				var reset;

				count = nHarmonics - 1;
				delta_amps = In.ar(aiBusDeltaAmplitudeFirst, count);
				delta_phases = In.ar(aiBusDeltaPhaseFirst, count);
				gate = In.ar(aiBusGate);
				reset = In.kr(ciBusReset);

				// Scale down the fundamental only
				scales = 1.0 ! (count-1) ++ 0.001;
				scaledCosines = MulAdd(delta_phases.cos, scales);
				scaledSines   = MulAdd(delta_phases.sin, scales);

				// The Gate.ar here should not be needed, according to the doc for KMeansRTv2...
				aIndex = Gate.ar(KMeansRTv2.ar(iBufferKMeansRT, delta_amps ++ scaledCosines ++ scaledSines, nClusters, gate, reset, learn), gate);
				Out.kr(ciBusReset, [0]); // Reset the reset flag, so we don't keep on resetting every control period
				Out.ar(aoBusClusterNumber, [aIndex]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// N Cluster No Reset SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameNClusterNoReset,
			{ | iBufferKMeansRT, // The buffer used by KMeansRT
				aiBusDeltaAmplitudeFirst, // The first of nHarmonics consecutive output buses for delta amplitudes
				aiBusDeltaPhaseFirst, // The first of nHarmonics consecutive output buses for delta phases
				aiBusGate, // Gate for when new DFT data is available through aiBusDeltaAmplitudeFirst and aiBusDeltaPhaseFirst
				aoBusClusterNumber | // The output bus for the cluster number

				var delta_amps, delta_phases, scales, scaledCosines, scaledSines;
				var aIndex, gate, count;

				count = nHarmonics - 1;
				scaledCosines = Array.new(count);
				scaledSines = Array.new(count);

				delta_amps = In.ar(aiBusDeltaAmplitudeFirst, count);
				delta_phases = In.ar(aiBusDeltaPhaseFirst, count);
				gate = In.ar(aiBusGate);

				// Scale down the fundamental only
				scales = 1.0 ! (count-1) ++ 0.001;
				scaledCosines = MulAdd(delta_phases.cos, scales);
				scaledSines   = MulAdd(delta_phases.sin, scales);

				// The Gate.ar here should not be needed, according to the doc for KMeansRTv2...
				aIndex = Gate.ar(KMeansRTv2.ar(iBufferKMeansRT, delta_amps ++ scaledCosines ++ scaledSines, nClusters, gate, 0, learn), gate);
				Out.ar(aoBusClusterNumber, [aIndex]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// SmoothedClusterCycle
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameSmoothedClusterCycle,
			{ | aiBusEGG,
				aiBusGateCycle,
				aiBusGateFilteredDFT,
				aiBusClusterNumber,
				oBuffer |

				var egg = In.ar(aiBusEGG);
				var gateCycle = In.ar(aiBusGateCycle);
				var gateFilteredDFT = In.ar(aiBusGateFilteredDFT);
				var clusterNumber = In.ar(aiBusClusterNumber);

				SmoothedClusterCycle.ar(
					oBuffer, // Required for output
					egg, // Required to grab the cycle data
					gateCycle, // Required to split the EGG signal into cycles
					gateFilteredDFT, // Required to know which cycles were discarded
					clusterNumber, // Required to know which cluster this cycle belongs to
					nClusters, minSamples, maxSamples, smoothFactor); // Constants
			}
		).add(libname);
	}

	*generatePoints { |
		aiBusAmplitudeFirst, // The first of nHarmonics consecutive buses with DFT amplitudes
		aiBusPhaseFirst, // The first of nHarmonics consecutive buses with DFT phases
		aoBusDeltaAmplitudeFirst, // The first of nHarmonics consecutive output buses for delta amplitudes
		aoBusDeltaPhaseFirst // The first of nHarmonics consecutive output buses for delta phases
		...args |

		^Array.with(nameGeneratePoints,
			[
				\aiBusAmplitudeFirst, aiBusAmplitudeFirst,
				\aiBusPhaseFirst, aiBusPhaseFirst,
				\aoBusDeltaAmplitudeFirst, aoBusDeltaAmplitudeFirst,
				\aoBusDeltaPhaseFirst, aoBusDeltaPhaseFirst,
			],
			*args
		);
	}

	*nClusters { |
		iBufferKMeansRT, // The buffer used by KMeansRT
		aiBusDeltaAmplitudeFirst, // The first of nHarmonics consecutive output buses for delta amplitudes
		aiBusDeltaPhaseFirst, // The first of nHarmonics consecutive output buses for delta phases
		aiBusGate, // Gate for when new DFT data is available through aiBusDeltaAmplitudeFirst and aiBusDeltaPhaseFirst
		ciBusReset, // The bus with the reset input for the KMeansRT UGen.
		aoBusClusterNumber // The output bus for the cluster number
		...args |

		^Array.with(nameNCluster,
			[
				\iBufferKMeansRT, iBufferKMeansRT,
				\aiBusDeltaAmplitudeFirst, aiBusDeltaAmplitudeFirst,
				\aiBusDeltaPhaseFirst, aiBusDeltaPhaseFirst,
				\aiBusGate, aiBusGate,
				\ciBusReset, ciBusReset,
				\aoBusClusterNumber, aoBusClusterNumber
			],
			*args
		);
	}

	*nClustersNoReset { |
		iBufferKMeansRT, // The buffer used by KMeansRT
		aiBusDeltaAmplitudeFirst, // The first of nHarmonics consecutive output buses for delta amplitudes
		aiBusDeltaPhaseFirst, // The first of nHarmonics consecutive output buses for delta phases
		aiBusGate, // Gate for when new DFT data is available through aiBusDeltaAmplitudeFirst and aiBusDeltaPhaseFirst
		aoBusClusterNumber // The output bus for the cluster number
		...args |

		^Array.with(nameNClusterNoReset,
			[
				\iBufferKMeansRT, iBufferKMeansRT,
				\aiBusDeltaAmplitudeFirst, aiBusDeltaAmplitudeFirst,
				\aiBusDeltaPhaseFirst, aiBusDeltaPhaseFirst,
				\aiBusGate, aiBusGate,
				\aoBusClusterNumber, aoBusClusterNumber
			],
			*args
		);
	}

	*smoothedClusterCycle { |
		aiBusEGG,
		aiBusGateCycle,
		aiBusGateFilteredDFT,
		aiBusClusterNumber,
		oBuffer
		...args |

		^Array.with(nameSmoothedClusterCycle,
			[
				\aiBusEGG, aiBusEGG,
				\aiBusGateCycle, aiBusGateCycle,
				\aiBusGateFilteredDFT, aiBusGateFilteredDFT,
				\aiBusClusterNumber, aiBusClusterNumber,
				\oBuffer, oBuffer
			],
			*args
		);
	}

}