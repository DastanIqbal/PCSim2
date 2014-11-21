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

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

public class PC2ImagePane extends Canvas {

	static final long serialVersionUID = 1;
	protected static final int WIDTH = PC2ControlPane.WIDTH;
	protected static final int HEIGHT = 120;
	protected Image logo = null;

	public PC2ImagePane() {
		try {
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			URL url = PC2ImagePane.class.getResource("images/pcsim2.ico");
			logo = toolkit.getImage(url);
		}
		catch (Exception e) {
			logo = null;
			System.out.println("Problem with the Toolkit: no images loaded!!!");
			e.printStackTrace();
		}
	}
	
//	public void paintComponent(Graphics g) {
//		setBackground(g);
//	}
//	
//	protected void setBackground(Graphics g) {
//		if (logo != null
//
//			&& logo.getWidth(this) != -1
//			&& logo.getHeight(this) != -1) {
//			int widthImage = logo.getWidth(this);
//			int heightImage = logo.getHeight(this);
//
////			int nbImagesX = getSize().width / widthImage + 1;
////			int nbImagesY = getSize().height / heightImage + 1;
//
//			// we don't draw the image above the top
//			g.drawImage(logo, 25, 40, this);
//			
//		}
//		else {
//
//			g.setColor(Color.white);
//			g.fillRect(25, 40, getSize().width, getSize().height);
//		}
//	}
	@Override
	public void paint(Graphics g) {
		g.drawImage(logo,25,40,this);
	}
}
