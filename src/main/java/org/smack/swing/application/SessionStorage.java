/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2001-2022 Michael Binz
 */
package org.smack.swing.application;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;

import org.smack.application.LocalStorage;
import org.smack.swing.application.session.PropertySupport;
import org.smack.swing.application.session.SplitPaneProperty;
import org.smack.swing.application.session.TabbedPaneProperty;
import org.smack.swing.application.session.TableProperty;
import org.smack.swing.application.session.WindowProperty;
import org.smack.util.ServiceManager;

/**
 * Support for storing GUI state that persists between Application sessions.
 * <p>
 * This class simplifies the common task of saving a little bit of an
 * application's GUI "session" state when the application shuts down,
 * and then restoring that state when the application is restarted.
 * Session state is stored on a per component basis, and only for
 * components with a {@link java.awt.Component#getName name} and for
 * which a {@code PropertySupport} object has been defined and registeres.
 * SessionState Properties that preserve the {@code bounds} {@code Rectangle}
 * for Windows, the {@code dividerLocation} for {@code JSliderPanes} and the
 * {@code selectedIndex} for {@code JTabbedPanes} are defined by default.  The
 * {@code ApplicationContext} {@link
 * ApplicationContext#getSessionStorage getSessionStorage} method
 * provides a shared {@code SessionStorage} object.
 * <p>
 * A typical Application saves session state in its
 * {@link Application#shutdown shutdown()} method, and then restores
 * session state in {@link Application#startup startup()}:
 * <pre>
 * public class MyApplication extends Application {
 *     &#064;Override protected void shutdown() {
 *         getContext().getSessionStorage().<b>save</b>(mainFrame, "session.xml");
 *     }
 *     &#064;Override protected void startup() {
 *         ApplicationContext appContext = getContext();
 *         appContext.setVendorId("Sun");
 *         appContext.setApplicationId("SessionStorage1");
 *         // ... create the GUI rooted by JFrame mainFrame
 *         appContext.getSessionStorage().<b>restore</b>(mainFrame, "session.xml");
 *     }
 *     // ...
 * }
 * </pre>
 * In this example, the bounds of {@code mainFrame} as well the
 * session state for any of its {@code JSliderPane} or {@code
 * JTabbedPane} will be saved when the application shuts down, and
 * restored when the applications starts up again.  Note: error
 * handling has been omitted from the example.
 * <p>
 * Session state is stored locally, relative to the user's
 * home directory, by the {@code LocalStorage}
 * {@link LocalStorage#save save} and {@link LocalStorage#save load}
 * methods.  The {@code startup} method must set the
 * {@code ApplicationContext} {@code vendorId} and {@code applicationId}
 * properties to ensure that the correct
 * {@link LocalStorage#getDirectory local directory} is selected on
 * all platforms.  For example, on Windows XP, the full pathname
 * for filename {@code "session.xml"} is typically:
 * <pre>
 * ${userHome}\Application Data\${vendorId}\${applicationId}\session.xml
 * </pre>
 * Where the value of {@code ${userHome}} is the the value of
 * the Java System property  {@code "user.home"}.  On Solaris or
 * Linux the file is:
 * <pre>
 * ${userHome}/.${applicationId}/session.xml
 * </pre>
 * and on OSX:
 * <pre>
 * ${userHome}/Library/Application Support/${applicationId}/session.xml
 * </pre>
 *
 * @see ApplicationContext#getSessionStorage
 * @see LocalStorage
 *
 * @author Michael Binz
 * @author Hans Muller
 */
public final class SessionStorage
{
    private static final Logger LOG = Logger.getLogger(SessionStorage.class.getName());

    private final Map<Class<?>, PropertySupport> _propertyMap =
            new HashMap<Class<?>, PropertySupport>();

    /**
     * Constructs a SessionStorage object.  The following {@link
     * PropertySupport PropertySupport} objects are registered by default:
     * <p>
     * <table border="1" cellpadding="4%">
     *     <tr>
     *       <th>Base Component Type</th>
     *       <th>PropertySupport</th>
     *       <th>PropertySupport Value</th>
     *     </tr>
     *     <tr>
     *       <td>Window</td>
     *       <td>WindowProperty</td>
     *       <td>WindowState</td>
     *     </tr>
     *     <tr>
     *       <td>JTabbedPane</td>
     *       <td>TabbedPaneProperty</td>
     *       <td>TabbedPaneState</td>
     *     </tr>
     *     <tr>
     *       <td>JSplitPane</td>
     *       <td>SplitPaneProperty</td>
     *       <td>SplitPaneState</td>
     *     </tr>
     *     <tr>
     *       <td>JTable</td>
     *       <td>TableProperty</td>
     *       <td>TableState</td>
     *     </tr>
     * </table>
     * <p>
     * Applications typically would not create a {@code SessionStorage}
     * object directly, they'd use the shared ApplicationContext value:
     * <pre>
     * ApplicationContext ctx = Application.getInstance(MyApplication.class).getContext();
     * SessionStorage ss = ctx.getSesssionStorage();
     * </pre>
     *
     *
     * @param a
     * @see ApplicationContext#getSessionStorage
     * @see #getProperty(Class)
     * @see #getProperty(Component)
     */
    public SessionStorage() {
        _propertyMap.put(Window.class, new WindowProperty());
        _propertyMap.put(JTabbedPane.class, new TabbedPaneProperty());
        _propertyMap.put(JSplitPane.class, new SplitPaneProperty());
        _propertyMap.put(JTable.class, new TableProperty());
    }

    private void checkSaveRestoreArgs(Component root, String fileName) {
        if (root == null) {
            throw new IllegalArgumentException("null root");
        }
        if (fileName == null) {
            throw new IllegalArgumentException("null fileName");
        }
    }

    /* At some point we may replace this with a more complex scheme.
     */
    private String getComponentName(Component c) {
        return c.getName();
    }

    /* Returns a string that uniquely identifies this component, or null
     * if Component c doesn't have a name per getComponentName().  The
     * pathname is basically the name of all of the components, starting
     * with c, separated by "/".  This path is the reverse of what's
     * typical, the first path element is c's name, rather than the name
     * of c's root Window or Applet.  That way pathnames can be
     * distinguished without comparing much of the string.  The names
     * of intermediate components *can* be null, we substitute
     * "[type][z-order]" for the name.  Here's an example:
     *
     * JFrame myFrame = new JFrame();
     * JPanel p = new JPanel() {};  // anonymous JPanel subclass
     * JButton myButton = new JButton();
     * myButton.setName("myButton");
     * p.add(myButton);
     * myFrame.add(p);
     *
     * getComponentPathname(myButton) =>
     * "myButton/AnonymousJPanel0/null.contentPane/null.layeredPane/JRootPane0/myFrame"
     *
     * Notes about name usage in AWT/Swing: JRootPane (inexplicably) assigns
     * names to it's children (layeredPane, contentPane, glassPane);
     * all AWT components lazily compute a name.  If we hadn't assigned the
     * JFrame a name, it's name would have been "frame0".
     */
    private String getComponentPathname(Component c)
    {
        String name = getComponentName(c);

        if (name == null) {
            return null;
        }

        StringBuilder path = new StringBuilder(name);

        while (
                (c.getParent() != null) &&
                !(c instanceof Window) )
        {
            c = c.getParent();
            name = getComponentName(c);
            if (name == null) {
                int n = c.getParent().getComponentZOrder(c);
                if (n >= 0) {
                    Class<?> cls = c.getClass();
                    name = cls.getSimpleName();
                    if (name.length() == 0) {
                        name = "Anonymous" + cls.getSuperclass().getSimpleName();
                    }
                    name = name + n;
                } else {
                    // Implies that the component tree is changing
                    // while we're computing the path. Punt.
                    LOG.warning("Couldn't compute pathname for " + c);
                    return null;
                }
            }
            path.append("/").append(name);
        }
        return path.toString();
    }

    /* Recursively walk the component tree, breadth first, storing the
     * state - PropertySupport.getSessionState() - of named components under
     * their pathname (the key) in stateMap.
     *
     * Note: the breadth first tree-walking code here should remain
     * structurally identical to restoreTree().
     */
    private void saveTree(List<Component> roots, Map<String, Object> stateMap) {
        List<Component> allChildren = new ArrayList<Component>();
        for (Component root : roots) {
            if (root != null) {
                PropertySupport p = getProperty(root);
                if (p != null) {
                    String pathname = getComponentPathname(root);
                    if (pathname != null) {
                        Object state = p.getSessionState(root);
                        if (state != null) {
                            stateMap.put(pathname, state);
                        }
                    }
                }
            }
            if (root instanceof Container) {
                Component[] children = ((Container) root).getComponents();
                if ((children != null) && (children.length > 0)) {
                    Collections.addAll(allChildren, children);
                }
            }
        }
        if (allChildren.size() > 0) {
            saveTree(allChildren, stateMap);
        }
    }

    /**
     * Saves the state of each named component in the specified hierarchy to
     * a file using {@link LocalStorage#save LocalStorage.save(fileName)}.
     * Each component is visited in breadth-first order: if a {@code PropertySupport}
     * {@link #getProperty(Component) exists} for that component,
     * and the component has a {@link java.awt.Component#getName name}, then
     * its {@link PropertySupport#getSessionState state} is saved.
     * <p>
     * Component names can be any string however they must be unique
     * relative to the name's of the component's siblings.  Most Swing
     * components do not have a name by default, however there are
     * some exceptions: JRootPane (inexplicably) assigns names to it's
     * children (layeredPane, contentPane, glassPane); and all AWT
     * components lazily compute a name, so JFrame, JDialog, and
     * JWindow also have a name by default.
     * <p>
     * The type of sessionState values (i.e. the type of values
     * returned by {@code PropertySupport.getSessionState}) must be one those
     * supported by {@link java.beans.XMLEncoder XMLEncoder} and
     * {@link java.beans.XMLDecoder XMLDecoder}, for example beans
     * (null constructor, read/write properties), primitives, and
     * Collections.  Java bean classes and their properties must be
     * public.  Typically beans defined for this purpose are little
     * more than a handful of simple properties.  The JDK 6
     * &#064;ConstructorProperties annotation can be used to eliminate
     * the need for writing set methods in such beans, e.g.
     * <pre>
     * public class FooBar {
     *     private String foo, bar;
     *     // Defines the mapping from constructor params to properties
     *     &#064;ConstructorProperties({"foo", "bar"})
     *     public FooBar(String foo, String bar) {
     *         this.foo = foo;
     *         this.bar = bar;
     *     }
     *     public String getFoo() { return foo; }  // don't need setFoo
     *     public String getBar() { return bar; }  // don't need setBar
     * }
     * </pre>
     *
     * @param root the root of the Component hierarchy to be saved.
     * @param fileName the {@code LocalStorage} filename.
     * @throws IOException
     * @see #restore
     * @see ApplicationContext#getLocalStorage
     * @see LocalStorage#save
     * @see #getProperty(Component)
     */
    public void save(Component root, String fileName) throws IOException {
        checkSaveRestoreArgs(root, fileName);
        Map<String, Object> stateMap = new HashMap<String, Object>();
        saveTree(Collections.singletonList(root), stateMap);
        LocalStorage lst = ServiceManager.getApplicationService( LocalStorage.class );
        lst.save(stateMap, fileName);
    }

    /* Recursively walk the component tree, breadth first, restoring the
     * state - PropertySupport.setSessionState() - of named components for which
     * there's a non-null entry under the component's pathName in
     * stateMap.
     *
     * Note: the breadth first tree-walking code here should remain
     * structurally identical to saveTree().
     */
    private void restoreTree(List<Component> roots, Map<String, Object> stateMap) {
        List<Component> allChildren = new ArrayList<Component>();
        for (Component root : roots) {
            if (root != null) {
                PropertySupport p = getProperty(root);
                if (p != null) {
                    String pathname = getComponentPathname(root);
                    if (pathname != null) {
                        Object state = stateMap.get(pathname);
                        if (state != null) {
                            p.setSessionState(root, state);
                        } else {
                            LOG.warning("No saved state for " + root);
                        }
                    }
                }
            }
            if (root instanceof Container) {
                Component[] children = ((Container) root).getComponents();
                if ((children != null) && (children.length > 0)) {
                    Collections.addAll(allChildren, children);
                }
            }
        }
        if (allChildren.size() > 0) {
            restoreTree(allChildren, stateMap);
        }
    }

    /**
     * Restores each named component in the specified hierarchy
     * from the session state loaded from
     * a file using {@link LocalStorage#save LocalStorage.load(fileName)}.
     * Each component is visited in breadth-first order: if a
     * {@link #getProperty(Component) PropertySupport} exists for that component,
     * and the component has a {@link java.awt.Component#getName name}, then
     * its state is {@link PropertySupport#setSessionState restored}.
     *
     * @param root the root of the Component hierarchy to be restored.
     * @param fileName the {@code LocalStorage} filename.
     * @throws IOException
     * @see #save
     * @see ApplicationContext#getLocalStorage
     * @see LocalStorage#save
     * @see #getProperty(Component)
     */
    @SuppressWarnings("unchecked")
    public void restore(Component root, String fileName) throws IOException {
        checkSaveRestoreArgs(root, fileName);
        LocalStorage lst = ServiceManager.getApplicationService(LocalStorage.class );
        Map<String, Object> stateMap = (Map<String, Object>) (lst.load(fileName));
        if (stateMap != null) {
            restoreTree(Collections.singletonList(root), stateMap);
        }
    }

    private void checkClassArg(Class<?> cls) {
        if (cls == null) {
            throw new IllegalArgumentException("null class");
        }
    }

    /**
     * Returns the {@code PropertySupport} object that was
     * {@link #putProperty registered} for the specified class
     * or a superclass.  If no PropertySupport has been registered,
     * return null.  To lookup the session state {@code PropertySupport}
     * for a {@code Component} use {@link #getProperty(Component)}.
     * <p>
     *
	 * @exception IllegalArgumentException if {@code cls} is null
     * @param cls the class to which the returned {@code PropertySupport} applies
     * @return the {@code PropertySupport} registered with {@code putProperty} for
     *   the specified class or the first one registered for a superclass
     *   of {@code cls}.
     * @see #getProperty(Component)
     * @see #putProperty
     * @see #save
     * @see #restore
     */
    public PropertySupport getProperty(Class<?> cls) {
        checkClassArg(cls);
        while (cls != null) {
            PropertySupport p = _propertyMap.get(cls);
            if (p != null) {
                return p;
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    /**
     * Register a {@code PropertySupport} for the specified class.
	 * <p>
	 * One can clear the {@code PropertySupport} for a class by setting the entry to null:
     * <pre>
     * sessionStorage.putProperty(myClass.class, null);
     * </pre>
	 *
	 * Register a custom {@code PropertySupport}:
	 * <pre>
     * ApplicationContext ctx = Application.getInstance(MyApplication.class).getContext();
     * SessionStorage ss = ctx.getSesssionStorage();
     * ctx.putProperty(JTable.class, new ExtendedTableProperty());
     * </pre>
	 *
     * @exception IllegalArgumentException if {@code cls} is null.
     * @param cls the class to which {@code propertySupport} applies.
     * @param propertySupport the {@code PropertySupport} object to register or null.
     * @see #getProperty(Component)
     * @see #getProperty(Class)
     * @see #save
     * @see #restore
     */
    public void putProperty(Class<?> cls, PropertySupport propertySupport) {
        checkClassArg(cls);

        // Remove property support for the clazz in case property argument is null
        if (propertySupport == null) {
            _propertyMap.remove(cls);
            return;
        }

        _propertyMap.put(cls, propertySupport);
    }

    /**
     * If a {@code sessionState PropertySupport} object exists for the
     * specified Component return it, otherwise return null.  This method
     * is used by the {@link #save save} and {@link #restore restore} methods
     * to lookup the  {@code sessionState PropertySupport} object for each component
     * to whose session state is to be saved or restored.
     * <p>
     * The {@code putProperty} method registers a PropertySupport object for
     * a class.  One can specify a PropertySupport object for a single Swing
     * component by setting the component's client property, like this:
     * <pre>
     * myJComponent.putClientProperty(PropertySupport.class, myPropertySupport);
     * </pre>
     * One can also create components that implement the
     * {@code PropertySupport} interface directly.
     *
     * @param component the component to retrive the {@code PropertySupport} from
     * @return if  {@code component} implements {@code PropertySupport}, then
     *     {@code component}, if {@code component} is a {@code JComponent} with a
     *     {@code PropertySupport} valued
     *     {@link javax.swing.JComponent#getClientProperty client property} under
     *     (client property key) {@code PropertySupport}, then
     *     return that, otherwise return the value of
     *     {@code getProperty(component.getClass())}.
     * <p>
     * @exception IllegalArgumentException if {@code Component component} is null.
     * @see javax.swing.JComponent#putClientProperty
     * @see #getProperty(Class)
     * @see #putProperty
     * @see #save
     * @see #restore
     */
    public final PropertySupport getProperty(Component component) {
        if (component == null) {
            throw new IllegalArgumentException("null component");
        }
        if (component instanceof PropertySupport) {
            return (PropertySupport) component;
        } else {
            PropertySupport p = null;
            if (component instanceof JComponent) {
                Object v = ((JComponent) component).getClientProperty(PropertySupport.class);
                p = (v instanceof PropertySupport) ? (PropertySupport) v : null;
            }
            return (p != null) ? p : getProperty(component.getClass());
        }
    }
}
