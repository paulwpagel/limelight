package limelight.ui.model.inputs;

import junit.framework.TestCase;

public class TextBoxPanelTest extends TestCase
{
  private TextBoxPanel panel;

  public void setUp() throws Exception
  {
    panel = new TextBoxPanel();
  }

  public void testHasJtextBox() throws Exception
  {
    assertEquals(panel.getTextBox().getClass(), TextBox.class);    
  }
  
  public void testCanBeBuffered() throws Exception
  {
    assertEquals(false, panel.canBeBuffered());
  }

}
