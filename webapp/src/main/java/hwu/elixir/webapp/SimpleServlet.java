package hwu.elixir.webapp;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONObject;

import hwu.elixir.utils.Validation;



@WebServlet(name = "SimpleServlet", urlPatterns = "/getRDF")
public class SimpleServlet extends HttpServlet {
	
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
 
		Map<String, String[]> allParams = request.getParameterMap();
		
		if(!allParams.containsKey("url")) {
			JSONObject json = new JSONObject();
			json.put("result", "error");
			json.put("message", "must supply 1 *url* parameter");
			
			response.setStatus(400);
			response.setHeader("Access-Control-Allow-Origin", "*");
			
			PrintWriter out = response.getWriter();
			out.print(json.toJSONString());
			out.close();
		}
		
		String[] allUrls = allParams.get("url");
		if(allUrls.length > 1) {
			JSONObject json = new JSONObject();
			json.put("result", "error");
			json.put("message", "must supply *1* url parameter");
			
			response.setStatus(400);
			response.setHeader("Access-Control-Allow-Origin", "*");
			
			PrintWriter out = response.getWriter();
			out.print(json.toJSONString());
			out.close();
		}
		
		String url2Scrape = allUrls[0];
		
		Validation validation = new Validation();
		if(!validation.validateURI(url2Scrape)) {
			JSONObject json = new JSONObject();
			json.put("result", "error");
			json.put("message", "must supply 1 *valid* url");
			
			response.setStatus(400);
			response.setHeader("Access-Control-Allow-Origin", "*");
			
			PrintWriter out = response.getWriter();
			out.print(json.toJSONString());
			out.close();			
		}
		
		
		try(PrintWriter out = response.getWriter()) {
			// scrape 
			
			out.println("Inside doGet");
		}
		
    }
	
}