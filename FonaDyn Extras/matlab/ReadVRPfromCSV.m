
function StructOut=ReadVRPfromCSV(filename);
%% Import data from text file.
% Script for importing data from the following text file:
%
%    C:\Users\stern\Documents\MATLAB\RBS-mdv-all_VRP.csv
%
% To extend the code to different selected data or a different text file,
% generate a function instead of a script.

% Auto-generated by MATLAB on 2016/04/22 11:15:58

%% Initialize variables.
%filename = '\\files.ug.kth.se\root\fs\3\voicelab\fonadyn\Filipa\wav\S1\ueS1_before-4c10h2x_VRP.csv';
delimiter = ';';
startRow = 2;

%% Format string for each line of text:
%   column1: double (%f)
%	column2: double (%f)
%   column3: double (%f)
%	column4: double (%f)
%   column5: double (%f)
%	column6: double (%f)
%   column7: double (%f)
%	column8: double (%f)
%   column9: double (%f)
%	column10: double (%f)
%   column11: double (%f)
% For more information, see the TEXTSCAN documentation.

%% Open the text file.
fileID = fopen(filename,'r');
FirstLine=fgetl(fileID);
Vars=strsplit(FirstLine,';');
if isempty(Vars{end})
    VarLength=length(Vars)-1;
else
    VarLength=length(Vars);
end

formatSpec=repmat('%f',1,VarLength);

%fileID = fopen(filename,'r');

%% Read columns of data according to format string.
% This call is based on the structure of the file used to generate this
% code. If an error occurs for a different file, try regenerating the code
% from the Import Tool.
dataArray = textscan(fileID, formatSpec, 'Delimiter', delimiter, 'HeaderLines' ,startRow-1, 'ReturnOnError', false);


%% Close the text file.
fclose(fileID);

%% Post processing for unimportable data.
% No unimportable data rules were applied during the import, so no post
% processing code is included. To generate code which works for
% unimportable data, select unimportable cells in a file and regenerate the
% script.

%% Allocate imported array to column variable names

StructOut.MIDI = dataArray{:, 1};
StructOut.dB = dataArray{:, 2};
StructOut.Total = dataArray{:, 3};
StructOut.Clarity = dataArray{:, 4};
StructOut.Crest = dataArray{:, 5};
StructOut.Entropy = dataArray{:, 6};
StructOut.maxCluster = dataArray{:, 7};

N = size(dataArray);
N = N(2);
StructOut.clusterN = N - 7

for i=1:StructOut.clusterN
    eval(['StructOut.Cluster' num2str(i) '= dataArray{:, i+7};']);
end

%% Clear temporary variables
clearvars delimiter startRow formatSpec fileID dataArray ans;