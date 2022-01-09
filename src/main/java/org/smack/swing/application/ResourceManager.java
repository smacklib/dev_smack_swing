/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2001-2022 Michael Binz
 */
package org.smack.swing.application;

import java.awt.Component;
import java.awt.Container;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.logging.Logger;

import javax.swing.AbstractButton;
import javax.swing.JLabel;
import javax.swing.JMenu;

import org.smack.util.ServiceManager;
import org.smack.util.resource.ResourceMap;

/**
 * The application's {@code ResourceManager} provides
 * read-only cached access to resources in {@code ResourceBundles} via the
 * {@link org.smack.util.resource.ResourceMap ResourceMap} class.  {@code ResourceManager} is a
 * property of the {@code ApplicationContext} and most applications
 * look up resources relative to it, like this:
 * <pre>
 * ResourceManager appResourceManager = Application.getResourceManager();
 * ResourceMap resourceMap = appResourceManager.getResourceMap(MyClass.class);
 * String msg = resourceMap.getString("msg");
 * Icon icon = resourceMap.getIcon("icon");
 * Color color = resourceMap.getColor("color");
 * </pre>
 * The {@code ResourceMap}
 * in this example contains resources from the ResourceBundle named
 * {@code MyClass}, and the rest of the
 * chain contains resources shared by the entire application.
 * <p>
 * The {@link Application} class itself may also provide resources. A complete
 * description of the naming conventions for ResourceBundles is provided
 * by the {@link #getResourceMap(Class, Class) getResourceMap()} method.
 * </p>
 * <p>
 * A stand alone {@link ResourceManager} can be created by the public
 * constructors.
 * <P>
 * @see ApplicationContext#getResourceManager
 * @see ApplicationContext#getResourceMap
 * @see ResourceMap
 *
 * @author Michael Binz
 * @author Hans Muller (Hans.Muller@Sun.COM)
 */
public final class ResourceManager
{
    private final static Logger LOG =
            Logger.getLogger( ResourceManager.class.getName() );

    private final org.smack.util.resource.ResourceManager _smackRm;

    /**
     * Creates an instance.
     */
    public ResourceManager()
    {
        this( ResourceManager.class );
    }

    /**
     * Construct a {@code ResourceManager}.  Typically applications
     * will not create a ResourceManager directly, they'll retrieve
     * the shared one from the {@code ApplicationContext} with:
     * <pre>
     * Application.getInstance().getContext().getResourceManager()
     * </pre>
     * Or just look up {@code ResourceMaps} with the ApplicationContext
     * convenience method:
     * <pre>
     * Application.getInstance().getContext().getResourceMap(MyClass.class)
     * </pre>
     * <p>This constructor is used if the resource system is to be used
     * independently from the rest of the jsp192 API, especially if no
     * {@link Application} class is created, for example in a command-line-
     * based application.
     *
     * @param applicationClass The application class.  Note that this is
     * not needed to inherit from {@link Application}.  It is used as the
     * parent of the returned resource maps, which allows to define
     * application wide resources in the application class' resources.
     *
     * @see ApplicationContext#getResourceManager
     * @see ApplicationContext#getResourceMap
     */
    public ResourceManager( Class<?> notUsed ) {
        _smackRm = ServiceManager.getApplicationService( org.smack.util.resource.ResourceManager.class );
    }

    /**
     * Return the ResourceMap chain for the specified class. This is
     * just a convenience method, it's the same as:
     * <code>getResourceMap(cls, cls)</code>.
     *
     * @param cls the class that defines the location of ResourceBundles
     * @return a {@code ResourceMap} that contains resources loaded from
     *   {@code ResourceBundles}  found in the resources subpackage of the
     *   specified class's package.
     * @see #getResourceMap(Class, Class)
     */
    public final ResourceMap getResourceMap( Class<?> cls )
    {
        return _smackRm.getResourceMap2( cls );
    }

    /**
     * Performs injection of attributes marked with the resource annotation.
     *
     * @param o The object whose resources should be injected. Null is not
     * allowed, array instances are not allowed, primitive classes are not
     * allowed.
     * @throws IllegalArgumentException In case a bad object was passed.
     */
    public void injectResources( Object o )
    {
        _smackRm.injectResources( o );
    }

    /**
     *
     * @param component
     * @param pd
     * @param key
     */
    private void injectComponentProperty(Component component, PropertyDescriptor pd, String key, ResourceMap map ) {
        Method setter = pd.getWriteMethod();
        Class<?> type = pd.getPropertyType();
        if ((setter != null) && (type != null) && map.containsKey(key)) {
            Object value = map.getAs(key, type,null);
            String propertyName = pd.getName();
            try {
                // Note: this could be generalized, we could delegate
                // to a component property injector.
                if ("text".equals(propertyName) && (component instanceof AbstractButton)) {
                    MnemonicText.configure(component, (String) value);
                } else if ("text".equals(propertyName) && (component instanceof JLabel)) {
                    MnemonicText.configure(component, (String) value);
                } else {
                    setter.invoke(component, value);
                }
            } catch (Exception e) {
                String pdn = pd.getName();
                String msg = "property setter failed";
                throw new RuntimeException(msg, e);  // key, component, pdn);
            }
        } else if (type != null) {
            String pdn = pd.getName();
            String msg = "no value specified for resource";
            throw new RuntimeException(msg);//, key, component, pdn);
        } else if (setter == null) {
            String pdn = pd.getName();
            String msg = "can't set read-only property";
            throw new RuntimeException(msg);//, key, component, pdn);
        }
    }

    /**
     *
     * @param componentName
     * @param component
     */
    private void injectComponentProperties(String componentName, Component component, ResourceMap map) {
        if ( componentName == null )
            return;

        /* Optimization: punt early if componentName doesn't
         * appear in any componentName.propertyName resource keys
         */
        boolean matchingResourceFound = false;
        for (String key : map.keySet()) {
            int i = key.lastIndexOf(".");
            if ((i != -1) && componentName.equals(key.substring(0, i))) {
                matchingResourceFound = true;
                break;
            }
        }
        if (!matchingResourceFound) {
            return;
        }
        BeanInfo beanInfo;
        try {
            beanInfo = Introspector.getBeanInfo(component.getClass());
        } catch (IntrospectionException e) {
            String msg = "introspection failed";
            throw new RuntimeException(msg, e); //null, component, null);
        }
        PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
        if ((pds != null) && (pds.length > 0)) {
            for (String key : map.keySet()) {
                int i = key.lastIndexOf(".");
                String keyComponentName = (i == -1) ? null : key.substring(0, i);
                if (componentName.equals(keyComponentName)) {
                    if ((i + 1) == key.length()) {
                        /* key has no property name suffix, e.g. "myComponentName."
                         * This is probably a mistake.
                         */
                        String msg = "component resource lacks property name suffix";
                        LOG.warning(msg);
                        break;
                    }
                    String propertyName = key.substring(i + 1);
                    boolean matchingPropertyFound = false;
                    for (PropertyDescriptor pd : pds) {
                        if (pd.getName().equals(propertyName)) {
                            injectComponentProperty(component, pd, key,map);
                            matchingPropertyFound = true;
                            break;
                        }
                    }
                    if (!matchingPropertyFound) {
                        String msg = String.format(
                                "[resource %s] component named %s doesn't have a property named %s",
                                key, componentName, propertyName);
                        LOG.warning(msg);
                    }
                }
            }
        }
    }

    /* Applies {@link #injectComponent} to each Component in the
     * hierarchy with root <tt>root</tt>.
     *
     * @param root the root of the component hierarchy
     * @throws PropertyInjectionException if a property specified by a resource can't be set
     * @throws IllegalArgumentException if target is null
     * @see #injectComponent
     */
    public void injectComponents(Component root, ResourceMap map) {
        injectComponent(root,map);
        if (root instanceof JMenu) {
            /* Warning: we're bypassing the popupMenu here because
             * JMenu#getPopupMenu creates it; doesn't seem right
             * to do so at injection time.  Unfortunately, this
             * means that attempts to inject the popup menu's
             * "label" property will fail.
             */
            JMenu menu = (JMenu) root;
            for (Component child : menu.getMenuComponents()) {
                injectComponents(child,map);
            }
        } else if (root instanceof Container) {
            Container container = (Container) root;
            for (Component child : container.getComponents()) {
                injectComponents(child,map);
            }
        }
    }

    /**
     * Set each property in <tt>target</tt> to the value of
     * the resource named <tt><i>componentName</i>.propertyName</tt>,
     * where  <tt><i>componentName</i></tt> is the value of the
     * target component's name property, i.e. the value of
     * <tt>target.getName()</tt>.  The type of the resource must
     * match the type of the corresponding property.  Properties
     * that aren't defined by a resource aren't set.
     * <p>
     * For example, given a button configured like this:
     * <pre>
     * myButton = new JButton();
     * myButton.setName("myButton");
     * </pre>
     * And a ResourceBundle properties file with the following
     * resources:
     * <pre>
     * myButton.text = Hello World
     * myButton.foreground = 0, 0, 0
     * myButton.preferredSize = 256, 256
     * </pre>
     * Then <tt>injectComponent(myButton)</tt> would initialize
     * myButton's text, foreground, and preferredSize properties
     * to <tt>Hello World</tt>, <tt>new Color(0,0,0)</tt>, and
     * <tt>new Dimension(256,256)</tt> respectively.
     * <p>
     * This method calls {@link #getObject} to look up resources
     * and it uses {@link Introspector#getBeanInfo} to find
     * the target component's properties.
     * <p>
     * If target is null an IllegalArgumentException is thrown.  If a
     * resource is found that matches the target component's name but
     * the corresponding property can't be set, an (unchecked) {@link
     * PropertyInjectionException} is thrown.
     *
     * @param target the Component to inject
     * @see #injectComponents
     * @see #getObject
     * @see ResourceConverter#forType
     * @throws PropertyInjectionException if a property specified by a resource can't be set
     * @throws IllegalArgumentException if target is null
     */
    public void injectComponent(Component target, ResourceMap map) {
        if (target == null) {
            throw new IllegalArgumentException("null target");
        }
        injectComponentProperties( target.getName(), target, map );
    }
    /**
     * Inject the passed bean's properties from this map. The prefix is
     * used to find the configuration keys in the map. Keys in the
     * map have to look like prefix.propertyName. The dot is added to
     * the prefix.
     *
     * @param bean The bean whose properties are injected.
     * @param prefix The prefix used to filter the map's keys.
     */
    public void injectProperties( Object bean, String prefix, ResourceMap map )
    {
        _smackRm.injectProperties( bean, prefix, map );
    }
}
