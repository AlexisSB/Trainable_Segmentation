package trainableSegmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.OvalRoi;
import ij.plugin.frame.RoiManager;

import ijopencv.ij.ImagePlusMatConverter;
import ijopencv.ij.ImagePlusMatVectorConverter;
import ijopencv.ij.ListOvalRoiMatConverter;

import java.util.ArrayList;

import org.bytedeco.javacpp.opencv_core;
import org.bytedeco.javacpp.opencv_imgproc;
import org.bytedeco.javacpp.opencv_core.MatVector;
import org.bytedeco.javacpp.opencv_features2d;
import org.bytedeco.javacpp.opencv_xfeatures2d;

public class OpenCVHandler {

    opencv_core.MatVector input;
    opencv_core.Mat output;
    opencv_core.Mat DisplayImage;
    static ImagePlusMatConverter converter = new ImagePlusMatConverter();

    //Constructor Wants to take the training labelled data and the set of orginal images.
    public OpenCVHandler(ImagePlus input){
        ImagePlusMatVectorConverter converterVector = new ImagePlusMatVectorConverter();
        this.input = converterVector.convert(input,MatVector.class);
    }

    public ImagePlus siftTest(ImagePlus inputIP, int sliceNumber){
        opencv_core.Mat temp = converter.convert(inputIP,opencv_core.Mat.class);
        opencv_imgproc.cvtColor(temp,temp,opencv_imgproc.COLOR_BGR2GRAY);
        opencv_core.KeyPointVector kp1 = null;
        opencv_features2d.Feature2D f2d = opencv_xfeatures2d.SIFT.create();

        f2d.detect(temp,kp1);
        opencv_core.Mat descriptor = null;
        f2d.compute(temp,kp1,descriptor);
        opencv_core.Scalar scalar = opencv_core.Scalar.all(-1);
        //opencv_features2d.drawKeyPoints(temp,kp1,temp);

        return converter.convert(temp, ImagePlus.class);

    }

    public static ImagePlus detectCircles(ImagePlus imp){
        IJ.log("Starting detect Circles");

        // Converters
        ImagePlusMatConverter ic = new ImagePlusMatConverter();
        opencv_core.Mat m = ic.convert(imp,opencv_core.Mat.class);
        ListOvalRoiMatConverter cc = new ListOvalRoiMatConverter();

        opencv_core.Mat gray = new opencv_core.Mat();
        opencv_imgproc.cvtColor(m,gray,opencv_imgproc.COLOR_BGR2GRAY);

        opencv_core.Mat circles = new opencv_core.Mat();
        opencv_imgproc.HoughCircles(gray,circles,opencv_imgproc.CV_HOUGH_GRADIENT,1.2,100);

        ArrayList<OvalRoi> or = new ArrayList<OvalRoi>();
        or= cc.convert(circles,or.getClass());
        IJ.log("Circles Detected: " + or.size());
        RoiManager rm = new RoiManager();

        for(int i=0;i<or.size();i++){
            rm.add(imp, or.get(i), i);
        }

        return converter.convert(circles,ImagePlus.class);

    }


}
