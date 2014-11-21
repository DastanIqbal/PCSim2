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

import java.util.Iterator;
import java.util.Properties;
import java.util.TooManyListenersException;

import javax.sip.InvalidArgumentException;
import javax.sip.ListeningPoint;
import javax.sip.ObjectInUseException;
import javax.sip.PeerUnavailableException;
import javax.sip.SipFactory;
import javax.sip.SipListener;
import javax.sip.SipProvider;
import javax.sip.SipStack;
import javax.sip.TransportNotSupportedException;

import com.cablelabs.common.Transport;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

public class PC2SipStack { // extends gov.nist.javax.sip.SipStackImpl {

	private LogAPI logger = LogAPI.getInstance(); // Logger.getLogger(SIPDistributor.class);

	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = null;
	
	/**
	 * The SIP Stack
	 */
	protected SipStack sipStack = null;

	protected SipListener listener = null;
	
	private ListeningPoint udpListeningPoint = null;
	
	private ListeningPoint tcpListeningPoint = null;
	
	private ListeningPoint tlsListeningPoint = null;
	
	/**
	 * The IP address that this SIP stack is using
	 */
	private String myIP;

	protected PC2SipStack(SipFactory factory, SipListener listener, Properties sip) 
		throws InvalidArgumentException, ObjectInUseException, 
			TooManyListenersException, TransportNotSupportedException {
		sipStack = null;
		if (sip != null) {
			String name = sip.getProperty("javax.sip.STACK_NAME");
			this.subCat = name;
			myIP = sip.getProperty("javax.sip.IP_ADDRESS");
			if (name != null && myIP != null) {
				int udpPort = -1;
				int tcpPort = -1;
				int tlsPort = -1;
				try {
					udpPort = Integer.parseInt(sip.getProperty(SettingConstants.UDP_PORT));
				}
				catch (Exception e) {
					logger.warn(PC2LogCategory.SIP, subCat,
							"SIP Stack(" + name + ") is not starting its' UDP Provider because " 
							+ "SIP UDP Port settings was not found or is not an integer.");
				}
				try {
					tcpPort = Integer.parseInt(sip.getProperty(SettingConstants.TCP_PORT));
				}
				catch (Exception e) {
					logger.warn(PC2LogCategory.SIP, subCat,
							"SIP Stack(" + name + ") is not starting the TCP Provider because SIP "
							+ "TCP Port settings was not found or is not an integer.");
				}
				try {
					tlsPort = Integer.parseInt(sip.getProperty(SettingConstants.TLS_PORT));
				}
				catch (Exception e) {
					logger.warn(PC2LogCategory.SIP, subCat,
							"SIP Stack(" + name + ") is not starting the TLS Provider because SIP "
							+ "TLS Port settings was not found or is not an integer.");
				}
				if (udpPort > 0 || tcpPort > 0 || tlsPort > 0) {
					try {
						// Create SipStack object
						sipStack = factory.createSipStack(sip);
						logger.debug(PC2LogCategory.SIP, subCat,
								"createSipStack " + sipStack);
						//return sipStack;
					} catch (PeerUnavailableException e) {
						// could not find
						// gov.nist.jain.protocol.ip.sip.SipStackImpl
						// in the classpath
						//e.printStackTrace();
						logger.error(PC2LogCategory.SIP, subCat,
								e.getMessage(),e);
						//System.exit(0);
					}
					if (udpPort > 0) {
						udpListeningPoint = sipStack.createListeningPoint(udpPort, "udp");
						SipProvider udpProvider = sipStack.createSipProvider(udpListeningPoint);
						udpProvider.addSipListener(listener);
						logger.debug(PC2LogCategory.SIP, subCat,
								"udp provider " + udpProvider);
						// As a precaution against case put each provider into the table
						// in lower case and upper case
//						providerTable.put("udp", udpProvider);
//						providerTable.put("UDP", udpProvider);
					}
					else
						logger.warn(PC2LogCategory.SIP, subCat,
								"SIPDistributor is not starting the UDP Provider because SIP "
								+ "UDP Port settings was not found or is set to -1.");

					if (tcpPort > 0) {
						tcpListeningPoint = sipStack.createListeningPoint(tcpPort, "tcp");
						SipProvider tcpProvider = sipStack.createSipProvider(tcpListeningPoint);
						logger.debug(PC2LogCategory.SIP, subCat,
								"tcp provider " + tcpProvider);
						tcpProvider.addSipListener(listener);
//						providerTable.put("tcp", tcpProvider);
//						providerTable.put("TCP", tcpProvider);
					}
					else
						logger.warn(PC2LogCategory.SIP, subCat,
								"SIPDistributor is not starting the TCP Provider because SIP "
								+ "TCP Port settings was not found or is set to -1.");

					if (tlsPort > 0) {
						tlsListeningPoint = sipStack.createListeningPoint(tlsPort, "tls");
						SipProvider tlsProvider = sipStack.createSipProvider(tlsListeningPoint);
						logger.debug(PC2LogCategory.SIP, subCat,
								"tls provider " + tlsProvider);
						tlsProvider.addSipListener(listener);
//						providerTable.put("tls", tlsProvider);
//						providerTable.put("TLS", tlsProvider);
					}
					else
						logger.warn(PC2LogCategory.SIP, subCat,
								"SIPDistributor is not starting the TLS Provider because SIP "
								+ "TLS Port settings was not found or is set to -1.");

				}
			}
			else {
				if (name == null)
					logger.warn(PC2LogCategory.SIP, subCat,
							"The SIP Stack is not being started because the "
							+ "SIP Stack <x> Name property is not configured.");
				else if (myIP == null)
					logger.warn(PC2LogCategory.SIP, subCat,
							"The SIP Stack is not being started because the "
							+ "SIP Stack <x> IP Address property is not configured.");
			}
		}
		else if (sip == null) {
			logger.warn(PC2LogCategory.SIP, subCat,
					"The SIP Stack is not being started since the "
					+ "SIP Stack <x> Name property information could not be found");
		}
	}
	
	protected SipProvider getProvider(int port, String transport) {
		Iterator<SipProvider> iter = sipStack.getSipProviders();
		while (iter.hasNext()) {
			SipProvider provider = iter.next();
			if (provider.getListeningPoint().getPort() == port &&
					provider.getListeningPoint().getTransport().equals(transport.toLowerCase())) {
				return provider;
			}
		}
		return null;
	}
	
	protected SipProvider getProvider(String transport) {
		Iterator<SipProvider> iter = sipStack.getSipProviders();
		while (iter.hasNext()) {
			SipProvider provider = iter.next();
//			String tmp = provider.getListeningPoint().getTransport();
			if (provider.getListeningPoint().getTransport().equals(transport.toLowerCase())) {
				return provider;
			}
		}
		return null;
	}
	
	protected SipProvider getProvider(Transport transport, String defTransport) {
		if (transport != null) 
			return getProvider(transport.toString());
		else if (defTransport != null)
			return getProvider(defTransport);
		
		return null;
	}
	protected String getIP() {
		return myIP;
	}
	
	protected String getLocalAddress(String transport) {
		return getIP();
	}
	
	protected int getLocalPort(String transport) {
		if (transport.equals(SettingConstants.UDP) &&
				udpListeningPoint != null)
			return udpListeningPoint.getPort();
		else if (transport.equals(SettingConstants.TCP) &&
				tcpListeningPoint != null)
			return tcpListeningPoint.getPort();
		else if (transport.equals(SettingConstants.TLS) &&
				tlsListeningPoint != null)
			return tlsListeningPoint.getPort();
		
		return -1;
	}
	public String getName() {
		return sipStack.getStackName();
	}
	protected void shutdown() {
		try {
			if (udpListeningPoint != null)
				sipStack.deleteListeningPoint(udpListeningPoint);
			if (tcpListeningPoint != null)
				sipStack.deleteListeningPoint(tcpListeningPoint);
		    if (tlsListeningPoint != null)
				sipStack.deleteListeningPoint(tlsListeningPoint);
			
			Iterator<SipProvider> iter = sipStack.getSipProviders();
			while (iter.hasNext()) {
				SipProvider sp = iter.next();
				sp.removeSipListener(listener);
				sipStack.deleteSipProvider(sp);
				iter = sipStack.getSipProviders();
			}
		}
		catch (Exception e) {
			logger.error(PC2LogCategory.SIP, subCat,
					"PC2SipStack(" + sipStack.getStackName() + 
					" encountered error while trying to shutdown.", e);
		}
	}
}
