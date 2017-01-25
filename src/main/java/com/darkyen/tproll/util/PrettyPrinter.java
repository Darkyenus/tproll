package com.darkyen.tproll.util;

/**
 * Utility class for human readable printing of objects.
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

    public static void append(StringBuilder sb, Object item, int maxArrayElements){
        if (item == null) {
            sb.append("null");
        } else if (item instanceof Integer) {//Prepared for more efficient SB impl
            sb.append(((Integer) item).intValue());
        } else if (item instanceof Float) {
            sb.append(((Float) item).floatValue());
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

                            final int length = java.lang.reflect.Array.getLength(item);
                            if(length == 0){
                                sb.append("[]");
                            }else{
                                final int printLength = length > maxArrayElements ? maxArrayElements : length;

                                sb.append('[');
                                if(arrayType.equals(Byte.class) || arrayType.equals(byte.class)){
                                    //Byte arrays are logged in hex without delimiter
                                    appendByteHex(sb, java.lang.reflect.Array.getByte(item, 0));
                                    for (int i = 1; i < printLength; i++) {
                                        appendByteHex(sb, java.lang.reflect.Array.getByte(item, i));
                                    }
                                    if(printLength < length){
                                        sb.append(" ... (").append(length - printLength).append(" more)");
                                    }
                                }else{
                                    append(sb, java.lang.reflect.Array.get(item, 0));
                                    for (int i = 1; i < printLength; i++) {
                                        sb.append(", ");
                                        append(sb, java.lang.reflect.Array.get(item, i));
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
}
