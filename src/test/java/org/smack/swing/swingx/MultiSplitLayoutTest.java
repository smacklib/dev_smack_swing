/*
 * Copyright © 2023 Michael G. Binz
 */
package org.smack.swing.swingx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.Container;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;

import javax.swing.JSplitPane;

import org.junit.jupiter.api.Test;
import org.smack.swing.swingx.MultiSplitLayout.ColSplit;
import org.smack.swing.swingx.MultiSplitLayout.Divider;
import org.smack.swing.swingx.MultiSplitLayout.InvalidLayoutException;
import org.smack.swing.swingx.MultiSplitLayout.Leaf;
import org.smack.swing.swingx.MultiSplitLayout.Node;
import org.smack.swing.swingx.MultiSplitLayout.RowSplit;
import org.smack.swing.swingx.MultiSplitLayout.Split;
import org.smack.swing.test.MockContainer;
import org.smack.util.Holder;
import org.smack.util.JavaUtil;

public class MultiSplitLayoutTest
{
    @Test
    public void basicTest() throws Exception
    {
        ///////////////////
        // left // right //
        ///////////////////
        //  bottom       //
        ///////////////////

        Split row = JavaUtil.make( () -> {
            var left = new Leaf("left");
            left.setWeight( .5 );
            var right = new Leaf( "right" );
            right.setWeight( .5 );

            return new RowSplit(
                    left,
                    new Divider(),
                    right );
        });

        Split column = JavaUtil.make( () -> {
            var bottom = new Leaf( "bottom" );
            bottom.setWeight( .5 );
            row.setWeight( .5 );

            var result = new ColSplit(
                    row,
                    new Divider(),
                    bottom );
            result.setRowLayout( false );

            return result;
        });

        var mspl = new MultiSplitLayout( column );
        mspl.setDividerSize( 10 );

        Container container = new MockContainer( 200, 100 );

        mspl.layoutContainer( container );

        {
            var d = mspl.getNodeForName( "left" ).getBounds();
            assertEquals(
                new Rectangle( 0, 0, 95, 50 ),
                d );
        }
        {
            var d = mspl.getNodeForName( "right" ).getBounds();
            assertEquals(
                new Rectangle( 105, 0, 95, 50 ),
                d );
        }
        {
            var d = mspl.getNodeForName( "bottom" ).getBounds();
            assertEquals(
                new Rectangle( 0, 50, 200, 50 ),
                d );
        }
    }

    @Test
    public void basicTest_rounding() throws Exception
    {
        ///////////////////
        // left // right //
        ///////////////////
        //  bottom       //
        ///////////////////

        Split row = JavaUtil.make( () -> {
            var left = new Leaf("left");
            left.setWeight( .5 );
            var right = new Leaf( "right" );
            right.setWeight( .5 );

            return new RowSplit(
                    left,
                    new Divider(),
                    right );
        });

        Split column = JavaUtil.make( () -> {
            var bottom = new Leaf( "bottom" );
            bottom.setWeight( .5 );
            row.setWeight( .5 );

            var result = new ColSplit(
                    row,
                    new Divider(),
                    bottom );
            result.setRowLayout( false );

            return result;
        });

        var mspl = new MultiSplitLayout( column );
        mspl.setDividerSize( 10 );

        final int WIDTH = 201;
        final int HEIGHT = 101;
        Container container = new MockContainer( WIDTH, HEIGHT );

        mspl.layoutContainer( container );

        {
            var d = mspl.getNodeForName( "left" ).getBounds();
            assertEquals(
                new Rectangle( 0, 0, 96, 51 ),
                d );
        }
        {
            var d = mspl.getNodeForName( "right" ).getBounds();
            assertEquals(
                new Rectangle( 105, 0, 96, 51 ),
                d );
        }
        {
            var d = mspl.getNodeForName( "bottom" ).getBounds();
            assertEquals(
                new Rectangle( 0, 51, 201, 51 ),
                d );
        }
        {
            assertEquals(
                    WIDTH,
                    mspl.getNodeForName( "left" ).getParent().extent() );
        }
    }

    @Test
    public void basicTest_rounding2() throws Exception
    {
        RowSplit row = JavaUtil.make( () -> {
            var one = new Leaf("1");
            one.setWeight( .3 );
            var two = new Leaf( "2" );
            two.setWeight( .3 );
            // TODO validate names.
            var three = new Leaf( "3" );
            three.setWeight( .0 );

            return new RowSplit(
                    one,
                    new Divider(),
                    two,
                    new Divider(),
                    three );

        });

        var mspl = new MultiSplitLayout( row );
        mspl.setDividerSize( 10 );

        final int WIDTH = 201;
        final int HEIGHT = 101;
        Container container = new MockContainer( WIDTH, HEIGHT );

        mspl.layoutContainer( container );

        {
            var d = mspl.getNodeForName( "1" ).getBounds();
            assertEquals(
                new Rectangle( 0, 0, 54, 101 ),
                d );
        }
        {
            var d = mspl.getNodeForName( "2" ).getBounds();
            assertEquals(
                new Rectangle( 64, 0, 54, 101 ),
                d );
        }
        {
            var d = mspl.getNodeForName( "3" ).getBounds();
            assertEquals(
                new Rectangle( 129, 0, 72, 101 ),
                d );
        }
        {
            assertEquals(
                    WIDTH,
                    row.extent() );
        }
    }

    @Test
    public void propDividerSizeTest() throws Exception
    {
        Holder<PropertyChangeEvent> pceh = new Holder<>();

        final var mspl = new MultiSplitLayout();

        mspl.addPropertyChangeListener( ce -> pceh.set( ce ) );
        assertNull( pceh.get() );

        mspl.setDividerSize( 313 );

        assertNotNull( pceh.get() );

        assertEquals( "dividerSize", pceh.get().getPropertyName() );
        assertEquals( 313, (int)pceh.get().getNewValue() );
        assertEquals( new JSplitPane().getDividerSize(), (int)pceh.get().getOldValue() );
        assertEquals( mspl, pceh.get().getSource() );
    }

    @Test
    public void propModelTest() throws Exception
    {
        Holder<PropertyChangeEvent> pceh = new Holder<>();

        final var mspl = new MultiSplitLayout();

        mspl.addPropertyChangeListener( ce -> pceh.set( ce ) );
        assertNull( pceh.get() );

        mspl.setModel( new Leaf( "test" ) );

        assertNotNull( pceh.get() );

        assertEquals(
                "model",
                pceh.get().getPropertyName() );
        assertEquals(
                "test",
                Leaf.class.cast(pceh.get().getNewValue() ).getName() );
        assertEquals(
                "default",
                Leaf.class.cast(pceh.get().getOldValue() ).getName() );
        assertEquals(
                mspl,
                pceh.get().getSource() );
    }


    @Test
    public void Split_validate_lessThanTwoNodes() throws Exception
    {
        final int count = 0;

        Node[] nodes = new Node[]
        {
            new Leaf( "one" ),
        };

        Split split = new Split( nodes );

        try {
            split.validate();
        }
        catch ( InvalidLayoutException e )
        {
            assertEquals( split, e.getNode() );
        }
    }

    @Test
    public void Split_children_weightExceeds100() throws Exception
    {
        final int count = 1;

        Node[] nodes = new Node[]
        {
            new Leaf( "one" ).weight( .8 ),
            new Divider(),
            new Leaf( "two" ).weight( .9 )
        };

        Split split = new Split( nodes );

        try {
            split.validate();
        }
        catch ( InvalidLayoutException e )
        {
            assertEquals( split, e.getNode() );
        }
    }

    @Test
    public void Split_children_0() throws Exception
    {
        final int count = 0;

        Node[] nodes = new Node[]
        {
            new Leaf( "one" ),
        };

        Split split = new Split( nodes );

        assertEquals( nodes.length, split.getChildren().size() );
        assertEquals( nodes.length-count, split.size() );
    }

    @Test
    public void Split_children_1() throws Exception
    {
        final int count = 1;

        Node[] nodes = new Node[]
        {
            new Leaf( "one" ),
            new Divider(),
            new Leaf( "two" )
        };

        Split split = new Split( nodes );

        assertEquals( nodes.length, split.getChildren().size() );
        assertEquals( nodes.length-count, split.size() );
    }

    @Test
    public void Split_children_2() throws Exception
    {
        int count = 2;

        Node[] nodes = new Node[]
        {
            new Leaf( "one" ),
            new Divider(),
            new Leaf( "two" ),
            new Divider(),
            new Leaf( "three" )
        };

        Split split = new Split( nodes );

        assertEquals( nodes.length, split.getChildren().size() );
        assertEquals( nodes.length-count, split.size() );
    }
}
