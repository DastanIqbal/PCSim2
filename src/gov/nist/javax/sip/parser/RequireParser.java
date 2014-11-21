package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;

/**
 * Parser for Require header.
 * 
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:31 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 * @version 1.0
 */
public class RequireParser extends HeaderParser {

	/**
	 * Creates a new instance of RequireParser 
	 * @param require the header to parse
	 */
	public RequireParser(String require) {
		super(require);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected RequireParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * @return SIPHeader (RequireList object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {
		RequireList requireList = new RequireList();
		if (debug)
			dbg_enter("RequireParser.parse");

		try {
			headerName(TokenTypes.REQUIRE);

			while (lexer.lookAhead(0) != '\n') {
				Require r = new Require();
				r.setHeaderName(SIPHeaderNames.REQUIRE);

				// Parsing the option tag
				this.lexer.match(TokenTypes.ID);
				Token token = lexer.getNextToken();
				r.setOptionTag(token.getTokenValue());
				this.lexer.SPorHT();

				requireList.add(r);

				while (lexer.lookAhead(0) == ',') {
					this.lexer.match(',');
					this.lexer.SPorHT();

					r = new Require();

					// Parsing the option tag
					this.lexer.match(TokenTypes.ID);
					token = lexer.getNextToken();
					r.setOptionTag(token.getTokenValue());
					this.lexer.SPorHT();

					requireList.add(r);
				}

			}
		} finally {
			if (debug)
				dbg_leave("RequireParser.parse");
		}

		return requireList;
	}

	/** Test program
	public static void main(String args[]) throws ParseException {
	    String r[] = {
	        "Require: 100rel \n",
	        "Require: 100rel, 200ok , 389\n"
	    };
	    
	    for (int i = 0; i < r.length; i++ ) {
	        RequireParser parser =
	        new RequireParser(r[i]);
	        RequireList rl= (RequireList) parser.parse();
	        System.out.println("encoded = " + rl.encode());
	    }
	}
	 */

}
/*
 * $Log: RequireParser.java,v $
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
