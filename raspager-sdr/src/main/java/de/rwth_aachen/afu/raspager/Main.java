package de.rwth_aachen.afu.raspager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Deque;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

public final class Main {
	private static final Logger log = Logger.getLogger(Main.class.getName());
	private static final String VERSION = "1.4.0";

	// TODO Get rid of all these static global vars
	public static ThreadWrapper<FunkrufServer> server;
	private static Deque<Message> messageQueue;
	public static Timer timer;
	public static Scheduler scheduler;
	public static TimeSlots timeSlots;
	public static SerialPortComm serialPortComm;
	public static GpioPortComm gpioPortComm;

	public static MainWindow mainWindow = null;
	public static boolean showGui = true;

	public static boolean running = false;

	public static final float DEFAULT_SEARCH_STEP_SIZE = 0.05f;
	public static float searchStepSize = DEFAULT_SEARCH_STEP_SIZE;

	private static final Configuration config = new Configuration();

	private static boolean parseArguments(String[] args) {
		Options opts = new Options();
		opts.addOption("c", "config", true, "Configuration file to use.");
		opts.addOption("h", "help", false, "Show this help.");
		opts.addOption("v", "version", false, "Show version infomration.");
		opts.addOption("s", "service", false, "Run as a service without a GUI.");

		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
		try {
			line = parser.parse(opts, args);
		} catch (ParseException ex) {
			ex.printStackTrace();
			return false;
		}

		// Show help
		if (line.hasOption('h')) {
			HelpFormatter fmt = new HelpFormatter();
			fmt.printHelp("raspager-sdr", opts);
			return false;
		}

		// Show version
		if (line.hasOption('v')) {
			// TODO impl
			return false;
		}

		// Start as a service
		if (line.hasOption('s')) {
			showGui = false;
		}

		try {
			String fileName = line.getOptionValue('c', "raspager.properties");
			config.load(fileName);
		} catch (Throwable ex) {
			ex.printStackTrace();
			return false;
		}

		return true;
	}

	private static boolean initSerialPortComm() {
		if (!config.getBoolean("useSerial", false))
			return true;

		if (serialPortComm == null) {
			try {
				// initialize serial port
				serialPortComm = new SerialPortComm(config.getString("serialPort"), config.getInt("serialPin"),
						config.getBoolean("invert"));
			} catch (NoSuchPortException e) {
				// no such port exception
				log.log(Level.SEVERE, "Serial port does not exist.", e);

				if (mainWindow != null) {
					mainWindow.showError("Server starten", "Serieller Port existiert nicht!");
				}

				return false;

			} catch (PortInUseException e) {
				// port in use exception
				log.log(Level.SEVERE, "Serial port already in use.", e);

				if (mainWindow != null) {
					mainWindow.showError("Server starten", "Serieller Port wird bereits benutzt!");
				}

				return false;

			} catch (UnsupportedCommOperationException e) {
				// unsupported comm operation exception
				log.log(Level.SEVERE, "Unsupported comm operation.", e);

				if (mainWindow != null) {
					mainWindow.showError("Server starten",
							"Beim Initialisieren der seriellen Schnittstelle ist ein Fehler aufgetreten!");
				}

				return false;
			}
		}

		return true;
	}

	private static boolean initGpioPortComm() {
		if (!config.getBoolean("useGPIO", false))
			return true;

		if (gpioPortComm == null) {
			gpioPortComm = new GpioPortComm(config.getString("gpioPin"), config.getBoolean("invert", false));
			return true;
		}

		log.severe("Failed to initialize GPIO port.");

		if (mainWindow != null) {
			mainWindow.showError("Server starten", "GPIO-Schnittstelle konnte nicht initialisiert werden!");
		}

		return false;
	}

	// start scheduler (or search scheduler)
	public static void startScheduler(boolean searching) {
		if (timer == null) {
			timer = new Timer();
			scheduler = searching ? new SearchScheduler(config, messageQueue) : new Scheduler(config, messageQueue);
		}

		if (!initSerialPortComm()) {
			stopScheduler();

			if (mainWindow != null) {
				mainWindow.resetButtons();
			}
			return;
		}

		if (!initGpioPortComm()) {
			stopScheduler();

			if (mainWindow != null) {
				mainWindow.resetButtons();
			}
			return;
		}

		timer.schedule(scheduler, 100, 100);
	}

	public static void stopScheduler() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
		if (scheduler != null) {
			scheduler.cancel();
			scheduler = null;
		}

		// close serial port
		if (serialPortComm != null) {
			serialPortComm.close();
			serialPortComm = null;
		}

		// close gpio communication
		if (gpioPortComm != null) {
			gpioPortComm.close();
			gpioPortComm = null;
		}
	}

	public static void startServer(boolean join) {
		if (messageQueue == null) {
			// initialize messageQueue
			messageQueue = new ConcurrentLinkedDeque<>();
		}

		if (server == null) {
			// initialize server thread
			server = new ThreadWrapper<FunkrufServer>(new FunkrufServer(config.getInt("port"), messageQueue));
		}

		// start scheduler (not searching)
		startScheduler(false);

		// start server
		server.start();

		// set running to true
		running = true;
		log.info("Server is running.");

		// if join is true
		if (join) {
			try {
				// join server thread
				server.join();
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, "Server thread interrupted.", e);
			}

			// stop server
			stopServer(true);
		}

		// set connection status to false
		if (mainWindow != null) {
			mainWindow.setStatus(false);
		}

	}

	public static void stopServer(boolean error) {
		log.info("Server is shutting down.");

		// if there was no error, halt server
		if (server != null) {
			server.getJob().shutdown();
		}

		server = null;

		// set running to false
		running = false;

		// stop scheduler
		stopScheduler();

		if (messageQueue != null) {
			messageQueue = null;
		}

		log.info("Server stopped.");

		if (showGui && mainWindow != null) {
			mainWindow.resetButtons();
		}
	}

	public static void serverError(String message) {
		// set running to false
		running = false;

		// stop scheduler
		stopScheduler();

		server = null;

		if (mainWindow != null) {
			// show error and reset start button
			mainWindow.showError("Server Error", message);
			mainWindow.resetButtons();

		}
	}

	public static void drawSlots() {
		if (mainWindow != null) {
			mainWindow.drawSlots();
		}
	}

	public static void stopSearching() {
		if (mainWindow != null) {
			mainWindow.runSearch(false);
		}
	}

	public static float getStepSize() {
		float stepWidth = searchStepSize;

		if (mainWindow != null) {
			String s = mainWindow.getStepWidth();

			if (!s.equals("")) {
				try {
					stepWidth = Float.parseFloat(s);
				} catch (NumberFormatException e) {
					log.log(Level.SEVERE, "Invalid step size.", e);
				}
			}
		}

		return stepWidth;
	}

	public static String getSkyperAddress() {
		String s = "";

		if (mainWindow != null) {
			s = mainWindow.getSkyperAddress();
			if (!s.equals("")) {
				int i;
				try {
					i = Integer.parseInt(s);
					s = Integer.toString(i, 16);
					System.out.println("Adresse (BC1F): " + s);
				} catch (NumberFormatException e) {
					log.log(Level.SEVERE, "Invalid Skyper address.", e);
				}
			}
		}

		return s;
	}

	public static void closeSerialPort() {
		if (serialPortComm != null) {
			serialPortComm.close();
			serialPortComm = null;
		}
	}

	public static void main(String[] args) {
		// to prevent rxtx to write to console
		PrintStream out = System.out;
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) throws IOException {
			}
		}));
		try {
			Class.forName("gnu.io.RXTXCommDriver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		System.setOut(out);

		// write name, version and authors
		System.out.println("FunkrufSlave - Version " + VERSION
				+ "\nby Ralf Wilke, Michael Delissen und Marvin Menzerath, powered by IHF RWTH Aachen\nNew Versions at https://github.com/dh3wr/SDRPager/releases\n");

		// parse arguments
		if (!parseArguments(args)) {
			return;
		}

		// initialize
		timeSlots = new TimeSlots();

		// set running to false
		running = false;

		// if gui
		if (showGui) {
			// create mainWindow
			mainWindow = new MainWindow(new ConfigWrapper(config));
		} else {
			// if no gui, start server and join
			startServer(true);
		}

	}
}
