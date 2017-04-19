package fr.univavignon.transpolosearch.websearch;

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
 * along with TranspoloSearch. If not, see <http://www.gnu.org/licenses/>.
 */

import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.web.WebTools;
import fr.univavignon.transpolosearch.tools.keys.KeyHandler;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * This class uses Bing to search the Web. More
 * precisely, it uses the Bing Search API v5.
 * <br/>
 * See the public fields for a description of the
 * modifiable search parameters.
 * 
 * @author Vincent Labatut
 */
public class BingEngine extends AbstractEngine
{
	/**
	 * Initializes the object used to search
	 * the Web with the Bing Search API.
	 */
	public BingEngine()
	{	// nothing to do here
	}

	/////////////////////////////////////////////////////////////////
	// SERVICE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Number of results on a page */
	private static final int NBR_RESULTS = 50;
	/** URL of the Bing Search service */
	private static final String SERVICE_URL = "https://api.cognitive.microsoft.com/bing/v5.0/search";
	/** Query to send to the service */
	private static final String SERVICE_PARAM_QUERY = "&q=";
	/** Number of results to return */
	private static final String SERVICE_PARAM_COUNT = "count="+NBR_RESULTS; //max is 50
	/** Number of results to skip */
	private static final String SERVICE_PARAM_OFFSET = "&offset=";
	/** Country/language */
	private static final String SERVICE_PARAM_LANGUAGE = "&mkt=fr-FR";
	/** Response filter */
	private static final String SERVICE_PARAM_FILTER = "&responseFilter=Webpages,News";
	/** Query a specific Website */
	private static final String QUERY_PARAM_WEBSITE = "site:";
//	/** Object used to format dates in the query */
//	private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
//	/** Name of the GCS application */
//	private static final String APP_NAME = "TranspoloSearch";
	/** Name of the first API key */
	private static final String API_KEY1_NAME = "MicrosoftSearch1";
	/** Name of the second API key */
	private static final String API_KEY2_NAME = "MicrosoftSearch2";
    /** First API key */ 
	private static final String API_KEY1 = KeyHandler.KEYS.get(API_KEY1_NAME);
    /** Second API key */
	private static final String APP_KEY2 = KeyHandler.KEYS.get(API_KEY2_NAME);
//	/** Number of results returned for one request */
//	private static final long PAGE_SIZE = 10; // max seems to be only 10!
	/** Maximal number of results (can be less if Bing does not provide) */
	public int MAX_RES_NBR = 100;
	
	/////////////////////////////////////////////////////////////////
	// DATA			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Textual name of this engine */
	private static final String ENGINE_NAME = "Bing Search";

	@Override
	public String getName()
	{	return ENGINE_NAME;
	}
	
	/////////////////////////////////////////////////////////////////
	// PARAMETERS	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
    /** Focus on pages hosted in a certain country */
	public String pageCountry = "countryFR";
	/** Focus on pages in a certain language */
	public String pageLanguage = "lang_fr";
//	/** Whether the result should be sorted by date, or not (in this case: by relevance). If {@link #sortByDate} is not {@code null}, only the specified time range is treated. */
//	public boolean sortByDate = false;
//	/** Date range the search should focus on. It should take the form YYYYMMDD:YYYYMMDD, or {@code null} for no limit. If {@link #sortByDate} is set to {@code false}, this range is ignored. */
//	public String dateRange = null;
	/** Maximal number of results (can be less if google doesn't provide) */
	public int resultNumber = 100;
	
	/////////////////////////////////////////////////////////////////
	// SEARCH		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public List<URL> search(String keywords, String website, Date startDate, Date endDate)  throws IOException
	{	logger.log("Applying Bing Search");
		logger.increaseOffset();
		
		List<URL> result = new ArrayList<URL>();
		
		// init search parameters
		String baseUrl = SERVICE_URL 
				+ SERVICE_PARAM_COUNT 
				+ SERVICE_PARAM_LANGUAGE
				+ SERVICE_PARAM_FILTER;
		String baseQuery = "";
		if(website!=null)
			baseQuery = QUERY_PARAM_WEBSITE + website;
		
		if(startDate!=null && endDate!=null)
		{	Calendar cal = Calendar.getInstance();
	    	cal.setTime(startDate);
	    	int year = cal.get(Calendar.YEAR);
	    	int month = cal.get(Calendar.MONTH) + 1;
	    	int day = cal.get(Calendar.DAY_OF_MONTH);
	    	LocalDate currentLocalDate = LocalDate.of(year, month, day);
	    	cal.setTime(endDate);
	    	year = cal.get(Calendar.YEAR);
	    	month = cal.get(Calendar.MONTH) + 1;
	    	day = cal.get(Calendar.DAY_OF_MONTH);
	    	LocalDate endLocalDate = LocalDate.of(year, month, day);
	    	
	    	// process separately each day of the considered time period
	    	while(currentLocalDate.isBefore(endLocalDate))
	    	{	int lastRes = 0;
	    		
	    		// repeat because of the pagination system
	    		do
	    		{	// setup query
	    			String query = baseQuery 
    					+ " " + currentLocalDate.getDayOfMonth() 
    					+ "/" + currentLocalDate.getMonthValue()
    					+ "/" + currentLocalDate.getYear();
	    			// setup url
	    			String url = baseUrl + SERVICE_PARAM_OFFSET + lastRes
	    				+ SERVICE_PARAM_QUERY + URLEncoder.encode(keywords, "UTF-8");
	    			logger.log("URL: "+url);
	    			
	    			// query the server	
	    			HttpClient httpclient = new DefaultHttpClient();   
	    			HttpGet request = new HttpGet(url);
	    			request.setHeader("Ocp-Apim-Subscription-Key", API_KEY1);
	    			HttpResponse response = httpclient.execute(request);
	    			
	    			// parse the JSON response
	    			String answer = WebTools.readAnswer(response);
					JSONParser parser = new JSONParser();
					JSONObject jsonData = (JSONObject)parser.parse(answer);

					JSONObject property = (JSONObject) jsonData.get("property");
					if(property!=null)
					{	JSONObject notable = (JSONObject) property.get("/common/topic/notable_types");
						JSONArray values = (JSONArray) notable.get("values");
						JSONObject value = (JSONObject)values.get(0);
						result = (String) value.get("id");
					}
					
					
	    			lastRes = lastRes + NBR_RESULTS;
	    		}
	    		while(result.size()<MAX_RES_NBR);
	    		
	    		// deal with the next day
	    		currentLocalDate = currentLocalDate.plusDays(1);
	    	}
			
		}
		
		
		
		
		
		
		
		

		
		
		
		

		// perform search
		List<Result> resList = searchGoogle(keywords,website,sortCriterion);
	
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
		
		logger.log("Search terminated: "+result.size()+"/"+resultNumber+" results retrieved");
		logger.decreaseOffset();
		return result;
	}

	
	/**
	 * Performs a search using Google Custom Search.
	 * The search is performed only on the specified site.
	 * <br/>
	 * See the public fields of this class for a
	 * description of the modifiable search parameters.
	 * 
	 * @param keywords
	 * 		Kewords to search.
	 * @param website
	 * 		Target site, or {@ode null} to search the whole Web.
	 * @param sortCriterion
	 * 		Criterion used for sorting (and possibly a range),
	 * 		or {@code null} to use the default (relevance).
	 * @return
	 * 		List of results presented using Google's class.
	 * 
	 * @throws IOException
	 * 		Problem while searching Google.
	 */
	public List<Result> searchGoogle(String keywords, String website, String sortCriterion)  throws IOException
	{			// init the other variables
		List<Result> result = new ArrayList<Result>();
		long start = 1;
		
		// repeat because of the pagination system
		logger.log("Retrieving the result pages:");
		logger.increaseOffset();
			try
			{	List<Result> response = null;
				do
				{	logger.log("Starting at position "+start+"/"+resultNumber);
					
					// create the GCS object
					Customsearch customsearch = builder.build();//new Customsearch(httpTransport, jsonFactory,null);
					Customsearch.Cse.List list = customsearch.cse().list(keywords);				
					list.setKey(API_KEY1);
					list.setCx(APP_KEY2);
					list.setCr(pageCountry);
					list.setLr(pageLanguage);
					list.setNum(PAGE_SIZE);
					if(sortCriterion!=null)
						list.setSort(sortCriterion);
					if(website!=null)
						list.setSiteSearch(website);
					list.setStart(start);
		            
					// send the request
					logger.log("Send request");
					Search search = list.execute();
					
//TODO traiter search pour récupérer les éventuels problèmes, genre 403					
					
					// add the results to the list
					response = search.getItems();
					if(response==null)
						logger.log("No more result could be retrieved");
					else
					{	logger.log("Retrieved "+response.size()+"/"+PAGE_SIZE+" items (total: "+result.size()+"/"+resultNumber+")");
						result.addAll(response);
					
						// udpate parameter
						start = start + PAGE_SIZE;
					}
				}
				while(result.size()<resultNumber && response!=null);
			}
			catch(IOException e)
			{	//e.printStackTrace();
				if(start<resultNumber)
					logger.log("Could not reach the specified number of results ("+result.size()+"/"+resultNumber+")");
			}
		logger.decreaseOffset();
		
        return result;
	}
}
