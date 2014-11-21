/*******************************************************************************
 * Product of NIST/ITL Advanced Networking Technologies Division (ANTD).        *
 *******************************************************************************/

package gov.nist.javax.sip.header;

import java.text.ParseException;
import javax.sip.header.*;
import java.util.*;

/**  
 * Supported SIP Header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.2 $ $Date: 2004/01/22 13:26:29 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 * @author Olivier Deruelle <deruelle@nist.gov><br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 */
public class Server extends SIPHeader implements ServerHeader {

	private static final long serialVersionUID = 1L;
	
	/** Product tokens.
	*/
	protected List<String> productTokens;

	/**
	 * Return canonical form.
	 * @return String
	 */
	private String encodeProduct() {
		StringBuffer tokens = new StringBuffer();
		ListIterator<String> it = productTokens.listIterator();

		while (it.hasNext()) {
			tokens.append((String) it.next());
			if (it.hasNext())
				tokens.append('/');
			else
				break;
		}
		return tokens.toString();
	}

	/** set the productToken field
	 * @param pt String to set
	 */
	public void addProductToken(String pt) {
		productTokens.add(pt);
	}

	/**
	 * Constructor.
	 */
	public Server() {
		super(NAME);
		productTokens = new LinkedList<String>();
	}

	/** Encode only the body of this header.
	*@return encoded value of the header.
	*/
	public String encodeBody() {
		return encodeProduct();
	}

	/**
	* Returns the list value of the product parameter.
	*
	* @return the software of this UserAgentHeader
	*/
	public ListIterator<String> getProduct() {
		if (productTokens == null || productTokens.isEmpty())
			return null;
		else
			return productTokens.listIterator();
	}

	/**
	 * Sets the product value of the UserAgentHeader.
	 *
	 * @param product - a List specifying the product value
	 * @throws ParseException which signals that an error has been reached
	 * unexpectedly while parsing the product value.
	 */
	public void setProduct(List<String> product) throws ParseException {
		if (product == null)
			throw new NullPointerException(
				"JAIN-SIP Exception, UserAgent, "
					+ "setProduct(), the "
					+ " product parameter is null");
		productTokens = product;
	}
}
/*
 * $Log: Server.java,v $
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
