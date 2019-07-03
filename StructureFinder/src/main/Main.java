package main;

import java.awt.Color;
import java.awt.Dimension;
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
	private static JLabel lRadius = new JLabel("Radius (Chunks)");
	private static JLabel lStartX = new JLabel("Starting X Pos");
	private static JLabel lStartZ = new JLabel("Starting Z Pos");
	private static JLabel lSeed = new JLabel("Seed");
	private static JLabel lDimension = new JLabel("Dimension (WIP)");
	private static JLabel lStructure = new JLabel("Structure");
	private static JLabel lWorldType = new JLabel("World Type");
	private static JPanel seedPanel = new JPanel(new GridBagLayout());
	private static StructureFinder sf;
	private static GridBagConstraints constraints = new GridBagConstraints();

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
		
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// jframe.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		jframe.setSize((int) (screenSize.width / 3.2), (int) (screenSize.height / 3.6));
		jframe.setResizable(false);
		jframe.add(jpanel);
		
		Insets insetDefault = new Insets(jframe.getHeight() / 40, jframe.getWidth() / 50, jframe.getHeight() / 40,
				jframe.getWidth() / 50);
		
		lDimension.setHorizontalAlignment(JLabel.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 0, 1, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lDimension, constraints);
		
		lStructure.setHorizontalAlignment(JLabel.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 1, 1, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lStructure, constraints);
		
		lWorldType.setHorizontalAlignment(JLabel.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 2, 1, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lWorldType, constraints);
		
		lRadius.setHorizontalAlignment(JLabel.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 0, 3, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lRadius, constraints);
		
		lStartX.setHorizontalAlignment(JLabel.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 1, 3, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lStartX, constraints);
		
		lStartZ.setHorizontalAlignment(JLabel.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 2, 3, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lStartZ, constraints);
		
		progressbar.setForeground(new Color(120, 230, 90));
		progressbar.setStringPainted(true);
		setConstraints(insetDefault, GridBagConstraints.BOTH, 0, 0, 3, 1, 0, 0.05, GridBagConstraints.CENTER);
		jpanel.add(progressbar, constraints);
		
		setConstraints(insetDefault, GridBagConstraints.NONE, 0, 2, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(dimensionbox, constraints);
		
		setConstraints(insetDefault, GridBagConstraints.NONE, 1, 2, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(structurebox, constraints);
		
		setConstraints(insetDefault, GridBagConstraints.NONE, 2, 2, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(worldtypebox, constraints);
		
		radius.setPreferredSize(dimensionbox.getPreferredSize());
		setConstraints(insetDefault, GridBagConstraints.NONE, 0, 4, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(radius, constraints);
		
		startX.setPreferredSize(structurebox.getPreferredSize());
		setConstraints(insetDefault, GridBagConstraints.NONE, 1, 4, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(startX, constraints);
		
		startZ.setPreferredSize(worldtypebox.getPreferredSize());
		setConstraints(insetDefault, GridBagConstraints.NONE, 2, 4, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(startZ, constraints);
		
		// seperate panel for seed so text is aligned
		lSeed.setHorizontalAlignment(JLabel.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 0, 0, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		seedPanel.add(lSeed, constraints);
		
		setConstraints(jframe.getHeight() / 40, jframe.getWidth() / 50, jframe.getHeight() / 20, jframe.getWidth() / 50, GridBagConstraints.HORIZONTAL, 0, 1, 1, 1, 1, 0, GridBagConstraints.PAGE_END);
		seedPanel.add(seed, constraints);
		
		setConstraints(jframe.getHeight() / 8, 0, 0, 0, GridBagConstraints.BOTH, 0, 6, 2, 1, 0, 0.1, GridBagConstraints.PAGE_END);
		jpanel.add(seedPanel, constraints);
		// seed panel ends here
		
		jbutton.setBorderPainted(false);
		setConstraints(0, jframe.getWidth() / 50, jframe.getHeight() / 20, jframe.getWidth() / 50, GridBagConstraints.NONE, 2, 6, 1, 1, 0, 0.1, GridBagConstraints.PAGE_END);
		jpanel.add(jbutton, constraints);
		
		output.setSize(scrollpane.getSize());
		output.setEditable(false);
		scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		setConstraints(insetDefault, GridBagConstraints.BOTH, 3, 0, 1, 7, 1, 1, GridBagConstraints.CENTER);
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
	
	private static void setConstraints(int iTop, int iLeft, int iBottom, int iRight, int fillConst, int gridx, int gridy, int gridw, int gridh, double weightx, double weighty, int anchor) {
		constraints.insets = new Insets(iTop, iLeft, iBottom, iRight);
		constraints.fill = fillConst;
		constraints.gridx = gridx;
		constraints.gridy = gridy;
		constraints.gridwidth = gridw;
		constraints.gridheight = gridh;
		constraints.weightx = weightx;
		constraints.weighty = weighty;
		constraints.anchor = anchor;
	}
	
	private static void setConstraints(Insets inset, int fillConst, int gridx, int gridy, int gridw, int gridh, double weightx, double weighty, int anchor) {
		constraints.insets = inset;
		constraints.fill = fillConst;
		constraints.gridx = gridx;
		constraints.gridy = gridy;
		constraints.gridwidth = gridw;
		constraints.gridheight = gridh;
		constraints.weightx = weightx;
		constraints.weighty = weighty;
		constraints.anchor = anchor;
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
