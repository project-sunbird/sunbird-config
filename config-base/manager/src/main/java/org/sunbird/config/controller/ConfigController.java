package org.sunbird.config.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.sunbird.common.controller.BaseController;
import org.sunbird.common.dto.Response;
import org.sunbird.common.dto.ResponseParams;
import org.sunbird.common.dto.ResponseParams.StatusType;
import org.sunbird.config.util.ConfigStore;
import org.sunbird.telemetry.logger.TelemetryManager;

import java.util.*;

@Controller
@RequestMapping("/v1/")
public class ConfigController extends BaseController {
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * API to refresh the config
     *
     * @return
     */
    @RequestMapping(value = "/refresh", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Response> refreshConfigurations(@RequestBody(required = true) Map<String, Object> map) {
        String apiId = "sunbird.config.refresh";

        try {
            JSONObject request = new JSONObject(map);
            String configPath = request.getJSONObject("request").getString("path");

            Boolean configRefreshed = ConfigStore.refresh(configPath);

            //Prepare and return Response
            Response response = new Response();
            ResponseParams params = new ResponseParams();

            if (configRefreshed) {
                params.setErr("0");
                params.setStatus(StatusType.successful.name());
                params.setErrmsg("Operation successful");
                response.setParams(params);
            } else {
                params.setErr("1");
                params.setStatus(StatusType.failed.name());
                params.setErrmsg("Operation unsuccessful");
                response.setParams(params);
            }
            return getResponseEntity(response, apiId, null);
        } catch (Exception e) {
            TelemetryManager.error("Get object size | Exception: " + e.getMessage(), e);
            return getExceptionResponseEntity(e, apiId, null);
        }
    }


    @RequestMapping(value = "/read", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Response> getConfigurations(@RequestBody(required = true) Map<String, Object> map) {
        String apiId = "sunbird.config.read";

        try {
            JSONObject request = new JSONObject(map);
            JSONObject configRequestObject = request.getJSONObject("request").getJSONObject("keys");
            Iterator<String> configScopes = configRequestObject.keys();

            Map<String, Object> result = new HashMap<>();
            while(configScopes.hasNext()) {
                String configScope = (String) configScopes.next();
                Iterator configKeys = configRequestObject.getJSONArray(configScope).iterator();

                Map<String, Object> configData = new HashMap<>();
                while(configKeys.hasNext()) {
                    String configKey = (String) configKeys.next();
                    try {
                        Object data = ConfigStore.read(configKey, configScope);
                        configData.put(configKey, data);
                    } catch (Exception e) {
                        TelemetryManager.error("ConfigService | Exception | could not retrieve the value of key: " + configKey, e);
                    }
                }

                if (configData.size() > 0) {
                    result.put(configScope, configData);
                }
            }

            TelemetryManager.log("ConfigService | GET | configScopeAndKeys: " + configRequestObject.toString());



            Response response = new Response();
            ResponseParams params = new ResponseParams();
            params.setErr("0");
            params.setStatus(StatusType.successful.name());
            params.setErrmsg("Operation successful");
            response.setParams(params);
            response.put("keys", result);

            TelemetryManager.log("ConfigService | successResponse: " + response.getResponseCode());

            return getResponseEntity(response, apiId, null);

        } catch (Exception e) {
            TelemetryManager.error("ConfigService | Exception", e);
            return getExceptionResponseEntity(e, apiId, null);
        }
    }

    @RequestMapping(value = "/status", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Response> getStatus() {
        String apiId = "sunbird.config.status";

        try {
            Map<String, String> info = ConfigStore.getInfo();

            Response response = new Response();
            ResponseParams params = new ResponseParams();
            params.setErr("0");
            params.setStatus(StatusType.successful.name());
            params.setErrmsg("Operation successful");
            response.setParams(params);

            for (Map.Entry<String,String> entry: info.entrySet()) {
                response.put(entry.getKey(), entry.getValue());
            }

            TelemetryManager.log("ConfigService | successResponse: " + response.getResponseCode());

            return getResponseEntity(response, apiId, null);

        } catch (Exception e) {
            TelemetryManager.error("ConfigService | Exception", e);
            return getExceptionResponseEntity(e, apiId, null);
        }
    }

    @RequestMapping(value = "/health", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Response> getHealth() {
        // TODO refactor the health status object creation
        String apiId = "sunbird.config.health";

        try {
            Response response = new Response();
            ResponseParams params = new ResponseParams();
            params.setErr("0");
            params.setStatus(StatusType.successful.name());
            params.setErrmsg("Operation successful");
            response.setParams(params);

            Boolean cassandraStatus = false;
            try {
                cassandraStatus = ConfigStore.checkDatabaseHealth();
            } catch (Exception e) {
                TelemetryManager.error("ConfigService | Exception", e);
            }

            Map<String, Object> cassandraHealth = new HashMap<>();
            cassandraHealth.put("name", "Cassandra service");
            cassandraHealth.put("healthy", cassandraStatus);
            cassandraHealth.put("err", "");
            if (!cassandraStatus) {
                cassandraHealth.put("errmsg", "Cassandra service is Unhealthy");
            } else {
                cassandraHealth.put("errmsg", "");
            }

            Map<String, Object> configHealth = new HashMap<>();
            configHealth.put("name", "Config service");
            configHealth.put("healthy", true);
            configHealth.put("err", "");
            configHealth.put("errmsg", "");

            Boolean overallHealth = cassandraStatus;

            List<Map<String, Object>> checks = new ArrayList<>();
            checks.add(configHealth);
            checks.add(cassandraHealth);

            Map<String, Object> aggregatedResponse = new HashMap<>();
            aggregatedResponse.put("checks", checks);
            aggregatedResponse.put("healthy", overallHealth);
            aggregatedResponse.put("name", "Complete health check API");


            response.put("response", aggregatedResponse);

            TelemetryManager.log("ConfigService | successResponse: " + response.getResponseCode());

            return getResponseEntity(response, apiId, null);

        } catch (Exception e) {
            TelemetryManager.error("ConfigService | Exception", e);
            return getExceptionResponseEntity(e, apiId, null);
        }
    }
}