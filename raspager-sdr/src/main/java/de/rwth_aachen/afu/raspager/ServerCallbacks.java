package de.rwth_aachen.afu.raspager;

import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.logging.Logger;

final class ServerCallbacks {
	private static final Logger log = Logger.getLogger(ServerCallbacks.class.getName());

	private Consumer<Message> messageHandler;
	private IntConsumer timeCorrectionHandler;
	private Consumer<String> timeSlotsHandler;
	private IntSupplier timeHandler;

	public void setAddMessageHandler(Consumer<Message> messageHandler) {
		this.messageHandler = messageHandler;
	}

	public void setTimeCorrectionHandler(IntConsumer timeCorrectionHandler) {
		this.timeCorrectionHandler = timeCorrectionHandler;
	}

	public void setTimeSlotsHandler(Consumer<String> rimeSlotsHandler) {
		this.timeSlotsHandler = rimeSlotsHandler;
	}

	public void setTimeHandler(IntSupplier timeHandler) {
		this.timeHandler = timeHandler;
	}

	void addMessage(Message m) {
		if (messageHandler != null) {
			messageHandler.accept(m);
		} else {
			log.severe("No add message handler registered.");
		}
	}

	void setTimeCorrection(int correction) {
		if (timeCorrectionHandler != null) {
			timeCorrectionHandler.accept(correction);
		} else {
			log.severe("No set time correction handler registered.");
		}
	}

	void setTimeSlots(String slot) {
		if (timeSlotsHandler != null) {
			timeSlotsHandler.accept(slot);
		} else {
			log.severe("No set time slots handler registered.");
		}
	}

	int getTime() {
		if (timeHandler != null) {
			return timeHandler.getAsInt();
		} else {
			log.severe("No get time handler registered.");
			return 0;
		}
	}
}
