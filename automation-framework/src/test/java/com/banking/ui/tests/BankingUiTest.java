package com.banking.ui.tests;

import com.banking.ui.pages.DashboardPage;
import com.banking.ui.pages.LoginPage;
import com.banking.ui.support.BankingApi;
import com.banking.ui.support.BankingApi.AccountFixture;
import com.banking.ui.support.BankingApi.UserFixture;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testng.Assert;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.math.BigDecimal;

public class BankingUiTest {

    private final BankingApi bankingApi = new BankingApi();
    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage dashboard;

    @BeforeMethod
    public void openBrowser() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--window-size=1440,1600", "--disable-gpu",
                "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        loginPage = new LoginPage(driver).open(System.getProperty("ui.baseUrl", "http://localhost:8080/"));
        dashboard = new DashboardPage(driver);
    }

    @AfterMethod(alwaysRun = true)
    public void closeBrowser(ITestResult result) throws IOException {
        if (driver == null) {
            return;
        }
        if (!result.isSuccess()) {
            Path screenshotDirectory = Path.of("target", "ui-failures");
            Files.createDirectories(screenshotDirectory);
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            String fileName = result.getMethod().getMethodName() + ".png";
            Files.copy(screenshot.toPath(), screenshotDirectory.resolve(fileName),
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
        driver.quit();
        driver = null;
    }

    @Test(description = "Registration through the UI creates a checking account and disables transfers without a second account")
    public void registrationCreatesDefaultCheckingAccount() {
        String email = "ui-register-" + System.nanoTime() + "@example.com";
        loginPage.openRegistration().register("New", "Customer", email, "UiRegister@123");

        Assert.assertTrue(loginPage.isDashboardVisible(), "Registration should open the dashboard");
        Assert.assertEquals(dashboard.firstName(), "New");
        Assert.assertEquals(dashboard.waitForAccountCount("1"), "1");
        Assert.assertTrue(dashboard.accountCards().get(0).contains("CHECKING"));
        Assert.assertTrue(dashboard.transferControlsDisabled(),
                "Transfer controls should remain disabled until a second account exists");
    }

    @Test(description = "A user can sign in and see accounts created through the API")
    public void loginShowsApiCreatedAccounts() {
        UserFixture user = bankingApi.createUser();
        bankingApi.createAccount(user, "SAVINGS", new BigDecimal("425.75"));

        loginPage.login(user.email(), user.password());

        Assert.assertTrue(loginPage.isDashboardVisible(), "Successful login should open the dashboard");
        Assert.assertEquals(dashboard.firstName(), user.firstName());
        Assert.assertEquals(dashboard.waitForAccountCount("2"), "2");
        Assert.assertEquals(dashboard.totalBalance(), "$425.75");
        Assert.assertTrue(dashboard.accountCards().stream().anyMatch(card -> card.contains("SAVINGS")
                && card.contains("$425.75")));
    }

    @Test(description = "Invalid credentials show an error and leave the user on the sign-in screen")
    public void invalidLoginShowsError() {
        UserFixture user = bankingApi.createUser();
        loginPage.login(user.email(), "IncorrectPassword@123");

        Assert.assertTrue(loginPage.waitForToast().contains("Invalid email or password"));
        Assert.assertFalse(loginPage.isDashboardVisible(), "Invalid login must not open the dashboard");
    }

    @Test(description = "A user can open an account from the dashboard")
    public void createAccountFromDashboard() {
        UserFixture user = bankingApi.createUser();
        loginPage.login(user.email(), user.password());
        Assert.assertTrue(loginPage.isDashboardVisible());
        Assert.assertEquals(dashboard.waitForAccountCount("1"), "1");

        dashboard.createAccount("SAVINGS", "150.50");

        Assert.assertEquals(dashboard.waitForAccountCount("2"), "2");
        Assert.assertEquals(dashboard.totalBalance(), "$150.50");
        Assert.assertTrue(dashboard.accountCards().stream()
                .anyMatch(card -> card.contains("SAVINGS") && card.contains("$150.50")));
    }

    @Test(description = "A user can transfer funds and see updated balances and activity")
    public void transferUpdatesBalancesAndActivity() {
        UserFixture user = bankingApi.createUser();
        AccountFixture source = bankingApi.createAccount(user, "CHECKING", new BigDecimal("250.00"));
        AccountFixture destination = bankingApi.createAccount(user, "SAVINGS", new BigDecimal("25.00"));
        loginPage.login(user.email(), user.password());
        Assert.assertTrue(loginPage.isDashboardVisible());
        Assert.assertEquals(dashboard.waitForAccountCount("3"), "3");

        dashboard.transfer(source.id(), destination.id(), "40.00", "UI transfer test");

        Assert.assertEquals(dashboard.totalBalance(), "$275.00");
        Assert.assertTrue(dashboard.accountCardFor(source.id()).contains("$210.00"));
        Assert.assertTrue(dashboard.accountCardFor(destination.id()).contains("$65.00"));
        Assert.assertTrue(dashboard.recentActivity().contains("UI transfer test"));
    }
}
