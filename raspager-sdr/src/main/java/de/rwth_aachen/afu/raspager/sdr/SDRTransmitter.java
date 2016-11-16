package de.rwth_aachen.afu.raspager.sdr;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.rwth_aachen.afu.raspager.Configuration;
import de.rwth_aachen.afu.raspager.Transmitter;

public final class SDRTransmitter implements Transmitter {

	private static final Logger log = Logger.getLogger(SDRTransmitter.class.getName());
	private AudioEncoder encoder;
	private SerialPortComm serial;
	private GpioPortComm gpio;
	private int txDelay = 0;

	@Override
	public void close() throws Exception {
		try {
			if (serial != null) {
				serial.close();
				serial = null;
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to close serial port.", t);
		}

		try {
			if (gpio != null) {
				gpio.close();
				gpio = null;
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to close GPIO port.", t);
		}

		encoder = null;
	}

	@Override
	public void init(Configuration config) throws Exception {
		close();

		txDelay = config.getInt("txDelay", 0);
		boolean invert = config.getBoolean("invert", false);

		if (config.getBoolean("serial.use", false)) {
			int pin = SerialPortComm.getPinNumber(config.getString("serial.pin"));
			serial = new SerialPortComm(config.getString("serial.port"), pin, invert);
		}

		if (config.getBoolean("gpio.use", true)) {
			gpio = new GpioPortComm(config.getString("gpio.pin"), invert);
		}

		encoder = new AudioEncoder(config.getString("sdr.device"));
		encoder.setCorrection(config.getFloat("sdr.correction", 0.0f));
	}

	@Override
	public void send(List<Integer> data) throws Exception {
		if (serial == null || gpio == null || encoder == null) {
			throw new UnsupportedOperationException("Not initialized");
		}

		try {
			byte[] enc = encoder.encode(data);

			enable();

			if (txDelay > 0) {
				try {
					Thread.sleep(txDelay);
				} catch (Throwable t) {
					log.log(Level.SEVERE, "Failed to wait for TX delay.", t);
				}
			}

			encoder.play(enc);
		} finally {
			disable();
		}
	}

	private void enable() {
		try {
			if (serial != null) {
				log.fine("Enabling serial pin.");
				serial.setOn();
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to enable serial port.", t);
			throw t;
		}

		try {
			if (gpio != null) {
				log.fine("Enabling GPIO pin.");
				gpio.setOn();
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to enable GPIO port.", t);
			throw t;
		}
	}

	private void disable() {
		try {
			if (serial != null) {
				log.fine("Disabling serial pin.");
				serial.setOff();
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to disable serial port.", t);
		}

		try {
			if (gpio != null) {
				log.fine("Disabling GPIO pin.");
				gpio.setOff();
			}
		} catch (Throwable t) {
			log.log(Level.SEVERE, "Failed to disable GPIO port.", t);
		}
	}

	public void setCorrection(float correction) {
		if (encoder != null) {
			encoder.setCorrection(correction);
		}
	}

	public float getCorrection() {
		if (encoder != null) {
			return encoder.getCorrection();
		} else {
			return 0.0f;
		}
	}
}
