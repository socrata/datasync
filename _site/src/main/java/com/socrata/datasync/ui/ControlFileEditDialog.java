package com.socrata.datasync.ui;

import com.socrata.datasync.model.ControlFileModel;

import javax.swing.*;
import java.awt.*;

/**
 * Dialog which takes a control file model and then generates all of the UI components necessarily to update that model
 *
 * Created by franklinwilliams.
 */
public class ControlFileEditDialog extends JDialog {

    ControlFileModel controlFileModel;
    private static final Dimension CONTROL_FILE_DIALOG_DIMENSIONS = new Dimension(550, 640);

    public ControlFileEditDialog(ControlFileModel controlFileModel, JFrame parent){
        super(parent,"Map Fields");
        this.controlFileModel = controlFileModel;

        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BorderLayout());

        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel,BoxLayout.Y_AXIS));

        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(1,0));

        AdvancedOptionsPanel options = new AdvancedOptionsPanel(controlFileModel);

        optionsPanel.add(options);

        JPanel infoPaneHolder = new JPanel();
        infoPaneHolder.setLayout(new BoxLayout(infoPaneHolder,BoxLayout.Y_AXIS));

        ValidationInfoPanel infoPane = new ValidationInfoPanel();
        infoPaneHolder.add(infoPane);
        infoPaneHolder.add(getIntroPanel());
        infoPaneHolder.add(new OptionsPanel(controlFileModel));
        headerPanel.add(infoPaneHolder);

        JPanel previewPanel = new JPanel();
        previewPanel.setLayout(new BoxLayout(previewPanel, BoxLayout.Y_AXIS));

        JScrollPane previewScroll = new JScrollPane();

        previewScroll.setViewportView(new MappingPanelContainer(controlFileModel));
        previewPanel.add(previewScroll);

        Dimension minSize = new Dimension(100,50);
        headerPanel.setMinimumSize(minSize);
        previewScroll.setMinimumSize(minSize);
        previewPanel.add(optionsPanel);

        ControlFileEditFooterPanel validateButtonPanel = new ControlFileEditFooterPanel(controlFileModel,infoPane);

        containerPanel.add(headerPanel,BorderLayout.NORTH);
        containerPanel.add(previewPanel, BorderLayout.CENTER);
        containerPanel.add(validateButtonPanel, BorderLayout.SOUTH);

        setMaximumSize(CONTROL_FILE_DIALOG_DIMENSIONS);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().add(containerPanel);
        pack();
        setSize(CONTROL_FILE_DIALOG_DIMENSIONS);
        setModal(true);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public JPanel getIntroPanel(){
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEmptyBorder(10,10,5,0));
        JLabel intro = new JLabel("Select how you would like to map the fields in your file.");
        panel.setLayout(new GridLayout(1,0));
        panel.add(intro);
        return panel;
    }


}



