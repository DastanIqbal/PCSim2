package com.cablelabs.hss;

public class ServicePointTrigger {

	protected Integer conditionNegated = null;
	protected Integer group = null;
	// protected LinkedList<RegistrationType> regType = null;
	protected SPTType type = null;
	protected String choice = null;
	protected String content = null;
	protected Integer sessionCase = null;

	public String getChoice() {
		return this.choice;
	}
	
	public String getContent() {
		return this.content;
	}
	
	public Integer getGroup() {
		return this.group;
	}
	
	public SPTType getSPTType() {
		return this.type;
	}
	
	public boolean isConditionNegated() {
		if (this.conditionNegated != null &&
				this.conditionNegated == 1)
			return true;
		return false;
	}
	public void setConditionNegated(int flag) {
		this.conditionNegated = flag;
	}
	public void setChoice(String s) {
		this.choice = s;
	}
	
	public void setContent(String s) {
		this.content = s;
	}
	
	public void setGroup(int value) {
		this.group = value;
	}
	public void setSPTType(SPTType t) {
		this.type = t;
	}
	
	public String encode() {
		String indent = "\t\t\t\t";
		String result = indent + "<" + IMSSubscriptionTags.SPT + ">\n";

		if (conditionNegated != null) {
			result += indent + "\t<" + IMSSubscriptionTags.CONDITION_NEGATED + ">" 
				+ conditionNegated + "</" + IMSSubscriptionTags.CONDITION_NEGATED + ">\n";
		}
		if (group != null) {
			result += indent + "\t<" + IMSSubscriptionTags.GROUP + ">" 
				+ group + "</" + IMSSubscriptionTags.GROUP + ">\n";
		}
		if (type != null) {
			if (type == SPTType.METHOD) {
				result += indent + "\t<" + IMSSubscriptionTags.METHOD + ">" 
				+ choice + "</" + IMSSubscriptionTags.METHOD + ">\n";
			}
			else if (type == SPTType.REQUEST_URI) {
				result += indent + "\t<" + IMSSubscriptionTags.REQUEST_URI + ">" 
				+ choice + "</" + IMSSubscriptionTags.REQUEST_URI + ">\n";
			}
			else if (type == SPTType.SIP_HEADER) {
				result += indent + "\t<" + IMSSubscriptionTags.SIP_HEADER + ">\n" 
					+ indent + "\t\t<" + IMSSubscriptionTags.HEADER + ">"
					+ choice + "</" + IMSSubscriptionTags.HEADER + ">\n";
					if (content != null) {
						result += indent + "\t\t<" + IMSSubscriptionTags.CONTENT + ">"
						+ content + "</" + IMSSubscriptionTags.CONTENT + ">\n";
					}
					result += indent + "\t</" + IMSSubscriptionTags.SIP_HEADER + ">\n";
			}
			else if (type == SPTType.SESSION_DESCRIPTOR) {
				result += indent + "\t<" + IMSSubscriptionTags.SESSION_DESCRIPTION + ">\n" 
					+ indent + "\t\t<" + IMSSubscriptionTags.LINE + ">"
					+ choice + "</" + IMSSubscriptionTags.LINE + ">\n";
					if (content != null) {
						result += indent + "\t\t<" + IMSSubscriptionTags.CONTENT + ">"
						+ content + "</" + IMSSubscriptionTags.CONTENT + ">\n";
					}
					result += indent + "\t</" + IMSSubscriptionTags.SESSION_DESCRIPTION + ">\n";
			}
			else if (type == SPTType.SESSION_CASE) {
				result += indent + "\t<" + IMSSubscriptionTags.SESSION_CASE + ">" 
					+ sessionCase + "</" + IMSSubscriptionTags.SESSION_CASE + ">\n";
			}
		}
			
		result += indent + "</" + IMSSubscriptionTags.SPT + ">\n";		
		return result;
	}
}
