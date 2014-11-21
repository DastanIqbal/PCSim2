package com.cablelabs.hss;

public class PublicUserIdentity {

	protected Integer barringIndication = null;
	protected IdentityType type = IdentityType.PUBLIC_USER_IDENTITY;
	protected String extension = null;
	
	protected String publicUserId = null;
	
	public PublicUserIdentity(IdentityType type) {
		this.type = type;
	}
	
	public boolean isBarringIndication() {
		if (barringIndication != null &&
				barringIndication == 1)
			return true;
		return false;
	}
	public void setBarringIndication(int barringIndication) {
		this.barringIndication = barringIndication;
	}
	public IdentityType getIdentityType() {
		return type;
	}
	public void setIdentityType(IdentityType type) {
		this.type = type;
	}

	public void setIdentity(String pui) {
		this.publicUserId = pui;
	}
	
	public String getIdentity() {
		return publicUserId;
	}
	
	public String encode() {
		String indent = "\t\t";
		String result = indent + "<" + IMSSubscriptionTags.PUBLIC_IDENTITY + ">\n";
		
		if (barringIndication != null) {
			result +=  indent + "\t<" + IMSSubscriptionTags.BARRING_INDICATION + ">" 
			+ barringIndication + "</" + IMSSubscriptionTags.BARRING_INDICATION + ">\n";
		}
		if (type != null) {
			if (type == IdentityType.PUBLIC_USER_IDENTITY) {
				result +=  indent + "\t<" + IMSSubscriptionTags.IDENTITY + ">" 
				+ publicUserId + "</" + IMSSubscriptionTags.IDENTITY + ">\n";
			}
		}
		result += indent + "</" + IMSSubscriptionTags.PUBLIC_IDENTITY + ">\n";		
		return result;
	}
//	public String getWildcardedPSI() {
//		return wildcardedPSI;
//	}
//	public void setWildcardedPSI(String wildcardedPSI) {
//		if (wildcardedPSI != null) {
//			this.wildcardedPSI = wildcardedPSI;
//			this.type = IdentityType.WILDCARDED_PUBLIC_SERVICE_IDENTITY;
//		}
//	}
//	public String getDefaultPublicId() {
//		return defaultPublicId;
//	}
//	public void setDefaultPublicId(String defaultPublicId) {
//		this.defaultPublicId = defaultPublicId;
//	}
//	public String getDisplayName() {
//		return displayName;
//	}
//	public void setDisplayName(String displayName) {
//		this.displayName = displayName;
//	}
//	public String getAliasGroupID() {
//		return aliasGroupID;
//	}
//	public void setAliasGroupID(String aliasGroupID) {
//		this.aliasGroupID = aliasGroupID;
//	}
//	public LinkedList<String> getPublicUserIds() {
//		return publicUserIds;
//	}
//	public void setPublicUserIds(LinkedList<String> publicUserIds) {
//		this.publicUserIds = publicUserIds;
//	}
}
