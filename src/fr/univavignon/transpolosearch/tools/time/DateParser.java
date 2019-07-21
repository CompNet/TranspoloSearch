package fr.univavignon.transpolosearch.tools.time;	

/*
 * TranspoloSearch
 * Copyright 2015-18 Vincent Labatut
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

import fr.univavignon.transpolosearch.data.article.ArticleLanguage;

import fr.univavignon.tools.log.HierarchicalLogger;
import fr.univavignon.tools.log.HierarchicalLoggerManager;

/**
 * Sets of methods used to find dates in natural text,
 * extract them and get a formal representation under
 * the form of a {@link Date} object. 
 * 
 * @author Vincent Labatut 
 */
public class DateParser
{	
	/////////////////////////////////////////////////////////////////
	// LOGGING			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	protected static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();
	
	/////////////////////////////////////////////////////////////////
	// PROCESSING	 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Parse the specified string and returns a {@link Period} object,
	 * or {@code null} if the method failed to parse the text.
	 * 
	 * @param text
	 * 		Text to parse.
	 * @param language
	 * 		Language of the text.
	 * @return
	 * 		The corresponding {@code Period} (may contain twice the same date),
	 * 		or {@code null} if the text could not be parsed.
	 */
	public static Period parseDate(String text, ArticleLanguage language)
	{	Period result = null;
		
		if(language!=null)
		{	switch(language)
			{	case EN:
					result = DateParserEn.parseDate(text);
					break;
				case FR:
					result = DateParserFr.parseDate(text);
					break;
			}
			
			if(result!=null && result.getStartDate()==null && result.getEndDate()==null)
				result = null;
		}
		
		return result;
	}
}
