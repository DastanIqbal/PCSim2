/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.fsm;

import java.util.Properties;
import java.util.Timer;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.sim.Stacks;
import com.cablelabs.sim.UtilityDistributor;
import com.cablelabs.utility.UtilityAttribute;
import com.cablelabs.utility.UtilityMessage;

public class RecordProvFileListener implements FSMListener {
	
	private Boolean success = null;
	private String value = null;
	private static Send deviceMsg = null;
	private UtilityDistributor ud = Stacks.getUtilDistributor();
	private Timer timer = null;
	private int timeout = 0;
	private ProvTimerTask timerTask = null;

	/**
	 * Logger
	 */
	protected LogAPI logger = LogAPI.getInstance();

	/**
	 * The subcategory to use when logging
	 * 
	 */
	protected String subCat = "RecProv";
		
	public RecordProvFileListener() {
		timeout = SystemSettings.getNoResponseTimeout();
		Properties dut = SystemSettings.getSettings(SettingConstants.DUT);
		if (dut == null) {
			value =  "The DUT settings could not be found. Aborting the rrecording provisioning file.";
			success = false;
		}

		if (deviceMsg == null && dut != null) {
			Send s = new Send(SettingConstants.UTILITY, UtilityConstants.PROV_GET_DEV);
			s.setTarget(null);
			deviceMsg = s;
		}
		return;
	}

	public boolean run() {
		logger.info(PC2LogCategory.PCSim2, subCat, "Set the policy and provisioning file for the DUT.");
		while (success == null) {
			if (timer == null) {
				timer = new Timer(subCat + ":Timer", true);
				timerTask = new ProvTimerTask(this, timeout);
				timer.schedule(timerTask, timeout);
				logger.debug(PC2LogCategory.FSM, subCat,
						"Starting no response timer(" + timer + ") for " + 
						timeout + " msecs.");
				ud.send(deviceMsg, this);
			}

			try {
				Thread.sleep(500);
			}
			catch (Exception e) {
				String err = "Auto provisioning terminated abnormally.";
				logger.fatal(PC2LogCategory.Parser, subCat,	err, e);
			}
		}

		if (timerTask != null) {
			timerTask.cancel();
			timerTask = null;
		}

		if (success == null)
			success = false;
		
		return success;
	}

	public void terminate() {
		success = false;
	}

	public void timerExpired(ProvTimerTask task) {
		if (task == timerTask) {
			success = false;
			value = " the timer expired before receiving a response";
		}
	}
	@Override
	public boolean processEvent(MsgEvent event) {
		if (event.eventName.equals(UtilityConstants.PROV_GET_DEV_ACK)) {
			logger.debug(PC2LogCategory.PCSim2, subCat, "The DUT has responded with the name of the provisioning file.");
			UtilityMessage um = ((UtilityMsg)event).getUtilityEvent().getMessage();
			UtilityAttribute ua = um.getAttribute(UtilityConstants.PROV_FILE_ATTR);
			if  (ua != null) {
				value = ua.getValue();
			}
			success = true;
		}
		else if (event.eventName.equals(UtilityConstants.PROV_GET_DEV_FAILURE)) {
			UtilityMessage um = ((UtilityMsg)event).getUtilityEvent().getMessage();
			UtilityAttribute ua = um.getAttribute(UtilityConstants.ERROR_STRING);
			if  (ua != null) {
				value = ua.getValue();
			}
			else {
				value = "no error message received";
			}
			success = false;
		}
		else if (event.eventName.equals(EventConstants.REGISTERED)||
				event.eventName.equals(TimeoutConstants.TIMER_EXPIRED))	{
			logger.debug(PC2LogCategory.PCSim2, subCat, "Ignoring event(" + event.eventName + ").");
		}
		else {
			logger.error(PC2LogCategory.PCSim2, subCat, 
					"RecordProvFileListener received an unexpected event(" + event.eventName 
					+ "). Aborting provisioning file retrieval.");
			success = false;
		}
		return success;
	}

	@Override
	public SIPMsg findByCallIdAndMethod(String callid, String method, int CSeqNo) {
		return null;
	}

	@Override
	public int getFsmUID() {
		return 0;
	}

	@Override
	public boolean isRegistered() {
		return false;
	}


	@Override
	public String getFSMName() {
		return "RecordProvFile";
	}


	@Override
	public int getCurrentMsgIndex() {
		return 0;
	}
	
	public String getValue() {
		return value;
	}

}


