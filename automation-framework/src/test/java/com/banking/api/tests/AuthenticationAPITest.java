package com.banking.api.tests;

import com.banking.api.base.BaseTest;
import com.banking.api.models.LoginRequest;
import com.banking.api.models.RegisterRequest;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import java.time.LocalDate;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Authentication API Tests
 * Tests for user registration, login, token validation, and logout
 */
public class AuthenticationAPITest extends BaseTest {
    
    private String testEmail;
    private String testPassword = "SecurePassword@123";
    
    @BeforeMethod
    public void beforeEachTest() {
        // Generate unique email for each test
        testEmail = "user_" + System.currentTimeMillis() + "@banking.com";
    }
    
    // ==================== POSITIVE TEST CASES ====================
    
    @Test(description = "User can register successfully with valid data")
    public void testSuccessfulRegistration() {
        RegisterRequest registerRequest = new RegisterRequest(
            "John",
            "Doe",
            testEmail,
            testPassword,
            LocalDate.of(1990, 5, 15)
        );
        
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("firstName", equalTo("John"))
                .body("lastName", equalTo("Doe"))
                .body("email", equalTo(testEmail))
                .body("token", notNullValue())
                .body("createdAt", notNullValue());
    }
    
    @Test(description = "User can login successfully with valid credentials")
    public void testSuccessfulLogin() {
        // First register a user
        RegisterRequest registerRequest = new RegisterRequest(
            "Jane",
            "Smith",
            testEmail,
            testPassword,
            LocalDate.of(1992, 3, 20)
        );
        
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(201);
        
        // Now login
        LoginRequest loginRequest = new LoginRequest(testEmail, testPassword);
        
        givenWithBody(toJson(loginRequest))
                .when()
                .post(AUTH_ENDPOINT + "/login")
                .then()
                .statusCode(200)
                .body("id", notNullValue())
                .body("email", equalTo(testEmail))
                .body("token", notNullValue())
                .body("firstName", equalTo("Jane"))
                .body("lastName", equalTo("Smith"));
    }
    
    @Test(description = "Login response includes valid JWT token")
    public void testLoginResponseIncludesToken() {
        // Register user first
        RegisterRequest registerRequest = new RegisterRequest(
            "Mike",
            "Johnson",
            testEmail,
            testPassword,
            LocalDate.of(1988, 7, 10)
        );
        
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(201);
        
        // Login and extract token
        LoginRequest loginRequest = new LoginRequest(testEmail, testPassword);
        
        Response response = givenWithBody(toJson(loginRequest))
                .when()
                .post(AUTH_ENDPOINT + "/login")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        String token = response.jsonPath().getString("token");
        
        // Verify token is in JWT format (has 3 parts separated by dots)
        assert token.matches("[A-Za-z0-9_.-]+\\.[A-Za-z0-9_.-]+\\.[A-Za-z0-9_.-]+") 
            : "Token is not in valid JWT format";
    }
    
    @Test(description = "User can access protected endpoint with valid token")
    public void testAccessProtectedEndpointWithValidToken() {
        // Register and login
        RegisterRequest registerRequest = new RegisterRequest(
            "Sarah",
            "Williams",
            testEmail,
            testPassword,
            LocalDate.of(1995, 1, 25)
        );
        
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(201);
        
        // Login to get token
        LoginRequest loginRequest = new LoginRequest(testEmail, testPassword);
        
        String token = givenWithBody(toJson(loginRequest))
                .when()
                .post(AUTH_ENDPOINT + "/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
        
        // Access protected endpoint with token
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .when()
                .get(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(200);
    }
    
    // ==================== NEGATIVE TEST CASES ====================
    
    @Test(description = "Registration fails with missing required fields")
    public void testRegistrationWithMissingEmail() {
        String invalidRequest = "{\n" +
                "  \"firstName\": \"John\",\n" +
                "  \"lastName\": \"Doe\",\n" +
                "  \"password\": \"" + testPassword + "\",\n" +
                "  \"dateOfBirth\": \"1990-05-15\"\n" +
                "}";
        
        givenWithBody(invalidRequest)
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }
    
    @Test(description = "Registration fails with invalid email format")
    public void testRegistrationWithInvalidEmailFormat() {
        RegisterRequest registerRequest = new RegisterRequest(
            "John",
            "Doe",
            "invalid-email-format",
            testPassword,
            LocalDate.of(1990, 5, 15)
        );
        
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }
    
    @Test(description = "Registration fails with weak password")
    public void testRegistrationWithWeakPassword() {
        RegisterRequest registerRequest = new RegisterRequest(
            "John",
            "Doe",
            testEmail,
            "weak",
            LocalDate.of(1990, 5, 15)
        );
        
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }
    
    @Test(description = "Registration fails with duplicate email")
    public void testRegistrationWithDuplicateEmail() {
        RegisterRequest registerRequest = new RegisterRequest(
            "John",
            "Doe",
            testEmail,
            testPassword,
            LocalDate.of(1990, 5, 15)
        );
        
        // First registration succeeds
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(201);
        
        // Second registration with same email fails
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(409)
                .body("message", containsString("already exists"));
    }
    
    @Test(description = "Login fails with invalid credentials")
    public void testLoginWithInvalidCredentials() {
        LoginRequest loginRequest = new LoginRequest(
            "nonexistent@banking.com",
            testPassword
        );
        
        givenWithBody(toJson(loginRequest))
                .when()
                .post(AUTH_ENDPOINT + "/login")
                .then()
                .statusCode(401)
                .body("error", notNullValue());
    }
    
    @Test(description = "Login fails with wrong password")
    public void testLoginWithWrongPassword() {
        // Register a user
        RegisterRequest registerRequest = new RegisterRequest(
            "John",
            "Doe",
            testEmail,
            testPassword,
            LocalDate.of(1990, 5, 15)
        );
        
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(201);
        
        // Try login with wrong password
        LoginRequest loginRequest = new LoginRequest(testEmail, "WrongPassword@123");
        
        givenWithBody(toJson(loginRequest))
                .when()
                .post(AUTH_ENDPOINT + "/login")
                .then()
                .statusCode(401)
                .body("error", notNullValue());
    }
    
    @Test(description = "Access to protected endpoint fails without token")
    public void testAccessProtectedEndpointWithoutToken() {
        given()
                .header("Content-Type", "application/json")
                .when()
                .get(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(401);
    }
    
    @Test(description = "Access to protected endpoint fails with invalid token")
    public void testAccessProtectedEndpointWithInvalidToken() {
        given()
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer invalid.token.here")
                .when()
                .get(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(401);
    }
    
    // ==================== EDGE CASE TESTS ====================
    
    @Test(description = "Registration with minimum valid data")
    public void testRegistrationWithMinimumValidData() {
        RegisterRequest registerRequest = new RegisterRequest(
            "A",
            "B",
            testEmail,
            testPassword,
            LocalDate.of(1950, 1, 1)
        );
        
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(201)
                .body("id", notNullValue());
    }
    
    @Test(description = "Registration with special characters in name")
    public void testRegistrationWithSpecialCharactersInName() {
        RegisterRequest registerRequest = new RegisterRequest(
            "Jean-Pierre",
            "O'Brien",
            testEmail,
            testPassword,
            LocalDate.of(1990, 5, 15)
        );
        
        givenWithBody(toJson(registerRequest))
                .when()
                .post(AUTH_ENDPOINT + "/register")
                .then()
                .statusCode(201)
                .body("firstName", equalTo("Jean-Pierre"))
                .body("lastName", equalTo("O'Brien"));
    }
}
