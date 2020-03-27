package main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.ScrollPaneConstants;
import javax.swing.SortOrder;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableRowSorter;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileSystemView;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.html.HTMLDocument;

import amidst.logging.AmidstLogger;
import amidst.mojangapi.file.DotMinecraftDirectoryNotFoundException;
import amidst.mojangapi.file.MinecraftInstallation;
import amidst.mojangapi.file.VersionList;
import amidst.mojangapi.file.json.ReleaseType;
import amidst.mojangapi.minecraftinterface.RecognisedVersion;
import amidst.mojangapi.world.coordinates.CoordinatesInWorld;
import amidst.mojangapi.world.coordinates.Resolution;
import amidst.parsing.FormatException;
import main.GuiTweaks.ConditionalComboBoxListener;
import main.GuiTweaks.ConditionalComboBoxRenderer;
import main.GuiTweaks.ConditionalString;
import main.GuiTweaks.LimitDocumentFilter;

public class Main {
	public static final String[] DIMENSIONS = { "Nether", "Overworld" };
	public static final String[] WORLD_TYPES = { "Default", "Flat", "Large Biomes", "Amplified" };
	public static final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	public static final Dimension minSize = new Dimension(600, 320);
	public static final Dimension maxSize = new Dimension(800, 320);
	public static final Font defaultFont = new Font(Font.decode(null).getName(), Font.PLAIN, 12);
	private static final JFrame jframe = new JFrame("StructureFinder");
	private static final JPanel jpanel = new JPanel(new GridBagLayout());
	private static final GridBagConstraints constraints = new GridBagConstraints();
	private static final JComboBox<String> coordtypebox = new JComboBox<>(DIMENSIONS);
	private static final JComboBox<ConditionalString> structurebox = new JComboBox<>();
	private static final JComboBox<String> worldtypebox = new JComboBox<>(WORLD_TYPES);
	private static final JButton runButton = new JButton("Run");
	private static final JTextField seed = new JTextField();
	private static final JSpinner radius = new JSpinner(new SpinnerNumberModel(500, 1, 62500, 1));
	private static final JSpinner startX = new JSpinner(new SpinnerNumberModel(0, -30000000, 30000000, 1));
	private static final JSpinner startZ = new JSpinner(new SpinnerNumberModel(0, -30000000, 30000000, 1));
	private static final JCheckBox checkbox = new JCheckBox("Include Unlikely End Cities", false);
	private static final JPanel checkboxpanel = new JPanel(new GridBagLayout());
	private static final JTable output = new JTable();
	private static final JScrollPane scrollpane = new JScrollPane(output);
	private static final JProgressBar progressbar = new JProgressBar();
	private static final JLabel lRadius = new JLabel("Radius (Chunks)");
	private static final JLabel lStartX = new JLabel("Starting X Pos");
	private static final JLabel lStartZ = new JLabel("Starting Z Pos");
	private static final JLabel lSeed = new JLabel("Seed");
	private static final JLabel lCoordType = new JLabel("Dimension");
	private static final JLabel lStructure = new JLabel("Structure");
	private static final JLabel lWorldType = new JLabel("World Type");
	private static final JPanel seedPanel = new JPanel(new GridBagLayout());
	private static final JMenuBar menubar = new JMenuBar();
	private static final JMenu versionMenu = new JMenu("Version");
	private static final JMenu toolsMenu = new JMenu("Tools");
	private static final JMenu exportMenu = new JMenu("Export Structures");
	private static final JMenuItem exportToTxt = new JMenuItem("Export to Text File (TXT) ...");
	private static final JMenuItem exportToCsv = new JMenuItem("Export to Spreadsheet (CSV) ...");
	private static final JMenu helpMenu = new JMenu("Help");
	private static final JMenuItem about = new JMenuItem("About StructureFinder");
	private static final JMenuItem viewLog = new JMenuItem("View Log");
	private static final JCheckBoxMenuItem showTooltips = new JCheckBoxMenuItem("Show Tooltips on Hover", true);
	private static final ButtonGroup versiongroup = new ButtonGroup();
	
	private static StructureFinder sf;
	
	private static String[] lastRunInfo;
	
	private static List<? extends SortKey> lastSortKeys;

	public static void main(String[] args) {
		AmidstLogger.info("Running from Java version " + System.getProperty("java.version") + ", vendor " + System.getProperty("java.vendor"));

		MenuScroller.setScrollerFor(versionMenu, 10);
		try {
			putVersionItemsAndInit(versionMenu, versiongroup, args.length > 0 ? Paths.get(args[0]) : null);
		} catch (DotMinecraftDirectoryNotFoundException e) {
			errorProcedure(".minecraft directory not found", true);
		} catch (Exception e) {
			errorProcedure(e, false);
		}
		
		swingSetup();
		initListeners();
	}

	@SuppressWarnings("unchecked")
	private static void swingSetup() {
		jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		jframe.setMinimumSize(minSize);
		jframe.setMaximumSize(maxSize);
		jframe.add(jpanel);

		Insets insetDefault = new Insets(jframe.getHeight() / 40, jframe.getWidth() / 50, jframe.getHeight() / 40,
				jframe.getWidth() / 50);

		showTooltips.setToolTipText(
					"<html>Enables or disables tooltips.<br>What you are reading right now is a tooltip.</html>");

		versionMenu.setToolTipText(
				"<html>A dropdown for selecting the version used.<br>Versions that are grayed out have not been<br>run from the minecraft launcher before.</html>");
		menubar.add(versionMenu);
		
		exportMenu.setEnabled(false);
		exportMenu.setToolTipText("<html>In order to export structures, there has to be items in the table.</html>");
		exportMenu.add(exportToTxt);
		exportMenu.add(exportToCsv);
		toolsMenu.add(exportMenu);
		menubar.add(toolsMenu);
		
		helpMenu.add(showTooltips);
		helpMenu.addSeparator();
		helpMenu.add(viewLog);
		helpMenu.add(about);
		menubar.add(helpMenu);
		
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 0, 0, 4, 1, 0, 0, GridBagConstraints.PAGE_START);
		jpanel.add(menubar, constraints);

		lStructure.setToolTipText("<html>The structure to be searched for.</html>");
		lStructure.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 0, 2, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lStructure, constraints);

		lWorldType.setToolTipText("<html>The world type used in the search.</html>");
		lWorldType.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 1, 2, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lWorldType, constraints);

		lCoordType.setToolTipText(
				"<html>Only applies to Nether Fortresses.<br><br>If you select \"Overworld\", it will output the<br>location you would put a portal in the<br>Overworld to get to the fortress.<br><br>If you select \"Nether\", it will output the Nether<br>coordinates of the fortress.</html>");
		lCoordType.setHorizontalAlignment(SwingConstants.CENTER);
		lCoordType.setEnabled(false);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 2, 2, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lCoordType, constraints);

		lStartX.setToolTipText("<html>The X position at the middle of the search.</html>");
		lStartX.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 0, 4, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lStartX, constraints);

		lStartZ.setToolTipText("<html>The Z position at the middle of the search.</html>");
		lStartZ.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 1, 4, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lStartZ, constraints);

		lRadius.setToolTipText(
				"<html>The radius (in a square) of the search in chunks.<br>Has a maximum of 62500 chunks, or 1 million blocks.</html>");
		lRadius.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 2, 4, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		jpanel.add(lRadius, constraints);

		progressbar.setStringPainted(true);
		setConstraints(insetDefault, GridBagConstraints.BOTH, 0, 1, 3, 1, 0, 0.05, GridBagConstraints.CENTER);
		jpanel.add(progressbar, constraints);

		structurebox.setToolTipText("<html>The structure to be searched for.</html>");
		setConstraints(insetDefault, GridBagConstraints.NONE, 0, 3, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(structurebox, constraints);

		worldtypebox.setToolTipText("<html>The world type used in the search.</html>");
		setConstraints(insetDefault, GridBagConstraints.NONE, 1, 3, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(worldtypebox, constraints);

		coordtypebox.setToolTipText(
				"<html>Only applies to Nether Fortresses.<br><br>If you select \"Overworld\", it will output the<br>location you would put a portal in the<br>Overworld to get to the fortress.<br><br>If you select \"Nether\", it will output the Nether<br>coordinates of the fortress.</html>");
		coordtypebox.setEnabled(false);
		setConstraints(insetDefault, GridBagConstraints.NONE, 2, 3, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(coordtypebox, constraints);

		startX.setToolTipText("<html>The X position at the middle of the search.</html>");
		startX.setPreferredSize(structurebox.getPreferredSize());
		setConstraints(insetDefault, GridBagConstraints.NONE, 0, 5, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(startX, constraints);

		startZ.setToolTipText("<html>The Z position at the middle of the search.</html>");
		startZ.setPreferredSize(worldtypebox.getPreferredSize());
		setConstraints(insetDefault, GridBagConstraints.NONE, 1, 5, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(startZ, constraints);

		radius.setToolTipText(
				"<html>The radius (in a square) of the search in chunks.<br>Has a maximum of 62500 chunks, or 1 million blocks.</html>");
		radius.setPreferredSize(coordtypebox.getPreferredSize());
		setConstraints(insetDefault, GridBagConstraints.NONE, 2, 5, 1, 1, 0, 0.1, GridBagConstraints.CENTER);
		jpanel.add(radius, constraints);

		// add jpanel for checkbox to not resize things when it comes into view
		checkbox.setVisible(false);
		setConstraints(0, 0, 0, 0, GridBagConstraints.NONE, 0, 1, 0, 0, 0, 0, GridBagConstraints.CENTER);
		checkboxpanel.add(checkbox, constraints);

		checkboxpanel.setPreferredSize(checkbox.getPreferredSize());
		setConstraints(0, 0, 0, 0, GridBagConstraints.NONE, 0, 7, 3, 1, 0, 0, GridBagConstraints.CENTER);
		jpanel.add(checkboxpanel, constraints);
		// checkbox panel ends here

		// seperate panel for seed so text is aligned
		lSeed.setToolTipText("<html>The seed of the world to be searched.</html>");
		lSeed.setHorizontalAlignment(SwingConstants.CENTER);
		setConstraints(0, 0, 0, 0, GridBagConstraints.BOTH, 0, 1, 1, 1, 0, 0, GridBagConstraints.PAGE_END);
		seedPanel.add(lSeed, constraints);

		seed.setToolTipText("<html>The seed of the world to be searched.</html>");
		((AbstractDocument) seed.getDocument()).setDocumentFilter(new LimitDocumentFilter(32));
		setConstraints(0, jframe.getWidth() / 50, jframe.getHeight() / 20, jframe.getWidth() / 50,
				GridBagConstraints.HORIZONTAL, 0, 2, 1, 1, 1, 0, GridBagConstraints.PAGE_END);
		seedPanel.add(seed, constraints);

		runButton.setToolTipText("<html>Click to start / cancel the search.</html>");
		setConstraints(0, jframe.getWidth() / 50, jframe.getHeight() / 20, jframe.getWidth() / 50,
				GridBagConstraints.NONE, 1, 2, 1, 1, 0, 0.1, GridBagConstraints.PAGE_END);
		seedPanel.add(runButton, constraints);

		setConstraints(jframe.getHeight() / 20, 0, 0, 0, GridBagConstraints.BOTH, 0, 8, 3, 1, 0, 0.1,
				GridBagConstraints.PAGE_END);
		jpanel.add(seedPanel, constraints);
		// seed panel ends here

		CoordTableModel tableModel = new CoordTableModel();
		output.setModel(tableModel);
		Action copyAction = new AbstractAction("copyAction") {
			private static final long serialVersionUID = 6540715274968882774L;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() instanceof JTable) {
					JTable table = (JTable) e.getSource();
					StringBuilder contents = new StringBuilder();
					boolean first = true;
					
					for (int i : table.getSelectedRows()) {
						if (first) {
							first = false;
						} else {
							contents.append("\n");
						}
						
						contents.append(((CoordTableModel) table.getModel()).getRow(
										((TableRowSorter<?>)table.getRowSorter()).convertRowIndexToModel(i)
									).toString()
								);
					}
					
					StringSelection selection = new StringSelection(contents.toString());
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
				}
			}
		};
		output.getActionMap().put("copyAction", copyAction);
		output.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("control C"),
				"copyAction");
		output.setRowSorter(new TableRowSorter<CoordTableModel>(tableModel));
		output.getTableHeader().setToolTipText("<html>Click on any one of the column headers to sort by it.</html>");
		output.getTableHeader().setReorderingAllowed(false);
		output.setDefaultRenderer(Float.class, new CoordTableModel.AngleRenderer());
		output.setDefaultRenderer(Byte.class, new CoordTableModel.DirectionRenderer());
		lastSortKeys = Arrays.asList(new RowSorter.SortKey[] { new RowSorter.SortKey(2, SortOrder.ASCENDING) });
		output.getRowSorter().setSortKeys(lastSortKeys);
		scrollpane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollpane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		setConstraints(insetDefault, GridBagConstraints.BOTH, 3, 1, 1, 8, 1, 1, GridBagConstraints.CENTER);
		jpanel.add(scrollpane, constraints);

		try {
			String lafClass = UIManager.getSystemLookAndFeelClassName();
			if (!lafClass.equals("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel")) {
				UIManager.setLookAndFeel(lafClass);
			} else {
				AmidstLogger.warn("Nimbus Look and Feel not supported, reverting to default...");
			}
			SwingUtilities.updateComponentTreeUI(jframe);
			JFrame.setDefaultLookAndFeelDecorated(true);
		} catch (Exception e) {
			errorProcedure(e, false);
		}
		structurebox.setRenderer(new ConditionalComboBoxRenderer());
		UIManager.put("ToolTip.background", UIManager.getColor("window"));
		if (UIManager.getLookAndFeel().getClass().getName()
				.equals("com.sun.java.swing.plaf.windows.WindowsLookAndFeel")) {
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
		}
		ToolTipManager.sharedInstance().setEnabled(showTooltips.isSelected());
		ToolTipManager.sharedInstance().setReshowDelay(0);
		ToolTipManager.sharedInstance().setDismissDelay(60000);

		about.setIcon(new ImageIcon(((ImageIcon) UIManager.getIcon("OptionPane.informationIcon")).getImage()
				.getScaledInstance(about.getPreferredSize().height - about.getIconTextGap(),
						about.getPreferredSize().height - about.getIconTextGap(), Image.SCALE_SMOOTH)));

		viewLog.setIcon(new ImageIcon(((ImageIcon) UIManager.getIcon("FileView.fileIcon")).getImage().getScaledInstance(
				(int) ((viewLog.getPreferredSize().height - viewLog.getIconTextGap()) * 0.8),
				viewLog.getPreferredSize().height - viewLog.getIconTextGap(), Image.SCALE_SMOOTH)));
		
		try {
			File tempTxt = File.createTempFile("tempTxt", ".txt");
			File tempCsv = File.createTempFile("tempCsv", ".csv");
			
			exportToTxt.setIcon(FileSystemView.getFileSystemView().getSystemIcon(tempTxt));
			exportToCsv.setIcon(FileSystemView.getFileSystemView().getSystemIcon(tempCsv));
			
			tempTxt.delete();
			tempCsv.delete();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		changeFont(jpanel, defaultFont);
		setFocus(jpanel, false);
		scrollpane.setPreferredSize(scrollpane.getSize());
		jframe.setSize(jframe.getWidth() + 100 - scrollpane.getWidth(), jframe.getMinimumSize().height);
		jframe.setVisible(true);
	}

	public static void putVersionItemsAndInit(JMenu menu, ButtonGroup group, Path minecraftDirectory)
			throws DotMinecraftDirectoryNotFoundException {
		boolean selected = true;
		List<String> versions = null;
		try {
			versions = VersionList.newRemoteVersionList().getVersions().stream()
					.filter(v -> v.getType().equals(ReleaseType.RELEASE)
							&& !RecognisedVersion.fromName(v.getId()).equals(RecognisedVersion.UNKNOWN))
					.map(v -> v.getId()).collect(Collectors.toList());
		} catch (FormatException | IOException e) {
			AmidstLogger.warn("Error getting versions list from launchermeta.mojang.com");
			try {
				versions = VersionList.newLocalVersionList().getVersions().stream()
						.filter(v -> v.getType().equals(ReleaseType.RELEASE)
								&& !RecognisedVersion.fromName(v.getId()).equals(RecognisedVersion.UNKNOWN))
						.map(v -> v.getId()).collect(Collectors.toList());
			} catch (FormatException | IOException e1) {
				errorProcedure("Error getting versions list from online and local", true);
			}
		}
		String latestVersion = RecognisedVersion.values()[1].getName();
		if (!versions.contains(latestVersion)) versions.add(0, latestVersion);

		MinecraftInstallation minecraftInstallation = minecraftDirectory != null
				? MinecraftInstallation.newLocalMinecraftInstallation(minecraftDirectory)
				: MinecraftInstallation.newLocalMinecraftInstallation();
		for (String v : versions) {
			JVersionMenuItem versionitem = new JVersionMenuItem(v, RecognisedVersion.fromName(v));
			versionitem.addActionListener((e) -> { // create an action listener for every version menu item
				RecognisedVersion ver = getSelectedVersion();
				try {
					sf.dispose();
					sf = new StructureFinder(ver, minecraftInstallation);
					updateStructures(ver);
				} catch (Exception ex) {
					errorProcedure(ex, false);
				}
			});
			menu.add(versionitem);
			group.add(versionitem);
			boolean disabled = false;
			try {
				if (minecraftInstallation.newLauncherProfile(v).getVersionId() == null) { // versions that are installed but don't have a profile get disabled here
					disabled = true;
				}
			} catch (Exception e1) { // versions that aren't installed get disabled here
				disabled = true;
			}

			if (disabled) {
				versionitem.setEnabled(false);
			}

			if (!disabled && selected) {
				versionitem.setSelected(selected);
				try {
					sf = new StructureFinder(versionitem.getVersion(), minecraftInstallation);
					updateStructures(versionitem.getVersion());
				} catch (Exception e) {
					errorProcedure("Error initializing with Minecraft version " + v, true);
				}
				selected = false;
			}
		}
		if (selected) {
			// if selected is still true here, then it never got atleast one version
			errorProcedure("No compatible minecraft versions detected.", true);
		}
	}

	public static void initListeners() {
		runButton.addActionListener(e -> {
			if (runButton.getText().equals("Run")) {
				if (!seed.getText().isEmpty()) {
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
						errorProcedure(e1, false);
					}
					if (!sf.isRunning()) {
						setIntermediate(true);
						if (isStructTypeNetherFortress() && !isCoordTypeNether()) {
							executeFinder(seed.getText(), String.valueOf(worldtypebox.getSelectedItem()),
									String.valueOf(structurebox.getSelectedItem()), (int) radius.getValue() >> 3,
									CoordinatesInWorld.from((int) startX.getValue(), (int) startZ.getValue()), res,
									checkbox.isSelected());
						} else {
							executeFinder(seed.getText(), String.valueOf(worldtypebox.getSelectedItem()),
									String.valueOf(structurebox.getSelectedItem()), (int) radius.getValue(),
									CoordinatesInWorld.from((int) startX.getValue(), (int) startZ.getValue()), res,
									checkbox.isSelected());
						}
						setButtonAction(false);
					}
				} else {
					errorProcedure("Seed is empty", false);
				}
			} else {
				sf.cancel();
				setButtonEnabled(false);
			}
			
		});
		structurebox.addActionListener(e -> {
			if (isStructTypeNetherFortress()) {
				lCoordType.setEnabled(true);
				coordtypebox.setEnabled(true);
			} else {
				lCoordType.setEnabled(false);
				coordtypebox.setEnabled(false);
			}
			if (String.valueOf(structurebox.getSelectedItem()).equals("End City")) {
				checkbox.setVisible(true);
			} else {
				checkbox.setVisible(false);
			}
		});
		about.addActionListener(e -> {
			JEditorPane text = new JEditorPane("text/html",
					"<html>StructureFinder - Quickly finds Minecraft structures<br/><br/>Author: burgerguy / burgerdude<br/>GitHub Link: <a href=\"https://github.com/burgerguy/StructureFinder/\">https://github.com/burgerguy/StructureFinder/</a><br><br>Amidst Author: skiphs<br>Amidst Github Link: <a href=\"https://github.com/toolbox4minecraft/amidst\">https://github.com/toolbox4minecraft/amidst</a></html>");
			String bodyRule = "body { font-family: " + defaultFont.getFamily() + "; " + "font-size: "
					+ defaultFont.getSize() + "pt; }";
			((HTMLDocument) text.getDocument()).getStyleSheet().addRule(bodyRule);
			text.setEditable(false);
			text.setOpaque(false);
			text.addHyperlinkListener(hle -> {
				if (HyperlinkEvent.EventType.ACTIVATED.equals(hle.getEventType())) {
					Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
					if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
						try {
							desktop.browse(hle.getURL().toURI());
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}
				}
			});

			JOptionPane.showMessageDialog(jframe, text, "Structure Finder: About", JOptionPane.INFORMATION_MESSAGE);
		});
		showTooltips.addActionListener(e -> {
			ToolTipManager.sharedInstance().setEnabled(showTooltips.isSelected());
		});
		viewLog.addActionListener(e -> {
			JTextArea logArea = new JTextArea();
			logArea.setFont(new Font(Font.decode(null).getName(), Font.PLAIN, 10));
			logArea.setEditable(false);
			logArea.setWrapStyleWord(true);
			logArea.setLineWrap(true);
			logArea.setText(AmidstLogger.getAllMessages());
			JScrollPane scrollPane = new JScrollPane(logArea);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setPreferredSize(
					new Dimension((int) (screenSize.getHeight() / 1.4), (int) (screenSize.getHeight() / 1.4)));

			JOptionPane.showMessageDialog(jframe, scrollPane, "Structure Finder: Log", JOptionPane.PLAIN_MESSAGE);
		});
		output.getModel().addTableModelListener(e -> {
			if(e.getSource() instanceof CoordTableModel) {
				CoordTableModel m = (CoordTableModel) e.getSource();
				boolean b = !m.isEmpty();
				if(exportMenu.isEnabled() != b) {
					exportMenu.setEnabled(b);
					if(b == true) {
						exportMenu.setToolTipText("<html>Export structure data to a file.</html>");
					} else {
						exportMenu.setToolTipText("<html>In order to export structures, there has to be items in the table.</html>");
					}
				}
			}
		});
		exportToTxt.addActionListener(e -> {
			Path p = FileSaveHelper.saveToPath(jpanel, "txt", lastRunInfo[1].substring(6), lastRunInfo[2].substring(16).toLowerCase().replace(' ', '_'));
			if (p != null) {
				try (PrintWriter out = new PrintWriter(p.toAbsolutePath().toString())) {
					for(String s : lastRunInfo) {
						if(s != null) {
							out.println(s);
						}
					}
					out.println();
					CoordTableModel m = (CoordTableModel) output.getModel();
					for(int i = 0; i < m.getRowCount(); i++) {
						int row = ((TableRowSorter<?>)output.getRowSorter()).convertRowIndexToModel(i);
						out.println(m.getValueAt(row, 0) + "," + m.getValueAt(row, 1));
					}
				} catch (FileNotFoundException e1) {
					errorProcedure(e1, false);
				}
			}
		});
		exportToCsv.addActionListener(e -> {
			Path p = FileSaveHelper.saveToPath(jpanel, "csv", lastRunInfo[1].substring(6), lastRunInfo[2].substring(16).toLowerCase().replace(' ', '_'));
			if (p != null) {
				try (PrintWriter out = new PrintWriter(p.toAbsolutePath().toString())) {
					out.println("X,Z,Distance,F3 Degree,Direction");
					
					CoordTableModel m = (CoordTableModel) output.getModel();
					for(int i = 0; i < m.getRowCount(); i++) {
						int row = ((TableRowSorter<?>)output.getRowSorter()).convertRowIndexToModel(i);
						out.println(m.getValueAt(row, 0) + ","
								+ m.getValueAt(row, 1) + ","
								+ m.getValueAt(row, 2) + ","
								+ CoordData.formatAngle((float) m.getValueAt(row, 3)) + ","
								+ AngleHelper.getIdFromAbbreviation((byte) m.getValueAt(row, 4)));
					}
				} catch (FileNotFoundException e1) {
					errorProcedure(e1, false);
				}
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

	public static void addRow(CoordData cd) {
		SwingUtilities.invokeLater(() -> {
			((CoordTableModel) output.getModel()).addRow(cd);
		});
	}

	private static Resolution getResolution() {
		if (isStructTypeNetherFortress() && !isCoordTypeNether()) {
			return Resolution.NETHER_CHUNK;
		}
		return Resolution.CHUNK;
	}

	public static void executeFinder(String seed, String worldType, String structureType, int radius,
			CoordinatesInWorld startPos, Resolution resolution, boolean unlikelyEndCities) {
		lastRunInfo = new String[] {
				"Version: " + getVersionSelected(),
				"Seed: " + seed,
				"Structure Type: " + structureType,
				"World Type: " + worldType,
				"Search Radius: " + radius,
				"Start Position: " + startPos,
				null,
				null
		};
		if(structureType.equals("End City")) {
			lastRunInfo[7] = "Include Unlikely End Cities: " + unlikelyEndCities;
		} else if(structureType.equals("Nether Fortress")) {
			lastRunInfo[6] = "Dimension Coordinate Type: " + resolution;
		}
		((CoordTableModel) output.getModel()).clearRows();
		if (String.valueOf(structurebox.getSelectedItem()).equals("Stronghold")) {
			progressbar.setMinimum(0);
			progressbar.setMaximum(127);
		} else {
			progressbar.setMinimum(-(Integer) Main.radius.getValue());
			progressbar.setMaximum((Integer) Main.radius.getValue());
		}
		progressbar.setValue(progressbar.getMinimum());
		sf.submit(seed, worldType, structureType, radius, startPos, resolution, unlikelyEndCities);
	}
	
	private static String getVersionSelected() {
		Enumeration<AbstractButton> enumButtons = versiongroup.getElements();
		while (enumButtons.hasMoreElements()) {
			AbstractButton b = enumButtons.nextElement();
			if (b.isSelected()) {
				return b.getText();
			}
		}
		return null;
	}

	public static JProgressBar getProgressBar() {
		return progressbar;
	}

	public static JTable getTextArea() {
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

	private static void setFocus(Component component, boolean focusable) {
		if (component instanceof JComboBox) {
			((JComboBox<?>) component).setFocusable(focusable);
		}
		if (component instanceof AbstractButton) {
			((AbstractButton) component).setFocusPainted(focusable);
		}
		if (component instanceof Container) {
			for (Component child : ((Container) component).getComponents()) {
				setFocus(child, focusable);
			}
		}
	}

	public static long roundToRes(Resolution r, int coord) {
		return r.convertFromThisToWorld(r.convertFromWorldToThis(coord));
	}

	public static void errorProcedure(Exception e, boolean exit) {
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

			setIntermediate(false);
			setChangeVersions(true);

			JOptionPane.showMessageDialog(null, scrollPane, "Structure Finder: Error", JOptionPane.ERROR_MESSAGE);
			if (exit) {
				System.exit(0);
			}
		});
	}

	public static void errorProcedure(String s, boolean exit) {
		if (exit) {
			AmidstLogger.error(s);
		} else {
			AmidstLogger.warn(s);
		}
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

			setIntermediate(false);
			setChangeVersions(true);

			JOptionPane.showMessageDialog(null, scrollPane, "Structure Finder: Error", JOptionPane.ERROR_MESSAGE);
			if (exit) {
				System.exit(0);
			}
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

	public static void setChangeVersions(boolean b) {
		SwingUtilities.invokeLater(() -> {
			versionMenu.setEnabled(b);
		});
	}

	private static RecognisedVersion getSelectedVersion() {
		RecognisedVersion version = null;
		for (Component menuitem : versionMenu.getMenuComponents()) {
			if (((JVersionMenuItem) menuitem).isSelected()) {
				version = ((JVersionMenuItem) menuitem).getVersion();
			}
		}
		return version;
	}

	public static void setButtonAction(boolean t) {
		SwingUtilities.invokeLater(() -> {
			if (t) {
				runButton.setText("Run");
			} else {
				runButton.setText("Cancel");
			}
		});
	}
	
	public static void setButtonEnabled(boolean enabled) {
		SwingUtilities.invokeLater(() -> {
			runButton.setEnabled(enabled);
		});
	}

	private static void updateStructures(RecognisedVersion version) {
		boolean mineshaft = RecognisedVersion.isNewer(version, RecognisedVersion._b1_7_3);
		boolean village = RecognisedVersion.isNewer(version, RecognisedVersion._b1_7_3);
		boolean stronghold = RecognisedVersion.isNewer(version, RecognisedVersion._b1_7_3);
		boolean nether_fortress = RecognisedVersion.isNewerOrEqualTo(version, RecognisedVersion._b1_9_pre1);
		boolean desert_temple = RecognisedVersion.isNewerOrEqualTo(version, RecognisedVersion._12w21a);
		boolean jungle_temple = RecognisedVersion.isNewerOrEqualTo(version, RecognisedVersion._12w22a);
		boolean witch_hut = RecognisedVersion.isNewerOrEqualTo(version, RecognisedVersion._1_4_2); // closest compatible version to 12w40a in amidst
		boolean ocean_monument = RecognisedVersion.isNewerOrEqualTo(version, RecognisedVersion._1_8); // closest compatible version to 14w25a in amidst
		boolean end_city = RecognisedVersion.isNewerOrEqualTo(version, RecognisedVersion._15w32c); // closest compatible version to 15w31a in amidst
		boolean igloo = RecognisedVersion.isNewer(version, RecognisedVersion._15w42a);
		boolean mansion = RecognisedVersion.isNewer(version, RecognisedVersion._16w38a);
		boolean ocean_ruin = RecognisedVersion.isNewerOrEqualTo(version, RecognisedVersion._18w09a);
		boolean buried_treasure = RecognisedVersion.isNewer(version, RecognisedVersion._18w09a);
		boolean shipwreck = RecognisedVersion.isNewerOrEqualTo(version, RecognisedVersion._18w11a);
		boolean pillager_outpost = RecognisedVersion.isNewer(version, RecognisedVersion._18w46a);

		ConditionalString[] structureTypes = {
					new ConditionalString("Mineshaft", mineshaft),
					new ConditionalString("Village", village), new ConditionalString("Stronghold", stronghold),
					new ConditionalString("Nether Fortress", nether_fortress),
					new ConditionalString("Desert Temple", desert_temple),
					new ConditionalString("Jungle Temple", jungle_temple), new ConditionalString("Witch Hut", witch_hut),
					new ConditionalString("Ocean Monument", ocean_monument), new ConditionalString("End City", end_city),
					new ConditionalString("Igloo", igloo), new ConditionalString("Mansion", mansion),
					new ConditionalString("Ocean Ruin", ocean_ruin),
					new ConditionalString("Buried Treasure", buried_treasure),
					new ConditionalString("Shipwreck", shipwreck),
					new ConditionalString("Pillager Outpost", pillager_outpost)
				};

		DefaultComboBoxModel<ConditionalString> model = new DefaultComboBoxModel<>(structureTypes);
		structurebox.setModel(model);
		structurebox.addActionListener(new ConditionalComboBoxListener(structurebox));
	}
}
