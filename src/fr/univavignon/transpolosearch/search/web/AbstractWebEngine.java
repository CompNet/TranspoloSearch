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

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;

/**
 * This class represents a search engine one can use to return
 * a list of articles from the general Web.
 * 
 * @author Vincent Labatut
 */
public abstract class AbstractWebEngine
{	
	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	public static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();

	/////////////////////////////////////////////////////////////////
	// NAME			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns a String representing the name
	 * of this search engine.
	 * 
	 * @return
	 * 		Name of this search engine.
	 */
	public abstract String getName();
	
	/////////////////////////////////////////////////////////////////
	// SEARCH		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Performs a search using the corresponding engine.
	 * 
	 * @param keywords
	 * 		Person we want to look for.
	 * @param website
	 * 		Target site, or {@code null} to search the whole Web.
	 * @param startDate
	 * 		Start of the period we want to consider, 
	 * 		or {@code null} for no constraint.
	 * @param endDate
	 * 		End of the period we want to consider,
	 * 		or {@code null} for no constraint.
	 * @return
	 * 		List of results taking the form of URLs.
	 * 
	 * @throws IOException
	 * 		Problem while searching the Web.
	 */
	public abstract List<URL> search(String keywords, String website, Date startDate, Date endDate)  throws IOException;

	/////////////////////////////////////////////////////////////////
	// STRING		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public String toString()
	{	return getName();
	}
}
