package fr.univavignon.transpolosearch.processing.internal.modelless.subee;

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
import fr.univavignon.transpolosearch.processing.AbstractProcessor;
import fr.univavignon.transpolosearch.processing.InterfaceRecognizer;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.processing.ProcessorName;
import fr.univavignon.transpolosearch.tools.freebase.FbCommonTools;

/**
 * Wikipedia-based custom recognizer.
 * See {@link SubeeDelegateRecognizer} for details.
 * 
 * <b>Note:</b> if you use this tool, make sure you set up your Freebase key
 * in class {@link FbCommonTools}.
 * 
 * @author Yasa Akbulut
 * @author Vincent Labatut
 */
public class Subee extends AbstractProcessor implements InterfaceRecognizer
{
	/**
	 * Builds and sets up an object representing
	 * Subee, our recognizer taking advantage of text
	 * containing hyperlinks.
	 * 
	 * @param additionalOccurrences
	 * 		Whether or not the tool should annotate the additional occurrences
	 * 		of some mention.
	 * @param useTitle
	 * 		Whether or not the tool should use the article title to infer
	 * 		the person name.
	 * @param notableType
	 * 		Whether the tool should use the single notable type provided by Freebase,
	 * 		or all available Freebase types.
	 * @param useAcronyms
	 * 		On their first occurrence, certain mentions are followed by the associated
	 * 		acronym: this option allows searching them in the rest of the text.
	 * @param discardDemonyms
	 * 		Ignore mentions whose string value corresponds to a demonym, i.e. the adjective
	 * 		associated to a place, or the name of its inhabitants. Subee generally takes them
	 * 		for the place itself, leading to an increased number of false positives.
	 */
	public Subee(boolean additionalOccurrences, boolean useTitle, boolean notableType, boolean useAcronyms, boolean discardDemonyms)
	{	delegateRecognizer = new SubeeDelegateRecognizer(this, additionalOccurrences, useTitle, notableType, useAcronyms, discardDemonyms);
	}

	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public ProcessorName getName()
	{	return ProcessorName.SUBEE;
	}

	/////////////////////////////////////////////////////////////////
	// FOLDER			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override	
	public String getRecognizerFolder()
	{	String result = delegateRecognizer.getFolder();
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// RECOGNIZER	 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Delegate in charge of recognizing entity mentions */
	private SubeeDelegateRecognizer delegateRecognizer;
	
	@Override
	public boolean isRecognizer()
	{	return true;
	}
	
	@Override
	public List<EntityType> getRecognizedEntityTypes()
	{	List<EntityType> result = delegateRecognizer.getHandledEntityTypes();
		return result;
	}

	@Override
	public boolean canRecognizeLanguage(ArticleLanguage language) 
	{	boolean result = delegateRecognizer.canHandleLanguage(language);
		return result;
	}
	
	@Override
	public Mentions recognize(Article article) throws ProcessorException
	{	Mentions result = delegateRecognizer.delegateRecognize(article);
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// RESOLVER			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public boolean isResolver()
	{	return false;
	}
	
	/////////////////////////////////////////////////////////////////
	// LINKER			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public boolean isLinker()
	{	return false;
	}
}