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
import java.awt.Rectangle;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

    private JavaBeanProperty<Node, MultiSplitLayout> _model =
            new JavaBeanProperty<>(
                    this,
                    new Leaf("default"),
                    "model" );

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
    public MultiSplitLayout(Node model)
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

        if (model instanceof Leaf)
            model.setBounds(bounds);
        else if (model instanceof Split)
            doLayoutByWeight( model, bounds );
    }

    private void doLayoutByWeight( Node node, Rectangle bounds )
    {
        int width = bounds.width;
        int height = bounds.height;
        Split split = (Split)node;
        List<Node> splitChildren = split.getChildren();
        double distributableWeight = 1.0;
        int unweightedComponents = 0;
        int dividerSpace = 0;
        for( Node splitChild : splitChildren ) {
            if ( !splitChild.isVisible())
                continue;
            else if ( splitChild instanceof Divider ) {
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
            for( Node splitChild : splitChildren ) {
                if ( !splitChild.isVisible() || ( splitChild instanceof Divider ))
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

                if ( splitChild instanceof Split )
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
            for( Node splitChild : splitChildren ) {
                if ( !splitChild.isVisible() || ( splitChild instanceof Divider ))
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

                if ( splitChild instanceof Split )
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
    private Component getComponentForNode( Node n )
    {
        String name = ((Leaf)n).getName();
        return (name != null) ? (Component)_childMap.get(name) : null;
    }

    /**
     * Get the MultiSplitLayout.Node associated with a component
     * @param name the name used to associate a component with the layout
     * @return the node associated with the component
     */
    public Node getNodeForName( String name )
    {
        final var model = getModel();

        if ( model instanceof Split ) {
            Split split = ((Split)model);
            return getNodeForName( split, name );
        } else if (model instanceof Leaf) {
            if (((Leaf) model).getName().equals(name)) {
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
    public Node getNodeForComponent( Split split, Component comp )
    {
        return getNodeForName( split, getNameForComponent( comp ));
    }

    /**
     * Get the MultiSplitLayout.Node associated with a component
     * @param split the layout split that owns the requested node
     * @param name the name used to associate a component with the layout
     * @return the node associated with the component
     */
    public Node getNodeForName( Split split, String name )
    {
        for(Node n : split.getChildren()) {
            if ( n instanceof Leaf ) {
                if ( ((Leaf)n).getName().equals( name ))
                    return n;
            }
            else if ( n instanceof Split ) {
                Node n1 = getNodeForName( (Split)n, name );
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
    public Node getModel()
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
    public void setModel(Node model) {
        if ((model == null) || (model instanceof Divider)) {
            throw new IllegalArgumentException("invalid model");
        }
        _model.set( model );
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

    private Component childForNode(Node node) {
        if (node instanceof Leaf) {
            Leaf leaf = (Leaf)node;
            String name = leaf.getName();
            return (name != null) ? _childMap.get(name) : null;
        }
        return null;
    }


    private Dimension preferredComponentSize(Node node) {
        if ( layoutMode == LayoutMode.NO_MIN_SIZE_LAYOUT )
            return new Dimension(0, 0);

        Component child = childForNode(node);
        return ((child != null) && child.isVisible() ) ? child.getPreferredSize() : new Dimension(0, 0);
    }

    private Dimension preferredNodeSize(Node root) {
        if (root instanceof Leaf) {
            return preferredComponentSize(root);
        }
        else if (root instanceof Divider) {
            if ( !((Divider)root).isVisible())
                return new Dimension(0,0);
            int divSize = getDividerSize();
            return new Dimension(divSize, divSize);
        }
        else {
            Split split = (Split)root;
            List<Node> splitChildren = split.getChildren();
            int width = 0;
            int height = 0;
            if (split.isRowLayout()) {
                for(Node splitChild : splitChildren) {
                    if ( !splitChild.isVisible())
                        continue;
                    Dimension size = preferredNodeSize(splitChild);
                    width += size.width;
                    height = Math.max(height, size.height);
                }
            }
            else {
                for(Node splitChild : splitChildren) {
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
    public Dimension minimumNodeSize(Node root) {
        assert( root.isVisible );
        if (root instanceof Leaf) {
            if ( layoutMode == LayoutMode.NO_MIN_SIZE_LAYOUT )
                return new Dimension(0, 0);

            Component child = childForNode(root);
            return ((child != null) && child.isVisible() ) ? child.getMinimumSize() : new Dimension(0, 0);
        }
        else if (root instanceof Divider) {
            if ( !((Divider)root).isVisible()  )
                return new Dimension(0,0);
            int divSize = getDividerSize();
            return new Dimension(divSize, divSize);
        }
        else {
            Split split = (Split)root;
            List<Node> splitChildren = split.getChildren();
            int width = 0;
            int height = 0;
            if (split.isRowLayout()) {
                for(Node splitChild : splitChildren) {
                    if ( !splitChild.isVisible())
                        continue;
                    Dimension size = minimumNodeSize(splitChild);
                    width += size.width;
                    height = Math.max(height, size.height);
                }
            }
            else {
                for(Node splitChild : splitChildren) {
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
    public Dimension maximumNodeSize(Node root) {
        assert( root.isVisible );
        if (root instanceof Leaf) {
            Component child = childForNode(root);
            return ((child != null) && child.isVisible() ) ? child.getMaximumSize() : new Dimension(0, 0);
        }
        else if (root instanceof Divider) {
            if ( !((Divider)root).isVisible()  )
                return new Dimension(0,0);
            int divSize = getDividerSize();
            return new Dimension(divSize, divSize);
        }
        else {
            Split split = (Split)root;
            List<Node> splitChildren = split.getChildren();
            int width = Integer.MAX_VALUE;
            int height = Integer.MAX_VALUE;
            if (split.isRowLayout()) {
                for(Node splitChild : splitChildren) {
                    if ( !splitChild.isVisible())
                        continue;
                    Dimension size = maximumNodeSize(splitChild);
                    width += size.width;
                    height = Math.min(height, size.height);
                }
            }
            else {
                for(Node splitChild : splitChildren) {
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


    private void minimizeSplitBounds(Split split, Rectangle bounds) {
        assert ( split.isVisible());
        Rectangle splitBounds = new Rectangle(bounds.x, bounds.y, 0, 0);
        List<Node> splitChildren = split.getChildren();
        Node lastChild = null;
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


    private void layoutShrink(Split split, Rectangle bounds) {
        Rectangle splitBounds = split.getBounds();
        ListIterator<Node> splitChildren = split.getChildren().listIterator();
        //Node lastWeightedChild = split.lastWeightedChild();

        if (split.isRowLayout()) {
            int totalWidth = 0;          // sum of the children's widths
            int minWeightedWidth = 0;    // sum of the weighted childrens' min widths
            int totalWeightedWidth = 0;  // sum of the weighted childrens' widths
            for(Node splitChild : split.getChildren()) {
                if ( !splitChild.isVisible())
                    continue;
                int nodeWidth = splitChild.getBounds().width;
                int nodeMinWidth = 0;
                if (( layoutMode == LayoutMode.USER_MIN_SIZE_LAYOUT ) && !( splitChild instanceof Divider ))
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
                        Node splitChild = splitChildren.next();
                        if ( !splitChild.isVisible()) {
                            if ( splitChildren.hasNext())
                                splitChildren.next();
                            continue;
                        }
                        Rectangle splitChildBounds = splitChild.getBounds();
                        double minSplitChildWidth = 0.0;
                        if (( layoutMode == LayoutMode.USER_MIN_SIZE_LAYOUT ) && !( splitChild instanceof Divider ))
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
                                if ( splitChild instanceof Divider ) {
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
            for(Node splitChild : split.getChildren()) {
                if ( !splitChild.isVisible())
                    continue;
                int nodeHeight = splitChild.getBounds().height;
                int nodeMinHeight = 0;
                if (( layoutMode == LayoutMode.USER_MIN_SIZE_LAYOUT ) && !( splitChild instanceof Divider ))
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
                        Node splitChild = splitChildren.next();
                        if ( !splitChild.isVisible()) {
                            if ( splitChildren.hasNext())
                                splitChildren.next();
                            continue;
                        }
                        Rectangle splitChildBounds = splitChild.getBounds();
                        double minSplitChildHeight = 0.0;
                        if (( layoutMode == LayoutMode.USER_MIN_SIZE_LAYOUT ) && !( splitChild instanceof Divider ))
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
                            if ( splitChild instanceof Divider ) {
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
                                if ( splitChild instanceof Divider ) {
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
    private boolean hasMoreVisibleSiblings( Node splitChild ) {
        Node next = splitChild.nextSibling();
        if ( next == null )
            return false;

        do {
            if ( next.isVisible())
                return true;
            next  = next.nextSibling();
        } while ( next != null );

        return false;
    }

    private void layoutGrow(Split split, Rectangle bounds) {
        Rectangle splitBounds = split.getBounds();
        ListIterator<Node> splitChildren = split.getChildren().listIterator();
        Node lastWeightedChild = split.lastWeightedChild();

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
                Node splitChild = splitChildren.next();
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
                Node splitChild = splitChildren.next();
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
    private void layout2(Node root, Rectangle bounds) {
        if (root instanceof Leaf) {
            Component child = childForNode(root);
            if (child != null) {
                child.setBounds(bounds);
            }
            root.setBounds(bounds);
        }
        else if (root instanceof Divider) {
            root.setBounds(bounds);
        }
        else if (root instanceof Split) {
            Split split = (Split)root;
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


    /* First pass of the layout algorithm.
     *
     * If the Dividers are "floating" then set the bounds of each
     * node to accomodate the preferred size of all of the
     * Leaf's java.awt.Components.  Otherwise, just set the bounds
     * of each Leaf/Split node so that it's to the left of (for
     * Split.isRowLayout() Split children) or directly above
     * the Divider that follows.
     *
     * This pass sets the bounds of each Node in the layout model.  It
     * does not resize any of the parent Container's
     * (java.awt.Component) children.  That's done in the second pass,
     * see layoutGrow() and layoutShrink().
     */
    private void layout1(Node root, Rectangle bounds) {
        if (root instanceof Leaf) {
            root.setBounds(bounds);
        }
        else if (root instanceof Split) {
            Split split = (Split)root;
            Iterator<Node> splitChildren = split.getChildren().iterator();
            Rectangle childBounds = null;
            int divSize = getDividerSize();
            boolean initSplit = false;


            /* Layout the Split's child Nodes' along the X axis.  The bounds
             * of each child will have the same y coordinate and height as the
             * layout1() bounds argument.
             *
             * Note: the column layout code - that's the "else" clause below
             * this if, is identical to the X axis (rowLayout) code below.
             */
            if (split.isRowLayout()) {
                double x = bounds.getX();
                while(splitChildren.hasNext()) {
                    Node splitChild = splitChildren.next();
                    if ( !splitChild.isVisible()) {
                        if ( splitChildren.hasNext())
                            splitChildren.next();
                        continue;
                    }
                    Divider dividerChild =
                            (splitChildren.hasNext()) ? (Divider)(splitChildren.next()) : null;

                    double childWidth = 0.0;

                    if ((dividerChild != null) && dividerChild.isVisible()) {
                        double cw = dividerChild.getBounds().getX() - x;
                        if ( cw > 0.0 )
                            childWidth = cw;
                        else {
                            childWidth = preferredNodeSize(splitChild).getWidth();
                            initSplit = true;
                        }
                    }
                    else {
                        childWidth = split.getBounds().getMaxX() - x;
                    }
                    childBounds = boundsWithXandWidth(bounds, x, childWidth);
                    layout1(splitChild, childBounds);

                    if (( initSplit || false) && (dividerChild != null) && dividerChild.isVisible()) {
                        double dividerX = childBounds.getMaxX();
                        Rectangle dividerBounds;
                        dividerBounds = boundsWithXandWidth(bounds, dividerX, divSize);
                        dividerChild.setBounds(dividerBounds);
                    }
                    if ((dividerChild != null) && dividerChild.isVisible()) {
                        x = dividerChild.getBounds().getMaxX();
                    }
                }
            }

            /* Layout the Split's child Nodes' along the Y axis.  The bounds
             * of each child will have the same x coordinate and width as the
             * layout1() bounds argument.  The algorithm is identical to what's
             * explained above, for the X axis case.
             */
            else {
                double y = bounds.getY();
                while(splitChildren.hasNext())
                {
                    Node splitChild = splitChildren.next();

                    if ( !splitChild.isVisible()) {
                        continue;
                    }

                    Divider dividerChild = (splitChildren.hasNext()) ?
                            (Divider)(splitChildren.next()) :
                                null;

                    double childHeight = 0.0;
                    if ((dividerChild != null) && dividerChild.isVisible()) {
                        double cy = dividerChild.getBounds().getY() - y;
                        if ( cy > 0.0 )
                            childHeight = cy;
                        else {
                            childHeight = preferredNodeSize(splitChild).getHeight();
                            initSplit = true;
                        }
                    }
                    else {
                        childHeight = split.getBounds().getMaxY() - y;
                    }

                    childBounds = boundsWithYandHeight(bounds, y, childHeight);
                    layout1(splitChild, childBounds);

                    if (( initSplit || false) && (dividerChild != null) && dividerChild.isVisible()) {
                        double dividerY = childBounds.getMaxY();
                        Rectangle dividerBounds = boundsWithYandHeight(bounds, dividerY, divSize);
                        dividerChild.setBounds(dividerBounds);
                    }
                    if ((dividerChild != null) && dividerChild.isVisible()) {
                        y = dividerChild.getBounds().getMaxY();
                    }
                }
            }
            /* The bounds of the Split node root are set to be just
             * big enough to contain all of its children, but only
             * along the axis it's allocating space on.  That's
             * X for rows, Y for columns.  The second pass of the
             * layout algorithm - see layoutShrink()/layoutGrow()
             * allocates extra space.
             */
            minimizeSplitBounds(split, bounds);
        }
    }

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
        private final Node node;
        public InvalidLayoutException(String msg, Node node) {
            super(msg);
            this.node = node;
        }
        /**
         * @return the invalid Node.
         */
        public Node getNode() { return node; }
    }

    private static void throwInvalidLayout(String msg, Node node) {
        throw new InvalidLayoutException(msg, node);
    }

    @Deprecated
    private void checkLayout(Node root) {
        if (root instanceof Split) {
            Split split = (Split)root;
            if (split.getChildren().size() <= 2) {
                throwInvalidLayout("Split must have > 2 children", root);
            }
            Iterator<Node> splitChildren = split.getChildren().iterator();
            double weight = 0.0;
            while(splitChildren.hasNext())       {
                Node splitChild = splitChildren.next();
                if ( !splitChild.isVisible()) {
                    if ( splitChildren.hasNext())
                        splitChildren.next();
                    continue;
                }
                if (splitChild instanceof Divider) {
                    continue;
                    //throwInvalidLayout("expected a Split or Leaf Node", splitChild);
                }
                if (splitChildren.hasNext()) {
                    Node dividerChild = splitChildren.next();
                    if (!(dividerChild instanceof Divider)) {
                        throwInvalidLayout("expected a Divider Node", dividerChild);
                    }
                }
                weight += splitChild.getWeight();
                checkLayout(splitChild);
            }
            if (weight > 1.0) {
                throwInvalidLayout("Split children's total weight > 1.0", root);
            }
        }
    }
    private void validateLayout( Node root )
    {
        root.validate( new HashSet<>() );
    }

    /**
     * Compute the bounds of all of the Split/Divider/Leaf Nodes in
     * the layout model, and then set the bounds of each child component
     * with a matching Leaf Node.
     */
    public void layoutContainerOld(Container parent)
    {
        if ( layoutByWeight )
            doLayoutByWeight( parent );

        checkLayout(getModel());
        Insets insets = parent.getInsets();
        Dimension size = parent.getSize();
        int width = size.width - (insets.left + insets.right);
        int height = size.height - (insets.top + insets.bottom);
        Rectangle bounds = new Rectangle( insets.left, insets.top, width, height);
        layout1(getModel(), bounds);
        layout2(getModel(), bounds);
    }

    /**
     *
     * @param node The node to layout.
     * @param bounds
     */
    private void _performLayout( Node node, Rectangle bounds )
    {
        node.layout( bounds, getDividerSize() );
    }

    private static List<Node> _completeWeights( List<Node> children )
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

    private Divider dividerAt(Node root, int x, int y) {
        if (root instanceof Divider) {
            Divider divider = (Divider)root;
            return (divider.getBounds().contains(x, y)) ? divider : null;
        }
        else if (root instanceof Split) {
            Split split = (Split)root;
            for(Node child : split.getChildren()) {
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
    public Divider dividerAt(int x, int y) {
        return dividerAt(getModel(), x, y);
    }

    private boolean nodeOverlapsRectangle(Node node, Rectangle r2) {
        Rectangle r1 = node.getBounds();
        return
                (r1.x <= (r2.x + r2.width)) && ((r1.x + r1.width) >= r2.x) &&
                (r1.y <= (r2.y + r2.height)) && ((r1.y + r1.height) >= r2.y);
    }

    private List<Divider> dividersThatOverlap(Node root, Rectangle r) {
        if (nodeOverlapsRectangle(root, r) && (root instanceof Split)) {
            List<Divider> dividers = new ArrayList<Divider>();
            for(Node child : ((Split)root).getChildren()) {
                if (child instanceof Divider) {
                    if (nodeOverlapsRectangle(child, r)) {
                        dividers.add((Divider)child);
                    }
                }
                else if (child instanceof Split) {
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
    public List<Divider> dividersThatOverlap(Rectangle r) {
        if (r == null) {
            throw new IllegalArgumentException("null Rectangle");
        }
        return dividersThatOverlap(getModel(), r);
    }


    /**
     * Base class for the nodes that model a MultiSplitLayout.
     */
    public static abstract class Node implements Serializable {
        private Split parent = null;
        private final Rectangle _bounds = new Rectangle();
        private double weight = 0.0;
        private boolean isVisible = true;
        public void setVisible( boolean b ) {
            isVisible = b;
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
        public Split getParent() { return parent; }

        /**
         * Set the value of this Node's parent property.  The default
         * value of this property is null.
         *
         * @param parent a Split or null
         * @see #getParent
         */
        public void setParent(Split parent) {
            this.parent = parent;
        }

        /**
         * @return A copy of the value of the bounds property.
         * @see #setBounds
         */
        public Rectangle getBounds() {
            return new Rectangle(this._bounds);
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
         * @throws IllegalArgumentException if bounds is null
         * @see #getBounds
         */
        public void setBounds(Rectangle bounds) {
            if (bounds == null) {
                throw new IllegalArgumentException("null bounds");
            }
            _bounds.setBounds( bounds );
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
        public Node weight( double weight )
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

        private Node siblingAtOffset(int offset) {
            Split p = getParent();
            if (p == null) { return null; }
            List<Node> siblings = p.getChildren();
            int index = siblings.indexOf(this);
            if (index == -1) { return null; }
            index += offset;
            return ((index > -1) && (index < siblings.size())) ? siblings.get(index) : null;
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
        public Node nextSibling() {
            return siblingAtOffset(+1);
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
        public Node previousSibling() {
            return siblingAtOffset(-1);
        }

        abstract void validate( Set<String> nameCollector );

        abstract void layout(
                Rectangle bounds,
                int dividerSize );
    }

    /**
     *
     */
    public static class Row extends Split {
        public Row() {
        }

        public Row(Node... children) {
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
        public void layout( Rectangle bounds, int dividerSize )
        {
            setBounds( bounds );

            var children =  _completeWeights( getChildren2() );

            final int dividerCount =
                    children.size() -1;
            final int netRowWidth =
                    bounds.width - dividerCount * dividerSize;

            double currentPosition = 0.0;

            for ( int i = 0 ; i < children.size() ; i++ )
            {
                var c = children.get( i );

                double w = c.getWeight() * netRowWidth;

                Rectangle subBounds = new Rectangle(
                        MathUtil.round( currentPosition ),
                        bounds.y,
                        MathUtil.round( w ),
                        bounds.height );

                c.layout(
                        subBounds,
                        dividerSize );

                currentPosition +=
                        (w + dividerSize);
            }

            if ( extent() != bounds.width )
            {
                // Correct the node positions.
                int error = extent() - bounds.width;

                LOG.warning( String.format(
                        "Expected width %d not %d.  Error=%d", extent(), bounds.width, error ) );

                for ( int i = children.size()-1 ; error > 0 ; i-- )
                {
                    Node c = children.get( i );
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
    }

    public static class Column extends Split {
        public Column() {
        }

        public Column(Node... children) {
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
        public void layout( Rectangle bounds, int dividerSize )
        {
            var children =  _completeWeights( getChildren2() );

            double currentPosition = 0.0;
            for ( var c : children )
            {
                double h = c.getWeight() * bounds.height;

                Rectangle subBounds = new Rectangle(
                        bounds.x,
                        MathUtil.round( currentPosition ),
                        bounds.width,
                        MathUtil.round( h ));

                c.layout( subBounds, dividerSize );

                currentPosition += h;
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

    }

    /**
     * Defines a vertical or horizontal subdivision into two or more tiles.
     */
    public static class Split extends Node {
        private List<Node> _children =
                Collections.emptyList();
        private List<Node> _childrenWoDividers =
                Collections.emptyList();
        private boolean rowLayout = true;
        private String name;

        /**
         * @deprecated Use Vertical Horizontal ctor.
         * @param children
         */
        @Deprecated
        public Split(Node... children)
        {
            setChildren(children);

            _childrenWoDividers = _children.stream().filter(
                    c -> ! Divider.class.isInstance( c ) ).collect(
                             Collectors.toList() );
        }

        /**
         * Default constructor to support xml (de)serialization and other bean spec dependent ops.
         * Resulting instance of Split is invalid until setChildren() is called.
         */
        public Split() {
        }

        public int size()
        {
            return _childrenWoDividers.size();
        }

        /**
         * @return The splits extent after layouting.
         */
        public int extent()
        {
            return -1;
        }

        /**
         * Determines whether this node should be visible when its
         * parent is visible. Nodes are
         * initially visible
         * @return <code>true</code> if the node is visible,
         * <code>false</code> otherwise
         */
        @Override
        public boolean isVisible() {
            for(Node child : _children) {
                if ( child.isVisible() && !( child instanceof Divider ))
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
        public List<Node> getChildren() {
            return new ArrayList<Node>(_children);
        }
        public List<Node> getChildren2() {
            return new ArrayList<Node>(_childrenWoDividers);
        }

        /**
         * Check the dividers to ensure that redundant dividers are hidden and do
         * not interfere in the layout, for example when all the children of a split
         * are hidden (the split is then invisible), so two dividers may otherwise
         * appear next to one another.
         * @param split the split to check
         */
        public void checkDividers( Split split ) {
            ListIterator<Node> splitChildren = split.getChildren().listIterator();
            while( splitChildren.hasNext()) {
                Node splitChild = splitChildren.next();
                if ( !splitChild.isVisible()) {
                    continue;
                }
                else if ( splitChildren.hasNext()) {
                    Node dividerChild = splitChildren.next();
                    if ( dividerChild instanceof Divider ) {
                        if ( splitChildren.hasNext()) {
                            Node rightChild = splitChildren.next();
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
                            if (( rightChild != null ) && ( rightChild instanceof Divider ))
                                dividerChild.setVisible( false );
                        }
                    }
                    else if (( splitChild instanceof Divider ) && ( dividerChild instanceof Divider )) {
                        splitChild.setVisible( false );
                    }
                }
            }
        }

        /**
         * Restore any of the hidden dividers that are required to separate visible nodes
         * @param split the node to check
         */
        public void restoreDividers( Split split ) {

            ListIterator<Node> splitChildren = split.getChildren().listIterator();
            while( splitChildren.hasNext()) {
                Node splitChild = splitChildren.next();
                if ( splitChild instanceof Divider ) {
                    Node prev = splitChild.previousSibling();
                    if ( prev.isVisible()) {
                        Node next = splitChild.nextSibling();
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
         * Set's the children property of this Split node.  The parent
         * of each new child is set to this Split node, and the parent
         * of each old child (if any) is set to null.  This method
         * defensively copies the incoming List.  Default value is
         * an empty List.
         *
         * @param children List of children
         * @see #getChildren
         * @throws IllegalArgumentException if children is null
         */
        public void setChildren(List<Node> children) {
            if (children == null) {
                throw new IllegalArgumentException("children must be a non-null List");
            }
            for(Node child : this._children) {
                child.setParent(null);
            }

            this._children = new ArrayList<Node>(children);
            for(Node child : this._children) {
                child.setParent(this);
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
        public void setChildren(Node... children) {
            setChildren(children == null ? null : Arrays.asList(children));
        }

        /**
         * Convenience method that returns the last child whose weight
         * is > 0.0.
         *
         * @return the last child whose weight is > 0.0.
         * @see #getChildren
         * @see Node#getWeight
         */
        public final Node lastWeightedChild() {
            List<Node> kids = getChildren();
            Node weightedChild = null;
            for(Node child : kids) {
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

        @Override
        void layout( Rectangle bounds, int dividerSize )
        {
            throw new AssertionError( "Unexpected." );
        }
    }

    /**
     * Models a java.awt Component child.
     */
    public static class Leaf extends Node
    {
        private String _name = StringUtil.EMPTY_STRING;

        /**
         * Create a Leaf node.  The default value of name is "".
         */
        public Leaf()
        {
        }

        /**
         * Create a Leaf node with the specified name.  Name cannot
         * be null.
         *
         * @param name value of the Leaf's name property
         * @throws IllegalArgumentException if name is null
         */
        public Leaf(String name) {
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
        void layout( Rectangle bounds, int dividerSize )
        {
            setBounds( bounds );
        }
    }

    /**
     * Models a single vertical/horizontal divider.
     *
     * @deprecated micbinz : Not needed in final design.
     */
    @Deprecated
    public static class Divider extends Node {
        /**
         * Convenience method, returns true if the Divider's parent
         * is a Split row (a Split with isRowLayout() true), false
         * otherwise. In other words if this Divider's major axis
         * is vertical, return true.
         *
         * @return true if this Divider is part of a Split row.
         */
        public final boolean isVertical() {
            Split parent = getParent();
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
        void layout( Rectangle bounds, int dividerSize )
        {
            throw new AssertionError("Unexpected");
        }
    }

    private static void throwParseException(StreamTokenizer st, String msg) throws Exception {
        throw new Exception("MultiSplitLayout.parseModel Error: " + msg);
    }

    private static void parseAttribute(String name, StreamTokenizer st, Node node) throws Exception {
        if ((st.nextToken() != '=')) {
            throwParseException(st, "expected '=' after " + name);
        }
        if (name.equalsIgnoreCase("WEIGHT")) {
            if (st.nextToken() == StreamTokenizer.TT_NUMBER) {
                node.setWeight(st.nval);
            }
            else {
                throwParseException(st, "invalid weight");
            }
        }
        else if (name.equalsIgnoreCase("NAME")) {
            if (st.nextToken() == StreamTokenizer.TT_WORD) {
                if (node instanceof Leaf) {
                    ((Leaf)node).setName(st.sval);
                }
                else if (node instanceof Split) {
                    ((Split)node).setName(st.sval);
                }
                else {
                    throwParseException(st, "can't specify name for " + node);
                }
            }
            else {
                throwParseException(st, "invalid name");
            }
        }
        else {
            throwParseException(st, "unrecognized attribute \"" + name + "\"");
        }
    }

    private static void addSplitChild(Split parent, Node child) {
        List<Node> children = new ArrayList<Node>(parent.getChildren());
        if (children.size() == 0) {
            children.add(child);
        }
        else {
            children.add(new Divider());
            children.add(child);
        }
        parent.setChildren(children);
    }

    private static void parseLeaf(StreamTokenizer st, Split parent) throws Exception {
        Leaf leaf = new Leaf();
        int token;
        while ((token = st.nextToken()) != StreamTokenizer.TT_EOF) {
            if (token == ')') {
                break;
            }
            if (token == StreamTokenizer.TT_WORD) {
                parseAttribute(st.sval, st, leaf);
            }
            else {
                throwParseException(st, "Bad Leaf: " + leaf);
            }
        }
        addSplitChild(parent, leaf);
    }

    private static void parseSplit(StreamTokenizer st, Split parent) throws Exception {
        int token;
        while ((token = st.nextToken()) != StreamTokenizer.TT_EOF) {
            if (token == ')') {
                break;
            }
            else if (token == StreamTokenizer.TT_WORD) {
                if (st.sval.equalsIgnoreCase("WEIGHT")) {
                    parseAttribute(st.sval, st, parent);
                }
                else if (st.sval.equalsIgnoreCase("NAME")) {
                    parseAttribute(st.sval, st, parent);
                }
                else {
                    addSplitChild(parent, new Leaf(st.sval));
                }
            }
            else if (token == '(') {
                if ((token = st.nextToken()) != StreamTokenizer.TT_WORD) {
                    throwParseException(st, "invalid node type");
                }
                String nodeType = st.sval.toUpperCase();
                if (nodeType.equals("LEAF")) {
                    parseLeaf(st, parent);
                }
                else if (nodeType.equals("ROW") || nodeType.equals("COLUMN")) {
                    Split split = new Split();
                    split.setRowLayout(nodeType.equals("ROW"));
                    addSplitChild(parent, split);
                    parseSplit(st, split);
                }
                else {
                    throwParseException(st, "unrecognized node type '" + nodeType + "'");
                }
            }
        }
    }

    private static Node parseModel(Reader r) {
        StreamTokenizer st = new StreamTokenizer(r);
        try {
            Split root = new Split();
            parseSplit(st, root);
            return root.getChildren().get(0);
        }
        catch (Exception e) {
            System.err.println(e);
        }
        finally {
            try { r.close(); } catch (IOException ignore) {}
        }
        return null;
    }

    /**
     * A convenience method that converts a string to a
     * MultiSplitLayout model (a tree of Nodes) using a
     * a simple syntax.  Nodes are represented by
     * parenthetical expressions whose first token
     * is one of ROW/COLUMN/LEAF.  ROW and COLUMN specify
     * horizontal and vertical Split nodes respectively,
     * LEAF specifies a Leaf node.  A Leaf's name and
     * weight can be specified with attributes,
     * name=<i>myLeafName</i> weight=<i>myLeafWeight</i>.
     * Similarly, a Split's weight can be specified with
     * weight=<i>mySplitWeight</i>.
     *
     * <p> For example, the following expression generates
     * a horizontal Split node with three children:
     * the Leafs named left and right, and a Divider in
     * between:
     * <pre>
     * (ROW (LEAF name=left) (LEAF name=right weight=1.0))
     * </pre>
     *
     * <p> Dividers should not be included in the string,
     * they're added automatically as needed.  Because
     * Leaf nodes often only need to specify a name, one
     * can specify a Leaf by just providing the name.
     * The previous example can be written like this:
     * <pre>
     * (ROW left (LEAF name=right weight=1.0))
     * </pre>
     *
     * <p>Here's a more complex example.  One row with
     * three elements, the first and last of which are columns
     * with two leaves each:
     * <pre>
     * (ROW (COLUMN weight=0.5 left.top left.bottom)
     *      (LEAF name=middle)
     *      (COLUMN weight=0.5 right.top right.bottom))
     * </pre>
     *
     *
     * <p> This syntax is not intended for archiving or
     * configuration files .  It's just a convenience for
     * examples and tests.
     *
     * @return the Node root of a tree based on s.
     */
    public static Node parseModel(String s) {
        return parseModel(new StringReader(s));
    }


    private static void printModel(String indent, Node root) {
        if (root instanceof Split) {
            Split split = (Split)root;
            System.out.println(indent + split);
            for(Node child : split.getChildren()) {
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
    public static void printModel(Node root) {
        printModel("", root);
    }
}
