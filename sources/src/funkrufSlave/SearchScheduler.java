package funkrufSlave;

import java.util.ArrayList;

public class SearchScheduler extends Scheduler {
	// for first message (the correction should not be increased at first time)
	private boolean firstTime = true;

	public SearchScheduler(Log log) {
		super(log);
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

		// if it is not the last slot
		if (!isLastSlot) {
			// draw slots
			Main.drawSlots();
		}

		// if slot is not the last slot or it is the first time
		if ((!isLastSlot || firstTime)) {
			// get data (slotCount = 0, because it is not needed here)
			getData(0);
		}

		// if serial delay is lower than or equals 0 and there is data
		if (this.serialDelay <= 0 && data != null) {
			// play data and set data to null
			AudioEncoder.play(data);
			data = null;
		}
	}

	// get data depending on slot count
	@Override
	public void getData(int slotCount) {
		this.serialDelay = Main.config.getDelay();

		// if it is not the first time
		if (!firstTime) {
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
		} else {
			// then next time wouldnt be the first time
			firstTime = false;
		}

		// create data
		data = new ArrayList<>();

		// add praeambel
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
			addMessage(new Message(
					("#00 6:1:" + skyperAddress + ":3:correction=" + String.format("%+4.2f", AudioEncoder.correction))
							.split(":")));
		}

		// set send time
		this.sendTime = data.size() * 2f / 75f + 0.1;
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
