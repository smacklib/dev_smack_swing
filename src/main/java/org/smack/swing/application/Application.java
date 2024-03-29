/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2001-2023 Michael Binz
 */
package org.smack.swing.application;

import java.awt.ActiveEvent;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.PaintEvent;
import java.beans.Beans;
import java.util.Collections;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.smack.swing.beans.AbstractBeanEdt;
import org.smack.swing.swingx.SwingXUtilities;
import org.smack.util.ServiceManager;
import org.smack.util.StringUtil;
import org.smack.util.resource.ResourceManager.Resource;

/**
 * The base class for Swing applications.
 *
 * <p>
 * This class defines a simple lifecyle for Swing applications: {@code
 * initialize}, {@code startup}, {@code ready}, and {@code shutdown}.
 * The {@code Application's} {@code startup} method is responsible for
 * creating the initial GUI and making it visible, and the {@code
 * shutdown} method for hiding the GUI and performing any other
 * cleanup actions before the application exits.  The {@code initialize}
 * method can be used configure system properties that must be set
 * before the GUI is constructed and the {@code ready}
 * method is for applications that want to do a little bit of extra
 * work once the GUI is "ready" to use.  Concrete subclasses must
 * override the {@code startup} method.
 * <p>
 * Applications are started with the static {@code launch} method.
 * Applications use the {@code ApplicationContext} {@link
 * Application#getContext} to find resources,
 * actions, local storage, and so on.
 * <p>
 * All {@code Application} subclasses must override {@code startup}
 * and they should call {@link #exit} (which
 * calls {@code shutdown}) to exit.
 * Here's an example of a complete "Hello World" Application:
 * <pre>
 * public class MyApplication extends Application {
 *     JFrame mainFrame = null;
 *     &#064;Override protected void startup() {
 *         mainFrame = new JFrame("Hello World");
 *         mainFrame.add(new JLabel("Hello World"));
 *         mainFrame.addWindowListener(new MainFrameListener());
 *         mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
 *         mainFrame.pack();
 *         mainFrame.setVisible(true);
 *     }
 *     &#064;Override protected void shutdown() {
 *         mainFrame.setVisible(false);
 *     }
 *     private class MainFrameListener extends WindowAdapter {
 *         public void windowClosing(WindowEvent e) {
 *            exit();
 *         }
 *     }
 *     public static void main(String[] args) {
 *         Application.launch(MyApplication.class, args);
 *     }
 * }
 * </pre>
 * <p>
 * The {@code mainFrame's} {@code defaultCloseOperation} is set
 * to {@code DO_NOTHING_ON_CLOSE} because we're handling attempts
 * to close the window by calling
 * {@code ApplicationContext} {@link #exit}.
 * <p>
 * Simple single frame applications like the example can be defined
 * more easily with the {@link SingleFrameApplication
 * SingleFrameApplication} {@code Application} subclass.
 *
 * <p>
 * All of the Application's methods are called (must be called) on
 * the EDT.
 *
 * <p>
 * All but the most trivial applications should define a ResourceBundle
 * in the resources subpackage with the same name as the application class (like {@code
 * resources/MyApplication.properties}).  This ResourceBundle contains
 * resources shared by the entire application and should begin with the
 * following the standard Application resources:
 * <pre>
 * Application.name = A short name, typically just a few words
 * Application.id = Suitable for Application specific identifiers, like file names
 * Application.title = A title suitable for dialogs and frames
 * Application.version = A version string that can be incorporated into messages
 * Application.vendor = A proper name, like Sun Microsystems, Inc.
 * Application.vendorId = suitable for Application-vendor specific identifiers, like file names.
 * Application.homepage = A URL like http://www.javadesktop.org
 * Application.description =  One brief sentence
 * Application.lookAndFeel = either system, default, or a LookAndFeel class name
 * </pre>
 * <p>
 * The {@code Application.lookAndFeel} resource is used to initialize the
 * {@code UIManager lookAndFeel} as follows:
 * <ul>
 * <li>{@code system} - the system (native) look and feel</li>
 * <li>{@code default} - use the JVM default, typically the cross platform look and feel</li>
 * <li>{@code nimbus} - use the modern cross platform look and feel Nimbus
 * <li>a LookAndFeel class name - use the specified class
 * </ul>
 *
 * @see SingleFrameApplication
 * @see ApplicationContext
 * @see UIManager#setLookAndFeel
 * @author Michael Binz
 */
public abstract class Application extends AbstractBeanEdt
{
    private static final Logger LOG =
            Logger.getLogger( Application.class.getName() );

    private static final String DEFAULT_LOOK_AND_FEEL =
            "nimbus";

    private static Application application =
            null;
    private final List<ExitListener> _exitListeners =
            new CopyOnWriteArrayList<ExitListener>();
    private final ApplicationContext context;
    private boolean ready;

    /**
     * Subclasses can provide a no-args constructor to initialize private
     * final state however GUI initialization, and anything else that might
     * refer to public API, should be done in the {@link #startup startup}
     * method.
     */
    protected Application()
    {
        // Inject resource-defined fields on the application instance.
        getApplicationService(
                ResourceManager.class ).injectResources(
                        this );

        context = new ApplicationContext(this);
    }

    /**
     * Creates an instance of the specified {@code Application}
     * subclass, sets the {@code ApplicationContext} {@code
     * application} property, and then calls the new {@code
     * Application's} {@code initialize} and {@code startup} methods.
     *
     * When UI is ready, method {@code ready} is called.
     *
     * The {@code launch} method is
     * typically called from the Application's {@code main}:
     * <pre>
     *     public static void main(String[] args) {
     *         Application.launch(MyApplication.class, args);
     *     }
     * </pre>
     * The {@code applicationClass} constructor and {@code startup} methods
     * run on the event dispatching thread.
     *
     * @param applicationClass the {@code Application} class to launch
     * @param args {@code main} method arguments
     * @see #shutdown
     * @see ApplicationContext#getApplication
     */
    public static synchronized <T extends Application> void launch(final Class<T> applicationClass, final String[] args) {
        Runnable doCreateAndShowGUI = new Runnable() {

            @Override
            public void run() {
                try {
                    application = create(applicationClass);
                    application.initialize(args);
                    application.startup();
                    application.waitForReady();
                } catch (Exception e) {
                    String msg = String.format("Application %s failed to launch", applicationClass);
                    LOG.log(Level.SEVERE, msg, e);
                    // Prevent a hanging vm if launching failed.
                    System.exit(1);
                }
            }
        };
        SwingUtilities.invokeLater(doCreateAndShowGUI);
    }

    /**
     * Initializes the ApplicationContext applicationClass and application
     * properties.
     *
     * Note that, as of Java SE 5, referring to a class literal
     * doesn't force the class to be loaded.  More info:
     * http://java.sun.com/javase/technologies/compatibility.jsp#literal
     * It's important to perform these initializations early, so that
     * Application static blocks/initializers happen afterwards.
     *
     * @param applicationClass the {@code Application} class to create.
     * @return The application instance.
     */
    private static <T extends Application> T create(
            Class<T> applicationClass)
        throws Exception
    {
        /* A common mistake for privileged applications that make
         * network requests (and aren't applets or web started) is to
         * not configure the http.proxyHost/Port system properties.
         * We paper over that issue here.
         */
        try {
            System.setProperty("java.net.useSystemProxies", "true");
        } catch (SecurityException ignoreException) {
            // Unsigned apps can't set this property.
        }

        T application =
                ServiceManager.getApplicationService( applicationClass );

        setLookAndFeel( application.getLookAndFeel() );

        // Generic registration with the Mac OS X application menu.
        // TODO micbinz -- This is not working on M1.
//        if ( PlatformType.OS_X == platform )
//        {
//            try {
//                OSXAdapter.setQuitHandler(application, Application.class.getDeclaredMethod("handleQuit", (Class[])null));
//            } catch (Exception e) {
//                LOG.log(Level.SEVERE, "Cannot set Mac Os X specific handler for Quit event", e);
//            }
//        }

        return application;
    }

    /**
     * Sets the look and feel.
     *
     * Uses the value of {@link #KEY_APPLICATION_LOOKANDFEEL} from the
     * application resources.
     * If this key is not set, then the system L&F is used.
     * If set to 'default', then no L&F is set.
     * Otherwise the value is used to look up a L&F from
     * the installed L&Fs by name.
     * If not found, then 'system' is used.
     */
    private static void setLookAndFeel( String lnf )
    {
        if ( StringUtil.isEmpty( lnf ) )
            throw new IllegalArgumentException( "Invalid LookAndFeel" );

        // For default nothing to do.
        if ( "default".equalsIgnoreCase( lnf ) )
            return;

        if ( "system".equalsIgnoreCase( lnf ) )
        {
            setSystemLnf();
            return;
        }

        try
        {
            SwingXUtilities.setLookAndFeel( lnf );
        } catch ( Exception e ) {
            LOG.warning( e.getMessage() );
        }
    }

    /**
     * Sets the system look and feel.
     */
    private static void setSystemLnf()
    {
        try
        {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName() );
        }
        catch ( Exception e )
        {
            LOG.log( Level.WARNING, "Could not set system look and feel.", e );
        }
    }

    /**
     *  Calls the ready method when the eventQ is quiet.
     */
    private void waitForReady() {
        new DoWaitForEmptyEventQ().execute();
    }

    /**
     * Responsible for initializations that must occur before the
     * GUI is constructed by {@code startup}.
     * <p>
     * This method is called by the static {@code launch} method,
     * before {@code startup} is called. Subclasses that want
     * to do any initialization work before {@code startup} must
     * override it.  The {@code initialize} method
     * runs on the event dispatching thread.
     * <p>
     * By default initialize() does nothing.
     *
     * @param args the main method's arguments.
     * @see #launch
     * @see #startup
     * @see #shutdown
     */
    protected void initialize(String[] args) {
    }

    /**
     * Responsible for starting the application; for creating and showing
     * the initial GUI.
     * <p>
     * This method is called by the static {@code launch} method,
     * subclasses must override it.  It runs on the event dispatching
     * thread.
     *
     * @see #launch
     * @see #initialize
     * @see #shutdown
     */
    protected abstract void startup();

    /**
     * Called after the startup() method has returned and there
     * are no more events on the
     * {@link Toolkit#getSystemEventQueue system event queue}.
     * When this method is called, the application's GUI is ready
     * to use.
     * <p>
     * It's usually important for an application to start up as
     * quickly as possible.  Applications can override this method
     * to do some additional start up work, after the GUI is up
     * and ready to use.
     *
     * @see #launch
     * @see #startup
     * @see #shutdown
     */
    protected void ready() {
    }

    /**
     * Called when the application {@link #exit exits}.
     * Subclasses may override this method to do any cleanup
     * tasks that are necessary before exiting.  Obviously, you'll want to try
     * and do as little as possible at this point.  This method runs
     * on the event dispatching thread.
     *
     * @see #startup
     * @see #ready
     * @see #exit
     * @see #addExitListener
     */
    protected void shutdown() {
        // TBD should call TaskService#shutdownNow() on each TaskService
    }

    /* An event that sets a flag when it's dispatched and another
     * flag, see isEventQEmpty(), that indicates if the event queue
     * was empty at dispatch time.
     */
    @SuppressWarnings("serial")
    private static class NotifyingEvent extends PaintEvent implements ActiveEvent {

        private boolean dispatched = false;
        private boolean qEmpty = false;

        NotifyingEvent(Component c) {
            super(c, PaintEvent.UPDATE, null);
        }

        synchronized boolean isDispatched() {
            return dispatched;
        }

        synchronized boolean isEventQEmpty() {
            return qEmpty;
        }

        @Override
        public void dispatch() {
            EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
            synchronized (this) {
                qEmpty = (q.peekEvent() == null);
                dispatched = true;
                notifyAll();
            }
        }
    }

    /**
     * Keep queuing up NotifyingEvents until the event queue is
     * empty when the NotifyingEvent is dispatched().
     */
    private void waitForEmptyEventQ(JPanel placeHolder) {
        boolean qEmpty = false;
        EventQueue q = Toolkit.getDefaultToolkit().getSystemEventQueue();
        while (!qEmpty) {
            NotifyingEvent e = new NotifyingEvent(placeHolder);
            q.postEvent(e);
            synchronized (e) {
                while (!e.isDispatched()) {
                    try {
                        e.wait();
                    } catch (InterruptedException ie) {
                        //ignore
                    }
                }
                qEmpty = e.isEventQEmpty();
            }
        }
    }

    /* When the event queue is empty, give the app a chance to do
     * something, now that the GUI is "ready".
     */
    private class DoWaitForEmptyEventQ extends Task<Void, Void> {
        private final JPanel placeHolder;
        DoWaitForEmptyEventQ() {
            super(Application.this);
            placeHolder = new JPanel();
        }

        @Override
        protected Void doInBackground() {
            waitForEmptyEventQ(placeHolder);
            return null;
        }

        @Override
        protected void finished() {
            ready = true;
            ready();
        }
    }

    /**
     * Gracefully shutdowns the application, calls {@code exit(null)}
     * This version of exit() is convenient if the decision to exit the
     * application wasn't triggered by an event.
     *
     * @see #exit(EventObject)
     */
    public final void exit() {
        exit(null);
    }

    /**
     * Gracefully shutdowns the application.
     * <p>
     * If none of the {@code ExitListener.canExit()} methods return false,
     * calls the {@code ExitListener.willExit()} methods, then
     * {@code shutdown()}, and then exits the Application with
     * {@link #end end}.  Exceptions thrown while running willExit() or shutdown()
     * are logged but otherwise ignored.
     * <p>
     * If the caller is responding to an GUI event, it's helpful to pass the
     * event along so that ExitListeners' canExit methods that want to popup
     * a dialog know on which screen to show the dialog.  For example:
     * <pre>
     * class ConfirmExit implements Application.ExitListener {
     *     public boolean canExit(EventObject e) {
     *         Object source = (e != null) ? e.getSource() : null;
     *         Component owner = (source instanceof Component) ? (Component)source : null;
     *         int option = JOptionPane.showConfirmDialog(owner, "Really Exit?");
     *         return option == JOptionPane.YES_OPTION;
     *     }
     *     public void willExit(EventObejct e) {}
     * }
     * myApplication.addExitListener(new ConfirmExit());
     * </pre>
     * The {@code eventObject} argument may be null, e.g. if the exit
     * call was triggered by non-GUI code, and {@code canExit}, {@code
     * willExit} methods must guard against the possibility that the
     * {@code eventObject} argument's {@code source} is not a {@code
     * Component}.
     *
     * @param event the EventObject that triggered this call or null
     * @see #addExitListener
     * @see #removeExitListener
     * @see #shutdown
     * @see #end
     */
    public void exit(final EventObject event) {
        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                for (ExitListener listener : _exitListeners) {
                    if (!listener.canExit(event)) {
                        return;
                    }
                }
                try {
                    for (ExitListener listener : _exitListeners) {
                        try {
                            listener.willExit(event);
                        } catch (Exception e) {
                            LOG.log(Level.WARNING, "ExitListener.willExit() failed", e);
                        }
                    }
                    shutdown();
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "unexpected error in Application.shutdown()", e);
                } finally {
                    end();
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(runnable);
            } catch (Exception ignore) {  }
        }
    }

    /**
     * Called by {@link #exit exit} to terminate the application.  Calls
     * {@code Runtime.getRuntime().exit(0)}, which halts the JVM.
     *
     * @see #exit
     */
    protected void end() {
        Runtime.getRuntime().exit(0);
    }

    /**
     * Gives the Application a chance to veto an attempt to exit/quit.
     * An {@code ExitListener's} {@code canExit} method should return
     * false if there are pending decisions that the user must make
     * before the app exits.  A typical {@code ExitListener} would
     * prompt the user with a modal dialog.
     * <p>
     * The {@code eventObject} argument will be the the value passed
     * to {@link #exit(EventObject) exit()}.  It may be null.
     * <p>
     * The {@code willExit} method is called after the exit has
     * been confirmed.  An ExitListener that's going to perform
     * some cleanup work should do so in {@code willExit}.
     * <p>
     * {@code ExitListeners} run on the event dispatching thread.
     *
     * @see #exit(EventObject)
     * @see #addExitListener
     * @see #removeExitListener
     */
    public interface ExitListener extends EventListener {

        /**
         * The method is called before the Application exits.
         *
         * @param event the {@code EventObject} object. It will be the the value passed
         * to {@link #exit(EventObject) exit()}.
         * @return {@code true} if application can proceed with shutdown
         * process; {@code false} if there are pending decisions that the
         * user must make before the application exits.
         */
        boolean canExit(EventObject event);

        /**
         * The method is called after the exit has been confirmed.
         *
         * @param event the {@code EventObject} object. It will be the the value passed
         * to {@link #exit(EventObject) exit()}.
         */
        void willExit(EventObject event);
    }

    /**
     * Adds an {@code ExitListener} to the list.
     *
     * @param listener the {@code ExitListener}
     * @see #removeExitListener
     * @see #getExitListeners
     */
    public void addExitListener(ExitListener listener) {
        _exitListeners.add(listener);
    }

    /**
     * Removes an {@code ExitListener} from the list.
     *
     * @param listener the {@code ExitListener}
     * @see #addExitListener
     * @see #getExitListeners
     */
    public void removeExitListener(ExitListener listener) {
        _exitListeners.remove(listener);
    }

    /**
     * All of the {@code ExitListeners} added so far.
     *
     * @return all of the {@code ExitListeners} added so far.
     */
    public final List<ExitListener> getExitListeners() {
        return Collections.unmodifiableList( _exitListeners );
    }

    /**
     * The default {@code Action} for quitting an application,
     * {@code quit} just exits the application by calling {@code exit(e)}.
     *
     * @param e the triggering event
     * @see #exit(EventObject)
     */
    public void quit(ActionEvent e) {
        exit(e);
    }

    /**
     * The ApplicationContext for this Application.
     *
     * @return the Application's ApplicationContext
     */
    public final ApplicationContext getContext() {
        return context;
    }

    /**
     * The {@code Application} singleton.
     * <p>
     * This method is only called after an Application has
     * been launched.
     *
     * @param applicationClass this Application's subclass
     * @return the launched Application singleton.
     * @see Application#launch
     */
    public static synchronized <T extends Application> T getInstance(Class<T> applicationClass) {

        if (Beans.isDesignTime() && application==null) {
            try {
                application = create(applicationClass);
            } catch (Exception ex) {
                String msg = String.format("Couldn't construct %s", applicationClass);
                Logger.getLogger(Application.class.getName()).log(Level.SEVERE, msg, ex);
                throw new Error(msg, ex);
            }
        }

        checkApplicationLaunched();
        return applicationClass.cast(application);
    }

    /**
     * The {@code Application} singleton.
     * <p>
     * This method is only called after an Application has
     * been launched.
     *
     * @return the Application singleton or a placeholder
     * @see Application#launch
     * @see Application#getInstance(Class)
     */
    public static synchronized Application getInstance()
    {
        checkApplicationLaunched();
        return application;
    }

    private static void checkApplicationLaunched() throws IllegalStateException {
        if (application == null) {
            throw new IllegalStateException("Application is not launched.");
        }
    }

    boolean isLaunched()
    {
        return application != null;
    }

    /**
     * Shows the application {@code View}
     * @param view The View to show
     */
    public void show(View view)
    {
        Window window = (Window) view.getRootPane().getParent();
        if (window != null) {
            window.pack();
            window.setVisible(true);
        }
    }

    /**
     * Hides the application {@code View}
     * @param view
     * @see View
     */
    public void hide(View view) {
        view.getRootPane().getParent().setVisible(false);
    }

    /**
     * The state of the initial UI.
     * @return true if the initial UI is ready
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * <p>Get the application's {@link ResourceManager}.  This returns
     * the application wide resource manager as returned by the
     * {@link ApplicationContext}.</p>
     * <p>This operation can also be used in case the {@link Application}
     * is not initialized yet or will never be initialized to use the
     * resource manager stand-alone.</p>
     *
     * @return A resource manager.  If the {@link Application} has not
     * been initialized this will return a newly allocated Resource manager
     * that can be used stand-alone. Never returns {@code null}.
     */
    public static org.smack.util.resource.ResourceManager getResourceManager()
    {
        return ServiceManager.getApplicationService( org.smack.util.resource.ResourceManager.class );
    }

    @Resource
    private String id = getClass().getSimpleName();

    /**
     * Return the application's id as defined in the resources.
     * @return The application's id.
     */
    public String getId()
    {
        return id;
    }

    @Resource
    private String title;
    /**
     * Return the application's title as defined in the resources.
     * @return The application's title.
     */
    public String getTitle()
    {
        return title;
    }

    @Resource
    private String version;
    /**
     * Return the application's version as defined in the resources.
     * @return The application's version.
     */
    public String getVersion()
    {
        return version;
    }

    @Resource
    private Image icon;
    /**
     * Return the application's icon as defined in the resources.
     * @return The application icon.
     */
    public Image getIcon()
    {
        return icon;
    }

    @Resource
    private String vendor;
    /**
     * Return the application's vendor as defined in the resources.
     * @return The vendor name.
     */
    public String getVendor()
    {
        return vendor;
    }

    @Resource
    private String vendorId;
    /**
     * Return the application's vendor as defined in the resources.
     * @return The vendor name.
     */
    public String getVendorId()
    {
        return vendorId;
    }

    @Resource( dflt = DEFAULT_LOOK_AND_FEEL )
    private String lookAndFeel;
    /**
     * Return the application's vendor as defined in the resources.
     * @return The vendor name.
     */
    public String getLookAndFeel()
    {
        return lookAndFeel;
    }

    /**
     * Get an application service of the specified type.
     *
     * @param singletonType The type of the application service.
     * @return An instance of the requested service.
     */
    protected static synchronized <T> T getApplicationService( Class<T> singletonType )
    {
        return ServiceManager.getApplicationService( singletonType );
    }
}
