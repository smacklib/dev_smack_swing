/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2003-2023 Michael Binz
 */

package org.smack.swing.swingx;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.logging.Logger;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import org.smack.swing.swingx.MultiSplitLayout.DividerImpl;
import org.smack.swing.swingx.MultiSplitLayout.NodeImpl;
import org.smack.swing.swingx.painter.AbstractPainter;

/**
 *
 * <p>
 * All properties in this class are bound: when a properties value
 * is changed, all PropertyChangeListeners are fired.
 *
 * @author Michael Binz
 * @author Hans Muller
 * @author Luan O'Carroll
 */
@SuppressWarnings("serial")
public final class JXMultiSplitPane extends JPanel
{
    private static final Logger LOG = Logger.getLogger( JXMultiSplitPane.class.getName() );

    private AccessibleContext accessibleContext = null;
    private final boolean continuousLayout = true;
    private DividerPainter dividerPainter = new DefaultDividerPainter();

    /**
     * Creates a MultiSplitPane with it's LayoutManager set to
     * to an empty MultiSplitLayout.
     */
    public JXMultiSplitPane() {
        this(new MultiSplitLayout());
    }

    /**
     * Creates a MultiSplitPane.
     * @param layout the new split pane's layout
     */
    public JXMultiSplitPane( MultiSplitLayout layout ) {
        super(layout);
        InputHandler inputHandler = new InputHandler();
        addMouseListener(inputHandler);
        addMouseMotionListener(inputHandler);
        addKeyListener(inputHandler);
        setFocusable(true);
    }

    /**
     * A convenience method that returns the layout manager cast
     * to MutliSplitLayout.
     *
     * @return this MultiSplitPane's layout manager
     * @see java.awt.Container#getLayout
     * @see #setModel
     */
    public final MultiSplitLayout getMultiSplitLayout() {
        return (MultiSplitLayout)getLayout();
    }

    /**
     * A convenience method that sets the MultiSplitLayout model.
     * Equivalent to <code>getMultiSplitLayout.setModel(model)</code>
     *
     * @param model the root of the MultiSplitLayout model
     * @see #getMultiSplitLayout
     * @see MultiSplitLayout#setModel
     */
    public final void setModel(NodeImpl model) {
        getMultiSplitLayout().setModel(model);
    }

    /**
     * A convenience method that sets the MultiSplitLayout dividerSize
     * property. Equivalent to
     * <code>getMultiSplitLayout().setDividerSize(newDividerSize)</code>.
     *
     * @param dividerSize the value of the dividerSize property
     * @see #getMultiSplitLayout
     * @see MultiSplitLayout#setDividerSize
     */
    public final void setDividerSize(int dividerSize) {
        getMultiSplitLayout().setDividerSize(dividerSize);
    }

    /**
     * A convenience method that returns the MultiSplitLayout dividerSize
     * property. Equivalent to
     * <code>getMultiSplitLayout().getDividerSize()</code>.
     *
     * @see #getMultiSplitLayout
     * @see MultiSplitLayout#getDividerSize
     */
    public final int getDividerSize() {
        return getMultiSplitLayout().getDividerSize();
    }

    /**
     * Returns the Divider that's currently being moved, typically
     * because the user is dragging it, or null.
     *
     * @return the Divider that's being moved or null.
     */
    public DividerImpl activeDivider() {
        return dragDivider;
    }

    /**
     * Draws a single Divider.  Typically used to specialize the
     * way the active Divider is painted.
     *
     * @see #getDividerPainter
     * @see #setDividerPainter
     */
    public static abstract class DividerPainter extends AbstractPainter<DividerImpl> {
    }

    private class DefaultDividerPainter extends DividerPainter implements Serializable {
        @Override
        protected void doPaint(Graphics2D g, DividerImpl divider, int width, int height) {
        }
    }

    private boolean dragUnderway = false;
    private MultiSplitLayout.DividerImpl dragDivider = null;
    private Rectangle initialDividerBounds = null;
    //private boolean oldFloatingDividers = true;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int dragMin = -1;
    private int dragMax = -1;

    private Point _dragPoint = null;

    private void startDrag( Point p ) {
        requestFocusInWindow();
        MultiSplitLayout msl = getMultiSplitLayout();
        dragDivider = msl.dividerAt( p.x, p.y );

        if ( dragDivider == null ) {
            LOG.warning( "No divider." );
            dragUnderway = false;
            return;
        }

        _dragPoint = p;

        LOG.info( "Start drag: " + _dragPoint );

        dragUnderway = true;

    }

    private void updateDrag( Point p ) {
        if (!dragUnderway) {
            return;
        }

        _dragPoint = dragDivider.move( _dragPoint, p );

        revalidate();
        repaint();
    }

    /**
     * Set the maximum node size. This method can be overridden to limit the
     * size of a node during a drag operation on a divider. When implementing
     * this method in a subclass the node instance should be checked, for
     * example:
     * <code>
     * class MyMultiSplitPane extends JXMultiSplitPane
     * {
     *   protected Dimension getMaxNodeSize( MultiSplitLayout msl, Node n )
     *   {
     *     if (( n instanceof Leaf ) && ((Leaf)n).getName().equals( "top" ))
     *       return msl.maximumNodeSize( n );
     *     return null;
     *   }
     * }
     * </code>
     * @param msl the MultiSplitLayout used by this pane
     * @param n the node being resized
     * @return the maximum size or null (by default) to ignore the maximum size.
     */
    protected Dimension getMaxNodeSize( MultiSplitLayout msl, NodeImpl n ) {
        return null;
    }

    /**
     * Set the minimum node size. This method can be overridden to limit the
     * size of a node during a drag operation on a divider.
     * @param msl the MultiSplitLayout used by this pane
     * @param n the node being resized
     * @return the maximum size or null (by default) to ignore the maximum size.
     */
    protected Dimension getMinNodeSize( MultiSplitLayout msl, NodeImpl n ) {
        return msl.minimumNodeSize(n);
    }

    private void clearDragState()
    {
        dragDivider = null;
        _dragPoint = null;
        dragUnderway = false;
    }

    private void finishDrag()
    {
        if (dragUnderway) {
            clearDragState();
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    private void cancelDrag() {
        if (dragUnderway) {
            dragDivider.setBounds(initialDividerBounds);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            repaint();
            revalidate();
            clearDragState();
        }
    }

    private void updateCursor(int x, int y, boolean show)
    {
        if (dragUnderway)
            return;

        int cursorID = Cursor.DEFAULT_CURSOR;
        if (show) {
            MultiSplitLayout.DividerImpl divider = getMultiSplitLayout().dividerAt(x, y);
            if (divider != null) {
                cursorID  = (divider.isVertical()) ?
                        Cursor.E_RESIZE_CURSOR :
                            Cursor.N_RESIZE_CURSOR;
            }
        }
        setCursor(Cursor.getPredefinedCursor(cursorID));
    }

    private class InputHandler extends MouseInputAdapter implements KeyListener {

        @Override
        public void mouseEntered(MouseEvent e) {
            updateCursor(e.getX(), e.getY(), true);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            updateCursor(e.getX(), e.getY(), true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            updateCursor(e.getX(), e.getY(), false);
        }

        @Override
        public void mousePressed(MouseEvent e) {
            startDrag( e.getPoint() );
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            finishDrag();
        }
        @Override
        public void mouseDragged(MouseEvent e) {
            updateDrag( e.getPoint() );
        }
        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                cancelDrag();
            }
        }
        @Override
        public void keyReleased(KeyEvent e) { }

        @Override
        public void keyTyped(KeyEvent e) { }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if( accessibleContext == null ) {
            accessibleContext = new AccessibleMultiSplitPane();
        }
        return accessibleContext;
    }

    protected class AccessibleMultiSplitPane extends AccessibleJPanel {
        @Override
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SPLIT_PANE;
        }
    }
}
