package com.banking.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class DashboardPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public DashboardPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public String firstName() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("firstNameLabel"))).getText();
    }

    public String totalBalance() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("totalBalance"))).getText();
    }

    public List<String> accountCards() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("accountList")));
        return driver.findElements(By.cssSelector("#accountList .account-card"))
                .stream()
                .map(element -> element.getText().replace('\n', ' '))
                .toList();
    }

    public String accountCardFor(long accountId) {
        String accountLabel = "ID " + accountId;
        return wait.until(driver -> driver.findElements(By.cssSelector("#accountList .account-card"))
                .stream()
                .filter(element -> element.getText().contains(accountLabel))
                .map(element -> element.getText().replace('\n', ' '))
                .findFirst()
                .orElse(null));
    }

    public boolean transferControlsDisabled() {
        return !driver.findElement(By.id("fromAccountId")).isEnabled()
                && !driver.findElement(By.id("toAccountId")).isEnabled()
                && !driver.findElement(By.cssSelector("#transferForm button[type='submit']")).isEnabled();
    }

    public void createAccount(String accountType, String initialBalance) {
        new Select(driver.findElement(By.id("accountType"))).selectByValue(accountType);
        driver.findElement(By.id("initialBalance")).clear();
        driver.findElement(By.id("initialBalance")).sendKeys(initialBalance);
        clickWhenVisible(By.cssSelector("#accountForm button[type='submit']"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("toast"), "Your new account is ready."));
    }

    public void transfer(long fromAccountId, long toAccountId, String amount, String note) {
        new Select(driver.findElement(By.id("fromAccountId"))).selectByValue(String.valueOf(fromAccountId));
        new Select(driver.findElement(By.id("toAccountId"))).selectByValue(String.valueOf(toAccountId));
        driver.findElement(By.id("transferAmount")).sendKeys(amount);
        driver.findElement(By.id("transferDescription")).sendKeys(note);
        clickWhenVisible(By.cssSelector("#transferForm button[type='submit']"));
        wait.until(ExpectedConditions.textToBePresentInElementLocated(By.id("toast"), "Transfer complete."));
    }

    public String recentActivity() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("historyList"))).getText();
    }

    public String waitForAccountCount(String expectedCount) {
        return wait.until(ExpectedConditions.textToBe(By.id("accountCount"), expectedCount))
                ? driver.findElement(By.id("accountCount")).getText()
                : "";
    }

    private void clickWhenVisible(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("toast")));
        WebElement button = wait.until(ExpectedConditions.elementToBeClickable(locator));
        new Actions(driver).moveToElement(button).click().perform();
    }
}
