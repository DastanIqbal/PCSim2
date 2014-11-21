package gov.nist.javax.sip.parser;

import gov.nist.javax.sip.header.*;
import gov.nist.core.*;
import java.text.ParseException;
import javax.sip.*;

/**
 * Parser for SubscriptionState header.
 *
 * @version JAIN-SIP-1.1 $Revision: 1.4 $ $Date: 2004/01/22 13:26:32 $
 *
 * @author Olivier Deruelle <deruelle@nist.gov>
 * @author M. Ranganathan <mranga@nist.gov> 
 * <a href="{@docRoot}/uncopyright.html">This code is in the public domain.</a>
 */
public class SubscriptionStateParser extends HeaderParser {

	/**
	 * Creates a new instance of SubscriptionStateParser 
	 * @param subscriptionState the header to parse
	 */
	public SubscriptionStateParser(String subscriptionState) {
		super(subscriptionState);
	}

	/**
	 * Constructor
	 * @param lexer the lexer to use to parse the header
	 */
	protected SubscriptionStateParser(Lexer lexer) {
		super(lexer);
	}

	/**
	 * parse the String message
	 * @return SIPHeader (SubscriptionState  object)
	 * @throws SIPParseException if the message does not respect the spec.
	 */
	public SIPHeader parse() throws ParseException {

		if (debug)
			dbg_enter("SubscriptionStateParser.parse");

		SubscriptionState subscriptionState = new SubscriptionState();
		try {
			headerName(TokenTypes.SUBSCRIPTION_STATE);

			subscriptionState.setHeaderName(SIPHeaderNames.SUBSCRIPTION_STATE);

			// State:
			lexer.match(TokenTypes.ID);
			Token token = lexer.getNextToken();
			subscriptionState.setState(token.getTokenValue());

			while (lexer.lookAhead(0) == ';') {
				this.lexer.match(';');
				this.lexer.SPorHT();
				lexer.match(TokenTypes.ID);
				token = lexer.getNextToken();
				String value = token.getTokenValue();
				if (value.equalsIgnoreCase("reason")) {
					this.lexer.match('=');
					this.lexer.SPorHT();
					lexer.match(TokenTypes.ID);
					token = lexer.getNextToken();
					value = token.getTokenValue();
					subscriptionState.setReasonCode(value);
				} else if (value.equalsIgnoreCase("expires")) {
					this.lexer.match('=');
					this.lexer.SPorHT();
					lexer.match(TokenTypes.ID);
					token = lexer.getNextToken();
					value = token.getTokenValue();
					try {
						int expires = Integer.parseInt(value);
						subscriptionState.setExpires(expires);
					} catch (NumberFormatException ex) {
						throw createParseException(ex.getMessage());
					} catch (InvalidArgumentException ex) {
						throw createParseException(ex.getMessage());
					}
				} else if (value.equalsIgnoreCase("retry-after")) {
					this.lexer.match('=');
					this.lexer.SPorHT();
					lexer.match(TokenTypes.ID);
					token = lexer.getNextToken();
					value = token.getTokenValue();
					try {
						int retryAfter = Integer.parseInt(value);
						subscriptionState.setRetryAfter(retryAfter);
					} catch (NumberFormatException ex) {
						throw createParseException(ex.getMessage());
					} catch (InvalidArgumentException ex) {
						throw createParseException(ex.getMessage());
					}
				} else {
					this.lexer.match('=');
					this.lexer.SPorHT();
					lexer.match(TokenTypes.ID);
					Token secondToken = lexer.getNextToken();
					String secondValue = secondToken.getTokenValue();
					subscriptionState.setParameter(value, secondValue);
				}
				this.lexer.SPorHT();
			}
		} finally {
			if (debug)
				dbg_leave("SubscriptionStateParser.parse");
		}

		return subscriptionState;
	}

	/** Test program
	public static void main(String args[]) throws ParseException {
	    String subscriptionState[] = {
	        "Subscription-State: active \n",
	        "Subscription-State: terminated;reason=rejected \n",
	        "Subscription-State: pending;reason=probation;expires=36\n",
	        "Subscription-State: pending;retry-after=10;expires=36\n",
	        "Subscription-State: pending;generic=void\n"
	    };
	    
	    for (int i = 0; i < subscriptionState.length; i++ ) {
	        SubscriptionStateParser parser =
	        new SubscriptionStateParser(subscriptionState[i]);
	        SubscriptionState ss= (SubscriptionState) parser.parse();
	        System.out.println("encoded = " + ss.encode());
	    }
	}
	 */
}
/*
 * $Log: SubscriptionStateParser.java,v $
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
