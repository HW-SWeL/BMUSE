package hwu.elixir.scrape.exceptions;

public class CannotWriteException extends Exception {

	/**
	 * Cannot write output file.
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public CannotWriteException(String url) {
		super("Cannot write " + url + " to a file!");
	}
}
