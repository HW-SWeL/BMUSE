package hwu.elixir.scrape.exceptions;

public class JsonLDInspectionException extends Exception {

	
	/**
	 * 
	 * Some unknown error with the JSON-LD. Human inspection recommended.
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public JsonLDInspectionException(String msg) {
		super(msg);
	}
}
