package de.rwth_aachen.afu.raspager;

final class TimeSlots {

	private final boolean[] slots = new boolean[16];
	private char lastSlot = ' ';

	/**
	 * Sets active slots based on string representation.
	 * 
	 * @param s
	 *            String representation of active slots.
	 */
	public synchronized void setSlots(String s) {
		// Reset all to false (instead of creating a new array)
		for (int i = 0; i < slots.length; ++i) {
			slots[i] = false;
		}

		for (int i = 0; i < s.length(); ++i) {
			int idx = Character.digit(s.charAt(i), 16);
			slots[idx] = true;
		}
	}

	/**
	 * Checks if slot is allowed and counts how many active slots are in a row.
	 * 
	 * @param cs
	 *            Slot to check.
	 * @return Number of active slots.
	 */
	public synchronized int getSlotCount(char cs) {
		int c = getSlotCount(Character.digit(cs, 16));
		lastSlot = cs;

		return c;
	}

	/**
	 * Checks if slot is allowed and counts how many active slots are in a row.
	 * 
	 * @param slot
	 *            Slot to check.
	 * @return Number of active slots.
	 */
	public synchronized int getSlotCount(int slot) {
		int count = 0;

		for (int i = slot; slots[i % 16] && (i < slot + 16); ++i) {
			++count;
		}

		return count;
	}

	/**
	 * Gets active slots as a string.
	 * 
	 * @return String containing active slot indices.
	 */
	public synchronized String getSlots() {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < slots.length; ++i) {
			if (slots[i]) {
				sb.append(String.format("%1x", i));
			}
		}

		return sb.toString();
	}

	/**
	 * Returns the value of the given slot.
	 * 
	 * @param index
	 *            Slot index (smaller than 16).
	 * @return Status of the slot at the given index.
	 */
	public synchronized boolean getSlot(int index) {
		return slots[index % 16];
	}

	/**
	 * Checks if the given slot number is the last slot.
	 * 
	 * @param cs
	 *            Slot number
	 * @return True if the given slot number is the last slot.
	 */
	public synchronized boolean isLastSlot(char cs) {
		return lastSlot == cs;
	}

	/**
	 * Cheks if the next slot will be active.
	 * 
	 * @param time
	 *            Time
	 * @return True if the next slot will be active.
	 */
	public synchronized boolean isNextSlotAllowed(int time) {
		return getSlot(getCurrentSlot(time) + 1);
	}

	/**
	 * Gets the current slot for the given time value.
	 * 
	 * @param time
	 *            Time value
	 * @return Current slot as hex number.
	 */
	public static char getCurrentSlot(int time) {
		return Character.forDigit(getSlotIndex(time), 16);
	}

	/**
	 * Gets the current slot index for the given time value.
	 * 
	 * @param time
	 *            Time value.
	 * @return Slot index.
	 */
	public static int getSlotIndex(int time) {
		// time (in 0.1s), time per slot 6.4 s = 64 * 0.1s
		// % 16 to warp around complete minutes, as there are 16 timeslots
		// avaliable.

		// **** IMPORTANT ****

		// This means 16 timeslots need 102.4 seconds, not 60.
		return ((int) (time / 64)) % 16;
	}

	public static int getStartTimeForSlot(int slot, int time) {
		return ((time % 1024) + (slot * 64));
	}

	public static int getEndTimeForSlot(int slot, int time) {
		return (getStartTimeForSlot(slot, time) + 1024);
	}

	public static int getTimeToNextSlot(int time) {
		int nextSlot = (getCurrentSlot(time) + 1) % 16;
		return (getStartTimeForSlot(nextSlot, time) - time);
	}

}
