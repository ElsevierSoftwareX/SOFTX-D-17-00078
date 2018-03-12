// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPSettingsDialog {
	var mDialog, mView;
	var mStaticTextProgramVersion;
	var mStaticTextProgramLicence;
	var mStaticTextCycleSeparationMethod;
	var mButtonCycleSeparationMethod;
	var mStaticTextClarityThreshold;
	var mNumberBoxClarityThreshold;
	var mCheckBoxCalibrationTone;
	var mCheckBoxKeepInputName;
	var mCheckBoxWriteGates;
	var mStaticTextExtraChannels;
	var mEditTextExtraChannels;
	var mStaticTextColorTheme;
	var mListColorThemes;

	var mButtonOK, mButtonCancel;

	*new { | parentMenu |
		^super.new.init(parentMenu);
	}

	init { | parentMenu |
		var static_font = Font(\Arial, 12);

		mDialog = Window.new("FonaDyn settings", resizable: false);
		mView = mDialog.view;
		mView.background_( Color.grey(0.65) );

		mStaticTextProgramVersion
		= StaticText.new(mView, Rect())
		.string_("Program version:" + VRPMain.mVersion.asString);

		mStaticTextProgramLicence
		= StaticText.new(mView, Rect())
		.string_("Distributed under EUPL v1.2.\n(click to read)" )
		.align_(\right)
		.stringColor_(Color.blue)
		.fixedWidth_(150)
		.mouseDownAction_( { ~gLicenceLink.openOS } );  // global link defined in VRPMain.sc

		mStaticTextCycleSeparationMethod
		= StaticText.new(mView, Rect())
		.string_("Cycle separation method:")
		.align_(\right);

		mButtonCycleSeparationMethod
		= Button(mView, Rect())
		.states_([["Phase tracker Φ"], ["Peak follower Λ"]])
		.value_(parentMenu.oldSettings.csdft.method);

		mStaticTextClarityThreshold
		= StaticText.new(mView, Rect())
		.string_("Clarity threshold:")
		.align_(\right);

		mNumberBoxClarityThreshold
		= NumberBox(mView, Rect())
		.clipLo_(0.5)
		.clipHi_(1.0)
		.step_(0.01)
		.scroll_step_(0.01)
		.value_(VRPDataVRP.clarityThreshold);

		mCheckBoxCalibrationTone
		= CheckBox(mView, Rect(), "Play calibration tone on 2nd output when recording")
		.value_(parentMenu.oldSettings.io.enabledCalibrationTone);

		mCheckBoxKeepInputName
		= CheckBox(mView, Rect(), "Keep input file name up to _Voice_EGG.wav")
		.value_(parentMenu.oldSettings.io.keepInputName);

		mCheckBoxWriteGates
		= CheckBox(mView, Rect(), "Write _Gates file with any cycle-synchronous output")
		.value_(parentMenu.oldSettings.io.enabledWriteGates);

		mStaticTextExtraChannels
		= StaticText.new(mView, Rect())
		.string_("Record also extra inputs:")
		.align_(\right);

		mEditTextExtraChannels
		= TextField.new(mView, Rect())
		.string_(if (parentMenu.oldSettings.io.enabledRecordExtraChannels,
			{ parentMenu.oldSettings.io.arrayRecordExtraInputs.asString},
			{ nil.asString }))
		.align_(\left);

		mStaticTextColorTheme
		= StaticText.new(mView, Rect())
		.string_("Colours:")
		.align_(\right);

		mListColorThemes = ListView(mView, Rect(0, 0, 70, 0))
		.fixedHeight_(50)
		.items_([ "Standard", "Studio", "Military" ])
		.font_(static_font)
		.selectionMode_(\single)
		.value_(parentMenu.oldSettings.general.colorThemeKey);

		mButtonCancel
		= Button(mView, Rect())
		.states_([["Cancel"]])
		.action_({ mDialog.close });

		mButtonOK
		= Button(mView, Rect())
		.states_([["OK"]])
		.action_( { this.accept(parentMenu) });

		mView.allChildren( { |v|
			v.font_(static_font);
			if (v.class == StaticText, { v.fixedWidth_(160); v.fixedHeight_(35) });
		});

		mStaticTextProgramVersion
		.font_(static_font.boldVariant)
		.fixedWidth_(180);

		mView.layout = VLayout.new(
			HLayout([mStaticTextProgramVersion, stretch: 1, align: \topLeft], [nil, s:5],
				[mStaticTextProgramLicence, stretch: 1, align: \right]),
			[nil, s:5],
			HLayout([mStaticTextCycleSeparationMethod, s: 1], [mButtonCycleSeparationMethod, s: 1], [nil, s: 1]),
			HLayout([mStaticTextClarityThreshold, s: 1], [mNumberBoxClarityThreshold, s: 1], [nil, s: 1]),
			[mCheckBoxCalibrationTone, s: 1],
			[mCheckBoxKeepInputName, s: 1],
			[mCheckBoxWriteGates, s: 1],
			HLayout([mStaticTextExtraChannels, s: 1], [mEditTextExtraChannels, s: 2], nil),
			HLayout([mStaticTextColorTheme, s: 1], [mListColorThemes, s: 1], [nil, s: 1]),
			[nil, s:20],
			HLayout([nil, s:20], [mButtonCancel, a: \right], [mButtonOK, a: \right]);
		);

		mView.layout.margins_(5);
		mDialog.front;
	}

	accept { | parentMenu |
		var arr, bExtra;
		VRPDataVRP.clarityThreshold_(mNumberBoxClarityThreshold.value);
		parentMenu.newSettings.csdft.method =
		switch( mButtonCycleSeparationMethod.value,
			0, VRPSettingsCSDFT.methodPhasePortrait,
			1, VRPSettingsCSDFT.methodPeakFollower
		);
		parentMenu.newSettings.io.enabledCalibrationTone_(mCheckBoxCalibrationTone.value);
		parentMenu.newSettings.io.enabledWriteGates_(mCheckBoxWriteGates.value);
		parentMenu.newSettings.io.keepInputName_(mCheckBoxKeepInputName.value);
		bExtra = value {
			arr = mEditTextExtraChannels.string.compile.value;
			if (arr.isNil or: arr.isKindOf(Array).not,
				{ false },
				{ arr.every({arg item, i; item.isKindOf(Number)}) }
			)
		};
		parentMenu.newSettings.io.enabledRecordExtraChannels = bExtra;
		parentMenu.newSettings.io.arrayRecordExtraInputs = if (bExtra, { arr }, { nil } );
		parentMenu.newSettings.general.colorThemeKey_(mListColorThemes.value);
		parentMenu.bSettingsChanged_(true);
		mDialog.close;
	}

	saveSettings {
		// Maybe implement persistent settings here
	}

	loadSettings {
	}

}