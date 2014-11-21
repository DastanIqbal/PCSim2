package com.cablelabs.diagram;

import java.util.LinkedList;
import java.util.ListIterator;

public class EventList {
	private LinkedList<Event> events = new LinkedList<Event>();

	public void addEvent(Event event) {
	    int i = 0;
	    for (; i < events.size(); i++) {
	        Event e = events.get(i);
	        if (e.compareTo(event) > 0) {
	            break;
	        }
	    }
	    events.add(i, event);
		//events.add(event);
	}

	public Event getEvent(int index) {
		if (events.size() >= index && index > -1) {
			return events.get(index);
		}
		return null;
	}

	public LinkedList<Event> getList() {
		return events;
	}

	public ListIterator<Event> listIterator() {
		return events.listIterator();
	}
}
