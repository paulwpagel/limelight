#- Copyright 2008 8th Light, Inc.
#- Limelight and all included source files are distributed under terms of the GNU LGPL.

module Cell
  
  def self.extended(prop)
  end
  
  def mouse_clicked(e)
    puts "mouse clicked"
    p = parent
    parent.remove(self)
  end
  
end