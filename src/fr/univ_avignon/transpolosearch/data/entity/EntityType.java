package fr.univ_avignon.transpolosearch.data.entity;

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
 * along with Nerwip - Named Entity Extraction in Wikipedia Pages.  
 * If not, see <http://www.gnu.org/licenses/>.
 */

import java.awt.Color;

/**
 * Type of the entities.
 * 
 * @author Yasa Akbulut
 * @author Vincent Labatut
 */
public enum EntityType
{	/** Date entity */
	DATE(Color.PINK),
	/** Location entity */
	LOCATION(Color.ORANGE),
	/** Organization entity */
	ORGANIZATION(Color.CYAN),
	/** Person entity */
	PERSON(Color.YELLOW); 
	
	/**
	 * Builds an entity type.
	 * 
	 * @param color
	 * 		Color associated to the entity in the GUI.
	 */
	EntityType(Color color)
	{	this.color = color;
	}
	
	/////////////////////////////////////////////////////////////////
	// COLOR			/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	/** Color associated to the entity in the GUI */
	private Color color;
	
	/**
	 * Returns the color associated to the entity
	 * in the GUI.
	 * 
	 * @return
	 * 		Color associated to the entity.
	 */
	public Color getColor()
	{	return color;
	}
}
