/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.fsm;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Timer;

import com.cablelabs.common.Transport;
import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;
import com.cablelabs.stun.StunStack;
import com.cablelabs.tools.RefLocator;

public class Stream implements Action, Runnable {

	/**
	 * The name of this stream.
	 */
	private String name = null;
	
	/**
	 * A flag indicating whether this is a start or a stop
	 * of a new file to stream.
	 */
	private boolean stop = false;
	
	/**
	 * The file to stream to the peer
	 */
	private File file = null;
	
	/**
	 * The message reference information to use for the originator's
	 * IP address for the stream.
	 */
	private Reference fromIP = null;
	
	/**
	 * The message reference information to use for the originator's
	 * IP address for the stream.
	 */
	private Reference fromPort = null;
	
	/**
	 * The message reference information to use for the destination's
	 * IP address for the stream. In other words the peers IP address.
	 */
	private Reference toIP = null;
	
	/**
	 * The message reference information to use for the destination
	 * IP port of the stream. In other words the peers IP port.
	 */
	private Reference toPort = null;
	/**
	 * The source IP address of the event when it is an external
	 * event.
	 */
	protected String srcIP = null;
	
	/**
	 * The source port of the event when it is an external event.
	 */
	protected int srcPort = 0;
	
	/**
	 * The destination IP address of the event when it is an external
	 * event.
	 */
	protected String destIP = null;
	
	/**
	 * The destination port of the event when it is an external event.
	 */
	protected int destPort = 0;

	/**
	 * The format to use when sending the stream.
	 */
	private StreamFormat format = null;
	
	/**
	 * A table of all of the known formats of the system.
	 * 
	 */
	private static Hashtable<String, StreamFormat> formats = 
		new Hashtable<String, StreamFormat>();
	
	/**
	 * The reference locator retrieves the IP address and port information
	 * at runtime.
	 */
	private static RefLocator refLocator = RefLocator.getInstance();
	
	/**
	 * The stack to use to transmit the reference information from.
	 */
	private StunStack stack = null;
	
	/**
	 * The processor ID of the socket to use for transmitting the stream.
	 */
	private int processorID = -1;
	
	/**
	 * New subcategory for logging. 
	 */
	private static String subCat = "Streams";
	
	/**
	 * The current thread the stream is running under.
	 */
	private Thread thread = null;
	
	/**
	 * A flag that is set to false to exit the stream threading operation.
	 */
	private boolean isRunning = false;
	
	/**
	 * The FSM that requested the streaming to occur.
	 * 
	 */
	private int fsmUID = -1;
	
	/**
	 * Accessor for logging to console and log files.
	 */
	private LogAPI logger = LogAPI.getInstance();
	
	/**
	 * The thread for the stream's scheduling timer.
	 */
	protected StreamTask streamTask = null;
	
	/**
	 * The stream's timer.
	 */
	protected Timer timer;
	
	/**
	 * A two dimensional array of media frames read from the file
	 */
	private byte [] [] dataFrames = null;
	
	/**
	 * Contains the number of frames read from the file
	 */
	private int frames = -1;
	
	/**
	 * Contains the last frame number sent
	 */
	private int count = -1;
	
	/**
	 * The socket address of the peer entity that the media frames are being 
	 * sent to.
	 */
	private InetSocketAddress remoteAddress = null;
	
	/**
	 * A reference to the queue that we will place a copy of each media frame
	 * sent to the peer device. 
	 */
	private MsgQueue q = MsgQueue.getInstance();
	
	static {
		StreamFormat sf = new StreamFormat("MPEG2", 1360, 160);
		formats.put(sf.getFormat(), sf);
		sf = new StreamFormat("G711", 492, 60);
		formats.put(sf.getFormat(), sf);
		sf = new StreamFormat("G711-20", 172, 20);
		formats.put(sf.getFormat(), sf);
	}
	
	public Stream(String name, File f, String formatType, boolean stop) throws IllegalArgumentException {
		this.name = name;
		this.stop = stop;
		if (!this.stop) {
			if (f.exists() && f.canRead()) {
				this.file = f;
			}
			else {
				if (!f.exists())
					throw new IllegalArgumentException("The file(" + f 
							+ ") can't be read by the platform.");
				else if (!f.canRead())
					throw new IllegalArgumentException("The file(" + f 
							+ ") doesn't exist.");
			}
			StreamFormat sf = formats.get(formatType);
			if (sf != null)
				this.format = sf;
			else {
				throw new IllegalArgumentException("The format(" + formatType 
						+ ") is not supported by the platform. " + getSupportedFormats());
			}
		}
	}
	
	public String getName() {
		return name;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public StreamFormat getFormat() {
		return format;
	}

	private String getSupportedFormats() {
		String msg = " Stream formats supported by platform: ";
		boolean first = true;
		Enumeration<String> keys = formats.keys();
		while (keys.hasMoreElements()) {
			if (first)
				msg += keys.nextElement();
			else
				msg += ", " + keys.nextElement();
		}
		throw new IllegalArgumentException(msg);
	}
	public void setFormat(String format) throws IllegalArgumentException {
		StreamFormat sf = formats.get(format);
		if (sf != null)
			this.format = sf;
		else {
			String msg = "The format type(" + format 
			+ ") is not supported by the platform. " + getSupportedFormats();
			throw new IllegalArgumentException(msg);
		}
	}

	public Reference getFromIP() {
		return fromIP;
	}

	public void setFromIP(Reference from) {
		this.fromIP = from;
	}
	
	public Reference getFromPort() {
		return fromPort;
	}

	public void setFromPort(Reference from) {
		this.fromPort = from;
	}

	public boolean isStop() {
		return stop;
	}

	public void setStop(boolean stop) {
		this.stop = stop;
	}

	public Reference getToIP() {
		return toIP;
	}

	public void setToIP(Reference to) {
		this.toIP = to;
	}

	public Reference getToPort() {
		return toPort;
	}

	public void setToPort(Reference to) {
		this.toPort = to;
	}
	
	public StunStack getStack() {
		return stack;
	}

	public void setStack(StunStack stack) {
		this.stack = stack;
	}
	
	public int getProcessorID() {
		return processorID;
	}

	public void setProcessorID(int id) {
		this.processorID = id;
	}
	
	public String getSrcIP() {
		return this.srcIP;
	}
	
	public String getDestIP() {
		return this.destIP;
	}
	
	public int getSrcPort() {
		return this.srcPort;
	}
	
	public int getDestPort() {
		return this.destPort;
	}
	
	public Thread getThread() {
		return this.thread;
	}
	
	/**
	 * Common operation to perform the action described by the
	 * derived class.
	 */
	@Override
	public void execute(FSMAPI api, int msgQueueIndex) throws PC2Exception {
		api.stream(this, msgQueueIndex);
	}
	
	/**
	 * This method obtains all of the IP address and port information for the
	 * to and from references.
	 * 
	 * @throws PC2Exception
	 */
	public void resolve(int fsm) throws PC2Exception {
		String refValue = refLocator.getReferenceInfo(fsm, toIP, null);
		if (refValue != null)
			destIP = refValue;
		else {
			throw new PC2Exception("Stream action class couldn't resolve toIP=" + toIP);
		}
		refValue = refLocator.getReferenceInfo(fsm, toPort, null);
		if (refValue != null) {
			try {
				int port = Integer.parseInt(refValue);
				destPort = port;
			}
			catch (NumberFormatException nfe) {
				throw new PC2Exception("Stream action couldn't convert the reference value(" 
						+ refValue + " to an integer for the toPort.");
			}
		}
		else {
			throw new PC2Exception("Stream action class couldn't resolve toPort=" + toPort);
		}
		refValue = refLocator.getReferenceInfo(fsm, fromIP, null);
		if (refValue != null)
			srcIP = refValue;
		else {
			throw new PC2Exception("Stream action class couldn't resolve fromIP=" + fromIP);
		}
		refValue = refLocator.getReferenceInfo(fsm, fromPort, null);
		if (refValue != null) {
			try {
				int port = Integer.parseInt(refValue);
				srcPort = port;
			}
			catch (NumberFormatException nfe) {
				throw new PC2Exception("Stream action couldn't convert the reference value(" 
						+ refValue + " to an integer for the fromPort.");
			}
		}
		else {
			throw new PC2Exception("Stream action class couldn't resolve fromPort=" + fromPort);
		}
		this.fsmUID = fsm;
	}
	
	/**
	 * Start the network listening thread.
	 *
	 * @throws IOException if we fail to setup the socket.
	 */
	public void start()	throws IOException	{
		this.isRunning = true;
		String threadName = "Stream " + format.getFormat() + " to=" + destIP + "|" + destPort;
		thread = new Thread(this, threadName);
		thread.setDaemon(true);
		thread.start();	
	}
	
	@Override
	public void run() {
		if (processorID != -1 && stack != null) {
			if (file.exists() && file.canRead()) {
				if (this.isRunning) {
					try {
						int size = format.getSize();
						long sleepTime = format.getInterval();
						FileInputStream in = new FileInputStream(file);
						int offset = 0;
						// to improve performance read all of the data from the file 
						long fileSize = file.length();
						frames = (int)fileSize/size;
						dataFrames = new byte[frames][size];
						for (int i=0; i <frames; i++) {
							int l = in.read(dataFrames[i], 0, size);
							offset += l;
							if (l <= 0) {
								logger.warn(PC2LogCategory.SIP, subCat, "Stream could not read in all of the frames from " + file + ".");
							}
						}
						
						// Sanity check 
						if (fileSize > offset)
							logger.warn(PC2LogCategory.SIP, subCat, "The stream did not read all of the bytes from the file " 
									+ "before sending. File contains " + fileSize + ", but only read" + " offset bytes.");
						
						// Now that we have all of the file read and the number of frames to send lets
						// setup the timer
						remoteAddress = new InetSocketAddress(destIP, destPort);
						if (remoteAddress != null) {
							timer = new Timer(name + ":streamTimer", true);
							streamTask = new StreamTask(this);
							timer.scheduleAtFixedRate(streamTask, 0, sleepTime);
						} 
						else {
							logger.warn(PC2LogCategory.SIP, subCat, "Unable to create an InetSocketAddress for " 
									+ destIP + "|" + destPort + ".");
						}
					}
					catch (Exception e) {
						logger.warn(PC2LogCategory.SIP, subCat,
								"While preparing to stream the file(" 
								+ file + ") the systeme encountered an exception.\n" 
								+ e.getMessage() + " " + e.getStackTrace());
						this.isRunning = false;
					}
				}
				logger.info(PC2LogCategory.SIP, subCat,
						"Streaming to " + destIP + "|" + destPort + " is complete.");
				
			}
			else {
				if (file.exists())
					throw new IllegalArgumentException("The file(" + file 
							+ ") can't be read by the platform.");
				else if (file.canRead())
					throw new IllegalArgumentException("The file(" + file 
							+ ") doesn't exist.");
			}
			
		}
		else {
			this.isRunning = false;
			logger.warn(PC2LogCategory.SIP, subCat,
					"The file(" + file + ") could not be streamed because the stack=" 
					+ stack + " and processorID=" + processorID);
		}
		
	}
	
	public synchronized void stop() {
		this.isRunning = false;
	}
	
	public void timerExpired() {
		if (isRunning && remoteAddress != null && count < frames) {
			count++;
			int l = dataFrames[count].length;
			logger.info(PC2LogCategory.SIP, subCat, "RTP writing " + l + " bytes from IP=" + srcIP + "|" + srcPort + " to IP=" + destIP + "|" + destPort);
			int seq = LogAPI.getSequencer();
			stack.sendRawMessage(processorID, dataFrames[count], l, seq, remoteAddress, subCat);
			RTPMsg event = new RTPMsg (fsmUID, System.currentTimeMillis(), seq,
					Transport.UDP, destIP, destPort, srcIP, srcPort, format.getFormat(), null, dataFrames[count]);
			if (event != null)
				q.add(event);
//			logger.debug(PC2LogCategory.SIP, subCat, "read " + l + " bytes.");
		}
	
		if (!isRunning || count == frames) {
			streamTask.cancel();
			timer = null;
		}
		
		
	}
	
	@Override
	public String toString() {
		String result = "\t";
		if (stop)
			result += "stop_stream name=" + name;
		else
			result += "start_stream name=" + name + " file=" + file;
		result += "\n\t\ttoIP=" + toIP + " toPort=" + toPort 
			+ "\n\t\tfromIP=" + fromIP + " fromPort=" + fromPort
			+ "format=" + format;
		return result;
	}
	
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		Stream retval = (Stream)super.clone();
		if (retval != null) {
			retval.stop = this.stop;
			if (this.name != null)
				retval.name = new String(this.name);
			if (this.file != null) 
				retval.file = new File(this.file.getAbsolutePath());
			if (this.toIP != null)
				retval.toIP = (MsgRef)this.toIP.clone();
			if (this.fromIP != null)
				retval.fromIP = (MsgRef)this.fromIP.clone();
			if (this.toPort != null)
				retval.toPort = (MsgRef)this.toPort.clone();
			if (this.fromPort != null)
				retval.fromPort = (MsgRef)this.fromPort.clone();
			if (this.srcIP != null)
				retval.srcIP = new String(this.srcIP);
			if (this.destIP != null)
				retval.destIP = new String(this.destIP);
			retval.destPort = this.destPort;
			retval.srcPort = this.srcPort;
			// stack and processorID should not be cloned. 
			// Make the system assign them for each instance independently.
		}	
		
		return retval;
	}
}
