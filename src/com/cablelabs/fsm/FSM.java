/*h
###########################h###########################################################
##                                                                                  ##
## (c) 2006-2012 Cable Televis.ision Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.fsm;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.cablelabs.log.PC2LogCategory;
import com.cablelabs.log.LogAPI;

/**
 * The PC 2.0 Simulator XML documents finite state machine implementation.
 * 
 * @author ghassler
 *
 */
public class FSM implements Cloneable {

	/**
	 * A static counter to uniquely identify each FSM created in the system
	 * for event delivery from a distributor.
	 */
	private static int uidCounter = 0;

	/** 
	 * This FSM's unique identifier.
	 */
	private int uid = 0;

	/**
	 * The name of this FSM. This value is also used as the subcategory
	 * for logging so that a test can limit logging of an FSM to only
	 * one of them in the test.
	 */

	private String name = null;

	/**
	 * Initial state for the state machine
	 */
	private String initState = null;

	/**
	 * Previous state for the state machine
	 */
	private State prevState = null;

	/**
	 * The current state of the state machine
	 */
	private State curState = null;

	/**
	 * Container for all of the states of this FSM.
	 */
	private Hashtable<String, State> states;

	/**
	 * The provider all the Actions that the FSM may need to
	 * have performed during execution.
	 */
	private FSMAPI api = null;

	/**
	 * The class to perform all of the comparison evaluations
	 * required by this FSM.
	 */
	private ComparisonEvaluator compEval = null;

	/**
	 * A queue of events awaiting processing by the FSM.
	 */
	//private LinkedList<MsgEvent> queue;

	private ConcurrentLinkedQueue<MsgEvent> queue =  null; // new ConcurrentLinkedQueue<MsgEvent>();
	
	/**
	 * Logger
	 */
	private LogAPI logger = LogAPI.getInstance();

	/**
	 * Performs all of the reference point processing upon each
	 * event received by the platform.
	 */
	private ReferencePointsFactory rpf = null;

	/**
	 * A table of all the FSM in the system by name for quick lookup
	 */
	private static Hashtable<String,FSM> FSMs = null; 

	/**
	 * The current model the FSM is attempting to operate as.
	 */
	private Model model = null;

	/**
	 * The network elements that the FSM is currently simulating
	 */
	private NetworkElements nes = null;

	/**
	 * The services that this fsm supports
	 */
	private LinkedList<String> services = new LinkedList<String>();
	/**
	 * A local variable to hold a utility message for use in multiple 
	 * actions created from a single utility event.
	 */
	private UtilityMsg lastUtilMsg = null;

	/**
	  * The name of the Stack to use when sending
	  * a message from a distributor that contains 
	  * multiple IPs. Each stack is associated with one
	  * and only one IP address for a given protocol.
	  * If the value is not set, the distributor will
	  * use the default stack name. Defining the stack
	  * name in an individual message, takes precedence
	  * over that defined by the default
	  * for the system.
	  */
	private String sipStack = null;
	
	/**
	  * The name of the Stack to use when sending
	  * a message from a distributor that contains 
	  * multiple IPs. Each stack is associated with one
	  * and only one IP address for a given protocol.
	  * If the value is not set, the distributor will
	  * use the default stack name. Defining the stack
	  * name in an individual message, takes precedence
	  * over that defined by the default
	  * for the system.
	  */
	private String diameterStack = null;
	
	/**
	 * The entry in the MsgQueue that is currently being processed
	 */
	private int msgQueueIndex = -1;

	/**
	 * The defaultNoResponseTimeout. This is either the value that 
	 * is in the settings or the one assigned by the model through
	 * the setDefaultNoResponseTimeout method. Each state is given
	 * this value and it adds any processing timeout values of its own
	 * to this value for the final value.
	 */
	private int defaultNoResponseTimeout = 0;
	
	/**
	 * This is a table of all of the ungrouped verify tests that 
	 * this FSM must make in order for the test to pass.
	 */
	private LinkedList<Verify> ungroupedTests = new LinkedList<Verify>();
	
	/**
	 * This is a table of all of the ungrouped prompting log messages that require
	 * the user to answer a log message with the verify flag set to
	 * true. 
	 */
	private LinkedList<LogMsg> ungroupedVerifyLogMsgs = new LinkedList<LogMsg>();
	
	/**
	 * This is a table of all of the grouped verify tests that 
	 * this FSM must make in order for the test to pass. Grouped
	 * verify tests are grouped because only one of the tests with
	 * the group must pass in order for the test to succeed.
	 */
	private static Hashtable<String, LinkedList<Verify>> groupedTests = new Hashtable<String, LinkedList<Verify>>();

	/**
	 * This is a table of all of the grouped prompting log messages that require
	 * the user to answer a log message with the verify flag set to
	 * true. 
	 */
	private static Hashtable<String, LinkedList<LogMsg>> groupedVerifyLogMsgs = new Hashtable<String, LinkedList<LogMsg>>();
	
	private FSMListener listener = null;
	/**
	 * This class creates a new finite state machine.  
	 */
	public FSM(String name) {
	logger.debug(PC2LogCategory.Model, " ", "CREATING FSM - " + name);
	this.name = name;
		if (FSMs == null) {
			FSMs = new Hashtable<String, FSM>();
		}
		FSMs.put(name, this);
		ENDState end = new ENDState(this);
		states = new Hashtable<String, State>();
		states.put(end.getName(), end);
//		queue = new ConcurrentLinkedQueue<MsgEvent>(); // new LinkedList<MsgEvent>();
//		logger = new LogAPI();   // Logger.getLogger(FSM.class);
		rpf = new ReferencePointsFactory(this);
		this.uid = ++uidCounter;
		if (this.defaultNoResponseTimeout == 0) {
			this.defaultNoResponseTimeout = SystemSettings.getNoResponseTimeout();
		}
	}

	 /**
	  * Adds a new state to the FSM. 
	  * 
	  * @param key - the new state to add.
	  * @throws PC2Exception - when the name of the state matches another state
	  * 		within the table.
	  */
	 public void addState(State key) throws PC2Exception {
		 State s = getState(key.getName());
		 if (s == null) {
			 states.put(key.getName(), key);
		 }
		 else {
			 String msg = new String("state tag requires an unique ID attribute," +
					 " parser found duplicate ID attribute " + key.getName() + " in fsm " + name);
			 throw new PC2Exception(msg);
		 }

	 }
	 
	 /**
	  * Adds a transition to a specific state.
	  * @param from - the state to add the transition.
	  * @param to - the state to move to upon the event.
	  * @param event - the event that triggers the moving of the from-to states
	  * 
	  */
	 public void addTransition(String from, String to, String event) {
		 Transition t = new Transition(from, to, event);
		 State s = getState(from);
		 s.addTransition(t);
	 }
	 
	 public void addVerify(Verify v) {
		 if (v.group != null) {
			 LinkedList<Verify> list = groupedTests.get(v.group);
			 if (list == null) {
				 list = new LinkedList<Verify>();
				 groupedTests.put(v.group, list);
			 }
			 list.add(v);
		 }
		 else 
			 ungroupedTests.add(v);
	 }
	 
	 public void addVerifyLogMsg(LogMsg lm) {
		 if (lm.group != null) {
			 LinkedList<LogMsg> list = groupedVerifyLogMsgs.get(lm.group);
		 	 if (list == null) {
		 		 list = new LinkedList<LogMsg>();
		 		groupedVerifyLogMsgs.put(lm.group, list);
		 	 }
		 	 list.add(lm);
		 }
		 else 
			 ungroupedVerifyLogMsgs.add(lm);
		
	 }
	 /**
	  * Performs the operation of changing from one state to another. It invokes
	  * the postlude operations of the current state, marks the test case as a
	  * failure if the event doesn't contain a required transition, sets the 
	  * FSMs previous state attribute, sets the current state to the new state
	  * specified by the transition and invokes the prelude operation on the new
	  * state.
	  * 
	  * @param eventName - the event just processed by the FSM.
	  */
	 protected void changeStates(String eventName) {
		 State nextState = null;
		 if (curState.transitionExists(eventName)) {
			 String nextStateName = curState.getNextState(eventName);
			 nextState = states.get(nextStateName);
		 }
		 else if (!(TimeoutConstants.isTimeoutEvent(eventName) || 
				 EventConstants.isEvent(eventName) || 
				 ReferencePointConstants.isReferencePointEvent(eventName))){
			 logger.error(PC2LogCategory.FSM, name, 
					 "FSM(" + name + ") no transition exists for state(" + curState.getName() + 
					 ") on event(" + eventName + "). Declaring test case failure.\n");
			 Result r = new Result(false);
			 api.pass(r);
		 }

		 // Next we need to test for reference points again

		 // Call to execute postlude operations
		 if (nextState != null) {
			 curState.processPostlude(msgQueueIndex);

			 logger.info(PC2LogCategory.FSM, name, 
					 "FSM (" + name + ") leaving State(" + curState.getName() + ").");
			 prevState = curState;
			 if (nextState != curState) {
				 curState.stopStateTimer();
			 }
			 else if (curState.stateTimerType.equals(State.TIMER_ONCE)) {
				 curState.stopStateTimer();
			 }
			 curState = nextState;

			 // Set the API for the current state and then enter it by
			 // calling the prelude method.
			 //curState.setAPI(api);
			 //curState.setComparisonEvaluator(compEval);
			 logger.info(PC2LogCategory.FSM, name, 
					 "FSM (" + name + ") entering State(" + curState.getName() + ").");
			 //curState.startNoResponseTimer();
			 curState.init(api,compEval, defaultNoResponseTimeout);
			 curState.processPrelude(msgQueueIndex);
		 }
		 else
			 curState.startNoResponseTimer();
	 }

	 /**
	  * Clears the local utility message variable.
	  *
	  */
	 public void clearLastUtilityMsg() {
		 lastUtilMsg = null;
	 }
	 
	 /**
	  * Sets the comparison operations implementation class
	  * for this FSM.
	  * @param ce
	  */
	 public ComparisonEvaluator getComparisonEvaluator() {
		 return this.compEval;
	 }
	/**
	 * Retrieves the number of times that state has been 
	 * entered.
	 * @return
	 */
	 public String getCurrentStateCount() {
		 if (curState != null) 
			 return new String(Integer.toString(curState.counter));
		 return null;
	 }
	 
	 /**
	  * Retrieves the msg queue index of the current event
	  * being processed by the fsm.
	  * @return
	  */
	 public int getCurrentMsgQueueIndex() {
		 return this.msgQueueIndex;
	 }
	
	 /**
	  * Sets the name of the diameter stack to use for this FSM.
	  * @return
	  */
	 public String getDiameterStack() {
		 return this.diameterStack;
	 }
	 /**
	  * Allows the model to get access to the queue for processing.
	  * @return
	  */
//	 protected ConcurrentLinkedQueue<MsgEvent> getEventQueue() {
//		 return this.queue;
//	 }
	 
	 /**
	  * Look's up an FSM by uid.
	  * @param key - uid of the FSM to search obtain.
	  * @return - the FSM with the given name or null.
	  */
	 public static FSM getFSM(int uid) {
		 Enumeration<FSM> elements = FSMs.elements();
		 while (elements.hasMoreElements()) {
			 FSM f = elements.nextElement();
			 if (f.getUID() == uid)
				 return f;
		 }
		 return null;
	 }
	 
	 /**
	  * Look's up an FSM by name.
	  * @param key - name of the FSM to search obtain.
	  * @return - the FSM with the given name or null.
	  */
	 public static FSM getFSM(String key) {
		 return FSMs.get(key);
	 }
	 
	 /**
	  * Retrieve a initial state within the FSM.
	  * @param name - name of the state to retrieve.
	  * @return - the state if it exists in the table, null otherwise.
	  */
	 public String getInitialState() {
//		 State s = (State)states.get(name);
//		 if (s != null)
//			 return s.getName();
//		 return null;
		 return initState;
	 }
	 /**
	  * Gets the last Utility message received by the platform 
	  */
	 public UtilityMsg getLastUtilityMsg() {
		 return lastUtilMsg;
	 }

	 protected FSMListener getListener() {
		 return this.listener;
	 }

	 /**
	  * Gets the LogAPI to use for subelements of this class
	  */
	 public LogAPI getLogger() {
		 return logger;
	 }
	 
	 /**
	  * Gets the model 
	  * @return
	  */
	 public Model getModel() {

		 return model;
	 }

	 /**
	  * Gets the name of this FSM.
	  */
	 public String getName() {
		 return name;
	 }

	 /**
	  * Gets the network elements being simulated
	  * @return
	  */
	 public NetworkElements getNetworkElements() {
		 return nes;
	 }

	 /**
	  * Accessor method for prevState attribute
	  * @return prevState
	  */
	 public State getPrevState()
	 {
		 return prevState;
	 }

	 /**
	  * Returns the size of the event queue when not in
	  * a sleep state. If the FSM is currently sleeping 
	  * it returns 0.
	  * @return
	  */
	 public int getQueueSize() {
		 synchronized (queue) {
			 if (curState.isSleeping()) {
				 logger.trace(PC2LogCategory.FSM, name, 
						 name + "'s queue contains " + queue.size() + 
				 " event(s) waiting for state machine to awaken.");
				 return 0;
			 }
			 else {
				 return queue.size(); 
				 
			 }
		 }
	 }

	 /**
	  * Sets the RefenecePointsFactory for this FSM.
	  * @return
	  */
	 public ReferencePointsFactory getReferencePointsFactory() {
		 return rpf;
	 }  
	 
	 /**
	  * Gets an iterator to the list of services supported
	  * by this FSM
	  */
	 public ListIterator<String> getServices() {
		 return this.services.listIterator();
	 }


	 
	 /**
	  * Gets the name of the SipStack to use when sending
	  * a message. This is used to override the default
	  * stack name.
	  */
	 public String getSipStack() {
		 return sipStack;
	 }
	 
	 /**
	  * Retrieve a specific state within the FSM.
	  * @param name - name of the state to retrieve.
	  * @return - the state if it exists in the table, null otherwise.
	  */
	 public State getState(String name) {
		 return states.get(name);
	 }
	 
	 /**
	  * Retrieve the name of the currently active state within the FSM.
	  * 
	  * @return - the name of the currently active state, null otherwise.
	  */
	 public String getCurrentStateName() {
		 if (curState != null)
			 return curState.getName();
		 else 
			 return null;
	 }
	 
	 /**
	  * Retrieves a handle to the states defined within the FSM.
	  * 
	  * @return an Enumeration into the data store containing the States for this FSM
	  */ 
	 public Enumeration<State> getStates() {
		 return states.elements();
	 }
	 
	 /**
	  * Gets the subcategory being used to log for this FSM
	  */
	 public String getSubcategory() {
		 return name;
	 }

	 public String getTestStats() {
		 // Test if this FSM had anything to validate
		 if (ungroupedTests.size() == 0 &&
				 ungroupedVerifyLogMsgs.size() == 0 &&
				 groupedVerifyLogMsgs.size() == 0 &&
				 groupedTests.size() == 0) {
			 return "\t Nothing to be verified.\n";
		 }
		 else {
			 String result = "";
			  ListIterator<Verify> iter = ungroupedTests.listIterator();
			  while (iter.hasNext()) {
				  Verify v = iter.next();
					  if (v.passed()) {

						  String step = " verify";
						  if (v.getStep() != null)
							  step = " Step " + v.getStep();
							  
						  result += "\t" + v.getState().getName() + step + " Passed: " 
						    + ((v.getRequirements() != null) ? v.getRequirements() : "") + "\n";
					  }
					  else {
						  if (v.executed) {
							  String step = " verify";
							  if (v.getStep() != null)
								  step = " Step " + v.getStep();
							  result += "\t" + v.getState().getName() + step + " Failed: " 
									    + ((v.getRequirements() != null) ? v.getRequirements() : "") + "\n";
						  }
						  else {
							  State s = v.getState();
							  if (s != null)
								  result += "Failed to execute in State (" 
								  + s.getName() + ") in FSM (" 
								  + this.name + ").";
							  else 
								  result += "Failed to execute in FSM (" 
								  + this.name + ").";
						  }
					  }  
			 }
			  ListIterator<LogMsg>iter2 = ungroupedVerifyLogMsgs.listIterator();
			  while (iter2.hasNext()) {
				  LogMsg lm = iter2.next();

					  if (lm.passed()) {
						  String step = " log";
						  if (lm.getStep() != null)
							  step = " Step " + lm.getStep();
						  result += "\t" + lm.getState().getName() + step + " Passed: " 
								    + ((lm.getRequirements() != null) ? lm.getRequirements() : "") + "\n";
					  }
					  else {
						 if (lm.executed) {
							 String step = " log";
							  if (lm.getStep() != null)
								  step = " Step " + lm.getStep();
							  result += "\t" + lm.getState().getName() + step + " Failed: " 
									    + ((lm.getRequirements() != null) ? lm.getRequirements() : "") + "\n";
						 }
						 else {
							 State s = lm.getState();
							 if (s != null)
								 result += "Failed to execute in State (" 
									 + s.getName() + ") in FSM (" 
									 + this.name + ").";
							 else 
								 result += "Failed to execute in FSM (" 
									 + this.name + ").";
						 }
					  }
//				  }
			 }
			 Enumeration<String> groups = groupedTests.keys();
		 
			 while (groups.hasMoreElements()) {
				 LinkedList<Verify> list = groupedTests.get(groups.nextElement());
				 iter = list.listIterator();
				 //Processing is different for the groups result
				 // first any of the entries in the list should have the list
				 // of requirements, but we need to look through all of them to
				 // see if any passed the verification.
				 Verify passed = null;
				 Verify failed = null;
				  while (iter.hasNext() && passed == null) {
					 Verify v = iter.next();
					 // Any verify pass is sufficient 
					 if (v.passed()) {
						passed = v;
					 } // only take the failures for this FSM
					 else if (failed == null && v.getState().getOwner().getName().equals(name)) {
						 failed = v;
					 }
				 }
				 if (passed != null && passed.getState().getOwner().getName().equals(name)) {
					 String step = " verify";
					 if (passed.getStep() != null)
						  step = " Step " + passed.getStep();
					  result += "\t" + passed.getState().getName() + step + " Passed: " 
							    + ((passed.getRequirements() != null) ? passed.getRequirements() : "") + "\n";
				 }
				 else if (failed != null && passed == null) {
					 String step = " verify";
					  if (failed.getStep() != null)
						  step = " Step " + failed.getStep();
					  result += "\t" + failed.getState().getName() + step + " Failed:" 
							  + ((failed.getRequirements() != null) ? failed.getRequirements() : "") + "\n";
				 }

					
			 }
			 Enumeration<String> logs = groupedVerifyLogMsgs.keys();
			 while (logs.hasMoreElements()) {
				 LinkedList<LogMsg> list = groupedVerifyLogMsgs.get(logs.nextElement());
				 iter2 = list.listIterator();
				 //Processing is different for the groups result
				 // first any of the entries in the list should have the list
				 // of requirements, but we need to look through all of them to
				 // see if any passed the verification.
				 LogMsg passed = null;
				 LogMsg failed = null;
				 while (iter2.hasNext() && passed == null) {
					 LogMsg lm = iter2.next();
					// Any logMsg pass is sufficient 
					 if (lm.passed()) {
						passed = lm;
					 } // only take the failures for this FSM
					 else if (failed == null && lm.getState().getOwner().getName().equals(name)) {
						 failed = lm;
					 }
				 }
				 if (passed != null && passed.getState().getOwner().getName().equals(name)) {
					  String step = " log";
					  if (passed.getStep() != null)
						  step = " Step " + passed.getStep();
					  result += "\t" + passed.getState().getName() + step + " Passed:" + ((passed.getRequirements() != null) ? passed.getRequirements() : "") + "\n";
				 }
				 else if (failed != null && passed == null) {
					 String step = " log";
					  if (failed.getStep() != null)
						  step = " Step " + failed.getStep();
					  result += "\t" + failed.getState().getName() + step + " Failed:" + ((failed.getRequirements() != null) ? failed.getRequirements() : "") + "\n";
				 }
			 }
			 return result;
		 }
	 }
	 /**
	  * Gets the unique identifier for this FSM.
	  * @return
	  */
	 public int getUID() {
		 return uid;
	 }
	 
	/**
	  * Looks to see if there is a state with the given name in the 
	  * state table.
	  * 
	  * @param key - name of the state.
	  * @return - true if there is a state with the name defined by key
	  * 		false otherwise
	  */
	 public boolean hasState(String key) {
		 State s = states.get(key);
		 if (s != null)
			 return true;

		 return false;
	 }

	 /**
	  * Allows one FSM to request if a specific UE is registered
	  * or not upon starting.
	  * @return
	  */
	 public boolean isCurrentStateRegistered() {
		 return (curState.getName().equals("Registered"));
	 }
	 
	 /**
	 * This initiates the FSM processing by setting the initial state and begin
	 * the initial states execution.
	 * 
	 * @throws IllegalStateException
	 */
	public void init(ConcurrentLinkedQueue<MsgEvent> eventQueue, FSMListener owner) throws IllegalStateException {
		if (eventQueue == null) {
			String err = "The FSM's must be given the event queue to process events from.";
			logger.fatal(PC2LogCategory.FSM, name, err);
			throw new IllegalStateException("FSM(" + name +") has failed because the Event Queue is equal to null.");
		}
		else
			queue = eventQueue;
		if (owner == null) {
			String err = "The FSM's must be given the FSMListener to add timer events.";
			logger.fatal(PC2LogCategory.FSM, name, err);
			throw new IllegalStateException("FSM(" + name +") has failed because the owner is equal to null.");
		}
		else
			listener = owner;
			
		// Verify that we have an API to process Actions
		if (api == null) {
			String err = "The FSM's API has not been properly set prior to initialization.";
			logger.fatal(PC2LogCategory.FSM, name, err);
			throw new IllegalStateException("FSM(" + name +") has failed because the FSM API is equal to null.");
		}

		// Set the state machine to the initial state
		curState = states.get(initState);

		if (curState == null) {
			logger.fatal(PC2LogCategory.FSM, name, 
					"FSM unable to locate initial state.");
			Result r = new Result(false);
			api.pass(r);
			throw new IllegalStateException("FSM(" + name +") has failed because the initialState doesn't exist.");
		}
		else {

			curState.init(api, compEval, defaultNoResponseTimeout);
			logger.debug(PC2LogCategory.FSM, name, 
				"FSM(" + name + ") entering State(" + curState.getName() + ").");
			curState.processPrelude(msgQueueIndex);
		}

		boolean endImmediately = SystemSettings.getBooleanSetting(SettingConstants.FSM_END_IMMEDIATELY);
		if (endImmediately) {
			Enumeration<State> elements = states.elements();
			while (elements.hasMoreElements()) {
				State s = elements.nextElement();
				if (s instanceof ENDState ||
						s instanceof EndSessionState) {
					((EndSessionState)s).setEndImmediately();
				}
			}
		}
	}

	/** 
	 * This method validates that the initial state of the FSM exists
	 * 
	 * @return - true if the initial state of the FSM exists, false otherwise.
	 */
	public boolean hasInitialState() {
		if (initState != null) {
			State state = states.get(initState);
			if (state != null)
				return true;
		}
		return false;
	}
	/**
	 * Notification to the FSM that no response has been received in the 
	 * specified time.
	 *  
	 * @param task - the NoResponseTimerExpired is added to the queue.
	 * @param owner - the state that created and started the timer.
	 */
	public void noResponseTimeout(TimerTask task, State owner) {
		if (curState.validTimerTask(task, owner)) {
			InternalMsg msg = new InternalMsg(uid, System.currentTimeMillis(), 
						LogAPI.getSequencer(), TimeoutConstants.NO_RESPONSE_TIMEOUT);
			listener.processEvent(msg);
		}
	}


	 /**
	  * This is the implementation for the FSMListener to be able to add
	  * an event to the FSM's list of event awaiting processing.
	  * 
	  * @param event - the new event to be processed
	  * @return - true if the event was added to the queue, false otherwise.
	  * 
	  * @throws IllegalArgumentException
	  */
	 public boolean processEvent(MsgEvent event) throws IllegalArgumentException {
		 boolean result = false;
		 if (curState instanceof EndSessionState &&
				 ((EndSessionState)curState).isComplete() &&
				 !(event.getEventName().equals(EventConstants.SESSION_TERMINATED))) {
			 logger.warn(PC2LogCategory.FSM, name,
					 "DROPPING EVENT(" + event.getEventName() + ") SENT TO FSM(" + name + "'s) QUEUE - " 
					 + event.getEventName() + " size = " + queue.size() 
					 + " because FSM is done processing.");
			 return result;
		 }
		 
		 synchronized (queue) {
//			 BRKPT
//			 if (event.getEventName().equalsIgnoreCase("ONLINE")) {
//			 int glh =0;
//			 }
			 queue.add(event);
			 queue.notifyAll();
			 logger.debug(PC2LogCategory.FSM, name,
					 "ADDING TO FSM(" + name + "'s) QUEUE - " + event.getEventName() + " size = " + queue.size());
			 result = true;
		 }
		
		 return result;

	 }

	 /**
	  * This is the initial processing of an event in the queue.
	  * 
	  * @return - true if the event could be processed, false otherwise.
	  * @throws IllegalArgumentException
	  */
	 public boolean processEvent() throws IllegalArgumentException {
		 boolean result = false;
		 MsgEvent event = null;

		 
		 synchronized (queue) {
			 event = queue.remove(); // .removeFirst();
		 	 logger.debug(PC2LogCategory.FSM, name,
					 name + " QUEUE SIZE - " + queue.size());
		 }
		 if (event != null) {

			  msgQueueIndex = event.getMsgQueueIndex();
			 result = true;
			 String eventName = event.getEventName();
//			 BRKPT
//			 if (getCurrentStateName().equals("Connected") && eventName.equalsIgnoreCase("TimerExpired")) {
//				 int glh =0;
//			 }
//			 if (getCurrentStateName().equals("Alerting") && event.getEventName().equalsIgnoreCase("180-Invite")) {
//				 int glh = 0;
//			 }
//			 logger.info(PC2LogCategory.FSM, name, 
//					 "FSM (" + name + ") - State (" + curState.getName() + ") processing event (" 
//						+ eventName + ") sequencer=" + event.getSequencer() + ".");

			 rpf.rcvd(event);

			 curState.processEvent(event);
			 // Next we need to test the reference points

			 // Lastly we need to change to the next state if there is
			 // one
			 changeStates(eventName);

		 }
		 return result;
	 }



	 public State removeState(String state) throws PC2Exception {
		 State s = states.remove(state);
		 // Now see if we need to remove any verifies from the tables
		 if (s != null) {
			 ListIterator<Verify> iter = ungroupedTests.listIterator();
			 while(iter.hasNext()) {
				 Verify v = iter.next();
				 if (v.getState() == s) {
					 iter.remove();
				 }
			 }
			 ListIterator<LogMsg>iter2 = ungroupedVerifyLogMsgs.listIterator();
			 while (iter2.hasNext()) {
				 LogMsg lm = iter2.next();
				 if (lm.getState() == s) {
					 iter2.remove();
				 }
			 }
			 Enumeration<String> groups = groupedTests.keys();
			 while (groups.hasMoreElements()) {
				 LinkedList<Verify> list = groupedTests.get(groups.nextElement());
				 iter = list.listIterator();
				 while (iter.hasNext()) {
					 Verify v = iter.next();
					 if (v.getState() == s) {
						 iter.remove();
						 if (list.size() == 0)
							 groupedTests.remove(list);
					 }
				 }
			 }
			 Enumeration<String> logs = groupedVerifyLogMsgs.keys();
			 while (logs.hasMoreElements()) {
				 LinkedList<LogMsg> list = groupedVerifyLogMsgs.get(logs.nextElement());
				 iter2 = list.listIterator();
				 while (iter2.hasNext()) {
					 LogMsg lm = iter2.next();
					 if (lm.getState() == s) {
						 iter2.remove();
						 if (list.size() == 0)
							 groupedVerifyLogMsgs.remove(list);
					 }
				 }
			 }
		 }
			 
		 return s;
	 }
	 
	 /**
	  * Resumes state machine processing after it has paused due to
	  * a call to suspendForPromp().
	  * 
	  */
	 public void resumeFromPrompt() {
		 curState.resume();
		
	 }
	 
	 /**
	  * Sets the processor of Actions that the FSM needs to
	  * have performed by the system.
	  * @param api
	  */
	 public void setAPI(FSMAPI api) {
		 this.api = api;
	 }
	 
	 /**
	  * Sets the comparison operations implementation class
	  * for this FSM.
	  * @param ce
	  */
	 public void setComparisonEvaluator(ComparisonEvaluator ce) {
		 this.compEval = ce;
	 }
	 
	 /**
	  * Sets the defaultNoResponseTimeout value to use in this
	  * FSM.
	  */
	 public void setDefaultNoResponseTimeout(int timeout) {
		 this.defaultNoResponseTimeout = timeout;
	 }
	 
	 /**
	  * Sets the name of the diameter stack to use in this
	  * FSM.
	  */
	 public void setDiameterStack(String diaStack) {
		 this.diameterStack = diaStack;
	 }
	 
	 /**
	  * Sets the initial state attribute for this FSM.
	  * 
	  * @param initialState
	  */
	 public void setInitialState(String initialState) {
		 this.initState = initialState;
	 }
	 
	 /**
	  * Sets the model attribute.
	  * @param m
	  */
	 public void setModel(Model m) {
		 this.model = m;
	 }

	 /**
	  * Sets the name of this FSM.
	  * @param name
	  */
	 public void setName(String name) {
		  FSM tmp = FSMs.remove(this.name);
	      this.name = name;  
		  FSMs.put(this.name, tmp);
	 }

	 /**
	  * Sets the network elements attribute
	  * @param n
	  */
	 public void setNetworkElements(NetworkElements n) {
		 this.nes = n;
	 }

	 /**
	  * Sets the list of services that this FSM supports
	  * @param ll - linked list of service protocols
	  */
	 public void setServices(LinkedList<String> ll) {
		 this.services = ll;
	 }

	 /**
	  * Sets the name of the SipStack to use when sending
	  * a message from a distributor that contains 
	  * multiple IPs. Each stack is associated with one
	  * and only one IP address for a given protocol.
	  * If the value is not set, the distributor will
	  * use the default stack name.
	  */
	 public void setSipStack(String stack) {
		 this.sipStack = stack;
	 }
	 
	 /**
	  * Initiates the sleep operation within this FSM.
	  * 
	  * @param time - the amount of time in milliseconds to sleep.
	  * @return
	  */
	 public boolean sleep(int time) {
		 return curState.startSleeping(time);
	 }

	 /**
	  * Terminates the sleep operation within this FSM.
	  * @return
	  */
	 public boolean sleepCancel() {
		 curState.sleepCancel();
		 return true;
	 }
	 /**
	  * Notification that the internal state timer has expired.
	  * 
	  * @param task - the TimerExpired event is added to the queue.
	  * @param owner - the state that created and started the timer.
	  */
	 public void stateTimerExpired(TimerTask task, State owner) {
		 if (curState.validTimerTask(task, owner)) {
			 InternalMsg msg = new InternalMsg(uid, System.currentTimeMillis(), 
						 LogAPI.getSequencer(), TimeoutConstants.TIMER_EXPIRED);
			listener.processEvent(msg);
		 }
	 }
	
	 /**
	  * Allows the FSM to cleanup before terminating
	  *
	  */
	 public void stop() {
		 if (curState != null)
			 curState.stopAllTimers();
		 Enumeration<State> e = states.elements();
		 while (e.hasMoreElements()) {
			 State s = e.nextElement();
			 s.stopAllTimers();
		 }
	 }

	 /**
	  * Suspends all state machine processing until user responds
	  * to a prompt. This is accomplished by calling resumeFromPrompt();
	  * 
	  */
	 public void suspendForPrompt() {
		 curState.startSleeping(-1);
	 }

	 /**
	  * Determines if a transition exists for a state.
	  * 
	  * @param from - the name of the state to search in 
	  * @param event - the event to transition upon.
	  * @return true if the transition exists for the state, false otherwise
	  */
	 public boolean transitionExists(String from, String event) {
		 State s = getState(from);
		 if (s != null)
			 return s.transitionExists(event);
		 return false;
	 }
	 
	 public boolean useValidate() {
		 if (ungroupedTests.size() > 0 ||
				 ungroupedVerifyLogMsgs.size() > 0 ||
				 groupedVerifyLogMsgs.size() > 0 ||
				 groupedTests.size() > 0) {
			 return true;
		 }
		 return false;
	 }
	 
	 public void setNoResponseTimeout(int newValue) {
			this.defaultNoResponseTimeout = newValue;
	 }
	 
	 public boolean validateTest() {
		 // Test if this FSM had anything to validate
		 if (ungroupedTests.size() == 0 &&
				 ungroupedVerifyLogMsgs.size() == 0 &&
				 groupedVerifyLogMsgs.size() == 0 &&
				 groupedTests.size() == 0) {
			 return true;
		 }
		 else {
			 Boolean result = null;
			 ListIterator<Verify> iter = ungroupedTests.listIterator();
			 while (iter.hasNext()) {
				 Verify v = iter.next();
				 if (result == null)
					 result = v.passed();
				 else 
					 result = result & v.passed();
			 }
			 ListIterator<LogMsg>iter2 = ungroupedVerifyLogMsgs.listIterator();
			 while (iter2.hasNext()) {
				 LogMsg lm = iter2.next();
				 if (result == null)
					 result = lm.passed();
				 else 
					 result = result & lm.passed();
			 }
			 Enumeration<String> groups = groupedTests.keys();
			 while (groups.hasMoreElements()) {
				 LinkedList<Verify> list = groupedTests.get(groups.nextElement());
				 iter = list.listIterator();
				 Boolean intermediate = null;
				 while (iter.hasNext()) {
					 Verify v = iter.next();
					 if (intermediate == null)
						 intermediate = v.passed();
					 else 
						 intermediate = intermediate | v.passed();
				 }
				 if (result == null)
					 result = intermediate;
				 else if (intermediate != null)
					 result = result & intermediate;
				 else 
					 result = false;
			 }
			 Enumeration<String> logs = groupedVerifyLogMsgs.keys();
			 while (logs.hasMoreElements()) {
				 LinkedList<LogMsg> list = groupedVerifyLogMsgs.get(logs.nextElement());
				 iter2 = list.listIterator();
				 Boolean intermediate = null;
				 while (iter2.hasNext()) {
					 LogMsg lm = iter2.next();
					 if (intermediate == null)
						 intermediate = lm.passed();
					 else 
						 intermediate = intermediate | lm.passed();
				 }
				 if (result == null)
					 result = intermediate;
				 else if (intermediate != null)
					 result = result & intermediate;
				 else 
					 result = false;
			 }
			 return result;
		 }
		 
	 }
	 
	 /** This implements a deep copy of the class for replicating 
	  * FSM information.
	  * 
	  * @throws CloneNotSupportedException if clone method is not supported
	  * @return Object
	  */ 
	 @Override
	public Object clone() throws CloneNotSupportedException {
		 FSM newFSM = (FSM) super.clone();
		 if (newFSM != null) {
			 newFSM.uid = ++uidCounter;
			 newFSM.name = new String (this.name + "-" + newFSM.uid);
			 if (this.initState != null) 
				 newFSM.initState = new String(this.initState);
			 if (this.states != null) {
				newFSM.states = new Hashtable<String, State>();
				Enumeration<State> ss = states.elements();
				 while (ss.hasMoreElements()) {
					 State s = ss.nextElement();
					 State newState = (State)s.clone();
					 newState.setOwner(newFSM);
					 newState.updateUID(newFSM.getUID(), uid);
					 newFSM.states.put(newState.getName(),newState);
				 }
			 }
			 if (this.api != null)  
				 newFSM.api = this.api;
			 if (this.compEval != null) 
				 newFSM.compEval = this.compEval;
			 newFSM.queue = new ConcurrentLinkedQueue<MsgEvent>(); // new LinkedList<MsgEvent>();
			 newFSM.logger = LogAPI.getInstance(); // Logger.getLogger(FSM.class);
			 newFSM.rpf = new ReferencePointsFactory(this);
			 FSMs.put(newFSM.getName(), newFSM);
			 if (this.model != null) 
				 newFSM.model = (Model)this.model.clone();
			 if (this.nes != null) 
				 newFSM.nes = (NetworkElements)this.nes.clone();
			 if (this.sipStack != null) 
				 newFSM.sipStack = new String(this.sipStack);
			 if (this.services != null) {
				 newFSM.services = new LinkedList<String>();
				 ListIterator<String> iter = this.services.listIterator();
				 while (iter.hasNext()) {
					 String service = iter.next();
					 newFSM.services.add(new String(service));
				 }
				 
			 }
			 
		 }
//		 System.out.println("this " + this.me());
//		 System.out.println("retval " + newFSM.me());
		 return newFSM;
	 }

	 /**
	  * Creates a string representation of the FSM.
	  */
	 @Override
	public String toString() {
		 String result = name + " - " + "\nuid=" + uid + "\ninitial state=" 
		 	+ initState + "\nmodel " + model.toString() 
		 	+ "\nNE " + nes.toString() + "\n sipStack " + sipStack + "\nStates:";
		 Enumeration<String> keys = states.keys();
		 while(keys.hasMoreElements()) {
			 String key = keys.nextElement();
			 result += states.get(key).toString(); 
		 }
		
		 return result;
	 }
	 
//	 This method was added as a validator for cloning
//	 public String me() {
//		 String result = "FSM - " + super.toString() + "\n";
//		 Enumeration keys = states.keys();
//		 while(keys.hasMoreElements()) {
//			 String key = (String)keys.nextElement();
//			 result += states.get(key).me() + "\n"; 
//		 }
//
//		 return result;
//	 }
}
