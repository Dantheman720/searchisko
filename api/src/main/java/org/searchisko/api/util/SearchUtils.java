/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.searchisko.api.util;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.elasticsearch.common.joda.time.format.ISODateTimeFormat;

/**
 * Distinct utility methods.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class SearchUtils {

	/**
	 * Load properties from defined path e.g. "/app.properties"
	 * 
	 * @param path
	 * @return newly initialized {@link Properties}
	 * @throws IOException
	 * @see {@link Class#getResourceAsStream(String)}
	 */
	public static Properties loadProperties(String path) throws IOException {
		Properties prop = new Properties();
		InputStream inStream = SearchUtils.class.getResourceAsStream(path);
		prop.load(inStream);
		inStream.close();

		return prop;
	}

	/**
	 * Trim string and return null if empty.
	 * 
	 * @param value to trim
	 * @return trimmed value or null if empty
	 */
	public static String trimToNull(String value) {
		if (value != null) {
			value = value.trim();
			if (value.isEmpty())
				value = null;
		}
		return value;
	}

	/**
	 * Check if String is blank.
	 * 
	 * @param value to check
	 * @return true if value is blank (so null or empty or whitespaces only string)
	 */
	public static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

	/**
	 * Convert JSON Map structure into String with JSON content.
	 * 
	 * @param jsonMapValue to convert
	 * @return
	 * @throws IOException
	 */
	public static String convertJsonMapToString(Map<String, Object> jsonMapValue) throws IOException {
		if (jsonMapValue == null)
			return "";
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDateFormat(getISODateFormat());
		return mapper.writeValueAsString(jsonMapValue);
	}

	/**
	 * Get ISO date time formatter.
	 * 
	 * @return DateFormat instance for ISO format
	 */
	public static DateFormat getISODateFormat() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		sdf.setLenient(false);
		return sdf;
	}

	/**
	 * Parse ISO date time formatted string into {@link Date} instance.
	 * 
	 * @param string ISO formatted date string to parse
	 * @param silent if true then null is returned instead of {@link IllegalArgumentException} thrown
	 * @return parsed date or null
	 * @throws IllegalArgumentException in case of bad format
	 */
	public static Date dateFromISOString(String string, boolean silent) throws IllegalArgumentException {
		if (string == null)
			return null;
		try {
			return ISODateTimeFormat.dateTimeParser().parseDateTime(string).toDate();
		} catch (IllegalArgumentException e) {
			if (!silent)
				throw e;
			else
				return null;
		}
	}

	/**
	 * Convert String with JSON content into JSON Map structure.
	 * 
	 * @param jsonData string to convert
	 * @return JSON MAP structure
	 * @throws IOException
	 */
	public static Map<String, Object> convertToJsonMap(String jsonData) throws IOException {
		if (jsonData == null)
			return null;
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(jsonData, new TypeReference<Map<String, Object>>() {
		});
	}

	/**
	 * Get Integer value from value in Json Map. Can convert from {@link String} and {@link Number} values.
	 * 
	 * @param map to get Integer value from
	 * @param key in map to get value from
	 * @return Integer value if found. <code>null</code> if value is not present in map.
	 * @throws NumberFormatException if valu from map is not convertible to integer number
	 */
	public static Integer getIntegerFromJsonMap(Map<String, Object> map, String key) throws NumberFormatException {
		if (map == null)
			return null;
		Object o = map.get(key);
		if (o == null)
			return null;
		if (o instanceof Integer)
			return (Integer) o;
		else if (o instanceof Number)
			return ((Number) o).intValue();
		else if (o instanceof String)
			return Integer.valueOf((String) o);
		else
			throw new NumberFormatException();
	}

	/**
	 * Extract contributor name from contributor id string. So extracts 'John Doe' from '
	 * <code>John Doe <john@doe.org></code>'.
	 * 
	 * @param contributor id to extract name from
	 * @return contributor name
	 */
	public static String extractContributorName(String contributor) {
		if (contributor == null)
			return null;
		int i = contributor.lastIndexOf("<");
		int i2 = contributor.lastIndexOf(">");
		if (i > -1 && i2 > -1 && i < i2) {
			return trimToNull(contributor.substring(0, i));
		}
		return trimToNull(contributor);
	}

}
