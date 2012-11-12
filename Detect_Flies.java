
import ij.*;
import ij.measure.*;
import ij.process.*;
import java.util.ArrayList;
import ij.plugin.filter.*;
import java.util.*;

/**
 * This PlugIn Detect_Flies performs the fly detection in each frame of a video.
 * It invokes a method of the Maximum_Finder_Modified.java class with preset paramaters
 * and makes a filtering of the found maxima.
 *
 */
public class Detect_Flies implements PlugInFilter {

	ImagePlus imp;

	private Maximum_Finder_Modified mf = new Maximum_Finder_Modified(); // shortly
																		// modified
																		// version
																		// of
																		// the
																		// MaximumFinder

	// TreeMap that saves the current frame as key and the counted flies as
	// value
	protected TreeMap<Double, Double> countedFlies = new TreeMap<Double, Double>();

	protected double flies; // variable for counted flies

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {

		mf.setup("", imp);

		// use a noise tolerance 17 (the higher the number, the less found
		// maxima)
		for (double tolerance = 17; tolerance <= 17; tolerance++) {
			findFlies(ip, imp, tolerance); // find maxima in frame
			filterMaximaWithDifference(ip, tolerance); // filter found maxima
		}
	}

	// method to find maxima (possible flies)
	private void findFlies(ImageProcessor ip, ImagePlus imp, double tolerance) {

		double threshold = ImageProcessor.NO_THRESHOLD; // no threshold is
														// applied on the frame
		int outputType = 4; // output is a list of all maxima
		boolean excludeOnEdges = true; // edge particles are included
		boolean isEDM = false;

		// part from original run-method from MaximumFinder to invert picture
		// (because of light background)
		float[] cTable = ip.getCalibrationTable();
		ip = ip.duplicate();

		if (cTable == null) { // invert image for finding minima of uncalibrated
								// images
			ip.invert();
		} else { // we are using getPixelValue, so the CalibrationTable must be
					// inverted
			float[] invertedCTable = new float[cTable.length];
			for (int i = cTable.length - 1; i >= 0; i--) {
				invertedCTable[i] = -cTable[i];
			}
			ip.setCalibrationTable(invertedCTable);
		}

		// invoking the findMaxima method of the MaximumFinder
		mf.findMaxima(ip, tolerance, threshold, outputType, excludeOnEdges,
				isEDM);

	}

	// method that filters the previously found maxima
	private void filterMaximaWithDifference(ImageProcessor ip, double tolerance) {

		// get maxima found by MaximumFinder
		ResultsTable rt = Analyzer.getResultsTable();
		rt = mf.getResultsTable();

		String[] splitt = new String[3];
		ArrayList<Double> xcoo = new ArrayList<Double>(); // array list for
															// saving the x
															// values
		ArrayList<Double> ycoo = new ArrayList<Double>(); // array list for
															// saving the y
															// values
		for (int i = 0; i <= rt.getCounter() - 1; i++) {
			String row = rt.getRowAsString(i);
			splitt = row.split(",");
			if (splitt.length == 1) {
				splitt = row.split("\t");
			}
			double x = Double.parseDouble(splitt[1]); // x values in first
														// column
			double y = Double.parseDouble(splitt[2]); // y values in second
														// column
			xcoo.add(x); // save x value in arraylist
			ycoo.add(y); // save y value in arraylist
		}

		// check for each found maxima whether it is a fly
		flies = 0; // counter for found flies is set to zero

		// all found maxima are checked
		for (int j = 0; j < xcoo.size(); j++) {

			double x_max = xcoo.get(j); // set current x coordinate
			double y_max = ycoo.get(j); // set current y coordinate

			double counter = 0; // set counter for cases in which the condition
								// is true

			// check pixel values for found maxima and their 8 neighbor pixels
			for (int hor = -1; hor <= 1; hor++) {
				for (int ver = -1; ver <= 1; ver++) {

					int diff = 70; // maximum difference between pixel values
					int abs = 105; // cutoff for absolute pixel value

					// if pixelvalue of centerpixel is < abs and the value
					// difference to the 8 neighbour pixels is < diff =>
					// possible fly

					if ((ip.getPixelValue((int) x_max + hor, (int) y_max + ver) < abs)
							&& (Math.abs(ip.getPixelValue((int) x_max + hor,
									(int) y_max + ver)
									- ip.getPixelValue((int) x_max + hor - 1,
											(int) y_max + ver)) < diff)
							&& (Math.abs(ip.getPixelValue((int) x_max + hor,
									(int) y_max + ver)
									- ip.getPixelValue((int) x_max + hor - 1,
											(int) y_max + ver - 1)) < diff)
							&& (Math.abs(ip.getPixelValue((int) x_max + hor,
									(int) y_max + ver)
									- ip.getPixelValue((int) x_max + hor,
											(int) y_max + ver - 1)) < diff)
							&& (Math.abs(ip.getPixelValue((int) x_max + hor,
									(int) y_max + ver)
									- ip.getPixelValue((int) x_max + hor + 1,
											(int) y_max + ver - 1)) < diff)
							&& (Math.abs(ip.getPixelValue((int) x_max + hor,
									(int) y_max + ver)
									- ip.getPixelValue((int) x_max + hor + 1,
											(int) y_max + ver)) < diff)
							&& (Math.abs(ip.getPixelValue((int) x_max + hor,
									(int) y_max + ver)
									- ip.getPixelValue((int) x_max + hor + 1,
											(int) y_max + ver + 1)) < diff)
							&& (Math.abs(ip.getPixelValue((int) x_max + hor,
									(int) y_max + ver)
									- ip.getPixelValue((int) x_max + hor,
											(int) y_max + ver + 1)) < diff)
							&& (Math.abs(ip.getPixelValue((int) x_max + hor,
									(int) y_max + ver)
									- ip.getPixelValue((int) x_max + hor - 1,
											(int) y_max + ver + 1)) < diff)) {

						counter++;
					}
				}
			}

			// if min. 6 neighbored pixels suggest a possible fly => real fly
			if (counter > 5) {
				flies++;
			}
		}
	}

	// get-method that returns the number of counted flies
	public double getCountedFlies() {

		return flies;

	}
}
