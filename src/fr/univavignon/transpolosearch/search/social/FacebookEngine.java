package fr.univavignon.transpolosearch.search.social;

/*
 * TranspoloSearch
 * Copyright 2015-17 Vincent Labatut
 * 
 * This file is part of TranspoloSearch.
 * 
 * TranspoloSearch is free software: you can redistribute it and/or modify it under 
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation, either version 2 of the License, or (at your option) any later version.
 * 
 * TranspoloSearch is distributed in the hope that it will be useful, but WITHOUT ANY 
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A 
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with TranspoloSearch. If not, see <http://www.gnu.org/licenses/>.
 */

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import facebook4j.Category;
import facebook4j.Comment;
import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.PagableList;
import facebook4j.Page;
import facebook4j.Paging;
import facebook4j.Post;
import facebook4j.Reading;
import facebook4j.ResponseList;
import facebook4j.conf.Configuration;
import facebook4j.conf.ConfigurationBuilder;
import fr.univavignon.transpolosearch.tools.keys.KeyHandler;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class uses the Facebook API to search the Web.
 * <br/>
 * Resources: 
 * http://facebook4j.github.io/en/index.html#source_code
 * http://facebook4j.github.io/en/code-examples.html
 * https://developers.facebook.com/docs/graph-api/using-graph-api
 * http://stackoverflow.com/questions/13165589/facebook-api-access-with-username-password-via-client-software#
 * 
 * @author Vincent Labatut
 */
public class FacebookEngine extends AbstractSocialEngine
{
	/**
	 * Initializes the object used to search Facebook.
	 * 
	 * @throws IOException
	 * 		Problem while logging in Facebook. 
	 * @throws MalformedURLException 
	 * 		Problem while logging in Facebook. 
	 * @throws FailingHttpStatusCodeException 
	 * 		Problem while logging in Facebook. 
	 * @throws URISyntaxException 
	 * 		Problem while logging in Facebook. 
	 */
	public FacebookEngine() throws FailingHttpStatusCodeException, MalformedURLException, IOException, URISyntaxException
	{	// logging in and getting the access token
		String login = KeyHandler.KEYS.get(USER_LOGIN);
		String pwd = KeyHandler.KEYS.get(USER_PASSWORD);	
		String accessToken = getAccessToken(login,pwd);
		
		// setting up the FB session
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		cb.setOAuthAppId(KeyHandler.KEYS.get(APP_ID));
		cb.setOAuthAppSecret(KeyHandler.KEYS.get(APP_SECRET));
		cb.setOAuthAccessToken(accessToken);
		cb.setOAuthPermissions("email, publish_stream, id, name, first_name, last_name, read_stream , generic");
		cb.setAppSecretProofEnabled(true);
		cb.setUseSSL(true); 
		cb.setJSONStoreEnabled(true);
		Configuration config = cb.build();
		factory = new FacebookFactory(config);
	}

	/////////////////////////////////////////////////////////////////
	// SERVICE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Name of the FB user login */
	private static final String URL_LOGIN = "https://www.facebook.com/v2.9/dialog/oauth?client_id=437488563263592&response_type=token&redirect_uri=";
	/** Redirection URL used during login */
	private static final String URL_REDIRECT = "https://www.facebook.com/connect/login_success.html";
	/** Redirection URL used during login */
	private static final String URL_PARAM_ACCESS = "access_token=";
	/** Name of the FB user login */
	private static final String USER_LOGIN = "FacebookUserLogin";
	/** Name of the FB user password */
	private static final String USER_PASSWORD = "FacebookUserPassword";
//	/** Object used to format dates in the query */
//	private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	/** Name of the id of the FB app */
	private static final String APP_ID = "FacebookAppId";
	/** Name of the FB app secret */
	private static final String APP_SECRET = "FacebookAppSecret";
//	/** Number of results returned for one request */
//	private static final long PAGE_SIZE = 10; // max seems to be only 10!
	
	/**
	 * Log in Facebook, using the specified user login and password.
	 * <br/>
	 * This method is adapted from the StackOverflow post by Nicola Marcacci Rossi:
	 * http://stackoverflow.com/a/13214455/1254730
	 * 
	 * @param username
	 * 		Login of the user.
	 * @param password
	 * 		Password associated to the user login.
	 * @return
	 * 		Access token.
	 * 
	 * @throws FailingHttpStatusCodeException
	 * 		Problem while logging.
	 * @throws MalformedURLException
	 * 		Problem while logging.
	 * @throws IOException
	 * 		Problem while logging.
	 * @throws URISyntaxException 
	 * 		Problem while logging.
	 */
	private static String getAccessToken(String username, String password) throws FailingHttpStatusCodeException, MalformedURLException, IOException, URISyntaxException 
	{	logger.log("Logging in Facebook");
		logger.increaseOffset();
		
		logger.log("Initializing Web client");
		WebClient wc = new WebClient();
		WebClientOptions opt = wc.getOptions();
		opt.setCssEnabled(false);
		opt.setJavaScriptEnabled(false);
		
		// go to the FB homepage
		logger.log("Going to FB connection page");
		String url = URL_LOGIN+URLEncoder.encode(URL_REDIRECT,"UTF-8"); 
		HtmlPage page = wc.getPage(url);
		HtmlForm form = (HtmlForm) page.getElementById("login_form");
		
		// setup the login and password
		form.getInputByName("email").setValueAttribute(username);
		form.getInputByName("pass").setValueAttribute(password);
		
		// search the ok button and click
		logger.log("Entering user info and connecting");
		HtmlButton button = form.getButtonByName("login");
		button.click();
		
		// get the redirected page
		logger.log("Following redirection");
		HtmlPage currentPage = (HtmlPage) wc.getCurrentWindow().getEnclosedPage();
		URL currentUrl = currentPage.getUrl();
		String newUrl = currentUrl.toString();
		logger.log(newUrl);
		int startIdx = newUrl.indexOf(URL_PARAM_ACCESS);
		int endIdx = newUrl.indexOf("&", startIdx+1);
		if(endIdx==-1)
			endIdx = newUrl.length();
		String result = newUrl.substring(startIdx+URL_PARAM_ACCESS.length(),endIdx);
		logger.log("Access token: "+result);
	    
		wc.close();
		logger.decreaseOffset();
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// DATA			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Textual name of this engine */
	private static final String ENGINE_NAME = "Facebook Search";

	@Override
	public String getName()
	{	return ENGINE_NAME;
	}
	
	/////////////////////////////////////////////////////////////////
	// PARAMETERS	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
    /** Focus on pages hosted in a certain country */
	public static final String PAGE_CNTRY = "countryFR";
	/** Focus on pages in a certain language */
	public static final String PAGE_LANG = "lang_fr";
//	/** Whether the result should be sorted by date, or not (in this case: by relevance). If {@link #sortByDate} is not {@code null}, only the specified time range is treated. */
//	public boolean sortByDate = false;
//	/** Date range the search should focus on. It should take the form YYYYMMDD:YYYYMMDD, or {@code null} for no limit. If {@link #sortByDate} is set to {@code false}, this range is ignored. */
//	public String dateRange = null;
	/** Maximal number of results (can be less if facebook does not provide) */
	public static final int MAX_RES_NBR = 100;

	/////////////////////////////////////////////////////////////////
	// BUILDER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Object used to build Facebook instances */
	private FacebookFactory factory;
	
	/////////////////////////////////////////////////////////////////
	// SEARCH		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public List<String> search(String keywords, Date startDate, Date endDate)  throws IOException
	{	logger.log("Searching Facebook");
		logger.increaseOffset();
		List<String> result = new ArrayList<String>();
		
		String query = keywords;
		Reading reading = null;
		if(startDate!=null && endDate!=null)
		{	reading = new Reading();
			reading.since(startDate);
			reading.until(endDate);
		}
		
		Facebook facebook = factory.getInstance();
		try
		{	logger.log("Look for the FB page corresponding to \""+keywords+"\"");
			logger.increaseOffset();
			ResponseList<Page> pages =  facebook.searchPages(query);
			if(pages.isEmpty())
				logger.log("No page found at all");
			else
			{	logger.log("Found at least "+pages.size()+" pages: using the first one");
				Page firstPage = pages.get(0);
				String pageId = firstPage.getId();
				logger.log("Title="+firstPage.getName()+", id="+pageId);
				
				// get all the posts of the targeted page
				List<Post> pagePosts = getPosts(pageId, facebook, reading);
				
		        // get the comments associated to all the targeted posts
				List<Comment> pageComments = new ArrayList<Comment>();
				logger.log("Retrieving the comments for each post (for the specified period, if any)");
				logger.increaseOffset();
				for(Post post: pagePosts)
				{	// get the message text
					String msg = post.getMessage();
					logger.log("Message: \""+msg+"\"");
					result.add(msg);
					
					// retrieve the comments associated to the message
					List<Comment> comments = getComments(post, facebook, reading);
					pageComments.addAll(comments);
				}
				logger.decreaseOffset();
				
				// get the authors of all the comments
				Set<String> authorIds = new TreeSet<String>();
				logger.log("Retrieving the authors for each comment");
				logger.increaseOffset();
				for(Comment comment: pageComments)
				{	// add the comment content to the result list
					String msg = comment.getMessage();
					result.add(msg);
					
					// get the author
					Category auth = comment.getFrom();
					String authId = auth.getId();
					if(!authorIds.contains(authId))
					{	String authName = auth.getName();
						logger.log(authName+" ("+authId+")");
						authorIds.add(authId);
					}
				}
				logger.decreaseOffset();
				
				// get the authors posts for this period
				logger.log("Retrieving the posts of the authors (for the specified period, if any)");
				logger.increaseOffset();
				for(String authId: authorIds)
				{	logger.log("Processing id"+authId);
					logger.increaseOffset();
					List<Post> authPosts = getPosts(authId, facebook, reading);
					for(Post post: authPosts)
					{	// get the message text
						String msg = post.getMessage();
						logger.log("Message: \""+msg+"\"");
						result.add(msg);
						
						// we do not get the comments, this time
					}
					logger.decreaseOffset();
				}
				logger.decreaseOffset();
			}
			
			logger.decreaseOffset();
		} 
		catch (FacebookException e) 
		{	//System.err.println(e.getMessage());
			e.printStackTrace();
			throw new IOException(e.getMessage());
		}
		
		// remove the line breaks
		List<String> temp = new ArrayList<String>();
		for(String msg: result)
		{	msg = msg.replaceAll("\\s+", " ");
			temp.add(msg);
		}
		result = temp;
		
		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Gets all the posts for the specified page or user.
	 *  
	 * @param id
	 * 		Id of the page or user of of interest.
	 * @param facebook
	 * 		Current Facebook instance.
	 * @param reading
	 * 		Graph API reading options (in our case: dates).
	 * @return
	 * 		The list of all posts associated to the page or user.
	 * 
	 * @throws FacebookException
	 * 		Problem while accessing the comments.
	 */
	private List<Post> getPosts(String id, Facebook facebook, Reading reading) throws FacebookException
	{	logger.log("Retrieving the posts of the FB page/user of id "+id);
		logger.increaseOffset();
		
		List<Post> result = new ArrayList<Post>();
		Paging<Post> paging;
		
		int i = 1;
		ResponseList<Post> postPage = facebook.getPosts(id,reading);
        do 
        {	logger.log("Processing post page #"+i);
			logger.increaseOffset();
        	i++;
        	
        	// add the post of the current page to the overall list
        	logger.log("Found "+postPage.size()+" posts in the current page");
        	for(Post post: postPage)
        		logger.log(post.getCreatedTime()+": "+post.getMessage());
        	result.addAll(postPage);
        	
        	// try to get the next page of posts
			paging = postPage.getPaging();
            postPage = null;
            if(paging!=null)
            {	logger.log("Getting the next page of posts");
            	postPage = facebook.fetchNext(paging);
            }
			logger.decreaseOffset();
        } 
        while(postPage!= null);
        
		logger.log("Total posts found for the targeted page/user: "+result.size());
		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Gets all the comments for the specified post.
	 *  
	 * @param post
	 * 		Post of interest.
	 * @param facebook
	 * 		Current Facebook instance.
	 * @param reading
	 * 		Graph API reading options (in our case: dates).
	 * @return
	 * 		The list of all comments associated to the post.
	 * 
	 * @throws FacebookException
	 * 		Problem while accessing the comments.
	 */
	private List<Comment> getComments(Post post, Facebook facebook, Reading reading) throws FacebookException 
	{	logger.log("Retrieving comments");
		logger.increaseOffset();
		
		List<Comment> result = new ArrayList<Comment>();
		Paging<Comment> paging;
		
		// get the first page of comments
        int i = 1;
//        PagableList<Comment> commentPage = post.getComments();
        String postId = post.getId();
        PagableList<Comment> commentPage = facebook.getPostComments(postId, reading);
        do 
        {	logger.log("Comment page #"+i);
			logger.increaseOffset();
			i++;
			
			// add the comments of the current result page to the result list
        	result.addAll(commentPage);
        	logger.log("Found "+commentPage.size()+" comments on the current result page");
        	List<String> commentsStr = new ArrayList<String>();
        	for(Comment comment: commentPage)
        		commentsStr.add(comment.getMessage());
			logger.log(commentsStr);
        	
			// TODO we could go and fetch the answers to these comments, but that does not seem necessary 
			
        	// try to get the next page of comments
            paging = commentPage.getPaging();
            commentPage = null;
            if(paging!=null)
            {	logger.log("Getting the next result page");
        		commentPage = facebook.fetchNext(paging);
            }
            logger.decreaseOffset();
		} 
        while(commentPage!= null);
		logger.log("Total comments found for this post: "+result.size());
	    
		logger.decreaseOffset();
	    return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// TEST			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Method used to test/debug this class.
	 * 
	 * @param args
	 * 		None needed.
	 * @throws Exception
	 * 		All exceptions are thrown.
	 */
	public static void main(String[] args) throws Exception
	{	Date startDate = new GregorianCalendar(2017,4,7).getTime();//new GregorianCalendar(2017,3,6).getTime();//null;
		Date endDate = new GregorianCalendar(2017,4,8).getTime();//new GregorianCalendar(2017,3,10).getTime();//null;
		FacebookEngine fe = new FacebookEngine();
		fe.search("François Hollande", startDate, endDate);
	}
}
