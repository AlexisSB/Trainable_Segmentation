package trainableSegmentation;

import ij.IJ;
import ij.plugin.frame.RoiManager;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class RoiManagerPanel extends JPanel {

    JList roiManagerList = null;
    DefaultListModel roiManagerListModel = null;


    public void constructRoiManagerPanel(){
        roiManagerList = new JList();
        roiManagerListModel = new DefaultListModel();
        roiManagerList.setModel(roiManagerListModel);
        JScrollPane scrollPane = new JScrollPane(roiManagerList, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add("Center", scrollPane);
        JPanel panel = new JPanel();
        int nButtons = 10;
        panel.setLayout(new GridLayout(nButtons,1,5,0));
        //JButton add = new JButton("Add");
        JButton delete = new JButton("Delete");
        JButton copyTo = new JButton("Copy To ...");
        JButton copyFrom = new JButton("Copy From ...");
        delete.addActionListener(roiManagerListener);
        copyTo.addActionListener(roiManagerListener);
        copyFrom.addActionListener(roiManagerListener);
        //panel.add(add);
        panel.add(delete);
        panel.add(copyTo);
        panel.add(copyFrom);
        add("East", panel);
    }
    private RoiManagerListener roiManagerListener = new RoiManagerListener();

    private class RoiManagerListener implements ActionListener, ListSelectionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String label = e.getActionCommand();
            if(label == null){
                return;
            }else if (label.equals("Delete")){
                int index = roiManagerList.getSelectedIndex();
                roiManagerList.remove(index);

            }else if (label.equals("Copy To ...")){
                int index = (int) IJ.getNumber("Select Slice to copy Roi to", 0);
                IJ.log("Copying to ... Slice " + index);
            }else if(label.equals("Copy From ...")){
                int index = (int) IJ.getNumber("Select Slice to copy Roi from :", 0);
                IJ.log("Copying from ... Slice " + index);
            }
        }

        @Override
        public void valueChanged(ListSelectionEvent e) {

        }
    }

}
