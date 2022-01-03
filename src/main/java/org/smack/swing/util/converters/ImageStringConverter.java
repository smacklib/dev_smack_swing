package org.smack.swing.util.converters;

import java.awt.Image;

import org.smack.swing.util.ResourceConverter;
import org.smack.swing.util.ResourceMap;

public class ImageStringConverter extends ResourceConverter {

    public ImageStringConverter() {
        super(Image.class);
    }

    @Override
    public Object parseString(String s, ResourceMap resourceMap) throws Exception {
        return ConverterUtils.loadImageIcon(s, resourceMap).getImage();
    }
}

