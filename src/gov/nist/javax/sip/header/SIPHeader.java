/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

/**
 * Root class from which all SIPHeader objects are subclassed.
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public abstract class SIPHeader
	extends SIPObject
	implements SIPHeaderNames, javax.sip.header.Header {

	private static final long serialVersionUID = 1L;
	
	/** name of this header
	 */
	protected String headerName;

	/** Value of the header.
	*/

	/** Constructor
	 * @param hname String to set
	 */
	protected SIPHeader(String hname) {
		headerName = hname;
	}

	/** Default constructor
	 */
	public SIPHeader() {
	}

	/**
	 * Name of the SIPHeader
	 * @return String
	 */
	public String getHeaderName() {
		return headerName;
	}

	/** Alias for getHaderName above.
	*
	*@return String headerName
	*
	*/
	public String getName() {
		return this.headerName;
	}

	/**
	     * Set the name of the header .
	     * @param hdrname String to set
	     */
	public void setHeaderName(String hdrname) {
		headerName = hdrname;
	}

	/** Get the header value (i.e. what follows the name:).
	* This merely goes through and lops off the portion that follows
	* the headerName:
	*/
	public String getHeaderValue() {
		String encodedHdr = null;
		try {
			encodedHdr = this.encode();
		} catch (Exception ex) {
			return null;
		}
		StringBuffer buffer = new StringBuffer(encodedHdr);
		while (buffer.length() > 0 && buffer.charAt(0) != ':') {
			buffer.deleteCharAt(0);
		}
		if (buffer.length() > 0)
			buffer.deleteCharAt(0);
		return buffer.toString().trim();
	}

	/** Return false if this is not a header list 
	* (SIPHeaderList overrrides this method).
	*@return false
	*/
	public boolean isHeaderList() {
		return false;
	}

	/** Encode this header into canonical form.
	*/
	public String encode() {
		return this.headerName + COLON + SP + this.encodeBody() + NEWLINE;
	}

	/** Encode the body of this header (the stuff that follows headerName).
	* A.K.A headerValue.
	*/
	protected abstract String encodeBody();

	/** Alias for getHeaderValue.
	 */
	public String getValue() {
		return this.getHeaderValue();
	}
}
/*
 * $Log: SIPHeader.java,v $
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
