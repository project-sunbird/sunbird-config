

//import static com.jayway.restassured.RestAssured.parameters; 
import static com.jayway.restassured.RestAssured.baseURI;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

//import org.junit.FixMethodOrder;
import org.junit.Test;

public class RestAssuredExample {
	
	/***
	public RestAssuredExample(){
		RestAssured.baseURI = "http://lp-sandbox.ekstep.org:8080/taxonomy-service"; 
	}
	***/
	
	
	/***
	@Test
    public void shouldRetrieveConcept() {
        //RestService service = new RestService();
        with().parameters("user-id", "vrayulu").expect().body("status", equalTo("OK")).when().
        		get("http://lp-sandbox.ekstep.org:8080/taxonomy-service/concept/377"); 
        }
     ***/
	
	
	@Test
	
	public void testDemo(){
	
	baseURI="http://lp-sandbox.ekstep.org:8080/taxonomy-service";
	
		given().
			header("Content-Type", "application/json").
			header("user-id", "rayuluv").
		when().
			get("/concept/numeracy_377?taxonomyId=numeracy").
		then().
			statusLine("HTTP/1.1 200 OK").
			body("id", equalTo("ekstep.lp.concept.find")).
			body("params.status", equalTo("successful"));
		
	}
	
	
	@Test
	public void getNumeracyDomain(){
		
		baseURI="http://lp-sandbox.ekstep.org:8080/taxonomy-service";
		
		given().
			header("Content-Type", "application/json").
			header("user-id", "rayuluv").
		when().
			get("v2/domains/numeracy").
		then().
			log().all().
			statusLine("HTTP/1.1 200 OK").
			body("id", equalTo("orchestrator./v2/domains/numeracy")).
			body("params.status", equalTo("successful"));
	}

}
