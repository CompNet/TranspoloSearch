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
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;

import org.xml.sax.SAXException;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.entity.AbstractEntity;
import fr.univavignon.transpolosearch.data.entity.Entities;
import fr.univavignon.transpolosearch.data.entity.EntityDate;
import fr.univavignon.transpolosearch.data.entity.EntityLocation;
import fr.univavignon.transpolosearch.data.entity.EntityOrganization;
import fr.univavignon.transpolosearch.data.entity.EntityPerson;
import fr.univavignon.transpolosearch.data.entity.EntityType;
import fr.univavignon.transpolosearch.data.event.Event;
import fr.univavignon.transpolosearch.recognition.AbstractRecognizer;
import fr.univavignon.transpolosearch.recognition.RecognizerException;
import fr.univavignon.transpolosearch.recognition.combiner.straightcombiner.StraightCombiner;
import fr.univavignon.transpolosearch.retrieval.ArticleRetriever;
import fr.univavignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univavignon.transpolosearch.search.web.AbstractWebEngine;
import fr.univavignon.transpolosearch.search.web.GoogleEngine;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.file.FileTools;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;
import fr.univavignon.transpolosearch.tools.string.StringTools;

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
	 * @throws RecognizerException 
	 * 		Problem while detecting the entities.
	 */
	public void performExtraction(String keywords, String website, Date startDate, Date endDate, boolean strictSearch, String compulsoryExpression) throws IOException, ReaderException, ParseException, SAXException, RecognizerException
	{	logger.log("Starting the information extraction");
		logger.increaseOffset();
		
		// perform the Web search
		List<URL> originalUrls = performWebSearch(keywords, website, startDate, endDate, strictSearch);
		
		// filter Web pages (remove PDFs, an so on)
		List<URL> filteredUrls = new ArrayList<URL>(originalUrls);
		filterUrls(filteredUrls);
		
		// retrieve the corresponding articles
		List<Article> originalArticles = retrieveArticles(filteredUrls);
		
		// detect the entities
		List<Entities> originalEntities = detectEntities(originalArticles);
		
		// possibly filter the articles depending on the dates and compulsory expression
		List<Article> filteredArticles = new ArrayList<Article>(originalArticles);
		List<Entities> filteredentities = new ArrayList<Entities>(originalEntities);
		filterArticles(filteredArticles,filteredentities,startDate,endDate,strictSearch,compulsoryExpression);
		
		// displays the remaining articles with their entities
		displayRemainingEntities(filteredArticles,filteredentities); //TODO for debug only
		
		// extract events from the remaining articles and entities
		boolean bySentence = false; //TODO for debug
		List<List<Event>> events = extractEvents(filteredArticles,filteredentities,bySentence);
		
		// export the events as a table
		exportEvents(originalUrls, filteredUrls, originalArticles, filteredArticles, events);
		
		logger.decreaseOffset();
		logger.log("Information extraction over");
	}

	/////////////////////////////////////////////////////////////////
	// WEB SEARCH	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of engines used for the Web search */
	private final List<AbstractWebEngine> engines = new ArrayList<AbstractWebEngine>();
	
	/**
	 * Initializes the default search engines.
	 * Currently: only Google Custom Search.
	 * (others can easily be added).
	 */
	private void initDefaultSearchEngines()
	{	// set up the google custom search
		GoogleEngine googleEngine = new GoogleEngine();
		engines.add(googleEngine);
		
		// set up Bing
		//  TODO
		
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
	{	boolean cachedSearch = true; //TODO for debug
		String cacheFilePath = FileNames.FO_OUTPUT + File.separator + FileNames.FI_SEARCH_RESULTS;
		File cacheFile = new File(cacheFilePath);
		
		List<URL> result;
		
		// use cached results
		if(cachedSearch && cacheFile.exists())
		{	logger.log("Loading the previous search results");
			result = new ArrayList<URL>();
			Scanner sc = FileTools.openTextFileRead(cacheFile);
			while(sc.hasNextLine())
			{	String urlStr = sc.nextLine();
				URL url = new URL(urlStr);
				result.add(url);
			}
			logger.log("Total number of pages loaded: "+result.size());
		}
		
		// search the results
		else
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
			Set<URL> set = new TreeSet<URL>(new Comparator<URL>()
			{	@Override
				public int compare(URL url1, URL url2)
				{	int result = url1.toString().compareTo(url2.toString());
					return result;
				}	
			});
			for(AbstractWebEngine engine: engines)
			{	logger.log("Applying search engine "+engine.getName());
				logger.increaseOffset();
					List<URL> temp = engine.search(keywords,website,startDate,endDate);
					set.addAll(temp);
				logger.decreaseOffset();
			}
			
			result = new ArrayList<URL>(set);
			logger.log("Total number of pages found: "+result.size());
			
			String filePath = FileNames.FO_OUTPUT + File.separator + FileNames.FI_SEARCH_RESULTS;
			logger.log("Recording all URLs in text file \""+filePath+"\"");
			PrintWriter pw = FileTools.openTextFileWrite(filePath);
			for(URL url: result)
				pw.println(url.toString());
			pw.close();
			
			logger.decreaseOffset();
		}
		
		return result;
	}
	
	/**
	 * Removes from the specified list the URLs
	 * which are not treatable.
	 * 
	 * @param urls
	 * 		List of Web addresses.
	 */
	private void filterUrls(List<URL> urls)
	{	logger.log("Filtering the retrieved URL to remove those we can't treat");
		logger.increaseOffset();
		
		Iterator<URL> it = urls.iterator();
		while(it.hasNext())
		{	URL url = it.next();
			String urlStr = url.toString();
			
			// we don't propcess PDF files
			if(urlStr.endsWith(FileNames.EX_PDF))
			{	logger.log("The following URL points towards a PDF, we can't use it: "+urlStr);
				it.remove();
			}
			
			else
				logger.log("We keep the URL "+urlStr);
		}
		
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
	 * 		The list of corresponding article objets.
	 * 
	 * @throws IOException
	 * 		Problem while retrieving a Web page.
	 * @throws ParseException
	 * 		Problem while retrieving a Web page.
	 * @throws SAXException
	 * 		Problem while retrieving a Web page.
	 */
	private List<Article> retrieveArticles(List<URL> urls) throws IOException, ParseException, SAXException
	{	logger.log("Starting the article retrieval");
		logger.increaseOffset();
		
		// init
		List<Article> result = new ArrayList<Article>();
		ArticleRetriever articleRetriever = new ArticleRetriever(true); //TODO cache disabled for debugging
		articleRetriever.setLanguage(ArticleLanguage.FR); // we know the articles will be in French (should be genralized later)

		// retrieve articles
		Iterator<URL> it = urls.iterator();
		int i = 0;
		while(it.hasNext())
		{	URL url = it.next();
			i++;
			logger.log("Retrieving article ("+i+"/"+urls.size()+") at URL "+url.toString());
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
		recognizer.setCacheEnabled(true);//TODO false for debugging
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
	{	logger.log("Detecting entities in all "+articles.size()+" articles");
		logger.increaseOffset();
		List<Entities> result = new ArrayList<Entities>();
		
		for(Article article: articles)
		{	logger.log("Processing article "+article.getTitle()+"("+article.getUrl()+")");
			logger.increaseOffset();
				Entities entities = recognizer.process(article);
				result.add(entities);
				
				logger.log("Found "+entities.getEntities().size()+" entities");
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
	 * the article not containing the compulsory expression.
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
	 * 		Whether the filtering should be applied ({@code false})
	 * 		or not ({@code true}).
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article,
	 * 		or {@code null} if there's no such constraint.
	 */
	private void filterArticles(List<Article> articles, List<Entities> entities, Date startDate, Date endDate, boolean strictSearch, String compulsoryExpression)
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
				Iterator<Entities> itEnt = entities.iterator();
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
					Iterator<Entities> itEnt = entities.iterator();
					int count = 0;
					while(itArt.hasNext())
					{	Article article = itArt.next();
						Entities ents = itEnt.next();
						logger.log("Processing article "+article.getTitle());
						List<AbstractEntity<?>> dates = ents.getEntitiesByType(EntityType.DATE);

						// check if the article contains a date between start and end
						boolean remove = dates.isEmpty();
						if(!remove)	
						{	fr.univavignon.transpolosearch.tools.time.Date date = null;
							Iterator<AbstractEntity<?>> it = dates.iterator();
							while(date==null && it.hasNext())
							{	AbstractEntity<?> entity = it.next();
								fr.univavignon.transpolosearch.tools.time.Date d = (fr.univavignon.transpolosearch.tools.time.Date) entity.getValue();
								if(d.isContained(start, end))
									date = d;
							}
							
							if(date!=null)
							{	logger.log("Found date "+date+" in article "+article.getTitle()+" >> removal ("+article.getUrl()+")");
								remove = true;
							}
						}
						
						// possibly remove the article/entities
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
	 * Displays the entities associated to each article.
	 * 
	 * @param articles
	 * 		List of articles.
	 * @param entities
	 * 		List of associated entities.
	 */
	private void displayRemainingEntities(List<Article> articles, List<Entities> entities)
	{	logger.log("Displaying remaining articles and entities");
		logger.increaseOffset();
		
		Iterator<Article> itArt = articles.iterator();
		Iterator<Entities> itEnt = entities.iterator();
		int count = 0;
		while(itArt.hasNext())
		{	Article article = itArt.next();
			Entities ents = itEnt.next();
			logger.log("Article: "+article.getTitle()+" ("+count+"/"+articles.size()+")");
			logger.increaseOffset();
				count++;
				List<AbstractEntity<?>> dates = ents.getEntitiesByType(EntityType.DATE);
				if(!dates.isEmpty())
				{	String first = "Dates ("+dates.size()+"):";
					List<String> msg = new ArrayList<String>();
					msg.add(first);
					for(AbstractEntity<?> entity: dates)
						msg.add(entity.toString());
					logger.log(msg);
				}
				List<AbstractEntity<?>> locations = ents.getEntitiesByType(EntityType.LOCATION);
				if(!locations.isEmpty())
				{	String first = "Locations ("+locations.size()+"):";
					List<String> msg = new ArrayList<String>();
					msg.add(first);
					for(AbstractEntity<?> entity: locations)
						msg.add(entity.toString());
					logger.log(msg);
				}
				List<AbstractEntity<?>> organizations = ents.getEntitiesByType(EntityType.ORGANIZATION);
				if(!organizations.isEmpty())
				{	String first = "Organizations ("+organizations.size()+"):";
					List<String> msg = new ArrayList<String>();
					msg.add(first);
					for(AbstractEntity<?> entity: organizations)
						msg.add(entity.toString());
					logger.log(msg);
				}
				List<AbstractEntity<?>> persons = ents.getEntitiesByType(EntityType.PERSON);
				if(!persons.isEmpty())
				{	String first = "Persons ("+persons.size()+"):";
					List<String> msg = new ArrayList<String>();
					msg.add(first);
					for(AbstractEntity<?> entity: persons)
						msg.add(entity.toString());
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
	 * Takes a list of articles and a list of the corresponding entities,
	 * and identifies the events described in the articles.
	 * 
	 * @param articles
	 * 		List of articles to treat.
	 * @param entities
	 * 		List of the associated entities.*
	 * @param bySentence
	 * 		Whether to retrieve events by sentence (all event-related entities
	 * 		must be in the same sentence) or by article.
	 * @return
	 * 		The resulting list of events, for each article.
	 */
	private List<List<Event>> extractEvents(List<Article> articles, List<Entities> entities, boolean bySentence)
	{	logger.log("Extracting events");
		logger.increaseOffset();
		List<List<Event>> result = new ArrayList<List<Event>>();
		
		// processing each article
		Iterator<Article> itArt = articles.iterator();
		Iterator<Entities> itEnt = entities.iterator();
		int count = 0;
		int eventNbr = 0;
		while(itArt.hasNext())
		{	Article article = itArt.next();
			Entities ents = itEnt.next();
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
					
					// for each sentence, we get the detected entities
					for(int ep: sentencePos)
					{	if(sp>=0)
						{	List<AbstractEntity<?>> le = ents.getEntitiesIn(sp, ep);
							List<AbstractEntity<?>> dates = Entities.filterByType(le,EntityType.DATE);
							// only go on if there is at least one date
							if(!dates.isEmpty())
							{	if(dates.size()>1)
									logger.log("WARNING: there are several dates in sentence \""+rawText.substring(sp,ep)+"\"");
								else
								{	EntityDate ed = (EntityDate)dates.get(0);
									List<AbstractEntity<?>> persons = Entities.filterByType(le,EntityType.PERSON);
									if(persons.isEmpty())
										logger.log("WARNING: there is a date ("+ed.getValue()+") but no persons in sentence \""+rawText.substring(sp,ep)+"\"");
									else
									{	Event event = new Event(ed);
										events.add(event);
										eventNbr++;
										for(AbstractEntity<?> entity: persons)
										{	EntityPerson person = (EntityPerson)entity;
											event.addPerson(person);
										}
										List<AbstractEntity<?>> organizations = Entities.filterByType(le,EntityType.ORGANIZATION);
										for(AbstractEntity<?> entity: organizations)
										{	EntityOrganization organization = (EntityOrganization)entity;
											event.addOrganization(organization);
										}
										List<AbstractEntity<?>> locations = Entities.filterByType(le,EntityType.LOCATION);
										for(AbstractEntity<?> entity: locations)
										{	EntityLocation location = (EntityLocation)entity;
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
				{	List<AbstractEntity<?>> dates = ents.getEntitiesByType(EntityType.DATE);
					// only go on if there is at least one date
					if(!dates.isEmpty())
					{	Event event;
						if(dates.size()>1)
						{	logger.log("There are several ("+dates.size()+") dates in the article >> merging them");
							Iterator<AbstractEntity<?>> it = dates.iterator();
							EntityDate ed = (EntityDate)it.next();
							event = new Event(ed);
							while(it.hasNext())
							{	ed = (EntityDate)it.next();
								fr.univavignon.transpolosearch.tools.time.Date d = ed.getValue(); 
								event.mergeDate(d);
							}
						}
						else
						{	EntityDate esd = (EntityDate)dates.get(0);
							event = new Event(esd);
						}
						
						List<AbstractEntity<?>> persons = ents.getEntitiesByType(EntityType.PERSON);
						if(persons.isEmpty())
							logger.log("WARNING: there is a date ("+event.getStartDate()+") but no person in article \""+article.getTitle()+"\"");
						else
						{	events.add(event);
							eventNbr++;
							
							for(AbstractEntity<?> entity: persons)
							{	EntityPerson person = (EntityPerson)entity;
								event.addPerson(person);
							}
							List<AbstractEntity<?>> organizations = ents.getEntitiesByType(EntityType.ORGANIZATION);
							for(AbstractEntity<?> entity: organizations)
							{	EntityOrganization organization = (EntityOrganization)entity;
								event.addOrganization(organization);
							}
							List<AbstractEntity<?>> locations = ents.getEntitiesByType(EntityType.LOCATION);
							for(AbstractEntity<?> entity: locations)
							{	EntityLocation location = (EntityLocation)entity;
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
//	 * Takes a list of entities and returns the list of
//	 * corresponding strings.
//	 * 
//	 * @param entities
//	 * 		List of entities.
//	 * @return
//	 * 		List of the associated strings.
//	 */
//	private List<String> extractEntityNames(List<AbstractEntity<?>> entities)
//	{	List<String> result = new ArrayList<String>();
//		
//		for(AbstractEntity<?> entity: entities)
//		{	Object object = entity.getValue();
//			String str = object.toString();
//			result.add(str);
//		}
//		
//		return result;
//	}
	
	/**
	 * Record the result of the search as a CSV file.
	 * 
	 * @param originalUrls
	 * 		List of treated URLs.
	 * @param filteredUrls
	 * 		List of remaning URLs after filtering.
	 * @param originalArticles
	 * 		List of treated articles.
	 * @param filteredArticles
	 * 		List of remaning articles after filtering.
	 * @param events
	 * 		List of detected events.
	 * 
	 * @throws UnsupportedEncodingException
	 * 		Problem while accessing to the result file.
	 * @throws FileNotFoundException
	 * 		Problem while accessing to the result file.
	 */
	private void exportEvents(List<URL> originalUrls, List<URL> filteredUrls, List<Article> originalArticles, List<Article> filteredArticles, List<List<Event>> events) throws UnsupportedEncodingException, FileNotFoundException
	{	String filePath = FileNames.FO_OUTPUT + File.separator + FileNames.FI_EVENT_TABLE;
		logger.log("Recording the avents as a CVS file: "+filePath);
		logger.decreaseOffset();
		
		PrintWriter pw = FileTools.openTextFileWrite(filePath);
		
		// write header
		List<String> colNames = Arrays.asList(
			"Rang",
			"Titre page",
			"Adresse",
			"Date",
			"Contenu",
			"Actif",
			"Concerne CH",
			"Type support",
			"Source",
			"Type contenu",
			"Thème",
			"Evénement",
			"Date",
			"Heure",
			"Pertinence Période",
			"Lieu",
			"Commentaires",
			"Rang événement"
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
		int j = 1;
		for(URL url: originalUrls)
		{	String urlStr = url.toString();
			String address = "\""+urlStr+"\"";
			int indexUrl = filteredUrls.indexOf(url);
			
			// URL not processed
			if(indexUrl==-1)
			{	if(urlStr.endsWith(FileNames.EX_PDF))
					pw.print(
						i+",,"+
						address+",,,,,"+
						"PDF,,,,,,,,,"+
						"URL filtrée,"
					);
				else
					pw.print(
						i+",,"+
						address+",,,,,,,,,,,,,,"+
						"URL filtrée,"
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
						"Non,,,,,,,,,,"+
						"Article filtré,"
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
							"Oui,,,,,,,,,,"+
							"Aucun évènement détecté,"+j
						);

					// at least one event detected
					else
					{	for(Event event: eventList)
						{	// get the dates
							fr.univavignon.transpolosearch.tools.time.Date startDate = event.getStartDate();
							fr.univavignon.transpolosearch.tools.time.Date endDate = event.getEndDate();
							String date = startDate.toString();
							if(endDate!=null)
								date = date + "-" + endDate.toString();
							
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
							{	String ent = itPo.next();
								persOrgs = persOrgs + ent;
								if(itPo.hasNext())
									persOrgs = persOrgs + ", ";
							}
							persOrgs = persOrgs + "\"";
						
							// write the row
							pw.print(
								i+","+
								title+","+
								address+",,,,"+
								"Oui,,,,,,"+
								date+",,,"+
								locations+","+
								persOrgs+","+j
							);
						}
					}
					j++;
				}
			}
			
			pw.println();
			i++;
		}
		
		pw.close();
		logger.decreaseOffset();
	}
}
