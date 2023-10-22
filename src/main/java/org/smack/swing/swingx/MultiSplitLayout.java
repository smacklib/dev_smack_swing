/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2003-2023 Michael Binz
 */
package org.smack.swing.swingx;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JSplitPane;

import org.smack.swing.beans.AbstractBean;
import org.smack.swing.beans.JavaBeanProperty;
import org.smack.util.JavaUtil;
import org.smack.util.MathUtil;
import org.smack.util.StringUtil;


/**
 * The MultiSplitLayout layout manager recursively arranges its
 * components in row and column groups called "Splits".  Elements of
 * the layout are separated by gaps called "Dividers".  The overall
 * layout is defined with a simple tree model whose nodes are
 * instances of MultiSplitLayout.Split, MultiSplitLayout.Divider,
 * and MultiSplitLayout.Leaf. Named Leaf nodes represent the space
 * allocated to a component that was added with a constraint that
 * matches the Leaf's name.  Extra space is distributed
 * among row/column siblings according to their 0.0 to 1.0 weight.
 * If no weights are specified then the last sibling always gets
 * all of the extra space, or space reduction.
 *
 * <p>
 * Although MultiSplitLayout can be used with any Container, it's
 * the default layout manager for MultiSplitPane.  MultiSplitPane
 * supports interactively dragging the Dividers, accessibility,
 * and other features associated with split panes.
 *
 * <p>
 * All properties in this class are bound: when a properties value
 * is changed, all PropertyChangeListeners are fired.
 *
 * @author Michael Binz
 * @see JXMultiSplitPane
 */

@SuppressWarnings("serial")
public class MultiSplitLayout
    extends
        AbstractBean
    implements
        LayoutManager, Serializable
{
    private static Logger LOG = Logger.getLogger( MultiSplitLayout.class.getName() );

    public enum LayoutMode { DEFAULT_LAYOUT, NO_MIN_SIZE_LAYOUT, USER_MIN_SIZE_LAYOUT };

    /**
     * A map holding the component-to-name mappings.
     */
    private final Map<String, Component> _childMap =
            new HashMap<String, Component>();
    private final Map<String, LeafImpl> _leafMap =
            new HashMap<>();

    private JavaBeanProperty<SplitImpl, MultiSplitLayout> _model =
            new JavaBeanProperty<>(
                    this,
                    null, //ew LeafImpl( "default" ),
                    "model" );

    private JavaBeanProperty<Integer, MultiSplitLayout> _dividerSize =
            new JavaBeanProperty<>(
                    this,
                    new JSplitPane().getDividerSize(),
                    "dividerSize" );

    private JavaBeanProperty<Integer, MultiSplitLayout> _minimumExtent =
            new JavaBeanProperty<>(
                    this,
                    20,
                    "minimumExtent" );

    private LayoutMode layoutMode;

    /**
     * Create a MultiSplitLayout with a default model with a single
     * Leaf node named "default".
     *
     * #see setModel
     */
    public MultiSplitLayout()
    {
    }

    /**
     * Create a MultiSplitLayout with the specified model.
     *
     * #see setModel
     */
    public MultiSplitLayout(SplitImpl model)
    {
        setModel( model );
    }

    /**
     * Get the component associated with a MultiSplitLayout.Node
     * @param n the layout node
     * @return the component handled by the layout or null if not found
     */
    private Component getComponentForNode( NodeImpl n )
    {
        String name = ((LeafImpl)n).getName();
        return (name != null) ? (Component)_childMap.get(name) : null;
    }

    /**
     * Get the MultiSplitLayout.Node associated with a component
     * @param name the name used to associate a component with the layout
     * @return the node associated with the component
     */
    LeafImpl getNodeForName( String name )
    {
        return _leafMap.get( name );
    }

    /**
     * Get the name used to map a component
     * @param child the component
     * @return the name used to map the component or null if no mapping is found
     */
    public String getNameForComponent( Component child )
    {
        String name = null;
        for(Map.Entry<String,Component> kv : _childMap.entrySet()) {
            if (kv.getValue() == child) {
                name = kv.getKey();
                break;
            }
        }

        return name;
    }

    /**
     * Return the root of the tree of Split, Leaf, and Divider nodes
     * that define this layout.
     *
     * @return the value of the model property
     * @see #setModel
     */
    public SplitImpl getModel()
    {
        return _model.get();
    }

    /**
     * Set the model root.
     *
     * @param model The model root.
     * @see #getModel
     */
    public void setModel(SplitImpl model)
    {
        Objects.requireNonNull( model );

        model.attach( this );

        _model.set( model );
    }

    public void setModel2(Split model)
    {
        _model.set( model.convert( this ) );
    }
    public Split getModel2()
    {
        return (Split)_model.get().convert();
    }

    /**
     * Returns the width of Dividers in Split rows, and the height of
     * Dividers in Split columns.
     *
     * @return the value of the dividerSize property
     * @see #setDividerSize
     */
    public int getDividerSize()
    {
        return _dividerSize.get();
    }

    /**
     * Sets the width of Dividers in Split rows, and the height of
     * Dividers in Split columns.
     *
     * @param dividerSize the size of dividers (pixels)
     * @throws IllegalArgumentException if dividerSize < 0
     * @see #getDividerSize
     */
    public void setDividerSize(int dividerSize)
    {
        if (dividerSize <= 0)
            throw new IllegalArgumentException("invalid dividerSize");

        _dividerSize.set( dividerSize );
    }



    /**
     * Add a component to this MultiSplitLayout.  The
     * <code>name</code> should match the name property of the Leaf
     * node that represents the bounds of <code>child</code>.  After
     * layoutContainer() recomputes the bounds of all of the nodes in
     * the model, it will set this child's bounds to the bounds of the
     * Leaf node with <code>name</code>.  Note: if a component was already
     * added with the same name, this method does not remove it from
     * its parent.
     *
     * @param name identifies the Leaf node that defines the child's bounds
     * @param child the component to be added
     * @see #removeLayoutComponent
     */
    @Override
    public void addLayoutComponent(String name, Component child) {
        if (name == null) {
            throw new IllegalArgumentException("name not specified");
        }
        _childMap.put(name, child);
    }

    /**
     * Removes the specified component from the layout.
     *
     * @param child the component to be removed
     * @see #addLayoutComponent
     */
    @Override
    public void removeLayoutComponent(Component child) {
        String name = getNameForComponent(
                Objects.requireNonNull( child ) );

        if ( name != null ) {
            _childMap.remove( name );
        }
    }

    private Component childForNode(NodeImpl node) {
        if (node instanceof LeafImpl) {
            LeafImpl leaf = (LeafImpl)node;
            String name = leaf.getName();
            return (name != null) ? _childMap.get(name) : null;
        }
        return null;
    }

    private Dimension preferredComponentSize(NodeImpl node) {
        if ( layoutMode == LayoutMode.NO_MIN_SIZE_LAYOUT )
            return new Dimension(0, 0);

        Component child = childForNode(node);
        return ((child != null) && child.isVisible() ) ? child.getPreferredSize() : new Dimension(0, 0);
    }

    /**
     * Not supported.
     */
    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return new Dimension( Integer.MAX_VALUE, Integer.MAX_VALUE );
    }

    /**
     * Not supported.
     */
    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return preferredLayoutSize(parent);
    }

    /**
     * Get the layout mode
     * @return current layout mode
     */
    @Deprecated
    public LayoutMode getLayoutMode()
    {
        return layoutMode;
    }

    /**
     * Set the layout mode. By default this layout uses the preferred and minimum
     * sizes of the child components. To ignore the minimum size set the layout
     * mode to MultiSplitLayout.LAYOUT_NO_MIN_SIZE.
     * @param layoutMode the layout mode
     * <ul>
     * <li>DEFAULT_LAYOUT - use the preferred and minimum sizes when sizing the children</li>
     * <li>LAYOUT_NO_MIN_SIZE - ignore the minimum size when sizing the children</li>
     * </li>
     */
    @Deprecated
    public void setLayoutMode( LayoutMode layoutMode )
    {
        this.layoutMode = layoutMode;
    }

    /**
     * Get the minimum node size
     * @return the minimum size
     */
    public int getMinimumExtent()
    {
        return _minimumExtent.get();
    }

    /**
     * Set the user defined minimum size support in the USER_MIN_SIZE_LAYOUT
     * layout mode.
     * @param minSize the new minimum size
     */
    public void setMinimumExtent( int minSize )
    {
        _minimumExtent.set( minSize );
    }

    /**
     * The specified Node is either the wrong type or was configured
     * incorrectly.
     */
    public static class InvalidLayoutException extends RuntimeException {
        private final NodeImpl node;
        public InvalidLayoutException(String msg, NodeImpl node) {
            super(msg);
            this.node = node;
        }
        /**
         * @return the invalid Node.
         */
        public NodeImpl getNode() { return node; }
    }

    private static void throwInvalidLayout(String msg, NodeImpl node) {
        throw new InvalidLayoutException(msg, node);
    }

    private void validateLayout( NodeImpl root )
    {
        root.validate( new HashSet<>() );
    }

    /**
     *
     * @param node The node to layout.
     * @param bounds
     */
    private void _performLayout( NodeImpl node, Rectangle bounds )
    {
        node.setBounds( bounds );

        node.layout( bounds );
    }

    private static List<NodeImpl> _completeWeights( List<NodeImpl> children )
    {
        double[] weights = new double[children.size()];
        for ( int i = 0 ; i < weights.length ; i++ )
        {
            var c = children.get( i );
                weights[i] = c.getWeight();
        }

        double unsetCount = 0.0;
        double setPercentage = 0.0;
        for ( var c : weights )
        {
            if ( c == 0.0 )
                unsetCount += 1.0;
            else if ( c > 0.0 )
                setPercentage += c;
        }

        JavaUtil.Assert( setPercentage <= 1.0 );
        JavaUtil.Assert( setPercentage >= 0.0 );

        if ( unsetCount == 0.0 && setPercentage == 1.0 )
            return children;

        if ( unsetCount == 0.0 && setPercentage < 1.0 )
            throw new IllegalArgumentException( "unsetCount == 0.0 && setPercentage < 1.0" );

        double unsetPercentage = (1.0 - setPercentage) / unsetCount;

        for ( var c : children )
        {
            if ( c._weight == 0.0 )
                c._weight = unsetPercentage;
        }

        double totalWeight = 0.0;
        for ( var c : children )
        {
            if ( c._weight > 0.0 )
                totalWeight += c._weight;
        }

        LOG.info( "totalWeight=" + totalWeight );

        return children;
    }

    // TODO utility candidate.
    public static Rectangle calculateInnerArea( Container c )
    {
        Objects.requireNonNull( c );

        Insets insets =
                c.getInsets();
        Rectangle result =
                new Rectangle();

        result.x = insets.left;
        result.y = insets.top;
        result.width = c.getWidth() - insets.left - insets.right;
        result.height = c.getHeight() - insets.top - insets.bottom;

        return result;
    }

    @Override
    public void layoutContainer(Container parent)
    {
        validateLayout( _model.get() );

        _performLayout(
                _model.get(),
                calculateInnerArea( parent ) );
    }

    private DividerImpl dividerAt(NodeImpl root, int x, int y) {
        if (root instanceof DividerImpl) {
            DividerImpl divider = (DividerImpl)root;
            return (divider.bounds().contains(x, y)) ? divider : null;
        }
        else if (root instanceof SplitImpl) {
            SplitImpl split = (SplitImpl)root;
            for(NodeImpl child : split.getChildren()) {
                if (child.bounds().contains(x, y)) {
                    return dividerAt(child, x, y);
                }
            }
        }
        return null;
    }

    /**
     * Return the Divider whose bounds contain the specified
     * point, or null if there isn't one.
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return the Divider at x,y
     */
    public DividerImpl dividerAt( Point p ) {
        return dividerAt(getModel(), p.x, p.y );
    }

    public static abstract class Node
    {
        private final double _weight;

        protected Node( double weight )
        {
            if ( weight < 0.0 )
                throw new IllegalArgumentException(
                        "Negative weight:" + weight );

            _weight = weight;
        }

        public double weight()
        {
            return _weight;
        }

        abstract NodeImpl convert( MultiSplitLayout host );
    }

    private static abstract class Split extends Node
    {
        private final List<Node> _nodes;

        protected Split( Node... nodes )
        {
            super( 0.0 );
            _nodes = Arrays.asList( nodes );
        }

        protected Split( double weight, Node... nodes )
        {
            super( weight );

            var totalWeights =
                    totalWeights(
                            Objects.requireNonNull( nodes ) );

            if ( totalWeights > 1.0 )
                throw new IllegalArgumentException(
                        "Total weights > 0: " + totalWeights );

            _nodes = Arrays.asList( nodes );
        }

        private double totalWeights( Node... nodes )
        {
            var result = 0.0;

            for ( var c : nodes )
                result += c.weight();

            return result;
        }

        @Override
        abstract SplitImpl convert( MultiSplitLayout host );

        @Override
        public String toString()
        {
            var nodeStrings = new ArrayList<String>();

            for ( var c : _nodes )
                nodeStrings.toString();

            return StringUtil.concatenate( ", ", _nodes );
        }
    }

    public static class Column extends Split
    {
        public Column( double weight, Node... nodes )
        {
            super( weight, nodes );
        }
        public Column( Node... nodes )
        {
            super( nodes );
        }

        @Override
        ColumnImpl convert( MultiSplitLayout host )
        {
            return new ColumnImpl( this, host );
        }

        @Override
        public String toString()
        {
            return String.format(
                    "Row( weight=%f, %s )",
                    weight(),
                    super.toString() );
        }
    }

    public static class Row extends Split
    {
        public Row( double weight, Node... nodes )
        {
            super( weight, nodes );
        }
        public Row( Node... nodes )
        {
            super( nodes );
        }

        @Override
        RowImpl convert( MultiSplitLayout host )
        {
            return new RowImpl( this, host );
        }

        @Override
        public String toString()
        {
            return String.format(
                    "Row( weight=%f, %s )",
                    weight(),
                    super.toString() );
        }
    }

    public static class Leaf extends Node
    {
        private final String _name;

        public Leaf( double weight, String name )
        {
            super( weight );
            _name = name;
        }

        public String name()
        {
            return _name;
        }

        @Override
        LeafImpl convert( MultiSplitLayout host )
        {
            var result = new LeafImpl( this );

            host._leafMap.put( _name, result );

            return result;
        }

        @Override
        public String toString()
        {
            return String.format(
                    "Leaf( weight=%s, name=%s )",
                    weight(),
                    _name );
        }
    }

    /**
     * Base class for the nodes that model a MultiSplitLayout.
     */
    public static abstract class NodeImpl implements Serializable {

        private int _parentIdx;

        private SplitImpl _parent = null;

        private final Rectangle _bounds = new Rectangle();

        private double _weight = 0.0;

        private boolean isVisible = true;

        private MultiSplitLayout _host;

        public void setVisible( boolean b ) {
            isVisible = b;
        }

        protected void setParentIdx( int idx )
        {
            _parentIdx = idx;
        }
        protected int getParentIdx()
        {
            return _parentIdx;
        }

        /**
         * Determines whether this node should be visible when its
         * parent is visible. Nodes are
         * initially visible
         * @return <code>true</code> if the node is visible,
         * <code>false</code> otherwise
         */
        protected boolean isVisible() {
            return isVisible;
        }

        /**
         * Returns the Split parent of this Node, or null.
         *
         * @return the value of the parent property.
         * @see #setParent
         */
        protected SplitImpl getParent()
        {
            return _parent;
        }

        /**
         * Set the value of this Node's parent property.  The default
         * value of this property is null.
         *
         * @param parent a Split or null
         * @see #getParent
         */
        // TODO: fluent api
        protected void setParent(SplitImpl parent) {
            JavaUtil.Assert( _parent == null );
            _parent = parent;
        }

        public NodeImpl _width( int width )
        {
            _bounds.width = width;
            return this;
        }

        public int _width()
        {
            return _bounds.width;
        }

        public NodeImpl _height( int height )
        {
            _bounds.height = height;
            return this;
        }
        public int _height()
        {
            return _bounds.height;
        }
        public NodeImpl _x( int x )
        {
            _bounds.x = x;
            return this;
        }
        public int _x()
        {
            return _bounds.x;
        }
        public NodeImpl _y( int y )
        {
            _bounds.y = y;
            return this;
        }
        public int _y()
        {
            return _bounds.y;
        }

        public void attach( MultiSplitLayout host )
        {
            _host = host;
        }

        /**
         * @return The bounds property.
         */
        protected Rectangle bounds()
        {
            return _bounds;
        }

        /**
         * Update the bounds of this node.
         *
         * @param bounds the new value of the bounds property
         * @throws NullPointerException if bounds is null
         * @see #bounds()
         */
        public void setBounds(Rectangle bounds)
        {
            Objects.requireNonNull( bounds );

            _bounds.x =
                    bounds.x;
            _bounds.y =
                    bounds.y;
            _bounds.width =
                    bounds.width;
            _bounds.height =
                    bounds.height;
        }

        /**
         * Value between 0.0 and 1.0 used to compute how much space
         * to add to this sibling when the layout grows or how
         * much to reduce when the layout shrinks.
         *
         * @return the value of the weight property
         */
        public double getWeight()
        {
            return _weight;
        }

        /**
         * Fluent API.
         * @param weight
         * @return
         */
        public NodeImpl weight( double weight )
        {
            if ((weight < 0.0)|| (weight > 1.0)) {
                throw new IllegalArgumentException("invalid weight");
            }

            _weight = weight;

            return this;
        }

        abstract void validate( Set<String> nameCollector );

        abstract void layout(
                Rectangle bounds );

        protected MultiSplitLayout host()
        {
            return _host;
        }

        protected abstract Node convert();
    }

    /**
     *
     */
    public static class RowImpl extends SplitImpl {
        public RowImpl() {
        }

        public RowImpl( Row column, MultiSplitLayout host )
        {
            super( column, host );
        }

        public RowImpl(NodeImpl... children) {
            super( children );
        }

        /**
         * Row.
         */
        @Override
        public void adjustWeights()
        {
            double toDistribute = 0.0;

            for ( var c : getChildren2() )
                toDistribute += c._width();

            for ( var c : getChildren2() )
                c.weight( c._width() / toDistribute );
        }

        @Override
        protected int _extent( int idx  )
        {
            return getChildAt( idx )._width();
        }

        @Override
        protected int _staticExtent()
        {
            return _height();
        }

        @Override
        protected SplitImpl _staticExtent( int idx, int extent )
        {
            getChildAt( idx )._height( extent );
            return this;
        }

        @Override
        protected int _extentPosition( int idx )
        {
            return getChildAt( idx )._x();
        }

        @Override
        protected SplitImpl _position( int idx, int p )
        {
            getChildAt( idx )._x( p );
            return this;
        }

        @Override
        protected SplitImpl _extent( int idx, int p )
        {
            getChildAt( idx )._width( p );
            return this;
        }

        @Override
        protected int _pointDelta( Point from, Point to )
        {
            return to.x - from.x;
        }

        @Override
        public int expectedExtent()
        {
            return bounds().width;
        }

        @Override
        protected SplitImpl _staticPosition( int idx, int p )
        {
            getChildAt( idx )._y( p );
            return this;
        }

        @Override
        protected int _staticPosition()
        {
            return bounds().y;
        }

        @Override
        protected int _extent()
        {
            return bounds().width;
        }

        @Override
        protected Row convert()
        {
            ArrayList<Node> nodes = new ArrayList<>();

            for ( var c : getChildren2() )
                nodes.add( c.convert() );

            return new Row( nodes.toArray( new Node[0] ) );
        }
    }

    public static class ColumnImpl extends SplitImpl
    {
        public ColumnImpl( Column column, MultiSplitLayout host )
        {
            super( column, host );
        }

        public ColumnImpl(NodeImpl... children) {
            super(children);
        }

        /**
         * Column.
         */
        @Override
        public void adjustWeights()
        {
            double toDistribute = 0.0;

            for ( var c : getChildren2() )
                toDistribute += c._height();

            for ( var c : getChildren2() )
                c.weight( c._height() / toDistribute );
        }

        @Override
        protected int _extent( int idx )
        {
            return getChildAt( idx )._height();
        }

        @Override
        protected int _staticExtent()
        {
            return _width();
        }

        @Override
        protected int _extentPosition( int idx )
        {
            return getChildAt( idx )._y();
        }

        @Override
        protected SplitImpl _position( int idx, int p )
        {
            getChildAt( idx )._y( p );
            return this;
        }

        @Override
        protected SplitImpl _extent( int idx, int p )
        {
            getChildAt( idx )._height( p );
            return this;
        }

        @Override
        protected int _pointDelta( Point from, Point to )
        {
            return to.y - from.y;
        }

        @Override
        public int expectedExtent()
        {
            return bounds().height;
        }

        @Override
        protected SplitImpl _staticExtent( int idx, int extent )
        {
            getChildAt( idx )._width( extent );
            return this;
        }

        @Override
        protected SplitImpl _staticPosition( int idx, int p )
        {
            getChildAt( idx )._x( p );
            return this;
        }

        @Override
        protected int _staticPosition()
        {
            return bounds().x;
        }

        @Override
        protected int _extent()
        {
            return bounds().height;
        }

        @Override
        protected Column convert()
        {
            ArrayList<Node> nodes = new ArrayList<>();

            for ( var c : getChildren2() )
                nodes.add( c.convert() );

            return new Column( nodes.toArray( new Node[0] ) );
        }
    }

    /**
     * Defines a vertical or horizontal subdivision into two or more tiles.
     */
    public static abstract class SplitImpl extends NodeImpl
    {
        private List<NodeImpl> _children =
                Collections.emptyList();

        /**
         * @param children
         */
        private SplitImpl( NodeImpl... children )
        {
            setChildren(children);
        }

        public SplitImpl( Split split, MultiSplitLayout host )
        {
            var splitChildCount =
                    split._nodes.size();

            JavaUtil.Assert( splitChildCount > 0 );

            List<NodeImpl> children =
                    new ArrayList<>(
                            splitChildCount +
                            splitChildCount -1 );

            int nodesIdx = 0;
            for ( int i = 0 ; i < children.size() ; i++ )
            {
                if ( MathUtil.isEven(i) )
                {
                    children.set(
                            i,
                            split._nodes.get( nodesIdx++ ).convert( host ) );
                }
                else
                {
                    children.set( i, new DividerImpl() );
                }

            }

            setChildren(children);
        }

        /**
         * Adjust the weights to the nodes sizes.
         */
        public abstract void adjustWeights();

        /**
         * @return The split's real extent after formatting.
         */
        public final int realExtent()
        {
            var lastIdx = getChildren().size() -1;

            var lastPosition =
                    _extentPosition( lastIdx);
            var lastExtent =
                    _extent( lastIdx );
            return
                    lastPosition +
                    lastExtent;
        }

        public abstract int expectedExtent();

        /**
         * Determines whether this node should be visible when its
         * parent is visible. Nodes are
         * initially visible
         * @return <code>true</code> if the node is visible,
         * <code>false</code> otherwise
         */
        @Override
        public boolean isVisible() {
            for(NodeImpl child : _children) {
                if ( child.isVisible() && !( child instanceof DividerImpl ))
                    return true;
            }
            return false;
        }

        /**
         * @return The split's dynamic extent.
         */
        protected abstract int _extent();

        /**
         * @param idx child index.
         * @return The dynamic extend of the child.
         */
        protected abstract int _extent( int idx );

        /**
         * Set the extent of the child.
         * @param idx The child index.
         * @param p The new extent
         * @return Fluent API.
         */
        protected abstract SplitImpl _extent( int idx, int p );

        /**
         * @return The static (common) extent of this split.
         */
        protected abstract int _staticExtent();

        protected abstract int _staticPosition();

        /**
         * Get the distributable extent.  This is the total
         * of the children extents without the dividers.
         */
        protected final int distributableExtent()
        {
            var result = _extent();

            int dividerCount =
                    getChildren().size() / 2;

            return
                    result -
                    (dividerCount * host().getDividerSize());
        }

        /**
         * @param idx The child index.
         * @return The dynamic position of the child.
         */
        protected abstract int _extentPosition( int idx );

        protected abstract SplitImpl _staticPosition( int idx, int p );

        /**
         * Set the position of the dynamic extent.
         * @param idx The child index.
         * @param p The new position.
         * @return Fluent API.
         */
        protected abstract SplitImpl _position( int idx, int p );

        protected abstract int _pointDelta( Point from, Point to );

        protected abstract SplitImpl _staticExtent( int idx, int extent );

        /**
         * @return true if the this Split's children are to be
         * laid out in a row.  If false,
         * children are laid out in a column.
         */
        private final boolean isRowLayout() { return this instanceof RowImpl; }

        /**
         * @return This node's children.
         */
        public List<NodeImpl> getChildren()
        {
            return _children;
        }

        public List<NodeImpl> getChildren2() {
            return _children.stream().filter(
                  c -> ! DividerImpl.class.isInstance( c ) ).collect(
                           Collectors.toList() );
        }

        public NodeImpl getChildAt( int idx )
        {
            return _children.get( idx );
        }

        /**
         * Set's the children property of this Split node.
         *
         * @param children List of children
         * @see #getChildren
         * @throws IllegalArgumentException if children is null
         */
        private void setChildren(List<NodeImpl> children)
        {
            JavaUtil.Assert( _children.size() == 0 );

            _children = Collections.unmodifiableList( children );

            int idx = 0;

            for(NodeImpl c : _children)
            {
                c.setParent(this);
                c.setParentIdx( idx++ );
            }

            _completeWeights( _children );
        }

        /**
         * Convenience method for setting the children of this Split node.  The parent
         * of each new child is set to this Split node, and the parent
         * of each old child (if any) is set to null.
         *
         * @param children array of children
         * @see #getChildren
         * @throws IllegalArgumentException if children is null
         */
        public void setChildren( NodeImpl... children )
        {
            setChildren( Arrays.asList(
                    Objects.requireNonNull( children ) ) );
        }

        @Override
        public String toString() {
            int nChildren = getChildren().size();
            StringBuffer sb = new StringBuffer("MultiSplitLayout.Split");
            sb.append(isRowLayout() ? " ROW [" : " COLUMN [");
            sb.append(nChildren + ((nChildren == 1) ? " child" : " children"));
            sb.append("] ");
            sb.append(bounds());
            return sb.toString();
        }

        @Override
        final void validate( Set<String> nameCollector )
        {
            if (getChildren().size() <= 2) {
                throwInvalidLayout("Split must have > 2 children", this);
            }

            {
                double totalWeight = 0.0;
                for ( var c : getChildren2() )
                {
                    c.validate( nameCollector );
                    totalWeight += c._weight;
                }
                if ( totalWeight > 1.0 )
                    throwInvalidLayout("Split children's total weight > 1.0", this);
            }
        }

        @Override
        public final void layout( Rectangle bounds )
        {
            final int distributableExtent =
                    distributableExtent();

            double currentPosition = .0;
            double _error = .0;

            for ( int i = 0 ; i < getChildren().size() ; i++ )
            {
                var c = getChildren().get( i );

                _position( i, MathUtil.round( currentPosition ) );
                _staticPosition( i, _staticPosition() );

                _staticExtent( i, _staticExtent() );

                // For the computation of the extent we have
                // to toggle between splits and dividers.
                if ( MathUtil.isEven( i ) )
                {
                    double w =
                            (c.getWeight() * distributableExtent) -
                            _error;
                    _extent(
                            i,
                            MathUtil.round( w ) );
                    _error =
                            _extent( i ) -
                            w;

                    c.layout(
                            c.bounds() );
                }
                else
                {
                    _extent( i, host().getDividerSize() );
                }

                currentPosition += _extent( i );
            }

            if ( realExtent() != _extent() )
            {
                LOG.severe( String.format(
                        "Corrected extent %d not %d.", realExtent(), _extent() ) );
                throw new AssertionError();
            }
        }

        @Override
        public void attach( MultiSplitLayout host )
        {
            super.attach( host );

            for ( var c : getChildren() )
                c.attach( host );
        }
    }

    /**
     * Models a java.awt Component child.
     */
    public static class LeafImpl extends NodeImpl
    {
        private final String _name;

        /**
         * Create a Leaf node.  The default value of name is "".
         */
        public LeafImpl( Leaf leaf )
        {
            _name = Objects.requireNonNull( leaf.name() );
        }

        /**
         * Create a Leaf node with the specified name.  Name cannot
         * be null.
         *
         * @param name value of the Leaf's name property
         * @throws IllegalArgumentException if name is null
         */
        public LeafImpl(String name) {
            _name = Objects.requireNonNull( name );
        }

        /**
         * Return the Leaf's name.
         *
         * @return the value of the name property.
         * @see #setName
         */
        public String getName() { return _name; }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("MultiSplitLayout.Leaf");
            sb.append(" \"");
            sb.append(getName());
            sb.append("\"");
            sb.append(" weight=");
            sb.append(getWeight());
            sb.append(" ");
            sb.append(bounds());
            return sb.toString();
        }

        @Override
        public void setBounds( Rectangle bounds )
        {
            super.setBounds( bounds );

            LOG.info( toString() );
        }

        @Override
        void validate( Set<String> nameCollector )
        {
            if ( nameCollector.contains( _name ) )
               throw new InvalidLayoutException( "Duplicate name: " + _name, this );
            nameCollector.add( _name );
        }

        @Override
        void layout( Rectangle bounds )
        {
            host().getComponentForNode( this ).setBounds( bounds );
        }

        @Override
        protected Node convert()
        {
            return new Leaf( getWeight(), _name );
        }
    }

    /**
     * Models a single vertical/horizontal divider.
     */
    public static class DividerImpl extends NodeImpl {
        /**
         * Convenience method, returns true if the Divider's parent
         * is a Split row (a Split with isRowLayout() true), false
         * otherwise. In other words if this Divider's major axis
         * is vertical, return true.
         *
         * @return true if this Divider is part of a Split row.
         */
        public final boolean isVertical() {
            SplitImpl parent = getParent();
            return (parent != null) ? parent.isRowLayout() : false;
        }

        /**
         * Dividers can't have a weight, they don't grow or shrink.
         * @throws UnsupportedOperationException
         */
        @Override
        public NodeImpl weight( double weight )
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getWeight()
        {
            return -1.0;
        }

        @Override
        public String toString() {
            return "MultiSplitLayout.Divider " + bounds().toString();
        }

        @Override
        void validate( Set<String> nameCollector )
        {
        }

        @Override
        void layout( Rectangle bounds )
        {
            throw new AssertionError("Unexpected");
        }

        /**
         * Moves this Divider.
         *
         * @param from
         * @param to
         * @param minimumExtent
         * @return The point position the divider was moved to.
         */
        public Point move( Point from, Point to, int minimumExtent )
        {
            final var parent = getParent();
            final var ownIdx = getParentIdx();
            final var prevIdx = ownIdx-1;
            final var nextIdx = ownIdx+1;

            var delta = parent._pointDelta(
                    from,
                    to );

            var prevWidth =
                    parent._extent( prevIdx ) +
                    delta;
            if ( prevWidth <= minimumExtent )
                return from;

            var nextWidth =
                    parent._extent( nextIdx ) -
                    delta;
            if ( nextWidth <= minimumExtent )
                return from;

            parent._extent( prevIdx, prevWidth );

            parent._extent(
                    nextIdx,
                    nextWidth );

            getParent().adjustWeights();

            return to;
        }

        @Override
        protected Node convert()
        {
            throw new AssertionError();
        }
    }

    private static void printModel(String indent, NodeImpl root) {
        if (root instanceof SplitImpl) {
            SplitImpl split = (SplitImpl)root;
            System.out.println(indent + split);
            for(NodeImpl child : split.getChildren()) {
                printModel(indent + "  ", child);
            }
        }
        else {
            System.out.println(indent + root);
        }
    }

    /**
     * Print the tree with enough detail for simple debugging.
     */
    public static void printModel(NodeImpl root) {
        printModel("", root);
    }
}
