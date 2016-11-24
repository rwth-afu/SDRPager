package de.rwth_aachen.afu.raspager;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.rwth_aachen.afu.raspager.sdr.SDRTransmitter;

class SearchScheduler extends Scheduler {
	private static final Logger log = Logger.getLogger(SearchScheduler.class.getName());
	private final RasPagerService service;
	private List<Integer> data;

	public SearchScheduler(Configuration config, Deque<Message> messageQueue, RasPagerService service) {
		super(messageQueue, service.getTransmitter());

		this.service = service;
	}

	@Override
	public void run() {
		// still get local time in 0.1s, add (or sub) correction (here: delay)
		// from master,
		// and take lowest 16 bits (this.max)
		time = ((int) (System.currentTimeMillis() / 100) + delay) % MAX;

		if (slots.hasChanged(time) && updateTimeSlotsHandler != null) {
			log.fine("Updating time slots.");
			updateTimeSlotsHandler.accept(slots);
		}

		switch (state) {
		case AWAITING_SLOT:
			encodeData();
			break;
		case DATA_ENCODED:
			sendData();
			break;
		case SLOT_STILL_ALLOWED:
			stillAllowed();
			break;
		default:
			log.log(Level.WARNING, "Unknown state {0}.", state);
		}
	}

	private void encodeData() {
		try {
			if (slots.isNextAllowed(time) && !messageQueue.isEmpty()
					&& TimeSlots.getTimeToNextSlot(time) <= MAX_ENCODE_TIME_100MS) {
				if (updateData()) {
					rawData = transmitter.encode(codeWords);
					this.state = State.DATA_ENCODED;
				}
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to encode data.", t);
		}
	}

	private void sendData() {
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
	}

	private void stillAllowed() {
		try {
			if (slots.isAllowed(time) && !messageQueue.isEmpty()) {
				if (updateData()) {
					rawData = transmitter.encode(codeWords);
					state = State.DATA_ENCODED;
				}
			} else {
				state = State.AWAITING_SLOT;
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to encode data.", t);
			state = State.AWAITING_SLOT;
		}
	}

	// get data depending on slot count
	private boolean updateData() {
		if (service.getWindow() == null) {
			throw new IllegalStateException("Main window is null.");
		}

		SDRTransmitter sdr = (SDRTransmitter) transmitter;

		float correction = sdr.getCorrection();
		float stepSize = service.getSearchStepSize();

		if (correction < 1.0f) {
			log.info(String.format("Correction {0}", correction));

			if (correction + stepSize > 1.0f) {
				sdr.setCorrection(1.0f);
			} else {
				sdr.setCorrection(correction + stepSize);
			}

			// TODO Refactor
			service.getWindow().updateCorrection();
		} else {
			service.stopSearching();
		}

		data = new ArrayList<>();
		for (int i = 0; i < 18; ++i) {
			data.add(Pocsag.PRAEAMBLE);
		}

		String addr = service.getWindow().getSkyperAddress();
		addMessage(new Message(("#00 5:1:9C8:0:000000   010112").split(":")));

		if (!addr.isEmpty()) {
			String[] parts = new String[] { "#00 6", "1", addr, "3",
					String.format("correction=%+4.2f", sdr.getCorrection()) };
			addMessage(new Message(parts));
		}

		return true;
	}

	private void addMessage(Message message) {
		// add sync-word (start of batch)
		data.add(Pocsag.SYNC);

		// get codewords of message
		List<Integer> cwBuf = message.getCodeWords();
		int framePos = cwBuf.get(0);

		// add idle-words until frame position is reached
		for (int c = 0; c < framePos; c++) {
			data.add(Pocsag.IDLE);
			data.add(Pocsag.IDLE);
		}

		// add codewords of message
		for (int c = 1; c < cwBuf.size(); c++) {
			if ((data.size() - 18) % 17 == 0)
				data.add(Pocsag.SYNC);

			data.add(cwBuf.get(c));
		}

		// fill last batch with idle-words
		while ((data.size() - 18) % 17 != 0) {
			data.add(Pocsag.IDLE);
		}
	}
}
