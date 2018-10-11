package org.sunbird.config.controller;

import java.lang.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.junit.FixMethodOrder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.config.util.ConfigStore;
import org.sunbird.telemetry.logger.TelemetryManager;
import org.junit.runners.MethodSorters;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import java.util.*;
import org.sunbird.common.dto.Response;
import org.sunbird.common.dto.ResponseParams;
import org.sunbird.common.dto.ResponseParams.StatusType;
import scala.Array;
import org.json.JSONObject;
import com.google.gson.JsonParser;
import org.springframework.http.ResponseEntity;

@ContextConfiguration
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConfigStore.class })
@PowerMockIgnore("javax.management.*")
public class ConfigControllerTest {

  @Autowired
  private ConfigController configController;

  @Before
  public void setup() {
    PowerMockito.mockStatic(ConfigStore.class);
    configController = new ConfigController();
  }

  @Test
  public void getConfigurationsTestValidKey() {
    String configKey = "instance.portal/sunbird_enable_signup";
    String configValue = "true";
    PowerMockito.when(ConfigStore.read(configKey)).thenReturn(configValue);
    HashMap<String, Object> requestMap = new HashMap<String, Object>();
    HashMap<String, Object> requestData = new HashMap<String, Object>();
    String[] configs = { configKey };
    requestData.put("keys", configs);
    requestMap.put("request", requestData);
    ResponseEntity<Response> response = configController.getConfigurations(requestMap);
    JSONObject result = new JSONObject(response.getBody().getResult());
    assertEquals(org.springframework.http.HttpStatus.OK.value(), response.getStatusCode().value());
    assertEquals(configValue, result.getJSONObject("keys").getString(configKey));
  }

  @Test
  public void getConfigurationsTestInvalidKey() {
    String configKey = "instances.invalid.portal";
    PowerMockito.when(ConfigStore.read(any(String.class))).thenReturn(null);
    HashMap<String, Object> requestMap = new HashMap<String, Object>();
    HashMap<String, Object> requestData = new HashMap<String, Object>();
    String[] configs = { configKey };
    requestData.put("keys", configs);
    requestMap.put("request", requestData);
    ResponseEntity<Response> response = configController.getConfigurations(requestMap);
    JSONObject result = new JSONObject(response.getBody().getResult());
    assertEquals(org.springframework.http.HttpStatus.OK.value(), response.getStatusCode().value());
    assertEquals(0, result.getJSONObject("keys").length());
  }

  @Test
  public void getConfigurationsTestError() {
    String configKey = "instance.portal";
    PowerMockito.when(ConfigStore.read(any(String.class))).thenReturn(null);
    HashMap<String, Object> requestMap = new HashMap<String, Object>();
    HashMap<String, Object> requestData = new HashMap<String, Object>();
    String[] configs = { configKey };
    requestData.put("keyss", configs);
    requestMap.put("request", requestData);
    ResponseEntity<Response> response = configController.getConfigurations(requestMap);
    JSONObject result = new JSONObject(response.getBody().getResult());
    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.getStatusCode().value());
  }

  @Test
  public void refreshConfigurationsTestSuccess() {

    String configPath = "config-1.10.json";
    PowerMockito.when(ConfigStore.refresh(configPath)).thenReturn(true);
    HashMap<String, Object> requestMap = new HashMap<String, Object>();
    HashMap<String, Object> requestData = new HashMap<String, Object>();
    requestData.put("path", configPath);
    requestMap.put("request", requestData);
    ResponseEntity<Response> response = configController.refreshConfigurations(requestMap);
    JSONObject result = new JSONObject(response.getBody());
    assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
    assertEquals(result.getJSONObject("params").getString("status"), "successful");
  }

  @Test
  public void refreshConfigurationsTestFail() {

    String configPath = "config-1.10.json";
    PowerMockito.when(ConfigStore.refresh(configPath)).thenReturn(false);
    HashMap<String, Object> requestMap = new HashMap<String, Object>();
    HashMap<String, Object> requestData = new HashMap<String, Object>();
    requestData.put("path", configPath);
    requestMap.put("request", requestData);
    ResponseEntity<Response> response = configController.refreshConfigurations(requestMap);
    JSONObject result = new JSONObject(response.getBody());
    assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
    assertEquals(result.getJSONObject("params").getString("status"), "failed");
  }

  @Test
  public void healthCheckTestSuccess() {
    Boolean healthStatus = true;
    PowerMockito.when(ConfigStore.checkDatabaseHealth()).thenReturn(healthStatus);
    ResponseEntity<Response> response = configController.getHealth();
    Map<String, Object> result = (Map<String, Object>) response.getBody().getResult();
    Map<String, Object> respData = (Map<String, Object>) result.get("response");
    assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
    assertEquals(respData.get("healthy"), healthStatus);
  }

  @Test
  public void healthCheckTestFail() {
    Boolean healthStatus = false;
    PowerMockito.when(ConfigStore.checkDatabaseHealth()).thenReturn(healthStatus);
    ResponseEntity<Response> response = configController.getHealth();
    Map<String, Object> result = (Map<String, Object>) response.getBody().getResult();
    Map<String, Object> respData = (Map<String, Object>) result.get("response");
    assertEquals(HttpStatus.OK.value(), response.getStatusCode().value());
    assertEquals(respData.get("healthy"), healthStatus);
  }

  @Configuration
  static class ConfigControllerTestConfiguration {
    @Bean
    public ConfigController configController() {
      return new ConfigController();
    }
  }

}
