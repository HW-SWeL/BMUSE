package hwu.elixir.scrape.exceptions;

public class SeleniumException extends Exception {

	/**
	 * Selenium has crashed.
	 */
	private static final long serialVersionUID = 1L;
	
	public SeleniumException(String url) {
		super("Selenium crashed for: " + url);
	}
}
