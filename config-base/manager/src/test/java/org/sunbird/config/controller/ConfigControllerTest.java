package org.sunbird.config.controller;

import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.sunbird.common.dto.Response;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration({ "classpath:servlet-context.xml" })
public class ConfigControllerTest {
	
	@Autowired
	private WebApplicationContext context;
	private ResultActions actions;
	private static String TEST_LANGUAGE = "en";
	private static String INVALID_LANGUAGE = "ee";
	
	@SuppressWarnings("unchecked")
	
	@Test
	public void getOrdinals_01(){
		
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v2/config/ordinals";
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.get(path).header(
					"user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
		Map<String, Object> result = response.getResult();
		Map<String, Object> ordinals = (Map<String, Object>) result.get("ordinals");
			assertEquals(30, ordinals.keySet().size());
			assertEquals(true, ordinals.keySet().contains("os"));
			assertEquals(true, ordinals.keySet().contains("optStatus"));
			assertEquals(true,ordinals.keySet().contains("skills"));
			assertEquals(30, ordinals.values().size());
			assertEquals(false,ordinals.values().isEmpty());
				
	}
	@Test
	public void getOrdinals_02(){
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v2/config/ordinal";
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.get(path).header(
					"user-id", "ilimi"));
			Assert.assertEquals(404, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void getResourceBundleForValidLanguage(){
		
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v2/config/resourcebundles/" + TEST_LANGUAGE;
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.get(path).header(
					"user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
		Map<String, Object> result = response.getResult();
		Map<String, String> map = new HashMap<String, String>();
		map = (Map<String, String>) result.get("en");
		assertEquals(true, result.containsKey("en"));
		assertEquals(true, map.containsKey("visibility"));	
	}
	@Test
	public void getResourceBundleForValidLanguage_01(){
		
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v2/config/resourcebundle/" + TEST_LANGUAGE;
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.get(path).header(
					"user-id", "ilimi"));
			Assert.assertEquals(404, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	@Ignore
	@Test
	public void getResourceBundles(){
		
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v2/config/resourcebundles";
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.get(path).header(
					"user-id", "ilimi"));
			Assert.assertEquals(200, actions.andReturn().getResponse()
					.getStatus());
			Assert.assertEquals(500, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}
		Response response = jsonToObject(actions);
		Assert.assertEquals("successful", response.getParams().getStatus());
		Map<String, Object> result = response.getResult();
		assertEquals(true, result.containsKey("resourcebundles"));
		assertEquals(false, result.isEmpty());
		Map<String, Object> resourceMap =  new HashMap<String, Object>();
	    resourceMap = (Map<String, Object>) result.get("resourcebundles");	
		assertEquals(true, resourceMap.containsKey("en"));
		assertEquals(true, resourceMap.containsKey("ka"));
		Map<String, String> langMap =  new HashMap<String, String>();
		langMap = (Map<String, String>) resourceMap.get("te");
		assertEquals("Visibility", langMap.get("visibility"));
		assertEquals("Code", langMap.get("code"));
		assertEquals("Name", langMap.get("name"));
	}
	
	@Test
	public void getResourceBundles_01(){
		
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v2/config/resourcebundl";
		try {
			actions = mockMvc.perform(MockMvcRequestBuilders.get(path).header(
					"user-id", "ilimi"));
			Assert.assertEquals(404, actions.andReturn().getResponse()
					.getStatus());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Ignore
	@Test
	public void getResourceBundleForInvalidLanguage(){
		
		MockMvc mockMvc;
		mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
		String path = "/v2/config/resourcebundles/" + INVALID_LANGUAGE;
			try {
				actions = mockMvc.perform(MockMvcRequestBuilders.get(path).header(
						"user-id", "ilimi"));
			} catch (Exception e) {
				e.getMessage();
			}
			Assert.assertEquals(404, actions.andReturn().getResponse()
					.getStatus());
	
		Response response = jsonToObject(actions);
		Assert.assertEquals("failed", response.getParams().getStatus());
	}

	public static Response jsonToObject(ResultActions actions) {
		String content = null;
		Response resp = null;
		try {
			content = actions.andReturn().getResponse().getContentAsString();
			ObjectMapper objectMapper = new ObjectMapper();
			if (StringUtils.isNotBlank(content))
				resp = objectMapper.readValue(content, Response.class);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resp;
	}
}
