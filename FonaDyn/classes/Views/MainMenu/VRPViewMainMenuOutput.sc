// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPViewMainMenuOutput {
	var mView;

	// Controls
	var mButtonPlayback;
	var mButtonLogAnalysis;
	var mStaticTextLogAnalysisPath;
	var mButtonSaveRecording;
	var mButtonSaveRecordingColoring;
	var mStaticTextSaveRecordingPath;
	var mButtonLogCycleDetection;
	var mStaticTextLogCycleDetectionPath;
	var mButtonOutputPoints;
	var mStaticTextOutputPointsPath;
	var mButtonOutputSampEn;
	var mStaticTextOutputSampEnPath;

	// Just edit this array to get custom log file frame rates
	var mListLogFileRatesDict =
		#[	[			-1,		   0, 				 50, 			100, 			200 ],
		["Analysis Log: Off", "Log @ cycles", "Log @ 50 Hz", "Log @ 100 Hz", "Log @ 200 Hz"]];

	*new { | view |
		^super.new.init(view);
	}

	init { | view |
		var b = view.bounds;
		var static_font = Font(\Arial, 12);
		mView = view;

		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

		mButtonPlayback = Button(mView, Rect(0, 0, 100, b.height))
		.states_([
			["Playback/Echo: Off", Color.black, Color.gray(0.9)],
			["Playback/Echo: Ready", Color.gray(0.7), Color.green(0.3)],
			["Playback/Echo: On", Color.white, Color.green(0.8)]
		])
		.action_( { |b| if (b.value==2, { b.value = 0 } )})
		.value_(1);

		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

		mButtonSaveRecording = Button(mView, Rect(0, 0, 100, b.height))
		.states_([
			["Record: Off", Color.black, Color.gray(0.9)],			// 0
			["Record: Ready", Color.gray(0.7), Color.red(0.3)],		// 1
			["Recording", Color.white, Color.red(0.8)],				// 2
 			["Recording", Color.white, Color.new(1, 0.5, 0)]		// 3 orange
		])
		.action_{ | btn |
			var enabled = btn.value > 0;
			mStaticTextSaveRecordingPath.visible_(enabled);
			if (btn.value > 1, { btn.valueAction = 0 });
		};

		mStaticTextSaveRecordingPath = TextField(mView, Rect(0, 0, 100, b.height))
		.enabled_(true)
		.visible_(false)
		.background_(Color.white);
		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

		mStaticTextLogAnalysisPath = TextField(mView, Rect(0, 0, 100, b.height))
		.enabled_(false)
		.visible_(false)
		.background_(Color.white);

		mButtonLogAnalysis = Button(mView, Rect(0, 0, 100, b.height))
		.states_( mListLogFileRatesDict[1].collect({|str, i| [str]}))
		.action_{ | btn |
			var enabled = btn.value >= 1;
			mStaticTextLogAnalysisPath.visible_(enabled);
		};

		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

		mStaticTextLogCycleDetectionPath = TextField(mView, Rect(0, 0, 100, b.height))
		.enabled_(false)
		.visible_(false)
		.background_(Color.white);

		mButtonLogCycleDetection = Button(mView, Rect(0, 0, 100, b.height))
		.states_([
			["Cycle Detection Log: Off"],
			["Cycle Log: On"]
		])
		.action_{ | btn |
			var enabled = btn.value == 1;
			mStaticTextLogCycleDetectionPath.visible_(enabled);
		};

		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

		mStaticTextOutputPointsPath = TextField(mView, Rect(0, 0, 100, b.height))
		.enabled_(false)
		.visible_(false)
		.background_(Color.white);

		mButtonOutputPoints = Button(mView, Rect(0, 0, 100, b.height))
		.states_([
			["Output Points: Off"],
			["Output Points: On"]
		])
		.action_{ | btn |
			var enabled = btn.value == 1;
			mStaticTextOutputPointsPath.visible_(enabled);
		};

		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

		mStaticTextOutputSampEnPath = TextField(mView, Rect(0, 0, 100, b.height))
		.enabled_(false)
		.visible_(false)
		.background_(Color.white);

		mButtonOutputSampEn = Button(mView, Rect(0, 0, 100, b.height))
		.states_([
			["Output SampEn: Off"],
			["Output SampEn: On"]
		])
		.action_{ | btn |
			var enabled = btn.value == 1;
			mStaticTextOutputSampEnPath.visible_(enabled);
		};

		////////////////////////////////////////////////////////////////////
		////////////////////////////////////////////////////////////////////

		mView.layout = HLayout(
			[mButtonPlayback, stretch: 1],
			[mButtonSaveRecording, stretch: 1],
			[mStaticTextSaveRecordingPath, stretch: 3],
			[mButtonLogAnalysis, stretch: 1],
			[mStaticTextLogAnalysisPath, stretch: 3],
			[mButtonLogCycleDetection, stretch: 1],
			[mStaticTextLogCycleDetectionPath, stretch: 3],
			[mButtonOutputPoints, stretch: 1],
			[mStaticTextOutputPointsPath, stretch: 3],
			[mButtonOutputSampEn, stretch: 1],
			[mStaticTextOutputSampEnPath, stretch: 3]
		);
	}

	fetch { | settings |
		var ios = settings.io;

		ios.enabledEcho = mButtonPlayback.value.odd;
		ios.enabledWriteAudio = (mButtonSaveRecording.value >= 1);
		ios.enabledWriteCycleDetection = mButtonLogCycleDetection.value == 1;
		ios.enabledWriteLog = mButtonLogAnalysis.value >= 1;
		ios.writeLogFrameRate_(mListLogFileRatesDict[0][mButtonLogAnalysis.value]);
		ios.enabledWriteOutputPoints = mButtonOutputPoints.value == 1;
		ios.enabledWriteSampEn = mButtonOutputSampEn.value == 1;
		mView.background_(settings.general.getThemeColor(\backPanel));
		mButtonSaveRecordingColoring = if (ios.inputType == VRPSettingsIO.inputTypeRecord, { 2 }, { 3 });
	}

	update { | data |
		var iod = data.io;
		var gd = data.general;

		if (mButtonPlayback.value > 0,
			{ if (gd.stopping, { mButtonPlayback.value = 1 },
				{ if (gd.started, { mButtonPlayback.value = 2 })}
			)}
		);
		if (mButtonSaveRecording.value > 0,
			{ if (gd.stopping, { mButtonSaveRecording.value = 1 },
				{ if (gd.started, { mButtonSaveRecording.value = mButtonSaveRecordingColoring })}
			)}
		);
		if (gd.starting, {
			mStaticTextOutputPointsPath.string = (iod.filePathOutputPoints ?? "").basename;
			mStaticTextLogCycleDetectionPath.string = (iod.filePathCycleDetectionLog ?? "").basename;
			mStaticTextSaveRecordingPath.string = (iod.filePathAudio ?? "").basename;
			mStaticTextOutputSampEnPath.string = (iod.filePathSampEn ?? "").basename;
			mStaticTextLogAnalysisPath.string = (iod.filePathLog ?? "").basename;
		});
		this.enableInterface(data.general.started.not);
	}

	enableInterface { | enable |
		[
			mButtonPlayback,
			mButtonSaveRecording,
			mStaticTextSaveRecordingPath,
			mButtonLogAnalysis,
			mButtonLogCycleDetection,
			mButtonOutputPoints,
			mButtonOutputSampEn
		]
		do: { | ctrl | ctrl.enabled_(enable); };
	}
	close {

	}
}