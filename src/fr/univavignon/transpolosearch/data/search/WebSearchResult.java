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

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.SAXException;

import fr.univavignon.transpolosearch.data.event.Event;
import fr.univavignon.transpolosearch.retrieval.ArticleRetriever;
import fr.univavignon.transpolosearch.retrieval.reader.ReaderException;
import fr.univavignon.transpolosearch.tools.file.FileNames;
import fr.univavignon.transpolosearch.tools.time.Period;
import fr.univavignon.transpolosearch.tools.time.TimeFormatting;

/**
 * Represents one result of a Web search engine (a URL) and some info
 * regarding how it was subsequently processed.
 * 
 * @author Vincent Labatut
 */
public class WebSearchResult extends AbstractSearchResult
{
	/**
	 * Initializes the Web search result.
	 * 
	 * @param url
	 * 		Address associated to the search result.
	 */
	public WebSearchResult(String url)
	{	super();
		this.url = url;
	}
	
	/////////////////////////////////////////////////////////////////
	// URL			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** URL associated to the result */
	public String url;
	
	/**
	 * Decides whether or not this result should be filtered depending on
	 * its url, and updates its status if needed.
	 * 
	 * @return
	 * 		{@code true} iff the result was filtered, i.e. it cannot be
	 * 		processed further.
	 */
	protected boolean filterUrl()
	{	boolean result = false;
		
		// we don't process PDF files
		if(url.endsWith(FileNames.EX_PDF))
		{	logger.log("The following URL points towards a PDF, we cannot currently use it: "+url);
			status = "PDF file";
			result = true;
		}
		
		else
			logger.log("We keep the URL "+url);
		
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// ENGINES		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Ranks of this result according to the search engines who returned it */
	public Map<String,String> ranks = new HashMap<String,String>();
	
	/**
	 * Indicates the rank the specified engine gave to this result.
	 * 
	 * @param engineName
	 * 		Name of the concerned search engine.
	 * @param rank
	 * 		Rank given by search engine.
	 */
	public void addEngine(String engineName, String rank)
	{	ranks.put(engineName,rank);
	}
	
	/////////////////////////////////////////////////////////////////
	// ARTICLE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Retrieve the article located at the URL associated to this result.
	 * 
	 * @param articleRetriever
	 * 		Object to use to retrieve the article.
	 * @param nbr
	 * 		Number of this result in the collection.
	 * @return
	 * 		{@code true} iff the article could be retrieved.
	 * 
	 * @throws ParseException
	 * 		Problem while retrieving the article.
	 * @throws SAXException
	 * 		Problem while retrieving the article.
	 * @throws IOException
	 * 		Problem while retrieving the article.
	 */
	protected boolean retrieveArticle(ArticleRetriever articleRetriever, int nbr) throws ParseException, SAXException, IOException
	{	boolean result = true;
		
		logger.log("Retrieving article #"+nbr+" at URL "+url);
		try
		{	article = articleRetriever.process(url);
		}
		catch (ReaderException e)
		{	logger.log("WARNING: Could not retrieve the article at URL "+url.toString()+" >> removing it from the result list.");
			status = "Article unvailable";
			result = false;
		}
		
		return result;
	}
	
	@Override
	protected boolean filterByKeyword(String compulsoryExpression, int nbr)
	{	boolean result = true;
		
		logger.log("Processing article "+article.getTitle()+" ("+nbr+")");
		logger.increaseOffset();
		{	String rawText = article.getRawText();
			if(!rawText.contains(compulsoryExpression))
			{	logger.log("Discarding article "+article.getTitle()+" ("+article.getUrl()+")");
				status = "Missing keyword";
				result = false;
			}
		}
		logger.decreaseOffset();
			
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// EVENTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected int extractEvents(boolean bySentence, int nbr)
	{	int result = extractEvents(bySentence,nbr,true);
		return result;
	}
	
	/**
	 * Records the results of the web search as a CSV file.
	 * 
	 * @return
	 * 		Map representing the events associated to this Web
	 * 		search result (can be empty). 
	 */
	@Override
	protected List<Map<String,String>> exportEvents()
	{	List<Map<String,String>> result = new ArrayList<Map<String,String>>();
		
		int rank = 0;
		for(Event event: events)
		{	Map<String,String> map = new HashMap<String,String>();
			result.add(map);
			rank++;

			// general stuff
			map.put(WebSearchResults.COL_TITLE,"\""+article.getTitle()+"\"");
			map.put(WebSearchResults.COL_URL,"\""+article.getUrl().toString()+"\"");
			map.put(WebSearchResults.COL_LENGTH,"\""+article.getRawText().length()+"\"");
			map.put(WebSearchResults.COL_STATUS,status);
			map.put(WebSearchResults.COL_COMMENTS,"");
			
			// publication date
			java.util.Date pubDate = article.getPublishingDate();
			if(pubDate!=null)
			{	String pubDateStr = TimeFormatting.formatDate(pubDate);
				map.put(WebSearchResults.COL_PUB_DATE,pubDateStr);
			}
			
			// author(s)
			List<String> authors = article.getAuthors();
			if(!authors.isEmpty())
			{	Iterator<String> it = authors.iterator();
				String authorStr = it.next();
				String authorsStr = "\"" + authorStr;
				while(it.hasNext())
				{	authorStr = it.next();
					authorsStr = authorsStr + ", " + authorStr;
				}
				authorsStr = authorsStr + "\"";
				map.put(WebSearchResults.COL_AUTHORS,authorsStr);
			}
			
			// search engine ranks
			for(Entry<String,String> entry: ranks.entrySet())
			{	String engineName = entry.getKey();
				String rk = entry.getValue();
				map.put(WebSearchResults.COL_RANK+engineName,rk);
			}

			if(event!=null)
			{	map.put(WebSearchResults.COL_EVENT_RANK,Integer.toString(rank));
				
				// dates
				Period period = event.getPeriod();
				String periodStr = period.toString();
				periodStr = periodStr.replaceAll("[\\n\\r]", " ");
				periodStr = periodStr.replaceAll("\"", "'");
				map.put(WebSearchResults.COL_EVENT_DATES,periodStr);
				
				// locations
				{	String locations = "\"";
					Collection<String> locs = event.getLocations();
					Iterator<String> itLoc = locs.iterator();
					while(itLoc.hasNext())
					{	String loc = itLoc.next();
						loc = loc.replaceAll("[\\n\\r]", " ");
						loc = loc.replaceAll("\"", "'");
						locations = locations + loc;
						if(itLoc.hasNext())
							locations = locations + ", ";
					}
					locations = locations + "\"";
					map.put(WebSearchResults.COL_EVENT_LOCATIONS,locations);
				}
				
				// persons
				{	String persons = "\"";
					Collection<String> perss = event.getPersons();
					Iterator<String> itPers = perss.iterator();
					while(itPers.hasNext())
					{	String pers = itPers.next();
						pers = pers.replaceAll("[\\n\\r]", " ");
						pers = pers.replaceAll("\"", "'");
						persons = persons + pers;
						if(itPers.hasNext())
							persons = persons + ", ";
					}
					persons = persons + "\"";
					map.put(WebSearchResults.COL_EVENT_PERSONS,persons);
				}
				
				// organizations
				{	String organizations = "\"";
					Collection<String> orgs = event.getOrganizations();
					Iterator<String> itOrg = orgs.iterator();
					while(itOrg.hasNext())
					{	String org = itOrg.next();
						org = org.replaceAll("[\\n\\r]", " ");
						org = org.replaceAll("\"", "'");
						organizations = organizations + org;
						if(itOrg.hasNext())
							organizations = organizations + ", ";
					}
					organizations = organizations + "\"";
					map.put(WebSearchResults.COL_EVENT_ORGANIZATIONS,organizations);
				}
				
				// functions
				{	String functions = "\"";
					Collection<String> funs = event.getFunctions();
					Iterator<String> itFun = funs.iterator();
					while(itFun.hasNext())
					{	String fun = itFun.next();
						fun = fun.replaceAll("[\\n\\r]", " ");
						fun = fun.replaceAll("\"", "'");
						functions = functions + fun;
						if(itFun.hasNext())
							functions = functions + ", ";
					}
					functions = functions + "\"";
					map.put(WebSearchResults.COL_EVENT_FUNCTIONS,functions);
				}
				
				// productions
				{	String productions = "\"";
					Collection<String> prods = event.getProductions();
					Iterator<String> itProd = prods.iterator();
					while(itProd.hasNext())
					{	String prod = itProd.next();
						prod = prod.replaceAll("[\\n\\r]", " ");
						prod = prod.replaceAll("\"", "'");
						productions = productions + prod;
						if(itProd.hasNext())
							productions = productions + ", ";
					}
					productions = productions + "\"";
					map.put(WebSearchResults.COL_EVENT_PRODUCTIONS,productions);
				}
				
				// meetings
				{	String meetings = "\"";
					Collection<String> meets = event.getMeetings();
					Iterator<String> itMeet = meets.iterator();
					while(itMeet.hasNext())
					{	String meet = itMeet.next();
						meet = meet.replaceAll("[\\n\\r]", " ");
						meet = meet.replaceAll("\"", "'");
						meetings = meetings + meet;
						if(itMeet.hasNext())
							meetings = meetings + ", ";
					}
					meetings = meetings + "\"";
					map.put(WebSearchResults.COL_EVENT_MEETINGS,meetings);
				}
			}
		}
		
		return result;
	}
}
