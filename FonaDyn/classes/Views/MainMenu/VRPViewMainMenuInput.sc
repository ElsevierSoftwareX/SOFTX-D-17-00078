// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPViewMainMenuInput {
	var mView;

	// States
	var mClipping;
	var mClockUpdate;

	// Controls
	var mButtonStart;
	var mButtonPause;

	var mStaticTextInput;
	var mListInputType;

	var mButtonBrowse;
	var mStaticTextFilePath;

	var mUserViewClipping;
	var mFontClipping;

	var mButtonAddFilePath;
	var mButtonRemoveFilePath;
	var mListBatchedFilePaths;
	var mStaticTextClock;

	// States
	var mLastIndex; // Last index into the batched file paths that was played
	var mPauseNow;

	classvar canStart = 0;
	classvar setStart = 1;
	classvar waitingForStart = 2;

	classvar canStop = 3;
	classvar setStop = 4;
	classvar waitingForStop = 5;

	classvar fromRecording = 0;
	classvar fromSingleFile = 1;
	classvar fromMultipleFiles = 2;

	*new { | view |
		^super.new.init(view);
	}

	init { | view |
		var b = view.bounds;
		var static_font = Font(\Arial, 12);
		mView = view;

		//////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////

		mStaticTextInput = StaticText(mView, Rect(0, 0, 100, 0))
		.string_("Source:")
		.font_(static_font);
		mStaticTextInput
		.fixedWidth_(mStaticTextInput.sizeHint.width)
		.fixedHeight_(35)
		.stringColor_(Color.white);

		mListInputType = ListView(mView, Rect(0, 0, 100, 0))
		.items_([
			"Record",
			"From file",
			"Batch multiple files"
		]);
		mListInputType
		.fixedHeight_(55)
		.fixedWidth_(mListInputType.sizeHint.width)
		.selectionMode_(\single)
		.font_(static_font)
		.action_{ | list |
			var is_recording = list.value == fromRecording;
			var is_from_file = list.value == fromSingleFile;
			var is_from_batched_files = list.value == fromMultipleFiles;

			// Make the additional controls visible if we're in from file mode.
			mButtonBrowse.visible_(is_from_file);
			mStaticTextFilePath.visible_(is_from_file);
			mUserViewClipping.visible_(is_recording);
			mButtonAddFilePath.visible_(is_from_batched_files);
			mButtonRemoveFilePath.visible_(is_from_batched_files);
			mListBatchedFilePaths.visible_(is_from_batched_files);

			this.updateMenu();
		};

		//////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////

		mButtonBrowse = Button(mView, Rect(0, 0, 100, b.height))
		.visible_(false)
		.states_([["Browse"]])
		.action_({ | btn |
			Dialog.openPanel(
				{ | path |
					// A file at path was selected
					mStaticTextFilePath.string_(path);

					// Make sure the start button is enabled.
					mButtonStart.enabled_(true);
				}, nil);
		});

		// Disabled TextField to easily present the path.
		mStaticTextFilePath = TextField(mView, Rect(0, 0, 100, b.height))
		.enabled_(false)
		.visible_(false)
		.background_(Color.white);

		//////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////

		mClipping = 0;
		mFontClipping = Font(\Arial, 32, true);

		mUserViewClipping = UserView(mView, Rect(0, 0, 100, 30))
		.fixedWidth_(250)
		.fixedHeight_(40)
		.drawFunc_ { | view |
			var r = view.bounds.resizeBy(-4, -4).moveTo(2, 2);
			Pen.use {
				// Draw a rectangle around the border to make it visible when not clipping
				Pen.width_(3);
				Pen.color_(Color.grey(0.35));
				Pen.addRect( r );
				Pen.perform([\stroke]);

				if (mClipping > 0, {
					"CLIPPING!".drawCenteredIn(r, mFontClipping, Color.red);
					mClipping = mClipping - 1;
				});
			}
		};

		//////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////

		mButtonAddFilePath = Button(mView, Rect())
		.visible_(false)
		.states_([
			["Add File/-s"]
		]);
		mButtonAddFilePath
		.fixedWidth_(mButtonAddFilePath.sizeHint.width)
		.fixedHeight_(mButtonAddFilePath.sizeHint.height)
		.action_{ | btn |
			Dialog.openPanel(
				{ | paths |
					mListBatchedFilePaths.items_(
						(mListBatchedFilePaths.items ? [])
						++
						paths
					);

					mButtonStart.enabled_(
						(mListBatchedFilePaths.items ? []).isEmpty.not
					);
				},
				multipleSelection: true
			);
		};

		//////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////

		mButtonRemoveFilePath = Button(mView, Rect())
		.visible_(false)
		.states_([
			["Remove File/-s"]
		]);
		mButtonRemoveFilePath
		.fixedWidth_(mButtonAddFilePath.sizeHint.width)
		.fixedHeight_(mButtonAddFilePath.sizeHint.height)
		.action_{ | btn |
			var s = mListBatchedFilePaths.selection;
			var items = mListBatchedFilePaths.items ? [];
			mListBatchedFilePaths
			.selection_([])
			.items_(
				items[ Array.iota(items.size).difference(s) ]
			);

			mButtonStart.enabled_(
				(mListBatchedFilePaths.items ? []).isEmpty.not
			);
		};

		//////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////

		mLastIndex = 0;
		mPauseNow = false;

		mListBatchedFilePaths = ListView(mView, Rect())
		.visible_(false)
		.selectionMode_(\extended);

		//////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////

		mButtonStart = Button(mView, Rect(0, 0, 100, b.height))
		.states_([
			["► START"],     // Can start
			["Starting.. "], // Starting... (not dispatched)
			["Starting..."], // Starting... (dispatched - waiting for completion)
			["█▌ STOP"],        // Can stop
			["Stopping.. "], // Stopping... (not dispatched)
			["Stopping..."]  // Stopping... (dispatched - waiting for completion)
		]);
		mButtonStart
		.fixedWidth_(mButtonStart.sizeHint.width)
		.fixedHeight_(mButtonStart.sizeHint.height * 2)
		.action_{ | btn |
			switch(btn.value,
				setStart, {
					mLastIndex = 0;
				},

				setStop, {
					mLastIndex = (mListBatchedFilePaths.items ?? []).size;
				}
			);

			this.updateMenu();
		};

		//////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////

		// The Pause button
		this addDependant: VRPMain.mAdapterUpdate;
		mButtonPause = Button(mView, Rect(0, 0, 100, b.height))
		.states_([
			[ "▐▐  Pause" ],
			[ "► Resume" ]
		]);
		mButtonPause
		.fixedWidth_(mButtonPause.sizeHint.width)
		.fixedHeight_(mButtonPause.sizeHint.height * 2)
		.enabled_(false)
		.action_({ mPauseNow = true })
		.visible_(true);

		//////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////

		mClockUpdate = 0;
		mStaticTextClock = StaticText(mView, Rect())
		.visible_(true)
		.font_(Font(\Arial, 32, true))
		.stringColor_(Color.gray)
		.align_(\right);
		mStaticTextClock
		.fixedHeight_(mStaticTextClock.sizeHint.height);

		//////////////////////////////////////////////////////////
		//////////////////////////////////////////////////////////

		mView.layout = HLayout(
			[mStaticTextInput, stretch: 1],
			[mListInputType, stretch: 1],
			[mButtonBrowse, stretch: 1],
			[mStaticTextFilePath, stretch: 8],
			[mUserViewClipping, stretch: 1],
			[mButtonAddFilePath, stretch: 1],
			[mButtonRemoveFilePath, stretch: 1],
			[mListBatchedFilePaths, stretch: 8],
			[nil, stretch: 5],
			[mButtonStart, stretch: 1, align: \right],
			[mButtonPause, stretch: 1, align: \right],
			[50],
			[mStaticTextClock, stretch: 1, align: \right]
		);
	}

	updateMenu {
		var not_started = mButtonStart.value == canStart;
		mListInputType.enabled_(not_started);
		mButtonAddFilePath.enabled_(not_started);
		mButtonRemoveFilePath.enabled_(not_started);
		// mListBatchedFilePaths.enabled_(not_started);

		mButtonStart.enabled_(
			(mButtonStart.value == canStart)
			or:
			(mButtonStart.value == canStop);
		);

		if ( mButtonStart.value == canStart, {
			// Enable/Disable the start button if have/haven't chosen an input file!
			switch (mListInputType.value,
				fromSingleFile, { // From single file
					mButtonStart.enabled_(
						mStaticTextFilePath.string.isEmpty.not
					);
				},

				fromMultipleFiles, { // From multiple batched files
					mButtonStart.enabled_(
						(mListBatchedFilePaths.items ? []).isEmpty.not
					);
				}
			);
		});

		mButtonPause.enabled_(mButtonStart.value == canStop);

	}

	fetch { | settings |
		var ios = settings.io;
		var gs = settings.general;
		var cs = settings.cluster;
		var ds = settings.csdft;

		// Update the settings
		if (settings.general.guiChanged, {
			mView.background_(gs.getThemeColor(\backPanel));
			mStaticTextInput.stringColor_(gs.getThemeColor(\panelText));
		});

		if ( mButtonStart.value == setStart, {
			gs.start = true;
			mButtonStart.value = waitingForStart;
		});

		if ( mButtonStart.value == setStop, {
			gs.stop = true;
			mButtonStart.value = waitingForStop;
		});

		ios.inputType = switch ( mListInputType.value,
			fromRecording, VRPSettingsIO.inputTypeRecord,
			fromSingleFile, VRPSettingsIO.inputTypeFile,
			fromMultipleFiles, VRPSettingsIO.inputTypeFile
		);

		ios.filePathInput = switch ( mListInputType.value,
			fromSingleFile, {
				mStaticTextFilePath.string
			},

			fromMultipleFiles, {
				(mListBatchedFilePaths.items ?? [])[mLastIndex]
			}
		);

		this.updateMenu();
	}

	update { | data |
		var iod = data.io;
		var gd = data.general;

		// Did we previously attempt to start the server?
		if ( (mButtonStart.value == waitingForStart) and: gd.starting.not, {
			mButtonStart
			.value_( if (gd.started, canStop, canStart) );
		});

		// Did we previously attempt to stop the server?
		if ( (mButtonStart.value == waitingForStop) and: gd.stopping.not, {
			mButtonStart
			.value_( if (gd.started, canStop, canStart) );
		});

		// Have we reached eof?
		if ( iod.eof and: mButtonStart.value == canStop, {
			mButtonStart
			.value_( waitingForStop );
		});

		// If we can start another one and we're using batching, and still have items left
		// Then immediately start the next file
		if ( mButtonStart.value == canStart, {
			mLastIndex = mLastIndex + 1;
			if ( (mListInputType.value == fromMultipleFiles) and: (mLastIndex < (mListBatchedFilePaths.items ?? []).size), {
				mButtonStart.value_(setStart);
				mListBatchedFilePaths.selection = mLastIndex.asArray;
			});
		});

		if ( iod.clip, {
			mClipping = 24;  	// Flash CLIPPING! for one second
			iod.clip = false;
		});

		if (mButtonStart.value == canStart,
			{ mButtonPause.value = 0 }
		);

		if ( mPauseNow,  {
			gd.pause = gd.pause + 1;  // advance the pause state
		});
		mPauseNow = false;

		mClockUpdate = mClockUpdate + 1;
		if ((gd.started and: gd.stopping.not) and: mClockUpdate.mod(24) == 0, {
			var a, d, str;
			a = data.scope.sampen.first;
			d = if (a.isNil, 0.0, { Point(a.last).x });
			str = format("%:%", (d/60).floor, d.mod(60).floor.asString.padLeft(2, "0"));
			mStaticTextClock.string_(str);
		});

		this.updateMenu();
	}

	close {

	}
}