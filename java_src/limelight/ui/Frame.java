package limelight.ui;

import javax.swing.*;
import java.awt.*;

public class Frame extends JFrame
{
  public static String ICON = "???";

  private Stage stage;

  public Frame(Stage stage)
  {
    this.stage = stage;
    setLayout(null);
//    setIconImage(new ImageIcon(ICON).getImage());
//    System.out.println("System.getProperty(\"mrj.version\") = " + System.getProperty("mrj.version"));
  }

  public void doLayout()
  {
    super.doLayout();
  }

  public void close()
  {
    setVisible(false);
    dispose();
  }

  public void open()
  {
    setVisible(true);
    repaint();
  }

  public void load(Component child)
  {
    getContentPane().removeAll();
    add(child);
  }

  public Stage getStage()
  {
    return stage;
  }
}
