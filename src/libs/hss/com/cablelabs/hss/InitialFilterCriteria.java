package com.cablelabs.hss;

public class InitialFilterCriteria {

	protected Integer priority = -1;
	
	// NOTE a null value indicates the iFC applies to both registered
	// and unregistered parts of the user profile.
	protected Integer profilePartIndicator = null;
	protected TriggerPoint triggerPoint = null;
	protected ApplicationServer appServer = null;
	
	public ApplicationServer getApplicationServer() {
		return this.appServer;
	}

	public Integer getPriority() {
		return this.priority;
	}
	
	public Integer getProfilePartIndicator() {
		return this.profilePartIndicator;
	}
	
	public TriggerPoint getTriggerPoint() {
		return this.triggerPoint;
	}
	
	public void setApplicationServer(ApplicationServer as) {
		this.appServer = as;
	}
	
	public void setPriority(int value) {
		this.priority = value;
	}
	
	public void setProfilePartIndicator(int ppi) {
		this.profilePartIndicator = ppi;
	}
	
	public void setTriggerPoint(TriggerPoint tp) {
		this.triggerPoint = tp;
	}

	public String encode() {
		String indent = "\t\t";
		String result = indent + "<" + IMSSubscriptionTags.INITIAL_FILTER_CRITERIA + ">\n";

		if (priority != null) {
			result += indent + "\t<" + IMSSubscriptionTags.PRIORITY + ">" 
				+ priority + "</" + IMSSubscriptionTags.PRIORITY + ">\n";
		}
		if (triggerPoint != null) {
			result += triggerPoint.encode();
		}
		if (appServer != null) {
			result += appServer.encode();
		}
		if (profilePartIndicator != null) {
			result += indent + "\t<" + IMSSubscriptionTags.PROFILE_PART_INDICATOR + ">" 
				+ profilePartIndicator 
			+ "</" + IMSSubscriptionTags.PROFILE_PART_INDICATOR + ">\n";
		}
		result += indent + "</" + IMSSubscriptionTags.INITIAL_FILTER_CRITERIA + ">\n";		
		return result;
	}
}
