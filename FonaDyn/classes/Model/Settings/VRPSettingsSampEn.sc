// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPSettingsSampEn {
	// States
	var <>amplitudeWindowSize; // The size of the window (in cycles) where we look for matching sequences via the Sample Entropy algorithm
	var <>amplitudeHarmonics; // The number of harmonics we use to produce the SampEn measurement
	var <>amplitudeSequenceLength; // The length of a matching sequence
	var <>amplitudeTolerance; // The tolerance for matching values in the sequences

	var <>phaseWindowSize; // The size of the window (in cycles) where we look for matching sequences via the Sample Entropy algorithm
	var <>phaseHarmonics; // The number of harmonics we use to produce the SampEn measurement
	var <>phaseSequenceLength; // The length of a matching sequence
	var <>phaseTolerance; // The tolerance for matching values in the sequences

	var <>limit; // Limit for producing arrows

	*new {
		^super.new.init;
	}

	init {
		limit = 0.1;
	}
}