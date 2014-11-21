/*
######################################################################################
##                                                                                  ##
## (c) 2006-2012 Cable Television Laboratories, Inc.  All rights reserved.  Any use ##
## of this documentation/package is subject to the terms and conditions of the      ##
## CableLabs License provided to you on download of the documentation/package.      ##
##                                                                                  ##
######################################################################################


*/
package com.cablelabs.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.LinkedList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class PC2TextPane extends JTextPane implements ActionListener, MouseListener, PopupMenuListener{

	static final long serialVersionUID = 1;
	protected JTextPane textPane = null;
	/**
	 * This value controls whether the text that is appended into the textPane is 
	 * wrapped or not
	 */
	protected boolean wrapText = false;
	protected int maxWidth = 0;
	protected LinkedList<PC2FindMatch> highlights = new LinkedList<PC2FindMatch>();
	protected Color HIGHLIGHT = Color.yellow;
	protected Color FOCUS = new Color(128,128,255);
	protected DefaultHighlighter.DefaultHighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(HIGHLIGHT);
	protected DefaultHighlighter.DefaultHighlightPainter focus = new DefaultHighlighter.DefaultHighlightPainter(FOCUS);
	protected Color originalBackGround = null;
	private String highlightPattern = null;
	private int highlightIndex = -1;
	protected JPopupMenu popup = null;
	protected final String CLEAR = "Clear";
	protected final String LOCK = "Lock Screen";
	protected PC2Console owner = null;
	private boolean allowLock = false;
	private boolean limitSize = false;
	private int maxSize = 1000000;
	private final int rolloverVal = 10000;
	public PC2TextPane(boolean includeLock, boolean limit) {
		super();
		textPane = this;
		this.allowLock = includeLock;
		this.limitSize = limit;
	}
	
	public PC2TextPane(StyledDocument doc, boolean includeLock) {
		super(doc);
		textPane = this;
		this.allowLock = includeLock;
		init(true, false);
	}
	
	public void init(boolean editable, boolean wrap) {
		textPane.setEditable(editable);
		//this.wrapText = wrap;
		popup = new JPopupMenu();
		JMenuItem item;
		popup.add(item = new JMenuItem(CLEAR));
		item.addActionListener(this);
		
		if (allowLock) {
			popup.add(new JSeparator());
			JCheckBoxMenuItem check;
			popup.add(check = new JCheckBoxMenuItem(LOCK));
			check.addActionListener(this);
		}
		popup.setBorder(new BevelBorder(BevelBorder.RAISED));
		popup.addPopupMenuListener(this);
		popup.setEnabled(false);
	}
	
	@Override
	public void actionPerformed(ActionEvent ev) {
		// Get the action command
		String action = ev.getActionCommand();
		if (action.equals(CLEAR) ) {
			Document doc = textPane.getDocument();
			int end = doc.getLength();
			try {
				doc.remove(0, end);
				owner.displayBuild();
			}
			catch (BadLocationException ble) {
				System.out.println("Could not clear the text from the window because\n" 
						+ ble.getMessage() + "\n" + ble.getStackTrace());
			}
		}
		else if (action.equals(LOCK)) {
			JCheckBoxMenuItem checkBox = (JCheckBoxMenuItem)ev.getSource();
			boolean on = checkBox.getState();
			if (on) 
				owner.lock();
			else 
				owner.unlock();
		}
	}
	
	public synchronized void append(String s, Font f, Color c) { // Better implementation--uses StyleContext
		//StyleContext sc = StyleContext.getDefaultStyleContext( );
		// AttributeSet aset = sc.addAttribute(SimpleAttributeSet.EMPTY,
		//                                    StyleConstants.Foreground, c);
		synchronized(textPane) {
			MutableAttributeSet attrs = textPane.getInputAttributes();


			// Set the font family, size, and style, based on properties of
			// the Font object. Note that JTextPane supports a number of
			// character attributes beyond those supported by the Font class.
			// For example, underline, strike-through, super- and sub-script.
			StyleConstants.setFontFamily(attrs, f.getFamily());
			StyleConstants.setFontSize(attrs, f.getSize());
			StyleConstants.setItalic(attrs, (f.getStyle() & Font.ITALIC) != 0);
			StyleConstants.setBold(attrs, (f.getStyle() & Font.BOLD) != 0);

			// Set the font color
			StyleConstants.setForeground(attrs, c);

			Document doc = textPane.getDocument();
			int end = doc.getLength();
			//int len = textPane.getText().length();
			//String text = textPane.getText();
			try {
				
				if (limitSize && 
						(end + s.length() > maxSize)) {
					doc.remove(0, rolloverVal);
					end -= rolloverVal;
				}
				textPane.setCaretPosition(end);  // Place caret at the end (with no selection).
				textPane.setCharacterAttributes(attrs, false);
				textPane.replaceSelection(s); // There is no selection, so insert at caret.
				
//				// Convert the new end location
//			      // to view co-ordinates
//			      Rectangle r = modelToView(doc.getLength());
//
//			      // Finally, scroll so that the new text is visible
//			      if (r != null) {
//			        scrollRectToVisible(r);
//			      }
			    
			}
//			catch (BadLocationException e) {
//				e.printStackTrace();
//		    }
			catch (Exception e) {
				e.printStackTrace();
			}
		}
	  }
	
	private void checkPopup(MouseEvent e) {
		if (e.isPopupTrigger( )) {
			Object src = e.getSource();
			if (src instanceof PC2TextPane) {
				popup.show(e.getComponent(),
						e.getX(), e.getY());
			}
		}
	}

	public void highlight(String pattern) {
		synchronized(textPane) {
			Highlighter highlighter = textPane.getHighlighter();
			// Highlight a new pattern
			if (highlightPattern == null) {
//				 DefaultStyledDocument 
				boolean firstMatch = true;
				Document document = textPane.getDocument();
//				for (HTMLDocument.Iterator it = document.getIterator(HTML.Tag.CONTENT);     
//				it.isValid(); it.next()) {  
				int offset = document.getStartPosition().getOffset();
				int length = document.getLength();
				while (offset < length) {
					try {    
						// Get as much of the text as we possibly can
						String fragment = document.getText(offset, length);
						int match = fragment.indexOf(pattern, offset);
						while (match != -1 ) {
							// textPane.setCaretPosition(match);
							int matchEnd = match+pattern.length();
							
							if (firstMatch) {
								Object tag = highlighter.addHighlight(match, matchEnd, focus);
								PC2FindMatch fm= new PC2FindMatch(match, tag);
								highlights.add(fm);
								firstMatch = false;
							}
							else {
								Object tag = highlighter.addHighlight(match, matchEnd, painter);
								PC2FindMatch fm= new PC2FindMatch(match, tag);
								highlights.add(fm);
							}
//							MutableAttributeSet attrs = textPane.getInputAttributes();
							//AttributeSet as = textPane.getCharacterAttributes();
//							originalBackGround = StyleConstants.getBackground(attrs);
//							StyleConstants.setBackground(attrs, HIGHLIGHT);
//							((DefaultStyledDocument)document).setCharacterAttributes(match, pattern.length(), attrs, false);
							offset = matchEnd + 1;
							match = fragment.indexOf(pattern, offset);
						}
						// Now that we have highlighted as much of the current region
						// as possible, set the offset to the end of the current region
						// and continue with any others that remain
						offset = fragment.length() +1;
//						//Matcher matcher = pattern.matcher(fragment);
//						while (matcher.find()) { 
//						highlighter.addHighlight(
//						it.getStartOffset() + matcher.start(),
//						it.getStartOffset() + matcher.end(),
//						FIND_HIGHLIGHT_PAINTER); 
//						++matchCount;    
//						}  
					}
					catch (Exception ex) {  

					}
				}
				
				if (highlights.size() > 0) {
					highlightIndex = 0;
					textPane.setCaretPosition(highlights.getFirst().getPosition());
					highlightPattern = pattern;
				}
			}
			else if (highlightPattern != null && highlightPattern.equals(pattern)) {
				nextHighlight();
			}
			else if (highlightPattern != null && !(highlightPattern.equals(pattern))) {
				highlighter.removeAllHighlights();
				highlights.clear();
				highlightPattern = new String(pattern);
			} 
			
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		//checkPopup(e);
	}
	
    @Override
	public void mouseEntered(MouseEvent e) {
    }
    
    @Override
	public void mouseExited(MouseEvent e) {
 
    }
    @Override
	public void mousePressed(MouseEvent e) {
    	// Need to check for popup for Linux/Unix system on press to 
    	// operate on the correct event
    	checkPopup(e);
    }
    @Override
	public void mouseReleased(MouseEvent e) {
    	checkPopup(e);
    }
	public void nextHighlight() {
		synchronized (textPane) {
			
			if (highlights.size() > 0) {
				// First clear the focus highlight from the current position and
				// then move to the next highlight
				updateHighlight(painter, false);
								
				highlightIndex++;
//				 If we have run out of entries wrap back to the top
				if (highlightIndex >= highlights.size())
					highlightIndex = 0;

				PC2FindMatch pos = highlights.get(highlightIndex);
				if (pos != null) {
					updateHighlight(focus, true);
//					fm = highlights.get(highlightIndex);
//					highlighter.removeHighlight(fm.getHighlighter());
//					highlighter.addHighlight(fm.getPosition(), 
//							(fm.getPosition()+highlightPattern.length()), painter);
//					textPane.setCaretPosition(pos);
				}
			}
		}
		
	}
	public void clearHighlight() {
		synchronized (textPane) {
			highlightPattern = null;

			Highlighter highlighter = textPane.getHighlighter();
			if (highlighter != null)
				highlighter.removeAllHighlights();
			highlights.clear();
		}
	}
	
	//	 turn off word wrap
	@Override
	public boolean getScrollableTracksViewportWidth() {
	    return false;
	}
	
	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		//System.out.println("Popup menu will be visible!");
	}
	@Override
	public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
		//System.out.println("Popup menu will be invisible!");
	}
	@Override
	public void popupMenuCanceled(PopupMenuEvent e) {
		// System.out.println("Popup menu is hidden!");
	}
	
	protected void setOwner(PC2Console console) {
		this.owner = console;
	}
	
	private void updateHighlight(DefaultHighlightPainter newPainter,
			boolean moveCursor) {
		try {
			Highlighter highlighter = textPane.getHighlighter();
			PC2FindMatch fm = highlights.get(highlightIndex);
			highlighter.removeHighlight(fm.getTag());
			Object newTag = highlighter.addHighlight(fm.getPosition(), 
				(fm.getPosition()+highlightPattern.length()), newPainter);
			if (newTag != null)
				fm.setHighlighter(newTag);
			if (moveCursor)
				textPane.setCaretPosition(fm.getPosition());
		}
		catch (BadLocationException ble) {
			clearHighlight();
		}
	}
	

}
