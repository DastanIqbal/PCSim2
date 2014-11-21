package com.cablelabs.diagram;

import java.awt.Container;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.cablelabs.log.LogAPI;
import com.cablelabs.log.LogAPIConfig;
import com.cablelabs.log.LogCategory;


public class SDT extends JFrame implements
ActionListener, WindowListener,
WindowStateListener, WindowFocusListener {
    
    private static boolean debug = false;
    
    static {
        // Initialize the logger
        LogAPIConfig config = new LogAPIConfig()
        .setCreateLogDirIfNoExists(true)
        .setCategoryClass(SDTLogCat.class)
        .setLogPrefix("SDT_")
        .setLogLevelConfigPath("../config/SDTLogConfig.txt");
    
        LogAPI.getInstance(config, true);
    }
	
	private static final LogAPI logger = LogAPI.getInstance();
	private static final String subCat = "SDT";

	private static final String EXIT = "Exit";
	private static final String OPEN = "Open";
	private static final String TITLE = "Sequence Diagram Tool";
	
	public static final String LOG_EXT = ".log";

	
	private static final long serialVersionUID = 1L;
	
	private JMenuBar menuBar;
	private SDTDiagramPanel sDTDiagramPanel;
	
	private File lastActiveDirectory = new File(".");

	private static File toOpen = null;
	public SDT() {
	    if (debug) {
	        File f = new File("../../../logs");
	        if (f.exists() && f.isDirectory()) {
	            lastActiveDirectory = f;
	        }
	    } else {
	        File f = new File("../logs");
	        if (f.exists() && f.isDirectory()) {
	            lastActiveDirectory = f;
	        }    
	    }

	    
		
        // Set the systems default look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception e) {
            System.err.println("Unable to set default system look and feel");
        }
		
        setTitle(TITLE);
        
		// Exit app when last frame is closed.
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		
		addWindowListener(this);
		addWindowStateListener(this);
		addWindowFocusListener(this);
		
		setSize(900, 630);
		setLocation(100, 100);
		
		/********************** The main container ****************************/

		Container container = this.getContentPane();
		container.setLayout(new GridBagLayout());
		//container.setBackground(Color.LIGHT_GRAY);
		
		createMenuBar();
				
		sDTDiagramPanel = new SDTDiagramPanel();
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0d;
		c.weighty = 1.0d;
		c.fill = GridBagConstraints.BOTH;
		container.add(sDTDiagramPanel, c);
		//container.setBackground(Color.BLUE);
		this.setMinimumSize(sDTDiagramPanel.getMinimumSize());
		
		//pack();
		setVisible(true);
	}

    @Override
	public void actionPerformed(ActionEvent ev) {
		// Create a file chooser that opens up as an Open dialog.
		String action = ev.getActionCommand();
		Object source = ev.getSource();

		logger.debug(LogCategory.APPLICATION, subCat,  "Action(" + action + ") preformed by (" + source + ")");

		if (action.equals(OPEN)) {
			openAction(ev, false);
		}
		else if (action.equals(EXIT)) {
			setVisible(false);
			close();
		}
	}

	@Override
	public void windowActivated(WindowEvent e) {
		logger.debug(LogCategory.APPLICATION, subCat,  "SeqDiagramFrame - windowActivated event=" + e);
	}

	@Override
	public void windowClosed(WindowEvent e) {
		logger.debug(LogCategory.APPLICATION, subCat,  "SeqDiagramFrame - windowClosed event=" + e);
		close();
	}

	@Override
	public void windowClosing(WindowEvent e) {
		logger.debug(LogCategory.APPLICATION, subCat,  "SeqDiagramFrame - windowClosing event=" + e);

		setVisible(false);
		close();
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		logger.debug(LogCategory.APPLICATION, subCat,  "SeqDiagramFrame - windowDeactivated event=" + e);
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		logger.debug(LogCategory.APPLICATION, subCat,  "SeqDiagramFrame - windowDeiconified event=" + e);
	}
	@Override
	public void windowGainedFocus(WindowEvent e) {
		logger.debug(LogCategory.APPLICATION, subCat,  "SeqDiagramFrame - WindowFocusListener method called: windowGainedFocus.");
	}

	@Override
	public void windowIconified(WindowEvent e) {
		logger.debug(LogCategory.APPLICATION, subCat,  "SeqDiagramFrame - windowIconified event=" + e);
	}

	@Override
	public void windowLostFocus(WindowEvent e) {
		logger.debug(LogCategory.APPLICATION, subCat,  "SeqDiagramFrame - WindowFocusListener method called: windowLostFocus.");
	}

	@Override
	public void windowOpened(WindowEvent e) {
		logger.debug(LogCategory.APPLICATION, subCat,  "SeqDiagramFrame - windowOpened event=" + e);
		if (toOpen != null) {
            openFile(toOpen);
            toOpen = null;
        }
	}

	@Override
	public void windowStateChanged(WindowEvent e) {
		displayStateMessage(
				"SeqDiagramFrame - WindowStateListener method called: windowStateChanged. ", e);
	}
	
	private static String convertStateToString(int state) {
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
	
	private static void displayStateMessage(String prefix, WindowEvent e) {
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
		logger.debug(LogCategory.APPLICATION, subCat,  msg);
	}

	private void close() {
		//System.exit(0);
	}
	
	private void createMenuBar() {
		
		/********************** Menu bar **************************************/
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic('F');

		JMenuItem item = new JMenuItem(OPEN, 'O');
		item.addActionListener(this);
		fileMenu.add(item);

		fileMenu.addSeparator();

		item = new JMenuItem(EXIT, 'x');
		item.addActionListener(this);
		fileMenu.add(item);
		menuBar.add(fileMenu);
	}

	void openFile(File f) {
        if (f != null) {
            logger.info(LogCategory.APPLICATION, subCat,  "Opening log file: " + f);
            setTitle(TITLE + " -" + f.getName());
            sDTDiagramPanel.display(f);
        }
	}
	
	 void openAction(ActionEvent ev, boolean closeOnCancle) {
		JFileChooser chooser = new JFileChooser( );
		chooser.setMultiSelectionEnabled(false);
		chooser.addChoosableFileFilter(new LogFileFilter(LOG_EXT, "PCSim2 Log File ( *" + LOG_EXT + " )"));
		chooser.setCurrentDirectory(lastActiveDirectory);
		int option = chooser.showOpenDialog(this);
		if (option == JFileChooser.APPROVE_OPTION) {
			openFile(chooser.getSelectedFile());
		}
		else {
			logger.debug(LogCategory.APPLICATION, subCat,  "You canceled.");
			if (closeOnCancle)
			    dispose();
		}
		lastActiveDirectory = chooser.getCurrentDirectory();
	}
	 
	 private static boolean parseArgs(String[] args) {
	     if (args == null || args.length == 0) return true;

	     StringBuilder errors = new StringBuilder();
	     for (int i = 0; i < args.length; i++) {
	         String arg = args[i];
	         // this is a hidden option
	         if (arg.equals("--debug")) {
	             debug = true; 
	         }
	         else if (arg.equals("-f")) {
	             if (args.length > i+1) {
	                 i++;
	                 File f = new File(args[i]);
	                 if (f.exists() && f.isFile() && f.canRead()) {
	                     toOpen = f;
	                 } else {
	                     errors.append("\nError file is not valid: " + f.getAbsolutePath());
	                 }
	             } else {
	                 errors.append("path to file is expected after option -f");
	             }
	         }
	         else {
	             errors.append("\nUnknown option " + arg);
	         }
	     }

	     if (errors.length() > 0) {
	         logger.fatal(SDTLogCat.LOG_MSG, subCat, "Error parsing command line arguments: " + errors.toString());
	         showUsage();
	         return false;
	     }

	     return true;
	 }
	 
	 private static void showUsage() {
	     // Keep --debug a hidden option
	     String usage = "Usage: java -jar SDT.jar [options]\n" +
	     		        "    options:\n" +
	     		        "      -f [PathToLogFile]\n";
	     logger.error(SDTLogCat.LOG_MSG, subCat, usage);
	 }
	 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    if (parseArgs(args)) {
	        new SDT();    
        } else {
            System.exit(-1);
        }
	}
}
