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

import fr.univavignon.transpolosearch.processing.InterfaceResolver;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.processing.internal.AbstractInternalDelegateResolver;
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
public abstract class AbstractModellessInternalDelegateResolver<T> extends AbstractInternalDelegateResolver<T>
{	
	/**
	 * Builds a new internal recognizer,
	 * using the specified options.
	 * 
	 * @param resolver
	 * 		Resolver associated to this delegate.
	 * @param resolveHomonyms
	 * 		Whether unresolved named entities should be resolved based
	 * 		on exact homonymy, or not.
	 */
	public AbstractModellessInternalDelegateResolver(InterfaceResolver resolver, boolean resolveHomonyms)
	{	super(resolver,resolveHomonyms);
	}
	
	/////////////////////////////////////////////////////////////////
	// PROCESSING		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected void prepareResolver() throws ProcessorException
	{	// nothing to do here
	}
}
