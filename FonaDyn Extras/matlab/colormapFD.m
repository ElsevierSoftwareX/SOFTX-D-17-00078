% This function creates the same colours as those displayed in FonaDyn itself (or close). 
% For 'nColors' pass the number of clusters; for 'saturation' pass 0.7 to match FonaDyn.
% The function returns a 'colormap' from cluster numbers to colours
% that you can apply to your own plots
function map = colormapFD(nColors, saturation)
hues = zeros(nColors, 3);
map = hues;
sat = saturation;
for i = 1:nColors
    hues(i,:) = [(i-1)/nColors sat 1]; 
end
map = hsv2rgb(hues);
end
