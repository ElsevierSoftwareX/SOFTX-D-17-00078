% Example of how to pick EGG cycles with a _Gates.wav file  
%
% Read data
gates = 'C:\Recordings\160219_133156_Gates.wav';
[data, samplerate] = audioread(gates);
[frames, channels] = size(data);

rawegg = data(:, 1);        % the EGG signal as it came from the audio interface
condegg = data(:, 2);       % the EGG signal after preconditioning
gc = data(:, 3);            % cycle markers for all cycle candidates
gdc = data(:, 4);           % cycle markers delayed one cycle
gfdft = data(:, 5);         % markers for cycles actually used for DFT analysis

% Find # of cycles
n = 0;
for i=1:frames
    if gc(i) > 0
        n = n + 1;
    end
end

% Fill a matrix with cycle ranges
idx = 1;
cycles = ones(n, 2);
first = 1;
for i=1:frames
    if gc(i) <= 0
        continue;
    end
    
    cycles(idx, 1) = first;
    cycles(idx, 2) = i - 1;
    first = i;
    idx = idx + 1;
end

% Allocate matrix for separating good and discarded cycles
goodc = ones(n, 2);
discardedc = ones(n, 2);

idx = 1;
goodidx = 1;
discardedidx = 1;
for i=1:frames
    if gdc(i) <= 0
        continue;
    end
    
    % We have a cycle
    if gfdft(i) > 0
        % Good cycle
        goodc(goodidx, :) = cycles(idx, :);
        goodidx = goodidx + 1;
    else
        % Discarded cycle
        discardedc(discardedidx, :) = cycles(idx, :);
        discardedidx = discardedidx + 1;
    end
    
    idx = idx + 1;
end

fprintf('We have %d good, %d discarded and %d clipped cycles!\nPlotting three random good and discarded cycles.\n', ...
    goodidx - 1, discardedidx - 1, n - (goodidx + discardedidx - 2) );

% Plot good cycles
figure;
idx = 1;
a = 1;
b = goodidx - 1;

for i = 1:3
    r = round( (b - a).*rand(1, 1) + a );
    cycle = goodc( r, : );
    subplot(3, 2, idx);
    plot( rawegg( cycle(1):cycle(2) ) );
    title( sprintf('Raw EGG: Good cycle #%d', ceil( idx / 2 ) ) );
    idx = idx + 1;
    subplot(3, 2, idx);
    plot( condegg( cycle(1):cycle(2) ) );
    title( sprintf('Conditioned EGG: Good cycle #%d', ceil( idx / 2 ) ) );
    idx = idx + 1;
end

% Plot discarded cycles
figure;
idx = 1;
a = 1;
b = discardedidx - 1;

for i = 1:3
    r = round( (b - a).*rand(1, 1) + a );
    cycle = discardedc( r, : );
    subplot(3, 2, idx);
    plot( rawegg( cycle(1):cycle(2) ) );
    title( sprintf('Raw EGG: Discarded cycle #%d', ceil( idx / 2 ) ) );
    idx = idx + 1;
    subplot(3, 2, idx);
    plot( condegg( cycle(1):cycle(2) ) );
    title( sprintf('Conditioned EGG: Discarded cycle #%d', ceil( idx / 2 ) ) );
    idx = idx + 1;
end
