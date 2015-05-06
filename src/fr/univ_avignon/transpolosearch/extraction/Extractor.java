package fr.univ_avignon.transpolosearch.extraction;

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

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.SAXException;

import fr.univ_avignon.transpolosearch.data.article.Article;
import fr.univ_avignon.transpolosearch.data.entity.Entities;
import fr.univ_avignon.transpolosearch.recognition.AbstractRecognizer;
import fr.univ_avignon.transpolosearch.recognition.RecognizerException;
import fr.univ_avignon.transpolosearch.recognition.combiner.straightcombiner.StraightCombiner;
import fr.univ_avignon.transpolosearch.retrieval.ArticleRetriever;
import fr.univ_avignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univ_avignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univ_avignon.transpolosearch.tools.log.HierarchicalLoggerManager;
import fr.univ_avignon.transpolosearch.websearch.AbstractEngine;
import fr.univ_avignon.transpolosearch.websearch.GoogleEngine;

/**
 * This class handles the main search, i.e. it :
 * <ol>
 * 	<li>Search: determines which articles are relevant, using one or several Web search engines.</li>
 * 	<li>Retrieval: retrieves them using our article reader.</li>
 * 	<li>Detection: detects the named entities they contain.</li>
 * 	<li>Save: records the corresponding events.</li>
 * <ol>
 * 
 * @author Vincent Labatut
 */
public class Extractor
{	
	/**
	 * Builds and initializes an extractor object,
	 * using the default parameter. 
	 * <br/>
	 * Override/modify the methods called here, 
	 * in order to change these parameters.
	 * 
	 * @throws RecognizerException
	 * 		Problem while initializing the NER tool. 
	 */
	public Extractor() throws RecognizerException
	{	initDefaultSearchEngines();
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
	 * 		Person we want to look for.
	 * @param website
	 * 		Target site, or {@ode null} to search the whole Web.
	 * @param startDate
	 * 		Start of the period we want to consider, 
	 * 		or {@code null} for no contraint.
	 * @param endDate
	 * 		End of the period we want to consider,
	 * 		or {@code null} for no contraint.
	 * @param strictSearch
	 * 		If {@code true}, both dates will be used directly in the Web search.
	 * 		Otherwise, they will be used <i>a posteri</i> to filter the detected events.
	 * 		If one of the dates is {@code null}, this parameter has no effect.
	 * 
	 * @throws IOException 
	 * 		Problem accessing the Web or a file.
	 * @throws SAXException 
	 * 		Problem while retrieving a Web page.
	 * @throws ParseException 
	 * 		Problem while retrieving a Web page.
	 * @throws ReaderException 
	 * 		Problem while retrieving a Web page.
	 * @throws RecognizerException 
	 * 		Problem while detecting the entities.
	 */
	public void performExtraction(String keywords, String website, Date startDate, Date endDate, boolean strictSearch) throws IOException, ReaderException, ParseException, SAXException, RecognizerException
	{	logger.log("Starting the information extraction");
		logger.increaseOffset();
		
		// perform the Web search
		List<URL> urls = performWebSearch(keywords, website, startDate, endDate, strictSearch);
		
		// retrieve the corresponding articles
		List<Article> articles = retrieveArticles(urls);
		
		// detect the entities
		List<Entities> entities = detectEntities(articles);
		
		// possibly filter the articles depending on the dates
		filterArticles(articles,entities,startDate,endDate,strictSearch);
		
		//TODO possibilité de spécifier un mot devant absolument être contenu dans les articles
		
		logger.decreaseOffset();
		logger.log("Information extraction over");
	}

	/////////////////////////////////////////////////////////////////
	// WEB SEARCH	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of engines used for the Web search */
	private final List<AbstractEngine> engines = new ArrayList<AbstractEngine>();
	
	/**
	 * Initializes the default search engines.
	 * Currently: only Google Custom Search.
	 * (others can easily be added).
	 */
	private void initDefaultSearchEngines()
	{	// set up the google custom search
		GoogleEngine googleEngine = new GoogleEngine();
		googleEngine.pageCountry = "countryFR";
		googleEngine.pageLanguage = "lang_fr";
		googleEngine.resultNumber = 200;
		engines.add(googleEngine);
		
		// set up duck duck go
		// TODO
	}
	
	/**
	 * Peforms the Web search using the specified parameters and
	 * each one of the engines registered in the {@code engines}
	 * list.
	 * 
	 * @param keywords
	 * 		Person we want to look for.
	 * @param website
	 * 		Target site, or {@ode null} to search the whole Web.
	 * @param startDate
	 * 		Start of the period we want to consider, 
	 * 		or {@code null} for no contraint.
	 * @param endDate
	 * 		End of the period we want to consider,
	 * 		or {@code null} for no contraint.
	 * @param strictSearch
	 * 		If {@code true}, both dates will be used directly in the Web search.
	 * 		Otherwise, they will be used <i>a posteri</i> to filter the detected events.
	 * 		If one of the dates is {@code null}, this parameter has no effect.
	 * @return
	 * 		List of results taking the form of URLs.
	 * 
	 * @throws IOException
	 * 		Problem accessing the Web.
	 */
	private List<URL> performWebSearch(String keywords, String website, Date startDate, Date endDate, boolean strictSearch) throws IOException
	{	logger.log("Applying iteratively each search engine");
		logger.increaseOffset();
		
		// log stuff
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
		Set<URL> set = new TreeSet<URL>();
		for(AbstractEngine engine: engines)
		{	logger.log("Applying search engine "+engine.getName());
			logger.increaseOffset();
				List<URL> temp = engine.search(keywords,website,startDate,endDate);
				set.addAll(temp);
			logger.decreaseOffset();
		}
		
		List<URL> result = new ArrayList<URL>(set);
		logger.log("Total number of pages found: "+result.size());
		
		logger.decreaseOffset();
		return result;
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
	 * 		The list of corresponding article objets.
	 * 
	 * @throws ReaderException
	 * 		Problem while retrieving a Web page.
	 * @throws IOException
	 * 		Problem while retrieving a Web page.
	 * @throws ParseException
	 * 		Problem while retrieving a Web page.
	 * @throws SAXException
	 * 		Problem while retrieving a Web page.
	 */
	private List<Article> retrieveArticles(List<URL> urls) throws ReaderException, IOException, ParseException, SAXException
	{	logger.log("Starting the article retrieval");
		logger.increaseOffset();
		
		// retrieve articles
		List<Article> result = new ArrayList<Article>();
		ArticleRetriever articleRetriever = new ArticleRetriever(false); //TODO cache disabled for debugging
		for(URL url: urls)
		{	logger.log("Retrieving article at URL "+url.toString());
			Article article = articleRetriever.process(url);
			result.add(article);
		}
		
		logger.decreaseOffset();
		logger.log("Article retrieval complete");
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// ENTITIES		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Tool used to detect named entities in the text */ 
	private AbstractRecognizer recognizer;
	
	/**
	 * Initializes the named entity detection tool, 
	 * which will be applied to identify names and dates in
	 * the retrieved articles.
	 * 
	 * @throws RecognizerException
	 * 		Problem while initializing the recognizer. 
	 */
	private void initDefaultRecognizer() throws RecognizerException
	{	recognizer = new StraightCombiner();
	}
	
	/**
	 * Detects the entities present in each specified article.
	 * 
	 * @param articles
	 * 		The list of articles to process.
	 * @return
	 * 		A list of entites for each article.
	 * @throws RecognizerException
	 * 		Problem while applying the NER tool.
	 */
	private List<Entities> detectEntities(List<Article> articles) throws RecognizerException
	{	List<Entities> result = new ArrayList<Entities>();
		
		for(Article article: articles)
		{	Entities entities = recognizer.process(article);
			result.add(entities);
		}
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// FILTERING	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Removes from the list the articles concerning events
	 * not contained in the specified date range.
	 *  
	 * @param articles
	 * 		List of articles to process.
	 * @param entities
	 * 		List of the entities detected in the listed articles.
	 * @param startDate
	 * 		Start of the time period.
	 * @param endDate
	 * 		End of the time period.
	 * @param strictSearch
	 * 		Whether the filtering should be applied ({@code false}
	 * 		or not ({@code true}).
	 */
	private void filterArticles(List<Article> articles, List<Entities> entities, Date startDate, Date endDate, boolean strictSearch)
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
		logger.decreaseOffset();

		// possibly filter the resulting texts
		if(!strictSearch)
		{	if(startDate==null || endDate==null)
				logger.log("WARNING: one date is null, so both of them are ignored");
			else
			{	Iterator<Article> itArt = articles.iterator();
				Iterator<Entities> itEnt = entities.iterator();
				while(itArt.hasNext())
				{	Article article = itArt.next();
					String rawText = article.getRawText();
					Entities ents = itEnt.next();
					//TODO check if the article contains a date between start and end
				}
			}
		}
		
		logger.decreaseOffset();
		logger.log("Article filtering complete");
	}
}
