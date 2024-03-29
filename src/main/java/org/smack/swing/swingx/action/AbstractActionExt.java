/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2001-2023 Michael Binz
 */
package org.smack.swing.swingx.action;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.KeyStroke;

import org.smack.util.StringUtil;

/**
 * Extends the concept of the Action to include toggle or group states.
 * <p>
 * SwingX 1.6.3 updates {@code AbstractActionExt} to use new features of {@link Action} that were
 * added in {@code Java 1.6}. The selection is now managed with {@link Action#SELECTED_KEY}, which
 * allows the action to correctly configured Swing buttons. The {@link #LARGE_ICON} has also been
 * changed to correspond to {@link Action#LARGE_ICON_KEY}.
 *
 * @author Michael Binz
 * @author swingx
 */
@SuppressWarnings("serial")
public abstract class AbstractActionExt extends AbstractAction
    implements ItemListener
{
    /**
     * The key for the button group
     */
    public static final String GROUP = "__Group__";

    /**
     * The key for the flag which indicates that this is a state type.
     */
    public static final String IS_STATE = "__State__";

    /**
     * Default constructor, does nothing.
     */
    public AbstractActionExt() {
        this((String) null);
    }

    /**
     * Copy constructor copies the state.
     */
    public AbstractActionExt(AbstractActionExt action) {
        Object[] keys = action.getKeys();
        for (int i = 0; i < keys.length; i++) {
            putValue((String)keys[i], action.getValue((String)keys[i]));
        }
        this.enabled = action.enabled;

        // Copy change listeners.
        PropertyChangeListener[] listeners = action.getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            addPropertyChangeListener(listeners[i]);
        }
    }

    public AbstractActionExt(String name) {
        super(name);
    }

    public AbstractActionExt(String name, Icon icon) {
        super(name, icon);
    }

    /**
     * Constructs an Action with the label and command
     *
     * @param name name of the action usually used as a label
     * @param command command key of the action
     */
    public AbstractActionExt(String name, String command) {
        this(name);
        setActionCommand(command);
    }

    /**
     * @param name display name of the action
     * @param command the value of the action command key
     * @param icon icon to display
     */
    public AbstractActionExt(String name, String command, Icon icon) {
        super(name, icon);
        setActionCommand(command);
    }

    /**
     * Returns a short description of the action.
     *
     * @return the short description or null
     */
    public String getShortDescription()  {
        return (String)getValue(Action.SHORT_DESCRIPTION);
    }
    public String shortDescription()  {
        return getShortDescription();
    }

    /**
     * Sets the short description of the action. This will also
     * set the long description value if it is null.
     * <p>
     * This is a convenience method for <code>putValue</code> with the
     * <code>Action.SHORT_DESCRIPTION</code> key.
     *
     * @param desc the short description; can be <code>null</code>w
     * @see Action#SHORT_DESCRIPTION
     * @see Action#putValue
     */
    public void setShortDescription(String desc) {
        putValue(Action.SHORT_DESCRIPTION, desc);
        if (desc != null && getLongDescription() == null) {
            setLongDescription(desc);
        }
    }
    public AbstractActionExt shortDescription(String desc) {
        setShortDescription( desc );
        return this;
    }

    /**
     * Returns the tooltip of the action.
     * Equivalent to {@link #getShortDescription()}.
     *
     * @return the short description or {@code null}.
     */
    public String getTooltip()  {
        return getShortDescription();
    }
    public String tooltip()  {
        return getTooltip();
    }

    /**
     * Sets the tooltip of the action. This will also
     * set the long description value if it is {@code null}.
     * Equivalent to {@link #setShortDescription(String)}.
     *
     * @param desc the short description; can be {@code null}.
     */
    public void setTooltip(String desc) {
        setShortDescription( desc );
    }
    public AbstractActionExt tooltip(String desc) {
        setTooltip( desc );
        return this;
    }

    /**
     * Returns a long description of the action.
     *
     * @return the long description or null
     */
    public String getLongDescription()  {
        return (String)getValue(Action.LONG_DESCRIPTION);
    }
    public String longDescription()  {
        return longDescription();
    }

    /**
     * Sets the long description of the action. This will also set the
     * value of the short description if that value is null.
     * <p>
     * This is a convenience method for <code>putValue</code> with the
     * <code>Action.LONG_DESCRIPTION</code> key.
     *
     * @param desc the long description; can be <code>null</code>
     * @see Action#LONG_DESCRIPTION
     * @see Action#putValue
     */
    public void setLongDescription(String desc) {
        putValue(Action.LONG_DESCRIPTION, desc);
        if (desc != null && getShortDescription() == null) {
            setShortDescription(desc);
        }
    }
    public AbstractActionExt longDescription(String desc) {
        setLongDescription( desc );
        return this;
    }

    /**
     * Returns a small icon which represents the action.
     *
     * @return the small icon or null
     */
    public Icon getSmallIcon() {
        return (Icon)getValue(SMALL_ICON);
    }
    public Icon smallIcon() {
        return getSmallIcon();
    }

    /**
     * Sets the small icon which represents the action.
     * <p>
     * This is a convenience method for <code>putValue</code> with the
     * <code>Action.SMALL_ICON</code> key.
     *
     * @param icon the small icon; can be <code>null</code>
     * @see Action#SMALL_ICON
     * @see Action#putValue
     */
    public void setSmallIcon(Icon icon) {
        putValue(SMALL_ICON, icon);
    }
    public AbstractActionExt smallIcon(Icon icon) {
        setSmallIcon( icon );
        return this;
    }

    /**
     * Returns a small icon which represents the action.
     * Equivalent to {@link #getSmallIcon()}.
     *
     * @return the small icon or null
     */
    public Icon getIcon() {
        return getSmallIcon();
    }
    public Icon icon() {
        return getIcon();
    }

    /**
     * Sets the small icon which represents the action.
     * Equivalent to {@link #setSmallIcon(Icon)}.
     * <p>
     * This is a convenience method for <code>putValue</code> with the
     * <code>Action.SMALL_ICON</code> key.
     *
     * @param icon the small icon; can be <code>null</code>
     * @see Action#SMALL_ICON
     * @see Action#putValue
     */
    public void setIcon(Icon icon) {
        setSmallIcon( icon );
    }
    public AbstractActionExt icon(Icon icon) {
        setIcon( icon );
        return this;
    }

    /**
     * Returns a large icon which represents the action.
     *
     * @return the large icon or null
     */
    public Icon getLargeIcon() {
        return (Icon)getValue( LARGE_ICON_KEY );
    }
    public Icon largeIcon() {
        return getLargeIcon();
    }

    /**
     * Sets the large icon which represents the action.
     * <p>
     * This is a convenience method for <code>putValue</code> with the
     * <code>LARGE_ICON</code> key.
     *
     * @param icon the large icon; can be <code>null</code>
     * @see #LARGE_ICON
     * @see Action#putValue
     */
    public void setLargeIcon(Icon icon) {
        putValue( LARGE_ICON_KEY, icon );
    }
    public AbstractActionExt largeIcon(Icon icon) {
        setLargeIcon( icon );
        return this;
    }

    /**
     * Returns the name of the action.
     *
     * @return The name of the Action; can be {@code null}.
     */
    public String getName() {
        return (String)getValue( NAME );
    }
    public String name() {
        return getName();
    }

    /**
     * Sets the name of the action.
     * This is a convenience method for {@code putValue(Action.NAME)}.
     *
     * @param name The name of the Action; can be {@code null}.
     */
    public void setName(String name) {
        putValue( NAME, name );
    }
    public AbstractActionExt name(String name) {
        setName( name );
        return this;
    }

    /**
     * Returns the name of the action.  Equivalent to {@link #getName()}.
     *
     * @return The name of the Action; can be {@code null}.
     */
    public String getText() {
        return getName();
    }
    public String text() {
        return getText();
    }

    /**
     * Sets the text of the action. Equivalent to {@link #setName(String)}.
     * This is a convenience method for {@code putValue(Action.NAME)}.
     *
     * @param name The name of the Action; can be {@code null}.
     */
    public void setText(String name) {
        setName( name );
    }
    public AbstractActionExt text(String name) {
        setText( name );
        return this;
    }

    /**
     * Returns the action name of the passed action. This is a convenience method
     * for {@code getValue()} with the {@code Action.NAME} key.
     *
     * @param action the target action.
     * @return The name of the Action; can be {@code null}.
     * @see #getText()
     */
    public static String getActionText( Action action ) {
        return (String) action.getValue( NAME );
    }

    /**
     * Sets the action name of the passed action. This is a convenience method
     * for {@code putValue()} with the {@code Action.NAME} key.
     *
     * @param action the target action.
     * @param name The name of the Action; can be {@code null}.
     * @see #setText(String)
     */
    public static void setActionText( Action action,  String name ) {
        action.putValue( NAME, name );
    }

    public void setMnemonic(String mnemonic) {
        if ( StringUtil.hasContent( mnemonic ) ) {
            putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemonic.charAt(0)));
        }
    }

    /**
     * Return the mnemonic key code for the action.
     *
     * @return the mnemonic or 0
     */
    public int getMnemonic() {
        Integer value = (Integer)getValue(Action.MNEMONIC_KEY);
        if (value != null) {
            return value.intValue();
        }
        return '\0';
    }
    public int mnemonic() {
        return getMnemonic();
    }

    /**
     * Sets the mnemonic key code for the action.
     * <p>
     * This is a convenience method for <code>putValue</code> with the
     * <code>Action.MNEMONIC_KEY</code> key.
     * <p>
     * This method does not validate the value. Please see
     * {@link javax.swing.AbstractButton#setMnemonic(int)} for details
     * concerning the value of the mnemonic.
     *
     * @param mnemonic an int key code mnemonic or 0
     * @see javax.swing.AbstractButton#setMnemonic(int)
     * @see Action#MNEMONIC_KEY
     * @see Action#putValue
     */
    public void setMnemonic(int mnemonic) {
        putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemonic));
    }
    public AbstractActionExt mnemonic(int mnemonic) {
        setMnemonic( mnemonic );
        return this;
    }

    /**
     * Returns the action command.
     *
     * @return the action command or null
     */
    public String getActionCommand() {
        return getActionCommand( this );
    }
    public String actionCommand() {
        return getActionCommand();
    }

    /**
     * Sets the action command key. The action command key
     * is used to identify the action.
     * <p>
     * This is a convenience method for <code>putValue</code> with the
     * <code>Action.ACTION_COMMAND_KEY</code> key.
     *
     * @param key the action command
     * @see Action#ACTION_COMMAND_KEY
     * @see Action#putValue
     */
    public void setActionCommand(String key) {
        setActionCommand( this, key );
    }
    public AbstractActionExt actionCommand(String key) {
        setActionCommand( key );
        return this;
    }

    /**
     * Returns the action command of the passed action.
     *
     * @param action the target action.
     * @return the action command or null
     */
    public static String getActionCommand( Action action ) {
        return (String) action.getValue(Action.ACTION_COMMAND_KEY);
    }

    /**
     * Sets the action command key of the passed action. The action command key
     * is used to identify the action.
     * <p>
     * This is a convenience method for <code>putValue</code> with the
     * <code>Action.ACTION_COMMAND_KEY</code> key.
     *
     * @param action the target action.
     * @param key the action command
     * @see Action#ACTION_COMMAND_KEY
     * @see Action#putValue
     */
    public static void setActionCommand( Action action,  String key ) {
        action.putValue(Action.ACTION_COMMAND_KEY, key);
    }

    /**
     * Returns the key stroke which represents an accelerator
     * for the action.
     *
     * @return the key stroke or null
     */
    public KeyStroke getAccelerator() {
        return (KeyStroke)getValue(Action.ACCELERATOR_KEY);
    }
    public KeyStroke accelerator() {
        return getAccelerator();
    }

    /**
     * Sets the key stroke which represents an accelerator
     * for the action.
     * <p>
     * This is a convenience method for <code>putValue</code> with the
     * <code>Action.ACCELERATOR_KEY</code> key.
     *
     * @param key the key stroke; can be <code>null</code>
     * @see Action#ACCELERATOR_KEY
     * @see Action#putValue
     */
    public void setAccelerator(KeyStroke key) {
        putValue(Action.ACCELERATOR_KEY, key);
    }
    public AbstractActionExt accelerator(KeyStroke key) {
        setAccelerator(key);
        return this;
    }

    public Object getGroup() {
        return getValue(GROUP);
    }
    public Object group() {
        return getGroup();
    }

    /**
     * Sets the group identity of the state action. This is used to
     * identify the action as part of a button group.
     */
    public void setGroup(Object group) {
        putValue(GROUP, group);
    }
    public AbstractActionExt group(Object group) {
        setGroup( group );
        return this;
    }

    /**
     * Indicates if this action has states. If this method returns
     * true then the this will send ItemEvents to ItemListeners
     * when the control constructed with this action in invoked.
     *
     * @return true if this can handle states
     */
    public boolean isStateAction() {
        Boolean state = (Boolean)getValue(IS_STATE);
        if (state != null) {
            return state.booleanValue();
        }
        return false;
    }
    public boolean stateAction() {
        return isStateAction();
    }

    /**
     * Set the state property to true.
     */
    public void setStateAction() {
        setStateAction(true);
    }

    /**
     * Set the state property.
     *
     * @param state if true then this action will fire ItemEvents
     */
    public void setStateAction(boolean state) {
        putValue(IS_STATE, Boolean.valueOf(state));
    }
    public AbstractActionExt stateAction(boolean state) {
        setStateAction( state );
        return this;
    }

    /**
     * @return true if the action is in the selected state
     */
    public boolean isSelected() {
        Boolean selected = (Boolean) getValue(SELECTED_KEY);

        if (selected == null) {
            return false;
        }

        return selected.booleanValue();
    }
    public boolean selected() {
        return isSelected();
    }

    /**
     * Changes the state of the action. This is a convenience method for updating the Action via the
     * value map.
     *
     * @param newValue
     *            true to set the action as selected of the action.
     * @see Action#SELECTED_KEY
     */
    public void setSelected(boolean newValue) {
        putValue(SELECTED_KEY, newValue);
    }
    public AbstractActionExt selected(boolean newValue) {
        setSelected( newValue );
        return this;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("[");
        // RG: Fix for J2SE 5.0; Can't cascade append() calls because
        // return type in StringBuffer and AbstractStringBuilder are different
        buffer.append(this.getClass().toString());
        buffer.append(":");
        try {
            Object[] keys = getKeys();
            for (int i = 0; i < keys.length; i++) {
                buffer.append(keys[i]);
                buffer.append('=');
                buffer.append(getValue( (String) keys[i]).toString());
                if (i < keys.length - 1) {
                    buffer.append(',');
                }
            }
            buffer.append(']');
        }
        catch (Exception ex) {  // RG: append(char) throws IOException in J2SE 5.0
            /** @todo Log it */
        }
        return buffer.toString();
    }

    /**
     * Callback method as <code>ItemListener</code>. Updates internal state based
     * on the given ItemEvent. <p>
     *
     * Here: syncs selected property if isStateAction(), does nothing otherwise.
     *
     * @param e the ItemEvent fired by a ItemSelectable on changing the selected
     *    state.
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (isStateAction()) {
            setSelected(ItemEvent.SELECTED == e.getStateChange());
        }
    }

    /**
     * Will perform cleanup on the object.
     * Should be called when finished with the Action. This should be used if
     * a new action is constructed from the properties of an old action.
     * The old action properties should be disposed.
     */
    public void dispose() {
        PropertyChangeListener[] listeners = getPropertyChangeListeners();
        for (int i = 0; i < listeners.length; i++) {
            removePropertyChangeListener(listeners[i]);
        }
    }
}
