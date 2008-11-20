package limelight.styles.styling;

import limelight.styles.abstrstyling.StyleAttributeCompiler;
import limelight.styles.abstrstyling.StyleAttribute;
import limelight.styles.abstrstyling.DimensionAttribute;

public class DimensionAttributeCompiler extends StyleAttributeCompiler
{
  public StyleAttribute compile(Object objValue)
  {
    String value = objValue.toString();
    try
    {
      DimensionAttribute attribute;
      attribute = attemptAutoAttribute(value);
      if(attribute == null)
        attribute = attemptPercentageAttribute(value);
      if(attribute == null)
        attribute = attemptStaticAttribute(value);

      if(attribute != null)
        return attribute;
      else
        throw makeError(value);
    }
    catch(Exception e)
    {
      throw makeError(value);
    }
  }

  private DimensionAttribute attemptStaticAttribute(String value)
  {
    int intValue = IntegerAttributeCompiler.convertToInt(value);

    if(intValue >= 0)
      return new StaticDimensionAttribute(intValue);
    else
      return null;
  }

  private DimensionAttribute attemptAutoAttribute(String value)
  {
    if("auto".equals(value.toLowerCase()))
      return new AutoDimensionAttribute();
    else
      return null;
  }

  private DimensionAttribute attemptPercentageAttribute(String value)
  {
    if(value.indexOf('%') != -1)
    {
      double percentValue = PercentageAttributeCompiler.convertToDouble(value);
      if(percentValue >= 0)
        return new PercentageDimensionAttribute(percentValue);
    }
    return null;
  }
}
