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

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;

import facebook4j.Facebook;
import facebook4j.FacebookException;
import facebook4j.FacebookFactory;
import facebook4j.Post;
import facebook4j.ResponseList;
import facebook4j.auth.AccessToken;
import facebook4j.conf.Configuration;
import facebook4j.conf.ConfigurationBuilder;
import fr.univavignon.transpolosearch.tools.keys.KeyHandler;
import fr.univavignon.transpolosearch.tools.web.WebTools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;

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
	 */
	public FacebookEngine()
	{	ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true);
		cb.setOAuthAppId(KeyHandler.KEYS.get(APP_ID));
		cb.setOAuthAppSecret(KeyHandler.KEYS.get(APP_SECRET));
		cb.setOAuthAccessToken("**************************************************");
		cb.setOAuthPermissions("email, publish_stream, id, name, first_name, last_name, read_stream , generic");
		cb.setUseSSL(true); 
		cb.setJSONStoreEnabled(true);
		Configuration config = cb.build();
		factory = new FacebookFactory(config);
	}

	/////////////////////////////////////////////////////////////////
	// SERVICE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Object used to format dates in the query */
	private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	/** Name of the id of the FB app */
	private static final String APP_ID = "FacebookAppId";
	/** Name of the FB app secret */
	private static final String APP_SECRET = "FacebookAppSecret";
	/** Name of the application key */
	private static final String APP_KEY_NAME = "GoogleEngine";
	/** Number of results returned for one request */
	private static final long PAGE_SIZE = 10; // max seems to be only 10!
   
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
	public List<URL> search(String keywords, String website, Date startDate, Date endDate)  throws IOException
	{	logger.log("Searching Facebook");
		logger.increaseOffset();
		
		String query = "";
		
		Facebook facebook = factory.getInstance();
		try
		{	ResponseList<Post> results =  facebook.getPosts(query);
			for(Post post: results)
			{	String msg = post.getMessage();
				
			}
		} 
		catch (FacebookException e) 
		{	e.printStackTrace();
			throw new IOException(e.getMessage());
		}
		
		
		
		
		
		
		// init GCS parameters
		String sortCriterion;
		if(startDate==null && endDate==null)
		{	sortCriterion = null;
		}
		else if(startDate!=null && endDate!=null)
		{	String dateRange = DATE_FORMAT.format(startDate)+":"+DATE_FORMAT.format(endDate);
			sortCriterion = "date:r:" + dateRange;
		}
		else
		{	logger.log("WARNING: one date is null, so we ignore both dates in the search");
			sortCriterion = null;
		}
		
		// display GCS parameters
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("keywords="+keywords);
			logger.log("website="+website);
			logger.log("pageCountry="+PAGE_CNTRY);
			logger.log("pageLanguage="+PAGE_LANG);
			logger.log("PAGE_SIZE="+PAGE_SIZE);
			logger.log("resultNumber="+MAX_RES_NBR);
			logger.log("sortCriterion="+sortCriterion);
		logger.decreaseOffset();

		// perform search
		List<Result> resList = searchFacebook(keywords,website,sortCriterion);
	
		// convert result list
		logger.log("Results obtained:");
		logger.increaseOffset();
		List<URL> result = new ArrayList<URL>();
		for(Result res: resList)
		{	String title = res.getHtmlTitle();
			String urlStr = res.getLink();
			logger.log(title+" - "+urlStr);
			URL url = new URL(urlStr);
			result.add(url);
		}
		logger.decreaseOffset();
		
		logger.log("Search terminated: "+result.size()+"/"+MAX_RES_NBR+" results retrieved");
		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Performs a search on Facebook using its API.
	 * <br/>
	 * See the public fields of this class for a
	 * description of the modifiable search parameters.
	 * 
	 * @param keywords
	 * 		Keywords to search.
	 * @param website
	 * 		Target site, or {@code null} to search the whole Web.
	 * @param sortCriterion
	 * 		Criterion used for sorting (and possibly a range),
	 * 		or {@code null} to use the default (relevance).
	 * @return
	 * 		List of results.
	 * 
	 * @throws IOException
	 * 		Problem while searching Facebook.
	 */
	public List<Result> searchFacebook(String keywords, String website, String sortCriterion)  throws IOException
	{	// init the other variables
		List<Result> result = new ArrayList<Result>();
		long start = 1;
		
		// repeat because of the pagination system
		logger.log("Retrieving the result pages:");
		logger.increaseOffset();
			try
			{	List<Result> response = null;
				do
				{	logger.log("Starting at position "+start+"/"+MAX_RES_NBR);
					
					// create the GCS object
					Customsearch customsearch = builder.build();//new Customsearch(httpTransport, jsonFactory,null);
					Customsearch.Cse.List list = customsearch.cse().list(keywords);				
					list.setKey(API_KEY);
					list.setCx(APP_KEY);
					list.setCr(PAGE_CNTRY);
					list.setLr(PAGE_LANG);
					list.setNum(PAGE_SIZE);
					if(sortCriterion!=null)
						list.setSort(sortCriterion);
					if(website!=null)
						list.setSiteSearch(website);
					list.setStart(start);
		            
					// send the request
					logger.log("Send request");
					Search search = list.execute();
					
//TODO handle search in order to catch possible problems, such as 403					
					
					// add the results to the list
					response = search.getItems();
					if(response==null)
						logger.log("No more result could be retrieved");
					else
					{	logger.log("Retrieved "+response.size()+"/"+PAGE_SIZE+" items (total: "+result.size()+"/"+MAX_RES_NBR+")");
						result.addAll(response);
					
						// udpate parameter
						start = start + PAGE_SIZE;
					}
				}
				while(result.size()<MAX_RES_NBR && response!=null);
			}
			catch(IOException e)
			{	//e.printStackTrace();
				if(start<MAX_RES_NBR)
					logger.log("Could not reach the specified number of results ("+result.size()+"/"+MAX_RES_NBR+")");
			}
		logger.decreaseOffset();
		
        return result;
	}
	public static void main(String[] args) throws Exception
	{	String url = "https://www.facebook.com/v2.9/dialog/oauth?client_id=437488563263592&redirect_uri="+URLEncoder.encode("https://www.facebook.com/connect/login_success.html","UTF-8"); 
		HttpClient httpclient = new DefaultHttpClient();
		HttpClientParams.setRedirecting(httpclient.getParams(), false);
		HttpGet request = new HttpGet(url);
		HttpResponse response = httpclient.execute(request);
		int responseCode = response.getStatusLine().getStatusCode();
		System.out.println(responseCode);
		
		url = response.getLastHeader("Location").getValue();
		System.out.println(url);
		HttpClient httpclient2 = new DefaultHttpClient();
		HttpClientParams.setRedirecting(httpclient2.getParams(), false);
		request = new HttpGet(url);
		response = httpclient2.execute(request);
		
		
//		URLConnection con = new URL( url ).openConnection();
//		System.out.println( "orignal url: " + con.getURL() );
//		con.connect();
//		System.out.println( "connected url: " + con.getURL() );
//		InputStream is = con.getInputStream();
//		System.out.println( "redirected url: " + con.getURL() );
//		is.close();
		
		
		
//		System.out.println(URLDecoder.decode("/login.php?skip_api_login=1&amp;api_key=437488563263592&amp;signed_next=1&amp;next=https%3A%2F%2Fwww.facebook.com%2Fv2.9%2Fdialog%2Foauth%3Fredirect_uri%3Dhttps%253A%252F%252Fwww.facebook.com%252Fconnect%252Flogin_success.html%26client_id%3D437488563263592%26ret%3Dlogin%26logger_id%3D959c8cc5-cfb7-0729-835f-b52bf88829af&amp;cancel_url=https%3A%2F%2Fwww.facebook.com%2Fconnect%2Flogin_success.html%3Ferror%3Daccess_denied%26error_code%3D200%26error_description%3DPermissions%2Berror%26error_reason%3Duser_denied%23_%3D_&amp;display=page&amp;locale=fr_FR&amp;logger_id=959c8cc5-cfb7-0729-835f-b52bf88829af&amp;_fb_noscript=1"));
		
		// parse the JSON response
//		String answer = WebTools.readAnswer(response);
//		System.out.println(answer);
		
	}
}
