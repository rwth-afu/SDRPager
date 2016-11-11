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

import de.rwth_aachen.afu.raspager.sdr.SDRTransmitter;

public final class Main {
	private static final Logger log = Logger.getLogger(Main.class.getName());
	private static final String VERSION = "1.4.0";

	// TODO Get rid of all these static global vars
	private static final float DEFAULT_SEARCH_STEP_SIZE = 0.05f;
	public static float searchStepSize = DEFAULT_SEARCH_STEP_SIZE;
	public static ThreadWrapper<FunkrufServer> server;
	private static MainWindow mainWindow;
	private static boolean showGui = true;
	public static boolean running = false;

	private static final Configuration config = new Configuration();
	private static final Timer timer = new Timer();
	private static final Deque<Message> messages = new ConcurrentLinkedDeque<>();
	private static final SDRTransmitter transmitter = new SDRTransmitter();
	private static Scheduler scheduler;

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
			log.log(Level.SEVERE, "Failed to parse command line.", ex);
			return false;
		}

		if (line.hasOption('h')) {
			HelpFormatter fmt = new HelpFormatter();
			fmt.printHelp("raspager-sdr", opts);
			return false;
		}

		if (line.hasOption('v')) {
			printVersion();
			return false;
		}

		if (line.hasOption('s')) {
			// Start as a service
			showGui = false;
		}

		try {
			String fileName = line.getOptionValue('c', "raspager.properties");
			config.load(fileName);
		} catch (Throwable ex) {
			log.log(Level.SEVERE, "Failed to load configuration file.", ex);
			return false;
		}

		return true;
	}

	// start scheduler (or search scheduler)
	public static void startScheduler(boolean searching) {
		try {
			transmitter.init(config);
		} catch (Exception ex) {
			if (mainWindow != null) {
				mainWindow.showError("Failed to init transmitter", ex.getMessage());
			}

			return;
		}

		if (searching) {
			scheduler = new SearchScheduler(config, messages, transmitter);
		} else {
			scheduler = new Scheduler(messages, transmitter);
		}

		if (mainWindow != null) {
			scheduler.setUpdateTimeSlotsHandler(mainWindow::updateTimeSlots);
		}

		timer.schedule(scheduler, 100, 100);
	}

	public static void stopScheduler() {
		timer.cancel();

		if (scheduler != null) {
			scheduler.cancel();
			scheduler = null;
		}

		try {
			transmitter.close();
		} catch (Exception e) {
			log.log(Level.SEVERE, "Failed to close transmitter.", e);
		}
	}

	public static void startServer(boolean join) {
		if (server == null) {
			FunkrufServer srv = new FunkrufServer(config);
			// Register event handlers
			srv.setAddMessageHandler(messages::push);
			srv.setGetTimeHandler(scheduler::getTime);
			srv.setTimeCorrectionHandler(scheduler::correctTime);
			srv.setTimeSlotsHandler(scheduler::setTimeSlots);
			// Create new server thread
			server = new ThreadWrapper<FunkrufServer>(srv);
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

		messages.clear();

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

	private static void initRxTx() {
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
			log.log(Level.SEVERE, "Failed to load RXTX.", e);
		}

		System.setOut(out);
	}

	private static void printVersion() {
		System.out.println("FunkrufSlave - Version " + VERSION);
		System.out.println("by Ralf Wilke, Michael Delissen und Marvin Menzerath, powered by IHF RWTH Aachen");
		System.out.println("New Versions at https://github.com/dh3wr/SDRPager/releases");
		System.out.println();
	}

	public static void main(String[] args) {
		initRxTx();

		if (!parseArguments(args)) {
			return;
		}

		running = false;

		if (showGui) {
			// TODO fix
			mainWindow = new MainWindow(null, transmitter);
		} else {
			startServer(true);
		}

		try {
			transmitter.close();
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to close transmitter.", t);
		}
	}
}
