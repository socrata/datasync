package com.socrata.datasync.ui;

import com.socrata.datasync.job.JobStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import java.awt.*;

/**
 * Created by franklinwilliams on 4/26/15.
 */
public class ValidationInfoPanel extends JPanel {

    JLabel descriptionLabel;
    JPanel container;

    public ValidationInfoPanel(){
        initializeComponents();
        layoutComponents();
    }

    public void initializeComponents(){
        container = new JPanel();
        descriptionLabel = new JLabel();
        descriptionLabel.setOpaque(false);
        displayWelcome();
    }

    public void layoutComponents(){
        this.setLayout(new GridLayout(1,0));

        container.setLayout(new BoxLayout(container, BoxLayout.X_AXIS));

        container.setAlignmentX(LEFT_ALIGNMENT);
        container.add(descriptionLabel);
        descriptionLabel.setBorder(BorderFactory.createEmptyBorder(5,5 ,2,5));
        container.setBorder(BorderFactory.createCompoundBorder(new EmptyBorder(5, 2, 5, 5), new EtchedBorder()));


        this.add(container);

        this.setVisible(false);
    }

    public void displayStatus(JobStatus status){
        if (status.isError())
            displayError(status.getMessage());
        else
            displaySuccess();
    }

    private void displayWelcome(){
        descriptionLabel.setText("");
    }

    private void displayError(String description){
        descriptionLabel.setText("<HTML>" + description.replace("\n", "<br/>") + "</HTML>");
        descriptionLabel.setForeground(new Color(122, 103, 16));
        container.setBorder(BorderFactory.createLineBorder(new Color(236, 215, 118)));
        container.setBackground(new Color(254, 248, 220));
    }

    private void displaySuccess(){
        descriptionLabel.setText("No errors found");
    }
}
