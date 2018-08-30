package org.sunbird.taxonomy.mgr.impl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.Platform;
import org.sunbird.common.dto.Response;
import org.sunbird.common.exception.ClientException;
import org.sunbird.common.exception.ServerException;
import org.sunbird.common.mgr.BaseManager;
import org.sunbird.common.router.RequestRouterPool;
import org.sunbird.graph.common.Identifier;
import org.sunbird.graph.dac.enums.GraphDACParams;
import org.sunbird.graph.dac.model.Node;
import org.sunbird.learning.util.ControllerUtil;
import org.sunbird.searchindex.dto.SearchDTO;
import org.sunbird.searchindex.elasticsearch.ElasticSearchUtil;
import org.sunbird.searchindex.processor.SearchProcessor;
import org.sunbird.searchindex.util.CompositeSearchConstants;
import org.sunbird.taxonomy.enums.SuggestionCodeConstants;
import org.sunbird.taxonomy.enums.SuggestionConstants;
import org.sunbird.taxonomy.mgr.IContentManager;
import org.sunbird.taxonomy.mgr.ISuggestionManager;
import org.sunbird.telemetry.logger.TelemetryManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import scala.concurrent.Await;

/**
 * The Class SuggestionManager provides implementations of the various
 * operations defined in the ISuggestionManager
 * 
 * @author Rashmi N, Mahesh Kumar Gangula
 * 
 * @see ISuggestionManager
 */
@Component
public class SuggestionManagerImpl extends BaseManager implements ISuggestionManager {

	@Autowired
	private IContentManager contentManager;
	
	/** The ControllerUtil */
	private ControllerUtil util = new ControllerUtil();

	/** The Object Mapper */
	private static ObjectMapper mapper = new ObjectMapper();

	/** The SearchProcessor */
	private SearchProcessor processor = null;

	@PostConstruct
	public void init() {
		ElasticSearchUtil.initialiseESClient(SuggestionConstants.SUGGESTION_INDEX,
				Platform.config.getString("search.es_conn_info"));
		processor = new SearchProcessor(SuggestionConstants.SUGGESTION_INDEX);

	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see ISuggestionManager
	 * #saveSuggestion(java.util.Map)
	 */
	@Override
	public Response saveSuggestion(Map<String, Object> request) {
		Response response = new Response();
		try {
			TelemetryManager.log("Fetching identifier from request");
			String identifier = (String) request.get(SuggestionCodeConstants.objectId.name());
			Node node = util.getNode(SuggestionConstants.GRAPH_ID, identifier);
			if (null != node) {
				TelemetryManager.log("saving the suggestions to elastic search index" + identifier);
				response = saveSuggestionToEs(request);
				if (checkError(response)) {
					TelemetryManager.log("Erroneous Response.");
					return response;
				}
			} else {
				throw new ClientException(SuggestionCodeConstants.INVALID_OBJECT_ID.name(),
						"Content_Id doesnt exists | Invalid Content_id");
			}
		} catch (ClientException e) {
			TelemetryManager.error("Error occured while processing request | Not a valid request"+ e.getMessage(),e);
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			throw new ClientException(SuggestionCodeConstants.INVALID_REQUEST.name(), "Error! Invalid Request");
		}
		TelemetryManager.log("Returning response from saveSuggestion" + response.getResponseCode());
		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ISuggestionManager
	 * #readSuggestion(java.lang.String,java.util.date)
	 */
	@Override
	public Response readSuggestion(String objectId, String startTime, String endTime, String status) {
		Response response = new Response();
		try {
			TelemetryManager.log("Checking if received parameters are empty or not " + objectId);
			List<Object> result = getSuggestionByObjectId(objectId, startTime, endTime, status);
			response.setParams(getSucessStatus());
			response.put(SuggestionCodeConstants.suggestions.name(), result);
			TelemetryManager.log("Fetching response from elastic search" + result.size());
			if (checkError(response)) {
				TelemetryManager.log("Erroneous Response.");
				return response;
			}
		} catch (Exception e) {
			TelemetryManager.error("Exception occured while fetching suggestions for contentId: " + e.getMessage(), e);
			throw e;
		}
		TelemetryManager.log("Response received from the readSuggestion: " + response.getResponseCode());
		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ISuggestionManager
	 * #approveSuggestion(java.lang.String,java.util.Map)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Response approveSuggestion(String suggestion_id, Map<String, Object> map) {
		Response response = new Response();
		try {
			
			String suggestionResponse = ElasticSearchUtil.getDocumentAsStringById(SuggestionConstants.SUGGESTION_INDEX,
					SuggestionConstants.SUGGESTION_INDEX_TYPE, suggestion_id);
			TelemetryManager.log("Result of suggestion using Id: " + suggestionResponse);
			Map<String, Object> suggestionObject = mapper.readValue(suggestionResponse, Map.class);
			Map<String, Object> paramsMap = (Map) suggestionObject.get(SuggestionCodeConstants.params.name());
			String contentId = (String) suggestionObject.get(SuggestionCodeConstants.objectId.name());
			String currentStatus = (String) suggestionObject.get(SuggestionCodeConstants.status.name());
            String suggestedBy = (String) suggestionObject.get(SuggestionCodeConstants.suggestedBy.name());
			
            if(SuggestionConstants.APPROVE_STATUS.equalsIgnoreCase(currentStatus) || SuggestionConstants.REJECT_STATUS.equalsIgnoreCase(currentStatus))
				throw new ClientException(SuggestionCodeConstants.INVALID_ACTION.name(), "Suggestion already "+ currentStatus);
			
			Map<String, Object> requestMap = dataToUpdate(map, SuggestionConstants.APPROVE_STATUS);
			String requestString = mapper.writeValueAsString(requestMap);
			TelemetryManager.log("request for suggestion approval: " + requestString);
			ElasticSearchUtil.updateDocument(SuggestionConstants.SUGGESTION_INDEX,
					SuggestionConstants.SUGGESTION_INDEX_TYPE, requestString, suggestion_id);
			response.setParams(getSucessStatus());
			response.put(SuggestionCodeConstants.suggestion_id.name(), suggestion_id);
			response.put(SuggestionCodeConstants.message.name(),
					"suggestion accepted successfully! Content Update started successfully");
			if (checkError(response)) {
				return response;
			}
			if (null != contentId && null != paramsMap){
				if(StringUtils.isNotBlank(suggestedBy))
					paramsMap.put(SuggestionCodeConstants.lastUpdatedBy.name(), suggestedBy);
				contentManager.updateAllContents(contentId, paramsMap);
			}
			
		} catch (ClientException e) {
			TelemetryManager.error("throwing exception received: " + e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			TelemetryManager.error("Server Exception occured while processing request: " + e.getMessage(), e);
			throw new ServerException(SuggestionCodeConstants.SERVER_ERROR.name(),
					"Error! Something went wrong while processing", e);
		}
		TelemetryManager.log("Returning response from approve suggestion" + response.getResponseCode());
		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ISuggestionManager
	 * #rejectSuggestion(java.lang.String, java.util.Map)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Response rejectSuggestion(String suggestion_id, Map<String, Object> map) {
		Response response = new Response();
		try {
			String suggestionResponse = ElasticSearchUtil.getDocumentAsStringById(SuggestionConstants.SUGGESTION_INDEX,
					SuggestionConstants.SUGGESTION_INDEX_TYPE, suggestion_id);
			TelemetryManager.log("Result of suggestion using Id: " + suggestionResponse);
			Map<String, Object> suggestionObject = mapper.readValue(suggestionResponse, Map.class);
			String currentStatus = (String) suggestionObject.get(SuggestionCodeConstants.status.name());
			
			if(SuggestionConstants.APPROVE_STATUS.equalsIgnoreCase(currentStatus) || SuggestionConstants.REJECT_STATUS.equalsIgnoreCase(currentStatus))
				throw new ClientException(SuggestionCodeConstants.INVALID_ACTION.name(), "Suggestion already "+ currentStatus);
			
			Map<String, Object> requestMap = dataToUpdate(map, SuggestionConstants.REJECT_STATUS);
			String results = mapper.writeValueAsString(requestMap);
			ElasticSearchUtil.updateDocument(SuggestionConstants.SUGGESTION_INDEX,
					SuggestionConstants.SUGGESTION_INDEX_TYPE, results, suggestion_id);
			response.setParams(getSucessStatus());
			response.put("suggestion_id", suggestion_id);
			response.put("message", "suggestion rejected successfully");
			if (checkError(response)) {
				TelemetryManager.log("Erroneous Response.");
				return response;
			}
		} catch (ClientException e) {
			TelemetryManager.error("throwing exception received: " + e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			e.printStackTrace();
			TelemetryManager.error("Server Exception occured while processing request: " + e.getMessage(), e);
			throw new ServerException(SuggestionCodeConstants.SERVER_ERROR.name(),
					"Error! Something went wrong while processing", e);
		}

		TelemetryManager.log("Returning response from rejectSuggestion" + response.getResponseCode());
		return response;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see ISuggestionManager
	 * #listSuggestion(java.util.Map)
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Response listSuggestion(Map<String, Object> map) {
		Response response = new Response();
		try {
			String status = null;
			String suggestedBy = null;
			String suggestionId = null;
			Map<String, Object> requestMap = (Map) map.get(SuggestionCodeConstants.request.name());
			Map<String, Object> contentReq = (Map) requestMap.get(SuggestionCodeConstants.content.name());
			TelemetryManager.log("Fetching fields to be retrived from request" + contentReq);
			if (contentReq.containsKey(SuggestionCodeConstants.status.name()))
				status = (String) contentReq.get(SuggestionCodeConstants.status.name());
			if (contentReq.containsKey(SuggestionCodeConstants.suggestedBy.name()))
				suggestedBy = (String) contentReq.get(SuggestionCodeConstants.suggestedBy.name());
			if (contentReq.containsKey(SuggestionCodeConstants.suggestion_id.name()))
				suggestionId = (String) contentReq.get(SuggestionCodeConstants.suggestion_id.name());

			TelemetryManager.log("calling getSuggestion method to get suggestions based on search criteria" + status
					+ suggestedBy + suggestionId);
			List<Object> list = getSuggestionsList(status, suggestedBy, suggestionId);

			TelemetryManager.log("Result from suggestion list API" + list);
			response.setParams(getSucessStatus());
			response.put("suggestions", list);
			if (checkError(response)) {
				TelemetryManager.log("Erroneous Response.");
				return response;
			}
		} catch (ClientException e) {
			TelemetryManager.error("throwing exception received: " + e.getMessage(), e);
			throw e;
		} catch (Exception e) {
			TelemetryManager.error("Server Exception occured while processing request: " + e.getMessage(), e);
			throw new ServerException(SuggestionCodeConstants.SERVER_ERROR.name(),
					"Error! Something went wrong while processing", e);
		}
		TelemetryManager.log("Returning response from list suggestion" + response.getResponseCode());
		return response;
	}

	/**
	 * This methods holds logic to save suggestion to elastic search index
	 * 
	 * @param entity_map
	 * 
	 * @return
	 * 
	 * @throws IOException
	 */
	private Response saveSuggestionToEs(Map<String, Object> entity_map) throws IOException {
		Response response = new Response();
		TelemetryManager.log("creating suggestion index in elastic search" + SuggestionConstants.SUGGESTION_INDEX);
		createIndex();
		TelemetryManager.log("Adding document to suggestion index" , entity_map);
		String identifier = addDocument(entity_map);

		TelemetryManager.log("Checking if suggestion_if is returned from response" + identifier);
		if (StringUtils.isNotBlank(identifier)) {
			response = setResponse(response, identifier);
			TelemetryManager.log("returning response from save suggestion" , response.getResult());
			return response;
		}
		return null;
	}

	/**
	 * This methods holds logic to create index for suggestions in elastic
	 * search
	 * 
	 * @return
	 * 
	 * @throws IOException
	 */
	private static void createIndex() throws IOException {
		String settings = "{\"analysis\": {       \"analyzer\": {         \"sg_index_analyzer\": {           \"type\": \"custom\",           \"tokenizer\": \"standard\",           \"filter\": [             \"lowercase\",             \"mynGram\"           ]         },         \"sg_search_analyzer\": {           \"type\": \"custom\",           \"tokenizer\": \"standard\",           \"filter\": [             \"standard\",             \"lowercase\"           ]         },         \"keylower\": {           \"tokenizer\": \"keyword\",           \"filter\": \"lowercase\"         }       },       \"filter\": {         \"mynGram\": {           \"type\": \"nGram\",           \"min_gram\": 1,           \"max_gram\": 20,           \"token_chars\": [             \"letter\",             \"digit\",             \"whitespace\",             \"punctuation\",             \"symbol\"           ]         }       }     }   }";
		String mappings = "{\"dynamic_templates\":[{\"longs\":{\"match_mapping_type\":\"long\",\"mapping\":{\"type\":\"long\",\"fields\":{\"raw\":{\"type\":\"long\"}}}}},{\"booleans\":{\"match_mapping_type\":\"boolean\",\"mapping\":{\"type\":\"boolean\",\"fields\":{\"raw\":{\"type\":\"boolean\"}}}}},{\"doubles\":{\"match_mapping_type\":\"double\",\"mapping\":{\"type\":\"double\",\"fields\":{\"raw\":{\"type\":\"double\"}}}}},{\"dates\":{\"match_mapping_type\":\"date\",\"mapping\":{\"type\":\"date\",\"fields\":{\"raw\":{\"type\":\"date\"}}}}},{\"strings\":{\"match_mapping_type\":\"string\",\"mapping\":{\"type\":\"text\",\"copy_to\":\"all_fields\",\"analyzer\":\"sg_index_analyzer\",\"search_analyzer\":\"sg_search_analyzer\",\"fields\":{\"raw\":{\"type\":\"text\",\"analyzer\":\"keylower\"}}}}}],\"properties\":{\"all_fields\":{\"type\":\"text\",\"analyzer\":\"sg_index_analyzer\",\"search_analyzer\":\"sg_search_analyzer\",\"fields\":{\"raw\":{\"type\":\"text\",\"analyzer\":\"keylower\"}}}}}";
		TelemetryManager.log("Creating Suggestion Index : " + SuggestionConstants.SUGGESTION_INDEX);
		ElasticSearchUtil.addIndex(SuggestionConstants.SUGGESTION_INDEX, SuggestionConstants.SUGGESTION_INDEX_TYPE,
				settings, mappings);
	}

	/**
	 * This methods holds logic to add document to suggestions index in elastic
	 * search
	 * 
	 * @return SuggestionId
	 * 
	 * @throws IOException
	 */
	private static String addDocument(Map<String, Object> request) throws IOException {
		String suggestionId = "sg_" + Identifier.getUniqueIdFromTimestamp();
		String document = null;
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		if (StringUtils.isNoneBlank(suggestionId)) {
			request.put(SuggestionCodeConstants.suggestion_id.name(), suggestionId);
			TelemetryManager.log("Checking if document is empty : " + request);

			if (!request.isEmpty()) {
				request.put(SuggestionCodeConstants.createdOn.name(), df.format(new Date()));
				request.put(SuggestionCodeConstants.status.name(), "new");
				document = mapper.writeValueAsString(request);
				TelemetryManager.log("converting request map to string : " + document);
			}
			if (StringUtils.isNotBlank(document)) {
				ElasticSearchUtil.addDocumentWithId(SuggestionConstants.SUGGESTION_INDEX,
						SuggestionConstants.SUGGESTION_INDEX_TYPE, suggestionId, document);
				TelemetryManager.log("Adding document to Suggetion Index : " + document);
			}
		}
		TelemetryManager.log("Returning suggestionId as response to saved suggestion" + suggestionId);
		return suggestionId;
	}

	/**
	 * This method holds logic to setResponse
	 * 
	 * @param response
	 *            The response
	 * 
	 * @param suggestionId
	 *            The suggestionId
	 * 
	 * @return The response
	 */
	private Response setResponse(Response response, String suggestionId) {
		TelemetryManager.log("Setting response" + suggestionId);
		response.setParams(getSucessStatus());
		response.getResult().put("suggestion_id", suggestionId);
		response.setResponseCode(response.getResponseCode());
		return response;
	}

	/**
	 * This method holds logic to getSuggestionById from elastic search based on
	 * search criteria
	 * 
	 * @param objectId
	 *            The objectId
	 * 
	 * @param start_date
	 *            The startDate
	 * 
	 * @param end_date
	 *            The endDate
	 * 
	 * @return The List of suggestions for given objectId
	 */
	private List<Object> getSuggestionByObjectId(String objectId, String start_date, String end_date, String status) {
		SearchDTO search = new SearchDTO();
		search.setProperties(setSearchFilters(objectId, start_date, end_date, status, null, null));
		search.setOperation(CompositeSearchConstants.SEARCH_OPERATION_AND);
		Map<String, String> sortBy = new HashMap<String, String>();
		sortBy.put(GraphDACParams.createdOn.name(), "desc");
		search.setSortBy(sortBy);
		TelemetryManager.log("setting search criteria to fetch records from ES" + search);
		List<Object> suggestionResult = search(search);
		TelemetryManager.log("list of fields returned from ES based on search query" + suggestionResult);
		return suggestionResult;
	}

	/**
	 * This method holds logic to get Suggestions based on search criteria
	 * 
	 * @param status
	 *            The status
	 * 
	 * @param suggestedBy
	 *            The suggestedBy
	 * 
	 * @param suggestionId
	 *            The suggestionId
	 * 
	 * @return List of suggestions
	 */
	private List<Object> getSuggestionsList(String status, String suggestedBy, String suggestionId) {
		SearchDTO search = new SearchDTO();
		search.setProperties(setSearchFilters(null, null, null, status, suggestedBy, suggestionId));
		search.setOperation(CompositeSearchConstants.SEARCH_OPERATION_AND);
		Map<String, String> sortBy = new HashMap<String, String>();
		sortBy.put(GraphDACParams.createdOn.name(), "desc");
		search.setSortBy(sortBy);
		TelemetryManager.log("setting search criteria to fetch records from ES" + search);
		List<Object> suggestionResult = search(search);
		TelemetryManager.log("list of fields returned from ES based on search query" + suggestionResult);
		return suggestionResult;
	}

	/**
	 * This method holds logic to set Search criteria
	 * 
	 * @param objectId
	 *            The objectId
	 * 
	 * @param start_date
	 *            The startDate
	 * 
	 * @param end_date
	 *            The endDate
	 * 
	 * @param status
	 *            The status
	 * 
	 * @param suggestedBy
	 *            The suggestedBy
	 * 
	 * @param suggestion_id
	 *            The suggestionId
	 * 
	 * @return List of suggestions
	 */
	@SuppressWarnings("rawtypes")
	private List<Map> setSearchFilters(String objectId, String start_date, String end_date, String status,
			String suggestedBy, String suggestion_id) {
		List<Map> properties = new ArrayList<Map>();

		TelemetryManager.log("setting search criteria for start_date");
		if (StringUtils.isNotBlank(start_date)) {
			Map<String, Object> property = new HashMap<String, Object>();
			property.put("operation", CompositeSearchConstants.SEARCH_OPERATION_RANGE);
			property.put("propertyName", GraphDACParams.createdOn.name());
			Map<String, Object> range_map = new HashMap<String, Object>();
			range_map.put(CompositeSearchConstants.SEARCH_OPERATION_RANGE_GTE, start_date);
			property.put("values", range_map);
			properties.add(property);
		}
		TelemetryManager.log("setting search criteria for end_date");
		if (StringUtils.isNotBlank(end_date)) {
			Map<String, Object> property = new HashMap<String, Object>();
			property.put("operation", CompositeSearchConstants.SEARCH_OPERATION_RANGE);
			property.put("propertyName", GraphDACParams.createdOn.name());
			Map<String, Object> range_map = new HashMap<String, Object>();
			range_map.put(CompositeSearchConstants.SEARCH_OPERATION_RANGE_LTE, end_date);
			property.put("values", range_map);
			properties.add(property);
		}
		TelemetryManager.log("setting search criteria for objectId");
		if (StringUtils.isNotBlank(objectId)) {
			Map<String, Object> property = new HashMap<String, Object>();
			property.put("operation", CompositeSearchConstants.SEARCH_OPERATION_EQUAL);
			property.put("propertyName", "objectId");
			property.put("values", Arrays.asList(objectId));
			properties.add(property);
		}
		TelemetryManager.log("setting search criteria for status");
		if (StringUtils.isNotBlank(status)) {
			Map<String, Object> property = new HashMap<String, Object>();
			property.put("operation", CompositeSearchConstants.SEARCH_OPERATION_EQUAL);
			property.put("propertyName", "status");
			property.put("values", Arrays.asList(status));
			properties.add(property);
		}
		TelemetryManager.log("setting search criteria for suggestedBy");
		if (StringUtils.isNotBlank(suggestedBy)) {
			Map<String, Object> property = new HashMap<String, Object>();
			property.put("operation", CompositeSearchConstants.SEARCH_OPERATION_EQUAL);
			property.put("propertyName", "suggestedBy");
			property.put("values", Arrays.asList(suggestedBy));
			properties.add(property);
		}
		TelemetryManager.log("setting search criteria for suggestion_id");
		if (StringUtils.isNotBlank(suggestion_id)) {
			Map<String, Object> property = new HashMap<String, Object>();
			property.put("operation", CompositeSearchConstants.SEARCH_OPERATION_EQUAL);
			property.put("propertyName", "suggestion_id");
			property.put("values", Arrays.asList(suggestion_id));
			properties.add(property);
		}
		TelemetryManager.log("returning the search filters" + properties);
		return properties;
	}

	/**
	 * This method holds logic to call search processor to search suggestions
	 * based on criteria
	 * 
	 * @param search
	 *            The searchDto
	 * 
	 * @return List of suggestions
	 */
	public List<Object> search(SearchDTO search) {
		List<Object> result = new ArrayList<Object>();
		try {
			TelemetryManager.log("sending search request to search processor" + search);
			result = Await.result(processor.processSearchQuery(search, false,
					SuggestionConstants.SUGGESTION_INDEX), RequestRouterPool.WAIT_TIMEOUT.duration());
			TelemetryManager.log("result from search processor" + result);
		} catch (Exception e) {
			TelemetryManager.error("error while processing the search request: "+ e.getMessage(), e);
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * This method holds logic to validate request
	 * 
	 * @param requestMap
	 *            The requestMap
	 * 
	 * @param statusToAdd
	 *            The expectedStatus
	 * 
	 * @return requestMap
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected Map<String, Object> dataToUpdate(Map<String, Object> requestMap, String statusToAdd) {
		Map<String, Object> updatedMessage = new HashMap<String, Object>();
		Map<String, Object> requestObj = (Map) requestMap.get(SuggestionCodeConstants.request.name());
		if (null != requestObj && !requestObj.isEmpty()) {
			Map<String, Object> contentMap = (Map) requestObj.get(SuggestionCodeConstants.content.name());
			if (null != contentMap && !contentMap.isEmpty())
				updatedMessage.putAll(contentMap);
		}
		updatedMessage.put(SuggestionCodeConstants.status.name(), statusToAdd);
		return updatedMessage;
	}
}
