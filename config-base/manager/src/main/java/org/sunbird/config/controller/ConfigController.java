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
import org.sunbird.common.exception.ResponseCode;
import org.sunbird.config.util.ConfigStore;
import org.sunbird.telemetry.logger.TelemetryManager;

import java.util.Map;

@Controller
@RequestMapping("/v1/")
public class ConfigController extends BaseController {
    private ObjectMapper mapper = new ObjectMapper();

    /**
     * API to refresh the config
     *
     * @param map
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

    @RequestMapping(value = "/read/{configKey:.+}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<Response> getConfiguration(@PathVariable(value = "configKey") String configKey) {
        String apiId = "sunbird.config.read";

        try {
            TelemetryManager.log("ConfigService | GET | configKey: " + configKey);

            Response response = new Response();
            ResponseParams params = new ResponseParams();

            if (ConfigStore.isConfigKeyExists(configKey)) {
                Object data = ConfigStore.read(configKey);

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
                TelemetryManager.log("ConfigService | FailureResponse for configKey: " + configKey, response.getResult());
            }
            return getResponseEntity(response, apiId, null);

        } catch (Exception e) {
            TelemetryManager.error("ConfigService | Exception", e);
            return getExceptionResponseEntity(e, apiId, null);
        }
    }
}