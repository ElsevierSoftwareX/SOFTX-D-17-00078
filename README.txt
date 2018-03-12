/*  FonaDyn installation README.TXT
    ===============================

The following procedure performs a basic install, using SuperCollider's default audio device.
We recommend that you first unzip the file "FonaDyn_Handbook.pdf" and read the chapter "Software Setup". 
If your computer is centrally managed, show that chapter to your IT support staff. 
Follow this file only if you are impatient, savvy and have admin rights on your computer.

Once FonaDyn is up and running, you may want to tweak various settings.
These, too, are described in the FonaDyn Handbook.

Install SuperCollider on your computer. 
The download is at http://supercollider.github.io/download.
DO NOT use version 3.9.0 - it will not work. Use only 3.8.0 or 3.8.1.

Download also the corresponding version of the "sc3-plugins"
and follow the instructions in its README file.


To install FonaDyn into your SuperCollider system, follow these steps:

1) Unpack the folders FonaDyn and FonaDynTools from this ZIP file
into folders with the same names.

2) Run SuperCollider (scide.exe) and wait for the "Post window"
to display this line:

  *** Welcome to SuperCollider 3.8.0. *** For help press Ctrl-D.

3) Choose File > New . This opens a new text file "Untitled" for editing.

4) In the window "Untitled", type

	Platform.userExtensionDir;    // if only you will be using FonaDyn on this computer
or
	Platform.systemExtensionDir;  // if you want all users on the computer to use FonaDyn

("Dir" is short for "directory" which is synonymous with "folder")

5) With the cursor still on that line, execute the line by pressing Shift+Enter.
Or, in the popup menu "Language", choose "Evaluate file".

SuperCollider will now print the full pathname
of the appropriate Extensions folder in its "Post window".

6) Move the unpacked folders FonaDyn and FonaDynTools to that folder.

7) Exit SuperCollider and restart it.
Or, choose "Language" > "Recompile Class Library" (Ctrl+Shift+L).

8) In a new text window, evaluate the line

	FonaDyn.install;

Note any messages that appear in the "Post window".

If the last message is "FonaDyn was installed successfully.",
then celebrate, briefly.
Now boot the SC server: press Ctrl+B and wait a few seconds.

To start FonaDyn, evaluate the line

	FonaDyn.run;


To uninstall FonaDyn (no questions asked!), evaluate the line

	FonaDyn.uninstall;

WARNING: this deletes everything in the folders FonaDyn and FonaDynTools,
including any changes you may have made to the source code.

*/

