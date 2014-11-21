/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD)         *
*******************************************************************************/
package gov.nist.javax.sip.message;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;

/**
 * Duplicate header exception:  thrown when there is more
 * than one header of a type where there should only be one.
 * The exception handler may choose to : 
 * 1. discard the duplicate  by returning null
 * 2. keep the duplicate by just returning it.
 * 3. Discard the entire message by throwing an exception.
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:31 $
 * @author M. Ranganathan mailto:mranga@nist.gov
 */
public class SIPDuplicateHeaderException extends ParseException {
	
	private static final long serialVersionUID = 1L;
	
	protected SIPHeader sipHeader;
	protected SIPMessage sipMessage;
	public SIPDuplicateHeaderException(String msg) {
		super(msg, 0);
	}
	public SIPMessage getSIPMessage() {
		return sipMessage;
	}

	public SIPHeader getSIPHeader() {
		return sipHeader;
	}

	public void setSIPHeader(SIPHeader sipHeader) {
		this.sipHeader = sipHeader;
	}

	public void setSIPMessage(SIPMessage sipMessage) {
		this.sipMessage = sipMessage;
	}
}
/*
 * $Log: SIPDuplicateHeaderException.java,v $
 * Revision 1.2  2004/01/22 13:26:31  sverker
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
