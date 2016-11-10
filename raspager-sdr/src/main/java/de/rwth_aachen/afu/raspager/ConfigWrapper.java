package de.rwth_aachen.afu.raspager;

public final class ConfigWrapper {

	private final Configuration cfg;

	public ConfigWrapper(Configuration config) {
		this.cfg = config;
	}

	public boolean getInvert() {
		return cfg.getBoolean("invert", false);
	}

	public void setInvert(boolean invert) {
		cfg.setBoolean("invert", invert);
	}

	public int getDelay() {
		return cfg.getInt("delay");
	}

	public void setDelay(int delay) {
		cfg.setInt("delay", delay);
	}

	public String getSoundDevice() {
		return cfg.getString("soundDevice", null);
	}

	public void setSoundDevice(String device) {
		setOrRemove("soundDevice", device);
	}

	public int getPort() {
		return cfg.getInt("net.port", 1337);
	}

	public void setPort(int port) {
		cfg.setInt("net.port", port);
	}

	public String[] getMasters() {
		String value = cfg.getString("net.masters", null);
		if (value != null) {
			return value.split(" +");
		} else {
			return null;
		}
	}

	public void setMasters(String[] masters) {
		setOrRemove("net.masters", String.join(" ", masters));
	}

	public boolean useSerial() {
		return cfg.getBoolean("serial.use", true);
	}

	public void setUseSerial(boolean use) {
		cfg.setBoolean("serial.use", use);
	}

	public String getSerialPort() {
		return cfg.getString("serial.port", null);
	}

	public void setSerialPort(String port) {
		setOrRemove("serial.port", port);
	}

	public String getSerialPin() {
		return cfg.getString("serial.pin", null);
	}

	public void setSerialPin(String pin) {
		setOrRemove("serial.pin", pin);
	}

	public String getRaspiRev() {
		return cfg.getString("gpio.raspirev", null);
	}

	public void setRaspiRev(String rev) {
		setOrRemove("gpio.raspirev", rev);
	}

	public boolean useGpio() {
		return cfg.getBoolean("gpio.use", true);
	}

	public void setUseGpio(boolean use) {
		cfg.setBoolean("gpio.use", use);
	}

	public String getGpioPin() {
		return cfg.getString("gpio.pin", null);
	}

	public void setGpioPin(String pin) {
		setOrRemove("gpio.pin", pin);
	}

	public Configuration getConfiguration() {
		return cfg;
	}

	private void setOrRemove(String key, String value) {
		if (value != null) {
			cfg.setString(key, value);
		} else {
			cfg.remove(key);
		}
	}
}
