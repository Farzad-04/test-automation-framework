package com.banking.ui.support;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class BankingApi {

    private static final String BASE_URL = System.getProperty("api.baseUrl", "http://localhost:8080/api");
    private static final String PASSWORD = "UiTestPassword@123";

    public UserFixture createUser() {
        String email = "ui-" + UUID.randomUUID() + "@example.com";
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "firstName", "UI",
                        "lastName", "Tester",
                        "email", email,
                        "password", PASSWORD))
                .post(BASE_URL + "/auth/register");

        if (response.statusCode() != 201) {
            throw new IllegalStateException("UI test user registration failed (" + response.statusCode() + "): "
                    + response.asString());
        }
        return new UserFixture(email, PASSWORD, response.jsonPath().getString("firstName"),
                response.jsonPath().getString("token"));
    }

    public AccountFixture createAccount(UserFixture user, String accountType, BigDecimal initialBalance) {
        Response response = RestAssured.given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + user.token())
                .body(Map.of("accountType", accountType, "initialBalance", initialBalance))
                .post(BASE_URL + "/accounts");

        if (response.statusCode() != 201) {
            throw new IllegalStateException("UI test account creation failed (" + response.statusCode() + "): "
                    + response.asString());
        }
        return new AccountFixture(response.jsonPath().getLong("accountId"));
    }

    public record UserFixture(String email, String password, String firstName, String token) {
    }

    public record AccountFixture(long id) {
    }
}
