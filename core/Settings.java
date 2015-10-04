/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.Stack;

import util.Range;

/**
 * Interface for simulation settings stored in setting file(s). Settings 
 * class should be initialized before using (with {@link #init(String)}). If
 * Settings isn't initialized, only settings in {@link #DEF_SETTINGS_FILE} 
 * are read. Normally, after initialization, settings in the given file can
 * override any settings defined in the default settings file and/or define 
 * new settings.
 * <P> All settings are key-value pairs. For parsing details see 
 * {@link java.util.Properties#getProperty(String)}. Value can be a single 
 * value or comma separated list of values. With CSV values, CSV methods
 * must be used (e.g. {@link #getCsvInts(String, int)}). Setting value should
 * not start and end with a bracket since those are reserved for run-specific
 * values (see {@link #setRunIndex(int)}). In file paths directory separator
 * should always be forward slash ("/").
 * </P> 
 */
public class Settings {
	/** properties object where the setting files are read into */
	protected static Properties props;
	/** file name of the default settings file ({@value}) */
	public static final String DEF_SETTINGS_FILE ="default_settings.txt";
	
	/** 
	 * Setting to define the file name where all read settings are written
	 * ({@value}. If set to an empty string, standard output is used. 
	 * By default setting are not written anywhere. 
	 */
	public static final String SETTING_OUTPUT_S = "Settings.output";
	
	/** delimiter for requested values in strings ({@value})
	 * @see #valueFillString(String) */
	public static final String FILL_DELIMITER = "%%";
	
	/** Stream where all read settings are written to */
	private static PrintStream out = null;
	private static Set<String> writtenSettings = new HashSet<String>();
	
	/** run index for run-specific settings */
	private static int runIndex = 0;
	private String namespace = null; // namespace to look the settings from
	private String secondaryNamespace = null;
	private Stack<String> oldNamespaces;
	private Stack<String> secondaryNamespaces;
	
	/**
	 * Creates a setting object with a namespace. Namespace is the prefix
	 * of the all subsequent setting requests.
	 * @param namespace Namespace to use
	 */
	public Settings(String namespace) {
		this.oldNamespaces = new Stack<String>();
		this.secondaryNamespaces = new Stack<String>();
		setNameSpace(namespace);
	}
	
	/**
	 * Create a setting object without namespace. All setting requests must
	 * be prefixed with a valid namespace (e.g. "Report.nrofReports").
	 */
	public Settings() {
		this(null);
	}
	
	/**
	 * Sets the run index for the settings (only has effect on settings with
	 * run array). A run array can be defined with syntax<BR>
	 * <CODE>[settingFor1stRun ; settingFor2ndRun ; SettingFor3rdRun]</CODE>
	 * <BR>I.e. settings are put in brackets and delimited with semicolon.
	 * First run's setting is returned when index is 0, second when index is
	 * 1 etc. If run index is bigger than run array's length, indexing wraps
	 * around in run array (i.e. return value is the value at index 
	 * <CODE>runIndex % arrayLength</CODE>). 
	 * To disable whole run-index-thing, set index to value smaller than
	 * zero (e.g. -1). When disabled, run-arrays are returned as normal values,
	 * including the brackets.
	 * @param index The run index to use for subsequent settings calls, or
	 * -1 to disable run indexing
	 */
	public static void setRunIndex(int index) {
		runIndex = index;
		writtenSettings.clear();
	}
	
	/**
	 * Checks that the given integer array contains a valid range. I.e., 
	 * the length of the array must be two and 
	 * <code>first_value <= second_value</code>.
	 * @param range The range array
	 * @param sname Name of the setting (for error messages)
	 * @throws SettingsError If the given array didn't qualify as a range
	 */
	public void assertValidRange(int range[], String sname) 
		throws SettingsError {
		if (range.length != 2) {
			throw new SettingsError("Range setting " + 
					getFullPropertyName(sname) + 
					" should contain only two comma separated integer values");
		}
		if (range[0] > range[1]) {
			throw new SettingsError("Range setting's " + 
					getFullPropertyName(sname) + 
					" first value should be smaller or equal to second value");
		}
	}
	
	/**
	 * Makes sure that the given settings value is positive
	 * @param value Value to check
	 * @param settingName Name of the setting (for error's message)
	 * @throws SettingsError if the value was not positive
	 */
	public void ensurePositiveValue(double value, String settingName) {
		if (value < 0) {
			throw new SettingsError("Negative value (" + value + 
					") not accepted for setting " + settingName);
		}
	}
	
	/**
	 * Sets the namespace to something else than the current namespace.
	 * This change can be reverted using {@link #restoreNameSpace()}
	 * @param namespace The new namespace
	 */
	public void setNameSpace(String namespace) {
		this.oldNamespaces.push(this.namespace);
		this.namespace = namespace;
	}
	
	/**
	 * Appends the given namespace to the the current namespace, <strong>
	 * for both the primary and secondary namespace </strong>.
	 * This change can be reverted using {@link #restoreNameSpace()} and
	 * {@link #restoreSecondaryNamespace()}.
	 * @param namespace The new namespace to append
	 */
	public void setSubNameSpace(String namespace) {
		this.oldNamespaces.push(this.namespace);
		this.namespace = this.namespace + "." + namespace;
		this.secondaryNamespaces.push(this.secondaryNamespace);
		this.secondaryNamespace = this.secondaryNamespace + "." + namespace;
	}
	
	/**
	 * Returns full (namespace prefixed) property name for a setting.
	 * @param setting The name of the setting
	 * @return The setting name prefixed with fully qualified name of the
	 * namespace where the requested setting would be retrieved from or null
	 * if that setting is not found from any of the current namespace(s)
	 */
	public String getFullPropertyName(String setting) {
		if (!contains(setting)) {
			return null;
		}
		
		if (props.getProperty(getFullPropertyName(setting, false)) != null) {
			return getFullPropertyName(setting, false);
		}
		
		// not found from primary, but Settings contains -> must be from 2ndary
		else return getFullPropertyName(setting, true);
	}
	
	/** 
	 * Returns the namespace of the settings object
	 * @return the namespace of the settings object
	 */
	public String getNameSpace() {
		return this.namespace;
	}
	
	/** 
	 * Returns the secondary namespace of the settings object
	 * @return the secondary namespace of the settings object
	 */
	public String getSecondaryNameSpace() {
		return this.secondaryNamespace;
	}
	
	/**
	 * Sets a secondary namespace where a setting is searched from if it
	 * isn't found from the primary namespace. Secondary namespace can
	 * be used e.g. as a "default" space where the settings are looked from
	 * if no specific setting is set.
	 * This change can be reverted using {@link #restoreSecondaryNamespace()}
	 * @param namespace The new secondary namespace or null if secondary
	 * namespace is not used (default behavior)
	 */
	public void setSecondaryNamespace(String namespace) {
		this.secondaryNamespaces.push(this.secondaryNamespace);
		this.secondaryNamespace = namespace;
	}
		
	/**
	 * Restores the namespace that was in use before a call to setNameSpace
	 * @see #setNameSpace(String)
	 */
	public void restoreNameSpace() {
		this.namespace = this.oldNamespaces.pop();
	}
	
	/**
	 * Restores the secondary namespace that was in use before a call to
	 * setSecondaryNameSpace
	 * @see #setSecondaryNamespace(String)
	 */
	public void restoreSecondaryNamespace() {
		this.secondaryNamespace = this.secondaryNamespaces.pop();
	}
	
	/**
	 * Reverts the change made with {@link #setSubNameSpace(String)}, i.e., 
	 * restores both the primary and secondary namespace.
	 */
	public void restoreSubNameSpace() {
		restoreNameSpace();
		restoreSecondaryNamespace();
	}
	
	/**
	 * Initializes the settings all Settings objects will use. This should be
	 * called before any setting requests. Subsequent calls replace all
	 * old settings and then Settings contains only the new settings.
	 * The file {@link #DEF_SETTINGS_FILE}, if exists, is always read.
	 * @param propFile Path to the property file where additional settings
	 * are read from or null if no additional settings files are needed.
	 * @throws SettingsError If loading the settings file(s) didn't succeed
	 */
	public static void init(String propFile) throws SettingsError {
		String outFile;
		try {
			if (new File(DEF_SETTINGS_FILE).exists()) {
				Properties defProperties = new Properties();
				defProperties.load(new FileInputStream(DEF_SETTINGS_FILE));
				props = new Properties(defProperties);
			}
			else {
				props = new Properties();
			}
			if (propFile != null) {
				props.load(new FileInputStream(propFile));
			}
		} catch (IOException e) {
			throw new SettingsError(e);
		}

		outFile = props.getProperty(SETTING_OUTPUT_S);
		if (outFile != null) {
			if (outFile.trim().length() == 0) {
				out = System.out;
			} else {
				try {
					out = new PrintStream(new File(outFile));
				} catch (FileNotFoundException e) {
					throw new SettingsError("Can't open Settings output file:" +
							e);
				}
			}
		}
	}
	
	/**
	 * Reads another settings file and adds the key-value pairs to the current
	 * settings overriding any values that already existed with the same keys.
	 * @param propFile Path to the property file
	 * @throws SettingsError If loading the settings file didn't succeed
	 * @see #init(String)
	 */
	public static void addSettings(String propFile) throws SettingsError {
		try {
			props.load(new FileInputStream(propFile));
		} catch (IOException e) {
			throw new SettingsError(e);
		}
	}
	
	/**
	 * Writes the given setting string to the settings output (if any)
	 * @param setting The string to write
	 */
	private static void outputSetting(String setting) {
		if (out != null && !writtenSettings.contains(setting)) {
			if (writtenSettings.size() == 0) {
				out.println("# Settings for run " + (runIndex + 1));
			}
			out.println(setting);
			writtenSettings.add(setting);
		}
	}
	
	/**
	 * Returns true if a setting with defined name (in the current namespace
	 * or secondary namespace if such is set) exists and has some value 
	 * (not just white space)
	 * @param name Name of the setting to check
	 * @return True if the setting exists, false if not
	 */
	public boolean contains(String name) {
		try {
			String value = getSetting(name); 
			if (value == null) {
				return false;
			}
			
			else return value.trim().length() > 0;
		}
		catch (SettingsError e) {
			return false; // didn't find the setting
		}
	}
	
	/**
	 * Returns full (namespace prefixed) property name for setting.
	 * @param name Name of the settings 
	 * @param secondary If true, the secondary namespace is used.
	 * @return full (prefixed with current namespace) property name for setting
	 */
	private String getFullPropertyName(String name, boolean secondary) {
		String usedNamespace = (secondary ? secondaryNamespace : namespace);
		
		if (usedNamespace != null) {
			return usedNamespace + "." + name;
		}
		else {
			return name;
		}
	}
	
	/**
	 * Returns a String-valued setting. Setting is first looked from the 
	 * namespace that is set (if any) and then from the secondary namespace
	 * (if any). All other getters use this method as their first step too
	 * (so all getters may throw SettingsError and look from both namespaces).
	 * @param name Name of the setting to get
	 * @return The contents of the setting in a String
	 * @throws SettingsError if the setting is not found from either one of 
	 * the namespaces
	 */
	public String getSetting(String name) {
		String fullPropName;
		if (props == null) {
			init(null);
		}
		fullPropName = getFullPropertyName(name, false);
		String value = props.getProperty(fullPropName);
		
		if (value != null) { // found value, check if run setting can be parsed
			value = parseRunSetting(value.trim());
		}
			
		if ((value == null || value.length() == 0) && 
				this.secondaryNamespace != null) {
			// try secondary namespace if the value wasn't found from primary
			fullPropName = getFullPropertyName(name, true);
			value = props.getProperty(fullPropName);
			
			if (value != null) {
				value = parseRunSetting(value.trim());
			}
		}
		
		if (value == null || value.length() == 0) {
			throw new SettingsError("Can't find setting " + 
					getPropertyNamesString(name));
		}
		
		outputSetting(fullPropName + " = " + value);
		return value;
	}
	
	/**
	 * Returns the given setting if it exists, or defaultValue if the setting
	 * does not exist
	 * @param name The name of the setting
	 * @param defaultValue The value to return if the given setting didn't exist
	 * @return The setting value or the default value if the setting didn't
	 * exist
	 */
	public String getSetting(String name, String defaultValue) {
		if (!contains(name)) {
			return defaultValue;
		} else {
			return getSetting(name);
		}
	}
	
	/**
	 * Parses run-specific settings from a String value
	 * @param value The String to parse
	 * @return The runIndex % arrayLength'th value of the run array
	 */
	private static String parseRunSetting(String value) {
		final String RUN_ARRAY_START = "[";
		final String RUN_ARRAY_END = "]";
		final String RUN_ARRAY_DELIM = ";";
		final int MIN_LENGTH = 3; // minimum run is one value. e.g. "[v]"
		
		if (!value.startsWith(RUN_ARRAY_START) || 
			!value.endsWith(RUN_ARRAY_END) || 
			runIndex < 0 ||
			value.length() < MIN_LENGTH) {
			return value; // standard format setting -> return
		}
		
		value = value.substring(1,value.length()-1); // remove brackets
		String[] valueArr = value.split(RUN_ARRAY_DELIM);
		int arrIndex = runIndex % valueArr.length;
		value = valueArr[arrIndex].trim();

		return value;
	}
	
	/**
	 * Returns the setting name appended to namespace name(s) on a String 
	 * (for error messages)
	 * @param name Name of the setting
	 * @return the setting name appended to namespace name(s) on a String 
	 */
	private String getPropertyNamesString(String name) {
		if (this.secondaryNamespace != null) {
			return "'"+ this.secondaryNamespace + "." + name + "' nor '" + 
				this.namespace + "." + name + "'";
		}
		else if (this.namespace != null){
			return "'" + this.namespace + "." + name + "'";
		}
		else {
			return "'" + name + "'";
		}
	}
	
	/**
	 * Returns a double-valued setting
	 * @param name Name of the setting to get
	 * @return Value of the setting as a double
	 */
	public double getDouble(String name) {
		return parseDouble(getSetting(name), name);
	}
	
	/**
	 * Returns a double-valued setting, or the default value if the given
	 * setting does not exist
	 * @param name Name of the setting to get
	 * @param defaultValue The value to return if the setting doesn't exist
	 * @return Value of the setting as a double (or the default value)
	 */
	public double getDouble(String name, double defaultValue) {
		return parseDouble(getSetting(name, ""+defaultValue), name);
	}
	
	/**
	 * Parses a double value from a String valued setting. Supports
	 * kilo (k), mega (M) and giga (G) suffixes.
	 * @param value String value to parse
	 * @param setting The setting where this value was from (for error msgs)
	 * @return The value as a double
	 * @throws SettingsError if the value wasn't a numeric value 
	 * (or the suffix wasn't recognized)
	 */
	private double parseDouble(String value, String setting) {
		double number;
		int multiplier = 1;
		
		if (value.endsWith("k")) {
			multiplier = 1000;
		}
		else if (value.endsWith("M")) {
			multiplier = 1000000;
		}
		else if (value.endsWith("G")) {
			multiplier = 1000000000;
		}
		
		if (multiplier > 1) { // take the suffix away before parsing
			value = value.substring(0,value.length()-1);
		}
		
		try {
			number = Double.parseDouble(value) * multiplier;
		} catch (NumberFormatException e) {
			throw new SettingsError("Invalid numeric setting '" + value + 
					"' for '" + setting +"'\n" + e.getMessage());
		}
		return number;
	}
	
	/**
	 * Returns a CSV setting. Value part of the setting must be a list of 
	 * comma separated values. Whitespace between values is trimmed away.
	 * @param name Name of the setting
	 * @return Array of values that were comma-separated
	 * @throws SettingsError if something went wrong with reading
	 */
	public String[] getCsvSetting(String name) {
		ArrayList<String> values = new ArrayList<String>();
		String csv = getSetting(name);
		Scanner s = new Scanner(csv);
		s.useDelimiter(",");

		while (s.hasNext()) {
			values.add(s.next().trim());
		}
		
		return values.toArray(new String[0]);
	}
	
	/**
	 * Returns a CSV setting containing expected amount of values.
	 * Value part of the setting must be a list of 
	 * comma separated values. Whitespace between values is trimmed away.
	 * @param name Name of the setting
	 * @param expectedCount how many values are expected
	 * @return Array of values that were comma-separated
	 * @throws SettingsError if something went wrong with reading or didn't
	 * read the expected amount of values.
	 */
	public String[] getCsvSetting(String name, int expectedCount) {
		String[] values = getCsvSetting(name);

		if (values.length != expectedCount) {
			throw new SettingsError("Read unexpected amount (" + values.length +
					") of comma separated values for setting '" 
					+ name + "' (expected " + expectedCount + ")");
		}
		
		return values;
	}

	/**
	 * Returns an array of CSV setting double values containing expected
	 * amount of values.
	 * @param name Name of the setting
	 * @param expectedCount how many values are expected
	 * @return Array of values that were comma-separated
	 * @see #getCsvSetting(String, int)
	 */
	public double[] getCsvDoubles(String name, int expectedCount) {
		return parseDoubles(getCsvSetting(name, expectedCount),name);
	}

	/**
	 * Returns an array of CSV setting double values.
	 * @param name Name of the setting
	 * @return Array of values that were comma-separated
	 * @see #getCsvSetting(String)
	 */
	public double[] getCsvDoubles(String name) {
		return parseDoubles(getCsvSetting(name), name);
	}
	
	/**
	 * Parses a double array out of a String array
	 * @param strings The array of strings containing double values
	 * @param name Name of the setting
	 * @return Array of double values parsed from the string values
	 */
	private double[] parseDoubles(String[] strings, String name) {
		double[] values = new double[strings.length];
		for (int i=0; i<values.length; i++) {
			values[i] = parseDouble(strings[i], name);
		}
		return values;		
	}
	
	/**
	 * Returns an array of CSV setting integer values
	 * @param name Name of the setting
	 * @param expectedCount how many values are expected
	 * @return Array of values that were comma-separated
	 * @see #getCsvSetting(String, int)
	 */
	public int[] getCsvInts(String name, int expectedCount) {
		return convertToInts(getCsvDoubles(name, expectedCount), name);
	}
	
	/**
	 * Returns an array of CSV setting integer values
	 * @param name Name of the setting
	 * @return Array of values that were comma-separated
	 * @see #getCsvSetting(String, int)
	 */
	public int[] getCsvInts(String name) {
		return convertToInts(getCsvDoubles(name), name);	
	}
	
	/**
	 * Returns comma-separated ranges (e.g., "3-5, 17-20, 15")
	 * @param name Name of the setting
	 * @return Array of ranges that were comma-separated in the setting
	 * @throws SettingsError if something went wrong with reading
	 */
	public Range[] getCsvRanges(String name) {
		String[] strRanges = getCsvSetting(name);
		Range[] ranges = new Range[strRanges.length];
		
		try {
			for (int i=0; i < strRanges.length; i++) {
				ranges[i] = new Range(strRanges[i]);
			}
		} catch (NumberFormatException nfe) {
			throw new SettingsError("Invalid numeric range value in " + 
					name, nfe);
		}
		
		return ranges;
	}
	
	/**
	 * Returns an integer-valued setting 
	 * @param name Name of the setting to get
	 * @return Value of the setting as an integer
	 */
	public int getInt(String name) {
		return convertToInt(getDouble(name), name);
	}
	
	/**
	 * Returns an integer-valued setting, or the default value if the
	 * setting does not exist
	 * @param name Name of the setting to get
	 * @param defaultValue The value to return if the setting didn't exist
	 * @return Value of the setting as an integer
	 */
	public int getInt(String name, int defaultValue) {
		return convertToInt(getDouble(name, defaultValue), name);
	}
	
	/**
	 * Converts a double value that is supposedly equal to an integer value 
	 * to an integer value.
	 * @param doubleValue The double value to convert
	 * @param name Name of the setting where this value is from (for 
	 * SettingsError)
	 * @return The integer value
	 * @throws SettingsError if the double value was not equal to any integer
	 * value 
	 */
	private int convertToInt(double doubleValue, String name) {
		int number = (int)doubleValue;
		
		if (number != doubleValue) {
			throw new SettingsError("Expected integer value for setting '" +
					name + "' " + " got '" + doubleValue + "'");
		}
		return number;		
	}
	
	/**
	 * Converts an array of double values to int values using 
	 * {@link #convertToInt(double, String)}.
	 * @param doubleValues The double valued array
	 * @param name Name of the setting where this value is from (for 
	 * SettingsError)	 
	 * @return Array of integer values
	 * @see #convertToInt(double, String)
	 */
	private int[] convertToInts(double [] doubleValues, String name) {
		int[] values = new int[doubleValues.length];
		for (int i=0; i<values.length; i++) {
			values[i] = convertToInt(doubleValues[i], name);
		}
		return values;	
	}

	/**
	 * Returns a boolean-valued setting
	 * @param name Name of the setting to get 
	 * @return True if the settings value was either "true" (case ignored)
	 *  or "1", false is the settings value was either "false" (case ignored)
	 *  or "0".
	 *  @throws SettingsError if the value wasn't any recognized value
	 *  @see #getSetting(String)
	 */
	public boolean getBoolean(String name) {
		String stringValue = getSetting(name);
		boolean value;
		
		if (stringValue.equalsIgnoreCase("true") || 
				stringValue.equals("1")) {
			value = true;
		}
		else if (stringValue.equalsIgnoreCase("false") || 
				stringValue.equals("0")) {
			value = false;
		}
		else {
			throw new SettingsError("Not a boolean value: '"+stringValue+
					"' for setting " + name);
		}
		
		return value;
	}
	
	/**
	 * Returns the given boolean setting if it exists, or defaultValue if the 
	 * setting does not exist
	 * @param name The name of the setting
	 * @param defaultValue The value to return if the given setting didn't exist
	 * @return The setting value or the default value if the setting didn't
	 * exist
	 */
	public boolean getBoolean(String name, boolean defaultValue) {
		if (!contains(name)) {
			return defaultValue;
		} else {
			return getBoolean(name);
		}
	}
	
	/**
	 * Returns an ArithmeticCondition setting. See {@link ArithmeticCondition}
	 * @param name Name of the setting to get
	 * @return ArithmeticCondition from the setting
 	 * @throws SettingsError if the value wasn't a valid arithmetic expression
	 */
	public ArithmeticCondition getCondition(String name) {
		return new ArithmeticCondition(getSetting(name));
	}

	/**
	 * Creates (and dynamically loads the class of) an object that
	 * initializes itself using the settings in this Settings object
	 * (given as the only parameter to the constructor). 
	 * @param className Name of the class of the object
	 * @return Initialized object
	 * @throws SettingsError if object couldn't be created
	 */
	public Object createIntializedObject(String className) {
		Class<?>[] argsClass = {Settings.class};
		Object[] args = {this};

		return loadObject(className, argsClass, args);
	}
	
	/**
	 * Creates (and dynamically loads the class of) an object using the
	 * constructor without any parameters.
	 * @param className Name of the class of the object
	 * @return Initialized object
	 * @throws SettingsError if object couldn't be created
	 */
	public Object createObject(String className) {
		return loadObject(className, null, null);
	}
	
	/**
	 * Dynamically loads and creates an object.
	 * @param className Name of the class of the object
	 * @param argsClass Class(es) of the argument(s) or null if no-argument
	 * constructor should be called
	 * @param args Argument(s)
	 * @return The new object
	 * @throws SettingsError if object couldn't be created
	 */
	private Object loadObject(String className, Class<?>[] argsClass, 
			Object[] args) {
		Object o = null;
		Class<?> objClass = getClass(className);
		Constructor<?> constructor;
		
		try {
			if (argsClass != null) { // use a specific constructor
				constructor = objClass.getConstructor((Class[])argsClass);
				o = constructor.newInstance(args);
			}
			else { // call empty constructor
				o = objClass.newInstance();
			}
		} catch (SecurityException e) {
			e.printStackTrace();
			throw new SettingsError("Fatal exception " + e, e);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new SettingsError("Fatal exception " + e, e);
		} catch (NoSuchMethodException e) {
			throw new SettingsError("Class '" + className + 
					"' doesn't have a suitable constructor", e);
		} catch (InstantiationException e) {
			throw new SettingsError("Can't create an instance of '" + 
					className + "'", e);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			throw new SettingsError("Fatal exception " + e, e);
		} catch (InvocationTargetException e) {
			// this exception occurs if initialization of the object fails
			if (e.getCause() instanceof SettingsError) {
				throw (SettingsError)e.getCause();
			}
			else {
				e.printStackTrace(); 
				throw new SimError("Couldn't create settings-accepting object"+
					" for '" + className + "'\n" + "cause: " + e.getCause(), e);
			}
		}

		return o;
	}
	
	/**
	 * Returns a Class object for the name of class of throws SettingsError
	 * if such class wasn't found. 
	 * @param name Full name of the class (including package name) 
	 * @return A Class object of that class
	 * @throws SettingsError if such class wasn't found or couldn't be loaded 
	 */
	private Class<?> getClass(String name) {
		String className = name;
		Class<?> c;
		
		try {
			c = Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new SettingsError("Couldn't find class '" + className + "'"+ 
					"\n" + e.getMessage(),e);
		}
		
		return c;
	}
	
	/**
	 * Fills a String formatted in a special way with values from Settings.
	 * String can contain (fully qualified) setting names surrounded by 
	 * delimiters (see {@link #FILL_DELIMITER}). Values for those settings
	 * are retrieved and filled in the place of place holders.
	 * @param input The input string that may contain value requests
	 * @return A string filled with requested values (or the original string
	 * if no requests were found)
	 * @throws SettingsError if such settings were not found
	 */
	public String valueFillString(String input) {
		if (!input.contains(FILL_DELIMITER)) {
			return input;	// nothing to fill
		}

		Settings s = new Settings(); // don't use any namespace
		String result = "";
		Scanner scan = new Scanner(input);
		scan.useDelimiter(FILL_DELIMITER);
		
		if (input.startsWith(FILL_DELIMITER)) {
			result += s.getSetting(scan.next());
		}
		
		while(scan.hasNext()) {
			result += scan.next();
			if (!scan.hasNext()) {
				break;
			}
			result += s.getSetting(scan.next());
		}
		
		return result;
	}
	
	/**
	 * Returns a String representation of the stored settings
	 * @return a String representation of the stored settings 
	 */
	public String toString() {
		return props.toString();
	}
	
}