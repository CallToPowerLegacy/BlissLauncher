package org.indin.blisslaunchero.framework.utils;

import android.util.LongSparseArray;

import java.util.Iterator;

/**
 * Extension of {@link LongSparseArray} with some utility methods.
 */
public class LongArrayMap<E> extends LongSparseArray<E> implements Iterable<E> {

    public boolean containsKey(long key) {
        return indexOfKey(key) >= 0;
    }

    public boolean isEmpty() {
        return size() <= 0;
    }

    @Override
    public LongArrayMap<E> clone() {
        return (LongArrayMap<E>) super.clone();
    }

    @Override
    public Iterator<E> iterator() {
        return new ValueIterator();
    }

    class ValueIterator implements Iterator<E> {

        private int mNextIndex = 0;

        @Override
        public boolean hasNext() {
            return mNextIndex < size();
        }

        @Override
        public E next() {
            return valueAt(mNextIndex++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
