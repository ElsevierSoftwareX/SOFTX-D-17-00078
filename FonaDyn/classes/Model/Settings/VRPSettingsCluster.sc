// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPSettingsCluster {
	// States
	var <>nClusters; // Number of clusters
	var <nDimensions; // The dimensionality of the points
	var <nHarmonics; // The number of harmonics used to produce the points
	var <>learn; // Whether the clustering algorithm should learn from the data or
	             // simply use prelearnt data to classify incoming points
	var <>reset; // Whether to allow resets in the data or not; a reset will reset
	             // learnt data (essentially replace it with initial values)

	var <>learnedData; // Pre-learned data that we should init KMeansRTv2 with as [clusterCounts, clusterCentroids]

	var <>nSamples; // # of samples to estimate the cycles
	var <>smoothFactor; // Smooth factor for the SmoothedClusterCycle

	nHarmonics_ { | n |
		nHarmonics = n + 1;  	// internally, one extra "harmonic" for the residual power
		// nDimensions = 2 * (n - 1);
		nDimensions = 3 * n ; // for clustering by dLevel, cos(dPhi) and sin(dPhi)
	}

	*new {
		^super.new.init;
	}

	init {
		learn = true;
		reset = false;

		nSamples = 100;
		smoothFactor = 0.995; // Keep 99.5% of the old value and take 0.5% of the new value
	}
}