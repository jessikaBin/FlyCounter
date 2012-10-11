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

	ImagePlus imp;
	
	private AVI_Writer aw = new AVI_Writer();

	
	public TreeMap <Integer, Double> movementRatio = new TreeMap <Integer, Double> ();
	public TreeMap <Integer, ArrayList <Double>> numbersforFrame = new TreeMap <Integer, ArrayList <Double>> ();
	
	//public ArrayList <ImageProcessor> slices = new ArrayList <ImageProcessor> ();
	
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		
		movement (ip);
				
		try {
			outputRatios(imp);
			outputNumbers(imp);
		} catch (IOException e) {
			System.err.println("Problem with writing the file");
		}
		
	}
	
	public void movement (ImageProcessor ip) {
	
		ResultsTable rt = Analyzer.getResultsTable();
	
	//	for (int i = 1; i <= imp.getStackSize()-1; i++) {
		
			ImagePlus imp1;
			ImageStack is1 = new ImageStack();
			SubstackMaker sm1 = new SubstackMaker();
			String start = String.valueOf(i); // starting frame is current slice
			String ende = String.valueOf(i); // ending frame is last slice
			StringBuilder text1 = new StringBuilder(start);
			text1.append("-");
			text1.append(ende);
			String stack = text1.toString();
			imp1 = sm1.makeSubstack(imp, stack); // substack is created			
			is1 = imp.getStack(); 
			ImageProcessor ip1 = is1.getProcessor(i); // specify number of slice 
			imp1.setProcessor(ip1);
			
			ImagePlus imp2;
			ImageStack is2 = new ImageStack();
			SubstackMaker sm2 = new SubstackMaker();
			start = String.valueOf(i+1); // starting frame is current slice
			ende = String.valueOf(i+1); // ending frame is last slice
			StringBuilder text2 = new StringBuilder(start);
			text2.append("-");
			text2.append(ende);
			stack = text2.toString();
			imp2 = sm2.makeSubstack(imp, stack); // substack is created			
			is2 = imp.getStack(); 
			ImageProcessor ip2 = is2.getProcessor(i+1); // specify number of slice 
			imp2.setProcessor(ip2);			
			
			ImageCalculator icMove = new ImageCalculator();
			ImageCalculator icStay = new ImageCalculator();

			ImagePlus impMo = icMove.run("Difference create", imp1, imp2);
			ImageProcessor ipMo = impMo.getProcessor();
			
		//	ImagePlus impMoIn = impMo;
		//	ImageProcessor ipMoIn = impMoIn.getProcessor();
		//	ipMoIn.invert();
			
			ipMo.setThreshold(0, 26, 0);
			
			ipMo.erode();
			ipMo.dilate();
			
			rt.reset();			
			ParticleAnalyzer pa = new ParticleAnalyzer(0, 1, rt, 12, Double.POSITIVE_INFINITY, 0, 1); 
			pa.analyze(impMo); // apply particle analysis
			
			double still = 0.0;
			for (int j=0; j< rt.getCounter(); j++){
				String [] splitt = new String [2];

				String row = rt.getRowAsString(j);
				splitt =  row.split(",");
				if (splitt.length == 1){
					splitt = row.split("\t");
				}	
				double a = Double.parseDouble(splitt[1]); // circularity values are in second column 
				still = still + a;
			}
			
			double width = (double)ipMo.getWidth();
			double height = (double)ipMo.getHeight();
			
			double moving = (width*height)-still;
			
			ImagePlus impSt = icStay.run("Max create", imp1, imp2);
			ImageProcessor ipSt = impSt.getProcessor();
			
			int auto = ipSt.getAutoThreshold();
			ipSt.setThreshold(0, auto-40, 0);
			
			ipSt.erode();
			ipSt.dilate();
			
			rt.reset();
			pa.analyze(impSt); // apply particle analysis
			
			double staying = 0.0;
			for (int j=0; j< rt.getCounter (); j++){
				String [] splitt = new String [2];

				String row = rt.getRowAsString(j);
				splitt =  row.split(",");
				if (splitt.length == 1){
					splitt = row.split("\t");
				}	
				double a = Double.parseDouble(splitt[1]); // circularity values are in second column 
				staying = staying + a;
			}
			
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
			
			ArrayList <Double> numbers = new ArrayList <Double> ();
			
			numbers.add(moving);
			numbers.add(staying);
			numbers.add(denominator);
			numbers.add(numerator);
			
			movementRatio.put(i, ratio);
			numbersforFrame.put(i, numbers);
		
	//	}

		
	}
	
	// public void createChannelVideo () {
	
		// ImageStack is = new ImageStack();
		
		// for (int i=0; i<slices.size();i++){
			// is.addSlice(slices.get(i));
		// }
		
		
				// //		aw.setup("", imp2); // write the output .avi-file
		// //		aw.run(ip2);
	
	
	// }
	
	public void outputNumbers (ImagePlus imp) throws IOException {
	
		String title = imp.getTitle();

		File file = new File("C:\\Users\\binderj\\Desktop\\" + title.substring(0, title.length() - 2) + "_numbers.txt");
		java.io.Writer output = new BufferedWriter(new FileWriter(file));
		
		Set set = numbersforFrame.entrySet();
		Iterator i = set.iterator();

		output.write("Frame" + "\t" + "Moving" + "\t" + "Staying"  + "\t" + "Denominator" + "\t" + "Numerator" + "\n");

		for (Entry<Integer, ArrayList<Double>> entry : numbersforFrame.entrySet()){ 
			output.write(((Integer)entry.getKey()).intValue() + "\t" + ((Double)entry.getValue().get(0)).doubleValue() + "\t" + 
			((Double)entry.getValue().get(1)).doubleValue()+ "\t" + ((Double)entry.getValue().get(2)).doubleValue() + "\t" + ((Double)entry.getValue().get(3)).doubleValue() + "\n");
		}

		output.close();
	
	}
	
	public void outputRatios (ImagePlus imp) throws IOException {
	
		String title = imp.getTitle();

		File file = new File("C:\\Users\\binderj\\Desktop\\" + title.substring(0, title.length() - 2) + "_ratio.txt");
		java.io.Writer output = new BufferedWriter(new FileWriter(file));
		
		Set set = movementRatio.entrySet();
		Iterator i = set.iterator();

		output.write("Frame" + "\t" + "Moving Rate" + "\n");

		while (i.hasNext()) {
			Map.Entry me = (Map.Entry) i.next();
			output.write(((Integer)me.getKey()).intValue() + "\t" + ((Double)me.getValue()).doubleValue() + "\n");
		}

		output.close();
	
	}
	

}