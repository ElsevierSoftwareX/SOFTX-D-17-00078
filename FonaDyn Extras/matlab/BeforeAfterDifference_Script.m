%Read the data in two structs
fileNameBefore = '\\files.ug.kth.se\root\fs\3\voicelab\fonadyn\Filipa\wav\S3\ueS3_before-4c10h2x_VRP.csv';
fileNameAfter  = '\\files.ug.kth.se\root\fs\3\voicelab\fonadyn\Filipa\wav\S3\uS3_after-4c10h2x_VRP.csv';
nClusters=4;

StructBefore=ReadVRPfromCSV(fileNameBefore);
StructAfter=ReadVRPfromCSV(fileNameAfter);

% %Make the VRPs comparable by assigning the values in a new clean matrix
[vrpafter, totalafter]=ComparableVRPs(StructAfter, nClusters, 'maxCluster');
[vrpbefore, totalbefore]=ComparableVRPs(StructBefore, nClusters, 'maxCluster');

%Product of the matrices cell by cell
prodc=vrpafter.*vrpbefore; 
%Find the cells that are common in both before and after productions
nonzerocells=find(prodc~=0);

%Find how the maxCluster changes in the two conditions
for m=1:nClusters
    BeforeAfterDiff(m,:)=[length(find(vrpbefore(nonzerocells)==m)) length(find(vrpafter(nonzerocells)==m)) totalbefore(m) totalafter(m)];  
end

MeshVRP(StructBefore, nClusters, 5, 1, fileNameBefore, -2.4);
MeshVRP(StructAfter, nClusters, 5, 2, fileNameAfter, -2.4);


