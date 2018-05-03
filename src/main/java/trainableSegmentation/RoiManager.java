package trainableSegmentation;

import fiji.util.gui.OverlayedImageCanvas;
import ij.IJ;
import ij.gui.PointRoi;
import ij.gui.Roi;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.awt.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Scanner;

public class RoiManager {

    /**
     * 50% alpha composite
     */
    private final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f);

    int numOfClasses = 9;

    ArrayList<Roi>[] LabelTraces = new ArrayList[numOfClasses];

    public RoiManager(){

        // assign colors to classes
        Color[] colors = new Color[numOfClasses];

        colors[0] = new Color(179, 204, 204);//Grey
        colors[1] = new Color(64, 224, 208); //Teal
        colors[2] = new Color(247, 219, 7); //Yellow
        colors[3] = new Color(214, 147, 31);   //Orange
        colors[4] = new Color(244, 4, 48);//Red
        colors[5] = new Color(142, 245, 250);//Light Blue
        colors[6] = new Color(255, 127, 127);//Light Red
        colors[7] = new Color(121, 235, 148);//Light Green
        colors[8] = new Color(0, 0, 255);//Blue

        // add roi list overlays (one per class)
        for (int i = 0; i < WekaSegmentation.MAX_NUM_CLASSES; i++) {
            roiOverlay[i] = new RoiListOverlay();
            roiOverlay[i].setComposite(transparency050);
            ((OverlayedImageCanvas) ic).addOverlay(roiOverlay[i]);
        }
    }

    protected void addRoi(int classIndex, Roi selection){
        LabelTraces[classIndex].add(selection);
    }

    protected ArrayList<Roi> getRoiList(int classIndex){
        ArrayList<Roi> temp = new ArrayList<Roi>();
        Collections.copy(LabelTraces[classIndex],temp);
        return temp;
    }


    protected void drawExamples()
        {
            final int currentSlice = displayImage.getCurrentSlice();
            for(int i = 0 ; i <numOfClasses;i++){
                ArrayList<Roi> classLabels = getRoiList(i);
                roiOverlay[i].setColor(colors[i]);
            final ArrayList< Roi > rois = new ArrayList<Roi>();
            for (Roi r : wekaSegmentation.getExamples(i, currentSlice))
            {
                rois.add(r);
                //IJ.log("painted ROI: " + r + " in color "+ colors[i] + ", slice = " + currentSlice);
            }
            roiOverlay[i].setRoi(rois);
        }

        displayImage.updateAndDraw();
    }

    *//**
     * Update the example lists in the GUI
     *//*
    protected void updateExampleLists()
    {
        final int currentSlice = displayImage.getCurrentSlice();

        for(int i = 0; i < wekaSegmentation.getNumOfClasses(); i++)
        {
            exampleList[i].removeAll();
            for(int j=0; j<wekaSegmentation.getExamples(i, currentSlice).size(); j++)
                exampleList[i].add("trace " + j + " (Z=" + currentSlice+")");
        }

    }

    //@author Alexis Barltrop
    private Roi convertCoordinateToROI(File coordinateFile) throws IOException {
        //could use wc line count to create arrays.
        Scanner scan = new Scanner(coordinateFile);
        ArrayList<Integer> rows = new ArrayList<Integer>();
        ArrayList<Integer> cols = new ArrayList<>();

        while(scan.hasNextInt()) {
            rows.add(scan.nextInt());
            cols.add(scan.nextInt());
        }
        //Convert to array of ints

        int[] rowsArray = new int[rows.size()];
        int[] colsArray = new int[cols.size()];
        IJ.log("Row length: " + rowsArray.length);

        for(int i = 0; i<rowsArray.length; i++){
            rowsArray[i] = rows.get(i).intValue();
            colsArray[i] = cols.get(i).intValue();
        }

        Roi result = new PointRoi(rowsArray, colsArray,rowsArray.length);
        IJ.log(result.toString());
        //Roi result = new PolygonRoi(rowsArray, colsArray,rowsArray.length,Roi.POLYLINE);


        return result;
    }


}
