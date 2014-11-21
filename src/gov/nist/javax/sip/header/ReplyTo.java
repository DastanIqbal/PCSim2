/*******************************************************************************
* Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
*******************************************************************************/
package gov.nist.javax.sip.header;

import gov.nist.core.*;
import gov.nist.javax.sip.address.*;
import javax.sip.header.*;

/**  
 * ReplyTo Header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 * @author Olivier Deruelle <deruelle@nist.gov><br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public final class ReplyTo
	extends AddressParametersHeader
	implements ReplyToHeader {

	private static final long serialVersionUID = 1L;
	
	/** Default constructor
	 */
	public ReplyTo() {
		super(NAME);
	}

	/** Default constructor given an address.
	 *
	 *@param address -- address of this header.
	 *
	 */
	public ReplyTo(AddressImpl address) {
		super(NAME);
		this.address = address;
	}

	/**
	 * Encode the header into a String.
	 * @return String
	 */
	public String encode() {
		return headerName + COLON + SP + encodeBody() + NEWLINE;
	}

	/**
	 * Encode the header content into a String.
	 * @return String
	 */
	public String encodeBody() {
		String retval = "";
		if (address.getAddressType() == AddressImpl.ADDRESS_SPEC) {
			retval += LESS_THAN;
		}
		retval += address.encode();
		if (address.getAddressType() == AddressImpl.ADDRESS_SPEC) {
			retval += GREATER_THAN;
		}
		if (!parameters.isEmpty()) {
			retval += SEMICOLON + parameters.encode();
		}
		return retval;
	}

	/**
	 * Conveniance accessor function to get the hostPort field from the address
	 * @return HostPort
	 */
	public HostPort getHostPort() {
		return address.getHostPort();
	}

	/**
	 * Get the display name from the address.
	 * @return String
	 */
	public String getDisplayName() {
		return address.getDisplayName();
	}
}
/*
 * $Log: ReplyTo.java,v $
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
