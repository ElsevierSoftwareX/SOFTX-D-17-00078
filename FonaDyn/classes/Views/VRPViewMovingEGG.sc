// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPViewMovingEGG {
	var mView;

	var mButtonNormalize;
	var mStaticTextCount;
	var mNumberBoxCount;
	var mStaticTextSamples;
	var mNumberBoxSamples;
	var mStaticTextMethod;
	var mUV;
	var mDMEGG;

	*new { | view |
		^super.new.init(view);
	}

	init { | view |
		var static_font = Font(\Arial, 12);

		mView = view;

		mButtonNormalize = Button(mView, Rect())
		.states_([
			["Normalize: Off"],
			["Normalize: On "]
		])
		.value_(1);

		///////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////

		mStaticTextCount = StaticText(mView, Rect())
		.string_("Count:")
		.font_(static_font);
		mStaticTextCount
		.fixedWidth_(mStaticTextCount.sizeHint.width)
		.fixedHeight_(35)
		.stringColor_(Color.white);

		mNumberBoxCount = NumberBox(mView, Rect())
		.value_(5)
		.clipLo_(1)
		.clipHi_(50)
		.step_(1)
		.scroll_step_(1)
		.fixedWidth_(30);

		///////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////

		mStaticTextSamples = StaticText(mView, Rect())
		.string_("Samples:")
		.font_(static_font);
		mStaticTextSamples
		.fixedWidth_(mStaticTextSamples.sizeHint.width)
		.fixedHeight_(35)
		.stringColor_(Color.white);

		mNumberBoxSamples = NumberBox(mView, Rect())
		.value_(80)
		.clipLo_(1)
		.clipHi_(200)
		.step_(1)
		.scroll_step_(1)
		.fixedWidth_(30);

		mStaticTextMethod = StaticText(mView, Rect())
		.string_("ΦΛ")
		.align_(\right)
		.font_(static_font.boldVariant);
		mStaticTextMethod
		.fixedWidth_(mStaticTextMethod.sizeHint.width)
		.fixedHeight_(35)
		.stringColor_(Color.white);

		///////////////////////////////////////////////////////////////
		///////////////////////////////////////////////////////////////

		mUV = UserView(mView, Rect())
		.background_(Color.white)
		.drawFunc_{
			if (mDMEGG.notNil, {
				mDMEGG.draw(mUV);
			});
		};

		mView.layout_(
			VLayout(
				[
					HLayout(
						[mButtonNormalize, stretch: 1],
						[mStaticTextCount, stretch: 1],
						[mNumberBoxCount, stretch: 1],
						[mStaticTextSamples, stretch: 1],
						[mNumberBoxSamples, stretch: 1],
						[mStaticTextMethod, stretch: 1, align: \right],
						[nil, stretch: 8]
					),
				stretch: 1],
				[mUV, stretch: 8]
			)
		);
	}

	fetch { | settings |
		var ss = settings.scope;
		var gs = settings.general;

		ss.normalize = mButtonNormalize.value;

		ss.movingEGGCount = mNumberBoxCount.value;
		ss.movingEGGSamples = mNumberBoxSamples.value;

		if (settings.general.guiChanged, {
			mView.background_(gs.getThemeColor(\backPanel));
			mView.allChildren do: ({ arg c;
				if (c.isKindOf(StaticText), { c.stringColor_(gs.getThemeColor(\panelText)) })}
			);
		});
	}

	update { | data |
		var sd = data.scope;
		var gd = data.general;
		var s = data.settings;
		var ss = s.scope;

		if (gd.starting and: mDMEGG.isNil, {
			mDMEGG = DrawableMovingEGG(ss.movingEGGCount, ss.movingEGGSamples, ss.normalize);
		});

		if (gd.stopping and: mDMEGG.notNil, {
			mDMEGG = nil;
		});

		if (gd.started and: mDMEGG.notNil, {
			mDMEGG.data = sd.movingEGGData;
		});

		mStaticTextMethod.string_(if (s.csdft.method == VRPSettingsCSDFT.methodPhasePortrait, "Φ", "Λ"));

		[
			mButtonNormalize,
			mNumberBoxSamples,
			mNumberBoxCount
		]
		do: { | x | x.enabled_(gd.started.not); };

		mUV.refresh;
	}

	close {}
}