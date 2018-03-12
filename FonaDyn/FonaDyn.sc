/* This installation assumes that files have been unpacked as follows

...\Extensions\FonaDyn: including FonaDyn.sc and Grid-ST.txt
...\Extensions\FonaDynTools: including the subfolders with the .scx or .so files,
	...\FonaDynTools\windows, including pthreadGC2.dll
	...\FonaDynTools\osx, including a recompiled PitchDetection.scx and libfftw3f.3.dylib
	...\FonaDynTools\linux

Two of the three subfolders will be deleted during the installation.
*/

FonaDyn {

	*run {
		var setPathStr;
		var resDir = Platform.resourceDir;
		var currentPath = "PATH".getenv;
		if (currentPath.find(resDir, true).isNil, {
			setPathStr = resDir ++ ";" ++ currentPath;
			"PATH".setenv(setPathStr);
		});
		VRPMain.new();
	}

	*setPaths {
		var fdAllUsers, fdExtPath, plugsAllUsers, plugsPath;

		// Find out if user has copied FonaDyn "per-user", or "system-wide"
		fdExtPath = PathName(VRPMain.class.filenameSymbol.asString.standardizePath).parentPath; // classes
		fdExtPath = PathName(fdExtPath).parentPath; // FonaDyn
		fdExtPath = PathName(fdExtPath).parentPath.asString.withoutTrailingSlash; // Extensions
		fdAllUsers = (fdExtPath == thisProcess.platform.systemExtensionDir);

		if (fdAllUsers,
			{ ~fdExtensions = thisProcess.platform.systemExtensionDir; },
			{ ~fdExtensions = thisProcess.platform.userExtensionDir; }
		);
		("Found FonaDyn in" + ~fdExtensions).postln;

		~fdProgram = ~fdExtensions +/+ "FonaDyn";
		~fdProgramTools = ~fdExtensions +/+ "FonaDynTools";

		// Find out if user has installed SC3Plugins "per-user", or "system-wide"
		plugsPath = PathName(Tartini.class.filenameSymbol.asString.standardizePath).parentPath; // classes
		plugsPath = PathName(plugsPath).parentPath; // PitchDetection
		plugsPath = PathName(plugsPath).parentPath; // SC3plugins
		plugsPath = PathName(plugsPath).parentPath.asString.withoutTrailingSlash; // Extensions
		plugsAllUsers = (plugsPath == thisProcess.platform.systemExtensionDir);

		if (plugsAllUsers,
			{ ~plugsExtensions = thisProcess.platform.systemExtensionDir; },
			{ ~plugsExtensions = thisProcess.platform.userExtensionDir; }
		);
		("Found SC3-plugins in" + ~plugsExtensions).postln;

	}

	*removeFolder { arg folder;
		var rmCmd;

		rmCmd = Platform.case(
			\windows, { "rmdir /s /q" },
			\osx,     { "rm -R" },
			\linux,   { "rm -R" }
		);

		rmCmd = rmCmd +  "\"" ++ folder ++ "\"";
		rmCmd.postln;
		rmCmd.unixCmd;
	}

	*install {
		var success;
		var dirName, fName;

		FonaDyn.setPaths;

		if (Main.versionAtMost(3,7),
			{
				postln ("FonaDyn will run best on SuperCollider 3.8.0 or higher.");
				postln ("This SuperCollider is at version" + Main.version + ".");
				postln ("Some FonaDyn functionality may be limited or incorrect.");
		});

		// Check that the SC3 plugins are installed
		// and post instructions if they are not.
		if (thisProcess.platform.hasFeature(\Tartini).not,
			{ FonaDyn.promptInstallSC3plugins;
				^"Then re-run this installation." }
		);

		// Move the log-grid option to the proper location
		// The .txt file is ignored, it can stay.
		// fName = PathName(~fdExtensions +/+ "SystemOverwrites" +/+ "Grid-ST.sc");

		dirName = ~fdExtensions +/+ "SystemOverwrites" ;
		dirName.mkdir;
		fName = dirName +/+ "Grid-ST.sc";
		if (File.exists(fName),
			{ (fName + "exists - ok,").postln },
			{ File.copy(~fdProgram +/+ "Grid-ST.txt", fName)}
		);

		success = Platform.case(
			\windows, { FonaDyn.install_win },
			\osx,     { FonaDyn.install_osx },
			\linux,   { FonaDyn.install_linux }
		);

		if (success == true,
			{ postln ("FonaDyn was installed successfully.") },
			{ postln ("There was a problem with the installation.")}
		);
	}

	*install_win {
		var retval = false;
		// Copy the file pthreadGC2.dll to where it belongs
		var dllName =  "pthreadGC2.dll";
		var srcPath = ~fdProgramTools +/+ "windows";
		var destPath = thisProcess.platform.resourceDir;

		var destName = destPath +/+ dllName;
		if (File.exists(destName).not, { File.copy(srcPath +/+ dllName, destName) } );
		postln ("Copied" + dllName + "to" + destName);
		FonaDyn.removeFolder(~fdProgramTools +/+ "osx");
		FonaDyn.removeFolder(~fdProgramTools +/+ "linux");
		^retval = true
	}

	*install_osx {
		var retval = false;
		// Rename the original PitchDetection.scx so that ours becomes the active one
		var scxName = "PitchDetection/PitchDetection.scx";
		var fftwLibPath = "/usr/local/lib";
		var fftwLibName = "libfftw3f.3.dylib";
		var destPath;
		var cmdLine;
		destPath = ~plugsExtensions +/+ "SC3plugins" +/+ scxName;
		if (File.exists(destPath), {
			cmdLine ="mv \"" ++ destPath ++ "\" \"" ++ destPath ++".original\"";
			cmdLine.postln;
			cmdLine.unixCmd;
			postln (scxName + "overridden.");
		},{
			("Did not find "+ scxName).postln;
		});

		// Install the FFTW library where our recompiled PitchDetection.scx expects to find it
		// destPath = fftwLibPath +/+ fftwLibName;
		if (File.exists(destPath),
			{ (destPath + "exists - ok,").postln },
			{
				var srcPath = ~fdProgramTools +/+ "osx" +/+ fftwLibName;
				cmdLine ="install -CSpv \"" ++ srcPath ++ "\" " ++ fftwLibPath;
				cmdLine.postln;
				cmdLine.unixCmd;
			};
		);

		FonaDyn.removeFolder(~fdProgramTools +/+ "windows");
		FonaDyn.removeFolder(~fdProgramTools +/+ "linux");
		^true
	}

	*install_linux {
		FonaDyn.removeFolder(~fdProgramTools +/+ "windows");
		FonaDyn.removeFolder(~fdProgramTools +/+ "osx");
		^true
	}

	*uninstall {
		warn("This removes all FonaDyn code, including any changes you have made.");
		FonaDyn.setPaths;
		FonaDyn.removeFolder(~fdProgram);
		FonaDyn.removeFolder(~fdProgramTools);
	}

	*promptInstallSC3plugins {
		postln ("The \"SC3 plugins\" are not yet installed.");
		postln ("Download the version for your system,");
		postln ("and follow the instructions in its file README.txt.");
	}

} /* class FonaDyn */


