package main;

import javax.swing.JRadioButtonMenuItem;

import amidst.mojangapi.minecraftinterface.RecognisedVersion;

public class JVersionMenuItem extends JRadioButtonMenuItem {
	private static final long serialVersionUID = -2349441153770290734L;
	private final RecognisedVersion version;
	
	public JVersionMenuItem(String text, RecognisedVersion version) {
		super(text);
		this.version = version;
	}
	
	public RecognisedVersion getVersion() {
		return version;
	}
}
