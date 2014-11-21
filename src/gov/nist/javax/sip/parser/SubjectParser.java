package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;

/**
 * Parser for Subject  header.
 *
 * @version  JAIN-SIP-1.1
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  <br/>
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class SubjectParser extends HeaderParser {

	/**
	 * Creates a new instance of SubjectParser 
	 * @param subject the header to parse
	 */
	public SubjectParser(String subject) {
		super(subject);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected SubjectParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * @return SIPHeader (Subject object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {
		Subject subject = new Subject();
		if (debug)
			dbg_enter("SubjectParser.parse");

		try {
			headerName(TokenTypes.SUBJECT);

			this.lexer.SPorHT();

			String s = this.lexer.getRest();
			subject.setSubject(s.trim());

		} finally {
			if (debug)
				dbg_leave("SubjectParser.parse");
		}

		return subject;
	}

	/** Test program
	public static void main(String args[]) throws ParseException {
	String subject[] = {
	        "Subject: Where is the Moscone?\n",
	        "Subject: Need more boxes\n"
	        };
		
	for (int i = 0; i < subject.length; i++ ) {
	    SubjectParser parser = 
		  new SubjectParser(subject[i]);
	    Subject s= (Subject) parser.parse();
	    System.out.println("encoded = " +s.encode());
	}
		
	}
	 */
}
/*
 * $Log: SubjectParser.java,v $
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
