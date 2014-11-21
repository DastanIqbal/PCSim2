/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.models;

import java.util.HashSet;

import com.cablelabs.fsm.PresenceStatus;

public class PresenceData {

	/**
	 * The status flag indicates whether the device
	 * is in the open or closed  status
	 */
	protected PresenceStatus status = null;
	
	/**
	 * The ne attribute is the network element label
	 * that is associated with this status.
	 */
	protected String neLabel = null;
	
	/**
	 * The entity-tag defines the last value given to the
	 * device when its Published its status as open.
	 */
	protected long entityTag = -1;
	
	/**
	 * This is a set of network elements that have subscribed
	 * to watch for status changes for this network element.
	 */
	protected HashSet<String> watchers = new HashSet<String>();
	
	/**
	 * This contains a set of network elements that are restricted
	 * by the platform from being notified of status changes on
	 * this network element.
	 */
	protected HashSet<String> restricted = new HashSet<String>();
	
	protected PresenceData(String ne, PresenceStatus status, long tag) {
		this.neLabel = ne;
		this.status = status;
		this.entityTag = tag;
		
	}
	
	public void addWatcher(String ne) {
		watchers.add(ne);
	}
	
	public Long getEntity() {
		return entityTag;
	}
	
	public String getLabel() {
		return neLabel;
	}
	
	public String getStatusString() {
		return status.toString().toLowerCase();
		
	}
	
	public boolean isOpen() {
		if (status == PresenceStatus.OPEN)
			return true;
		return false;
	}
	
	public boolean isClosed() {
		if (status == PresenceStatus.CLOSED)
			return true;
		return false;
	}
	
	public void removeWatcher(String ne) {
		watchers.remove(ne);
	}
	
	public void setStatus(PresenceStatus ps) {
		this.status = ps;
	}
	
	@Override
	public String toString() {
		String result = "NELabel=" + neLabel 
			+ "'s has entity=" + entityTag + " and status=" + status;
		
		return result;
	}
	
	public void updateEntity(long newEntity) {
		this.entityTag = newEntity;
	}
}
