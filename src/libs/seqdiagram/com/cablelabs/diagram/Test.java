package com.cablelabs.diagram;

import java.util.HashMap;

public class Test implements Comparable<Test> {

    private HashMap<String, Event> sequenceMap = new HashMap<String,Event>();
    
	protected long endTime = 0;
	// This table contains all of the messages for a single FSM during
	// a test. The key is the name of the FSM.
	protected HashMap<String, EventList> fsms = null;
	protected String name = null;
	protected String path = null;
	protected String separator = null;
	protected long startTime = 0;
	protected int startSequencer = Integer.MAX_VALUE;
	
	protected Test(String name, String path, String separator, long start) {
		this.name = name;
		this.path = path;
		this.separator = separator;
		this.startTime = start;
		this.fsms = new HashMap<String, EventList>();
	}

	public void addEvent(String fsm, Event event) {
	    if (startSequencer > event.getSequenceInt()) {
	        startSequencer = event.getSequenceInt();
	    }
	    
	    event.setTest(this);
	    event.setFsm(fsm);
	    
		EventList ml = fsms.get(fsm);
		if (ml == null) {
			ml = new EventList();		
			fsms.put(fsm, ml);
		}
		ml.addEvent(event);
		
		Event other = sequenceMap.get(event.getSequence());
		if (other != null) {
		    System.err.println("Error: Multiple events in test(" + name + ") have the same sequence number(" + event.getSequence() + ") unexpected behavior may occur.");
		}

		sequenceMap.put(event.getSequence(), event);
	}

	public long getDuration() {
		return (this.endTime-this.startTime);
	}

	public EventList getEventList(String fsm) {
		return fsms.get(fsm);
	}

	public HashMap<String, EventList> getFSMsTable() {
		return fsms;
	}

	public String getName() {
		return this.name;
	}

	public String getPath() {
		return this.path;
	}

//	public ListIterator getList(String key) {
//		return list.getList(key).listIterator();
//	}

	public String getPathAndName() {
		return this.path + this.separator + this.name;
	}

	public long getRelativeEnd() {
		return endTime;
	}

	public long getRelativeStart() {
		return startTime;
	}

	public void setRelativeEnd(long end) {
		this.endTime = end;
	}

    @Override
    public int compareTo(Test other) {
        
        long diff = this.startTime - other.startTime;
        return Long.signum(diff);
    }
}
