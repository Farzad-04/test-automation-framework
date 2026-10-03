package com.banking.ui.cucumber;

import com.banking.ui.pages.DashboardPage;
import com.banking.ui.pages.LoginPage;
import com.banking.ui.support.BankingApi;
import com.banking.ui.support.BankingApi.AccountFixture;
import com.banking.ui.support.BankingApi.UserFixture;
import com.banking.ui.support.UiDriverFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.Assert;

import java.math.BigDecimal;
import java.util.UUID;

public class BankingUiSteps {

    private final BankingApi bankingApi = new BankingApi();
    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage dashboard;
    private UserFixture user;
    private AccountFixture checkingAccount;
    private AccountFixture savingsAccount;

    @Before
    public void openBrowser() {
        driver = UiDriverFactory.create();
        loginPage = new LoginPage(driver).open(System.getProperty("ui.baseUrl", "http://localhost:8080/"));
        dashboard = new DashboardPage(driver);
    }

    @After
    public void closeBrowser(Scenario scenario) {
        if (driver == null) {
            return;
        }
        try {
            if (scenario.isFailed()) {
                scenario.attach(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES),
                        "image/png", "Failure - " + scenario.getName());
            }
        } finally {
            driver.quit();
            driver = null;
        }
    }

    @Given("I open the registration form")
    public void openRegistrationForm() {
        loginPage.openRegistration();
    }

    @When("I register as {string} {string}")
    public void registerCustomer(String firstName, String lastName) {
        String email = "cucumber-" + UUID.randomUUID() + "@example.com";
        loginPage.register(firstName, lastName, email, "CucumberTest@123");
    }

    @Then("the dashboard greets {string}")
    public void dashboardGreetsCustomer(String firstName) {
        Assert.assertTrue(loginPage.isDashboardVisible(), "Registration should open the dashboard");
        Assert.assertEquals(dashboard.firstName(), firstName);
    }

    @Then("the customer has {string} checking account")
    public void customerHasCheckingAccount(String expectedCount) {
        Assert.assertEquals(dashboard.waitForAccountCount(expectedCount), expectedCount);
        Assert.assertTrue(dashboard.accountCards().get(0).contains("CHECKING"));
    }

    @Then("transfers are disabled until another account is opened")
    public void transfersAreDisabledUntilAnotherAccountExists() {
        Assert.assertTrue(dashboard.transferControlsDisabled(),
                "Transfer controls should remain disabled until a second account exists");
    }

    @Given("I am signed in as a customer")
    public void signInAsCustomer() {
        user = bankingApi.createUser();
        loginPage.login(user.email(), user.password());
        Assert.assertTrue(loginPage.isDashboardVisible(), "Successful login should open the dashboard");
    }

    @When("I open a savings account with an opening balance of {string}")
    public void openSavingsAccount(String balance) {
        dashboard.createAccount("SAVINGS", balance);
    }

    @Then("the dashboard shows {string} accounts and a total balance of {string}")
    public void dashboardShowsAccountsAndBalance(String expectedCount, String expectedBalance) {
        Assert.assertEquals(dashboard.waitForAccountCount(expectedCount), expectedCount);
        Assert.assertEquals(dashboard.totalBalance(), expectedBalance);
        Assert.assertTrue(dashboard.accountCards().stream()
                .anyMatch(account -> account.contains("SAVINGS") && account.contains(expectedBalance)));
    }

    @Given("I am signed in with a checking balance of {string} and a savings balance of {string}")
    public void signInWithTwoAccounts(String checkingBalance, String savingsBalance) {
        user = bankingApi.createUser();
        checkingAccount = bankingApi.createAccount(user, "CHECKING", new BigDecimal(checkingBalance));
        savingsAccount = bankingApi.createAccount(user, "SAVINGS", new BigDecimal(savingsBalance));
        loginPage.login(user.email(), user.password());
        Assert.assertTrue(loginPage.isDashboardVisible(), "Successful login should open the dashboard");
    }

    @When("I transfer {string} from checking to savings with note {string}")
    public void transferBetweenAccounts(String amount, String note) {
        dashboard.transfer(checkingAccount.id(), savingsAccount.id(), amount, note);
    }

    @Then("the checking balance is {string} and the savings balance is {string}")
    public void balancesAreUpdated(String expectedCheckingBalance, String expectedSavingsBalance) {
        Assert.assertTrue(dashboard.accountCardFor(checkingAccount.id()).contains(expectedCheckingBalance));
        Assert.assertTrue(dashboard.accountCardFor(savingsAccount.id()).contains(expectedSavingsBalance));
    }

    @Then("the transfer note {string} appears in recent activity")
    public void transferNoteAppears(String note) {
        Assert.assertTrue(dashboard.recentActivity().contains(note));
    }
}
