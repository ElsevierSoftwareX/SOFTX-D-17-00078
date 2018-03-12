// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
/**
 * Manages the buffers, synths and settings concerning the calculations of the VRP display.
 */

VRPControllerVRP {
	var mData;
	var mTarget;
	var mSynth;

	*new { | target, data |
		^super.new.init(target, data);
	}

	// Init given the target and output data structure
	// This function is called once only!
	init { | target, data |
		mTarget = target;
		mData = data;
	}

	// Tell the busManager what buses it requires.
	// This function is called before prepare.
	requires { | busManager |
		busManager
		.requireAudio(\ConditionedMicrophone)
		.requireControl(\Frequency)
		.requireControl(\Amplitude)
		.requireControl(\Clarity)
		.requireControl(\Crest);
	}

	// Prepare to start - init the SynthDefs and allocate Buffers.
	// This function is always called before start.
	prepare { | libname, server, busManager, clock |
		VRPSDVRP.compile(libname);
	}

	// Start - Create the synths, and initiate fetching of data at the regular
	// interval given by the clock parameter.
	// This function is always called between prepare and stop.
	start { | server, busManager, clock |
		var bm = busManager;

		mSynth = Synth(*VRPSDVRP.analyzeAudio(
			bm.audio(\ConditionedMicrophone),
			bm.control(\Frequency),
			bm.control(\Amplitude),
			bm.control(\Clarity),
			bm.control(\Crest),
			mTarget),
			addAction: \addToTail
		);
	}

	// Free the synths and buffers after finishing fetching data.
	// The synths are guaranteed to be paused at this point - so buffers should be stable.
	stop {
		// Free synths...
		mSynth.free;

		// ...and buffers
	}

	sync {

	}
}