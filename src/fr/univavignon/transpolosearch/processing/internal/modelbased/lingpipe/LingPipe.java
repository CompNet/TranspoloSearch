package fr.univavignon.transpolosearch.processing.internal.modelbased.lingpipe;

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

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.entity.EntityType;
import fr.univavignon.transpolosearch.data.entity.mention.Mentions;
import fr.univavignon.transpolosearch.processing.AbstractProcessor;
import fr.univavignon.transpolosearch.processing.InterfaceRecognizer;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.processing.ProcessorName;

import java.util.List;

/**
 * This class acts as a delegate for the mention recognition with 
 * the LingPipe library.
 * It handles mention recognition.
 * See {@link LingPipeDelegateRecognizer} for more details.
 * <br/>
 * Official LingPipe website: <a href="http://alias-i.com/lingpipe">http://alias-i.com/lingpipe/</a>
 * 
 * @author Samet Atdağ
 * @author Vincent Labatut
 */
public class LingPipe extends AbstractProcessor implements InterfaceRecognizer
{
	/**
	 * Builds and sets up an object representing
	 * a LingPipe recognizer.
	 * 
	 * @param chunkingMethod
	 * 		Method used to detect mentions.
	 * @param loadChunkerOnDemand
	 * 		Whether or not the chunker should be loaded when initializing this
	 * 		recognizer, or only when necessary. 
	 * @param splitSentences
	 * 		Whether or not the text should be processed one sentence at once.
	 * @param trim
	 * 		Whether or not the beginings and ends of mentions should be 
	 * 		cleaned from any non-letter/digit chars.
	 * @param ignorePronouns
	 * 		Whether or not pronouns should be excluded from the detection.
	 * @param exclusionOn
	 * 		Whether or not stop words should be excluded from the detection.
	 * 
	 * @throws ProcessorException 
	 *		Problem while initializing the model.
	 */
	public LingPipe(LingPipeModelName chunkingMethod, boolean loadChunkerOnDemand, boolean splitSentences, boolean trim, boolean ignorePronouns, boolean exclusionOn) throws ProcessorException
	{	delegateRecognizer = new LingPipeDelegateRecognizer(this, chunkingMethod, loadChunkerOnDemand, splitSentences, trim, ignorePronouns, exclusionOn);
	}
	
	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public ProcessorName getName()
	{	return ProcessorName.LINGPIPE;
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
	// RECOGNIZER 			/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Delegate in charge of recognizing entity mentions */
	private LingPipeDelegateRecognizer delegateRecognizer;
	
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
