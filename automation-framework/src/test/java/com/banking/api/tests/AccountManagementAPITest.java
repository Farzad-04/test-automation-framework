package com.banking.api.tests;

import com.banking.api.base.BaseTest;
import com.banking.api.models.CreateAccountRequest;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Account Management API Tests
 * Tests for creating, retrieving, and managing bank accounts
 */
public class AccountManagementAPITest extends BaseTest {
    
    // ==================== POSITIVE TEST CASES ====================
    
    @Test(description = "User can create a new savings account")
    public void testCreateSavingsAccount() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("SAVINGS", 5000.0);
        
        givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .body("accountId", notNullValue())
                .body("accountType", equalTo("SAVINGS"))
                .body("balance", equalTo(5000.0f))
                .body("isActive", equalTo(true))
                .body("accountNumber", notNullValue())
                .body("createdAt", notNullValue());
    }
    
    @Test(description = "User can create a checking account")
    public void testCreateCheckingAccount() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("CHECKING", 2000.0);
        
        givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .body("accountType", equalTo("CHECKING"))
                .body("balance", equalTo(2000.0f));
    }
    
    @Test(description = "User can create a money market account")
    public void testCreateMoneyMarketAccount() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("MONEY_MARKET", 10000.0);
        
        givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .body("accountType", equalTo("MONEY_MARKET"))
                .body("balance", equalTo(10000.0f));
    }
    
    @Test(description = "User can retrieve all their accounts")
    public void testRetrieveAllAccounts() {
        // Create an account first
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("SAVINGS", 5000.0);
        
        givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201);
        
        // Get all accounts
        givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].accountId", notNullValue())
                .body("[0].balance", notNullValue())
                .body("[0].isActive", equalTo(true));
    }
    
    @Test(description = "User can retrieve a specific account by ID")
    public void testRetrieveAccountById() {
        // Create an account
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("SAVINGS", 5000.0);
        
        Long accountId = givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .response()
                .jsonPath()
                .getLong("accountId");
        
        // Retrieve the specific account
        givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + accountId)
                .then()
                .statusCode(200)
                .body("accountId", equalTo(accountId))
                .body("balance", notNullValue())
                .body("accountType", notNullValue());
    }
    
    @Test(description = "Account has valid account number format")
    public void testAccountHasValidAccountNumber() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("SAVINGS", 5000.0);
        
        Response response = givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        String accountNumber = response.jsonPath().getString("accountNumber");
        
        // Account number should be 10 digits
        assert accountNumber.matches("\\d{10}") 
            : "Account number should be 10 digits, got: " + accountNumber;
    }
    
    @Test(description = "User can create account with zero initial balance")
    public void testCreateAccountWithZeroBalance() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("SAVINGS", 0.0);
        
        givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .body("balance", equalTo(0.0f));
    }
    
    // ==================== NEGATIVE TEST CASES ====================
    
    @Test(description = "Account creation fails with invalid account type")
    public void testCreateAccountWithInvalidType() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("INVALID_TYPE", 5000.0);
        
        givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }
    
    @Test(description = "Account creation fails with negative balance")
    public void testCreateAccountWithNegativeBalance() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("SAVINGS", -1000.0);
        
        givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }
    
    @Test(description = "Account creation fails with null account type")
    public void testCreateAccountWithNullType() {
        String invalidRequest = "{\n" +
                "  \"accountType\": null,\n" +
                "  \"initialBalance\": 5000.0\n" +
                "}";
        
        givenWithTokenAndBody(invalidRequest)
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(400);
    }
    
    @Test(description = "Retrieving non-existent account returns 404")
    public void testRetrieveNonExistentAccount() {
        givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/999999")
                .then()
                .statusCode(404)
                .body("error", notNullValue());
    }
    
    @Test(description = "Account creation fails without authorization token")
    public void testCreateAccountWithoutToken() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("SAVINGS", 5000.0);
        
        given()
                .header("Content-Type", "application/json")
                .body(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(401);
    }
    
    // ==================== EDGE CASE TESTS ====================
    
    @Test(description = "User can create multiple accounts of same type")
    public void testCreateMultipleAccountsOfSameType() {
        CreateAccountRequest createAccountRequest1 = new CreateAccountRequest("SAVINGS", 1000.0);
        CreateAccountRequest createAccountRequest2 = new CreateAccountRequest("SAVINGS", 2000.0);
        
        Long accountId1 = givenWithTokenAndBody(toJson(createAccountRequest1))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .response()
                .jsonPath()
                .getLong("accountId");
        
        Long accountId2 = givenWithTokenAndBody(toJson(createAccountRequest2))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .response()
                .jsonPath()
                .getLong("accountId");
        
        // Verify they have different IDs and account numbers
        assert !accountId1.equals(accountId2) : "Account IDs should be different";
        
        String accountNumber1 = givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + accountId1)
                .then()
                .statusCode(200)
                .extract()
                .path("accountNumber");
        
        String accountNumber2 = givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + accountId2)
                .then()
                .statusCode(200)
                .extract()
                .path("accountNumber");
        
        assert !accountNumber1.equals(accountNumber2) 
            : "Account numbers should be different";
    }
    
    @Test(description = "Account creation with very large balance")
    public void testCreateAccountWithLargeBalance() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("SAVINGS", 999999999.99);
        
        givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .body("balance", equalTo(999999999.99f));
    }
    
    @Test(description = "Created account is immediately active")
    public void testCreatedAccountIsActive() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("SAVINGS", 5000.0);
        
        givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .body("isActive", equalTo(true));
    }
    
    @Test(description = "Account timestamps are in correct format")
    public void testAccountTimestampsAreValid() {
        CreateAccountRequest createAccountRequest = new CreateAccountRequest("SAVINGS", 5000.0);
        
        Response response = givenWithTokenAndBody(toJson(createAccountRequest))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .response();
        
        String createdAt = response.jsonPath().getString("createdAt");
        
        // Verify timestamp is in ISO format (e.g., 2024-01-15T10:30:00.000Z)
        assert createdAt.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*") 
            : "createdAt should be in ISO format, got: " + createdAt;
    }
}
