package com.darkyen.tproll.util;

import com.darkyen.tproll.util.prettyprint.PrettyPrinterFileModule;
import com.darkyen.tproll.util.prettyprint.PrettyPrinterPathModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.util.*;

/**
 * Utility class for safe and human readable printing of objects.
 *
 * Thread safe.
 */
@SuppressWarnings({"unused", "rawtypes", "unchecked"})
public final class PrettyPrinter {

    private static final Logger LOG = LoggerFactory.getLogger("Tproll-PrettyPrinter");

    private static final char[] HEX_NUMBERS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    public static void appendByteHex(@NotNull StringBuilder sb, byte b){
        final char[] HEX_NUMBERS = PrettyPrinter.HEX_NUMBERS;
        sb.append(HEX_NUMBERS[(b >> 4) & 0xF]).append(HEX_NUMBERS[b & 0xF]);
    }

    public static void append(@NotNull StringBuilder sb, @Nullable Object item){
        append(sb, item, Integer.MAX_VALUE);
    }

    /** Pretty printing of arrays is using some reflection, which may fail under some unusual conditions.
     * Failure is costly, so when it happens, it turns array pretty printing off using this switch. */
    private static boolean prettyPrintArrays = true;

    public enum PrettyPrintMode {
        /**
         * Pretty print, iterate with iterator.
         */
        YES,
        /**
         * Pretty print, iterate with forEach. (Somewhat slower maxCollectionElements support)
         */
        YES_SYNCHRONIZED,
        /**
         * Pretty print, cast to List and iterate with get(int).
         * (Does not apply to maps!)
         */
        YES_RANDOM,
        /**
         * Do not pretty print
         */
        NO,
    }

    private static final @NotNull Map<@NotNull Class<? extends Collection>, @NotNull PrettyPrintMode> PRETTY_PRINT_COLLECTIONS = Collections.synchronizedMap(new HashMap<>());
    static {
        PRETTY_PRINT_COLLECTIONS.put(AbstractCollection.class, PrettyPrintMode.YES);
        PRETTY_PRINT_COLLECTIONS.put(AbstractList.class, PrettyPrintMode.YES);
        PRETTY_PRINT_COLLECTIONS.put(ArrayList.class, PrettyPrintMode.YES_RANDOM);
        PRETTY_PRINT_COLLECTIONS.put(LinkedList.class, PrettyPrintMode.YES);
        PRETTY_PRINT_COLLECTIONS.put(AbstractSet.class, PrettyPrintMode.YES);
        PRETTY_PRINT_COLLECTIONS.put(HashSet.class, PrettyPrintMode.YES);
        PRETTY_PRINT_COLLECTIONS.put(TreeSet.class, PrettyPrintMode.YES);

        PRETTY_PRINT_COLLECTIONS.put(Collections.synchronizedCollection(Collections.emptyList()).getClass(), PrettyPrintMode.YES_SYNCHRONIZED);
        PRETTY_PRINT_COLLECTIONS.put(Collections.synchronizedList(Collections.emptyList()).getClass(), PrettyPrintMode.YES_SYNCHRONIZED);
        PRETTY_PRINT_COLLECTIONS.put(Collections.synchronizedSet(Collections.emptySet()).getClass(), PrettyPrintMode.YES_SYNCHRONIZED);
        try {
            PRETTY_PRINT_COLLECTIONS.put(Collections.synchronizedNavigableSet(Collections.emptyNavigableSet()).getClass(), PrettyPrintMode.YES_SYNCHRONIZED);
        } catch (Throwable ignored) {}
    }

    public static @Nullable PrettyPrintMode setPrettyPrintModeForCollection(@NotNull Class<? extends Collection> type, @Nullable PrettyPrintMode mode) {
        assert Collection.class.isAssignableFrom(type);
        if (mode == null) {
            return PRETTY_PRINT_COLLECTIONS.remove(type);
        } else {
            return PRETTY_PRINT_COLLECTIONS.put(type, mode);
        }
    }

    public static @NotNull PrettyPrintMode getPrettyPrintModeForCollection(@NotNull Class<? extends Collection> type) {
        assert Collection.class.isAssignableFrom(type);

        PrettyPrintMode mode = PRETTY_PRINT_COLLECTIONS.get(type);
        if (mode != null) {
            return mode;
        }

        try {
            final Class toStringDeclaringClass = type.getMethod("toString").getDeclaringClass();
            mode = PRETTY_PRINT_COLLECTIONS.get(toStringDeclaringClass);
            if (mode == null) {
                mode = PrettyPrintMode.NO;
            } else if (mode == PrettyPrintMode.YES
                    && RandomAccess.class.isAssignableFrom(type)
                    && List.class.isAssignableFrom(type)) {
                mode = PrettyPrintMode.YES_RANDOM;
            }
        } catch (Exception e) {
            LOG.warn("Failed to determine pretty-print mode for class {}, defaulting to NO", type, e);
            mode = PrettyPrintMode.NO;
        }

        PRETTY_PRINT_COLLECTIONS.put(type, mode);
        return mode;
    }

    private static final @NotNull Map<@NotNull Class<? extends Map>, @NotNull PrettyPrintMode> PRETTY_PRINT_MAPS = Collections.synchronizedMap(new HashMap<>());
    static {
        PRETTY_PRINT_MAPS.put(AbstractMap.class, PrettyPrintMode.YES);
        PRETTY_PRINT_MAPS.put(HashMap.class, PrettyPrintMode.YES);
        PRETTY_PRINT_MAPS.put(TreeMap.class, PrettyPrintMode.YES);

        PRETTY_PRINT_MAPS.put(Collections.synchronizedMap(Collections.emptyMap()).getClass(), PrettyPrintMode.YES_SYNCHRONIZED);
        try {
            PRETTY_PRINT_MAPS.put(Collections.synchronizedNavigableMap(Collections.emptyNavigableMap()).getClass(), PrettyPrintMode.YES_SYNCHRONIZED);
        } catch (Throwable ignored) {}
    }

    public static @Nullable PrettyPrintMode setPrettyPrintModeForMap(@Nullable Class<? extends Map> type, @Nullable PrettyPrintMode mode) {
        assert type != null;
        assert Map.class.isAssignableFrom(type);

        if (mode == null) {
            return PRETTY_PRINT_MAPS.remove(type);
        } else {
            if (mode == PrettyPrintMode.YES_RANDOM) {
                LOG.warn("YES_RANDOM is not applicable to Maps, defaulting to YES");
                mode = PrettyPrintMode.YES;
            }
            return PRETTY_PRINT_MAPS.put(type, mode);
        }
    }

    public static @NotNull PrettyPrintMode getPrettyPrintModeForMap(@NotNull Class<? extends Map> type) {
        assert Map.class.isAssignableFrom(type);

        PrettyPrintMode mode = PRETTY_PRINT_MAPS.get(type);
        if (mode != null) {
            return mode;
        }

        try {
            final Class toStringDeclaringClass = type.getMethod("toString").getDeclaringClass();
            mode = PRETTY_PRINT_MAPS.get(toStringDeclaringClass);
            if (mode == null) {
                mode = PrettyPrintMode.NO;
            }
        } catch (Exception e) {
            LOG.warn("Failed to determine pretty-print mode for class {}, defaulting to NO", type, e);
            mode = PrettyPrintMode.NO;
        }

        PRETTY_PRINT_MAPS.put(type, mode);
        return mode;
    }

    /**
     * Additional modules with application specific pretty printing logic.
     * Modules will be tried after all basic built-in matchers were tried and did not match,
     * but before collection and toString logic.
     *
     * As the collection is provided as is, without any null checks or synchronization,
     * it is advised to modify it only at the start of your program, from single thread.
     *
     * Nulls are not permitted.
     */
    public static final @NotNull ArrayList<@NotNull PrettyPrinterModule> PRETTY_PRINT_MODULES = new ArrayList<>();

    private static <E> int appendCollection(@NotNull StringBuilder sb, @NotNull Collection<E> collection, int maxCollectionElements) {
        int written = 0;
        for (E element : collection) {
            if (written == maxCollectionElements) {
                return collection.size() - written;
            }

            if (written != 0) {
                sb.append(',').append(' ');
            }
            if (element == collection) {
                sb.append("(this Collection)");
            } else {
                append(sb, element, maxCollectionElements);
            }
            written++;
        }
        return 0;
    }

    private static <E> int appendCollectionRandom(@NotNull StringBuilder sb, @NotNull List<E> collection, int maxCollectionElements) {
        final int collectionSize = collection.size();
        if (collectionSize <= 0) {
            return 0;
        }

        append(sb, collection.get(0), maxCollectionElements);
        for (int i = 1; i < collectionSize; i++) {
            if (i == maxCollectionElements) {
                return collectionSize - i;
            }

            sb.append(',').append(' ');
            final E element = collection.get(i);
            if (element == collection) {
                sb.append("(this Collection)");
            } else {
                append(sb, element, maxCollectionElements);
            }
        }
        return 0;
    }

    private static <E> int appendCollectionSynchronized(@NotNull StringBuilder sb, @NotNull Collection<E> collection, int maxCollectionElements) {
        final int[] writtenAndRemaining = {0, 0};
        for (E element : collection) {
            if (writtenAndRemaining[0] == maxCollectionElements) {
                writtenAndRemaining[1]++;
            } else {
                if (writtenAndRemaining[0] != 0) {
                    sb.append(',').append(' ');
                }
                if (element == collection) {
                    sb.append("(this Collection)");
                } else {
                    append(sb, element, maxCollectionElements);
                }
                writtenAndRemaining[0]++;
            }
        }
        return writtenAndRemaining[1];
    }

    private static <K, V> int appendMap(@NotNull StringBuilder sb, @NotNull Map<K, V> map, int maxCollectionElements) {
        int written = 0;
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (written == maxCollectionElements) {
                return map.size() - written;
            }

            if (written != 0) {
                sb.append(',').append(' ');
            }
            final K key = entry.getKey();
            final V value = entry.getValue();
            if (key == map) {
                sb.append("(this Map)");
            } else {
                append(sb, key, maxCollectionElements);
            }
            sb.append('=');
            if (value == map) {
                sb.append("(this Map)");
            } else {
                append(sb, value, maxCollectionElements);
            }

            written++;
        }
        return 0;
    }

    private static <K, V> int appendMapSynchronized(@NotNull StringBuilder sb, @NotNull Map<K, V> map, int maxCollectionElements) {
        final int[] writtenAndRemaining = {0, 0};
        for (Map.Entry<K, V> entry : map.entrySet()) {
            final K key = entry.getKey();
            final V value = entry.getValue();

            if (writtenAndRemaining[0] == maxCollectionElements) {
                writtenAndRemaining[1]++;
            } else {
                if (writtenAndRemaining[0] != 0) {
                    sb.append(", ");
                }
                if (key == map) {
                    sb.append("(this Map)");
                } else {
                    append(sb, key, maxCollectionElements);
                }
                sb.append('=');
                if (value == map) {
                    sb.append("(this Map)");
                } else {
                    append(sb, value, maxCollectionElements);
                }
                writtenAndRemaining[0]++;
            }
        }
        return writtenAndRemaining[1];
    }

    /**
     * Appends given item into the sb.
     *
     * Primitives and null is appended unchanged.
     *
     * Files are printed as normalized path, with " ⌫" suffix when not-exists,
     * or with / suffix when directory, and/or with " ⇥" suffix when broken link,
     * or " → "+path when points to a different file after symlink resolution.
     *
     * Arrays and collections are printed in a pretty-print format, unless specified otherwise.
     *
     * Example of pretty-printed array: int[4, 6, 3, ... (15 more)] (when maxCollectionElements is 3).
     * byte[] arrays are printed in hex, without delimiters: byte[FFC473423456].
     *
     * Example of pretty-printed ArrayList of HashMaps: ArrayList[HashMap{1=2, 3=4}, HashMap{foo=bar, baz=4, ... (2 more)}].
     * (when maxCollectionElements is 2 - this cascades to nested pretty-printed collections).
     *
     * Example of maxCollectionElements = 0: Boolean[23 elements]
     *
     * @param sb to append to
     * @param item to append
     * @param maxCollectionElements to print, when printing arrays or pretty-printed collections.
     *                              0 means print only size, negative means ignore
     */
    public static void append(@NotNull StringBuilder sb, @Nullable Object item, int maxCollectionElements) {
        //To use faster/low-garbage overloads
        if (item == null) {
            sb.append((String) null);
            return;
        }
        if (item instanceof Boolean) {
            sb.append(((Boolean) item).booleanValue());
            return;
        }
        if (item instanceof Character) {
            sb.append(((Character) item).charValue());
            return;
        }
        if (item instanceof Long) {
            sb.append(((Long) item).longValue());
            return;
        }
        if (item instanceof Float) {
            sb.append(((Float) item).floatValue());
            return;
        }
        if (item instanceof Double) {
            sb.append(((Double) item).doubleValue());
            return;
        }
        if (item instanceof Integer || item instanceof Short || item instanceof Byte) {
            sb.append(((Number) item).intValue());
            return;
        }

        if (maxCollectionElements < 0) {
            maxCollectionElements = Integer.MAX_VALUE;
        }

        if (prettyPrintArrays) {
            try {
                if (item.getClass().isArray()) {
                    //It is an array, print it nicely

                    //Append element type
                    final Class<?> arrayType = item.getClass().getComponentType();
                    sb.append(arrayType.getSimpleName());

                    final int length = Array.getLength(item);
                    if (maxCollectionElements == 0) {
                        sb.append('[').append(length);
                        if (length == 1) {
                            sb.append(" element]");
                        } else {
                            sb.append(" elements]");
                        }
                    } else if (length <= 0) {
                        sb.append("[]");
                    } else {
                        final int printLength = Math.min(length, maxCollectionElements);

                        sb.append('[');
                        if (arrayType.equals(Byte.class) || arrayType.equals(byte.class)) {
                            //Byte arrays are logged in hex without delimiter
                            for (int i = 0; i < printLength; i++) {
                                appendByteHex(sb, Array.getByte(item, i));
                            }
                            if (printLength < length) {
                                sb.append(" ... (").append(length - printLength).append(" more)");
                            }
                        } else {
                            append(sb, Array.get(item, 0));
                            for (int i = 1; i < printLength; i++) {
                                sb.append(", ");
                                append(sb, Array.get(item, i));
                            }
                            if (printLength < length) {
                                sb.append(", ... (").append(length - printLength).append(" more)");
                            }
                        }

                        sb.append(']');
                    }

                    return;
                }//else Not an array, fallback to standard printing
            } catch (Exception ex) {
                prettyPrintArrays = false;
                LOG.warn("Failed to pretty-print array: ", ex);
            }
        }

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < PRETTY_PRINT_MODULES.size(); i++) {
            final PrettyPrinterModule module = PRETTY_PRINT_MODULES.get(i);
            if (module.accepts(item)) {
                module.append(sb, item, maxCollectionElements);
                return;
            }
        }

        prettyPrintCollections:
        if (item instanceof Collection) {
            //noinspection unchecked
            final Class<? extends Collection> collectionClass = (Class<? extends Collection>) item.getClass();
            final PrettyPrintMode collectionPrettyPrintMode = getPrettyPrintModeForCollection(collectionClass);
            if (collectionPrettyPrintMode == PrettyPrintMode.NO) {
                break prettyPrintCollections;
            }

            final int originalSbLength = sb.length();
            final Collection collection = (Collection) item;
            sb.append(collectionClass.getSimpleName()).append('[');

            final int postHeaderSbLength = sb.length();
            try {
                final int size = collection.size();
                final int moreElements;
                if (maxCollectionElements == 0) {
                    sb.append(size);
                    if (size == 1) {
                        sb.append(" element]");
                    } else {
                        sb.append(" elements]");
                    }
                    return;
                } else if (collectionPrettyPrintMode == PrettyPrintMode.YES_RANDOM) {
                    moreElements = appendCollectionRandom(sb, (List)collection, maxCollectionElements);
                } else if (collectionPrettyPrintMode == PrettyPrintMode.YES_SYNCHRONIZED) {
                    moreElements = appendCollectionSynchronized(sb, collection, maxCollectionElements);
                } else {
                    if (collectionPrettyPrintMode != PrettyPrintMode.YES) {
                        LOG.error("Unexpected PrettyPrintMode: {}", collectionPrettyPrintMode);
                    }
                    moreElements = appendCollection(sb, collection, maxCollectionElements);
                }

                if (moreElements > 0) {
                    sb.append(", ... (").append(moreElements).append(" more)");
                }
                sb.append(']');
                return;
            } catch (StackOverflowError ex) {
                sb.setLength(postHeaderSbLength);
                sb.append("<<<StackOverflow>>>]");
                LOG.warn("StackOverflow while pretty-printing a collection. Self containing collection?");
                return;
            } catch (Exception ex) {
                sb.setLength(originalSbLength);
                LOG.error("Exception while pretty-printing a collection.", ex);
            }
        }

        prettyPrintMaps:
        if (item instanceof Map) {
            //noinspection unchecked
            final Class<? extends Map> collectionClass = (Class<? extends Map>) item.getClass();
            final PrettyPrintMode mapPrettyPrintMode = getPrettyPrintModeForMap(collectionClass);
            if (mapPrettyPrintMode == PrettyPrintMode.NO) {
                break prettyPrintMaps;
            }

            final int originalSbLength = sb.length();
            final Map map = (Map) item;
            sb.append(collectionClass.getSimpleName()).append('{');

            final int postHeaderSbLength = sb.length();
            try {
                final int size = map.size();
                final int moreElements;
                if (maxCollectionElements == 0) {
                    sb.append(size);
                    if (size == 1) {
                        sb.append(" entry}");
                    } else {
                        sb.append(" entries}");
                    }
                    return;
                } else if (mapPrettyPrintMode == PrettyPrintMode.YES_SYNCHRONIZED) {
                    moreElements = appendMapSynchronized(sb, map, maxCollectionElements);
                } else {
                    if (mapPrettyPrintMode != PrettyPrintMode.YES) {
                        LOG.error("Unexpected PrettyPrintMode: {}", mapPrettyPrintMode);
                    }
                    moreElements = appendMap(sb, map, maxCollectionElements);
                }

                if (moreElements > 0) {
                    sb.append(", ... (").append(moreElements).append(" more)");
                }
                sb.append('}');
                return;
            } catch (StackOverflowError ex) {
                sb.setLength(postHeaderSbLength);
                sb.append("<<<StackOverflow>>>]");
                LOG.warn("StackOverflow while pretty-printing a map. Self containing map?");
                return;
            } catch (Exception ex) {
                sb.setLength(originalSbLength);
                LOG.error("Exception while pretty-printing a map.", ex);
            }
        }

        String itemString;
        try {
            itemString = item.toString();
        } catch (Exception ex) {
            LOG.error("Failed to convert object to string", ex);
            itemString = "<toString() failed>";
        }
        sb.append(itemString);
    }

    public static @NotNull String toString(@Nullable Object object){
        final StringBuilder result = new StringBuilder();
        append(result, object);
        return result.toString();
    }

    public static @NotNull String toString(@Nullable Object object, int maxCollectionElements){
        final StringBuilder result = new StringBuilder();
        append(result, object, maxCollectionElements);
        return result.toString();
    }

    private static final @NotNull ThreadLocal<@NotNull StringBuilderWriter> sbwCache = new ThreadLocal<StringBuilderWriter>() {
        @Override
        protected StringBuilderWriter initialValue() {
            return new StringBuilderWriter();
        }
    };

    /**
     * Substitutes given objects into the template, one by one, on places where "{}" characters are.
     * @param out to which the result is appended
     * @param template which is appended into out with {} substituted
     * @param objects to substitute into template
     */
    public static void patternSubstituteInto(@NotNull StringBuilder out, @NotNull CharSequence template, @NotNull List<Object> objects) {
        if (objects.isEmpty()) {
            out.append(template);
        } else {
            boolean escaping = false;
            boolean substituting = false;
            int substitutingIndex = 0;
            Throwable throwable = null;

            for (int i = 0, l = template.length(); i < l; i++) {
                final char c = template.charAt(i);
                if (substituting) {
                    substituting = false;
                    if (c == '}') {
                        if (substitutingIndex != objects.size()) {
                            final Object item = objects.get(substitutingIndex);
                            if (item instanceof Throwable) {
                                throwable = (Throwable) item;
                            }
                            append(out, item);
                            substitutingIndex++;
                        } else {
                            out.append("{}");
                        }
                        continue;
                    } else {
                        out.append('{');
                    }
                }

                if (c == '\\') {
                    if (escaping) {
                        out.append('\\');
                    } else {
                        escaping = true;
                    }
                } else if (c == '{') {
                    if (escaping) {
                        escaping = false;
                        out.append('{');
                    } else {
                        substituting = true;
                    }
                } else {
                    out.append(c);
                }
            }
            //There are items that were not appended yet, because they have no {}
            //It could be just one throwable, in that case do not substitute it in
            if(substitutingIndex == objects.size() - 1 && objects.get(substitutingIndex) instanceof Throwable){
                throwable = (Throwable) objects.get(substitutingIndex);
            } else if (substitutingIndex < objects.size()) {
                //It is not one throwable. It could be more things ended with throwable though
                out.append(" {");
                do{
                    final Object item = objects.get(substitutingIndex);
                    append:{
                        if (item instanceof Throwable) {
                            throwable = (Throwable) item;
                            if(substitutingIndex == objects.size() - 1) {
                                //When throwable is last in list and not in info string, don't print it.
                                //It is guaranteed that it will be printed by trace.
                                break append;
                            }
                        }
                        append(out, item);
                    }
                    substitutingIndex++;

                    out.append(", ");
                }while(substitutingIndex < objects.size());
                out.setLength(out.length() - 2);
                out.append('}');
            }

            //Append throwable if any
            if (throwable != null) {
                StringBuilderWriter sbw = sbwCache.get();
                sbw.setStringBuilder(out);

                out.append('\n');
                throwable.printStackTrace(sbw);
                //Strip \n at the end
                if (out.charAt(out.length() - 1) == '\n') {
                    out.setLength(out.length()-1);
                }
            }
        }
    }

    /**
     * Extra logic for PrettyPrinter, for custom application specific classes.
     * @see #PRETTY_PRINT_MODULES
     */
    public interface PrettyPrinterModule {

        /** Check if this module will print given object.
         * @param item to check, not null */
        boolean accepts(@NotNull Object item);

        /**
         * Print given item to the string builder.
         * May use {@link PrettyPrinter#append(StringBuilder, Object)} in its implementation.
         * Must not throw any exceptions.
         *
         * @param sb to append the pretty-printed representation to
         * @param item to be pretty-printed. Not null. Called only after {@link #accepts(Object)} returned true for this instance.
         * @param maxCollectionElements >=0, if item is a collection, it is advisable to print only this many elements
         */
        void append(@NotNull StringBuilder sb, @NotNull Object item, int maxCollectionElements);
    }

    static {
        PRETTY_PRINT_MODULES.add(new PrettyPrinterFileModule());

        // Try to load modules for newer Java than 1.6 or on Android (maybe?)
        try {
            PRETTY_PRINT_MODULES.add(new PrettyPrinterPathModule());
        } catch (Throwable ignored) {}
    }
}
