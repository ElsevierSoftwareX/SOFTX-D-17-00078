// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPDataGeneral {
	var <timestamp; // Timestamp as YYMMDD_HHMMSS

	var <>error; // Error message to be presented - this message takes precedence over warning
	var <>warning; // Warning to be presented - this message takes precedence over notification
	var <>notification; // Notification to be presented - this message is only presented if no error or warning is present

	var <>stopping; // True if the server is stopping
	var <>starting; // True if the server is starting
	var <>started;  // True if the server IS started
	var <>pause;   /// Pause states: 0=not, 1=pausing, 2=paused, 3=resuming

	*new { | settings |
		^super.new.init(settings);
	}

	init { | settings |
		timestamp = Date.localtime.stamp;

		started = false;
		starting = false;
		stopping = false;
		pause = 0;

		error = nil;
		warning = nil;
		notification = nil;
	}

	reset { | old |
		started = old.started;
		starting = old.starting;
		stopping = old.stopping;
		pause = old.pause;

		error = old.error;
		warning = old.warning;
		notification = old.notification;
	}
}