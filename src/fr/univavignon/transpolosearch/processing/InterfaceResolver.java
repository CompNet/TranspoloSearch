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
import fr.univavignon.transpolosearch.data.entity.Entities;
import fr.univavignon.transpolosearch.data.entity.EntityType;
import fr.univavignon.transpolosearch.data.entity.MentionsEntities;

/**
 * Interface implemented by all classes able to perform
 * coreference resolution, i.e. determine if two entity object
 * actually correspond to the same entity or not.
 * 		 
 * @author Vincent Labatut
 */
public interface InterfaceResolver extends InterfaceProcessor
{	
	/////////////////////////////////////////////////////////////////
	// FOLDER			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns the name of the folder containing the results of this
	 * resolver.
	 * <br/>
	 * This name takes into account the name of the tool, but also the 
	 * parameters it uses. It can also be used just whenever a string 
	 * representation of the tool and its parameters is needed.
	 * 
	 * @return 
	 * 		Name of the appropriate folder.
	 */
	public String getResolverFolder();
	
	/////////////////////////////////////////////////////////////////
	// ENTITY TYPES		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns the types of entities this resolver
	 * can handle with its current model/parameters.
	 * 
	 * @return 
	 * 		A list of entity types.
	 */
	public List<EntityType> getResolvedEntityTypes();
	
	/////////////////////////////////////////////////////////////////
	// LANGUAGES		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Checks whether the specified language is supported by this
	 * resolver, given its current settings (parameters, model...).
	 * 
	 * @param language
	 * 		The language to be checked.
	 * @return 
	 * 		{@code true} iff this resolver supports the specified
	 * 		language, with its current parameters (model, etc.).
	 */
	public boolean canResolveLanguage(ArticleLanguage language);
	
	/////////////////////////////////////////////////////////////////
	// RECOGNIZER		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns the recognizer applied before this linker.
	 * 
	 * @return
	 * 		Recognizer used before this linker.
	 */
	public InterfaceRecognizer getRecognizer();
	
	/////////////////////////////////////////////////////////////////
	// PROCESSING		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Applies this processor to the specified article,
	 * in order to resolve co-occurrences.
	 * <br/>
	 * The recognizer that was set up for this resolver will automatically
	 * be applied, or its results will be loaded if its cache is enabled 
	 * (and the results are cached). The corresponding {@code Mentions}
	 * object will be completed and returned with the {@link Entities}.
	 * 
	 * @param article
	 * 		Article to be processed.
	 * @return
	 * 		Sets of the updated mentions and their associated entities.
	 * 
	 * @throws ProcessorException
	 * 		Problem while resolving co-occurrences. 
	 */
	public MentionsEntities resolve(Article article) throws ProcessorException;
}
