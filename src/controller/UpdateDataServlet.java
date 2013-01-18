package controller;

import java.io.IOException;
import javax.servlet.http.*;
import net.sf.stackwrap4j.json.JSONException;

@SuppressWarnings("serial")
public class UpdateDataServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {
		long numOfLoaded = 0;
		try {
			numOfLoaded = ManageData.singleUpdateData();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		resp.setContentType("text/html");
		resp.getWriter().println("<style>@import url('css/exampleoverflow.css');#message{margin: 60px 290px;position: absolute;width: 25em;font-size: 22;}</style>");
		resp.getWriter().println("<a href='/'><img id='logo' src='images/exampleoverflow_logo_jquery.png' alt='exampleoverflow'/></a>");
		resp.getWriter().println("<p id='message'>Repository was updated successfully with "+numOfLoaded+" snippets</p>");
	}
}
