package com.darkyen.tproll.util;

import java.io.File;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.List;

/**
 * Utility class for safe and human readable printing of objects.
 *
 * Thread safe.
 */
public final class PrettyPrinter {

    private static final char[] HEX_NUMBERS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
    public static void appendByteHex(StringBuilder sb, byte b){
        final char[] HEX_NUMBERS = PrettyPrinter.HEX_NUMBERS;
        sb.append(HEX_NUMBERS[(b >> 4) & 0xF]).append(HEX_NUMBERS[b & 0xF]);
    }

    public static void append(StringBuilder sb, Object item){
        append(sb, item, Integer.MAX_VALUE);
    }

    /** Pretty printing of arrays is using some reflection, which may fail under some unusual conditions.
     * Failure is costly, so when it happens, it turns array pretty printing off using this switch. */
    private static boolean prettyPrintArrays = true;

    private static Path APPLICATION_ROOT_DIRECTORY = null;

    private static void append(StringBuilder sb, Path path) {
        path = path.normalize();

        final Path root = APPLICATION_ROOT_DIRECTORY;
        final Path shownPath;
        if (root != null && path.startsWith(root)) {
            shownPath = root.relativize(path);
        } else {
            shownPath = path;
        }

        String showPathString = shownPath.toString();
        if (showPathString.isEmpty()) {
            // For empty strings (that is, current directory) write .
            sb.append('.');
        } else {
            sb.append(showPathString);
        }

        // Not following links, if this returns true, the file is simply not there
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            // File exists!
            if (Files.isDirectory(path)) {
                // It is a directory, indicate that
                sb.append('/');
            }

            if (!Files.exists(path)) {
                // File does not exist when following links, therefore it is a broken link
                sb.append(" ⇥");
            } else {
                // Where does the file lead when following links?
                Path leadsToPath = null;
                try {
                    final Path realPath = path.toRealPath();
                    if (!path.equals(realPath)) {
                        leadsToPath = realPath;
                    }
                } catch (Throwable ignored) {}

                if (leadsToPath != null) {
                    sb.append(" → ").append(leadsToPath.toString());
                }
            }
        } else {
            // File does not exist
            sb.append(" ⌫");
        }
    }

    public static void append(StringBuilder sb, Object item, int maxArrayElements){
        //To use faster overloads
        if (item == null) {
            sb.append((String)null);
        } else if (item instanceof Boolean) {
            sb.append((boolean)item);
        } else if (item instanceof Character) {
            sb.append((char)item);
        } else if (item instanceof Long) {
            sb.append((long) item);
        } else if (item instanceof Float) {
            sb.append((float) item);
        } else if (item instanceof Double) {
            sb.append((double) item);
        } else if (item instanceof Integer || item instanceof Short || item instanceof Byte) {
            sb.append((int) item);
        } else if (item instanceof File) {
            append(sb, ((File) item).toPath());
        } else if (item instanceof Path) {
            append(sb, (Path) item);
        } else {
            //It can be an array or plain object, check it
            printStandard:
            {
                if (prettyPrintArrays) {
                    try {
                        if (item.getClass().isArray()) {
                            //It is an array, print it nicely

                            //Append element type
                            final Class<?> arrayType = item.getClass().getComponentType();
                            sb.append(arrayType.getSimpleName());

                            final int length = Array.getLength(item);
                            if(length == 0){
                                sb.append("[]");
                            }else{
                                final int printLength = length > maxArrayElements ? maxArrayElements : length;

                                sb.append('[');
                                if(arrayType.equals(Byte.class) || arrayType.equals(byte.class)){
                                    //Byte arrays are logged in hex without delimiter
                                    appendByteHex(sb, Array.getByte(item, 0));
                                    for (int i = 1; i < printLength; i++) {
                                        appendByteHex(sb, Array.getByte(item, i));
                                    }
                                    if(printLength < length){
                                        sb.append(" ... (").append(length - printLength).append(" more)");
                                    }
                                }else{
                                    append(sb, Array.get(item, 0));
                                    for (int i = 1; i < printLength; i++) {
                                        sb.append(", ");
                                        append(sb, Array.get(item, i));
                                    }
                                    if(printLength < length){
                                        sb.append(", ... (").append(length - printLength).append(" more)");
                                    }
                                }

                                sb.append(']');
                            }

                            break printStandard; //Skip standard printing
                        }//else Not an array, fallback to standard printing
                    } catch (Exception ex) {
                        sb.append("<Internal Error: Failed to pretty-print array: ").append(ex).append(">");
                        prettyPrintArrays = false;
                    }
                }

                String itemString;
                try {
                    itemString = item.toString();
                } catch (Exception ex) {
                    itemString = "(Failed to convert to string: " + ex + ")";
                }
                sb.append(itemString);
            }
        }
    }

    public static String toString(Object object, int maxArrayElements){
        final StringBuilder result = new StringBuilder();
        append(result, object, maxArrayElements);
        return result.toString();
    }

    private static final ThreadLocal<StringBuilderWriter> sbwCache = ThreadLocal.withInitial(StringBuilderWriter::new);

    /**
     * Substitutes given objects into the template, one by one, on places where "{}" characters are.
     * @param out to which the result is appended
     * @param template which is appended into out with {} substituted
     * @param objects to substitute into template
     */
    public static void patternSubstituteInto(StringBuilder out, CharSequence template, List<Object> objects) {
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
            objects.clear();

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
     * Applications with well specified root directory can put it here. All file paths under this directory will
     * be printed out by this class in relative form
     */
    public static Path getApplicationRootDirectory() {
        return APPLICATION_ROOT_DIRECTORY;
    }

    /**
     * @see #getApplicationRootDirectory()
     */
    public static void setApplicationRootDirectory(Path applicationRootDirectory) {
        if (applicationRootDirectory == null) {
            APPLICATION_ROOT_DIRECTORY = null;
            return;
        }
        APPLICATION_ROOT_DIRECTORY = applicationRootDirectory.normalize().toAbsolutePath();
    }

    /**
     * @see #getApplicationRootDirectory()
     */
    public static void setApplicationRootDirectory(File applicationRootDirectory) {
        if (applicationRootDirectory == null) {
            APPLICATION_ROOT_DIRECTORY = null;
            return;
        }
        setApplicationRootDirectory(applicationRootDirectory.toPath());
    }
}
