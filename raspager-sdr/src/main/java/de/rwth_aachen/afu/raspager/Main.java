package de.rwth_aachen.afu.raspager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public final class Main {
	private static final Logger log = Logger.getLogger(Main.class.getName());
	private static boolean startService = false;
	private static boolean withTrayIcon = true;

	private static void initRxTx() {
		// Preventing rxtx to write to the console
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
		} finally {
			System.setOut(out);
		}
	}

	private static void printVersion() {
		System.out.println("FunkrufSlave - Version 2.0.0");
		System.out.println("by Ralf Wilke, Michael Delissen und Marvin Menzerath, powered by IHF RWTH Aachen");
		System.out.println("New Versions at https://github.com/dh3wr/SDRPager/releases");
		System.out.println();
	}

	private static boolean parseArguments(String[] args, Configuration config) {
		Options opts = new Options();
		opts.addOption("c", "config", true, "Configuration file to use.");
		opts.addOption("h", "help", false, "Show this help.");
		opts.addOption("v", "version", false, "Show version infomration.");
		opts.addOption("s", "service", false, "Run as a service without a GUI.");
		opts.addOption("notrayicon", false, "Disable tray icon.");

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
			startService = true;
		}

		if (line.hasOption("notrayicon")) {
			withTrayIcon = false;
		}

		try {
			String configFile = line.getOptionValue('c', "raspager.properties");
			config.load(configFile);
		} catch (FileNotFoundException ex) {
			log.log(Level.WARNING, "Failed to load configuration: {0}", ex.getMessage());
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to load configuration.", t);
		}

		return true;
	}

	public static void main(String[] args) {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			log.log(Level.SEVERE, String.format("Uncaught exception in thread %s.", t.getName()), e);
		});

		Configuration config = new Configuration();
		if (!parseArguments(args, config)) {
			return;
		}

		initRxTx();

		RasPagerService app = null;
		try {
			app = new RasPagerService(config, startService, withTrayIcon);
			app.run();
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Main application error.", t);
		} finally {
			if (app != null) {
				app.shutdown();
			}
		}
	}
}
