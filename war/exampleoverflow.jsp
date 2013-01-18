<%@page import="view.AjaxSearchServlet"%>
<%@page import="com.google.appengine.api.users.UserService"%>
<%@page import="com.google.appengine.api.users.UserServiceFactory"%>
<%@page import="java.security.Principal"%>
<%@ page language="java" contentType="text/html; charset=windows-1255"
	pageEncoding="windows-1255"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta name="description" content="A code repository search engine for code snippets extracted from Stack Overflow." />
<meta http-equiv="Content-Type"
	content="text/javascript; charset=UTF-8">
<title>Example Overflow</title>
<link
	href="css/exampleoverflow.css"
	rel="stylesheet" type="text/css" />
<script
	src="js/jquery-1.8.3.min.js" type="text/javascript" charset="utf-8"></script>
<script
	src="js/jquery-ui-1.9.2.custom.min.js" type="text/javascript" charset="utf-8"></script>
<script src="js/textext-1.3.1.js" type="text/javascript" charset="utf-8"></script>
<script src="js/shCore.js" type="text/javascript" charset="utf-8"></script>
<script src="js/shAutoloader.js" type="text/javascript" charset="utf-8"></script>
<script src='js/shBrushXml.js' type='text/javascript' charset="utf-8"></script>
<script src='js/shBrushJScript.js' type='text/javascript' charset="utf-8"></script>
<script src='js/shBrushCss.js' type='text/javascript' charset="utf-8"></script>
<script src='js/shBrushPhp.js' type='text/javascript' charset="utf-8"></script>


<script>
	var cfg = ($.hoverintent = {
		sensitivity : 7,
		interval : 100
	});

	$.event.special.hoverintent = {
		setup : function() {
			$(this).bind("mouseover", jQuery.event.special.hoverintent.handler);
		},
		teardown : function() {
			$(this).unbind("mouseover",
					jQuery.event.special.hoverintent.handler);
		},
		handler : function(event) {
			event.type = "hoverintent";
			var self = this, args = arguments, target = $(event.target), cX, cY, pX, pY;

			function track(event) {
				cX = event.pageX;
				cY = event.pageY;
			}
			;
			pX = event.pageX;
			pY = event.pageY;
			function clear() {
				target.unbind("mousemove", track).unbind("mouseout",
						arguments.callee);
				clearTimeout(timeout);
			}
			function handler() {
				if ((Math.abs(pX - cX) + Math.abs(pY - cY)) < cfg.sensitivity) {
					clear();
					jQuery.event.handle.apply(self, args);
				} else {
					pX = cX;
					pY = cY;
					timeout = setTimeout(handler, cfg.interval);
				}
			}
			var timeout = setTimeout(handler, cfg.interval);
			target.mousemove(track).mouseout(clear);
			return true;
		}
	};
	
	function showLoadingImage(){
		$('#loading').html("<img src='images/loading.gif' width=25 height=25 />");
	};
	
	function hideLoadingImage(){
		$('#loading').html("")
	};
	
	function showLoadingMoreImage(){
		$('#loadingmore').html("<img src='images/bert.gif' />");
	};
	
	function hideLoadingMoreImage(){
		$('#loadingmore').html("")
	};

	function loadMoreSearchResults(){
	    if( typeof loadMoreSearchResults.nothingToLoad == 'undefined' ) {
	        loadMoreSearchResults.nothingToLoad = false;
	    }
	    if (loadMoreSearchResults.nothingToLoad){
	    	return;
	    }
	    if( typeof loadMoreSearchResults.loadMoreInProgress == 'undefined' ) {
	        loadMoreSearchResults.loadMoreInProgress = false;
	    }
	    if (loadMoreSearchResults.loadMoreInProgress){
	    	return;
	    }
	    loadMoreSearchResults.loadMoreInProgress = true;
	    showLoadingMoreImage();
		var x = document.getElementById("searchResults");
		var str = $('#search').val();
		var prevTags = $('#search').textext()[0].hiddenInput().val();
		var req = new XMLHttpRequest();
	    if( typeof loadMoreSearchResults.counter == 'undefined' ) {
	        loadMoreSearchResults.counter = 1;
	    }
		req.open("GET", "search?q=" + str + "&p=" + loadMoreSearchResults.counter + "&tags=" + prevTags, true);
		loadMoreSearchResults.counter++;
		req.onreadystatechange = function() {
			if (req.readyState == 4) {
				if (req.status == 200) {
					hideLoadingMoreImage();
					printResults(x.innerHTML+req.responseText);
					if (req.responseText==""){
						loadMoreSearchResults.nothingToLoad = true;
					}
					else{
						loadMoreSearchResults.nothingToLoad = false;
					}
					log("loadMoreSearchResults","q="+str+"%26p="+(loadMoreSearchResults.counter-1));
					loadMoreSearchResults.loadMoreInProgress = false;
				}
			}
		};
		req.send();
	};
	
	$(window).scroll(
			function() {
				if ($(window).scrollTop() + 2 >= $(document).height() - $(window).height()) {
					loadMoreSearchResults();
				}
			});
	
	$(document).ready(function() {
		if ($('#search').val() != "") {
			ajaxSearch($('#search').val());
		}
	});
	
	$(document).ready(function() {
		<%
			UserService userService = UserServiceFactory.getUserService();
			Principal principal = request.getUserPrincipal();
			String username = (principal==null)?null:principal.getName();
		%>
		var content = document.getElementById("auth_href");
		var isUserSigned = <%=(principal != null)%>;
		if (isUserSigned){
			content.innerHTML = '<%=username%>' + ' ,Sign out';
			content.href = '<%=UserServiceFactory.getUserService().createLogoutURL("/")%>';
		}
	});
</script>
<script>
	function ajaxSearch(str) {
		var tags = $('#search').textext()[0].hiddenInput().val();
		showLoadingImage();
		var req = new XMLHttpRequest();
		var prevTags = $('#search').textext()[0].hiddenInput().val();
		loadMoreSearchResults.counter = 1;
		req.open("GET", "search?q=" + str + '&tags=' + tags, true);
		req.onreadystatechange = function() {
			if (req.readyState == 4) {
				if (req.status == 200) {
					if(str==$("#search").val()){
						hideLoadingImage();
						//spellcheck(str);
						printResults(req.responseText);
						loadMoreSearchResults.nothingToLoad = false;
						log("search","q="+str);
/////////////////
 						if (($(document).height() <= $(window).height()+2)&&(req.responseText)) {
 							var mat = req.responseText.match(/Found \d+ results/);
 							if (mat!=null){
 								var numResults = parseInt(mat[0].substring(6,mat[0].length-8));
 								if (numResults>5){
 									loadMoreSearchResults();
 								}
 							}
 						}
/////////////////						
					}
				}
			}
		};
		req.send();
	};
	
	function delayedAjaxSearch(str){
		var allowedTags = 'ajax jquery facebook autocomplete javascript';
		var tags = $('#search').textext()[0].hiddenInput().val();
		var i;
		var words = str.toLowerCase().split(' ');
		for (i in words){
			if ((allowedTags.match(words[i])!=null)&&(tags.match(words[i])==null)){
				$('#search').textext()[0].tags().addTags([ words[i] ]);
			}
		}
//		var tags = tagsArray.substring(1,tagsArray.length-1).replace('"','').replace(',','%20');
		setTimeout(function(){
			if(str==$("#search").val()){
				ajaxSearch(str);
				}
			}, 
			200);
	};

	function printResults(results) {
		var x = document.getElementById("searchResults");
		x.innerHTML = results;

		$(function() {
			$(".accordionClass").accordion({
				autoHeight : false,
				event : "hoverintent"
			});

			$(".questionsBody").mouseenter(function(){log("open","question");});
			$(".questions").hide();
			$(".accordionClass").hover(function() {
				var title = $(this).find(".titles").text();
				log("hover",title);
			}, function() {
				$(".accordionClass").accordion('activate', 0);
			});
			$(".accordionClass").hover(function() {
				$(this).find(".questions").show();
			}, function() {
				$(this).find(".questions").hide();
			});
			$(".answersBody").mouseenter(function(){log("open","answer");});
			$(".answers").hide();
			$(".accordionClass").hover(function() {
				$(this).find(".answers").show();
			}, function() {
				$(this).find(".answers").hide();
			});
			
			function path()
			{
			  var args = arguments,
			      result = []
			      ;
			       
			  for(var i = 0; i < args.length; i++)
			      result.push(args[i].replace('@', '/js/'));       
			  return result
			};
			 
			SyntaxHighlighter.autoloader.apply(null, path(
			  'applescript            @shBrushAppleScript.js',
			  'actionscript3 as3      @shBrushAS3.js',
			  'bash shell             @shBrushBash.js',
			  'coldfusion cf          @shBrushColdFusion.js',
			  'cpp c                  @shBrushCpp.js',
			  'c# c-sharp csharp      @shBrushCSharp.js',
			  'css                    @shBrushCss.js',
			  'delphi pascal          @shBrushDelphi.js',
			  'diff patch pas         @shBrushDiff.js',
			  'erl erlang             @shBrushErlang.js',
			  'groovy                 @shBrushGroovy.js',
			  'java                   @shBrushJava.js',
			  'jfx javafx             @shBrushJavaFX.js',
			  'js jscript javascript  @shBrushJScript.js',
			  'perl pl                @shBrushPerl.js',
			  'php                    @shBrushPhp.js',
			  'text plain             @shBrushPlain.js',
			  'py python              @shBrushPython.js',
			  'ruby rails ror rb      @shBrushRuby.js',
			  'sass scss              @shBrushSass.js',
			  'scala                  @shBrushScala.js',
			  'sql                    @shBrushSql.js',
			  'vb vbnet               @shBrushVb.js',
			  'xml xhtml xslt html    @shBrushXml.js'
			));
					 
			SyntaxHighlighter.all();
			
// 			$(".stars-wrapper").stars({
// 				inputType: "select"
// 			});
		});
	};
	
	function spellcheck(query) {
		var req = new XMLHttpRequest();
		req.open("GET", "spellcheck?q=" + query, true);
		req.onreadystatechange = function() {
			if (req.readyState == 4) {
				if (req.status == 200) {
					var x = document.getElementById("spellcheck");
					x.innerHTML = req.responseText;
				}
			}
		};
		req.send();
	};
	
	function log(action, param) {
		var req = new XMLHttpRequest();
		var sid = "<%=session.getId()%>";
		req.open("GET", "http://example-embed.appspot.com/log?action=" + action + "&param=" + param + "&sid=" + sid, true);
		req.send();
	};
</script>
<style>
.accordionClass {
	width: 960px;
}
</style>
</head>

<body style="font-size: 95%;">
	<div id='signin'><a id='auth_href' href="<%=UserServiceFactory.getUserService().createLoginURL("/")%>">Sign in</a></div>
	<a href='/'><img id='logo' src="images/exampleoverflow_logo_jquery.png" alt="exampleoverflow"/></a> 
	<br><br>
	<form method='get'>
		<input type='text' size=110 id='search' name='q' value='<%=(request.getParameter("q")==null)?"":request.getParameter("q")%>'
			onKeyUp="delayedAjaxSearch(this.value);" />
	<div id='loading'></div></form>
	<div id='spellcheck'></div>
	<div id='searchResults'></div>
	<div id='loadingmore'></div>
	<div id='copyright'><p id='innerCopyright'>exampleoverflow &copy; 2012-2013 Alexey Zagalsky; user contributions (stackoverflow.com) licensed under cc-wiki with attribution required</p></div>
	<script type="text/javascript">
		$('#search').textext({
			 plugins : 'filter tags',
			 filterItems : [
			                'jquery','ajax','javascript','facebook','autocomplete'
		        ]
		}).bind('isTagAllowed', function(e, data){
			var tags = $('#search').textext()[0].hiddenInput().val();
	        if(tags.match(data.tag)!=null)
	        {
	            data.result = false;
	        }
	    });
	</script>
</body>
</html>