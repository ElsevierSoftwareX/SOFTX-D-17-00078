GatedBufWr : UGen {
	*kr { | inputArray, bufnum, gate, phase, loop = 1 |
		^this.multiNewList( ['control', bufnum, gate, phase, loop] ++ inputArray );
	}
}