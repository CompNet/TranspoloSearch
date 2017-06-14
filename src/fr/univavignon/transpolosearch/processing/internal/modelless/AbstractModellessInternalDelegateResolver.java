package fr.univavignon.transpolosearch.processing.internal.modelless;

import fr.univavignon.transpolosearch.processing.InterfaceResolver;
import fr.univavignon.transpolosearch.processing.ProcessorException;
import fr.univavignon.transpolosearch.processing.internal.AbstractInternalDelegateResolver;
import fr.univavignon.transpolosearch.processing.internal.modelbased.AbstractModelbasedInternalDelegateRecognizer;

/**
 * This class is used to represent or implement recognizers invocable 
 * internally, i.e. programmatically, from within Nerwip, and not
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
