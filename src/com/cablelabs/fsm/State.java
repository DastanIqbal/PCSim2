/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.fsm;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

/**
 * This class defines the operations that can occur within a state in the 
 * finite state machine. It performs all of the actions operations that are
 * defined within a PC 2.0 Simulator XML document.
 * 
 * @author ghassler
 *
 */
public class State implements Cloneable {
	
	/**
	 * String name of state for printing
	 */
	protected String name;
	
	/**
	 * The FSM API to use for executing Actions from within the 
	 * state.
	 */
	protected FSMAPI api = null;
	
	/**
	 * The comparison evaluator to perform any comparison operations.
	 */
	protected ComparisonEvaluator compEval = null;
	
	/**
	 * A private counter specifying how many times this state has 
	 * been entered.
	 */
	protected int counter = 0;
	
	/**
	 * A global flag to disable all of the NoResponse timers in the
	 * platform.
	 */
	protected boolean disableNoResponseTimer = false;
	
	/**
	 * Logger
	 */
	protected LogAPI logger = LogAPI.getInstance(); // Logger.getLogger(FSM.class);
	
	/**
	 * The thread for this state's NoResponse timer.
	 */
	protected FSMNoResponseTask noResponseTask = null;
	 
	/**
	 * The state's NoResponse timer
	 */
	protected Timer noResponseTimer;
	
	/**
	 * The default interval for the NoResponse timer.
	 */
	protected int noResponseTimeout = 0;
	
	/**
	 * The FSM that owns this state.
	 */
	protected FSM owner = null;
	
	/**
	 * The actions to perform each time the state is entered.
	 */
	protected ActionFactory prelude;
	
	/**
	 * The actions to perform each time the state is being exited.
	 */
	protected ActionFactory postlude;
	
	/**
	 * The comparison operations to perform upon receipt of an event.
	 */
	protected Responses response;
	
	/**
	 * The thread of the internal state's timer.
	 */
	protected FSMTimerTask stateTimerTask = null;
	
	/**
	 * Whether the state is sleeping 
	 */
	protected boolean sleeping = false;
	
	/**
	 * Whether a state is suspended indefinitely 
	 * until user responds to prompt.
	 */
	protected boolean suspended = false;
	
	/**
	 * The thread for the state's sleep timer.
	 */
	protected FSMSleepTask sleepTask = null;
	
	/**
	 * The sleep timer.
	 */
	protected Timer slumber;
	
	/**
	 * The states internal timer.
	 */
	protected Timer stateTimer;
	
	/**
	 * The time to use for the internal timer
	 */
	protected int timeout = 0;
	
	/**
	 * The table of transactions for this state. The
	 * key is the event itself.
	 */
	protected Hashtable<String, Transition> transitions;

	/**
	 * The subcategory to use when logging
	 * 
	 */
	protected String subCat = null;

	protected final static String TIMER_REUSE = "REUSE";
	protected final static String TIMER_ONCE = "ONCE";
	protected final static String TIMER_PERSISTENT = "PERSISTENT";
	
	protected String stateTimerType = TIMER_PERSISTENT;
	/**
	 * Constructor
	 * @param name - the name of this state
	 * @param owner - the FSM that owns this state
	 */
	public State(String name, FSM owner) {
	 
		this.name = name;
		this.owner = owner;
//		this.logger = owner.getLogger();
		this.subCat = owner.getSubcategory();
		this.transitions = new Hashtable<String, Transition>();
		//transitionKeys = new LinkedList<String>();
	}
	
	/**
	 * Adds a transition to the table. Duplicates replace the previous
	 * transition in the table.
	 * @param t
	 */
	public void addTransition(Transition t) {
	   String key = t.getEvent().toUpperCase();
	   Transition transition = transitions.get(key);
	   if (transition != null) {
		   transition.setTo(t.getTo());
	   }
	   else {
		   transitions.put(key, t);
	   }
	}
	
	/**
	 * Gets the internal states counter
	 * @return
	 */
	public int getCounter() {
		return counter;
	}
	
	/**
	 * Gets the name of the state
	 * @return
	 */public String getName()
	{
		return name;
	}

	 /**
	 * Retrieves the name of the next state if it exists in the transition
	 * table
	 * @param key - the event that is triggering the transition
	 * @return the name of the next state or null.
	 */
	public String getNextState(String key) {
		Transition trans = findTransition(key);
		if (trans != null) {
			return trans.getTo();
		}
		return null;
	}
	
	/**
	 * Gets the preludes actions for the state
	 * @return
	 */
	public ActionFactory getPrelude() {
	 
		return prelude;
	}
	
	/**
	 * Gets the postlude actions for the state
	 * @return
	 */
	public ActionFactory getPostlude() {
		return postlude;
	}
	
	/**
	 * Gets the responses for the state
	 * @return
	 */
	 public Responses getResponse() {
		return response;
	}
	 
	 /**
	 * Gets the internal timer's timer interval.
	 * @return int
	 */
	 
	public int getTimeout() {
		return this.timeout;
	}
	 
	public Enumeration<Transition> getTransitions() {
		 return transitions.elements();
	 }

    protected Transition findTransition(String key) {
		String keyUpperCase = key.toUpperCase();
		Transition transition = transitions.get(keyUpperCase);

		if (transition != null) {
			return transition;
		}
		// Now try for a wildcard response code of <digit><digit>x
		else if (SIPConstants.isResponseType(key)) {
// BRKPT
//if (key.equalsIgnoreCase("180-Invite")) {
//	int glh = 0;
//	Enumeration keys = transitions.keys();
//	while (keys.hasMoreElements()) {
//		logger.error("\tTransition[" + i + "] =" + (String)keys.nextElement());
//	}
//}
			String wildcard = keyUpperCase.substring(0,2) + "X" + keyUpperCase.substring(3,keyUpperCase.length());
			transition = transitions.get(wildcard);
			if (transition != null)
				return transition;
			//	Next try for a wildcard response code of <digit>XX
			else {
				String wildcard2 = wildcard.substring(0,1) + "XX" + keyUpperCase.substring(3,keyUpperCase.length());
				transition = transitions.get(wildcard2);
				if (transition != null)
					return transition;
				// Next try for a wildcard response code of three X's
				else {
					String wildcard3 =  "XXX" + keyUpperCase.substring(3,keyUpperCase.length());
					transition = transitions.get(wildcard3);
					if (transition != null)
						return transition;
				}
			}
		}
		return null;
	}

	/**
	 * Initializes the state's interfaces and starts the 
	 * no response timer
	 * @param api
	 * @param ce
	 */
	public void init(FSMAPI api, ComparisonEvaluator ce, int noResponseTimeout) {
		this.api = api;
		if (this.noResponseTimeout == 0) {
			this.noResponseTimeout = noResponseTimeout + this.timeout;
		}
		this.compEval = ce;
		startNoResponseTimer();
	}
	
	/**
	 * Test whether the state is currently sleeping
	 * @return
	 */
	public boolean isSleeping() {
		return (sleeping || suspended);
	}
	
	/** 
	 * Processes each event for the state.
	 * 
	 * @param event - the current message event to process
	 * @param eventName - the name of the event to process
	 * 
	 * @return true if the event is processed, false otherwise
	 * @throws IllegalArgumentException
	 */
	public boolean processEvent(MsgEvent event) throws IllegalArgumentException {
		// First log when the timer is stopping if it is not the noResponseTimeout
		boolean failTest = false;
//		 BRKPT
//		if (event.getEventName().equalsIgnoreCase("200-INVITE")) {
//			int glh = 0;
//		}
//		if (name.equals("UE1WaitForNotify")) {
//			int glh = 0;
//		}
		
		if (!disableNoResponseTimer && noResponseTimer != null) 
			noResponseTimer.cancel();
		
		if (!disableNoResponseTimer && !event.getEventName().equals(TimeoutConstants.NO_RESPONSE_TIMEOUT)) {
			logger.debug(PC2LogCategory.FSM, subCat, 
					"Stopping state's(" + name + ") no response timer(" + noResponseTimer + ").");
			if (noResponseTimer != null)
				stopNoResponseTimer();
		}

		else
			failTest = true;

		// LOG USING LOGMSG category so that the user can't disable which
		// would break the trace tool.
		logger.info(PC2LogCategory.LOG_MSG, subCat, 
				"FSM (" + owner.getName() + ") - State (" + name + ") processing event (" 
				+ event.getEventName() + ") sequencer=" + event.getSequencer() + ".");
		
		if (response != null) {
			try {
				response.execute(api, compEval, event);
			}
			catch (PC2Exception pce) {
				logger.error(PC2LogCategory.FSM, subCat,
						"State " + name + " encountered an error when processing the event (" + 
						event.getEventName() + ").\n" + pce.getMessage() + "\n" + pce.getStackTrace());
				// If we get an exception while trying to perform our if testing 
				// declare the test a failure
				failTest = true;
			}
		}

		// Lastly test if the event is the noResponseTimer
		// Then we need to fail this test case
		if (failTest) {
			Result r = new Result(false);
			api.pass(r);
			// If the document didn't create a transition for this event,
			// add one to take the FSM to the END state.
			if (!(transitionExists(TimeoutConstants.NO_RESPONSE_TIMEOUT))) {
				Transition t = new Transition(this.name, "END", TimeoutConstants.NO_RESPONSE_TIMEOUT);
				addTransition(t);
			}
		}

		return true;
	}
	
	/**
	 * Performs all of the actions during the prelude processing
	 *
	 */
	public void processPrelude(int msgQueueIndex) {
		// First increment the state entered counter 
		counter++;
		logger.debug(PC2LogCategory.FSM, subCat,
				owner.getName() + " entering state (" + name + ") for the " + counter + " time.");
// BRKPT
//		if (name.equals("DUTOffHook")) {
//		int glh = 0;
//	}
		// Next see if the state timer timeout has a value greater than zero
		if (timeout > 0 && 
				((stateTimerType.equals(TIMER_ONCE) || 
						stateTimerType.equals(TIMER_PERSISTENT)
						&& counter == 1) ||
				(stateTimerType.equals(TIMER_REUSE) && stateTimer == null))) {
			stateTimer = new Timer(name + ":stateTimer", true);
			stateTimerTask = new FSMTimerTask(owner, this);
			stateTimer.schedule(stateTimerTask, timeout);
			logger.debug(PC2LogCategory.FSM, subCat,
					"Starting state timer(" +stateTimer + ") for " + 
					timeout + " msecs.");
		}

		if (prelude != null) {
			try {
				
				prelude.executeActions(api, msgQueueIndex);
			}
			catch (PC2Exception e) {
				logger.warn(PC2LogCategory.FSM, subCat,
						"State encountered exception on an action during prelude processing.");
			}
		}
		else {
			logger.debug(PC2LogCategory.FSM, subCat,
					"State " + name + " doesn't contain any prelude actions.");
		}
	}

	/**
	 * Executes all of the actions during postlude processing
	 *
	 */
	public void processPostlude(int msgQueueIndex) {
		if (postlude != null) {
			try {
				postlude.executeActions(api, msgQueueIndex);
			}
			catch (PC2Exception e) {
				logger.warn(PC2LogCategory.FSM, subCat,
						"State encountered exception on an action during postlude processing.");
			}
		}
		else {
			logger.debug(PC2LogCategory.FSM, subCat,
					"State " + name + " doesn't contain any postlude actions.");
			
		}
		// To eliminate any chance of a mistake, stop no response timer
		if (noResponseTimer != null)
			stopNoResponseTimer();
	}

	public Transition removeTransition(String event) {
		String key = event.toUpperCase();
//		Enumeration keys = transitions.keys();
//		int i = 0;
//		while (keys.hasMoreElements()) {
//			String key = (String)keys.nextElement();
//			if (key.equals(event))
//				System.out.println("Key(" + i + ")" + keys.nextElement());
//			i++;
//		}
//		System.out.println();
		return transitions.remove(key);
	}
	/**
	 * Disables the NoResponseTimer from being used.
	 *
	 */
	public void setDisableNoResponseTimer() {
		this.disableNoResponseTimer = true;
	}

	/**
	 * Sets the owner of this FSM
	 */
	protected void setOwner(FSM fsm) {
		this.owner = fsm;
	}
	
	public FSM getOwner() {
	    return owner;
	}
	
	/**
	 * Sets the actions to perform upon entering the state.
	 * @param p
	 */
	public void setPrelude(ActionFactory p) {
		this.prelude = p;
	}
	
	/**
	 * Sets the actions to perform when exiting a state.
	 * @param p
	 */
	public void setPostlude(ActionFactory p) {
		this.postlude = p; 
	}
	
	/**
	 * Sets the response operations to perform in this state.
	 * @param r
	 */
	public void setResponses(Responses r) {
		this.response = r;
	}
	
	public void setStateTimerType(String type) {
		this.stateTimerType = type;
	}
	
	/**
	 * Sets the internal timer's timer interval.
	 * @param time
	 */
	public void setTimeout(int time) {
		this.timeout = time;
	}

	public void resume() {
		 startNoResponseTimer();
		 suspended = false;
	}
	/**
	 * Terminates the state from sleeping.
	 *
	 */
	public void sleepCancel() {
		if (!sleeping)
			logger.warn(PC2LogCategory.FSM, subCat,
					"State machine received sleep timeout event in another state.");
		
		logger.debug(PC2LogCategory.FSM, subCat,
					"State (" + name + ") awakening from sleep.");
		sleeping = false;
	}
	
	/**
	 * Starts the NoResponse timer.
	 *
	 */
	public void startNoResponseTimer() {
		// Next start the NoResponse timer
		if(!disableNoResponseTimer) {
			if (noResponseTimer == null) {

				noResponseTimer = new Timer(name + ":NoResponseTimer", true);
				
				noResponseTask = new FSMNoResponseTask(owner, this, noResponseTimeout);
				noResponseTimer.schedule(noResponseTask, noResponseTimeout);
				logger.debug(PC2LogCategory.FSM, subCat,
						"Starting no response timer(" + noResponseTimer + ") for " + 
						noResponseTimeout + " msecs. in " + name + ". TimerTask(" + noResponseTask + ")");
			}
		}
		else
			logger.debug(PC2LogCategory.FSM, subCat,
					"No response timer disabled msecs. in " + name);
	}
	
	/**
	 * Starts the NoResponse timer.
	 *
	 */
	private void stopNoResponseTimer() {
		if (noResponseTimer != null) {
			noResponseTimer.cancel();
			logger.debug(PC2LogCategory.FSM, subCat,
					"Stopping no response timer(" + noResponseTimer + ") for " + 
					noResponseTimeout + " msecs. in " + name + ". TimerTask(" + noResponseTask + ")");
			noResponseTimer = null;
//			if (noResponseTask != null) {
//				noResponseTask.cancel();
//				noResponseTask = null;
//			}
		}
		if (noResponseTask != null) {
			noResponseTask.cancel();
			noResponseTask = null;
		}
	}

	/**
	 * Start's the state to sleep for the specified time interval
	 * 
	 * @param time - the time to sleep
	 * @return true
	 */
	public boolean startSleeping(int time) {
		if (time > 0) {
			logger.debug(PC2LogCategory.FSM, subCat,
					"State (" + name + ") starting to sleep for " + time + " msecs.");
			// We need to extend the No Response Timer to include the sleep time
			stopNoResponseTimer();
			int temp = noResponseTimeout;
			noResponseTimeout = time + temp;
			startNoResponseTimer();
			sleeping = true;
			try {
				 Thread.sleep(time);
				 sleeping = false;
				 return true;
			 }
			 catch (InterruptedException ie) {
				 return false;
			 }
		}
		else if (time == -1) {
			logger.debug(PC2LogCategory.FSM, subCat,
					"State (" + name + ") starting to sleep until user responds.");
			stopNoResponseTimer();
			suspended = true;
			
		}
		
		return isSleeping();
	}

	/**
	 * Stops any timers that may be running in the State
	 *
	 */
	public void stopAllTimers() {
		if (stateTimer != null) {
			stateTimer.cancel();
			stateTimer = null;
			if (stateTimerTask != null) {
				stateTimerTask.cancel();
				stateTimerTask = null;
			}
		}
		if (slumber != null) {
			slumber.cancel();
			slumber = null;
			if (sleepTask != null) {
				sleepTask.cancel();
				sleepTask = null;
			}
		}
		stopNoResponseTimer();
	}
	/**
	 * Stops the internal state timer
	 *
	 */
	public void stopStateTimer() {
		if (stateTimer != null) {
			stateTimer.cancel();
			stateTimer = null;
			if (stateTimerTask != null) {
				stateTimerTask.cancel();
				stateTimerTask = null;
			}
		}
	}
	
	/**
	 * Tests whether a transition exists for a given event.
	 * 
	 * @param key - the name of the event to search in the table for.
	 * @return - true if a transition exists in the table, false otherwise
	 */
	public boolean transitionExists(String key) {
		Transition trans = findTransition(key);
		
		if (trans != null)
			return true;
		return false;
	}
	
	/**
	 * Converts the UIDs of the state from the original values that
	 * were set during the cloning to that of a new FSM.
	 * @param newUID
	 * @param origUID
	 */
	protected void updateUID(int newUID, int origUID) {
		if (prelude != null)
			prelude.updateUIDs(newUID, origUID);
		if (postlude != null)
			postlude.updateUIDs(newUID, origUID);
		if (response != null)
			response.updateUIDs(newUID, origUID);
	}
	/**
	 * Determines if the timer is owned by this state or 
	 * if another state forgot to stop the timer.
	 * 
	 * @param tt - the timer thread to test
	 * @param owner - the name of the state that created the timer.
	 * 
	 * @return - true if this state owns the timer thread, false otherwise 
	 */
	public boolean validTimerTask(TimerTask tt, State owner) {
		if (this.getClass().equals(owner.getClass()) && 
				(tt == stateTimerTask || tt == noResponseTask || tt == sleepTask)) {
			return true;
		}
		logger.warn(PC2LogCategory.FSM, subCat,
				"State received timer expired notice in state(" + name + 
				") that was created in state(" + owner.getName() + ").");
		tt.cancel();
		return false;
	}
	
	/**
	 * Creates a string representation of the container and its
	 * contents.
	 */
	@Override
	public String toString() {
		String result = "\nstate name=" + name; 
		if (timeout > 0)
			result += " timer=" + timeout + " msec.";
		if (prelude != null) {
			result += "\nprelude:\n" + prelude.toString();
		}
		
		if (response != null) {
			result += "\nresponse:" + response.toString();
		}

		if (postlude != null) {
			result += "\npostlude:\n" + postlude.toString();
		}
		
		if (transitions.size() > 0) {
			result += "\ntransitions: ";
				Enumeration<Transition> elements = transitions.elements();
				while (elements.hasMoreElements()) {	
					Transition t = elements.nextElement();
					if (t != null)
						result += "\n      on event " + t.getEvent() + " go to " + t.getTo();
				}
		}
		
		result += "\n";
		
		return result;
		
	}

// This method was added as a validator for cloning
//	public String me() {
//		String result = super.toString() + " " + name + "\n";
//		if (prelude != null) 
//			result += prelude.me();
//		if (postlude != null)
//			result += postlude.me();
//		if (response != null) 
//			result += response.me();
//		return result;
//		
//	}
	/** This implements a deep copy of the class for replicating 
	 * FSM information.
	 * 
	 * NOTE: After cloning a State, the invoker will need to set the
	 * FSM that owns this instance of the class for it to function
	 * properly.
	 *  
	 * @throws CloneNotSupportedException if clone method is not supported
	 * @return Object
	 */ 
	@Override
	public Object clone() throws CloneNotSupportedException {
		State retval = (State)super.clone();
		if (retval != null) {
			if (this.name != null)
				retval.name = new String(this.name);
			if (this.api != null)
				retval.api = this.api;
			if (this.compEval != null)
				retval.compEval = this.compEval;
			retval.disableNoResponseTimer = this.disableNoResponseTimer;
			retval.logger = LogAPI.getInstance(); //Logger.getLogger(FSM.class);
			retval.noResponseTimeout = this.noResponseTimeout;
			if (this.prelude != null) {
				retval.prelude = (ActionFactory)this.prelude.clone();
				if (retval.prelude.actions != null) {
					ListIterator<Action> iter = retval.prelude.actions.listIterator();
					while (iter.hasNext()) {
						Action a = iter.next();
						if (a instanceof Verify) {
							((Verify)a).setState(retval);
						}
					}
				}
			}
			if (this.postlude != null) {
				retval.postlude = (ActionFactory)this.postlude.clone();
				if (retval.postlude.actions != null) {
					ListIterator<Action> iter = retval.postlude.actions.listIterator();
					while (iter.hasNext()) {
						Action a = iter.next();
						if (a instanceof Verify) {
							((Verify)a).setState(retval);
						}
					}
				}
			}
			if (this.response != null) {
				retval.response = (Responses)this.response.clone();
				if (retval.response.operations != null) {
					ListIterator<ActiveOp> iter = retval.response.operations.listIterator();
					while (iter.hasNext()) {
						ActiveOp a = iter.next();
						if (a instanceof Verify) {
							((Verify)a).setState(retval);
						}
					}
				}
			}
			retval.sleeping = false;
			retval.subCat = new String(this.subCat);
			if (this.transitions != null) {
				retval.transitions = new Hashtable<String, Transition>();
				Enumeration<Transition> ts = this.transitions.elements();
				while (ts.hasMoreElements()) {
					Transition t = ts.nextElement();
					Transition newTran = (Transition)t.clone();
					retval.transitions.put(newTran.getEvent().toUpperCase(), newTran);
				}
			}
			retval.timeout = this.timeout;
						
		}
		return retval;
	}
}
