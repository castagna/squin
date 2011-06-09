/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.util;

import java.util.Iterator;


/**
 * This is an abstract base class for iterators that convert each element
 * provided by an input iterator to a corresponding element of another type.
 * A concrete implementation of such a converting iterator just have to provide
 * an implementation of the abstract method {@link #convert}.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
abstract public class ConvertingIterator<I,O> implements Iterator<O>
{
	// members

	final protected Iterator<I> input;


	// initialization

	protected ConvertingIterator ( Iterator<I> input )
	{
		this.input = input;
	}


	// implementation of the Iterator interface

	final public boolean hasNext ()
	{
		return input.hasNext();
	}

	final public O next ()
	{
		return convert( input.next() );
	}

	final public void remove ()
	{
		input.remove();
	}


	// abstract methods

	/** Converts the given I object into an O object. */
	abstract protected O convert ( I i );
}