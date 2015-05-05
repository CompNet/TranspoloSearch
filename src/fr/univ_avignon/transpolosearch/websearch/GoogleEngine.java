package fr.univ_avignon.transpolosearch.websearch;

/*
 * TranspoloSearch
 * Copyright 2015 Vincent Labatut
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
 * along with Nerwip - Named Entity Extraction in Wikipedia Pages.  
 * If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.customsearch.Customsearch;
import com.google.api.services.customsearch.model.Result;
import com.google.api.services.customsearch.model.Search;

import fr.univ_avignon.transpolosearch.tools.keys.KeyHandler;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class uses Google to search the Web. More
 * precisely, it uses the Google Custom Search API.
 * <br/>
 * See the public fields for a description of the
 * modifiable search parameters.
 * <br/>
 * This code is inspired by the 
 * <a href="http://weblog4j.com/2014/06/03/having-fun-with-google-custom-search-api-and-java/">weblog4j.com post</a> 
 * of Niraj Singh.
 * 
 * @author Vincent Labatut
 */
public class GoogleEngine extends AbstractEngine
{
	/**
	 * Initializes the object used to search
	 * the Web with the Google Custom Search (GCS) API.
	 */
	public GoogleEngine()
	{	// Set up the HTTP transport and JSON factory
		HttpTransport httpTransport = new NetHttpTransport();
		//JsonFactory jsonFactory = new GsonFactory();
		JsonFactory jsonFactory = GsonFactory.getDefaultInstance();
		
		// builds the builder and set the application name
		//HttpRequestInitializer initializer = (HttpRequestInitializer)new CommonGoogleClientRequestInitializer(API_KEY);
		builder = new Customsearch.Builder(httpTransport, jsonFactory, null);
		builder.setApplicationName(APP_NAME);
	}

	/////////////////////////////////////////////////////////////////
	// DATA			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
//	/** Root of the URL */
//	static final private String GOOGLE_SEARCH_URL = "https://www.googleapis.com/customsearch/v1?";
	/** Name of the GCS application */
	private static final String APP_NAME = "TranspoloSearch";
	/** Name of the API key */
	private static final String API_KEY_NAME = "GoogleProject";
	/** Name of the application key */
	private static final String APP_KEY_NAME = "GoogleEngine";
    /** GCS API key */ 
	private static final String API_KEY = KeyHandler.KEYS.get(API_KEY_NAME);
    /** Application id */
	private static final String APP_KEY = KeyHandler.KEYS.get(APP_KEY_NAME);
	/** Number of results returned for one request */
	private static final long PAGE_SIZE = 10; // max seems to be only 10!
   
	/////////////////////////////////////////////////////////////////
	// PARAMETERS	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
    /** Focus on pages hosted in a certain country */
	public String pageCountry = "countryFR";
	/** Focus on pages in a certain language */
	public String pageLanguage = "lang_fr";
	/** Whether the result should be sorted by date, or not (in this case: by relevance). If {@link #sortByDate} is not {@code null}, only the specified time range is treated. */
	public boolean sortByDate = false;
	/** Date range the search should focus on. It should take the form YYYYMMDD:YYYYMMDD, or {@code null} for no limit. If {@link #sortByDate} is set to {@code false}, this range is ignored. */
	public String dateRange = null;
	/** Maximal number of results (can be less if google doesn't provide) */
	public int resultNumber = 100;

	/////////////////////////////////////////////////////////////////
	// BUILDER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Object used to build GoogleEngine instances */
	private Customsearch.Builder builder;
	
	/////////////////////////////////////////////////////////////////
	// SEARCH		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public List<URL> search(String keyword) throws IOException
	{	List<URL> result = search(keyword, null);
		return result;
	}

	@Override
	public List<URL> search(String keyword, String targetSite)  throws IOException
	{	// perform search
		List<Result> resList = searchGoogle(keyword,targetSite);
	
		// build result list
		List<URL> result = new ArrayList<URL>();
		for(Result res: resList)
		{	// TODO log
			String title = res.getHtmlTitle();
			String urlStr = res.getFormattedUrl();
			URL url = new URL(urlStr);
			result.add(url);
		}
		
		return result;
	}

	/**
	 * Performs a search using Google Custom Search.
	 * The search is performed on the whole Web.
	 * <br/>
	 * See the public fields of this class for a
	 * description of the modifiable search parameters.
	 * 
	 * @param keyword
	 * 		Kewords to search.
	 * @return
	 * 		List of results presented using Google's class.
	 * 
	 * @throws IOException 
	 * 		Problem while searching Google.
	 */
	public List<Result> searchGoogle(String keyword) throws IOException
	{	List<Result> result = searchGoogle(keyword,null);
		return result;
	}
	
	/**
	 * Performs a search using Google Custom Search.
	 * The search is performed only on the specified site.
	 * <br/>
	 * See the public fields of this class for a
	 * description of the modifiable search parameters.
	 * 
	 * @param keyword
	 * 		Kewords to search.
	 * @param targetSite
	 * 		Target site, or {@ode null} to search the whole Web.
	 * @return
	 * 		List of results presented using Google's class.
	 * 
	 * @throws IOException
	 * 		Problem while searching Google.
	 */
	public List<Result> searchGoogle(String keyword, String targetSite)  throws IOException
	{	logger.log("Applying Google Custom Search");
		logger.increaseOffset();
		
		// set up the sort criterion
		String sortCriterion = null;
		if(sortByDate)
		{	sortCriterion = "date";
			if(dateRange!=null)
				sortCriterion = sortCriterion + ":r:"+dateRange;
		}
		
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("keyword="+keyword);
			logger.log("targetSite="+targetSite);
			logger.log("pageCountry="+pageCountry);
			logger.log("pageLanguage="+pageLanguage);
			logger.log("PAGE_SIZE="+PAGE_SIZE);
			logger.log("sortCriterion="+sortCriterion);
		logger.decreaseOffset();

		// init the other variables
		List<Result> result = new ArrayList<Result>();
		long start = 1;
		
		// repeat because of the pagination system
		logger.log("Retrieving the result pages:");
		logger.increaseOffset();
			try
			{	do
				{	logger.log("Starting at position "+start+"/"+resultNumber);
					
					// create the GCS object
					Customsearch customsearch = builder.build();//new Customsearch(httpTransport, jsonFactory,null);
					Customsearch.Cse.List list = customsearch.cse().list(keyword);				
					list.setKey(API_KEY);
					list.setCx(APP_KEY);
					list.setCr(pageCountry);
					list.setLr(pageLanguage);
					list.setNum(PAGE_SIZE);
					if(sortCriterion!=null)
						list.setSort(sortCriterion);
					if(targetSite!=null)
						list.setSiteSearch(targetSite);
					list.setStart(start);
		            
					// send the request
					logger.log("Send request");
					Search search = list.execute();
					
//TODO traiter search pour récupérer les éventuels problèmes, genre 403					
					
					// add the results to the list
					List<Result> res = search.getItems();
					logger.log("Retrieved "+res.size()+"/"+PAGE_SIZE+" items (total: "+result.size()+"/"+resultNumber+")");
					result.addAll(res);
					
					// udpate parameter
					start = start + PAGE_SIZE;
				}
				while(result.size()<resultNumber);
			}
			catch(IOException e)
			{	//e.printStackTrace();
				if(start<resultNumber)
					logger.log("Could not reach the specified number of results ("+result.size()+"/"+resultNumber+")");
			}
			logger.decreaseOffset();
			
			logger.log("Search terminated: "+result.size()+"/"+resultNumber+" results retrieved");
			logger.decreaseOffset();
	        return result;
	}
}
