package org.smack.swing.util.converters;

import org.smack.swing.application.ResourceConverter.ResourceConverterException;
import org.smack.swing.util.ResourceConverter;
import org.smack.swing.util.ResourceMap;

abstract class NumberStringConverter extends ResourceConverter {

    private final Class<?> primitiveType;

    NumberStringConverter(Class<?> type, Class<?> primitiveType) {
        super(type);
        this.primitiveType = primitiveType;
    }

    protected abstract Number parseString(String s) throws NumberFormatException;

    @Override
    public Object parseString(String s, ResourceMap ignore) throws ResourceConverterException {
        try {
            return parseString(s);
        } catch (NumberFormatException e) {
            throw new ResourceConverterException("invalid " + type.getSimpleName(), s, e);
        }
    }

    @Override
    public boolean supportsType(Class<?> testType) {
        return testType.equals(type) || testType.equals(primitiveType);
    }
}
