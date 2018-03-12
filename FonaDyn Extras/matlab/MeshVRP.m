%% This function takes the struct DS built by the function ReadVRPfromCSV
% and plots the clusters in the VRP, using interpolated meshes 

function MeshVRP(DS, nClusters, minCycles, nFigure, strName, dBcal)
%%
dspl=0.25;
df0=0.25;
cmap=colormapFD(nClusters, 0.7);
xmax = 80;  % or, autoscale with max(DS.MIDI)+10;
xmin = 40;  % min(MIDI)-10;
ymax = 120; % max(dB)+10;
ymin = 40;  % min(dB)-10;
offsetSPL = dBcal;

figure(nFigure);
grid on

threshIx = find(DS.Total>=minCycles);
% Or, add more constraints, to exclude outliers, for example:
% threshIx = find((DS.Total>=minCycles) & (DS.MIDI>=50) & (DS.MIDI<70));

colors=zeros(10*nClusters,3);
for c=1:nClusters
    for i=1:10
        colors((c-1)*10+i,:) = cmap(c,:)*(i/20 + 0.5);
    end
end

colormap(colors);

for c=1:nClusters
    eval(['rel = DS.Cluster' num2str(c) '(threshIx) ./ DS.Total(threshIx);']);
    midis = DS.MIDI(threshIx);
    spls  = DS.dB(threshIx)+dBcal;
    FDATATEST=scatteredInterpolant(midis, spls, rel, 'natural', 'none');
%     FDATATEST.Method = 'natural';
%     FDATATEST.ExtrapolationMethod = 'linear';
    [xq,yq]=meshgrid(min(midis):df0:max(midis),min(spls):dspl:max(spls));
    vq=FDATATEST(xq,yq);
    vq(vq>0.999)=0.999;
    xlim([xmin xmax]);
    ylim([ymin ymax]);
    view(2);
    m = mesh(xq,yq,vq,vq+(c-1));
    m.LineStyle = 'none';
    m.FaceColor = 'interp';
    hold on ; 
%plot3(MIDI,dB,Total)
end

[p, n, x] = fileparts(strName);
title(n,'Interpreter','none');


%%
% colorz=lines(5);
% for m=1:5
%     plot(MIDI(find(maxCluster==m)),dB(find(maxCluster==m)),'.','Color',colorz(m,:),'markersize',20)
%     hold on
% end

