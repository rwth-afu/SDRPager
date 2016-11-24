package funkrufSlave;

import java.util.ArrayList;
import java.util.TimerTask;

public class Scheduler extends TimerTask {
	public final static int MAXENCODETIME_100MS = 3;
	public final static int TIMERCYCLE_MS = 10;

	protected enum states {
		WAITING_FOR_NEXT_SLOT_TO_BE_ALLOWED,
		DATA_ENCODED,
		WAITING_TX_DELAY,
		CURRENTLY_TRANSMITTING,
		CURRENT_SLOT_IS_STILL_ALLOWED
	}

	protected states currentState = states.WAITING_FOR_NEXT_SLOT_TO_BE_ALLOWED;

	// time corrected by master in 0.1s steps, values should be from 0x0000 to 0xffff
	protected int time = 0x0000;

	// received offset from master in 0.1s steps
	protected int delay = 0;

	// max time value, as time is a wrapping counter of 0.1 ms and 16 bit long.
	protected final int max = (int) (Math.pow(2, 16));

	// in general: delay in ms between "PTT On" and start of audio
	// delay time (for serial / gpio) (countdown)
	protected int txDelay_ms = 0;

	// data list (codewords)
	protected ArrayList<Integer> data;
	protected byte[] soundData;

	protected Log log = null;

	// write message into log file (log level normal)
	protected void log(String message, int type) {
		log(message, type, Log.DEBUG_SENDING);
	}

	// write message with given log level into log file
	protected void log(String message, int type, int logLevel) {
		if (this.log != null) {
			this.log.println(message, type, logLevel);
		}
	}

	// constructor
	public Scheduler(Log log) {
		this.log = log;
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
				if (Main.timeSlots.isNextSlotAllowed(this.time) && (!Main.messageQueue.isEmpty())) {
					// Get time left until next active slot will start
					int timeLeft_100ms = Main.timeSlots.getTimeToNextSlot(this.time);
					// timeLeft_100ms contains now the time until next active slot in 0.1s

					// Check it now its time to encode?
					if (timeLeft_100ms <= MAXENCODETIME_100MS) {

						// Just DEBUG
						if (!Main.messageQueue.isEmpty()) {
							log("Scheduler: messageQueue is not empty", Log.INFO);
						} else {
							log("Scheduler: messageQueue is empty", Log.INFO);
						}

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
						if (prepareData(allowedSlotsCount)) {

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
						int currentSlot = Main.timeSlots.getCurrentSlot(this.time);
						int allowedSlotsCount = Main.timeSlots.getAllowedSlotsInRow(currentSlot);
						if (prepareData(allowedSlotsCount)) {

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

	public void notifyAudioIsSentCompletely() {
		// If Audio is sent, first

		// turn transmitter off
		Main.log.println("Turning transmitter off...", Log.INFO);
		if (Main.serialPortComm != null) Main.serialPortComm.setOff();
		if (Main.gpioPortComm != null) Main.gpioPortComm.setOff();

		// if the current slot is still allowed, remember this
		if (Main.timeSlots.isSlotAllowed(time)) {
			this.currentState = states.CURRENT_SLOT_IS_STILL_ALLOWED;
		} else {
			// else reset state machine
			this.currentState = states.WAITING_FOR_NEXT_SLOT_TO_BE_ALLOWED;
		}
	}

	// Prepare data depending on number of allowed slots
	private boolean prepareData(int slotCount) {
		int numberOfMessagesProcessed = 0;
		// send batches
		// max batches per slot: (slot time - preamble time) / bps / ((frames + (1 = sync)) * bits per frame)
		// (6.4 - 0.48) * 1200 / ((16 + 1) * 32)
		int maxBatch = (int) ((((6.4 * slotCount) - 0.48 - (Main.config.getDelay() / 1000)) * 1200) / 544);

		// create data
		data = new ArrayList<>();

		// add preamble
		for (int i = 0; i < 18; i++) {
			data.add(Pocsag.PRAEEMBEL);
		}

		// get messages as long as message queue is not empty
		while (!Main.messageQueue.isEmpty()) {
			// get message
			Message message = Main.messageQueue.pop();
			// Increase counter
			numberOfMessagesProcessed++;

			// get codewords and frame position
			ArrayList<Integer> cwBuf = message.getCodeWords();
			int framePos = cwBuf.get(0);
			int cwCount = cwBuf.size() - 1;

			// falls message nicht mehr passt (da dann zu viele Batches), zurück in Queue

			// (data.size() - 18) / 17 = aktBatches
			// aktBatches + (cwCount + 2 * framePos) / 16 + 1 = Batches NACH hinzufügen
			// also Batches NACH hinzufügen > maxBatches, dann keine neue Nachricht holen
			// if count of batches + this message is greater than max batches
			if (((data.size() - 18) / 17 + (cwCount + 2 * framePos) / 16 + 1) > maxBatch) {
				// push message back in queue (first position)
				Main.messageQueue.addFirst(message);

				// Decrease counter, as the last messaged processed did not fit
				numberOfMessagesProcessed--;
				break;
			}

			// each batch starts with sync-word
			data.add(Pocsag.SYNC);

			// add idle-words until frame position is reached
			for (int c = 0; c < framePos; c++) {
				data.add(Pocsag.IDLE);
				data.add(Pocsag.IDLE);
			}

			// add data
			for (int c = 1; c < cwBuf.size(); c++) {
				if ((data.size() - 18) % 17 == 0)
					data.add(Pocsag.SYNC);

				data.add(cwBuf.get(c));
			}

			// fill batch with idle-words
			while ((data.size() - 18) % 17 != 0) {
				data.add(Pocsag.IDLE);
			}
		}

		// info about max batches
		log("Scheduler: # used batches (" + ((data.size() - 18) / 17) + " / " + maxBatch + ")", Log.INFO);
		return (numberOfMessagesProcessed > 0);
	}

	// get current time
	public int getTime() {
		return time;
	}

	// correct time by delay
	public void correctTime(int delay) {
		this.delay += delay;
	}
}
