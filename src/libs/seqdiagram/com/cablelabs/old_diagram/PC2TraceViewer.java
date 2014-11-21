package com.cablelabs.old_diagram;

import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.*;

import javax.swing.*;


import tools.tracesviewer.*;

public class PC2TraceViewer extends javax.swing.JFrame implements
	ActionListener, WindowListener, WindowStateListener, WindowFocusListener {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	private boolean standaloneViewer = false;

	// Menus
	protected JMenuBar menuBar;
	protected Long startTime = null;

	protected JPanel firstPanel;
	protected JPanel secondPanel;
	protected JPanel firstSubPanel;
	protected JPanel secondSubPanel;
	protected JPanel thirdSubPanel;
	protected TextArea messageContentTextArea = null;

	public static final String EXT = ".log";
	private File lastActiveDirectory = null;
	private File lastFile = null;

	protected HashMap<String, MessageLogList> messageLogs = null;
	protected TracesSessions sessions = null;
	protected TracesSessionsList sessionList = null;
	protected TracesCanvas canvas = null;
	protected PC2TracesListener listener = null;
	protected PC2TraceParser parser = null;
	protected TracesAnimationThread animationThread = null;

	public PC2TraceViewer(String platform, String dut) {
	    
	    File logs = new File("../../logs");
	    if (logs.exists() && logs.canRead() && logs.isDirectory()) {
	        lastActiveDirectory = logs;
	    } else {
	        lastActiveDirectory = new File(".");
	    }
	    
		parser = new PC2TraceParser();
		addWindowListener(this);
		addWindowStateListener(this);
		addWindowFocusListener(this);
		setTitle("Trace Tool");
		// Exit app when frame is closed.
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// Enable AA on fonts
		System.setProperty("awt.useSystemAAFontSettings","lcd");
		System.setProperty("swing.aatext", "true");

		
		createMenuBar();
		
		// width, height
		this.setSize(800, 620);
		this.setLocation(100, 100);
		setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent ev) {
		// Create a file chooser that opens up as an Open dialog.
		String action = ev.getActionCommand();

		if (action.equals("Open")) {
			openAction(ev);
		}
		else if (action.equals("Exit")) {
			setVisible(false);
			close();
		}
	}


	private void close() {
		System.exit(0);
	}
	String convertStateToString(int state) {
		if (state == Frame.NORMAL) {
			return "NORMAL";
		}
		if ((state & Frame.ICONIFIED) != 0) {
			return "ICONIFIED";
		}
		//MAXIMIZED_BOTH is a concatenation of two bits, so
		//we need to test for an exact match.
		if ((state & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH) {
			return "MAXIMIZED_BOTH";
		}
		if ((state & Frame.MAXIMIZED_VERT) != 0) {
			return "MAXIMIZED_VERT";
		}
		if ((state & Frame.MAXIMIZED_HORIZ) != 0) {
			return "MAXIMIZED_HORIZ";
		}
		return "UNKNOWN";
	}

	private void createMenuBar() {
		/********************** The main container ****************************/

		Container container = this.getContentPane();
		container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
		//container.setLayout( new PercentLayout() );
		container.setBackground(Color.LIGHT_GRAY);


		/********************** Menu bar **************************************/
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');
		
		// Open
		JMenuItem item = new JMenuItem("Open", 'O');
		item.addActionListener(this);
		fileMenu.add(item);

		// Separator
		fileMenu.addSeparator();

		// Exit
		item = new JMenuItem("Exit", 'x');
		item.addActionListener(this);
		fileMenu.add(item);
		menuBar.add(fileMenu);
	}

	private void display(File f ) {
		sessions = new TracesSessions();
		sessions.setName(f.getName());
		if (messageLogs != null) {
			Enumeration<MessageLogList> elements = messageLogs.elements();
			while (elements.hasMoreElements()) {
				MessageLogList mll = elements.nextElement();
				TracesSession ts = new TracesSession(mll);
				ts.setName(f.getName());
				ts.setInfo("");
				ts.setLogDescription("");
				sessions.add(ts);
			}
		}
		if (sessions.isEmpty()) {
			TracesSession ts = new TracesSession();
			ts.setName("No available session, refresh");
			sessions.add(ts);
		}


		sessionList = new TracesSessionsList();
		initComponents();
		
		// try to prevent some of the flicker
		canvas.createBufferStrategy(2); // Note this can not be called until the object is visible
		
		listener = new PC2TracesListener(sessionList, canvas);

		sessionList.setTracesSessions(sessions);
	}

	void displayMessage(String msg) {
		System.out.println(msg);
	}

	void displayStateMessage(String prefix, WindowEvent e) {
		int state = e.getNewState();
		int oldState = e.getOldState();
		String msg = prefix
		+ "\n "
		+ "New state: "
		+ convertStateToString(state)
		+ "\n "
		+ "Old state: "
		+ convertStateToString(oldState);
		//display.append(msg + "\n ");
		System.out.println(msg);
	}

	private void initComponents() {
		Container container = this.getContentPane();
		/********************    FIRST PANEL         ********************************/
		firstPanel = new JPanel();
		firstPanel.setOpaque(false);// If False: we see the container's background
		firstPanel.setLayout(new PercentLayout());
		//firstPanel.setLayout(  new BorderLayout() );
		container.add(firstPanel);

		// Sub right panel:
		// topx %, topy %, width %, height % 73, 100-> 65, 95
		PercentLayoutConstraint firstPanelConstraint = new PercentLayoutConstraint(30, 0, 70, 100);
		sessionList.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				listener.tracesSessionsListStateChanged(e);
			}
		});
		sessionList.setForeground(Color.black);
		sessionList.setFont(new Font("Dialog", 1, 14));

		ScrollPane scroll = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
		TracesSession tracesSession = sessions.firstElement();
//		String name = tracesSession.getName();
		String logDescription = tracesSession.getLogDescription();
//		String callId = sessionList.getCallId(name);
//		String origin = sessionList.getOrigin(name);

		// Warning: to put before for the canvas!!!!
		messageContentTextArea = new TextArea();
//		JButton messageContentButton = new JButton("SIP Message:");
		JLabel messageContentLabel = new JLabel("Message Details:");
		canvas = new TracesCanvas(tracesSession, messageContentTextArea,
					logDescription, null);


		sessionList.setTracesCanvas(canvas);
		// The ScrollPane for the Canvas
		scroll.add(canvas);
		firstPanel.add(scroll, firstPanelConstraint);

		/***************************    SECOND PANEL         ********************************/

		//  left panel:
		secondPanel = new JPanel();
		secondPanel.setBackground(Color.black);
		// rows, columns
		//  secondPanel.setLayout(new GridLayout(3,1,0,0) );
		secondPanel.setLayout(new BorderLayout());
		// topx %, topy %, width %, height %
		PercentLayoutConstraint secondPanelConstraint =
			new PercentLayoutConstraint(0, 0, 30, 100);
		firstPanel.add(secondPanel, secondPanelConstraint);

		/****************************** FIRST SUB PANEL **********************************/

//		// Sub left panel:
//		firstSubPanel = new JPanel();
//		firstSubPanel.setBackground(Color.black);
//		// Top, left, bottom, right
//		firstSubPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 7, 5));

//		if (!standaloneViewer) {
//			// rows, columns, gap, gap
//			firstSubPanel.setLayout(new GridLayout(2, 1, 3, 6));
//			secondPanel.add(firstSubPanel, BorderLayout.NORTH);
//
//			JPanel panelGrid = new JPanel();
//			panelGrid.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
//			panelGrid.setLayout(new GridLayout(2, 1, 0, 0));
//
//			JPanel panelBox = new JPanel();
//			panelBox.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
//			panelBox.setLayout(new BorderLayout());
//
//			JLabel scriptLabel = new JLabel("Display the event script:");
//			scriptLabel.setToolTipText(
//				"Display the content of the selected script");
//
//			scriptLabel.setHorizontalAlignment(AbstractButton.CENTER);
//			scriptLabel.setForeground(Color.black);
//			scriptLabel.setFont(new Font("Dialog", 1, 14));
//			// If put to true: we see the label's background
//			scriptLabel.setOpaque(true);
//			panelGrid.add(scriptLabel);

//			choice = new Choice();
//			panelBox.add(choice, BorderLayout.CENTER);
//
//			scriptButton = new JButton("Open");
//			scriptButton.setToolTipText(
//				"Get the script controlling the current session");
//			scriptButton.setFont(new Font("Dialog", 1, 14));
//			scriptButton.setFocusPainted(false);
//			scriptButton.setBackground(new Color(186, 175, 175));
//			scriptButton.setBorder(new BevelBorder(BevelBorder.RAISED));
//			scriptButton.setVerticalAlignment(AbstractButton.CENTER);
//			scriptButton.setHorizontalAlignment(AbstractButton.CENTER);
//			panelBox.add(scriptButton, BorderLayout.EAST);
//			scriptButton.addActionListener(new ActionListener() {
//				public void actionPerformed(ActionEvent evt) {
//					listenerTracesViewer.scriptActionPerformed(evt);
//				}
//			});
//			panelGrid.add(panelBox);
//			firstSubPanel.add(panelGrid);
			//initComboBox();


//			refreshButton=new JButton("Refresh");
//			refreshButton.setToolTipText("Refresh all the sessions");
//			refreshButton.setFont(new Font ("Dialog", 1, 14));
//			refreshButton.setFocusPainted(false);
//			//refreshButton.setBackground(new Color(186,175,175));
//			refreshButton.setBackground( new Color(51,153,255));
//			refreshButton.setBorder(new BevelBorder(BevelBorder.RAISED));
//			refreshButton.setVerticalAlignment(AbstractButton.CENTER);
//			refreshButton.setHorizontalAlignment(AbstractButton.CENTER);
//			firstSubPanel.add(refreshButton);
//			refreshButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent evt) {
//			         listenerTracesViewer.refreshActionPerformed(evt);
//			   }
//			}
//			);

//			ImageIcon icon;
//			if (logoNist != null) {
//				icon = new ImageIcon(logoNist);
//
//				JLabel label = new JLabel(icon);
//				label.setVisible(true);
//				label.setToolTipText("The NIST logo!!!");
//				//label.setHorizontalAlignment(AbstractButton.CENTER);
//				label.setForeground(Color.black);
//				//label.setFont(new Font ("Dialog", 1, 14));
//				label.setOpaque(false);
//				firstSubPanel.add(label);
//			}
//		} else {
			// rows, columns, gap, gap
//			firstSubPanel.setLayout(new GridLayout(1, 1, 3, 6));
//			secondPanel.add(firstSubPanel, BorderLayout.NORTH);

//			ImageIcon icon;
//			if (logoNist != null) {
//				icon = new ImageIcon(logoNist);
//				JLabel label = new JLabel(icon);
//				label.setVisible(true);
//				label.setToolTipText("The NIST logo!!!");
//				label.setHorizontalAlignment(AbstractButton.CENTER);
//				label.setForeground(Color.black);
//				label.setFont(new Font("Dialog", 1, 14));
//				label.setOpaque(false);
//				firstSubPanel.add(label);
//			}
//		}

		/****************** SECOND SUB PANEL ****************************************/
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(2, 1, 0, 0));
		secondPanel.add(panel, BorderLayout.CENTER);
		secondSubPanel = new JPanel();
		secondSubPanel.setBackground(Color.black);
		secondSubPanel.setLayout(new BorderLayout());
		//secondPanel.add(secondSubPanel);
		panel.add(secondSubPanel);

		JLabel sessionsLabel = new JLabel("Sessions available:");
		sessionsLabel.setToolTipText("All the sessions currently available");
		// Alignment of the text
		sessionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		// Color of the text
		sessionsLabel.setForeground(Color.black);
		// Size of the text
		sessionsLabel.setFont(new Font("Dialog", 1, 14));
		// If put to true: we see the label's background
		sessionsLabel.setOpaque(true);
		sessionsLabel.setBackground(Color.lightGray);
		sessionsLabel.setBorder(BorderFactory.createLineBorder(Color.darkGray));
		secondSubPanel.add(sessionsLabel, BorderLayout.NORTH);

		ScrollPane scrollList = new ScrollPane(ScrollPane.SCROLLBARS_ALWAYS);
		scrollList.add(sessionList);
		secondSubPanel.add(sessionList, BorderLayout.CENTER);

		/******************** THIRD SUB PANEL ****************************************/

		thirdSubPanel = new JPanel();
		thirdSubPanel.setBackground(Color.black);
		thirdSubPanel.setLayout(new BorderLayout());
		//secondPanel.add(thirdSubPanel);
		panel.add(thirdSubPanel);

		messageContentLabel.setToolTipText(
			"Display all the content of the current SIP message");
		// Alignment of the text
		messageContentLabel.setHorizontalAlignment(SwingConstants.CENTER);
		// Color of the text
		messageContentLabel.setForeground(Color.black);
		// Size of the text
		messageContentLabel.setFont(new Font("Dialog", 1, 14));
		// If put to true: we see the label's background
		messageContentLabel.setOpaque(true);
		messageContentLabel.setBackground(Color.lightGray);
		messageContentLabel.setBorder(
			BorderFactory.createLineBorder(Color.darkGray));
//		messageContentButton.addActionListener(new ActionListener() {
//			public void actionPerformed(ActionEvent evt) {
//				listener.debugActionPerformed(evt);
//			}
//		});
		messageContentTextArea.setBackground(Color.white);
		messageContentTextArea.setEditable(false);
		messageContentTextArea.setFont(new Font("Dialog", 1, 12));
		messageContentTextArea.setForeground(Color.black);
		thirdSubPanel.add(messageContentLabel, BorderLayout.NORTH);
		thirdSubPanel.add(messageContentTextArea, BorderLayout.CENTER);

		validateTree();

	}


	protected void openAction(ActionEvent ev) {
		JFileChooser chooser = new JFileChooser();
		chooser.setMultiSelectionEnabled(false);
		chooser.addChoosableFileFilter(new PC2FileFilter(EXT, "PCSim2 Log File ( *" + EXT + " )"));
		chooser.setCurrentDirectory(lastActiveDirectory);
		int option = chooser.showOpenDialog(this);
		if (option == JFileChooser.APPROVE_OPTION) {
			File sf = chooser.getSelectedFile();
			if (sf != null) {
				System.out.println("You chose " + sf);
				messageLogs = parser.parse(sf);
				display(sf);
			}
		}
		else {
			System.out.println("You canceled.");
		}
		lastActiveDirectory = chooser.getCurrentDirectory();
	}

	@Override
	public void windowOpened(WindowEvent e) {
		System.out.println("Tracer - windowOpened event=" + e);
	}

	@Override
	public void windowClosed(WindowEvent e) {
		System.out.println("Tracer - windowClosed event=" + e);
		close();
	}

	@Override
	public void windowClosing(WindowEvent e) {
		System.out.println("Tracer - windowClosing event=" + e);

		setVisible(false);
		close();
	}
	@Override
	public void windowIconified(WindowEvent e) {
		System.out.println("Tracer - windowIconified event=" + e);
	}
	@Override
	public void windowDeiconified(WindowEvent e) {
		System.out.println("Tracer - windowDeiconified event=" + e);
	}
	@Override
	public void windowActivated(WindowEvent e) {
		System.out.println("Tracer - windowActivated event=" + e);
	}
	@Override
	public void windowDeactivated(WindowEvent e) {
		System.out.println("Tracer - windowDeactivated event=" + e);
	}

	@Override
	public void windowGainedFocus(WindowEvent e) {
		displayMessage("Tracer - WindowFocusListener method called: windowGainedFocus.");
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
		displayMessage("Tracer - WindowFocusListener method called: windowLostFocus.");
	}

	@Override
	public void windowStateChanged(WindowEvent e) {
		displayStateMessage(
				"Tracer - WindowStateListener method called: windowStateChanged.", e);
	}

	public static void main(String args[]) {
		new PC2TraceViewer("10.4.10.31:5060", "10.4.10.31:5070");
		return;
	}


}
