package de.rwth_aachen.afu.raspager;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Deque;
import java.util.Timer;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.rwth_aachen.afu.raspager.sdr.SDRTransmitter;

final class RasPagerService {
	private static final Logger log = Logger.getLogger(RasPagerService.class.getName());

	// TODO Get rid of all these static global vars
	private static final float DEFAULT_SEARCH_STEP_SIZE = 0.05f;
	private float searchStepSize = DEFAULT_SEARCH_STEP_SIZE;
	private ThreadWrapper<Server> server;
	private boolean running = false;

	private Timer timer = new Timer();
	private final Deque<Message> messages = new ConcurrentLinkedDeque<>();
	private final SDRTransmitter transmitter = new SDRTransmitter();
	private final Configuration config;
	private final RasPagerWindow window;
	private Scheduler scheduler;

	public RasPagerService(Configuration config, boolean startService) throws FileNotFoundException, IOException {
		this.config = config;

		if (!startService) {
			window = new RasPagerWindow(this);
		} else {
			window = null;
		}
	}

	public Configuration getConfig() {
		return config;
	}

	public boolean isRunning() {
		return running;
	}

	public boolean isServerRunning() {
		return server != null;
	}

	public SDRTransmitter getTransmitter() {
		return transmitter;
	}

	public float getSearchStepSize() {
		return searchStepSize;
	}

	public void startScheduler(boolean searching) {
		try {
			transmitter.init(config);
		} catch (Exception ex) {
			if (window != null) {
				window.showError("Failed to init transmitter", ex.getMessage());
			}

			return;
		}

		int period = 100;
		if (searching) {
			scheduler = new SearchScheduler(messages, this);
			period = 5000;
		} else {
			scheduler = new Scheduler(messages, transmitter);
		}

		if (window != null) {
			scheduler.setUpdateTimeSlotsHandler(window::updateTimeSlots);
		}

		if (server != null) {
			server.getJob().setGetTimeHandler(scheduler::getTime);
			server.getJob().setTimeCorrectionHandler(scheduler::correctTime);
			server.getJob().setTimeSlotsHandler(scheduler::setTimeSlots);
		}

		timer = new Timer();
		timer.schedule(scheduler, 100, period);
	}

	public void stopScheduler() {
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

	public void startServer(boolean join) {
		if (server == null) {
			Server srv = new Server(config);
			// Register event handlers
			srv.setAddMessageHandler(messages::push);
			// Create new server thread
			server = new ThreadWrapper<Server>(srv);
		}

		// start scheduler (not searching)
		startScheduler(false);

		server.start();

		running = true;
		log.info("Server is running.");

		if (join) {
			try {
				server.join();
			} catch (InterruptedException e) {
				log.log(Level.SEVERE, "Server thread interrupted.", e);
			}

			stopServer(true);
		}

		if (window != null) {
			window.setStatus(false);
		}
	}

	public void stopServer(boolean error) {
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

		if (window != null) {
			window.resetButtons();
		}
	}

	public void serverError(String message) {
		// set running to false
		running = false;

		// stop scheduler
		stopScheduler();

		server = null;

		if (window != null) {
			window.showError("Server Error", message);
			window.resetButtons();
		}
	}

	public void stopSearching() {
		if (window != null) {
			window.runSearch(false);
		}
	}

	public float getStepSize() {
		float stepWidth = searchStepSize;

		if (window != null) {
			String s = window.getStepWidth();

			if (!s.isEmpty()) {
				try {
					stepWidth = Float.parseFloat(s);
				} catch (NumberFormatException e) {
					log.log(Level.SEVERE, "Invalid step size.", e);
				}
			}
		}

		return stepWidth;
	}

	public void run() {
		if (window == null) {
			startServer(true);
		}
	}

	public void shutdown() {
		try {
			if (transmitter != null) {
				transmitter.close();
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to close transmitter.", t);
		}

		timer.cancel();
	}

	public RasPagerWindow getWindow() {
		// TODO replace
		return window;
	}
}
