package org.sunbird.graph.service.request.validator;

import java.util.Map;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.Platform;
import org.sunbird.common.dto.Request;
import org.sunbird.common.exception.ClientException;
import org.sunbird.graph.cache.util.RedisStoreUtil;
import org.sunbird.graph.dac.enums.GraphDACParams;
import org.sunbird.graph.dac.model.Node;
import org.sunbird.graph.service.common.DACConfigurationConstants;
import org.sunbird.graph.service.common.DACErrorCodeConstants;
import org.sunbird.graph.service.common.DACErrorMessageConstants;
import org.sunbird.telemetry.logger.TelemetryManager;

public class Neo4JBoltAuthorizationValidator extends Neo4JBoltBaseValidator {

	public void validateAuthorization(String graphId, Node node, Request request) {
		TelemetryManager.log("Graph Id: "+graphId);
		TelemetryManager.log("Graph Engine Node: ", null);

		if (StringUtils.isBlank(graphId))
			throw new ClientException(DACErrorCodeConstants.INVALID_GRAPH.name(),
					DACErrorMessageConstants.INVALID_GRAPH_ID + " | [Invalid or 'null' Graph Id.]");
		if (null == node)
			throw new ClientException(DACErrorCodeConstants.INVALID_NODE.name(),
					DACErrorMessageConstants.INVALID_NODE + " | [Invalid or 'null' Node.]");
		if (null == request)
			throw new ClientException(DACErrorCodeConstants.INVALID_REQUEST.name(),
					DACErrorMessageConstants.INVALID_REQUEST + " | [Invalid or 'null' Request Object.]");

		if (BooleanUtils.isFalse(isAuthorized(graphId, node, request)))
			throw new ClientException(DACErrorCodeConstants.NOT_FOUND.name(),
					DACErrorMessageConstants.NODE_NOT_FOUND + " | [Node (Object) not found.]");

	}

	private boolean isAuthorized(String graphId, Node node, Request request) {
		boolean isAuthorized = true;

		// Checking if Authorization Check Required, If not then the request is
		// authorized
		if (BooleanUtils.isTrue(isAuthorizationCheckRequired(request)) && StringUtils.isNotBlank(graphId)
				&& null != node && null != node.getMetadata() && null != request) {
			String consumerId = (String) request.getContext().get(GraphDACParams.CONSUMER_ID.name());
			TelemetryManager.log("Consumer Id: " + consumerId);

			String redisConsumerId = RedisStoreUtil.getNodeProperty(graphId, node.getIdentifier(),
					GraphDACParams.consumerId.name());

			if (!StringUtils.isBlank(redisConsumerId)) {
				TelemetryManager.log("Neo4J Node Consumer Id in Redis: " + redisConsumerId);
				if (!StringUtils.equals(consumerId, redisConsumerId))
					isAuthorized = false;
			} else {
				TelemetryManager.log("Fetching the Neo4J Node Metadata.");
				Map<String, Object> neo4jNode = getNeo4jNodeProperty(graphId, node.getIdentifier());
				if (null != neo4jNode && !neo4jNode.isEmpty()) {
					TelemetryManager.log("Fetched the Neo4J Node Id: " + neo4jNode.get(GraphDACParams.identifier.name())
							+ " | [Node Id: '" + node.getIdentifier() + "']");

					String neo4jNodeConsumerId = (String) neo4jNode.get(GraphDACParams.consumerId.name());
					TelemetryManager.log("Neo4J Node Consumer Id: " + neo4jNodeConsumerId);

					if (!StringUtils.equals(consumerId, neo4jNodeConsumerId))
						isAuthorized = false;
				} else {
					// Setting the 'consumerId' for node since node doesn't
					// exist in Neo4J
					TelemetryManager.log("Setting the 'consumerId' Property Since it's a node creation operation.");
					node.getMetadata().put(GraphDACParams.consumerId.name(), consumerId);
				}
			}

		}

		TelemetryManager.log("Is Authorized (For Node Id : '" + node.getIdentifier() + "') ? " + isAuthorized);
		return isAuthorized;
	}

	private boolean isAuthorizationCheckRequired(Request request) {
		boolean isCheckRequired = true;
		if (StringUtils.isBlank((String) request.getContext().get(GraphDACParams.CONSUMER_ID.name()))
				|| BooleanUtils.isFalse(isAuthEnabled()))
			isCheckRequired = false;

		TelemetryManager.log("Authorization Check Required ? " + isCheckRequired);
		return isCheckRequired;
	}

	private boolean isAuthEnabled(){
		String authEnabled = Platform.config.getString(DACConfigurationConstants.AUTHORIZATION_ENABLED_PROPERTY);
		
		if(authEnabled!=null)
			return Boolean.parseBoolean(authEnabled);
		
		return false;
	}
}
