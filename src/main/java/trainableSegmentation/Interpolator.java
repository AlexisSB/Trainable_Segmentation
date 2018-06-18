package trainableSegmentation;


import fiji.util.gui.GenericDialogPlus;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.plugin.FolderOpener;
import ij.IJ;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Interpolator {

    String[] filterLabels = new String[] { "RGB Filter", "LookBack Filter", "Neighbour Filter"};
    ImagePlus originalImages = null;
    FirstColourFilter rgb = null;

    private boolean[] enableFeatures = new boolean[]{
            true, 	/* RGB Filter */
            false, 	/* LookBack Filter */
            false, 	/* Neighbour Filter */
    };
    /** Directory for the raw images of hand labelled segmentation*/
    String trainingDataDirectory = null;
    /** Directory for the already processed training data*/
    String processedDataDirectory = null;
    /** Location of C++ exceutable */
    String runExeLocation = "/Users/alexis/Anatomy_Project/FasciaSegmentation/LookBackwardsClassifier/cmake-build-debug/LookBackwardsClassifier";

    ArrayList<String> groundTruthImageLocations = new ArrayList<String>();

    public Interpolator(ImagePlus originalImages){
        this.originalImages = originalImages;

        //Open Dialog Box
        //Set Options

    }

    public void addSliceToInterpolator(ImagePlus slice,int sliceIndex){
        //Generate image name
        String fileName = trainingDataDirectory + "/output/Label" + sliceIndex + ".bmp";
        //Save
        FileSaver fileSaver = new FileSaver(slice);
        if(fileSaver.saveAsBmp(fileName)) {
            IJ.log("Save added successfully");
        }
        //Add to list of arguments
        groundTruthImageLocations.add(fileName);
    }

    public void openInterpolatorSettings(){
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

    public void runInterpolator(){
        try{
            ProcessBuilder builder = new ProcessBuilder();
            List<String> commands = new ArrayList<String>();
            commands.add(runExeLocation);
            commands.add("/Users/alexis/Anatomy_Project/Working_Images/300DPI_renamed/");
            commands.add(trainingDataDirectory + "/output/");
            commands.add(groundTruthImageLocations.size() + "");
            //commands.addAll(groundTruthImageLocations);
            builder.command(commands);
            Process process = builder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                IJ.log(line);
            }
            inputStream.close();
            bufferedReader.close();
        }catch(Exception ioe) {
            ioe.printStackTrace();
        }
       //runRGBInterpolator();

    }

    public void runRGBInterpolator(){
        FolderOpener fp = new FolderOpener();
        //Check if data already created
        ImagePlus trainingData =  fp.openFolder(trainingDataDirectory);
        //Create RGB Filter
        IJ.log("Calling RGB Filter: ");
        if(originalImages == null){
            IJ.log("original images null");
        }
        if (trainingData == null){
            IJ.log("training images null");
        }
        rgb = new FirstColourFilter(originalImages,trainingData);
        //Pass in the original ImagePlus object.
        IJ.log("Finished making RGB Filter");
        rgb.run();
        //Generate the Probability Matrix.

    }

}
