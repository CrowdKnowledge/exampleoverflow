package view;

import java.io.IOException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class SearchSuggestServlet extends HttpServlet {

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		String searchQuery = req.getParameter("q");
		searchQuery = (searchQuery==null)?"":searchQuery;
		if (searchQuery.matches("\\s*")){
			return;
		}
		String[] suggestionArray = null;
		try{
			suggestionArray = getSuggestions(searchQuery);
		}catch (Exception e){
			return;
		}
		if (suggestionArray.length==0){
			return;
		}
		resp.getWriter().print("<p>Did you mean:");
		for (String suggestion:suggestionArray){
			resp.getWriter().print("<a href='/?q="+suggestion+"'>"+suggestion+"</a>");
		}
		resp.getWriter().print("</p>");
	}

	public static String[] getSuggestions(String searchQuery) {
		return null;
	}
}

