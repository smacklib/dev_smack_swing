/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2001-2022 Michael Binz
 */
package org.smack.swing.application.session;

import java.awt.Component;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.JFrame;

import org.smack.swing.SwingUtil;

/**
 * A {@code sessionState} property for Window.
 * <p>
 * This class defines how the session state for {@code Windows}
 * is {@link WindowProperty#getSessionState saved} and
 * and {@link WindowProperty#setSessionState restored} in
 * terms of a property called {@code sessionState}.  The
 * Window's {@code bounds Rectangle} is saved and restored
 * if the dimensions of the Window's screen have not changed.
 * <p>
 * {@code WindowProperty} is registered for {@code Window.class} by
 * default, so this class applies to the AWT {@code Window},
 * {@code Dialog}, and {@code Frame} class, as well as their
 * Swing counterparts: {@code JWindow}, {@code JDialog}, and
 * {@code JFrame}.
 *
 * @see org.smack.swing.application.SessionStorage#save
 * @see org.smack.swing.application.SessionStorage#restore
 * @see WindowState
 */
public class WindowProperty implements PropertySupport {

	private void checkComponent(Component component) {
        if (component == null) {
            throw new IllegalArgumentException("null component");
        }
        if (!(component instanceof Window)) {
            throw new IllegalArgumentException("invalid component");
        }
    }

    private int getScreenCount() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices().length;
    }

    /**
     * Returns a {@link WindowState WindowState} object
     * for {@code Window c}.
     * <p>
     * Throws an {@code IllegalArgumentException} if {@code Component c}
     * isn't a non-null {@code Window}.
     *
     * @param c the {@code Window} whose bounds will be stored
     *     in a {@code WindowState} object.
     * @return the {@code WindowState} object
     * @see #setSessionState
     * @see WindowState
     */
    @Override
    public Object getSessionState(Component c) {
        checkComponent(c);
        int frameState = Frame.NORMAL;
        if (c instanceof Frame) {
            frameState = ((Frame) c).getExtendedState();
        }
        GraphicsConfiguration gc = c.getGraphicsConfiguration();
        Rectangle gcBounds = (gc == null) ? null : gc.getBounds();
        Rectangle frameBounds = c.getBounds();
        /* If this is a JFrame created by FrameView and it's been maximized,
         * retrieve the frame's normal (not maximized) bounds.  More info:
         * see FrameStateListener#windowStateChanged in FrameView.
         */
        if ((c instanceof JFrame) && (0 != (frameState & Frame.MAXIMIZED_BOTH))) {
            frameBounds = SwingUtil.getWindowNormalBounds((JFrame)c);
        }

        // Michab added null check.
        if ( frameBounds == null || frameBounds.isEmpty() )
            return null;

        return new WindowState(frameBounds, gcBounds, getScreenCount(), frameState);
    }

    /**
     * Restore the {@code Window's} bounds if the dimensions of its
     * screen ({@code GraphicsConfiguration}) haven't changed, the
     * number of screens hasn't changed, and the
     * {@link Window#isLocationByPlatform isLocationByPlatform}
     * property, which indicates that native Window manager should
     * pick the Window's location, is false.  More precisely:
     * <p>
     * If {@code state} is non-null, and Window {@code c's}
     * {@code GraphicsConfiguration}
     * {@link GraphicsConfiguration#getBounds bounds} matches
     * the {@link WindowState#getGraphicsConfigurationBounds WindowState's value},
     * and Window {@code c's}
     * {@link Window#isLocationByPlatform isLocationByPlatform}
     * property is false, then set the Window's to the
     * {@link WindowState#getBounds saved value}.
     * <p>
     * Throws an {@code IllegalArgumentException} if {@code c} is
     * not a {@code Window} or if {@code state} is non-null
     * but not an instance of {@link WindowState}.
     *
     * @param c the Window whose state is to be restored
     * @param state the {@code WindowState} to be restored
     * @see #getSessionState
     * @see WindowState
     */
    @Override
    public void setSessionState(Component c, Object state) {
        checkComponent(c);
        if ((state != null) && !(state instanceof WindowState)) {
            throw new IllegalArgumentException("invalid state");
        }
        Window w = (Window) c;
        WindowState windowState = (WindowState) state;
        SwingUtil.putWindowNormalBounds(w, windowState.getBounds());
        if (!w.isLocationByPlatform() && (state != null)) {

            Rectangle gcBounds0 = windowState.getGraphicsConfigurationBounds();
            if (gcBounds0 != null && SwingUtil.isResizable(w)) {
                if (SwingUtil.computeVirtualGraphicsBounds().contains(gcBounds0.getLocation())) {
                    w.setBounds(windowState.getBounds());
                } else {
                    w.setSize(windowState.getBounds().getSize());
                }
            }
            if (w instanceof Frame) {
                ((Frame) w).setExtendedState(windowState.getFrameState());

            }
        }
    }
}
