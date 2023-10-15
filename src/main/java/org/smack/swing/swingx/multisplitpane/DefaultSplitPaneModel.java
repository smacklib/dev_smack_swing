/*
 * $Id: 19800350656bd23f120c4fe8d2afcd89d69584ca $
 *
 * Copyright 2006 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.smack.swing.swingx.multisplitpane;

import org.smack.swing.swingx.MultiSplitLayout.ColumnImpl;
import org.smack.swing.swingx.MultiSplitLayout.DividerImpl;
import org.smack.swing.swingx.MultiSplitLayout.LeafImpl;
import org.smack.swing.swingx.MultiSplitLayout.RowImpl;
import org.smack.swing.swingx.MultiSplitLayout.SplitImpl;
/**
 * A simplified SplitPaneLayout for common split pane needs. A common multi splitpane
 * need is:
 *
 * +-----------+-----------+
 * |           |           |
 * |           +-----------+
 * |           |           |
 * +-----------+-----------+
 *
 * @author rbair
 */
public class DefaultSplitPaneModel extends RowImpl {
    public static final String LEFT = "left";
    public static final String TOP = "top";
    public static final String BOTTOM = "bottom";

    /** Creates a new instance of DefaultSplitPaneLayout */
    public DefaultSplitPaneModel() {
        //setRowLayout( true );
        //SplitImpl row = new RowImpl();
        SplitImpl col = new ColumnImpl();
        //col.setRowLayout(false);
        setChildren(new LeafImpl(LEFT), new DividerImpl(), col);
        col.setChildren(new LeafImpl(TOP), new DividerImpl(), new LeafImpl(BOTTOM));
    }
}
