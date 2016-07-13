package com.socrata.datasync.ui;

import com.socrata.datasync.config.controlfile.LocationColumn;
import com.socrata.datasync.model.ControlFileModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by franklinwilliams on 5/17/15.
 */
public class SyntheticLocationPanel extends JPanel {

    String fieldName;
    LocationColumn locationColumn;
    ControlFileModel model;

    public SyntheticLocationPanel(ControlFileModel model, String fieldName, LocationColumn column){
        this.model = model;
        this.fieldName = fieldName;
        this.locationColumn = column;
        initComponents();
    }

    private void initComponents(){
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        JLabel label = new JLabel();
        label.setText("" + getSyntheticMappedColumns() + " " + '\u2192' + " " +  fieldName);
        JButton manage = UIUtility.getButtonAsLink("Manage");
        JButton remove = UIUtility.getButtonAsLink("Remove");


        manage.setPreferredSize(new Dimension(100,16));
        manage.setMaximumSize(new Dimension(100, 16));
        remove.setPreferredSize(new Dimension(100, 16));
        remove.setMaximumSize(new Dimension(100, 16));

        manage.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JDialog dialog = new SyntheticLocationDialog(model,(JFrame) ((JDialog)SwingUtilities.getRoot((JButton) e.getSource())).getParent(),model.getSyntheticLocations(),fieldName,"Manage synthetic columns");
            }
        });

        remove.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                model.removeSyntheticColumn(fieldName);
            }
        });
        label.setAlignmentX(LEFT_ALIGNMENT);
        manage.setAlignmentX(LEFT_ALIGNMENT);
        remove.setAlignmentX(LEFT_ALIGNMENT);

        label.setHorizontalAlignment(SwingConstants.LEFT);
        manage.setHorizontalAlignment(SwingConstants.LEFT);
        remove.setHorizontalAlignment(SwingConstants.LEFT);

        this.add(label);
        this.add(manage);
        this.add(remove);
    }

    private String getSyntheticMappedColumns(){
        StringBuffer buffer = new StringBuffer();
      //  buffer.append("Creating \"" + fieldName + "\" from ");
        Map<String ,String> components = locationColumn.findComponentColumns();
        Collection<String> values = components.values();
        Iterator<String> it = values.iterator();
        while (it.hasNext()){

            String component = it.next();
            //TODO: Ugh. Have the underlying name.  Going to get the index and then the friendly name.  Clean this up.
            int index = model.getIndexOfColumnName(component);
            String displayName = model.getDisplayName(index);
            buffer.append(displayName);
            if (it.hasNext())
                buffer.append(", ");
        }
        return buffer.toString();
    }

}
