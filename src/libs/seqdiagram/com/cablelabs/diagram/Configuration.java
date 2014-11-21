package com.cablelabs.diagram;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;

public class Configuration {

	public static final int CONFIG_POS = 1;

	public static final int EVENT_POS = 4;


	public static final int FSM_POS = 3;

	public static final int LOG_POS = 0;

	public static final int TEST_POS = 2;

	public static final int TREE_DEPTH = 5;
	
	public static final String UNKNOWN_DUT = "Unknown";

	private String logMessage = null;

	/**
	 * A mapping from ip|port to the platform socket
	 * example "10.2.0.32|5060" => "SIP UDP"
	 */
	private HashMap<String,String> platformIPs = null;
	private Actor platform;
	
	private HashMap<String,Actor> ipToActor = new HashMap<String,Actor>();
	private HashMap<String,Actor> actors = new HashMap<String,Actor>(); // Maps NELabel to Actor
	
	/**
	 * A list of ip port pairs that the platform
	 * is currently using to conduct tests.
	 */
	private LinkedList<String> platformSockets = null;

	protected Test curTest = null;

	/**
	 * This is the relative time to the start of
	 * the file that this configuration
	 * file stopped being used.
	 */
	protected long endTime = 0;
	/**
	 * The name of this configuration file used
	 * during this set of tests.
	 */
	protected String name = null;
	/**
	 * The path to the configuration file used during
	 * this set of tests.
	 */
	protected String path = null;
	protected String separator = File.separator;
	/**
	 * This is the relative time to the start of
	 * the file that this configuration file
	 * started being used.
	 */
	protected long startTime = 0;
	protected LinkedList<Test> tests = null;

	protected Configuration(String line, long timeStamp, LinkedList<String> sockets, String configFilePath) {
		this.platformSockets = sockets;
		this.tests = new LinkedList<Test>();
		this.startTime = timeStamp;
		this.logMessage = line;

		if (UNKNOWN_DUT.equals(configFilePath)) {
		    name = configFilePath;
		    path = "";
		} else {
		    File f = new File (configFilePath);
            name = f.getName();
            path = f.getPath().substring(0, (f.getPath().length() - (name.length()+ 1)));
		}
        updatePlatformIPs();
	}

	public long getDuration() {
		return (this.endTime - this.startTime);
	}

	public Event getEvent(int testIndex, String fsm, int eventIndex) {
		Test t = tests.get(testIndex);
		if (t != null) {
			EventList el = t.getEventList(fsm);
			if (el != null) {
				return el.getEvent(eventIndex);
			}
		}

		return null;
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public String getPathAndName() {
		return path + separator + name;
	}

	public Test getTest(String testName) {
		ListIterator<Test> iter = tests.listIterator();
		while (iter.hasNext()) {
			Test t = iter.next();
			if (t.getName().equals(testName)) {
				return t;
			}
		}
		System.err.println("Configuration could not find a Test with the name("
				+ testName +").");
		return null;
	}

	public Test getTestByIndex(int index) {
	    if (index < 0 || index >= tests.size()) return null;

	    return tests.get(index);
	}

	public void setPlatformSockets(LinkedList<String> sockets) {
	    this.platformSockets = sockets;
	    updatePlatformIPs();
	}
	private void updatePlatformIPs() {
	    platformIPs = new HashMap<String,String>();
	    platform = new Actor(SDTCanvas.PLATFORM);
	    
	    for (String sock : platformSockets) {
	        String[] parts = sock.split(" ", 3);
	        String ip = parts[2];
	        platform.addInterface(ip);
	        
	        String prot = parts[0] + " " + parts[1];
	        if (platformIPs.containsKey(ip)) {
	            prot = platformIPs.get(ip) + ", " + prot;
	        }
	        platformIPs.put(ip, prot);
	    }

    }

	protected void addEvent(String fsm, Event event) {
		if (curTest != null) {
			curTest.addEvent(fsm, event);
		}
	}

	protected void complete(long timeStamp) {
		endTime = timeStamp;
	}

	protected void endTest(long end) {
		if (curTest != null) {
			curTest.setRelativeEnd(end);
			curTest = null;
		}
	}
	protected ListIterator<Test> getTestsListIterator() {
		return tests.listIterator();
	}

	protected boolean isPlatformAddress(String addr) {
	    if (platformIPs == null) return false;
	    return (platformIPs.get(addr) != null);
	}

	protected void startTest(String testName, long start) {
		if (curTest != null) {
		    endTest(start);
		    System.err.println("Error Starting new test(" + testName + " when previous test(" + curTest.getName() + ") has not been finished!");
		}

		File f = new File (testName);
		String testFileName = f.getName();
		String testPath = f.getPath().substring(0, (f.getPath().length() - (testFileName.length()+ 1)));
		curTest = new Test(testFileName, testPath, separator, start);
		tests.add(curTest);
		Collections.sort(tests);
	}

    public String getLogMessage() {
        return logMessage;
    }

    public void updateNE(String ne, ArrayList<String> ips) {        
        Actor actor = actors.get(ne);
        if (actor == null) {
            actor = new Actor(ne);
            actors.put(ne, actor);
        }
        
        for (String sock : ips) {
            String[] parts = sock.split(" ", 2);
            String ip = null;
            if (parts.length == 1) {
                ip = parts[0];
            } else if (parts.length == 2) {
                ip = parts[1];
            } else {
                continue;
            }
            //platform.addInterface(ip);
            
            ipToActor.put(ip, actor);
            actor.addInterface(ip);
        }    
    }

    public Actor getActor(String ipPort) {
        if (platform != null && platformIPs.containsKey(ipPort)) {
            return platform;
        }
        return ipToActor.get(ipPort);
    }

}
