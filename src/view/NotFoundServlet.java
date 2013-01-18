package view;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class NotFoundServlet extends HttpServlet {

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		resp.getWriter().println("<style>@import url('css/exampleoverflow.css');#message{margin: 60px 338px;position: absolute;width: 18em;font-size: 22;}</style>");
		resp.getWriter().println("<a href='/'><img id='logo' src='images/exampleoverflow_logo_jquery.png' alt='exampleoverflow'/></a>");
		resp.getWriter().println("<p id='message'>The requested page was not found (404)</p>");
	}
}

