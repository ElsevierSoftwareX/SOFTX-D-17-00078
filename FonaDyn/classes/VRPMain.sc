// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
/*
// FonaDyn (C) Sten Ternström, Dennis Johansson 2015-2017
// KTH Royal Institute of Technology, Stockholm, Sweden.
// For full details of using this software, please see the FonaDyn Handbook, and the class help files.
// The main entry point to the online help files is that of the class VRPMain().
*/

// REVISION NOTES
// v1.5.0 License info added to all VRP*.sc files; release to SoftwareX
// v1.4.4 in FonaDyn.sc, added a check of PATH and prepending <resourceDir>; to it,
//        if necessary - not sure if this is Windows-specific or not
// v1.4.3 simplified the file format _clusters.csv to a rectangular array
// v1.4.2 added a Pause button, and moved the Start button to its left
//        Pause will also mute any audio feedback.
// v1.4.1 optimized "fetch"; tweaked the color schemes a little; optimized VRP drawing
// 		  so as not to have to redraw everything on every update.
// v1.4.0 added interface for reordering the clusters manually: On cluster columns,
//          ctrl-click left to swap with cluster to the left,
//          ctrl-click right to swap with with cluster to the right.
//          Wraps around at first and last cluster.
// v1.3.6.3 added Settings... GUI for recording of extra channels
// v1.3.6.2 added recording of extra channels;
//		with a hardcoded framerate 100 Hz, enable and input array are in VRPSettingsIO.sc.
// 		Also ganged the left cluster slider to the VRP display but not vice versa
// v1.3.6 added "color schemes"; more trimming of the GUI
// v1.3.5 Took out the weighting of phase (cos, sin), much better!
// v1.3.4 GUI: repaired the tab order, enabled/disabled buttons on stop/start,
//        tweaked text field sizes, and the SampEn grid
// v1.3.3 Disk-in now records conditioned signals and colors output buttons
// v1.301 fixed the Cluster bug in isochronous log files
// v1.30 added Hz log scale to VRP (toggle with right-click)
//       (the source code: Grid-ST.sc must be included in .\Extensions\SystemOverwrites)
//       Also realigned the VRP grid to the cell boundaries

VRPMain {
	// Graphics
	var mWindow;
	var mViewMain;

	// Data
	var <mContext;
	var mGUIRunning;
	var mMutexGUI;
	classvar <mVersion = "1.5.0";

	// Edit the character after $ to set the column delimiter in CSV files
	classvar <cListSeparator = $; ;

	// Clocks
	var mClockGuiUpdates; 	// Send updates to the GUI via this tempoclock
	var mClockControllers; 	// Clock used by the controllers to schedule fetches etc
//	var mClockDebug;  		// Clock used to dump debug info at intervals

	// Sundry
	classvar <mAdapterUpdate;
	classvar <panelColor;

	*new {
		^super.new.start;
	}

	postLicence {
		var texts = [
			"=========== FonaDyn Version % ============",
			"© 2017 Sten Ternström, Dennis Johansson, KTH Royal Institute of Technology",
			"Distributed under European Union Public License v1.2, see ",
			~gLicenceLink ];

		format(texts[0], mVersion).postln;
		texts[1].postln;
		texts[2].post;
		texts[3].postln;
	}

	start {
		~gLicenceLink = "https://eupl.eu";

		// Set the important members
		mContext = VRPContext(\global, Server.default);
		mClockGuiUpdates = TempoClock(24);					// Maybe increase the queuesize here?
		mClockControllers = TempoClock(60, queueSize: 1024); // Enough space for 512 entries
		//	mClockDebug = TempoClock(0.2);

		// Start the server
		mContext.model.server.boot;
		mContext.model.server.doWhenBooted( { this.postLicence } );

		panelColor = mContext.model.settings.general.getThemeColor(\backPanel);

		// Create the main window
		mWindow = Window("FonaDyn", Window.availableBounds().insetAll(20, 200, 400, 0), true, true, mContext.model.server);
		mWindow.view.background_( Color.grey(0.85) );   // no effect?

		mAdapterUpdate =
		{ | menu, what, pause |
			if (what == \dialogSettings,
				{ mContext.model.resetData }
			);
		};

		// Create the Main View
		mViewMain = VRPViewMain( mWindow.view );
		mViewMain.fetch(mContext.model.settings);
		mContext.model.resetData;
		// mContext.inspect;

		mWindow.onClose_ {
			var gd = mContext.model.data.general;
			var gs = mContext.model.settings.general;
			mGUIRunning = false;
			if (gd.started, {
				gs.stop = true;
			});
			gs.start = false;
		};

		mWindow.front;

		// Initiate GUI updates
		mMutexGUI = Semaphore();
		mGUIRunning = true;
		mClockGuiUpdates.sched(1, {
			var ret = if (mGUIRunning, 1, nil);
			Routine.new({this.update}).next;
			ret
		});

		/*
		mClockDebug.sched(1, {
		var ret = if (mGUIRunning, 1, nil);
		Routine.new({
		"Free: " + Main.totalFree.postln;
		}.defer ).next;
		ret
		});
		*/
	}

	guiUpdate {
		// Propagates the update to the views if the GUI is running
		var m = mContext.model;
		if ( mGUIRunning, {
			defer {
				if (mWindow.isClosed.not, {
					mViewMain.update(m.data);
					mViewMain.fetch(m.settings);
				});
				mMutexGUI.signal;
			};
			mMutexGUI.wait;
		});
	}

	update {
		var cond = Condition();
		var c = mContext;
		var cs = c.controller;
		var m = c.model;
		var s = m.server;
		var d = m.data;
		var se = m.settings;
		var bm = m.busManager;

		block { | break |
			if ( se.general.start, {
				se.general.start = false;
				\START.postln;

				// We should start the server!
				if (d.general.started or: d.general.starting, {
					d.general.error = "Unable to start the server as it is already started!";
					break.value; // Bail out
				});

				d.general.starting = true;
				this.guiUpdate(); // Let the views know that we're starting the server

				if ( se.sanityCheck.not, {
					// Some check failed - bail out
					d.general.starting = false;
					d.general.started = false;
					break.value;
				});

				// Reset the data - grabbing the new settings
				m.resetData;
				d = m.data;

				// Wait for the server to fully boot
				s.bootSync(cond);

				// Allocate the groups
				value {
					var c = Condition();
					var sub_groups = { Group.basicNew(s) } ! 8;
					var main_group = Group.basicNew(s);
					var msgs = [main_group.newMsg(s), main_group.runMsg(false)]; // Ensure that the main group is paused immediately!
					msgs = msgs ++ ( sub_groups collect: { | g | g.newMsg(main_group, \addToTail) } ); // Create the rest normally

					m.groups.putPairs([
						\Main, main_group,
						\Input, sub_groups[0],
						\AnalyzeAudio, sub_groups[1],
						\CSDFT, sub_groups[2],
						\Cluster, sub_groups[3],
						\SampEn, sub_groups[4],
						\PostProcessing, sub_groups[5],
						\Scope, sub_groups[6],
						\Output, sub_groups[7],
					]);

					// Send the bundle and sync
					s.sync(c, msgs);
				};

				// Create the controllers
				cs.io = VRPControllerIO(m.groups[\Input], m.groups[\Output], m.groups[\Main], d);
				cs.cluster = VRPControllerCluster(m.groups[\Cluster], d);
				cs.csdft = VRPControllerCSDFT(m.groups[\CSDFT], d);
				cs.sampen = VRPControllerSampEn(m.groups[\SampEn], d);
				cs.scope = VRPControllerScope(m.groups[\Scope], d);
				cs.vrp = VRPControllerVRP(m.groups[\AnalyzeAudio], d);
				cs.postp = VRPControllerPostProcessing(m.groups[\PostProcessing], d);

				// Find out what buses are required and allocate them
				cs.asArray do: { | c | c.requires(bm); };
				bm.allocate();
				s.sync;
				// bm.debug;  // Post the bus numbers for inspection

				// Prepare all controllers and sync
				cs.asArray do: { | x | x.prepare(m.libname, s, bm, mClockControllers); };
				s.sync;

				// Start all controllers and sync
				cs.asArray do: { | x | x.start(s, bm, mClockControllers); };
				s.sync;

				// Resume the main group so all synths can start!
				m.groups[\Main].run;
				d.general.started = true;
				d.general.starting = false;
				d.general.pause = 0;
			}); // End start

			if (d.general.pause == 1, {
				m.groups[\Main].run(false);
				d.general.pause = 2;
				"Pausing - ".post;
			});

			if (d.general.pause == 3, {
				m.groups[\Main].run(true);
				d.general.pause = 0;
				"resumed".postln;
			});

			// Either user wants to stop, or we reached EOF - make sure we're not already stopping or have stopped the server.
			if (se.general.stop or: (d.io.eof and: d.general.stopping.not and: d.general.started), {
				se.general.stop = false;
				\STOP.postln;

				// Perform sanity checks, if they fail -> bail out
				if (d.general.started.not, {
					d.general.error = "Unable to stop: the server has not yet been started.";
					break.value; // Bail out
				});

				d.general.stopping = true;

				// Pause the main group
				m.groups[\Main].run(false);

				// Stop the controllers and sync
				cs.asArray do: { | x | x.stop; };

				this.guiUpdate(); // Let the views know that we're stopping the server

				cs.asArray do: { | x | x.sync; };

				// Free the buses & groups
				m.busManager.free;
				m.groups[\Main].free;
				m.groups.clear;

				// Done
				s.sync;
				d.general.started = false;
				d.general.stopping = false;
			}); // End stop
		}; // End block

		this.guiUpdate();
	}
}