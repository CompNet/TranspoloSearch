package fr.univavignon.transpolosearch.extraction;

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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

import fr.univavignon.transpolosearch.data.search.SocialSearchResult;
import fr.univavignon.transpolosearch.data.search.SocialSearchResults;
import fr.univavignon.transpolosearch.data.search.WebSearchResults;
import fr.univavignon.transpolosearch.processing.InterfaceRecognizer;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.processing.combiner.straightcombiner.StraightCombiner;
import fr.univavignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univavignon.transpolosearch.search.social.AbstractSocialEngine;
import fr.univavignon.transpolosearch.search.social.FacebookEngine;
import fr.univavignon.transpolosearch.search.web.AbstractWebEngine;
import fr.univavignon.transpolosearch.search.web.BingEngine;
import fr.univavignon.transpolosearch.search.web.GoogleEngine;
import fr.univavignon.transpolosearch.search.web.QwantEngine;
import fr.univavignon.transpolosearch.search.web.YandexEngine;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.file.FileTools;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;

/**
 * This class handles the main search, i.e. it :
 * <ol>
 * 	<li>Search: determines which articles are relevant, using one or several Web search engines.</li>
 * 	<li>Retrieval: retrieves them using our article reader.</li>
 * 	<li>Detection: detects the named entities they mention.</li>
 * 	<li>Save: records the corresponding events.</li>
 * <ol>
 * 
 * @author Vincent Labatut
 */
public class Extractor
{	
	/**
	 * Builds and initializes an extractor object,
	 * using the default parameters. 
	 * <br/>
	 * Override/modify the methods called here, 
	 * in order to change these parameters.
	 * 
	 * @throws ProcessorException
	 * 		Problem while initializing the NER tool. 
	 */
	public Extractor() throws ProcessorException
	{	initDefaultSearchEngines();
		initDefaultSocialEngines();
		initDefaultRecognizer();
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	public static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();

	/////////////////////////////////////////////////////////////////
	// PROCESS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Launches the main search.
	 * 
	 * @param keywords
	 * 		Person we want to look up.
	 * @param website
	 * 		Target site, or {@ode null} to search the whole Web.
	 * @param startDate
	 * 		Start of the period we want to consider, 
	 * 		or {@code null} for no constraint.
	 * @param endDate
	 * 		End of the period we want to consider,
	 * 		or {@code null} for no constraint.
	 * @param searchDate
	 * 		If {@code true}, both dates will be used directly in the Web search.
	 * 		Otherwise, they will be used <i>a posteri</i> to filter the detected events.
	 * 		If one of the dates is {@code null}, this parameter has no effect.
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article,
	 * 		or {@code null} if there's no such constraint.
	 * @param extendedSocialSearch
	 * 		Whether the social media search should retrieve the posts published by the
	 * 		users commenting the posts of interest, for the considered period. If 
	 * 		{@code false}, only the posts on the targeted page and their direct comments
	 * 		are returned. 
	 * 
	 * @throws IOException 
	 * 		Problem accessing the Web or a file.
	 * @throws SAXException 
	 * 		Problem while retrieving a Web page.
	 * @throws ParseException 
	 * 		Problem while retrieving a Web page.
	 * @throws ReaderException 
	 * 		Problem while retrieving a Web page.
	 * @throws ProcessorException 
	 * 		Problem while detecting the entity mentions.
	 */
	public void performExtraction(String keywords, String website, Date startDate, Date endDate, boolean searchDate, String compulsoryExpression, boolean extendedSocialSearch) throws IOException, ReaderException, ParseException, SAXException, ProcessorException
	{	logger.log("Starting the information extraction");
		logger.increaseOffset();
		
		// setup the output folder
		String outFolder = keywords.replace(' ','_');
		FileNames.setOutputFolder(outFolder);
		
		// perform the Web search
//		logger.log("Performing the Web search");
//		performWebExtraction(keywords, website, startDate, endDate, searchDate, compulsoryExpression);
		
		// perform the social search
		logger.log("Performing the social media search");
		performSocialExtraction(keywords, startDate, endDate, compulsoryExpression, extendedSocialSearch);
		
		logger.decreaseOffset();
		logger.log("Information extraction over");
	}

	/**
	 * Launches the main search.
	 * 
	 * @param keywords
	 * 		Person we want to look up.
	 * @param website
	 * 		Target site, or {@ode null} to search the whole Web.
	 * @param startDate
	 * 		Start of the period we want to consider, 
	 * 		or {@code null} for no constraint.
	 * @param endDate
	 * 		End of the period we want to consider,
	 * 		or {@code null} for no constraint.
	 * @param searchDate
	 * 		If {@code true}, both dates will be used directly in the Web search.
	 * 		Otherwise, they will be used <i>a posteri</i> to filter the detected events.
	 * 		If one of the dates is {@code null}, this parameter has no effect.
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article,
	 * 		or {@code null} if there is no such constraint.
	 * 
	 * @throws IOException 
	 * 		Problem accessing the Web or a file.
	 * @throws SAXException 
	 * 		Problem while retrieving a Web page.
	 * @throws ParseException 
	 * 		Problem while retrieving a Web page.
	 * @throws ReaderException 
	 * 		Problem while retrieving a Web page.
	 * @throws ProcessorException 
	 * 		Problem while detecting the entity mentions.
	 */
	private void performWebExtraction(String keywords, String website, Date startDate, Date endDate, boolean searchDate, String compulsoryExpression) throws IOException, ReaderException, ParseException, SAXException, ProcessorException
	{	logger.log("Starting the web extraction");
		logger.increaseOffset();
		
		// perform the Web search
		WebSearchResults results = performWebSearch(keywords, website, startDate, endDate, searchDate);

		// filter Web pages (remove PDFs, and so on)
		results.filterByUrl();
		
		// retrieve the corresponding articles
		results.retrieveArticles();
		results.exportAsCsv(FileNames.FI_SEARCH_RESULTS_URL);
		
		// detect the entity mentions
		results.detectMentions(recognizer);
		
		// possibly filter the articles depending on the dates and compulsory expression
		results.filterByContent(startDate,endDate,searchDate,compulsoryExpression);
		results.exportAsCsv(FileNames.FI_SEARCH_RESULTS_CONTENT);

		// displays the remaining articles with their mentions	//TODO maybe get the entities instead of the mention, eventually?
		results.displayRemainingMentions(); //TODO for debug only
		
		// extract events from the remaining articles and mentions
		boolean bySentence[] = {false,true};
		for(boolean bs: bySentence)
		{	results.extractEvents(bs);
			// export the events as a table
			results.exportEvents(bs);
		}
		
		logger.decreaseOffset();
		logger.log("Web extraction over");
	}
	
	/////////////////////////////////////////////////////////////////
	// PROCESS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Launches the main search.
	 * 
	 * @param keywords
	 * 		Person we want to look up.
	 * @param startDate
	 * 		Start of the period we want to consider, 
	 * 		or {@code null} for no constraint.
	 * @param endDate
	 * 		End of the period we want to consider,
	 * 		or {@code null} for no constraint.
	 * @param extendedSocialSearch
	 * 		Whether the social media search should retrieve the posts published by the
	 * 		users commenting the posts of interest, for the considered period. If 
	 * 		{@code false}, only the posts on the targeted page and their direct comments
	 * 		are returned. 
	 * @param compulsoryExpression
	 * 		String expression which must be present in the groups of posts,
	 * 		or {@code null} if there is no such constraint.
	 * 
	 * @throws IOException 
	 * 		Problem accessing the Web or a file.
	 * @throws SAXException 
	 * 		Problem while retrieving a Web page.
	 * @throws ParseException 
	 * 		Problem while retrieving a Web page.
	 * @throws ReaderException 
	 * 		Problem while retrieving a Web page.
	 * @throws ProcessorException 
	 * 		Problem while detecting the entity mentions.
	 */
	private void performSocialExtraction(String keywords, Date startDate, Date endDate, String compulsoryExpression, boolean extendedSocialSearch) throws IOException, ReaderException, ParseException, SAXException, ProcessorException
	{	logger.log("Starting the social media extraction");
		logger.increaseOffset();
		
		// perform the social search
		boolean includeComments = false;
		SocialSearchResults results = performSocialSearch(keywords, startDate, endDate, extendedSocialSearch, includeComments);
		
		// convert the posts to proper articles
		results.buildArticles(includeComments);
		
		// possibly filter the articles depending on the compulsory expression
//		results.filterByContent(null,null,true,compulsoryExpression);
		
		// detect the entity mentions
		results.detectMentions(recognizer);
		
		// displays the remaining articles with their mentions	//TODO maybe get the entities instead of the mention, eventually?
		results.displayRemainingMentions(); //TODO for debug only
		
		// extract events from the remaining articles and mentions
		boolean bySentence[] = {false,true};
		for(boolean bs: bySentence)
		{	results.extractEvents(bs);
			// export the events as a table
			results.exportEvents(bs);
		}
		
		logger.decreaseOffset();
		logger.log("Social media extraction over");
	}
	
	/////////////////////////////////////////////////////////////////
	// WEB SEARCH	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of engines used for the Web search */
	private final List<AbstractWebEngine> webEngines = new ArrayList<AbstractWebEngine>();
//	/** Comparator used for result keys */
//	private static final Comparator<String> KEY_COMPARATOR = new Comparator<String>() 
//	{	@Override
//		public int compare(String key1, String key2) 
//		{	int result;
//			Integer d1 = null;
//			Integer r1 = null;
//			Integer d2 = null;
//			Integer r2 = null;
//			
//			if(key1.contains("-"))
//			{	String tmp1[] = key1.split("-");
//				d1 = Integer.parseInt(tmp1[0]);
//				r1 = Integer.parseInt(tmp1[1]);
//			}
//			else
//				r1 = Integer.parseInt(key1);
//			
//			if(key2.contains("-"))
//			{	String tmp2[] = key2.split("-");
//				d2 = Integer.parseInt(tmp2[0]);
//				r2 = Integer.parseInt(tmp2[1]);
//			}
//			else
//				r2 = Integer.parseInt(key2);
//			
//			if(d1==null)
//				if(d2==null)
//					result = r1.compareTo(r2);
//				else
//					result = -1;
//			else
//				if(d2==null)
//					result = 1;
//				else
//				{	result = d1.compareTo(d2);
//					if(result==0)
//						result = r1.compareTo(r2);
//				}
//			
//			return result;
//		}
//	};
	
	/**
	 * Initializes the default search engines.
	 * Currently: Google, Bing, Qwant, Yandex.
	 */
	private void initDefaultSearchEngines()
	{	// set up the google custom search
		GoogleEngine googleEngine = new GoogleEngine();
		webEngines.add(googleEngine);
		
		// set up Bing
		BingEngine bingEngine = new BingEngine();
		webEngines.add(bingEngine);
		
		// set up Qwant
		QwantEngine qwantEngine = new QwantEngine();
		webEngines.add(qwantEngine);
		
		// set up Yandex
		YandexEngine yandexEngine = new YandexEngine();
		webEngines.add(yandexEngine);
	}
	
	/**
	 * Performs the Web search using the specified parameters and
	 * each one of the engines registered in the {@link #webEngines}
	 * list.
	 * 
	 * @param keywords
	 * 		Person we want to look for.
	 * @param website
	 * 		Target site, or {@ode null} to search the whole Web.
	 * @param startDate
	 * 		Start of the period we want to consider, 
	 * 		or {@code null} for no constraint.
	 * @param endDate
	 * 		End of the period we want to consider,
	 * 		or {@code null} for no constraint.
	 * @param searchDate
	 * 		If {@code true}, both dates will be used directly in the Web search.
	 * 		Otherwise, they will be used <i>a posteriori</i> to filter the detected events.
	 * 		If one of the dates is {@code null}, this parameter has no effect.
	 * @return
	 * 		List of web search results.
	 * 
	 * @throws IOException
	 * 		Problem accessing the Web.
	 */
	private WebSearchResults performWebSearch(String keywords, String website, Date startDate, Date endDate, boolean searchDate) throws IOException
	{	boolean cachedSearch = true; //TODO for debug
		
		// log search parameters
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("keywords="+keywords);
			logger.log("startDate="+startDate);
			logger.log("endDate="+endDate);
			String txt = "searchDate="+searchDate;
			if(!searchDate)
				txt = txt + "(dates are ignored here, because the search is not strict)";
			logger.log(txt);
		logger.decreaseOffset();
		
		// nullify dates if the search is not strict
		if(!searchDate)
		{	startDate = null;
			endDate = null;
		}
		
		// apply each search engine
		logger.log("Applying iteratively each search engine");
		logger.increaseOffset();
			WebSearchResults result = new WebSearchResults();
			
			for(AbstractWebEngine engine: webEngines)
			{	Map<String,URL> urls;
				
				// possibly use cached results
				String cacheFilePath = FileNames.FO_WEB_SEARCH_RESULTS + File.separator + engine.getName();
				File cacheFolder = new File(cacheFilePath);
				cacheFolder.mkdirs();
				cacheFilePath = cacheFilePath + File.separator + FileNames.FI_SEARCH_RESULTS;
				File cacheFile = new File(cacheFilePath);
				if(cachedSearch && cacheFile.exists())
				{	logger.log("Loading the previous search results from file "+cacheFilePath);
					urls = new HashMap<String,URL>();	
					Scanner sc = FileTools.openTextFileRead(cacheFile,"UTF-8");
					while(sc.hasNextLine())
					{	String line = sc.nextLine();
						String tmp[] = line.split("\t");
						String key = tmp[0].trim();
						String urlStr = tmp[1].trim();
						URL url = new URL(urlStr);
						urls.put(key,url);
					}
					logger.log("Number of URLs loaded: "+urls.size());
				}
				
				// search the results
				else
				{	logger.log("Applying search engine "+engine.getName());
					logger.increaseOffset();
						// apply the engine
						urls = engine.search(keywords,website,startDate,endDate);
						//TODO comment faire pour numéroter chaque sous-recherche indép ? >> faut renvoyer une map au lieu d'une liste
						
						// possibly record its results
						if(cachedSearch)
						{	logger.log("Recording all URLs in text file \""+cacheFilePath+"\"");
							PrintWriter pw = FileTools.openTextFileWrite(cacheFile,"UTF-8");
							for(Entry<String, URL> entry: urls.entrySet())
							{	String key = entry.getKey();
								URL url = entry.getValue();
								pw.println(key+"\t"+url.toString());
							}
							pw.close();
						}
					logger.decreaseOffset();
				}
				
//				// sort the URL keys
//				TreeSet<String> keys = new TreeSet<String>(KEY_COMPARATOR);
//				keys.addAll(urls.keySet());
				
				// add to the overall map of URLs
				String engineName = engine.getName();
				for(Entry<String,URL> entry: urls.entrySet())
				{	String rank = entry.getKey();
					URL url = entry.getValue();
					String urlStr = url.toString();
					result.addResult(urlStr, engineName, rank);
				}
			}
		logger.decreaseOffset();
		logger.log("Total number of pages found: "+result.size());
		
		// record the complete list of URLs (not for cache, just as a result)
		result.exportAsCsv(FileNames.FI_SEARCH_RESULTS_ALL);
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// SOCIAL MEDIA SEARCH	/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of engines used to search social medias */
	private final List<AbstractSocialEngine> socialEngines = new ArrayList<AbstractSocialEngine>();
	
	/**
	 * Initializes the default search engines for social medias.
	 * Currently: only Facebook.
	 */
	private void initDefaultSocialEngines()
	{	// set up Facebook
		try
		{	FacebookEngine facebookEngine = new FacebookEngine();
			socialEngines.add(facebookEngine);
		} 
		catch (FailingHttpStatusCodeException | IOException | URISyntaxException e) 
		{	e.printStackTrace();
		}
	}
	
	/**
	 * Performs the Web search using the specified parameters and
	 * each one of the engines registered in the {@link #socialEngines}
	 * list.
	 * 
	 * @param keywords
	 * 		Person we want to look for.
	 * @param startDate
	 * 		Start of the period we want to consider, 
	 * 		or {@code null} for no constraint.
	 * @param endDate
	 * 		End of the period we want to consider,
	 * 		or {@code null} for no constraint.
	 * @param extendedSearch
	 * 		Whether or not to look for the posts of the commenting users. 
	 * @param includeComments 
	 * 		Whether ({@code true}) or not ({@code false}) to include comments 
	 * 		in the proper article (or just the main post).
	 * @return
	 * 		List of results taking the form of URLs.
	 * 
	 * @throws IOException
	 * 		Problem accessing the Web.
	 */
	private SocialSearchResults performSocialSearch(String keywords, Date startDate, Date endDate, boolean extendedSearch, boolean includeComments) throws IOException
	{	boolean cachedSearch = true; //TODO for debug
		// log search parameters
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("keywords="+keywords);
			logger.log("startDate="+startDate);
			logger.log("endDate="+endDate);
			logger.log("extendedSearch="+extendedSearch);
		logger.decreaseOffset();
		
		// apply each search engine
		logger.log("Applying iteratively each social engine");
		logger.increaseOffset();
			SocialSearchResults result = new SocialSearchResults();
			
			for(AbstractSocialEngine engine: socialEngines)
			{	List<SocialSearchResult> posts = new ArrayList<SocialSearchResult>();
				
				// possibly use cached results
				String cacheFilePath = FileNames.FO_SOCIAL_SEARCH_RESULTS + File.separator + engine.getName();
				File cacheFolder = new File(cacheFilePath);
				cacheFolder.mkdirs();
				cacheFilePath = cacheFilePath + File.separator + FileNames.FI_SEARCH_RESULTS;
				File cacheFile = new File(cacheFilePath);
				if(cachedSearch && cacheFile.exists())
				{	logger.log("Loading the previous search results from file "+cacheFilePath);
					Scanner sc = FileTools.openTextFileRead(cacheFile,"UTF-8");
					while(sc.hasNextLine())
					{	SocialSearchResult post = SocialSearchResult.readFromText(sc);
						posts.add(post);
					}
					logger.log("Number of posts loaded (not counting the comments): "+posts.size());
				}
				
				// search the results
				else
				{	logger.log("Applying search engine "+engine.getName());
					logger.increaseOffset();
						// apply the engine
						posts = engine.search(keywords,startDate,endDate, extendedSearch);
						
						// possibly record its results
						if(cachedSearch)
						{	logger.log("Recording all posts in text file \""+cacheFilePath+"\"");
							PrintWriter pw = FileTools.openTextFileWrite(cacheFile,"UTF-8");
							for(SocialSearchResult post: posts)
								post.writeAsText(pw);
							pw.close();
						}
					logger.decreaseOffset();
				}
				
				// add to the overall result object
				int rank = 1;
				for(SocialSearchResult post: posts)
				{	post.rank = rank;
					post.buildArticle(includeComments);
					result.addResult(post);
					rank++;
				}
			}
		logger.decreaseOffset();
		logger.log("Total number of posts found: "+result.size());
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// ENTITIES		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Tool used to recognize named entity mentions in the text */ 
	private InterfaceRecognizer recognizer;
	
	/**
	 * Initializes the recognizer, 
	 * which will be applied to identify names and dates in
	 * the retrieved articles.
	 * 
	 * @throws ProcessorException
	 * 		Problem while initializing the recognizer. 
	 */
	private void initDefaultRecognizer() throws ProcessorException
	{	recognizer = new StraightCombiner();
		recognizer.setCacheEnabled(true);//TODO false for debugging
	}
}
