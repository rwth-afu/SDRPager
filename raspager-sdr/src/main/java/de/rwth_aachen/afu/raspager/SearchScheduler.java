package de.rwth_aachen.afu.raspager;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.rwth_aachen.afu.raspager.sdr.SDRTransmitter;

class SearchScheduler extends Scheduler {
	private static final Logger log = Logger.getLogger(SearchScheduler.class.getName());
	private final float cfgStepSize;
	// for first message (the correction should not be increased at first time)
	private boolean firstTime = true;
	private List<Integer> data;

	public SearchScheduler(Configuration config, Deque<Message> messageQueue, SDRTransmitter transmitter) {
		super(messageQueue, transmitter);

		cfgStepSize = config.getFloat("search.stepSize");
	}

	@Override
	public void run() {
		/*
		 * // if active if (active) { // increase time this.time = ++this.time %
		 * this.MAX;
		 * 
		 * // if serial delay is lower than or equals 0, decrease send time if
		 * (this.serialDelay <= 0) { if (this.sendTime > 0) this.sendTime -=
		 * 0.1f; }
		 * 
		 * // decrease serial delay if (this.serialDelay > 0) this.serialDelay
		 * -= 100; }
		 * 
		 * // check slot char slot = TimeSlots.getCurrentSlot(time); boolean
		 * isLastSlot = state.slots.isLastSlot(slot);
		 * 
		 * // if it is not the last slot if (!isLastSlot) { // draw slots
		 * Main.drawSlots(); }
		 * 
		 * // if slot is not the last slot or it is the first time if
		 * ((!isLastSlot || firstTime)) { // get data (slotCount = 0, because it
		 * is not needed here) getData(0); }
		 * 
		 * // if serial delay is lower than or equals 0 and there is data if
		 * (this.serialDelay <= 0 && data != null) { try {
		 * transmitter.send(data); } catch (Exception ex) {
		 * log.log(Level.SEVERE, "Failed to send data.", ex); } finally { data =
		 * null; } }
		 */
	}

	// get data depending on slot count
	private List<Integer> getData(int slotCount) {
		/*
		 * serialDelay = cfgDelay; SDRTransmitter sdr = (SDRTransmitter)
		 * transmitter;
		 * 
		 * // if it is not the first time if (!firstTime) { // is correction
		 * lower than 1.0? float correction = sdr.getCorrection(); if
		 * (correction < 1.0f) { log.log(Level.FINE, "Correction: {0}",
		 * correction);
		 * 
		 * // increase correction or set it to 1.0 if (correction + cfgStepSize
		 * > 1.0f) { correction = 1.0f; } else { correction += cfgStepSize; }
		 * 
		 * sdr.setCorrection(correction);
		 * 
		 * // if there is the main window if (Main.mainWindow != null) { //
		 * update correction display Main.mainWindow.updateCorrection(); } }
		 * else { // if correction equals (or is greater than) 1.0, stop
		 * searching Main.stopSearching(); } } else { // then next time wouldnt
		 * be the first time firstTime = false; }
		 * 
		 * // create data List<Integer> data = new ArrayList<>();
		 * 
		 * // add praeambel for (int i = 0; i < 18; i++) {
		 * data.add(Pocsag.PRAEAMBLE); }
		 * 
		 * // create time message // #00 6:1:ADDRESS:3:MESSAGE String
		 * skyperAddress = Main.getSkyperAddress();
		 * 
		 * // send time addMessage(new
		 * Message("#00 5:1:9C8:0:000000   010112"));
		 * 
		 * // send message to skyper address if (!skyperAddress.isEmpty()) {
		 * String[] parts = new String[] { "#00 6", "1", skyperAddress, "3",
		 * String.format("correction=%+4.2f", sdr.getCorrection()) };
		 * 
		 * // StringBuilder sb = new StringBuilder(); // sb.append("#00 6:1:");
		 * // sb.append(skyperAddress); // sb.append(":3:correction="); //
		 * sb.append(String.format("%+4.2f", AudioEncoder.correction));
		 * 
		 * addMessage(new Message(parts)); }
		 * 
		 * // set send time sendTime = data.size() * 2f / 75f + 0.1;
		 * 
		 * return data;
		 */
		return null;
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
