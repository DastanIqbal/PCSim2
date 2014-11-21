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

public class PC2StatusCellRenderer extends DefaultTableCellRenderer{

	static final long serialVersionUID = 1;
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,  
			boolean isSelected, boolean hasFocus, int row, int column) {
		Component cell = super.getTableCellRendererComponent(table, value, 
				isSelected, hasFocus, row, column);
		if (value instanceof PC2RegistrarStatus) {
			PC2RegistrarStatus status = (PC2RegistrarStatus)value;
			if (status == PC2RegistrarStatus.REGISTERED) {
				cell.setBackground(Color.GREEN);
			}
			else if (status == PC2RegistrarStatus.ATTEMPTING) {
				cell.setBackground(Color.YELLOW);
			}
			else if (status == PC2RegistrarStatus.UNREGISTERED) {
				cell.setBackground(Color.ORANGE);
			}
			else if (status == PC2RegistrarStatus.INACTIVE) {
				cell.setBackground(Color.GRAY);
			}
			else if (status == PC2RegistrarStatus.DENIED) {
				cell.setBackground(Color.RED);
			}
			// Could also customize the FONT and Foreground if we
			// wanted to hear
			
			
		}
		
		return cell;
	}
}
