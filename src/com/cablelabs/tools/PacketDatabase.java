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

import com.cablelabs.fsm.CaptureRef;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.ParserFilter;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

public class PacketDatabase {

	private String name = null;
	private String tool = null;
	protected ParserFilter filter = null;
	protected LinkedList<Packet> table = new LinkedList<Packet>();
	protected Hashtable<String, LinkedList<Packet>> protocolIndex = null;

	/**
	 * This table is an indexing of DHCP v4 messages in the table attribute.
	 */
	protected LinkedList<Packet> bootpTable = null;
	
	/**
	 * This table is an indexing of DHCP v6 messages in the table attribute.
	 */
	protected LinkedList<Packet> dhcpv6Table = null;
	
	/**
	 * This table is an indexing of ICMP v6 messages in the table attribute.
	 */
	protected LinkedList<Packet> icmpv6Table = null;
	
	/**
	 * This table is an indexing of TFTP messages in the table attribute.
	 */
	protected LinkedList<Packet> tftpTable = null;
	
	/**
	 * This table is an indexing of DNS messages in the table attribute.
	 */
	protected LinkedList<Packet> dnsTable = null;
	
	/**
	 * This table is an indexing of Kerberos messages in the table attribute.
	 */
	protected LinkedList<Packet> kerberosTable = null;
	
	/**
	 * This table is an indexing of PacketCable messages in the table attribute.
	 */
	protected LinkedList<Packet> pcTable = null;
	
	/**
	 * This table is an indexing of RTCP messages in the table attribute.
	 */
	protected LinkedList<Packet> rtcpTable = null;

	/**
	 * This table is an indexing of RTP messages in the table attribute.
	 */
	protected LinkedList<Packet> rtpTable = null;

	/**
	 * This table is an indexing of SNMP messages in the table attribute.
	 */
	protected LinkedList<Packet> snmpTable = null;

	/**
	 * This table is an indexing of Syslog messages in the table attribute.
	 */
	protected LinkedList<Packet> syslogTable = null;
	
	/**
	 * This table is an indexing of Time messages in the table attribute.
	 */
	protected LinkedList<Packet> todTable = null;
	
	/**
	 * The console and log file interface.
	 */
	private LogAPI logger = null;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "PDML";
	
	protected PacketDatabase(String tool, String name, ParserFilter cpf) {
		this.tool = tool;
		this.name = name;
		this.filter = cpf;
		logger = LogAPI.getInstance(); 
	}
	
	protected void addPacket(Packet p) {
		if (table != null &&
				p != null) {
			if (p.getProtocol().equals(PDMLTags.BOOTP_PROTOCOL)) {
				if (bootpTable == null)
					bootpTable = new LinkedList<Packet>();
				bootpTable.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.DHCPv6_PROTOCOL)) {
				if (dhcpv6Table == null)
					dhcpv6Table = new LinkedList<Packet>();
				dhcpv6Table.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.TFTP_PROTOCOL)) {
				if (tftpTable == null)
					tftpTable = new LinkedList<Packet>();
				tftpTable.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.TOD_PROTOCOL)) {
				if (todTable == null)
					todTable = new LinkedList<Packet>();
				todTable.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.DNS_PROTOCOL)) {
				if (dnsTable == null)
					dnsTable = new LinkedList<Packet>();
				dnsTable.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.PACKET_CABLE_PROTOCOL)) {
				if (pcTable == null)
					pcTable = new LinkedList<Packet>();
				pcTable.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.RTCP_PROTOCOL)) {
				if (rtcpTable == null)
					rtcpTable = new LinkedList<Packet>();
				rtcpTable.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.RTP_PROTOCOL)) {
				if (rtpTable == null)
					rtpTable = new LinkedList<Packet>();
				rtpTable.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.SNMP_PROTOCOL)) {
				if (snmpTable == null)
					snmpTable = new LinkedList<Packet>();
				snmpTable.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.SYSLOG_PROTOCOL)) {
				if (syslogTable == null)
					syslogTable = new LinkedList<Packet>();
				syslogTable.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.KERBEROS_PROTOCOL)) {
				if (kerberosTable == null)
					kerberosTable = new LinkedList<Packet>();
				kerberosTable.add(p);
			}
			else if (p.getProtocol().equals(PDMLTags.ICMPV6_PROTOCOL)) {
				if (icmpv6Table == null)
					icmpv6Table = new LinkedList<Packet>();
				icmpv6Table.add(p);
			}
			
			table.add(p);
		}
	}
	
	public String getName() {
		return this.name;
	}
	
	public Packet getPacket(String name, String instance) {
		if (table != null) {
			
		}
		return null;
	}
	
	
	public LinkedList<Packet> getBootpPackets(CaptureRef cr) {
		return getPackets(bootpTable, cr);
	}
	
	public LinkedList<Packet> getDHCPv6Packets(CaptureRef cr) {
		return getPackets(dhcpv6Table, cr);
	}
	
	public LinkedList<Packet> getICMPv6Packets(CaptureRef cr) {
		return getPackets(icmpv6Table, cr);
	}
	
	public LinkedList<Packet> getDnsPackets(CaptureRef cr) {
		return getPackets(dnsTable, cr);
	}
	
	public LinkedList<Packet> getKerberosPackets(CaptureRef cr) {
		return getPackets(kerberosTable, cr);
	}
	
	public LinkedList<Packet> getPktcPackets(CaptureRef cr) {
		return getPackets(pcTable, cr);
	}
	
	public LinkedList<Packet> getSnmpPackets(CaptureRef cr) {
		return getPackets(snmpTable, cr);
	}
	
	public LinkedList<Packet> getSyslogPackets(CaptureRef cr) {
		return getPackets(syslogTable, cr);
	}
	
	public LinkedList<Packet> getTodPackets(CaptureRef cr) {
		return getPackets(todTable, cr);
	}
	
	public LinkedList<Packet> getTftpPackets(CaptureRef cr) {
		return getPackets(tftpTable, cr);
	}
	
	private void getIndex(LinkedList<Packet> ll, LinkedList<Packet> dbTable, String name, int index) {
		ListIterator<Packet>iter = dbTable.listIterator();
		int ndx = index;
		while (iter.hasNext()) {
			Packet p = iter.next();
			if (p.getName().equals(name)) {
				ndx--;
			}
			if (ndx == 0) {
				ll.add(p);
				return;
			}
		}
	}
	
	private String getFrames(LinkedList<Packet> ll) {
		ListIterator<Packet> iter = ll.listIterator();
		String result = null;
		while (iter.hasNext()) {
			Packet p = iter.next();
			if (result == null)
				result = "Frames[" + p.getFrame();
			else
				result += ", " + p.getFrame();
		}
		if (result != null) {
			result += "]";
		}
		else 
			result = "";
		return result;
	}
	
	private void getFirst(LinkedList<Packet> ll, LinkedList<Packet> dbTable, String name) {
		ListIterator<Packet> iter = dbTable.listIterator();
		while (iter.hasNext()) {
			Packet p = iter.next();
			if (p.getName().equals(name)) {
				ll.add(p);
				return;
			}
		}
	}
	
	private void getLast(LinkedList<Packet> ll, LinkedList<Packet> dbTable, String name) {
		ListIterator<Packet>iter = dbTable.listIterator(dbTable.size());
		while (iter.hasPrevious()) {
			Packet p = iter.previous();
			if (p.getName().equals(name)) {
				ll.add(p);
				return;
			}
		}
	}
	
	private void getPrev(LinkedList<Packet> ll, LinkedList<Packet> dbTable, String name) {
		ListIterator<Packet>iter = dbTable.listIterator(dbTable.size());
		while (iter.hasPrevious()) {
			Packet p = iter.previous();
			if (p.getName().equals(name)) {
				while (iter.hasPrevious()) {
					Packet p2 = iter.previous();
					if (p2.getName().equals(name)) {
						ll.add(p2);
						return;
					}
				}
			}
		}
	}
	
	private void getAll(LinkedList<Packet> ll, LinkedList<Packet> dbTable, String name) {
		ListIterator<Packet>iter = dbTable.listIterator();
		while (iter.hasNext()) {
			Packet p = iter.next();
			if (p.getName().equals(name)) {
				ll.add(p);
			}
		}
	}
	
	private LinkedList<Packet> getPackets(LinkedList<Packet> table, CaptureRef cr) {
		LinkedList<Packet> packets = new LinkedList<Packet>();
		if (table != null) {
			String instance = cr.getMsgInstance();
			if (instance.equals(MsgQueue.FIRST)) {
				getFirst(packets, table, cr.getMsgType());
			}
			else if (instance.equals(MsgQueue.LAST)) {
				getLast(packets, table, cr.getMsgType());
			}
			else if (instance.equals(MsgQueue.ANY)) {
				getAll(packets, table, cr.getMsgType());
			}
			else if (instance.equals(MsgQueue.PREV)) {
				getPrev(packets, table, cr.getMsgType());
			}
			else {
				try {
					int index = Integer.parseInt(instance);
					getIndex(packets, table, cr.getMsgType(), index);
					
				}
				catch (NumberFormatException nfe) {
					logger.error(PC2LogCategory.PCSim2, subCat, 
							"The msg_instance value(" + instance 
							+ ") of the capture reference is an unsupported value.");
				}
			}
		}
		return packets;
	}
	
	@Override
	public String toString() {
		String result = "PacketDatabase(" + name + ") using " + tool + " contains " + table.size() 
			+ " packets:\n";
		boolean noPackets = true;
		if (bootpTable != null) {
			result += "\tbootp packets:" + bootpTable.size() + "\n";
			String frames = getFrames(bootpTable);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (dhcpv6Table != null) {
			result += "\tdhcpv6 packets:" + dhcpv6Table.size() + "\n";
			String frames = getFrames(dhcpv6Table);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (icmpv6Table != null) {
			result += "\ticmpv6 packets:" + icmpv6Table.size() + "\n";
			String frames = getFrames(icmpv6Table);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (dnsTable != null) {
			result += "\tdns packets:" + dnsTable.size() + "\n";
			String frames = getFrames(dnsTable);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (kerberosTable != null) {
			result += "\tkerberos packets:" + kerberosTable.size() + "\n";
			String frames = getFrames(kerberosTable);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (pcTable != null) {
			result += "\tPacketCable packets:" + pcTable.size() + "\n";
			String frames = getFrames(pcTable);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (rtcpTable != null) {
			result += "\trtcp packets:" + rtcpTable.size() + "\n";
			String frames = getFrames(rtcpTable);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (rtpTable != null) {
			result += "\trtp packets:" + rtpTable.size() + "\n";
			String frames = getFrames(rtpTable);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		
		if (snmpTable != null) {
			result += "\tsnmp packets:" + snmpTable.size() + "\n";
			String frames = getFrames(snmpTable);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (syslogTable != null) {
			result += "\tsyslog packets:" + syslogTable.size() + "\n";
			String frames = getFrames(syslogTable);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (tftpTable != null) {
			result += "\ttftp packets:" + tftpTable.size() + "\n";
			String frames = getFrames(tftpTable);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (todTable != null) {
			result += "\ttime packets:" + todTable.size() + "\n";
			String frames = getFrames(todTable);
			result += "\t    " + frames + "\n";
			noPackets = false;
		}
		if (noPackets) {
			result += "\tNo packets!\n";
		}
		if (filter != null)
			result += "\n\t" + filter.toString() + "\n";
		
		return result;
	}
	
	public void setFilter(ParserFilter cpf) {
		this.filter = cpf;
	}
	
	public ParserFilter getFilter() {
		return this.filter;
	}
}
