/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
* See ../../../../doc/uncopyright.html for conditions of use.                  *
* Author: M. Ranganathan (mranga@nist.gov)                                     *
* Modified By:  O. Deruelle (deruelle@nist.gov)                                * 
* Questions/Comments: nist-sip-dev@antd.nist.gov                               *
*******************************************************************************/
package gov.nist.javax.sip.header;

/**
*   Media Range 
* @see Accept
* @since 0.9
* @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
* <pre>
* Revisions:
*
* Version 1.0
*    1. Added encode method.
*
* media-range    = ( "STAR/STAR"
*                        | ( type "/" STAR )
*                        | ( type "/" subtype )
*                        ) *( ";" parameter )       
* 
* HTTP RFC 2616 Section 14.1
* </pre>
*/
public class MediaRange extends SIPObject {

	private static final long serialVersionUID = 1L;
	
	/** type field
	 */
	protected String type;

	/** subtype field
	 */
	protected String subtype;

	/** Default constructor
	 */
	public MediaRange() {
	}

	/** get type field
	 * @return String
	 */
	public String getType() {
		return type;
	}

	/** get the subType field.
	 * @return String
	 */
	public String getSubtype() {
		return subtype;
	}

	/**
	 * Set the type member
	 * @param t String to set
	 */
	public void setType(String t) {
		type = t;
	}

	/**
	 * Set the subtype member
	 * @param s String to set
	 */
	public void setSubtype(String s) {
		subtype = s;
	}

	/**
	 * Encode the object.
	 * @return String
	 */
	public String encode() {
		String encoding = type + SLASH + subtype;
		return encoding;
	}
}
/*
 * $Log: MediaRange.java,v $
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
