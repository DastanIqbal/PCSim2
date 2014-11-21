package com.cablelabs.hss;

import java.util.*;

public class ServiceAuthorization {

	protected Integer subscribedMediaProfileId = null;
	protected LinkedList<String> serviceIds = null;
	
	protected Integer getSubscribedMediaProfileId() {
		return this.subscribedMediaProfileId;
	}
	
	protected ListIterator<String> getServiceIds() {
		if (serviceIds != null) 
			return this.serviceIds.listIterator();
		else
			return null;
	}
	
	protected void setSubscribedMediaProfileId(int id) {
		this.subscribedMediaProfileId = id;
	}
	
	protected void setServiceIds(LinkedList<String> ids) {
		this.serviceIds = ids;
	}
	
	protected void addServiceId(String id) {
		if (serviceIds != null) 
			serviceIds.add(id);
		else {
			serviceIds = new LinkedList<String>();
			serviceIds.add(id);
		}
	}
	
	protected void removeServiceId(String id) {
		if (serviceIds != null) {
			ListIterator<String> iter = this.serviceIds.listIterator();
			while (iter.hasNext()) {
				String entry = iter.next();
				if (id.equals(entry)) 
					serviceIds.remove();
			}
		}
		if (serviceIds.size() <= 0)
			serviceIds = null;
	}
}
