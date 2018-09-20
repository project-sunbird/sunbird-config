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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
            Iterator configKeys = request.getJSONObject("request").getJSONArray("keys").iterator();

            TelemetryManager.log("ConfigService | GET | configKeys: " + configKeys.toString());

            Map<String, Object> result = new HashMap<>();
            while(configKeys.hasNext()) {
                String configKey = (String)configKeys.next();

                if (ConfigStore.isConfigKeyExists(configKey)) {
                    Object data = ConfigStore.read(configKey);
                    result.put(configKey, data);
                }
            }

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
            Map<String, Object> result = new HashMap<>();

            Integer configCount = ConfigStore.getConfigCount();
            Long lastRefresh = ConfigStore.getLastRefreshTimestamp();

            result.put("size", configCount);
            result.put("lastUpdated", lastRefresh);

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
}