function [vrp, totals]=ComparableVRPs(DataStruct, minCycles, metric)


vrp=zeros(67,81);
totals=zeros(4);
for m=1:length(DataStruct.MIDI)
    if ((DataStruct.Total(m) >= minCycles) && (DataStruct.MIDI(m) > 29))
        %vrp(DataStruct.MIDI(m)-29,DataStruct.dB(m)-39)=DataStruct.maxCluster(m);
        eval(['vrp(DataStruct.MIDI(m)-29,DataStruct.dB(m)-39)=DataStruct.' metric '(m);']);
        totals(DataStruct.maxCluster(m)) = totals(DataStruct.maxCluster(m)) + DataStruct.Total(m);
    end
end