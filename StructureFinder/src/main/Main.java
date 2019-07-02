package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.Border;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import amidst.mojangapi.world.coordinates.CoordinatesInWorld;

public class Main {
	public static final String[] DIMENSIONS = { "Overworld", "Nether", "End" };
	public static final String[] STRUCTURE_TYPES = { "Village", "Mineshaft", "Mansion", "Jungle Temple",
			"Desert Temple", "Igloo", "Shipwreck", "Swamp Hut", "Stronghold", "Monument", "Ocean Ruin",
			"Nether Fortress", "End City"}; // Amidst doesn't support buried treasure
	public static final String[] WORLD_TYPES = { "Default", "Flat", "Large Biomes", "Amplified" };
	public static final Border emptyBorder = BorderFactory.createEmptyBorder();
	private static JFrame jframe = new JFrame("Structure Finder");
	private static JPanel jpanel = new JPanel(new GridBagLayout());
	private static JComboBox<?> dimensionbox = new JComboBox<Object>(DIMENSIONS);
	private static JComboBox<?> structurebox = new JComboBox<Object>(STRUCTURE_TYPES);
	private static JComboBox<?> worldtypebox = new JComboBox<Object>(WORLD_TYPES);
	private static JButton jbutton = new JButton("Run");
	private static JTextField seed = new JTextField();
	private static JSpinner radius = new JSpinner(new SpinnerNumberModel(500, 1, 6250, 1));
	private static JSpinner startX = new JSpinner(new SpinnerNumberModel(0, -30000000, 30000000, 1));
	private static JSpinner startZ = new JSpinner(new SpinnerNumberModel(0, -30000000, 30000000, 1));
	private static JTextPane output = new JTextPane();
	private static JScrollPane scrollpane = new JScrollPane(output);
	public static JProgressBar progressbar = new JProgressBar();
	private static JLabel lRadius = new JLabel("Radius (in Chunks)");
	private static JLabel lStartX = new JLabel("Starting X Pos");
	private static JLabel lStartZ = new JLabel("Starting Z Pos");
	private static JLabel lSeed = new JLabel("Seed");
	private static JLabel lDimension = new JLabel("Dimension (WIP)");
	private static JLabel lStructure = new JLabel("Structure");
	private static JLabel lWorldType = new JLabel("World Type");
	private static StructureFinder sf;

	public static void main(String[] args) {
		StructureFinder.init();
		sf = new StructureFinder(seed.getText(), String.valueOf(worldtypebox.getSelectedItem()),
				String.valueOf(structurebox.getSelectedItem()), (Integer) radius.getValue(), CoordinatesInWorld
						.from(((Integer) startX.getValue()).longValue(), ((Integer) startX.getValue()).longValue()));
		swingSetup();
		initListeners();
	}

	public static void swingSetup() {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Font font = new Font("Arial", Font.BOLD, screenSize.height / 90);
		lRadius.setFont(font);
		lStartX.setFont(font);
		lStartZ.setFont(font);
		lSeed.setFont(font);
		lDimension.setFont(font);
		lStructure.setFont(font);
		lWorldType.setFont(font);
		GridBagConstraints constraints = new GridBagConstraints();
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jframe.setSize((int) (screenSize.width / 3.2), (int) (screenSize.height / 3.6));
		jframe.setResizable(false);
		jframe.add(jpanel);
		// constraints.ipadx = jframe.getWidth() / 20;
		// constraints.ipadx = jframe.getHeight() / 20;
		constraints.insets = new Insets(jframe.getHeight() / 40, jframe.getWidth() / 50, jframe.getHeight() / 40,
				jframe.getWidth() / 50);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.gridwidth = 3;
		constraints.weighty = 0.05;
		progressbar.setForeground(new Color(120, 230, 90));
		progressbar.setStringPainted(true);
		jpanel.add(progressbar, constraints);
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.PAGE_END;
		constraints.gridx = 0;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		constraints.weighty = 0.1;
		jpanel.add(dimensionbox, constraints);
		constraints.weighty = 0;
		constraints.anchor = GridBagConstraints.PAGE_START;
		jpanel.add(lDimension, constraints);
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.PAGE_END;
		constraints.gridx = 1;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		constraints.weighty = 0.1;
		jpanel.add(structurebox, constraints);
		constraints.weighty = 0;
		constraints.anchor = GridBagConstraints.PAGE_START;
		jpanel.add(lStructure, constraints);
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.PAGE_END;
		constraints.gridx = 2;
		constraints.gridy = 1;
		constraints.gridwidth = 1;
		constraints.weighty = 0.1;
		jpanel.add(worldtypebox, constraints);
		constraints.weighty = 0;
		constraints.anchor = GridBagConstraints.PAGE_START;
		jpanel.add(lWorldType, constraints);
		radius.setPreferredSize(dimensionbox.getPreferredSize());
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.PAGE_END;
		constraints.gridx = 0;
		constraints.gridy = 2;
		constraints.gridwidth = 1;
		constraints.weighty = 0.1;
		jpanel.add(radius, constraints);
		constraints.weighty = 0;
		constraints.anchor = GridBagConstraints.PAGE_START;
		jpanel.add(lRadius, constraints);
		startX.setPreferredSize(structurebox.getPreferredSize());
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.PAGE_END;
		constraints.gridx = 1;
		constraints.gridy = 2;
		constraints.gridwidth = 1;
		constraints.weighty = 0.1;
		jpanel.add(startX, constraints);
		constraints.weighty = 0;
		constraints.anchor = GridBagConstraints.PAGE_START;
		jpanel.add(lStartX, constraints);
		startZ.setPreferredSize(worldtypebox.getPreferredSize());
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.PAGE_END;
		constraints.gridx = 2;
		constraints.gridy = 2;
		constraints.gridwidth = 1;
		constraints.weighty = 0.1;
		jpanel.add(startZ, constraints);
		constraints.weighty = 0;
		constraints.anchor = GridBagConstraints.PAGE_START;
		jpanel.add(lStartZ, constraints);
		constraints.insets = new Insets(jframe.getHeight() / 10, jframe.getWidth() / 50, jframe.getHeight() / 20,
				jframe.getWidth() / 50);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.PAGE_END;
		constraints.gridx = 0;
		constraints.gridy = 3;
		constraints.gridwidth = 2;
		constraints.weighty = 0.2;
		jpanel.add(seed, constraints);
		constraints.insets = new Insets(jframe.getHeight() / 15, 0, 0, 0);
		constraints.weighty = 0;
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.CENTER;
		jpanel.add(lSeed, constraints);
		constraints.insets = new Insets(jframe.getHeight() / 10, jframe.getWidth() / 50, jframe.getHeight() / 20,
				jframe.getWidth() / 50);
		constraints.fill = GridBagConstraints.NONE;
		constraints.anchor = GridBagConstraints.PAGE_END;
		constraints.gridx = 2;
		constraints.gridy = 3;
		constraints.gridwidth = 1;
		constraints.weighty = 0.2;
		jbutton.setBorderPainted(false);
		jpanel.add(jbutton, constraints);
		constraints.insets = new Insets(jframe.getHeight() / 40, jframe.getWidth() / 50, jframe.getHeight() / 40,
				jframe.getWidth() / 50);
		output.setSize(scrollpane.getSize());
		output.setEditable(false);
		scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		constraints.fill = GridBagConstraints.BOTH;
		constraints.gridx = 3;
		constraints.gridy = 0;
		constraints.gridwidth = 1;
		constraints.gridheight = 4;
		constraints.weightx = 1;
		constraints.weighty = 1;
		jpanel.add(scrollpane, constraints);
		jframe.setVisible(true);
	}

	public static void initListeners() {
		jbutton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!seed.getText().trim().isEmpty()) {
					try {
						startX.setValue(((Integer) startX.getValue() >> 4) << 4);
						startZ.setValue(((Integer) startZ.getValue() >> 4) << 4);
						radius.commitEdit();
						startX.commitEdit();
						startZ.commitEdit();
					} catch (ParseException e1) {
						e1.printStackTrace();
					}
					if (!sf.isAlive()) {
						sf = new StructureFinder(seed.getText(), String.valueOf(worldtypebox.getSelectedItem()),
								String.valueOf(structurebox.getSelectedItem()), (Integer) radius.getValue(),
								CoordinatesInWorld.from((Integer) startX.getValue() >> 4,
										(Integer) startZ.getValue() >> 4));
						executeFinder();
					}
				} else {
					appendText("Seed is empty", Color.RED);
				}
			}
		});
	}

	public static void appendText(String i, Color c) {
		StyledDocument doc = output.getStyledDocument();
		Style style = output.addStyle("", null);
		StyleConstants.setForeground(style, c);
		try {
			doc.insertString(doc.getLength(), i + "\n", style);
		} catch (BadLocationException exc) {
			exc.printStackTrace();
		}
		output.setCaretPosition(output.getDocument().getLength());
	}

	public static void appendText(String i) {
		StyledDocument doc = output.getStyledDocument();
		Style style = output.addStyle("", null);
		StyleConstants.setForeground(style, Color.BLACK);
		try {
			doc.insertString(doc.getLength(), i + "\n", style);
		} catch (BadLocationException exc) {
			exc.printStackTrace();
		}
		output.setCaretPosition(output.getDocument().getLength());
	}

	public static void executeFinder() {
		output.setText("");
		progressbar.setMinimum(-(Integer) radius.getValue());
		progressbar.setMaximum((Integer) radius.getValue());
		sf.start();
	}

	public static JProgressBar getProgressBar() {
		return progressbar;
	}

}
