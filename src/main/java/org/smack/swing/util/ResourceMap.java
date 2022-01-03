package org.smack.swing.util;

import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;

import org.smack.swing.smack.util.StringUtils;

/**
 * A map holding all resources defined in the resources for
 * the passed class.  Resources for a class foo.bar.Elk are
 * defined in the property file foo.bar.resources.Elk.
 *
 * @version $Rev$
 * @author Michael Binz
 */
@SuppressWarnings("serial")
public class ResourceMap extends HashMap<String, String>
{
    private final String _bundleName;

    private final Class<?> _class;

    public ResourceMap( Class<?> cl )
    {
        _class = cl;

        String pack =
                cl.getPackage().getName();
        if ( StringUtils.isEmpty( pack ) )
            pack = StringUtils.EMPTY_STRING;

        _bundleName =
                String.format( "%s.resources.%s",
                        pack,
                        cl.getSimpleName() );

        ClassLoader cldr = cl.getClassLoader();
        if ( cldr == null )
            cldr = Thread.currentThread().getContextClassLoader();

        try
        {
            ResourceBundle bundle =
                    ResourceBundle.getBundle(
                            _bundleName,
                            Locale.getDefault(),
                            cldr,
                            Control.getControl(
                                    Control.FORMAT_PROPERTIES ) );

            for ( String ck : bundle.keySet() )
                put( ck, bundle.getString( ck ) );
        }
        catch ( MissingResourceException e )
        {
            return;
        }
    }

    public String get( String key )
    {
        return super.get( _class.getSimpleName() + "." + key );
    }

    /**
     * Get the name of the underlying resource bundle.
     */
    public String getName()
    {
        return _bundleName;
    }

    public ClassLoader getClassLoader()
    {
        return _class.getClassLoader();
    }

    public String getResourceDir()
    {
        throw new IllegalArgumentException( "Not implemented yet." );
    }
}
