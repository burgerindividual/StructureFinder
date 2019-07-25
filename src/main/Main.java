package main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.text.AbstractDocument;

import amidst.mojangapi.minecraftinterface.RecognisedVersion;
import amidst.mojangapi.world.coordinates.CoordinatesInWorld;
import amidst.mojangapi.world.coordinates.Resolution;

public class Main {
	public static final RecognisedVersion VERSION = RecognisedVersion._1_14_3;
	public static final String[] DIMENSIONS = { "Nether", "Overworld" };
	public static final String[] STRUCTURE_TYPES = { "Buried Treasure", "Desert Temple", "End City", "Igloo",
			"Jungle Temple", "Mansion", "Mineshaft", "Monument", "Nether Fortress", "Ocean Ruin", "Pillager Outpost",
			"Shipwreck", "Stronghold", "Swamp Hut", "Village" };
	public static final String[] WORLD_TYPES = { "Default", "Flat", "Large Biomes", "Amplified" };
	public static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	public static Font defaultFont = new Font(Font.decode(null).getName(), Font.PLAIN, 12);
	private static JFrame jframe = new JFrame("Structure Finder");
	private static JPanel jpanel = new JPanel(new GridBagLayout());
	private static JComboBox<String> coordtypebox = new JComboBox<String>(DIMENSIONS);
	private static JComboBox<String> structurebox = new JComboBox<String>(STRUCTURE_TYPES);
	private static JComboBox<String> worldtypebox = new JComboBox<String>(WORLD_TYPES);
	private static JButton jbutton = new JButton("Run");
	private static JTextField seed = new JTextField();
	private static JSpinner radius = new JSpinner(new SpinnerNumberModel(500, 1, 6250, 1));
	private static JSpinner startX = new JSpinner(new SpinnerNumberModel(0, -30000000, 30000000, 1));
	private static JSpinner startZ = new JSpinner(new SpinnerNumberModel(0, -30000000, 30000000, 1));
	private static JCheckBox checkbox = new JCheckBox("Include Unlikely End Cities", false);
	private static JPanel checkboxpanel = new JPanel(new GridBagLayout());
	private static JTextArea output = new JTextArea();
	private static JScrollPane scrollpane = new JScrollPane(output);
	private static JProgressBar progressbar = new JProgressBar();
	private static JLabel lRadius = new JLabel("Radius (Chunks)");
	private static JLabel lStartX = new JLabel("Starting X Pos");
	private static JLabel lStartZ = new JLabel("Starting Z Pos");
	private static JLabel lSeed = new JLabel("Seed");
	private static JLabel lCoordType = new JLabel("Dimension");
	private static JLabel lStructure = new JLabel("Structure");
	private static JLabel lWorldType = new JLabel("World Type");
	private static JPanel seedPanel = new JPanel(new GridBagLayout());
	private static StructureFinder sf;
	private static GridBagConstraints constraints = new GridBagConstraints();

	public static void main(String[] args) {

		System.out.println("Running from Java version " + System.getProperty("java.version"));
		System.out.println("Temp Directory: " + System.getProperty("java.io.tmpdir"));
		File path = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		if (path.getName().contains(".jar") && !path.isDirectory()) {
			if (args.length == 0) {
				try {
					Runtime.getRuntime().exec(new String[] { "java", "-XX:+UseParallelGC", "-XX:GCTimeRatio=19",
							"-Xms64m", "-Xmx384m", "-jar", path.getName(), "test" });
				} catch (IOException ioe) {
					errorProcedure(ioe);
				}
				System.exit(0);
			}
		}
		System.out.println(path);

		StructureFinder.init();
		sf = new StructureFinder(seed.getText(), String.valueOf(worldtypebox.getSelectedItem()),
				String.valueOf(structurebox.getSelectedItem()), (Integer) radius.getValue(),
				CoordinatesInWorld.from((int) startX.getValue(), (int) startX.getValue()), Resolution.CHUNK, false);
		swingSetup();
		initListeners();
	}

	private static void swingSetup() {
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jframe.setSize((int) (screenSize.getHeight() / 1.8), (int) (screenSize.getHeight() / 3.6));
		jframe.setResizable(false);
		jframe.add(jpanel);

		Insets insetDefault = new Insets(jframe.getHeight() / 40, jframe.getWidth() / 50, jframe.getHeight() / 40,
				jframe.getWidth() / 50);

		lStructure.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 0, 1, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lStructure, constraints);

		lWorldType.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 1, 1, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lWorldType, constraints);

		lCoordType.setHorizontalAlignment(SwingConstants.CENTER);
		lCoordType.setEnabled(false);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 2, 1, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lCoordType, constraints);

		lStartX.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 0, 3, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lStartX, constraints);

		lStartZ.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 1, 3, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lStartZ, constraints);

		lRadius.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 2, 3, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lRadius, constraints);

		progressbar.setStringPainted(true);
		setConstraints(insetDefault, GridBagConstraints.BOTH, 0, 0, 3, 1, 0, 0.05, GridBagConstraints.CENTER);
		jpanel.add(progressbar, constraints);

		setConstraints(insetDefault, GridBagConstraints.NONE, 0, 2, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(structurebox, constraints);

		setConstraints(insetDefault, GridBagConstraints.NONE, 1, 2, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(worldtypebox, constraints);

		coordtypebox.setEnabled(false);
		setConstraints(insetDefault, GridBagConstraints.NONE, 2, 2, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(coordtypebox, constraints);

		startX.setPreferredSize(structurebox.getPreferredSize());
		setConstraints(insetDefault, GridBagConstraints.NONE, 0, 4, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(startX, constraints);

		startZ.setPreferredSize(worldtypebox.getPreferredSize());
		setConstraints(insetDefault, GridBagConstraints.NONE, 1, 4, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(startZ, constraints);

		radius.setPreferredSize(coordtypebox.getPreferredSize());
		setConstraints(insetDefault, GridBagConstraints.NONE, 2, 4, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(radius, constraints);
		
		// add jpanel for checkbox to not resize things when it comes into view
		checkbox.setVisible(false);
		checkbox.setFocusPainted(false);
		setConstraints(0, 0, 0, 0, GridBagConstraints.NONE, 0, 0, 0, 0, 0, 0, GridBagConstraints.CENTER);
		checkboxpanel.add(checkbox, constraints);
		
		checkboxpanel.setPreferredSize(checkbox.getPreferredSize());
		setConstraints(0, 0, 0, 0, GridBagConstraints.NONE, 0, 6, 3, 1, 0, 0, GridBagConstraints.CENTER);
		jpanel.add(checkboxpanel, constraints);
		// checkbox panel ends here

		// seperate panel for seed so text is aligned
		lSeed.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 0, 0, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		seedPanel.add(lSeed, constraints);

		((AbstractDocument) seed.getDocument()).setDocumentFilter(new LimitDocumentFilter(32));
		setConstraints(0, jframe.getWidth() / 50, jframe.getHeight() / 20, jframe.getWidth() / 50,
				GridBagConstraints.HORIZONTAL, 0, 1, 1, 1, 1, 0, GridBagConstraints.PAGE_END);
		seedPanel.add(seed, constraints);

		jbutton.setFocusPainted(false);
		setConstraints(0, jframe.getWidth() / 50, jframe.getHeight() / 20, jframe.getWidth() / 50,
				GridBagConstraints.NONE, 1, 1, 1, 1, 0, 0.1, GridBagConstraints.PAGE_END);
		seedPanel.add(jbutton, constraints);

		setConstraints(jframe.getHeight() / 20, 0, 0, 0, GridBagConstraints.BOTH, 0, 7, 3, 1, 0, 0.1,
				GridBagConstraints.PAGE_END);
		jpanel.add(seedPanel, constraints);
		// seed panel ends here

		output.setEditable(false);
		scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		setConstraints(insetDefault, GridBagConstraints.BOTH, 3, 0, 1, 8, 1, 1, GridBagConstraints.CENTER);
		jpanel.add(scrollpane, constraints);

		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			SwingUtilities.updateComponentTreeUI(jframe);
			JFrame.setDefaultLookAndFeelDecorated(true);
		} catch (Exception e) {
			errorProcedure(e);
		}
		progressbar.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
		progressbar.setBackground(new Color(230, 230, 230));
		progressbar.setForeground(new Color(120, 230, 90));
		progressbar.setUI(new BasicProgressBarUI() {
			@Override
			protected Color getSelectionBackground() {
				return new Color(50, 50, 50);
			}

			@Override
			protected Color getSelectionForeground() {
				return new Color(50, 50, 50);
			}
		});
		changeFont(jpanel, defaultFont);
		scrollpane.setPreferredSize(scrollpane.getSize());
		jframe.setVisible(true);
	}

	public static void initListeners() {
		jbutton.addActionListener((e) -> {
			if (!seed.getText().trim().isEmpty()) {
				Resolution res = getResolution();
				try {
					if (isStructTypeNetherFortress() && isCoordTypeNether()) {
						startX.setValue((int) roundToRes(Resolution.NETHER_CHUNK, (int) startX.getValue()));
						startZ.setValue((int) roundToRes(Resolution.NETHER_CHUNK, (int) startZ.getValue()));
					} else {
						startX.setValue((int) roundToRes(res, (int) startX.getValue()));
						startZ.setValue((int) roundToRes(res, (int) startZ.getValue()));
					}
					radius.commitEdit();
					startX.commitEdit();
					startZ.commitEdit();
				} catch (ParseException e1) {
					errorProcedure(e1);
				}
				if (!sf.isAlive()) {
					setIntermediate(true);
					if (isStructTypeNetherFortress() && !isCoordTypeNether()) {
						sf = new StructureFinder(seed.getText(), String.valueOf(worldtypebox.getSelectedItem()),
								String.valueOf(structurebox.getSelectedItem()), (int) radius.getValue() >> 3,
								CoordinatesInWorld.from((int) startX.getValue(), (int) startZ.getValue()), res,
								checkbox.isSelected());
						executeFinder();
					} else {
						sf = new StructureFinder(seed.getText(), String.valueOf(worldtypebox.getSelectedItem()),
								String.valueOf(structurebox.getSelectedItem()), (int) radius.getValue(),
								CoordinatesInWorld.from((int) startX.getValue(), (int) startZ.getValue()), res,
								checkbox.isSelected());
						executeFinder();
					}
				}
			} else {
				appendText("Seed is empty");
			}
		});
		structurebox.addActionListener((e) -> {
			if (isStructTypeNetherFortress()) {
				lCoordType.setEnabled(true);
				coordtypebox.setEnabled(true);
			} else {
				lCoordType.setEnabled(false);
				coordtypebox.setEnabled(false);
			}
			if(String.valueOf(structurebox.getSelectedItem()).equals("End City")) {
				checkbox.setVisible(true);
			} else {
				checkbox.setVisible(false);
			}
		});
	}

	private static void setConstraints(int iTop, int iLeft, int iBottom, int iRight, int fillConst, int gridx,
			int gridy, int gridw, int gridh, double weightx, double weighty, int anchor) {
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

	private static void setConstraints(Insets inset, int fillConst, int gridx, int gridy, int gridw, int gridh,
			double weightx, double weighty, int anchor) {
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

	public static void appendText(String i) {
		output.append(i + "\n");
		output.setCaretPosition(output.getDocument().getLength());
	}

	private static Resolution getResolution() {
		if (isStructTypeNetherFortress() && !isCoordTypeNether()) {
			return Resolution.NETHER_CHUNK;
		}
		return Resolution.CHUNK;
	}

	public static void executeFinder() {
		output.setText("");
		if (String.valueOf(structurebox.getSelectedItem()).equals("Stronghold")) {
			progressbar.setMinimum(0);
			progressbar.setMaximum(127);
		} else {
			progressbar.setMinimum(-(Integer) radius.getValue());
			progressbar.setMaximum((Integer) radius.getValue());
		}
		progressbar.setValue(progressbar.getMinimum());
		sf.start();
	}

	public static JProgressBar getProgressBar() {
		return progressbar;
	}

	public static JTextArea getTextArea() {
		return output;
	}

	public static boolean isCoordTypeNether() {
		return String.valueOf(coordtypebox.getSelectedItem()).equals("Nether");
	}

	public static boolean isStructTypeNetherFortress() {
		return String.valueOf(structurebox.getSelectedItem()).equals("Nether Fortress");
	}

	private static void changeFont(Component component, Font font) {
		component.setFont(font);
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				changeFont(child, font);
			}
		}
	}

	public static long roundToRes(Resolution r, int coord) {
		return r.convertFromThisToWorld(r.convertFromWorldToThis(coord));
	}

	public static void errorProcedure(Exception e) {
		e.printStackTrace();
		SwingUtilities.invokeLater(() -> {
			final JTextArea textArea = new JTextArea();
			textArea.setFont(new Font(Font.decode(null).getName(), Font.PLAIN, 10));
			textArea.setEditable(false);
			StringWriter writer = new StringWriter();
			e.printStackTrace(new PrintWriter(writer));
			textArea.setText(writer.toString());

			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setPreferredSize(
					new Dimension((int) (screenSize.getHeight() / 4), (int) (screenSize.getHeight() / 9)));

			JOptionPane.showMessageDialog(jframe, scrollPane, "Structure Finder: Error", JOptionPane.ERROR_MESSAGE);
		});
	}

	public static void errorProcedure(String s) {
		System.err.print(s);
		SwingUtilities.invokeLater(() -> {
			final JTextArea textArea = new JTextArea();
			textArea.setFont(new Font(Font.decode(null).getName(), Font.PLAIN, 10));
			textArea.setEditable(false);
			textArea.setWrapStyleWord(true);
			textArea.setLineWrap(true);
			textArea.setText(s);

			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setPreferredSize(
					new Dimension((int) (screenSize.getHeight() / 5), (int) (screenSize.getHeight() / 14.4)));

			JOptionPane.showMessageDialog(jframe, scrollPane, "Structure Finder: Error", JOptionPane.ERROR_MESSAGE);
		});
	}

	public static void setIntermediate(boolean b) {
		SwingUtilities.invokeLater(() -> {
			if (b) {
				progressbar.setString("");
			} else {
				progressbar.setString(null);
			}
			progressbar.setIndeterminate(b);
		});
	}
}
