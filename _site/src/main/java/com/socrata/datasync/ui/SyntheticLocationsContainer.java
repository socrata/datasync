package com.socrata.datasync.ui;

import com.socrata.datasync.config.controlfile.LocationColumn;
import com.socrata.datasync.model.ControlFileModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Container for holding the synthetic locations in the control file.  Available when the customer clicks
 * "manage synthetic locations" from the Control File Editor dialog box.
 *
 * Created by franklinwilliams
 */
public class SyntheticLocationsContainer extends JPanel implements Observer {
    ControlFileModel model;

    public SyntheticLocationsContainer(ControlFileModel model){
        this.model = model;
        model.addObserver(this);
        styleComponents();
        updateComponents();
    }

    private void styleComponents(){
        this.setBorder(BorderFactory.createTitledBorder("Synthetic Locations"));
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    private JPanel getEmptyPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
        panel.setPreferredSize(new Dimension(-1, 16));
        JLabel label = new JLabel();
        label.setText("No synthetic locations configured");
        label.setHorizontalAlignment(SwingConstants.LEFT);
        label.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(Box.createRigidArea(new Dimension(10,10)));
        panel.add(label);
        JButton button = getAddButton();
        panel.add(button);
        panel.setAlignmentX(LEFT_ALIGNMENT);
        return panel;
    }

    private JButton getAddButton(){
        JButton button = UIUtility.getButtonAsLink("Add");
        button.setAlignmentX(LEFT_ALIGNMENT);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setHorizontalTextPosition(SwingConstants.LEFT);
        button.setPreferredSize(new Dimension(16, -1));
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new SyntheticLocationDialog(model, (JFrame) ((JDialog) SwingUtilities.getRoot((JButton) e.getSource())).getParent(), model.getSyntheticLocations(), null, "Manage synthetic columns");
            }
        });
        return button;
    }

    private void updateComponents(){
        this.removeAll();
        Map<String, LocationColumn> locations = model.getSyntheticLocations();
        if (locations != null && !locations.isEmpty()){
            for (String key : locations.keySet()){
                JPanel panel  = new SyntheticLocationPanel(model,key,locations.get(key));
                panel.setAlignmentX(LEFT_ALIGNMENT);
                this.add(Box.createRigidArea(new Dimension(10,5)));
                this.add(panel);
            }

            if (model.getDatasetModel().getLocationCount() > model.getSyntheticLocations().size())
                this.add(Box.createRigidArea(new Dimension(10, 5)));
                this.add(getAddButton());
        }
        else
            this.add(getEmptyPanel());
        this.revalidate();
    }


    @Override
    public void update(Observable o, Object arg) {
        updateComponents();
    }
}
