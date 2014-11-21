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


public class ProvListener implements FSMListener {

	//private ProvisioningData pd = null;
	private Boolean success = null;
	private static Send provMsg = null;
	private static Send rebootMsg = null;
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
	protected String subCat = "AutoProv";
	
	public ProvListener(ProvisioningData pd) {
		timeout = SystemSettings.getNoResponseTimeout();
		Properties dut = SystemSettings.getSettings(SettingConstants.DUT);
		if (dut == null) {
			logger.error(PC2LogCategory.PCSim2, subCat, "The DUT settings could not be found. Aborting Auto provisioning.");
			success = false;
		}
		
		// Create the generic message information for each of the messages that the system will send
		if (provMsg == null && dut != null) {
			Send s = new Send(SettingConstants.UTILITY, UtilityConstants.PROV_DEVICE_MOD);
			s.setTarget(null);
			// Now build up the argument for the policy file
			Mod m = new Mod(SettingConstants.ADD_MOD_TYPE);
			m.setHeader(UtilityConstants.PROV_POLICY_ATTR);
			Literal l = new Literal(pd.getPolicyFileName());
			m.setRef(l);
			s.addModifier(m);
			provMsg = s;
		}

		if (rebootMsg == null && dut != null) {
			String cmIP = dut.getProperty(SettingConstants.CABLE_MODEM_IP_ADDRESS);
			Send s = new Send(SettingConstants.UTILITY, UtilityConstants.SNMP_SET);
			s.setTarget(null);
			Mod m = new Mod(SettingConstants.REPLACE_MOD_TYPE);
			m.setHeader(UtilityConstants.SNMP_AGENT_IP);
			Literal l = new Literal(cmIP);
			m.setRef(l);
			s.addModifier(m);

			m = new Mod(SettingConstants.ADD_MOD_TYPE);
			m.setHeader(UtilityConstants.SNMP_ARRAY);
			l = new Literal(SettingConstants.SNMP_CM_REBOOT_ARRAY);
			m.setRef(l);
			s.addModifier(m);
			rebootMsg = s;
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
				ud.send(provMsg, this);
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
		
		return success;
	}
	
	public void terminate() {
		success = false;
	}
	
	public void timerExpired(ProvTimerTask task) {
		if (task == timerTask) {
			success = false;
		}
	}
	@Override
	public boolean processEvent(MsgEvent event) {
		if (event.eventName.equals(UtilityConstants.PROV_DEVICE_MOD_ACK)) {
			logger.debug(PC2LogCategory.PCSim2, subCat, "Starting the reboot of the device");
			ud.send(rebootMsg, this);
		}
		else if (event.eventName.equals(UtilityConstants.SNMP_RESP)) {
			logger.debug(PC2LogCategory.PCSim2, subCat, "The DUT is rebooting with the new policy and provisioning files.");
			success = true;
		}
		else if (event.eventName.equals(UtilityConstants.PROV_GET_DEV_ACK)) {
			logger.debug(PC2LogCategory.PCSim2, subCat, "The DUT has responded with the name of the provisioning file.");
			success = true;
		}
		else if (event.eventName.equals(EventConstants.REGISTERED)||
				event.eventName.equals(TimeoutConstants.TIMER_EXPIRED))	{
			logger.debug(PC2LogCategory.PCSim2, subCat, "Ignoring event(" + event.eventName + ").");
		}
		else {
			logger.error(PC2LogCategory.PCSim2, subCat, 
					"ProvListener received an unexpected event(" + event.eventName 
					+ "). Aborting auto provisioning.");
			success = false;
		}
		return true;
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
		return "AutoProv";
	}
	
	
	@Override
	public int getCurrentMsgIndex() {
		return 0;
	}
	
}
