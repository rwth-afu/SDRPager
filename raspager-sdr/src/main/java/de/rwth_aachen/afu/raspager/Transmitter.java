package de.rwth_aachen.afu.raspager;

import java.util.List;

public interface Transmitter extends AutoCloseable {

	/**
	 * Initializes the transmitter.
	 * 
	 * @param config
	 *            Handle to the configuration file.
	 * @throws Exception
	 *             If an error occurred during initialization.
	 */
	void init(Configuration config) throws Exception;

	/**
	 * Encodes the code words into a raw byte array.
	 * 
	 * @param data
	 *            Code words to encode.
	 * @return Byte array containing the encoded data.
	 * @throws Exception
	 *             If an error occurred while encoding the data.
	 */
	byte[] encode(List<Integer> data) throws Exception;

	/**
	 * Sends the encoded data over the air.
	 * 
	 * @param data
	 *            Data to send.
	 * @throws Exception
	 *             If an error occurred while sending the data.
	 */
	void send(byte[] data) throws Exception;
}
