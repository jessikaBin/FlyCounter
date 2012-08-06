import java.io.File;
import java.io.*;
import java.util.*; 

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.io.Opener;
import ij.process.ImageConverter;
import ij.gui.GenericDialog;


public class Batch_Run implements PlugIn { 

	private Border_Substack bs = new Border_Substack ();
	private AVI_Writer aw = new AVI_Writer ();	

	protected static String filename = "";
	protected static String myDir1 = "";	// Source Directory
	protected static String myDir2 = "";	// Saving Directory


	protected static int flyThreshold = thresholdDialog ();	// Threshold for Counting Flies

	public void run(String arg) { 
		
		myDir1 = IJ.getDirectory("Select Image Source Folder...");				// Opening source folder window in ImageJ
		if (myDir1==null) {
			return;									// If no source folder is selected the plugin stops here
		}
		String[] myListSources = new File(myDir1).list();					// all the file names in the source folder
		if (myListSources==null) {
			return;
		}
		IJ.log("The source image folder chosen was"+myDir1);				// opens the log window
		
		myDir2 = IJ.getDirectory("Select Image Saving Folder...");				// Opening saving folder window in ImageJ
		if (myDir2==null) {
			return;									//If no saving folder is selected the plugin stops here
		}
		IJ.log("The saving folder chosen was"+myDir2);
			
		for (int FileNumber= 0; FileNumber< myListSources.length; FileNumber++) {
			
			IJ.showProgress(FileNumber,myListSources.length);			// showing progress
			IJ.log((FileNumber+1)+":"+myListSources[FileNumber]);
			IJ.showStatus(FileNumber+"/"+myListSources.length);				

			if (myListSources[FileNumber].contains(".avi")) {

				filename = myListSources[FileNumber].substring(0, myListSources[FileNumber].length()-4);

				AVI_Reader ar = new AVI_Reader ();				// reading the AVI file
				ar.run("");
				ImagePlus myImPlus = ar.getImagePlus ();

				myImPlus.show();						// show video	
				ImageProcessor myIp = myImPlus.getProcessor();
				myImPlus.updateAndDraw();


				bs.setup("", myImPlus);						// run Border_Substack class
				bs.run (myIp);

				ImagePlus imp2 = bs.getImp();
				ImageProcessor ip2 = imp2.getProcessor();

				try {
					outputFile (myDir2, imp2);				// write the output .xls-file
				}
				catch (IOException e) {
					System.err.println( "Problem with writing the file" );
				}
				

				aw.setup ("",imp2);						// write the output .avi-file
				aw.run(ip2);

				imp2.close();
				myImPlus.close();						
			}
						
		}

		IJ.log("Completed");
	}


	// Method to set the Threshold in a Dialog
	public static int thresholdDialog () {

    		int threshold = 100;
	
	      	GenericDialog gd = new GenericDialog("Set Threshold");
	      	gd.addNumericField("Threshold: ", threshold, 0);

	      	gd.showDialog();
	      	if (gd.wasCanceled()) {
			System.err.println( "No threshold was selected" );
		}
	      	threshold = (int)gd.getNextNumber();
     	
		return threshold;
	}


	// Method for Output a .xls-file for the results
	public void outputFile (String MyDir2, ImagePlus imp2) throws IOException {

		String title = imp2.getTitle();

		File file = new File(MyDir2 + title.substring(0,title.length()-2) + ".xls");
		java.io.Writer output = new BufferedWriter(new FileWriter(file));

		ArrayList <TreeMap <Integer, Integer>> summaries = bs.getMaps ();
		TreeMap <Integer, Integer> summaryWater = summaries.get(0);
		TreeMap <Integer, Integer> summarySugar = summaries.get(1);

		Set set = summaryWater.entrySet(); 
		Iterator i = set.iterator(); 

		output.write("Slice" + "\t" + "Count Water " + "\t" + "Count Sugar" + "\n");

		while (i.hasNext()) { 
			Map.Entry me = (Map.Entry)i.next(); 
			output.write(me.getKey() + "\t" + me.getValue() + "\t" + summarySugar.get(me.getKey()) + "\n"); 
		} 

  		output.close();

	}
}
