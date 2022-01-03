
/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package org.jdesktop.application;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.ActionMap;

/**
 * An {@link javax.swing.ActionMap ActionMap} class where each entry
 * corresponds to an <tt>&#064;Action</tt> method from a single
 * <tt>actionsClass</tt> (i.e. a class that contains one or more
 * <tt>&#064;Actions</tt>).  Each entry's key is the <tt>&#064;Action's</tt>
 * name (the method name by default), and the value is an
 * {@link ApplicationAction} that calls the <tt>&#064;Actions</tt> method.
 * For example, the code below prints <tt>"Hello World"</tt>:
 * <pre>
 * public class HelloWorldActions {
 *     public &#064;Action void Hello() { System.out.print("Hello "); }
 *     public &#064;Action void World() { System.out.println("World"); }
 * }
 * // ...
 * ApplicationActionMap appAM = new ApplicationActionMap(SimpleActions.class);
 * ActionEvent e = new ActionEvent("no src", ActionEvent.ACTION_PERFORMED, "no cmd");
 * appAM.get("Hello").actionPerformed(e);
 * appAM.get("World").actionPerformed(e);
 * </pre>
 *
 * <p>
 * If a <tt>ResourceMap</tt> is provided then each
 * <tt>ApplicationAction's</tt> ({@link javax.swing.Action#putValue
 * putValue}, {@link javax.swing.Action#getValue getValue}) properties
 * are initialized from the ResourceMap.
 *
 * @see ApplicationAction
 * @see ResourceMap
 * @author Hans Muller (Hans.Muller@Sun.COM)
 */
@SuppressWarnings("serial")
public class ApplicationActionMap extends ActionMap {

    private final Class<?> actionsClass;
    private final Object actionsObject;
    private final List<ApplicationAction> proxyActions;

    /**
     * Creates {@code ApplicationActionMap} object.
     * <p>
     * The created action map will contain actions which are defined in the {@code actionsObject} and all
     * its ancestors up to the {@code actionsClass}. If {@code actionsClass} is a type of the {@code actionsObject} then
     * actions only from this class will be added to the map.
     *
     * @param context the Application context
     * @param actionsClass a super class for the {@code actionsObject}. Actions will be retrieved starting from this class.
     * @param actionsObject the object to be scanned for the actions.
     * @param resourceMap the {@code ResourceMap} to be used for those actions
     */
    public ApplicationActionMap(Class<?> actionsClass, Object actionsObject, ResourceMap resourceMap) {
        if (actionsClass == null) {
            throw new IllegalArgumentException("null actionsClass");
        }
        if (actionsObject == null) {
            throw new IllegalArgumentException("null actionsObject");
        }
        if (!(actionsClass.isInstance(actionsObject))) {
            throw new IllegalArgumentException("actionsObject not an instanceof actionsClass");
        }
        this.actionsClass = actionsClass;
        this.actionsObject = actionsObject;
        this.proxyActions = new ArrayList<ApplicationAction>();
        addAnnotationActions(resourceMap);
        maybeAddActionsPCL();
    }

    /**
     * Returns the base class for actions retrieval
     * @return the base class for actions retrieval
     */
    public final Class<?> getActionsClass() {
        return actionsClass;
    }

    /**
     * Returns the object with actions
     * @return the object with actions
     */
    public final Object getActionsObject() {
        return actionsObject;
    }

    /**
     * All of the {@code @ProxyActions} recursively defined by this
     * {@code ApplicationActionMap} and its parent ancestors.
     * <p>
     * Returns a read-only list of the {@code @ProxyActions} defined
     * by this {@code ApplicationActionMap's} {@code actionClass}
     * and, recursively, by this {@code ApplicationActionMap's} parent.
     * If there are no proxyActions, an empty list is returned.
     *
     * @return a list of all the proxyActions for this {@code ApplicationActionMap}
     */
    public List<ApplicationAction> getProxyActions() {
        // TBD: proxyActions that shadow should be merged
        ArrayList<ApplicationAction> allProxyActions = new ArrayList<ApplicationAction>(proxyActions);
        ActionMap parent = getParent();
        while (parent != null) {
            if (parent instanceof ApplicationActionMap) {
                allProxyActions.addAll(((ApplicationActionMap) parent).proxyActions);
            }
            parent = parent.getParent();
        }
        return Collections.unmodifiableList(allProxyActions);
    }

    private String aString(String s, String emptyValue) {
        return (s.length() == 0) ? emptyValue : s;
    }

    private void putAction(String key, ApplicationAction action) {
        if (get(key) != null) {
            // TBD log a warning - two actions with the same key
        }
        put(key, action);
    }


    /* Add Actions for each actionsClass method with an @Action
     * annotation and for the class's @ProxyActions annotation
     */
    private void addAnnotationActions(ResourceMap resourceMap) {
        final Class<?> actionsClass = getActionsClass();
        // @Action
        for (Method m : actionsClass.getDeclaredMethods()) {
            Action action = m.getAnnotation(Action.class);
            if (action != null) {
                final String methodName = m.getName();
                final String enabledProperty = aString(action.enabledProperty(), null);
                final String disabledProperty = aString(action.disabledProperty(), null);
                final String selectedProperty = aString(action.selectedProperty(), null);
                final String actionName = aString(action.name(), methodName);
                final String taskService = aString(action.taskService(), TaskService.DEFAULT_NAME);
                final Task.BlockingScope block = action.block();

                if(enabledProperty != null && disabledProperty != null)
                    throw new IllegalArgumentException("Action annotation contains both enabled and disabled attributes.");

                boolean inverted = disabledProperty != null;

                ApplicationAction appAction =
                        new ApplicationAction(this, resourceMap, actionName, m, inverted?disabledProperty:enabledProperty,
                        inverted, selectedProperty, taskService, block);
                putAction(actionName, appAction);
            }
        }
        // @ProxyActions
        ProxyActions proxyActionsAnnotation = actionsClass.getAnnotation(ProxyActions.class);
        if (proxyActionsAnnotation != null) {
            for (String actionName : proxyActionsAnnotation.value()) {
                ApplicationAction appAction = new ApplicationAction(this, resourceMap, actionName);
                appAction.setEnabled(false); // will track the enabled property of the Action it's bound to
                putAction(actionName, appAction);
                proxyActions.add(appAction);
            }
        }
    }

    /**
     * If any of the ApplicationActions need to track an
     * enabled or selected property defined in actionsClass, then add our
     * PropertyChangeListener.  If none of the @Actions in actionClass
     * provide an enabledProperty or selectedProperty argument, then
     * we don't need to do this.
     */
    private void maybeAddActionsPCL() {
        boolean needsPCL = false;
        Object[] keys = keys();
        if (keys != null) {
            for (Object key : keys) {
                javax.swing.Action value = get(key);
                if (value instanceof ApplicationAction) {
                    ApplicationAction actionAdapter = (ApplicationAction) value;
                    if ((actionAdapter.getEnabledProperty() != null) ||
                            (actionAdapter.getSelectedProperty() != null)) {
                        needsPCL = true;
                        break;
                    }
                }
            }
            if (needsPCL) {
                try {
                    final Class<?> actionsClass = getActionsClass();
                    final Method m = actionsClass.getMethod("addPropertyChangeListener", PropertyChangeListener.class);
                    m.invoke(getActionsObject(), new ActionsPCL());
                } catch (Exception e) {
                    final String s = "addPropertyChangeListener undefined " + actionsClass;
                    throw new Error(s, e);
                }
            }
        }
    }

    /* When the value of an actionsClass @Action enabledProperty or
     * selectedProperty changes, forward the PropertyChangeEvent to
     * the ApplicationAction object itself.
     */
    private class ActionsPCL implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            String propertyName = event.getPropertyName();
            Object[] keys = keys();
            if (keys != null) {
                for (Object key : keys) {
                    javax.swing.Action value = get(key);
                    if (value instanceof ApplicationAction) {
                        ApplicationAction appAction = (ApplicationAction) value;
                        if (propertyName.equals(appAction.getEnabledProperty())) {
                            appAction.forwardPropertyChangeEvent(event, "enabled");
                        } else if (propertyName.equals(appAction.getSelectedProperty())) {
                            appAction.forwardPropertyChangeEvent(event, "selected");
                        }
                    }
                }
            }
        }
    }
}
