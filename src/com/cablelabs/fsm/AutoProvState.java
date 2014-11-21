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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.commons.net.tftp.TFTP;
import org.apache.commons.net.tftp.TFTPClient;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.provgen.ProvGen;

public class AutoProvState extends State implements Cloneable {

	private ProvisioningData pd = null;
	
	private String nextState = null;
	/**
	 * Constructor. 
	 * 
	 * @param name - the name of the state which should be END for 
	 * 		any XML documents to parse properly.
	 * @param fsm - the FSM that this state is associated.
	 */
	public AutoProvState(String name, FSM fsm, ProvisioningData pd, String nextState) {
		super(name, fsm);
		this.pd = pd;
		this.owner = fsm;
		this.nextState = nextState;
		
		buildState(fsm);
		
		
	}
	
	@Override
	public void processPrelude(int msgQueueIndex) {
		autoGenerate(msgQueueIndex);
		
	}
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		AutoProvState retval = (AutoProvState)super.clone();
		return retval;
	}
	
	private String autoGenerate(int msgIndexQueue) {
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		Properties dut = SystemSettings.getSettings(SettingConstants.DUT);
		Boolean autoProv = SystemSettings.getBooleanSetting(SettingConstants.AUTO_GENERATE);
		Boolean autoGen = SystemSettings.getBooleanSetting(SettingConstants.AUTO_PROVISION);
		if (autoProv && autoGen) {
			if (pd != null && 
					platform != null && 
					dut != null) {
				String pcscfLabel = dut.getProperty(SettingConstants.PCSCF);
				String macAddr = dut.getProperty(SettingConstants.MAC_ADDRESS);
				String tftpIP = platform.getProperty(SettingConstants.TFTP_SERVER_IP);
				String tftpPort = platform.getProperty(SettingConstants.TFTP_SERVER_PORT);
				String phone1 = dut.getProperty(SettingConstants.PHONE_NUMBER_1);
				String phone2 = dut.getProperty(SettingConstants.PHONE_NUMBER_2);
				String cw = platform.getProperty(SettingConstants.CW_NUMBER);
				if (macAddr != null &&
						pcscfLabel != null && 
						tftpIP != null && 
						tftpPort != null &&
						cw != null) {

					// Next verify the port is not set to zero
					try {
						int port = Integer.parseInt(tftpPort);
						if (port > 0 && port <= 65535) {

							// Next make sure the TFTP Server IP is not set to 0.0.0.0
							if (tftpIP.equals("0.0.0.0")) {
								logger.warn(PC2LogCategory.PCSim2, subCat, 
										"The TFTP Server IP setting in the platform file is not valid. Ending auto generate operation.");
								return null;
							}

							File input = new File(SettingConstants.AUTO_PROV_FILE_DIRECTORY + File.separator + SettingConstants.CW + cw + File.separator + pd.getProvFileName());
							if (input != null) {
								ProvGen pg = new ProvGen(input);
								if (phone1 != null)
									pg.changePhoneNum(SettingConstants.AUTO_GENERATE_PHONE_NUMBER_1, phone1);
								if (phone2 != null)
									pg.changePhoneNum(SettingConstants.AUTO_GENERATE_PHONE_NUMBER_2, phone2);
								Properties pcscf = SystemSettings.getSettings(pcscfLabel);
								if (pcscf != null) {
									String pcscfIP = pcscf.getProperty(SettingConstants.IP);
									if (pcscfIP != null)
										pg.changePCSCF(pcscfIP);
								}
								String newFileName =  macAddr + ".bin";
								if (pg.output(SettingConstants.AUTO_PROV_FILE_DIRECTORY + File.separator + SettingConstants.CW +  cw + File.separator +newFileName)) {
									// Test system
									//File output = new File(SettingConstants.AUTO_PROV_FILE_DIRECTORY + newFileName);
									//File pact = new File(SettingConstants.AUTO_PROV_FILE_DIRECTORY + "chinmaya_base_ph1_pcscf.bin");
									//pg.compare(pact, output);
									// Create a data entry of the issued event
									//ProvisioningData issuePD = new ProvisioningData(macAddr, pd.getPolicyFileName(), newFileName);
									logger.info(PC2LogCategory.FSM, subCat, "AutoProvState beginning to TFTP the new provisioning file.");

									// Next we need to TFTP the file to the server
									TFTPClient client = new TFTPClient();
									File binFile = new File(SettingConstants.AUTO_PROV_FILE_DIRECTORY + File.separator + SettingConstants.CW + cw + File.separator + newFileName);
									if (binFile.isFile() && binFile.canRead()) {
										FileInputStream istrm = new FileInputStream(binFile);
										//InetAddress ia = InetAddress.getByName("10.4.1.37");
										client.open(); // client.open(20003, ia);
										client.sendFile(newFileName, TFTP.BINARY_MODE, istrm, tftpIP, port);
										client.close();
										logger.info(PC2LogCategory.FSM, subCat, "TFTP of the new provisioning file is complete.");
										super.processPrelude(msgIndexQueue);
									}
									else {
										logger.warn(PC2LogCategory.FSM, subCat, 
												"The " + macAddr + ".bin doesn't appear in the " 
														+ SettingConstants.AUTO_PROV_FILE_DIRECTORY + File.separator + SettingConstants.CW + cw + " Ending auto generate operation.");
									}
								}
								else {
									logger.error(PC2LogCategory.FSM, subCat, 
											"AutoProvState could not locate provisioning template file[" 
													+ input.getAbsolutePath() + "].");
								}
							}
							//						else {
							//							logger.info(PC2LogCategory.FSM, subCat, "AutoProvState is terminating because the input directory is null.");
							//						}
						}
						else {
							logger.info(PC2LogCategory.PCSim2, subCat, "AutoProvState is terminating because the port(" + port + ") is less than 0 or greater than 65535.");
						}
					}
					catch (NumberFormatException nfe) {
						logger.warn(PC2LogCategory.FSM, subCat, 
								"AutoProvState is not auto generating a provisioning file because the " 
										+ "TFTP Server Port setting doesn't appear to be a number.");
					}
					catch (UnknownHostException uhe) {
						logger.warn(PC2LogCategory.FSM, subCat, 
								"AutoProvState is not auto generating a provisioning file because the " 
										+ "system encountered an error when attempting to send the file to the TFTP Server.\n" 
										+ uhe.getMessage() + "\n" + uhe.getStackTrace());
					}
					catch (IOException ioe) {
						logger.warn(PC2LogCategory.FSM, subCat, 
								"AutoProvState is not auto generating a provisioning file because the " 
										+ "system encountered an error when attempting to send the file to the TFTP Server.\n" 
										+ ioe.getMessage() + "\n" + ioe.getStackTrace());
					}

				}
				else {
					logger.info(PC2LogCategory.FSM, subCat, "AutoProvState is stopping because one of the values is null.\n" 
							+ "macAddr=" + macAddr + " pcscfLabel=" + pcscfLabel + " tftpIP=" + tftpIP + " tftpPort=" + tftpPort);
				}
			}
			else {
				if (pd != null)
					logger.info(PC2LogCategory.FSM, subCat, "The provisioning data is null, terminating processing.");
				if (platform != null)
					logger.info(PC2LogCategory.FSM, subCat, "The Platform settings is null, terminating processing.");
				if (dut != null)
					logger.info(PC2LogCategory.FSM, subCat, "The DUT settings is null, terminating processing.");
			}
		}
		else {
			Generate g = new Generate(EventConstants.AUTO_PROV_PROMPT, null, this.owner.getName());
			try {
				g.execute(super.api, 0 );
			}
			catch (PC2Exception pce) {
				logger.error(PC2LogCategory.FSM, subCat,
						name + " couldn't generate " 
						+ EventConstants.AUTO_PROV_PROMPT + " event to the FSM.");
			}
		}
		
		return null;
	}
	
	private void buildState(FSM fsm) {
		// String origInit = fsm.getInitialState();
		// Create a transition for the TimerExpired event to go to the original
		// initial state of the FSM.
		Transition t = new Transition(name, ENDState.NAME, TimeoutConstants.TIMER_EXPIRED);
		addTransition(t);
		
		// Add a transition for the REGISTERED event if it can occur before the 
		// TimerExpires
		//t = fsm.getState(origInit).findTransition(EventConstants.REGISTERED);
		if (t != null) {
			// Transition reg = new Transition(NAME, t.getTo(), t.getEvent());
			Transition reg = new Transition(name, name, t.getEvent());
			addTransition(reg);
		}
		
		// Add the transitions appropriate for our operations
		t = new Transition(name, name, UtilityConstants.PROV_DEVICE_MOD_ACK);
		addTransition(t);
		t = new Transition(name, ENDState.NAME, UtilityConstants.PROV_DEVICE_MOD_FAILURE);
		addTransition(t);
		
		Send s = new Send(SettingConstants.UTILITY, UtilityConstants.PROV_DEVICE_MOD);
		s.setTarget(null);
		// Now build up the argument for the policy file
		Mod m = new Mod(SettingConstants.ADD_MOD_TYPE);
		m.setHeader(UtilityConstants.PROV_POLICY_ATTR);
		Literal l = new Literal(pd.getPolicyFileName());
		m.setRef(l);
		s.addModifier(m);
		
		// Now build up the argument for the provisioning file
//		m = new Mod(SettingConstants.ADD_MOD_TYPE);
//		m.setHeader(UtilityConstants.PROV_FILE_ATTR);
//		l = new Literal(pd.getProvFileName());
//		m.setRef(l);
//		s.addModifier(m);
		
		// Next add the send to the prelude of this state
		ActionFactory af =  new ActionFactory(fsm.getSubcategory(), true);
		if (af != null) {
			af.addAction(s);
			af.setOnce(true);
			this.setPrelude(af);
		
		}
		
		// Next we need to create a responses operations
		Responses resp = new Responses();
		ComparisonOp co =  new ComparisonOp("eq");
		UtilityRef utilRef = new UtilityRef("utility");
		utilRef.setMsgType("Response");	
		l = new Literal(UtilityConstants.PROV_DEVICE_MOD_ACK);
		co.setLeft(utilRef);
		co.setRight(l);
		ActionFactory then = new ActionFactory(fsm.getSubcategory(), false);
		Properties dut = SystemSettings.getSettings(SettingConstants.DUT);

		if (then != null && dut != null) {
			String cmIP = dut.getProperty(SettingConstants.CABLE_MODEM_IP_ADDRESS);
			s = new Send(SettingConstants.UTILITY, UtilityConstants.SNMP_SET);
			s.setTarget(null);
			m = new Mod(SettingConstants.REPLACE_MOD_TYPE);
			m.setHeader(UtilityConstants.SNMP_AGENT_IP);
			l = new Literal(cmIP);
			m.setRef(l);
			s.addModifier(m);

			m = new Mod(SettingConstants.ADD_MOD_TYPE);
			m.setHeader(UtilityConstants.SNMP_ARRAY);
			l = new Literal(SettingConstants.SNMP_CM_REBOOT_ARRAY);
			m.setRef(l);
			s.addModifier(m);
			then.addAction(s);
			If curIf = new If();
			curIf.setCond(co);
			curIf.setThenActions(then);
			resp.addActiveOp(curIf);
			this.setResponses(resp);
		}
		t = new Transition(name, nextState, UtilityConstants.SNMP_RESP);
		addTransition(t);
		t = new Transition(name, nextState, UtilityConstants.SNMP_RESP);
		addTransition(t);
		t = new Transition(name, ENDState.NAME, UtilityConstants.SNMP_RESP_FAILURE);
		addTransition(t);
		
		// Create a second set of states to prompt the user to change the provisioning and policy
		// files for the next portion of the test. 
		// This is used when the Auto Generate AND Auto Provision are not set to true in 
		// the Platform Configuration File.
		String logStateName = name + "_PROMPT";
		t = new Transition(name, logStateName, EventConstants.AUTO_PROV_PROMPT);
		addTransition(t);
		State logState = new State(logStateName, fsm);
		LogMsg lm = new LogMsg(logState, "Update the provisioning and/or policy file before continuing the test.");
		lm.setPromptUser(true);
		Generate g = new Generate(EventConstants.AUTO_REBOOT_EVENT, null, fsm.getName());
		ActionFactory laf =  new ActionFactory(fsm.getSubcategory(), true);
		if (laf != null) {
			laf.addAction(lm);
			laf.addAction(g);
			laf.setOnce(true);
			logState.setPrelude(laf);
		
		}
		t = new Transition(logStateName, nextState, EventConstants.AUTO_REBOOT_EVENT);
		logState.addTransition(t);
		try{
			fsm.addState(logState);
		}
		catch (Exception ex) {
			
		}
		
	}
}
