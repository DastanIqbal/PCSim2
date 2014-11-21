/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

/**
 *  Protocol name and version.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 *<a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class Protocol extends SIPObject {

	private static final long serialVersionUID = 1L;
	
	/** protocolName field
	 */
	protected String protocolName;

	/** protocolVersion field
	 */
	protected String protocolVersion;

	/** transport field
	 */
	protected String transport;

	/**
	 * Compare two To headers for equality.
	 * @return true if the two headers are the same.
	 * @param other Object to set
	 */
	public boolean equals(Object other) {
		if (!other.getClass().equals(this.getClass())) {
			return false;
		}
		Protocol that = (Protocol) other;
		if (this.protocolName.compareToIgnoreCase(that.protocolName) != 0) {
			return false;
		}
		if (this.transport.compareToIgnoreCase(that.transport) != 0) {
			return false;
		}
		if (this.protocolVersion.compareTo(that.protocolVersion) != 0) {
			return false;
		}
		return true;
	}

	/**
	 * Return canonical form.
	 * @return String
	 */
	public String encode() {
		return protocolName.toUpperCase()
			+ SLASH
			+ protocolVersion
			+ SLASH
			+ transport.toUpperCase();
	}

	/** get the protocol name
	 * @return String
	 */
	public String getProtocolName() {
		return protocolName;
	}

	/** get the protocol version
	 * @return String
	 */
	public String getProtocolVersion() {
		return protocolVersion;
	}

	/** get the transport
	 * @return String
	 */
	public String getTransport() {
		return transport;
	}

	/**
	     * Set the protocolName member
	     * @param p String to set
	     */
	public void setProtocolName(String p) {
		protocolName = p;
	}

	/**
	     * Set the protocolVersion member
	     * @param p String to set
	     */
	public void setProtocolVersion(String p) {
		protocolVersion = p;
	}

	/**
	     * Set the transport member
	     * @param t String to set
	     */
	public void setTransport(String t) {
		transport = t;
	}

	/** 
	* Default constructor.
	*/
	public Protocol() {
		protocolName = "SIP";
		protocolVersion = "2.0";
		transport = "UDP";
	}
}
/*
 * $Log: Protocol.java,v $
 * Revision 1.2  2004/01/22 13:26:29  sverker
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
