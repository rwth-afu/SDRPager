package de.rwth_aachen.afu.raspager;

import de.rwth_aachen.afu.raspager.sdr.SDRTransmitter;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class SearchScheduler extends Scheduler {
	private static final Logger log = Logger.getLogger(SearchScheduler.class.getName());
	private final RasPagerService service;

	public SearchScheduler(Deque<Message> messageQueue, RasPagerService service) {
		super(messageQueue, service.getTransmitter());
		this.service = service;
	}

	@Override
	public void run() {
		try {
			if (updateData()) {
				rawData = transmitter.encode(codeWords);
				sendData();
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to encode data.", t);
		}
	}

	private void sendData() {
		log.fine("Activating transmitter.");
		try {
			transmitter.send(rawData);
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to send data.", t);
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
			service.getWindow().updateCorrection(correction);
		} else {
			service.stopSearching();
		}

		codeWords = new ArrayList<>();
		for (int i = 0; i < 18; ++i) {
			codeWords.add(Pocsag.PRAEAMBLE);
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
