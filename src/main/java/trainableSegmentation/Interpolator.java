package trainableSegmentation;


import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.OptionHandler;
import weka.core.Utils;
import weka.gui.GenericObjectEditor;
import weka.gui.PropertyPanel;

import java.awt.*;
import java.util.Vector;

public class Interpolater {

    String[] filterLabels = new String[] { "RGB Filter", "LookBack Filter", "Neighbour Filter"};

    private boolean[] enableFeatures = new boolean[]{
            true, 	/* RGB Filter */
            false, 	/* LookBack Filter */
            false, 	/* Neighbour Filter */
    };
    /** Directory for the raw images of hand labelled segmentation*/
    String trainingDataDirectory = null;
    /** Directory for the already processed training data*/
    String processedDataDirectory = null;

    public Interpolater(){

        //Open Dialog Box
        //Set Options

    }


    public void openInterpolaterSettings(){
        GenericDialogPlus gd = new GenericDialogPlus("Segmentation settings");

        gd.addMessage("Training features:");
        final int rows = (int) Math.round( filterLabels.length/2.0 );

        gd.addCheckboxGroup( rows, 2,filterLabels,enableFeatures);

        gd.addMessage("Training Data");

        gd.addDirectoryField("Training Data Directory",trainingDataDirectory);
        gd.addDirectoryOrFileField("Processed Data File", processedDataDirectory);

        gd.showDialog();
        //Don't save any changes if cancelled
        if (gd.wasCanceled()) return;
        //Check for updates and make changes.
        //Check the check boxes and update the enabled features.
        for(int i = 0 ;i < filterLabels.length;i++){
            enableFeatures[i] = gd.getNextBoolean();
        }

       //Check the dialog boxes
        trainingDataDirectory = gd.getNextString();
        processedDataDirectory = gd.getNextString();


    }

    public void runInterpolater(){


    }

    public void runRGBIntepolater(){



    }

}
