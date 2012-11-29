/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.util;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.dcp.api.model.QuerySettings;
import org.jboss.dcp.api.model.QuerySettings.SortByValue;

/**
 * Query settings parser
 * 
 * @author Libor Krzyzanek
 * @author Lukas Vlcek
 */
public class QuerySettingsParser {

	public static enum MonthIntervalNames {
		DAY("day"), WEEK("1w"), // variable can not start with number, hence WEEK instead of 1W
		MONTH("month");

		private String value;

		private MonthIntervalNames(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}
	}

	public static enum PastIntervalNames {
		WEEK("week"), MONTH("month"), QUARTER("quarter"), YEAR("year");

		private String value;

		private PastIntervalNames(String value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return this.value;
		}
	}

	private final static Logger log = Logger.getLogger(QuerySettingsParser.class.getName());

	public static void sanityQuery(QuerySettings settings) throws Exception {
		if (settings == null) {
			throw new Exception("No query settings provided!");
		}
		if (settings.getQuery() != null) {
			settings.setQuery(settings.getQuery().trim());
			settings.setQuery(settings.getQuery().replaceAll("\\*\\?", "*"));
			settings.setQuery(settings.getQuery().replaceAll("\\?\\*", "*"));
			settings.setQuery(settings.getQuery().replaceAll("\\*+", "*"));
			settings.setQuery(settings.getQuery().replaceAll("\\?+", "?"));
		} else {
			settings.setQuery("match_all:{}");
		}
	}

	/**
	 * Parse REST parameters to standardized query settings
	 * 
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public static QuerySettings parseUriParams(MultivaluedMap<String, String> params) throws IllegalArgumentException {
		QuerySettings settings = new QuerySettings();
		if (params == null) {
			return settings;
		}

		settings.setContentType(params.getFirst("type"));
		QuerySettings.Filters filters = new QuerySettings.Filters();

		if (params.containsKey(QuerySettings.Filters.START_KEY)) {
			try {
				filters.setStart(new Integer(params.getFirst(QuerySettings.Filters.START_KEY)));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Parameter " + QuerySettings.Filters.START_KEY + " has bad value");
			}
		}

		if (params.containsKey(QuerySettings.Filters.COUNT_KEY)) {
			try {
				filters.setStart(new Integer(params.getFirst(QuerySettings.Filters.COUNT_KEY)));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Parameter " + QuerySettings.Filters.COUNT_KEY + " has bad value");
			}
		}

		if (params.containsKey(QuerySettings.SORT_BY_KEY)) {
			String sortByString = params.getFirst(QuerySettings.SORT_BY_KEY);
			if (QuerySettings.SortByValue.NEW.name().equalsIgnoreCase(sortByString)) {
				settings.setSortBy(SortByValue.NEW);
			} else if (QuerySettings.SortByValue.OLD.name().equalsIgnoreCase(sortByString)) {
				settings.setSortBy(SortByValue.OLD);
			} else {
				throw new IllegalArgumentException("Parameter " + QuerySettings.SORT_BY_KEY + " has bad value");
			}
		}

		settings.setFilters(filters);

		return settings;
	}

	/**
	 * Parse HTTP request URL parameters into query settings
	 * 
	 * @param parameterMap
	 * @return
	 * @throws Exception
	 */
	public static QuerySettings parseSettings(Map<String, String[]> parameterMap) throws Exception {

		if (parameterMap == null) {
			throw new Exception("no parameters found!");
		}

		// TODO: Rewrite implementation of parseSettings to new REST based parseUriParams method

		QuerySettings settings = new QuerySettings();
		Set<String> keys = parameterMap.keySet();

		// log.info("In parseSettings...");
		// for (String key : parameterMap.keySet()) {
		// log.info("key: {}, value: {}",key, parameterMap.get(key));
		// }

		if (keys.contains("count")) {
			boolean value = false;
			try {
				value = Boolean.parseBoolean(parameterMap.get("count")[0]);
			} catch (Throwable e) {
				log.log(Level.FINE, "Error parsing value of count param {0}", parameterMap.get("count")[0]);
			}
			settings.setCount(value);
		}

		String value = MonthIntervalNames.MONTH.toString();
		if (keys.contains("filters[interval]")) {
			try {
				value = parameterMap.get("filters[interval]")[0];
				if ("1w".equalsIgnoreCase(value)) {
					value = "week";
				}
				value = value.toUpperCase();
				value = MonthIntervalNames.valueOf(value).toString();
			} catch (Throwable e) {
				log.log(Level.FINE, "Error parsing value of filters[interval] param '{0}', using default value",
						parameterMap.get("filters[interval]")[0]);
				value = MonthIntervalNames.MONTH.toString();
			}
		}
		settings.setInterval(value);

		String q = "";
		if (keys.contains("query")) {
			q = parameterMap.get("query")[0];
		}
		settings.setQuery(q);

		// if (keys.contains("filters[start]")) {
		// ensureFilters(settings).setStart(new Integer(parameterMap.get("filters[start]")[0]));
		// }

		// if (keys.contains("sortBy")) {
		// ensureFilters(settings).setSortBy(new Integer(parameterMap.get("filters[start]")[0]));
		// settings.setSortBy(SortByValue.valueOf(params.getFirst(QuerySettings.SORT_BY_KEY)));
		// }

		if (keys.contains("filters[author][]")) {
			ensureFilters(settings).setAuthor(parameterMap.get("filters[author][]"));
		}

		if (keys.contains("filters[project][]")) {
			ensureFilters(settings).setProject(parameterMap.get("filters[project][]"));
		}

		if (keys.contains("filters[mailList][]")) {
			ensureFilters(settings).setMailList(parameterMap.get("filters[mailList][]"));
		}

		if (keys.contains("filters[from]")) {
			ensureFilters(settings).setFrom(parameterMap.get("filters[from]")[0]);
		}

		if (keys.contains("filters[to]")) {
			ensureFilters(settings).setTo(parameterMap.get("filters[to]")[0]);
		}

		if (keys.contains("filters[past]")) {
			ensureFilters(settings).setPast(parameterMap.get("filters[past]")[0]);
		}

		return settings;

	}

	private static QuerySettings.Filters ensureFilters(QuerySettings settings) {
		if (settings.getFilters() == null) {
			settings.setFilters(new QuerySettings.Filters());
		}
		return settings.getFilters();
	}
}
