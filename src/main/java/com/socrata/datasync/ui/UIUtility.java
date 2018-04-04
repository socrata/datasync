package com.socrata.datasync.ui;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.BalloonTipStyle;
import net.java.balloontip.styles.ToolTipBalloonStyle;
import net.java.balloontip.utils.ToolTipUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Author: Adrian Laurenzi
 * Date: 11/27/13
 */
public class UIUtility {
    final static ImageIcon helpIcon = new ImageIcon(UIUtility.class.getResource("/help.png"));
    final static BalloonTipStyle helpBubbleStyle = new ToolTipBalloonStyle(Color.LIGHT_GRAY, Color.BLUE);
    final static int HELP_ICON_LEFT_PADDING = 5;
    final static int HELP_ICON_TOP_PADDING_DEFAULT = 0;

    private UIUtility() {
        throw new AssertionError("Never instantiate utility classes!");
    }

    public static JPanel generateLabelWithHelpBubble(String labelText, String helpBubbleText) {
        return generateLabelWithHelpBubble(labelText, helpBubbleText, HELP_ICON_TOP_PADDING_DEFAULT);
    }

    public static JPanel generateLabelWithHelpBubble(String labelText, String helpBubbleText, int topOffset) {
        JPanel labelContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, HELP_ICON_LEFT_PADDING, topOffset));
        labelContainer.add(new JLabel(labelText));
        JLabel help = new JLabel(helpIcon);
        BalloonTip chunkSizeTip = new BalloonTip(help, helpBubbleText, helpBubbleStyle, false);
        ToolTipUtils.balloonToToolTip(chunkSizeTip, 100, 100000);
        labelContainer.add(help);
        return labelContainer;
    }

    public static JLabel generateHelpBubble(String helpBubbleText) {
        JLabel help = new JLabel(helpIcon);
        BalloonTip chunkSizeTip = new BalloonTip(help, helpBubbleText, helpBubbleStyle, false);
        ToolTipUtils.balloonToToolTip(chunkSizeTip, 100, 100000);
        return help;
    }

    public static JButton getButtonAsLink(String text){
        JButton button = new JButton();
        //button.setText("<HTML><FONT color=\"#000099\"><U>"+text+"</U></FONT></HTML>");
        button.setText(text);
        button.setHorizontalAlignment(SwingConstants.RIGHT);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setForeground(Color.BLUE);
        //button.setBackground(Color.WHITE);
        return button;
    }

    public static FileNameExtensionFilter getFileChooserFilter(java.util.List<String> allowedExtensions) {
        String extensionsMsg = "";
        int numExtensions = allowedExtensions.size();
        String[] allowedFileExtensions = new String[numExtensions];
        for(int i = 0; i < numExtensions; i++) {
            if(i > 0)
                extensionsMsg += ", ";
            allowedFileExtensions[i] = allowedExtensions.get(i);
            extensionsMsg += "*." + allowedFileExtensions[i];
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                extensionsMsg, allowedFileExtensions);
        return filter;
    }

    public static GridBagConstraints getGridBagLabelConstraints(int xpos, int ypos) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = xpos;
        labelConstraints.gridy = ypos;
        labelConstraints.weightx = 1.0;
        labelConstraints.anchor = GridBagConstraints.LINE_START;
        return labelConstraints;
    }

    public static GridBagConstraints getGridBagFieldConstraints(int xpos, int ypos) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = xpos;
        constraints.gridy = ypos;
        constraints.weightx = 0;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.ipadx = 10;
        return constraints;
    }

    /**
     * Copies given text to clipboard
     * @param textToCopy text to copy to clipboard
     */
    static void copyToClipboard(String textToCopy) {
        StringSelection stringSelection = new StringSelection(textToCopy);
        Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
        clpbrd.setContents(stringSelection, null);
    }

    public static String relativize(File base, File file) {
        file = file.getAbsoluteFile();
        List<String> segments = new ArrayList<>();

        File pointer = file;
        while(pointer != null && !pointer.equals(base)) {
            segments.add(pointer.getName());
            pointer = pointer.getParentFile();
        }

        if(pointer == null) {
            return file.getAbsolutePath();
        } else if(segments.isEmpty()) {
            // we selected the current directory?  I suppose this
            // is theoretically possible in the presence of
            // filesystem changes during selection.
            return file.getAbsolutePath();
        } else {
            Collections.reverse(segments);
            Iterator<String> it = segments.iterator();
            File result = new File(it.next());
            while(it.hasNext()) {
                result = new File(result, it.next());
            }
            return result.getPath();
        }
    }
}
