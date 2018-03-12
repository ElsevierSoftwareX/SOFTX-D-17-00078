GatedDiskOut : UGen {
	*ar { | bufnum, gate, channelsArray |
		^this.multiNewList(['audio', bufnum, gate] ++ channelsArray.asArray)
	}
}
