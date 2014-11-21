/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import java.text.ParseException;

/**
 * Allow SIPHeader.
 *
 * @author M. Ranganathan <mranga@nist.gov> NIST/ITL ANTD. <br/>
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class Allow extends SIPHeader implements javax.sip.header.AllowHeader {

	private static final long serialVersionUID = 1L;
	
	/** method field
	 */
	protected String method;

	/** default constructor
	 */
	public Allow() {
		super(ALLOW);
	}

	/** constructor
	 * @param m String to set
	 */
	public Allow(String m) {
		super(ALLOW);
		method = m;
	}

	/** get the method field
	 * @return String
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * Set the method member
	 * @param method method to set.
	 */
	public void setMethod(String method) throws ParseException {
		if (method == null)
			throw new NullPointerException(
				"JAIN-SIP Exception"
					+ ", Allow, setMethod(), the method parameter is null.");
		this.method = method;
	}

	/** Return body encoded in canonical form.
	 * @return body encoded as a string.
	 */
	protected String encodeBody() {
		return method;
	}
}
/*
 * $Log: Allow.java,v $
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
