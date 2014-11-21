/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import javax.sip.header.*;
import java.text.ParseException;

/**
 * the Priority header. 
 *
 * @author Olivier Deruelle <deruelle@nist.gov><br/>
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class Priority extends SIPHeader implements PriorityHeader {

	private static final long serialVersionUID = 1L;
	
	/** constant EMERGENCY field
	*/
	public static final String EMERGENCY = ParameterNames.EMERGENCY;

	/** constant URGENT field
	 */
	public static final String URGENT = ParameterNames.URGENT;

	/** constant NORMAL field
	 */
	public static final String NORMAL = ParameterNames.NORMAL;

	/** constant NON_URGENT field
	 */
	public static final String NON_URGENT = ParameterNames.NON_URGENT;
	/** priority field
	 */
	protected String priority;

	/** Default constructor
	 */
	public Priority() {
		super(NAME);
	}

	/**
	 * Encode into canonical form.
	 * @return String
	 */
	public String encodeBody() {
		return priority;
	}

	/**
	 * get the priority value.
	 * @return String
	 */
	public String getPriority() {
		return priority;
	}

	/**
	 * Set the priority member
	 * @param p String to set
	 */
	public void setPriority(String p) throws ParseException {
		if (p == null)
			throw new NullPointerException(
				"JAIN-SIP Exception,"
					+ "Priority, setPriority(), the priority parameter is null");
		priority = p;
	}
}
/*
 * $Log: Priority.java,v $
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
