/* $Id$
 *
 * http://sourceforge.net/projects/smackfw/
 *
 * Copyright © 2005-2012 Michael G. Binz
 */
package org.smack.swing.application.util;

import org.smack.swing.application.Application;
import org.smack.swing.application.ResourceManager;
import org.smack.swing.application.ResourceMap;

/**
 * Help methods on application level.
 *
 * @author Michael Binz
 * @author Vity
 */
public final class AppHelper {

    private AppHelper() {
        throw new AssertionError();
    }

    /**
     * Get the resource manager for the passed application.
     *
     * @param application The application.
     * @return The associated resource manager.
     */
    public static ResourceManager getResourceManager( Application application )
    {
        return application.getApplicationService( ResourceManager.class );
    }

    /**
     * Returns the application resource map.
     *
     * @param application The application instance.
     * @return The application resource map.
     */
    public static ResourceMap getResourceMap( Application application )
    {
        return getResourceManager( application ).getResourceMap( application.getClass() );
    }

    /**
     * Returns the resource map for the given class.
     *
     * @param app The application instance.
     * @param pClass The target class.
     * @return The associated resource map.
     */
    public static ResourceMap getResourceMap( Application app, Class<?> pClass )
    {
        return getResourceManager( app ).getResourceMap(pClass);
    }
}
