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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import fr.univavignon.transpolosearch.data.article.Article;
import fr.univavignon.transpolosearch.data.article.ArticleLanguage;
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
import fr.univavignon.transpolosearch.processing.ProcessorName;
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
	/** Problematic status: no mention was found in the page */
	public final static String STATUS_NO_MENTION = "No mention found";
	/** Problematic status: no event was found in the page */
	public final static String STATUS_NO_EVENT = "No event found";
	/** Problematic status: the targeted date was found in the page */
	public final static String STATUS_MISSING_DATE = "Missing targeted date";
	/** Problematic status: the targeted name was found in the page */
	public final static String STATUS_MISSING_KEYWORD = "Missing keyword";
	/** Problematic status: the page is not written using the targeted language */
	public final static String STATUS_INCORRECT_LANGUAGE = "Incorrect language";
	/** Problematic status: the format of the document is not supported */
	public final static String STATUS_UNSUPPORTED_FORMAT = "Unsupported format";
	/** Problematic status: the page is not an article, but a list of articles */
	public final static String STATUS_LIST = "List of articles";
	/** Problematic status: the document could not be retrieved */
	public final static String STATUS_UNAVAILABLE = "Server unvailable";
	
	/**
	 * Adds the status value to the specified map.
	 * 
	 * @param result
	 * 		Map to fill with the required field.
	 */
	protected void exportStatus(Map<String,String> result)
	{	if(status!=null)
			result.put(AbstractSearchResults.COL_STATUS, status);
	}
	
	/////////////////////////////////////////////////////////////////
	// CLUSTER		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Id of the cluster containing this result (when clustering the results...) */
	public String cluster = null;
	
	/**
	 * Adds the article cluster to the specified map.
	 * 
	 * @param result
	 * 		Map to fill with the required field.
	 */
	protected void exportCluster(Map<String,String> result)
	{	if(cluster!=null)
			result.put(WebSearchResults.COL_ARTICLE_CLUSTER,cluster);
	}
	
	/////////////////////////////////////////////////////////////////
	// ARTICLE		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Article associated to this result */
	public Article article = null;
	/** Format used to export dates */
	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	/**
	 * Adds the article title to the specified map.
	 * 
	 * @param result
	 * 		Map to fill with the required field.
	 */
	protected void exportTitle(Map<String,String> result)
	{	if(article!=null)
		{	String title = article.getTitle();
			result.put(WebSearchResults.COL_TITLE,title);
			
			if(this instanceof WebSearchResult)
				result.put(WebSearchResults.COL_TITLE_CONTENT,title);
			else if(this instanceof SocialSearchResult)
			{	String rawText = article.getRawText();
				String beginning = rawText.replace('\n',' ');
				beginning = beginning.replace('"','\'');
				beginning = beginning.substring(0,Math.min(25,beginning.length()));
				result.put(WebSearchResults.COL_TITLE_CONTENT,beginning);
			}
		}
	}
	
	/**
	 * Adds the article length to the specified map.
	 * 
	 * @param result
	 * 		Map to fill with the required field.
	 */
	protected void exportLength(Map<String,String> result)
	{	if(article!=null)
		{	int length = article.getRawText().length();
			result.put(AbstractSearchResults.COL_LENGTH, Integer.toString(length));
		}
	}
	
	/**
	 * Adds the article publication date to the specified map.
	 * 
	 * @param result
	 * 		Map to fill with the required field.
	 */
	protected void exportPublicationDate(Map<String,String> result)
	{	if(article!=null)
		{	java.util.Date pubDate = article.getPublishingDate();
			if(pubDate!=null)
			{	String pubDateStr = DATE_FORMAT.format(pubDate);
				result.put(WebSearchResults.COL_PUB_DATE ,pubDateStr);
			}
		}
	}
	
	/**
	 * Adds the article authors to the specified map.
	 * 
	 * @param result
	 * 		Map to fill with the required field.
	 */
	protected void exportAuthors(Map<String,String> result)
	{	if(article!=null)
		{	List<String> authors = article.getAuthors();
			if(!authors.isEmpty())
			{	Iterator<String> it = authors.iterator();
				String authorStr = it.next();
				String authorsStr = authorStr;
				while(it.hasNext())
				{	authorStr = it.next();
					authorsStr = authorsStr + ", " + authorStr;
				}
				result.put(WebSearchResults.COL_AUTHORS,authorsStr);
			}
		}
	}
	
	/**
	 * Discards results not matching the targeted language.
	 *  
	 * @param language
	 * 		The targeted language.
	 * @param nbr
	 * 		Number of this result in the collection.
	 * @return
	 * 		{@code true} iff the result was discarded.
	 */
	protected boolean filterByLanguage(ArticleLanguage language, int nbr)
	{	logger.log("Processing article "+article.getTitle()+" ("+nbr+")");
		logger.increaseOffset();
			ArticleLanguage lang = article.getLanguage();
			logger.log("Article language: "+lang+" (vs. "+language+")");
			boolean result = language!=lang;
			if(result)
				logger.log("The article language matches the targeted one >> we keep it");
			else
				logger.log("The article language does not match the targeted one >> we discard it");
			
			// possibly remove the article
			if(result)
				status = STATUS_INCORRECT_LANGUAGE;

		logger.decreaseOffset();
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
		logger.increaseOffset();
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
				status = STATUS_MISSING_DATE;
		
		logger.decreaseOffset();
		return result;
	}
	
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
	protected abstract boolean filterByKeyword(String compulsoryExpression, int nbr);
	
	/////////////////////////////////////////////////////////////////
	// CSV			/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Export the article as a line in a CSV file.
	 * 
	 * @return
	 * 		A map representing the line.
	 */
	protected abstract Map<String,String> exportResult();
	
	/////////////////////////////////////////////////////////////////
	// MENTIONS		/////////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Mentions detected in the article associated to this search result */
	public Mentions mentions = null;
	
	/**
	 * Adds the article entity mentions to the specified map.
	 * 
	 * @param result
	 * 		Map to fill with the required field.
	 * @param type 
	 * 		Type of mentions we want to export.
	 */
	protected void exportMentions(Map<String,String> result, EntityType type)
	{	if(mentions!=null)
		{	// get key
			String col = null;
			switch(type)
			{	case DATE:
					col = AbstractSearchResults.COL_ENT_DATES;
					break;
				case FUNCTION:
					col = AbstractSearchResults.COL_ENT_FUNCTIONS;
					break;
				case LOCATION:
					col = AbstractSearchResults.COL_ENT_LOCATIONS;
					break;
				case MEETING:
					col = AbstractSearchResults.COL_ENT_MEETINGS;
					break;
				case ORGANIZATION:
					col = AbstractSearchResults.COL_ENT_ORGANIZATIONS;
					break;
				case PERSON:
					col = AbstractSearchResults.COL_ENT_PERSONS;
					break;
				case PRODUCTION:
					col = AbstractSearchResults.COL_ENT_PRODUCTIONS;
					break;
			}
			
			String str = "";
			List<AbstractMention<?>> fm = mentions.getMentionsByType(type);
			Iterator<AbstractMention<?>> it = fm.iterator();
			while(it.hasNext())
			{	AbstractMention<?> m = it.next();
				String form;
				Object tmp = m.getValue();
				if(tmp!=null)
					form = tmp.toString();
				else
					form = m.getStringValue();
				form = form.replaceAll("[\\n\\r]", " ");
				form = form.replaceAll("\"", "'");
				str = str + form;
				if(it.hasNext())
					str = str + ", ";
			}
			result.put(col,str);
		}
	}
	
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
	{	logger.log("Detecting mentions in article #"+nbr+" ("+article.getTitle()+")");
		logger.increaseOffset();
			mentions = recognizer.recognize(article);
			int result = mentions.getMentions().size();
			logger.log("Found "+result+" entities");
			if(result==0)
				status = STATUS_NO_MENTION;
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
	{	logger.log("Mentions detected in article #"+nbr+" ("+article.getTitle()+")");
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
	protected abstract int extractEvents(boolean bySentence, int nbr);
	
	/**
	 * Identifies the events described in the article associated to
	 * this search result.
	 * 
	 * @param bySentence
	 * 		Whether to retrieve events by sentence (all event-related entity mentions
	 * 		must be in the same sentence) or by article.
	 * @param nbr
	 * 		Number of this result in the collection.
	 * @param usePubDate
	 * 		Whether or not to use the article publication date if no other date is
	 * 		explicitly present in the content.
	 * @return 
	 * 		Number of extracted events.
	 */
	protected int extractEvents(boolean bySentence, int nbr, boolean usePubDate)
	{	logger.log("Processing article #"+nbr+" ("+article.getTitle()+")");
		logger.increaseOffset();
			events.clear();
			String rawText = article.getRawText();
			Date pubDate = null;
			if(article.getPublishingDate()!=null)
					pubDate = new Date(article.getPublishingDate());
			if(bySentence)
			{	// retrieving the sentence positions
				List<Integer> sentencePos = StringTools.getSentencePositions(rawText);
				sentencePos.add(rawText.length()); // to mark the end of the last sentence
				int sp = -1;
				
				// for each sentence, we get the detected entity mentions
				for(int ep: sentencePos)
				{	if(sp>=0)
					{	String sentenceStr = rawText.substring(ep, sp);
						List<AbstractMention<?>> le = mentions.getMentionsIn(sp, ep);
						List<AbstractMention<?>> dates = Mentions.filterByType(le,EntityType.DATE);
						// only go on if there is at least one date
						if(!dates.isEmpty() || (usePubDate && pubDate!=null))
						{	MentionDate ed;
							if(dates.isEmpty())
							{	ed = new MentionDate(-1, -1, ProcessorName.REFERENCE, pubDate.toString(), pubDate);
								logger.log("WARNING: no explicit date in the post, using the publication date instead (\""+rawText.substring(sp,ep)+"\")");
							}
							else
							{	ed = (MentionDate)dates.get(0);
								if(dates.size()>1)
									logger.log("WARNING: there are several dates in the sentence >> using the first one (\""+rawText.substring(sp,ep)+"\")");
							} 
							List<AbstractMention<?>> persons = Mentions.filterByType(le,EntityType.PERSON);
							if(persons.isEmpty())
								logger.log("WARNING: there is no person in sentence \""+rawText.substring(sp,ep)+"\"");
							else
							{	Event event = new Event(ed);
								event.setText(sentenceStr);
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
						else
							logger.log("No usable date, so no event in this article.");
					}
					sp = ep;
				}
			}
			
			else // by article
			{	List<AbstractMention<?>> dates = mentions.getMentionsByType(EntityType.DATE);
				// only go on if there is at least one date
				if(!dates.isEmpty() || (usePubDate && pubDate!=null))
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
					else if(dates.size()==1)
					{	MentionDate esd = (MentionDate)dates.get(0);
						event = new Event(esd);
					}
					else
					{	MentionDate ed = new MentionDate(-1, -1, ProcessorName.REFERENCE, pubDate.toString(), pubDate);
						event = new Event(ed);
						logger.log("WARNING: no explicit date in the post, using the publication date instead ("+pubDate+")");
					}
					event.setText(rawText);
					
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
				status = STATUS_NO_EVENT;
		logger.decreaseOffset();
		return result;
	}
	
	/**
	 * Adds the event dates to the specified map.
	 * 
	 * @param event
	 * 		The event to consider.
	 * @param result
	 * 		The map to complete.
	 */
	private void exportEventDateMentions(Event event, Map<String,String> result)
	{	Period period = event.getPeriod();
		String periodStr = period.toString();
		periodStr = periodStr.replaceAll("[\\n\\r]", " ");
		periodStr = periodStr.replaceAll("\"", "'");
		result.put(WebSearchResults.COL_ENT_DATES,periodStr);
	}
	
	/**
	 * Adds the event mentions to the specified map.
	 * 
	 * @param event
	 * 		The event to consider.
	 * @param type
	 * 		The entity type to consider.
	 * @param result
	 * 		The map to complete.
	 */
	private void exportEventNamedMentions(Event event, EntityType type, Map<String,String> result)
	{	Collection<String> collec = null;
		String col = null;
		switch(type)
		{	case FUNCTION:
				collec = event.getFunctions();
				col = AbstractSearchResults.COL_ENT_FUNCTIONS;
				break;
			case LOCATION:
				collec = event.getLocations();
				col = AbstractSearchResults.COL_ENT_LOCATIONS;
				break;
			case MEETING:
				collec = event.getMeetings();
				col = AbstractSearchResults.COL_ENT_MEETINGS;
				break;
			case ORGANIZATION:
				collec = event.getOrganizations();
				col = AbstractSearchResults.COL_ENT_ORGANIZATIONS;
				break;
			case PERSON:
				collec = event.getPersons();
				col = AbstractSearchResults.COL_ENT_PERSONS;
				break;
			case PRODUCTION:
				collec = event.getProductions();
				col = AbstractSearchResults.COL_ENT_PRODUCTIONS;
				break;
		}
		
		String str = "";
		Iterator<String> it = collec.iterator();
		while(it.hasNext())
		{	String mention = it.next();
			mention = mention.replaceAll("[\\n\\r]", " ");
			mention = mention.replaceAll("\"", "'");
			str = str + mention;
			if(it.hasNext())
				str = str + ", ";
		}
		result.put(col,str);
	}
	
	/**
	 * Adds the event cluster to the specified map.
	 * 
	 * @param event
	 * 		The event to consider.
	 * @param result
	 * 		The map to complete.
	 */
	private void exportEventCluster(Event event, Map<String,String> result)
	{	Integer evtCluster = event.cluster;
		if(evtCluster!=null)
		{	String clusterStr = Integer.toString(evtCluster);
			result.put(AbstractSearchResults.COL_EVENT_CLUSTER, clusterStr);
		}
	}
	
	/**
	 * Adds the event text to the specified map.
	 * 
	 * @param event
	 * 		The event to consider.
	 * @param result
	 * 		The map to complete.
	 */
	private void exportEventText(Event event, Map<String,String> result)
	{	String text = event.getText();
		if(text!=null)
			result.put(AbstractSearchResults.COL_EVENT_SENTENCE, text);
	}
	
	/**
	 * Adds the event information to the specified map.
	 * 
	 * @param event
	 * 		The event to consider.
	 * @param rank
	 * 		Event rank.
	 * @param result
	 * 		The map to complete.
	 */
	protected void exportEvent(Event event, int rank, Map<String,String> result)
	{	if(event!=null)
		{	// cluster
			exportEventCluster(event, result);
			
			// rank
			result.put(AbstractSearchResults.COL_EVENT_RANK,Integer.toString(rank));
			
			// sentence
			exportEventText(event, result);
			
			// mentions
			exportEventDateMentions(event, result);
			exportEventNamedMentions(event, EntityType.LOCATION, result);
			exportEventNamedMentions(event, EntityType.PERSON, result);
			exportEventNamedMentions(event, EntityType.ORGANIZATION, result);
			exportEventNamedMentions(event, EntityType.FUNCTION, result);
			exportEventNamedMentions(event, EntityType.PRODUCTION, result);
			exportEventNamedMentions(event, EntityType.MEETING, result);
		}
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
