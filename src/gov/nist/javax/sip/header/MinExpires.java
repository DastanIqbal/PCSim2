/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import javax.sip.*;
import javax.sip.header.*;

/**  
 * MinExpires SIP Header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 * @author Olivier Deruelle <deruelle@nist.gov><br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class MinExpires extends SIPHeader implements MinExpiresHeader {

	private static final long serialVersionUID = 1L;
	
	/** expires field
	 */
	protected int expires;

	/** default constructor
	 */
	public MinExpires() {
		super(NAME);
	}

	/**
	 * Return canonical form.
	 * @return String
	 */
	public String encodeBody() {
		return new Integer(expires).toString();
	}

	/**
	 * Gets the expires value of the ExpiresHeader. This expires value is
	 * relative time.
	 *
	 * @return the expires value of the ExpiresHeader.
	 * @since JAIN SIP v1.1
	 */
	public int getExpires() {
		return expires;
	}

	/**
	 * Sets the relative expires value of the ExpiresHeader. 
	 * The expires value MUST be greater than zero and MUST be 
	 * less than 2**31.
	 *
	 * @param expires - the new expires value of this ExpiresHeader
	 *
	 * @throws InvalidArgumentException if supplied value is less than zero.
	 *
	 * @since JAIN SIP v1.1
	 *
	 */
	public void setExpires(int expires) throws InvalidArgumentException {
		if (expires < 0)
			throw new InvalidArgumentException("bad argument " + expires);
		this.expires = expires;
	}

}
/*
 * $Log: MinExpires.java,v $
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
