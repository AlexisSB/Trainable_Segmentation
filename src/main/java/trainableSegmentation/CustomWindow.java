package trainableSegmentation;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.gui.StackWindow;
import ij.process.LUT;

import javax.swing.*;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.*;

/**
 * Custom window to define the Trainable Weka Segmentation GUI
 */
public class CustomWindow extends StackWindow {
    /**
     * default serial version UID
     */
    private static final long serialVersionUID = 1L;
    /**
     * layout for annotation panel
     */
    private GridBagLayout boxAnnotation = new GridBagLayout();
    /**
     * constraints for annotation panel
     */
    private GridBagConstraints annotationsConstraints = new GridBagConstraints();

    /**
     * scroll panel for the label/annotation panel
     */
    private JScrollPane scrollPanel = null;

    /**
     * panel containing the annotations panel (right side of the GUI)
     */
    private JPanel labelsRightSideJPanel = new JPanel();
    /**
     * Panel with class radio buttons and lists
     */
    private JPanel annotationsPanel = new JPanel();

    /**
     * buttons panel (left side of the GUI)
     */
    private JPanel buttonsPanel = new JPanel();
    /**
     * training panel (included in the left side of the GUI)
     */
    private JPanel trainingJPanel = new JPanel();
    /**
     * options panel (included in the left side of the GUI)
     */
    private JPanel optionsJPanel = new JPanel();
    /**
     * main GUI panel (containing the buttons panel on the left,
     * the image in the center and the annotations panel on the right
     */
    private Panel all = new Panel();

    /**
     * 50% alpha composite
     */
    private final Composite transparency050 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f);
    /** 25% alpha composite */
    //final Composite transparency025 = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f );
    /**
     * opacity (in %) of the result overlay image
     */
    private int overlayOpacity = 33;
    /**
     * alpha composite for the result overlay image
     */
    private Composite overlayAlpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, overlayOpacity / 100f);
    /**
     * current segmentation result overlay
     */
    private ImageOverlay resultOverlay;

    /**
     * boolean flag set to true when training is complete
     */
    private boolean trainingComplete = false;

    private JCheckBox[] showRoiClassSelectButtons = new JCheckBox[FasciaMain.NUM_OF_CLASSES+1];



    /**
     * Construct the plugin window
     *
     * @param imp input image
     */
    CustomWindow(ImagePlus imp) {
        super(imp, new CustomCanvas(imp));

        final CustomCanvas canvas = (CustomCanvas) getCanvas();

        // Remove the canvas from the window, to add it later
        removeAll();

        setTitle(FasciaMain.PLUGIN_NAME + " " + FasciaMain.PLUGIN_VERSION);

        // Annotations panel
        annotationsConstraints.anchor = GridBagConstraints.NORTHWEST;
        annotationsConstraints.fill = GridBagConstraints.HORIZONTAL;
        annotationsConstraints.gridwidth = 1;
        annotationsConstraints.gridheight = 1;
        annotationsConstraints.gridx = 0;
        annotationsConstraints.gridy = 0;

        annotationsPanel.setBorder(BorderFactory.createTitledBorder("Labels"));
        annotationsPanel.setLayout(boxAnnotation);

        for (int i = 0; i < FasciaMain.NUM_OF_CLASSES; i++) {
            showRoiClassSelectButtons[i] = new JCheckBox(FasciaMain.LABEL_STRINGS[i]);
            showRoiClassSelectButtons[i].setToolTipText("Add markings of label '" + FasciaMain.LABEL_STRINGS[i] + "'");

            annotationsConstraints.insets = new Insets(5, 5, 6, 6);

            annotationsPanel.add(showRoiClassSelectButtons[i], annotationsConstraints);
            annotationsConstraints.gridy++;

//            annotationsConstraints.insets = new Insets(0, 0, 0, 0);
  //          annotationsPanel.add(exampleList[i], annotationsConstraints);
    //        annotationsConstraints.gridy++;
        }

        showRoiClassSelectButtons[FasciaMain.NUM_OF_CLASSES] = new JCheckBox("Show All");
        annotationsConstraints.insets = new Insets(5, 5, 6, 6);

        annotationsPanel.add(showRoiClassSelectButtons[FasciaMain.NUM_OF_CLASSES], annotationsConstraints);
        annotationsConstraints.gridy++;

        // Select first class
        //addExampleButton[0].setSelected(true);

        // Add listeners
        for (int i = 0; i < FasciaMain.NUM_OF_CLASSES+1; i++)
            showRoiClassSelectButtons[i].addActionListener(listener);

        /*overlayButton.addActionListener(listener);
        loadDataButton.addActionListener(listener);
        saveDataButton.addActionListener(listener);
        addClassButton.addActionListener(listener);
        settingsButton.addActionListener(listener);*/


        //@author ALEXIS BARLTROP
        /** Add ROI select button */
        //loadROIButton.addActionListener(listener);

        // add especial listener if the training image is a stack
        if (null != sliceSelector) {
            // set slice selector to the correct number
            sliceSelector.setValue(imp.getCurrentSlice());
            // add adjustment listener to the scroll bar
            sliceSelector.addAdjustmentListener(new AdjustmentListener() {

                //This listener changes the rois according to the slice number
                /*
                public void adjustmentValueChanged(final AdjustmentEvent e) {
                    exec.submit(new Runnable() {
                        public void run() {
                            if (e.getSource() == sliceSelector) {
                                displayImage.killRoi();
                                //drawExamples();
                                //updateExampleLists();
                                if (showColorOverlay) {
                                    updateResultOverlay();
                                    displayImage.updateAndDraw();
                                }
                            }

                        }
                        */
                    });

                }
           // });

            /*// mouse wheel listener to update the rois while scrolling
            addMouseWheelListener(new MouseWheelListener() {

                @Override
                public void mouseWheelMoved(final MouseWheelEvent e) {

                    exec.submit(new Runnable() {
                        public void run() {
                            //IJ.log("moving scroll");
                            displayImage.killRoi();
                            //drawExamples();
                            //updateExampleLists();
                            if (showColorOverlay) {
                                updateResultOverlay();
                                displayImage.updateAndDraw();
                            }
                        }
                    });

                }
            });*/

            /*// key listener to repaint the display image and the traces
            // when using the keys to scroll the stack
            KeyListener keyListener = new KeyListener() {

                @Override
                public void keyTyped(KeyEvent e) {
                }

                @Override
                public void keyReleased(final KeyEvent e) {
                    exec.submit(new Runnable() {
                        public void run() {
                            if (e.getKeyCode() == KeyEvent.VK_LEFT ||
                                    e.getKeyCode() == KeyEvent.VK_RIGHT ||
                                    e.getKeyCode() == KeyEvent.VK_LESS ||
                                    e.getKeyCode() == KeyEvent.VK_GREATER ||
                                    e.getKeyCode() == KeyEvent.VK_COMMA ||
                                    e.getKeyCode() == KeyEvent.VK_PERIOD) {
                                //IJ.log("moving scroll");
                                displayImage.killRoi();
                                //updateExampleLists();
                                //drawExamples();
                                if (showColorOverlay) {
                                    updateResultOverlay();
                                    displayImage.updateAndDraw();
                                }
                            }
                        }
                    });

                }

                @Override
                public void keyPressed(KeyEvent e) {
                }
            };

            // add key listener to the window and the canvas
            addKeyListener(keyListener);
            canvas.addKeyListener(keyListener);
*/
        }

        // Labels panel (includes annotations panel)
        GridBagLayout labelsLayout = new GridBagLayout();
        GridBagConstraints labelsConstraints = new GridBagConstraints();
        labelsRightSideJPanel.setLayout(labelsLayout);
        labelsConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelsConstraints.fill = GridBagConstraints.HORIZONTAL;
        labelsConstraints.gridwidth = 1;
        labelsConstraints.gridheight = 1;
        labelsConstraints.gridx = 0;
        labelsConstraints.gridy = 0;
        labelsRightSideJPanel.add(annotationsPanel, labelsConstraints);

        // Scroll panel for the label panel
        scrollPanel = new JScrollPane(labelsRightSideJPanel);
        scrollPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanel.setMinimumSize(labelsRightSideJPanel.getPreferredSize());


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

        //@Alexis Barltrop
        optionsJPanel.add(loadROIButton, optionsConstraints);
        optionsConstraints.gridy++;
        //optionsJPanel.add(applyButton, optionsConstraints);
        //optionsConstraints.gridy++;
        //optionsJPanel.add(loadClassifierButton, optionsConstraints);
        //optionsConstraints.gridy++;
        //optionsJPanel.add(saveClassifierButton, optionsConstraints);
        //optionsConstraints.gridy++;
        //optionsJPanel.add(loadDataButton, optionsConstraints);
        //optionsConstraints.gridy++;
        //optionsJPanel.add(saveDataButton, optionsConstraints);
        //optionsConstraints.gridy++;
        optionsJPanel.add(addClassButton, optionsConstraints);
        optionsConstraints.gridy++;
        optionsJPanel.add(settingsButton, optionsConstraints);
        optionsConstraints.gridy++;

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
        if (null != sliceSelector)
            all.add(sliceSelector, allConstraints);
        allConstraints.gridy--;

        allConstraints.gridx++;
        allConstraints.anchor = GridBagConstraints.NORTHEAST;
        allConstraints.weightx = 0;
        allConstraints.weighty = 0;
        allConstraints.gridheight = 1;
        all.add(scrollPanel, allConstraints);

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
        setMinimumSize(getPreferredSize());


        // Propagate all listeners
        for (Component p : new Component[]{all, buttonsPanel}) {
            for (KeyListener kl : getKeyListeners()) {
                p.addKeyListener(kl);
            }
        }

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                // cleanup
                if (null != trainingImage) {
                    // display training image
                    if (null == trainingImage.getWindow())
                        trainingImage.show();
                    trainingImage.getWindow().setVisible(true);
                }
                // Stop any thread from the segmentator
                if (null != trainingTask)
                    trainingTask.interrupt();

                exec.shutdownNow();

                for (int i = 0; i < numOfClasses; i++)
                    addExampleButton[i].removeActionListener(listener);

                overlayButton.removeActionListener(listener);

                loadDataButton.removeActionListener(listener);
                saveDataButton.removeActionListener(listener);
                addClassButton.removeActionListener(listener);
                settingsButton.removeActionListener(listener);


                //@ALEXIS BARLTROP
                loadROIButton.removeActionListener(listener);

                // Set number of classes back to 2

            }
        });

        canvas.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ce) {
                Rectangle r = canvas.getBounds();
                canvas.setDstDimensions(r.width, r.height);
            }
        });

    }


    /**
     * Get current label lookup table (used to color the results)
     *
     * @return current overlay LUT
     */
    public LUT getOverlayLUT() {
        return overlayLUT;
    }

    /*  *//**
     * Draw the painted traces on the display image
     *//*
        protected void drawExamples()
        {
            final int currentSlice = displayImage.getCurrentSlice();

            for(int i = 0; i < numOfClasses; i++)
            {
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

        */

    /**
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

        }*/
    protected boolean isToogleEnabled() {
        return showColorOverlay;
    }

    /**
     * Get the displayed image. This method can be used to
     * extract the ROIs from the current image.
     *
     * @return image being displayed in the custom window
     */
    protected ImagePlus getDisplayImage() {
        return this.getImagePlus();
    }

    /**
     * Set the slice selector enable option
     *
     * @param b true/false to enable/disable the slice selector
     */
    public void setSliceSelectorEnabled(boolean b) {
        if (null != sliceSelector)
            sliceSelector.setEnabled(b);
    }

    /**
     * Repaint all panels
     */
    public void repaintAll() {
        this.annotationsPanel.repaint();
        getCanvas().repaint();
        this.buttonsPanel.repaint();
        this.all.repaint();
    }

    /**
     * Add new segmentation class (new label and new list on the right side)
     */
    public void addClass() {
        int classNum = numOfClasses;

        exampleList[classNum] = new java.awt.List(5);
        exampleList[classNum].setForeground(colors[classNum]);

        exampleList[classNum].addActionListener(listener);
        exampleList[classNum].addItemListener(itemListener);
        addExampleButton[classNum] = new JButton("Add to " + LABEL_STRINGS[classNum]);

        annotationsConstraints.fill = GridBagConstraints.HORIZONTAL;
        annotationsConstraints.insets = new Insets(5, 5, 6, 6);

        boxAnnotation.setConstraints(addExampleButton[classNum], annotationsConstraints);
        annotationsPanel.add(addExampleButton[classNum]);
        annotationsConstraints.gridy++;

        annotationsConstraints.insets = new Insets(0, 0, 0, 0);

        boxAnnotation.setConstraints(exampleList[classNum], annotationsConstraints);
        annotationsPanel.add(exampleList[classNum]);
        annotationsConstraints.gridy++;

        // Add listener to the new button
        addExampleButton[classNum].addActionListener(listener);

        numOfClasses++;

        // recalculate minimum size of scroll panel
        scrollPanel.setMinimumSize(labelsRightSideJPanel.getPreferredSize());

        repaintAll();
    }

    /**
     * Set the image being displayed on the custom canvas
     *
     * @param imp new image
     */
    public void setImagePlus(final ImagePlus imp) {
        super.imp = imp;
        ((FasciaMain.CustomCanvas) super.getCanvas()).setImagePlus(imp);
        Dimension dim = new Dimension(Math.min(512, imp.getWidth()), Math.min(512, imp.getHeight()));
        ((FasciaMain.CustomCanvas) super.getCanvas()).setDstDimensions(dim.width, dim.height);
        imp.setWindow(this);
        repaint();
    }

    /**
     * Enable / disable buttons
     *
     * @param s enabling flag
     */
    protected void setButtonsEnabled(boolean s) {

        overlayButton.setEnabled(s);
        loadDataButton.setEnabled(s);
        saveDataButton.setEnabled(s);
        addClassButton.setEnabled(s);
        settingsButton.setEnabled(s);


        //@ALEXIS BARLTROP
        loadROIButton.setEnabled(s);


        for (int i = 0; i < numOfClasses; i++) {
            exampleList[i].setEnabled(s);
            addExampleButton[i].setEnabled(s);
        }
        setSliceSelectorEnabled(s);
    }


    /**
     * Toggle between overlay and original image with markings
     */
    void toggleOverlay() {
        showColorOverlay = !showColorOverlay;
        //IJ.log("toggle overlay to: " + showColorOverlay);
        if (showColorOverlay && null != classifiedImage) {
            updateResultOverlay();
        } else
            resultOverlay.setImage(null);

        displayImage.updateAndDraw();
    }

    /**
     * Set a new result (classified) image
     *
     * @param classifiedImage new result image
     */
    protected void setClassfiedImage(ImagePlus classifiedImage) {
        updateClassifiedImage(classifiedImage);
    }

    /**
     * Update the buttons to add classes with current information
     */
    public void updateAddClassButtons() {
        int wekaNumOfClasses = numOfClasses;
        while (numOfClasses < wekaNumOfClasses)
            win.addClass();
        for (int i = 0; i < numOfClasses; i++)
            addExampleButton[i].setText("Add to " + LABEL_STRINGS[i]);

        repaintWindow();
    }

    /**
     * Set the flag to inform the the training has finished or not
     *
     * @param b tranining complete flag
     */
    void setTrainingComplete(boolean b) {
        this.trainingComplete = b;
    }

    /**
     * Get training image
     *
     * @return training image
     */
    public ImagePlus getTrainingImage() {
        return trainingImage;
    }
    // end class CustomWindow




    /**
     * Button listener
     */
    private ActionListener listener = new ActionListener() {

        public void actionPerformed(final ActionEvent e) {

            final String command = e.getActionCommand();

            // listen to the buttons on separate threads not to block
            // the event dispatch thread
            exec.submit(new Runnable() {

                public void run()
                {
                    if(e.getSource() == overlayButton){
                        // Macro recording
                        String[] arg = new String[] {};
                        record(TOGGLE_OVERLAY, arg);
                        win.toggleOverlay();
                    }
                    else if(e.getSource() == loadDataButton){
                        //loadTrainingData();
                    }
                    else if(e.getSource() == saveDataButton){
                        //saveTrainingData();
                    }
                    else if(e.getSource() == addClassButton){
                        //addNewClass();
                    }
                    else if(e.getSource() == settingsButton){
                        //showSettingsDialog();
                        //win.updateButtonsEnabling();
                    }

                    else if(e.getSource() == loadROIButton){

                    }

                    else{
                        for(int i = 0; i < numOfClasses; i++)
                        {
                            if(e.getSource() == addExampleButton[i])
                            {
                                final Roi r = displayImage.getRoi();
                                if (null == r)
                                    return;

                                IJ.log("Adding trace to list " + i);

                                final int n = displayImage.getCurrentSlice();
                                displayImage.killRoi();
                                String[] arg = new String[] {
                                        Integer.toString(i),
                                        Integer.toString(n)	};
                                record(ADD_TRACE, arg);
                                roiManager.addRoi(i,r);
                                break;
                            }
                        }
                    }

                }


            });
        }
    };

}
