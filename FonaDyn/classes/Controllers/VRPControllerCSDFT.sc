// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //

VRPControllerCSDFT {
	var mTarget;
	var mData;

	// Synths
	var mSynthDFTFilter;
	var mSynthNDFTs;
	var mSynthCycleSeparation;

	*new { | target, data |
		^super.new.init(target, data);
	}

	// Init given the target and output data structure
	// This function is called once only!
	init { | target, data |
		mTarget = target;
		mData = data; // General data
	}

	// Tell the busManager what buses it requires.
	// This function is called before prepare.
	requires { | busManager |
		var d = mData;
		var cd = d.csdft;
		var s = d.settings;
		var cs = s.csdft;

		busManager
		.requireAudio(\ConditionedEGG)
		.requireAudio(\GateCycle)
		.requireAudio(\GateDelayedCycle)
		.requireAudio(\GateDFT)
		.requireAudio(\GateFilteredDFT)
		.requireAudio(\AmplitudeFirst, cs.nHarmonics)
		.requireAudio(\PhaseFirst, cs.nHarmonics)
		.requireControl(\Clarity)
	}

	// Prepare to start - init the SynthDefs and allocate Buffers.
	// This function is always called before start.
	prepare { | libname, server, busManager, clock |
		var d = mData;
		var cd = d.csdft;
		var s = d.settings;
		var cs = s.csdft;

		VRPSDCSDFT.compile(libname, cs.nHarmonics, cs.tau, cs.minFrequency, cs.minSamples);
	}

	// Start - Create the synths, and initiate fetching of data at the regular
	// interval given by the clock parameter.
	// This function is always called between prepare and stop.
	start { | server, busManager, clock |
		var bm = busManager;
		var d = mData;
		var s = d.settings;
		var cs = s.csdft;

		// Select the appropriate method
		var csfn =
		switch (cs.method,
			VRPSettingsCSDFT.methodPhasePortrait, \phasePortrait,
			VRPSettingsCSDFT.methodPeakFollower, \peakFollower
		);


		mSynthCycleSeparation = Synth(*
			VRPSDCSDFT.perform(csfn,
			bm.audio(\ConditionedEGG),
			bm.audio(\GateCycle),
			mTarget,
			\addToTail)
		);

		mSynthNDFTs = Synth(*VRPSDCSDFT.nDFTs(
			bm.audio(\ConditionedEGG),
			bm.audio(\GateCycle),
			bm.control(\Clarity),
			bm.audio(\GateDFT),
			bm.audio(\GateDelayedCycle),
			bm.audio(\AmplitudeFirst),
			bm.audio(\PhaseFirst),
			mTarget,
			\addToTail)
		);

		mSynthDFTFilter = Synth(*VRPSDCSDFT.dftFilters(
			bm.audio(\GateDFT),
			bm.audio(\GateCycle),
			bm.audio(\GateDelayedCycle),
			bm.control(\Clarity),
			bm.audio(\GateFilteredDFT),
			mTarget,
			\addToTail)
		);
	}

	// Free the synths and buffers after finishing fetching data.
	// The synths are guaranteed to be paused at this point - so buffers should be stable.
	stop {
		// Free synths
		mSynthDFTFilter.free;
		mSynthNDFTs.free;
		mSynthCycleSeparation.free;

		// And buffers...
	}

	sync {} // Nothing to do
}