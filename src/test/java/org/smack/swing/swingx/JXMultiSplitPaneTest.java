/*
 * Copyright © 2023 Michael G. Binz
 */
package org.smack.swing.swingx;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;
import org.smack.swing.swingx.MultiSplitLayout.Column;
import org.smack.swing.swingx.MultiSplitLayout.Divider;
import org.smack.swing.swingx.MultiSplitLayout.Leaf;
import org.smack.swing.swingx.MultiSplitLayout.Row;
import org.smack.swing.swingx.MultiSplitLayout.Split;
import org.smack.util.JavaUtil;

public class JXMultiSplitPaneTest
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

            return new Row(
                    left,
                    new Divider(),
                    right );
        });

        Split column = JavaUtil.make( () -> {
            var bottom = new Leaf( "bottom" );
            bottom.setWeight( .5 );
            row.setWeight( .5 );

            var result = new Column(
                    row,
                    new Divider(),
                    bottom );
            result.setRowLayout( false );

            return result;
        });

        var mspl = new MultiSplitLayout( column );

        JXMultiSplitPane msp = new JXMultiSplitPane( mspl );

        JLabel leftComponent = new JLabel( "leftComponent" );
        JLabel rightComponent = new JLabel( "rightComponent" );
        JLabel bottomComponent = new JLabel( "bottomComponent" );

        msp.add( leftComponent, "left" );
        msp.add( rightComponent, "right" );
        msp.add( bottomComponent, "bottom" );
        msp.add( bottomComponent, "micbinz" );

    }

}
