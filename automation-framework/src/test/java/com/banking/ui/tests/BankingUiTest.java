package com.banking.ui.tests;

import com.banking.ui.pages.DashboardPage;
import com.banking.ui.pages.LoginPage;
import com.banking.ui.support.BankingApi;
import com.banking.ui.support.BankingApi.UserFixture;
import com.banking.ui.support.UiDriverFactory;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
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
        driver = UiDriverFactory.create();
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

}
