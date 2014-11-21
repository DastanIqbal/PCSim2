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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;

import com.cablelabs.common.Conversion;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.sim.PCSim2;


public class SystemSettings {

	/**
	 * The single SystemSettings class for the platform.
	 */
	private static SystemSettings ss = null;
	
	/**
	 * The table containing all of the configuration parameter
	 * regardless of their respective origin.
	 */
	private static Hashtable<String, Properties> settings;
	
	/**
	 * This table contains all of the properties read in for a DUT configuration
	 * File. When the simulator starts a test it invokes the setDUTProperties
	 * method to have the specific files information added to the active settings.
	 * Once the test is complete and the reset method is called, all of the entries
	 * that end with the value of 0, eg. UE0 will be removed from the active
	 * settings.
	 * 
	 * The key to the table is the fileName that was used to read in the file.
	 */
	private static Hashtable<String, Hashtable<String, Properties>> dutSettings = null;
	
	/**
	 * The setting for the GRUU extension for this specific
	 * test case.
	 */
	private Extension gruu = Extension.DISABLED;
	/**
	 * The setting for the GRUU extension for this specific
	 * test case.
	 */
	private Extension reliability = Extension.DISABLED;
	/**
	 * The setting for the precondition extension for this specific
	 * test case.
	 */
	private Extension precondition = Extension.DISABLED;

	/**
	 * The value to use for the No Response Timeout within each 
	 * state during a test.
	 */
	private static int noResponseTimeout = 0;
	
	/**
	 * The original setting of the use of the Inspector to influence 
	 * the tests result inthe platform settings file before possibly 
	 * being overwritten by an individual test script.
	 */
	private boolean originalPlatformInspectorSetting = false;

	/**
	 * The original setting of the use of the GRUU extension in
	 * the platform settings file before possibly being overwritten
	 * by an individual test script.
	 */
	private Extension originalGruu = Extension.DISABLED;

	/**
	 * The original setting of the use of the 100rel extension in
	 * the platform settings file before possibly being overwritten
	 * by an individual test script.
	 */
	private Extension originalReliability = Extension.DISABLED;

	/**
	 * The original setting of the use of the precondition extension in
	 * the platform settings file before possibly being overwritten
	 * by an individual test script.
	 */
	private Extension originalPrecondition = Extension.DISABLED;
	
	/**
	 * The original settings for the platform. This allows a script
	 * to override a property for a single test and then reset the 
	 * value back to the original value for a subsequent test.
	 */
	private Properties originalPlatform = null;
	/**
	 * A flag indicating whether the inspector can influence this 
	 * specific test case.
	 */
	private boolean inspect = false;

	private static LogAPI logger = LogAPI.getInstance(); //Logger.getLogger(SystemSettings.class);

	/**
	 * The current run number to concatenate to the files for this test.
	 */
	private String curRunNum = null;
	
	/**
	 * The name of the record file that the system produces during this test.
	 */
	private String curLogName = null;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private static String subCat = "";
	
	/**
	 * Contains all of the global variables created in the XML test script.
	 */
	private GlobalVariables globals = GlobalVariables.getInstance();
	
	/**
	 * This is a counter for the number of packet captures started during a
	 * test.
	 *
	 */
	private int captures = 0;
	
	/**
	 * This contains the number of the interface for wireshark to use.
	 */
	private int captureInterface = 2;
	
	private static boolean useTransportParameter = true;
	
	/**
	 * A table containing the property of a network element indexed by
	 * telephone number instead of the network element label. This table
	 * is null, unless getPropertiesByTelephone is called at least once.
	 */
	private Hashtable<String, Properties> telephoneDirectory = null;
	
	private static final int VOICE_PORTS = 4;
	private static VoicetronixPort[] vtports = new VoicetronixPort [VOICE_PORTS];
	private static int defaultVTPort = -1;
	
	private SystemSettings() {
		settings = new Hashtable<String, Properties>();
		dutSettings = new Hashtable<String, Hashtable<String, Properties>>();
	}

	
	/**
	 * Assignes the device type configuration parameter to 
	 * elements defined in the platform settings file based
	 * upon the elements NE label
	 * @param key - the NE label
	 * @param p - the configuration parameter table to add the
	 * parameter to.
	 */ 
	private void addDeviceType(String key, Properties p) {
		if (key.equals("DUT")) {
			String deviceType = p.getProperty(SettingConstants.DEVICE_TYPE);
			if (deviceType != null) {
				if (deviceType.equals("UE")) {
					if (settings != null)
						settings.put(deviceType+"0", p);
					else 
						logger.error(PC2LogCategory.Settings, subCat, 
								"Couldn't add setting information for device type(" 
								+ deviceType+"0" + ") for properties(" + p + ".");
				}
			}
		}
		else if (key.length() == 3) {
			 String ne = key.substring(0,2);
			 if (ne.equals("UE")) 
				 p.setProperty("Device Type", "UE");
			 
			 else if (ne.equals("AS")) 
				 p.setProperty("Device Type", "AS");
		}
		else if (key.length() == 4) {
			 String ne = key.substring(0,3);
			 if (ne.equals("HSS")) 
				 p.setProperty("Device Type", "HSS");
		}
		else if (key.length() == 5) {
			 String ne = key.substring(0,4);
			 if (ne.equals("PRES")) 
				 p.setProperty("Device Type", "PRES");
		}
		else if (key.length() == 6) {
			String ne = key.substring(0,5);
			if (ne.equals("PCSCF"))
				p.setProperty("Device Type", "PCSCF");
			else if (ne.equals("SCSCF")) 
				p.setProperty("Device Type", "SCSCF");
			else if (ne.equals("ICSCF")) 
				p.setProperty("Device Type", "ICSCF");
		}
		
		p.setProperty("NE", key);
	}
	
	/** 
	 * Clears the voicetronix port mappings
	 */
	private void clearVoicetronixPorts() {
		for (int i=0; i< VOICE_PORTS; i++) {
			vtports[i] = null;
		}
	}
	
	/** 
	 * This method is invoked when a user closes a batch file. It causes
	 * the class to restore all of the original value to each attribute.
	 *
	 */
	public synchronized void closeBatch() {
		settings.clear();
		dutSettings.clear();
		clearVoicetronixPorts();
		gruu = Extension.DISABLED;
		reliability = Extension.DISABLED;
		precondition = Extension.DISABLED;
		noResponseTimeout = 0;
		originalPlatformInspectorSetting = false;
		originalGruu = Extension.DISABLED;
		originalReliability = Extension.DISABLED;
		originalPrecondition = Extension.DISABLED;
		originalPlatform = null;
		inspect = false;
		curRunNum = null;
		curLogName = null;
		globals.clear();
		captures = 0;
		captureInterface = 2;
	}
	
	private String createLinkLocalAddress(String propertyValue) {
		if (propertyValue == null) {
			return "";
		}
		if (propertyValue.length() == 11)
			propertyValue = "0" + propertyValue;
		
		// First two quads of link-local address are pre-defined constant "fe80::"
		StringBuffer strBuffer = new StringBuffer("fe80::");
			
		// In first octet of MAC Address, invert 6th bit (zero based)
		int bitmask = 0x02;
		int tempInt = Integer.parseInt(propertyValue.substring(0, 2), 16);
		int reversedOctetInt = (tempInt ^ bitmask);
		String reversedOctetString = Integer.toHexString(reversedOctetInt);

		// Append modified MAC Address formatted in IPv6 notation
		strBuffer.append(reversedOctetString);
		strBuffer.append(propertyValue.substring(2, 4));
		strBuffer.append(":");
		strBuffer.append(propertyValue.substring(4, 6));
		// By algorithm for forming LinkLocal Address, insert contant "ff:fe" here.
		strBuffer.append("ff:");
		strBuffer.append("fe");
		strBuffer.append(propertyValue.substring(6, 8));
		strBuffer.append(":");
		strBuffer.append(propertyValue.substring(8, 12));
		
		return strBuffer.toString().toUpperCase();
	}
	
	
	private String createMACAddressWithColons(String v) {
		if (v.length() == 11) {
			return "0" + v.substring(0,1) + ":" + v.substring(1,3) + ":" + v.substring(3,5) + ":" + v.substring(5,7) +":" +
		            v.substring(7,9) + ":" + v.substring(9);
		}
		else if (v.length() == 12){
			return  v.substring(0,2) + ":" + v.substring(2,4) + ":" + v.substring(4,6) + ":" + v.substring(6,8) +":" +
		            v.substring(8,10) + ":" + v.substring(10);
		}
		return null;
	}
	
	
	public static String dumpKeys() {
		String result = "Current property keys= ";
		Enumeration<String> e = settings.keys();
		if (e.hasMoreElements()) {
			result += e.nextElement();
			while (e.hasMoreElements()) {
				result += ", " + e.nextElement();
			}
		}
		return result;
	}
	/**
	 * Obtains a reference to the singleton SystemSettings class.
	 * 
	 * @return 
	 */
	public synchronized static SystemSettings getInstance() {
		if (ss == null) {
			ss = new SystemSettings();
			
		}	
		return ss;
	}

	/**
	 * Enables the inspector to declare a test case a failure if
	 * an error is encountered during the inspection process.
	 *
	 */
	public void enableInspector() {
		this.inspect = true;
	}

	/**
	 * This method provides a common operation of obtaining a boolean
	 * Platform setting. It returns true if the setting is either true,
	 * on or enable and false otherwise.
	 * 
	 * @param setting - the setting to lookup in the Platform settings table
	 * @return - true if it is enable, on or true; false otherwise
	 * 
	 */
	public static boolean getBooleanSetting(String setting) {
		if (setting != null) {
			Properties platform = settings.get(SettingConstants.PLATFORM);
			if (platform != null) {
				String value = platform.getProperty(setting);
				if (value != null && (value.equalsIgnoreCase("true") ||

						value.equalsIgnoreCase("enable") ||
						value.equalsIgnoreCase("on"))) 
					return true;
			}
		}
		return false;
	}
	
	public int getCaptureInterface() {
		return captureInterface;
	}
	
	public String getCurrentRunNumber() {
		return curRunNum;
	}
	
	public Extension getGRUU() {
		return gruu;
	}

	public Extension getExtension(String ext) {
		if (ext.equalsIgnoreCase("100rel")) {
			return reliability;
		}
		else if (ext.equalsIgnoreCase("precondition")) {
			return precondition;
		}
		else if (ext.equalsIgnoreCase("gruu")) {
			return gruu;
		}
		return null;
	}

	/**
	 * This method creates the capture file name (without an extension) for this
	 * test script.
	 * @return
	 */
	public String getNextCaptureName() {
		try {
			String logName = getLogPrependName(PCSim2.getTestName()) 
			+ "_" + (++captures) + "_ss";
			return logName;
		}
		catch (PC2Exception ex) {
			logger.error(PC2LogCategory.Model, "", "Couldn't create capture file name.");
		}
		return null;
	}
	
	public static int getNoResponseTimeout() {
		return noResponseTimeout;
	}

	/**
	 * Generates the log file name based upon the document name and the 
	 * current platform settings information.
	 * 
	 * @param testName
	 * @return
	 * @throws PC2Exception
	 */
	public String getLogPrependName(String testName) throws PC2Exception {
		if (curLogName == null) {
			Properties platform = settings.get(SettingConstants.PLATFORM);
			
			if (platform != null) {
				Properties dut = settings.get("DUT");
				
				String newTestName = testName;
				newTestName = newTestName.replace(" ", "_");
				newTestName = newTestName.replace(".", "-");
				String logDir = platform.getProperty(SettingConstants.LOG_DIRECTORY);
				File dir = new File(logDir);
				if (dir.exists() && dir.isDirectory()) {
					int runNumber = 1;
					
					String name = "CW" 
						+ platform.getProperty(SettingConstants.CW_NUMBER)
						// Remove the version of the tool and the subgroup from the name of
						// the output files generated by the tool.
//						+ "-P" + platform.getProperty(SettingConstants.DUT_VERSION) 
//						+ "_" + platform.getProperty(SettingConstants.DUT_SUBGROUP) 
						+ "_" + dut.getProperty(SettingConstants.DUT_VENDOR) 
						+ "_" + newTestName 
						+ "_RUN";
					String [] files = dir.list();
					for (int i = 0; i < files.length; i++) {
						// int nl = name.length() + 8;
						// int l = files[i].length();
						// System.out.println("files[i]=" + files[i].substring(0,name.length()) 
						//		+ "\n len=" + l + "\n name=" + name + "\n nameLen=" + nl);
						if (name.length()+8 <= files[i].length() &&
								files[i].substring(0,name.length()).equals(name)) {
							int index = files[i].indexOf("_ss");
							if (index != -1) {
								try {
									
									int number = Integer.parseInt(files[i].substring(name.length(), index));
									if (number >= runNumber)
										runNumber = ++number; 
								}
								catch (NumberFormatException ex) {
									// ignore file
								}
							}
						}
					}
					curRunNum = ((Integer)runNumber).toString();
					name += curRunNum;
					curLogName = logDir + "/" + name;
					return curLogName;
				}
				else {
					throw new PC2Exception(logDir 
							+ " either doesn't exist or isn't the name of a directory.");
				}
			}
			
		}
		
		return curLogName;

	}


	public Extension getPrecondition() {
		return precondition;
	}


    public static String getLabelByIP(String ip) {
    	Enumeration<String> keys = settings.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (key.substring(0,2).equals("UE") ||
					key.equals("DUT")) {
				Properties element = settings.get(key);
				String elementIP = element.getProperty(SettingConstants.IP);
				if (elementIP != null && elementIP.equals(ip)) {
					return key;
				}
			}
		}
		return "";
    }

    public static String getLabelByValue(String value, String valueName) {
    	Enumeration<String> keys = settings.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (key != null && 
					// exclude any keys for registrar that use pui as key
					!(key.contains("@"))) {
				Properties element = settings.get(key);
				String vn = element.getProperty(valueName);
				if (vn != null && 
						vn.equals(value)) {
					return key;
				}
			}
		}
		return "";
    }
    
	public static Properties getPropertiesByIP(String ip, String neType) {
		Enumeration<String> keys = settings.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (key.substring(0,neType.length()).equals(neType)) {
				Properties p = settings.get(key);
				if (p != null) {
					String neIP = p.getProperty(SettingConstants.IP);
					if (neIP != null && neIP.equals(ip))
						return p;
//					else if (neIP == null) {
//						Enumeration<Object> s = p.keys();
//						
//						String values = "";
//						while (s.hasMoreElements()) {
//							String prop = (String)s.nextElement();
//							values += prop + "=[" + p.getProperty(prop) + "]\n";
//						}
//						
//					}
				}
			}
		}
		logger.error(PC2LogCategory.Settings, subCat,
				"Couldn't find 'IP' property for " + ip 
				+ ". NE type is: \n" + neType);
		return null;
	}
	
	/**
	 * Gets the network element's Properties for the element that has the 
	 * specified phone number. 
	 * 
	 * @param phoneNum - the phone number to search for in the properties.
	 * @return
	 */
	public static Properties getPropertiesByPhoneNumber(String phoneNum) {
		String value = phoneNum;
		if (value.startsWith("+1")) {
			value = value.substring(2);
		}
		else if (value.startsWith("1") ||
				value.startsWith("+")) {
			value = value.substring(1);
		}
		Enumeration<Properties> elements = settings.elements();
		while (elements.hasMoreElements()) {
			// First we need to filter the phone number for any leading + or 1
			
			Properties p = elements.nextElement();
			String l = p.getProperty(SettingConstants.PHONE_LINES);
			if (l != null) {
				try {
					int lines = Integer.parseInt(l);
					for (int i=0; i<lines; i++) {
						String num = p.getProperty(SettingConstants.PHONE_NUMBER + (i+1));
						if (num != null &&
								num.equals(value))
							return p;
					}
				}
				catch (NumberFormatException nfe) {
					logger.error(PC2LogCategory.PCSim2, subCat, l + "could not be converted to a number for the phone line settings");
				}
			}
		}
		return null;
	}

	/**
	 * This method supports the auto routing operations
	 * @param phoneNum
	 * @return
	 */public Properties getPropertiesByTelephone(String phoneNum) {
		if (telephoneDirectory == null) {
			// Create table
			logger.debug(PC2LogCategory.Settings, subCat,
					"Generating telephone directory.");
			Enumeration<String> e = settings.keys();
			telephoneDirectory = new Hashtable<String, Properties>();
			while (e.hasMoreElements()) {
				String key = e.nextElement();
				if (key.startsWith("UE")) {
					Properties p = settings.get(key);
					if (p != null) {
						String pn = p.getProperty(SettingConstants.PUI);
						String pn2 = p.getProperty(SettingConstants.PUI2);
						int amp = pn.indexOf("@");
						if (amp != -1) {
							pn = pn.substring(0, amp);
							telephoneDirectory.put(pn, p);
						}
						amp = pn2.indexOf("@");
						if (amp != -1) {
							pn2 = pn2.substring(0, amp);
							telephoneDirectory.put(pn2, p);
						}
						
					}
				}
			}
		}
		
		return telephoneDirectory.get(phoneNum);
	}
	
	/**
	 * Gets the network element's Properties for the element that has the 
	 * specified property (key) set to the current value defined
	 * by the value argument. 
	 * 
	 * @param key - the property field to search through (eg. Device Type)
	 * @param value - the current value to search for.
	 * @return
	 */
	public static Properties getPropertiesByValue(String key, String value) {
		Enumeration<Properties> elements = settings.elements();
		while (elements.hasMoreElements()) {
			Properties p = elements.nextElement();
			String setting = p.getProperty(key);
			if (setting != null && setting.equals(value)) {
				return p;
			}
		}
		return null;
	}

	public Extension getReliability() {
		return reliability;
	}

	/**
	 * Obtains the configuration parameters for the requested
	 * network element.
	 * 
	 * @param key - The network element label configuration 
	 * 		parameters to return.
	 *
	 * @return - the configuration parameters for the 
	 * 		named network element or null.
	 */
	public static Properties getSettings(String key) throws IllegalArgumentException {
		if (key == null) {
			logger.error(PC2LogCategory.PCSim2, subCat, "NULL is not a valid network element label");
			throw new IllegalArgumentException();
		}
		if (settings != null)
			return settings.get(key);
		return null;
	}

	
	/**
	 * Returns the enumeration of keys in the settings table.
	 * 
	 * @return
	 */
	public static Enumeration<String> getPlatformLabels() {
		if (settings != null)
			return settings.keys();
		return null;
	}
	
	public static VoicetronixPort getVoicePort(int index) {
		if (vtports != null &&
				index >= 0 && index < vtports.length)
			return vtports[index];
		
		return null;
	}
	
	public static VoicetronixPort getVoicePort(PlatformRef pr) {
		try {
			String voiceport = pr.getParameter();
			String index = voiceport.substring(SettingConstants.VOICE_PORT.length());
			if (index != null) {
			     int ndx = Integer.parseInt(index);
			     return getVoicePort(ndx);
			}
		}
		catch (Exception ex) {
			
		}
		return null;
	}
	

	public static VoicetronixPort getDefaultVoicePort() {
		if (vtports != null && defaultVTPort > -1) {
				return vtports[defaultVTPort];
		}
		return null;
	}
	
	public static int getVoicePortNumbber(String neLabel, int line) {
		if (settings != null) {
			for (int i=0; i< VOICE_PORTS; i++) {
				VoicetronixPort vp = vtports[i];
				if (neLabel.equals(vp.getNELabel()) &&
						vp.getLine() == line) {
					return i;
				}
			}
		}	
		return -1;
	}
	
//	public static int getVoicePort(String neLabel, String line) {
//		try {
//			int lineNum = Integer.parseInt(line);
//			return getVoicePort(neLabel, lineNum); 
//		}
//		catch (Exception ex) {
//		}	
//		return -1;
//	}
//	
	//	Gets the contents of a cell regardless of the cell type	
	private String getXlsCellStringValue(HSSFCell cell) {
		int cellType = 0;
		String value = "";

		if (cell != null) {
			cellType = cell.getCellType();

			switch (cellType) {
			case HSSFCell.CELL_TYPE_NUMERIC :
				Double doubleValue = cell.getNumericCellValue();
				value = Long.toString(doubleValue.longValue());
				break;
			case HSSFCell.CELL_TYPE_STRING :
				value = cell.getRichStringCellValue().getString();
				break;
			case HSSFCell.CELL_TYPE_FORMULA :
				value = cell.getCellFormula();
				break;
			case HSSFCell.CELL_TYPE_BLANK :
				value = "";
				break;
			case HSSFCell.CELL_TYPE_BOOLEAN :
				value = Boolean.toString(cell.getBooleanCellValue());
				break;
			case HSSFCell.CELL_TYPE_ERROR :
				value = "ERROR";
				break;
			default :
			}
		}
		return value;
	}

//	public static String isRegistrarClient(String ip) {
//		Properties platform = settings.get(ip);
//		if (platform != null) {
//			return getLabelByIP(ip);
//		}
//		return null;
//	}
	
	public static boolean knownNetworkElementByIP(String ip) {
		Enumeration<String> keys = settings.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (key.substring(0,1).equals("UE") ||
					key.equals("DUT")) {
				Properties element = settings.get(key);
				String elementIP = element.getProperty(SettingConstants.IP);
				if (elementIP != null && elementIP.equals(ip)) {
					return true;
				}
			}
		}
		return false;
	}

	public String loadDUTSettings(String fileName, boolean forRegistrar, boolean forRemoval) {
		String[] argArray = { fileName, "DUT", null, "0", "1"};
		try {
			Hashtable<String, Properties> dutProp = null;
			if (fileName.endsWith(".xls"))
				dutProp = readConfigParameters(argArray);
			else if (fileName.endsWith(".properties"))
				dutProp = readDUTParameters(fileName);
				
			Properties dut = dutProp.get("DUT");
			// Lastly if the DUT is of type CN (Core Network) we need to propagate all of the same
			// settings to each of the elements based upon the composition property	
			String deviceType = null;
			if (dut != null) {
				deviceType = dut.getProperty(SettingConstants.DEVICE_TYPE);
				if (deviceType != null) {
					if (deviceType.equals("CN")) {
						String composition = dut.getProperty(SettingConstants.COMPOSITION);
						if (composition != null) {
							StringTokenizer tokens = new StringTokenizer(composition);
							while (tokens.hasMoreTokens()) {
								String label = tokens.nextToken();
								logger.debug(PC2LogCategory.Settings, subCat,
										"Self generating properties for label=" + label);
								Properties overloadedProp = dutProp.get(label);
								Properties merged = new Properties();
								// Make a deep copy of all the settings for the network element
								Enumeration<Object> elements = dut.keys();
								while (elements.hasMoreElements()) {
									String element = new String((String)elements.nextElement());
									String value = new String(dut.getProperty(element));
									merged.put(element, value);
								}									
								// Next see if there were any specific elements for the network 
								// element in the file. If so replace the values generated from
								// the deep copy with those for the specific element.
								if (overloadedProp != null) {
									elements = overloadedProp.keys();
									while (elements.hasMoreElements()) {
										String element = new String((String)elements.nextElement());
										String value = new String(overloadedProp.getProperty(element));
										//if (merged.containsKey(element))
										//	merged.remove(element);
										merged.put(element, value);
									}	
								}
								dutProp.put(label, merged);
							}
						}
						else {
							logger.error(PC2LogCategory.Settings, subCat,
									"The DUT Configuration File(" + fileName 
									+ " is invalid because the device type is set to CN but there " 
									+ "does not appear to be any composition setting for the NE type of DUT.");
							return null;
						}
					}
					// This section addes the PUI and PUI2 to the list of keys in the settings
					// attribute. This is necessary so that the global registrar can allow
					// real network elements that are not currently involved in a test to 
					// register to the platform. 
					else if (deviceType.equals("UE") && forRegistrar) {
						String pui = dut.getProperty(SettingConstants.PUI);
						String pui2 = dut.getProperty(SettingConstants.PUI2);
						if (forRemoval) {
							// Remove it immediately from the settings
							if (pui != null) {
								settings.remove(pui);
								logger.debug(PC2LogCategory.PCSim2, subCat, 
										"Removing label=" + pui + " from system settings.");
								dutProp.remove(pui);
								logger.debug(PC2LogCategory.PCSim2, subCat, 
										"Removing label=" + pui + " to DUT settings.");
							}
							if (pui2 != null) {
								settings.remove(pui2);
								logger.debug(PC2LogCategory.PCSim2, subCat, 
										"Removing label=" + pui2 + " from system settings.");
								dutProp.remove(pui2);
								logger.debug(PC2LogCategory.PCSim2, subCat, 
										"Removing label=" + pui2 + " to DUT settings.");
							}
						}
						else {
							if (pui != null) {
								settings.put(pui,dut);
								logger.debug(PC2LogCategory.PCSim2, subCat, 
										"Adding label=" + pui + " from system settings.");
								dutProp.put(pui,dut);
								logger.debug(PC2LogCategory.PCSim2, subCat, 
										"Adding label=" + pui + " to DUT settings.");
							}
							if (pui2 != null) {
								settings.put(pui2,dut);
								logger.debug(PC2LogCategory.PCSim2, subCat, 
										"Adding label=" + pui + " from system settings.");
								dutProp.put(pui2,dut);
								logger.debug(PC2LogCategory.PCSim2, subCat, 
										"Adding label=" + pui2 + " to DUT settings.");
							}
						}
					}
				}
				else {
					logger.error(PC2LogCategory.Settings, subCat,
							"The DUT Configuration File(" + fileName 
							+ " is invalid because the setting " + SettingConstants.DEVICE_TYPE 
							+ " doesn't appear to be in the file."); 
					return null;
				}
			}
			else {
				logger.error(PC2LogCategory.Settings, subCat,
						"The DUT Configuration File(" + fileName 
						+ " is invalid because there were no properties found with the label set to DUT." );
				return null;
			}	
			// Add the DUTs properties the dutSettings table
			String neLabels = null;
			Enumeration<String> keys = dutProp.keys();
			while (keys.hasMoreElements()) {
				if (neLabels == null)
					neLabels = keys.nextElement();
				else
					neLabels += ", " + keys.nextElement();
			}
			logger.info(PC2LogCategory.LOG_MSG, subCat,
				"The DUT Configuration File(" + fileName 
				+ " has been added to the dutSettings table with NELabels=" + neLabels + "."); 
			dutSettings.put(fileName, dutProp);
			
			
//			Properties p = settings.get(neLabel);

//			if (p != null && forRegistrar) { 
//				String pui = p.getProperty(SettingConstants.PUI);
//				String pui2 = p.getProperty(SettingConstants.PUI2);
//				if (forRemoval) {
//					//key = registrarKey;
//					if (pui != null) {
//						settings.remove(pui);
//						logger.debug(PC2LogCategory.PCSim2, subCat, 
//								"Removing label=" + pui + " from system settings.");
//					}
//					if (pui2 != null) {
//						settings.remove(pui2);
//						logger.debug(PC2LogCategory.PCSim2, subCat, 
//								"Removing label=" + pui2 + " from system settings.");
//					}
//				}
//				else {
//					//key = registrarKey;
//					if (pui != null) {
//						settings.put(pui,p);
//						logger.debug(PC2LogCategory.PCSim2, subCat, 
//								"Adding label=" + pui + " to system settings.");
//					}
//					if (pui2 != null) {
//						settings.put(pui2,p);
//						logger.debug(PC2LogCategory.PCSim2, subCat, 
//								"Adding label=" + pui2 + " to system settings.");
//					}
//				}

//				}
//			}
//			else {
//				logger.error(PC2LogCategory.Settings, subCat, 
//						fileName + " doesn't appear to be a valid DUT Configuration File.");
//			}
			return deviceType+"0";
		}
		catch (Exception e) {
			logger.error(PC2LogCategory.Settings, subCat, 
					fileName + " doesn't appear to be a valid DUT Configuration File.");
			return null;
		}
	}

	public boolean loadPlatformSettings(String fileName) {
		// platformSettings = new Hashtable();
		String[] argArray = {fileName, SettingConstants.PLATFORM, "0", "1", "2"};
		try {
			Hashtable<String, Properties> newSettings = null;
			if (fileName.endsWith(".xls"))
					newSettings = readConfigParameters(argArray); 
			else if (fileName.endsWith(".properties"))
				newSettings = readPlatformParameters(fileName);
			
			if (newSettings != null) 
				settings = newSettings;
			Properties platform = settings.get(SettingConstants.PLATFORM);
			// GLH
			// platform.setProperty("No Response Timeout", "600000");
			// platform.setProperty("SIP Ignore Retransmissions", "false");
			// platform.setProperty("Global Registrar No Response Timeout", "60000000");
			// platform.setProperty("SIP Stack 1 Embedded Stun", "true");
			// platform.setProperty("Voicetronix Enabled", "true");
			// platform.setProperty("Script Editor", "C:\\Program Files\\Vim\\vim70\\gvim.exe");
			// platform.setProperty("UT_PATH", "C:/Documents and Settings/Garey Hassler/My Documents/PC2_Scripts/Script Development");
			// platform.setProperty("FSM Process Duplicate Messages", "true");
			//platform.setProperty("Global Registrar", "false");
			
			SystemSettings.noResponseTimeout = Integer.parseInt(platform.getProperty(SettingConstants.NO_RESP_TIMEOUT));
			this.originalPlatformInspectorSetting = getBooleanSetting("SIP Inspector");
			this.inspect = this.originalPlatformInspectorSetting;
			useTransportParameter = getBooleanSetting("SIP Include Transport Parameter");
			
			// Now make a copy of the original platform settings to reset
			// any dynamic settings that the test script may have changed
			this.originalPlatform = new Properties();
			Enumeration<Object> keys = platform.keys();
			while (keys.hasMoreElements()) {
				String key = (String)keys.nextElement();
				String value = (String)platform.get(key);
				
				this.originalPlatform.setProperty(key, value);
			}
			
			loadSipStackSettings();
			loadDiameterStackSettings();
			setVoicetronixMapping();
			
			String packetInterface = platform.getProperty(SettingConstants.WIRESHARK_INTERFACE);
			if (packetInterface != null) {
				try {
					captureInterface = Integer.parseInt(packetInterface);
				}
				catch (NumberFormatException nfe) {
					logger.error(PC2LogCategory.Settings, subCat, 
							"The Platform Wireshark Interface configuration settings is not an integer." + 
							" This must be corrected in order to create a capture.");
				}
			}
			return true;
		}
		catch (Exception e) {
			logger.fatal(PC2LogCategory.Settings, subCat,
					e.getMessage(), e);
			return false;
		}
		
	}

	private void loadDiameterStackSettings() {
		Properties platform = settings.get(SettingConstants.PLATFORM);
		int curStack = 0;
		try {
			String numOfStacks = platform.getProperty(SettingConstants.NUM_DIAMETER_STACKS);
			if (numOfStacks != null) {
				int stacks = Integer.parseInt(numOfStacks);
				for (int i = 1; i <= stacks && stacks != -1; i++) {
					curStack = i;
					Properties diaStack = new Properties();
					String name = platform.getProperty("Diameter Stack " + i + " Name");
					boolean enabled = resolveBooleanSetting(
							platform.getProperty("Diameter Stack " + i + " Enabled"));
					if (enabled) {
						String ip = platform.getProperty("Diameter Stack " + i + " IP Address");
						String tcp = platform.getProperty("Diameter Stack " + i + " TCP Port");
						String tls = platform.getProperty("Diameter Stack " + i + " TLS Port");
						String sctp = platform.getProperty("Diameter Stack " + i + " SCTP Port");
						String peers = platform.getProperty("Diameter Stack " + i + " SCTP Peers");
						String type = platform.getProperty("Diameter Stack " + i + " Type");
						diaStack.setProperty(name + " IP Address", ip);
						diaStack.setProperty(name + " TCP Port", tcp);
						diaStack.setProperty(name + " TLS Port", tls);
						diaStack.setProperty(name + " SCTP Port", sctp);
						diaStack.setProperty(name + " SCTP Peers", peers);
						diaStack.setProperty(name + " Type", type);
						diaStack.setProperty("Diameter Stack Enabled", "enable");


						//System.setProperty("javax.net.ssl.keyStore",platform.getProperty("TLS Keystore"));
						//System.setProperty("javax.net.ssl.keyStorePassword",platform.getProperty("TLS Keystore Password"));
						//System.setProperty("javax.net.ssl.keyStoreType",platform.getProperty("TLS Keystore Type"));

						settings.put(name, diaStack);
					}	
				}
			}
			
		}
		catch (Exception e) {
			logger.error(PC2LogCategory.Settings, subCat,
					"Platform could not process the SipStack information for stack number " 
					+ curStack + ".", e);
		}
	}
	
	private void loadSipStackSettings() {
		Properties platform = settings.get(SettingConstants.PLATFORM);
		int curStack = 0;
		try {
			int stacks = Integer.parseInt(platform.getProperty(SettingConstants.NUM_SIP_STACKS));
			for (int i = 1; i <= stacks; i++) {
				curStack = i;
				Properties sipStack = new Properties();
				String name = platform.getProperty("SIP Stack " + i + " Name");
				String ip = platform.getProperty(name + " IP Address");
				String compressStun = platform.getProperty(SettingConstants.STUN_COMPRESS_FORM);
				if (compressStun == null)
					compressStun = "false";
				if (name != null && ip != null) {
					sipStack.setProperty("javax.sip.STACK_NAME", name);
					if (Conversion.isIPv6Address(ip)) {
						String zone = platform.getProperty(name + " IPv6 Zone");
						ip = Conversion.addZone(ip, zone);
					}
					sipStack.setProperty("javax.sip.IP_ADDRESS", ip);
					String port = platform.getProperty(name + " UDP Port");
					sipStack.setProperty(SettingConstants.UDP_PORT, port);
					port = platform.getProperty(name + " TCP Port");
					sipStack.setProperty(SettingConstants.TCP_PORT, port );
					port = platform.getProperty(name + " TLS Port");
					sipStack.setProperty(SettingConstants.TLS_PORT, port);


					sipStack.setProperty("javax.sip.STACK_NAME", platform.getProperty(name + " Name"));
					sipStack.setProperty("javax.sip.RETRANSMISSION_FILTER",	
							platform.getProperty(name + " Retransmission Filter"));

					// The following properties are specific to nist-sip
					// and are not necessarily part of any other jain-sip
					// implementation.
					// You can set a max message size for tcp transport to
					// guard against denial of service attack.
					sipStack.setProperty("gov.nist.javax.sip.MAX_MESSAGE_SIZE", 
							platform.getProperty(name + " Max Message Size"));
					// Set these values so the sip stack doesn't throw an exception. The
					// stack won't use them though
					// PC 2.0 These are necessary for the stack, but they will not be used.
					sipStack.setProperty("gov.nist.javax.sip.DEBUG_LOG", "pc2sim.txt");
					sipStack.setProperty("gov.nist.javax.sip.SERVER_LOG","pc2simlog.txt");

					// Drop the client connection after we are done with the transaction.
					sipStack.setProperty("gov.nist.javax.sip.CACHE_CLIENT_CONNECTIONS", "false");
					// Set to 0 in your production code for max speed.
					// You need 16 for logging traces. 32 for debug + traces.
					// Your code will limp at 32 but it is best for debugging.
					sipStack.setProperty("gov.nist.javax.sip.TRACE_LEVEL", "32");
					// This property was added for PC2.0
					sipStack.setProperty("Reestablish TCP", platform.getProperty("SIP Reestablish TCP Sockets"));
					sipStack.setProperty("gov.nist.javax.sip.EMBEDDED_STUN", platform.getProperty(name + " Embedded Stun"));
					sipStack.setProperty("gov.nist.javax.sip.COMPRESS_STUN", compressStun);
					// Guard against starvation.
					sipStack.setProperty("gov.nist.javax.sip.READ_TIMEOUT", "1000");
					// Set general JSSE System properties so that TLS Server Socket Factory gets 
					// initialized properly
					
					String keystore = platform.getProperty("TLS Keystore");
					String password = platform.getProperty("TLS Keystore Password");
					String type = platform.getProperty("TLS Keystore Type");
					
					if (keystore != null &&
							password != null &&
							type != null) {System.setProperty("javax.net.ssl.keyStore",keystore);
							System.setProperty("javax.net.ssl.keyStorePassword",password);
							System.setProperty("javax.net.ssl.keyStoreType",type);

							//String keyStore = "../config/server.jks";
//							String DL = java.io.File.separator;
//							String keyStore = System.getProperty("java.home") + DL + "lib" + DL + "security" + DL + "cacerts";
//							System.setProperty("javax.net.ssl.keyStore",keyStore);
//							System.setProperty("javax.net.ssl.keyStorePassword","changeit");
//							System.setProperty("javax.net.ssl.keyStoreType","JKS");
							/*
				    CR
							 */		
							//String DL = java.io.File.separator;
//							String trustStore = System.getProperty("java.home") + DL + "lib" + DL + "security" + DL + "cacerts";
							//String trustStore = "../config/client.jks";

							System.setProperty("javax.net.ssl.trustStore",keystore);
							System.setProperty("javax.net.ssl.trustStorePassword",password);
							System.setProperty("javax.net.ssl.trustStoreType",type);
					}

		
						
					// GLH SSLServerSocket sss = (SSLServerSocket) sssf.createServerSocket(9096);
					// GLH sss.setNeedClientAuth(true);
					settings.put(name, sipStack);
					
				}
				else if (name == null) {
					logger.error(PC2LogCategory.Settings, subCat, 
							"SystemSettings could not find the SIP Stack " + i 
							+ " Name property value in the Platform Settings file.");
				}
				else if (ip == null) {
					logger.error(PC2LogCategory.Settings, subCat,
							"SystemSettings could not find the SIP Stack " + i 
							+ " IP Address property value in the Platform Settings file.");
				}
			}
		}
		catch (Exception e) {
			logger.error(PC2LogCategory.Settings, subCat,
					"Platform could not process the SipStack information for stack number " 
					+ curStack + ".", e);
		}
	}
	public void logSettings() {
		Enumeration<String> keys = settings.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			Properties element = settings.get(key);
			Enumeration<Object> setting = element.keys();
			int count = 0;
			String msg = key + " settings:\n";
			while (setting.hasMoreElements()) {
				String tag = (String)setting.nextElement();
				String value = element.getProperty(tag);
				if (count > 0)
					msg += ", " + tag + "=" + value;
				else 
					msg += " " + tag + "=" + value;
				count++;
			}
			msg += "\n";
			logger.debug(PC2LogCategory.Settings, subCat, msg);
		}
	}
	
	/**
	 * Reads the configuration parameters from the specified worksheet in the 
	 * Xcel spreadsheet. 
	 * 
	 * @param arglist - the arguments of the spreadsheet to read, such as file
	 * 			name, sheet, and columns to read.
	 * @param forRegistrar - a flag stating whether to use the hashKey as the key
	 * 			in the properties table or the value of the IP property
	 * @param forRemoval - whether this is being added or removed from the 
	 * 			properties table.
	 * 
	 * @return - returns the Network Element Label this properties were saved under or
	 * null.
	 */
	@SuppressWarnings("deprecation")
	private Hashtable<String, Properties> readConfigParameters(String[] arglist) {

		POIFSFileSystem fs = null;
		HSSFWorkbook wb = null;

		int xlsSheetIndex = 0;
		ArrayList<Object> hashKeyList = new ArrayList<Object>();
		String hashKey = null;
		String propertyKey = null;
		String propertyValue = null;
		Hashtable<String, Properties> table = null;
		// verify execution syntax - make sure proper number of parameters are passed in
		if (arglist.length != 5) {
			logger.trace(PC2LogCategory.Settings, subCat, 
					"Syntax: configparams <xls_filename> <xls_sheetname> <hashkey_column> <propertykey_column> <propertyvalue_column>\n");
			//System.out.print("Syntax: configparams <xls_filename> <xls_sheetname> <hashkey_column> <propertykey_column> <propertyvalue_column>\n");
			return table;
		}
		String xlsFileName = arglist[0];
		String xlsSheetName = arglist[1];
		String xlsHashKeyColumn = arglist[2];
		String xlsPropertyKeyColumn = arglist[3];
		String xlsPropertyValueColumn = arglist[4];
		logger.trace(PC2LogCategory.Settings, subCat,
				"Excel File Name is: " + xlsFileName 
				+ "\nExcel Sheet Name is: " + xlsSheetName
				+ "\nExcel Key Column is: " + xlsHashKeyColumn
				+ "\nExcel Field Name Column is: " + xlsPropertyKeyColumn
				+ "\nExcel Value Column is: " + xlsPropertyValueColumn);


		// use POI to read the excel file
		try {
			fs = new POIFSFileSystem(new FileInputStream(xlsFileName));
			logger.trace(PC2LogCategory.Settings, subCat, "FS= " + fs);
		} catch (IOException exception) {
			logger.trace(PC2LogCategory.Settings, subCat,
					"Failed to read the file named " + xlsFileName);
		};

		// read the workbook
		try {
			wb = new HSSFWorkbook(fs);
		} catch (IOException exception) {
			logger.trace(PC2LogCategory.Settings, subCat,
					"Failed to create a workbook");
		};

		try {
			xlsSheetIndex = wb.getSheetIndex(xlsSheetName);
			HSSFSheet sheet = wb.getSheetAt(xlsSheetIndex);
			HSSFRow row = null;
			HSSFCell cell = null;
			boolean formatKnown = false;
			table = new Hashtable<String, Properties>();
			int initialRow = 0;
			if(sheet.getRow(0) == null ) //|| sheet.getRow(0).getCell(Short.decode("0")) != null)
				initialRow = 1;
			

			int rows = sheet.getLastRowNum();
			for (int r = initialRow; r <= rows; r++) {
				row = sheet.getRow(r);
				if (row != null) {
					if (xlsHashKeyColumn != null) {
						cell = row.getCell(Short.decode(xlsHashKeyColumn));
						// Support the original format of the settings files where the network element is in column 1 instead of zero
						if (!formatKnown && cell == null && xlsHashKeyColumn.equals("0")) {
							xlsHashKeyColumn = "1";
							xlsPropertyKeyColumn = "3";
							xlsPropertyValueColumn = "4";
							cell = row.getCell(Short.decode(xlsHashKeyColumn));
						}
						formatKnown = true;
					
					}
					else if (!formatKnown){
						hashKey = SettingConstants.DUT;
						cell = row.getCell(Short.decode("0"));
						// Support the original format of the settings files where the network element is in column 1 instead of zero
						cell = row.getCell(Short.decode(xlsPropertyKeyColumn));
						propertyKey = getXlsCellStringValue(cell);
						if (cell == null || propertyKey.startsWith("Step 1")) {
							xlsPropertyKeyColumn = "3";
							xlsPropertyValueColumn = "4";
							
						} 
						cell = null;
						formatKnown = true;
						
					}
					if (cell != null) {
						hashKey = getXlsCellStringValue(cell);
						
					}
					if ((hashKey != null) && (hashKey != "")) {
						if (!hashKeyList.contains(hashKey)) {
							hashKeyList.add(hashKey);
							table.put(hashKey, new Properties());
						}
					}
					Properties p = null;
					if (hashKey != null)
						p = table.get(hashKey);
					if (p != null) {
							
						cell = row.getCell(Short.decode(xlsPropertyKeyColumn));
						propertyKey = getXlsCellStringValue(cell);
						cell = row.getCell(Short.decode(xlsPropertyValueColumn));
						propertyValue = getXlsCellStringValue(cell);

						if (propertyKey.equals("MAC Address")) {
							p.setProperty("LinkLocalAddress", createLinkLocalAddress(propertyValue));
						}
						// PCPCSII-125  
						// Create a colon verions of each MAC Address in the configuration files
						if (propertyKey.contains("MAC Address")) {
							String value = createMACAddressWithColons(propertyValue);
							p.setProperty(propertyKey + " Colon", value);
						}

						// Before putting the key/value pair into the property class,
						// (except for the LinkLocalAddress property),
						// see if it is an IP property and if so determine if the
						// value is an IPv6 address using the compressed form.
						if (propertyKey.contains(SettingConstants.IP) &&
								propertyValue.contains(SettingConstants.IP_COMP_FORM)) {
							try {
								propertyValue = Conversion.ipv6LongForm(propertyValue);
							}
							catch (IllegalArgumentException iae) {
								logger.error(PC2LogCategory.Settings, subCat, 
										hashKey + "- Error processing key=" + propertyKey + " value=" + propertyValue 
										+ ". Detected an invalid IPv6 address.");
							}

						}

						if (propertyKey != null && propertyValue != null &&
								propertyKey != "" && propertyValue != "")
							p.setProperty(propertyKey, propertyValue);
						cell = null;
						logger.trace(PC2LogCategory.Settings, subCat,
								hashKey + "- Adding key=" + propertyKey + " value=" + propertyValue);
								
								
						}
					}
				}
			
			for (int q = 0; q < hashKeyList.size(); q++) {

				String currentHashKey = hashKeyList.get(q).toString();
				Properties p = table.get(currentHashKey);
				addDeviceType(currentHashKey, p);
				// key = currentHashKey;
				if (currentHashKey.equals("DUT")) {
					String deviceType = p.getProperty(SettingConstants.DEVICE_TYPE);
					if (deviceType != null) {
						table.put(deviceType+"0", p);
						logger.debug(PC2LogCategory.PCSim2, subCat, 
							"Adding label=" + deviceType + "0 to system settings.");
						
					}
				}
				else if (currentHashKey.startsWith("UE")) {
					String sim = p.getProperty(SettingConstants.SIMULATED);
					if (sim != null &&
							(sim.equalsIgnoreCase("false") ||
							 sim.equalsIgnoreCase("no")||
							 sim.equalsIgnoreCase("disable"))) {
						String pui = p.getProperty(SettingConstants.PUI);
						String pui2 = p.getProperty(SettingConstants.PUI2);
						if (pui != null) {
							table.put(pui, p);
							logger.debug(PC2LogCategory.PCSim2, subCat, 
									"Adding label=" + pui + " to system settings.");
							
						}
						if (pui2 != null && !pui2.equals(pui)) {
							table.put(pui2, p);
							logger.debug(PC2LogCategory.PCSim2, subCat, 
									"Adding label=" + pui2 + " to system settings.");
						}
					}
				}
			}
	
		
//			table = new Hashtable<String, Properties>();
//			
//			for (int q = 0; q < hashKeyList.size(); q++) {
//
//				String currentHashKey = hashKeyList.get(q).toString();
//				logger.trace(PC2LogCategory.Settings, subCat,
//						"****** OK hashKey(q) = " + hashKeyList.get(q) + " ******");
//				logger.trace(PC2LogCategory.Settings, subCat,
//						"Loop " + q);
//				Properties p = new Properties();
//				for (int r = 0; r <= rows; r++) {
//					row = sheet.getRow(r);
//					if (row != null) {
//						if (xlsHashKeyColumn != null) {
//							cell = row.getCell(Short.decode(xlsHashKeyColumn));
//						}
//						if (cell != null) {
//							hashKey = getXlsCellStringValue(cell);
//						}
//						else {
//							hashKey = SettingConstants.DUT;
//						}	
//						if (hashKey == hashKeyList.get(q)) {
//							cell = row.getCell(Short.decode(xlsPropertyKeyColumn));
//							propertyKey = getXlsCellStringValue(cell);
//							cell = row.getCell(Short.decode(xlsPropertyValueColumn));
//							propertyValue = getXlsCellStringValue(cell);
//
//							if (propertyKey.equals("MAC Address")) {
//								p.setProperty("LinkLocalAddress", createLinkLocalAddress(propertyValue));
//							}
//							// PCPCSII-125  
//							// Create a colon verions of each MAC Address in the configuration files
//							if (propertyKey.contains("MAC Address")) {
//								String value = createMACAddressWithColons(propertyValue);
//								p.setProperty(propertyKey + " Colon", value);
//							}
//
//							// Before putting the key/value pair into the property class,
//							// (except for the LinkLocalAddress property),
//							// see if it is an IP property and if so determine if the
//							// value is an IPv6 address using the compressed form.
//							if (propertyKey.contains(SettingConstants.IP) &&
//									propertyValue.contains(SettingConstants.IP_COMP_FORM)) {
//								try {
//									propertyValue = Conversion.ipv6LongForm(propertyValue);
//								}
//								catch (IllegalArgumentException iae) {
//									logger.error(PC2LogCategory.Settings, subCat, 
//											currentHashKey + "- Error processing key=" + propertyKey + " value=" + propertyValue 
//											+ ". Detected an invalid IPv6 address.");
//								}
//
//							}
//
//							p.setProperty(propertyKey, propertyValue);
//							logger.trace(PC2LogCategory.Settings, subCat,
//									currentHashKey + "- Adding key=" + propertyKey + " value=" + propertyValue);
//						}
//
//					}	
//				}
//
//				if (!(currentHashKey.equals("Network Element Label") ||
//						currentHashKey.equals("Label"))){
//					addDeviceType(currentHashKey, p);
//					table.put(currentHashKey, p);
//					// key = currentHashKey;
//					if (currentHashKey.equals("DUT")) {
//						String deviceType = p.getProperty(SettingConstants.DEVICE_TYPE);
//						if (deviceType != null) {
//							table.put(deviceType+"0", p);
//							logger.debug(PC2LogCategory.PCSim2, subCat, 
//								"Adding label=" + deviceType + "0 to system settings.");
//							
//						}
//					}
//					else if (currentHashKey.startsWith("UE")) {
//						String sim = p.getProperty(SettingConstants.SIMULATED);
//						if (sim != null &&
//								(sim.equalsIgnoreCase("false") ||
//								 sim.equalsIgnoreCase("no")||
//								 sim.equalsIgnoreCase("disable"))) {
//							String pui = p.getProperty(SettingConstants.PUI);
//							String pui2 = p.getProperty(SettingConstants.PUI2);
//							if (pui != null) {
//								table.put(pui, p);
//								logger.debug(PC2LogCategory.PCSim2, subCat, 
//										"Adding label=" + pui + " to system settings.");
//								
//							}
//							if (pui2 != null && !pui2.equals(pui)) {
//								table.put(pui2, p);
//								logger.debug(PC2LogCategory.PCSim2, subCat, 
//										"Adding label=" + pui2 + " to system settings.");
//							}
//						}
//					}
//				}
//
//			}
			

		} catch (Exception e) {
			logger.error(PC2LogCategory.Settings, subCat,
					"Check xls workbook name, sheet name, and column parameters.");
			e.printStackTrace();
		}
		return table;

	}
	

	private Hashtable<String, Properties> readDUTParameters(String fileName) {
		Properties p = new Properties();
		Hashtable<String, Properties> table = null;
    	try {
    		p.load(new FileInputStream(fileName));
    		table = new Hashtable<String, Properties>();
    		String value = p.getProperty(SettingConstants.MAC_ADDRESS);
    		if (value != null) {
    			p.setProperty("LinkLocalAddress", createLinkLocalAddress(value));
    			String colonMAC = createMACAddressWithColons(value);
    			p.setProperty(SettingConstants.MAC_ADDRESS + " Colon", colonMAC);
    		}
    		// PCPCSII-125  
    		// Create a colon verions of each MAC Address in the configuration files
    		value = p.getProperty(SettingConstants.CABLE_MODEM_MAC_ADDRESS);
    		if (value != null) {
    			String colonMAC = createMACAddressWithColons(value);
    			p.setProperty(SettingConstants.CABLE_MODEM_MAC_ADDRESS + " Colon", colonMAC);
    		}
    		addDeviceType(SettingConstants.DUT, p);
    		String deviceType = p.getProperty(SettingConstants.DEVICE_TYPE);
    		if (deviceType != null) {
    			table.put(deviceType+"0", p);
    			logger.debug(PC2LogCategory.PCSim2, subCat, 
    					"Adding label=" + deviceType + "0 to system settings.");

    		}
    		table.put(SettingConstants.DUT, p);
 
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }

		return table;
	}
	
	private Hashtable<String, Properties> readPlatformParameters(String fileName) {
	
		Properties p = new Properties();
		Hashtable<String, Properties> table = null;
    	try {
    		File f = new File(fileName);
    		String d = f.getParent();
    		p.load(new FileInputStream(f));
    		table = new Hashtable<String, Properties>();
    		
    		Enumeration<Object> e = p.keys();
    		while(e.hasMoreElements()) {
    			String key = (String)e.nextElement();
    			if (key.startsWith(SettingConstants.UE) ||
    					key.startsWith(SettingConstants.PCSCF) ||
    					key.startsWith(SettingConstants.SCSCF)) {
    				String propFile = p.getProperty(key);
    				if (propFile.endsWith(".properties")) {
    					Properties ne = new Properties();
    					File temp = new File(d + File.separator + propFile);
    					ne.load(new FileInputStream(d + File.separator + propFile));
    					String label = ne.getProperty(SettingConstants.NE);
    					addDeviceType(label, ne);
    					if (label != null) {
    						if (label.startsWith(SettingConstants.UE)) {
    							String sim = p.getProperty(SettingConstants.SIMULATED);

    							if (sim != null &&
    									(sim.equalsIgnoreCase("false") ||
    											sim.equalsIgnoreCase("no")||
    											sim.equalsIgnoreCase("disable"))) {
    								String pui = p.getProperty(SettingConstants.PUI);
    								String pui2 = p.getProperty(SettingConstants.PUI2);
    								if (pui != null) {
    									table.put(pui, p);
    									logger.debug(PC2LogCategory.PCSim2, subCat, 
    											"Adding label=" + pui + " to system settings.");

    								}
    								if (pui2 != null && !pui2.equals(pui)) {
    									table.put(pui2, p);
    									logger.debug(PC2LogCategory.PCSim2, subCat, 
    											"Adding label=" + pui2 + " to system settings.");
    								}
    							}
    						}
    						table.put(label,  ne);
    					}
    				}
    			}
    		}

    		table.put(SettingConstants.PLATFORM, p);
 
    	} catch (IOException ex) {
    		ex.printStackTrace();
        }

		return table;
	}

	/**
	 * Common operation to determine if a setting value is set
	 * to "on", "true" or "enable".
	 * 
	 * @param value - the setting to test
	 * @return true if it matches one of the specified strings
	 */
	public static boolean resolveBooleanSetting(String value) {
		if (value != null && (value.equalsIgnoreCase("true") ||
				value.equalsIgnoreCase("on") ||
				value.equalsIgnoreCase("enable"))) {
			return true;
		}
		return false;
	}

	/**
	 * This method resets platfrom settings that may have been
	 * overwritten from within a Test Script document for one
	 * test, but should not affect all tests.
	 *
	 */
	public void reset()	{
		globals.clear();
		this.curRunNum = null;
		this.curLogName = null;
		this.inspect = originalPlatformInspectorSetting;
		this.gruu = this.originalGruu;
		this.precondition = this.originalPrecondition;
		this.reliability = this.originalReliability;
		this.captures = 0;
		this.telephoneDirectory = null;
		
		// Next we need to set the noResponseTimeout interval to that of the
		// platform settings file on the chance that it was overwritten in the
		// test script that was just executed.
		SystemSettings.noResponseTimeout = Integer.parseInt(
				originalPlatform.getProperty(SettingConstants.NO_RESP_TIMEOUT));

		// Now we need to restore any of the dynamic settings that were
		// changed at the start of the test.
		String [] dynamicKeys = SettingConstants.getDynamicSettingsKeys();
		Properties platform = settings.get(SettingConstants.PLATFORM);
		for (int i=0; i< dynamicKeys.length; i++) {
			String value = this.originalPlatform.getProperty(dynamicKeys[i]);
			if (value != null)
				platform.setProperty(dynamicKeys[i], value);
		}
		// Now we want to remove all elements that end with the value '0'
		Enumeration<String> keys = settings.keys();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			if (key.endsWith("0")) {
				settings.remove(key);
				logger.debug(PC2LogCategory.Settings, subCat, 
						"Removing network label (" 
						+ key + " from active settings.");
			}
		}
		
	}

	public void setCaptureInterface(int i) {
		if (i > 0)
			this.captureInterface = i;
	}
	
	/** 
	 * This method returns an array of string. The even elements
	 * are the pui and the odd elements are the new network element
	 * label that is being assigned to the dut
	 * @param fileName
	 * @return
	 */
	public String [] setDUTProperties(String fileName) {
		Hashtable<String, Properties> dutProp = dutSettings.get(fileName);
		String [] changes = null;
		if (dutProp != null && dutProp.size() > 0) {
			changes = new String [dutProp.size()];
			int i = 0;
			Enumeration<String> keys = dutProp.keys();
			while(keys.hasMoreElements()) {
				String key = keys.nextElement();
				Properties p = dutProp.get(key);
				logger.info(PC2LogCategory.Settings, subCat, 
						"Settings are adding the network element label " + key + 
						" for file " + fileName);
				settings.put(key, p);
				changes[i++] = key;
			}
		}
		return changes;
	}
	
	public void setDynamicPlatformSettings(Properties p) {
		Properties platform = settings.get(SettingConstants.PLATFORM);
		if (platform != null && p != null) {
			Enumeration<Object> e = p.keys();
			while (e.hasMoreElements()) {
				String key = (String)e.nextElement();
				String newValue = p.getProperty(key);
				if (newValue != null) {
					logger.info(PC2LogCategory.Settings, subCat, 
						"Dynamically changing  \"" 
						+ key + "\" configuration from " 
						+ platform.getProperty(key) + " to " 
						+ newValue + ".");
					platform.setProperty(key, newValue);
					if (key.equals(SettingConstants.NO_RESP_TIMEOUT))
						SystemSettings.noResponseTimeout = Integer.parseInt(newValue);
					else if (key.equals(SettingConstants.SIP_INSPECTOR)) {
						this.inspect = resolveBooleanSetting(newValue);
					}
				}
			}
		}
	}
	public void setGRUUExtension(Extension e) {
		this.gruu = e;
	}

	public void setReliabilityExtension(Extension e) {
		this.reliability = e;
	}

	public void setPreconditionExtension(Extension e) {
		this.precondition = e;
	}

	private void setVoicetronixMapping() {
		Properties platform = settings.get(SettingConstants.PLATFORM);
		VoicetronixPort defaultVP = null;
		for (int i=0; i<VOICE_PORTS; i++) {
			String key = SettingConstants.VOICE_PORT+i;
			String value = platform.getProperty(key);
			if (value != null) {
				StringTokenizer tokens = new StringTokenizer(value);
				if (tokens.countTokens() == 3) {
					String label = tokens.nextToken();
					String line = tokens.nextToken();
					if (line.equals("line")) {
						String lineNum = tokens.nextToken();
						Properties device = settings.get(label);
						if (device != null) {
							try {
								String lines = device.getProperty(SettingConstants.PHONE_LINES);
								Integer deviceLines = Integer.parseInt(lines);
								Integer num = Integer.parseInt(lineNum);
								if (deviceLines >= num) {
									VoicetronixPort vp = new VoicetronixPort(i, label, num);
									vtports[i] = vp;
								}
								else {
									logger.error(PC2LogCategory.Settings, subCat,"Device " + label + " doesn't have enough phone lines "
											+ " in the configuration files. Assigning no voicetronix mapping for port " + i + ".");
								}
							}
							catch (Exception ex){
								logger.error(PC2LogCategory.Settings, subCat,"Unable to convert line number to a integer " + label 
										+ " in the configuration files. Assigning no voicetronix mapping for port " + i + ".");
							}

						}	
						else if (label.equals(SettingConstants.DUT) ||
								label.equals("UE0")) {
							try {
								Integer num = Integer.parseInt(lineNum);
								VoicetronixPort vp = new VoicetronixPort(i, label, num);
								if (defaultVP == null)
									defaultVP = vp;
								else if (defaultVP.getLine() >= num) {
									defaultVP = vp;
								}
								vtports[i] = vp;
							}
							catch (Exception ex){
								logger.error(PC2LogCategory.Settings, subCat,"Unable to find convert line number to a integer " + label 
										+ " in the configuration files. Assigning no voicetronix mapping for port " + i + ".");
							}
						}

						else {
							logger.error(PC2LogCategory.Settings, subCat,"Unable to find network element label " + label 
									+ " in the configuration files. Assigning no voicetronix mapping for port " + i + ".");
						}
					}
					else {
						logger.error(PC2LogCategory.Settings, subCat,"Didn't find the keyword line in the correct position " + value 
								+ " Format should be <Network Element Label> line <line number> (e.g. UE0 line 2.\nAssigning no voicetronix mapping for port " + i + ".");
					}
				}
			}
			else {
				logger.info(PC2LogCategory.Settings, subCat, "Did not find any voiceport settings for voiceport" + i + ".");
			}

		}
		defaultVTPort = defaultVP.getPort();
		
	}


	
	public boolean useInspector() {
		return this.inspect;
	}

	public static boolean useTransportParameter() {
		return useTransportParameter;
	}
}
