package fr.univavignon.transpolosearch.processing.combiner.straightcombiner;

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
 * This combiner uses uniform weighting on a selection of tools
 * able to handle French. The way the votes are conducted is different
 * from VoteCombiner though, because VoteCombiner is designed to handle
 * a group of tools able to detect the same entity types. This is not
 * the case here, for instance some tools are able to detect functions 
 * whereas others cannot.
 * <br/>
 * Here is the principle of the voting process. We call 'activated tool' a tool
 * which has detected a mention for a given expression. When there is at least
 * one activated tool:
 * <ol>
 * 	<li>Type vote: we keep the majority entity type, among all activated tools.</li>
 *  <li>Existence vote: only the tools able to handle the selected type can vote.
 *      If the activated tools are majority among them, the process goes on.</li>
 *  <li>Position vote: all activated tools vote, the majority positions win.
 * </ol> 
 * <br/>
 * The recognizers used by this combiner are:
 * <ul>
 * 		<li>HeidelTime (dates)</li>
 * 		<li>Nero (dates, functions, persons, locations, organizations and productions)</li>
 * 		<li>OpenCalais (dates, persons, locations and organizations)</li>
 * 		<li>OpeNer (dates, persons, locations and organizations)</li>
 * 		<li>TagEn (dates, persons, locations and organizations)</li>
 * </ul>
 * There is no option to change its behavior (yet).
 * 
 * @author Vincent Labatut
 */
public class StraightCombiner extends AbstractProcessor implements InterfaceRecognizer
{	
	/**
	 * Builds a new straight combiner.
	 *  
	 * @throws ProcessorException
	 * 		Problem while loading some combiner or tokenizer.
	 */
	public StraightCombiner() throws ProcessorException
	{	delegateRecognizer = new StraightCombinerDelegateRecognizer(this);
	}
	
	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public ProcessorName getName()
	{	return ProcessorName.STRAIGHTCOMBINER;
	}

	/////////////////////////////////////////////////////////////////
	// FOLDER 			/////////////////////////////////////////////
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
	private StraightCombinerDelegateRecognizer delegateRecognizer;
	
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
