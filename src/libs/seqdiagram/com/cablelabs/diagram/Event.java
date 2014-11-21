package com.cablelabs.diagram;

public class Event implements Comparable<Event> {

	public static final String ADDR_DELIMITER = "|";

	private String firstLine = null;
	private String from = null;
	private String fromIP = null;
	private String message = null;
	private Long offset = 0L;
	private String protocol = null;
	// The value sent is based upon the perspective of the platform.
	private boolean sent = false;
	private String seq = null;
	private Integer seqI = null;
	private Long timestamp = 0L;
	private String timestampStr = null;
	private String to = null;
	private String toIP = null;

	Configuration parentConfig;
	Test test;
	String fsm;

	public Event(String to, String from, String seqNo, String firstLine,
			String message, Long timestamp, String timestampStr, Long offset, boolean sent,
			Configuration parentConfig) {

	    if (to == null) {
	        throw new IllegalArgumentException("to can not be null");
	    }
	    if (from == null) {
            throw new IllegalArgumentException("from can not be null");
        }
	    
		this.to = to;
		this.from = from;
		this.seq = seqNo;
		this.seqI = Integer.parseInt(seqNo);
		this.firstLine = firstLine;
		this.message = message;
		this.timestamp = timestamp;
		this.timestampStr = timestampStr;
		this.offset = offset;
		this.sent = sent;
		this.parentConfig = parentConfig;

		int index = to.indexOf(Event.ADDR_DELIMITER);
		if (index != -1) {
			this.toIP = to.substring(0, index);
		}
		index = from.indexOf(Event.ADDR_DELIMITER);
		if (index != -1) {
			fromIP = from.substring(0, index);
		}
	}

	public String getFirstLine() {
		return firstLine;
	}


	public String getFrom() {
		return from;
	}


	public String getFromIP() {
		return fromIP;
	}

	public String getMessage() {
		return message;
	}

	public Long getOffset() {
		return offset;
	}

	public Configuration getParentConfig() {
	    return parentConfig;
	}

	public String getProtocol() {
		return protocol;
	}


	public String getSequence() {
		return seq;
	}
	
	public Integer getSequenceInt() {
	    return seqI;
	}

	public Long getTimestamp() {
		return timestamp;
	}

	public String getTimeStampStr() {
	    return timestampStr;
	}

	public String getTo() {
		return to;
	}

	public String getToIP() {
		return toIP;
	}

	public boolean isSender() {
		return sent;
	}
    
    public Test getTest() {
        return test;
    }

    
    public void setTest(Test test) {
        this.test = test;
    }

    public String getFsm() {
        return fsm;
    }

    public void setFsm(String fsm) {
        this.fsm = fsm;
    }

    @Override
    public int compareTo(Event other) {
        if (this.timestamp == other.timestamp) {
            int diff = this.seqI - other.seqI; 
            return (diff > 0 ? 1: (diff == 0 ? 0 : -1));
        } else {
            long diff = this.timestamp - other.timestamp;
            return (diff > 0 ? 1: (diff == 0 ? 0 : -1));
        }
    }
    
    @Override
    public String toString() {
        return firstLine;
    }
}
