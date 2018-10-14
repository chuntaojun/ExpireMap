package com.hashexpiremap.lct;


import com.sun.org.apache.bcel.internal.classfile.ClassFormatException;

import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

/**
 * @author liaochuntao
 */
public class HashExpireMap<K, V, T extends Long> extends Observable {

    ScheduledExecutorService service = newSingleThreadScheduledExecutor();

    /**
     * The default initial capacity - MUST be a power of two.
     */
    static final int DEFAULT_INITIAL_CAPACITY = 1 << 4; // aka 16

    /**
     * The maximum capacity, used if a higher value is implicitly specified
     * by either of the constructors with arguments.
     * MUST be a power of two <= 1<<30.
     */
    static final int MAXIMUM_CAPACITY = 1 << 30;

    /**
     * The load factor used when none specified in constructor.
     */
    static final float DEFAULT_LOAD_FACTOR = 0.75f;

    /**
     * The bin count threshold for using a tree rather than list for a
     * bin.  Bins are converted to trees when adding an element to a
     * bin with at least this many nodes. The value must be greater
     * than 2 and should be at least 8 to mesh with assumptions in
     * tree removal about conversion back to plain bins upon
     * shrinkage.
     */
    static final int TREEIFY_THRESHOLD = 8;

    /**
     * The bin count threshold for untreeifying a (split) bin during a
     * resize operation. Should be less than TREEIFY_THRESHOLD, and at
     * most 6 to mesh with shrinkage detection under removal.
     */
    static final int UNTREEIFY_THRESHOLD = 6;

    /**
     * The smallest table capacity for which bins may be treeified.
     * (Otherwise the table is resized if too many nodes in a bin.)
     * Should be at least 4 * TREEIFY_THRESHOLD to avoid conflicts
     * between resizing and treeification thresholds.
     */
    static final int MIN_TREEIFY_CAPACITY = 64;

    /**
     * The number of key-value mappings contained in this map.
     */
    transient int size;

    /**
     * The number of times this HashMap has been structurally modified
     * Structural modifications are those that change the number of mappings in
     * the HashMap or otherwise modify its internal structure (e.g.,
     * rehash).  This field is used to make iterators on Collection-views of
     * the HashMap fail-fast.  (See ConcurrentModificationException).
     */
    transient int modCount;

    /**
     * The next size value at which to resize (capacity * load factor).
     *
     * @serial
     */
    // (The javadoc description is true upon serialization.
    // Additionally, if the table array has not been allocated, this
    // field holds the initial array capacity, or zero signifying
    // DEFAULT_INITIAL_CAPACITY.)
    int threshold;

    /**
     * The load factor for the hash table.
     *
     * @serial
     */
    final float loadFactor;

    public HashExpireMap(long initialDelay,
                         long period,
                         TimeUnit timeUnit) {
        addObserver(new ExpireNotify());
        service.scheduleAtFixedRate(new HashExpireMap.ScanThread(), initialDelay, period + 10,
                timeUnit);
        // all other fields defaulted
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    static class Node<K, V, T> {
        final int hash;
        final K key;
        V value;
        T time;
        Node<K, V, T> next;

        Node(int hash, K key, V value, T time, Node<K, V, T> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.time = time;
            this.next = next;
        }

        public final K getKey() {
            return key;
        }

        public final V getValue() {
            return value;
        }

        public final T getTime() {
            return time;
        }

        @Override
        public final String toString() {
            return key + "=" + value + "=" + time;
        }

        @Override
        public final int hashCode() {
            return Objects.hashCode(key) ^ Objects.hashCode(value) ^ Objects.hashCode(time);
        }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }

        @Override
        public final boolean equals(Object o) {
            if (o == this)
                return true;
            if (o instanceof Node) {
                Node<?, ?, ?> e = (Node<?, ?, ?>) o;
                if (Objects.equals(key, e.getKey()) &&
                        Objects.equals(value, e.getValue()) && Objects.equals(time, e.getTime()))
                    return true;
            }
            return false;
        }
    }

    HashExpireMap.Node<K, V, T> newNode(int hash, K key, V value, T time, HashExpireMap.Node<K, V, T> next) {
        return new HashExpireMap.Node<>(hash, key, value, time, next);
    }

    static class Entry<K, V, T> extends Node {
        Entry(int hash, Object key, Object value, Object time, Node next) {
            super(hash, key, value, time, next);
        }
    }

    static class Item<V, T> {
        V value;
        T time;

        public Item(V value, T time) {
            this.value = value;
            this.time = time;
        }


    }

    static final int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    static int indexFor(int h, int length) {
        return h & (length - 1);
    }

    static final int tableSizeFor(int cap) {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    transient HashExpireMap.Node<K, V, T>[] table;
    transient Set<HashExpireMap.Node<K, V, T>> entrySet;

    public V put(K key, V value) {
        return putVal(hash(key), key, value, (T) Long.valueOf(System.currentTimeMillis() + 6 * 60 * 1000), false, true);
    }

    public V put(K key, V value, String time) {
        if (time instanceof String) {
            return putVal(hash(key), key, value, setExpireTime(time), false, true);
        }
        throw new ClassFormatException("The time must be java.lang.String");
    }

    public Item get(Object key) {
        HashExpireMap.Node<K,V,T> e;
        return (e = getNode(hash(key), key)) == null ? null : new Item(e.value, e.time);
    }

    public Item remove(Object key) {
        HashExpireMap.Node<K,V, T> e;
        return (e = removeNode(hash(key), key, null, false, true)) == null ?
                null : new Item(e.value, e.time);
    }

    public boolean containsKey(Object key) {
        return getNode(hash(key), key) != null;
    }

    final V putVal(int hash, K key, V value, T time, boolean onlyIfAbsent,
                   boolean evict) {
        HashExpireMap.Node<K, V, T>[] tab;
        HashExpireMap.Node<K, V, T> p;
        int n, i;
        if ((tab = table) == null || (n = tab.length) == 0)
            n = (tab = resize()).length;
        if ((p = tab[i = (n - 1) & hash]) == null)
            tab[i] = newNode(hash, key, value, time, null);
        else {
            HashExpireMap.Node<K, V, T> e = null;
            K k;
            for (int binCount = 0; ; ++binCount) {
                if ((e = p.next) == null) {
                    p.next = newNode(hash, key, value, time, null);
                    break;
                }
                if (e.hash == hash && ((k = e.key) == key || (key != null && key.equals(k))))
                    break;
                p = e;
            }
            if (e != null) { // existing mapping for key
                V oldValue = e.value;
                if (!onlyIfAbsent || oldValue == null)
                    e.value = value;
                return oldValue;
            }
        }
        ++modCount;
        if (++size > threshold)
            resize();
        return null;
    }

    final HashExpireMap.Node<K, V, T> getNode(int hash, Object key) {
        HashExpireMap.Node<K, V, T>[] tab;
        HashExpireMap.Node<K, V, T> first, e;
        int n;
        K k;
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (first = tab[(n - 1) & hash]) != null) {
            if (first.hash == hash && // always check first node
                    ((k = first.key) == key || (key != null && key.equals(k))))
                return first;
            if ((e = first.next) != null) {
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key || (key != null && key.equals(k))))
                        return e;
                } while ((e = e.next) != null);
            }
        }
        return null;
    }

    final HashExpireMap.Node<K, V, T> removeNode(int hash, Object key, Object value,
                                                 boolean matchValue, boolean movable) {
        HashExpireMap.Node<K, V, T>[] tab;
        HashExpireMap.Node<K, V, T> p;
        int n, index;
        if ((tab = table) != null && (n = tab.length) > 0 &&
                (p = tab[index = (n - 1) & hash]) != null) {
            HashExpireMap.Node<K, V, T> node = null, e;
            K k;
            V v;
            if (p.hash == hash &&
                    ((k = p.key) == key || (key != null && key.equals(k))))
                node = p;
            else if ((e = p.next) != null) {
                do {
                    if (e.hash == hash &&
                            ((k = e.key) == key ||
                                    (key != null && key.equals(k)))) {
                        node = e;
                        break;
                    }
                    p = e;
                } while ((e = e.next) != null);
            }
            if (node != null && (!matchValue || (v = node.value) == value ||
                    (value != null && value.equals(v)))) {
                if (node == p)
                    tab[index] = node.next;
                else
                    p.next = node.next;
                ++modCount;
                --size;
                return node;
            }
        }
        return null;
    }

    public Set<HashExpireMap.Node<K,V,T>> entrySet() {
        Set<HashExpireMap.Node<K,V,T>> es;
        return (es = entrySet) == null ? (entrySet = new HashExpireMap.EntrySet()) : es;
    }

    final class EntrySet extends AbstractSet<HashExpireMap.Node<K,V,T>> {
        @Override
        public final int size()                 { return size; }
        @Override
        public final void clear()               { HashExpireMap.this.clear(); }
        @Override
        public final Iterator<HashExpireMap.Node<K,V,T>> iterator() {
            return new HashExpireMap.EntryIterator();
        }
        @Override
        public final boolean contains(Object o) {
            if (!(o instanceof Map.Entry))
                return false;
            Map.Entry<?,?> e = (Map.Entry<?,?>) o;
            Object key = e.getKey();
            HashExpireMap.Node<K,V,T> candidate = getNode(hash(key), key);
            return candidate != null && candidate.equals(e);
        }
        @Override
        public final boolean remove(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>) o;
                Object key = e.getKey();
                Object value = e.getValue();
                return removeNode(hash(key), key, value, true, true) != null;
            }
            return false;
        }
        @Override
        public final Spliterator<HashExpireMap.Node<K,V,T>> spliterator() {
            return new HashExpireMap.EntrySpliterator(HashExpireMap.this, 0, -1, 0, 0);
        }
        @Override
        public final void forEach(Consumer<? super HashExpireMap.Node<K,V,T>> action) {
            HashExpireMap.Node<K,V,T>[] tab;
            if (action == null)
                throw new NullPointerException();
            if (size > 0 && (tab = table) != null) {
                int mc = modCount;
                for (int i = 0; i < tab.length; ++i) {
                    for (HashExpireMap.Node<K,V, T> e = tab[i]; e != null; e = e.next)
                        action.accept(e);
                }
                if (modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }
    }

    final HashExpireMap.Node<K, V, T>[] resize() {
        HashExpireMap.Node<K, V, T>[] oldTab = table;
        int oldCap = (oldTab == null) ? 0 : oldTab.length;
        int oldThr = threshold;
        int newCap, newThr = 0;
        if (oldCap > 0) {
            if (oldCap >= MAXIMUM_CAPACITY) {
                threshold = Integer.MAX_VALUE;
                return oldTab;
            } else if ((newCap = oldCap << 1) < MAXIMUM_CAPACITY &&
                    oldCap >= DEFAULT_INITIAL_CAPACITY)
                newThr = oldThr << 1; // double threshold
        } else if (oldThr > 0) // initial capacity was placed in threshold
            newCap = oldThr;
        else {               // zero initial threshold signifies using defaults
            newCap = DEFAULT_INITIAL_CAPACITY;
            newThr = (int) (DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);
        }
        if (newThr == 0) {
            float ft = (float) newCap * loadFactor;
            newThr = (newCap < MAXIMUM_CAPACITY && ft < (float) MAXIMUM_CAPACITY ?
                    (int) ft : Integer.MAX_VALUE);
        }
        threshold = newThr;
        @SuppressWarnings({"rawtypes", "unchecked"})
        HashExpireMap.Node<K, V, T>[] newTab = (HashExpireMap.Node<K, V, T>[]) new HashExpireMap.Node[newCap];
        table = newTab;
        if (oldTab != null) {
            for (int j = 0; j < oldCap; ++j) {
                HashExpireMap.Node<K, V, T> e;
                if ((e = oldTab[j]) != null) {
                    oldTab[j] = null;
                    if (e.next == null)
                        newTab[e.hash & (newCap - 1)] = e;
                    else { // preserve order
                        HashExpireMap.Node<K, V, T> loHead = null, loTail = null;
                        HashExpireMap.Node<K, V, T> hiHead = null, hiTail = null;
                        HashExpireMap.Node<K, V, T> next;
                        do {
                            next = e.next;
                            if ((e.hash & oldCap) == 0) {
                                if (loTail == null)
                                    loHead = e;
                                else
                                    loTail.next = e;
                                loTail = e;
                            } else {
                                if (hiTail == null)
                                    hiHead = e;
                                else
                                    hiTail.next = e;
                                hiTail = e;
                            }
                        } while ((e = next) != null);
                        if (loTail != null) {
                            loTail.next = null;
                            newTab[j] = loHead;
                        }
                        if (hiTail != null) {
                            hiTail.next = null;
                            newTab[j + oldCap] = hiHead;
                        }
                    }
                }
            }
        }
        return newTab;
    }

    /**
     * Removes all of the mappings from this map.
     * The map will be empty after this call returns.
     */
    public void clear() {
        HashExpireMap.Node<K, V, T>[] tab;
        modCount++;
        if ((tab = table) != null && size > 0) {
            size = 0;
            for (int i = 0; i < tab.length; ++i)
                tab[i] = null;
        }
    }

    abstract class HashIterator {
        Node<K,V,T> next;        // next entry to return
        Node<K,V,T> current;     // current entry
        int expectedModCount;  // for fast-fail
        int index;             // current slot

        HashIterator() {
            expectedModCount = modCount;
            Node<K,V,T>[] t = table;
            current = next = null;
            index = 0;
            if (t != null && size > 0) { // advance to first entry
                do {} while (index < t.length && (next = t[index++]) == null);
            }
        }

        public final boolean hasNext() {
            return next != null;
        }

        final Node<K,V,T> nextNode() {
            Node<K,V,T>[] t;
            Node<K,V,T> e = next;
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            if (e == null)
                throw new NoSuchElementException();
            if ((next = (current = e).next) == null && (t = table) != null) {
                do {} while (index < t.length && (next = t[index++]) == null);
            }
            return e;
        }

        public final void remove() {
            Node<K,V,T> p = current;
            if (p == null)
                throw new IllegalStateException();
            if (modCount != expectedModCount)
                throw new ConcurrentModificationException();
            current = null;
            K key = p.key;
            removeNode(hash(key), key, null, false, false);
            expectedModCount = modCount;
        }
    }

    final class EntryIterator extends HashExpireMap.HashIterator
            implements Iterator<HashExpireMap.Node<K,V,T>> {
        @Override
        public final HashExpireMap.Node<K,V,T> next() { return nextNode(); }
    }

    static class ExpireTimeMapSpliterator<K,V,T> {
        final HashExpireMap<K,V,Long> map;
        Node<K,V,Long> current;          // current node
        int index;                  // current index, modified on advance/split
        int fence;                  // one past last index
        int est;                    // size estimate
        int expectedModCount;       // for comodification checks

        ExpireTimeMapSpliterator(HashExpireMap<K,V,Long> m, int origin,
                                 int fence, int est,
                                 int expectedModCount) {
            this.map = m;
            this.index = origin;
            this.fence = fence;
            this.est = est;
            this.expectedModCount = expectedModCount;
        }

        final int getFence() { // initialize fence and size on first use
            int hi;
            if ((hi = fence) < 0) {
                HashExpireMap<K,V,Long> m = map;
                est = m.size;
                expectedModCount = m.modCount;
                Node<K,V,Long>[] tab = m.table;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            return hi;
        }

        public final long estimateSize() {
            getFence(); // force init
            return (long) est;
        }
    }

    static final class EntrySpliterator<K,V,T extends Long>
            extends ExpireTimeMapSpliterator<K,V,Long>
            implements Spliterator<HashExpireMap.Node<K,V,Long>> {
        EntrySpliterator(HashExpireMap<K,V, Long> m, int origin, int fence, int est,
                         int expectedModCount) {
            super(m, origin, fence, est, expectedModCount);
        }

        @Override
        public EntrySpliterator<K,V,T> trySplit() {
            int hi = getFence(), lo = index, mid = (lo + hi) >>> 1;
            return (lo >= mid || current != null) ? null :
                    new EntrySpliterator<>(map, lo, index = mid, est >>>= 1,
                            expectedModCount);
        }

        @Override
        public void forEachRemaining(Consumer<? super HashExpireMap.Node<K,V,Long>> action) {
            int i, hi, mc;
            if (action == null)
                throw new NullPointerException();
            HashExpireMap<K,V,Long> m = map;
            Node<K,V,Long>[] tab = m.table;
            if ((hi = fence) < 0) {
                mc = expectedModCount = m.modCount;
                hi = fence = (tab == null) ? 0 : tab.length;
            }
            else
                mc = expectedModCount;
            if (tab != null && tab.length >= hi &&
                    (i = index) >= 0 && (i < (index = hi) || current != null)) {
                Node<K,V,Long> p = current;
                current = null;
                do {
                    if (p == null)
                        p = tab[i++];
                    else {
                        action.accept(p);
                        p = p.next;
                    }
                } while (p != null || i < hi);
                if (m.modCount != mc)
                    throw new ConcurrentModificationException();
            }
        }

        @Override
        public boolean tryAdvance(Consumer<? super HashExpireMap.Node<K,V,Long>> action) {
            int hi;
            if (action == null)
                throw new NullPointerException();
            Node<K,V,Long>[] tab = map.table;
            if (tab != null && tab.length >= (hi = getFence()) && index >= 0) {
                while (current != null || index < hi) {
                    if (current == null)
                        current = tab[index++];
                    else {
                        Node<K,V,Long> e = current;
                        current = current.next;
                        action.accept(e);
                        if (map.modCount != expectedModCount)
                            throw new ConcurrentModificationException();
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public int characteristics() {
            return (fence < 0 || est == map.size ? Spliterator.SIZED : 0) |
                    Spliterator.DISTINCT;
        }
    }

    /**
     *
     * @param strTime
     * @return
     */
    final T setExpireTime(String strTime) {
        char timeType = strTime.charAt(strTime.length() - 1);
        Long timeNumer = Long.valueOf(strTime.substring(0, strTime.length() - 1));
        if (timeType == 's' || timeType == 'S') {
            return (T) new Long(System.currentTimeMillis() + timeNumer * 1000);
        } else if (timeType == 'm' || timeType == 'M') {
            return (T) new Long(System.currentTimeMillis() + timeNumer * 60 * 1000);
        } else if (timeType == 'h' || timeType == 'H') {
            return (T) new Long(System.currentTimeMillis() * timeNumer * 60 * 60 * 1000);
        }
        throw new RuntimeException("当前仅支持秒(s|S)、分(m|M)、时(h|H)");
    }

    class ScanThread implements Runnable {

        private boolean isExpire(K k, T t) {
            if ((Long)t < System.currentTimeMillis()) {
                setChanged();
                notifyObservers(k);
                return true;
            }
            return false;
        }

        @Override
        public void run() {
            for (Iterator<HashExpireMap.Node<K, V, T>> iterator = entrySet().iterator(); iterator.hasNext(); iterator.next()) {
                HashExpireMap.Node<K,V,T> node = iterator.next();
                if (isExpire(node.key, node.time)) {
                    iterator.remove();
                }
            }
        }

    }

}
