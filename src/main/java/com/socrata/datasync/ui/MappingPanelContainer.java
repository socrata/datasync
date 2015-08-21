package com.socrata.datasync.ui;

import com.socrata.datasync.model.CSVModel;
import com.socrata.datasync.model.ControlFileModel;
import com.socrata.datasync.model.DatasetModel;

import javax.swing.*;
import java.awt.*;
import java.util.Observable;
import java.util.Observer;

/**
 * Container which lays out individual mapping panels for all items in the CSV.  This will iterate over
 * all columns in the CSV model, creating an individual mapping panel for each of them.  The customers can
 * then use those panels to map the items in the the CSV to items in the dataset.
 *
 * It also listens for updates to the control file model, relaying the panel out if the model changes.
 *
 * Created by franklinwilliams.
 */
public class MappingPanelContainer extends JPanel implements Observer {
    ControlFileModel model;
    DatasetModel dataset;
    int lastColumnsCount;

    public MappingPanelContainer(ControlFileModel model){
        this.model = model;
        this.dataset = model.getDatasetModel();
        initializeComponents();
        layoutComponents();
        model.addObserver(this);
    }

    private void initializeComponents(){
        Dimension paddingBetweenItems = new Dimension(0,10);
        CSVModel csv = model.getCsvModel();
        this.removeAll();
        //Create a header panel
        this.add(MappingPanel.getHeaderPanel());
        //For each item in the CSV, add a panel that defines the mapping
        for (int i = 0; i < csv.getColumnCount(); i++){
            this.add(Box.createRigidArea(paddingBetweenItems));
            this.add(new MappingPanel(i,model,dataset));
        }

        this.add(Box.createRigidArea(paddingBetweenItems));
        this.revalidate();
        this.setBackground(Color.WHITE);
        lastColumnsCount = csv.getColumnCount();
    }

    private void layoutComponents(){
        this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
    }

    @Override
    public void update(Observable o, Object arg) {
        ControlFileModel model = (ControlFileModel) o;
        //Check to the if the structure has changed.  If it has, then we need to layout it out again.
        if (model.getCsvModel().getColumnCount() != lastColumnsCount)
            initializeComponents();
        else {
            //If not, then update each of the components
            for (Object obj : this.getComponents()) {
                if (obj.getClass().equals(MappingPanel.class)) {
                    MappingPanel panel = (MappingPanel) obj;
                    panel.update();
                }
            }
        }
    }
}
