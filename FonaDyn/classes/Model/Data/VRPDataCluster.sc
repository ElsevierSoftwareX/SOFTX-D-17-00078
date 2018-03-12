// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPDataCluster {
	/**
	 * Fetching cluster data as:
	 * pointsInCluster = [# of points in cluster 1..nClusters]
	 * centroids = [ Centroid 1..nClusters ]
	 * where each Centroid is defined as an array of length nDimensions (the center of the cluster).
	 */
	var <>pointsInCluster;
	var <>centroids;

	var <>cycleData; // From SmoothedClusterCycle

	var <palette; // Palette for the clusters

	var <>resetNow; // If true it will reset the counts/centroids on the next possible chance.

	*new { | settings |
		^super.new.init(settings);
	}

	init { | settings |
		var s = settings;
		var n = s.cluster.nClusters;

		resetNow = false;
		palette = this.class.palette(n);
	}

}
