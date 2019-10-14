package hwu.elixir.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChromeDriverCreator {
	
	private static final String propertiesFile = "application.properties";
	
	private volatile static WebDriver driver;
	private static ChromeOptions chromeOptions = new ChromeOptions();
	private static Logger logger = LoggerFactory.getLogger(System.class.getName());
	
	static {
		ClassLoader classLoader = ChromeDriverCreator.class.getClassLoader();

		InputStream is = classLoader.getResourceAsStream(propertiesFile);
		if(is == null) {
			logger.error("     Cannot find " + propertiesFile + " file");
			throw new IllegalArgumentException(propertiesFile + "file is not found!");
		}

		Properties prop = new Properties();

		try {
			prop.load(is);
		} catch (IOException e) {
			logger.error("     Cannot load application.properties", e);
			System.exit(0);
		}

		String chromiumDriverLoc  = prop.getProperty("chromiumDriverLocation").trim();
		logger.info("     location of chromiun driver: " + chromiumDriverLoc);
		System.setProperty("webdriver.chrome.driver", chromiumDriverLoc);
		chromeOptions.addArguments("--headless");
	}
	
	
	private ChromeDriverCreator()  {}
	
	
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
}
