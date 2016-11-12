package funkrufSlave;

public class TimeSlots {
	// slots are from 0 to F, first is 0.

	// slot configuration
	private boolean[] slots;
	// last slot
	private int lastSlot = -1;

	// constructor
	public TimeSlots() {
		// create boolean array
		this.slots = new boolean[16];
	}

	// Take a string with allowed slots and set the local bool array accordingly
	public void setAllowedSlots(String slots) {
		this.slots = new boolean[16];

		for (int i = 0; i < slots.length(); i++) {
			// set slot which is the char interpreted as hex to int
			this.slots[Integer.parseInt(slots.charAt(i) + "", 16)] = true;
		}
	}

	public char iSlotTocSlot (int iSlot)
	{
		if (iSlot >= 0 && iSlot <= 16) {
			return String.format("%1x", iSlot).charAt(0);
		} else
			// TODO: Log-Message
			return "";
	}

	public int cSlotToiSlot (chat cSlot)
	{
		// char interpreted as hex to int
		int iSlot = Integer.parseInt(cSlot + "", 16);
		if (iSlot >= 0 && iSlot <= 16) {
			return iSlot;
		} else
			//TODO: Log-Message
			return (-1);
	}

	public boolean isSlotAllowed(int iSlot)
	{
		if (iSlot >= 0 && iSlot <= 16) {
			return this.slots[iSlot];
		} else {
			// TODO: give log message
			return false;
		}
	}


	// check if slot is allowed and count how many allowed slots are in a row
	// (starting at given slot)
	// Return 0 if given slot is not active
	public int getAllowedSlotsInRow(int iSlot) {
		int count = 0;
		// Loop through all possible timeslots and check if they are allowed
		// If the given slot is already not allowed, the for loop will newer
		// be executed, thus the result is 0
		for (int i = iSlot; (this.slots[i % 16]) && (i < iSlot + 16); i++) {
			// count allowed slots
			count++;
		}
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
		return String.format("%1x", getCurrentSlot(time)).charAt(0);
	}

	// get current slot as int by time
	public int getCurrentSlot(int time) {
		// time (in 0.1s), time per slot 3.75s = 37.5 * 0.1s
		// One time slot has a length of 60 sec/16 slot = 3.75 s = 37.5 * 0.1 s
		// % 16 to warp around complete minutes, as every minute has 16 slots
		int slot = ((int) (time / 37.5)) % 16;
		return slot;
	}

	// check if given slot is last slot
	public boolean isLastSlot(char cSlot) {
		return this.lastSlot == cSlot;
	}

	public boolean isNextSlotAllowed(int time) {
		// Check if next slot is active
		return this.isSlotAllowed((this.getCurrentSlot(time) + 1 ) % 16);
	}
}
