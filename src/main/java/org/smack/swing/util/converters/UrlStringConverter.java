package org.smack.swing.util.converters;

import java.net.MalformedURLException;
import java.net.URL;

import org.smack.swing.application.ResourceConverter.ResourceConverterException;
import org.smack.swing.util.ResourceConverter;
import org.smack.swing.util.ResourceMap;

public class UrlStringConverter extends ResourceConverter {

        public UrlStringConverter() {
            super(URL.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            try {
                return new URL(s);
            } catch (MalformedURLException e) {
                throw new ResourceConverterException("invalid URL", s, e);
            }
        }
    }

