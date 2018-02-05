package fr.univavignon.transpolosearch.processing.internal.modelless;

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

import fr.univavignon.transpolosearch.processing.InterfaceRecognizer;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.processing.internal.AbstractInternalDelegateRecognizer;
import fr.univavignon.transpolosearch.processing.internal.modelbased.AbstractModelbasedInternalDelegateRecognizer;

/**
 * This class is used to represent or implement recognizers invocable 
 * internally, i.e. programmatically, from within the software, and not
 * using any model, i.e. external files to be loaded (as opposed to
 * {@link AbstractModelbasedInternalDelegateRecognizer} recognizers.
 * 
 * @param <T>
 * 		Class of the internal representation of the mentions resulting from the detection.
 * 		 
 * @author Vincent Labatut
 */
public abstract class AbstractModellessInternalDelegateRecognizer<T> extends AbstractInternalDelegateRecognizer<T>
{	
	/**
	 * Builds a new internal recognizer,
	 * using the specified options.
	 * 
	 * @param recognizer
	 * 		Recognizer associated to this delegate.
	 * @param trim
	 * 		Whether or not the beginings and ends of mentions should be 
	 * 		cleaned from any non-letter/digit chars.
	 * @param ignorePronouns
	 * 		Whether or not pronouns should be ignored.
	 * @param ignoreNumbers
	 * 		Whether or not numbers should be ignored (only for non-dates).
	 * @param exclusionOn
	 * 		Whether or not stop words should be ignored.
	 */
	public AbstractModellessInternalDelegateRecognizer(InterfaceRecognizer recognizer, boolean trim, boolean ignorePronouns, boolean ignoreNumbers, boolean exclusionOn)
	{	super(recognizer,trim,ignorePronouns,ignoreNumbers,exclusionOn);
	}
	
	/////////////////////////////////////////////////////////////////
	// PROCESSING		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected void prepareRecognizer() throws ProcessorException
	{	// nothing to do here
	}
}
