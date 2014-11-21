package com.cablelabs.hss;

public class ApplicationServer {
	
	protected String serverName = null;
	protected Integer defHandling = null;
	protected String serviceInfo = null;
	protected String defHandlingIndex = null;
	public ApplicationServer() {
		
	}
	
	public ApplicationServer(String serverName, Integer handling) {
		this.serverName = serverName;
		this.defHandling = handling;
	}
	public String getServerName() {
		return this.serverName;
	}
	
	public Integer getDefaultHandling() {
		return this.defHandling;
	}
	
	public String getDefaultHandlingIndex() {
		return this.defHandlingIndex;
	}
	public String getServiceInfo() {
		return this.serviceInfo;
	}
	
	public void setServiceInfo(String info)  {
		this.serviceInfo = info;
	}
	
	public void setServerName(String name) {
		this.serverName = name;
	}
	
	public void setDefaultHandling(int dh) {
		this.defHandling = dh;
	}
	
	public void setDefaultHandlingIndex(String index) {
		this.defHandlingIndex = index;
	}
	
	public String encode() {
		String indent = "\t\t\t\t";
		String result = indent + "<" + IMSSubscriptionTags.APPLICATION_SERVER + ">\n";
		if (serverName != null) {
			result += indent + "\t<" + IMSSubscriptionTags.SERVER_NAME 
			+ ">" + serverName + "<" + IMSSubscriptionTags.SERVER_NAME + "/>\n";
		}
		if (defHandling != null) {
			result += indent + "\t<" + IMSSubscriptionTags.DEFAULT_HANDLING;
			if (defHandlingIndex != null) {
				result += " index=\"" + defHandlingIndex + "\"";
			}
			result += ">" + defHandling + "<" + IMSSubscriptionTags.DEFAULT_HANDLING 
			+ "/>\n";
		}
		if (serviceInfo != null) {
			result += indent + "\t<" + IMSSubscriptionTags.SERVICE_INFO 
			+ ">" + serviceInfo + "<" + IMSSubscriptionTags.SERVICE_INFO + "/>\n";
		}
		
		result += indent + "</" + IMSSubscriptionTags.APPLICATION_SERVER + ">\n";
		return result;
	}
}
