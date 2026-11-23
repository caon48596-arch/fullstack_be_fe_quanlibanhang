package com.example.demo.System_test;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.HttpURLConnection;
import java.net.URI;
import java.time.Duration;

public abstract class BaseSystemTest {

    protected static final String BASE_URL = System.getProperty("frontend.base.url", "http://localhost:3000");
    protected WebDriver driver;
    protected WebDriverWait wait;

    @BeforeAll
    public static void setUpDriverBinary() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setUpBrowser() {
        // Bỏ qua System Test nếu Frontend Next.js chưa chạy để không làm fail build
        Assumptions.assumeTrue(isFrontendAvailable(),
                "Bỏ qua System Test: Máy chủ Frontend Next.js hiện chưa chạy tại " + BASE_URL);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDownBrowser() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected static boolean isFrontendAvailable() {
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(BASE_URL).toURL().openConnection();
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            return code >= 200 && code < 500;
        } catch (Exception e) {
            return false;
        }
    }

    protected WebElement waitForVisibility(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected void click(By locator) {
        waitForClickable(locator).click();
    }

    protected void type(By locator, String text) {
        WebElement element = waitForVisibility(locator);
        element.clear();
        element.sendKeys(text);
    }

    protected void setAuthTokenInStorage(String token) {
        if (driver instanceof JavascriptExecutor js) {
            js.executeScript("sessionStorage.setItem('accessToken', arguments[0]);" +
                    "localStorage.setItem('accessToken', arguments[0]);", token);
        }
    }

    protected void clearAuthTokenInStorage() {
        if (driver instanceof JavascriptExecutor js) {
            js.executeScript("sessionStorage.removeItem('accessToken');" +
                    "localStorage.removeItem('accessToken');");
        }
    }
}
