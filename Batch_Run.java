
import java.io.File;
import java.io.*;
import java.util.*;

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.gui.GenericDialog;

/**
 * This plugIn Batch_Run starts the batch processing for analyzing the videos.
 * The Input- and Output-Folders can be chosen with a user dialog, and the
 * analysis for each video is invoked. Furthermore it can be chosen (dialog
 * window), whether to analyze the fly movement, the preference index or both.
 * 
 */

public class Batch_Run implements PlugIn {

	private Video_Analysis bs = new Video_Analysis();
	private AVI_Writer_WD aw = new AVI_Writer_WD();

	protected static String filename = ""; // source filename
	protected static String myDir1 = ""; // Source Directory
	protected static String myDir2 = ""; // Saving Directory
	
	// HashMap that saves each video as key and the diameters of the arena as value
	protected static HashMap <String, String> diameters = new HashMap <String, String> ();
	
	protected static int arenaDiam; // parameter for diameter for the arena
	
	protected static boolean [] bord = borderSetting(); // parameter for border setting
	
	protected static boolean[] res = inputDialog(); // parameter for types of
													// analysis

	protected static boolean mov = getMovement(); // parameter for Movement
													// analysis (TRUE: do
													// analysis, FALSE: no
													// analysis)
	protected static boolean det = getDetection(); // parameter for preference
													// index (TRUE: do analysis,
													// FALSE: no analysis)

	public void run(String arg) {

		myDir1 = IJ.getDirectory("Select Image Source Folder..."); // Opening
																	// source
																	// folder
																	// window in
																	// ImageJ
		if (myDir1 == null) {
			return; // If no source folder is selected the plugin stops here
		}
		
		String[] myListSources = new File(myDir1).list(); // all the file names
															// in the source
															// folder
		if (myListSources == null) {
			return;
		}
		
		IJ.log("The source image folder chosen was " + myDir1); // opens the log
																// window

		myDir2 = IJ.getDirectory("Select Image Saving Folder..."); // Opening
																	// saving
																	// folder
																	// window in
																	// ImageJ
		if (myDir2 == null) {
			return; // If no saving folder is selected the plugin stops here
		}
		
		IJ.log("The saving folder chosen was " + myDir2);
		
		diamRead ();

		// do analysis for each .avi file in the chosen folder
		for (int FileNumber = 0; FileNumber < myListSources.length; FileNumber++) {

			IJ.showProgress(FileNumber, myListSources.length); // showing
																// progress
			IJ.log((FileNumber + 1) + ":" + myListSources[FileNumber]);
			IJ.showStatus(FileNumber + "/" + myListSources.length);
			

			if (myListSources[FileNumber].contains(".avi")
					&& !(myListSources[FileNumber].contains(".MOV.avi"))
					&& diameters.containsKey(myListSources[FileNumber])) {

				filename = myListSources[FileNumber].substring(0,
						myListSources[FileNumber].length() - 4);
						
				arenaDiam = Integer.parseInt(diameters.get(myListSources[FileNumber]));
				
			
				AVI_Reader_WD ar = new AVI_Reader_WD(); // reading the AVI file
				ar.run("");
				ImagePlus myImPlus = ar.getImagePlus();

				myImPlus.show(); // show video
				ImageProcessor myIp = myImPlus.getProcessor();
				myImPlus.updateAndDraw();

				bs.setup("", myImPlus); // start analysis for video
				bs.run(myIp);

				// ImagePlus imp2 = bs.getImp();
				// ImageProcessor ip2 = imp2.getProcessor();

				if (det == true && (bord[0]==true||bord[1]==true)) {
					try {
						outputFile(myDir2, myImPlus); // write the output
														// .xls-file
					} catch (IOException e) {
						IJ.error("Problem with writing the file");
					}
				}

				// aw.setup ("",imp2); // write the output .avi-file
				// aw.run(ip2);

				// imp2.close();
				myImPlus.close();
			}

		}

		IJ.log("Completed");
	}
	
	// method to open input dialog for diameter file
	// file is tab-separated, each line has the following infos for 
	// one video: Path	Video	Diameter
	public static void diamRead () {
	
		String file = IJ.openAsString("");
		String [] videos = file.split("\n");
		String [] diam = new String [videos.length];
		String [] vid = new String [videos.length];
		
		for (int i = 0; i < videos.length; i++) {
			String [] line = videos[i].split("\t");
			vid[i] = line [1];
			vid[i] = vid[i].replaceAll(".mov", ".avi");
			diam[i] = line [2];
			
			diameters.put(vid[i],diam[i]);
		}
	}
	
	// method to get arena diameter
	public static int getDiam () {
	
		return arenaDiam;
		
	}

	public static boolean[] borderSetting() {
	
		boolean real;
		boolean shift;
		boolean without;
		boolean manual;
		
		boolean[] input = new boolean[4];

		GenericDialog gdBor = new GenericDialog("Border options");

		gdBor.addCheckbox("Choose real border (not shifted)", false);
		gdBor.addCheckbox("Choose shifted border (1.5 mm)", false);
		gdBor.addCheckbox("Choose no border (no Pi calculation possible, only fly counting)", false);
		gdBor.addCheckbox("Choose manual ROI (no Pi calculation possible, only fly counting)", false);

		gdBor.showDialog();
		if (gdBor.wasCanceled()) {
			IJ.error("No border option is chosen");
		}
		real = gdBor.getNextBoolean(); // real border is assigned to the first
											// checkbox
		shift = gdBor.getNextBoolean(); // shifted border is assigned to the
											// second checkbox
		without = gdBor.getNextBoolean(); // without border is assigned to the
											// third checkbox
		manual = gdBor.getNextBoolean(); // manual ROI is assigned to the
											// fourth checkbox

		if ((real == true && shift == false && without == false && manual == false)||
			(real == false && shift == true && without == false && manual == false)||
			(real == false && shift == false && without == true && manual == false)||
			(real == false && shift == false && without == false && manual == true)) {
			
			input[0] = real;
			input[1] = shift;
			input[2] = without;
			input[3] = manual;
		} else {
			IJ.error("Only one border option can be chosen!");
			borderSetting();
		}
		
		return input;
		
	}
	
	
	// method for opening an inputDialog, that lets the user chose between the
	// analyzing types
	public static boolean[] inputDialog() {

		boolean movement;
		boolean detection;

		boolean[] input = new boolean[2];

		GenericDialog gdIn = new GenericDialog("Choose Results");

		gdIn.addCheckbox("Fly Movement", false);
		gdIn.addCheckbox("Fly Detection", false);

		gdIn.showDialog();
		if (gdIn.wasCanceled()) {
			IJ.error("No results are chosen");
		}
		movement = gdIn.getNextBoolean(); // movement is assigned to the first
											// checkbox
		detection = gdIn.getNextBoolean(); // fly detection is assigned to the
											// second checkbox

		if (movement == false && detection == false) {
			IJ.error("No results are chosen");
			inputDialog();
		} else {
			input[0] = movement;
			input[1] = detection;
		}

		return input;

	}

	// get-method to get the value of variable movement
	public static boolean getMovement() {

		boolean movement = res[0];
		return movement;

	}

	// get-method to get the value of variable detection
	public static boolean getDetection() {

		boolean detection = res[1];
		return detection;

	}

	// Method for Output a .txt-file for the results of the preference index
	public void outputFile(String MyDir2, ImagePlus imp2) throws IOException {

		String title = imp2.getTitle();

		File file = new File(MyDir2 + title.substring(0, title.length() - 2)
				+ "_preferenceIndex" + ".txt");
		java.io.Writer output = new BufferedWriter(new FileWriter(file));

		TreeMap<Integer, Double> prefInd = bs.getPrefInd();

		Set set = prefInd.entrySet();
		Iterator i = set.iterator();

		output.write("Slice" + "\t" + "Preference_Index" + "\n");

		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			output.write(me.getKey() + "\t"
					+ ((Double) me.getValue()).doubleValue() + "\n");
		}

		output.close();

	}
}
