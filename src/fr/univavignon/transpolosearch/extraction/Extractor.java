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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.entity.EntityType;
import fr.univavignon.transpolosearch.data.entity.mention.AbstractMention;
import fr.univavignon.transpolosearch.data.entity.mention.MentionDate;
import fr.univavignon.transpolosearch.data.entity.mention.MentionLocation;
import fr.univavignon.transpolosearch.data.entity.mention.MentionOrganization;
import fr.univavignon.transpolosearch.data.entity.mention.MentionPerson;
import fr.univavignon.transpolosearch.data.entity.mention.Mentions;
import fr.univavignon.transpolosearch.data.event.Event;
import fr.univavignon.transpolosearch.processing.InterfaceRecognizer;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.processing.combiner.straightcombiner.StraightCombiner;
import fr.univavignon.transpolosearch.retrieval.ArticleRetriever;
import fr.univavignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univavignon.transpolosearch.search.social.AbstractSocialEngine;
import fr.univavignon.transpolosearch.search.social.FacebookEngine;
import fr.univavignon.transpolosearch.search.social.SocialMediaPost;
import fr.univavignon.transpolosearch.search.web.AbstractWebEngine;
import fr.univavignon.transpolosearch.search.web.BingEngine;
import fr.univavignon.transpolosearch.search.web.GoogleEngine;
import fr.univavignon.transpolosearch.search.web.QwantEngine;
import fr.univavignon.transpolosearch.search.web.YandexEngine;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.file.FileTools;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;
import fr.univavignon.transpolosearch.tools.string.StringTools;
import fr.univavignon.transpolosearch.tools.time.Period;

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
	 * @param strictSearch
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
	public void performExtraction(String keywords, String website, Date startDate, Date endDate, boolean strictSearch, String compulsoryExpression, boolean extendedSocialSearch) throws IOException, ReaderException, ParseException, SAXException, ProcessorException
	{	logger.log("Starting the information extraction");
		logger.increaseOffset();
		
		// perform the Web search
		logger.log("Performing the Web search");
		performWebExtraction(keywords, website, startDate, endDate, strictSearch, compulsoryExpression);
		
		// perform the social search
		logger.log("Performing the social media search");
		performSocialExtraction(keywords, startDate, endDate, extendedSocialSearch);
		
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
	 * @param strictSearch
	 * 		If {@code true}, both dates will be used directly in the Web search.
	 * 		Otherwise, they will be used <i>a posteri</i> to filter the detected events.
	 * 		If one of the dates is {@code null}, this parameter has no effect.
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article,
	 * 		or {@code null} if there's no such constraint.
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
	public void performWebExtraction(String keywords, String website, Date startDate, Date endDate, boolean strictSearch, String compulsoryExpression) throws IOException, ReaderException, ParseException, SAXException, ProcessorException
	{	logger.log("Starting the web extraction");
		logger.increaseOffset();
		
		// perform the Web search
		Map<String,List<String>> originalUrls = performWebSearch(keywords, website, startDate, endDate, strictSearch);

		// filter Web pages (remove PDFs, and so on)
		List<String> filteredUrls = new ArrayList<String>(originalUrls.keySet());
		filterUrls(filteredUrls);
		
		// retrieve the corresponding articles
		List<Article> originalArticles = retrieveArticles(filteredUrls);
		
		// detect the entity mentions
		List<Mentions> originalMentions = detectMentions(originalArticles);
		
		// possibly filter the articles depending on the dates and compulsory expression
		List<Article> filteredArticles = new ArrayList<Article>(originalArticles);
		List<Mentions> filteredMentions = new ArrayList<Mentions>(originalMentions);
		filterArticles(filteredArticles,filteredMentions,startDate,endDate,strictSearch,compulsoryExpression);
		
		// displays the remaining articles with their mentions	//TODO maybe get the entities instead of the mention, eventually?
		displayRemainingMentions(filteredArticles,filteredMentions); //TODO for debug only
		
		// extract events from the remaining articles and mentions
		boolean bySentence = false; //TODO for debug
		List<List<Event>> events = extractEvents(filteredArticles,filteredMentions,bySentence);
		
		// export the events as a table
		exportWebEvents(originalUrls, filteredUrls, originalArticles, filteredArticles, events);
		
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
	public void performSocialExtraction(String keywords, Date startDate, Date endDate, boolean extendedSocialSearch) throws IOException, ReaderException, ParseException, SAXException, ProcessorException
	{	logger.log("Starting the social media extraction");
		logger.increaseOffset();
		
		// perform the social search
		List<SocialMediaPost> originalPosts = performSocialSearch(keywords, startDate, endDate, extendedSocialSearch);

		// convert the posts to articles for later use
		List<Article> convertedPosts = convertPosts(originalPosts);
		
//		// detect the entity mentions
//		List<Mentions> postMentions = detectMentions(convertedPosts);
//		
//		// displays the remaining articles with their mentions
//		displayRemainingMentions(convertedPosts,postMentions); //TODO for debug only
//		
//		// extract events from the remaining articles and mentions
//		boolean bySentence = false; //TODO for debug
//		List<List<Event>> events = extractEvents(convertedPosts, postMentions, bySentence);
//		
//		// export the events as a table
//		exportSocialEvents(originalPosts, convertedPosts, events);
		
		logger.decreaseOffset();
		logger.log("Social media extraction over");
	}
	
	/////////////////////////////////////////////////////////////////
	// WEB SEARCH	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of engines used for the Web search */
	private final List<AbstractWebEngine> webEngines = new ArrayList<AbstractWebEngine>();
	/** URL comparator (apparently, the default one is not appropriate? Can't remember why I added this...) */
	private final static Comparator<URL> URL_COMPARATOR = new Comparator<URL>()
	{	@Override
		public int compare(URL url1, URL url2)
		{	int result = url1.toString().compareTo(url2.toString());
			return result;
		}	
	};
	
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
	 * @param strictSearch
	 * 		If {@code true}, both dates will be used directly in the Web search.
	 * 		Otherwise, they will be used <i>a posteriori</i> to filter the detected events.
	 * 		If one of the dates is {@code null}, this parameter has no effect.
	 * @return
	 * 		List of results taking the form of a map associating URLs (as strings) with
	 * 		search engine names.
	 * 
	 * @throws IOException
	 * 		Problem accessing the Web.
	 */
	private Map<String,List<String>> performWebSearch(String keywords, String website, Date startDate, Date endDate, boolean strictSearch) throws IOException
	{	boolean cachedSearch = true; //TODO for debug
		
		// log search parameters
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("keywords="+keywords);
			logger.log("startDate="+startDate);
			logger.log("endDate="+endDate);
			String txt = "strictSearch="+strictSearch;
			if(!strictSearch)
				txt = txt + "(dates are ignored here, because the search is not strict)";
			logger.log(txt);
		logger.decreaseOffset();
		
		// nullify dates if the search is not strict
		if(!strictSearch)
		{	startDate = null;
			endDate = null;
		}
		
		// apply each search engine
		logger.log("Applying iteratively each search engine");
		logger.increaseOffset();
			Map<String,List<String>> result = new HashMap<String,List<String>>();
			
			for(AbstractWebEngine engine: webEngines)
			{	Set<URL> urls = new TreeSet<URL>(URL_COMPARATOR);
				
				// possibly use cached results
				String cacheFilePath = FileNames.FO_WEB_SEARCH_RESULTS + File.separator + engine.getName();
				File cacheFolder = new File(cacheFilePath);
				cacheFolder.mkdirs();
				cacheFilePath = cacheFilePath + File.separator + FileNames.FI_SEARCH_RESULTS;
				File cacheFile = new File(cacheFilePath);
				if(cachedSearch && cacheFile.exists())
				{	logger.log("Loading the previous search results from file "+cacheFilePath);
					Scanner sc = FileTools.openTextFileRead(cacheFile,"UTF-8");
					while(sc.hasNextLine())
					{	String urlStr = sc.nextLine();
						URL url = new URL(urlStr);
						urls.add(url);
					}
					logger.log("Number of URLs loaded: "+urls.size());
				}
				
				// search the results
				else
				{	logger.log("Applying search engine "+engine.getName());
					logger.increaseOffset();
						// apply the engine
						List<URL> temp = engine.search(keywords,website,startDate,endDate);
						urls.addAll(temp);
						
						// possibly record its results
						if(cachedSearch)
						{	logger.log("Recording all URLs in text file \""+cacheFilePath+"\"");
							PrintWriter pw = FileTools.openTextFileWrite(cacheFile,"UTF-8");
							for(URL url: urls)
								pw.println(url.toString());
							pw.close();
						}
					logger.decreaseOffset();
				}
				
				// add to the overall map of URLs
				for(URL url: urls)
				{	String urlStr = url.toString();
					List<String> list = result.get(urlStr);
					if(list==null)
					{	list = new ArrayList<String>();
						result.put(urlStr, list);
					}
					list.add(engine.getName());
				}
			}
		logger.decreaseOffset();
		logger.log("Total number of pages found: "+result.size());
		
		// record the complete list of URLs (not for cache, just as a result)
		File folder = new File(FileNames.FO_WEB_SEARCH_RESULTS);
		folder.mkdirs();
		String cacheFilePath = FileNames.FO_WEB_SEARCH_RESULTS + File.separator + FileNames.FI_SEARCH_RESULTS;
		logger.log("Recording all URLs in CSV file \""+cacheFilePath+"\"");
		PrintWriter pw = FileTools.openTextFileWrite(cacheFilePath,"UTF-8");
		pw.println("URL,Engines");
		for(Entry<String,List<String>> entry: result.entrySet())
		{	String urlStr = entry.getKey();
			List<String> engineList = entry.getValue();
			String val = "\"";
			for(String engineName: engineList)
				val = val + engineName + ", ";
			if(!val.isEmpty())
				val = val.substring(0, val.length()-2);
			val = val + "\"";
			pw.println(urlStr+","+val);
		
		}
		pw.close();
		
		return result;
	}
	
	/**
	 * Removes from the specified list the URLs
	 * which cannot be processed.
	 * 
	 * @param urls
	 * 		List of Web addresses.
	 */
	private void filterUrls(List<String> urls)
	{	logger.log("Filtering the retrieved URL to remove those we can't treat");
		logger.increaseOffset();
		
		Iterator<String> it = urls.iterator();
		while(it.hasNext())
		{	String url = it.next();
			
			// we don't process PDF files
			if(url.endsWith(FileNames.EX_PDF))
			{	logger.log("The following URL points towards a PDF, we cannot currently use it: "+url);
				it.remove();
			}
			
			else
				logger.log("We keep the URL "+url);
		}
		
		Collections.sort(urls);
		logger.decreaseOffset();
		logger.log("Filtering complete: "+urls.size()+" pages kept");
	}
	
	/////////////////////////////////////////////////////////////////
	// RETRIEVAL	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Retrieve all the articles whose URLs are indicated
	 * in the list parameter.
	 * 
	 * @param urls
	 * 		List of URLs to process.
	 * @return
	 * 		The list of corresponding article objects.
	 * 
	 * @throws IOException
	 * 		Problem while retrieving a Web page.
	 * @throws ParseException
	 * 		Problem while retrieving a Web page.
	 * @throws SAXException
	 * 		Problem while retrieving a Web page.
	 */
	private List<Article> retrieveArticles(List<String> urls) throws IOException, ParseException, SAXException
	{	logger.log("Starting the article retrieval");
		logger.increaseOffset();
		
		// init
		List<Article> result = new ArrayList<Article>();
		ArticleRetriever articleRetriever = new ArticleRetriever(true); //TODO cache disabled for debugging
		articleRetriever.setLanguage(ArticleLanguage.FR); // TODO we know the articles will be in French (should be generalized later)

		// retrieve articles
		Iterator<String> it = urls.iterator();
		int nbr = urls.size();
		int i = 0;
		while(it.hasNext())
		{	String url = it.next();
			i++;
			logger.log("Retrieving article ("+i+"/"+nbr+") at URL "+url);
			try
			{	Article article = articleRetriever.process(url);
				result.add(article);
			}
			catch (ReaderException e)
			{	logger.log("WARNING: Could not retrieve the article at URL "+url.toString()+" >> removing it from the result list.");
				it.remove();
			}
		}
		
		logger.decreaseOffset();
		logger.log("Article retrieval complete");
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
	 * @return
	 * 		List of results taking the form of URLs.
	 * 
	 * @throws IOException
	 * 		Problem accessing the Web.
	 */
	private List<SocialMediaPost> performSocialSearch(String keywords, Date startDate, Date endDate, boolean extendedSearch) throws IOException
	{	boolean cachedSearch = true; //TODO for debug
		// log search parameters
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("keywords="+keywords);
			logger.log("startDate="+startDate);
			logger.log("endDate="+endDate);
		logger.decreaseOffset();
		
		// apply each search engine
		logger.log("Applying iteratively each social engine");
		logger.increaseOffset();
			List<SocialMediaPost> result = new ArrayList<SocialMediaPost>();
			
			for(AbstractSocialEngine engine: socialEngines)
			{	List<SocialMediaPost> posts = new ArrayList<SocialMediaPost>();
				
				// possibly use cached results
				String cacheFilePath = FileNames.FO_SOCIAL_SEARCH_RESULTS + engine.getName();
				File cacheFolder = new File(cacheFilePath);
				cacheFolder.mkdirs();
				cacheFilePath = cacheFilePath + File.separator + FileNames.FI_SEARCH_RESULTS;
				File cacheFile = new File(cacheFilePath);
				if(cachedSearch && cacheFile.exists())
				{	logger.log("Loading the previous search results from file "+cacheFilePath);
					Scanner sc = FileTools.openTextFileRead(cacheFile,"UTF-8");
					while(sc.hasNextLine())
					{	SocialMediaPost post = SocialMediaPost.readFromText(sc);
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
							for(SocialMediaPost post: posts)
								post.writeAsText(pw);
							pw.close();
						}
					logger.decreaseOffset();
				}
				
				// add to the overall list of URL
				result.addAll(posts);
			}
		logger.decreaseOffset();
		logger.log("Total number of posts found: "+result.size());
		
		return result;
	}
	
	/**
	 * Convert each social media post from the specified list to
	 * a proper article. The comments are simply concatenated as new
	 * paragraphs, at the end of the post.
	 * 
	 * @param posts
	 * 		List of post to convert.
	 * @return
	 * 		Resulting list of articles.
	 */
	private List<Article> convertPosts(List<SocialMediaPost> posts)
	{	List<Article> result = new ArrayList<Article>();
		
		for(SocialMediaPost post: posts)
		{	Article article = post.convert();
			result.add(article);
		}
		
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
	
	/**
	 * Detects the entity mentions present in each specified article.
	 * 
	 * @param articles
	 * 		The list of articles to process.
	 * @return
	 * 		A list of entity mentions for each article.
	 * @throws ProcessorException
	 * 		Problem while applying the NER tool.
	 */
	private List<Mentions> detectMentions(List<Article> articles) throws ProcessorException
	{	logger.log("Detecting entities in all "+articles.size()+" articles");
		logger.increaseOffset();
		List<Mentions> result = new ArrayList<Mentions>();
		
		for(Article article: articles)
		{	logger.log("Processing article "+article.getTitle()+"("+article.getUrl()+")");
			logger.increaseOffset();
				Mentions mentions = recognizer.recognize(article);
				result.add(mentions);
				
				logger.log("Found "+mentions.getMentions().size()+" entities");
			logger.decreaseOffset();
		}
		
		logger.decreaseOffset();
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// FILTERING	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Removes from the list the articles concerning events
	 * not contained in the specified date range. Also removes
	 * the articles not containing the compulsory expression.
	 *  
	 * @param articles
	 * 		List of articles to process.
	 * @param mentions
	 * 		List of the entity mentions detected in the listed articles.
	 * @param startDate
	 * 		Start of the time period.
	 * @param endDate
	 * 		End of the time period.
	 * @param strictSearch
	 * 		Whether the filtering should be applied ({@code false})
	 * 		or not ({@code true}).
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article,
	 * 		or {@code null} if there's no such constraint.
	 */
	private void filterArticles(List<Article> articles, List<Mentions> mentions, Date startDate, Date endDate, boolean strictSearch, String compulsoryExpression)
	{	logger.log("Starting filtering the articles");
		logger.increaseOffset();
		
		// log stuff
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("startDate="+startDate);
			logger.log("endDate="+endDate);
			String txt = "strictSearch="+strictSearch;
			if(strictSearch)
				txt = txt + "(dates are ignored here, because the search is strict)";
			logger.log(txt);
			logger.log("compulsoryExpression="+compulsoryExpression);
		logger.decreaseOffset();
		
		// possibly filter the resulting texts depending on the compulsory expression
		if(compulsoryExpression!=null)
		{	logger.log("Removing articles not containing the compulsory expression \""+compulsoryExpression+"\"");
			logger.increaseOffset();
				int count = 0;
				Iterator<Article> itArt = articles.iterator();
				Iterator<Mentions> itEnt = mentions.iterator();
				while(itArt.hasNext())
				{	Article article = itArt.next();
					itEnt.next();
					logger.log("Processing article "+article.getTitle());
					String rawText = article.getRawText();
					if(!rawText.contains(compulsoryExpression))
					{	logger.log("Removing article "+article.getTitle()+" ("+article.getUrl()+")");
						itArt.remove();
						itEnt.remove();
						count++;
					}
				}
				logger.log(">Number of articles removed: "+count);
				logger.log(">Number of articles remaining: "+articles.size());
			logger.decreaseOffset();
		}
		else
			logger.log("No compulsory expression to process");

		// possibly filter the resulting texts depending on the dates they contain
		if(!strictSearch)
		{	if(startDate==null || endDate==null)
				logger.log("WARNING: one date is null, so both of them are ignored");
			else
			{	logger.log("Removing articles not fitting the date constraints: "+startDate+"->"+endDate);
				logger.increaseOffset();
					fr.univavignon.transpolosearch.tools.time.Date start = new fr.univavignon.transpolosearch.tools.time.Date(startDate);
					fr.univavignon.transpolosearch.tools.time.Date end = new fr.univavignon.transpolosearch.tools.time.Date(endDate);
					Iterator<Article> itArt = articles.iterator();
					Iterator<Mentions> itEnt = mentions.iterator();
					int count = 0;
					while(itArt.hasNext())
					{	Article article = itArt.next();
						Mentions ments = itEnt.next();
						logger.log("Processing article "+article.getTitle());
						List<AbstractMention<?>> dates = ments.getMentionsByType(EntityType.DATE);

						// check if the article contains a date between start and end
						boolean remove = dates.isEmpty();
						if(!remove)	
						{	fr.univavignon.transpolosearch.tools.time.Date date = null;
							Iterator<AbstractMention<?>> it = dates.iterator();
							while(date==null && it.hasNext())
							{	AbstractMention<?> mention = it.next();
								fr.univavignon.transpolosearch.tools.time.Date d = (fr.univavignon.transpolosearch.tools.time.Date) mention.getValue();
								if(d.isContained(start, end))
									date = d;
							}
							
							if(date!=null)
							{	logger.log("Found date "+date+" in article "+article.getTitle()+" >> removal ("+article.getUrl()+")");
								remove = true;
							}
						}
						
						// possibly remove the article/mentions
						if(remove)
						{	itArt.remove();
							itEnt.remove();
							count++;
						}
					}
					logger.log(">Number of articles removed: "+count);
					logger.log(">Number of articles remaining: "+articles.size());
				logger.decreaseOffset();
			}
		}
		
		logger.decreaseOffset();
		logger.log("Article filtering complete");
	}

	/////////////////////////////////////////////////////////////////
	// DISPLAY	/////////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Displays the entity mentions associated to each article.
	 * 
	 * @param articles
	 * 		List of articles.
	 * @param mentions
	 * 		List of associated entity mentions.
	 */
	private void displayRemainingMentions(List<Article> articles, List<Mentions> mentions)
	{	logger.log("Displaying remaining articles and entity mentions");
		logger.increaseOffset();
		
		Iterator<Article> itArt = articles.iterator();
		Iterator<Mentions> itEnt = mentions.iterator();
		int count = 0;
		while(itArt.hasNext())
		{	Article article = itArt.next();
			Mentions ments = itEnt.next();
			logger.log("Article: "+article.getTitle()+" ("+count+"/"+articles.size()+")");
			logger.increaseOffset();
				count++;
				List<AbstractMention<?>> dates = ments.getMentionsByType(EntityType.DATE);
				if(!dates.isEmpty())
				{	String first = "Dates ("+dates.size()+"):";
					List<String> msg = new ArrayList<String>();
					msg.add(first);
					for(AbstractMention<?> mention: dates)
						msg.add(mention.toString());
					logger.log(msg);
				}
				List<AbstractMention<?>> locations = ments.getMentionsByType(EntityType.LOCATION);
				if(!locations.isEmpty())
				{	String first = "Locations ("+locations.size()+"):";
					List<String> msg = new ArrayList<String>();
					msg.add(first);
					for(AbstractMention<?> mention: locations)
						msg.add(mention.toString());
					logger.log(msg);
				}
				List<AbstractMention<?>> organizations = ments.getMentionsByType(EntityType.ORGANIZATION);
				if(!organizations.isEmpty())
				{	String first = "Organizations ("+organizations.size()+"):";
					List<String> msg = new ArrayList<String>();
					msg.add(first);
					for(AbstractMention<?> mention: organizations)
						msg.add(mention.toString());
					logger.log(msg);
				}
				List<AbstractMention<?>> persons = ments.getMentionsByType(EntityType.PERSON);
				if(!persons.isEmpty())
				{	String first = "Persons ("+persons.size()+"):";
					List<String> msg = new ArrayList<String>();
					msg.add(first);
					for(AbstractMention<?> mention: persons)
						msg.add(mention.toString());
					logger.log(msg);
				}
			logger.decreaseOffset();
		}
		
		logger.decreaseOffset();
		logger.log("Display complete");
	}
	
	/////////////////////////////////////////////////////////////////
	// EVENTS PROCESSING	/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Takes a list of articles and a list of the corresponding entity mentions,
	 * and identifies the events described in the articles.
	 * 
	 * @param articles
	 * 		List of articles to treat.
	 * @param mentions
	 * 		List of the associated entity mentions.
	 * @param bySentence
	 * 		Whether to retrieve events by sentence (all event-related entity mentions
	 * 		must be in the same sentence) or by article.
	 * @return
	 * 		The resulting list of events, for each article.
	 */
	private List<List<Event>> extractEvents(List<Article> articles, List<Mentions> mentions, boolean bySentence)
	{	logger.log("Extracting events");
		logger.increaseOffset();
		List<List<Event>> result = new ArrayList<List<Event>>();
		
		// processing each article
		Iterator<Article> itArt = articles.iterator();
		Iterator<Mentions> itMent = mentions.iterator();
		int count = 0;
		int eventNbr = 0;
		while(itArt.hasNext())
		{	Article article = itArt.next();
			Mentions ments = itMent.next();
			logger.log("Article: "+article.getTitle()+" ("+count+"/"+articles.size()+")");
			logger.increaseOffset();
				count++;
				String rawText = article.getRawText();
				List<Event> events = new ArrayList<Event>();
				result.add(events);
				
				if(bySentence)
				{	// retrieving the sentence positions
					List<Integer> sentencePos = StringTools.getSentencePositions(rawText);
					sentencePos.add(rawText.length()); // to mark the end of the last sentence
					int sp = -1;
					
					// for each sentence, we get the detected entity mentions
					for(int ep: sentencePos)
					{	if(sp>=0)
						{	List<AbstractMention<?>> le = ments.getMentionsIn(sp, ep);
							List<AbstractMention<?>> dates = Mentions.filterByType(le,EntityType.DATE);
							// only go on if there is at least one date
							if(!dates.isEmpty())
							{	if(dates.size()>1)
									logger.log("WARNING: there are several dates in sentence \""+rawText.substring(sp,ep)+"\"");
								else
								{	MentionDate ed = (MentionDate)dates.get(0);
									List<AbstractMention<?>> persons = Mentions.filterByType(le,EntityType.PERSON);
									if(persons.isEmpty())
										logger.log("WARNING: there is a date ("+ed.getValue()+") but no persons in sentence \""+rawText.substring(sp,ep)+"\"");
									else
									{	Event event = new Event(ed);
										events.add(event);
										eventNbr++;
										for(AbstractMention<?> mention: persons)
										{	MentionPerson person = (MentionPerson)mention;
											event.addPerson(person);
										}
										List<AbstractMention<?>> organizations = Mentions.filterByType(le,EntityType.ORGANIZATION);
										for(AbstractMention<?> mention: organizations)
										{	MentionOrganization organization = (MentionOrganization)mention;
											event.addOrganization(organization);
										}
										List<AbstractMention<?>> locations = Mentions.filterByType(le,EntityType.LOCATION);
										for(AbstractMention<?> mention: locations)
										{	MentionLocation location = (MentionLocation)mention;
											event.addLocation(location);
										}
										logger.log(Arrays.asList("Event found for sentence \""+rawText.substring(sp,ep)+"\"",event.toString()));
									}
								}
							}
						}
						sp = ep;
					}
				}
				
				else // by article
				{	List<AbstractMention<?>> dates = ments.getMentionsByType(EntityType.DATE);
					// only go on if there is at least one date
					if(!dates.isEmpty())
					{	Event event;
						if(dates.size()>1)
						{	logger.log("There are several ("+dates.size()+") dates in the article >> merging them");
							Iterator<AbstractMention<?>> it = dates.iterator();
							MentionDate ed = (MentionDate)it.next();
							event = new Event(ed);
							while(it.hasNext())
							{	ed = (MentionDate)it.next();
								Period p = ed.getValue(); 
								event.mergePeriod(p);
							}
						}
						else
						{	MentionDate esd = (MentionDate)dates.get(0);
							event = new Event(esd);
						}
						
						List<AbstractMention<?>> persons = ments.getMentionsByType(EntityType.PERSON);
						if(persons.isEmpty())
							logger.log("WARNING: there is a date ("+event.getPeriod()+") but no person in article \""+article.getTitle()+"\"");
						else
						{	events.add(event);
							eventNbr++;
							
							for(AbstractMention<?> mention: persons)
							{	MentionPerson person = (MentionPerson)mention;
								event.addPerson(person);
							}
							List<AbstractMention<?>> organizations = ments.getMentionsByType(EntityType.ORGANIZATION);
							for(AbstractMention<?> mention: organizations)
							{	MentionOrganization organization = (MentionOrganization)mention;
								event.addOrganization(organization);
							}
							List<AbstractMention<?>> locations = ments.getMentionsByType(EntityType.LOCATION);
							for(AbstractMention<?> mention: locations)
							{	MentionLocation location = (MentionLocation)mention;
								event.addLocation(location);
							}
							logger.log(Arrays.asList("Event found for article \""+article.getTitle()+"\"",event.toString()));
						}
					}
				}
				
			logger.log("Total number of events for this article: "+events.size());
			logger.decreaseOffset();
		}
		
		logger.decreaseOffset();
		logger.log("Event extraction complete: "+eventNbr+" events detected in "+articles.size()+" articles");
		return result;
	}
	
//	/**
//	 * Takes a list of entity mentions and returns the list of
//	 * corresponding strings.
//	 * 
//	 * @param mentions
//	 * 		List of entity mentions.
//	 * @return
//	 * 		List of the associated strings.
//	 */
//	private List<String> extractMentionNames(List<AbstractMention<?>> mentions)
//	{	List<String> result = new ArrayList<String>();
//		
//		for(AbstractMention<?> mention: mentions)
//		{	Object object = mention.getStringValue();
//			String str = object.toString();
//			result.add(str);
//		}
//		
//		return result;
//	}
	
	/**
	 * Records the results of the web search as a CSV file.
	 * 
	 * @param originalUrls
	 * 		List of treated URLs.
	 * @param filteredUrls
	 * 		List of remaining URLs after filtering.
	 * @param originalArticles
	 * 		List of treated articles.
	 * @param filteredArticles
	 * 		List of remaining articles after filtering.
	 * @param events
	 * 		List of detected events.
	 * 
	 * @throws UnsupportedEncodingException
	 * 		Problem while accessing to the result file.
	 * @throws FileNotFoundException
	 * 		Problem while accessing to the result file.
	 */
	private void exportWebEvents(Map<String,List<String>> originalUrls, List<String> filteredUrls, List<Article> originalArticles, List<Article> filteredArticles, List<List<Event>> events) throws UnsupportedEncodingException, FileNotFoundException
	{	String filePath = FileNames.FO_WEB_SEARCH_RESULTS + File.separator + FileNames.FI_EVENT_TABLE;
		logger.log("Recording the events as a CVS file: "+filePath);
		logger.decreaseOffset();
		
		List<String> urls = new ArrayList<String>(originalUrls.keySet());
		Collections.sort(urls);
		
		PrintWriter pw = FileTools.openTextFileWrite(filePath, "UTF-8");
		
		// write header
		List<String> colNames = Arrays.asList(
			"Rank",
			"Page title",
			"Url",
			"Date",
			"Content",
			"Active",
			"Contains keyword",
			"Support type",
			"Source",
			"Content type",
			"Theme",
			"Event",
			"Date",
			"Hour",
			"Period relevance",
			"Location",
			"Persons/Organizations",
			"Event rank"
		);
		Iterator<String> it = colNames.iterator();
		while(it.hasNext())
		{	String colName = it.next();
			pw.print(colName);
			if(it.hasNext())
				pw.print(",");
		}
		pw.println();
		
		// write data
		logger.log("Treat each article separately");
		int i = 1;
		for(String url: urls)
		{	String address = "\""+url+"\"";
			int indexUrl = filteredUrls.indexOf(url);
			
			// URL not processed
			if(indexUrl==-1)
			{	if(url.endsWith(FileNames.EX_PDF))
					pw.print(
						i+",,"+
						address+",,,,,"+
						"PDF,,,,,,,,,"+
						"Filtered URL,"
					);
				else
					pw.print(
						i+",,"+
						address+",,,,,,,,,,,,,,"+
						"Filtered URL,"
					);
			}
			
			// URL processed
			else
			{	Article article = originalArticles.get(indexUrl);
				String title = "\""+article.getTitle()+"\"";
				int indexArticle = filteredArticles.indexOf(article);
				
				// article not processed
				if(indexArticle==-1)
				{	pw.print(
						i+","+
						title+","+
						address+",,,,"+
						"No,,,,,,,,,,"+
						"Filtered article,"
					);
				}
				
				// article processed
				else
				{	List<Event> eventList = events.get(indexArticle);

					// no event detected for this article
					if(eventList.isEmpty())
						pw.print(
							i+","+
							title+","+
							address+",,,,"+
							"Yes,,,,,,,,,,"+
							"No event detected,0"
						);

					// at least one event detected
					else
					{	for(int j=0;j<events.size();j++)
						{	Event event = eventList.get(j);
							String beginning = i+","+title+","+address+",,,,"+"Yes,,,,,"; 
							String ending = convertEvent2Csv(event);
							pw.print(beginning+","+ending+","+(j+1));
						}
					}
				}
			}
			
			pw.println();
			i++;
		}
		
		pw.close();
		logger.decreaseOffset();
	}
	
	/**
	 * Records the results of the social media search as a CSV file.
	 * 
	 * @param originalPosts
	 * 		Posts originally retrieved.
	 * @param convertedPosts
	 * 		List of articles representing the above posts.
	 * @param events
	 * 		List of detected events.
	 * 
	 * @throws UnsupportedEncodingException
	 * 		Problem while accessing to the result file.
	 * @throws FileNotFoundException
	 * 		Problem while accessing to the result file.
	 */
	private void exportSocialEvents(List<SocialMediaPost> originalPosts, List<Article> convertedPosts, List<List<Event>> events) throws UnsupportedEncodingException, FileNotFoundException
	{	String filePath = FileNames.FO_SOCIAL_SEARCH_RESULTS + File.separator + FileNames.FI_EVENT_TABLE;
		logger.log("Recording the events as a CVS file: "+filePath);
		logger.decreaseOffset();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/mm/yyyyy",Locale.FRANCE); //TODO we suppose we deal with French articles, but this should be generalized later
		
		PrintWriter pw = FileTools.openTextFileWrite(filePath, "UTF-8");
		
		// write header
		List<String> colNames = Arrays.asList(
			"Title",
			"ID",
			"Url",
			"Date",
			"Content",
			"Source",
			"Event",
			"Date",
			"Hour",
			"Location",
			"Persons/Organizations",
			"Event rank"
		);
		Iterator<String> it = colNames.iterator();
		while(it.hasNext())
		{	String colName = it.next();
			pw.print(colName);
			if(it.hasNext())
				pw.print(",");
		}
		pw.println();
		
		// write data
		logger.log("Treat each post separately");
		for(int i=0;i<originalPosts.size();i++)
		{	// get the data
			SocialMediaPost post = originalPosts.get(i);
			Article article = convertedPosts.get(i);
			List<Event> eventList = events.get(i);
			
			// title and id
			String beginningStr = "\""+article.getTitle()+"\"";
			beginningStr = beginningStr + ","+post.id;
			
			// url
			String url = article.getUrl().toString();
			if(url==null)
				beginningStr = beginningStr + ",";
			else
				beginningStr = beginningStr + ",\""+url+"\"";
			
			// date
			Date date = article.getPublishingDate();
			String dateStr = sdf.format(date);
			beginningStr = beginningStr + ","+dateStr;
			
			// content
			beginningStr = beginningStr + ",\""+article.getRawText()+"\"";
			
			// source
			beginningStr = beginningStr + ","+post.source;

			// events
			if(eventList.isEmpty())
				pw.print(beginningStr+",No event detected,,,,,");
			else
			{	for(int j=0;j<events.size();j++)
				{	Event event = eventList.get(j);
					String ending = convertEvent2Csv(event);
					pw.print(beginningStr+","+ending+","+(j+1));
				}
			}
			
			pw.println();
		}
		
		pw.close();
		logger.decreaseOffset();
	}
	
	/**
	 * Converts an event to a CSV-compatible representation.
	 * 
	 * @param event
	 * 		Event of interest.
	 * @return
	 * 		String representing the event in a CSV file.
	 */
	private String convertEvent2Csv(Event event)
	{	// get the dates
		Period period = event.getPeriod();
		String periodStr = period.toString();
		
		// get the locations
		String locations = "\"";
		{	Collection<String> locs = event.getLocations();
			Iterator<String> itLoc = locs.iterator();
			while(itLoc.hasNext())
			{	String loc = itLoc.next();
				locations = locations + loc;
				if(itLoc.hasNext())
					locations = locations + ", ";
			}
			locations = locations + "\"";
		}
		
		// get the persons/organizations
		String persOrgs = "\"";
		Collection<String> all = new ArrayList<String>();
		all.addAll(event.getPersons());
		all.addAll(event.getOrganizations());
		Iterator<String> itPo = all.iterator();
		while(itPo.hasNext())
		{	String ment = itPo.next();
			persOrgs = persOrgs + ment;
			if(itPo.hasNext())
				persOrgs = persOrgs + ", ";
		}
		persOrgs = persOrgs + "\"";
		
		String result = periodStr+",,,"+locations+","+persOrgs;
		return result;
	}
}
