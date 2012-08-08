import ij.*;
import ij.measure.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import java.util.*;
import java.awt.Color;

public class Detect_Border implements PlugInFilter {

	ImagePlus imp;

	public float [] xPoints = new float [4];
	public float [] yPoints = new float [4]; 

	private float xwidth = (float)0.0;	// float width of image
	private double max;

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
		
		// get size and position of ROI 1 (left)
		int rwidth = width/5;	// width
		int rheight = height/5;	// height
		int x = width/6;		// upper left x coordinate
		int y = 2*height/5;	// upper left y coordinate
		imp.setRoi(x, y, rwidth, rheight);		// set ROI 1

		imp.repaintWindow();

		// set threshold
		max = 140.0;	// threshold
		setThresh(max, ip, imp, x, y, rwidth, rheight);	// applying setThresh method

		ip.resetRoi();

		// get size and position of ROI 2 (right)
		x = 5*width/9;	// upper left x coordinate
		imp.setRoi(x, y, rwidth, rheight);		// set ROI 2

		imp.repaintWindow();

		// set threshold
		max = setThresh(max, ip, imp, x, y, rwidth, rheight);	// applying setThresh method
		
		drawBorder (ip,imp);	// applying drawBorder method

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


	// method to set the threshold
	//>> setTresh gibt es doch in Border_Substack schon?
	//>> Die Rekursion hat nicht wirklich eine vernuenftige 
	//>> terminationsbedingung und die xy werte werden auch mehrfach 
	//>> gespeicher. vieleicht wäre eine else abzweigung ganz gut?
	private double setThresh (double max, ImageProcessor ip, ImagePlus imp, int x, int y, int rwidth, int rheight) {

		// setting the threshold
		double minThreshold = 0.0;
		ip.setThreshold(minThreshold, max, 0);	

		// define results table
		ResultsTable rt = Analyzer.getResultsTable();

		// measurements: area & center of mass, options: show nothing, minSize: particle not smaller than 3, maxSize: particle not bigger than 100, minCirc/maxCirc: no  defined circularity
		rt = tableAnalyser (imp, rt, 65, 0, 3, 100, 0, Double.POSITIVE_INFINITY);

		// invoke method recursive with a higher threshold, if no particle is thresholded
		if (rt.getCounter() < 1) {
			max++;
			imp.setRoi(x, y, rwidth, rheight);
			max = setThresh (max, ip, imp, x, y, rwidth, rheight);
		}

		// invoke method recursive with a lower threshold, if too many particles are thresholded
		if (rt.getCounter() >3) {
			max--;
			imp.setRoi(x, y, rwidth, rheight);
			max = setThresh (max, ip, imp, x, y, rwidth, rheight);
		}

		// read x and y coordinates from results table
		String [] splitt = new String [3];
		ArrayList <String> xcoo = new ArrayList <String> (); 
		ArrayList <String> ycoo = new ArrayList <String> (); 

		for (int i =0; i<=rt.getCounter()-1; i++){
			String row = rt.getRowAsString(i);
			splitt =  row.split(",");

			if (splitt.length == 1){
				splitt = row.split("\t");
			}

			xcoo.add(splitt[2]);
			ycoo.add(splitt[3]);
		}

		double ox1 = Double.parseDouble(xcoo.get(0));
		double oy1 = Double.parseDouble(ycoo.get(0));

		xa.add((float)ox1);		// saving x coordinates of thresholded areas 
		ya.add((float)oy1);		// saving y coordinates of thresholded areas

		return max;

	}

	// method to draw the border 
	private void drawBorder (ImageProcessor ip, ImagePlus imp) {

		xPoints = new float [] {0, 0, xwidth, xwidth};	// array with x coordinates of detected border ROI
		
		// create array with y coordinates of detected border ROI
		if (xa.get(0) > xa.get(xa.size() - 1)) {	// first x value is bigger than second, then the corresponding y value specifies right height
			yPoints = new float [] {0, ya.get(xa.size() - 1), ya.get(0), 0 };
		} else {					// first x value is lower than second, then the corresponding y value specifies left height
			yPoints = new float [] {0, ya.get(0), ya.get(xa.size() - 1), 0 };
		}

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

}
