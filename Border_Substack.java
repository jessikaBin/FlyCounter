import ij.*;
import ij.measure.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.ArrayList;
import ij.plugin.filter.*;
import ij.plugin.SubstackMaker;

import java.io.*;
import java.util.*; 
import java.lang.Math;


public class Border_Substack implements PlugInFilter {

	static ImagePlus imp;
	ImagePlus imp2;
	ImageProcessor ip2;
	ImageWindow iw;
	static Roi prSugar;
	static Roi prWater;

	private int flyCount = 0;   // count for testing whether flies are in frame or not  
	
//	private Detect_Border db = new Detect_Border (); 
//	private Detect_Border_CF db = new Detect_Border_CF (); 
	private Detect_Flies df = new Detect_Flies ();
	private Fly_Movement fm = new Fly_Movement ();
	
	public String result = "";
	
	private int test = 0;
	private double numberSlides = 0;

	private double startingThreshold = 140.0;					// starting threshold for border detection
//	private double maxThreshold = Batch_Run.flyThreshold;		// threshold for detecting the flies
	private boolean mov = Batch_Run.mov;
	private boolean det = Batch_Run.det;

	//protected TreeMap <Integer, Integer> summaryWater = new TreeMap <Integer, Integer> ();
	//protected TreeMap <Integer, Integer> summarySugar = new TreeMap <Integer, Integer> ();
	protected TreeMap <Integer, Double> preferenceIndex = new TreeMap <Integer, Double> ();
	
	protected ArrayList <TreeMap <Integer, Double>> sugar = new ArrayList <TreeMap <Integer, Double>> ();
	protected ArrayList <TreeMap <Integer, Double>> water = new ArrayList <TreeMap <Integer, Double>> ();

	protected TreeMap <Integer, ArrayList<Double>> movementRatio = new TreeMap <Integer, ArrayList<Double>>();
	

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}


	public void run(ImageProcessor ip) {

	//	db.setup("",imp);
		df.setup("",imp);
		fm.setup("",imp);

		// get size of image
		byte[] pixels = (byte[])ip.getPixels();
		int width = ip.getWidth();
		int height = ip.getHeight();
		
		// get size and position of ROI 
		int rwidth = width/2;	// width
		int rheight = height/4;	// height
		int x = width/4;		// upper left x coordinate
		int y = 3*height/8;	// upper left y coordinate

		int stackSize = imp.getStackSize() ;	// get number of frames 
		int curr = 2;	// variable for starting frame of substack

		for (int i = 2; i <= stackSize; i++) {	// start with frame 2, because frame 1 is black

			imp.setSlice(i);	// current slice is set

			if ( i == 2) {	// only in frame 2 the threshold is set
				imp.setRoi(x, y, rwidth, rheight);		// set ROI
				// Rectangle r = ip.getRoi();

				// set threshold
				double startingThreshold = 140.0;	// starting threshold
				startingThreshold = setThresh(startingThreshold, ip, imp, x, y, rwidth, rheight);	// applying setThresh method
			}

			findFirstFlies (ip, imp);	// applying findFirstFlies method
			
			if ( flyCount != 0) {	// flies are found in current slice
				curr = imp.getCurrentSlice();	// starting frame is changed to current slice
				imp.setSlice(curr-2);

				// db.run(ip);
				fm.startBorderDetect(imp,ip);
			//	result = db.getString ();
				break;	
			}

		}

		imp.killRoi();
	//	substack (ip, imp, curr, stackSize);	// create substack from current slice until end of stack

		detectFlies (ip, imp, curr);
	//	imp.close();
	//	imp2.close();
		if (mov == true) {
			try {
				fm.outputRatios(imp);
				fm.outputNumbers(imp);
				fm.createChannelVideo();
			} catch (IOException e) {
				System.err.println("Problem with writing the file");
			  }
		}

	}

	public String getString () {

		return result;	
	
	}

	// method to set the threshold in the first frame 
	private double setThresh (double startingThreshold, ImageProcessor ip, ImagePlus imp, int x, int y, int rwidth, int rheight) {

		double minThreshold = 0.0;
		ip.setThreshold(minThreshold, startingThreshold, 0);	
				
		// define results table
		ResultsTable rt = Analyzer.getResultsTable();

		// measurements: area & circularity, options: show nothing, minSize: particle not smaller than 3, maxSize: infinity, minCirc/maxCirc: no  defined circularity
		rt = Detect_Border_CF.tableAnalyser (imp, rt, 8193, 0, 3, Double.POSITIVE_INFINITY, 0, 1);

		if (rt.getCounter() == 0) {
//			if (test == 3) {
//				int curr = imp.getCurrentSlice();	// starting frame is changed to current slice
//				imp.setSlice(curr+1);
//				test = 2;
				startingThreshold++;
//				imp.setRoi(x, y, rwidth, rheight);
//				setThresh (startingThreshold, ip, imp, x, y, rwidth, rheight);
//			} else {
//				test = 2;
//				startingThreshold++;
				imp.setRoi(x, y, rwidth, rheight);
				setThresh (startingThreshold, ip, imp, x, y, rwidth, rheight);
//			}
		}
		
		else if (rt.getCounter () == 1) {
			
			String [] splitt = new String [3];

			String row = rt.getRowAsString(0);
			splitt =  row.split(",");
			if (splitt.length == 1){
				splitt = row.split("\t");
			}	
			double a = Double.parseDouble(splitt[1]); // circularity values are in second column 


			if ( a >= 3000) {
				imp.setSlice(imp.getCurrentSlice()+1);
				imp.setRoi(x, y, rwidth, rheight);
				setThresh (startingThreshold, ip, imp, x, y, rwidth, rheight);
			}
			else {
				startingThreshold++;
				imp.setRoi(x, y, rwidth, rheight);
				setThresh (startingThreshold, ip, imp, x, y, rwidth, rheight);
			
			}
		}

		else if (rt.getCounter() > 3) {
//			if (test == 2) {
//				int curr = imp.getCurrentSlice();	// starting frame is changed to current slice
//				imp.setSlice(curr+1);
//				test = 3;
				startingThreshold++;
//				imp.setRoi(x, y, rwidth, rheight);
//				setThresh (startingThreshold, ip, imp, x, y, rwidth, rheight);
//			} else  {
//				test = 3;
//				startingThreshold--;
				imp.setRoi(x, y, rwidth, rheight);
				setThresh (startingThreshold, ip, imp, x, y, rwidth, rheight);
//			}
		}

		return startingThreshold;

	}


	// method to find first flies in frame
	private int findFirstFlies (ImageProcessor ip, ImagePlus imp) {

		// define results table
		ResultsTable rt = Analyzer.getResultsTable();

		// measurements: area & circularity, options: show nothing, minSize: flies not smaller than 80, maxSize: flies not bigger than 550, minCirc/maxCirc: flies have circularity between 0.5 and 0.9
		rt = Detect_Border_CF.tableAnalyser (imp, rt, 8193, 0, 80, 800, 0.5, 0.9);

		String [] splitt = new String [3];
		ArrayList <Double> circ = new ArrayList <Double> (); // array list for saving the values for circularity

		for (int i =0; i<=rt.getCounter()-1; i++){	// circularity values of results table are saved in array list
			String row = rt.getRowAsString(i);
			splitt =  row.split(",");
			if (splitt.length == 1){
				splitt = row.split("\t");
			}	
			double c = Double.parseDouble(splitt[2]); // circularity values are in second column 
			circ.add(c);
		}

		flyCount = 0;	// count is set to 0

		if (circ.size() >= 2){
			flyCount++;
		}

		return flyCount;
	}

	
	// method to create the substack with only flies in the frames
	public void substack (ImageProcessor ip, ImagePlus imp, int curr, int stackSize) {

		String start = String.valueOf(curr) ;	// starting frame is current slice
		String ende = String.valueOf(stackSize) ;	// ending frame is last slice
		StringBuilder text = new StringBuilder(start);
		text.append("-");
		text.append(ende);
		String stack = text.toString();

		ImageStack is = new ImageStack();
		SubstackMaker sm = new SubstackMaker ();
		imp2 = sm.makeSubstack(imp, stack); 	// substack is created 
		is = imp2.getStack();

		ip2 = imp2.getProcessor();

		String filename = imp.getTitle ();
		String title = "";

		if (filename.contains(".avi")){
			title = filename.substring(0, filename.length() -4);
		} else {
			title = filename;
		}

		imp2.setTitle(title);
		
		iw = new ImageWindow(imp2) ;	// substack is opened in new window
		WindowManager.setCurrentWindow (iw);

	}

	public void detectFlies (ImageProcessor ip,ImagePlus imp, int curr) {
		
		int stackSize = imp.getStackSize() ;	// get number of frames 
		
		for (int i = curr; i <= stackSize; i++) {	

			imp.setSlice(i);	// current slice is set

			// set threshold

			countFlies(ip, imp, i, preferenceIndex);	// applying setThresh method
			
		}

	}

	// public ArrayList <TreeMap <Integer, Integer>> getMaps () {

		// ArrayList <TreeMap <Integer, Integer>> areas = new ArrayList <TreeMap <Integer, Integer>> ();
		// areas.add(0,summaryWater);
		// areas.add(1,summarySugar);

		// return areas;

	// }
	
	public TreeMap <Integer, Double> getPrefInd () {
	
		return preferenceIndex;
		
	}

	public static ImagePlus getImp () {

		return imp;
	}

	private void countFlies (ImageProcessor ip, ImagePlus imp, int i, TreeMap <Integer, Double> preferenceIndex) {

		if (det == true){
		
			ResultsTable rt = Analyzer.getResultsTable();

			prWater = fm.setWaterRoi(rt);
			imp.setRoi(prWater);		// set ROI

			// double minThreshold = 0.0;
			// ip2.setThreshold(minThreshold, maxThreshold, 0);	

			// define results table
			// ResultsTable rt = Analyzer.getResultsTable();

			// measurements: area & circularity & slice, options: show nothing, minSize: flies not smaller than 8, maxSize: flies not bigger than 150, minCirc/maxCirc: no defined circularity
			// rt = db.tableAnalyser (imp2, rt, 9217, 0, 8, 150, 0, 1);
		
			df.run(ip);

			double wat = df.getCountedFlies();
			//summaryWater.put(i, wat);
		
			imp.killRoi();
			
			
			prSugar = fm.setSugarRoi(ip, rt);
			imp.setRoi(prSugar);		// set ROI

			// measurements: area & circularity & slice, options: show nothing, minSize: flies not smaller than 8, maxSize: flies not bigger than 150, minCirc/maxCirc: no defined circularity
			//	rt = db.tableAnalyser (imp2, rt, 9217, 0, 8, 150, 0, 1);

			df.run(ip);
			double sug = df.getCountedFlies();
			//summarySugar.put(i, sug);
		
			double prefInd = (sug - wat)/(sug + wat);
		
			preferenceIndex.put(i, prefInd);
			
		}
		
		if ((mov == true) && (i <= imp.getStackSize()-1)){
			fm.run(ip);
			movementRatio = fm.getMovementRatio();
		}
		
	}
	

	

}
