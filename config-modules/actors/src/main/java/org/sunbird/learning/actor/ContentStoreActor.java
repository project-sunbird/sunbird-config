package org.sunbird.learning.actor;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.sunbird.common.dto.Request;
import org.sunbird.common.exception.ClientException;
import org.sunbird.graph.common.mgr.BaseGraphManager;
import org.sunbird.learning.common.enums.LearningErrorCodes;
import org.sunbird.learning.contentstore.ContentStore;
import org.sunbird.learning.contentstore.ContentStoreOperations;
import org.sunbird.learning.contentstore.ContentStoreParams;
import org.sunbird.telemetry.logger.TelemetryManager;

import akka.actor.ActorRef;

/**
 * The Class ContentStoreActor, provides akka actor functionality to access the
 * content store for body and other properties
 *
 * @author rayulu
 */
public class ContentStoreActor extends BaseGraphManager {

	ContentStore contentStore = new ContentStore();

	@SuppressWarnings("unchecked")
	@Override
	public void onReceive(Object msg) throws Exception {
		try {
			Request request = (Request) msg;
			String operation = request.getOperation();
			if (StringUtils.equalsIgnoreCase(ContentStoreOperations.updateContentBody.name(), operation)) {
				String contentId = (String) request.get(ContentStoreParams.content_id.name());
				String body = (String) request.get(ContentStoreParams.body.name());
				contentStore.updateContentBody(contentId, body);
				OK(sender());
			} else if (StringUtils.equalsIgnoreCase(ContentStoreOperations.getContentBody.name(), operation)) {
				String contentId = (String) request.get(ContentStoreParams.content_id.name());
				String body = contentStore.getContentBody(contentId);
				OK(ContentStoreParams.body.name(), body, sender());
			} else if (StringUtils.equalsIgnoreCase(ContentStoreOperations.getContentProperty.name(), operation)) {
				String contentId = (String) request.get(ContentStoreParams.content_id.name());
				String property = (String) request.get(ContentStoreParams.property.name());
				String value = contentStore.getContentProperty(contentId, property);
				OK(ContentStoreParams.value.name(), value, sender());
			} else if (StringUtils.equalsIgnoreCase(ContentStoreOperations.getContentProperties.name(), operation)) {
				String contentId = (String) request.get(ContentStoreParams.content_id.name());
				List<String> properties = (List<String>) request.get(ContentStoreParams.properties.name());
				Map<String, Object> value = contentStore.getContentProperties(contentId, properties);
				OK(ContentStoreParams.values.name(), value, sender());
			} else if (StringUtils.equalsIgnoreCase(ContentStoreOperations.updateContentProperty.name(), operation)) {
				String contentId = (String) request.get(ContentStoreParams.content_id.name());
				String property = (String) request.get(ContentStoreParams.property.name());
				String value = (String) request.get(ContentStoreParams.value.name());
				contentStore.updateContentProperty(contentId, property, value);
				OK(sender());
			} else if (StringUtils.equalsIgnoreCase(ContentStoreOperations.updateContentProperties.name(), operation)) {
				String contentId = (String) request.get(ContentStoreParams.content_id.name());
				Map<String, Object> map = (Map<String, Object>) request.get(ContentStoreParams.properties.name());
				contentStore.updateContentProperties(contentId, map);
				OK(sender());
			} else {
				TelemetryManager.log("Unsupported operation: " + operation);
				throw new ClientException(LearningErrorCodes.ERR_INVALID_OPERATION.name(),
						"Unsupported operation: " + operation);
			}
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			TelemetryManager.error("Error in ContentStoreActor: "+ e.getMessage(), e);
			handleException(e, getSender());
		}
	}

	@Override
	protected void invokeMethod(Request request, ActorRef parent) {
		// TODO Auto-generated method stub
	}

}
