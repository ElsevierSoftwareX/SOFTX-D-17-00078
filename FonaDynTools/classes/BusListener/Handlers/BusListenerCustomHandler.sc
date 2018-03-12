BusListenerCustomHandler {
	var mFn;

	*new { | fn |
		^super.new.init(fn);
	}

	init { | fn |
		mFn = fn;
	}

	dispatch { | data |
		mFn.(data);
	}

	free {
	}
}