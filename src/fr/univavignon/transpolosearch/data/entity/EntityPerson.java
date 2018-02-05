package fr.univavignon.transpolosearch.data.entity;

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

/**
 * Class representing a person entity, which is a kind of named entity.
 * 
 * @author Vincent Labatut
 */
public class EntityPerson extends AbstractNamedEntity
{	
	/**
	 * Constructs a person entity.
	 * 
	 * @param mainName
	 * 		Main string representation of the entity to create.
	 * @param internalId
	 * 		Internal id of the entity to create.
	 */
	public EntityPerson(String mainName, long internalId)
	{	super(mainName,internalId);
	}
	
	/////////////////////////////////////////////////////////////////
	// TYPE				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	public EntityType getType()
	{	return EntityType.PERSON;
	}
}
