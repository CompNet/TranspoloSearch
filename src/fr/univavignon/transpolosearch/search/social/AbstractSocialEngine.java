package fr.univavignon.transpolosearch.search.social;

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
import java.util.Date;
import java.util.List;

import fr.univavignon.transpolosearch.data.search.SocialSearchResult;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;

/**
 * This class represents a search engine one can use to return
 * a list of posts from a specific social media.
 * 
 * @author Vincent Labatut
 */
public abstract class AbstractSocialEngine
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
	 * Performs a search using the corresponding engine. The method
	 * returns the posts published on the page corresponding to the 
	 * specified keywords, as weel as the direct comments of these
	 * posts. If {@code extendedSearch} is {@code true}, the method
	 * also returns the posts of the commenting users for the same
	 * period.
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
	 * 		If {@code true}, the method returns the posts by the commenting
	 * 		users, for the specified period. 
	 * @return
	 * 		List of posts and comments.
	 * 
	 * @throws IOException
	 * 		Problem while searching the Web.
	 */
	public abstract List<SocialSearchResult> search(String keywords, Date startDate, Date endDate, boolean extendedSearch)  throws IOException;
}
