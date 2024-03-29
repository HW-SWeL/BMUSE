package hwu.elixir.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
//import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import hwu.elixir.scrape.scraper.ScraperOutput;
import hwu.elixir.scrape.scraper.WebScraper;
import hwu.elixir.utils.Validation;

/**
 * 
 * Basic servlet that provides access to an unfiltered scraper. Scrapes (without
 * processing) a given URL.
 * 
 * Output is NTriples wrapped in JSON.
 * 
 */
@WebServlet(name = "SimpleServlet", urlPatterns = "/getRDF")
public class SimpleServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z");
	private static Logger logger = LoggerFactory.getLogger(SimpleServlet.class.getName());

	//private static final Logger logger = Logger.getLogger(System.class.getName());

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		Map<String, String[]> allParams = request.getParameterMap();
		boolean error = false;
		JSONObject jsonObject = null;
		// get url to scrape parameter
		if (allParams == null || !allParams.containsKey("url")) {
			jsonObject = new JSONObject();
			jsonObject.put("result", "error");
			jsonObject.put("message", "must supply 1 *url* parameter");
			logger.info(jsonObject.toJSONString());
			
			error = true;
		}

		String[] allUrls = allParams.get("url");
		if (allUrls == null || allUrls.length > 1) {
			jsonObject = new JSONObject();
			jsonObject.put("result", "error");
			jsonObject.put("message", "must supply *1* url parameter");
			logger.info(jsonObject.toJSONString());
			
			error = true;
		}
		
		if(error) {
			response.setStatus(400);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json");

			PrintWriter out = response.getWriter();
			out.print(jsonObject.toJSONString());
			out.close();	
		}

		for (String url : allUrls) {
			logger.info("URL to scrape: " + url);
		}

		String url2Scrape = allUrls[0];

		// get output type parameter
		String[] allOutputTypes = allParams.get("output");
		ScraperOutput outputType = ScraperOutput.JSONLD;
		if (allOutputTypes != null) {
			logger.info("Output type requested: " + allOutputTypes[0]);
			
			if(allOutputTypes[0].equalsIgnoreCase("turtle")) outputType = ScraperOutput.TURTLE;
		}
		
		Validation validation = new Validation();

		if (!validation.validateURI(url2Scrape)) {
			jsonObject = new JSONObject();
			jsonObject.put("result", "error");
			jsonObject.put("message", "must supply 1 *valid* url");

			response.setStatus(400);
			response.setHeader("Access-Control-Allow-Origin", "*");
			response.setCharacterEncoding("UTF-8");
			response.setContentType("application/json");

			PrintWriter out = response.getWriter();
			out.print(jsonObject.toJSONString());
			out.close();
			logger.info(jsonObject.toJSONString());
		}

		logger.info("Creating web scraper");
		WebScraper scraper = new WebScraper();
		jsonObject = new JSONObject();
		JSONArray result = new JSONArray();
		boolean success = true;
		try {
			logger.info("Start scrape: " + "TIMESTAMP" + formatter.format(new Date(System.currentTimeMillis())));
			logger.info("URL: " + url2Scrape);
			result = scraper.scrape(url2Scrape, outputType);
			logger.info("End scrape: " + "TIMESTAMP" + formatter.format(new Date(System.currentTimeMillis())));
			//logger.info("RESULT: " + result);
		} catch (Exception e) {
			//logger.severe(e.getMessage());
			logger.error(e.getMessage());
			jsonObject.put("result", "error");
			jsonObject.put("message", e.getMessage());
			success = false;
		}

		if (result == null) {
			//logger.severe("no markup!");
			logger.error("no markup");
			jsonObject.put("result", "error");
			jsonObject.put("message", "cannot find any markup");
			success = false;

		}
		
		if (!success) {
			response.setStatus(500);
		} else {
			jsonObject.put("result", "success");
						
			jsonObject.put("type", outputType.toString());
			jsonObject.put("rdf", result);

			response.setStatus(200);
		}
		response.setHeader("Access-Control-Allow-Origin", "*");
		response.setHeader("Access-Control-Allow-Methods", "GET");
		response.setHeader("Access-Control-Allow-Headers", "*");
		response.setContentType("application/json");
		response.setCharacterEncoding("UTF-8");
		PrintWriter out = response.getWriter();
		out.print(jsonObject.toJSONString());
		out.close();
	}

}