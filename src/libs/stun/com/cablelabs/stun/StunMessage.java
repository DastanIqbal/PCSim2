/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################
*/
package com.cablelabs.stun;

import java.util.*;
import com.cablelabs.log.*;
import com.cablelabs.stun.attributes.*;
import com.cablelabs.common.*;


public class StunMessage {
	/*
	 * The message length MUST contain the size, in bytes, of the message
	 * not including the 20 byte STUN header.
	 */
	private static LogAPI logger = LogAPI.getInstance(); 
	
	private boolean ignoreHdr = false;
	
	private String name = null;
	private char type = 0;
	/**
	 * Length is the length of the header plus all of the attributes. 
	 */
	private int length = 0;
	private byte [] transactionID = new byte [StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH];
	
	

	private LinkedList<StunAttribute> attributes = new LinkedList<StunAttribute>();
	
	/**
	 * This container allows us to store any unknown comprehensive-required attribute
	 * type in a table for easy lookup later. The table is filled as a message is
	 * decoded by the system.
	 */
	private LinkedList<Character> unknownAttrTypes = new LinkedList<Character>();
	
	public StunMessage(char type) throws IllegalArgumentException {
		this.type = type;
		generateTransactionID();
		this.name = StunConstants.getMethodName(this.type);
		this.length = StunConstants.STUN_HEADER_LENGTH;
	}
	public StunMessage(char type, byte [] id) throws IllegalArgumentException {
		this.type = type;
		this.setTransactionID(id);
		this.name = StunConstants.getMethodName(this.type);
		this.length = StunConstants.STUN_HEADER_LENGTH;
	}
	
	/**
	 * This method returns the size of the Stun Message header only.
	 * @return
	 */
	public int size() {
		int size = 0;
		if (!ignoreHdr) 
			size += StunConstants.STUN_HEADER_LENGTH;
		
		return size;
	}

	/**
	 * This method encodes the message for transmission to another network
	 * element.
	 * 
	 * @return - the serialized message.
	 */
	public byte [] encode() {
		int msgLen = size();
		ListIterator<StunAttribute> iter = attributes.listIterator();
		while (iter.hasNext()) {
			StunAttribute attr = (StunAttribute)iter.next();
			msgLen += attr.size();
		}
		byte [] msgBuf = new byte [msgLen];
		int offset = 0;
		if (!ignoreHdr) {
			byte [] temp = Conversion.charToByteArray(type);
			copy(temp, msgBuf, offset, StunConstants.STUN_MESSAGE_TYPE_LENGTH);
			offset += StunConstants.STUN_MESSAGE_TYPE_LENGTH;
			// The length field sent in the message should not include the STUN_HEADER_LENGTH
			temp = StunConstants.lengthToByteArray(length-StunConstants.STUN_HEADER_LENGTH);
			copy(temp, msgBuf, offset, StunConstants.STUN_LENGTH_LENGTH);
			offset += StunConstants.STUN_LENGTH_LENGTH;
			System.arraycopy(transactionID, 0, msgBuf, offset, StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH);
			offset += StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH;
		}
		iter = attributes.listIterator();
		while (iter.hasNext()) {
			StunAttribute attr = (StunAttribute)iter.next();
			offset += attr.encode(msgBuf, offset);
		}
		return msgBuf;
	}
	
	/**
	 * This method returns the message for MD5 digestion excluding the final
	 * attribute in the message, since this is the attribute that will receive
	 * the hash value.
	 * 
	 * @return - the serialized message excluding the final attribute
	 */
	public byte [] encodeForDigest() {
////		int msgLen = size();
////		ListIterator<StunAttribute> iter = attributes.listIterator();
////		while (iter.hasNext()) {
////			StunAttribute attr = (StunAttribute)iter.next();
////			msgLen += attr.size();
////		}
//		int msgLen = length;
//		byte [] msgBuf = new byte [msgLen];
//		int offset = 0;
//		if (!ignoreHdr) {
//			byte [] temp = Conversion.charToByteArray(type);
//			copy(temp, msgBuf, offset, 2);
//			offset += StunConstants.STUN_MESSAGE_TYPE_LENGTH;
//			temp = StunConstants.lengthToByteArray(length);
//			copy(temp, msgBuf, offset, 2);
//			offset += StunConstants.STUN_MESSAGE_TYPE_LENGTH;
//			System.arraycopy(transactionID, 0, msgBuf, offset, StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH);
//			offset += StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH;
//		}
//		ListIterator<StunAttribute> iter = attributes.listIterator();
//		int size = attributes.size();
//		while (iter.hasNext() && size > 1) {
//			StunAttribute attr = (StunAttribute)iter.next();
//			if (attr.getType() == StunConstants.MESSAGE_INTEGRITY_TYPE)
//				size = 1;
//			else
//				offset += attr.encode(msgBuf, offset);
//			size--;
//		}
//		return msgBuf;
		// Encoding for the digest simply requires the length field to include the 
		// message length plus the length of the Message-Integrity field.
		// Store the length before the Message-Integrity attribute is added
		int temp = length;
		length += StunConstants.MESSAGE_INTEGRITY_LENGTH;
		byte [] msgBuf = encode();
		// Restore length
		length = temp;
		return msgBuf;
		
	}
	
	public byte [] encodeForFingerPrint() {
		int temp = length;
		length += StunConstants.STUN_FINGERPRINT_LENGTH *2;
		byte [] msgBuf = encode();
		// Restore length
		length = temp;
		return msgBuf;
	}
	
	private static StunAttribute createAttribute(StunMessage sm, byte [] type, byte [] value) {
		char t = Conversion.getChar(type,0);
		   // (char)(((type[0]<<8) & 0xFF00) | (type[1]&0xFF));
		switch (t) {
		case StunConstants.BANDWIDTH_TYPE :
			return new Bandwidth(value);
		case StunConstants.MAPPED_ADDRESS_TYPE :
			return new MappedAddress(value);
		case StunConstants.USERNAME_TYPE :
			return new Username(value);
		case StunConstants.MESSAGE_INTEGRITY_TYPE :
			return new MessageIntegrity(value);
		case StunConstants.ERROR_CODE_TYPE :
			return new ErrorCode(value);
		case StunConstants.UNKNOWN_ATTRIBUTES_TYPE :
			return new UnknownAttributes(value);
		case StunConstants.REALM_TYPE :
			return new Realm(value);
		case StunConstants.NONCE_TYPE :
			return new Nonce(value);
		case StunConstants.XOR_MAPPED_ADDRESS_TYPE :
			return new XorMappedAddress(value, sm.transactionID);
		case StunConstants.CHANNEL_NUMBER_TYPE :
			return new ChannelNumber(value);
		case StunConstants.LIFETIME_TYPE :
			return new Lifetime(value);
		case StunConstants.PEER_ADDRESS_TYPE :
			return new PeerAddress(value, sm.transactionID);
		case StunConstants.DATA_TYPE :
			return new Data(value);
		case StunConstants.RELAY_ADDRESS_TYPE :
			return new RelayAddress(value, sm.transactionID);
		case StunConstants.REQUESTED_PROPS_TYPE :
			return new RequestedProps(value);
		case StunConstants.REQUESTED_TRANSPORT_TYPE :
			return new RequestedTransport(value);
		case StunConstants.RESERVATION_TOKEN_TYPE :
			return new ReservationToken(value);
		case StunConstants.SOFTWARE_TYPE :
			return new Software(value);
		case StunConstants.ALTERNATE_SERVER_TYPE :
			return new AlternateServer(value);
		case StunConstants.FINGERPRINT_TYPE :
			return new FingerPrint(value);
		case StunConstants.PASSWORD_TYPE :
			return new Password(value);
		case StunConstants.ICE_CONTROLLED_TYPE :
			return new IceControlled(value);
		case StunConstants.ICE_CONTROLLING_TYPE :
			return new IceControlling(value);
		case StunConstants.USE_CANDIDATE_TYPE :
			return new UseCandidate(value);
		case StunConstants.PRIORITY_TYPE :
			return new Priority(value);
		default :
			if (StunConstants.isComprehensionRequired(t))
					sm.unknownAttrTypes.add(t);
			return new StunAttribute(type, value);
		}
	}
	/**
	 * This method copies all bytes from data up to the given length
	 * into the message buffer starting at the offset position in the 
	 * message buffer.
	 * @param data
	 * @param buffer
	 * @param offset
	 * @param length
	 */
	private void copy(byte [] data, byte [] buffer, int offset, int length) {
		int j = 0;
		for (int i = offset; j < length; i++, j++) {
			buffer[i] = data[j];
		}
	}
	
//	private byte[] lengthToByteArray() {
//		byte[] byteArray = new byte[2];
//		byteArray[0] = (byte)((length & 0x0000FF00)>>>8);
//		byteArray[1] = (byte)((length & 0x000000FF));
//		return (byteArray);
//	}
	
	public static StunMessage decode(byte packetBuf[], 
			int offset, int bufferLen) throws StunException  {
		bufferLen = (int)Math.min(packetBuf.length, bufferLen);
		byte [] msgEvent = new byte [bufferLen];
		System.arraycopy(packetBuf, 0, msgEvent, offset, bufferLen);
		
		if(packetBuf == null || bufferLen - offset < StunConstants.STUN_HEADER_LENGTH)
			throw new StunException("The packet buffer doesn't contain valid StunMessage.");
		
		// byte [] msgType = new byte [StunConstants.STUN_MESSAGE_TYPE_LENGTH];
		// System.arraycopy(packetBuf, offset, msgType, 0, StunConstants.STUN_MESSAGE_TYPE_LENGTH);
		char msgType = Conversion.getChar(packetBuf, offset);
		 //(char)((packetBuf[offset++]<<8) | (packetBuf[offset++]&0xFF));
		offset += StunConstants.STUN_MESSAGE_TYPE_LENGTH;
		int length = Conversion.getChar(packetBuf, offset) + StunConstants.STUN_HEADER_LENGTH;
		//(char)((packetBuf[offset++]<<8) | (packetBuf[offset++]&0xFF));
		offset += StunConstants.STUN_LENGTH_LENGTH;
		logger.trace(PC2LogCategory.STUN, "Stack",
				"arrayLen=" + bufferLen + " offset=" + offset);
		
		if(bufferLen < length)
			throw new StunException("The packet buffer does not contain a whole StunMessage.");
		
		byte [] transID = new byte[StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH];
		System.arraycopy(packetBuf, offset, transID, 0, StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH);
		
		if (transID[0] != 0) {
			byte [] mc = new byte [StunConstants.STUN_MAGIC_COOKIE_LENGTH];
			System.arraycopy(transID, 0, mc, 0, mc.length);
			if (java.util.Arrays.equals(StunConstants.STUN_MAGIC_COOKIE,mc)) {
				try {
					//String temp = new String(msgType, 0, 2);
					StunMessage sm = new StunMessage(msgType, transID);
					offset += StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH;
//					// Test if there are any attributes, if not return
					if (offset == length)
						return sm;
					boolean msgIntegritySeen = false;
					boolean fingerPrintSeen = false;
					while(offset < length) {
						byte [] attrType = new byte [StunConstants.STUN_ATTRIBUTE_TYPE_LENGTH];
						System.arraycopy(packetBuf, offset, attrType, 
								0, StunConstants.STUN_ATTRIBUTE_TYPE_LENGTH);
						offset += StunConstants.STUN_ATTRIBUTE_TYPE_LENGTH;
						int attrLen = Conversion.getChar(packetBuf, offset);
						 //(char)((packetBuf[offset++]<<8) | (packetBuf[offset++]&0xFF));
						offset += StunConstants.STUN_LENGTH_LENGTH;
						int padding = StunAttribute.WORD_SIZE - (attrLen % StunAttribute.WORD_SIZE);
						if (padding == 4)
							padding = 0;
						//if ((attrLen % StunAttribute.WORD_SIZE) == 0) {
							byte [] value = new byte [attrLen]; 
							System.arraycopy(packetBuf, offset, value, 0, attrLen);
							try {
								StunAttribute attr = createAttribute(sm, attrType, value); // new StunAttribute(attrType, value);
								if (msgIntegritySeen && !(attr instanceof FingerPrint)) {
									logger.warn(PC2LogCategory.STUN, "Stack",
											"StunStack identified an attribute[" + sm.getName() 
											+ "] following the message integrity attribute.");
								}
								else if (fingerPrintSeen) {
									logger.warn(PC2LogCategory.STUN, "Stack",
											"StunStack identified an attribute[" + sm.getName() 
											+ "] following the fingerprint attribute.");
								}
								if (attr instanceof MessageIntegrity)
									msgIntegritySeen = true;
								if (attr instanceof FingerPrint) 
									fingerPrintSeen = true;
								sm.addAttribute(attr);
							}
							catch (IllegalArgumentException ia) {
								throw new StunException(ia.getMessage());
							}
						//}
						//offset += StunConstants.STUN_ATTRIBUTE_HEADER_LENGTH + attrLen + padding;
						offset += attrLen + padding;
					}
					
					if (offset != length) {
						throw new StunException("The packet length doesn't match the information parsed. Discarding packet.");
					}
					return sm;
				}
				catch (IllegalArgumentException ia) {
					throw new StunException(ia.getMessage());
				}
				
			}
			else 
				throw new StunException("The packet buffer does not contain the macic cookie in the header.");
		}
		else 
			throw new StunException("The packet buffer does not contain the macic cookie in the header.");

	}
	

	
	private void generateTransactionID() {
		Random random = new Random(System.currentTimeMillis());
		long lower = random.nextLong(); 
        System.arraycopy(StunConstants.STUN_MAGIC_COOKIE, 0, 
        		transactionID, 0, StunConstants.STUN_MAGIC_COOKIE_LENGTH);
        for(int i = 4; i < StunConstants.STUN_TRANSACTION_ID_LENGTH; i++) {
	           transactionID[i] = (byte)((lower >> (i*8))& 0xFFl);
	    }
	}
	
	public char getMessageType() {
		return this.type;
	}
	
	public void setMessageType(char type) throws IllegalArgumentException {
		this.type = type;
	}
	
	public int getLength() {
		return this.length;
	}
	
	public void setLength(int length) {
		this.length = length;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
//	private void assignName() {
//		if (Arrays.equals(messageType, StunConstants.BINDING_REQUEST)) 
//			this.name = StunConstants.BINDING_REQUEST;
//		else if (Arrays.equals(messageType, StunConstants.BINDING_RESPONSE))
//			this.name = StunConstants.BINDING_RESPONSE;
//		else if (Arrays.equals(messageType, StunConstants.BINDING_ERROR_RESPONSE))
//			this.name = StunConstants.BINDING_ERROR_RESPONSE;
//	}
	public byte [] getTransactionID() {
		return this.transactionID;
	}
	
	public void setTransactionID(byte [] id) throws IllegalArgumentException {
		if (id.length == StunConstants.STUN_FULL_TRANSACTION_ID_LENGTH) {
			System.arraycopy(id, 0, transactionID, 	0, id.length);
		}
		else {
			String errMsg = "The transaction id must be sixteen bytes in length.";
			throw new IllegalArgumentException(errMsg);
		}
			
	}
	
	public void setIgnoreHeader() {
		ignoreHdr = true;
	}
	
	public void addAttribute(StunAttribute attr) {
		attributes.add(attr);
		length += attr.size();
	}
	
	public StunAttribute getAttribute(String name) {
		ListIterator<StunAttribute> iter = attributes.listIterator();
		while (iter.hasNext()) {
			StunAttribute attr = (StunAttribute)iter.next();
			if (attr.getName().equals(name)) 
				return attr;
		}
		return null;
	}
	
	public StunAttribute getAttribute(char type) throws IllegalArgumentException {
//		if (type.length == 2) {
			ListIterator<StunAttribute> iter = attributes.listIterator();
			while (iter.hasNext()) {
				StunAttribute attr = (StunAttribute)iter.next();
				if (attr.getType() == type) 
					return attr;
			}
			return null;
//		}
//		else {
//			String errMsg = "The attribute type can only be two bytes in length.";
//			throw new IllegalArgumentException(errMsg);
//		}
	}
	
	public void removeAttribute(char type) throws IllegalArgumentException {
//		if (type.length == 2) {
			ListIterator<StunAttribute> iter = attributes.listIterator();
			boolean found = false;
			StunAttribute attr = null;
			while (iter.hasNext() && !found) {
				attr = (StunAttribute)iter.next();
				if (attr.getType() == type) {
					found = true;
				}
			}
			if (found) {
				// Since we found the attribute we need to remove.
				// Adjust the length of the message
				length -= attr.size();
				// Now remove it from the list of attributes
				attributes.remove(attr);
			}
//		}
//		else {
//			String errMsg = "The attribute type can only be two bytes in length.";
//			throw new IllegalArgumentException(errMsg);
//		}
	}
	public String toString() {
		String result = "STUN " + name + " messageType[" +
//		if (name != null) {
//			result += getName();
//		}
//		else {
//			result += 
				Conversion.hexString(type) 
//		}
//		result += 
			+ "]\n length=[" + length + "]\n transactionID=[" 
			+  Conversion.hexString(transactionID) 
			+ "] and " + attributes.size();
		if (attributes.size() == 1)
			result += " attribute.";
		else
			result += " attributes.";
		ListIterator<StunAttribute> iter = attributes.listIterator();
		while (iter.hasNext()) {
			StunAttribute attr = (StunAttribute)iter.next();
			result += "\n  " + attr.toString(); 
		}
		result += "\n";
		
		return result;
	}
}
