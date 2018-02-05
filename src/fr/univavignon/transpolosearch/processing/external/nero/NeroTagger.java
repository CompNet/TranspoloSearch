package fr.univavignon.transpolosearch.processing.external.nero;

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
 * Represents the tagger used by Nero.
 * 
 * @author Vincent Labatut
 */
public enum NeroTagger
{	/** 
     * Use the Conditional Random Fields Tagger
     * developped for the French language. 
     */
	CRF,
	/** 
	 * Use the Finite State Transducer Tagger
	 * developped for the French language. 
	 */
	FST;
}
