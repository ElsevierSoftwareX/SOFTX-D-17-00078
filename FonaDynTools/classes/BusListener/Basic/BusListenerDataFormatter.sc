//
// Data formatter for the BusListener implementation, using lazy initialization,
// so multiple handlers using the same data format can avoid formatting the same
// data multiple times.
//
BusListenerDataFormatter {
	var mDataRaw;
	var mDataAsFrames;
	var mDataAsChannels;
	var mnBuses;

	*new { | rawData, nBuses |
		^super.new.init(rawData, nBuses);
	}

	init { | rawData, nBuses |
		mDataRaw = rawData;
		mnBuses = nBuses;
	}

	nBuses { ^mnBuses; }

	dataAsRaw {
		^mDataRaw;
	}

	dataAsFrames {
		if (mDataAsFrames.isNil, {
			mDataAsFrames = mDataRaw.unlace(mDataRaw.size / mnBuses, mnBuses);
		});

		^mDataAsFrames;
	}

	dataAsChannels {
		if (mDataAsChannels.isNil, {
			mDataAsChannels = mDataRaw.unlace(mnBuses);
		});

		^mDataAsChannels;
	}
}