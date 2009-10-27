package limelight.ui.api;

public class UtilitiesProduction implements Production
{
  private Production production;

  public UtilitiesProduction(Production production)
  {
    this.production = production;
  }

  public String getName()
  {
    return production.getName();
  }

  public void setName(String name)
  {
    production.setName(name);
  }

  public boolean allowClose()
  {
    return production.allowClose();
  }

  public void close()
  {
    production.close();
  }

  public Object callMethod(String name, Object... args)
  {
    return production.callMethod(name, args);
  }

  public Object alert(String message)
  {
    return production.callMethod("alert", message);
  }

  public Object shouldProceedWithIncompatibleVersion(String name, String version)
  {
    return production.callMethod("proceed_with_incompatible_version?", name, version);
  }

  public Production getProduction()
  {
    return production;
  }
}