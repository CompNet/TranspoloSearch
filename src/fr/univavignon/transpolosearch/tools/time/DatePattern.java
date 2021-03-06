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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class overrode on the fly in {@link DateParserEn}.
 * Each one is able to handle a specific textual
 * form of date.
 * 
 * @author Vincent Labatut
 */
public abstract class DatePattern
{	
	/**
	 * Builds a new pattern processor.
	 * 
	 * @param pattern
	 * 		targeted pattern.
	 */
	public DatePattern(String pattern)
	{	this.pattern = Pattern.compile(pattern);
	}
	
	/** Pattern associated to this processor */
	public Pattern pattern;
	
	/**
	 * Extracts a Date instance from the
	 * specified text.
	 * 
	 * @param text
	 * 		Raw text.
	 * @param matcher
	 * 		Regex matcher used to find the pattern.  
	 * @return
	 * 		Corresponding date.
	 */
	public abstract Period extractDate(String text, Matcher matcher);
}
