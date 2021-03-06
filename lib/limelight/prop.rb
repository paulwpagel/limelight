#- Copyright � 2008-2009 8th Light, Inc. All Rights Reserved.
#- Limelight and all included source files are distributed under terms of the GNU LGPL.

require 'limelight/java_util'
require 'limelight/pen'
require 'limelight/paint_action'
require 'limelight/animation'

module Limelight

  # Prop is the fundamental building block of a scene.  A prop represents a rectangular area in the scene, of almost any dimension.
  # It may have borders, backgrounds, margin, padding, and it may contain other props or text.  However it is the props'
  # Styles that determine their size and appearance.
  #
  # A Prop may have one parent and many children.  Hense, when put together, they form a tree structure.  The Scene is
  # the root Prop of a tree.
  #
  class Prop

    class << self

      def event(event_symbol)
        @events ||= []
        @events << event_symbol unless @events.include?(event_symbol)
        define_method(event_symbol) { |event|  } # do nothing by default
      end

      def events
        return @events
      end

      def event2(event_symbol)
        @events ||= []
        @events << event_symbol unless @events.include?(event_symbol)
        define_method("accepts_#{event_symbol}".to_sym) { return self.respond_to?(event_symbol) }
      end

    end

    include UI::Api::Prop

    attr_accessor :style, :hover_style
    attr_reader :panel #:nodoc:
    attr_reader :children, :parent, :name, :id, :players
    getters :panel, :style, :hover_style, :name, :scene, :loader #:nodoc:

    # When creating a Prop, an optional Hash is accepted. These are called initialization options.
    # The key/value pairs in the initialiaztion options will be used to
    # set properties on the Prop, it Style, or included Player properties. These properties are not set
    # until the prop is added to a Prop tree with a Scene.
    #
    def initialize(hash = {})
      @options = hash || {}
      @children = []
      @style = Styles::ScreenableStyle.new
      @panel = UI::Model::Panel.new(self)
    end

    # Add a Prop as a child of this Prop.
    #
    def add(child)
      child.set_parent(self)
      @children << child
      @panel.add(child.panel)
    end

    # Same as add.  Returns self so adding may be chained.
    #
    #   prop << child1 << child2 << child3
    #
    def <<(child)
      add(child)
      return self
    end

    # Allows the adding of child Props using the PropBuilder DSL.
    #
    #    prop.build do
    #      child1 do
    #        grand_child
    #      end
    #      child2
    #    end
    #
    def build(options = {}, &block)
      require 'limelight/dsl/prop_builder'
      builder = Limelight::DSL::PropBuilder.new(self)
      builder.__install_instance_variables(options)
      builder.__loader__ = scene.loader
      builder.instance_eval(&block)
    end

    # Removes a child Prop.  The child Prop will be parentless after removal.
    #
    def remove(child)
      if children.delete(child)
        scene.unindex_prop(child) if scene
        @panel.remove(child.panel)
      end
    end

    # Removes all child Props.
    #
    def remove_all
      @panel.remove_all
      @children.each { |child| scene.unindex_prop(child) } if scene
      @children = []
    end

    # Injects the behavior of the specified Player into the Prop.  The Player must be a Module.
    #
    def include_player(player_module)
      unless self.is_a?(player_module)
        extend player_module
        self.casted if player_module.instance_methods.include?("casted")
      end 
    end

    def update #:nodoc:
      return if (scene.nil? || !scene.visible)
      @panel.doLayout
      @panel.repaint
    end

    def update_now #:nodoc:
      return if (scene.nil? || !scene.visible)
      @panel.doLayout()
      @panel.paintImmediately(0, 0, @panel.width, @panel.height)
    end

    # A hook to invoke behavior after a Prop is painted.
    #
    def after_painting(flag = true, &block)
      if flag
        @panel.after_paint_action = PaintAction.new(&block)
      else
        @panel.after_paint_action = nil
      end
    end

    # Searches all descendant of the Prop (including itself) for Props with the specified name.
    # Returns an Array of matching Props. Returns an empty Array if none are found.
    #
    def find_by_name(name, results = [])
      results << self if @name == name
      @children.each { |child| child.find_by_name(name, results) }
      return results
    end

    # Sets the text of this Prop.  If a prop is given text, it will become sterilized (it may not have any more children).
    # Some Players such as text_box, will cause the text to appear in the text_box.
    #
    def text=(value)    
      @panel.text = value.to_s
    end

    # Returns the text of the Prop.
    #
    def text
      return panel.text
    end

    # Returns the scene to which this prop belongs to.
    #
    def scene 
      return nil if @parent.nil?
      @scene = @parent.scene if @scene.nil?
      return @scene
    end

    # TODO get rid of me.... The Java Prop interface declares this method.
    def loader
      return scene.production.root;
    end

    # Returns the current Production this Prop lives in.
    #
    def production
      return scene.production
    end

    def to_s #:nodoc:
      return "#{self.class.name}[id: #{@id}, name: #{@name}]"
    end

    def inspect #:nodoc:
      return self.to_s
    end

    # unusual name because it's not part of public api
    def set_parent(parent) #:nodoc:
      @parent = parent
      if @parent.illuminated?
        illuminate
      end
    end

    # Allows the addition of extra initialization options.  Will raise an exception if the Prop has already been
    # illuminated (added to a scene).
    #
    def add_options(more_options)
      raise "Too late to add options" if illuminated?
      @options.merge!(more_options)
    end

    # Returns a Box representing the relative bounds of the Prop. Is useful with usign the Pen.
    #
    #   box = prop.area
    #   box.x, box.y # represents the Prop's location within its parent Prop
    #   box.width, box.height # represents the Prop's dimensions
    #
    def area
      return panel.get_bounding_box.clone
    end

    # Returns a Box representing the bounds inside the borders of the prop.  If the Prop's style has no margin or
    # border_width, then this will be equivalant to area.
    #
    def bordered_area
      return panel.get_box_inside_borders.clone
    end

    # Returns a Box representing the bounds inside the padding of the prop.  This is the area where child props may
    # be located
    #
    def child_area
      return panel.getChildConsumableArea().clone
    end

    # Returns a Pen object. Pen objects allow to you to draw directly on the screen, withing to bounds of this Prop.
    #
    def pen
      return Pen.new(panel.getGraphics)
    end

    # Initiate an animation loop.  Options may include :name (string), :updates_per_second (int: defaults to 60)
    # An Animation object is returned.
    # The provided block will be invoked :updates_per_second times per second until the Animation is stopped.
    #
    #    @animation = prop.animate(:updates_per_second => 20) do
    #      prop.style.border_width = (prop.style.top_border_width.to_i + 1).to_s
    #      @animation.stop if prop.style.top_border_width.to_i > 60
    #    end
    #
    # This above example will cause the Prop's border to grow until it is 60 pixels wide.
    #
    def animate(options={}, &block)
      animation = Animation.new(self, block, options)
      animation.start
      return animation
    end

    # Plays a sound on the computers audio output.  The parameter is the filename of a .au sound file.
    # This filename should relative to the root directory of the current Production, or an absolute path.
    #
    def play_sound(filename)
      @panel.play_sound(scene.loader.path_to(filename))
    end

    # Luanches the spcified URL using the OS's default handlers. For example, opening a URL in a browser:
    #
    #   launch('http://www.google.com')
    #
    # To create a link prop add an accessor on the player (say url) and use that in the prop definition
    # Ex:
    #
    #   link :text => "I am a link", :url => "http://www.8thlight.com"
    def launch(url)
      Context.instance.os.launch(url)
    end

    # GUI Events ##########################################

    # TODO MDM - This may be very inefficient.  If seems like these methods are generated for each instance of prop.
    event2 :mouse_clicked
    event :mouse_entered
    event :mouse_exited
    event2 :mouse_pressed
    event2 :mouse_released
    event :mouse_dragged
    event :mouse_moved
    event :key_typed
    event :key_pressed
    event :key_released
    event :focus_gained
    event :focus_lost
    event :button_pressed
    event :value_changed

    # TODO Try to get me out of public scope
    #
    def illuminate #:nodoc:
      if illuminated?
        scene.index_prop(self) if @id
      else
        set_id(@options.delete(:id))
        @name = @options.delete(:name)
        @players = @options.delete(:players)
        @additional_styles = @options.delete(:styles)

        inherit_styles
        scene.casting_director.fill_cast(self)
        apply_options

        @options = nil
      end
      
      children.each do |child|
        child.illuminate
      end
    end

    def illuminated? #:nodoc:
      return @options.nil?
    end  

    private ###############################################

    def set_id(id)
      return if id.nil? || id.to_s.empty?
      @id = id.to_s
      scene.index_prop(self)
    end

    def apply_options
      @options.each_pair do |key, value|
        setter_sym = "#{key.to_s}=".to_sym
        if self.respond_to?(setter_sym)
          self.send(setter_sym, value)
        elsif self.style.respond_to?(setter_sym)
          self.style.send(setter_sym, value.to_s)
        elsif is_event_setter(key)
          define_event(key, value)
        end
      end
    end

    def is_event_setter(symbol)
      string_value = symbol.to_s
      return string_value[0..2] == "on_" && self.class.events.include?(string_value[3..-1].to_sym)
    end

    def define_event(symbol, value)
      event_name = symbol.to_s[3..-1]
      self.instance_eval "def #{event_name}(event); #{value}; end"
    end

    def inherit_styles
      style_names = []
      style_names << @name unless @name.nil?
      style_names += @additional_styles.gsub(',', ' ').split(' ') unless @additional_styles.nil?   
      style_names.each do |style_name|    
        new_style = scene.styles[style_name]
        @style.add_extension(new_style) if new_style
        new_hover_style = scene.styles["#{style_name}.hover"]
        if new_hover_style
          if @hover_style.nil?
            @hover_style = new_hover_style
          else
            @hover_style.add_extension(new_hover_style)
          end
        end
      end
    end

  end
end