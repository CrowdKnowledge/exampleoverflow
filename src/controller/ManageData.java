package controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jdo.JDOObjectNotFoundException;
import javax.jdo.PersistenceManager;
import model.Codedata;
import model.UpdateState;
import net.sf.stackwrap4j.StackOverflow;
import net.sf.stackwrap4j.StackWrapper;
import net.sf.stackwrap4j.entities.Answer;
import net.sf.stackwrap4j.entities.Question;
import net.sf.stackwrap4j.json.JSONException;
import net.sf.stackwrap4j.query.SearchQuery;
import net.sf.stackwrap4j.query.SearchQuery.Sort;
import com.google.appengine.api.search.Document;
import com.google.appengine.api.search.Field;
import com.google.appengine.api.search.GetRequest;
import com.google.appengine.api.search.GetResponse;
import com.google.appengine.api.search.Index;
import com.google.appengine.api.search.IndexSpec;
import com.google.appengine.api.search.PutException;
import com.google.appengine.api.search.SearchServiceFactory;
import com.google.appengine.api.search.StatusCode;
import com.google.apphosting.api.ApiProxy.ApiDeadlineExceededException;


public class ManageData {
	
	private static final String API_KEY = "m4Qd_8cYykKi5MBOpwFdSg";
	private static final int MAX_PAGE_SIZE = 100;
	private static final int MAX_RESULTS = 15;
	private static final int NUM_OF_CODE_SNIPPETS_TO_EXTRACT = 1;
	private static double NUM_OF_LINES = 0;
	private static double NUM_OF_SNIPPETS = 0;
	
	public static final Logger LOG = Logger.getLogger(ManageData.class.getName());
	
	public static Index getIndex() {
	    IndexSpec indexSpec = IndexSpec.newBuilder().setName("shared_index").build();
	    return SearchServiceFactory.getSearchService().getIndex(indexSpec);
	}
	
	public static long singleUpdateData() throws IOException, JSONException{		
		PersistenceManager pm = PMF.get().getPersistenceManager();
		
		StackWrapper stackOverflow = new StackOverflow(API_KEY);

		/* create stackoverflow query */
		SearchQuery query = new SearchQuery();
		query.setTags("jquery");
		query.setPageSize(MAX_PAGE_SIZE);
		query.setAutoIncrement(true);
		Sort sort = SearchQuery.Sort.creation();
		String minCreationDate = "";
		
		UpdateState updateState = null;
		try{
			updateState = pm.getObjectById(UpdateState.class,"state");
			minCreationDate = updateState.getLastCreationDate();
		}catch (JDOObjectNotFoundException e){
			updateState = new UpdateState();
		}
		
		Codedata codedata = null;
		try{
			codedata = pm.getObjectById(Codedata.class,"codedata");
			NUM_OF_LINES = codedata.getNumOfLines();
			NUM_OF_SNIPPETS = codedata.getNumOfSnippets();
		}catch (JDOObjectNotFoundException e){
			codedata = new Codedata();
		}
		
		sort.setMax(minCreationDate);
		query.setSort(sort);

		int pageSize = MAX_PAGE_SIZE;
		long countAcceptedAnswers = 0;

		while (pageSize!=0&&countAcceptedAnswers<MAX_RESULTS){
			List<Question> questions = stackOverflow.search(query);
			pageSize = questions.size();
			if (pageSize==0){
				/* if no results found try looking for another batch of 2000 results */			
				query = new SearchQuery();
				query.setTags("jquery");
				query.setPageSize(MAX_PAGE_SIZE);
				query.setAutoIncrement(true);
				sort = SearchQuery.Sort.creation();
				sort.setMax(minCreationDate);
				query.setSort(sort);
				questions = stackOverflow.search(query);
				pageSize = questions.size();
				/* no more results at all */
				if (pageSize==0){
					break;
				}
			}

			/* filter questions without accepted answers */
			List<Integer> acceptedIDs = new LinkedList<Integer>();
			List<Question> tempQuestions = new LinkedList<Question>();
			for (int i=0;i<questions.size();i++){
				int acceptedAnswerID = questions.get(i).getAcceptedAnswerId();
				if(acceptedAnswerID!=-1){
					tempQuestions.add(questions.get(i));
					acceptedIDs.add(acceptedAnswerID);
				}
			}
			questions = tempQuestions;

			/* create an array of accepted answers ids from a list */
			int[] ids = new int[acceptedIDs.size()];
			for (int i=0;i<acceptedIDs.size();i++){
				ids[i] = acceptedIDs.get(i);
			}
			if (ids.length==0){
				continue;
			}
			/* remove Answers without code snippet */
			tempQuestions = new LinkedList<Question>();
			List<Answer> tempAnswers = new LinkedList<Answer>();
			List<Answer> answers = stackOverflow.getAnswersById(ids);
			for (int i=0;i<answers.size();i++){
				if (extractCodeText(answers.get(i).getBody())!=null){
					tempAnswers.add(answers.get(i));
					tempQuestions.add(questions.get(i));
				}
			}
			answers = tempAnswers;
			questions = tempQuestions;

			/* get full questions with body */
			int[] questionIDs = new int[answers.size()];
			for (int i=0;i<answers.size();i++){
				questionIDs[i] = answers.get(i).getQuestionId();
			}
			questions = stackOverflow.getQuestionsById(questionIDs);
			
			for (int i=0;(i<questions.size())&&(countAcceptedAnswers<MAX_RESULTS);i++){
				//Question q = answers.get(i).getParentQuestion();//questions.get(i);
				Answer acceptedAnswer = answers.get(i);
				Question q = getQuestionByAnswer(questions, acceptedAnswer);
				if (q==null){
					continue;
				}
				/* check if we already have it in the datastore */
//				try {
//				    // Query the index.
//				    Results<ScoredDocument> results = getIndex().search("postID:"+acceptedAnswer.getPostId());
//
//				    if (results.iterator().hasNext()){
//				    	continue;
//				    }
//				} catch (SearchException e) {
//				    if (StatusCode.TRANSIENT_ERROR.equals(e.getOperationResult().getCode())) {
//				        // retry
//				    }
//				}
				String code = extractCodeText(acceptedAnswer.getBody());
				Document.Builder docBuilder = Document.newBuilder()
						.setId(Integer.toString(acceptedAnswer.getPostId()))
						.addField(Field.newBuilder().setName("postID").setAtom(Integer.toString(acceptedAnswer.getPostId())))
						.addField(Field.newBuilder().setName("questionID").setAtom(Integer.toString(acceptedAnswer.getQuestionId())))
						.addField(Field.newBuilder().setName("title").setText(acceptedAnswer.getTitle()))
						.addField(Field.newBuilder().setName("body").setText(acceptedAnswer.getBody()))
						.addField(Field.newBuilder().setName("code").setText(code))
						.addField(Field.newBuilder().setName("questionBody").setText(q.getBody()))
						.addField(Field.newBuilder().setName("score").setNumber(acceptedAnswer.getScore()))
						.addField(Field.newBuilder().setName("viewCount").setNumber(acceptedAnswer.getViewCount()));
				List<String> tags = q.getTags();
				if (tags != null) {
					for (String tag:tags) {
						docBuilder.addField(Field.newBuilder().setName("tag")
								.setAtom(tag));
					}
				}

				Document doc = docBuilder.build();
				LOG.info("Adding document:\n" + doc.toString());
				try {
					getIndex().put(doc);
					minCreationDate = Long.toString(acceptedAnswer.getCreationDate()-1);
					countAcceptedAnswers++;
					countCodeLines(code);
				}catch (PutException e) {
				    if (StatusCode.TRANSIENT_ERROR.equals(e.getOperationResult().getCode())) {
				        // retry putting the document
				    }

				}catch (ApiDeadlineExceededException ex){
					return countAcceptedAnswers;
				}catch (RuntimeException e) {
					LOG.log(Level.SEVERE, "Failed to add " + doc, e);
				}
			}
		}
		updateState.setLastCreationDate(minCreationDate);
		pm.makePersistent(updateState);
		codedata.setNumOfLines(NUM_OF_LINES);
		codedata.setNumOfSnippets(NUM_OF_SNIPPETS);
		pm.makePersistent(codedata);
		pm.close();
		return countAcceptedAnswers;
	}
	
	private static Question getQuestionByAnswer(List<Question> questions, Answer answer){
		if (questions==null){
			return null;
		}
		for (Question q:questions){
			if (q.getPostId()==answer.getQuestionId()){
				return q;
			}
		}
		Question alternativeQ;
		try {
			alternativeQ = answer.getParentQuestion();
		} catch (IOException e) {
			return null;
		} catch (JSONException e) {
			return null;
		}
		return alternativeQ;
	}
	
	public static Logger getLogger(){
		return LOG;
	}
	
	public static String extractCodeText(String body){
		String result = "";
		int startIndex = 0;
		int countCodeSnippets = 0;
		while (startIndex<body.length()&&countCodeSnippets<NUM_OF_CODE_SNIPPETS_TO_EXTRACT){
			int beginIndex = body.indexOf("<pre><code>", startIndex)+11;
			if (beginIndex==-1+11){
				break;
			}
			// need to add handling for </pre></code> - currently ignores it
			int endIndex = body.indexOf("</code></pre>", beginIndex);
			if (endIndex==-1){
				break;
			}
			if ((beginIndex<0||endIndex<0)||(beginIndex>endIndex)){
				System.out.println("ERROR: extract code method - begindIndex="+beginIndex+" endIndex="+endIndex);
				System.out.println(body);
				System.out.println("##############");
				break;
			}
			String stringFound = body.substring(beginIndex, endIndex);
			result = result.concat(stringFound);
			countCodeSnippets++;
			startIndex = endIndex;
		}
		if (result.equals("")){
			return null;
		}
		return result;
	}

	private static void countCodeLines(String code) {
		int numOfLines = 1;
		for (char c:code.toCharArray()){
			if (c=='\n'){
				numOfLines++;
			}
		}
		NUM_OF_SNIPPETS++;
		NUM_OF_LINES += numOfLines;
	}
	
	public static long removeDocsFromIndex(){
		long removedDocsCount = 0;
		try {
		    while (true) {
		        List<String> docIds = new ArrayList<String>();
		        // Return a set of document IDs.
		        GetRequest request = GetRequest.newBuilder().setReturningIdsOnly(true).build();
		        GetResponse<Document> response = getIndex().getRange(request);
		        if (response.getResults().isEmpty()) {
		            break;
		        }
		        for (Document doc : response) {
		            docIds.add(doc.getId());
		            removedDocsCount++;
		        }
		        getIndex().delete(docIds);
		    }
		} catch (RuntimeException e) {
		    LOG.log(Level.SEVERE, "Failed to remove documents", e);
		}
		return removedDocsCount;
	}
}