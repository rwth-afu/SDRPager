package funkrufSlave;

import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Timer;

import com.pi4j.io.gpio.Pin;

public class Main {
	public static final String VERSION = "1.3";

	public static ServerThread server;
	public static MessageQueue messageQueue;
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

	public static void printHelp() {
		System.out.println(
				"syntax: FunkrufSlave.jar [-nogui] [-logfile=logfile] [-configfile=configfile] [-v] [-loglevel=loglevel]");
		System.out.println("-nogui\t\t\tConsole only, no gui");
		System.out.println("-logfile=logfile\tWrite log information into logfile");
		System.out.println("-configfile=configfile\tLoad given configfile and use it");
		System.out.println(
				"-v, -verbose\t\tWrite log information to console (if in combination with -logfile, log information will be written to logfile AND console)");
		System.out.println("-loglevel=loglevel\tOverwrite loglevel with the given one");
	}

	// parse command line arguments
	public static void parseArguments(String[] args) {
		// valid parameters
		// -nogui, -logfile=, -configfile=, -v, -verbose

		String logfile = "";
		String configfile = "";
		int loglevel = -1;
		boolean verbose = false;

		for (int i = 0; i < args.length; i++) {

			String[] parts = args[i].split("=");

			// parameter -nogui
			if (parts[0].equals("-nogui")) {

				log("Parameter: nogui", Log.INFO);
				gui = false;

			}
			// parameter -logfile=...
			else if (parts[0].equals("-logfile") && parts.length == 2) {

				// logfile
				log("Parameter: logFile", Log.INFO);
				logfile = parts[1];

			}
			// parameter -loglevel=
			else if (parts[0].equals("-loglevel") && parts.length == 2) {
				log("Parameter: loglevel", Log.INFO);
				try {
					loglevel = Integer.parseInt(parts[1]);
				} catch (NumberFormatException e) {
					log("Loglevel hat kein gültiges Format!", Log.ERROR);
					loglevel = -1;
				}
			}
			// parameter -configfile=...
			else if (parts[0].equals("-configfile") && parts.length == 2) {

				// config file
				log("Parameter: configFile", Log.INFO);
				configfile = parts[1];

			}
			// parameter -v or -verbose
			else if (parts[0].equals("-v") || parts[0].equals("-verbose")) {
				log("Parameter: verbose", Log.INFO);
				verbose = true;
			}
			// version
			else if (parts[0].equals("-version")) {
				System.out.println("FunkrufSlave - Version " + VERSION);

				System.exit(0);
			}
			// help
			else if (parts[0].equals("--help") || parts[0].equals("-help") || parts[0].equals("-h")) {
				printHelp();

				System.exit(0);
			}
			// invalid parameter
			else {
				log("Parameter: Invalid # " + args[i], Log.ERROR);
			}

		}

		if (!logfile.equals("") || verbose) {
			log = new Log(logfile, verbose);
		}

		if (!configfile.equals("")) {
			try {
				config = new Config(log, configfile);
			} catch (InvalidConfigFileException e) {
				log(e.getMessage(), Log.ERROR);
			}
		} else if (!gui) {
			System.out.println(
					"Damit der FunkrufSlave ohne GUI gestartet werden kann, muss eine Konfigurationsdatei angegeben werden! -configfile=/foo/bar");
			System.exit(1);
		}

		if (loglevel != -1) {
			if (log != null) {
				log.setLogLevel(loglevel);
			}
		}
	}

	// set connection status
	public static void setStatus(boolean status) {
		if (mainWindow != null) {
			mainWindow.setStatus(status);
		}
	}

	// initialize
	public static void initialize() {
		// initialize timeSlots
		timeSlots = new TimeSlots();
	}

	public static boolean initSerialPortComm() {
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

	public static boolean initGpioPortComm() {
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
			scheduler = searching ? new SearchScheduler(log) : new Scheduler(log);
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

	// stop scheduler
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

	// start server (and join if needed)
	public static void startServer(boolean join) {
		if (messageQueue == null) {
			// initialize messageQueue
			messageQueue = new MessageQueue();
		}

		if (server == null) {
			// initialize server thread
			server = new ServerThread(config.getPort(), messageQueue, log);
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
				server.join(0);
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

	// stop server
	public static void stopServer(boolean error) {
		log("Server is going to shutdown.", Log.INFO);

		// if there was no error, halt server
		if (server != null) {
			server.halt();
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

	// call on server error
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

	// draw slots
	public static void drawSlots() {
		if (mainWindow != null) {
			mainWindow.drawSlots();
		}
	}

	// stop searching
	public static void stopSearching() {
		if (mainWindow != null) {
			mainWindow.runSearch(false);
		}
	}

	// get step width (for search)
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

	// get search skyper address
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

	// close serial port
	public static void closeSerialPort() {
		if (serialPortComm != null) {
			serialPortComm.close();
			serialPortComm = null;
		}
	}

	// close log
	public static void closeLog() {
		if (log != null) {
			log.close();
		}
	}

	// remove socket thread from list
	public static void removeSocketThread(SocketThread thread) {
		if (server != null) {
			server.removeSocketThread(thread);
		}
	}

	// main
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
