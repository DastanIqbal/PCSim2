/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.vpbapi;

import java.util.*;
import java.io.IOException;
import java.io.File;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.utility.*;

public class VpbAPI implements Runnable {
	
	public static final int BOARD = 1;
	public static final int CHANNELS = 4;
	// The latest version of the vpbapi.h file doesn't contain settings
	// for the VPB_REORDER and VPB_CWTONE any more. We need to change them
	// to match the new 
	public static final int REORDER = 7;
	public static final int CALL_WAITING = 8;
	public static final String WINDOWS_LIB = "vpb_jni";
	public static final String VPB_LIB = "libvpb";
	public static final String LINUX_LIB = "vpb_jni";
	public static final String PLXAPI_LIB = "PlxApi";
	// open needs to invoke vpb_seterrormode(VPB_EXCEPTION);
	protected native int open_card(int port);
	protected native void close_card(int channel);
	protected native int onhook_channel(int channel);
	protected native int offhook_channel(int channel);
	protected native int dial_channel(int channel, String digits);
	protected native int hookflash_channel(int channel);
	protected native boolean check_dialtone(int channel, int timeout);
	protected native void detectTone(int channel, int id);
	protected native int setRingback(int channel);
	protected native int setReorder(int channel);
	protected native int setDialTone(int channel);
	protected native int setBusyTone(int channel);
	protected native int setCallWaiting(int channel);
	protected native int playFile(int channel, String file);
	protected native int setFlash(int timems);
	
	// A timeout of zero will have the thread hold until an event occurs
	protected native void getEvent(int timeout);
	private VpbEvent event = null;
	private LinkedList<VpbEvent> events = new LinkedList<VpbEvent>();

	//public static final int DEBUG = 1;
	//public static final int ERROR_CODE = 2;
	//public static final int EXCEPTIONS = 3;
	//public native void seterrormode(int mode);
	
//	private ConcurrentLinkedQueue<VpbEvent> queue = new
//	ConcurrentLinkedQueue<VpbEvent>();
	private UtilityListener listener  = null;
	
	/**
	 * Create 5 handles using index 1-4 to represent the ports
	 * on the card.
	 */
	private int handles[] = new int[CHANNELS];
	
	/**
	 * This allows the system to operate statelessly. The platform
	 * doesn't care to receive events from the Voicetronix board 
	 * unless it has specifically requested the information.
	 * 
	 */
	private Monitor[] monitor = new Monitor[CHANNELS];
	
	private Integer[] twoWaySecondChannel = new Integer [CHANNELS];
	/**
	 * A flag that is set to false to exit the stack.
	 */
	private boolean isRunning;
	
	private Thread thread = null;
	/**
	 * The subcategory to use when logging
	 * 
	 */
	private String subcat = "API";
	
	public LogAPI logger = LogAPI.getInstance();
	
	static {
		String osname = System.getProperty("os.name");
		//System.out.println(System.getProperties());
		String dir = System.getProperty("user.dir");
		String path = null;
		int slash = dir.lastIndexOf(File.separator);
		if(osname.toLowerCase().startsWith("windows")) {
//			System.loadLibrary(VPB_LIB);
//			System.loadLibrary("PlxApi");
//			System.loadLibrary(WINDOWS_LIB);
			if (slash != -1) {
				path = dir.substring(0, slash+1) + "lib" + File.separator + "win32" + File.separator;
			}
			// We use load instead of load path so that we can have two copies of the library on the system one
			// for windows and one for linux
			
			System.load(path + PLXAPI_LIB + ".dll");
			System.load(path + VPB_LIB + ".dll");
			System.load(path + WINDOWS_LIB + ".dll");
		}
		else {
			if (slash != -1) {
				path = dir.substring(0, slash+1) + "lib" + File.separator + "linux" + File.separator;
			}
			System.load(path + "lib" + PLXAPI_LIB + ".so");
			System.load(path + "lib" + VPB_LIB + ".so");
			
			System.load(path + "lib" + LINUX_LIB + ".so");
		}
	}
	
	public VpbAPI(UtilityListener listener) {
		this.listener = listener;
	}
	
	public void busy(int channel, int transID, boolean expected) {
		VpbEventType type = VpbEventType.BUSY;
		if (!canMonitor(channel, type)) {
			generateFailure(monitor[channel], channel);
		}
		monitor[channel] = new Monitor(VpbEventType.BUSY, transID, expected);
		// System.out.println(
		logger.debug(PC2LogCategory.UTILITY, subcat, 
				"VPB API begin monitoring for busy on channel[" + channel +"].");
		
	}
	
	public void callwaiting(int channel, int transID, boolean expected) {
		VpbEventType type = VpbEventType.CALL_WAITING_TONE;
		if (!canMonitor(channel, type)) {
			generateFailure(monitor[channel], channel);
		}
		monitor[channel] = new Monitor(VpbEventType.CALL_WAITING_TONE, transID, expected);
		// System.out.println(
		logger.debug(PC2LogCategory.UTILITY, subcat, 
				"VPB API begin monitoring for call waiting tone on channel[" + channel +"].");
	}
	
	private boolean canMonitor(int channel, VpbEventType event) {
		if (monitor[channel] != null) {
			// System.out.println(
			logger.debug(PC2LogCategory.UTILITY, subcat, 
					"Received request to monitor for event[" 
					+ event 
					+ "] but the system is already monitoring for the event["
					+ monitor[channel].type + "]");
			return false;
		}
		return true;
	}
	private void createEvent(String eventType, int transID, String errorMsg) {
		String tID = ((Integer)transID).toString();
		UtilityMessage um = new UtilityMessage(eventType, tID);
		if (errorMsg != null) {
			UtilityAttribute ua = new UtilityAttribute("errorMsg", errorMsg);
			um.addAttribute(ua);
		}	
		UtilityEvent ue = new UtilityEvent(um, LogAPI.getSequencer(),  null, 0, null, 0);
		
		listener.processEvent(ue);
	}
	
	/**
	 * This method allows the wrapper library to create a VpbEvent in the system 
	 * when an unsolicited event is generated by the VPB library.
	 * @param type - type of event
	 * @param handle - the channel generating the event
	 * @param data - the details of the event for elements such as TONEDETECTED
	 * @param data1 - ?
	 */
	public void createEvent(int type, int handle, int data, long data1, String eventStr) {
		//logger.info(PC2LogCategory.UTILITY, subCat, 
		// Update the eventStr for the correct phrase
		if (type ==  2 && data == 4) {
			eventStr = "EventStr=[00] Tone Detect: Reorder";
		}
		else if (type ==  2 && data == 5) {
			eventStr = "EventStr=[00] Tone Detect: Call Waiting";
		}
		// System.out.println(
		logger.debug(PC2LogCategory.UTILITY, subcat, 
				"Received Voicetronix Event type=" + type + " handle=" + handle + " data=" 
				+ data + " data1=" + data1 + "\nEventStr=" + eventStr);
		switch (type) {
			
			case 0:
			case 2:
			case 4:
			case 100:
			case 102:
				VpbEvent e = new VpbEvent(type, handle, data, data1);
				events.add(e);
				break;

				
		}
	}
	
	public void dial(int channel, String digits, int transID) {
		VpbEventType type = VpbEventType.DIAL;
		if (!canMonitor(channel, type)) {
			generateFailure(monitor[channel], channel);
		}
		if (digits != null) {
			dial_channel(handles[channel], digits);
			monitor[channel] = new Monitor(VpbEventType.DIAL, transID, true);
			// System.out.println(
			logger.debug(PC2LogCategory.UTILITY, subcat, 
					"VPB API begin monitoring for dial digits on channel[" + channel +"].");
		}
		else {
			generateFailure(monitor[channel], channel);
		}
	}
	
	public void dialtone(int channel, int transID, boolean expected) {
		VpbEventType type = VpbEventType.DIALTONE;
		if (!canMonitor(channel, type)) {
			generateFailure(monitor[channel], channel);
		}
		monitor[channel] = new Monitor(VpbEventType.DIALTONE, transID, expected);
		// System.out.println(
		logger.debug(PC2LogCategory.UTILITY, subcat, 
				"VPB API begin monitoring for dial tone on channel[" + channel +"].");
	}
	
	private void generateFailure(Monitor m, int channel) {
		boolean error = m.expected;
		VpbEventType type = m.type;
		if (error) {
			String errType = VpbConstants.getErrorEventType(type);
			if (type.equals(VpbEventType.VOICE_DETECTED) &&
					(monitor[channel].twoWayVoiceVerify ||
							twoWaySecondChannel[channel] != null)){
				errType = VpbConstants.VERIFY_VOICE_PATH_TWO_WAY_ERROR;
				twoWaySecondChannel[channel] = null;
			}
			if (errType != null) {
				createEvent(errType, m.transactionID, null);
			}
		}
		else {
			String compType = VpbConstants.getEventType(type);
			if (type.equals(VpbEventType.VOICE_DETECTED) &&
					(monitor[channel].twoWayVoiceVerify ||
							twoWaySecondChannel[channel] != null)){
				compType = VpbConstants.VERIFY_VOICE_PATH_TWO_WAY_COMPLETE;
				twoWaySecondChannel[channel] = null;
			}
			if (compType != null)
				createEvent(compType, m.transactionID, null);
		}
		monitor[channel] = null;
	}
	
	public void hookflash(int channel, int transID) {
		int result = hookflash_channel(handles[channel]);
		if (result != 0) {
			// System.out.println(
			logger.debug(PC2LogCategory.UTILITY, subcat, 
					"Hookflash operation failed for channel[" + channel 
					+ "] with return code=" + result + ".");
			createEvent(VpbConstants.HOOKFLASH_ERROR, transID, null);
		}
		else {
			//System.out.println(
			logger.debug(PC2LogCategory.UTILITY, subcat, 
					"Hookflash operation succeeded for channel[" + channel 
					+ "] with return code=" + result + ".");
			// HOOK FLASH ISN'T WORKING YET, SO ALWAYS GENERATE ERROR
			// createEvent(VpbConstants.HOOKFLASH_ERROR, transID, null);
			createEvent(VpbConstants.HOOKFLASH_COMPLETE, transID, null);
		}
	}

	public void init() {
		for (int i=0; i<CHANNELS; i++) {
			handles[i] = open_card(i+1);
			//System.out.println(
			logger.debug(PC2LogCategory.UTILITY, subcat, 
					"Handle[" + i + "] has value=" + handles[i]);
			int tone = setRingback(handles[i]);
			if (tone != 0)
				//System.err.println(
				logger.error(PC2LogCategory.UTILITY, subcat, 
						"SetRingback failed with return value " + tone);
			
			tone = setReorder(handles[i]);
			if (tone != 0)
				// System.err.println(
				logger.error(PC2LogCategory.UTILITY, subcat, 
						"SetReorder failed with return value " + tone);
			
			tone = setCallWaiting(handles[i]);
			if (tone != 0)
				//System.err.println(
				logger.error(PC2LogCategory.UTILITY, subcat, 
						"SetCallWating failed with return value " + tone);
			
			//tone = setDialTone(handles[i]);
			//if (tone != 0)
				//System.err.println(
			//	logger.error(PC2LogCategory.UTILITY, subcat, 
			//			"SetDialTone failed with return value " + tone);
		}
		
		int result = setFlash(500);
		if (result != 0)
			logger.error(PC2LogCategory.UTILITY, subcat, 
				"SetFlash failed with return value " + result);
		
		//System.out.println(
		logger.debug(PC2LogCategory.UTILITY, subcat, 
				"Handle has " + handles.length + " elements.");
		
	}
	
	public void offhook(int channel, int transID) {
		int result = offhook_channel(handles[channel]);
		if (result != 0) {
			//System.out.println(
			logger.debug(PC2LogCategory.UTILITY, subcat, 
					"Offhook operation failed for channel[" + channel 
					+ "] with return code=" + result + ".");
			createEvent(VpbConstants.OFFHOOK_ERROR, transID, null);
		}
		else {
			//System.out.println(
			logger.debug(PC2LogCategory.UTILITY, subcat, 
					"Offhook operation succeeded for channel[" + channel 
					+ "] with return code=" + result + ".");
			createEvent(VpbConstants.OFFHOOK_COMPLETE, transID, null);
		}
	}
	
	public void onhook(int channel, int transID) {
		int result = onhook_channel(handles[channel]);
		if (result != 0) {
			//System.out.println(
			logger.debug(PC2LogCategory.UTILITY, subcat, 
					"Onhook operation failed for channel[" + channel 
					+ "] with return code=" + result + ".");
			createEvent(VpbConstants.ONHOOK_ERROR, transID, null);
		}
		else {
			//System.out.println(
			logger.debug(PC2LogCategory.UTILITY, subcat, 
					"Onhook operation succeeded for channel[" + channel 
					+ "] with return code=" + result + ".");
			createEvent(VpbConstants.ONHOOK_COMPLETE, transID, null);
		}
	}
	
	
	public void reorder(int channel, int transID, boolean expected) {
		VpbEventType type = VpbEventType.REORDER;
		if (!canMonitor(channel, type)) {
			generateFailure(monitor[channel], channel);
		}
		monitor[channel] = new Monitor(VpbEventType.REORDER, transID, expected);
		//System.out.println(
		logger.debug(PC2LogCategory.UTILITY, subcat, 
				"VPB API begin monitoring for reorder tone on channel[" + channel +"].");
	}
	
	public void reset() {
		for (int i=0; i < CHANNELS; i++) {
			monitor[i] = null;
		}
	}
	
	public void ring(int channel, int transID, boolean expected) {
		VpbEventType type = VpbEventType.RINGING;
		if (!canMonitor(channel, type)) {
			generateFailure(monitor[channel], channel);
		}	
		monitor[channel] = new Monitor(VpbEventType.RINGING, transID, expected);
		// System.out.println(
		logger.debug(PC2LogCategory.UTILITY, subcat, 
				"VPB API begin monitoring for ringing on channel[" + channel +"].");
	}
	
	public void ringback(int channel, int transID, boolean expected) {
		VpbEventType type = VpbEventType.RINGBACK;
		if (!canMonitor(channel, type)) {
			generateFailure(monitor[channel], channel);
		}
		monitor[channel] = new Monitor(VpbEventType.RINGBACK, transID, expected);
		//System.out.println(
		logger.debug(PC2LogCategory.UTILITY, subcat, 
				"VPB API begin monitoring for ringback on channel[" + channel +"].");
	}
	
	public void run() {
		logger.info(PC2LogCategory.UTILITY, subcat, 
		//System.out.println(
				"Begin monitoring for VPB Events.");
		while (this.isRunning) {
			try	{
			    getEvent(3000);
			    if (events.size() > 0) {
			    	event = events.removeFirst();
					if (event.channel >= 0 && event.channel < CHANNELS) {

						if (monitor[event.channel] == null) {
							logger.debug(PC2LogCategory.UTILITY, subcat, "monitor[" + event.channel 
									+ "] is null, ignoring event.");
						}
						else {
							synchronized (monitor[event.channel]) {
								if (event.type == monitor[event.channel].type) {
									switch (event.type) {
									case RINGING :
										if (monitor[event.channel].expected)
											createEvent(VpbConstants.VERIFY_RING_COMPLETE, monitor[event.channel].transactionID, null);
										else
											createEvent(VpbConstants.VERIFY_RING_ERROR, monitor[event.channel].transactionID, null);
										monitor[event.channel] = null;
										break;
									case DIALTONE :
										if (monitor[event.channel].expected)
											createEvent(VpbConstants.VERIFY_DIAL_TONE_COMPLETE, monitor[event.channel].transactionID, null);
										else 
											createEvent(VpbConstants.VERIFY_DIAL_TONE_ERROR, monitor[event.channel].transactionID, null);
										monitor[event.channel] = null;
										break;
									case DIAL :
										if (monitor[event.channel].expected)
											createEvent(VpbConstants.DIAL_DIGITS_COMPLETE, monitor[event.channel].transactionID, null);
										else 
											createEvent(VpbConstants.DIAL_DIGITS_ERROR, monitor[event.channel].transactionID, null);
										monitor[event.channel] = null;
										break;
									case RINGBACK :
										if (monitor[event.channel].expected)
											createEvent(VpbConstants.VERIFY_RING_BACK_COMPLETE, monitor[event.channel].transactionID, null);
										else
											createEvent(VpbConstants.VERIFY_RING_BACK_ERROR, monitor[event.channel].transactionID, null);
										monitor[event.channel] = null;
										break;
									case REORDER :
										if (monitor[event.channel].expected)
											createEvent(VpbConstants.VERIFY_REORDER_COMPLETE, monitor[event.channel].transactionID, null);
										else
											createEvent(VpbConstants.VERIFY_REORDER_ERROR, monitor[event.channel].transactionID, null);
										monitor[event.channel] = null;
										break;
									case CALL_WAITING_TONE :
										if (monitor[event.channel].expected)
											createEvent(VpbConstants.VERIFY_CALL_WAITING_TONE_COMPLETE, monitor[event.channel].transactionID, null);
										else
											createEvent(VpbConstants.VERIFY_CALL_WAITING_TONE_ERROR, monitor[event.channel].transactionID, null);
										monitor[event.channel] = null;
										break;
									case BUSY :
										if (monitor[event.channel].expected)
											createEvent(VpbConstants.VERIFY_BUSY_COMPLETE, monitor[event.channel].transactionID, null);
										else
											createEvent(VpbConstants.VERIFY_BUSY_ERROR, monitor[event.channel].transactionID, null);
										monitor[event.channel] = null;
										break;
									case VOICE_DETECTED :
										if (monitor[event.channel].expected) {
											if (twoWaySecondChannel[event.channel] != null){
												voicepath(twoWaySecondChannel[event.channel], monitor[event.channel].transactionID, 
														monitor[event.channel].expected);
												Monitor m = monitor[twoWaySecondChannel[event.channel]];
												m.twoWayVoiceVerify = true;
												twoWaySecondChannel[event.channel] = null;
												monitor[event.channel] = null;
											}
											else if (monitor[event.channel].twoWayVoiceVerify) {
												createEvent(VpbConstants.VERIFY_VOICE_PATH_TWO_WAY_COMPLETE, monitor[event.channel].transactionID, null);
												monitor[event.channel] = null;
											}
											else {
												createEvent(VpbConstants.VERIFY_VOICE_PATH_COMPLETE, monitor[event.channel].transactionID, null);
												monitor[event.channel] = null;
											}
										}
										else {
											if (monitor[event.channel].twoWayVoiceVerify) {
												createEvent(VpbConstants.VERIFY_VOICE_PATH_TWO_WAY_ERROR, monitor[event.channel].transactionID, null);
											}
											else
												createEvent(VpbConstants.VERIFY_VOICE_PATH_ERROR, monitor[event.channel].transactionID, null);
											monitor[event.channel] = null;
										}
										break;
									}
								}
								else {
									//System.out.println(
									logger.debug(PC2LogCategory.UTILITY, subcat, 
											"Ignoring " + event.type + " event.");
								}
							}
						}
					}
				}
				
				//Next we need to see if any of the waiting items we are monitoring for have failed to 
				// receive a response and issue an error for them.
				long curTime = System.currentTimeMillis();
				for (int i=0; i< CHANNELS; i++) {
					if (monitor[i] != null) {
						if (curTime > monitor[i].expires) {
							switch (monitor[i].type) {

							case RINGING :
								if (monitor[i].type == VpbEventType.RINGING) {
									if (monitor[i].expected) {
										createEvent(VpbConstants.VERIFY_RING_ERROR, monitor[i].transactionID,
												"Timed out waiting ring tone.");
									}
									else
										createEvent(VpbConstants.VERIFY_RING_COMPLETE, monitor[i].transactionID, null);
									monitor[i] = null;
								}
								break;
							case DIALTONE :
								if (monitor[i].type == VpbEventType.DIALTONE) {
									if (monitor[i].expected)
										createEvent(VpbConstants.VERIFY_DIAL_TONE_ERROR, monitor[i].transactionID,
												"Timed out waiting dial tone.");
									else
										createEvent(VpbConstants.VERIFY_DIAL_TONE_COMPLETE, monitor[i].transactionID, null);
									monitor[i] = null;
								}
								break;

							case RINGBACK :
								if (monitor[i].type == VpbEventType.RINGBACK) {
									if (monitor[i].expected)
										createEvent(VpbConstants.VERIFY_RING_BACK_ERROR, monitor[i].transactionID,
												"Timed out waiting ringback tone.");
									else
										createEvent(VpbConstants.VERIFY_RING_BACK_ERROR, monitor[i].transactionID, null);
									monitor[i] = null;
								}
								break;
							case REORDER :
								if (monitor[i].type == VpbEventType.REORDER) {
									if (monitor[i].expected)
										createEvent(VpbConstants.VERIFY_REORDER_ERROR, monitor[i].transactionID,
												"Timed out waiting reorder tone.");
									else
										createEvent(VpbConstants.VERIFY_REORDER_COMPLETE, monitor[i].transactionID, null);
									monitor[i] = null;
								}
								break;
							case CALL_WAITING_TONE :
								if (monitor[i].type == VpbEventType.CALL_WAITING_TONE) {
									if (monitor[i].expected)
										createEvent(VpbConstants.VERIFY_CALL_WAITING_TONE_ERROR, monitor[i].transactionID,
												"Timed out waiting call waiting tone.");
									else
										createEvent(VpbConstants.VERIFY_CALL_WAITING_TONE_COMPLETE, monitor[i].transactionID, null);
									monitor[i] = null;
								}
								break;
							case BUSY :
								if (monitor[i].type == VpbEventType.BUSY) {
									if (monitor[i].expected)
										createEvent(VpbConstants.VERIFY_BUSY_ERROR, monitor[i].transactionID, 
												"Timed out waiting busy tone." );
									else
										createEvent(VpbConstants.VERIFY_BUSY_COMPLETE, monitor[i].transactionID, null);
									monitor[i] = null;
								}
								break;

							case VOICE_DETECTED :
								if (monitor[i].type == VpbEventType.VOICE_DETECTED) {
									if (monitor[i].twoWayVoiceVerify) {
										if (monitor[i].expected)
											createEvent(VpbConstants.VERIFY_VOICE_PATH_TWO_WAY_ERROR, monitor[i].transactionID, null);	
										else
											createEvent(VpbConstants.VERIFY_VOICE_PATH_TWO_WAY_COMPLETE, monitor[i].transactionID, null);
									}
									else {
										if (monitor[i].expected)
											createEvent(VpbConstants.VERIFY_VOICE_PATH_ERROR, monitor[i].transactionID, null);
										else
											createEvent(VpbConstants.VERIFY_VOICE_PATH_COMPLETE, monitor[i].transactionID, null);
									}
									monitor[i] = null;
								}
								break;
//							case TWO_WAY_VOICE_DETECTED :
//								if (monitor[i].type == VpbEventType.TWO_WAY_VOICE_DETECTED) {
//									if (monitor[i].expected)
//										createEvent(VpbConstants.VERIFY_VOICE_PATH_TWO_WAY_COMPLETE, monitor[i].transactionID, null);
//									else
//										createEvent(VpbConstants.VERIFY_VOICE_PATH_TWO_WAY_ERROR, monitor[i].transactionID, null);
//									monitor[i] = null;
//								}
//								break;
							}
						}
					}
				}
			}
			catch (Exception ex) {
				logger.error(PC2LogCategory.UTILITY, subcat,
						"VPB API encountered an error.", ex);
			}
		}
		for (int i=0; i< CHANNELS; i++)
			close_card(handles[i]);
	}
	
	
	public void stop() {
		this.isRunning = false;
	}
	public void start()	throws IOException	{
		try {
				this.isRunning = true;
				thread = new Thread(this, "VPB API");
				thread.setDaemon(true);
				thread.start();
			}
			catch (Exception ex) {
				logger.warn(PC2LogCategory.UTILITY, subcat,
						"StunStack encountered an error when starting the StunMessageProcessor.", ex);
			}
	}
	
	public void voicepath(int channel, int transID, boolean expected) {
		VpbEventType type = VpbEventType.VOICE_DETECTED;
		if (!canMonitor(channel, type)) {
			generateFailure(monitor[channel], channel);
		}
		int result = playFile(handles[channel], "../config/tones/test.wav");
		File f = new File("../config/tones/test.wav");
		if (f.exists() && f.isFile()) {
			if (result >= 0) {

				monitor[channel] = new Monitor(type, transID, expected);
				//System.out.println(
				logger.debug(PC2LogCategory.UTILITY, subcat, 
						"VPB API begin monitoring for one way voice traffic on channel[" + channel +"].");
			}
		}
		else {
			logger.error(PC2LogCategory.UTILITY, subcat, "VPB API can not test for two way voice traffice on channel[" 
					+ channel + "] because the file[ " + f.getAbsoluteFile() + "] doesn't exist.");
			//monitor[channel] = new Monitor(type, transID, expected);
			generateFailure(monitor[channel], channel);
		}
		
		
	}
	
	public void voicepath2way(int channel, int channel2, int transID, boolean expected) {
		VpbEventType type = VpbEventType.VOICE_DETECTED;
		twoWaySecondChannel[channel] = channel2;		
		if (!canMonitor(channel, type)) {
			generateFailure(monitor[channel], channel);
		}
		File f = new File("../config/tones/test.wav");
		if (f.exists() && f.isFile()) {
					
			int result = playFile(handles[channel], "../config/tones/test.wav");
			if (result >= 0) {
				monitor[channel] = new Monitor(type, transID, expected);

				//System.out.println(
				logger.debug(PC2LogCategory.UTILITY, subcat, 
						"VPB API begin monitoring for two way voice traffic on channel[" + channel +"].");
			}
		
		}
		else {
			logger.error(PC2LogCategory.UTILITY, subcat, "VPB API can not test for two way voice traffice on channel[" 
					+ channel + "] because the file[ " + f.getAbsoluteFile() + "] doesn't exist.");
			//monitor[channel] = new Monitor(type, transID, expected);
			generateFailure(monitor[channel], channel);
		}
	}
	
	public static void main(String[] args) throws Exception {
		long time = System.currentTimeMillis();
		System.out.println("time=" + time);

		EventProcessor ep = new EventProcessor();
		VpbAPI api = new VpbAPI(ep);
		LogAPI.setConsoleCreated();
		api.init();
		api.start();
		int transID=1;
		int channel = 0;
		int channel1 = 1;

		api.onhook(channel, ++transID);
		api.onhook(channel1, ++transID);
//		
//		api.offhook(channel, transID);
//		
//		Thread.sleep(1000);
//		
//		try {
//			api.dialtone(channel, transID, true);
//		
//		
////	Thread.sleep(4000);
////		api.ring(channel, ++transID, true);
//		
//		api.dial(channel, "3035552000", ++transID);
////		api.dial(channel, "3033514402", ++transID);
//		
////		Thread.sleep(7000);
//		
////		api.ring(channel1, ++transID, true);
////		api.ringback(channel, ++transID, true);
//		api.offhook(channel1, ++transID);
////		api.busy(channel, ++transID, true);
////		api.voicepath(channel, ++transID, true);
//		Thread.sleep(7000);
//		api.voicepath2way(channel, channel1, ++transID, true);
//		
////		api.callwaiting(channel, transID, true);
//		Thread.sleep(15000);
//		//api.reorder(channel, transID, true);
////		api.hookflash(channel, ++transID);
//		
//		//api.hookflash(channel, ++transID);
//		
////		Thread.sleep(5000);
//		
////		api.hookflash(channel, ++transID);
////		Thread.sleep(5000);
//		api.onhook(channel, ++transID);
//		
//		api.onhook(channel1, ++transID);
//		
//		api.reset();
//		
//		api.stop();
//		}
//		catch (Exception ex) {
//			handleException(ex);
//			api.onhook(channel, ++transID);
//			
//			api.onhook(channel1, ++transID);
//		api.reset();
//			
//			api.stop();
//		}
//		Thread.sleep(10000);
	}
	
	static public void handleException(Exception e)
	{
		System.out.println(e.toString());
		e.printStackTrace();
		
	}
}
