/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import java.util.*;
import java.text.ParseException;
import javax.sip.header.*;

/**
 * List of ALLOW headers. The sip message can have multiple Allow headers
 *
 * @author M. Ranganathan <mranga@nist.gov>  NIST/ITL/ANTD <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 */
public class AllowList extends SIPHeaderList {

	private static final long serialVersionUID = 1L;
	
	/** default constructor
	 */
	public AllowList() {
		super(Allow.class, AllowHeader.NAME);
	}

	/**
	 * Gets an Iterator of all the methods of the AllowHeader. Returns an empty
	 *
	 * Iterator if no methods are defined in this Allow Header.
	 *
	 *
	 *
	 * @return Iterator of String objects each identifing the methods of
	 *
	 * AllowHeader.
	 *
	 * @since JAIN SIP v1.1
	 *
	 */
	public ListIterator<String> getMethods() {
		ListIterator<?> li = super.hlist.listIterator();
		LinkedList<String> ll = new LinkedList<String>();
		while (li.hasNext()) {
			Allow allow = (Allow) li.next();
			ll.add(allow.getMethod());
		}
		return ll.listIterator();
	}

	/**
	 * Sets the methods supported defined by this AllowHeader.
	 *
	 *
	 *
	 * @param methods - the Iterator of Strings defining the methods supported
	 *
	 * in this AllowHeader
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
			Allow allow = new Allow();
			allow.setMethod((String) it.next());
			this.add(allow);
		}
	}
}
/*
 * $Log: AllowList.java,v $
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
