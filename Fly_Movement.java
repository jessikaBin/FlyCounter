
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.plugin.ImageCalculator;
import ij.gui.PolygonRoi;
import ij.measure.ResultsTable;
import ij.ImageStack;

import java.util.*;
import java.io.*;
import java.util.Map.Entry;

/**
 * This PlugIn Fly_Movement detects movement of flies in frames and calculates a
 * ratio to indicate this.
 * 
 */
public class Fly_Movement implements PlugInFilter {

	private Detect_Border_CF db = new Detect_Border_CF();

	ImagePlus imp;

	// TreeMap that saves to each frame (key) a movement ratio (value)
	public TreeMap<Integer, ArrayList<Double>> movementRatio = new TreeMap<Integer, ArrayList<Double>>();

	/*
	 * TreeMap that saves to each frame (key) a ArrayList of different numbers
	 * for each the sugar and water side(value) in following order: moving
	 * particles|staying particles|denominator|numerator
	 */
	public TreeMap<Integer, ArrayList<Double>> numbersForFrame = new TreeMap<Integer, ArrayList<Double>>();

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {

		movement(ip);

	}

	// method only used to start the border detection
	public void startBorderDetect(ImagePlus imp, ImageProcessor ip) {
		db.setup("", imp);
		db.run(ip);
	}
	
	// method that sets the ROI for sugar side
	public void setRoiSug(ImagePlus imp, ImageProcessor ip, ResultsTable rt) {

		PolygonRoi sug = setSugarRoi(ip, rt);
		imp.setRoi(sug);
		
	}

	// method that sets the ROI for water side
	public void setRoiWat(ImagePlus imp, ImageProcessor ip) {

		PolygonRoi wat = setWaterRoi();
		imp.setRoi(wat);
		
	}

	// method that gets the coordinates for ROI of water side
	protected PolygonRoi setWaterRoi() {

		// create polygon for ROI with 4 points
		int nPoints = 4;
		int type = 2;

		float[] xPoints = db.getXPointsWat();
		float[] yPoints = db.getYPointsWat();

		float[] copy_of_xPoints = (float[]) xPoints.clone();
		float[] copy_of_yPoints = (float[]) yPoints.clone();

		PolygonRoi water = new PolygonRoi(copy_of_xPoints, copy_of_yPoints, nPoints,
				type);

		return water;
	}

	// method that gets the coordinates for ROI of water side
	protected PolygonRoi setSugarRoi(ImageProcessor ip, ResultsTable rt) {

		// create polygon for ROI with 4 points
		int nPoints = 4;
		int type = 2;

		float[] xPoints = db.getXPointsSug();
		float[] yPoints = db.getYPointsSug();

		float[] copy_of_xPoints = (float[]) xPoints.clone();
		float[] copy_of_yPoints = (float[]) yPoints.clone();

		PolygonRoi sugar = new PolygonRoi(copy_of_xPoints, copy_of_yPoints, nPoints,
				type);

		return sugar;

	}

	// method that analyses the movement
	public void movement(ImageProcessor ip) {

		ResultsTable rt = Analyzer.getResultsTable(); // create results table

		imp = Video_Analysis.getImp();
		int current = imp.getCurrentSlice(); // use current slice

		// create new ImagePlus and ImageProcessor for current frame
		ImagePlus imp1;
		ImageStack is1 = new ImageStack();
		is1 = imp.getStack();
		ImageProcessor ip1 = is1.getProcessor(current);
		imp1 = new ImagePlus("Imp1", ip1);

		// create new ImagePlus and ImageProcessor for frame after current frame
		ImagePlus imp2;
		ImageStack is2 = new ImageStack();
		is2 = imp.getStack();
		ImageProcessor ip2 = is2.getProcessor(current + 1);
		imp2 = new ImagePlus("Imp2", ip2);

		// create ImageCalculator object to perform calculations on frames
		ImageCalculator ic = new ImageCalculator();

		/*
		 * Calculate the difference between the two frames, in order to get the
		 * parts of flies that are moving between them, saves the result in a
		 * new image
		 */
		ImagePlus impMo = ic.run("Difference create", imp1, imp2);
		ImageProcessor ipMo = impMo.getProcessor();

		// a threshold is set to threshold the fly parts
		ipMo.threshold(26);

		// filter out noise particles
		ipMo.dilate();
		ipMo.erode();

		rt.reset();

		float[] xPoints = db.getXPointsWat(); // x coordinates of the calculates
												// ROI for the water side
		float[] yPoints = db.getYPointsWat(); // y coordinates of the calculates
												// ROI for the water side

		double width = (double) xPoints[3]; // width of the image

		boolean w = true; // parameter that is TRUE on water side
		boolean m = true; // parameter that is TRUE when moving

		// counts moving particles on water side
		double movWat = analyseRoi(width, yPoints, ipMo, m, w);

		w = false; // change to sugar side

		// counts moving particles on sugar side
		double movSug = analyseRoi(width, yPoints, ipMo, m, w);

		/*
		 * Calculate the Maximum between the two frames, in order to get the
		 * part of flies that are not changing between the two frames, saves the
		 * result in a new image
		 */
		ImagePlus impSt = ic.run("Max create", imp1, imp2);
		ImageProcessor ipSt = impSt.getProcessor();

		// a threshold is set to threshold the fly parts
		int auto = ipSt.getAutoThreshold();
		ipSt.threshold(auto - 40);

		// filter out noise particles
		ipSt.erode();
		ipSt.dilate();

		rt.reset();

		w = true;
		m = false;

		// counts staying particles on water side
		double stayWat = analyseRoi(width, yPoints, ipSt, m, w);

		// change to sugar side
		w = false;

		// counts staying particles on sugar side
		double staySug = analyseRoi(width, yPoints, ipSt, m, w);

		// ArrayList that saves the numbers for the TreeMap numbersForFrame
		ArrayList<Double> numbers = new ArrayList<Double>();

		// calculate a ratio for the sugar and water side
		double ratioSug = calculate(movSug, staySug, numbers);
		double ratioWat = calculate(movWat, stayWat, numbers);

		// ArrayList that saves the ratios for sugar and water side
		ArrayList<Double> ratio = new ArrayList<Double>();

		ratio.add(ratioSug); // add ratio sugar side
		ratio.add(ratioWat); // add ratio water side

		movementRatio.put(current, ratio); // fill TreeMap
		numbersForFrame.put(current, numbers); // fill TreeMap

	}

	// method that calculates the ratios for the sugar and water side of the
	// arend
	public double calculate(double moving, double staying,
			ArrayList<Double> numbers) {

		double ratio; // parameter for the ratio
		double denominator; // parameter for denominator of equation
		double numerator = moving / 2; // parameter for numerator of equation
		if (moving == 0.0) { // if no flies are moving
			ratio = 0.0;
			denominator = staying;
		} else if (staying == 0.0) { // if all flies are moving
			ratio = 1.0;
			denominator = moving / 2;
		} else {
			denominator = (moving / 2) + staying;
			ratio = (moving / 2) / denominator; // calculate ratio
			ratio = (double) Math.round(ratio * 100) / 100; // round ratio on
															// two decimal
															// places
		}

		// fill treeMap with numbers
		numbers.add(moving);
		numbers.add(staying);
		numbers.add(denominator);
		numbers.add(numerator);

		return ratio;

	}

	// method that counts the pixels that indicate flies
	protected double analyseRoi(double width, float[] yPoints,
			ImageProcessor ip, boolean m, boolean w) {

		double count = 0.0; // parameter for counting pixels
		double max = Math.floor(Math.max(yPoints[1], yPoints[2])); // higher y
																	// coordinate
		double min = Math.floor(Math.min(yPoints[1], yPoints[2])); // lower y
																	// coordinate

		/*
		 * TreeMap that uses the former calculated x coordinates to the y
		 * coordinates between max and min
		 */
		TreeMap<Double, Double> values = new TreeMap<Double, Double>();
		values = (TreeMap<Double, Double>) db.calculateCoordinates();

		if (w == true) {	// water side

			for (int j = 0; j < min; j++) {	// calculates in straight part of ROI
				for (int k = 0; k <= width; k++) {
					if (m == true) {	// moving
						if (ip.getPixel(k, j) == 255) {	// all white pixels indicate flies
							count++;
						}
					} else {	// staying
						if (ip.getPixel(k, j) == 0) {	// all black pixels indicate flies
							count++;
						}
					}
				}
			}

			for (double i = min; i <= max; i++) {	// calculate in slope part of ROI

				double coord = values.get(i);	// get x coordinate to current y coordinate

				if (max == yPoints[1]) {	// check pixels in row until x coordinate
					for (double l = 0; l <= coord; l++) {
						if (m == true) {	// moving
							if (ip.getPixelInterpolated(l, i) == 255) { // all white pixels indicate flies
								count++;
							}
						} else {	// staying
							if (ip.getPixelInterpolated(l, i) == 0) { // all black pixels indicate flies
								count++;
							}
						}
					}
				} else {	// check pixels in row from x coordinate on 
					for (double l = coord; l <= width; l++) {
						if (m == true) {	// moving
							if (ip.getPixelInterpolated(l, i) == 255) { // all white pixels indicate flies
								count++;
							}
						} else { // staying
							if (ip.getPixelInterpolated(l, i) == 0) { // all black pixels indicate flies
								count++;
							}
						}
					}
				}
			}

		} else {	// sugar side
			for (int j = (int) max + 1; j <= width; j++) { // calculate straight part of ROI
				for (int k = 0; k <= width; k++) {
					if (m == true) {
						if (ip.getPixel(k, j) == 255) {
							count++;
						}
					} else {
						if (ip.getPixel(k, j) == 0) {
							count++;
						}
					}
				}
			}

			for (double i = min; i <= max; i++) { // calculate slope part of ROI

				double coord = values.get(i);	// get x coordinate to current y coordinate

				if (max == yPoints[1]) { // check pixels in row from x coordinate on
					for (double l = coord; l <= width; l++) {
						if (m == true) { // moving
							if (ip.getPixelInterpolated(l, i) == 255) {
								count++;
							}
						} else {	// staying
							if (ip.getPixelInterpolated(l, i) == 0) {
								count++;
							}
						}
					}
				} else {	// check pixels in row until x coordinate
					for (double l = 0; l <= coord; l++) {
						if (m == true) {	// moving
							if (ip.getPixelInterpolated(l, i) == 255) {
								count++;
							}
						} else {	// staying
							if (ip.getPixelInterpolated(l, i) == 0) {
								count++;
							}
						}
					}
				}
			}
		}

		return count;

	}

	// get-method that returns the TreeMap that saves the movement ratio 
	public TreeMap<Integer, ArrayList<Double>> getMovementRatio() {

		return movementRatio;

	}

	// get method that returns the TreeMap that saves the miscellaneous numbers
	public TreeMap<Integer, ArrayList<Double>> getnumbersForFrame() {

		return numbersForFrame;

	}

	// method that makes the output of TreeMap numbersForFrame
	public void outputNumbers(ImagePlus imp) throws IOException {

		String title = imp.getTitle();
		String path = Batch_Run.myDir2;	// input path

		File file = new File(path + title.substring(0, title.length() - 2)
				+ "_denominator.txt");
		java.io.Writer output = new BufferedWriter(new FileWriter(file));

		output.write("Frame" + "\t" + "Moving Sugar" + "\t" + "Staying Sugar"
				+ "\t" + "Denominator Sugar" + "\t" + "Numerator Sugar" + "\t"
				+ "Moving Water" + "\t" + "Staying Water" + "\t"
				+ "Denominator Water" + "\t" + "Numerator Water" + "\n");

		for (Entry<Integer, ArrayList<Double>> entry : numbersForFrame
				.entrySet()) {
			output.write(((Integer) entry.getKey()).intValue() + "\t");

			for (int k = 0; k < entry.getValue().size(); k++) {
				output.write(((Double) entry.getValue().get(k)).doubleValue()
						+ "\t");
			}
			output.write("\n");
		}

		output.close();

	}

	// method that makes the output of TreeMap movingRatio
	public void outputRatios(ImagePlus imp) throws IOException {

		String title = imp.getTitle();
		String path = Batch_Run.myDir2;

		File file = new File(path + title.substring(0, title.length() - 2)
				+ "_movingRatio.txt");
		java.io.Writer output = new BufferedWriter(new FileWriter(file));

		output.write("Frame" + "\t" + "Moving Rate Sugar" + "\t"
				+ "Moving Rate Water" + "\n");

		for (Entry<Integer, ArrayList<Double>> entry : movementRatio.entrySet()) {

			output.write((((Integer) entry.getKey()).intValue()) + "\t");
			for (int k = 0; k < entry.getValue().size(); k++) {
				output.write(((Double) entry.getValue().get(k)).doubleValue()
						+ "\t");
			}
			output.write("\n");
		}

		output.close();

	}
}
