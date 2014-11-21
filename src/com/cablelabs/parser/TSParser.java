/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.parser;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import com.cablelabs.common.Conversion;
import com.cablelabs.common.Transport;
import com.cablelabs.fsm.ActionFactory;
import com.cablelabs.fsm.ArrayIndex;
import com.cablelabs.fsm.ArrayRef;
import com.cablelabs.fsm.Assign;
import com.cablelabs.fsm.AutoProvState;
import com.cablelabs.fsm.Capture;
import com.cablelabs.fsm.CaptureAttributeType;
import com.cablelabs.fsm.CaptureOp;
import com.cablelabs.fsm.CaptureRef;
import com.cablelabs.fsm.ChangeStatus;
import com.cablelabs.fsm.ComparisonOp;
import com.cablelabs.fsm.CurStateRef;
import com.cablelabs.fsm.ElseIf;
import com.cablelabs.fsm.EndSessionState;
import com.cablelabs.fsm.EventConstants;
import com.cablelabs.fsm.EventRef;
import com.cablelabs.fsm.ExtensionConstants;
import com.cablelabs.fsm.ExtensionRef;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.Generate;
import com.cablelabs.fsm.If;
import com.cablelabs.fsm.Literal;
import com.cablelabs.fsm.LogMsg;
import com.cablelabs.fsm.LogicalOp;
import com.cablelabs.fsm.Mod;
import com.cablelabs.fsm.Model;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.MsgRef;
import com.cablelabs.fsm.NetworkElements;
import com.cablelabs.fsm.PC2Exception;
import com.cablelabs.fsm.ParserFilter;
import com.cablelabs.fsm.PlatformRef;
import com.cablelabs.fsm.PresenceModel;
import com.cablelabs.fsm.PresenceStatus;
import com.cablelabs.fsm.ProvisioningData;
import com.cablelabs.fsm.Proxy;
import com.cablelabs.fsm.RTPRef;
import com.cablelabs.fsm.Reference;
import com.cablelabs.fsm.ReferencePointConstants;
import com.cablelabs.fsm.Responses;
import com.cablelabs.fsm.Result;
import com.cablelabs.fsm.Retransmit;
import com.cablelabs.fsm.SDPConstants;
import com.cablelabs.fsm.SDPRef;
import com.cablelabs.fsm.SIPBodyRef;
import com.cablelabs.fsm.SIPConstants;
import com.cablelabs.fsm.SIPRef;
import com.cablelabs.fsm.Send;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.Sleep;
import com.cablelabs.fsm.State;
import com.cablelabs.fsm.Stream;
import com.cablelabs.fsm.StunRef;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.fsm.TimeoutConstants;
import com.cablelabs.fsm.Transition;
import com.cablelabs.fsm.UtilityConstants;
import com.cablelabs.fsm.UtilityRef;
import com.cablelabs.fsm.VarExprRef;
import com.cablelabs.fsm.VarRef;
import com.cablelabs.fsm.Variable;
import com.cablelabs.fsm.Verify;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.stun.StunConstants;
import com.cablelabs.tools.PDMLTags;

/**
 * This is the heart of the XML parsing for the PC 2.0
 * test platform. It takes the name of a file argument
 * to the parse method and attempts to verify the document
 * for well-formedness as well as syntacticly correct to 
 * the best of it's ability.
 * 
 * The results produced by the class is a TSDocument for
 * delivery into the Platform engine for test case execution.
 * 
 * @author ghassler
 *
 */
public class TSParser extends DefaultHandler {
	
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
	 * This is the list of FSMs within the document currently
	 * being processed by the Test Script Parser.
	 */
	private LinkedList<FSM> fsms;
	
	/**
	 * The final result is a TSDocument. This is that result.
	 */
	protected TSDocument doc = null;
	
	/**
	 * This is used to verify that the <pc2xml> tag is the root
	 * tag of the document.
	 */
	private	boolean validRoot = false;
	
	/**
	 * This is used to verify that the document contains at least
	 * one <fsm> element.
	 */
	private boolean hasFSM = false;
	
	/**
	 * This is used to verify that the document contains at least
	 * one <NE> element with at least one attribute.
	 */
	private boolean hasNE = false;
	
	/**
	 * This is used to verify that the document contains at least
	 * one <state> element within an <fsm> element.
	 */
	private boolean hasState = false;
	
	/**
	 * This is used to verify that the document contains at least
	 * one <model> element with at least one supported model defined.
	 */
	private boolean hasModel = false;
	
	/**
	 * A place holder for the current <fsm> element being parsed.
	 */
	private FSM curFSM = null;
	
	/**
	 * A place holder for the current <model> element being parsed.
	 */
	private Model curModel = null;

	/**
	 * A place holder for the current <state> element being parsed.
	 */
	private State curState = null;
	
	/**
	 * A place holder for the current "action" element being parsed.
	 */
	private ActionFactory curAction = null;
	
	/**
	 * A place holder for the current <response> element being parsed.
	 */
	private Responses curResponse = null;
	
	/**
	 * A place holder for the current <send> element being parsed.
	 */
	private Send curSend = null;
	
	/**
	 * A place holder for the current <proxy> element being parsed.
	 */
	private Proxy curProxy = null;
	
	/**
	 * A place holder for the current <if> element being parsed.
	 */
	private If curIf = null;
	
	/**
	 * A place holder for the current <elseif> element being parsed.
	 */
	private ElseIf curElseIf = null;
	
	/**
	 * A place holder for the previous <elseif> element when the 
	 * <elseif> elements are nested.
	 */
	private ElseIf prevElseIf = null;
	
	/**
	 * A place holder for the previous <curVerify> element when the 
	 * <curVerify> elements are nested.
	 */
	private Verify curVerify = null;
	/**
	 * This allows multiple elseif tags to be associated to a single
	 * if statement
	 */
	private LinkedList<ElseIf> nestedElseIfs = new LinkedList<ElseIf>();
	
	/**
	 * A place holder for the current "comparison operator" element 
	 * being parsed.
	 */
	private ComparisonOp curCompOp = null;
	
	/**
	 * A place holder for the nesting of "logical operator" element being parsed.
	 */
	private LogicalOp [] logOp = null;
	
	/**
	 * An index to the current "logical operator" being constructed.
	 */
	private int logOpIndex = -1;
	
	/**
	 * A place holder for the current <msg_ref> element being parsed.
	 */
	private MsgRef curMsgRef = null;
	
	/**
	 * A place holder for the current <expr> element being parsed.
	 */
	private Literal curLit = null;
	
	/**
	 * A place holder for the current <mod> element being parsed.
	 */
	private Mod curMod = null;
	
	/**
	 * A place holder for the current tag being parsed.
	 */
	private String curTag = null;
	
	/**
	 * A place holder for the current <var_exp> element being parsed.
	 */
	private VarExprRef curVarExpr = null;
	
	/**
	 * A place holder for the current <var> element being parsed.
	 */
	private Variable curVar = null;
	
	/**
	 * A place holder for the current <assign> element being parsed.
	 * 
	 */
	private Assign curAssign = null;
	
	/**
	 * A place holder for the current <retransmit> element being parsed.
	 */
	private Retransmit curRetransmit = null;
	
	/**
	 * A place holder for the current <start_capture> or <stop_capture> tags.
	 */
	private Capture curCapture = null;
	
	/**
	 * A place holder for the current <parser_filter> tag.
	 */
	private ParserFilter parserFilter = null;
	
	/**
	 * The console and log file interface.
	 */
	private LogAPI logger = null;
	
	/**
	 * A flag identifying that the parser is working to parse an 
	 * named event.
	 */
	private boolean lookForEvent = false;
	
	/**
	 * This is a container to store characters (data) between XML
	 * elements. Since the parse may make more than one callout for
	 * data between elements, we need a container to accumulate all
	 * of the information for an element until the end tag for 
	 * the element is detected.
	 */
	private String partialChar = null;
	
	/**
	 * The name of the file currently being parsed.
	 */
	private String fileName = null;
	
	/**
	 * This flag specifies whether the logs file should be updated
	 * for a test or not.
	 */
	private boolean resetLogs = true;
	
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subCat = "";
	
	/**
	 * This flag specifies whether the script is in the middle of 
	 * an add alteration to an existing FSM from a template or local
	 * to the current document.
	 */
	private boolean add = false;
	
	/**
	 * This flag specifies whether the script is allowed to contain 
	 * the add and remove tags. These two tags are only allowed if the
	 * 'template' tag exists in the script prior to an add or remove
	 * tag
	 */
	private boolean containsTemplate = false;
	
	/**
	 * This flag specifies whether the script is in the middle of 
	 * a remove alteration to an existing FSM from a template or local
	 * to the current document.
	 */
	private boolean rm = false;

	/**
	 * This flag is used to identify a new element being added to
	 * an FSM defined within an external document. This allows the 
	 * standard parsing method to operate on a completely new state
	 * within a document that is altering another
	 * document.
	 */
	private boolean newAlterationElement = false;
	
	/**
	 * A flag indicating that the current alteration is occurring 
	 * upon a new Prelude ActionFactory.
	 */
	private boolean newPrelude = false;

	/**
	 * A flag indicating that we are parsing the <where> tag of 
	 * an <array_ref> that is a child of a <var> tag.
	 * 
	 */
	private boolean whereTag = false;
	
	private String maskPattern = "0x[0-9]+";

	private int wildcardDim = -1;
	
	/**
	 * A flag indicating that the filter tag is the parent for the
	 * Literal or MsgRef
	 */
	private boolean filterTag = false;
	/**
	 * A place holder for the <array_index> element currently being
	 * parsed.
	 */
	private ArrayIndex curArrayIndex = null;
	
	/**
	 * A place holder for the <array_ref> element currently being
	 * parsed.
	 */
	private ArrayRef curArrayRef = null;
	
	/**
	 * A flag indicating that the current alteration is occurring 
	 * upon a new Postlude ActionFactory.
	 */
	private boolean newPostlude = false;
	
	/**
	 * A flag indicating that the current alteration is occurring 
	 * upon a new Response FlowControl.
	 */
	private boolean newResponse = false;

	/**
	 * A table of MsgRefs that need to have their FSM UID values set
	 * at the end of parsing all the FSMs.
	 */
	private LinkedList<IncompleteMsgRef> unresolvedTable = new LinkedList<IncompleteMsgRef>(); 
	
	/**
	 * A list of parents that the Action class can be a child of.
	 */
	private static final String actionParents = "prelude, postlude, then or else";
	
	/**
	 * This flag is used to log when a deltaScript has completed all of its modifications
	 * to its loaded document(s).
	 */
	private boolean deltaScript = false;
	
	/**
	 * A flag indicating the parent is a model tag.
	 */
	private boolean modelParent = false;
	
	/**
	 * A flag indicating that a <to> tag was parsed as the parent
	 */
	private boolean toParent = false;
	
	/**
	 * A flag indicating that a <from> tag was parsed as the parent
	 */
	private boolean fromParent = false;
	
	/**
	 * A flag indicating the parent is an <ip> element.
	 */
	private boolean ipFlag = false;
	
	/**
	 * A flag indicating the parent is a <port> element.
	 */
	private boolean portFlag = false;

	 /**
	 * A place holder for the current <start_stream> or <stop_stream> element
	 * being parsed.
	 */
	private Stream curStream = null;
	
	private ActionFactory nestedParentAction = null;
	
	/**
	 * Default constructor for the Test Script Parser.
	 * @param resetLog 
	 */
	public TSParser(boolean resetLog) {
		super();
		fsms = new LinkedList<FSM>();
		logger = LogAPI.getInstance(); // Logger.getLogger(TSParser.class.getName());
		this.resetLogs = resetLog;
	}
	
	/**
	 * The entry point to parse a PC 2.0 Test Script XML document.
	 * The method accepts the name of the file to be parse and 
	 * produces either location and information about a problem within
	 * the document or a TSDocument for use by the PC 2.0 platform 
	 * engine.
	 * @param fileName - the PC 2.0 Test Script XML document to be parsed.
	 * @return TSDocument - wrapper class for the platform test engine.
	 * @throws SAXException - Parsing error has occurred while processing the
	 * 						document.
	 * @throws IOException - file access issue has occurred.
	 */
	public TSDocument parse(String fileName) throws SAXException, IOException {
		xr = XMLReaderFactory.createXMLReader();
		this.fileName = fileName;
		if (xr != null) {
			xr.setContentHandler(this);
			FileReader reader = new FileReader(this.fileName);
			xr.parse(new InputSource(reader));
			if (doc != null)
			    doc.setFileName(fileName);
			return doc;
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
	 * may need. The TSParser simply logs that it is starting and 
	 * creates the final TSDocument container.
	 */
	@Override
    public void startDocument() throws SAXException {
		// handle a start-of-document event
		logger.info(PC2LogCategory.Parser, subCat, "Starting to parse...");
		doc = new TSDocument();
	}
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called by the parser has completed parsing the document.
	 * This method verifies mandatory elements occur within the 
	 * document before return control back to the invoking class.
	 */
	@Override
    public void endDocument() throws SAXException {
		boolean initStates = verifyInitialStates();
		boolean msgRefsResolved = resolveMsgRefs();
		boolean oneCDF = verifyOnlyOneCDF(); 
		boolean validTrans = validateTrans();
		if (!(validRoot &&
				hasFSM &&
				hasNE && 
				hasState &&
				hasModel &&
				validTrans &&
				oneCDF && initStates )) {
			int count = 0;
			String names = "";
			if (doc.getFsms() != null) {
				doc.getFsms().size();
				ListIterator<FSM> iter = doc.getFsms().listIterator();
				while (iter.hasNext()) {
					FSM f = iter.next();
					names += f.getName() + " initialState[" + f.getInitialState() + "].";
				}
			}
			
			String msg = new String("Invalid test case. Mandatory components identified:\n" +
					"pc2xml: " + validRoot +
					"\n fsm:    " + hasFSM +
					"\n NE:     " + hasNE +
					"\n State:  " + hasState +
					"\n Model:  " + hasModel +
					"\n Transitions: " + validTrans +
					"\n MsgRefs Resolved: " + msgRefsResolved 
					+ "\n one CDF: " + oneCDF
					+ "\n InitalStates: " + initStates
					+ "\n number of fsms="  + count +
					"\n names=" + names);
			throw new PC2XMLException(fileName, msg, l);
		}
		logger.info(PC2LogCategory.Parser, subCat, "Completed parsing of document.");
		if (fsms.size() >= 1)
			logger.info(PC2LogCategory.Parser, subCat, "There are " + fsms.size() + " finite state machines in the document!\n");
		else
			logger.info(PC2LogCategory.Parser, subCat, "There is " + fsms.size() + " finite state machine in the document!\n");
		
		if (deltaScript) 
			logger.info(PC2LogCategory.Parser, subCat, 
					"All alterations in the document " + fileName + " are complete.");
		
		if (doc != null) {
			doc.setFsms(fsms);
			logger.debug(PC2LogCategory.Parser, subCat, "\n" + doc.toString() + "\n");
		}
	}
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called at the start of each element and is were the 
	 * TSParser begins to separate the various tags into their 
	 * respective objects. Validation of each mandatory attribute
	 * and children elements begins in this method.
	 * 
	 * (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
    public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
		
		logger.trace(PC2LogCategory.Parser, subCat, "starting element (" + qName + ")");
		logger.trace(PC2LogCategory.Parser, subCat, "   number of attributes: " + atts.getLength());
		curTag = qName;
		boolean unrecognizedTag = false;
		
		if (qName.equals("pc2xml")) {
			parseRoot(qName, atts);
			return;
		}
		
		// First see if we are in the middle of an alteration
		// If we are we want to invoke the appropriate method
		// to make the alteration to the existing FSM.
		if ((add || rm ) && !newAlterationElement) {
			alteration(qName, atts);
		}
		else {
			if (qName.equals("add_to")) {
				parseAddTo(qName, atts);
				return;
			}
			else if (qName.equals("remove_from")) {
				parseRemoveFrom(qName, atts);
				return;
			}
//			Parse the fsm tag
			else if (qName.equals("fsm")) {
				parseFsm(qName, atts);
				return;
			}
			else if (qName.equals("template")) {
				parseTemplate(qName, atts, name);
				return;
			}
			else if (qName.equals("configure")) {
				parseConfigure(qName, atts);
				return;
			}
			
			if (curFSM != null) {
				
				switch (qName.charAt(0)) {
				case 'a':
					if (qName.equals("add_ref")) {
						if (curMod != null || 
								curAssign != null || 
								curIf != null ||
								curVerify != null) {
							parseMsgRef(qName, atts);
						}
						else {
							String msg = qName + " tag can only be a child element of a mod or assign tag.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else if (qName.equals("and")) {
						parseLogicOp(qName, atts);
					}
					else if (qName.equals("array_index") &&
							whereTag) {
						// Need to setup the curMsgRef variable
						// for proper parsing
						if (curVar != null && 
								curVar.getRef() instanceof MsgRef) {
							curMsgRef = (MsgRef)curVar.getRef();
						}
						parseArrayIndex(qName, atts);
					}
					else if (qName.equals("array_ref")) {
						parseMsgRef(qName, atts);
					}
					else if (qName.equals("assign")) {
						parseAssign(qName, atts);
					}
					else 
						unrecognizedTag = true;
					
					break;
					
				case 'c':
					if (qName.equals("contains")) {
						parseOperator(qName, atts);
					}
					else if (qName.equals("count")) {
						parseOperator(qName, atts);
					}
					else if (qName.equals("cur_state")) {
						if (curCompOp != null && curCompOp.getOperator().equals("count")) {
							// do nothing
						}
						else {
							String msg = qName + " tag can only be a child element of a count tag.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else if (qName.equals("capture_ref")) {
						parseCaptureRef(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'd':
					if (qName.equals("digest")) {
						parseOperator(qName, atts);
					}
					else if (qName.equals("dnc")) {
						parseOperator(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'e':
					if (qName.equals("element")) {
						parseElement(qName, atts);
					}
					else if (qName.equals("else")) {
						parseElse(qName, atts);
					}
					else if (qName.equals("elseif")) {
						parseElseIf(qName, atts);
					}
					else if (qName.equals("eq")) {
						parseOperator(qName, atts);
					}
					else if (qName.equals("expr")) {
						parseExpr(qName, atts);
					}
					else if (qName.equals("endsWith")) {
						parseOperator(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'f':
					if (qName.equals("fail")) {
						parsePassFail(qName, atts);
					}
					else if (qName.equals("filter")) {
						filterTag = true;
					}
					else if (qName.equals("from")) {
						if (curStream != null)
							fromParent = true;
						else {
							String msg =  qName + " can only be a child to a <start_stream> or <stop_stream> element.";
							throw new PC2XMLException(fileName,msg, l);
						
						}
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'g':
					if (qName.equals("generate")) {
						parseGenerate(qName, atts);
					}
					else if (qName.equals("gt")) {
						parseOperator(qName, atts);
					}
					else if (qName.equals("gte")) {
						parseOperator(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'i':
					if (qName.equals("if")) {
						parseIf(qName, atts);
					}
					else if (qName.equals("ip")) {
						if (toParent || fromParent)
							ipFlag = true;
						else {
							String msg =  qName + " can only be a child to a <to> or <from> element.";
							throw new PC2XMLException(fileName,msg, l);
						
						}
					}
					else if (qName.equals("ipv4")) {
						parseOperator(qName, atts);
					}
					else if (qName.equals("ipv6")) {
						parseOperator(qName, atts);
					}
					else if (qName.equals("isDate")) {
					  parseOperator(qName, atts);  
					} else 
						unrecognizedTag = true;
					break;
					
				case 'l':
					if (qName.equals("log")) {
						parseLog(qName, atts);
					}
					else if (qName.equals("lt") ) {
						parseOperator(qName, atts);
					}
					else if (qName.equals("lte")) {
						parseOperator(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'm':
					if (qName.equals("msg_ref")) {
						parseMsgRef(qName, atts);
					}
					else if (qName.equals("mod")) {
						parseMod(qName, atts);
					}
					// Parse the model tag
					else if (qName.equals("models")) {
						// parseModel(qName, atts);
						modelParent = true;
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'N':
					// Parse Network Elements (NE tag)
					if (qName.equals("NE")) {
						hasNE = parseNetworkElements(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
				case 'n':
					if (qName.equals("neq") ) {
						parseOperator(qName, atts);
					}
					else if (qName.equals("null")) {
						parseOperator(qName, atts);
					}
					else if (qName.equals("notnull")) {
						parseOperator(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'o':
					if (qName.equals("or")) {
						parseLogicOp(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'p':
					if (qName.equals("pass")) {
						parsePassFail(qName, atts);
					}
					else if (qName.equals("port")) {
						if (toParent || fromParent)
							portFlag = true;
						else {
							String msg =  qName + " can only be a child to a <to> or <from> element.";
							throw new PC2XMLException(fileName,msg, l);
						
						}
					}
//					Parse Prelude and Postlude Tag
					else if (qName.equals("prelude")||
							qName.equals("postlude")) {
						parsePrelude(qName, atts);
					}
					else if (qName.equals("presenceServer")) {
						parsePresenceServer(qName, atts);
					}
					else if (qName.equals("proxy")) {
						parseProxy(qName, atts);
					}
					else if (qName.equals("parse_capture")) {
						parseCapture(qName, atts, CaptureOp.PARSE);
					}
					else if (qName.equals("parser_filter")) {
						parseParserFilter(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'q':
					if (qName.equals("queue")) {
						if (curCompOp != null &&
								(curCompOp.getOperator().equals("contains") ||
										curCompOp.getOperator().equals("dnc") ||
										curCompOp.getOperator().equals("count"))) {
							parseMsgRef(qName, atts);
						}
						else {
							String msg = qName + " tag can only be a child element of a contains, dnc, or count tag.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'r':
					if (qName.equals("register")) {
						parseCommonModel(qName, atts);
					}
					else if (qName.equals("registrar")) {
						parseCommonModel(qName, atts);
					}
					// Parse Response Tag
					else if (qName.equals("response")) {
						parseResponse(qName, atts);
					}
					else if (qName.equals("retransmit")) {
						parseRetransmit(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 's':
//					Parse Send Tag
					if (qName.equals("send")) {
						parseSend(qName, atts);
					}
					else if (qName.equals("services")) {
						parseServices(qName, atts);
					}
					// Parse session tag
					else if (qName.equals("session")) {
						parseSession(qName, atts);
					}
					else if (qName.equals("sleep")) {
						parseSleep(qName, atts);
					}
					else if (qName.equals("start_capture")) {
						parseCapture(qName, atts, CaptureOp.START);
					}
					else if (qName.equals("start_stream")) {
						parseStream(qName, atts, false);
					}
//					Parse State tag
					else if (qName.equals("state")) {
						parseState(qName, atts);
					}
					//	Parse States tag
					else if (qName.equals("states")) {
						parseStates(atts);
					}
					else if (qName.equals("stop_capture")) {
						parseCapture(qName, atts, CaptureOp.STOP);
					}
					else if (qName.equals("stop_stream")) {
						parseStream(qName, atts, true);
					}
					else if (qName.equals(MsgRef.STUN_MSG_TYPE)) {
						parseCommonModel(qName, atts);
					}
					else if (qName.equals("subtract_ref")) {
						if (curMod != null || 
								curAssign != null || 
								curIf != null ||
								curVerify != null) {
							parseMsgRef(qName, atts);
						}
						else {
							String msg =qName + " tag can only be a child element of a mod or assign tag.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else if (qName.equals("startsWith")) {
						parseOperator(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 't':	
					if (qName.equals("to")) {
						if (curStream != null || curSend != null)
							toParent = true;
						else {
							String msg =  qName + " can only be a child to a <start_stream>, <stop_stream> or <send> element.";
							throw new PC2XMLException(fileName,msg, l);
						
						}
					}
					else if (qName.equals("then")) {
						parseThen(qName, atts);
					}
					else if (qName.equals("tls")) {
						parseCommonModel(qName, atts);
					}
//					Parse Transitions tag
					else if (qName.equals("transition")) {
						parseTransition(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'v':
					if (qName.equals("verify")) {
						parseVerify(qName, atts);
					}
					else if (qName.equals("var")) {
						parseVar(qName, atts);
					}
					else if (qName.equals("var_expr")) {
						if (curVarExpr == null && 
								(curMod != null || 
										curVar != null ||
										filterTag)) {
							curVarExpr = new VarExprRef(curFSM.getUID());
						}
						else {
							String msg = qName + " tag can only be a child element of a mod tag.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else if (qName.equals("var_ref")) {
						parseVarRef(qName, atts);
					}
					else 
						unrecognizedTag = true;
					break;
					
				case 'w':
					if (qName.equals("where")) {
						if (curVar != null && 
								(curVar.getRef() instanceof MsgRef ||
										curMsgRef instanceof MsgRef)) {
							whereTag = true;
						}
					} 
					else 
						unrecognizedTag = true;
					break;
					
				default :
					throw new PC2XMLException(fileName,"Encountered unexpected tag(" + qName + ") element.", l);
				}
			}
			else {
				throw new PC2XMLException(fileName,"Encountered unexpected tag(" + qName + ") outside of fsm element.", l);
			}
			
			if (unrecognizedTag)
				throw new PC2XMLException(fileName,"Encountered unexpected tag(" + qName + ") element.", l);
		}

		
	}
	
	/**
	 * This is a method that must be overwritten for the SAXParser.
	 * It is called when the parser detects the end tag for an element.
	 * The TSParser addes the constructed object for the element into
	 * the resulting TSDocument.
	 * 
	 * (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 *
	 */
	@Override
    public void endElement(String uri, String name, String qName) throws SAXException {
		logger.trace(PC2LogCategory.Parser, subCat, "ending element (" + qName + ")");

		switch (qName.charAt(0)) {
		case 'a' :
			if (qName.equals("add_ref")) {
				endReference(uri, name, qName);
			}
			else if (qName.equals("add_to")) {
				if (add) {
					add = false;
					curFSM = null;
				}
				else 
					throw new PC2XMLException(fileName,"Encountered ending <add> tag without identifying starting tag.", l);
			}
			else if (qName.equals("and")) {
				endLogicalOp(uri, name, qName);
			}
			else if (qName.equals("array_index")) {
				if (curCompOp != null) {
					if (curCompOp.getLeft() != null) {
						if (curCompOp.getRight() == null) {
							try {
								UtilityRef ur = (UtilityRef)curMsgRef.clone();
								ArrayRef ar = new ArrayRef(curArrayIndex);
								ur.setArrayReference(ar);
								curCompOp.setRight(ur);
							}
							catch (Exception e) {
								throw new PC2XMLException(fileName,e.getMessage(), l);
							}
							curArrayIndex = null;
							curMsgRef = null;
						}
						else {
							String msg = qName + " tag appears to occur too many times in parent element.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else {
						try {
							UtilityRef ur = (UtilityRef)curMsgRef.clone();
							ArrayRef ar = new ArrayRef(curArrayIndex);
							ur.setArrayReference(ar);
							curCompOp.setLeft(ur);
						}
						catch (Exception e) {
							throw new PC2XMLException(fileName,e.getMessage(), l);
						}
						curArrayIndex = null;
						curMsgRef = null;
					}
				}
				else if (curArrayRef != null) {
					try {
						curArrayRef.setIndexes(curArrayIndex);
						curArrayIndex = null;
					}
					catch (Exception e) {
						throw new PC2XMLException(fileName,e.getMessage(), l);
					}
					
				}
			}
			else if (qName.equals("array_ref")) {
					curTag = qName;
				endReference(uri, name, qName);
			}
			else if (qName.equals("assign")) {
				// the assign was already added to the list of actions
				// simply clear up the local place holder.
				curAssign = null;
			}
			break;
			
		case 'c':
			if (qName.equals("contains")) {
				endComparisonOp(uri, name, qName);
			}
			else if (qName.equals("count")) {
				endComparisonOp(uri, name, qName);
			}
			else if (qName.equals("cur_state") && 
					curState != null) {
				CurStateRef stateCountRef = new CurStateRef(curState);
				if (curCompOp != null) {
					if (curCompOp.getLeft() != null) {
						if (curCompOp.getRight() == null) {
							curCompOp.setRight(stateCountRef);
						}
						else {
							String msg = qName + " tag appears to occur too many times in parent element.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else {
						curCompOp.setLeft(stateCountRef);
					}
				}
				else if (curMod != null) {
					curMod.setRef(stateCountRef);
				}
				
			}
			else if (qName.equals("capture_ref")) {
				endReference(uri, name, qName);
				lookForEvent = false;
			}
			break;
		case 'd':
			if (qName.equals("digest")) {
				endComparisonOp(uri, name, qName);
			}
			else if (qName.equals("dnc")) {
				endComparisonOp(uri, name, qName);
			}
			break;
			
		case 'e' :
			if (qName.equals("else")) {
				 if (curVerify != null && curVerify.getElseActions() == null) {
					curVerify.setElseActions(curAction);
					curAction = null;
					// Now see if we had to store the prevAction
					if (nestedParentAction != null) {
						curAction = nestedParentAction;
						nestedParentAction = null;
					}
				}
				 else if (curElseIf != null) {
					curElseIf.setElseActions(curAction);
					curAction = null;
				}
				
				else if (curIf != null && curIf.getElseActions() == null) {
					curIf.setElseActions(curAction);
					curAction = null;
				}
				else {
					String msg = qName + " ending tag appears to be outside expected element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else if (qName.equals("elseif")) {
				if (curIf != null) {
					nestedElseIfs.add(curElseIf);
					// Next see if there are more than one entry in the 
					//curIf.setElseif(curElseIf);
					if (prevElseIf != null) {
						prevElseIf.setElseif(curElseIf);
					}
					prevElseIf = curElseIf;
					curElseIf = null;
				}
				else {
					String msg = qName + " ending tag appears to be outside expected element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else if (qName.equals("eq")) {
				endComparisonOp(uri, name, qName);
			}
			else if (qName.equals("expr")) {
				if (lookForEvent && partialChar != null) {
					lookForEvent = false;
					if (!EventConstants.isEvent(partialChar) && 
							!TimeoutConstants.isTimeoutEvent(partialChar) &&
							!ReferencePointConstants.isReferencePointEvent(partialChar)) {
						String msg =  "Identified invalid event (" + partialChar 
						+ ") for expr tag." + "\n Parser only understands:\n\t" 
						+ EventConstants.getEvents() + "\n\t" 
						+ TimeoutConstants.getEvents() + "\n\t"
						+ ReferencePointConstants.getEvents();
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				
				if (curCompOp != null) {
					if (curCompOp.getLeft() != null) {
						if (curCompOp.getRight() == null) {
							String tmp = null;
							if (partialChar != null)
								tmp = replaceControlCharacters(partialChar);
							else
								tmp = new String();
							curLit.setExpr(tmp);
							curCompOp.setRight(curLit);
							partialChar = null;
						}
						else {
							String msg = qName + " tag appears to occur too many times in parent element.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else {
						String tmp = null;
						if (partialChar != null)
							tmp = replaceControlCharacters(partialChar);
						else
							tmp = new String();
						curLit.setExpr(tmp);
						curCompOp.setLeft(curLit);
						partialChar = null;
					}
				}
				else if (curVarExpr != null) {
					String tmp = replaceControlCharacters(partialChar);
					curLit.setExpr(tmp);
					curVarExpr.addLiteral(curLit);
					partialChar = null;
				}
				else if (curMod != null) {
					String tmp = replaceControlCharacters(partialChar);
					curLit.setExpr(tmp);
					curMod.setRef(curLit);
					partialChar = null;
				}
				else if (curVar != null) {
					String tmp = replaceControlCharacters(partialChar);
					curLit.setExpr(tmp);
					curVar.setRef(curLit);
					partialChar = null;
				}
				else if (curAssign != null) {
					String tmp = replaceControlCharacters(partialChar);
					curLit.setExpr(tmp);
					curAssign.setRef(curLit);
					partialChar = null;
				}
				else if (filterTag) {
					String tmp = replaceControlCharacters(partialChar);
					curLit.setExpr(tmp);
					curCapture.setFilter(curLit);
					partialChar = null;
				}
				else if (ipFlag) {
					if (toParent) {
						if (curStream != null) {
							String tmp = replaceControlCharacters(partialChar);
							curLit.setExpr(tmp);
							curStream.setToIP(curLit);
							partialChar = null;
						}
					}
					else if (fromParent) {
						String tmp = replaceControlCharacters(partialChar);
						curLit.setExpr(tmp);
						curStream.setFromIP(curLit);
						partialChar = null;
					}
				}
				else if (portFlag) {
					if (toParent) {
						if (curStream != null) {
							String tmp = replaceControlCharacters(partialChar);
							curLit.setExpr(tmp);
							curStream.setToPort(curLit);
							partialChar = null;
						}
					} 
					else if (fromParent) {
						String tmp = replaceControlCharacters(partialChar);
						curLit.setExpr(tmp);
						curStream.setFromPort(curLit);
						partialChar = null;
					}
				}
				curLit = null;
			}
			else if (qName.equals("endsWith")) {
				endComparisonOp(uri, name, qName);
			}
			break;
			
		case 'f':
			if (qName.equals("fail")) {
				// Actions were added to the proper element when read into the parser
				// do nothing
			}
			else if (qName.equals("filter")) {
				filterTag = false;
			}
			else if (qName.equals("from")) {
				fromParent = false;
			}
			else if (qName.equals("fsm")) {
				testDuplicateFSM(curFSM);
				if (curFSM.hasInitialState()) {
					fsms.addLast(curFSM);
					hasFSM = true;
					curFSM = null;
				}
				else  {
					throw new PC2XMLException(fileName, "FSM(" + curFSM.getName() + ") doesn't contain the initial state.", l );
				}
			}
			break;
			
		case 'g':
			if (qName.equals("gt")) {
				endComparisonOp(uri, name, qName);
			}
			else if (qName.equals("gte")) {
				endComparisonOp(uri, name, qName);
			}
			break;
			
		case 'i':
//			Parse Conditionals
			if (qName.equals("if")) {
				curResponse.addActiveOp(curIf);
				logger.trace(PC2LogCategory.Parser, subCat, 
						curIf.toString());
				if (!nestedElseIfs.isEmpty()) {
					ElseIf root = nestedElseIfs.getFirst();
					if (root != null)
						curIf.setElseif(root);
					nestedElseIfs.clear();
					
				}
				prevElseIf = null;
				curIf = null;
			}
			else if (qName.equals("ip")) {
				ipFlag = false;
			}
			else if (qName.equals("ipv4")) {
				endComparisonOp(uri, name, qName);
			}
			else if (qName.equals("ipv6")) {
				endComparisonOp(uri, name, qName);
			}
			else if (qName.equals("isDate")) {
			    endComparisonOp(uri, name, qName);
			}
			break;
			
		case 'l':
			if (qName.equals("log")) {
				// Actions were added to the proper element when read into the parser
				// do nothing
			}
			else if (qName.equals("lt")) {
				endComparisonOp(uri, name, qName);
			}
			else if (qName.equals("lte")) {
				endComparisonOp(uri, name, qName);
			}
			break;
			
		case 'm':
			if (qName.equals("mod")) {
				if (curSend != null) {
					if (curMod.getRef() != null ||
							curMod.getModType().equals("delete") ||
							curMod.getModType().equals("add")) {
						curSend.addModifier(curMod);
					}
					else {
						
						String msg = qName + " tag does not contain a child of expr, msg_ref, add_ref or substract_ref element.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else if (curProxy != null) {
					if (curMod.getRef() != null ||
							curMod.getModType().equals("delete") ||
							curMod.getModType().equals("add")) {
						curProxy.addModifier(curMod);
					}
					else {
						String msg = qName + " tag does not contain a child of expr, msg_ref, add_ref or substract_ref element.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else if (curRetransmit != null) {
					if (curMod.getRef() != null ||
							curMod.getModType().equals("delete") ||
							curMod.getModType().equals("add")) {
						curRetransmit.addModifier(curMod);
					}
					else {
						String msg = qName + " tag does not contain a child of expr, msg_ref, add_ref or substract_ref element.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				curMod = null;
			}
			else if (qName.equals("models")) {
				if (hasModel)
					curFSM.setModel(curModel);
				curModel = null;
				modelParent = false;
			}
			else if (qName.equals("msg_ref")) {
				endReference(uri, name, qName);
				lookForEvent = false;
			}
			break;
		case 'n':
			if (qName.equals("neq")) {
				endComparisonOp(uri, name, qName);
			}
			else if (qName.equals("null")) {
				endComparisonOp(uri, name, qName);
			}
			else if (qName.equals("notnull")) {
				endComparisonOp(uri, name, qName);
			}
			break;
			
		case 'o':
			if (qName.equals("or")) {
				endLogicalOp(uri, name, qName);
			}
			break;
			
		case 'p':
			if (qName.equals("pass")) {
				// Actions were added to the proper element when read into the parser
				// do nothing
			}
			else if (qName.equals("port")) {
				portFlag = false;
			}
			else if (qName.equals("postlude")) {
				// Add postludes to the state and reset local container
				curState.setPostlude(curAction);
				curAction = null;
			}
			else if (qName.equals("prelude")) {
				// Add preludes to the state and reset local container
				curState.setPrelude(curAction);
				curAction = null;
			}
			else if (qName.equals("proxy")) {
				if (curAction != null) {
					curAction.addAction(curProxy);
					curProxy = null;
				}
				else {
					String msg =  "ending proxy tag occurs outside of an action element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else if (qName.equals("parse_capture")) {
				if (curAction != null) {
					curAction.addAction(curCapture);
					curCapture = null;
				}
				else {
					String msg =  "ending " + curCapture.getOperation().toString() 
						+ " tag occurs outside of an action element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else if (qName.equals("parser_filter")) {
				
				curCapture.setFilter(parserFilter);
				parserFilter = null;
			}
			break;
			
		case 'q':
			if (qName.equals("queue")) {
				endReference(uri, name, qName);
			}
			break;
			
		case 'r' :
			if (qName.equals("remove_from")) {
				if (rm) {
					rm = false;
					curFSM = null;
				}
				else 
					throw new PC2XMLException(fileName,"Encountered ending <remove> tag without identifying starting tag.", l);
			}
			else if (qName.equals("response")) {
				curState.setResponses(curResponse);
				curResponse = null;
			}
			else if (qName.equals("retransmit")) {
				if (curAction != null) {
					curAction.addAction(curRetransmit);
					curRetransmit = null;
				}
				else {
					String msg =  "ending retransmit tag occurs outside of an action element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			break;
		case 's':
			if (qName.equals("send")) {
				if (curAction != null) {
					curAction.addAction(curSend);
					curSend = null;
				}
				else {
					String msg =  "ending send tag occurs outside of an action element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else if (qName.equals("sleep")) {
				// Actions were added to the proper element when read into the parser
				// do nothing
			}
			else if (qName.equals("start_capture") ||
					qName.equals("stop_capture")) {
				if (curCapture != null) {
					curAction.addAction(curCapture);
					curCapture = null;
				}
				else {
					String msg =  "ending " + qName + " tag occurs outside of an action element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			if (qName.equals("start_stream") ||
					qName.equals("stop_stream")) {
				if (curAction != null) {
					if (qName.equals("stop_stream"))
						curAction.addAction(curStream);
					else if (curStream.getFromIP() != null &&
							curStream.getFromPort() != null && 
							curStream.getToIP() != null &&
							curStream.getToPort() != null)
						curAction.addAction(curStream);
					
					else {
						String msg =  "ending " + qName 
							+ " tag occurs without all of the child elements appearing." +
							" The <from> and <to> elements are required children of " + qName;
						throw new PC2XMLException(fileName,msg, l);
					}
					curStream = null;
				}
				else {
					String msg =  "ending send tag occurs outside of an action element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else if (qName.equals("state")) {
				endState(uri, name, qName);
			}	
			else if (qName.equals("subtract_ref")) {
				endReference(uri, name, qName);
			}
			else if (qName.equals("startsWith")) {
				endComparisonOp(uri, name, qName);
			}
			break;	
			
		case 't':
			if (qName.equals("then")) {
				
				if (curVerify != null && curVerify.getThenActions() == null) {
					curVerify.setThenActions(curAction);
					curAction = null;
					// Now see if we had to store the prevAction into the 
					// curIf because of nesting issues
					if (nestedParentAction != null) {
						curAction = nestedParentAction;
						nestedParentAction = null;
					}
				}
				else if (curElseIf != null) {
					curElseIf.setThenActions(curAction);
					curAction = null;
				}
				else if (curIf != null && curIf.getThenActions() == null) {
					curIf.setThenActions(curAction);
					curAction = null;
				}
				else {
					String msg = qName + " ending tag appears to be outside expected element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else if (qName.equals("to")) {
				toParent = false;
			}
//			Parse Transitions tag
			else if (qName.equals("transition")) {
				// Transitions are added to the current FSM at the start of the tag
				//newAlterationElement = false;
			}
			break;
			
		case 'v':
//			Parse Conditionals
			if (qName.equals("verify")) {
				// First see if we are in the middle of an action
				// if yes, then we add the verify to it
				if (curAction != null) {
					curAction.addAction(curVerify);
					curFSM.addVerify(curVerify);
					logger.trace(PC2LogCategory.Parser, subCat, 
							curVerify.toString());
					curVerify = null;
					
				}
				else if (curResponse != null) {
					curResponse.addActiveOp(curVerify);
					logger.trace(PC2LogCategory.Parser, subCat, 
						curVerify.toString());
					curFSM.addVerify(curVerify);
					curVerify = null;
				}
				 
			}
			else if (qName.equals("var")) {
				if (curVar != null) {
					
					// See if the var is part of a
					// then or else first then see
					// if it is part of a response
					if (curAction != null) 
						curAction.addAction(curVar);
					else if (curResponse != null)
						curResponse.addActiveOp(curVar);
					curVar = null;
					curArrayIndex = null;
				}
				else {
					throw new PC2XMLException(fileName,"Error encountered when processing <var> tag.", l);
				}
			}
			else if (qName.equals("var_expr")) {
				if (curMod != null) {
					curMod.setRef(curVarExpr);
					curVarExpr = null;
				}
				else if (curVar != null) {
					curVar.setRef(curVarExpr);
					curVarExpr = null;
				}
				else if (filterTag) {
					curCapture.setFilter(curVarExpr);
					curVarExpr = null;
				}
				else {
					String msg =  "ending var_expr tag occurs outside of a <mod> element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else if (qName.equals("var_ref")) {
				endReference(uri, name, qName);
			}
			break;
		case 'w':
			if (qName.equals("where")) {
				if (curVar != null) {
					Reference ref = curVar.getRef();
					if (ref != null && 
							ref instanceof UtilityRef) {
						((UtilityRef)ref).setArrayReference(curArrayRef);
						
					}
				}
				wildcardDim = -1;
				curArrayRef = null;
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
	@Override
    public void characters(char ch[], int start, int length) throws SAXException {
		String data =  new String(ch,start,length);
		logger.trace(PC2LogCategory.Parser, subCat, 
				curTag + " - CDATA: " + length + " characters. Data=[" + data + "].");
		
		if (curTag != null) {
			if (curTag.equals("msg_ref") || curTag.equals("add_ref") ||
					curTag.equals("subtract_ref") || curTag.equals("queue") ||
					curTag.equals("array_ref") || curTag.equals("var_ref") || 
					curTag.equals("capture_ref")) {
				if (curMsgRef != null) {
					if (partialChar == null) 
						partialChar = new String (data);
					else
						partialChar += data;
					logger.trace(PC2LogCategory.Parser, subCat, 
							curTag + " contains characters (" + partialChar + ").");
				}
			}
			else if (curTag.equals("expr")) {
				logger.trace(PC2LogCategory.Parser, subCat, 
						curTag + " contains characters (" + data + ").");
				if (partialChar == null) 
					partialChar = new String (data);
				else
					partialChar += data;
				
			}
		}
		else  {
			logger.trace(PC2LogCategory.Parser, subCat, 
					"Ignoring characters(" + data + ").");
		}
	}
	

	private void alteration(String qName, Attributes atts) throws SAXException {
		if (qName.equals("NE")) {
			alterNetworkElements(atts);
		}
		else if (qName.equals("state")) {
			alterState(atts);
		}
		else if (qName.equals("states")) {
			parseStates(atts);
		}
		else if (qName.equals("services")) {
			parseServices(qName, atts);
		}
		else if (qName.equals("transition")) {
			parseTransition(qName, atts);
		}
		else if (qName.equals("config")) {
			parseConfigure(qName, atts);
		}
	}

	/**
	 * Alters an existing state object within the FSM
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException
	 */
	private void alterState(Attributes atts) throws SAXException {
		String sub = atts.getValue("sub");
		// When the sub attribute is null then the state is going to
		// be a new state within the FSM.
		if (sub == null) {
			if (add) {
				newAlterationElement = true;
				parseState("state", atts);
			}

			else if (rm) {
				removeState(atts.getValue("ID"));
			}
		}
		else if (sub != null) {
			String ID = atts.getValue("ID");
			curState = curFSM.getState(ID);
			if (curState != null) {
				if (sub.equals("transition")) {
					newAlterationElement = true;
				}
				else if (sub.equals("prelude")) {
					String once = atts.getValue("once");
					if (add) {
						curAction = curState.getPrelude();
						if (curAction == null) {
							newPrelude = true;
							curAction = new ActionFactory(curFSM.getSubcategory(), false);
						}
						if (SystemSettings.resolveBooleanSetting(once))
							curAction.setOnce(true);
					}

					else if (rm) 
						curState.setPrelude(null);
					newAlterationElement = true;
				}
				else if (sub.equals("postlude")) {
					String once = atts.getValue("once");
					if (add) {
						curAction = curState.getPostlude();
						if (curAction == null)  {
							newPostlude = true;
							curAction = new ActionFactory(curFSM.getSubcategory(), false);
						}
						if (SystemSettings.resolveBooleanSetting(once))
							curAction.setOnce(true);
					}

					else if (rm) {
						curState.setPostlude(null);
						curAction = null;
					}
					
					
					newAlterationElement = true;
				}
				else if (sub.equals("response")) {
					String ifIndex = atts.getValue("if");
					if (ifIndex == null) {
						if (curAction == null) {
							if (add) {
								curResponse = curState.getResponse();
								if (curResponse == null) {
									curResponse = new Responses();
									newResponse = true;
								}
								newAlterationElement = true;
							}
							else if (rm) {
								curState.setResponses(null);
								newAlterationElement = true;
							}
						}
						else 
							throw new PC2XMLException(fileName,"Attempting to alter response logic with local variable curAction already set.", l);
					}

				}
				else if (sub.equals("")) {
					// An empty string indicates that the script is trying to change
					// a value for the state itself. An example might be making it
					// an EndSession state or changing the length of the timer
					String timer = atts.getValue("timer");
					String ES = atts.getValue("ES");
					if (ES != null && ES.equals("true"))
						curState = new EndSessionState(ID, curFSM);
					if (timer != null) {
						try {
							int timeout = Integer.parseInt(timer);
							
							logger.info(PC2LogCategory.Parser, subCat, 
									"Altering timeout of state(" + ID + ") from(" + curState.getTimeout() + ") to(" + timeout + ").");
							curState.setTimeout(timeout);
						}
						catch (Exception e) {
							String msg = "The <state> tag contains an invalid value for the timer attribute." +
							" Value must be an integer.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
				}
				else if (sub.equals("transaction")) {
					throw new PC2XMLException(fileName,"Can't alter the transition in state("
							+ ID + ") because the state doesn't exist.", l);
				}
				else if (sub.equals("prelude")) {
					throw new PC2XMLException(fileName,"Can't alter the prelude in state("
							+ ID + ") because the state doesn't exist.", l);
				}
				else if (sub.equals("postlude")) {
					throw new PC2XMLException(fileName,"Can't alter the postlude in state("
							+ ID + ") because the state doesn't exist.", l);
				}
				else if (sub.equals("response")) {
					throw new PC2XMLException(fileName,"Can't alter the postlude in state("
							+ ID + ") because the state doesn't exist.", l);
				}
			}
			else
				throw new PC2XMLException(fileName,"Can't alter the the state("
						+ ID + ") because the state doesn't exist.", l);
		}
	}
	/**
	 * Finds the requested fsm in the list if it exists.
	 * 
	 * @param fsm - the name of the FSM to retrieve
	 * 
	 * @return - the requested FSM or null
	 */
	private FSM getFsm(String fsm) {
		ListIterator<FSM> iter = fsms.listIterator();
		while (iter.hasNext()) {
			FSM f = iter.next();
			if (f.getName().equals(fsm))
				return f;
		}
		return null;
	}


	private void alterNetworkElements(Attributes atts) throws SAXException {
		NetworkElements nes = curFSM.getNetworkElements();
		String simType = atts.getValue("sim_type");
		String ele = atts.getValue("elements");
		String sExt = atts.getValue("ext_supported");
		String rExt = atts.getValue("ext_require");
		String dExt = atts.getValue("ext_disable");
		String t = atts.getValue("targets");
		
		if (add) {
			if (simType != null) 
				throw new PC2XMLException(fileName,"PC2XML document may not alter the sim_type attribute of a template.",l);
			if (ele != null) {
				StringTokenizer tokens = new StringTokenizer(ele);
				int numTokens = tokens.countTokens();
				for (int i = 0; i < numTokens; i++) {
					boolean addElement = true;
					String newElement = tokens.nextToken();
					ListIterator<String> iter = nes.getElements();
					while (iter.hasNext()) {
						// Make sure we don't add a duplicate
						if (newElement.equals(iter.next()))
							addElement = false;
						
					}
					if (addElement)
						nes.addElement(newElement);
				}
			}
			if (sExt != null) {
				LinkedList<String> ext = parseExtensions(sExt);
				nes.addSupportedExtensions(ext);
			}
			if (rExt != null) {
				LinkedList<String> ext = parseExtensions(rExt);
				nes.addRequireExtensions(ext);
			}
			if (dExt != null) {
				LinkedList<String> ext = parseExtensions(dExt);
				nes.addDisableExtensions(ext);
			}
			if (t != null && t.length() >= 3) {
				StringTokenizer tokens = new StringTokenizer(t);
				int numTokens = tokens.countTokens();
				for (int i = 0; i < numTokens; i++) {
					String target = tokens.nextToken();
//					ListIterator<String> iter = nes.getElements();
//					boolean found = false;
//					while (iter.hasNext() && !found) {
//						if (iter.next().equals(target)) {
							// Lastly, make sure that the target element 
							// doesn't already exist in the list
//							ListIterator<String> targetIter = nes.getTargets();
//							boolean addTarget = true;
//							while (targetIter.hasNext()) {
//								if (targetIter.next().equals(target))
//									addTarget = false;
//							}
//							if (addTarget)
								nes.addTarget(target);
//							found = true;
//						}
//					}
//					if (!found) {
//						String msg =  "targets contains a network element label that doesn't" 
//							+ " appear in the elements attribute.";
//						throw new PC2XMLException(fileName,msg, l);
//					}
				}

			}
		}
//		else if (rep) {
//			if (simType != null) 
//				throw new PC2XMLException(fileName,"PC2XML document may not alter the sim_type attribute of a template.",l);
//			if (ele != null) {
//				LinkedList<String> elements = new LinkedList<String>();
//				StringTokenizer tokens = new StringTokenizer(ele);
//				int numTokens = tokens.countTokens();
//				for (int i = 0; i < numTokens; i++) {
//					elements.add(tokens.nextToken());
//				}
//				nes.setElements(elements);
//			}
//			if (sExt != null) {
//				LinkedList<String> ext = parseExtensions(sExt);
//				nes.setSupportedExtensions(ext);
//			}
//			if (rExt != null) {
//				LinkedList<String> ext = parseExtensions(sExt);
//				nes.setRequireExtensions(ext);
//			}
//			if (dExt != null) {
//				LinkedList<String> ext = parseExtensions(sExt);
//				nes.setDisableExtensions(ext);
//			}
//			if (t != null && t.length() >= 3) {
//				LinkedList<String> targets = new LinkedList<String>();
//				StringTokenizer tokens = new StringTokenizer(t);
//				int numTokens = tokens.countTokens();
//				for (int i = 0; i < numTokens; i++) {
//					String target = tokens.nextToken();
//					ListIterator<String> iter = nes.getElements();
//					boolean found = false;
//					while (iter.hasNext() && !found) {
//						if (iter.next().equals(target)) {
//							targets.add(target);
//							found = true;
//						}
//					}
//					if (!found) {
//						String msg =  "targets contains a network element label that doesn't" 
//							+ " appear in the elements attribute.";
//						throw new PC2XMLException(fileName,msg, l);
//					}
//					nes.setTargets(targets);
//				}
//			}
//		}
		else if (rm) {
			if (simType != null) 
				throw new PC2XMLException(fileName,"PC2XML document may not alter the sim_type attribute of a template.",l);
			if (ele != null) {
				StringTokenizer tokens = new StringTokenizer(ele);
				int numTokens = tokens.countTokens();
				for (int i = 0; i < numTokens; i++) {
					nes.removeElement(tokens.nextToken());
				}
			}
			if (sExt != null) {
				LinkedList<String> ext = parseExtensions(sExt);
				ListIterator<String> iter = ext.listIterator();
				while (iter.hasNext())
					nes.removeSupportedExtension(iter.next());
			}
			if (rExt != null) {
				LinkedList<String> ext = parseExtensions(rExt);
				ListIterator<String> iter = ext.listIterator();
				while (iter.hasNext())
					nes.removeRequireExtension(iter.next());
			}
			if (dExt != null) {
				LinkedList<String> ext = parseExtensions(dExt);
				ListIterator<String> iter = ext.listIterator();
				while (iter.hasNext())
					nes.removeDisableExtension(iter.next());
			}
			if (t != null && t.length() >= 3) {
				StringTokenizer tokens = new StringTokenizer(t);
				int numTokens = tokens.countTokens();
				for (int i = 0; i < numTokens; i++) {
					String target = tokens.nextToken();
					nes.removeTarget(target);
				}
			}
		}
	}
	
	private void endComparisonOp(String uri, String name, String qName) throws SAXException {
		// First see if there is a logical operator we are adding this
		// comparison operator to.
		if (logOpIndex > -1) {
			if (logOp[logOpIndex] != null) {
				if (logOp[logOpIndex].getLeft() == null) {
					logOp[logOpIndex].setLeft(curCompOp);
					
				}
				else if (logOp[logOpIndex].getRight() == null) {
					logOp[logOpIndex].setRight(curCompOp);
				}
				else {
					String msg =  qName + " tag found both nodes filled on ending tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
		}
		else if (curVerify != null)
			curVerify.setCond(curCompOp);
		else if (whereTag && 
				curVar != null) {
			Reference ref = curVar.getRef();
			if (ref != null && ref instanceof UtilityRef) {
				UtilityRef ur = (UtilityRef)ref;
				curArrayRef = new ArrayRef(curCompOp);
				ur.setArrayReference(curArrayRef);
			}

		}	
//		 Next look for an ElseIf object
		else if (curElseIf != null) {
		    curElseIf.setCond(curCompOp);
		}
		else if (curIf != null) {
			curIf.setCond(curCompOp);
		}
		

		curCompOp = null;
	}
	
	private void endLogicalOp(String uri, String name, String qName) throws SAXException {
//		 Simply move the current one to the previous ones
		// left node if empty or right node if left is not null
		if (logOpIndex > 0) {
			LogicalOp op = logOp[logOpIndex];
			if (op.getLeft() == null ||
					op.getRight() == null) {
				String msg =  qName + " tag has only one comparison operator on an ending " + qName + " tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
			int prevIndex = logOpIndex - 1;
			if (logOp[prevIndex] != null) {
				
				if (logOp[prevIndex].getLeft() == null) {
					logOp[prevIndex].setLeft(op);
					
				}
				else if (logOp[prevIndex].getRight() == null) {
					logOp[prevIndex].setRight(op);
				}
				else {
					String msg =  qName + " tag found both nodes filled on ending tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
				
				logOp[logOpIndex] = null;
				logOpIndex = prevIndex;
			}
		}
		// If we get this far and the index is now -1 we are ready to
		// put this on the If or ElseIf
		else if (logOpIndex == 0 && 
				(curIf != null || curVerify != null)) {
			LogicalOp op = logOp[logOpIndex];
			if (op.getLeft() == null ||
					op.getRight() == null) {
				String msg =  qName + " tag has only one comparison operator on an ending " + qName + " tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
			if (curVerify != null) {
				curVerify.setCond(op);
			}
			else if (curElseIf != null) {
				
				curElseIf.setCond(op);
			}
			else {
				curIf.setCond(op);
			}

			logOp[logOpIndex] = null;
			logOpIndex--;
		}
		else if (logOpIndex == 0 && whereTag &&
				curVar != null) {
			Reference ref = curVar.getRef();
			if (ref != null && ref instanceof UtilityRef) {
				UtilityRef ur = (UtilityRef)ref;
				curArrayRef = new ArrayRef(logOp[logOpIndex]);
				ur.setArrayReference(curArrayRef);
			}
			logOp[logOpIndex] = null;
			logOpIndex--;
		}
	}
	
	private void endReference(String uri, String name, String qName) throws SAXException {
		validateReference();
		if (curCompOp != null) {
			if (curCompOp.getLeft() != null) {
				if (curCompOp.getRight() == null) {
					curCompOp.setRight(curMsgRef);
				}
				else {
					String msg = qName + " tag appears to occur too many times in parent element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				curCompOp.setLeft(curMsgRef);
			}
		}
		else if (curVarExpr != null) {
			curVarExpr.addMsgRef(curMsgRef);
		}
		else if (curMod != null) {
			curMod.setRef(curMsgRef);
		}
		else if (curVar != null) {
			curVar.setRef(curMsgRef);
//			curResponse.addActiveOp(curVar);
//			curVar = null;
//			curArrayIndex = null;
//			whereTag = false;
		}
		else if (curAssign != null) {
			curAssign.setRef(curMsgRef);
		}
		else if (filterTag) {
			curCapture.setFilter(curMsgRef);
		}
		else if (ipFlag) {
			if (toParent) {
				if (curStream != null)
					curStream.setToIP(curMsgRef);
				else if (curSend != null)
					curSend.setToIP(curMsgRef);
			}
			else if (fromParent)
				curStream.setFromIP(curMsgRef);
		}
		else if (portFlag) {
			if (toParent) {
				if (curStream != null)
					curStream.setToPort(curMsgRef);
				else if (curSend != null)
					curSend.setToPort(curMsgRef);
			} 
			else if (fromParent)
				curStream.setFromPort(curMsgRef);
		}
		partialChar = null;
		curMsgRef = null;
	
	}

	private void endState(String uri, String name, String qName) throws SAXException {
		if (!rm) {
			try {
				if (add) {
					State s = curFSM.getState(curState.getName());
					if (s != curState) 
						// Note this is an error, we simply
						// want the correct error message and stack
						// trace to appear
						curFSM.addState(curState);
					// Since the else would be that they are the 
					// same state simply see if there is a new element.
					else {
						if (newPrelude) {
							curState.setPrelude(curAction);
							newPrelude = false;
							curAction = null;
							logger.debug(PC2LogCategory.Parser, subCat,
									curState.getName() 
									+ " state with new prelude:\n" + curState);
						}
						else if (newPostlude) {
							curState.setPostlude(curAction);
							newPostlude = false;
							curAction = null;
							logger.debug(PC2LogCategory.Parser, subCat,
									curState.getName() 
									+ " state with new postlude:\n" + curState);
						}
						else if (newResponse) {
							curState.setResponses(curResponse);
							newResponse = false;
							curResponse = null;
							logger.debug(PC2LogCategory.Parser, subCat,
									curState.getName() 
									+ " state with new response:\n" + curState);
						}
					}
				}
				else
					curFSM.addState(curState);
			}
			catch (PC2Exception e) {
				throw new PC2XMLException(fileName,e.getMessage(), l);
			}
		}
		if (newAlterationElement) {
			// curAction = null;
			newAlterationElement = false;
		}
		curState = null;
		hasState = true;
	}
	
	
	private CaptureAttributeType isPDMLAttributeType(String attrType) {
		String tmp = attrType.toUpperCase();
		if (tmp.equals(CaptureAttributeType.VALUE.toString()))
				return CaptureAttributeType.VALUE;
		else if (tmp.equals(CaptureAttributeType.SHOW.toString()))
				return CaptureAttributeType.SHOW;
		else if (tmp.equals(CaptureAttributeType.SHOWNAME.toString()))
				return CaptureAttributeType.SHOWNAME;
		
		else if (tmp.equals(CaptureAttributeType.SIZE.toString()))
				return CaptureAttributeType.SIZE;
		else if (tmp.equals(CaptureAttributeType.NAME.toString()))
				return CaptureAttributeType.NAME;
		else if (tmp.equals(CaptureAttributeType.TIMESTAMP.toString()))
				return CaptureAttributeType.TIMESTAMP;
		else if (tmp.equals(CaptureAttributeType.POS.toString()))
				return CaptureAttributeType.POS;
		else if (tmp.equals(CaptureAttributeType.NUM.toString()))
				return CaptureAttributeType.NUM;
		else if (tmp.equals(CaptureAttributeType.CAP_LEN.toString()))
				return CaptureAttributeType.CAP_LEN;
		else if (tmp.equals(CaptureAttributeType.HIDE.toString()))
				return CaptureAttributeType.HIDE;
		else if (attrType.equals(CaptureAttributeType.UNMASKED_VALUE.toString()))
				return CaptureAttributeType.UNMASKED_VALUE;
		
		return null;
		
	}
	/**
	 * Parses the add_to element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseAddTo(String qName, Attributes atts) throws SAXException {
		if (containsTemplate) {
			String fsm = atts.getValue("fsm");

			if (fsm != null) {
				curFSM = getFsm(fsm);
				if (curFSM != null)
					add = true;
				else 
					throw new PC2XMLException(fileName,"The fsm(" + fsm 
							+ ") could not be found in the document to perform add alterations.", l);
			}
			else {
				String msg = "Invalid add alteration requested. FSM attribute is required field";
				throw new PC2XMLException(fileName,msg,l);
			}
		}
		else {
			throw new PC2XMLException(fileName,
					"Invalid add alteration request. The <template> tag must appear prior to using an <add> tag.",l);
		}
	}
	/**
	 * Parses the array_index element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseArrayIndex(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 1) {
			String index = atts.getValue("index");
			if (index != null) {
				if (curArrayIndex == null &&
						whereTag && curCompOp != null ) {
					StringTokenizer tokens = new StringTokenizer(index);
					Integer [] indexes = new Integer[tokens.countTokens()];
					int dim = 0;
					while (tokens.hasMoreTokens()) {
						String token = tokens.nextToken();
						if (token.equals("*") && 
								(wildcardDim == -1 ||
								dim == wildcardDim)) {
							// Replace * with -1 value
							indexes[dim] = -1;
							wildcardDim = dim;
						}
						else if (token.matches("[0-9]+")) {
							indexes[dim] = new Integer(token);
						}
						else {
							String msg = qName + "'s index attribute contains an invalid value in the " +
							   dim + " dimension. " +
							   " Valid values are 0 or greater integer values with the exception of one" +
							   " wildcard (*) value. All wildcards in the same where element must appear in the same dimension.";
							 throw new PC2XMLException(fileName,msg, l);
						}
						dim++;
					}
					curArrayIndex = new ArrayIndex(indexes);
					if (wildcardDim > -1)
						curArrayIndex.setWildcard(wildcardDim);
				}
				else {
					String msg =  qName + " tag can only appear as a child to a " +
							" array_ref or a comparison operator when the grandparent" +
							" is a where element " + 
					". The parser found tag as a child to a different tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag does not contain the index attribute.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not contain the required number of attributes (1).";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	/**
	 * Parses the assign element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseAssign(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 1 && atts.getLength() <= 2) {
			if (curAction != null) {
				String name = atts.getValue("name");
				String fsm = atts.getValue("fsm");
				if (name != null) {
					if (fsm == null) {
						fsm = curFSM.getName();
						curAssign = new Assign(name, fsm);
						curAction.addAction(curAssign);
					}
				}
				else {
					String msg =  qName + " tag must contain the name attribute. " +
							". The parser could not find this attribute.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
					actionParents 
					+ ". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not contain the required number of attributes (1-2).";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * Parses the log element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @param op - the type of capture operations.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseCapture(String qName, Attributes atts, CaptureOp op) throws SAXException {
		if (op == CaptureOp.PARSE) { 
			if (atts.getLength() < 1) {
				String msg =  qName + " tag must contain the name attribute, it only contains " 
				+ atts.getLength() + " attributes.";
				throw new PC2XMLException(fileName,msg, l);
			}
			else if (curAction != null) {
				String name = atts.getValue("name");
				String file = atts.getValue("file");
				if (name != null)
					curCapture = new Capture(op, name, file);
				else {
					String msg =  qName + " tag must contain the attribute name.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
				actionParents 
				+ ". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			if (atts.getLength() > 0) {
				String msg =  qName + " tag must not contain any attribute, parser found that it contains "  
				+ atts.getLength() + " attributes.";
				throw new PC2XMLException(fileName,msg, l);
			}

			if (curAction != null) {
				curCapture = new Capture(op);
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
				actionParents 
				+ ". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
	}
	
	/**
	 * Parses the <capture_ref> element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseCaptureRef(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 2) {
			String type = atts.getValue("type");
			String name = atts.getValue("name");
			String msgInstance = atts.getValue("msg_instance");
			String hdrInstance = atts.getValue("hdr_instance");
			String fsmRef = atts.getValue("fsm");
			String converter = atts.getValue("convertTo");
			String add = atts.getValue("add");
			String substring = atts.getValue("substring");
			String length = atts.getValue("length");
			if (type != null) {
				if (logOpIndex > -1 || 
						(curCompOp != null && 
								(!curCompOp.getOperator().equalsIgnoreCase("digest"))) ||
						 curMod != null ||
						curVar != null ||
						curAssign != null ||
						filterTag ||
						// Allow to or from with ip or port
						((toParent || fromParent) && (ipFlag || portFlag) )) {
					if (curMsgRef == null) {
						// Verify that the parser understands reference type
						if (PDMLTags.isCaptureType(type) && name != null) {
							CaptureRef cr = new CaptureRef(type.toLowerCase(), name);
							curMsgRef = cr;
							// For capture references we always default the 
							// nsg_instance to FIRST
							curMsgRef.setMsgInstance(MsgQueue.FIRST);
							
							if (converter != null)
								cr.setConverter(converter);
							
							if (add != null)
								cr.setAdd(add);
						}
						else {
							String msg =  qName + " tag's type attribute is not a supported capture type or the name attribute is missing.";
							throw new SAXParseException(msg, l);
						}
						
						if (msgInstance != null && validInstance(msgInstance)) {
							if (validInstance(msgInstance))
									curMsgRef.setMsgInstance(msgInstance);
							else {
								String msg =  qName + " tag's msg_instance attribute is set to an invalid value.";
								throw new SAXParseException(msg, l);
							}
						}
							
						if (hdrInstance != null) {
							if (validInstance(hdrInstance))
								curMsgRef.setHdrInstance(hdrInstance);
							else {
								String msg =  qName + " tag's hdr_instance attribute is set to an invalid value.";
								throw new SAXParseException(msg, l);
							}
						}
						
						if (fsmRef != null) {
							ListIterator<FSM> iter = fsms.listIterator();
							while (iter.hasNext()) {
								FSM f = iter.next();
								if (f.getName().equals(fsmRef)) 
									curMsgRef.setUID(f.getUID());
							}
							if (curMsgRef.getUID() == 0) {
								IncompleteMsgRef imr = new IncompleteMsgRef(fsmRef, curMsgRef, fileName, l.getLineNumber() );
								unresolvedTable.add(imr);
							}
						}
						else 
							curMsgRef.setUID(curFSM.getUID());
						
						if (substring != null) {
							parseSubstringAttr(qName, curMsgRef, substring);
						}
						
						if (length != null && 
								(length.equalsIgnoreCase("true" ) ||
										length.equalsIgnoreCase("on"))) {
							curMsgRef.setLengthFlag(true);
						}
					}
					else {
						String msg =  qName + " tag can only appear as a child to a " +
								" logical operator, comparison operator, modifier, variable, filter, or an assign" +
								". The parser found tag as a child to a different tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  qName + " tag can only appear as a child to a " +
							" logical operator, comparison operator, modifier, variable, filter or an assign." +
							"The parser found tag as a child to a different tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag does not contain the type attribute.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not contain the minimum number of attributes (2, name and type).";
			throw new PC2XMLException(fileName,msg, l);
		}		
	}
	
	private void parseCommonModel(String qName, Attributes atts) throws SAXException {
		if (modelParent && curModel == null) {
			curModel = new Model();
			hasModel = true;
			curModel.setName(qName);
			String transportProtocol = atts.getValue(SettingConstants.TRANSPORT_PROTOCOL);
			if (transportProtocol != null) {
				if (transportProtocol.equals(Transport.UDP))
					curModel.setProperty(SettingConstants.TRANSPORT_PROTOCOL, transportProtocol);
				else if (transportProtocol.equals(Transport.TCP.toString()))
					curModel.setProperty(SettingConstants.TRANSPORT_PROTOCOL, transportProtocol);
				else if (transportProtocol.equals(Transport.TLS.toString()))
					curModel.setProperty(SettingConstants.TRANSPORT_PROTOCOL, transportProtocol);
				else if (transportProtocol.equals(Transport.SCTP.toString()))
					curModel.setProperty(SettingConstants.TRANSPORT_PROTOCOL, transportProtocol);
			}
		}
		else {
			String msg = qName + " can not appear as a child element to another " + qName + " tag.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	/**
	 * Parses the configure element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseConfigure(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 2) {
			String name = atts.getValue("name");
			String value = atts.getValue("value");
			if (doc.documentConfigurableProperty(name) && value != null) {
				doc.addProperty(name, value);
//				if (name.equals("No Response Timeout"))
//					try{
//						int timeout = Integer.parseInt(value);
//						curFSM.setDefaultNoResponseTimeout(timeout);
//					}
//					catch (Exception e) {
//						String msg =  
//								"Encountered an when trying to update \"No Response Timeout\" for the curFSM[" 
//								+ curFSM.getName() + "].";
//						throw new PC2XMLException(fileName,msg, l);
//					}
			}
			else {
				String msg =  name + 
					" is not a dynamically configurable platform configuration setting.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not contain the required number of attributes 2 (name and value).";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
			
	/**
	 * Parses the elseif element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseElseIf(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 0) {
			if (curResponse != null) {
				if (curIf != null) {
					if (curElseIf == null) {
						curElseIf = new ElseIf();
//						nestedElseIfs.add(curElseIf);
					}
					else {
						String msg =  "elseif tag received as a child to another action element." +
						". The parser found tag as a child to prelude, postlude, then or else tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  "elseif tag can only appear as a child to a " +
							" if or elseif tag. " + 
					". The parser found tag as a child to a different tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  "else tag can only appear as a child to a " +
						" response tag. " + 
				". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "then tag does not contain the required number of attributes (0).";
			throw new PC2XMLException(fileName,msg, l);
		}	
	}
	
	/**
	 * Parses the else element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseElement(String qName, Attributes atts) throws SAXException {
		if (modelParent && curModel instanceof PresenceModel) {
			if (atts.getLength() == 2) {
				String label = atts.getValue("label");
				String status = atts.getValue("status");
				if (label != null) {
					if (status != null) {
						if (status.equalsIgnoreCase("open")) {
							ChangeStatus cs = new ChangeStatus(label, PresenceStatus.OPEN);
							((PresenceModel)curModel).addElement(cs);
						}
						else if (status.equalsIgnoreCase("closed")) {
							ChangeStatus cs = new ChangeStatus(label, PresenceStatus.CLOSED);
							((PresenceModel)curModel).addElement(cs);
						}
						else {
							String msg = "The status attribute contains an unrecognized value." 
								+ " Valid values for the attribute are open or closed.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else {
						String msg =  qName 
							+ " tag does not contain the required 'status' of attribute.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  qName 
						+ " tag does not contain the required 'label' of attribute.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName 
					+ " tag does not contain the required number of attributes (2).";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " can only appear as a child to the presenceServer tag.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * Parses the else element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseElse(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 0) {
			if (curResponse != null || curVerify != null) {
				if (curIf != null || curVerify != null) {
					if (curAction == null && curFSM != null) {
						curAction = new ActionFactory(curFSM.getSubcategory(), false);
					}
					else if (curAction != null && curIf != null && curVerify != null) {
						// place the curAction in the curIf 
						nestedParentAction = curAction;
						curAction = new ActionFactory(curFSM.getSubcategory(), false);
					}
					else {
						String msg =  "elsetag received as a child to another action element." +
						". The parser found tag as a child to prelude, postlude, then or else tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  "else tag can only appear as a child to a " +
							" if, elseif or verify tag. " + 
					". The parser found tag as a child to a different tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  "else tag can only appear as a child to a " +
						" response or verify tag. " + 
				". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "else tag does not contain the required number of attributes (0).";
			throw new PC2XMLException(fileName,msg, l);
		}		
	}
	
	/**
	 * Parses the expr element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseExpr(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 0) {
			if (curResponse != null || curAction != null || curVerify != null) {
				if (curIf != null || curVerify != null ||
						whereTag ||
						curAssign != null ||
						filterTag ||
						curVar != null || 
						(logOpIndex > -1 || 
								curMod != null || 
								(curCompOp != null && (!curCompOp.getOperator().equalsIgnoreCase("digest")))) ||
								// Allow to or from with ip or port
								((toParent || fromParent) && (ipFlag || portFlag) ) &&
						(curMsgRef == null)) {
						curLit = new Literal();
				}
				else {
					String msg =  qName + " tag can only appear as a child to a " +
							" if, elseif, logical operator, comparison operator, modifier, " +
							"variable, filter, an assign, verify or a where tag. " +
					". The parser found tag as a child to a different element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
						" response, var, or verify tag. " + 
				". The parser found tag as a child to a different element.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not contain the required number of attributes (0).";
			throw new PC2XMLException(fileName,msg, l);
		}			
	}
	
	/**
	 * Parses the "extension" elements.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @return an array of the String representations of the extensions.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private LinkedList<String> parseExtensions(String ext) throws SAXException {
		LinkedList<String> result = new LinkedList<String>();
		if (ext != null) {
			StringTokenizer tokens = new StringTokenizer(ext);
			int numTokens = tokens.countTokens();
			for (int i = 0; i < numTokens; i++) {
					String token = tokens.nextToken();
					if (token.equals("gruu") ||
							token.equals("precondition") ||
							token.equals("100rel")) {
						result.add(token);
					}
					else {
						String msg =  "Document contains an invalid extension " +
								token + " in the NE tag at location " + i + ".";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
			
//			else {
//				String msg =  "Document contains too many extensions in the NE tag."
//						+ " The maximum number support is " + NetworkElements.getMaxExtensions() + ".";
//				throw new PC2XMLException(fileName,msg, l);
//			}
		}
		return result;
	}

	/**
	 * Parses the Extensions version of the msg_ref element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseExtensionsMsgRef(StringTokenizer tokens) throws PC2XMLException {
		ExtensionRef extRef = (ExtensionRef)curMsgRef;
		
		int count = tokens.countTokens();
		if (count == 1) {
			String ext = tokens.nextToken();
			if (ExtensionConstants.isValidExt(ext)) {
				extRef.setExt(ext);
			}
			else {
				String msg =  "extensions argument type is unknown by parser. " 
						+ " Parser identifed type as " + ext + ".";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "extensions msg_ref requires one argument. " 
					+ " Parser identifed " + count + " arguments.";
			throw new PC2XMLException(fileName,msg, l);		
		}
	}





	/**
	 * 
	 * @param atts
	 * @throws SAXException
	 */
//	private void alterAction(Attributes atts) throws SAXException {
//		if (curAction != null) {
//			
//		}
//		else 
//			throw new PC2XMLException(fileName,"Couldn't perform alteration on action class because the current factory is null.",l);
//		
//	}

	/**
	 * Parses the fsm element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseFsm(String qName, Attributes atts) throws SAXException {
		String fsmName = atts.getValue("name");
		if (fsmName != null) {
			curFSM = new FSM(fsmName);
			String sipStack = atts.getValue("sipStack");
			if (sipStack != null) 
				curFSM.setSipStack(sipStack);
		}
		else {
			String msg = "fsm must have an unique name attribute.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * Parses the generate element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseGenerate(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 1 && atts.getLength() <= 2) {
			if (curAction != null) {
				String event = atts.getValue("event");
				String target = atts.getValue("fsm");
				if (event != null) {
					if (EventConstants.isEvent(event) ||
							TimeoutConstants.isTimeoutEvent(event) ||
							ReferencePointConstants.isReferencePointEvent(event)) {
// This allows targets of null to be sent to the owner of the
// FSM.
//						if (target == null) {
//							target = curFSM.getName();
//						}
						Generate g = new Generate(event, target, curFSM.getName());
						curAction.addAction(g);
					}
					else {
						String msg =  qName + "'s event attribute must contain only system " +
								"known events, timeouts, or reference points. " +
								". The value for event (." + event + ") is not valid.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  qName + " tag must contain the event attribute. " +
							". The parser could not find this attribute.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
				actionParents 
				+ ". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not contain the required number of attributes (0).";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * Parses the if element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseIf(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 0) {
			if (curResponse != null) {
				curIf = new If();
			}
			else {
				String msg =  "if tag can only appear as a child to a " +
						" response tag. " + 
				". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "if tag does not contain the required number of attributes (0).";
			throw new PC2XMLException(fileName,msg, l);
		}	
	}
	
	/**
	 * Parses the then element. 
	 * 
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseThen(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 0) {
			if (curResponse != null || curVerify != null) {
				if (curIf != null || curVerify != null) {
					if (curAction == null && curFSM != null) {
						curAction = new ActionFactory(curFSM.getSubcategory(), false);
					}
					else if (curAction != null && (curIf != null || curVerify != null)) {
						// place the curAction in the curIf 
						nestedParentAction = curAction;
						curAction = new ActionFactory(curFSM.getSubcategory(), false);
					}
					else {
						String msg =  "then tag received as a child to another action element." +
						". The parser found tag as a child to prelude, postlude, then or else tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  "then tag can only appear as a child to a " +
							" if, elseif or verify tag. " + 
					". The parser found tag as a child to a different tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  "then tag can only appear as a child to a " +
						" response or verify tag. " + 
				". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "then tag does not contain the required number of attributes (0).";
			throw new PC2XMLException(fileName,msg, l);
		}	
	}

	/**
	 * Parses the log element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseLog(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 1) {
			String expr = atts.getValue("expr");
			String level = atts.getValue("level");
			String prompt = atts.getValue("promptUser");
			String verify = atts.getValue("verify");
			String yes = atts.getValue("yesExpected");
			String step = atts.getValue("step");
			String requirements = atts.getValue("requirements");
			String group = atts.getValue("group");
			if (expr != null) {
				if (curAction != null && curState != null) {
					boolean verifyOn = false;
					boolean promptOn = false;
					boolean yesExpected = false;
					String updatedExpr = replaceControlCharacters(expr);
					LogMsg lm = new LogMsg(curState, updatedExpr);
					if (level != null) {
						lm.setLevel(level);
					}
					if (prompt != null && prompt.equalsIgnoreCase("true")) {
						lm.setPromptUser(true);	
						promptOn = true;
					}
					if (promptOn && verify != null &&
							verify.equalsIgnoreCase("true")) {
						lm.setVerify(true);
						verifyOn = true;
						if (step != null)
							lm.setStep(step);
						if (requirements != null) 
							lm.setRequirements(requirements);
						if (group != null)
							lm.setGroup(group);
						curFSM.addVerifyLogMsg(lm);
					}
					if (verifyOn && yes != null && 
							yes.equalsIgnoreCase("false")) {
						lm.setYesExpected(false);
						yesExpected = true;
						
					}
					if (!promptOn && (verifyOn || yesExpected)) {
						String msg =  "log tag can has the verify and/or yesExpected attribute set " +
						" without including the promptUser tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
					curAction.addAction(lm);
				}
				else {
					String msg =  qName + " tag can only appear as a child to a " +
					actionParents 
					+ ". The parser found tag as a child to a different tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  "log tag does not contain the expr attribute.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "log tag does not contain the minimum number of attributes (1).";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * Parses the "logical operator" elements.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseLogicOp(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 0) {
			if (curResponse != null || curVerify != null) {
				if (curIf != null || curVerify != null || whereTag) {
					if (logOpIndex == -1)
						logOp = new LogicalOp[50];
					if (logOpIndex >= -1) {
						boolean isAnd = false;
						if (qName.equalsIgnoreCase("and"))
							isAnd = true;
						logOpIndex++;
						logOp[logOpIndex] = new LogicalOp(curFSM.getSubcategory(), isAnd);
					}
					else {
						String msg =  qName + " tag received as a child to another logical operator element.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  qName + " tag can only appear as a child to a " +
							" if, elseif, verify, where, and, or an or tag. " + 
					". The parser found tag as a child to a different element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
						" response or verify tag. " + 
				". The parser found tag as a child to a different element.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName 
			+ " tag does not contain the required number of attributes (0).";
			throw new PC2XMLException(fileName,msg, l);
		}					
	}
	
	/**
	 * Parses the mod element.
	 * 
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseMod(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 1) {
			String modtype = atts.getValue("modtype");
			String hdr = atts.getValue("hdr");
			String hdrInstance = atts.getValue("hdr_instance");
			String bodyInstance = atts.getValue("body_instance");
			String param = atts.getValue("param");
			String before = atts.getValue("before");
			String separate = atts.getValue("separate");
			String body = atts.getValue("body");
			String value = atts.getValue("xml_value");
			if (modtype != null) {
				if (curAction != null) {
					if (curSend != null || 
							curProxy != null || 
							curRetransmit != null) {
						if (validModType(modtype)) {
							if (curMod == null) {
								curMod = new Mod(modtype);
								if (hdr != null) 
									curMod.setHeader(hdr);
								if (hdrInstance != null) {
									if (validInstance(hdrInstance))
										curMod.setHeaderInstance(hdrInstance);
									else {
										String msg =  qName + " tag's hdr_instance attribute is set to an invalid value.";
										throw new SAXParseException(msg, l);
									}
								}
								if (bodyInstance != null) {
									if (validInstance(bodyInstance))
										curMod.setBodyInstance(bodyInstance);
									else {
										String msg =  qName + " tag's body_instance attribute is set to an invalid value.";
										throw new SAXParseException(msg, l);
									}
								}
									
								if (param != null)
									curMod.setParam(param);
								if (modtype.equals("add") && before != null && before.equals("true")) {
									curMod.setBefore();
								}
								if (modtype.equals("add") && separate != null && separate.equals("true")) {
									curMod.setSeparate();
								}
								if (body != null) {
									curMod.setBody(body);
									if (value != null) {
										if (param == null) {
											if (value.equalsIgnoreCase("true") ||
													value.equalsIgnoreCase("enable") ||
													value.equalsIgnoreCase("on"))
												curMod.setXMLValue(true);
										}
										else  {
											String msg =  "value attribute of the mod tag can only appear when the param attribute " +
											" is not used.";
											throw new PC2XMLException(fileName,msg, l);
										}
									}
								}
							} 
							else {
								String msg =  "mod tag can only appear as a child to a " +
								" send. The parser found tag as a child to another mod tag.";
								throw new PC2XMLException(fileName,msg, l);
							}
						}
						else {
							String msg =  "mod tag contains an unrecognizable modtype attribute " +
									modtype + ".";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else {
						String msg =  "mod tag can only appear as a child to a " +
						" send or proxy. The parser found tag as a child to another tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  "mod tag can only appear as a child to a " +
					" send. The parser found tag as a child to a different element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  "mod tag does not contain the modtype attribute.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "mod tag does not contain the minimum number of attributes (1).";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * Parses the msg_ref element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseMsgRef(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 1 ||
				(qName.equals("array_ref") && atts.getLength() == 0)) {
			String type = atts.getValue("type");
			if (qName.equals("array_ref") && type == null)
				type = MsgRef.UTILITY_MSG_TYPE;
			String msgInstance = atts.getValue("msg_instance");
			String hdrInstance = atts.getValue("hdr_instance");
			String value = atts.getValue("value");
			String fsmRef = atts.getValue("fsm");
			String mask = atts.getValue("mask");
			String and = atts.getValue("and");
			String index = atts.getValue("index");
			String bodyType = atts.getValue("bodyType");
			String bodyInstance = atts.getValue("body_instance");
			String substring = atts.getValue("substring");
			String length = atts.getValue("length");
			String ancestor = atts.getValue("ancestor");
			String escape = atts.getValue("escape");
			if (type != null) {
				if (logOpIndex > -1 || 
						(curCompOp != null && 
								(!curCompOp.getOperator().equalsIgnoreCase("digest"))) ||
						 curMod != null ||
						curVar != null ||
						curAssign != null ||
						filterTag ||
						// Allow to or from with ip or port
						((toParent || fromParent) && (ipFlag || portFlag) )) {
					if (curMsgRef == null) {
						// Verify that the parser understands reference type
						if (validMsgRef(type)) {
							if (type.equalsIgnoreCase(MsgRef.SIP_MSG_TYPE)) {
								if (bodyType != null && bodyType.equals("text"))
									curMsgRef = new SIPBodyRef(type.toLowerCase(), false, true);
								else
									curMsgRef = new SIPRef(type.toLowerCase());
							}
							else if  (type.endsWith("+xml") ||
								type.equals("simple-message-summary")) {
								String xmlValue = atts.getValue("xml_value");
								boolean xml = false;
								if (xmlValue != null &&
										(xmlValue.equalsIgnoreCase("true") ||
										xmlValue.equalsIgnoreCase("enable") ||
										xmlValue.equalsIgnoreCase("on")))
									xml = true;
								SIPBodyRef bodyRef = new SIPBodyRef(type.toLowerCase(), xml, false);
								curMsgRef = bodyRef;
								if (ancestor != null) {
									if (ancestor.equalsIgnoreCase("parent")) 
										bodyRef.setParent();
									else if  (ancestor.equalsIgnoreCase("grandparent")) 
										bodyRef.setGrandparent();
									else {
										String msg = qName + "'s ancestor attribute contains an invalid value. " +
										" Valid values are parent or grandparent.";
										throw new PC2XMLException(fileName,msg, l);
									}
										
								}
							}
							else if (type.equalsIgnoreCase(MsgRef.SDP_MSG_TYPE)) {
								curMsgRef = new SDPRef(type.toLowerCase());
							}
							else if (type.equalsIgnoreCase(MsgRef.PLATFORM_MSG_TYPE)) {
								curMsgRef = new PlatformRef(type.toLowerCase());
							}
							else if (type.equalsIgnoreCase(MsgRef.UTILITY_MSG_TYPE)) {
								UtilityRef utilRef = new UtilityRef(type.toLowerCase());
								curMsgRef = utilRef;
								if (index != null) {
									StringTokenizer tokens = new StringTokenizer(index);
									Integer [] indexes = new Integer[tokens.countTokens()];
									int dim = 0;
									while (tokens.hasMoreTokens()) {
										String token = tokens.nextToken();
										if (token.matches("[0-9]+")) {
											indexes[dim] = new Integer(token);
										}
										else {
											String msg = qName + "'s index attribute contains an invalid value in the " +
											dim + " dimension. " +
											" Valid values are 0 or greater integer values with the exception of one" +
											" wildcard (*) value.";
											throw new PC2XMLException(fileName,msg, l);
										}
										dim++;
									}
									ArrayIndex ai = new ArrayIndex(indexes);
									ArrayRef ar = new ArrayRef(ai);
									utilRef.setArrayReference(ar);
								}
							}
							else if (type.equalsIgnoreCase("extensions")) {
								curMsgRef = new ExtensionRef(type.toLowerCase());
							}
							else if (type.equalsIgnoreCase("event")) {
								curMsgRef = new EventRef(type.toLowerCase());
								lookForEvent = true;
							}
							else if (type.equalsIgnoreCase(MsgRef.STUN_MSG_TYPE)) {
								curMsgRef = new StunRef(type);
							}
							else if  (type.equalsIgnoreCase(MsgRef.RTP_MSG_TYPE)) {
								curMsgRef = new RTPRef(type.toLowerCase());
							}
																					
							if (msgInstance != null && validInstance(msgInstance)) {
								if (validInstance(msgInstance))
										curMsgRef.setMsgInstance(msgInstance);
								else {
									String msg =  qName + " tag's msg_instance attribute is set to an invalid value.";
									throw new SAXParseException(msg, l);
								}
							}
							
							if (hdrInstance != null) {
								if (validInstance(hdrInstance))
									curMsgRef.setHdrInstance(hdrInstance);
								else {
									String msg =  qName + " tag's hdr_instance attribute is set to an invalid value.";
									throw new SAXParseException(msg, l);
								}
							}
							
							if (bodyInstance != null) {
								if (validInstance(bodyInstance))
									curMsgRef.setBodyInstance(bodyInstance);
								else {
									String msg =  qName + " tag's body_instance attribute is set to an invalid value.";
									throw new SAXParseException(msg, l);
								}
							}
							
							if (curTag.equals("add_ref")) { 
								if (value != null) {
									try {
										if (value.contains(".")) {
											double v = Double.parseDouble(value);
											curMsgRef.setAddRef(v);
										}
										else {
											int v = Integer.parseInt(value);
											curMsgRef.setAddRef(v);
										}
									}
									catch (Exception e) {
										throw new PC2XMLException(fileName,e.getMessage(), l);
									}
								}
								else {
									curMsgRef.setAddRef(1);

								}
							}
							else if (curTag.equals("subtract_ref")) {
								if (value != null) {
									
									try {
										if (value.contains(".")) {
											double v = Double.parseDouble(value);
											curMsgRef.setAddRef(v);
										}
										else {
											int v = Integer.parseInt(value);									
											curMsgRef.setSubRef(v);
										}
									}
									catch (Exception e) {
										throw new PC2XMLException(fileName,e.getMessage(), l);
									}
								}
								else {
									curMsgRef.setSubRef(1);
								}
							}
							else if (curTag.equals("queue")) {
								curMsgRef.setQueueRef();
							}
							if (fsmRef != null) {
								ListIterator<FSM> iter = fsms.listIterator();
								while (iter.hasNext()) {
									FSM f = iter.next();
									if (f.getName().equals(fsmRef)) 
										curMsgRef.setUID(f.getUID());
								}
								if (curMsgRef.getUID() == 0) {
									IncompleteMsgRef imr = new IncompleteMsgRef(fsmRef, curMsgRef, fileName, l.getLineNumber() );
									unresolvedTable.add(imr);
								}
							}
							else 
								curMsgRef.setUID(curFSM.getUID());

							if (mask != null && mask.matches(maskPattern)) {
								boolean op = false;
								if (and != null && and.equalsIgnoreCase("true"))
									op = true;
								curMsgRef.setBinaryRef(mask, op);
							}
							
							if (escape != null) {
								boolean enabled = SystemSettings.resolveBooleanSetting(escape);
								if (enabled) {
									curMsgRef.setEscape(enabled);
								}
							}
							
							
							if (substring != null) {
								parseSubstringAttr(qName, curMsgRef, substring);
							}
							
							if (length != null && 
									(length.equalsIgnoreCase("true" ) ||
											length.equalsIgnoreCase("on"))) {
								curMsgRef.setLengthFlag(true);
							}
						}
						else {
							String msg =  qName + " tag contains an unrecognizable message type(" + type + ").";
							throw new SAXParseException(msg, l);
						}
					}
					else {
						String msg =  qName + " tag can only appear as a child to a " +
								" logical operator, comparison operator, modifier, variable, filter, or an assign" +
								". The parser found tag as a child to a different tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  qName + " tag can only appear as a child to a " +
							" logical operator, comparison operator, modifier, variable, filter or an assign." +
							"The parser found tag as a child to a different tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag does not contain the type attribute.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not contain the minimum number of attributes (1).";
			throw new PC2XMLException(fileName,msg, l);
		}		
	}
	/**
	 * Parses the NE element. 
	 * 
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @return true if valid, false otherwise.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private boolean parseNetworkElements(String qName, Attributes atts) throws SAXException {
		String simType = atts.getValue("sim_type");
		String ele = atts.getValue("elements");
		String sExt = atts.getValue("ext_supported");
		String rExt = atts.getValue("ext_require");
		String dExt = atts.getValue("ext_disable");
		String t = atts.getValue("targets");
		LinkedList<String> elements = null;
		LinkedList<String> targets = null;
		if (simType == null) {
			String msg =  "sim-type is a required attribute of NE," +
			" parser did not find an attribute of this type";
			throw new PC2XMLException(fileName,msg, l);
		}
		else {
			if (!(simType.equals("orig") || simType.equals("term")))  {
				String msg =  "sim-type can only be set to 'orig' or 'term'." +
						" Value is set to " + simType;
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		if (ele == null){
			String msg =  "elements is a required attribute of NE," +	
			" parser did not find an attribute of this type";
			throw new PC2XMLException(fileName,msg, l);
		}
		else {
			elements = new LinkedList<String>();
			StringTokenizer tokens = new StringTokenizer(ele);
			int numTokens = tokens.countTokens();
			for (int i = 0; i < numTokens; i++) {
				String token = tokens.nextToken();
				elements.add(token);
				if (token.equals("DUT")) {
					Properties dut = SystemSettings.getSettings(token);
					if (dut != null) {
						String device = dut.getProperty(SettingConstants.DEVICE_TYPE);
						if (device != null)
							elements.add(device+"0");
					}
						
				}
			}
		}
		if (t == null || t.length() < 3) {
			String msg =  "targets is a required attribute of NE," 
					+ " the parse did not find an attribute of this type or " 
					+ " it wasn't at least 3 characters in length";
			throw new PC2XMLException(fileName,msg, l);
		}
		else {
			targets = new LinkedList<String>();
			StringTokenizer tokens = new StringTokenizer(t);
			int numTokens = tokens.countTokens();
			for (int i = 0; i < numTokens; i++) {
				String target = tokens.nextToken();
				targets.add(target);
			}
		}
		LinkedList<String> supported = parseExtensions(sExt);
		LinkedList<String> require = parseExtensions(rExt);
		LinkedList<String> disable = parseExtensions(dExt);
		NetworkElements nes = new NetworkElements(simType, elements, targets);
		nes.setSupportedExtensions(supported);
		nes.setRequireExtensions(require);
		nes.setDisableExtensions(disable);
		if (curFSM != null) {
			curFSM.setNetworkElements(nes);
			return true;
		}
		return false;
		
	}
	

	

	



	/**
	 * Parses the "comparison operator" elements.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseOperator(String qName, Attributes atts) throws SAXException {
		if (qName.equals("eq") || qName.equals("neq") || 
		        qName.equals("contains") || qName.equals("dnc") || qName.equals("isDate")) { 
			if (atts.getLength() > 1) {
				String msg =  qName + " tag does not contain the required number of attributes (0 or 1).";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else if (atts.getLength() != 0) {
			String msg =  qName + " tag does not contain the required number of attributes (0).";
			throw new PC2XMLException(fileName,msg, l);
		}	
		if (curResponse != null || curVerify != null) {
			if (curIf != null || curVerify != null || whereTag) {
				if (curCompOp == null) {
					curCompOp = new ComparisonOp(qName);
					String ignoreCase = atts.getValue("ignoreCase");
					if (ignoreCase != null) {
						if (ignoreCase.equalsIgnoreCase("true")) {
							curCompOp.setIgnoreCase(true);
						} else if (ignoreCase.equalsIgnoreCase("false")) {
							curCompOp.setIgnoreCase(false);
						}
					}
				
					String dateFormat = atts.getValue("format");
					if (dateFormat != null)
					    curCompOp.setDateFormat(dateFormat);
				}
				else {
					String msg =  qName + " tag received as a child to another comparison element.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
				" bit_and, bit_or, if, elseif, verify, an and, or an or tag. " + 
				". The parser found tag as a child to a different element.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag can only appear as a child to a " +
			" response or verify tag. " + 
			". The parser found tag as a child to a different element.";
			throw new PC2XMLException(fileName,msg, l);
		}
				
	}

	/**
	 * Parses the <parser_filter element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseParserFilter(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 1) {
			if (parserFilter == null &&
					curCapture != null &&
					curCapture.isParse()) {
				String ip = atts.getValue("ip");
				String protocol = atts.getValue("protocol");
				String port = atts.getValue("port");
				String limit = atts.getValue("limit_to");
				String msgType = atts.getValue("msgtype");
				String clientMac = atts.getValue("clientMAC");
				if (protocol == null || PDMLTags.isCaptureType(protocol)) {
					parserFilter = new ParserFilter(ip, protocol, port, limit);
					if (msgType != null) {
						if (protocol != null) {
							if (protocol.equals(PDMLTags.BOOTP_PROTOCOL)) {
								if (PDMLTags.isBootpMsgType(msgType))
									parserFilter.setMsgType(msgType);
							}
							else if (protocol.equals(PDMLTags.DHCPv6_PROTOCOL)) {
								if (PDMLTags.isDHCPv6MsgType(msgType))
									parserFilter.setMsgType(msgType);
							}
							else if (protocol.equals(PDMLTags.ICMPV6_PROTOCOL)) {
								if (PDMLTags.isICMPv6MsgType(msgType))
									parserFilter.setMsgType(msgType);
							}
							else if (protocol.equals(PDMLTags.DNS_PROTOCOL)) {
								if (PDMLTags.isDNSMsgType(msgType))
									parserFilter.setMsgType(msgType);
							}
							else if (protocol.equals(PDMLTags.SNMP_PROTOCOL)) {
								if (PDMLTags.isSNMPMsgType(msgType))
									parserFilter.setMsgType(msgType);
							}
							else if (protocol.equals(PDMLTags.TFTP_PROTOCOL)) {
								if (PDMLTags.isTFTPMsgType(msgType))
									parserFilter.setMsgType(msgType);
							}
							else if (protocol.equals(PDMLTags.TOD_PROTOCOL)) {
								if (PDMLTags.isTODMsgType(msgType))
									parserFilter.setMsgType(msgType);
							}
							else if (protocol.equals(PDMLTags.SYSLOG_PROTOCOL)) {
							    if (PDMLTags.isSyslogMsgType(msgType))
							        parserFilter.setMsgType(msgType);
							}
							else {
								String msg = qName + 
								" tag's msgtype attribute is not a valid value for protocol(" + protocol + ").";
								throw new PC2XMLException(fileName,msg, l);
							}
						}
						else {
							String msg = qName + 
							" tag's msgtype attribute can only appear if the protocol attribute is set to a valid value.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					if (clientMac != null)	{
						if (protocol != null) {
							if (protocol.equals(PDMLTags.BOOTP_PROTOCOL) ||
									protocol.equals(PDMLTags.DHCPv6_PROTOCOL)) {
								parserFilter.setClientMacAddr(clientMac);
							}
							else {
								String msg = qName + 
								" tag's clientMAC attribute is not a valid value for protocol(" + protocol + ").";
								throw new PC2XMLException(fileName,msg, l);
							}
						}
						else {
							String msg = qName + 
							" tag's msgtype attribute can only appear if the protocol attribute is set to a valid value.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
				}
				else {
					String msg = qName + 
					" tag's protocol attribute appears contain an unacceptable value.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg = qName + 
				" tag is only allowed to be a child of a <parse_capture> tag and must have at lease one attribute.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg = qName + " tag is only allowed to be a child of a <parse_capture> tag and must have at lease one attribute.";
			throw new PC2XMLException(fileName,msg, l);
		}	
	}
	
	/**
	 * Parses the pass or fail elements.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parsePassFail(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 0) {
			if (curAction != null) {
				boolean flag = false;
				if (qName.equalsIgnoreCase("pass"))
					flag =true;
				Result r = new Result(flag);
				curAction.addAction(r);
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
					actionParents 
				+ ". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not contain the required number of attributes (0).";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * Parses the Platform configuration version of the msg_ref element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parsePlatformMsgRef(StringTokenizer tokens) throws PC2XMLException {
		PlatformRef platRef = (PlatformRef)curMsgRef;
		
		int count = tokens.countTokens();
		if (count > 0 && count < 3) {
			// The first token should be a network element ID
			// Unfortunately we can't validate content.
			platRef.setNELabel(tokens.nextToken());
			// The next token is a parameter of the network element 
			if (count > 1) {
				platRef.setParameter(tokens.nextToken());
			}
		}
		else {
			String msg =  "platform settings msg_ref needs at least one argument, method name, to be valid and " 
					+ "no more than 3 arguments. Parser identifed " + count + " arguments.";
			throw new PC2XMLException(fileName,msg, l);		
		}
	}

	/**
	 * Parses the prelude element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parsePrelude(String qName, Attributes atts) throws SAXException {
		int length = atts.getLength();
		String once = null;
		for (int i = 0; i < length; i++) {
			String attr = atts.getLocalName(i);
			if (attr.equals("once"))
				once = atts.getValue(attr);
			else {
				String msg = qName + " tag has illegal attribute[" + attr + "].";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		if (curState != null) {
			if (curAction == null && curFSM != null) {
				boolean oneTime = false;
				if (once != null && once.equalsIgnoreCase("true"))
					oneTime = true;
				curAction = new ActionFactory(curFSM.getSubcategory(), oneTime);
			}
			else {
				String msg =  qName + " tag can only appear as a child to a state," +
				" parser found tag as a child to another prelude/postlude tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag can only appear as a child to a state," +
			" parser found tag outside of a state tag.";
			throw new PC2XMLException(fileName,msg, l);
		}
//		}
//		else {
//			String msg =  qName + " tag does not allow an attribute," +
//			" parser found an attribute for this tag.";
//			throw new PC2XMLException(fileName,msg, l);
//		}
	}

	/**
	 * Parses the presenceServer model element 
	 * 
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @return true if valid, false otherwise.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private boolean parsePresenceServer(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 0) {
			if (modelParent && curModel == null) {
				curModel = new PresenceModel();
				curModel.setName(qName);
				hasModel = true;
			}
			else {
				String msg = qName + " can not appear as a child element to another " + qName + " tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not allow an attribute," +
			" Parser found an attribute for this tag.";
			throw new PC2XMLException(fileName,msg, l);
		}
		return true;
	}
	
	/**
	 * Parses the proxy element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseProxy(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 1 && atts.getLength() <= 7) {
			String target = atts.getValue("target");
			String port = atts.getValue("port");
			String stack = atts.getValue("stack");
			String originator = atts.getValue("originator");
			String transport = atts.getValue("transportProtocol");
			String protocol = atts.getValue("protocol");
			String msgInstance = atts.getValue("msg_instance");
			String compact = atts.getValue("compact");
//			String destination = atts.getValue("destination");
			if (curAction != null) {
				if (curProxy == null) {
					curProxy= new Proxy();
					if (target != null) {
						curProxy.setTarget(target);
					}
					if (port != null) {
						try {
							int p = Integer.parseInt(port);
							curProxy.setPort(p);
						}
						catch (Exception e) {
							String msg =  "proxy tag contains an invalid value for port attribute.";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					if (stack != null) {
						curProxy.setStack(stack);
					}
					if (originator != null) {
						curProxy.setOriginator(originator);
					}
					if (transport != null) {
						if (transport.equals("UDP"))
							curProxy.setTransport(Transport.UDP);
						else if (transport.equals("TCP"))
							curProxy.setTransport(Transport.TCP);
						else if (transport.equals("TLS"))
							curProxy.setTransport(Transport.TLS);
						else if (transport.equals("SCTP"))
							curProxy.setTransport(Transport.SCTP);
					}
					if (msgInstance != null)
						curProxy.setMsgInstance(msgInstance);
					if (protocol != null)
						curProxy.setProtocol(protocol);
					if (compact != null) {
						if (compact.equalsIgnoreCase("true"))
							curSend.setCompact(true);
					}
//					if (destination != null)
//						curProxy.setDestination(destination);
				}
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
				actionParents 
				+ ". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
				
		}
		else {
			String msg = null;
			if (atts.getLength() < 1) 
				msg = "proxy tag does not appear to contain the mandatory target attribute.";
			else
				msg = "proxy tag contains more than seven attributes.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	/**
	 * Parses the remove_from element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseRemoveFrom(String qName, Attributes atts) throws SAXException {
		if (containsTemplate) {
			String fsm = atts.getValue("fsm");

			if (fsm != null) {
				curFSM = getFsm(fsm);
				if (curFSM != null)
					rm = true;
				else 
					throw new PC2XMLException(fileName,"The fsm(" + fsm 
							+ ") could not be found in the document to perform remove alterations.", l);
			}
			else {
				String msg = "Invalid add alteration requested. FSM attribute is required field";
				throw new PC2XMLException(fileName,msg,l);
			}
		}
		else {
			throw new PC2XMLException(fileName,
					"Invalid add alteration request. The <template> tag must appear prior to using an <remove> tag.",l);
		}
	}
	
	/**
	 * Parses the response element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseResponse(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 0) {
			if (curState != null) {
				if (curAction == null) {
					if (curResponse == null) {
						curResponse = new Responses();
					}
					else {
						String msg =  "response tag can only appear as a child to a state," +
						" parser found tag as a child to another response tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  "postlude tag can only appear as a child to a state," +
					" parser found tag as a child to another prelude/postlude tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  "postlude tag can only appear as a child to a state," +
				" parser found tag outside of a state tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "postlude tag does not allow an attribute," +
			" parser found an attribute for this tag.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	/**
	 * Parses the retransmit element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseRetransmit(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 2) {
			String protocol = atts.getValue("protocol");
			String msgType = atts.getValue("msgtype");
			String target = atts.getValue("target");
			String port = atts.getValue("port");
			String originator = atts.getValue("originator");
			String transportProtocol = atts.getValue("transportProtocol");
			String stack = atts.getValue("stack");
			String dest = atts.getValue("destination");
			if (protocol != null && msgType != null) {
				if (curAction != null) {
					if (curRetransmit == null) {
						if (validProtocol(protocol)) {
							if (validMsgType(msgType)) {
								curRetransmit = new Retransmit(protocol, msgType);
								if (target != null) {
									if (protocol.equals(MsgRef.UTILITY_MSG_TYPE)) {
										if (UtilityConstants.validUtilityTarget(target)) 
											curRetransmit.setTarget(target);
										else {
											String msg =  "retransmit tag contains an invalid target attribute.";
											throw new PC2XMLException(fileName,msg, l);
										}
									}
									else 
										curRetransmit.setTarget(target);
								}
								else if (protocol.equals(MsgRef.UTILITY_MSG_TYPE)){
									// If the document doesn't assign a target
									// set the utility messages's target to null
									curRetransmit.setTarget(null);
									logger.trace(PC2LogCategory.Parser, subCat, 
											"Setting retransmit's target to null for Utility message.");
								}
								if (port != null) {
									try {
										int p = Integer.parseInt(port);
										curRetransmit.setPort(p);
									}
									catch (Exception e) {
										String msg =  "retransmit tag contains an invalid value for port attribute.";
										throw new PC2XMLException(fileName,msg, l);
									}
								}
								if (originator != null) {
									curRetransmit.setOriginator(originator);
								}
								if (dest != null) {
									curRetransmit.setDestination(dest);
								}
								if (transportProtocol != null) {
									if (transportProtocol.equals("UDP") ||
											transportProtocol.equals("TCP") ||
											transportProtocol.equals("TLS")) {
										curRetransmit.setTransportProtocol(transportProtocol);
									}
								}
								if (stack != null) {
									curRetransmit.setStack(stack);
								}
							}
							else {
								String msg =  "retransmit tag contains an unrecognizable msgtype attribute " +
										msgType + ".";
								throw new PC2XMLException(fileName,msg, l);
							}
						}
						else {
							String msg =  "retransmit tag contains an unrecognizable protocol attribute " +
									protocol + ".";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else {
						String msg =  "retransmit tag can only appear as a child to a " +
								" prelude, postlude, then or else. " +
						" The parser found tag as a child to another send tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  "retransmit tag can only appear as a child to a " +
							" prelude, postlude, then or else. " +
					" The parser found tag as a child to a different tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  "retransmit tag does not contains either the protocol or the msgtype attribute.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "retransmit tag does not contains the minimum number of attributes.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}

	/**
	 * Parses the root element (pc2xml).
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseRoot(String qName, Attributes atts) throws SAXException {
		validRoot = true;
		String nm = atts.getValue("name");
		String descrip = atts.getValue("descrip");
		String number = atts.getValue("number");
		String inspector = atts.getValue("inspector");
		String version = atts.getValue("version");
		if (nm != null && descrip != null && number != null && version != null) {
			doc.setName(nm);
			doc.setDescrip(descrip);
			doc.setNumber(number);
			doc.setVersion(version);
			// Now change the file that we are logging all of the details to
			// using the name of the pc2xml document.
			if (inspector != null && inspector.equalsIgnoreCase("enable")) {
				doc.enableInspector();
			}
			if (resetLogs) {
				try {
					String logFileName = SystemSettings.getInstance().getLogPrependName(nm) + "_ss.log";
					logger.createTestLog(logFileName);
					doc.setLogFileName(logFileName);
				}
				catch (Exception e) {
					logger.fatal(PC2LogCategory.Parser, subCat, 
							"Failed to update to new log file name.", e);
					throw new PC2XMLException(fileName,e.getMessage(), l);
				}
			}
			else {
				logger.info(PC2LogCategory.Parser, subCat, 
						"Parsing script name " + fileName + " v." + doc.getVersion());
			}

		}
		else {
			String msg = "pc2xml tag must contain a version, name, descrip, and number attribute.\n" +
			" version=" + version + " name=" + nm + " descrip=" + descrip + " number=" + number;
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	/**
	 * Parses the SIP message version of msg_ref element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseRTPMsgRef(StringTokenizer tokens) throws PC2XMLException {
		RTPRef stunRef = (RTPRef)curMsgRef;
		int count = tokens.countTokens();
		if (count > 0 && count < 4) {
			String token = tokens.nextToken();
			// The first token should be a valid SIP method name
			if (StunConstants.isRTPType(token)) 
				stunRef.setMethod(token);
			
			// If there are more tokens the next must be a header 
			if (count > 1) {
				stunRef.setHeader(tokens.nextToken());
			}
			
			// If there are more tokens the next must be a parameter
			if (count > 2) {
				stunRef.setParameter(tokens.nextToken());
			}
		}
		else {
			String msg =  "rtp msg_ref needs at least one argument, method name, to be valid and " 
					+ "no more than 3 arguments. Parser identifed " + count + " arguments.";
			throw new PC2XMLException(fileName,msg, l);		
		}
	}
	/**
	 * Parses the send element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseSend(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 2) {
			String protocol = atts.getValue("protocol");
			String msgType = atts.getValue("msgtype");
			String target = atts.getValue("target");
			String port = atts.getValue("port");
			String originator = atts.getValue("originator");
			String transportProtocol = atts.getValue("transportProtocol");
			String stack = atts.getValue("stack");
			String bodies = atts.getValue("bodies");
			String destination = atts.getValue("destination");
			String subscribeType = atts.getValue("subscribeType");
			String compact = atts.getValue("compact");
			String origReq = atts.getValue("origReq");
			String origInstance = atts.getValue("orig_instance");
//			String referFSM = atts.getValue("referFSM");
			if (protocol != null && msgType != null) {
				if (curAction != null) {
					if (curSend == null) {
						if (validProtocol(protocol)) {
							if (validMsgType(msgType)) {
								curSend= new Send(protocol, msgType);
								if (target != null) {
									if (protocol.equals(MsgRef.UTILITY_MSG_TYPE)) {
										if (UtilityConstants.validUtilityTarget(target)) 
											curSend.setTarget(target);
										else {
											String msg =  "send tag contains an invalid target attribute.";
											throw new PC2XMLException(fileName,msg, l);
										}
									}
									else 
										curSend.setTarget(target);
								}
								else if (protocol.equals(MsgRef.UTILITY_MSG_TYPE)){
									// If the document doesn't assign a target
									// set the utility messages's target to null
									curSend.setTarget(null);
									logger.trace(PC2LogCategory.Parser, subCat, 
											"Setting send's target to null for Utility message.");
								}
								if (port != null) {
									try {
										int p = Integer.parseInt(port);
										curSend.setPort(p);
									}
									catch (Exception e) {
										String msg =  "send tag contains an invalid value for port attribute.";
										throw new PC2XMLException(fileName,msg, l);
									}
								}
								if (originator != null) {
									curSend.setOriginator(originator);
								}
								if (transportProtocol != null) {
									if (transportProtocol.equals("UDP"))
										curSend.setTransportProtocol(Transport.UDP);
									
									else if (	transportProtocol.equals("TCP"))
										curSend.setTransportProtocol(Transport.TCP);
									else if (transportProtocol.equals("TLS"))
										curSend.setTransportProtocol(Transport.TLS);
									else if (transportProtocol.equals("SCTP"))
										curSend.setTransportProtocol(Transport.SCTP);
								}
								if (stack != null) {
									curSend.setStack(stack);
								}
								if (bodies != null && protocol.equals(MsgRef.SIP_MSG_TYPE)) {
									StringTokenizer tokens = new StringTokenizer(bodies);
									
									while (tokens.hasMoreElements()) {
										String token = tokens.nextToken();
										if (validBody(token))
											curSend.addBody(token);
										else {
											String msg =  "send tag contains an unrecognizable body attribute value(" +
											token + ").";
											throw new PC2XMLException(fileName,msg, l);
										}
//										if (token.equals("SDP"))
//											curSend.setIncludeSDP(true);
//										else if (token.equalsIgnoreCase("open"))
//											curSend.setIncludeOpen(true);
//										else if (token.equalsIgnoreCase("closed")) {
//											curSend.setIncludeClosed(true);
//										}
//										else if (token.equalsIgnoreCase("message-summary")) {
//											curSend.setIncludeMessageSummary(true);
//										}
									}
								}
								
								if (destination != null) {
									curSend.setDestination(destination);
								}
								
								if (subscribeType != null) {
									curSend.setSubscribeType(subscribeType);
								}
								if (compact != null) {
									if (compact.equalsIgnoreCase("true"))
										curSend.setCompact(true);
								}
								if (protocol.equals("sip") && 
										SIPConstants.isRequestType(origReq)) {
									curSend.setOriginalRequest(origReq);
									if (origInstance != null &&
											validInstance(origInstance)) {
										curSend.setOriginalInstance(origInstance);
									}
								}
								else if (protocol.equals("stun")) {
									String candidate = atts.getValue("candidate");
									String ice = atts.getValue("ice");
									String id = atts.getValue("id");
									String controlling = atts.getValue("controlling");
									if (candidate != null && candidate.equalsIgnoreCase("true"))
										curSend.setUseCandidate(true);
									if (ice != null && ice.equalsIgnoreCase("true")) 
										curSend.setIceLite(true);
									if (id != null) {
										byte [] idBytes = Conversion.hexStringToByteArray(id);
										if (idBytes != null)
											curSend.setTransactionId(idBytes);
									}
									if (controlling != null) {
										try {
											Long value = Long.valueOf(controlling);
											curSend.setIceControlling(value);
										}
										catch (NumberFormatException nfe) {
											
										}
									}
								}
							}
							else {
								String msg =  "send tag contains an unrecognizable msgtype attribute " +
										msgType + ".";
								throw new PC2XMLException(fileName,msg, l);
							}
						}
						else {
							String msg =  "send tag contains an unrecognizable protocol attribute " +
									protocol + ".";
							throw new PC2XMLException(fileName,msg, l);
						}
					}
					else {
						String msg =  qName + " tag can only appear as a child to a " +
						actionParents 
						+ ". The parser found tag as a child to a different tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  qName + " tag can only appear as a child to a " +
					actionParents 
					+ ". The parser found tag as a child to a different tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg = "";
				if (protocol == null && msgType != null) 
					msg += qName + " tag does not contain the protocol attribute. ";
				
				if (protocol != null && msgType == null) 
					msg += qName + " tag does not contain the msgtype attribute.";
							
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "send tag does not contains the minimum number of attributes.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}

	

	

	/**
	 * Parses the SDP body version of the msg_ref element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseSDPMsgRef(StringTokenizer tokens) throws PC2XMLException {
		SDPRef sdpRef = (SDPRef)curMsgRef;
		
		int count = tokens.countTokens();
		if (count > 0 && count < 4) {
			String token = tokens.nextToken();
			// The first token should be a valid SIP method name
			if (SIPConstants.isRequestType(token) ||
					SIPConstants.isGenericType(token)) {
				sdpRef.setMethod(token);
			}
			else if (SIPConstants.isResponseType(token)) {
				StringTokenizer resp = new StringTokenizer(token, "-");
				sdpRef.setStatusCode(resp.nextToken());
				sdpRef.setMethod(resp.nextToken());
			}
			else {
				String msg =  "sdp msg_ref unrecognized method name, " + token + ".";
				throw new PC2XMLException(fileName,msg, l);	
			}
			// If there are more tokens the next must be a SDP header 
			if (count > 1) {
				String header = tokens.nextToken();
			if (SDPConstants.isValidSDPHeader(header)) {
					sdpRef.setHeader(header);
				}
				else {
					String msg =  "sdp msg_ref unrecognized header type, " + header + ".";
					throw new PC2XMLException(fileName,msg, l);	
				}
			}
			// If there are more tokens the next must be a SDP parameter
			if (count > 2) {
				String param = tokens.nextToken();
				if (param != null && 
						SDPConstants.isValidSDPParameter(param)) {
					sdpRef.setParameter(param);
				}
				else {
					String msg =  "sdp msg_ref unrecognized parameter type, " + param + ".";
					throw new PC2XMLException(fileName,msg, l);	
				}
			}
		}
		else {
			String msg =  "sdp msg_ref needs at least one argument, method name, to be valid and " 
					+ "no more than 3 arguments. Parser identifed " + count + " arguments.";
			throw new PC2XMLException(fileName,msg, l);		
		}
	}
	
	/**
	 * Parses the SDP body version of the msg_ref element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseSIPBodyMsgRef(StringTokenizer tokens) throws PC2XMLException {
		SIPBodyRef sipBodyRef = (SIPBodyRef)curMsgRef;
		String type = sipBodyRef.getType();
		
		int count = tokens.countTokens();
		if (count > 0 && count < 4) {
			String token = tokens.nextToken();
			if (SIPConstants.isRequestType(token) ||
					SIPConstants.isGenericType(token)) {
				sipBodyRef.setMethod(token);
			}
			else if (SIPConstants.isResponseType(token)) {
				StringTokenizer resp = new StringTokenizer(token, "-");
				sipBodyRef.setStatusCode(resp.nextToken());
				sipBodyRef.setMethod(resp.nextToken());
			}
			else {
				String msg =  type + " msg_ref unrecognized method name, " + token + ".";
				throw new PC2XMLException(fileName,msg, l);	
			}
				
			// If there are more tokens the next must be an XML tag 
			if (count > 1) {
				String header = tokens.nextToken();
				if (header != null)
					sipBodyRef.setHeader(header);
			}
			// If there are more tokens the next must be a SDP parameter
			if (count > 2) {
				String param = tokens.nextToken();
				if (param != null) {
					sipBodyRef.setParameter(param);
				}
			}
		}
		else {
			String msg =  type + " msg_ref needs at least one argument, method name, to be valid and " 
					+ "no more than 3 arguments. Parser identifed " + count + " arguments.";
			throw new PC2XMLException(fileName,msg, l);		
		}
	}
	
	/**
	 * Parses the services element. 
	 * 
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @return true if valid, false otherwise.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private boolean parseServices(String qName, Attributes atts) throws SAXException {
		String clients = atts.getValue("clients");
		LinkedList<String> elements = null;
		if (clients == null) {
			String msg =  "clients is a required attribute of services," +
			" parser did not find an attribute of this type";
			throw new PC2XMLException(fileName,msg, l);
		}
		
		if (add) {
			ListIterator<String> iter = curFSM.getServices();
			elements = new LinkedList<String>();
			while (iter.hasNext()) {
				elements.add(iter.next());
			}
		}
		else
			elements = new LinkedList<String>();

	
		if (curFSM != null) {
			curFSM.setServices(elements);
			return true;
		}
		return false;
		
	}
	
	/**
	 * Parses the session model element 
	 * 
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @return true if valid, false otherwise.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private boolean parseSession(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 0) {
			if (modelParent && curModel == null) {
				curModel = new Model();
				curModel.setName(qName);
				hasModel = true;
			}
			else {
				String msg = qName + " can not appear as a child element to another " + qName + " tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not allow an attribute," +
			" Parser found an attribute for this tag.";
			throw new PC2XMLException(fileName,msg, l);
		}
		return true;
	}


	
	/**
	 * Parses the SIP message version of <msg_ref> element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseSIPMsgRef(StringTokenizer tokens) throws PC2XMLException {
		SIPRef sipRef = (SIPRef)curMsgRef;
		int count = tokens.countTokens();
		if (count > 0 && count < 4) {
			String token = tokens.nextToken();
			// The first token should be a valid SIP method name
//if (token.equals("18x-INIVITE")) {
//	int glh =0;
//}
			if (SIPConstants.isRequestType(token) ||
					SIPConstants.isGenericType(token)) {
				sipRef.setMethod(token);
			}
			else if (SIPConstants.isResponseType(token)) {
				StringTokenizer resp = new StringTokenizer(token, "-");
				sipRef.setStatusCode(resp.nextToken());
				sipRef.setMethod(resp.nextToken());
			}
			else {
				String msg =  "sip msg_ref has an invalid method value in the reference.";
				throw new PC2XMLException(fileName,msg, l);	
			}
			
			// If there are more tokens the next must be a header 
			if (count > 1) {
				sipRef.setHeader(tokens.nextToken());
			}
			// If there are more tokens the next must be a parameter
			if (count > 2) {
				// Because of the dot structure we use for references
				// we encounter an issue for the +sip.instance parameter
				// within gruu. Therefore, the XML syntax uses 
				// sip-instance to represent this parameter, but
				// we must convert it to +sip.instance when parsing the
				// document.
				String param = tokens.nextToken();
				if (param.equals("sip-instance"))
					param = "+sip.instance";
				sipRef.setParameter(param);
			}
		}
		else {
			String msg =  "sip msg_ref needs at least one argument, method name, to be valid and " 
					+ "no more than 3 arguments. Parser identifed " + count + " arguments.";
			throw new PC2XMLException(fileName,msg, l);		
		}
	}
	/**
	 * Parses the send element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseStream(String qName, Attributes atts, boolean stop) throws SAXException {
		if (stop) {
			if (atts.getLength() < 1) {
				String msg =  qName 
					+ " tag does not contains the minimum number of attributes(1).";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			if (atts.getLength() < 3) {
				String msg =  qName 
					+ " tag does not contains the minimum number of attributes(1).";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		
		String name = atts.getValue("name");
		String file = atts.getValue("file");
		String format = atts.getValue("format");
		
		if (name != null) {
			if (curStream == null) {
				File f = null;
				if (file != null)
					f = new File (file);
				try {
					curStream = new Stream (name, f, format, stop);
				}
				catch (IllegalArgumentException iae) {
					throw new PC2XMLException(file, iae.getMessage(), l);
				}
			}
			else {
				String msg =  qName 
				+ " appears to be a child of another 'start_stream' or 'stop_stream' element.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName 
			+ " tag does not contains the mandatory attribute 'name'.";
			throw new PC2XMLException(fileName,msg, l);
		}
		
		
	}
	/**
	 * Parses the state element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseState(String qName, Attributes atts) throws SAXException {
		String ID = atts.getValue("ID");
		String ES = atts.getValue("ES");
		String offHookOK = atts.getValue("offHookOK");
		String provFile = atts.getValue("provFile");
		String policyFile = atts.getValue("policyFile");
		String nextState = atts.getValue("nextState");
		if (ID != null) {
			if (ES != null && 
					ES.equalsIgnoreCase("true")) {
				curState = new EndSessionState(ID, curFSM);
				if (offHookOK != null && 
						(offHookOK.equalsIgnoreCase("true") ||
						offHookOK.equalsIgnoreCase("enable") ||
						offHookOK.equalsIgnoreCase("on"))) {
					((EndSessionState)curState).setIgnoreOffHook();
				}	
			}
			else if (provFile != null &&
					policyFile != null &&
					nextState  != null &&
					ES == null) {
				ProvisioningData pd = new ProvisioningData(ID, policyFile, provFile);
				curState = new AutoProvState(ID, curFSM, pd, nextState);
			}
			else
				curState = new State(ID, curFSM);
			String timer = atts.getValue("timer");
			if (timer != null) {
				try {
					int timeout = Integer.parseInt(timer);
					curState.setTimeout(timeout);
				}
				catch (Exception e) {
					String msg = qName + " tag contains an invalid value for the timer attribute." +
					" Value must be an integer.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			String timerType = atts.getValue("timerType");
			if (timerType != null) {
				if (timerType.equalsIgnoreCase("reuse") ||
						timerType.equalsIgnoreCase("persistent") ||
						timerType.equalsIgnoreCase("once")) {
					curState.setStateTimerType(timerType.toUpperCase());
				}
			}
				
		}
		else {
			String msg =  "state tag requires an ID attribute," +
			" parser did not find an attribute of this tag.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}

	/**
	 * Parses the sleep element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseSleep(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 1) {
			String time = atts.getValue("time");
			if (time != null) {
				if (curAction != null) {
					try {
						int t = Integer.parseInt(time);
						Sleep s = new Sleep(t);
						curAction.addAction(s);
					}
					catch (Exception e) {
						e.printStackTrace();
						String msg = "sleep tag time attribute is not an integer number.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
				else {
					String msg =  qName + " tag can only appear as a child to a " +
					actionParents 
					+ ". The parser found tag as a child to a different tag.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  "sleep tag does not contain the time attribute.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  "sleep tag does not contain the required number of attributes (1).";
			throw new PC2XMLException(fileName,msg, l);
		}
		
	}
	/**
	 * Parses the states element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseStates(Attributes atts) throws SAXException {
		String initState = atts.getValue("initialstate");
		if (initState != null ) {
			curFSM.setInitialState(initState);
		}
		else {
			String msg = "states tag requires an initialstate attribute," +
			" parser did not find an attribute of this tag.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * Parses the SIP message version of msg_ref element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseStunMsgRef(StringTokenizer tokens) throws PC2XMLException {
		StunRef stunRef = (StunRef)curMsgRef;
		int count = tokens.countTokens();
		if (count > 0 && count < 4) {
			String token = tokens.nextToken();
			// The first token should be a valid SIP method name
			if (StunConstants.isStunType(token)) {
				stunRef.setMethod(token);
			}
			else if (StunConstants.isErrorType(token)) {
				StringTokenizer resp = new StringTokenizer(token, "-");
				stunRef.setStatusCode(resp.nextToken());
				stunRef.setMethod(resp.nextToken());
			}
			

			// If there are more tokens the next must be a header 
			if (count > 1) {
				stunRef.setHeader(tokens.nextToken());
			}
			
			// If there are more tokens the next must be a parameter
			if (count > 2) {
				stunRef.setParameter(tokens.nextToken());
			}
		}
		else {
			String msg =  "stun msg_ref needs at least one argument, method name, to be valid and " 
					+ "no more than 3 arguments. Parser identifed " + count + " arguments.";
			throw new PC2XMLException(fileName,msg, l);		
		}
	}
	
	/**
	 * Parses the transition element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseSubstringAttr(String qName, MsgRef mr, String substring) throws SAXException {
		if (substring != null) {
			StringTokenizer tokens = new StringTokenizer(substring, " ");
			int numTokens = tokens.countTokens();
			if (numTokens == 1) {
				// This is the substring starting at token to the end of the string
				// Verify the value is a positive number
				try {
					String token = tokens.nextToken();
					String first = validSubstring(token);
					if (first != null) {
						if (first.equals(token)) {
							// This means first the token should have
							// only been a number
							mr.setFirstIsOffsetFromLength(false);
							mr.setFirstChar(token);
						}
						else {
							// This means first the token if an offset 
							// from length so set the offset flag
							mr.setFirstIsOffsetFromLength(true);
							mr.setFirstChar(first);
						}
						
					}
				}
				catch (NumberFormatException nfe) {
					String msg =  qName + "'s substring attribute must be 0 or greater integer, two 0 " +
							"or greater integers separated by a space, or the variable lenght- some positive integer.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else if (numTokens == 2) {
				// This is the substring starting at the first token to the second token
				// Verify the value is a positive number
				String token = tokens.nextToken();
				String first = validSubstring(token);
				
				try {
					if (first != null) {
						if (first.equals(token)) {
							// This means first the token should have
							// only been a number
							curMsgRef.setFirstIsOffsetFromLength(false);				
						}
						else {
							// This means first the token if an offset 
							// from length so set the offset flag
							curMsgRef.setFirstIsOffsetFromLength(true);
						}
						curMsgRef.setFirstChar(token);
					}
					token = tokens.nextToken();
					String last = validSubstring(token);
					if (last != null) {
						if (last.equals(token)) {
							// This means last the token should have
							// only been a number
							curMsgRef.setLastIsOffsetFromLength(false);
							curMsgRef.setLastChar(token);
						}
						else {
							// This means last the token if an offset 
							// from length so set the offset flag
							curMsgRef.setLastIsOffsetFromLength(true);
							curMsgRef.setLastChar(last);
						}
						
					}
				}
				catch (NumberFormatException nfe) {
					String msg =  qName + "'s substring attribute must be 0 or greater integer or two 0 " +
							"or greater integers separated by a space.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + "'s substring attribute must be 0 or greater integer or two 0 " +
					"or greater integers separated by a space.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
	}

	/**
	 * Parses the transition element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseTemplate(String qName, Attributes atts, String name) throws SAXException {
		deltaScript = true;
		String tempName = atts.getValue("name");
		String prepend = atts.getValue("prepend");
		String tempFile = atts.getValue("file");
		String sipStack = atts.getValue("sipStack");
		String file = null;
		if (tempFile.contains("$")) {
			int index = tempFile.indexOf("$");
			if (index > -1) {
				// Replace environment variable with platform
				// setting if it exists otherwise replace with
				// current directory
				String var = null;
				String fileSep = null;
				if (tempFile.contains("/"))
					fileSep = "/";
				else if (tempFile.contains("\\"))
					fileSep = "\\";
				int end = tempFile.indexOf(fileSep, index);
				if (end != -1)
					var = tempFile.substring(index+1, end);
				
				if (var != null) {
					Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
					String path = platform.getProperty(var);
					if (path != null && end != -1) {
							file = tempFile.substring(0, index) +
								path + tempFile.substring(end);
					}
					else if (end != -1) {
							file = tempFile.substring(0, index) +
								"." + tempFile.substring(end);
					}
				}
			}
		}
		else
			file = tempFile;
		
		File f = new File(file);
		if (f.exists() && f.canRead() && f.isFile() &&
				f.getName().endsWith(".xml")) {
			if (tempName != null && prepend != null) 
				throw new PC2XMLException(fileName,"The attributes prepend and name are may not appear in the same template tag.", l);
			
			logger.info(PC2LogCategory.Parser, subCat, "Attempting to read and parse template " + tempFile);
			TSParser tmpParser = new TSParser(false);
			try {
				TSDocument tmpDoc = tmpParser.parse(f.getAbsolutePath());
				if (tmpDoc != null && tmpDoc.getFsms().size() > 0) {
					if (tmpDoc.getFsms().size() > 1 && tempName != null) 
						throw new PC2XMLException(fileName,"The name attribute can not be used in conjuction with a file attribute"
								+ " set to an external document with more than one FSM.", l );
					ListIterator<FSM> iter = tmpDoc.getFsms().listIterator();
					while (iter.hasNext()) {
						FSM fsm = iter.next();
						testDuplicateFSM(fsm);
						if (tempName != null) {
							logger.info(PC2LogCategory.Parser, subCat,
									"Renaming template fsm(" + fsm.getName() 
									+ ") to (" + tempName + ").");
							fsm.setName(tempName);
						}
						else if (prepend != null){
							logger.info(PC2LogCategory.Parser, subCat,
									"Prepending string(" + name + ") to form new fsm name(" + fsm.getName() 
									+ ") for template.");
							fsm.setName(prepend + fsm.getName());
						}
						// Next we need to copy of all of the properties from the template
						doc.platformChanges = tmpDoc.platformChanges;
						if (sipStack != null) {
							fsm.setSipStack(sipStack);
						}
						if (fsm.hasInitialState()) {
							fsms.addLast(fsm);
							containsTemplate = true;
							hasFSM = true;
							hasNE = true;
							hasModel = true;
							hasState = true;
							curFSM = null;
							logger.info(PC2LogCategory.Parser, subCat, "Parsing complete for template " + tempFile);
							logger.info(PC2LogCategory.Parser, subCat, 
									"Starting alterations defined in document " + fileName);
						}
						else  {
							throw new PC2XMLException(fileName, "FSM(" + fsm.getName() + ") doesn't contain the initial state.", l );
						}
						
					}
				}
				else {
					String msg = "The referred template(" + f + ") doesn't contain any FSMs.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			catch (IOException io) {
				throw new PC2XMLException(fileName,io.getMessage(), l);
			}
		}
		else {
			String msg = "The template " + f.getAbsolutePath();
			if (!f.exists()) 
				msg += " doesn't exist.";
			else if (!f.isFile()) 
				msg += " isn't a file.";
			else if (!f.canRead())	
				msg += " isn't readible.";
			else if (!f.getName().endsWith(".xml"))
				msg += " doesn't end with extension .xml";

			throw new PC2XMLException(fileName,msg, l);
		}
	}
	/**
	 * Parses the transition element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseTransition(String qName, Attributes atts) throws SAXException {
		String from = atts.getValue("from");
		String to = atts.getValue("to");
		String event = atts.getValue("event");
		if (from == null && curState != null) {
			from = curState.getName();
		}
		if (rm) {
			State temp = curFSM.getState(from);
			if (temp != null && from != null && to != null && event != null) {
				Transition t = temp.removeTransition(event);
				if (t == null) {
					logger.warn(PC2LogCategory.Parser, subCat, "Parser couldn't remove transition from state(" + 
							from + ") to(" + to + ") on event(" + event + ") because it could not be found.");
				}
			}
		}
		else {
			try {
//				if (!validState(from)) {
				if (from == null) {
					String msg =  "transition's from attribute must be a state previously defined in the document," +
					" parser did not find a previously defined state named " + from;
					throw new PC2XMLException(fileName,msg, l);	
				}
//				else if (!validState(to)) {
				else if (to == null) {
					String msg =  "transition's to attribute must be a state previously defined in the document," +
					" parser did not find a previously defined state named " + to;
					throw new PC2XMLException(fileName,msg, l);	
				}
				else if (!validEvent(event)) {
					String msg =  "transition's event attribute must be a system recognizable event," +
					" parser did not find an event with the name " + event;
					throw new PC2XMLException(fileName,msg, l);		
				}
				else {
					State s = curFSM.getState(from);
					if (s == null) {
						if (curState.transitionExists(event)) {
							logger.info(PC2LogCategory.Parser, subCat, 
									"A transition in state(" + from + ") already exists for event(" +
									event + "). It is being overwritten with a new transition to state(" +
									to + ").");
						}
						Transition t = new Transition(from, to, event);
						curState.addTransition(t);
					}
					else if (curFSM.transitionExists(from, event)) {

						logger.info(PC2LogCategory.Parser, subCat, 
								"A transition in state(" + from + ") already exists for event(" +
								event + "). It is being overwritten with a new transition to state(" +
								to + ").");
						State temp = curFSM.getState(from);
						temp.removeTransition(event);
						curFSM.addTransition(from, to, event);
					}
					else {
						curFSM.addTransition(from, to, event);
					}
					
				}
			}
			catch (NullPointerException npe) {
				String msg = "One of the attributes is null. from=" + from + " to=" + to + " event=" + event;
				throw new PC2XMLException(fileName,msg, l);
			}
		}
	}

	/**
	 * Parses the Utility message version of the msg_ref element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseUtilityMsgRef(StringTokenizer tokens) throws PC2XMLException {
		UtilityRef utilRef = (UtilityRef)curMsgRef;

		int count = tokens.countTokens();
		//	No characters are currently valid for Utility message references
		if (count > 0 && count < 4) {
			String token = tokens.nextToken();
			utilRef.setMsgType(token);
			if (count > 1)
				utilRef.setHeader(tokens.nextToken());
		}
		else if (count >= 4) {

			String msg =  "utility msg_ref does not currently support more than 3 arguments. " 
				+ " Parser identifed " + count + " arguments.";
			throw new PC2XMLException(fileName,msg, l);	
		}
		else {
			String msg =  "utility msg_ref must contain at least one argument." 
				+ " Parser identifed " + count + " arguments.";
			throw new PC2XMLException(fileName,msg, l);	
		}
	}

	
	/**
	 * Parses the var element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseVar(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() == 1) {
			if (curResponse != null || curAction != null) {
				String name = atts.getValue("name");
				if (name != null) {
					curVar = new Variable(name);
				}
				else {
					String msg = qName + " tag doesn't contain the required name attribute.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
						" response tag. " + 
				". The parser found tag as a child to a different tag.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg = qName + " tag does not contain the required number of attributes (1).";
			throw new PC2XMLException(fileName,msg, l);
		}	
	}
	
	/**
	 * Parses the var_ref element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseVarRef(String qName, Attributes atts) throws SAXException {
		if (atts.getLength() >= 1 && atts.getLength() <= 5) {
			String name = atts.getValue("name");			String index = atts.getValue("index");
			String mask = atts.getValue("mask");
			String and = atts.getValue("and");
			String protocol = atts.getValue("protocol");
			String hdr = atts.getValue("hdr");
			String param = atts.getValue("param");
			String substring = atts.getValue("substring");
			String hdr_instance = atts.getValue("hdr_instance");
			if ((logOpIndex > -1 || 
					curMod != null ||
					filterTag ||
					curVar != null ||
					toParent || 
					(curCompOp != null && 
					(!curCompOp.getOperator().equalsIgnoreCase("digest")))) &&
					curMsgRef == null) {
				if (name != null) {
					if (index != null) {
						StringTokenizer tokens = new StringTokenizer(index);
						Integer [] indexes = new Integer[tokens.countTokens()];
						int dim = 0;
						while (tokens.hasMoreTokens()) {
							String token = tokens.nextToken();
							if (token.matches("[0-9]+")) {
								indexes[dim] = new Integer(token);
							}
							else {
								String msg = qName + "'s index attribute contains an invalid value in the " +
								dim + " dimension. " +
								" Valid values are 0 or greater integer values with the exception of one" +
								" wildcard (*) value.";
								throw new PC2XMLException(fileName,msg, l);
							}
							dim++;
						}
						curMsgRef = new VarRef(name, indexes);
					}
					else 
						curMsgRef = new VarRef(name);
					
					if (mask != null && mask.matches(maskPattern)) {
						boolean op = false;
						if (and != null && and.equalsIgnoreCase("true"))
							op = true;
						curMsgRef.setBinaryRef(mask, op);
					}
					
					if (protocol != null && 
							hdr != null && 
							validProtocol(protocol)) {
						VarRef vr = (VarRef)curMsgRef;
						vr.setProtocol(protocol);
						vr.setHdr(hdr);
						vr.setParam(param);
						if (hdr_instance == null)
							vr.setHdrInstance(MsgQueue.FIRST);
						else
							vr.setHdrInstance(hdr_instance);
					}
					else if ((protocol != null && hdr == null) ||
								(protocol == null && hdr != null)) {
						String msg = qName + " tag must contain both the protocol and hdr attributes if either exists for the tag.";
						throw new PC2XMLException(fileName,msg, l);
					}
					
					if (substring != null) {
						parseSubstringAttr(qName, curMsgRef, substring);
					}
					
				}
				else {
					String msg = qName + " tag must contain the name attribute.";
					throw new PC2XMLException(fileName,msg, l);
				}
			}
			else {
				String msg =  qName + " tag can only appear as a child to a " +
						" variable, logical operator, comparison operator, filter, a to tag, or a modifier. " +
				". The parser found tag as a child to a different element.";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else {
			String msg =  qName + " tag does not contain the required number of attributes (1-4).";
			throw new PC2XMLException(fileName,msg, l);
		}			
	}
	
	/**
	 * Parses the verify element.
	 *  
	 * @param qName - the current tag
	 * @param atts - the current attribute associated with the element.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void parseVerify(String qName, Attributes atts) throws SAXException {
		int length = atts.getLength();
		String step = null;
		String requirements = null;
		String group = null;
		for (int i = 0; i < length; i++) {
			String attr = atts.getLocalName(i);
			if (attr.equals("step")) {
				step = atts.getValue(attr);
			}
			else if (attr.equals("requirements"))
				requirements = atts.getValue(attr);
			else if (attr.equals("group"))
				group = atts.getValue(attr);
			else {
				String msg = qName + " tag has illegal attribute[" + attr + "].";
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		if ((curResponse != null || 
				curAction != null) &&
				curState != null) {
			Verify v = new Verify(curState);
			v.setStep(step);
			v.setRequirements(requirements);
			v.setGroup(group);
			curVerify = v;
		}
		else {
			String msg =  qName + " tag can only appear as a child to a" +
			" prelude, response, or postlude tag. " + 
			". The parser found tag as a child to a different tag.";
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	/**
	 * Removes a state from the current FSM.
	 */
	private void removeState(String ID) throws SAXException {
		if (ID != null) {
			try {
				State s = curFSM.removeState(ID);
				if (s == null)
					throw new PC2XMLException(fileName,"Could not remove the state("
							+ ID + ") from FSM(" 
							+ curFSM.getName() + ") because it wasn't found.", l);
			}
			catch (PC2Exception e)  {
				throw new PC2XMLException(fileName,e.getMessage(), l);
			}
		}
		else 
			throw new PC2XMLException(fileName,"Could not perform remove alteration because ID attribute was not set."
					, l);
	}
	/**
	 * The method attempts convert the two control character representation
	 * of '\n', '\t', and '\r' into the single control character used by
	 * the system. 
	 * 
	 * @param origStr - the string to perform the operation upon.
	 * 
	 * @return - the resulting string with the control characters converted.
	 */
	private String replaceControlCharacters(String origStr) {
		String tmp = origStr;
// BRKPT
//if (tmp == null) {
//	int glh = 0;
//}
	int index = tmp.indexOf("\\");
		while (index != -1) {
			char lookAhead = tmp.charAt(index+1);
			if (lookAhead == 'n') {
				tmp = tmp.substring(0,index) + "\n" + tmp.substring(index+2, tmp.length());
			}
			else if (lookAhead == 't') {
				tmp = tmp.substring(0,index) + "\t" + tmp.substring(index+2, tmp.length());
			}
			else if (lookAhead == 'r') {
				tmp = tmp.substring(0,index) + "\r" + tmp.substring(index+2, tmp.length());
			}
			index = tmp.indexOf("\\", index+1);
		}
		
		return tmp;
	}
	
	private boolean resolveMsgRefs() throws PC2XMLException {
		if (!hasFSM)
			return false;
		ListIterator<IncompleteMsgRef> iter = unresolvedTable.listIterator();
		boolean resolved = true;
		while(iter.hasNext()) {
			IncompleteMsgRef imr = iter.next();
			MsgRef mr = imr.getMsgRef();
			ListIterator<FSM> fsmIter = fsms.listIterator();
			boolean found = false;
			while (fsmIter.hasNext() && !found) {
				FSM f = fsmIter.next();
				if (f.getName().equals(imr.getFSM())) {
					mr.setUID(f.getUID());
					found = true;
				}
			}
			if (!found)
				throw new PC2XMLException(fileName,"Parser could not resolve the attribute fsm="
						+ imr.getFSM() + " in the PC2XML document(" 
						+ imr.fileName + ") at line number=" + imr.getLine(),l);
		}
		return resolved;	
	}
	private void testDuplicateFSM(FSM fsm) throws PC2XMLException {
		int numFSMs = fsms.size();
		for (int i = 0; i < numFSMs; i++) {
		    if (fsms.get(i).getName().equals(fsm.getName())) {
		    	String msg =  "Duplicate fsm identified in document."
						+ " Name attribute must be unique in the entire document.";
				throw new PC2XMLException(fileName,msg, l);
		    }
		}
	}
	
	private boolean validBody(String body) {
		 if (body.equals("multipart") || 
			 body.equalsIgnoreCase("SDP") ||
				body.equalsIgnoreCase("open") ||
				body.equalsIgnoreCase("closed") ||
				body.equalsIgnoreCase("message-summary") ||
				body.equalsIgnoreCase("SDPOffer") ||
				body.equalsIgnoreCase("SDPAnswer"))
				return true;
		return false;
	}

	/**
	 * Verifies that the event is known and can be understood by the engine.
	 *  
	 * @param key - the name of the event in string form.
	 * @return true if valid, false otherwise.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private boolean validEvent(String key) {
		if (SIPConstants.isSIPEvent(key) || 
				UtilityConstants.isUtilityMsg(key) ||
				TimeoutConstants.isTimeoutEvent(key) ||
				ReferencePointConstants.isReferencePointEvent(key) ||
				EventConstants.isEvent(key) ||
				StunConstants.isStunEvent(key)) {
			return true;
		}
		return false;
	}
	private boolean validInstance(String instance) {
		if (instance.equals(MsgQueue.FIRST) ||
				instance.equals(MsgQueue.ANY) ||
				instance.equals(MsgQueue.LAST) ||
				instance.equals(MsgQueue.PREV) ||
				instance.equals(MsgQueue.CURRENT))
			return true;
		else {
			try {
				int value = Integer.parseInt(instance);
				if (value > 0)
					return true;
				else
					return false;
			}
			catch (NumberFormatException nfe) {
				return false;
			}
		}
	}
	/**
	 * Verifies that the protocol type attribute being used in the element is
	 * known and can be understood by the engine.
	 * 
	 * @param p - the protocol of the message.
	 * @return true if valid, false otherwise.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private boolean validProtocol(String p) {
		if (p.equalsIgnoreCase(MsgRef.SIP_MSG_TYPE) ||
				p.equalsIgnoreCase(MsgRef.UTILITY_MSG_TYPE) ||
				p.equalsIgnoreCase("tls") ||
				p.equalsIgnoreCase(MsgRef.SDP_MSG_TYPE) ||
				p.equalsIgnoreCase(MsgRef.STUN_MSG_TYPE) ||
				p.equalsIgnoreCase(MsgRef.RTP_MSG_TYPE)) {
			return true;
		}
		return false;
	}
	
	/**
	 * Verifies that the msgType attribute in an element is either SIP
	 * Request or Response type or that it is a valid Utility type.
	 *  
	 * @param type - the msgType of the element.
	 * @return true if valid, false otherwise.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private boolean validMsgType(String type) {
	if (SIPConstants.isRequestType(type) ||

				SIPConstants.isResponseType(type) ||
				UtilityConstants.isUtilityMsg(type) ||
				StunConstants.isStunType(type)) {
			return true;
		}
		return false;			
	}
	
	/**
	 * Verifies that the type attribute of a <msg_ref> element is valid
	 * and can be understood by the engine.
	 * 
	 * @param type - the type attribute of the msg_ref element
	 * @return true if valid, false otherwise.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private boolean validMsgRef(String type) {
		if (type.equalsIgnoreCase(MsgRef.SIP_MSG_TYPE) ||
				type.equalsIgnoreCase(MsgRef.SDP_MSG_TYPE) ||
				type.equalsIgnoreCase(MsgRef.PLATFORM_MSG_TYPE) ||
				type.equalsIgnoreCase("extensions") ||
				type.equalsIgnoreCase("event") ||
				type.equalsIgnoreCase(MsgRef.STUN_MSG_TYPE) ||
				type.equalsIgnoreCase(MsgRef.UTILITY_MSG_TYPE) ||
				type.equalsIgnoreCase(MsgRef.RTP_MSG_TYPE) ||
				type.equalsIgnoreCase("reginfo+xml") ||
				type.equalsIgnoreCase("pidf+xml") ||
				type.equalsIgnoreCase("dialog-info+xml") ||
				type.equalsIgnoreCase("simple-message-summary")) {
			return true;
		}
		return false;
	}
	
	/**
	 * Verifies that the modType attribute of a <mod> element is valid.
	 *  
	 * @param type - the modType of the <mod> element.
	 * @return true if valid, false otherwise.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private boolean validModType(String type) {
		if (type.equals("add") ||
				type.equals("replace") ||
				type.equals("delete")) {
			return true;
		}
		return false;
	}
	/**
	 * Verifies that the state being referenced in the document has been
	 * defined and exists in the current FSM.
	 *  
	 * @param key - the name of the State to validate.
	 * @return true if valid, false otherwise.
	 * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private boolean validState(String key) {
		
		if (curFSM != null) {
			return curFSM.hasState(key); 
		}
		return false;
	}
	
	/**
	 * This method verifies that the parameters of the substring attribute
	 * are properly formatted;
	 * @param token
	 * @return
	 */
	private String validSubstring(String token) {
		if (token.contains("length-")) {
			int minus = token.indexOf("-");
			if (minus != -1) {
				minus++;
				int space = token.indexOf(" ", minus);
				int pos = -1;
				if (space != -1) {
					pos = Integer.parseInt(token.substring(minus, space));
					if (pos != -1)
						return token.substring(minus, space);
				}
				else {
					pos = Integer.parseInt(token.substring(minus));
					if (pos != -1)
						return token.substring(minus);
				}
			}
		}
		else {
			int pos = Integer.parseInt(token);
			if (pos != -1)
				return token;
		}
		return null;
	}
	
	/**
	 * Verifies that all of the transitions with the each FSM can be resolved
	 * when operating.
	 */
	private boolean validateTrans() {
		ListIterator<FSM> iter = fsms.listIterator();
		boolean valid = true;
		while(iter.hasNext()) {
			curFSM = iter.next();
			Enumeration<State> states = curFSM.getStates();
			while (states.hasMoreElements()) {
				State s = states.nextElement();
				Enumeration<Transition> transitions = s.getTransitions();
				while (transitions.hasMoreElements()) {
					Transition t = transitions.nextElement();
					if (!(validState(t.getTo()) &&
							validState(t.getFrom()))) {
						if (!validState(t.getTo())) {
							logger.fatal(PC2LogCategory.Parser, subCat, 
							"Parser could not resolve the to attribute of the transition " + t 
								+ " in state " + s + " of fsm=" + curFSM.getName() + ".");
						}
						else if (!validState(t.getFrom())) {
							logger.fatal(PC2LogCategory.Parser, subCat, 
						
								"Parser could not resolve the from attribute of the transition " + t 
								+ " in state " + s + " of fsm=" + curFSM.getName() + ".");
						}
						valid = false;
					}
				}
			}
		}
		
		return valid;
	}
	/**
	 * Validates and separates the various <msg_ref> elements into their
	 * respective parsing methods.
	 *  
	 * * @throws SAXException - A syntax error has occurred within the XML document.
	 */
	private void validateReference() throws PC2XMLException {
		if (curTag.equals("msg_ref") || curTag.equals("add_ref") ||
				curTag.equals("subtract_ref") || curTag.equals("queue") ||
				curTag.equals("array_ref") || curTag.equals("where") || 
				curTag.equals("capture_ref")) {
			String type = curMsgRef.getType();
			
			if (curMsgRef != null && partialChar != null) {
				logger.trace(PC2LogCategory.Parser, subCat, 
						curTag + " contains characters (" + partialChar + ").");
				if (curMsgRef instanceof CaptureRef) {
					// String tmp = partialChar;
					int first = partialChar.indexOf(".");
					String token = null;
					if (first != -1)
						token = partialChar.substring(0, first);
					else 
						token = partialChar;
					if (first != -1 && PDMLTags.validMsgType(token)) {
						CaptureRef cr = (CaptureRef)curMsgRef;
						cr.setMsgType(token);
						int last = partialChar.lastIndexOf(".");
						if (last != -1 && first < last) {
							token = partialChar.substring(last+1);
							CaptureAttributeType cat = isPDMLAttributeType(token);
							if (cat != null) {
								cr.setAttributeType(cat);
								cr.setField(partialChar.substring(first+1, last));
							}
							else 
								cr.setField(partialChar.substring(first+1));
								
						}
						else if (first < last) {
							token = partialChar.substring(first+1);
							if (token.length() > 0)
								cr.setField(token);
						}
						else if (first == last && first < partialChar.length()) {
							token = partialChar.substring(last+1);
							CaptureAttributeType cat = isPDMLAttributeType(token);
							if (cat != null) {
								cr.setAttributeType(cat);
								// Leave the field null in this situation
							}
							else 
								cr.setField(partialChar.substring(first+1));
						}
//						String field = null;
//						while (tokens.hasMoreTokens()) {
//							token = tokens.nextToken();
//							if (first) {
//								first = false;
//								// We don't need to include the type in the 
//								// field in formation if it matches the type
//								if (!token.equals(curMsgRef.getType())) {
//									CaptureAttributeType cat = isPDMLAttributeType(token);
//									if (cat != null)
//										cr.setAttributeType(cat);
//									else
//										field = token;
//								}
//							}
//							else {
//								CaptureAttributeType cat = isPDMLAttributeType(token);
//								if (cat != null)
//									cr.setAttributeType(cat);
//								else
//									field += "." + token;
//							}
//						}
//						if (field != null) {
//							cr.setField(field);
//						}
					}
				}
				else {
					StringTokenizer tokens = new StringTokenizer(partialChar, ".");
					if (type.equals(MsgRef.SIP_MSG_TYPE) &&
							curMsgRef instanceof SIPRef) {
						parseSIPMsgRef(tokens);
					}
					else if (type.equals(MsgRef.SDP_MSG_TYPE) && 
							curMsgRef instanceof SDPRef) {
						parseSDPMsgRef(tokens);
					}
					else if (type.endsWith("+xml") && 
							curMsgRef instanceof SIPBodyRef) {
						parseSIPBodyMsgRef(tokens);
					}
					else if (type.equals("simple-message-summary") && 
							curMsgRef instanceof SIPBodyRef) {
						parseSIPBodyMsgRef(tokens);
					}
					else if (type.equals(MsgRef.PLATFORM_MSG_TYPE) &&
							curMsgRef instanceof PlatformRef) {
						parsePlatformMsgRef(tokens);
					}
					else if (type.equals(MsgRef.UTILITY_MSG_TYPE) &&
							curMsgRef instanceof UtilityRef) {
						parseUtilityMsgRef(tokens);
					}
					else if (type.equals("extensions") &&
							curMsgRef instanceof ExtensionRef) {
						parseExtensionsMsgRef(tokens);
					}
					else if (type.equals(MsgRef.STUN_MSG_TYPE) &&
							curMsgRef instanceof StunRef) {
						parseStunMsgRef(tokens);
					}
					else if (type.equals(MsgRef.RTP_MSG_TYPE) &&
							curMsgRef instanceof RTPRef) {
						parseRTPMsgRef(tokens);
					}
					else if (type.equals(MsgRef.EVENT_MSG_TYPE) &&
							curMsgRef instanceof EventRef) {
						String token = tokens.nextToken();
						if (EventConstants.isEvent(token) &&
								tokens.hasMoreTokens()) {
							((EventRef)curMsgRef).setType(token);
							String hdr = tokens.nextToken();
							if (hdr != null)
								((EventRef)curMsgRef).setHeader(hdr);
						}
					}
					else if (curMsgRef instanceof CaptureRef) {
						String token = tokens.nextToken();
						if (PDMLTags.validMsgType(token)) {
							CaptureRef cr = (CaptureRef)curMsgRef;
							cr.setMsgType(token);
							boolean first = true;
							String field = null;
							while (tokens.hasMoreTokens()) {
								token = tokens.nextToken();
								if (first) {
									first = false;
									// We don't need to include the type in the 
									// field in formation if it matches the type
									if (!token.equals(curMsgRef.getType())) {
										CaptureAttributeType cat = isPDMLAttributeType(token);
										if (cat != null)
											cr.setAttributeType(cat);
										else
											field = token;
									}
								}
								else {
									CaptureAttributeType cat = isPDMLAttributeType(token);
									if (cat != null)
										cr.setAttributeType(cat);
									else
										field += "." + token;
								}
							}
							if (field != null) {
								cr.setField(field);
							}
						}
					}
					else {
						String msg =  "Identified invalid type setting in msg_ref element.";
						throw new PC2XMLException(fileName,msg, l);
					}
				}
			}
			else if (curMsgRef instanceof EventRef) {
				// do nothing
			}
			else {
				String msg =  "CurMsgRef=" + curMsgRef + " or partialChar=" + partialChar + " is set to null. curTag=" + curTag;
				throw new PC2XMLException(fileName,msg, l);
			}
		}
		else if (curTag.equals("var_ref")) {
			// do nothing
		}
		else {
			String msg =  "curTag is set to " + curTag;
			throw new PC2XMLException(fileName,msg, l);
		}
	}
	
	private boolean verifyInitialStates() {
		boolean result = true;
		ListIterator<FSM> iter = fsms.listIterator();
		if (iter.hasNext()) {
			while (iter.hasNext()) {
				FSM f = iter.next();
				if (!f.hasInitialState()) {
					logger.warn(PC2LogCategory.Parser, subCat, 
							"The FSM[" + f.getName() + "] doesn't have an initial state called[" + 
							f.getInitialState() + "].");
				}
			}
		}
		else 
			result = false;
		
		return result;
	}
	private boolean verifyOnlyOneCDF() {
		int count = 0;
		String [] fsmLocs = new String [fsms.size()];
		for (int i = 0; i < fsms.size(); i++) {
			NetworkElements ne = fsms.get(i).getNetworkElements();
			if (ne == null) continue;
			ListIterator<String> iter = ne.getTargets();
			while (iter.hasNext()) {
				String target = iter.next();
				if (target.startsWith("CDF")) {
					fsmLocs[count] = fsms.get(i).getName();
					count++;
					
				}
			}
		}
		
		if (count > 1) {
			String fs = " ";
			for (int i= 0; i < count; i++)
				fs = fsmLocs[i] + " ";
			String msg = "There is more than one FSM that has requested to receive the diameter accounting messages." +
			" ie. they have CDFxx in their <NE targets> attribute setting." 
			+ "The value appears in the following fsms:\n" + fs + "\n";
			logger.fatal(PC2LogCategory.Parser, subCat, msg);
			return false;
		}
		
		return true;
	}
}
