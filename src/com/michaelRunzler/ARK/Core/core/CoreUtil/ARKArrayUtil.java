package core.CoreUtil;

import com.sun.istack.internal.NotNull;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;

/**
 * Provides a variety of utilities for array conversion and manipulation.
 */
public class ARKArrayUtil
{
    /**
     * Converts a character array to a byte array.
     * @param input the char[] to convert
     * @return a new byte[] of equivalent size to the input, containing the converted data
     */
    public static byte[] charToByteArray(char[] input)
    {
        if(input != null) {
            byte[] output = new byte[input.length];

            for(int i = 0; i < input.length; i++) {
                output[i] = (byte)input[i];
            }
            return output;
        }
        return null;
    }

    /**
     * Converts a byte array to a character array.
     * @param input the byte[] to convert
     * @return a new char[] of equivalent size to the input, containing the converted data
     */
    public static char[] byteToCharArray(byte[] input)
    {
        if(input != null){
            char[] output = new char[input.length];

            for(int i = 0; i < input.length; i++){
                output[i] = (char)input[i];
            }
            return output;
        }
        return null;
    }

    /**
     * Converts a character array to a String object.
     * @param input the char[] to convert
     * @return the characters in the input array, concatenated into a String
     */
    public static String charArrayToString(char[] input)
    {
        if(input != null){
            StringBuilder output = new StringBuilder();

            for(char c : input){
                output.append(c);
            }
            return output.toString();
        }
        return null;
    }

    /**
     * Searches for a byte[] inside of another byte[].
     * Sourced from user 'robermann' on Stack Overflow
     * @param input the byte[] to search
     * @param searchedFor the byte[] to look for
     * @return the index of the searched-for array inside of the searched array
     */
    public static int searchByteArray(byte[] input, byte[] searchedFor) {
        //convert byte[] to Byte[]
        Byte[] searchedForB = new Byte[searchedFor.length];
        for(int x = 0; x<searchedFor.length; x++){
            searchedForB[x] = searchedFor[x];
        }

        int idx = -1;

        //search:
        Deque<Byte> q = new ArrayDeque<>(input.length);
        for(int i=0; i<input.length; i++){
            if(q.size() == searchedForB.length){
                //here I can check
                Byte[] cur = q.toArray(new Byte[]{});
                if(Arrays.equals(cur, searchedForB)){
                    //found!
                    idx = i - searchedForB.length;
                    break;
                } else {
                    //not found
                    q.pop();
                    q.addLast(input[i]);
                }
            } else {
                q.addLast(input[i]);
            }
        }

        return idx;
    }

    /**
     * Searches a byte array for sequences using searchByteArray(). If both sequences are found, copies all data
     * between the end character of the first sequence (exclusive) and the beginning character of the second sequence
     * (exclusive). Returns the result as a new byte[].
     * @param input the byte[] to search (treated as read-only). If this is null, returns null
     * @param startSequence a byte[] to search for in the input array. If found, the end byte of this array will be
     *                      used as the exclusive start index for the copied data
     * @param endSequence a byte[] to search for in the input array. If found, the first byte of this array will be
     *                    used as the exclusive end index for the copied data
     * @return the data between the startSequence and endSequence in the input array, or a new byte[0] if both search
     * parameters are not found
     */
    public static byte[] searchByteArrayAndCopy(final byte[] input, byte[] startSequence, byte[] endSequence)
    {
        if(input != null) {
            int SIDX = searchByteArray(input, startSequence);
            int EIDX = searchByteArray(input, endSequence);
            if (SIDX > -1 && EIDX > -1) {
                SIDX += startSequence.length;
                byte[] output = new byte[EIDX - SIDX];
                System.arraycopy(input, SIDX, output, 0, output.length);
                return output;
            }
            return new byte[0];
        }
        return null;
    }

    /**
     * Gets the number of occurrences of a specified byte in the supplied byte[].
     * @param input the byte[] to search
     * @param instance the byte to search for
     * @return an int[] containing the indexes in the source array at which the specified byte was found,
     * with length equal to the number of occurrences
     */
    public static int[] instances(byte[] input, byte instance)
    {
        if(input != null) {
            ArrayList<Integer> idxs = new ArrayList<>();
            for(int i = 0; i < input.length; i++) {
                if(input[i] == instance){
                    idxs.add(i);
                }
            }
            int[] retV = new int[idxs.size()];
            for(int i = 0; i < idxs.size(); i++){
                retV[i] = idxs.get(i);
            }
            return retV;
        }
        return null;
    }

    /**
     * Compares multiple byte arrays to see if they match.
     * All provided arrays must be of the same length and contain exactly the same byte sequence to pass the check.
     * @param arrays two or more byte arrays to compare
     * @return true if all provided arrays match, false if otherwise
     */
    public static boolean compareByteArrays(byte[]... arrays)
    {
        if(arrays == null) return false;
        if(arrays.length <= 1) return true;

        byte[] original = arrays[0];
        for(int i = 1; i < arrays.length; i++)
        {
            byte[] compare = arrays[i];
            if(compare.length != original.length) return false;
            else for(int j = 0; j < original.length; j++){
                    if(compare[j] != original[j]) return false;
                }
        }

        return true;
    }

    /**
     * Converts a String into a byte array by parsing it two characters at a time and converting the found characters
     * into a byte value equivalent to the hex value (<i>NOT</i> the character ID) of the parsed characters.
     * For example, the string {@code "ff1002fe"} would be parsed into the byte values {@code 127,-112,-126,126}, which
     * are equivalent to the hex value {@code FF1002FE}.
     * @param input the string to parse for values
     * @return a byte array containing the hex equivalents of the parsed characters. The array's length will be equivalent
     * to {@code (int)Math.ceil(input.length / 2)}. Since the array represents <i>pairs</i> of input characters as single
     * bytes, it is impossible to represent a single odd character at the end of an input string. Thus, in this case, the
     * ending character of the input string will be padded with a virtual leading zero.
     * If the input is null or zero-length, the result will be null.
     * @see ARKArrayUtil#byteArrayToHexString(byte[]) for information about the reverse of this conversion
     */
    public static byte[] hexStringToBytes(@NotNull String input)
    {
        if(input == null || input.length() == 0) return null;

        String hex = input.trim().toLowerCase();

        ByteBuffer buffer = ByteBuffer.allocate((int)Math.ceil(hex.length() / 2.0));
        try{
            boolean done = false;

            if(hex.length() <= 2) {
                buffer.putInt(Integer.parseInt(hex, 16));
                done = true;
            }

            int last = 0;
            int interval = 2;
            while (!done){
                buffer.put((byte)(Integer.parseInt(hex.substring(last, last + interval), 16) - 128));
                last += interval;
                if(last == hex.length()) done = true;
                else if(last + interval > hex.length()){
                    interval = 1;
                }
            }
        } catch (NumberFormatException e){
            return null;
        }

        return buffer.array();
    }

    /**
     * Converts a byte array into a String by parsing each byte in the array and converting it to a two-character hex value.
     * The resultant string will be a concatenation of all of the two-character values produced in this way. Note that the
     * result is not produced by <i>totalling</i> the bytes in the array, but rather by reading each value separately and
     * concatenating them once they have been converted to Strings.
     * @param input the byte array to parse into a String
     * @return the hex equivalent of each byte in the array in String form. If the input is zero-length or null, the result
     * will be null.
     * @see ARKArrayUtil#hexStringToBytes(String) for information about the reverse of this conversion
     */
    public static String byteArrayToHexString(@NotNull byte[] input)
    {
        if(input == null || input.length == 0) return null;

        StringBuilder st = new StringBuilder();
        for(byte b : input ){
            st.append(String.format("%02x", b + 128));
        }
        return st.toString();
    }

    /**
     * Converts a base-36 string (consisting of the characters: a-z A-Z 0-9) to a base-16 string (consisting of the
     * characters: 0-9 A-F). This is done by breaking the input string into 12-character chunks, decoding each chunk individually
     * into a decimal number, re-encoding each chunk into a hexadecimal number, and finally concatenating all of the chunks
     * back into a single string.
     * Can also be used to convert base-64 strings, but will not actually decode them via base-64,
     * instead interpreting the string as base-36 and decoding it that way.
     * @param input the base-36 input string to decode. If the input String is an odd number of characters long, the last
     * character will be padded with a leading zero.
     * @return the hexadecimal string equivalent of the input
     */
    public static String base36ToHexString(@NotNull String input)
    {
        if(input == null || input.length() == 0) return null;

        ArrayList<String> subStrings = new ArrayList<>();
        if(input.length() > 12){
            int rem = input.length();
            while(rem > 0){
                subStrings.add(input.substring((rem - 12 < 0 ? 0 : rem - 12), rem).toLowerCase().trim().replace(" ", ""));
                rem -= 12;
            }
        }else{
            subStrings.add(input);
        }

        StringBuilder res = new StringBuilder();
        for(String s : subStrings){
            // take out '=' when decoding in case the input is base-64
            try {
                String r = Long.toHexString(Long.parseLong(s.replace("=", ""), 36));
                // prepend leading 0 if result is a 1-digit hex value
                if(r.length() % 2 == 1) r = r.substring(0, r.length() - 1) + "0" + r.substring(r.length() - 1, r.length());
                res.append(r);
            }catch (NumberFormatException e){
                return null;
            }
        }

        return res.toString();
    }

    /**
     * Gets the index of a partial or complete {@link String} in a provided {@link String} array.
     * The search is conducted by calling {@link String#contains(CharSequence)} on each entry in the array and returning
     * the index of the first array entry that returns {@code true}, if any. Will return {@code -1} if no matching entries
     * are found, or if the input array or query are invalid.
     * @param array the array to search
     * @param search a {@link String} to be used as the search criteria
     * @return the index of the first matching {@link String} in the provided array, or -1 if none are found
     */
    public static int containsString(String[] array, String search)
    {
        if(array == null || array.length == 0 || search == null) return -1;
        if(search.isEmpty()) return 0;

        for(int i = 0; i < array.length; i++){
            if(array[i].contains(search)) return i;
        }

        return -1;
    }

    /**
     * Checks the contents of any array to see if they are all equivalent to null.
     * @param array the array to check
     * @return {@code true} if the array itself or all of its entries are null, {@code false} otherwise
     */
    public static boolean allNull(Object[] array)
    {
        if(array == null || array.length == 0) return true;

        for(Object o : array) if (o != null) return false;
        return true;
    }
}
