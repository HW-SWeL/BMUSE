package hwu.elixir.utils;

import org.apache.commons.validator.routines.UrlValidator;

public class Validation {
	
	UrlValidator validator = new UrlValidator();


	/**
	 * Validates a given URL using Apache commons
	 * 
	 * @param uri
	 * @return
	 */
	public boolean validateURI(String uri) {		
		return validator.isValid(uri);		
	}	
}
