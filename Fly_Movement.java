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

import java.util.TreeMap;
import java.util.*;

public class Fly_Movement implements PlugInFilter {

	ImagePlus imp;
	
	public TreeMap <Integer, Double> movementRatio = new TreeMap <Integer, Double> ();

	
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_ALL;
	}

	public void run(ImageProcessor ip) {
		
		movement (ip);
	}
	
	public void movement (ImageProcessor ip) {
	
		ResultsTable rt = Analyzer.getResultsTable();
	
		for (int i = 1; i <= imp.getStackSize()-1; i++) {
		
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
			if (moving == 0.0) {
				ratio = 0.0;
			} else if (staying == 0.0){
				ratio = 1.0;
				} else {
				ratio = (moving/2)/((moving/2)+staying);
			}
			
			movementRatio.put(i, ratio);
		
		}
		
		rt.reset();
		Set set = movementRatio.entrySet();
		Iterator i = set.iterator();
 
        while (i.hasNext()) {
			rt.incrementCounter() ;
			Map.Entry me = (Map.Entry) i.next();
            rt.addValue("Frames",((Integer)me.getKey()).intValue()); 
            rt.addValue("Moving Rate",((Double)me.getValue()).doubleValue());    
        } 
		
		rt.show("Results");
	
	}
	

}