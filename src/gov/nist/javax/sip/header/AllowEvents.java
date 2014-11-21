/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import java.text.ParseException;

/**
 * AllowEvents SIPHeader.
 *
 * @author M. Ranganathan <mranga@nist.gov> NIST/ITL ANTD. <br/>
 * @author Olivier Deruelle <deruelle@nist.gov><br/>
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 */
public class AllowEvents
	extends SIPHeader
	implements javax.sip.header.AllowEventsHeader {

	private static final long serialVersionUID = 1L;
	
	/** method field
	 */
	protected String eventType;

	/** default constructor
	 */
	public AllowEvents() {
		super(ALLOW_EVENTS);
	}

	/** constructor
	 * @param m String to set
	 */
	public AllowEvents(String m) {
		super(ALLOW_EVENTS);
		eventType = m;
	}

	/**
	 * Sets the eventType defined in this AllowEventsHeader.
	 *
	 * @param eventType - the String defining the method supported
	 * in this AllowEventsHeader
	 * @throws ParseException which signals that an error has been reached
	 * unexpectedly while parsing the Strings defining the eventType supported
	 */
	public void setEventType(String eventType) throws ParseException {
		if (eventType == null)
			throw new NullPointerException(
				"JAIN-SIP Exception,"
					+ "AllowEvents, setEventType(), the eventType parameter is null");
		this.eventType = eventType;
	}

	/**
	 * Gets the eventType of the AllowEventsHeader. 
	 *
	 * @return the String object identifing the eventTypes of AllowEventsHeader.
	 */
	public String getEventType() {
		return eventType;
	}

	/** Return body encoded in canonical form.
	    * @return body encoded as a string.
	    */
	protected String encodeBody() {
		return eventType;
	}
}
/*
 * $Log: AllowEvents.java,v $
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
