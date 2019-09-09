package hwu.elixir.scrape.exceptions;

public class MissingContextException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	
	public MissingContextException(String url) {
		super(url + " appears to not use rdfa, but no context is found. So no markup?");		
	}
}
