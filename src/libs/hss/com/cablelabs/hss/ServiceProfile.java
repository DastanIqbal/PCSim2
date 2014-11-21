package com.cablelabs.hss;

import java.util.*;

public class ServiceProfile {
	
	protected LinkedList<PublicUserIdentity> publicUserIds = null;
	protected LinkedList<ServiceAuthorization> serviceAuths = null;
	protected LinkedList<InitialFilterCriteria> ifcs = null;
	protected SharedInitialFilterCriteria sifc = null;

	public ServiceProfile() {
		this.publicUserIds = new LinkedList<PublicUserIdentity>();
	}
	
	public void add(PublicUserIdentity pui) {
		this.publicUserIds.add(pui);
	}
	
	public void addIFC(InitialFilterCriteria pui) {
		if (ifcs == null)
			ifcs = new LinkedList<InitialFilterCriteria>();
		this.ifcs.add(pui);
	}
	
	public boolean contains(String pui) {
		ListIterator<PublicUserIdentity> iter = publicUserIds.listIterator();
		while (iter.hasNext()) {
			PublicUserIdentity id = iter.next();
			if (pui.equals(id)) 
				return true;
		}
		return false;
	}
	
	public ListIterator<InitialFilterCriteria> getIFCs() {
		return this.ifcs.listIterator();
	}
	public boolean remove(String pui) {
		ListIterator<PublicUserIdentity> iter = publicUserIds.listIterator();
		while (iter.hasNext()) {
			PublicUserIdentity id = iter.next();
			if (pui.equals(id)) {
				publicUserIds.remove();
				return true;
			}
		}
		return false;
	}
	
	public void removeAll() {
		publicUserIds.clear();
	}
	
	public void removeAllIFCs() {
		ifcs.clear();
	}
	
	public String encode() {
		String indent = "\t";
		String result = indent + "<" + IMSSubscriptionTags.SERVICE_PROFILE + ">\n";

		if (publicUserIds != null) {
			ListIterator<PublicUserIdentity> iter = publicUserIds.listIterator();
			while (iter.hasNext()) {
				PublicUserIdentity pui = (PublicUserIdentity)iter.next();
				result += pui.encode();
			}
		}
		if (ifcs != null) {
			ListIterator<InitialFilterCriteria> iter = ifcs.listIterator();
			while (iter.hasNext()) {
				InitialFilterCriteria ifc = (InitialFilterCriteria)iter.next();
				result += ifc.encode();
			}
		}
//		if (sifc != null) {
//			result += indent + "\t<" + IMSSubscriptionTags.SHARED_IFC_SET_ID + ">" + privateUserId 
//				+ "</" + IMSSubscriptionTags.SHARED_IFC_SET_ID + ">\n";
//		}
		result += indent + "</" + IMSSubscriptionTags.SERVICE_PROFILE + ">\n";		
		return result;
	}
	
//	<?xml version="1.0" encoding="UTF-8"?>
//	<IMSSubscription xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="D:\ \CxDataType.xsd">
//			<PrivateID>IMPI1@homedomain.com</PrivateID>
//			<ServiceProfile>
//				<PublicIdentity>
//					<BarringIndication>1</BarringIndication>
//					<Identity> sip:IMPU1@homedomain.com </Identity>
//				</PublicIdentity>
//				<PublicIdentity>
//					<Identity> sip:IMPU2@homedomain.com </Identity>
//				</PublicIdentity>
//				<InitialFilterCriteria>
//					<Priority>0</Priority>
//					<TriggerPoint>
//						<ConditionTypeCNF>1</ConditionTypeCNF>
//						<SPT>
//							<ConditionNegated>0</ConditionNegated>
//							<Group>0</Group>
//							<Method>INVITE</Method>
//						</SPT>
//						<SPT>
//							<ConditionNegated>0</ConditionNegated>
//							<Group>0</Group>
//							<Method>MESSAGE</Method>
//						</SPT>
//						<SPT>
//							<ConditionNegated>0</ConditionNegated>
//							<Group>0</Group>
//							<Method>SUBSCRIBE</Method>
//						</SPT>
//						<SPT>
//							<ConditionNegated>0</ConditionNegated>
//							<Group>1</Group>
//							<Method>INVITE</Method>
//						</SPT>
//						<SPT>
//							<ConditionNegated>0</ConditionNegated>
//							<Group>1</Group>
//							<Method>MESSAGE</Method>
//						</SPT>
//
//						<SPT>
//							<ConditionNegated>1</ConditionNegated>
//							<Group>1</Group>
//							<SIPHeader>
//								<Header>From</Header>
//								<Content>"joe"</Content>
//							</SIPHeader>
//						</SPT>
//					</TriggerPoint>
//					<ApplicationServer>
//						<ServerName>sip:AS1@homedomain.com</ServerName>
//						<DefaultHandling>0</DefaultHandling>
//					</ApplicationServer>
//				</InitialFilterCriteria>
//			</ServiceProfile>
//	</IMSSubscription>

	
}
