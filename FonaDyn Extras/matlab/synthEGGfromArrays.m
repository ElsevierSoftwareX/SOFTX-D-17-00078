%% Resynthesize the EGG waveshapes from a [levels, phases] array.
% To extend the code to different selected data or a different text file,
% generate a function instead of a script.

function [egg, Qc, maxDegg] = synthEGGfromArrays(levels, phases, nHarmonics, points, periods)
    actualAmps = power(10, levels(1:nHarmonics)/2.0);
    actualPhases = phases(1:nHarmonics);
    nPeriod = points;
    nStep = 2*pi/nPeriod;

    %Compute periods of the waveshapes 
    harmonics = zeros(nHarmonics, periods*nPeriod);
    for k = 1:nHarmonics
        for i = 1:(periods*nPeriod)
            harmonics(k, i) = actualAmps(k) * cos((i-1)*(k*nStep) + actualPhases(k)); 
        end
    end
    wave = sum(harmonics);
    aMax = max(wave);
    aMin = min(wave);
    wave = wave(:) ./ (aMax-aMin);
    wPeriod = wave(1:nPeriod+1);
    closed = length(find(wPeriod>0));
    Qc = closed/nPeriod; 
    wPeriodDiff = wave(2:nPeriod+1) - wave(1:nPeriod);
    maxDegg = max(wPeriodDiff)*points;
    egg = wave;
end

