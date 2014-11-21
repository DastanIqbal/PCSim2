/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.sim;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;

import org.apache.commons.net.tftp.TFTP;
import org.apache.commons.net.tftp.TFTPClient;
import org.xml.sax.SAXParseException;

import com.cablelabs.common.Conversion;
import com.cablelabs.common.Transport;
import com.cablelabs.fsm.Extension;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.InternalMsg;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.MsgRef;
import com.cablelabs.fsm.NetworkElements;
import com.cablelabs.fsm.PC2Exception;
import com.cablelabs.fsm.PresenceModel;
import com.cablelabs.fsm.ProvDatabase;
import com.cablelabs.fsm.ProvListener;
import com.cablelabs.fsm.ProvisioningData;
import com.cablelabs.fsm.RecordProvFileListener;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.gui.PC2PlatformControls;
import com.cablelabs.gui.PC2Result;
import com.cablelabs.gui.PC2UI;
import com.cablelabs.log.LogAPIConfig;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.models.GlobalRegistrar;
import com.cablelabs.models.PC2Models;
import com.cablelabs.models.PresenceServer;
import com.cablelabs.models.Register;
import com.cablelabs.models.Registrar;
import com.cablelabs.models.Session;
import com.cablelabs.models.Stun;
import com.cablelabs.parser.PC2XMLException;
import com.cablelabs.parser.TSDocument;
import com.cablelabs.parser.TSParser;
import com.cablelabs.provgen.ProvGen;


/**
 * This is the initial entry class for the PC 2.0
 * Test Script Platform. It reads in a test script
 * document, parses the information. Reads the 
 * platform and DUT configuration files and executes
 * the test. 
 * 
 * It is in charge of terminating all of the
 * threads and protocol stacks when a fatal error has
 * been detected. 
 * 
 * @author ghassler
 *
 */
public class PCSim2 implements PC2PlatformControls {
	
	/**
	 * The container class for all of the configuration
	 * parameters to be used during the test
	 */
	protected static SystemSettings ss = null;
	
	/**
	 * Logger
	 */
	protected static final LogAPI logger;
	
	
	static {
	    if (LogAPI.isConfigured()) {
	        logger = LogAPI.getInstance();
	    }
	    else {
	        LogAPIConfig config = new LogAPIConfig()
            .setCategoryClass(PC2LogCategory.class)
            .setLogPrefix("PCSim2_");
        
	        logger = LogAPI.getInstance(config);
	    }
	}

	/** 
	 * The current Test Script document to execute.
	 */
	protected static TSDocument doc = null;
	
	/** 
	 * Global test result for this test case.
	 */
	protected static Boolean testPassed = null;
	
	
	/**
	 * A handle to the SIP Stack to notify it when 
	 * to shutdown
	 */
	protected SIPDistributor dist = null;
	
	/** A handle to the Stun Stack to notify it when
	 * to shutdown.
	 */
	protected StunDistributor stun = null;
	
	/**
	 * The protocol stacks container class
	 */
	protected static Stacks stacks = null;
	
	/**
	 * A flag indicating a specific test pair is 
	 * complete.
	 * 
	 */
	private static boolean complete = false;
	
	/**
	 * A flag indicating that the system should shutdown
	 */
	private static boolean shutdown = false;
	
	/**
	 * The global message queue used for comparison and 
	 * analysis by the FSMs.
	 */
	private static MsgQueue q = MsgQueue.getInstance();
	
	/**
	 * Name of the platform settings file to use while executing the
	 * batch of DUT configuration file(s) and Test Script file(s).
	 * 
	 */
	private static String platformSettingsFile = null;
	
	/**
	 * The pending platform settings file to use
	 */
	private String pendingPSFile = null;
	
	/**
	 * The time the platformSettingsFile was last modified
	 */
	private long psLastModified = 0;
	
	/**
	 * List of DUT configuration files to use execute this batch of 
	 * tests.
	 */
	private LinkedList<String> dutConfigFiles = new LinkedList<String>();
	
	/**
	 * List of Test Script files to use execute this batch of 
	 * tests.
	 */
	private LinkedList<String> testScriptFiles = new LinkedList<String>();
	
	/**
	 * This flag specifies whether the dut configuration files
	 * are the primary looping mechanism for conducting the tests
	 * or the test scripts.
	 */
	private boolean dutPrimary = true;
	
	/**
	 * The active DUT Configuration file being used to execute
	 * the active test script file.
	 */
	private static String activeDUTFile = null;
	
	/**
	 * The active test script currently being executed by the
	 * platform
	 */
	private static String activeTestScriptFile = null;
	
	/**
	 * A flag indicating whether global registrar is enabled during
	 * the tests or not.
	 */
	private boolean globalRegEnabled = false;
	
	/**
	 * The XML document to use for the global registrar when enabled.
	 */
	private String globalRegFile = null;
	
	/**
	 * A flag indicating whether presence server is enabled during
	 * the tests or not.
	 */
	private boolean presenceServerEnabled = false;
	
	/**
	 * The XML document to use for the presence server when enabled.
	 */
	private String presenceServerFile = null;
	
	/**
	 * This is the graphical user interface for the PCSim2 platform.
	 * 
	 */
	private static PC2UI ui = null;
	
	/**
	 * This container has all of the policy and provisioning scripts
	 * that the platform should use when conducting a test. 
	 */
	private static ProvDatabase provDB = null;
	
	/**
	 * This flag indicates that the device was auto provisioned and
	 * doesn't need to be rebooted during the script execution.
	 * 
	 */
	private static boolean autoProvisioned = false;
	
	/**
	 * A list of the currently active Models that are performing
	 * the test. Global Registrar models are not currently in
	 * this list.
	 */
	private LinkedList<PC2Models> activeModels = new LinkedList<PC2Models>();
	
	/**
	 * A list of DUT configuration files to add to the global registrar for 
	 * processing. The UI adds files to the list and the main thread reads
	 * entries from the list, loads the configuration into the SystemSettings
	 * class. 
	 */
	private LinkedList<File> dutConfigsToRead = new LinkedList<File>();
	
	/**
	 * A list of DUT configuration files to remove from the global registrar. The
	 * UI adds files to the list and the main thread reads the entries in the 
	 * list and removes them from SystemSettings and terminates any FSMs that 
	 * may have started as a result.
	 * 
	 */
	private LinkedList<File> dutConfigsToRemove = new LinkedList<File>();
	
	/**
	 * This is a flag indicating that the close button for the batch file
	 * has been pressed by the user.
	 */
	private boolean closeBatchFile = false;
	
	/**
	 * The subcategory to use when logging
	 */
	private static String subCat = "";
	
	/**
	 * The sub-directory under the logs directory to store provisioning file information.
	 */
	private static String PROV_FILE_DIRECTORY = "prov_files";
	
	/**
	 * Store the main thread created once the class starts so that we can send any interrupts
	 * when a test completes.
	 * 
	 */
	 private static Thread mainThread = null;
	/**
	 * Constructor.
	 *
	 */
	
	public PCSim2() {
	
	}
	
	@Override
	public boolean addDUTConfig(File f) {
		if (f.exists() && f.canRead() && f.isFile()) {
			synchronized (dutConfigsToRead) {
				dutConfigsToRead.add(f);
			}
			logger.debug(PC2LogCategory.Parser, subCat,
					"Adding client defined by " + f.getName() 
					+ " to list of accepted clients for global registrar");
			return true;
		}
		else {
			String reason = validateFile(f.getAbsolutePath());
			logger.warn(PC2LogCategory.Parser, subCat,
					"Couldn't process DUT configuration for global registrar because " 
					+ reason);
		}
		return false;
	}
	
	/**
	 * This method locates any UEs that may need to be updated and added to the 
	 * registrar settings.
	 *
	 */
	private void addRegistrarClients() {
		if (globalRegEnabled) {
			Enumeration<String> keys = SystemSettings.getPlatformLabels();
			if (keys != null) {
				while (keys.hasMoreElements()) {
					String key = keys.nextElement();
					if (key.startsWith("UE") && !key.contains("@")) {
						Properties ue = SystemSettings.getSettings(key);
						String sim = ue.getProperty(SettingConstants.SIMULATED);
						if (sim != null && (sim.equalsIgnoreCase("false") ||
								sim.equalsIgnoreCase("no")||
								sim.equalsIgnoreCase("disable"))) {
							Properties p = SystemSettings.getSettings(key);
							LinkedList<String> puis = createRegistrarLabels(p);
							Stacks.addSIPRegistrarClient(key, puis);
						}
					}
				}
			}
		}
	}
	
	/**
	 * This method determines if the provisioning file used to conduct a test needs to
	 * be generated from one of the templates prior to starting the test.
	 * @return - the MAC address used for the name of the file
	 */
	private String autoGenerate(ProvisioningData pd) {
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		Properties dut = SystemSettings.getSettings(SettingConstants.DUT);
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
				// First see if we have already issued a generated file.
				if (!provDB.issued(macAddr, pd)) {
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

							File input = new File(SettingConstants.AUTO_PROV_FILE_DIRECTORY + File.separator 
									+ SettingConstants.CW + cw + File.separator + pd.getProvFileName());
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
									logger.info(PC2LogCategory.PCSim2, subCat, "Beginning to TFTP the new provisioning file.");
									provDB.setIssuedData(macAddr, pd);

									// Next we need to TFTP the file to the server
									TFTPClient client = new TFTPClient();
									File binFile = new File(SettingConstants.AUTO_PROV_FILE_DIRECTORY + File.separator + SettingConstants.CW + cw + File.separator + newFileName);
									if (binFile.isFile() && binFile.canRead()) {
										FileInputStream istrm = new FileInputStream(binFile);
										//InetAddress ia = InetAddress.getByName("10.4.1.37");
										client.open(); // client.open(20003, ia);
										client.sendFile(newFileName, TFTP.BINARY_MODE, istrm, tftpIP, port);
										client.close();
										logger.info(PC2LogCategory.PCSim2, subCat, "TFTP of the new provisioning file is complete.");
										return macAddr;
									}
									else {
										logger.warn(PC2LogCategory.PCSim2, subCat, 
												"The " + macAddr + ".bin doesn't appear in the " 
												+ SettingConstants.AUTO_PROV_FILE_DIRECTORY + File.separator + SettingConstants.CW + cw + " Ending auto generate operation.");
									}
								}
								else {
									logger.error(PC2LogCategory.PCSim2, subCat, 
											"PCSim2 could not locate provisioning template file[" 
											+ input.getAbsolutePath() + "].");
								}
							}
//							else {
//								logger.info(PC2LogCategory.PCSim2, subCat, "Auto provisioning is terminating because the input directory is null.");
//							}
						}
						else {
							logger.info(PC2LogCategory.PCSim2, subCat, "Auto provisioning is terminating because the port(" + port + ") is less than 0 or greater than 65535.");
						}
					}
					catch (NumberFormatException nfe) {
						logger.warn(PC2LogCategory.PCSim2, subCat, 
								"PCSim2 is not auto generating a provisioning file because the " 
								+ "TFTP Server Port setting doesn't appear to be a number.");
					}
					catch (UnknownHostException uhe) {
						logger.warn(PC2LogCategory.PCSim2, subCat, 
								"PCSim2 is not auto generating a provisioning file because the " 
								+ "system encountered an error when attempting to send the file to the TFTP Server.\n" 
								+ uhe.getMessage() + "\n" + uhe.getStackTrace());
					}
					catch (IOException ioe) {
						logger.warn(PC2LogCategory.PCSim2, subCat, 
								"PCSim2 is not auto generating a provisioning file because the " 
								+ "system encountered an error when attempting to send the file to the TFTP Server.\n" 
								+ ioe.getMessage() + "\n" + ioe.getStackTrace());
					}
				}
				else {
					logger.info(PC2LogCategory.PCSim2, subCat, "Auto provisioning detected the same same provisioning template is already in use, skipping operation.");
		
				}
			}
			else {
				logger.info(PC2LogCategory.PCSim2, subCat, "Auto provisioning is stopping because one of the values is null.\n" 
						+ "macAddr=" + macAddr + " pcscfLabel=" + pcscfLabel + " tftpIP=" + tftpIP + " tftpPort=" + tftpPort);
			}
		}
		else {
			if (pd != null)
				logger.info(PC2LogCategory.PCSim2, subCat, "The provisioning data is null, terminating processing.");
			if (platform != null)
				logger.info(PC2LogCategory.PCSim2, subCat, "The Platform settings is null, terminating processing.");
			if (dut != null)
				logger.info(PC2LogCategory.PCSim2, subCat, "The DUT settings is null, terminating processing.");
		}
		
		
		return null;
	}
	/**
	 * This method determines if any provisioning needs to take place for the
	 * test prior to the beginning of the execution.
	 * @return
	 */
	private ProvisioningData autoProvision(String testCase) {
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		Properties dut = SystemSettings.getSettings(SettingConstants.DUT);
		if (platform != null && dut != null) {
			String deviceType = dut.getProperty(SettingConstants.DEVICE_TYPE);
			boolean autoProv = SystemSettings.resolveBooleanSetting(platform.getProperty(SettingConstants.AUTO_PROVISION));
			if (autoProv && deviceType.equals(SettingConstants.UE)) {
				// Now that we know we are testing a UE and auto provisioning is 
				// active, see if we have the policy and provisioning files for the 
				// DUT
				ProvisioningData pd = provDB.getData(testCase);
				if (pd != null) {
					logger.info(PC2LogCategory.PCSim2, subCat, 
							"Using the policy and provisioning file for the DUT to " 
							+ pd.getPolicyFileName() + " and " + pd.getProvFileName() 
							+ " respectively.");
					return pd;
				}
			}
			 
		}
		return null;
	}
	
	public static boolean autoProvisionedDevice() {
		//Only allow the value to be obtained once
		boolean result = autoProvisioned;
		logger.debug(PC2LogCategory.PCSim2, subCat, "Returning " + autoProvisioned + " for auto provisioned reboot flag.");
		autoProvisioned = false;
		return result;
	}
	/** 
	 * This method creates a list of the various alias labels that are used for
	 * the registrar processing by the application for the various DUTs that 
	 * may attempt to register.
	 * 
	 * @param p - the properties class for the network element
	 * @return
	 */
	private static LinkedList<String> createRegistrarLabels(Properties p) {
		String pui = p.getProperty(SettingConstants.PUI);
		String pui2 = p.getProperty(SettingConstants.PUI2);
		LinkedList<String> puis = new LinkedList<String>();
		if (pui != null)
			puis.add(pui);
		if (pui2 != null)
			puis.add(pui2);
		return puis;
	}
	
	/** 
	 * This method is invoked when the user closes a batch file. 
	 * It causes the platform to remove all of the test scripts
	 * and configuration files from the system. It also causes all
	 * of the registrar settings to be cleared.
	 */
	@Override
	public void closeBatch() {
		closeBatchFile = true;
	}
	
	/**
	 * This method determines whether the global registration needs to be started
	 * 
	 */
	private boolean configGlobalRegistrar() {
		globalRegEnabled = SystemSettings.getBooleanSetting("Global Registrar");
		if (globalRegEnabled) {
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			String grName = platform.getProperty(SettingConstants.GLOBAL_REGISTRAR_FSM);
			if (globalRegFile == null) {
				globalRegFile = grName;
			}
			else if (globalRegFile.equals(grName)) {
				return true;
			}
			else {
				if (stacks != null)
					stacks.shutdownGlobalRegistrars();
				globalRegFile = grName;
			}	
			if (globalRegFile != null) {
				File gr = new File(globalRegFile);
				if (gr != null && gr.exists() && 
						gr.canRead() && gr.isFile()) {
					TSParser tsp = new TSParser(false);
					try {
						logger.info(PC2LogCategory.Parser, subCat,
								"Parsing document " + globalRegFile + " for GlobalRegistrar processing.");
						TSDocument grDoc = tsp.parse(globalRegFile);
						LinkedList<FSM> fsms = grDoc.getFsms();
						if (fsms.size() == 1) {
							// Initialize the settings that can be overwritten from
							// within the document 
							setExtensions(fsms);
							FSM grFsm = fsms.getFirst();
							String transport = grFsm.getModel().getProperty(SettingConstants.TRANSPORT_PROTOCOL);
							Transport t = Transport.UDP;
							if (transport != null) {
								if (transport.equals(Transport.UDP.toString()))
									t = Transport.UDP;
								else if (transport.equals(Transport.TCP.toString()))
									t = Transport.TCP;
								else if (transport.equals(Transport.TLS.toString()))
									t = Transport.TLS;
							}
							else {
								if (platform != null) {
									transport = platform.getProperty(SettingConstants.SIP_DEF_TRANPORT_PROTOCOL);
									if (transport != null) {
										if (transport.equals(Transport.UDP.toString()))
											t = Transport.UDP;
										else if (transport.equals(Transport.TCP.toString()))
											t = Transport.TCP;
										else if (transport.equals(Transport.TLS.toString()))
											t = Transport.TLS;
									}
								}
							}
							GlobalRegistrar.setMasterFSM(grFsm, t);

							return true;
							
						}
					}
					catch (PC2XMLException pe){
						String err = "\n** Parsing error in file \n    " + pe.getFileName() +
						" at line " + pe.getLineNumber();
						if (pe.getSystemId() != null) {
							err += ", uri " + pe.getSystemId();
						}
						if (pe.getPublicId() != null) {
							err +=  ", public " + pe.getPublicId();
						}
						err += "\n";

						logger.fatal(PC2LogCategory.Parser, subCat,
								err, pe);
					}
					catch (SAXParseException spe){
						String err = "\n** Parsing error in file \n    " + globalRegFile
						+ " at line " + spe.getLineNumber();
						if (spe.getSystemId() != null) {
							err += ", uri " + spe.getSystemId();
						}
						if (spe.getPublicId() != null) {
							err +=  ", public " + spe.getPublicId();
						}
						err += "\n";
						
						logger.fatal(PC2LogCategory.Parser, subCat,
								err, spe);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
//					if (gr == null) {
//						logger.fatal(PC2LogCategory.Parser, subCat,
//								"The platform configuration file doesn't appear to have a " 
//								+ "value for the \"Global Registrar FSM\" setting.");
//					}
					if (!gr.exists()) {
						logger.fatal(PC2LogCategory.Parser, subCat,
								"The \"Global Registrar FSM\" setting=[" + gr 
								+ "] doesn't appear to define a valid path or file name.");
					}
					if (!gr.canRead()) { 
						logger.fatal(PC2LogCategory.Parser, subCat,
								"The \"Global Registrar FSM\" setting=[" + gr 
								+ "] can not be read by the system.");
					} 
					if (!gr.isFile()) {
						logger.fatal(PC2LogCategory.Parser, subCat,
								"The \"Global Registrar FSM\" setting=[" + gr 
								+ "] doesn't appear to define a file.");
					}
				}
			
			}
		}
		return false;
	}
	
	/**
	 * This method determines whether the global presence server needs to be started
	 * 
	 */
	private boolean configPresenceServer() {
		presenceServerEnabled = SystemSettings.getBooleanSetting("Presence Server");
		if (presenceServerEnabled) {
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			String psName = platform.getProperty(SettingConstants.PRESENCE_SERVER_FSM);
			if (presenceServerFile == null) {
				presenceServerFile = psName;
			}
			else if (presenceServerFile.equals(psName)) {
				return true;
			}
			else {
				if (stacks != null)
					stacks.shutdownPresenceServer();
				presenceServerFile = psName;
			}	
			if (presenceServerFile != null) {
				File ps = new File(presenceServerFile);
				if (ps != null && ps.exists() && 
						ps.canRead() && ps.isFile()) {
					TSParser tsp = new TSParser(false);
					try {
						logger.info(PC2LogCategory.Parser, subCat,
								"Parsing document " + presenceServerFile + " for PresenceServer processing.");
						TSDocument psDoc = tsp.parse(presenceServerFile);
						LinkedList<FSM> fsms = psDoc.getFsms();
						if (fsms.size() == 1) {
							FSM f = fsms.getFirst();
							if (f.getModel() instanceof PresenceModel) {
								PresenceModel model = (PresenceModel)f.getModel();
								// Initialize the settings that can be overwritten from
								// within the document 
								setExtensions(fsms);
								PresenceServer server = PresenceServer.getInstance(f, model.getElements());
								if (server != null) {
									Stacks.setPresenceServer(server);
									server.init();
									return true;
								}
							}
							
						}
					}
					catch (PC2XMLException pe){
						String err = "\n** Parsing error in file \n    " + pe.getFileName() +
						" at line " + pe.getLineNumber();
						if (pe.getSystemId() != null) {
							err += ", uri " + pe.getSystemId();
						}
						if (pe.getPublicId() != null) {
							err +=  ", public " + pe.getPublicId();
						}
						err += "\n";

						logger.fatal(PC2LogCategory.Parser, subCat,
								err, pe);
					}
					catch (SAXParseException spe){
						String err = "\n** Parsing error in file \n    " + presenceServerFile
						+ " at line " + spe.getLineNumber();
						if (spe.getSystemId() != null) {
							err += ", uri " + spe.getSystemId();
						}
						if (spe.getPublicId() != null) {
							err +=  ", public " + spe.getPublicId();
						}
						err += "\n";
						
						logger.fatal(PC2LogCategory.Parser, subCat,
								err, spe);
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				else {
//					if (ps == null) {
//						logger.fatal(PC2LogCategory.Parser, subCat,
//								"The platform configuration file doesn't appear to have a " 
//								+ "value for the \"Presence Server FSM\" setting.");
//					}
					if (!ps.exists()) {
						logger.fatal(PC2LogCategory.Parser, subCat,
								"The \"Presence Server FSM\" setting=[" + ps 
								+ "] doesn't appear to define a valid path or file name.");
					}
					if (!ps.canRead()) { 
						logger.fatal(PC2LogCategory.Parser, subCat,
								"The \"Presence Server FSM\" setting=[" + ps 
								+ "] can not be read by the system.");
					} 
					if (!ps.isFile()) {
						logger.fatal(PC2LogCategory.Parser, subCat,
								"The \"Presence Server FSM\" setting=[" + ps 
								+ "] doesn't appear to define a file.");
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * Displays the current results state for a test pair
	 */
	public static void displayResults() {
		if (isGUIActive() && 
				activeDUTFile != null &&
				activeTestScriptFile != null) {
			PC2Result r = PC2Result.TESTING;
			if (complete) {
				if (testPassed == null || testPassed)
					r = PC2Result.PASSED;
				else
					r = PC2Result.FAILED;
			}
			ui.setTestResults(r, ss.getCurrentRunNumber(), activeTestScriptFile, activeDUTFile);
		}
	}
	/**
	 * This method performs the actual task of starting the test
	 * and waiting for it to complete.
	 *
	 */
	private void executeTest() {
		// First of all make sure that the we reset the 
		// complete flag before starting the test
		complete = false;
		testPassed = null;

		File dut = new File(activeDUTFile);
		String [] settingChanges = ss.setDUTProperties(dut.getAbsolutePath());
		if (settingChanges == null) {
			logger.fatal(PC2LogCategory.Parser, subCat,
			"DUTConfiguration property information could not be found and loaded.");
			setTestPassed(false);
			setTestComplete();
		}
		else {
			
			stacks.updateRegistrarDisplay(settingChanges);

			boolean parsed = parseAndStart();
			// Before starting test log if the Global Registrar is running or not.
			if (globalRegEnabled) {
				logger.info(PC2LogCategory.PCSim2, subCat, "The global registrar is enabled.");
				
			}
			else 
				logger.info(PC2LogCategory.PCSim2, subCat, "The global registrar is disabled.");
			
			if (activeModels.size() > 0 && parsed) {
				ui.startingTest(ss.getCurrentRunNumber(), activeTestScriptFile, activeDUTFile);
				logger.info(PC2LogCategory.LOG_MSG, "",
						"Commencing test \"" + activeTestScriptFile + "\" for DUT \"" + activeDUTFile +"\".");
				
				while (!complete) {
					
					try {
						Thread.sleep(250);
					}
					catch (InterruptedException ie) {
						
					}
					catch (Exception e) {
						String err = "Simulator failed to sleep. Terminating test.";
						logger.fatal(PC2LogCategory.Parser, subCat,	err, e);
						setTestPassed(false);
						setTestComplete();
					}
				}
				
				// Determine if we are using the new validation or not
				boolean useValidate = doc.useValidate();
				if (useValidate) {
					if (testPassed == null)
						testPassed = doc.validateTest();
					else 
						testPassed &= doc.validateTest();
				}
				else if (testPassed == null)
					testPassed = true;
				
				// Document the test results
				// Use the logMsg category to insure that the information
				// is displayed
				if (testPassed) {
					logger.info(PC2LogCategory.LOG_MSG, subCat,
							"Test \"" + doc.getName() + "\" Passed.\n" 
							+ ((useValidate) ? doc.getTestStats() : "")
							+ logger.dumpLogStats()); 
				}
				else
					logger.info(PC2LogCategory.LOG_MSG, subCat,
							"Test \"" + doc.getName() + "\" Failed.\n" 
							+ ((useValidate) ? doc.getTestStats() : "")
							+ logger.dumpLogStats());
				displayResults();
				generateResults(doc);
			}
			else {
				setTestPassed(false);
				String err = "PCSim2 failed to parse the script or there were no models to conduct the test." 
					+ "Terminating test. Declaring test case failure.";
				logger.fatal(PC2LogCategory.Model, subCat, err);
				
				setTestComplete();
				displayResults();
				if (doc != null)
					generateResults(doc);
				
			}
		}
		// Now clean up for the next test
		ss.reset();
		Stacks.reset();
		doc = null;
		autoProvisioned = false;
		logger.debug(PC2LogCategory.PCSim2, subCat, "Resetting the auto provisioned reboot flag to " + autoProvisioned + ".");
		// We need to clear the message queue of any
		// models that only exist for the length of the
		// test. Information in a global model should 
		// remain for operations that could occur between
		// tests, eg. registrar
		ListIterator<PC2Models> iter = activeModels.listIterator();
		LinkedList<Integer> fsmUIDs = new LinkedList<Integer>();
		while (iter.hasNext()) {
			PC2Models model = iter.next();
			fsmUIDs.add(model.getFsmUID());
		}
		if (!fsmUIDs.isEmpty())
			q.reset(fsmUIDs);
		
		if (activeModels != null) {
			
			activeModels.clear();
		}
		logger.clear();
		
		
		
	}
	/**
	 * Initializes and executes the test defined by the
	 * file in the arguments.
	 * 
	 * @param args - the fully-qualified name of the test
	 * 			script document.
	 */
	public void init(String [] args) {
		PCSim2.ui = new PC2UI(this);

	if (logger == null) {
			System.out.println("LogAPI failed to create application log file. GLH PC2UI.vendor");
			setTestComplete();
			return;
		}
		else {
			// This is a hack to get the console's background color to be white
			// from the minute the window is launched
			System.out.println(
					"\t" + PC2UI.vendor  + "   PCSim2  v." + PC2UI.version  + " " + PC2UI.build
							+ "                                                                       "
							+ "                                                                       " 
							+ "                                                                       " 
							+ "                                                                    \n\t");
		}
		
		if (ui != null) {
			ss = SystemSettings.getInstance();
			PC2UI.init();
			
		}
// GLH - This operation is deprecated since we are not going to support headless system.
//		else if (processArgs(args)) {
//			
//			ss = SystemSettings.getInstance();
//			if (!ss.loadPlatformSettings(platformSettingsFile)) {
//				logger.fatal(PC2LogCategory.Parser, subCat,
//						"PCSim2 encountered error while trying to read the Platform Settings information.");
//				setTestPassed(false);
//				setTestComplete();
//				//shutdown();
//				return;
//			}
//			else {
//				startServers();
//			}
//			
//			String neLabel = ss.loadDUTSettings(dutConfigFiles.getFirst(), false, false);
//			if (neLabel == null) {
//				logger.fatal(PC2LogCategory.Parser, subCat,
//						"PCSim2 encountered error while trying to read the DUT Configuration information.");
//				setTestPassed(false);
//				setTestComplete();
//				//shutdown();
//			}
//			
//			//dist = new SIPDistributor();
//			//dist.init();
//		}
		
//		dist.test();		
//		if (parse()) {
//		run();
//		//dist.test();
//		}
//		else
//		shutdown();
//		}
//		else 
//		shutdown();
		run();
	}
	
	@Override
	public void injectUserEvent(String fsm, String event) {
		if (fsm != null && event != null) {
			ListIterator<PC2Models> iter = activeModels.listIterator();
			boolean found = false;
			if (iter.hasNext()) {
				while (iter.hasNext() && !found) {
					PC2Models m = iter.next();
					if (m.getFSMName().equals(fsm)) {
						found = true;
						InternalMsg msg = new InternalMsg(m.getFsmUID(), 
								System.currentTimeMillis(), 
								LogAPI.getSequencer(), 
								event);
						boolean success = m.processEvent(msg);
						if (success) {
							logger.info(PC2LogCategory.PCSim2, subCat,
									"Delivered user event(" + event + ") to FSM(" + fsm + ").\n");
						}
						else {
							logger.warn(PC2LogCategory.PCSim2, subCat,
									"Failed delivering user event(" + event + ") to FSM(" + fsm + ") because FSM is null.\n");
						}
					}
				}
			}
			else {
				logger.warn(PC2LogCategory.PCSim2, subCat,
						"Failed delivering user event(" + event + ") to FSM(" + fsm + ") because there are no active FSM(s) running.\n");
			}
		}
		else {
			if (fsm == null)
				logger.warn(PC2LogCategory.PCSim2, subCat,
						"Could not deliver user event(" + event + ") to FSM(" + fsm + ") because FSM is null.\n");
			if (event == null)
				logger.warn(PC2LogCategory.PCSim2, subCat,
						"Could not deliver user event(" + event + ") to FSM(" + fsm + ") because event is null.\n");
		}
	}
	
	/**
	 * This method creates the T.I.M results file for the a specific
	 * test case.
	 */
	private void generateResults(TSDocument doc) {
		String fileName = doc.getLogFileName();
		int index = fileName.lastIndexOf("_ss.log");
		String timFileName = fileName.substring(0,index)
		+ "_ss.res";
		File tim = new File(timFileName);
		if (!tim.exists()) {
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			Properties dut = SystemSettings.getSettings("DUT");
			try {
				FileOutputStream output = null;
				if (tim.createNewFile()) {
					output = new FileOutputStream(tim);
				}	
				else {
					output = new FileOutputStream((timFileName+"_"+System.currentTimeMillis()));
				}	
				
				String testerName = platform.getProperty(SettingConstants.TESTER_NAME);
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
				Date stop = new Date();
				String cw = platform.getProperty(SettingConstants.CW_NUMBER);
				if (!cw.startsWith("CW"))
					cw = "CW" + cw;
				String result = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
					+ "<res-document xmlns=\"http://cablelabs.com/TEPResultDocument\" version=\"1.0\">\n\t"
					+ "<execution method=\"automatic\" start=\""
					+ sdf.format(doc.getStart()) + "\" stop=\"" 
					+ sdf.format(stop) + "\" />\n\t"
					+ "<tester username=\"" + testerName + "\" />\n\t"
					+ "<certwave name=\"" + cw + "\" />\n\t"
					+ "<test-result type=\"" 
					+ platform.getProperty(SettingConstants.DUT_SUBGROUP) + "\" name=\""
					+ doc.getNumber() + "\"\n\tproduct=\"" 
					+ dut.getProperty(SettingConstants.DUT_VENDOR)
					+ "\" result=\"" + ((testPassed == null || testPassed) ? "PASS" : "FAIL") 
					+ "\"\n\tunit=\"" + dut.getProperty(SettingConstants.PRODUCT_UNIT) + "\"/>\n"
					+ "</res-document>";
				if (output != null) {
					output.write(result.getBytes());
					output.close();
				}
//				else 
//					logger.fatal(PC2LogCategory.Parser, subCat,
//							"Couldn't write TIM file! Writing to log file for preservation!\n" + result);
			}
			catch (IOException ioe) {
				logger.error(PC2LogCategory.Parser, subCat,
						"Could not create new TIM file[" + timFileName + "].");
			}
			
			String tftpIP = platform.getProperty(SettingConstants.TFTP_SERVER_IP);
			String tftpPort = platform.getProperty(SettingConstants.TFTP_SERVER_PORT);
			
			if (tftpIP != null && 
					tftpPort != null) {
				boolean recProv = SystemSettings.resolveBooleanSetting(platform.getProperty(SettingConstants.RECORD_PROVISIONING_FILE));
				if (recProv && dut != null)
					try {
						RecordProvFileListener rpfl = new RecordProvFileListener();
						boolean success = rpfl.run();
						String provFile = rpfl.getValue();
						if (success) {

							int port = Integer.parseInt(tftpPort);
							if (port > 0 && port <= 65535) {

								// Next make sure the TFTP Server IP is not set to 0.0.0.0
								if (tftpIP.equals("0.0.0.0")) {
									logger.warn(PC2LogCategory.PCSim2, subCat, 
									"The TFTP Server IP setting in the platform file is not valid. Ending auto generate operation.");
								}
								else {
									// Next we need to TFTP the file from the server
									TFTPClient client = new TFTPClient();
									int dirIndex = fileName.lastIndexOf("/", index);
									File dir = new File(fileName.substring(0,dirIndex) + PROV_FILE_DIRECTORY);
									if (dir.exists() && !dir.isDirectory()) {
										logger.error(PC2LogCategory.PCSim2, subCat, 
										"The path " + dir.getPath() + " is not a directory. Terminating the recording of the provisioning file.");
									}
									File binFile = new File(fileName.substring(0,dirIndex + 1 ) +  PROV_FILE_DIRECTORY 
											+ File.separator + fileName.substring(dirIndex+1, index) + "_prov.bin");
									boolean exists = false;
									if (!binFile.exists())
										exists = binFile.createNewFile();
									if (exists && binFile.canWrite()) {
										FileOutputStream ostrm = new FileOutputStream(binFile);
										//InetAddress ia = InetAddress.getByName("10.4.1.37");
										client.open(); // client.open(20003, ia);
										client.receiveFile(provFile, TFTP.BINARY_MODE, ostrm, tftpIP, port);
										client.close();
										logger.info(PC2LogCategory.PCSim2, subCat, "TFTP of the record provisioning file is complete.");
									}
									else {
										logger.warn(PC2LogCategory.PCSim2, subCat, 
												"The system could not TFTP the provisioning file because TFTP address is " + tftpIP + ".");
									}
								}
							}
							else {
								logger.warn(PC2LogCategory.PCSim2, subCat, 
										"Recording of the provisioning file is terminating because the port(" 
										+ port + ") is less than 0 or greater than 65535.");
							}
						}
						else {
							logger.warn(PC2LogCategory.PCSim2, subCat, 
									"Recording of the provisioning file is terminating because PACT returned an error string of \"" 
									+ provFile + "\".");
						}
					}
				catch (NumberFormatException nfe) {
					logger.warn(PC2LogCategory.PCSim2, subCat, 
							"PCSim2 is not auto generating a provisioning file because the " 
							+ "TFTP Server Port setting doesn't appear to be a number.");
				}
				catch (UnknownHostException uhe) {
					logger.warn(PC2LogCategory.PCSim2, subCat, 
							"PCSim2 is not auto generating a provisioning file because the " 
							+ "system encountered an error when attempting to send the file to the TFTP Server.\n" 
							+ uhe.getMessage() + "\n" + uhe.getStackTrace());
				}
				catch (IOException ioe) {
					logger.warn(PC2LogCategory.PCSim2, subCat, 
							"PCSim2 is not auto generating a provisioning file because the " 
							+ "system encountered an error when attempting to send the file to the TFTP Server.\n" 
							+ ioe.getMessage() + "\n" + ioe.getStackTrace());
				}
			} 
		}
	}

	
	/**
	 * Gets the global message queue for the test.
	 *
	 */
	public static MsgQueue getMsgQueue() {
		return q;
	}

	/**
	 * Gets the current name of the Platform Configuration
	 * file being used to conduct tests.
	 * @return
	 */
	public static String getPlatformSettingsFileName() {
		return platformSettingsFile;
	}
	/**
	 * Gets the name attribute within the currently active
	 * test script if the document is not null.
	 * @return
	 */
	public static String getTestName() {
		if (doc != null)
			return doc.getName();
		return null;
	}
	public static PC2UI getUI() {
		return ui;
	}
	/**
	 * This method displays the command-line arguments for the
	 * platform. This method may soon be depricated.
	 *
	 * 
	 */
//	private void help() {
//		String msg = "java -jar PC_Sim2-1.0-beta_1.jar [options]\n\n" 
//			+ "\toptions:\n"
//			+ "\t-p <platform settings file> to use to conduct test(s).\n"
//			+ "\t-t <test script file(s)>\n\t\t"
//			+ "where <test script file(s)> is space separated list of file(s).\n"
//			+ "\t-d <DUT configuration file(s)>\n\t\t"
//			+ "where <DUT configuration file(s)> is space separated list of file(s).\n"
//			+ "\t-h help\n\n"
//			+ "NOTE: All files need to be include the absolute or relative-path\n" 
//			+ "       as part of the file name.";
//		logger.info(PC2LogCategory.Parser, subCat, msg);
//	}
//	private boolean isArgOption(String arg) {
//		if (arg.equals("-t") ||
//				arg.equals("-p") ||
//				arg.equals("-d")) {
//			return true;
//		}
//		return false;
//	}
	/**
	 * This method specifies whether the GUI has been started or 
	 * if the application is command-line only.
	 */
	public static boolean isGUIActive() {
		if (ui != null)
			return true;
		return false;
	}
	
	private static void logNE(String ne, Properties p) {
	    // Only log the ip|port data for non-simulated elements.
	    if (SystemSettings.resolveBooleanSetting(p.getProperty(SettingConstants.SIMULATED))){
	        return;
	    }
	    
	    String ip1 = p.getProperty(SettingConstants.IP);
        if (Conversion.isIPv6Address(ip1)) {
            ip1 = Conversion.makeAddrURL(ip1, p.getProperty(SettingConstants.IPv6_ZONE));
        }
        
        String ip2 = p.getProperty(SettingConstants.IP2);
        String[] ips;
        if (!ip1.equalsIgnoreCase(ip2)) {
            if (Conversion.isIPv6Address(ip2)) {
                ip2 = Conversion.makeAddrURL(ip2, p.getProperty(SettingConstants.IPv6_ZONE));
            }
            ips = new String[] { ip1, ip2 };
        }
        else {
            ips = new String[] { ip1 };
        }
        
        String udp = p.getProperty(SettingConstants.UDP_PORT);
        String tcp = p.getProperty(SettingConstants.TCP_PORT);
        String tls = p.getProperty(SettingConstants.TLS_PORT);
        String sdp1 = p.getProperty(SettingConstants.SDP_PORT);
        String sdp2 = p.getProperty(SettingConstants.SDP_PORT2);
        
        StringBuilder ipPorts = new StringBuilder();
        boolean addComma = false;
        for (String ip: ips) {
            if (udp != null && udp.trim().length() > 0 && !udp.equals("0")) {
                if (addComma) ipPorts.append(", ");
                ipPorts.append("UDP " + ip + "|" + udp);
                addComma = true;
            }
             
            if (tcp != null && tcp.trim().length() > 0 && !tcp.equals("0")) {
                if (addComma) ipPorts.append(", ");
                ipPorts.append("TCP " + ip + "|" + tcp);
                addComma = true;
            }
            
            if (tls != null && tls.trim().length() > 0 && !tls.equals("0")) {
                if (addComma) ipPorts.append(", ");
                ipPorts.append("TLS " + ip + "|" + tls);
                addComma = true;
            }
            
            if (sdp1 != null && sdp1.trim().length() > 0 && !sdp1.equals("0")) {
                if (addComma) ipPorts.append(", ");
                ipPorts.append("SDP " + ip + "|" + sdp1);
                addComma = true;
            }
            
            if (sdp2 != null && sdp2.trim().length() > 0 && !sdp2.equals("0")) {
                if (addComma) ipPorts.append(", ");
                ipPorts.append("SDP2 " + ip + "|" + sdp2);
                addComma = true;
            }
        }
        
        
        logger.info(PC2LogCategory.LOG_MSG, subCat, "IP|Ports of " + ne + " : " + ipPorts.toString());
	}
	
	private static void logPackage() {
		Package p = Package.getPackage("com.cablelabs.sim");
		if (p == null) {
			logger.info(PC2LogCategory.Parser, subCat, 
					"com.cablelabs.sim not loaded");
			return;
		}
		
		logger.info(PC2LogCategory.Parser, subCat,
				"com.cablelabs.sim version " + 
				p.getSpecificationVersion() +
				" build-" + 
				p.getImplementationVersion());
//		if (logFile.exists()) 	
//			logDebugInfo(logFile);
		logger.logConfigSettings();
		
	}
	
	/**
	 * Parses the XML Document passed into the application.
	 * 
	 * @param args - the fully-qualified name of the test
	 * 			script document.
	 * @return - true if the document parsed successfully, 
	 * 			false otherwise.
	 */
	protected boolean parseAndStart() {
		//String ts = testScriptFiles.getFirst(); 
		File f = new File (activeTestScriptFile);
		if (f != null) {
			logger.info(PC2LogCategory.Parser, subCat,
					"Using input document " + activeTestScriptFile);
			
			TSParser tsp = new TSParser(true);
			try {
				doc = tsp.parse(activeTestScriptFile);
				logPackage();
				logger.info(PC2LogCategory.Parser, subCat,
						"Using input document " + activeTestScriptFile + " v." + doc.getVersion());
			
				Stacks.logStackSocketInformation();
				ss.setDynamicPlatformSettings(doc.getProperties());
				ss.logSettings();
				
				// Now see if we are running a test for an UE and if so
				// See if we should auto provision the device?
				ProvisioningData pd = autoProvision(doc.getName());
				if (pd != null) {
					String macAddr = autoGenerate(pd);
					if (macAddr != null) {
						// doc.setAutoProv(pd);
						logger.info(PC2LogCategory.PCSim2, subCat, "Updating the global registrar's that the DUT is rebooting");
						
						ProvListener pl = new ProvListener(pd);
						if (pl != null) {
							logger.debug(PC2LogCategory.PCSim2, subCat, "Starting the provisioning listener operation.");
							autoProvisioned = pl.run();
							if (!autoProvisioned) {
								logger.error(PC2LogCategory.PCSim2, subCat, "Auto provisioning did not occur as expected, test is terminating.");
								provDB.clearIssuedData(macAddr);
								return false;
							}
							else {
								Properties dut = SystemSettings.getSettings(SettingConstants.DUT);
								String pui = dut.getProperty(SettingConstants.PUI);
								if (pui != null)
									Stacks.generateAutoRebootEvent(pui);
								String pui2 = dut.getProperty(SettingConstants.PUI2);
								if (pui2 != null)
									Stacks.generateAutoRebootEvent(pui2);
								logger.info(PC2LogCategory.PCSim2, subCat, "Test execution pausing while resetting the DUT.");
								Thread.sleep(5000);
								logger.info(PC2LogCategory.PCSim2, subCat, "Pause complete.");
							}
						}
					}
					else {
						logger.info(PC2LogCategory.PCSim2, subCat, "Auto generate didn't return an updated data file for provisioning.");
					}
				}
				else {
					logger.info(PC2LogCategory.PCSim2, subCat, "Auto provisioning did not return a data file for the device.");
				}
				
				
				
				LinkedList<FSM> fsms = doc.getFsms();
				// Initialize the settings that can be overwritten from
				// within the document 
				setExtensions(fsms);
				if (doc.getInspector()) 
					ss.enableInspector();
								
				for (int i = 0; i< fsms.size(); i++) {
					FSM fsm = fsms.get(i);
					PC2Models testModel = null;
					String modelName = fsm.getModel().getName();
					if (modelName.equalsIgnoreCase("session")) {
						testModel = new Session(fsm);
						if (testModel != null)
							testModel.init();
						try {
							fsm.getNetworkElements().certify();
							}
						catch (PC2Exception e)	{
							
						}
					}
					else if (modelName.equalsIgnoreCase(MsgRef.STUN_MSG_TYPE)) {
						testModel = new Stun(fsm);
						if (testModel != null)
							testModel.init();
						//if (!fsm.getNetworkElements().certify())
						//   return false;
					}
					else if (modelName.equalsIgnoreCase("registrar")) {
						Properties dut = SystemSettings.getSettings("DUT");
						String pui = dut.getProperty(SettingConstants.PUI);
						Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
						String destIP = platform.getProperty(SettingConstants.IP);
						if (pui != null) {
							testModel = new Registrar(fsm, pui, destIP);
							if (testModel != null)
								testModel.init();
							try {
								fsm.getNetworkElements().certify();
								}
							catch (PC2Exception e)	{
								
							}
						}
						else 
							return false;
					}
					if (modelName.equalsIgnoreCase("register")) {
						testModel = new Register(fsm);
						if (testModel != null)
							testModel.init();
						try {
							fsm.getNetworkElements().certify();
							}
						catch (PC2Exception e)	{
							
						}
					}
					
					if (testModel != null) {
						testModel.start();
						activeModels.add(testModel);
					}
				}
				return true;
			}
			catch (PC2XMLException pe){
				String err = "\n** Parsing error in file \n    " + pe.getFileName() +
				" at line " + pe.getLineNumber();
				if (pe.getSystemId() != null) {
					err += ", uri " + pe.getSystemId();
				}
				if (pe.getPublicId() != null) {
					err +=  ", public " + pe.getPublicId();
				}
				err += "\n";

				logger.fatal(PC2LogCategory.Parser, subCat,
						err, pe);
			}
			catch (SAXParseException spe){
				String err = "\n** Parsing error in file \n    " 
					+ activeTestScriptFile
					+ " at line " + spe.getLineNumber();
				if (spe.getSystemId() != null) {
					err += ", uri " + spe.getSystemId();
				}
				if (spe.getPublicId() != null) {
					err +=  ", public " + spe.getPublicId();
				}
				err += "\n";
				
				logger.fatal(PC2LogCategory.Parser, subCat, err, spe);
			}
			catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		return false;
	}
	
//	private boolean processArgs(String [] args) {
//		boolean valid = true;
//		
//		// Process the arguments 
//		if (ui != null) {
//			// No valid arguments are necessary if the UI exists
//			// integrate passed in arguments to the UI
//		}
//		else if (args.length >= 1) {
//			logger.trace(PC2LogCategory.Parser, subCat,
//					"Beginning to process application arguments.");
//			LinkedList<String> invalidArgs = new LinkedList<String>();
//			int count = args.length;
//			for (int i=0; i < count; i++) {
//				logger.trace(PC2LogCategory.Parser, subCat,
//						"Argument["+i+"]="+args[i]);
//				if (args[i].equals("-p")) {
//					String fileName = args[++i];
//					logger.trace(PC2LogCategory.Parser, subCat, 
//							"Argument["+i+"]="+fileName);
//					String reason = validateFile(fileName);
//					if (reason == null)
//						platformSettingsFile = fileName;
//					else 
//						invalidArgs.add("-p " + fileName + reason);
//				}
//				else if (args[i].equals("-t")) {
//					do {
//						String fileName = args[++i];
//						logger.trace(PC2LogCategory.Parser, subCat,
//								"Argument["+i+"]="+fileName);
//						String reason = validateFile(fileName);
//						if (reason == null)
//							testScriptFiles.add(fileName);
//						else {
//							invalidArgs.add("-t " + fileName + reason);
//						}
//					}	while ((i+1) < count && !isArgOption(args[i+1]));
//				}
//				else if (args[i].equals("-d")) {
//					do {
//						String fileName = args[++i];
//						logger.trace(PC2LogCategory.Parser, subCat, 
//								"Argument["+i+"]="+fileName);
//						String reason = validateFile(fileName);
//						if (reason == null)
//							dutConfigFiles.add(fileName);
//						else 
//							invalidArgs.add("-d " + fileName + reason);
//					}	while ((i+1) < count && !isArgOption(args[i+1]));
//				}
//				else if (args[i].equals("-h")) {
//					help();
//					valid = false;
//				}
//				else
//					invalidArgs.add(args[i]);
//			}
//			if (invalidArgs.size() > 0) {
//				valid = false;
//				logger.fatal(PC2LogCategory.Parser, subCat,
//						"The following arguments are invalid:");
//				for (int i = 0; i < invalidArgs.size(); i++) 
//					logger.fatal(PC2LogCategory.Parser, subCat,
//							"\t" + invalidArgs.get(i));
//				help();
//				
//			}
//			else
//				logger.trace(PC2LogCategory.Parser, subCat,
//						"Processing of application arguments completed successfully.");
//		}
//		else
//			valid = false;
//		
//		return valid;
//	}
	
	/**
	 * 
	 */
	@Override
	public void removeDUTConfig(File f) {
		if (f.exists() && f.canRead() && f.isFile()) {
			synchronized (dutConfigsToRemove) {
				dutConfigsToRemove.add(f);
			}
			logger.debug(PC2LogCategory.Parser, subCat,
					"Removing client defined by " + f.getName() 
					+ " from list of accepted clients for global registrar.");
		}
		else {
			String reason = validateFile(f.getAbsolutePath());
			logger.warn(PC2LogCategory.Parser, subCat,
					"Couldn't process DUT configuration for global registrar because " 
					+ reason);
		}
		
		
	}
	/**
	 * Main thread simply looks for notification that the
	 * test is complete, informs the various protocol stacks
	 * to shutdown and then terminates the test.
	 *
	 */
	public void run() {
		mainThread = Thread.currentThread();
		// GAREY test code 		int count = 0;
		while (!shutdown) {
			// First determine if there are any tests to conduct
			boolean configsToProcess = false;
			synchronized (dutConfigsToRead) {
				synchronized(dutConfigsToRemove) {
					if (dutConfigsToRead.size() > 0 ||
							dutConfigsToRemove.size() > 0) {
						configsToProcess = true;
					}
				}
			}
			
			if (!configsToProcess && closeBatchFile) {
				if (ss != null) {
					ss.closeBatch();
					
				}
				if (stacks != null) {
					stacks.shutdown();
				}
				closeBatchFile = false;
				platformSettingsFile = null;
				if (globalRegEnabled) {
					globalRegFile = null;
					GlobalRegistrar.setMasterFSM(null, Transport.UDP);
					globalRegEnabled = false;
				}
				if (presenceServerEnabled) {
					presenceServerFile = null;
					presenceServerEnabled = false;
				}
			}
			// Next see if we are ready to run some test pairs
			// This can only be done if there are no dutConfigs to
			// process in add or remove lists
			else if (dutConfigFiles.size() > 0 &&
					testScriptFiles.size() > 0 &&
					!configsToProcess && 
					platformSettingsFile != null) {
				try {
					if (dutPrimary) {

						ListIterator<String> duts  = dutConfigFiles.listIterator();
						while (duts.hasNext() && !shutdown) {
							activeDUTFile = duts.next();
							ListIterator<String> testScripts = testScriptFiles.listIterator();
							while (testScripts.hasNext() && !shutdown) {
								activeTestScriptFile = testScripts.next();
								executeTest();
							}
						}
					}
					else {
						ListIterator<String> testScripts = testScriptFiles.listIterator();
						while (testScripts.hasNext() && !shutdown) {
							activeTestScriptFile = testScripts.next();
							ListIterator<String> duts = dutConfigFiles.listIterator();
							while (duts.hasNext() && !shutdown) {
								activeDUTFile = duts.next();
								executeTest();
							}
						}
					}
					testsComplete();
				}
				// This most likely will happen if the user stop's the tests. When the 
				// user terminates the test, the lists are emptied so that no record is made for 
				// unattempted test pairs.
				catch (ConcurrentModificationException cme) {
					logger.debug(PC2LogCategory.PCSim2, subCat,
					"Either DUT or Test Script list have changed during test pair execution.");
				}
			}
			else if (pendingPSFile != null) {
				boolean restart = false;
				// Determine if we need to restart the existing stacks and 
				// configuration parameters of the platform.
				if (platformSettingsFile == null)
					restart = true;
				else if (platformSettingsFile != null) {
					 // First we need to remove any UEs that are not being simulated from the
					 // table of allowable registering devices.
					 Enumeration<String> keys = SystemSettings.getPlatformLabels();
					 if (keys != null) {
						 while (keys.hasMoreElements()) {
							 String key = keys.nextElement();
							 if (key.startsWith("UE")) {
								 Properties p = SystemSettings.getSettings(key);
								 LinkedList<String> puis = createRegistrarLabels(p);
								 Stacks.removeSIPRegistrarClient(key, puis);
							 }
						 }
					 }
					 if (!pendingPSFile.equals(platformSettingsFile))
					 	 restart = true;
					 else if (psLastModified < (new File(platformSettingsFile)).lastModified())
				   		 restart = true;
				}
				if (restart && stacks != null)
					stacks.restart();
				platformSettingsFile = pendingPSFile;
				pendingPSFile = null;
				File ps = new File(platformSettingsFile);
				psLastModified = ps.lastModified();
				if (!ss.loadPlatformSettings(platformSettingsFile)) {
					logger.fatal(PC2LogCategory.Parser, subCat,
							"PCSim2 encountered error while trying to read the Platform Settings information.");
					// Clear the file information
					platformSettingsFile = null;
				}
				else{
				    Enumeration<String> labels = SystemSettings.getPlatformLabels();
				    if (labels != null) {
    				    while(labels.hasMoreElements()) {
    				        String ne = labels.nextElement();
    				        if (!ne.startsWith(SettingConstants.UE) 
    				                && !ne.startsWith(SettingConstants.PCSCF) 
    				                && !ne.startsWith(SettingConstants.SCSCF)) continue;
    				        Properties p = SystemSettings.getSettings(ne);
    				        logNE(ne, p);
    				    }
				    }
				    
				    if (restart){
				        logger.info(PC2LogCategory.PCSim2, subCat, "Starting servers.");
	                    startServers();
				    }
					
				}
				
				// Next reload the provisioning test script/policy/provisioning file table
				if (platformSettingsFile != null) {
					Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
					String cw = platform.getProperty(SettingConstants.CW_NUMBER);
					if (provDB == null)
						provDB = new ProvDatabase(cw);
					else
						provDB.load(cw);
				}
					
				
				// Now add all of the real devices in the platform settings to the 
				// allowable registering devices table.
				addRegistrarClients();
			}
			
			else {
				try {
					if (configsToProcess) {
						synchronized (dutConfigsToRead) {

							synchronized (dutConfigsToRemove) {
								if (dutConfigsToRead.size() > 0 ||
										dutConfigsToRemove.size() > 0) {
									ListIterator<File> rmIter = dutConfigsToRemove.listIterator();
									// Loop through the reads 
									File read = null;
										if (dutConfigsToRead.size() > 0)
											read = dutConfigsToRead.removeFirst();
									while (read != null) {
										int prevRmIndex = -1;
										// Make sure the read doesn't also appear in the 
										// remove list
										while (rmIter.hasNext()) {
											File rm = rmIter.next();
											if (rm.equals(read)) {
												prevRmIndex = rmIter.previousIndex();

											}
										}
										// If the file appears in the remove list at the
										// same time we try to read, simply don't read
										// and remove from the remove list
										if (prevRmIndex != -1) {
											dutConfigsToRemove.remove(prevRmIndex);
											logger.warn(PC2LogCategory.Parser, subCat,
													"Skipping reading " + read.getName() 
													+ " because file was also found in DUT Config remove list.");
										}
										else {
											try {
												// Load the read file
												String neLabel = ss.loadDUTSettings(read.getAbsolutePath(), true, false);
												if (neLabel != null && neLabel.startsWith("UE")) {
													// Now obtains the DUT's IP address and the expected IP address for
													// it to register with
													Properties p = SystemSettings.getSettings(neLabel);
													if (p != null) {
													    logNE(neLabel, p);
													    
														LinkedList<String> puis = createRegistrarLabels(p);
														if (puis.size() > 0) {															
															Stacks.addSIPRegistrarClient(neLabel, puis);
															// With the new design of using Public User Identifier we will need to
															// use the first entry in the puis as the key for the table
//															dutConfigFileToPUIIndex.put(read, puis.get(0));
															if (SystemSettings.getBooleanSetting(
																	SettingConstants.GLOBAL_REGISTRAR)) {
																logger.info(PC2LogCategory.Parser, subCat,
																		"Loaded " + read.getName()
																		+ " for global registrar.");
															}
															else
																logger.info(PC2LogCategory.Parser, subCat,
																		read.getName()
																		+ " not loaded for global registrar because registrar is disabled.");
														}
														else {
															logger.error(PC2LogCategory.Parser, subCat, 
															"The DUT configuration file appears to not contain any Public User Identitier.");
														}
													}
													else {
														logger.warn(PC2LogCategory.Parser, subCat,
																"Failed to load " 
																+ read.getName() + " for global registrar.");
													}
												}
												else if (neLabel != null && neLabel.startsWith("CN")) {
													// Since the CN could contain the PCSCF that real UEs
													// need to register, temporarily load the file's 
													// settings, add the clients and then remove them
													ss.setDUTProperties(read.getAbsolutePath());
													addRegistrarClients();
													ss.reset();
												}
												else {
													logger.info(PC2LogCategory.Parser, subCat,
															"No elements added for global register for label " + neLabel + " from file "
															+ read.getName() + ".");
												}
											}
											catch (Exception e) {
												logger.error(PC2LogCategory.PCSim2, subCat,
														"The DUT Configuration File defined by (" 
														+ read.getAbsolutePath() 
														+ ") could not be processed. File is being dropped.");
											}
										}
										read = null;
										if (dutConfigsToRead.size() > 0)
											read = dutConfigsToRead.removeFirst();
									}
									
									File rm = null;
									if (dutConfigsToRemove.size() > 0)
										rm = dutConfigsToRemove.removeFirst();
									while (rm != null) {
										try {
											String neLabel = ss.loadDUTSettings(rm.getAbsolutePath(), true, true);
											if (neLabel != null && neLabel.startsWith("UE")) {
												Properties p = SystemSettings.getSettings(neLabel);
												LinkedList<String> puis = createRegistrarLabels(p);
												Stacks.removeSIPRegistrarClient(neLabel, puis);
												logger.info(PC2LogCategory.Parser, subCat,
														"removing client defined by " + rm.getName()
														+ " to list of accepted clients for global registrar");
											}
										}
										catch (Exception e) {
											String err = "Simulator failed to sleep. Terminating test.";
											logger.fatal(PC2LogCategory.Parser, subCat, err, e);
										}
										rm = null;
										if (dutConfigsToRemove.size() > 0)
											rm = dutConfigsToRemove.removeFirst();
									}
									dutConfigsToRead.clear();
									dutConfigsToRemove.clear();
								}
							}
						}
					}	
					else {
						// Since there are aren't any dut and test script
						// files we can simply go to sleep and check again 
						// later for any work.
						Thread.sleep(250);
					}
					
				}
				catch (InterruptedException ie) {
					String msg = "Simulator sleep interrupted.";
					logger.info(PC2LogCategory.Parser, subCat, msg);
				}
			   catch (Exception e) {
					String err = "Simulator failed to sleep. Terminating test.";
					logger.fatal(PC2LogCategory.Parser, subCat, err, e);
				}
			}
		}
		LogAPI.shutdown();
	}
	
	
	
	
	
	/**
	 * Sets the extensions and services that may have been overwritten within
	 * a single test script for the test
	 *
	 */
	public void setExtensions(LinkedList<FSM> fsms) {
		ListIterator<FSM> iter = fsms.listIterator();
		while (iter.hasNext()) {
			FSM fsm = iter.next();
			NetworkElements nes = fsm.getNetworkElements();
			
			ListIterator<String> extIter = nes.getDisableExtensions();
			Extension e = Extension.DISABLED;
			while (extIter.hasNext()) {
				 String ext = extIter.next();
				 if (ext != null) {
					 if (ext.equals("gruu")) 
						 ss.setGRUUExtension(e);
					 else if (ext.equals("precondition"))
						 ss.setPreconditionExtension(e);
					 else if (ext.equals("100rel"))
						 ss.setReliabilityExtension(e);
				 }
			}
			extIter = nes.getSupportedExtensions();
			e = Extension.SUPPORTED;
			while (extIter.hasNext()) {
				String ext = extIter.next();
				if (ext != null) {
					if (ext.equals("gruu")) 
						ss.setGRUUExtension(e);
					else if (ext.equals("precondition"))
						ss.setPreconditionExtension(e);
					else if (ext.equals("100rel"))
						ss.setReliabilityExtension(e);
				}
			}
			extIter = nes.getRequireExtensions();
			e = Extension.REQUIRED;
			while (extIter.hasNext()) {
				String ext = extIter.next();
				if (ext != null) {
					if (ext.equals("gruu")) 
						ss.setGRUUExtension(e);
					else if (ext.equals("precondition"))
						ss.setPreconditionExtension(e);
					else if (ext.equals("100rel"))
						ss.setReliabilityExtension(e);
				}
			}
		}
	}
	
	/**
	 * Implementation for setting the platform settings file
	 * and starting the servers from the user interface.
	 * If the servers were already running, invocation of this
	 * method will result in their restart.
	 */
	@Override
	public boolean setPlatformSettings(String fileName) {
		File f = new File(fileName);
		if (f.exists() && f.canRead() && f.isFile()) {
			pendingPSFile = fileName;
			return true;
		}
		return false;
			
	}
	/**
	 * Global accessor to terminate the test.
	 *
	 */
	public static void setTestComplete() {
		complete = true;
		if (mainThread != null) {
			mainThread.interrupt();
		}
		if (isGUIActive())
			ui.testComplete();
	}	
	
	/** 
	 * Updates the global test result with a new setting. Since
	 * the value is && with the previous setting, a test that has
	 * already encountered a failure condition will continue to 
	 * be marked as a failure.
	 *
	 * By default all tests start assuming that they will succeed
	 * so all failures call this method to declare a test as a
	 * failure.
	 * @param flag - true test operations are operating as expected
	 * 				false if an error has occurred.
	 */
	public static void setTestPassed(boolean flag) {
//		if (!flag) {
//			Throwable t = new Throwable();
//			StackTraceElement [] ste = t.getStackTrace();
//			boolean done = false;
//			String trace = "";
//			for (int i = 1; i < ste.length && !done; i++) {
//				trace += "\n\t" + ste[i];
//				if (i >= 4)
//					done = true;
//			}	
//			logger.error(PC2LogCategory.PCSim2, subCat, 
//			
//					"Test is being declared a failure by: " + trace);
//		}
		if (testPassed == null)
			testPassed = flag;
		else 
			testPassed &= flag;
	
		if (complete) {
			displayResults();
		}
	}
	
	/**
	 * Cleans up the protocol stacks before terminating.
	 *
	 */
	@Override
	public void shutdown() {
		shutdown = true;
		ListIterator<PC2Models> iter = activeModels.listIterator();
		while (iter.hasNext()) {
			PC2Models m = iter.next();
			if (m != null)
				m.shutdown();
		}
		if (stacks != null) {
			stacks.shutdown();
		}
		
	}
	
	/**
	 * This method starts the protocol stacks and the global registrar if it is configured
	 * to run.
	 */
	private void startServers() {
		stacks = Stacks.getInstance();
		stacks.init();
		dist = Stacks.getSipDistributor();
		stun = Stacks.getStunDistributor();
		boolean success = configGlobalRegistrar();
		if (globalRegEnabled && !success) {
			logger.fatal(PC2LogCategory.Parser, subCat,
					"PCSim2 encountered error while trying to start the global registrar.");
			setTestPassed(false);
			setTestComplete();
			//shutdown();
			return;
		}
		else if (success) {
			logger.info(PC2LogCategory.Parser, subCat, 
					"PCSim2 has started master globalRegistrar process.");
		}
		
		success = configPresenceServer();
		if (presenceServerEnabled && !success) {
			logger.fatal(PC2LogCategory.Parser, subCat,
					"PCSim2 encountered error while trying to start the presence server.");
			setTestPassed(false);
			setTestComplete();
			//shutdown();
			return;
		}
		else if (success) {
			logger.info(PC2LogCategory.Parser, subCat, 
					"PCSim2 has started presenceServer process.");
		}
	}
	
	@Override
	public boolean startTests(LinkedList<File> dutFiles,
			LinkedList<File> testCaseFiles, boolean dutPrimary) {
		ListIterator<File> iter = dutFiles.listIterator();
		while(iter.hasNext()) {
			dutConfigFiles.add(iter.next().getAbsolutePath());
		}
		iter = testCaseFiles.listIterator();
		while(iter.hasNext()) {
			testScriptFiles.add(iter.next().getAbsolutePath());
		}
		this.dutPrimary = dutPrimary;
		return true;
	}
	
	@Override
	public boolean stopTests() {
		ListIterator<PC2Models> models = activeModels.listIterator();
		while(models.hasNext()) {
			PC2Models model = models.next();
			logger.error(PC2LogCategory.LOG_MSG, subCat, "Shutting down test model " + model.getName());
			model.shutdown();
		}

		logger.error(PC2LogCategory.LOG_MSG, subCat, "User terminated testing.");
		setTestPassed(false);
		setTestComplete();
		displayResults();
		testsComplete();
		return true;
	}
	
	private void testsComplete() {
		// We have attempted to execute all of the test pairs
		// now clear the two lists and the active files
		dutConfigFiles.clear();
		testScriptFiles.clear();
		activeDUTFile = null;
		activeTestScriptFile = null;
		if (isGUIActive())
			ui.testsComplete();
	}
	/**
	 * This method validates that the file exists, is readible,
	 * and if a file.
	 * 
	 * @param fileName - the path and file name to test
	 * @return true if valid file, false otherwise
	 */
	
	private String validateFile(String fileName) {
		String reason = null;
		File f = new File(fileName);
		if (!f.exists()) {
			reason = " does not exist.";
			logger.error(PC2LogCategory.Parser, subCat, 
					"Looking for file in " + f.getAbsolutePath());
		}
		else if (!f.isFile())
			reason = " isn't a file.";
		else if (!f.canRead())
			reason = " can't be read.";
		
		return reason;
	}
	
	/**
	 *  Starting point for the platform engine. It simply
	 *  passes the command-line arguments to the init method 
	 *  
	 */
	public static void main(String[] args) {
		// First determine if the license has been agreed to 
		// or not 
		// With the new installation process license acceptance
		// occurs during install, not upon running.
//		PC2LicenseScreen splash = new PC2LicenseScreen();
//		if (!splash.isAccepted()) {
//			splash.showLicense();
//			splash.run();
//			if (!splash.isAccepted()) {
//				System.out.println("PCSim2 is terminating because license"
//						+ " has not been agreed to by user.");
//				System.exit(-1);
//			}
//		}
		PCSim2 sim = new PCSim2();
		sim.init(args);
//	GLH Test Logger	-
//		try { 
//			 LogAPI l = new LogAPI();
//			 l.unitTest();
//		 }
//		 catch (IOException io) {
//			 System.out.println("LogAPI failed to create application log file.");
//		 }
		
	}
	/**
	 * Allows Session FSMs to determine if an endpoint has 
	 * already registered with the system prior to it's 
	 * creation.
	 * 
	 * @param ip - source address of the peer network element
	 * @return - true if the device is registered, false otherwise.
	 */
	/*	public static boolean isRegistered(String ip) {
	 if (registrar != null) {
	 return registrar.isRegistered(ip);
	 }
	 return false;
	 }*/
}
