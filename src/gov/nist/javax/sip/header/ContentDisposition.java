/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import java.text.*;

/**
 * Content Dispositon SIP Header.
 * 
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * <a href="${docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public final class ContentDisposition
	extends ParametersHeader
	implements javax.sip.header.ContentDispositionHeader {

	private static final long serialVersionUID = 1L;
	
	/**
	 * DispositionType field.  
	 */
	protected String dispositionType;

	/**
	 * Default constructor.
	 */
	public ContentDisposition() {
		super(NAME);
	}

	/**
	 * Encode value of header into canonical string.
	 * @return encoded value of header.
	 *
	 */
	public String encodeBody() {
		StringBuffer encoding = new StringBuffer(dispositionType);
		if (!this.parameters.isEmpty()) {
			encoding.append(SEMICOLON).append(parameters.encode());
		}
		return encoding.toString();
	}

	/**
	 * Set the disposition type.
	 * @param dispositionType type.
	 */
	public void setDispositionType(String dispositionType)
		throws ParseException {
		if (dispositionType == null)
			throw new NullPointerException(
				"JAIN-SIP Exception"
					+ ", ContentDisposition, setDispositionType(), the dispositionType parameter is null");
		this.dispositionType = dispositionType;
	}

	/**
	 * Get the disposition type.
	 * @return Disposition Type
	 */
	public String getDispositionType() {
		return this.dispositionType;
	}

	/**
	 * Get the dispositionType field.
	 * @return String
	 */
	public String getHandling() {
		return this.getParameter("handling");
	}

	/** set the dispositionType field.
	 * @param handling String to set.
	 */
	public void setHandling(String handling) throws ParseException {
		if (handling == null)
			throw new NullPointerException(
				"JAIN-SIP Exception"
					+ ", ContentDisposition, setHandling(), the handling parameter is null");
		this.setParameter("handling", handling);
	}

	/**
	 * Gets the interpretation of the message body or message body part of
	 * this ContentDispositionHeader.
	 * 
	 * @return interpretation of the message body or message body part
	 */
	public String getContentDisposition() {
		return this.encodeBody();
	}
}
/*
 * $Log: ContentDisposition.java,v $
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
