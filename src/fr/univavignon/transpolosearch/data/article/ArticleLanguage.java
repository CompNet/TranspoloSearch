package fr.univavignon.transpolosearch.data.article;

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

import java.util.Locale;

/**
 * Language of an article.
 * 
 * @author Vincent Labatut
 */
public enum ArticleLanguage
{	/** English */
	EN,
	/** French */
	FR;
	
//	/**
//	 * Returns the word "and" in this language. This is used when replacing 
//	 * the symbol "&" during text cleaning.
//	 * 
//	 * @return
//	 * 		A string corresponding to the word "and" in this language.
//	 */
//	public String getEt()
//	{	String result = null;
//		
//		switch(this)
//		{	case EN:
//				result = "and";
//				break;
//			case FR:
//				result = "et";
//				break;
//		}
//		
//		return result;
//	}
	
	/**
	 * Returns the {@code Locale} associted to this language.
	 * 
	 * @return
	 * 		The corresponding {@code Locale}.
	 */
	public Locale getLocale()
	{	Locale result = null;
		
		switch(this)
		{	case EN:
				result = Locale.ENGLISH;
				break;
			case FR:
				result = Locale.FRENCH;
				break;
		}
		
		return result;
	}
}
