package fr.univ_avignon.transpolosearch.recognition.internal.modelless;

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

import fr.univ_avignon.transpolosearch.recognition.RecognizerException;
import fr.univ_avignon.transpolosearch.recognition.internal.AbstractInternalConverter;
import fr.univ_avignon.transpolosearch.recognition.internal.AbstractInternalRecognizer;

/**
 * This class is used to represent or implement NER tools invocable 
 * internally, i.e. programmatically, from within Nerwip, and not
 * using any model, i.e. external files to be loaded (as opposed to
 * {@code AbstractModelBasedInternalRecognizer} recognizers.
 * 
 * @param <T>
 * 		Class of the converter associated to this recognizer.
 * @param <U>
 * 		Class of the internal representation of the entities resulting from the detection.
 * 		 
 * @author Vincent Labatut
 */
public abstract class AbstractModellessInternalRecognizer<U,T extends AbstractInternalConverter<U>> extends AbstractInternalRecognizer<U,T>
{	
	/**
	 * Builds a new internal recognizer,
	 * using the specified options.
	 * 
	 * @param trim
	 * 		Whether or not the beginings and ends of entities should be 
	 * 		cleaned from any non-letter/digit chars.
	 * @param ignorePronouns
	 * 		Whether or not pronouns should be ignored.
	 * @param exclusionOn
	 * 		Whether or not stop words should be ignored.
	 */
	public AbstractModellessInternalRecognizer(boolean trim, boolean ignorePronouns, boolean exclusionOn)
	{	super(trim,ignorePronouns,exclusionOn);
	}
	
	/////////////////////////////////////////////////////////////////
	// MISC				/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected void prepareRecognizer() throws RecognizerException
	{	// nothing to do here
	}
}
