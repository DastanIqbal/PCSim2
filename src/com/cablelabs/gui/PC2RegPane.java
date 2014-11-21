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

import javax.swing.JScrollPane;
import javax.swing.JTextPane;

public class PC2RegPane extends JTextPane {

	static final long serialVersionUID = 1;
	private JScrollPane regScrollPane = null;
	
	public PC2RegPane() {
	
	 //textPane.setText(text);
	 regScrollPane = new JScrollPane(this);
//	    tp.setText(markup);
	    // Create an AttributeSet with which to change color and font.
//	    SimpleAttributeSet attrs = new SimpleAttributeSet( );
//	    StyleConstants.setForeground(attrs, Color.blue);
//	    StyleConstants.setFontFamily(attrs, "Serif");
//	    // Apply the AttributeSet to a few blocks of text.
//	    StyledDocument sdoc = tp.getStyledDocument( );
//	    sdoc.setCharacterAttributes(14, 29, attrs, false);
//	    sdoc.setCharacterAttributes(51, 7, attrs, false);
//	    sdoc.setCharacterAttributes(78, 28, attrs, false);
//	    sdoc.setCharacterAttributes(114, 7, attrs, false);
//	    JScrollPane scroll3 = new JScrollPane(tp);
	}

	protected JScrollPane getScrollPane() {
		return regScrollPane;
	}
}
