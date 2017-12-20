package com.socrata.datasync.ui;

import com.socrata.datasync.config.controlfile.SyntheticPointColumn;
import com.socrata.datasync.config.controlfile.GeocodedPointColumn;
import com.socrata.datasync.model.ControlFileModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * Container for holding the synthetic points in the control file.  Available when the customer clicks
 * "manage synthetic points" from the Control File Editor dialog box on an NBE dataset.
 */
public class SyntheticPointsContainer extends JPanel implements Observer {
    ControlFileModel model;

    public SyntheticPointsContainer(ControlFileModel model){
        this.model = model;
        model.addObserver(this);
        styleComponents();
        updateComponents();
    }

    private void styleComponents(){
        this.setBorder(BorderFactory.createTitledBorder("Synthetic Points"));
        this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    private JPanel getEmptyPanel(){
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel,BoxLayout.X_AXIS));
        panel.setPreferredSize(new Dimension(-1, 16));
        JLabel label = new JLabel();
        label.setText("No synthetic points configured");
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
                JDialog dialog = SyntheticPointDialog.create(model, (JFrame) ((JDialog) SwingUtilities.getRoot((JButton) e.getSource())).getParent(), model.getSyntheticPoints());
            }
        });
        return button;
    }

    private void updateComponents(){
        this.removeAll();
        Map<String, SyntheticPointColumn> locations = model.getSyntheticPoints();
        if (locations != null && !locations.isEmpty()){
            for (String key : locations.keySet()){
                JPanel panel  = new SyntheticPointPanel(model, key, locations.get(key));
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
