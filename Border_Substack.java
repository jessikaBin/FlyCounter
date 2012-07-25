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


public class Border_Substack implements PlugInFilter {

	ImagePlus imp;
	ImagePlus imp2;
	ImageProcessor ip2;
	ImageWindow iw;
	//>> should probaly have a better name than k -> smth like fly_count?
	private int k = 0;   // count for testing whether flies are in frame or not     
	private Detect_Border db = new Detect_Border (); 

	private int test = 0;
	private double numberSlides = 0;
	//>> stylguide -> nicht mehr als 80 Zeichen pro zeile!!
	protected TreeMap <Integer, Integer> summaryWater = new TreeMap <Integer, Integer> ();
	protected TreeMap <Integer, Integer> summarySugar = new TreeMap <Integer, Integer> ();


	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}


	public void run(ImageProcessor ip) {

		db.setup("",imp);

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
				//>> this is a paramter that should be set in the beginning and 
				//>> have a reasonable name
				double max = 140.0;	// starting threshold
				max = setThresh(max, ip, imp, x, y, rwidth, rheight);	// applying setThresh method
			}

			findFlies (ip, imp);	// applying findFlies method

			if ( k != 0) {	// flies are found in current slice
				curr = imp.getCurrentSlice();	// starting frame is changed to current slice
				imp.setSlice(curr-2);

				db.run(ip);
				break;	
			}

		}

		imp.killRoi();
		//>> ich glaube die ganze susbtack geschichte kann man sich sparen 
		//>> wenn man einfach oben den for loop oben klug durchdenkt
		//>> so lange lauffen bis fleigen auftauchen und da sich die anzahl
		//>> der fleiegn pro frame merken. dadurch spart man sich den 
		//>> substack kram sowie zwei methoden...
		substack (ip, imp, curr, stackSize);	// create substack from current slice until end of stack

		detectFlies (ip2, imp2);
	//	imp.close();
	//	imp2.close();

	}



	// method to set the threshold in the first frame 
	private double setThresh (double max, ImageProcessor ip, ImagePlus imp, int x, int y, int rwidth, int rheight) {

		double minThreshold = 0.0;
		ip.setThreshold(minThreshold, max, 0);	
				
		// define results table
		ResultsTable rt = Analyzer.getResultsTable();

		// measurements: area & circularity, options: show nothing, minSize: particle not smaller than 3, maxSize: infinity, minCirc/maxCirc: no  defined circularity
		//>> 8193, 0, 3 sind parameter die zentral gestezt werden sollten
		rt = db.tableAnalyser (imp, rt, 8193, 0, 3, Double.POSITIVE_INFINITY, 0, Double.POSITIVE_INFINITY);

		if (rt.getCounter() < 2) {
			if (test == 3) {
			int curr = imp.getCurrentSlice();	// starting frame is changed to current slice
			imp.setSlice(curr+1);
			test = 2;
			max++;
			imp.setRoi(x, y, rwidth, rheight);
			setThresh (max, ip, imp, x, y, rwidth, rheight);
			} else {
			test = 2;
			max++;
			imp.setRoi(x, y, rwidth, rheight);
			setThresh (max, ip, imp, x, y, rwidth, rheight);
			}
		}

		if (rt.getCounter() > 3) {
			if (test ==2) {
			int curr = imp.getCurrentSlice();	// starting frame is changed to current slice
			imp.setSlice(curr+1);
			test = 3;
			max++;
			imp.setRoi(x, y, rwidth, rheight);
			setThresh (max, ip, imp, x, y, rwidth, rheight);
			} else  {
			test = 3;
			max--;
			imp.setRoi(x, y, rwidth, rheight);
			setThresh (max, ip, imp, x, y, rwidth, rheight);
			}
		}

		return max;

	}


	// method to find flies in frame
	private int findFlies (ImageProcessor ip, ImagePlus imp) {

		// define results table
		ResultsTable rt2 = Analyzer.getResultsTable();

		// measurements: area & circularity, options: show nothing, minSize: flies not smaller than 25, maxSize: flies not bigger than 150, minCirc/maxCirc: flies have circularity between 0.6 and 0.8
		//>> warum heist der rt2?
		rt2 = db.tableAnalyser (imp, rt2, 8193, 0, 25, 150, 0.6, 0.8);

		String [] splitt = new String [3];
		ArrayList <Double> circ = new ArrayList <Double> (); // array list for saving the values for circularity

		for (int i =0; i<=rt2.getCounter()-1; i++){	// circularity values of results table are saved in array list
			String row = rt2.getRowAsString(i);
			splitt =  row.split(",");
			if (splitt.length == 1){
				splitt = row.split("\t");
			}	
			double c = Double.parseDouble(splitt[2]); // circularity values are in second column 
			circ.add(c);
		}

		k = 0;	// count is set to 0

		if (circ.size() >= 2){
			k++;
		}

		return k;
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

	public void detectFlies (ImageProcessor ip2,ImagePlus imp2) {
		
		int stackSize = imp2.getStackSize() ;	// get number of frames 
		
		for (int i = 1; i <= stackSize; i++) {	

			imp2.setSlice(i);	// current slice is set

			// set threshold
			double max = 100.0;	// starting threshold
			countFlies(max, ip2, imp2, i, summaryWater, summarySugar);	// applying setThresh method

		}

	}

	public ArrayList <TreeMap <Integer, Integer>> getMaps () {

		ArrayList <TreeMap <Integer, Integer>> areas = new ArrayList <TreeMap <Integer, Integer>> ();
		areas.add(0,summaryWater);
		areas.add(1,summarySugar);

		return areas;

	}

	public ImagePlus getImp () {

		return imp2;
	}

	private void countFlies (double max, ImageProcessor ip2, ImagePlus imp2, int i, TreeMap <Integer, Integer> summaryWater, TreeMap <Integer, Integer> summarySugar) {

		// create polygon for ROI with 4 points
		int nPoints = 4;
		int type = 2;

		float [] xPoints = db.getXPoints (); 
		float [] yPoints = db.getYPoints ();   

		ip2.drawLine((int) xPoints[1], (int)yPoints[1] , (int)xPoints[2], (int) yPoints[2]) ;

		Roi prWater = new PolygonRoi(xPoints, yPoints, nPoints, type);

		imp2.setRoi(prWater);		// set ROI

		double minThreshold = 0.0;
		ip2.setThreshold(minThreshold, max, 0);	

		// define results table
		ResultsTable rt2 = Analyzer.getResultsTable();

		// measurements: area & circularity & slice, options: show nothing, minSize: flies not smaller than 8, maxSize: flies not bigger than 150, minCirc/maxCirc: no defined circularity
		//>> 9217, 0, 8, 150, 0, sind parameter die zentral gestezt werden sollten
		rt2 = db.tableAnalyser (imp2, rt2, 9217, 0, 8, 150, 0, Double.POSITIVE_INFINITY);

		int particles = rt2.getCounter();
		summaryWater.put(i, particles);

		imp2.killRoi();

		float [] xPoints2 = {0, 0, xPoints[2], xPoints[3]}; 
		float [] yPoints2 = {yPoints[1]+1, (float) ip2.getHeight(),(float)  ip2.getHeight(), yPoints[2]+1};  

		Roi prSugar = new PolygonRoi(xPoints2, yPoints2, nPoints, type);

		imp2.setRoi(prSugar);		// set ROI

		// measurements: area & circularity & slice, options: show nothing, minSize: flies not smaller than 8, maxSize: flies not bigger than 150, minCirc/maxCirc: no defined circularity
		//>> 9217, 0, 8, 150, 0, sind parameter die zentral gestezt werden sollten
		rt2 = db.tableAnalyser (imp2, rt2, 9217, 0, 8, 150, 0, Double.POSITIVE_INFINITY);

		particles = rt2.getCounter();
		summarySugar.put(i, particles);

	}
	


}
