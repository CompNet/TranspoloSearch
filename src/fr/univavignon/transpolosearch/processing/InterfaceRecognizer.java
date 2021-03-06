package fr.univavignon.transpolosearch.processing;

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

import java.util.List;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.entity.EntityType;
import fr.univavignon.transpolosearch.data.entity.mention.Mentions;

/**
 * Interface implemented by all classes able to perform
 * mention recognition, i.e. detect in some text all the
 * expression corresponding to entities.
 * 		 
 * @author Vincent Labatut
 */
public interface InterfaceRecognizer extends InterfaceProcessor
{	
	/////////////////////////////////////////////////////////////////
	// FOLDER			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns the name of the folder containing the results of this
	 * recognizer.
	 * <br/>
	 * This name takes into account the name of the tool, but also the 
	 * parameters it uses. It can also be used just whenever a string 
	 * representation of the tool and its parameters is needed.
	 * 
	 * @return 
	 * 		Name of the appropriate folder.
	 */
	public String getRecognizerFolder();
	
	/////////////////////////////////////////////////////////////////
	// ENTITY TYPES		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns the types of entities this recognizer
	 * can handle with its current model/parameters.
	 * 
	 * @return 
	 * 		A list of entity types.
	 */
	public List<EntityType> getRecognizedEntityTypes();
	
	/////////////////////////////////////////////////////////////////
	// LANGUAGES		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Checks whether the specified language is supported by this
	 * recognizer, given its current settings (parameters, model...).
	 * 
	 * @param language
	 * 		The language to be checked.
	 * @return 
	 * 		{@code true} iff this recognizer supports the specified
	 * 		language, with its current parameters (model, etc.).
	 */
	public boolean canRecognizeLanguage(ArticleLanguage language);
	
	/////////////////////////////////////////////////////////////////
	// PROCESSING		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Applies this processor to the specified article,
	 * in order to recognize entity mentions.
	 * 
	 * @param article
	 * 		Article to be processed.
	 * @return
	 * 		List of the recognized mentions.
	 * 
	 * @throws ProcessorException
	 * 		Problem while recognizing the mentions. 
	 */
	public Mentions recognize(Article article) throws ProcessorException;
}
