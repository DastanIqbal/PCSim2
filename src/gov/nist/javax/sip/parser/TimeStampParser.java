package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import java.text.ParseException;
import javax.sip.*;

/**
 * Parser for TimeStamp header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:32 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov>  <br/>
 * @author M. Ranganathan <mranga@nist.gov>  <br/>
 *
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class TimeStampParser extends HeaderParser {

	/**
	 * Creates a new instance of TimeStampParser 
	 * @param timeStamp the header to parse
	 */
	public TimeStampParser(String timeStamp) {
		super(timeStamp);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected TimeStampParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * @return SIPHeader (TimeStamp object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {

		if (debug)
			dbg_enter("TimeStampParser.parse");
		TimeStamp timeStamp = new TimeStamp();
		try {
			headerName(TokenTypes.TIMESTAMP);

			timeStamp.setHeaderName(SIPHeaderNames.TIMESTAMP);

			this.lexer.SPorHT();
			// PC 2.0 - Change the parsing to use strings.
			String firstNumber = this.lexer.number();

			try {
//				float ts;

				if (lexer.lookAhead(0) == '.') {
					this.lexer.match('.');
//					String secondNumber = this.lexer.number();

//					String s = new String(firstNumber + "." + secondNumber);
//					ts = Float.parseFloat(s);
				} 
//				else
//					ts = Float.parseFloat(firstNumber);

				timeStamp.setTimeStamp(firstNumber);
			} catch (NumberFormatException ex) {
				throw createParseException(ex.getMessage());
			} catch (InvalidArgumentException ex) {
				throw createParseException(ex.getMessage());
			}

			this.lexer.SPorHT();
			if (lexer.lookAhead(0) != '\n') {
				firstNumber = this.lexer.number();

				try {
//					float ts;

					if (lexer.lookAhead(0) == '.') {
						this.lexer.match('.');
//						String secondNumber = this.lexer.number();

//						String s = new String(firstNumber + "." + secondNumber);
//						ts = Float.parseFloat(s);
					} 
//					else
//						ts = Float.parseFloat(firstNumber);

					timeStamp.setDelay(firstNumber);
				} catch (NumberFormatException ex) {
					throw createParseException(ex.getMessage());
				} catch (InvalidArgumentException ex) {
					throw createParseException(ex.getMessage());
				}
			}

		} finally {
			if (debug)
				dbg_leave("TimeStampParser.parse");
		}

		return timeStamp;
	}

	/** Test program
	public static void main(String args[]) throws ParseException {
	    String timeStamp[] = {
	        "Timestamp: 54 \n",
	        "Timestamp: 52.34 34.5 \n"
	    };
	    
	    for (int i = 0; i < timeStamp.length; i++ ) {
	        TimeStampParser parser =
	        new TimeStampParser(timeStamp[i]);
	        TimeStamp ts= (TimeStamp) parser.parse();
	        System.out.println("encoded = " + ts.encode());
	    }
	    
	}
	 */
}
/*
 * $Log: TimeStampParser.java,v $
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
