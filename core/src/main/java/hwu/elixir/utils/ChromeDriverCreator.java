package hwu.elixir.utils;

import org.openqa.selenium.NoSuchSessionException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Creates and managers the chromium driver for Selenium.
 * Based on singleton pattern
 *
 */
public class ChromeDriverCreator {
	
	private volatile static WebDriver driver = null;
	private static ChromeOptions chromeOptions = new ChromeOptions();
	private static Logger logger = LoggerFactory.getLogger(ChromeDriverCreator.class.getName());
	
	static {
		ScraperProperties prop = ScraperProperties.getInstance();
		String chromiumDriverLoc  = prop.getChromiumDriverLocation().trim();
		logger.info("Location of chromiun driver: " + chromiumDriverLoc);
		System.setProperty("webdriver.chrome.driver", chromiumDriverLoc);
		chromeOptions.addArguments("--headless");
	}
	
	
	private ChromeDriverCreator()  {}
	
	/**
	 * Get a pointer to the active WebDriver. If non exists, it creates one.
	 * 
	 * @return
	 */
	public static synchronized WebDriver getInstance() {
		if(driver == null) {
			synchronized (ChromeDriverCreator.class) {				
				if(driver == null) {
					logger.info("starting a new chrome driver");
					driver = new ChromeDriver(chromeOptions);					
				}
			}
		}
		return driver;
	}
	
	/**
	 * Use only to recover from {@link NoSuchSessionException}
	 * To properly shutdown use close() or quit() on the {@link WebDriver}
	 * close() shuts the current page; quit() closes then exits the driver.
	 * 
	 * This method provides a more extreme close and reopen.
	 * 
	 * @return New WebDriver
	 */
	public static synchronized WebDriver killAndReopen() {
		driver = null;
		return getInstance();
	}
}
