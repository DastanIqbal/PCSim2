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

import java.util.LinkedList;

import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.SIPConstants;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

public class SIPLocator {

	/**
	 * Private logger for the class
	 */
	private LogAPI logger = LogAPI.getInstance(); // Logger.getLogger("Locators");

	private static SIPLocator locator = null;

	public static final String REQUEST_LINE = "Request-Line";
	public static final String STATUS_LINE = "Status-Line";
	
	/**
	 * Used to specify the type of delimiter at the end of a SIP header
	 * for modification processing on a SIP message. This is the default.
	 */
	public static final int NO_COMMA = -1;

	/**
	 * Used to specify the type of delimiter at the end of a SIP header
	 * for modification processing on a SIP message. This one specifies
	 * the header is the first entry in a list of comma delimited headers.
	 */
	public static final int COMMA_DELIMITED_FRONT = 0;

	/**
	 * Used to specify the type of delimiter at the end of a SIP header
	 * for modification processing on a SIP message. This one specifies
	 * the header is a middle entry in a list of comma delimited headers.
	 */
	public static final int COMMA_DELIMITED_MIDDLE = 1;

	/**
	 * Used to specify the type of delimiter at the end of a SIP header
	 * for modification processing on a SIP message. This one specifies
	 * the header is the last entry in a list of comma delimited headers.
	 */
	public static final int COMMA_DELIMITED_END = 2;

	/**
	 * Used to specify the type of delimiter at the end of a SIP header
	 * for modification processing on a SIP message.This one specifies
	 * the header is the single entry in a CRLF delimited header.
	 */
	public static final int CRLF = 3;

	/**
	 * Used to specify the type of delimiter at the end of a SIP parameter
	 * for modification processing on a SIP message. This type of parameter
	 * is in the form (parameterName=value).
	 */
	public static final int GENERIC_PARAM = 0;

	/**
	 * Used to specify the type of delimiter at the end of a SIP parameter
	 * for modification processing on a SIP message. This type of parameter
	 * is position relative to the beginning of the header and is usually
	 * a mandatory field.
	 */
	public static final int VALUE_PARAM = 1;

	/**
	 * Used to specify the type of delimiter at the end of a SIP parameter
	 * for modification processing on a SIP message. This type of parameter
	 * is actually concatenated headers separated with commas that are
	 * being accessed by a param reference in the mod. 
	 * 
	 * Eg. Require: 100rel,precondition,gruu
	 * 
	 */
	public static final int VALUE_COMMA_PARAM = 2;

	public static final String ADDR_SPEC = "addr-spec";
	
	public static final String NAME_ADDR = "name-addr";
	
	public static final int PARENT = 1;
	public static final int GRANDPARENT = 2;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Locator";
	
	/**
	 * Private Constructor
	 *
	 */
	private SIPLocator() {

	}
	
	private MultipartLocation isMultipart(String msg, String subType) {
		MultipartLocation mpl = null;
		
		int mpOffset = 0;
		int mpEnd = 0;
		String boundary = null;
		// First see if the body is a multipart body or not 
		if (msg.startsWith("--")) {
			int crlf = msg.indexOf("\r\n");
			boundary = "--" + msg.substring(2,crlf);
			// String body = new String (msg);
			if (crlf != -1) {
				boolean found = false;
				mpOffset += boundary.length()+2;
				while (mpOffset < msg.length() &&
						!found) {
					mpEnd = msg.indexOf(boundary, mpOffset);
					//String tmp2 = msg.substring(mpOffset);
					if (mpEnd == -1) {
						mpEnd = msg.length();
						mpOffset = msg.length();
					}
					else if ( msg.substring(mpOffset,mpEnd).contains(subType)) {
						found = true;
						mpl = new MultipartLocation(mpOffset, mpEnd, boundary);
					}
					else {
						mpOffset = mpEnd + boundary.length();
						//msg.indexOf(boundary, mpEnd);
					}

				}
			}
		}
		
		return mpl;
	}

	/**
	 * Creates and initializes the parameter location table
	 * for a header that contains only generic parameters.
	 * 
	 * @return
	 */
	private int [] initGenericLocation() {
		int [] paramLocation = new int [4];
		paramLocation[0] = -1;
		paramLocation[1] = -1;
		paramLocation[2] = -1;
		paramLocation[3] = GENERIC_PARAM;
		return paramLocation;
	}
	
	/**
	 * Creates and initializes the parameter location table
	 * for a header that contains at least one fixed positional
	 * parameter.
	 * 
	 * @return
	 */
	private int [] initValueLocation() {
		int [] paramLocation = new int [4];
		paramLocation[0] = -1;
		paramLocation[1] = -1;
		paramLocation[2] = -1;
		paramLocation[3] = VALUE_PARAM;
		return paramLocation;
	}
	
	/**
	 * Retrieves the single instance of the SIPLocator if it 
	 * already exists. If it doesn't exist it will create it prior
	 * to returning it.
	 *
	 */
	public synchronized static SIPLocator getInstance() {
		if (locator == null) {
			locator = new SIPLocator();
		}
		return locator;
	}

	/**
	 * This method locates the start and end of a header within the msg.
	 * 
	 * @param subType - The media subtype of the body being sought by the
	 * 		invoker.
	 * @param hdr - The header to find
	 * @param hdrInstance - the instance of the header to find.
	 * @param param - The parameter being sought if any.
	 * @param value - Whether the value field of the device is sought or not.
	 * @param msg - The message to search for the header.
	 * @param ignoreMIA - this is a flag telling the system not to log that it couldn't 
	 * 		find the value sought by the invoker.
	 * 
	 * @return location [] - entry 0 is the starting offset location of the header, entry 1
	 * is the ending offset location of the header, and entry 2 specifies whether the comma 
	 * delimited or not.
	 * 
	 */
	public synchronized int [] locateSIPBody(String subType, String hdr, 
			String hdrInstance, String param, boolean value, String msg,
			boolean ignoreMIA) {
		int [] location = new int [3];
		location[0] = -1;
		location[1] = -1;
		location[2] = NO_COMMA;
		int instance = -1;
//		int newlineLen = 2;
		int offset = 0;
		int startAdjust = 0;
		int endAdjust = 0;
		// Ending delimiter assumes the type is xml based
		String endDelimiter = null;
		boolean xmlType = false;
		int mpOffset = 0;
		int mpEnd = 0;
//		boolean sipHdr = SIPConstants.canAppearInBody(hdr);
//		String boundary = null;
		String delimiter = hdr;
		// First see if the body is a multipart body or not 
		MultipartLocation mpl = isMultipart(msg, subType);
		if (mpl != null) {
			mpOffset = mpl.startLocation;
			mpEnd = mpl.endLocation;
			endDelimiter = mpl.boundaryTag;
		}
		
		if (subType.contains("xml") && delimiter != null) {
				xmlType = true;
				endDelimiter = "</" + delimiter + ">";
				startAdjust = 0;
				endAdjust = endDelimiter.length();
				delimiter = "<" + delimiter + ">";
				if (mpl != null &&
						(hdr.equals("Content-Type") ||
								hdr.equals("Content-ID") || 
								hdr.equals("Content-Length"))) {
					// We need the SIP header's not the xml body
					endDelimiter = "\r\n";
					delimiter = hdr;
					endAdjust = endDelimiter.length();
				}
								
		}
		else if (subType.equals("simple-message-summary")) {
			return locateSimpleMessageBody(subType, delimiter, 
			hdrInstance, param, value,  msg,
			ignoreMIA);
		}
		else if (subType.equals("text") ||
				subType.equals("sipfrag")) {
			location[0] = 0;
			if (msg.endsWith("\r\n"))
				location[1] = msg.length()-2;
			else 
				location[1] = msg.length();
			return location;
		}
		
		offset = mpOffset;
		
		
		if (delimiter != null && endDelimiter != null) {
			try {
				//Dead code
//				if (sipHdr && boundary != null) {
//					// We have the offset for the body, now move it to the hdr
//					offset = msg.indexOf(delimiter, offset);
//					// For xmlType we assume the starting element will end with the
//					// '>' marker. However if there is no match we should try for 
//					// '<tag ' as the tag may have attributes. 
//					if (offset == -1 && xmlType)
//						offset = msg.indexOf((delimiter.substring(0, delimiter.length()-1) + " "), 0);
//					if (offset != -1) {
//
//						int offsetEnd = msg.indexOf("\r\n", offset);
//						if (offsetEnd != -1) {
//							location[0] = offset;
//							location[1] = offsetEnd;
//							location[2] = CRLF;
//							if (param != null) {
//								int [] paramLocation = locateSIPParameter(delimiter, param, location, msg);
//								if (paramLocation[0] != -1 &&
//										paramLocation[1] != -1 &&
//										paramLocation[2] != -1 &&
//										paramLocation[3] != -1) {
//									location[0] = paramLocation[1];
//									location[1] = paramLocation[2];
//									location[2] = paramLocation[3];
//									return location;
//								}
//
//							}
//							else 
//								return location;
//						}
//					}
//					return location;
//				}
				instance = Integer.parseInt(hdrInstance);
//				boolean front = true;
				while (instance > 0) {
					int pos = offset;
					offset = msg.indexOf(delimiter, offset);
					// For xmlType we assume the starting element will end with the
					// '>' marker. However if there is no match we should try for 
					// '<tag ' as the tag may have attributes. 
					if (offset == -1 && xmlType) {
						String alternative = delimiter.substring(0, delimiter.length()-1);
						offset = msg.indexOf((alternative + " "), pos);
						if (offset == -1) {
							offset = msg.indexOf((alternative + "\r"), pos);
							if (offset == -1) {
								offset = msg.indexOf((alternative + "\n"), pos);
							}
						}
					}
					
					if (offset != -1) {
						int offsetEnd = msg.indexOf(endDelimiter, offset);
						if (offsetEnd != -1) {
							instance--;
							if (instance == 0) {
								// Adjust the offsets to include any
								// special character wanted by the subtype
								location[0] = offset + startAdjust;
								location[1] = offsetEnd + endAdjust;
								location[2] = CRLF;
							}
							else {
								offset = offsetEnd + endDelimiter.length();
							}
						}
						else {
							if (!ignoreMIA)
								logger.error(PC2LogCategory.SIP, subCat,
										"SIPLocator was unable to locate header instance number[" +
										hdrInstance + "] in the message.");
							instance = -1;
						}
					}
					else {
						if (!ignoreMIA)
							logger.error(PC2LogCategory.SIP, subCat,
									"SIPLocator was unable to locate header instance number[" +
									hdrInstance + "] in the message.");
						instance = -1;
					}
				}
			}
			catch (NumberFormatException nfe) {
				offset = msg.indexOf(delimiter, offset);
				// For xmlType we assume the starting element will end with the
				// '>' marker. However if there is no match we should try for 
				// '<tag ' as the tag may have attributes. 
				if (offset == -1 && xmlType) {
					offset = msg.indexOf((delimiter.substring(0, delimiter.length()-1) + " "), 0);
					if (offset == -1) {
						String alternative = delimiter.substring(0, delimiter.length()-1);
						offset = msg.indexOf((alternative + " "), 0);
						if (offset == -1) {
							offset = msg.indexOf((alternative + "\r"), 0);
							if (offset == -1) {
								offset = msg.indexOf((alternative + "\n"), 0);
							}
						}
					}
				}
				
				if (offset != -1) {
					int offsetEnd = -1;
					if (hdrInstance.equals(MsgQueue.FIRST)) 
						offsetEnd = msg.indexOf(endDelimiter, offset);
					else if (hdrInstance.equals(MsgQueue.LAST)) 
						offsetEnd = msg.lastIndexOf(endDelimiter, offset);
					if (offsetEnd != -1) {
						// Adjust the offsets to include any
						// special character wanted by the subtype
						location[0] = offset + startAdjust;
						location[1] = offsetEnd + endAdjust;
						location[2] = CRLF;

					}
					else {
						// See if we can locate an starting and stopping 
						endDelimiter = "/>";
						endAdjust = endDelimiter.length();
						if (hdrInstance.equals(MsgQueue.FIRST)) 
							offsetEnd = msg.indexOf(endDelimiter, offset);
						else if (hdrInstance.equals(MsgQueue.LAST)) 
							offsetEnd = msg.lastIndexOf(endDelimiter, offset);
						if (offsetEnd != -1) {
							// Adjust the offsets to include any
							// special character wanted by the subtype
							location[0] = offset + startAdjust;
							location[1] = offsetEnd + endAdjust;
							location[2] = CRLF;

						}
					}
				}

			}
		}
		// return whole body
		else if (endDelimiter != null){
			int offsetEnd = msg.lastIndexOf(endDelimiter, mpEnd);
			if (offsetEnd != -1) {
				// Adjust the offsets to include any
				// special character wanted by the subtype
				location[0] = offset + startAdjust;
				location[1] = offsetEnd + endAdjust;
				location[2] = CRLF;

			}
		}

		if (xmlType && 
				location[0] != -1 && 
				location[1] != -1) {
			if (param != null) {
				String startDelimiter = param+"=\"";
				endDelimiter = "\"";
				offset = msg.indexOf(startDelimiter, location[0]);
				if (offset != -1) {
					// Adjust for the length of the tag
					offset += startDelimiter.length();
					int offsetQuote = msg.indexOf(endDelimiter, offset );
					if (offsetQuote != -1 && offsetQuote < location[1]) {
						location[0] = offset;
						location[1] = offsetQuote;
						location[2] = CRLF;
					}
				}
				// This means that the attribute doesn't exist in the
				// tag. Go to the first '>' as the location
				else if (!value) {
					offset = msg.indexOf(">", location[0]);
					if (offset != -1) {
						if (msg.charAt(offset-1) == '/') 
							// Adjust if the preceding character is a 
							// '/'
							offset--;
						location[0] = offset;
						location[1] = offset;
						location[2] = CRLF;
					}
				}
			}
			else if (value) {
				location[0] = offset + delimiter.length();
				location[1] = location[1] - endDelimiter.length();
				location[2] = CRLF;
			}
		}
		return location;
	}

	/**
	 * This method locates the start and end of a header within the msg.
	 * 
	 * @param hdr - The header to find
	 * @param hdrInstance - the instance of the header to find.
	 * @param msg - The message to search for the header.
	 * @param includeHdr - This flag states whether to include the header in the 
	 * 		location information returned by the operation.
	 * @param ignoreMIA - this is a flag telling the system not to log that it couldn't 
	 * 		find the value sought by the invoker.
	 * 
	 * @return location [] - entry 0 is the starting offset location of the header, entry 1
	 * is the ending offset location of the header, and entry 2 specifies whether the comma 
	 * delimited or not.
	 * 
	 */
	public synchronized int [] locateSIPHeader(String hdr, String hdrInstance, 
			String msg, boolean includeHdr, boolean ignoreMIA) {
		int [] location = new int [3];
		location[0] = -1;
		location[1] = -1;
		location[2] = NO_COMMA;
		int instance = -1;
		int newlineLen = 2;
		int offset = 0;
		if (hdr != null) {
			String header = "\r\n"+hdr+": ";
			if (hdr.equals(REQUEST_LINE) || hdr.equals(STATUS_LINE)) {
				// In some tests (e.g. SIP-UE 1.7.3.2), there might
				// be a \r\n before the first line if so move past it
				if (msg.startsWith("\r\n"))
					offset= 2;
				// This means the first line
				int offsetEnd = msg.indexOf("\r\n", offset);
				if (offsetEnd > 0  && offsetEnd < msg.length()) {
					location[0] = offset;
					location[1] = offsetEnd;
					location[2] = CRLF;
				}

			}
			else {
				// Locate the beginning of the body since we don't need to test this
				// part of the message
				int body = msg.indexOf("\r\n\r\n");
				if (body == -1) {
					body = msg.length();
				}
				else if (hdr.equals("SDP")) {
					// Adjust for CRLF CRLF
					body += 4;
					location[0] = body;
					location[1] = msg.length();
					location[2] = CRLF;
				}
				try {
					instance = Integer.parseInt(hdrInstance);
					boolean front = true;
					while (instance > 0) {
						offset = msg.indexOf(header, offset);
						if (offset != -1) {
							
							if (includeHdr)
								// Adjust the offset past the CRLF
								offset += 2;
							else 
								// Adjust the offset past the CRLF, the header, the colon and the space
								offset += header.length();
							int offsetEnd = msg.indexOf("\r\n", offset);
							int offsetComma = msg.indexOf(",", offset);
							if (offsetEnd != -1) {
								// We need to see if there is a comma before
								// the new line since multiple headers can appear
								// on the same line
								if (offsetComma != -1 && offsetComma < offsetEnd) {
									// Since it is comma delimited remove the header name from the list
// THE HEADER HAS ALREADY BEEN REMOVED ABOVE
//									if (includeHdr)
//										// Adjust the offset past the ':'
//										offset += 1;
//									else 
//										offset += hdr.length() + 1; // include the ':'
									if (msg.charAt(offset) == ' ')
										offset++; // adjust for the space.
									// We found a comma so there should be two or more instances
									// of the header on this single line.
									do {
										instance--;
										if (instance == 0) {
											location[0] = offset;
											location[1] = offsetComma;
											if (front)
												location[2] = COMMA_DELIMITED_FRONT;
											else 
												location[2] = COMMA_DELIMITED_MIDDLE;
											break;
										}
										else {
											offset = offsetComma + 1;
											offsetComma = msg.indexOf(",", offset);
											front = false;
										}
									}	while (offsetComma != -1 && offsetComma < offsetEnd);
									if (instance == 1) {
										if (offsetComma > offsetEnd || offsetComma == -1) {
											instance--;
											location[0] = offset;
											location[1] = offsetEnd;
											location[2] = COMMA_DELIMITED_END;
										}
									}
									else if (instance > 1) {
										// The header does't exist
										instance = 0;
									}
								}
								else {
									instance--;
									if (instance == 0) {
										location[0] = offset;
										location[1] = offsetEnd;
										location[2] = CRLF;
									}
									else {
										offset = offsetEnd + newlineLen;
									}
								}
							}
							else {
								offset = offsetEnd + newlineLen;
							}
						}
						else {
							if (!ignoreMIA)
								logger.error(PC2LogCategory.SIP, subCat,
									"SIPLocator was unable to locate instance number[" +
									hdrInstance + "] of header(" + hdr+ ") in the message.");
							instance = -1;
						}
					}
				}
				catch (NumberFormatException nfe) {
					if (hdrInstance.equals(MsgQueue.FIRST)) {
						// Locate the first instance of the header.
						offset = msg.indexOf(header, offset);
						if (offset != -1) {
							if (includeHdr)
								// Adjust the offset past the CRLF
								offset += 2;
							else 
								// Adjust the offset past the CRLF, the header, the colon and the space
								offset += header.length();
							
							// Next determine whether the CRLF occurs before
							// a comma,
							int offsetEnd = msg.indexOf("\r\n", offset);
							int offsetComma = -1;
							if (SIPConstants.multipleHeadersAllowed(hdr))
								offsetComma = msg.indexOf(",", offset);
							if (offsetEnd != -1) {
								if (offsetComma != -1 &&
										offsetComma < offsetEnd) {
									location[0] = offset;
									location[1] = offsetComma;
									location[2] = COMMA_DELIMITED_FRONT;
								}
								else {
									location[0] = offset;
									location[1] = offsetEnd;
									location[2] = CRLF;
								}
							}
						}
					}
					else if (hdrInstance.equals(MsgQueue.LAST)) {
						// Locate the last instance of the header.
						offset = msg.lastIndexOf(header, body);
						if (offset != -1) {
							if (includeHdr)
								// Adjust the offset past the CRLF
								offset += 2;
							else 
								// Adjust the offset past the CRLF, the header, the colon and the space
								offset += header.length();
							// Next determine whether the CRLF occurs before
							// a comma,
							int offsetEnd = msg.indexOf("\r\n", offset);
							int offsetComma = msg.lastIndexOf(",", offsetEnd);
							if (offsetEnd != -1) {
								if (offsetComma != -1 &&
										offsetComma < offsetEnd) {
									location[0] = offsetComma;
									location[1] = offsetEnd;
									location[2] = COMMA_DELIMITED_END;
								}
								else {
									location[0] = offset;
									location[1] = offsetEnd;
									location[2] = CRLF;
								}
							}
						}
					}
					else if (hdrInstance.equals(MsgQueue.ANY)) {
						offset = msg.lastIndexOf(header, body);
						//offset = msg.indexOf(header, body);
						if (offset != -1) {
							if (includeHdr)
								// Adjust the offset past the CRLF
								offset += 2;
							else 
								// Adjust the offset past the CRLF, the header, the colon and the space
								offset += header.length();
							
							int offsetEnd = msg.indexOf("\r\n", offset);
							if (offsetEnd != -1) {
								location[0] = offset;
								location[1] = offsetEnd;
								location[2] = CRLF;
								
							}
						}
					}
				}
			}
		}
		// return whole message
		else {
			int body = msg.indexOf("\r\n\r\n");
			if (body == -1) {
				body = msg.length();
			}
			location[0] = 0;
			location[1] = body;
		}

		// Lastly if the location information is still empty and the hdr has a 
		// compact form retry with this value
		String compactForm = SIPConstants.hasCompactForm(hdr);
		if (compactForm != null && location[0] == -1 && location[1] == -1) {
			location = locateSIPHeader(compactForm, hdrInstance, 
					msg, includeHdr, ignoreMIA);
		}
		return location;
	}

	/**
	 * When a parameter is a generic parameter this method returns the data
	 * portion of the parameter. When a parameter is a presence parameter it returns the 
	 * the parameter_name itself. For positional parameters, the contents of the 
	 * parameter are returned. 
	 *
	 * @param hdr - The header to find
	 * @param hdrInstance - The parameter to locate.
	 * @param msg - The message to search for the header.
	 * @param ignoreMIA - this is a flag telling the system not to log that it couldn't 
	 * 		find the value sought by the invoker.
	 * 
	 * @return the contents of the parameter or the parameter itself if it is a presence
	 * parameter.
	 * 		
	 * 
	 */
	public synchronized String [] getAllSIPHeader(String hdr, String msg) {
		String [] hdrs = new String [10];
		int count = 0;
		int [] hdrLocation = locateSIPHeader(hdr, MsgQueue.FIRST, 
				msg, false, true);
		while (hdrLocation[0] != -1 && hdrLocation[1] != -1 && count < 10) {
			
			hdrs[count] = msg.substring(hdrLocation[0], hdrLocation[1]);
			logger.debug(PC2LogCategory.Model, subCat,
					"SIPLocator retrieved value=[" + hdrs[count] 
					+ "] for the hdr=" + hdr + " instance=" + count 
					+ ".");
			count++;
			hdrLocation = locateSIPHeader(hdr, Integer.toString(count+1), 
					msg, false, true);
		}
		if (count > 0)
			return hdrs;
		else 
			return null;
	}
	
	/**
	 * When a parameter is a generic parameter this method returns the data
	 * portion of the parameter. When a parameter is a presence parameter it returns the 
	 * the parameter_name itself. For positional parameters, the contents of the 
	 * parameter are returned. 
	 *
	 * @param hdr - The header to find
	 * @param hdrInstance - The parameter to locate.
	 * @param msg - The message to search for the header.
	 * 
	 * @return the contents of the header without the header itself
	 *  		
	 * 
	 */
	public synchronized String getSIPHeader(String hdr,  
			String hdrInstance, String msg) {
		int [] hdrLocation = locateSIPHeader(hdr, hdrInstance, 
				msg, false, false);
		if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
			String value = msg.substring(hdrLocation[0], hdrLocation[1]);
				logger.debug(PC2LogCategory.Model, subCat,
						"SIPLocator retrieved value=[" + value 
						+ "] for the hdr=" + hdr + " instance=" + hdrInstance 
						+ ".");
				return value;
			
		}
		return null;
	}
	
	
	
	/**
	 * This method retrieves the data for a parameter within a SIP message header.
	 * The method separates the message by header so that any fixed-positional parameters
	 * (referred to as positional parameters) can be found. If a parameter is not a positional
	 * parameter, this method will see if the parameter is a presence parameter. 
	 * A presence parameter is one that appears with no associated value, e.g. lr of a URI, but
	 * is terminated with a semi-colon. If the presence parameter is followed by an equal
	 * sign then the parameter is treated as a generic parameter that follows the 
	 * parameter_name=value; syntax. 
	 * 
	 * When a parameter is a generic parameter this method returns the data
	 * portion of the parameter. When a parameter is a presence parameter it returns the 
	 * the parameter_name itself. For positional parameters, the contents of the 
	 * parameter are returned. 
	 *
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
	public synchronized String getSIPParameter(String hdr, String param, 
			String hdrInstance, String msg) {
		int [] hdrLocation = locateSIPHeader(hdr, hdrInstance, 
				msg, false, false);
		if (hdrLocation[0] != -1 && hdrLocation[1] != -1) {
			int [] paramLocation = locateSIPParameter(hdr, 
					param, hdrLocation, msg);
			if (paramLocation[0] != -1 && 
					paramLocation[1] != -1 && 
					paramLocation[2] != -1) {
				String value = msg.substring(paramLocation[1], paramLocation[2]);
				logger.debug(PC2LogCategory.Model, subCat,
						"SIPLocator retrieved value=[" + value 
						+ "] for the hdr=" + hdr + " instance=" + hdrInstance 
						+ " param=" + param + ".");
				return value;
			}
		}
		return null;
	}
	
	public synchronized String getXMLAncestor(String body, 
			int ancestor, String type, String hdr) {

		int start = 0;
		int stop = body.length(); 
		int offset = 0;
		// We need to cover the case where the body is a multipart
		MultipartLocation mpl = isMultipart(body, type);
		if (mpl != null) {
			start = mpl.startLocation;
			stop = mpl.endLocation;
			offset = start;
		}
		LinkedList<String> stack = new LinkedList<String>();
		//offset = body.indexOf("<", offset);
		boolean done = false;
		while (offset != -1 && offset < stop && !done) {
			boolean endTag = false;
			boolean collect = false;
			int begin = 0;
			char c = body.charAt(offset);
			if (c == '<') {
				collect = true;
				offset++;
				begin = offset;
				while (offset != -1 && offset < stop && collect) {
					c = body.charAt(offset);
					if (c == ' ' ||
							c == '>' |
							c == '\t' ||
							c == '\n' ||
							c == '\r') {
						// We found the end of the tag
						String tag = body.substring(begin, offset);
						if (tag.equals(hdr)) {
							// We have parsed the tree to the target tag
							// now we need to stop looping and return the
							// ancestor desired
							offset = stop;
							collect = false;
							done = true;
						}
						else if (endTag) {
							String val = stack.removeFirst();
							if (!val.equals(tag)) {
								logger.warn(PC2LogCategory.Model, subCat,
										"SIPLocator encountered mismatch tag in " + type + " XML body. The start tag is [" + val 
										+ "] and the end tag is " + tag + ".");
							}
							collect = false;
						}
						else {
							stack.addFirst(tag);
							collect = false;
						}
					}
					else if (c == '/') {
						// First see if this is an end tag in the form </...>
						if (begin == offset) {
							// This means we are working on an end tag
							endTag = true;
							begin = offset+1;
						}
						else if (body.charAt(offset+1) == '>') {
							String tag = body.substring(begin, offset);
							if (tag.equals(hdr)) {
								// We have parsed the tree to the target tag
								// now we need to stop looping and return the
								// ancestor desired
								offset = stop;
								collect = false;
								done = true;
							}
							else {
								collect = false;
							}
							// There is no need to keep this tag as it began and ended with no 
							// children
						}
					}
					else if (c == '?') {
						collect = false;
					}
					offset++;
				}
			}
			else {
				offset++;
			}
		}
//		while (offset != -1 && offset < stop) {
//			// Move past the less than symbol
//			offset++;
//			boolean endTag = false;
//			if (body.charAt(offset) == '/') {
//				endTag = true;
//				// Adjust for the '/' character
//				offset++;
//			}
//
//			int gt = body.indexOf(">", offset);
//			int space = body.indexOf(" ", offset);
//			int cr = body.indexOf("/r", offset);
//			int emptyTag = body.indexOf("/>", offset);
//			// Test for end of tag condition first
//			if (endTag && gt != -1) {
//				stack.removeFirst();
//				offset = gt + 1;
//			}
//			else {
//				int end = -1;
//				boolean skip = false;
//				// This should be the start of a tag, however it could 
//				// be the start and stop of a tag (e.g. <tag/> or <tag />) or 
//				// it might be a CRLF
//				if (emptyTag != -1 && emptyTag < gt) {
//					if (space != -1 && space < emptyTag)
//						end = space;
////					else if (cr != -1 && cr < emptyTag)
////							end = cr;
//					else 
//						end = emptyTag;
//					// We don't need to add it to the stack if the tag
//					// starts and stops on the same line because it has no
//					// descendants
//					skip = true;
//				}
//				else if (gt != -1 && (gt < space || space == -1)) {
//					end = gt;
//				}
//				else if (space != -1 && space < gt)
//					end = space;
////				else if (cr != -1 && cr < gt)
////					end = cr;
//				else 
//					// This is a failure stop looking
//					offset = start;
//
//				if (end != -1) {
//					String tag = body.substring(offset, end);
//					if (!tag.equals("?xml") && !skip) {
//						if (tag.equals(hdr)) {
//							// We have parsed the tree to the target tag
//							// now we need to stop looping and return the
//							// ancestor desired
//							offset = stop;
//							continue;
//						}
//						else 
//							stack.addFirst(tag);
//					}
//					offset = gt+1;
//					offset = body.indexOf("<", offset);
//				}
//			}
//		}
		if (ancestor == PARENT && stack.size() >= 1) {
			return stack.removeFirst();
		}
		else if (ancestor == GRANDPARENT && stack.size() >= 2) {
			stack.removeFirst();
			return stack.removeFirst();
		}
		return null;
	}
	
	/**
	 * This method determines if the parameter that has already been located in 
	 * a header begins and ends with a double quote character.
	 * 
	 * @param location
	 * @param msg
	 * @return
	 */
	private boolean isQuoted(int [] location, String msg) {
		if (location[1] != -1 && location[2] != -1) {
			if (msg.charAt(location[1]) == '"' &&
			 		msg.charAt(location[2]-1) == '"') {
				return true;
			}
		}
		return false;
	}
	
	private synchronized int [] locateSimpleMessageBody(String subType, String hdr, 
			String hdrInstance, String param, boolean value, String msg,
			boolean ignoreMIA) {
		int [] location = new int [3];
		location[0] = -1;
		location[1] = -1;
		location[2] = NO_COMMA;
		if (hdr == null) {
			// Return the whole body
			location[0] = 0;
			location[1] = msg.length();
			location[2] = CRLF;
		}
		else {
			int offset = msg.indexOf(hdr);
			if (offset != -1) {
				int offsetEnd = msg.indexOf("\r\n", offset);
				if (offsetEnd != -1) {
					if (param != null) {
						if (SIPConstants.isMWISIPHeader(hdr)) {
							location[0] = offset;
							location[1] = offsetEnd;
							location[2] = CRLF;
							int [] paramLocation = locateSIPParameter(hdr, param, location, msg);
							if (paramLocation[0] != -1 &&
									paramLocation[1] != -1 &&
									paramLocation[2] != -1 &&
									paramLocation[3] != -1) {
								location[0] = paramLocation[1];
								location[1] = paramLocation[2];
								location[2] = paramLocation[3];
							}
						}
						else if (param.equals("new-msgs")){ 
							offset += hdr.length() + 2;
							int delim = msg.indexOf("/", offset);
							if (delim != -1 && delim < offsetEnd) {
								location[0] = offset;
								location[1] = delim;
								location[2] = CRLF;
							}
						}
						else if (param.equals("old-msgs")){ 
							offset = msg.indexOf("/", offset);
							if (offset != -1 && offset < offsetEnd) {
								offset++;
								int delim = msg.indexOf(" ", offset);
								if (delim != -1 && delim < offsetEnd) {
									location[0] = offset;
									location[1] = delim;
									location[2] = CRLF;
								}
							}
						}
						else if (param.equals("new-urgentmsgs")){ 
							offset = msg.indexOf("(", offset);
							if (offset != -1 && offset < offsetEnd) {
								offset++;
								int delim = msg.indexOf("/", offset);
								if (delim != -1 && delim < offsetEnd) {
									location[0] = offset;
									location[1] = delim;
									location[2] = CRLF;
								}
							}
						}
						else if (param.equals("old-urgentmsgs")){ 
							offset = msg.indexOf("(", offset);
							if (offset != -1 && offset < offsetEnd) {
								offset = msg.indexOf("/", offset);
								if (offset != -1 && offset < offsetEnd) {
									offset++;
									int delim = msg.indexOf(")", offset);
									if (delim != -1 && delim < offsetEnd) {
										location[0] = offset;
										location[1] = delim;
										location[2] = CRLF;
									}
								}
							}
						}
					}
				}
			}
		}
		return location;
	}
	/**
	 * This method locates the start and end of a parameter within a SIP message header.
	 * The method separates the message by header so that any fixed-positional parameters
	 * (referred to as positional parameters) can be found. If a parameter is not a positional
	 * parameter, this method will see if the parameter is a presence parameter. 
	 * A presence parameter is one that appears with no associated value, e.g. lr of a URI, but
	 * is terminated with a semi-colon. If the presence parameter is followed by an equal
	 * sign then the parameter is treated as a generic parameter that follows the 
	 * parameter_name=value; syntax. 
	 * 
	 * When a parameter is a generic parameter this method returns the location of the value
	 * portion of the parameter. When a parameter is a presence parameter it returns the 
	 * location of the parameter_name itself. For positional parameters, the location of the
	 * of the contents of the parameter are returned. 
	 *
	 * @param hdr - The header to find
	 * @param param - The parameter to locate.
	 * @param location - the starting and ending offset of the header within the message.
	 * @param msg - The message to search for the header.
	 * 
	 * @return location [] - A four dimension integer array. 
	 * 		entry 0 is the start of the offset of the parameter name, 
	 * 		entry 1 is the starting offset location of the parameter value,
	 * 		entry 2 is the ending offset location of the parameter name and value, 
	 * 		entry 3 specifies whether the parameter is terminated by CRLF, semi-colon, 
	 * 				or a comma and in the case of a comma, if the parameter is the 
	 * 				first entry in a comma delimited list, a middle entry, or the 
	 * 				last entry.
	 * 		
	 * 
	 */
	public synchronized int [] locateSIPParameter(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = new int [4];
		paramLocation[0] = -1;
		paramLocation[1] = -1;
		paramLocation[2] = -1;
		paramLocation[3] = -1;

		// For performance reasons test the first character to reduce the number
		// of string comparison's that we have to make
		switch (hdr.charAt(0)) {
		case 'A':
			// Now test the specific Header
			if (hdr.equals("Accept")) 
				return locateAcceptParam(hdr, param, location, msg);
			else if (hdr.equals("Accept-Contact")) 
				return locateAcceptContactParam(hdr, param, location, msg);
			else if (hdr.equals("Accept-Encoding")) 
				return locateAcceptEncodingParam(hdr, param, location, msg);
			else if (hdr.equals("Accept-Language")) 
				return locateAcceptLanguageParam(hdr, param, location, msg);
			else if (hdr.equals("Alert-Info")) 
				return locateAlertInfoParam(hdr, param, location, msg);
			else if (hdr.equals("Allow")) 
				return locateAllowParam(hdr, param, location, msg);
			else if (hdr.equals("Allow-Events")) 
				return locateAllowEventsParam(hdr, param, location, msg);
			else if (hdr.equals("Authentication-Info")) 
				return locateAuthenticationInfoParam(hdr, param, location, msg);
			else if (hdr.equals("Authorization")) 
				return locateAuthorizationParam(hdr, param, location, msg);
			break;
		case 'C':
			if (hdr.equals("Call-ID"))
				return locateCallIDParam(hdr, param, location, msg);
			else if (hdr.equals("Call-Info"))
				return locateCallInfoParam(hdr, param, location, msg);
			else if (hdr.equals("Contact"))
				return locateContactParam(hdr, param, location, msg);
			else if (hdr.equals("Content-Disposition"))
				return locateContentDispositionParam(hdr, param, location, msg);
			else if (hdr.equals("Content-Encoding"))
				return locateContentEncodingParam(hdr, param, location, msg);
			else if (hdr.equals("Content-ID"))
				return locateContentIDParam(hdr, param, location, msg);
			else if (hdr.equals("Content-Language"))
				return locateContentLanguageParam(hdr, param, location, msg);
			else if (hdr.equals("Content-Length"))
				return locateContentLengthParam(hdr, param, location, msg);
			else if (hdr.equals("Content-Type"))
				return locateContentTypeParam(hdr, param, location, msg);
			else if (hdr.equals("CSeq"))
				return locateCSeqParam(hdr, param, location, msg);
			break;
		case 'E':
			if (hdr.equals("Event"))
				return locateEventParam(hdr, param, location, msg);
			else if (hdr.equals("Expires"))
				return locateExpiresParam(hdr, param, location, msg);
			break;
		case 'F':
			if (hdr.equals("From"))
				return locateFromParam(hdr, param, location, msg);
			break;
		case 'G':
			if (hdr.equals("Geolocation"))
				return locateGeolocationParam(hdr, param, location, msg);
			break;
		case 'H':
			if (hdr.equals("HistoryInfo"))
				return locateHistoryInfoParam(hdr, param, location, msg);
			break;
		case 'J':
			if (hdr.equals("Join"))
				return locateJoinParam(hdr, param, location, msg);
			break;
		case 'M':
			if (hdr.equals("Max-Forwards"))
				return locateMaxForwardsParam(hdr, param, location, msg);
			if (hdr.equals("Min-Expires"))
				return locateMinExpiresParam(hdr, param, location, msg);
			break;
		case 'P':
			if (hdr.equals("Path"))
				return locatePathParam(hdr, param, location, msg);
			else if (hdr.equals("P-Access-Network-Info"))
				return locatePAccessNetworkInfoParam(hdr, param, location, msg);
			else if (hdr.equals("P-Asserted-Identity"))
				return locatePAssertedIdentityParam(hdr, param, location, msg);
			else if (hdr.equals("P-Associated-URI"))
				return locatePAssociatedURIParam(hdr, param, location, msg);
			else if (hdr.equals("P-Called-Party-ID"))
				return locatePCalledPartyIDParam(hdr, param, location, msg);
			else if (hdr.equals("P-Charging-Vector"))
				return locatePChargingVectorParam(hdr, param, location, msg);
			else if (hdr.equals("P-Charging-Function-Addrress"))
				return locatePChargingFunctionAddressParam(hdr, param, location, msg);
			// P-Charging-Function-Addresses uses the generic form for all params
			// P-Charging-Vector uses the generic form for all params
			else if (hdr.equals("P-Preferred-Identity"))
				return locatePPreferredIdentityParam(hdr, param, location, msg);
			else if (hdr.equals("Privacy"))
				return locatePrivacyParam(hdr, param, location, msg);
			else if (hdr.equals("Proxy-Authenticate"))
				return locateProxyAuthenticateParam(hdr, param, location, msg);
			else if (hdr.equals("Proxy-Authorization"))
				return locateProxyAuthorizationParam(hdr, param, location, msg);
			else if (hdr.equals("Proxy-Require"))
				return locateProxyRequireParam(hdr, param, location, msg);
			else if (hdr.equals("Priority"))
				return locatePriorityParam(hdr, param, location, msg);
			break;
		case 'R':
			if (hdr.equals(REQUEST_LINE))
				return locateRequestLineParam(hdr, param, location, msg);
			else if (hdr.equals("RAck"))
				return locateRAckParam(hdr, param, location, msg);
			else if (hdr.equals("Reason"))
				return locateReasonParam(hdr, param, location, msg);
			else if (hdr.equals("Record-Route"))
				return locateRecordRouteParam(hdr, param, location, msg);
			else if (hdr.equals("Referred-By"))
				return locateReferredByParam(hdr, param, location, msg);
			else if (hdr.equals("Refer-To"))
				return locateReferToParam(hdr, param, location, msg);
			else if (hdr.equals("Reject-Contact"))
				return locateRejectContactParam(hdr, param, location, msg);
			else if (hdr.equals("Replaces"))
				return locateReplacesParam(hdr, param, location, msg);
			else if (hdr.equals("Request-Disposition"))
				return locateRequestDispositionParam(hdr, param, location, msg);
			else if (hdr.equals("Require"))
				return locateRequireParam(hdr, param, location, msg);
			else if (hdr.equals("Retry-After"))
				return locateRetryAfterParam(hdr, param, location, msg);
			else if (hdr.equals("Route")) 
				return locateRouteParam(hdr, param, location, msg);
			else if (hdr.equals("RSeq"))
				return locateRSeqParam(hdr, param, location, msg);
			break;
		case 'S':
			if (hdr.equals(STATUS_LINE))
				return locateStatusLineParam(hdr, param, location, msg);
			else if (hdr.equals("Service-Route"))
				return locateServiceRouteParam(hdr, param, location, msg);
			else if (hdr.equals("SIP-ETag"))
				return locateSipETagParam(hdr, param, location, msg);
			else if (hdr.equals("SIP-If-Match"))
				return locateSipIfMatchParam(hdr, param, location, msg);
			else if (hdr.equals("Subscription-State"))
				return locateSubscriptionStateParam(hdr, param, location, msg);
			else if (hdr.equals("Supported"))
				return locateSupportedParam(hdr, param, location, msg);
			else if (hdr.equals("Security-Client") || 
					hdr.equals("Security-Server") || 
					hdr.equals("Security-Verify"))
				return locateSecurityParam(hdr, param, location, msg);
			break;
		case 'T':
			if (hdr.equals("To")) 
				return locateToParam(hdr, param, location, msg);
			else if (hdr.equals("Timestamp")) 
				return locateTimestampParam(hdr, param, location, msg);
			else if (hdr.equals("Target-Dialog")) 
				return locateTargetDialogParam(hdr, param, location, msg);
			break;
		case 'U':
			if (hdr.equals("Unsupported"))
				return locateUnsupportedParam(hdr, param, location, msg);
			break;
		case 'V':
			if (hdr.equals("Via"))
				return locateViaParam(hdr, param, location, msg);
			break;
		case 'W':
			if (hdr.equals("WWW-Authenticate"))
				return locateWWWAuthenticateParam(hdr, param, location, msg);
			break;

		}

		paramLocation = locateGenericParam(param, location, msg, ";");
				
		return paramLocation;
	}

	/**
	 * Locates the first fixed positional parameter within a
	 * Header. The value obtained is the one in the postion
	 * Header: value <DELIMITER> / CRLF
	 *  
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateFirstPositionParam(String hdr,
			String param, int [] location, String msg, String delimiter) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Header must have the following format for fixed position parameters
			// Header: value SEMI | CRLF
			// The Header however may be optional in the location information
			boolean adjust = false;
			int offset = -1;
			String temp = msg.substring(location[0], location[1]);
			if (temp.contains(hdr)) {
				offset = msg.indexOf(":", location[0]);
				adjust = true;
			}
			else 
				offset = location[0];
			
			if (offset != -1 && offset < location[1]) {
				// Adjust the beginning offset for the colon and space
				if (adjust) {
					offset++; // Adjust for colon
					offset += adjustSP(msg, offset); // Adjust for one or more white-space
				}
				int offsetDelim = msg.indexOf(delimiter, offset);
				// Value is the first field after the header
				if (offsetDelim != -1 && offsetDelim < location[1]) {
					paramLocation[0] = offset;
					paramLocation[1] = offset;
					paramLocation[2] = offsetDelim;
					paramLocation[3] = VALUE_PARAM;
				}
				else {
					int newline = msg.indexOf("\r\n");
					if (newline != -1 && newline < location[1]) {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = location[1];
						paramLocation[3] = VALUE_PARAM;
					}
				}
			}
		}
		return paramLocation;
	}

	/**
	 * Locates the first fixed positional parameter within a
	 * Header. The value obtained is the one in the postion
	 * Header: <DELIMITER>param=value<DELIMITER>
	 *  
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @param delimiter - the leading and ending delimiter for the parameter usually ';' or ','
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateGenericParam(String param, int [] location, String msg, String delimiter) {
		int [] paramLocation = initGenericLocation();
		
//		As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			int offset = msg.indexOf(delimiter+param+"=", location[0]);
			if (offset != -1 && offset < location[1]) {
				// Adjust the offset for the length of the delimiter, length of the param and the '='
				int paramStart = offset + delimiter.length();
				offset += (delimiter.length() + param.length() + 1);
				// Locate end of parameter
				// Look for delimiter
				int offsetSemi = msg.indexOf(delimiter, offset);
				if (offsetSemi != -1 && offsetSemi < location[1]) {
					paramLocation[0] = paramStart;
					paramLocation[1] = offset;
					paramLocation[2] = offsetSemi;
				}
				else {
					// Make sure it isn't a parameter to a name-addr
					int offsetlt = msg.indexOf(">", offset);
					if (offsetlt != -1 && offsetlt < location[1]) {
						paramLocation[0] = paramStart;
						paramLocation[1] = offset;
						paramLocation[2] = offsetlt;
					}
					// Make sure it isn't a parameter that ends at a space
					else {
						int offsetSpace = msg.indexOf(" ", offset);
						if (offsetSpace != -1 && offsetSpace < location[1]) {
							paramLocation[0] = paramStart;
							paramLocation[1] = offset;
							paramLocation[2] = offsetSpace;
						}
						else {
							paramLocation[0] = paramStart;
							paramLocation[1] = offset;
							paramLocation[2] = location[1];
						}
					}
				}
			}
		}
		return paramLocation;
	}
	
	/**
	 * Locates the first fixed positional parameter within a
	 * Header. The value obtained is the one in the postion
	 * Header: <DELIMITER1>param=value<DELIMITER2>
	 *  
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @param leadDelimiter - the leading delimiter for the parameter usually ';' or ','
	 * @param endDelimiter - the ending delimiter for the parameter usually ';' or ','
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateGenericParam(String param, int [] location, String msg, 
			String leadDelimiter, String endDelimiter) {
		int [] paramLocation = initGenericLocation();
		
//		As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			int offset = msg.indexOf(leadDelimiter+param+"=", location[0]);
			if (offset != -1 && offset < location[1]) {
				// Adjust the offset for the length of the delimiter, length of the param and the '='
				int paramStart = offset + leadDelimiter.length();
				offset += (leadDelimiter.length() + param.length() + 1);
				// Locate end of parameter
				// Look for delimiter
				int offsetSemi = msg.indexOf(endDelimiter, offset);
				if (offsetSemi != -1 && offsetSemi < location[1]) {
					paramLocation[0] = paramStart;
					paramLocation[1] = offset;
					paramLocation[2] = offsetSemi;
				}
				else {
					// Make sure it isn't a parameter to a name-addr
					int offsetlt = msg.indexOf(">", offset);
					if (offsetlt != -1 && offsetlt < location[1]) {
						paramLocation[0] = paramStart;
						paramLocation[1] = offset;
						paramLocation[2] = offsetlt;
					}
					// Make sure it isn't a parameter that ends at a space
					else {
						int offsetSpace = msg.indexOf(" ", offset);
						if (offsetSpace != -1 && offsetSpace < location[1]) {
							paramLocation[0] = paramStart;
							paramLocation[1] = offset;
							paramLocation[2] = offsetSpace;
						}
						else {
							paramLocation[0] = paramStart;
							paramLocation[1] = offset;
							paramLocation[2] = location[1];
						}
					}
				}
			}
		}
		return paramLocation;
	}

	/**
	 * Locates the name-addr parameter within a Header. 
	 * The value is everything after the header's colon SP value to the 
	 * terminating '>' character. This will also include the display-name 
	 * field if present
	 *  
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateNameAddr(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// name-addr has the following format for fixed position parameters
			// Header: [ display-name ] LAQUOT addr-spec RAQUOT 
			// addr-spec      =  SIP-URI / SIPS-URI / absoluteURI
			// absoluteURI    =  scheme ":" ( hier-part / opaque-part )
			// SIP-URI        =  "sip:" [ userinfo ] hostport uri-parameters [ headers ]
			// SIPS-URI       =  "sips:" [ userinfo ] hostport uri-parameters [ headers ]
			// 
			// The rules are to look for a '>' character, if this doesn't exist, look for
			// ';', and if this doesn't exist look for CRLF
			// The Header however may be optional in the location information
			int offset = -1;
			String temp = msg.substring(location[0], location[1]);
			boolean adjust = false;
			if (location[2] == COMMA_DELIMITED_MIDDLE ||
					location[2] == COMMA_DELIMITED_END) {
				offset = msg.indexOf("<", location[0]);
			}
			else if (temp.contains(hdr)) {
				offset =  msg.indexOf(":", location[0]);
				adjust = true;
			}
			else {
				offset = location[0];
			}
			
			if (offset != -1 && offset < location[1]) {
				// Adjust the beginning offset for the colon and space
				if (adjust) {
					offset++; // Adjust for colon
					offset += adjustSP(msg, offset); // Adjust for one or more white-space
				}
				
				// Because the name-addr can have the optional value of display-name, need to
				// first look for the character after the 'Header:' and proceed to the '>' 
				// character should mark the end of the name-addr field
				int offsetGT = msg.indexOf(">", offset);
				if (offsetGT != -1 && offsetGT < location[1]) {
					// Adjust ending index for the '>' character.
					paramLocation[0] = offset;
					paramLocation[1] = offset;
					paramLocation[2] = ++offsetGT;
					paramLocation[3] = VALUE_PARAM;
				}
				else {
					int offsetSemi = msg.indexOf(";", offset);
					if (offsetSemi != -1 && offsetSemi < location[1]) {
						// Adjust ending index for the '>' character.
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = offsetSemi;
						paramLocation[3] = VALUE_PARAM;
					}
					else {
						int offsetEnd = msg.indexOf("\r\n", offset);
						if (offsetEnd != -1 && offsetEnd < location[1]) {
							// Adjust ending index for the '>' character.
							paramLocation[0] = offset;
							paramLocation[1] = offset;
							paramLocation[2] = offsetEnd;
							paramLocation[3] = VALUE_PARAM;
						}
					}
				}
			}
			
		}
		return paramLocation;
	}

	/**
	 * Locates the addr-spec parameter within a Header. 
	 * The value is everything after the first '<' character value to the 
	 * terminating '>' character excluding both the '<' and '>' characters.
	 *  
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateAddrSpec(String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// name-addr has the following format for fixed position parameters
			// Header: [ display-name ] LAQUOT addr-spec RAQUOT 
			// addr-spec      =  SIP-URI / SIPS-URI / absoluteURI
			// absoluteURI    =  scheme ":" ( hier-part / opaque-part )
			// SIP-URI        =  "sip:" [ userinfo ] hostport uri-parameters [ headers ]
			// SIPS-URI       =  "sips:" [ userinfo ] hostport uri-parameters [ headers ]
			int offset = msg.indexOf("<", location[0]);
			if (offset != -1 && offset < location[1]) {
				// Adjust the beginning offset for the < 
				offset++;
				// Proceed to the '>' character should mark the end of the addr-spec field
				int offsetGT = msg.indexOf(">", offset);
				// Value is the first field after the header
				if (offsetGT != -1 && offsetGT < location[1]) {
					// Adjust ending index for the '>' character.
					paramLocation[0] = offset;
					paramLocation[1] = offset;
					paramLocation[2] = offsetGT;
					paramLocation[3] = VALUE_PARAM;
				}
			}
			else {
				// Since the '<' character doesn't appear in the name-addr assume we
				// are at the start
				offset = msg.indexOf(":", location[0]);
				if (offset != -1 && offset < location[1]) {
					// Adjust the beginning offset for the colon and space
					offset++; // Adjust for colon
					offset += adjustSP(msg, offset); // Adjust for one or more white-space
					int offsetSemi = msg.indexOf(";", offset);
					// Value is the first field after the header
					if (offsetSemi != -1 && offsetSemi < location[1]) {
						// Adjust ending index for the '>' character.
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = offsetSemi;
						paramLocation[3] = VALUE_PARAM;
					}
					else {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = location[1];
						paramLocation[3] = VALUE_PARAM;
					}
				}
			}
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter for an element in a list of values delimited by
	 * a comma or some other delimiter.  An example is the an option-tag in the Require Header.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateListParam(String hdr, String param, 
			int [] location, String msg, String delimiter) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			int offset = -1;
			int adjust = 0;
			String end = "\r\n";
			if (location[2] == CRLF) {
				String temp = msg.substring(location[0], location[1]);
				if (temp.contains(hdr)) {
					offset = msg.indexOf(":", location[0]);
					adjust = 2;
				}
				else {
					offset = location[0];
					adjust = 0;
				}
			}
			if (location[2] == COMMA_DELIMITED_FRONT) {
				String temp = msg.substring(location[0], location[1]);
				if (temp.contains(hdr)) {
					offset = msg.indexOf(":", location[0]);
					adjust = 2;
				}
				else {
					offset = location[0];
					adjust = 0;	
				}
				end = ",";
			}
			else if (location[2] == COMMA_DELIMITED_END) {
				offset = msg.lastIndexOf(delimiter, location[1]);
				adjust = 1;
			}
			else if (location[2] == COMMA_DELIMITED_MIDDLE ) {
				offset = location[0];
				end = delimiter;
			}
			if (offset != -1 && offset < location[1]) {
				// Adjust the beginning offset for the colon and space
				offset += adjust;
				int offsetSemi = msg.indexOf(end, offset);
				// Value is the first field after the header
				if (offsetSemi != -1 && offsetSemi <= location[1]) {
					paramLocation[0] = offset;
					paramLocation[1] = offset;
					paramLocation[2] = offsetSemi;
					paramLocation[3] = VALUE_COMMA_PARAM;
				}
				else {
					int newline = msg.indexOf("\r\n", offset);
					if (newline != -1 && newline < location[1]) {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = location[1];
						paramLocation[3] = VALUE_COMMA_PARAM;
					}
				}
			}
		}
		return paramLocation;
	}

	/**
	 * Locates the name-addr parameter within a Header. 
	 * The value is everything after the header's colon SP value to the 
	 * terminating '>' character.
	 *  
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @param initDelimit - the initial delimiter to locate the start of the parameter.
	 * 		Typically this will be an '@' value, but sometimes it may be a ' ' in the case like
	 * 		the Via.
	 * @param endDelimit - any special end delimiter that may needed instead of the default 
	 * 		value of CRLF, e.g. the space value that appears in the Request-Line
	 * @return location [] - A four dimension integer array. 
	 * 		entry 0 is the start of the offset of the parameter name, 
	 * 		entry 1 is the starting offset location of the parameter value,
	 * 		entry 2 is the ending offset location of the parameter name and value, 
	 * 		entry 3 specifies whether the parameter is terminated by CRLF, semi-colon, 
	 * 				or a comma and in the case of a comma, if the parameter is the 
	 * 				first entry in a comma delimited list, a middle entry, or the 
	 * 				last entry.
	 */
	private int [] locatePort(String param, int [] location, String msg, 
			String initDelimit, String endDelimit) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && initDelimit != null) {
			// name-addr has the following format for fixed position parameters
			// Header: [ display-name ] LAQUOT addr-spec RAQUOT 
			// addr-spec      =  SIP-URI / SIPS-URI / absoluteURI
			// absoluteURI    =  scheme ":" ( hier-part / opaque-part )
			// SIP-URI        =  "sip:" [ userinfo ] hostport uri-parameters [ headers ]
			// SIPS-URI       =  "sips:" [ userinfo ] hostport uri-parameters [ headers ]
			// hostport       =  host [ ":" port ]
			// uri-parameters =  *( ";" uri-parameter)
			// 
			// The rules are to look for the inital delimit character, look for
			// ';', and see if there is a : before it. 
			// If yes the value must be the value  between the ':' and the ';'
			// If not look for the '>' character and see if there is a : before it
			// If not look for the special character if it is defined and 
			// 		see if there is a : before it.
			// Otherwise look for the header delimiter as the last option.
			int offset = -1;
			boolean front = true;
			
			// Detemine where to start based upon the type of 
			// value used to locate the end of the header
			if (location[2] == COMMA_DELIMITED_MIDDLE || 
					location[2] == COMMA_DELIMITED_END) {
				offset = msg.indexOf("<", location[0]);
				front = false;
			}
			else
				offset = msg.indexOf(":", location[0]);
			
			// Next see what is the type delimiter that marks the end of the header
			// we are concerned with for this search.
			// The initial value covers single headers or COMMA_DELIMITED_END
			String hdrDelimit = "\r\n";
			if (endDelimit != null)
				hdrDelimit = endDelimit;
			else if (location[2] == COMMA_DELIMITED_FRONT ||
					location[2] == COMMA_DELIMITED_MIDDLE)
				hdrDelimit = ",";
			
			if (offset != -1 && offset < location[1]) {
				// Adjust the beginning offset for the colon and space
				if (front) {
					offset++; // Adjust for colon
					offset += adjustSP(msg, offset); // Adjust for one or more white-space
				}
				
				int offsetFront = msg.indexOf(initDelimit, offset);
				if (offsetFront != -1 && offsetFront < location[1]) {
					int offsetSemi = msg.indexOf(";", offsetFront);
					if (offsetSemi != -1 && offsetSemi < location[1]) {
						int offsetColon = msg.indexOf(":", offsetFront);
						if (offsetColon != -1 && offsetColon < offsetSemi) {
							// Adjust for the colon
							offsetColon++;
							paramLocation[0] = offsetColon;
							paramLocation[1] = offsetColon;
							paramLocation[2] = offsetSemi;
							paramLocation[3] = VALUE_PARAM;
						}
					}
					else {
						int offsetGT = msg.indexOf(">", offset);
						if (offsetGT != -1 && offsetGT < location[1]) {
							int offsetColon = msg.indexOf(":", offsetFront);
							if (offsetColon != -1 && offsetColon < offsetGT) {
								// Adjust for the colon
								offsetColon++;
								paramLocation[0] = offsetColon;
								paramLocation[1] = offsetColon;
								paramLocation[2] = offsetGT;
								paramLocation[3] = VALUE_PARAM;
							}
						}
						else {
							int offsetEnd = msg.indexOf(hdrDelimit, offset);
							if (offsetEnd != -1 && offsetEnd < location[1]) {
								int offsetColon = msg.indexOf(":", offsetFront);
								if (offsetColon != -1 && offsetColon < offsetEnd) {
									// Adjust for the colon
									offsetColon++;
									paramLocation[0] = offsetColon;
									paramLocation[1] = offsetColon;
									paramLocation[2] = offsetEnd;
									paramLocation[3] = VALUE_PARAM;
								}
							}
						}
					}
				}
				
			}
			
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter for an element that doesn't usually have an '='
	 * after but needs to be tested for it's presence in a header. An example of this 
	 * is the lr parameter or the audio parameter in Accept-Contact header.
	 * 	 * 
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * 
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locatePresenceParam(String param, int [] location, String msg) {
		int [] paramLocation = initGenericLocation();

		if (param != null) {
			String leadingDelimiter = ";";
			if (param.equals("*")) 
				leadingDelimiter = " ";
			int offset = msg.indexOf(leadingDelimiter+param, location[0]);
			if (offset != -1 && offset < location[1]) {
				// Adjust the beginning offset for the semi-colon 
				offset++;
				char lookAhead = msg.charAt(offset+param.length());
				if (lookAhead == '=') {
					// Get the value in the generic fashion
					return locateGenericParam(param, location, msg, ";");
				}
				else {
					paramLocation[0] = offset;
					paramLocation[1] = offset;
					paramLocation[2] = offset+param.length();
					paramLocation[3] = VALUE_PARAM;
				}
			}
		}
		return paramLocation;
	}
	
	private int [] locateDomain(String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
		// For the domain, the value lies between the @ (if it exists) of the
		// URI up to either the ] of an IPv6 address, a : of the port value, a ; for
		// a parameter or the > marking the end of the URI
		// whichever index is the lowest
		int offset = msg.indexOf("sip", location[0]);
		if (offset != -1 && offset < location[1]) {
			if (msg.charAt(offset+3) == 's')
				offset += 5;
			else 
				offset += 4;
			
			int offsetAmp = msg.indexOf("@", offset);
			if (offsetAmp != -1 && offsetAmp < location[1]) {
				// Adjust the beginning offset for the ampersand
				offset = offsetAmp + 1;
			}

				int offsetBrace = msg.indexOf("]", offset);
				int offsetColon = msg.indexOf(":", offset);
				int offsetSemi = msg.indexOf(";", offset);
				int offsetGT = msg.indexOf(">", offset);
				int offsetSpace = msg.indexOf(" ", offset);
				if (offsetBrace != -1 && offsetBrace < location[1] &&
						(offsetSemi == -1 || offsetBrace < offsetSemi) &&
						(offsetGT == -1 || offsetBrace < offsetGT)) {
					paramLocation[0] = offset;
					paramLocation[1] = offset;
					paramLocation[2] = offsetBrace + 1;
					paramLocation[3] = VALUE_PARAM;
				}
				else if (offsetColon != -1 && offsetColon < location[1] &&
						(offsetSemi == -1 || offsetColon < offsetSemi) &&
						(offsetGT == -1 || offsetColon < offsetGT)) {
					paramLocation[0] = offset;
					paramLocation[1] = offset;
					paramLocation[2] = offsetColon;
					paramLocation[3] = VALUE_PARAM;
				}
				else if (offsetSemi != -1 && offsetSemi < location[1] &&
						(offsetGT == -1 || offsetSemi < offsetGT)) {
					paramLocation[0] = offset;
					paramLocation[1] = offset;
					paramLocation[2] = offsetSemi;
					paramLocation[3] = VALUE_PARAM;
				}
				else if (offsetGT != -1 && offsetGT < location[1]) {
					paramLocation[0] = offset;
					paramLocation[1] = offset;
					paramLocation[2] = offsetGT;
					paramLocation[3] = VALUE_PARAM;
				}
				else if (offsetSpace != -1 && offsetSpace < location[1]) {
					paramLocation[0] = offset;
					paramLocation[1] = offset;
					paramLocation[2] = offsetSpace;
					paramLocation[3] = VALUE_PARAM;
				}
			}
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter media-range within a 
	 * Accept Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateAcceptParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			// Allow has the following format for fixed position parameters
			// Allow: [ accept-range *(COMMA accept-range) ]
			// accept-range   =  media-range *(SEMI accept-param)
			if (param.equals("media-range")) 
				return locateListParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter ac-value within a 
	 * Accept-Contact Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateAcceptContactParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			// Accept-Contact has the following format for fixed position parameters
			// Accept-Contact: ac-value *(COMMA ac-value)
			// ac-value = "*" *(SEMI ac-params)
			//  ac-params =  feature-param / req-param / explicit-param / generic-param
			//           ;;feature param from RFC 3840
			//           ;;generic-param from RFC 3261
			// req-param =  "require"
			// explicit-param  =  "explicit"
			if (param.equals("ac-value")) 
				return locatePresenceParam(param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter value within a 
	 * Accept-Encoding Header. It will also return generic parameters:
	 * 	handling and other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateAcceptEncodingParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Accept-Encoding has the following format for fixed position parameters
			// Accept-Encoding  =  "Accept-Encoding" HCOLON
            // [ encoding *(COMMA encoding) ]
            //  encoding         =  codings *(SEMI accept-param)
            //  codings          =  content-coding / "*"
            //  content-coding   =  token
			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter value within a 
	 * Accept-Language Header. It will also return generic parameters:
	 * 	handling and other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateAcceptLanguageParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Accept-Language has the following format for fixed position parameters
			// Accept-Language  =  "Accept-Language" HCOLON
            // [ language *(COMMA language) ]
            //  language         =  language-range *(SEMI accept-param)
            //  language-range   =  ( ( 1*8ALPHA *( "-" 1*8ALPHA ) ) / "*" )
			// accept-param   =  ("q" EQUAL qvalue) / generic-param
			// qvalue         =  ( "0" [ "." 0*3DIGIT ] )
            // ( "1" [ "." 0*3("0") ] )
			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter absoluteURI within a 
	 * Alert-Info  Header. It will also return generic parameters.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateAlertInfoParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Alert-Info  has the following format for fixed position parameters
			// Alert-Info : alert-param *(COMMA alert-param)
			// alert-param =  LAQUOT absoluteURI RAQUOT *( SEMI generic-param )
			if (param.equals("absoluteURI")) 
				return locateNameAddr(hdr, param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}	

	/**
	 * Locates the positional parameter method within a 
	 * Allow Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateAllowParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			// Allow has the following format for fixed position parameters
			// Allow: [Method *(COMMA Method)]
			if (param.equals("method")) 
				return locateListParam(hdr, param, location, msg, ",");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter event-type within a 
	 * Allow-Events Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateAllowEventsParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			// Allow-Events has the following format for fixed position parameters
			// Allow-Events: event-type *( SEMI event-type )
			if (param.equals("event-type")) {
				return locateListParam(hdr, param, location, msg, ";");
				//int offset = -1;
//				int adjust = 0;
//				String end = "\r\n";
//				if (location[2] == CRLF) {
//					offset = msg.indexOf(":", location[0]);
//					adjust = 2;
//				}
//				if (location[2] == COMMA_DELIMITED_FRONT) {
//					offset = msg.indexOf(":", location[0]);
//					adjust = 2;
//					end = ",";
//				}
//				else if (location[2] == COMMA_DELIMITED_END) {
//					offset = msg.lastIndexOf(",", location[1]);
//					adjust = 1;
//				}
//				else if (location[2] == COMMA_DELIMITED_MIDDLE ) {
//					offset = location[0];
//					end = ",";
//				}
//				if (offset != -1 && offset < location[1]) {
//					// Adjust the beginning offset for the colon and space
//					offset += adjust;
//					int offsetSemi = msg.indexOf(end, offset);
//					// Value is the first field after the header
//					if (offsetSemi != -1 && offsetSemi <= location[1]) {
//						paramLocation[0] = offset;
//						paramLocation[1] = offset;
//						paramLocation[2] = offsetSemi;
//						paramLocation[3] = VALUE_COMMA_PARAM;
//					}
//					else {
//						int newline = msg.indexOf("\r\n", offset);
//						if (newline != -1 && newline < location[1]) {
//							paramLocation[0] = offset;
//							paramLocation[1] = offset;
//							paramLocation[2] = location[1];
//							paramLocation[3] = VALUE_COMMA_PARAM;
//						}
//					}
//				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates only generic parameters within a Authenticate-Info Header. 
	 * realm, doamin, algorithm, nonce, qop, and opaque are all generic
	 * parameters
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateAuthenticationInfoParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			paramLocation = locateGenericParam(param, location, msg, ",");
			// If we fail to find the parameter, we need to check to see if 
			// it is the first parameter as this one will not start with a comma
			if (paramLocation[0] == -1 &&
					paramLocation[1] == -1 &&
					paramLocation[2] == -1) {
				paramLocation = locateGenericParam(param, location, msg, " ", ",");
			}
			
			if (isQuoted(paramLocation, msg)) {
				paramLocation[1]++;
				paramLocation[2]--;
			}
		}
		return paramLocation;
	}
	
	/**
	 * Locates only generic parameters within a Authorization Header. 
	 * realm, doamin, algorithm, nonce, qop, and opaque are all generic
	 * parameters
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateAuthorizationParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();

		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			paramLocation = locateGenericParam(param, location, msg, ",");
			// If we fail to find the parameter, we need to check to see if 
			// it is the first parameter as this one will not start with a comma
			if (paramLocation[0] == -1 &&
					paramLocation[1] == -1 &&
					paramLocation[2] == -1) {
				paramLocation = locateGenericParam(param, location, msg, " ", ",");
			}
			
			if (isQuoted(paramLocation, msg)) {
				paramLocation[1]++;
				paramLocation[2]--;
			}
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter value within a 
	 * Call-ID Header. It will also return generic parameters:
	 * 	handling and other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateCallIDParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Call-ID has the following format for fixed position parameters
			// Call-ID: dHCOLON callid
			// callid =  word [ "@" word ]
			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter name-addr or addr-spec within a Call-Info Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateCallInfoParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// To has the following format for fixed position parameters
			// To: [ display-name ] LAQUOT addr-spec RAQUOT *( SEMI tag-param / generic-param)
			if (param.equals(NAME_ADDR)) {
				return locateNameAddr(hdr, param, location, msg);
			}
			else if (param.equals(ADDR_SPEC)) {
				return locateAddrSpec(param, location, msg);
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter name-addr and addr-spec within a 
	 * Contact Header. It will also return generic parameters:
	 * q, expires, and any other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateContactParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Contact has the following format for fixed position parameters
			// Contact: ( STAR / (contact-param *(COMMA contact-param)))
			// contact-param  =  (name-addr / addr-spec) *(SEMI contact-params)
			// contact-params =  c-p-q / c-p-expires / contact-extension
			// c-p-q              =  "q" EQUAL qvalue
			// c-p-expires        =  "expires" EQUAL delta-seconds
			// contact-extension  =  generic-param
			// delta-seconds      =  1*DIGIT
			if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals(ADDR_SPEC)) 
				return locateAddrSpec(param, location, msg);
			else if (param.equals("username")) {
				// For the user name we need to find the : after the 
				// URI type and it includes everything up to the @
				int offset = msg.indexOf(" ", location[0]);
				if (offset != -1 && offset < location[1]) {
					// Adjust the beginning offset for the space
					offset += 1;
					int offsetType = msg.indexOf(":", offset);
					if (offsetType != -1 && offsetType < location[1]) {
						// Adjust for the colon
						offsetType += 1;
						int offsetAmp = msg.indexOf("@", offsetType);
						if (offsetAmp != -1 && offsetAmp < location[1]) {
							paramLocation[0] = offsetType;
							paramLocation[1] = offsetType;
							paramLocation[2] = offsetAmp;
							paramLocation[3] = VALUE_PARAM;
						}
					}
				}
			}
			else if (param.equals("domain")) {
				// For the domain, the value lies between the @ of the
				// URI up to either the : of the port value, a ; for
				// a parameter or the > marking the end of the URI
				// whichever index is the lowest
				return locateDomain(param, location, msg);
//				int offset = msg.indexOf(" ", location[0]);
//				if (offset != -1 && offset < location[1]) {
//					// Adjust the beginning offset for the space
//					offset += 1;
//					int offsetAmp = msg.indexOf("@", offset);
//					if (offsetAmp != -1 && offsetAmp < location[1]) {
//						// Adjust the beginning offset for the ampersand
//						offsetAmp += 1;
//						int offsetColon = msg.indexOf(":", offsetAmp);
//						int offsetSemi = msg.indexOf(";", offsetAmp);
//						int offsetGT = msg.indexOf(">", offsetAmp);
//						int offsetSpace = msg.indexOf(" ", offsetAmp);
//						if (offsetColon != -1 && offsetColon < location[1] &&
//								(offsetSemi == -1 || offsetColon < offsetSemi) &&
//								(offsetGT == -1 || offsetColon < offsetGT)) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetColon;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetSemi != -1 && offsetSemi < location[1] &&
//								(offsetGT == -1 || offsetSemi < offsetGT)) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetSemi;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetGT != -1 && offsetGT < location[1]) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetGT;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetSpace != -1 && offsetSpace < location[1]) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetSpace;
//							paramLocation[3] = VALUE_PARAM;
//						}
//					}
//				}
			}
			else if (param.equals("port")) {
				return locatePort(param, location, msg, "@", null);
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}	

	/**
	 * Locates the positional parameter value within a 
	 * Content-Disposition Header. it will also return generic parameters:
	 * 	handling and other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateContentDispositionParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Content-Disposition has the following format for fixed position parameters
			// Content-Disposition: disp-type *( SEMI disp-param )
			// disp-type = "render" / "session" / "icon" / "alert" / disp-extension-token
			// disp-param =  handling-param / generic-param
			// handling-param =  "handling" EQUAL ( "optional" / "required" / other-handling )
			// other-handling  =  token
			// disp-extension-token =  token
			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter value within a 
	 * Content-Encoding Header. It will also return generic parameters:
	 * 	handling and other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateContentEncodingParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Content-Encoding has the following format for fixed position parameters
			// Content-Encoding  =  ( "Content-Encoding" / "e" ) HCOLON
			// content-coding *(COMMA content-coding)

			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter name-addr or addr-spec within a Content-ID Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateContentIDParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();

		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			if (param.equals(NAME_ADDR)) {
				return locateNameAddr(hdr, param, location, msg);
			}
			else if (param.equals(ADDR_SPEC)) {
				return locateAddrSpec(param, location, msg);
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter value within a 
	 * Content-Language Header. It will also return generic parameters:
	 * 	handling and other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateContentLanguageParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Content-Language has the following format for fixed position parameters
			// Content-Language  =  "Content-Language" HCOLON
            // language-tag *(COMMA language-tag)
            // language-tag      =  primary-tag *( "-" subtag )
            // primary-tag       =  1*8ALPHA
            // subtag            =  1*8ALPHA
			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter value within a 
	 * Content-Length Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateContentLengthParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Content-Length has the following format for fixed position parameters
			// Content-Length: 1*DIGIT
			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameters value, media-type and media-subtype within a 
	 * Content-Type Header. It will also return generic parameters.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateContentTypeParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Content-Type has the following format for fixed position parameters
			// Content-Type: m-type SLASH m-subtype *(SEMI m-parameter)
			// m-type           =  discrete-type / composite-type
			// discrete-type    =  "text" / "image" / "audio" / "video"
			//                     / "application" / extension-token
			// composite-type   =  "message" / "multipart" / extension-token
			// extension-token  =  ietf-token / x-token
			// ietf-token       =  token
			// x-token          =  "x-" token
			// m-subtype        =  extension-token / iana-token
			// iana-token       =  token
			// m-parameter      =  m-attribute EQUAL m-value
			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, " ");
			else if (param.equals("media-type")) 
				return locateFirstPositionParam(hdr, param, location, msg, "/");
			else if (param.equals("media-subtype")) {
				// Request-URI begins after space following the method 
				int offset = msg.indexOf("/", location[0]);
				if (offset != -1 && offset < location[1]) {
					// Adjust the beginning offset for the '/' character
					offset++;
					// m-subtype ends at next semi-colon or CRLF 
					int offsetSemi = msg.indexOf(";", offset);
					if (offsetSemi != -1 && offsetSemi < location[1]) {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = offsetSemi;
						paramLocation[3] = VALUE_PARAM;
					}
					else {
						int newline = msg.indexOf("\r\n");
						if (newline != -1 && newline < location[1]) {
							paramLocation[0] = offset;
							paramLocation[1] = offset;
							paramLocation[2] = location[1];
							paramLocation[3] = VALUE_PARAM;
						}
					}
				}
			}
			else
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameters value and method within a 
	 * CSeq Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateCSeqParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// CSeq has the following format for fixed position parameters
			// CSeq: 1*DIGIT LWS Method
			if (param.equals("value")) {
				return locateFirstPositionParam(hdr, param, location, msg, " ");
			}
			else if (param.equals("method")) {
				int offset = -1;
				String temp = msg.substring(location[0], location[1]);
				boolean adjust = false;
				if (temp.contains(hdr)) {
					offset = msg.indexOf(":", location[0]);
					adjust = true;
				}
				else 
					offset = location[0];
				
				if (offset != -1 && offset < location[1]) {
					// Adjust the beginning offset for the colon and space
					if (adjust) {
						offset++; // Adjust for colon
						offset += adjustSP(msg, offset); // Adjust for one or more white-space
					}
					int offsetSpace = msg.indexOf(" ", offset);
					// Method is the second field after the header
					if (offsetSpace != -1 && offsetSpace < location[1]) {
						int offsetCR = msg.indexOf("\r", offsetSpace+1);
						if (offsetCR != -1 && offsetCR <= location[1]) {
							paramLocation[0] = offsetSpace+1;
							paramLocation[1] = offsetSpace+1;
							paramLocation[2] = offsetCR;
							paramLocation[3] = VALUE_PARAM;
						}
					}
				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter event-type within an 
	 * Event Header. It will also return generic parameters:
	 * 	id and other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateEventParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Event has the following format for fixed position parameters
			// Event: event-type *( SEMI event-params )
			// event-type        =  event-package *( "." event-template )
			// event-package     =  token-nodot
			// event-template    =  token-nodot
			// event-param       =  generic-param / ( "id" EQUAL token )
			if (param.equals("event-type")) {
				int offset = -1;
				String temp = msg.substring(location[0], location[1]);
				boolean adjust = false;
				if (temp.contains(hdr)) {
					offset =msg.indexOf(":", location[0]);
					adjust = true;
				}
				else 
					offset = location[0];
				if (offset != -1 && offset < location[1]) {
					if (adjust) {
						// Adjust the beginning offset for the colon and space
						offset++; // Adjust for colon
						offset += adjustSP(msg, offset); // Adjust for one or more white-space
					}	
					int offsetSemi = msg.indexOf(";", offset);
					// Value is the first field after the header
					if (offsetSemi != -1 && offsetSemi < location[1]) {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = offsetSemi;
						paramLocation[3] = VALUE_PARAM;
					}
					else {
						int newline = msg.indexOf("\r\n");
						if (newline != -1 && newline < location[1]) {
							paramLocation[0] = offset;
							paramLocation[1] = offset;
							paramLocation[2] = location[1];
							paramLocation[3] = VALUE_PARAM;
						}
					}
				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter value within a 
	 * Expires Header. I
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateExpiresParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Expires has the following format for fixed position parameters
			// Expires: delta-seconds
			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter name-addr and addr-spec within a 
	 * From Header. It will also return generic parameters:
	 * tag and any other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateFromParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// From has the following format for fixed position parameters
			// From: from-spec
			// from-spec = ( name-addr / addr-spec ) *( SEMI from-param )
			// from-param  =  tag-param / generic-param
			// tag-param   =  "tag" EQUAL token
			if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals(ADDR_SPEC)) 
				return locateAddrSpec(param, location, msg);
			else if (param.equals("display-name"))
				return locateFirstPositionParam(hdr, param, location, msg, " ");
			else if (param.equals("domain")) {
				// For the domain, the value lies between the @ of the
				// URI up to either the : of the port value, a ; for
				// a parameter or the > marking the end of the URI
				// whichever index is the lowest
				return locateDomain(param, location, msg);
//				int offset = msg.indexOf(" ", location[0]);
//				if (offset != -1 && offset < location[1]) {
//					// Adjust the beginning offset for the space
//					offset += 1;
//					int offsetAmp = msg.indexOf("@", offset);
//					if (offsetAmp != -1 && offsetAmp < location[1]) {
//						// Adjust the beginning offset for the ampersand
//						offsetAmp += 1;
//						int offsetColon = msg.indexOf(":", offsetAmp);
//						int offsetSemi = msg.indexOf(";", offsetAmp);
//						int offsetGT = msg.indexOf(">", offsetAmp);
//						int offsetSpace = msg.indexOf(" ", offsetAmp);
//						if (offsetColon != -1 && offsetColon < location[1] &&
//								(offsetSemi == -1 || offsetColon < offsetSemi) &&
//								(offsetGT == -1 || offsetColon < offsetGT)) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetColon;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetSemi != -1 && offsetSemi < location[1] &&
//								(offsetGT == -1 || offsetSemi < offsetGT)) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetSemi;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetGT != -1 && offsetGT < location[1]) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetGT;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetSpace != -1 && offsetSpace < location[1]) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetSpace;
//							paramLocation[3] = VALUE_PARAM;
//						}
//					}
//				}
			}
			else 
				return locatePresenceParam(param, location, msg);
				
		}
		return paramLocation;
	}	
	/**
	 * Locates the positional parameter name-addr, addr-spec or lr within a To Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateGeolocationParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
//			Geolocation        =  "Geolocation" HCOLON (locationValue *(COMMA
//                  locationValue))
//			locationValue      =  LAQUOT locationURI RAQUOT *(SEMI geoloc-param)
//			locationURI        =  sip-URI / sips-URI / pres-URI 
//			                  / cid-url ; (from RFC 2392)
//			                  / absoluteURI ; (from RFC 3261)
//			geoloc-param       =  "inserted-by" EQUAL geoloc-inserter
//			                  / "used-for-routing"
//			                  / generic-param ; (from RFC 3261)
//			geoloc-inserter    =  DQUOTE hostport DQUOTE
//			                  / gen-value ; (from RFC 3261)
//		   From RFC 2392 - 
//		   A "cid" URL is converted to the corresponding Content-ID message
//		   header [MIME] by removing the "cid:" prefix, converting the % encoded
//		   character to their equivalent US-ASCII characters, and enclosing the
//		   remaining parts with an angle bracket pair, "<" and ">".  For
//		   example, "cid:foo4%25foo1@bar.net" corresponds to
//
//		     Content-ID: <foo4%25foo1@bar.net>


		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			if (param.equals(NAME_ADDR)) {
				return locateNameAddr(hdr, param, location, msg);
			}
			else if (param.equals(ADDR_SPEC)) {
				return locateAddrSpec(param, location, msg);
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter name-addr and addr-spec within a 
	 * Contact Header. It will also return generic parameters:
	 * q, expires, and any other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateHistoryInfoParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// History-Info has the following format for fixed position parameters
			// History-Info = "History-Info" HCOLON hi-entry *(COMMA hi-entry)
			//
			// hi-entry = hi-targeted-to-uri *( SEMI hi-param )
			//
			// hi-targeted-to-uri= name-addr
			//
			// hi-param = hi-index / hi-extension
			//
			// hi-index = "index" EQUAL 1*DIGIT *(DOT 1*DIGIT)
			//
			// hi-extension = generic-param

			if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals(ADDR_SPEC)) 
				return locateAddrSpec(param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter name-addr or addr-spec within a Join Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateJoinParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// To has the following format for fixed position parameters
			// To: [ display-name ] LAQUOT addr-spec RAQUOT *( SEMI tag-param / generic-param)
			if (param.equals("call-id")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter value within a 
	 * Max-Forwards Header. I
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateMaxForwardsParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Max-Forwards has the following format for fixed position parameters
			// Max-Forwards: 1*DIGIT
			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter value within a 
	 * Min-Expires Header. I
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateMinExpiresParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Min-Expires has the following format for fixed position parameters
			// Min-Expires: delta-seconds

			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter access-type within a 
	 * P-Access-Network-Info. It will also return generic parameters.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locatePAccessNetworkInfoParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation =initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// P-Access-Network-Info has the following format for fixed position parameters
			// P-Access-Network-Info: access-net-spec
			// access-net-spec = access-type *(SEMI access-info)
			// access-type = "IEEE-802.11a" / "IEEE-802.11b" /
			//    "3GPP-GERAN" / "3GPP-UTRAN-FDD" /
			//    "3GPP-UTRAN-TDD" /
			//    "3GPP-CDMA2000" / token
			// access-info = cgi-3gpp / utran-cell-id-3gpp / extension-access-info
			// extension-access-info  = gen-value
			// cgi-3gpp = "cgi-3gpp" EQUAL (token / quoted-string)
			// utran-cell-id-3gpp = "utran-cell-id-3gpp" EQUAL (token / quoted-string)	 
			if (param.equals("access-type")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter name-addr and addr-spec within a 
	 * P-Asserted-Identity Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locatePAssertedIdentityParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// P-Asserted-Identity has the following format for fixed position parameters
			// P-Asserted-Identity: PAssertedID-value *(COMMA PAssertedID-value)
			// PAssertedID-value = name-addr / addr-spec
			if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals(ADDR_SPEC)) 
				return locateAddrSpec(param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter icid-value within a 
	 * P-Charging-Vector. It will also return generic parameters.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locatePChargingVectorParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation =initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			
			if (param.equals("icid-value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else if (param.equals("orig-ioi") || param.equals("term-ioi")) {
				int offset = msg.indexOf(param+"=", location[0]);
				if (offset != -1 && offset < location[1]) {
					// Adjust the offset for the length of the param, the '=' and
					// the '"'
					offset += param.length()+2;
					// Locate end of parameter
					// Look for delimiter
					int offsetQuote = msg.indexOf("\"", offset);
					if (offsetQuote != -1 && offsetQuote < location[1]) {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = offsetQuote;
					}
				}
			}
			
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates only generic parameters within a P-Charging-Function-Address Header. 
	 * realm, doamin, algorithm, nonce, qop, and opaque are all generic
	 * parameters
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locatePChargingFunctionAddressParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			return locateGenericParam(param, location, msg, ",");
		}
		return paramLocation;
	}


	/**
	 * Locates the positional parameters path-value, name-addr or lr within a 
	 * Path Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locatePathParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Pathhas the following format for fixed position parameters
			// Path: path-value *( COMMA path-value )
			// path-value = name-addr *( SEMI rr-param )
			if (param.equals("path-value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ",");
			if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals("lr")) 
				return locatePresenceParam(param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameters name-addr or lr within a 
	 * P-Associated-URI Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locatePAssociatedURIParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// P-Associated-URI has the following format for fixed position parameters
			// P-Associated-URI: (p-aso-uri-spec) *(COMMA p-aso-uri-spec)
			// p-aso-uri-spec = name-addr *(SEMI ai-param)
			if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals("lr")) 
				return locatePresenceParam(param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter name-addr within a 
	 * P-Called-Party-ID Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locatePCalledPartyIDParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// P-Called-Party-ID has the following format for fixed position parameters
			// P-Called-Party-ID: called-pty-id-spec
			// called-pty-id-spec = name-addr *(SEMI cpid-param)
			if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter name-addr and addr-spec within a 
	 * P-Preferred-Identity Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locatePPreferredIdentityParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// P-Preferred-Identity has the following format for fixed position parameters
			// P-Preferred-Identity: PPreferredID-value *(COMMA PPreferredID-value)
			// PPreferredID-value = name-addr / addr-spec
			if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals(ADDR_SPEC)) 
				return locateAddrSpec(param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter value within a 
	 * Priority Header.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locatePriorityParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			if (param.equals("value")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter priv-value within a 
	 * Privacy Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locatePrivacyParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			// Privacy has the following format for fixed position parameters
			// Privacy: priv-value *(";" priv-value)
			if (param.equals("priv-value")) 
				return locateListParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates only generic parameters within a Proxy-Authenticate Header. 
	 * realm, doamin, algorithm, nonce, qop, and opaque are all generic
	 * parameters
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateProxyAuthenticateParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Proxy-Authenticate has the following format for fixed position parameters
			// Proxy-Authenticate: challenge
			// challenge = ("Digest" LWS digest-cln *(COMMA digest-cln)) / other-challenge
			// other-challenge = auth-scheme LWS auth-param *(COMMA auth-param)
			// digest-cln  =  realm / domain / nonce / opaque / stale / algorithm / qop-options / auth-param
			// realm =  "realm" EQUAL realm-value
			// realm-value =  quoted-string
			// domain = "domain" EQUAL LDQUOT URI *( 1*SP URI ) RDQUOT
			// URI = absoluteURI / abs-path
			// nonce  =  "nonce" EQUAL nonce-value
			// nonce-value =  quoted-string
			// opaque =  "opaque" EQUAL quoted-string
			// stale =  "stale" EQUAL ( "true" / "false" )
			// algorithm =  "algorithm" EQUAL ( "MD5" / "MD5-sess" / token )
			// qop-options = "qop" EQUAL LDQUOT qop-value *("," qop-value) RDQUOT
			// qop-value =  "auth" / "auth-int" / token
			paramLocation = locateGenericParam(param, location, msg, ",");
			// If we fail to find the parameter, we need to check to see if 
			// it is the first parameter as this one will not start with a comma
			if (paramLocation[0] == -1 &&
					paramLocation[1] == -1 &&
					paramLocation[2] == -1) {
				paramLocation = locateGenericParam(param, location, msg, " ", ",");
			}
			
			if (isQuoted(paramLocation, msg)) {
				paramLocation[1]++;
				paramLocation[2]--;
			}
		}
		return paramLocation;
	}
	
	/**
	 * Locates only generic parameters within a Proxy-Authorization Header. 
	 * realm, doamin, algorithm, nonce, qop, and opaque are all generic
	 * parameters
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateProxyAuthorizationParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			paramLocation = locateGenericParam(param, location, msg, ",");
			// If we fail to find the parameter, we need to check to see if 
			// it is the first parameter as this one will not start with a comma
			if (paramLocation[0] == -1 &&
					paramLocation[1] == -1 &&
					paramLocation[2] == -1) {
				paramLocation = locateGenericParam(param, location, msg, " ", ",");
			}
			
			if (isQuoted(paramLocation, msg)) {
				paramLocation[1]++;
				paramLocation[2]--;
			}
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter option-tag within a 
	 * Proxy-Require Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateProxyRequireParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			// Proxy-Require has the following format for fixed position parameters
			// Proxy-Require: option-tag *(COMMA option-tag)
			if (param.equals("option-tag")) 
				return locateListParam(hdr, param, location, msg, ",");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameters response-num, cseq-num and method within a 
	 * RAck Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateRAckParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// RAck has the following format for fixed position parameters
			// RAck: response-num LWS CSeq-num LWS Method
			if (param.equals("response-num")) 
				return locateFirstPositionParam(hdr, param, location, msg, " ");
			else if (param.equals("cseq-num")) {
				int offset = -1;
				String temp = msg.substring(location[0], location[1]);
				boolean adjust = false;
				if (temp.contains(hdr)) {
					offset = msg.indexOf(":", location[0]);
					adjust = true;
				}
				else 
					offset = location[0];
				
				if (offset != -1 && offset < location[1]) {
					if (adjust) {
						// Adjust the beginning offset for the colon and space
						offset++; // Adjust for colon
						offset += adjustSP(msg, offset); // Adjust for one or more white-space
					}
					int offsetSpace1 = msg.indexOf(" ", offset);
					if (offsetSpace1 != -1 && offsetSpace1 < location[1]) {
						offsetSpace1++; // Adjust for colon
						offsetSpace1 += adjustSP(msg, offsetSpace1); // Adjust for one or more white-space
						int offsetSpace2 = msg.indexOf(" ", offsetSpace1);
						// Value is the second field after the header
						if (offsetSpace2 != -1 && offsetSpace2 < location[1]) {
							paramLocation[0] = offsetSpace1;
							paramLocation[1] = offsetSpace1;
							paramLocation[2] = offsetSpace2;
							paramLocation[3] = VALUE_PARAM;
						}

					}
				}
			}
			else if (param.equals("method")) {
				int offset = -1;
				String temp = msg.substring(location[0], location[1]);
				boolean adjust = false;
				if (temp.contains(hdr)) {
					offset = msg.indexOf(":", location[0]);
					adjust = true;
				}
				else 
					offset = location[0];
				
				if (offset != -1 && offset < location[1]) {
					if (adjust) {// Adjust the beginning offset for the colon and space
						offset++; // Adjust for colon
						offset += adjustSP(msg, offset); // Adjust for one or more white-space
					}
					int offsetSpace1 = msg.indexOf(" ", offset);
					if (offsetSpace1 != -1 && offsetSpace1 < location[1]) {
						// Move past LWS
						offsetSpace1++; // Adjust for colon
						offsetSpace1 += adjustSP(msg, offsetSpace1); // Adjust for one or more white-space
						int offsetSpace2 = msg.indexOf(" ", offsetSpace1);
						// Value is the third field after the header
						if (offsetSpace2 != -1 && offsetSpace2 < location[1]) {
							offsetSpace2++; // Adjust for colon
							offsetSpace2 += adjustSP(msg, offsetSpace2); // Adjust for one or more white-space
							int offsetSpace3 = msg.indexOf(" ", offsetSpace2);
							// Value is the third field after the header
							if (offsetSpace3 != -1 && offsetSpace3 < location[1]) {
								paramLocation[0] = offsetSpace1;
								paramLocation[1] = offsetSpace1;
								paramLocation[2] = offsetSpace2;
								paramLocation[3] = VALUE_PARAM;
							}
							else {
								int newline = msg.indexOf("\r\n", offsetSpace2);
								if (newline != -1 && newline <= location[1]) {
									paramLocation[0] = offsetSpace2;
									paramLocation[1] = offsetSpace2;
									paramLocation[2] = location[1];
									paramLocation[3] = VALUE_PARAM;
								}
							}
						}
					}
				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter protocol within a 
	 * Reason Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateReasonParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Reason has the following format for fixed position parameters
			// Reason: reason-value *(COMMA reason-value)
			// reason-value      =  protocol *(SEMI reason-params)
			// protocol          =  "SIP" / "Q.850" / token
			// reason-params     =  protocol-cause / reason-text / reason-extension
			// protocol-cause    =  "cause" EQUAL cause
			// cause             =  1*DIGIT
			// reason-text       =  "text" EQUAL quoted-string
			// reason-extension  =  generic-param
			if (param.equals("protocol")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameters rec-route, name-addr, addr-spec and lr within a 
	 * Record-Route Header. It will also return generic parameters.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateRecordRouteParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Record-Route has the following format for fixed position parameters
			// Record-Route: rec-route *(COMMA rec-route)
			// rec-route = name-addr *( SEMI rr-param )
			if (param.equals("sr-value")) 
				// sr-value is everything between the first space and either a comma
				// or CRLF
				return locateFirstPositionParam(hdr, param, location, msg, ",");
			else if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals(ADDR_SPEC))		
				return locateAddrSpec(param, location, msg);
			else if (param.equals("lr")) 
				return locatePresenceParam(param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter name-addr and addr-spec within a 
	 * Referred-By Header. It will also return generic parameters:
	 * any other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateReferredByParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Referred-By has the following format for fixed position parameters
			// Referred-By: referrer-uri *( SEMI (referredby-id-param / generic-param) )
			// referrer-uri = ( name-addr / addr-spec )
			// referredby-id-param = "cid" EQUAL sip-clean-msg-id
			// sip-clean-msg-id = LDQUOT dot-atom "@" (dot-atom / host) RDQUOT
			// dot-atom = atom *( "." atom )
			// atom = 1*( alphanum / "-" / "!" / "%" / "*" /
			//         "_" / "+" / "'" / "`" / "~"   )
			if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals(ADDR_SPEC)) 
				return locateAddrSpec(param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter name-addr and addr-spec within a 
	 * Refer-To Header. It will also return generic parameters:
	 * any other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateReferToParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Refer-To has the following format for fixed position parameters
			// Refer-To: ( name-addr / addr-spec ) *(SEMI refer-param)
			// refer-param = generic-param / feature-param
			if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals(ADDR_SPEC)) 
				return locateAddrSpec(param, location, msg);
			else if (param.equals("Replaces")) {
				paramLocation = locateGenericParam(param, location, msg, "", ">");
				// Before we get the tag, make sure there isn't another header following
				// the Replaces header
				int amp = msg.indexOf("&", paramLocation[1]);
				if (amp != -1 && amp < paramLocation[2]) {
					paramLocation[2] = amp;
				}
				if (paramLocation[0] != -1 && 
						paramLocation[1] != -1 && 
						paramLocation[2] != -1) {
					int hex = msg.indexOf("%3B", paramLocation[1]);
					if (hex == -1 && hex < paramLocation[2]) 
						hex = msg.indexOf("%3b", paramLocation[1]);
					if (hex != -1 && 
							hex > paramLocation[1] && hex < paramLocation[2]) 
						paramLocation[2] = hex;
				}
			}
			else if (param.equals("from-tag")) {
				paramLocation = locateGenericParam("Replaces", location, msg, "", ">");
				if (paramLocation[0] != -1 && 
						paramLocation[1] != -1 && 
						paramLocation[2] != -1) {
					// Before we get the tag, make sure there isn't another header following
					// the Replaces header
					int amp = msg.indexOf("&", paramLocation[1]);
					if (amp != -1 && amp < paramLocation[2]) {
						paramLocation[2] = amp;
					}
					String tag = "from-tag%3D";
					int hex = msg.indexOf(tag, paramLocation[1]);
					if (hex == -1 && hex < paramLocation[2]) {
						tag = tag.replaceFirst("D", "d");
						hex = msg.indexOf(tag, paramLocation[1]);
					}	
					if (hex != -1 &&
							hex > paramLocation[1] &&
							hex < paramLocation[2]) {
						paramLocation[0] = hex;
						paramLocation[1] = hex+tag.length();
					}
					hex = msg.indexOf("%3B", paramLocation[1]);
					if (hex == -1 && hex < paramLocation[2]) 
						hex = msg.indexOf("%3b", paramLocation[1]);
					if (hex == -1 && hex < paramLocation[2]) 
						hex = msg.indexOf(">", paramLocation[1]);
					if (hex != -1 && 
							hex > paramLocation[1] && hex < paramLocation[2]) 
						paramLocation[2] = hex;
				}
			}
			else if (param.equals("to-tag")) {
				paramLocation = locateGenericParam("Replaces", location, msg, "", ">");
				// Before we get the tag, make sure there isn't another header following
				// the Replaces header
				int amp = msg.indexOf("&", paramLocation[1]);
				if (amp != -1 && amp < paramLocation[2]) {
					paramLocation[2] = amp;
				}
				if (paramLocation[0] != -1 && 
						paramLocation[1] != -1 && 
						paramLocation[2] != -1) {
					String tag = "to-tag%3D";
					int hex = msg.indexOf(tag, paramLocation[1]);
					if (hex == -1 && hex < paramLocation[2]) {
						tag = tag.replaceFirst("D", "d");
						hex = msg.indexOf(tag, paramLocation[1]);
					}	
					if (hex != -1 &&
							hex > paramLocation[1] &&
							hex < paramLocation[2]) {
						paramLocation[0] = hex;
						paramLocation[1] = hex+tag.length();
					}
					hex = msg.indexOf("%3B", paramLocation[1]);
					if (hex == -1 && hex < paramLocation[2]) 
						hex = msg.indexOf("%3b", paramLocation[1]);
					if (hex == -1 && hex < paramLocation[2]) 
						hex = msg.indexOf(">", paramLocation[1]);
					if (hex != -1 && 
							hex > paramLocation[1] && hex < paramLocation[2]) 
						paramLocation[2] = hex;
				}
			}
			else if (param.equals("uri")) {
				paramLocation = locateNameAddr(hdr, param, location, msg);
				if (paramLocation[0] != -1 && 
						paramLocation[1] != -1 && 
						paramLocation[2] != -1) {
					int semi = msg.indexOf("?", paramLocation[1]);
					//int question = msg.indexOf("?", paramLocation[1]);
					if (semi != -1 &&
							semi < paramLocation[2]) {
						if (msg.charAt(paramLocation[1]) == '<') {
							paramLocation[0]++;
							paramLocation[1]++;
						}
						paramLocation[2] = semi;
					}
				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter rc-value within a 
	 * Reject-Contact Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateRejectContactParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			// Accept-Contact has the following format for fixed position parameters
			// Accept-Contact: rc-value *(COMMA rc-value)
			// rc-value = "*" *(SEMI rc-params)
			// rc-params       =  feature-param / generic-param
			//           ;;feature param from RFC 3840
			//           ;;generic-param from RFC 3261
			if (param.equals("rc-value")) 
				return locatePresenceParam(param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameters callid and early-only within a 
	 * Replaces Header. It will also return generic parameters:
	 * 	to-tag, from-tag and any other generic-param
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateReplacesParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Replaces has the following format for fixed position parameters
			// Replaces: callid *(SEMI replaces-param)
			// replaces-param  = to-tag / from-tag / early-flag / generic-param
			//  early-flag = "early-only"
			if (param.equals("callid")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else if (param.equals("early-only")) {
				return locatePresenceParam(param, location, msg);
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter directive within a 
	 * Request-Disposition Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateRequestDispositionParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			// Request-Disposition has the following format for fixed position parameters
			// Request-Disposition: directive *(COMMA directive)
			if (param.equals("directive")) 
				return locateListParam(hdr, param, location, msg, ",");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter Request-URI within the 
	 * RequestLine. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateRequestLineParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			if (param.equals("Request-URI")) {
				// Request-URI begins after space following the method 
				int offset = msg.indexOf(" ", location[0]);
				if (offset != -1 && offset < location[1]) {
					// Adjust the beginning offset for the space
					offset += 1;
					int offsetSpace = msg.indexOf(" ", offset);
					// Request-URI ends at next space 
					if (offsetSpace != -1 && offsetSpace < location[1]) {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = offsetSpace;
						paramLocation[3] = VALUE_PARAM;
					}
					else {
						int newline = msg.indexOf("\r\n");
						if (newline != -1 && newline < location[1]) {
							paramLocation[0] = offset;
							paramLocation[1] = offset;
							paramLocation[2] = location[1];
							paramLocation[3] = VALUE_PARAM;
						}
					}
				}
			}
			else if (param.equals("username")) {
				// For the user name we need to find the : after the 
				// URI type and it includes everything up to the @
				int offset = msg.indexOf(" ", location[0]);
				if (offset != -1 && offset < location[1]) {
					// Adjust the beginning offset for the space
					offset += 1;
					int offsetType = msg.indexOf(":", offset);
					if (offsetType != -1 && offsetType < location[1]) {
						// Adjust for the colon
						offsetType += 1;
						int offsetAmp = msg.indexOf("@", offsetType);
						if (offsetAmp != -1 && offsetAmp < location[1]) {
							paramLocation[0] = offsetType;
							paramLocation[1] = offsetType;
							paramLocation[2] = offsetAmp;
							paramLocation[3] = VALUE_PARAM;
						}
					}
				}
			}
			else if (param.equals("domain")) {
				// For the domain, the value lies between the @ of the
				// URI up to either the : of the port value, a ; for
				// a parameter or the > marking the end of the URI
				// whichever index is the lowest
				return locateDomain(param, location, msg);
//				int offset = msg.indexOf(" ", location[0]);
//				if (offset != -1 && offset < location[1]) {
//					// Adjust the beginning offset for the space
//					offset += 1;
//					int offsetAmp = msg.indexOf("@", offset);
//					if (offsetAmp != -1 && offsetAmp < location[1]) {
//						// Adjust the beginning offset for the ampersand
//						offsetAmp += 1;
//						int offsetColon = msg.indexOf(":", offsetAmp);
//						int offsetSemi = msg.indexOf(";", offsetAmp);
//						int offsetGT = msg.indexOf(">", offsetAmp);
//						int offsetSpace = msg.indexOf(" ", offsetAmp);
//						if (offsetColon != -1 && offsetColon < location[1] &&
//								(offsetSemi == -1 || offsetColon < offsetSemi) &&
//								(offsetGT == -1 || offsetColon < offsetGT)) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetColon;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetSemi != -1 && offsetSemi < location[1] &&
//								(offsetGT == -1 || offsetSemi < offsetGT)) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetSemi;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetGT != -1 && offsetGT < location[1]) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetGT;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetSpace != -1 && offsetSpace < location[1]) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetSpace;
//							paramLocation[3] = VALUE_PARAM;
//						}
//					}
//				}
			}
			else if (param.equals("port")) {
				return locatePort(param, location, msg, "@", " ");
			}
			else if (param.equals("phone-number")) {
				// For the user name we need to find the : after the 
				// URI type and it includes everything up to the @
				int offset = msg.indexOf(" ", location[0]);
				if (offset != -1 && offset < location[1]) {
					// Adjust the beginning offset for the space
					offset += 1;
					int offsetType = msg.indexOf("tel:+", offset);
					if (offsetType != -1 && offsetType < location[1]) {
						// Adjust for the tel, colon, & +
						offsetType += 5;
						int offsetSemi = msg.indexOf(";", offsetType);
						int offsetSpace = msg.indexOf(" ", offsetType);
						if (offsetSemi != -1 && offsetSemi < location[1]) {
							paramLocation[0] = offsetType;
							paramLocation[1] = offsetType;
							paramLocation[2] = offsetSemi;
							paramLocation[3] = VALUE_PARAM;
						}
						else if (offsetSpace != -1 && offsetSpace < location[1]){
							paramLocation[0] = offsetType;
							paramLocation[1] = offsetType;
							paramLocation[2] = offsetSpace;
							paramLocation[3] = VALUE_PARAM;
						}
						else {
							paramLocation[0] = offsetType;
							paramLocation[1] = offsetType;
							paramLocation[2] = location[1];
							paramLocation[3] = VALUE_PARAM;
						}
					}
				}
			}
			else if (param.equals("method")) {
				int offset = msg.indexOf(" ", location[0]);
				if (offset != -1) {
					paramLocation[0] = location[0];
					paramLocation[1] = location[0];
					paramLocation[2] = offset;
					paramLocation[3] = VALUE_PARAM;
				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter option-tag within a 
	 * Require Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateRequireParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			// Require has the following format for fixed position parameters
			// Require: option-tag *(COMMA option-tag)
			if (param.equals("option-tag")) {
				return locateListParam(hdr, param, location, msg, ",");

			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameters value and comment within a 
	 * Retry-After Header. It will also return generic parameters.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateRetryAfterParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Retry-After has the following format for fixed position parameters
			// Retry-After: delta-seconds [ comment ] *( SEMI retry-param )
			if (param.equals("value")) {
				int [] semiLocation = locateFirstPositionParam(hdr, param, location, msg, ";");
				int [] spaceLocation = locateFirstPositionParam(hdr, param, location, msg, " ");
				if (semiLocation[0] != -1 && semiLocation[1] != -1) {
					if (spaceLocation[0] != -1 && spaceLocation[1] != -1 &&
							spaceLocation[1] < semiLocation[1]) {
						return spaceLocation;
					}
					else
						return semiLocation;
				}
			}
			else if (param.equals("comment")) {
				int offset = -1;
				String temp = msg.substring(location[0], location[1]);
				boolean adjust = false;
				if (temp.contains(hdr)) {
					offset = msg.indexOf(":", location[0]);
					adjust = true;
				}
				else 
					offset = location[0];
				if (offset != -1 && offset < location[1]) {
					if (adjust) {
						// Adjust the beginning offset for the colon and space
						offset++; // Adjust for colon
						offset += adjustSP(msg, offset); // Adjust for one or more white-space
					}
					int offsetSpace = msg.indexOf(" ", offset);
					// Value is the second field after the header if it exists
					if (offsetSpace != -1 && offsetSpace < location[1]) {
						offsetSpace += adjustSP(msg, offsetSpace); // Adjust for one or more white-space
						int offsetSemi = msg.indexOf(";", offsetSpace);
						if (offsetSemi != -1 && offsetSemi < location[1]) {
							paramLocation[0] = offsetSpace;
							paramLocation[1] = offsetSpace;
							paramLocation[2] = offsetSemi;
							paramLocation[3] = VALUE_PARAM;
						}
						else {
							int newline = msg.indexOf("\r\n");
							if (newline != -1 && newline < location[1]) {
								paramLocation[0] = offsetSpace;
								paramLocation[1] = offsetSpace;
								paramLocation[2] = location[1];
								paramLocation[3] = VALUE_PARAM;
							}
						}
					}
				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}




	/**
	 * Locates the positional parameters route-param, name-addr, addr-spec or lr within a 
	 * Route Header. It will also return generic parameters.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateRouteParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Route has the following format for fixed position parameters
			// Route: route-param *(COMMA route-param)
			// route-param  =  name-addr *( SEMI rr-param )
			if (param.equals("route-param")) 
				// sr-value is everything between the first space and either a comma
				// or CRLF
				return locateFirstPositionParam(hdr, param, location, msg, ",");
			else if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals(ADDR_SPEC))		
				return locateAddrSpec(param, location, msg);
			else if (param.equals("lr"))
				return locatePresenceParam(param, location, msg);
			else if (param.equals("domain")) {
				// For the domain, the value lies between the @ of the
				// URI up to either the : of the port value, a ; for
				// a parameter or the > marking the end of the URI
				// whichever index is the lowest
				return locateDomain(param, location, msg);
//				int offset = msg.indexOf(" ", location[0]);
//				if (offset != -1 && offset < location[1]) {
//					// Adjust the beginning offset for the space
//					offset += 1;
//					int offsetAmp = msg.indexOf("@", offset);
//					if (offsetAmp != -1 && offsetAmp < location[1]) {
//						// Adjust the beginning offset for the ampersand
//						offsetAmp += 1;
//						int offsetColon = msg.indexOf(":", offsetAmp);
//						int offsetSemi = msg.indexOf(";", offsetAmp);
//						int offsetGT = msg.indexOf(">", offsetAmp);
//						int offsetSpace = msg.indexOf(" ", offsetAmp);
//						if (offsetColon != -1 && offsetColon < location[1] &&
//								(offsetSemi == -1 || offsetColon < offsetSemi) &&
//								(offsetGT == -1 || offsetColon < offsetGT)) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetColon;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetSemi != -1 && offsetSemi < location[1] &&
//								(offsetGT == -1 || offsetSemi < offsetGT)) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetSemi;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetGT != -1 && offsetGT < location[1]) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetGT;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetSpace != -1 && offsetSpace < location[1]) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetSpace;
//							paramLocation[3] = VALUE_PARAM;
//						}
//					}
//				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter response-num within a 
	 * RSeq Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateRSeqParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// RSeq has the following format for fixed position parameters
			// RSeq: response-num 
			if (param.equals("response-num")) 
				return locateFirstPositionParam(hdr, param, location, msg, " ");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter mechanism within an 
	 * Security-Client Header. It will also return generic parameters:
	 * 	id and other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateSecurityParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// security-client  = "Security-Client" HCOLON sec-mechanism *(COMMA sec-mechanism)
			// security-server  = "Security-Server" HCOLON sec-mechanism *(COMMA sec-mechanism)
		    // security-verify  = "Security-Verify" HCOLON sec-mechanism *(COMMA sec-mechanism)
            // 
			// sec-mechanism    = mechanism-name *(SEMI mech-parameters)
			// mechanism-name   = ( "digest" / "tls" / "ipsec-ike" / "ipsec-man" / token )
			// mech-parameters  = ( preference / digest-algorithm / digest-qop / digest-verify / extension )
			// preference       = "q" EQUAL qvalue
			// qvalue           = ( "0" [ "." 0*3DIGIT ] ) / ( "1" [ "." 0*3("0") ] )
			// digest-algorithm = "d-alg" EQUAL token
			// digest-qop       = "d-qop" EQUAL token
			// digest-verify    = "d-ver" EQUAL LDQUOT 32LHEX RDQUOT
			// extension        = generic-param
			if (param.equals("mechanism")) {
				return locateFirstPositionParam(hdr, param, location, msg, " ");
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameters sr-value name-addr, addr-spec or lr within a 
	 * Service-Route Header. It will also return generic parameters.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateServiceRouteParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Service-Route has the following format for fixed position parameters
			// Service-Route: sr-value *( COMMA sr-value)
			// sr-value = name-addr *( SEMI generic-param )
			if (param.equals("sr-value")) 
				// sr-value is everything between the first space and either a comma
				// or CRLF
				return locateFirstPositionParam(hdr, param, location, msg, ",");
			else if (param.equals(NAME_ADDR)) 
				return locateNameAddr(hdr, param, location, msg);
			else if (param.equals(ADDR_SPEC))		
				return locateAddrSpec(param, location, msg);
			else if (param.equals("lr"))
				return locatePresenceParam(param, location, msg);
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}





	/**
	 * Locates the positional parameter entity-tag within a 
	 * SIP-ETag Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateSipETagParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// SIP-ETag has the following format for fixed position parameters
			// SIP-ETag: entity-tag
			if (param.equals("entity-tag")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter entity-tag within a 
	 * SIP-If-Match Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateSipIfMatchParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// SIP-If-Match has the following format for fixed position parameters
			// SIP-If-Match: entity-tag
			if (param.equals("entity-tag")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameters version, status-code, and reason-phrase within the Status-Line. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateStatusLineParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Status-Line has the following format for fixed position parameters
			// Status-Line  =  SIP-Version SP Status-Code SP Reason-Phrase CRLF
			if (param.equals("version")) {
				int offset = msg.indexOf(" ", location[0]);
				if (offset != -1 && offset < location[1]) {
					// Version is the first field 
					paramLocation[0] = location[0];
					paramLocation[1] = location[0];
					paramLocation[2] = offset;
					paramLocation[3] = VALUE_PARAM;
				}
			}
			else if (param.equals("status-code")) {
				int offset = msg.indexOf(" ", location[0]);
				if (offset != -1 && offset < location[1]) {
					offset += adjustSP(msg, offset); // Adjust for one or more white-space
					int offsetSpace = msg.indexOf(" ", offset);
					// Value is the second field after the header
					if (offsetSpace != -1 && offsetSpace < location[1]) {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = offsetSpace;
						paramLocation[3] = VALUE_PARAM;
					}
				}
			}
			else if (param.equals("reason-phrase")) {
				int offset = msg.indexOf(" ", location[0]);
				if (offset != -1 && offset < location[1]) {
					// Adjust the beginning offset for the colon and space
					offset++; // Adjust for colon
					offset += adjustSP(msg, offset); // Adjust for one or more white-space
					int offsetSpace = msg.indexOf(" ", offset);
					// Value is the second field after the header
					if (offsetSpace != -1 && offsetSpace < location[1]) {
						offsetSpace += adjustSP(msg, offsetSpace);
						int offsetCRLF = msg.indexOf("\r\n", offsetSpace);
						if (offsetCRLF != -1 && offsetCRLF <= location[1]) {
							paramLocation[0] = offsetSpace;
							paramLocation[1] = offsetSpace;
							paramLocation[2] = offsetCRLF;
							paramLocation[3] = VALUE_PARAM;
						}
					}
				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter substate-value within a 
	 * Subscription-State Header. It will also return generic parameters:
	 * 	reason, expires, retry-after and other generic parameter
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateSubscriptionStateParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Subscription-State has the following format for fixed position parameters
			// Subscription-State: substate-value *( SEMI subexp-params )
			// substate-value = "active" / "pending" / "terminated" / token
			if (param.equals("substate-value")) {
				return locateFirstPositionParam(hdr, param, location, msg, ";");
//				int offset = msg.indexOf(":", location[0]);
//				if (offset != -1 && offset < location[1]) {
//					// Adjust the beginning offset for the colon and space
//					offset++; // Adjust for colon
//					offset += adjustSP(msg, offset); // Adjust for one or more white-space
//					int offsetSemi = msg.indexOf(";", offset);
//					// Value is the first field after the header
//					if (offsetSemi != -1 && offsetSemi < location[1]) {
//						paramLocation[0] = offset;
//						paramLocation[1] = offset;
//						paramLocation[2] = offsetSemi;
//						paramLocation[3] = VALUE_PARAM;
//					}
//					else {
//						int newline = msg.indexOf("\r\n");
//						if (newline != -1 && newline < location[1]) {
//							paramLocation[0] = offset;
//							paramLocation[1] = offset;
//							paramLocation[2] = location[1];
//							paramLocation[3] = VALUE_PARAM;
//						}
//					}
//				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter option-tag within an
	 * Supported Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateSupportedParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			if (param.equals("option-tag")) {
				return locateListParam(hdr, param, location, msg, ",");
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter value within a 
	 * Target-Dialog Header.
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateTargetDialogParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			if (param.equals("Call-ID")) 
				return locateFirstPositionParam(hdr, param, location, msg, ";");
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	
	/**
	 * Locates the positional parameter value within a 
	 * Timestamp Header. It will also return generic parameters:
	 * 	id and other generic parameter
	 * 
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateTimestampParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Event has the following format for fixed position parameters
			// Event: event-type *( SEMI event-params )
			// event-type        =  event-package *( "." event-template )
			// event-package     =  token-nodot
			// event-template    =  token-nodot
			// event-param       =  generic-param / ( "id" EQUAL token )
			if (param.equals("value")) {
				int offset = -1;
				String temp = msg.substring(location[0], location[1]);
				boolean adjust = false;
				if (temp.contains(hdr)) {
					offset = msg.indexOf(":", location[0]);
					adjust = true;
				}
				else 
					offset = location[0];
				if (offset != -1 && offset < location[1]) {
					if (adjust) {
						// Adjust the beginning offset for the colon and space
						offset++; // Adjust for colon
						offset += adjustSP(msg, offset); // Adjust for one or more white-space
					}
					int offsetSemi = msg.indexOf(";", offset);
					// Value is the first field after the header
					if (offsetSemi != -1 && offsetSemi < location[1]) {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = offsetSemi;
						paramLocation[3] = VALUE_PARAM;
					}
					else {
						int offsetSpace = msg.indexOf(" ", offset);
						if (offsetSpace != -1 && offsetSpace < location[1]) {
							paramLocation[0] = offset;
							paramLocation[1] = offset;
							paramLocation[2] = offsetSpace;
							paramLocation[3] = VALUE_PARAM;
						}
						else {
							int newline = msg.indexOf("\r\n");
							if (newline != -1 && newline < location[1]) {
								paramLocation[0] = offset;
								paramLocation[1] = offset;
								paramLocation[2] = location[1];
								paramLocation[3] = VALUE_PARAM;
							}
						}
					}
				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates the positional parameter name-addr, addr-spec or lr within a To Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateToParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// To has the following format for fixed position parameters
			// To: [ display-name ] LAQUOT addr-spec RAQUOT *( SEMI tag-param / generic-param)
			if (param.equals(NAME_ADDR)) {
				return locateNameAddr(hdr, param, location, msg);
			}
			else if (param.equals(ADDR_SPEC)) {
				return locateAddrSpec(param, location, msg);
			}
			else if (param.equals("lr")) {
				return locatePresenceParam(param, location, msg);
			}
			else if (param.equals("domain")) {
				// For the domain, the value lies between the @ of the
				// URI up to either the : of the port value, a ; for
				// a parameter or the > marking the end of the URI
				// whichever index is the lowest
				return locateDomain(param, location, msg);
//				int offset = msg.indexOf(" ", location[0]);
//				if (offset != -1 && offset < location[1]) {
//					// Adjust the beginning offset for the space
//					offset += 1;
//					int offsetAmp = msg.indexOf("@", offset);
//					if (offsetAmp != -1 && offsetAmp < location[1]) {
//						// Adjust the beginning offset for the ampersand
//						offsetAmp += 1;
//						int offsetColon = msg.indexOf(":", offsetAmp);
//						int offsetSemi = msg.indexOf(";", offsetAmp);
//						int offsetGT = msg.indexOf(">", offsetAmp);
//						int offsetSpace = msg.indexOf(" ", offsetAmp);
//						if (offsetColon != -1 && offsetColon < location[1] &&
//								(offsetSemi == -1 || offsetColon < offsetSemi) &&
//								(offsetGT == -1 || offsetColon < offsetGT)) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetColon;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetSemi != -1 && offsetSemi < location[1] &&
//								(offsetGT == -1 || offsetSemi < offsetGT)) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetSemi;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetGT != -1 && offsetGT < location[1]) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetGT;
//							paramLocation[3] = VALUE_PARAM;
//						}
//						else if (offsetSpace != -1 && offsetSpace < location[1]) {
//							paramLocation[0] = offsetAmp;
//							paramLocation[1] = offsetAmp;
//							paramLocation[2] = offsetSpace;
//							paramLocation[3] = VALUE_PARAM;
//						}
//					}
//				}
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameter option-tag within an
	 * Unsupported Header. 
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter.
	 */
	private int [] locateUnsupportedParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initValueLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1 && location[2] != NO_COMMA) {
			// Unsupported has the following format for fixed position parameters
			// Unsupported: option-tag *(COMMA option-tag)
			if (param.equals("option-tag")) {
				return locateListParam(hdr, param, location, msg, ",");
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}

	/**
	 * Locates the positional parameters sent-protocol and sent-by 
	 * within a Via Header. It will also return generic parameters:
	 * 	ttl, maddr, received, branch, rport
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateViaParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			// Via has the following format for fixed position parameters
			// Via: sent-protocol sent-by
			if (param.equals("sent-protocol")) {
				int offset = -1;
				String temp = msg.substring(location[0], location[1]);
				boolean adjust = false;
				if (temp.contains(hdr)) {
					offset = msg.indexOf(":", location[0]);
					adjust = true;
				}
				else 
					offset = location[0];
				if (offset != -1 && offset < location[1]) {
					if (adjust) {
						// Adjust the beginning offset for the colon and space
						offset++; // Adjust for colon
						offset += adjustSP(msg, offset); // Adjust for one or more white-space
					}
					int offsetSpace = msg.indexOf(" ", offset);
					// Value is the first field after the header
					if (offsetSpace != -1 && offsetSpace < location[1]) {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = offsetSpace;
						paramLocation[3] = VALUE_PARAM;
					}
				}
			}
			else if (param.equals("sent-by")) {
				int offset = -1;
				String temp = msg.substring(location[0], location[1]);
				boolean adjust = false;
				if (temp.contains(hdr)) {
					offset = msg.indexOf(":", location[0]);
					adjust = true;
				}
				else 
					offset = location[0];
				
				if (offset != -1 && offset < location[1]) {
					if (adjust) {
						// Adjust the beginning offset for the colon, space, sent-protocol and space
						offset++; // Adjust for colon
						offset += adjustSP(msg, offset); // Adjust for one or more white-space
					}
					offset = msg.indexOf(" ", offset);
					// Adjust the beginning offset for the colon and space
					offset += adjustSP(msg, offset); // Adjust for one or more white-space
					int offsetSemi = msg.indexOf(";", offset);
					// Value is the first field after the header
					if (offsetSemi != -1 && offsetSemi < location[1]) {
						// Adjust ending index for the '>' character.
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = offsetSemi;
						paramLocation[3] = VALUE_PARAM;
					}
					else {
						paramLocation[0] = offset;
						paramLocation[1] = offset;
						paramLocation[2] = location[1];
						paramLocation[3] = VALUE_PARAM;
					}
				}
			}
			else if (param.equals("port")) {
				return locatePort(param, location, msg, " ", null);
			}
			else 
				return locatePresenceParam(param, location, msg);
		}
		return paramLocation;
	}
	/**
	 * Locates only generic parameters within a WWW-Authenticate Header. 
	 * realm, doamin, algorithm, nonce, qop, and opaque are all generic
	 * parameters
	 * 
	 * @param hdr - the header that the parameter is located in.
	 * @param param - the parameter to search for in the message.
	 * @param location - the beginning and ending indexes to search within the message.
	 * @param msg - the complete message
	 * @return int [] - returns the beginning and ending index of the parameter
	 * as well as the terminating delimiter type.
	 */
	private int [] locateWWWAuthenticateParam(String hdr, String param, 
			int [] location, String msg) {
		int [] paramLocation = initGenericLocation();
		
		// As a precaution make sure that we have a value for the header
		if (location[0] != -1 && location[1] != -1) {
			paramLocation = locateGenericParam(param, location, msg, ",");
			// If we fail to find the parameter, we need to check to see if 
			// it is the first parameter as this one will not start with a comma
			if (paramLocation[0] == -1 &&
					paramLocation[1] == -1 &&
					paramLocation[2] == -1) {
				paramLocation = locateGenericParam(param, location, msg, " ", ",");
			}
			
			if (isQuoted(paramLocation, msg)) {
				paramLocation[1]++;
				paramLocation[2]--;
			}
		}
		return paramLocation;
	}
	
	/**
	 * This method tests an array to make sure that it is valid. In other
	 * words, it makes sure that the none of elements are not set to a
	 * -1. It also verifies that the lenght of the array is 4.
	 * @param paramLocation
	 * @return
	 */
	public static boolean validParamLocation(int [] paramLocation) {
		if (paramLocation.length == 4 && 
				paramLocation[0] != -1 &&
				paramLocation[1] != -1 &&
				paramLocation[2] != -1 &&
				paramLocation[3] != -1)
			return true;
		return false;
	}

	/**
	 * This method tries to calculate the number of white spaces between the 
	 * beginning index and the next parameter.
	 * 
	 * @param msg
	 * @param beginIndex
	 * @return
	 */
	private int adjustSP(String msg, int beginIndex) {
		int index = beginIndex;
		boolean done = false;
		while (!done && index > -1) {
			if (msg.charAt(index) == ' ' ||
					msg.charAt(index) == '\t' ||
					msg.charAt(index) == '\n') {
				index++;
			}
			else
				done = true;
		}
		return (index - beginIndex);
	}

}
