package trainableSegmentation;

import fiji.Debug;
import ij.IJ;

/**
 * Simple test to launch the plugin
 * 
 * @author Ignacio Arganda-Carreras
 *
 */
public class TestGUI {
	
	/**
	 * Main method to test and debug the Trainable Weka
	 * Segmentation GUI
	 *  
	 * @param args
	 */
	public static void main( final String[] args )
	{		
		// Call the plugin with empty arguments (this
		// will pop up an Open dialog)		
		Debug.runPlugIn("Fascia Segmentation", "",true);
		}

		}
