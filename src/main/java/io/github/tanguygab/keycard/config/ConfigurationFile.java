package io.github.tanguygab.keycard.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract class for configuration file
 */
@SuppressWarnings("unchecked")
public abstract class ConfigurationFile {

	//comments in the header of the file
	protected List<String> header;
	
	//config values
	protected Map<String, Object> values;
	
	//the file
	protected File file;
	
	/**
	 * Constructs new instance and copies file from source to destination if it does not exist
	 * @param source - source to copy from if file does not exist
	 * @param destination - destination to load
	 * @param header - comments on top of the file
	 * @throws IllegalStateException - if file does not exist and source is null
	 * @throws IOException - if I/O file operation fails
	 */
	public ConfigurationFile(InputStream source, File destination, List<String> header) throws IllegalStateException, IOException {
		this.header = header;
		this.file = destination;
		if (file.getParentFile() != null) file.getParentFile().mkdirs();
		if (!file.exists()) {
			if (source == null) throw new IllegalStateException("File does not exist and source is null");
			Files.copy(source, file.toPath());
		}
	}
	
	/**
	 * Saves the file to disk
	 */
	public abstract void save();
	
	/**
	 * Returns name of the file
	 * @return name of the file
	 */
	public String getName() {
		return file.getName();
	}
	
	/**
	 * Returns the root map
	 * @return all values
	 */
	public Map<String, Object> getValues(){
		return values;
	}
	
	/**
	 * Replaces values with provided map
	 * @param values - values to replace map with
	 */
	public void setValues(Map<String, Object> values){
		this.values = values;
	}
	
	/**
	 * Gets config option with specified path. If the option is not present and defaultValue is not null,
	 * value is inserted, save() called and defaultValue returned.
	 * @param path - path of the config option
	 * @param defaultValue - value to be inserted and returned if option is not present
	 * @return value from config
	 */
	public Object getObject(String path, Object defaultValue) {
		try {
			Object value = values;
			for (String tab : path.split("\\.")) {
				//reverting compensation for groups with "." in their name
				tab = tab.replace("@#@", ".");
				value = getIgnoreCase((Map<Object, Object>) value, tab);
			}
			if (value == null && defaultValue != null) {
				set(path, defaultValue);
				return defaultValue;
			}
			return value;
		} catch (Throwable e) {
			if (defaultValue != null) set(path, defaultValue);
			return defaultValue;
		}
	}
	
	/**
	 * Returns config option with specified path or null if not present
	 * @param path - path to the value
	 * @return value from file or null if not present
	 */
	public Object getObject(String path) {
		return getObject(path, null);
	}
	
	/**
	 * Gets value from map independent of case
	 * @param map - map to be taken from
	 * @param key - case insensitive key name
	 * @return value from map
	 */
	private Object getIgnoreCase(Map<Object, Object> map, String key) {
		for (Object mapkey : map.keySet()) {
			if (mapkey.toString().equalsIgnoreCase(key)) return map.get(mapkey);
		}
		return map.get(key);
	}
	
	/**
	 * Returns config option with specified path or null if not present
	 * @param path - path to the value
	 * @return value from file or null if not present
	 */
	public String getString(String path) {
		return getString(path, null);
	}
	
	/**
	 * Gets config option with specified path. If the option is not present and defaultValue is not null,
	 * value is inserted, save() called and defaultValue returned.
	 * @param path - path of the config option
	 * @param defaultValue - value to be inserted and returned if option is not present
	 * @return value from config
	 */
	public String getString(String path, String defaultValue) {
		Object value = getObject(path, defaultValue);
		if (value == null) return defaultValue;
		return String.valueOf(value);
	}
	
	/**
	 * Returns config option with specified path or null if not present
	 * @param path - path to the value
	 * @return value from file or null if not present
	 */
	public List<String> getStringList(String path) {
		return getStringList(path, null);
	}
	
	/**
	 * Gets config option with specified path. If the option is not present and defaultValue is not null,
	 * value is inserted, save() called and defaultValue returned.
	 * @param path - path of the config option
	 * @param defaultValue - value to be inserted and returned if option is not present
	 * @return value from config
	 */
	public List<String> getStringList(String path, List<String> defaultValue) {
		Object value = getObject(path, defaultValue);
		if (value == null) return defaultValue;
		if (!(value instanceof List)) {
			return new ArrayList<String>();
		}
		List<String> fixedList = new ArrayList<String>();
		for (Object key : (List<Object>)value) {
			fixedList.add(key.toString());
		}
		return fixedList;
	}
	
	/**
	 * Returns whether file has specified option or not
	 * @param path - path to variable
	 * @return true if present, false if not
	 */
	public boolean hasConfigOption(String path) {
		return getObject(path) != null;
	}
	
	/**
	 * Returns config option with specified path or null if not present
	 * @param path - path to the value
	 * @return value from file or null if not present
	 */
	public Integer getInt(String path) {
		return getInt(path, null);
	}
	
	/**
	 * Gets config option with specified path. If the option is not present and defaultValue is not null,
	 * value is inserted, save() called and defaultValue returned.
	 * @param path - path of the config option
	 * @param defaultValue - value to be inserted and returned if option is not present
	 * @return value from config
	 */
	public Integer getInt(String path, Integer defaultValue) {
		Object value = getObject(path, defaultValue);
		if (value == null) return defaultValue;
		try{
			return Integer.parseInt(value.toString());
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	/**
	 * Returns config option with specified path or null if not present
	 * @param path - path to the value
	 * @return value from file or null if not present
	 */
	public Boolean getBoolean(String path) {
		return getBoolean(path, null);
	}
	
	/**
	 * Gets config option with specified path. If the option is not present and defaultValue is not null,
	 * value is inserted, save() called and defaultValue returned.
	 * @param path - path of the config option
	 * @param defaultValue - value to be inserted and returned if option is not present
	 * @return value from config
	 */
	public Boolean getBoolean(String path, Boolean defaultValue) {
		Object value = getObject(path, defaultValue);
		if (value == null) return defaultValue;
		try{
			return Boolean.parseBoolean(value.toString());
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	/**
	 * Gets config option with specified path. If the option is not present and defaultValue is not null,
	 * value is inserted, save() called and defaultValue returned.
	 * @param path - path of the config option
	 * @param defaultValue - value to be inserted and returned if option is not present
	 * @return value from config
	 */
	public Double getDouble(String path, double defaultValue) {
		Object value = getObject(path, defaultValue);
		if (value == null) return defaultValue;
		try{
			return Double.parseDouble(value.toString());
		} catch (Exception e) {
			return defaultValue;
		}
	}
	
	/**
	 * Returns config option with specified path or empty map if not present
	 * @param path - path to the value
	 * @return value from file or empty map if not present
	 */
	public <K, V> Map<K, V> getConfigurationSection(String path) {
		if (path == null || path.length() == 0) return (Map<K, V>) values;
		Object value = getObject(path, null);
		if (value == null) return new HashMap<>();
		if (value instanceof Map) {
			return (Map<K, V>) value;
		} else {
			return new HashMap<>();
		}
	}
	
	/**
	 * Sets value to the specified path and saves to disk
	 * @param path - path to save to
	 * @param value - value to save
	 */
	public void set(String path, Object value) {
		set(values, path, value);
		save();
	}
	
	/**
	 * Sets value into provided map with provided path and value
	 * @param map - map to insert to
	 * @param path - path
	 * @param value - value
	 * @return the inserted map to allow code chaining
	 */
	private Map<String, Object> set(Map<String, Object> map, String path, Object value) {
		if (path.contains(".")) {
			String keyWord = getRealKey(map, path.split("\\.")[0]);
			Object submap = map.get(keyWord);
			if (submap == null || !(submap instanceof Map)) {
				submap = new HashMap<String, Object>();
			}
			map.put(keyWord.replace("@#@", "."), set((Map<String, Object>) submap, path.substring(keyWord.length()+1, path.length()), value));
		} else {
			if (value == null) {
				map.remove(getRealKey(map, path));
			} else {
				map.put(path, value);
			}
		}
		return map;
	}
	
	/**
	 * Returns the real key name without case sensitivity
	 * @param map - map to check
	 * @param key - key to find
	 * @return The real key name
	 */
	private String getRealKey(Map<?, ?> map, String key) {
		for (Object mapkey : map.keySet()) {
			if (mapkey.toString().equalsIgnoreCase(key)) return mapkey.toString();
		}
		return key;
	}
	
	/**
	 * Returns whether the file contains header with comments or not
	 * @return true if contains or configured header is null, false otherwise
	 */
	public boolean hasHeader() {
		if (header == null) return true;
		for (String line : readAllLines(file)) {
			if (line.contains("#")) return true;
		}
		return false;
	}
	
	/**
	 * Inserts header into file
	 */
	public void fixHeader() {
		if (header == null) return;
		try {
			List<String> content = new ArrayList<String>(header);
			content.addAll(readAllLines(file));
			file.delete();
			file.createNewFile();
			BufferedWriter buf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8));
			for (String line : content) {
				buf.write(line + System.getProperty("line.separator"));
			}
			buf.close();
		} catch (Exception ex) {
			System.out.println("[TAB-BukkitBridge] Failed to modify file " + file);
			ex.printStackTrace();
		}
	}
	
	/**
	 * Reads all lines in file and returns them as List
	 * @return list of lines in file
	 */
	private List<String> readAllLines(File file) {
		List<String> list = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
			String line;
			while ((line = br.readLine()) != null) {
				list.add(line);
			}
			br.close();
		} catch (Exception ex) {
			System.out.println("[TAB-BukkitBridge] Failed to read file " + file);
			ex.printStackTrace();
		}
		return list;
	}
}