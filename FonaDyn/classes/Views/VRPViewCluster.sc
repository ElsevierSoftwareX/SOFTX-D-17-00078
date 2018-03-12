// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //

VRPViewCluster {
	// Views
	var mView;
	var mViewClusterCentroids;
	var mViewClusterStats;

	// Controls
	var mSliderCluster;
	var mStaticTextCluster;

	var mButtonLearn;
	var mButtonReset;
	var mButtonLoad;
	var mButtonSave;
	var mButtonStart;

	var mStaticTextClusters;
	var mNumberBoxClusters;

	var mStaticTextHarmonics;
	var mNumberBoxHarmonics;

	// States
	var mSelected;
	var mGridCentroids;

	var mStarted; // We need to know when the server is started, just started/stopped, and by keeping a member we can do that.
	var mResetNow; // We need to know when the server should reset the counts/centroids.

	var mClusterCounts;
	var mClusterCentroids;  // the array
	var mClusterDimensions; // its dimensionality (a scalar)
	var mCurrentCluster;

	var mDrawCycleData; // True if it should draw the cycle data, false otherwise.
	var mDrawableClusterCycle; // Object handling the drawing of the cluster cycle

	var mPalette;
	var mFont;
	var mGlyphs;
	var mStatsHeight;
	var mcDelim;

	var mLoadedData;
	var mSynthCurves; // array of polylines, one for each clustered waveform

	// Constants
	classvar iRelearn = 0;
	classvar iPrelearned = 1;

	classvar iLearn = 0;
	classvar iDontLearn = 1;

	classvar iLoad = 0;
	classvar iUnload = 1;

	// Settings
	classvar nMinDb = -60;
	classvar nMaxDb = 0;

	classvar nMinHarmonics = 2;
	classvar nMaxHarmonics = 20;

	classvar nMinClusters = 2;
	classvar nMaxClusters = 20;

	classvar nDefaultStatsHeight = 125;

	*new { | view |
		^super.new.init(view);
	}

	init { | view |
		var gridYellow = Color.new(0.7, 0.7, 0);
		mView = view;
		this addDependant: VRPViewVRP.mAdapterUpdate;

		mViewClusterCentroids = UserView(mView, mView.bounds);
		mViewClusterCentroids
		.background_(Color.black)
		.drawFunc_{ this.drawCentroids(); };

		mViewClusterStats = UserView(mView, mView.bounds);
		mViewClusterStats
		.background_(Color.black)
		.drawFunc_{ | uv |
			if ( mDrawCycleData and: mPalette.notNil,
				{
					if (mSelected > 0, {
						mDrawableClusterCycle.index_(mSelected-1);
						mDrawableClusterCycle.draw(uv, mPalette.(mSelected-1));
						this.drawResynthEGG(uv, mSelected-1, 0);
					},{
						this.drawResynthEGG(uv, mCurrentCluster, mClusterCounts.size);
						mDrawableClusterCycle.index_(mCurrentCluster);
						mDrawableClusterCycle.draw(uv, mPalette.(mCurrentCluster));
					})
				}, {
					this.drawStats();
				}
			);
		};

		mDrawCycleData = false;
		mDrawableClusterCycle = DrawableClusterCycle(0, 0);

		mGridCentroids =
		DrawGrid( Rect(),
			GridLines(ControlSpec(-1.0, 1, units: "π")),
			GridLines(ControlSpec(nMinDb.asFloat, nMaxDb.asFloat, default: 0, units: "dB"))
		)
		.fontColor_(gridYellow)
		.gridColors_([gridYellow, gridYellow]);

		mPalette = nil;
		mClusterCounts = nil;
		mClusterCentroids = nil;
		mSynthCurves = List.newClear(nMaxClusters);
		mCurrentCluster = 0; // avoid nil, to prevent certain palette errors
		mFont = Font("Arial", 10, true, false, true);
		mStatsHeight = nDefaultStatsHeight;
		mStarted = false;
		mResetNow = false;
		mcDelim = VRPMain.cListSeparator ;	// column separator in CSV files - locale-dependent

		this.initMenu();

		// Click on a colored column to select that cluster
		// Click in the upper third of the window to display all clusters
		// Hold down Ctrl and click left or right on a column to shift the cluster order
		mViewClusterStats.mouseDownAction_{
			| uv, x, y, mod, buttonNumber |
			if ( mClusterCounts.notNil, {
				var idx;
				idx = ((x / uv.bounds.width) * mClusterCounts.size).asInteger;
				if ((mDrawCycleData.not and: mod.isCtrl), {
					switch (buttonNumber,
						0, { this.shiftorder(idx, -1) },
						1, { this.shiftorder(idx,  1) }
					);
				},{
					if ((y < (uv.bounds.height/3)) or: mDrawCycleData,
						{
							idx = 0;
							mSliderCluster.valueAction_(idx);
						},
						{
							mDrawableClusterCycle.index = idx;
							mSliderCluster.valueAction_((idx+1)/mClusterCounts.size);
						}
					);
					mDrawCycleData = mDrawCycleData.not;
				})
			});
		};

		mView.layout_(
			VLayout(
				[
					HLayout(
						[mButtonStart, stretch: 1],
						[mButtonLearn, stretch: 1],
						[mButtonReset, stretch: 1],
						[mButtonLoad, stretch: 1],
						[mButtonSave, stretch: 1]
					), stretch: 1
				],

				[
					HLayout(
						[mStaticTextClusters, stretch: 1],
						[mNumberBoxClusters, stretch: 1],
						[mStaticTextHarmonics, stretch: 1],
						[mNumberBoxHarmonics, stretch: 1],
						[mStaticTextCluster, stretch: 1],
						[mSliderCluster, stretch: 5]
					), stretch: 1
				],

				[
					HLayout(
						[mViewClusterCentroids, stretch: 1],
						[mViewClusterStats, stretch: 1]
					), stretch: 50
				] // Force the menu to take up as little space as possible!
			)
		);
	}

	initMenu {
		var static_font = Font(\Arial, 12);

		////////////////////////////////////////////////////////////////////

		mButtonStart = Button(mView, Rect())
		.enabled_(false)
		.states_([
			["Init: Relearn"],
			["Init: Pre-learned"]
		])
		.action_{ | btn |
			this.updateLoadedData;
			this.updateMenu;
		};

		/////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////

		mButtonLearn = Button(mView, Rect())
		.enabled_(false)
		.states_([
			["Learning: On"],
			["Learning: Off"]
		])
		.action_{ | btn |
			this.updateMenu;
		};

		/////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////

		mButtonReset = Button(mView, Rect())
		.states_([ 	["Reset Counts"]  ])
		.mouseDownAction_({ if ( mStarted, {
					// The server is started - so let it deal with the reset
					mResetNow = true;
		})})
		.action_{
			if ( mStarted.not, {
					// The server is not started - so lets deal with it locally.
					if ( mClusterCounts.notNil, {
						mClusterCounts.fill(0);
					});
				}
			);
		};

		/////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////

		mButtonLoad = Button(mView, Rect())
		.states_([
			["Load"],
			["Unload"]
		])
		.action_{ | btn |
			var load = btn.value == iUnload;
			if ( load,
				{
					this.loadClusterData;
				}, {
					mClusterCounts = nil;
					mClusterCentroids = nil;
				}
			);
			this.updateMenu;
		};

		/////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////

		mButtonSave = Button(mView, Rect())
		.enabled_(false)
		.states_([
			["Save"]
		])
		.action_{
			this.saveClusterData;
		};

		/////////////////////////////////////////////////////////////////////

		mStaticTextClusters = StaticText(mView, Rect(0, 0, 100, 0))
		.string_("Clusters:")
		.font_(static_font);
		mStaticTextClusters
		.fixedWidth_(mStaticTextClusters.sizeHint.width)
		.fixedHeight_(35)
		.stringColor_(Color.white);

		mNumberBoxClusters = NumberBox(mView, Rect(0, 0, 100, 35))
		.value_(5)							// This is the default value
		.clipLo_(2)
		.clipHi_(nMaxClusters)
		.step_(1)
		.scroll_step_(1)
		.fixedWidth_(30)
		.action_{
			this.updateMenu;
		};

		/////////////////////////////////////////////////////////////////////
		/////////////////////////////////////////////////////////////////////

		mStaticTextHarmonics = StaticText(mView, Rect(0, 0, 100, 0))
		.string_("Harmonics:")
		.font_(static_font);
		mStaticTextHarmonics
		.fixedWidth_(mStaticTextHarmonics.sizeHint.width)
		.fixedHeight_(35)
		.stringColor_(Color.white);

		mNumberBoxHarmonics = NumberBox(mView, Rect(0, 0, 100, 35))
		.value_(10)							// This is the default value
		.clipLo_(2)
		.clipHi_(nMaxHarmonics)
		.step_(1)
		.scroll_step_(1)
		.fixedWidth_(30)
		.action_{
			this.updateMenu;
		};

		/////////////////////////////////////////////////////////////////////

		mStaticTextCluster = TextField(mView, [0, 0, 100, 30]);
		mStaticTextCluster
		.resize_(4)
		.enabled_(false);

		mSliderCluster = Slider(mView, [0, 0, mView.bounds.width, 30]);
		mSliderCluster
		.resize_(4)
		.maxHeight_(23);

		/////////////////////////////////////////////////////////////////////

		mSelected = 0;
		this.updateMenu;
	}

	updateMenu {
		if ( mStarted.not, { // All disabled while it is started

			// Update the slider
			var req_step = 1 / mNumberBoxClusters.value;

			if ( mSliderCluster.step != req_step, {
				mSliderCluster
				.step_(req_step)
				.action_{
					mSelected = (mSliderCluster.value * mNumberBoxClusters.value).round.asInteger;

					// Update the cluster text
					mStaticTextCluster.string_(
						"Cluster: " ++
						if ( mSelected == 0, "All", mSelected.asString )
					);

					// Signal change of selected cluster
					this.changed(\selectCluster, mSliderCluster.value);
				};

				// Reset the slider to 0 if the slider since it is no longer valid
				mSliderCluster.valueAction_(0);
				mSelected = 0;
			});

			// Enable/Disable depending on current states
			mButtonLoad.enabled_(true); // Load is always available

			switch (mButtonLoad.value,
				iLoad, {
					mButtonStart
					.enabled_(false)
					.value_(iRelearn); // Must relearn without any prelearned data

					// Can update the # of harmonics or clusters when nothing is loaded
					mNumberBoxHarmonics
					.enabled_(true);
					mNumberBoxClusters
					.enabled_(true);
				},

				iUnload, {
					mButtonStart
					.enabled_(true); // May choose to use prelearned data or not

					// Cannot update the # of harmonics or clusters since we have data loaded
					mNumberBoxHarmonics
					.enabled_(false);
					mNumberBoxClusters
					.enabled_(false);
				}
			);

			switch (mButtonStart.value,
				iRelearn, {
					mButtonLearn
					.enabled_(false)
					.value_(iLearn); // Must learn with relearn active
				},

				iPrelearned, {
					mButtonLearn
					.enabled_(true); // May choose to continue learning or not

					// Cannot update the # of harmonics or clusters with prelearned data
					mNumberBoxHarmonics
					.enabled_(false);
					mNumberBoxClusters
					.enabled_(false);
				}
			);

			// Can only use reset while learning is on
			mButtonReset
			.enabled_( mButtonLearn.value == iLearn );

			// Cannot save without any data
			mButtonSave
			.enabled_( mButtonLoad.value == iUnload );
		});
	}

	saveClusterDataOld {  // to v1.4.2 - deprecated
		Dialog.savePanel({
			| path |
			if (path.endsWith(".csv").not) {
				path = path ++ "_clusters.csv";
			};
			File.use(path, "w", { | file |
				// Write the number of clusters and the dimensionality of the centroids on a single line
				var dim = mClusterDimensions;
				file << mNumberBoxClusters.value;
				file.put(mcDelim);
				file << dim;
				file.put($\r); file.nl;

				// Write the cluster counts on its own line first
				mClusterCounts do: { | count, i |
					file << count.asInteger;
					if (i < (mClusterCounts.size - 1)) {file.put(mcDelim)} ;
				};
				file.put($\r); file.nl;

				// Followed by each centroid on its own line
				mClusterCentroids do: { | centroid |
					centroid do: { | value, i |
						file << value;
						if (i < (centroid.size - 1)) {file.put(mcDelim)} ;
					};
					file.put($\r); file.nl;
				};
			});
		});
	}

	saveClusterData {  // from v1.4.3
		Dialog.savePanel({ 	| path |
			var nClusters = mNumberBoxClusters.value;
			var dim = mClusterDimensions ?? (mNumberBoxHarmonics.value * 3);
			if (path.endsWith(".csv").not) {
				path = path ++ "_clusters.csv";
			};
			File.use(path, "w", { | file |
				nClusters do: { |i|
					file << mClusterCounts[i].asInteger;
					file.put(mcDelim);
					mClusterCentroids[i] do: { | value, i |
						file << value;
						if (i < (dim - 1), {file.put(mcDelim)} );
					};
					file.put($\r); file.nl;
				}
			});
		});
	}

	loadClusterData {
		var cArray;
		var c, d, h;
		var counts;
		var centroids;
		Dialog.openPanel({ 	| path |
			var bNew;
			cArray = FileReader.read(path, skipEmptyLines: true, skipBlanks: true, delimiter: mcDelim);
			if (cArray[0].size < 4,
				{		// Old cluster format
					bNew = false;
						// Read the number of clusters and the dimensionality of the centroids
					c = cArray[0][0].asInteger;
					d = cArray[0][1].asInteger;
				}, { 	// new cluster format
					bNew = true;
					c = cArray.size;			// number of rows
					d = cArray[0].size - 1;		// number of columns, less one
				}
			);
			h = ((d/3) + 1).asInteger;

			try {
				// Small check for errors
				if ( c.inclusivelyBetween(nMinClusters, nMaxClusters).not
					or: h.inclusivelyBetween(nMinHarmonics, nMaxHarmonics).not,
					{
						Error("Bad input file, invalid # of clusters/harmonics").throw;
					}
				);

				if (bNew, {
					// Read the cluster counts
					mClusterCounts = c collect: { | i | cArray[i][0].asInteger };
					// Read the centroids
					mClusterCentroids = Array.fill2D(c, d, { arg row, col; cArray[row][col+1].asFloat });

				},	{
					// Read the cluster counts
					mClusterCounts = c collect: { | i | cArray[1][i].asInteger };
					// Read the centroids
					mClusterCentroids = Array.fill2D(c, d, { arg row, col; cArray[row+2][col].asFloat });
				});

				// Set the members
				mNumberBoxClusters.value = c;
				mNumberBoxHarmonics.value = h - 1;
				mGlyphs = (h - 2) collect: { | n | (n + 2).asString };
				mGlyphs = mGlyphs.add ($H.asString);

				mDrawCycleData = false;		// New clusters invalidate the old averaged cycles
				this.updateLoadedData;

			} {
				mClusterCounts = nil;
				mClusterCentroids = nil;
				mButtonLoad.value = iLoad; // Failed
				"Load CSV failed".postln;
			};
		}, {
			mButtonLoad.value = iLoad; // Cancelled
		});
	}

	updateLoadedData {
		if ( mButtonStart.value == iPrelearned,
			{
				// Should use a prelearned matrix - so add it to the settings
				mLoadedData = [mClusterCounts, mClusterCentroids];
			}, {
				// Shouldn't use a prelearned matrix - so set it to nil
				mLoadedData = nil;
			}
		);
	}

	reorder { arg newOrder;
		var tmp, bOK;
		bOK = true;
		if ((mStarted.not) and: (newOrder.class == Array) and: (newOrder.size == mClusterCounts.size),
			{
				newOrder.do { |elem, i| if (elem.class != Integer,  { bOK = false } )};
				if (bOK, {
					tmp = mClusterCounts[newOrder];
					mClusterCounts = tmp;
					tmp = mClusterCentroids[newOrder];
					mClusterCentroids = tmp;
					mDrawCycleData = false;		// New clusters invalidate the old averaged cycles
					this.updateLoadedData;
					this.changed(\reorderClusters, newOrder);
					}
				)
		})
	}

	// Shift cluster iCluster by nSteps (< 0 left, > 0 right)
	shiftorder { arg iCluster, nSteps;
		var nC, kC;
		var newOrder;
		nC = mClusterCounts.size;
		kC = (iCluster + nSteps).mod(nC);
		newOrder = (0..nC-1).swap(iCluster, kC);
		this.reorder(newOrder);
	}

	fetch { | settings |
		var cs = settings.cluster;
		cs.learnedData = mLoadedData;

		cs.learn = mButtonLearn.value == iLearn;
		cs.nHarmonics_(mNumberBoxHarmonics.value.asInteger);
		// This invokes a setter function which makes cs.nHarmonics one larger,
		// and also recomputes cs.nDimensions!
		cs.nClusters = mNumberBoxClusters.value.asInteger;

		if (settings.general.guiChanged, {
			mView.background_(settings.general.getThemeColor(\backPanel));
			mViewClusterCentroids.background_(settings.general.getThemeColor(\backGraph));
			mViewClusterStats.background_(settings.general.getThemeColor(\backGraph));
			[mStaticTextClusters, mStaticTextHarmonics].do ({ arg c;
				c.stringColor_(settings.general.getThemeColor(\panelText))
			})
		});
	}

	update { | data |
		var cd = data.cluster;
		var cs = data.settings.cluster;

		// Always need to grab the palette
		mPalette = cd.palette;

		// NOTE: We check if the server is started with mStarted, since we rather care
		// about not missing out on data at the end, than starting to grab data at the
		// first possible chance.
		if (mStarted, {
			// Grab the newly updated data
			mClusterCounts = cd.pointsInCluster;
			mClusterCentroids = cd.centroids;
			mNumberBoxHarmonics.value = cs.nHarmonics.asInteger - 1;
			mNumberBoxClusters.value = cs.nClusters.asInteger;
			mClusterDimensions = cs.nDimensions;
			mCurrentCluster = data.vrp.currentCluster ? 0;
			mDrawableClusterCycle.data = cd.cycleData;

			if ( mResetNow, {
				cd.resetNow = true;
			});
		});

		mResetNow = false;

		if (mStarted.not and: data.general.started, {
			var nGlyphs = cs.nHarmonics.asInteger - 1;
			mGlyphs = (nGlyphs-1) collect: { | n | (n + 2).asString };
			mGlyphs = mGlyphs.add ($H.asString);

			// Update the # of clusters/samples in DrawableClusterCycle
			// Just started the server - so forcefully disable all input controls except reset
			[
				mNumberBoxClusters,
				mNumberBoxHarmonics,
				mButtonStart,
				mButtonLearn,
				mButtonLoad,
				mButtonSave
			]
			do: { | x | x.enabled_(false); };

			mDrawableClusterCycle.count = cs.nClusters;
			mDrawableClusterCycle.samples = cs.nSamples;

			// Reset the mStatsHeight
			mStatsHeight = nDefaultStatsHeight;

			// Clear the list of drawn cycles
			mSynthCurves = List.newClear(nMaxClusters);
		});

		if (mStarted and: data.general.started.not, {
			// Just stopped the server
			cd.resetNow = false; // It shouldn't matter leaving it as true, but we do this for safety.
			mButtonLoad.value_(iUnload); // Have data since we just stopped the server
		});

		mStarted = data.general.started;
		this.updateLoadedData;
		this.updateMenu;
		mViewClusterCentroids.refresh;
		mViewClusterStats.refresh;
	}

	drawCentroids {
		var seq, bounds;

		// Draw the grid first!
		mGridCentroids.bounds_(
			mViewClusterCentroids.bounds
			.moveTo(0, 0)
			.insetAll(0, 0, 1, 1)
		);
		mGridCentroids.draw;

		// Draw the glyphs after!
		if (mClusterCentroids.notNil, {
			bounds = mGridCentroids.bounds;

			if (0 == mSelected, {
				seq = ((mNumberBoxClusters.value-1)..0);
			}, {
				seq = [mSelected - 1];
			});
			Pen.use{
				var nGlyphs = mNumberBoxHarmonics.value.asInteger;

				seq do: { | i |
					var cosX, sinX, x, y, xPix, yPix, color;
					color = mPalette.(i);
					nGlyphs do: { | j |
						cosX = mClusterCentroids[ i ][ j + nGlyphs ];
						sinX = mClusterCentroids[ i ][ j + (nGlyphs*2) ];
						// Un-weighting of cos and sin is not needed
						x = atan2(sinX, cosX);
						xPix = x.linlin(-pi, pi, 0, bounds.width);
						y = mClusterCentroids[ i ][ j ];
						yPix = y.linlin(nMinDb*0.1, 0, bounds.height, 0);  // -6 Bel or -60 dB
						mGlyphs[j].drawAtPoint(xPix@yPix, mFont, color);
					};
				};
				"1".drawAtPoint(bounds.width.half @ 0, mFont, Color.gray(0.7));
			};
		});
	}

	drawStats {
		if (mClusterCounts.notNil, {
			var rc, bounds, barWidth, fColor, max;
			bounds = mViewClusterStats.bounds;
			rc = Rect();
			barWidth = bounds.width / mNumberBoxClusters.value;

			// Update the virtual height of the stats view
			max = mClusterCounts.maxItem;
			while ( { mStatsHeight < max }, { mStatsHeight = mStatsHeight * 2; } );

			Pen.use{
				(mStatsHeight.asString + "cycles").drawAtPoint(0@0, mFont, Color.yellow(0.7));
				mNumberBoxClusters.value.do ({ | i |
					var xPix, yPix, count;
					count = mClusterCounts[i];
					xPix = i.linlin(0, mNumberBoxClusters.value, 0, bounds.width);
					yPix = count.linlin(0, mStatsHeight, 0, bounds.height);
					rc.set(xPix, bounds.height - yPix, barWidth, yPix);
					Pen.fillColor = mPalette.(i);
					Pen.fillRect(rc);
				});
			};
		});
	}

	drawResynthEGG { arg uView, clustNr, overlays;
		var shape, amps, phases, phase1, list;
		var errHeight = DrawableClusterCycle.errorSpace;
		var b = uView.bounds.moveTo(0,0);
		var nDeltas = mNumberBoxHarmonics.value.asInteger;
		var nSteps = 80;

		// First resynthesize the EGG curve for the current cluster only
		if (mClusterCounts.notNil && mClusterCentroids.notNil, {
			var phiStep = 2pi/nSteps;
			var iGain;
			amps = [1.0] ++ (nDeltas collect: { |i| (mClusterCentroids[clustNr][i]*10).dbamp } );

			// v1.23 (cos, sin) do not need to be un-weighted here, since atan2 is proportional
			phases = nDeltas collect: { |i| atan2(mClusterCentroids[clustNr][i + (nDeltas*2)],
				mClusterCentroids[clustNr][i + nDeltas]) };
			phase1 = phases[nDeltas-1];
			phases = [0.0] ++ phases + phase1;  // Reconstruct actual phases (not deltas)

			shape = FloatArray.fill(nSteps+1, { arg ix;
				var phi = ix * phiStep;
				var acc = 0.0;
				nDeltas.do { arg j; acc = acc + (amps[j] * cos((phi*(j+1)) + phases[j])) };
				acc
			} ).normalize;

			// Now save this curve into the list mSynthCurves
			mSynthCurves.put(clustNr, shape);
		});

		// Now draw the cycle curves in the list mSynthCurves, one per cluster
		if (overlays > 0, { list = (0..overlays-1) }, { list = [clustNr] } );
		list.do { arg index;
			if ((shape = mSynthCurves.at(index)).notNil, {
				Pen.use {
					var color = mPalette.(index);
					if (clustNr == index, {
						format("Normalized sum of % harmonics", nDeltas)
						.drawRightJustIn(Rect(0, 0, b.width, errHeight), mFont, color);
						Pen.width = 0.02
					},{
						Pen.width = 0.01
					});
					Pen.strokeColor = color ? Color.grey;
					Pen.translate(b.left, b.top);
					Pen.translate( 0.0, errHeight );
					Pen.scale( b.width / nSteps, errHeight - b.height);
					Pen.translate( 0, -1);

					// Draw the lines
					Pen.moveTo(0@shape[0]);
					shape do: { |v, i| Pen.lineTo(i@v) };
					Pen.stroke;
				};
			});
		}
	}

	close {
		this removeDependant: VRPViewVRP.mAdapterUpdate;
	}
}

