VariadicDelay : UGen {
	*ar { | in, ingate, outgate, bufsize |
		^this.multiNew('audio', in, ingate, outgate, bufsize);
	}

	*kr { | in, ingate, outgate, bufsize |
		^this.multiNew('control', in, ingate, outgate, bufsize);
	}
}