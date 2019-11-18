package hwu.elixir.scrape.exceptions;

public class MissingHTMLException extends Exception {

	/**
	 * No HTML obtained.
	 */
	private static final long serialVersionUID = 1L;
	
	public MissingHTMLException(String url) {
		super("Cannot retrieve HTML for " + url);
	}
}
