package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import javax.sip.*;

/**
 * Parser for RAck header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:31 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  <br/>
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 *
 * @version 1.0
 */
public class RAckParser extends HeaderParser {

	/**
	 * Creates a new instance of RAckParser 
	 * @param rack the header to parse 
	 */
	public RAckParser(String rack) {
		super(rack);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected RAckParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * @return SIPHeader (RAck object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {

		if (debug)
			dbg_enter("RAckParser.parse");
		RAck rack = new RAck();
		try {
			headerName(TokenTypes.RACK);

			rack.setHeaderName(SIPHeaderNames.RACK);

			try {
				String number = this.lexer.number();
				rack.setRSeqNumber(Integer.parseInt(number));
				this.lexer.SPorHT();
				number = this.lexer.number();
				rack.setCSeqNumber(Integer.parseInt(number));
				this.lexer.SPorHT();
				this.lexer.match(TokenTypes.ID);
				Token token = lexer.getNextToken();
				rack.setMethod(token.getTokenValue());

			} catch (InvalidArgumentException ex) {
				throw createParseException(ex.getMessage());
			}
			this.lexer.SPorHT();
			this.lexer.match('\n');

			return rack;
		} finally {
			if (debug)
				dbg_leave("RAckParser.parse");
		}
	}

	/** Test program
	public static void main(String args[]) throws ParseException {
	String r[] = {
	        "RAck: 776656 1 INVITE\n"
	        };
		
	for (int i = 0; i < r.length; i++ ) {
	    RAckParser parser = 
		  new RAckParser(r[i]);
	    RAck ra= (RAck) parser.parse();
	    System.out.println("encoded = " + ra.encode());
	}		
	}
	 */
}
/*
 * $Log: RAckParser.java,v $
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
