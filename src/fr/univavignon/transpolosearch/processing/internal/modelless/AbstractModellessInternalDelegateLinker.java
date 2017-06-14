package fr.univavignon.transpolosearch.processing.internal.modelless;

import fr.univavignon.transpolosearch.processing.InterfaceLinker;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.processing.internal.AbstractInternalDelegateLinker;
import fr.univavignon.transpolosearch.processing.internal.modelbased.AbstractModelbasedInternalDelegateRecognizer;

/**
 * This class is used to represent or implement linkers invocable 
 * internally, i.e. programmatically, from within Nerwip, and not
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
