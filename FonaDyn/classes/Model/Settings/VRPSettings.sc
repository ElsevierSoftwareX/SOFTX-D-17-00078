// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
//
// General manager of settings.
//
VRPSettings {
	var <io;
	var <sampen;
	var <csdft;
	var <cluster;
	var <vrp;
	var <scope;
	var <general;

	*new {
		^super.new.init;
	}

	init {
		io = VRPSettingsIO();
		sampen = VRPSettingsSampEn();
		csdft = VRPSettingsCSDFT();
		cluster = VRPSettingsCluster();
		vrp = VRPSettingsVRP();
		scope = VRPSettingsScope();
		general = VRPSettingsGeneral();
	}
}