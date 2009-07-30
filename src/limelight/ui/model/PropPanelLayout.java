//- Copyright � 2008-2009 8th Light, Inc. All Rights Reserved.
//- Limelight and all included source files are distributed under terms of the GNU LGPL.

package limelight.ui.model;

import limelight.styles.Style;
import limelight.ui.Panel;
import limelight.ui.model.inputs.ScrollBarPanel;
import limelight.util.Aligner;
import limelight.util.Box;

import java.util.LinkedList;
import java.util.List;
import java.awt.*;

public class PropPanelLayout
{
  public static PropPanelLayout instance = new PropPanelLayout();

  // TODO MDM This gets called ALOT!  Possible speed up by re-using objects, rather then reallocating them. (rows list, rows)
  public void doLayout(PropPanel panel)
  {
    panel.layoutStarted();
    panel.doFloatLayout();
    Style style = panel.getStyle();

    if(panel.sizeChanged() || style.hasPercentageDimension() || style.hasAutoDimension())
      panel.snapToSize();

    establishScrollBars(panel);

    Dimension consumedDimensions = new Dimension();
    if(!hasNonScrollBarChildren(panel))
      collapseAutoDimensions(panel, consumedDimensions);
    else
    {
      doLayoutOnChildren(panel);
      LinkedList<Row> rows = buildRows(panel);
      calculateConsumedDimentions(rows, consumedDimensions);
      collapseAutoDimensions(panel, consumedDimensions);
      layoutRows(panel, consumedDimensions, rows);
    }
    layoutScrollBars(panel, consumedDimensions);

    panel.updateBorder();
    panel.markAsDirty();
    panel.wasLaidOut();
  }

  private boolean hasNonScrollBarChildren(PropPanel panel)
  {
    List<Panel> children = panel.getChildren();
    if(children.size() == 0)
      return false;
    else if(children.size() == 1)
      return !(children.contains(panel.getVerticalScrollBar()) || children.contains(panel.getHorizontalScrollBar()));
    else if(children.size() == 2)
      return !(children.contains(panel.getVerticalScrollBar()) && children.contains(panel.getHorizontalScrollBar()));
    else
      return true;
  }

  private void layoutScrollBars(PropPanel panel, Dimension consumedDimensions)
  {
    ScrollBarPanel vertical = panel.getVerticalScrollBar();
    ScrollBarPanel horizontal = panel.getHorizontalScrollBar();
    Box area = panel.getChildConsumableArea();
    if(vertical != null)
    {
      vertical.setHeight(area.height);
      vertical.setLocation(area.right() + 1, area.y);
      vertical.configure(area.height, consumedDimensions.height);
    }
    if(horizontal != null)
    {
      horizontal.setWidth(area.width);
      horizontal.setLocation(area.x, area.bottom() + 1);
      horizontal.configure(area.width, consumedDimensions.width);
    }
  }

  private void collapseAutoDimensions(PropPanel panel, Dimension consumedDimensions)
  {
    Style style = panel.getStyle();

    int width = style.getCompiledWidth().collapseExcess(panel.getWidth(), consumedDimensions.width + horizontalInsets(panel), style.getCompiledMinWidth(), style.getCompiledMaxWidth());
    int height = style.getCompiledHeight().collapseExcess(panel.getHeight(), consumedDimensions.height + verticalInsets(panel), style.getCompiledMinHeight(), style.getCompiledMaxHeight());

    panel.setSize(width, height);
  }

  public int horizontalInsets(PropPanel panel)
  {
    return panel.getBoundingBox().width - panel.getBoxInsidePadding().width;
  }

  public int verticalInsets(PropPanel panel)
  {
    return panel.getBoundingBox().height - panel.getBoxInsidePadding().height;
  }

  protected void doLayoutOnChildren(PropPanel panel)
  {
    for(Panel child : panel.getChildren())
    {
      if(child.needsLayout())
      {
        child.doLayout();
      }
    }
  }

  public void layoutRows(PropPanel panel, Dimension consumeDimension, LinkedList<Row> rows)
  {
    Aligner aligner = buildAligner(panel);
    aligner.addConsumedHeight(consumeDimension.height);
    int y = aligner.startingY();
    if(panel.getVerticalScrollBar() != null)
      y -= panel.getVerticalScrollBar().getValue();
    for(Row row : rows)
    {
      int x = aligner.startingX(row.width);
      if(panel.getHorizontalScrollBar() != null)
        x -= panel.getHorizontalScrollBar().getValue();
      row.layoutComponents(x, y);
      y += row.height;
    }
  }

  protected LinkedList<Row> buildRows(PropPanel panel)
  {
    LinkedList<Row> rows = new LinkedList<Row>();
    Row currentRow = newRow(panel, rows);

    for(Panel child : panel.getChildren())
    {
      if(child instanceof ScrollBarPanel)
        ;//ignore
      else if(!child.isFloater())
      {
        if(!currentRow.isEmpty() && !currentRow.fits(child))
          currentRow = newRow(panel, rows);
        currentRow.add(child);
      }
    }
    return rows;
  }

  protected Aligner buildAligner(PropPanel panel)
  {
    Style style = panel.getProp().getStyle();
    return new Aligner(panel.getChildConsumableArea(), style.getCompiledHorizontalAlignment().getAlignment(), style.getCompiledVerticalAlignment().getAlignment());
  }

  private Row newRow(PropPanel panel, LinkedList<Row> rows)
  {
    Row currentRow = new Row(panel.getChildConsumableArea().width);
    rows.add(currentRow);
    return currentRow;
  }

  private void calculateConsumedDimentions(LinkedList<Row> rows, Dimension consumedDimensions)
  {
    for(Row row : rows)
    {
      consumedDimensions.height += row.height;
      if(row.width > consumedDimensions.width)
        consumedDimensions.width = row.width;
    }
  }

  public void establishScrollBars(PropPanel panel)
  {
    Style style = panel.getStyle();
    if(panel.getVerticalScrollBar() == null && style.getCompiledVerticalScrollbar().isOn())
      panel.addVerticalScrollBar();
    else if(panel.getVerticalScrollBar() != null && style.getCompiledVerticalScrollbar().isOff())
      panel.removeVerticalScrollBar();
    if(panel.getHorizontalScrollBar() == null && style.getCompiledHorizontalScrollbar().isOn())
      panel.addHorizontalScrollBar();
    else if(panel.getHorizontalScrollBar() != null && style.getCompiledHorizontalScrollbar().isOff())
      panel.removeHorizontalScrollBar();

  }

  private class Row
  {
    private final LinkedList<Panel> items;
    private final int maxWidth;
    public int width;
    public int height;

    public Row(int maxWidth)
    {
      this.maxWidth = maxWidth;
      width = 0;
      height = 0;
      items = new LinkedList<Panel>();
    }

    public void add(Panel panel)
    {
      items.add(panel);
      width += panel.getWidth();
      if(panel.getHeight() > height)
        height = panel.getHeight();
    }

    public boolean isEmpty()
    {
      return items.size() == 0;
    }

    public boolean fits(Panel panel)
    {
      return (width + panel.getWidth()) <= maxWidth;
    }

    public void layoutComponents(int x, int y)
    {
      for(Panel panel : items)
      {
        panel.setLocation(x, y);
        x += panel.getWidth();
      }
    }
  }
}

