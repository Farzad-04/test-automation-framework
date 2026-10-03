package com.banking.api.base;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.AfterClass;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class BaseTest {

    protected static final String BASE_URL = "http://localhost:8080/api";
    protected static final String AUTH_ENDPOINT = "/auth";
    protected static final String ACCOUNTS_ENDPOINT = "/accounts";
    protected static final String TRANSACTIONS_ENDPOINT = "/transactions";

    protected String authToken;
    protected Gson gson;
    protected long testUserId;
    protected String testUserEmail;
    protected String testPassword = "Password@123";

    @BeforeMethod
    public void setUp() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

        gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();

        testUserEmail = "test_" + System.currentTimeMillis() + "@banking.com";
        registerTestUser();
    }

    @AfterClass
    public void tearDown() {
        authToken = null;
        testUserId = 0;
    }

    protected void registerTestUser() {
        try {
            String registrationBody = "{\n" +
                    "  \"firstName\": \"Test\",\n" +
                    "  \"lastName\": \"User\",\n" +
                    "  \"email\": \"" + testUserEmail + "\",\n" +
                    "  \"password\": \"" + testPassword + "\",\n" +
                    "  \"dateOfBirth\": \"1990-01-15\"\n" +
                    "}";

            Response response = givenWithBody(registrationBody)
                    .when()
                    .post(AUTH_ENDPOINT + "/register")
                    .then()
                    .extract()
                    .response();

            if (response.statusCode() == 201 || response.statusCode() == 200) {
                testUserId = response.jsonPath().getLong("id");
                authToken = response.jsonPath().getString("token");
            }
        } catch (Exception e) {
            System.out.println("Registration failed: " + e.getMessage());
        }
    }

    protected RequestSpecification givenWithToken() {
        return RestAssured.given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .log()
                .all();
    }

    protected RequestSpecification givenWithBody(String body) {
        return RestAssured.given()
                .header("Content-Type", "application/json")
                .body(body)
                .log()
                .all();
    }

    protected RequestSpecification givenWithTokenAndBody(String body) {
        return RestAssured.given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + authToken)
                .body(body)
                .log()
                .all();
    }

    protected String toJson(Object object) {
        return gson.toJson(object);
    }

    protected <T> T fromJson(String json, Class<T> classOfT) {
        return gson.fromJson(json, classOfT);
    }
}