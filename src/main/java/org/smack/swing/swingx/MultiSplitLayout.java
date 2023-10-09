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
import java.util.ListIterator;
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
 * @author Hans Muller
 * @author Luan O'Carroll
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

    private JavaBeanProperty<NodeImpl, MultiSplitLayout> _model =
            new JavaBeanProperty<>(
                    this,
                    new LeafImpl( "default" ),
                    "model" );

//    private JavaBeanProperty<Node, MultiSplitLayout> _modelNew =
//            new JavaBeanProperty<>(
//                    this,
//                    new Leaf( 0.0, "default" ),
//                    "modelNew" );

    private JavaBeanProperty<Integer, MultiSplitLayout> _dividerSize =
            new JavaBeanProperty<>(
                    this,
                    new JSplitPane().getDividerSize(),
                    "dividerSize" );

    private boolean layoutByWeight = false;

    private LayoutMode layoutMode;
    private int _userMinSize = 20;

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
    public MultiSplitLayout(NodeImpl model)
    {
        _model.set( model );
    }

    /**
     * Create a MultiSplitLayout with a default model with a single
     * Leaf node named "default".
     *
     * @param layoutByWeight if true the layout is initialized in proportion to
     * the node weights rather than the component preferred sizes.
     * #see setModel
     */
    public MultiSplitLayout(boolean layoutByWeight)
    {
        this.layoutByWeight = layoutByWeight;
    }

    /**
     * Set the size of the child components to match the weights of the children.
     * If the components to not all specify a weight then the available layout
     * space is divided equally between the components.
     */
    public void layoutByWeight( Container parent )
    {
        doLayoutByWeight( parent );

        layoutContainer( parent );
    }

    /**
     * Set the size of the child components to match the weights of the children.
     * If the components do not all specify a weight then the available layout
     * space is divided equally between the components.
     */
    private void doLayoutByWeight( Container parent )
    {
        Dimension size = parent.getSize();
        Insets insets = parent.getInsets();
        int width = size.width - (insets.left + insets.right);
        int height = size.height - (insets.top + insets.bottom);
        Rectangle bounds = new Rectangle(insets.left, insets.top, width, height);

        final var model = getModel();

        if (model instanceof LeafImpl)
            model.setBounds(bounds);
        else if (model instanceof SplitImpl)
            doLayoutByWeight( model, bounds );
    }

    private void doLayoutByWeight( NodeImpl node, Rectangle bounds )
    {
        int width = bounds.width;
        int height = bounds.height;
        SplitImpl split = (SplitImpl)node;
        List<NodeImpl> splitChildren = split.getChildren();
        double distributableWeight = 1.0;
        int unweightedComponents = 0;
        int dividerSpace = 0;
        for( NodeImpl splitChild : splitChildren ) {
            if ( !splitChild.isVisible())
                continue;
            else if ( splitChild instanceof DividerImpl ) {
                dividerSpace += getDividerSize();
                continue;
            }

            double weight = splitChild.getWeight();
            if ( weight > 0.0 )
                distributableWeight -= weight;
            else
                unweightedComponents++;
        }

        if ( split.isRowLayout()) {
            width -= dividerSpace;
            double distributableWidth = width * distributableWeight;
            for( NodeImpl splitChild : splitChildren ) {
                if ( !splitChild.isVisible() || ( splitChild instanceof DividerImpl ))
                    continue;

                double weight = splitChild.getWeight();
                Rectangle splitChildBounds = splitChild.getBounds();
                if ( weight >= 0 )
                    splitChildBounds = new Rectangle( splitChildBounds.x, splitChildBounds.y, (int)( width * weight ), height );
                else
                    splitChildBounds = new Rectangle( splitChildBounds.x, splitChildBounds.y, (int)( distributableWidth / unweightedComponents ), height );

                if ( layoutMode == LayoutMode.USER_MIN_SIZE_LAYOUT ) {
                    splitChildBounds.setSize( Math.max( splitChildBounds.width, _userMinSize ), splitChildBounds.height );
                }

                splitChild.setBounds( splitChildBounds );

                if ( splitChild instanceof SplitImpl )
                    doLayoutByWeight( splitChild, splitChildBounds );
                else {
                    Component comp = getComponentForNode( splitChild );
                    if ( comp != null )
                        comp.setPreferredSize( splitChildBounds.getSize());
                }
            }
        }
        else {
            height -= dividerSpace;
            double distributableHeight = height * distributableWeight;
            for( NodeImpl splitChild : splitChildren ) {
                if ( !splitChild.isVisible() || ( splitChild instanceof DividerImpl ))
                    continue;

                double weight = splitChild.getWeight();
                Rectangle splitChildBounds = splitChild.getBounds();
                if ( weight >= 0 )
                    splitChildBounds = new Rectangle( splitChildBounds.x, splitChildBounds.y, width, (int)( height * weight ));
                else
                    splitChildBounds = new Rectangle( splitChildBounds.x, splitChildBounds.y, width, (int)( distributableHeight / unweightedComponents ));

                if ( layoutMode == LayoutMode.USER_MIN_SIZE_LAYOUT ) {
                    splitChildBounds.setSize( splitChildBounds.width, Math.max( splitChildBounds.height, _userMinSize ) );
                }

                splitChild.setBounds( splitChildBounds );

                if ( splitChild instanceof SplitImpl )
                    doLayoutByWeight( splitChild, splitChildBounds );
                else {
                    Component comp = getComponentForNode( splitChild );
                    if ( comp != null )
                        comp.setPreferredSize( splitChildBounds.getSize());
                }
            }
        }
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
    public NodeImpl getNodeForName( String name )
    {
        final var model = getModel();

        if ( model instanceof SplitImpl ) {
            SplitImpl split = ((SplitImpl)model);
            return getNodeForName( split, name );
        } else if (model instanceof LeafImpl) {
            if (((LeafImpl) model).getName().equals(name)) {
                return model;
            } else {
                return null;
            }
        } else {
            return null;
        }
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
     * Get the MultiSplitLayout.Node associated with a component
     * @param split the layout split that owns the requested node
     * @param comp the component being positioned by the layout
     * @return the node associated with the component
     */
    public NodeImpl getNodeForComponent( SplitImpl split, Component comp )
    {
        return getNodeForName( split, getNameForComponent( comp ));
    }

    /**
     * Get the MultiSplitLayout.Node associated with a component
     * @param split the layout split that owns the requested node
     * @param name the name used to associate a component with the layout
     * @return the node associated with the component
     */
    public NodeImpl getNodeForName( SplitImpl split, String name )
    {
        for(NodeImpl n : split.getChildren()) {
            if ( n instanceof LeafImpl ) {
                if ( ((LeafImpl)n).getName().equals( name ))
                    return n;
            }
            else if ( n instanceof SplitImpl ) {
                NodeImpl n1 = getNodeForName( (SplitImpl)n, name );
                if ( n1 != null )
                    return n1;
            }
        }
        return null;
    }

    /**
     * Is there a valid model for the layout?
     * @return true if there is a model
     */
    public boolean hasModel()
    {
        return getModel() != null;
    }

    /**
     * Return the root of the tree of Split, Leaf, and Divider nodes
     * that define this layout.
     *
     * @return the value of the model property
     * @see #setModel
     */
    public NodeImpl getModel()
    {
        return _model.get();
    }

    /**
     * Set the root of the tree of Split, Leaf, and Divider nodes
     * that define this layout.  The model can be a Split node
     * (the typical case) or a Leaf.  The default value of this
     * property is a Leaf named "default".
     *
     * @param model the root of the tree of Split, Leaf, and Divider node
     * @throws IllegalArgumentException if model is a Divider or null
     * @see #getModel
     */
    public void setModel(NodeImpl model) {
        if ((model == null) || (model instanceof DividerImpl)) {
            throw new IllegalArgumentException("invalid model");
        }
        _model.set( model );
    }

    public void setModel(Node model)
    {
        NodeImpl internalModel = model.convert( this );

        _model.set( internalModel );
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
     * Dividers in Split columns.  The default value of this property
     * is the same as for JSplitPane Dividers.
     *
     * @param dividerSize the size of dividers (pixels)
     * @throws IllegalArgumentException if dividerSize < 0
     * @see #getDividerSize
     */
    public void setDividerSize(int dividerSize)
    {
        if (dividerSize < 0)
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
        String name = getNameForComponent( child );

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

    private Dimension preferredNodeSize(NodeImpl root) {
        if (root instanceof LeafImpl) {
            return preferredComponentSize(root);
        }
        else if (root instanceof DividerImpl) {
            if ( !((DividerImpl)root).isVisible())
                return new Dimension(0,0);
            int divSize = getDividerSize();
            return new Dimension(divSize, divSize);
        }
        else {
            SplitImpl split = (SplitImpl)root;
            List<NodeImpl> splitChildren = split.getChildren();
            int width = 0;
            int height = 0;
            if (split.isRowLayout()) {
                for(NodeImpl splitChild : splitChildren) {
                    if ( !splitChild.isVisible())
                        continue;
                    Dimension size = preferredNodeSize(splitChild);
                    width += size.width;
                    height = Math.max(height, size.height);
                }
            }
            else {
                for(NodeImpl splitChild : splitChildren) {
                    if ( !splitChild.isVisible())
                        continue;
                    Dimension size = preferredNodeSize(splitChild);
                    width = Math.max(width, size.width);
                    height += size.height;
                }
            }
            return new Dimension(width, height);
        }
    }

    /**
     * Get the minimum size of this node. Sums the minumum sizes of rows or
     * columns to get the overall minimum size for the layout node, including the
     * dividers.
     * @param root the node whose size is required.
     * @return the minimum size.
     */
    public Dimension minimumNodeSize(NodeImpl root) {
        assert( root.isVisible );
        if (root instanceof LeafImpl) {
            if ( layoutMode == LayoutMode.NO_MIN_SIZE_LAYOUT )
                return new Dimension(0, 0);

            Component child = childForNode(root);
            return ((child != null) && child.isVisible() ) ? child.getMinimumSize() : new Dimension(0, 0);
        }
        else if (root instanceof DividerImpl) {
            if ( !((DividerImpl)root).isVisible()  )
                return new Dimension(0,0);
            int divSize = getDividerSize();
            return new Dimension(divSize, divSize);
        }
        else {
            SplitImpl split = (SplitImpl)root;
            List<NodeImpl> splitChildren = split.getChildren();
            int width = 0;
            int height = 0;
            if (split.isRowLayout()) {
                for(NodeImpl splitChild : splitChildren) {
                    if ( !splitChild.isVisible())
                        continue;
                    Dimension size = minimumNodeSize(splitChild);
                    width += size.width;
                    height = Math.max(height, size.height);
                }
            }
            else {
                for(NodeImpl splitChild : splitChildren) {
                    if ( !splitChild.isVisible())
                        continue;
                    Dimension size = minimumNodeSize(splitChild);
                    width = Math.max(width, size.width);
                    height += size.height;
                }
            }
            return new Dimension(width, height);
        }
    }

    /**
     * Get the maximum size of this node. Sums the minimum sizes of rows or
     * columns to get the overall maximum size for the layout node, including the
     * dividers.
     * @param root the node whose size is required.
     * @return the minimum size.
     */
    public Dimension maximumNodeSize(NodeImpl root) {
        assert( root.isVisible );
        if (root instanceof LeafImpl) {
            Component child = childForNode(root);
            return ((child != null) && child.isVisible() ) ? child.getMaximumSize() : new Dimension(0, 0);
        }
        else if (root instanceof DividerImpl) {
            if ( !((DividerImpl)root).isVisible()  )
                return new Dimension(0,0);
            int divSize = getDividerSize();
            return new Dimension(divSize, divSize);
        }
        else {
            SplitImpl split = (SplitImpl)root;
            List<NodeImpl> splitChildren = split.getChildren();
            int width = Integer.MAX_VALUE;
            int height = Integer.MAX_VALUE;
            if (split.isRowLayout()) {
                for(NodeImpl splitChild : splitChildren) {
                    if ( !splitChild.isVisible())
                        continue;
                    Dimension size = maximumNodeSize(splitChild);
                    width += size.width;
                    height = Math.min(height, size.height);
                }
            }
            else {
                for(NodeImpl splitChild : splitChildren) {
                    if ( !splitChild.isVisible())
                        continue;
                    Dimension size = maximumNodeSize(splitChild);
                    width = Math.min(width, size.width);
                    height += size.height;
                }
            }
            return new Dimension(width, height);
        }
    }

    private Dimension sizeWithInsets(Container parent, Dimension size) {
        Insets insets = parent.getInsets();
        int width = size.width + insets.left + insets.right;
        int height = size.height + insets.top + insets.bottom;
        return new Dimension(width, height);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Dimension size = preferredNodeSize(getModel());
        return sizeWithInsets(parent, size);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        Dimension size = minimumNodeSize(getModel());
        return sizeWithInsets(parent, size);
    }


    private Rectangle boundsWithYandHeight(Rectangle bounds, double y, double height) {
        Rectangle r = new Rectangle();
        r.setBounds((int)(bounds.getX()), (int)y, (int)(bounds.getWidth()), (int)height);
        return r;
    }

    private Rectangle boundsWithXandWidth(Rectangle bounds, double x, double width) {
        Rectangle r = new Rectangle();
        r.setBounds((int)x, (int)(bounds.getY()), (int)width, (int)(bounds.getHeight()));
        return r;
    }


    private void minimizeSplitBounds(SplitImpl split, Rectangle bounds) {
        assert ( split.isVisible());
        Rectangle splitBounds = new Rectangle(bounds.x, bounds.y, 0, 0);
        List<NodeImpl> splitChildren = split.getChildren();
        NodeImpl lastChild = null;
        int lastVisibleChildIdx = splitChildren.size();
        do  {
            lastVisibleChildIdx--;
            lastChild = splitChildren.get( lastVisibleChildIdx );
        } while (( lastVisibleChildIdx > 0 ) && !lastChild.isVisible());

        if ( !lastChild.isVisible())
            return;
        if ( lastVisibleChildIdx >= 0 ) {
            Rectangle lastChildBounds = lastChild.getBounds();
            if (split.isRowLayout()) {
                int lastChildMaxX = lastChildBounds.x + lastChildBounds.width;
                splitBounds.add(lastChildMaxX, bounds.y + bounds.height);
            }
            else {
                int lastChildMaxY = lastChildBounds.y + lastChildBounds.height;
                splitBounds.add(bounds.x + bounds.width, lastChildMaxY);
            }
        }
        split.setBounds(splitBounds);
    }


    private void layoutShrink(SplitImpl split, Rectangle bounds) {
        Rectangle splitBounds = split.getBounds();
        ListIterator<NodeImpl> splitChildren = split.getChildren().listIterator();
        //Node lastWeightedChild = split.lastWeightedChild();

        if (split.isRowLayout()) {
            int totalWidth = 0;          // sum of the children's widths
            int minWeightedWidth = 0;    // sum of the weighted childrens' min widths
            int totalWeightedWidth = 0;  // sum of the weighted childrens' widths
            for(NodeImpl splitChild : split.getChildren()) {
                if ( !splitChild.isVisible())
                    continue;
                int nodeWidth = splitChild.getBounds().width;
                int nodeMinWidth = 0;
                if (( layoutMode == LayoutMode.USER_MIN_SIZE_LAYOUT ) && !( splitChild instanceof DividerImpl ))
                    nodeMinWidth = _userMinSize;
                else if ( layoutMode == LayoutMode.DEFAULT_LAYOUT )
                    nodeMinWidth = Math.min(nodeWidth, minimumNodeSize(splitChild).width);
                totalWidth += nodeWidth;
                if (splitChild.getWeight() > 0.0) {
                    minWeightedWidth += nodeMinWidth;
                    totalWeightedWidth += nodeWidth;
                }
            }

            double x = bounds.getX();
            double extraWidth = splitBounds.getWidth() - bounds.getWidth();
            double availableWidth = extraWidth;
            boolean onlyShrinkWeightedComponents =
                    (totalWeightedWidth - minWeightedWidth) > extraWidth;

                    while(splitChildren.hasNext()) {
                        NodeImpl splitChild = splitChildren.next();
                        if ( !splitChild.isVisible()) {
                            if ( splitChildren.hasNext())
                                splitChildren.next();
                            continue;
                        }
                        Rectangle splitChildBounds = splitChild.getBounds();
                        double minSplitChildWidth = 0.0;
                        if (( layoutMode == LayoutMode.USER_MIN_SIZE_LAYOUT ) && !( splitChild instanceof DividerImpl ))
                            minSplitChildWidth = _userMinSize;
                        else if ( layoutMode == LayoutMode.DEFAULT_LAYOUT )
                            minSplitChildWidth = minimumNodeSize(splitChild).getWidth();
                        double splitChildWeight = (onlyShrinkWeightedComponents)
                                ? splitChild.getWeight()
                                        : (splitChildBounds.getWidth() / totalWidth);

                        if (!splitChildren.hasNext()) {
                            double newWidth = Math.max(minSplitChildWidth, bounds.getMaxX() - x);
                            Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, newWidth);
                            layout2(splitChild, newSplitChildBounds);
                        }
                        if ( splitChild.isVisible()) {
                            if ((availableWidth > 0.0) && (splitChildWeight > 0.0)) {
                                double oldWidth = splitChildBounds.getWidth();
                                double newWidth;
                                if ( splitChild instanceof DividerImpl ) {
                                    newWidth = getDividerSize();
                                }
                                else {
                                    double allocatedWidth = Math.rint(splitChildWeight * extraWidth);
                                    newWidth = Math.max(minSplitChildWidth, oldWidth - allocatedWidth);
                                }
                                Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, newWidth);
                                layout2(splitChild, newSplitChildBounds);
                                availableWidth -= (oldWidth - splitChild.getBounds().getWidth());
                            }
                            else {
                                double existingWidth = splitChildBounds.getWidth();
                                Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, existingWidth);
                                layout2(splitChild, newSplitChildBounds);
                            }
                            x = splitChild.getBounds().getMaxX();
                        }
                    }
        }

        else {
            int totalHeight = 0;          // sum of the children's heights
            int minWeightedHeight = 0;    // sum of the weighted childrens' min heights
            int totalWeightedHeight = 0;  // sum of the weighted childrens' heights
            for(NodeImpl splitChild : split.getChildren()) {
                if ( !splitChild.isVisible())
                    continue;
                int nodeHeight = splitChild.getBounds().height;
                int nodeMinHeight = 0;
                if (( layoutMode == LayoutMode.USER_MIN_SIZE_LAYOUT ) && !( splitChild instanceof DividerImpl ))
                    nodeMinHeight = _userMinSize;
                else if ( layoutMode == LayoutMode.DEFAULT_LAYOUT )
                    nodeMinHeight = Math.min(nodeHeight, minimumNodeSize(splitChild).height);
                totalHeight += nodeHeight;
                if (splitChild.getWeight() > 0.0) {
                    minWeightedHeight += nodeMinHeight;
                    totalWeightedHeight += nodeHeight;
                }
            }

            double y = bounds.getY();
            double extraHeight = splitBounds.getHeight() - bounds.getHeight();
            double availableHeight = extraHeight;
            boolean onlyShrinkWeightedComponents =
                    (totalWeightedHeight - minWeightedHeight) > extraHeight;

                    while(splitChildren.hasNext()) {
                        NodeImpl splitChild = splitChildren.next();
                        if ( !splitChild.isVisible()) {
                            if ( splitChildren.hasNext())
                                splitChildren.next();
                            continue;
                        }
                        Rectangle splitChildBounds = splitChild.getBounds();
                        double minSplitChildHeight = 0.0;
                        if (( layoutMode == LayoutMode.USER_MIN_SIZE_LAYOUT ) && !( splitChild instanceof DividerImpl ))
                            minSplitChildHeight = _userMinSize;
                        else if ( layoutMode == LayoutMode.DEFAULT_LAYOUT )
                            minSplitChildHeight = minimumNodeSize(splitChild).getHeight();
                        double splitChildWeight = (onlyShrinkWeightedComponents)
                                ? splitChild.getWeight()
                                        : (splitChildBounds.getHeight() / totalHeight);

                        // If this split child is the last visible node it should all the
                        // remaining space
                        if ( !hasMoreVisibleSiblings( splitChild )) {
                            double oldHeight = splitChildBounds.getHeight();
                            double newHeight;
                            if ( splitChild instanceof DividerImpl ) {
                                newHeight = getDividerSize();
                            }
                            else {
                                newHeight = Math.max(minSplitChildHeight, bounds.getMaxY() - y);
                            }
                            Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, newHeight);
                            layout2(splitChild, newSplitChildBounds);
                            availableHeight -= (oldHeight - splitChild.getBounds().getHeight());
                        }
                        else /*if ( splitChild.isVisible()) {*/
                            if ((availableHeight > 0.0) && (splitChildWeight > 0.0)) {
                                double newHeight;
                                double oldHeight = splitChildBounds.getHeight();
                                // Prevent the divider from shrinking
                                if ( splitChild instanceof DividerImpl ) {
                                    newHeight = getDividerSize();
                                }
                                else {
                                    double allocatedHeight = Math.rint(splitChildWeight * extraHeight);
                                    newHeight = Math.max(minSplitChildHeight, oldHeight - allocatedHeight);
                                }
                                Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, newHeight);
                                layout2(splitChild, newSplitChildBounds);
                                availableHeight -= (oldHeight - splitChild.getBounds().getHeight());
                            }
                            else {
                                double existingHeight = splitChildBounds.getHeight();
                                Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, existingHeight);
                                layout2(splitChild, newSplitChildBounds);
                            }
                        y = splitChild.getBounds().getMaxY();
                    }
        }

        /* The bounds of the Split node root are set to be
         * big enough to contain all of its children. Since
         * Leaf children can't be reduced below their
         * (corresponding java.awt.Component) minimum sizes,
         * the size of the Split's bounds maybe be larger than
         * the bounds we were asked to fit within.
         */
        minimizeSplitBounds(split, bounds);
    }

    /**
     * Check if the specified node has any following visible siblings
     * @param splitChild the node to check
     * @param true if there are visible children following
     */
    private boolean hasMoreVisibleSiblings( NodeImpl splitChild ) {
        NodeImpl next = splitChild.nextSibling();
        if ( next == null )
            return false;

        do {
            if ( next.isVisible())
                return true;
            next  = next.nextSibling();
        } while ( next != null );

        return false;
    }

    private void layoutGrow(SplitImpl split, Rectangle bounds) {
        Rectangle splitBounds = split.getBounds();
        ListIterator<NodeImpl> splitChildren = split.getChildren().listIterator();
        NodeImpl lastWeightedChild = split.lastWeightedChild();

        /* Layout the Split's child Nodes' along the X axis.  The bounds
         * of each child will have the same y coordinate and height as the
         * layoutGrow() bounds argument.  Extra width is allocated to the
         * to each child with a non-zero weight:
         *     newWidth = currentWidth + (extraWidth * splitChild.getWeight())
         * Any extraWidth "left over" (that's availableWidth in the loop
         * below) is given to the last child.  Note that Dividers always
         * have a weight of zero, and they're never the last child.
         */
        if (split.isRowLayout()) {
            double x = bounds.getX();
            double extraWidth = bounds.getWidth() - splitBounds.getWidth();
            double availableWidth = extraWidth;

            while(splitChildren.hasNext()) {
                NodeImpl splitChild = splitChildren.next();
                if ( !splitChild.isVisible()) {
                    continue;
                }
                Rectangle splitChildBounds = splitChild.getBounds();
                double splitChildWeight = splitChild.getWeight();

                if ( !hasMoreVisibleSiblings( splitChild )) {
                    double newWidth = bounds.getMaxX() - x;
                    Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, newWidth);
                    layout2(splitChild, newSplitChildBounds);
                }
                else if ((availableWidth > 0.0) && (splitChildWeight > 0.0)) {
                    double allocatedWidth = (splitChild.equals(lastWeightedChild))
                            ? availableWidth
                                    : Math.rint(splitChildWeight * extraWidth);
                    double newWidth = splitChildBounds.getWidth() + allocatedWidth;
                    Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, newWidth);
                    layout2(splitChild, newSplitChildBounds);
                    availableWidth -= allocatedWidth;
                }
                else {
                    double existingWidth = splitChildBounds.getWidth();
                    Rectangle newSplitChildBounds = boundsWithXandWidth(bounds, x, existingWidth);
                    layout2(splitChild, newSplitChildBounds);
                }
                x = splitChild.getBounds().getMaxX();
            }
        }

        /* Layout the Split's child Nodes' along the Y axis.  The bounds
         * of each child will have the same x coordinate and width as the
         * layoutGrow() bounds argument.  Extra height is allocated to the
         * to each child with a non-zero weight:
         *     newHeight = currentHeight + (extraHeight * splitChild.getWeight())
         * Any extraHeight "left over" (that's availableHeight in the loop
         * below) is given to the last child.  Note that Dividers always
         * have a weight of zero, and they're never the last child.
         */
        else {
            double y = bounds.getY();
            double extraHeight = bounds.getHeight() - splitBounds.getHeight();
            double availableHeight = extraHeight;

            while(splitChildren.hasNext()) {
                NodeImpl splitChild = splitChildren.next();
                if ( !splitChild.isVisible()) {
                    continue;
                }
                Rectangle splitChildBounds = splitChild.getBounds();
                double splitChildWeight = splitChild.getWeight();

                if (!splitChildren.hasNext()) {
                    double newHeight = bounds.getMaxY() - y;
                    Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, newHeight);
                    layout2(splitChild, newSplitChildBounds);
                }
                else if ((availableHeight > 0.0) && (splitChildWeight > 0.0)) {
                    double allocatedHeight = (splitChild.equals(lastWeightedChild))
                            ? availableHeight
                                    : Math.rint(splitChildWeight * extraHeight);
                    double newHeight = splitChildBounds.getHeight() + allocatedHeight;
                    Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, newHeight);
                    layout2(splitChild, newSplitChildBounds);
                    availableHeight -= allocatedHeight;
                }
                else {
                    double existingHeight = splitChildBounds.getHeight();
                    Rectangle newSplitChildBounds = boundsWithYandHeight(bounds, y, existingHeight);
                    layout2(splitChild, newSplitChildBounds);
                }
                y = splitChild.getBounds().getMaxY();
            }
        }
    }


    /* Second pass of the layout algorithm: branch to layoutGrow/Shrink
     * as needed.
     */
    private void layout2(NodeImpl root, Rectangle bounds) {
        if (root instanceof LeafImpl) {
            Component child = childForNode(root);
            if (child != null) {
                child.setBounds(bounds);
            }
            root.setBounds(bounds);
        }
        else if (root instanceof DividerImpl) {
            root.setBounds(bounds);
        }
        else if (root instanceof SplitImpl) {
            SplitImpl split = (SplitImpl)root;
            boolean grow = split.isRowLayout()
                    ? (split.getBounds().width <= bounds.width)
                            : (split.getBounds().height <= bounds.height);
            if (grow) {
                layoutGrow(split, bounds);
                root.setBounds(bounds);
            }
            else {
                layoutShrink(split, bounds);
            }
        }
    }


//    /* First pass of the layout algorithm.
//     *
//     * If the Dividers are "floating" then set the bounds of each
//     * node to accomodate the preferred size of all of the
//     * Leaf's java.awt.Components.  Otherwise, just set the bounds
//     * of each Leaf/Split node so that it's to the left of (for
//     * Split.isRowLayout() Split children) or directly above
//     * the Divider that follows.
//     *
//     * This pass sets the bounds of each Node in the layout model.  It
//     * does not resize any of the parent Container's
//     * (java.awt.Component) children.  That's done in the second pass,
//     * see layoutGrow() and layoutShrink().
//     */
//    private void layout1(NodeImpl root, Rectangle bounds) {
//        if (root instanceof LeafImpl) {
//            root.setBounds(bounds);
//        }
//        else if (root instanceof SplitImpl) {
//            SplitImpl split = (SplitImpl)root;
//            Iterator<NodeImpl> splitChildren = split.getChildren().iterator();
//            Rectangle childBounds = null;
//            int divSize = getDividerSize();
//            boolean initSplit = false;
//
//
//            /* Layout the Split's child Nodes' along the X axis.  The bounds
//             * of each child will have the same y coordinate and height as the
//             * layout1() bounds argument.
//             *
//             * Note: the column layout code - that's the "else" clause below
//             * this if, is identical to the X axis (rowLayout) code below.
//             */
//            if (split.isRowLayout()) {
//                double x = bounds.getX();
//                while(splitChildren.hasNext()) {
//                    NodeImpl splitChild = splitChildren.next();
//                    if ( !splitChild.isVisible()) {
//                        if ( splitChildren.hasNext())
//                            splitChildren.next();
//                        continue;
//                    }
//                    DividerImpl dividerChild =
//                            (splitChildren.hasNext()) ? (DividerImpl)(splitChildren.next()) : null;
//
//                    double childWidth = 0.0;
//
//                    if ((dividerChild != null) && dividerChild.isVisible()) {
//                        double cw = dividerChild.getBounds().getX() - x;
//                        if ( cw > 0.0 )
//                            childWidth = cw;
//                        else {
//                            childWidth = preferredNodeSize(splitChild).getWidth();
//                            initSplit = true;
//                        }
//                    }
//                    else {
//                        childWidth = split.getBounds().getMaxX() - x;
//                    }
//                    childBounds = boundsWithXandWidth(bounds, x, childWidth);
//                    layout1(splitChild, childBounds);
//
//                    if (( initSplit || false) && (dividerChild != null) && dividerChild.isVisible()) {
//                        double dividerX = childBounds.getMaxX();
//                        Rectangle dividerBounds;
//                        dividerBounds = boundsWithXandWidth(bounds, dividerX, divSize);
//                        dividerChild.setBounds(dividerBounds);
//                    }
//                    if ((dividerChild != null) && dividerChild.isVisible()) {
//                        x = dividerChild.getBounds().getMaxX();
//                    }
//                }
//            }
//
//            /* Layout the Split's child Nodes' along the Y axis.  The bounds
//             * of each child will have the same x coordinate and width as the
//             * layout1() bounds argument.  The algorithm is identical to what's
//             * explained above, for the X axis case.
//             */
//            else {
//                double y = bounds.getY();
//                while(splitChildren.hasNext())
//                {
//                    NodeImpl splitChild = splitChildren.next();
//
//                    if ( !splitChild.isVisible()) {
//                        continue;
//                    }
//
//                    DividerImpl dividerChild = (splitChildren.hasNext()) ?
//                            (DividerImpl)(splitChildren.next()) :
//                                null;
//
//                    double childHeight = 0.0;
//                    if ((dividerChild != null) && dividerChild.isVisible()) {
//                        double cy = dividerChild.getBounds().getY() - y;
//                        if ( cy > 0.0 )
//                            childHeight = cy;
//                        else {
//                            childHeight = preferredNodeSize(splitChild).getHeight();
//                            initSplit = true;
//                        }
//                    }
//                    else {
//                        childHeight = split.getBounds().getMaxY() - y;
//                    }
//
//                    childBounds = boundsWithYandHeight(bounds, y, childHeight);
//                    layout1(splitChild, childBounds);
//
//                    if (( initSplit || false) && (dividerChild != null) && dividerChild.isVisible()) {
//                        double dividerY = childBounds.getMaxY();
//                        Rectangle dividerBounds = boundsWithYandHeight(bounds, dividerY, divSize);
//                        dividerChild.setBounds(dividerBounds);
//                    }
//                    if ((dividerChild != null) && dividerChild.isVisible()) {
//                        y = dividerChild.getBounds().getMaxY();
//                    }
//                }
//            }
//            /* The bounds of the Split node root are set to be just
//             * big enough to contain all of its children, but only
//             * along the axis it's allocating space on.  That's
//             * X for rows, Y for columns.  The second pass of the
//             * layout algorithm - see layoutShrink()/layoutGrow()
//             * allocates extra space.
//             */
//            minimizeSplitBounds(split, bounds);
//        }
//    }

    /**
     * Get the layout mode
     * @return current layout mode
     */
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
    public void setLayoutMode( LayoutMode layoutMode )
    {
        this.layoutMode = layoutMode;
    }

    /**
     * Get the minimum node size
     * @return the minimum size
     */
    public int getUserMinSize()
    {
        return _userMinSize;
    }

    /**
     * Set the user defined minimum size support in the USER_MIN_SIZE_LAYOUT
     * layout mode.
     * @param minSize the new minimum size
     */
    public void setUserMinSize( int minSize )
    {
        _userMinSize = minSize;
    }

    /**
     * Get the layoutByWeight falg. If the flag is true the layout initializes
     * itself using the model weights
     * @return the layoutByWeight
     */
    public boolean getLayoutByWeight()
    {
        return layoutByWeight;
    }

    /**
     * Sset the layoutByWeight falg. If the flag is true the layout initializes
     * itself using the model weights
     * @param state the new layoutByWeight to set
     */
    public void setLayoutByWeight( boolean state )
    {
        layoutByWeight = state;
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

//    @Deprecated
//    private void checkLayout(NodeImpl root) {
//        if (root instanceof SplitImpl) {
//            SplitImpl split = (SplitImpl)root;
//            if (split.getChildren().size() <= 2) {
//                throwInvalidLayout("Split must have > 2 children", root);
//            }
//            Iterator<NodeImpl> splitChildren = split.getChildren().iterator();
//            double weight = 0.0;
//            while(splitChildren.hasNext())       {
//                NodeImpl splitChild = splitChildren.next();
//                if ( !splitChild.isVisible()) {
//                    if ( splitChildren.hasNext())
//                        splitChildren.next();
//                    continue;
//                }
//                if (splitChild instanceof DividerImpl) {
//                    continue;
//                    //throwInvalidLayout("expected a Split or Leaf Node", splitChild);
//                }
//                if (splitChildren.hasNext()) {
//                    NodeImpl dividerChild = splitChildren.next();
//                    if (!(dividerChild instanceof DividerImpl)) {
//                        throwInvalidLayout("expected a Divider Node", dividerChild);
//                    }
//                }
//                weight += splitChild.getWeight();
//                checkLayout(splitChild);
//            }
//            if (weight > 1.0) {
//                throwInvalidLayout("Split children's total weight > 1.0", root);
//            }
//        }
//    }
    private void validateLayout( NodeImpl root )
    {
        root.validate( new HashSet<>() );
    }

//    /**
//     * Compute the bounds of all of the Split/Divider/Leaf Nodes in
//     * the layout model, and then set the bounds of each child component
//     * with a matching Leaf Node.
//     */
//    public void layoutContainerOld(Container parent)
//    {
//        if ( layoutByWeight )
//            doLayoutByWeight( parent );
//
//        checkLayout(getModel());
//        Insets insets = parent.getInsets();
//        Dimension size = parent.getSize();
//        int width = size.width - (insets.left + insets.right);
//        int height = size.height - (insets.top + insets.bottom);
//        Rectangle bounds = new Rectangle( insets.left, insets.top, width, height);
//        layout1(getModel(), bounds);
//        layout2(getModel(), bounds);
//    }

    /**
     *
     * @param node The node to layout.
     * @param bounds
     */
    private void _performLayout( NodeImpl node, Rectangle bounds )
    {
        node.layout( bounds, this );
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
            if ( c.weight == 0.0 )
                c.weight = unsetPercentage;
        }

        double totalWeight = 0.0;
        for ( var c : children )
        {
            if ( c.weight > 0.0 )
                totalWeight += c.weight;
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
            return (divider.getBounds().contains(x, y)) ? divider : null;
        }
        else if (root instanceof SplitImpl) {
            SplitImpl split = (SplitImpl)root;
            for(NodeImpl child : split.getChildren()) {
                if ( !child.isVisible())
                    continue;
                if (child.getBounds().contains(x, y)) {
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
    public DividerImpl dividerAt(int x, int y) {
        return dividerAt(getModel(), x, y);
    }

    private boolean nodeOverlapsRectangle(NodeImpl node, Rectangle r2) {
        Rectangle r1 = node.getBounds();
        return
                (r1.x <= (r2.x + r2.width)) && ((r1.x + r1.width) >= r2.x) &&
                (r1.y <= (r2.y + r2.height)) && ((r1.y + r1.height) >= r2.y);
    }

    private List<DividerImpl> dividersThatOverlap(NodeImpl root, Rectangle r) {
        if (nodeOverlapsRectangle(root, r) && (root instanceof SplitImpl)) {
            List<DividerImpl> dividers = new ArrayList<DividerImpl>();
            for(NodeImpl child : ((SplitImpl)root).getChildren()) {
                if (child instanceof DividerImpl) {
                    if (nodeOverlapsRectangle(child, r)) {
                        dividers.add((DividerImpl)child);
                    }
                }
                else if (child instanceof SplitImpl) {
                    dividers.addAll(dividersThatOverlap(child, r));
                }
            }
            return dividers;
        }
        else {
            return Collections.emptyList();
        }
    }

    /**
     * Return the Dividers whose bounds overlap the specified
     * Rectangle.
     *
     * @param r target Rectangle
     * @return the Dividers that overlap r
     * @throws IllegalArgumentException if the Rectangle is null
     */
    public List<DividerImpl> dividersThatOverlap(Rectangle r) {
        if (r == null) {
            throw new IllegalArgumentException("null Rectangle");
        }
        return dividersThatOverlap(getModel(), r);
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
    }

    /**
     * Base class for the nodes that model a MultiSplitLayout.
     */
    public static abstract class NodeImpl implements Serializable {

        private int _parentIdx;

        private SplitImpl _parent = null;

        private final Rectangle _bounds = new Rectangle();

        private double weight = 0.0;

        private boolean isVisible = true;

        public void setVisible( boolean b ) {
            isVisible = b;
        }

        public void setParentIdx( int idx )
        {
            _parentIdx = idx;
        }
        public int getParentIdx()
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
        public boolean isVisible() {
            return isVisible;
        }

        /**
         * Returns the Split parent of this Node, or null.
         *
         * @return the value of the parent property.
         * @see #setParent
         */
        public SplitImpl getParent()
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
        public void setParent(SplitImpl parent) {
            JavaUtil.Assert( _parent == null );
            _parent = parent;
        }

        /**
         * @return A copy of the value of the bounds property.
         * @see #setBounds
         */
        public Rectangle getBounds() {
            return new Rectangle(this._bounds);
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

        /**
         * @return The bounds property.
         */
        protected Rectangle bounds()
        {
            return _bounds;
        }

        /**
         * Set the bounding Rectangle for this node.  The value of
         * bounds may not be null.  The default value of bounds
         * is equal to <code>new Rectangle(0,0,0,0)</code>.
         *
         * @param bounds the new value of the bounds property
         * @throws NullPointerException if bounds is null
         * @see #getBounds
         */
        public void setBounds(Rectangle bounds) {
            _bounds.setBounds( Objects.requireNonNull( bounds ) );
        }

        /**
         * Value between 0.0 and 1.0 used to compute how much space
         * to add to this sibling when the layout grows or how
         * much to reduce when the layout shrinks.
         *
         * @return the value of the weight property
         * @see #setWeight
         */
        public double getWeight() { return weight; }
        /**
         * Fluent API.
         * @param weight
         * @return
         */
        public NodeImpl weight( double weight )
        {
            setWeight( weight );
            return this;
        }

        /**
         * The weight property is a between 0.0 and 1.0 used to
         * compute how much space to add to this sibling when the
         * layout grows or how much to reduce when the layout shrinks.
         * If rowLayout is true then this node's width grows
         * or shrinks by (extraSpace * weight).  If rowLayout is false,
         * then the node's height is changed.  The default value
         * of weight is 0.0.
         *
         * @param weight a double between 0.0 and 1.0
         * @see #getWeight
         * @see MultiSplitLayout#layoutContainer
         * @throws IllegalArgumentException if weight is not between 0.0 and 1.0
         */
        public void setWeight(double weight) {
            if ((weight < 0.0)|| (weight > 1.0)) {
                throw new IllegalArgumentException("invalid weight");
            }
            this.weight = weight;
        }

        private NodeImpl siblingAtOffset(int offset) {
            SplitImpl p = getParent();
            if (p == null) { return null; }
            List<NodeImpl> siblings = p.getChildren();
            int index = siblings.indexOf(this);
            if (index == -1) { return null; }
            index += offset;
            return ((index > -1) && (index < siblings.size())) ? siblings.get(index) : null;
        }
        private NodeImpl _siblingAtOffset(int offset)
        {
            SplitImpl p = getParent();

            if (p == null)
                return null;

            var siblings = p.getChildren();

            JavaUtil.Assert( siblings != null );

            int siblingIdx = _parentIdx + offset;

            JavaUtil.Assert( siblingIdx >= 0 );
            JavaUtil.Assert( siblingIdx < siblings.size() );

            return siblings.get( siblingIdx );
        }

        /**
         * Return the Node that comes after this one in the parent's
         * list of children, or null.  If this node's parent is null,
         * or if it's the last child, then return null.
         *
         * @return the Node that comes after this one in the parent's list of children.
         * @see #previousSibling
         * @see #getParent
         */
        public NodeImpl nextSibling() {
            return siblingAtOffset(+1);
        }
        public NodeImpl next() {
            return _siblingAtOffset(1);
        }

        /**
         * Return the Node that comes before this one in the parent's
         * list of children, or null.  If this node's parent is null,
         * or if it's the last child, then return null.
         *
         * @return the Node that comes before this one in the parent's list of children.
         * @see #nextSibling
         * @see #getParent
         */
        public NodeImpl previousSibling() {
            return siblingAtOffset(-1);
        }
        public NodeImpl previous() {
            return _siblingAtOffset(-1);
        }

        abstract void validate( Set<String> nameCollector );

        abstract void layout(
                Rectangle bounds,
                MultiSplitLayout host );
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
            setRowLayout( true );
        }

        /**
         * Returns true if this Split's children are to be
         * laid out in a row: all the same height, left edge
         * equal to the previous Node's right edge.  If false,
         * children are laid on in a column.
         *
         * @return the value of the rowLayout property.
         * @see #setRowLayout
         */
        @Override
        public final boolean isRowLayout() { return true; }

        @Override
        public void subLayout( Rectangle bounds, MultiSplitLayout host )
        {
            setBounds( bounds );

            var children =  _completeWeights( getChildren2() );

            final int dividerCount =
                    children.size() -1;
            final int netRowWidth =
                    bounds.width - dividerCount * host.getDividerSize();

            double currentPosition = 0.0;

            for ( int i = 0 ; i < getChildren().size() ; i++ )
            {
                // Toggle between splits and dividers.
                if ( MathUtil.isEven( i ) )
                {
                    var c = getChildren().get( i );
                    double w = c.getWeight() * netRowWidth;

                    Rectangle subBounds = new Rectangle(
                            MathUtil.round( currentPosition ),
                            bounds.y,
                            MathUtil.round( w ),
                            bounds.height );

                    c.layout(
                            subBounds,
                            host );

                    currentPosition += w;
                }
                else
                {
                    DividerImpl divider = (DividerImpl)getChildren().get( i );

                    Rectangle subBounds = new Rectangle(
                            MathUtil.round( currentPosition ),
                            bounds.y,
                            host.getDividerSize(),
                            bounds.height );

                    divider.setBounds( subBounds );
                    currentPosition += host.getDividerSize();

                }
            }

            if ( extent() != bounds.width )
            {
                // Correct the node positions.
                int error = extent() - bounds.width;

                LOG.warning( String.format(
                        "Expected width %d not %d.  Error=%d", extent(), bounds.width, error ) );

                for ( int i = children.size()-1 ; error > 0 ; i-- )
                {
                    NodeImpl c = children.get( i );
                    c.bounds().x -= error;
                    error--;
                }
            }

            if ( extent() != bounds.width )
            {
                LOG.warning( String.format(
                        "Corrected width %d not %d.", extent(), bounds.width ) );
            }
        }

        @Override
        public int extent()
        {
            var children =
                    getChildren2();
            var lastNode =
                    children.get(
                            children.size()-1 );
            var bounds =
                    lastNode._bounds;

            return bounds.x + bounds.width;
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
                c.setWeight( c._width() / toDistribute );
        }
    }

    public static class ColumnImpl extends SplitImpl {
//        public ColumnImpl() {
//        }

        public ColumnImpl( Column column, MultiSplitLayout host )
        {
            super( column, host );
        }

        public ColumnImpl(NodeImpl... children) {
            super(children);
            setRowLayout( false );
        }

        /**
         * Returns true if the this Split's children are to be
         * laid out in a row: all the same height, left edge
         * equal to the previous Node's right edge.  If false,
         * children are laid on in a column.
         *
         * @return the value of the rowLayout property.
         * @see #setRowLayout
         */
        @Override
        public final boolean isRowLayout() { return false; }

        @Override
        public void subLayout( Rectangle bounds, MultiSplitLayout host )
        {
            setBounds( bounds );

            var children =  _completeWeights( getChildren2() );

            final int dividerCount =
                    children.size() -1;
            final int netRowHeigth =
                    bounds.height - dividerCount * host.getDividerSize();

            double currentPosition = 0.0;

            for ( int i = 0 ; i < getChildren().size() ; i++ )
            {
                // Toggle between splits and dividers.
                if ( MathUtil.isEven( i ) )
                {
                    var c = getChildren().get( i );
                    double h = c.getWeight() * netRowHeigth;

                    Rectangle subBounds = new Rectangle(
                            bounds.x, // MathUtil.round( currentPosition ),
                            MathUtil.round( currentPosition ), // bounds.y,
                            bounds.width, // MathUtil.round( w ),
                            MathUtil.round( h ) ); // bounds.height );

                    c.layout(
                            subBounds,
                            host );

                    currentPosition += h;
                }
                else
                {
                    DividerImpl divider = (DividerImpl)getChildren().get( i );

                    Rectangle subBounds = new Rectangle(
                            bounds.x, // MathUtil.round( currentPosition ),
                            MathUtil.round( currentPosition ),
                            bounds.width,
                            host.getDividerSize()
                            );

                    divider.setBounds( subBounds );
                    currentPosition += host.getDividerSize();

                }
            }

            if ( extent() != bounds.width )
            {
                // Correct the node positions.
                int error = extent() - bounds.height;

                LOG.warning( String.format(
                        "Expected width %d not %d.  Error=%d", extent(), bounds.height, error ) );

                for ( int i = children.size()-1 ; error > 0 ; i-- )
                {
                    NodeImpl c = children.get( i );
                    c.bounds().y -= error;
                    error--;
                }
            }

            if ( extent() != bounds.height )
            {
                LOG.warning( String.format(
                        "Corrected width %d not %d.", extent(), bounds.height ) );
            }
        }

        @Override
        public int extent()
        {
            var children =
                    getChildren2();
            var lastNode =
                    children.get(
                            children.size()-1 );
            var bounds =
                    lastNode._bounds;

            return bounds.y + bounds.height;
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
                c.setWeight( c._height() / toDistribute );
        }
    }

    /**
     * Defines a vertical or horizontal subdivision into two or more tiles.
     */
    public static abstract class SplitImpl extends NodeImpl {
        private List<NodeImpl> _children =
                Collections.emptyList();
        private List<NodeImpl> _childrenWoDividers =
                Collections.emptyList();
        private boolean rowLayout = true;
        private String name;

        /**
         * @deprecated Use Vertical Horizontal ctor.
         * @param children
         */
        @Deprecated
        public SplitImpl(NodeImpl... children)
        {
            setChildren(children);

            _childrenWoDividers = _children.stream().filter(
                    c -> ! DividerImpl.class.isInstance( c ) ).collect(
                             Collectors.toList() );
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

            _childrenWoDividers = _children.stream().filter(
                    c -> ! DividerImpl.class.isInstance( c ) ).collect(
                             Collectors.toList() );
        }

        /**
         * Default constructor to support xml (de)serialization and other bean spec dependent ops.
         * Resulting instance of Split is invalid until setChildren() is called.
         */
        public SplitImpl() {
        }

        public int size()
        {
            return _childrenWoDividers.size();
        }

        /**
         * Adjust the weights to the nodes sizes.
         */
        public abstract void adjustWeights();

        /**
         * @return The splits extent after layouting.
         */
        public abstract int extent();

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
         * Returns true if the this Split's children are to be
         * laid out in a row: all the same height, left edge
         * equal to the previous Node's right edge.  If false,
         * children are laid out in a column.
         *
         * @return the value of the rowLayout property.
         * @see #setRowLayout
         */
        public boolean isRowLayout() { return rowLayout; }

        /**
         * Set the rowLayout property.  If true, all of this Split's
         * children are to be laid out in a row: all the same height,
         * each node's left edge equal to the previous Node's right
         * edge.  If false, children are laid on in a column.  Default
         * value is true.
         *
         * @param rowLayout true for horizontal row layout, false for column
         * @see #isRowLayout
         */
        public void setRowLayout(boolean rowLayout) {
            this.rowLayout = rowLayout;
        }

        /**
         * Returns this Split node's children.  The returned value
         * is not a reference to the Split's internal list of children
         *
         * @return the value of the children property.
         * @see #setChildren
         */
        public List<NodeImpl> getChildren() {
            return new ArrayList<NodeImpl>(_children);
        }
        public List<NodeImpl> getChildren2() {
            return new ArrayList<NodeImpl>(_childrenWoDividers);
        }

        /**
         * Check the dividers to ensure that redundant dividers are hidden and do
         * not interfere in the layout, for example when all the children of a split
         * are hidden (the split is then invisible), so two dividers may otherwise
         * appear next to one another.
         * @param split the split to check
         */
        public void checkDividers( SplitImpl split ) {
            ListIterator<NodeImpl> splitChildren = split.getChildren().listIterator();
            while( splitChildren.hasNext()) {
                NodeImpl splitChild = splitChildren.next();
                if ( !splitChild.isVisible()) {
                    continue;
                }
                else if ( splitChildren.hasNext()) {
                    NodeImpl dividerChild = splitChildren.next();
                    if ( dividerChild instanceof DividerImpl ) {
                        if ( splitChildren.hasNext()) {
                            NodeImpl rightChild = splitChildren.next();
                            while ( !rightChild.isVisible()) {
                                rightChild = rightChild.nextSibling();
                                if ( rightChild == null ) {
                                    // No visible right sibling found, so hide the divider
                                    dividerChild.setVisible( false );
                                    break;
                                }
                            }

                            // A visible child is found but it's a divider and therefore
                            // we have two visible and adjacent dividers - so we hide one
                            if (( rightChild != null ) && ( rightChild instanceof DividerImpl ))
                                dividerChild.setVisible( false );
                        }
                    }
                    else if (( splitChild instanceof DividerImpl ) && ( dividerChild instanceof DividerImpl )) {
                        splitChild.setVisible( false );
                    }
                }
            }
        }

        /**
         * Restore any of the hidden dividers that are required to separate visible nodes
         * @param split the node to check
         */
        public void restoreDividers( SplitImpl split ) {

            ListIterator<NodeImpl> splitChildren = split.getChildren().listIterator();
            while( splitChildren.hasNext()) {
                NodeImpl splitChild = splitChildren.next();
                if ( splitChild instanceof DividerImpl ) {
                    NodeImpl prev = splitChild.previousSibling();
                    if ( prev.isVisible()) {
                        NodeImpl next = splitChild.nextSibling();
                        while ( next != null ) {
                            if ( next.isVisible()) {
                                splitChild.setVisible( true );
                                break;
                            }
                            next = next.nextSibling();
                        }
                    }
                }
            }
            if ( split.getParent() != null )
                restoreDividers( split.getParent());
        }

        /**
         * Set's the children property of this Split node.
         *
         * @param children List of children
         * @see #getChildren
         * @throws IllegalArgumentException if children is null
         */
        public void setChildren(List<NodeImpl> children)
        {
            JavaUtil.Assert( _children.size() == 0 );

            _children = new ArrayList<NodeImpl>(
                    Objects.requireNonNull( children ) );

            int idx = 0;

            for(NodeImpl c : _children)
            {
                c.setParent(this);
                c.setParentIdx( idx++ );
            }
        }

        /**
         * Convenience method for setting the children of this Split node.  The parent
         * of each new child is set to this Split node, and the parent
         * of each old child (if any) is set to null.  This method
         * defensively copies the incoming array.
         *
         * @param children array of children
         * @see #getChildren
         * @throws IllegalArgumentException if children is null
         */
        public void setChildren(NodeImpl... children) {
            setChildren(children == null ? null : Arrays.asList(children));
        }

        /**
         * Convenience method that returns the last child whose weight
         * is > 0.0.
         *
         * @return the last child whose weight is > 0.0.
         * @see #getChildren
         * @see NodeImpl#getWeight
         */
        public final NodeImpl lastWeightedChild() {
            List<NodeImpl> kids = getChildren();
            NodeImpl weightedChild = null;
            for(NodeImpl child : kids) {
                if ( !child.isVisible())
                    continue;
                if (child.getWeight() > 0.0) {
                    weightedChild = child;
                }
            }
            return weightedChild;
        }

        /**
         * Return the Leaf's name.
         *
         * @return the value of the name property.
         * @see #setName
         */
        public String getName() { return name; }

        /**
         * Set the value of the name property.  Name may not be null.
         *
         * @param name value of the name property
         * @throws IllegalArgumentException if name is null
         */
        public void setName(String name) {
            if (name == null) {
                throw new IllegalArgumentException("name is null");
            }
            this.name = name;
        }

        @Override
        public String toString() {
            int nChildren = getChildren().size();
            StringBuffer sb = new StringBuffer("MultiSplitLayout.Split");
            sb.append(" \"");
            sb.append(getName());
            sb.append("\"");
            sb.append(isRowLayout() ? " ROW [" : " COLUMN [");
            sb.append(nChildren + ((nChildren == 1) ? " child" : " children"));
            sb.append("] ");
            sb.append(getBounds());
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
                for ( var c : _childrenWoDividers )
                {
                    c.validate( nameCollector );
                    totalWeight += c.weight;
                }
                if ( totalWeight > 1.0 )
                    throwInvalidLayout("Split children's total weight > 1.0", this);
            }
        }

        abstract void subLayout( Rectangle bounds, MultiSplitLayout host );

        @Override
        final void layout( Rectangle bounds, MultiSplitLayout host )
        {
            setBounds( bounds );

            subLayout( bounds, host );
        }
    }

    /**
     * Models a java.awt Component child.
     */
    public static class LeafImpl extends NodeImpl
    {
        private String _name = StringUtil.EMPTY_STRING;

        /**
         * Create a Leaf node.  The default value of name is "".
         */
        public LeafImpl( Leaf leaf )
        {
            _name = Objects.requireNonNull( leaf.name() );
        }

        /**
         * Create a Leaf node.  The default value of name is "".
         */
        @Deprecated
        public LeafImpl( )
        {
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

        /**
         * Set the value of the name property.  Name may not be null.
         *
         * @param name value of the name property
         * @throws IllegalArgumentException if name is null
         */
        public void setName(String name) {
            if (name == null) {
                throw new IllegalArgumentException("name is null");
            }
            _name = name;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("MultiSplitLayout.Leaf");
            sb.append(" \"");
            sb.append(getName());
            sb.append("\"");
            sb.append(" weight=");
            sb.append(getWeight());
            sb.append(" ");
            sb.append(getBounds());
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
        void layout( Rectangle bounds, MultiSplitLayout host )
        {
            setBounds( bounds );

            host.getComponentForNode( this ).setBounds( bounds );
        }
    }

    /**
     * Models a single vertical/horizontal divider.
     *
     * @deprecated micbinz : Not needed in final design.
     */
    @Deprecated
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
        public void setWeight(double weight) {
            throw new UnsupportedOperationException();
        }

        @Override
        public double getWeight()
        {
            return -1.0;
        }

        @Override
        public String toString() {
            return "MultiSplitLayout.Divider " + getBounds().toString();
        }

        @Override
        void validate( Set<String> nameCollector )
        {
        }

        @Override
        void layout( Rectangle bounds, MultiSplitLayout host )
        {
            throw new AssertionError("Unexpected");
        }

        public Point move( Point from, Point to )
        {
            var prev = previous();
            var next = next();

            if ( isVertical() )
            {
                var delta = to.x - from.x;

                prev._width( prev._width() + delta );
                _x( _x() + delta );
                next._x( next._x() + delta );
                next._width( next._width() - delta );
            }
            else
            {
                var delta = to.y - from.y;

                prev._height( prev._height() + delta );
                _y( _y() + delta );
                next._y( next._y() + delta );
                next._height( next._height() - delta );
            }

            getParent().adjustWeights();

            return to;
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
