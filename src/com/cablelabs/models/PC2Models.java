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

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.cablelabs.capture.Wireshark;
import com.cablelabs.fsm.Assign;
import com.cablelabs.fsm.Capture;
import com.cablelabs.fsm.ComparisonEvaluator;
import com.cablelabs.fsm.EventConstants;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.FSMAPI;
import com.cablelabs.fsm.FSMListener;
import com.cablelabs.fsm.Generate;
import com.cablelabs.fsm.GlobalVariables;
import com.cablelabs.fsm.InternalMsg;
import com.cablelabs.fsm.LogMsg;
import com.cablelabs.fsm.Mod;
import com.cablelabs.fsm.MsgEvent;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.MsgRef;
import com.cablelabs.fsm.ParserFilter;
import com.cablelabs.fsm.PlatformRef;
import com.cablelabs.fsm.PresenceStatus;
import com.cablelabs.fsm.Proxy;
import com.cablelabs.fsm.Reference;
import com.cablelabs.fsm.ReferencePointsFactory;
import com.cablelabs.fsm.Result;
import com.cablelabs.fsm.Retransmit;
import com.cablelabs.fsm.SIPConstants;
import com.cablelabs.fsm.SIPMsg;
import com.cablelabs.fsm.Send;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.Sleep;
import com.cablelabs.fsm.Stream;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.fsm.UtilityConstants;
import com.cablelabs.fsm.UtilityMsg;
import com.cablelabs.fsm.Variable;
import com.cablelabs.fsm.Verify;
import com.cablelabs.fsm.VoicetronixPort;
import com.cablelabs.gui.PC2UI;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.sim.PCSim2;
import com.cablelabs.sim.SIPDistributor;
import com.cablelabs.sim.Stacks;
import com.cablelabs.sim.StunDistributor;
import com.cablelabs.sim.UtilityDistributor;
import com.cablelabs.stun.StunConstants;
import com.cablelabs.tools.PDMLParser;
import com.cablelabs.tools.PacketDatabase;
import com.cablelabs.tools.RefLocator;

/**
 * The base class for all models within the PC 2.0 Platform
 * engine. It implements the FSMListener interface for action
 * request made by the FSM as well as the FSMAPI interface for
 * delivering new events received by the protocol stacks to the
 * FSM.
 * 
 * Most of the functionality of models is common and can be 
 * performed by this class, but it provides a convenient way of
 * overriding a specific method when necessary by a model.
 * 
 * @author ghassler
 *
 */
public class PC2Models extends Thread implements FSMAPI, FSMListener {

	/**
	 * Flag stating whether the system is shutting down or not.
	 */
	protected boolean shuttingDown = false;
	
	/**
	 * The FSM for this test.
	 */
	protected FSM fsm;
	
	/**
	 * Logger
	 */
	protected LogAPI logger = LogAPI.getInstance();

	/**
	 * The subcategory to use when logging
	 * 
	 */
	protected String subCat = "PC2Model";
	
	/**
	 * The name of our FSM
	 */
	protected String name;
	
	/**
	 * The interface to perform comparison operations for this test.
	 */
	protected Examiner examiner = null;
	
	/**
	 * The message queue for message received by the platform.
	 */
	protected MsgQueue q = null;

	/**
	 * A flag specifying whether to ignore duplicate messages or 
	 * not by the system.
	 */
	protected boolean processDuplicates = false;
	
	/**
	 * A handle to the FSM's event queue.
	 * 
	 */
	protected ConcurrentLinkedQueue<MsgEvent> queue =  null;
	
	/** 
	 * The capture process that the script may have started.
	 * 
	 */
	protected Wireshark captureInterface = null;
	protected boolean captureRunning = false;
	/** 
	 * The name of the output file for the capture tool. (ie. Wireshark)
	 */
	protected String captureFileName = null;
	
	/** 
	 * The name of the last output file for the capture tool. (ie. Wireshark)
	 */
	protected String lastCaptureFileName = null;
	
	/**
	 * The name of the packet capture tool to use.
	 */
	//w.startCapture(w.getCommand(), "-i", "\\Device\\NPF_{F8B3118C-29C3-4C10-AB91-8EEE5FEC08D7}", "-w", pcap, "-l", "-q");
	protected final String captureApp =  "tshark.exe"; // "wireshark.exe"; // "dumpcap.exe";
	
	protected final String CAPTURE_EXT = ".pcap";
	protected final String CAPTURE_EXT2 = ".cap";
	
	protected final String CAPTURE_DIR = ".." + File.separator + "logs";
	/**
	 * Indicates whether a text file has been generated from the 
	 * capture file.
	 */
	protected boolean captureTextGenerated = false;
	
	/**
	 * A list of the capture files that have been converted to pdml
	 * 
	 */
	protected Hashtable<String, File> convertedCaptureFiles = null;
	 
	/**
	 * This is a tool to retrieve the specific information defined by
	 * a MsgRef object.
	 */
	protected RefLocator refLocator = RefLocator.getInstance();
	
	/**
	 * A handle to the thread we started on behalf of the FSM
	 */
	protected Thread thread = null;
	
	/**
	 * This is a table common to all Registrar's including the global 
	 * register to maintain the contact address sent in the register message
	 * by an UE for their respective telephone number.
	 */
	static protected Hashtable<String, String> contactTable = new Hashtable<String, String>();
	
	/**
	 * This table keeps track of all of the PacketDatabase created by the
	 * <parse_capture> tags within a test script.
	 */
	static protected Hashtable<String, PacketDatabase> parsedCaptureDBs = null;
	
	protected long CAPTURE_AUTO_TERMINATE = 600000L;
	
	/**
	 * This flag allows the system to flush the msgQueue whenever there are no
	 * pending messages to process. This should keep the system from running 
	 * out of memory when running for extended periods of time.
	 */
	protected boolean autoRouting = false;
	
	/**
	 * This flag indicates that the FSM has autoProvisioning enabled and needs
	 * to allow the system to provide additional wait time when waiting 
	 */
	//protected boolean autoProvision = false;
	
	/**
	 * Constructor for the model.
	 * 
	 * @param fsm - the FSM to use during this test.
	 * @param name - the name of our FSM.
	 * @param model - the name of the model and the value
	 * 	used as the subcategory for logging.
	 * 
	 */
	public PC2Models(FSM fsm, String name, String model) {
		super(name);
		this.fsm = fsm;
		this.name = name;
		this.subCat = model;
		this.queue = new ConcurrentLinkedQueue<MsgEvent>(); // new ConcurrentLinkedQueue<MsgEvent>();
		setName(name);
		
		// Allow the thread to be terminated anytime.
		setDaemon(true);
		
		q = PCSim2.getMsgQueue();
		this.examiner = new Examiner(fsm);
		// Have the FSM update the No Response Timeout if it
		// has been changed in the test document.
		fsm.setNoResponseTimeout(SystemSettings.getNoResponseTimeout());
		
        // Next see if the capture default timeout is changing
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		if (platform != null) {
			String value = platform.getProperty("Wireshark Default Timeout");
			if (value != null) {
				try {
					int timeout = Integer.parseInt(value);
					if (timeout >=0 && timeout <= 3600) {
						CAPTURE_AUTO_TERMINATE = timeout * 1000;
					}
				} catch (NumberFormatException nfe) {
					logger.warn (PC2LogCategory.Model, subCat,
							"The Wireshark Default Timeout is not a valid integer, ignoring setting and using 600 seconds.");
				}
			}
		}
	}
 
	static public void addContact(String phoneNum, String contact) {
		contactTable.put(phoneNum, contact);
	}
	

	
	@Override
	public boolean assign(Assign a, int msgQueueIndex) {
		GlobalVariables gv = GlobalVariables.getInstance();
		if (gv != null) {
			Variable var = new Variable(a.getName());
			var.setRef(a.getRef());
			ComparisonEvaluator ce = fsm.getComparisonEvaluator();
			MsgEvent event = q.get(msgQueueIndex);
			try {
				return var.performOp(this, ce, event);
			}
			catch (Exception ex) {
				logger.error(PC2LogCategory.Model, subCat, 
						"Could not perform assign operation.\n" 
						+ ex.getMessage() + " \n" + ex.getStackTrace());
			}
			
		}
		return false;
	}
	
	@Override
	public boolean capture(Capture c) {
		if (c.isStart()) {
				//w.startCapture(w.getCommand(), "-i", "\\Device\\NPF_{F8B3118C-29C3-4C10-AB91-8EEE5FEC08D7}", "-w", pcap, "-l", "-q");
			// Since the script called stopCapture or the model did, make sure
			// that the capture interface was reset to null.
			if (captureInterface != null && !captureRunning) {
				captureInterface = null;
			}
			if (captureInterface == null) {
				startCapture(c);
			}
			else {
				logger.error(PC2LogCategory.Model, subCat, 
						"The model already has a capture in progress." + 
				" You need to stop the current capture before starting another one.");
			}
		}
		else if (c.isStop() && 
				(captureInterface != null || captureRunning)) {
				stopCapture();
		}
		else if (c.isParse()) {
			String captureFile = c.getFile();
			File f = null;
			if (c != null && captureFile != null) {
				String fileName = CAPTURE_DIR + File.separator + captureFile;
				f = new File (fileName);
			}
			else {
				// Assume the last capture file name
				if (captureFileName != null)
					f = new File(captureFileName + CAPTURE_EXT);
			}
			parseCapture(f, c);
		}
		else {
			logger.warn(PC2LogCategory.Model, subCat, "There is no capture currently running to stop.");
		}
		return true;
	}
	
	@Override
	public boolean changeStatus(String ne, PresenceStatus status) {
		PresenceServer ps = Stacks.getPresenceServer();
		if (ps != null) {
			return ps.newStatus(ne, status);
		}
		else {
			logger.warn(PC2LogCategory.Model, subCat,
					"Could not find the FSM(" + PresenceServer.NAME 
					+ ") to deliver event(changeStatus).");
		}
		return true;
	}
	
	protected boolean createCaptureDB(String fileName, Capture c) {
		// There is no need to convert the file to
		// pdml.
		PDMLParser parser = new PDMLParser();
		ParserFilter pf = (ParserFilter)c.getFilter();
		try {
			PacketDatabase db = parser.parse(fileName, c.getName(), pf);
			if (db != null) {
				logger.info(PC2LogCategory.Model, subCat, "Creating capture database " + db.toString());
				parsedCaptureDBs.put(c.getName(), db);
				return true;
			}
		}
		catch (Exception ex) {
			System.err.println("Exception occurred:\n" + ex.getMessage() 
					+ "\n" + ex.getStackTrace());
		}
		return false;
	}

	/** 
	 * Creates the global variable for use by the FSM.
	 * 
	 */
	@Override
	public boolean createVariable(Variable v, int msgQueueIndex) {
		boolean result = false;
		try {
			MsgEvent event = null;
			if (msgQueueIndex > -1)
				event = q.get(msgQueueIndex);
			
				v.performOp(this, fsm.getComparisonEvaluator(), event);
			result = true;
		}
		catch (Exception e) {
			String err = "Exception encountered during var action processing.\n" 
				+ e.getMessage() + "\n" + e.getStackTrace();
			logger.warn(PC2LogCategory.Model, subCat, err);
		}

		return result;
	}
	/**
     * Gets a request message based upon its CallId Header value.
     */
	@Override
	public SIPMsg findByCallIdAndMethod(String callid, String method, int cSeqNo) {
		logger.debug(PC2LogCategory.Model, subCat, 
				"Asked to find " + callid + method + cSeqNo + " in callID message index.");
		return q.findByCallIdAndMethod(callid, method, cSeqNo);
	}
	
	/** Removes all of the items that this listener has added in the init()
	 * method.
	 *
	 */
	@Override
	public void fsmComplete() {
		Stacks.removeSessionListeners(this);
	}
	
	static public PacketDatabase getCaptureDB(String name) {
		if (parsedCaptureDBs != null) {
			return parsedCaptureDBs.get(name);
		}
		return null;
	}
	static public String getContact(String phoneNum) {
		return contactTable.get(phoneNum);
	}

	@Override
	public int getCurrentMsgIndex() {
		return fsm.getCurrentMsgQueueIndex();
	}
	/**
	 * Gets the examiner for comparison operations.
	 *
	 */
	public Examiner getExaminer() {
		return examiner;
	}


	/**
	 * Get the uniqueID of the FSM
	 */
	@Override
	public int getFsmUID() {
		return fsm.getUID();
	}
	
	/**
	 * Get the name of the FSM
	 */
	@Override
	public String getFSMName() {
		return fsm.getName();
	}

	private VoicetronixPort [] getNetworkElementForUtilityPrompt(Send s) {
		int ports = 1;
		if (s.getMsgType().equalsIgnoreCase(UtilityConstants.VERIFY_VOICE_PATH_TWO_WAY))
			ports = 2;
		VoicetronixPort[] results = new VoicetronixPort[ports];
		
		if (ports == 1) {
			Mod m = s.getModifier(SettingConstants.VOICE_PORT);
			if (m != null) {
				results[0] = SystemSettings.getVoicePort((PlatformRef)m.getRef());
			}
			else {
				results[0] = SystemSettings.getDefaultVoicePort();
			}
		}
		else if (ports == 2) {
			Mod m = s.getModifier(SettingConstants.FROM_VOICE_PORT);
			results[0] = SystemSettings.getVoicePort((PlatformRef)m.getRef());
			m = s.getModifier(SettingConstants.TO_VOICE_PORT);
			results[1] = SystemSettings.getVoicePort((PlatformRef)m.getRef());
		}
		
		return results;
	}
	/**
	 * Gets a Request message for inclusion in a Response request
	 * for the SIP and Stun stacks.
	 */
	public MsgEvent getRequest(String method, String index) {
		return q.find(fsm.getUID(), method, index, fsm.getCurrentMsgQueueIndex());
		
	}

	/**
	 * Gets the last response message in the MsgQueue if it matches the index 
	 * 
	 * @return - the previous message, otherwise null.
	 */
	public MsgEvent getResponse(String method, String index) {
		return q.find(fsm.getUID(), method, index, fsm.getCurrentMsgQueueIndex());
		
	}

	protected void informSessions(String event) {
		Enumeration<FSMListener> listeners = Stacks.getSessionListeners();
		LinkedList<Integer> notified = new LinkedList<Integer>();
		while (listeners.hasMoreElements()) {
			FSMListener l = listeners.nextElement();
			Integer uid = l.getFsmUID();
			ListIterator<Integer> iter = notified.listIterator();
			boolean duplicate = false;
			while (iter.hasNext() && !duplicate) {
				Integer prevUID = iter.next();
				if (prevUID == uid)
					duplicate = true;
			}
			
			if (!duplicate) {
				InternalMsg msg = new InternalMsg(l.getFsmUID(), System.currentTimeMillis(), 
						LogAPI.getSequencer(), event);
				notified.add(uid);
				logger.info(PC2LogCategory.Model, subCat,
						"GR = Creating event -" + msg.getEventName() + " for FSM (" + l.getFSMName() + ")");
				l.processEvent(msg);
			}
		}
	}
	/**
	 * Initializes its' local attributes
	 *
	 */
	public void init() {
		// before we start the test, make sure there are no orphan captures
		Wireshark.terminateOrphans();
		// Now add ourselves to the list of listeners in the system.
		Stacks.addListener(name, this);
		processDuplicates = SystemSettings.getBooleanSetting(SettingConstants.FSM_PROCESS_DUPLICATE_MSGS);
		
	}
	
	/**
	 * @return - true when the state is "Registered", false
	 * 	 otherwise
	 */
	@Override
	public  boolean isRegistered() {
		return false;
	}

	protected void locateRequest(Send s) {
		String protocol = s.getProtocol();
		String msgtype = s.getMsgType();
		if (protocol.equals(MsgRef.SIP_MSG_TYPE)) { 
			// First see if the user specified a specific message to use
			if (s.getOriginalRequest() != null) {
				MsgEvent req = q.find(fsm.getUID(), s.getOriginalRequest(), 
						s.getOriginalInstance(), fsm.getCurrentMsgQueueIndex());
				if (req != null) {
					s.setRequest(req);
					return;
				}
				// Otherwise let it fall through to the normal algorithm
			}
			
			if (SIPConstants.isResponseType(msgtype)) {
				String method = msgtype.substring(4);
				if (method.equalsIgnoreCase(SIPConstants.INVITE)) {
					MsgEvent e1 = q.find(fsm.getUID(), SIPConstants.INVITE, 
							MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
					MsgEvent r1 = q.find(fsm.getUID(), "200-" + SIPConstants.INVITE, 
							MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
					MsgEvent e2 = q.find(fsm.getUID(),SIPConstants.REINVITE, 
							MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
					if (e2 != null && !s.isCleanUpEvent() && 
							e1.getMsgQueueIndex() < e2.getMsgQueueIndex()) 
						s.setRequest(e2);
					else if (e2 != null && s.isCleanUpEvent() &&
							r1 != null &&
							r1.getMsgQueueIndex() < e2.getMsgQueueIndex())
						s.setRequest(e2);
					else
						s.setRequest(e1);
				}
				else {
					s.setRequest(q.find(fsm.getUID(), method, 
							MsgQueue.LAST, fsm.getCurrentMsgQueueIndex()));
				}
			}
			else if (msgtype.equalsIgnoreCase("CANCEL")) {
				MsgEvent e1 = q.find(fsm.getUID(), SIPConstants.INVITE, 
						MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
				s.setRequest(e1);
			}
			// Each of these messages should be for the very last INVITE
			// by default as long as there was originally a final response
			else if (msgtype.equalsIgnoreCase("ACK") ||
					msgtype.equalsIgnoreCase("OPTIONS") ||
					msgtype.equalsIgnoreCase("PRACK") ||
					msgtype.equalsIgnoreCase("UPDATE") ||
					msgtype.equalsIgnoreCase("PUBLISH")) {
				MsgEvent e1 = q.find(fsm.getUID(), SIPConstants.INVITE, 
						MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
				MsgEvent r1 = q.find(fsm.getUID(), "200-" + SIPConstants.INVITE, 
						MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
				MsgEvent e2 = q.find(fsm.getUID(), SIPConstants.REINVITE, 
						MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
				if (e2 != null && 
						e1 != null &&
						!s.isCleanUpEvent() &&
						e2.getMsgQueueIndex() > e1.getMsgQueueIndex())
					s.setRequest(e2);
				else if (e2 != null && 
						e1 != null &&
						s.isCleanUpEvent() &&
					 	r1 != null && 
						r1.getMsgQueueIndex() < e2.getMsgQueueIndex() &&
						e2.getMsgQueueIndex() > e1.getMsgQueueIndex())
					s.setRequest(e2);
				else 
					s.setRequest(e1);

				// request = api.getRequest("Invite", MsgQueue.LAST);

			}
			else if (msgtype.equalsIgnoreCase("BYE")) {
				s.setRequest(q.find(fsm.getUID(), SIPConstants.INVITE, 
						MsgQueue.LAST, fsm.getCurrentMsgQueueIndex()));
			}
			else if (msgtype.equalsIgnoreCase("REGISTER")) {
				// Need to see if there is a previous register an response
				MsgEvent response = q.find(fsm.getUID(), "401-REGISTER", 
						MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
				MsgEvent resp200 = q.find(fsm.getUID(), "200-REGISTER", 
						MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
				if (response != null && resp200 == null) {
					MsgEvent regReq = q.find(fsm.getUID(), "Register", 
							MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
					if (regReq != null &&
							response.getMsgQueueIndex() > regReq.getMsgQueueIndex())
						s.setRequest(regReq);
				}
				else if (response != null && resp200 != null &&
						response.getMsgQueueIndex() < resp200.getMsgQueueIndex()) {
					MsgEvent regReq = q.find(fsm.getUID(), "Register", 
							MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
					if (regReq != null &&
							resp200.getMsgQueueIndex() > regReq.getMsgQueueIndex())
						s.setRequest(regReq);
				}
			}
			else if (msgtype.equalsIgnoreCase("REINVITE")) {
				s.setRequest(q.find(fsm.getUID(), "Invite", 
						MsgQueue.LAST, fsm.getCurrentMsgQueueIndex()));

			}
			else if (msgtype.equalsIgnoreCase("REFER")) {
				s.setRequest(q.find(fsm.getUID(), "Invite", 
						MsgQueue.FIRST, fsm.getCurrentMsgQueueIndex()));

			}
			else if (msgtype.equalsIgnoreCase("NOTIFY")) {
				if (s.getSubscribeType() != null) {
					MsgEvent e1 = q.find(fsm.getUID(), SIPConstants.SUBSCRIBE, 
							MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
					if (e1 instanceof SIPMsg) {
						String subscribeType = ((SIPMsg)e1).getSubscribeType();
						if (subscribeType.equals(s.getSubscribeType()))
							s.setRequest(e1);
						else {
							e1 = q.find(fsm.getUID(), SIPConstants.SUBSCRIBE, 
									MsgQueue.LAST, e1.getMsgQueueIndex()-1);
							boolean found = false;
							while (e1 != null && !found) {
								if (e1 instanceof SIPMsg) {
									subscribeType = ((SIPMsg)e1).getSubscribeType();
									if (subscribeType.equals(s.getSubscribeType())) {
										s.setRequest(e1);
										found = true;
									}
								}
								if (!found) {
									e1 = q.find(fsm.getUID(), SIPConstants.SUBSCRIBE, 
											MsgQueue.LAST, e1.getMsgQueueIndex()-1);
								}
							}
						}
					}
				}
				else {
					MsgEvent e1 = q.find(fsm.getUID(), SIPConstants.SUBSCRIBE, 
							MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());

					MsgEvent e2 = q.find(fsm.getUID(), SIPConstants.REFER, 
							MsgQueue.LAST, fsm.getCurrentMsgQueueIndex());
					if (e2 != null && 
							e1 != null && 
							e1.getMsgQueueIndex() < e2.getMsgQueueIndex())
						s.setRequest(e2);
					else if (e1 != null && e2 != null &&
							e2.getMsgQueueIndex() < e1.getMsgQueueIndex())
						s.setRequest(e1);
					else if (e2 != null && e1 == null)
						s.setRequest(e2);
					else if (e1 != null && e2 == null)
						s.setRequest(e1);
				}
			}
		}
		
		else if (protocol.equalsIgnoreCase(MsgRef.STUN_MSG_TYPE) &&

				StunConstants.isStunResponse(msgtype)) {
			s.setRequest(q.find(fsm.getUID(), StunConstants.BINDING_REQUEST, 
					MsgQueue.LAST, fsm.getCurrentMsgQueueIndex()));
		}
	}
	/**
	 * Implementation for the Log action within a FSM.
	 * 
	 * @returns true when the message has been logged, false otherwise
	 */
	@Override
	public boolean log(LogMsg l) {
		boolean result = false;
		if (logger != null) {
//			 See if there is a global variable in the expr.
			String expr = l.getExpr();
			int index = expr.indexOf("$");
			while (index != -1 && index < expr.length()) {
				int indexSpace = expr.indexOf(" ", index);
				int indexPeriod = expr.indexOf(".", index);
				int indexQuestion = expr.indexOf("?", index);
				int indexComma = expr.indexOf(",", index);
				int end = -1;
				if (indexSpace != -1 && indexSpace < expr.length())
					end = indexSpace;
				else if (indexPeriod != -1 && indexPeriod < expr.length())
					end = indexPeriod;
				else if (indexQuestion != -1 && indexQuestion < expr.length())
					end = indexQuestion;
				else if (indexComma != -1 && indexComma < expr.length())
					end = indexComma;
				else 
					end = expr.length();
				if (end != -1) {
					// See if we can replace it with some value in the 
					// global variables.
					GlobalVariables gv = GlobalVariables.getInstance();
//					String varName = expr.substring(index+1,indexSpace);
					Variable var = gv.get(expr.substring(index+1,end));
					if (var != null) {
						Object value = var.getVariable();
						if (value instanceof String) {
							expr = expr.substring(0, index) 
								+ (String)value 
								+ expr.substring(end);
						}
					}
					index = end;
				}
				index = expr.indexOf("$", ++index);
			}
			if (l.getLevel().equalsIgnoreCase("info")) {
				logger.info(PC2LogCategory.LOG_MSG, subCat, 
						" USER: \n" + expr);
				result = true;
			}
			else if (l.getLevel().equalsIgnoreCase("fatal")) {
				logger.fatal(PC2LogCategory.LOG_MSG, subCat, 
						" USER: \n" + expr);
				result = true;
			}
			else if (l.getLevel().equalsIgnoreCase("warn")) {
				logger.warn(PC2LogCategory.LOG_MSG, subCat, 
						" USER: \n" + expr);
				result = true;
			}
			else if (l.getLevel().equalsIgnoreCase("debug")) {
				logger.debug(PC2LogCategory.LOG_MSG, subCat, 
						" USER: \n" + expr);
				result = true;
			}
			else if (l.getLevel().equalsIgnoreCase("trace")) {
				logger.trace(PC2LogCategory.LOG_MSG, subCat, 
						" USER: \n" + expr);
				result = true;
			}
			else if (l.getLevel().equalsIgnoreCase("error")) {
				logger.error(PC2LogCategory.LOG_MSG, subCat, 
						" USER: \n" + expr);
				result = true;
			}
			
			if (l.getPromptUser()) {
				fsm.suspendForPrompt();
				logger.info(PC2LogCategory.Model, subCat, 
						"FSM(" + fsm.getName() + ") pausing until user responds.");
				if (PCSim2.isGUIActive()) {
					PC2UI ui = PCSim2.getUI();
				
					boolean response = ui.notifyUser(expr, 
							l.isVerify(), l.isYesExpected());
					if (l.isVerify()) {
						PCSim2.setTestPassed(response);
						l.setPassed(response);
						
						String testResult = null;
						if (response)
							testResult = "VERIFY PASSED";
						else 
							testResult = "VERIFY FAILED";
												
						if (l.getStep() != null)
							testResult += " Step " + l.getStep();
						if (l.getRequirements() != null) 
							testResult += " Requirement " + l.getRequirements();
						if (l.getGroup() != null)
							testResult += " Group[" + l.getGroup() + "]";
						
						testResult += " - \n\t";
						if (testResult != null) {
//							String err = "VERIFY FAILED for LogMsg because the user declared the test a failure "
//								+ "by their response to the question\n[" + expr + "].";
							testResult += l.getExpr();
							if (response)
								logger.info(PC2LogCategory.Model, subCat, testResult);
							else
								logger.error(PC2LogCategory.Model, subCat, testResult);
						}
					}
					fsm.resumeFromPrompt();
				}
				else {
					System.out.println("\nPress ENTER to continue test.");
					
					byte [] input = new byte [1024];
					try {
						System.in.read(input);
						// Since we don't care what the user hit, simply
						// resume the test.
						fsm.resumeFromPrompt();
						logger.info(PC2LogCategory.Model, subCat, 
								"Resuming test from prompt.");
						System.out.println("\nResuming test.");
						result = true;
					}
					catch (IOException ioe) {
						logger.fatal(PC2LogCategory.Model, subCat, 
								"System encountered error while awaiting response from user. Terminating test!");
						PCSim2.setTestComplete();
						PCSim2.setTestPassed(false);
						shutdown();
					}
				}
				logger.info(PC2LogCategory.Model, subCat, 
				"FSM(" + fsm.getName() + ") resuming execution.");
			}
		}
		return result;
	}
	

	
	protected void parseCapture(File f, Capture c) {
		if (f == null) {
			logger.error(PC2LogCategory.Model, subCat, 
					"The specified capture file(" + null 
					+ ") does not exist. Skipping action operation.");
					return;
		}
		if (!f.exists()) {
			logger.error(PC2LogCategory.Model, subCat, 
			"The specified capture file(" + f.getAbsolutePath() 
			+ ") does not exist. Skipping action operation.");
			return;
		}
		if (!f.isFile()) {
			logger.error(PC2LogCategory.Model, subCat, 
			"The specified capture file(" + f.getAbsolutePath() 
			+ ") is not a file. Skipping action operation.");
			return;
		}
		if (!f.canRead()) {
			logger.error(PC2LogCategory.Model, subCat, 
			"The specified capture file(" + f.getAbsolutePath() 
			+ ") can not be read. Skipping action operation.");
			return;
		}
		PacketDatabase db = null;
		// Next make sure there isn't a database with the name
		if (parsedCaptureDBs == null)
			parsedCaptureDBs = new Hashtable<String, PacketDatabase>();
		db = parsedCaptureDBs.get(c.getName());
		if (db != null) {
			logger.error(PC2LogCategory.Model, subCat, 
				"The parsed capture database already has a database with the name(" + c.getName() 
				+ "). Skipping action operation.");
				return;
		}
		String ext = f.getName();
		fsm.suspendForPrompt();
		if (ext.endsWith("pdml") || 
				ext.endsWith("xml")) {
			// There is no need to convert the file to
			// pdml.
			createCaptureDB(f.getAbsolutePath(), c);
		}
		else if (ext.toUpperCase().endsWith(CAPTURE_EXT.toUpperCase()) ||
				ext.toUpperCase().endsWith(CAPTURE_EXT2.toUpperCase())){
			// Call the operation to convert file to xml
			// but only do it once.
			File pdml = null;
			if (captureFileName != null)
				pdml = new File(captureFileName + ".xml");
			else if (f != null) {
				// Put the file in the logs directory 
				pdml = new File(".." + File.separator + "logs" + File.separator + ext.substring(0,(ext.length()-CAPTURE_EXT.length()))+ ".xml");
			}
			if (convertedCaptureFiles == null || 
					!convertedCaptureFiles.containsKey(pdml.getName())) {
				
				try {
					if (convertedCaptureFiles == null)
						convertedCaptureFiles = new Hashtable<String, File>();
					if (captureInterface == null) {
						Properties platform = SystemSettings.getSettings("Platform");
					    if (platform !=  null) {
					    	String setting = platform.getProperty(SettingConstants.WIRESHARK_DIRECTORY);
					    	if (setting != null)
					    		captureInterface = new Wireshark(setting,CAPTURE_AUTO_TERMINATE);
					    	else
					    		captureInterface = new Wireshark(CAPTURE_AUTO_TERMINATE);
					    }
					    else
					    	captureInterface = new Wireshark(CAPTURE_AUTO_TERMINATE);
					}	
					logger.info(PC2LogCategory.Model, subCat, "Starting capture conversion.");
					
					// See if the capture file has already been converted once, if so we can
					// continue without performing the operation again
					if (pdml.exists() && pdml.canRead())
						convertedCaptureFiles.put(pdml.getName(), pdml);
					else {
						captureInterface.convertToPDML(f, pdml);
										
						if (pdml.exists() && pdml.canRead()) {
							convertedCaptureFiles.put(pdml.getName(), pdml);

							//convertedCaptureFile = new File (ext.substring(0,(ext.length()-CAPTURE_EXT.length()))+ ".xml");
						}
						else {
							logger.error(PC2LogCategory.Model, subCat, 
									"The conversion of the capture file(" + 
									captureFileName + ") to pdml appears to have failed.\n");

						}
					}
				}
				catch (IOException ioex) {
					logger.error(PC2LogCategory.Model, subCat, 
							"The model encountered an exception when trying to convert the capture file(" + 
							captureFileName + ").\n" + ioex.getMessage() + "\n" + ioex.getStackTrace());

				}
			}
			
			if (convertedCaptureFiles.containsKey(pdml.getName())) {
				logger.info(PC2LogCategory.Model, subCat, "Conversion complete.\nCreating internal database."); 
				
				createCaptureDB(pdml.getAbsolutePath(), c);
				
				logger.info(PC2LogCategory.Model, subCat,  "Database created.");
			}
			
		}
		else {
			logger.error(PC2LogCategory.Model, subCat, 
					"The conversion of the capture file(" + 
					captureFileName + ") failed due to unexpected file extension. Software supports .pcap or .cap only\n");
		}
		fsm.resumeFromPrompt();
	}
	
	/**
	 * Implementation for the Pass and Fail actions within a FSM.
	 * 
	 * @return - true if the global result value was updated, false otherwise
	 */
	@Override
	public boolean pass(Result r) {
		if (r.getPass())
			logger.info(PC2LogCategory.Model, subCat, "PASS");
		else {
			//logger.info(PC2LogCategory.Model, subCat, "FAIL");
			String err = "FAIL - The script declared the test a failure with the <fail> tag."
			+ " Declaring test case failure.\n";
			logger.fatal(PC2LogCategory.Model, subCat, err);
		}
		PCSim2.setTestPassed(r.getPass());
		return true;
	}
	
	/**
	 * Implementation for the Pass and Fail actions within a FSM.
	 * 
	 * @return - true if the global result value was updated, false otherwise
	 */
	@Override
	public boolean proxy(Proxy p) {
		SIPDistributor dist = Stacks.getSipDistributor();
		if (dist != null) {
			SIPMsg msgSent =  dist.proxy(this, p, fsm.getNetworkElements(),
					fsm.getSipStack());
			if (msgSent == null)
				return false;
			
			// Reference points are not used when we are proxying messages.
			// ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
			//if (rpf != null) {
			//	rpf.sent(msgSent);
				q.add(msgSent);
				// For system testing the target will be set to auto for 
				// auto routing and therefore to keep the msg queue from
				// growing to the point we have no memory, we need to remove
				// all of the messages in it that have an id less than the 
				// most recently received.
				if (!autoRouting &&
						p.getTarget().equals("auto")) {
					autoRouting = true;
					q.flush();
				}
//				 LOG USING LOGMSG category so that the user can't disable which
				// would break the trace tool.
				logger.info(PC2LogCategory.LOG_MSG, subCat, 
						"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() + ") sent event (" 
						+ msgSent.getEventName() + ") sequencer=" + msgSent.getSequencer() + ".");
			//}
			return true;
			
		
		}
		return false;
	}


	
	/**
	 * Implementation of the interface for protocol stacks to 
	 * deliver events to a FSM.
	 * 
	 * @return - true if the message was delivered successfully to the
	 * 			FSM, false otherwise
	 */
	@Override
	public boolean processEvent(MsgEvent event) throws IllegalArgumentException {
		boolean result = false;
		if (fsm != null && !shuttingDown) {
			q.add(event);
			
			if (processDuplicates && event.isDuplicate()) 
				result = fsm.processEvent(event);
					
			else if (!event.isDuplicate()) 
				result = fsm.processEvent(event);
				
			else
				logger.info(PC2LogCategory.Model, subCat, 
					"Dropping duplicate event.");
				
		}
		else if (!event.equals(EventConstants.SHUTDOWN)){
			logger.info(PC2LogCategory.Model, subCat, "Model " 
					+ name + " unable to add event(" + event.getEventName() 
					+ ") to QUEUE because fsm=" + fsm.getName() + " and shutting down=" 
					+ shuttingDown);
			return false;
		}
        return result;
	  
	}

	/**
	 * Implementation for delivering an event from one FSM to another.
	 * 
	 * @return -true if the message was delivered successfully to the
	 * 			target FSM, false otherwise. 
	 */
	@Override
	public boolean processEvent(Generate g) {
		
		if (g.getTarget() == null || g.getTarget().equals(fsm.getName())) {
			InternalMsg sm = new InternalMsg(fsm.getUID(), 
					System.currentTimeMillis(), LogAPI.getSequencer(), g.getEvent(), g.getSender());
			// q.add(sm);
			logger.info(PC2LogCategory.Model, subCat, 
					" Delivered " + g.getEvent() + " request to FSM (" + fsm.getName() 
					+ ") for processing:\n");
			((FSMListener)this).processEvent(sm);
			return true;
		}
		else {
			FSMListener t = Stacks.getFSMListenerByName(g.getTarget());
			if (t != null) {
				InternalMsg msg = new InternalMsg(t.getFsmUID(), System.currentTimeMillis(),
						LogAPI.getSequencer(), g.getEvent(), g.getSender());
				logger.info(PC2LogCategory.Model, subCat, 
						" Delivered " + g.getEvent() + " request to FSM (" + t.getFSMName()
						+ ") for processing:\n");
				return t.processEvent(msg);
			}
			else {
				logger.warn(PC2LogCategory.Model, subCat,
						"Could not find FSM defined by (" + g.getTarget() 
						+ ") to deliver event(" + g.getSender() + ").");
			}
		}
		return false;
	}
	
	public static boolean removeContact(String phoneNum) {
		String contact = contactTable.remove(phoneNum);
		if (contact != null)
			return true;
		return false;
	}
	
	@Override
	public boolean retransmit(Retransmit r, int msgQueueIndex) {
		LinkedList<MsgEvent> llme = q.findAll(fsm.getUID(), r.getMsgType(),
				fsm.getCurrentMsgQueueIndex());
		if (llme.size() > 0) {
			MsgEvent event = llme.get(llme.size()-1);
				if (r.getProtocol().equals(MsgRef.SIP_MSG_TYPE) &&
						event instanceof SIPMsg) {
					SIPMsg req = (SIPMsg)event;
					if (req.hasSentMsg()) {
						SIPDistributor dist = Stacks.getSipDistributor();
						SIPMsg msgSent =  dist.retransmit(this, r, req, 
								fsm.getNetworkElements(), fsm.getLastUtilityMsg(),
								fsm.getSipStack());
						if (msgSent == null)
							return false;
						else {
                            // LOG USING LOGMSG category so that the user can't disable which
							// would break the trace tool.
							logger.info(PC2LogCategory.LOG_MSG, subCat, 
									"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() + ") sent event (" 
									+ msgSent.getEventName() + ") sequencer=" + msgSent.getSequencer() + ".");
							q.add(msgSent);
							return true;
						}
					}
				
			}
			
		}
		
		return false;
	}
	
	/**
	 * The main loop for processing incoming event into the
	 * FSM. It loops though all of the events in the FSM awaiting
	 * processing until the queue is empty then waits until a
	 * new events arrives.
	 */
	@Override
	public void run() {
		thread = Thread.currentThread();
		logger.debug(PC2LogCategory.Model, subCat, "Current thread name is = " + thread.getName());
		while (!shuttingDown) {
			try	{
				synchronized (queue) {
					while (queue.isEmpty()) {
						// Check to see if we need to exit.
						if (shuttingDown) {
							logger.info(PC2LogCategory.Model, subCat, "FSM(" + name + ") complete.");
							return;
						}
						try {
							fsm.clearLastUtilityMsg();
							queue.wait();
						} 
						catch (InterruptedException ex) {
							if (shuttingDown) {
								logger.info(PC2LogCategory.Model, subCat, "FSM(" + name + ") complete.");
								return;
							}
						}
					}
				}
					try {
						fsm.processEvent();
					}
					catch (Exception e) {
						logger.fatal(PC2LogCategory.Model, subCat, 
								"Exception in processEvent within the FSM", e);
					}

				
			}
			catch (Exception ex) {
			logger.error(PC2LogCategory.Model, subCat,
					name + " encountered an error.", ex);
			}
		
		}
//		logger.info(PC2LogCategory.Model, subCat, "FSM(" + name + ") complete.");
		stopCapture();
		fsm.stop();
		ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
		rpf.shutdown();
		if (parsedCaptureDBs != null)
			parsedCaptureDBs.clear();
	}
	
	/**
	 * Shutdown the thread and notify the application the
	 * test is complete.
	 */
	@Override
	public void shutdown() {
		fsm.stop();
		ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
		rpf.shutdown();
		shuttingDown = true;
		if (captureInterface != null ||
				captureRunning) {
			stopCapture();
		}
		logger.debug(PC2LogCategory.Model, subCat, "Shutting down FSM(" + name + ").");
		
		// This gives an event for the FSM thread to be awakened and processed
		InternalMsg msg = new InternalMsg(fsm.getUID(), System.currentTimeMillis(), 
				LogAPI.getSequencer(), EventConstants.SHUTDOWN);
		processEvent(msg);
		
		if (parsedCaptureDBs != null)
			parsedCaptureDBs.clear();

	}
	
	protected boolean sendSNMP(Send s, FSMListener listener, String label) {
		boolean result = false;
		String prompt = null;
		if (label == null)
			prompt = UtilityConstants.SNMP_REBOOT_PROMPT;
		else 
			prompt = "Power on (or reset) the " + label + " EDVA.";

		UtilityDistributor ud = Stacks.getUtilDistributor();
		ListIterator<Mod> iter = s.getModifiers().listIterator();
		boolean promptUser = false;
		while (iter.hasNext() && !promptUser) {
			Mod m = iter.next();
			if (m.getHeader().equals(UtilityConstants.SNMP_ARRAY)) {
				Reference ref = m.getRef();
				String value = refLocator.getReferenceInfo(fsm.getUID(), ref, null);
				if (value != null && 
						(value.contains(UtilityConstants.SNMP_DVA_REBOOT_OID) || 
								value.contains(UtilityConstants.SNMP_CM_REBOOT_OID)));
					promptUser = true;
			}
		}
		if (promptUser) {
			// Use the logMsg category because this one is forced to
			// all files and the console. 
			
			
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			String snmpPort = platform.getProperty(SettingConstants.SNMP_PORT);
			boolean autoRespond = false;
			if (snmpPort != null) {
				try {
					int port = Integer.parseInt(snmpPort);
					if (port > 0) {
						UtilityMsg um = ud.send(s, this);
						if (um != null) {
							MsgEvent msgSent = um;
							ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
							if (rpf != null)
								rpf.sent(msgSent);
							q.add(msgSent);
							result = true;
							logger.info(PC2LogCategory.LOG_MSG, subCat, 
									"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() + ") sent event (" 
									+ msgSent.getEventName() + ") sequencer=" + msgSent.getSequencer() + ".");
						}
					}
				}
				catch (NumberFormatException nfe) {
					logger.warn(PC2LogCategory.Model, subCat,
							"Model couldn't convert the setting [" 
							+ SettingConstants.SNMP_PORT + "] to an integer." );
					autoRespond = true;
				}
			
			}
			if (autoRespond) {
				logger.info(PC2LogCategory.LOG_MSG, subCat, " USER: \n" + prompt);
				if (PCSim2.isGUIActive() && label == null) {
					PC2UI ui = PCSim2.getUI();
					ui.notifyUser(UtilityConstants.SNMP_REBOOT_PROMPT, false, false);
				}
				UtilityMsg msgSent = ud.autoRespond(s, this, (Boolean)null);
				ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
				if (rpf != null) {
					rpf.sent(msgSent);
					q.add(msgSent);
					result = true;
				}
			}
		}
		else {
			logger.error(PC2LogCategory.LOG_MSG, subCat, 
					"Couldn't send SNMP Utility message because either Utility or SNMP is not enabled.");
		}
		
		return result;
	}
	/**
	 * Implementation for the Send action within a FSM.
	 * 
	 * @return - returns the sent message's message queue index, -1 otherwise
	 */
	@Override
	public boolean send(Send s) {
		if (s.getProtocol().equals(MsgRef.SIP_MSG_TYPE)) {
			locateRequest(s);
			SIPDistributor dist = Stacks.getSipDistributor();
			if (dist != null) {
				// Next look for this dialog in the table
				SIPMsg msgSent =  dist.send(this, s, 
						fsm.getNetworkElements(), fsm.getLastUtilityMsg(),
						fsm.getSipStack());
				if (msgSent == null) {
					logger.warn(PC2LogCategory.LOG_MSG, subCat, 
							"FSM (" + fsm.getName() + ") - State (" 
							+ fsm.getCurrentStateName() 
							+ ") failed to send any message for send action(" + s   
							+ ".");
							return false;
				}
					
				
				ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
				if (rpf != null) {
//					if (msgSent.getEventName().equalsIgnoreCase("183-Invite") ||
//							msgSent.getEventName().equalsIgnoreCase("180-Invite")) {
//						int glh = 0;
//					}
					rpf.sent(msgSent);
					
					q.add(msgSent);
//					 LOG USING LOGMSG category so that the user can't disable which
					// would break the trace tool.
					logger.info(PC2LogCategory.LOG_MSG, subCat, 
							"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() + ") sent event (" 
							+ msgSent.getEventName() + ") sequencer=" + msgSent.getSequencer() + ".");
					
				}
				return true;
				
			}
		}
		else if (s.getProtocol().equals(MsgRef.UTILITY_MSG_TYPE)) {
			return sendUtility(s, this);
		}
		
		else if (s.getProtocol().equals(MsgRef.STUN_MSG_TYPE) || 
				s.getProtocol().equals(MsgRef.RTP_MSG_TYPE)) {
			locateRequest(s);
			StunDistributor dist = Stacks.getStunDistributor();
			if (dist != null) {
				MsgEvent msgSent =  dist.send(this, s, fsm.getNetworkElements());
				
				if (msgSent == null) {
					logger.warn(PC2LogCategory.LOG_MSG, subCat, 
							"FSM (" + fsm.getName() + ") - State (" 
							+ fsm.getCurrentStateName() 
							+ ") failed to send any message for send action(" + s   
							+ ".");
							return false;
				}
					
				if (msgSent != null) {
		
					q.add(msgSent);
//					 LOG USING LOGMSG category so that the user can't disable which
					// would break the trace tool.
					logger.info(PC2LogCategory.LOG_MSG, subCat, 
							"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() + ") sent event (" 
							+ msgSent.getEventName() + ") sequencer=" + msgSent.getSequencer() + ".");
				}
				return true;
			}
		}
		return false;
	}
	
	protected boolean sendUtility(Send s, FSMListener listener) {
		boolean result = false;
		UtilityDistributor ud = Stacks.getUtilDistributor();
		UtilityMsg um = null;
		// First see if the message is Voicetronix, because if it is 
		// and Voicetronix is disabled, then the user will be prompted
		// first before delivering it to the distributor for generating
		// the automated response.
		if (!UtilityDistributor.voicetronixEnabled() && 
				UtilityConstants.isVoicetronixMsg(s.getMsgType())) {
			if (UtilityDistributor.verifyForVoicetronix(s.getMsgType())) {
				
				fsm.suspendForPrompt();
				logger.info(PC2LogCategory.Model, subCat, 
						"Pausing until user responds.");
				if (PCSim2.isGUIActive()) {
					PC2UI ui = PCSim2.getUI();
					VoicetronixPort[] vps = getNetworkElementForUtilityPrompt(s);
					boolean yesExpected = ud.getExpectedAnswer(s, fsm.getUID());
					
					String prompt = UtilityConstants.getVoicetronixPrompt(s, fsm.getUID(), vps, yesExpected);
					logger.info(PC2LogCategory.LOG_MSG, subCat, " USER: \n" + prompt);
					//boolean response = ui.notifyUser(prompt, true, yesExpected);
					// Now that the system prompts the user based upon the voiceExpected attribute
					// we should always expect a yes response to the prompt
					boolean response = ui.notifyUser(prompt, true, true);
					UtilityMsg msgSent = ud.autoRespond(s, this, response);
					ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
					if (rpf != null) {
						rpf.sent(msgSent);
						q.add(msgSent);
						result = true;
					}
					fsm.resumeFromPrompt();
				}
				logger.info(PC2LogCategory.Model, subCat, 
					"Resume processing following user response.");
			}
			else if (UtilityDistributor.promptForVoicetronix(s.getMsgType())) {
				// Get the network element label that has the matching
				// line and device of voicetronix
// BRK PT
//				if (s.getMsgType().equalsIgnoreCase(UtilityConstants.OFFHOOK)) {
//					int glh = 0;
//				}
				
				VoicetronixPort [] vps = getNetworkElementForUtilityPrompt(s);
				
				// If the message is an onhook event and the device is not
				// in the reference point factory table, the device should
				// already be on hook so ignore operation.
				if (s.getMsgType().equals(UtilityConstants.ONHOOK)) {
					if (vps != null) {
						UtilityMsg onhook = ReferencePointsFactory.getOffHookMsg(vps[0].getPort());
						if (onhook == null) {
							logger.info(PC2LogCategory.Model, subCat, 
									"Not issuing OnHook operation because " + vps[0].getNELabel()
						           + " is already on hook based upon the reference point factory.");
							return true;
						}
					}
				}
				//logger.info(PC2LogCategory.Model, subCat, 
				//	"Pausing until user responds.");
				String prompt = UtilityConstants.getVoicetronixPrompt(s, fsm.getUID(), vps, true);

				// Use the logMsg category because this one is forced to
				// all files and the console. 
				logger.info(PC2LogCategory.LOG_MSG, subCat, " USER: \n" + prompt);
				UtilityMsg msgSent = ud.autoRespond(s, this, (Boolean)null);
				ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
				if (rpf != null) {
					rpf.sent(msgSent);
					q.add(msgSent);
					result = true;
				}
			}
		}	
		else if (UtilityConstants.isSNMPMsg(s.getMsgType())) {
			// Determine if the oid in question is 
			if ( !UtilityDistributor.snmpEnabled() && 
					s.hasModifiers()) {
				result = sendSNMP(s, listener, null);
			}
			else {
				um = ud.send(s, this);
				if (um != null) {
					MsgEvent msgSent = um;
					ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
					if (rpf != null)
						rpf.sent(msgSent);
					q.add(msgSent);
					result = true;
					logger.info(PC2LogCategory.LOG_MSG, subCat, 
							"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() + ") sent event (" 
							+ msgSent.getEventName() + ") sequencer=" + msgSent.getSequencer() + ".");
					
					
				}
			}
		}	
		else {
			um = ud.send(s, this);
			if (um != null) {
				MsgEvent msgSent = um;
				ReferencePointsFactory rpf = fsm.getReferencePointsFactory();
				if (rpf != null)
					rpf.sent(msgSent);
				q.add(msgSent);
				result = true;
				logger.info(PC2LogCategory.LOG_MSG, subCat, 
						"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() + ") sent event (" 
						+ msgSent.getEventName() + ") sequencer=" + msgSent.getSequencer() + ".");

			}
		}
		
		return result;
	}
	
	/**
	 * Implemenation for the Sleep action within a FSM.
	 * 
	 * @return - true when sleep invoked, false otherwise
	 */
	@Override
	public boolean sleep(Sleep s) {
		if (fsm != null) {
			fsm.sleep(s.getTime());
			return true;
		}
		return false;
	}
	
	
	
	protected String stripContact(String contact) {
		if (contact.contains(";bnc")) {
			contact = contact.replace(";bnc", "");
		}
		if (contact.charAt(0) == '<') {
			if (contact.charAt(contact.length()-1) == '>') {
				return contact.substring(1,contact.length()-1);
			}
		}
	
		return contact;
	}

	protected void startCapture(Capture c) {
		SystemSettings ss = SystemSettings.getInstance();
		captureTextGenerated = false;
		
		Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
		String wiresharkDir = platform.getProperty(SettingConstants.WIRESHARK_DIRECTORY);
		if (wiresharkDir != null)
			captureInterface = new Wireshark(wiresharkDir, CAPTURE_AUTO_TERMINATE);
		else
			captureInterface = new Wireshark(CAPTURE_AUTO_TERMINATE);
		//String program = wiresharkDir + "/" + captureApp;
		captureFileName = ss.getNextCaptureName();
		Reference filterRef = c.getFilter();
		String filter = null;			
		if (filterRef != null) {
			filter = "\"" + refLocator.getReferenceInfo(fsm.getUID(), filterRef, null) + "\"";
		}				

		String interfaceNum = Integer.toString(ss.getCaptureInterface());
		try {
			String program = captureInterface.useTShark(wiresharkDir);
			if (filter != null)
				captureInterface.startCapture(program, "-i", interfaceNum, "-w", (captureFileName + ".pcap"), "-f", filter, "-l", "-q");
			else
				captureInterface.startCapture(program, "-i", interfaceNum, "-w", (captureFileName + ".pcap"), "-l", "-q");
			captureRunning = true;
			logger.info(PC2LogCategory.Model, subCat, 
					"Starting capture using " + captureApp + ". Capture recorded in " + captureFileName + ".pcap " 
					+ ((filter != null) ? filter : ""));

		}
		catch (IOException ioex) {
			logger.error(PC2LogCategory.Model, subCat, 
					"System encountered an exception while trying to start a capture using " + 
					captureApp + ".\n" + ioex.getMessage() + "\n" + ioex.getStackTrace());
		}
	}
	protected void stopCapture() {
		if (captureInterface != null && captureRunning) {
			logger.info(PC2LogCategory.Model, subCat, "Stopping capture.");
			captureInterface.stopCapture();
		}
		captureRunning = false;
	}
	
	@Override
	public boolean stream(Stream s, int msgQueueIndex) {
		StunDistributor dist = Stacks.getStunDistributor();
		if (dist != null) {
			Thread t =  dist.stream(fsm.getUID(), s, fsm.getNetworkElements());
			if (!s.isStop() && 
					(t == null || 
							!t.isAlive())) {
				logger.warn(PC2LogCategory.LOG_MSG, subCat, 
						"FSM (" + fsm.getName() + ") - State (" 
						+ fsm.getCurrentStateName() 
						+ ") failed to start streaming data for start_stream action(" + s   
						+ ".");
				return false;
			}
			else if (s.isStop() && 
					t != null &&
					t.isAlive())	{
				logger.warn(PC2LogCategory.LOG_MSG, subCat, 
						"FSM (" + fsm.getName() + ") - State (" 
						+ fsm.getCurrentStateName() 
						+ ") failed to stop streaming data for stop_stream action(" + s   
						+ ".");
				return false;
				
			}
			else if (!s.isStop() && 
					t != null && 
					t.isAlive()){
				logger.info(PC2LogCategory.LOG_MSG, subCat, 
						"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() 
						+ ") started streaming information to=" +  s.getDestIP() 
						+ "|" + s.getDestPort() + ".");
				return true;
			}
			else if (s.isStop() && 
						(t == null || 
							!t.isAlive())) {
				logger.info(PC2LogCategory.LOG_MSG, subCat, 
						"FSM (" + fsm.getName() + ") - State (" + fsm.getCurrentStateName() 
						+ ") stopped streaming information to=" +  s.getDestIP() 
						+ "|" + s.getDestPort() + ".");
				return true;
			}
			
		}
		return false;
	}
	
	/**
	 * The verify is the current verification that needs to be executed.
	 * The msgQueueIndex is the index of the current event being processed.
	 */
	@Override
	public boolean verify(Verify v, int msgQueueIndex) {
		boolean result = false;
		MsgEvent event = q.get(msgQueueIndex);
// GLH Comment out the need for the event to not be null. If this creates an issue, add test in performOp to make sure the msg_instance is current
		//		if (event != null) {
			try {
				v.performOp(this, fsm.getComparisonEvaluator(), event);
				result = true;
			}
			catch (Exception e) {
				String err = "Exception encountered during verify action processing.";
				logger.warn(PC2LogCategory.Model, subCat, err);
//				throw new PC2Exception(err + e.getMessage() + "\n" + e.getStackTrace());
			}
//		}
//		else {
//			logger.warn(PC2LogCategory.Model, subCat, "verify not executed because there are no messages in the queue.\n" + v);
//		}
		return result;
	}
	
}
