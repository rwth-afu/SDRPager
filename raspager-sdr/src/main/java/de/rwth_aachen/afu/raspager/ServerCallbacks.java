package de.rwth_aachen.afu.raspager;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.logging.Logger;

/**
 * This class provides functional-interface callbacks for the server handler.
 */
final class ServerCallbacks {
	private static final Logger log = Logger.getLogger(ServerCallbacks.class.getName());

	private Consumer<Message> messageHandler;
	private IntConsumer timeCorrectionHandler;
	private Consumer<String> timeSlotsHandler;
	private IntSupplier timeHandler;

	/**
	 * Sets the handler for new message packets.
	 * 
	 * @param messageHandler
	 *            Handler to use.
	 */
	public void setAddMessageHandler(Consumer<Message> messageHandler) {
		this.messageHandler = messageHandler;
	}

	/**
	 * Sets the handler for time correction packets.
	 * 
	 * @param timeCorrectionHandler
	 *            Handler to use.
	 */
	public void setTimeCorrectionHandler(IntConsumer timeCorrectionHandler) {
		this.timeCorrectionHandler = timeCorrectionHandler;
	}

	/**
	 * Sets the handler for time slot activation packets.
	 * 
	 * @param timeSlotsHandler
	 *            Handler to use.
	 */
	public void setTimeSlotsHandler(Consumer<String> timeSlotsHandler) {
		this.timeSlotsHandler = timeSlotsHandler;
	}

	/**
	 * Sets the handler for time packets.
	 * 
	 * @param timeHandler
	 *            Handler to use.
	 */
	public void setTimeHandler(IntSupplier timeHandler) {
		this.timeHandler = timeHandler;
	}

	/**
	 * Called by the server handler whenever a new message packet must be
	 * handled.
	 * 
	 * @param m
	 *            Message to handle.
	 */
	void addMessage(Message m) {
		if (messageHandler != null) {
			messageHandler.accept(m);
		} else {
			log.severe("No add message handler registered.");
		}
	}

	/**
	 * Called by the server handler whenever a time correction packet must be
	 * handled.
	 * 
	 * @param correction
	 *            Time correction factor.
	 */
	void setTimeCorrection(int correction) {
		if (timeCorrectionHandler != null) {
			timeCorrectionHandler.accept(correction);
		} else {
			log.severe("No set time correction handler registered.");
		}
	}

	/**
	 * Called by the server handler whenever a set timeslots packet must be
	 * handled.
	 * 
	 * @param slot
	 *            Time slot data.
	 */
	void setTimeSlots(String slot) {
		if (timeSlotsHandler != null) {
			timeSlotsHandler.accept(slot);
		} else {
			log.severe("No set time slots handler registered.");
		}
	}

	/**
	 * Called by the server handler whenever the current time value is
	 * requested.
	 * 
	 * @return Current time value.
	 */
	int getTime() {
		if (timeHandler != null) {
			return timeHandler.getAsInt();
		} else {
			log.severe("No get time handler registered.");
			return 0;
		}
	}
}
