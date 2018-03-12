// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPSDCSDFT {
	classvar namePeakFollower = \sdPeakFollower;
	classvar namePhasePortrait = \sdPhasePortrait;
	classvar nameNDFTs = \sdNDFT;
	classvar nameDFTFilters = \sdDFTFilters;

	*compile { | libname, nHarmonics, tau, minFrequency, minSamples |

		nHarmonics = nHarmonics.asInteger;
		minSamples = minSamples.asInteger;

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Peak Follower SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(namePeakFollower,
			{ | aiBusConditionedEGG,
				aoBusGate |

				var in, inLP, dEGG, signalPlus, signalMinus, z;

				in = In.ar(aiBusConditionedEGG);     // Get the preconditioned EGG signal
				dEGG = in - Delay1.ar(in);       // Compute its "derivative"

				// Double Sided Peak Follower Cycle Detection
				// dEGG input is offset +1 to work around the fact
				// that PeakFollower first takes the absolute value of its input.
				// So dEGG magnitudes should be < 1 , which is always true
				signalPlus  = HPF.ar(PeakFollower.ar(dEGG.madd( 1, 1), tau), 20);
				signalMinus = HPF.ar(PeakFollower.ar(dEGG.madd(-1, 1), tau), 20);

				// Trigger on EGG cycles and measure their duration
				z = Trig1.ar(SetResetFF.ar(signalPlus, signalMinus), 0);

				Out.ar(aoBusGate, [z]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Phase Portrait SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(namePhasePortrait,
			{ | aiBusConditionedEGG,
				aoBusGate |

				var in, inLP, integr, phi, z, signalPlus, signalMinus;

				integr = DC.ar(0.0); // Dummy to initialize the variable
				in = In.ar(aiBusConditionedEGG);     // Get the preconditioned EGG signal

				// Phase Portrait
				// New method 2016-08-14: take integral rather than Hilbert
				// Saves CPU, works well on weak noisy signals

				if (Main.versionAtLeast(3,7), {
					integr = Integrator.ar(in, 0.999, 0.05);  // works only from SC 3.7.x
					},{
					integr = Delay1.ar(integr, 0.999, in).madd(0.05);  // Workaround for 3.6.6 compatibility
					}
				);
				inLP = HPF.ar(integr, 50);	// Attenuate integrated DC shifts
				phi = atan2(in, inLP);

				// Double Sided Peak Follower Cycle Detection, on phi
				// phi input is offset +pi to work around the fact
				// that PeakFollower first takes the absolute value of its input.
				// So phi magnitudes should be <= pi, which is always true
				signalPlus  = HPF.ar(PeakFollower.ar(phi.madd( 1, pi), tau), 20);
				signalMinus = HPF.ar(PeakFollower.ar(phi.madd(-1, pi), tau), 20);

				z = Trig1.ar(SetResetFF.ar(signalPlus, signalMinus), 0);

				Out.ar(aoBusGate, [z]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// N DFTs SynthDef  - modified such that the last harmonic represents the power of all remaining harmonics
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameNDFTs,
			{ | aiBusConditionedEGG,
				aiBusGateCycle,
				ciBusClarity,
				aoBusGateDFT,
				aoBusGateDelayedCycle,
				aoBusAmplitudeFirst,
				aoBusPhaseFirst |

				var in, ix, clarity, gcycle, gDFT, gSkipped, length, gLength, inPower, invLength, amps, phases;
				var res, complex, complexAbs;
				var inAcc, harmPower = 0.0;

				in = In.ar(aiBusConditionedEGG);
				gcycle = In.ar(aiBusGateCycle);

				// Perform the DFT calculations for each cycle marked by gate3
				// Ignores DFT cycles that are too long/short. Using default with a minimum of 10 samples
				// and a minimum of 50 cycles/s (typically equates to at most 882 samples)
				#gDFT, gSkipped, length ...res = DFT2.ar(in, gcycle, (1..nHarmonics), minFrequency, minSamples);
				gLength = Gate.ar(length, gDFT);   // DFT2 outputs zeros most of the time
				invLength = gLength.reciprocal;

				complex = nHarmonics collect: { | i | Complex(res[2*i], res[2*i+1]) };

				complexAbs = complex.abs * invLength;

				phases = complex.phase;
				phases[nHarmonics-1] = MulAdd(phases[0], 2, 0);
				// Equates the phase of "high partials" to that of the fundamental - changed 2016-07-06

				// Compute total power in the analyzed harmonics less the highest
				ix = (0..(nHarmonics-2));
				harmPower = Gate.ar(complexAbs[ix].squared.sum, gDFT);

				// accumulate the total power in the input signal; reset on "gcycle"
				inAcc = AverageOutput.ar(in.squared, gcycle);
				inPower = Gate.ar(VariadicDelay.ar(inAcc, gcycle, [gDFT + gSkipped], 600), gDFT);

				complexAbs[nHarmonics-1] = (inPower - harmPower); // .sqrt? replace abs of highest partial
				amps = complexAbs.ampdb * 0.1;   // operate with Bel rather than dB

				// The delayed cycle gate is simply whenever we skip or output a DFT for each input cycle
				Out.ar(aoBusGateDelayedCycle, [gDFT + gSkipped]);
				Out.ar(aoBusGateDFT, [gDFT]); // Whenever we have DFT output in amps/phases
				Out.ar(aoBusAmplitudeFirst, Gate.ar(amps, gDFT));
				Out.ar(aoBusPhaseFirst, Gate.ar(phases, gDFT));
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// DFT Filters
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameDFTFilters,
			{ | aiBusGateDFT,
				aiBusGateCycle,
				aiBusGateDelayedCycle,
				ciBusClarity,
				aoBusGateFilteredDFT |

				// Grab maxSamples constant
				var maxSamples = SampleRate.ir / minFrequency;

				// Grab gates
				var gdft = In.ar(aiBusGateDFT);
				var gc = In.ar(aiBusGateCycle);
				var gdc = In.ar(aiBusGateDelayedCycle);
				var gout; // Output gate

				// Grab EGG measurements and delay them
				// Note that we need to delay the measurements made at the beginning of each EGG cycle
				// until the DFT output is ready (or skipped).
				var clarity;

				// Note that VariadicDelay.ar converts the control rate inputs into audio rate outputs
				#clarity =
				VariadicDelay.ar(
				[
					In.kr(ciBusClarity)
				]
				, gc, gdc, maxSamples);

				// Use the "Clarity" metric as a gate > 0.99 (ceil rounds up to the nearest integer)
				// i.e. we get 0 if clarity < threshold and 1 otherwise
				gout = gdft * (clarity - VRPDataVRP.clarityThreshold).ceil;
				Out.ar(aoBusGateFilteredDFT, [gout]);
			}
		).add(libname);
	}

	*peakFollower { |
		aiBusConditionedEGG, // The conditioned EGG
		aoBusGate // The output gate, open when a new cycle begins
		...args |

		^Array.with(namePeakFollower,
			[
				\aiBusConditionedEGG, aiBusConditionedEGG,
				\aoBusGate, aoBusGate
			],
			*args
		);
	}

	*phasePortrait { |
		aiBusConditionedEGG, // The conditioned EGG
		aoBusGate // The output gate, open when a new cycle begins
		...args |

		^Array.with(namePhasePortrait,
			[
				\aiBusConditionedEGG, aiBusConditionedEGG,
				\aoBusGate, aoBusGate
			],
			*args
		);
	}

	*nDFTs { |
		aiBusConditionedEGG, // The conditioned EGG signal
		aiBusGateCycle, // The gate that marks when a new cycle begins
		ciBusClarity, // A clarity measurement
		aoBusGateDFT, // The output gate for when DFT data is available
		aoBusGateDelayedCycle, // The output gate when DFT data is available, or a cycle got skipped
		aoBusAmplitudeFirst, // The first of nHarmonics consecutive output buses for the amplitudes
		aoBusPhaseFirst // The first of nHarmonics consecutive output buses for the phases
		...args |

		^Array.with(nameNDFTs,
			[
				\aiBusConditionedEGG, aiBusConditionedEGG,
				\aiBusGateCycle, aiBusGateCycle,
				\ciBusClarity, ciBusClarity,
				\aoBusGateDFT, aoBusGateDFT,
				\aoBusGateDelayedCycle, aoBusGateDelayedCycle,
				\aoBusAmplitudeFirst, aoBusAmplitudeFirst,
				\aoBusPhaseFirst, aoBusPhaseFirst
			],
			*args
		);
	}

	*dftFilters { |
		aiBusGateDFT,
		aiBusGateCycle,
		aiBusGateDelayedCycle,
		ciBusClarity,
		aoBusGateFilteredDFT
		...args |

		^Array.with(nameDFTFilters,
			[
				\aiBusGateDFT, aiBusGateDFT,
				\aiBusGateCycle, aiBusGateCycle,
				\aiBusGateDelayedCycle, aiBusGateDelayedCycle,
				\ciBusClarity, ciBusClarity,
				\aoBusGateFilteredDFT, aoBusGateFilteredDFT
			],
			*args
		);
	}
}