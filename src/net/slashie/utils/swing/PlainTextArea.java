package net.slashie.utils.swing;

import javax.swing.JTextArea;

public class PlainTextArea extends JTextArea{
	public PlainTextArea() {
		setWrapStyleWord(true);
		setLineWrap(true);
		setFocusable(false);
		setEditable(false);
		setOpaque(false);
	}	
}