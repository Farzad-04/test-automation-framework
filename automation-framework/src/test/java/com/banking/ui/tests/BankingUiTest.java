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
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.Reporter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BankingUiTest {

    private final BankingApi bankingApi = new BankingApi();
    private WebDriver driver;
    private LoginPage loginPage;
    private DashboardPage dashboard;
    private static final Path REPORT_SCREENSHOTS = Path.of("target", "e2e-report", "screenshots");

    @BeforeSuite
    public void prepareE2eReport() throws IOException {
        if (Files.exists(REPORT_SCREENSHOTS)) {
            try (var files = Files.walk(REPORT_SCREENSHOTS)) {
                for (Path path : files.sorted(java.util.Comparator.reverseOrder()).toList()) {
                    Files.delete(path);
                }
            }
        }
        Files.createDirectories(REPORT_SCREENSHOTS);
    }

    @BeforeMethod
    public void openBrowser() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1440,1100", "--disable-gpu",
                "--no-sandbox", "--disable-dev-shm-usage");
        if (Boolean.getBoolean("ui.headless")) {
            options.addArguments("--headless=new");
        }
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
            captureScreenshot(result, "failure");
        }
        driver.quit();
        driver = null;
    }

    private void captureScreenshot(ITestResult result, String stepName) throws IOException {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        String fileName = result.getMethod().getMethodName() + "-" + stepName + ".png";
        Files.copy(screenshot.toPath(), REPORT_SCREENSHOTS.resolve(fileName),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        @SuppressWarnings("unchecked")
        List<String> screenshots = (List<String>) result.getAttribute("e2eScreenshots");
        if (screenshots == null) {
            screenshots = new ArrayList<>();
            result.setAttribute("e2eScreenshots", screenshots);
        }
        screenshots.add(fileName);
    }

    @Test(description = "Registration through the UI creates a checking account and disables transfers without a second account")
    public void registrationCreatesDefaultCheckingAccount() throws IOException {
        String email = "ui-register-" + System.nanoTime() + "@example.com";
        loginPage.openRegistration().register("New", "Customer", email, "UiRegister@123");

        Assert.assertTrue(loginPage.isDashboardVisible(), "Registration should open the dashboard");
        Assert.assertEquals(dashboard.firstName(), "New");
        Assert.assertEquals(dashboard.waitForAccountCount("1"), "1");
        Assert.assertTrue(dashboard.accountCards().get(0).contains("CHECKING"));
        Assert.assertTrue(dashboard.transferControlsDisabled(),
                "Transfer controls should remain disabled until a second account exists");
        captureScreenshot(Reporter.getCurrentTestResult(), "registration-dashboard");
    }

    @Test(description = "A user can sign in and see accounts created through the API")
    public void loginShowsApiCreatedAccounts() throws IOException {
        UserFixture user = bankingApi.createUser();
        bankingApi.createAccount(user, "SAVINGS", new BigDecimal("425.75"));

        loginPage.login(user.email(), user.password());

        Assert.assertTrue(loginPage.isDashboardVisible(), "Successful login should open the dashboard");
        captureScreenshot(Reporter.getCurrentTestResult(), "login-dashboard");
        Assert.assertEquals(dashboard.firstName(), user.firstName());
        Assert.assertEquals(dashboard.waitForAccountCount("2"), "2");
        Assert.assertEquals(dashboard.totalBalance(), "$425.75");
        Assert.assertTrue(dashboard.accountCards().stream().anyMatch(card -> card.contains("SAVINGS")
                && card.contains("$425.75")));
    }

    @Test(description = "Invalid credentials show an error and leave the user on the sign-in screen")
    public void invalidLoginShowsError() throws IOException {
        UserFixture user = bankingApi.createUser();
        loginPage.login(user.email(), "IncorrectPassword@123");

        Assert.assertTrue(loginPage.waitForToast().contains("Invalid email or password"));
        Assert.assertFalse(loginPage.isDashboardVisible(), "Invalid login must not open the dashboard");
        captureScreenshot(Reporter.getCurrentTestResult(), "invalid-login");
    }

    @Test(description = "A user can open an account from the dashboard")
    public void createAccountFromDashboard() throws IOException {
        UserFixture user = bankingApi.createUser();
        loginPage.login(user.email(), user.password());
        Assert.assertTrue(loginPage.isDashboardVisible());
        Assert.assertEquals(dashboard.waitForAccountCount("1"), "1");
        captureScreenshot(Reporter.getCurrentTestResult(), "before-account-create");

        dashboard.createAccount("SAVINGS", "150.50");

        Assert.assertEquals(dashboard.waitForAccountCount("2"), "2");
        Assert.assertEquals(dashboard.totalBalance(), "$150.50");
        Assert.assertTrue(dashboard.accountCards().stream()
                .anyMatch(card -> card.contains("SAVINGS") && card.contains("$150.50")));
        captureScreenshot(Reporter.getCurrentTestResult(), "after-account-create");
    }

    @Test(description = "A user can transfer funds and see updated balances and activity")
    public void transferUpdatesBalancesAndActivity() throws IOException {
        UserFixture user = bankingApi.createUser();
        AccountFixture source = bankingApi.createAccount(user, "CHECKING", new BigDecimal("250.00"));
        AccountFixture destination = bankingApi.createAccount(user, "SAVINGS", new BigDecimal("25.00"));
        loginPage.login(user.email(), user.password());
        Assert.assertTrue(loginPage.isDashboardVisible());
        Assert.assertEquals(dashboard.waitForAccountCount("3"), "3");
        captureScreenshot(Reporter.getCurrentTestResult(), "before-transfer");

        dashboard.transfer(source.id(), destination.id(), "40.00", "UI transfer test");

        Assert.assertEquals(dashboard.totalBalance(), "$275.00");
        Assert.assertTrue(dashboard.accountCardFor(source.id()).contains("$210.00"));
        Assert.assertTrue(dashboard.accountCardFor(destination.id()).contains("$65.00"));
        Assert.assertTrue(dashboard.recentActivity().contains("UI transfer test"));
        captureScreenshot(Reporter.getCurrentTestResult(), "after-transfer");
    }
}
