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
import java.awt.Font;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

import javax.swing.JScrollPane;

public class PC2Console extends ByteArrayOutputStream {

	public static final Font FATAL = new Font("Serif", Font.BOLD+Font.ITALIC, 14);
	public static final Font ERROR = new Font("Serif", Font.BOLD, 12);
	public static final Font WARN = new Font("Dialog", Font.BOLD+Font.ITALIC, 12); //new Font("Sans-serif", Font.ITALIC, 12);
	public static final Font INFO = new Font("Serif", Font.PLAIN, 12);
	public static final Font DEBUG = new Font("Serif", Font.PLAIN, 11);
	public static final Font TRACE = new Font("Serif", Font.PLAIN, 11);
	public static final Font MSG = new Font("Serif", Font.BOLD, 12);
	public static final Font VERIFY = new Font("Dialog", Font.BOLD, 12); 
	public static final Font PROMPT = new Font("Monospaced", Font.BOLD+Font.ITALIC, 20);
	public static final Color DK_YELLOW  = new Color(206,206,0);	// new Color(226, 214, 10);
	public static final Color RED  = Color.RED;
	public static final Color NORMAL = Color.BLACK;
	public static final Color BLUE = Color.BLUE;
	public static final Color GREEN = new Color(0, 128, 0); // Color.GREEN;
	public static final Color ORANGE = new Color(255, 128, 0); // Color.ORANGE;
	public static final Color GRAY = Color.GRAY;
	public static final Color DK_GRAY = Color.DARK_GRAY;
	
	protected PC2TextPane textPane = null;
	protected JScrollPane sp = null;	
	protected boolean lockScreen = false;
	protected StringBuffer lockBuffer = null;
	
	public PC2Console(JScrollPane scroll, PC2TextPane pane) {
		super();
		this.sp = scroll;
		this.textPane = pane;
		pane.setOwner(this);
	}

	private void appendBuffer(String s) {
		synchronized (lockBuffer) { 
			if (lockScreen && lockBuffer != null) {
				if (lockBuffer.length() < 1000000000) {
					lockBuffer.append(s);
				}
				else {
					lockBuffer.delete(0, s.length());
					lockBuffer.append(s);
				}
			}
		}
	}

	public synchronized void append(String s) throws IllegalArgumentException {
		if (lockScreen)
			appendBuffer(s);
		else {
			boolean error = false;
			boolean fatal = false;
			boolean warn = false;
			boolean info = false;
			boolean debug = false;
			boolean trace = false;
			if (s != null) {
				if (s.length() >= 5) {

					String level = s.substring(0,5);

					if (level.equals("DEBUG"))
						debug = true;
					else if (level.equals("INFO ")) 
						info = true;
					else if (level.equals("WARN ")) 
						warn = true;
					else if (level.equals("ERROR")) 
						error = true;
					else if (level.equals("TRACE")) 
						trace = true;
					else if (level.equals("FATAL")) 
						fatal = true;


					if (fatal) 
						textPane.append(s, FATAL, RED);
					else if (error) {
						if (s.contains("ERROR  VERIFY FAILED"))
							textPane.append(s, VERIFY, RED);
						else
							textPane.append(s, ERROR, RED);
					}
					else if (info) {
						if (s.startsWith("INFO  >>>>> RX:")) {
							textPane.append(s, MSG, GREEN);
						}
						else if (s.startsWith("INFO  <<<<< TX:")) {
							textPane.append(s, MSG, ORANGE);
						}
						else if (s.startsWith("INFO   USER:"))
							textPane.append(s, PROMPT, BLUE);
						else if (s.startsWith("INFO  Test ")) {
							if (s.contains("Passed."))
								textPane.append(s, FATAL, GREEN);
							else if (s.contains("Failed."))
								appendFailedTestResults(s);
							//textPane.append(s, FATAL, RED);
						}
						else if (s.startsWith("INFO  Commencing test ")) {
							textPane.append(s, FATAL, GREEN);
						}
						else if (s.startsWith("INFO  \n\tVendor: ")) {
							textPane.append(s, MSG, BLUE);
						}
						else if (s.contains("INFO  VERIFY PASSED")) 
							textPane.append(s, VERIFY, BLUE);

						else
							textPane.append(s, INFO, NORMAL);
					}
					else if (warn)
						textPane.append(s, WARN, DK_YELLOW);
					else if (debug)
						textPane.append(s, DEBUG, DK_GRAY);
					else if (trace)
						textPane.append(s, TRACE, GRAY);
					else
						textPane.append(s, INFO, NORMAL);
				}
				else
					textPane.append(s, INFO, NORMAL);
				// Lastly update the scroll position to appear at the bottom
//				JScrollBar tmpbar = sp.getVerticalScrollBar();
//				int extent  = tmpbar.getVisibleAmount();
//				int minimum = tmpbar.getMinimum();
//				int maximum = tmpbar.getMaximum();

//				if (maximum > extent) {
//				tmpbar.setValues(maximum-extent, extent, minimum,
//				maximum);
//				}

//				JScrollBar vsb = sp.getVerticalScrollBar();
//				if (vsb != null) {
//				int max = vsb.getMaximum();
//				if (max > 0)
//				vsb.setMaximum(max);
//				}
			}
			else {
				throw new IllegalArgumentException("LogViewer doesn't allow a null string for an argument.");
			}
		}
	}

	private void appendFailedTestResults(String s) {
		if (lockScreen) 
			appendBuffer(s);
		else {
			// For failures we are going to display part of the text
			// red and partially in green if any of the requirements
			// passed.
			StringTokenizer st = new StringTokenizer(s, "\n");
			while (st.hasMoreTokens()) {
				String temp = st.nextToken();
				if (temp.contains("Passed"))
					textPane.append((temp+"\n"), FATAL, GREEN);
				else
					textPane.append((temp+"\n"), FATAL, RED);
			}
		}
	}
	
	public void lock() {
		lockScreen = true;
		lockBuffer = new StringBuffer();
	}
	
	public void print(String s) {
		append(s);
	}
	public void println(String s) {
		append(s);
	}

	/* Override Ancestor method */
	@Override
	public void write(byte b[]) throws IOException {
		String str = new String(b);
		if (lockScreen)
			appendBuffer(str);
		else
			println(str);
		
	}

	/* Override Ancestor method */
	@Override
	public void write(byte b[], int off, int len) {
		String str = new String(b, off, len);
		if (lockScreen)
			appendBuffer(str);
		else
			println(str);
		
	}

	/* Override Ancestor method */
	@Override
	public void write(int b){
		String str = new String(new char[] { (char) b});
		if (lockScreen)
			appendBuffer(str);
		else
			println(str);
		
	}

	public void unlock() {
		synchronized (lockBuffer) { 
			if (lockScreen && lockBuffer != null) {
				lockScreen = false;
				StringTokenizer lines = new StringTokenizer(lockBuffer.toString(), "\n");
				// Each log message may be across multiple lines, so we need to search until 
				// either the end or the first log level
				String temp = null;
				while (lines.hasMoreTokens()) {
					String line = lines.nextToken();
					if (line.startsWith("DEBUG") ||
							line.startsWith("INFO ") ||
							line.startsWith("WARN ") ||
							line.startsWith("ERROR") ||
							line.startsWith("TRACE") ||
							line.startsWith("FATAL")) {
						if (temp != null) {
							append(temp);
							temp = line + "\n";
						}
						else {
							if (temp == null)
								temp = line + "\n";
						}
					}
					else {
						if (temp == null)
							temp = line + "\n";
						else
							temp += line + "\n";
					}
				}
				if (temp != null)
					append(temp);
				lockBuffer = null;
			}
		}
	}
	
	protected void displayBuild() {
		boolean temp = lockScreen;
		lockScreen = false;
		append("\t" + "   PCSim2  v." + PC2UI.version  + " " + PC2UI.build
							+ "                                                                       "
							+ "                                                                       " 
							+ "                                                                       " 
							+ "                                                                    \n\t");
		lockScreen = temp;
	}
}
