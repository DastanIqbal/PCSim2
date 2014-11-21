package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;

/**
 * Parser for Supported header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:32 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  <br/>
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 * @version 1.0
 */
public class SupportedParser extends HeaderParser {

	/**
	 * Creates a new instance of SupportedParser 
	 * @param supported the header to parse
	 */
	public SupportedParser(String supported) {
		super(supported);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected SupportedParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * @return SIPHeader (Supported object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {
		SupportedList supportedList = new SupportedList();
		if (debug)
			dbg_enter("SupportedParser.parse");

		try {
			headerName(TokenTypes.SUPPORTED);

			while (lexer.lookAhead(0) != '\n') {
				this.lexer.SPorHT();
				Supported supported = new Supported();
				supported.setHeaderName(SIPHeaderNames.SUPPORTED);

				// Parsing the option tag
				this.lexer.match(TokenTypes.ID);
				Token token = lexer.getNextToken();
				supported.setOptionTag(token.getTokenValue());
				this.lexer.SPorHT();

				supportedList.add(supported);

				while (lexer.lookAhead(0) == ',') {
					this.lexer.match(',');
					this.lexer.SPorHT();

					supported = new Supported();

					// Parsing the option tag
					this.lexer.match(TokenTypes.ID);
					token = lexer.getNextToken();
					supported.setOptionTag(token.getTokenValue());
					this.lexer.SPorHT();

					supportedList.add(supported);
				}

			}
		} finally {
			if (debug)
				dbg_leave("SupportedParser.parse");
		}

		return supportedList;
	}

	/** Test program
	public static void main(String args[]) throws ParseException {
	    String supported[] = {
	        "Supported: 100rel \n",
	        "Supported: foo1, foo2 ,foo3 , foo4 \n"
	    };
	    
	    for (int i = 0; i < supported.length; i++ ) {
	        SupportedParser parser =
	        new SupportedParser(supported[i]);
	        SupportedList s= (SupportedList) parser.parse();
	        System.out.println("encoded = " + s.encode());
	    }
	    
	}
	 */
}
/*
 * $Log: SupportedParser.java,v $
 * Revision 1.4  2004/01/22 13:26:32  sverker
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
