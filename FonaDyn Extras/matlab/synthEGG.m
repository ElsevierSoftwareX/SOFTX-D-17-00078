%% Resynthesize the EGG waveshapes from a FonaDyn 1.3.2 cluster data file.
% (filename_clustw.csv)
% You can use the plotEGG function to show the result.

function egg = synthEGG(filename, points, periods)

%% Initialize variables.
%filename = 'L:\fonadyn\wav\S2\cuS2-before-5c6h2x_clusters.csv';
delimiter = ';';

% Open the file, read as one large array, and parse the first two rows
cData = dlmread(filename, delimiter);

% Parse cData depending on the file format (old <= 1.4.2; new >= 1.4.3)
if (cData(1,3) == 0) 
    nClusters = cData(1, 1);
    nDeltas = cData(1, 2) / 3;
    nCounts = cData(2,1:(nDeltas-1));
    % Build three arrays, of delta-levels, cosines and sines
    cArray = cData(3:(nClusters+2),:);
else
    nClusters = size(cData,1);
    nDeltas   = (size(cData,2)-1) / 3;
    nCounts   = cData(:,1);
    cArray    = cData(:,2:end);
end

% Build three arrays, of delta-levels, cosines and sines
cArray = cData(3:(nClusters+2),:);
dLevels = [zeros([nClusters,1]) cArray(1:nClusters,1:(nDeltas-1))];
ix = [nDeltas 1:(nDeltas-1)];
dCosines = cArray(1:nClusters, ix+nDeltas);
dSines = cArray(1:nClusters, ix+(2*nDeltas));

% Compute arrays of amplitudes and phases for each 'harmonic'
amps = power(10, dLevels/2);
phases = atan2(dSines, dCosines);
for n = 1:nClusters
   phases(n,2:nDeltas) = phases(n,2:nDeltas) + phases(n,1);
end

nPeriod = points;
nStep = 2*pi/nPeriod;

%Compute two periods of the waveshapes for all clusters
harmonics = zeros(nDeltas, periods*nPeriod);
waves = zeros(nClusters, periods*nPeriod);
for n = 1:nClusters
    for i = 1:(periods*nPeriod)
        for k = 1:nDeltas
            harmonics(k, i) = amps(n,k) * cos((i-1)*(k*nStep) + phases(n,k)); 
        end
    end
    waves(n,:) = sum(harmonics);
end
egg = waves';
end
