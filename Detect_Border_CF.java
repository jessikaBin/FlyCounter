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
	public int goBack;

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
		int rheight = height/8;	// height
		int x = width/4;		// upper left x coordinate
		int y = 7*height/16;	// upper left y coordinate
		
			
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
	


	private double setThresh (double max, ImageProcessor ip, ImagePlus imp, int x, int y, int rwidth, int rheight) {

		// setting the threshold
		double minThreshold = 0.0;
		ip.setThreshold(minThreshold, max, 0);	

		// define results table
		ResultsTable rt = Analyzer.getResultsTable();

		// measurements: area & center of mass, options: show nothing, minSize: particle not smaller than 3, maxSize: particle not bigger than 200, minCirc/maxCirc: no  defined circularity
		rt = tableAnalyser (imp, rt, 65, 0, 3, 200, 0, 1);

		// invoke method recursive with a higher threshold, if no particle is thresholded
		if (rt.getCounter() < 2) {
			max++;
			imp.setRoi(x, y, rwidth, rheight);
			max = setThresh (max, ip, imp, x, y, rwidth, rheight);
		}

		// invoke method recursive with a lower threshold, if too many particles are thresholded
		else if (rt.getCounter() > 6) {
			max--;
			imp.setRoi(x, y, rwidth, rheight);
			max = setThresh(max, ip, imp, x, y, rwidth, rheight);
		}

		else {
		
			ArrayList <Integer> roiX = new ArrayList <Integer> ();	// x coordinates of detected border ROI
			ArrayList <Integer> roiY = new ArrayList <Integer> ();	// y coordinates of detected border ROI 
		
		
			for (int rh = 0; rh <= rheight; rh++) {
				for (int rw = 0; rw <= rwidth; rw++) {
					int beginX = x+rw;
					int beginY = y+rh;
						if (ip.getPixel(beginX, beginY) <= max) {
							roiX.add(beginX);
							roiY.add(beginY);
						}
				}
			}
		
			double [] xcoo = new double [roiX.size()];
			double [] ycoo = new double [roiY.size()];
		
			for ( int j = 0; j < roiX.size();j++){
				xcoo[j] = (double)roiX.get(j);
				ycoo[j] = (double)roiY.get(j);
			}
			
			double [] initialParams = {156,0.01};
		
			CurveFitter cf = new CurveFitter(xcoo, ycoo);

			cf.doCustomFit("y = a + b*x", initialParams, false);
		
			params = cf.getParams();
			result = cf.getResultString();
			
			if (params[1] > 0.03 || params[1] < -0.03) {
				imp.setSlice(imp.getCurrentSlice()-1);
				run(ip);
			
			}
			
		}

		return max;

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
		
	}
	
	public float [] getXPoints () {

		return xPoints;	
	
	}


	public float [] getYPoints () {

		return yPoints;	
	
	}
	
	public String getString () {

		return result;	
	
	}

}
