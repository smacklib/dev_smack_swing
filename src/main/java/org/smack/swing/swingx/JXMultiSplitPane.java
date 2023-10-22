/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2003-2023 Michael Binz
 */
package org.smack.swing.swingx;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import org.smack.swing.swingx.MultiSplitLayout.DividerImpl;
import org.smack.swing.swingx.MultiSplitLayout.SplitImpl;

/**
 * Properties in this class are bound.
 *
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public final class JXMultiSplitPane
    extends JPanel
{
    private static final Logger LOG =
            Logger.getLogger( JXMultiSplitPane.class.getName() );

    /**
     * Creates a MultiSplitPane with it's LayoutManager set to
     * to an empty MultiSplitLayout.
     */
    public JXMultiSplitPane()
    {
        this( new MultiSplitLayout() );
    }

    /**
     * Creates a MultiSplitPane.
     * @param layout the new split pane's layout
     */
    public JXMultiSplitPane( MultiSplitLayout layout )
    {
        super( layout );

        InputHandler inputHandler =
                new InputHandler();
        addMouseListener(
                inputHandler);
        addMouseMotionListener(
                inputHandler);
        addKeyListener(
                inputHandler);

        setFocusable(true);
    }

    /**
     * A convenience method that returns the layout manager cast
     * to MultiSplitLayout.
     *
     * @return this MultiSplitPane's layout manager
     * @see java.awt.Container#getLayout
     * @see #setModel
     */
    private final MultiSplitLayout getMultiSplitLayout()
    {
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
    public final void setModel( SplitImpl model )
    {
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
    public final void setDividerSize(int dividerSize)
    {
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
    public final int getDividerSize()
    {
        return getMultiSplitLayout().getDividerSize();
    }

    /**
     * Returns the Divider that's currently being moved, typically
     * because the user is dragging it, or null.
     *
     * @return the Divider that's being moved or null.
     */
    public DividerImpl activeDivider()
    {
        return dragDivider;
    }

    private boolean dragUnderway = false;
    private MultiSplitLayout.DividerImpl dragDivider = null;
    private Rectangle initialDividerBounds = null;

    private Point _dragPoint = null;

    private void startDrag( Point p )
    {
        requestFocusInWindow();
        MultiSplitLayout msl = getMultiSplitLayout();
        dragDivider = msl.dividerAt( p );

        if ( dragDivider == null ) {
            LOG.warning( "No divider." );
            dragUnderway = false;
            return;
        }

        _dragPoint = p;

        LOG.info( "Start drag: " + _dragPoint );

        dragUnderway = true;

    }

    private void updateDrag( Point p )
    {
        if (!dragUnderway) {
            return;
        }

        _dragPoint = dragDivider.move( _dragPoint, p, 20 );

        revalidate();
        repaint();
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

    private void updateCursor( Point p, boolean show )
    {
        if (dragUnderway)
            return;

        int cursorID = Cursor.DEFAULT_CURSOR;
        if (show) {
            MultiSplitLayout.DividerImpl divider = getMultiSplitLayout().dividerAt( p );
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
            updateCursor(e.getPoint(), true);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            updateCursor(e.getPoint(), true);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            updateCursor(e.getPoint(), false);
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
}
