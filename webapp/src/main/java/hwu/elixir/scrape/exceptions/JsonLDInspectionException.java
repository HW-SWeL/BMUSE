package hwu.elixir.scrape.exceptions;

public class JsonLDInspectionException extends Exception {

	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public JsonLDInspectionException(String url) {
		super(url + " needs human inspection");
	}
}
