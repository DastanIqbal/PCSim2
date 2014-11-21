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

import java.util.StringTokenizer;

import javax.swing.JScrollPane;

public class PC2LogViewer extends PC2Console {

	public PC2LogViewer(JScrollPane scroll, PC2TextPane pane) {
		super(scroll, pane);
		pane.wrapText = false;
	}
	
	public void appendLog(String s) throws IllegalArgumentException {
		boolean error = false;
		boolean fatal = false;
		boolean warn = false;
		boolean info = false;
		boolean debug = false;
		boolean trace = false;
		if (s != null) {
			if (s.length() >= 29) {

				String level = s.substring(24,29);
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
				else if (info){
					if (s.contains(">>>>> RX:")) {
						textPane.append(s, MSG, GREEN);
					}
					else if (s.contains("<<<<< TX:")) {
						textPane.append(s, MSG, ORANGE);
					}
					else if (s.contains("INFO   USER:"))
						textPane.append(s, PROMPT, BLUE);
					else if (s.contains("INFO  Test ")) {
						if (s.contains("Passed."))
							textPane.append(s, FATAL, GREEN);
						else if (s.contains("Failed."))
							appendFailedTestResults(s);
						// textPane.append(s, FATAL, RED);
					}
					else if (s.contains("INFO  Commencing test")) {
						textPane.append(s, FATAL, GREEN);
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
		}
		else {
			throw new IllegalArgumentException("LogViewer doesn't allow a null string for an argument.");
		}

	}

	public void appendConsole(String s) throws IllegalArgumentException {
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
				else if (error)	{ 
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
						//	textPane.append(s, FATAL, RED);
					}
					else if (s.startsWith("INFO  Commencing test")) {
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
		}
	}
	
	private void appendFailedTestResults(String s) {
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
	
	public boolean isConsoleLog(String msg) {
		if (msg.startsWith("DEBUG ") || 
				msg.startsWith("INFO ") || 
				msg.startsWith("WARN ") ||
				msg.startsWith("ERROR ") ||
				msg.startsWith("TRACE ") || 
				msg.startsWith("FATAL ")) 
			return true;
		return false;
	}
}
