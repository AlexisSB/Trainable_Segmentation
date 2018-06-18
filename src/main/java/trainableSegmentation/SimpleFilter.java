package trainableSegmentation;

import ij.*;
import ij.process.ImageProcessor;
import ijopencv.ij.ImagePlusMatConverter;
import ijopencv.ij.ImagePlusMatVectorConverter;
import org.bytedeco.javacpp.opencv_core.*;
import org.bytedeco.javacpp.opencv_imgproc;

public class SimpleFilter {

    ImagePlus images = null;
    ImagePlusMatVectorConverter converter = new ImagePlusMatVectorConverter();


    public SimpleFilter(ImagePlus images){
        this.images = images;

    }

    public void convertToGreyScale(){
        IJ.log("1");
        MatVector imagesMat = converter.convert(this.images, MatVector.class);

        IJ.log("2");
        for(int slice = 0 ; slice < imagesMat.size(); slice++){
            IJ.log("" +slice);
            opencv_imgproc.cvtColor(imagesMat.get(slice),imagesMat.get(slice), opencv_imgproc.COLOR_BGR2GRAY);
            ImageProcessor imp = images.getProcessor();
            imp = converter.convert(imagesMat.get(slice),ImageProcessor.class);
            images.setProcessor(imp);
        }

    }

    public ImagePlus getImages(){
        return images;
    }


}
