package trainableSegmentation;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.IJ;

import java.awt.*;

public class RoiScanner {

    ImagePlus displayImage;

    public RoiScanner(ImagePlus displayImage){
        this.displayImage = displayImage;

    }

    void scanAndExport() {

        int X = 0;
        int Y = 0;
        int maxX = displayImage.getWidth();
        int maxY = displayImage.getHeight();
        int[] rgb = new int[3];
        ImageProcessor impr = displayImage.getProcessor();

        impr.getPixel(X, Y, rgb);
        IJ.doWand(X,Y);
        IJ.saveAs("XY Coordinates", "/Users/alexis/Anatomy_Project/XCode Projects/CoordinateExtraction/test.txt");

        //every ten pixels
        //check color
        //If color different to previous color
        //do a wand select on the area
        //save coordinates to file.
        //Let the color determine the file name and then count the number.
        //check the starting coordinates of the selection

    }

}
