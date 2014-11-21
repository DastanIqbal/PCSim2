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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class PC2LicenseScreen extends JFrame implements Runnable, ActionListener, ItemListener, ChangeListener{
	
	static final long serialVersionUID = 1;
//	private boolean accepted = false;
//	private File agreement = new File("./config/.accepted");
	private JScrollPane sp = null;
	private final String licenseFile = "../docs/PCSim2 License.txt";
	
	public PC2LicenseScreen() {
		super();
		setTitle("PCSim2 License Agreement");
//		if (agreement.exists() && agreement.isFile())
//			accepted = true;
	}
	
	public boolean isAccepted() {
		return true;
	}
	
	// A simple little method to show a title screen in the center of the screen for
	// the amount of time given in the constructor
	public void showLicense( ) {
		JPanel content = (JPanel)getContentPane( );
		content.setBackground(Color.white);
		
		// Set the window's bounds, centering the window.
		int width = 600;
		int height =400;
		Dimension screen = Toolkit.getDefaultToolkit( ).getScreenSize( );
		int x = (screen.width-width)/2;
		int y = (screen.height-height)/2;
		setBounds(x,y,width,height);
		
		// Build the splash screen.
		JTextArea lic = new JTextArea();
		lic.setLineWrap(true);
		lic.setWrapStyleWord(false);
		//Dimension taSize = new Dimension(width-10, height-100);
		//lic.setSize(taSize);
		loadLicense(lic);
		sp = new JScrollPane(lic);
		content.add(sp, BorderLayout.CENTER);
		
		JPanel panel = new JPanel();
		// Add accepted  buttons only if licenses hasn't
		// been accepted to before. This allows the HELP
		// menu button to eventually display the license 
		// agreement using the same class.
//		if (!accepted) {
//			JButton agree = new JButton("Accept");
//			JButton decline = new JButton("Decline");
//			
//			agree.addActionListener(this);
//			agree.addItemListener(this);
//			agree.addChangeListener(this);
//			
//			decline.addActionListener(this);
//			decline.addItemListener(this);
//			decline.addChangeListener(this);
//			
//			panel.add(agree, BorderLayout.WEST);
//			panel.add(decline, BorderLayout.EAST);
//					
//			content.add(panel, BorderLayout.SOUTH);
//			content.getRootPane().setDefaultButton(decline);
//		}
//		else {
			JButton done = new JButton("OK");
			
			done.addActionListener(this);
			done.addItemListener(this);
			done.addChangeListener(this);
			panel.add(done, BorderLayout.SOUTH);
			content.add(panel, BorderLayout.SOUTH);
			content.getRootPane().setDefaultButton(done);
//		}
				
		Image img = Toolkit.getDefaultToolkit().getImage(
				ClassLoader.getSystemResource("images/pcsim2.jpg"));
		setIconImage( img );
		// Display it.
		setVisible(true);
	}
	
	private void loadLicense(JTextArea ta) {
		File f = new File (licenseFile);
		if (f.exists() && f.canRead() && f.isFile()) {
			try {
				FileInputStream in = new FileInputStream(f);
				int len = 0;
				int l = in.available();
				byte[] ch = new byte[l];                        
				len = in.read(ch,0,l);              
				if (len > 0) {
					String strUnicode = new String(ch,"UTF-8");
					ta.append(strUnicode);
					
					in.close();
					ta.setEditable(false);
				}
				
			}
			catch (FileNotFoundException fnf) {
				// TODO warn user couldn't find file
				System.out.println(licenseFile + " could not be found.");
				System.exit(-1);
			}
			catch (IOException io) {
				// TODO warn user of ioexception
				System.out.println(licenseFile + " could not be read.");
				System.exit(-1);
			}
			catch (IllegalArgumentException ia) {
				System.out.println("Encountered illegal argument when attempting to load license.");
				System.exit(-1);
			}
		}
		else {
			System.out.println(licenseFile + " could not be found or read.");
			System.exit(-1);
		}
	}
	@Override
	public void actionPerformed(ActionEvent ev) {
//		String choice = ev.getActionCommand();
//		if (choice.equals("Accept")) {
//			if (!agreement.exists() || !agreement.isFile()) {
//			    try {
//			    	if (agreement.createNewFile()) {
//			    		System.out.println("License Accepted.");
//			    		// TODO if java allows the file to be
//			    		// set has hidden in the future. It would
//			    		// be nice to have this file be hidden.
//			    	}
//			    }
//			    catch (IOException io) {
//			    	System.err.println("License locally accepted.");
//			    }
//			    finally {
//			    	 accepted = true;
//			    }
//			   
//			}
//			accepted = true;
//		}
//		else if (choice.equals("Decline")) {
//			accepted = false;
//		}
//		else if (choice.equals("Done")) {
//			// do nothing
//		}
		setVisible(false);
			
	}
	
	@Override
	public void itemStateChanged(ItemEvent ev) {
//		System.out.println("License StateChanged " + ev);
	}
	
	@Override
	public void run() {
		boolean complete = false;
		if (sp != null) {
			// Set the vertical scroll bar to the top of the
			// text.
			JScrollBar v = sp.getVerticalScrollBar();
			v.getModel().setValue(0);
		}

		while (!complete) {
		
			try {
				Thread.sleep(100);
				if (!isVisible())
					complete = true;
			}
			catch (Exception e) {
				complete = true;
			}
		}
	}
	@Override
	public void stateChanged(ChangeEvent ev) {
		//System.out.println("LicenseScreen ChangeEvent " + ev.getSource());
	}
	
	public static void main(String[] args) {
		// Throw a nice little title page up on the screen first.
		PC2LicenseScreen splash = new PC2LicenseScreen();
		// Normally, we'd call splash.showSplash( ) and get on with the program.
		// But, since this is only a test...
		//splash.showSplashAndExit( );
		splash.showLicense();
		splash.run();
		System.exit(0);
	}
	
}
