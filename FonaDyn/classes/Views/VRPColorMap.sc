+ VRPDataVRP {
	*paletteDensity {
		^{ | v |
			// map 1..<100 to light...darker grey
			var cSat = v.clip(1, 100).linlin(1, 100, 0.9, 0.5);
			Color.grey(cSat, 1);
		};
	}

	*paletteClarity {
		^{ | v |
			// Map values above the threshold to a green shade (brighter green the better the clarity)
			// Map values below the threshold to gray
			if (v > VRPDataVRP.clarityThreshold,
				Color.green(v.linlin(VRPDataVRP.clarityThreshold, 1.0, 0.5, 1.0)),
				Color.gray)
		};
	}

	*paletteCrest {
		^{ | v |
			var cHue;

			// map crest factor 1.4 (+3 dB) ... <4 (+12 dB) to green...red
			cHue = v.clip(0, 5.0).linlin(1.4, 4, 0.33, 0);
			Color.hsv(cHue, 1, 1);
		};
	}

	*paletteEntropy {
		^{ | v |
			var sat;
			// Brown, saturated at 20. Should be scaled for nHarmonics in SampEn
			sat = v.clip(1, 20).linlin(1, 20, 0.1, 1.0);
			Color.white.blend(Color.new255(165, 42, 42), sat);
		};
	}

	*paletteCluster { | typeColor |
		^{ | v |
			// Blend with white depending on the count. Counts >= 50 aren't blended at all.
			var sat, cSat;
			sat = v.clip(1, 50).linlin(1, 50, 0.5, 0);
			cSat = typeColor.blend(Color.white, sat);
			cSat;
		};
	}

	*paletteCursor {
		^{ | v |
			// Map values above the threshold to a blue shade
			// Map values below the threshold to gray
			if (v > VRPDataVRP.clarityThreshold,
				Color.blue(v**3),
				Color.gray
			)
		};
	}
}

+ VRPDataCluster {
	*palette { | nClusters |
		^{ | v |
			var color, cHue, sat;
			(v.class == Array).if(
				{	// invoked with [index, count]
					sat = v[1].clip(1, 100).linlin(1, 100, 0, 0.7);
					color = Color.hsv(v[0].linlin(0.0, nClusters, 0.0, 0.999), sat, 1);
				},{ // invoked with index only
					cHue = v.linlin(0.0, nClusters, 0.0, 0.999);
					color = Color.hsv(cHue, 0.7, 1);
			});
			color;
		};
	}
}