// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPDataScope {
	// Data
	var <>sampen; // Plot data for SampEn
	var <>audioegg; // Plot data for Audio & EGG

	var <>movingEGGData;

	*new { | settings |
		^super.new.init(settings);
	}

	init { | settings |
	}
}