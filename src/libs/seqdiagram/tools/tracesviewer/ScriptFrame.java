/*
 * ScriptFrame.java
 *
 * Created on April 30, 2002, 11:50 AM
 */
package tools.tracesviewer;

import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author  deruelle
 * @version
 */
public class ScriptFrame extends Dialog {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	public TextArea infoTextArea;
	public Container container;
	public Button ok;

	/** Creates new ScriptFrame */
	public ScriptFrame() {
		super(new Frame(), " Auxiliary information ", false);
		initComponents();
	}

	public void initComponents() {

		setLayout(new BorderLayout());
		setBackground(Color.lightGray);
		setSize(512, 384);

		/**********************************************************************/

		infoTextArea = new TextArea();
		infoTextArea.setEditable(false);
		infoTextArea.setBackground(Color.white);
		add(infoTextArea, BorderLayout.CENTER);

		ok = new Button("  Ok  ");
		ok.setBackground(Color.lightGray);
		ok.setForeground(Color.black);
		add(ok, BorderLayout.SOUTH);
		ok.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				setVisible(false);
				dispose();
			}
		});

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				setVisible(false);

				dispose();

			}
		});

		setVisible(false);
	}

	public void setText(String host, String text) {
		setTitle("Auxiliary information for " + host);
		infoTextArea.setText(text);
	}

}
