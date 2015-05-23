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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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
public class Event
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
	
	/**
	 * Returns the start date of the event, or
	 * the only date if this is a single-date event.
	 * 
	 * @return
	 * 		Start (or only) date of this event.
	 */
	public Date getStartDate()
	{	return startDate;
	}
	
	/**
	 * Returns the end date of the event, or
	 * {@code null} if this is only described by
	 * a single date.
	 * 
	 * @return
	 * 		End date or {@code null} if single date event.
	 */
	public Date getEndDate()
	{	return endDate;
	}
	
	/////////////////////////////////////////////////////////////////
	// LOCATIONS		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of strings representing the locations associated to this event */
	private final Set<String> locations = new TreeSet<String>();
	
	/**
	 * Returns the set of locations associated
	 * to this event.
	 * 
	 * @return
	 * 		A list of location names.
	 */
	public Collection<String> getLocations()
	{	return locations;
	}
	
	/**
	 * Adds a location name to the current list.
	 * 
	 * @param location
	 * 		The location entity whose <i>normalized</i>
	 * 		name will be added to this event.
	 */
	public void addLocation(EntityLocation location)
	{	String normalizedName = location.getValue();
		locations.add(normalizedName);
	}
	
	/////////////////////////////////////////////////////////////////
	// ORGANIZATIONS	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of strings representing the organizations associated to this event */
	private final Set<String> organizations = new TreeSet<String>();
	
	/**
	 * Returns the set of organizations associated
	 * to this event.
	 * 
	 * @return
	 * 		A list of organization names.
	 */
	public Collection<String> getOrganizations()
	{	return organizations;
	}
	
	/**
	 * Adds an organization name to the current list.
	 * 
	 * @param organization
	 * 		The organization entity whose <i>normalized</i>
	 * 		name will be added to this event.
	 */
	public void addOrganization(EntityOrganization organization)
	{	String normalizedName = organization.getValue();
		locations.add(normalizedName);
	}
	
	/////////////////////////////////////////////////////////////////
	// PERSONS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of strings representing the persons associated to this event */
	private final Set<String> persons = new TreeSet<String>();
	
	/**
	 * Returns the set of persons associated
	 * to this event.
	 * 
	 * @return
	 * 		A list of person names.
	 */
	public Collection<String> getPersons()
	{	return persons;
	}
	
	/**
	 * Adds a person name to the current list.
	 * 
	 * @param person
	 * 		The person entity whose <i>normalized</i>
	 * 		name will be added to this event.
	 */
	public void addPerson(EntityPerson person)
	{	String normalizedName = person.getValue();
		locations.add(normalizedName);
	}
	
	/////////////////////////////////////////////////////////////////
	// COMPATIBILITY	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	public boolean isCompatible(Event event)
	{	boolean result;
		
		// check if dates are compatible
		result = areCompatibleDates(startDate,endDate,event.startDate,event.endDate);

		// check if at least one compatible location
		if(result)
			result = areCompatibleLocations(locations,event.locations);
		
		// check if at least one compatible person
		if(result)
			result = areCompatiblePersons(persons,event.persons);
	
		// check if at least one compatible organization
		if(result)
			result = areCompatibleOrganizations(organizations,event.organizations);

		return result;
	}
	
	public boolean areCompatibleDates(Date startDate1, Date endDate1, Date startDate2, Date endDate2)
	{	boolean result;
		
		// TODO
		
		return result;
	}
	
	public boolean areCompatibleLocations(Set<String> locations1, Set<String> locations2)
	{	boolean result;
		
		// TODO
		
		return result;
	}
	
	public boolean areCompatiblePersons(Set<String> persons1, Set<String> persons2)
	{	boolean result;
		
		// TODO
		
		return result;
	}
	
	public boolean areCompatibleOrganizations(Set<String> organizations1, Set<String> organizations2)
	{	boolean result;
		
		// TODO
		
		return result;
	}
	
	/**
	 * TODO faudra identifier les entités de façon unique
	 *  - changer la "value" des entités pour un code genre freebase
	 *  - adapter les classes entités, notamment l'enregistrement/lecture de ce code
	 *  - traiter les cas non répertoriés dans freebase (liste manuelle ? outil de désambiguisation ?)
	 *  - créer de vraies classes "entités valeurs" et renommer les existantes en "mentions d'entités"
	 *  - peut être placer les tests de compatibilité directement dans les classes correspondant aux valeurs d'entités ?
	 *  - autres ?
	 */
	
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
	public Element exportAsElement()
	{
		
	}
	
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
	public static Event importFromElement(Element element, RecognizerName source)
	{	Event result = null;
		
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
}
