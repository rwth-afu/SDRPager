package funkrufSlave;

public class TimeSlots {
	// slot configuration
	private boolean[] slots;
	// last slot
	private char lastSlot = ' ';

	// constructor
	public TimeSlots() {
		// create boolean array
		this.slots = new boolean[16];
	}

	// parse string and set configuration
	public void setSlots(String slots) {
		this.slots = new boolean[16];

		for (int i = 0; i < slots.length(); i++) {
			// set slot which is the char interpreted as hex to int
			this.slots[Integer.parseInt(slots.charAt(i) + "", 16)] = true;
		}
	}

	// check if slot is allowed and count how many allowed slots are in a row
	// (starting at given slot)
	public int checkSlot(char cSlot) {
		int count = 0;
		// char interpreted as hex to int
		int iSlot = Integer.parseInt(cSlot + "", 16);

		for (int slot = iSlot; this.slots[slot % 16] && slot < iSlot + 16; slot++) {
			// count allowed slots
			count++;
		}

		// set last slot to this slot
		this.lastSlot = cSlot;

		return count;
	}

	// get all allowed slots as string
	public String getSlots() {
		String s = "";

		for (int i = 0; i < this.slots.length; i++) {
			// if slot is allowed
			if (this.slots[i]) {
				// int to char in hex format
				s += String.format("%1x", i);
			}
		}

		return s;
	}

	// get slot array
	public boolean[] getSlotsArray() {
		return this.slots;
	}

	// get current slot as char by time
	public char getCurrentSlot(int time) {
		// getCurrentSlotInt and convert int to char in hex format
		return String.format("%1x", getCurrentSlotInt(time)).charAt(0);

	}

	// get current slot as int by time
	public int getCurrentSlotInt(int time) {
		// time (in 0.1s)
		// time per slot 3.75s = 37.5 * 0.1s
		// total 16 slots
		int slot = ((int) (time / 64.0)) % 16;

		return slot;
	}

	// check if given slot is last slot
	public boolean isLastSlot(char cSlot) {
		return this.lastSlot == cSlot;
	}

}
