package org.smack.swing.util.converters;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.smack.swing.util.ResourceConverter;
import org.smack.swing.util.ResourceMap;

public class IconStringConverter extends ResourceConverter {

    public IconStringConverter() {
        super(Icon.class);
    }

    @Override
    public Object parseString(String s, ResourceMap resourceMap) throws Exception {
        return ConverterUtils.loadImageIcon(s, resourceMap);
    }

    @Override
    public boolean supportsType(Class<?> testType) {
        return testType.equals(Icon.class) || testType.equals(ImageIcon.class);
    }
}
