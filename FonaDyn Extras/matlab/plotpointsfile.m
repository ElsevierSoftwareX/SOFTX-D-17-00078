% Read data
recdir = 'C:\Users\stern\Documents\SuperCollider\Recordings\Ragnhild_Isak\';
fileName = '160223_113318_Points.aiff';
[data, samplerate] = audioread(strcat(recdir, fileName));
[frames, channels] = size(data);
nDiffs = channels/2;

% cF0 = data(:, 1);
% cLevel = data(:, 2);
% cClarity = data(:, 3);
% cCrest = data(:, 4);
% cCluster = data(:, 5);
% cSampEn = data(:, 6);

figure(1);

subplot(2,1,1);
hold on

for i = 1:nDiffs
    plot (data(:,i));
end;

subplot(2,1,2);
hold on    
%%
%% figure(2);


for i = nDiffs+1:channels
    plot (data(:,i));
end;


