// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPDataVRP {
	// Data
	var <density; // Density as # of values that went into each slot, or nil if no data exists
	var <clarity; // Clarity at each slot, or nil if no data exists
	var <>maxDensity; // Largest value in the density matrix, or nil if no data exists
	var <crest; // Crest factor at each slot, or nil if no data exists
	var <entropy; // SampEn entropy at each slot, or nil if no data exists
	var <mostCommonCluster; // The most common cluster in each slot, or nil if no data exists
	var <clusters; // A density matrix for each cluster

	// The most recent frequency, amplitude, clarity, entropy and cluster measurements
	var <>currentFrequency;
	var <>currentAmplitude;
	var <>currentClarity;
	var <>currentEntropy;
	var <>currentCluster;

	// Settings
	classvar <nMinMIDI = 30;
	classvar <nMaxMIDI = 96;
	classvar <nMinSPL = 40;
	classvar <nMaxSPL = 120;
	classvar <>clarityThreshold = 0.96;
//	classvar <>waitSeconds = 1.0;

	classvar <vrpWidth = 66;  // nMaxMIDI - nMinMIDI; // 1 cell per semitone
	classvar <vrpHeight = 80; // nMaxSPL  - nMinSPL;  // 1 cell per dB

	*new { | settings |
		^super.new.init(settings);
	}

	init { | settings |
		var w = this.class.vrpWidth + 1;    // +1 because we want to include the upper limit
		var h = this.class.vrpHeight + 1;
		var cs = settings.cluster;
		var clusterPalette = VRPDataCluster.palette( cs.nClusters );

		density = DrawableSparseMatrix(h, w, VRPDataVRP.paletteDensity);
		maxDensity = nil;
		clarity = DrawableSparseMatrix(h, w, VRPDataVRP.paletteClarity);
		crest = DrawableSparseMatrix(h, w, VRPDataVRP.paletteCrest);
		entropy = DrawableSparseMatrix(h, w, VRPDataVRP.paletteEntropy);
		mostCommonCluster = DrawableSparseMatrix(h, w, clusterPalette);
		clusters = Array.fill(cs.nClusters,
			{ | idx |
				var color = clusterPalette.(idx);
				DrawableSparseMatrix(h, w,
					VRPDataVRP.paletteCluster(color)
				)
			}
		);
	}

	*frequencyToIndex { arg freq, width = VRPDataVRP.vrpWidth;
		^freq
		.linlin(nMinMIDI, nMaxMIDI, 0, width)
		.round
		.asInteger;
	}

	*amplitudeToIndex { arg amp, height = VRPDataVRP.vrpHeight;
		^(amp + nMaxSPL)
		.linlin(nMinSPL, nMaxSPL, 0, height)
		.round
		.asInteger;
	}

	reorder { arg newOrder;
		var tmp;
		var color;
		// To recolor, we need only change to the new order and reset the palettes
		if ((newOrder.class==Array) and: (newOrder.size==clusters.size),
			{
			var bOK = true;
			var clusterPalette = VRPDataCluster.palette( newOrder.size );
				newOrder.do { arg elem, i; if (elem.class != Integer, { bOK = false }) };
				if (bOK, {
					tmp = clusters[newOrder];
					clusters = tmp;
					clusters.do { arg c, i;
						if (c.notNil, {
							color = clusterPalette.(i);
							c.recolor(VRPDataVRP.paletteCluster(color));
							}, {
								post(c);
						})
					};
					mostCommonCluster.renumber(newOrder);
				})
			}
		);
	}

	saveVRPdata {
		var cDelim = VRPMain.cListSeparator;

		Dialog.savePanel({
			| path |
			if (path.endsWith(".csv").not) {
				path = path ++ "_VRP.csv"; // "VRP Save File"
			};

			// Write every non-nil VRP cell as a line
			// First cols are x,y in (MIDI, dB),
			// then density, clarity, crest factor, maxEntropy, topCluster, cluster 1..n
			File.use(path, "w", { | file |
				var cv;

				// Build and output the title row
				cv = List.newUsing(["MIDI", "dB", "Total", "Clarity", "Crest", "Entropy", "maxCluster"]);
				clusters.size.do ({ |i| cv.add("Cluster"+(i+1).asString)});
				cv.do ({|v, i| file << v; file.put(cDelim)});
				file.put($\r); file.nl;
				cv.clear;

				// Build and output the data rows
				density.rows.do({ |r|
					density.columns.do({arg c; var dValue, mc;
						dValue = density.at(r, c);
						if (dValue.notNil, {
							cv.add(c+nMinMIDI);
							cv.add(r+nMinSPL);
							cv.add(dValue);
							cv.add(clarity.at(r, c));
							cv.add(crest.at(r, c));
							cv.add(entropy.at(r, c) ? 0);
							mc = mostCommonCluster.at(r, c);
							if (mc.isNil, { cv.add(-1) }, { cv.add(1+mc[0]) });
							clusters.size.do ({|k| cv.add(clusters[k].at(r, c) ? 0)});
							cv.do ({|v, i| file << v; file.put(cDelim)});
							file.put($\r); file.nl;
							cv.clear;
						})}
			)})});
		});
	}
}
