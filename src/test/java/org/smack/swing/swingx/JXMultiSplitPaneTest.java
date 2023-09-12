/*
 * Copyright © 2023 Michael G. Binz
 */
package org.smack.swing.swingx;

import javax.swing.JLabel;

import org.junit.jupiter.api.Test;
import org.smack.swing.swingx.MultiSplitLayout.ColumnImpl;
import org.smack.swing.swingx.MultiSplitLayout.DividerImpl;
import org.smack.swing.swingx.MultiSplitLayout.LeafImpl;
import org.smack.swing.swingx.MultiSplitLayout.RowImpl;
import org.smack.swing.swingx.MultiSplitLayout.SplitImpl;
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
