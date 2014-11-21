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

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;

import com.cablelabs.fsm.CaptureAttributeType;

//import com.cablelabs.log.*;

public class Packet {
/**
 * This class contains all of the information for a single packet in the capture file.
 * It contains the information that describes the packet listed in the geninfo proto
 * tag rather than having a separate Protocol class for the information. Below is an
 * example of the proto tag containing the geninfo portion. 
 * 
 * 	<proto name="geninfo" pos="0" showname="General information" size="139">
 * 		<field name="num" pos="0" show="3" showname="Number" value="3" size="139"/>
 * 		<field name="len" pos="0" show="139" showname="Packet Length" value="8b" size="139"/>
 * 		<field name="caplen" pos="0" show="139" showname="Captured Length" value="8b" size="139"/>
 * 		<field name="timestamp" pos="0" show="Oct  1, 2008 17:14:15.625383000" showname="Captured Time" value="1222902855.625383000" size="139"/>
 * 	</proto>
 * 
 * The protocol of a Packet is derived from the highest layer protocol in the packet. As an
 * example there are several protocol layers for a DHCP Discover packet,
 * frame, eth, ip, udp, then bootp  
 */
	
	protected String protocol = null;
	protected String name = null;
	protected String frame = null;
	protected String length = null;
	protected String caplen = null;
	protected String timestamp = null;
	protected String srcAddr = null;
	protected String dstAddr = null;
	protected String srcPort = null;
	protected String dstPort = null;
	protected String clientMacAddr = null;
	
	protected Protocol genInfo = null;
	protected Protocol frameProtocol = null;
	protected LinkedList<Protocol> protocols = null;
	protected Hashtable<String, Protocol> protocolIndex = null;

//	private LogAPI logger = null;
//	private String subCat = "PDML";
	
	public Packet(String protocol, String name, String frame, String length, String caplen, String timestamp, Protocol genInfo) {
		this.protocol = protocol;
		this.name = name;
		this.frame = frame;
		this.length = length;
		this.caplen = caplen;
		this.timestamp = timestamp;
		this.genInfo = genInfo;
		protocolIndex = new Hashtable<String, Protocol>();
		protocols = new LinkedList<Protocol>();
		
	}
	
	public Packet() {
		protocolIndex = new Hashtable<String, Protocol>();
		protocols = new LinkedList<Protocol>();
//		logger = LogAPI.getInstance();
	}
	
	public boolean addProtocol(Protocol p) {
		if (p != null && protocols != null) {
			protocolIndex.put(p.name, p);
			protocols.add(p);
			// Get the filter criteria information from the protocol as they are added to the 
			// Packet
			if (p.protocol.equals(PDMLTags.IP_PROTOCOL)) {
				Field f = p.getField("src");
				if (f != null) {
					srcAddr = f.getShow();
				}
				f = p.getField("dst");
				if (f != null) {
					dstAddr = f.getShow();
				}
			}
			else if (p.protocol.equals(PDMLTags.IPV6_PROTOCOL)) {
				Field f = p.getField("src");
				if (f != null) {
					srcAddr = f.getShow();
				}
				f = p.getField("dst");
				if (f != null) {
					dstAddr = f.getShow();
				}
			}
			else if (p.protocol.equals(PDMLTags.UDP_PROTOCOL) ||
					p.protocol.equals(PDMLTags.TCP_PROTOCOL)) {
				Field f = p.getField("srcport");
				if (f != null) {
					srcPort = f.getShow();
				}
				f = p.getField("dstport");
				if (f != null) {
					dstPort = f.getShow();
				}
			}
			else if (p.protocol.equals(PDMLTags.BOOTP_PROTOCOL)) {
				Field f = p.getField("mac_addr");
				if (f != null)
					clientMacAddr = f.getValue();
			}
			else if (p.protocol.equals(PDMLTags.DHCPv6_PROTOCOL)) {
				// First we need to see if the message is a relay message or a different message
				Protocol tmp = p;
				Field type = p.getField("msgtype");
				if (type != null && (type.getShow().equals(PDMLTags.RELAY_MSG_VALUE) ||
						type.getShow().equals(PDMLTags.RELAY_REPLY_MSG_VALUE))) {
					Protocol sp = p.getSubProtocol(PDMLTags.DHCPv6_PROTOCOL) ;
					if (sp != null) {
						tmp = sp;
					}
				}
				
				if (tmp != null) {
					Field f = tmp.getField("Option1");
					if (f != null) {
						Field sf = f.getSubField("Link-layer address");
						if (sf != null)
							clientMacAddr = sf.getValue();
						else {
							sf = f.getSubField("link_layer_addr");
							if (sf != null)
								clientMacAddr = sf.getValue();
						}
					}
				}
				
			}
				
			this.protocol = p.protocol;
			if (protocol.equals(PDMLTags.BOOTP_PROTOCOL) ||
					protocol.equals(PDMLTags.DHCPv6_PROTOCOL) ||
					protocol.equals(PDMLTags.DNS_PROTOCOL) ||
					protocol.equals(PDMLTags.KERBEROS_PROTOCOL) ||
					protocol.equals(PDMLTags.PACKET_CABLE_PROTOCOL) ||
					protocol.equals(PDMLTags.SNMP_PROTOCOL)||
					protocol.equals(PDMLTags.TFTP_PROTOCOL)||
					protocol.equals(PDMLTags.TOD_PROTOCOL) || 
					protocol.equals(PDMLTags.ICMPV6_PROTOCOL)) {
			}
			else {
				this.name = p.name;
			}
			return true;
		}
		return false;
	}
	
	public String getAttribute(CaptureAttributeType cat) {
		if (cat == CaptureAttributeType.DEFAULT) {
			if (name != null)
				return this.name;
		}
		else if (cat == CaptureAttributeType.CAP_LEN)
			return this.caplen;
		else if (cat == CaptureAttributeType.NUM)
			return this.frame;
		else if (cat == CaptureAttributeType.TIMESTAMP)
			return this.timestamp;
		else if (cat == CaptureAttributeType.SHOWNAME)
			return this.name;
		else if (cat == CaptureAttributeType.SHOW)
			return this.name;
		else if (cat == CaptureAttributeType.SIZE)
			return this.frameProtocol.size;
	
		return null;
	}
	
	public Protocol getProtocol(String name) {
		if (name != null && protocolIndex != null) {
			Protocol p = protocolIndex.get(name);
			if (p != null)
				return p;
			else {
				ListIterator<Protocol> iter = protocols.listIterator();
				while (iter.hasNext()) {
					p = iter.next();
					if (p.hasSubProtocol()) {
						ListIterator<Protocol> iter2 = p.getSubProtocols();
						while (iter2.hasNext()) {
							Protocol sp = iter2.next();
							if (sp.name.equals(name))
								return sp;
						}
					}
				}
			}
			
		}
		return null;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFrame() {
		return frame;
	}

	public void setFrame(String frame) {
		this.frame = frame;
	}

	public String getLength() {
		return length;
	}

	public void setLength(String length) {
		this.length = length;
	}

	public String getCapLen() {
		return caplen;
	}

	public void setCapLen(String caplen) {
		this.caplen = caplen;
	}

	public String getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public Protocol getGenInfo() {
		return genInfo;
	}

	public void setGenInfo(Protocol genInfo) {
		this.genInfo = genInfo;
		protocolIndex.put(PDMLTags.GENINFO_PROTOCOL, genInfo);
	}
	
	public Protocol getFrameProtocol() {
		return frameProtocol;
	}

	public void setFrameProtocol(Protocol fp) {
		this.frameProtocol = fp;
		protocolIndex.put(PDMLTags.FRAME_PROTOCOL, fp);
	}
	
	public String abbreviatedString() {
		String result = "packet[" + protocol + "|" + name + "|" + length + "] - ";
		if (protocols != null) {
			ListIterator<Protocol> iter = protocols.listIterator();
			int count = 0;
			while (iter.hasNext()) {
				Protocol p = iter.next();
				if (count > 0) {
					result += ":" + p.protocol;
				}
				else {
					result += p.protocol;
				}
				count++;
				
			}
		}
		return result;
	}
	
	public String detailString() {
		String result = "packet (" + frame + ") [" + protocol + "|" + name + "|" + length + "] - ";
		String fs = null;
		if (protocols != null) {
			ListIterator<Protocol> iter = protocols.listIterator();
			int count = 0;
			String indent = "    ";
			while (iter.hasNext()) {
				Protocol p = iter.next();
				if (count > 0) {
					result += ":" + p.protocol;
				}
				else {
					result += p.protocol;
				}
				if (fs == null) {
					fs = "\nprotocol(" + p.protocol + ") has fields:\n";
				}
				else
					fs += "protocol(" + p.protocol + ") has fields:\n";
				boolean includeProtocol = true;
				if (protocol.equals(p.protocol))
					includeProtocol = false;
				
				fs += detailProtocolString(p, name, indent, includeProtocol);
			    /* botte, 08/21/2012, fixes printing dhcpv6, works OK on bootp, does it break printing other protocols?
				ListIterator<Protocol> iter2 = p.getSubProtocols();
			    if (iter2 != null) {
			    	while (iter2.hasNext()) {
			    		Protocol sp = iter2.next();
			    		fs += detailProtocolString(sp, name, indent + indent, includeProtocol);
			    	}
			    }
			    */
				count++;
			}
		}
		return result + fs;
	}
	
//	public String detailProtocolString(Protocol p, String name, String indent, boolean includeProtocolName) {
//		String result = "";
//		String msgType = name;
//
//		ListIterator<Field> iter = p.fields.listIterator();
//		while (iter.hasNext()) {
//			Field f = iter.next();
//			if (p.isTunnelingProtocol()) {
//				msgType = name + "." + p.getTunnelName();
//			}
//			if (includeProtocolName)
//				result += indent + msgType + f.abbreviatedString() + "\n";
//			else
//				result += indent + msgType + "." + p.protocol + f.abbreviatedString() + "\n";
//
//
//			if (f.hasSubFields()) {
//				ListIterator<Field> iter2 = f.subFields.listIterator();
//				while (iter2.hasNext()) {
//					Field sf = iter2.next();
//					if (sf.hasSubFields()) {
//						int glh = 0;
//					}
//					if (includeProtocolName)
//						result += indent + "    " + msgType  + sf.abbreviatedString() + "\n";
//					else
//						result += indent + "    " + msgType + "." + p.protocol + sf.abbreviatedString() + "\n";
//				}
//			}
//		}
//
//		return result;
//	}
	
	public String detailProtocolString(Protocol p, String name, String indent, boolean includeProtocolName) {
		String result = "";
		String msgType = name;

		ListIterator<Field> iter = p.fields.listIterator();
		while (iter.hasNext()) {
			Field f = iter.next();
			//if (p.isTunnelingProtocol()) {
			//	msgType = name + "." + p.getTunnelName();
			//}
			String label = null;
			if (includeProtocolName)
				label = indent + msgType + "." + p.protocol + "." + f.getName();
			else
				label = indent + msgType + "." + f.getName() ;

			result += label + "\n";

			if (f.hasSubFields()) {
				result += detailSubFields(f.subFields, label, indent + "    ");
			};
		}
		//}
		

		return result;
	}

	private String detailSubFields(LinkedList<Field> fields, String prefix, String indent) {
		String result = "";
		ListIterator<Field> iter = fields.listIterator();
		
		while (iter.hasNext()) {
			Field f = iter.next();
			String label = null;		
			label = prefix  + "." + f.getName();
			result += label + "\n";
			if (f.hasSubFields()) {
				result += detailSubFields(f.subFields, label, indent + "    ");
			}
		}
		return result;
	}
	

	
	@Override
	public String toString() {
		String result = "\t<packet>" + "\n\t<!-- ";
		if (name != null && name.equals(protocol))
			result += " name=\"" + name + "\"";
		else
			result += " name=\"" + protocol + "." + name + "\"";
		
		result += " frame=\"" + frame + "\"" + " length=\"" + length + "\""
			+ " caplen=\"" + caplen + "\"" + " timestamp=\"" + timestamp 
			+ "\" -->\n"
			+ genInfo.toString() + "\n" + frameProtocol.toString();
		
		if (protocols != null) {
			ListIterator<Protocol> iter = protocols.listIterator();
			while (iter.hasNext()) {
				result += "\n" + iter.next();
			}
			result += "\n\t\t</proto>\n</packet>";
		}
		else
			result += "/>";
		return result;
	}

	public String getSrcAddr() {
		return srcAddr;
	}

	public void setSrcAddr(String srcAddr) {
		this.srcAddr = srcAddr;
	}

	public String getDstAddr() {
		return dstAddr;
	}

	public void setDstAddr(String dstAddr) {
		this.dstAddr = dstAddr;
	}

	public String getSrcPort() {
		return srcPort;
	}

	public void setSrcPort(String srcPort) {
		this.srcPort = srcPort;
	}

	public String getDstPort() {
		return dstPort;
	}

	public void setDstPort(String dstPort) {
		this.dstPort = dstPort;
	}

	public String getClientMacAddr() {
		return clientMacAddr;
	}

	public void setClientMacAddr(String clientMacAddr) {
		this.clientMacAddr = clientMacAddr;
	}
}
