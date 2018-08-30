package org.sunbird.config.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.typesafe.config.ConfigValue;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.controller.BaseController;
import org.sunbird.common.dto.Response;
import org.sunbird.common.dto.ResponseParams;
import org.sunbird.common.dto.ResponseParams.StatusType;
import org.sunbird.common.exception.ResponseCode;
import org.sunbird.common.util.HttpDownloadUtility;
import org.sunbird.config.util.CloudStore;
import org.sunbird.telemetry.logger.TelemetryManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@Controller
@RequestMapping("/v1/")
public class ConfigController extends BaseController {
	private ObjectMapper mapper = new ObjectMapper();

	//TODO review if the Map<String, String> is sufficient
    private static Map<String,Object> configStore = new HashMap<>();

	/**
	 * Returns the value of the provided key from the Hashmap
	 * @param key Key of the configuration to be retrieved
	 * @return Any
	 */
	private Object getConfig(String key) {
        return configStore.get(key);
    }

	/**
	 * Stores the provided configuration data (as key, value) in the Hashmap
	 * @param key Key of the configuration
	 * @param value Value of the configuration
	 */
	private void setConfig(String key, Object value) {
        configStore.put(key,value);
    }

    /**
     * Returns the value of the provided key from the Hashmap
     * @param key Key of the configuration to be retrieved
     * @return Any
     */
    private Boolean isConfigKeyExists(String key) {
        return configStore.containsKey(key);
    }

    /**
     * Clears the current config data
     */
    private void clearConfig() {
        configStore.clear();
    }

    /**
     * API to refresh the config
     * @param map
     * @return
     */
    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Response> refreshConfigurations(@RequestBody(required=true) Map<String, Object> map) {
		String apiId = "sunbird.config.refresh";

		try {
            JSONObject request = new JSONObject(map);
            String configPath = request.getJSONObject("request").getString("path");

            //Get the config file from cloud store
            String configData = CloudStore.getObjectData(configPath);

            //Parse the received config using typesafe config utility
            Config parsedConfigData = ConfigFactory.parseString(configData);
            Set parsedConfigDataList = parsedConfigData.entrySet();

            //If there is some data provided
            if (parsedConfigDataList.size() > 0) {
                //Clear the previous data
                clearConfig();

                // Iterate over flat config and Store
                for (Map.Entry<String, ConfigValue> entry : parsedConfigData.entrySet()) {
                    String key = entry.getKey();
                    Object val = entry.getValue().unwrapped();
                    setConfig(key, val);
                }
            }

            //Prepare and return Response
			Response response = new Response();
			ResponseParams params = new ResponseParams();
			params.setErr("0");
			params.setStatus(StatusType.successful.name());
			params.setErrmsg("Operation successful");
			response.setParams(params);
			return getResponseEntity(response, apiId, null);
		} catch (Exception e) {
			TelemetryManager.error("Get object size | Exception: " + e.getMessage(), e);
			return getExceptionResponseEntity(e, apiId, null);
		}
	}

	@RequestMapping(value = "/read/{configKey:.+}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<Response> getConfiguration(@PathVariable(value = "configKey") String configKey) {
		String apiId = "sunbird.config.read";

		try {
			TelemetryManager.log("ConfigService | GET | configKey: " + configKey);

			Response response = new Response();
            ResponseParams params = new ResponseParams();

            if (isConfigKeyExists(configKey)) {
                Object data = getConfig(configKey);

                params.setErr("0");
                params.setStatus(StatusType.successful.name());
                params.setErrmsg("Operation successful");
                response.setParams(params);
                response.put(configKey, data);
                TelemetryManager.log("ConfigService | successResponse: " + response.getResponseCode());
			} else {
                params.setErr("1");
                params.setStatus(StatusType.failed.name());
                params.setErrmsg("Operation failed");
                response.setParams(params);
                response.setResponseCode(ResponseCode.RESOURCE_NOT_FOUND);
                TelemetryManager.log("ConfigService | FailureResponse for configKey: "+ configKey, response.getResult());
			}
            return getResponseEntity(response, apiId, null);

        } catch (Exception e) {
			TelemetryManager.error("ConfigService | Exception", e);
			return getExceptionResponseEntity(e, apiId, null);
		}
	}
}