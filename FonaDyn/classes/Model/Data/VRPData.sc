// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
//
// General manager of the data to present in the views.
//
VRPData {
	var <io;
	var <sampen;
	var <csdft;
	var <cluster;
	var <vrp;
	var <scope;
	var <general;

	var <settings; // A deep copy of the settings made on each start

	*new { | s |
		^super.new.init(s);
	}

	init { | s |
		settings = s.deepCopy;

		io = VRPDataIO(settings);
		sampen = VRPDataSampEn(settings);
		csdft = VRPDataCSDFT(settings);
		cluster = VRPDataCluster(settings);
		vrp = VRPDataVRP(settings);
		scope = VRPDataScope(settings);
		general = VRPDataGeneral(settings);
	}
}