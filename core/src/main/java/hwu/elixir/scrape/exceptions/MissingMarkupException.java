package hwu.elixir.scrape.exceptions;

public class MissingMarkupException extends Exception {

	/**
	 * No HTML obtained.
	 */
	private static final long serialVersionUID = 1L;
	
	public MissingMarkupException(String url) {
		super("Cannot retrieve markup from " + url);
	}
}
