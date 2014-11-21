package gov.nist.javax.sip.header;

import java.text.ParseException;

/**  
 * TargetDialog SIP Header.
 */
public class TargetDialog
	extends ParametersHeader
	implements javax.sip.header.TargetDialogHeader {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * callIdentifier field
	 */
	protected CallIdentifier callIdentifier;

	/**
	 * Default constructor
	 */
	public TargetDialog() {
		super(NAME);
	}

	/**
	 * Compare two call ids for equality.
	 * @param other Object to set
	 * @return true if the two call ids are equals, false otherwise
	 */
	public boolean equals(Object other) {
		if (!this.getClass().equals(other.getClass())) {
			return false;
		}
		CallID that = (CallID) other;
		return this.callIdentifier.equals(that.callIdentifier);
	}

	/**
	 * Encode the body part of this header (i.e. leave out the hdrName).
	 *@return String encoded body part of the header.
	 */
	public String encodeBody() {
		if (callIdentifier == null)
			return null;
		else {
			StringBuffer encoding = new StringBuffer();
			encoding.append(callIdentifier.encode());	
			if (!parameters.isEmpty()) {
				encoding.append(SEMICOLON).append(parameters.encode());
			}
			return encoding.toString();
		}
	}

	/**
	 * get the CallId field. This does the same thing as
	 * encodeBody 
	 * @return String the encoded body part of the 
	 */
	public String getCallId() {
		return encodeBody();
	}

	/**
	 * get the call Identifer member.
	 * @return CallIdentifier
	 */
	public CallIdentifier getCallIdentifer() {
		return callIdentifier;
	}

	/**
	 * set the CallId field
	 * @param cid String to set. This is the body part of the Call-Id
	 *  header. It must have the form localId@host or localId.
	 * @throws IllegalArgumentException if cid is null, not a token, or is 
	 * not a token@token.
	 */
	public void setCallId(String cid) throws ParseException {
		try {
			callIdentifier = new CallIdentifier(cid);
		} catch (IllegalArgumentException ex) {
			throw new ParseException(cid, 0);
		}
	}

	/**
	 * Set the callIdentifier member.
	 * @param cid CallIdentifier to set (localId@host).
	 */
	public void setCallIdentifier(CallIdentifier cid) {
		callIdentifier = cid;
	}

	/** Constructor given the call Identifier.
	 *@param callId string call identifier (should be localid@host)
	 *@throws IllegalArgumentException if call identifier is bad.
	 */
	public TargetDialog(String callId) throws IllegalArgumentException {
		super(NAME);
		this.callIdentifier = new CallIdentifier(callId);
	}

	public Object clone() {
		CallID retval = (CallID) super.clone();
		if (this.callIdentifier != null)
			retval.callIdentifier = (CallIdentifier) this.callIdentifier.clone();
		return retval;
	}
}



