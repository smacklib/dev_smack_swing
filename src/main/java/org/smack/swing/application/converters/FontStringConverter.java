package org.smack.swing.application.converters;

import java.awt.Font;

import org.smack.swing.application.ResourceConverter;
import org.smack.swing.application.ResourceMap;

public class FontStringConverter extends ResourceConverter {

    public FontStringConverter() {
        super(Font.class);
    }
    /* Just delegates to Font.decode.
     * Typical string is: face-STYLE-size, for example "Arial-PLAIN-12"
     */

    @Override
    public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
        return Font.decode(s);
    }
}
