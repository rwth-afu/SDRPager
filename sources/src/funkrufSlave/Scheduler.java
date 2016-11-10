package funkrufSlave;

import java.util.ArrayList;
import java.util.TimerTask;

public class Scheduler extends TimerTask {
	// time corrected by master in 0.1s steps, values should be from 0x0000 to 0xffff
	protected int time = 0x0000;

	// received offset from master in 0.1s steps
	protected int delay = 0;

	// if active is true, time is increased each run
	protected boolean active = true;

	// max time value, as time is a wrapping counter of 0.1 ms and 16 bit long.
	protected final int max = (int) (Math.pow(2, 16));

	// remaining time span to be on air in seconds
	protected double sendTime = 0.0;

	// in general: delay in ms between "PTT On" and start of audio
	// delay time (for serial / gpio) (countdown)
	protected int serialDelay = 0;

	// data list (codewords)
	protected ArrayList<Integer> data;

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
		// if active
		if (active) {
			// increase time
			// this.time = ++this.time % this.max;
			// still get local time in 0.1s, add (or sub) correction (here: delay) from master,
			// and take lowest 16 bits (this.max)
			this.time = ((int) (System.currentTimeMillis() / 100) + this.delay) % this.max;

			// if serial delay is lower than or equals 0
			if (this.serialDelay <= 0) {
				// decrease send time in seconds, 10ms less for each timer run.
				this.sendTime -= 0.01;
			}

			// decrease serial delay by 10ms, as this is the time cycle time
			this.serialDelay -= 10;
		}

		// check slot
		char slot = Main.timeSlots.getCurrentSlot(time);
		boolean isLastSlot = Main.timeSlots.isLastSlot(slot);

		// get count of active slots
		int slotCount = Main.timeSlots.checkSlot(slot);

		// if slot is not the same as the last slot
		if (!isLastSlot) {
			// draw all slots
			Main.drawSlots();
		}

		// if transmission duration is over, switch transmitter off
		if (this.sendTime <= 0) {
			// set pin to off
			if (Main.serialPortComm != null)
				Main.serialPortComm.setOff();
			if (Main.gpioPortComm != null)
				Main.gpioPortComm.setOff();
		}

		// if last transmission is done (send time <= 0) &&
		// there is at least 1 slot &&
		// current slot is not the same as last slot &&
		// the message queue is not empty
		// ---> Prepare next transmission
		// *** This means that data was received during active time slots ***
		if (this.sendTime <= 0 && slotCount > 0 && !isLastSlot && !Main.messageQueue.isEmpty()) {
			log("Scheduler: checkSlot# Erlaubter Slot (" + slot + ") - Anzahl " + slotCount, Log.INFO);

			// set serial delay
			this.serialDelay = Main.config.getDelay();

			// get data and *** set sendTime according to delay and slot count ***
			getData(slotCount);

			// Set Transmitter to on.
			if (Main.serialPortComm != null)
				Main.serialPortComm.setOn();
			if (Main.gpioPortComm != null)
				Main.gpioPortComm.setOn();
		}

		// if serial delay is lower than or equals 0 and there is data
		if (this.serialDelay <= 0 && data != null) {
			// play data and set data to null
			AudioEncoder.play(data);
			data = null;
		}
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

		// set send time
		// data.size gives number of 32 bit values, so total bit number = data.size() * 32
		// time in seconds needs to send this is = total bit number / 1200 Bit/sec
		// reason for +0.1 unknown ??

		this.sendTime = ((data.size() * 32.0) / 1200) + 0.1;
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
