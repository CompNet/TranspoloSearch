package fr.univavignon.transpolosearch.data.entity;

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

import fr.univavignon.transpolosearch.tools.time.Period;

/**
 * Abstract class representing a valued entity, i.e. an entity
 * associated to some specific numerical value.
 * 
 * @param <T>
 * 		Type of the entity value.
 *  
 * @author Vincent Labatut
 */
public abstract class AbstractValuedEntity<T extends Comparable<T>> extends AbstractEntity
{	
	/**
	 * General constructor for a valued entity.
	 * 
	 * @param value
	 * 		Numerical value associated to the entity to create.
	 * @param internalId
	 * 		Internal id of the entity to create.
	 */
	public AbstractValuedEntity(T value, long internalId)
	{	super(internalId);
		
		if(value==null)
			throw new NullPointerException("Tried to create a new valued entity with a null value");
		else
			this.value = value;
	}
	
	/**
	 * Builds a named entity of the specified type, using the specified
	 * name and id.
	 * 
	 * @param internalId
	 * 		Id of the entity to build ({@code -1} to automatically define it 
	 * 		when inserting in an {@link Entities} object).
	 * @param value
	 * 		Value of the entity to build.
	 * @param type
	 * 		Entity type of the entity to build.
	 * @return
	 * 		The built entity.
	 */
	public static AbstractValuedEntity<?> buildEntity(long internalId, Comparable<?> value, EntityType type)
	{	AbstractValuedEntity<?> result = null;
		switch(type)
		{	case DATE:
				result = new EntityDate((Period)value,internalId);
				break;
		}
		return result;
	}
	
	/////////////////////////////////////////////////////////////////
	// VALUE			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Value corresponding to this entity */
	protected T value = null;
	
	/**
	 * Returns the value of this entity.
	 * 
	 * @return
	 * 		Value of this entity. 
	 */
	public T getValue()
	{	return value;
	}

	/**
	 * Changes the value of this entity.
	 * 
	 * @param value
	 * 		New value of this entity.
	 */
	public void setValue(T value)
	{	this.value = value;
	}

	/////////////////////////////////////////////////////////////////
	// OBJECT			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public String toString()
	{	String result = getType().toString()+"(";
		result = result + "ID=" + internalId + "";
		result = result + ", VALUE=\"" + value + "\"";
		result = result + ")";
		return result;
	}
}
