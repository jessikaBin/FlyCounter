
import ij.*;
import ij.measure.*;
import ij.process.*;
import ij.plugin.filter.*;
import java.util.*;
import ij.measure.CurveFitter;

/**
 * This PlugIn Detect_Border_CF detects the border between the sugar and water
 * side of the fly arena.
 */
public class Detect_Border_CF implements PlugInFilter {

	ImagePlus imp;

	public float[] xPointsWat = new float[4]; // x coordinates for ROI of the
												// water side
	public float[] yPointsWat = new float[4]; // y coordinates for ROI of the
												// water side

	public float[] xPointsSug = new float[4]; // x coordinates for ROI of the
												// sugar side
	public float[] yPointsSug = new float[4]; // y coordinates for ROI of the
												// sugar side

	private float xwidth = (float) 0.0; // float width of image
	private double lowerThreshold; // threshold for finding the border
	public double[] params = new double[2]; // array that includes the
											// calculated parameters of the
											// fitted line

	/*
	 * TreeMap that saves to all y coordinates of the diagonal part of the ROI
	 * (key) the calculated x value (value) at which the ROI begin/ends
	 * (depending whether the larger y value is on the left or right side of the
	 * frame
	 */
	protected TreeMap<Double, Double> borderValues = new TreeMap<Double, Double>();

	public int setup(String arg, ImagePlus imp) {

		this.imp = imp;
		return DOES_ALL;

	}

	public void run(ImageProcessor ip) {

		// get size of image
		int width = ip.getWidth(); // width of frame
		int height = ip.getHeight(); // height of frame

		xwidth = (float) width;

		// define size of ROI in which the border is searched
		int rwidth = width / 2; // width
		int rheight = height / 8; // height
		int x = width / 4; // upper left x coordinate
		int y = 7 * height / 16; // upper left y coordinate

		ip.findEdges(); // perform findEdges on frame
		imp.setRoi(x, y, rwidth, rheight); // set former defined Roi

		imp.repaintWindow();

		// set threshold
		lowerThreshold = 30.0; // starting threshold for border search
		setThresh(lowerThreshold, ip, imp, x, y, rwidth, rheight); // applying
																	// setThresh
		// method

		calculateBorder(ip, imp); // applying drawBorder method

	}

	// method to define ResultsTable and run Particle Analyser
	protected static ResultsTable tableAnalyser(ImagePlus imp, ResultsTable rt,
			int measurements, int options, double minSize, double maxSize,
			double minCirc, double maxCirc) {

		// define results table
		rt.reset();

		// set particle analyzer
		ParticleAnalyzer pa = new ParticleAnalyzer(options, measurements, rt,
				minSize, maxSize, minCirc, maxCirc);
		pa.analyze(imp); // apply particle analysis

		return rt;

	}

	// method that sets the right threshold so that the object is found
	private double setThresh(double min, ImageProcessor ip, ImagePlus imp,
			int x, int y, int rwidth, int rheight) {

		// setting the threshold
		double maxThreshold = 255.0; // upper threshold bound
		ip.setThreshold(min, maxThreshold, 0);

		// define results table
		ResultsTable rt = Analyzer.getResultsTable();

		// measurements: area & center of mass, options: show nothing, minSize:
		// particle not smaller than 3, maxSize: particle not bigger than 200,
		// minCirc/maxCirc: no defined circularity
		rt = tableAnalyser(imp, rt, 65, 0, 3, 200, 0, 1);

		// invoke method recursive with a lower threshold, if too less particles
		// are
		// thresholded
		if (rt.getCounter() < 2) {
			min--;
			imp.setRoi(x, y, rwidth, rheight);
			min = setThresh(min, ip, imp, x, y, rwidth, rheight);
		}

		// invoke method recursive with a higher threshold, if too many
		// particles
		// are thresholded
		else if (rt.getCounter() > 6) {
			min++;
			imp.setRoi(x, y, rwidth, rheight);
			min = setThresh(min, ip, imp, x, y, rwidth, rheight);
		}

		// right threshold for finding the border is set
		else {

			ArrayList<Integer> roiX = new ArrayList<Integer>(); // x coordinates
																// of
																// thresholded
																// pixel
			ArrayList<Integer> roiY = new ArrayList<Integer>(); // y coordinates
																// of
																// thresholdes
																// pixel
			// the ROI is searched for pixels that are thresholded and their
			// coordinates are safed
			for (int rh = 0; rh <= rheight; rh++) {
				for (int rw = 0; rw <= rwidth; rw++) {
					int beginX = x + rw;
					int beginY = y + rh;
					if (ip.getPixel(beginX, beginY) >= min) {
						roiX.add(beginX); // save x coordinate
						roiY.add(beginY); // save y coordinate
					}
				}
			}

			double[] xcoo = new double[roiX.size()];
			double[] ycoo = new double[roiY.size()];

			for (int j = 0; j < roiX.size(); j++) {
				xcoo[j] = (double) roiX.get(j); // save former found x
												// coordinates in an array
				ycoo[j] = (double) roiY.get(j); // save former found y
												// coordinates in an array
			}

			// set initial parameters for curvefitter
			double[] initialParams = { 156, 0.01 };

			// create CurveFitter object on the coordinates of the pixels
			CurveFitter cf = new CurveFitter(xcoo, ycoo);

			// fit straight line to chosen pixels
			cf.doCustomFit("y = a + b*x", initialParams, false);

			params = cf.getParams(); // get calculated parameters

			// if parameter indicate a too steep border, flies have disturbed
			// the fitting
			// repeat finding the border one frame before
			if (params[1] > 0.08 || params[1] < -0.08) {
				imp.setSlice(imp.getCurrentSlice() - 1); // decrease current
															// frame number
				run(ip);
			}
		}

		return min;

	}

	/*
	 * method that calculates to all y coordinates of the diagonal part of the
	 * ROI an x value (value) at which the ROI begin/ends (depending whether the
	 * larger y value is on the left or right side of the frame
	 */
	protected TreeMap<Double, Double> calculateCoordinates() {

		double begin = Math.floor(Math.min(params[0], params[0] + params[1]
				* xwidth)); // lower y value
		double end = Math.floor(Math.max(params[0], params[0] + params[1]
				* xwidth)); // higher y value

		for (double i = begin; i <= end; i++) {
			double value = (i - params[0]) / params[1]; // calculate x value
			borderValues.put(i, value); // fill TreeMap
		}

		return borderValues;
	}

	// method to draw the border
	private void calculateBorder(ImageProcessor ip, ImagePlus imp) {

		// // array with x coordinates of water side
		xPointsWat = new float[] { 0, 0, xwidth, xwidth };

		// array with y coordinates of water side
		yPointsWat = new float[] { 0, (float) params[0],
				(float) params[0] + (float) params[1] * xwidth, 0 };

		// array with x coordinates of sugar side
		xPointsSug = new float[] { xPointsWat[0], xPointsWat[1], xPointsWat[2],
				xPointsWat[3] };

		// array with y coordinates of sugar side
		float a = yPointsWat[1] + (float) 1.0;
		float b = xPointsWat[2];
		float c = xPointsWat[2];
		float d = yPointsWat[2] + (float) 1.0;
		yPointsSug = new float[] { a, b, c, d };

	}

	// get-method to get x coordinates of water side
	public float[] getXPointsWat() {

		float[] copy_of_xPointsWat = (float[]) xPointsWat.clone();

		return copy_of_xPointsWat;

	}

	// get-method to get y coordinates of water side
	public float[] getYPointsWat() {

		float[] copy_of_yPointsWat = (float[]) yPointsWat.clone();

		return copy_of_yPointsWat;

	}

	// get-method to get x coordinates of sugar side
	public float[] getXPointsSug() {

		float[] copy_of_xPointsSug = (float[]) xPointsSug.clone();

		return copy_of_xPointsSug;

	}

	// get-method to get y coordinates of sugar side
	public float[] getYPointsSug() {

		float[] copy_of_yPointsSug = (float[]) yPointsSug.clone();

		return copy_of_yPointsSug;

	}
}
