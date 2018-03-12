
%%
colorz=lines(3);
nopoints=length(dB);
dspl=0.25;
df0=0.25;

rel = Total;
FDATATEST=scatteredInterpolant(MIDI,dB,rel, 'natural','none');
[xq,yq]=meshgrid(min(MIDI):df0:max(MIDI),min(dB):dspl:max(dB));
vq=FDATATEST(xq,yq);
figure ; mesh(xq,yq,vq) 
hold on ; 
%plot3(MIDI,dB,Total)

%%
colorz=lines(5);
for m=1:5
    plot(MIDI(find(maxCluster==m)),dB(find(maxCluster==m)),'.','Color',colorz(m,:),'markersize',20)
    hold on
end

