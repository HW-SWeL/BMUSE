package hwu.elixir.utils;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChromeDriverFactory {
	private volatile static WebDriver driver;
	private static ChromeOptions chromeOptions = new ChromeOptions();
	private static Logger logger = LoggerFactory.getLogger(System.class.getName());
	
	static {
		System.setProperty("webdriver.chrome.driver", "/Users/kcm/Applications/chromedriver");
		chromeOptions.addArguments("--headless");
	}
	
	
	private ChromeDriverFactory()  {}
	
	
	public static synchronized WebDriver getInstance() {
		if(driver == null) {
			synchronized (ChromeDriverFactory.class) {				
				if(driver == null) {
					logger.info("starting a new chrome driver");
					driver = new ChromeDriver(chromeOptions);					
				}
			}
		}
		return driver;
	}
}
