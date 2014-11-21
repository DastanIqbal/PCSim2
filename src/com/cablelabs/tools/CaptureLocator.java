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
import java.util.ListIterator;
import java.util.StringTokenizer;

import com.cablelabs.fsm.CaptureRef;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

public class CaptureLocator {

	/**
	 * Private logger for the class
	 */
	private LogAPI logger = LogAPI.getInstance(); 
	private static CaptureLocator locator = null;

	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "Locator";

	/**
	 * Private Constructor
	 *
	 */
	private CaptureLocator() {

	}

	/**
	 * Retrieves the single instance of the CapturePLocator if it 
	 * already exists. If it doesn't exist it will create it prior
	 * to returning it.
	 *
	 */
	public synchronized static CaptureLocator getInstance() {
		if (locator == null) {
			locator = new CaptureLocator();
		}
		return locator;
	}

	public LinkedList<String> getReferenceInfo(PacketDatabase db, CaptureRef cr) {
		LinkedList<String> results = new LinkedList<String>();
		if (db != null) {
			String protocol = cr.getType();

			switch (protocol.charAt(0)) {
			case 'b':
				if (protocol.equals(PDMLTags.BOOTP_PROTOCOL)) {
					LinkedList<Packet> packets = db.getBootpPackets(cr); 
					resolve(results, packets, cr);
				}
				break;
			case 'd':
				if (protocol.equals(PDMLTags.DHCPv6_PROTOCOL)) {
					LinkedList<Packet> packets = db.getDHCPv6Packets(cr); 
					resolve(results, packets, cr);
				}
				else if (protocol.equals(PDMLTags.DNS_PROTOCOL)){
					LinkedList<Packet> packets = db.getDnsPackets(cr); 
					resolve(results, packets, cr);
				}
				break;


			case 'e':
				if (protocol.equals(PDMLTags.ETH_PROTOCOL)) {

				}

				break;
			case 'f':
				if (protocol.equals(PDMLTags.FRAME_PROTOCOL)) {

				}

				break;
			case 'g':
				if (protocol.equals(PDMLTags.GENINFO_PROTOCOL)) {

				}

				break;
			case 'i':
				if (protocol.equals(PDMLTags.ICMPV6_PROTOCOL)) {
					LinkedList<Packet> packets = db.getICMPv6Packets(cr); 
					resolve(results, packets, cr);
				}
				else if (protocol.equals(PDMLTags.IP_PROTOCOL)) {

				}

				break;
			case 'k':
				if (protocol.equals(PDMLTags.KERBEROS_PROTOCOL)) {
					LinkedList<Packet> packets = db.getKerberosPackets(cr); 
					resolve(results, packets, cr);
				}

				break;
			case 'p':
				if (protocol.equals(PDMLTags.PACKET_CABLE_PROTOCOL)) {
					LinkedList<Packet> packets = db.getPktcPackets(cr); 
					resolve(results, packets, cr);
				}
				break;
			case 'r':
				if (protocol.equals(PDMLTags.RTCP_PROTOCOL)) {

				}
				else if (protocol.equals(PDMLTags.RTP_PROTOCOL)) {

				}
				break;
			case 's':
				if (protocol.equals(PDMLTags.SNMP_PROTOCOL)) {
					LinkedList<Packet> packets = db.getSnmpPackets(cr); 
					resolve(results, packets, cr);
				}
				else if (protocol.equals(PDMLTags.SYSLOG_PROTOCOL)) {
					LinkedList<Packet> packets = db.getSyslogPackets(cr); 
					
					resolve(results, packets, cr);
				}

				break;
			case 't':
				if (protocol.equals(PDMLTags.TCP_PROTOCOL)) {

				}
				else if (protocol.equals(PDMLTags.TFTP_PROTOCOL)) {
					LinkedList<Packet> packets = db.getTftpPackets(cr); 
					resolve(results, packets, cr);
				}
				else if (protocol.equals(PDMLTags.TOD_PROTOCOL)) {
					LinkedList<Packet> packets = db.getTodPackets(cr); 
					resolve(results, packets, cr);
				}
				break;
			case 'u':
				if (protocol.equals(PDMLTags.UDP_PROTOCOL)) {

				}

				break;

			}
		}
		return results;
	}

//	private Protocol getProtocol(Packet p, CaptureRef cr, String token) {
//	String protocol = getProtocolKey(cr.getType());
//	if (protocol != null && protocol.equals(token)) {
//	return p.getProtocol(protocol);
//	}
//	else if (!PDMLTags.supportedProtocol(token))
//	return p.getProtocol(protocol);
//	else {
//	return p.getProtocol(token);
//	}
//	}

	private String getProtocolKey(String protocol) {
		if (protocol != null) {
			if (PDMLTags.isBootpMsgType(protocol))
				return PDMLTags.BOOTP_MSG_TYPE;
			else 
				return protocol;
		}
		return null;
	}

	private LinkedList<String> getFields(String prevToken, StringTokenizer tokens) {
		// If the prevToken is not null, it should be the first entry in the list
		LinkedList<String> fields = new LinkedList<String>();

		if (prevToken != null) 
			fields.add(prevToken);

		while (tokens.hasMoreTokens()) {
			fields.add(tokens.nextToken());
		}
		return fields;
	}

	private void resolve(LinkedList<String> results, LinkedList<Packet> packets, CaptureRef cr) {
		ListIterator<Packet> iter = packets.listIterator();
		if (packets.size() == 0)
			results.add(null);
		else {
			while (iter.hasNext()) {

				Packet packet = iter.next();

				if (cr.getField() != null) {
					StringTokenizer tokens = new StringTokenizer(cr.getField(), ".");
					if (tokens != null) {
						// The next token should be the protocol, unless it has
						// been truncated because it is the type of message.
						String token = tokens.nextToken();
						Protocol protocol = null;
						String protocolKey = getProtocolKey(cr.getType());
						if (PDMLTags.supportedProtocol(token)) { 
							protocol = packet.getProtocol(token);
							token = null;

						}
						else {
							protocol = packet.getProtocol(protocolKey);
							// See if the protocol retrieved is a tunneling protocol
							if (protocol.isTunnelingProtocol()) {
								// If the token is the tunneling protocol msg type, then drop it to move on 
								// to the fields portion of the notation
								if (PDMLTags.isTunnelingMsgType(token)) {
									token = tokens.nextToken();
								}
								// Now we need to see if we are obtain information for the tunneled message or the wrapping message
								if (!cr.getMsgType().equals(protocol.getTunnelName())) {
									protocol = protocol.getSubProtocol(protocolKey);
								}
							}
							
						}
						//	getProtocol(packet, cr, token);
						if (protocol != null) {
							LinkedList<String> fields = getFields(token, tokens);
							ListIterator<String> iter2 = fields.listIterator();
							String fieldName = null;
							Field f = null;
							if (iter2.hasNext()) {
								fieldName = iter2.next();
								f = protocol.getField(fieldName);

								while (iter2.hasNext() && f != null) {
									String sf = iter2.next();
									Field next = f.getSubField(sf);
									f = next;
								}
								if (f != null) {
									String data = f.getAttribute(cr.getAttributeType());
									if (data != null) {
										results.add(data);
										logger.info(PC2LogCategory.PCSim2, subCat, 
												"Returning data=" + data + " for capture reference.");
									}
									else {
										logger.info(PC2LogCategory.PCSim2, subCat, 
												"Couldn't find capture reference[" + cr.toString() + "]");
										results.add(null);
									}

								}
								else {
									logger.info(PC2LogCategory.PCSim2, subCat, 
											"Couldn't find capture reference[" + cr.toString() + "]");
									results.add(null);
								}
							}
							else {
								String data = protocol.getAttribute(cr.getAttributeType());
								if (data != null) {
									results.add(data);
									logger.info(PC2LogCategory.PCSim2, subCat, 
											"Returning data=" + data + " for capture reference.");
								}
								else {
									logger.info(PC2LogCategory.PCSim2, subCat, 
											"Couldn't find capture reference[" + cr.toString() + "]");
									results.add(null);
								}
							}
						}
					}
				}
				else {
					String data = packet.getAttribute(cr.getAttributeType());
					if (data != null) {
						results.add(data);
						logger.info(PC2LogCategory.PCSim2, subCat, 
								"Returning data=" + data + " for capture reference.");
					}
					else {
						logger.info(PC2LogCategory.PCSim2, subCat, 
								"Couldn't find capture reference[" + cr.toString() + "]");
						results.add(null);
					}
				}
			}

		}
	}
}
