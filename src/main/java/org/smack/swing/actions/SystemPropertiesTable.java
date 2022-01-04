/* $Id$
 *
 * MACK
 *
 * Released under Gnu Public License
 * Copyright (c) 2003-2006 Michael G. Binz
 */
package org.smack.swing.actions;

import java.util.Enumeration;

import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;



/**
 * A table model serving the Java system properties.  Used by
 * <code>ActAbout</code>.
 *
 * @see de.michab.mack.actions.ActAbout
 * @author Michael G. Binz
 */
final class SystemPropertiesTable implements TableModel
{
  /**
   * The model actually holding our data.  Most operations are delegated to
   * this.
   */
  private final DefaultTableModel _dataDelegateModel =
    new DefaultTableModel();



  /**
   * Creates an initialised instance.
   */
  public SystemPropertiesTable()
  {
    int COLUMN_COUNT = 2;

    _dataDelegateModel.setColumnIdentifiers(
        new Object[]{ "Name", "Value" } );

    java.util.Properties sprops = System.getProperties();
    Enumeration names = sprops.propertyNames();

    String[] newRow = new String[ COLUMN_COUNT ];

    while ( names.hasMoreElements() )
    {
      newRow[0] = (String)names.nextElement();
      newRow[1] = sprops.getProperty( newRow[0] );
      _dataDelegateModel.addRow( newRow );
    }
  }



  /*
   * Inherit Javadoc.
   */
  @Override
public int getRowCount()
  {
    return _dataDelegateModel.getRowCount();
  }



  /*
   * Inherit Javadoc.
   */
  @Override
public int getColumnCount()
  {
    return _dataDelegateModel.getColumnCount();
  }



  /*
   * Inherit Javadoc.
   */
  @Override
public String getColumnName(int columnIndex)
  {
    return _dataDelegateModel.getColumnName( columnIndex );
  }



  /*
   * Inherit Javadoc.
   */
  @Override
public Class getColumnClass(int columnIndex)
  {
    return _dataDelegateModel.getColumnClass( columnIndex );
  }



  /**
   * Returns whether a cell is editable.  A model of this type is generally
   * <i>not</i> editable.
   *
   * @param rowIndex The index of the row.
   * @param columnIndex The index of the column.
   * @return Always <code>false</code>.
   */
  @Override
public boolean isCellEditable(int rowIndex, int columnIndex)
  {
    return false;
  }



  /*
   * Inherit Javadoc.
   */
  @Override
public Object getValueAt(int rowIndex, int columnIndex)
  {
    return _dataDelegateModel.getValueAt( rowIndex, columnIndex );
  }



  /**
   * Sets a cell's value.  Part of the <code>TableModel</code>
   * interface.  This is an empty operation on this class.
   *
   * @param rowIndex The cell's row.
   * @param columnIndex The cell's column.
   * @param aValue The cell's new content.
   */
  @Override
public void setValueAt(Object aValue, int rowIndex, int columnIndex)
  {
    // No-op -- Set neither needed nor supported.
    return;
  }


  /*
   * Inherit Javadoc.
   */
  @Override
public void addTableModelListener(TableModelListener l)
  {
    _dataDelegateModel.addTableModelListener( l );
  }



  /*
   * Inherit Javadoc.
   */
  @Override
public void removeTableModelListener(TableModelListener l)
  {
    _dataDelegateModel.removeTableModelListener( l );
  }
}
