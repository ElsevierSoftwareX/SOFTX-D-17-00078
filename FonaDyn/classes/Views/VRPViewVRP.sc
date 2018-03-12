// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //

VRPViewVRP{
	// Views
	var mView;
	var mUserViewMatrix;
	var mUserViewMatrixBack;
	var mUserViewCursor;
	var mUserViewSampEn;
	var mDrawableSparseMatrix;
	var mDrawableSparseMatrixBack;
	var mDrawableSparseArrowMatrix;
	var mGrid;
	var mGridLinesMIDI, mGridLinesHz;
	var mGridHorzGridSelectHz;

	// Controls
	var mDropDownType;
	var mSliderCluster;
	var mStaticTextCluster;

	var mButtonShowSampEnArrows; // Show/Hide SampEn arrows
	var mStaticTextSampEnLimit;
	var mNumberBoxSampEnLimit; // Limit for when SampEn arrows are generated
	var mButtonSaveVRPdata;
	var mButtonSaveVRPimage;
	var mStaticTextInfo;

	// Entire VRP data
	var mVRPdata;

	// States
	var mnClusters;
	var mSelected;
	var mbFullRedrawNeeded;
	var mCursorColor;
	var mCursorRect;
	var mCellCount;

	var mNewClusterOrder;
	classvar <mAdapterUpdate;


	// Constants
	classvar iDensity = 0,
	         iClarity = 1,
	         iCrestFactor = 2,
			 iEntropy = 3,
	         iClusters = 4;

	classvar mInfoTexts = #[
		"Darkest: >100 cycles",
		"Threshold: ",
		"Red: >= 12 dB",
		"Most brown: >20",
		"Whiter: more overlap",
		"Most color: >50 cycles" ];


	classvar iCursorWidth = 9;
	classvar iCursorHeight = 9;

	*new { | view |
		^super.new.init(view);
	}

	fullRefresh { arg b;
		mbFullRedrawNeeded = b;
		^b;
	}

	invalidate {
		this.fullRefresh(true);
		if (mDrawableSparseMatrix.notNil,     { mDrawableSparseMatrix.invalidate });
		if (mDrawableSparseMatrixBack.notNil, { mDrawableSparseMatrixBack.invalidate });
		if (mUserViewMatrix.notNil,           { mUserViewMatrix.clearDrawing });
		if (mUserViewMatrixBack.notNil,       { mUserViewMatrixBack.clearDrawing });
	}

	init { | view |
		var minFreq, maxFreq;

		mView = view;
		mView.background_(VRPMain.panelColor);

		this.invalidate;
		mView.onResize_( { this.invalidate} );

		mCellCount = 0;
		mNewClusterOrder = nil;

		mGridHorzGridSelectHz = false;
		minFreq = VRPDataVRP.nMinMIDI;
		maxFreq = VRPDataVRP.nMaxMIDI;
		mGridLinesMIDI = GridLines(ControlSpec(minFreq, maxFreq, warp: \lin, units: "MIDI"));
		mGridLinesHz   = GridLines(ControlSpec(minFreq.midicps, maxFreq.midicps, warp: \exp, units: "Hz"));

		mGrid = DrawGrid(
			Rect(),
			mGridLinesMIDI,
			GridLines(ControlSpec(VRPDataVRP.nMinSPL,  VRPDataVRP.nMaxSPL, units: "dB"))
		);

		mGrid.fontColor_(Color.gray);

		mUserViewMatrixBack = UserView(mView, mView.bounds);
		mUserViewMatrixBack
		.clearOnRefresh_(false)
		.background_(Color.white)
		.drawFunc_{ | uv |
			var b = uv.bounds.moveTo(0, 0);
			if (mbFullRedrawNeeded, {
				mGrid.bounds_(b);
				mGrid.draw;			// draws grid behind "back" matrix
			});
			if (mDrawableSparseMatrixBack.notNil, {
				Pen.use {
					// Flip the drawing of the matrix vertically, since the y-axis is flipped in the grid
					Pen.translate(0, b.height);
					Pen.scale(1, -1);
					mDrawableSparseMatrixBack.draw(uv);
				};
			});
		} ;

		mUserViewMatrix = UserView(mView, mView.bounds);
		mUserViewMatrix
		.clearOnRefresh_(false)
		.acceptsMouse_(true)
		.drawFunc_{ | uv |
			var b = uv.bounds.moveTo(0, 0);
			Pen.use {
				// Flip the drawing of the matrix vertically, since the y-axis is flipped in the grid
				Pen.translate(0, b.height);
				Pen.scale(1, -1);
				if (mDrawableSparseMatrix.notNil, { mDrawableSparseMatrix.draw(uv) });
			};
		};

		mUserViewMatrix.mouseDownAction_({
			| uv, x, y, m, bn |
			if (bn ==  1,    {  // only on right-click
				mGridHorzGridSelectHz = mGridHorzGridSelectHz.not;
				mGrid.horzGrid_(if (mGridHorzGridSelectHz, { mGridLinesHz }, { mGridLinesMIDI } ));
				this.invalidate;
			});
		});

		mUserViewSampEn = UserView(mView, mView.bounds);
		mUserViewSampEn
		.acceptsMouse_(false)
		.drawFunc_{ | uv |
			if ( mDrawableSparseArrowMatrix.notNil, {
				var b = uv.bounds;
				Pen.use {
					// Flip the drawing of the matrix vertically, since the y-axis is flipped in the grid
					Pen.translate(0, b.height);
					Pen.scale(1, -1);
					mDrawableSparseArrowMatrix.draw(uv, Color.black);
				};
			});
		};

		mUserViewCursor = UserView(mView, mView.bounds);
		mUserViewCursor
		.acceptsMouse_(false)
		.drawFunc_{ | uv |
			if ( mCursorRect.notNil, {
				Pen.use {
					Pen.fillColor = mCursorColor;
					Pen.strokeColor = Color.black;
					Pen.fillRect(mCursorRect);
					Pen.strokeRect(mCursorRect);
				};
			});
			this.fullRefresh(false);   // Everything has now been drawn
		};

		this.initMenu();

		mView.layout_(
			VLayout(
				[
					HLayout(
						[mButtonShowSampEnArrows, stretch: 1],
						[mStaticTextSampEnLimit, stretch: 1],
						[mNumberBoxSampEnLimit, stretch: 1],
						[10, stretch: 5],
						[mButtonSaveVRPdata, stretch: 1],
						[mButtonSaveVRPimage, stretch: 1],
						[nil, stretch: 50]  // Force the controls to take up as little space as possible
					), stretch: 1
				],

				[
					HLayout(
						[mDropDownType, stretch: 1],
						[mStaticTextCluster, stretch: 1],
						[mSliderCluster, stretch: 5],
						[mStaticTextInfo, stretch: 1]
					), stretch: 1
				],

				[
					StackLayout(
						mUserViewCursor,
						mUserViewSampEn,
						mUserViewMatrix,
						mUserViewMatrixBack
					).mode_(\stackAll) // Draw mUserViewCursor on top of mMatrixViewer.view

					, stretch: 50 // Force the menu to take up as little space as possible!
				]

			)
		);

		mAdapterUpdate =
		{ | menu, what, newValue |
			if (what == \selectCluster,
				{ mSliderCluster.value_(newValue);  }
			);
			if (what == \reorderClusters,
				{ mNewClusterOrder = newValue }
			);
			this.invalidate;
		};

		this.updateView;
	}

	initMenu {
		var static_font = Font(\Arial, 12);

		mButtonShowSampEnArrows = Button(mView, Rect());
		mButtonShowSampEnArrows
		.states_([
			["SampEn Arrows: Off"],
			["SampEn Arrows: On "]
		]);

		mStaticTextSampEnLimit = StaticText(mView, Rect())
		.string_("Threshold:")
		.font_(static_font);
		mStaticTextSampEnLimit
		.fixedWidth_(mStaticTextSampEnLimit.sizeHint.width)
		.stringColor_(Color.white);

		mNumberBoxSampEnLimit = NumberBox(mView, Rect())
		.clipLo_(0.1)
		.step_(0.1)
		.scroll_step_(0.1)
		.value_(1);

		mButtonShowSampEnArrows
		.action_({ | btn |
			var show = btn.value == 1;
			mUserViewSampEn.visible_(show);
			mStaticTextSampEnLimit.visible_(show);
			mNumberBoxSampEnLimit.visible_(show);
		})
		.valueAction_(0)
		.fixedHeight_(mNumberBoxSampEnLimit.sizeHint.height);

		mButtonSaveVRPdata = Button(mView, Rect());
		mButtonSaveVRPdata
		.states_([["Save VRP data"]])
		.action_( { |btn|
			if (mVRPdata.notNil, { mVRPdata.saveVRPdata; } );
		})
		.enabled_(false);

		mButtonSaveVRPimage = Button(mView, Rect());
		mButtonSaveVRPimage
		.states_([["Save VRP image"]])
		.action_( { |btn|  this.writeImage()} );

		mDropDownType = PopUpMenu(mView, [0, 0, 100, 30]);
		mDropDownType
		.items_([
			"Density",
			"Clarity",
			"Crest Factor",
			"Max Entropy",
			"Clusters"
		])
		.action_{
			this.invalidate;
			this.updateView();
		}
		.resize_(4);

		mStaticTextCluster = TextField(mView, [0, 0, 100, 30]);
		mStaticTextCluster
		.fixedWidth_(100)
		.enabled_(false);

		mSliderCluster = Slider(mView, [0, 0, mView.bounds.width, 30]);
		mSliderCluster
		.maxHeight_(24)
		.resize_(4);

		mStaticTextInfo = StaticText(mView, Rect())
		.string_("Info")
		.align_(\right)
		.font_(static_font.boldVariant);
		mStaticTextInfo
		.fixedWidth_(150)
		.fixedHeight_(35);

		mnClusters = 5;
		mSelected = 0;
	}

	updateView {
		var is_clusters = iClusters == mDropDownType.value;
		var infoStr;

		// Update the slider step
		mSliderCluster.step_(1 / mnClusters);
		mSliderCluster.action_{
			mSelected = (mSliderCluster.value * mnClusters).round(0.01).asInteger;
			this.invalidate;
			this.updateView();
		};

		if ( (mSliderCluster.value * mnClusters).round(0.01).asInteger != mSelected, {
			mSelected = (mSliderCluster.value * mnClusters).round(0.01).asInteger;
		});

		// Show or hide the cluster controls
		mStaticTextCluster.visible_(is_clusters);
		mSliderCluster.visible_(is_clusters);

		// Update the cluster text
		mStaticTextCluster.string_(
			"Cluster: " ++
			if ( mSelected == 0, "All", mSelected.asString )
		);

		// Update the info text for the current display
		switch (mDropDownType.value,
			iDensity, {
				mStaticTextInfo.stringColor_(Color.grey);
				infoStr = mInfoTexts[iDensity];
			},

			iCrestFactor, {
				mStaticTextInfo
				.stringColor_(Color.red);
				infoStr = mInfoTexts[iCrestFactor];
			},

			iClarity, {
				mStaticTextInfo
				.stringColor_(Color.green(0.5));
				infoStr = mInfoTexts[iClarity] + VRPDataVRP.clarityThreshold.asString;
			},

			iEntropy, {
				mStaticTextInfo
				.stringColor_(Color.new255(165, 42, 42));
				infoStr = mInfoTexts[iEntropy];
			},

			iClusters, {
				if (mSelected == 0,
					{ mStaticTextInfo
					.stringColor_(Color.white);
					infoStr = mInfoTexts[iClusters];
					},
					{ mStaticTextInfo
					.stringColor_(VRPDataCluster.palette(mnClusters).value(mSelected-1));
					infoStr = mInfoTexts[iClusters+1];
					}
				)
			},
			{ infoStr = "" }
		);
		if (mCellCount > 0, { infoStr = mCellCount.asString + "cells" } );
		mStaticTextInfo.string_(infoStr);
	}

	fetch { | settings |
		settings.sampen.limit = mNumberBoxSampEnLimit.value;
		if (settings.general.guiChanged, {
			mView.background_(settings.general.getThemeColor(\backPanel));
			mStaticTextSampEnLimit.stringColor_(settings.general.getThemeColor(\panelText));
		});
	}

	update { | data |
		var vrpd;
		var sd;
		var palette;

		if (mNewClusterOrder.notNil, {
			data.vrp.reorder(mNewClusterOrder);
			mNewClusterOrder = nil;
		} );
		vrpd = data.vrp;
		sd = data.sampen;
		palette = data.cluster.palette;
		mnClusters = data.settings.cluster.nClusters;
		this.updateView;

		if (data.general.stopping, {
			mButtonSaveVRPdata.enabled = true;
			mVRPdata = vrpd; // Remember for saving
		}); // Enable if stopping

		if (data.general.starting, {
			mButtonSaveVRPdata.enabled = false; // Disable when starting
			mCellCount = 0;
			this.invalidate;					// Clear the old graph
		});

		// Update the graph depending on the type selected in the dropdown menu
		mDrawableSparseMatrixBack = nil;
		mDrawableSparseMatrix.notNil.if { mDrawableSparseMatrix.mbActive_(false) };
		switch (mDropDownType.value,
			iDensity, {
				mDrawableSparseMatrix = vrpd.density;
			},

			iClarity, {
				mDrawableSparseMatrix = vrpd.clarity;
			},

			iCrestFactor, {
				mDrawableSparseMatrix = vrpd.crest;
			},

			iEntropy, {
				mDrawableSparseMatrix = vrpd.entropy;
			},

			iClusters, {
				if ( mSliderCluster.value == 0,
					{
						// View all clusters
						mDrawableSparseMatrix = vrpd.mostCommonCluster;
					}, {
						// View specific cluster
						var idx = ( (mSliderCluster.value * mnClusters) - 1 ).round.asInteger;
						mDrawableSparseMatrix = vrpd.clusters[idx];
						mDrawableSparseMatrixBack = vrpd.density;
						mDrawableSparseMatrixBack.notNil.if {  mDrawableSparseMatrixBack.mbActive_(true) };
					}
				);
			}
		); // End switch

		mDrawableSparseMatrix.notNil.if {
			mDrawableSparseMatrix.mbActive_(true);
//			mCellCount = mDrawableSparseMatrix.size;
		};
		mUserViewMatrixBack.refresh;
		mUserViewMatrix.refresh;

		// Update the cursor
		if (vrpd.currentAmplitude.notNil and: vrpd.currentFrequency.notNil and: vrpd.currentClarity.notNil, {
			var idx_midi = VRPDataVRP.frequencyToIndex( vrpd.currentFrequency );
			var idx_spl = VRPDataVRP.amplitudeToIndex( vrpd.currentAmplitude );

			var px = VRPDataVRP.frequencyToIndex( vrpd.currentFrequency, mUserViewCursor.bounds.width );
			var py = mUserViewCursor.bounds.height - 1 - // Flip vertically
			VRPDataVRP.amplitudeToIndex( vrpd.currentAmplitude, mUserViewCursor.bounds.height );

			mCursorRect = Rect.aboutPoint(
				px@py,
				iCursorWidth.half.asInteger,
				iCursorHeight.half.asInteger
			);

			// Update the cursor depending on the type selected in the dropdown menu
			if (mDropDownType.value == iClusters,
				{mCursorColor = palette.(vrpd.currentCluster ? 0)},  // testing for nil just in case
				{mCursorColor = VRPDataVRP.paletteCursor.( vrpd.currentClarity )}
			);

			mUserViewCursor.refresh;
		});

		if ( data.general.started.not,
			{
			mCursorRect = nil;
			}
		);

		// Update the points
		mDrawableSparseArrowMatrix = sd.sampenPoints;
		mUserViewSampEn.refresh;
	}

	writeImage {
		var rect = (mDropDownType.bounds union: mUserViewMatrix.bounds).insetBy(-5);
		var iTotal = Image.fromWindow(mView, rect);
		var tmpWnd = iTotal.plot(bounds: rect.moveTo(200,200), freeOnClose:false, showInfo: false);
		var str = format("Supported image file formats:\n % ", Image.formats);
		str.postln;
		tmpWnd
		.setInnerExtent(rect.width, rect.height)
		.onClose_({
			Dialog.savePanel({ arg path;
				iTotal.write(path, format: nil);
				("Image saved to" + path).postln;
				iTotal.free;
			} );
		})
		.front;
	}

	close {
	}
}



