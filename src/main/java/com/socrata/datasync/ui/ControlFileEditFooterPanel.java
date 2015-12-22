package com.socrata.datasync.ui;

import com.socrata.datasync.job.JobStatus;
import com.socrata.datasync.model.ControlFileModel;
import com.socrata.model.importer.Column;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * Footer panel for the ControlFile editor.  When the user confirms their
 * updates it calls validate on the control file, either terminating if there are no errors, or calling back into the
 * another class to display the error.
 *
 * Created by franklinwilliams
 */
public class ControlFileEditFooterPanel extends JPanel {
    ControlFileModel model;
    ValidationInfoPanel validationPanel;

    public ControlFileEditFooterPanel(ControlFileModel controlFileModel, ValidationInfoPanel validationPanel){
        model = controlFileModel;
        this.validationPanel = validationPanel;
        setLayout(new FlowLayout(FlowLayout.RIGHT));
        layoutComponents();
    }

    public void layoutComponents(){
        JButton okButton = new JButton("OK");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JobStatus status = model.validate();

                // If the job is in an error state, then show the error to the user, and drop them back in the
                // control file editor to fix it.  Otherwise, close the window.
                if (status.isError()) {
                    validationPanel.displayStatus(status);
                    validationPanel.setVisible(true);
                }
                else {
                    JDialog topFrame = (JDialog) SwingUtilities.getWindowAncestor((JButton) e.getSource());
                    topFrame.dispose();
                }
            }
        });
        add(okButton);
    }

}
