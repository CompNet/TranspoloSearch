package fr.univavignon.transpolosearch.data.search;

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

import java.util.Date;

import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
import fr.univavignon.transpolosearch.processing.InterfaceRecognizer;
import fr.univavignon.transpolosearch.processing.ProcessorException;

/**
 * Collection of search results returned by a collection of
 * search engines, with additional info resulting from their
 * subsequent processing.
 * 
 * @param <T>
 * 		Type of results handled by this class. 
 * 
 * @author Vincent Labatut
 */
public abstract class AbstractSpecificSearchResults<T extends AbstractSearchResult> extends AbstractSearchResults<T>
{	
	/////////////////////////////////////////////////////////////////
	// FILTERING	/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Discards results whose language does not matche the targeted one.
	 *
	 * @param language
	 * 		targeted language of the articles.
	 */
	private void filterByLanguage(ArticleLanguage language)
	{	logger.log("Removing articles not matching the language constraint: "+language);
		logger.increaseOffset();
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	if(result.status==null)
				{	total++;
					if(!result.filterByLanguage(language,total))
						count++;
				}
			}
		logger.decreaseOffset();
		logger.log("Language-based filtering complete: "+count+"/"+total);
	}
	
	/**
	 * Discards results describing only events not contained 
	 * in the specified date range.
	 *  
	 * @param startDate
	 * 		Start of the time period.
	 * @param endDate
	 * 		End of the time period.
	 */
	private void filterByEntityDate(Date startDate, Date endDate)
	{	logger.log("Removing articles not fitting the entity date constraints: "+startDate+"->"+endDate);
		logger.increaseOffset();
			fr.univavignon.transpolosearch.tools.time.Date start = new fr.univavignon.transpolosearch.tools.time.Date(startDate);
			fr.univavignon.transpolosearch.tools.time.Date end = new fr.univavignon.transpolosearch.tools.time.Date(endDate);
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	if(result.status==null)
				{	total++;
					if(!result.filterByEntityDate(start,end,total))
						count++;
				}
			}
		logger.decreaseOffset();
		logger.log("Date-based filtering complete: "+count+"/"+total);
	}
	
	/**
	 * Discards results published out of the specified date range. 
	 *  
	 * @param startDate
	 * 		Start of the time period.
	 * @param endDate
	 * 		End of the time period.
	 */
	private void filterByPublicationDate(Date startDate, Date endDate)
	{	logger.log("Removing articles not fitting the publication date constraints: "+startDate+"->"+endDate);
		logger.increaseOffset();
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	if(result.status==null)
				{	total++;
					if(!result.filterByPublicationDate(startDate,endDate,total))
						count++;
				}
			}
		logger.decreaseOffset();
		logger.log("Publication date-based filtering complete: "+count+"/"+total);
	}
	
	/**
	 * Discards results corresponding only to articles not containing 
	 * the compulsory expression.
	 *  
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article.
	 */
	private void filterByKeyword(String compulsoryExpression)
	{	logger.log("Discarding articles not containing the compulsory expression \""+compulsoryExpression+"\"");
		logger.increaseOffset();
			int count = 0;
			int total = 0;
			for(T result: results.values())
			{	
if(result instanceof WebSearchResult && ((WebSearchResult)result).url.equalsIgnoreCase
		("http://www.lamarseillaise.fr/vaucluse/developpement-durable/58144-avignon-ca-bouge-autour-du-technopole-de-l-agroparc"))				
	System.out.print("");
				
				if(result.status==null)
				{	total++;
					if(!result.filterByKeyword(compulsoryExpression,total))
						count++;
				}
			}
		logger.decreaseOffset();
		logger.log("Keyword-based filtering complete: "+count+"/"+total);
	}

	/**
	 * Discards results describing only events not contained 
	 * in the specified date range, or not containing the 
	 * compulsory expression.
	 *  
	 * @param startDate
	 * 		Start of the period we want to consider, 
	 * 		or {@code null} for no constraint.
	 * @param endDate
	 * 		End of the period we want to consider,
	 * 		or {@code null} for no constraint.
	 * @param filterByPubDate
	 * 		Whether or not to filter articles depending on their publication date.
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article,
	 * 		or {@code null} if there is no such constraint.
	 * @param language
	 * 		targeted language of the articles.
	 */
	public void filterByContent(Date startDate, Date endDate, boolean filterByPubDate, String compulsoryExpression, ArticleLanguage language)
	{	logger.log("Starting filtering the articles by content");
		logger.increaseOffset();
		
		// log stuff
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("startDate="+startDate);
			logger.log("endDate="+endDate);
			logger.log("filterByPubDate="+filterByPubDate);
			logger.log("compulsoryExpression="+compulsoryExpression);
			logger.log("language="+language);
		logger.decreaseOffset();
		
		// filter depending on the language
		if(language!=null)
			filterByLanguage(language);
		else
			logger.log("No targeted language to process");
		
		// possibly filter the resulting texts depending on the compulsory expression
		if(compulsoryExpression!=null)
			filterByKeyword(compulsoryExpression);
		else
			logger.log("No compulsory expression to process");
		
		// possibly filter the remaining texts depending on the publication date
		if(filterByPubDate)
			filterByPublicationDate(startDate,endDate);
		else
			logger.log("No publication date filtering");
		
		logger.decreaseOffset();
		logger.log("Content-based filtering complete");
	}
	
	/**
	 * Discards results describing only events not contained
	 * in the specified time period.
	 * 
	 * @param startDate
	 * 		Start of the time period.
	 * @param endDate
	 * 		End of the time period.
	 * @param filterByEntDate
	 * 		Whether or not filtering the articles depending on the fact they contain
	 * 		a date belonging to the targeted period.
	 */
	public void filterByEntity(Date startDate, Date endDate, boolean filterByEntDate)
	{	logger.log("Starting filtering the articles by entity");
		logger.increaseOffset();
		
		// log stuff
		logger.log("Parameters:");
		logger.increaseOffset();
			logger.log("startDate="+startDate);
			logger.log("endDate="+endDate);
			logger.log("filterByEntDate="+filterByEntDate);
		logger.decreaseOffset();
		
		// possibly filter the texts depending on the dates they contain
		if(filterByEntDate)
		{	if(startDate==null || endDate==null)
				logger.log("WARNING: one date is null, so both of them are ignored");
			else
				filterByEntityDate(startDate, endDate);
		}
		
		logger.decreaseOffset();
		logger.log("Entity-based filtering complete");
	}
	
	/////////////////////////////////////////////////////////////////
	// MENTIONS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Detects the entity mentions present in each specified article.
	 * 
	 * @param recognizer
	 * 		The recognizer used to detect the mentions.
	 * @throws ProcessorException
	 * 		Problem while applying the NER tool.
	 */
	public void detectMentions(InterfaceRecognizer recognizer) throws ProcessorException
	{	logger.log("Detecting entity mentions in all the articles");
		logger.increaseOffset();
		
			int count = 0;
			int total = 0;
boolean doit = false;
			for(T result: results.values())
			{	
//if(result instanceof WebSearchResult && ((WebSearchResult)result).url.equalsIgnoreCase
//		("http://www.republicain-lorrain.fr/actualite/2017/03/11/routes-treize-stars-s-engagent"))				
//	System.out.print("");
	doit = true;
				
				if(result.status==null && doit)
				{	total++;
					if(result.detectMentions(recognizer,total)>0)
						count++;
				}
			}
		
		logger.decreaseOffset();
		logger.log("Mention detection complete: ("+count+" for "+total+" articles)");
	}

	/**
	 * Displays the entity mentions associated to each remaining article.
	 */
	public void displayRemainingMentions()
	{	logger.log("Displaying remaining articles and entity mentions");
		logger.increaseOffset();
		
		int total = 0;
		for(T result: results.values())
		{	if(result.status==null)
			{	total++;
				result.displayRemainingMentions(total);
			}
		}
		
		logger.decreaseOffset();
		logger.log("Display complete");
	}
}
