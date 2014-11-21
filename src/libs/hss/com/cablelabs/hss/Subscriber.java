package com.cablelabs.hss;

import java.util.*;
public class Subscriber {

	protected String privateUserId = null;
	protected LinkedList<ServiceProfile> serviceProfiles = null;
	
	public Subscriber() {
		
	}
	public Subscriber(String privateUserId) {
		this.privateUserId = privateUserId;
	}
	
	public void setPrivateUserId(String privateUserId) {
		this.privateUserId = privateUserId;
	}
	
	public String getPrivateUserId() {
		return this.privateUserId;
	}

	public void addServiceProfile(ServiceProfile sp) {
		if (serviceProfiles == null) {
			serviceProfiles = new LinkedList<ServiceProfile>();
		}
		serviceProfiles.add(sp);
	}
	
	public String encode() {
		String indent = "\t";
		String result = indent + "<" + IMSSubscriptionTags.PRIVATE_ID + ">" + privateUserId 
			+ "</" + IMSSubscriptionTags.PRIVATE_ID + ">\n";
		if (serviceProfiles != null) {
			ListIterator<ServiceProfile> iter = serviceProfiles.listIterator();
			while (iter.hasNext()) {
				ServiceProfile sp = (ServiceProfile)iter.next();
				result += sp.encode();
			}
		}
				
		return result;
	}
}
