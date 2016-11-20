package de.rwth_aachen.afu.raspager.sdr;

import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.Mixer;

/**
 * This class contains the audio encoder that encodes code words into an audio
 * clip which can be played by a sound card.
 */
final class AudioEncoder {
	// 0-2 = begin, 4-36 = constant, 38-39 = end
	private static final AudioFormat af48000 = new AudioFormat(48000, 16, 1, true, false);
	private static final float[] bitChange = { -0.9f, -0.7f, 0.0f, 0.7f, 0.9f };
	private Mixer.Info device = null;
	private float correction = 0.0f;
	private Object playMutex = new Object();

	/**
	 * Constructs a new audio encoder using the given sound device.
	 * 
	 * @param soundDevice
	 *            Name of the sound device to use. This must match the value
	 *            returned by {@link Mixer.Info#getName() getName}.
	 */
	public AudioEncoder(String soundDevice) {
		Mixer.Info[] soundDevices = AudioSystem.getMixerInfo();
		for (Mixer.Info device : soundDevices) {
			if (device.getName().equalsIgnoreCase(soundDevice)) {
				this.device = device;
				break;
			}
		}

		if (device == null) {
			throw new IllegalArgumentException("Sound device does not exist.");
		}
	}

	/**
	 * Gets the correction factor.
	 * 
	 * @return Correction factor.
	 */
	public float getCorrection() {
		return correction;
	}

	/**
	 * Sets the correction factor.
	 * 
	 * @param correction
	 */
	public void setCorrection(float correction) {
		this.correction = correction;
	}

	/**
	 * Encodes a list of code words into a byte array that can be send to the
	 * soundcard.
	 * 
	 * @param data
	 *            List of code words
	 * @return Byte array containing the encoded data.
	 */
	public byte[] encode(List<Integer> data) {
		return encode(toByteArray(data), correction);
	}

	/**
	 * Plays the encoded data via the sound device.
	 * 
	 * @param data
	 *            Data to play.
	 * @throws Exception
	 *             If an error occurred.
	 */
	public void play(byte[] data) throws Exception {
		try (Clip c = AudioSystem.getClip(device)) {
			/*
			 * // === DOWNSAMPLING AUF 44100 Hz ===
			 * 
			 * // Original inputData / soundData AudioInputStream
			 * audioInputStream = new AudioInputStream(new
			 * ByteArrayInputStream(soundData), af48000, soundData.length);
			 * 
			 * // Obtains an audio input stream of the indicated format, by
			 * converting the provided audio input stream. AudioInputStream
			 * inputStream = AudioSystem.getAudioInputStream(new
			 * AudioFormat(44100, 16, 1, true, false), audioInputStream);
			 * 
			 * c.open(inputStream);
			 */

			// auskommentieren, falls Downsampling verwendet werden soll
			c.open(af48000, data, 0, data.length);
			c.addLineListener((e) -> {
				if (e.getType() == LineEvent.Type.STOP) {
					c.close();
					e.getLine().close();

					synchronized (playMutex) {
						playMutex.notify();
					}
				}
			});

			c.start();
			c.loop(0);

			try {
				synchronized (playMutex) {
					playMutex.wait();
				}
			} catch (InterruptedException ex) {
				throw ex;
			}
		}
	}

	private static byte[] encode(byte[] inputData, float correction) {
		byte sample_size = (byte) (af48000.getSampleSizeInBits() / 8);
		// 100 extra bytes to get the end data to be sent
		byte[] data = new byte[40 * sample_size * inputData.length * 8 + 100];
		int max = (int) Math.pow(2, af48000.getSampleSizeInBits()) / 2 - 1;

		boolean lastHigh = false;
		int value = 0;

		for (int i = 0; i < inputData.length; i++) {
			// 0b1000 0000 Bit selection mask
			int comp = 128;

			// one byte containing 8 data bits to encode
			for (int j = 0; j < 8; j++) {
				// j = Bit index in current byte of input data

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

				// shift bit selection mask 1 bit right to select the
				// next bit in the following loop
				comp /= 2;
			}
		}

		return data;
	}

	/**
	 * Converts the integer list into a byte array.
	 * 
	 * @param data
	 *            Integer list
	 * @return Byte array containing the integer data.
	 */
	private static byte[] toByteArray(List<Integer> data) {
		byte[] byteData = new byte[data.size() * 4];

		for (int i = 0; i < data.size(); i++) {
			int value = data.get(i);

			byteData[i * 4] = (byte) (value >>> 24);
			byteData[i * 4 + 1] = (byte) (value >>> 16);
			byteData[i * 4 + 2] = (byte) (value >>> 8);
			byteData[i * 4 + 3] = (byte) value;
		}

		return byteData;
	}
}