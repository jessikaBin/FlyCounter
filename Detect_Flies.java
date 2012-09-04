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
	
	private Maximum_Finder_Modified mf = new Maximum_Finder_Modified ();
	protected TreeMap <Double, Double> countedFlies = new TreeMap <Double, Double> ();


	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {

		// get size of image
		byte[] pixels = (byte[])ip.getPixels();
		
		int stackSize = imp.getStackSize() ;	// get number of frames 
		
		mf.setup("", imp);
		
		for (int i = 1; i <= stackSize; i++) {
			
			imp.setSlice(i);
			findFlies(ip, imp);
			filterMaxima (ip);
		
		}
		
		outputCountedFlies();
		
	}

	private void findFlies (ImageProcessor ip, ImagePlus imp) {
	
		double tolerance = 45;
		double threshold = ImageProcessor.NO_THRESHOLD;
		int outputType = 4; // output is a list of all maxima
		boolean excludeOnEdges = true;
		boolean isEDM = false;
		
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
	
		mf.findMaxima(ip, tolerance, threshold, outputType, excludeOnEdges, isEDM);
		//mf.run(ip);

	}
	
	private void filterMaxima (ImageProcessor ip) {
	
		int width = ip.getWidth();
		int height = ip.getHeight();
	
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
		
		double flies = 0;
		for (int j = 0; j<xcoo.size();j++){
		
			double x_max = xcoo.get(j);
			double y_max = ycoo.get(j);
			
			if (ip.getPixelValue((int)x_max, (int)y_max) < 100) {
			
				double counter = 0;
				for (int hor = -1; hor<=1; hor++){
					for (int ver =-1; ver <=1; ver++){
			
						if ((Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor-1, (int)y_max+ver))<70) &&
							(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor-1, (int)y_max+ver-1))<70) &&
							(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor, (int)y_max+ver-1))<70) &&
							(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor+1, (int)y_max+ver-1))<70) &&
							(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor+1, (int)y_max+ver))<70) &&
							(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor+1, (int)y_max+ver+1))<70) &&
							(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor, (int)y_max+ver+1))<70) &&
							(Math.abs(ip.getPixelValue((int)x_max+hor, (int)y_max+ver)-ip.getPixelValue((int)x_max+hor-1, (int)y_max+ver+1))<70)) {
			
								counter++;
						}
					}
				}
				if (counter > 5) {
					ip.drawDot ((int)x_max, (int)y_max);
					flies++;
				
				}
			}
		
		}
		
		double frame = (double)imp.getCurrentSlice();
		countedFlies.put(frame, flies);
		
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
