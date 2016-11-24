package de.rwth_aachen.afu.raspager.sdr;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;

final class GpioPortComm {
	private GpioController gpio;
	private GpioPinDigitalOutput gpioPin = null;
	private boolean invert = false;
	private boolean curOn = true;

	/**
	 * Creates a new GPIO controller.
	 * 
	 * @param pinName
	 *            Name of the GPIO pin to use.
	 * @param invert
	 *            Invert pin behavior.
	 */
	public GpioPortComm(String pinName, boolean invert) {
		this.invert = invert;
		gpio = GpioFactory.getInstance();

		Pin pin = RaspiPin.getPinByName(pinName);
		gpioPin = gpio.provisionDigitalOutputPin(pin, "FunkrufSlave", PinState.LOW);
		gpioPin.setShutdownOptions(true, PinState.LOW);
	}

	/**
	 * Enables the GPIO pin.
	 * 
	 * @throws IllegalStateException
	 *             If the GPIO pin is not initialized.
	 */
	public void setOn() {
		setStatus(!invert);
	}

	/**
	 * Disables the GPIO pin.
	 * 
	 * @throws IllegalStateException
	 *             If the GPIO pin is not initialized.
	 */
	public void setOff() {
		setStatus(invert);
	}

	/**
	 * Sets the pin status.
	 * 
	 * @param on
	 *            Enable or disable the pin.
	 * @throws IllegalStateException
	 *             If the GPIO pin is not initialized.
	 */
	private void setStatus(boolean on) {
		if (gpioPin == null) {
			throw new IllegalStateException("GPIO pin not initialized");
		}

		if (on && !curOn) {
			gpioPin.low();
			curOn = true;
		} else if (!on && curOn) {
			gpioPin.high();
			curOn = false;
		}
	}

	/**
	 * Closes the GPIO pin.
	 */
	public void close() {
		if (gpio != null) {
			gpioPin.low();
			gpio.shutdown();
			gpio.unprovisionPin(gpioPin);
		}
	}
}
