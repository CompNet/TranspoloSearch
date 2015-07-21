package fr.univavignon.transpolosearch.recognition;

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

/**
 * Problem while performing the detection
 * of entities in some text.
 * 
 * @author Yasa Akbulut
 * @author Vincent Labatut
 */
public class RecognizerException extends Exception
{	/** Class id */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Builds a new exception,
	 * without any message.
	 */
	public RecognizerException()
	{ super();
	}
	
	/**
	 * Builds a new exception,
	 * using the specified message.
	 * 
	 * @param message
	 * 		Exception message.
	 */
	public RecognizerException(String message)
	{	super(message);
	}
}
