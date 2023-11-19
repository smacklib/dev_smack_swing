/*
 * smack_swing @ https://github.com/smacklib/dev_smack_swing
 *
 * Copyright © 2015-2023 Michael G. Binz
 */
package org.smack.swing.beans;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeListenerProxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.smack.util.JavaUtil;
import org.smack.util.StringUtil;

/**
 * Support class for implementing java bean properties on components.
 *
 * <pre>
 * class JXDesktop extends AbstractBean // or offers property change support.
 * {
 *     ...
 *
 *     public final JavaBeanProperty<Image,JXDesktop>  P_BACKGROUND_IMAGE =
 *         new JavaBeanProperty<Image,JXDesktop>( this, null, "backgroundImage" );
 *
 *     public void setBackgroundImage( Image newValue )
 *     {
 *         P_BACKGROUND_IMAGE.set( newValue );
 *     }
 *
 *     public Image getBackgroundImage()
 *     {
 *        return P_BACKGROUND_IMAGE.get();
 *     }
 *     ...
 * }
 * </pre>
 *
 * @author Michael Binz
 */
public class JavaBeanProperty<T,B> implements PropertyType<T,B>
{
    private final String _name;
    private T _value;
    private final B _bean;
    private final Class<T> _beantype;

    private final PropertyAdapter _pa;

    /**
     * Create an instance.
     *
     * @param bean The target bean instance.
     * @param initialValue
     * @param propertyName
     */
    public JavaBeanProperty( B bean, T initialValue, String propertyName )
    {
        Objects.requireNonNull( bean );
        JavaUtil.Assert(
                StringUtil.hasContent( propertyName ),
                "Empty propertyName" );

        // This internally validates if the host offers the required
        // set and get operations.
        _beantype = new PropertyProxy<T,B>( propertyName, bean ).getType();

        // This internally validates if the bean has the pcl operations.
        _pa = new PropertyAdapter( bean );

        _bean = bean;
        _value = initialValue;
        _name = propertyName;
    }

    @Override
    public void set( T newValue )
    {
        if ( Objects.equals( _value, newValue ) )
            return;

        T oldValue = _value;
        _value = newValue;

        PropertyChangeEvent evt =
                new PropertyChangeEvent( _bean, _name, oldValue, newValue );

        for ( PropertyChangeListener c : getPcls() )
            c.propertyChange( evt );
    }

    /**
     * @return The property change listeners to call.
     */
    private List<PropertyChangeListener> getPcls()
    {
        List<PropertyChangeListener> result = new ArrayList<PropertyChangeListener>();

        result.addAll(
            Arrays.asList( _pa.getPropertyChangeListeners( _name ) ) );

        // Get the pcls that were added w/o a name. Since we seem to get all pcls,
        // with and w/o a name, we filter in the loop below.
        // See comment in AbstractBean#getPropertyChangeListeners.
        for ( PropertyChangeListener c : _pa.getPropertyChangeListeners() )
        {
            if ( c instanceof PropertyChangeListenerProxy )
                continue;

            if ( ! result.contains( c ) )
                result.add( c );
        }

        return result;
    }

    @Override
    public T get()
    {
        return _value;
    }

    @Override
    public Class<T> getType()
    {
        return _beantype;
    }

    @Override
    public String getName()
    {
        return _name;
    }

    @Override
    public B getBean()
    {
        return _bean;
    }
}
