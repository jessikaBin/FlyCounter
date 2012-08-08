import ij.*;
import ij.measure.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.util.*;
import java.awt.Color;
import ij.measure.CurveFitter;

import ij.gui.GenericDialog;

import ij.IJ;

public class Detect_Border_CF implements PlugInFilter {

	ImagePlus imp;

	public float [] xPoints = new float [4];
	public float [] yPoints = new float [4]; 
	
	public String result = "";

	private float xwidth = (float)0.0;	// float width of image
	private double max;
	public double [] params = new double [2];

	ArrayList <Float> xa = new ArrayList <Float> ();	// x coordinates of detected border ROI
	ArrayList <Float> ya = new ArrayList <Float> ();	// y coordinates of detected border ROI 	

	

	public int setup(String arg, ImagePlus imp) {

		this.imp = imp;
		return DOES_ALL;

	}

	
	public void run(ImageProcessor ip) {

		// get size of image
		byte[] pixels = (byte[])ip.getPixels();
		int width = ip.getWidth();
		int height = ip.getHeight();

		xwidth = (float) width;
		
		int rwidth = width/2;	// width
		int rheight = height/4;	// height
		int x = width/4;		// upper left x coordinate
		int y = 3*height/8;	// upper left y coordinate
		
		
		imp.setRoi(x, y, rwidth, rheight);		// set ROI 1

		imp.repaintWindow();

		// set threshold
		max = 140.0;	// threshold
		setThresh(max, ip, imp, x, y, rwidth, rheight);	// applying setThresh method

		drawBorder(ip,imp);	// applying drawBorder method

	}	

	// method to define ResultsTable and run Particle Analyser
	protected ResultsTable tableAnalyser (ImagePlus imp, ResultsTable rt, int measurements, int options, double minSize, double maxSize, double minCirc, double maxCirc) {

		// define results table
		rt.reset();
		if (rt == null) {
        			rt = new ResultsTable();
       			Analyzer.setResultsTable(rt);
		}

		// set particle analyzer
		ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt, minSize, maxSize, minCirc, maxCirc); 
		pa.analyze(imp); // apply particle analysis

		return rt;

	}


	public float [] getXPoints () {

		return xPoints;	
	
	}


	public float [] getYPoints () {

		return yPoints;	
	
	}
	
	
		
	private double setThresh (double max, ImageProcessor ip, ImagePlus imp, int x, int y, int rwidth, int rheight) {

		// setting the threshold
		double minThreshold = 0.0;
		ip.setThreshold(minThreshold, max, 0);	

		// define results table
		ResultsTable rt = Analyzer.getResultsTable();

		// measurements: area & center of mass, options: show nothing, minSize: particle not smaller than 3, maxSize: particle not bigger than 100, minCirc/maxCirc: no  defined circularity
		rt = tableAnalyser (imp, rt, 65, 0, 2, 200, 0, Double.POSITIVE_INFINITY);

		// invoke method recursive with a higher threshold, if no particle is thresholded
		if (rt.getCounter() < 2) {
			max++;
			imp.setRoi(x, y, rwidth, rheight);
			max = setThresh (max, ip, imp, x, y, rwidth, rheight);
		}

		// invoke method recursive with a lower threshold, if too many particles are thresholded
		else if (rt.getCounter() >6) {
			max--;
			imp.setRoi(x, y, rwidth, rheight);
			max = setThresh(max, ip, imp, x, y, rwidth, rheight);
		}

		else {
		// read x and y coordinates from results table
		String [] splitt = new String [3];
		double [] xcoo = new double [6];
		double [] ycoo = new double [6];
				
		for (int i =0; i<=rt.getCounter()-1; i++){
			String row = rt.getRowAsString(i);
			splitt =  row.split(",");

			if (splitt.length == 1){
				splitt = row.split("\t");
			}

			xcoo[i] = Double.valueOf(splitt[2]);
			ycoo[i] = Double.valueOf(splitt[3]);
		}

		double [] initialParams = {156,0.01};
		
		CurveFitter cf = new CurveFitter(xcoo, ycoo);

		cf.doCustomFit("y = a + b*x", initialParams, true);
		
		params = cf.getParams();
		result = cf.getResultString();
		
		}

		return max;

	}
	
	public String getString () {

		return result;	
	
	}

	// method to draw the border 
	private void drawBorder (ImageProcessor ip, ImagePlus imp) {

		xPoints = new float [] {0, 0, xwidth, xwidth};	// array with x coordinates of detected border ROI
		yPoints = new float [] {0, (float)params[0], (float)params[0]+(float)params[1]*xwidth, 0 };


		// create polygon for ROI with 4 points
		int nPoints = 4;
		int type = 2;
		
		Roi pr = new PolygonRoi(xPoints, yPoints, nPoints, type);

		int stackSize = imp.getStackSize() ;	// get number of frames 

		for ( int i = imp.getCurrentSlice(); i <= stackSize; i ++)  {
			imp.setSlice(i);
			imp.setRoi(pr);	// draw detected border ROI
		}
		
			GenericDialog gd = new GenericDialog("Set Threshold");
			for (int j = 0; j < yPoints.length; j++){
	      	gd.addNumericField("y", yPoints[j], 0);
			}

	      	gd.showDialog();
	}

}
