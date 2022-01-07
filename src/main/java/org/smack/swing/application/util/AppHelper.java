/* $Id$
 *
 * http://sourceforge.net/projects/smackfw/
 *
 * Copyright © 2005-2012 Michael G. Binz
 */
package org.smack.swing.application.util;

import org.smack.swing.application.Application;
import org.smack.util.ServiceManager;
import org.smack.util.resource.ResourceManager;
import org.smack.util.resource.ResourceMap;

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
    private static ResourceManager getResourceManager( Application application )
    {
        return ServiceManager.getApplicationService( ResourceManager.class );
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
}
