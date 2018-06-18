package trainableSegmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.StackWindow;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.assertEquals;

public class ScissorTest {

    @Test
    public void testDjikstra(){
        ImagePlus image = IJ.openImage("/Users/alexis/Anatomy_Project/FasciaSegmentation/ImageJIntelligentScissors/StripTest.bmp");

        IntelligentScissors myScissors = new IntelligentScissors();
        myScissors.setImage(image);
        Point start = new Point (0, 60);
        Point one = new Point(10,60);
        Point two = new Point(20,60);
        myScissors.addUserSelectedPoints(start);
        myScissors.addUserSelectedPoints(one);
        myScissors.addUserSelectedPoints(two);
        PolygonRoi line = (PolygonRoi) myScissors.drawShortestPath();

        System.out.println("Roi line : \n" + line);
        StackWindow satWindow = new StackWindow(myScissors.saturation);

        }

    @Test
    public void testInfoMatrixSetup(){
        ImagePlus image = IJ.openImage("/Users/alexis/Anatomy_Project/FasciaSegmentation/ImageJIntelligentScissors/StripTest.bmp");
        IntelligentScissors myScissors = new IntelligentScissors();
        myScissors.setImage(image);

        for(int i = 0; i < image.getHeight(); i++){
            for(int j = 0; j < image.getWidth(); j++){
                assertEquals(i, myScissors.infoMatrix[i][j].getPoint().y);
                assertEquals(j, myScissors.infoMatrix[i][j].getPoint().x);
            }
        }


    }


}
