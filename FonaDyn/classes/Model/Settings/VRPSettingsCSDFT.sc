// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPSettingsCSDFT {
	// States
	var <>nHarmonics; // The number of harmonics of interest
	var <>tau; // Cutoff frequency in Hertz for the 2nd order Butterworth highpass
	           // filter used by the PeakFollower implementation for cycle separation
	var <>minSamples; // Minimum required samples for the DFT calculations
	var <>minFrequency; // Smallest frequency to calculate DFTs for.

	var <>method;

	classvar <methodPhasePortrait = 0;
	classvar <methodPeakFollower = 1;

	*new {
		^super.new.init;
	}

	init {
		tau = 0.99;
		minSamples = 20;
		minFrequency = 50;
		method = methodPhasePortrait;
	}
}