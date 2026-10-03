package com.banking.ui.support;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;

public final class UiDriverFactory {

    private UiDriverFactory() {
    }

    public static WebDriver create() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--window-size=1440,1100", "--disable-gpu",
                "--no-sandbox", "--disable-dev-shm-usage");
        if (Boolean.getBoolean("ui.headless")) {
            options.addArguments("--headless=new");
        }
        WebDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        return driver;
    }
}
