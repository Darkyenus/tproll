package com.darkyen.tproll.util;

import org.slf4j.Marker;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Simple marker implementation
 */
@SuppressWarnings("serialVersionUID")
public abstract class SimpleMarker implements Marker {

    private Marker[] references;

    public SimpleMarker() {
        references = NO_REFERENCES;
    }

    protected SimpleMarker(Marker[] references) {
        this.references = references;
    }

    @Override
    public synchronized void add(Marker reference) {
        if (reference == null) throw new NullPointerException("reference may not be null");
        if (reference == this) return;
        final Marker[] references = this.references;
        if (references == NO_REFERENCES) {
            this.references = new Marker[]{reference};
        } else if (!contains(reference)) {
            final int oldReferencesLen = references.length;
            final Marker[] newReferences = new Marker[oldReferencesLen + 1];
            System.arraycopy(references, 0, newReferences, 0, oldReferencesLen);
            newReferences[oldReferencesLen] = reference;
            this.references = newReferences;
        }
    }

    @Override
    public synchronized boolean remove(Marker reference) {
        final Marker[] references = this.references;
        if (reference == null || references.length == 0) {
            return false;
        } else {
            final int index = indexOf(references, reference);
            if (index < 0) return false;
            final int oldReferencesLen = references.length;
            final Marker[] newReferences = new Marker[oldReferencesLen - 1];
            System.arraycopy(references, 0, newReferences, 0, index);
            System.arraycopy(references, index + 1, newReferences, index, oldReferencesLen - index - 1);
            this.references = newReferences;
            return true;
        }
    }

    public void clear() {
        this.references = NO_REFERENCES;
    }

    @Override
    public boolean hasChildren() {
        return hasReferences();
    }

    @Override
    public boolean hasReferences() {
        return references.length > 0;
    }

    public Marker[] references() {
        return this.references;
    }

    @Override
    public Iterator<Marker> iterator() {
        final Marker[] references = this.references;
        if (references.length == 0) {
            return Collections.<Marker>emptyList().iterator();
        } else {
            return new Iterator<Marker>() {
                private int index = 0;
                
                @Override
                public boolean hasNext() {
                    return index < references.length;
                }

                @Override
                public Marker next() {
                    if (index >= references.length) throw new NoSuchElementException("iteration has ended");
                    return references[index++];
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    @Override
    public boolean contains(Marker other) {
        if (other == null) return false;
        if (other == this) return true;
        for (Marker marker : references) {
            if (marker.contains(other)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean contains(String name) {
        if (name == null) return false;
        if (name.equals(getName())) return true;
        for (Marker marker : references) {
            if (marker.contains(name)) return true;
        }
        return false;
    }

    public String toString() {
        final Marker[] references = this.references;
        if (references.length == 0) {
            return this.getName();
        }
        StringBuilder sb = new StringBuilder(this.getName());
        sb.append(' ').append('[');
        for (int i = 0;;) {
            sb.append(references[i].toString());
            i++;
            if (i < references.length) {
                sb.append(',').append(' ');
            } else {
                break;
            }
        }
        sb.append(']');
        return sb.toString();
    }

    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public static <T extends Marker> void findMarkersByType(Marker from, Class<T> type, Consumer<T> consumer) {
        if (from == null) return;
        if (type.isInstance(from)) {
            consumer.accept((T)from);
        }
        try {
            if (from instanceof SimpleMarker) {
                final Marker[] references = ((SimpleMarker) from).references;
                for (Marker reference : references) {
                    findMarkersByType(reference, type, consumer);
                }
            } else if (from.hasReferences()) {
                final Iterator<Marker> iterator = from.iterator();
                while (iterator.hasNext()) {
                    findMarkersByType(iterator.next(), type, consumer);
                }
            }
        } catch (StackOverflowError ex) {
            throw new IllegalArgumentException("Marker "+from.getName()+" (most likely) contains cycles", ex);
        }
    }


    private static final Marker[] NO_REFERENCES = {};

    private static int indexOf(Marker[] in, Marker of) {
        for (int i = 0; i < in.length; i++) {
            if (in[i] == of) return i;
        }
        return -1;
    }
}
