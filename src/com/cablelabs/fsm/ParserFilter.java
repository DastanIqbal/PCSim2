/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.fsm;

public class ParserFilter implements Reference {

	private String ip = null;
	private String protocol = null;
	//private boolean and = true;
	private String port = null;
	private boolean srcOnly = false;
	private boolean dstOnly = false;
	private String msgType = null;
	private String clientMacAddr = null;
	
	public ParserFilter() {
		
	}
	
	public ParserFilter(String ip, String protocol, String port, String limit) {
		this.ip = ip;
		this.protocol = protocol;
		this.port = port;
		if (limit != null) {
			if (limit.equalsIgnoreCase("src"))
				this.srcOnly = true;
			else if (limit.equalsIgnoreCase("dst") ||
					limit.equalsIgnoreCase("dest")) 
				this.dstOnly = true;
				
		}
	}
	@Override
    public Object clone() throws CloneNotSupportedException {
		ParserFilter retval = (ParserFilter)super.clone();
		if (retval != null ) {
			if (this.ip != null) 
				retval.ip = new String(this.ip);
			if (this.protocol != null) 
				retval.protocol = new String(this.protocol);
			if (this.port != null) 
				retval.port = new String(this.port);
			//retval.and = this.and;
			retval.srcOnly = this.srcOnly;
			retval.dstOnly = this.dstOnly;
		}	

		return retval;
	}

	/**
	 * The declaration for defining a toString method for all references.
	 */
	@Override
    public String toString() {
		String result = "";
		if (ip != null ||
				port != null ||
				protocol != null) {
			result += "parser_filter";
			if (ip != null)
				result += " ip=" + ip;

			if (port != null)
				result += " port=" + port;

			if (srcOnly)
				result += " srcOnly=" + srcOnly;
			else if (dstOnly)
				result += " dstOnly=" + dstOnly;

			if (protocol != null)
				result += " protocol=" + protocol;
			
			if (clientMacAddr != null) {
				result += " clientMAC=" + clientMacAddr;
			}
		}
		return result;
	}
	
	@Override
    public String display() {
		return toString();
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
	    this.ip = ip;
	}

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public boolean isSrcOnly() {
		return srcOnly;
	}

	public void setSrcOnly(boolean srcOnly) {
		this.srcOnly = srcOnly;
		if (srcOnly)
			this.dstOnly = false;
	}

	public boolean isDstOnly() {
		return dstOnly;
	}

	public void setDstOnly(boolean dstOnly) {
		this.dstOnly = dstOnly;
		if (dstOnly)
			this.srcOnly = false;
	}

	public String getMsgType() {
		return msgType;
	}

	public void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public String getClientMacAddr() {
		return clientMacAddr;
	}

	public void setClientMacAddr(String clientMacAddr) {
		this.clientMacAddr = clientMacAddr;
	}

}
