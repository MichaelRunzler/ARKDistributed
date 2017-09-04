package core.CoreUtil;

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
}
