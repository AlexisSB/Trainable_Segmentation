package trainableSegmentation;

import fiji.util.gui.GenericDialogPlus;
import ij.IJ;
import ij.ImagePlus;
import ij.io.OpenDialog;
import ij.plugin.ImageCalculator;
import ij.process.ColorBlitter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SkinSelector {


    String fileDirectory;
    String outputDirectory;
    int reductionFactor = 8;
    String runExeLocation = "/Users/alexis/Anatomy_Project/GrabCut/cmake-build-debug/GrabCut";
    String runXORLocation = "/Users/alexis/Anatomy_Project/SkinMask/cmake-build-debug/SkinMask";

    public SkinSelector(){

    }

    public void run(boolean inside){
        try{
            ProcessBuilder builder = new ProcessBuilder();
            List<String> commands = new ArrayList<String>();
            commands.add(runExeLocation);
            commands.add(fileDirectory);
            commands.add(outputDirectory);
            commands.add(reductionFactor+ "");
            if(inside){
                commands.add("1");
            }else{
                commands.add("0");
            }
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
    }

    public void runXOR(){
        try{
            ProcessBuilder builder = new ProcessBuilder();
            List<String> commands = new ArrayList<String>();
            commands.add(runXORLocation);
            commands.add(outputDirectory + "/insideMask.bmp");
            commands.add(outputDirectory + "/outsideMask.bmp");
            commands.add(outputDirectory);

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
    }

    public void setFileDirectory(String fileDirectory) {
        this.fileDirectory = fileDirectory;
    }

    public void setOutputDirectory(String outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public void openSettings(){
        GenericDialogPlus gd = new GenericDialogPlus("Segmentation settings");

        gd.addMessage("Training Data");

        gd.addDirectoryOrFileField("Current Image Directory",fileDirectory);
        gd.addDirectoryField("Output Directory", outputDirectory);
        gd.addNumericField("Reduction factor",8,1);

        gd.showDialog();
        //Don't save any changes if cancelled
        if (gd.wasCanceled()) return;
        //Check for updates and make changes.
        //Check the check boxes and update the enabled features.

        //Check the dialog boxes
       fileDirectory = gd.getNextString();
       outputDirectory = gd.getNextString();
       reductionFactor = (int) gd.getNextNumber();

    }

    public void labelSkin(){
        ImagePlus inside = IJ.openImage();
        ImagePlus outside = IJ.openImage();
        ImageCalculator ic = new ImageCalculator();
        ImagePlus skinMask = ic.run("XOR create", inside, outside);

        skinMask.getProcessor();



    }


}
