/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.models;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.sip.header.AuthenticationInfoHeader;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Request;
import javax.sip.message.Response;

import com.cablelabs.common.Conversion;
import com.cablelabs.common.DateUtils;
import com.cablelabs.fsm.CaptureRef;
import com.cablelabs.fsm.ComparisonEvaluator;
import com.cablelabs.fsm.CurStateRef;
import com.cablelabs.fsm.EventRef;
import com.cablelabs.fsm.Extension;
import com.cablelabs.fsm.ExtensionRef;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.GlobalVariables;
import com.cablelabs.fsm.InternalMsg;
import com.cablelabs.fsm.Literal;
import com.cablelabs.fsm.MsgEvent;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.MsgRef;
import com.cablelabs.fsm.PlatformRef;
import com.cablelabs.fsm.RTPMsg;
import com.cablelabs.fsm.RTPRef;
import com.cablelabs.fsm.Reference;
import com.cablelabs.fsm.SDPConstants;
import com.cablelabs.fsm.SDPRef;
import com.cablelabs.fsm.SIPBodyRef;
import com.cablelabs.fsm.SIPConstants;
import com.cablelabs.fsm.SIPMsg;
import com.cablelabs.fsm.SIPRef;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.StunMsg;
import com.cablelabs.fsm.StunRef;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.fsm.UtilityMsg;
import com.cablelabs.fsm.UtilityRef;
import com.cablelabs.fsm.VarRef;
import com.cablelabs.fsm.Variable;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.sim.PCSim2;
import com.cablelabs.stun.StunConstants;
import com.cablelabs.stun.StunMessage;
import com.cablelabs.stun.attributes.FingerPrint;
import com.cablelabs.tools.CaptureLocator;
import com.cablelabs.tools.PacketDatabase;
import com.cablelabs.tools.RefLocator;
import com.cablelabs.tools.SDPLocator;
import com.cablelabs.tools.SIPLocator;
import com.cablelabs.tools.STUNLocator;
import com.cablelabs.tools.UtilityLocator;



/**
 * This class implements the comparison operation defined within the
 * PC2.0 Test Script XML Documents. It allows for some abstraction
 * between the original XML Document and the implementaion of comparing
 * specific protocol information. 
 * 
 * It gathers the various message reference material from the queue or 
 * configruation parameters and performs the operation upon the data.
 * 
 * @author ghassler
 *
 */
public class Examiner implements ComparisonEvaluator {

	/**
	 * A reference to the global message queue.
	 */
	private MsgQueue q = null;

	/**
	 * The unique id of the FSM that created me and that
	 * we are performing our operation on behalf.
	 */
	private int fsmUID = 0;
	
	/**
	 * The FSM that the class is serving
	 */
	private FSM fsm = null;

	/**
	 * Logger
	 */
	private LogAPI logger = LogAPI.getInstance();

	/**
	 * Digester for authentication processing
	 */
	private MessageDigest digester = null;

	/**
	 * Local reference to the SIPLocator for modifying 
	 * the SIP portion of messages.
	 */
	private SIPLocator sipLocator = SIPLocator.getInstance();

	/**
	 * Local reference to the SIPLocator for modifying 
	 * the SIP portion of messages.
	 */
	private SDPLocator sdpLocator = SDPLocator.getInstance();

	/**
	 * Local reference to the UtilityLocator for obtaining 
	 * the Utility tag value's of a message.
	 */
	private UtilityLocator utilLocator = UtilityLocator.getInstance();
	
	private RefLocator refLocator = RefLocator.getInstance();

	private STUNLocator stunLocator = STUNLocator.getInstance();
	private CaptureLocator capLocator = CaptureLocator.getInstance();
	
	String logLabel = "Examiner";
	
	/**
	 * The subcategory to use when logging. This is the same as the
	 * name of the FSM that it is operating for.
	 * 
	 */
	private String subCat = null;
	
	private final static String LAQUOT = "<";
	private final static String RAQUOT = ">";
	/**
	 * Constructor
	 * @param fsm - the FSM that we're operating for.
	 */
	public Examiner(FSM fsm) {
		this.q = PCSim2.getMsgQueue();
		this.fsmUID = fsm.getUID();
		this.fsm = fsm;
		this.subCat = fsm.getSubcategory();
		try {
			digester = MessageDigest.getInstance("MD5");
		}
		catch (NoSuchAlgorithmException ex) {
			logger.warn(PC2LogCategory.Examiner, subCat,
					"Could not obtain MD5 Message Digest for Examiner.", ex);
		}
	}

	/** 
	 * Performs the comparison operation. It first obtains the left and right
	 * hand operand and then performs the operation defined by the operator 
	 * parameter.
	 * 
	 * @param operator - the operation to perform on the operand
	 * @param left - the left-hand operand
	 * @param right - the right-hand operand
	 * @param event - the current event.
	 * @param ignoreCase - whether the comparison operater is defined to ignore case
	 * @param dateFormat - a string defing the date format to use in an isDate op
	 * @return - true if the operation is valid, false otherwise.
	 */
	@Override
    public boolean evaluate(Object source, String operator, Reference left, 
			Reference right, MsgEvent event, Boolean ignoreCase, String dateFormat) {
		boolean result = false;
		LinkedList<String> leftSide = null;
		LinkedList<String> rightSide = null;
		boolean rightCaseSensitive = true;
		boolean leftCaseSensitive = true;
		boolean ic = false;

		boolean leftAny = false;
		boolean rightAny = false;
				
		if (ignoreCase != null)
			ic = ignoreCase;
		try {
			// First check whether the operator is digest or not
			// this is because digest only operates on the event 
			// parameter.
			if (operator.equals("digest")) 	{
				result = digest(event);
			}
			else if (operator.equals("ipv4")) {
				leftCaseSensitive = caseSensitive(left);
				leftSide = findRef(left, event);
				if (leftSide.size() == 1) {
					result = isIPv4Address(leftSide.getFirst(), left);
				}
			}
			else if (operator.equals("ipv6")) {
				leftCaseSensitive = caseSensitive(left);
				leftSide = findRef(left, event);
				if (leftSide.size() == 1) {
					result = isIPv6Address(leftSide.getFirst(), left);
				}
			}
			else if (operator.equals("isDate")) {
			    leftSide = findRef(left, event);
			    if (leftSide.size() == 1) {
			    	result = isDate(leftSide.getFirst(), left, dateFormat);
			    }
			}
			else {
				// Next since we are not performing a digest operation,
				// decide the reference type for each side of the operator
				// and obtain the information that is referred to by the
				// reference for the left side
				// Test SDPRef before SIPRef since SDPRef derives from it
				try {
					leftCaseSensitive = caseSensitive(left);
					leftAny = isHdrInstanceAny(left);

					leftSide = findRef(left, event);
					// Next see if the reference was an add_ref or subtract_ref
					if (left instanceof MsgRef ) {
						if (((MsgRef)left).isArithmeticRef()) {
							leftSide = performArithmeticUpdate(leftSide, left);
						} else if (left instanceof CaptureRef && ((CaptureRef)left).getAdd() != null) {
							leftSide = performArithmeticUpdate(leftSide, (CaptureRef)left);
						}
//						MsgRef l = (MsgRef)left;
//						ListIterator<String> iter = leftSide.listIterator();
//						LinkedList<String> updatedList = new LinkedList<String>();
//						while (iter.hasNext()) {
//							String value = iter.next();
//							String sign = null;
//							if (l.isAddRef())
//								sign = " + ";
//							else if (l.isSubRef())
//								sign = " - ";
//							String logMsg = "Performing arithmetic change on value original=" 
//								+ value + sign + l.getArithmeticMod();
//							logger.info(PC2LogCategory.Examiner, subCat, logMsg);
//							value = l.updateArithmeticRef(value);
//							updatedList.add(value);
//						}
//						leftSide = updatedList;
					}

					// Obtain the information that is referred to by the
					// reference for the right side
					rightCaseSensitive = caseSensitive(right);
					rightAny = isHdrInstanceAny(right);
					rightSide = findRef(right, event);

//					Lastly see if the reference was an add_ref or subtract_ref
					if (right instanceof MsgRef) {
						if (((MsgRef)right).isArithmeticRef()) {
							rightSide = performArithmeticUpdate(rightSide, right);
						}
						else if (right instanceof CaptureRef && ((CaptureRef)right).getAdd() != null) {
							rightSide = performArithmeticUpdate(rightSide, (CaptureRef)right);
						}
//						MsgRef r = (MsgRef)right;
//						ListIterator<String> iter = rightSide.listIterator();
//						LinkedList<String> updatedList = new LinkedList<String>();
//						while (iter.hasNext()) {
//							String value = iter.next();
//							String sign = null;
//							if (r.isAddRef())
//								sign = " + ";
//							else if (r.isSubRef())
//								sign = " - ";
//							String logMsg = "Performing arithmetic change on value original=" 
//								+ value + sign + r.getArithmeticMod();
//							logger.info(PC2LogCategory.Examiner, subCat, logMsg);
//							value = r.updateArithmeticRef(value);
//							updatedList.add(value);
//						}
//						rightSide = updatedList;
					}
				}
				catch (OutOfMemoryError oom) {
					logger.error(PC2LogCategory.Examiner, subCat, 
							logLabel + " ran out of memory attempting to get reference for comparison operation.\n"
							+ left);
					return false;
				}
				boolean useCase = (leftCaseSensitive && rightCaseSensitive);
				if (ic)
					useCase = false;
				// Now that we have the data perform the operation
				if (leftSide != null) {
					if (operator.equals("eq")) {

						//	For the equals operator we should only have one
						// entry in the list for each side of the operator
						// unless one of the reference used the hdrInstance
						// "any" value
						if (rightSide != null) {
							if (leftSide.size() == 1 && rightSide.size() == 1) {
								result = isEqual(leftSide, left, rightSide, right, operator, useCase, false);
							}
							else if (leftAny || rightAny) {
								result = isAnyEqual(leftSide, left, rightSide, right, operator, useCase, false);
							}
							else {
								unevenEqual(leftSide, rightSide, operator, result);
							}
						}
						else {
							noValue(leftSide, rightSide, operator, result);
						}
					}

					else if (operator.equals("neq")) {
						//	For the not equals operator we should only have one
						// entry in the list for each side of the operator
						if (rightSide != null) {
							if (leftSide.size() == 1 && rightSide.size() == 1) {
								result = isEqual(leftSide, left, rightSide, right, operator, useCase, true);
							}
							else if (leftAny || rightAny) {
								result = isAnyEqual(leftSide, left, rightSide, right, operator, useCase, true);
							}
							else {
								// result is set to true because the two operand are not
								// equal.
								result = true;	
								unevenEqual(leftSide, rightSide, operator, result);
							}

						}
						else {
							// result is set to true because the two operand are not
							// equal.
							result = true;
							noValue(leftSide, rightSide, operator, result);
						}
					}
					else if (operator.equals("gt") ||
							operator.equals("gte") ||
							operator.equals("lt") ||
							operator.equals("lte") ||
							operator.equals("count")) {
						// For the greater than, greater than or equal to, less than, 
						// and less than or equal operators we should only have one
						// entry in the list for each side of the operands
						// and each must be able to be converted to a number
						if (rightSide != null) {
							if (leftSide.size() == 1 && rightSide.size() == 1) {
								String l = leftSide.getFirst();
								String r = rightSide.getFirst();
								if (l != null && r != null){
									if (l.contains(".") || r.contains("."))
										result = compareDecimals(l, operator, r);
									else
										result = compareIntegers(l, operator, r);
								}
							}
							else {
								unevenEqual(leftSide, rightSide, operator, result);
							}
						}
						else {
							noValue(leftSide, rightSide, operator, result);
						}
					}
					else if (operator.equals("contains")) {
						// For the contains operator there can be multiple entries in
						// the left list but there should only be zero or one in the right
						if (rightSide != null) {
							if (rightSide.size() == 1) {
								result = contains(leftSide, left, rightSide, right, operator, useCase, false);
							}
						}
						else if (leftSide.size() == 1 && rightSide == null && right == null) {
							// This means the msg_ref information existed in the message.
							result = true;
							logger.info(PC2LogCategory.Examiner, subCat, 
									logLabel + " if " + leftSide.getFirst() 
									+ getSymbol(operator) + "is " + result + ".");							
						}

						else {
							noValue(leftSide, rightSide, operator, result);
						}
					}
					else if (operator.equals("dnc")) {
						// For the does not contain operator there can be multiple entries in
						// the left list but there should only be one in the right
						if (rightSide != null) {
							if (rightSide.size() == 1) {
								result = contains(leftSide, left, rightSide, right, operator, useCase, true);
							}
						}
						else if (leftSide.size() == 1 && rightSide == null && right == null) {
							// This means the msg_ref information existed in the message.
							result = true;
							logger.info(PC2LogCategory.Examiner, subCat,
									logLabel + " if " + leftSide.getFirst()  
									+ getSymbol(operator) + " is " + result + ".");	
						}
						else {
							noValue(leftSide, rightSide, operator, result);
							logger.info(PC2LogCategory.Examiner, subCat,
									logLabel + " if " + leftSide + getSymbol(operator)
									+ rightSide + " is " + result +
							" because the right operand is null.");
						}
					}
					else if (operator.equals("null")) {
						if (leftSide.size() == 1) {
							String l = resolveSubstring(leftSide.getFirst(), left);
							if (l == null) {
								result = true;
							}

							logger.info(PC2LogCategory.Examiner, subCat,
									logLabel + " if " + l + " == null is " + result 
									+ "   caseSensitive=" + useCase + ".");
						}
					}
					else if (operator.equals("notnull")) {
						if (leftSide.size() == 1) {
							String l = resolveSubstring(leftSide.getFirst(), left);
							if (l != null) {
								result = true;
							}

							logger.info(PC2LogCategory.Examiner, subCat,
									logLabel + " if " + l + " != null is " + result 
									+ "   caseSensitive=" + useCase + ".");
						}
						else {
							logger.info(PC2LogCategory.Examiner, subCat,
									logLabel + " if " + leftSide + " != null is " + result 
									+ "   caseSensitive=" + useCase + ".");
						}
					}
					else if (operator.equals("endsWith")) {

						//	For the equals operator we should only have one
						// entry in the list for each side of the operator
						// unless one of the reference used the hdrInstance
						// "any" value
						if (rightSide != null) {
							if (leftSide.size() == 1 && rightSide.size() == 1) {
								String l = resolveSubstring(leftSide.getFirst(), left);
								String r = resolveSubstring(rightSide.getFirst(), right);
								if (l != null && 
										r != null) {
									result = l.endsWith(r);
								}
								else {
									result = false;
								}
								
								logger.info(PC2LogCategory.Examiner, subCat,
										logLabel + " if " + l + getSymbol(operator) 
										+ r + " is " + result 
										+ "   caseSensitive=" + useCase + ".");
							}
							else if (leftAny || rightAny) {
								ListIterator<String> liter = leftSide.listIterator();
								while(liter.hasNext() && !result) {
									String l = resolveSubstring(liter.next(), left);
									//if ()
									ListIterator<String> riter = rightSide.listIterator();
									while (riter.hasNext() && !result) {
										String r = resolveSubstring(riter.next(), right);
										if (r != null)
											result = l.endsWith(r);
										else 
											result = false;
										logger.info(PC2LogCategory.Examiner, subCat,
												logLabel + " if " + l + getSymbol(operator) 
												+ r + " is " + result 
												+ "   caseSensitive=" + useCase + ".");
									}
								}
							}
							else {
								String l = (leftSide.size() >= 1) ? leftSide.getFirst() : null;
								String r = (rightSide.size() >= 1) ? rightSide.getFirst() : null;
								logger.info(PC2LogCategory.Examiner, subCat,
										logLabel + " if " + l + getSymbol(operator) 
										+ r + " is " + result + ".");
							}
						}
						else {
							logger.info(PC2LogCategory.Examiner, subCat,
									logLabel + " if " + leftSide + getSymbol(operator)
									+ rightSide + " is " + result +
							" because the right operand is null.");
						}
					}
					else if (operator.equals("startsWith")) {

						//	For the startsWith operator we should only have one
						// entry in the list for each side of the operator
						// unless one of the reference used the hdrInstance
						// "any" value
						if (rightSide != null) {
							if (leftSide.size() == 1 && rightSide.size() == 1) {
								String l = resolveSubstring(leftSide.getFirst(), left);
								String r = resolveSubstring(rightSide.getFirst(), right);

								if (l != null && r != null)
									result = l.startsWith(r);
								logger.info(PC2LogCategory.Examiner, subCat,
										logLabel + " if " + l + getSymbol(operator) 
										+ r + " is " + result 
										+ "   caseSensitive=" + useCase + ".");
							}
							else if (leftAny || rightAny) {
								ListIterator<String> liter = leftSide.listIterator();
								while(liter.hasNext() && !result) {
									String l = resolveSubstring(liter.next(), left);
									ListIterator<String> riter = rightSide.listIterator();
									while (riter.hasNext() && !result) {
										String r = resolveSubstring(riter.next(), right);

										result = l.startsWith(r);
										logger.info(PC2LogCategory.Examiner, subCat,
												logLabel + " if " + l + getSymbol(operator) 
												+ r + " is " + result 
												+ "   caseSensitive=" + useCase + ".");
									}
								}
							}
							else {
								String l = (leftSide.size() >= 1) ? leftSide.getFirst() : null;
								String r = (rightSide.size() >= 1) ? rightSide.getFirst() : null;
								logger.info(PC2LogCategory.Examiner, subCat,
										logLabel + " if " + l + getSymbol(operator) 
										+ r + " is " + result + ".");
							}
						}
					}
				
					else {
						logger.info(PC2LogCategory.Examiner, subCat,
								logLabel + " if " + leftSide + getSymbol(operator)
								+ rightSide + " is " + result +
						" because the right operand is null.");
					}
						
				}
				else {
					if (operator.equals("neq")||
							operator.equals("dnc")) {
						// result is set to true because the two opreand are not
						// equal.
						if (rightSide == null && leftSide == null)
							result = false;
						else
							result = true;
					}	
					logger.info(PC2LogCategory.Examiner, subCat,
							logLabel + " if " + leftSide + getSymbol(operator)
							+ rightSide + " is " + result 
							+ " because the left operand is null.");

				}
			}
		}
		catch (Exception e) {
			if (operator.equals("neq") ||
					operator.equals("dnc"))
				result = true;
			else 
				result = false;
			logger.warn(PC2LogCategory.Examiner, subCat,
					logLabel + " encountered an exception while processing operator(" 
					+ operator + ") returning " + result + ":\n" + 
					e.getMessage() + "\n" + e.getStackTrace());
		}

		return result;

	}
	
	/**
	 * Provides a common routine to convert a buffer
	 * to a string representation for displaying to the user.
	 * The format of the string is a single string of hex.
	 * 
	 * @param buffer - the bytes to convert
	 * @return - the string representation
	 */
	public static StringBuffer asHex(byte [] buffer) {
		StringBuffer iStr = new StringBuffer();
	       for(int i = 0; i < buffer.length; i++) {
	            if((buffer[i]&0xFF) <= 15)
	            {
	                iStr.append("0");
	            }
	            iStr.append(Integer.toHexString(buffer[i]&0xff).toLowerCase());
 	        }
	       return iStr;
	}

	private boolean caseSensitive(Reference ref) {
		boolean cs = true;
		
		if (ref instanceof SIPRef) {
			// Since this is SIP decide if this is a request for method, header or parameter
			if (((SIPRef)ref).getHeader() == null) 
				cs = false;

		}
		else if (ref instanceof ExtensionRef) {
			cs = false;
		}
		else if (ref instanceof UtilityRef) {
			UtilityRef ur = (UtilityRef)ref;
			if ((ur.getHeader() == null && 
					ur.getParameter() == null) ||
					ur.getMsgType().equals("Request") ||
					ur.getMsgType().equals("Response")) {
				cs = false;
			}
		}
		return cs;
	}

	/**
	 * This takes the string and determines if it adheres to the 
	 * 0x.... hexadecimal format. If it does it performs the binary
	 * operation upon the string and returns the new value otherwise
	 * simply the passed in value is returned.
	 * 
	 * @param ref - 
	 * @param value
	 * @return
	 */
	private String checkForBinaryRef(MsgRef ref, String value) {
		String result = value;
		if (value != null && ref.isBinaryRef()) {
			String temp = new String(value);
			String mask = ref.getBinaryMask();
			boolean and = ref.isBinaryAndOp();
			if (temp.matches("0x[0-9]+")) {
				if (mask.length() == temp.length()) {
					int v = Integer.decode(temp);
					int m = Integer.decode(mask);
					if (and) {
						Integer r = v & m;
						result = Integer.toHexString(r);
					}
					else {
						Integer r = v | m;
						result = Integer.toHexString(r);
					}
				}
				else if (mask.length() < temp.length()) {
					for (int i=(temp.length() -mask.length()); i< temp.length(); i++)
						temp += "0";
					int v = Integer.decode(temp);
					int m = Integer.decode(mask);
					if (and) {
						Integer r = v & m;
						result = Integer.toHexString(r);
					}
					else {
						Integer r = v | m;
						result = Integer.toHexString(r);
					}

				}
				else if (mask.length() > temp.length()) {
					temp = temp.substring(0,temp.length());
					int v = Integer.decode(temp);
					int m = Integer.decode(mask);
					if (and) {
						Integer r = v & m;
						result = Integer.toHexString(r);
					}
					else {
						Integer r = v | m;
						result = Integer.toHexString(r);
					}
				}
				String pad = "0x";
				if (result.length() < temp.length()) {
					int end = (temp.length()-result.length()-pad.length());
					for (int i = 0; i < end; i++)
						pad += "0";
				}
				result = pad + result;
			}
		}

		return result;
			
		
	}
	
	/**
	 * Compare to integer numbers for the various operators
	 * 
	 * @param left
	 * @param right
	 */
	private boolean compareDecimals(String left, String operator, String right) {
		boolean result = false;
		Double l = null;
		Double r = null;
		try {
			l = Double.parseDouble(left);
			r = Double.parseDouble(right);


			if (operator.equals("gt"))
				result = (l > r);
			else if (operator.equals("gte"))
				result = (l >= r);
			else if (operator.equals("lt"))
				result = (l < r);
			else if (operator.equals("lte"))
				result = (l <= r);
			else if (operator.equals("count"))
				result = (l == r);
			

			logger.info(PC2LogCategory.Examiner, subCat,
					logLabel + " if " + l + getSymbol(operator) + r 
					+ " is " + result + ".");
		}
		catch (Exception e) {
			logger.info(PC2LogCategory.Examiner, subCat,
					logLabel + " if " + l 
					+ getSymbol(operator) + r + " is " + result +
			" because one of the values could not be converted to a number.");
		}
		
		return result;
	}
	
	/**
	 * Compare to integer numbers for the various operators
	 * 
	 * @param left
	 * @param right
	 */
	private boolean compareIntegers(String left, String operator, String right) {
		boolean result = false;
		Long l = null;
		Long r = null;
		try {
			l = Long.parseLong(left);
			r = Long.parseLong(right);


			if (operator.equals("gt"))
				result = (l > r);
			else if (operator.equals("gte"))
				result = (l >= r);
			else if (operator.equals("lt"))
				result = (l < r);
			else if (operator.equals("lte"))
				result = (l <= r);
			else if (operator.equals("count"))
				result = (l == r);
			

			logger.info(PC2LogCategory.Examiner, subCat,
					logLabel + " if " + l + getSymbol(operator) + r 
					+ " is " + result + ".");
		}
		catch (Exception e) {
			logger.info(PC2LogCategory.Examiner, subCat,
					logLabel + " if " + l 
					+ getSymbol(operator) + r + " is " + result +
			" because one of the values could not be converted to a number.");
		}
		
		return result;
	}

	/**
	 * Compares to URIs for equality
	 * 
	 * @param left - the left URI to compare
	 * @param right - the right URI to compare
	 * @return
	 */
	private boolean compareURI(String left, String right, boolean useCase) {
		// The rules are the following:
		// 1. remove the leading '<' and trailing '>' from both strings
		// 2. tokenize the result based upon the delimiter ';'
		// 3. compare the first token of each set as these must match
		// 4. compare that each token in the left set appears somewhere
		//    in the second set. If all of appear and there are no
		//    remaining elements in either set. The URIs match.
		// Step 1:
		String ltemp = left.substring(1, left.length()-1);
		String rtemp = right.substring(1, right.length()-1);
		
		// Step 2:
		StringTokenizer ltokens = new StringTokenizer(ltemp, ";");
		StringTokenizer rtokens = new StringTokenizer(rtemp, ";");
		LinkedList<String> rlist = new LinkedList<String>();
		
		// Step 3:
		if (!ltokens.nextToken().equals(rtokens.nextToken()))
			return false;
		
		// Load the list with the information in right
		while (rtokens.hasMoreTokens())
			rlist.add(rtokens.nextToken());
			
		boolean result = true;
		while (ltokens.hasMoreTokens() && result) {
			String l = ltokens.nextToken();
			ListIterator<String> iter = rlist.listIterator();
			boolean done = false;
			// This variable is only used if no match is found
			String options = null;
			while (iter.hasNext() && !done) {
				String r = iter.next();
				if (useCase) {
					if (l.equals(r)) {
						// since we have a match
						// remove it from the list
						rlist.remove(iter);
						logger.debug(PC2LogCategory.Examiner, subCat,
								"Examiner if " + l + " == " 
								+ r + " is " + result 
								+ "   caseSensitive=" + useCase + ".");
						done = true;
					}
					else {
						if (options == null)
							options = "(" + r;
						else
							options = "," + r;
					}
				}
				else {
					if (l.equalsIgnoreCase(r)) {
						// since we have a match
						// remove it from the list 
						rlist.remove(iter);
						logger.debug(PC2LogCategory.Examiner, subCat,
								"Examiner if " + l + " == " 
								+ r + " is " + result 
								+ "   caseSensitive=" + useCase + ".");
						done = true;
					}
					else {
						if (options == null)
							options = "(" + r;
						else
							options = "," + r;
					}
				}
			}
			if (!done) {
				// since the flag is false we couldn't find a match for the
				// left hand token so fail comparison
				result = false;
				logger.debug(PC2LogCategory.Examiner, subCat,
						"Examiner if " + l + " equals any element in the set " 
						+ options + ") is " + result 
						+ "   caseSensitive=" + useCase + ".");
				
			}	
		}
		return result;
	}
	
	/**
	 * Evaluates whether the left operand contains the information in the right operand
	 * 
	 * @param leftSide - list of items for the left operand
	 * @param left - the original reference details to resolve to the value to use in the operation
	 * @param rightSide - list of items for the right operand
	 * @param right - the original reference details to resolve to the value to use in the operation
	 * @param operator - the label for the operation being performed
	 * @param useCase - whether the operation is case sensitive or not
	 * 
	 * @return result
	 */
	private boolean contains(LinkedList<String> leftSide, Reference left, 	
			LinkedList<String> rightSide, Reference right, String operator, boolean useCase, boolean negative) {
		boolean result = false;
		String key = resolveSubstring(rightSide.getFirst(), right);

		ListIterator<String> iter = leftSide.listIterator();
		String msg = new String();
		while(iter.hasNext() && !result) {
			String element = resolveSubstring(iter.next(), left);
			if (element != null) {
				if (useCase) {
					result = element.contains(key);
				} else {
					result = element.toUpperCase().contains(key.toUpperCase());
				}
			} else { 
				result = false;
			}
			msg += element + " ";
		}
		if (msg.equals(""))
			msg = "\"\"";
		
		if (negative) {
			result = !result;
		}
		logger.info(PC2LogCategory.Examiner, subCat,
				logLabel + " if " + msg +  getSymbol(operator)
				+ (useCase ? "" : "(case insensitive) ")
				+ key + " is " + result + ".");
		return result;
	}	
	/**
	 * Calculates the digest for a message authentication
	 */
	private boolean digest(MsgEvent event) {
	 
		boolean result = false;
		if (event instanceof SIPMsg) {
			SIPMsg msg = (SIPMsg)event;
			if (msg.isRequestMsg()) 
				result = digest(msg.getRequest());
			else if (msg.isResponseMsg()) 
				result = digest(msg.getResponse());
			else
				return false;
		}
		else if (event instanceof StunMsg) {
			result = digest((StunMsg)event);
		}
		else 
			result = false;
		logger.info(PC2LogCategory.Examiner, subCat,
				"Examiner if message has a valid digest result is " + result + ".");
		
		return result;
	}
	
	/**
	 * Authenticates a SIP Request message compared to the previous
	 * response using digest.
	 * 
	 * @param req
	 * @return
	 */
	private boolean digest(Request req) {
//		 KD(secret, data) = H(concat(secret, ":", data))
//
//		request-digest  = <"> < KD ( H(A1),     unq(nonce-value)
//        					":" nc-value
//        					":" unq(cnonce-value)
//        					":" unq(qop-value)
//        					":" H(A2)
//        					) <">
//        A1       = unq(username-value) ":" unq(realm-value) ":" passwd
//        If the "qop" directive's value is "auth" or is unspecified, then A2
//        is:
//
//           A2       = Method ":" digest-uri-value
//
//        If the "qop" value is "auth-int", then A2 is:
//
//           A2       = Method ":" digest-uri-value ":" H(entity-body)

		boolean result = false;
		try {
			if (req == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
					"Digest failed because the req parameter is null.");
				return result;
			}
			String method = req.getMethod();
			String callId = ((CallIdHeader)req.getHeader(CallIdHeader.NAME)).getCallId();
			
			boolean useProxyHdrs = false;
			MsgEvent event401 = null;
			MsgEvent event407 = null;
			LinkedList<MsgEvent> events = q.findAll(fsmUID, "401-" + method, fsm.getCurrentMsgQueueIndex());
			ListIterator<MsgEvent> iter = null;
			boolean done = false;
			if (events.size() > 0) {
				// Set it to the last element so we can move backwards.
				iter = events.listIterator(events.size());
				while (iter.hasPrevious() && !done) {
					MsgEvent event = iter.previous();
					Response resp = ((SIPMsg)event).getResponse();
					String respCallId = ((CallIdHeader)resp.getHeader(CallIdHeader.NAME)).getCallId();
					if (respCallId.equals(callId)) {
						event401 = event;
						done = true;
					}
				}
			}
			done = false;
			events = q.findAll(fsmUID, "407-" + method, fsm.getCurrentMsgQueueIndex());
			if (events.size() > 0) {
				// Set it to the last element
				iter = events.listIterator(events.size());
				while (iter.hasPrevious() && !done) {
					MsgEvent event = iter.previous();
					Response resp = ((SIPMsg)event).getResponse();
					String respCallId = ((CallIdHeader)resp.getHeader(CallIdHeader.NAME)).getCallId();
					if (respCallId.equals(callId)) {
						event407 = event;
						done = true;
					}
				}
			}
//			MsgEvent event401 = q.find(fsmUID, "401-" + method, 
//					MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
//			MsgEvent event407 = q.find(fsmUID, "407-" + method, 
//					MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
			if (event401 == null && event407 == null) {
				logger.debug(PC2LogCategory.Examiner, subCat,
						"Neither a 401 nor a 407 response could be found for the " 
						+ method + " request message" +
						".");
				return result;
			}
			MsgEvent msgEvent = null;
			if (event401 != null && event407 == null)
				msgEvent = event401;
			else if (event401 == null && event407 != null) {
				msgEvent = event407;
				useProxyHdrs = true;
			}
			else if (event401 != null && event407 != null) {
				if (event401.getMsgQueueIndex() > event407.getMsgQueueIndex())
					msgEvent = event401;
				else {
					msgEvent = event407;
					useProxyHdrs = true;
				}
			}
			
			if (msgEvent == null) {
				logger.info(PC2LogCategory.Examiner, subCat,
						"A 401 nor 407 response could be found for the " 
						+ method + " request message" +
						".");
				return result;
			}
			
			String alg = null;
			String auth = null;
			String azOpaque = null;
			String username = null;
			String uri = null;
			String cnonce = null;
			String qop_value = null;
			String nonceCount = null;
			String authResponse = null;
			if (useProxyHdrs) {
				ProxyAuthorizationHeader pah = (ProxyAuthorizationHeader)req.getHeader(ProxyAuthorizationHeader.NAME); 
				if (pah == null) {
					logger.warn(PC2LogCategory.Examiner, subCat,
					"Digest failed because the ProxyAuthorization header could not be found.");
					return result;
				}
				alg = pah.getAlgorithm();
				auth = pah.getQop();
				// OPAQUE parameter is no longer necessary according to Stuart Hoggan
				azOpaque = unq(pah.getOpaque());
				if (azOpaque == null) {
					logger.info(PC2LogCategory.Examiner, subCat,
					"The opaque field of in the ProxyAuthorization header is null.");
					azOpaque = "";
					// OPAQUE parameter is no longer necessary according to Stuart Hoggan
					// return result;
				}
				// Get username from current message
				username = unq(pah.getUsername());
				if (username == null)	{
					logger.warn(PC2LogCategory.Examiner, subCat,
						"The username field in the ProxyAuthorization header of the current request is null.");
					return result;
				}
				Object azURI = pah.getURI();
				if (azURI == null) {
					logger.warn(PC2LogCategory.Examiner, subCat,
						"The uri field in the ProxyAuthorization header of the current request is null.");
					uri = "";
				}
				else 
					uri = unq(pah.getURI().toString());
									
				cnonce = unq(pah.getCNonce());
				qop_value = unq(pah.getQop());
				nonceCount = pah.getNonceCount();
				authResponse = pah.getResponse();
			}
			else {
				AuthorizationHeader ah = (AuthorizationHeader)req.getHeader(AuthorizationHeader.NAME); 
				if (ah == null) {
					logger.warn(PC2LogCategory.Examiner, subCat,
					"Digest failed because the Authorization header could not be found.");
					return result;
				}
				alg = ah.getAlgorithm();
				auth = ah.getQop();
				azOpaque = unq(ah.getOpaque());
				if (azOpaque == null) {
					logger.info(PC2LogCategory.Examiner, subCat,
					"The opaque field of in the Authorization header is null.");
					azOpaque = "";
					// OPAQUE parameter is no longer necessary according to Stuart Hoggan
					// return result;
				}
				// Get username, method and digest-uri from current message
				username = unq(ah.getUsername());
				if (username == null)	{
					logger.warn(PC2LogCategory.Examiner, subCat,
						"The username field in the Authorization header of the current request is null.");
					return result;
				}
				Object azURI = ah.getURI();
				if (azURI == null) {
					logger.warn(PC2LogCategory.Examiner, subCat,
						"The uri field in the Authorization header of the current request is null.");
					uri = "";
				}
				else 
					uri = unq(ah.getURI().toString());
									
				cnonce = unq(ah.getCNonce());
				qop_value = unq(ah.getQop());
				nonceCount = ah.getNonceCount();
				authResponse = ah.getResponse();
			}
			if (alg == null || alg.equals("MD5")) {  // NULL value means MD5
				if (auth == null || auth.equals("auth") || auth.equals("auth-int")) { // NULL value means auth
					boolean authInt = false;
					if (auth != null && auth.equals("auth-int"))
						authInt = true;
					
					// Get the last response
					Response resp = ((SIPMsg)msgEvent).getResponse();
					if (resp == null) {
						logger.warn(PC2LogCategory.Examiner, subCat,
								"The previous SIP " + method 
								+ " event doesn't contain a response event.");
						return result;
					}
					
					String authOpaque = null;
					String nonce = null;
					String realm = null;
					if (useProxyHdrs) {
						// Get the WWWAuthenticate header from the last response
						ProxyAuthenticateHeader authHeader = (ProxyAuthenticateHeader)resp.getHeader(ProxyAuthenticateHeader.NAME);
						if (authHeader == null) {
							logger.warn(PC2LogCategory.Examiner, subCat,
									"The previous SIP " + method 
									+ " response doesn't contain a ProxyAuthenticate header.");
							return result;
						}
						// Get  realm,  and nonce from original 401 message
						
						authOpaque = unq(authHeader.getOpaque());
						if (authOpaque == null) {
							logger.warn(PC2LogCategory.Examiner, subCat,
							"The opaque field of in the ProxyAuthenticate header is null.");
							authOpaque = "";
							// OPAQUE parameter is no longer necessary according to Stuart Hoggan
							// return result;
						}
						nonce = unq(authHeader.getNonce());
						if (nonce == null) {
							logger.warn(PC2LogCategory.Examiner, subCat,
									"The nonce field in the ProxyAuthenticate header of the previous response is null.");
							return result;
						}
						realm = unq(authHeader.getRealm());
						if (realm == null) {
							logger.warn(PC2LogCategory.Examiner, subCat,
								"The realm field in the ProxyAuthenticate header of the previous response is null.");
							return result;
						}
			
					}
					else {
						WWWAuthenticateHeader authHeader = (WWWAuthenticateHeader)resp.getHeader(WWWAuthenticateHeader.NAME);
						if (authHeader == null) {
							logger.warn(PC2LogCategory.Examiner, subCat,
									"The previous SIP " + method 
									+ " response doesn't contain a WWWAuthenticate header.");
							return result;
						}	
						// Get  realm, opaque, and nonce from original 401 message
						
						authOpaque = unq(authHeader.getOpaque());
						if (authOpaque == null) {
							logger.warn(PC2LogCategory.Examiner, subCat,
							"The opaque field of in the WWWAuthenticate header is null.");
							authOpaque = "";
							// OPAQUE parameter is no longer necessary according to Stuart Hoggan
							// return result;
						}
						nonce = unq(authHeader.getNonce());
						if (nonce == null) {
							logger.warn(PC2LogCategory.Examiner, subCat,
									"The nonce field in the WWWAuthenticate header of the previous response is null.");
							return result;
						}
						realm = unq(authHeader.getRealm());
						if (realm == null) {
							logger.warn(PC2LogCategory.Examiner, subCat,
								"The realm field in the WWWAuthenticate header of the previous response is null.");
							return result;
						}
						
					}
					// Get  realm, opaque, and nonce from original 401 message
					
					// Since both headers have an opaque header make sure they match.
					if (!azOpaque.equals(authOpaque)) {
						logger.warn(PC2LogCategory.Examiner, subCat,
								"The opaque field in the WWWAuthenticate header of the previous response to " + method 
								+ " and the current request's Authorization header are not equal.");
						return result;
					}

					// Get password from system settings
					Properties prui = SystemSettings.getPropertiesByValue(SettingConstants.PRUI, username);
					if (prui == null) {
						logger.warn(PC2LogCategory.Examiner, subCat,
							"No property with its' \"prui\" set to the value (" + username + ") could be found in the configuration files.");
						return result;
					}
					String passwd = prui.getProperty(SettingConstants.PASSWORD);
					if (passwd == null ) {
						logger.warn(PC2LogCategory.Examiner, subCat,
							"The \"password\" setting for the network element label (" 
								+ prui.getProperty(SettingConstants.NE) + ") is null.");
						return result;
					}
					
					String a1 = username + ":" + realm + ":" + passwd;
					String mid = "";
					if (auth == null)
						mid = ":" + nonce + ":";
					else
						mid = ":" + nonce + ":" + nonceCount + ":" +
						cnonce + ":" + qop_value + ":";
					String a2 = method + ":" + uri;
					if (authInt) {
						String entityBody = "";
						Object content = req.getContent();
						if (content instanceof String)
							entityBody = (String)content;
						else if (content instanceof byte []) {
							byte [] bytes = (byte [])content;
							entityBody = new String(bytes, 0, bytes.length);
						}
						byte[] HEntity = digester.digest(entityBody.getBytes());
						a2 += ":" + Conversion.hexString(HEntity);
					}
					
					byte [] HA1 = digester.digest(a1.getBytes());
					byte [] HA2 = digester.digest(a2.getBytes());

					String nd = asHex(HA1) + mid + asHex(HA2);
					byte [] reqDigest = digester.digest(nd.getBytes());

					logger.info(PC2LogCategory.Examiner, subCat,
							"\n\na1 - " + a1
							+ "\na2 - " + a2
							+ "\nmid - " + mid
							+ "\nHA1 - " + asHex(HA1)
							+ "\nrandNum = " + cnonce
							+ "\nHA2 - " + asHex(HA2)
							+ "\nCAT - " + nd.toString()
							+ "\nKD - " + asHex(reqDigest) + "\n\n");

					logger.info(PC2LogCategory.Examiner, subCat,
							"Auth response =" + authResponse + "\t KD " 
							+ asHex(reqDigest));
					String finalDigest = asHex(reqDigest).toString();
					if (finalDigest.equals(authResponse)) {
						logger.debug(PC2LogCategory.Examiner, subCat,
						"Digest matches.");
						result = true;
					}	
					else 
						logger.debug(PC2LogCategory.Examiner, subCat,
						"Digest failed.");
				}
			}
			
			else if (alg.equals("MD5-sess")) {
				logger.warn(PC2LogCategory.Examiner, subCat,
						"Client is using MD5-sess digest algorithm which is not currently" +
				" supported in the platform.");
				
			}
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.Examiner, subCat, 
					"Digest failed because of exception, " + ex.getMessage() + "\n" + ex.getStackTrace());
		}
		
		return result;
		
	}

	/**
	 * Authenticates a SIP Response message compared to the previous
	 * request using digest.
	 * 
	 * @param resp
	 * @return
	 */
	private boolean digest(Response resp) {
		// NOTE: Currently this algorithm assumes Qop is auth, it does NOT
		// support auth-int
//		 KD(secret, data) = H(concat(secret, ":", data))
		//
//				request-digest  = <"> < KD ( H(A1),     unq(nonce-value)
//		        					":" nc-value
//		        					":" unq(cnonce-value)
//		        					":" unq(qop-value)
//		        					":" H(A2)
//		        					) <">
//        A1       = unq(username-value) ":" unq(realm-value) ":" passwd
		 
//		 A2       = ":" digest-uri-value
//
//		   and if "qop=auth-int", then A2 is
//
//		 A2       = ":" digest-uri-value ":" H(entity-body)
//
//		   where "digest-uri-value" is the value of the "uri" directive on the
//		   Authorization header in the request. The "cnonce-value" and "nc-
//		   value" MUST be the ones for the client request to which this message
//		   is the response. The "response-auth", "cnonce", and "nonce-count"
//		   directives MUST BE present if "qop=auth" or "qop=auth-int" is
//		   specified.
		boolean result = false;
		try {
			if (resp == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
					"Digest failed because the resp parameter is null.");
				return result;
			}
			
			CSeqHeader ch = (CSeqHeader)resp.getHeader(CSeqHeader.NAME);
			if (ch == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
				"Digest failed because the CSeqHeader is not in the response\n[" + resp + "].");
				return result;
			}
			
			String method = ch.getMethod();
			MsgEvent reqEvent = q.find(fsmUID, method, 
					MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
			if (reqEvent == null) {
				logger.info(PC2LogCategory.Examiner, subCat,
							"Digest failed because the " + method 
							+ " request message could not be found.");
				return result;
			}
		
			Request req = ((SIPMsg)reqEvent).getRequest();
			AuthorizationHeader ah = (AuthorizationHeader)req.getHeader(AuthorizationHeader.NAME); 
			if (ah == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
				"Digest failed because the Authorization header could not be found in the last " + method + " message.");
				return result;
			}
			
			AuthenticationInfoHeader aih = (AuthenticationInfoHeader)resp.getHeader(AuthenticationInfoHeader.NAME);
			if (aih == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
						"Digest failed because the Authentication-Info header could not be found in the current " 
						+ resp.getStatusCode() + "-" + method + " message.");
				return result;
			}
			
			// Get username, method and digest-uri from current message
			String username = unq(ah.getUsername());
			// Get password from system settings
			Properties prui = SystemSettings.getPropertiesByValue(SettingConstants.PRUI, username);
			if (prui == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
					"No property with its' \"prui\" set to the value (" + username + ") could be found in the configuration files.");
				return result;
			}
			String passwd = prui.getProperty(SettingConstants.PASSWORD);
			if (passwd == null ) {
				logger.warn(PC2LogCategory.Examiner, subCat,
					"The \"password\" setting for the network element label (" 
						+ prui.getProperty(SettingConstants.NE) + ") is null.");
				return result;
			}
			
			String uri = null;
			Object azURI = ah.getURI();
			if (azURI == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
					"The uri field in the Authorization header of the last " + method + " request message is null.");
				uri = "";
			}
			else 
				uri = unq(ah.getURI().toString());
								
			String cnonce = unq(ah.getCNonce());
			if (cnonce == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
						"The cnonce field in the Authorization header of the last " + method + " request is null.");
				return result;
			}
			String qop_value = unq(ah.getQop());
			if (qop_value == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
						"The qop field in the Authorization header of the last " + method + " request is null.");
				qop_value = "";
				// OPAQUE parameter is no longer necessary according to Stuart Hoggan
				//return result;
			}
			String nonceCount = ah.getNonceCount();
			if (nonceCount == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
						"The nc field in the Authorization header of the last " + method + " request is null.");
				return result;
			}
			
			String rspAuth = aih.getResponse();
			String nonce = unq(ah.getNonce());
			if (nonce == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
						"The nonce field in the Authorization header of the last " + method + " request is null.");
				return result;
			}
			String realm = unq(ah.getRealm());
			if (realm == null) {
				logger.warn(PC2LogCategory.Examiner, subCat,
					"The realm field in the Authorization header of the last " + method + " request is null.");
				return result;
			}
			String a1 = username + ":" + realm + ":" + passwd;
			String mid = "";
			
			String auth = null;
			if (qop_value != null && qop_value.length() > 0)
				auth = qop_value;
			
			if (auth == null)
				mid = ":" + nonce + ":";
			else
				mid = ":" + nonce + ":" + nonceCount + ":" +
				cnonce + ":" + qop_value + ":";
			String a2 = ":" + uri;
			byte [] HA1 = digester.digest(a1.getBytes());
			byte [] HA2 = digester.digest(a2.getBytes());

			String nd = asHex(HA1) + mid + asHex(HA2);
			byte [] respDigest = digester.digest(nd.getBytes());

			String finalDigest = asHex(respDigest).toString();
			logger.debug(PC2LogCategory.Examiner, subCat,
					"\n\na1 - " + a1
					+ "\na2 - " + a2
					+ "\nmid - " + mid
					+ "\nHA1 - " + asHex(HA1)
					+ "\nrandNum = " + cnonce
					+ "\nHA2 - " + asHex(HA2)
					+ "\nCAT - " + nd.toString()
					+ "\nKD - " + finalDigest + "\n\n");

			
			logger.info(PC2LogCategory.Examiner, subCat,
					"Authentication-Info rspAuth =" + rspAuth + "\t KD " 
					+ finalDigest);
			
			if (finalDigest.equals(rspAuth)) {
				logger.debug(PC2LogCategory.Examiner, subCat,
				"Digest matches.");
				result = true;
			}	
			else {
				logger.debug(PC2LogCategory.Examiner, subCat,
				"Digest failed.");
			}
		}
		catch (Exception ex) {
			logger.warn(PC2LogCategory.Examiner, subCat, 
					"Digest failed because of exception, " + ex.getMessage() + "\n" + ex.getStackTrace());
		}
		
		return result;
	}
	/**
	 * Authenticates a request message compared to the previous
	 * response using digest.
	 * 
	 * @param req
	 * @return
	 */
	private boolean digest(StunMsg event) {
		StunMessage msg = event.getMessage();
		byte [] bytes = msg.encodeForDigest();
		byte [] hash = digester.digest(bytes);
		FingerPrint fp = (FingerPrint)msg.getAttribute(StunConstants.FINGERPRINT_TYPE);
		if (fp != null) {
			byte [] fpHash = fp.getValue();
			if (java.util.Arrays.equals(fpHash, hash))
					return true;
		}
		return false;
	}
	
	
	private LinkedList<String> findRef(Reference ref, MsgEvent event) {
		LinkedList<String> result = null;
		if (ref instanceof SDPRef) {
			result = getSDPString((SDPRef)ref, event);
		}
		else if (ref instanceof SIPBodyRef) {
			result = getSIPBodyString((SIPBodyRef)ref, event);
		}
		else if (ref instanceof SIPRef) {
			// Since this is SIP decide if this is a request for method, header or parameter
			result = getSIPString((SIPRef)ref, event); 
		}
		
		else if (ref instanceof UtilityRef) {
			result = getUtilityString((UtilityRef)ref, event);
		}
		else if (ref instanceof ExtensionRef) {
			ExtensionRef er = (ExtensionRef)ref;
			Extension e = SystemSettings.getInstance().getExtension(er.getExt());
			result = new LinkedList<String>();
			if (e != null) {
				if (e == Extension.DISABLED) {
					result.add("disable");
				}
				else if (e == Extension.REQUIRED) {
					result.add("require");
				}
				else if (e == Extension.SUPPORTED) {
					result.add("support");
				}
			}
		}
		else if (ref instanceof PlatformRef) {
			PlatformRef pr = (PlatformRef)ref;
			Properties p = SystemSettings.getSettings(pr.getNELabel());
			if (p != null) {
				String param = p.getProperty(pr.getParameter());
				if (param != null) {
					result = new LinkedList<String>();
					logger.debug(PC2LogCategory.Examiner, subCat,
							"Examiner returning param value=[" + param 
							+ "] for ref=" + ref);
					result.add(param);
				}
			}
		}
		else if (ref instanceof EventRef) {
			if (event instanceof InternalMsg) {
				result = new LinkedList<String>();
				logger.debug(PC2LogCategory.Examiner, subCat,
						"Examiner returning param value=[" + event.getEventName() 
						+ "] for ref=" + ref);
				String hdr = ((EventRef)ref).getHeader();
				if (hdr != null) {
					String value = refLocator.getEventReference(hdr, null, event);
					if (value != null) {
						result.add(value);
					}
				}
				else
					result.add(event.getEventName()); 
			}
		}
		else if (ref instanceof Literal) {
			result = new LinkedList<String>();
			logger.debug(PC2LogCategory.Examiner, subCat,
					"Examiner returning param value=[" + ((Literal)ref).getExpr() 
					+ "] for ref=" + ref);
			result.add(((Literal)ref).getExpr());
		}
		else if (ref instanceof StunRef) {
			result = getStunString((StunRef)ref, event); 

		}
		else if (ref instanceof VarRef) {
			result = getVarRefString((VarRef)ref, event); 

		}
		else if (ref instanceof CurStateRef) {
			result = new LinkedList<String>();
			result.add(fsm.getCurrentStateCount());
		}
		else if (ref instanceof RTPRef) {
			result = getRTPString((RTPRef)ref, event); 
		}
		else if (ref instanceof CaptureRef) {
			CaptureRef cr = (CaptureRef)ref;
			PacketDatabase db = PC2Models.getCaptureDB(cr.getDBName());
			result = capLocator.getReferenceInfo(db, cr);
		}
		
		return result;
	}

	
	/**
	 * Retrieves a list of Request messages from the message queue that meets
	 * the criteria defined by the reference.
	 * 
	 * @param ref - the msg_ref criteria to find.
	 * @param event - the current event.
	 * @return - a list of SIP Request messages in string format that 
	 * 		fulfills the criteria in the reference.
	 */
	private LinkedList<String> getRequestRef(SIPRef ref, MsgEvent event) {
		String msgInstance = ref.getMsgInstance();
		LinkedList<String> ll = new LinkedList<String>();
		boolean nullParameter = (ref.getParameter() == null);
		String hdr = ref.getHeader();
		String method = ref.getMethod();
		boolean nullMethod = (method == null);
		boolean nullHdr = (hdr == null);
		
		LinkedList<MsgEvent> searchResults = new LinkedList<MsgEvent>();
		if (msgInstance.equals(MsgQueue.CURRENT) && 
				method != null && 
				(method.equalsIgnoreCase(event.getEventName()) || 
						method.equalsIgnoreCase(SIPConstants.REQUEST))) {
			searchResults.add(event);

		}
		else if (msgInstance.equals(MsgQueue.ANY)) {
			searchResults = q.findAll(ref.getUID(), ref.getMethod(), 
					fsm.getCurrentMsgQueueIndex());

		}
		else {
			MsgEvent msgEvent = q.find(ref.getUID(), ref.getMethod(), 
					ref.getMsgInstance(), fsm.getCurrentMsgQueueIndex());
			searchResults.add(msgEvent);

		}
		ListIterator<MsgEvent> iter = searchResults.listIterator();
		while(iter.hasNext()) {
			MsgEvent element = iter.next();
			if (element instanceof SIPMsg) {
				SIPMsg msg = (SIPMsg)element;
				if (msg.isRequestMsg()) {
					if (ref.isReferenceOnEvent()) {
						String value = refLocator.getEventReference(hdr, ref.getParameter(), element);
						if (value != null) {
							ll.add(value);
						}
					}
					// Return the method name if the method equals request and there is no
					// there is not header
					else if ((ref.getMethod().equals("Request") && nullHdr) ||
							(nullMethod && nullParameter)) {
						ll.add(msg.getEvent());
					}
					else if (msg.hasSentMsg())
						ll.add(msg.getSentMsg());
					else
						ll.add(msg.getRequest().toString());
				}
			}
		}
		return ll;
	}

	/**
	 * Retrieves a list of Response messages from the message queue that meets
	 * the criteria defined by the reference.
	 * 
	 * @param ref - the msg_ref criteria to find.
	 * @param event - the current event.
	 * @return - a list of SIP Response messages in string format that
	 * 		fulfills the criteria in the reference.
	 */
	private LinkedList<String> getResponseRef(SIPRef ref, MsgEvent event) {
		String msgInstance = ref.getMsgInstance();
		LinkedList<String> ll = new LinkedList<String>();
		boolean nullMethod = (ref.getMethod() == null);
		boolean nullParameter = (ref.getParameter() == null);
		String hdr = ref.getHeader();
		boolean nullHdr = (hdr == null);
		
		LinkedList<MsgEvent> searchResults = new LinkedList<MsgEvent>();
		if (!nullMethod) {
			String method = null;
			String statusCode = null;
			if (ref.getMethod().equals(SIPConstants.RESPONSE)) 
				method = SIPConstants.RESPONSE;
			else {
				method = ref.getStatusCode() + "-" + ref.getMethod();
				statusCode = ref.getStatusCode();
			}
			if (msgInstance.equals(MsgQueue.CURRENT) && 
					method != null) {
				// If the method is RESPONSE or matches exactly then
				// add it.
				if 	(method.equalsIgnoreCase(event.getEventName()) || 
							method.equalsIgnoreCase(SIPConstants.RESPONSE)) 
					searchResults.add(event);
				// However there is a chance that the status code contains
				// a wildcard so look for each based upon the most exact
				// criteria
				else if (statusCode != null && statusCode.length() == 3) {
					String name = event.getEventName();
					if (statusCode.equals("xxx")) {
						// Since the we are looking for any response that matches the 
						// method just compare methods.
						if (name.substring(4).equalsIgnoreCase(ref.getMethod())) {
							searchResults.add(event);
						}
					}
					else if (statusCode.substring(1,3).equals("xx")) {
						// This means we are looking for any response that matches
						// a hundred range. So match the first number and the method.
						if (name.substring(0,1).equals(statusCode.substring(0,1)) &&
								name.substring(4).equalsIgnoreCase(ref.getMethod())) {
							searchResults.add(event);
						}
					}
					if (statusCode.charAt(2) == 'x') {
						// This means we are looking for any response that matches
						// a tens range. So match the first two numbers and the method.
						if (name.substring(0,2).equals(statusCode.substring(0,2)) &&
								name.substring(4).equalsIgnoreCase(ref.getMethod())) {
							searchResults.add(event);
						}
					}
				}
			}
			else if (msgInstance.equals(MsgQueue.ANY)) {
				searchResults = q.findAll(ref.getUID(), method, 
						fsm.getCurrentMsgQueueIndex());

			}
			else {

				MsgEvent msgEvent = q.find(ref.getUID(), method, 
						ref.getMsgInstance(), fsm.getCurrentMsgQueueIndex());
				searchResults.add(msgEvent);
			}
		}
		ListIterator<MsgEvent> iter = searchResults.listIterator();
		while(iter.hasNext()) {
			MsgEvent element = iter.next();
			if (element instanceof SIPMsg) {
				SIPMsg msg = (SIPMsg)element;
				if (ref.isReferenceOnEvent()) {
					String value = refLocator.getEventReference(hdr, ref.getParameter(), element);
					ll.add(value);
				}
				else if (msg.isResponseMsg()) {
					if ((ref.getMethod().equals("Response") && nullHdr) ||
							(nullMethod && nullParameter)) {
						Response resp = msg.getResponse();
						if (nullParameter)
							ll.add(resp.getStatusCode() + "-" + 
									((CSeqHeader)resp.getHeader(CSeqHeader.NAME)).getMethod());
						else	
							ll.add(resp.toString());
					}
					
					else if (msg.hasSentMsg())
						ll.add(msg.getSentMsg());
					else
						ll.add(msg.getResponse().toString());
				}
			}
		}
		return ll;
	}
	
	/**
	 * Retrieves a list of RTP messages from the message queue that meets
	 * the criteria defined by the reference.
	 * 
	 * @param ref - the msg_ref criteria to find.
	 * @param event - the current event.
	 * @return - a list of RTP messages that fulfills the criteria in the
	 * 			reference.
	 */
	private LinkedList<String> getRTPString(RTPRef ref,  MsgEvent event) {
		LinkedList<String> ll = new LinkedList<String>();
		boolean nullHeader = (ref.getHeader() == null);
		if (ref.getMsgInstance().equals(MsgQueue.CURRENT)) {
			if (event instanceof RTPMsg)	 {
				String msgType = ((RTPMsg)event).getEventName();
				if (msgType != null) {
					if (nullHeader)
						ll.add(msgType);
					else 
						parseRTPHeader(ll, ref, (RTPMsg)event);
				}

			}
		}	
		else if (ref.getMsgInstance().equals(MsgQueue.ANY)) {
			LinkedList<MsgEvent> searchResults = q.findAll(ref.getUID(), ref.getMethod(), 
					fsm.getCurrentMsgQueueIndex());
			ListIterator<MsgEvent> iter = searchResults.listIterator();
			while(iter.hasNext()) {
				Object element = iter.next();
				if (element instanceof RTPMsg) {
					String msgType = ((RTPMsg)element).getEventName();
					if (msgType != null) {
						if (nullHeader)
							ll.add(msgType);
						else 
							parseRTPHeader(ll, ref, (RTPMsg)element);
					}
				}
			}
		}
		else {
			MsgEvent msgEvent = q.find(ref.getUID(), ref.getMethod(), 
					ref.getMsgInstance(), fsm.getCurrentMsgQueueIndex());
			if (msgEvent instanceof RTPMsg) {
				String msgType = ((RTPMsg)msgEvent).getEventName();
				if (msgType != null) {
					if (nullHeader) 
						ll.add(msgType);
					else
						parseRTPHeader(ll, ref, (RTPMsg)msgEvent);
				}
			}
		}
		return ll;
	}

	/**
	 * Retrieves the correct SDP string to use in a comparison based
	 * upon the information in the reference parameter.
	 * 
	 * @param ref - the SDP msg_ref information
	 * @param event - the current event.
	 * @return - a list of strings matching the information defined by the 
	 * 			reference.
	 */
	private LinkedList<String> getSDPString(SDPRef ref,  MsgEvent event) {
		// First obtain all the messages to operate upon
		// The messages are either the event if the msg_instance
		// in ref is set to current or one or more entries in the 
		// message queue if the msg_instance is another value.
		LinkedList<String> result = new LinkedList<String>();
		try {
			LinkedList<String> msgRefs = null;
			if (ref.isSIPResponseRef()) 
				msgRefs = getResponseRef(ref, event);
			else 
				msgRefs = getRequestRef(ref, event);

			ListIterator<String> iter = msgRefs.listIterator();
			while (iter.hasNext()) {
				String resp = iter.next();
				String ct = "Content-Type: ";
				int ctBegin = resp.indexOf(ct);
				if (ctBegin != -1) {
					int ctEnd = resp.indexOf("\r\n", ctBegin);
					if (ctEnd != -1) {
						String ctHdr = resp.substring(ctBegin, ctEnd);
						boolean sdp = ctHdr.contains(MsgRef.SDP_MSG_TYPE);
						if (sdp) {
							int bodyBegin = resp.indexOf("\r\n\r\n", ctEnd);
							if (bodyBegin != -1) {
								// Adjust the string past double CRLF
								String msg = resp.substring(bodyBegin+4);
								//parseSDPHeaders(result, msg, ref);
								getSDPHeaders(result, msg, ref);
							}
						}
						else {
							boolean multipart = ctHdr.contains(MsgRef.MULTIPART_MIXED_TYPE);
							if (multipart) {
								String boundary = sipLocator.getSIPParameter("Content-Type", "boundary", 
										MsgQueue.FIRST, resp);
								if (boundary != null) {
									int bodyBegin = resp.indexOf("\r\n\r\n", ctEnd);
									if (bodyBegin != -1) {
										// Now that we know where the body begin find the correct 
										// content
										ctBegin = resp.indexOf("Content-Type: application/sdp", bodyBegin);
										if (ctBegin != -1) {
											int sdpInstance = resp.lastIndexOf(boundary, ctBegin);
											if (sdpInstance != -1 && sdpInstance >= bodyBegin) {
												bodyBegin = sdpInstance;
												// Now move to the correct body instance
												// Now we need to make some decisions based upon the 
												// body.
												int value = -1;
												String bodyInstance = ref.getBodyInstance();
												try {
													value = Integer.parseInt(bodyInstance);
												}
												catch (NumberFormatException nfe) {
													value = -1;
												}
												if (bodyInstance.equals(MsgQueue.FIRST) ||
														value == 1) {
													// Nothing more to do for instance
												}
												else if (bodyInstance.equals(MsgQueue.LAST)) {
													int last = resp.lastIndexOf(ct + "application/sdp");
													if (last != -1 && last > bodyBegin) {
														sdpInstance = resp.lastIndexOf(boundary, bodyBegin);
														if (sdpInstance != -1 && sdpInstance >= bodyBegin) {
															bodyBegin = sdpInstance;
														}
													}
												}
												else if (value > 1){
													int instance = 0;
													int offset = resp.indexOf(ct + "application/sdp", bodyBegin);
													boolean done = false;
													while (offset != -1 && !done) {
														instance++;
														if (instance == value) {
															bodyBegin = offset;
															done = true;
														}
														else 
															offset = resp.indexOf(ct + "application/sdp", offset);
													}
												}
												String endKey = "--" + boundary + "-";
												int bodyEnd = resp.indexOf(endKey, bodyBegin);
												if (bodyEnd == -1)
													// Assume the end of the body then as the end marker may be
													// missing from the stack.
													bodyEnd = resp.length();

												if (bodyEnd != -1) {
													bodyBegin += boundary.length() + 2;
													String body = resp.substring(bodyBegin, bodyEnd);
													getSDPHeaders(result, body, ref);
//													if (ctBegin != -1) {
//														bodyBegin = body.indexOf("\r\n\r\n");
//														if (bodyBegin != -1) {
//															bodyBegin +=4; // Move past CRLFCRLF
//															bodyEnd = body.indexOf("\r\n\r\n", bodyBegin);
//															if (bodyEnd != -1) {
//																String msg = body.substring(bodyBegin, bodyEnd);
//																//parseSDPHeaders(result, msg, ref);
//																
//															}
//														}
//													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}

		}
		catch (Exception e) {
			logger.info(PC2LogCategory.Examiner, subCat,
					"Comparison Evaluator couldn't obtain SDP field for comparison operation.", e);

		}
		return result;
	}

	/**
	 * Parses a SDP message for specific headers.
	 * 
	 * @param result - a list of headers that meet the criteria
	 * @param msg - the specific message to parse.
	 * @param ref - the criteria being sought 
	 */
	private void getSDPHeaders(LinkedList<String> result, String sdp, SDPRef ref) {
		if (sdp != null ) {
			// test if we are looking for a parameter,
			// header or the entire message
			String boundary = null;
			if (sdp.startsWith("--")) {
				int end = sdp.indexOf("\r\n");
				boundary = sdp.substring(2, end);
			}
			if (ref.getHeader() != null) {
				String hdrInstance = ref.getHdrInstance();
				int [] hdrLocation = sdpLocator.locateSDPHeader(sdp, hdrInstance, 
						ref.getBodyInstance(), boundary, ref.getHeader());
				// Locate the end of the header, then insert data just prior to the "\r\n"
				if (hdrLocation[0] != -1 && hdrLocation[1] != 1) {
					if (ref.getParameter() != null) {
						int [] paramLocation = sdpLocator.locateSDPParam(ref.getHeader(), 
								ref.getParameter(), hdrLocation, sdp);
						if (paramLocation[0] != -1 && paramLocation[1] != -1) {
							String value = sdp.substring(paramLocation[0],paramLocation[1]);
							String hdr = ref.getHeader();
							if ((hdr.equals(SDPConstants.AUDIO) ||
									hdr.equals(SDPConstants.VIDEO)) && 
									hdrInstance.equals(MsgQueue.ANY) && 
									ref.getParameter().equals(SDPConstants.PAYLOAD_TYPE)) {
								StringTokenizer tokens = new StringTokenizer(value, " ");
								while (tokens.hasMoreTokens()) {
									String token = tokens.nextToken();
									logger.debug(PC2LogCategory.Examiner, subCat,
										"Examiner returning param value=" + token 
										+ " for ref=" + ref);
									result.add(token);
								}
							}
							else {
								logger.debug(PC2LogCategory.Examiner, subCat,
									"Examiner returning param value=" + value 
									+ " for ref=" + ref);
								result.add(value);
							}
						}
					}
					else {
						String value = sdp.substring(hdrLocation[0],hdrLocation[1]);
						
						logger.debug(PC2LogCategory.Examiner, subCat,
								"Examiner returning param value=" + value 
								+ " for ref=" + ref);
						result.add(value);
					}
				}
			}
		}
	}

	/**
	 * Retrieves the correct SDP string to use in a comparison based
	 * upon the information in the reference parameter.
	 * 
	 * @param ref - the SDP msg_ref information
	 * @param event - the current event.
	 * @return - a list of strings matching the information defined by the 
	 * 			reference.
	 */
	private LinkedList<String> getSIPBodyString(SIPBodyRef ref,  MsgEvent event) {
		// First obtain all the messages to operate upon
		// The messages are either the event if the msg_instance
		// in ref is set to current or one or more entries in the 
		// message queue if the msg_instance is another value.
		LinkedList<String> result = new LinkedList<String>();
		try {
			LinkedList<String> msgRefs = null;
			if (ref.isSIPResponseRef()) 
				msgRefs = getResponseRef(ref, event);
			else 
				msgRefs = getRequestRef(ref, event);

			ListIterator<String> iter = msgRefs.listIterator();
			while (iter.hasNext()) {
				String resp = iter.next();
				String ct = "Content-Type: ";
				int ctBegin = resp.indexOf(ct);
				if (ctBegin != -1) {
					int ctEnd = resp.indexOf("\r\n", ctBegin);
					if (ctEnd != -1) {
						String ctHdr = resp.substring(ctBegin, ctEnd);
						boolean hasType = ctHdr.contains(ref.getType());
						if (!hasType && ref.isTextBody())
							hasType = ctHdr.contains("text/plain");
						if (!hasType && ctHdr.contains("multipart")) {
							int index = resp.indexOf("\r\n\r\n");
							if (index != -1) {
								String body = resp.substring(index);
								hasType = body.contains(ref.getType());
							}
						}
						if (hasType) {
							int bodyBegin = resp.indexOf("\r\n\r\n", ctEnd);
							if (bodyBegin != -1) {
								// Adjust the string past double CRLF
								String msg = resp.substring(bodyBegin+4);
								//parseSDPHeaders(result, msg, ref);
								String type = ref.getType();
								if (type.equals(MsgRef.SIP_MSG_TYPE) && ref.isTextBody())
									type = "text";
								if (ref.hasAncestor()) {
									String value = sipLocator.getXMLAncestor(msg, 
											ref.getAncestor(), type, ref.getHeader());
									result.add(value);
								}
								else {
									int [] location = sipLocator.locateSIPBody(type,
											ref.getHeader(), ref.getHdrInstance(),
											ref.getParameter(), ref.isXMLValue(),
											msg, false);
									if (location[0] != -1 && location[1] != -1) {
										String value = msg.substring(location[0], location[1]);

										result.add(value);
									}
								}
							}
						}
					}
				}
			}

		}
		catch (Exception e) {
			logger.info(PC2LogCategory.Examiner, subCat,
					"Comparison Evaluator couldn't obtain SDP field for comparison operation.", e);

		}
		return result;
	}
	/**
	 * Retrieves the correct SIP string to use in a comparison based
	 * upon the information in the reference parameter.
	 * 
	 * @param ref - the SIP msg_ref information
	 * @param event - the current event.
	 * @return - a list of strings matching the information defined by the 
	 * 			reference.
	 */
	private LinkedList<String> getSIPString(SIPRef ref,  MsgEvent event) {
		// First obtain all the messages to operate upon
		// The messages are either the event if the msg_instance
		// in ref is set to current or one or more entries in the 
		// message queue if the msg_instance is another value.
		LinkedList<String> result = new LinkedList<String>();
		//boolean generic = false;
		boolean nullMethod = (ref.getMethod() == null);
		boolean nullParameter = (ref.getParameter() == null);
		String hdr = ref.getHeader();
		boolean nullHdr = (hdr == null);
// BRKPT
//if (ref.getHeader().equals("Require")) {
//	int glh = 0;
//}
		if (ref.isSIPResponseRef()) {
			LinkedList<String> resps = getResponseRef(ref, event);
			ListIterator<String> iter = resps.listIterator();
			while (iter.hasNext()) {
				String resp = iter.next();
				if (ref.getMethod().equals("Response") ||
						(nullMethod && nullParameter)) {
					if (nullParameter || ref.isReferenceOnEvent()) {
						if (nullHdr || ref.isReferenceOnEvent())
							result.add(resp);
						else 
							getSIPHeaders(result, resp, ref);
					}
					else 
						getSIPHeaders(result, resp, ref);
				}	
				else if (hdr != null && ref.isReferenceOnEvent()) {
					result.add(resp);
				}
				else {
					getSIPHeaders(result, resp, ref);
				}
			}
		}
		else {
			LinkedList<String> reqs = getRequestRef(ref, event);
			ListIterator<String> iter = reqs.listIterator();
			while (iter.hasNext()) {
				String req = iter.next();
				if (ref.getMethod().equals("Request") ||
						(nullMethod && nullParameter)) {
					if (nullParameter || ref.isReferenceOnEvent()) {
						if (nullHdr || ref.isReferenceOnEvent())
							result.add(req);
						else
							getSIPHeaders(result, req, ref);
					}
					else
						getSIPHeaders(result, req, ref);
				}
				else if (hdr != null && ref.isReferenceOnEvent()) {
					result.add(req);
				}
				else {
					getSIPHeaders(result, req, ref);
				}	
			}
		}
		return result;
	}

	/**
	 * Parses a SIP message for specific headers.
	 * 
	 * @param result - a list of headers that meet the criteria
	 * @param msg - the specific message to parse.
	 * @param ref - the criteria being sought 
	 */
	private void getSIPHeaders(LinkedList<String> result, String msg, SIPRef ref) {
		if (msg != null ) {
			// test if we are looking for a parameter,
			// header or the entire message
			if (ref.getMethod() != null) {
				// We must be looking for a parameter
				// 1. remove unwanted headers
				// 2. finally get all of the matching parameters for header
				//    and parameter.

				String hdr = ref.getHeader();
				boolean loop = false;
				int offset = 0;
				if (ref.getHdrInstance().equals(MsgQueue.ANY)) 
					loop = true;
				do {
					int [] hdrLocation = sipLocator.locateSIPHeader(hdr, 
							ref.getHdrInstance(), msg.substring(offset), 
							false, false);

					if (ref.getParameter() != null &&
							hdrLocation[0] != -1 && 
							hdrLocation[1] != -1) {
							
						boolean paramLoop = false;
						int comma = msg.indexOf(",", hdrLocation[0]);
						int end = hdrLocation[1];
						
						if (SIPConstants.multipleHeadersAllowed(hdr) && comma != -1) {
							paramLoop = true;
							// As a sanity make sure comma is < end
							if (comma != -1 && comma < end) {
								paramLoop = true;
								hdrLocation[1] = comma;
							}
						}
						do {
							int [] paramLocation = sipLocator.locateSIPParameter(hdr, 
									ref.getParameter(), hdrLocation, msg);
							if (paramLocation[0] != -1 && 
									paramLocation[1] != -1 && 
									paramLocation[2] != -1) {
								String value = msg.substring(paramLocation[1], paramLocation[2]);
								// If the header does not allow multiple instances check the qop parameter
								// for quotes.
								if (!SIPConstants.multipleHeadersAllowed(hdr) 
										&& ref.getParameter().equals("qop") 
										&& !value.contains(","))
									value = unq(value);
								// Lastly see if the reference was an add_ref or subtract_ref
//								if (ref.isArithmeticRef()) {
//									String sign = null;
//									if (ref.isAddRef())
//										sign = " + ";
//									else if (ref.isSubRef())
//										sign = " - ";
//									String logMsg = "Performing arimetic change on value original=" 
//										+ value + sign + ref.getArithmeticMod();
//									logger.info(PC2LogCategory.Examiner, subCat, logMsg);
//									value = ref.updateArithmeticRef(value);
//								}
								logger.debug(PC2LogCategory.Examiner, subCat,
										"Examiner returning param value=[" + value 
										+ "] for ref=" + ref);
								result.add(value);
								
								if (hdrLocation[1] >= end)
									paramLoop = false;
								else {
									hdrLocation[0] = hdrLocation[1]+1;
									comma = msg.indexOf(",", hdrLocation[0]);
									if (SIPConstants.multipleHeadersAllowed(hdr)) {
										// As a sanity make sure comma is < end
										if (comma != -1 && comma < end) {
											hdrLocation[1] = comma;
										}
										else 
											// We should be at the end
											hdrLocation[1] = end;
									}
									else 
										// We should be at the end
										hdrLocation[1] = end;
								}
							}
							else
								paramLoop = false;
						} while (paramLoop);
					}
					else if (hdrLocation[0] != -1 && hdrLocation[1] != -1){
						String value = msg.substring(hdrLocation[0], hdrLocation[1]);
						logger.debug(PC2LogCategory.Examiner, subCat,
								"Examiner returning header value=[" + value 
								+ "] for ref=" + ref);
						// Check to see if the returned value contains multiple headers
						if (loop && SIPConstants.multipleHeadersAllowed(hdr)) {
							StringTokenizer tokens = new StringTokenizer(value, ",");
							while (tokens.hasMoreTokens()) {
								result.add(tokens.nextToken());
							}
						}
						else
							result.add(value);
					}
					else if (hdrLocation[0] == -1) {
						loop = false;
					}
					offset = hdrLocation[1];

				} while (loop && offset != -1);
			}
		}
	}
	/**
	 * Retrieves a list of Stun messages from the message queue that meets
	 * the criteria defined by the reference.
	 * 
	 * @param ref - the msg_ref criteria to find.
	 * @param event - the current event.
	 * @return - a list of STUN messages that fulfills the criteria in the
	 * 			reference.
	 */
	private LinkedList<String> getStunString(StunRef ref,  MsgEvent event) {
		LinkedList<String> ll = new LinkedList<String>();
		boolean nullHeader = (ref.getHeader() == null);
		if (ref.getMsgInstance().equals(MsgQueue.CURRENT)) {
			if (event instanceof StunMsg)	 {
				String msgType = ((StunMsg)event).getMsgType();
				if (msgType != null) {
					if (nullHeader)
						ll.add(msgType);
					else 
						parseStunHeader(ll, ref, (StunMsg)event);
				}

			}
		}	
		else if (ref.getMsgInstance().equals(MsgQueue.ANY)) {
			LinkedList<MsgEvent> searchResults = q.findAll(ref.getUID(), ref.getMethod(), 
					fsm.getCurrentMsgQueueIndex());
			ListIterator<MsgEvent> iter = searchResults.listIterator();
			while(iter.hasNext()) {
				Object element = iter.next();
				if (element instanceof StunMsg) {
					String msgType = ((StunMsg)element).getMsgType();
					if (msgType != null) {
						if (nullHeader)
							ll.add(msgType);
						else 
							parseStunHeader(ll, ref, (StunMsg)element);
					}
				}
			}
		}
		else {
			MsgEvent msgEvent = q.find(ref.getUID(), ref.getMethod(), ref.getMsgInstance(), 
					fsm.getCurrentMsgQueueIndex());
			if (msgEvent instanceof StunMsg) {
				String msgType = ((StunMsg)msgEvent).getMsgType();
				if (msgType != null) {
					if (nullHeader) 
						ll.add(msgType);
					else
						parseStunHeader(ll, ref, (StunMsg)msgEvent);
				}
			}
		}
		return ll;
	}
	
	/**
	 * This method converts the operator value to a symbol if appropriate. e.g.
	 * eq gets converted to ==.
	 * @param op
	 * @return
	 */
	private String getSymbol(String op) {
		if (op.equals("eq"))
			return " == ";
		else if (op.equals("neq"))
			return " != ";
		else if (op.equals("lt"))
			return " < ";
		else if (op.equals("gt"))
			return " > ";
		else if (op.equals("lte"))
			return " <= ";
		else if (op.equals("gte"))
			return " >= ";
		else if (op.equals("contains"))
			return " contains ";
		else if (op.equals("dnc"))
			return " does not contain ";
		else if (op.equals("count"))
			return " == ";
		else if (op.equals("digest"))
			return " digest ";
		else if (op.equals("ipv4"))
			return " is ipv4 ";
		else if (op.equals("ipv6"))
			return " is ipv6 ";
		else if (op.equals("startsWith"))
			return " startsWith ";
		else if (op.equals("endsWith"))
			return " endsWith ";
		else 
			return op;
	}

	/**
	 * Retrieves a list of Utility messages from the message queue that meets
	 * the criteria defined by the reference.
	 * 
	 * @param ref - the msg_ref criteria to find.
	 * @param event - the current event.
	 * @return - a list of Utility messages that fulfills the criteria in the
	 * 			reference.
	 */
	private LinkedList<String> getUtilityString(UtilityRef ref,  MsgEvent event) {
		LinkedList<String> ll = new LinkedList<String>();
		boolean nullHeader = (ref.getHeader() == null);
		if (ref.getMsgInstance().equals(MsgQueue.CURRENT)) {
			if (event instanceof UtilityMsg) {
				String msgType = ((UtilityMsg)event).getEventType();
				if (msgType != null) {
					if (nullHeader)
						ll.add(msgType);
					else 
						getUtilityHeader(ll, ref, (UtilityMsg)event);
				}

			}

		}	
		else if (ref.getMsgInstance().equals(MsgQueue.ANY)) {
			LinkedList<MsgEvent> searchResults = q.findAll(ref.getUID(), ref.getMsgType(), 
					fsm.getCurrentMsgQueueIndex());
			ListIterator<MsgEvent> iter = searchResults.listIterator();
			while(iter.hasNext()) {
				Object element = iter.next();
				if (element instanceof UtilityMsg) {
					String msgType = ((UtilityMsg)element).getEventType();
					if (msgType != null) {
						if (nullHeader)
							ll.add(msgType);
						else 
							getUtilityHeader(ll, ref, (UtilityMsg)event);
					}
				}
			}
		}
		else {
			MsgEvent msgEvent = q.find(ref.getUID(), ref.getMsgType(), ref.getMsgInstance(), 
					fsm.getCurrentMsgQueueIndex());
			if (msgEvent instanceof UtilityMsg) {
				String msgType = ((UtilityMsg)msgEvent).getEventType();
				if (msgType != null) {
					if (nullHeader) 
						ll.add(msgType);
					else
						getUtilityHeader(ll, ref, (UtilityMsg)event);
				}
			}
		}
		return ll;
	}
	
	/**
	 * Parses a Utiility message for specific header value.
	 * 
	 * @param result - a list of header values that meet the criteria
	 * @param ref - the criteria being sought 
	 * @param msg - the specific message to parse.
	 * 
	 */
	private void getUtilityHeader(LinkedList<String> result, UtilityRef ref, UtilityMsg msg) {
		String value = utilLocator.locateUtilityValue(ref, msg);
		if (value != null) {
			value = checkForBinaryRef(ref, value);
			result.add(value);
		}
	}
	
	/**
	 * Retrieves the value stored in the global variables.
	 * 
	 * @param result - a list of header values that meet the criteria
	 * @param ref - the criteria being sought 
	 * @param msg - the specific message to parse.
	 * 
	 */
	private LinkedList<String> getVarRefString(VarRef ref,  MsgEvent event) {
		LinkedList<String> ll = new LinkedList<String>();
		GlobalVariables gv = GlobalVariables.getInstance();
		Variable var = gv.get(ref.getName());
		Integer [] indexes = ref.getIndexes();
		if (var != null) {
			if (indexes != null) {
				String value = var.getElement(indexes);
				if (value != null) {
					value = checkForBinaryRef(ref, value);
					ll.add(value);
				}
				else {
					ll.add(value);
				}
			}
			else {
				String value = ref.resolve(var);
				if (value != null) {
					value = checkForBinaryRef(ref, value);
					ll.add(value);
				}
				else {
					ll.add(value);
				}
			}
		}
		return ll;
	}
	
	/** 
	 * Evaluates if the value parameter is a calendar date
	 * 
	 * @param value - the item to evaluate for a date value
	 * @param ref - the reference object requested for evaluation
	 * @param dateFormat - the format to compare the date
	 * 
	 * @return true if a date, false otherwise
	 */
	private boolean isDate(String value, Reference ref, String dateFormat) {
		boolean result = false;

		String dateStr = resolveSubstring(value, ref);

		Date date = null;
		if (dateFormat == null)
			date = DateUtils.parse(dateStr);
		else
			date = DateUtils.parse(dateStr, dateFormat);

		if (date != null) 
			result = true;



		logger.info(PC2LogCategory.Examiner, subCat,
				logLabel + " if " + dateStr + "is a valid date is " + result + ".");
		return result;
	}
	
	private boolean isEqual(LinkedList<String> leftSide, Reference left,  
			LinkedList<String> rightSide, Reference right, String operator, 
			boolean useCase, boolean negative) {
		boolean result = false;


		String l = resolveSubstring(leftSide.getFirst(), left);
		String r = resolveSubstring(rightSide.getFirst(), right);
		if (l != null && 
				r != null && 
				l.startsWith(LAQUOT) && 
				l.endsWith(RAQUOT) &&
				r.startsWith(LAQUOT) && 
				r.endsWith(RAQUOT)) {
			result = compareURI(l, r, useCase);
		}
		else {
			if (l == null || r == null) {
				result = false;
			} else  {
				if (useCase) {
					result = l.equals(r);
				}
				else {
					result = l.equalsIgnoreCase(r);
				}
			}
		}

		// If the negative flag is set change the result to the opposite value
		if (negative){
			result = !result;
		}
		logger.info(PC2LogCategory.Examiner, subCat,
				logLabel + " if " + l + getSymbol(operator) 
				+ r + " is " + result 
				+ "   caseSensitive=" + useCase + ".");
	
		return result;

	}

	private boolean isAnyEqual(LinkedList<String> leftSide, Reference left,  
		LinkedList<String> rightSide, Reference right, String operator, 
		boolean useCase, boolean negative) {
		
		boolean result = false;
		ListIterator<String> liter = leftSide.listIterator();
		while(liter.hasNext() && !result) {
			String l = liter.next();
			ListIterator<String> riter = rightSide.listIterator();
			while (riter.hasNext() && !result) {
				String r = resolveSubstring(riter.next(), right);
				if (l != null && 
						r != null && 
						l.startsWith(LAQUOT) && 
						l.endsWith(RAQUOT) &&
						r.startsWith(LAQUOT) && 
						r.endsWith(RAQUOT)) {
					result = compareURI(l, r, useCase);
				}
				else {
					if (l == null || r == null) 
						result = false;
					else  {
						if (useCase) {

							result = l.equals(r);
						}
						else {
							result = l.equalsIgnoreCase(r);

						}
					}

				}
				
				// If the negative flag is set change the result to the opposite value
				if (negative){
					result = !result;
				}
				
				logger.info(PC2LogCategory.Examiner, subCat,
						logLabel + " if " + l + getSymbol(operator) 
						+ r + " is " + result 
						+ "   caseSensitive=" + useCase + ".");
			}
		}
		return result;
	}
	
	/**
	 * Determines if the header instance value of the reference is 
	 * set to "any" or not.
	 * 
	 * @param left - the reference to check
	 * @return
	 */
	private boolean isHdrInstanceAny(Reference ref) {
		if (ref instanceof MsgRef) {
			if (((MsgRef)ref).getHdrInstance().equals(MsgQueue.ANY)) {
				return true;
			}
		}
		return false;
	}
	
	/** 
	 * Evaluates if the value parameter is an IPv4 address or not
	 * 
	 * @param value - the item to evaluate for IPv4 address
	 * @param ref - the reference object requested for evaluation
	 */
	private boolean isIPv4Address(String value, Reference ref) {
		boolean result = false;
		String ip = null;
		
		ip = resolveSubstring(value, ref);
		if (ip != null) {
			try {
				InetAddress ia = InetAddress.getByName(ip);
				if (ia instanceof Inet4Address)
					result = true;
			}
			catch (UnknownHostException uhe) {
			}
		}
	
		logger.info(PC2LogCategory.Examiner, subCat,
				logLabel + " if " + ip + "is an IPv4 address is " + result + ".");

		return result;
	}
	
	/** 
	 * Evaluates if the value parameter is an IPv6 address or not
	 * 
	 * @param value - the item to evaluate for IPv6 address
	 * @param ref - the reference object requested for evaluation
	 */
	private boolean isIPv6Address(String value, Reference ref) {
		boolean result = false;
		String ip = null;
		
		ip = resolveSubstring(value, ref);
		if (ip != null) {
			try {
				InetAddress ia = InetAddress.getByName(ip);

				if (ia instanceof Inet6Address)
					result = true;
			}
			catch (UnknownHostException uhe) {
			}
		}

		logger.info(PC2LogCategory.Examiner, subCat,
				logLabel + " if " + ip + "is an IPv6 address is " + result + ".");
		
		return result;
	}
	
	private void noValue(LinkedList<String> leftSide, LinkedList<String> rightSide, String operator, boolean result) {
		logger.info(PC2LogCategory.Examiner, subCat,
				logLabel + " if " + leftSide + getSymbol(operator)
				+ rightSide + " is " + result +
		" because the right operand is null.");
	}
	
	/**
	 * Parses a RTP message for specific headers and parameters.
	 * 
	 * @param result - a list of headers that meet the criteria
	 * @param ref - the criteria being sought 
	 * @param msg - the specific message to parse.
	 * 
	 */
	private void parseRTPHeader(LinkedList<String> result, RTPRef ref, RTPMsg msg) {
		String hdr = ref.getHeader();
		String param = ref.getParameter();
		if (ref.isReferenceOnEvent()) {
			String value = refLocator.getEventReference(hdr, param, msg);
			if (value != null)
				result.add(value);
		}	
	}
	
	/**
	 * Parses a Stun message for specific headers and parameters.
	 * 
	 * @param result - a list of headers that meet the criteria
	 * @param ref - the criteria being sought 
	 * @param msg - the specific message to parse.
	 * 
	 */
	private void parseStunHeader(LinkedList<String> result, StunRef ref, StunMsg msg) {
		String hdr = ref.getHeader();
		String param = ref.getParameter();
		if (ref.isReferenceOnEvent()) {
			String value = refLocator.getEventReference(hdr, param, msg);
			if (value != null)
				result.add(value);
		}
		else if (hdr != null) { 
			String value =  stunLocator.getSIPParameter(hdr, param, 
					ref.getHdrInstance(), msg.getEvent().getEvent());
			if (value != null)
				result.add(value);
		}
	}
	
	private LinkedList<String> performArithmeticUpdate(LinkedList<String> list, CaptureRef ref) {
		ListIterator<String> iter = list.listIterator();
		LinkedList<String> updatedList = new LinkedList<String>();
		while (iter.hasNext()) {
			String value = iter.next();
			String add = ref.getAdd();
			if (add != null) {
				String orig = value;
				try {
					// We want to allow for the possibility of a decimal value instead of an integer.
					if (add.contains(".")) {
						Double val = Double.parseDouble(value);
						Double adjust = Double.parseDouble(add);
						value = Double.toString((val + adjust));
					}
					else { // Treat value as integer
						Long val = Long.parseLong(value);
						Long adjust = Long.parseLong(add);
						value = Long.toString((val + adjust));
					}
				}
				catch (NumberFormatException nfe) {
					logger.error(PC2LogCategory.Examiner, subCat, "An add value could not be performed for reference=" + ref);
				}
				String logMsg = "Performing arithmetic change on value original=" 
						+ orig + " + " + add + " = " + value;
						logger.info(PC2LogCategory.Examiner, subCat, logMsg);
			}
			updatedList.add(value);
		}
		return updatedList;
	}
	
	private LinkedList<String> performArithmeticUpdate(LinkedList<String> list, Reference ref) {
		MsgRef l = (MsgRef)ref;
		ListIterator<String> iter = list.listIterator();
		LinkedList<String> updatedList = new LinkedList<String>();
		while (iter.hasNext()) {
			String value = iter.next();
			String sign = null;
			if (l.isAddRef())
				sign = " + ";
			else if (l.isSubRef())
				sign = " - ";
			String logMsg = "Performing arithmetic change on value original=" 
				+ value + sign + l.getArithmeticMod();
			logger.info(PC2LogCategory.Examiner, subCat, logMsg);
			value = l.updateArithmeticRef(value);
			updatedList.add(value);
		}
		return updatedList;
	}

	/**
	 * Determines if the reference needs to use a substring of the 
	 * reference or not.
	 * 
	 * @param left - the reference to check
	 * @return
	 */
	private String resolveSubstring(String value, Reference ref) {
		String result = value;
		if (ref instanceof MsgRef || ref instanceof CaptureRef) {
			MsgRef mr = (MsgRef)ref;
			if (mr.getFirstChar() != null) {
				try {
					int first = -1;
					String start = mr.getFirstChar();
					if (mr.isFirstAnOffsetFromLength()) {
						first = value.length() - Integer.parseInt(start);
					}
					else 
						first = Integer.parseInt(start);
					String last = mr.getLastChar();
					int end = -1;
					if (last != null) {
						if (mr.isLastAnOffsetFromLength()) {
							end = value.length() - Integer.parseInt(last);
						}
						else 
							end = Integer.parseInt(last);
						
						result = value.substring(first, end);
					}
					else 
						result = value.substring(first);
				}
				catch (NumberFormatException nfe) {
					logger.error(PC2LogCategory.Examiner, subCat, "A substring value could not be converted for reference=" + mr);
					return result;
				}
			}
			
			// This indicates that the user wants the length of the value instead of 
			// the actual value.
			if (mr.useLength()) {
				result = Integer.toString(result.length());
			}
			if (mr.getEscape()) {
				result = result.replaceAll("=", "%3d");
				result = result.replaceAll(";", "%3b");
				result = result.replaceAll("@", "%40");
				result = result.replaceAll("\"", "%22");
			}
			// See if this is an IPv6 address that needs to be converted to long-form for comparison
			if (result != null) {
				if (result.startsWith("[") && result.endsWith("]") && result.contains(":")) {
					if (result.contains("%")) {
						int index = result.indexOf("%");
						if (index > -1) {
							result = Conversion.ipv6LongForm(result.substring(1,index));
						}
						else
							result = Conversion.ipv6LongForm(result.substring(1,result.length()-1));
					}
					else
						result = Conversion.ipv6LongForm(result.substring(1,result.length()-1));	
				}
				else if (result.startsWith("<") && result.endsWith(">") && result.contains("::")) {
					// There better be a square brace, otherwise don't change data
					int ob = result.indexOf("[");
					if (ob != -1){
						int cb = result.indexOf("]", ob);
						int index = result.indexOf("%");
						if (index > -1) {
							String ip = Conversion.ipv6LongForm(result.substring(ob+1,index));
							result = result.substring(0,ob+1) + ip + result.substring(index);
						}
						else {
							String ip = Conversion.ipv6LongForm(result.substring(ob+1,cb));
							result = result.substring(0,ob+1) + ip + result.substring(cb); 
						}
					}

				}
				else if ((result.startsWith("sip:") || result.startsWith("sips:") || result.startsWith("tel:")) && result.contains("::")) {
					// There better be a square brace, otherwise don't change data
					int ob = result.indexOf("[");
					if (ob != -1){
						int cb = result.indexOf("]", ob);
						int index = result.indexOf("%");
						if (index > -1) {
							String ip = Conversion.ipv6LongForm(result.substring(ob+1,index));
							result = result.substring(0,ob+1) + ip + result.substring(index);
						}
						else {
							String ip = Conversion.ipv6LongForm(result.substring(ob+1,cb));
							result = result.substring(0,ob+1) + ip + result.substring(cb); 
						}
					}

				}
				
			}

		}

		// Now see if the reference is a CaptureRef
		if (ref instanceof CaptureRef) {
			CaptureRef cr = (CaptureRef)ref;
			if (cr.getConverter() != null) {
				String how = cr.getConverter();
				if (how.equalsIgnoreCase("string")) {
					if (value != null)
						result = Conversion.hexStringToString(value);
				}
				else {
					logger.error(PC2LogCategory.Examiner, subCat, 
							"The system does not support converting the reference to type (" + how + ").");
				}
			}
			else if (cr.getAdd() != null) {
				try {
					// We want to allow for the possibility of a decimal value instead of an integer.
					String add = cr.getAdd();
					if (add.contains(".")) {
						Double val = Double.parseDouble(value);
						Double adjust = Double.parseDouble(cr.getAdd());
						result = Double.toString((val + adjust));
					}
					else { // Treat value as integer
						Long val = Long.parseLong(value);
						Long adjust = Long.parseLong(cr.getAdd());
						result = Long.toString((val + adjust));
					}
				}
				catch (NumberFormatException nfe) {
					logger.error(PC2LogCategory.Examiner, subCat, "An add value could not be performed for reference=" + cr);
					return result;
				}
			}
			else if (result != null && result.contains("::")) {
				try {
					InetAddress ia = InetAddress.getByName(result);
                    
					if (ia instanceof Inet6Address) {
						String ip = Conversion.ipv6LongForm(result);
						result = ip;						
					}
				
				}
				catch (UnknownHostException uhe) {

				}
			}
				
		}
		return result;
	}
	
	private void unevenEqual(LinkedList<String> leftSide, LinkedList<String> rightSide, String operator, boolean result) {
		String l = (leftSide.size() >= 1) ? leftSide.getFirst() : null;
		String r = (rightSide.size() >= 1) ? rightSide.getFirst() : null;
		logger.info(PC2LogCategory.Examiner, subCat,
				logLabel + " if " + l + getSymbol(operator) 
				+ r + " is " + result + ".");
	}
	
	
	/**
	 * This utility method removes a leading and trailing quotation-mark(") from a 
	 * String.
	 * 
	 * @param field
	 * @return
	 */
	private String unq(String field) {
		String result = "";
		if (field != null) {
			StringTokenizer tokens = new StringTokenizer(field,"\"");

			if (tokens.countTokens() > 1) {
				int count = tokens.countTokens();
				for (int i = 0; i < count; i++) {
					result += tokens.nextToken();
				}
				return result;
			}
			else if (tokens.countTokens() == 1) {
				return tokens.nextToken();
			}
		}
		return result;
	}

}
