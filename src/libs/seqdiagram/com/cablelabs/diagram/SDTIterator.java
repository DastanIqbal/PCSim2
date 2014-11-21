package com.cablelabs.diagram;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

/**
 * A helper class to correctly iterate over the events contained in the Configuration objects of a SDT
 * based on the currently selected element in the tree.
 *
 * @author rvail
 *
 */
public class SDTIterator implements Iterator<Event> {

    private Iterator<Event> hiddenIter = null;
    private LinkedList<Event> selectedEvents = null;

    SDTIterator(Map<String, Configuration> configs, Configuration selectedConfig, Test selectedTest, String selectedFSM) {

        selectedEvents = new LinkedList<Event>();

        Iterator<String> configKeyIter = configs.keySet().iterator();
        while(configKeyIter.hasNext()) {
            String configKey = configKeyIter.next();
            Configuration config = configs.get(configKey);
            if (selectedConfig != null && !selectedConfig.equals(config)) continue;

            LinkedList<Event> configEvents = new LinkedList<Event>();
            
            Iterator<Test> testIter = config.getTestsListIterator();
            while (testIter.hasNext()) {
                Test test = testIter.next();
                if (selectedTest != null && !selectedTest.equals(test)) continue;

                HashMap<String, EventList>fsms = test.getFSMsTable();
                Iterator<String> keys = fsms.keySet().iterator();
                while (keys.hasNext()) {
                    String fsm = keys.next();
                    if (selectedFSM != null && !selectedFSM.equals(fsm)) continue;

                    EventList evtLst = fsms.get(fsm);
                    ListIterator<Event> evtIter = evtLst.listIterator();
                    while (evtIter.hasNext()) {
                        Event e = evtIter.next();
                        configEvents.add(e);
                    }
                }
            }
           
            // each configuration needs to be sorted but the events for each configuration should not be intermingled
            Collections.sort(configEvents);
            selectedEvents.addAll(configEvents);
        }

        hiddenIter = selectedEvents.iterator();
    }

    @Override
    public boolean hasNext() {
        return hiddenIter.hasNext();
    }

    @Override
    public Event next() {
        return hiddenIter.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
