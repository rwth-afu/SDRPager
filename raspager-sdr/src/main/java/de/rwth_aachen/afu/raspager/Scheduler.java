package de.rwth_aachen.afu.raspager;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.TimerTask;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

class Scheduler extends TimerTask {
	protected enum State {
		AWAITING_SLOT, DATA_ENCODED, SLOT_STILL_ALLOWED
	}

	private static final Logger log = Logger.getLogger(Scheduler.class.getName());
	// max time value (2^16)
	protected static final int MAX = 65536;

	protected final TimeSlots slots = new TimeSlots();
	protected final Deque<Message> messageQueue;
	protected final Transmitter transmitter;

	protected int time = 0;
	protected int delay = 0;
	protected Consumer<TimeSlots> updateTimeSlotsHandler;
	protected State state = State.AWAITING_SLOT;
	protected List<Integer> codeWords;
	protected byte[] rawData;

	public Scheduler(Deque<Message> messageQueue, Transmitter transmitter) {
		this.messageQueue = messageQueue;
		this.transmitter = transmitter;
	}

	public void setUpdateTimeSlotsHandler(Consumer<TimeSlots> handler) {
		updateTimeSlotsHandler = handler;
	}

	@Override
	public void run() {
		time = ((int) (System.currentTimeMillis() / 100) + delay) % MAX;

		if (slots.hasChanged(time) && updateTimeSlotsHandler != null) {
			log.fine("Updating time slots.");
			updateTimeSlotsHandler.accept(slots);
		}

		switch (state) {
		case AWAITING_SLOT:
			if (slots.isNextAllowed(time) && !messageQueue.isEmpty()) {

			}
			break;
		case DATA_ENCODED:
			if (slots.get(TimeSlots.getIndex(time))) {
				log.fine("Activating transmitter.");
				try {
					transmitter.send(rawData);
				} catch (Throwable t) {
					log.log(Level.SEVERE, "Failed to send data.", t);
				} finally {
					state = State.SLOT_STILL_ALLOWED;
				}
			}
			break;
		case SLOT_STILL_ALLOWED:
			if (slots.isAllowed(time)) {

			} else {
				state = State.AWAITING_SLOT;
			}
			break;
		default:
			log.log(Level.WARNING, "Unknown state {0}.", state);
		}
	}

	/**
	 * Gets data depending on the given slot count.
	 * 
	 * @param slotCount
	 *            Slot count.
	 * @return Code words to send.
	 */
	private void updateData(int slotCount) {
		// send batches
		// max batches per slot: (slot time - praeambel time) / bps / ((frames +
		// (1 = sync)) * bits per frame)
		// (3,75 - 0,48) * 1200 / ((16 + 1) * 32)
		int maxBatch = (int) ((6.40 * slotCount - 0.48 - delay / 1000) * 1200 / 544);

		codeWords = new ArrayList<>();

		// add praeembel
		for (int i = 0; i < 18; i++) {
			codeWords.add(Pocsag.PRAEAMBLE);
		}

		while (!messageQueue.isEmpty()) {
			Message message = messageQueue.pop();

			// get codewords and frame position
			List<Integer> cwBuf = message.getCodeWords();
			int framePos = cwBuf.get(0);
			int cwCount = cwBuf.size() - 1;

			// (data.size() - 18) / 17 = aktBatches
			// aktBatches + (cwCount + 2 * framePos) / 16 + 1 = Batches NACH
			// hinzufügen
			// also Batches NACH hinzufügen > maxBatches, dann keine neue
			// Nachricht holen
			// if count of batches + this message is greater than max batches
			if (((codeWords.size() - 18) / 17 + (cwCount + 2 * framePos) / 16 + 1) > maxBatch) {
				messageQueue.addFirst(message);
				break;
			}

			// each batch starts with a sync code word
			codeWords.add(Pocsag.SYNC);

			// add idle code words until frame position is reached
			for (int c = 0; c < framePos; c++) {
				codeWords.add(Pocsag.IDLE);
				codeWords.add(Pocsag.IDLE);
			}

			// add actual payload
			for (int c = 1; c < cwBuf.size(); c++) {
				if ((codeWords.size() - 18) % 17 == 0) {
					codeWords.add(Pocsag.SYNC);
				}

				codeWords.add(cwBuf.get(c));
			}

			// fill batch with idle-words
			while ((codeWords.size() - 18) % 17 != 0) {
				codeWords.add(Pocsag.IDLE);
			}
		}

		log.fine(String.format("Batches used: {0}/{1}", ((codeWords.size() - 18) / 17), maxBatch));
	}

	public TimeSlots getSlots() {
		return slots;
	}

	public void setTimeSlots(String s) {
		slots.setSlots(s);
	}

	/**
	 * Gets current time.
	 * 
	 * @return Current time.
	 */
	public int getTime() {
		return time;
	}

	/**
	 * Sets time correction.
	 * 
	 * @param delay
	 *            Time correction.
	 */
	public void correctTime(int delay) {
		this.delay += delay;
	}
}
