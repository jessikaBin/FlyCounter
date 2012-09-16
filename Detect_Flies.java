import ij.*;
import ij.measure.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.ArrayList;
import ij.plugin.filter.*;
import ij.plugin.SubstackMaker;

import java.util.*; 

public class Detect_Flies implements PlugInFilter {
	
	ImagePlus imp;
	
	private Maximum_Finder_Modified mf = new Maximum_Finder_Modified (); // shortly modified version of the MaximumFinder
	protected TreeMap <Double, Double> countedFlies = new TreeMap <Double, Double> ();


	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {

		
		byte[] pixels = (byte[])ip.getPixels(); // get size of image
		int stackSize = imp.getStackSize() ;	// get number of frames 
		
		mf.setup("", imp);
		
		for (int i = 1; i <= stackSize; i++) {
			
			imp.setSlice(i);
			findFlies(ip, imp);
			filterMaximaWithDifference(ip);
		//	filterMaximaWithWand (ip);
		
		}
		
		outputCountedFlies();
		
	}

	// method to find maxima (possible flies)
	private void findFlies (ImageProcessor ip, ImagePlus imp) {
	
		double tolerance = 10; // noise tolerance (the higher the number, the less found maxima)
		double threshold = ImageProcessor.NO_THRESHOLD;
		int outputType = 4; // output is a list of all maxima
		boolean excludeOnEdges = true;
		boolean isEDM = false;
		
		// part from original run-method from MaximumFinder to invert picture (because of light background)
		float[] cTable = ip.getCalibrationTable();
        ip = ip.duplicate();
		
        if (cTable==null) {                 //invert image for finding minima of uncalibrated images
			ip.invert();
        } 
		else {                            //we are using getPixelValue, so the CalibrationTable must be inverted
            float[] invertedCTable = new float[cTable.length];
            for (int i=cTable.length-1; i>=0; i--){ 
                invertedCTable[i] = -cTable[i];
			}
            ip.setCalibrationTable(invertedCTable);
        }
	
		// invoking the findMaxima method of the MaximumFinder
		mf.findMaxima(ip, tolerance, threshold, outputType, excludeOnEdges, isEDM);
		
	}
	
	private void filterMaximaWithDifference (ImageProcessor ip) {
	
		int width = ip.getWidth();
		int height = ip.getHeight();
	
		// get maxima found by MaximumFinder
		ResultsTable rt = Analyzer.getResultsTable();
		rt = mf.getResultsTable ();
	
		String [] splitt = new String [3];
		ArrayList <Double> xcoo = new ArrayList <Double> (); // array list for saving the x values
		ArrayList <Double> ycoo = new ArrayList <Double> (); // array list for saving the y values

		for (int i =0; i<=rt.getCounter()-1; i++){	
			String row = rt.getRowAsString(i);
			splitt =  row.split(",");
			if (splitt.length == 1){
				splitt = row.split("\t");
			}	
			double x = Double.parseDouble(splitt[1]); 
			double y = Double.parseDouble(splitt[2]);
			xcoo.add(x);
			ycoo.add(y);	
		}
		
		//check for each found maxima whether it is a fly
		
		double flies = 0;
		for (int j = 0; j<xcoo.size();j++){
		
			double x_max = xcoo.get(j);
			double y_max = ycoo.get(j);
			
			double counter = 0;
						
			// check pixel values for found maxima and their 8 neighbor pixelss
			
			for (int hor = -1; hor<=1; hor++){
				for (int ver =-1; ver <=1; ver++){
				
					int diff = 70; // maximum difference between pixel values 
					int abs = 115; // cutoff for absolute pixel value
					
					// if pixelvalue of centerpixel is < abs and the value difference to the 8 neighbour pixels is < diff => possible fly
					
					if ((ip.getPixelValue((int)x_max+hor, (int)y_max+ver) < abs) &&
						(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor-1, (int)y_max+ver))<diff) &&
						(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor-1, (int)y_max+ver-1))<diff) &&
						(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor, (int)y_max+ver-1))<diff) &&
						(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor+1, (int)y_max+ver-1))<diff) &&
						(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor+1, (int)y_max+ver))<diff) &&
						(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor+1, (int)y_max+ver+1))<diff) &&
						(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor, (int)y_max+ver+1))<diff) &&
						(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor-1, (int)y_max+ver+1))<diff)) {
		
							counter++;
					}
				}
			}
			
			// if min. 6 neighboured pixels suggest a possible fly => fly
			
			if (counter > 5) {
				ip.drawDot ((int)x_max, (int)y_max);
				flies++;
			}			
		}
		
		double frame = (double)imp.getCurrentSlice();
		countedFlies.put(frame, flies);
		
	}
	
	public void filterMaximaWithWand (ImageProcessor ip) {
	
		HashMap <ArrayList<Double>, Double> areaToCoordinates = new HashMap <ArrayList<Double>, Double> ();
	
		ResultsTable rt = Analyzer.getResultsTable();
		//rt.reset();
		rt = mf.getResultsTable ();
	
		String [] splitt = new String [3];
		ArrayList <Double> xcoo = new ArrayList <Double> (); // array list for saving the x values
		ArrayList <Double> ycoo = new ArrayList <Double> (); // array list for saving the y values

		for (int i =0; i<=rt.getCounter()-1; i++){	
			String row = rt.getRowAsString(i);
			splitt =  row.split(",");
			if (splitt.length == 1){
				splitt = row.split("\t");
			}	
			double x = Double.parseDouble(splitt[1]); 
			double y = Double.parseDouble(splitt[2]);
			xcoo.add(x);
			ycoo.add(y);	
		}
		
		double flies = 0;
		for (int j = 0; j<xcoo.size();j++){
		
			rt.reset();
		
			double x_max = xcoo.get(j);
			double y_max = ycoo.get(j);
			
			double lower = 0;
			double upper = 110;
			int mode = 8;
			
			Wand w = new Wand(ip);
			w.autoOutline((int)x_max, (int) y_max, lower, upper, mode);
			
			int [] x = w.xpoints;
			int [] y = w.ypoints;
			
			rt.reset();
			
			int count = 0;
			for (int k=0; k<x.length;k++){
				
				if (x[k] != 0 && y[k] != 0) {
					count ++;
				}
			}
			
			int [] xPoints = new int [count];
			int [] yPoints = new int [count];
			


			for (int k=0; k<x.length;k++){
			try {
				
				if (x[k] != 0 && y[k] != 0) {
					xPoints[k]=x[k];
					yPoints[k]=y[k];
				}
				
				}
				    catch (IndexOutOfBoundsException ex )
    {
      System.out.println("Es ist folgendes Problem aufgetreten: " + ex.getMessage()
          + "\nHier ist wobei es passiert ist:\n");
      ex.printStackTrace();
    }
			}
		
			
			int nPoints = xPoints.length;
			int type = 2;
		
			Roi pr = new PolygonRoi(xPoints, yPoints, nPoints, type);
			imp.setRoi(pr);
			
			ip.setThreshold(0, upper, 0);
			
			ParticleAnalyzer pa = new ParticleAnalyzer(0, 1, rt, 0, Double.POSITIVE_INFINITY, 0, 1);
			pa.analyze(imp);
			
			String [] splitt2 = new String [2];

			String row = rt.getRowAsString(0);
			splitt2 =  row.split(",");
			if (splitt2.length == 1){
				splitt2 = row.split("\t");
			}	
			double area = Double.parseDouble(splitt2[1]); // area values are in second column 
			
			if (area > 9) {
				ip.drawDot ((int)x_max, (int)y_max);
				flies ++;
			}
			
			rt.reset();
			
			ArrayList <Double> coordinates = new ArrayList <Double>();
			coordinates.add(x_max);
			coordinates.add(y_max);

			areaToCoordinates.put(coordinates,area);
			
	
		}
		
		double frame = (double)imp.getCurrentSlice();
		countedFlies.put(frame, flies);
		
		// rt.reset();
		
		// Set set = areaToCoordinates.entrySet(); 
		// Iterator i = set.iterator(); 

		// while (i.hasNext()) { 
			// Map.Entry me = (Map.Entry)i.next(); 
			// rt.incrementCounter();
			
        // //    rt.addValue("Coordinates", ((Double)me.getKey()).doubleValue());
			// rt.addValue("Area", ((Double)me.getValue()).doubleValue());
		// } 	
		
		// rt.show ("Results");
		
		// rt.reset();

	
	}
	
	public void outputCountedFlies () {
	
		ResultsTable rt = Analyzer.getResultsTable();
		rt.reset();
		
		Set set = countedFlies.entrySet(); 
		Iterator i = set.iterator(); 

		while (i.hasNext()) { 
			Map.Entry me = (Map.Entry)i.next(); 
			rt.incrementCounter();
            rt.addValue("Frame", ((Double)me.getKey()).doubleValue());
			rt.addValue("Flies", ((Double)me.getValue()).doubleValue());
		} 	
		
		rt.show ("Results");
		
	}
}



