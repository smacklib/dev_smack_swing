package org.smack.swing.util.converters;

import org.smack.swing.util.ResourceConverter;
import org.smack.swing.util.ResourceMap;

public class BooleanStringConverter extends ResourceConverter {

        public BooleanStringConverter() {
            super(Boolean.class);
        }

        @Override
        public Object parseString(String s, ResourceMap ignore) {

            return Boolean.parseBoolean( s );
        }

        @Override
        public boolean supportsType(Class<?> testType) {
            return testType.equals(Boolean.class) || testType.equals(boolean.class);
        }
}
