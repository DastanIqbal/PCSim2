/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import javax.sip.InvalidArgumentException;
import javax.sip.header.*;

/**  
 * MimeVersion SIP Header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 * @author Olivier Deruelle <deruelle@nist.gov><br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class MimeVersion extends SIPHeader implements MimeVersionHeader {

	private static final long serialVersionUID = 1L;
	
	/**
	 * mimeVersion field
	 */
	protected int minorVersion;

	/**
	 * majorVersion field
	 */
	protected int majorVersion;

	/**
	 * Default constructor
	 */
	public MimeVersion() {
		super(MIME_VERSION);
	}

	/**
	 * Gets the Minor version value of this MimeVersionHeader.
	 *
	 * @return the Minor version of this MimeVersionHeader
	 */
	public int getMinorVersion() {
		return minorVersion;
	}

	/**
	* Gets the Major version value of this MimeVersionHeader.
	*
	* @return the Major version of this MimeVersionHeader
	*/
	public int getMajorVersion() {
		return majorVersion;
	}

	/**
	 * Sets the Minor-Version argument of this MimeVersionHeader to the supplied
	 * <var>minorVersion</var> value.
	 *
	 * @param minorVersion - the new integer Minor version
	 * @throws InvalidArgumentException
	 */
	public void setMinorVersion(int minorVersion)
		throws InvalidArgumentException {
		if (minorVersion < 0)
			throw new InvalidArgumentException(
				"JAIN-SIP Exception"
					+ ", MimeVersion, setMinorVersion(), the minorVersion parameter is null");
		this.minorVersion = minorVersion;
	}

	/**
	 * Sets the Major-Version argument of this MimeVersionHeader to the supplied
	 * <var>majorVersion</var> value.
	 *
	 * @param majorVersion - the new integer Major version
	 * @throws InvalidArgumentException
	 */
	public void setMajorVersion(int majorVersion)
		throws InvalidArgumentException {
		if (majorVersion < 0)
			throw new InvalidArgumentException(
				"JAIN-SIP Exception"
					+ ", MimeVersion, setMajorVersion(), the majorVersion parameter is null");
		this.majorVersion = majorVersion;
	}

	/**
	 * Return canonical form.
	 * @return String
	 */
	public String encodeBody() {
		return new Integer(majorVersion).toString()
			+ DOT
			+ new Integer(minorVersion).toString();
	}

}
/*
 * $Log: MimeVersion.java,v $
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
