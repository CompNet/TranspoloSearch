package fr.univavignon.transpolosearch.recognition.combiner.straightcombiner;

/*
 * TranspoloSearch
 * Copyright 2015-17 Vincent Labatut
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.data.entity.AbstractEntity;
import fr.univavignon.transpolosearch.data.entity.Entities;
import fr.univavignon.transpolosearch.data.entity.EntityType;
import fr.univavignon.transpolosearch.recognition.AbstractRecognizer;
import fr.univavignon.transpolosearch.recognition.RecognizerException;
import fr.univavignon.transpolosearch.recognition.RecognizerName;
import fr.univavignon.transpolosearch.recognition.combiner.AbstractCombiner;
import fr.univavignon.transpolosearch.recognition.internal.modelbased.heideltime.HeidelTime;
import fr.univavignon.transpolosearch.recognition.internal.modelbased.heideltime.HeidelTimeModelName;
import fr.univavignon.transpolosearch.recognition.internal.modelless.opencalais.OpenCalais;

/**
 * This combiner is very basic: it is meant to combine
 * complementary tools, in the sense that these tools detect
 * different types of entities. So, there is no voting of
 * any sort here.
 * <br/>
 * The NER tools handled by this combiner are:
 * <ul>
 * 		<li>OpenCalais to detect persons, locations and organizations</li>
 * 		<li>HeidelTime to detect dates</li>
 * </ul>
 * There is no option to change its behavior (yet).
 * 
 * @author Vincent Labatut
 */
public class StraightCombiner extends AbstractCombiner
{	
	/**
	 * Builds a new straight combiner.
	 *  
	 * @throws RecognizerException
	 * 		Problem while loading some combiner or tokenizer.
	 */
	public StraightCombiner() throws RecognizerException
	{	super();
		
		initRecognizers();
		setSubCacheEnabled(cache);

		initConverter();
	}
	
	/////////////////////////////////////////////////////////////////
	// NAME				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public RecognizerName getName()
	{	return RecognizerName.STRAIGHTCOMBINER;
	}

	/////////////////////////////////////////////////////////////////
	// FOLDER			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override	
	public String getFolder()
	{	String result = getName().toString();
		
//		result = result + "_" + "combi="+combiner.toString();
		
//		result = result + "_" + "trim=" + trim;
//		result = result + "_" + "ignPro=" + ignorePronouns;
//		result = result + "_" + "exclude=" + exclusionOn;
		
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// ENTITY TYPES		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of entities recognized by this combiner */
	private static final List<EntityType> HANDLED_TYPES = Arrays.asList(
		EntityType.DATE,
		EntityType.LOCATION,
		EntityType.ORGANIZATION,
		EntityType.PERSON
	);
	
	@Override
	public List<EntityType> getHandledEntityTypes()
	{	return HANDLED_TYPES;
	}

	/////////////////////////////////////////////////////////////////
	// LANGUAGES	 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of entities recognized by this combiner */
	private static final List<ArticleLanguage> HANDLED_LANGUAGES = Arrays.asList(
//		ArticleLanguage.EN,
		ArticleLanguage.FR
	);
	
	@Override
	public boolean canHandleLanguage(ArticleLanguage language)
	{	boolean result = HANDLED_LANGUAGES.contains(language);
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// TOOLS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected void initRecognizers() throws RecognizerException
	{	logger.increaseOffset();
	
		// HeidelTime
		{	logger.log("Init HeidelTime (Dates only)");
			HeidelTimeModelName modelName = HeidelTimeModelName.FRENCH_NARRATIVES;
			boolean loadModelOnDemand = true;
			boolean doIntervalTagging = false;
			HeidelTime heidelTime = new HeidelTime(modelName, loadModelOnDemand, doIntervalTagging);
			recognizers.add(heidelTime);
		}
		
		// other combiner
		{	logger.log("Init OpenCalais");
			boolean ignorePronouns = false;
			boolean exclusionOn = false;
			OpenCalais openCalais = new OpenCalais(ignorePronouns, exclusionOn);
			recognizers.add(openCalais);
		}
		
		logger.decreaseOffset();		
	}

	/////////////////////////////////////////////////////////////////
	// GENERAL MODEL	 	/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public String getModelPath()
	{	return null; // no model here
	}
	
	/////////////////////////////////////////////////////////////////
	// PROCESSING	 		/////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected Entities combineEntities(Article article, Map<AbstractRecognizer,Entities> entities, StringBuffer rawOutput) throws RecognizerException
	{	logger.increaseOffset();
		Entities result = new Entities(getName());
		Iterator<AbstractRecognizer> it = recognizers.iterator();
		
		// first get the dates
		AbstractRecognizer heidelTime = it.next();
		Entities dates = entities.get(heidelTime);
		result.addEntities(dates);
		
		// then add the rest of the non-overlapping, non-date entities
		AbstractRecognizer openCalais = it.next();
		Entities ents = entities.get(openCalais);
		List<AbstractEntity<?>> entList = ents.getEntities();
		for(AbstractEntity<?> entity: entList)
		{	EntityType type = entity.getType();
			if(type!=EntityType.DATE && !result.isEntityOverlapping(entity))
				result.addEntity(entity);
		}
		
		// sort the entities
		result.sortByPosition();
		
		logger.decreaseOffset();
		return result;
	}
}
