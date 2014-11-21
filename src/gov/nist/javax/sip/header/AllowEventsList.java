/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import java.util.*;
import java.text.ParseException;
import javax.sip.header.*;

/**
 * List of AllowEvents headers. 
 * The sip message can have multiple AllowEvents headers which are strung
 * together in a list.
 *
 * @author M. Ranganathan <mranga@nist.gov> <br/>
 * @author Olivier Deruelle <deruelle@nist.gov><br/>
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 */
public class AllowEventsList extends SIPHeaderList {

	private static final long serialVersionUID = 1L;
	
	/** default constructor
	 */
	public AllowEventsList() {
		super(AllowEvents.class, AllowEventsHeader.NAME);
	}

	/**
	 * Gets an Iterator of all the methods of the AllowEventsHeader. Returns an empty
	 *
	 * Iterator if no methods are defined in this AllowEvents Header.
	 *
	 *
	 *
	 * @return Iterator of String objects each identifing the methods of
	 *
	 * AllowEventsHeader.
	 *
	 * @since JAIN SIP v1.1
	 *
	 */
	public ListIterator<String> getMethods() {
		ListIterator<?> li = super.hlist.listIterator();
		LinkedList<String> ll = new LinkedList<String>();
		while (li.hasNext()) {
			AllowEvents allowEvents = (AllowEvents) li.next();
			ll.add(allowEvents.getEventType());
		}
		return ll.listIterator();
	}

	/**
	 * Sets the methods supported defined by this AllowEventsHeader.
	 *
	 *
	 *
	 * @param methods - the Iterator of Strings defining the methods supported
	 *
	 * in this AllowEventsHeader
	 *
	 * @throws ParseException which signals that an error has been reached
	 *
	 * unexpectedly while parsing the Strings defining the methods supported.
	 *
	 * @since JAIN SIP v1.1
	 *
	 */
	public void setMethods(List<String> methods) throws ParseException {
		ListIterator<String> it = methods.listIterator();
		while (it.hasNext()) {
			AllowEvents allowEvents = new AllowEvents();
			allowEvents.setEventType((String) it.next());
			this.add(allowEvents);
		}
	}
}
/*
 * $Log: AllowEventsList.java,v $
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
