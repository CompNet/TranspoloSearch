package fr.univavignon.transpolosearch.processing.internal.modelbased.heideltime;

/*
 * TranspoloSearch
 * Copyright2015-18Vincent Labatut
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

/**
 * This class acts as an interface with the HeidelTime tool.
 * It handles mention recognition.
 * See {@link HeidelTimeDelegateRecognizer} for more details. 
 * <br/>
 * Note we ignore some of the data output by HeidelTime when
 * converting to the inner format.
 * <br/>
 * Official HeidelTime website: <a href="https://code.google.com/p/heideltime/">https://code.google.com/p/heideltime/</a>
 * 
 * @author Vincent Labatut
 */
public class HeidelTime extends AbstractProcessor implements InterfaceRecognizer
{	
	/**
	 * Builds and sets up an object representing
	 * an HeidelTime recognizer.
	 * 
	 * @param modelName
	 * 		Predefined mainModel used for mention detection.
	 * @param loadModelOnDemand
	 * 		Whether or not the mainModel should be loaded when initializing this
	 * 		recognizer, or only when necessary. 
	 * @param doIntervalTagging
	 * 		Whether intervals should be detected or ignored (?). 
	 * 
	 * @throws ProcessorException 
	 * 		Problem while loading the models or tokenizers.
	 */
	public HeidelTime(HeidelTimeModelName modelName, boolean loadModelOnDemand, boolean doIntervalTagging) throws ProcessorException
	{	delegateRecognizer = new HeidelTimeDelegateRecognizer(this, modelName, loadModelOnDemand, doIntervalTagging);
	}
	
	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public ProcessorName getName()
	{	return ProcessorName.HEIDELTIME;
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
	private HeidelTimeDelegateRecognizer delegateRecognizer;
	
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
