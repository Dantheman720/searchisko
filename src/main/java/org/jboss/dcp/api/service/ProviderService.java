/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 */
package org.jboss.dcp.api.service;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.settings.SettingsException;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilders;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessor;
import org.jboss.elasticsearch.tools.content.StructuredContentPreprocessorFactory;

/**
 * Service related to Content Provider.
 * 
 * @author Libor Krzyzanek
 * @author Vlastimil Elias (velias at redhat dot com)
 * 
 */
@Named
@ApplicationScoped
public class ProviderService {

	/** Key for provider's name which is unique identifier - can be same as provdier's ID **/
	public static final String NAME = "name";

	/** Key for password hash **/
	public static final String PASSWORD_HASH = "pwd_hash";

	/** Key for flag is provider is super provider **/
	public static final String SUPER_PROVIDER = "super_provider";

	/** Key for content type **/
	public static final String TYPE = "type";
	/** Key for index settings **/
	public static final String INDEX = "index";
	/** Key for dcp_type setting **/
	public static final String DCP_TYPE = "dcp_type";

	@Inject
	protected Logger log;

	@Inject
	@Named("providerServiceBackend")
	protected ElasticsearchEntityService entityService;

	@Inject
	protected SecurityService securityService;

	@Inject
	protected SearchClientService searchClientService;

	/**
	 * Check if password matches for given provider.
	 * 
	 * @param providerName name of provider
	 * @param password password to check
	 * @return true if provider name and password matches so it's authenticated
	 */
	public boolean authenticate(String providerName, String password) {
		if (providerName == null || password == null) {
			return false;
		}

		Map<String, Object> providerData = findProvider(providerName);
		if (providerData == null) {
			return false;
		}
		Object hash = providerData.get(PASSWORD_HASH);
		if (hash == null) {
			log.log(Level.SEVERE, "Provider {0} doesn't have any password hash defined.", providerName);
			return false;
		} else {
			return securityService.checkPwdHash(providerName, password, hash.toString());
		}
	}

	/**
	 * Check if requested provider is superprovider.
	 * 
	 * @param providerName name of provider to check
	 * @return true if it is superprovider
	 */
	public boolean isSuperProvider(String providerName) {
		Map<String, Object> providerData = findProvider(providerName);

		Object superProviderFlag = providerData.get(SUPER_PROVIDER);

		if (superProviderFlag != null && ((Boolean) superProviderFlag).booleanValue()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Find provider based on its name (called <code>dcp_content_provider</code>).
	 * 
	 * @param providerName of provider - DCP wide unique
	 * @return provider configuration
	 */
	public Map<String, Object> findProvider(String providerName) {
		SearchRequestBuilder searchBuilder = entityService.createSearchRequestBuilder();
		searchBuilder.setFilter(FilterBuilders.queryFilter(QueryBuilders.matchQuery("name", providerName)));
		searchBuilder.setQuery(QueryBuilders.matchAllQuery());

		SearchResponse response = entityService.search(searchBuilder);
		if (response.getHits().getTotalHits() == 1) {
			return response.getHits().getHits()[0].getSource();
		} else if (response.getHits().getTotalHits() == 0) {
			return null;
		} else {
			throw new SettingsException("More than one configurations found for content provider name '" + providerName
					+ "'. Contact administrators please.");
		}
	}

	/**
	 * Find 'provider content type' configuration based on its identifier (called <code>dcp_content_type</code>). Each
	 * 'provider content type' identifier have to be DCP wide unique!
	 * 
	 * @param typeId <code>dcp_content_type</code> to look for
	 * @return content type configuration structure
	 */
	public Map<String, Object> findContentType(String typeId) {
		SearchRequestBuilder searchBuilder = entityService.createSearchRequestBuilder();
		searchBuilder.setFilter(FilterBuilders.existsFilter("type." + typeId + ".dcp_type"));
		searchBuilder.setQuery(QueryBuilders.matchAllQuery());
		searchBuilder.addField("type." + typeId);

		SearchResponse response = entityService.search(searchBuilder);
		if (response.getHits().getTotalHits() == 1) {
			return response.getHits().getHits()[0].getFields().get("type." + typeId).getValue();
		} else if (response.getHits().getTotalHits() == 0) {
			return null;
		} else {
			throw new SettingsException("More than one configurations found for dcp_content_type=" + typeId
					+ ". Contact administrators please.");
		}
	}

	/**
	 * Run defined content preprocessors on passed in content.
	 * 
	 * @param preprocessorsDef definition of preprocessors - see {@link #getPreprocessors(Map)}
	 * @param content to run preprocessors on
	 */
	public void runPreprocessors(List<Map<String, Object>> preprocessorsDef, Map<String, Object> content) {
		List<StructuredContentPreprocessor> preprocessors = StructuredContentPreprocessorFactory.createPreprocessors(
				preprocessorsDef, searchClientService.getClient());
		for (StructuredContentPreprocessor preprocessor : preprocessors) {
			content = preprocessor.preprocessData(content);
		}
	}

	/**
	 * Generate DCP wide unique <code>dcp_id</code> value from <code>dcp_content_type</code> and
	 * <code>dcp_content_id</code>.
	 * 
	 * @param type <code>dcp_content_type</code> value which is DCP wide unique
	 * @param contentId <code>dcp_content_id</code> value which is unique for <code>dcp_content_type</code>
	 * @return DCP wide unique <code>dcp_id</code> value
	 */
	public String generateDcpId(String type, String contentId) {
		return type + "-" + contentId;
	}

	/**
	 * Get configuration for one <code>dcp_content_type</code> from provider configuration.
	 * 
	 * @param providerDef provider configuration structure
	 * @param type name of <code>dcp_content_type</code> to get configuration for
	 * @return type configuration or null if doesn't exist
	 * @throws SettingsException for incorrect configuration structure
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> getContentType(Map<String, Object> providerDef, String type) {
		try {
			Map<String, Object> types = (Map<String, Object>) providerDef.get(TYPE);
			if (types != null) {
				if (types.containsKey(type)) {
					return (Map<String, Object>) types.get(type);
				}
			}
			return null;
		} catch (ClassCastException e) {
			throw new SettingsException("Incorrect configuration for provider '" + providerDef.get(NAME)
					+ "' when trying to find type '" + type + "'. Contact administrators please.");
		}
	}

	/**
	 * Get preprocessors configuration from one <code>dcp_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @return list of preprocessor configurations
	 */
	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> getPreprocessors(Map<String, Object> typeDef) {
		return (List<Map<String, Object>>) typeDef.get("input_preprocessors");
	}

	/**
	 * Get search subsystem index name from one <code>dcp_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @return search index name
	 */
	@SuppressWarnings("unchecked")
	public static String getIndexName(Map<String, Object> typeDef) {
		if (typeDef.get(INDEX) != null)
			return ((Map<String, Object>) typeDef.get(INDEX)).get("name").toString();
		else
			return null;
	}

	/**
	 * Get search subsystem type name from one <code>dcp_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @return search type name
	 */
	@SuppressWarnings("unchecked")
	public static String getIndexType(Map<String, Object> typeDef) {
		if (typeDef.get(INDEX) != null)
			return ((Map<String, Object>) typeDef.get(INDEX)).get("type").toString();
		else
			return null;
	}

	/**
	 * Get <code>dcp_type</code> value from one <code>dcp_content_type</code> configuration structure.
	 * 
	 * @param typeDef <code>dcp_content_type</code> configuration structure
	 * @return <code>dcp_type</code> value
	 * @throws SettingsException if value is not present in configuration or is invalid
	 */
	public static String getDcpType(Map<String, Object> typeDef, String type) {
		String ret = null;
		if (typeDef.get(DCP_TYPE) != null)
			ret = typeDef.get(DCP_TYPE).toString();

		if (ret == null || ret.trim().isEmpty())
			throw new SettingsException("dcp_type is not defined correctly for dcp_provider_type=" + type
					+ ". Contact administrators please.");

		return ret;
	}

	public EntityService getEntityService() {
		return entityService;
	}

}
