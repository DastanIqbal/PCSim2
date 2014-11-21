/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import java.text.ParseException;

/**
 * the Unsupported header. 
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:30 $
 * @author Olivier Deruelle <deruelle@nist.gov><br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class Unsupported
	extends SIPHeader
	implements javax.sip.header.UnsupportedHeader {

	private static final long serialVersionUID = 1L;
	
	/** option-Tag field.
	 */
	protected String optionTag;

	/** Default Constructor.
	 */
	public Unsupported() {
		super(NAME);
	}

	/** Constructor
	 * @param ot String to set
	 */
	public Unsupported(String ot) {
		super(NAME);
		optionTag = ot;
	}

	/**
	 * Return a canonical value.
	 * @return String.
	 */
	public String encodeBody() {
		return optionTag;
	}

	/** get the option tag field
	 * @return option Tag field
	 */
	public String getOptionTag() {
		return optionTag;
	}

	/**
	 * Set the option member
	 * @param o String to set
	 */
	public void setOptionTag(String o) throws ParseException {
		if (o == null)
			throw new NullPointerException(
				"JAIN-SIP Exception, "
					+ " Unsupported, setOptionTag(), The option tag parameter is null");
		optionTag = o;
	}
}
/*
 * $Log: Unsupported.java,v $
 * Revision 1.2  2004/01/22 13:26:30  sverker
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
