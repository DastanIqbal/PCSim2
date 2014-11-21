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

import java.io.File;
import java.util.LinkedList;
import java.util.ListIterator;

public class LogMonitor implements Runnable {

	LogAPI logger = null;
	boolean isRunning = true;
	Thread thread = null;
	private LinkedList<MonitorListener> listeners = null;

	public LogMonitor(LogAPI logger) {
		this.logger = logger;
	}
	
	@Override
	public void run() {
		while (isRunning) {
			try {
				Thread.sleep(500);
				File logFile = new File(logger.getLogLevelConfigPath());
				if (logFile.lastModified() > LogAPI.getLastRefresh()){
					logger.info(LogCategory.LOG_MSG, "",
					"Platform detected a change in log configuration file, refreshing.");
					LogAPI.readConfig();
					logger.logConfigSettings();
				}
				// Next invoke the timerTick method of any MonitorListeners
				// that we may have
				if (listeners != null) {
					ListIterator<MonitorListener> iter = listeners.listIterator();
					while (iter.hasNext()) {
						MonitorListener ml = iter.next();
						ml.timerTick();
					}
				}
			}
			catch (Exception ex) {
				logger.warn(LogCategory.LOG_MSG, "",
						"LogMonitor encountered exception while running.\n"
						+ ex.getMessage() + "\n" + ex.getStackTrace());
			}
		}

	}
	public void shutdown() {
		this.isRunning = false;
	}

	public void start() {
		try {
			this.isRunning = true;
			thread = new Thread(this, "LogMonitor");
			thread.setDaemon(true);
			thread.start();
		}
		catch (Exception ex) {
			logger.warn(LogCategory.LOG_MSG, "",
					"LogMonitor encountered an error when starting.\n", ex);
		}
	}

	protected void addListener(MonitorListener ml) {
		if (listeners == null)
			listeners = new LinkedList<MonitorListener>();

		listeners.add(ml);
	}

	protected boolean removeListener(MonitorListener ml) {
		if (listeners == null)
			return false;
		else {
			return listeners.remove(ml);
		}
	}
}
