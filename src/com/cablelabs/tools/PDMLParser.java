/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.cablelabs.common.Conversion;
import com.cablelabs.fsm.ParserFilter;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.parser.PC2XMLException;

public class PDMLParser extends DefaultHandler {

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
	 * A place holder for the current <packet> tag being parsed.
	 */
	private Packet curPacket = null;

	/**
	 * A place holder for the current <protocol> tag being parsed.
	 */
	private Protocol curProtocol = null;

	/**
	 * A place holder for the current <field> tag being parsed.
	 */
	private Field curField = null;

	/**
	 * A place holder for the parent <field> tag of the current <field> tag being parsed.
	 */
	private Field parentField = null;

	/**
	 * The current database we are creating from the packets in the file.
	 */
	private PacketDatabase curDB = null;

	/**
	 * The name of the database that the user defined in the script.
	 */
	private String dbName = null;

	/**
	 * A container to control the hierarchy of the field structure
	 */
	private LinkedList<Field> stack = null;

	/**
	 * A place holder for the parent <protocol> tag of the current <protocol> tag being parsed.
	 */
	private Protocol parentProtocol = null;

	/**
	 * A place holder for the ancestor <protocol> tag of the current <protocol> tag being parsed.
	 * The first element in the ancestorProtocols should be the same as the parentProtocol.
	 */
	private LinkedList<Protocol> ancestorProtocols = new LinkedList<Protocol>();

	/**
	 * The filter to be applied to the packets as they are parsed.
	 */
	private ParserFilter filter = null;

	/**
	 * A flag indicating that the parser should be filtering on the IP address
	 * of the packet.
	 */
	private boolean filterIP = false;

	/**
	 * A flag indicating that the parser should be filtering on the protocol type
	 * of the packet.
	 */
	private boolean filterProtocol = false;

	/**
	 * A flag indicating that the parser should be filtering on the IP port
	 * of the packet.
	 */
	private boolean filterPort = false;

	/**
	 * A flag indicating that the parser should be filtering on the type
	 * of packet.
	 */
	private boolean filterMsgType = false;

	/**
	 * A flag indicating that the parser should be filtering any bootp protocol
	 * packet based upon the client MAC address field.
	 */
	private boolean filterClientMAC = false;

	/**
	 * This is a container to store characters (data) between XML
	 * elements. Since the parse may make more than one callout for
	 * data between elements, we need a container to accumulate all
	 * of the information for an element until the end tag for
	 * the element is detected.
	 */
//	private String partialChar = null;

	/**
	 * Flags indicating the current protocol being processed.
	 */
	private boolean bootp = false;
	private boolean dhcpv6 = false;
	private boolean dns = false;
	private boolean dns_request = false;
	private boolean dns_response = false;
	private boolean icmpv6 = false;
	private boolean pktc = false;
	private boolean kerberos = false;
	private boolean snmp = false;
	private boolean syslog = false;
	private boolean tftp = false;
	private boolean tod = false;
    private boolean dhcpOption = false;
    
	private String lastFrame = null;
	private String lastProto = null;
	private String lastField = null;

	/**
	 * The subcategory to use when logging
	 *
	 */
	private String subCat = "PDML";

	public PDMLParser() {
		super();
		logger = LogAPI.getInstance();
	}

	public PacketDatabase parse(String fileName, String dbName, ParserFilter filter) throws SAXException, IOException {
		xr = XMLReaderFactory.createXMLReader();
		this.fileName = fileName;
		this.dbName = dbName;
		this.filter = filter;
		resolveFilter();
		if (xr != null) {
			xr.setContentHandler(this);
			repair(new File(fileName));
			FileReader reader = new FileReader(this.fileName);
			try {
				xr.parse(new InputSource(reader));
			}
			catch (SAXException se) {
				logger.error(PC2LogCategory.Parser, subCat, "PDMLParser encountered an error in the file located in or near Frame["
						+ lastFrame + "], Proto[" + lastProto + "] and Field[" + lastField + "].\n" + se);
			}
			return curDB;
		}
		else
			throw new SAXException("XMLReader did not get created successfully.");
	}

	/**
	 * A method to set the document locator for error reporting.
	 */
	@Override
	public void setDocumentLocator(Locator l) {
	    this.l = l;
	}

	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called by the parser upon initiating parsing allowing
	 * the application to create any variables or infrastructure it
	 * may need. The HSSParser simply logs that it is starting and
	 * creates the final HSSData container.
	 */
	@Override
	public void startDocument() throws SAXException {
		// handle a start-of-document event
		stack = new LinkedList<Field>();
		logger.info(PC2LogCategory.Parser, subCat, "PDMLParser - Starting to parse " + fileName);
	
	}

	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called by the parser has completed parsing the document.
	 * This method verifies mandatory elements occur within the
	 * document before return control back to the invoking class.
	 */
	@Override
	public void endDocument() throws SAXException {

	}

	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called at the start of each element and is were the
	 * PDMLParser begins to separate the various tags into their
	 * respective objects. Validation of each mandatory attribute
	 * and children elements begins in this method.
	 *
	 * (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
	
	//	logger.debug(PC2LogCategory.Parser, subCat, "starting element (" + qName + ") number of attributes: " + atts.getLength());
		
		curTag = qName;
		boolean unrecognizedTag = false;

		if (curDB != null) {

			switch (qName.charAt(0)) {
			case 'f':
				if (qName.equals(PDMLTags.FIELD)) {
					if (curField != null) {
						// This means we are about to start a subField for
						// an existing field. Add the curField to the stack.
						stack.addFirst(curField);
						parentField = curField;
						curField = null;
					}
					parseField(qName, atts);
				}
				else
					unrecognizedTag = true;
				break;
		
			case 'p':
				if (qName.equals(PDMLTags.PROTO)) {
					parseProto(qName, atts);
				}
				else if (qName.equals(PDMLTags.PACKET)) {
					parsePacket(qName, atts);
				}
				else
					unrecognizedTag = true;
				break;

			default :
				throw new PC2XMLException(fileName,"Encountered unexpected tag(" + qName + ") element.", l);
			}
		}
		else if (qName.equals("pdml")) {
			String tool = atts.getValue("creator");
			curDB = new PacketDatabase(tool, dbName, filter);
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
	 * The PDMLParser addes the constructed object for the element into
	 * the resulting PacketDatabase.
	 *
	 * (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 *
	 */
	@Override
	public void endElement(String uri, String name, String qName) throws SAXException {
		switch (qName.charAt(0)) {
		case 'f' :
			if (qName.equals(PDMLTags.FIELD)) {
				if (curField != null) {
					if (parentField != null) {
						
						// Wireshark 1.7.1 dhcpv6 messages, Option6, contains multiple fields (Options)
						// where the names field is "Requested Option code."  We need to distinguish 
						// between these files to retrieve each Option separately.
						if (parentField.getName().equals("Option6")) {
							if (curField.getName().equals("Requested Option code")) {
//								logger.debug(PC2LogCategory.Parser, subCat, "dhcpv6 Option6 SubOption generation: " + getSubOption(curField.getShow()));
								curField.setName(getSubOption(curField.getShow()));
							}
						}
						
						// 5-27-11, botte, PCPCSII-144 
						if (curField.getName().contains("asd.snmp")) {
							String nameToken = null;
							StringTokenizer crFieldTokens = new StringTokenizer(curField.getName(), ".");
							while (crFieldTokens.hasMoreTokens())
								nameToken = crFieldTokens.nextToken();
							curField.setName(nameToken);
							logger.trace(PC2LogCategory.Parser, subCat, "Modifying Field.name before adding Field to Parent."); 
						}

						parentField.addSubField(curField);
						logger.trace(PC2LogCategory.Parser, subCat,
								"Adding subfield =" + curField.getName() + " to parent = " + parentField.getName());
//						logger.info(PC2LogCategory.Parser, subCat,
//								"Adding subfield (curField to parentField) =" + curField.abbreviatedString());
					}
					else if (parentField == null &&
							curProtocol != null &&
							stack.size() == 0) {
						
						curProtocol.addField(curField);
						logger.trace(PC2LogCategory.Parser, subCat,
								"Adding field =" + curField.getName() + " to protocol = " + curProtocol.getName());
//						logger.info(PC2LogCategory.Parser, subCat,
//								"Adding field (curField to curProtocol) =" + curField.abbreviatedString());
					}
					curField = null;
				}
				else if (parentField != null && stack.size() > 1) {
					// With Wireshark version 1.7.1, the vendor specific option has now nested all of the suboption
					// under the enterprise field, rearrange all of the data so that enterprise is a field for the
					// message an all of its sub fields become a field on the option field.
					if (bootp && parentField.getName().equals("enterprise")) {
						if (parentField.hasSubFields()) {
							stack.removeFirst();
							Field grandParent = stack.getFirst();
							ListIterator<Field> iter = parentField.subFields.listIterator();
							while (iter.hasNext()) {
								Field f = iter.next();
									grandParent.addSubField(f);
							}
							parentField.clearSubFields();
							parentField = grandParent;
						}
										
					}
					else if (curProtocol != null && curProtocol.getName().equals("kerberos")) {
						if (parentField.hasSubFields()) {
							stack.removeFirst();
							Field grandParent = stack.getFirst();
							ListIterator<Field> iter = parentField.subFields.listIterator();
							while (iter.hasNext()) {
								Field f = iter.next();
									grandParent.addSubField(f);
							}
							parentField.clearSubFields();
							parentField = grandParent;
						}
					}
					else {
					//curProtocol.addField(parentField);
					logger.trace(PC2LogCategory.Parser, subCat,
							"Adding field =" + parentField.getName() + " to protocol = " + curProtocol.getName());

                    // Next we need to handle the nesting of fields within the xml document 
					// so take the current parent and add it as a sub field to the grandparent
					stack.removeFirst();
					Field grandparent = stack.getFirst();
					grandparent.addSubField(parentField);
					parentField = grandparent; // stack.getFirst();
					}
					
					// Next if we have reached the top field in the stack
					// We need to add it to the protocol 
					if (stack.size() == 1) {
						curProtocol.addField(parentField);
					}
					
				}
				else if (parentField != null &&
						curProtocol != null &&
						stack.size() == 1) {

					curProtocol.addField(parentField);
					logger.trace(PC2LogCategory.Parser, subCat,
							"Adding field =" + parentField.getName() + " to protocol = " + curProtocol.getName());
//					logger.info(PC2LogCategory.Parser, subCat,
//							"Adding field (parentField to curProtocol 2) =" + parentField.abbreviatedString());
					stack.removeFirst();
					parentField = null;
					dhcpOption = false;
				}
			}
			break;
		
		case 'p' :
			if (qName.equals(PDMLTags.PROTO)) {
				if (curProtocol != null) {
					if (curPacket != null) {
						if (curProtocol.name.equals(PDMLTags.GENINFO_PROTOCOL)) {
							updatePacketForGenInfo();
							curPacket.setGenInfo(curProtocol);
						}
						else if (curProtocol.name.equals(PDMLTags.FRAME_PROTOCOL)) {
							curPacket.setFrameProtocol(curProtocol);
						}
						else if (curProtocol.name.equals(PDMLTags.SNMP_PROTOCOL)) {
							curProtocol.collapseSNMPFields();
							curPacket.addProtocol(curProtocol);
						}
						else {

							// Now we need to see if there is there is a parentProtocol.
							
							// botte, 09/11/2012, Wireshark truncates AS-REQ packet,
							// and PDML extraction does not add closing <\proto> tag, 
							// and 'short' protocol contains no relevant data.
							if (curProtocol.getName().equals("short")) {
								// 
							}		
							else if (parentProtocol != null) {
								parentProtocol.addSubProtocol(curProtocol);
								clearProtocolFlag(curProtocol.protocol);
							}
							else {
								if (!curProtocol.getName().equals("fake-field-wrapper") 
										&& !curProtocol.getName().equals("short")) {
									curPacket.addProtocol(curProtocol);
//									logger.info(PC2LogCategory.Parser, subCat,
//											"Adding protocol=" + curProtocol.abbreviatedString());
								}
							}
						}
					}
					else {
						throw new PC2XMLException(fileName,"Processing ending tag (" + qName + ") when the curPacket is null.", l);
					}
				}
				else {
					throw new PC2XMLException(fileName,"Processing ending tag (" + qName + ") when the curProtocol is null.", l);
				}
				curProtocol = null;

				if (parentProtocol != null) {
					curProtocol = parentProtocol;
					ancestorProtocols.removeFirst();
					if (ancestorProtocols.size() > 0)
						parentProtocol = ancestorProtocols.getFirst();
					else
						parentProtocol = null;
				}
			}
			else if (qName.equals(PDMLTags.PACKET)) {
				if (curPacket != null) {
					if (curDB != null && acceptPacket(curPacket)) {
						curDB.addPacket(curPacket);
						logger.debug(PC2LogCategory.Parser, subCat,
								"Adding " + curPacket.detailString()
								+ " to DB=" + curDB.getName());
					}
				}
				curPacket = null;
				clearProtocolFlags();
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
	 * the start and end tags, the PDMLParser must be prepared to receive
	 * more than one callout before receiving notification of the ending
	 * tag.
	 *
	 *  (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char ch[], int start, int length) throws SAXException {
		String data =  new String(ch,start,length);
		logger.trace(PC2LogCategory.Parser, subCat,
				curTag + " - CDATA: " + length + " characters. Data=[" + data + "].");
	
//		if (curTag != null) {
////			logger.trace(PC2LogCategory.Parser, subCat,
////						"Ignoring characters(" + data + ").");
//		}
//		else  {
////			logger.trace(PC2LogCategory.Parser, subCat,
////					"Ignoring characters(" + data + ").");
//		}
	}

	private void clearProtocolFlag(String protocol) {
		if (protocol.equals(PDMLTags.BOOTP_PROTOCOL))
				bootp = false;
		else if (protocol.equals(PDMLTags.DHCPv6_PROTOCOL)) {
			dhcpv6 = false;
		}
		else if (protocol.equals(PDMLTags.DNS_PROTOCOL)) {
			dns = false;
			dns_request = false;
			dns_response = false;
		}
		else if (protocol.equals(PDMLTags.PACKET_CABLE_PROTOCOL))
			pktc = false;
		else if (protocol.equals(PDMLTags.KERBEROS_PROTOCOL))
			kerberos = false;
		else if (protocol.equals(PDMLTags.SNMP_PROTOCOL))
			snmp = false;
		else if (protocol.equals(PDMLTags.TFTP_PROTOCOL))
			tftp = false;
		else if (protocol.equals(PDMLTags.TOD_PROTOCOL))
			tod = false;
//		else if (protocol.equals(PDMLTags.IP_PROTOCOL))
//			ip = false;
		else if (protocol.equals(PDMLTags.SYSLOG_PROTOCOL))
		    syslog = false;
	}

	private void clearProtocolFlags() {
		bootp = false;
		dhcpv6 = false;
		dns = false;
		dns_request = false;
		dns_response = false;
		kerberos = false;
		pktc = false;
		snmp = false;
		tftp = false;
		tod = false;
		syslog = false;
	}

	/**
	 * This method applies the filter criteria, if there is any,
	 * to the current packet.
	 * @param p
	 * @return true if it should be included in the database, false otherwise.
	 */
	private boolean acceptPacket(Packet p) {
		if (filter != null) {
			// See if we are filtering on protocol first
			if (filterProtocol) {
				if (!p.getProtocol().equals(filter.getProtocol()))
						return false;
			
				// Next see if we have a msgType to filter
				if (filterMsgType && !p.getName().equals(filter.getMsgType()))
					return false;
			
				// Lastly see if we have a clientMacAddr to filter
				if (filterClientMAC && (p.getClientMacAddr() == null ||
						(p.getClientMacAddr() != null &&
						!p.getClientMacAddr().equals(filter.getClientMacAddr()))))
					return false;
			}
		
			if (filterIP) {
				if (filter.isSrcOnly() && !p.getSrcAddr().equals(filter.getIp()))
					return false;
				else if (filter.isDstOnly() && !p.getDstAddr().equals(filter.getIp()))
					return false;
				else if (!filter.getIp().equals(p.getSrcAddr()) &&
						!filter.getIp().equals(p.getDstAddr()))
					return false;
			}
		
			if (filterPort) {
				if (filter.isSrcOnly() && !p.getSrcPort().equals(filter.getPort()))
					return false;
				else if (filter.isDstOnly() && !p.getDstPort().equals(filter.getPort()))
					return false;
				else if (!filter.getPort().equals(p.getSrcPort()) &&
						!filter.getPort().equals(p.getDstPort()))
					return false;
			}
		}
		return true;
	}
	

	/**
	 * The field name for DNS message is often "", therefore this
	 * function helps to try and assign a name to the field so that
	 * the information can be accessed by the scripts. The rules are
	 * fairly simple, we look at the show attribute and apply the following
	 * rules:
	 * 		if show = "Queries" set the name to qry
	 * 		else if show = "Answers" set the name to resp
	 * 		else if show starts with "Authoritative ..." set the name to auth
	 * 		else if show starts with "Additional ..." set the name to addl
	 * 		else if show has a ':' in the field
	 * 			 set the name to everything preceding the ':' concatenated with the parent
	 * 				with any '.' converted to '-'
	 * 				(e.g. 'Name server: 10.32.0.195' becomes 'Name server')
	 * 				except where the value ends in .com then we
	 * 				use the keyword element
	 * 				(e.g. 'pact20.redlab.com: type A, class IN, addr 10.32.0.195' becomes 'element')
	 * 		
	 * 		else
	 * 			use show
	 *
	 * @param show
	 * @return
	 */
	private String getDNSFieldName(String show) {
//		if (show.equals(PDMLTags.DNS_QUERIES))
//			return PDMLTags.DNS_QRY;
//		else if (show.equals(PDMLTags.DNS_ANSWER))
//			return PDMLTags.DNS_RESP;
//		else if (show.startsWith(PDMLTags.DNS_AUTHORITATIVE))
//			return PDMLTags.DNS_AUTH;
//		else if (show.startsWith(PDMLTags.DNS_ADDITIONAL))
//			return PDMLTags.DNS_ADDL;
		if (show.equals(PDMLTags.DNS_QUERIES) ||
				show.equals(PDMLTags.DNS_ANSWER) ||
				show.startsWith(PDMLTags.DNS_AUTHORITATIVE) ||
				show.startsWith(PDMLTags.DNS_ADDITIONAL))
			return show;
		
		else if (parentField != null &&
					(parentField.name.equals(PDMLTags.DNS_QUERIES) ||
						parentField.name.equals(PDMLTags.DNS_ANSWER) ||
						parentField.name.startsWith(PDMLTags.DNS_AUTHORITATIVE) ||
						parentField.name.startsWith(PDMLTags.DNS_ADDITIONAL)))	{
			int colon = show.indexOf(":");
			if (colon != -1) {
				String tmp = show.substring(0, colon);
				if (tmp.endsWith(".com") ||
						tmp.endsWith(".net") ||
						tmp.endsWith(".COM") ||
						tmp.endsWith(".NET")) {
					if (parentField.name.equals(PDMLTags.DNS_QUERIES))
					return PDMLTags.DNS_QRY;
				else if (parentField.name.equals(PDMLTags.DNS_ANSWER))
					return PDMLTags.DNS_RESP;
				else if (parentField.name.startsWith(PDMLTags.DNS_AUTHORITATIVE))
					return PDMLTags.DNS_AUTH;
				else if (parentField.name.startsWith(PDMLTags.DNS_ADDITIONAL))
					return PDMLTags.DNS_ADDL;
				}
			}
		}
		else if (parentField != null &&
				(parentField.name.equals(PDMLTags.DNS_QRY) ||
					parentField.name.equals(PDMLTags.DNS_RESP) ||
					parentField.name.equals(PDMLTags.DNS_AUTH) ||
					parentField.name.equals(PDMLTags.DNS_ADDL)))	{
			int colon = show.indexOf(":");
			if (colon != -1) {
				String tmp = show.substring(0, colon);
				return tmp;
			}
		}
	
		return show;	
	}

	/**
	 * The field name for Kerberos message is quite complex, therefore this
	 * function helps to try and assign a name to the field so that
	 * the information can be accessed by the scripts. The rules are
	 * fairly simple, we look at the name attribute and apply the following
	 * rules:
	 * 		test if the field begins with the protocol value kerberos
	 * 			if yes remove it and the remainder after the '.' is the name
	 * 			else
	 * 				the entire name attribute is the name
	 * 	
	 * @param xmlname
	 * @return
	 */
	private String getKerberosFieldName(String xmlname, int offset) {
		if (kerberos) {
			if (xmlname.startsWith(PDMLTags.KERBEROS_PROTOCOL)) {
				return xmlname.substring(offset+1);
			}
		}
		else if (pktc) {
			if (xmlname.startsWith(PDMLTags.PACKET_CABLE_PROTOCOL)) {
				return xmlname.substring(offset+1);
			}
		}
		return xmlname;	
	}

	private String getMsgName(String xmlname, String name, String show, String showname) {
//  BKPT
//		if (bootp && showname.equals("DHCP: Discover (1)")) {
//        	int glh=0;
//        }
//		
		if (bootp &&
				xmlname != null &&
				showname != null &&
				xmlname.equals(PDMLTags.BOOTP_MSG_TYPE_FIELD) &&
				showname.contains(PDMLTags.BOOTP_MSG_TYPE_SHOWNAME)) {		
			// There have been changes to the Wireshark output. The newer versions starting with 1.7.1 
			// put the message type in the showname attribute instead of show. Update logic to look in 
			// showname first and show second.
				int offset = showname.indexOf(PDMLTags.BOOTP_MSG_TYPE_SHOWNAME);
				if (offset != -1) {
					offset += PDMLTags.BOOTP_MSG_TYPE_SHOWNAME.length();
					int end = showname.indexOf(' ', offset);
					if (end != -1) {
						return showname.substring(offset, end);
					}
				}	

			
			}
			else if (bootp &&
					xmlname != null &&
					show != null &&
					xmlname.equals("")  && 
					show.contains(PDMLTags.BOOTP_MSG_TYPE)) {	
						int offset = show.indexOf(PDMLTags.BOOTP_MSG_TYPE);
						if (offset != -1) {
							offset += PDMLTags.BOOTP_MSG_TYPE.length();
							return show.substring(offset);

						}

		}
		else if (dhcpv6 &&
				xmlname != null &&
				showname != null &&
				xmlname.equals("dhcpv6.msgtype") &&
				showname.startsWith(PDMLTags.RELAY_MSG_TYPE)) {
			String str = null;
			StringTokenizer token = new StringTokenizer(showname);
			while (token.hasMoreTokens()) {
				str = token.nextToken();
				if (str.contains(":")) {
					str = token.nextToken();
					if (str.equals("Relay-forw") || 
							str.equals("Relay-reply"))
							str = PDMLTags.DHCPv6_RELAY;
					break;
				}
			}
			return str;
		}

		else if (icmpv6 &&
				xmlname != null &&
				showname != null &&
				xmlname.equals("icmpv6.type") &&
				showname.startsWith(PDMLTags.ICMPV6_MSG_TYPE)) {
			String str = null;
			int endIndex = showname.indexOf('(');
			if (endIndex > 0)
				str = showname.substring((PDMLTags.ICMPV6_MSG_TYPE.length()), endIndex-1);
			else
				str = showname.substring(PDMLTags.ICMPV6_MSG_TYPE.length());
				
			
			return str;
		}
		// Use the field with the name set to dns.flags.response for DNS
		// to determine if the message is a request or response
		else if (dns &&
				xmlname != null &&
				show != null) {
			if (!dns_request && !dns_response){
				if (xmlname.equals(PDMLTags.DNS_MSG_TYPE_RESPONSE_FIELD)) {
					if (show.equals("0"))
						//dns_request = true;
						return PDMLTags.DNS_QUERY;
					else if (show.equals("1"))
						//dns_response = true;
						return PDMLTags.DNS_RESPONSE;
				}
			}

		}
		else if ((kerberos || pktc) &&
				xmlname != null &&
				showname != null &&
				xmlname.equals(PDMLTags.KERBEROS_MSG_TYPE_FIELD) &&
				showname.contains(PDMLTags.KERBEROS_MSG_TYPE)) {
			int offset = showname.indexOf(PDMLTags.KERBEROS_MSG_TYPE);
			if (offset != -1) {
				offset += PDMLTags.KERBEROS_MSG_TYPE.length();
				int end = showname.indexOf(" ", offset);
				if (end != -1) {
					return showname.substring(offset, end);
				}
			
			
			}
		}
		// Use the field with the name set to snmp.data for the SNMP
		// Message Name
		else if (snmp &&
				xmlname != null &&
				show != null &&
				xmlname.equals(PDMLTags.SNMP_MSG_TYPE_FIELD)) {
			if (show.equals(PDMLTags.SNMP_MSG_TYPE_2))
				return PDMLTags.GET_RESPONSE;
			else if (show.equals(PDMLTags.SNMP_MSG_TYPE_3))
				return PDMLTags.SET_REQUEST;
			else if (show.equals(PDMLTags.SNMP_MSG_TYPE_6))
				return PDMLTags.INFORM_REQUEST;
                        else if (show.equals(PDMLTags.SNMP_MSG_TYPE_7))
                                return PDMLTags.TRAP;
		}
		// Use the field with the name set to snmp.data for the SNMP
		// Message Name
		else if (syslog &&
				xmlname != null &&
				show != null &&
				xmlname.equals(PDMLTags.SYSLOG_PROTOCOL)) {
			if (showname.startsWith("Syslog message:")) {
				return PDMLTags.SYSLOG_MSG_TYPE;
			}
		}
		// Use the field with the name set to tftp.opcode for the TFTP
		// Message Name
		else if (tftp &&
				xmlname != null &&
				show != null &&
				xmlname.equals(PDMLTags.TFTP_MSG_TYPE_FIELD)) {
			if (show.equals(PDMLTags.TFTP_MSG_TYPE_1))
				return PDMLTags.READ_REQUEST;
			else if (show.equals(PDMLTags.TFTP_MSG_TYPE_3))
				return PDMLTags.DATA_PACKET;
			else if (show.equals(PDMLTags.TFTP_MSG_TYPE_4))
				return PDMLTags.ACKNOWLEDGEMENT;
			else
				return PDMLTags.TFTP_PROTOCOL;
		
		}
	
		// Use the field with the name set to "" for the TOD
		// Message Name
		else if (tod &&
				xmlname != null &&
				show != null &&
				xmlname.equals("") &&
				show.startsWith(PDMLTags.TOD_MSG_TYPE)) {
			int offset = show.indexOf(PDMLTags.TOD_MSG_TYPE);
			if (offset != -1) {
				offset += PDMLTags.TOD_MSG_TYPE.length();
				return show.substring(offset);
			
			}
		}
		return null;
	}

	private String getOption(String show) {
		// If there is a parent field, then this should
		// be a suboption we are processing.
		if (parentField != null) {

			int index = show.indexOf(" ");
			if (index != -1) {
				String so = show.substring(0, index);
				if (so.equals("0")) {
					//int sfs = parentField.getSubFieldSize() + 1;
					return "padding"; // + sfs;
				}
				else if (so.startsWith("0x")) {
					if (so.endsWith(":"))
						so = so.substring(2, so.length()-1);
					else
						so = so.substring(2);
					//byte b = Byte.parseByte(so);
					int x = Integer.valueOf(so, 16).intValue();
					return "suboption" + x;
				}
				else if (so.equalsIgnoreCase("suboption")) {
					index++;
					int colon = show.indexOf(":", index);
					if (colon != -1)
						return "suboption" + show.substring(index, colon);
				}
				else if (show.contains(" = ")) {
					int equal = show.indexOf('=');
					if (equal != -1) {
						// Now look for a space after the one following the equal sign
						int space = show.indexOf(' ', equal+2);
						if (space != -1) {
							return show.substring(equal+2, space);
						}
						else
							return show.substring(equal+2);
					}
				}
				else {
					try {
						int value = Integer.parseInt(so);
						return "suboption" + value;
					}
					catch (NumberFormatException nfe) {
						int colon = show.indexOf(":");
						if (colon != -1)
							return show.substring(0, colon);
						else
							return show;
					}
				}
			}

		}
		else {
			int index = show.indexOf("(t=");
			int index2 = show.indexOf(PDMLTags.BOOTP_OPTION_VALUE);
			if (index != -1) {
				index +=3;
				int comma = show.indexOf(",", index);
				if (comma != -1)
					return "Option" + show.substring(index, comma);
			}
			else if (index2 != -1){
				index2 += PDMLTags.BOOTP_OPTION_VALUE.length();
				int openPar = show.indexOf('(', index2);
				int closePar = show.indexOf(')',index2);
				if (openPar != -1 && 
						closePar != -1 &&
						openPar < closePar) {
					return "Option" + show.substring(openPar+1, closePar);
				}
			}
			else
				return show;
		}
	
		return "OptionX";
	}


	private String getV6Option(String show) {
		if (show.contains("Suboption:")) {
		// SubOption
			String tmpStr = null;
			StringTokenizer token = new StringTokenizer(show);
//			for (int i=0; i<2; i++)
//				tmpStr = token.nextToken();
//			String rtnStr = tmpStr.substring(0,tmpStr.length()-1);
			while (token.hasMoreElements()) {
				tmpStr = token.nextToken();
				if (tmpStr.startsWith("("))
					tmpStr = tmpStr.substring(1,tmpStr.length()-1);
			}
			return "suboption" + tmpStr;
		}

		// Part of the Option that is not a SubOption
		if (show.contains(":"))
			return show.substring(0, show.indexOf(":"));
		else
			return show;
	}
	
	/** 
	 * Find the name to use for the Field of a DHCP v4 tag
	 * 
	 * @param xmlname - the name attribute from the xml data
	 * @param show - the show attribute from the xml data
	 * @param showname - the showname attribute from the xml data
	 */
	private String getDHCPName(String xmlname, String show, String showname) {
		String tmp = xmlname;
		if (tmp.startsWith("bootp.")) {
			tmp = tmp.replaceFirst("bootp.", "");
			if (tmp.startsWith("option.")) {
				tmp = tmp.replaceFirst("option.", "");
				if (tmp.contains(".")) {
					int index = tmp.lastIndexOf(".");
					tmp = tmp.substring(index+1);
				}
				
			    
				// List of exceptions to search for suboption
				if (!tmp.equals("enterprise")) {

					tmp = getDHCPSuboption(tmp, showname);
				
				}
			}
			
			else {
				String value = showname;
				if (value == null) {
					value = show;
				}
				tmp = getDHCPSuboption(tmp, value);
			}
			
		}
		else if (parentField != null && tmp.contains(".")) {
			int index = tmp.lastIndexOf(".");
			tmp = tmp.substring(index+1);
		}
		return tmp;
	}
	
	/**
	 * Attempts to resolve the suboption value to use in a DHCP v4 field tag
	 * 
	 * @param name
	 * @param show
	 * 
	 * @return value to use for the name of the field
	 */
	private String getDHCPSuboption(String name, String show) {

		// Wireshark has changed the values used to determine a suboption in version 1.7 so need to 
		// evaluate the formating
		if (show.startsWith("Option ")) {
			return getSubOption(show);
		}
		else {
			if (show.contains("0x")) {
				int index = show.indexOf("0x");
				if (index != -1) {
					String so = show.substring(index+2, index+4);
					int x = Integer.valueOf(so, 16).intValue();
					return "suboption" + x;
				}
			}
			else {
				if (show.contains(" = ")) {
					int equal = show.indexOf('=');
					if (equal != -1) {
						int space = show.indexOf(' ', equal);
						if (space != -1) {
							return show.substring(equal+2, space);
						}
						else
							return show.substring(equal+2);
					}
				}
				else if (name.equals("request_list_item")){
					String tmp = getSubOption(show);
					if (!tmp.equals(show))
						return tmp;
				}
			}
		}
	
		return name;
	}

	/**
	 * Looks through the value to see if there is a number in parenthesis to use as 
	 * the suboption value
	 * 
	 * @param show
	 * @return the value to use 
	 * 
	 */
	private String getSubOption(String show) {
	 
		int openPar = show.indexOf('(');
		int closePar = show.indexOf(')');
		if (openPar != -1 && 
				closePar != -1 &&
				openPar < closePar) {
			return "suboption" + show.substring(openPar+1, closePar);
		}

		return show;
	}

	/**
	 * The field name for PKTC message is occasionally "", therefore this
	 * function helps to try and assign a name to the field so that
	 * the information can be accessed by the scripts. The rules are
	 * fairly simple, we look at the show attribute and apply the following
	 * rules:
	 * 		get the substring to the first space
	 * 		remove any final : from the substring
	 *
	 * @param show
	 * @return
	 */
	private String getPKTCFieldName(String show) {
		int space = show.indexOf(" ");
		if (space != -1) {
			if (show.charAt(space-1) == ':')
				space--;
			String tmp = show.substring(0, space);
			return tmp;
		}
	
		return show;	
	}

	/**
	 * The field name for SNMP message is often "", therefore this
	 * function helps to try and assign a name to the field so that
	 * the information can be accessed by the scripts. The rules are
	 * fairly simple, we look at the show attribute and apply the following
	 * rules:
	 * 		get the substring to the first space
	 * 		remove any final : from the substring
	 *
	 * @param show
	 * @return
	 */
	private String getSNMPFieldName(String show) {
		int space = show.indexOf(" ");
		if (space != -1) {
			if (show.charAt(space-1) == ':')
				space--;
			String tmp = show.substring(0, space);
			tmp = tmp.replaceAll("[.]", "-");
			return tmp;
		}
	
		return show;	
	}
	/**
	 * Parses the <field> element.
	 *
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseField(String qName, Attributes atts) throws SAXException {
		
		if (curField == null) {	
			String xmlname = atts.getValue(PDMLTags.NAME_ATTR);
			String showname = atts.getValue(PDMLTags.SHOW_NAME_ATTR);
			if (showname == null)
				showname = atts.getValue(PDMLTags.SHOW_NAME_ATTR2);
			String size = atts.getValue(PDMLTags.SIZE_ATTR);
			String show = atts.getValue(PDMLTags.SHOW_ATTR);
			String value = atts.getValue(PDMLTags.VALUE_ATTR);
			String pos = atts.getValue(PDMLTags.POS_ATTR);
			String hide = atts.getValue(PDMLTags.HIDE_ATTR);
			String unmasked = atts.getValue(PDMLTags.UNMASKED_VALUE_ATTR);

			if (logger.isTraceEnabled(PC2LogCategory.Parser, subCat)) {
				String msg = "Parsing line <" + qName;
				if (xmlname != null)
					msg += " name=\"" + xmlname + "\"";
				if (showname != null)
					msg += " showname=\"" + showname + "\"";
				if (hide != null)
					msg += " hide=\"" + hide + "\"";
				if (size != null)
					msg += " size=\"" + size + "\"";
				if (pos != null)
					msg += " pos=\"" + pos + "\"";
				if (show != null)
					msg += " show=\"" + show + "\"";
				if (value != null)
					msg += " value=\"" + value + "\"";
				if (unmasked != null)
					msg += " unmaskedvalue=\"" + unmasked + "\"";
				msg += ">";
				
				logger.trace(PC2LogCategory.Parser, subCat, msg);
			}

			// Each field tag must have a name field.
			if (xmlname != null) {
				// Next we need to divide the xmlname into the
				// actual protocol and field names
// BRKPT
//				if (xmlname.equals("bootp.option.type")) {
//					int glh = 0;
//				}
//				else if (xmlname.equals("")) {
//					int glh = 0;
//				}
			
				// The format for a field may be something like
				//   <field name="ip.flags.rb"  ....
				// We want the protocol to be the first element and
				// the field name to be the last element of the name
				int protoIndex = xmlname.indexOf(".");
				int fieldIndex = xmlname.lastIndexOf(".");

				String protocol = null;
				String name = null;
				if (protoIndex != -1 &&
						fieldIndex != -1) {

					if (parentField != null) {						
						if (xmlname.equals("dhcpv6.option.type")) {
							// botte, 08/21/2012, dhcpv6.Option9 is not really an Option, it's a Relay message
							if (show.equals("9"))
								parentField.setName("Relay");
							else
								parentField.setName("Option" + show);
						}
					}

					protocol = curProtocol.protocol; 

					name = xmlname.substring(fieldIndex+1);
					if (kerberos || pktc) {
						name = getKerberosFieldName(xmlname, protoIndex);
					}
					// DHCP v4 fields now contain a value in many of the name attributes so
					// we need to process them here starting with version 1.7.1 of Wireshark
					else if (bootp) {
						if (xmlname.contains(PDMLTags.BOOTP_OPTION_TYPE_FIELD)) {
							name = getOption(showname);
							dhcpOption = true;

						}
						else if (dhcpOption) {
							name = getDHCPName(xmlname, show, showname);
						}
					}
					else if (dhcpv6) {
						if (xmlname.equals("dhcpv6.cablelabs.opt")) {
							String tmpStr = null;
							StringTokenizer token = new StringTokenizer(show);
							while (token.hasMoreElements()) {
								tmpStr = token.nextToken();
								if (tmpStr.startsWith("("))
									tmpStr = tmpStr.substring(1,tmpStr.length()-1);
							}
							name = "suboption" + tmpStr;
						}
						else if (xmlname.equals("dhcpv6.docsis.cccV6.suboption") ||
								xmlname.equals("dhcpv6.docsis.cccV6.tlv5.suboption") ||
								xmlname.equals("dhcpv6.packetcable.cccV6.tlv5.suboption")) {
							// SubOption
							String tmpStr = null;
							StringTokenizer token = new StringTokenizer(show);
							while (token.hasMoreElements()) {
								tmpStr = token.nextToken();
								if (tmpStr.startsWith("("))
									tmpStr = tmpStr.substring(1,tmpStr.length()-1);
							}
							name = "suboption" + tmpStr;
						}
					}
				}
				else {
					protocol = curProtocol.protocol; // xmlname;
					name = xmlname;
					if (name.equals("") ) {

						if (protocol.equals(PDMLTags.BOOTP_PROTOCOL)) {
							name = getOption(show);
						}
						if (protocol.equals(PDMLTags.DHCPv6_PROTOCOL)) {
							name = getV6Option(show);
                                                }
						else if (protocol.equals(PDMLTags.DNS_PROTOCOL)) {
							name = getDNSFieldName(show);
						}
						else if (protocol.equals(PDMLTags.SNMP_PROTOCOL)) {
							name = getSNMPFieldName(show);
						}
						else if (protocol.equals(PDMLTags.PACKET_CABLE_PROTOCOL)) {
							name = getPKTCFieldName(show);
						}
					}
				
				
//	num?				logger.warn(PC2LogCategory.Parser, subCat,
//							"The " + qName + " at " + l.getLineNumber()
//							+ " doesn't appear to have the name attribute set to the expected "
//							+ "[protocol].[field] format, using the value for both protocol and name.");
				}
				lastField = name;
				curField = new Field(protocol, name, showname, size, show, value, pos, hide, unmasked);
			
				String msgName = getMsgName(xmlname, name, show, showname);
				if (msgName != null) {
					// Test to see if the protocol defines a tunneling protocol.
					// If yes we want to mark it appropriately and set its tunneling name 
					// to separate the elements for retrieval
					if (PDMLTags.isTunnelingMsgType(msgName) &&
							curProtocol != null) {
						curProtocol.setTunnelingName(msgName);
					}
					curPacket.setName(msgName);
				}
			}
			else  {
				String msg =  qName
					+ " can not be processed because it doesn't contain a name attribute.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else  {
			String msg =  qName
				+ " can not be processed when the curField attribute is not equal to null.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}

	/**
	 * Parses the <proto> element.
	 *
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseProto(String qName, Attributes atts) throws SAXException {
		if (curProtocol != null) {
				// Treat this situation as a subProtocol
				parentProtocol = curProtocol;
				ancestorProtocols.addFirst(parentProtocol);
				curProtocol = null;

		}

		String xmlname = atts.getValue(PDMLTags.NAME_ATTR);
		String showname = atts.getValue(PDMLTags.SHOW_NAME_ATTR);
		if (showname == null)
			showname = atts.getValue(PDMLTags.SHOW_NAME_ATTR2);
		String size = atts.getValue(PDMLTags.SIZE_ATTR);
		String pos = atts.getValue(PDMLTags.POS_ATTR);
		
		if (logger.isTraceEnabled(PC2LogCategory.Parser, subCat)) {
			String msg = "Parsing line <" + qName;
			if (xmlname != null)
				msg += " name=\"" + xmlname + "\"";
			if (showname != null)
				msg += " showname=\"" + showname + "\"";
			
			if (size != null)
				msg += " size=\"" + size + "\"";
			if (pos != null)
				msg += " pos=\"" + pos + "\"";
			
			msg += ">";
			
			logger.trace(PC2LogCategory.Parser, subCat, msg);
		}
		// Each field tag must have a name field.
		if (xmlname != null) {
			// Next we need to divide the xmlname into the
			// actual protocol and field names
			int index = xmlname.indexOf(".");
			String protocol = null;
			String name = null;
			if (index != -1) {
				protocol = xmlname.substring(0,index);
				name = xmlname.substring(index+1);
			}
			else {
				protocol = xmlname;
				name = xmlname;
//				geninfo?				logger.warn(PC2LogCategory.Parser, subCat,
//				"The " + qName + " at " + l.getLineNumber()
//				+ " doesn't appear to have the name attribute set to the expected "
//				+ "[protocol].[field] format, using the value for both protocol and name.");
			}
			lastProto = name;
			if (name.equalsIgnoreCase(PDMLTags.FRAME_PROTOCOL))
				lastFrame = showname;
		
			curProtocol = new Protocol(protocol, name, showname, size, pos);
					
			if (protocol.equals(PDMLTags.BOOTP_PROTOCOL))
				bootp = true;
			else if (protocol.equals(PDMLTags.DHCPv6_PROTOCOL))
				dhcpv6 = true;
			else if (protocol.equals(PDMLTags.ICMPV6_PROTOCOL))
				icmpv6 = true;
			else if (protocol.equals(PDMLTags.DNS_PROTOCOL))
				dns = true;
			else if (protocol.equals(PDMLTags.KERBEROS_PROTOCOL))
				kerberos = true;
			else if (protocol.equals(PDMLTags.SNMP_PROTOCOL))
				snmp = true;
			else if (protocol.equals(PDMLTags.TFTP_PROTOCOL))
				tftp = true;
			else if (protocol.equals(PDMLTags.TOD_PROTOCOL))
				tod = true;
//			else if (protocol.equals(PDMLTags.IP_PROTOCOL))
//				ip = true;
			else if (protocol.equals(PDMLTags.PACKET_CABLE_PROTOCOL))
				pktc = true;
			else if (protocol.equals(PDMLTags.SYSLOG_PROTOCOL))
			    syslog = true;
		}
		else  {
			String msg =  qName
			+ " can not be processed because it doesn't contain a name attribute.";
			throw new PC2XMLException(fileName,msg, l);
		}
	
	}

	/**
	 * Parses the <packet> element.
	 *
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parsePacket(String qName, Attributes atts) throws SAXException {
		if (curPacket == null) {
			curPacket = new Packet();
		}
		else  {
			String msg =  qName
				+ " can not be a child of a <packet> tag.";
			throw new PC2XMLException(fileName,msg, l);
		}

	}

	/**
	 * This file reads the data first before passing it to the XML parser to correct any extraneous
	 * issues that would cause the parse to fail. Examples of this are an incomplete packet because
	 * the capture was stopped prematurely or some control character is in the middle of a string
	 * that must be removed.
	 */
	private void repair(File pdml) {
		// Perform conversion on the file if it contains an unexpected control character
		// in the Bogus Length field
		try {
			BufferedReader in = new BufferedReader(new FileReader(pdml));
			int fileSep = pdml.getPath().lastIndexOf(File.separator);
			File output = new File(pdml.getPath().substring(0, fileSep+1) + "temp.pdml");
			BufferedWriter out = new BufferedWriter(new FileWriter(output, false));
			String line = in.readLine();
			//String prevLine = null;
			boolean repair = false;
			boolean openPacket = false;
			boolean hasFinalTag = false;
			while (line != null) {
				if (line.contains("Bogus")) {
					line = line.replaceAll("","");
					if (!repair)
						logger.info(PC2LogCategory.Model, subCat, "The capture file[" + pdml + "] had to have control characters removed from a line in the file.");
					repair=true;
				}

				if (line.equals("<packet>"))
					openPacket = true;
				else if (line.equals("</packet>"))
					openPacket = false;
				else if (line.equals("</pdml>"))
					hasFinalTag = true;
				out.write(line + "\n");
			
				line = in.readLine();
			}
			out.flush();
			// Next we need to see if there is an open packet tag in the file.
			if (openPacket) {
				logger.warn(PC2LogCategory.Parser, subCat, "The capture file[" + pdml + "] has an incomplete packet tag.");
			}
		
			// Next repair that we may have to complete is adding the end tag for the file
			// if the final packet was incomplete.
		
			if (!hasFinalTag) {
				out.write("</pdml>");
				repair = true;
				logger.warn(PC2LogCategory.Parser, subCat,
						"The capture file[" + pdml + "] had to have the end tag included.");
			}
		    out.close();
		    in.close();
	
			if (repair) {
				pdml.delete();
				boolean success = output.renameTo(pdml);
				if (!success) {
					logger.warn(PC2LogCategory.Parser, subCat,
							"Unable to move the temp.pdml file back to file["
							+ pdml.getName() + "].");
				}
			}
			else {
				output.delete();
			}
		}
		catch (Exception ex) {
			logger.error(PC2LogCategory.Parser, subCat, "PDMLParser encountered an exception while parsing file[" + pdml + "].");
		}
	}

	/**
	 * This method applies the filter criteria, if there is any,
	 * to the current packet.
	 * @param p
	 * @return true if it should be included in the database, false otherwise.
	 */
	private void resolveFilter() {
		if (filter == null)
			return;
		else {
			String ip = filter.getIp();
			if (ip != null) {
				// Next see if the ip is a reference to a setting or an
				// actual address.
				int offset = ip.indexOf(".");
				if (offset != -1) {
					String neLabel = ip.substring(0, offset);
					Properties p = SystemSettings.getSettings(neLabel);
					if (p != null) {
						String setting = p.getProperty(ip.substring(offset+1));
						if (setting != null) {
							// Next we need to use the compressed form if it is an IP address since that is 
							// the form used by the capture tool
							try {
								setting = Conversion.ipv6ShortForm(setting);
							}
							catch (IllegalArgumentException iae) {
									logger.error(PC2LogCategory.Parser, subCat, "Could not resolve IP filter");
							}
//							if (setting.contains(":")) {
//								int start = -1;
//								String pattern = ":0:";
//								int length = 0;
//								int index = setting.indexOf(pattern);
//								if (index != -1) {
//									start = index;
//									length = 3;
//									// move past the initial ":0" to look for the next pattern match
//									index += 2;
//									index = setting.indexOf(pattern, index);
//									while (index != -1) {
//										length += 2;
//										index += 2;
//										index = setting.indexOf(pattern, index);
//									}
//															
//									setting = setting.substring(0, start) + "::" + setting.substring((start+length));
//								}
//							}
							logger.info(PC2LogCategory.Parser, subCat,
									"Resolved IP filter to (" + setting + ").");
							filter.setIp(setting);
						}
					}
				}
				filterIP = true;
			}
			String port = filter.getPort();
			if (port != null) {
				// Next see if the port is a reference to a setting or an
				// actual address.
				int offset = port.indexOf(".");
				if (offset != -1) {
					String neLabel = port.substring(0, offset);
					Properties p = SystemSettings.getSettings(neLabel);
					if (p != null) {
						String setting = p.getProperty(port.substring(offset+1));
						if (setting != null) {
							logger.info(PC2LogCategory.Parser, subCat,
									"Resolved port filter to (" + setting + ").");
							filter.setPort(setting);
						}
					}
				}
				filterPort = true;
			}
		}
		if (filter.getProtocol() != null) {
			filterProtocol = true;
		}
	
		if (filter.getMsgType() != null) {
			filterMsgType = true;
		}
		if (filter.getClientMacAddr() != null) {
			String mac = filter.getClientMacAddr();
			if (mac != null) {
				// Next see if the MAC Address is a reference to a setting or an
				// actual address.
				int offset = mac.indexOf(".");
				if (offset != -1) {
					String neLabel = mac.substring(0, offset);
					Properties p = SystemSettings.getSettings(neLabel);
					if (p != null) {
						String setting = p.getProperty(mac.substring(offset+1));
						if (setting != null) {
							logger.info(PC2LogCategory.Parser, subCat,
									"Resolved Client MAC Address filter to (" + setting + ").");
							filter.setClientMacAddr(setting.toLowerCase());
						}
					}
				}
				else
					filter.setClientMacAddr(mac);
				filterClientMAC = true;
			}
		
		}
	}

	private void updatePacketForGenInfo() throws SAXException {
		Field f = curProtocol.getField(PDMLTags.NUM_ATTR);
		if (f != null && f.show != null)
			curPacket.setFrame(f.show);
		else {
			String msg = "The <geninfo> tag doesn't appear to have a field with the name 'num' in it.";
			throw new PC2XMLException(fileName,msg, l);
		}
		f = curProtocol.getField(PDMLTags.LEN_ATTR);
		if (f != null && f.size != null)
			curPacket.setLength(f.size);
		else {
			String msg = "The <geninfo> tag doesn't appear to have a field with the name 'len' in it.";
			throw new PC2XMLException(fileName,msg, l);
		}
		f = curProtocol.getField(PDMLTags.CAP_LEN_ATTR);
		if (f != null && f.size != null)
			curPacket.setCapLen(f.size);
		else {
			String msg = "The <geninfo> tag doesn't appear to have a field with the name 'caplen' in it.";
			throw new PC2XMLException(fileName,msg, l);
		}
		f = curProtocol.getField(PDMLTags.TIMESTAMP_ATTR);
		if (f != null && f.value != null)
			curPacket.setTimestamp(f.value);
		else {
			String msg = "The <geninfo> tag doesn't appear to have a field with the name 'timestamp' in it.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	public static void main(String[] args) throws SAXException, IOException {
	    
	    final String filePath = "C:\\Documents and Settings\\rvail\\My Documents\\Projects\\PCSim2\\CL\\logs\\Syslog-ProvComplete.xml";
	    final String dbName = "syslogTest";
	    ParserFilter filter = new ParserFilter(null, "syslog", null, null);
	    
	    PDMLParser parser = new PDMLParser();
	    PacketDatabase db = parser.parse(filePath, dbName, filter);
	    
	    @SuppressWarnings("unused")
        LinkedList<Packet> syspacks = db.syslogTable;
	         
	    
	}
}
