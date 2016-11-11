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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.system.SystemInfo.BoardType;

import de.rwth_aachen.afu.raspager.sdr.SDRTransmitter;
import de.rwth_aachen.afu.raspager.sdr.SerialPortComm;

public class MainWindow extends JFrame {
	private static final Logger log = Logger.getLogger(MainWindow.class.getName());
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
	private JPanel panel_serial;
	private JPanel panel_gpio;
	private JComboBox<String> serialPortList;
	private JComboBox<String> serialPin;
	private JCheckBox invert;
	private JTextField delay;
	private JComboBox raspiList;
	private JComboBox gpioList;
	private JButton btnGpiopins;
	private JRadioButton radioUseSerial;
	private JRadioButton radioUseGpio;
	private JComboBox soundDeviceList;

	private JButton searchStart;
	private JButton searchStop;
	private JTextField searchAddress;

	private final Configuration config;
	private final SDRTransmitter transmitter;
	private TimeSlots timeSlots = null;

	// constructor
	public MainWindow(Configuration config, SDRTransmitter transmitter) {
		this.config = config;
		this.transmitter = transmitter;

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
				// if server is running, ask to quit
				if (Main.running
						&& !showConfirm("Beenden", "Der Server laeuft zur Zeit. Wollen Sie wirklich beenden?")) {
					return;
				}

				// if server is running
				if (Main.running) {
					// stop server
					Main.stopServer(false);
				}

				// dispose window
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
		JLabel correctionLabel = new JLabel("Korrektur");
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
			transmitter.setCorrection(correctionSlider.getValue() / 100.0f);
		});
		main.add(correctionSlider);

		// search run label
		JLabel searchLabel = new JLabel("Suchlauf");
		searchLabel.setBounds(200, 414, 100, 18);
		main.add(searchLabel);

		// search run start
		searchStart = new JButton("Start");
		searchStart.setBounds(200, 434, 70, 18);
		searchStart.addActionListener((e) -> {
			runSearch(true);
		});
		main.add(searchStart);

		// search run stop
		searchStop = new JButton("Stop");
		searchStop.setBounds(275, 434, 70, 18);
		searchStop.setEnabled(false);
		searchStop.addActionListener((e) -> {
			Main.stopSearching();
		});
		main.add(searchStop);

		// search run step label
		JLabel searchStepLabel = new JLabel("Schrittweite:");
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
		JLabel searchAddressLabel = new JLabel("Skyper-Adresse:");
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
		JLabel slotDisplayLabel = new JLabel("Slots");
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
					if (timeSlots.getSlot(i)) {
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

		// status display bounds
		Rectangle statusDisplayBounds = new Rectangle(320, 10, 120, 18);

		// status display label
		JLabel statusDisplayLabel = new JLabel("Status:");
		statusDisplayLabel.setBounds(200, 10, 60, 18);
		main.add(statusDisplayLabel);

		// status display
		statusDisplay = new JLabel("getrennt");
		statusDisplay.setBounds(new Rectangle(263, 10, 120, 18));
		main.add(statusDisplay);

		// server start button bounds
		Rectangle startButtonBounds = new Rectangle(statusDisplayBounds.x + statusDisplayBounds.width + 20,
				statusDisplayBounds.y, 150, 18);

		// server start button
		startButton = new JButton("Server starten");
		startButton.addActionListener((e) -> {
			if (Main.running) {
				Main.stopServer(false);
				startButton.setText("Server starten");

			} else {
				Main.startServer(false);
				startButton.setText("Server stoppen");
			}
		});
		startButton.setBounds(new Rectangle(675, 10, 150, 18));
		main.add(startButton);

		// configuration panel
		JPanel configurationPanel = new JPanel(null);
		configurationPanel
				.setBorder(new TitledBorder(null, "Konfiguration", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		configurationPanel.setBounds(new Rectangle(200, 30, 625, 372));
		main.add(configurationPanel);

		// master list bounds
		Rectangle masterListBounds = new Rectangle(0, 30, 150, 200);

		// master list label
		JLabel masterListLabel = new JLabel("Master");
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
		JLabel serialDelayLabel = new JLabel("Delay:");
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
		JLabel lblSoundkarte = new JLabel("Soundgerät:");
		lblSoundkarte.setBounds(174, 318, 100, 15);
		configurationPanel.add(lblSoundkarte);

		soundDeviceList = new JComboBox();
		soundDeviceList.setBounds(265, 316, 349, 18);
		Mixer.Info[] soundDevices = AudioSystem.getMixerInfo();
		for (Mixer.Info device : soundDevices) {
			soundDeviceList.addItem(device);
		}
		configurationPanel.add(soundDeviceList);

		// config button bounds
		Rectangle configButtonBounds = new Rectangle(0, portBounds.y + portBounds.height + 20, 130, 18);

		// config apply button
		JButton applyButton = new JButton("Übernehmen");
		applyButton.addActionListener((e) -> {
			setConfig();
		});
		applyButton.setBounds(new Rectangle(12, 345, 130, 18));
		configurationPanel.add(applyButton);

		configButtonBounds.x += configButtonBounds.width + 10;
		configButtonBounds.width = 100;

		// config load button
		JButton loadButton = new JButton("Laden");
		loadButton.addActionListener((event) -> {
			JFileChooser fileChooser = new JFileChooser("");
			if (fileChooser.showOpenDialog(Main.mainWindow) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();

				try {
					config.load(file.getPath());
				} catch (Exception e) {
					log.log(Level.SEVERE, "Invalid configuration file.", e);
					showError("Config laden", "Die Datei ist keine gueltige Config-Datei!");

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
		JButton saveButton = new JButton("Speichern");
		saveButton.addActionListener((event) -> {
			JFileChooser fileChooser = new JFileChooser("");
			if (fileChooser.showSaveDialog(Main.mainWindow) == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();

				try {
					setConfig();
					config.save(file.getPath());
				} catch (Exception ex) {
					log.log(Level.SEVERE, "Failed to save configuration file.", ex);
					showError("Config speichern", "Die Datei konnte nicht gespeichert werden!");

					return;
				}
			}
		});

		saveButton.setBounds(new Rectangle(265, 345, 110, 18));
		configurationPanel.add(saveButton);

		JPanel panel_2 = new JPanel();
		panel_2.setBorder(new TitledBorder(null, "Neuer Master", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_2.setBounds(174, 22, 183, 92);
		configurationPanel.add(panel_2);
		panel_2.setLayout(null);

		// master name field
		masterIP = new JTextField();
		masterIP.setBounds(12, 20, 159, 18);
		panel_2.add(masterIP);

		// master add button
		JButton masterAdd = new JButton("Hinzufügen");
		masterAdd.setBounds(12, 42, 159, 18);
		panel_2.add(masterAdd);

		// master remove button
		JButton masterRemove = new JButton("Löschen");
		masterRemove.setBounds(12, 64, 159, 18);
		panel_2.add(masterRemove);

		// serial invert
		invert = new JCheckBox("Invertieren");
		invert.setBounds(174, 268, 141, 18);
		configurationPanel.add(invert);

		JPanel panel = new JPanel();
		panel.setBorder(new TitledBorder(null, "PTT-Steuerung", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel.setBounds(174, 126, 440, 130);
		configurationPanel.add(panel);
		panel.setLayout(null);

		panel_serial = new JPanel();
		panel_serial.setBounds(12, 20, 183, 100);
		panel.add(panel_serial);
		panel_serial.setBorder(
				new TitledBorder(null, "Serieller Port", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_serial.setLayout(null);

		// serial port
		serialPortList = new JComboBox<>();
		serialPortList.setBounds(12, 20, 151, 18);
		panel_serial.add(serialPortList);

		// serial pin
		serialPin = new JComboBox<>();
		serialPin.setBounds(12, 42, 151, 18);
		panel_serial.add(serialPin);
		serialPin.addItem("DTR"); // index 0 = SerialPortComm.DTR
		serialPin.addItem("RTS");

		radioUseSerial = new JRadioButton("");
		radioUseSerial.setSelected(true);
		radioUseSerial.setBounds(154, 69, 21, 23);
		radioUseSerial.setEnabled(false);
		panel_serial.add(radioUseSerial);

		panel_gpio = new JPanel();
		panel_gpio.setBounds(201, 20, 227, 100);
		panel.add(panel_gpio);
		panel_gpio.setBorder(
				new TitledBorder(null, "GPIO-Pin (RasPi)", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		panel_gpio.setLayout(null);
		panel_gpio.setEnabled(false);

		raspiList = new JComboBox();
		raspiList.setBounds(12, 20, 203, 18);
		panel_gpio.add(raspiList);
		raspiList.addItem("Deaktiviert");
		raspiList.setEnabled(false);

		gpioList = new JComboBox();
		gpioList.setBounds(12, 42, 203, 18);
		panel_gpio.add(gpioList);
		gpioList.addItem("Deaktiviert");
		gpioList.setEnabled(false);

		btnGpiopins = new JButton("GPIO-Pins");
		btnGpiopins.setBounds(12, 70, 115, 18);
		panel_gpio.add(btnGpiopins);
		btnGpiopins.setEnabled(false);

		radioUseGpio = new JRadioButton("");
		radioUseGpio.setBounds(194, 69, 21, 23);
		panel_gpio.add(radioUseGpio);
		radioUseGpio.setEnabled(true);

		radioUseGpio.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (radioUseGpio.isSelected()) {
					radioUseGpio.setEnabled(false);
					radioUseSerial.setEnabled(true);
					radioUseSerial.setSelected(false);
					panel_gpio.setEnabled(true);
					panel_serial.setEnabled(false);
					raspiList.setEnabled(true);
					gpioList.setEnabled(true);
					btnGpiopins.setEnabled(true);

					serialPortList.setEnabled(false);
					serialPin.setEnabled(false);
				}
			}
		});
		btnGpiopins.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							new GpioView().setVisible(true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			}
		});

		raspiList.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				gpioList.removeAllItems();
				gpioList.addItem("Deaktiviert");

				if (!(raspiList.getSelectedItem() instanceof BoardType))
					return;

				for (Pin p : RaspiPin.allPins((BoardType) raspiList.getSelectedItem())) {
					gpioList.addItem(p);
				}
			}
		});

		radioUseSerial.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (radioUseSerial.isSelected()) {
					radioUseSerial.setEnabled(false);
					radioUseGpio.setEnabled(true);
					radioUseGpio.setSelected(false);
					panel_serial.setEnabled(true);
					panel_gpio.setEnabled(false);
					serialPortList.setEnabled(true);
					serialPin.setEnabled(true);

					raspiList.setEnabled(false);
					gpioList.setEnabled(false);
					btnGpiopins.setEnabled(false);
				}
			}
		});

		// get available serial ports and add them to list
		ArrayList<String> serialPorts = SerialPortComm.getPorts();
		for (int i = 0; i < serialPorts.size(); i++) {
			serialPortList.addItem(serialPorts.get(i));
		}

		for (BoardType bt : BoardType.values()) {
			raspiList.addItem(bt);
		}

		masterRemove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// check if there is a selection
				if (masterList.getSelectedItem() != null) {
					// ask to remove
					if (showConfirm("Master loeschen", "Soll der ausgewaehlt Master wirklich geloescht werden?")) {
						// remove master
						masterList.remove(masterList.getSelectedIndex());
					}
				}

			}
		});

		// serial port bounds
		Rectangle serialPortBounds = new Rectangle(masterAdd.getX(),
				masterRemove.getY() + masterRemove.getHeight() + 40, 100, 18);
		masterAdd.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {

				String master = masterIP.getText();
				String[] masters = masterList.getItems();

				// is textfield empty?
				if (master.equals("")) {
					return;
				}

				// check if master is already in list
				for (int i = 0; i < masters.length; i++) {
					if (masters[i].equals(master)) {

						showError("Master hinzufuegen", "Master ist bereits in der Liste vorhanden!");
						return;

					}
				}

				// add master
				masterList.add(master);
				// clear textfield
				masterIP.setText("");

			}

		});

		// show window
		pack();
		setVisible(true);

		loadConfig();

		// create tray icon
		Image trayImage = Toolkit.getDefaultToolkit().getImage("icon.ico");

		PopupMenu trayMenu = new PopupMenu("FunkrufSlave");
		MenuItem menuItem = new MenuItem("Anzeigen");
		menuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// show window
				setExtendedState(Frame.NORMAL);
				setVisible(true);
			}

		});
		trayMenu.add(menuItem);

		trayIcon = new TrayIcon(trayImage, "FunkrufSlave", trayMenu);
		try {
			SystemTray.getSystemTray().add(trayIcon);
		} catch (AWTException e) {
			log.warning("Failed to add tray icon.");
		}

	}

	// set connection status
	public void setStatus(boolean status) {
		this.statusDisplay.setText(status ? "verbunden" : "getrennt");
	}

	// run search
	public void runSearch(boolean run) {
		if (searchAddress.getText().equals("")) {
			showError("Suchlauf - Fehler",
					"Damit der Suchlauf funktioniert, muss eine Skyper-Adresse eingegeben werden!");
			return;
		}

		if (run) {
			if (Main.server != null) {
				if (!showConfirm("Suchlauf",
						"Um einen Suchlauf durchzufuehren, darf der Server nicht laufen. Wollen Sie den Server jetzt beenden?")) {
					return;
				}

				Main.stopServer(false);
			}

			Main.startScheduler(true);

			searchStart.setEnabled(false);
			searchStop.setEnabled(true);

			searchStepWidth.setEditable(false);
			searchAddress.setEditable(false);

		} else {
			Main.stopScheduler();

			searchStart.setEnabled(true);
			searchStop.setEnabled(false);

			searchStepWidth.setEditable(true);
			searchAddress.setEditable(true);
		}
	}

	public void setConfig() {
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

		if (Main.running) {
			if (showConfirm("Config uebernehmen",
					"Der Server laeuft bereits. Um die Einstellungen zu uebernehmen, muss der Server neugestartet werden. Soll er jetzt neugestartet werden?")) {
				Main.stopServer(false);
				Main.startServer(false);

				startButton.setText("Server stoppen");
			}
		}
	}

	public void loadConfig() {
		port.setText(Integer.toString(config.getInt(ConfigKeys.NET_PORT, 1337)));

		// load masters
		masterList.removeAll();
		String value = config.getString(ConfigKeys.NET_MASTERS);
		if (value != null && !value.isEmpty()) {
			Arrays.stream(value.split(" +")).forEach((m) -> masterList.add(m));
		}

		// load serial
		serialPortList.setSelectedItem(config.getString(ConfigKeys.SERIAL_PORT));
		serialPin.setSelectedItem(config.getString(ConfigKeys.SERIAL_PIN));
		delay.setText(Integer.toString(config.getInt(ConfigKeys.TX_DELAY, 0)));

		// load raspi / gpio
		gpioList.removeAllItems();
		gpioList.addItem("Deaktiviert");

		invert.setSelected(config.getBoolean(ConfigKeys.INVERT, false));

		if (config.getBoolean(ConfigKeys.SERIAL_USE, false)) {
			radioUseSerial.setSelected(true);
			radioUseSerial.setEnabled(false);
			radioUseGpio.setEnabled(true);
			radioUseGpio.setSelected(false);
			panel_serial.setEnabled(true);
			panel_gpio.setEnabled(false);
			serialPortList.setEnabled(true);
			serialPin.setEnabled(true);

			raspiList.setEnabled(false);
			gpioList.setEnabled(false);
			btnGpiopins.setEnabled(false);
		} else {
			radioUseGpio.setSelected(true);
			radioUseGpio.setEnabled(false);
			radioUseSerial.setEnabled(true);
			radioUseSerial.setSelected(false);
			panel_gpio.setEnabled(true);
			panel_serial.setEnabled(false);
			raspiList.setEnabled(true);
			gpioList.setEnabled(true);
			btnGpiopins.setEnabled(true);

			serialPortList.setEnabled(false);
			serialPin.setEnabled(false);
		}

		value = config.getString(ConfigKeys.GPIO_RASPI_REV);
		if (value != null) {
			raspiList.setSelectedItem(value);
			// TODO impl
			// for (Pin p : RaspiPin.allPins(Main.config.getRaspi())) {
			// gpioList.addItem(p);
			// }
			gpioList.setSelectedItem(config.getString(ConfigKeys.GPIO_PIN));
		}

		soundDeviceList.setSelectedItem(config.getString(ConfigKeys.SDR_DEVICE));

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
		startButton.setText("Server starten");

		searchStart.setEnabled(true);
		searchStop.setEnabled(false);

		searchStepWidth.setEditable(true);
		searchAddress.setEditable(true);

		setStatus(false);
	}

	public void showError(String title, String message) {
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
	}

	public boolean showConfirm(String title, String message) {
		return JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
	}

	public void updateTimeSlots(TimeSlots slots) {
		this.timeSlots = slots;
		slotDisplay.repaint();
	}

	public String getStepWidth() {
		if (searchStepWidth.getText().equals("")) {
			searchStepWidth.setText("" + Main.searchStepSize);
		}

		return searchStepWidth.getText();
	}

	public String getSkyperAddress() {
		return searchAddress.getText();
	}

	public void updateCorrection() {
		correctionActual.setText(String.format("%+4.2f", transmitter.getCorrection()));
		correctionSlider.setValue((int) (transmitter.getCorrection() * 100));
	}
}