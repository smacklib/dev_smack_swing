/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2003-2023 Michael Binz
 */
package org.smack.swing.swingx;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.event.MouseInputAdapter;

import org.smack.swing.swingx.MultiSplitLayout.Split;

/**
 * A Component that allows multiple splits.
 * <p>
 * Define the split structure using the model
 * described in {@link MultiSplitLayout}.
 * <p>
 * The splits can be resized by dragging the divider between the
 * components.  A drag can be aborted by pressing the ESC key.
 *
 * @author Michael Binz
 */
public final class JXMultiSplitPane
    extends JPanel
{
    private static final Logger LOG =
            Logger.getLogger( JXMultiSplitPane.class.getName() );

    /**
     * Creates a MultiSplitPane with its LayoutManager set to
     * an empty MultiSplitLayout.
     */
    public JXMultiSplitPane()
    {
        this( new MultiSplitLayout() );
    }

    /**
     * Creates a MultiSplitPane.
     *
     * @param layout The layout to use.
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
     * @return The layout manager cast to MultiSplitLayout.
     */
    private final MultiSplitLayout getMultiSplitLayout()
    {
        return (MultiSplitLayout)getLayout();
    }

    /**
     * Sets the model.
     *
     * @param model The new model.
     * @see MultiSplitLayout#setModel
     */
    public final void setModel( Split model )
    {
        getMultiSplitLayout().setModel(model);
    }

    /**
     * Get the model.
     *
     * @return The model.
     */
    public Split getModel()
    {
        return getMultiSplitLayout().getModel();
    }

    /**
     * Sets the MultiSplitLayout's dividerSize.
     *
     * @param dividerSize The dividerSize in pixels.
     * @see MultiSplitLayout#setDividerSize
     */
    public final void setDividerSize(int dividerSize)
    {
        getMultiSplitLayout().setDividerSize(dividerSize);
    }

    /**
     * Get the MultiSplitLayout dividerSize.
     *
     * @return The divider size in pixels.
     * @see MultiSplitLayout#getDividerSize
     */
    public final int getDividerSize()
    {
        return getMultiSplitLayout().getDividerSize();
    }

    /**
     * @return {@code true} if a drag is active.
     */
    private boolean inDrag()
    {
        return _dragPoint != null;
    }

    /**
     * The drag divider that is currently dragged.
     */
    private MultiSplitLayout.DividerImpl dragDivider = null;

    /**
     * Fallback bounds if the drag is canceled.
     */
    private Point _cancelDragPoint = null;

    /**
     * The point that is the start of a drag increment.
     */
    private Point _dragPoint = null;

    private void startDrag( Point p )
    {
        requestFocusInWindow();

        MultiSplitLayout msl = getMultiSplitLayout();

        dragDivider = msl.dividerAt( p );

        if ( dragDivider == null ) {
            LOG.warning( "No divider." );
            return;
        }

        _cancelDragPoint = new Point( p );
        _dragPoint = p;

        LOG.info( "Start drag: " + _dragPoint );
    }

    private void updateDrag( Point p )
    {
        if (!inDrag()) {
            return;
        }

        _dragPoint = dragDivider.move( _dragPoint, p );

        revalidate();
    }

    private void finishDrag()
    {
        if ( inDrag() )
            clearDragState();

        setCursor(Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
    }

    private void cancelDrag()
    {
        if ( ! inDrag() )
            return;

        dragDivider.move(
                _dragPoint,
                _cancelDragPoint );
        setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.DEFAULT_CURSOR) );
        clearDragState();

        revalidate();
    }

    private void clearDragState()
    {
        dragDivider = null;
        _dragPoint = null;
        _cancelDragPoint = null;
    }

    private void updateCursor( Point p, boolean show )
    {
        if ( inDrag() )
            return;

        int cursorID = Cursor.DEFAULT_CURSOR;

        if ( show )
        {
            MultiSplitLayout.DividerImpl divider =
                    getMultiSplitLayout().dividerAt( p );

            if ( divider != null )
            {
                cursorID  = divider.isVertical() ?
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

    private static final long serialVersionUID = 4848027846403598426L;
}
