/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.stun;

import com.cablelabs.common.*;
import com.cablelabs.log.*;
import com.cablelabs.stun.attributes.*;

import java.net.*;

public class StunFactory {

	private LogAPI logger = LogAPI.getInstance(); // Logger.getLogger(StunConstants.loggerName);
//	private MessageDigest mdSha = null;

	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Stack";
		
	public StunFactory() {
		try {
//			mdSha = MessageDigest.getInstance("SHA");
			
		}
		catch (Exception ex) {
			logger.error(PC2LogCategory.STUN, subCat,
					"StunFactory failed to obtain the SHA message digest!");
		}
	}

	public StunMessage createAllocateRequest(Transport t, boolean includeBW,
			int ttl, boolean includeProps, boolean includeResvToken,
			String username, String realm, 
			String password, Nonce nonce, 
			boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating AllocateRequest message.");
		
		StunMessage req = new StunMessage(StunConstants.ALLOCATE_REQUEST_MSG_TYPE);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				req.addAttribute(user);
		}
		if (realm != null) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				req.addAttribute(r);
		}
		// Let the nonce self calculate a value
		if (nonce != null)
			req.addAttribute(nonce);
		else {
			Nonce n = new Nonce();
			if (n != null) {
				req.addAttribute(n);
			}
		}
		if (t != null) {
			RequestedTransport rt = new RequestedTransport();
			if (rt != null)
				req.addAttribute(rt);
		}
		if (includeBW) {
			// TODO Bandwidth b = new Bandwidth()
		}
		// if ttl is greater than 0, add Lifetime attribute
		if (ttl > 0) {
//			Lifetime l =  null;
			if (ttl == Allocation.TTL) {
			//	TODO l = new Lifetime()
			}
			else {
			// TODO	l = new Lifetime()
			}
//			if (l != null)
//				req.addAttribute(l);
		}
		// TODO ReservationToken and RequestedProps attributes must be 
		// mutually exclusive
		if (includeResvToken && !includeProps) {
			
		}
		else if (!includeResvToken && includeProps) {
			RequestedProps rp = new RequestedProps(RequestedProps.EVEN_PORT_NUMBER);
			if (rp != null)
				req.addAttribute(rp);
		}
	
		addMessageIntegrity(req, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(req);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "AllocateRequest message created.");
		return req;
	}
	
	public StunMessage createAllocateResponse(Transport t, boolean includeBW,
			int ttl, boolean includeProps, boolean includeResvToken,
			String username, String realm, 
			String password, Nonce nonce,
			boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating AllocateResponse message.");
		
		StunMessage resp = new StunMessage(StunConstants.ALLOCATE_RESPONSE_MSG_TYPE);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				resp.addAttribute(user);
		}
		if (realm != null) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				resp.addAttribute(r);
		}
		// Let the nonce self calculate a value
		if (nonce != null)
			resp.addAttribute(nonce);
		else {
			Nonce n = new Nonce();
			if (n != null) {
				resp.addAttribute(n);
			}
		}
		if (t != null) {
			RequestedTransport rt = new RequestedTransport();
			if (rt != null)
				resp.addAttribute(rt);
		}
		if (includeBW) {
			byte [] bw = Conversion.intToByteArray(Allocation.maxBW);
			Bandwidth b = new Bandwidth(bw);
			if (b != null)
				resp.addAttribute(b);
		}
		// if ttl is greater than 0, add Lifetime attribute
		if (ttl > 0) {
			Lifetime l =  null;
			if (ttl == Allocation.TTL) {
				byte [] lifetime = Conversion.intToByteArray(ttl);
				l = new Lifetime(lifetime);
			}
			else {
			// TODO	l = new Lifetime()
			}
			if (l != null)
				resp.addAttribute(l);
		}
		// TODO ReservationToken and RequestedProps attributes must be 
		// mutually exclusive
		if (includeResvToken && !includeProps) {
			
		}
		else if (!includeResvToken && includeProps) {
			
		}
	
		addMessageIntegrity(resp, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(resp);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "AllocateResponse message created.");
		return resp;
	}
	
	public StunMessage createAllocateResponse(Transport t, boolean includeBW,
			int ttl, boolean includeProps, boolean includeResvToken,
			StunMessage req, String password, boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating AllocateResponse message.");
		
		StunMessage resp = new StunMessage(StunConstants.ALLOCATE_RESPONSE_MSG_TYPE);
		Username user = (Username)req.getAttribute(StunConstants.USERNAME_TYPE);
		Realm r = (Realm)req.getAttribute(StunConstants.REALM_TYPE);
		Nonce n = (Nonce)req.getAttribute(StunConstants.NONCE_TYPE);
		String username = new String(user.getValue());
		String realm = new String(r.getValue());
		
		if (user != null) {
			resp.addAttribute(user);
		}
		if (r != null) {
			resp.addAttribute(r);
		}
		// Let the nonce self calculate a value
		if (n != null)
			resp.addAttribute(n);
		
		if (t != null) {
			RequestedTransport rt = new RequestedTransport();
			if (rt != null)
				resp.addAttribute(rt);
		}
		if (includeBW) {
			// TODO Bandwidth b = new Bandwidth()
		}
		// if ttl is greater than 0, add Lifetime attribute
		if (ttl > 0) {
//			Lifetime l =  null;
			if (ttl == Allocation.TTL) {
			//	TODO l = new Lifetime()
			}
			else {
			// TODO	l = new Lifetime()
			}
//			if (l != null)
//				resp.addAttribute(l);
		}
		// TODO ReservationToken and RequestedProps attributes must be 
		// mutually exclusive
		if (includeResvToken && !includeProps) {
			
		}
		else if (!includeResvToken && includeProps) {
			
		}
	
		addMessageIntegrity(resp, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(resp);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "AllocateResponse message created.");
		return resp;
	}
	public StunMessage createAllocateErrorResponse(int statusCode, Transport t, boolean includeBW,
			int ttl, boolean includeProps, boolean includeResvToken,
			String username, String realm, 
			String password, Nonce nonce, boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating AllocateErrorResponse message.");
		StunMessage resp = new StunMessage(StunConstants.ALLOCATE_ERROR_RESPONSE_MSG_TYPE);
		ErrorCode ec = new ErrorCode(null);
		ec.setValue(statusCode);
		resp.addAttribute(ec);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				resp.addAttribute(user);
		}
		if (realm != null) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				resp.addAttribute(r);
		}
		// Let the nonce self calculate a value
		if (nonce != null)
			resp.addAttribute(nonce);
		else {
			Nonce n = new Nonce();
			if (n != null) {
				resp.addAttribute(n);
			}
		}
		if (t != null) {
			RequestedTransport rt = new RequestedTransport();
			if (rt != null)
				resp.addAttribute(rt);
		}
		if (includeBW) {
			// TODO Bandwidth b = new Bandwidth()
		}
		// if ttl is greater than 0, add Lifetime attribute
		if (ttl > 0) {
//			Lifetime l =  null;
			if (ttl == Allocation.TTL) {
			//	TODO l = new Lifetime()
			}
			else {
			// TODO	l = new Lifetime()
			}
//			if (l != null)
//				resp.addAttribute(l);
		}
		// TODO ReservationToken and RequestedProps attributes must be 
		// mutually exclusive
		if (includeResvToken && !includeProps) {
			
		}
		else if (!includeResvToken && includeProps) {
			
		}
	
		addMessageIntegrity(resp, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(resp);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "AllocateErrorResponse message created.");
		return resp;
	}
	public StunMessage createBindingRequest() {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating BindingRequest message.");
		StunMessage req = new StunMessage(StunConstants.BINDING_REQUEST_MSG_TYPE);

		logger.trace(PC2LogCategory.STUN, subCat, "BindingRequest message created.");
		return req;
	}
	
	public StunMessage createBindingRequest(String username, String realm, 
			String password, Nonce nonce, boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating BindingRequest message.");
		StunMessage req = new StunMessage(StunConstants.BINDING_REQUEST_MSG_TYPE);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				req.addAttribute(user);
		}
		if (realm != null) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				req.addAttribute(r);
		}
		
		if (password != null) {
			Password pwd = new Password(password.getBytes());
			if (pwd != null) {
				req.addAttribute(pwd);
			}
		}
		// Let the nonce self calculate a value
		if (nonce != null)
			req.addAttribute(nonce);
		else {
			Nonce n = new Nonce();
			if (n != null) {
				req.addAttribute(n);
			}
		}
		
		addMessageIntegrity(req, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(req);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "BindingRequest message created.");
		return req;
	}
	
	public StunMessage createBindingRequest(String username, String password,
		boolean includeFingerPrint, int priority, boolean iceLite, 
		boolean useCandidate, long iceControlling, byte [] transId) {
		
		logger.trace(PC2LogCategory.STUN, subCat, "Creating BindingRequest message.");
		
		StunMessage req = null;
		if (transId != null)
			req = new StunMessage(StunConstants.BINDING_REQUEST_MSG_TYPE, transId);
		else 
			req = new StunMessage(StunConstants.BINDING_REQUEST_MSG_TYPE);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				req.addAttribute(user);
		}
//		if (realm != null) {
//			Realm r = new Realm(realm.getBytes());
//			if (r != null)
//				req.addAttribute(r);
//		}
//		// Let the nonce self calculate a value
//		if (nonce != null)
//			req.addAttribute(nonce);
//		else {
//			Nonce n = new Nonce();
//			if (n != null) {
//				req.addAttribute(n);
//			}
//		}
		Long icVal = iceControlling;
		if (iceControlling ==-1)
				icVal = System.currentTimeMillis();
		IceControlling ic = new IceControlling(Conversion.longToByteArray(icVal));
		if (ic != null)
			req.addAttribute(ic);
		
		Priority p = new Priority(Conversion.intToByteArray(priority)); 
		if (p != null) {
			req.addAttribute(p);
		}
		
		if (useCandidate) {
			UseCandidate uc = new UseCandidate();
			if (uc != null)
				req.addAttribute(uc);
		}
		
		if (password != null) {
			Password pwd = new Password(password.getBytes());
			if (pwd != null) {
				req.addAttribute(pwd);
			}
		}
		
		addMessageIntegrity(req, password);
		if (includeFingerPrint) {
			addFingerPrint(req);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "BindingRequest message created.");
		return req;
	}
	
	public StunMessage createBindingResponse(StunEvent se) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating BindingResponse message.");
		StunMessage resp = new StunMessage(StunConstants.BINDING_RESPONSE_MSG_TYPE, 
				se.getEvent().getTransactionID());
		RawData rd = se.getRawData();
		XorMappedAddress xma = null;
		try {
			InetAddress ia = InetAddress.getByName(rd.getSrcIP());
	
		if (ia instanceof Inet4Address) {
		     xma = new XorMappedAddress(StunConstants.XOR_MAPPED_ADDRESS_TYPE,
				StunConstants.FAMILY_IPv4, rd.getSrcPort(), 
				rd.getSrcIP(), resp.getTransactionID());
		}
		else if (ia instanceof Inet6Address) {
		     xma = new XorMappedAddress(StunConstants.XOR_MAPPED_ADDRESS_TYPE,
					StunConstants.FAMILY_IPv6, rd.getSrcPort(), 
					rd.getSrcIP(), resp.getTransactionID());
			
		}
		if (xma != null) 
			resp.addAttribute(xma);
		}
		catch (UnknownHostException uhe) {
			
		}
		
		logger.trace(PC2LogCategory.STUN, subCat, "BindingResponse message created.");
		return resp;
	}
	
	public StunMessage createBindingResponse(StunEvent se, String username, 
			String realm, String password, Nonce n,
			boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating BindingResponse message.");
		StunMessage resp = new StunMessage(StunConstants.BINDING_RESPONSE_MSG_TYPE, 
				se.getEvent().getTransactionID());
		RawData rd = se.getRawData();
		XorMappedAddress xma = new XorMappedAddress(StunConstants.XOR_MAPPED_ADDRESS_TYPE,
				StunConstants.FAMILY_IPv4, rd.getSrcPort(), 
				rd.getSrcIP(), resp.getTransactionID());
		if (xma != null) 
			resp.addAttribute(xma);
		
		addMessageIntegrity(resp, username, realm, password);
		if (includeFingerPrint) {
			addFingerPrint(resp);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "BindingResponse message created.");
		return resp;
	}
	public StunMessage createBindingErrorResponse(StunEvent se, int statusCode, 
			String realm) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating BindingErrorResponse message.");
		StunMessage resp = new StunMessage(StunConstants.BINDING_ERROR_RESPONSE_MSG_TYPE, 
				se.getEvent().getTransactionID());
		ErrorCode ec = new ErrorCode(null);
		ec.setValue(statusCode);
		resp.addAttribute(ec);
		if (statusCode == 401) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				resp.addAttribute(r);

			Nonce n = new Nonce();
			if (n != null) {
				resp.addAttribute(n);
			}
		}
		logger.trace(PC2LogCategory.STUN, subCat, "BindingErrorResponse message created.");
		return resp;
	}
	
	public StunMessage createBindingErrorResponse(StunEvent se, int statusCode,
			String username, String realm, String password,
			boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating BindingErrorResponse message.");
		StunMessage resp = new StunMessage(StunConstants.BINDING_ERROR_RESPONSE_MSG_TYPE, 
				se.getEvent().getTransactionID());
		ErrorCode ec = new ErrorCode(null);
		ec.setValue(statusCode);
		resp.addAttribute(ec);
		if (statusCode == 401) {
			if (realm != null) {

				Realm r = new Realm(realm.getBytes());
				if (r != null)
					resp.addAttribute(r);
			}
			Nonce n = new Nonce();
			if (n != null) {
				resp.addAttribute(n);
			}
		}
		if (includeFingerPrint) {
			addFingerPrint(resp);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "BindingErrorResponse message created.");
		return resp;
	}
	public StunMessage createChannelBindRequest(String username, String realm, 
			String password, Nonce nonce, int channel, String peerAddress, 
			int peerPort, boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating ChannelBindRequest message.");
		StunMessage req = new StunMessage(StunConstants.CHANNEL_BIND_REQUEST_MSG_TYPE);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				req.addAttribute(user);
		}
		if (realm != null) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				req.addAttribute(r);
		}
		// Let the nonce self calculate a value
		if (nonce != null)
			req.addAttribute(nonce);
		else {
			Nonce n = new Nonce();
			if (n != null) {
				req.addAttribute(n);
			}
		}
		ChannelNumber cn = new ChannelNumber(channel);
		if (cn != null)
			req.addAttribute(cn);
		
		if (peerAddress != null) {
			PeerAddress pa = new PeerAddress(StunConstants.PEER_ADDRESS_TYPE,
					StunConstants.FAMILY_IPv4, peerPort, peerAddress, req.getTransactionID());
			if (pa != null)
				req.addAttribute(pa);
			
		}
		addMessageIntegrity(req, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(req);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "ChannelBindRequest message created.");
		return req;
	}
	
	public StunMessage createChannelBindErrorResponse(StunMessage req, int statusCode,
			String username, String realm, 
			String password, boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating ChannelBindError message.");
		StunMessage resp = new StunMessage(StunConstants.CHANNEL_BIND_ERROR_RESPONSE_MSG_TYPE, 
				req.getTransactionID());
		ErrorCode ec = new ErrorCode(null);
		ec.setValue(statusCode);
		resp.addAttribute(ec);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				resp.addAttribute(user);
		}
		if (realm != null) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				resp.addAttribute(r);
		}
		// Let the nonce self calculate a value
		Nonce nonce = (Nonce)req.getAttribute(StunConstants.NONCE_TYPE);
		if (nonce != null)
			resp.addAttribute(nonce);
		else {
			Nonce n = new Nonce();
			if (n != null) {
				resp.addAttribute(n);
			}
		}
		
		addMessageIntegrity(resp, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(resp);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "ChannelBindErrorResponse message created.");
		return resp;
	}
	public StunMessage createChannelBindResponse(StunMessage req, String username, String realm, 
			String password, int ttl, boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating ChannelBindResponse message.");
		StunMessage resp = new StunMessage(StunConstants.CHANNEL_BIND_RESPONSE_MSG_TYPE, 
				req.getTransactionID());
		Nonce n = (Nonce)req.getAttribute(StunConstants.NONCE_TYPE);
		PeerAddress pa = (PeerAddress)req.getAttribute(StunConstants.PEER_ADDRESS_TYPE);
		ChannelNumber cn = (ChannelNumber)req.getAttribute(StunConstants.CHANNEL_NUMBER_TYPE);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				resp.addAttribute(user);
		}
		if (realm != null) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				resp.addAttribute(r);
		}
		
		if (n != null) {
			resp.addAttribute(n);
		}
		
		if (ttl >= 0) {
			Lifetime l =  null;
			byte [] lifetime = Conversion.intToByteArray(ttl);
			l = new Lifetime(lifetime);
			
			if (l != null)
				resp.addAttribute(l);
		}
		if (pa != null) 
			resp.addAttribute(pa);
		
		if (cn != null)
			resp.addAttribute(cn);
		
		addMessageIntegrity(resp, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(resp);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "ChannelBindResponse message created.");
		return resp;
	}
	
	public StunMessage createDataIndication(InetAddress peerAddress, 
			int port, byte [] data, byte[] transID,
			boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating DataIndication message.");
		StunMessage ind = new StunMessage(StunConstants.DATA_INDICATION_MSG_TYPE);
		PeerAddress pa = null;
		if (peerAddress != null) {
			if (peerAddress instanceof Inet4Address) {
				pa = new PeerAddress(StunConstants.PEER_ADDRESS_TYPE, 
						StunConstants.FAMILY_IPv4, port, peerAddress.getAddress(),
						transID);
			}
			else if (peerAddress instanceof Inet6Address) {
				pa = new PeerAddress(StunConstants.PEER_ADDRESS_TYPE, 
						StunConstants.FAMILY_IPv6, port, peerAddress.getAddress(),
						transID);
			}
		}
		if (pa != null)
			ind.addAttribute(pa);
		if (data != null) {
			Data d = new Data(data);

			if (d != null)
				ind.addAttribute(d);
		}
		if (includeFingerPrint) {
			addFingerPrint(ind);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "DataIndication message created.");
		return ind;
	}
	

	public StunMessage createRefreshRequest(String username, String realm, 
			String password, Nonce nonce, int ttl,
			boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating RefreshRequest message.");
		StunMessage req = new StunMessage(StunConstants.REFRESH_REQUEST_MSG_TYPE);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				req.addAttribute(user);
		}
		if (realm != null) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				req.addAttribute(r);
		}
		// Let the nonce self calculate a value
		if (nonce != null)
			req.addAttribute(nonce);
		else {
			Nonce n = new Nonce();
			if (n != null) {
				req.addAttribute(n);
			}
		}
		if (ttl >= 0) {
			Lifetime l =  null;
			byte [] lifetime = Conversion.intToByteArray(ttl);
			l = new Lifetime(lifetime);
			
			if (l != null)
				req.addAttribute(l);
		}
		addMessageIntegrity(req, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(req);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "RefreshRequest message created.");
		return req;
	}
	
	public StunMessage createRefreshErrorResponse(StunEvent se, int statusCode,
			String username, String realm, 
			String password, Nonce nonce, int ttl,
			boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating RefreshErrorResponse message.");
		StunMessage resp = new StunMessage(StunConstants.REFRESH_ERROR_RESPONSE_MSG_TYPE, 
				se.getEvent().getTransactionID());
		ErrorCode ec = new ErrorCode(null);
		ec.setValue(statusCode);
		resp.addAttribute(ec);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				resp.addAttribute(user);
		}
		if (realm != null) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				resp.addAttribute(r);
		}
		// Let the nonce self calculate a value
		if (nonce != null)
			resp.addAttribute(nonce);
		else {
			Nonce n = new Nonce();
			if (n != null) {
				resp.addAttribute(n);
			}
		}
		if (ttl >= 0) {
			Lifetime l =  null;
			byte [] lifetime = Conversion.intToByteArray(ttl);
			l = new Lifetime(lifetime);
			
			if (l != null)
				resp.addAttribute(l);
		}
		addMessageIntegrity(resp, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(resp);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "RefreshErrorResponse message created.");
		return resp;
	}
	public StunMessage createRefreshResponse(String username, String realm, 
			String password, Nonce nonce, int ttl,
			boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating RefreshResponse message.");
		StunMessage resp = new StunMessage(StunConstants.REFRESH_RESPONSE_MSG_TYPE);
		if (username != null) {
			Username user = new Username(username.getBytes());
			if (user != null)
				resp.addAttribute(user);
		}
		if (realm != null) {
			Realm r = new Realm(realm.getBytes());
			if (r != null)
				resp.addAttribute(r);
		}
		// Let the nonce self calculate a value
		if (nonce != null)
			resp.addAttribute(nonce);
		else {
			Nonce n = new Nonce();
			if (n != null) {
				resp.addAttribute(n);
			}
		}
		if (ttl >= 0) {
			Lifetime l =  null;
			byte [] lifetime = Conversion.intToByteArray(ttl);
			l = new Lifetime(lifetime);
			
			if (l != null)
				resp.addAttribute(l);
		}
		addMessageIntegrity(resp, username, realm, password);
		
		if (includeFingerPrint) {
			addFingerPrint(resp);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "RefreshResponse message created.");
		return resp;
	}
	public StunMessage createSendIndication(PeerAddress pa, Data data,
			boolean includeFingerPrint) {
		logger.trace(PC2LogCategory.STUN, subCat, "Creating SendIndication message.");
		StunMessage ind = new StunMessage(StunConstants.SEND_INDICATION_MSG_TYPE);
		if (pa != null)
			ind.addAttribute(pa);
		if (data != null) {
			ind.addAttribute(data);
		}
		if (includeFingerPrint) {
			addFingerPrint(ind);
		}
		logger.trace(PC2LogCategory.STUN, subCat, "SendIndication message created.");
		return ind;
	}
	
	private void addMessageIntegrity(StunMessage msg, String password) {
		MessageIntegrity mi = new MessageIntegrity(StunConstants.STUN_EMPTY_MESSAGE_INTEGRITY);
		if (mi != null) {
			mi.calculate(msg, password);
			msg.addAttribute(mi);
		}
	}
	
	private void addMessageIntegrity(StunMessage msg, String username, String realm, String password) {
		MessageIntegrity mi = new MessageIntegrity(StunConstants.STUN_EMPTY_MESSAGE_INTEGRITY);
		if (mi != null) {
			mi.calculate(msg, username, realm, password);
			msg.addAttribute(mi);
		}
	}
	private void addFingerPrint(StunMessage msg) {
		FingerPrint fp = new FingerPrint(StunConstants.STUN_EMPTY_FINGER_PRINT);
		if (fp != null) {
			fp.calculate(msg);
			msg.addAttribute(fp);
		}
	}
}
