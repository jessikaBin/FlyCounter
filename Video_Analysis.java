
import ij.*;
import ij.measure.*;
import ij.process.*;
import ij.gui.*;
import java.util.ArrayList;
import ij.plugin.filter.*;

import java.io.*;
import java.util.*;


/**
 * This PlugIn Video_Analysis performs the analysis of each video and
 * invokes classes that are used for substeps.
 *
 */
public class Video_Analysis implements PlugInFilter {

	static ImagePlus imp;
	ImagePlus imp2;
	ImageProcessor ip2;
	ImageWindow iw;
	static Roi prSugar; // ROI for sugar side of arena
	static Roi prWater; // ROI for water side of arena

	private int flyCount = 0; // count for testing whether flies are in frame or
								// not

	private Detect_Flies df = new Detect_Flies();
	private Fly_Movement fm = new Fly_Movement();

	public String result = "";

	private boolean mov = Batch_Run.mov; // variable, if movement analysis
											// should be applied
	private boolean det = Batch_Run.det; // variable, if preference index should
											// be calculated

	// TreeMap, that saves current slice as key and the corresponding preference
	// index as value
	protected TreeMap<Integer, Double> preferenceIndex = new TreeMap<Integer, Double>();

	// TreeMap, that saves current slice as key and the corresponding ArrayList
	// with the movement ratios as values
	// the arraylist has the sugar side on first, and water side on second
	// position
	protected TreeMap<Integer, ArrayList<Double>> movementRatio = new TreeMap<Integer, ArrayList<Double>>();

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {

		df.setup("", imp);
		fm.setup("", imp);

		// get size of image
		int width = ip.getWidth();
		int height = ip.getHeight();

		// get size and position of ROI
		int rwidth = width / 2; // width
		int rheight = height / 4; // height
		int x = width / 4; // upper left x coordinate
		int y = 3 * height / 8; // upper left y coordinate

		int stackSize = imp.getStackSize(); // get number of frames
		int curr = 2; // variable for starting frame of substack

		for (int i = 2; i <= stackSize; i++) { // start with frame 2, because
												// frame 1 is black
			imp.setSlice(i); // current slice is set

			if (i == 2) { // only in frame 2 the threshold is set
				imp.setRoi(x, y, rwidth, rheight); // set ROI

				// set threshold
				double startingThreshold = 140.0; // starting threshold
				startingThreshold = setThresh(startingThreshold, ip, imp, x, y,
						rwidth, rheight); // applying setThresh method
			}

			findFirstFlies(ip, imp); // applying findFirstFlies method

			if (flyCount != 0) { // if flies are found in current slice
				curr = imp.getCurrentSlice(); // starting frame is changed to
												// current slice
				imp.setSlice(curr - 2); // go 2 frames backwards

				// start border detection
				fm.startBorderDetect(imp, ip);
				break; // stop searching for first flies, once they are found
			}
		}

		imp.killRoi();
		startAnalysis(ip, imp, curr); // start analysis for video

		if (mov == true) { // create output file if movement analysis is chosen
			try {
				fm.outputRatios(imp); // output movement ratios
				fm.outputNumbers(imp); // output denominators
				// fm.createChannelVideo();
			} catch (IOException e) {
				System.err.println("Problem with writing the file");
			}
		}
	}

	// method to set the threshold in the first frame
	private double setThresh(double startingThreshold, ImageProcessor ip,
			ImagePlus imp, int x, int y, int rwidth, int rheight) {

		double minThreshold = 0.0; // lower threshold bound
		ip.setThreshold(minThreshold, startingThreshold, 0); // set chosen
																// threshold
		// define results table
		ResultsTable rt = Analyzer.getResultsTable();

		// measurements: area & circularity, options: show nothing, minSize:
		// particle not smaller than 3, maxSize: infinity, minCirc/maxCirc: no
		// defined circularity
		rt = Detect_Border_CF.tableAnalyser(imp, rt, 8193, 0, 3,
				Double.POSITIVE_INFINITY, 0, 1); // analyse particles

		if (rt.getCounter() == 0) { // if no pixels are thresholded in ROI,
									// increase upper threshold bound
			startingThreshold++;
			imp.setRoi(x, y, rwidth, rheight);
			setThresh(startingThreshold, ip, imp, x, y, rwidth, rheight); // recursive
																			// method
																			// invoke
																			// of
																			// setThresh
		}

		else if (rt.getCounter() == 1) {

			// split results from resultsTable to work with them
			String[] splitt = new String[3];
			String row = rt.getRowAsString(0);
			splitt = row.split(",");
			if (splitt.length == 1) {
				splitt = row.split("\t");
			}
			double a = Double.parseDouble(splitt[1]); // area values are
														// in first column
			// chose next frame, if particle size is too high or
			// increase upper threshold
			if (a >= 3000) {
				imp.setSlice(imp.getCurrentSlice() + 1);
				imp.setRoi(x, y, rwidth, rheight);
				setThresh(startingThreshold, ip, imp, x, y, rwidth, rheight); // recursive
																				// method
																				// invoke
																				// of
																				// setThresh
			} else {
				startingThreshold++;
				imp.setRoi(x, y, rwidth, rheight);
				setThresh(startingThreshold, ip, imp, x, y, rwidth, rheight); // recursive
																				// method
																				// invoke
																				// of
																				// setThresh

			}
		}

		// if too many particles are threshold, increase upper threshold bound
		else if (rt.getCounter() > 3) {
			startingThreshold++;
			imp.setRoi(x, y, rwidth, rheight);
			setThresh(startingThreshold, ip, imp, x, y, rwidth, rheight); // recursive
																			// method
																			// invoke
																			// of
																			// setThresh
		}

		return startingThreshold;

	}

	// method to find first flies in frame
	private int findFirstFlies(ImageProcessor ip, ImagePlus imp) {

		// define results table
		ResultsTable rt = Analyzer.getResultsTable();

		// measurements: area & circularity, options: show nothing, minSize:
		// flies not smaller than 100, maxSize: flies not bigger than 800,
		// minCirc/maxCirc: flies have circularity between 0.5 and 0.9
		rt = Detect_Border_CF.tableAnalyser(imp, rt, 8193, 0, 100, 800, 0.5,
				0.9);

		String[] splitt = new String[3];
		ArrayList<Double> circ = new ArrayList<Double>(); // array list for
															// saving the values
															// for circularity

		for (int i = 0; i <= rt.getCounter() - 1; i++) { // circularity values
															// of results table
															// are saved in
															// array list
			String row = rt.getRowAsString(i);
			splitt = row.split(",");
			if (splitt.length == 1) {
				splitt = row.split("\t");
			}
			double c = Double.parseDouble(splitt[2]); // circularity values are
														// in second column
			circ.add(c);
		}

		flyCount = 0; // counted flies in current frame is set to 0

		if (circ.size() >= 2) { // more than 2 flies are found in current frame
			flyCount++; // counted flies is increased
		}

		return flyCount;
	}

	// method that starts and performs the analysis for each frame
	public void startAnalysis(ImageProcessor ip, ImagePlus imp, int curr) {

		int stackSize = imp.getStackSize(); // get number of frames

		// perform analysis from first frame with flies until the end
		for (int i = curr; i <= stackSize; i++) {
			imp.setSlice(i); // current slice is set
			performAnalysis(ip, imp, i, preferenceIndex); // applying setThresh
															// method
		}
	}

	// get-Method to get the TreeMap for the preference index
	public TreeMap<Integer, Double> getPrefInd() {

		return preferenceIndex;

	}

	// get-Method to get the ImagePlus for the frame
	public static ImagePlus getImp() {

		return imp;
	}

	// method that performs the preference index calculation and invokes the
	// movement analysis
	private void performAnalysis(ImageProcessor ip, ImagePlus imp, int i,
			TreeMap<Integer, Double> preferenceIndex) {

		if (det == true) { // if preference index should be calculated

			ResultsTable rt = Analyzer.getResultsTable(); // create resultsTable

			prWater = fm.setWaterRoi();
			imp.setRoi(prWater); // set ROI for water side
			df.run(ip); // flies are detected
			double wat = df.getCountedFlies(); // number of flies on water side

			imp.killRoi();

			prSugar = fm.setSugarRoi(ip, rt);
			imp.setRoi(prSugar); // set ROI for sugar side
			df.run(ip); // flies are detected
			double sug = df.getCountedFlies(); // number of flies in sugar side

			double prefInd = (sug - wat) / (sug + wat); // calculation of
														// preference index
			preferenceIndex.put(i, prefInd); // fill TreeMap for preference
												// index
		}

		if ((mov == true) && (i <= imp.getStackSize() - 1)) { // if movement
																// should be
																// analyzed
			fm.run(ip); // start movement detection
			movementRatio = fm.getMovementRatio();
		}
	}
}
