colorz=lines(clusterN);
figureN = 1;

h = 0;
if mod(clusterN+1,2) == 0
    h = (clusterN+1)/2; %already even
else
    h = (clusterN+2)/2; %number was odd
end

xmax = 80;  % max(MIDI)+10;
xmin = 40;  % min(MIDI)-10;
ymax = 120; % max(dB)+10;
ymin = 40;  % min(dB)-10;

figure(2)
cmap = flipud(colormap(jet(clusterN)));
cmap = [1,1,1;cmap];
for c=1:clusterN
    cmap = [cmap;1,1,1;cmap(c+1,:)];
end

figureN = 1;
allPixels = ones(150,100)*NaN;

for c=1:clusterN
    pixels = ones(150,100)*NaN;
    indices = find(maxCluster==c);
    for i=1:length(indices)
        pixels(dB(indices(i)),MIDI(indices(i))) = c;
        allPixels(dB(indices(i)),MIDI(indices(i))) = c;
    end
%     ax = subplot(2,h,c+1);
%     colormap(ax,[1,1,1;cmap(c+1,:)]);
%     handles(c) = pcolor(pixels);
%     set(handles(c), 'EdgeColor', 'none');
%     xlim(ax,[xmin xmax])
%     ylim(ax,[ymin ymax])
end


%ax = subplot(1,1,1);% subplot(2,h,1);
%colormap(ax,cmap(1:clusterN+1,:));

handle = pcolor(allPixels);
%colormap(cmap);
set(handle, 'EdgeColor', 'none');
xlim([xmin xmax])
ylim([ymin ymax])
grid on

