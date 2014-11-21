/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.Priority;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.helpers.ISO8601DateFormat;

public class LogAPI {

	/**
	 * The path and file name to use for the application log file.
	 *
	 */
	private static String appFileName;

	/**
	 * The flag defines when the console window of the gui has been
	 * created. Once this has been created, the console logging can
	 * begin. Otherwise the information is directed at the window that
	 * started the application.
	 */
	private static boolean consoleCreated = false;

	/**
	 * The actual logger for the console window.
	 */
	private static Logger console = null;

	/**
	 * The console's appender
	 */
	private static ConsoleAppender ca = null;

	/**
	 * The layout to use for the console window
	 */
	private static PatternLayout cLayout = null;

	/**
	 * The actual logger for the application log file.
	 */
	private static Logger appLog = null;
	/**
	 * The application log file's appender
	 */
	private static RollingFileAppender appFA = null;

	/**
	 * The layout to use for the application log file.
	 */
	private static PatternLayout aLayout = null;

	/**
	 * The actual logger for the test log file
	 */
	private static Logger testLog = null;

	/**
	 * The test log file's appender
	 */
	private static FileAppender testFA = null;

	/**
	 * The layout to use for the test log file.
	 */
	private static PatternLayout tLayout = null;

	/**
	 * The log controls for the console window. The table is
	 * indexed upon Category and then Subcategory to obtain the
	 * level of logging to perform on a specific message for the
	 * console window.
	 */
	private static ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>> consoleTable = null;

	/**
	 * The log controls for the application log file. The table is
	 * indexed upon Category and then Subcategory to obtain the
	 * level of logging to perform on a specific message for the
	 * application log file.
	 */
	private static ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>> appTable = null;

	/**
	 * The log controls for the test log file. The table is
	 * indexed upon Category and then Subcategory to obtain the
	 * level of logging to perform on a specific message for the
	 * test log file.
	 */
	private static ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>> testTable = null;


	/**
	 * A string representation of the logger's configuration being used by the system.
	 */
	private static String logLevelConfig = "";

	/**
	 * A flag to control the reading of the configuration file only once.
	 */
	private static boolean initialized = false;

	/**
	 * The LogAPI is a singleton so only allow for one to be constructed
	 */
	//private static LogAPI logger = null;
	private static class SingletonHolder {
	    public static final LogAPI logAPI = new LogAPI();
	}

	/**
	 * A list of external log4j Logger class that should be called to attempt
	 * log events as well as the interal ones defined within this class.
	 */
	private LinkedList<Logger> externalLoggers = new LinkedList<Logger>();

	private DateFormat df = new ISO8601DateFormat();

	/**
	 * The timestamp of the log configuration file the last time it was read.
	 */
	private static long configFileLastModified = 0;

	/**
	 * A container for the statistical information kept for the log categories
	 *
	 */
	private static ConcurrentHashMap<String, LogStats> statsTable =
		new ConcurrentHashMap<String, LogStats>();

	
	private static LogMonitor monitor = null;

	/**
	 * This variable is used to sequence the messages received/sent by the platform
	 * for the trace tool. Each message that is received/sent should request a number
	 * from the logger and add it to the MsgEvent. This will allow the trace tool to
	 * determine which FSM each message was delivered for processing. The attribute
	 * is reset to zero each time the clear method is invoked.
	 */
	private static int msgSequencer = 0;
	
	private static LogAPIConfig config;


	/**
	 * Private constructor to keep class a singleton
	 */
	private LogAPI() {

	}

	public void addMonitorListener(MonitorListener ml) {
		if (monitor != null)
			monitor.addListener(ml);
	}

	/**
	 * Terminates and closes all logging to the test log file.
	 *
	 */
	public void clear() {
		if (testLog != null)
			testLog.removeAppender(testFA);
		testFA = null;
		tLayout = null;
		statsTable.clear();
		msgSequencer = 0;
	}
	
	/**
	 * Returns the path to the log level configuration file being used.
	 * @return
	 */
	public String getLogLevelConfigPath() {
	    if (config != null) {
	        return config.getLogLevelConfigPath();
	    }
	    return null;
	}

	/**
	 * Creates the test log file, using the file name
	 * passed in, and its associated appender
	 *
	 * @param name - the name and path of the log file
	 * to assign to the appender.
	 */
	public void createTestLog(String name) {
		if (testFA == null) {
			try {
				tLayout = new PatternLayout(config.getFilePattern());
				testFA = new FileAppender(tLayout, name, false);
				if (testLog == null)
					testLog = Logger.getLogger("Test");
				testLog.addAppender(testFA);
				testLog.setAdditivity(false);
				testLog.setLevel(Level.ALL);
				testLog.info(logLevelConfig);
			}
			catch (IOException io) {
				console.fatal("LogAPI failed to create test log file.");
			}
		}
		else {
			console.error("Test logger didn't get cleared before starting new test");

		}
	}

	public void debug(LogCategory cat, String subcat, Object o ) {
//		System.err.println("Enter logger(debug)");
		Object n = null;
		try {
			if (console != null &&
		
				Priority.DEBUG_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
			synchronized(console) {
				console.debug(o);
			}
		}
		if (appLog != null &&
				Priority.DEBUG_INT >= getPriorityFromTable(appTable, cat, subcat)) {
			synchronized(appLog) {
				if (n == null)
					n = format(Level.DEBUG, o, new Throwable());
				appLog.debug(n);
			}
		}
		if (testLog != null && testFA != null) {
			if (Priority.DEBUG_INT >= getPriorityFromTable(testTable, cat, subcat)) {
				synchronized(testLog) {
					if (n == null)
						n = format(Level.DEBUG, o, new Throwable());
					testLog.debug(n);
				}
			}
		}
		if (externalLoggers.size() > 0) {
			ListIterator<Logger> iter = externalLoggers.listIterator();
			while(iter.hasNext()) {
				Logger l = iter.next();
				synchronized(l) {
					l.debug(o);
				}
			}
		} 
		} catch (java.lang.Error error) {
			
		}
//		System.err.println("\tExit logger(debug)");
	}

	public void debug(LogCategory cat, String subcat, Object o, Throwable t ) {
//		System.err.println("Enter logger(debug)");
		Object n = null;
		try {
			if (console != null &&
				Priority.DEBUG_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
			synchronized(console) {
				console.debug(o, t);
			}
		}
		if (appLog != null &&
				Priority.DEBUG_INT >= getPriorityFromTable(appTable, cat, subcat)) {
			synchronized(appLog) {
				if (n == null)
					n = format(Level.DEBUG, o,t);
				appLog.debug(n, t);
			}
		}
		if (testLog != null && testFA != null) {
			if (Priority.DEBUG_INT >= getPriorityFromTable(testTable, cat, subcat)) {
				synchronized(testLog) {
					if (n == null)
						n = format(Level.DEBUG, o, t);
					testLog.debug(n, t);
				}
			}
		}
		if (externalLoggers.size() > 0) {
			ListIterator<Logger> iter = externalLoggers.listIterator();
			while(iter.hasNext()) {
				Logger l = iter.next();
				synchronized(l) {
					l.debug(o,t);
				}
			}
		}
		} catch (java.lang.Error error) {
			
		}
	}

	public String dumpLogStats() {
		String result = "\n    Log Statistics :\n";
		if (statsTable.size() > 0) {
			Enumeration<LogStats> e = statsTable.elements();
			while (e.hasMoreElements()) {
				LogStats ls = e.nextElement();
				result += ls.toString() + "\n";
			}
		}
		else
			result += "\t NONE.";
		return result;

	}

	public void error(LogCategory cat, String subcat, Object o ) {
		updateStats(cat, subcat, Level.ERROR);
		Object n = null;
		try {
			if (console != null &&
					Priority.ERROR_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
				synchronized(console) {
					console.error(o);
				}
			}
			if (appLog != null &&
					Priority.ERROR_INT >= getPriorityFromTable(appTable, cat, subcat)) {
				synchronized(appLog) {
					if (n == null)
						n = format(Level.ERROR, o, new Throwable());
					appLog.error(n);
				}
			}
			if (testLog != null && testFA != null) {
				if (Priority.ERROR_INT >= getPriorityFromTable(testTable, cat, subcat)) {
					synchronized(testLog) {
						if (n == null)
							n = format(Level.ERROR, o, new Throwable());
						testLog.error(n);
					}
				}
			}
			if (externalLoggers.size() > 0) {
				ListIterator<Logger> iter = externalLoggers.listIterator();
				while(iter.hasNext()) {
					Logger l = iter.next();
					synchronized(l) {
						l.error(o);
					}
				}
			}
		} catch (java.lang.Error error) {

		}
	}

	public void error(LogCategory cat, String subcat, Object o, Throwable t ) {
		updateStats(cat, subcat, Level.ERROR);
		Object n = null;
		try {
			if (console != null &&
					Priority.ERROR_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
				synchronized(console) {
					console.error(o, t);
				}
			}
			if (appLog != null &&
					Priority.ERROR_INT >= getPriorityFromTable(appTable, cat, subcat)) {
				synchronized(appLog) {
					if (n == null)
						n = format(Level.FATAL, o, t);
					appLog.error(n, t);
				}
			}
			if (testLog != null && testFA != null ) {
				if (Priority.ERROR_INT >= getPriorityFromTable(testTable, cat, subcat)) {
					synchronized(testLog) {
						if (n == null)
							n = format(Level.FATAL, o, t);
						testLog.error(n, t);
					}
				}
			}
			if (externalLoggers.size() > 0) {
				ListIterator<Logger> iter = externalLoggers.listIterator();
				while(iter.hasNext()) {
					Logger l = iter.next();
					synchronized(l) {
						l.error(o, t);
					}
				}
			}
		} catch (java.lang.Error error) {

		}
	}

	public void fatal(LogCategory cat, String subcat, Object o ) {
		Object n = null;
		try {
			updateStats(cat, subcat, Level.FATAL);
			if (console != null &&
					Priority.FATAL_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
				synchronized(console) {
					console.fatal(o);
				}
			}
			if (appLog != null &&
					Priority.FATAL_INT >= getPriorityFromTable(appTable, cat, subcat)) {
				synchronized(appLog) {
					if (n == null)
						n = format(Level.FATAL, o, new Throwable());
					appLog.fatal(n);
				}
			}
			if (testLog != null && testFA != null) {
				if (Priority.FATAL_INT >= getPriorityFromTable(testTable, cat, subcat)) {
					synchronized(testLog) {
						if (n == null)
							n = format(Level.FATAL, o, new Throwable());
						testLog.fatal(o);
					}
				}
			}
			if (externalLoggers.size() > 0) {
				ListIterator<Logger> iter = externalLoggers.listIterator();
				while(iter.hasNext()) {
					Logger l = iter.next();
					synchronized(l) {
						l.fatal(o);
					}
				}
			}
		} catch (java.lang.Error error) {

		}
	}

	public void fatal(LogCategory cat, String subcat, Object o, Throwable t ) {
		updateStats(cat, subcat, Level.FATAL);
		Object n = null;
		try{
			if (console != null &&
					Priority.FATAL_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
				synchronized(console) {
					console.fatal(o, t);
				}
			}
			if (appLog != null &&
					Priority.FATAL_INT >= getPriorityFromTable(appTable, cat, subcat)) {
				synchronized(appLog) {
					if (n == null)
						n = format(Level.FATAL, o, t);
					appLog.fatal(n, t);
				}
			}
			if (testLog != null && testFA != null) {
				if (Priority.FATAL_INT >= getPriorityFromTable(testTable, cat, subcat)) {
					synchronized(testLog) {
						if (n == null)
							n = format(Level.FATAL, o, t);
						testLog.fatal(n, t);
					}
				}
			}
			if (externalLoggers.size() > 0) {
				ListIterator<Logger> iter = externalLoggers.listIterator();
				while(iter.hasNext()) {
					Logger l = iter.next();
					synchronized(l) {
						l.fatal(o,t);
					}
				}
			}
		} catch (java.lang.Error error) {

		}
	}

	public void info(LogCategory cat, String subcat, Object o ) {
		//		System.err.println("Enter logger(info)");
		Object n = null;
		try {
			if (console != null &&
					Priority.INFO_INT >= getPriorityFromTable(consoleTable, cat, subcat)){
				synchronized(console) {
					console.info(o);
				}
			}
			if (appLog != null &&
					Priority.INFO_INT >= getPriorityFromTable(appTable, cat, subcat)) {
				synchronized(appLog) {
					Throwable t = new Throwable();
					if (n == null)
						n = format(Level.INFO, o, t);
					appLog.info(n);
				}
			}
			if (testLog != null && testFA != null) {
				if (Priority.INFO_INT >= getPriorityFromTable(testTable, cat, subcat)) {
					synchronized(testLog) {
						if (n == null)
							n = format(Level.INFO, o, new Throwable());
						testLog.info(n);
					}
				}
			}
			if (externalLoggers.size() > 0) {
				ListIterator<Logger> iter = externalLoggers.listIterator();
				while(iter.hasNext()) {
					Logger l = iter.next();
					synchronized(l) {
						l.info(o);
					}
				}
			}
		} catch (java.lang.Error error) {

		}
	}

	public void info(LogCategory cat, String subcat, Object o, Throwable t ) {

		Object n = null;
		try {
			if (console != null &&
					Priority.INFO_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
				synchronized(console) {
					console.info(o, t);
				}
			}
			if (appLog != null &&
					Priority.INFO_INT >= getPriorityFromTable(appTable, cat, subcat)) {
				synchronized(appLog) {
					if (n == null)
						n = format(Level.INFO, o, t);
					appLog.info(n, t);
				}
			}
			if (testLog != null && testFA != null) {
				if (Priority.INFO_INT >= getPriorityFromTable(testTable, cat, subcat)) {
					synchronized(testLog) {
						if (n == null)
							n = format(Level.INFO, o, t);
						testLog.info(n, t);
					}
				}
			}
			if (externalLoggers.size() > 0) {
				ListIterator<Logger> iter = externalLoggers.listIterator();
				while(iter.hasNext()) {
					Logger l = iter.next();
					synchronized(l) {
						l.info(o,t);
					}
				}
			}
		} catch (java.lang.Error error) {

		}
	}


	public synchronized boolean isDebugEnabled(LogCategory cat, String subcat) {
		boolean c = false;
		boolean a = false;
		boolean t = false;
		if (Priority.DEBUG_INT == getPriorityFromTable(consoleTable, cat, subcat))
			c=true;
		if (Priority.DEBUG_INT == getPriorityFromTable(appTable, cat, subcat))
			a=true;
		if (testLog != null) {
			if (Priority.DEBUG_INT == getPriorityFromTable(testTable, cat, subcat))
				t=true;
		}

		return (c || a || t);
	}

	public synchronized boolean isTraceEnabled(LogCategory cat, String subcat) {
		boolean c = false;
		boolean a = false;
		boolean t = false;
		if (Level.TRACE_INT == getPriorityFromTable(consoleTable, cat, subcat))
			c=true;
		if (Level.TRACE_INT == getPriorityFromTable(appTable, cat, subcat))
			a=true;
		if (testLog != null) {
			if (Level.TRACE_INT == getPriorityFromTable(testTable, cat, subcat))
				t=true;
		}

		return (c || a || t);
	}


	public void logConfigSettings() {
		if (Priority.DEBUG_INT >= getPriorityFromTable(consoleTable, LogCategory.APPLICATION, ""))
			console.debug(logLevelConfig);
		appLog.info(logLevelConfig);
		if (testLog != null) {
			testLog.info(logLevelConfig);
		}
	}

	/**
	 * This method should be called when some logging
	 * needs to be directed to the log files regardless
	 * of the Log Configuration settings and yet we
	 * don't want it to appear on the Console screen.
	 * @param o
	 */
	public void preserve(Object o ) {
		Object n = format(Level.INFO, o, new Throwable());
		synchronized(appLog) {
			appLog.info(n);
		}
		if (testLog != null) {
			synchronized(testLog) {
				testLog.info(n);
			}
		}
	}


	/**
	 * This method should be called when some logging
	 * needs to be directed to the log files regardless
	 * of the Log Configuration settings and yet we
	 * don't want it to appear on the Console screen.
	 * @param o - the message to log
	 * @param t - the throwable object to include
	 */
	public void preserve(Object o, Throwable t ) {
		Object n = format(Level.INFO, o, t);
		synchronized(appLog) {
			appLog.info(n, t);
		}
		if (testLog != null) {
			synchronized(testLog) {
				testLog.info(n, t);
			}
		}
	}

	public boolean removeMonitorListener(MonitorListener ml) {
		if (monitor != null)
			return monitor.removeListener(ml);

		return false;
	}


	public void trace(LogCategory cat, String subcat, Object o ) {
		Object n = null;
		try {
			if (console != null &&
					Level.TRACE_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
				synchronized(console) {
					console.trace(o);
				}
			}
			if (appLog != null &&
					Level.TRACE_INT >= getPriorityFromTable(appTable, cat, subcat)) {
				synchronized(appLog) {
					if (n == null)
						n = format(Level.TRACE, o, new Throwable());
					appLog.trace(n);
				}
			}
			if (testLog != null && testFA != null) {
				if (Level.TRACE_INT >= getPriorityFromTable(testTable, cat, subcat)) {
					synchronized(testLog) {
						if (n == null)
							n = format(Level.TRACE, o, new Throwable());
						testLog.trace(n);
					}
				}
			}
			if (externalLoggers.size() > 0) {
				ListIterator<Logger> iter = externalLoggers.listIterator();
				while(iter.hasNext()) {
					Logger l = iter.next();
					synchronized(l) {
						l.trace(o);
					}
				}
			}
		} catch (java.lang.Error error) {

		}
	}

	public void trace(LogCategory cat, String subcat, Object o, Throwable t ) {

		Object n = null;
		try {
			if (console != null &&
					Level.TRACE_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
				synchronized(console) {
					console.trace(o, t);
				}
			}
			if (appLog != null &&
					Level.TRACE_INT >= getPriorityFromTable(appTable, cat, subcat)) {
				synchronized(appLog) {
					if (n == null)
						n = format(Level.TRACE, o, t);
					appLog.trace(n, t);
				}
			}
			if (testLog != null && testFA != null) {
				if (Level.TRACE_INT >= getPriorityFromTable(testTable, cat, subcat)) {
					synchronized(testLog) {
						if (n == null)
							n = format(Level.TRACE, o, t);
						testLog.trace(n, t);
					}
				}
			}
			if (externalLoggers.size() > 0) {
				ListIterator<Logger> iter = externalLoggers.listIterator();
				while(iter.hasNext()) {
					Logger l = iter.next();
					synchronized(l) {
						l.trace(o,t);
					}
				}
			}
		} catch (java.lang.Error error) {

		}
	}

	public void warn(LogCategory cat, String subcat, Object o ) {
		updateStats(cat, subcat, Level.WARN);
		Object n = null;
		try { 
			if (console != null &&
					Priority.WARN_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
				synchronized(console) {
					console.warn(o);
				}
			}
			if (appLog != null &&
					Priority.WARN_INT >= getPriorityFromTable(appTable, cat, subcat)) {
				synchronized(appLog) {
					if (n == null)
						n = format(Level.WARN, o, new Throwable());
					appLog.warn(n);
				}
			}
			if (testLog != null && testFA != null) {
				if (Priority.WARN_INT >= getPriorityFromTable(testTable, cat, subcat)) {
					synchronized(testLog ) {
						if (n == null)
							n = format(Level.WARN, o, new Throwable());
						testLog.warn(n);
					}
				}
			}
			if (externalLoggers.size() > 0) {
				ListIterator<Logger> iter = externalLoggers.listIterator();
				while(iter.hasNext()) {
					Logger l = iter.next();
					synchronized(l) {
						l.warn(o);
					}
				}
			}
		} catch (java.lang.Error error) {

		}

	}

	public void warn(LogCategory cat, String subcat, Object o, Throwable t ) {
		//		System.err.println("Enter logger(warn)");
		updateStats(cat, subcat, Level.WARN);
		Object n = null;
		try {
			if (console != null &&
					Priority.WARN_INT >= getPriorityFromTable(consoleTable, cat, subcat)) {
				synchronized(console) {
					console.warn(o, t);
				}
			}
			if (appLog != null &&
					Priority.WARN_INT >= getPriorityFromTable(appTable, cat, subcat)) {
				synchronized(appLog) {
					if (n == null)
						n = format(Level.WARN, o, t);
					appLog.warn(n, t);
				}
			}
			if (testLog != null && testFA != null) {
				if (Priority.WARN_INT >= getPriorityFromTable(testTable, cat, subcat)) {
					synchronized(testLog) {
						if (n == null)
							n = format(Level.WARN, o, t);
						testLog.warn(n, t);
					}
				}
			}
			if (externalLoggers.size() > 0) {
				ListIterator<Logger> iter = externalLoggers.listIterator();
				while(iter.hasNext()) {
					Logger l = iter.next();
					synchronized(l) {
						l.warn(o,t);
					}
				}
			}
		} catch (java.lang.Error error) {

		}

	}

	private Object format(Level l, Object o, Throwable t) {
		if (o instanceof String) {
			StackTraceElement [] ste = t.getStackTrace();
			String name = null;
			String line = null;
			boolean done = false;
			for (int i=0; i < ste.length && !done; i++){
				if (ste[i].getFileName() == null) {
					name = ste[i].getClassName();
					line = Integer.toString(ste[i].getLineNumber());
					done = true;
				}
				else if (!(ste[i].getFileName().equals("LogAPI.java"))) {

					name = ste[i].getFileName();
					line = Integer.toString(ste[i].getLineNumber());
					done = true;
				}
			}
			Date date = new Date(System.currentTimeMillis());

			String ts = df.format(date);
			Thread thread = Thread.currentThread();
			String level = l.toString();
			if (level.length() < 5) {
				for (int i= 0; i < (5-level.length()); i++)
					level += " ";
			}
			String result = ts + " " + level + " "
					+ (String)o + " - " + name + " "
					+ line + " [" + thread.getName() + "]\n";
			return result;

		}
		return o;

	}

	/**
	 * This method updates the specified category-subcategories log statistics
	 * based upon the level of the error.
	 */
	private static void updateStats(LogCategory cat, String subCat, Level l) {
		String key = cat.toString() + "-" + subCat;
		LogStats ls = statsTable.get(key);
		if (ls == null) {
			ls = new LogStats(cat, subCat);
			statsTable.put(key, ls);
		}

		if (l == Level.WARN)
			ls.warn();
		else if (l == Level.ERROR)
			ls.error();
		else if (l == Level.FATAL)
			ls.fatal();
	}

	/**
	 * Retrieves a reference to the LogAPI.
	 * <p>
	 * <b>NOTE:</b> The first getInstance called determines the configuration settings used. <br/>
	 * If this version is called first the default settings as defined in {@link LogAPIConfig} will be used.
	 * </P>
	 *
	 * @return LogAPI
	 */
	public static LogAPI getInstance() {
		//		if (logger == null)
		//			logger = new LogAPI();
		init();
		return SingletonHolder.logAPI;
	}

	/**
	 * Retrieves a reference to the LogAPI.
	 * 
	 * @param logApiConfig The configuration settings to use.
	 * 
	 * <p>
	 * <b>NOTE:</b> The first getInstance called determines the configuration settings used. <br/>
	 * If this version is called first the settings found in the passed in {@link LogAPIConfig} will be used.<br/>
	 * Any changes to this object after calling this may result in unexpected behavior.
	 * </p>
	 *
	 * @return LogAPI
	 */
	public static LogAPI getInstance(LogAPIConfig logApiConfig) {
		if (LogAPI.config == null) {
			LogAPI.config = logApiConfig;
		}
		init();
		return SingletonHolder.logAPI;
	}

	/**
	 * Retrieves a reference to the LogAPI.
	 * 
	 *  @param logApiConfig The configuration settings to use.
	 *  @param consoleSet true if the console is already set.
	 *  
	 * <p>
	 * <b>NOTE:</b> The first getInstance called determines the configuration settings used. <br/>
	 * If this version is called first the settings found in the passed in {@link LogAPIConfig} will be used.<br/>
	 * Any changes to this object after calling this may result in unexpected behavior.
	 * </p>
	 *
	 * @return LogAPI
	 */
	public static LogAPI getInstance(LogAPIConfig logApiConfig, boolean consoleSet) {
		if (LogAPI.config == null) {
			LogAPI.config = logApiConfig;
		}
		if (consoleSet)
			LogAPI.consoleCreated = consoleSet;
		init();
		return SingletonHolder.logAPI;
	}

	//	private Throwable generateStackTrace() {
	//		Throwable t = new Throwable();
	//		StackTraceElement [] origSTE = t.getStackTrace();
	//		StackTraceElement [] newSTE = new StackTraceElement[origSTE.length-1];
	//		System.arraycopy(origSTE, 1, newSTE, 0, newSTE.length);
	//		t.setStackTrace(newSTE);
	//		return t;
	//	}

	public static long getLastRefresh() {
		return configFileLastModified;
	}

	public static int getSequencer() {
		return (++msgSequencer);
	}

	public static boolean isConfigured() {
		return (config != null);
	}

	public static void readConfig() {
		File f = new File(config.getLogLevelConfigPath());
		if (f.exists() && f.canRead() && f.isFile()) {
			try {
				// Since we can read the file add the entries for the Log actions of an FSM to each
				// table
				String logData = "";
				ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>> ct =
						new ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>>();
				ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>> at =
						new ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>>();
				ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>> tt =
						new ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>>();
				//				ConcurrentHashMap<String, Integer> logMsgSubTable = new ConcurrentHashMap<String, Integer>();
				//				
				//				logMsgSubTable.put("*", Priority.ALL_INT);
				//				ct.put(LogCategory.LOG_MSG, logMsgSubTable);
				//				at.put(LogCategory.LOG_MSG, logMsgSubTable);
				//				tt.put(LogCategory.LOG_MSG, logMsgSubTable);

				int lineNum = 0;
				BufferedReader in = new BufferedReader(new FileReader(f));
				String line = in.readLine();
				while (line != null) {
					lineNum++;
					// First check if line is a comment
					if (line.length() <= 0 || line.charAt(0) == '#') {
						// do nothing
					}
					else {
						// Since it isn't a comment the line should be
						// Logger SP Category SP Subcategory SP LogLevel
						StringTokenizer tokens = new StringTokenizer(line);
						if (tokens.countTokens() == 4) {
							String log = tokens.nextToken();
							String cat = tokens.nextToken();
							String subCat = tokens.nextToken();
							String level = tokens.nextToken();
							LogCategory lc = LogCategory.getCategory(cat);
							Integer l = getLevel(level);
							if (lc != null && l != null && isValidLogger(log)) {
								boolean all = false;
								if (log.equals("ALL"))
									all = true;

								if (all || log.equals("CONSOLE")) {
									addTableEntry(ct, lc, subCat, l);
								}
								if (all || log.equals("APP")) {
									addTableEntry(at, lc, subCat, l);
								}
								if (all || log.equals("TEST")) {
									addTableEntry(tt, lc, subCat, l);
								}
								logData += log + " " + cat + " " + subCat + " " + level + "\n";
							}
							else {
								if (lc == null) {
									logError("Invalid Category in the LogConfig file at line number "
											+ lineNum + " (" + line + ").");

								} else if (l == null) {
									logError("Invalid Level in the LogConfig file at line number "
											+ lineNum + " (" + line + ").");
								} else {
									logError("Invalid Logger in the LogConfig file at line number "
											+ lineNum + " (" + line + ").");
								}
							}
						}
						else {
							logError("Incorrect number of parameters in the LogConfig file at line number "
									+ lineNum + " (" + line + ").");
						}
					}
					line = in.readLine();
				}
				if (consoleTable == null)
					consoleTable = ct;
				else {
					synchronized (consoleTable) {
						consoleTable = ct;
					}
				}

				if (appTable == null)
					appTable = at;
				else {
					synchronized (appTable) {
						appTable = at;
					}
				}

				if (testTable == null)
					testTable = tt;
				else {
					synchronized (testTable) {
						testTable = tt;
					}
				}
				configFileLastModified = f.lastModified();
				logLevelConfig = logData;
				initialized = true;
				//				debugging
				//				console.info("\n\nConsole value =" + getPriorityFromTable(consoleTable, LogCategory.SIP, "Distributor" ));
				//				console.info("App value =" + getPriorityFromTable(appTable, LogCategory.SIP, "Distributor" ));
				//				console.info("Test value =" + getPriorityFromTable(testTable, LogCategory.SIP, "Distributor" ) + "\n\n");
			}
			catch (FileNotFoundException fnf) {
				logError("UI couldn't find the batch file at " + f.getAbsolutePath());
				fnf.printStackTrace();
			}
			catch (IOException io) {
				logError("UI encountered an error while trying to read the batch file[" + f.getAbsolutePath() + "].");
				io.printStackTrace();
			}
		}
		else {
			if (!f.exists()) {
				logError("The logAPI could not find the log level configuration file at "
						+ config.getLogLevelConfigPath());
			}
			else if (!f.isFile()) {
				logError("The log level configuration file at "
						+ config.getLogLevelConfigPath() + " is not a file.");
			}
			else if (!f.canRead()) {
				logError("The log level configuration file at "
						+ config.getLogLevelConfigPath() + " can not be read by the system.");
			}

			logError("No log level configuration settings loaded. All message levels will be shown.");


			initialized = true;
			consoleTable = new ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>>();
			appTable = new ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>>();
			testTable = new ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>>();

			// Config file could not be found so log everything.
			addTableEntry(consoleTable, LogCategory.ALL, "*", Priority.ALL_INT);
			addTableEntry(appTable, LogCategory.ALL, "*", Priority.ALL_INT);
			addTableEntry(testTable, LogCategory.ALL, "*", Priority.ALL_INT);

		}

		// Add the log message category to all tables
		addTableEntry(consoleTable, LogCategory.LOG_MSG, "*", Priority.ALL_INT);
		addTableEntry(appTable, LogCategory.LOG_MSG, "*", Priority.ALL_INT);
		addTableEntry(testTable, LogCategory.LOG_MSG, "*", Priority.ALL_INT);
	}

	/**
	 * Notifies the class that the console window has been set
	 * and that construction of the console appender and logger
	 * can begin. It is necessary to wait when a UI holds the
	 * console, because this must be constructed and the System.out
	 * and System.err must be set appropriately before the logger
	 * and appender classes are created.
	 *
	 */
	static public void setConsoleCreated() {
		consoleCreated = true;
	}

	public static void shutdown() {
		monitor.shutdown();
	}

	/**
	 * Allows a system to add external log4j Loggers to the class for
	 * additional logging to other appenders.
	 *
	 *
	 * @param log - add the specified Logger to the external logger list.
	 *
	 *
	 * @return
	 */
	public static LogAPI subscribe(Logger log) {
		SingletonHolder.logAPI.externalLoggers.add(log);
		return SingletonHolder.logAPI;
	}

	/**
	 * Adds a new log level entry into the specific loggers
	 * table based upon category, subcategory, and level
	 */
	private static void addTableEntry(ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>> table,
			LogCategory cat, String subcat, Integer level) {
		//		debugging String tableName = "CONSOLE table";
		//		debugging if (table == appTable)
		//		debugging	 tableName = "APP table";
		//		debugging else if (table == testTable)
		//		debugging   tableName = "TEST table";

		ConcurrentHashMap<String, Integer> subTable = null;
		subTable = table.get(cat);
		if (subTable == null)
			subTable = new ConcurrentHashMap<String, Integer>();
		subTable.put(subcat, level);
		//		debugging		console.debug("Adding subCat(" + subcat+ ") with level="
		//		debugging			+ level + " to category(" + getCategory(cat) + ") to "  + tableName + ".");
		table.put(cat, subTable);

		//		debugging   Integer value = getPriorityFromTable(table, cat, subcat);
		//		debugging	if (value < Integer.MAX_VALUE)
		//		debugging		console.debug("Value set for " + tableName + getCategory(cat) + subcat + value );
	}

	/**
	 * Creates the application log file and its associated appender
	 *
	 */
	private static void createAppLog() {
		try {
			// We need to find the last run number used for the application log
			// Once we have it, we can construct the first file for this new
			// run of the application
			File dir = new File(config.getLogDirectory());
			String logPrefix = config.getLogPrefix();
			String logExt = config.getLogExtension();

			if (!dir.exists() && config.getCreateLogDirIfNoExists()) {
				dir.mkdirs();
			}

			if (dir.exists() && dir.isDirectory()) {
				File [] files = dir.listFiles();

				long lastTime = 0;
				String curRun = null;
				int run = 1;
				for (int i = 0; i < files.length; i++) {
					String name = files[i].getName();
					if (name.startsWith(logPrefix) && name.endsWith(logExt)) {
						int offset = name.indexOf(logPrefix) + logPrefix.length();
						int end = name.indexOf("_", offset);

						if (offset != -1 && end != -1) {
							if (files[i].lastModified() >= lastTime) {
								lastTime = files[i].lastModified();
								curRun = name.substring(offset, end);
								try {
									run = Integer.parseInt(curRun);
								}
								catch (NumberFormatException ex) {
									//									// ignore file
								}
							}
						}
					}
				}
				// If we already have the maximum number of runs, start reusing
				// the oldest run number
				if (run >= config.getMaxAppLogs() || (lastTime == 0 && curRun == null)) {
					run = 1;
				}
				else {
					run++;
				}
				appFileName = config.getLogDirectory() + logPrefix
						+ run + "_" + config.getLogSuffix() + logExt;
			}
			else {
				appFileName = "Log_output" + logExt;
			}
			aLayout = new PatternLayout(config.getFilePattern());

			appFA = new RollingFileAppender(aLayout, appFileName, false);
			File f = new File(appFileName);
			f.setLastModified(System.currentTimeMillis());
			appFA.setName("PCSim2Appender");
			appFA.setMaxFileSize(config.getMaxFileSize());
			appFA.setMaxBackupIndex(config.getMaxAppLogs());
			appLog = Logger.getLogger("App");
			appLog.addAppender(appFA);
			appLog.setAdditivity(false);
			appLog.setLevel(Level.ALL);
		}
		catch (IOException io) {
			logError("LogAPI failed to create application log file.");
		}
	}

	/**
	 * Creates the console log interface and appender.
	 *
	 */
	private static void createConsoleLog() {
		Properties p  = new Properties();
		PropertyConfigurator.configure(p);
		cLayout = new PatternLayout(config.getConsolePattern());
		ca = new ConsoleAppender(cLayout, "System.out");
		ca.setName("ConsoleAppender");
		console = Logger.getLogger("Console");
		console.addAppender(ca);
		console.setAdditivity(false);
		Logger root = Logger.getRootLogger();
		root.setLevel(Level.OFF);
		console.setLevel(Level.ALL);
		PropertyConfigurator.configure(p);
	}

	/**
	 * Returns the integer representation of the log level for the
	 * string value
	 * @param level - the string version of the logging level
	 *
	 * @return - the integer equivalent or null
	 */
	private static Integer getLevel(String level) {
		if (level.equals("ALL"))
			return Priority.ALL_INT;
		else if (level.equals("FATAL"))
			return Priority.FATAL_INT;
		else if (level.equals("ERROR"))
			return Priority.ERROR_INT;
		else if (level.equals("WARN"))
			return Priority.WARN_INT;
		else if (level.equals("INFO"))
			return Priority.INFO_INT;
		else if (level.equals("DEBUG"))
			return Priority.DEBUG_INT;
		else if (level.equals("TRACE"))
			return Level.TRACE_INT;
		else if (level.equals("OFF"))
			return Priority.OFF_INT;
		return null;
	}

	private static int getPriorityFromTable(
			ConcurrentHashMap<LogCategory, ConcurrentHashMap<String, Integer>> table,
			LogCategory cat, String sub) {
		synchronized (table) {
			if (sub != null) {
				ConcurrentHashMap<String, Integer> subTable = table.get(cat);
				Integer retVal = null;
				if (subTable != null) {
					retVal = subTable.get(sub);
					if (retVal != null){
						return retVal;
					}
					else {
						retVal = subTable.get("*");
						if (retVal != null){
							return retVal;
						}
					}
				}
				else if (subTable == null){
					subTable = table.get(LogCategory.ALL);
					if (subTable != null) {
						retVal = subTable.get("*");
						if (retVal != null) {
							return retVal;
						}
					}
				}
			}
		}
		return Integer.MAX_VALUE;
	}


	/**
	 * Creates each of the three loggers as
	 * the current configuration dictates.
	 *
	 */
	private static void init() {
		if (config == null) {
			config = new LogAPIConfig();
			logWarn("No configuration data given using default configuration settings.");
		}

		if (LogCategory.APPLICATION == null) {
			LogCategory.updateAppCategories(config.getCategoryClass());
		}

		if (ca == null && consoleCreated) {
			createConsoleLog();
		}
		if (appFA == null) {
			createAppLog();
		}

		if (!initialized)
			readConfig();

		if (monitor == null) {
			monitor = new LogMonitor(SingletonHolder.logAPI);
			monitor.start();
		}
	}
	private static boolean isValidLogger(String loggerStr) {
		if (loggerStr.equals("ALL") ||
				loggerStr.equals("CONSOLE") ||
				loggerStr.equals("APP") ||
				loggerStr.equals("TEST"))
			return true;
		return false;
	}

	private static void logWarn(String warn) {
		if (console != null) {
			console.warn(warn);
		} else {
			System.err.println(warn);
		}
	}

	private static void logError(String error) {
		if (console != null) {
			console.error(error);
		} else {
			System.err.println(error);
		}
	}
}
