package gov.nist.javax.sip.stack;

import java.util.EventObject;

/**
 * An event that indicates that a transaction has encountered an error.
 *
 * @author Jeff Keyser 
 * @author M. Ranganathan
 *
 * @version  JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:33 $
 */
public class SIPTransactionErrorEvent extends EventObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 *	This event ID indicates that the transaction has timed out.
	 */
	public static final int TIMEOUT_ERROR = 1;

	/**
	 * This event ID indicates that there was an error sending a message using
	 * the underlying transport.
	 */
	public static final int TRANSPORT_ERROR = 2;

	/**
	 * Retransmit signal to application layer.
	 */
	public static final int TIMEOUT_RETRANSMIT = 3;

	// ID of this error event
	private int errorID;

	/**
	 * Creates a transaction error event.
	 *
	 * @param sourceTransaction Transaction which is raising the error.
	 * @param transactionErrorID ID of the error that has ocurred.
	 */
	SIPTransactionErrorEvent(
		SIPTransaction sourceTransaction,
		int transactionErrorID) {

		super(sourceTransaction);
		errorID = transactionErrorID;

	}

	/**
	 * Returns the ID of the error.
	 *
	 * @return Error ID.
	 */
	public int getErrorID() {
		return errorID;
	}
}
/*
 * $Log: SIPTransactionErrorEvent.java,v $
 * Revision 1.4  2004/01/22 13:26:33  sverker
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
