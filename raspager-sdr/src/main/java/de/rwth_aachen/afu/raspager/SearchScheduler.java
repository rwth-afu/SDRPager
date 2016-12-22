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

	public SearchScheduler(RasPagerService service, Deque<Message> messageQueue) {
		super(service.getConfig(), messageQueue, service.getTransmitter());
		this.service = service;
	}

	@Override
	public void run() {
		try {
			if (updateData()) {
				log.fine("Encoding data.");
				rawData = transmitter.encode(codeWords);
				log.fine("Sending data.");
				transmitter.send(rawData);
			}
		} catch (IllegalStateException ex) {
			// This happens when the task is cancelled while executing.
			if (!canceled.get()) {
				log.log(Level.SEVERE, "SearchScheduler interrupted.", ex);
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "SearchScheduler failed.", t);
		}
	}

	private boolean updateData() {
		if (service.getWindow() == null) {
			throw new IllegalStateException("Main window is null.");
		}

		SDRTransmitter sdr = (SDRTransmitter) transmitter;

		float correction = sdr.getCorrection();
		float stepSize = service.getStepSize();

		if (correction < 1.0f) {
			correction += stepSize;
			if (correction > 1.0f) {
				correction = 1.0f;
			}

			sdr.setCorrection(correction);
			// TODO Refactor
			service.getWindow().updateCorrection(correction);
		} else {
			service.stopSearching();
		}

		codeWords = new ArrayList<>();
		for (int i = 0; i < 18; ++i) {
			codeWords.add(Pocsag.PRAEAMBLE);
		}

		addMessage(new Message(("#00 5:1:9C8:0:000000   010112").split(":")));

		// TODO Remove? Empty field is checked in button handler.
		String addr = service.getWindow().getSkyperAddress();
		if (addr != null && !addr.isEmpty()) {
			String[] parts = new String[] { "#00 6", "1", addr, "3",
					String.format("correction=%+4.2f", sdr.getCorrection()) };
			addMessage(new Message(parts));

			return true;
		} else {
			codeWords = null;

			return false;
		}
	}

	private void addMessage(Message message) {
		// add sync-word (start of batch)
		codeWords.add(Pocsag.SYNC);

		// get codewords of message
		List<Integer> cwBuf = message.getCodeWords();
		int framePos = cwBuf.get(0);

		// add idle-words until frame position is reached
		for (int c = 0; c < framePos; c++) {
			codeWords.add(Pocsag.IDLE);
			codeWords.add(Pocsag.IDLE);
		}

		// add codewords of message
		for (int c = 1; c < cwBuf.size(); c++) {
			if ((codeWords.size() - 18) % 17 == 0)
				codeWords.add(Pocsag.SYNC);
			codeWords.add(cwBuf.get(c));
		}

		// fill last batch with idle-words
		while ((codeWords.size() - 18) % 17 != 0) {
			codeWords.add(Pocsag.IDLE);
		}
	}
}
