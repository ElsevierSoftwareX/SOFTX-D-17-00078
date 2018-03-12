% Plotting of FonaDyn  _Log.aiff files
% FonaDyn version 1.3.2 
% Customize to your own needs

% % Read _Log file data
recdir = 'L:\fonadyn\CMPB article\';    % the folder of the _Log.aiff file 
fileName = '170130_085255_Log.aiff';    % the name of the file
[data, samplerate] = audioread(strcat(recdir, fileName));
[frames, channels] = size(data);

% cTime = data(:, 1);
% cF0 = data(:, 2);
% cLevel = data(:, 3);
% cClarity = data(:, 4);
% cCrest = data(:, 5);
% cCluster = data(:, 6);
% cSampEn = data(:, 7);

figure(1);

labelStrings = [['fo (ST) ']; ['SL dBFS']; ['Clarity ']; ['Crest dB']; ['Cluster#']; ['SampEn  ']];
axisLabels = cellstr(labelStrings);

% Bracket y axis plotting here [min max fo; ... ]
yLimits = [30 96; -40 0; 0.96 1.01; 0 5; -1 3; 0 10]; 

for i = 1:6
    ax(i) = subplot(6,1,i);
    plot (data(:,1),data(:,i+1));
    ylabel(ax(i), axisLabels{i}, 'FontSize', 8);
    ylim(ax(i), yLimits(i,:));
    grid on
    grid minor
end;
xlabel ('time (s)');

%%

%Plot the levels and phases for the first 4 harmonics in a separate figure
figure(2);

nharm = (channels-7)/2; 
% # of harmonics + 1
% The last "harmonic" holds the power level of residual higher harmonics, 
% and a copy of the phase of the fundamental. 

% This example assumes that at least 4 harmonics were specified
for i = 8:11
    subplot(2,1,1)
    plot (data(:,1), data(:,i).*10); % the levels are in Bels, not decibels
    %title('First 4 levels');
    ylabel('Level (dB down)');
    grid on
    grid minor
    hold on
    subplot(2,1,2)
    %plot (unwrap(data(:,1), data(:,i+nharm)));
    plot (data(:,1), data(:,i+nharm));
    %title('First 4 phases');
    xlabel('time (s)');
    ylabel('phase (rad)');
    ylim([-pi, pi]);
    grid on
    grid minor
    hold on
end;

subplot(2,1,1)
legend('FD1','FD2','FD3','FD4');

