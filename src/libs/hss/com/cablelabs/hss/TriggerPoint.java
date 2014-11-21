package com.cablelabs.hss;

import java.util.*;

public class TriggerPoint {

	protected Integer conditionTypeCNF = null;
	protected LinkedList<ServicePointTrigger> serviceTriggers = null;
	
	public TriggerPoint() {
		
	}
	
	public TriggerPoint(int conditionType) {
		this.conditionTypeCNF = conditionType;
	}
	
	public ListIterator<ServicePointTrigger> getServicePointTriggers() {
		if (serviceTriggers != null)
			return serviceTriggers.listIterator();
		else
			return null;
	}
	
	public boolean isConditionTypeCNF() {
		if (conditionTypeCNF != null && conditionTypeCNF == 1) 
			return true;
		else
			return false;
	}
	
	public void setConditionTypeCNF(int condition) {
		this.conditionTypeCNF = condition;
	}
	
	public void setServivePointTriggers(LinkedList<ServicePointTrigger> triggers) {
		serviceTriggers = triggers;
	}
	
	public void addServicePointTrigger(ServicePointTrigger trigger) {
		if (serviceTriggers != null)
			serviceTriggers.add(trigger);
		else {
			serviceTriggers = new LinkedList<ServicePointTrigger>();
			serviceTriggers.add(trigger);
		}
	}
	
	public String encode() {
		String indent = "\t\t\t";
		String result = indent + "<" + IMSSubscriptionTags.TRIGGER_POINT + ">\n";

		if (conditionTypeCNF != null) {
			result += indent + "\t<" + IMSSubscriptionTags.CONDITION_TYPE_CNF + ">" 
				+ conditionTypeCNF + "</" + IMSSubscriptionTags.CONDITION_TYPE_CNF + ">\n";
		}
		if (serviceTriggers != null) {
			ListIterator<ServicePointTrigger> iter = serviceTriggers.listIterator();
			while (iter.hasNext()) {
				ServicePointTrigger trigger = (ServicePointTrigger)iter.next();
				result += trigger.encode();
			}
		}
	
		result += indent + "</" + IMSSubscriptionTags.TRIGGER_POINT + ">\n";		
		return result;
	}
}
