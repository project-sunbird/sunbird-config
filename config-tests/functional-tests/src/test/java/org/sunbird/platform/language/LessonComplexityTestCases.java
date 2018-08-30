package org.sunbird.platform.language;

import org.sunbird.platform.domain.BaseTest;

import static org.hamcrest.CoreMatchers.*;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.http.ContentType.JSON;


public class LessonComplexityTestCases extends BaseTest {
	
	String jsonUpdateSingleTextbook = "{\"request\":{\"gradeLevel\":\"Grade 1\",\"languageLevel\":\"First\",\"source\":\"rajastan\",\"text\":\"\"}}";
	String jsonTextAnalysisValid = "{\"request\": {\"text\":\"\"}}";
	
	// Get Grade level complexity with valid language
	
	@Test
	public void getGradeLevelComplexityValidExpectSuccess200(){
		
		setURI();
		Response R = 
		given().
			spec(getRequestSpec(contentType, validuserId)).
		when().	
			get("language/v1/language/hi/gradeLevelComplexity").
		then().
			log().all().
			spec(get200ResponseSpec()).
		extract().
			response();
		
		// Extract the response
		JsonPath jPath = R.jsonPath();
		if (jPath.get("result.grade_level_complexity.metadata.gradeLevel").toString().equals("Grade 2") && jPath.get("result.grade_level_complexity.metadata.languageLevel").toString().equals("First")){
			Float avgComplexity = jPath.get("result.grade_level_complexity.metadata.averageTotalComplexity");
			Assert.assertTrue(avgComplexity!=null);
		}
	}
	// Get Grade level complexity with invalid language
	
	@Test
	public void getGradeLevelComplexityInvalidExpect400(){
		
		setURI();
		given().
			spec(getRequestSpec(contentType, validuserId)).
		when().
			get("language/v1/language/hkjhc/gradeLevelComplexity").
		then().
			spec(get200ResponseSpec()).
			body("result.grade_level_complexity", hasItem(null));
	}
	
	// Update grade level complexity with single textbook
	
	@Test
	public void updateComplexitySingleTextbookExpectSuccess200(){
		
		setURI();
		Response R =
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonUpdateSingleTextbook).
		with().
			contentType(JSON).
		when().
			patch("language/v1/language/hi/GradeLevelComplexity/").
		then().
			spec(get200ResponseSpec()).
		extract().
			response();
		
		// Extract the response
		JsonPath jPath = R.jsonPath();
		Float avgComplexity = jPath.get("result.grade_level_complexity.metadata.averageTotalComplexity");
		Assert.assertTrue(avgComplexity!=null);		
	}
	
	// Update grade level complexity with multiple textbook

	@Test 
	public void updateComplexityMultipleTextbooksExpectSuccess200(){
		
		// Update the complexity value with single book
		setURI();
			given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonUpdateSingleTextbook).
		with().
			contentType(JSON).
		when().
			patch("language/v1/language/hi/GradeLevelComplexity/").
		then().
			spec(get200ResponseSpec());
		
		//Get the complexity value
		setURI();
		Response R = 
		given().
			spec(getRequestSpec(contentType, validuserId)).
		when().	
			get("language/v1/language/hi/gradeLevelComplexity").
		then().
			spec(get200ResponseSpec()).
		extract().
			response();
		
		// Extract the response
		JsonPath jPath = R.jsonPath();
		Float avgComplexity = jPath.get("result.grade_level_complexity.metadata.averageTotalComplexity");
		System.out.println(avgComplexity);
		
		// Update text of multiple textbooks
		setURI();
		JSONObject js = new JSONObject(jsonUpdateSingleTextbook);
		js.getJSONObject("request").put("text", "");
		String jsonUpdateMultipleTextbook = js.toString();
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonUpdateMultipleTextbook).
		with().
			contentType(JSON).
		when().
			patch("language/v1/language/hi/GradeLevelComplexity/").
		then().
			spec(get200ResponseSpec());
		
		//Get the complexity value
		setURI();
		Response R2 = 
		given().
			spec(getRequestSpec(contentType, validuserId)).
		when().	
			get("language/v1/language/hi/gradeLevelComplexity").
		then().
			spec(get200ResponseSpec()).
		extract().
			response();
		
		// Extract the response
		JsonPath jPath2 = R2.jsonPath();
		Float avgComplexity2 = jPath2.get("result.text_complexity.averageTotalComplexity");
		Assert.assertFalse(avgComplexity.equals(avgComplexity2));
		System.out.println(avgComplexity2);
	}
	
	// Update grade level complexity with value of second language higher than first language
	
	@Test
	public void updateInvalidComplexityValuesWithLanguage(){

		// Update the complexity value for first language	
		setURI();
		JSONObject js = new JSONObject(jsonUpdateSingleTextbook);
		js.getJSONObject("request").put("text", "");
		String jsonUpdateValidComplexityLanguage = js.toString();
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonUpdateValidComplexityLanguage).
		with().
			contentType(JSON).
		then().
			patch("language/v1/language/hi/GradeLevelComplexity");
		
		//Get the complexity value
		setURI();
		Response R = 
		given().
			spec(getRequestSpec(contentType, validuserId)).
		when().	
			get("language/v1/language/hi/gradeLevelComplexity").
		then().extract().
			response();
		
		// Extract the response
		JsonPath jPath = R.jsonPath();
		if (jPath.get("result.grade_level_complexity.metadata.gradeLevel").toString().equals("Grade 2") && jPath.get("result.grade_level_complexity.metadata.languageLevel").toString().equals("First")){
			Float avgComplexity = jPath.get("result.grade_level_complexity.metadata.averageTotalComplexity");
			System.out.println(avgComplexity);
		}
		// Update the complexity value for second language
		setURI();
		JSONObject js1 = new JSONObject(jsonUpdateSingleTextbook);
		js1.getJSONObject("request").put("languageLevel","Second").put("text", "");
		String jsonUpdateSecondLanguage = js1.toString();
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonUpdateSecondLanguage).
		with().
			contentType(JSON).
		when().
			patch("language/v1/language/hi/GradeLevelComplexity").
		then().
			spec(get400ResponseSpec());
		}

	// Update complexity with value of grade-1 first language higher than grade-2 first language
	
	@Test
	public void updateInvalidComplexitValuesWithGrade(){
		
		// Update the complexity value for grade-2 first language	
		setURI();
		JSONObject js = new JSONObject(jsonUpdateSingleTextbook);
		js.getJSONObject("request").put("gradeLevel","Grade 2").put("text", "");
		String jsonUpdateValidComplexityGrade = js.toString();
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonUpdateValidComplexityGrade).
		with().
			contentType(JSON).
		then().
			patch("language/v1/language/hi/GradeLevelComplexity");
		
		//Get the complexity value
		setURI();
		Response R = 
		given().
			spec(getRequestSpec(contentType, validuserId)).
		when().	
			get("language/v1/language/hi/gradeLevelComplexity").
		then().
		extract().
			response();
		
		// Extract the response
		JsonPath jPath = R.jsonPath();
		//System.out.println(jPath.get("result.grade_level_complexity.metadata.averageTotalComplexity").toString());
		if (jPath.get("result.grade_level_complexity.metadata.gradeLevel").toString().equals("Grade 2") && jPath.get("result.grade_level_complexity.metadata.languageLevel").toString().equals("First")){
			Float avgComplexity = jPath.get("result.grade_level_complexity.metadata.averageTotalComplexity");
			System.out.println(avgComplexity);
		}
		// Update text the complexity value for grade-1 first language(higher than grade-2)
		
		setURI();
		JSONObject js1 = new JSONObject(jsonUpdateSingleTextbook);
		js1.getJSONObject("request").put("gradeLevel","Grade 1").put("text", "");
		String jsonUpdateInvalidComplexityGrade = js1.toString();
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonUpdateInvalidComplexityGrade).
		with().
			contentType(JSON).
		when().
			patch("language/v1/language/hi/GradeLevelComplexity").
		then().
			spec(get400ResponseSpec());		
		}	
	
	// Update complexity with non-indian language
	
	@Test
	public void updateComplexityWithNonIndianLanguageExpect500(){
		
		setURI();
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonUpdateSingleTextbook).
		with().
			contentType(JSON).
		when().
			patch("language/v1/language/en/GradeLevelComplexity").
		then().
			spec(get500ResponseSpec());		
	}
	
	// Text analysis with text lies within the complexity value's range
	
	@Test
	public void textAnalysisWithValidTextExpectSuccess200(){
		
		setURI();
		Response R =
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonTextAnalysisValid).
		with().
			contentType(JSON).
		when().
			post("/v1/language/tools/textAnalysis/hi/text/").
		then().
			spec(get200ResponseSpec()).
		extract().
			response();
		
		JsonPath jPath = R.jsonPath();
		String avgComplexity = jPath.get("result.text_complexity.averageTotalComplexity");
		System.out.println(avgComplexity);
	}
	
	// Text analysis with text lies within the complexity value's range of more than one grade

	@Test
	public void textAnalysisValidTextMultipleGradesExpectSuccess200() {
		setURI();
		Response R =
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body("").
		with().
			contentType(JSON).
		when().
			post("/v1/language/tools/textAnalysis/hi/text/").
		then().
			spec(get200ResponseSpec()).
		extract().
			response();
		
		JsonPath jPath = R.jsonPath();
		String avgComplexity = jPath.get("result.text_complexity.averageTotalComplexity");
		System.out.println(avgComplexity);
	}
	
	// Text analysis with text which doesn't have complexity values
	@Test
	public void textAnalysisForTextWithNoComplexityExpectSuccess200() {
		
		setURI();
		JSONObject js = new JSONObject(jsonTextAnalysisValid);
		js.getJSONObject("request").put("gradeLevel","Grade 1").put("text", "");
		String jsonTextAnalysisKannada = js.toString();
		Response R =
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonTextAnalysisKannada).
		with().
			contentType(JSON).
		when().
			post("/v1/language/tools/textAnalysis/ka/text/").
		then().
			spec(get200ResponseSpec()).
		extract().
			response();
		
		JsonPath jPath = R.jsonPath();
		String avgComplexity = jPath.get("result.text_complexity.averageTotalComplexity");
		System.out.println(avgComplexity);
	}
	
	// Text analysis with text which doesn't support complexity calculation
	
	@Test
	public void textAnlaysisForUnsupportedLanguageExpect4xx(){
		
		setURI();
		given().
			spec(getRequestSpec(contentType, validuserId)).
			body(jsonTextAnalysisValid).
		with().
			contentType(JSON).
		when().
			post("/v1/language/tools/textAnalysis/en/text/").
		then().
			spec(get400ResponseSpec());
	}	
}
