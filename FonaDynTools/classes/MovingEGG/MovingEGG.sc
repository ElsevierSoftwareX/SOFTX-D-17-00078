MovingEGG : UGen {

	*allocBuffer { | server, numCycles, numSamples |
		^Buffer.alloc(server, numCycles * numSamples);
	}

	*ar { | bufnum, in, gate, count = 20, minSamples = 100, maxSamples = 882, normalize = 1 |
		^this.multiNew('audio', bufnum, in, gate, count, minSamples, maxSamples, normalize);
	}

}