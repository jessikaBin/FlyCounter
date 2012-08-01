import java.io.File;
import java.io.*;
import java.util.*; 

import ij.IJ;
import ij.ImagePlus;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import ij.io.Opener;
import ij.process.ImageConverter;

//This plugin shows the basic structure of a batch code. (one capable to recuperate images in a hard disk, treat them, ans save them.

public class Batch_Run implements PlugIn { 

	private Border_Substack bs = new Border_Substack ();
	private AVI_Writer aw = new AVI_Writer ();	

	protected static String filename = "";
	protected static String myDir2 = "";
	protected static String myDir1 = "";

	public void run(String arg) { //Method Belonging to PlugIn Implementation
		
		myDir1 = IJ.getDirectory("Select Image Source Folder...");				//This plugin opens the select folder window in ImageJ
		if (myDir1==null) {
			return;									//If not source folder is selected the plugin stops here
		}
		String[] myListSources = new File(myDir1).list();						//This string contains all the files names in the source folder
		if (myListSources==null) {
			return;
		}
		IJ.log("The source image folder chosen was"+myDir1);					//This command opens the log window and writes on the variables on it.
		
		myDir2 = IJ.getDirectory("Select Images Saving Folder...");
		if (myDir2==null) {
			return;									//If not saving folder is selected the plugin stops here
		}
		IJ.log("The saving folder chosen was"+myDir2);
			
		int FileNumber;

		for (FileNumber= 0; FileNumber< myListSources.length; FileNumber++) {
			
			IJ.showProgress(FileNumber,myListSources.length);					//This command sets magnitude the progress bar on ImageJ
			IJ.log((FileNumber+1)+":"+myListSources[FileNumber]);
			IJ.showStatus(FileNumber+"/"+myListSources.length);				//This command sets a text in the ImageJ status bar

			if (myListSources[FileNumber].contains(".avi")) {
				filename = myListSources[FileNumber].substring(0, myListSources[FileNumber].length()-4);

				Opener o = new Opener ();
				ImagePlus myImPlus = o.openImage(myDir1, myListSources[FileNumber]);

				ImageConverter ic = new ImageConverter (myImPlus);
				ic.convertToGray8();

				myImPlus.show();							//This command opens a window with the corresponding ImagePlus
				ImageProcessor myIp = myImPlus.getProcessor();
				myImPlus.updateAndDraw();


				bs.setup("", myImPlus);
				bs.run (myIp);

				ImagePlus imp2 = bs.getImp();
				ImageProcessor ip2 = imp2.getProcessor();

				try {
					outputFile (myDir2, imp2);
				}
				catch (IOException e) {
					System.err.println( "Problem with writing the file" );
				}
				

				aw.setup ("",imp2);
				aw.run(ip2);

				imp2.close();
				myImPlus.close();							//This method closes the image window and it sets the its image processor to null (this means it empties the corresponding memory in ImageJ.
			}
						
		}

		IJ.log("Completed");
	}

  	public void outputFile (String MyDir2, ImagePlus imp2) throws IOException {

		String title = imp2.getTitle();

  		File file = new File(MyDir2 + title + ".xls");
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
