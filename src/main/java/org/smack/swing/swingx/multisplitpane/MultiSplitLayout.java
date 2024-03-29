/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2003-2023 Michael Binz
 */
package org.smack.swing.swingx.multisplitpane;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.swing.JSplitPane;

import org.smack.swing.SwingUtil;
import org.smack.swing.beans.AbstractBean;
import org.smack.swing.beans.JavaBeanProperty;
import org.smack.swing.swingx.JXMultiSplitPane;
import org.smack.util.JavaUtil;
import org.smack.util.MathUtil;
import org.smack.util.StringUtil;

/**
 * The MultiSplitLayout layout manager recursively arranges its
 * components in row and column groups called "Splits".
 * <p>
 * Although MultiSplitLayout can be used with any Container, it's
 * the default layout manager for MultiSplitPane.  MultiSplitPane
 * supports interactively dragging the Dividers.
 *
 * @author Michael Binz
 * @see JXMultiSplitPane
 */
@SuppressWarnings("serial")
public class MultiSplitLayout
    extends
        AbstractBean
    implements
        LayoutManager
{
    private final static Logger LOG =
            Logger.getLogger( MultiSplitLayout.class.getName() );

    /**
     * Holds the name to component mappings.
     */
    private final Map<String, Component> _childMap =
            new HashMap<String, Component>();

    /**
     * Holds the name to Leaf mappings.
     */
    private final Map<String, LeafImpl> _leafMap =
            new HashMap<>();

    private SplitImpl _internalModel;

    private JavaBeanProperty<Split, MultiSplitLayout> _model =
            new JavaBeanProperty<>(
                    this,
                    null,
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

    /**
     * Create a MultiSplitLayout with a default model with a single
     * Leaf node named "default".
     *
     * #see setModel
     */
    public MultiSplitLayout()
    {
        _internalModel = null;
    }

    /**
     * Create a MultiSplitLayout with the specified model.
     *
     * #see setModel
     */
    public MultiSplitLayout( Split model  )
    {
        setModel( model );
    }

    /**
     * Get the Leaf for the passed name.  This is for
     * testing purposes.
     *
     * @param name The leaf's name.
     * @return The leaf.
     * @throws IllegalArgumentException If the leaf is not found.
     */
    LeafImpl getLeafForName( String name )
    {
        if ( ! _leafMap.containsKey( name ) )
            throw new IllegalArgumentException( "Unknown name: " + name );

        return _leafMap.get( name );
    }

    /**
     * Get the component associated with a MultiSplitLayout.Node
     * @param n the layout node
     * @return the component handled by the layout or null if not found
     */
    private Component getComponentForNode( NodeImpl n )
    {
        String name = ((LeafImpl)n).name();
        return (name != null) ? (Component)_childMap.get(name) : null;
    }

    /**
     * Get the name used to map a component
     * @param child the component
     * @return the name used to map the component or null if no mapping is found
     */
    private String getNameForComponent( Component child )
    {
        for(Map.Entry<String,Component> kv : _childMap.entrySet())
        {
            if (kv.getValue() == child)
                return kv.getKey();
        }

        return null;
    }

    /**
     * Return the root of the tree of Split, Leaf, and Divider nodes
     * that define this layout.
     *
     * @return the value of the model property
     * @see #setModel
     */
    public Split getModel()
    {
        return _internalModel == null ?
                null :
                _internalModel.convert();
    }

    /**
     * Set the model root.
     *
     * @param model The model root.
     * @see #getModel
     */
    public void setModel(Split model)
    {
        var internalModel =
                Objects.requireNonNull( model ).convert( this );

        validateLayout( internalModel );

        _model.set( model );

        _internalModel = internalModel;
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
     * Sets the size of Dividers.
     *
     * @param size the size of dividers (pixels)
     * @throws IllegalArgumentException if dividerSize < 0
     * @see #getDividerSize
     */
    public void setDividerSize(int size)
    {
        if (size <= 0)
            throw new IllegalArgumentException("invalid dividerSize");

        _dividerSize.set( size );
    }

    /**
     * Add a component to this MultiSplitLayout.  The
     * <code>name</code> should match the name property of the Leaf
     * node that represents the bounds of <code>child</code>.
     *
     * @param name The name of the Leaf node that defines the child's bounds.
     * @param child The component to be added.
     * @see #removeLayoutComponent
     */
    @Override
    public void addLayoutComponent(String name, Component child)
    {
        if ( StringUtil.isEmpty( name ) )
            throw new IllegalArgumentException( "Empty name." );

        if ( ! _leafMap.containsKey( name ) )
            throw new IllegalArgumentException( "Unknown leaf: " + name );

        _childMap.put( name, child );
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

        if ( name != null )
            _childMap.remove( name );
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
     * Get the minimum node size.
     * @return the minimum size.
     */
    public int getMinimumExtent()
    {
        return _minimumExtent.get();
    }

    /**
     * Set the minimum node size.
     *
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
    private static class InvalidLayoutException extends IllegalArgumentException {
        public InvalidLayoutException(String msg, NodeImpl node) {
            super(msg + ":" + node );
        }
    }

    private static void throwInvalidLayout(String msg, NodeImpl node) {
        throw new InvalidLayoutException(msg, node);
    }

    private void validateLayout( NodeImpl root )
    {
        root.validate();
    }

    /**
     *
     * @param node The node to layout.
     * @param bounds
     */
    private void performLayout( NodeImpl node, Rectangle bounds )
    {
        node.setBounds( bounds );

        node.layout( bounds );
    }

    private static List<NodeImpl> completeWeights( List<NodeImpl> children )
    {
        double[] weights = new double[children.size()];

        for ( int i = 0 ; i < weights.length ; i++ )
        {
            var c = children.get( i );
                weights[i] = c.weight();
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

    @Override
    public void layoutContainer(Container parent)
    {
        performLayout(
                _internalModel,
                SwingUtil.calculateInnerArea( parent ) );
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
     * @param p The point to check.
     * @return The Divider at the passed point.
     */
    public DividerImpl dividerAt( Point p )
    {
        return dividerAt(_internalModel, p.x, p.y );
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

        protected abstract String toString( int indentLevel );

        @Override
        public String toString()
        {
            return toString( 0 );
        }

        abstract NodeImpl convert( MultiSplitLayout host );
    }

    public static abstract class Split extends Node
    {
        private final List<Node> _nodes;

        protected Split( double weight, Node... nodes )
        {
            super( weightInRange( weight ) );

            _nodes = Arrays.asList(
                    validate( Objects.requireNonNull( nodes ) ) );
        }

        protected Split( Node... nodes )
        {
            this( .0, nodes );
        }

        private Node[] validate( Node[] nodes )
        {
            double totalWeight = .0;
            boolean hasFreeWeight = false;

            for ( var c : nodes )
            {
                var currentWeight = c.weight();

                if ( currentWeight == .0 )
                   hasFreeWeight = true;

                totalWeight += currentWeight;
            }

            if ( totalWeight > 1.0 )
                throw new IllegalArgumentException(
                        "Weights > 1.0: " + totalWeight );
            if ( totalWeight < 1. && ! hasFreeWeight )
                throw new IllegalArgumentException(
                        "Weights < 1.0 without compensation: " + totalWeight );

            return nodes;
        }

        @Override
        abstract SplitImpl convert( MultiSplitLayout host );

        @Override
        public final String toString( int indentLevel )
        {
            var nodeStrings = new ArrayList<String>();

            for ( var c : _nodes )
                nodeStrings.add( c.toString( indentLevel +1 ) );

            final var indentation = StringUtil.createFilledString(
                    "\t",
                    indentLevel );

            return String.format(
                    "%s%s( weight=%s,%n%s )",
                    indentation,
                    getClass().getSimpleName(),
                    weight(),
                    StringUtil.concatenate( ",\n", nodeStrings ) );
        }
    }

    public final static class Column extends Split
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
            return host.new ColumnImpl( this, host );
        }
    }

    public final static class Row extends Split
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
            return host.new RowImpl( this );
        }
    }

    public final static class Leaf extends Node
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
            return host.new LeafImpl( this );
        }

        @Override
        public String toString( int indentLevel )
        {
            final var indentation = StringUtil.createFilledString(
                    "\t",
                    indentLevel );

            return String.format(
                    "%sLeaf( weight=%s, name=%s )",
                    indentation,
                    weight(),
                    _name );
        }
    }

    /**
     * Base class for the nodes that model a MultiSplitLayout.
     */
    abstract class NodeImpl
    {
        /**
         * The parent node or null if the node is the root of the
         * hierarchy.
         */
        private SplitImpl _parent = null;

        /**
         * The node's index in its parent.
         */
        private int _parentIdx;

        /**
         * The node's bounds.
         */
        private final Rectangle _bounds = new Rectangle();

        /**
         * The node's weight.
         */
        private double _weight = .0;

        protected final NodeImpl parentIdx( int idx )
        {
            _parentIdx = idx;
            return this;
        }

        protected final int parentIdx()
        {
            return _parentIdx;
        }

        /**
         * @return The parent of this Node, or null.
         * @see NodeImpl#parent(SplitImpl))
         */
        protected SplitImpl parent()
        {
            return _parent;
        }

        /**
         * Set the parent.
         *
         * @param parent a Split or null
         * @see NodeImpl#parent()
         */
        protected NodeImpl parent(SplitImpl parent)
        {
            JavaUtil.Assert( _parent == null );
            _parent = parent;
            return this;
        }

        final NodeImpl width( int width )
        {
            _bounds.width = width;
            return this;
        }

        final int width()
        {
            return _bounds.width;
        }

        final NodeImpl height( int height )
        {
            _bounds.height = height;
            return this;
        }

        final int height()
        {
            return _bounds.height;
        }

        final NodeImpl x( int x )
        {
            _bounds.x = x;
            return this;
        }

        final int x()
        {
            return _bounds.x;
        }

        final NodeImpl y( int y )
        {
            _bounds.y = y;
            return this;
        }

        final int y()
        {
            return _bounds.y;
        }

        /**
         * @return The bounds property.
         */
        final Rectangle bounds()
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
        final void setBounds(Rectangle bounds)
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
         * @return The node's weight.
         */
        final double weight()
        {
            return _weight;
        }

        /**
         * Fluent API.
         * @param weight
         * @return
         */
        final NodeImpl weight( double weight )
        {
            _weight = weight;

            return this;
        }

        /**
         * Validate the node.  The default implementation is empty.
         */
        protected void validate()
        {
        }

        abstract void layout(
                Rectangle bounds );

        protected abstract Node convert();
    }

    /**
     *
     */
    class RowImpl extends SplitImpl
    {
        public RowImpl( Row column )
        {
            super( column );
        }

        public RowImpl(NodeImpl... children)
        {
            super( children );
        }

        @Override
        protected int extent( int idx  )
        {
            return getChildAt( idx ).width();
        }

        @Override
        protected int staticExtent()
        {
            return height();
        }

        @Override
        protected SplitImpl staticExtent( int idx, int extent )
        {
            getChildAt( idx ).height( extent );
            return this;
        }

        @Override
        protected int extentPosition( int idx )
        {
            return getChildAt( idx ).x();
        }

        @Override
        protected SplitImpl position( int idx, int p )
        {
            getChildAt( idx ).x( p );
            return this;
        }

        @Override
        protected SplitImpl extent( int idx, int p )
        {
            getChildAt( idx ).width( p );
            return this;
        }

        @Override
        protected int pointDelta( Point from, Point to )
        {
            return to.x - from.x;
        }

        @Override
        public int expectedExtent()
        {
            return bounds().width;
        }

        @Override
        protected SplitImpl staticPosition( int idx, int p )
        {
            getChildAt( idx ).y( p );
            return this;
        }

        @Override
        protected int staticPosition()
        {
            return bounds().y;
        }

        @Override
        protected int extent()
        {
            return bounds().width;
        }

        @Override
        protected Row convert()
        {
            ArrayList<Node> nodes = new ArrayList<>();

            for ( var c : getChildren2() )
                nodes.add( c.convert() );

            return new Row( weight(), nodes.toArray( new Node[0] ) );
        }
    }

    public class ColumnImpl extends SplitImpl
    {
        ColumnImpl( Column column, MultiSplitLayout host )
        {
            super( column );
        }

        ColumnImpl(NodeImpl... children) {
            super(children);
        }

        @Override
        protected int extent( int idx )
        {
            return getChildAt( idx ).height();
        }

        @Override
        protected int staticExtent()
        {
            return width();
        }

        @Override
        protected int extentPosition( int idx )
        {
            return getChildAt( idx ).y();
        }

        @Override
        protected SplitImpl position( int idx, int p )
        {
            getChildAt( idx ).y( p );
            return this;
        }

        @Override
        protected SplitImpl extent( int idx, int p )
        {
            getChildAt( idx ).height( p );
            return this;
        }

        @Override
        protected int pointDelta( Point from, Point to )
        {
            return to.y - from.y;
        }

        @Override
        public int expectedExtent()
        {
            return bounds().height;
        }

        @Override
        protected SplitImpl staticExtent( int idx, int extent )
        {
            getChildAt( idx ).width( extent );
            return this;
        }

        @Override
        protected SplitImpl staticPosition( int idx, int p )
        {
            getChildAt( idx ).x( p );
            return this;
        }

        @Override
        protected int staticPosition()
        {
            return bounds().x;
        }

        @Override
        protected int extent()
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
    public abstract class SplitImpl extends NodeImpl
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

        SplitImpl( Split split )
        {
            weight( split.weight() );

            var splitChildCount =
                    split._nodes.size();

            JavaUtil.Assert( splitChildCount > 0 );

            NodeImpl[] children =
                    new NodeImpl[
                            splitChildCount +
                            splitChildCount -1 ];

            int nodesIdx = 0;
            for ( int i = 0 ; i < children.length ; i++ )
            {
                children[i] = MathUtil.isEven(i) ?
                        split._nodes.get( nodesIdx++ ).convert( MultiSplitLayout.this ) :
                        new DividerImpl();
            }

            setChildren( Arrays.asList( children ) );
        }

        @Override
        abstract protected Split convert();

        /**
         * Update the weights from the node sizes.  Used in dragging.
         */
        final void adjustWeights()
        {
            double toDistribute = .0;

            for ( var c : getChildren2() )
                toDistribute += extent( c.parentIdx() );

            for ( var c : getChildren2() )
                c.weight( extent( c.parentIdx() ) / toDistribute );
        }

        /**
         * @return The split's real extent after formatting.
         */
        public final int realExtent()
        {
            var lastIdx = getChildren().size() -1;

            var lastPosition =
                    extentPosition( lastIdx);
            var lastExtent =
                    extent( lastIdx );
            return
                    lastPosition +
                    lastExtent;
        }

        public abstract int expectedExtent();

        /**
         * @return The split's dynamic extent.
         */
        protected abstract int extent();

        /**
         * @param idx child index.
         * @return The dynamic extend of the child.
         */
        protected abstract int extent( int idx );

        /**
         * Set the extent of the child.
         * @param idx The child index.
         * @param p The new extent
         * @return Fluent API.
         */
        protected abstract SplitImpl extent( int idx, int p );

        /**
         * @return The static (common) extent of this split.
         */
        protected abstract int staticExtent();

        protected abstract int staticPosition();

        /**
         * Get the distributable extent.  This is the total
         * of the children extents without the dividers.
         */
        protected final int distributableExtent()
        {
            var result = extent();

            int dividerCount =
                    getChildren().size() / 2;

            return
                    result -
                    (dividerCount * MultiSplitLayout.this.getDividerSize());
        }

        /**
         * @param idx The child index.
         * @return The dynamic position of the child.
         */
        protected abstract int extentPosition( int idx );

        /**
         * Set the static position of a child.  That is, the position that is
         * the same for all elements of a Split.
         *
         * @param idx The child index.
         * @param p The new position to set.
         * @return Fluent API.
         */
        protected abstract SplitImpl staticPosition( int idx, int p );

        /**
         * Set the position of the dynamic extent.
         * @param idx The child index.
         * @param p The new position.
         * @return Fluent API.
         */
        protected abstract SplitImpl position( int idx, int p );

        /**
         * Two drag points.
         * @param from The start point.
         * @param to The target point.
         * @return The significant delta depending on the Split type.
         */
        protected abstract int pointDelta( Point from, Point to );

        /**
         * Set the static extent.  That is, the extent that is the same for
         * all elements in a Split.
         *
         * @param idx The element index.
         * @param extent The new extent to set.
         * @return Fluent API.
         */
        protected abstract SplitImpl staticExtent( int idx, int extent );

        /**
         * @return true if the this Split's children are to be
         * laid out in a row.  If false,
         * children are laid out in a column.
         */
        private final boolean isRowLayout()
        {
            return this instanceof RowImpl;
        }

        /**
         * @return This node's children.
         */
        List<NodeImpl> getChildren()
        {
            return _children;
        }

        /**
         *
         * @return The children without intermediate dividers.
         */
        List<NodeImpl> getChildren2() {
            return _children.stream().filter(
                  c -> ! DividerImpl.class.isInstance( c ) ).collect(
                           Collectors.toList() );
        }

        NodeImpl getChildAt( int idx )
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

            for( NodeImpl c : _children )
            {
                c.parent(this);
                c.parentIdx( idx++ );
            }

            completeWeights( _children );
        }

        /**
         * Convenience method for setting the children of this Split node.
         *
         * @param children array of children
         * @see #getChildren
         */
        private void setChildren( NodeImpl... children )
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
        protected void validate()
        {
            double totalWeight = .0;
            for ( var c : getChildren2() )
            {
                c.validate();
                totalWeight += c._weight;
            }
            if ( totalWeight > 1.0 )
                throwInvalidLayout("Split children's total weight > 1.0", this);
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

                position( i, MathUtil.round( currentPosition ) );
                staticPosition( i, staticPosition() );

                staticExtent( i, staticExtent() );

                // For the computation of the extent we have
                // to toggle between splits and dividers.
                if ( MathUtil.isEven( i ) )
                {
                    double w =
                            (c.weight() * distributableExtent) -
                            _error;
                    extent(
                            i,
                            MathUtil.round( w ) );
                    _error =
                            extent( i ) -
                            w;

                    c.layout(
                            c.bounds() );
                }
                else
                {
                    extent( i, MultiSplitLayout.this.getDividerSize() );
                }

                currentPosition += extent( i );
            }

            if ( realExtent() != extent() )
            {
                LOG.severe( String.format(
                        "Corrected extent %d not %d.", realExtent(), extent() ) );
                throw new AssertionError();
            }
        }
    }

    /**
     * Models a java.awt.Component child.
     */
    public class LeafImpl extends NodeImpl
    {
        private final String _name;

        /**
         * Create a Leaf node.
         */
        LeafImpl( Leaf leaf )
        {
            this( leaf.weight(), leaf.name() );
        }

        /**
         * Create a Leaf node with the specified name.  Name cannot
         * be null.
         *
         * @param name value of the Leaf's name property
         * @throws IllegalArgumentException if name is null
         */
        LeafImpl( double weight, String name )
        {
            weight( weight );

            if ( StringUtil.isEmpty( name ) )
                throw new IllegalArgumentException( "Name is empty." );

            _name = name;

            var leafNames =
                    MultiSplitLayout.this._leafMap;

            if ( leafNames.containsKey( _name ) )
                throw new IllegalArgumentException(
                        "Duplicate leaf: " + _name );

            leafNames.put( _name, this );
        }

        LeafImpl( String name )
        {
            this( .0, name );
        }

        /**
         * @return the Leaf's name.
         */
        String name()
        {
            return _name;
        }

        @Override
        public String toString()
        {
            return String.format(
                    "%s( name=\"%s\" weight=%f bounds=%s )",
                    getClass().getSimpleName(),
                    name(),
                    weight(),
                    bounds() );
        }

        @Override
        void layout( Rectangle bounds )
        {
            var node = MultiSplitLayout.this.
                getComponentForNode( this );

            if ( node == null )
                return;

            node.setBounds( bounds );
        }

        @Override
        protected Node convert()
        {
            return new Leaf( weight(), _name );
        }
    }

    /**
     * Models a single vertical/horizontal divider.
     */
    public class DividerImpl extends NodeImpl
    {
        public DividerImpl()
        {
            super.weight( -1.0 );
        }

        /**
         * Returns true if the Divider's parent
         * is a Split row (a Split with isRowLayout() true), false
         * otherwise. In other words if this Divider's major axis
         * is vertical, return true.
         *
         * @return true if this Divider is part of a Split row.
         */
        public final boolean isVertical()
        {
            SplitImpl parent =
                    parent();
            return (parent != null) ?
                    parent.isRowLayout() :
                    false;
        }

        @Override
        public String toString() {
            return "MultiSplitLayout.Divider " + bounds().toString();
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
        public Point move( Point from, Point to )
        {
            final var minimumExtent =
                    MultiSplitLayout.this.getMinimumExtent();
            final var parent = parent();
            final var ownIdx = parentIdx();
            final var prevIdx = ownIdx-1;
            final var nextIdx = ownIdx+1;

            var delta = parent.pointDelta(
                    from,
                    to );

            var prevWidth =
                    parent.extent( prevIdx ) +
                    delta;
            if ( prevWidth <= minimumExtent )
                return from;

            var nextWidth =
                    parent.extent( nextIdx ) -
                    delta;
            if ( nextWidth <= minimumExtent )
                return from;

            parent.extent(
                    prevIdx,
                    prevWidth );
            parent.extent(
                    nextIdx,
                    nextWidth );

            parent().adjustWeights();

            return to;
        }

        @Override
        protected Node convert()
        {
            throw new AssertionError();
        }
    }

    /**
     * Checks if the passed weight is in range [0.0 .. 1.0].
     *
     * @param weight The weight to check.
     * @return The passed weight.
     * @throws IllegalArgumentException If the passed weight is
     * not in the required range.
     */
    private static double weightInRange( double weight )
    {
        if ( weight < .0 )
            new IllegalArgumentException(
                    "Weight is negative: " + weight );
        if ( weight > 1. )
            new IllegalArgumentException(
                    "Weight greater 1.0: " + weight );

        return weight;
    }

    //
    // Parser.
    //

    static private class ParseX extends ParseException
    {
        public ParseX( String expected, StreamTokenizer st )
        {
            super( String.format(
                    "Expected: %s",
                    expected ),
                    st.lineno() );
        }
    }

    static private void consume( StreamTokenizer st, char token )
            throws Exception
    {
        if ( st.nextToken() != token )
            throw new ParseX( Character.toString( token ), st );
    }

    static double parseNumberAssignment( StreamTokenizer st, String name )
            throws Exception
    {
        if ( st.nextToken() != StreamTokenizer.TT_WORD )
            throw new ParseX( "TT_WORD", st );

        if ( ! name.equals( st.sval ) )
            throw new ParseX( name, st );

        consume( st, '=' );

        if ( st.nextToken() != StreamTokenizer.TT_NUMBER )
            throw new ParseX( "TT_NUMBER", st );

        return st.nval;
    }

    static String parseStringAssignment( StreamTokenizer st, String name )
            throws Exception
    {
        if ( st.nextToken() != StreamTokenizer.TT_WORD )
            throw new ParseX( "TT_WORD", st );

        if ( ! name.equals( st.sval ) )
            throw new ParseX( name, st );

        consume( st, '=' );

        if ( st.nextToken() != StreamTokenizer.TT_WORD )
            throw new ParseX( "TT_WORD", st );

        return st.sval;
    }

    /**
     *
     * @param st
     * @param nodes
     * @return true if the list continues.
     * @throws Exception
     */
    static private boolean parseSplitArgumentsRestElement(
        StreamTokenizer st,
        List<Node> nodesOut )
            throws Exception
    {
        nodesOut.add( parseNode( st ) );

        if ( st.nextToken() == ',' )
            return true;

        st.pushBack();

        return false;
    }

    /**
     *
     * @param st
     * @param nodes
     * @return weight of the node
     * @throws Exception
     */
    static private double parseSplitArgumentsPrefix( StreamTokenizer st )
            throws Exception
    {
        // ... weight=0.0, node, node )
        var result = parseNumberAssignment( st, "weight" );

        consume( st, ',' );

        return result;
    }

    static private double parseSplitArguments( StreamTokenizer st, List<Node> nodes )
            throws Exception
    {
        var result = parseSplitArgumentsPrefix( st );

        while ( parseSplitArgumentsRestElement( st, nodes ) )
            ;

        consume( st, ')' );

        return result;
    }


    private final static Node[] EMPTY_NODES = new Node[0];

    /**
     * Leaf( weight=0.0, name="left" )
     *     ^
     *
     * @param st The tokenizer.
     * @return A Leaf.
     * @throws Exception In case of IO error or parser failure.
     */
    static private Leaf parseLeaf( StreamTokenizer st ) throws Exception
    {
        consume( st, '(' );

        var weight = parseNumberAssignment( st, "weight" );

        consume( st, ',' );

        var name = parseStringAssignment( st, "name" );

        consume( st, ')' );

        return new Leaf( weight, name );
    }

    static private Node parseNode( StreamTokenizer st ) throws Exception
    {
        if ( st.nextToken() != StreamTokenizer.TT_WORD )
            throw new ParseX( "TT_WORD", st );

        if ( st.sval.equals( "Leaf" ) )
            return parseLeaf( st );

        st.pushBack();

        return parseSplit( st );
    }

    static private <R> R parseSplit(
            StreamTokenizer st,
            BiFunction<Double,Node[],R> maker )
                    throws Exception
    {
        consume( st, '(' );

        List<Node> nodeCollector =
                new ArrayList<>();

        double weight =
                parseSplitArguments( st, nodeCollector );

        return maker.apply(
                weight,
                nodeCollector.toArray( EMPTY_NODES ) );
    }

    private static Split parseSplit(StreamTokenizer st)
            throws Exception
    {
        if  ( st.nextToken() != StreamTokenizer.TT_WORD )
            throw new ParseX( "TT_WORD", st );

        if ( st.sval.equals( Row.class.getSimpleName() ) )
            return parseSplit( st, Row::new );
        if ( st.sval.equals( Column.class.getSimpleName() ) )
            return parseSplit( st, Column::new );

        throw new ParseX( "Row|Column", st );
    }

    private static Split parseModel(Reader r) throws Exception
    {
        return parseSplit(
                new StreamTokenizer(r) );
    }

    /**
     * Example input:
     * <pre>
     *  {@code
     *  Column( weight=0.0,
     *      Row( weight=0.0,
     *          Leaf( weight=0.5, name=left ),
     *          Leaf( weight=0.0, name=right ) ),
     *      Leaf( weight=0.5, name=bottom ) )
     * }
     * <pre/>
     *
     * @return the Node root of a tree based on s.
     * @throws ParseException
     */
    public static Split parseModel(String s) throws ParseException
    {
        if ( StringUtil.isEmpty( s ) )
            return null;

        try ( var reader = new StringReader( s )  )
        {
            return parseModel( reader );
        }
        catch ( ParseException e )
        {
            throw e;
        }
        catch ( Exception e )
        {
            throw new InternalError( e );
        }
    }
}
