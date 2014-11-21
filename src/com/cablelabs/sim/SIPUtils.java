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

import gov.nist.core.NameValue;
import gov.nist.javax.sip.address.SipUri;
import gov.nist.javax.sip.header.ParameterNames;

import java.security.MessageDigest;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;

import javax.sip.InvalidArgumentException;
import javax.sip.SipProvider;
import javax.sip.address.Address;
import javax.sip.address.AddressFactory;
import javax.sip.address.SipURI;
import javax.sip.address.URI;
import javax.sip.header.AcceptHeader;
import javax.sip.header.AuthenticationInfoHeader;
import javax.sip.header.AuthorizationHeader;
import javax.sip.header.CSeqHeader;
import javax.sip.header.CallIdHeader;
import javax.sip.header.ContactHeader;
import javax.sip.header.ContentTypeHeader;
import javax.sip.header.EventHeader;
import javax.sip.header.ExpiresHeader;
import javax.sip.header.FromHeader;
import javax.sip.header.Header;
import javax.sip.header.HeaderFactory;
import javax.sip.header.MaxForwardsHeader;
import javax.sip.header.MimeVersionHeader;
import javax.sip.header.ProxyAuthenticateHeader;
import javax.sip.header.ProxyAuthorizationHeader;
import javax.sip.header.RSeqHeader;
import javax.sip.header.RecordRouteHeader;
import javax.sip.header.ReferToHeader;
import javax.sip.header.RequireHeader;
import javax.sip.header.RouteHeader;
import javax.sip.header.SubscriptionStateHeader;
import javax.sip.header.SupportedHeader;
import javax.sip.header.TargetDialogHeader;
import javax.sip.header.ToHeader;
import javax.sip.header.ViaHeader;
import javax.sip.header.WWWAuthenticateHeader;
import javax.sip.message.Message;

import com.cablelabs.common.Conversion;
import com.cablelabs.fsm.Extension;
import com.cablelabs.fsm.SDPConstants;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

/**
 * 
 * A wrapper class for creating the standard SIP headers and
 * addresses that the application layer has requested the SIP stack
 * send to a target network element. The class helps construct the 
 * messages based upon the definitions of the network elements
 * within the application layer as well as the original request
 * in the case of responses.
 * 
 * @author ghassler
 *
 */
public class SIPUtils {

	/**
	 * The SIP stacks address factory.
	 */
	private static AddressFactory addressFactory = null;

	/**
	 * The SIP stacks header factory.
	 */
	private static HeaderFactory headerFactory = null;

	/** 
	 * The configuration settings in use during the current
	 * test case.
	 */
	private static SystemSettings ss = null;

	/** 
	 * The last request sequence number used by the 
	 * platform in a SIP request message.
	 */
	private int requestSeqNum = 1000;

	/**
	 * The last reliable prack sequence number used by the platform
	 * in a SIP PRACK message.
	 */
	private int prackSeqNum = 0;
	
	/**
	 * The RFC 3261 magic cookie.
	 */
	protected static final String mc = "z9hG4bK";

	/**
	 * A random number generator for constructing
	 * opaque parameter in WWWAuthenticate Headers.
	 */
	// OPAQUE parameter is no longer necessary according to Stuart Hoggan
	//	private Random rand = null;

	/**
	 * MessageDigest for calculating MD5 hash
	 */
	private MessageDigest digester = null;
	
	private LogAPI logger = LogAPI.getInstance();
	
	private final static String subCat = SIPDistributor.subCat;
	
	private SIPManufacturer manufacturer = null;
	
	private final static String DEFAULT_PORT = "12345";
	
	private static long BRANCH = 0;
	/**
	 * Constructor.
	 *
	 */
	protected SIPUtils(SIPManufacturer m) {
		ss = SystemSettings.getInstance();
		addressFactory = SIPDistributor.getAddressFactory();
		headerFactory = SIPDistributor.getHeaderFactory();
		this.manufacturer = m;
		// OPAQUE parameter is no longer necessary according to Stuart Hoggan
		// rand = new Random();
		try {
			digester = MessageDigest.getInstance("MD5");
		}
		catch (Exception e) {

		}
	}

	/**
	 * Provides a common routine to convert a buffer
	 * to a string representation for displaying to the user.
	 * 
	 * 
	 * @param buffer - the bytes to convert
	 * @return - the string representation
	 */
	private StringBuffer asHexString(byte [] buffer) {
		StringBuffer iStr = new StringBuffer();
		for(int i = 0; i < buffer.length; i++) {
			if((buffer[i]&0xFF) <= 15) {
				iStr.append("0");
			}
			iStr.append(Integer.toHexString(buffer[i]&0xff).toLowerCase());
		}
		return iStr;
	}

	/**
	 * Copies the Record-Route header information in the list arguement to
	 * the Message argument's Route Header
	 * 
	 * @param rrIter - the list of Record-Route headers that should be 
	 * 		converted to a Route header and added to the msg argument
	 * @param msg - the Message to add the Route header(s) to.
	 * 
	 * @throws ParseException
	 */
	protected void addRouteFromRecordRoute(ListIterator<Header> rrIter, Message msg) throws ParseException {
		LinkedList<RouteHeader> rtes = new LinkedList<RouteHeader>();
		while (rrIter.hasNext()) {
			RecordRouteHeader rrh = (RecordRouteHeader)rrIter.next();
			String transport = rrh.getParameter("transport");
			RouteHeader rh = createRouteHeader(rrh, transport);
			rtes.addFirst(rh);
		}
		ListIterator<RouteHeader> iter = rtes.listIterator();
		while (iter.hasNext()) {
			msg.addHeader(iter.next());
		}
	}
	
	protected SipURI createSipURI(Properties p, String name, int port ) throws ParseException {
//		String name = p.getProperty(SettingConstants.USER_NAME);
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
		
		boolean addPort = false;
		String address = null;
		if (addrType == null || 
				addrType.equals(SettingConstants.IP) ||
				addrType.equals(SettingConstants.LAB_FQDN)) {
			address = p.getProperty(SettingConstants.IP); 
			if (Conversion.isIPv6Address(address)) {
				String zone = p.getProperty(SettingConstants.IPv6_ZONE);
				address = Conversion.addZone(address, zone); 
			}
			addPort = true;
		}
		else if (addrType.equals(SettingConstants.FQDN)) {
			address = p.getProperty(SettingConstants.DOMAIN);
		}

		SipURI uri =
			addressFactory.createSipURI(name, address);
		
		if (uri != null && 
				port > 0 && 
				addPort) {
			uri.setPort(port);
		}
		return uri;
	}
	
	protected String calculateRspAuth(AuthorizationHeader ah, Properties p) {
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
		if (ah != null && p != null) {
			// Based upon 24.229 REQ 8716, the username field must be the private user identifier
			String uri = ah.getURI().toString();
			String user = p.getProperty(SettingConstants.PRUI);
			// For realm we use the value in the domain setting
			String realm = p.getProperty(SettingConstants.DOMAIN);
			String passwd = p.getProperty(SettingConstants.PASSWORD);
			String nonce = ah.getNonce();
			String nonceCount = ah.getNonceCount(); 
			String cnonce = ah.getCNonce();
			if (cnonce == null)
				cnonce ="";
			String s1 = user + ":" + realm + ":" + passwd;
			String s2 = ":" + uri.toString();
			String qop = ah.getQop();
			if (qop == null) {
				logger.warn(PC2LogCategory.SIP, subCat, 
					"The WWW-Authenticate header received in the resposne message doesn't contain a "
						+ "qop parameter.\nUsing auth in Authentication-Info header anyway.");
				qop = "auth";
			}
			
				
			String auth = "auth";
//			String entityBody = "xml\n";
//			if (body != null) {
				// auth-int has been removed for now.
//				auth = "auth-int";
//				byte [] HEntity = digester.digest(entityBody.getBytes());
//				System.out.println("H(entityBody) - " + StunConstants.asHex(HEntity));
//				s2 += ":" + StunConstants.asHex(HEntity);

//			}

			byte [] a1 = digester.digest(s1.getBytes());
			byte [] a2 = digester.digest(s2.getBytes());

			// Empty auth is not allowed
//			String mid = "";
//			if (auth == null)
//				mid = ":" + nonce + ":";
//			else
				String mid = ":" + nonce + ":" + nonceCount + ":" + cnonce + ":" + auth + ":";

			String nd = Conversion.asHex(a1) + mid + Conversion.asHex(a2);
			byte [] digest = digester.digest(nd.getBytes());

			// Use asHex method of Conversion because we don't want any
			// 0x or spacing in the values.
			logger.info(PC2LogCategory.SIP, subCat, "\n\na1 - " + s1);
			logger.info(PC2LogCategory.SIP, subCat, "a2 - " + s2);
			logger.info(PC2LogCategory.SIP, subCat, "mid - " + mid);
			logger.info(PC2LogCategory.SIP, subCat, "\n\nHA1 - " + Conversion.asHex(a1));
			logger.info(PC2LogCategory.SIP, subCat, "cNonce = " + cnonce);
			logger.info(PC2LogCategory.SIP, subCat, "HA2 - " + Conversion.asHex(a2));
			logger.info(PC2LogCategory.SIP, subCat, "KD - " + Conversion.asHex(digest) + "\n\n");
			
			String rspAuth = Conversion.asHex(digest).toString();
			return rspAuth;
		}

		return null;
		
	}
	
	protected String calculateRspAuth(ProxyAuthorizationHeader pah, Properties p) {
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
		if (pah != null && p != null) {
			// Based upon 24.229 REQ 8716, the username field must be the private user identifier
			String uri = pah.getURI().toString();
			String user = p.getProperty(SettingConstants.PRUI);
			// For realm we use the value in the domain setting
			String realm = p.getProperty(SettingConstants.DOMAIN);
			String passwd = p.getProperty(SettingConstants.PASSWORD);
			String nonce = pah.getNonce();
			String nonceCount = pah.getNonceCount(); 
			String cnonce = pah.getCNonce();
			if (cnonce == null)
				cnonce ="";
			String s1 = user + ":" + realm + ":" + passwd;
			String s2 = ":" + uri.toString();
			String qop = pah.getQop();
			if (qop == null) {
				logger.warn(PC2LogCategory.SIP, subCat, 
					"The Proxy-Authenticate header received in the resposne message doesn't contain a "
						+ "qop parameter.\nUsing auth in Authentication-Info header anyway.");
				qop = "auth";
			}
			
				
			String auth = "auth";
//			String entityBody = "xml\n";
//			if (body != null) {
				// auth-int has been removed for now.
//				auth = "auth-int";
//				byte [] HEntity = digester.digest(entityBody.getBytes());
//				System.out.println("H(entityBody) - " + StunConstants.asHex(HEntity));
//				s2 += ":" + StunConstants.asHex(HEntity);

//			}

			byte [] a1 = digester.digest(s1.getBytes());
			byte [] a2 = digester.digest(s2.getBytes());

			// Empty auth is not allowed
//			String mid = "";
//			if (auth == null)
//				mid = ":" + nonce + ":";
//			else
			String	mid = ":" + nonce + ":" + nonceCount + ":" + cnonce + ":" + auth + ":";

			String nd = Conversion.asHex(a1) + mid + Conversion.asHex(a2);
			byte [] digest = digester.digest(nd.getBytes());

			// Use asHex method of Conversion because we don't want any
			// 0x or spacing in the values.
			logger.info(PC2LogCategory.SIP, subCat, "\n\na1 - " + s1);
			logger.info(PC2LogCategory.SIP, subCat, "a2 - " + s2);
			logger.info(PC2LogCategory.SIP, subCat, "mid - " + mid);
			logger.info(PC2LogCategory.SIP, subCat, "\n\nHA1 - " + Conversion.asHex(a1));
			logger.info(PC2LogCategory.SIP, subCat, "cNonce = " + cnonce);
			logger.info(PC2LogCategory.SIP, subCat, "HA2 - " + Conversion.asHex(a2));
			logger.info(PC2LogCategory.SIP, subCat, "KD - " + Conversion.asHex(digest) + "\n\n");
			
			String rspAuth = Conversion.asHex(digest).toString();
			return rspAuth;
		}

		return null;
		
	}


	protected AuthenticationInfoHeader createAuthenticationInfoHeader(String response) throws ParseException {
//		if (response != null) {
		AuthenticationInfoHeader header = headerFactory.createAuthenticationInfoHeader(response);
		return header;
//		}
//		return null;
	}

	protected AuthorizationHeader createAuthorizationHeader(String method, Properties src, int cSeqNo,
			Object body, WWWAuthenticateHeader wwwAH) throws ParseException {

		AuthorizationHeader header = headerFactory.createAuthorizationHeader("Digest");
		if (header != null && src != null) {
			// Based upon 24.229 REQ 8716, the username field must be the private user identifier
			String user = src.getProperty(SettingConstants.PRUI);
			header.setUsername(user);
			// For realm we use the value in the domain setting
			String realm = src.getProperty(SettingConstants.DOMAIN);
			String passwd = src.getProperty(SettingConstants.PASSWORD);
			header.setRealm(realm);
			String nonceVal = null;
			if (wwwAH != null) {
				nonceVal = wwwAH.getNonce();
				header.setNonce(wwwAH.getNonce());

			}
			else {
				nonceVal = "";
				header.setNonce(nonceVal);
				header.setResponse("");
			}
			header.setAlgorithm("MD5");
			
//			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
//			String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
//			String sipAddr = null;
//			if (addrType == null || 
//					addrType.equals(SettingConstants.IP) ||
//					addrType.equals(SettingConstants.LAB_FQDN)) {
//				sipAddr = src.getProperty(SettingConstants.IP); // maybe + ":" + ((Integer)port).toString();
//				if (Conversion.isIPv6Address(sipAddr)) {
//					String zone = src.getProperty(SettingConstants.IPv6_ZONE);
//					sipAddr = Conversion.addZone(sipAddr, zone); 
//				}
//			}
//			else if (addrType.equals(SettingConstants.FQDN)) {
//				sipAddr = src.getProperty(SettingConstants.FQDN);
//			}
//			SipURI uri = 
//				addressFactory.createSipURI(user, sipAddr);
			SipURI uri = createSipURI(src, user, -1);
			header.setURI(uri);

			String s1 = user + ":" + realm + ":" + passwd;
			String s2 = method + ":" + uri.toString();
			String wwwQOP = null;
			if (wwwAH != null) {
				wwwQOP = wwwAH.getQop();
				if (wwwQOP == null) {
					logger.warn(PC2LogCategory.SIP, subCat, 
						"The WWW-Authenticate header received in the resposne message doesn't contain a qop parameter.");
				}
			}
			else
				wwwQOP = "auth";
				
			String auth = null;
			if (wwwQOP != null && wwwQOP.contains("auth")) 
				auth = "auth";
//			String entityBody = "xml\n";
			if (body != null) {
				// auth-int has been removed for now.
//				auth = "auth-int";
//				byte [] HEntity = digester.digest(entityBody.getBytes());
//				System.out.println("H(entityBody) - " + StunConstants.asHex(HEntity));
//				s2 += ":" + StunConstants.asHex(HEntity);

			}
			byte [] a1 = digester.digest(s1.getBytes());
			byte [] a2 = digester.digest(s2.getBytes());

			String cNonce = "7681910e9b43129ab5c82584b910315e";
			String nc = Integer.toString(cSeqNo+10);
			String mid = "";
			if (auth == null)
				mid = ":" + nonceVal + ":";
			else
				mid = ":" + nonceVal + ":" + nc + ":" + cNonce + ":" + auth + ":";
//			digester.update(a1);
//			digester.update(mid.getBytes());
//			digester.update(a2);
			String nd = Conversion.asHex(a1) + mid + Conversion.asHex(a2);
			byte [] digest = digester.digest(nd.getBytes());

			// Use asHex method of Conversion because we don't want any
			// 0x or spacing in the values.
			logger.info(PC2LogCategory.SIP, subCat, "\n\na1 - " + s1);
			logger.info(PC2LogCategory.SIP, subCat, "a2 - " + s2);
			logger.info(PC2LogCategory.SIP, subCat, "mid - " + mid);
			logger.info(PC2LogCategory.SIP, subCat, "\n\nHA1 - " + Conversion.asHex(a1));
			logger.info(PC2LogCategory.SIP, subCat, "cNonce = " + cNonce);
			logger.info(PC2LogCategory.SIP, subCat, "HA2 - " + Conversion.asHex(a2));
			logger.info(PC2LogCategory.SIP, subCat, "KD - " + Conversion.asHex(digest) + "\n\n");
			if (wwwAH != null) {
				String opaque = wwwAH.getOpaque();
				if (opaque != null)
					header.setOpaque(opaque);
				if (auth != null) {
					header.setCNonce(cNonce);
					header.setNonceCount(nc);
					header.setQop("auth");
				}
				String resp = Conversion.asHex(digest).toString();
				header.setResponse(resp);
			}
			
			return header;
		}

		return null;
	}

	/**
	 * Creates the ContentType Header for the network element described by the
	 * properties parameter
	 * @param p - the properties of the network element 
	 * @return - a ContentType Header for the network element
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected AcceptHeader createAcceptHeader() throws ParseException, InvalidArgumentException {
		return createAcceptHeader("application", "sdp");
		
	}

	/**
	 * Creates the ContentType Header for the network element described by the
	 * properties parameter
	 * @param p - the properties of the network element 
	 * @return - a ContentType Header for the network element
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected AcceptHeader createAcceptHeader(String type, String subType ) throws ParseException, InvalidArgumentException {
		// Create ContentTypeHeader
		AcceptHeader header =
			headerFactory.createAcceptHeader(type, subType);
		return header;
	}
	
	/**
	 * Creates a CallId Header for the network element described by the 
	 * properties parameter
	 * @param p - the properties of the network element
	 * @param provider - the provider that is sending the message.
	 * @return - a CallId Header
	 */
	protected CallIdHeader createCallIdHeader(Properties p, SipProvider provider) {
		// Create a new CallId header

		CallIdHeader header = provider.getNewCallId();
		return header;
	}
	/**
	 * Creates a Contact Header for the network element described by the 
	 * properties parameter.
	 * 
	 * @param reqURI - the properties information for the network element.
	 * @param dest - the properties of the destination of the message.
	 * 
	 * @return - a Contact Header for the network element.
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected ContactHeader createContactHeader(URI reqURI, Properties dest) throws ParseException, InvalidArgumentException {
		if (reqURI != null)	   { 	
			String gruuFormat = null;
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			Address contactAddress = null;
			RegistrarData rd = null;
			if (ss.getGRUU() == Extension.REQUIRED) {
				gruuFormat = platform.getProperty(SettingConstants.GRUU_FORMAT);
				String name = dest.getProperty(SettingConstants.USER_NAME);
				String fromSipAddress = null;
//				GLH ADDR
				String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT); // "IP";	
				if (addrType == null || 
						addrType.equals(SettingConstants.IP) ||
						addrType.equals(SettingConstants.LAB_FQDN)) {
					fromSipAddress = dest.getProperty(SettingConstants.IP);
					if (Conversion.isIPv6Address(fromSipAddress)) {
						String zone = dest.getProperty(SettingConstants.IPv6_ZONE);
						fromSipAddress = Conversion.addZone(fromSipAddress, zone); 
					}
				}
				else if (addrType.equals(SettingConstants.FQDN)) {
					fromSipAddress = dest.getProperty(SettingConstants.FQDN);
				}
				if (gruuFormat != null &&
						gruuFormat.equals(SettingConstants.TEMPORARY_GRUU)) {
					rd = manufacturer.aorGruuIndex.get(fromSipAddress);
					if (rd != null) {
						name = rd.getTempGruuParameter();
						fromSipAddress = rd.getTempGruuDomain();
					}
					SipURI contactURI = addressFactory.createSipURI(name, fromSipAddress);
					contactAddress = addressFactory.createAddress(contactURI);
				}
			}
			else 
				contactAddress = addressFactory.createAddress(reqURI);
			
			if (contactAddress instanceof SipURI) {
				SipURI su = (SipURI)contactAddress.getURI();
				su.setPort(-1);
			}
						
			ContactHeader header =
				headerFactory.createContactHeader(contactAddress);
			
			// Now see if we need to add the gr parameter
			if (rd != null && gruuFormat != null) {
				if (gruuFormat.equals(SettingConstants.PUBLIC_GRUU)) {
					header.setParameter(SettingConstants.GR, rd.getGrParameter());
				}
				else if (gruuFormat.equals(SettingConstants.TEMPORARY_GRUU)) {
					header.setParameter(SettingConstants.GR, "");
				}
			}
			
			return header;
		}
		return null;
	}


	/**
	 * Creates a Contact Header for the network element described by the 
	 * properties parameter.
	 * 
	 * @param p - the properties information for the network element.
	 * @param host -
	 * @param port - the transport port number being used for the message.
	 * @param applyGRUU - specifies whether GRUU addressing can be used
	 * 		when constructing the header. This is false if the method
	 * 		doesn't use GRUU in the contact e.g. SUBSCRIBE and REGISTER.
	 * 
	 * @return - a Contact Header for the network element.
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected ContactHeader createContactHeader(Properties p, 
			String host, int port, boolean applyGRUU) throws ParseException, InvalidArgumentException {
		String fromName = null; 
		SipURI contactURI = null;
		Address contactAddress = null;
		boolean useGRUU = false;
		ContactHeader header = null;
		RegistrarData rd = null;
		boolean addPort = false;
		
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
		String gruuFormat = null;
		if (applyGRUU && ss.getGRUU() == Extension.REQUIRED) {
			useGRUU = true;
			gruuFormat = platform.getProperty(SettingConstants.GRUU_FORMAT);
		}

		String name = p.getProperty(SettingConstants.USER_NAME);
		String fromSipAddress = null;
//		GLH ADDR
		addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);	
		if (addrType == null || 
				addrType.equals(SettingConstants.IP) ||
				addrType.equals(SettingConstants.LAB_FQDN)) {
			fromSipAddress = p.getProperty(SettingConstants.IP);
			if (Conversion.isIPv6Address(fromSipAddress)) {
				String zone = p.getProperty(SettingConstants.IPv6_ZONE);
				fromSipAddress = Conversion.addZone(fromSipAddress, zone); 
			}
			addPort = true;
		}
		else if (addrType.equals(SettingConstants.FQDN)) {
			fromSipAddress = p.getProperty(SettingConstants.FQDN) 
				+ ":" + ((Integer)port).toString();
			
		}
		if (gruuFormat != null &&
				gruuFormat.equals(SettingConstants.TEMPORARY_GRUU)) {
			rd = manufacturer.aorGruuIndex.get(fromSipAddress);
			if (rd != null) {
				name = rd.getTempGruuParameter();
				fromSipAddress = rd.getTempGruuDomain();
			}
		}
		else
			contactURI = addressFactory.createSipURI(name, fromSipAddress);
		contactAddress = addressFactory.createAddress(contactURI);

		// Add the contact address.
		if (!useGRUU)
			contactAddress.setDisplayName(fromName);
		if (addPort)
			contactURI.setPort(port);
		header =
			headerFactory.createContactHeader(contactAddress);

		// Now see if we need to add the gr parameter
		if (useGRUU && rd != null && gruuFormat != null) {
			if (gruuFormat.equals(SettingConstants.PUBLIC_GRUU)) {
				header.setParameter(SettingConstants.GR, rd.getGrParameter());
			}
			else if (gruuFormat.equals(SettingConstants.TEMPORARY_GRUU)) {
				header.setParameter(SettingConstants.GR, "");
			}
//			header.setParameter("opaque", "\"urn:uuid:" + 
//			p.getProperty(SettingConstants.OPAQUE_UUID) + "\"");
//			header.setParameter("grid", "12345");
//			header.setParameter("+sip.instance", "\"<urn:uuid:" + 
//			p.getProperty(SettingConstants.SIP_INSTANCE_UUID) + ">\"");
//			header.setExpires(3600);

		}
		return header;
	}

	/**
	 * Creates the ContentType Header for the network element described by the
	 * properties parameter
	 * @param p - the properties of the network element 
	 * @return - a ContentType Header for the network element
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected ContentTypeHeader createContentTypeHeader(String content, String subContent) throws ParseException, InvalidArgumentException {
		// Create ContentTypeHeader
		ContentTypeHeader header = null;
		if (content == null || subContent == null)
			header = headerFactory.createContentTypeHeader("application", "sdp");
		else
			header = headerFactory.createContentTypeHeader(content, subContent);
		return header;
	}

	/**
	 * Creates a CSeq Header with a new number for a SIP request message. 
	 * 
	 * @param method - the method of the message.
	 * @return - A CSeq Header
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected CSeqHeader createCSeqHeader(String method) throws ParseException, InvalidArgumentException {
		// Create a new Cseq header
		CSeqHeader header =
			headerFactory.createCSeqHeader((++requestSeqNum), method);
		return header;
	}

	/**
	 * Creates a CSeq Header with a given number for a SIP request message. 
	 * 
	 * @param number - the number value for the header
	 * @param method - the method of the message.
	 * @return - A CSeq Header
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected CSeqHeader createCSeqHeader(int number, String method) throws ParseException, InvalidArgumentException {
		// Create a new Cseq header
		CSeqHeader header =
			headerFactory.createCSeqHeader(number, method);
		return header;
	}
	
	protected String createDialogEventProfile(Properties src, Properties dest) {
		String username = dest.getProperty(SettingConstants.USER_NAME);
		String domain = dest.getProperty(SettingConstants.DOMAIN);
		String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" 
	    + "<dialog-info xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n"
	    + " xsi:schemaLocation=\"urn:ietf:params:xml:ns:dialog-info\"\r\n"
	    + "	version=\"0\" state=\"full\" entity=\"sip:" 
		+  username + "@" + domain 	+ "\">\r\n" 
		+ "   <dialog id=\"as7d900as8\">\r\n"
		+ "      <state>confirmed</state>\r\n"
		+ "      <duration>274</duration>\r\n"
		+ "      <local>\r\n"
        + "         <identity display=\"Anonymous\">sip:anonymous@anonymous.invalid</identity>\r\n"
        + "         <target uri=\"sip:10.32.0.127\">\r\n"
        + "            <param pname=\"isfocus\" pval=\"true\"/>\r\n"
        + "            <param pname=\"class\" pval=\"personal\"/>\r\n"
        + "         </target>\r\n"
        + "      </local>\r\n"
        + "      <remote>\r\n"
        + "         <identity display=\"Anonymous\">sip:anonymous@anonymous.invalid</identity>\r\n"
        + "         <target uri=\"sip:10.32.0.127\">\r\n"
        + "            <param pname=\"isfocus\" pval=\"true\"/>\r\n"
        + "            <param pname=\"class\" pval=\"personal\"/>\r\n"
        + "         </target>\r\n"
        + "      </remote>\r\n"
        + "   </dialog>\r\n"
        + "</dialog-info>\r\n";
		
		return body;
	}
	/**
	 * Creates a From Header for the message based upon the properties file
	 * given as a parameter.
	 * 
	 * @param p - the properties information for the network element that
	 * 			the From header is being create for.
	 * @param port - the transport port number being used for the message.
	 * @param tag - the tag to use in the header
	 * 
	 * @return - the From header for the network element.
	 * @throws ParseException
	 */
	protected FromHeader createDomainFromHeader(Properties p, int port, String tag) throws ParseException {
		String fromName = p.getProperty(SettingConstants.USER_NAME);
		String fromDisplayName = p.getProperty(SettingConstants.DISPLAY_NAME);
		String fromDomain = p.getProperty(SettingConstants.DOMAIN);

		SipURI fromAddress =
			addressFactory.createSipURI(fromName, fromDomain);

		Address fromNameAddress = addressFactory.createAddress(fromAddress);
		if (SystemSettings.getBooleanSetting("From Include Display Name"))
			fromNameAddress.setDisplayName(fromDisplayName);
		FromHeader fromHeader =
			headerFactory.createFromHeader(fromNameAddress, tag);
		return fromHeader;
	}

	/**
	 * Creates a To Header for the message based upon the properties file
	 * and to tag given as parameters.
	 * @param p - the properties information for the network element that
	 * 			the To header is being create for.
	 * @param tag - the tag to use in the header
	 * @param includeDisplayName - a flag stating whether to include the
	 * 		display-name property for the network element.
	 * @param port - the transport port number being used for the message.
	 * 
	 * @return - the To header for the network element.
	 * @throws ParseException
	 */
	protected ToHeader createDomainToHeader(Properties p, String tag, 
			boolean includeDisplayName, int port) throws ParseException {

		String toUser = p.getProperty(SettingConstants.USER_NAME);
		String toDisplayName = p.getProperty(SettingConstants.DISPLAY_NAME);
		String toDomain = p.getProperty(SettingConstants.DOMAIN);

		SipURI toAddress =
			addressFactory.createSipURI(toUser, toDomain);
		Address toNameAddress = addressFactory.createAddress(toAddress);
		if (includeDisplayName)
			toNameAddress.setDisplayName(toDisplayName);
		ToHeader toHeader =
			headerFactory.createToHeader(toNameAddress, tag);
		return toHeader;
	}

	/**
	 * Creates a Event Header with the specified event type
	 * 
	 * @param type - the type of event in the header
	 * @return - an Event Header
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected EventHeader createEventHeader(String type) throws ParseException, InvalidArgumentException {
		// Create a new MaxForwardsHeader
		EventHeader header =
			headerFactory.createEventHeader(type);
		return header;
	}

	/**
	 * Creates a Expires Header 
	 * 
	 *  @return - an Expires Header
	 *  @throws ParseException
	 */
	protected ExpiresHeader createExpiresHeader(Integer value) throws ParseException {
		ExpiresHeader header = null;
		int expires = 600000;
		if (value != null)
			expires = value;

		try {
			header= headerFactory.createExpiresHeader(expires);
		}
		catch (InvalidArgumentException iae) {
			throw new ParseException(iae.getMessage(), 0);
		}
		return header;
	}



	/**
	 * Creates a From Header for the message based upon the properties file
	 * given as a parameter.
	 * 
	 * @param p - the properties information for the network element that
	 * 			the From header is being create for.
	 * @param port - the transport port number being used for the message.
	 * @param tag - the tag to use in the header
	 * 
	 * @return - the From header for the network element.
	 * @throws ParseException
	 */
	protected FromHeader createFromHeader(Properties p, int port, String tag) throws ParseException {
		String name = p.getProperty(SettingConstants.USER_NAME);
//		String fromSipAddress = null;
		String fromDisplayName = p.getProperty(SettingConstants.DISPLAY_NAME);
//		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
//		String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
//		boolean addPort = false;
		
//		if (addrType == null || 
//				addrType.equals(SettingConstants.IP) ||
//				addrType.equals(SettingConstants.LAB_FQDN)) {
//			fromSipAddress = p.getProperty(SettingConstants.IP); 
//			if (Conversion.isIPv6Address(fromSipAddress)) {
//				String zone = p.getProperty(SettingConstants.IPv6_ZONE);
//				fromSipAddress = Conversion.addZone(fromSipAddress, zone); 
//			}
//			addPort = true;
//		}
//		else if (addrType.equals(SettingConstants.FQDN)) {
//			fromSipAddress = p.getProperty(SettingConstants.DOMAIN);
//		}
//
//		SipURI fromAddress =
//			addressFactory.createSipURI(fromName, fromSipAddress);
		SipURI fromAddress = createSipURI(p, name, port);

		Address fromNameAddress = addressFactory.createAddress(fromAddress);
		if (SystemSettings.getBooleanSetting(SettingConstants.FROM_DISPLAY_NAME))
			fromNameAddress.setDisplayName(fromDisplayName);
//		if (addPort)
//			fromAddress.setPort(port);
		FromHeader fromHeader =
			headerFactory.createFromHeader(fromNameAddress, tag);
		return fromHeader;
	}

	/**
	 * Converts a From Header to a From Header.
	 * 
	 * @param to - the To Header to convert
	 * @return - the From header
	 * @throws ParseException
	 */
	protected FromHeader createFromHeader(ToHeader to) throws ParseException {
		FromHeader fromHeader = headerFactory.createFromHeader(to.getAddress(), to.getTag());
		return fromHeader;
	}

	/**
	 * Creates a Message Summary Body to be sent in a NOTIFY message indicating
	 * that there is one voice mail message waiting.
	 */
	protected String createMessageSummary(Properties p) throws ParseException {
		// LinkedList<String> elements = PresenceServer.get;
		String user = p.getProperty(SettingConstants.USER_NAME);
		String domain = p.getProperty(SettingConstants.DOMAIN);
		String body = "Messages-Waiting: yes\r\n" 
			+ "Message-Account: sip:" + user + "@" + domain + "\r\n"
			+ "Voice-Message: 1/1 (0/0)\r\n";

		return body;
	} 
	
	/**
	 * Creates a MaxForwards Header with the specified number of hops
	 * 
	 * @param hops - the number of hops to include in the header
	 * @return - a MaxForwards Header
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected MaxForwardsHeader createMaxForwardsHeader(int hops) throws ParseException, InvalidArgumentException {
		// Create a new MaxForwardsHeader
		MaxForwardsHeader header =
			headerFactory.createMaxForwardsHeader(hops);
		return header;
	}

	/**
	 * Creates a Mime Version Header with the specified number of hops
	 * 
	 * @param hops - the number of hops to include in the header
	 * @return - a MaxForwards Header
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected MimeVersionHeader createMimeVersionHeader() throws ParseException, InvalidArgumentException {
		// Create a new MaxForwardsHeader
		MimeVersionHeader header =
			headerFactory.createMimeVersionHeader(1,0);
		return header;
	}


	/**
	 * Creates a P-Access-Network-Info Header using the CSeq Header
	 * 
	 *  @param p - the properties of the network element to make the header on behalf
	 *  	
	 *  @return - a generic Extension Header
	 *  @throws ParseException
	 */
	protected Header createPAccessNetworkInfoHeader(Properties p) throws ParseException {

		Header header = headerFactory.createHeader("P-Access-Network-Info", "DOCSIS");
		return header;
	}
	
	/**
	 * Creates a Reg-Info Body to be sent in a NOTIFY message.
	 */
	protected String createMWIBody(Properties p, String contactURI) throws ParseException {
		
	
		String pui = p.getProperty(SettingConstants.PUI);
		String body = "Messages-Waiting: yes\r\nMessage-Account: sip:" + pui + "\r\n"; 
			
		return body;
	}
	
	protected String createMultipartBody(LinkedList<String> bodies, Properties p, String neLabel,
			boolean offer, String remoteDirectionTag,	boolean reinvite,
			long sessionId, long sessionVersion) {
		ListIterator<String> iter = bodies.listIterator();
		String body = null;
		while (iter.hasNext()) {
			String bodyType = iter.next();
			String temp = null;
			if (bodyType.equalsIgnoreCase("multipart")) {
				// do nothing special for this value
			}
			else if (bodyType.equalsIgnoreCase("SDPOffer")) {
				 temp = "--boundary1\r\nContent-Type: application/sdp\r\n"
					+ "Content-Disposition: early-session\r\n\r\n" 
				 + createSDPData(p, neLabel, false, 
							remoteDirectionTag, false, false, sessionId, sessionVersion);	
			}
			else if (bodyType.equalsIgnoreCase("SDPAnswer")) {
				 temp = "--boundary1\r\nContent-Type: application/sdp\r\n"
						+ "Content-Disposition: session\r\n\r\n" 
					 + createSDPData(p, neLabel, false, 
								remoteDirectionTag, false, true, sessionId, sessionVersion);	
			}
			if (body == null)
				body = temp;
			else
				body += "\r\n" + temp;
		}
		if (body != null) {
			body += "\r\n--boundary1--\r\n";
		}
		return body;
	}
	
//	  P-Asserted-Identity 
	protected Header createPAssertedIdentityHeader(Properties src) throws ParseException {
		String value = src.getProperty(SettingConstants.PUI);
		Header header = null;
		if (value != null) {
			String uri = "sip:" + value;
			header = headerFactory.createHeader("P-Asserted-Identity", uri);
		}
		return header;
	}
	
//	  P-Associated-URI
	protected Header createPAssociatedURI(Address from) throws ParseException {
		Header header = null;
		header = headerFactory.createHeader("P-Associated-URI", from.toString());
		return header;
	}
	
//	  P-Called-Party-ID
	protected Header createPCalledPartyID(Properties p) throws ParseException {
		Header header = null;
		String pui = p.getProperty(SettingConstants.PUI);
		if (pui != null) {
			String uri = "<" + pui + ">";
			header = headerFactory.createHeader("P-Called-Party-ID", uri);
		}
		return header;
	}
	
//	  P-Charging-Vector 
	protected Header createPChargingVectorHeader(Properties src, Properties dest) throws ParseException {
		Header header = null;
		String srcPCSCF = src.getProperty(SettingConstants.PCSCF);
		if (srcPCSCF != null) {
			Properties myP = SystemSettings.getSettings(srcPCSCF);
			String srcSCSCF = myP.getProperty(SettingConstants.SCSCF);
			if (srcSCSCF != null) {
				Properties myS = SystemSettings.getSettings(srcSCSCF);
				String value = "icid-value=\"PCSF:" + myS.getProperty(SettingConstants.IP)
				+ "-1234567890-1234567890\";orig-ioi=\"Type 3 " 
				+ myS.getProperty(SettingConstants.DOMAIN) + "\"";
				if (value != null) {
					header = headerFactory.createHeader("P-Charging-Vector", value);
				}
			}
		}
		
		return header;
	}
	
//	  P-Charging-Function-Address 
	protected Header createPChargingFunctionAddressHeader(Properties src, Properties dest) throws ParseException {
		Header header = headerFactory.createHeader("P-Charging-Function-Address", "ccf=\"aaa://hss.com\"");
		return header;
	}
	
	protected Header createPathHeader(Properties scscf, Properties pcscf) throws ParseException {
		// Based upon version D03 of SIP-UE only the P-CSCF is in the Path header
		String value;
		
		String fqdn = pcscf.getProperty(SettingConstants.FQDN);
		// if the fqdn is really an ipv6 address put it in []
		String[] parts = fqdn.split(":");
		if (parts.length == 1)
			value = "<sip:" + fqdn + ";lr>";
		else
			value = "<sip:[" + fqdn + "];lr>";

		Header header = headerFactory.createHeader("Path", value);
		return header;
	}
	
	protected ProxyAuthenticateHeader createProxyAuthenticateHeader(Properties src, Properties target) throws ParseException {

		ProxyAuthenticateHeader header = headerFactory.createProxyAuthenticateHeader(ParameterNames.DIGEST);

		// For realm we use the domain property
		header.setRealm(target.getProperty(SettingConstants.DOMAIN));
		// auth-int was removed from the possibilities for the 
		// time being
		// header.setQop("auth,auth-int");
		header.setQop("auth");
		String nonce = ((Long)System.currentTimeMillis()).toString();
		
		byte [] nvDigest = digester.digest(nonce.getBytes());
		StringBuffer hexNV = asHexString(nvDigest);

		header.setNonce(hexNV.toString());
		// OPAQUE parameter is no longer necessary according to Stuart Hoggan
//		long opaqueNum = rand.nextLong();
//		String ov = ((Long)opaqueNum).toString();
//		byte [] oDigest = digester.digest(ov.getBytes());
//		StringBuffer hexO = asHexString(oDigest);
//		header.setOpaque(hexO.toString());
		
		header.setAlgorithm("MD5");
		return header;
	}
	
	protected ProxyAuthorizationHeader createProxyAuthorizationHeader(String method, 
			Properties src, int cSeqNo,	Object body, 
			ProxyAuthenticateHeader pah) throws ParseException {

		ProxyAuthorizationHeader header = headerFactory.createProxyAuthorizationHeader("Digest");
		if (header != null && src != null) {
			// Based upon 24.229 REQ 8716, the username field must be the private user identifier
			String user = src.getProperty(SettingConstants.PRUI);
			header.setUsername(user);
			// For realm we use the value in the domain setting
			String realm = src.getProperty(SettingConstants.DOMAIN);
			String passwd = src.getProperty(SettingConstants.PASSWORD);
			header.setRealm(realm);
			String nonceVal = null;
			if (pah != null) {
				nonceVal = pah.getNonce();
				header.setNonce(pah.getNonce());

			}
			else {
				nonceVal = "";
				header.setNonce(nonceVal);
				header.setResponse("");
			}
			header.setAlgorithm("MD5");
			header.setQop("auth");
//			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
//			String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
//			String sipAddr = null;
//			if (addrType == null || 
//					addrType.equals(SettingConstants.IP) ||
//					addrType.equals(SettingConstants.LAB_FQDN)) {
//				sipAddr = src.getProperty(SettingConstants.IP); 
//				if (Conversion.isIPv6Address(sipAddr)) {
//					String zone = src.getProperty(SettingConstants.IPv6_ZONE);
//					sipAddr = Conversion.addZone(sipAddr, zone); 
//				}
//				// maybe + ":" + ((Integer)port).toString();
//			}
//			else if (addrType.equals(SettingConstants.FQDN)) {
//				sipAddr = src.getProperty(SettingConstants.FQDN);
//			}
//			SipURI uri = 
//				addressFactory.createSipURI(user, sipAddr);
			SipURI uri = createSipURI(src, user, -1);
			header.setURI(uri);

			String s1 = user + ":" + realm + ":" + passwd;
			String s2 = method + ":" + uri.toString();
			String auth = "auth";
//			String entityBody = "xml\n";
			if (body != null) {
				// auth-int has been removed for now.
//				auth = "auth-int";
//				byte [] HEntity = digester.digest(entityBody.getBytes());
//				System.out.println("H(entityBody) - " + StunConstants.asHex(HEntity));
//				s2 += ":" + StunConstants.asHex(HEntity);

			}
			byte [] a1 = digester.digest(s1.getBytes());
			byte [] a2 = digester.digest(s2.getBytes());

			String cNonce = "7681910e9b43129ab5c82584b910315e";
			String nc = "00000001"; // Integer.toString(cSeqNo+10);
			String mid = ":" + nonceVal + ":" + nc + ":" + cNonce + ":" + auth + ":";

			String nd = Conversion.asHex(a1) + mid + Conversion.asHex(a2);
			byte [] digest = digester.digest(nd.getBytes());

			// Use asHex method of Conversion because we don't want any
			// 0x or spacing in the values.
			logger.info(PC2LogCategory.SIP, subCat, "\n\na1 - " + s1);
			logger.info(PC2LogCategory.SIP, subCat, "a2 - " + s2);
			logger.info(PC2LogCategory.SIP, subCat, "mid - " + mid);
			logger.info(PC2LogCategory.SIP, subCat, "\n\nHA1 - " + Conversion.asHex(a1));
			logger.info(PC2LogCategory.SIP, subCat, "cNonce = " + cNonce);
			logger.info(PC2LogCategory.SIP, subCat, "HA2 - " + Conversion.asHex(a2));
			logger.info(PC2LogCategory.SIP, subCat, "KD - " + Conversion.asHex(digest) + "\n\n");
			if (pah != null) {
				String opaque = pah.getOpaque();
				if (opaque != null)
					header.setOpaque(pah.getOpaque());
				header.setCNonce(cNonce);
				header.setNonceCount(nc);
			}
			String resp = Conversion.asHex(digest).toString();
			header.setResponse(resp);
			return header;
		}

		return null;
	}
	
	protected Header createProxyAuthenticationInfoHeader(String hdr) throws ParseException {
		Header header = null;
		header = headerFactory.createHeader("Proxy-Authentication-Info", hdr.toString());
		return header;

	}
	
	/**
	 * Creates a RAck Header using the CSeq Header
	 * 
	 *  @param - a CSeq Header to construct the RAck header with
	 *  @return - a generic Extension Header
	 *  @throws ParseException
	 */
	protected Header createRAckHeader(RSeqHeader rseq, CSeqHeader cseq) throws ParseException {
		String extValue = rseq.getSequenceNumber() + " " + 
		cseq.getSequenceNumber() + " " + cseq.getMethod();
		Header header = headerFactory.createHeader("RAck", extValue);
		return header;
	}
	
	/**
	 * Creates RecordRoute Header for the network element described by the 
	 * properties parameter.
	 * 
	 * @param p - the properties of the network element.
	 * @param port - the transport port number being used for the message.
	 * 
	 * @return - the RecordRoute Header
	 * @throws ParseException
	 */
	protected RecordRouteHeader createRecordRouteHeader(Properties p, String transport) throws ParseException {
		
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
//		 GLH ADDR
		String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT); // "IP";
		String host = getHost(p, addrType);

		SipURI address =
			addressFactory.createSipURI(null, host);
		Address routeAddress = addressFactory.createAddress(address);
		String ne = p.getProperty(SettingConstants.NE);
		address.setParameter("lr", null);
		if (ne.startsWith("PCSCF"))
			address.setParameter("keep-stun", null);
		
		if (transport != null)
			address.setTransportParam(transport.toLowerCase());
		
		RecordRouteHeader header =
			headerFactory.createRecordRouteHeader(routeAddress);

		
			
		return header;
	}

	protected ReferToHeader createReferToHeader() throws ParseException {
//		String peerHost = "";
		Properties p = SystemSettings.getSettings("UE2");
		if (p != null) {
			String name = p.getProperty(SettingConstants.USER_NAME);
//
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
//			// GLH ADDR
//			// addrType = "IP";
//			if (addrType == null || 
//					addrType.equals(SettingConstants.IP) ||
//					addrType.equals(SettingConstants.LAB_FQDN)) {
//				peerHost = p.getProperty(SettingConstants.IP); 
//			}
//			else if (addrType.equals(SettingConstants.FQDN)) {
//				toUser = p.getProperty(SettingConstants.USER_NAME);
//				peerHost = p.getProperty(SettingConstants.DOMAIN);
//			}
//
//			SipURI uri =
//				addressFactory.createSipURI(toUser, peerHost);
			SipURI uri = createSipURI(p, name, -1);
			
			Address addr = addressFactory.createAddress(uri);
			ReferToHeader header = headerFactory.createReferToHeader(addr);
			if (header != null) {
				Properties ue0 = SystemSettings.getSettings("UE0");
				String user = ue0.getProperty(SettingConstants.USER_NAME);
				String ue0Host = null;
				if (addrType == null || 
						addrType.equals(SettingConstants.IP) ||
						addrType.equals(SettingConstants.LAB_FQDN)) {
					ue0Host = p.getProperty(SettingConstants.IP); // + ":" + ((Integer)port).toString();
				}
				else if (addrType.equals(SettingConstants.FQDN)) {
					ue0Host = p.getProperty(SettingConstants.DOMAIN);
				}
				String replace = "0001_" + user + "%40" + ue0Host + "%3Bto-tag%3D12345%3Bfrom-tag%3D6789";
				//header.setParameter("Replaces", replace);
				// The Replaces, to-tag, and from-tag are qualify headers of the URI
				// not the header.
				// uri.setParameter("Replaces", replace);
				SipUri su = (SipUri)uri;
				NameValue nv = new NameValue("Replaces", replace);
				su.setQHeader(nv);
			}
			return header;
		}
		return null;
	}

	/**
	 * Creates a Reg-Info Body to be sent in a NOTIFY message.
	 */
	protected String createRegInfoBody(Properties p, Byte regId, int regVersion, String contactURI) throws ParseException {
		String id = null;
		if (regId != null)
			id = Byte.toString(regId);
		else
			id = "1";
		
		String pui = p.getProperty(SettingConstants.PUI);
		String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" 
			+ "<reginfo xmlns=\"urn:ietf:params:xml:ns:reginfo\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"" + regVersion 
			+ "\" state=\"full\">\r\n\t<registration aor=\"sip:" 
			+  pui + "\" id=\"" 
			+ id + "\" state=\"active\">\r\n\t\t<contact id=\"" 
			+ id + "\" state=\"active\" event=\"registered\">\r\n\t\t\t<uri>" 
			+ contactURI + "</uri>\r\n";
		
//		 TEMP
//		String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" 
//			+ "<reginfo xmlns=\"urn:ietf:params:xml:ns:reginfo\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"" + regVersion 
//			+ "\" state=\"full\"><registration aor=\"sip:" 
//			+  pui + "\" id=\"" 
//			+ id + "\" state=\"active\"><contact id=\"" 
//			+ id + "\" state=\"active\" event=\"registered\"><uri>" 
//		
//		+ contactURI + "</uri>"
//
//		+ "<unknown-param name=\"+sip.instance\"><urn:uuid:" 
//			+ p.getProperty(SettingConstants.UUID)
//			+ "></unknown-param><unknown-param name=\"reg-id\">"
//			+ regId + "</unknown-param>" 
//
//			;
//		END_TEMP
		
		if (ss.getGRUU() == Extension.REQUIRED) {
			body += "\t\t\t<unknown-param name=\"+sip.instance\"><" 
				+ p.getProperty(SettingConstants.SIP_INSTANCE_UUID)
				+ "></unknown-param>\r\n\t\t\t<gruu>sip:"
				+ pui + ";gruu;opaque=\"" 
				+ p.getProperty(SettingConstants.OPAQUE_UUID)
				+ "\"</gruu>\r\n";
			
		}
		
 		body += "\t\t</contact>\r\n\t</registration>\r\n</reginfo>\r\n";
// 	TEMP	body += "</contact></registration></reginfo>";
		
		return body;
	}
	/**
	 * Creats a SIP URI from the IP address of the properties
	 * file given in the argument.
	 * 
	 * @param p - the properties information for the network element
	 * 			that the URI is being created on behalf of.
	 * @return - the constructed SIP URI.
	 * @throws ParseException
	 */
	protected SipURI createRegisterRequestURI(Properties p, int port) throws ParseException {
		String domain = p.getProperty(SettingConstants.DOMAIN);
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
		if (addrType != null && 
				addrType.equals(SettingConstants.IP))
			domain = p.getProperty(SettingConstants.IP);
		
		SipURI requestURI =
			addressFactory.createSipURI(null, domain);
		return requestURI;
	}

	/**
	 * Creats a URI from the IP address of the properties
	 * file given in the argument.
	 * 
	 * @param addrSpec - the string representation of the uri.
	 * @return - the constructed SIP URI.
	 * @throws ParseException
	 */
	protected URI createRequestURI(String addrSpec) throws ParseException {
		URI requestURI = null;
		if (addrSpec != null) 
			requestURI =
				addressFactory.createURI(addrSpec);
		
		return requestURI;
	}
	
	/**
	 * Creats a SIP URI from the IP address of the properties
	 * file given in the argument.
	 * 
	 * @param p - the properties information for the network element
	 * 			that the URI is being created on behalf of.
	 * @return - the constructed SIP URI.
	 * @throws ParseException
	 */
	protected SipURI createRequestSipURI(String addrSpec) throws ParseException {
		SipURI requestURI = null;
		String temp = addrSpec;
		if (addrSpec != null) {
			if (temp.charAt(0) == '<')
				temp = temp.substring(1);
			if (temp.charAt(temp.length()-1) == '>')
				temp = temp.substring(0, temp.length()-1);
			requestURI =
				addressFactory.createSipURI(temp);
		}
		
		return requestURI;
	}

	/**
	 * Creats a SIP URI from the IP address of the properties
	 * file given in the argument.
	 * 
	 * @param p - the properties information for the network element
	 * 			that the URI is being created on behalf of.
	 * @return - the constructed SIP URI.
	 * @throws ParseException
	 */
	protected SipURI createRequestURI(Properties p, int port, String addrType) throws ParseException {
		SipURI requestURI = null;
		String user = p.getProperty(SettingConstants.USER_NAME);
		String type = addrType;
		if (addrType != null && addrType.equals(SettingConstants.FQDN))
			type = SettingConstants.DOMAIN;
				
		String host = getHost(p, type);
		requestURI =
			addressFactory.createSipURI(user, host);
		if (requestURI != null && port > 0)
			requestURI.setPort(port);

		return requestURI;
	}
	/**
	 * Creates the Require Header for a SIP message based upon the 
	 * current extension settings for the platform.
	 * 
	 * @param options - other extensions to include in the header
	 * @return - the Require Header
	 * @throws ParseException
	 */
	protected RequireHeader createRequireHeader(String options) throws ParseException {
		String require = null;
		int count = 0;

		if (options != null) {
			require = options;
			count++;
		}

		if (ss.getGRUU() == Extension.REQUIRED) {
			if (count > 0)
				require += new String(",gruu");
			else
				require = new String("gruu");
			count++;
		}
		if (ss.getPrecondition() == Extension.REQUIRED) {
			if (count > 0)
				require += new String(",precondition");
			else
				require = new String("precondition");
			count++;
		}
		if (ss.getReliability() == Extension.REQUIRED) {
			if (count > 0)
				require += new String(",100rel");
			else
				require = new String("100rel");
			count++;
		}		
		RequireHeader header = null; 
		if (require != null)
			header = headerFactory.createRequireHeader(require);
		return header;
	}
	
	/**
	 * Creates the Route Header for a SIP message based upon the 
	 * current extension settings for the platform.
	 * 
	 * @param p - the property information for the network element to 
	 * 	include in the header
	 * @param includeKA - specifies whether to include the keep-stun 
	 * 	parameter or not
	 * @param port - the transport port number being used for the message.
	 * 
	 * @return - the Route Header
	 * @throws ParseException
	 */
	protected RouteHeader createRouteHeader(Properties p, boolean includeKA, int port, String transport) throws ParseException {
//		String host = null;
//		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
//		String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
//
//		if (addrType == null || 
//				addrType.equals(SettingConstants.IP) ||
//				addrType.equals(SettingConstants.LAB_FQDN)) {
//			host = p.getProperty(SettingConstants.IP) + ":" + ((Integer)port).toString();
//		
//		}
//		else if (addrType.equals(SettingConstants.FQDN)) {
//			host = p.getProperty(SettingConstants.FQDN);
//		}
//
//		SipURI address =
//			addressFactory.createSipURI(null, host);
		SipURI address = createSipURI(p, null, port);

		
		Address routeAddress = addressFactory.createAddress(address);
		if (includeKA)
			address.setParameter("keep-stun", null);
		address.setParameter("lr", null);
		if (address != null && transport != null)
			address.setTransportParam(transport.toLowerCase());
		RouteHeader header = headerFactory.createRouteHeader(routeAddress);

	

		return header;
	}
	
	/**
	 * Creates the Route Header for a SIP message based upon the 
	 * current extension settings for the platform.
	 * 
	 * @param p - the property information for the network element to 
	 * 	include in the header
	 * @param includeKA - specifies whether to include the keep-stun 
	 * 	parameter or not
	 * @param port - the transport port number being used for the message.
	 * 
	 * @return - the Route Header
	 * @throws ParseException
	 */
	protected RouteHeader createRouteHeader(RecordRouteHeader rr, String transport) throws ParseException {
		if (rr != null) {
			Address addr = rr.getAddress();
			if (addr != null && 
					transport != null &&
					addr.getURI().isSipURI()) {
				SipURI uri = (SipURI)addr.getURI();
				uri.setTransportParam(transport.toLowerCase());
			}
			RouteHeader header = headerFactory.createRouteHeader(addr);
			
			return header;
		}
		return null;
	}
	/**
	 * Creates a RSeq Header with a new number for a SIP request message. 
	 * 
	 * @return - A RSeq Header
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected RSeqHeader createRSeqHeader() throws ParseException, InvalidArgumentException {
		// Create a new Cseq header
		RSeqHeader header =
			headerFactory.createRSeqHeader((++prackSeqNum));
		return header;
	}
	protected Header createServiceRouteHeader(Properties scscf) throws ParseException {
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);
		
		String value = null;
		if (addrType == null || 
				addrType.equals(SettingConstants.IP) ||
				addrType.equals(SettingConstants.LAB_FQDN)) {
			String ip = scscf.getProperty(SettingConstants.IP);
			if (Conversion.isIPv6Address(ip)) {
				value = "<sip:[" + ip + "]>";
			} else {
				value = "<sip:" + ip + ">";
			}
		}
		else if (addrType.equals(SettingConstants.FQDN)) {
			
			String fqdn = scscf.getProperty(SettingConstants.FQDN);
			// if the fqdn is really an ipv6 address put it in []
			String[] parts = fqdn.split(":");
			if (parts.length == 1)
				value = "<sip:" + fqdn + ";lr>";
			else
				value = "<sip:[" + fqdn + "];lr>";
		}
		
		Header header = headerFactory.createHeader("Service-Route", value);
		return header;
	}

	protected Header createSIPETagHeader(Properties p, String entityTag) throws ParseException {

		Header header = headerFactory.createHeader("SIP-ETag", entityTag );
		return header;
	}
	
	protected Header createSIPIfMatchHeader(Properties p, String entityTag) throws ParseException {

		Header header = headerFactory.createHeader("SIP-If-Match", entityTag );
		return header;
	}
	
	protected SubscriptionStateHeader createSubscriptionStateHeader(String state, int expires) throws ParseException, InvalidArgumentException {

		SubscriptionStateHeader header = headerFactory.createSubscriptionStateHeader(state);
		if (expires >= 0)
			header.setExpires(expires);
	
		return header;
	}
	/**
	 * Creates the Supported Header for a SIP message based upon the 
	 * current extension settings for the platform.
	 * 
	 * @param options - the initial options that are not externally configurable.
	 * 
	 * return - the Supported Header
	 * @throws ParseException
	 */
	protected SupportedHeader createSupportHeader(String options) throws ParseException {
		String support = null;
		int count = 0;

		if (options != null) {
			support= options;
			count++;
		}

		if (ss.getGRUU() == Extension.SUPPORTED) {
			if (count > 0)
				support += new String(",gruu");
			else
				support = new String("gruu");
			count++;
		}
		if (ss.getPrecondition() == Extension.SUPPORTED) {
			if (count > 0)
				support += new String(",precondition");
			else
				support = new String("precondition");
			count++;
		}
		if (ss.getReliability() == Extension.SUPPORTED) {
			if (count > 0)
				support += new String(",100rel");
			else
				support = new String("100rel");
			count++;
		}		
		SupportedHeader header = null; 
		if (support != null)
			header = headerFactory.createSupportedHeader(support);
		return header;
	}

	protected TargetDialogHeader createTargetDialogHeader(String callId, String localTag, String remoteTag) throws ParseException {
//		String td = callId; // + ";local-tag=" + localTag + ";remote-tag=" + remoteTag;
		TargetDialogHeader header = null;
		try {
			header = headerFactory.createTargetDialogHeader(callId);

			if (header != null) {
				header.setParameter("local-tag", localTag);
				header.setParameter("remote-tag", remoteTag);
			}
		}
		catch (Exception e) {
			logger.warn(PC2LogCategory.SIP, subCat, 
					"SIPUtils couldn't create the Target-Dialog header because it encountered an exception.\n" 
					+ e.getMessage());
		}
		return header;
	}
	
	/**
	 * Converts a From Header to a To Header.
	 * 
	 * @param from - the From Header to convert
	 * @return - the To header
	 * @throws ParseException
	 */
	protected ToHeader createToHeader(FromHeader from) throws ParseException {
		ToHeader toHeader = headerFactory.createToHeader(from.getAddress(), from.getTag());
		return toHeader;
	}


	/**
	 * Creates a To Header for the message based upon the properties file
	 * and to tag given as parameters.
	 * @param p - the properties information for the network element that
	 * 			the To header is being create for.
	 * @param tag - the tag to use in the header
	 * @param includeDisplayName - a flag stating whether to include the
	 * 		display-name property for the network element.
	 * @param port - the transport port number being used for the message.
	 * 
	 * @return - the To header for the network element.
	 * @throws ParseException
	 */
	protected ToHeader createToHeader(Properties p, String tag, 
			boolean includeDisplayName, int port) throws ParseException {
//		String toSipAddress = null;
		String name = p.getProperty(SettingConstants.USER_NAME);
		String toDisplayName = p.getProperty(SettingConstants.DISPLAY_NAME);
//		boolean addPort = false;
		
//		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
//		String addrType = platform.getProperty(SettingConstants.ADDR_FORMAT);

//		if (addrType == null || 
//				addrType.equals(SettingConstants.IP) ||
//				addrType.equals(SettingConstants.LAB_FQDN)) {
//			//toSipAddress = p.getProperty(SettingConstants.IP) + ":" + ((Integer)port).toString();
//			toSipAddress = p.getProperty(SettingConstants.IP); 
//			if (Conversion.isIPv6Address(toSipAddress)) {
//				String zone = p.getProperty(SettingConstants.IPv6_ZONE);
//				toSipAddress = Conversion.addZone(toSipAddress, zone); 
//			}
//			addPort = true;
//		}
//		else if (addrType.equals(SettingConstants.FQDN)) {
//			toSipAddress = p.getProperty(SettingConstants.DOMAIN);
//		}
//
//
//		SipURI toAddress =
//			addressFactory.createSipURI(toUser, toSipAddress);
		SipURI toAddress = createSipURI(p, name, port);
		
		Address toNameAddress = addressFactory.createAddress(toAddress);
		if (includeDisplayName)
			toNameAddress.setDisplayName(toDisplayName);
//		if (addPort) {
//			toAddress.setPort(port);
//		}
		ToHeader toHeader =
			headerFactory.createToHeader(toNameAddress, tag);
		return toHeader;
	}

	protected String getHost(Properties p, String addrType) {
		String host = null;
		
		if (addrType == null || 
				addrType.equals(SettingConstants.IP) ||
				addrType.equals(SettingConstants.LAB_FQDN)) {
			host = p.getProperty(SettingConstants.IP);
			if (Conversion.isIPv6Address(host)) {
				String zone = p.getProperty(SettingConstants.IPv6_ZONE);
				host = Conversion.addZone(host, zone); 
			}
		}
		else if (addrType.equals(SettingConstants.FQDN))
			host = p.getProperty(SettingConstants.FQDN); 
		else if (addrType.equals(SettingConstants.DOMAIN))
			host = p.getProperty(SettingConstants.DOMAIN); 

		if (host == null) 
			logger.warn(PC2LogCategory.SIP, subCat, 
					"Couldn't find information to build URI for a header.");
		
		return host;
	}
	
	protected int getPort(Properties p, String transport) {
		int port = -1;

		if (transport.equalsIgnoreCase("UDP"))
			port = Integer.parseInt(p.getProperty(SettingConstants.UDP_PORT));
		else if (transport.equalsIgnoreCase("TCP"))
			port = Integer.parseInt(p.getProperty(SettingConstants.TCP_PORT));
		else if (transport.equalsIgnoreCase("TLS"))
			port = Integer.parseInt(p.getProperty(SettingConstants.TLS_PORT));
		return port;
	}

	protected String createUAProfileBody(Properties p) {
		String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n"
			+ "<ATP:CL-PKTC-FEATURE-DATA xmlns:ATP=\"http://www.cablelabs.com/namespaces/PacketCable/"
			+ "R2/XSD/v1/CL-PKTC-RST-FT-DATA\" "
			+ "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
			+ "xsi:schemaLocation=\"http://www.cablelabs.com/namespaces/"
			+ "PacketCable/R2/XSD/v1/CL-PKTC-RST-FT-DATA DFD_Schema.xsd\">\r\n"
			+ "<pktcFeatureData>\r\n<pktcRSTFMinorVersion>0</pktcRSTFMinorVersion>\r\n"
			+ "<ActiveFeatureSet>\r\n<SetElement>cfv</SetElement>\r\n"
			+ "<SetElement>scf</SetElement>\r\n<SetElement>dnd</SetElement>\r\n"
			+ "</ActiveFeatureSet>\r\n<CFVForwardedCallCount>1</CFVForwardedCallCount>\r\n"
			+ "<SCFForwardedCallCount>1</SCFForwardedCallCount>\r\n</pktcFeatureData>\r\n"
			+ "</ATP:CL-PKTC-FEATURE-DATA>\r\n";
		return body;
	}
	
	/**
	 * Creates the Via Header for the network element described by the
	 * properties parameter.
	 * @param p - the properties file describing the network element
	 * @param cSeqNo - the CSeq number being used for the message.
	 * @param transport - the transport protocol being used for the message.
	 * @param includeLR - include the lr parameter
	 * @return - the Via Header constructed for the network element
	 * @throws ParseException
	 * @throws InvalidArgumentException
	 */
	protected ViaHeader createViaHeader(Properties p, String postfix, String transport, 
			boolean includeLR) throws ParseException, InvalidArgumentException {
		
		String ne = p.getProperty(SettingConstants.NE);
		
		int port = getPort(p, transport);

		// GLH ADDR
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		String addrType =  platform.getProperty(SettingConstants.ADDR_FORMAT); // "IP"; 
		String host = getHost(p, addrType);

		String b = generateBranch(ne, postfix);
		
		ViaHeader header =
			headerFactory.createViaHeader(
					host,
					port,
					transport.toLowerCase(),
					b);
 
		// Add keep-stun if the device is a PCSCF
		if (ne.startsWith("PCSCF"))
			header.setParameter("keep-stun", null);
		if (includeLR)
			header.setParameter("lr", null);
		return header;
	}
	// Notes for headers that will eventually have to be supported by the Utils
	// class

	/*
  	protected AcceptHeader createAcceptHeader(Properties p) throws ParseException {

		AcceptHeader header = headerFactory.createAcceptHeader();
		return header;
	}

	// GLH missing Accept-Contact definition
//	protected AcceptContactHeader createAcceptContactHeader(Properties p) throws ParseException {
//		
//		AcceptContactHeader header = headerFactory.createAcceptContactHeader();
//	}
	protected AlertInfoHeader createAlertInfoHeader(Properties p) throws ParseException {

		AlertInfoHeader header = headerFactory.createAlertInfoHeader();
		return header;
	}
	protected AllowHeader createAllowHeader(Properties p) throws ParseException {

		AllowHeader header = headerFactory.createAllowHeader();
		return header;
	}
	protected AllowEventsHeader createAllowEventsHeader(Properties p) throws ParseException {

		AllowEventsHeader header = headerFactory.createAllowEventsHeader();
		return header;
	}
	protected AuthenticationInfoHeader createAuthenticationInfoHeader(Properties p) throws ParseException {

		AuthenticationInfoHeader header = headerFactory.createAuthenticationInfoHeader();
		return header;
	}

	protected ContentDispositionHeader createContentDispositionHeader(Properties p) throws ParseException {

		ContentDispositionHeader header = headerFactory.createContentDispositionHeader();
		return header;
	}
	protected EventHeader createEventHeader(Properties p) throws ParseException {

		EventHeader header = headerFactory.createEventHeader();
		return header;
	}*/

	/*	protected MinExpiresHeader createMinExpiresHeader(Properties p) throws ParseException {

		MinExpiresHeader header = headerFactory.createMinExpiresHeader();
		return header;
	}*/

	/*
 //	 missing P-Asserted-Identity definition
//	protected PAssertedIdentityHeader createPAssertedIdentityHeader(Properties p) throws ParseException {
//		
//		PAssertedIdentityHeader header = headerFactory.createPAssertedIdentityHeader();
//		return header;
//	}
	// P-Associated-URI definition
//	protected PAssociatedURIHeader createPAssociatedURIHeader(Properties p) throws ParseException {
//		
//		PAssociatedURIHeader header = headerFactory.createPAssociatedURIHeader();
//		return header;
//	} */

	/*protected PCalledPartyIDHeader createPCalledPartyIDHeader(Properties p) throws ParseException {

		PCalledPartyIDHeader header = headerFactory.createPCalledPartyIDHeader();
		return header;
	}
	protected PChargingFunctionAddressHeader createPChargingFunctionAddressHeader(Properties p) throws ParseException {

		PChargingFunctionAddressHeader header = headerFactory.createPChargingFunctionAddressHeader();
		return header;
	}
	protected PChargingVectorHeader createPChargingVectorHeader(Properties p) throws ParseException {

		PChargingVectorHeader header = headerFactory.createPChargingVectorHeader();
		return header;
	}
	protected PPreferredIdentityHeader createPPreferredIdentityHeader(Properties p) throws ParseException {

		PPreferredIdentityHeader header = headerFactory.createPPreferredIdentityHeader();
		return header;
	}
	protected PrivacyHeader createPrivacyHeader(Properties p) throws ParseException {

		PrivacyHeader header = headerFactory.createPrivacyHeader();
		return header;
	}
	protected ProxyAuthenticateHeader createProxyAuthenticateHeader(Properties p) throws ParseException {

		ProxyAuthenticateHeader header = headerFactory.createProxyAuthenticateHeader();
		return header;
	}
	protected ProxyRequireHeader createProxyRequireHeader(Properties p) throws ParseException {

		ProxyRequireHeader header = headerFactory.createProxyRequireHeader();
		return header;
	}*/



	/*	protected ReasonHeader createReasonHeader(Properties p) throws ParseException {

		ReasonHeader header = headerFactory.createReasonHeader();
		return header;
	}
	protected ReferredByHeader createReferredByHeader(Properties p) throws ParseException {

		ReferredByHeader header = headerFactory.createReferredByHeader();
		return header;
	}


	protected RejectContactHeader createRejectContactHeader(Properties p) throws ParseException {

		RejectContactHeader header = headerFactory.createRejectContactHeader();
		return header;
	}
	protected ReplacesHeader createReplacesHeader(Properties p) throws ParseException {

		ReplacesHeader header = headerFactory.createReplacesHeader();
		return header;
	}
	protected RequestDispositionHeader createRequestDispositionHeader(Properties p) throws ParseException {

		RequestDispositionHeader header = headerFactory.createRequestDispositionHeader();
		return header;
	}
	protected RequireHeader createRequireHeader(Properties p) throws ParseException {

		RequireHeader header = headerFactory.createRequireHeader();
		return header;
	}
	protected RetryAfterHeader createRetryAfterHeader(Properties p) throws ParseException {

		RetryAfterHeader header = headerFactory.createRetryAfterHeader();
		return header;
	}

	protected RSeqHeader createRSeqHeader(Properties p) throws ParseException {

		RSeqHeader header = headerFactory.createRSeqHeader();
		return header;
	}*/



	/*protected SIPEtagHeader createSIPEtagHeader(Properties p) throws ParseException {

		SIPEtagHeader header = headerFactory.createSIPEtagHeader();
		return header;
	}
	protected SIPIfMatchHeader createSIPIfMatchHeader(Properties p) throws ParseException {

		SIPIfMatchHeader header = headerFactory.createSIPIfMatchHeader();
		return header;
	}

	protected UnsupportedHeader createUnsupportedHeader(Properties p) throws ParseException {

//		UnsupportedHeader header = headerFactory.createUnsupportedHeader();
		return header;
	} */
	protected WWWAuthenticateHeader createWWWAuthenticateHeader(Properties src, Properties target) throws ParseException {

		WWWAuthenticateHeader header = headerFactory.createWWWAuthenticateHeader(ParameterNames.DIGEST);

		// For realm we use the domain property
		header.setRealm(target.getProperty(SettingConstants.DOMAIN));
		// auth-int was removed from the possibilities for the 
		// time being
		// header.setQop("auth,auth-int");
		header.setQop("auth");
		String nonce = ((Long)System.currentTimeMillis()).toString();
		//byte [] opaqueBuf = new byte [16];

		byte [] nvDigest = digester.digest(nonce.getBytes());
		StringBuffer hexNV = asHexString(nvDigest);
		header.setNonce(hexNV.toString());

		// OPAQUE parameter is no longer necessary according to Stuart Hoggan
//		long opaqueNum = rand.nextLong();
//		String ov = ((Long)opaqueNum).toString();
//		byte [] oDigest = digester.digest(ov.getBytes());
//		StringBuffer hexO = asHexString(oDigest);
//		header.setOpaque(hexO.toString());

		header.setAlgorithm("MD5");
		return header;
	}

	/**
	 * Creates a Reg-Info Body to be sent in a NOTIFY message.
	 */
//	protected String createPresenceBody(Properties p, PC2SipData sipData) throws ParseException {
//		// LinkedList<String> elements = PresenceServer.get;
//		String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
//		   "<presence xmlns=\"urn:ietf:params:xml:ns:pidf\"" +
//		       "entity=\"sip:presenceServer@pclab.com\">\r\n" +
//		     "<tuple id=\"ue1\">\r\n" +
//		       "<status>\r\n" +
//		         "<basic>open</basic>\r\n" +
//		       "</status>\r\n" +
//		       "<contact priority=\"0.8\">sip:UE1@pclab.com</contact>\r\n" +
//		     "</tuple>\r\n" +
//		   "</presence>\r\n";
//		return body;
//	} 

	/**
	 * Creates a PIDF Body to be sent in a NOTIFY message.
	 */
	protected String createPidfBody(Properties p, String status) throws ParseException {
		// LinkedList<String> elements = PresenceServer.get;
		String user = p.getProperty(SettingConstants.USER_NAME);
		String domain = p.getProperty(SettingConstants.DOMAIN);
		String ne = p.getProperty(SettingConstants.NE);
		String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
		   "<presence xmlns=\"urn:ietf:params:xml:ns:pidf\"" +
		       "entity=\"pres:presenceServer@pclab.com\"\r\n" +
		       "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
		       "xmlns:dm=\"urn:ietf:params:xml:ns:pidf:data-model\"\r\n" +
		       "xmlns:r=\"urn:ietf:params:xml:ns:pidf:rpid\">\r\n" +
		     "<tuple id=\"" + ne + "\">\r\n" +
		       "<status>\r\n" +
		         "<basic>" + status.toLowerCase() + "</basic>\r\n" +
		       "</status>\r\n" +
		       "<contact priority=\"0.8\">sip:" 
		       + user + "@" + domain + "</contact>\r\n" +
		     "</tuple>\r\n" +
		     "<dm:device id=\"stb001\">\r\n" +
		     	"<dm:note>tbs,101</dm:note>\r\n" +
		     "</dm:device>\r\n" +
		     "<dm:person id=\"alice\">\r\n" +
		     	"<r:activities>\r\n" +
		     		"<r: tv/>\r\n" +
		     	"</r:activites>\r\n" +
		     	"<r:mood>\r\n" +
		     		"<r:happy/>\r\n" +
		     	"</r:mood>\r\n" +
		     "</dm:person>\r\n" +
		   "</presence>\r\n";
		return body;
	} 
	/** 
	 * Creates an SDP body for inclusion within a SIP message based upon
	 * the network element described by the properties parameter
	 * 
	 * @param p - the properties of the network element
	 * @param offer - is the SDP an offer or an answer
	 * @param remoteDirectionTag - the value to set for the remote Direction Tag in an SDP answer
	 * @param reinvite - SDP is for a reinvite so don't include precondition information
	 * @param usePort2 - a flag indicating that the send SDP port setting should be used instead
	 * 		of the normal first port.
	 * @param sessionId - the session-id field to use in the o line.
	 * @param sessionVersion - the session-version field to use in the o line.
	 * 
	 * @return - the String SDP body
	 */
	protected String createSDPData(Properties p, String neLabel,
			boolean offer, String remoteDirectionTag,
			boolean reinvite, boolean usePort2,
			long sessionId, long sessionVersion) {
		String ip = p.getProperty(SettingConstants.IP);
		String port = null;
			
		if (usePort2)
			port = p.getProperty(SettingConstants.SDP_PORT2);
		else 
			port = p.getProperty(SettingConstants.SDP_PORT);
		
		String username = p.getProperty(SettingConstants.USER_NAME);

		if (port == null) {
			if (neLabel.equals(SettingConstants.DUT)){
				port = DEFAULT_PORT;
			}
			else {
				logger.warn(PC2LogCategory.SIP, subCat, "SIPUtils could not find the setting for " 
					+ SettingConstants.SDP_PORT + " for network element label "
					+ neLabel);
			}
		}
		
		StringTokenizer tokens = new StringTokenizer(ip, ".");
		String ipv = "IP6";
		if (tokens.countTokens() == 4) {
			ipv = "IP4";
		}

		if (username == null)
			username = "-";

		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		String body = platform.getProperty("SDP Body");
		//String ignoreBWMod = platform.getProperty("SDP Ignore Bandwidth Modifiers");
		boolean includeBW = !(SystemSettings.getBooleanSetting("SDP Ignore Bandwidth Modifiers"));
		String codec = "0";
		String codecName = "PCMU";
		String clockRate = "8000";
		// According to Kevin the values for AS is 80 and TIAS is 64000 for 20 msec
		// G711u law codec.
		String as = "80"; // Previous value "11";
		String tias = "64000"; // Previous value "80000";
		String maxprate = "50.0";
		String dir = "sendrecv";
		String ptime = "20";
		if (body != null && body.equalsIgnoreCase("wide-band")) {
			codec = "9";
                        codecName = "G722-64";
                        as = "80";
                        tias = "64000";
                        maxprate = "50.0";
		}
		else if (body != null && body.equalsIgnoreCase("video")) {
			codec = "100";
			codecName = "H264";
			clockRate = "90000";
			as = "22";
			tias = "160000";
			if (offer)
				dir = "sendonly";
			else 
				dir = "recvonly";
		}
		

		String sdpData =
			"v=0\r\n"
			+ "o=" + username + " " + sessionId + " " + sessionVersion  // "13760799956958020 13760799956958020"
			+ " IN " + ipv + " " + ip + "\r\n"
			+ "s=-\r\n" 
			+ "c=IN " + ipv + " " + ip + "\r\n"
			+ "t=0 0\r\n"
			+ "m=audio " + port + " RTP/AVP " + codec + "\r\n";

		if (includeBW) {
			sdpData += "b=AS:" + as + "\r\n"			
			+ "b=TIAS:" + tias +"\r\n"
			+ "a=maxprate:" + maxprate + "\r\n";
		}

		sdpData	+= "a=rtpmap:" + codec + " " + codecName + "/" + clockRate + "\r\n" // Wide band change 0 to 9
		+ "a=" + dir + "\r\n"
		+ "a=ptime:" + ptime + "\r\n";

		if (!reinvite && ss.getPrecondition() != Extension.DISABLED) {
			String localStrength = "mandatory";
			if (ss.getPrecondition() == Extension.SUPPORTED)
				localStrength = "optional";
			if (offer) {
				sdpData += SDPConstants.A_CUR_QOS_L + "sendrecv\r\n"
					+ SDPConstants.A_CUR_QOS_R + "none\r\n"
					+ SDPConstants.A_DES_QOS + localStrength + " local sendrecv\r\n"
					+ SDPConstants.A_DES_QOS + localStrength + " remote sendrecv\r\n"
					+ SDPConstants.A_CONF_QOS_R + "sendrecv\r\n";
			}
			else if (remoteDirectionTag != null){
				sdpData += SDPConstants.A_CUR_QOS_L + "sendrecv\r\n"
					+ SDPConstants.A_CUR_QOS_R + remoteDirectionTag + "\r\n"
					+ SDPConstants.A_DES_QOS + localStrength + " local sendrecv\r\n"
					+ SDPConstants.A_DES_QOS + localStrength + " remote sendrecv\r\n"
					+ SDPConstants.A_CONF_QOS_R + "sendrecv\r\n";
			}
		}

		return sdpData;
	}

	/**
	 * This method creates the SessionReport metrics used in a PUBLISH message to
	 * report the metrics of the simulated UE.
	 * 
	 * @return
	 * TODO use real data in the report details
	 */
	protected String createSessionReport() {
		String sessRpt = "VQSessionReport\r\n"
			+ "LocalMetrics:\r\n"
			+ "TimeStamps:START=2004-10-10T18:23:43Z STOP=2004-10-01T18:26:02Z\r\n"
			+ "SessionDesc:PT=18 PD=G729 SR=8000 FD=20 FO=20 FPP=2 PPS=50 FMTP=\"annexb=no\" PLC=3 SSUP=on\r\n"
			+ "CallID:1890463548@alice.example.org\r\n"
			+ "LocalAddr:IP=10.10.1.100 PORT=5000 SSRC=2468abcd\r\n"
			+ "RemoteAddr:IP=11.1.1.150 PORT=5002 SSRC=1357efff\r\n"
			+ "JitterBuffer:JBA=3 JBR=2 JBN=40 JBM=80 JBX=120\r\n"
			+ "PacketLoss:NLR=5.0 JDR=2.0\r\n"
			+ "BurstGapLoss:BLD=0 BD=0 GLD=2.0 GD=500 GMIN=16\r\n"
			+ "Delay:RTD=200 ESD=140 IAJ=2 OWD=100\r\n"
			+ "Signal:SL=2 NL=-10 RERL=14\r\n"
			+ "QualityEst:RLQ=90 RCQ=85 EXTRI=90 MOSLQ=3.4 MOSCQ=3.3 QoEEstAlg=AlgX\r\n"
			+ "RemoteMetrics:\r\n"
			+ "TimeStamps:START=2004-10-10T18:23:43Z STOP=2004-10-01T18:26:02Z\r\n"
			+ "SessionDesc:PT=18 PD=G729 SR=8000 FD=20 FO=20 FPP=2 PPS=50 FMTP=\"annexb=no\" PLC=3 SSUP=on\r\n"
			+ "CallID:1890463548@alice.example.org\r\n"
			+ "LocalAddr:IP=11.1.1.150 PORT=5002 SSRC=1357efff\r\n"
			+ "RemoteAddr:IP=10.10.1.100 PORT=5000 SSRC=2468abcd\r\n"
			+ "JitterBuffer:JBA=3 JBR=2 JBN=40 JBM=80 JBX=120\r\n"
			+ "PacketLoss:NLR=5.0 JDR=2.0\r\n"
			+ "BurstGapLoss:BLD=0 BD=0 GLD=2.0 GD=500 GMIN=16\r\n"
			+ "Delay:RTD=200 ESD=140 IAJ=2 OWD=100\r\n"
			+ "Signal:SL=2 NL=-10 RERL=0\r\n"
			+ "QualityEst:RLQ=90 RCQ=85 MOSLQ=3.4 MOSCQ=3.3 QoEEstAlg=AlgX\r\n"
			+ "DialogID:1890463548@alice.example.org;to-tag=8472761;from-tag=9123dh311\r\n";

		return sessRpt;
	}
	
	protected String generateBranch(String ne, String postfix) {
		if (postfix == null)
			return mc + "_" + ne + "-" + (++BRANCH);
		else
			return mc + "_" + ne + "-" + postfix;
		
	}
	
	protected String generateBranch(String postfix) {
		if (postfix == null)
			return mc + "-" + (++BRANCH);
		else
			return mc + "-" + postfix;
		
	}
}
