// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPSettingsIO {
	// States
	var <>inputType; // One of the inputType*s below.
	var <>enabledEcho; // True if the recorded voice or playback voice should be echoed through the speakers
	var >enabledCalibrationTone; // True if calibration tone should be played on 2nd channel during Record
	var <>enabledWriteLog; // True if a log should be written
	var <writeLogFrameRate; // 0 means cycle-synchronous, otherwise Hz for isochronous frames (100..500 Hz recommended)
	var <>enabledWriteAudio; // True if the voice/EGG data should be written to a file
	var <>enabledWriteCycleDetection; // True if the cycle detection data should be written to a file
	var <>enabledWriteOutputPoints; // True if the points used for clustering should be output to a file
	var <>enabledWriteSampEn; // True if the SampEn values should be output to a file
	var <>enabledWriteGates; // True if a _Gates file should be written
	var <>enabledRecordExtraChannels;
	var <>arrayRecordExtraInputs;

	var <>filePathInput; // The path to the input file
	var <>keepInputName;

	classvar <inputTypeFile = 1; // Input is taken from a file.
	classvar <inputTypeRecord = 2; // Input is a live recording.

	*new {
		^super.new.init;
	}

	init {
		writeLogFrameRate = 0;	// cycle-synchronous frames
		enabledCalibrationTone = false;
		enabledWriteGates = false;
		enabledRecordExtraChannels = false;
		arrayRecordExtraInputs = nil;
 		keepInputName = false;
	}

	enabledCalibrationTone {
		^((inputType == inputTypeRecord) and: enabledCalibrationTone );
	}

	writeLogFrameRate_ { | lfr |
		writeLogFrameRate = lfr;
	}
}
