package com.socrata.datasync.ui;

import net.java.balloontip.BalloonTip;
import net.java.balloontip.styles.BalloonTipStyle;
import net.java.balloontip.styles.ToolTipBalloonStyle;
import net.java.balloontip.utils.ToolTipUtils;

import javax.swing.*;
import java.awt.*;

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
        JLabel chunkSizeHelp = new JLabel(helpIcon);
        BalloonTip chunkSizeTip = new BalloonTip(chunkSizeHelp, helpBubbleText, helpBubbleStyle, false);
        ToolTipUtils.balloonToToolTip(chunkSizeTip, 100, 100000);
        labelContainer.add(chunkSizeHelp);
        return labelContainer;
    }

    public static JLabel generateHelpBubble(String helpBubbleText) {
        JLabel chunkSizeHelp = new JLabel(helpIcon);
        BalloonTip chunkSizeTip = new BalloonTip(chunkSizeHelp, helpBubbleText, helpBubbleStyle, false);
        ToolTipUtils.balloonToToolTip(chunkSizeTip, 100, 100000);
        return chunkSizeHelp;
    }
}
