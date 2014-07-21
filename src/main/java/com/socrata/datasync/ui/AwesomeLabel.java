package com.socrata.datasync.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Uses the png versions of font awesome
 */
public class AwesomeLabel extends JLabel {

   public AwesomeLabel(String icon) {
      this(icon, 18, 13);
   }

   public AwesomeLabel(String icon, int width, int height) {
      try {
         Image img = ImageIO.read(getClass().getResource("/awesome/png/" + icon + ".png"));
         Image newimg = img.getScaledInstance( width, height, java.awt.Image.SCALE_SMOOTH ) ;
         setIcon(new ImageIcon(newimg));
      } catch (IOException ex) {
         ex.printStackTrace();
      }
   }
}
