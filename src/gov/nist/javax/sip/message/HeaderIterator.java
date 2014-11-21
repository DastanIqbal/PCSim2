package gov.nist.javax.sip.message;

import gov.nist.javax.sip.header.*;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * Allows for uniform removal handling for singleton headers.
 * @version JAIN-SIP-1.1 $Revision: 1.5 $ $Date: 2004/01/22 13:26:31 $
 */
public class HeaderIterator implements ListIterator<Object> {
	private boolean toRemove;
	private int index;
	private SIPMessage sipMessage;
	private SIPHeader sipHeader;

	protected HeaderIterator(SIPMessage sipMessage, SIPHeader sipHeader) {
		this.sipMessage = sipMessage;
		this.sipHeader = sipHeader;
	}

	public Object next() throws NoSuchElementException {
		if (sipHeader == null || index == 1)
			throw new NoSuchElementException();
		toRemove = true;
		index = 1;
		return (Object) sipHeader;
	}

	public Object previous() throws NoSuchElementException {
		if (sipHeader == null || index == 0)
			throw new NoSuchElementException();
		toRemove = true;
		index = 0;
		return (Object) sipHeader;
	}

	public int nextIndex() {
		return 1;
	}

	public int previousIndex() {
		return index == 0 ? -1 : 0;
	}

	public void set(Object header) {
		throw new UnsupportedOperationException();
	}

	public void add(Object header) {
		throw new UnsupportedOperationException();
	}

	public void remove() throws IllegalStateException {
		if (this.sipHeader == null)
			throw new IllegalStateException();
		if (toRemove) {
			this.sipHeader = null;
			this.sipMessage.removeHeader(sipHeader.getName());
		} else {
			throw new IllegalStateException();
		}
	}

	public boolean hasNext() {
		return index == 0;
	}

	public boolean hasPrevious() {
		return index == 1;
	}
}
/*
 * $Log: HeaderIterator.java,v $
 * Revision 1.5  2004/01/22 13:26:31  sverker
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
