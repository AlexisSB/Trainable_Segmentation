package trainableSegmentation;

import fiji.util.gui.GenericDialogPlus;
import fiji.util.gui.OverlayedImageCanvas;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.*;
import ij.io.SaveDialog;
import ij.plugin.EventListener;
import ij.plugin.PlugIn;
import ij.plugin.frame.Recorder;
import ij.plugin.frame.RoiManager;
import ij.process.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imagej.ops.Ops;
import weka.core.WekaPackageManager;
import weka.gui.GenericObjectEditor;
import weka.core.PluginManager;

/**
 *
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Authors: Ignacio Arganda-Carreras (iargandacarreras@gmail.com), Verena Kaynig,
 *          Albert Cardona
 */



/**
 * Segmentation plugin based on the machine learning library Weka
 */
public class FasciaMain implements PlugIn
{
    /** plugin's name */
    public static final String PLUGIN_NAME = "Trainable Weka Segmentation";
    /** plugin's current version */
    public static final String PLUGIN_VERSION = "v" +
            FasciaMain.class.getPackage().getImplementationVersion();



    /** reference to the segmentation backend */
    private WekaSegmentation wekaSegmentation = null;

    private MyRoiManager myRoiManager = null;

    /** image to display on the GUI */
    private ImagePlus displayImage = null;
    /** image to be used in the training */
    private ImagePlus trainingImage = null;
    /** result image after classification */
    private ImagePlus classifiedImage = null;
    /** GUI window */
    private CustomWindow win = null;
    /** number of classes in the GUI */
    private int numOfClasses = 2;
    /** array of number of traces per class */
    private int[] traceCounter = new int[WekaSegmentation.MAX_NUM_CLASSES];
    /** flag to display the overlay image */
    private boolean showColorOverlay = false;
    /** executor service to launch threads for the plugin methods and events */
    private final ExecutorService exec = Executors.newFixedThreadPool(1);


    /** toggle overlay button */
    private JButton overlayButton = null;

    /** Button to Start ItelligentScissor Protocol*/
    private JButton scissorSelectButton = null;
    private JButton stopScissorButton = null;

    /** Buttons to modify ROI line */
    private JButton startModifyScissorSelection = null;
    private JButton stopModifyScissorSelection = null;

    /** Save the overlays button*/
    private JButton saveOverlayButton = null;

    /** reset the overlay */
    private JButton resetOverlayButton = null;

    /** copy selected points from previous slice */
    private JButton interpolateSelectedPointsButton = null;

    public final JFileChooser fc = new JFileChooser();

    /** array of roi list overlays to paint the transparent rois of each class */
    private RoiListOverlay [] roiOverlay = null;

    /** Overlay for temporary path drawing*/
    private RoiListOverlay temporaryOverlay = null;


    /** Array of image overlays for hand labelled training labels */
    private ImagePlus [] classifiedImages = null;

    /** Array of booleans for ground truth labelling*/
    private boolean[] groundTruthLabels = null;

    /** available for available classes */
    private Color[] colors = new Color[]{Color.red, Color.green, Color.blue,
            Color.cyan, Color.magenta};

    /** Lookup table for the result overlay image */
    private LUT overlayLUT = null;

    /** array of trace lists for every class */
    private java.awt.List[] exampleList = null;
    /** array of buttons for adding each trace class */
    private JButton [] addExampleButton = null;

    private JCheckBox[] showClassCheckBox = null;

    /** boolean flag set to true while training */
    private boolean trainingFlag = false;

    private boolean roiListenerActive = false;

    private final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f);

    private ImageOverlay currentResultOverlay = null;
    private boolean isProcessing3D = false;
    private Interpolator interpolator = null;
    private SkinSelector skinSelector = null;

    private IntelligentScissors scissors = null;

    private ArrayList<ArrayList<StackPointList>> selectedPointStackList = null;


    /** Labels Enum class*/
    private enum Label {
        EXTERIOR(new Color(179,204,204)),
        INSIDE(new Color(64,224,208)),
        BONE(new Color(247,219,7)),
        SKIN(new Color(214, 147, 31)),
        ARTERY(new Color(244, 4, 48)),
        GM_FASCIA(new Color(142, 245, 250)),
        TENSOR_FASCIAE_LATAE(new Color(255, 127, 127)),
        VASTUS_LATERALIS(new Color(121, 235, 148)),
        ILIAC_LIGAMENTS(new Color(0, 0, 255));

        private final Color color;

        Label(Color color){
            this.color = color;
        }

    }


    /**
     * Basic constructor for graphical user interface use
     */
    public FasciaMain()
    {
        // Create overlay LUT
        final byte[] red = new byte[ 256 ];
        final byte[] green = new byte[ 256 ];
        final byte[] blue = new byte[ 256 ];

        // assign colors to classes
        colors = new Color[ MyRoiManager.MAX_NUM_CLASSES ];

        //TODO use enums here.
        colors[0] = new Color(179, 204, 204);//Grey
        colors[1] = new Color(64, 224, 208); //Teal
        colors[2] = new Color(247, 219, 7); //Yellow
        colors[3] = new Color(214, 147, 31);   //Orange
        colors[4] = new Color(244, 4, 48);//Red
        colors[5] = new Color(142, 245, 250);//Light Blue
        colors[6] = new Color(255, 127, 127);//Light Red
        colors[7] = new Color(121, 235, 148);//Light Green
        colors[8] = new Color(0, 0, 255);//Blue


        for(int i = 0; i < MyRoiManager.MAX_NUM_CLASSES; i++)
        {
            //IJ.log("i = " + i + " color index = " + colorIndex);
            red[i] = (byte) Label.values()[ i ].color.getRed();
            green[i] = (byte) Label.values()[ i ].color.getGreen();
            blue[i] = (byte) Label.values()[ i ].color.getBlue();
        }
        overlayLUT = new LUT(red, green, blue);

        exampleList = new java.awt.List[WekaSegmentation.MAX_NUM_CLASSES];
        addExampleButton = new JButton[WekaSegmentation.MAX_NUM_CLASSES];
        showClassCheckBox = new JCheckBox[WekaSegmentation.MAX_NUM_CLASSES];

        roiOverlay = new RoiListOverlay[WekaSegmentation.MAX_NUM_CLASSES];

        overlayButton = new JButton("Toggle overlay");
        overlayButton.setToolTipText("Toggle between current segmentation and original image");
        overlayButton.setEnabled(true);

        scissorSelectButton = new JButton( "Start Scissor Select");
        scissorSelectButton.setToolTipText("Start selection using scissor method");
        scissorSelectButton.setEnabled(true);

        stopScissorButton = new JButton("Stop Scissor Select");
        stopScissorButton.setToolTipText(" Stop selection using scissor method");
        stopScissorButton.setEnabled(false);

        startModifyScissorSelection = new JButton("Modify Scissor Selection");
        startModifyScissorSelection.setToolTipText("Start modifying the scissor selection");
        startModifyScissorSelection.setEnabled(false);

        stopModifyScissorSelection = new JButton("Stop Scissor Modification");
        stopModifyScissorSelection.setToolTipText(" Stops the modification of the scissor selection");
        stopModifyScissorSelection.setEnabled(false);

        saveOverlayButton = new JButton("Save Overlay");
        saveOverlayButton.setToolTipText("Save current Overlay");
        saveOverlayButton.setEnabled(true);

        resetOverlayButton = new JButton("Reset Overlay");
        resetOverlayButton.setToolTipText("Reset the Current Overlay");
        resetOverlayButton.setEnabled(false);

        interpolateSelectedPointsButton = new JButton("Interpolate Selection");
        interpolateSelectedPointsButton.setToolTipText("Copies the selection of points from the previous layer");
        interpolateSelectedPointsButton.setEnabled(true);

        showColorOverlay = false;
    }

    /** Thread that runs the training. We store it to be able to
     * to interrupt it from the GUI */
    private Thread trainingTask = null;


    /**
     * Button listener
     */
    private ActionListener listener = new ActionListener() {

        public void actionPerformed(final ActionEvent e) {

            final String command = e.getActionCommand();

            // listen to the buttons on separate threads not to block
            // the event dispatch thread
            exec.submit(new Runnable() {

                public void run() {
                    if (e.getSource() == overlayButton) {
                        win.toggleOverlay();
                    } else if (e.getSource() == scissorSelectButton) {
                        exec.submit(new Runnable() {
                            @Override
                            public void run() {
                                displayImage.killRoi();
                                scissors.setImage(win.getDisplayImage());
                                temporaryOverlay.setRoi(null);
                                displayImage.updateAndDraw();
                                stopScissorButton.setEnabled(true);
                                scissors.reset();
                                IJ.log("Start Scissors");
                                IJ.setTool("multipoint");
                                scissors.activeFlag = true;
                                updateScissorPath();
                            }
                        });

                    } else if (e.getSource() == stopScissorButton) {
                        scissors.activeFlag = false;
                        stopScissorButton.setEnabled(false);
                        //reset Selected points
                        startModifyScissorSelection.setEnabled(true);
                    } else if (e.getSource() == startModifyScissorSelection) {
                        assert (!scissors.activeFlag);
                        roiListenerActive = true;
                        stopModifyScissorSelection.setEnabled(true);
                        startModifyScissorSelection.setEnabled(false);
                    } else if (e.getSource() == stopModifyScissorSelection) {
                        roiListenerActive = false;
                        stopModifyScissorSelection.setEnabled(false);
                        startModifyScissorSelection.setEnabled(true);

                    } else if (e.getSource() == saveOverlayButton){
                        ImagePlus labelImage = getLabelImage(displayImage.getCurrentSlice());
                        ImageConverter ic = new ImageConverter(labelImage);

                        ic.convertRGBtoIndexedColor(256);
                        labelImage.setLut(overlayLUT);
                        StackWindow labelWindow = new StackWindow(labelImage);
                        labelWindow.pack();
                    } else if (e.getSource() == resetOverlayButton){

                    } else if (e.getSource() == interpolateSelectedPointsButton){

                        //Open dialog box to pick the slice to copy from
                        int sliceIndex = (int) IJ.getNumber("Enter the slice to copy points from : ", displayImage.getCurrentSlice()-1);

                        //go to current slice roi stack
                        //for each one in target index copy to the current slice index.
                        //Clear current stack
                        selectedPointStackList.get(displayImage.getCurrentSlice()).clear();
                        for( int i =0; i < numOfClasses; i++) {
                            StackPointList stackToCopy = selectedPointStackList.get(sliceIndex).get(i).copy(displayImage.getCurrentSlice());
                            selectedPointStackList.get(displayImage.getCurrentSlice()).add(stackToCopy);
                        }



                    } else{
                        for(int i = 0; i < myRoiManager.getNumOfClasses(); i++)
                        {
                            if(e.getSource() == exampleList[i])
                            {
                                deleteSelected(e);
                                break;
                            }
                            if(e.getSource() == addExampleButton[i])
                            {
                                addExamples(i);
                                //roiManagerPanel.addElement("Test");
                                break;
                            }

                        }
                        win.updateButtonsEnabling();
                    }

                }
            });
        }
    };

   /* private void updateScissorPath(){

        IJ.log("Scissor User Selected Point \n " + scissors.getUserSelectedPoints().toString());
        if(scissors.getUserSelectedPoints().size() == 0){
            return;
        }
        //Delete temporary Path
        if(scissors.getUserSelectedPoints().size() == 1) {
            PointRoi chosenPoints = scissors.getUserSelectedPointROI();
            displayImage.setRoi(chosenPoints);
            displayImage.updateAndDraw();

        } else{
            PolygonRoi line = (PolygonRoi) scissors.drawShortestPath();
            PointRoi chosenPoints = (PointRoi) scissors.getUserSelectedPointROI();
            //IJ.log(chosenPoints.toString());


            ArrayList<Roi> roi = new ArrayList<Roi>();
            roi.add(line);
            temporaryOverlay.setColor(new Color(0, 255, 0));
            temporaryOverlay.setRoi(roi);
            //Line deleted when path updated
            displayImage.setRoi(chosenPoints);
            //displayImage.setRoi(chosenPoints);
            //win.getDisplayImage().setRoi(line,true);
            //addExamples(4);

            displayImage.updateAndDraw();
        }
    }*/

    private void updateScissorPath(){

        PointRoi roi = (PointRoi) displayImage.getRoi();
        IJ.log("Scissor User Selected Point \n " + Arrays.toString(roi.getContainedPoints()));
        //Pull points from current roi
        temporaryOverlay.setRoi(null);
        if(roi.getContainedPoints().length < 2){
            IJ.log("Point A");
        }else{
            //Add point to scissors
            IJ.log("Point B");
            PolygonRoi line = (PolygonRoi) scissors.drawShortestPath(roi.getContainedPoints());
            line.setStrokeWidth(5);
            IJ.log("Point C");
            ArrayList<Roi> tempRoi = new ArrayList<Roi>();
            tempRoi.add(line);
            IJ.log("Point D");

            temporaryOverlay.setColor(new Color(0, 255, 0));
            temporaryOverlay.setRoi(tempRoi);
            //Line deleted when path updated
            //displayImage.setRoi(chosenPoints);
            //win.getDisplayImage().setRoi(line,true);
            //addExamples(4);

        }
        displayImage.updateAndDraw();
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
        //result = new ShapeRoi(result);
        //IJ.log(result.toStringPointRoi());
        //Roi result = new PolygonRoi(rowsArray, colsArray,rowsArray.length,Roi.POLYLINE);


        return result;
    }


    /**
     * Item listener for the trace lists
     */
    private ItemListener itemListener = new ItemListener() {
        public void itemStateChanged(final ItemEvent e) {
            exec.submit(new Runnable() {
                public void run() {
                    for(int i = 0; i < myRoiManager.getNumOfClasses(); i++)
                    {
                        if(e.getSource() == exampleList[i])
                            listSelected(e, i);

                        //Listen for check boxes
                        if(e.getSource() == showClassCheckBox[i]){
                            if(e.getStateChange() == ItemEvent.SELECTED) {
                                toggleClassOverlay(i, true);
                            }
                            if(e.getStateChange() == ItemEvent.DESELECTED){
                                toggleClassOverlay(i,false);
                            }
                            break;
                        }
                    }

                }
            });
        }
    };

    /**
     * Custom window to define the Trainable Weka Segmentation GUI
     */
    private class CustomWindow extends StackWindow
    {
        /** default serial version UID */
        private static final long serialVersionUID = 1L;
        /** layout for annotation panel */
        private GridBagLayout boxAnnotation = new GridBagLayout();
        /** constraints for annotation panel */
        private GridBagConstraints annotationsConstraints = new GridBagConstraints();

        /** scroll panel for the label/annotation panel */
        private JScrollPane scrollPanel = null;

        /** panel containing the annotations panel (right side of the GUI) */
        private JPanel labelsJPanel = new JPanel();
        private GridBagLayout boxCheckBoxes = new GridBagLayout();
        private GridBagConstraints checkBoxConstraints = new GridBagConstraints();
        private JPanel checkBoxJPanel = new JPanel();

        /** Panel with class radio buttons and lists */
        private JPanel annotationsPanel = new JPanel();

        /** buttons panel (left side of the GUI) */
        private JPanel buttonsPanel = new JPanel();
        /** training panel (included in the left side of the GUI) */
        private JPanel trainingJPanel = new JPanel();
        /** options panel (included in the left side of the GUI) */
        private JPanel optionsJPanel = new JPanel();
        /** roi Manager Panel (included on the left side of the GUI */
        private final RoiManagerPanel roiManagerPanel = new RoiManagerPanel();
        /** main GUI panel (containing the buttons panel on the left,
         *  the image in the center and the annotations panel on the right */
        private Panel all = new Panel();

        /** 50% alpha composite */
        private final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f );
        /** 25% alpha composite */
        //final Composite transparency025 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f );
        /** opacity (in %) of the result overlay image */
        private int overlayOpacity = 33;
        /** alpha composite for the result overlay image */
        private Composite overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayOpacity / 100f);
        /** current segmentation result overlay */
        private ImageOverlay resultOverlay;

        /** boolean flag set to true when training is complete */
        private boolean trainingComplete = false;

        /**
         * Construct the plugin window
         *
         * @param imp input image
         */
        CustomWindow(ImagePlus imp)
        {
            super(imp, new CustomCanvas(imp));

            //ic is the image canvas, this gets set to the custom canvas when the constructor makes the object.
            final CustomCanvas canvas = (CustomCanvas) getCanvas();

            // add roi list overlays (one per class)
            for(int i = 0; i < MyRoiManager.MAX_NUM_CLASSES; i++)
            {
                roiOverlay[i] = new RoiListOverlay();
                roiOverlay[i].setComposite( transparency050 );
                ((OverlayedImageCanvas)ic).addOverlay(roiOverlay[i]);
            }

            //Add Overlay for temporary line drawing
            temporaryOverlay = new RoiListOverlay();
            temporaryOverlay.setComposite(transparency050);
            ((OverlayedImageCanvas)ic).addOverlay(temporaryOverlay);

            // add result overlay
            resultOverlay = new ImageOverlay();
            resultOverlay.setComposite( overlayAlpha );
            ((OverlayedImageCanvas)ic).addOverlay(resultOverlay);

            // Remove the canvas from the window, to add it later
            removeAll();

            setTitle( FasciaMain.PLUGIN_NAME + " " + FasciaMain.PLUGIN_VERSION );

            // Annotations panel
            annotationsConstraints.anchor = GridBagConstraints.NORTHWEST;
            annotationsConstraints.fill = GridBagConstraints.HORIZONTAL;
            annotationsConstraints.gridwidth = 1;
            annotationsConstraints.gridheight = 1;
            annotationsConstraints.gridx = 0;
            annotationsConstraints.gridy = 0;

            annotationsPanel.setBorder(BorderFactory.createTitledBorder("Labels"));
            annotationsPanel.setLayout(boxAnnotation);

            for(int i = 0; i < myRoiManager.getNumOfClasses(); i++)
            {
                exampleList[i].addActionListener(listener);
                exampleList[i].addItemListener(itemListener);
                addExampleButton[i] = new JButton("Add to " + myRoiManager.getClassLabel(i));
                addExampleButton[i].setToolTipText("Add markings of label '" + myRoiManager.getClassLabel(i) + "'");

                annotationsConstraints.insets = new Insets(5, 5, 6, 6);

                annotationsPanel.add( addExampleButton[i], annotationsConstraints );
                annotationsConstraints.gridy++;

                annotationsConstraints.insets = new Insets(0,0,0,0);

                //annotationsPanel.add( exampleList[i], annotationsConstraints );
                //annotationsConstraints.gridy++;
            }

            // Select first class
            addExampleButton[0].setSelected(true);

            // Checkbox panel
            checkBoxConstraints.anchor = GridBagConstraints.NORTHWEST;
            checkBoxConstraints.fill = GridBagConstraints.HORIZONTAL;
            checkBoxConstraints.gridheight = 1;
            checkBoxConstraints.gridx = 0;
            checkBoxConstraints.gridy = 0;

            checkBoxJPanel.setBorder(BorderFactory.createTitledBorder("Show Labels"));
            checkBoxJPanel.setLayout(boxCheckBoxes);

            for(int i = 0; i < myRoiManager.getNumOfClasses(); i++)
            {

                showClassCheckBox[i] = new JCheckBox("Add to " + myRoiManager.getClassLabel(i));
                showClassCheckBox[i].setToolTipText("Add markings of label '" + myRoiManager.getClassLabel(i) + "'");
                showClassCheckBox[i].setSelected(true);
                checkBoxConstraints.insets = new Insets(5, 5, 6, 6);

                checkBoxJPanel.add( showClassCheckBox[i], checkBoxConstraints );
                checkBoxConstraints.gridy++;

                checkBoxConstraints.insets = new Insets(0,0,0,0);

                //annotationsPanel.add( exampleList[i], annotationsConstraints );
                //annotationsConstraints.gridy++;
            }

            addListeners();

            // Labels panel (includes annotations panel)
            GridBagLayout labelsLayout = new GridBagLayout();
            GridBagConstraints labelsConstraints = new GridBagConstraints();
            labelsJPanel.setLayout( labelsLayout );
            labelsConstraints.anchor = GridBagConstraints.NORTHWEST;
            labelsConstraints.fill = GridBagConstraints.HORIZONTAL;
            labelsConstraints.gridwidth = 1;
            labelsConstraints.gridheight = 1;
            labelsConstraints.gridx = 0;
            labelsConstraints.gridy = 0;
            labelsJPanel.add( annotationsPanel, labelsConstraints );
            labelsConstraints.gridy++;
            labelsJPanel.add(checkBoxJPanel,labelsConstraints);

            // Scroll panel for the label panel
            scrollPanel = new JScrollPane( labelsJPanel );
            scrollPanel.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_NEVER );
            scrollPanel.setMinimumSize( labelsJPanel.getPreferredSize() );

            // Training panel (left side of the GUI)
            trainingJPanel.setBorder(BorderFactory.createTitledBorder("Training"));
            GridBagLayout trainingLayout = new GridBagLayout();
            GridBagConstraints trainingConstraints = new GridBagConstraints();
            trainingConstraints.anchor = GridBagConstraints.NORTHWEST;
            trainingConstraints.fill = GridBagConstraints.HORIZONTAL;
            trainingConstraints.gridwidth = 1;
            trainingConstraints.gridheight = 1;
            trainingConstraints.gridx = 0;
            trainingConstraints.gridy = 0;
            trainingConstraints.insets = new Insets(5, 5, 6, 6);
            trainingJPanel.setLayout(trainingLayout);

            trainingJPanel.add(overlayButton, trainingConstraints);
            trainingConstraints.gridy++;
            trainingJPanel.add(scissorSelectButton, trainingConstraints);
            trainingConstraints.gridy++;
            trainingJPanel.add(stopScissorButton, trainingConstraints);
            trainingConstraints.gridy++;
            trainingJPanel.add(startModifyScissorSelection, trainingConstraints);
            trainingConstraints.gridy++;
            trainingJPanel.add(stopModifyScissorSelection, trainingConstraints);
            trainingConstraints.gridy++;

            // Options panel
            optionsJPanel.setBorder(BorderFactory.createTitledBorder("Options"));
            GridBagLayout optionsLayout = new GridBagLayout();
            GridBagConstraints optionsConstraints = new GridBagConstraints();
            optionsConstraints.anchor = GridBagConstraints.NORTHWEST;
            optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
            optionsConstraints.gridwidth = 1;
            optionsConstraints.gridheight = 1;
            optionsConstraints.gridx = 0;
            optionsConstraints.gridy = 0;
            optionsConstraints.insets = new Insets(5, 5, 6, 6);
            optionsJPanel.setLayout(optionsLayout);

            optionsJPanel.add(saveOverlayButton,optionsConstraints);
            optionsConstraints.gridy++;

            //Roi Manager Panel
            roiManagerPanel.setBorder(BorderFactory.createTitledBorder("Roi Manager"));
            BorderLayout roiManagerLayout = new BorderLayout();
            roiManagerPanel.setLayout(roiManagerLayout);
            roiManagerPanel.constructRoiManagerPanel();

            // Buttons panel (including training and options)
            GridBagLayout buttonsLayout = new GridBagLayout();
            GridBagConstraints buttonsConstraints = new GridBagConstraints();
            buttonsPanel.setLayout(buttonsLayout);
            buttonsConstraints.anchor = GridBagConstraints.NORTHWEST;
            buttonsConstraints.fill = GridBagConstraints.HORIZONTAL;
            buttonsConstraints.gridwidth = 1;
            buttonsConstraints.gridheight = 1;
            buttonsConstraints.gridx = 0;
            buttonsConstraints.gridy = 0;
            buttonsPanel.add(trainingJPanel, buttonsConstraints);
            buttonsConstraints.gridy++;
            buttonsPanel.add(optionsJPanel, buttonsConstraints);
            buttonsConstraints.gridy++;
            buttonsPanel.add(roiManagerPanel, buttonsConstraints);
            buttonsConstraints.gridy++;
            buttonsConstraints.insets = new Insets(5, 5, 6, 6);

            GridBagLayout layout = new GridBagLayout();
            GridBagConstraints allConstraints = new GridBagConstraints();
            all.setLayout(layout);

            allConstraints.anchor = GridBagConstraints.NORTHWEST;
            allConstraints.fill = GridBagConstraints.BOTH;
            allConstraints.gridwidth = 1;
            allConstraints.gridheight = 2;
            allConstraints.gridx = 0;
            allConstraints.gridy = 0;
            allConstraints.weightx = 0;
            allConstraints.weighty = 0;

            all.add(buttonsPanel, allConstraints);

            allConstraints.gridx++;
            allConstraints.weightx = 1;
            allConstraints.weighty = 1;
            allConstraints.gridheight = 1;
            all.add(canvas, allConstraints);

            allConstraints.gridy++;
            allConstraints.weightx = 0;
            allConstraints.weighty = 0;
            if(null != sliceSelector)
                all.add(sliceSelector, allConstraints);
            allConstraints.gridy--;

            allConstraints.gridx++;
            allConstraints.anchor = GridBagConstraints.NORTHEAST;
            allConstraints.weightx = 0;
            allConstraints.weighty = 0;
            allConstraints.gridheight = 1;
            all.add( scrollPanel, allConstraints );

            GridBagLayout wingb = new GridBagLayout();
            GridBagConstraints winc = new GridBagConstraints();
            winc.anchor = GridBagConstraints.NORTHWEST;
            winc.fill = GridBagConstraints.BOTH;
            winc.weightx = 1;
            winc.weighty = 1;
            setLayout(wingb);
            add(all, winc);

            // Fix minimum size to the preferred size at this point
            pack();
            setMinimumSize( getPreferredSize() );


            // Propagate all listeners
            for (Component p : new Component[]{all, buttonsPanel}) {
                for (KeyListener kl : getKeyListeners()) {
                    p.addKeyListener(kl);
                }
            }

            addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    super.windowClosing( e );
                    // cleanup
                    if( null != trainingImage )
                    {
                        // display training image
                        if( null == trainingImage.getWindow() )
                            trainingImage.show();
                        trainingImage.getWindow().setVisible( true );
                    }

                    // Stop any thread from the segmentator

                    exec.shutdownNow();

                    for(int i = 0; i < myRoiManager.getNumOfClasses(); i++)
                        addExampleButton[i].removeActionListener(listener);
                    overlayButton.removeActionListener(listener);
                    scissorSelectButton.removeActionListener(listener);
                    stopScissorButton.removeActionListener(listener);
                    startModifyScissorSelection.removeActionListener(listener);
                    stopModifyScissorSelection.removeActionListener(listener);
                    saveOverlayButton.removeActionListener(listener);

                }
            });

            canvas.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent ce) {
                    Rectangle r = canvas.getBounds();
                    canvas.setDstDimensions(r.width, r.height);
                }
            });

        }

        private void addListeners(){

            //ic is the image canvas, this gets set to the custom canvas when the constructor makes the object.
            final CustomCanvas canvas = (CustomCanvas) getCanvas();

            // Add listeners
            for(int i = 0; i < myRoiManager.getNumOfClasses(); i++) {
                addExampleButton[i].addActionListener(listener);
                showClassCheckBox[i].addItemListener(itemListener);
            }

            overlayButton.addActionListener(listener);
            scissorSelectButton.addActionListener(listener);
            stopScissorButton.addActionListener(listener);
            stopModifyScissorSelection.addActionListener(listener);
            startModifyScissorSelection.addActionListener(listener);
            saveOverlayButton.addActionListener(listener);

            MouseListener mouseListener = new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {

                }

                @Override
                public void mousePressed(MouseEvent e) {

                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if(scissors.activeFlag) {

                        exec.submit(new Runnable() {
                            @Override
                            public void run() {
                                //IJ.log("Mouse Released");
                                int x = e.getX();
                                int y = e.getY();
                                int offscreenX = getCanvas().offScreenX(x);
                                int offscreenY = getCanvas().offScreenY(y);
                                //scissors.addUserSelectedPoints(new Point(offscreenX, offscreenY));
                                IJ.log("Mouse released : " + offscreenX + " " + offscreenY);
                                updateScissorPath();
                            }
                        });
                    }else if (roiListenerActive) {
                        exec.submit(new Runnable() {
                            @Override
                            public void run() {
                                updateScissorPath();
                            }
                        });
                    }


                }

                @Override
                public void mouseEntered(MouseEvent e) {

                }

                @Override
                public void mouseExited(MouseEvent e) {

                }

            };

            addMouseListener(mouseListener);
            canvas.addMouseListener(mouseListener);


            // add especial listener if the training image is a stack
            if(null != sliceSelector)
            {
                // set slice selector to the correct number
                sliceSelector.setValue( imp.getCurrentSlice() );
                // add adjustment listener to the scroll bar
                sliceSelector.addAdjustmentListener(new AdjustmentListener()
                {

                    public void adjustmentValueChanged(final AdjustmentEvent e) {
                        exec.submit(new Runnable() {
                            public void run() {
                                if(e.getSource() == sliceSelector)
                                {
                                    displayImage.killRoi();
                                    drawExamples();
                                    updateExampleLists();
                                    //updateResultOverlay();
                                    if(showColorOverlay)
                                    {
                                        updateResultOverlay();
                                        displayImage.updateAndDraw();
                                    }
                                }

                            }
                        });

                    }
                });

                // mouse wheel listener to update the rois while scrolling
                addMouseWheelListener(new MouseWheelListener() {

                    @Override
                    public void mouseWheelMoved(final MouseWheelEvent e) {

                        exec.submit(new Runnable() {
                            public void run()
                            {
                                //IJ.log("moving scroll");
                                displayImage.killRoi();
                                drawExamples();
                                updateExampleLists();
                                if(showColorOverlay)
                                {
                                    updateResultOverlay();
                                    displayImage.updateAndDraw();
                                }
                            }
                        });

                    }
                });

                // key listener to repaint the display image and the traces
                // when using the keys to scroll the stack
                KeyListener keyListener = new KeyListener() {

                    @Override
                    public void keyTyped(KeyEvent e) {}

                    @Override
                    public void keyReleased(final KeyEvent e) {
                        exec.submit(new Runnable() {
                            public void run()
                            {
                                if(e.getKeyCode() == KeyEvent.VK_LEFT ||
                                        e.getKeyCode() == KeyEvent.VK_RIGHT ||
                                        e.getKeyCode() == KeyEvent.VK_LESS ||
                                        e.getKeyCode() == KeyEvent.VK_GREATER ||
                                        e.getKeyCode() == KeyEvent.VK_COMMA ||
                                        e.getKeyCode() == KeyEvent.VK_PERIOD)
                                {
                                    IJ.log("moving scroll");
                                    displayImage.killRoi();
                                    updateExampleLists();
                                    drawExamples();
                                    if(showColorOverlay)
                                    {
                                        updateResultOverlay();
                                        displayImage.updateAndDraw();
                                    }
                                }
                            }
                        });

                    }

                    @Override
                    public void keyPressed(KeyEvent e) {}
                };
                // add key listener to the window and the canvas
                addKeyListener(keyListener);
                canvas.addKeyListener(keyListener);
            }

        }


        /**
         * Get the Weka segmentation object. This tricks allows to
         * extract the information from the plugin and use it from
         * static methods.
         *
         * @return Weka segmentation data associated to the window.
         */
        protected WekaSegmentation getWekaSegmentation()
        {
            return wekaSegmentation;
        }

        protected MyRoiManager getRoiManager(){
            return myRoiManager;
        }

        /**
         * Get current label lookup table (used to color the results)
         * @return current overlay LUT
         */
        public LUT getOverlayLUT()
        {
            return overlayLUT;
        }

        /**
         * Draw the painted traces on the display image
         */
        protected void drawExamples()
        {
            final int currentSlice = displayImage.getCurrentSlice();

            for(int i = 0; i < myRoiManager.getNumOfClasses(); i++)
            {
                roiOverlay[i].setColor(Label.values()[i].color);
                final ArrayList< Roi > rois = new ArrayList<Roi>();
                for (Roi r : myRoiManager.getRoiList(i, currentSlice))
                {
                    rois.add(r);
                    //IJ.log("painted ROI: " + r + " in color "+ colors[i] + ", slice = " + currentSlice);
                }
                roiOverlay[i].setRoi(rois);
            }

            displayImage.updateAndDraw();
        }

        /**
         * Update the example lists in the GUI
         */
        protected void updateExampleLists()
        {
            final int currentSlice = displayImage.getCurrentSlice();

            for(int i = 0; i < myRoiManager.getNumOfClasses(); i++)
            {
                exampleList[i].removeAll();
                for(int j = 0; j< myRoiManager.getRoiList(i, currentSlice).size(); j++)
                    exampleList[i].add("trace " + j + " (Z=" + currentSlice+")");
            }

        }

        protected boolean isToogleEnabled()
        {
            return showColorOverlay;
        }

        /**
         * Get the displayed image. This method can be used to
         * extract the ROIs from the current image.
         *
         * @return image being displayed in the custom window
         */
        protected ImagePlus getDisplayImage()
        {
            return this.getImagePlus();
        }

        /**
         * Set the slice selector enable option
         * @param b true/false to enable/disable the slice selector
         */
        public void setSliceSelectorEnabled(boolean b)
        {
            if(null != sliceSelector)
                sliceSelector.setEnabled(b);
        }

        /**
         * Repaint all panels
         */
        public void repaintAll()
        {
            this.annotationsPanel.repaint();
            getCanvas().repaint();
            this.buttonsPanel.repaint();
            this.roiManagerPanel.repaint();
            this.all.repaint();
        }

        /**
         * Add new segmentation class (new label and new list on the right side)
         */
        public void addClass()
        {
            int classNum = numOfClasses;

            exampleList[classNum] = new java.awt.List(5);
            exampleList[classNum].setForeground(Label.values()[classNum].color);

            exampleList[classNum].addActionListener(listener);
            exampleList[classNum].addItemListener(itemListener);
            addExampleButton[classNum] = new JButton("Add to " + myRoiManager.getClassLabel(classNum));

            annotationsConstraints.fill = GridBagConstraints.HORIZONTAL;
            annotationsConstraints.insets = new Insets(5, 5, 6, 6);

            boxAnnotation.setConstraints(addExampleButton[classNum], annotationsConstraints);
            annotationsPanel.add(addExampleButton[classNum]);
            annotationsConstraints.gridy++;

            annotationsConstraints.insets = new Insets(0,0,0,0);

            boxAnnotation.setConstraints(exampleList[classNum], annotationsConstraints);
            annotationsPanel.add(exampleList[classNum]);
            annotationsConstraints.gridy++;

            // Add listener to the new button
            addExampleButton[classNum].addActionListener(listener);

            numOfClasses++;

            // recalculate minimum size of scroll panel
            scrollPanel.setMinimumSize( labelsJPanel.getPreferredSize() );

            repaintAll();
        }

        /**
         * Set the image being displayed on the custom canvas
         * @param imp new image
         */
        public void setImagePlus(final ImagePlus imp)
        {
            super.imp = imp;
            ((CustomCanvas) super.getCanvas()).setImagePlus(imp);
            Dimension dim = new Dimension(Math.min(512, imp.getWidth()), Math.min(512, imp.getHeight()));
            ((CustomCanvas) super.getCanvas()).setDstDimensions(dim.width, dim.height);
            imp.setWindow(this);
            repaint();
        }

        /**
         * Enable / disable buttons
         * @param s enabling flag
         */
        protected void setButtonsEnabled(boolean s)
        {
            for(int i = 0; i < myRoiManager.getNumOfClasses(); i++)
            {
                exampleList[i].setEnabled(s);
                addExampleButton[i].setEnabled(s);
            }
            setSliceSelectorEnabled(s);
        }

        /**
         * Update buttons enabling depending on the current status of the plugin
         */
        protected void updateButtonsEnabling()
        {
            // While training, set disable all buttons except the train buttons,
            // which will be used to stop the training by the user.
            if( trainingFlag )
            {
                setButtonsEnabled( false );
            }
            else // If the training is not going on
            {
                final boolean classifierExists =  null != wekaSegmentation.getClassifier();
                final boolean resultExists = null != classifiedImage &&
                        null != classifiedImage.getProcessor();

                // Check if there are samples in any slice
                boolean examplesEmpty = true;
                for( int n = 1; n <= displayImage.getImageStackSize(); n++ )
                    for(int i = 0; i < myRoiManager.getNumOfClasses(); i ++)
                        if( wekaSegmentation.getExamples( i, n ).size() > 0)
                        {
                            examplesEmpty = false;
                            break;
                        }
                boolean loadedTrainingData = null != wekaSegmentation.getLoadedTrainingData();


                for(int i = 0; i < myRoiManager.getNumOfClasses(); i++)
                {
                    exampleList[i].setEnabled(true);
                    addExampleButton[i].setEnabled(true);
                }
                setSliceSelectorEnabled(true);
            }
        }

        /**
         * Toggle between overlay and original image with markings
         */
        void toggleOverlay()
        {
            showColorOverlay = !showColorOverlay;

            IJ.log("toggle overlay to: " + showColorOverlay);
            if (showColorOverlay && null != classifiedImages)
            {
                updateResultOverlay();
            }
            else {
                //This needs to change
                resultOverlay.setImage(null);
            }
            displayImage.updateAndDraw();
        }

        /**
         * Set a new result (classified) image
         * @param classifiedImage new result image
         */
        protected void setClassfiedImage(ImagePlus classifiedImage)
        {
            updateClassifiedImage(classifiedImage);
        }

        /**
         * Update the buttons to add classes with current information
         */
        public void updateAddClassButtons()
        {
            int wekaNumOfClasses = myRoiManager.getNumOfClasses();
            while (numOfClasses < wekaNumOfClasses)
                win.addClass();
            for (int i = 0; i < numOfClasses; i++)
                addExampleButton[i].setText("Add to " + wekaSegmentation.getClassLabel(i));

            win.updateButtonsEnabling();
            repaintWindow();
        }

        /**
         * Set the flag to inform the the training has finished or not
         *
         * @param b tranining complete flag
         */
        void setTrainingComplete(boolean b)
        {
            this.trainingComplete = b;
        }

        /**
         * Get training image
         * @return training image
         */
        public ImagePlus getTrainingImage()
        {
            return trainingImage;
        }
    }// end class CustomWindow

    /**
     * Plugin run method
     */
    public void run(String arg)
    {
        // check if the image should be process in 3D
        if( arg.equals( "3D" ) )
            isProcessing3D = true;


        // instantiate segmentation backend
        wekaSegmentation = new WekaSegmentation( isProcessing3D );


        //get current image
        if (null == WindowManager.getCurrentImage())
        {
            trainingImage = IJ.openImage();

            if (null == trainingImage) return; // user canceled open dialog
        }
        else
        {
            trainingImage = WindowManager.getCurrentImage();
            // hide input image (to avoid accidental closing)
            trainingImage.getWindow().setVisible( false );
        }


        if (Math.max(trainingImage.getWidth(), trainingImage.getHeight()) > 1024)
            IJ.log("Warning: at least one dimension of the image "  +
                    "is larger than 1024 pixels.\n" +
                    "Feature stack creation and classifier training " +
                    "might take some time depending on your computer.\n");


        //Instantiate Roi Manager
        myRoiManager = new MyRoiManager(trainingImage);



        for(int i = 0; i < myRoiManager.getNumOfClasses() ; i++)
        {
            exampleList[i] = new java.awt.List(5);
            exampleList[i].setForeground(Label.values()[i].color);
        }
        numOfClasses = myRoiManager.getNumOfClasses();

        //Commented out to conserve memory, will cause lots of errors for Weka code.
        //wekaSegmentation.setTrainingImage(trainingImage);

        // The display image is a copy of the training image (single image or stack)
        displayImage = trainingImage.duplicate();
        displayImage.setSlice( trainingImage.getCurrentSlice() );
        displayImage.setTitle( FasciaMain.PLUGIN_NAME + " " + FasciaMain.PLUGIN_VERSION );

        ij.gui.Toolbar.getInstance().setTool(ij.gui.Toolbar.FREELINE);

        //RoiScanner roiScanner = new RoiScanner(displayImage);
        //roiScanner.scanAndExport();
        IJ.log("No. of slices : " + displayImage.getNSlices());
        classifiedImages = new ImagePlus[displayImage.getNSlices()];
        groundTruthLabels = new boolean[displayImage.getNSlices()];

        interpolator = new Interpolator(displayImage);
        skinSelector = new SkinSelector();

        scissors = new IntelligentScissors();

        //Initialise stack of lines for interpolating
       /* for(int i = 0; i <  displayImage.getNSlices(); i++) {
            selectedPointStackList.add(new ArrayList<StackPointList>());
            for (int label = 0; label < numOfClasses; label++) {
                selectedPointStackList.get(i).add(new StackPointList(label, i));
            }
        }*/

        //Initialise the classifiedImages array of Image Plus objects and ground truth labels
        for( int i = 0; i < classifiedImages.length; i++){
            classifiedImages[i] = null;
            groundTruthLabels[i] = false;
        }

        //Build GUI
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        win = new CustomWindow(displayImage);
                        win.pack();
                    }
                });
    }

    /**
     * Add examples defined by the user to the corresponding list
     * in the GUI and the example list in the segmentation object.
     *
     * @param i GUI list index
     */
    private void addExamples(int i)
    {
         //Check temporary overlay to see if it is being used
        IJ.log(temporaryOverlay.getRoi() + "");
        IJ.log((temporaryOverlay.getRoi() != (null)) + "");
        if (temporaryOverlay.getRoi() != (null)){
            IJ.log(temporaryOverlay.getRoi().toString());
            final Roi r = (PolygonRoi) temporaryOverlay.getRoi().get(0);
            IJ.log("Adding fascia line roi " + r);
            IJ.log("Adding trace to list " + i);

            final int n = displayImage.getCurrentSlice();

            myRoiManager.addRoi(i, r, n);
            //selectedPointStackList.get(displayImage.getCurrentSlice()).get(i).addRoi(r);
            displayImage.killRoi();
            temporaryOverlay.killRoi();
            traceCounter[i]++;
            win.drawExamples();
            win.updateExampleLists();

        }else {
            IJ.log("Normal addition");
            //get selected pixels
            final Roi r = displayImage.getRoi();
            IJ.log("Adding roi " + r);

            if (null == r)
                return;

            IJ.log("Adding trace to list " + i);

            final int n = displayImage.getCurrentSlice();

            displayImage.killRoi();
            temporaryOverlay.killRoi();
            myRoiManager.addRoi(i, r, n);
            traceCounter[i]++;
            win.drawExamples();
            win.updateExampleLists();
        }
    }

    private void toggleClassOverlay(int classNum, boolean toggle){
        displayImage.killRoi();
        IJ.log("Toggling class" + myRoiManager.getClassLabel(classNum));
        if(toggle){
            ((OverlayedImageCanvas) win.getCanvas()).addOverlay(roiOverlay[classNum]);
            win.drawExamples();
            win.updateExampleLists();
        }else{
            ((OverlayedImageCanvas) win.getCanvas()).removeOverlay(roiOverlay[classNum]);
            win.drawExamples();
            win.updateExampleLists();
        }
    }

    //@Alexis Barltrop

    private void addExamples(int i, Roi roi){
        if(roi ==null) {
            return;
        }
        final int n = displayImage.getCurrentSlice();
        IJ.log("Adding trace to list " + i);
        displayImage.killRoi();
        myRoiManager.addRoi(i,roi,n);
        traceCounter[i]++;
        win.drawExamples();
        win.updateExampleLists();
    }

    /**
     * Update the result image
     *
     * @param classifiedImage new result image
     */
    public void updateClassifiedImage(ImagePlus classifiedImage)
    {
        this.classifiedImage = classifiedImage;
    }

    /**
     * Update the result image overlay with the corresponding slice
     */
    public void updateResultOverlay()
    {
        //((OverlayedImageCanvas)win.getCanvas()).removeOverlay(currentResultOverlay);
        win.resultOverlay.setImage(null);
        if ( classifiedImages[displayImage.getCurrentSlice()-1] != null ) {
            ImageProcessor overlay = classifiedImages[displayImage.getCurrentSlice()-1].getProcessor().duplicate();

            //ImageOverlay imageOverlay = new ImageOverlay(overlay);
            //imageOverlay.setComposite( transparency050 );
            //((OverlayedImageCanvas)win.getCanvas()).addOverlay(imageOverlay);
            //currentResultOverlay = imageOverlay;
            //IJ.log("updating overlay with result from slice " + displayImage.getCurrentSlice());

            win.resultOverlay.setImage(overlay);
        }else{

            //((OverlayedImageCanvas)win.getCanvas()).removeOverlay(currentResultOverlay);
        }
        win.drawExamples();
    }

    /**
     * Select a list and deselect the others
     *
     * @param e item event (originated by a list)
     * @param i list index
     */
    void listSelected(final ItemEvent e, final int i)
    {
        // find the right slice of the corresponding ROI

        win.drawExamples();
        displayImage.setColor(Color.YELLOW);

        for(int j = 0; j < myRoiManager.getNumOfClasses(); j++)
        {
            if (j == i)
            {
                final Roi newRoi =
                        wekaSegmentation.getExamples(i, displayImage.getCurrentSlice())
                                .get(exampleList[i].getSelectedIndex());
                // Set selected trace as current ROI
                newRoi.setImage(displayImage);
                displayImage.setRoi(newRoi);
            }
            else
                exampleList[j].deselect(exampleList[j].getSelectedIndex());
        }

        displayImage.updateAndDraw();
    }

    /**
     * Delete one of the ROIs
     *
     * @param e action event
     */
    void deleteSelected(final ActionEvent e)
    {
        for(int i = 0; i < myRoiManager.getNumOfClasses(); i++)
            if (e.getSource() == exampleList[i])
            {
                //delete item from ROI
                int index = exampleList[i].getSelectedIndex();

                // kill Roi from displayed image
                if(displayImage.getRoi().equals(
                        wekaSegmentation.getExamples(i, displayImage.getCurrentSlice()).get(index) ))
                    displayImage.killRoi();

                // delete item from the list of ROIs of that class and slice
                wekaSegmentation.deleteExample(i, displayImage.getCurrentSlice(), index);
                //delete item from GUI list
                exampleList[i].remove(index);

            }

        win.drawExamples();
        win.updateExampleLists();
    }

     /**
     * Add new class in the panel (up to MAX_NUM_CLASSES)
     */
    private void addNewClass()
    {
        if(myRoiManager.getNumOfClasses() == WekaSegmentation.MAX_NUM_CLASSES)
        {
            IJ.showMessage("Trainable Weka Segmentation", "Sorry, maximum number of classes has been reached");
            return;
        }

        String inputName = JOptionPane.showInputDialog("Please input a new label name");

        if(null == inputName)
            return;


        if (null == inputName || 0 == inputName.length())
        {
            IJ.error("Invalid name for class");
            return;
        }
        inputName = inputName.trim();

        if (0 == inputName.toLowerCase().indexOf("add to "))
            inputName = inputName.substring(7);

        // Add new name to the list of labels
        wekaSegmentation.setClassLabel(myRoiManager.getNumOfClasses(), inputName);
        wekaSegmentation.addClass();

        // Add new class label and list
        win.addClass();

        repaintWindow();

    }

    /**
     * Repaint whole window
     */
    private void repaintWindow()
    {
        // Repaint window
        SwingUtilities.invokeLater(
                new Runnable() {
                    public void run() {
                        win.invalidate();
                        win.validate();
                        win.repaint();
                    }
                });
    }

  /*  // Quite of a hack from Johannes Schindelin:
    // use reflection to insert classifiers, since there is no other method to do that...
    static {
        try {
            IJ.showStatus("Loading Weka properties...");
            IJ.log("Loading Weka properties...");
            Field field = GenericObjectEditor.class.getDeclaredField("EDITOR_PROPERTIES");
            field.setAccessible(true);
            Properties editorProperties = (Properties)field.get(null);
            String key = "weka.classifiers.Classifier";
            String value = editorProperties.getProperty(key);
            value += ",hr.irb.fastRandomForest.FastRandomForest";
            editorProperties.setProperty(key, value);
            //new Exception("insert").printStackTrace();
            //System.err.println("value: " + value);
            WekaPackageManager.loadPackages( true );
            // add classifiers from properties (needed after upgrade to WEKA version 3.7.11)
            PluginManager.addFromProperties(editorProperties);
        } catch (Exception e) {
            IJ.error("Could not insert my own cool classifiers!");
        }
    }*/


    /**
     * Add the current ROI to a specific class and slice.
     *
     * @param classNum string representing the class index
     * @param nSlice string representing the slice number
     */
    public static void addTrace(
            String classNum,
            String nSlice)
    {
        final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
        if( iw instanceof CustomWindow )
        {
            final CustomWindow win = (CustomWindow) iw;
            final MyRoiManager myRoiManager = win.getRoiManager();
            final Roi roi = win.getDisplayImage().getRoi();
            myRoiManager.addRoi(Integer.parseInt(classNum),
                    roi, Integer.parseInt(nSlice));
            win.getDisplayImage().killRoi();
            win.drawExamples();
            win.updateExampleLists();
        }
    }

    /**
     * Delete a specific ROI from the list of a specific class and slice
     *
     * @param classNum string representing the class index
     * @param nSlice string representing the slice number
     * @param index string representing the index of the trace to remove
     */
    public static void deleteTrace(
            String classNum,
            String nSlice,
            String index)
    {
        final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
        if( iw instanceof CustomWindow )
        {
            final CustomWindow win = (CustomWindow) iw;
            final MyRoiManager myRoiManager = win.getRoiManager();
            myRoiManager.deleteRoi(Integer.parseInt(classNum),
                    Integer.parseInt(nSlice),
                    Integer.parseInt(index) );
            win.getDisplayImage().killRoi();
            win.drawExamples();
            win.updateExampleLists();
        }
    }


    /**
     * Toggle current result overlay image
     */
    public static void toggleOverlay()
    {
        final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
        if( iw instanceof CustomWindow )
        {
            final CustomWindow win = (CustomWindow) iw;
            win.toggleOverlay();
        }
    }

    /**
     * Create a new class
     *
     * @param inputName new class name
     */
    public static void createNewClass( String inputName )
    {
        final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
        if( iw instanceof CustomWindow )
        {
            final CustomWindow win = (CustomWindow) iw;
            final WekaSegmentation wekaSegmentation = win.getWekaSegmentation();

            if (null == inputName || 0 == inputName.length())
            {
                IJ.error("Invalid name for class");
                return;
            }
            inputName = inputName.trim();

            if (0 == inputName.toLowerCase().indexOf("add to "))
                inputName = inputName.substring(7);

            // Add new name to the list of labels
            wekaSegmentation.setClassLabel(wekaSegmentation.getNumOfClasses(), inputName);
            wekaSegmentation.addClass();

            // Add new class label and list
            win.addClass();

            win.updateAddClassButtons();
        }
    }

    /**
     * Change a class name
     *
     * @param classIndex index of the class to change
     * @param className new class name
     */
    public static void changeClassName(String classIndex, String className)
    {
        final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
        if( iw instanceof CustomWindow )
        {
            final CustomWindow win = (CustomWindow) iw;
            final MyRoiManager myRoiManager = win.getRoiManager();

            int classNum = Integer.parseInt(classIndex);
            myRoiManager.setClassLabel(classNum, className);
            win.updateAddClassButtons();
            win.pack();
        }
    }

    /**
     * Set overlay opacity
     * @param newOpacity string containing the new opacity value (integer 0-100)
     */
    public static void setOpacity( String newOpacity )
    {
        final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
        if( iw instanceof CustomWindow )
        {
            final CustomWindow win = (CustomWindow) iw;
            win.overlayOpacity = Integer.parseInt(newOpacity);
            AlphaComposite alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,  win.overlayOpacity  / 100f);
            win.resultOverlay.setComposite(alpha);
        }
    }

    /**
     * Create label image out of the current user traces. For convention, the
     * label zero is used to define pixels with no class assigned. The rest of
     * integer values correspond to the order of the classes (1 for the first
     * class, 2 for the second class, etc.).
     *
     * @return label image containing user-defined traces (zero for undefined pixels)
     */
    public ImagePlus getLabelImage(int sliceNumber)
    {
        final ImageWindow iw = WindowManager.getCurrentImage().getWindow();
        if( iw instanceof CustomWindow )
        {
            final CustomWindow win = (CustomWindow) iw;
            final MyRoiManager myRoiManager = win.getRoiManager();

            final int numClasses = myRoiManager.getNumOfClasses();
            final int width = win.getTrainingImage().getWidth();
            final int height = win.getTrainingImage().getHeight();
            //final int depth = win.getTrainingImage().getNSlices();
            final int depth = 1;

            final ImageStack labelStack;
            labelStack = ImageStack.create( width, height, depth, 24);

            final ImagePlus labelImage = new ImagePlus( "Labels", labelStack );

           /* for( int i=0; i<depth; i++ )
            {
                */
            IJ.log("i value : " + (sliceNumber-1));

            //labelImage.setSlice( sliceNumber );
            final ImageProcessor ip = labelImage.getProcessor();

            if(!(classifiedImages[sliceNumber-1] == null)) {
                IJ.log( " Copying");
                ip.copyBits(classifiedImages[sliceNumber-1].getProcessor(),0,0,Blitter.COPY);
            }
            for( int j=0; j<numClasses; j++ )
            {
                IJ.log( " Point 0");
                List<Roi> rois = myRoiManager.getRoiList(j,sliceNumber);
                for( final Roi r : rois )
                {
                    IJ.log("Point A");

                    //IJ.log(ip.toString());
                    //IJ.log("Point B");
                    //Import colours from enum?
                    //How to set backgroundd to transparent?
                    Color color = Label.values()[j].color;
                    IJ.log("Point C");
                    //IJ.log("Color" + color);
                    ip.setColor(color);
                    IJ.log("Point D");
                    if( r.isLine() )
                    {
                        //IJ.log("Point E");
                        ip.setLineWidth( Math.round( r.getStrokeWidth() ) );
                        ip.draw( r );
                        IJ.log("Point F");
                    }
                    else {
                        IJ.log("Point G");
                        ip.fill(r);
                    }
                }
                IJ.log("Done : " + j);
            }
            //}
            labelImage.setSlice( 1 );
            //labelImage.setDisplayRange( 0, numClasses );
            //IJ.log("Point Z");
            return labelImage;
        }
        return null;
    }
}// end of FasciaMain class

