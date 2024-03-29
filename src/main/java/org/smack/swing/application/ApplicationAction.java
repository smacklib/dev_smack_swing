/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package org.smack.swing.application;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.swing.ActionMap;

import org.smack.swing.swingx.action.MackAction;
import org.smack.util.ReflectionUtil;
import org.smack.util.StringUtil;
import org.smack.util.resource.ResourceMap;


/**
 * The {@link javax.swing.Action} class used to implement the
 * <tt>&#064;Action</tt> annotation.  This class is typically not
 * instantiated directly, it's created as a side effect of constructing
 * an <tt>ApplicationActionMap</tt>:
 * <pre>
 * public class MyActions {
 *     &#064;Action public void anAction() { }  // an &#064;Action named "anAction"
 * }
 * ApplicationContext ac = ApplicationContext.getInstance();
 * ActionMap actionMap = ac.getActionMap(new MyActions());
 * myButton.setAction(actionMap.get("anAction"));
 * </pre>
 *
 * <p>
 * When an ApplicationAction is constructed, it initializes all of its
 * properties from the specified <tt>ResourceMap</tt>.  Resource names
 * must match the {@code @Action's} name, which is the name of the
 * corresponding method, or the value of the optional {@code @Action} name
 * parameter.  To initialize the text and shortDescription properties
 * of the action named <tt>"anAction"</tt> in the previous example, one
 * would define two resources:
 * <pre>
 * anAction.Action.text = Button/Menu/etc label text for anAction
 * anAction.Action.shortDescription = Tooltip text for anAction
 * </pre>
 *
 * <p>
 * A complete description of the mapping between resources and Action
 * properties can be found in the ApplicationAction {@link
 * #ApplicationAction constructor} documentation.
 *
 * <p>
 * An ApplicationAction's <tt>enabled</tt> and <tt>selected</tt>
 * properties can be delegated to boolean properties of the
 * Actions class, by specifying the corresponding property names.
 * This can be done with the {@code @Action} annotation, e.g.:
 * <pre>
 * public class MyActions {
 *     &#064;Action(enabledProperty = "anActionEnabled")
 *     public void anAction() { }
 *     public boolean isAnActionEnabled() {
 *         // will fire PropertyChange when anActionEnabled changes
 *         return anActionEnabled;
 *     }
 * }
 * </pre>
 * If the MyActions class supports PropertyChange events, then then
 * ApplicationAction will track the state of the specified property
 * ("anActionEnabled" in this case) with a PropertyChangeListener.
 *
 * <p>
 * ApplicationActions can automatically <tt>block</tt> the GUI while the
 * <tt>actionPerformed</tt> method is running, depending on the value of
 * block annotation parameter.  For example, if the value of block is
 * <tt>Task.BlockingScope.ACTION</tt>, then the action will be disabled while
 * the actionPerformed method runs.
 *
 * <p>
 * An ApplicationAction can have a <tt>proxy</tt> Action, i.e.
 * another Action that provides the <tt>actionPerformed</tt> method,
 * the enabled/selected properties, and values for the Action's long
 * and short descriptions.  If the proxy property is set, this
 * ApplicationAction tracks all of the aforementioned properties, and
 * the <tt>actionPerformed</tt> method just calls the proxy's
 * <tt>actionPerformed</tt> method.  If a <tt>proxySource</tt> is
 * specified, then it becomes the source of the ActionEvent that's
 * passed to the proxy <tt>actionPerformed</tt> method.  Proxy action
 * dispatching is as simple as this:
 * <pre>
 * public void actionPerformed(ActionEvent actionEvent) {
 *     javax.swing.Action proxy = getProxy();
 *     if (proxy != null) {
 *         actionEvent.setSource(getProxySource());
 *         proxy.actionPerformed(actionEvent);
 *     }
 *     // ....
 * }
 * </pre>
 *
 * @version $Rev$
 * @author Michael Binz
 * @author Hans Muller (Hans.Muller@Sun.COM)
 * @see ApplicationContext#getActionMap(Object)
 * @see ResourceMap
 * @deprecated Use {@link Action8}
 */
@Deprecated
@SuppressWarnings("serial")
public class ApplicationAction extends MackAction
{
    private final ApplicationActionMap appAM;
    private final Method actionMethod;      // The @Action method
    private final String enabledProperty;   // names a bound appAM.getActionsClass() property
    private final Method isEnabledMethod;   // Method object for is/getEnabledProperty
    private final Method setEnabledMethod;  // Method object for setEnabledProperty
    private final String selectedProperty;  // names a bound appAM.getActionsClass() property
    private final Method isSelectedMethod;  // Method object for is/getSelectedProperty
    private final Method setSelectedMethod; // Method object for setSelectedProperty
    private final String taskService;
    private final Task.BlockingScope block;
    private javax.swing.Action proxy = null;
    private Object proxySource = null;
    private PropertyChangeListener proxyPCL = null;
    private final boolean enabledNegated; // support for negated enabledProperty

    /**
     * Construct an <tt>ApplicationAction</tt> that implements an <tt>&#064;Action</tt>.
     *
     * <p>
     * If a {@code ResourceMap} is provided, then all of the
     * {@link javax.swing.Action Action} properties are initialized
     * with the values of resources whose key begins with {@code baseName}.
     * ResourceMap keys are created by appending an &#064;Action resource
     * name, like "Action.shortDescription" to the &#064;Action's baseName
     * For example, Given an &#064;Action defined like this:
     * <pre>
     * &#064;Action void actionBaseName() { }
     * </pre>
     * <p>
     * Then the shortDescription resource key would be
     * <code>actionBaseName.Action.shortDescription</code>, as in:
     * <pre>
     * actionBaseName.Action.shortDescription = Do perform some action
     * </pre>
     *
     * <p>
     * The complete set of &#064;Action resources is:
     * <pre>
     * Action.icon
     * Action.text
     * Action.shortDescription
     * Action.longDescription
     * Action.smallIcon
     * Action.largeIcon
     * Action.command
     * Action.accelerator
     * Action.mnemonic
     * Action.displayedMnemonicIndex
     * </pre>
     *
     * <p>
     * A few the resources are handled specially:
     * <ul>
     * <li><tt>Action.text</tt><br>
     * Used to initialize the Action properties with keys
     * <tt>Action.NAME</tt>, <tt>Action.MNEMONIC_KEY</tt> and
     * <tt>Action.DISPLAYED_MNEMONIC_INDEX</tt>.
     * If the resources's value contains an "&" or an "_" it's
     * assumed to mark the following character as the mnemonic.
     * If Action.mnemonic/Action.displayedMnemonic resources are
     * also defined (an odd case), they'll override the mnemonic
     * specfied with the Action.text marker character.
     *
     * <li><tt>Action.icon</tt><br>
     * Used to initialize both ACTION.SMALL_ICON,LARGE_ICON.  If
     * Action.smallIcon or Action.largeIcon resources are also defined
     * they'll override the value defined for Action.icon.
     *
     * <li><tt>Action.displayedMnemonicIndexKey</tt><br>
     * The corresponding javax.swing.Action constant is only defined in Java SE 6.
     * We'll set the Action property in Java SE 5 too.
     * </ul>
     *
     * @param appAM the ApplicationActionMap this action is being constructed for.
     * @param resourceMap initial Action properties are loaded from this ResourceMap.
     * @param baseName the name of the &#064;Action
     * @param actionMethod unless a proxy is specified, actionPerformed calls this method.
     * @param enabledProperty name of the enabled property.
     * @param enabledNegated enabled property is inverted
     * @param selectedProperty name of the selected property.
     * @param taskService name of the task service for this action
     * @param block how much of the GUI to block while this action executes.
     *
     * @see #getName
     * @see ApplicationActionMap#getActionsClass
     * @see ApplicationActionMap#getActionsObject
     */
    public ApplicationAction(ApplicationActionMap appAM,
            org.smack.util.resource.ResourceMap resourceMap,
            String baseName,
            Method actionMethod,
            String enabledProperty,
            boolean enabledNegated, // New in bsaf
            String selectedProperty,
            String taskService, // New in basf
            Task.BlockingScope block) {

        super( baseName, resourceMap );

        if (appAM == null) {
            throw new IllegalArgumentException("null appAM");
        }
        if (baseName == null) {
            throw new IllegalArgumentException("null baseName");
        }
        this.appAM = appAM;
        this.actionMethod = actionMethod;
        this.enabledProperty = enabledProperty;
        this.enabledNegated = enabledNegated;
        this.selectedProperty = selectedProperty;
        this.taskService = taskService;
        this.block = block;

        /* If enabledProperty is specified, lookup up the is/set methods and
         * verify that the former exists.
         */
        if (enabledProperty != null) {
            setEnabledMethod = propertySetMethod(this.enabledProperty, boolean.class);
            isEnabledMethod = propertyGetMethod(this.enabledProperty);
            if (isEnabledMethod == null) {
                throw newNoSuchPropertyException(this.enabledProperty);
            }
        } else {
            this.isEnabledMethod = null;
            this.setEnabledMethod = null;
        }

        /* If selectedProperty is specified, lookup up the is/set methods and
         * verify that the former exists.
         */
        if (selectedProperty != null) {
            setSelectedMethod = propertySetMethod(selectedProperty, boolean.class);
            isSelectedMethod = propertyGetMethod(selectedProperty);
            if (isSelectedMethod == null) {
                throw newNoSuchPropertyException(selectedProperty);
            }
            super.putValue(SELECTED_KEY, invokeBooleanMethod(appAM.getActionsObject(), isSelectedMethod));
        } else {
            this.isSelectedMethod = null;
            this.setSelectedMethod = null;
        }
    }

    /* Shorter convenience constructor used to create ProxyActions,
     * see ApplicationActionMap.addProxyAction().
     */
    ApplicationAction(ApplicationActionMap appAM, org.smack.util.resource.ResourceMap resourceMap, String actionName) {
        this(appAM, resourceMap, actionName, null, null, false, null, TaskService.DEFAULT_NAME, Task.BlockingScope.NONE);
    }

    private IllegalArgumentException newNoSuchPropertyException(String propertyName) {
        String actionsClassName = appAM.getActionsClass().getName();
        String msg = String.format("no property named %s in %s", propertyName, actionsClassName);
        return new IllegalArgumentException(msg);
    }

    /**
     * The name of the {@code @Action} enabledProperty
     * whose value is returned by {@link #isEnabled isEnabled},
     * or null.
     *
     * @return the name of the enabledProperty or null.
     * @see #isEnabled
     */
    String getEnabledProperty() {
        return enabledProperty;
    }

    /**
     * The name of the {@code @Action} selectedProperty whose value is
     * returned by {@link #isSelected isSelected}, or null.
     *
     * @return the name of the selectedProperty or null.
     * @see #isSelected
     */
    String getSelectedProperty() {
        return selectedProperty;
    }

    /**
     * Return the proxy for this action or null.
     *
     * @return the value of the proxy property.
     * @see #setProxy
     * @see #setProxySource
     * @see #actionPerformed
     */
    public javax.swing.Action getProxy() {
        return proxy;
    }

    /**
     * Set the proxy for this action.  If the proxy is non-null then
     * we delegate/track the following:
     * <ul>
     * <li><tt>actionPerformed</tt><br>
     * Our <tt>actionPerformed</tt> method calls the delegate's after
     * the ActionEvent source to be the value of <tt>getProxySource</tt>
     *
     * <li><tt>shortDescription</tt><br>
     * If the proxy's shortDescription, i.e. the value for key
     * {@link javax.swing.Action#SHORT_DESCRIPTION SHORT_DESCRIPTION} is not null,
     * then set this action's shortDescription.  Most Swing components use
     * the shortDescription to initialize their tooltip.
     *
     * <li><tt>longDescription</tt><br>
     * If the proxy's longDescription, i.e. the value for key
     * {@link javax.swing.Action#LONG_DESCRIPTION LONG_DESCRIPTION} is not null,
     * then set this action's longDescription.
     * </ul>
     *
     * @param proxy
     * @see #setProxy
     * @see #setProxySource
     * @see #actionPerformed
     */
    public void setProxy(javax.swing.Action proxy) {
        javax.swing.Action oldProxy = this.proxy;
        this.proxy = proxy;
        if (oldProxy != null) {
            oldProxy.removePropertyChangeListener(proxyPCL);
            proxyPCL = null;
        }
        if (this.proxy != null) {
            updateProxyProperties();
            proxyPCL = new ProxyPCL();
            proxy.addPropertyChangeListener(proxyPCL);
        } else if (oldProxy != null) {
            setEnabled(false);
            setSelected(false);
        }
        firePropertyChange("proxy", oldProxy, this.proxy);
    }

    /**
     * Return the value that becomes the <tt>ActionEvent</tt> source  before
     * the ActionEvent is passed along to the proxy Action.
     *
     * @return the value of the proxySource property.
     * @see #getProxy
     * @see #setProxySource
     * @see ActionEvent#getSource
     */
    public Object getProxySource() {
        return proxySource;
    }

    /**
     * Set the value that becomes the <tt>ActionEvent</tt> source before
     * the ActionEvent is passed along to the proxy Action.
     *
     * @param source the <tt>ActionEvent</tt> source/
     * @see #getProxy
     * @see #getProxySource
     * @see ActionEvent#setSource
     */
    public void setProxySource(Object source) {
        Object oldValue = this.proxySource;
        this.proxySource = source;
        firePropertyChange("proxySource", oldValue, this.proxySource);
    }

    private void maybePutDescriptionValue(String key, javax.swing.Action proxy) {
        Object s = proxy.getValue(key);
        if (s instanceof String) {
            putValue(key, s);
        }
    }

    private void updateProxyProperties() {
        javax.swing.Action proxy = getProxy();
        if (proxy != null) {
            setEnabled(proxy.isEnabled());
            Object s = proxy.getValue(SELECTED_KEY);
            setSelected((s instanceof Boolean) && (Boolean) s);
            maybePutDescriptionValue(javax.swing.Action.SHORT_DESCRIPTION, proxy);
            maybePutDescriptionValue(javax.swing.Action.LONG_DESCRIPTION, proxy);
        }
    }

    /* This PCL is added to the proxy action, i.e. getProxy().  We
     * track the following properties of the proxy action we're bound to:
     * enabled, selected, longDescription, shortDescription.  We only
     * mirror the description properties if they're non-null.
     */
    private class ProxyPCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            if ((propertyName == null) ||
                    "enabled".equals(propertyName) ||
                    "selected".equals(propertyName) ||
                    javax.swing.Action.SHORT_DESCRIPTION.equals(propertyName) ||
                    javax.swing.Action.LONG_DESCRIPTION.equals(propertyName)) {
                updateProxyProperties();
            }
        }
    }

    private String propertyMethodName(String prefix, String propertyName) {
        return prefix + StringUtil.ensureFirstCharacterUppercase(propertyName);
    }

    private Method propertyGetMethod(String propertyName) {
        Class<?> actionsClass = appAM.getActionsClass();
        // Check for getXxx..
        Method plainGet = ReflectionUtil.getMethod(
                actionsClass,
                propertyMethodName("get", propertyName) );
        if ( plainGet != null )
            return plainGet;

        return ReflectionUtil.getMethod(
                actionsClass,
                propertyMethodName("is", propertyName) );
    }

    private Method propertySetMethod(String propertyName, Class<?> type) {
        return ReflectionUtil.getMethod(
                appAM.getActionsClass(),
                propertyMethodName("set", propertyName) );
    }

    /**
     *
     * The name of this Action.  This string begins with the name of
     * the corresponding &#064;Action method (unless the <tt>name</tt>
     * &#064;Action parameter was specified).
     *
     * <p>
     * This name is used as a prefix to look up action resources,
     * and the ApplicationContext Framework uses it as the key for this
     * Action in ApplicationActionMaps.
     *
     * <p>
     * Note: this property should not confused with the {@link
     * javax.swing.Action#NAME Action.NAME} key.  That key is actually
     * used to initialize the <tt>text</tt> properties of Swing
     * components, which is why we call the corresponding
     * ApplicationAction resource "Action.text", as in:
     * <pre>
     * myCloseButton.Action.text = Close
     * </pre>
     *
     * @return the (read-only) value of the name property
     * @deprecated use {@link #getKey()}
     */
//    @Deprecated
//    @Override
//    public String getName() {
//        return getKey();
//    }

    /**
     *
     * Provides parameter values to &#064;Action methods.  By default, parameter
     * values are selected based exclusively on their type:
     * <table border=1>
     *   <tr>
     *     <th>Parameter Type</th>
     *     <th>Parameter Value</th>
     *   </tr>
     *   <tr>
     *     <td><tt>ActionEvent</tt></td>
     *     <td><tt>actionEvent</tt></td>
     *   </tr>
     *   <tr>
     *     <td><tt>javax.swing.Action</tt></td>
     *     <td>this <tt>ApplicationAction</tt> object</td>
     *   </tr>
     *   <tr>
     *     <td><tt>ActionMap</tt></td>
     *     <td>the <tt>ActionMap</tt> that contains this <tt>Action</tt></td>
     *   </tr>
     *   <tr>
     *     <td><tt>ResourceMap</tt></td>
     *     <td>the <tt>ResourceMap</tt> of the the <tt>ActionMap</tt> that contains this <tt>Action</tt></td>
     *   </tr>
     *   <tr>
     *     <td><tt>ApplicationContext</tt></td>
     *     <td>the value of <tt>ApplicationContext.getInstance()</tt></td>
     *   </tr>
     * </table>
     *
     * <p>
     * ApplicationAction subclasses may also select values based on
     * the value of the <tt>Action.Parameter</tt> annotation, which is
     * passed along as the <tt>pKey</tt> argument to this method:
     * <pre>
     * &#064;Action public void doAction(&#064;Action.Parameter("myKey") String myParameter) {
     *    // The value of myParameter is computed by:
     *    // getActionArgument(String.class, "myKey", actionEvent)
     * }
     * </pre>
     *
     * <p>
     * If <tt>pType</tt> and <tt>pKey</tt> aren't recognized, this method
     * calls {@link #actionFailed} with an IllegalArgumentException.
     *
     *
     * @param pType parameter type
     * @param pKey the value of the &#064;Action.Parameter annotation
     * @param actionEvent the ActionEvent that triggered this Action
     */
    protected Object getActionArgument(Class<?> pType, String pKey, ActionEvent actionEvent) {
        Object argument = null;
        if (pType == ActionEvent.class) {
            argument = actionEvent;
        } else if (pType == javax.swing.Action.class) {
            argument = this;
        } else if (pType == ActionMap.class) {
            argument = appAM;
        } else if (pType == ResourceMap.class) {
            argument = getResourceMap();
//        } else if (pType == ApplicationContext.class) {
//            argument = appAM.getContext();
//        } else if (pType == Application.class) {
//            argument = appAM.getContext().getApplication();
        } else {
            Exception e = new IllegalArgumentException("unrecognized @Action method parameter");
            actionFailed(e);
        }
        return argument;
    }

    private Task.InputBlocker createInputBlocker(Task<?, ?> task, ActionEvent event) {
        Object target = event.getSource();
        if (block == Task.BlockingScope.ACTION) {
            target = this;
        }
        return new DefaultInputBlocker(task, block, target, this);
    }

    private void noProxyActionPerformed(ActionEvent actionEvent) {
        Object taskObject = null;

        /* Create the arguments array for actionMethod by
         * calling getActionArgument() for each parameter.
         */
        Annotation[][] allPAnnotations = actionMethod.getParameterAnnotations();
        Class<?>[] pTypes = actionMethod.getParameterTypes();
        Object[] arguments = new Object[pTypes.length];
        for (int i = 0; i < pTypes.length; i++) {
            String pKey = null;
            for (Annotation pAnnotation : allPAnnotations[i]) {
                if (pAnnotation instanceof Action.Parameter) {
                    pKey = ((Action.Parameter) pAnnotation).value();
                    break;
                }
            }
            arguments[i] = getActionArgument(pTypes[i], pKey, actionEvent);
        }

        /* Call target.actionMethod(arguments).  If the return value
         * is a Task, then execute it.
         */
        try {
            Object target = appAM.getActionsObject();
            taskObject = actionMethod.invoke(target, arguments);
        } catch (Exception e) {
            actionFailed(e);
        }

        if (taskObject instanceof Task<?,?>) {
            Task<?, ?> task = (Task<?, ?>) taskObject;
            if (task.getInputBlocker() == null) {
                task.setInputBlocker(createInputBlocker(task, actionEvent));
            }
//            final ApplicationContext ctx = appAM.getContext();
            // micbinz fixme
            final TaskService ts = null; //ctx.getTaskService(taskService);
            if (ts != null) {
                ts.execute(task);
            } else {
                actionFailed(new IllegalArgumentException("Task Service ["+taskService+"] does not exist."));
            }
        }
    }

    /**
     * This method implements this <tt>Action's</tt> behavior.
     * <p>
     * If there's a proxy Action then call its actionPerformed
     * method.  Otherwise, call the &#064;Action method with parameter
     * values provided by {@code getActionArgument()}.  If anything goes wrong
     * call {@code actionFailed()}.
     *
     * @param actionEvent @{inheritDoc}
     * @see #setProxy
     * @see #getActionArgument
     * @see Task
     */
    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        javax.swing.Action proxy = getProxy();
        if (proxy != null) {
            actionEvent.setSource(getProxySource());
            proxy.actionPerformed(actionEvent);
        } else if (actionMethod != null) {
            noProxyActionPerformed(actionEvent);
        }
    }

    /**
     * If the proxy action is null and {@code enabledProperty} was
     * specified, then return the value of the enabled property's
     * is/get method applied to our ApplicationActionMap's
     * {@code actionsObject}.
     * Otherwise return the value of this Action's enabled property.
     *
     * @return {@inheritDoc}
     * @see #setProxy
     * @see #setEnabled
     * @see ApplicationActionMap#getActionsObject
     */
    @Override
    public boolean isEnabled() {
        if ((getProxy() != null) || (isEnabledMethod == null)) {
            return super.isEnabled();
        } else {
            try {
                boolean b = (Boolean) isEnabledMethod.invoke(appAM.getActionsObject());
                return enabledNegated^b;
            } catch (Exception e) {
                throw newInvokeError(isEnabledMethod, e);
            }
        }
    }

    /**
     * If the proxy action is null and {@code enabledProperty} was
     * specified, then set the value of the enabled property by
     * invoking the corresponding {@code set} method on our
     * ApplicationActionMap's {@code actionsObject}.
     * Otherwise set the value of this Action's enabled property.
     *
     * @param enabled {@inheritDoc}
     * @see #setProxy
     * @see #isEnabled
     * @see ApplicationActionMap#getActionsObject
     */
    @Override
    public void setEnabled(boolean enabled) {
        if ((getProxy() != null) || (setEnabledMethod == null)) {
            super.setEnabled(enabled);
        } else {
            try {
                setEnabledMethod.invoke(appAM.getActionsObject(), enabledNegated^enabled);
            } catch (Exception e) {
                throw newInvokeError(setEnabledMethod, e, enabled);
            }
        }
    }

    /**
     * If the proxy action is null and {@code selectedProperty} was
     * specified, then return the value of the selected property's
     * is/get method applied to our ApplicationActionMap's {@code actionsObject}.
     * Otherwise return the value of this Action's enabled property.
     *
     * @return true if this Action's JToggleButton is selected
     * @see #setProxy
     * @see #setSelected
     * @see ApplicationActionMap#getActionsObject
     */
    @Override
    public boolean isSelected() {
        if ((getProxy() != null) || (isSelectedMethod == null)) {
            Object v = getValue(SELECTED_KEY);
            return (v instanceof Boolean) && (Boolean) v;
        } else {
            return invokeBooleanMethod(appAM.getActionsObject(), isSelectedMethod);
        }
    }

    private Boolean invokeBooleanMethod(Object obj, Method method) {
        try {
            Object b = method.invoke(obj);
            return (Boolean) b;
        } catch (Exception e) {
            throw newInvokeError(method, e);
        }
    }

    /**
     * If the proxy action is null and {@code selectedProperty} was
     * specified, then set the value of the selected property by
     * invoking the corresponding {@code set} method on our
     * ApplicationActionMap's {@code actionsObject}.
     * Otherwise set the value of this Action's selected property.
     *
     * @param selected this Action's JToggleButton's value
     * @see #setProxy
     * @see #isSelected
     * @see ApplicationActionMap#getActionsObject
     */
    @Override
    public void setSelected(boolean selected) {
        if ((getProxy() != null) || (setSelectedMethod == null)) {
            super.putValue(SELECTED_KEY, Boolean.valueOf(selected));
        } else {
            try {
                super.putValue(SELECTED_KEY, Boolean.valueOf(selected));
                if (selected != isSelected()) {
                    setSelectedMethod.invoke(appAM.getActionsObject(), selected);
                }
            } catch (Exception e) {
                throw newInvokeError(setSelectedMethod, e, selected);
            }
        }
    }

    /**
     * Keeps the {@code @Action selectedProperty} in sync when
     * the value of {@code key} is {@code Action.SELECTED_KEY}.
     *
     * @param key {@inheritDoc}
     * @param value {@inheritDoc}
     */
    @Override
    public void putValue(String key, Object value) {
        if (SELECTED_KEY.equals(key) && (value instanceof Boolean)) {
            setSelected((Boolean) value);
        } else {
            super.putValue(key, value);
        }
    }

    /* Throw an Error because invoking Method m on the actionsObject,
     * with the specified arguments, failed.
     */
    private Error newInvokeError(Method m, Exception e, Object... args) {
        StringBuilder argsString = new StringBuilder((args.length == 0) ? "" : args[0].toString());
        for (int i = 1; i < args.length; i++) {
            argsString.append(", ").append(args[i].toString());
        }
        String actionsClassName = appAM.getActionsObject().getClass().getName();
        String msg = String.format("%s.%s(%s) failed", actionsClassName, m, argsString.toString());
        return new Error(msg, e);
    }

    /* Forward the @Action class's PropertyChangeEvent e to this
     * Action's PropertyChangeListeners using actionPropertyName instead
     * the original @Action class's property name.  This method is used
     * by ApplicationActionMap#ActionsPCL to forward @Action
     * enabledProperty and selectedProperty changes.
     */
    void forwardPropertyChangeEvent(PropertyChangeEvent e, String actionPropertyName) {
        if ("selected".equals(actionPropertyName) && (e.getNewValue() instanceof Boolean)) {
            putValue(SELECTED_KEY, e.getNewValue());
        }
        firePropertyChange(actionPropertyName, e.getOldValue(), e.getNewValue());
    }

    /* Log enough output for a developer to figure out
     * what went wrong.
     */
    private void actionFailed(Exception e) {
        // TBD Log an error
        // e.printStackTrace();
        throw new Error(e);
    }

    /**
     * Returns a string representation of this
     * <tt>ApplicationAction</tt> that should be useful for debugging.
     * If the action is enabled it's name is enclosed by parentheses;
     * if it's selected then a "+" appears after the name.  If the
     * action will appear with a text label, then that's included too.
     * If the action has a proxy, then we append the string for
     * the proxy action.
     *
     * @return A string representation of this ApplicationAction
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName());
        sb.append(" ");
        boolean enabled = isEnabled();
        if (!enabled) {
            sb.append("(");
        }
        sb.append(getKey());
        Object selectedValue = getValue(SELECTED_KEY);
        if (selectedValue instanceof Boolean) {
            if ((Boolean) selectedValue) {
                sb.append("+");
            }
        }
        if (!enabled) {
            sb.append(")");
        }
        Object nameValue = getValue(javax.swing.Action.NAME); // [getName()].Action.text
        if (nameValue instanceof String) {
            sb.append(" \"");
            sb.append((String) nameValue);
            sb.append("\"");
        }
        proxy = getProxy();
        if (proxy != null) {
            sb.append(" Proxy for: ");
            sb.append(proxy.toString());
        }
        return sb.toString();
    }
}
