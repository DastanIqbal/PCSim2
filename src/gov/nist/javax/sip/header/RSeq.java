package gov.nist.javax.sip.header;

import javax.sip.*;

/**
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class RSeq extends SIPHeader implements javax.sip.header.RSeqHeader {

	private static final long serialVersionUID = 1L;
	
	protected int sequenceNumber;

	/** Creates a new instance of RSeq */
	public RSeq() {
		super(NAME);
	}

	/** Gets the sequence number of this RSeqHeader.
	 *
	 * @return the integer value of the Sequence number of the RSeqHeader
	 */
	public int getSequenceNumber() {
		return this.sequenceNumber;
	}

	/** Sets the sequence number value of the RSeqHeader of the provisional
	 * response. The sequence number MUST be expressible as a 32-bit unsigned
	 * integer and MUST be less than 2**31.
	 *
	 * @param sequenceNumber - the new Sequence number of this RSeqHeader
	 * @throws InvalidArgumentException if supplied value is less than zero.
	 */
	public void setSequenceNumber(int sequenceNumber)
		throws InvalidArgumentException {
		if (sequenceNumber <= 0)
			throw new InvalidArgumentException(
				"Bad seq number " + sequenceNumber);
		this.sequenceNumber = sequenceNumber;
	}

	/** Encode the body of this header (the stuff that follows headerName).
	 * A.K.A headerValue.
	 */
	protected String encodeBody() {
		return new Integer(this.sequenceNumber).toString();
	}
}
/*
 * $Log: RSeq.java,v $
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
