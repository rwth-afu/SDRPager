package de.rwth_aachen.afu.raspager;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Deque;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import com.pi4j.io.gpio.Pin;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

public class Main {
	public static final String VERSION = "1.4.0";

	// TODO Get rid of all these static global vars
	public static ThreadWrapper<FunkrufServer> server;
	private static Deque<Message> messageQueue;
	public static Timer timer;
	public static Scheduler scheduler;
	public static TimeSlots timeSlots;
	public static SerialPortComm serialPortComm;
	public static GpioPortComm gpioPortComm;

	public static MainWindow mainWindow = null;
	public static boolean gui = true;

	public static boolean running = false;

	public static Config config = null;
	public static Log log = null;

	public static final float DEFAULT_SEARCH_STEP_WIDTH = 0.05f;
	public static float searchStepWidth = DEFAULT_SEARCH_STEP_WIDTH;

	private static void log(String message, int type) {
		log(message, type, Log.NORMAL);
	}

	private static void log(String message, int type, int logLevel) {
		if (log != null) {
			log.println(message, type, logLevel);
		}
	}

	private static void parseArguments(String[] args) {
		Options opts = new Options();
		opts.addOption("c", "config", true, "Configuration file to use.");
		opts.addOption("h", "help", false, "Show this help.");
		opts.addOption("v", "version", false, "Show version infomration.");
		opts.addOption("s", "service", false, "Run as a service without a GUI.");
		// TODO Remove legacy stuff once config is using settings.cl

		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
		try {
			line = parser.parse(opts, args);
		} catch (ParseException ex) {
			ex.printStackTrace();
			return;
		}

		// Show help
		if (line.hasOption('h')) {
			HelpFormatter fmt = new HelpFormatter();
			fmt.printHelp("raspager-sdr", opts);
			return;
		}

		// Show version
		if (line.hasOption('v')) {
			// TODO impl
			return;
		}

		// TODO
		// Start as a service
		// boolean gui = true;
		// if (line.hasOption('s')) {
		// gui = false;
		// }

		// Config file
		try {
			String filename = line.getOptionValue('c', "raspager.properties");
			config = new Config(log, filename);
		} catch (InvalidConfigFileException ex) {
			ex.printStackTrace();
			return;
		}
	}

	private static void initialize() {
		// initialize timeSlots
		timeSlots = new TimeSlots();
	}

	private static boolean initSerialPortComm() {
		if (!Main.config.useSerial())
			return true;

		if (serialPortComm == null) {
			try {
				// initialize serial port
				serialPortComm = new SerialPortComm(Main.config.getSerialPort(), Main.config.getSerialPin(),
						Main.config.getInvert(), log);
			} catch (NoSuchPortException e) {
				// no such port exception
				log("startServer # Serieller Port existiert nicht!", Log.ERROR);

				if (mainWindow != null) {
					mainWindow.showError("Server starten", "Serieller Port existiert nicht!");
				}

				return false;

			} catch (PortInUseException e) {
				// port in use exception
				log("startServer # Serieller Port wird bereits genutzt!", Log.ERROR);

				if (mainWindow != null) {
					mainWindow.showError("Server starten", "Serieller Port wird bereits benutzt!");
				}

				return false;

			} catch (UnsupportedCommOperationException e) {
				// unsupported comm operation exception
				log("startServer # UnsupportedCommOperation!", Log.ERROR);

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
		if (!Main.config.useGpio())
			return true;

		if (Main.config.getGpioPin() == null || !(Main.config.getGpioPin() instanceof Pin)) {
			log("startServer # GPIO-Pin existiert nicht!", Log.ERROR);

			if (mainWindow != null) {
				mainWindow.showError("Server starten", "GPIO-Pin existiert nicht!");
			}
			return false;
		}

		if (gpioPortComm == null) {
			gpioPortComm = new GpioPortComm(Main.config.getGpioPin(), Main.config.getInvert(), log);
			return true;
		}

		log("startServer # GPIO-Schnittstelle konnte nicht initialisiert werden!", Log.ERROR);

		if (mainWindow != null) {
			mainWindow.showError("Server starten", "GPIO-Schnittstelle konnte nicht initialisiert werden!");
		}
		return false;
	}

	// start scheduler (or search scheduler)
	public static void startScheduler(boolean searching) {
		if (timer == null) {
			timer = new Timer();
			scheduler = searching ? new SearchScheduler(messageQueue) : new Scheduler(messageQueue);
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
			server = new ThreadWrapper<FunkrufServer>(new FunkrufServer(config.getPort(), messageQueue));
		}

		// start scheduler (not searching)
		startScheduler(false);

		// start server
		server.start();

		// set running to true
		running = true;
		log("Server is running.", Log.INFO);

		// if join is true
		if (join) {
			try {
				// join server thread
				server.join();
			} catch (InterruptedException e) {
				log("ServerThread interrupted", Log.INFO);
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
		log("Server is going to shutdown.", Log.INFO);

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

		log("Server halted.", Log.INFO);

		// if there is no gui, the log has to be closed
		if (!gui) {
			log.close();
		} else {
			// if there is a gui, the start button has to be reseted
			if (mainWindow != null) {
				mainWindow.resetButtons();
			}
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

	public static float getStepWidth() {
		float stepWidth = searchStepWidth;

		if (mainWindow != null) {
			String s = mainWindow.getStepWidth();

			if (!s.equals("")) {
				try {
					stepWidth = Float.parseFloat(s);
				} catch (NumberFormatException e) {
					log("stepWidth # Keine gueltige Schrittweite!", Log.ERROR);
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
					log("skyperAddress # Keine gueltige Skyper-Adresse!", Log.ERROR);
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

	public static void closeLog() {
		if (log != null) {
			log.close();
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
		parseArguments(args);

		// load config, if not loaded
		if (config == null) {
			config = new Config(log);
			config.loadDefault();
		}

		// initialize
		initialize();

		// set running to false
		running = false;

		// if gui
		if (gui) {
			// create mainWindow
			mainWindow = new MainWindow(log);
		} else {
			// if no gui, start server and join
			startServer(true);
		}

	}
}
