// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPSettingsScope {
	// States
	var <>duration; // Duration of the scopes (up to duration seconds back is stored)

	var <>minSamples; // Minimum # of samples required by the MovingEGG
	var <>maxSamples; // Maximum # of samples required by the MovingEGG.
	var <>normalize; // True means it should normalize the height
	var <>movingEGGCount; // # of overlapping EGG cycles
	var <>movingEGGSamples; // # of samples representing each EGG cycle

	*new {
		^super.new.init;
	}

	init {
		duration = 2;

		minSamples = 20;
		maxSamples = 882;
		normalize = true;

		movingEGGCount = 5;
		movingEGGSamples = 80;
	}
}