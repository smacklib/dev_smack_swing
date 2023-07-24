/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2016-2023 Michael G. Binz
 */
package org.smack.swing.beans;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.smack.swing.application.ApplicationProperties;
import org.smack.util.JavaUtil;
import org.smack.util.ServiceManager;
import org.smack.util.StringUtil;
import org.smack.util.converters.StringConverter;

/**
 * A JavaBean property that uses {@link ApplicationProperties} for persistence.
 *
 * @param <T> The property's type.
 * @param <B> The bean the property is part of.
 *
 * @author Michael G. Binz
 */
public class PersistentJavaBeanProperty<T,B> extends JavaBeanProperty<T,B>
{
    private static final Logger LOG =
            Logger.getLogger( PersistentJavaBeanProperty.class.getName() );

    private final StringConverter _converter;

    private final String _keyName;

    /**
     * Create an instance.  The passed bean has to offer a JavaBean
     * property for 'name' that must be set.  This name is used in
     * the persistence store to match a bean against its value.
     *
     * @param bean A reference to our bean.
     * @param initialValue The property's initial value.
     * @param propertyName The name of the property.
     * @param converter A string converter for the property type.
     *
     * @throws Exception if bean does not offer a 'name' property.
     */
    public PersistentJavaBeanProperty(
            B bean,
            T initialValue,
            String propertyName,
            StringConverter converter )
    {
        super( bean, initialValue, propertyName );

        PropertyProxy<String, B> named = new PropertyProxy<>( "name", bean );
        JavaUtil.Assert( String.class.equals( named.getType() ) );
        String name = named.get();
        JavaUtil.Assert( StringUtil.hasContent( name ), "Empty name." );

        _keyName = name + ":" + propertyName;

        _converter = converter;

        String value =
                getAps().get( bean.getClass(), _keyName, null );

        if ( value != null )
        {
            try
            {
                super.set( _converter.convert( getType(), value ) );
                return;
            }
            catch ( Exception e )
            {
                // Could not convert what we found in persistence.
                // Continue and re-initialize...
                LOG.log( Level.WARNING, propertyName, e );
            }
        }

        // Either the value was not in the persistence or what was
        // found there could not be converted. Initialize the entry
        // now.
        set( initialValue );
    }

    @Override
    public void set( T newValue )
    {
        // TODO: we need a conversion from T->String, but the
        // StringConverter currently only supports String->T.
        // For now we fall back to #toString() but this may fail
        // for complex types.
        String value =
                newValue == null ?
                        StringUtil.EMPTY_STRING :
                        newValue.toString();

        // Put the value into the persistence.
        getAps().put(
                getBean().getClass(),
                _keyName,
                value );

        super.set( newValue );
    }

    private ApplicationProperties getAps()
    {
        return ServiceManager.getApplicationService(
                ApplicationProperties.class );
    }
}
