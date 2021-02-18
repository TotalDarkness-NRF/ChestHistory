package net.totaldarkness.ChestHistory.client.util;

import java.util.*;
import java.util.stream.Stream;

public class AutoRemoveSet<E> implements Set<E> {

    final HashMap<E, Long> map;
    final int maxTime;

    public AutoRemoveSet() {
        this(0);
    }

    public AutoRemoveSet(final int maxTime) {
        map = new HashMap<>();
        this.maxTime = maxTime;
    }

    @Override
    public int size() {
        int result = 0;
        final Iterator<E> iterator = this.iterator();
        while (iterator.hasNext()) {
            result ++;
            iterator.next();
        }
        return result;
    }

    @Override
    public boolean isEmpty() {
        return size() < 1;
    }

    @Override
    public boolean contains(final Object element) {
        return getTimeLeft(element) > 0;
    }

    @Override
    public Iterator<E> iterator() {
        return this.map.keySet().stream().filter(this::contains).iterator();
    }

    /**
     * just for debugging...
     * @return
     */
    public Stream<E> streamAll(){
        return this.map.keySet().stream();
    }

    @Override
    public Object[] toArray() {
        return stream().toArray();
    }

    @Override
    public <E> E[] toArray(final E[] a) {
        return stream().toArray(integer -> Arrays.copyOf(a, integer));
    }

    @Override
    public boolean add(final E element) {
        return add(element, maxTime);
    }

    public boolean add(final E element, final int maxTime) {
        return map.put(element, System.currentTimeMillis() + maxTime) == null;
    }

    @Override
    public boolean remove(final Object element) {
        return map.remove(element, map.get(element));
    }

    @Override
    public boolean containsAll(final Collection<?> collection) {
        return collection.stream().allMatch(this::contains);
    }

    @Override
    public boolean addAll(final Collection<? extends E> collection) {
        // allMatch has to be used because this isn't after style guides and the operation add has to return a boolean
        return collection.stream().allMatch(this::add);
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        // from AbstractCollectfion line 405
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<E> it = iterator();
        while (it.hasNext()) {
            if (!c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        // from AbstractCollection line 371
        Objects.requireNonNull(c);
        boolean modified = false;
        Iterator<?> it = iterator();
        while (it.hasNext()) {
            if (c.contains(it.next())) {
                it.remove();
                modified = true;
            }
        }
        return modified;
    }

    @Override
    public void clear() {
        map.clear();
    }

    public int getTimeLeft(final Object element) {
        final Long expiration = this.map.get(element);
        if (expiration == null) {
            return 0;
        }
        return (int) (expiration - System.currentTimeMillis());
    }

    public void resetTimer(final E element) {
        map.put(element, System.currentTimeMillis() + maxTime);
    }
}


