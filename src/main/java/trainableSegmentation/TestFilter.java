package trainableSegmentation;

import ij.IJ;
import ij.ImagePlus;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestFilter {

String imageDirectory;

    public TestFilter(String imageDirectory){
        this.imageDirectory = imageDirectory;
    }

    public void convertToGreyScale(){
        try{
        //Pass it the image directory and the program name.
        ProcessBuilder builder = new ProcessBuilder("/Users/alexis/Anatomy_Project/LookBackwardsClassifier/cmake-build-debug/LookBackwardsClassifier", "/Users/alexis/Anatomy_Project/Working_Images/300DPI_renamed/", "/Users/alexis/Anatomy_Project/Labelled_Slices/");
        Process process = builder.start();
        InputStream inputStream = process.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            IJ.log(line);
        }
        inputStream.close();
        bufferedReader.close();
    } catch (Exception ioe) {
        //ioe.printStackTrace();
    }

    }

}
