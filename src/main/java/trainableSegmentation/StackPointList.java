package trainableSegmentation;

import ij.gui.Roi;

import java.awt.*;
import java.util.ArrayList;

public class StackPointList {

    ArrayList<Roi> pointList = new ArrayList<>();
    int stackIndex;
    int label;

    StackPointList(int label, int stackIndex){
        this.label = label;
        this.stackIndex = label;

    }

    void addLine(Point p, int slice){

    }
    void addRoi(Roi roi){
        pointList.add(roi);
    }

    StackPointList copy(int stackIndex){
        StackPointList copy = new StackPointList(this.label, stackIndex);

        for (Roi r:pointList){
            copy.addRoi((Roi)r.clone());
        }
        return copy;
    }

}
