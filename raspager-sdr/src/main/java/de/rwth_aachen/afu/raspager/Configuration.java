package de.rwth_aachen.afu.raspager;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Properties;

/**
 * Wrapper class for the main configuration file.
 * 
 * @author Philipp Thiel
 */
final class Configuration {
	private final Properties props = new Properties();

	/**
	 * Loads the given configuration file.
	 * 
	 * @param fileName
	 *            Configuration file to load.
	 * @throws FileNotFoundException
	 *             If the configuration file does not exist.
	 * @throws IOException
	 *             If an IO error occurred while reading the configuration file.
	 */
	public void load(String fileName) throws FileNotFoundException, IOException {
		try (FileInputStream fin = new FileInputStream(fileName)) {
			props.load(fin);
		}
	}

	/**
	 * Saves the current configuration to the given file.
	 * 
	 * @param fileName
	 *            Name of the configuration file.
	 * @throws FileNotFoundException
	 *             If the configuration file does not exist.
	 * @throws IOException
	 *             If an IO error occurred while writing the configuration file.
	 */
	public void save(String fileName) throws FileNotFoundException, IOException {
		try (FileOutputStream fout = new FileOutputStream(fileName)) {
			props.store(fout, null);
		}
	}

	/**
	 * Checks whether a key exists in the configuration file.
	 * 
	 * @param key
	 *            Key to look for.
	 * @return True if the key exists, false otherwise.
	 */
	public boolean contains(String key) {
		return props.containsKey(key);
	}

	/**
	 * Removes a key from the configuration file.
	 * 
	 * @param key
	 *            Key to remove.
	 * @return True if the key was removed, false otherwise.
	 */
	public boolean remove(String key) {
		return (props.remove(key) != null);
	}

	/**
	 * Sets a boolean value.
	 * 
	 * @param key
	 *            Configuration key.
	 * @param value
	 *            Value to set.
	 */
	public void setBoolean(String key, boolean value) {
		props.setProperty(key, Boolean.toString(value));
	}

	/**
	 * Gets a boolean from the configuration file.
	 * 
	 * @param key
	 *            Configuration key.
	 * @return Value of the configuration key.
	 * @throws NoSuchElementException
	 *             If the given key does not exist.
	 */
	public boolean getBoolean(String key) {
		String value = props.getProperty(key);
		if (value != null) {
			return Boolean.parseBoolean(value);
		} else {
			throw new NoSuchElementException(key);
		}
	}

	/**
	 * Gets a boolean from the configuration file or a default value if the key
	 * does not exist.
	 * 
	 * @param key
	 *            Configuration key.
	 * @param defaultValue
	 *            Default value in case the key does not exist.
	 * @return Value for the configuration key or default value when the key
	 *         does not exist.
	 */
	public boolean getBoolean(String key, boolean defaultValue) {
		String value = props.getProperty(key);
		if (value == null) {
			return defaultValue;
		} else {
			return Boolean.parseBoolean(value);
		}
	}

	/**
	 * Gets a string from the configuration file.
	 * 
	 * @param key
	 *            Configuration key.
	 * @return Value of the configuration key.
	 * @throws NoSuchElementException
	 *             If the given key does not exist.
	 */
	public String getString(String key) {
		String value = props.getProperty(key);
		if (value != null) {
			return value;
		} else {
			throw new NoSuchElementException(key);
		}
	}

	/**
	 * Gets a string from the configuration file.
	 * 
	 * @param key
	 *            Configuration key.
	 * @param defaultValue
	 *            Default value in case the key does not exist.
	 * @return Value for the configuration key or default value when the key
	 *         does not exist.
	 */
	public String getString(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	}

	/**
	 * Sets a string value.
	 * 
	 * @param key
	 *            Configuration key.
	 * @param value
	 *            Value to set.
	 */
	public void setString(String key, String value) {
		props.setProperty(key, value);
	}

	/**
	 * Gets an integer from the configuration file.
	 * 
	 * @param key
	 *            Configuration key.
	 * @return Value for the configuration key.
	 * @throws NoSuchElementException
	 *             If the given key does not exist.
	 * @throws NumberFormatException
	 *             If the configuration value is not a valid number.
	 */
	public int getInt(String key) {
		String value = props.getProperty(key);
		if (value != null) {
			return Integer.parseInt(key);
		} else {
			throw new NoSuchElementException(key);
		}
	}

	/**
	 * Gets an integer from the configuration file.
	 * 
	 * @param key
	 *            Configuration key.
	 * @param defaultValue
	 *            Default value in case the key does not exist.
	 * @return Value for the configuration key or default value when the key
	 *         does not exist.
	 * @throws NumberFormatException
	 *             If the configuration value is not a valid number.
	 */
	public int getInt(String key, int defaultValue) {
		String value = props.getProperty(key);
		if (value != null) {
			return Integer.parseInt(value);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Sets an integer value.
	 * 
	 * @param key
	 *            Configuration key.
	 * @param value
	 *            Value to set.
	 */
	public void setInt(String key, int value) {
		props.setProperty(key, Integer.toString(value));
	}

	/**
	 * Gets a float from the configuration file.
	 * 
	 * @param key
	 *            Configuration key.
	 * @return Value for the configuration key.
	 * @throws NoSuchElementException
	 *             If the given key does not exist.
	 * @throws NumberFormatException
	 *             If the configuration value is not a valid number.
	 */
	public float getFloat(String key) {
		String value = props.getProperty(key);
		if (value != null) {
			return Float.parseFloat(value);
		} else {
			throw new NoSuchElementException(key);
		}
	}

	/**
	 * Gets a float from the configuration file.
	 * 
	 * @param key
	 *            Configuration key.
	 * @param defaultValue
	 *            Default value in case the key does not exist.
	 * @return Value for the configuration key or default value when the key
	 *         does not exist.
	 * @throws NumberFormatException
	 *             If the configuration value is not a valid number.
	 */
	public float getFloat(String key, float defaultValue) {
		String value = props.getProperty(key);
		if (value != null) {
			return Float.parseFloat(value);
		} else {
			return defaultValue;
		}
	}

	/**
	 * Gets a float from the configuration file.
	 * 
	 * @param key
	 *            Configuration key.
	 * @return Value for the configuration key.
	 * @throws NoSuchElementException
	 *             If the given key does not exist.
	 * @throws NumberFormatException
	 *             If the configuration value is not a valid number.
	 */
	public double getDouble(String key) {
		String value = props.getProperty(key);
		if (value != null) {
			return Double.parseDouble(value);
		} else {
			throw new NoSuchElementException(key);
		}
	}

	/**
	 * Gets a double from the configuration file.
	 * 
	 * @param key
	 *            Configuration key.
	 * @param defaultValue
	 *            Default value in case the key does not exist.
	 * @return Value for the configuration key or default value when the key
	 *         does not exist.
	 * @throws NumberFormatException
	 *             If the configuration value is not a valid number.
	 */
	public double getDouble(String key, double defaultValue) {
		String value = props.getProperty(key);
		if (value != null) {
			return Double.parseDouble(value);
		} else {
			return defaultValue;
		}
	}
}
