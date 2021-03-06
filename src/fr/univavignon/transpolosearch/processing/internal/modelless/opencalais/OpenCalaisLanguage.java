package fr.univavignon.transpolosearch.processing.internal.modelless.opencalais;

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

import java.util.Arrays;
import java.util.List;

import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.entity.EntityType;

/**
 * Lists the languages supported by both
 * OpenCalais and the software.
 * 
 * @author Yasa Akbulut
 * @author Vincent Labatut
 */
public enum OpenCalaisLanguage
{	/** Treat English text */
	EN
	(	ArticleLanguage.EN,
		Arrays.asList
		(	EntityType.DATE,
			EntityType.FUNCTION,
			EntityType.LOCATION,
			EntityType.ORGANIZATION,
			EntityType.PERSON,
			EntityType.PRODUCTION
		)
	), 
	/** Treat French text */
	FR
	(	ArticleLanguage.FR,
		Arrays.asList
		(	//EntityType.DATE,
			//EntityType.FUNCTION,
			EntityType.LOCATION,
			EntityType.ORGANIZATION,
			EntityType.PERSON
			//EntityType.PRODUCTION
		)
	);
	
	/**
	 * Initializes a language symbol.
	 * 
	 * @param language
	 * 		Corresponding ArticleLanguage value.
	 * @param handledTypes
	 * 		Types supported for this language.
	 */
	OpenCalaisLanguage(ArticleLanguage language, List<EntityType> handledTypes)
	{	this.language = language;
		this.handledTypes = handledTypes;
	}
	
	/////////////////////////////////////////////////////////////////
	// LANGUAGE		 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Corresponding article language value */
	private ArticleLanguage language;
	
	/**
	 * Checks if this language matches the specified one.
	 * 
	 * @param language
	 * 		Language to check.
	 * @return
	 * 		{@code true} iff the languages are the same.
	 */
	public boolean handlesLanguage(ArticleLanguage language)
	{	boolean result = this.language==language;
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// TYPES		 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Entity types supported by OpenCalais for this language */
	private List<EntityType> handledTypes;
	
	/**
	 * Returns the list of entity types supported by 
	 * OpenCalais for this language.
	 * 
	 * @return
	 * 		List of supported entity types.
	 */
	public List<EntityType> getHandledTypes()
	{	return handledTypes;
	}
}
