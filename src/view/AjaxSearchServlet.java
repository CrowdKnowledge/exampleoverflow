package view;

import java.io.IOException;
import java.util.Collections;
import java.util.logging.Level;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheFactory;
import net.sf.jsr107cache.CacheManager;
import com.google.appengine.api.search.Cursor;
import com.google.appengine.api.search.Field;
//import com.google.appengine.api.search.MatchScorer;
import com.google.appengine.api.search.Query;
import com.google.appengine.api.search.QueryOptions;
import com.google.appengine.api.search.Results;
import com.google.appengine.api.search.ScoredDocument;
//import com.google.appengine.api.search.SortExpression;
//import com.google.appengine.api.search.SortOptions;

@SuppressWarnings("serial")
public class AjaxSearchServlet extends HttpServlet {
	private static final int NUM_OF_RESULTS_TO_SHOW = 5;
//	private static final float TITLE_BOOSTER = 4.0f;
//	private static final float TAG_BOOSTER = 4.0f;
//	private static final float CODE_BOOSTER = 2.0f;
//	private static final float QUESTION_BOOSTER = 1.0f;
//	private static final float ANSWER_BOOSTER = 1.0f;

	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		/* Get search parameters */
		String searchQuery = req.getParameter("q");
		searchQuery = (searchQuery==null)?"":searchQuery;
		if (searchQuery.matches("\\s*")){
			return;
		}
		String pageIndex = req.getParameter("p");
		int page = (pageIndex==null)?0:Integer.parseInt(pageIndex);
		Cursor nextCursor = (Cursor)req.getSession().getAttribute("cursor");
		if (nextCursor==null && page!=0){
			return;
		}
		if (nextCursor==null || page==0){
			nextCursor = Cursor.newBuilder().build();
		}

		String prevTags = req.getParameter("tags");
		prevTags = (prevTags==null)?"[]":prevTags;
		prevTags = prevTags.substring(1, prevTags.length()-1).replaceAll("\\\"", "").replaceAll(",", " ");

		/* Create Memcache object */
		Cache cache = null;
		try {
			CacheFactory cacheFactory = CacheManager.getInstance().getCacheFactory();
			cache = cacheFactory.createCache(Collections.emptyMap());
		} catch (CacheException e) {
			cache = null;
		}
		//cache.clear();
		Results<ScoredDocument> results = null;
//		if (cache!=null){
//			results = (Results<ScoredDocument>)cache.get(searchQuery+nextCursor.toWebSafeString());
//		}
		if ((cache==null)||(results==null)){
			int limit = NUM_OF_RESULTS_TO_SHOW;
			try {
				// Rather than just using a query we build a search request.
				// This allows us to specify other attributes, such as the
				// number of documents to be returned by search.
//				SortOptions sortOptions = SortOptions.newBuilder()
//					       .setMatchScorer(MatchScorer.newBuilder())
//					       .addSortExpression(SortExpression.newBuilder()
//					           .setExpression(String.format(
//					                   "%s", SortExpression.SCORE_FIELD_NAME))
//					           .setDirection(SortExpression.SortDirection.DESCENDING)
//					           .setDefaultValueNumeric(0))
//					       .build();
				
				Query query = Query.newBuilder()
						.setOptions(QueryOptions.newBuilder()
								.setLimit(limit).
								// for deployed apps, uncomment the line below to demo snippeting.
								// This will not work on the dev_appserver.
								// setFieldsToSnippet("content").
								setCursor(nextCursor).
								//setSortOptions(sortOptions).
								build())
								.build(searchQuery);
				controller.ManageData.LOG.info("Sending query " + query);
				results = controller.ManageData.getIndex().search(query);
				cache.put(searchQuery+nextCursor.toWebSafeString(), results);
			} catch (RuntimeException e) {
				controller.ManageData.LOG.log(Level.SEVERE, "Search with query '" + searchQuery + "' failed", e);
			}
		}
		if (results==null){
			return;
		}
		if ((results.getNumberReturned()>0)&&(page==0)){
			resp.getWriter().println("<p id='resultnum'><font color='#C0C0C0'>Found "+results.getNumberFound()+" results</font></p>");
		}

		for (ScoredDocument scoredDoc : results) {
			resp.getWriter().println("<div class='accordionClass'>");
			resp.getWriter().println("<h3 class='titles'><a href=\""+"http://stackoverflow.com/a/"+scoredDoc.getOnlyField("postID").getAtom()+"\">"+getTags(scoredDoc)+" "+scoredDoc.getOnlyField("title").getText()+/*" ["+e.getViewCount()+" views]*/"</a></h3>");

			resp.getWriter().println("<div oncopy='log(\"copy\",\""+scoredDoc.getOnlyField("postID").getAtom()+"\");'>"+"<pre class='brush: js; gutter: false;toolbar: false; html-script:false;'>"+scoredDoc.getOnlyField("code").getText()+"</pre>");
			//resp.getWriter().println("<form><span id='stars-cap'></span><div class='stars-wrapper'><select name='selrate'><option value='1'>Very poor</option><option value='2'>Not that bad</option><option value='3'>Average</option><option value='4' selected='selected'>Good</option><option value='5'>Perfect</option></select></div></form>");
			resp.getWriter().println("</div>");

			resp.getWriter().println("<h3 class='questions' onclick='log(\"clickURL\",\"question\");'><a href=\""+"http://stackoverflow.com/a/"+scoredDoc.getOnlyField("questionID").getAtom()+"\">Question</a></h3>");
			resp.getWriter().println("<div class='questionsBody'>"+scoredDoc.getOnlyField("questionBody").getText()+"</div>");
			resp.getWriter().println("<h3 class='answers' onclick='log(\"clickURL\",\"answer\");'><a href=\""+"http://stackoverflow.com/a/"+scoredDoc.getOnlyField("postID").getAtom()+"\">Answer</a></h3>");
			resp.getWriter().println("<div class='answersBody'>"+scoredDoc.getOnlyField("body").getText()+"</div>");
			resp.getWriter().println("</div><br>");
		}
		HttpSession session = req.getSession();
		session.setAttribute("cursor", results.getCursor());
	}

	private static String getTags(ScoredDocument scoredDoc){
		StringBuilder output = new StringBuilder("[");
		for (Field field:scoredDoc.getFields()){
			if (field.getName().equals("tag")){
					output.append(field.getAtom()+",");
			}
		}
		output.replace(output.length()-1, output.length(),"]");
		return output.toString();
	}
}

