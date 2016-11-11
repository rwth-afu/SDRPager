package funkrufSlave;

import java.util.ArrayList;
import java.util.TimerTask;

public class Scheduler extends TimerTask {
	public final static int MAXENCODETIME_100ms = 3;
	public final static int TIMERCYCLE_MS = 10;

	protected enum states {
		WAITING_FOR_NEXT_SLOT_TO_BE_ACTIVE, DATA_ENCODED, WAITING_TX_DELAY, CURRENTLY_TRANSMITTING
	}

	protected states currentState = states.WAITING_FOR_NEXT_SLOT_TO_BE_ACTIVE;

	// time corrected by master in 0.1s steps, values should be from 0x0000 to 0xffff
	protected int time = 0x0000;

	// received offset from master in 0.1s steps
	protected int delay = 0;

	// max time value, as time is a wrapping counter of 0.1 ms and 16 bit long.
	protected final int max = (int) (Math.pow(2, 16));

	// in general: delay in ms between "PTT On" and start of audio
	// delay time (for serial / gpio) (countdown)
	protected int TxDelay_ms = 0;

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
		char slot = Main.timeSlots.getCurrentSlot(time);
		if (!Main.timeSlots.isLastSlot(slot)) {
			// draw all slots
			Main.drawSlots();
			log("Scheduler: Updating GUI, now slot " + slot, Log.INFO);
		}

		switch (currentState) {
			case WAITING_FOR_NEXT_SLOT_TO_BE_ACTIVE:
				if (Main.timeSlots.nextSlotIsActive(this.time) && (!Main.messageQueue.isEmpty())) {
/*					// Get time left until next active slot will start
					int time_left_100ms = 0;
					// As long as the time_left_100ms is NOT enough to reach next time slot...
					while (!
							// Next timeslot regarding from now on
							(((Main.timeSlots.getCurrentSlotInt(time) + 1) % 16) ==
									// Timeslot of (now+time_left_100ms
									Main.timeSlots.getCurrentSlotInt((time + time_left_100ms) % max)))
					{
						// Increase and try again
						time_left_100ms++;
					}

					// time_left_100ms contains now the time until next active slot in 0.1s

					// Check it now its time to encode?
					if (time_left_100ms <= MAXENCODETIME_100ms) {
*/

					if (!Main.messageQueue.isEmpty())
					{
						log("Scheduler: messageQueue is not empty", Log.INFO);
					} else
					{
						log("Scheduler: messageQueue is empty", Log.INFO);
					}

					slot = Main.timeSlots.getCurrentSlot(time);
					// get count of active slots
					int slotCount = Main.timeSlots.checkSlot(slot);

					log("Scheduler: next slot will be active", Log.INFO);

					log("Scheduler: checkSlot# Erlaubter Slot (" + slot + ") - Anzahl " + slotCount, Log.INFO);


					// get data and *** set sendTime according to delay and slot count ***
					getData(slotCount);

					// convert data to a playable sound
					soundData = AudioEncoder.encode(AudioEncoder.getByteData(this.data));
					log("Scheduler: Encoding fertig, state = DATA_ENCODED", Log.INFO);

					this.currentState = states.DATA_ENCODED;

				}
				break;

			case DATA_ENCODED:
				// Current slot already given

				// Check is current slot is active
				if (Main.timeSlots.checkSlot(slot) > 0) {
					// turn transmitter on
					Main.log.println("Turning transmitter on...", Log.INFO);
					if (Main.serialPortComm != null) Main.serialPortComm.setOn();
					if (Main.gpioPortComm != null) Main.gpioPortComm.setOn();

					// Get current tx delay setting form config
					this.TxDelay_ms = Main.config.getDelay();

					log("Scheduler: TX is now on, starting to wait Txdelay", Log.INFO);

					this.currentState = states.WAITING_TX_DELAY;
				}
				break;
			case WAITING_TX_DELAY:
				// Decrease
				TxDelay_ms -= TIMERCYCLE_MS;
				// If TXdelay is over...
				if (TxDelay_ms <= 0) {
					log("Scheduler: TX delay is over, play started", Log.INFO);

					// play sound in new thread
					AudioEncoder.play(soundData, this);
					this.currentState = states.CURRENTLY_TRANSMITTING;
				}
				break;

			case CURRENTLY_TRANSMITTING:

				break;
		}
	}

	public void Notify_Audio_is_sent_completely() {
		// If Audio is sent, first

		// turn transmitter off
		Main.log.println("Turning transmitter off...", Log.INFO);
		if (Main.serialPortComm != null) Main.serialPortComm.setOff();
		if (Main.gpioPortComm != null) Main.gpioPortComm.setOff();

		// then reset state machine
		this.currentState = states.WAITING_FOR_NEXT_SLOT_TO_BE_ACTIVE;
	}

	// get data depending on slot count
	public void getData(int slotCount) {
		// send batches
		// max batches per slot: (slot time - preamble time) / bps / ((frames + (1 = sync)) * bits per frame)
		// (3,75 - 0,48) * 1200 / ((16 + 1) * 32)
		int maxBatch = (int) ((6.40 * slotCount - 0.48 - Main.config.getDelay() / 1000) * 1200 / 544);

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

		// infos about max batches
		log("Scheduler: # used batches (" + ((data.size() - 18) / 17) + " / " + maxBatch + ")", Log.INFO);
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
