/*
 * Copyright © 2023 Michael G. Binz
 */
package org.smack.swing.swingx;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Container;
import java.awt.Rectangle;

import org.junit.jupiter.api.Test;
import org.smack.swing.swingx.MultiSplitLayout.ColSplit;
import org.smack.swing.swingx.MultiSplitLayout.Divider;
import org.smack.swing.swingx.MultiSplitLayout.Leaf;
import org.smack.swing.swingx.MultiSplitLayout.RowSplit;
import org.smack.swing.swingx.MultiSplitLayout.Split;
import org.smack.swing.test.MockContainer;
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

        Container container = new MockContainer( 200, 100 );

        mspl.layoutContainer( container );

        {
            var d = mspl.getNodeForName( "left" ).getBounds();
            assertEquals(
                new Rectangle( 0, 0, 100, 50 ),
                d );
        }
        {
            var d = mspl.getNodeForName( "right" ).getBounds();
            assertEquals(
                new Rectangle( 100, 0, 100, 50 ),
                d );
        }
        {
            var d = mspl.getNodeForName( "bottom" ).getBounds();
            assertEquals(
                new Rectangle( 0, 50, 200, 50 ),
                d );
        }
    }
}
