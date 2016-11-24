package funkrufSlave;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import java.util.ArrayList;

public class AudioEncoder {
	// 0-2 = begin, 4-36 = constant, 38-39 = end

	private static AudioFormat af48000 = new AudioFormat(48000, 16, 1, true, false);
	private static final float[] bitChange = {-0.9f, -0.7f, 0.0f, 0.7f, 0.9f};
	public static float DEFAULT_CORRECTION = 0f;
	public static float correction = DEFAULT_CORRECTION; // for correction
	// public static float correction = -1;

	public static void play(byte[] inputData, Scheduler scheduler) {
		try {
			Clip c = AudioSystem.getClip(Main.config.getSoundDevice());
			c.open(af48000, inputData, 0, inputData.length);
			c.addLineListener(e -> {
				if (e.getType() == LineEvent.Type.STOP) {
					c.close();
					e.getLine().close();

					scheduler.notifyAudioIsSentCompletely();
				}
			});

			c.start();
			c.loop(0);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static byte[] encode(byte[] inputData) {
		byte sample_size = (byte) (af48000.getSampleSizeInBits() / 8);
		// 100 extra bytes to get the end data to be sent
		byte[] data = new byte[40 * sample_size * inputData.length * 8 + 100];
		int max = (int) Math.pow(2, af48000.getSampleSizeInBits()) / 2 - 1;
		// System.out.println(max);

		boolean lastHigh = false;
		int value = 0;

		// main loop through inputData-array
		for (int i = 0; i < inputData.length; i++) {
			// one byte containing 8 data bit to encode

			int comp = 128; // 0b1000 0000 Bit selection mask

			for (int j = 0; j < 8; j++) {
				// J = Bit index in current byte of input data

				// one bit
				// get index, which tells the start sample in audio sample array
				int index = (i * 8 + j) * 40;

				// select current bit high or low and compare with last
				boolean high = ((inputData[i] & comp) == comp);

				// System.out.println("Aktuelles Byte besthend aus 8 Bits" +
				// inputData[i]);
				// System.out.println("Aktuelles Bit " + high);

				boolean same = (high == lastHigh);
				lastHigh = high;

				// correction factor (for high to low/low to high and high or
				// low)
				// first 576 bits = praeembel
				// if ((high) || (i < 576)) { int f = 1} else { int f = -1}

				// Entweder i < 576 (Präambel), dann immer 1,
				// oder aktueller Bit-Wert ist in f, Werte 1 (=High) oder -1
				// (=Low)
				// int f = high || i < 576 ? 1 : -1;

				// geändert am 12.09.; fixt Ausgabe
				int f = high ? 1 : -1;

				// first 3 bits
				if (index == 0) {
					// start
					for (int l = 0; l <= 2; l++) {
						value = (int) ((int) (bitChange[2 + l] * max) * f * correction);

						// convert value to bytes
						for (int c = 0; c < sample_size; c++) {
							byte sample_byte = (byte) ((value >> (8 * c)) & 0xff);
							data[(index + l) * sample_size + c] = sample_byte;
						}
					}
				} else {
					// other bits
					if (same) {
						for (int l = 0; l < 5; ++l) {
							value = (int) (f * max * correction);

							for (int c = 0; c < sample_size; ++c) {
								byte sample_byte = (byte) ((value >> (8 * c)) & 0xff);
								data[(index - 2 + l) * sample_size + c] = sample_byte;
							}
						}
					} else {
						for (int l = 0; l < 5; ++l) {
							value = (int) (f * bitChange[l] * max * correction);

							for (int c = 0; c < sample_size; ++c) {
								byte sample_byte = (byte) ((value >> (8 * c)) & 0xff);
								data[(index - 2 + l) * sample_size + c] = sample_byte;
							}
						}
					}
				}

				for (int k = 3; k <= 37; k++) {
					// constant value
					value = (int) (f * max * correction);

					// convert value to bytes
					for (int c = 0; c < sample_size; c++) {
						byte sample_byte = (byte) ((value >> (8 * c)) & 0xff);
						data[(index + k) * sample_size + c] = sample_byte;
					}
				}

				// last 2 bit out of 40
				if (i == inputData.length - 1 && j == 7) {
					// end
					for (int l = 0; l <= 1; l++) {
						value = (int) ((int) (bitChange[l] * max) * -f * correction);

						// convert value to bytes
						for (int c = 0; c < sample_size; c++) {
							byte sample_byte = (byte) ((value >> (8 * c)) & 0xff);
							data[(index + 38 + l) * sample_size + c] = sample_byte;
						}
					}
				}

				comp /= 2; // shift bit selection mask 1 bit right to select the
				// next bit in the following loop

			}
		}

		return data;
	}

	public static byte[] getByteData(ArrayList<Integer> data) {
		byte[] byteData = new byte[data.size() * 4];

		for (int i = 0; i < data.size(); i++) {
			for (int c = 0; c < 4; c++) {
				byteData[i * 4 + c] = (byte) (data.get(i) >>> (8 * (3 - c)));
			}
		}

		return byteData;
	}
}