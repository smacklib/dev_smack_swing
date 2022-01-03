package org.smack.swing.util.converters;

import org.smack.swing.util.ResourceConverter;
import org.smack.swing.util.ResourceMap;
import org.smack.util.StringUtil;

/**
 *
 * @version $Rev$
 * @author Michael Binz
 */
public class StringStringConverter extends ResourceConverter {

    public StringStringConverter() {
        super(String.class);
    }

    @Override
    public Object parseString( String s, ResourceMap r )
    {
        String[] quotedParts = StringUtil.splitQuoted( s );

        return StringUtil.concatenate(
                StringUtil.EMPTY_STRING,
                quotedParts );
    }
}
