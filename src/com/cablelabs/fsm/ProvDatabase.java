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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

public class ProvDatabase {
	
	/**
	 * This is a table index on the name of the scripts and its associated
	 * policy file name and provisioning file name
	 */
	protected Hashtable<String, ProvisioningData> table = null;
	
	/**
	 * This is a table indexed on the MAC Address of the device which indicates
	 * the last Provisioning Template file issued for the device and whether the provisioning
	 * file was auto generated.
	 */
	protected Hashtable<String, ProvisioningData> issued = null;
	
	protected long lastModified = -1;
	/**
	 * Logger
	 */
	private LogAPI logger = LogAPI.getInstance();
	
	public static final String subCat = "PROV";
	public static final String DATABASE_DIR = SettingConstants.AUTO_PROV_FILE_DIRECTORY; //"../config/prov_db/cfg_files";
	protected static final String DELIMITER = "|";
	
	public ProvDatabase(String cw) {
		load(cw);
	}
	
	public void clearIssuedData(String macAddr) {
		issued.remove(macAddr);
	}
	
	public boolean containsKey(String testCase) {
		return table.containsKey(testCase);
	}
	
	public ProvisioningData getData(String testCase) {
		if (table != null) {
			ProvisioningData pd = table.get(testCase);
			if (pd != null) {
				return pd;
			}
			else {
				logger.warn(PC2LogCategory.PCSim2, subCat, "The testCase(" + testCase + ") could not be found in the database.");
			}
		}
		else {
			logger.warn(PC2LogCategory.PCSim2, subCat, "The database is empty.");
		}
		
		return null;
	}
	public String getPolicy(String testCase) {
		if (table != null) {
			ProvisioningData pd = table.get(testCase);
			if (pd != null)
				return pd.getPolicyFileName();
			else {
				logger.warn(PC2LogCategory.PCSim2, subCat, "The testCase(" + testCase + ") could not be found in the database.");
			}
		}
		else {
			logger.warn(PC2LogCategory.PCSim2, subCat, "The database is empty.");
		}
		return null;

	}

	public String getProvFile(String testCase) {
		if (table != null) {
			ProvisioningData pd = table.get(testCase);
			if (pd != null)
				return pd.getProvFileName();
			else {
				logger.warn(PC2LogCategory.PCSim2, subCat, "The testCase(" + testCase + ") could not be found in the database.");
			}
		}
		else {
			logger.warn(PC2LogCategory.PCSim2, subCat, "The database is empty.");
		}
		return null;
	}
	
	public boolean issued(String macAddr, ProvisioningData pd) {
		// Now see if there was a generated file for the template
		if (macAddr != null) {
			ProvisioningData prevPD = issued.get(macAddr);
			if (prevPD != null && 
					prevPD.getProvFileName().equals(pd.getProvFileName()) &&
					prevPD.getPolicyFileName().equals(pd.getPolicyFileName())) {
				logger.debug(PC2LogCategory.PCSim2, subCat, "issued method is returning true because prevPD is not null, \n"
						+ prevPD.getProvFileName() + "==" + pd.getProvFileName() + "\n" 
						+ prevPD.getPolicyFileName() + "==" + pd.getPolicyFileName());
				return true;
			}
			else {
				logger.debug(PC2LogCategory.PCSim2, subCat, "issued method is returning false because either prevPD is null, \n"
						+ null + "!=" + pd.getProvFileName() + "\n" 
						+ null + "!=" + pd.getPolicyFileName());
			}
		}
		else {
			logger.warn(PC2LogCategory.PCSim2, subCat, "MacAddr is null, returning false.");
		}
		
		return false;
	}
	
	public void load(String cw) {
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		if (platform != null) {
			String fileName = platform.getProperty(SettingConstants.AUTO_PROVISIONING_DATABASE);
			if (fileName != null) {
				File input = new File(DATABASE_DIR + File.separator + cw + File.separator + fileName);
				if (input != null && 
						input.exists() &&
						input.canRead() &&
						input.isFile()) {

					if (table == null || 
							lastModified < input.lastModified()) {
						table = new Hashtable<String, ProvisioningData>();
						// If we clean the table, clean the issued table
						issued = new Hashtable<String, ProvisioningData>();
						read(input);
						lastModified = input.lastModified();
					}
				}	
			}
		}
	}
	protected void read(File input) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(input));
			String line = in.readLine();
			int lineCount = 0;
			while (line != null) {
				// Test if the line is a comment
				lineCount++;
				if (line.length() > 0 && line.charAt(0) != '#') {
					StringTokenizer tokens = new StringTokenizer(line, DELIMITER);
					if (tokens.countTokens() == 3) {
						String testCase = tokens.nextToken();
						String policy = tokens.nextToken();
						String prov = tokens.nextToken();
						ProvisioningData pd = new ProvisioningData(testCase, policy, prov);
						table.put(testCase, pd);
					}
					else {
						logger.debug(PC2LogCategory.PCSim2, subCat,
								"Provisioning Database line[" + lineCount + "] value=["
								+ line + "] does not appear to follow the proper syntax. Ignoring entry.");
					}
				}
				line = in.readLine();
			}
		}
		catch (FileNotFoundException fnf) {
			logger.warn(PC2LogCategory.PCSim2, subCat,
					"Provisioning Database couldn't find the provisioning test file information at " 
					+ input.getAbsolutePath());
			fnf.printStackTrace();
		}
		catch (IOException io) {
			logger.warn(PC2LogCategory.PCSim2, subCat,
					"Provisioning Database encountered an error while trying to read the "
					+ "provisioning test file information in [" 
					+ input.getAbsolutePath() + "].");

			io.printStackTrace();
		}
	}
	
	public void setIssuedData(String macAddr, ProvisioningData pd) {
		issued.put(macAddr, pd);
	}
}
