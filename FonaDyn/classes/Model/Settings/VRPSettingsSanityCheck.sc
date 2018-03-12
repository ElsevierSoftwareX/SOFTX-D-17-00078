//
// Addition of a sanity check for all the settings.
// May help to contain all the sanityChecks in the same file, instead of small checks in each individual settings file.
//

+ VRPSettings {
	sanityCheck {
		var ret = true;

		ret = ret and: csdft.sanityCheck(this);

		^ret;
	}
}

+ VRPSettingsCSDFT {
	sanityCheck { | settings |
		nHarmonics =
		[
			settings.cluster.nHarmonics,
			settings.sampen.amplitudeHarmonics,
			settings.sampen.phaseHarmonics
		].maxItem;

		^true;
	}
}