package org.sunbird;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;
import static org.hamcrest.CoreMatchers.hasItems;

import java.util.HashMap;

import static com.jayway.restassured.RestAssured.baseURI;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

public class ConfigAPITest extends BaseTest {

	public String channelId = "in.ekstep";
	public String invalidChannelId = "in.ekstep.invalid";
	String jsonValidFetchRequest = "{\"request\":{\"keys\":[\"instance.portal\"]}}";
	String jsonInValidFetchRequest = "{\"request\":{\"keyss\":[\"instance.portal\"]}}";
	String jsonValidRefreshRequest = "{\"request\":{\"path\":\"config-template-v6.json\"}}";
	String jsonInValidRefreshRequest = "{\"request\":{\"paths\":\"config-template-v6.json\"}}";
	String appId = "sunbird";
	String contentType = "Application/json";
	String validuserId = "";
	String APIToken = "";

	@Test
	public void fetchConfigExpectSuccess200() {
		setURI();
		Response R = given().spec(getRequestSpec(contentType, validuserId)).body(jsonValidFetchRequest).with()
				.contentType(JSON).when().post(baseURI + "/read").then().log().all().spec(get200ResponseSpec())
				.extract().response();

		// Extracting the JSON path
		JsonPath jp = R.jsonPath();
		HashMap<String, Object> keys = jp.get("result.keys");
		String responseCode = jp.get("responseCode");
		Assert.assertTrue(keys != null);
		Assert.assertEquals("OK", responseCode);
	}

	@Test
	public void fetchConfigExpectError500() {
		setURI();
		Response R = given().spec(getRequestSpec(contentType, validuserId)).body(jsonInValidFetchRequest).with()
				.contentType(JSON).when().post(baseURI + "/read").then().log().all().spec(get500ResponseSpec())
				.extract().response();

		// Extracting the JSON path
		JsonPath jp = R.jsonPath();
		String responseCode = jp.get("responseCode");
		String errCode = jp.getString("params.err");
		Assert.assertEquals("SYSTEM_ERROR", errCode);
		Assert.assertEquals("SERVER_ERROR", responseCode);
	}

	@Test
	public void refreshConfigExpectSuccess200() {
		setURI();
		Response R = given().spec(getRequestSpec(contentType, validuserId)).body(jsonValidRefreshRequest).with()
				.contentType(JSON).when().post(baseURI + "/refresh").then().log().all().spec(get200ResponseSpec())
				.extract().response();

		// Extracting the JSON path
		JsonPath jp = R.jsonPath();
		String responseCode = jp.get("responseCode");
		String status = jp.get("params.status");
		Assert.assertEquals("successful", status);
		Assert.assertEquals("OK", responseCode);
	}

	@Test
	public void refreshConfigExpectError500() {
		setURI();
		Response R = given().spec(getRequestSpec(contentType, validuserId)).body(jsonInValidRefreshRequest).with()
				.contentType(JSON).when().post(baseURI + "/refresh").then().log().all().spec(get500ResponseSpec())
				.extract().response();

		// Extracting the JSON path
		JsonPath jp = R.jsonPath();
		String responseCode = jp.get("responseCode");
		String errCode = jp.getString("params.err");
		Assert.assertEquals("SYSTEM_ERROR", errCode);
		Assert.assertEquals("SERVER_ERROR", responseCode);
	}

	@Test
	public void checkHealthExpectSuccess200() {
		setURI();
		Response R = given().spec(getRequestSpec(contentType, validuserId)).with().contentType(JSON).when()
				.get(baseURI + "/health").then().log().all().spec(get200ResponseSpec()).extract().response();

		// Extracting the JSON path
		JsonPath jp = R.jsonPath();
		String responseCode = jp.get("responseCode");
		String healthy = jp.getString("result.response.healthy");
		Assert.assertEquals("true", healthy);
		Assert.assertEquals("OK", responseCode);
	}

}