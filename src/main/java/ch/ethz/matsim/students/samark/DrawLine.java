package ch.ethz.matsim.students.samark;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.swing.*;
 
public class DrawLine extends JPanel {
 
	private static final long serialVersionUID = 1L;
	@SuppressWarnings("unchecked")
	@Override
	public void paintComponent(Graphics g) {
	    super.paintComponent(g);

	    BufferedImage bgImage = null;
	    try {
	    	bgImage = ImageIO.read(new File("SIM16_50GEN_2SurvivingNetworksConverged.png"));
	    } catch (IOException e) {e.printStackTrace();}
	    g.drawImage(bgImage, 0, 0, this.getWidth(), this.getHeight(), null);
	    
	    // make here reference coordinate system calculations so that new route lines are positioned correctly wrt backgroundImage
	    
	    
	    // load here (a list=sequence) of gene manipulations
		ArrayList<Map<String, String>> pedigreeTree = new ArrayList<Map<String, String>>();
		try {
			pedigreeTree.addAll(XMLOps.readFromFile(pedigreeTree.getClass(), "zurich_1pm/Evolution/Population/HistoryLog/pedigreeTree.xml"));
		} catch (FileNotFoundException e) {e.printStackTrace();}

		// define which bloodline shall be retrieved (from which network).
		String finalNetworkToGenerateBloodLineFrom = "Network1";
		
//	    try {
//    	    BufferedImage bi = createImage(panel);
//    	    File outputfile = new File("SIM16_50GEN_2OUT.png");
//    	    ImageIO.write(bi, "png", outputfile);
//	    } catch (IOException e) {e.printStackTrace();}
		
		//vertical line
	     g.setColor(Color.red);
	     g.drawLine(20, 20, 400, 400);
	     //horizontal line
	     g.setColor(Color.green);
	     g.drawLine(20, 20, 120, 20);
	     //diagonal line 
	     g.setColor(Color.blue);
	     g.drawLine(20, 20, 120, 120);
	  
	  }
 
	  public static void main(String[] args) {
//	    JFrame.setDefaultLookAndFeelDecorated(true);
//	    JFrame frame = new JFrame("Draw Line");
//	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//	    frame.setBackground(Color.white);
//	    frame.setSize(1458, 854);
//	    DrawLine panel = new DrawLine();
//	    frame.add(panel);
//	    frame.setVisible(true);

		    DrawLine panel = new DrawLine();
	    try {
    	    BufferedImage bi = createImage(panel);
    	    File outputfile = new File("SIM16_50GEN_2OUT.png");
    	    ImageIO.write(bi, "png", outputfile);
	    } catch (IOException e) {e.printStackTrace();}
	  }
	  
	  public static BufferedImage createImage(JPanel panel) {
		    int w = panel.getWidth();
		    int h = panel.getHeight();
		    BufferedImage bi = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		    Graphics2D g = bi.createGraphics();
		    panel.paint(g);
		    g.dispose();
		    return bi;
		}
}