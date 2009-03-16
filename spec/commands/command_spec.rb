#- Copyright � 2008-2009 8th Light, Inc. All Rights Reserved.
#- Limelight and all included source files are distributed under terms of the GNU LGPL.

require File.expand_path(File.dirname(__FILE__) + "/../spec_helper")
require 'limelight/commands/command'

describe Limelight::Commands::Command do

  before(:each) do
  end

  it "should allow subclassed to be installed" do
    myclass = class MyClass < Limelight::Commands::Command; install_as "mine"; end;

    Limelight::Commands::LISTING["mine"].should == myclass
  end

end