package fr.univavignon.transpolosearch.data.event;

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

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.jdom2.Element;

import fr.univavignon.transpolosearch.data.entity.EntityDate;
import fr.univavignon.transpolosearch.data.entity.EntityLocation;
import fr.univavignon.transpolosearch.data.entity.EntityOrganization;
import fr.univavignon.transpolosearch.data.entity.EntityPerson;
import fr.univavignon.transpolosearch.tools.string.StringTools;
import fr.univavignon.transpolosearch.tools.time.Date;

/**
 * An event is a group of entities:
 * <ul>
 *  <li>None, one or two dates (two dates means an interval);</li>
 *  <li>None, one or several persons;</li>
 *  <li>None, one or several organizations;</li>
 *  <li>None, one or several locations;</li>
 * </ul>
 * <br/>
 * A basic event should contain at least either two acting entity (person or
 * organization), or one acting entity and a date.
 * <br/>
 * TODO This is a very basic version, that should be improved later. As of now,
 * entities are compared just by matching their names <i>exactly</i>: hierarchies are not
 * taken into account, nor are the various versions of proper names.
 * <ul>
 *  <li>Change the name of the current entity classes to "entity mentions" (which they are) and create an actual entity class</li>
 *  <li>Maybe change the "value" field in the current entities to a full object, and define an actual entity class</li>
 *  <li>Complete the compatibility tests from this class, and move them in the actual entity classes</li>
 *  <li>The result of the compatibility tests could be a real value instead of a Boolean one</li>
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
	// TEXT				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Text containing the event */
	private String text = null;
	
	/**
	 * Returns the text containing this event.
	 * 
	 * @return
	 * 		Text containing this event.
	 */
	public String getText()
	{	return text;
	}
	
	/**
	 * Changes the text containing this event.
	 * 
	 * @param text
	 * 		New text containing this event.
	 */
	public void setText(String text)
	{	this.text = text;
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
	
	/**
	 * Combines the specified date to the existing ones in
	 * this event. If the date is more extreme than one of
	 * the bounds, it becomes the new bound. Same thing if
	 * it is more <i>precise</i>.
	 * 
	 * @param date
	 * 		The date to merge to this event current dates.
	 * @return
	 * 		{@code true} iff one of the start/end dates of
	 * 		this event was modified during the processed.
	 */
	public boolean mergeDate(Date date)
	{	boolean changedStart = false;
		boolean changedEnd = false;
		if(startDate==null)
		{	startDate = date;
			changedStart = true;
		}
		else
		{	int year1 = startDate.getYear();
			int year2 = date.getYear();
			int month1 = startDate.getMonth();
			int month2 = date.getMonth();
			int day1 = startDate.getDay();
			int day2 = date.getDay();
			
			// check date vs start date
			if(year1==0)
			{	startDate = date;
				changedStart = true;
			}
			else if(year2<year1)
			{	if(endDate==null)
				{	endDate = startDate;
					changedEnd = true;
				}
				startDate = date;
				changedStart = true;
			}	
			else if(year2==year1)
			{	if(month1==0)
				{	startDate = date;
					changedStart = true;
				}
				else if(month2<month1)
				{	if(endDate==null)
					{	endDate = startDate;
						changedEnd = true;
					}
					startDate = date;
					changedStart = true;
				}
				else if(month2==month1)
				{	if(day1==0)
					{	startDate = date;
						changedStart = true;
					}
					else if(day2<day1)
					{	if(endDate==null)
						{	endDate = startDate;
							changedEnd = true;
						}
						startDate = date;
						changedStart = true;
					}
				}
			}
			
			// check date vs end date
			if(!changedStart && !changedEnd)
			{	if(endDate==null)
				{	endDate = date;
					changedEnd = true;
				}
			
				else
				{	year1 = endDate.getYear();
					month1 = endDate.getMonth();
					day1 = endDate.getDay();
					
					if(year2>year1)
					{	endDate = date;
						changedEnd = true;
					}
					else if(year2==year1)
					{	if(month2>month1)
						{	endDate = date;
							changedEnd = true;
						}
						else if(month2==month1)
						{	if(day2>day1)
							{	endDate = date;
								changedEnd = true;
							}
						}
					}
				}
			}
		}
		
		boolean result = changedEnd || changedStart;
		return result; 
	}
	
	/////////////////////////////////////////////////////////////////
	// LOCATIONS		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of strings representing the locations associated to this event */
	private final Set<String> locations = new TreeSet<String>(StringTools.COMPARATOR); //TODO comparator should be removed if we use ids instead of plain names. same thing in the other sets
	
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
		if(normalizedName==null)
			normalizedName = location.getStringValue();
		locations.add(normalizedName);
	}
	
	/**
	 * Adds the location names to the current list.
	 * 
	 * @param locations
	 * 		The location entities whose <i>normalized</i>
	 * 		name will be added to this event.
	 */
	public void addLocation(List<EntityLocation> locations)
	{	for(EntityLocation location: locations)
			addLocation(location);
	}
	
	/////////////////////////////////////////////////////////////////
	// ORGANIZATIONS	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of strings representing the organizations associated to this event */
	private final Set<String> organizations = new TreeSet<String>(StringTools.COMPARATOR);
	
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
		if(normalizedName==null)
			normalizedName = organization.getStringValue();
		organizations.add(normalizedName);
	}
	
	/**
	 * Adds the organization names to the current list.
	 * 
	 * @param organizations
	 * 		The location entities whose <i>normalized</i>
	 * 		name will be added to this event.
	 */
	public void addOrganizations(List<EntityOrganization> organizations)
	{	for(EntityOrganization organization: organizations)
			addOrganization(organization);
	}
	
	/////////////////////////////////////////////////////////////////
	// PERSONS			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** List of strings representing the persons associated to this event */
	private final Set<String> persons = new TreeSet<String>(StringTools.COMPARATOR);
	
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
		if(normalizedName==null)
			normalizedName = person.getStringValue();
		persons.add(normalizedName);
	}
	
	/**
	 * Adds the person names to the current list.
	 * 
	 * @param persons
	 * 		The person entities whose <i>normalized</i>
	 * 		name will be added to this event.
	 */
	public void addPerson(List<EntityPerson> persons)
	{	for(EntityPerson person: persons)
			addPerson(person);
	}
	
	/////////////////////////////////////////////////////////////////
	// COMPATIBILITY	/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/**
	 * Determines if the specified event instance is compatible with
	 * this one, i.e. represents to the same <i>actual</i> event.
	 * <br/>
	 * See the other compatibility methods for details.
	 * 
	 * @param event
	 * 		The event to compare to this one.
	 * @return
	 * 		{@code true} iff both events have the same semantical value.
	 */
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
	
	/**
	 * Determines if the specified event dates are compatible, 
	 * i.e. represent similar dates (not necessarily exactly the
	 * same date).
	 * <br/>
	 * For now, we check if one date/period contains the other.
	 * 
	 * @param startDate1
	 * 		Start date of the first event.
	 * @param endDate1
	 * 		End date of the first event.
	 * @param startDate2
	 * 		Start date of the second event.
	 * @param endDate2
	 * 		End date of the second event.
	 * @return
	 * 		{@code true} iff both periods/dates have similar semantical value.
	 */
	public boolean areCompatibleDates(Date startDate1, Date endDate1, Date startDate2, Date endDate2)
	{	boolean result = startDate1.isCompatible(startDate2)
			&& ((endDate1==null && endDate2==null) || endDate1.isCompatible(endDate2)); 
		
		return result;
	}
	
	/**
	 * Determines if the specified event locations are compatible, 
	 * i.e. represent the same place.
	 * <br/>
	 * For now, two locations are compared by testing whether both strings match exactly.
	 * Two sets of locations are compatible if they contain a common location (given the
	 * previous definition of location comparison).
	 * 
	 * @param locations1
	 * 		Locations of the first event.
	 * @param locations2
	 * 		Locations of the second event.
	 * @return
	 * 		{@code true} iff both location sets have similar semantical value.
	 */
	public boolean areCompatibleLocations(Set<String> locations1, Set<String> locations2)
	{	Set<String> locs = new TreeSet<String>(locations1);
		locs.retainAll(locations2);
		
		boolean result = !locs.isEmpty();
		return result;
	}
	
	/**
	 * Determines if the specified event persons are compatible, 
	 * i.e. represent the same people.
	 * <br/>
	 * For now, two persons are compared by testing whether both strings match exactly.
	 * Two sets of persons are compatible if they contain a common person (given the
	 * previous definition of person comparison).
	 * 
	 * @param persons1
	 * 		Persons of the first event.
	 * @param persons2
	 * 		Persons of the second event.
	 * @return
	 * 		{@code true} iff both person sets have similar semantical value.
	 */
	public boolean areCompatiblePersons(Set<String> persons1, Set<String> persons2)
	{	Set<String> pers = new TreeSet<String>(persons1);
		pers.retainAll(persons2);
	
		boolean result = !pers.isEmpty();
		return result;
	}
	
	/**
	 * Determines if the specified event organizations are compatible, 
	 * i.e. represent the same companies.
	 * <br/>
	 * For now, two organizations are compared by testing whether both strings match exactly.
	 * Two sets of organizations are compatible if they contain a common organization (given the
	 * previous definition of organization comparison).
	 * 
	 * @param organizations1
	 * 		Organizations of the first event.
	 * @param organizations2
	 * 		Organizations of the second event.
	 * @return
	 * 		{@code true} iff both organization sets have similar semantical value.
	 */
	public boolean areCompatibleOrganizations(Set<String> organizations1, Set<String> organizations2)
	{	Set<String> orgs = new TreeSet<String>(organizations1);
		orgs.retainAll(organizations2);
	
		boolean result = !orgs.isEmpty();
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
	public Element exportAsElement()
	{	Element result = null;
		//TODO
		return result;
	}
	
	/**
	 * Builds an entity from the specified
	 * XML element.
	 * 
	 * @param element
	 * 		XML element representing the entity.
	 * @return
	 * 		The entity corresponding to the specified element.
	 */
	public static Event importFromElement(Element element)
	{	Event result = null;
		
		// TODO	
		
		return result;
	}

	/////////////////////////////////////////////////////////////////
	// STRING			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public String toString()
	{	String result = "<";
		
		if(endDate==null)
			result = result + "date=" + startDate;
		else
			result = result + " start=" + startDate + " end=" + endDate;
		
		if(!persons.isEmpty())
			result = result + " persons=" + persons;
		
		if(!locations.isEmpty())
			result = result + " locations=" + locations;
		
		if(!organizations.isEmpty())
			result = result + " organizations=" + organizations;
		
		result = result + " >";
		return result;
	}
}
