package core.CoreUtil.ARKJsonParser;

import com.sun.istack.internal.NotNull;
import core.CoreUtil.IOTools;

import java.util.ArrayList;

/**
 * Represents all elements contained within a JSON document.
 * Includes methods for parsing data in said document.
 */
public class ARKJsonObject
{
    private String content;
    private ArrayList<ARKJsonElement> arrays;
    private ArrayList<ARKJsonElement> elements;
    private boolean hasLoaded;

    private final char compoundStartMark= '{';
    private final char compoundEndMark  = '}';
    private final char arrayStartMark   = '[';
    private final char arrayEndMark     = ']';
    private final char valueStartMark   = '"';
    
    private final String keyStartMark     = "\"";
    private final String keyEndMark       = "\"";
    private final String separator        = ":";
    private final String elementEndMark   = "," + ARKJsonParser.LINE_SEPARATOR;
    private final String altElementEndMark= ARKJsonParser.LINE_SEPARATOR;

    public ARKJsonObject(@NotNull String content)
    {
        this.content = content;
        // Try to validate. If it fails, try force-formatting the input. If that still fails, throw an IAX.
        if(!validateContent()){
            this.content = ARKJsonParser.formatJSONStr(content, ARKJsonParser.INDENT_SPACING_COUNT);
            if(!validateContent()) throw new IllegalArgumentException("Input data does not appear to be valid JSON.");
        }
        this.arrays = new ArrayList<>();
        this.elements = new ArrayList<>();
        hasLoaded = false;
    }

    //
    // GENERAL METHODS
    //

    /**
     * Parses the {@link #content} of this object into a JSON object map.
     * Automatically called whenever one of the element retrieval methods is called.
     * Since this operation is rather time-consuming, it may be initiated manually if needed.
     * Once this method has been called, either internally or externally, any successive calls
     * to this method will have no effect.
     */
    public void parse()
    {
        // Stored content was validated in the constructor, so we don't need to validate it.
        ArrayList<ARKJsonElement> result = parseDocumentInternal(this.content);

        for(ARKJsonElement e : result){
            if(e == null) continue;
            if(e.isArray()) arrays.add(e);
            else elements.add(e);
        }

        hasLoaded = true;
    }

    /**
     * Gets the current top-level element map (does not include arrays).
     * @return a copy of the current element map
     */
    public ArrayList<ARKJsonElement> getElementMap() {
        return new ArrayList<>(elements);
    }

    /**
     * Gets the current top-level array map (does not include normal elements).
     * @return a copy of the current array map
     */
    public ArrayList<ARKJsonElement> getArrayMap() {
        return new ArrayList<>(arrays);
    }

    /**
     * Converts this object's stored JSON object map into a JSON document, with all formatting and object data in place.
     * @return the JSON document interpretation of all JSON data stored by this object.
     */
    public String getJSONText()
    {
        if(!hasLoaded || (elements.size() == 0 && arrays.size() == 0)) return "";

        StringBuilder output = new StringBuilder();

        // Append JSON header ({\r\n).
        output.append(compoundStartMark).append(ARKJsonParser.LINE_SEPARATOR);

        // Add all standalone elements or compound elements.
        for(int i = 0; i < elements.size(); i++)
        {
            ARKJsonElement e = elements.get(i);

            output.append(e.toJSON(ARKJsonParser.INDENT_SPACING_COUNT, i == elements.size() - 1));
        }

        // Add all standalone arrays.
        for(int i = 0; i < arrays.size(); i++)
        {
            ARKJsonElement e = arrays.get(i);

            output.append(e.toJSON(ARKJsonParser.INDENT_SPACING_COUNT, i == arrays.size() -1));
        }

        // Add compound end mark.
        output.append(compoundEndMark);

        return output.toString();
    }

    //
    // ELEMENT METHODS
    //

    /**
     * Gets a JSON element or compound element by name.
     * Only searches the top layer of the map.
     * To search other map layers or the entire map, see {@link #getElementByNameInEntireMap(String)} or
     * {@link #getElementByNameInSubElements(String, ARKJsonElement)}.
     * @param name the name of the element to get
     * @return the first element with the specified name, or null if the element does not exist in the object map
     */
    public ARKJsonElement getElementByName(String name)
    {
        if(!hasLoaded) parse();

        for(ARKJsonElement e : elements){
            if(e.getName().equals(name)){
                return e;
            }
        }
        return null;
    }

    /**
     * Gets a JSON element or compound element by name.
     * Also parses sub-elements of all elements in the object map, searching the entire map instead of just the top layer.
     * @param name the name of the element to get
     * @return the first element with the specified name, or null if the element does not exist in the object map
     */
    public  ARKJsonElement getElementByNameInEntireMap(String name)
    {
        if(!hasLoaded) parse();

        for(ARKJsonElement e : elements) {
            ARKJsonElement result = parseSubElementsInternal(name, e);
            if(result != null) return result;
        }
        return null;
    }

    /**
     * Gets a JSON element or compound element by name.
     * Searches only this element and its sub-elements.
     * @param name the name of the element to get
     * @return the first element with the specified name, or null if the element does not exist in the object map
     */
    public ARKJsonElement getElementByNameInSubElements(String name, @NotNull ARKJsonElement element)
    {
        if(!hasLoaded) parse();

        return parseSubElementsInternal(name, element);
    }

    /**
     * Gets all instances of {@link ARKJsonElement}s with the specified name.
     * Searches the entire map.
     * <b>WARNING: </b> VERY memory- and processor-intensive. Avoid doing this unless you absolutely need to, especially
     * on larger JSON maps.
     * @param name the name of the element(s) to search for
     * @return an {@link ArrayList} containing all elements with the specified name
     */
    public ArrayList<ARKJsonElement> getOccurrencesByName(String name)
    {
        ArrayList<ARKJsonElement> results = new ArrayList<>();

        for(ARKJsonElement e : elements) {
            results.addAll(parseSubElementOccurrencesInternal(name, e));
        }

        return results;
    }

    /**
     * Gets the number of elements with the specified name in the object map.
     * Searches the entire map.
     * <b>WARNING: </b> VERY memory- and processor-intensive. Avoid doing this unless you absolutely need to, especially
     * on larger JSON maps.
     * @param name the name of the element(s) to search for
     * @return the number of times elements with the specified name occur in the object map
     */
    public int getOccurrenceCountByName(String name) {
        return getOccurrencesByName(name).size();
    }

    //
    // ARRAY METHODS
    //

    /**
     * Gets a JSON array-type element by name.
     * @param name the name of the element to get
     * @return the first array with the specified name, or null if the array does not exist in the object map
     */
    public ARKJsonElement getArrayByName(String name)
    {
        if(!hasLoaded) parse();

        for(ARKJsonElement e : arrays){
            if(e.getName().equals(name)){
                return e;
            }
        }
        return null;
    }

    /**
     * Gets a JSON array-type element by name.
     * Also parses sub-elements of all elements in the object map, searching the entire map instead of just the top layer.
     * @param name the name of the element to get
     * @return the first array with the specified name, or null if the array does not exist in the object map
     */
    public  ARKJsonElement getArrayElementByNameInEntireMap(String name)
    {
        if(!hasLoaded) parse();

        for(ARKJsonElement e : arrays) {
            ARKJsonElement result = parseArraySubElementsInternal(name, e);
            if(result != null) return result;
        }
        return null;
    }

    /**
     * Gets a JSON array-type element by name.
     * Searches only this element and its sub-elements.
     * @param name the name of the element to get
     * @return the first array with the specified name, or null if the array does not exist in the object map
     */
    public ARKJsonElement getArrayElementByNameInSubElements(String name, @NotNull ARKJsonElement element)
    {
        if(!hasLoaded) parse();

        return parseArraySubElementsInternal(name, element);
    }

    /**
     * Gets all instances of array-type {@link ARKJsonElement}s with the specified name.
     * Searches the entire map.
     * <b>WARNING: </b> VERY memory- and processor-intensive. Avoid doing this unless you absolutely need to, especially
     * on larger JSON maps.
     * @param name the name of the element(s) to search for
     * @return an {@link ArrayList} containing all array-type elements with the specified name
     */
    public ArrayList<ARKJsonElement> getArrayOccurrencesByName(String name)
    {
        ArrayList<ARKJsonElement> results = new ArrayList<>();

        for(ARKJsonElement e : arrays) {
            results.addAll(parseArraySubElementOccurrencesInternal(name, e));
        }

        return results;
    }

    /**
     * Gets the number of elements with the specified name in the object map.
     * Searches the entire map.
     * <b>WARNING: </b> VERY memory- and processor-intensive. Avoid doing this unless you absolutely need to, especially
     * on larger JSON maps.
     * @param name the name of the element(s) to search for
     * @return the number of times array-type elements with the specified name occur in the object map
     */
    public int getArrayOccurrenceCountByName(String name) {
        return getArrayOccurrencesByName(name).size();
    }

    //
    // INTERNAL UTILITY METHODS
    //

    private ARKJsonElement parseSubElementsInternal(String name, ARKJsonElement e)
    {
        if(e.getName().equals(name)) return e;

        if(e.hasSubElements()){
            for(ARKJsonElement element : e.getSubElements())
            {
                ARKJsonElement result = parseSubElementsInternal(name, element);
                if(result != null) return result;
            }
        }
        return null;
    }

    private ArrayList<ARKJsonElement> parseSubElementOccurrencesInternal(String name, ARKJsonElement e)
    {
        ArrayList<ARKJsonElement> results = new ArrayList<>();

        if(e.getName().equals(name)) results.add(e);

        if(e.hasSubElements()){
            for(ARKJsonElement element : e.getSubElements())
            {
                ArrayList<ARKJsonElement> result = parseSubElementOccurrencesInternal(name, element);
                if(result.size() != 0) results.addAll(result);
            }
        }
        return results;
    }

    private ARKJsonElement parseArraySubElementsInternal(String name, ARKJsonElement e)
    {
        if(e.getName().equals(name) && e.isArray()) return e;

        if(e.hasSubElements()){
            for(ARKJsonElement element : e.getSubElements())
            {
                ARKJsonElement result = parseSubElementsInternal(name, element);
                if(result != null) return result;
            }
        }
        return null;
    }

    private ArrayList<ARKJsonElement> parseArraySubElementOccurrencesInternal(String name, ARKJsonElement e)
    {
        ArrayList<ARKJsonElement> results = new ArrayList<>();

        if(e.getName().equals(name) && e.isArray()) results.add(e);

        if(e.hasSubElements()){
            for(ARKJsonElement element : e.getSubElements())
            {
                ArrayList<ARKJsonElement> result = parseSubElementOccurrencesInternal(name, element);
                if(result.size() != 0) results.addAll(result);
            }
        }
        return results;
    }

    private ArrayList<ARKJsonElement> parseDocumentInternal(String content)
    {
        if(content == null || content.isEmpty()) return new ArrayList<>();
        ArrayList<ARKJsonElement> results = new ArrayList<>();

        // Iterate through the document until we hit the end of our scope.
        int lastMark = 0;
        while (lastMark >= 0)
        {
            // Get key name and update start mark to the index of the separator (plus one for the following space if there is one)
            String key = IOTools.getFieldFromData(content, keyStartMark, keyEndMark, lastMark);
            lastMark = content.indexOf(separator, lastMark);
            if(content.charAt(lastMark + 1) == ' ') lastMark ++;

            if (content.charAt(lastMark + 1) == compoundStartMark)
            {
                // We're dealing with a compound tag, so get the end index of the tag block.
                int blockEndIndex = getElementTagBlockClosureIndex(content, lastMark + 1, compoundEndMark);
                // If the end index is -1, the block is probably zero-length, and we can skip looking for sub-elements.
                if (blockEndIndex < 0) {
                    lastMark = -1;
                }else {
                    // Otherwise, parse all of its sub-elements as well, and add the resulting elements to the result list.
                    ArrayList<ARKJsonElement> res = parseDocumentInternal(content.substring(lastMark, blockEndIndex));
                    results.add(new ARKJsonElement(key, false, null, res.toArray(new ARKJsonElement[res.size()])));
                    lastMark = blockEndIndex + 1;
                }
            } else if (content.charAt(lastMark + 1) == arrayStartMark)
            {
                // We're dealing with an array, so get the end index of the array delimiter.
                int blockEndIndex = getElementTagBlockClosureIndex(content, lastMark + 1, arrayEndMark);
                // If the end index is -1, the block is probably zero-length, and we can skip looking for sub-elements.
                if (blockEndIndex < 0) {
                    lastMark = -1;
                }else {
                    // Otherwise, parse all of its sub-elements as well, and add the resulting elements to the result list.
                    int spaces = getLeadingSpaceCount(content, content.indexOf(compoundStartMark, lastMark));
                    ArrayList<ARKJsonElement> res = parseDocumentArrayInternal(content.substring(lastMark, blockEndIndex), spaces);
                    results.add(new ARKJsonElement(key, true, null, res.toArray(new ARKJsonElement[res.size()])));
                    lastMark = blockEndIndex + 1;
                }
            } else if (content.charAt(lastMark + 1) == valueStartMark)
            {
                // Assume that the element is a string or other escaped literal.
                String value = IOTools.getFieldFromData(content, " ", elementEndMark, lastMark);
                // If the first try fails with the standard end mark, try again with the alternate end mark.
                if(value == null) value = IOTools.getFieldFromData(content, " ", altElementEndMark, lastMark);

                // Add the result to the result list.
                results.add(new ARKJsonElement(key, false, value));
                int nextElement = content.indexOf(elementEndMark, lastMark);
                lastMark = nextElement == -1 ? -1 : nextElement + 1;
            } else {
                // Assume that the element is an unescaped integer or boolean expression, format it as such.
                String value = IOTools.getFieldFromData(content, " ", elementEndMark, lastMark);
                // If the first try fails with the standard end mark, try again with the alternate end mark.
                if(value == null) value = IOTools.getFieldFromData(content, " ", altElementEndMark, lastMark);

                // Add the result to the result list.
                results.add(new ARKJsonElement(key, false, value));
                int nextElement = content.indexOf(elementEndMark, lastMark);
                lastMark = nextElement == -1 ? -1 : nextElement + 1;
            }
        }
        
        return results;
    }

    private ArrayList<ARKJsonElement> parseDocumentArrayInternal(String content, int spaceCount)
    {
        ArrayList<ARKJsonElement> results = new ArrayList<>();

        // Start with the index of the block open character. Iterate through the scope, running the parser on each contained block
        // element and storing its sub-elements into the result list. Once done, update the start point to the next block
        // and repeat until we run out of blocks.
        int lastBlock = content.indexOf(compoundStartMark);
        while (lastBlock > -1)
        {
            // Get the index of its matching close-block character.
            int nextBlockClose = getBlockClosureIndex(content, lastBlock, spaceCount, compoundEndMark);
            // If we can't find the closure point, assume the entire scope instead.
            if(nextBlockClose == -1) nextBlockClose = content.length();

            // Get sub-elements, and add them to the array under a null-named element.
            ArrayList<ARKJsonElement> res = parseDocumentInternal(content.substring(lastBlock, nextBlockClose));
            results.add(new ARKJsonElement(null, false, null, res.toArray(new ARKJsonElement[res.size()])));

            // Update the starting point. If there are no more blocks, this will be -1, and the loop will terminate on the next cycle.
            lastBlock = content.indexOf(compoundStartMark, nextBlockClose);
        }

        return results;
    }

    private int getElementTagBlockClosureIndex(String content, int previousKeyEndSeparatorIndex, char blockClose)
    {
        // Back up to remove the ": " from the head of the element name.
        int elementKeyEnd = previousKeyEndSeparatorIndex - 5;

        // Get the number of leading spaces behind the key name. This tells us how far it's indented, and as a result,
        // how indented the end of this particular block will be.
        int spaceCount = getLeadingSpaceCount(content, previousIndexOf(content, "\"", elementKeyEnd));

        return getBlockClosureIndex(content, previousKeyEndSeparatorIndex, spaceCount, blockClose);
    }

    private int getBlockClosureIndex(String content, int blockOpenCharIndex, int spaceCount, char blockClose)
    {
        // Iterate through the document, checking each instance of the block close character to see how far
        // indented it is. If its indentation count matches the block opening character, it's the close for this
        // block. If we can't find one, assume that the document is incomplete and just use all of it. If the first block
        // end character is found right next to the block open character, assume that the block is empty and return without
        // checking spacing.
        int blockEndIndex = -1;
        int lastBlockClose = blockOpenCharIndex;
        while(blockEndIndex == -1){
            int nextBlockClose = content.indexOf(blockClose, lastBlockClose + 1);
            if(nextBlockClose - blockOpenCharIndex <= 1) return nextBlockClose;
            if(nextBlockClose == -1) blockEndIndex = content.length();
            else if(getLeadingSpaceCount(content, nextBlockClose) == spaceCount) blockEndIndex = nextBlockClose + 1;
            else lastBlockClose = nextBlockClose;
        }

        return blockEndIndex;
    }

    private int getLeadingSpaceCount(String input, int startIndex)
    {
        if(input == null || input.isEmpty() || startIndex < 0 || startIndex > input.length()) return -1;

        boolean found = false;
        int pos = startIndex - 1;
        int count = 0;
        while(!found){
            if(pos <= 0 || input.charAt(pos) != ' '){
                found = true;
            }else{
                count ++;
                pos --;
            }
        }
        return count;
    }

    private int previousIndexOf(String input, char ch, int startIndex) {
        return previousIndexOf(input, "" + ch, startIndex);
    }

    private int previousIndexOf(String input, String str, int startIndex)
    {
        if(input == null || input.isEmpty() || startIndex < 0 || startIndex > input.length() || str == null || str.isEmpty()) return -1;

        int pos = startIndex;
        while(true){
            if(pos - str.length() < 0){
                return -1;
            }else if(input.substring(pos - str.length(), pos).equals(str)){
                return pos - str.length();
            }else {
                pos --;
            }
        }
    }

    private boolean validateContent() {
        return this.content != null && !this.content.isEmpty() && this.content.charAt(0) == '{' && (this.content.charAt(1) == 0x0d || this.content.charAt(1) == 0x0a);
    }
}