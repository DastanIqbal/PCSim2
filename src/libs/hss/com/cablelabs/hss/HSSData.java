package com.cablelabs.hss;

import java.util.*;

public class HSSData {

	protected Hashtable<String, Subscriber> db = new Hashtable<String, Subscriber>();
	
	public HSSData() {
		
	}
	
	public void addSubscriber(String prid, Subscriber s) {
		db.put(prid, s);
	}
	
	public void clear() {
		db.clear();
	}
	
	public Subscriber getSubscriber(String prid) {
		return db.get(prid);
	}
	
	public boolean removeSubscriber(String prid) {
		Subscriber s = db.remove(prid);
		if (s != null)
			return true;
		return false;
	}
	
	public String encode() {
		String result = "";
		if (db != null) {
			Enumeration<Subscriber> elements = db.elements();
			result = "<" + IMSSubscriptionTags.IMS_SUBSCRIPTION + " " 
				+ IMSSubscriptionTags.NAMESPACE + ">\n";
			while (elements.hasMoreElements()) {
				Subscriber s = (Subscriber)elements.nextElement();
				result += s.encode();
			}
			result += "</" + IMSSubscriptionTags.IMS_SUBSCRIPTION + ">\n";
			return result;
		}
		return null;
	}
}
