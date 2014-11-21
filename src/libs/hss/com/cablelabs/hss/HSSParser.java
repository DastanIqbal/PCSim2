package com.cablelabs.hss;

import java.io.FileReader;
import java.io.IOException;
//import java.util.LinkedList;

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.*;

//import com.cablelabs.fsm.FSM;
import com.cablelabs.log.LogAPI;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.parser.PC2XMLException;

public class HSSParser extends DefaultHandler {

	/**
	 * The XML document reader and interface to the parser.
	 */
	private XMLReader xr;
	
	/**
	 * The locator class maintains the current location within
	 * the XML document for error reporting purposes.
	 */
	private Locator l;
	
	/**
	 * The console and log file interface.
	 */
	private LogAPI logger = null;
	
	/**
	 * The name of the file currently being parsed.
	 */
	private String fileName = null;
	
	/**
	 * A place holder for the current tag being parsed.
	 */
	private String curTag = null;
	
	/**
	 * A place holder for the current <PrivateID> tags.
	 */
	private Subscriber curSubscriber = null;
	
	/**
	 * A place holder for the current <SerivceProfile> tags.
	 */
	private ServiceProfile curProfile = null;
	
	/**
	 * A place holder for the current <PublicIdentity> tags.
	 */
	private PublicUserIdentity curPublicIdentity = null;
	
	/**
	 * A place holder for the current <InitialFilterCriteria> tags.
	 */
	private InitialFilterCriteria curIFC = null;
	
	/**
	 * A place holder for the current <TriggerPoint> tags.
	 */
	private TriggerPoint curTrigger = null;
	
	/**
	 * A place holder for the current <SPT> tags.
	 */
	private ServicePointTrigger curSPT = null;

	/**
	 * A place holder for the current <ApplicationServer> tags.
	 */
	private ApplicationServer curAS = null;
	
	/**
	 * This is a container to store characters (data) between XML
	 * elements. Since the parse may make more than one callout for
	 * data between elements, we need a container to accumulate all
	 * of the information for an element until the end tag for 
	 * the element is detected.
	 */
	private String partialChar = null;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "HSS";
	
	public HSSParser() {
		super();
		logger = LogAPI.getInstance(); // PC2Logger.getInstance(); 
	}
	
	public Subscriber parse(String fileName) throws SAXException, IOException {
		xr = XMLReaderFactory.createXMLReader();
		this.fileName = fileName;
		if (xr != null) {
			xr.setContentHandler(this);
			FileReader reader = new FileReader(this.fileName);
			xr.parse(new InputSource(reader));
			return curSubscriber;
		}
		else 
			throw new SAXException("XMLReader did not get created successfully.");
	}
	
	/**
	 * A method to set the document locator for error reporting.
	 */
	public void setDocumentLocator(Locator l) {
	    this.l = l;
	} 
	
	/**
	 * Parses the <PrivateID> element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
//	private void parsePrivateId(String qName, Attributes atts) throws SAXException {
//		if (curSubscriber == null) {
//			curSubscriber = new Subscriber();
//			if (atts.getLength() > 0) {
//				String msg = "";
//				for (int i=0; i<atts.getLength(); i++) {
//					String attr = atts.getLocalName(i);
//					msg += qName + " tag contains unhandled attribute(" + attr + ").\n";
//				}
//			}
//		}
//		else {
//			String msg =  qName + " can not be a child of " + qName + ".";
//			throw new PC2XMLException(fileName,msg, l);
//		}
//	}
	
	/**
	 * Parses the <PublicIdentity> element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parsePublicId(String qName, Attributes atts) throws SAXException {
		if (curSubscriber != null &&
				curProfile != null &&
				curPublicIdentity == null) {
			curPublicIdentity = new PublicUserIdentity(IdentityType.PUBLIC_USER_IDENTITY);
			if (atts.getLength() > 0) {
				String msg = "";
				for (int i=0; i<atts.getLength(); i++) {
					String attr = atts.getLocalName(i);
					msg += qName + " tag contains unhandled attribute(" + attr + ").\n";
				}
				logger.debug(PC2LogCategory.Diameter, subCat, "parsePulicId:" + msg);
			}
		}
		else {
			String msg =  qName + " can not be a child of " + qName + ".";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * Parses the <ServiceProfile> element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseServiceProfile(String qName, Attributes atts) throws SAXException {
		if (curProfile == null && curSubscriber != null) {
			curProfile = new ServiceProfile();
			if (atts.getLength() > 0) {
				String msg = "";
				for (int i=0; i<atts.getLength(); i++) {
					String attr = atts.getLocalName(i);
					msg += qName + " tag contains unhandled attribute(" + attr + ").\n";
				}
				logger.debug(PC2LogCategory.Diameter, subCat, "parsePulicId:" + msg);
			}
		}
		else {
			String msg =  qName + " can not be a child of " + qName + ".";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called by the parser upon initiating parsing allowing
	 * the application to create any variables or infrastructure it
	 * may need. The HSSParser simply logs that it is starting and 
	 * creates the final HSSData container.
	 */
	public void startDocument() throws SAXException {
		// handle a start-of-document event
		logger.info(PC2LogCategory.Diameter, subCat, "Starting to parse...");
		
	}
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called by the parser has completed parsing the document.
	 * This method verifies mandatory elements occur within the 
	 * document before return control back to the invoking class.
	 */
	public void endDocument() throws SAXException {

	}
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called at the start of each element and is were the 
	 * HSSParser begins to separate the various tags into their 
	 * respective objects. Validation of each mandatory attribute
	 * and children elements begins in this method.
	 * 
	 * (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
		
		logger.trace(PC2LogCategory.Diameter, subCat, "starting element (" + qName + ")");
		logger.trace(PC2LogCategory.Diameter, subCat, "   number of attributes: " + atts.getLength());
		curTag = qName;
		boolean unrecognizedTag = false;

		if (curSubscriber != null) {

			switch (qName.charAt(0)) {
			case 'A':
				if (qName.equals(IMSSubscriptionTags.APPLICATION_SERVER)) {
					if (curAS == null && curIFC != null) {
						curAS = new ApplicationServer();
					}
					else {
						String msg =  qName + " can not be a child of " + qName + ".";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else 
					unrecognizedTag = true;
				break;
			
			case 'B':
				if (qName.equals(IMSSubscriptionTags.BARRING_INDICATION)) {
					// Do nothing here, all of the work happens at the 
					// end of the tag.
				}
				
				else 
					unrecognizedTag = true;
				break;
				
			
			case 'C':
				if (qName.equals(IMSSubscriptionTags.CONDITION_TYPE_CNF) ||
						qName.equals(IMSSubscriptionTags.CONDITION_NEGATED) ||
						qName.equals(IMSSubscriptionTags.CONTENT)) {
					// Do nothing here, all of the work happens at the 
					// end of the tag.
				}
				else 
					unrecognizedTag = true;
				break;

			case 'D':
				if (qName.equals(IMSSubscriptionTags.DEFAULT_HANDLING)) {
					if (curAS != null) {
						String index = atts.getValue("index");
						if (index != null)
							curAS.setDefaultHandlingIndex(index);
					}
					else {
						String msg =  qName + " can not be a child of " + qName + ".";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else 
					unrecognizedTag = true;
				break;
			
			case 'G':
				if (qName.equals(IMSSubscriptionTags.GROUP)) {
					// Do nothing here, all of the work happens at the 
					// end of the tag.
				}
				else 
					unrecognizedTag = true;
				break;

			case 'H':	
				if (qName.equals(IMSSubscriptionTags.HEADER)) {
					// Do nothing here, all of the work happens at the 
					// end of the tag.
					//int glh = 0;
				}
				else 
					unrecognizedTag = true;
				break;
			
			case 'I':
				if (qName.equals(IMSSubscriptionTags.INITIAL_FILTER_CRITERIA)) {
					if (curIFC == null) {
						curIFC = new InitialFilterCriteria();
					}
					else {
						String msg =  qName + " can not be a child of " + qName + ".";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else if (qName.equals(IMSSubscriptionTags.IDENTITY)) {
					// Do nothing here, all of the work happens at the 
					// end of the tag.
				}
				
				else 
					unrecognizedTag = true;
				break;
				
			case 'M':
				if (qName.equals(IMSSubscriptionTags.METHOD)) {
					// Do nothing here, all of the work happens at the 
					// end of the tag.
				}

				else 
					unrecognizedTag = true;
				break;

			case 'P':
				if (qName.equals(IMSSubscriptionTags.PRIVATE_ID)) {
					// Do nothing here, all of the work happens at the 
					// end of the tag.
					// parsePrivateId(qName, atts);
				}
				else if (qName.equals(IMSSubscriptionTags.PUBLIC_IDENTITY)) {
					parsePublicId(qName, atts);
				}
				else if (qName.equals(IMSSubscriptionTags.PRIORITY) ||
						qName.equals(IMSSubscriptionTags.PROFILE_PART_INDICATOR)) {
					// Do nothing here, all of the work happens at the 
					// end of the tag.
				}
				else 
					unrecognizedTag = true;
				break;

			case 'S':
				if (qName.equals(IMSSubscriptionTags.SERVICE_PROFILE)) {
					parseServiceProfile(qName, atts);
				}
				else if (qName.equals(IMSSubscriptionTags.SPT)) {
					if (curTrigger != null && curSPT == null) {
						curSPT = new ServicePointTrigger();
					}
					else {
						String msg =  qName + " can not be a child of " + qName + ".";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else if (qName.equals(IMSSubscriptionTags.SIP_HEADER) ||
						qName.equals(IMSSubscriptionTags.SERVER_NAME) ||
						qName.equals(IMSSubscriptionTags.SERVICE_INFO)) {
					// Do nothing here, all of the work happens at the 
					// end of the tag.
				}
				else 
					unrecognizedTag = true;
				break;

			case 'T':
				if (qName.equals(IMSSubscriptionTags.TRIGGER_POINT)) {
					if (curIFC != null && curTrigger == null) {
						curTrigger = new TriggerPoint();
					}
					else {
						String msg =  qName + " can not be a child of " + qName + ".";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else 
					unrecognizedTag = true;
				break;

			default :
				throw new PC2XMLException(fileName,"Encountered unexpected tag(" + qName + ") element.", l);
			}
		}
		else if (qName.equals("IMSSubscription")) {
			curSubscriber = new Subscriber();
			return;
		}
		else {
			throw new PC2XMLException(fileName,"Encountered unexpected tag(" + qName + ") outside of fsm element.", l);
		}

		if (unrecognizedTag)
			throw new PC2XMLException(fileName,"Encountered unexpected tag(" + qName + ") element.", l);
	}

		

	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called when the parser detects the end tag for an element.
	 * The HSSParser addes the constructed object for the element into
	 * the resulting Subscriber.
	 * 
	 * (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 *
	 */
	public void endElement(String uri, String name, String qName) throws SAXException {
		logger.trace(PC2LogCategory.Diameter, subCat, "ending element (" + qName + ")");

		switch (qName.charAt(0)) {
		case 'A' :
			if (qName.equals(IMSSubscriptionTags.APPLICATION_SERVER)) {
				if (curIFC != null) {
					curIFC.setApplicationServer(curAS);
					curAS = null;
				}
			}
			break;
			
		case 'B' :
			if (qName.equals(IMSSubscriptionTags.BARRING_INDICATION)) {
				if (curPublicIdentity != null) {
					Integer value = resolveInteger();
					curPublicIdentity.setBarringIndication(value);
					partialChar = null;
				}
			}
			break;

		case 'C':
			if (qName.equals(IMSSubscriptionTags.CONDITION_TYPE_CNF)) {
				if (curTrigger != null) {
					Integer flag = resolveInteger();
					curTrigger.setConditionTypeCNF(flag);
					partialChar = null;
				}
			}
			else if (qName.equals(IMSSubscriptionTags.CONDITION_NEGATED)) {
				if (curSPT != null) {
					Integer flag = resolveInteger();
					curSPT.setConditionNegated(flag);
					partialChar = null;
				}
			}
			else if (qName.equals(IMSSubscriptionTags.CONTENT)) {
				if (curSPT != null) {
					if (partialChar != null) 
						curSPT.setContent(partialChar);
					partialChar = null;
				}
			}
			break;
		
		case 'D':
			if (qName.equals(IMSSubscriptionTags.DEFAULT_HANDLING)) {
				if (curAS != null) {
					Integer value = resolveInteger();
					if (value != null) {
						curAS.setDefaultHandling(value);
					}
					partialChar = null;
				}
				
			}
			break;
			
		case 'G':
			if (qName.equals(IMSSubscriptionTags.GROUP)) {
				if (curSPT != null) {
					Integer value = resolveInteger();
					if (value != null)
						curSPT.setGroup(value);
					partialChar = null;
				}
				
			}
			break;
		
		case 'H':
			if (qName.equals(IMSSubscriptionTags.HEADER)) {
				if (curSPT != null) {
					if (partialChar != null) 
						curSPT.setChoice(partialChar);
					partialChar = null;
				}
			}
			break;
			
		case 'I':
			if (qName.equals(IMSSubscriptionTags.IDENTITY)) {
				if (curPublicIdentity != null) {
					if (partialChar != null)
						curPublicIdentity.setIdentity(partialChar);
					partialChar = null;
				}
			}
			else if (qName.equals(IMSSubscriptionTags.INITIAL_FILTER_CRITERIA)) {
				if (curProfile != null) {
					curProfile.addIFC(curIFC);
					curIFC = null;
				}
			}
			break;
			
		case 'M':
			if (qName.equals(IMSSubscriptionTags.METHOD)) {
				if (curSPT != null) {
					if (partialChar != null) 
						curSPT.setChoice(partialChar);
					curSPT.setSPTType(SPTType.METHOD);
					partialChar = null;
				}
			}
			break;
			
		case 'P':
			if (qName.equals(IMSSubscriptionTags.PRIVATE_ID)) {
				if (partialChar != null) {
					curSubscriber.setPrivateUserId(partialChar);
					partialChar = null;
				}
			}
			else if (qName.equals(IMSSubscriptionTags.PUBLIC_IDENTITY)) {
				curProfile.add(curPublicIdentity);
				curPublicIdentity = null;
			}
			else if (qName.equals(IMSSubscriptionTags.PRIORITY)) {
				if (curIFC != null) {
					Integer value = resolveInteger();
					if (value != null)
						curIFC.setPriority(value);
					partialChar = null;
				}
				
			}
			else if (qName.equals(IMSSubscriptionTags.PROFILE_PART_INDICATOR)) {
				if (curIFC != null) {
					Integer value = resolveInteger();
					if (value != null)
						curIFC.setProfilePartIndicator(value);
					partialChar = null;
				}
				
			}
			break;
			
		case 'S':
			if (qName.equals(IMSSubscriptionTags.SERVICE_PROFILE)) {
				curSubscriber.addServiceProfile(curProfile);
				curProfile = null;
			}
			else if (qName.equals(IMSSubscriptionTags.SPT)) {
				if (curTrigger != null) {
					curTrigger.addServicePointTrigger(curSPT);
				}
				curSPT = null;
			}
			else if (qName.equals(IMSSubscriptionTags.SIP_HEADER)) {
				if (curSPT != null) {
					curSPT.setSPTType(SPTType.SIP_HEADER);
					partialChar = null;
				}
			}
			else if (qName.equals(IMSSubscriptionTags.SERVER_NAME)) {
				if (curAS != null) {
					if (partialChar != null) 
						curAS.setServerName(partialChar);
					partialChar = null;
				}
			}
			else if (qName.equals(IMSSubscriptionTags.SERVICE_INFO)) {
				if (curAS != null) {
					if (partialChar != null) 
						curAS.setServiceInfo(partialChar);
					partialChar = null;
				}
			}
			break;	
			
		case 'T':
			if (qName.equals(IMSSubscriptionTags.TRIGGER_POINT)) {
				if (curIFC != null) {
					curIFC.setTriggerPoint(curTrigger);
				}
				curTrigger = null;
			}
			break;
		}
		
		// Lastly clear the curTag holder
		curTag = null;
	}
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called for the charaters that lie between tags. Since the
	 * parse doesn't guarantee delivery of all of the characters between
	 * the start and end tags, the TSParser must be prepared to receive
	 * more than one callout before receiving notification of the ending
	 * tag.
	 * 
	 *  (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char ch[], int start, int length) throws SAXException {
		String data =  new String(ch,start,length);
		logger.trace(PC2LogCategory.Diameter, subCat, 
				curTag + " - CDATA: " + length + " characters. Data=[" + data + "].");
		
		if (curTag != null) {
			if (curTag.equals(IMSSubscriptionTags.BARRING_INDICATION) || 
					curTag.equals(IMSSubscriptionTags.IDENTITY) ||
					curTag.equals(IMSSubscriptionTags.PRIORITY) || 
					curTag.equals(IMSSubscriptionTags.CONDITION_TYPE_CNF) ||
					curTag.equals(IMSSubscriptionTags.CONDITION_NEGATED) || 
					curTag.equals(IMSSubscriptionTags.METHOD) ||
					curTag.equals(IMSSubscriptionTags.PRIVATE_ID) ||
					curTag.equals(IMSSubscriptionTags.LINE) ||
					curTag.equals(IMSSubscriptionTags.HEADER) ||
					curTag.equals(IMSSubscriptionTags.CONTENT) ||
					curTag.equals(IMSSubscriptionTags.SESSION_CASE) ||
					curTag.equals(IMSSubscriptionTags.SESSION_DESCRIPTION) ||
					curTag.equals(IMSSubscriptionTags.SERVER_NAME) ||
					curTag.equals(IMSSubscriptionTags.DEFAULT_HANDLING) ||
					curTag.equals(IMSSubscriptionTags.GROUP)) {
				if (partialChar == null) 
					partialChar = data;
				else
					partialChar += data;
				logger.trace(PC2LogCategory.Diameter, subCat, 
						curTag + " contains characters (" + partialChar + ").");
			}
			else  {
				logger.trace(PC2LogCategory.Diameter, subCat, 
						"Ignoring characters(" + data + ").");
			}
		}
		else  {
			logger.trace(PC2LogCategory.Diameter, subCat, 
					"Ignoring characters(" + data + ").");
		}
	}
	
//	private Boolean resolveBoolean() throws PC2XMLException {
//		if (partialChar != null) {
//			if (partialChar.length() > 1) {
//				if (partialChar.equals(IMSSubscriptionTags.FALSE))
//					return false;
//				else if (partialChar.equals(IMSSubscriptionTags.TRUE))
//					return true;
//			}
//			else {
//				String msg =  "HSSParser can not resolve boolean data because " +
//						"character data's length is greater than 1 data=(" 
//					+ partialChar + ").";
//				throw new PC2XMLException(fileName,msg, l);
//			}
//		}
//		else {
//			String msg =  "HSSParser can not resolve boolean data because character data is null.";
//			throw new PC2XMLException(fileName,msg, l);
//		}
//		return null;
//	}
	
	private Integer resolveInteger() throws PC2XMLException {
		if (partialChar != null) {
			try {
				Integer value = Integer.parseInt(partialChar);
				return value;
			}
			catch (NumberFormatException nfe) {
				String msg =  "HSSParser can not resolve integer data because character data is (" + partialChar + ").";
				throw new PC2XMLException(fileName,msg, l);
			}
			
		}
		else {
			String msg =  "HSSParser can not resolve boolean data because character data is null.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
}
