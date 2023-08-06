package org.smack.swing.test;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;

public class MockContainer extends Container
{
    private final Dimension _dimension;
    private final Insets _insets;

    public MockContainer( Dimension dimension, Insets insets )
    {
        _dimension = dimension;
        _insets = insets;
    }

    public MockContainer( Dimension dimension )
    {
        this( dimension, new Insets( 0, 0, 0, 0 ) );
    }

    public MockContainer( int width, int height )
    {
        this(
            new Dimension( width, height ),
            new Insets( 0, 0, 0, 0 ) );
    }

    @Override
    public Dimension getSize()
    {
        return _dimension;
    }

    @Override
    public Insets getInsets()
    {
        return _insets;
    }

    private static final long serialVersionUID = -4010178581018704463L;
}
