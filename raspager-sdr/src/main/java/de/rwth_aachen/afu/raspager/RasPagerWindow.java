package de.rwth_aachen.afu.raspager;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.List;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.system.SystemInfo.BoardType;

import de.rwth_aachen.afu.raspager.sdr.SerialPortComm;

public class RasPagerWindow extends JFrame {
	private static final Logger log = Logger.getLogger(RasPagerWindow.class.getName());
	private static final long serialVersionUID = 1L;

	private JPanel main;
	private final int WIDTH = 633;
	private final int HEIGHT = 450;

	private TrayIcon trayIcon;

	private JLabel correctionActual;
	private JSlider correctionSlider;
	private List masterList;
	private JLabel statusDisplay;
	private JTextField searchStepWidth;
	private JButton startButton;
	private JTextField masterIP;
	private JTextField port;
	private Canvas slotDisplay;
	private JPanel serialPanel;
	private JPanel gpioPanel;
	private JComboBox<String> serialPortList;
	private JComboBox<String> serialPin;
	private JCheckBox invert;
	private JTextField delay;
	private JComboBox<String> raspiList;
	private JComboBox<String> gpioList;
	private JButton btnGpioPins;
	private JRadioButton radioUseSerial;
	private JRadioButton radioUseGpio;
	private JComboBox<Mixer.Info> soundDeviceList;

	private JButton searchStart;
	private JButton searchStop;
	private JTextField searchAddress;

	private final RasPagerService app;
	// private final Configuration config;
	// private final SDRTransmitter transmitter;
	private TimeSlots timeSlots = new TimeSlots();
	private final ResourceBundle texts;

	// constructor
	public RasPagerWindow(RasPagerService app) {
		this.app = app;

		// Load locale stuff
		texts = ResourceBundle.getBundle("MainWindow");

		// set window preferences
		setTitle("FunkrufSlave");
		setResizable(false);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		// window listener
		addWindowListener(new WindowListener() {
			@Override
			public void windowActivated(WindowEvent arg0) {
			}

			@Override
			public void windowClosed(WindowEvent arg0) {
				System.exit(0);
			}

			@Override
			public void windowClosing(WindowEvent event) {
				if (app.isRunning() && !showConfirmResource("askQuitTitle", "askQuitText")) {
					return;
				}

				if (app.isRunning()) {
					app.stopServer(false);
				}

				dispose();
			}

			@Override
			public void windowDeactivated(WindowEvent arg0) {
			}

			@Override
			public void windowDeiconified(WindowEvent arg0) {
				setVisible(true);
			}

			@Override
			public void windowIconified(WindowEvent arg0) {
				setVisible(false);
			}

			@Override
			public void windowOpened(WindowEvent arg0) {
			}
		});

		// main panel
		main = new JPanel(null);
		main.setPreferredSize(new Dimension(840, 470));
		main.setBounds(0, 0, WIDTH, HEIGHT);
		getContentPane().add(main, BorderLayout.SOUTH);

		// correction slider bounds
		Rectangle correctionSliderBounds = new Rectangle(100, 68, 30, 260);

		// correction slider label
		JLabel correctionLabel = new JLabel(texts.getString("correctionLabel"));
		correctionLabel.setBounds(correctionSliderBounds.x - 20, correctionSliderBounds.y - 38, 80, 18);
		main.add(correctionLabel);

		// correction slider actual position
		correctionActual = new JLabel("0.00");
		correctionActual.setHorizontalAlignment(SwingConstants.CENTER);
		correctionActual.setBounds(correctionSliderBounds.x - 20, correctionSliderBounds.y - 20, 68, 18);
		main.add(correctionActual);

		// correction slider position 1
		JLabel correctionPosition0 = new JLabel("1");
		correctionPosition0.setBounds(correctionSliderBounds.x - 20, correctionSliderBounds.y, 18, 18);
		correctionPosition0.setHorizontalAlignment(SwingConstants.RIGHT);
		main.add(correctionPosition0);

		// correction slider position 0
		JLabel correctionPosition1 = new JLabel("0");
		correctionPosition1.setBounds(correctionSliderBounds.x - 20,
				correctionSliderBounds.y + correctionSliderBounds.height / 2 - 9, 18, 18);
		correctionPosition1.setHorizontalAlignment(SwingConstants.RIGHT);
		main.add(correctionPosition1);

		// correction slider position -1
		JLabel correctionPosition2 = new JLabel("-1");
		correctionPosition2.setBounds(correctionSliderBounds.x - 20,
				correctionSliderBounds.y + correctionSliderBounds.height - 18, 18, 18);
		correctionPosition2.setHorizontalAlignment(SwingConstants.RIGHT);
		main.add(correctionPosition2);

		// correction slider
		correctionSlider = new JSlider(SwingConstants.VERTICAL, -100, 100, 0);
		correctionSlider.setBounds(correctionSliderBounds);
		correctionSlider.addChangeListener((e) -> {
			correctionActual.setText(String.format("%+5.2f", correctionSlider.getValue() / 100.));
			app.getTransmitter().setCorrection(correctionSlider.getValue() / 100.0f);
		});
		main.add(correctionSlider);

		// search run label
		JLabel searchLabel = new JLabel(texts.getString("searchLabel"));
		searchLabel.setBounds(200, 414, 100, 18);
		main.add(searchLabel);

		// search run start
		searchStart = new JButton(texts.getString("searchStart"));
		searchStart.setBounds(200, 434, 70, 18);
		searchStart.addActionListener((e) -> {
			runSearch(true);
		});
		main.add(searchStart);

		// search run stop
		searchStop = new JButton(texts.getString("searchStop"));
		searchStop.setBounds(275, 434, 70, 18);
		searchStop.setEnabled(false);
		searchStop.addActionListener((e) -> {
			app.stopSearching();
		});
		main.add(searchStop);

		// search run step label
		JLabel searchStepLabel = new JLabel(texts.getString("searchStepLabel"));
		searchStepLabel.setBounds(350, 414, 100, 18);
		main.add(searchStepLabel);

		// search run step
		searchStepWidth = new JTextField();
		searchStepWidth.setBounds(new Rectangle(350, 434, 80, 18));
		searchStepWidth.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent event) {
				char key = event.getKeyChar();
				if ((key > '9' || key < '0') && key != '.') {
					event.consume();
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyPressed(KeyEvent arg0) {
			}
		});
		main.add(searchStepWidth);

		// search address label
		JLabel searchAddressLabel = new JLabel(texts.getString("searchAddressLabel"));
		searchAddressLabel.setBounds(455, 414, 120, 18);
		main.add(searchAddressLabel);

		// search address
		searchAddress = new JTextField();
		searchAddress.setBounds(455, 434, 100, 18);
		searchAddress.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent event) {
				char key = event.getKeyChar();
				if ((key > '9' || key < '0')) {
					event.consume();
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyPressed(KeyEvent arg0) {
			}
		});
		main.add(searchAddress);

		// slot display bounds
		Rectangle slotDisplayBounds = new Rectangle(10, 68, 30, 260);

		// slot display label
		JLabel slotDisplayLabel = new JLabel(texts.getString("slotDisplayLabel"));
		slotDisplayLabel.setBounds(slotDisplayBounds.x - 2, slotDisplayBounds.y - 38, 50, 18);
		main.add(slotDisplayLabel);

		// slot display
		slotDisplay = new Canvas() {
			private static final long serialVersionUID = 1L;

			@Override
			public void paint(Graphics g) {
				super.paint(g);
				int width = getWidth() - 1;
				int height = getHeight() - 1;
				int x = 15;

				// draw border
				g.drawRect(x, 0, width - x, height);

				int step = getHeight() / 16;

				for (int i = 0, y = step; i < 16; y += step, i++) {

					Font font = g.getFont();
					Color color = g.getColor();

					// if this is allowed slot
					if (timeSlots.get(i)) {
						// change font and color
						g.setFont(new Font(font.getFontName(), Font.BOLD, font.getSize()));
						g.setColor(Color.green);
					}

					g.drawString("" + Integer.toHexString(i).toUpperCase(), 0, y);
					g.setFont(font);
					g.setColor(color);

					// draw line
					if (i < 16 - 1) {
						g.drawLine(x, y, width, y);
					}

				}

				// if scheduler does not exist, function ends here
				// TODO fix
				// if (state.scheduler == null) {
				// return;
				// }
				return;

				// Color color = g.getColor();
				// g.setColor(Color.green);
				//
				// // get slot count
				// int slot = TimeSlots.getSlotIndex(state.scheduler.getTime());
				// int slotCount = timeSlots.getSlotCount(String.format("%1x",
				// slot).charAt(0));
				//
				// // draw current slots (from slot to slot + slotCount) with
				// // different color
				// for (int i = 0; i < slotCount; i++) {
				// g.fillRect(x + 1, (slot + i) * step + 1, width - x - 1, step
				// - 1);
				// }
				//
				// g.setColor(Color.yellow);
				//
				// g.fillRect(x + 1, slot * step + 1, width - x - 1, step - 1);
				//
				// g.setColor(color);
			}
		};
		slotDisplay.setBounds(slotDisplayBounds);
		main.add(slotDisplay);

		// status display label
		JLabel statusDisplayLabel = new JLabel(texts.getString("statusDisplayLabel"));
		statusDisplayLabel.setBounds(200, 10, 60, 18);
		main.add(statusDisplayLabel);

		// status display
		statusDisplay = new JLabel(texts.getString("statusDisplayDis"));
		statusDisplay.setBounds(new Rectangle(263, 10, 120, 18));
		main.add(statusDisplay);

		// server start button
		startButton = new JButton(texts.getString("startButtonStart"));
		startButton.addActionListener((e) -> {
			if (app.isRunning()) {
				app.stopServer(false);
				startButton.setText(texts.getString("startButtonStart"));

			} else {
				app.startServer(false);
				startButton.setText(texts.getString("startButtonStop"));
			}
		});
		startButton.setBounds(new Rectangle(675, 10, 150, 18));
		main.add(startButton);

		// configuration panel
		JPanel configurationPanel = new JPanel(null);
		configurationPanel.setBorder(new TitledBorder(null, texts.getString("configurationPanel"), TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		configurationPanel.setBounds(new Rectangle(200, 30, 625, 372));
		main.add(configurationPanel);

		// master list bounds
		Rectangle masterListBounds = new Rectangle(0, 30, 150, 200);

		// master list label
		JLabel masterListLabel = new JLabel(texts.getString("masterListLabel"));
		masterListLabel.setBounds(12, 20, 70, 18);
		configurationPanel.add(masterListLabel);

		// master list
		masterList = new List();
		masterList.setName("masterList");

		// master list pane
		JScrollPane masterListPane = new JScrollPane(masterList);
		masterListPane.setBounds(new Rectangle(12, 38, 150, 218));
		configurationPanel.add(masterListPane);

		// serial delay label
		JLabel serialDelayLabel = new JLabel(texts.getString("serialDelayLabel"));
		serialDelayLabel.setBounds(174, 292, 50, 18);
		configurationPanel.add(serialDelayLabel);

		// serial delay
		delay = new JTextField();
		delay.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent event) {
				char key = event.getKeyChar();

				// check if key is between 0 and 9
				if (key > '9' || key < '0') {
					event.consume();
				}
			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyPressed(KeyEvent arg0) {
			}
		});
		delay.setBounds(265, 292, 50, 18);
		configurationPanel.add(delay);

		// serial delay ms
		JLabel serialDelayMs = new JLabel("ms");
		serialDelayMs.setBounds(317, 292, 40, 18);
		configurationPanel.add(serialDelayMs);

		// port bounds
		Rectangle portBounds = new Rectangle(50, masterListBounds.y + masterListBounds.height + 15, 50, 18);

		// port label
		JLabel portLabel = new JLabel("Port:");
		portLabel.setBounds(12, 268, 50, 18);
		configurationPanel.add(portLabel);

		// port
		port = new JTextField();
		port.setBounds(new Rectangle(50, 268, 50, 18));
		port.addKeyListener(new KeyListener() {

			@Override
			public void keyTyped(KeyEvent event) {
				char key = event.getKeyChar();

				// check if key is between 0 and 9
				if (key > '9' || key < '0') {
					event.consume();
				}

			}

			@Override
			public void keyReleased(KeyEvent arg0) {
			}

			@Override
			public void keyPressed(KeyEvent arg0) {
			}
		});
		configurationPanel.add(port);

		// sounddevice
		JLabel soundDeviceLabel = new JLabel(texts.getString("soundDeviceLabel"));
		soundDeviceLabel.setBounds(174, 318, 100, 15);
		configurationPanel.add(soundDeviceLabel);

		soundDeviceList = new JComboBox<>();
		soundDeviceList.setBounds(265, 316, 349, 18);
		Mixer.Info[] soundDevices = AudioSystem.getMixerInfo();
		for (Mixer.Info device : soundDevices) {
			soundDeviceList.addItem(device);
		}
		configurationPanel.add(soundDeviceList);

		// config button bounds
		Rectangle configButtonBounds = new Rectangle(0, portBounds.y + portBounds.height + 20, 130, 18);

		// config apply button
		JButton applyButton = new JButton(texts.getString("applyButton"));
		applyButton.addActionListener((e) -> {
			setConfig();
		});
		applyButton.setBounds(new Rectangle(12, 345, 130, 18));
		configurationPanel.add(applyButton);

		configButtonBounds.x += configButtonBounds.width + 10;
		configButtonBounds.width = 100;

		// config load button
		JButton loadButton = new JButton(texts.getString("loadButton"));
		loadButton.addActionListener((event) -> {
			JFileChooser fileChooser = new JFileChooser("");
			if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();

				try {
					app.getConfig().load(file.getPath());
				} catch (Exception e) {
					log.log(Level.SEVERE, "Invalid configuration file.", e);
					showErrorResource("invalidConfigTitle", "invalidConfigText");

					return;
				}

				loadConfig();
			}
		});

		loadButton.setBounds(new Rectangle(153, 345, 100, 18));
		configurationPanel.add(loadButton);

		configButtonBounds.x += configButtonBounds.width + 10;
		configButtonBounds.width = 120;

		// config save button
		JButton saveButton = new JButton(texts.getString("saveButton"));
		saveButton.addActionListener((event) -> {
			JFileChooser fileChooser = new JFileChooser("");
			if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();

				try {
					setConfig();
					app.getConfig().save(file.getPath());
				} catch (Exception ex) {
					log.log(Level.SEVERE, "Failed to save configuration file.", ex);
					showErrorResource("failedConfigTitle", "failedConfigText");

					return;
				}
			}
		});

		saveButton.setBounds(new Rectangle(265, 345, 110, 18));
		configurationPanel.add(saveButton);

		JPanel masterPanel = new JPanel();
		masterPanel.setBorder(new TitledBorder(null, texts.getString("masterPanel"), TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		masterPanel.setBounds(174, 22, 183, 92);
		configurationPanel.add(masterPanel);
		masterPanel.setLayout(null);

		// master name field
		masterIP = new JTextField();
		masterIP.setBounds(12, 20, 159, 18);
		masterPanel.add(masterIP);

		// master add button
		JButton masterAdd = new JButton(texts.getString("masterAdd"));
		masterAdd.setBounds(12, 42, 159, 18);
		masterPanel.add(masterAdd);

		// master remove button
		JButton masterRemove = new JButton(texts.getString("masterRemove"));
		masterRemove.setBounds(12, 64, 159, 18);
		masterPanel.add(masterRemove);

		// serial invert
		invert = new JCheckBox(texts.getString("invert"));
		invert.setBounds(174, 268, 141, 18);
		configurationPanel.add(invert);

		JPanel pttPanel = new JPanel();
		pttPanel.setBorder(new TitledBorder(null, texts.getString("pttPanel"), TitledBorder.LEADING, TitledBorder.TOP,
				null, null));
		pttPanel.setBounds(174, 126, 440, 130);
		configurationPanel.add(pttPanel);
		pttPanel.setLayout(null);

		serialPanel = new JPanel();
		serialPanel.setBounds(12, 20, 183, 100);
		pttPanel.add(serialPanel);
		serialPanel.setBorder(new TitledBorder(null, texts.getString("serialPanel"), TitledBorder.LEADING,
				TitledBorder.TOP, null, null));
		serialPanel.setLayout(null);

		// serial port
		serialPortList = new JComboBox<>();
		serialPortList.setBounds(12, 20, 151, 18);
		serialPanel.add(serialPortList);

		// serial pin
		serialPin = new JComboBox<>();
		serialPin.setBounds(12, 42, 151, 18);
		serialPanel.add(serialPin);
		serialPin.addItem("DTR"); // index 0 = SerialPortComm.DTR
		serialPin.addItem("RTS");

		radioUseSerial = new JRadioButton("");
		radioUseSerial.setSelected(true);
		radioUseSerial.setBounds(154, 69, 21, 23);
		radioUseSerial.setEnabled(false);
		serialPanel.add(radioUseSerial);

		gpioPanel = new JPanel();
		gpioPanel.setBounds(201, 20, 227, 100);
		pttPanel.add(gpioPanel);
		gpioPanel.setBorder(new TitledBorder(null, texts.getString("gpioPanel"), TitledBorder.LEADING, TitledBorder.TOP,
				null, null));
		gpioPanel.setLayout(null);
		gpioPanel.setEnabled(false);

		raspiList = new JComboBox<>();
		raspiList.setBounds(12, 20, 203, 18);
		gpioPanel.add(raspiList);
		raspiList.addItem(texts.getString("itemDeactivated"));
		raspiList.setEnabled(false);

		gpioList = new JComboBox<>();
		gpioList.setBounds(12, 42, 203, 18);
		gpioPanel.add(gpioList);
		gpioList.addItem(texts.getString("itemDeactivated"));
		gpioList.setEnabled(false);

		btnGpioPins = new JButton(texts.getString("btnGpioPins"));
		btnGpioPins.setBounds(12, 70, 115, 18);
		gpioPanel.add(btnGpioPins);
		btnGpioPins.setEnabled(false);

		radioUseGpio = new JRadioButton("");
		radioUseGpio.setBounds(194, 69, 21, 23);
		gpioPanel.add(radioUseGpio);
		radioUseGpio.setEnabled(true);

		radioUseGpio.addActionListener((e) -> {
			if (radioUseGpio.isSelected()) {
				radioUseGpio.setEnabled(false);
				radioUseSerial.setEnabled(true);
				radioUseSerial.setSelected(false);
				gpioPanel.setEnabled(true);
				serialPanel.setEnabled(false);
				raspiList.setEnabled(true);
				gpioList.setEnabled(true);
				btnGpioPins.setEnabled(true);

				serialPortList.setEnabled(false);
				serialPin.setEnabled(false);
			}
		});
		btnGpioPins.addActionListener((e) -> {
			EventQueue.invokeLater(() -> {
				try {
					new GpioView().setVisible(true);
				} catch (Exception ex) {
					log.log(Level.SEVERE, "Failed to open GPIO view.", ex);
				}
			});
		});

		raspiList.addActionListener((e) -> {
			gpioList.removeAllItems();
			gpioList.addItem(texts.getString("itemDeactivated"));

			BoardType type = BoardType.valueOf(raspiList.getSelectedItem().toString());
			for (Pin p : RaspiPin.allPins(type)) {
				gpioList.addItem(p.toString());
			}
		});

		radioUseSerial.addActionListener((e) -> {
			if (radioUseSerial.isSelected()) {
				radioUseSerial.setEnabled(false);
				radioUseGpio.setEnabled(true);
				radioUseGpio.setSelected(false);
				serialPanel.setEnabled(true);
				gpioPanel.setEnabled(false);
				serialPortList.setEnabled(true);
				serialPin.setEnabled(true);

				raspiList.setEnabled(false);
				gpioList.setEnabled(false);
				btnGpioPins.setEnabled(false);
			}
		});

		java.util.List<String> serialPorts = SerialPortComm.getPorts();
		for (int i = 0; i < serialPorts.size(); i++) {
			serialPortList.addItem(serialPorts.get(i));
		}

		for (BoardType bt : BoardType.values()) {
			raspiList.addItem(bt.toString());
		}

		masterRemove.addActionListener((e) -> {
			if (masterList.getSelectedItem() != null && showConfirmResource("delMasterTitle", "delMasterText")) {
				masterList.remove(masterList.getSelectedIndex());
			}
		});

		masterAdd.addActionListener((e) -> {
			String master = masterIP.getText();
			if (master.isEmpty()) {
				return;
			}

			// check if master is already in list
			for (String m : masterList.getItems()) {
				if (m.equalsIgnoreCase(master)) {
					showErrorResource("addMasterFailTitle", "addMasterFailText");
					return;
				}
			}

			masterList.add(master);
			masterIP.setText("");
		});

		// show window
		pack();
		setVisible(true);

		loadConfig();

		// create tray icon
		Image trayImage = Toolkit.getDefaultToolkit().getImage("icon.ico");

		PopupMenu trayMenu = new PopupMenu(texts.getString("trayMenu"));
		MenuItem menuItem = new MenuItem(texts.getString("trayMenuShow"));
		menuItem.addActionListener((e) -> {
			setExtendedState(Frame.NORMAL);
			setVisible(true);
		});
		trayMenu.add(menuItem);

		trayIcon = new TrayIcon(trayImage, "RasPager", trayMenu);
		try {
			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
			log.warning("Failed to add tray icon.");
		}

	}

	// set connection status
	public void setStatus(boolean connected) {
		if (connected) {
			statusDisplay.setText(texts.getString("statusDisplayCon"));
		} else {
			statusDisplay.setText(texts.getString("statusDisplayDis"));
		}
	}

	// run search
	public void runSearch(boolean run) {
		if (searchAddress.getText().isEmpty()) {
			showErrorResource("searchErrorTitle", "noSearchAddress");
			return;
		}

		// TODO What?
		if (run && app.isServerRunning()) {
			if (app.isServerRunning()) {
				if (!showConfirmResource("searchRunningTitle", "searchRunningText")) {
					return;
				}

				app.stopServer(false);
			}

			app.startScheduler(true);

			searchStart.setEnabled(false);
			searchStop.setEnabled(true);

			searchStepWidth.setEditable(false);
			searchAddress.setEditable(false);

		} else {
			app.stopScheduler();

			searchStart.setEnabled(true);
			searchStop.setEnabled(false);

			searchStepWidth.setEditable(true);
			searchAddress.setEditable(true);
		}
	}

	public void setConfig() {
		Configuration config = app.getConfig();

		config.setInt(ConfigKeys.NET_PORT, Integer.parseInt(port.getText()));
		config.setString(ConfigKeys.NET_MASTERS, String.join(" ", masterList.getItems()));

		if (!radioUseSerial.isSelected()) {
			config.setBoolean(ConfigKeys.SERIAL_USE, false);
		} else {
			config.setBoolean(ConfigKeys.SERIAL_USE, true);
			config.setString(ConfigKeys.SERIAL_PORT, serialPortList.getSelectedItem().toString());
			config.setString(ConfigKeys.SERIAL_PIN, serialPin.getSelectedItem().toString());
		}

		if (!radioUseGpio.isSelected()) {
			config.setBoolean(ConfigKeys.GPIO_USE, false);
		} else {
			config.setBoolean(ConfigKeys.GPIO_USE, true);
			config.setString(ConfigKeys.GPIO_PIN, gpioList.getSelectedItem().toString());
			config.setString(ConfigKeys.GPIO_RASPI_REV, raspiList.getSelectedItem().toString());
		}

		config.setBoolean(ConfigKeys.INVERT, invert.isSelected());
		config.setInt(ConfigKeys.TX_DELAY, Integer.parseInt(delay.getText()));
		config.setString(ConfigKeys.SDR_DEVICE, soundDeviceList.getSelectedItem().toString());
		config.setFloat(ConfigKeys.SDR_CORRECTION, correctionSlider.getValue() / 100.0f);

		if (app.isRunning()) {
			if (showConfirmResource("cfgRunningTitle", "cfgRunningText")) {
				app.stopServer(false);
				app.startServer(false);

				startButton.setText("Server stoppen");
			}
		}
	}

	public void loadConfig() {
		Configuration config = app.getConfig();

		port.setText(Integer.toString(config.getInt(ConfigKeys.NET_PORT, 1337)));

		// load masters
		masterList.removeAll();
		String value = config.getString(ConfigKeys.NET_MASTERS, null);
		if (value != null && !value.isEmpty()) {
			Arrays.stream(value.split(" +")).forEach((m) -> masterList.add(m));
		}

		// load serial
		serialPortList.setSelectedItem(config.getString(ConfigKeys.SERIAL_PORT, null));
		serialPin.setSelectedItem(config.getString(ConfigKeys.SERIAL_PIN, null));
		delay.setText(Integer.toString(config.getInt(ConfigKeys.TX_DELAY, 0)));

		// load raspi / gpio
		gpioList.removeAllItems();
		gpioList.addItem(texts.getString("itemDeactivated"));

		invert.setSelected(config.getBoolean(ConfigKeys.INVERT, false));

		if (config.getBoolean(ConfigKeys.SERIAL_USE, false)) {
			radioUseSerial.setSelected(true);
			radioUseSerial.setEnabled(false);
			radioUseGpio.setEnabled(true);
			radioUseGpio.setSelected(false);
			serialPanel.setEnabled(true);
			gpioPanel.setEnabled(false);
			serialPortList.setEnabled(true);
			serialPin.setEnabled(true);

			raspiList.setEnabled(false);
			gpioList.setEnabled(false);
			btnGpioPins.setEnabled(false);
		} else {
			radioUseGpio.setSelected(true);
			radioUseGpio.setEnabled(false);
			radioUseSerial.setEnabled(true);
			radioUseSerial.setSelected(false);
			gpioPanel.setEnabled(true);
			serialPanel.setEnabled(false);
			raspiList.setEnabled(true);
			gpioList.setEnabled(true);
			btnGpioPins.setEnabled(true);

			serialPortList.setEnabled(false);
			serialPin.setEnabled(false);
		}

		value = config.getString(ConfigKeys.GPIO_RASPI_REV, null);
		if (value != null) {
			BoardType type = BoardType.valueOf(value);
			raspiList.setSelectedItem(type.toString());

			for (Pin p : RaspiPin.allPins(type)) {
				gpioList.addItem(p.getName());
			}

			gpioList.setSelectedItem(config.getString(ConfigKeys.GPIO_PIN, null));
		}

		soundDeviceList.setSelectedItem(config.getString(ConfigKeys.SDR_DEVICE, null));
		// Correction is loaded by transmitter
		updateCorrection();
	}

	public boolean useSerial() {
		return this.radioUseSerial.isSelected();
	}

	public boolean useGpio() {
		return this.radioUseGpio.isSelected();
	}

	// reset buttons
	public void resetButtons() {
		startButton.setText(texts.getString("startButtonStart"));

		searchStart.setEnabled(true);
		searchStop.setEnabled(false);

		searchStepWidth.setEditable(true);
		searchAddress.setEditable(true);

		setStatus(false);
	}

	public void showError(String title, String message) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
	}

	private void showErrorResource(String title, String text) {
		JOptionPane.showMessageDialog(null, texts.getString(text), texts.getString(title), JOptionPane.ERROR_MESSAGE);
	}

	private boolean showConfirmResource(String title, String message) {
		return JOptionPane.showConfirmDialog(this, texts.getString(message), texts.getString(title),
				JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

	public void updateTimeSlots(TimeSlots slots) {
		this.timeSlots = slots;
		slotDisplay.repaint();
	}

	public String getStepWidth() {
		if (searchStepWidth.getText().isEmpty()) {
			searchStepWidth.setText(Float.toString(app.getSearchStepSize()));
		}

		return searchStepWidth.getText();
	}

	public String getSkyperAddress() {
		return searchAddress.getText();
	}

	public void updateCorrection() {
		float correction = app.getTransmitter().getCorrection();
		correctionActual.setText(String.format("%+4.2f", correction));
		correctionSlider.setValue((int) (correction * 100));
	}
}