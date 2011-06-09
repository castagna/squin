/*
    This file is part of SQUIN and it falls under the
    copyright as specified for the whole SQUIN package.
*/
package org.squin.util;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * This is a generic implementation of an iterator that concatenates multiple
 * input iterators of the same type.
 * Sequentially, this iterator returns all elements of all input iterators.
 *
 * @author Olaf Hartig (hartig@informatik.hu-berlin.de)
 */
public class ConcatenatingIterator<T> implements Iterator<T>
{
	// member

	final protected Iterator<Iterator<T>> inputIterators;
	protected Iterator<T> currentIterator = null;


	// initialization

	public ConcatenatingIterator ( Iterator<Iterator<T>> inputIterators )
	{
		assert inputIterators != null;
		this.inputIterators = inputIterators;
	}

	public ConcatenatingIterator ( List<Iterator<T>> inputIterators )
	{
		this.inputIterators = inputIterators.iterator();
	}


	// implementation of the Iterator interface

	public boolean hasNext ()
	{
		while ( currentIterator == null || ! currentIterator.hasNext() ) {
			if ( ! inputIterators.hasNext() ) {
				return false;
			}
			currentIterator = inputIterators.next();
		}
		return true;
	}

	public T next ()
	{
		if ( ! hasNext() ) {
			throw new NoSuchElementException();
		}
		return currentIterator.next();
	}

	public void remove ()
	{
		throw new UnsupportedOperationException();
	}

}
