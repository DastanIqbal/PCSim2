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

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

public class PC2ResultCellRenderer extends DefaultTableCellRenderer {

	static final long serialVersionUID = 1;
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,  
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component cell = super.getTableCellRendererComponent(table, value, 
				isSelected, hasFocus, row, column);
		if (value instanceof PC2Result) {
			PC2Result status = (PC2Result)value;
			if (status == PC2Result.PASSED) {
				cell.setBackground(Color.GREEN);
			}
			else if (status == PC2Result.TESTING) {
				cell.setBackground(Color.CYAN);
			}
			else if (status == PC2Result.FAILED) {
				cell.setBackground(Color.RED);
			}
			// Could also customize the FONT and Foreground if we
			// wanted to hear
			
			
		}
		return cell;
	}
}
