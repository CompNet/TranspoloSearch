package fr.univ_avignon.transpolosearch.data.event;

/*
 * TranspoloSearch
 * Copyright 2015 Vincent Labatut
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

import java.util.Iterator;
import java.util.List;

import org.jdom2.Element;

import fr.univ_avignon.transpolosearch.data.article.Article;
import fr.univ_avignon.transpolosearch.data.entity.EntityDate;
import fr.univ_avignon.transpolosearch.data.entity.EntityLocation;
import fr.univ_avignon.transpolosearch.data.entity.EntityOrganization;
import fr.univ_avignon.transpolosearch.data.entity.EntityPerson;
import fr.univ_avignon.transpolosearch.data.entity.EntityType;
import fr.univ_avignon.transpolosearch.recognition.RecognizerName;
import fr.univ_avignon.transpolosearch.tools.time.Date;
import fr.univ_avignon.transpolosearch.tools.xml.XmlNames;

/**
 * An event is a group of entities:
 * <ul>
 *  <li>None, one or two dates (two dates means an interval);</li>
 *  <li>None, one or several persons;</li>
 *  <li>None, one or several organizations;</li>
 *  <li>None, one or several locations;</li>
 * </ul>
 * 
 * @author Vincent Labatut
 */
public class Event implements Comparable<Event>
{	
	/**
	 * Builds a new event using the specified date.
	 * 
	 * @param startDate
	 * 		Starting date.
	 */
	public Event(EntityDate startDate)
	{	this.startDate = startDate.getValue();
		this.endDate = null;
	}

	/**
	 * Builds a new event using the specified period.
	 * 
	 * @param startDate
	 * 		Starting date.
	 * @param endDate
	 * 		Ending date.
	 */
	public Event(EntityDate startDate, EntityDate endDate)
	{	this.startDate = startDate.getValue();
		this.endDate = endDate.getValue();
	}
	
	/////////////////////////////////////////////////////////////////
	// DATES			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Start date */
	private Date startDate = null;
	/** End date (or {@code null} if no end date) */
	private Date endDate = null;
	
	/////////////////////////////////////////////////////////////////
	// LOCATIONS		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	
	
	
	/////////////////////////////////////////////////////////////////
	// COMPARISON		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
//	/**
//	 * Checks if the specified entity overlaps
//	 * with this one. However, one should not
//	 * contain the other. Perfect matches are not
//	 * allowed neither.
//	 * 
//	 * @param entity
//	 * 		Entity to be compared with this one. 
//	 * @return
//	 * 		{@code true} only if they partially overlap.
//	 */
//	public boolean overlapsStrictlyWith(AbstractEntity<?> entity)
//	{	int startPos2 = entity.getStartPos();
//		int endPos2 = entity.getEndPos();
//		
//		boolean result = (startPos2<endPos && endPos2>endPos)
//			|| (startPos<endPos2 && endPos>endPos2);
//		
//		return result;
//	}
	
	/**
	 * Checks if the specified entity overlaps
	 * with this one. Inclusion and perfect match
	 * are also allowed.
	 * 
	 * @param entity
	 * 		Entity to be compared with this one. 
	 * @return
	 * 		{@code true} only if they partially overlap.
	 */
	public boolean overlapsWith(Event<?> entity)
	{	int startPos2 = entity.getStartPos();
		int endPos2 = entity.getEndPos();
		
		boolean result = (startPos2<=endPos && endPos2>=endPos)
			|| (startPos<=endPos2 && endPos>=endPos2);
		
		return result;
	}

	/**
	 * Checks if this entity overlaps with <i>at least</i>
	 * one of the entities in the specified list. Inclusion 
	 * and perfect match are also allowed.
	 * 
	 * @param entities
	 * 		List of entities to be compared with this one. 
	 * @return
	 * 		{@code true} only if this entity partially overlaps
	 * 		with at least one of the listed entities.
	 */
	public boolean overlapsWithOne(List<Event<?>> entities)
	{	boolean result = false;
		Iterator<Event<?>> it = entities.iterator();
		
		while(!result && it.hasNext())
		{	Event<?> entity = it.next();
			result = overlapsWith(entity);
		}
		
		return result;
	}

//	/**
//	 * Checks if the specified entity is strictly contained
//	 * in this entity. Perfect matches are not allowed.
//	 * 
//	 * @param entity
//	 * 		Entity to be compared with this one. 
//	 * @return
//	 * 		{@code true} only if this entity contained the specified one.
//	 */
//	public boolean containsStrictly(AbstractEntity<?> entity)
//	{	int startPos2 = entity.getStartPos();
//		int endPos2 = entity.getEndPos();
//		
//		boolean result = startPos2>=startPos && endPos2<endPos
//			|| startPos2>startPos && endPos2<=endPos;
//
//		return result;
//	}

	/**
	 * Checks if the specified entity is contained in,
	 * or matches this entity.
	 * 
	 * @param entity
	 * 		Entity to be compared with this one. 
	 * @return
	 * 		{@code true} only if this entity contained the specified one.
	 */
	public boolean contains(Event<?> entity)
	{	int startPos2 = entity.getStartPos();
		int endPos2 = entity.getEndPos();
		
		boolean result = startPos2>=startPos && endPos2<=endPos;
		
		return result;
	}
	
	/**
	 * Checks if the specified entity matches (spatially)
	 * exactly this entity.
	 * 
	 * @param entity
	 * 		Entity to be compared with this one. 
	 * @return
	 * 		{@code true} only if this entity matches the specified one.
	 */
	public boolean hasSamePosition(Event<?> entity)
	{	int startPos2 = entity.getStartPos();
		int endPos2 = entity.getEndPos();
	
		boolean result = startPos==startPos2 && endPos==endPos2;
		
		return result;
	}
	
	/**
	 * Returns the length of this entity,
	 * calculated from its positions in the text.
	 * 
	 * @return
	 * 		Length of the entity in characters.
	 */
	public int getLength()
	{	int result = endPos-startPos;
		return result;
	}

	/**
	 * Checks if this entity is located before
	 * the specified entity. Only the starting
	 * position is considered. And using a strict
	 * comparison.
	 * 
	 * @param entity
	 * 		Entity to compare to this one.
	 * @return
	 * 		{@code true} iff this entity is located 
	 * 		before the specified one.
	 */
	public boolean precedes(Event<?> entity)
	{	int startPos = entity.getStartPos();
		boolean result = this.startPos < startPos;
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// TEXT				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Checks if the text recorded for this entity
	 * corresponds to the one found in the article
	 * at the entity positions.
	 * 
	 * @param article
	 * 		Article of reference. 
	 * @return 
	 * 		{@code true} iff the article and entity texts are the same.
	 */
	public boolean checkText(Article article)
	{	String text = article.getRawText();
		String valueStr2 = text.substring(startPos,endPos);
		boolean result = valueStr.equals(valueStr2);
		return result;
	}
	
	@Override
	public String toString()
	{	String result = "ENTITY(";
		result = result + "STRING=\"" + valueStr+"\"";
		result = result + ", TYPE=" + getType(); 
		result = result + ", POS=("+startPos+","+endPos+")"; 
		result = result + ", SOURCE="+source.toString();
		if(value!=null)
			result = result + ", VALUE=(" + value.toString() + ")"; 
		result = result + ")";
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// XML				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Returns a representation of this entity
	 * as an XML element.
	 * 
	 * @return
	 * 		An XML element representing this entity.
	 */
	public abstract Element exportAsElement();
	
	/**
	 * Builds an entity from the specified
	 * XML element.
	 * 
	 * @param element
	 * 		XML element representing the entity.
	 * @param source
	 * 		Name of the NER tool which detected the entity.
	 * @return
	 * 		The entity corresponding to the specified element.
	 */
	public static Event<?> importFromElement(Element element, RecognizerName source)
	{	Event<?> result = null;
		
		String typeStr = element.getAttributeValue(XmlNames.ATT_TYPE);
		EntityType type = EntityType.valueOf(typeStr);
		switch(type)
		{	case DATE:
				result = EntityDate.importFromElement(element,source);
				break;
			case LOCATION:
				result = EntityLocation.importFromElement(element,source);
				break;
			case ORGANIZATION:
				result = EntityOrganization.importFromElement(element,source);
				break;
			case PERSON:
				result = EntityPerson.importFromElement(element,source);
				break;
		}
		
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// COMPARABLE		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public int compareTo(Event<?> o)
	{	int startPos = o.getStartPos();
		int result = this.startPos - startPos;
		if(result==0)
		{	int endPos = o.getEndPos();
			result = this.endPos - endPos;
			if(result==0)
			{	String valueStr = o.getStringValue();
				result = this.valueStr.compareTo(valueStr);
			}
		}
		return result;
	}
	
	@Override
	public int hashCode()
	{	String temp = startPos + ":" + endPos + ":" + valueStr + ":" + source;
		int result = temp.hashCode();
		return result;
	}
	
	@Override
	public boolean equals(Object obj)
	{	boolean result = false;
		
		if(obj!=null)
		{	if(obj instanceof Event<?>)
			{	Event<?> entity = (Event<?>)obj;
				int start = entity.getStartPos();
				if(this.startPos==start)
				{	int endPos = entity.getEndPos();
					if(this.endPos==endPos)
					{	String valueStr = entity.getStringValue();
						result = this.valueStr.equals(valueStr);
					}
				}
			}
		}
		
		return result;
	}
}
