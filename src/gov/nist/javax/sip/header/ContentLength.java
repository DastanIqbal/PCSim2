/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import javax.sip.*;

/**
* ContentLength SIPHeader (of which there can be only one in a SIPMessage).
*<pre>
*Fielding, et al.            Standards Track                   [Page 119]
*RFC 2616                        HTTP/1.1                       June 1999
*
*
*      14.13 Content-Length
*
*   The Content-Length entity-header field indicates the size of the
*   entity-body, in decimal number of OCTETs, sent to the recipient or,
*   in the case of the HEAD method, the size of the entity-body that
*   would have been sent had the request been a GET.
*
*       Content-Length    = "Content-Length" ":" 1*DIGIT
*
*   An example is
*
*       Content-Length: 3495
*
*   Applications SHOULD use this field to indicate the transfer-length of
*   the message-body, unless this is prohibited by the rules in section
*   4.4.
*
*   Any Content-Length greater than or equal to zero is a valid value.
*   Section 4.4 describes how to determine the length of a message-body
*   if a Content-Length is not given.
*
*   Note that the meaning of this field is significantly different from
*   the corresponding definition in MIME, where it is an optional field
*   used within the "message/external-body" content-type. In HTTP, it
*   SHOULD be sent whenever the message's length can be determined prior
*   to being transferred, unless this is prohibited by the rules in
*   section 4.4.
* </pre>
*
*@author M. Ranganathan <mranga@nist.gov>  <br/>
*@author Olivier Deruelle <deruelle@nist.gov><br/>
* @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
*/
public class ContentLength
	extends SIPHeader
	implements javax.sip.header.ContentLengthHeader {

	private static final long serialVersionUID = 1L;
	
	/**
	 * contentLength field.
	 */
	protected Integer contentLength;

	/**
	 * Default constructor.
	 */
	public ContentLength() {
		super(NAME);
	}

	/** 
	 * Constructor given a length.
	 */
	public ContentLength(int length) {
		super(NAME);
		this.contentLength = new Integer(length);
	}

	/**
	 * get the ContentLength field.
	 * @return int
	 */
	public int getContentLength() {
		return contentLength.intValue();
	}

	/**
	 * Set the contentLength member
	 * @param contentLength int to set
	 */
	public void setContentLength(int contentLength)
		throws InvalidArgumentException {
		if (contentLength < 0)
			throw new InvalidArgumentException(
				"JAIN-SIP Exception"
					+ ", ContentLength, setContentLength(), the contentLength parameter is <0");
		this.contentLength = new Integer(contentLength);
	}

	/**
	 * Encode into a canonical string.
	 * @return String
	 */
	public String encodeBody() {
		if (contentLength == null)
			return "0";
		else
			return contentLength.toString();
	}

	/**
	 * Pattern matcher ignores content length.
	 */
	public boolean match(Object other) {
		if (other instanceof ContentLength)
			return true;
		else
			return false;
	}
}
/*
 * $Log: ContentLength.java,v $
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
