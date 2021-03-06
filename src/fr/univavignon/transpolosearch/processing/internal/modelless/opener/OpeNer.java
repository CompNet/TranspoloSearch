package fr.univavignon.transpolosearch.processing.internal.modelless.opener;

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

/**
 * This class acts as an interface with the OpeNer Web service.
 * It handles mention recognition.
 * See {@link OpeNerDelegateRecognizer} for more details.
 * <br/>
 * Official OpeNer website: 
 * <a href="http://www.opener-project.eu/webservices/">
 * http://www.opener-project.eu/webservices/</a>
 * <br/>
 * <b>Notes:</b> the English version is able to recognize mentions
 * referring to the same entity, and to resolve coreferences. The 
 * tool also seems to be able to do entity linking (vs. a knowledge base).
 * <br/>
 * TODO OpeNer is available as a set of Java libraries. We should directly 
 * integrate them in the software.
 * 
 * @author Sabrine Ayachi
 * @author Vincent Labatut
 */
public class OpeNer extends AbstractProcessor implements InterfaceRecognizer
{
	/**
	 * Builds and sets up an object representing
	 * the OpeNer recognizer.
	 * 
	 * @param parenSplit 
	 * 		Indicates whether mentions containing parentheses
	 * 		should be split (e.g. "Limoges (Haute-Vienne)" is split 
	 * 		in two distinct mentions).
	 * @param ignorePronouns
	 * 		Whether or not pronouns should be excluded from the detection.
	 * @param exclusionOn
	 * 		Whether or not stop words should be excluded from the detection.
	 */
	public OpeNer(boolean parenSplit, boolean ignorePronouns, boolean exclusionOn)
	{	delegateRecognizer = new OpeNerDelegateRecognizer(this, parenSplit, ignorePronouns, exclusionOn);
	}
	
	/**
	 * Methods used to perform various tests.
	 * 
	 * @param args
	 * 		Not used.
	 * 
	 * @throws Exception
	 * 		Can throw whatever exception. 
	 */
	public static void main(String[] args) throws Exception
	{	String txt = "Je vous invite à partager cette vidéo en masse Pour ajouter ce mec son liens est http:-www.facebook.com-monsieurterenzio Elle est belle votre liberté d'expression Le respect c'est quelque choses de personnel et d'intimes. Il suffit pas de mettre   je suis charlie  pour être respecteux. on dirais que pour vous il suf. youtube.com Fais gaffe tu vas rire . 1 2014.";
		Article article = new Article("test");
		article.setRawText(txt);
		
		boolean parenSplit = true;
		boolean ignorePronouns = true;
		boolean exclusionOn = true;
		OpeNer recognizer = new OpeNer(parenSplit, ignorePronouns, exclusionOn);
		recognizer.delegateRecognizer.detectMentions(article);
	}
	
	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public ProcessorName getName()
	{	return ProcessorName.OPENER;
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
	private OpeNerDelegateRecognizer delegateRecognizer;
	
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
