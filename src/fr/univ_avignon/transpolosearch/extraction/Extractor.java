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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.api.services.customsearch.model.Result;

import fr.univ_avignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univ_avignon.transpolosearch.tools.log.HierarchicalLoggerManager;
import fr.univ_avignon.transpolosearch.websearch.AbstractEngine;
import fr.univ_avignon.transpolosearch.websearch.GoogleEngine;

/**
 * This class handles the main search, i.e. it :
 * <ol>
 * 	<li>determines which articles are relevant, using a Web search engine</li>
 * 	<li>retrieves them using the article reader</li>
 * 	<li>detects the named entities they contain</li>
 * 	<li>records the corresponding events</li>
 * <ol>
 * 
 * @author Vincent Labatut
 */
public class Extractor
{	
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
	 * @param person
	 * 		Person we want to look for.
	 * @param startDate
	 * 		Start of the period we want to consider.
	 * @param endDate
	 * 		End of the period we want to consider.
	 * @param strictSearch
	 * 		If {@code true}, both dates will be used directly in the Web search.
	 * 		Otherwise, they will be used <i>a posteri</i> to filter the detected events.
	 */
	public void search(String person, Date startDate, Date endDate, boolean strictSearch)
	{	
		// perform the Web search
		
	}

	/////////////////////////////////////////////////////////////////
	// WEB SEARCH	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of engines used for the Web search */
	private final List<AbstractEngine> engines = new ArrayList<AbstractEngine>();
	
	
	private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	
	private static List<URL> webSearch(String person, Date startDate, Date endDate, boolean strictSearch) throws IOException
	{	
		
	}
	
	private static List<URL> googleSearch(String person, Date startDate, Date endDate, boolean strictSearch) throws IOException
	{	// init the google engine
		GoogleEngine gs = new GoogleEngine();
		// number of results
		gs.resultNumber = 200;
		String query = person;
		if(strictSearch)
		{	// sort by date
			gs.sortByDate = true;
			// date range
			gs.dateRange = DATE_FORMAT.format(startDate)+":"+DATE_FORMAT.format(endDate);
		}
		else
		{	// query
			//query = person + " " + DATE_FORMAT.format(startDate) + "-" + DATE_FORMAT.format(endDate);
			// sort by date
//			gs.sortByDate = true;	// otherwise: sort by relevance (by default)
		}

		
		return result;
	}
}
