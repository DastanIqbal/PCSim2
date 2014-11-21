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

import java.util.*;
import java.io.*;

import com.cablelabs.log.*;

public class Wireshark implements TimerListener {

	public boolean isDebugEnabled = true;
	private boolean stopCapture = false;
//	private boolean processExitted = false;
	
	private String command = null;
	private Object room = new Integer(0);
	private Object room2 = new Integer(0);
//	private File fileToWrite;
	private long timeout = -1;
	private ProcessTimerTask timerTask = null;
	private Timer timer = null;
	
	protected static LogAPI logger = LogAPI.getInstance();
	
	protected static String subCat = "Capture";
	public static String DEF_WIN_TOOL_DIR = "win";
	public static String DEF_LINUX_TOOL_DIR = "linux";
	protected String toolsSubDir = "DEF_WIN_TOOL_DIR";
	
	protected String application = null;
	public final static String WIRESHARK = "Wireshark";
	public final static String TSHARK = "tshark";
	private String appdir = null;
	
	
	private final static String DEF_WIRESHARK_WIN_DIR = "C:" + File.separator + "Progra~1" + File.separator + "Wireshark";
	private final static String DEF_WIRESHARK_LINUX_DIR = File.separator + "usr" + File.separator + "sbin";
	protected OperatingSystem OS = null; 
	protected ProcessIds pid = null;
	
	
	
	private static HashMap<Integer, Process> proctracker =
		new HashMap<Integer, Process>();
	static {
		Runtime.getRuntime().addShutdownHook(
			new Thread("WireShark-ShutdownHook") {
			public void run() {
				Wireshark.info("Capture - Terminating any orphan process, " +
					"available = " + proctracker.size());
				Wireshark.terminateOrphans();
			}
		});
	}


	public static void terminateOrphans() {
		for(int key : proctracker.keySet()) {
			Process p = proctracker.get(key);
			Wireshark.info("Capture - Terminating orphan process ... " + key);
			try {
				p.exitValue();
			} catch(IllegalThreadStateException ex) {
				try {
					p.destroy();
					p.waitFor();
				} catch(Exception ex2) {
				}
			}
		}
		proctracker.clear();
		Wireshark.info("Capture - Terminating orphan process ... done");
	}

	public Wireshark() {
		setOperatingSystem();
	}

	public Wireshark(long timeInMillis) {
		setOperatingSystem();
		this.timeout = timeInMillis;
	}
	
	public Wireshark(String dir, long timeInMillis) {
		setOperatingSystem();
		useTShark(dir);
		useWireshark(dir);
		this.timeout = timeInMillis;
	}
	
	private void stopTimer() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}
		
		
	}

	public void expired() {
		stopCapture();
	}
	
	private String setCommand()  {
		if(command == null) {
			if (OS == OperatingSystem.WINDOWS) {
				toolsSubDir = DEF_WIN_TOOL_DIR;
				command =  appdir + 
						File.separator + application + ".exe";
			} else {
				toolsSubDir = "DEF_LINUX_TOOL_DIR";
				command =  File.separator + application;
			}
		} 
		
		return command;
	}

	public String getCommand() {
		if(command == null) {
			if (application == null)
				application = TSHARK;;
			if (OS == OperatingSystem.WINDOWS) {
				toolsSubDir = "win";
				appdir =  DEF_WIRESHARK_WIN_DIR;
				command = appdir + File.separator + application + ".exe";
			} else {
				toolsSubDir = "linux";
				appdir = DEF_WIRESHARK_LINUX_DIR;
				command = appdir + File.separator + "tshark";
			}
		} 

		return command;
		
	}
	
	private void setOperatingSystem() {
		String osname = System.getProperty("os.name");
		if(osname.toLowerCase().startsWith("windows")) {
			OS = OperatingSystem.WINDOWS;
			appdir = DEF_WIRESHARK_WIN_DIR;
		}
		else {
			OS = OperatingSystem.LINUX;
			appdir = DEF_WIRESHARK_LINUX_DIR;
		}
	}

//	public void setFileToWrite(File fileToWrite) {
//		this.fileToWrite = fileToWrite;
//	}

	public void convertToPDML(File infile, File pdml) throws IOException {
		InputStream stdStream = null;
		InputStream errStream = null;
		OutputStream stdIn = null;

		FileWriter fw = null;
		IOReader stdReader = null;
		Process proc = null;
		if (infile.exists()) {
			try {
				useTShark(appdir);
				String[] cmd2 = new String[]{command, "-r",
						infile.getCanonicalPath(), "-T", "pdml", "-l"};
				Runtime rt = Runtime.getRuntime();
				debug("Capture - Performing exec to convert to pdml... ");
				//for(String s : cmd2) {
				//	System.out.print(s); System.out.print(" ");
				//}
				String[] env = null;
				File wd = null;
				proc = rt.exec(cmd2, env, wd);
				proctracker.put(proc.hashCode(), proc);

				errStream = proc.getErrorStream();
				stdStream = proc.getInputStream();
				stdIn = proc.getOutputStream();

				StringWriter errsw = new StringWriter();
				StringWriter outsw = new StringWriter(1024);
				fw = new FileWriter(pdml);
				//IOReader errReader = new IOReader(errStream, errsw, null);
				stdReader = new IOReader("Capture Converter StdOut Reader",
						stdStream, fw, null);

				//errReader.start();
				stdReader.start();



				try {
					int exitValue = proc.waitFor(); 
//					processExitted = true;
					stdReader.breakTh = true;
					//errReader.breakTh = true;

					stdReader.waitTillBreak();
					//errReader.waitTillBreak();

					debug("Capture - pdml convert process exitted with value " +
							exitValue);
				} catch(InterruptedException ex) {
					debug("Capture - error while waiting for process to exit");
				}
				errsw.flush();
				//StringBuffer serr = errsw.getBuffer();

				outsw.flush();
//				StringBuffer sout = outsw.getBuffer();

				//wholesw.flush();
				//wout = wholesw.getBuffer();

			} finally {
				try { stdIn.close(); } catch(Exception ex) { }
				try { stdStream.close(); } catch(Exception ex) { }
				try { errStream.close(); } catch(Exception ex) { }
				try { fw.close(); } catch(Exception ex) { }
				try { 
					// double check that process is terminated
					if (proc != null) {
						proc.destroy(); 
						proc.waitFor(); 
					}
				} catch(Exception ex) {
				} finally {
					if (proc != null)
						proctracker.remove(proc.hashCode());
				}
			}
		}
		else {
			error("Conversion failed because the input file[" + infile + "] does not exist.");
		}
	}


	/**
	 *	@param options each option that needs to be passed on to tshark
	 			for e.g if you would run tshark as
	 				# tshark -I eth0 -R bootp host 10.32.0.20
	 			then method call would be
	 			startCapture("-I", "eth0", "-R", "bootp", "host", "10.32.0.20")
	 */
	public void startCapture(String ... options) throws IOException {
		final String[] options2 = options;
		debug("Starting capture ... ");
		Thread th = new Thread("Wireshark Capture") {
			public void run() {
				try {
					Wireshark.this.exec(null, null, options2);
				} catch(Exception ex) {
					error("Capture - Error while running capture ", ex);
				}
			}
		};
		th.setDaemon(true);
		th.start();

	}

	public void stopCapture() {
		this.stopCapture = true;
		debug("stopCapture has been set to " + stopCapture);
		stopTimer();
		if(pid == null) {
			return;
		}
		else if (ProcessIds.pid != null) {
			pid.terminate();
			pid = null;
		}
		
	}
	
	public String useTShark(String dir) {
		application = TSHARK;
		appdir = dir;
			
		return setCommand();
		
	}
	
	public String useWireshark(String dir) {
		application = WIRESHARK;
		appdir = dir;
		
		return setCommand();
		
	}

	void exec(File wd, String[] env, String ... cmd)
		throws IOException {
		Process proc = null;
		try {

			Runtime rt = Runtime.getRuntime();
			String args = "";
			for(String s : cmd) {
				args += s + " ";
			}
			debug("Capture - Performing exec " + args);
			
			proc = rt.exec(cmd, env, wd);
			proctracker.put(proc.hashCode(), proc);

			stopCapture = false;
			//final Process proc_i = proc;
			
			if (timeout > 0) {
				timer = new Timer("Capture Timer", true);
				timerTask = new ProcessTimerTask(this);
				timer.schedule(timerTask, timeout);
				debug("Starting timeout timer(" + timerTask + ") for " + 
						timeout + " msecs.");
			}
			
			// Now we need to get the process id of this version of the tool
			String tlistCmd = ProcessIds.getCommand();
			File tmp = new File (tlistCmd);
			
			if (tmp.exists()) {
				Process tlist = rt.exec(tlistCmd);
				pid = new ProcessIds(tlist, this);
				pid.start();
//				InputStream procIdInStream = tlist.getInputStream();
//				byte [] buffer = new byte [1024];
//				int bytes = procIdInStream.read(buffer);
//				if (bytes > 0) {
//					String procInfo = "";
//					int total = bytes;
//					while (bytes > 0) {
//						procInfo += new String(buffer);
//						bytes = procIdInStream.read(buffer);
//						total += bytes;
//					}
//					if (procInfo != null) {
//						int offset = procInfo.indexOf(application);
//						if (offset != -1) {
//							int newline = procInfo.lastIndexOf(application, offset);
//							if (newline != -1) {
//								newline++; // Move forward past newline
//								offset -= 2; // Move back past space 
//								String pid = procInfo.substring(newline, offset);
//								debug("The " + tlistTool 
//										+ " operation has identified the process id = " 
//										+ pid); 
//							}
//							else {
//								error("The " + tlistTool + " operation did not find a newline " 
//										+ " in the data[\n" + procInfo + "]"); 
//							}
//						}
//						else {
//							error("The " + tlistTool + " operation did not find " + application 
//									+ " in the data[\n" + procInfo + "]"); 
//						}
//					}
//					else {
//						error("The " + tlistTool + " operation did not find " + application 
//								+ " in the data[\n" + bytes + "]"); 
//					}
//				}
//				else {
//					error("The " + tlistTool + " operation did not return any information."); 
//				}
//			}
//			else {
//				error("The " + tlistCmd + " application does not exist."); 
			}
//			
//			Thread th2 = new Thread("StopThread") {
//				public void run() {
//					while(true) {
//						if(!stopCapture) {
//							try {
//								// wait for 2 seconds, let wireshark close
//								Thread.sleep(1000);
//								continue;
//							} catch(InterruptedException ex) {
//							}
//						}
//						if(processExitted) {
//							break;
//						}
//						//debug("Sending ctrl+c to tshark");
//						try {
//							proc_i.destroy();
//							Thread.sleep(5000);
//							break;
//							
//						} catch(Exception ex) {
//							error("Error while Sending ctrl+c ", ex);
//						}
//					}
//				}
//			};
//			th2.setDaemon(true);
//			th2.start();

			try {
				int exitValue = proc.waitFor(); 
//				processExitted = true;

				debug("Capture - process exitted with value " + exitValue);
			} catch(InterruptedException ex) {
				debug("Capture - error while waiting for process to exit");
			}

//			if(fileToWrite != null) {
//				StringReader sr = null;
//				FileWriter fw = null;
//				try {
//					fw = new FileWriter(fileToWrite);
//					char[] buff = new char[1024*500];
//					while(sr.read(buff, 0, buff.length) > 0) {
//						fw.write(buff);
//					}
//					fw.flush();
//				} catch(Exception ex) {
//					error("Capture - Error while saving to file ", ex);
//				} finally {
//					try { sr.close(); } catch(Exception ex) { }
//					try { fw.close(); } catch(Exception ex) { }
//				}
//			}


		} finally {
			synchronized(room) {
				room.notifyAll();
			}
			synchronized(room2) {
				room2.notifyAll();
			}
			try {
				proc.destroy();
				proc.waitFor();
			} catch(Exception ex) {
				// don't care wat exception is.
				// Just making a last attempt to terminate it all
			} finally {
				proctracker.remove(proc.hashCode());
			}
		}
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


	public static void main(String[] args) throws Exception {
		Wireshark w = new Wireshark(30000);
		String pcap = "pcap.cap";
		//String pcap2 = "pcap2.cap";

		String cmd = w.useTShark(w.appdir);
		//String cmd = w.useWireshark(w.appdir);
		w.startCapture(cmd, 
			// "-i", "\\Device\\NPF_{5E714466-F115-4DD2-A33A-4E955CF0576B}",
		    "-i", "3",
			"-w", pcap, "-l", "-q");
		
		Thread.sleep(60*1000);
		//w.stopCapture();
		

		//System.out.println("Output is \n" + w.getStdOut());
		w.convertToPDML(new File(pcap), new File("pdml.xml"));
	}


} // Wireshark
