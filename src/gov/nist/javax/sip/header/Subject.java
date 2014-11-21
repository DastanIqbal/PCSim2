/*******************************************************************************

* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *

*******************************************************************************/

package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.*;

/**  
 * Supported SIP Header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 * @author Olivier Deruelle <deruelle@nist.gov><br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class Subject extends SIPHeader implements SubjectHeader {

	private static final long serialVersionUID = 1L;
	
	/** subject field
	 */
	protected String subject;

	/** Default Constructor.
	 */
	public Subject() {
		super(SUBJECT);
	}

	/**
	 * Generate the canonical form.
	 * @return String.
	 */
	public String encodeBody() {
		if (subject != null) {
			return subject;
		} else {
			return "";
		}
	}

	/**
	 * Sets the subject value of the SubjectHeader to the supplied string
	 * subject value.
	 *
	 * @param subject - the new subject value of this header
	 * @throws ParseException which signals that an error has been reached
	 * unexpectedly while parsing the subject value.
	 */
	public void setSubject(String subject) throws ParseException {
		if (subject == null)
			throw new NullPointerException(
				"JAIN-SIP Exception, "
					+ " Subject, setSubject(), the subject parameter is null");
		this.subject = subject;
	}

	/**
	 * Gets the subject value of SubjectHeader
	 *
	 * @return subject of SubjectHeader
	 */
	public String getSubject() {
		return subject;
	}

}
/*
 * $Log: Subject.java,v $
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
