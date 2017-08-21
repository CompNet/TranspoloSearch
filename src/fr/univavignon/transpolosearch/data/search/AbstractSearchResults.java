package fr.univavignon.transpolosearch.data.search;

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
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import fr.univavignon.transpolosearch.processing.InterfaceRecognizer;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;

/**
 * Collection of search results returned by a collection of
 * search engines, with additional info resulting from their
 * subsequent processing.
 * 
 * @param <T>
 * 		Type of results handled by this class. 
 * 
 * @author Vincent Labatut
 */
public abstract class AbstractSearchResults<T extends AbstractSearchResult>
{
	/**
	 * Initializes the search result.
	 */
	public AbstractSearchResults()
	{	
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	public static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();
	
	/////////////////////////////////////////////////////////////////
	// ENGINES		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of search engines involved in the current search */
	protected Set<String> engineNames = new TreeSet<String>();
	
	/////////////////////////////////////////////////////////////////
	// RESULTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Map of results */
	protected Map<String,T> results = new HashMap<String,T>();
	
	/**
	 * Returns the number of entries in this collection of search results.
	 * 
	 * @return
	 * 		Number of entries in this map.
	 */
	public int size()
	{	int result = results.size();
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// FILTERING	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Discards results describing only events not contained 
	 * in the specified date range.
	 *  
	 * @param startDate
	 * 		Start of the time period.
	 * @param endDate
	 * 		End of the time period.
	 */
	private void filterByDate(Date startDate, Date endDate)
	{	logger.log("Removing articles not fitting the date constraints: "+startDate+"->"+endDate);
		logger.increaseOffset();
			fr.univavignon.transpolosearch.tools.time.Date start = new fr.univavignon.transpolosearch.tools.time.Date(startDate);
			fr.univavignon.transpolosearch.tools.time.Date end = new fr.univavignon.transpolosearch.tools.time.Date(endDate);
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	if(result.status==null)
				{	total++;
					if(!result.filterByDate(start,end,total))
						count++;
				}
			}
		logger.decreaseOffset();
		logger.log("Date-based filtering complete: "+count+"/"+total);
	}

	/**
	 * Discards results corresponding only to articles not containing 
	 * the compulsory expression.
	 *  
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article.
	 */
	private void filterByKeyword(String compulsoryExpression)
	{	logger.log("Discarding articles not containing the compulsory expression \""+compulsoryExpression+"\"");
		logger.increaseOffset();
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	
if(result instanceof WebSearchResult && ((WebSearchResult)result).url.equalsIgnoreCase
		("http://www.lamarseillaise.fr/vaucluse/developpement-durable/58144-avignon-ca-bouge-autour-du-technopole-de-l-agroparc"))				
	System.out.print("");
				if(result.status==null)
				{	total++;
					if(!result.filterByKeyword(compulsoryExpression,total))
						count++;
				}
			}
		logger.decreaseOffset();
		logger.log("Keyword-based filtering complete: "+count+"/"+total);
	}

	/**
	 * Discards results describing only events not contained 
	 * in the specified date range, or not containing the 
	 * compulsory expression.
	 *  
	 * @param startDate
	 * 		Start of the time period.
	 * @param endDate
	 * 		End of the time period.
	 * @param searchDate
	 * 		Whether the date constraint was applied before ({@code true}) at search time,
	 * 		or should be applied <i>a posteriori</i> here ({@code false}).
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article,
	 * 		or {@code null} if there is no such constraint.
	 */
	public void filterByContent(Date startDate, Date endDate, boolean searchDate, String compulsoryExpression)
	{	logger.log("Starting filtering the articles");
		logger.increaseOffset();
		
		// log stuff
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("startDate="+startDate);
			logger.log("endDate="+endDate);
			String txt = "searchDate="+searchDate;
			if(searchDate)
				txt = txt + "(dates are ignored here, because they were already used during the search)";
			logger.log(txt);
			logger.log("compulsoryExpression="+compulsoryExpression);
		logger.decreaseOffset();
		
		// possibly filter the resulting texts depending on the compulsory expression
		if(compulsoryExpression!=null)
			filterByKeyword(compulsoryExpression);
		else
			logger.log("No compulsory expression to process");

		// possibly filter the resulting texts depending on the dates they contain
		if(!searchDate)
		{	if(startDate==null || endDate==null)
				logger.log("WARNING: one date is null, so both of them are ignored");
			else
				filterByDate(startDate, endDate);
		}
		
		logger.decreaseOffset();
		logger.log("Article filtering complete");
	}
		
	/////////////////////////////////////////////////////////////////
	// MENTIONS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Detects the entity mentions present in each specified article.
	 * 
	 * @param recognizer
	 * 		The recognizer used to detect the mentions.
	 * @throws ProcessorException
	 * 		Problem while applying the NER tool.
	 */
	public void detectMentions(InterfaceRecognizer recognizer) throws ProcessorException
	{	logger.log("Detecting entity mentions in all the articles");
		logger.increaseOffset();
		
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	
if(result instanceof WebSearchResult && ((WebSearchResult)result).url.equalsIgnoreCase
		("http://www.lamarseillaise.fr/vaucluse/developpement-durable/58144-avignon-ca-bouge-autour-du-technopole-de-l-agroparc"))				
	System.out.print("");
				if(result.status==null)
				{	total++;
					if(result.detectMentions(recognizer,total)>0)
						count++;
				}
			}
		
		logger.decreaseOffset();
		logger.log("Mention detection complete: ("+count+"/"+total+")");
	}

	/**
	 * Displays the entity mentions associated to each remaining article.
	 */
	public void displayRemainingMentions()
	{	logger.log("Displaying remaining articles and entity mentions");
		logger.increaseOffset();
		
		int total = 0;
		for(T result: results.values())
		{	if(result.status==null)
			{	total++;
				result.displayRemainingMentions(total);
			}
		}
		
		logger.decreaseOffset();
		logger.log("Display complete");
	}
	
	/////////////////////////////////////////////////////////////////
	// EVENTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Identifies the events described in the articles associated to
	 * the search results.
	 * 
	 * @param bySentence
	 * 		Whether to retrieve events by sentence (all event-related entity mentions
	 * 		must be in the same sentence) or by article.
	 */
	public void extractEvents(boolean bySentence)
	{	logger.log("Extracting events from all the articles");
		logger.increaseOffset();
		
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	
if(result instanceof WebSearchResult && ((WebSearchResult)result).url.equalsIgnoreCase
		("http://www.lamarseillaise.fr/vaucluse/developpement-durable/58144-avignon-ca-bouge-autour-du-technopole-de-l-agroparc"))				
	System.out.print("");
				
				if(result.status==null)
				{	total++;
					if(result.extractEvents(bySentence,total)>0)
						count++;
				}
			}
		
		logger.decreaseOffset();
		logger.log("Event extraction complete: ("+count+"/"+total+")");
	}
	
	/////////////////////////////////////////////////////////////////
	// EXPORT		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Title of the Web resource */
	protected static final String COL_PAGE_TITLE = "Page title";
	/** URL of the Web resource */
	protected static final String COL_PAGE_URL = "URL";
	/** File format of the Web resource (HTML, PDF, etc.) */
	protected static final String COL_PAGE_STATUS = "Format";
	/** Publication date of the Web resource */
	protected static final String COL_PUB_DATE = "Publication date";
	/** Name of the social search engine */
	protected static final String COL_SOCIAL_ENGINE = "Social media";
	/** Rank according to some search engine */
	protected static final String COL_RANK = "Rank ";
	/** Rank of the event in the article */
	protected static final String COL_EVENT_RANK = "Event rank";
	/** Dates associated to the event */
	protected static final String COL_EVENT_DATES = "Dates";
	/** Locations associated to the event */
	protected static final String COL_EVENT_LOCATIONS = "Locations";
	/** Persons associated to the event */
	protected static final String COL_EVENT_PERSONS = "Persons";
	/** Organizations associated to the event */
	protected static final String COL_EVENT_ORGANIZATIONS = "Organizations";
	/** Personal roles associated to the event */
	protected static final String COL_EVENT_FUNCTIONS = "Functions";
	/** Intellectual productions associated to the event */
	protected static final String COL_EVENT_PRODUCTIONS = "Production";
	/** Meetings associated to the event */
	protected static final String COL_EVENT_MEETINGS = "Meetings";
	/** Misc comments */
	protected static final String COL_COMMENTS = "Comments";
	
	/**
	 * Records the results of the search as a CSV file.
	 * 
	 * @throws UnsupportedEncodingException
	 * 		Problem while accessing to the result file.
	 * @throws FileNotFoundException
	 * 		Problem while accessing to the result file.
	 */
	public abstract void exportEvents() throws UnsupportedEncodingException, FileNotFoundException;
}
