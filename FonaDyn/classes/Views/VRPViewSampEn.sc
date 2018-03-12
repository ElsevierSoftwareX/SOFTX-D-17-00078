// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //

VRPViewSampEn {
	// Views
	var mView;
	var mViewScope;
	var mScopeViewer;

	// Controls
	var mStaticTextAmplitude;
	var mStaticTextPhase;
	var mStaticTextTolerance;
	var mStaticTextWindowSize;
	var mStaticTextSequenceLength;
	var mStaticTextHarmonics;

	var mNumberBoxToleranceAmplitude;
	var mNumberBoxWindowSizeAmplitude;
	var mNumberBoxSequenceLengthAmplitude;
	var mNumberBoxHarmonicsAmplitude;

	var mNumberBoxTolerancePhase;
	var mNumberBoxWindowSizePhase;
	var mNumberBoxSequenceLengthPhase;
	var mNumberBoxHarmonicsPhase;

	// Constats
	classvar nMinSampleEntropy = -0.01; // Minimum sample entropy point written

	// Settings


	*new { | view |
		^super.new.init(view);
	}

	init { | view |
		mView = view;

		this.initMenu;

		// Create the scope viewer
		value {
			var max1 =
			mNumberBoxHarmonicsAmplitude.value.asInteger *
			SampleEntropyFromBus.upperBounds(
				mNumberBoxWindowSizeAmplitude.value.asInteger,
				mNumberBoxSequenceLengthAmplitude.value.asInteger
			);

			var max2 =
			mNumberBoxHarmonicsPhase.value.asInteger *
			SampleEntropyFromBus.upperBounds(
				mNumberBoxWindowSizePhase.value.asInteger,
				mNumberBoxSequenceLengthPhase.value.asInteger
			);

			var maxSampEn = max1 + max2;

			mScopeViewer = ScopeViewer(mView,
				ControlSpec(-1, 0, units: "s"),
				ControlSpec(nMinSampleEntropy, maxSampEn, units: "SampEn")
			);
		};
		mScopeViewer.view
		.background_(Color.black);

		// Force a grid redraw when any SampEn-related parameter is changed
		mView.allChildren do: { |v| if (v.class == NumberBox, { v.addAction( { mScopeViewer.refresh } ) }) } ;

		mView.layout_(
			VLayout(
				[
					HLayout(
						[
							GridLayout.rows(
								[
									nil,
									mStaticTextTolerance,
									mStaticTextWindowSize,
									mStaticTextSequenceLength,
									mStaticTextHarmonics
								],
								[
									mStaticTextAmplitude,
									mNumberBoxToleranceAmplitude,
									mNumberBoxWindowSizeAmplitude,
									mNumberBoxSequenceLengthAmplitude,
									mNumberBoxHarmonicsAmplitude
								],
								[
									mStaticTextPhase,
									mNumberBoxTolerancePhase,
									mNumberBoxWindowSizePhase,
									mNumberBoxSequenceLengthPhase,
									mNumberBoxHarmonicsPhase
								]
							), stretch: 1
						],
						[nil, stretch: 10] // Force the menu to the left
					)
				],
				[ mScopeViewer.view, stretch: 10]
			)
		);
		mView.layout.spacing_(2);
	}

	initMenu {
		var static_font = Font(\Arial, 11);
		var general_font = Font(\Arial, 12);

		mStaticTextAmplitude = StaticText(mView, Rect())
		.string_("Levels:")
		.font_(general_font);
		mStaticTextAmplitude
		.fixedWidth_(mStaticTextAmplitude.sizeHint.width)
		.fixedHeight_(30)
		.stringColor_(Color.white);

		mStaticTextPhase = StaticText(mView, Rect())
		.string_("Phases:")
		.font_(general_font);
		mStaticTextPhase
		.fixedWidth_(mStaticTextPhase.sizeHint.width)
		.fixedHeight_(30)
		.stringColor_(Color.white);

		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////

		mStaticTextTolerance = StaticText(mView, Rect())
		.string_("Tolerance")
		.font_(static_font);
		mStaticTextTolerance
		.fixedWidth_(mStaticTextTolerance.sizeHint.width)
		.fixedHeight_(30)
		.stringColor_(Color.white)
		.align_(\center);

		mNumberBoxToleranceAmplitude = NumberBox(mView, Rect())
		.value_(0.2)
		.clipLo_(0)
		.step_(0.1)
		.scroll_step_(0.1)
		.fixedWidth_(50);

		mNumberBoxTolerancePhase = NumberBox(mView, Rect())
		.value_(0.4)
		.clipLo_(0)
		.step_(0.1)
		.scroll_step_(0.1)
		.fixedWidth_(50);

		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////

		mStaticTextWindowSize = StaticText(mView, Rect())
		.string_("Window")
		.font_(static_font);
		mStaticTextWindowSize
		.fixedWidth_(mStaticTextWindowSize.sizeHint.width)
		.fixedHeight_(30)
		.stringColor_(Color.white)
		.align_(\center);

		mNumberBoxWindowSizeAmplitude = NumberBox(mView, Rect())
		.value_(10)
		.clipLo_(2)
		.step_(1)
		.scroll_step_(1)
		.fixedWidth_(50)
		.action_ { | nb |
			// Note that the sequence length cannot be larger than or equal to the window size.
			if (mNumberBoxSequenceLengthAmplitude.value > (nb.value - 1), {
				mNumberBoxSequenceLengthAmplitude.valueAction_( nb.value - 1 );
			});
			mNumberBoxSequenceLengthAmplitude.clipHi_(nb.value - 1);
		};

		mNumberBoxWindowSizePhase = NumberBox(mView, Rect())
		.value_(10)
		.clipLo_(2)
		.step_(1)
		.scroll_step_(1)
		.fixedWidth_(50)
		.action_ { | nb |
			// Note that the sequence length cannot be larger than or equal to the window size.
			if (mNumberBoxSequenceLengthPhase.value > (nb.value - 1), {
				mNumberBoxSequenceLengthPhase.valueAction_( nb.value - 1 );
			});
			mNumberBoxSequenceLengthPhase.clipHi_(nb.value - 1);
		};

		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////

		mStaticTextSequenceLength = StaticText(mView, Rect())
		.string_("Length")
		.font_(static_font);
		mStaticTextSequenceLength
		.fixedWidth_(mStaticTextSequenceLength.sizeHint.width)
		.fixedHeight_(30)
		.stringColor_(Color.white)
		.align_(\center);

		mNumberBoxSequenceLengthAmplitude = NumberBox(mView, Rect())
		.value_(1)
		.clipLo_(1)
		.clipHi_(mNumberBoxWindowSizeAmplitude.value - 1)
		.step_(1)
		.scroll_step_(1)
		.fixedWidth_(50);

		mNumberBoxSequenceLengthPhase = NumberBox(mView, Rect())
		.value_(1)
		.clipLo_(1)
		.clipHi_(mNumberBoxWindowSizePhase.value - 1)
		.step_(1)
		.scroll_step_(1)
		.fixedWidth_(50);

		////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////

		mStaticTextHarmonics = StaticText(mView, Rect())
		.string_("Harmonics")
		.font_(static_font);
		mStaticTextHarmonics
		.fixedWidth_(mStaticTextHarmonics.sizeHint.width)
		.fixedHeight_(30)
		.stringColor_(Color.white)
		.align_(\center);

		mNumberBoxHarmonicsAmplitude = NumberBox(mView, Rect())
		.value_(2)
		.clipLo_(1)
		.clipHi_(20) // We don't know how many are actually available.
		.step_(1)
		.scroll_step_(1)
		.fixedWidth_(50);

		mNumberBoxHarmonicsPhase = NumberBox(mView, Rect())
		.value_(2)
		.clipLo_(1)
		.clipHi_(20) // We don't know how many are actually available.
		.step_(1)
		.scroll_step_(1)
		.fixedWidth_(50);
	}

	enableInterface { | enable |
		[
			mNumberBoxToleranceAmplitude,
			mNumberBoxWindowSizeAmplitude,
			mNumberBoxSequenceLengthAmplitude,
			mNumberBoxHarmonicsAmplitude,
			mNumberBoxTolerancePhase,
			mNumberBoxWindowSizePhase,
			mNumberBoxSequenceLengthPhase,
			mNumberBoxHarmonicsPhase
		]
		do: { | ctrl | ctrl.enabled_(enable); };
	}

	layout { ^mView; }

	fetch { | settings |
		var ss = settings.sampen;
		var gs = settings.general;

		ss.amplitudeTolerance = mNumberBoxToleranceAmplitude.value;
		ss.amplitudeWindowSize = mNumberBoxWindowSizeAmplitude.value;
		ss.amplitudeSequenceLength = mNumberBoxSequenceLengthAmplitude.value;
		ss.amplitudeHarmonics = mNumberBoxHarmonicsAmplitude.value;

		ss.phaseTolerance = mNumberBoxTolerancePhase.value;
		ss.phaseWindowSize = mNumberBoxWindowSizePhase.value;
		ss.phaseSequenceLength = mNumberBoxSequenceLengthPhase.value;
		ss.phaseHarmonics = mNumberBoxHarmonicsPhase.value;

		// Update the maximum SampEn measurement:
		value {
			var max1 =
			ss.amplitudeHarmonics *
			SampleEntropyFromBus.upperBounds(
				ss.amplitudeWindowSize,
				ss.amplitudeSequenceLength
			);

			var max2 =
			ss.phaseHarmonics *
			SampleEntropyFromBus.upperBounds(
				ss.phaseWindowSize,
				ss.phaseSequenceLength
			);

			var maxSampEn = max1 + max2;
			mScopeViewer.vspec = ControlSpec(nMinSampleEntropy, maxSampEn, units: "SampEn")
		};

		if (settings.general.guiChanged, {
			// Set the theme colors
			mView.background_(gs.getThemeColor(\backPanel));
			mView.allChildren.do ({ arg c;
				if (c.isKindOf(StaticText), { c.stringColor_(gs.getThemeColor(\panelText)) })}
			);
			mScopeViewer.view.background_(gs.getThemeColor(\backGraph));
			mScopeViewer.gridFontColor_(gs.getThemeColor(\panelText));
			this.updateGraphOnly;
		});
	}

	update { | data |
		var scopeData = data.scope.sampen;
		var duration = data.settings.scope.duration;

		this.enableInterface(data.general.started.not);

		if ( duration != mScopeViewer.hspec.range, {
			mScopeViewer.hspec = ControlSpec(duration.neg, 0, units: "s");
		});

		if ( scopeData.notNil and: data.general.started, {
			mScopeViewer.update(scopeData.first, Array.with( scopeData.last ), (data.general.pause == 2));
		});

		if ( data.general.starting, {
			mScopeViewer.reset;
		});

		if ( data.general.stopping, {
			mScopeViewer.stop;
		});
	}

	updateGraphOnly {
		mScopeViewer.refresh;
	}

	close {
	}
}