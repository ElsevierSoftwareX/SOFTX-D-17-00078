%% plotEGG
% plots several egg waveforms as prepared by function synthEGG
% The 'filename' is used only to make a figure title

function plotEGG(waves, filename)
nCurves = size(waves, 2);
figure(1)
[p, n, x] = fileparts(filename);
title(n,'Interpreter','none');
mapFD = colormapFD(nCurves, 0.7);
hold on
for i=1:nCurves
    %y = waves(:,i)+(nCurves-i);
    y = waves(:,i)+i;
    plot(y, 'LineWidth', 3, 'Color', mapFD(i,:));
end
xlabel('cycle time (%)')
hold off
end


