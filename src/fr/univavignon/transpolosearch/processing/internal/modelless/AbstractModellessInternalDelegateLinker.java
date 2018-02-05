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

import fr.univavignon.transpolosearch.processing.InterfaceLinker;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.processing.internal.AbstractInternalDelegateLinker;
import fr.univavignon.transpolosearch.processing.internal.modelbased.AbstractModelbasedInternalDelegateRecognizer;

/**
 * This class is used to represent or implement linkers invocable 
 * internally, i.e. programmatically, from within the software, and not
 * using any model, i.e. external files to be loaded (as opposed to
 * {@link AbstractModelbasedInternalDelegateRecognizer} recognizers.
 * 
 * @param <T>
 * 		Class of the internal representation of the mentions resulting from the detection.
 * 		 
 * @author Vincent Labatut
 */
public abstract class AbstractModellessInternalDelegateLinker<T> extends AbstractInternalDelegateLinker<T>
{	
	/**
	 * Builds a new internal recognizer,
	 * using the specified options.
	 * 
	 * @param linker
	 * 		Linker associated to this delegate.
	 * @param revision
	 * 		Whether or not merge entities previously considered
	 * 		as distinct, but turning out to be linked to the same id.
	 */
	public AbstractModellessInternalDelegateLinker(InterfaceLinker linker, boolean revision)
	{	super(linker,revision);
	}
	
	/////////////////////////////////////////////////////////////////
	// PROCESSING		/////////////////////////////////////////////
	/////////////////////////////////////////////////////////////////
	@Override
	protected void prepareLinker() throws ProcessorException
	{	// nothing to do here
	}
}
