/*
 * Copyright © 2023 Michael G. Binz
 */
package org.smack.swing.swingx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Container;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.util.HashSet;

import javax.swing.JSplitPane;

import org.junit.jupiter.api.Test;
import org.smack.swing.swingx.MultiSplitLayout.Column;
import org.smack.swing.swingx.MultiSplitLayout.ColumnImpl;
import org.smack.swing.swingx.MultiSplitLayout.DividerImpl;
import org.smack.swing.swingx.MultiSplitLayout.InvalidLayoutException;
import org.smack.swing.swingx.MultiSplitLayout.Leaf;
import org.smack.swing.swingx.MultiSplitLayout.LeafImpl;
import org.smack.swing.swingx.MultiSplitLayout.NodeImpl;
import org.smack.swing.swingx.MultiSplitLayout.Row;
import org.smack.swing.swingx.MultiSplitLayout.RowImpl;
import org.smack.swing.swingx.MultiSplitLayout.SplitImpl;
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

        SplitImpl row = JavaUtil.make( () -> {
            var left = new LeafImpl("left");
            left.setWeight( .5 );
            var right = new LeafImpl( "right" );
            right.setWeight( .5 );

            return new RowImpl(
                    left,
                    new DividerImpl(),
                    right );
        });

        SplitImpl column = JavaUtil.make( () -> {
            var bottom = new LeafImpl( "bottom" );
            bottom.setWeight( .5 );
            row.setWeight( .5 );

            var result = new ColumnImpl(
                    row,
                    new DividerImpl(),
                    bottom );

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

        SplitImpl row = JavaUtil.make( () -> {
            var left = new LeafImpl("left");
            left.setWeight( .5 );
            var right = new LeafImpl( "right" );
            right.setWeight( .5 );

            return new RowImpl(
                    left,
                    new DividerImpl(),
                    right );
        });

        SplitImpl column = JavaUtil.make( () -> {
            var bottom = new LeafImpl( "bottom" );
            bottom.setWeight( .5 );
            row.setWeight( .5 );

            var result = new ColumnImpl(
                    row,
                    new DividerImpl(),
                    bottom );

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
        RowImpl row = JavaUtil.make( () -> {
            var one = new LeafImpl("1");
            one.setWeight( .3 );
            var two = new LeafImpl( "2" );
            two.setWeight( .3 );
            // TODO validate names.
            var three = new LeafImpl( "3" );
            three.setWeight( .0 );

            return new RowImpl(
                    one,
                    new DividerImpl(),
                    two,
                    new DividerImpl(),
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

//    @Test
//    public void propModelTest() throws Exception
//    {
//        Holder<PropertyChangeEvent> pceh = new Holder<>();
//
//        final var mspl = new MultiSplitLayout();
//
//        mspl.addPropertyChangeListener( ce -> pceh.set( ce ) );
//        assertNull( pceh.get() );
//
//        mspl.setModel( new LeafImpl( "test" ) );
//
//        assertNotNull( pceh.get() );
//
//        assertEquals(
//                "model",
//                pceh.get().getPropertyName() );
//        assertEquals(
//                "test",
//                LeafImpl.class.cast(pceh.get().getNewValue() ).getName() );
//        assertEquals(
//                "default",
//                LeafImpl.class.cast(pceh.get().getOldValue() ).getName() );
//        assertEquals(
//                mspl,
//                pceh.get().getSource() );
//    }


    @Test
    public void Split_validate_lessThanTwoNodes() throws Exception
    {
        final int count = 0;

        NodeImpl[] nodes = new NodeImpl[]
        {
            new LeafImpl( "one" ),
        };

        SplitImpl split = new ColumnImpl( nodes );

        try {
            split.validate( new HashSet<>() );
        }
        catch ( InvalidLayoutException e )
        {
            assertEquals( split, e.getNode() );
        }
    }

    @Test
    public void Split_children_weightExceeds100() throws Exception
    {
        NodeImpl[] nodes = new NodeImpl[]
        {
            new LeafImpl( "one" ).weight( .8 ),
            new DividerImpl(),
            new LeafImpl( "two" ).weight( .9 )
        };

        SplitImpl split = new ColumnImpl( nodes );

        try {
            split.validate( new HashSet<>() );
        }
        catch ( InvalidLayoutException e )
        {
            assertEquals( split, e.getNode() );
        }
    }

    @Test
    public void Split_children_duplicateName() throws Exception
    {
        NodeImpl[] nodes = new NodeImpl[]
        {
            new LeafImpl( "one" ).weight( .8 ),
            new DividerImpl(),
            new LeafImpl( "one" ).weight( .0 )
        };

        SplitImpl split = new RowImpl( nodes );

        try {
            split.validate( new HashSet<>() );
        }
        catch ( InvalidLayoutException e )
        {
            assertEquals( nodes[2], e.getNode() );
        }
    }

    @Test
    public void Split_children_0() throws Exception
    {
        final int count = 0;

        NodeImpl[] nodes = new NodeImpl[]
        {
            new LeafImpl( "one" ),
        };

        SplitImpl split = new RowImpl( nodes );

        assertEquals( nodes.length, split.getChildren().size() );
        assertEquals( nodes.length-count, split.size() );
    }

    @Test
    public void Split_children_1() throws Exception
    {
        final int count = 1;

        NodeImpl[] nodes = new NodeImpl[]
        {
            new LeafImpl( "one" ),
            new DividerImpl(),
            new LeafImpl( "two" )
        };

        SplitImpl split = new ColumnImpl( nodes );

        assertEquals( nodes.length, split.getChildren().size() );
        assertEquals( nodes.length-count, split.size() );
    }

    @Test
    public void Split_children_2() throws Exception
    {
        int count = 2;

        NodeImpl[] nodes = new NodeImpl[]
        {
            new LeafImpl( "one" ),
            new DividerImpl(),
            new LeafImpl( "two" ),
            new DividerImpl(),
            new LeafImpl( "three" )
        };

        SplitImpl split = new RowImpl( nodes );

        assertEquals( nodes.length, split.getChildren().size() );
        assertEquals( nodes.length-count, split.size() );
    }

    @Test
    public void new_names() throws Exception
    {
        var model = new Column(
                new Row(
                        .5,
                        new Leaf( 0.5, "left" ),
                        new Leaf( 0.5, "right" ) ),
                new Leaf(
                        .5,
                        "bottom" ) );

        assertNotNull( model );
    }

    @Test
    public void new_node_validation() throws Exception
    {
        try
        {
            new Leaf( -1.0, "negative" );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
        }
    }

    @Test
    public void new_node_split_validation() throws Exception
    {
        try
        {
            new Row(
                    .5,
                    new Leaf( 0.6, "left" ),
                    new Leaf( 0.5, "right" ) );
            fail();
        }
        catch ( IllegalArgumentException e )
        {
        }
    }
}
