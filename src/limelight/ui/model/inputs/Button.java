package limelight.ui.model.inputs;

import limelight.ui.model.updates.Updates;
import limelight.ui.model.updates.BoundedPaintUpdate;

import javax.swing.*;
import java.awt.*;

public class Button extends JButton
{
  private ButtonPanel panel;

  public Button(ButtonPanel panel)
  {
    this.panel = panel;
  }

  public void repaint()
  {
    if(panel != null)
      panel.setNeededUpdate(Updates.paintUpdate);
  }

  public void repaint(long tm, int x, int y, int width, int height)
  {
    if(panel != null)
      panel.setNeededUpdate(new BoundedPaintUpdate(x, y, width, height));
  }

  public void repaint(Rectangle r)
  {
    if(panel != null)
      panel.setNeededUpdate(new BoundedPaintUpdate(r));
  }

  public boolean isShowing()
  {
    return true;
  }
}
