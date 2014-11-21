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

import com.cablelabs.common.Conversion;
import com.cablelabs.fsm.CaptureAttributeType;
import com.cablelabs.fsm.CaptureRef;
import com.cablelabs.fsm.ParserFilter;
import com.cablelabs.log.LogAPI;

public class CaptureTester {

	public String file = "./temp.xml";
	
	public PDMLParser parser = new PDMLParser();

	public PacketDatabase db = null;
	
	public LogAPI logger = LogAPI.getInstance();
	
	public CaptureLocator cl = CaptureLocator.getInstance();
	/**
	 * @param args
	 */
	
	public boolean parse(String name, ParserFilter filter) {
		try {
			db = parser.parse(file, name, filter);
			if (db != null)
				return true;
			
		}
		catch (Exception ex) {
			System.err.println("Exception occurred:\n" + ex.getMessage() 
					+ "\n" + ex.getStackTrace());
		}
		return false;
	}
	
	public void compare(CaptureRef cr, String expected) {
		LinkedList<String> values = cl.getReferenceInfo(db, cr);
		if (values.size() == 1) {
			String val = values.getFirst();
			if (cr.getConverter() != null) {
				if (cr.getConverter().equalsIgnoreCase("string")) {
					val = Conversion.hexStringToString(val);
				}
			}
			else if (cr.getAdd() != null) {
				try {
					int offset = val.indexOf(".");
					if (offset != -1) {
						val = val.substring(0, offset);
					}
					Long v = Long.parseLong(val);
					Long adjust = Long.parseLong(cr.getAdd());
					val = Long.toString((v + adjust));
				}
				catch (NumberFormatException nfe) {
					System.err.println("An add value could not be performed for reference=" + cr);
				}
			}
			if (val.equals(expected))
				System.out.println("Success 1 value(" + val + ") is " + expected + ".");
			else
				System.out.println("Fail 1 value(" + val + ") is not " + expected + ".");
		}
		else
			System.out.println("Failed " + values.size() + " values.");
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		CaptureTester ct = new CaptureTester();
		LogAPI.setConsoleCreated();
		String name = "eCM";
		String dva = "eDVA";
		//String ip = "10.32.107.1"; 
		// ip = "10.32.157.1"; 
		// String limit = "dst";
		// String port = "67";
		// String proto = "bootp";
		ParserFilter filter = null; // new ParserFilter(null, proto, null, null);
		//filter.setMsgType(PDMLTags.DHCP_DISCOVER);
		//filter.setClientMacAddr("00d059e138ce");
		if (ct.parse(name, filter)) {
			System.out.println(ct.db.toString());
			System.out.println("Success.");
		}
		else 
			System.out.println("Failure");
		filter = new ParserFilter("10.32.157.209", "snmp", null, null);
		if (ct.parse(dva, filter)) {
			System.out.println(ct.db.toString());
			System.out.println("Success.");
		}
		else 
			System.out.println("Failure");
		
		CaptureRef cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_NAK);
		cr.setAttributeType(CaptureAttributeType.NUM);
		ct.compare(cr, "12");
		
		cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_OFFER);
		cr.setAttributeType(CaptureAttributeType.SIZE);
		ct.compare(cr, "387");
		
		cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_DISCOVER);
		cr.setMsgInstance("2");
		cr.setAttributeType(CaptureAttributeType.NUM);
		ct.compare(cr, "26");
		
		cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_DISCOVER);
		cr.setMsgInstance("2");
		cr.setField("geninfo.num");
		cr.setAttributeType(CaptureAttributeType.SHOW);
		ct.compare(cr, "26");
		
		cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_REQUEST);
		cr.setField("geninfo");
		cr.setAttributeType(CaptureAttributeType.TIMESTAMP);
		ct.compare(cr, "1222902866.436610000");
		
		cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_REQUEST);
		cr.setField("frame.number");
		cr.setAttributeType(CaptureAttributeType.DEFAULT);
		ct.compare(cr, "11");
		
		cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_OFFER);
		cr.setField("Option54");
		cr.setAttributeType(CaptureAttributeType.DEFAULT);
		ct.compare(cr, "36040a2000c3");
		
		cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_OFFER);
		cr.setField("Option54.value");
		cr.setAttributeType(CaptureAttributeType.DEFAULT);
		ct.compare(cr, "0a2000c3");
		
		cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_OFFER);
		cr.setField("Option54.value");
		cr.setAttributeType(CaptureAttributeType.DEFAULT);
		ct.compare(cr, "0a2000c3");
		
		cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_ACK);
		cr.setField("udp.srcport");
		cr.setAttributeType(CaptureAttributeType.SHOW);
		ct.compare(cr, "67");
		
		cr = new CaptureRef(PDMLTags.BOOTP_PROTOCOL, name);
		cr.setMsgType(PDMLTags.DHCP_OFFER);
		cr.setField("Option122.suboption2");
		cr.setAttributeType(CaptureAttributeType.SIZE);
		ct.compare(cr, "6");
		
		cr = new CaptureRef(PDMLTags.SNMP_PROTOCOL, dva);
		cr.setMsgType(PDMLTags.SET_REQUEST);
		cr.setField("RFC1213-MIB::mib-2-140-1-2-9-0.octets");
		cr.setAttributeType(CaptureAttributeType.VALUE);
		cr.setConverter("string");
		ct.compare(cr, "tftp://10.32.0.200/Device_Level_Secure.cfg");
		
		cr = new CaptureRef(PDMLTags.SNMP_PROTOCOL, dva);
		cr.setMsgType(PDMLTags.SET_REQUEST);
		cr.setField("geninfo.timestamp");
		cr.setAttributeType(CaptureAttributeType.VALUE);
		cr.setAdd("15000");
		ct.compare(cr, "1222917878");
	}
	
}
