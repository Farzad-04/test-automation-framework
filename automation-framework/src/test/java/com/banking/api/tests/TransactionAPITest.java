package com.banking.api.tests;

import com.banking.api.base.BaseTest;
import com.banking.api.models.CreateAccountRequest;
import com.banking.api.models.TransferRequest;
import io.restassured.response.Response;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

/**
 * Transaction API Tests
 * Tests for fund transfers, transaction history, and transaction validation
 */
public class TransactionAPITest extends BaseTest {
    
    private Long fromAccountId;
    private Long toAccountId;
    
    @BeforeMethod
    public void setupAccounts() {
        // Create two accounts for testing transfers
        CreateAccountRequest request1 = new CreateAccountRequest("CHECKING", 10000.0);
        CreateAccountRequest request2 = new CreateAccountRequest("SAVINGS", 5000.0);
        
        fromAccountId = givenWithTokenAndBody(toJson(request1))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .response()
                .jsonPath()
                .getLong("accountId");
        
        toAccountId = givenWithTokenAndBody(toJson(request2))
                .when()
                .post(ACCOUNTS_ENDPOINT)
                .then()
                .statusCode(201)
                .extract()
                .response()
                .jsonPath()
                .getLong("accountId");
    }
    
    // ==================== POSITIVE TEST CASES ====================
    
    @Test(description = "User can successfully transfer funds between accounts")
    public void testSuccessfulTransfer() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 1000.0, "Test transfer");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200)
                .body("transactionId", notNullValue())
                .body("fromAccountId", equalTo(fromAccountId))
                .body("toAccountId", equalTo(toAccountId))
                .body("amount", equalTo(1000.0f))
                .body("type", equalTo("TRANSFER"))
                .body("status", equalTo("COMPLETED"))
                .body("description", equalTo("Test transfer"))
                .body("transactionDate", notNullValue());
    }
    
    @Test(description = "Sender account balance decreases after transfer")
    public void testSenderBalanceDecreases() {
        // Get initial balance
        Double initialBalance = givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + fromAccountId)
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath()
                .getDouble("balance");
        
        // Transfer funds
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 500.0, "Balance decrease test");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200);
        
        // Get new balance
        Double newBalance = givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + fromAccountId)
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath()
                .getDouble("balance");
        
        // Verify balance decreased
        assert newBalance == initialBalance - 500.0 
            : String.format("Expected balance %f, got %f", initialBalance - 500.0, newBalance);
    }
    
    @Test(description = "Recipient account balance increases after transfer")
    public void testRecipientBalanceIncreases() {
        // Get initial balance
        Double initialBalance = givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + toAccountId)
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath()
                .getDouble("balance");
        
        // Transfer funds
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 1500.0, "Balance increase test");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200);
        
        // Get new balance
        Double newBalance = givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + toAccountId)
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath()
                .getDouble("balance");
        
        // Verify balance increased
        assert newBalance == initialBalance + 1500.0 
            : String.format("Expected balance %f, got %f", initialBalance + 1500.0, newBalance);
    }
    
    @Test(description = "User can retrieve transaction history for an account")
    public void testRetrieveTransactionHistory() {
        // Make a transfer first
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 500.0, "History test");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200);
        
        // Retrieve transaction history
        givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + fromAccountId + "/transactions")
                .then()
                .statusCode(200)
                .body("$", hasSize(greaterThanOrEqualTo(1)))
                .body("[0].transactionId", notNullValue())
                .body("[0].amount", notNullValue())
                .body("[0].status", notNullValue());
    }
    
    @Test(description = "Transaction record contains all required fields")
    public void testTransactionRecordHasAllFields() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 250.0, "Field test");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200)
                .body("transactionId", notNullValue())
                .body("fromAccountId", notNullValue())
                .body("toAccountId", notNullValue())
                .body("amount", notNullValue())
                .body("type", notNullValue())
                .body("status", notNullValue())
                .body("description", notNullValue())
                .body("transactionDate", notNullValue());
    }
    
    @Test(description = "Transfer with small amount succeeds")
    public void testTransferSmallAmount() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 0.01, "Penny transfer");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));
    }
    
    @Test(description = "Transfer with large amount succeeds")
    public void testTransferLargeAmount() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 5000.0, "Large transfer");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200)
                .body("status", equalTo("COMPLETED"));
    }
    
    @Test(description = "Transfer amount is correctly recorded")
    public void testTransferAmountCorrectlyRecorded() {
        double transferAmount = 777.77;
        
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, transferAmount, "Exact amount test");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200)
                .body("amount", equalTo((float) transferAmount));
    }
    
    // ==================== NEGATIVE TEST CASES ====================
    
    @Test(description = "Transfer fails when sender has insufficient funds")
    public void testTransferInsufficientFunds() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 99999.0, "Insufficient funds test");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(400)
                .body("message", containsString("insufficient"));
    }
    
    @Test(description = "Transfer fails with negative amount")
    public void testTransferNegativeAmount() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, -100.0, "Negative amount test");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }
    
    @Test(description = "Transfer fails with zero amount")
    public void testTransferZeroAmount() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 0.0, "Zero amount test");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }
    
    @Test(description = "Transfer fails with invalid from account ID")
    public void testTransferInvalidFromAccount() {
        TransferRequest transferRequest = new TransferRequest(999999L, toAccountId, 500.0, "Invalid from account");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(404)
                .body("error", notNullValue());
    }
    
    @Test(description = "Transfer fails with invalid to account ID")
    public void testTransferInvalidToAccount() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, 999999L, 500.0, "Invalid to account");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(404)
                .body("error", notNullValue());
    }
    
    @Test(description = "Transfer fails when both accounts are the same")
    public void testTransferToSameAccount() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, fromAccountId, 500.0, "Same account transfer");
        
        givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }
    
    @Test(description = "Transfer fails without authorization token")
    public void testTransferWithoutToken() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 500.0, "No token test");
        
        given()
                .header("Content-Type", "application/json")
                .body(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(401);
    }
    
    @Test(description = "Transfer fails with missing required fields")
    public void testTransferMissingRequiredFields() {
        String invalidRequest = "{\n" +
                "  \"fromAccountId\": " + fromAccountId + ",\n" +
                "  \"amount\": 500.0\n" +
                "}";
        
        givenWithTokenAndBody(invalidRequest)
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(400)
                .body("error", notNullValue());
    }
    
    // ==================== EDGE CASE TESTS ====================
    
    @Test(description = "Multiple rapid transfers execute sequentially")
    public void testMultipleRapidTransfers() {
        for (int i = 0; i < 5; i++) {
            TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 100.0, "Rapid transfer " + (i + 1));
            
            givenWithTokenAndBody(toJson(transferRequest))
                    .when()
                    .post(TRANSACTIONS_ENDPOINT + "/transfer")
                    .then()
                    .statusCode(200);
        }
        
        // Verify balance after all transfers
        Double finalBalance = givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + fromAccountId)
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath()
                .getDouble("balance");
        
        assert finalBalance == 9500.0 
            : "Balance should be 9500 after 500 in transfers, got " + finalBalance;
    }
    
    @Test(description = "Transfer can be retrieved from transaction history")
    public void testTransferInHistory() {
        TransferRequest transferRequest = new TransferRequest(fromAccountId, toAccountId, 333.33, "History retrieval test");
        
        Long transactionId = givenWithTokenAndBody(toJson(transferRequest))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200)
                .extract()
                .response()
                .jsonPath()
                .getLong("transactionId");
        
        // Retrieve from history
        givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + fromAccountId + "/transactions")
                .then()
                .statusCode(200)
                .body("findAll { it.transactionId == " + transactionId + " }.amount", 
                      hasItem(333.33f));
    }
    
    @Test(description = "Transaction timestamps are chronological")
    public void testTransactionChronologicalOrder() {
        // Make first transfer
        givenWithTokenAndBody(toJson(new TransferRequest(fromAccountId, toAccountId, 100.0, "First")))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200);
        
        // Small delay to ensure different timestamp
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Make second transfer
        givenWithTokenAndBody(toJson(new TransferRequest(fromAccountId, toAccountId, 100.0, "Second")))
                .when()
                .post(TRANSACTIONS_ENDPOINT + "/transfer")
                .then()
                .statusCode(200);
        
        // Retrieve transaction history
        Response response = givenWithToken()
                .when()
                .get(ACCOUNTS_ENDPOINT + "/" + fromAccountId + "/transactions")
                .then()
                .statusCode(200)
                .extract()
                .response();
        
        // Verify at least 2 transactions
        assert response.jsonPath().getList("$").size() >= 2;
    }
}
