package com.socrata.datasync.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Uses the png versions of font awesome
 *
 */
public class AwesomeButton extends JButton {

   public AwesomeButton(String icon) {
      this(icon, 24, 16);
   }

   public AwesomeButton(String icon, int width, int height) {
      try {
         Image img = ImageIO.read(getClass().getResource("/" + icon + ".png"));
         Image newimg = img.getScaledInstance( width, height, java.awt.Image.SCALE_SMOOTH ) ;
         setMargin(new Insets(0, 0, 0, 0));
         setIcon(new ImageIcon(newimg));
      } catch (IOException ex) {
         ex.printStackTrace();
      }
   }
}
