/*
 * Copyright © 2023 Michael G. Binz
 */
package org.smack.swing.swingx.multisplitpane;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.awt.Container;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.text.ParseException;

import javax.swing.JSplitPane;

import org.junit.jupiter.api.Test;
import org.smack.swing.swingx.multisplitpane.MultiSplitLayout.Column;
import org.smack.swing.swingx.multisplitpane.MultiSplitLayout.Leaf;
import org.smack.swing.swingx.multisplitpane.MultiSplitLayout.Row;
import org.smack.swing.swingx.multisplitpane.MultiSplitLayout.Split;
import org.smack.swing.test.MockContainer;
import org.smack.util.Holder;
import org.smack.util.StringUtil;

public class MultiSplitLayoutTest
{
    /**
     * Support operation.
     */
    private static String normalizeLines( String lines )
    {
        String[] splitLines = lines.split( StringUtil.EOL );

        for ( int i = 0 ; i < splitLines.length ; i++ )
            splitLines[i] = splitLines[i].trim();

        return StringUtil.concatenate( " ", splitLines );
    }

    @Test
    public void normalizeLinesTest() throws Exception
    {
        var actual = "Row( weight=0.5,\n"
                + "    Leaf( weight=0.5, name=left ),\n"
                + "    Leaf( weight=0.5, name=right ) )";

        var expected =
                "Row( weight=0.5, Leaf( weight=0.5, name=left ), Leaf( weight=0.5, name=right ) )";

        assertEquals( expected, normalizeLines( actual ) );
    }

    @Test
    public void Model_toString_Leaf() throws Exception
    {
        Leaf left = new Leaf( .5, "left" );
        assertEquals( "Leaf( weight=0.5, name=left )", left.toString() );
    }

    @Test
    public void Model_toString_Row() throws Exception
    {
        Leaf left = new Leaf( .5, "left" );
        assertEquals( "Leaf( weight=0.5, name=left )", left.toString() );
        Leaf right = new Leaf( .5, "right" );
        assertEquals( "Leaf( weight=0.5, name=right )", right.toString() );

        Row row = new Row( .5, left, right );
        assertEquals(
                "Row( weight=0.5, Leaf( weight=0.5, name=left ), Leaf( weight=0.5, name=right ) )",
                normalizeLines( row.toString() ) );
    }

    @Test
    public void Model_toString_Column() throws Exception
    {
        Leaf left = new Leaf( .5, "left" );
        assertEquals( "Leaf( weight=0.5, name=left )", left.toString() );
        Leaf right = new Leaf( .5, "right" );
        assertEquals( "Leaf( weight=0.5, name=right )", right.toString() );

        Column col = new Column( .5, left, right );
        assertEquals(
                "Column( weight=0.5, Leaf( weight=0.5, name=left ), Leaf( weight=0.5, name=right ) )",
                normalizeLines( col.toString() ) );
    }


    @Test
    public void ModelImpl_toString_LeafImpl() throws Exception
    {
        MultiSplitLayout mspl = new MultiSplitLayout();

        var leaf = mspl.new LeafImpl( "middle" );

        assertEquals(
                "LeafImpl( name=\"middle\""
                + " weight=0,000000"
                + " bounds=java.awt.Rectangle[x=0,y=0,width=0,height=0]"
                + " )", leaf.toString() );
    }

    @Test
    public void ModelImpl_Err_duplicateLeaf() throws Exception
    {
        try
        {
            MultiSplitLayout mspl = new MultiSplitLayout();

            mspl.new LeafImpl( "name" );
            mspl.new LeafImpl( "name" );

            fail();
        }
        catch ( IllegalArgumentException e )
        {
            assertEquals( "Duplicate leaf: name", e.getMessage() );
        }
    }

    @Test
    public void basicTest() throws Exception
    {
        ///////////////////
        // left // right //
        ///////////////////
        //  bottom       //
        ///////////////////

        Split row = new Row(
                new Leaf( .5, "left" ),
                new Leaf( .5, "right" ));


        Column column = new Column(
                row,
                new Leaf( .0, "bottom" ) );

        var mspl = new MultiSplitLayout( column );
        mspl.setDividerSize( 10 );

        Container container = new MockContainer( 200, 100 );

        mspl.layoutContainer( container );

        assertEquals( 10,  mspl.getDividerSize() );

        {
            var d = mspl.getLeafForName( "left" ).bounds();
            assertEquals(
                new Rectangle( 0, 0, 95, 45 ),
                d );
        }
        {
            var d = mspl.getLeafForName( "right" ).bounds();
            assertEquals(
                new Rectangle( 105, 0, 95, 45 ),
                d );
        }
        {
            var d = mspl.getLeafForName( "left" ).parent().bounds();
            assertEquals(
                new Rectangle( 0, 0, 200, 45 ),
                d );
        }
        {
            var d = mspl.getLeafForName( "bottom" ).bounds();
            assertEquals(
                new Rectangle( 0, 55, 200, 45 ),
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

        Split row = new Row(
                new Leaf( .5, "left" ),
                new Leaf( .5, "right" ));


        Column column = new Column(
                row,
                new Leaf( .0, "bottom" ) );

        var mspl = new MultiSplitLayout( column );
        mspl.setDividerSize( 10 );

        final int WIDTH = 201;
        final int HEIGHT = 101;
        Container container = new MockContainer( WIDTH, HEIGHT );

        mspl.layoutContainer( container );

        assertEquals( 10,  mspl.getDividerSize() );

        {
            var d = mspl.getLeafForName( "left" ).bounds();
            assertEquals(
                new Rectangle( 0, 0, 96, 46 ),
                d );
        }
        {
            var d = mspl.getLeafForName( "right" ).bounds();
            assertEquals(
                new Rectangle( 106, 0, 95, 46 ),
                d );
        }
        {
            var d = mspl.getLeafForName( "left" ).parent().bounds();
            assertEquals(
                new Rectangle( 0, 0, 201, 46 ),
                d );
        }
        {
            var d = mspl.getLeafForName( "bottom" ).bounds();
            assertEquals(
                new Rectangle( 0, 56, 201, 45 ),
                d );
        }
        {
            var left = mspl.getLeafForName( "left" ).bounds();
            var right = mspl.getLeafForName( "right" ).bounds();

            assertEquals(
                    WIDTH,
                    left.getBounds().width +
                    mspl.getDividerSize() +
                    right.getBounds().width );
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
    public void Split_children_weightExceeds100() throws Exception
    {
        try {
            new Column(
                    new Leaf( 0.8, "one" ),
                    new Leaf( 0.9, "two" ) );
        }
        catch ( IllegalArgumentException e )
        {
            assertTrue( e.getMessage().startsWith( "Weights > 1.0:" ) );
        }
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

    @Test
    public void parseTest() throws Exception
    {
        var inputModel =
                new Column(
                        .0,
                        new Row(
                                .0,
                                new Leaf( .5, "left" ),
                                new Leaf( .5, "right" ) ),
                        new Leaf( .5, "bottom" ) );

        var modelString =
                inputModel.toString();

        var modelFromString =
                MultiSplitLayout.parseModel( modelString );

        assertEquals(
                inputModel.toString(),
                modelFromString.toString() );
    }

    @Test
    public void parseTestFromString() throws Exception
    {
        var modelString =
                "Column( weight=0.0,\n"
                        + "        Row( weight=0.0,\n"
                        + "            Leaf( weight=0.5, name=left ),\n"
                        + "            Leaf( weight=0.0, name=right ) ),\n"
                        + "        Leaf( weight=0.5, name=bottom ) )";
        var modelFromString =
                MultiSplitLayout.parseModel( modelString );

        assertEquals(
                normalizeLines( modelString ),
                normalizeLines( modelFromString.toString() ) );
    }

    private String expectedMessage( ParseException pe )
    {
        var msg = pe.getMessage();

        final var prefix = "Expected: ";

        assertTrue( msg.startsWith( prefix ) );

        return msg.substring( prefix.length() );
    }

    @Test
    public void parseError_NoTopSplit() throws Exception
    {
        try
        {
        var modelString =
                "Leaf( weight=0.5, name=left )";

        MultiSplitLayout.parseModel( modelString );

        fail();
        }
        catch (ParseException e) {
            assertEquals(
                    "Row|Column",
                    expectedMessage( e ));
        }
    }

    @Test
    public void parseError_TopSplitIncomplete_1() throws Exception
    {
        try
        {
            var modelString =
                    "Row(";

            MultiSplitLayout.parseModel( modelString );

            fail();
        }
        catch (ParseException e) {
            assertEquals(
                    "TT_WORD",
                    expectedMessage( e ));
        }
    }

    @Test
    public void parseError_TopSplitIncomplete_2() throws Exception
    {
        try
        {
            var modelString =
                    "Row( 0.0";

            MultiSplitLayout.parseModel( modelString );

            fail();
        }
        catch (ParseException e) {
            assertEquals(
                    "TT_WORD",
                    expectedMessage( e ));
        }
    }

    @Test
    public void parseError_TopSplitIncomplete_3() throws Exception
    {
        try
        {
            var modelString =
                    "Row( wrong";

            MultiSplitLayout.parseModel( modelString );

            fail();
        }
        catch (ParseException e) {
            assertEquals(
                    "weight",
                    expectedMessage( e ));
        }
    }

    @Test
    public void parseError_TopSplitIncomplete_5() throws Exception
    {
        try
        {
            var modelString =
                    "Row( weight =";

            MultiSplitLayout.parseModel( modelString );

            fail();
        }
        catch (ParseException e) {
            assertEquals(
                    "TT_NUMBER",
                    expectedMessage( e ));
        }
    }
}
