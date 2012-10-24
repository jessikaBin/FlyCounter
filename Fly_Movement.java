import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.filter.*;
import ij.plugin.ImageCalculator;
import ij.measure.ResultsTable;
import ij.plugin.filter.ParticleAnalyzer;
import ij.plugin.SubstackMaker;
import ij.plugin.filter.Binary;
import ij.plugin.RGBStackMerge;
import ij.ImageStack;

import java.util.TreeMap;
import java.util.*;
import java.io.*;
import java.util.Map.Entry;


public class Fly_Movement implements PlugInFilter {

	private Detect_Border_CF db = new Detect_Border_CF ();

	ImagePlus imp;
	
	private AVI_Writer aw = new AVI_Writer();

	
	public TreeMap <Integer, ArrayList<Double>> movementRatio = new TreeMap <Integer, ArrayList<Double>> ();
	public TreeMap <Integer, ArrayList <Double>> numbersForFrame = new TreeMap <Integer, ArrayList <Double>> ();
	
	public ArrayList <ImageProcessor> slices = new ArrayList <ImageProcessor> ();
	
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
	
		
		movement (ip);
				
		// try {
			// outputRatios(imp);
			// outputNumbers(imp);
		// } catch (IOException e) {
			// System.err.println("Problem with writing the file");
		// }
		
	}
	
	public void startBorderDetect (ImagePlus imp, ImageProcessor ip) {
		db.setup("",imp);
		db.run(ip);
	}
	
	public void setRoiSug (ImagePlus imp, ImageProcessor ip, ResultsTable rt) {
	
			Roi sug = setSugarRoi(ip, rt);
			imp.setRoi(sug);
	
	}
	
	public void setRoiWat (ImagePlus imp, ImageProcessor ip, ResultsTable rt) {
	
			Roi wat = setWaterRoi(rt);
			imp.setRoi(wat);
	
	}
	
	protected Roi setWaterRoi(ResultsTable rt){
			
		// create polygon for ROI with 4 points
		int nPoints = 4;
		int type = 2;
	
		float [] xPoints = db.getXPointsWat (); 
		float [] yPoints = db.getYPointsWat ();   

		//ip.drawLine((int) xPoints[1], (int)yPoints[1] , (int)xPoints[2], (int) yPoints[2]) ;

		float [] copy_of_xPoints = (float[])xPoints.clone();
		float [] copy_of_yPoints = (float[])yPoints.clone();
		
		Roi water = new PolygonRoi(copy_of_xPoints, copy_of_yPoints, nPoints, type);
		
				// rt.reset();
		
				// for ( int i = 0; i<xPoints.length; i++){
			// rt.incrementCounter();
			
			// rt.addValue("X", xPoints[i]);
			// rt.addValue("Y", yPoints[i]);	
		// }
		
		// rt.show ("Results");
		
		return water;
	}
	
	protected Roi setSugarRoi(ImageProcessor ip, ResultsTable rt){
	
		// create polygon for ROI with 4 points
		int nPoints = 4;
		int type = 2;
	
		float [] xPoints = db.getXPointsSug (); 
		float [] yPoints = db.getYPointsSug (); 
	
		// float a = yPoints[1]+1.0f;
		// float b = xPoints[2];
		// float c = xPoints[2];
		// float d = yPoints[2]+1.0f;
	
		// float [] xPoints2 = {0, 0, xPoints[2], xPoints[3]}; 
		// float [] yPoints2 = {a,b,c,d};  

		//rt.reset();
		// String myString = String.format("get_y=%s,%s,%s,%s", yPoints[0],yPoints[1],yPoints[2],yPoints[3]);
		// IJ.log(myString);
		
		float [] copy_of_xPoints = (float[])xPoints.clone();
		float [] copy_of_yPoints = (float[])yPoints.clone();
		
		Roi sugar = new PolygonRoi(copy_of_xPoints, copy_of_yPoints, nPoints, type);	
		
				// for ( int i = 0; i<xPoints.length; i++){
			// rt.incrementCounter();
			
			// rt.addValue("X", xPoints[i]);
			// rt.addValue("Y", yPoints[i]);	
		// }
		
				// rt.reset();
		// rt.incrementCounter();
		// rt.addValue("a", yPoints[0]);
		// rt.addValue("b", yPoints[1]);
		// rt.addValue("c", yPoints[2]);
		// rt.addValue("d", yPoints[3]);		
		
		

		
		
//		rt.show ("Results");
		
		
		return sugar;
				
	}
	
	public void movement (ImageProcessor ip) {
	
		ResultsTable rt = Analyzer.getResultsTable();
	
	//	for (int i = 1; i <= imp.getStackSize()-1; i++) {
			imp = Border_Substack.getImp();
			int i = imp.getCurrentSlice();
		
			ImagePlus imp1;
			ImageStack is1 = new ImageStack();
			// SubstackMaker sm1 = new SubstackMaker();
			// String start = String.valueOf(i); // starting frame is current slice
			// String ende = String.valueOf(i); // ending frame is last slice
			// StringBuilder text1 = new StringBuilder(start);
			// text1.append("-");
			// text1.append(ende);
			// String stack = text1.toString();
			// imp1 = sm1.makeSubstack(imp, stack); // substack is created			
			is1 = imp.getStack(); 
			ImageProcessor ip1 = is1.getProcessor(i); // specify number of slice 
			imp1 = new ImagePlus ("Imp1",ip1);
			
			ImagePlus imp2;
			ImageStack is2 = new ImageStack();
			// SubstackMaker sm2 = new SubstackMaker();
			// start = String.valueOf(i+1); // starting frame is current slice
			// ende = String.valueOf(i+1); // ending frame is last slice
			// StringBuilder text2 = new StringBuilder(start);
			// text2.append("-");
			// text2.append(ende);
			// stack = text2.toString();
			// imp2 = sm2.makeSubstack(imp, stack); // substack is created			
			is2 = imp.getStack(); 
			ImageProcessor ip2 = is2.getProcessor(i+1); // specify number of slice 
			// imp2.setProcessor(ip2);			
			imp2 = new ImagePlus ("Imp2",ip2);
			
			ImageCalculator ic = new ImageCalculator();
		//	ImageCalculator icStay = new ImageCalculator();

			ImagePlus impMo = ic.run("Difference create", imp1, imp2);
			ImageProcessor ipMo = impMo.getProcessor();
			
			// ImagePlus impMoIn = impMo.duplicate();
			// ImageProcessor ipMoIn = impMoIn.getProcessor();
			// ipMoIn.invert();
			
			ipMo.setThreshold(0, 26, 0);
			
			ipMo.erode();
			ipMo.dilate();
			
			rt.reset();	
			
			ParticleAnalyzer pa = new ParticleAnalyzer(0, 1, rt, 12, Double.POSITIVE_INFINITY, 0, 1); 
			

			setRoiWat(impMo, ipMo, rt);
			double movWat = analyzeMoving(rt, pa, impMo, ipMo);
			impMo.killRoi();
		//	ipMo.resetRoi();
			setRoiSug(impMo, ipMo, rt);
			double movSug = analyzeMoving(rt, pa, impMo, ipMo);
			impMo.killRoi();
			// ipMo.resetRoi();


			ImagePlus impSt = ic.run("Max create", imp1, imp2);
			ImageProcessor ipSt = impSt.getProcessor();
			
			int auto = ipSt.getAutoThreshold();
			ipSt.setThreshold(0, auto-40, 0);
			
			ipSt.erode();
			ipSt.dilate();
			
			rt.reset();

			setRoiWat(impSt, ipSt, rt);
			double stayWat = analyzeStaying(rt, pa, impSt, ipSt);
			impSt.killRoi();
			// ipSt.resetRoi();
			setRoiSug(impSt, ipSt, rt);
			double staySug = analyzeStaying(rt, pa, impSt, ipSt);
			impSt.killRoi();
			// ipSt.resetRoi();
			
			ArrayList <Double> numbers = new ArrayList <Double> ();
			
			double ratioSug = calculate(movSug, staySug, numbers);
			double ratioWat = calculate(movWat, stayWat, numbers);
			
			ArrayList <Double> ratio = new ArrayList <Double> ();
			
			ratio.add(ratioSug);
			ratio.add(ratioWat);

			movementRatio.put(i, ratio);
			numbersForFrame.put(i, numbers);
			
			
			// ImagePlus[] im = new ImagePlus[2];
			// im[0] = impMoIn;
			// im[1] = impSt;
			// RGBStackMerge m = new RGBStackMerge();
			// ImagePlus impChannel = m.mergeChannels(im, true);
			// ImageProcessor ipChannel = impChannel.getProcessor();
			
			// slices.add (ipChannel);	
			
	//	}

		
	}
	
	public double calculate (double moving, double staying, ArrayList <Double> numbers) {
	
		double ratio;
		double denominator;
		double numerator = moving/2;
		if (moving == 0.0) {
			ratio = 0.0;
			denominator = staying;
		} else if (staying == 0.0){
			ratio = 1.0;
			denominator = moving/2;
			} else {
			denominator = (moving/2)+staying;
			ratio = (moving/2)/denominator;
			ratio = (double)Math.round(ratio*100)/100;
		}
		
		//ImagePlus[] im = new ImagePlus[2];
		//im[0] = impMoIn;
		//im[1] = impSt;
		//RGBStackMerge m = new RGBStackMerge();
		//ImagePlus impChannel = m.mergeChannels(im, true);
		//ImageProcessor ipChannel = impChannel.getProcessor();
		
		//slices.add (ipChannel);			
	
		numbers.add(moving);
		numbers.add(staying);
		numbers.add(denominator);
		numbers.add(numerator);
		
		return ratio;
	
	}
	
	
	public double analyzeMoving(ResultsTable rt, ParticleAnalyzer pa, ImagePlus impMo, ImageProcessor ipMo) {

		pa.analyze(impMo); // apply particle analysis
			
		double still = 0.0;
		for (int j=0; j< rt.getCounter(); j++){
			String [] splitt = new String [rt.getLastColumn()];

			String row = rt.getRowAsString(j);
			splitt =  row.split(",");
			if (splitt.length == 1){
				splitt = row.split("\t");
			}	
			double a = Double.parseDouble(splitt[1]); // area values are in second column 
			still = still + a;
		}
			
		double width = (double)ipMo.getWidth();
		double height = (double)ipMo.getHeight();
			
		double moving = (width*height)-still;	

		return moving;
	
	}
	
	public double analyzeStaying (ResultsTable rt, ParticleAnalyzer pa, ImagePlus impSt, ImageProcessor ipSt) {

		pa.analyze(impSt); // apply particle analysis

		double staying = 0.0;
		for (int j=0; j< rt.getCounter (); j++){
			String [] splitt = new String [rt.getLastColumn()];

			String row = rt.getRowAsString(j);
			splitt =  row.split(",");
			if (splitt.length == 1){
				splitt = row.split("\t");
			}	
			double a = Double.parseDouble(splitt[1]); // area values are in second column 
			staying = staying + a;
		}
		
		return staying;

	}
	
	
	
	public TreeMap <Integer, ArrayList<Double>> getMovementRatio () {
	
		return movementRatio;
		
	}
	
	public TreeMap <Integer, ArrayList <Double>> getnumbersForFrame () {
	
		return numbersForFrame;
	
	}
	
	public void createChannelVideo () {
	
		ImageStack is = new ImageStack();
		
		for (int i=0; i<slices.size();i++){
			//is.addSlice(slices.get(i));
		}
		
		
		//aw.setup("", imp2); // write the output .avi-file
		//aw.run(ip2);
	
	
	}
	
	public void outputNumbers (ImagePlus imp) throws IOException {
	
		String title = imp.getTitle();
		String path = Batch_Run.myDir2;

		File file = new File(path + title.substring(0, title.length() - 2) + "_denominator.txt");
		java.io.Writer output = new BufferedWriter(new FileWriter(file));
		
		Set set = numbersForFrame.entrySet();
		Iterator i = set.iterator();

		output.write("Frame" + "\t" + "Moving Sugar" + "\t" + "Staying Sugar"  + "\t" + "Denominator Sugar" + "\t" + "Numerator Sugar" + "\t" + "Moving Water" + "\t" + "Staying Water"  + "\t" + "Denominator Water" + "\t" + "Numerator Water" + "\n");

		for (Entry<Integer, ArrayList<Double>> entry : numbersForFrame.entrySet()){ 
			output.write(((Integer)entry.getKey()).intValue() + "\t");
			
				for (int k=0; k<entry.getValue().size(); k++){
					output.write(((Double)entry.getValue().get(k)).doubleValue() + "\t");
				}
			output.write("\n");
		}

		output.close();
	
	}
					
	public void outputRatios (ImagePlus imp) throws IOException {
	
		String title = imp.getTitle();
		String path = Batch_Run.myDir2;


		File file = new File(path + title.substring(0, title.length() - 2) + "_movingRatio.txt");
		java.io.Writer output = new BufferedWriter(new FileWriter(file));
		
		Set set = movementRatio.entrySet();
		Iterator i = set.iterator();
		
		
		output.write("Frame" + "\t" + "Moving Rate Sugar" + "\t" + "Moving Rate Water" + "\n");

		for (Entry<Integer, ArrayList<Double>> entry : movementRatio.entrySet()){ 
			
			output.write((((Integer)entry.getKey()).intValue()) + "\t");
				for (int k=0; k<entry.getValue().size();k++){
					output.write(((Double)entry.getValue().get(k)).doubleValue() + "\t");
				}
			output.write("\n");
		}
			

		output.close();
	
	}
	

}