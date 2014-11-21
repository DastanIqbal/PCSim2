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

import java.util.Properties;

import com.cablelabs.common.Conversion;
import com.cablelabs.common.Transport;
import com.cablelabs.fsm.Proxy;
import com.cablelabs.fsm.Send;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;

public class Route {

	/**
	 * The IP address that we are using to send the message.
	 */
	protected String localAddress = null;
	/**
	 * The port that we are using to send the message.
	 */
	protected int localPort = 0;
	/**
	 * The IP address of the peer device that we are sending
	 * the message to.
	 */
	protected String peerAddress = null;
	/**
	 * The port information of the peer device that we are sending
	 * the message to.
	 */
	protected int peerPort = 0;


	/**
	 * The send data used to construct the route and message.
	 */
	protected Send send = null;

	/**
	 * The proxy data used to construct the route and message.
	 */
	protected Proxy proxy = null;
	/**
	 * The transport protocol being used for the message.
	 */
	protected Transport transport = null;
	/**
	 * The source is the network element creating the message to be
	 * sent.
	 */
	protected String srcNE = null;
	/** 
	 * The target is who we are going to actually send the message to
	 * on the transport layer.
	 */
	protected String targetNE = null;
	/**
	 *  The destination is the final intended network element that this
	 *  message is to be routed to.
	 */
	protected String destNE = null;

	/**
	 * The properties of the source network element.
	 */
	protected Properties src = null;

	/**
	 * The properties of the target network element.
	 */
	protected Properties target = null;
	/**
	 * The properties of the destination network element.
	 */
	protected Properties dest = null;

	/**
	 * This is the type of device that the source of the 
	 * message is.
	 */
	protected String srcDeviceType = null;

	/**
	 * This is the type of device that the destination of the 
	 * message is.
	 */
	protected String destDeviceType = null;

	/**
	 * This is the type of device that the target of the 
	 * message is.
	 */
	protected String targetDeviceType = null;


	public Route(Send s, String srcNE, Transport transport, 
			String localAddr, int localPort) throws IllegalArgumentException {
		if (s == null)
			throw new IllegalArgumentException("The send parameter is null.");
		if (srcNE == null)
			throw new IllegalArgumentException("The source network element label parameter is null.");
		if (transport == null)
			throw new IllegalArgumentException("The transport parameter is null.");
		if (localAddr == null)
			throw new IllegalArgumentException("The local address parameter is null.");
		if (localPort <= 0)
			throw new IllegalArgumentException("The local port parameter less than or equal to zero.");

		this.send = s;
		this.transport = transport;
		this.localAddress = localAddr;
		this.localPort = localPort;

		this.srcNE = srcNE;
		this.src = SystemSettings.getSettings(srcNE);
		// if we couldn't find the value by the NE label and it ends with a zero
		// assume it is the DUT
		if (this.src == null && srcNE.endsWith("0")) {
			this.src = SystemSettings.getSettings("DUT");
		}
		if (src == null)
			throw new IllegalArgumentException("The source network element's properties, " 
					+ srcNE + ", is null.\n\t" + SystemSettings.dumpKeys());
		// Update the src network label to the <device_type>0 
		// nomenclature
		//		 Next set the device type for the source
		this.srcDeviceType = src.getProperty(SettingConstants.DEVICE_TYPE);
		if (srcNE.equals("DUT") && this.srcDeviceType != null) {
			srcNE = this.srcDeviceType + "0";
		}
		if (srcDeviceType == null)
			throw new IllegalArgumentException("The source network element's(" 
					+ srcNE + ") " + SettingConstants.DEVICE_TYPE + " is null.\n\t" + src);

		


		// The target is who we are going to actually send the message to
		// on the transport layer.
		this.targetNE = send.getTarget();
		this.target = SystemSettings.getSettings(targetNE);
		if (target == null)
			throw new IllegalArgumentException("The target network element's properties, " 
					+ targetNE + ", is null.\n\t" + SystemSettings.dumpKeys());
		//		 Next set the device type for the target
		this.targetDeviceType = this.target.getProperty(SettingConstants.DEVICE_TYPE);

		if (targetDeviceType == null)
			throw new IllegalArgumentException("The target network element's(" 
					+ targetNE + ") " + SettingConstants.DEVICE_TYPE + " is null.\n\t" + target);

		// The destination is the final intended network element that this
		// message is to be routed to.

		this.destNE = send.getDestination();
		if (destNE == null) {
			destNE = targetNE;
			this.dest = target;
			// if a UE is sending to another UE the target is really the source UEs PCSCF
			// and the destination is the other UE
			if ((srcNE.equals("DUT") || srcNE.equals("UE0")) && targetDeviceType.equals("UE") && targetDeviceType.equals(srcDeviceType)) {
				targetNE = src.getProperty(SettingConstants.PCSCF);
				this.target = SystemSettings.getSettings(targetNE);
				targetDeviceType = SettingConstants.PCSCF;
			}
		}
		else {
			this.dest = SystemSettings.getSettings(destNE);
		}


		if (dest == null)
			throw new IllegalArgumentException("The destination network element's properties, " 
					+ destNE + ", is null.\n\t" + SystemSettings.dumpKeys());

		//		 Next set the device type for the target
		this.destDeviceType = dest.getProperty(SettingConstants.DEVICE_TYPE);
		if (destDeviceType == null)
			throw new IllegalArgumentException("The destination network element's(" 
					+ destNE + ") " + SettingConstants.DEVICE_TYPE + " is null.\n\t" + dest);


		String targetIP = target.getProperty(SettingConstants.IP);
		boolean localIsV6 = Conversion.isIPv6Address(localAddress);
		if (localIsV6 != Conversion.isIPv6Address(targetIP)) {
			targetIP = target.getProperty(SettingConstants.IP2);
			if (localIsV6 != Conversion.isIPv6Address(targetIP)) {
				throw new IllegalArgumentException("The target network element(" 
						+ targetNE + ") does not have a needed ip" + (localIsV6 ? "v6" : "v4") + " address for communication with " + srcNE + ".");
			}
		}

		this.peerAddress = targetIP;
		if (Conversion.isIPv6Address(peerAddress)) {
			String zone = target.getProperty(SettingConstants.IPv6_ZONE);
			peerAddress = Conversion.makeAddrURL(peerAddress, zone);
		}
		this.peerPort = Integer.parseInt(
				target.getProperty(
						transport.toString().toUpperCase()+ "Port"));
	}

	public Route(Proxy p, String srcNE, Transport transport, 
			String localAddr, int localPort) throws IllegalArgumentException {
		if (p == null)
			throw new IllegalArgumentException("The proxy parameter is null.");
		if (srcNE == null)
			throw new IllegalArgumentException("The source network element label parameter is null.");
		if (transport == null)
			throw new IllegalArgumentException("The transport parameter is null.");
		if (localAddr == null)
			throw new IllegalArgumentException("The local address parameter is null.");
		if (localPort <= 0)
			throw new IllegalArgumentException("The local port parameter less than or equal to zero.");

		this.proxy = p;
		this.transport = transport;
		this.localAddress = localAddr;
		this.localPort = localPort;

		this.srcNE = srcNE;
		this.src = SystemSettings.getSettings(srcNE);
		if (src == null)
			throw new IllegalArgumentException("The source network element's properties, " 
					+ srcNE + ", is null.\n\t" + SystemSettings.dumpKeys());
		// Update the src network label to the <device_type>0 
		// nomenclature
		if (srcNE.equals("DUT")) {
			String device = src.getProperty(SettingConstants.DEVICE_TYPE);
			if (device != null)
				srcNE = device + "0";
		}
		// The target is who we are going to actually send the message to
		// on the transport layer.
		this.targetNE = p.getTarget();
		this.target = SystemSettings.getSettings(targetNE);
		if (target == null)
			throw new IllegalArgumentException("The target network element's properties, " 
					+ targetNE + ", is null.\n\t" + SystemSettings.dumpKeys());
		
		// The destination is the final intended network element that this
		// message is to be routed to.
		this.destNE = p.getTarget();
		if (destNE == null) {
			destNE = targetNE;
			this.dest = target;
		}
		else {
			this.dest = SystemSettings.getSettings(destNE);
		}
		
		if (dest == null)
			throw new IllegalArgumentException("The destintation network element's properties, " 
					+ destNE + ", is null.\n\t" + SystemSettings.dumpKeys());

		this.peerAddress = dest.getProperty(SettingConstants.IP);
		if (Conversion.isIPv6Address(localAddress) != Conversion.isIPv6Address(peerAddress)) {
			this.peerAddress = dest.getProperty(SettingConstants.IP2);
			if (Conversion.isIPv6Address(localAddress) != Conversion.isIPv6Address(peerAddress)) {
				throw new IllegalArgumentException("The target does not have a needed ip" +  (Conversion.isIPv6Address(localAddress) ? "v6" : "v4") + " address");
			}
		}
		
		if (Conversion.isIPv6Address(peerAddress)) {
			String zone = target.getProperty(SettingConstants.IPv6_ZONE);
			peerAddress = Conversion.makeAddrURL(peerAddress, zone);
		}
		this.peerPort = Integer.parseInt(
				target.getProperty(transport.toString().toUpperCase()+ "Port"));
	}

	/**
	 * This method causes the peerAddress and peerPort information to
	 * be refreshed from the system settings information while maintaining 
	 * the ip type of the last value.
	 *
	 */
	public void refresh() {
		
		String pa = target.getProperty(SettingConstants.IP);
		if (Conversion.isIPv6Address(pa)) {
			String zone = target.getProperty(SettingConstants.IPv6_ZONE);
			pa = Conversion.makeAddrURL(pa, zone);
		}
		
		boolean ipv6 = Conversion.isIPv6Address(peerAddress); 
		if (ipv6 != Conversion.isIPv6Address(pa)) {
			pa = target.getProperty(SettingConstants.IP2);
			if (Conversion.isIPv6Address(pa)) {
				String zone = target.getProperty(SettingConstants.IPv6_ZONE);
				pa = Conversion.makeAddrURL(pa, zone);
				if (ipv6 != Conversion.isIPv6Address(pa)) {
					throw new IllegalStateException("The target network element's(" 
							+ targetNE + ") does not have a needed ip" + (ipv6 ? "v6" : "v4") + " address.");
				}
			}
		}
		
		this.peerPort = Integer.parseInt(target.getProperty(transport.toString().toUpperCase()+ "Port"));
		this.peerAddress = pa;
	}
}
