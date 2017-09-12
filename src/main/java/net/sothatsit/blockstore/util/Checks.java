package net.sothatsit.blockstore.util;

public class Checks {

    public static void ensureNonNull(Object argument, String argName) {
        ensureTrue(argument != null, argName + " cannot be null");
    }

    public static <T> void ensureArrayNonNull(T[] array, String arrayName) {
        ensureNonNull(array, arrayName);

        for(T element : array) {
            ensureTrue(element != null, arrayName + " cannot contain null values");
        }
    }

    public static void ensureTrue(boolean expression, String message) {
        if(!expression)
            throw new IllegalArgumentException(message);
    }

}
