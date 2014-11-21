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

import javax.swing.JTable;

public class PC2Table extends JTable {

	static final long serialVersionUID = 1;
	public PC2Table() {
		
	}
	
	@Override
	public boolean isCellEditable(int row, int col) {
		return false;
	}
}
