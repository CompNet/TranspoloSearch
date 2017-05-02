package fr.univavignon.transpolosearch.search.web;

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

import fr.univavignon.transpolosearch.tools.web.WebTools;
import fr.univavignon.transpolosearch.tools.keys.KeyHandler;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * This class uses Bing to search the Web. More
 * precisely, it uses the Bing Search API v5.
 * 
 * @author Vincent Labatut
 */
public class BingEngine extends AbstractWebEngine
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
	/** Number of results returned for one request (max: 50) */
	private static final int PAGE_SIZE = 50;
	/** URL of the Bing Search service */
	private static final String SERVICE_URL = "https://api.cognitive.microsoft.com/bing/v5.0/search";
	/** Query to send to the service */
	private static final String SERVICE_PARAM_QUERY = "&q=";
	/** Number of results to return */
	private static final String SERVICE_PARAM_COUNT = "?count="+PAGE_SIZE;
	/** Number of results to skip */
	private static final String SERVICE_PARAM_OFFSET = "&offset=";
	/** Country/language */
	private static final String SERVICE_PARAM_LANGUAGE = "&mkt="+BingEngine.PAGE_LANG+"-"+BingEngine.PAGE_CNTRY;
	/** Response filter */
	private static final String SERVICE_PARAM_FILTER = "&responseFilter=Webpages,News";
	/** Query a specific Website */
	private static final String QUERY_PARAM_WEBSITE = "site:";
	/** Object used to format dates */
	private final static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-mm-dd");
	/** Name of the first API key */
	private static final String API_KEY1_NAME = "MicrosoftSearch1";
//	/** Name of the second API key */
//	private static final String API_KEY2_NAME = "MicrosoftSearch2";
    /** First API key */ 
	private static final String API_KEY1 = KeyHandler.KEYS.get(API_KEY1_NAME);
//    /** Second API key */
//	private static final String APP_KEY2 = KeyHandler.KEYS.get(API_KEY2_NAME);
	
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
	public static final String PAGE_CNTRY = "FR";
	/** Focus on pages in a certain language */
	public static final String PAGE_LANG = "fr";
	/** Maximal number of results (can be less if Bing does not provide) */
	public int MAX_RES_NBR = 200;
	
	/////////////////////////////////////////////////////////////////
	// SEARCH		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public List<URL> search(String keywords, String website, Date startDate, Date endDate)  throws IOException
	{	logger.log("Applying Bing Search");
		logger.increaseOffset();
		List<URL> result = new ArrayList<URL>();
		
		// init search parameters
		logger.log("Keywords: "+keywords);
		String baseUrl = SERVICE_URL 
				+ SERVICE_PARAM_COUNT 
				+ SERVICE_PARAM_LANGUAGE
				+ SERVICE_PARAM_FILTER;
		String baseQuery = "";
		if(website==null)
			logger.log("No website specified");
		else
		{	logger.log("Search restricted to website: "+website);
			baseQuery = QUERY_PARAM_WEBSITE + website + " " ;
		}
		baseQuery = baseQuery + keywords;
		
		// if there is a time range constraint
		if(startDate!=null && endDate!=null)
		{	logger.log("Dates detected: "+startDate+"-"+endDate);
			logger.increaseOffset();
			
			Calendar cal = Calendar.getInstance();
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
	    	while(currentLocalDate.isBefore(endLocalDate) || currentLocalDate.isEqual(endLocalDate))
	    	{	logger.log("Processing date "+currentLocalDate);
				try
	    		{	searchBing(baseUrl, baseQuery, currentLocalDate, result);
	    		}
	    		catch(ParseException e)
		    	{	e.printStackTrace();
	    			throw new IOException(e.getMessage());	
		    	}
	    		
	    		// deal with the next day
	    		currentLocalDate = currentLocalDate.plusDays(1);
	    	}
			logger.decreaseOffset();
		}
		
		// if there is no time constraint
		else
		{	logger.log("No date detected");
			
			try
			{	searchBing(baseUrl, baseQuery, null, result);
			}
			catch(ParseException e)
	    	{	e.printStackTrace();
				throw new IOException(e.getMessage());	
	    	}
		}
		
		logger.log("Search terminated: "+result.size()+"/"+MAX_RES_NBR+" results retrieved");
		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Invokes Bing Search API using the specified search parameters,
	 * and completes the list of results accordingly.
	 * 
	 * @param baseUrl
	 * 		Base URL (to be completed).
	 * @param baseQuery
	 * 		Base query (to be completed, too).
	 * @param targetedDate
	 * 		Date of the searched articles (can be {@code null}, if none).
	 * @param result
	 * 		Current list of URL.
	 * 
	 * @throws ClientProtocolException
	 * 		Problem while invoking Bing.
	 * @throws IOException
	 * 		Problem while invoking Bing.
	 * @throws ParseException
	 * 		Problem while parsing Bing JSON results.
	 */
	private void searchBing(String baseUrl, String baseQuery, LocalDate targetedDate, List<URL> result) throws ClientProtocolException, IOException, ParseException
	{	// repeat because of the pagination system
		int lastRes = 0;
		do
		{	logger.log("Getting results "+lastRes+"-"+(lastRes+PAGE_SIZE-1));
			
			// setup query
			String query = baseQuery;
			if(targetedDate!=null)
				query = query
				+ " " + String.format("%02d",targetedDate.getDayOfMonth()) 
				+ "/" + String.format("%02d",targetedDate.getMonthValue())
				+ "/" + targetedDate.getYear();
			logger.log("Query: \""+query+"\"");
			// setup url
			String url = baseUrl + SERVICE_PARAM_OFFSET + lastRes
				+ SERVICE_PARAM_QUERY + URLEncoder.encode(query, "UTF-8");
			logger.log("URL: "+url);
			
			// query the server
			HttpClient httpclient = HttpClientBuilder.create().build();
			HttpGet request = new HttpGet(url);
			request.setHeader("Ocp-Apim-Subscription-Key", API_KEY1);
			HttpResponse response = httpclient.execute(request);
			
			// parse the JSON response
			String answer = WebTools.readAnswer(response);
			JSONParser parser = new JSONParser();
			JSONObject jsonData = (JSONObject)parser.parse(answer);
			
			// web results
			{	JSONObject webRes = (JSONObject)jsonData.get("webPages");
				if(webRes==null)
					logger.log("WARNING: could not find any web results for this query");
				else
				{	JSONArray valueArray = (JSONArray)webRes.get("value");
					logger.log("Found "+valueArray.size()+" web results for this query");
					logger.increaseOffset();
					int i = 1;
					for(Object val: valueArray)
					{	logger.log("Processing web result "+i+"/"+valueArray.size());
						logger.increaseOffset();
						JSONObject value = (JSONObject)val;
						String urlStr = (String)value.get("url");
						logger.log("url: "+urlStr);
						URL resUrl = new URL(urlStr);
						result.add(resUrl);
						logger.decreaseOffset();
						i++;
					}
					logger.increaseOffset();
				}
			}

			// news results
			{	JSONObject newsRes = (JSONObject)jsonData.get("news");
				if(newsRes==null)
					logger.log("WARNING: could not find any news results for this query");
				else
				{	JSONArray valueArray = (JSONArray)newsRes.get("value");
					logger.log("Found "+valueArray.size()+" news results for this query");
					logger.increaseOffset();
					int i = 1;
					for(Object val: valueArray)
					{	logger.log("Processing news result "+i+"/"+valueArray.size());
						logger.increaseOffset();
						JSONObject value = (JSONObject)val;
						String urlStr = (String)value.get("url");
						logger.log("url: "+urlStr);
						String dateStr = (String)value.get("datePublished");
						dateStr = dateStr.substring(0,10); // yyyy-mm-dd = 10 chars
						logger.log("date: "+dateStr);
						boolean keepArticle = true;
						if(dateStr!=null && targetedDate!=null)
						{	LocalDate artDate = LocalDate.parse(dateStr, DATE_FORMATTER);
							keepArticle = artDate.equals(targetedDate);
						}
						if(keepArticle)
						{	URL resUrl = new URL(urlStr);
							result.add(resUrl);
							logger.log("No publication date, or equal to the targeted date >> keeping the article");
						}
						else
							logger.log("The article publication date is not compatible with the targeted date >> article ignored");
						logger.decreaseOffset();
						i++;
					}
					logger.decreaseOffset();
				}
			}
			
			// go to next result page
			lastRes = lastRes + PAGE_SIZE;
		}
		while(result.size()<MAX_RES_NBR);
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
	{	
//		BingEngine engine = new BingEngine();
//		
//		String keywords = "François Hollande";
//		String website = null;//"http://lemonde.fr";
//		Date startDate = new GregorianCalendar(2016,3,1).getTime();//null;
//		Date endDate = new GregorianCalendar(2016,3,2).getTime();//null;
//		
//		List<URL> result = engine.search(keywords, website, startDate, endDate);
//		
//		System.out.println(result);
		
		
		// check the URL returned by Bing
//		String urlStr = "http://www.bing.com/cr?IG=C688D700F2FC417AA9B10AA9F7337042&CID=24569A2FBE076C2425F49044BFE06DBD&rd=1&h=VUF8nVTYmDh6zzfje1tbK4pq9WLYMHZZsZtHW0Y5jI0&v=1&r=http%3a%2f%2fwww.closermag.fr%2farticle%2ffrancois-hollande-danse-avec-barack-obama-et-devient-la-risee-de-twitter-photo-604494&p=DevEx,5093.1";
//		HttpClient httpclient = new DefaultHttpClient();   
//		HttpGet request = new HttpGet(urlStr);
//		HttpResponse response = httpclient.execute(request);
//		String answer = WebTools.readAnswer(response);
//		PrintWriter pw = FileTools.openTextFileWrite(FileNames.FO_OUTPUT+File.separator+"test.html");
//		pw.print(answer);
//		pw.close();
		
		System.out.println();
	}
}