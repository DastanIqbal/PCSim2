package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;

/**
 * Parser for ProxyAuthenticate headers.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:31 $
 *
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class ProxyAuthenticateParser extends ChallengeParser {

	/**
	 * Constructor
	 * @param proxyAuthenticate message to parse
	 */
	public ProxyAuthenticateParser(String proxyAuthenticate) {
		super(proxyAuthenticate);
	}

	/**
	 * Cosntructor
	 * @param Lexer lexer to set
	 */
	protected ProxyAuthenticateParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message 
	 * @return SIPHeader (ProxyAuthenticate object)
	 * @throws ParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {
		headerName(TokenTypes.PROXY_AUTHENTICATE);
		ProxyAuthenticate proxyAuthenticate = new ProxyAuthenticate();
		super.parse(proxyAuthenticate);
		return proxyAuthenticate;
	}

	/** Test program
	public static void main(String args[]) throws ParseException {
	String paAuth[] = {
	"Proxy-Authenticate: Digest realm=\"MCI WorldCom SIP\","+
	"domain=\"sip:ss2.wcom.com\", nonce=\"ea9c8e88df84f1cec4341ae6cbe5a359\","+
	"opaque=\"\", stale=FALSE, algorithm=MD5\n",
	                
	"Proxy-Authenticate: Digest realm=\"MCI WorldCom SIP\","+ 
	"qop=\"auth\" , nonce-value=\"oli\"\n"
	        };
		
	for (int i = 0; i < paAuth.length; i++ ) {
	    ProxyAuthenticateParser pap = 
		  new ProxyAuthenticateParser(paAuth[i]);
	    ProxyAuthenticate pa= (ProxyAuthenticate) pap.parse();
	    System.out.println("encoded = " + pa.encode());
	}
		
	}
	 */
}
/*
 * $Log: ProxyAuthenticateParser.java,v $
 * Revision 1.4  2004/01/22 13:26:31  sverker
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
