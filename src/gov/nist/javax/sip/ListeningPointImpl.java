package gov.nist.javax.sip;

import javax.sip.*;
import gov.nist.javax.sip.stack.*;

/**
 * Implementation of the ListeningPoint interface
 *
 * @version JAIN-SIP-1.1 $Revision: 1.6 $ $Date: 2004/01/22 13:26:28 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class ListeningPointImpl implements javax.sip.ListeningPoint {

	private static final long serialVersionUID = 1L;
	
	protected String host;

	protected String transport;

	/** My port. (same thing as in the message processor) */

	int port;

	/**
	 * Pointer to the imbedded mesage processor.
	 */
	protected MessageProcessor messageProcessor;

	/**
	 * Provider back pointer
	 */
	protected SipProviderImpl sipProviderImpl;

	/**
	 * Our stack
	 */
	protected SipStackImpl sipStack;

	/**
	 * Construct a key to refer to this structure from the SIP stack
	 * @param host host string
	 * @param port port
	 * @param transport transport
	 * @return a string that is used as a key
	 */
	public static String makeKey(String host, int port, String transport) {
		return new StringBuffer(host)
			.append(":")
			.append(port)
			.append("/")
			.append(transport)
			.toString()
			.toLowerCase();
	}

	/**
	 * Get the key for this strucut
	 * @return  get the host
	 */
	protected String getKey() {
		return makeKey(host, port, transport);
	}

	/**
	 * Set the sip provider for this structure.
	 * @param sipProviderImpl provider to set
	 */
	protected void setSipProvider(SipProviderImpl sipProviderImpl) {
		this.sipProviderImpl = sipProviderImpl;
	}

	/**
	 * Remove the sip provider from this listening point.
	 */
	protected void removeSipProvider() {
		this.sipProviderImpl = null;
	}

	/**
	 * Constructor
	 * @param sipStack Our sip stack
	 */
	protected ListeningPointImpl(
		SipStack sipStack,
		int port,
		String transport) {
		this.sipStack = (SipStackImpl) sipStack;
		this.host = sipStack.getIPAddress();
		this.port = port;
		this.transport = transport;
	}

	/**
	 * Clone this listening point. Note that a message Processor is not
	 * started. The transport is set to null.
	 * @return cloned listening point.
	 */
	public Object clone() {
		ListeningPointImpl lip =
			new ListeningPointImpl(this.sipStack, this.port, null);
		lip.sipStack = this.sipStack;
		return lip;
	}

	/** Gets host name of this ListeningPoint
	 *
	 * @return host of ListeningPoint
	 */
	public String getHost() {
		return this.sipStack.getHostAddress();
	}

	/**
	 * Gets the port of the ListeningPoint. The default port of a ListeningPoint
	 * is dependent on the scheme and transport.  For example:
	 * <ul>
	 * <li>The default port is 5060 if the transport UDP the scheme is <i>sip:</i>.
	 * <li>The default port is 5060 if the transport is TCP the scheme is <i>sip:</i>.
	 * <li>The default port is 5060 if the transport is SCTP the scheme is <i>sip:</i>.
	 * <li>The default port is 5061 if the transport is TLS over TCP the scheme is <i>sip:</i>.
	 * <li>The default port is 5061 if the transport is TCP the scheme is <i>sips:</i>.
	 * </ul>
	 *
	 * @return port of ListeningPoint
	 */
	public int getPort() {
		return messageProcessor.getPort();
	}

	/**
	 * Gets transport of the ListeningPoint.
	 *
	 * @return transport of ListeningPoint
	 */
	public String getTransport() {
		return messageProcessor.getTransport();
	}

	/**
	 * Get the provider.
	 *
	 * @return the provider.
	 */
	public SipProviderImpl getProvider() {
		return this.sipProviderImpl;
	}
}
/*
 * $Log: ListeningPointImpl.java,v $
 * Revision 1.6  2004/01/22 13:26:28  sverker
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
