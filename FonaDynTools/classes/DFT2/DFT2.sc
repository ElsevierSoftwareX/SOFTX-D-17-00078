DFT2 : MultiOutUGen {
	var n;

	*ar { | in, gate, kArray, minFrequency = 50, minSamples = 10 |
		^this.multiNew('audio', in, gate, minFrequency, minSamples, *kArray);
	}

	init { | ... theInputs | //Required for MultiOutUgen
		inputs = theInputs;
		^this.initOutputs(3 + (2 * (theInputs.size - 4)), rate); // outGate + skipGate + cycle length + complex number for each k in kArray
	}
}