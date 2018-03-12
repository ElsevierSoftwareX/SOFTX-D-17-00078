// Copyright (C) 2017 by Sten Ternström & Dennis J. Johansson, KTH Stockholm
// Released under European Union Public License v1.2, at https://eupl.eu
// *** EUPL *** //
VRPSDIO {
	classvar nameLiveInput = \sdLiveInput;
	classvar nameDiskInput = \sdDiskInput;
	classvar nameEchoMicrophone = \sdEchoMicrophone;
	classvar nameWriteAudio = \sdWriteAudio;
	classvar nameWriteCycleDetectionLog = \sdWriteCycleDetectionLog;
	classvar nameWritePoints = \sdWritePoints;
	classvar nameWriteSampEn = \sdWriteSampEn;
	classvar nameWriteFreqAmp = \sdWriteFrequencyAmplitude;
	classvar nameWriteLog = \sdWriteLog;
	classvar nameWriteGates = \sdWriteGates;
	classvar nameRecordExtraChannels = \sdRecordExtraChannels;

	/* FIR filter additions	*/

	classvar lpBuffer = nil;
	classvar hpBuffer = nil;

	*loadCoeffs {
		var lpCoeffs, hpCoeffs;

		/* FIR coeffs generated in Matlab for lowpass @ 10 kHz */
		lpCoeffs = FloatArray.newFrom(	[-3.5182e-05,-0.0015057,-0.0049337,-0.0069663,-0.0032395,0.0033051,0.0036945,-0.0027759,-0.0046698,0.0026512,0.0061951,-0.0024016,-0.0081783,0.0018642,0.010632,-0.000894,-0.013598,-0.00065739,0.01719,0.0030058,-0.021633,-0.0065155,0.027357,0.011863,-0.035302,-0.020541,0.047824,0.036685,-0.072758,-0.077662,0.15995,0.43894,0.43894,0.15995,-0.077662,-0.072758,0.036685,0.047824,-0.020541,-0.035302,0.011863,0.027357,-0.0065155,-0.021633,0.0030058,0.01719,-0.00065739,-0.013598,-0.000894,0.010632,0.0018642,-0.0081783,-0.0024016,0.0061951,0.0026512,-0.0046698,-0.0027759,0.0036945,0.0033051,-0.0032395,-0.0069663,-0.0049337,-0.0015057,-3.5182e-05]);

		lpBuffer !? { lpBuffer.free; lpBuffer = nil };
		lpBuffer = Buffer.sendCollection(Server.default, lpCoeffs, 1, -1,
			{ /* |b| (b.numFrames.asString + 'lpCoeffs loaded.').postln */ });

		/* FIR coeffs generated in Matlab for highpass @ 100 Hz */
		hpCoeffs = FloatArray.newFrom(
			[0.010487,0.00020157,0.00020376,0.00020529,0.00020743,0.00020891,0.00021099,0.0002124,
		0.00021442,0.00021576,0.00021775,0.00021903,0.00022097,0.00022219,0.00022411,0.00022526,
		0.00022717,0.00022829,0.00023019,0.00023129,0.00023322,0.0002343,0.00023629,0.00023736,
		0.00023945,0.00024053,0.00024273,0.00024384,0.00024617,0.00024726,0.00024978,0.00025076,
		0.00025339,0.00025411,0.00025682,0.00025685,0.00025946,0.0002577,0.00025994,0.00025043,
		0.00026339,0.00026691,0.00026308,0.00026641,0.00026568,0.00026797,0.00026787,0.00026969,
		0.00026983,0.00027134,0.0002716,0.00027294,0.00027326,0.00027443,0.00027482,0.00027586,
		0.00027625,0.0002772,0.0002776,0.00027844,0.00027883,0.00027957,0.00027992,0.00028053,
		0.00028082,0.00028129,0.00028145,0.00028174,0.0002818,0.00028187,0.00028186,0.00028177,
		0.00028174,0.00028155,0.00028169,0.00028157,0.00028216,0.00028205,0.00028327,0.00028085,
		0.0002789,0.00028141,0.00027955,0.00027996,0.0002788,0.00027883,0.00027778,0.00027762,
		0.0002766,0.00027629,0.00027523,0.00027478,0.00027369,0.0002731,0.00027194,0.00027123,
		0.00026996,0.00026912,0.00026776,0.00026678,0.00026531,0.00026419,0.00026263,0.0002614,
		0.00025975,0.00025846,0.00025674,0.00025535,0.0002536,0.0002521,0.00025025,0.00024862,
		0.00024661,0.00024473,0.00024246,0.0002403,0.00023792,0.00023579,0.00023401,0.0002328,
		0.00022864,0.00022716,0.0002244,0.00022215,0.00021937,0.00021695,0.00021405,0.00021148,
		0.00020847,0.00020577,0.00020264,0.00019978,0.00019654,0.00019355,0.00019017,0.00018707,
		0.00018358,0.00018035,0.00017676,0.00017342,0.0001697,0.00016623,0.00016238,0.00015878,
		0.00015476,0.000151,0.00014685,0.0001429,0.00013862,0.00013458,0.00013017,0.00012605,
		0.0001216,0.00011747,0.00011291,0.00010859,0.00010367,9.8967e-05,9.3619e-05,8.9915e-05,
		8.4542e-05,7.9551e-05,7.4576e-05,6.9499e-05,6.4308e-05,5.9153e-05,5.3809e-05,4.8531e-05,
		4.3034e-05,3.7641e-05,3.2021e-05,2.6493e-05,2.0733e-05,1.5095e-05,9.1709e-06,3.3907e-06,
		-2.669e-06,-8.6059e-06,-1.4814e-05,-2.0885e-05,-2.723e-05,-3.3426e-05,-3.9899e-05,-4.6196e-05,
		-5.2778e-05,-5.921e-05,-6.5882e-05,-7.2448e-05,-7.9299e-05,-8.5988e-05,-9.3025e-05,-9.9917e-05,
		-0.00010714,-0.00011413,-0.0001214,-0.00012836,-0.0001357,-0.00014279,-0.00015082,-0.00015808,
		-0.00016539,-0.00017321,-0.0001808,-0.00018856,-0.00019638,-0.00020423,-0.0002122,-0.00022016,
		-0.00022828,-0.00023637,-0.00024462,-0.00025284,-0.00026124,-0.00026955,-0.00027807,-0.00028651,
		-0.00029513,-0.00030368,-0.00031242,-0.00032109,-0.00032997,-0.00033877,-0.00034779,-0.00035673,
		-0.00036586,-0.00037496,-0.0003842,-0.00039337,-0.00040275,-0.00041198,-0.00042142,-0.00043077,
		-0.00044038,-0.00044992,-0.00045972,-0.00046937,-0.00047919,-0.00048869,-0.00049864,-0.00050884,
		-0.00051858,-0.00052869,-0.00053878,-0.00054889,-0.00055912,-0.00056933,-0.00057967,-0.00058996,
		-0.00060039,-0.00061079,-0.00062133,-0.0006318,-0.00064246,-0.00065305,-0.00066379,-0.0006745,
		-0.00068535,-0.00069614,-0.00070709,-0.00071797,-0.00072899,-0.00073994,-0.00075103,-0.00076209,
		-0.00077324,-0.00078438,-0.00079568,-0.00080688,-0.00081828,-0.00082962,-0.00084108,-0.00085243,
		-0.00086391,-0.00087533,-0.00088693,-0.00089852,-0.00091028,-0.00092189,-0.00093333,-0.00094524,
		-0.00095699,-0.00096869,-0.00098059,-0.00099235,-0.0010043,-0.0010161,-0.0010281,-0.00104,
		-0.0010521,-0.0010641,-0.0010762,-0.0010882,-0.0011003,-0.0011124,-0.0011246,-0.0011367,
		-0.0011489,-0.0011611,-0.0011733,-0.0011855,-0.0011979,-0.0012101,-0.0012225,-0.0012348,
		-0.0012472,-0.0012594,-0.0012719,-0.0012842,-0.0012966,-0.001309,-0.0013215,-0.0013339,
		-0.0013464,-0.0013588,-0.0013712,-0.0013836,-0.0013961,-0.0014087,-0.0014211,-0.0014335,
		-0.0014462,-0.0014585,-0.0014712,-0.0014836,-0.0014962,-0.0015086,-0.0015212,-0.0015336,
		-0.0015461,-0.0015585,-0.0015711,-0.0015835,-0.0015961,-0.0016085,-0.001621,-0.0016334,
		-0.0016459,-0.0016583,-0.0016708,-0.0016831,-0.0016956,-0.0017079,-0.0017203,-0.0017326,
		-0.0017451,-0.0017573,-0.0017697,-0.001782,-0.0017943,-0.0018064,-0.0018187,-0.0018309,
		-0.0018432,-0.0018553,-0.0018675,-0.0018796,-0.0018916,-0.0019037,-0.0019159,-0.0019277,
		-0.0019398,-0.0019516,-0.0019637,-0.0019755,-0.0019874,-0.0019992,-0.0020111,-0.0020227,
		-0.0020346,-0.0020462,-0.0020579,-0.0020694,-0.0020811,-0.0020925,-0.0021041,-0.0021154,
		-0.0021269,-0.0021382,-0.0021496,-0.0021608,-0.0021722,-0.0021833,-0.0021945,-0.0022055,
		-0.0022166,-0.0022275,-0.0022385,-0.0022494,-0.0022603,-0.002271,-0.0022818,-0.0022924,
		-0.0023031,-0.0023136,-0.0023242,-0.0023346,-0.0023451,-0.0023553,-0.0023658,-0.0023758,
		-0.0023861,-0.0023961,-0.0024062,-0.0024161,-0.0024261,-0.0024358,-0.0024457,-0.0024553,
		-0.002465,-0.0024746,-0.0024841,-0.0024935,-0.002503,-0.0025122,-0.0025215,-0.0025305,
		-0.0025397,-0.0025486,-0.0025576,-0.0025664,-0.0025753,-0.0025839,-0.0025926,-0.0026011,
		-0.0026096,-0.002618,-0.0026263,-0.0026344,-0.0026427,-0.0026507,-0.0026588,-0.0026666,
		-0.0026745,-0.0026821,-0.0026899,-0.0026974,-0.002705,-0.0027122,-0.0027196,-0.0027268,
		-0.002734,-0.002741,-0.002748,-0.0027548,-0.0027617,-0.0027683,-0.002775,-0.0027814,
		-0.0027879,-0.0027941,-0.0028004,-0.0028065,-0.0028126,-0.0028185,-0.0028244,-0.0028301,
		-0.0028358,-0.0028413,-0.0028468,-0.0028521,-0.0028575,-0.0028626,-0.0028677,-0.0028727,
		-0.0028775,-0.0028823,-0.0028871,-0.0028915,-0.0028961,-0.0029004,-0.0029047,-0.0029089,
		-0.002913,-0.0029169,-0.0029208,-0.0029245,-0.0029283,-0.0029318,-0.0029353,-0.0029386,
		-0.0029419,-0.002945,-0.002948,-0.0029509,-0.0029538,-0.0029565,-0.0029592,-0.0029616,
		-0.0029641,-0.0029663,-0.0029686,-0.0029706,-0.0029727,-0.0029745,-0.0029763,-0.0029779,
		-0.0029796,-0.0029809,-0.0029823,-0.0029835,-0.0029847,-0.0029857,-0.0029866,-0.0029874,
		-0.0029881,-0.0029886,-0.0029893,-0.0029896,-0.0029899,-0.00299,0.99701,-0.00299,-0.0029899,
		-0.0029896,-0.0029893,-0.0029886,-0.0029881,-0.0029874,-0.0029866,-0.0029857,-0.0029847,
		-0.0029835,-0.0029823,-0.0029809,-0.0029796,-0.0029779,-0.0029763,-0.0029745,-0.0029727,
		-0.0029706,-0.0029686,-0.0029663,-0.0029641,-0.0029616,-0.0029592,-0.0029565,-0.0029538,
		-0.0029509,-0.002948,-0.002945,-0.0029419,-0.0029386,-0.0029353,-0.0029318,-0.0029283,
		-0.0029245,-0.0029208,-0.0029169,-0.002913,-0.0029089,-0.0029047,-0.0029004,-0.0028961,
		-0.0028915,-0.0028871,-0.0028823,-0.0028775,-0.0028727,-0.0028677,-0.0028626,-0.0028575,
		-0.0028521,-0.0028468,-0.0028413,-0.0028358,-0.0028301,-0.0028244,-0.0028185,-0.0028126,
		-0.0028065,-0.0028004,-0.0027941,-0.0027879,-0.0027814,-0.002775,-0.0027683,-0.0027617,
		-0.0027548,-0.002748,-0.002741,-0.002734,-0.0027268,-0.0027196,-0.0027122,-0.002705,
		-0.0026974,-0.0026899,-0.0026821,-0.0026745,-0.0026666,-0.0026588,-0.0026507,-0.0026427,
		-0.0026344,-0.0026263,-0.002618,-0.0026096,-0.0026011,-0.0025926,-0.0025839,-0.0025753,
		-0.0025664,-0.0025576,-0.0025486,-0.0025397,-0.0025305,-0.0025215,-0.0025122,-0.002503,
		-0.0024935,-0.0024841,-0.0024746,-0.002465,-0.0024553,-0.0024457,-0.0024358,-0.0024261,
		-0.0024161,-0.0024062,-0.0023961,-0.0023861,-0.0023758,-0.0023658,-0.0023553,-0.0023451,
		-0.0023346,-0.0023242,-0.0023136,-0.0023031,-0.0022924,-0.0022818,-0.002271,-0.0022603,
		-0.0022494,-0.0022385,-0.0022275,-0.0022166,-0.0022055,-0.0021945,-0.0021833,-0.0021722,
		-0.0021608,-0.0021496,-0.0021382,-0.0021269,-0.0021154,-0.0021041,-0.0020925,-0.0020811,
		-0.0020694,-0.0020579,-0.0020462,-0.0020346,-0.0020227,-0.0020111,-0.0019992,-0.0019874,
		-0.0019755,-0.0019637,-0.0019516,-0.0019398,-0.0019277,-0.0019159,-0.0019037,-0.0018916,
		-0.0018796,-0.0018675,-0.0018553,-0.0018432,-0.0018309,-0.0018187,-0.0018064,-0.0017943,
		-0.001782,-0.0017697,-0.0017573,-0.0017451,-0.0017326,-0.0017203,-0.0017079,-0.0016956,
		-0.0016831,-0.0016708,-0.0016583,-0.0016459,-0.0016334,-0.001621,-0.0016085,-0.0015961,
		-0.0015835,-0.0015711,-0.0015585,-0.0015461,-0.0015336,-0.0015212,-0.0015086,-0.0014962,
		-0.0014836,-0.0014712,-0.0014585,-0.0014462,-0.0014335,-0.0014211,-0.0014087,-0.0013961,
		-0.0013836,-0.0013712,-0.0013588,-0.0013464,-0.0013339,-0.0013215,-0.001309,-0.0012966,
		-0.0012842,-0.0012719,-0.0012594,-0.0012472,-0.0012348,-0.0012225,-0.0012101,-0.0011979,
		-0.0011855,-0.0011733,-0.0011611,-0.0011489,-0.0011367,-0.0011246,-0.0011124,-0.0011003,
		-0.0010882,-0.0010762,-0.0010641,-0.0010521,-0.00104,-0.0010281,-0.0010161,-0.0010043,
		-0.00099235,-0.00098059,-0.00096869,-0.00095699,-0.00094524,-0.00093333,-0.00092189,-0.00091028,
		-0.00089852,-0.00088693,-0.00087533,-0.00086391,-0.00085243,-0.00084108,-0.00082962,-0.00081828,
		-0.00080688,-0.00079568,-0.00078438,-0.00077324,-0.00076209,-0.00075103,-0.00073994,-0.00072899,
		-0.00071797,-0.00070709,-0.00069614,-0.00068535,-0.0006745,-0.00066379,-0.00065305,-0.00064246,
		-0.0006318,-0.00062133,-0.00061079,-0.00060039,-0.00058996,-0.00057967,-0.00056933,-0.00055912,
		-0.00054889,-0.00053878,-0.00052869,-0.00051858,-0.00050884,-0.00049864,-0.00048869,-0.00047919,
		-0.00046937,-0.00045972,-0.00044992,-0.00044038,-0.00043077,-0.00042142,-0.00041198,-0.00040275,
		-0.00039337,-0.0003842,-0.00037496,-0.00036586,-0.00035673,-0.00034779,-0.00033877,-0.00032997,
		-0.00032109,-0.00031242,-0.00030368,-0.00029513,-0.00028651,-0.00027807,-0.00026955,-0.00026124,
		-0.00025284,-0.00024462,-0.00023637,-0.00022828,-0.00022016,-0.0002122,-0.00020423,-0.00019638,
		-0.00018856,-0.0001808,-0.00017321,-0.00016539,-0.00015808,-0.00015082,-0.00014279,-0.0001357,
		-0.00012836,-0.0001214,-0.00011413,-0.00010714,-9.9917e-05,-9.3025e-05,-8.5988e-05,-7.9299e-05,
		-7.2448e-05,-6.5882e-05,-5.921e-05,-5.2778e-05,-4.6196e-05,-3.9899e-05,-3.3426e-05,-2.723e-05,
		-2.0885e-05,-1.4814e-05,-8.6059e-06,-2.669e-06,3.3907e-06,9.1709e-06,1.5095e-05,2.0733e-05,
		2.6493e-05,3.2021e-05,3.7641e-05,4.3034e-05,4.8531e-05,5.3809e-05,5.9153e-05,6.4308e-05,
		6.9499e-05,7.4576e-05,7.9551e-05,8.4542e-05,8.9915e-05,9.3619e-05,9.8967e-05,0.00010367,
		0.00010859,0.00011291,0.00011747,0.0001216,0.00012605,0.00013017,0.00013458,0.00013862,
		0.0001429,0.00014685,0.000151,0.00015476,0.00015878,0.00016238,0.00016623,0.0001697,
		0.00017342,0.00017676,0.00018035,0.00018358,0.00018707,0.00019017,0.00019355,0.00019654,
		0.00019978,0.00020264,0.00020577,0.00020847,0.00021148,0.00021405,0.00021695,0.00021937,
		0.00022215,0.0002244,0.00022716,0.00022864,0.0002328,0.00023401,0.00023579,0.00023792,
		0.0002403,0.00024246,0.00024473,0.00024661,0.00024862,0.00025025,0.0002521,0.0002536,
		0.00025535,0.00025674,0.00025846,0.00025975,0.0002614,0.00026263,0.00026419,0.00026531,
		0.00026678,0.00026776,0.00026912,0.00026996,0.00027123,0.00027194,0.0002731,0.00027369,
		0.00027478,0.00027523,0.00027629,0.0002766,0.00027762,0.00027778,0.00027883,0.0002788,
		0.00027996,0.00027955,0.00028141,0.0002789,0.00028085,0.00028327,0.00028205,0.00028216,
		0.00028157,0.00028169,0.00028155,0.00028174,0.00028177,0.00028186,0.00028187,0.0002818,
		0.00028174,0.00028145,0.00028129,0.00028082,0.00028053,0.00027992,0.00027957,0.00027883,
		0.00027844,0.0002776,0.0002772,0.00027625,0.00027586,0.00027482,0.00027443,0.00027326,
		0.00027294,0.0002716,0.00027134,0.00026983,0.00026969,0.00026787,0.00026797,0.00026568,
		0.00026641,0.00026308,0.00026691,0.00026339,0.00025043,0.00025994,0.0002577,0.00025946,
		0.00025685,0.00025682,0.00025411,0.00025339,0.00025076,0.00024978,0.00024726,0.00024617,
		0.00024384,0.00024273,0.00024053,0.00023945,0.00023736,0.00023629,0.0002343,0.00023322,
		0.00023129,0.00023019,0.00022829,0.00022717,0.00022526,0.00022411,0.00022219,0.00022097,
		0.00021903,0.00021775,0.00021576,0.00021442,0.0002124,0.00021099,0.00020891,0.00020743,
		0.00020529,0.00020376,0.00020157,0.010487,0,0,0] );

		hpBuffer !? { hpBuffer.free; hpBuffer = nil };
		hpBuffer = Buffer.sendCollection(Server.default, hpCoeffs, 1, -1,
			{ /* |b| (b.numFrames.asString + 'hpCoeffs loaded.').postln */ } );
	}


	*compile { | libname, triggerIDEOF, triggerIDClip, mainGroupId, nHarmonics, calTone=false, logRate=0, arrayRecordExtraInputs |
		this.loadCoeffs;

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Live Input SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameLiveInput,
			{ | aiBusMic, aiBusEGG, aoBusMic, aoBusEGG, aoBusConditionedMic, aoBusConditionedEGG |
				var inMic, inEGG, micCond, eggCond, bClip;

				inMic = SoundIn.ar(aiBusMic);       // Get input from a live source
				inEGG = SoundIn.ar(aiBusEGG);

				bClip = PeakFollower.ar(inEGG.madd(1, -0.995), 0.9999); // Detect when EGG signal is too large
				SendTrig.ar(bClip, triggerIDClip, bClip);			    // Tell main about clipping

				micCond = HPF.ar(inMic, 30);				            // HPF +12 db/oct to remove rumble
				eggCond   = Convolution2.ar(inEGG, hpBuffer.bufnum, 0, hpBuffer.numFrames);	// HP @100 Hz
				eggCond	= Median.ar(9, eggCond);											// suppress EG2 "crackle"
				eggCond = Convolution2.ar(eggCond, lpBuffer.bufnum, 0, lpBuffer.numFrames);	// LP @10 kHz

				Out.ar(aoBusMic, [inMic]);                              // Feed the raw input to aoBusMic
				Out.ar(aoBusEGG, [inEGG]);                              // Feed the raw input to aoBusEGG
				Out.ar(aoBusConditionedMic, [micCond]);
				Out.ar(aoBusConditionedEGG, [eggCond]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Disk Input SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameDiskInput,
			{ | iBufferDisk, aoBusMic, aoBusEGG, aoBusConditionedMic, aoBusConditionedEGG |
				var inMic, inEGG, micCond, eggCond, eofGate;

				#inMic, inEGG = DiskIn.ar(2, iBufferDisk);		// Add a Done.kr to stop on end-of-file

				eofGate = Done.kr([inMic, inEGG]);
				SendTrig.kr(eofGate, triggerIDEOF, -1);  // Notify about end-of-file
				Pause.kr( 1 - eofGate, mainGroupId ); // Pause the main group (all synths)

				micCond = HPF.ar(inMic, 30);				    // HPF +12 db/oct to remove rumble
				eggCond   = Convolution2.ar(inEGG, hpBuffer.bufnum, 0, hpBuffer.numFrames);	// HP @100 Hz
				eggCond	= Median.ar(9, eggCond);												// suppress EG2 "crackle"
				eggCond = Convolution2.ar(eggCond, lpBuffer.bufnum, 0, lpBuffer.numFrames);	// LP @10 kHz

				Out.ar(aoBusMic, [inMic]);                      // Feed the raw input to aoBusMic
				Out.ar(aoBusEGG, [inEGG]);                      // Feed the raw input to aoBusEGG
				Out.ar(aoBusConditionedMic, [micCond]);
				Out.ar(aoBusConditionedEGG, [eggCond]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Echo Microphone SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		if (calTone==true, {
			SynthDef(nameEchoMicrophone,
				{ | aiBusMic, aoBusSpeaker |
					Out.ar(aoBusSpeaker, [In.ar(aiBusMic), SinOsc.ar(250, 0, 0.3)]);
				}
			).add(libname);
		},{
			SynthDef(nameEchoMicrophone,
				{ | aiBusMic, aoBusSpeaker |
					Out.ar(aoBusSpeaker, In.ar(aiBusMic) ! 2);
				}
			).add(libname);
		});

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write Audio SynthDef
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteAudio,
			{ | aiBusMic, aiBusEGG, oBufferAudio |
				GatedDiskOut.ar(oBufferAudio, 1, [ In.ar(aiBusMic), In.ar(aiBusEGG) ]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write the EGG signal and the new cycle markers
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteCycleDetectionLog,
			{ | aiBusEGG, aiBusGateCycle, oBufferLog |
				GatedDiskOut.ar(oBufferLog, 1, [In.ar(aiBusEGG), In.ar(aiBusGateCycle)]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write points
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWritePoints,
			{ | aiBusGate,
				aiBusDeltaAmplitudeFirst,
				aiBusDeltaPhaseFirst,
				oBuffer |

				var gate = In.ar(aiBusGate);
				var damps = In.ar(aiBusDeltaAmplitudeFirst, nHarmonics - 1);
				var dphases = In.ar(aiBusDeltaPhaseFirst, nHarmonics - 1);

				GatedDiskOut.ar(oBuffer, gate, damps ++ dphases);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write SampEn
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteSampEn,
			{ | aiBusGate,
				aiBusSampEn,
				oBuffer |

				var gate = In.ar(aiBusGate);
				var sampen = In.ar(aiBusSampEn);

				GatedDiskOut.ar(oBuffer, gate, [sampen]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write Frequency Amplitude
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteFreqAmp,
			{ | aiBusGate,
				aiBusFrequency,
				aiBusAmplitude,
				oBuffer |

				var gate = In.ar(aiBusGate);
				var freq = In.ar(aiBusFrequency);
				var amp = In.ar(aiBusAmplitude);

				GatedDiskOut.ar(oBuffer, gate, [freq, amp]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write Log
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteLog,
			{ | aiBusGate,
				aiBusTimestamp,
				aiBusFrequency,
				aiBusAmplitude,
				aiBusClarity,
				aiBusCrest,
				aiBusClusterNumber,
				aiBusSampEn,
				aiBusAmplitudeFirst,
				aiBusPhaseFirst,
				oBuffer |

				var gate = Select.ar((logRate > 0).asInteger, [In.ar(aiBusGate), LFPulse.ar(logRate, 0, 0)]);
				var time = In.ar(aiBusTimestamp);
				var freq = In.ar(aiBusFrequency);
				var amp = In.ar(aiBusAmplitude);
				var clarity = In.ar(aiBusClarity);
				var crest = In.ar(aiBusCrest);
				var cluster_number = In.ar(aiBusClusterNumber);
				var sampen = In.ar(aiBusSampEn);
				var amps = In.ar(aiBusAmplitudeFirst, nHarmonics);
				var phases = In.ar(aiBusPhaseFirst, nHarmonics);

				GatedDiskOut.ar(oBuffer, gate, [time, freq, amp, clarity, crest, cluster_number, sampen] ++ amps ++ phases);
			},
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Write Gates
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		SynthDef(nameWriteGates,
			{ | aiBusEGG,
				aiBusConditionedEGG,
				aiBusGateCycle,
				aiBusGateDelayedCycle,
				aiBusGateFilteredDFT,
				oBuffer |

				var egg = In.ar(aiBusEGG);
				var cegg = In.ar(aiBusConditionedEGG);
				var gcycle = In.ar(aiBusGateCycle);
				var gdcycle = In.ar(aiBusGateDelayedCycle);
				var gfdft = In.ar(aiBusGateFilteredDFT);

				GatedDiskOut.ar(oBuffer, 1, [egg, cegg, gcycle, gdcycle, gfdft]);
			}
		).add(libname);

		///////////////////////////////////////////////////////////////////////////////////////////////////////
		// Record extra channels if requested
		///////////////////////////////////////////////////////////////////////////////////////////////////////

		if (arrayRecordExtraInputs.notNil , {
			SynthDef(nameRecordExtraChannels,
				{ | oBuffer |
					var physios = SoundIn.ar(arrayRecordExtraInputs);
					var gate = LFPulse.ar(100, 0, 0);  //hard-coded frame rate 100 Hz
					GatedDiskOut.ar(oBuffer, gate, physios);
				}
			).add(libname);
		});

	}

	*liveInput { | aiBusMic, aiBusEGG, aoBusMic, aoBusEGG, aoBusConditionedMic, aoBusConditionedEGG ...args |
		^Array.with(nameLiveInput,
			[
				\aiBusMic, aiBusMic,
				\aiBusEGG, aiBusEGG,
				\aoBusMic, aoBusMic,
				\aoBusEGG, aoBusEGG,
				\aoBusConditionedMic, aoBusConditionedMic,
				\aoBusConditionedEGG, aoBusConditionedEGG
			],
			*args
		);
	}

	*diskInput { | iBufferDisk, aoBusMic, aoBusEGG, aoBusConditionedMic, aoBusConditionedEGG ...args |
		^Array.with(nameDiskInput,
			[
				\iBufferDisk, iBufferDisk,
				\aoBusMic, aoBusMic,
				\aoBusEGG, aoBusEGG,
				\aoBusConditionedMic, aoBusConditionedMic,
				\aoBusConditionedEGG, aoBusConditionedEGG
			],
			*args
		);
	}

	*echoMicrophone { | aiBusMic, aoBusSpeaker ...args |
		^Array.with(nameEchoMicrophone,
			[
				\aiBusMic, aiBusMic,
				\aoBusSpeaker, aoBusSpeaker
			],
			*args
		);
	}

	*writeAudio { | aiBusMic, aiBusEGG, oBufferAudio ...args |
		^Array.with(nameWriteAudio,
			[
				\aiBusMic, aiBusMic,
				\aiBusEGG, aiBusEGG,
				\oBufferAudio, oBufferAudio
			],
			*args
		);
	}

	*writeCycleDetectionLog { | aiBusEGG, aiBusGateCycle, oBufferLog ...args |
		^Array.with(nameWriteCycleDetectionLog,
			[
				\aiBusEGG, aiBusEGG,
				\aiBusGateCycle, aiBusGateCycle,
				\oBufferLog, oBufferLog
			],
			*args
		);
	}

	*writePoints { |
		aiBusGate,
		aiBusDeltaAmplitudeFirst,
		aiBusDeltaPhaseFirst,
		oBuffer
		...args |

		^Array.with(nameWritePoints,
			[
				\aiBusGate, aiBusGate,
				\aiBusDeltaAmplitudeFirst, aiBusDeltaAmplitudeFirst,
				\aiBusDeltaPhaseFirst, aiBusDeltaPhaseFirst,
				\oBuffer, oBuffer
			],
			*args
		);
	}

	*writeSampEn { |
		aiBusGate,
		aiBusSampEn,
		oBuffer
		...args |

		^Array.with(nameWriteSampEn,
			[
				\aiBusGate, aiBusGate,
				\aiBusSampEn, aiBusSampEn,
				\oBuffer, oBuffer
			],
			*args
		);
	}

	*writeFrequencyAmplitude { |
		aiBusGate,
		aiBusFrequency,
		aiBusAmplitude,
		oBuffer
		...args |

		^Array.with(nameWriteFreqAmp,
			[
				\aiBusGate, aiBusGate,
				\aiBusFrequency, aiBusFrequency,
				\aiBusAmplitude, aiBusAmplitude,
				\oBuffer, oBuffer
			],
			*args
		);
	}

	*writeLog { |
		aiBusGate,
		aiBusTimestamp,
		aiBusFrequency,
		aiBusAmplitude,
		aiBusClarity,
		aiBusCrest,
		aiBusClusterNumber,
		aiBusSampEn,
		aiBusAmplitudeFirst,
		aiBusPhaseFirst,
		oBuffer
		...args |

		^Array.with(nameWriteLog,
			[
				\aiBusGate, aiBusGate,
				\aiBusTimestamp, aiBusTimestamp,
				\aiBusFrequency, aiBusFrequency,
				\aiBusAmplitude, aiBusAmplitude,
				\aiBusClarity, aiBusClarity,
				\aiBusCrest, aiBusCrest,
				\aiBusClusterNumber, aiBusClusterNumber,
				\aiBusSampEn, aiBusSampEn,
				\aiBusAmplitudeFirst, aiBusAmplitudeFirst,
				\aiBusPhaseFirst, aiBusPhaseFirst,
				\oBuffer, oBuffer
			],
			*args
		);
	}

	*writeGates { |
		aiBusEGG,
		aiBusConditionedEGG,
		aiBusGateCycle,
		aiBusGateDelayedCycle,
		aiBusGateFilteredDFT,
		oBuffer
		...args |

		^Array.with(nameWriteGates,
			[
				\aiBusEGG, aiBusEGG,
				\aiBusConditionedEGG, aiBusConditionedEGG,
				\aiBusGateCycle, aiBusGateCycle,
				\aiBusGateDelayedCycle, aiBusGateDelayedCycle,
				\aiBusGateFilteredDFT, aiBusGateFilteredDFT,
				\oBuffer, oBuffer
			],
			*args
		);
	}

	*recordExtraChannels { |
		oBuffer
		...args |

		^Array.with(nameRecordExtraChannels,
			[
				\oBuffer, oBuffer
			],
			*args
		);
	}

}