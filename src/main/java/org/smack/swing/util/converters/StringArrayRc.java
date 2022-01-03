package org.smack.swing.util.converters;

import java.lang.reflect.Array;

import org.smack.swing.util.ResourceConverter;
import org.smack.swing.util.ResourceMap;
import org.smack.util.StringUtil;

/**
 * Converts string arrays, handles quoting.
 *
 * @version $Revision$
 * @author Michael Binz
 */
public final class StringArrayRc extends ResourceConverter
{
    public StringArrayRc()
    {
        super( String[].class );

        if ( ! type.isArray() )
            throw new IllegalArgumentException();
    }

    @Override
    public Object parseString( String s, ResourceMap r )
            throws Exception
    {
        String[] split = StringUtil.splitQuoted( s );

        Object result = Array.newInstance(
                getType().getComponentType(), split.length );

        int idx = 0;
        for ( String c : split )
        {
            Array.set(
                    result,
                    idx++,
                    c );
        }

        return result;
    }
}
