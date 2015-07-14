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
                // If all of the columns in the dataset aren't mapped, then provide the user with the option to
                // ignore the unmapped ones
                if (status == JobStatus.MISSING_COLUMNS){
                    ArrayList<Column> unmappedColumns = model.getUnmappedDatasetColumns();
                    int result = showIgnoreColumnsDialogBox(unmappedColumns);
                    if (result == JOptionPane.YES_OPTION){
                        //Ignore all of the unmapped columns
                        for (Column unmapped : unmappedColumns) {
                            model.ignoreColumnFromDataset(unmapped);
                        }
                        // Validate the model again in case there are other errors hiding behind this one
                        status = model.validate();
                    }
                }

                // If the job is still in an error state, then show the error to the user, and drop them back in the
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

    private int showIgnoreColumnsDialogBox(ArrayList<Column> unmappedColumns){
        Object[] options = {"Ignore columns", "Cancel"};
        return JOptionPane.showOptionDialog(null,
                getIgnoreColumnMessage(unmappedColumns),
                "Unmapped columns",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null, options, options[0]
        );
    }

    private String getIgnoreColumnMessage(ArrayList<Column> unmappedColumns){

        StringBuffer message = new StringBuffer("<HTML>The following columns exist in your dataset, but are not currently mapped:<br/>");
        for (Column column : unmappedColumns){
            message.append("<br/>   \u2022 " + getFriendlyColumnString(column));
        }
        message.append("<br/><br/>Would you like to ignore the columns (note that ignored columns will end up with \"null\" values)?");
        message.append("</HTML>");
        return message.toString();
    }

    private String getFriendlyColumnString(Column column){
        return column.getName() + " (" + column.getFieldName() + ")";
    }

}
