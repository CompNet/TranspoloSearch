package fr.univavignon.transpolosearch.data.search;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.entity.EntityType;
import fr.univavignon.transpolosearch.data.entity.mention.AbstractMention;
import fr.univavignon.transpolosearch.data.entity.mention.MentionDate;
import fr.univavignon.transpolosearch.data.entity.mention.MentionFunction;
import fr.univavignon.transpolosearch.data.entity.mention.MentionLocation;
import fr.univavignon.transpolosearch.data.entity.mention.MentionMeeting;
import fr.univavignon.transpolosearch.data.entity.mention.MentionOrganization;
import fr.univavignon.transpolosearch.data.entity.mention.MentionPerson;
import fr.univavignon.transpolosearch.data.entity.mention.MentionProduction;
import fr.univavignon.transpolosearch.data.entity.mention.Mentions;
import fr.univavignon.transpolosearch.data.event.Event;
import fr.univavignon.transpolosearch.processing.InterfaceRecognizer;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLogger;
import fr.univavignon.transpolosearch.tools.log.HierarchicalLoggerManager;
import fr.univavignon.transpolosearch.tools.string.StringTools;
import fr.univavignon.transpolosearch.tools.time.Date;
import fr.univavignon.transpolosearch.tools.time.Period;

/**
 * Represents one result of a search engine and some info 
 * regarding how it was subsequently processed.
 * 
 * @author Vincent Labatut
 */
public abstract class AbstractSearchResult
{
	/**
	 * Initializes the search result.
	 */
	public AbstractSearchResult()
	{	
	}
	
	/////////////////////////////////////////////////////////////////
	// LOGGER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Common object used for logging */
	public static HierarchicalLogger logger = HierarchicalLoggerManager.getHierarchicalLogger();
	
	/////////////////////////////////////////////////////////////////
	// STATUS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Last operation conducted on the result during its processing */
	public String status = null;
	
	/////////////////////////////////////////////////////////////////
	// ARTICLE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Article associated to this result */
	public Article article = null;
	
	/**
	 * Discards the result if its article does not contain 
	 * the specified compulsory expression.
	 *  
	 * @param compulsoryExpression
	 * 		String expression which must be present in the article.
	 * @param nbr
	 * 		Number of this result in the collection.
	 * @return
	 * 		{@code true} iff the result was discarded.
	 */
	protected boolean filterByKeyword(String compulsoryExpression, int nbr)
	{	boolean result = true;
		
		logger.log("Processing article "+article.getTitle()+" ("+nbr+")");
		String rawText = article.getRawText();
		if(!rawText.contains(compulsoryExpression))
		{	logger.log("Discarding article "+article.getTitle()+" ("+article.getUrl()+")");
			status = "Missing keyword";
			result = false;
		}
			
		return result;
	}
	
	/**
	 * Discards results describing only events not contained 
	 * in the specified date range.
	 *  
	 * @param startDate
	 * 		Start of the time period.
	 * @param endDate
	 * 		End of the time period.
	 * @param nbr
	 * 		Number of this result in the collection.
	 * @return
	 * 		{@code true} iff the result was discarded.
	 */
	protected boolean filterByDate(Date startDate, Date endDate, int nbr)
	{	logger.log("Processing article "+article.getTitle()+" ("+nbr+")");
		List<AbstractMention<?>> dateMentions = mentions.getMentionsByType(EntityType.DATE);
		boolean result = dateMentions.isEmpty();
		if(!result)	
		{	Period period = null;
			Iterator<AbstractMention<?>> it = dateMentions.iterator();
			while(period==null && it.hasNext())
			{	AbstractMention<?> mention = it.next();
				Period p = (Period) mention.getValue();
				if(p.contains(startDate) ||  p.contains(endDate))
					period = p;
			}
			
			if(period==null)
			{	logger.log("Did not find any appropriate date in article "+article.getTitle()+" >> removal ("+article.getUrl()+")");
				result = true;
			}
			else
				logger.log("Found date "+period+" in article "+article.getTitle()+" >> keep ("+article.getUrl()+")");
			
		}
		
		// possibly remove the article/mentions
		if(result)
			status = "Missing date";
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// MENTIONS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Mentions detected in the article associated to this search result */
	public Mentions mentions = null;
	
	/**
	 * Detects the entity mentions in the article retrieved for this result.
	 * 
	 * @param recognizer
	 * 		Recognizer used when detecting the mentions.
	 * @param nbr
	 * 		Number of this result in the collection.
	 * @return 
	 * 		Number of detected mentions.
	 * 
	 * @throws ProcessorException
	 * 		Problem while detecting the mentions.
	 */
	protected int detectMentions(InterfaceRecognizer recognizer, int nbr) throws ProcessorException
	{	logger.log("Retrieving article #"+nbr+" ("+article.getTitle()+")");
		logger.increaseOffset();
			mentions = recognizer.recognize(article);
			int result = mentions.getMentions().size();
			logger.log("Found "+result+" entities");
			if(result==0)
				status = "No mention found";
		logger.decreaseOffset();
		return result;
	}


	/**
	 * Displays the entity mentions associated to each remaining article.
	 * 
	 * @param nbr
	 * 		Number of this result in the collection.
	 */
	protected void displayRemainingMentions(int nbr)
	{	logger.log("Mentions for article #"+nbr+" ("+article.getTitle()+")");
		logger.increaseOffset();
			List<AbstractMention<?>> dates = mentions.getMentionsByType(EntityType.DATE);
			if(!dates.isEmpty())
			{	String first = "Dates ("+dates.size()+"):";
				List<String> msg = new ArrayList<String>();
				msg.add(first);
				for(AbstractMention<?> mention: dates)
					msg.add(mention.toString());
				logger.log(msg);
			}
			List<AbstractMention<?>> locations = mentions.getMentionsByType(EntityType.LOCATION);
			if(!locations.isEmpty())
			{	String first = "Locations ("+locations.size()+"):";
				List<String> msg = new ArrayList<String>();
				msg.add(first);
				for(AbstractMention<?> mention: locations)
					msg.add(mention.toString());
				logger.log(msg);
			}
			List<AbstractMention<?>> organizations = mentions.getMentionsByType(EntityType.ORGANIZATION);
			if(!organizations.isEmpty())
			{	String first = "Organizations ("+organizations.size()+"):";
				List<String> msg = new ArrayList<String>();
				msg.add(first);
				for(AbstractMention<?> mention: organizations)
					msg.add(mention.toString());
				logger.log(msg);
			}
			List<AbstractMention<?>> persons = mentions.getMentionsByType(EntityType.PERSON);
			if(!persons.isEmpty())
			{	String first = "Persons ("+persons.size()+"):";
				List<String> msg = new ArrayList<String>();
				msg.add(first);
				for(AbstractMention<?> mention: persons)
					msg.add(mention.toString());
				logger.log(msg);
			}
		logger.decreaseOffset();
	}
	
	/////////////////////////////////////////////////////////////////
	// EVENTS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Event detected for this search result */
	public List<Event> events = new ArrayList<Event>();
	
	/**
	 * Identifies the events described in the article associated to
	 * this search result.
	 * 
	 * @param bySentence
	 * 		Whether to retrieve events by sentence (all event-related entity mentions
	 * 		must be in the same sentence) or by article.
	 * @param nbr
	 * 		Number of this result in the collection.
	 * @return 
	 * 		Number of extracted events.
	 */
	protected int extractEvents(boolean bySentence, int nbr)
	{	logger.log("Retrieving article #"+nbr+" ("+article.getTitle()+")");
		logger.increaseOffset();
			String rawText = article.getRawText();
			if(bySentence)
			{	// retrieving the sentence positions
				List<Integer> sentencePos = StringTools.getSentencePositions(rawText);
				sentencePos.add(rawText.length()); // to mark the end of the last sentence
				int sp = -1;
				
				// for each sentence, we get the detected entity mentions
				for(int ep: sentencePos)
				{	if(sp>=0)
					{	List<AbstractMention<?>> le = mentions.getMentionsIn(sp, ep);
						List<AbstractMention<?>> dates = Mentions.filterByType(le,EntityType.DATE);
						// only go on if there is at least one date
						if(!dates.isEmpty())
						{	if(dates.size()>1)
								logger.log("WARNING: there are several dates in sentence \""+rawText.substring(sp,ep)+"\"");
							else
							{	MentionDate ed = (MentionDate)dates.get(0);
								List<AbstractMention<?>> persons = Mentions.filterByType(le,EntityType.PERSON);
								if(persons.isEmpty())
									logger.log("WARNING: there is a date ("+ed.getValue()+") but no persons in sentence \""+rawText.substring(sp,ep)+"\"");
								else
								{	Event event = new Event(ed);
									events.add(event);
									for(AbstractMention<?> mention: persons)
									{	MentionPerson person = (MentionPerson)mention;
										event.addPerson(person);
									}
									List<AbstractMention<?>> organizations = Mentions.filterByType(le,EntityType.ORGANIZATION);
									for(AbstractMention<?> mention: organizations)
									{	MentionOrganization organization = (MentionOrganization)mention;
										event.addOrganization(organization);
									}
									List<AbstractMention<?>> locations = Mentions.filterByType(le,EntityType.LOCATION);
									for(AbstractMention<?> mention: locations)
									{	MentionLocation location = (MentionLocation)mention;
										event.addLocation(location);
									}
									List<AbstractMention<?>> meetings = Mentions.filterByType(le,EntityType.MEETING);
									for(AbstractMention<?> mention: meetings)
									{	MentionMeeting meeting = (MentionMeeting)mention;
										event.addMeeting(meeting);
									}
									List<AbstractMention<?>> functions = Mentions.filterByType(le,EntityType.FUNCTION);
									for(AbstractMention<?> mention: functions)
									{	MentionFunction function = (MentionFunction)mention;
										event.addFunction(function);
									}
									List<AbstractMention<?>> productions = Mentions.filterByType(le,EntityType.PRODUCTION);
									for(AbstractMention<?> mention: productions)
									{	MentionProduction production = (MentionProduction)mention;
										event.addProduction(production);
									}
									logger.log(Arrays.asList("Event found for sentence \""+rawText.substring(sp,ep)+"\"",event.toString()));
								}
							}
						}
					}
					sp = ep;
				}
			}
			
			else // by article
			{	List<AbstractMention<?>> dates = mentions.getMentionsByType(EntityType.DATE);
				// only go on if there is at least one date
				if(!dates.isEmpty())
				{	Event event;
					if(dates.size()>1)
					{	logger.log("There are several ("+dates.size()+") dates in the article >> merging them");
						Iterator<AbstractMention<?>> it = dates.iterator();
						MentionDate ed = (MentionDate)it.next();
						event = new Event(ed);
						while(it.hasNext())
						{	ed = (MentionDate)it.next();
							Period p = ed.getValue(); 
							event.mergePeriod(p);
						}
					}
					else
					{	MentionDate esd = (MentionDate)dates.get(0);
						event = new Event(esd);
					}
					
					List<AbstractMention<?>> persons = mentions.getMentionsByType(EntityType.PERSON);
					if(persons.isEmpty())
						logger.log("WARNING: there is a date ("+event.getPeriod()+") but no person in article \""+article.getTitle()+"\"");
					else
					{	events.add(event);
						
						for(AbstractMention<?> mention: persons)
						{	MentionPerson person = (MentionPerson)mention;
							event.addPerson(person);
						}
						List<AbstractMention<?>> organizations = mentions.getMentionsByType(EntityType.ORGANIZATION);
						for(AbstractMention<?> mention: organizations)
						{	MentionOrganization organization = (MentionOrganization)mention;
							event.addOrganization(organization);
						}
						List<AbstractMention<?>> locations = mentions.getMentionsByType(EntityType.LOCATION);
						for(AbstractMention<?> mention: locations)
						{	MentionLocation location = (MentionLocation)mention;
							event.addLocation(location);
						}
						logger.log(Arrays.asList("Event found for article \""+article.getTitle()+"\"",event.toString()));
					}
				}
			}

			int result = events.size();
			logger.log("Found "+result+" events");
			if(result==0)
				status = "No event found";
		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Records the results of the search as a CSV file.
	 * 
	 * @return
	 * 		Map representing the events associated to this social
	 * 		search result (can be empty). 
	 */
	protected abstract List<Map<String,String>> exportEvents();
}
