package org.smack.swing.util.converters;

import java.net.URI;
import java.net.URISyntaxException;

import org.smack.swing.application.ResourceConverter.ResourceConverterException;
import org.smack.swing.util.ResourceConverter;
import org.smack.swing.util.ResourceMap;

public class UriStringConverter extends ResourceConverter {

        public UriStringConverter() {
            super(URI.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
            try {
                return new URI(s);
            } catch (URISyntaxException e) {
                throw new ResourceConverterException("invalid URI", s, e);
            }
        }
    }
