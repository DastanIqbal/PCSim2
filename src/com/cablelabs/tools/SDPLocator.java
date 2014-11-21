/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.tools;

import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.SDPConstants;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;


public class SDPLocator {
	
	/**
	 * Private logger for the class
	 */
	private LogAPI logger = LogAPI.getInstance(); // Logger.getLogger("Locators");
	
	private static SDPLocator locator = null;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Locator";
	
	/**
	 * Private Constructor
	 *
	 */
	private SDPLocator() {
		
	}
	
	/**
	 * Retreives the single instance of the SDPLocator if it 
	 * already exists. If it doesn't exist it will create it prior
	 * to returning it.
	 *
	 */
	public synchronized static SDPLocator getInstance() {
		if (locator == null) {
			locator = new SDPLocator();
		}
		return locator;
	}

	/**
	 * When a parameter is a generic parameter this method returns the data
	 * portion of the parameter. When a parameter is a presence parameter it returns the 
	 * the parameter_name itself. For positional parameters, the contents of the 
	 * parameter are returned. 
	 *
	 * @param hdr - The header to find
	 * @param hdrInstance - The parameter to locate.
	 * @param bodyInstance - the instance of the body being sought
	 * @param boundary - the boundary marker within the multipart body
	 * @param msg - The message to search for the header.
	 * 
	 * @return the contents of the header without the header itself
	 *  		
	 * 
	 */
	public synchronized String getSDPHeader(String hdr,  
			String hdrInstance, String bodyInstance, String boundary, String msg) {
		int [] hdrLocation = locateSDPHeader(msg, hdrInstance, 
				bodyInstance, boundary, hdr);
		if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
			String value = msg.substring(hdrLocation[0], hdrLocation[1]);
				logger.debug(PC2LogCategory.Model, subCat,
						"SDPLocator retrieved value=[" + value 
						+ "] for the hdr=" + hdr + " instance=" + hdrInstance 
						+ ".");
				return value;
			
		}
		return null;
	}
	
	/**
	 * This method retrieves the data for a SDP parameter within a SIP message.
	 * 	 *
	 * @param hdr - The header to find
	 * @param param - The parameter to locate.
	 * @param hdrInstance - the instance of the header to extract the parameter from.
	 * @param msg - The message to search for the header.
	 * 
	 * @return the contents of the parameter or the parameter itself if it is a presence
	 * parameter.
	 * 		
	 * 
	 */
	public synchronized String getSDPParameter(String hdr, String param, 
			String hdrInstance, String bodyInstance, String boundary, String msg) {
		int [] hdrLocation = locateSDPHeader(msg, hdrInstance, 
				bodyInstance, boundary, hdr);
		if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
			int [] paramLocation = locateSDPParam(hdr, 
					param, hdrLocation, msg);
			if (paramLocation[0] != -1 && 
					paramLocation[1] != -1) {
				String value = msg.substring(paramLocation[0], paramLocation[1]);
				logger.debug(PC2LogCategory.Model, subCat,
						"SDPLocator retrieved value=[" + value 
						+ "] for the hdr=" + hdr + " instance=" + hdrInstance 
						+ " param=" + param + ".");
				return value;
			}
		}
		return null;
	}
	
	/**
	 * This method locates the starting and ending offset location from the start of the SDP body 
	 * of the specified header using the header instance to define the correct header.
	 * 
	 * @param sdp - the SDP body to search through
	 * @param hdrInstance - the instance of header being sought
	 * @param bodyInstance - the instance of the body being sought
	 * @param boundary - the boundary marker within the multipart body
	 * @param hdr - the header type being sought
	 * @return - the offset from the start of sdp that the header begins or -1
	 */
	public synchronized int [] locateSDPHeader(String sdp, String hdrInstance, 
			 String bodyInstance, String boundary, String hdr) {
		int [] hdrLocation = new int [3];
		hdrLocation[0] = -1;
		hdrLocation[1] = -1; 
		hdrLocation[2] = -1;
		
		int instance = -1;
		int offsetStart = -1;
		int mpOffset = 0;
		int mpEnd = -1;
		// For locating a header in the SDP body, we need to get the string that we
		// are looking for.
		String key = SDPConstants.getKey(hdr);
		if (key != null) {
			// Unfortunately there is some special processing around the 
			// mode, media-modifier, and desire qos precondition header 
			// lines since they can vary and the header delimiter could 
			// be of several types.
			boolean lookingForMode = false;	
			boolean lookingForMediaModifier = false;
			boolean lookingForQOSDes = false;
			boolean lookingForICECandidate = false;
			boolean lookingForFMTP = false;
			if (key.equals(SDPConstants.A_MODE))
				lookingForMode = true;
			else if (key.equals(SDPConstants.MEDIA_MODIFIER) ||
					key.equals(SDPConstants.BW))
				lookingForMediaModifier = true;	
			else if (key.equals(SDPConstants.A_DES_QOS))
				lookingForQOSDes = true;
			else if (key.equals(SDPConstants.A_FMTP))
				lookingForFMTP = true;
			else if (SDPConstants.isICECandidateHeaderKey(key))
				lookingForICECandidate = true;
					
			if (boundary != null) {
				// We will need to locate the beginning of the body.
				String ct = "Content-Type: application/sdp";
				int value = -1;
				try {
					value = Integer.parseInt(bodyInstance);
				}
				catch (NumberFormatException nfe) {
					value = -1;
				}
				if (bodyInstance.equals(MsgQueue.FIRST) ||
						value == 1)
					mpOffset = sdp.indexOf(ct);
				else if (bodyInstance.equals(MsgQueue.LAST)) {
					mpOffset = sdp.lastIndexOf(ct);
				}
				else if (value > 1){
					int bi = 0;
					int offset = sdp.indexOf(ct);
					boolean done = false;
					while (offset != -1 && !done) {
						bi++;
						if (bi == value) {
							mpOffset = offset;
							done = true;
						}
						else {
							offset += ct.length();
							offset = sdp.indexOf(ct, offset);
						}
					}
				}

				if (mpOffset > 0) {
					mpOffset = sdp.indexOf("\r\n\r\n", mpOffset);
					mpEnd = sdp.indexOf("--" + boundary, mpOffset);
				}
			}

			try {

				instance = Integer.parseInt(hdrInstance);
				if (instance == 1)  
					offsetStart = sdp.indexOf(key, mpOffset);
				else if (instance > 1 ) {
					boolean done = false;
					offsetStart = mpOffset;
					while (!done) {
						offsetStart = sdp.indexOf(key, offsetStart);
						if (offsetStart != -1 && offsetStart < sdp.length()) {
							// Next get the end of the current line
							int endOfLine = sdp.indexOf("\r\n");
							if (endOfLine != -1 && endOfLine < sdp.length()) {
								// Decrement the instance count if it is the 
								// correct header.
								if (lookingForMode) {							
									if (SDPConstants.isAttributeMode(
											sdp.substring(offsetStart, offsetStart+11))) {
										instance--;
									}
								}
								else if (lookingForMediaModifier) {							
									if (SDPConstants.isMediaModifier(
											sdp.substring(offsetStart, offsetStart+7))) {
										instance--;
									}
								}
								else if (lookingForFMTP) {							
									if (SDPConstants.isFMTP(
											sdp.substring(offsetStart, offsetStart+7))) {
										if (hdr.equals(SDPConstants.FMTP_FAX) && 
												(sdp.charAt(offsetStart+10) == 'T' ||
														sdp.charAt(offsetStart+11) == 'T')) {
														instance--;
										}
										else if (hdr.equals(SDPConstants.FMTP) && sdp.substring(offsetStart, endOfLine).contains("/")) {
											instance--;
										}
									}
								}
								else if (lookingForQOSDes) {							
									if (hdr.equals(SDPConstants.QOS_DESIRED_LOCAL)) {
										if (sdp.substring(offsetStart, endOfLine).contains(" local "))
											instance--;
									}
									else if (hdr.equals(SDPConstants.QOS_DESIRED_REMOTE)) {
										if (sdp.substring(offsetStart, endOfLine).contains(" remote "))
											instance--;
									}
									else if (hdr.equals(SDPConstants.QOS_DESIRED_E2E)) {
										if (sdp.substring(offsetStart, endOfLine).contains(" e2e "))
											instance--;
									}
								}
								else if (lookingForICECandidate) {
									// Candidate attribute are difficult because we need to match on 
									// the candidate-type field and the component-id field.
									// The rules are:
									//	 1. locate the end of the previous line. 
									//	 2. Check the candidate-type, 
									//		if it matches return the line
									//	    else match hdr in remainder of the body starting at endOfLine
									int prevLine = sdp.lastIndexOf("\r\n", offsetStart);
									if (prevLine != -1) {
										// move to the end of the a=candidate: position
										offsetStart = prevLine + 2;
										prevLine += SDPConstants.A_ICE_CANDIDATE.length() + 1;
										// locate the space
										int space = sdp.indexOf(" ", prevLine);
										if (space != -1) {
											space++;
											int endCompId = sdp.indexOf(" ", space);
											if (endCompId != -1) {
												if (SDPConstants.componentIdMatch(hdr, sdp.substring(space, endCompId)))
													instance--;
												else {
													// try to find a second candidate
													offsetStart = endOfLine;
													offsetStart = sdp.indexOf(key, offsetStart);
													if (endOfLine != -1 && endOfLine < sdp.length()) {
														prevLine = sdp.lastIndexOf("\r\n", offsetStart);
														if (prevLine != -1) {
															// move to the end of the a=candidate: position
															offsetStart = prevLine + 2;
															prevLine += SDPConstants.A_ICE_CANDIDATE.length() + 1;
															// locate the space
															space = sdp.indexOf(" ", prevLine);
															if (space != -1) {
																space++;
																endCompId = sdp.indexOf(" ", space);
																if (endCompId != -1) {
																	if (SDPConstants.componentIdMatch(hdr, sdp.substring(space, endCompId)))
																		instance--;
																}
															}
														}
													}
												}
											}
										}
									}
								}
								else  
									instance--;
								// If we have reached zero, we have found the header
								if (instance == 0) {
									hdrLocation[0] = offsetStart;
									hdrLocation[1] = endOfLine; 
									done = true;
								}
								// otherwise move to the start of the next line
								else {
									// This isn't the a=mode line that we are seeking,
									// skip to the next header.
									// Adjust by two to account for CRLF
									offsetStart = endOfLine + 2;
								}

							}
							else {
								done = true;
								offsetStart = -1;
							}
						}
						else {
								done = true;
								offsetStart = -1;
						}
		
					}
				}
			}
			catch (NumberFormatException nfe) {
				// This means we are looking for a different type of header instance
				if (hdrInstance.equals(MsgQueue.FIRST))  {
					// We have to loop because the a=mode line may match some false
					// positives
					boolean done = false;
					offsetStart = mpOffset;
					while (!done && offsetStart < sdp.length()) {
						offsetStart = sdp.indexOf(key,offsetStart);
						if (offsetStart != -1 && offsetStart < sdp.length()) {
							int endOfLine = sdp.indexOf("\r\n", offsetStart);
							if (endOfLine != -1 && endOfLine < sdp.length()) {
								if (lookingForMode) {							
									if (SDPConstants.isAttributeMode(
											sdp.substring(offsetStart, offsetStart+11))) {
										hdrLocation[0] = offsetStart;
										hdrLocation[1] = endOfLine;
										done = true;
									}
									else 
										// reset the offsetStart to next line
										// Adjust for CRLF
										offsetStart = endOfLine + 2;
								}
								else if (lookingForMediaModifier) {							
									if (SDPConstants.isMediaModifier(
											sdp.substring(offsetStart, offsetStart+7))) {
										hdrLocation[0] = offsetStart;
										hdrLocation[1] = endOfLine;
										done = true;
									}
									else 
										// reset the offsetStart to next line
										// Adjust for CRLF
										offsetStart = endOfLine + 2;
								}
								else if (lookingForFMTP) {							
									//String tmp = sdp.substring(offsetStart, offsetStart+7);
									if (SDPConstants.isFMTP(
											sdp.substring(offsetStart, offsetStart+7))) {
										//tmp=sdp.substring(offsetStart, offsetStart+11);
										//String tmp2=sdp.substring(offsetStart, endOfLine);
										if (hdr.equals(SDPConstants.FMTP_FAX) && 
												(sdp.charAt(offsetStart+10) == 'T' ||
														sdp.charAt(offsetStart+11) == 'T')) {
											hdrLocation[0] = offsetStart;
											hdrLocation[1] = endOfLine;
											done = true;
										}
										else if (hdr.equals(SDPConstants.FMTP) && sdp.substring(offsetStart, endOfLine).contains("/")) {
											hdrLocation[0] = offsetStart;
											hdrLocation[1] = endOfLine;
											done = true;
										}
										else 
											// reset the offsetStart to next line
											// Adjust for CRLF
											offsetStart = endOfLine + 2;
									}
									else 
										// reset the offsetStart to next line
										// Adjust for CRLF
										offsetStart = endOfLine + 2;
								}
								else if (lookingForQOSDes) {							
									if (hdr.equals(SDPConstants.QOS_DESIRED_LOCAL)) {
										if (sdp.substring(offsetStart, endOfLine).contains(" local ")) {
											hdrLocation[0] = offsetStart;
											hdrLocation[1] = endOfLine;
											done = true;
										}
										else 
											// reset the offsetStart to next line
											// Adjust for CRLF
											offsetStart = endOfLine + 2;
									}
									else if (hdr.equals(SDPConstants.QOS_DESIRED_REMOTE)) {
										if (sdp.substring(offsetStart, endOfLine).contains(" remote ")) {
											hdrLocation[0] = offsetStart;
											hdrLocation[1] = endOfLine;
											done = true;
										}
										else 
											// reset the offsetStart to next line
											// Adjust for CRLF
											offsetStart = endOfLine + 2;
									}
									else if (hdr.equals(SDPConstants.QOS_DESIRED_E2E)) {
										if (sdp.substring(offsetStart, endOfLine).contains(" e2e ")) {
											hdrLocation[0] = offsetStart;
											hdrLocation[1] = endOfLine;
											done = true;
										}
										else 
											// reset the offsetStart to next line
											// Adjust for CRLF
											offsetStart = endOfLine + 2;
									}
								}
								else if (lookingForICECandidate) {
									// Candidate attribute are difficult because we need to match on 
									// the candidate-type field and the component-id field.
									// The rules are:
									//	 1. locate the end of the previous line. 
									//	 2. Check the candidate-type, 
									//		if it matches return the line
									//	    else match hdr in remainder of the body starting at endOfLine
									int prevLine = sdp.lastIndexOf("\r\n", offsetStart);
									if (prevLine != -1) {
										// move to the end of the a=candidate: position
										offsetStart = prevLine + 2;
										prevLine += SDPConstants.A_ICE_CANDIDATE.length() + 1;
										// locate the space
										int space = sdp.indexOf(" ", prevLine);
										if (space != -1) {
											space++;
											int endCompId = sdp.indexOf(" ", space);
											if (endCompId != -1) {
												if (SDPConstants.componentIdMatch(hdr, sdp.substring(space, endCompId))) {
													hdrLocation[0] = offsetStart;
													hdrLocation[1] = endOfLine;
													done = true;
												}
												else 
													// reset the offsetStart to next line
													// Adjust for CRLF
													offsetStart = endOfLine + 2;
											}
										}
									}
								}
								else {
									hdrLocation[0] = offsetStart;
									hdrLocation[1] = endOfLine;
									done = true;
								}
							}
							else 
								done = true;
						}
						else {
							if (boundary != null) {
								// this allows the add to work in the multipart body
								// The -4 is to move before the CRLFCRLF that ends the body
								hdrLocation[0] = mpEnd-4;
								hdrLocation[1] = mpEnd-4;
								done = true;
							}
							else
							done = true;
						}
					}
				}
				else if (hdrInstance.equals(MsgQueue.LAST)) {
//					if (mpEnd == -1) 
//						offsetStart = sdp.lastIndexOf(key);
//					else 
//						offsetStart = sdp.lastIndexOf(key, mpEnd);
					// We have to loop because the a=mode line may match some false
					// positives
					boolean done = false;
					if (mpEnd == -1)
						offsetStart = sdp.length();
					else 
						offsetStart = mpEnd;
					while (!done && offsetStart >= 0) {
						offsetStart = sdp.lastIndexOf(key,offsetStart);
						if (offsetStart != -1 ) {
							int endOfLine = sdp.indexOf("\r\n", offsetStart);
							if (endOfLine != -1 && endOfLine < sdp.length()) {
								// Decrement the instance count if it is the 
								// correct header.
								if (lookingForMode) {							
									if (SDPConstants.isAttributeMode(
											sdp.substring(offsetStart, offsetStart+11))) {
										hdrLocation[0] = offsetStart;
										hdrLocation[1] = endOfLine;
										done = true;
									}
									else 
										// reset the offsetStart to end of previous line
										// Adjust for CRLF
										offsetStart--;
								}
								else if (lookingForMediaModifier) {							
									if (SDPConstants.isMediaModifier(
											sdp.substring(offsetStart, offsetStart+7))) {
										hdrLocation[0] = offsetStart;
										hdrLocation[1] = endOfLine;
										done = true;
									}
									else 
										// reset the offsetStart to next line
										// Adjust for CRLF
										offsetStart--;
								}
								else if (lookingForFMTP) {							
									if (SDPConstants.isFMTP(
											sdp.substring(offsetStart, offsetStart+7))) {
										if (hdr.equals(SDPConstants.FMTP_FAX) && 
												(sdp.charAt(offsetStart+10) == 'T' ||
														sdp.charAt(offsetStart+11) == 'T')) {
											hdrLocation[0] = offsetStart;
											hdrLocation[1] = endOfLine;
											done = true;
										}
										else if (hdr.equals(SDPConstants.FMTP) && sdp.substring(offsetStart, endOfLine).contains("/")) {
											hdrLocation[0] = offsetStart;
											hdrLocation[1] = endOfLine;
											done = true;
										}
										else 
											// reset the offsetStart to end of previous line
											// Adjust for CRLF
											offsetStart--;
									}
									else 
										// reset the offsetStart to next line
										// Adjust for CRLF
										offsetStart--;
								}
								else if (lookingForQOSDes) {							
									if (hdr.equals(SDPConstants.QOS_DESIRED_LOCAL)) {
										if (sdp.substring(offsetStart, endOfLine).contains(" local ")) {
											hdrLocation[0] = offsetStart;
											hdrLocation[1] = endOfLine;
											done = true;
										}
										else 
											// reset the offsetStart to next line
											// Adjust for CRLF
											offsetStart--;
									}
									else if (hdr.equals(SDPConstants.QOS_DESIRED_REMOTE)) {
										if (sdp.substring(offsetStart, endOfLine).contains(" remote ")) {
											hdrLocation[0] = offsetStart;
											hdrLocation[1] = endOfLine;
											done = true;
										}
										else 
											// reset the offsetStart to next line
											// Adjust for CRLF
											offsetStart--;
									}
									else if (hdr.equals(SDPConstants.QOS_DESIRED_E2E)) {
										if (sdp.substring(offsetStart, endOfLine).contains(" e2e ")) {
											hdrLocation[0] = offsetStart;
											hdrLocation[1] = endOfLine;
											done = true;
										}
										else 
											// reset the offsetStart to next line
											// Adjust for CRLF
											offsetStart--;
									}
									
									else if (lookingForICECandidate) {
										// Candidate attribute are difficult because we need to match on 
										// the candidate-type field and the component-id field.
										// The rules are:
										//	 1. locate the end of the previous line. 
										//	 2. Check the candidate-type, 
										//		if it matches return the line
										//	    else match hdr in remainder of the body starting at endOfLine
										int prevLine = sdp.lastIndexOf("\r\n", offsetStart);
										if (prevLine != -1) {
											// move to the end of the a=candidate: position
											offsetStart = prevLine + 2;
											prevLine += SDPConstants.A_ICE_CANDIDATE.length() + 1;
											// locate the space
											int space = sdp.indexOf(" ", prevLine);
											if (space != -1) {
												space++;
												int endCompId = sdp.indexOf(" ", space);
												if (endCompId != -1) {
													if (SDPConstants.componentIdMatch(hdr, sdp.substring(space, endCompId))) {
														hdrLocation[0] = offsetStart;
														hdrLocation[1] = endOfLine;
														done = true;
													}
													else 
														// reset the offsetStart to next line
														// Adjust for CRLF
														offsetStart--;
												}
											}
										}
									}
								}
								else {
									hdrLocation[0] = offsetStart;
									hdrLocation[1] = endOfLine;
									done = true;
								}
							}
							else 
								done = true;
						}
						else {
							if (boundary != null) {
								// this allows the add to work in the multipart body
								// The -4 is to move before the CRLFCRLF that ends the body
								hdrLocation[0] = mpEnd-4;
								hdrLocation[1] = mpEnd-4;
								done = true;
							}
							else 
								done = true;
							
						}
					}
				}
			}
		}
		return hdrLocation;

 	}
	/**
	 * This method identifies the starting and ending positions within the header of the
	 * specified parameter.
	 * 
	 * @param hdr - The header to find
	 * @param param - The parameter to locate.
	 * @param hdrLocation - the starting and ending offset of the header within the message.
	 * @param msg - The message to search for the header.
	 * 
	 * @return - an integer array of two containing the starting index in the 
	 * 		first integer and the ending index in the second or -1 in both.
	 */
	public synchronized int [] locateSDPParam(String hdr, String param, int [] hdrLocation, String msg) {
		int [] paramLocation = new int [2];
		int start = -1;
		int end = -1;
		paramLocation[0] = start;
		paramLocation[1] = end;
		// This method uses the following rules to locate the end of a parameter
		// First it attempts to locate the next SPACE, if it can't find one before the
		// end of the line, it uses the CRLF as the end of the field. 
		String key = SDPConstants.getKey(hdr);
		if (key != null) {
			start = hdrLocation[0] + key.length();
			// Format of origin is the following
			// v=number
			if (SDPConstants.VERSION.equals(hdr)) {
				if (param.equals(SDPConstants.NUMBER)) {
					// This allows for a space between = and number parameter
					if (msg.charAt(start) == ' ')
						start++;
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
			else if (SDPConstants.ORIGIN.equals(hdr)) {
				// Format of origin is the following
				// o=user session_id session_version net_type address_type address

				String delimiter = " ";
				int instance = 0;
				// This allows for a space between = and user parameter
				if (msg.charAt(start) == ' ')
					start++;

				if (param.equals(SDPConstants.USER)) {
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
				// Since it isn't the first element, simply assign the
				// delimiter and the instance value and let a common
				// method locate the start position
				else if (param.equals(SDPConstants.SESSION_ID)) 
					instance = 1;
				else if (param.equals(SDPConstants.SESSION_VERSION)) 
					instance = 2;
				else if (param.equals(SDPConstants.NET_TYPE)) 
					instance = 3;
				else if (param.equals(SDPConstants.ADDRESS_TYPE)) 
					instance = 4;
				else if (param.equals(SDPConstants.ADDRESS)) 
					instance = 5;;

					if (instance > 0) {
						start = getPosition(msg, start, delimiter, instance);
						if (start != -1)
							end = getEndOfSDPParam(msg, start, hdrLocation[1]);
					}
			}
			else if (SDPConstants.SESSION.equals(hdr)) {
				// Format of session is the following
				// s=name 
				if (param.equals(SDPConstants.NAME)) {
					// This allows for a space between = and name parameter
					if (msg.charAt(start) == ' ')
						start++;
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
			else if (SDPConstants.CONNECTION_TYPE.equals(hdr)) {
				// Format of connection type is the following
				// c=net-type address-type address
				// This allows for a space between = and name parameter
				if (msg.charAt(start) == ' ')
					start++;
				if (param.equals(SDPConstants.NET_TYPE)) {
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
				else if (param.equals(SDPConstants.ADDRESS_TYPE)) {
					start = getPosition(msg, start, " ", 1);
					if (start != -1)
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
				else if (param.equals(SDPConstants.ADDRESS)) {
					start = getPosition(msg, start, " ", 2);
					if (start != -1)
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
			
			else if (SDPConstants.TIAS_MODIFIER.equals(hdr)) {
				// Format of bandwidth's tias modifier is the following
				// b=TIAS:value
				// This allows for a space between : and value parameter
				if (msg.charAt(start) == ' ')
					start++;
				if (param.equals(SDPConstants.VALUE)) 
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.MAX_PACKET_RATE.equals(hdr)) {
				// Format of bandwidth's max prate modifier is the following
				// b=maxprate:value
				// This allows for a space between : and value parameter
				if (msg.charAt(start) == ' ')
					start++;
				if (param.equals(SDPConstants.VALUE)) 
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.MEDIA_MODIFIER.equals(hdr)) {
				// Format of bandwidth's media modifier is the following
				// b= AS: | CT: value
				// Because the media modifier can have to values AS or CT
				// we need to confirm that this is the leading elements and
				// and we adjust the offset for them.
				start = hdrLocation[0] + key.length();
				String media = msg.substring(start,start+3);
				if (media.equals("AS:") || media.equals("CT:")) {
					start += media.length();
					// This allows for a space between : and value parameter
					if (msg.charAt(start) == ' ')
						start++;
					if (param.equals(SDPConstants.VALUE)) 
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
			else if (SDPConstants.TIME.equals(hdr)){
				// Format of time is the following
				// t=start_time stop_time
				// This allows for a space between = and start_time parameter
				if (msg.charAt(start) == ' ')
					start++;
				if (param.equals(SDPConstants.START)) 
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				else if (param.equals(SDPConstants.STOP)) {
					start = getPosition(msg, start, " ", 1);
					if (start != -1)
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
			else if (SDPConstants.AUDIO.equals(hdr) || 
					SDPConstants.VIDEO.equals(hdr) || 
					SDPConstants.IMAGE.equals(hdr)) {
				// Format of media of audio type is the following
				// m=audio port protocol payload-type"\r\n
				// NOTE: No offset is needed after audio
				// Format of media of video type is the following
				// m=video port protocol payload-type"\r\n
				// NOTE: No offset is needed after video
				if (param.equals(SDPConstants.PORT)) 
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				else if (param.equals(SDPConstants.PROTOCOL)) {
					start = getPosition(msg, start, " ", 1);
					if (start != -1)
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
				else if (param.equals(SDPConstants.PAYLOAD_TYPE)) {
					start = getPosition(msg, start, " ", 2);
					if (start != -1)
						// Get all of the values up to the CRLF
						end = msg.indexOf("\r\n", start);
				}
			}
			else if (SDPConstants.RTPMAP.equals(hdr)) {
				// Format of attribute's rtpmap is the following
				// a=rtpmap: payload-type codec-name/clock-rate
				// This allows for a space between : and payload-type parameter
				if (msg.charAt(start) == ' ')
					start++;

				if (param.equals(SDPConstants.PAYLOAD_TYPE)) 
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				else if (param.equals(SDPConstants.CODEC_NAME)) {
					// Skip the first space and move the next one
					start = getPosition(msg, start, " ", 1);
					if (start != -1) {
						end = msg.indexOf("/", start);
					}
				}
				else if (param.equals(SDPConstants.CLOCKRATE)) {
					start = getPosition(msg, start, "/", 1);
					if (start != -1)
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
			else if (SDPConstants.PTIME.equals(hdr)) {
				// Format of attribute's ptime is the following
				// a=ptime:value
				// This allows for a space between : and value parameter
				if (msg.charAt(start) == ' ')
					start++;
				if (param.equals(SDPConstants.VALUE)) 
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.MODE.equals(hdr)) {
				// Format of attribute's ptime is the following
				// a=value
				// This allows for a space between = and value parameter
				if (msg.charAt(start) == ' ')
					start++;
				if (param.equals(SDPConstants.VALUE)) 
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.FAX_VERSION.equals(hdr) ||
					SDPConstants.FAX_MAX_DATAGRAM.equals(hdr) ||
					SDPConstants.FAX_RATE_MGMT.equals(hdr) ||
					SDPConstants.FAX_UDP_EC.equals(hdr) ) {
				// Format of the fax attributes is the following
				// a=T38FaxVersion:value
				// a=T38FaxMaxDatagram:value
				// a=T38FaxRateManagement:value
				// a=T38FaxUdpEC:value
				if (param.equals(SDPConstants.VALUE)) {
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
					if (end == -1) {
						end = msg.indexOf(";", start);
					}
				}
			}
			else if (SDPConstants.FMTP.equals(hdr)) {
				// Format of the fmtp attributes is the following
				// a=fmtp:101 0/0
				//
				// where 101 is a dynamic codec value ranging from 96-127
				// Because of the similarities between the two lines we have to isolate to the correct line
				if (msg.charAt(start) == ' ')
					start++;

				if (param.equals(SDPConstants.PAYLOAD_TYPE)) 
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				else if (param.equals(SDPConstants.CODEC_NAME)) {
					// Skip the first space and move the next one
					start = getPosition(msg, start, " ", 1);
					if (start != -1) {
						end = msg.indexOf("/", start);
					}
				}
				else if (param.equals(SDPConstants.CLOCKRATE)) {
					start = getPosition(msg, start, "/", 1);
					if (start != -1)
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
			else if (SDPConstants.FMTP_FAX.equals(hdr)) {
				// Format of the fmtp-fax attributes is the following
				// a=fmtp:101 T38FaxRateManagement=transferedTCF; T38FaxVersion=0;T38FaxMaxDatagram=173
				// where 101 is a dynamic codec value ranging from 96-127
				// Because of the similarities between the two lines we have to isolate to the correct line
				if (msg.charAt(start) == ' ')
					start++;

				if (param.equals(SDPConstants.PAYLOAD_TYPE)) 
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				else if (param.equals(SDPConstants.PARAMS)) {
					// Skip the first space and move the next one
					start = getPosition(msg, start, " ", 1);
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
//					if (end == -1) {
//						end = msg.indexOf(";", start);
//					}
				}
			}
			else if (SDPConstants.QOS_CURRENT_REMOTE.equals(hdr)) {
				// Format of attribute's current qos is the following
				// a=curr:qos remote value
				// NOTE: No offset is needed after remote
				if (param.equals(SDPConstants.DIRECTION))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.QOS_CURRENT_LOCAL.equals(hdr)) {
				// Format of attribute's current qos is the following
				// a=curr:qos local value
				// NOTE: No offset is needed after local
				if (param.equals(SDPConstants.DIRECTION))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.QOS_CURRENT_E2E.equals(hdr)) {
				// Format of attribute's current qos is the following
				// a=curr:qos e2e value
				// NOTE: No offset is needed after e2e
				if (param.equals(SDPConstants.DIRECTION))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.QOS_DESIRED_REMOTE.equals(hdr)) {
				// Format of attribute's desire qos is the following
				// a=des:qos strength remote direction
				// NOTE: No offset is needed after qos
				if (param.equals(SDPConstants.STRENGTH))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				else if (param.equals(SDPConstants.DIRECTION)) {
					start = getPosition(msg, start, " ", 2);
					if (start != -1)
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
			else if (SDPConstants.QOS_DESIRED_LOCAL.equals(hdr)) {
				// Format of attribute's desire qos is the following
				// a=des:qos  strength local direction
				// NOTE: No offset is needed after qos
				if (param.equals(SDPConstants.STRENGTH))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				else if (param.equals(SDPConstants.DIRECTION)) {
					start = getPosition(msg, start, " ", 2);
					if (start != -1)
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
			else if (SDPConstants.QOS_DESIRED_E2E.equals(hdr)) {
				// Format of attribute's desire qos is the following
				// a=des:qos strength e2e direction
				// NOTE: No offset is needed after qos
				if (param.equals(SDPConstants.STRENGTH))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				else if (param.equals(SDPConstants.DIRECTION)) {
					start = getPosition(msg, start, " ", 2);
					if (start != -1)
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
			else if (SDPConstants.QOS_CONF_REMOTE.equals(hdr)) {
				// Format of attribute's confirm qos is the following
				// a=conf:qos remote value
				// NOTE: No offset is needed after remote
				if (param.equals(SDPConstants.DIRECTION))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.QOS_CONF_LOCAL.equals(hdr)) {
				// Format of attribute's confirm qos is the following
				// a=conf:qos local value
				// NOTE: No offset is needed after local
				if (param.equals(SDPConstants.DIRECTION))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.QOS_CONF_E2E.equals(hdr)) {
				// Format of attribute's confirm qos is the following
				// a=conf:qos e2e value
				// NOTE: No offset is needed after e2e
				if (param.equals(SDPConstants.DIRECTION))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.ICE_PWD.equals(hdr)) {
				// Format of attribute is:
				// a=ice-pwd:asd88fgpdd777uzjYhagZg
				if (param.equals(SDPConstants.VALUE))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.ICE_UFRAG.equals(hdr)) {
				// Format of attribute is:
				// a=ice-ufrag:8hhY
				if (param.equals(SDPConstants.VALUE))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			// ICE_LITE and ICE_MISMATCH have no parameters, so we can skip these
			else if (SDPConstants.ICE_OPTIONS.equals(hdr)) {
				// Format of attribute is:
				// a=ice-options:xxxxx
				if (param.equals(SDPConstants.VALUE))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
			}
			else if (SDPConstants.isICECandidateHeader(hdr)) {
				// Format of attribute is:
				// a=candidate:2 1 UDP 1694498815 192.0.2.3 45664 typ srflx raddr 10.0.1.1 rport 8998
				// The code below assumes that the correct attribute line was obtained by the locateHeader operation.
				String delimiter = " ";
				int instance = 0;
				if (param.equals(SDPConstants.FOUNDATION)) {
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
				// Since it isn't the first element, simply assign the
				// delimiter and the instance value and let a common
				// method locate the start position
				else if (param.equals(SDPConstants.COMPONENT_ID)) 
					instance = 1;
				else if (param.equals(SDPConstants.TRANSPORT)) 
					instance = 2;
				else if (param.equals(SDPConstants.PRIORITY)) 
					instance = 3;
				else if (param.equals(SDPConstants.CONNECTION_ADDRESS)) 
					instance = 4;
				else if (param.equals(SDPConstants.PORT)) 
					instance = 5;
				else if (param.equals(SDPConstants.REL_ADDR)) 
					instance = 9;
				else if (param.equals(SDPConstants.REL_PORT)) 
					instance = 11;

					if (instance > 0) {
						start = getPosition(msg, start, delimiter, instance);
						if (start != -1)
							end = getEndOfSDPParam(msg, start, hdrLocation[1]);
					}
			}
			else if (SDPConstants.ICE_REMOTE_1.equals(hdr) ||
					SDPConstants.ICE_REMOTE_2.equals(hdr)) {
				// Format of attribute is:
				// a= remote-candidate:1 192.0.2.180 45664
				String delimiter = " ";
				int instance = 0;
				if (param.equals(SDPConstants.COMPONENT_ID))
					end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				else if (param.equals(SDPConstants.CONNECTION_ADDRESS)) 
					instance = 1;
				else if (param.equals(SDPConstants.PORT)) 
					instance = 2;
				
				if (instance > 0) {
					start = getPosition(msg, start, delimiter, instance);
					if (start != -1)
						end = getEndOfSDPParam(msg, start, hdrLocation[1]);
				}
			}
		}
		if (end != -1 && end <= hdrLocation[1]) {
			paramLocation[0] = start;
			paramLocation[1] = end;
		}
		else {
			logger.warn(PC2LogCategory.SIP, subCat, 
					"SDPLocator could not locate SDP parameter(" + param + 
					") from hdr(" + hdr + ").");
		}
		
		return paramLocation;
	}
	
	/**
	 * This method retrieves the ending position of a parameter in a given header.
	 * 
	 * @param msg - the SDP message body to search in
	 * @param start - the starting location to begin the search
	 * @return - integer of the ending position or -1
	 */
	private int getEndOfSDPParam(String msg, int start, int hdrEnd) {
		int offset = start;
		int end = -1;
		while (offset <= hdrEnd && end == -1) {
			char c = msg.charAt(offset);
			if (c == ' ' || c == '\r' || c == '\n') 
				end = offset;
			else
				offset++;
		}
		// Since the \r\n has probably already been stripped 
		// assume it is the last field in the header.
//		if (end == -1){
//			end = offset;
//		}
		return end;
	}
	
	
	private int getPosition(String msg, int start, String delimiter, int instance) {
		int offset = start;
		int result = -1;
		if (instance > 0)  {
			while (offset < msg.length() && result == -1) {
				int index = msg.indexOf(delimiter, offset);
				if (index != -1 && index < msg.length()) {
					instance--;
					if (instance == 0) 
						result = index + delimiter.length();
					else
						offset = index + delimiter.length();

				}
			}
		}
		return result;
	}
	
}
