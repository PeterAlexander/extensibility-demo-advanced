package com.sap.training;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.jsv.JsonSchemaValidator;
import io.vavr.control.Try;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.testutil.MockUtil;
import com.sap.training.servlets.BusinessPartnerServletSimple;
import io.vavr.control.Try;
import com.sap.cloud.sdk.cloudplatform.connectivity.Destination;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;

import static io.restassured.RestAssured.when;

@RunWith(Arquillian.class)
public class BusinessPartnerServletResilienceTest {
	private static final MockUtil mockUtil = new MockUtil();
	private static final Logger logger = LoggerFactory.getLogger(BusinessPartnerServletResilienceTest.class);

	@ArquillianResource
	private URL baseUrl;

	@Deployment
	public static WebArchive createDeployment() {
		return TestUtil.createDeployment(BusinessPartnerServletSimple.class);
	}

	@BeforeClass
	public static void beforeClass() {
		mockUtil.mockDefaults();
		mockUtil.mockErpDestination("MyErpSystem", "ERP_001");
	}

	@Before
	public void before() {
		RestAssured.baseURI = baseUrl.toExternalForm();
	}

	@Test
	public void testService() {
		// JSON schema validation from resource definition
		final JsonSchemaValidator jsonValidator = JsonSchemaValidator
				.matchesJsonSchemaInClasspath("businesspartners-schema.json");

		// HTTP GET response OK, JSON header and valid schema
		when().get("/businesspartners-resilience").then().statusCode(200).contentType(ContentType.JSON).body(jsonValidator);
	}

	@Test
	public void testCache() {

		final JsonSchemaValidator jsonValidator = JsonSchemaValidator
				.matchesJsonSchemaInClasspath("businesspartners-schema.json");

		mockUtil.mockErpDestination("MyErpSystem", "ERP_001");
		when().get("/businesspartners-resilience").then().statusCode(200).contentType(ContentType.JSON).body(jsonValidator);

		Destination dummyDestination = null;
		// Simulate a failed VDM call with non-existent destination
		DestinationAccessor.setLoader((n, o) -> Try.success(dummyDestination));
		when().get("/businesspartners-resilience").then().statusCode(200).contentType(ContentType.JSON).body(jsonValidator);
	}

}