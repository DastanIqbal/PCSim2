package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;

/**
 * Parser for ProxyRequire header.
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:31 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  <br/>
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class ProxyRequireParser extends HeaderParser {

	/**
	 * Creates a new instance of ProxyRequireParser 
	 * @param require the header to parse 
	 */
	public ProxyRequireParser(String require) {
		super(require);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected ProxyRequireParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * @return SIPHeader (ProxyRequireList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {
		ProxyRequireList list = new ProxyRequireList();
		if (debug)
			dbg_enter("ProxyRequireParser.parse");

		try {
			headerName(TokenTypes.PROXY_REQUIRE);

			while (lexer.lookAhead(0) != '\n') {
				ProxyRequire r = new ProxyRequire();
				r.setHeaderName(SIPHeaderNames.PROXY_REQUIRE);

				// Parsing the option tag
				this.lexer.match(TokenTypes.ID);
				Token token = lexer.getNextToken();
				r.setOptionTag(token.getTokenValue());
				this.lexer.SPorHT();

				list.add(r);

				while (lexer.lookAhead(0) == ',') {
					this.lexer.match(',');
					this.lexer.SPorHT();

					r = new ProxyRequire();

					// Parsing the option tag
					this.lexer.match(TokenTypes.ID);
					token = lexer.getNextToken();
					r.setOptionTag(token.getTokenValue());
					this.lexer.SPorHT();

					list.add(r);
				}

			}
		} finally {
			if (debug)
				dbg_leave("ProxyRequireParser.parse");
		}

		return list;
	}

	/** Test program
	public static void main(String args[]) throws ParseException {
	    String r[] = {
	        "Proxy-Require: foo \n",
	        "Proxy-Require: foo1, foo2 , 389\n"
	    };
	    
	    for (int i = 0; i < r.length; i++ ) {
	        ProxyRequireParser parser =
	        new ProxyRequireParser(r[i]);
	        ProxyRequireList rl= (ProxyRequireList) parser.parse();
	        System.out.println("encoded = " + rl.encode());
	    }
	}
	 */
}
/*
 * $Log: ProxyRequireParser.java,v $
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
