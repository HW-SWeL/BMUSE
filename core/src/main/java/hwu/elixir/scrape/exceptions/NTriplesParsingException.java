package hwu.elixir.scrape.exceptions;

public class NTriplesParsingException extends Exception {

	
	/**
	 * 
	 * Some unknown error with the JSON-LD. Human inspection recommended.
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NTriplesParsingException(String msg) {
		super(msg);
	}
}
