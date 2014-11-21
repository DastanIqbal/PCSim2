/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.provgen;

import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.Arrays;
import com.cablelabs.log.*;
import com.cablelabs.common.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
public class ProvGen {

	/**
	 * The input file can be an individual file or a directory
	 */
	private File input = null;

	/**
	 * All of the bytes read from the input file along with any modifications
	 * made through one of the "change" methods.
	 */
	private byte [] buffer = null;
	
	//private boolean isDirectory = false;

	private LogAPI logger = LogAPI.getInstance();

	private String subCat = "ProvGen";

	private static final int DIGIT_MAP_OFFSET = 27;
	private static final int PARENT_OFFSET = 7;
	private static final int GRANDPARENT_OFFSET = 3;
	private static final int SHA_OFFSET = 42;

	private static byte [] HASH_PATTERN = { 0x0b, 0x25, 0x30, 0x23, 0x06, 0x0b, 0x2b, 0x06, 
		0x01, 0x02, 0x01, (byte)0x81, 0x0c, 0x01, 0x02, 0x0b, 
		0x00, 0x04, 0x14 };

	private static String AREA_CODE_PATTERN = "areaCode = \"";
	
	private static byte [] PCSCF_IPv4_PATTERN = { 0x0a, 0x20, 0x00, (byte)0x42 };


    private static byte [] PCSCF_IPv6_PATTERN = {0x00, 0x01, 0x00, 0x02, 0x00, 0x03, 0x00, 0x04, 0x00, 0x05, 0x00, 0x06, 0x00, 0x07, (byte)0x00, (byte)0x08 };



	public ProvGen(File input, byte [] pcscfPattern) {
		this.input = input;
		if (pcscfPattern.length == 4)
			PCSCF_IPv4_PATTERN = pcscfPattern;
		else if (pcscfPattern.length == 16) 
			PCSCF_IPv6_PATTERN = pcscfPattern;
		
		if (input != null && input.canRead() && input.isFile()) {
			int length = (int)input.length();
			buffer = new byte [(int)length];
			try {
				FileInputStream fis = new FileInputStream(input);
				fis.read(buffer, 0, length);
				fis.close();
			}
			catch (Exception ex) {
				logger.error(LogCategory.APPLICATION, subCat, 
						"ProvGen encountered an exception while trying to read the file[" 
						+ input.getName() + "].\n" + ex.getMessage());
			}
		}
		else {
			logger.warn(LogCategory.APPLICATION, subCat, "Platform could not find or read the provisioning file name(" + input +").");
		}
		
	}
	
	public ProvGen(File input) {
		this.input = input;

		if (input != null && input.canRead() && input.isFile()) {
			int length = (int)input.length();
			buffer = new byte [(int)length];
			try {
				FileInputStream fis = new FileInputStream(input);
				fis.read(buffer, 0, length);
				fis.close();
			}
			catch (Exception ex) {
				logger.error(LogCategory.APPLICATION, subCat, 
						"ProvGen encountered an exception while trying to read the file[" 
						+ input.getName() + "].\n" + ex.getMessage());
			}
		}
		else {
			logger.warn(LogCategory.APPLICATION, subCat, "Platform could not find or read the provisioning file name(" + input +").");
		}
	}

	public void changeDigitMap(File digitMap) throws IllegalArgumentException {
		if (!digitMap.exists()) {
			throw new IllegalArgumentException("The new digit map file doesn't exist.");
		}
		else if (!digitMap.isFile()) {
			throw new IllegalArgumentException("The new digit map argument is not a file.");
		}
		else if (!digitMap.canRead()) {
			throw new IllegalArgumentException("The new digit map file is not readable.");
		}

		int length = (int)digitMap.length();
		if (length > 0) {
			try {
				byte [] mapBuffer = new byte [(int)length];
				FileInputStream fis = new FileInputStream(digitMap);
				fis.read(mapBuffer, 0, length);
				fis.close();

				updateDigitMap(mapBuffer);
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("Changing Digit Map failed due to exception,\n" + ex.getMessage()); 
			}
		}
		else {
			throw new IllegalArgumentException("The new digit map file doesn't contain any information.");
		}

	}

	public void changeDigitMap(String digitMap) {
		updateDigitMap(digitMap.getBytes());
	}

	public void changePCSCF(String ipAddr) {
		if (input != null) {
			try {
				InetAddress ia = InetAddress.getByName(ipAddr);
				if (ia != null) {
					if (ia instanceof Inet4Address) {
						byte [] addr = ia.getAddress();
						int offset = 0;
						while (offset+3 < buffer.length) {
							if (buffer[offset] == PCSCF_IPv4_PATTERN[0]) {
								if (buffer[offset+1] == PCSCF_IPv4_PATTERN[1] &&
										buffer[offset+2] == PCSCF_IPv4_PATTERN[2] &&
										buffer[offset+3] == PCSCF_IPv4_PATTERN[3]) {
									//System.out.println("pattern at " + offset);
									if (addr.length == PCSCF_IPv4_PATTERN.length) {
										buffer[offset] = addr[0];
										buffer[offset+1] = addr[1];
										buffer[offset+2] = addr[2];
										buffer[offset+3] = addr[3];
									}
									else {
										//System.out.println(
										logger.error(LogCategory.APPLICATION, subCat, 
										"Couldn't update the PCSCF setting because the length of the ipAddr address is not 4.");
									}
									offset = buffer.length;
								}
								else 
									offset++;
							}
							else 
								offset++;
						}
					}
					else if (ia instanceof Inet6Address) {
						byte [] addr = ia.getAddress();
						int offset = 0;
						while (offset+15 < buffer.length) {
							if (buffer[offset] == PCSCF_IPv6_PATTERN[0]) {
								if (buffer[offset+1] == PCSCF_IPv6_PATTERN[1] &&
										buffer[offset+2] == PCSCF_IPv6_PATTERN[2] &&
										buffer[offset+3] == PCSCF_IPv6_PATTERN[3] &&
										buffer[offset+4] == PCSCF_IPv6_PATTERN[4] &&
										buffer[offset+5] == PCSCF_IPv6_PATTERN[5] &&
										buffer[offset+6] == PCSCF_IPv6_PATTERN[6] &&
										buffer[offset+7] == PCSCF_IPv6_PATTERN[7] &&
										buffer[offset+8] == PCSCF_IPv6_PATTERN[8] &&
										buffer[offset+9] == PCSCF_IPv6_PATTERN[9] &&
										buffer[offset+10] == PCSCF_IPv6_PATTERN[10] &&
										buffer[offset+11] == PCSCF_IPv6_PATTERN[11] &&
										buffer[offset+12] == PCSCF_IPv6_PATTERN[12] &&
										buffer[offset+13] == PCSCF_IPv6_PATTERN[13] &&
										buffer[offset+14] == PCSCF_IPv6_PATTERN[14] &&
										buffer[offset+15] == PCSCF_IPv6_PATTERN[15]) {
									//System.out.println("pattern at " + offset);
									if (addr.length == PCSCF_IPv6_PATTERN.length) {
										buffer[offset] = addr[0];
										buffer[offset+1] = addr[1];
										buffer[offset+2] = addr[2];
										buffer[offset+3] = addr[3];
										buffer[offset+4] = addr[4];
										buffer[offset+5] = addr[5];
										buffer[offset+6] = addr[6];
										buffer[offset+7] = addr[7];
										buffer[offset+8] = addr[8];
										buffer[offset+9] = addr[9];
										buffer[offset+10] = addr[10];
										buffer[offset+11] = addr[11];
										buffer[offset+12] = addr[12];
										buffer[offset+13] = addr[13];
										buffer[offset+14] = addr[14];
										buffer[offset+15] = addr[15];
									}
									else {
										//System.out.println(
										logger.error(LogCategory.APPLICATION, subCat, 
										"Couldn't update the PCSCF setting because the length of the ipAddr address is not 4.");
									}
									offset = buffer.length;
								}
								else 
									offset++;
							}
							else 
								offset++;
						}
					}
				}
			}
			catch (UnknownHostException uhe) {
				
			}
		}
	}
	
	public void changePhoneNum(String origNum, String newNum) {
		try {
			if (buffer != null) {
				
				// Create a string for indexing the location to replace
				// Some of the bytes are not retained in the conversion to 
				// String so we use it only for indexing.
				String temp = new String(buffer, 0, buffer.length, "ISO-8859-1");
				String origAC = getAreaCode(origNum);
				String newAC = getAreaCode(newNum);
								
				if (origNum.length() == newNum.length()) {
					// We need to replace each location with the same number of bytes
					int offset = temp.indexOf(origNum);
					byte [] tele = newNum.getBytes();
					while (offset != -1) {
						System.arraycopy(tele, 0, buffer, offset, tele.length);
						offset += tele.length;
						offset = temp.indexOf(origNum, offset);
					}
								
					// Next see if we need to update the area code in the digit map
					if (!origAC.equals(newAC)) {
					    offset = temp.indexOf(AREA_CODE_PATTERN);
						while (offset != -1) {
							offset += AREA_CODE_PATTERN.length();
							System.arraycopy(newAC.getBytes(), 0, buffer, offset, 3);
							offset = temp.indexOf(AREA_CODE_PATTERN, offset);
						}
					}

					
				}
			}
		}
		catch (Exception ex) {
			logger.error(LogCategory.APPLICATION, subCat, 
			//System.err.println(
					"ProvGen encountered an exception while trying to change phone numbers from[" 
					+ origNum + " to " + newNum +"].\n" + ex.getMessage());
		}
	}

	/**
	 * This method determines if the embedded has is included in the data
	 * or not.
	 * @param buffer - the binary data to examine for the embedded hash
	 * @return - true if the buffer has the embedded hash, false otherwise
	 */
	private boolean containsEmbeddedHash(byte [] buffer) {
		byte [] tmp = new byte [HASH_PATTERN.length];
		System.arraycopy(buffer, (buffer.length-SHA_OFFSET), tmp, 0, HASH_PATTERN.length);
//		System.out.println(
//				"tmp \n" + Conversion.hexString(tmp, 0, HASH_PATTERN.length) +
//				"hash\n" + Conversion.hexString(HASH_PATTERN, 0, HASH_PATTERN.length));
		
		boolean hasHash = Arrays.equals(tmp, HASH_PATTERN);
		return hasHash;
	}

	public void compare(File orig, File newFile) {
		try {
			int origLen = (int)orig.length();
			byte [] origBuf = new byte [(int)origLen];
			FileInputStream fis = new FileInputStream(orig);
			fis.read(origBuf, 0, origLen);
			fis.close();

			int newLen = (int)newFile.length();
			byte [] newBuf = new byte [(int)newLen];
			fis = new FileInputStream(newFile);
			fis.read(newBuf, 0, newLen);
			fis.close();

			int end = 0;
			if (origLen > newLen) {
			    logger.info(LogCategory.APPLICATION, subCat, 
						"Original file has " + (origLen - newLen) + " more bytes than the newFile.");
				end = newLen;
			}
			else if (origLen < newLen) {
				logger.info(LogCategory.APPLICATION, subCat, 
						"The new file has " + (newLen - origLen) + " more bytes than the original file.");
				end = origLen;
			}
			else if (origLen > 0){
				end = origLen;
			}
//			int replace = -1;
			String pos = null;
//			int run = 0;
//			int [] ndx = new int [2];
			for (int i=0; i<end; i++) {
//				boolean match = false;
				if (origBuf[i] != newBuf[i]) {
					if (pos == null) {
						pos = Integer.toString(i);
					}
					else {
						pos += ", " + i;
					}
				}
			}

			if (pos != null) {
				logger.info(LogCategory.APPLICATION, subCat,  
						"The files differ at positions:\n" + pos);
			}

		}
		catch (Exception ex) {
			logger.error(LogCategory.APPLICATION, subCat, 
					"ProvGen encountered an exception while trying to compare files[" 
					+ orig + ", " + newFile +"].");
		}
	}

	/**
	 * Generates a new SHA-1 hash value for a configuration file that contains
	 * an embedded hash TLV.
	 * 
	 * @param newBuf
	 * @return - the configuration file with the new corrected embedded hash
	 */
	private byte [] generateHash(byte [] newBuf) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-1");
			int hashPos = newBuf.length - SHA_OFFSET;
			// The hash is calculated without the original hash
			// Therefore create a copy of the data without the hash TLV
			// Calculate the SHA-1 hash and then substitute the new
			// value in the buffer.
			byte [] tmp = new byte [hashPos+3];

			System.arraycopy(newBuf, 0, tmp, 0, hashPos);
			System.arraycopy(newBuf, hashPos + SHA_OFFSET -3 , tmp, tmp.length-3, 3);
			byte [] hash = md.digest(tmp);
			System.arraycopy(hash, 0, newBuf, hashPos + 19, hash.length);
			
		}
		catch (NoSuchAlgorithmException ex) {
			logger.warn(LogCategory.APPLICATION, subCat,
					"Could not obtain SHA-1 Message Digest for ProvGen.", ex);
		}
		return newBuf;
	}
	
	public boolean output(String newName) {
		try {
			// Now the check to see if the item is to update the embedded hash information for the file
			// if it contains one.
			
			if (containsEmbeddedHash(buffer)) {
				buffer = generateHash(buffer);
			}
			
			File output = new File(newName);
			FileOutputStream fos = new FileOutputStream(output);
			fos.write(buffer);
			fos.close();
			return true;
		}
		catch (Exception ex) {
			logger.error(LogCategory.APPLICATION, subCat, 
			//System.err.println(
					"ProvGen encountered an exception while trying to write the output to file[" 
					+ newName + "].\n" + ex.getMessage());
		}
		return false;
	}

	/**
	 * This method determines the area code for a 10 or 11 digit phone number
	 * @param number - the number to extract the area code from
	 * @return - the area code if found, null otherwise
	 */
	private String getAreaCode(String number) {
		if (number.length() == 10)
			return number.substring(0, 3);
		else if (number.length() == 11) {
			return number.substring(1, 4);
		}
		return null;
	}

	private int locateDigitMap(byte [] cfg) {
		int end = cfg.length;
		boolean found = false;
		for (int i=0; (i<end-27 && !found); i++) {
			if (cfg[i] == 0x40) {
//				byte a = origBuf[i+3];
//				byte b = origBuf[i+7];
//				byte c = origBuf[i+27];
				if (cfg[i+GRANDPARENT_OFFSET] == 0x30 &&
						cfg[i+PARENT_OFFSET] == 0x06 &&
						cfg[i+DIGIT_MAP_OFFSET] == 0x04)
					return i;
			}
		}
		return -1;
	}
	
	private void updateDigitMap(byte [] bytes) {
//		File [] temps = null;
//		if (isDirectory) {
//			temps = input.listFiles();
//		}
//		else {
//			temps = new File [1];
//			temps[0] = input;
//		}

//		int files = temps.length;
//		for (int i = 0; i < files; i++) {
//			File temp = temps[i];
//			int length = (int)temp.length();
			try {
//				byte [] tmpBuf = new byte [(int)length];
//				FileInputStream fis = new FileInputStream(temp);
//				fis.read(tmpBuf, 0, length);
//				fis.close();

				int location = locateDigitMap(buffer);
				if (location != -1) {
					int map = location + DIGIT_MAP_OFFSET;
					// Get all of the current length values in the nested data
					int gp = Conversion.byteArrayToInt(buffer, location+1, 2);
					int p = Conversion.byteArrayToInt(buffer, location+5, 2);
					int dm = Conversion.byteArrayToInt(buffer, map+2, 2);

					// Calculate the new nested length
					int newGP = gp - dm + bytes.length;
					int newP = p - dm + bytes.length;

					byte [] gpBytes = Conversion.intToByteArray(newGP);
					byte [] pBytes = Conversion.intToByteArray(newP);
					byte [] dmBytes = Conversion.intToByteArray(bytes.length);

					byte [] newBuf = new byte [buffer.length - dm + bytes.length];
					// To keep the computation simple, replace the ancestor's length values
					buffer[location+1] = gpBytes[2];
					buffer[location+2] = gpBytes[3];
					buffer[location+5] = pBytes[2];
					buffer[location+6] = pBytes[3];
					buffer[map+2] = dmBytes[2];
					buffer[map+3] = dmBytes[3];

					// Now to actually replace the data
					System.arraycopy(buffer, 0, newBuf, 0, map+4);
					System.arraycopy(bytes, 0, newBuf, map+4, bytes.length);
					System.arraycopy(buffer, (map+4+dm), newBuf, (map+4+bytes.length), (buffer.length-dm-(map+4)));

					// Now the last item is to update the embedded hash information for the file
					// if it contains one.
					if (containsEmbeddedHash(newBuf)) {
						newBuf = generateHash(newBuf);
					}

					buffer = newBuf;
//					try {
//					byte [] empty = new byte[20];
//					MessageDigest md = MessageDigest.getInstance("SHA-1");
//					int hashPos = newBuf.length - SHA_OFFSET;
//					byte [] tmp = new byte [hashPos+3];
//					byte [] tmp2 = new byte [hashPos+3];
//					System.arraycopy(newBuf, 0, tmp2, 0, hashPos);
//					tmp2[hashPos] = newBuf[newBuf.length-3];
//					tmp2[hashPos+1] = newBuf[newBuf.length-2];
//					tmp2[hashPos+2] = newBuf[newBuf.length-1];

//					//System.arraycopy(empty, 0, newBuf, hashPos+2, 20);
//					System.arraycopy(newBuf, 0, tmp, 0, hashPos);
//					System.arraycopy(newBuf, hashPos + SHA_OFFSET -3 , tmp, tmp.length-3, 3);
//					byte [] hash = md.digest(tmp);
//					boolean same = Arrays.equals(tmp, tmp2);
//					System.arraycopy(hash, 0, newBuf, hashPos + 19, hash.length);
//					}
//					catch (NoSuchAlgorithmException ex) {
//					logger.warn(PC2LogCategory.Examiner, subCat,
//					"Could not obtain SHA-1 Message Digest for ProvGen.", ex);
//					}
//					String name = input.getName();
//					String file = null;
//					int index = name.lastIndexOf(".");
//					if (index != -1) {
//						file = name.substring(0, index) + "_dm" + name.substring(index);
//					}
//					else 
//						file = name + "_dm";
//
//					File output = new File(file);
//					FileOutputStream fos = new FileOutputStream(output);
//					fos.write(newBuf, 0, newBuf.length);
//					fos.close();

				}

			}
			catch (Exception ex) {
				logger.error(LogCategory.APPLICATION, subCat, 
				//System.err.println(
						"ProvGen encountered an exception while trying to update the digit map in file[" 
						+ input.getName() + "].");
			}
//		}
	}

	public static String usage() {
			Package p = Package.getPackage("com.cablelabs.provgen");
			String version = "unknown";
			if (p != null) {
				version = "v. " + 
					p.getSpecificationVersion() +
					" rev-" + 
					p.getImplementationVersion();
			}
			String result = 
			"ProvGen(" + version  + ") takes a source PacketCable 2.0 EDVA provisioning configuration file and\n"
			+ "changes all of the phone numbers, the address of the PCSCF,"
			+ " or replaces the digit map. The tool will generate\n"
			+ "a new file starting with the same name as the source file with a '_<new phone number>'\n"
			+ "or '_dm' added depending if it is a phone number or digit map change.\n\n"
			+ "    -t [original phone number] [new phone number]\n"
			+ "        NOTE: Both phone numbers must be the same length.\n"
			+ "              All instances of the phone numbers will be replaced.\n"
			+ "              The areaCode element in the digit map will be replaced if the\n"
			+ "                  new phone number is 10 or 11 digits in length.\n"
			+ "    -s [source file]\n" 
			+ "        NOTE: The source file argument may be a single file or a complete directory.\n"
			+ "         The source file must contain the relative or absolute path of the file.\n"
			+ "    -m [new digit map file]\n"
			+ "        NOTE: The -t and -m arguments are mutually exclusive.\n"
			+ "    -p [new IP address of the PCSCF]"  
			+ "    -h usage\n\n"
			+ "  Below are examples for using the tool:\n"
			+ "\tProvGen -t 3036615000 3037771000 -s ./CallWaiting.bin\n"
			+ "\tProvGen -m ./new_digit_map.txt -s C:\\ProvConfigs\\Secure.bin\n"
			+ "\tProvGen -p 10.4.1.37 -s C:\\ProvConfigs\\Secure.bin\n"
			+ "\tProvGen -t 3036615000 3037771000 -s C:/ProvConfigs\n";
		return result;
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 1 || args.length > 5) {
			// Treat this as a usage case
			System.out.println(ProvGen.usage());
			return;
		}
		else {
			String origNum = null;
			String newNum = null;
			String src = null;
			String dm = null;
			String pcscf = null;
			String newFile = null;
			
			boolean unrecognized = false;
			int skip = 0;
			// Parse through all of the arguments first
			for (int i=0; i < args.length; i++) {
				if (args[i].equals("-t") && 
						(i+2) <= args.length) {
					origNum = args[i+1];
					newNum = args[i+2];
					skip = 2;
				}
				else if (args[i].equals("-s") &&
						(i+1) <= args.length) {
					src = args[i+1];
					skip = 1;
				}
				else if (args[i].equals("-m") &&
						(i+1) <= args.length) {
					dm = args[i+1];
					skip = 1;
				}
				else if (args[i].equals("-p") &&
						(i+1) <= args.length) {
					pcscf = args[i+1];
					skip = 1;
				}
				else if (skip > 0) {
					skip--;
				}
				else {
					// This also covers the -h argument
					unrecognized = true;
				}
			}
			
			if (unrecognized) {
				System.out.println(ProvGen.usage());
				return;
			}	
			else if (origNum != null && 
					newNum != null && 
					src != null &&
					dm == null &&
					origNum.length() == newNum.length()){
				try {
					File srcFile = new File(src);
					ProvGen pg = new ProvGen(srcFile);
					pg.changePhoneNum(origNum, newNum);
					String name = srcFile.getName();
					String file = null;
					int index = name.lastIndexOf(".");
					if (index != -1) {
						file = name.substring(0, index) + "_" + newNum + name.substring(index);
					}
					else 
						file = name + "_" + newNum;
					pg.output(file);
				}
				catch (Exception ex) {
					System.err.println(
							"ProvGen encountered an exception while trying to update the phone number in file[" 
							+ src + "].");
					System.out.println(ProvGen.usage());
					return;
				}
			}
			else if (origNum == null && 
					newNum == null && 
					src != null &&
					dm != null){
				try {
					File srcFile = new File(src);
					ProvGen pg = new ProvGen(srcFile);
					File dmFile = new File(dm);
					pg.changeDigitMap(dmFile);
					//File newFile = new File("./chinmaya_digitmap_changed.bin");
					//File glh = new File("./chinmaya_base_dm.bin");
					//pg.compare(glh, newFile);
				}
				catch (Exception ex) {
					System.err.println(
							"ProvGen encountered an exception while trying to update the digit map in file[" 
							+ src + "].");
					System.out.println(ProvGen.usage());
					return;
				}
			}
			else if (src != null &&
					pcscf != null){
				try {
					File srcFile = new File(src);
					ProvGen pg = new ProvGen(srcFile);
					pg.changePCSCF(pcscf);
					String name = srcFile.getName();
					String file = null;
					int index = name.lastIndexOf(".");
					if (index != -1) {
						file = name.substring(0, index) + "_pcscf" + name.substring(index);
					}
					else 
						file = name + "_" + newNum;
					pg.output(file);
					newFile = file;
				}
				catch (Exception ex) {
					System.err.println(
							"ProvGen encountered an exception while trying to update the digit map in file[" 
							+ src + "].");
					System.out.println(ProvGen.usage());
					return;
				}
			}
			else {
				System.err.println("Invalid arguments.");
				System.out.println(ProvGen.usage());
				return;
			}	
			

			if (newFile != null) {
				File orig = new File("./v6.C.1.2.3_converted.bin");
				File update = new File("./" + newFile);
				ProvGen pg = new ProvGen(orig);
				LogAPI.setConsoleCreated();
				pg.compare(orig, update);
				System.out.println("Complete.");
			}


//			// File orig = new File("./chinmaya_base.bin");
//			File newFile = new File("./chinmaya_digitmap_changed.bin");
//			File phoneFile = new File("./chinmaya_phone11digit.bin");
//			File glh = new File("./chinmaya_base_dm.bin");
//			File digitMap = new File("./digitmap2.txt");
//			//pg.changePhoneNum("7203217703", "3033514411");
//			//pg.compare(glh, newFile);
//			pg.changeDigitMap(digitMap);
//			pg.compare(glh, newFile);

//			//pg.compare(orig, glh);
			//pg.compare(orig, newFile);
//			pg.compare(orig, phoneFile);
//			System.out.println("Complete.");
		}
	}
}
