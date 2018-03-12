// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //

VRPSDVRP {
	classvar nameAnalyzeAudio = \sdAnalyzeAudio;

	*compile { | libname |

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Analyze Audio SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////  ;
		SynthDef(nameAnalyzeAudio,
			{ | aiBusConditionedMic,
				coBusFrequency,
				coBusAmplitude,
				coBusClarity,
				coBusCrest |

				var in, inpow, amp, freq, crest, gate, hasFreq, out;

				in = In.ar(aiBusConditionedMic);
				gate = LFPulse.kr(100, 0, 0.01);	// Gate added 2016-01-23 to reduce Crest recalc frequency
				crest = Crest.kr(in, 441, gate);   	// Crest may contain a library bug (abs rather than square)

				// Mean-square over 30Hz, remove dips; - should ideally be cycle-synchronous
				// The median filter delay approximates the freq and condEGG delays
				inpow = Median.kr(17, WAmp.kr(in.squared, 0.033));

				// The following line serves only to guard agains true-zero audio in test files
				amp = Select.kr(InRange.kr(inpow, -1.0, 0.0), [0.5 * inpow.ampdb, DC.kr(-100)]);

				// Integrator brings down the HF
				// # freq, hasFreq = Pitch.kr(Integrator.ar(in, 0.995), execFreq: 20);
				# freq, hasFreq = Tartini.kr(Integrator.ar(in, 0.995) /*, n: 512, overlap: 256*/);
				freq = freq.cpsmidi;

				Out.kr(coBusFrequency, [freq]);
				Out.kr(coBusAmplitude, [amp]);
				Out.kr(coBusClarity, [hasFreq]);
				Out.kr(coBusCrest, [crest]);
			}
		).add(libname);
	}

	*analyzeAudio { | aiBusConditionedMic, coBusFrequency, coBusAmplitude, coBusClarity, coBusCrest ...args |
		^Array.with(nameAnalyzeAudio,
			[
				\aiBusConditionedMic, aiBusConditionedMic,
				\coBusFrequency, coBusFrequency,
				\coBusAmplitude, coBusAmplitude,
				\coBusClarity, coBusClarity,
				\coBusCrest, coBusCrest
			],
			*args
		);
	}
}