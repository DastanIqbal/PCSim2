package com.cablelabs.hss;

public class IMSSubscriptionTags {

	public static final String APPLICATION_SERVER = "ApplicationServer"; 
	// children - ServerName (1)
	//            DefaultHandling (0 to 1)
	//            ServiceInfo (0 to 1)
	
	public static final String BARRING_INDICATION = "BarringIndication"; 
	// boolean 0 (false) 1 (true)
	public static final String FALSE = "0";
	public static final String TRUE = "1";
	
	public static final String CONDITION_NEGATED = "ConditionNegated"; 
	// boolean 0 (false) 1 (true)
	
	public static final String CONDITION_TYPE_CNF = "ConditionTypeCNF"; 
	// boolean 0 (false) 1 (true)
	
	public static final String CONTENT = "Content"; // string
	
	public static final String CORE_NETWORK_SERVICES_AUTHORIZATION = "CoreNetworkServicesAuthorization"; 
	// children - SubscribedMediaProfileId (0 to 1)
	//            Extension (0 to 1)
	
	public static final String DEFAULT_HANDLING = "DefaultHandling"; 
	// enumerated 0 (SESSION_CONTINUED) 1 (SESSION_TERMINATED)
	public static final int SESSION_CONTINUED = 0;
	public static final int SESSION_TERMINATED = 1;
	
	public static final String DISPLAY_NAME = "DisplayName"; // string
	
	public static final String EXTENSION = "Extension";
	// children - SharedIFCSetID (0 to n)
	//            RegistrationType (0 to 2)
	//            IdentityType (0 to 1)
	//            WildcardedPSI (0 to 1)
	//            DisplayName (0 to 1)
	//            ListOfServiceIds (0 to 1)
	//            ServiceId (0 to n)
	
	public static final String GROUP = "Group"; // integer >=0
	
	public static final String HEADER = "Header"; // string
	
	public static final String IDENTITY = "Identity"; // anyURI from RFC 2486, RFC3261 or RFC 3966
	
	public static final String IDENTITY_TYPE = "IdentityType"; 
	// enumerated 0 (PUBLIC_USER_IDENTITY), 1 (DISTINCT_PSI), 2 (WILDCARDED_PSI)
	public static final int PUBLIC_USER_IDENTITY = 0;
	public static final int DISTINCT_PSI_ID = 1;
	public static final int WILDCARDED_PSI_ID = 2;
	
	public static final String IMS_SUBSCRIPTION = "IMSSubscription";
		// children - PrivateID (1) 
		//			  ServiceProfile (1 to n)
		
	public static final String INITIAL_FILTER_CRITERIA = "InitialFilterCriteria";
	// children - Priority (1)
	//            TriggerPoint (0 to 1)
	//            ApplicationServer (1)
	//            ProfilePartIndicator (0 to 1);
	
	public static final String LINE = "Line"; // string
	
	public static final String METHOD = "Method"; // string
	
	public static final String NAMESPACE = "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"D:\\CxDataType.xsd\"";
	
	public static final String PRIORITY = "Priority"; // integer >= 0
	
	public static final String PRIVATE_ID = "PrivateID"; // anyURI from RFC 2486
	
	public static final String PROFILE_PART_INDICATOR = "ProfilePartIndicator"; 
	// enumerated 0 (REGISTERED), 1 (UNREGISTERED)
	public static final int REGISTERED = 0;
	public static final int UNREGISTERED = 1;
	
	public static final String PUBLIC_IDENTITY = "PublicIdentity";
	// children - BarringIndication (0 to 1)
	//            Identity 1
	//            Extension (0 to 1)
	
	public static final String REGISTRATION_TYPE = "RegistrationType"; 
	// enumerated 0 (INITIAL_REGISTRATION), 1 (RE_REGISTRATION), 2 (DE-REGISTRATION)
	public static final int INITIAL_REGISTRATION = 0;
	public static final int RE_REGISTRATION = 1;
	public static final int DE_REGISTRATION = 2;
	
	public static final String REQUEST_URI = "RequestURI"; // string
	
	public static final String SERVER_NAME = "ServerName";
	
	public static final String SERVICE_INFO = "ServiceInfo"; // string
	
	public static final String SERVICE_PROFILE = "ServiceProfile";
	//	children - PublicIdentity (1 to n)
	//             InitialFilterCriteria (0 to n)
	//             CoreNetworkServicesAuthorization (0 to 1)
	//             Extension (0 to 1)
	
	public static final String SESSION_CASE = "SessionCase"; 
	// enumerated 0 (ORIGINATING_SESSION), 1 (TERMINATING_REGISTERED),
	//            2 (TERMINATING_UNREGISTERED), 3 (ORIGINATING_UNREGISTERED)
	public static final int ORIGINATING_SESSION = 0;
	public static final int TERMINATING_REGISTERED = 1;
	public static final int TERMINATING_UNREGISTERED = 2;
	public static final int ORIGINATING_UNREGISTERED = 3;
	
	public static final String SESSION_DESCRIPTION = "SessionDescription";
	// children - Line (1)
	//            Content (0 to 1)
	
	public static final String SHARED_IFC_SET_ID = "SharedIFCSetID"; 
	// integer >=0
	
	
	public static final String SIP_HEADER = "SIPHeader";
	// children - Header (1)
	//            Content (0 to 1)
	
	
	public static final String SPT = "SPT";
	// children - ConidtionNegated (0 to 1)
	//            Group (1 to n)
	// Choice of RequestURI, Method, SIPHeader, SessionCase, or SessionDescription (1)
	//            Extension (0 to 1);
	
	public static final String SUBSCRIBED_MEDIA_PROFILE_ID = "SubscribedMediaProfileId"; // integer >= 0
	
	public static final String TRIGGER_POINT = "TriggerPoint";
	// children - ConditionTypeCNF (1)
	//            SPT (1 to n)
	
	public static final String WILDCARDED_PSI = "WildcardedPSI"; // anyURI from TS 23.003
}
