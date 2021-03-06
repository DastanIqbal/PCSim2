/******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).      *
 ******************************************************************************/
package gov.nist.javax.sip.stack;
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.net.SocketException;
import gov.nist.core.*;
import java.net.*;
import java.util.*;

/**
 * Sit in a loop waiting for incoming tcp connections and start a
 * new thread to handle each new connection. This is the active
 * object that creates new TCP MessageChannels (one for each new
 * accept socket).  
 *
 * @version  JAIN-SIP-1.1 $Revision: 1.22 $ $Date: 2004/12/01 19:05:16 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 * Acknowledgement: Jeff Keyser suggested that a
 * Stop mechanism be added to this. Niklas Uhrberg suggested that
 * a means to limit the number of simultaneous active connections
 * should be added. Mike Andrews suggested that the thread be
 * accessible so as to implement clean stop using Thread.join().
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class TCPMessageProcessor extends MessageProcessor {

	protected Thread thread;


	protected int port;

	protected int nConnections;

	private boolean isRunning;


	private Hashtable<String, TCPMessageChannel> tcpMessageChannels;

	private ServerSocket sock;

	protected int useCount;

	/**
	 * The SIP Stack Structure.
	 */
	protected SIPMessageStack sipStack;

	/**
	 * Constructor.
	 * @param sipStack SIPStack structure.
	 * @param port port where this message processor listens.
	 */
	protected TCPMessageProcessor(SIPMessageStack sipStack, int port) {
		this.sipStack = sipStack;
		this.port = port;
		this.tcpMessageChannels = new Hashtable<String, TCPMessageChannel>();
	}

	/**
	 * Start the processor.
	 */
	public void start() throws IOException {
		thread = new Thread(this);
		// PC 2.0 thread.setName("TCPMessageProcessor");
		thread.setDaemon(true);
		this.sock = sipStack.getNetworkLayer().createServerSocket(this.port, 0, sipStack.savedStackInetAddress);
		this.isRunning = true;
		// PC 2.0 changed the thread name to something more meaningful.
		thread.setName("SIP - TCP" + sock.getInetAddress().toString() + ":" + sock.getLocalPort());
		thread.start();

	}

	
	/**
	* Return our thread.
	*
	*@return -- our thread. This is used for joining 
	*/
	public Thread getThread() {
		return this.thread;
	}

	/**
	 * Run method for the thread that gets created for each accept
	 * socket.
	 */
	public void run() {
		// Accept new connectins on our socket.
		while (this.isRunning) {
			try {
				synchronized (this) {
					// sipStack.maxConnections == -1 means we are
					// willing to handle an "infinite" number of
					// simultaneous connections (no resource limitation).
					// This is the default behavior.
					while (this.isRunning
						&& sipStack.maxConnections != -1
						&& this.nConnections >= sipStack.maxConnections) {
						try {
							this.wait();

							if (!this.isRunning)
								return;
						} catch (InterruptedException ex) {
							break;
						}
					}
					this.nConnections++;
				}

				Socket newsock = sock.accept();
				if (LogWriter.needsLogging) {
					getSIPStack().logWriter.logMessage(
						"Accepting new connection!");
				}
				// Note that for an incoming message channel, the
				// thread is already running
				TCPMessageChannel tcpMessageChannel =
					new TCPMessageChannel(newsock, sipStack, this);
				if (tcpMessageChannel != null)
					tcpMessageChannels.put(tcpMessageChannel.getKey(), 
							tcpMessageChannel);
			} catch (SocketException ex) {
				this.isRunning = false;
			} catch (IOException ex) {
				// Problem accepting connection.
				if (LogWriter.needsLogging)
					getSIPStack().logWriter.logException(ex);
				continue;
			} catch (Exception ex) {
				InternalErrorHandler.handleException(ex);
			}
		}
	}

	/**
	 * Return the transport string.
	 * @return the transport string
	 */
	public String getTransport() {
		return "tcp";
	}

	/**
	 * Returns the port that we are listening on.
	 * @return Port address for the tcp accept.
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * Returns the stack.
	 * @return my sip stack.
	 */
	public SIPMessageStack getSIPStack() {
		return sipStack;
	}

	/**
	 * Stop the message processor.
	 * Feature suggested by Jeff Keyser.
	 */
	public synchronized void stop() {
		isRunning = false;
		this.listeningPoint = null;
		try {
			sock.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Collection<TCPMessageChannel> en = tcpMessageChannels.values();
		for ( Iterator<TCPMessageChannel> it = en.iterator(); it.hasNext(); ) {
		       TCPMessageChannel next = 
				(TCPMessageChannel)it.next() ;
			next.close();
		}
		this.notify();
	}


	protected synchronized void remove
		(TCPMessageChannel tcpMessageChannel) {

		String key = tcpMessageChannel.getKey();
		if (LogWriter.needsLogging) {
		   sipStack.logWriter.logMessage	
		   ( Thread.currentThread() + " removing " + key);
		}

		/** May have been removed already */
		if (tcpMessageChannels.get(key) == tcpMessageChannel) 
			this.tcpMessageChannels.remove(key);
	}



	public synchronized  
		MessageChannel createMessageChannel(HostPort targetHostPort)
		throws IOException {
		String key = MessageChannel.getKey(targetHostPort,"TCP");
		if (tcpMessageChannels.get(key) != null)  {
			return (TCPMessageChannel) 
			this.tcpMessageChannels.get(key);
		} else {
		     TCPMessageChannel retval = new TCPMessageChannel(
			targetHostPort.getInetAddress(),
			targetHostPort.getPort(),
			sipStack,
			this);
		     this.tcpMessageChannels.put(key,retval);
		     retval.isCached = true;
		     if (LogWriter.needsLogging ) {
			  sipStack.logWriter.logMessage
				("key " + key);
		          sipStack.logWriter.logMessage("Creating " + retval);
		      }
		     return retval;
		}
	}


	protected synchronized  void cacheMessageChannel 
		(TCPMessageChannel messageChannel) {
		String key = messageChannel.getKey();
		TCPMessageChannel currentChannel = 
			(TCPMessageChannel) tcpMessageChannels.get(key);
		// PC 2.0 - Make sure that they are not the same object.
		if (currentChannel != null && messageChannel != currentChannel)  {
		        if (LogWriter.needsLogging) 
				sipStack.logWriter.logMessage("Closing " + key);
			currentChannel.close();
		}
		if (LogWriter.needsLogging) 
			sipStack.logWriter.logMessage("Caching " + key);
	        this.tcpMessageChannels.put(key,messageChannel);

	}

	public  synchronized MessageChannel 
	       createMessageChannel(InetAddress host, int port)
		throws IOException {
		try {
		   String key = MessageChannel.getKey(host,port,"TCP");
		   if (tcpMessageChannels.get(key) != null)  {
			return (TCPMessageChannel) 
				this.tcpMessageChannels.get(key);
		   } else {
		        TCPMessageChannel retval  = new TCPMessageChannel(host, port, sipStack, this);
			this.tcpMessageChannels.put(key,retval);
		        retval.isCached = true;
			if (LogWriter.needsLogging) {
		        	sipStack.logMessage("key " + key);
		        	sipStack.logMessage("Creating " + retval);
			}
			return retval;
		   }
		} catch (UnknownHostException ex) {
			throw new IOException (ex.getMessage());
		}
	}



	/**
	 * TCP can handle an unlimited number of bytes.
	 */
	public int getMaximumMessageSize() {
		return Integer.MAX_VALUE;
	}

	/**
	 * TCP NAPTR service name.
	 */
	public String getNAPTRService() {
		return "SIP+D2T";
	}

	/**
	 * TCP SRV prefix.
	 */
	public String getSRVPrefix() {
		return "_sip._tcp.";
	}

	public boolean inUse() {
		return this.useCount != 0;
	}

	/**
	 * Default target port for TCP
	 */
	public int getDefaultTargetPort() {
		return 5060;
	}

	/**
	 * TCP is not a secure protocol.
	 */
	public boolean isSecure() {
		return false;
	}
}
/*
 * $Log: TCPMessageProcessor.java,v $
 * Revision 1.22  2004/12/01 19:05:16  mranga
 * Reviewed by:   mranga
 * Code cleanup remove the unused SIMULATION code to reduce the clutter.
 * Fix bug in Dialog state machine.
 *
 * Revision 1.21  2004/09/04 14:59:54  mranga
 * Reviewed by:   mranga
 *
 * Added a method to expose the Thread for the message processors so that
 * stack.stop() can join to wait for the threads to die rather than sleep().
 * Feature requested by Mike Andrews.
 *
 * Revision 1.20  2004/08/30 16:04:47  mranga
 * Submitted by:  Mike Andrews
 * Reviewed by:   mranga
 *
 * Added a network layer.
 *
 * Revision 1.19  2004/08/23 23:56:21  mranga
 * Reviewed by:   mranga
 * forgot to set isDaemon in one or two places where threads were being
 * created and cleaned up some minor junk.
 *
 * Revision 1.18  2004/06/21 04:59:53  mranga
 * Refactored code - no functional changes.
 *
 * Revision 1.17  2004/05/14 20:20:03  mranga
 *
 * Submitted by:  Dave Stuart
 * Reviewed by:  mranga
 *
 * Stun support hacks -- use the original address specified to bind tcp transport
 * socket.
 *
 * Revision 1.16  2004/03/30 16:40:30  mranga
 * Reviewed by:   mranga
 * more tweaks to reference counting for cleanup.
 *
 * Revision 1.15  2004/03/30 15:38:18  mranga
 * Reviewed by:   mranga
 * Name the threads so as to facilitate debugging.
 *
 * Revision 1.14  2004/03/25 19:01:44  mranga
 * Reviewed by:   mranga
 * check for key before removing it from cache
 *
 * Revision 1.13  2004/03/25 18:08:15  mranga
 * Reviewed by:   mranga
 * Fix connection caching for ill behaved clients which connect multiple times
 * for the same incoming request.
 *
 * Revision 1.12  2004/03/25 15:15:05  mranga
 * Reviewed by:   mranga
 * option to log message content added.
 *
 * Revision 1.11  2004/03/19 23:41:30  mranga
 * Reviewed by:   mranga
 * Fixed connection and thread caching.
 *
 * Revision 1.10  2004/03/19 17:06:20  mranga
 * Reviewed by:   mranga
 * Fixed some stack cleanup issues. Stack should release all resources when
 * finalized.
 *
 * Revision 1.9  2004/01/22 18:39:42  mranga
 * Reviewed by:   M. Ranganathan
 * Moved the ifdef SIMULATION and associated tags to the first column so Prep preprocessor can deal with them.
 *
 * Revision 1.8  2004/01/22 14:23:45  mranga
 * Reviewed by:   mranga
 * Fixed some minor formatting issues.
 *
 * Revision 1.7  2004/01/22 13:26:33  sverker
 * Issue number:
 * Obtained from:
 * Submitted by:  sverker
 * Reviewed by:   mranga
 *
 * Major reformat of code to conform with style guide. Resolved compiler and javadoc warnings. Added CVS tags.
 *
 * CVS: ----------------------------------------------------------------------
 * CVS: Issue number:
 * CVS:   If this change addresses one or more issues,
 * CVS:   then enter the issue number(s) here.
 * CVS: Obtained from:
 * CVS:   If this change has been taken from another system,
 * CVS:   then name the system in this line, otherwise delete it.
 * CVS: Submitted by:
 * CVS:   If this code has been contributed to the project by someone else; i.e.,
 * CVS:   they sent us a patch or a set of diffs, then include their name/email
 * CVS:   address here. If this is your work then delete this line.
 * CVS: Reviewed by:
 * CVS:   If we are doing pre-commit code reviews and someone else has
 * CVS:   reviewed your changes, include their name(s) here.
 * CVS:   If you have not had it reviewed then delete this line.
 *
 */
