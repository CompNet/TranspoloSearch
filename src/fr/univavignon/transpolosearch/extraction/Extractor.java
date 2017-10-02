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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.search.AbstractSearchResults;
import fr.univavignon.transpolosearch.data.search.CombinedSearchResults;
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
	{	initRecognizer();
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
	 * @param websites
	 * 		List of targeted sites, including {@ode null} to search the whole Web.
	 * @param additionalSeeds
	 * 		List of secondary pages to process during the search on social media. 
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
	 * @param language
	 * 		Language targeted during the search.
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
	public void performExtraction(String keywords, List<String> websites, List<String> additionalSeeds, Date startDate, Date endDate, boolean searchDate, String compulsoryExpression, boolean extendedSocialSearch, ArticleLanguage language) throws IOException, ReaderException, ParseException, SAXException, ProcessorException
	{	logger.log("Starting the information extraction");
		logger.increaseOffset();
		
		// setup the output folder
		String outFolder = keywords.replace(' ','_');
		if(websites.size()==1)
		{	String website = websites.get(0);
			if(websites.get(0)!=null)
			{	URL url = new URL(website);
				String host = url.getHost();
				outFolder = outFolder + "_" + host;
			}
		}
		FileNames.setOutputFolder(outFolder);
		
		// perform the Web search
		logger.log("Performing the Web search");
		WebSearchResults webRes = performWebExtraction(keywords, websites, startDate, endDate, searchDate, compulsoryExpression, language);
		
		// perform the social search
		logger.log("Performing the social media search");
		SocialSearchResults socialRes = performSocialExtraction(keywords, additionalSeeds, startDate, endDate, compulsoryExpression, extendedSocialSearch, language);
		
		// merge results and continue processing
		logger.log("Merging web and social media results");
		combineResults(webRes, socialRes, language);
		
		logger.decreaseOffset();
		logger.log("Information extraction over");
	}

	/////////////////////////////////////////////////////////////////
	// WEB SEARCH			/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of engines used for the Web search */
	private final List<AbstractWebEngine> webEngines = new ArrayList<AbstractWebEngine>();
	
	/**
	 * Initializes the default search engines.
	 * Currently: Google, Bing, Qwant, Yandex.
	 * 
	 * @param websites
	 * 		List of target websites, can contain {@code null} to search the whole Web.
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
	 */
	private void initWebSearchEngines(List<String> websites, Date startDate, Date endDate, boolean searchDate)
	{	logger.log("Initializing the Web search engines");
		logger.increaseOffset();
		
		// nullify dates if the search is not strict
		if(!searchDate)
		{	startDate = null;
			endDate = null;
		}
		
		// iterate over each website
		int i = 1;
		for(String website: websites)
		{	logger.log("Website "+website+" ("+i+"/"+websites.size()+")");
			i++;
			
			// set up the Google
			GoogleEngine googleEngine = new GoogleEngine(website,startDate,endDate);
			webEngines.add(googleEngine);
			
			// set up Bing
			BingEngine bingEngine = new BingEngine(website,startDate,endDate);
			webEngines.add(bingEngine);
			
			// set up Qwant
			QwantEngine qwantEngine = new QwantEngine(website,startDate,endDate);
			webEngines.add(qwantEngine);
			
			// set up Yandex
			YandexEngine yandexEngine = new YandexEngine(website,startDate,endDate);
			webEngines.add(yandexEngine);
		}
		
		logger.decreaseOffset();
	}
	
	/**
	 * Performs the Web search using the specified parameters and
	 * each one of the engines registered in the {@link #webEngines}
	 * list.
	 * 
	 * @param keywords
	 * 		Person we want to look for.
	 * @return
	 * 		List of web search results.
	 * 
	 * @throws IOException
	 * 		Problem accessing the Web.
	 */
	private WebSearchResults performWebSearch(String keywords) throws IOException
	{	WebSearchResults result = new WebSearchResults();
		
		// apply each search engine
		logger.log("Applying iteratively each search engine");
		logger.increaseOffset();
		for(AbstractWebEngine engine: webEngines)
		{	logger.log("Processing search engine "+engine);
			logger.increaseOffset();
				Map<String,URL> urls = engine.retrieveResults(keywords);
			
//				// sort the URL keys
//				TreeSet<String> keys = new TreeSet<String>(KEY_COMPARATOR);
//				keys.addAll(urls.keySet());
				
				// add to the overall map of URLs
				String engineStr = engine.toString();
				for(Entry<String,URL> entry: urls.entrySet())
				{	String rank = entry.getKey();
					URL url = entry.getValue();
					String urlStr = url.toString();
					result.addResult(urlStr, engineStr, rank);
				}
			logger.decreaseOffset();
		}
		logger.decreaseOffset();
		logger.log("Total number of pages found: "+result.size());
		
		// record the complete list of URLs (not for cache, just as a result)
		result.exportResults(FileNames.FI_SEARCH_RESULTS_ALL);
		
		return result;
	}
	
	/**
	 * Launches the main search.
	 * 
	 * @param keywords
	 * 		Person we want to look up.
	 * @param websites
	 * 		List of targeted sites, including {@ode null} to search the whole Web.
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
	 * @param language
	 * 		Language targeted during the search.
	 * 
	 * @return
	 * 		The Web search results.
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
	private WebSearchResults performWebExtraction(String keywords, List<String> websites, Date startDate, Date endDate, boolean searchDate, String compulsoryExpression, ArticleLanguage language) throws IOException, ReaderException, ParseException, SAXException, ProcessorException
	{	logger.log("Starting the web extraction");
		logger.increaseOffset();
		
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
				logger.log("websites=");
				logger.log(websites);
			logger.decreaseOffset();
			
			// initializes the Web search engines
			initWebSearchEngines(websites, startDate, endDate, searchDate);
			
			// perform the Web search
			WebSearchResults results = performWebSearch(keywords);
	
			// filter Web pages (remove PDFs, and so on)
			results.filterByUrl();
			
			// retrieve the corresponding articles
			results.retrieveArticles();
			results.exportResults(FileNames.FI_SEARCH_RESULTS_URL);
	
			// possibly filter the articles depending on the content
			results.filterByContent(compulsoryExpression, language);
			results.exportResults(FileNames.FI_SEARCH_RESULTS_CONTENT);
			
			// detect the entity mentions
			results.detectMentions(recognizer);
			
			// possibly filter the articles depending on the entities
			results.filterByEntity(startDate, endDate, searchDate);
			results.exportResults(FileNames.FI_SEARCH_RESULTS_ENTITY);
			
			// displays the remaining articles with their mentions	//TODO maybe get the entities instead of the mentions, eventually?
			results.displayRemainingMentions(); // for debug only
			
			// extract events from the remaining articles and mentions
			extractEvents(results, "4_");
			
			// cluster the article by content
			results.clusterArticles(language);
			results.exportResults(FileNames.FI_SEARCH_RESULTS_CLUSTERS);
			
		logger.decreaseOffset();
		logger.log("Web extraction over");
		return results;
	}
	
	/////////////////////////////////////////////////////////////////
	// SOCIAL SEARCH	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of engines used to search social medias */
	private final List<AbstractSocialEngine> socialEngines = new ArrayList<AbstractSocialEngine>();
	
	/**
	 * Initializes the default search engines for social medias.
	 * Currently: only Facebook.
	 * 
	 * @param seeds 
	 * 		List of pages to process during the search on social media. 
	 * @param startDate
	 * 		Start of the period we want to consider, or {@code null} for no constraint.
	 * @param endDate
	 * 		End of the period we want to consider, or {@code null} for no constraint.
	 * @param extendedSearch
	 * 		Whether the social media search should retrieve the posts published by the
	 * 		users commenting the posts of interest, for the considered period. If 
	 * 		{@code false}, only the posts on the targeted page and their direct comments
	 * 		are returned. 
	 */
	private void initSocialMediaEngines(List<String> seeds, Date startDate, Date endDate, boolean extendedSearch)
	{	logger.log("Initializing the social media search engines");
		logger.increaseOffset();
		
		// iterate over each seed
		int i = 1;
		for(String seed: seeds)
		{	logger.log("Seed "+seed+" ("+i+"/"+seeds.size()+")");
			i++;
			
			// set up Facebook
			try
			{	FacebookEngine facebookEngine = new FacebookEngine(seed, startDate, endDate, extendedSearch);
				socialEngines.add(facebookEngine);
			} 
			catch (FailingHttpStatusCodeException | IOException | URISyntaxException e) 
			{	e.printStackTrace();
			}
		}
		logger.decreaseOffset();
	}
	
	/**
	 * Performs the Web search using the specified parameters and
	 * each one of the engines registered in the {@link #socialEngines}
	 * list.
	 * 
	 * @param keywords
	 * 		Person we want to look for.
	 * @param includeComments 
	 * 		Whether ({@code true}) or not ({@code false}) to include comments 
	 * 		in the proper article (or just the main post).
	 * @return
	 * 		List of results taking the form of URLs.
	 * 
	 * @throws IOException
	 * 		Problem accessing the Web.
	 */
	private SocialSearchResults performSocialSearch(String keywords, boolean includeComments) throws IOException
	{	SocialSearchResults result = new SocialSearchResults();
		
		// apply each search engine
		logger.log("Applying iteratively each social engine");
		logger.increaseOffset();
		for(AbstractSocialEngine engine: socialEngines)
		{	logger.log("Processing search engine "+engine);
			logger.increaseOffset();
				List<SocialSearchResult> posts = engine.retrieveResults(keywords);
			
				// add to the overall result object
				String engineStr = engine.toString();
				int rank = 1;
				for(SocialSearchResult post: posts)
				{	post.rank = rank;
					post.buildArticle(includeComments);
					result.addResult(post, engineStr);
					rank++;
				}
			logger.decreaseOffset();
		}
		logger.decreaseOffset();
		logger.log("Total number of posts found: "+result.size());
		
		// record the complete list of URLs (not for cache, just as a result)
		result.exportResults(FileNames.FI_SEARCH_RESULTS_ALL);
		
		return result;
	}
	
	/**
	 * Launches the main search.
	 * 
	 * @param keywords
	 * 		Person we want to look up.
	 * @param additionalSeeds 
	 * 		List of secondary pages to process during the search on social media. 
	 * @param startDate
	 * 		Start of the period we want to consider, 
	 * 		or {@code null} for no constraint.
	 * @param endDate
	 * 		End of the period we want to consider,
	 * 		or {@code null} for no constraint.
	 * @param extendedSearch
	 * 		Whether the social media search should retrieve the posts published by the
	 * 		users commenting the posts of interest, for the considered period. If 
	 * 		{@code false}, only the posts on the targeted page and their direct comments
	 * 		are returned. 
	 * @param compulsoryExpression
	 * 		String expression which must be present in the groups of posts,
	 * 		or {@code null} if there is no such constraint.
	 * @param language
	 * 		Language targeted during the search.
	 * 
	 * @return
	 * 		The social search results.
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
	private SocialSearchResults performSocialExtraction(String keywords, List<String> additionalSeeds, Date startDate, Date endDate, String compulsoryExpression, boolean extendedSearch, ArticleLanguage language) throws IOException, ReaderException, ParseException, SAXException, ProcessorException
	{	logger.log("Starting the social media extraction");
		logger.increaseOffset();
		
			// log search parameters
			logger.log("Parameters:");
			logger.increaseOffset();
				logger.log("keywords="+keywords);
				logger.log("startDate="+startDate);
				logger.log("endDate="+endDate);
				logger.log("extendedSearch="+extendedSearch);
				logger.log("additionalPages=");
				if(!additionalSeeds.isEmpty())
					logger.log(additionalSeeds);
			logger.decreaseOffset();
			
			// initializes the social media search engines
			List<String> seeds = new ArrayList<String>();
			seeds.add(null);
			seeds.addAll(additionalSeeds);
			initSocialMediaEngines(additionalSeeds, startDate, endDate, extendedSearch);
			
			// perform the social search
			boolean includeComments = false;
			SocialSearchResults results = performSocialSearch(keywords, includeComments);
			
			// convert the posts to proper articles
			results.buildArticles(includeComments);
			
			// possibly filter the articles depending on the content
			results.filterByContent(compulsoryExpression,language);
			results.exportResults(FileNames.FI_SEARCH_RESULTS_CONTENT);
			
			// detect the entity mentions
			results.detectMentions(recognizer);
			
			// possibly filter the articles depending on the entities
// unnecessary, unless we add other entity-based constraints than dates		
//			results.filterByEntity(null,null,true);
//			results.exportAsCsv(FileNames.FI_SEARCH_RESULTS_ENTITY);
			
			// displays the remaining articles with their mentions	//TODO maybe get the entities instead of the mention, eventually?
			results.displayRemainingMentions(); // for debug only
			
			// extract events from the remaining articles and mentions
			extractEvents(results, "4_");
		
			// cluster the articles by content
			results.clusterArticles(language);
			results.exportResults(FileNames.FI_SEARCH_RESULTS_CLUSTERS);
			
		logger.decreaseOffset();
		logger.log("Social media extraction over");
		return results;
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
	private void initRecognizer() throws ProcessorException
	{	recognizer = new StraightCombiner();
		recognizer.setCacheEnabled(true);//TODO set to false for debugging
	}

	/////////////////////////////////////////////////////////////////
	// MERGE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Extract the events by article and by sentence, for the specified results.
	 * Then export the events, cluster them, and export the resulting clusters 
	 * of events.
	 * 
	 * @param results
	 * 		Search results used for event extraction.
	 * @param filePrefix 
	 * 		String used to name the file to create.
	 * 
	 * @throws UnsupportedEncodingException
	 * 		Problem while exporting the events.
	 * @throws FileNotFoundException
	 * 		Problem while exporting the events.
	 */
	private void extractEvents(AbstractSearchResults<?> results, String filePrefix) throws UnsupportedEncodingException, FileNotFoundException
	{	boolean bySentence[] = {false,true};
		for(boolean bs: bySentence)
		{	// identify the events
			results.extractEvents(bs);
			// export the events as a table
			results.exportEvents(bs, false, filePrefix);
			// try to group similar events together
			results.clusterEvents();
			// export the resulting groups as a table
			results.exportEvents(bs, true, filePrefix);
		}
	}
	
	/**
	 * Combines the Web and social media results, and repeat the analysis
	 * steps on these combined results.
	 *  
	 * @param webRes
	 * 		Web search results.
	 * @param socRes
	 * 		Social media search results.
	 * @param language
	 * 		Targeted language.
	 * 
	 * @throws UnsupportedEncodingException
	 * 		Problem while exporting the results.
	 * @throws FileNotFoundException
	 * 		Problem while exporting the results.
	 */
	private void combineResults(WebSearchResults webRes, SocialSearchResults socRes, ArticleLanguage language) throws UnsupportedEncodingException, FileNotFoundException
	{	logger.log("Combining all results in a single file.");
		logger.increaseOffset();
		
			// merging the results and record
			CombinedSearchResults combRes = new CombinedSearchResults(webRes, socRes);
			combRes.resetClusters();
			combRes.exportResults(FileNames.FI_SEARCH_RESULTS_ENTITY);
			
			// extract events from the remaining articles and mentions
			extractEvents(combRes, "4_");

			// cluster the combined articles by content
			combRes.clusterArticles(language);
			combRes.exportResults(FileNames.FI_SEARCH_RESULTS_CLUSTERS);
			
			//TODO keep only entities present in the same cluster
			// redo the events
			// TODO pb: this changes the data, and affects the post merge operations

			
			
			// filter mentions to keep only those present in all the articles of the same cluster
			// TODO
			
//			// extract events from the remaining articles and mentions
//			boolean bySentence[] = {false,true};
//			for(boolean bs: bySentence)
//			{	results.extractEvents(bs);
//				// export the events as a table
//				results.exportEvents(bs, false);
//				// try to group similar events together
//				results.clusterEvents();
//				// export the resulting groups as a table
//				results.exportEvents(bs, true);
//			}
			
		logger.decreaseOffset();
	}
}
