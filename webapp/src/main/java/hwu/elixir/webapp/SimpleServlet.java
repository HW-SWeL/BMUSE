package hwu.elixir.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import hwu.elixir.scrape.exceptions.FourZeroFourException;
import hwu.elixir.scrape.exceptions.JsonLDInspectionException;
import hwu.elixir.scrape.exceptions.MissingHTMLException;
import hwu.elixir.scrape.exceptions.SeleniumException;
import hwu.elixir.scrape.scraper.WebScraper;
import hwu.elixir.utils.Validation;



@WebServlet(name = "SimpleServlet", urlPatterns = "/getRDF")
public class SimpleServlet extends HttpServlet {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private static final Logger logger = Logger.getLogger(System.class.getName());

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
 
		Map<String, String[]> allParams = request.getParameterMap();
						
		if(allParams == null || !allParams.containsKey("url")) {
			JSONObject json = new JSONObject();
			json.put("result", "error");
			json.put("message", "must supply 1 *url* parameter");
			
			response.setStatus(400);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");
			
			PrintWriter out = response.getWriter();
			out.print(json.toJSONString());
			out.close();
			logger.info(json.toJSONString());
		}
		
		String[] allUrls = allParams.get("url");
		if(allUrls == null || allUrls.length > 1) {
			JSONObject json = new JSONObject();
			json.put("result", "error");
			json.put("message", "must supply *1* url parameter");
			
			response.setStatus(400);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");
			
			PrintWriter out = response.getWriter();
			out.print(json.toJSONString());
			out.close();
			logger.info(json.toJSONString());
		}
		
		for(String url : allUrls) {
			logger.info(url);
		}
		
		
		String url2Scrape = allUrls[0];
		
		Validation validation = new Validation();
		if(!validation.validateURI(url2Scrape)) {
			JSONObject json = new JSONObject();
			json.put("result", "error");
			json.put("message", "must supply 1 *valid* url");
			
			response.setStatus(400);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");
			
			PrintWriter out = response.getWriter();
			out.print(json.toJSONString());
			out.close();			
			logger.info(json.toJSONString());
		}
		
		logger.info("About to create scraper");		
		WebScraper scraper = new WebScraper();
		JSONObject json = new JSONObject();
		JSONArray result = new JSONArray();
		try {
			logger.info("About to scrape");	
			result = scraper.scrape(url2Scrape);
			logger.info("Scraped");	
		} catch (Exception e) {			
			e.printStackTrace();
			json.put("result", "error");
			json.put("message", e.getMessage());
			
			response.setStatus(500);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");
			
			PrintWriter out = response.getWriter();
			out.print(json.toJSONString());
			out.close();
		} 
		
		if(result == null) {
			json.put("result", "error");
			json.put("message", "cannot find any markup");
			
			response.setStatus(500);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");
			
			PrintWriter out = response.getWriter();

			out.print(json.toJSONString());
			out.close();
		}

		json.put("result", "success");
		json.put("type", "n3");		
		json.put("rdf", result);
		
		response.setStatus(200);
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setCharacterEncoding("UTF-8");
		
		PrintWriter out = response.getWriter();

		out.print(json.toJSONString());
		out.close();		
    }
	
}