package trainableSegmentation;

import ij.ImagePlus;
import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import ijopencv.ij.ImagePlusMatConverter;
import ij.IJ;

public class FirstColourFilter {

    ImagePlus originalImages = null;
    ImagePlus trainingData = null;
    Mat[] reducedOriginalImages = null;
    HashMap<Color,int[]> colourMapCount = null;
    HashMap<Color,int[]> probabilityOutput = null;
    final ImagePlusMatConverter converter = new ImagePlusMatConverter();
    int numLabels = 9;

    public FirstColourFilter(ImagePlus originalImages, ImagePlus trainingData) {

        IJ.log("Starting to make FirstColourFilter");
        this.originalImages = originalImages;
        IJ.log("1");
        this.trainingData = trainingData;
        IJ.log("2");
        colourMapCount = new HashMap<Color, int[]>(255);
        probabilityOutput = new HashMap<Color, int[]>(255);
        IJ.log("3");

        reducedOriginalImages = new  Mat[originalImages.getNSlices()];

        IJ.log("Created FirstColourFilter");

    }

    public void run(){
        reduceColorSpace();
        countRGBData();


    }

    public void reduceColorSpace(){
        IJ.log("Starting to reduce colour space");
        //Construct lookuptable
        int colourSpaceReductionFactor = 10;
        byte[] table = new byte[256];
        for ( int i = 0 ; i < 256; i++){
            table[i] = (byte) (i/colourSpaceReductionFactor);
        }

       IJ.log(Arrays.toString(table));

         Mat lut = new Mat(1,256,CvType.CV_8U);
         lut.put(0,0,table);

         for(int slice = 0 ; slice < originalImages.getNSlices();slice++) {
             reducedOriginalImages[slice] = new Mat(trainingData.getHeight(), trainingData.getWidth(), CvType.CV_8UC3);
             //Convert original image to Mat
             originalImages.setSlice(slice);
             Mat originalImageMat = converter.convert(originalImages.getSlice(), Mat.class);
             Core.LUT(originalImageMat, lut, reducedOriginalImages[slice]);
         }
        IJ.log("Finished reducedColourSpace");
    }

    public void countRGBData() {

        IJ.log("Counting RGB");
        //For each slice
        for (int slice = 0; slice < originalImages.getNSlices(); slice++) {
            IJ.log("Processing slice : " + slice);
            //Get the Mat data as a byte array
            trainingData.setSlice(slice);
            int totalbytes = (int) (reducedOriginalImages[slice].total() * reducedOriginalImages[slice].elemSize());
            byte[] buffer = new byte[totalbytes];
            reducedOriginalImages[slice].get(0, 0, buffer);
            int rows = reducedOriginalImages[slice].rows();
            int cols = reducedOriginalImages[slice].cols();
            int row = 0;
            int col = 0;
            for (int i = 0; i < totalbytes; i += 3) {
                Color color = new Color(buffer[i + 2], buffer[i + 1], buffer[i]);
                int index = trainingData.getPixel(col, row)[3];
                col++;
                if (col >= cols) {
                    col = 0;
                    row++;
                }
                if (colourMapCount.containsKey(color)) {
                    colourMapCount.get(color)[index]++;
                    colourMapCount.get(color)[numLabels]++;
                } else {
                    colourMapCount.put(color, new int[numLabels + 1]);
                    colourMapCount.get(color)[index]++;
                    colourMapCount.get(color)[numLabels]++;
                }


            }
            //Index accessed through
            //ip.getPixel(x,y)[3]
            //Parse through it extracting the colors in groups of three ints b/g/r
            //Pick up the label index from the training label
            //Add to hash table.
        }
        IJ.log("RGB Processing finsihed");
        StringBuilder testoutput = new StringBuilder();

        for (Color color: colourMapCount.keySet()){
            String key = color.toString();
            String value = Arrays.toString(colourMapCount.get(color));
            testoutput.append(key + " " + value + " \n");
        }
        IJ.log( testoutput.toString());
    }





}
