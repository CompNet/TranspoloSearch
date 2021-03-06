package fr.univavignon.transpolosearch.processing.external.tagen;

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
 * Class representing the predefined
 * model used by TagEn for 
 * detecting mentions.
 * 
 * @author Vincent Labatut
 */
public enum TagEnModelName
{	
	/** 
	 * Model built on the MUC dataset. 
	 * <ul>
	 * 		<li>Date</li> TODO to confirm
	 * 		<li>Location</li>
	 * 		<li>Money</li>
	 * 		<li>Organization</li>
	 * 		<li>Percent</li>
	 * 		<li>Person</li>
	 * 		<li>Time</li>
	 * </ul>
	 * Only handles the French language.
	 */ 
	MUC_MODEL(
		"MucFr",
		"mucfr",
		Arrays.asList(//TODO to confirm
			EntityType.DATE,
			EntityType.LOCATION,
			EntityType.ORGANIZATION,
			EntityType.PERSON
		),
		Arrays.asList(ArticleLanguage.FR)
	),
	
	/** 
	 * Model built on medical articles. 
	 * <ul>
	 * 	<li>?</li> TODO
	 * </ul> 
	 * Only handles the French language.
	 */ 
	MEDICFR_MODEL(
		"MedicFr",
		"equer",
		Arrays.asList(//TODO to confirm
			EntityType.LOCATION,
			EntityType.ORGANIZATION,
			EntityType.PERSON
		),
		Arrays.asList(ArticleLanguage.FR)
	),
	
	/** 
	 * Model built from Wikipedia articles. 
	 * <ul>
	 * 	<li>?</li> TODO
	 * </ul> 
	 * Only handles the English language.
	 */
	WIKI_MODEL(
		"WikiEn",
		"wiki",
		Arrays.asList(//TODO to confirm
			EntityType.DATE,
			EntityType.LOCATION,
			EntityType.ORGANIZATION,
			EntityType.PERSON
		),
		Arrays.asList(ArticleLanguage.EN)
	),
	
	/** 
	 * Model built on Medical articles. 
	 * <ul>
	 * 	<li>?</li> TODO
	 * </ul> 
	 * Only handles the English language.
	 */ 
	MEDICEN_MODEL(
		"MedicEn",
		"bio",
		Arrays.asList(//TODO to confirm
			EntityType.LOCATION,
			EntityType.ORGANIZATION,
			EntityType.PERSON
		),
		Arrays.asList(ArticleLanguage.EN)
	);
	
	/**
	 * Builds a new value representing
	 * an Illinois model.
	 * 
	 * @param name
	 * 		User-friendly name of the model.
	 * @param parameter
	 * 		Parameter used when invoking TagEn.
	 * @param types
	 * 		List of the entity types handled by the model.
	 * @param languages
	 * 		List of the languages handled by the model.
	 */
	TagEnModelName(String name, String parameter, List<EntityType> types, List<ArticleLanguage> languages)
	{	this.name = name;
		this.parameter = parameter;
		this.types = types;
		this.languages = languages;
	}
	
	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** User-friendly name of the model */
	private String name;
	
	@Override
	public String toString()
	{	return name;
	}
	
	/////////////////////////////////////////////////////////////////
	// PARAMETER		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Parameter used when invoking TagEn */
	private String parameter;
	
	/**
	 * Returns the parameter to use when invoking TagEn.
	 * 
	 * @return
	 * 		A string representing the appropriate parameter.
	 */
	public String getParameter()
	{	return parameter;
	}
	
	/////////////////////////////////////////////////////////////////
	// ENTITY TYPES		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of entity types this model can treat */
	private List<EntityType> types;
	
	/**
	 * Returns the list of types
	 * supported by this predefined model.
	 * 
	 * @return
	 * 		A list of supported {@link EntityType}.
	 */
	public List<EntityType> getHandledTypes()
	{	return types;
	}
	
	/////////////////////////////////////////////////////////////////
	// LANGUAGES		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of languages this model can treat */
	private List<ArticleLanguage> languages;
	
	/**
	 * Checks whether the specified language is supported by this  model.
	 * 
	 * @param language
	 * 		The language to be checked.
	 * @return 
	 * 		{@code true} iff this model supports the specified language.
	 */
	public boolean canHandleLanguage(ArticleLanguage language)
	{	boolean result = languages.contains(language);
		return result;
	}
}
