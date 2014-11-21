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

import java.text.ParseException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Properties;

import javax.sip.message.Request;

import com.cablelabs.fsm.ChangeStatus;
import com.cablelabs.fsm.EventConstants;
import com.cablelabs.fsm.FSM;
import com.cablelabs.fsm.FSMListener;
import com.cablelabs.fsm.Generate;
import com.cablelabs.fsm.InternalMsg;
import com.cablelabs.fsm.MsgEvent;
import com.cablelabs.fsm.MsgQueue;
import com.cablelabs.fsm.PresenceStatus;
import com.cablelabs.fsm.SIPConstants;
import com.cablelabs.fsm.SIPMsg;
import com.cablelabs.fsm.Send;
import com.cablelabs.fsm.SettingConstants;
import com.cablelabs.fsm.SystemSettings;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.sim.PCSim2;
import com.cablelabs.tools.SIPLocator;

/**
 * The PresenceServer class attempts to emulate the Presence
 * Server for the PC 2.0 architecture. The model itself maintains
 * presentity information as well as subscription information 
 * automatically. 
 * 
 * The Presence Server FSM XML document defines how and when to 
 * respond to any external events received by the system.
 * @author ghassler
 *
 */
public class PresenceServer extends PC2Models {

	/**
	 * The single PresenceServer class for the platform.
	 */
	private static PresenceServer server = null;

	public final static String NAME = "PresenceServer";
	public final static String MY_LABEL = "PRES1";
	
	// We are going to need to have some data for the 
	// maintaining the status of all of the presentities
	// and requests for subscription
	/**
	 * This is a list of the presence data indexed on the network element label
	 * of the device.
	 */
	private static Hashtable<String, PresenceData> neTable = new Hashtable<String, PresenceData>();
	
	/**
	 * This is a list of presence data indexed on the entity-tag returned in the
	 * SIP-If-Match header of the 200 OK to the Subscribe.
	 */
	private static Hashtable<Long, PresenceData> entityTable = new Hashtable<Long, PresenceData>();

	private SIPLocator sipLocator = SIPLocator.getInstance();
	
	private LinkedList<PresenceData> notifyWatchers = new LinkedList<PresenceData>();
	
	private PresenceServer(FSM f, LinkedList<ChangeStatus> initDB) {
		super(f, NAME, NAME);
		
		fsm.setAPI(this);
		fsm.setComparisonEvaluator(examiner);
		ListIterator<ChangeStatus> iter = initDB.listIterator();
		while (iter.hasNext()) {
			ChangeStatus cs = iter.next();
			PresenceData pd = new PresenceData(cs.getLabel(), cs.getStatus(), 
					System.currentTimeMillis());
			if (pd != null) {
				neTable.put(pd.getLabel(), pd);
				entityTable.put(pd.getEntity(), pd);
			}
			
		}
	}

	static public long getEntityTag(String ne) {
		if (server != null) {
			PresenceData pd = neTable.get(ne);
			if (pd != null) {
				return pd.getEntity();
			}
		}	
		return -1;
	}
	/**
	 * Obtains a reference to the singleton PresenceServer class.
	 * 
	 * @return 
	 */
	static public PresenceServer getInstance(FSM f, LinkedList<ChangeStatus> initDB) {
		if (server == null)
			server = new PresenceServer(f, initDB);
		
		return server;
	}

	@Override
	public void init() {
		try {
			super.init();
			fsm.init(queue, this);
			Properties platform = SystemSettings.getSettings(SettingConstants.PLATFORM);
			if (platform != null) {
				try {
					int noResponseTimeout = Integer.parseInt(
							platform.getProperty(SettingConstants.PRESENCE_SERVER_TIMEOUT));
					fsm.setDefaultNoResponseTimeout(noResponseTimeout);
					logger.info(PC2LogCategory.Model, "",
							"PresenceServer's No Response Timeout set to " 
							+ noResponseTimeout + ".");
				}
				catch (NumberFormatException nfe) {
					logger.warn(PC2LogCategory.Model, "",
							"PresenceServer could not set the No Response Timeout from " 
							+ "the settings, because value isn't a number. System using 60000 " 
							+ "msec. by default");
					fsm.setDefaultNoResponseTimeout(60000);
				}
			}
			this.start();
		}
		catch (IllegalStateException ise) {
			PCSim2.setTestPassed(false);
			String err = "PresenceServer model failed during state machine initialization." + 
			" Test terminated. Declaring test case failure.";
			logger.fatal(PC2LogCategory.Model, subCat, err);
			shuttingDown = true;
			PCSim2.setTestComplete();
		}
	}
//	private void informWatchers(PresenceData pd) {
//		Send s = new Send()
//	}

	protected boolean newStatus(String label, PresenceStatus status) {
		PresenceData pd = neTable.get(label);
		if (pd != null) {
			pd.setStatus(status);
			return true;
		}
		else {
			Properties p = SystemSettings.getSettings(label);
			if (p != null) {
//				String simulated = p.getProperty(SettingConstants.SIMULATED);
//				if (SystemSettings.resolveBooleanSetting(simulated)) {
//					
//				}
				pd = new PresenceData(label, status, System.currentTimeMillis());
				neTable.put(pd.getLabel(), pd);
				entityTable.put(pd.getEntity(), pd);
				return true;
			}
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
			// We have some special processing to do if the event
			// is a Publish message
			if (event.getEventName().equals(SIPConstants.PUBLISH)) 
				updateStatus(event);
				
			if (processDuplicates && event.isDuplicate()) 
				result = fsm.processEvent(event);
					
			else if (!event.isDuplicate()) 
				result = fsm.processEvent(event);
				
			else
				logger.info(PC2LogCategory.Model, subCat, 
					"Dropping duplicate event.");
				
		}
		else if (!event.equals(EventConstants.SHUTDOWN)) {
			logger.info(PC2LogCategory.Model, subCat, "Model " 
					+ name + " unable to add event(" + event.getEventName() 
					+ ") to QUEUE because fsm=" + fsm + " and shutting down=" 
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
			if (g.getEvent().equalsIgnoreCase(EventConstants.OPEN)) {

				informSessions(g.getEvent());
				InternalMsg msg = new InternalMsg(getFsmUID(), 
						System.currentTimeMillis(), LogAPI.getSequencer(), g.getEvent());

				((FSMListener)this).processEvent(msg);
				return true;
			}
			else if (g.getEvent().equalsIgnoreCase(EventConstants.CLOSED)) {

				informSessions(g.getEvent());
				InternalMsg msg = new InternalMsg(getFsmUID(), 
						System.currentTimeMillis(), LogAPI.getSequencer(), g.getEvent());

				((FSMListener)this).processEvent(msg);
				return true;
			}	
		}
		return super.processEvent(g);
	}
	
	static public String createPresenceBody(Properties p) throws ParseException {
		Properties pres1 = SystemSettings.getSettings(MY_LABEL);
		if (pres1 != null) {
			String srcNE = p.getProperty(SettingConstants.NE);
			String username = pres1.getProperty(SettingConstants.USER_NAME);
			String domain = pres1.getProperty(SettingConstants.DOMAIN);
			if (username != null && domain != null) {
			String body = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\r\n" +
			   "<presence xmlns=\"urn:ietf:params:xml:ns:pidf\"" +
			       "entity=\"pres:" + username + "@" + domain+ "\"\r\n" +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" +
				"xmlns:dm=\"urn:ietf:params:xml:ns:pidf:data-model\"\r\n" +
				"xmlns:r=\"urn:ietf:params:xml:ns:pidf:rpid\">\r\n";
			Enumeration<String> nes = neTable.keys();
			while (nes.hasMoreElements()) {
				String ne = nes.nextElement();
				// Ignore the requester's status
				if (!ne.equals(srcNE)) {
					PresenceData pd = neTable.get(ne); 
					if (pd != null) {
						Properties neProp = SystemSettings.getSettings(ne);
						if (neProp != null) {
							String user = neProp.getProperty(SettingConstants.USER_NAME);
							String userDomain = neProp.getProperty(SettingConstants.DOMAIN);
							body += "<tuple id=\"" + pd.getLabel() + "\">\r\n<status>\r\n<basic>" 
							+ pd.getStatusString() 
							+ "</basic>\r\n</status>\r\n<contact priority=\"0.8\">sip:"
							+ user + "@" + userDomain + "</contact>\r\n</tuple>\r\n"
							+ "<dm:device id=\"stb002\">\r\n" +
					     		"<dm:note>cnn,51</dm:note>\r\n" +
					     		"</dm:device>\r\n" +
					     		"<dm:person id=\"bob\">\r\n" +
					     		"<r:activities>\r\n" +
					     		"<r: tv/>\r\n" +
					     		"</r:activites>\r\n" +
					     		"<r:mood>\r\n" +
					     		"<r:sad/>\r\n" +
					     		"</r:mood>\r\n" +
					     		"</dm:person>\r\n";
						}
					}
				}
//				"<tuple id=\"ue1\">\r\n" +
//			       "<status>\r\n" +
//			         "<basic>open</basic>\r\n" +
//			       "</status>\r\n" +
//			       "<contact priority=\"0.8\">sip:UE1@pclab.com</contact>\r\n" +
//			     "</tuple>\r\n" +
			}
			body += "</presence>\r\n";
			return body;
			}
			else {
				String msg = SettingConstants.USER_NAME + " or  " 
				+ SettingConstants.DOMAIN 
				+ " property is not set for the " + MY_LABEL 
				+ " in the Platform Configuration File.";
				throw new ParseException(msg, 0);
			}
		}
		else {
			String msg = "PresenceServer could not locate its property information";
			throw new ParseException(msg, 0);
		}
	}
	
	@Override
	public void run() {
		super.run();
		server = null;
	}
	
	@Override
	public boolean send(Send s) {
		return super.send(s);
	}
	
	private void updateStatus(MsgEvent event) {
		// TODO GAREY -  Publish response needs to include the SIP-Etag header
		// we assign this value and reuse in future PUBLISH response.
//		The rules for what type of publish event it is is based upon the values
//		in the table below:
//		      +-----------+-------+---------------+---------------+
//		      | Operation | Body? | SIP-If-Match? | Expires Value |
//		      +-----------+-------+---------------+---------------+
//		      | Initial   | yes   | no            | > 0           |
//		      | Refresh   | no    | yes           | > 0           |
//		      | Modify    | yes   | yes           | > 0           |
//		      | Remove    | no    | yes           | 0             |
//		      +-----------+-------+---------------+---------------+
		if (event instanceof SIPMsg) {
			Request req = ((SIPMsg)event).getRequest();
			if (req != null) {
				String publish = req.toString();
				// Get the SIP-If-Match information
				String entityTag = sipLocator.getSIPParameter("SIP-If-Match", 
						"entity-tag",  MsgQueue.FIRST, publish);
				// Get the Expires value
				String expires = sipLocator.getSIPParameter("Expires", 
						"value", MsgQueue.FIRST, publish);
				// Get the Content-Length	value
				String length = sipLocator.getSIPParameter("Content-Length", 
						"value", MsgQueue.FIRST, publish);
				int exp = -1;
				boolean hasBody = false;
				int contentLen = -1;
				if (length != null) {
					try {
						contentLen = Integer.parseInt(length);
						if (expires != null) {
							exp = Integer.parseInt(expires);
						}
						if (contentLen > 0)
							hasBody = true;
					}
					catch (NumberFormatException nfe) {
						logger.warn(PC2LogCategory.Model, subCat,
								"PresenceSever retrieved a value from the Content-Length header that doesn't appear to be an integer.");
						
					}
				}
				// Initial condition
				if (entityTag == null) {
					Properties p = SystemSettings.getPropertiesByValue(SettingConstants.IP, event.getSrcIP());
					if (p != null) {
						String ne = p.getProperty(SettingConstants.NE);
						PresenceData pd = new PresenceData(ne, PresenceStatus.OPEN, System.currentTimeMillis());
						neTable.put(pd.getLabel(), pd);
						entityTable.put(pd.getEntity(), pd);
					}
				}
				else if (entityTag != null) {
					// Remove condition
					if (contentLen == 0 && exp == 0) {
						PresenceData pd = entityTable.get(entityTag);
						if (pd != null)
							notifyWatchers.add(pd);
					}
					else if (exp > 0) {
						// This indicates that the device is only updating its existing
						// status.
						PresenceData pd = entityTable.remove(entityTag);
						if (pd != null) {
							pd.updateEntity(System.currentTimeMillis());
							entityTable.put(pd.getEntity(), pd);
							if (hasBody) {
								// This indicates that the device is modifying its information
								notifyWatchers.add(pd);
							}
						}
						
					}
				}
			}
		}
	}
	
	
}
