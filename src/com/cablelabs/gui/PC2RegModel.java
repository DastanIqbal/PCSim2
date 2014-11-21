/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.gui;

import javax.swing.table.AbstractTableModel;

public class PC2RegModel extends AbstractTableModel {

	static final long serialVersionUID = 1;
	protected final String status = "Status";
	protected final String label = "Label";
	protected final String ipAddr = "IP Address";
	protected String colHdrs[] = new String[] {
			    status, label, ipAddr};
	
	Class<?> types[] = new Class[] { 
			    PC2RegistrarStatus.class, String.class, String.class };
			      
	Object data[][];

	public PC2RegModel() { 
		
	}

	// Implement the methods of the TableModel interface we're interested
	// in. Only getRowCount( ), getColumnCount( ), and getValueAt( ) are
	// required. The other methods tailor the look of the table.
	@Override
	public int getRowCount( ) { return data.length; }
	@Override
	public int getColumnCount( ) { return colHdrs.length; }
	@Override
	public String getColumnName(int c) { return colHdrs[c]; }
	@Override
	public Class<?> getColumnClass(int c) { return types[c]; }
	@Override
	public Object getValueAt(int r, int c) { return data[r][c]; }
  

}
