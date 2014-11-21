/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.capture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.cablelabs.log.*;

public class ProcessIds implements Runnable {

	Process proc = null;
	private static LogAPI logger = null;
	private static String subCat = null;
	private String application = null;
//	private String tool = null;
//	private boolean stopApp = false;
//	private boolean processExit = false;
//	private Wireshark w = null;
	private Thread thread = null;
//	private String threadName = "ProcessIds";
	public final static String PID_TOOL = "tlist";
	protected static String tlistTool = null;
		
	protected static String pid = null;
	protected static String toolsSubDir = null;
	
	public final static String KILL_TOOL = "kill";
	protected String killTool = null;
	
	public ProcessIds(Process p, Wireshark w) {
		this.proc = p;
//		this.w = w;
		this.application = w.application;
		ProcessIds.toolsSubDir = w.toolsSubDir;
		
		logger = Wireshark.logger;
		subCat = Wireshark.subCat;
		if (w.OS == OperatingSystem.WINDOWS) {
			tlistTool = PID_TOOL + ".exe";
			killTool = KILL_TOOL + ".exe";
		} else {
			tlistTool = PID_TOOL ;
			killTool = KILL_TOOL;
		}
	}
	
	static protected String getCommand() {
		String osname = System.getProperty("os.name");
		if(osname.toLowerCase().startsWith("windows")) {
			tlistTool = PID_TOOL + ".exe";
			toolsSubDir = Wireshark.DEF_WIN_TOOL_DIR;
		} else {
			tlistTool = PID_TOOL ;
			toolsSubDir = Wireshark.DEF_LINUX_TOOL_DIR;
		}
		return 	"." + File.separator + toolsSubDir + File.separator + tlistTool;
	}
	
	public void start()	throws IOException	{
		thread = new Thread(this, "ProcessIds");
		
		thread.setDaemon(true);
		thread.start();	
	}
	
	public void run() {
		// Now we need to get the process id of this version of the tool
		try {
			InputStream procIdInStream = proc.getInputStream();
		
			byte [] buffer = new byte [1024];
			int bytes = procIdInStream.read(buffer);
			if (bytes > 0) {
				int total = bytes;
				String procInfo = "";	
				while (bytes > 0) {
					procInfo += new String(buffer);
					bytes = procIdInStream.read(buffer);
					total += bytes;
				}
				info("Read a total of " + total + " bytes from list of process ids.");
				if (procInfo != null) {
					int offset = procInfo.indexOf(application);
					if (offset != -1) {
						int newline = procInfo.lastIndexOf("\n", offset);
						if (newline != -1) {
							newline++; // Move forward past newline
							offset--; // Move back past space 
							String pid = procInfo.substring(newline, offset);
							debug("The " + tlistTool 
									+ " operation has identified the process id = " 
									+ pid); 
							ProcessIds.pid = pid;
						}
						else {
							error("The " + tlistTool + " operation did not find a newline " 
									+ " in the data[\n" + procInfo + "]"); 
						}
					}
					else {
						error("The " + tlistTool + " operation did not find " + application 
								+ " in the data[\n" + procInfo + "]"); 
					}
				}
//				else {
//					error("The " + tlistTool + " operation did not find " + application 
//							+ " in the data[\n" + bytes + "]"); 
//				}
			}
			else {
				error("The " + tlistTool + " operation did not return any information."); 
			}
		}
		catch (Exception ex) {
			error(ex.getMessage());
		}
	}
	
	public synchronized void stop() {
		//this.stopApp = true;
	}
	
	public void terminate() {
		if (pid != null) {
			String cmd = "." + File.separator + toolsSubDir + File.separator + killTool + " " + pid;
			Runtime rt = Runtime.getRuntime();
			debug("Terminating " + application + "(" + pid + ").");
			try {
				Process proc = rt.exec(cmd);
				proc.waitFor();
			}
			catch (Exception ex) {
				error(ex.getMessage());
			}
		}
		else {
			error("The pid is set to null, nothing terminated.");
		}
		pid = null;
	}
	
	static void debug(String str) {
		//System.out.println(str);
		logger.debug(LogCategory.APPLICATION, subCat, str);
	}
	
	static void info(String str) {
		//System.out.println(str);
		logger.info(LogCategory.APPLICATION, subCat, str);
	}
	static void error(String str) {
		//System.out.println(str);
		logger.error(LogCategory.APPLICATION, subCat, str);
	}
	static void error(String str, Exception ex) {
		//System.out.println(str);
		//ex.printStackTrace(System.out);
		logger.error (LogCategory.APPLICATION, subCat, str + "\n", ex);
	}

}
