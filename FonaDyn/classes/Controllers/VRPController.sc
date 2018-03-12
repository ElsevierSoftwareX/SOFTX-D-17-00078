// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPController {
	var <>io;
	var <>sampen;
	var <>csdft;
	var <>cluster;
	var <>vrp;
	var <>scope;
	var <>postp; // post processing

	*new {
		^super.new.init;
	}

	init { }

	asArray {
		^[io, sampen, csdft, cluster, vrp, scope, postp];
	}
}