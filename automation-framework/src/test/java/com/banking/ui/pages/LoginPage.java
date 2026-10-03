package com.banking.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

public class LoginPage {

    private final WebDriver driver;
    private final WebDriverWait wait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    public LoginPage open(String baseUrl) {
        driver.get(baseUrl);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("loginForm")));
        return this;
    }

    public LoginPage openRegistration() {
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("[data-tab='register']"))).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#registerForm.active")));
        return this;
    }

    public void register(String firstName, String lastName, String email, String password) {
        type(By.id("registerFirstName"), firstName);
        type(By.id("registerLastName"), lastName);
        type(By.id("registerEmail"), email);
        type(By.id("registerPassword"), password);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#registerForm button[type='submit']"))).click();
    }

    public void login(String email, String password) {
        type(By.id("loginEmail"), email);
        type(By.id("loginPassword"), password);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#loginForm button[type='submit']"))).click();
    }

    public String waitForToast() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("toast"))).getText();
    }

    public boolean isDashboardVisible() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.visibilityOfElementLocated(By.id("dashboard"))).isDisplayed();
        } catch (org.openqa.selenium.TimeoutException exception) {
            return false;
        }
    }

    private void type(By locator, String value) {
        WebElement field = wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        field.clear();
        field.sendKeys(value);
    }
}
