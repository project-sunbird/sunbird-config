package org.sunbird.config.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.typesafe.config.ConfigValue;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.cloud.storage.factory.StorageConfig;
import org.sunbird.cloud.storage.factory.StorageServiceFactory;
import org.sunbird.common.Platform;
import org.sunbird.common.controller.BaseController;
import org.sunbird.common.dto.Response;
import org.sunbird.common.dto.ResponseParams;
import org.sunbird.common.dto.ResponseParams.StatusType;
import org.sunbird.common.exception.ResponseCode;
import org.sunbird.common.util.AWSUploader;
import org.sunbird.common.util.S3PropertyReader;
import org.sunbird.config.util.CloudStore;
import org.sunbird.common.util.HttpDownloadUtility;
import org.sunbird.telemetry.logger.TelemetryManager;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.Option;

@Controller
@RequestMapping("/v1/")
public class ConfigController extends BaseController {
	private ObjectMapper mapper = new ObjectMapper();

	//TODO review the usage of config factory
//    private static Config defaultConf = ConfigFactory.load();
//    private static Config envConf = ConfigFactory.systemEnvironment();
//    public static Config config = defaultConf.withFallback(envConf);

	//TODO review if the Map<String, String> is sufficient
    public static Map<String,String> hm = new HashMap<>();

	/**
	 * Returns the value of the provided key from the Hashmap
	 * @param key Key of the configuration to be retrieved
	 * @return Any
	 */
	public String getConfig(String key) {
        return hm.get(key);
    }

	/**
	 * Stores the provided configuration data (as key, value) in the Hashmap
	 * @param key Key of the configuration
	 * @param value Value of the configuration
	 */
	public void setConfig(String key, String value) {
        System.out.println("key: " + key);
        System.out.println("value: " + value);
        hm.put(key,value);
        System.out.println("Hash Map: " + hm);

    }

    @RequestMapping(value = "/reload", method = RequestMethod.POST)
	@ResponseBody
	public ResponseEntity<Response> refreshConfigurations(@RequestParam(value = "config-path", required = true) String configPath) {
		String apiId = "sunbird.config.reload";

//		System.out.println("ConfigPath: " + configPath);

		try {
            //Get the config file from cloud store
            //TODO replace key below with the input param
            String configData = CloudStore.getObjectData(configPath);

            //Parse the received config using typesafe config utility
            Config parsedConfigData = ConfigFactory.parseString(configData);
            System.out.println("*********************** printing config starts********************");
            Set parsedConfigDataList = parsedConfigData.entrySet();
            System.out.println(parsedConfigDataList);
            System.out.println("*********************** printing config ends, Printing config line starts********************");

            // Iterate over flat config
            for (Map.Entry<String, ConfigValue> entry : parsedConfigData.entrySet()) {
                System.out.println(entry.getKey());
                System.out.println(entry.getValue());
                System.out.println(entry.getValue().valueType());
                System.out.println(entry.getValue().unwrapped());
                System.out.println(entry.getValue().toString());
                System.out.println("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$");
            }

            System.out.println("*********************** printing config line ends********************");

            //TODO store each flat config in memory


            //TODO Remove old data, only if the read from new version of config file successfull


			Response response = new Response();
//			response.put("msg", parsedConfigDataList);
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

//	// TODO this should be read API
//	// TODO this should read from in-memory
//	@RequestMapping(value = "/resourcebundles/read/{languageId}", method = RequestMethod.GET)
//	@ResponseBody
//	public ResponseEntity<Response> getResourceBundle(@PathVariable(value = "languageId") String languageId) {
//		String apiId = "ekstep.config.resourebundles.read";
//
//		try {
//			TelemetryManager.log("ResourceBundle | GET | languageId: " + languageId);
//			Response response = new Response();
//			String data = HttpDownloadUtility
//					.readFromUrl(baseUrl + folderName + "/" + languageId + ".json");
//			if (StringUtils.isNotBlank(data)) {
//				ResponseParams params = new ResponseParams();
//				params.setErr("0");
//				params.setStatus(StatusType.successful.name());
//				params.setErrmsg("Operation successful");
//				response.setParams(params);
//				response.put("ttl", 24.0);
//				try {
//					Map<String, Object> map = mapper.readValue(data, new TypeReference<Map<String, Object>>() {
//					});
//					response.put(languageId, map);
//					TelemetryManager.log("getResourceBundle | successResponse: " + response.getResponseCode());
//				} catch (Exception e) {
//					TelemetryManager.error("getResourceBundle | Exception: " + e.getMessage(), e);
//				}
//				return getResponseEntity(response, apiId, null);
//			} else {
//				ResponseParams params = new ResponseParams();
//				params.setErr("1");
//				params.setStatus(StatusType.failed.name());
//				params.setErrmsg("Operation failed");
//				response.setParams(params);
//				response.getResponseCode();
//				response.setResponseCode(ResponseCode.RESOURCE_NOT_FOUND);
//				TelemetryManager.log("getResourceBundle | FailureResponse for languageId: "+ languageId, response.getResult());
//				return getResponseEntity(response, apiId, null);
//			}
//		} catch (Exception e) {
//			TelemetryManager.error("getResourceBundle | Exception", e);
//			return getExceptionResponseEntity(e, apiId, null);
//		}
//	}

//	// TODO This API is not needed
//	@RequestMapping(value = "/ordinals/list", method = RequestMethod.GET)
//	@ResponseBody
//	public ResponseEntity<Response> getOrdinals() {
//		String apiId = "ekstep.config.ordinals.list";
//		String ordinals = "";
//		Response response = new Response();
//		try {
//			ordinals = HttpDownloadUtility.readFromUrl(baseUrl + "ordinals.json");
//			ResponseParams params = new ResponseParams();
//			params.setErr("0");
//			params.setStatus(StatusType.successful.name());
//			params.setErrmsg("Operation successful");
//			response.setParams(params);
//			response.put("ttl", 24.0);
//			try {
//				Map<String, Object> map = mapper.readValue(ordinals, new TypeReference<Map<String, Object>>() {
//				});
//				response.put("ordinals", map);
//			} catch (Exception e) {
//				TelemetryManager.error("Get Ordinals | Exception", e);
//			}
//			TelemetryManager.log("Get Ordinals | Response code: " + response.getResponseCode());
//			return getResponseEntity(response, apiId, null);
//		} catch (Exception e) {
//			TelemetryManager.error("getOrdinalsException", e);
//			return getExceptionResponseEntity(e, apiId, null);
//		}
//	}
//
//	private Map<String, String> getUrlFromS3() {
//		TelemetryManager.error("getting url from S3");
//		Map<String, String> urlList = new HashMap<String, String>();
//		String apiUrl = "";
//		TelemetryManager.error("Getting AWS object list");
//		List<String> res = AWSUploader.getObjectList(folderName, "config");
//		TelemetryManager.log("ResourceBundle Urls fetched from s3: " + res.size());
//		TelemetryManager.error("ResourceBundle Urls fetched from s3: " + res.size());
//		for (String data : res) {
//			if (StringUtils.isNotBlank(FilenameUtils.getExtension(data))) {
//				apiUrl = baseUrl + data;
//				urlList.put(FilenameUtils.getBaseName(data), apiUrl);
//			}
//		}
//		return urlList;
//	}

	//    @RequestMapping(value = "/resourcebundles/read", method = RequestMethod.GET)
//    @ResponseBody
//    public ResponseEntity<Response> getResourceBundles() {
//        String res = getConfig("sample1");
//        String apiId = "ekstep.config.object.read";
//
//
//        Response response = new Response();
//        response.put("res", res);
//        ResponseParams params = new ResponseParams();
//        params.setErr("0");
//        params.setStatus(StatusType.successful.name());
//        params.setErrmsg("Operation successful");
//        response.setParams(params);
//        return getResponseEntity(response, apiId, null);
//    }
}