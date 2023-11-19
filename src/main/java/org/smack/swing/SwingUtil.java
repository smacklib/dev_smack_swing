/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2001-2023 Michael Binz
 */
package org.smack.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.Random;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.smack.swing.application.Application;
import org.smack.swing.application.SingleFrameApplication;
import org.smack.util.StringUtil;

/**
 * Tools.
 *
 * @version $Revision$
 */
public class SwingUtil
{
    /**
     * Icon for information messages. This is the default icon.
     */
    public static final Icon ICON_INFO = UIManager.getIcon( "OptionPane.informationIcon" );

    /**
     * Icon for warning messages.
     */
    public static final Icon ICON_WARN = UIManager.getIcon("OptionPane.warningIcon");

    /**
     * Icon for error messages.
     */
    public static final Icon ICON_ERROR = UIManager.getIcon("OptionPane.errorIcon");

    /**
     * Icon for information messages. This is the default icon.
     */
    public static final Icon ICON_QUESTION = UIManager.getIcon( "OptionPane.questionIcon" );

    /**
     * @param c
     *            the component to add the double click action
     * @param a
     *            the action to add
     */
    public static void registerDoubleClick(Component c, final Action a) {
        c.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ev) {
                if (ev.getClickCount() == 2
                        && SwingUtilities.isLeftMouseButton(ev)
                        && a.isEnabled()) {
                    a.actionPerformed(new ActionEvent(ev.getSource(),
                            ActionEvent.ACTION_PERFORMED, (String) a
                                    .getValue(Action.ACTION_COMMAND_KEY)));
                }
            }
        });
    }

    /**
     * Shows a modal dialog asking the user about the given message.
     *
     * Provides YES and NO options. The dialog has the title "Confirm".
     *
     * @param pParent
     *            the parent component relative to which the displayed dialog
     *            will be modal.
     * @param pMessage
     *            the message to be displayed in the dialog.
     * @return whether the user confirmed with YES.
     */
    public static boolean confirm(Component pParent, String pMessage)
    {
        int option = JOptionPane.showConfirmDialog(
                    pParent,
                    pMessage,
                    getText( SwingUtil.class, "confirmDialogTitle" ),
                    JOptionPane.YES_NO_OPTION);
        return JOptionPane.YES_OPTION == option;
    }

    /**
     * Registers the given action to be fired when the ESC key is pressed in
     * the given dialog.
     *
     * @param pDialog
     * @param pEscAction
     */
    public static void registerEsc(
            final JDialog pDialog,
            final Action pEscAction)
    {
        JLayeredPane layeredPane = pDialog.getLayeredPane();
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-it");
        layeredPane.getActionMap().put("close-it", pEscAction);
    }

    /**
     * Registers the given action to be fired when the given window is closed.
     *
     * @param pDialog
     *            the dialog on which closing to listen.
     * @param pCancelAction
     *            the action to fire when the dialog closes. This is usualiy the
     *            same action as in the {@link #registerEsc(JDialog, Action)}.
     */
    public static void registerWindowClosing(
            final JDialog pDialog,
            final Action pCancelAction)
    {
        pDialog.addWindowListener(
                new WindowAdapter()
                {
                    @Override
                    public void windowClosing(WindowEvent pE)
                    {
                        pCancelAction.actionPerformed( new ActionEvent(
                                pE.getSource(),
                                pE.getID(),
                                null ) );
                    }
                } );
    }

    /**
     * Registers the given action to be fired when the Enter key is pressed in
     * the given dialog.
     *
     * @param dlg
     * @param pEnterAction
     */
    public static void registerEnter(final JDialog dlg, Action pEnterAction) {
        JLayeredPane layeredPane = dlg.getLayeredPane();
        layeredPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "doenter-it");
        layeredPane.getActionMap().put("doenter-it", pEnterAction);
    }

    /**
     * Adds the key binding to the given action.
     *
     * @param pComp
     *            the component on which to register the key stroke.
     * @param pKeyStroke
     *            e.g. KeyStroke.getKeyStroke( KeyEvent.VK_ENTER,
     *            InputEvent.ALT_DOWN_MASK )
     * @param pCondition
     *            e.g. {@link JComponent#WHEN_FOCUSED}
     * @param pAction
     *            the action to execute when the key stroke is entered.
     */
    public static void addKeyBinding(
            JComponent pComp,
            KeyStroke pKeyStroke,
            int pCondition,
            Action pAction)
    {
        Object key = pAction.getValue(Action.ACTION_COMMAND_KEY);
        if (key == null) {
            key = pAction.getValue(Action.NAME);
        }
        if (key == null) {
            key = "Action" + new Random().nextInt();
        }
        pComp.getInputMap( pCondition ).put( pKeyStroke, key );
        pComp.getActionMap().put( key, pAction );
        pAction.putValue(Action.ACCELERATOR_KEY, pKeyStroke );
    }

    /**
     * Returns the application's main frame.
     *
     * @return The application's main frame.
     */
    public static JFrame getFrame() {
        return Application.getInstance( SingleFrameApplication.class ).getMainFrame();
    }

    /**
     * Returns the localized text for the given key and class.
     *
     * @param pClass
     *            the class which properties to use as resource.
     * @param pKey
     *            the resource key.
     * @param pArgs
     *            optional arguments for the message. If this array is not null
     *            or empty then the arguments in it will be replaced in the
     *            localised resource text as by the method
     *            {@link String#format(String, Object...)}.
     */
    private static String getText(Class<?> pClass, String pKey, Object... pArgs) {
        if (pArgs == null)
            pArgs = new Object[0];

        return Application.getResourceManager().getResourceMap(pClass).getFormatted( pKey, pArgs );
    }

    /**
     * Returns the localized text for the given key and class.
     */
    public static String getText(Class<?> pClass, String pKey) {
        return getText( pClass, pKey, new Object[0] );
    }

    /**
     * Looks up the action for the given name in the action map associated
     * with the passed object.
     *
     * @param pObj The object that carries the action.
     * @param pName The name of the action.
     */
    public static Action getAction(Object pObj, String pName) {
        return getAction( pObj.getClass(), pObj, pName );
    }

    /**
     * Looks up the action for the given name in the action map associated
     * with the passed object.
     *
     * @param pClass The target class.
     * @param pObject The target instance.
     * @return The Action reference.
     * @throws IllegalArgumentException If the Action was not found.
     */
    private static Action getAction(
            Class<?> pClass,
            Object pObject,
            String pName)
    {
        Action a = Application.getInstance().getContext().
                    getActionMap(pClass, pObject).get(pName);

        if ( a == null )
        {
            throw new IllegalArgumentException( String.format(
                "Action '%s' not found on class '%s'.",
                    pName,
                    pClass.getName() ) );
        }

        if (a.getValue( Action.ACTION_COMMAND_KEY ) == null)
            a.putValue( Action.ACTION_COMMAND_KEY, pName );
        return a;
    }

    /**
     * The standard gap between the components.
     */
    public static final int GAP = 10;

    /**
     * Empty border with GAP space on all sides.
     */
    public static final Border GAP_BORDER = BorderFactory.createEmptyBorder(
            GAP, GAP, GAP, GAP);

    private static final String WINDOW_STATE_NORMAL_BOUNDS = "WindowState.normalBounds";

    /**
     * Calculates virtual graphic bounds.
     * On multiscreen systems all screens are united into one virtual screen.
     * @return the graphic bounds
     */
    public static Rectangle computeVirtualGraphicsBounds() {
        Rectangle virtualBounds = new Rectangle();
        GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        GraphicsDevice[] gs = ge.getScreenDevices();
        for (GraphicsDevice gd : gs) {
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            virtualBounds = virtualBounds.union(gc.getBounds());
        }
        return virtualBounds;
    }

    /**
     * Checks whether the window supports resizing
     * @param window the {@code Window} to be checked
     * @return true if the window supports resizing
     */
    public static boolean isResizable(Window window) {
        boolean resizable = true;
        if (window instanceof Frame) {
            resizable = ((Frame) window).isResizable();
        } else if (window instanceof Dialog) {
            resizable = ((Dialog) window).isResizable();
        }
        return resizable;
    }

    /**
     * Calculates default location for the specified window.
     * @return default location for the window
     * @param window the window location is calculated for.
     *               It should not be null.
     */
    public static Point defaultLocation(Window window) {
        GraphicsConfiguration gc = window.getGraphicsConfiguration();
        Rectangle bounds = gc.getBounds();
        Insets insets = window.getToolkit().getScreenInsets(gc);
        int x = bounds.x + insets.left;
        int y = bounds.y + insets.top;
        return new Point(x, y);
    }

    /**
     * Finds the nearest RootPaneContainer of the provided Component.
     * Primarily, if a JPopupMenu (such as used by JMenus when they are visible) has no parent,
     * the search continues with the JPopupMenu's invoker instead.
     *
     * @return a RootPaneContainer for the provided component
     * @param root the Component
     */
    public static RootPaneContainer findRootPaneContainer(Component root) {
        while (root != null) {
            if (root instanceof RootPaneContainer) {
                return (RootPaneContainer) root;
            } else if (root instanceof JPopupMenu && root.getParent() == null) {
                root = ((JPopupMenu) root).getInvoker();
            } else {
                root = root.getParent();
            }
        }
        return null;
    }

    /**
     * Gets {@code Window} bounds from the client property
     * @param window the source {@code Window}
     * @return bounds from the client property
     */
    public static Rectangle getWindowNormalBounds(Window window) {
        if (window instanceof JFrame) {
            Object res = ((JFrame) window).getRootPane().getClientProperty(WINDOW_STATE_NORMAL_BOUNDS);
            if (res instanceof Rectangle) {
                return (Rectangle) res;
            }
        }
        return null;
    }

    /**
     * Puts {@code Window} bounds to client property.
     * @param window the target {@code Window}
     * @param bounds bounds
     */
    public static void putWindowNormalBounds(Window window, Rectangle bounds) {
        if (window instanceof JFrame) {
            ((JFrame) window).getRootPane().putClientProperty(WINDOW_STATE_NORMAL_BOUNDS, bounds);
        }
    }

    /**
     * Returns position and size of the inner painting area of a component.
     * Differs from
     * {@link SwingUtilities#calculateInnerArea(JComponent, Rectangle)}
     * by offering a Container-typed argument.
     *
     * @param c The component.
     * @return The inner area.
     */
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

    /**
     * Access the text that is currently in the clipboard.
     *
     * @return The text currently in the clipboard.  If the clipboard
     * does not contain text, the empty string is returned.
     */
    public static String getClipboardText()
    {
        try
        {
            String result = (String)
                    Toolkit.
                    getDefaultToolkit().
                    getSystemClipboard().
                    getData(
                            DataFlavor.stringFlavor);

            if ( result == null )
                return StringUtil.EMPTY_STRING;

            return result;
        }
        catch ( Exception ignore )
        {
            return StringUtil.EMPTY_STRING;
        }
    }

    /**
     * This is a library and cannot be instantiated.
     */
    private SwingUtil()
    {
        throw new AssertionError();
    }
}
