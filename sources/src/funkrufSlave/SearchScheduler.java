package funkrufSlave;

import java.util.ArrayList;

/**
 * SearchScheduler extends the already existing Scheduler to reduce duplicated code.
 * All constant values can be found in the Scheduler-classfile.
 */
public class SearchScheduler extends Scheduler {

	// constructor
	public SearchScheduler(Log log) {
		super(log);
	}

	// "main"
	@Override
	public void run() {
		// still get local time in 0.1s, add (or sub) correction (here: delay) from master,
		// and take lowest 16 bits (this.max)
		this.time = ((int) (System.currentTimeMillis() / 100) + this.delay) % this.max;

		// update GUI
		if (Main.timeSlots.isSlotChanged(time)) {
			// draw all slots
			Main.drawSlots();
			char slot = Main.timeSlots.getCurrentSlotChar(time);
			log("Scheduler: Updating GUI, now slot " + String.valueOf(slot), Log.INFO);
		}

		switch (currentState) {
			case WAITING_FOR_NEXT_SLOT_TO_BE_ALLOWED:
				if (Main.timeSlots.isNextSlotAllowed(this.time)) {
					// Get time left until next active slot will start
					int timeLeft_100ms = Main.timeSlots.getTimeToNextSlot(this.time);
					// timeLeft_100ms contains now the time until next active slot in 0.1s

					// Check it now its time to encode?
					if (timeLeft_100ms <= MAXENCODETIME_100MS) {
						// Get number of allowed slots in row, starting with the next slot (which must
						// be allowed, otherwise we wouldn't be here. Get next slot by adding 1 and
						// wrapping around

						int nextAllowedSlot = (Main.timeSlots.getCurrentSlot(this.time) + 1) % 16;
						int allowedSlotsCount = Main.timeSlots.getAllowedSlotsInRow(nextAllowedSlot);

						log("Scheduler: next slot will be allowed", Log.INFO);

						log("Scheduler: checkSlot# Next allowed slot: " +
								String.valueOf(Main.timeSlots.iSlotTocSlot(nextAllowedSlot)) +
								" - Number of allowed slots in row: " + String.valueOf(allowedSlotsCount), Log.INFO);


						// Get data from queue according to time slot count
						if (prepareData()) {

							// convert data to a playable sound
							soundData = AudioEncoder.encode(AudioEncoder.getByteData(this.data));
							log("Scheduler: Encoding fertig, state = DATA_ENCODED", Log.INFO);

							this.currentState = states.DATA_ENCODED;
						}
					}
				}
				break;
			case DATA_ENCODED:
				// Current slot already given

				// Check is current slot is allowed
				if (Main.timeSlots.isSlotAllowed(Main.timeSlots.getCurrentSlot(time))) {
					// turn transmitter on
					Main.log.println("Turning transmitter on...", Log.INFO);
					if (Main.serialPortComm != null) Main.serialPortComm.setOn();
					if (Main.gpioPortComm != null) Main.gpioPortComm.setOn();

					// Get current tx delay setting form config
					this.txDelay_ms = Main.config.getDelay();

					log("Scheduler: TX is now on, starting to wait Txdelay", Log.INFO);

					this.currentState = states.WAITING_TX_DELAY;
				}
				break;
			case WAITING_TX_DELAY:
				// Decrease
				txDelay_ms -= TIMERCYCLE_MS;
				// If TXdelay is over...
				if (txDelay_ms <= 0) {
					log("Scheduler: TX delay is over, play started", Log.INFO);

					// play sound in new thread
					AudioEncoder.play(soundData, this);
					this.currentState = states.CURRENTLY_TRANSMITTING;
				}
				break;
			case CURRENTLY_TRANSMITTING:
				break;
			case CURRENT_SLOT_IS_STILL_ALLOWED:
				// If we are still in an allowed slot
				if (Main.timeSlots.isSlotAllowed(time)) {

					// if there is something to send
					if (!Main.messageQueue.isEmpty()) {
						log("Scheduler: Current slot still allowed, prepare data to be transmitted", Log.INFO);
						// Prepare transmitting
						if (prepareData()) {

							// convert data to a playable sound
							soundData = AudioEncoder.encode(AudioEncoder.getByteData(this.data));
							log("Scheduler: Encoding done in current slot duration, state = DATA_ENCODED", Log.INFO);

							this.currentState = states.DATA_ENCODED;
						}
					}
				} else {
					// Reset state machine
					this.currentState = states.WAITING_FOR_NEXT_SLOT_TO_BE_ALLOWED;
					log("Scheduler: Allowed slots left, resetting state machine", Log.INFO);
				}
				break;
		} // switch
	}

	// Prepare data depending on number of allowed slots
	private boolean prepareData() {
		// is correction lower than 1.0?
		if (AudioEncoder.correction < 1.0f) {
			log("correction: " + AudioEncoder.correction, Log.INFO, Log.NORMAL);

			// increase correction or set it to 1.0
			if (AudioEncoder.correction + Main.getStepWidth() > 1.0f) {
				AudioEncoder.correction = 1.0f;
			} else {
				AudioEncoder.correction += Main.getStepWidth();
			}

			// if there is the main window
			if (Main.mainWindow != null) {
				// update correction display
				Main.mainWindow.updateCorrection();
			}
		} else {
			// if correction equals (or is greater than) 1.0, stop searching
			Main.stopSearching();
		}

		// create data
		data = new ArrayList<>();

		// add preamble
		for (int i = 0; i < 18; i++) {
			data.add(Pocsag.PRAEEMBEL);
		}

		// create time message
		// #00 6:1:ADDRESS:3:MESSAGE
		String skyperAddress = Main.getSkyperAddress();

		// send time
		addMessage(new Message(("#00 5:1:9C8:0:000000   010112").split(":")));

		// send message to skyper address
		if (!skyperAddress.equals("")) {
			addMessage(new Message(("#00 6:1:" + skyperAddress + ":3:correction=" + String.format("%+4.2f", AudioEncoder.correction)).split(":")));
		}
		return true;
	}

	private void addMessage(Message message) {
		// add sync-word (start of batch)
		data.add(Pocsag.SYNC);

		// get codewords of message
		ArrayList<Integer> cwBuf = message.getCodeWords();
		int framePos = cwBuf.get(0);

		// add idle-words until frame position is reached
		for (int c = 0; c < framePos; c++) {
			data.add(Pocsag.IDLE);
			data.add(Pocsag.IDLE);
		}

		// add codewords of message
		for (int c = 1; c < cwBuf.size(); c++) {
			if ((data.size() - 18) % 17 == 0) data.add(Pocsag.SYNC);
			data.add(cwBuf.get(c));
		}

		// fill last batch with idle-words
		while ((data.size() - 18) % 17 != 0) {
			data.add(Pocsag.IDLE);
		}
	}
}
