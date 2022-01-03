package org.smack.swing.application.converters;

import java.awt.Image;

import org.smack.swing.application.ResourceConverter;
import org.smack.swing.application.ResourceMap;

public class ImageStringConverter extends ResourceConverter {

    public ImageStringConverter() {
        super(Image.class);
    }

    @Override
    public Object parseString(String s, ResourceMap resourceMap) throws ResourceConverterException {
        return ConverterUtils.loadImageIcon(s, resourceMap).getImage();
    }
}

