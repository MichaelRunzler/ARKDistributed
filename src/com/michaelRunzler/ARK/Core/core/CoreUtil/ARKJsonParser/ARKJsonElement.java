package core.CoreUtil.ARKJsonParser;

/**
 * Represents a JSON element or compound element. Elements are identified by a name, and may contain either a value or
 * any number of sub-elements (instances of this object). As such, element trees may be (theoretically) infinitely deep.
 * However, recursion is not possible due to the lack of object references in JSON.
 * If this is an array-type element, all immediate sub-entries will have name parameters equal to {@code null} or {@code ""} (empty string).
 * Call {@link #isArray()} to see if this is an array-type element.
 *
 * Sub-elements are parsable by index, direct reference, or (if this is not an array-type element) name.
 */
public class ARKJsonElement
{
    private ARKJsonElement[] subElements;
    private String name;
    private String value;
    private boolean isArray;

    /**
     * Constructs a new instance of this object.
     * @param name the name or key value of this element, used to identify it. This should be {@code null} for elements that
     *             are members of an array-type element.
     * @param isArray this should be {@code true} if this element represents an array, {@code false} if this represents
     *                a standard element, compound element, or array member.
     * @param value the value of this element. Values may be of any type, but are always represented as strings.
     *              This should be {@code null} if this is a compound element, array-type element, or array member element.
     * @param subElements the list of child or sub-elements below this element in the element hierarchy. This should be {@code null}
     *                    if this is a standard-type element with a non-{@code null} value property.
     */
    public ARKJsonElement(String name, boolean isArray, String value, ARKJsonElement... subElements)
    {
        this.name = name;
        this.value = value;
        this.isArray = isArray;
        this.subElements = subElements;
    }

    /**
     * Gets the name (or 'key') property of this element. Elements may not have names if they are array members.
     * @return the name of this element, or null if it does not have one
     */
    public String getName(){
        return name;}

    /**
     * Gets the value property of this element. Elements may not have values if they are compound elements or array-type elements.
     * @return the value of this element, or null if it does not have one
     */
    public String getValue(){
        return hasSubElements() ? null : value;}

    /**
     * Gets the value property of this element. Elements may not have values if they are compound elements or array-type elements.
     * Removes delimiting quotes if the value has them.
     * @return the de-quoted value of this element, or null if it does not have one
     */
    public String getDeQuotedValue() {
        return ARKJsonParser.deQuoteJSONValue(this.getValue());
    }

    /**
     * Gets if this element has sub-elements. This is a good way of telling if this element is a compound or array-type
     * element.
     * @return {@code true} if this element has sub-elements and no value, {@code false} if otherwise.
     */
    public boolean hasSubElements(){
        return this.subElements != null && this.value == null;}

    /**
     * Gets the current set of sub-elements possessed by this element, or null if it does not have any.
     * @return this element's sub-element list
     */
    public ARKJsonElement[] getSubElements(){return this.subElements;}

    /**
     * Gets if this element is an array-type element. If a call to this method returns {@code true}, calling {@link #hasSubElements()}
     * must also return {@code true}.
     * @return {@code true} if this element is an array-type compound element, {@code false} if otherwise
     */
    public boolean isArray() {
        return isArray;
    }

    /**
     * Gets one of this element's sub-elements by searching the top level of the sub-element list by name.
     * Will thrown an {@link IllegalArgumentException} if this element is non-compound and non-array.
     * @param name the name of the sub-element to search for
     * @return the first element in the sub-element list with a name matching the query, or null if none was found
     */
    public ARKJsonElement getSubElementByName(String name)
    {
        if(!this.hasSubElements()) throw new IllegalArgumentException("This object does not possess sub-elements.");

        for(ARKJsonElement e : subElements) {
            if(e.getName().equals(name)) return e;
        }
        return null;
    }

    /**
     * Gets one of this element's sub-elements by its relative index in the sub-element array.
     * Will thrown an {@link IllegalArgumentException} if this element is non-compound and non-array, or if the provided
     * index is negative or out-of-bounds.
     * Equivalent to calling {@code {@link #getSubElements()}[index]}.
     * @param index the index of the element to search for
     * @return the sub-element at the provided index. Will not return null unless the element at the specified index is null.
     */
    public ARKJsonElement getSubElementByIndex(int index)
    {
        if(!this.hasSubElements() || index < 0 || index >= subElements.length) throw new IllegalArgumentException("Index is out of acceptable bounds.");

        return subElements[index];
    }

    /**
     * Adds a sub-element to the end of this object's sub-element list.
     * Will thrown an {@link IllegalArgumentException} if this element is non-compound and non-array.
     * @param e the element to add to this element's sub-element list. May be null.
     */
    public void addSubElement(ARKJsonElement e)
    {
        if(!this.hasSubElements()) throw new IllegalArgumentException("This element is not compound, cannot have sub-elements.");

        ARKJsonElement[] temp = new ARKJsonElement[subElements.length + 1];
        System.arraycopy(subElements, 0, temp, 0, subElements.length);
        temp[temp.length - 1] = e;
        subElements = temp;
    }

    /**
     * Inserts a sub-element into the sub-element list at the specified index.
     * Other elements in the sub-element list at or after the specified index will be moved forward one position to allow for the new element.
     * Will thrown an {@link IllegalArgumentException} if this element is non-compound and non-array.
     * @param e the element to add to this element's sub-element list. May be null.
     * @param index the index to insert the new element at
     */
    public void addSubElement(ARKJsonElement e, int index)
    {
        if(!this.hasSubElements() || index < 0 || index >= subElements.length) throw new IllegalArgumentException("Index is out of acceptable bounds.");

        int after = subElements.length - index;
        ARKJsonElement[] temp = new ARKJsonElement[subElements.length + 1];
        if(index > 0) System.arraycopy(subElements, 0, temp, 0, index);
        if(after > 0) System.arraycopy(subElements, index, temp, index + 1, after);
        temp[index] = e;
        subElements = temp;
    }

    /**
     * Translates this element into formatted JSON text with proper spacing. Recursively calls this method on all of its
     * sub-elements if it has any.
     * @param spaceCount the number of leading spaces to be placed before this element. If this element has sub-elements,
     *                   they will use spacing equal to this argument plus {@link ARKJsonParser#INDENT_SPACING_COUNT} for each
     *                   level of sub-element.
     * @param isFinal if this is set to {@code false}, this element will append a comma after its value or sub-element tree.
     *                If set to {@code true}, only the line separator will be appended.
     * @return the JSON text representation of this element and its sub-elements
     */
    public String toJSON(int spaceCount, boolean isFinal)
    {
        // If this element has neither a value nor a name, and is not compound, it is invalid, and should not be added to the output.
        // Return an empty string.
        if(this.name == null && this.value == null && !this.hasSubElements()) return "";

        StringBuilder str = new StringBuilder();

        String spacer = "";
        for(int i = 0; i < spaceCount; i++) spacer += " ";

        // Since this element is non-compound, it must have either a name or a value (or one of the two). Append them,
        // along with spacing and comma if necessary.
        if(!this.hasSubElements()){
            str.append(spacer);
            if(this.name != null) str.append("\"").append(this.name).append("\"").append(": ");
            str.append(this.value == null ? "" : value);
            if(!isFinal) str.append(",");
            str.append(ARKJsonParser.LINE_SEPARATOR);
            return str.toString();
        }

        // If this element is compound, append its name if it has one and its block opener character.
        str.append(spacer);
        if(this.name != null) str.append("\"").append(this.name).append("\"").append(": ");
        str.append(this.isArray ? '[' : '{');
        str.append(ARKJsonParser.LINE_SEPARATOR);

        // Add all sub-elements.
        for(int i = 0; i < subElements.length; i++){
            ARKJsonElement e = subElements[i];
            str.append(e.toJSON(spaceCount + ARKJsonParser.INDENT_SPACING_COUNT, i == subElements.length - 1));
        }

        // Add block close character, comma if this is not the final entry, and spacing.
        str.append(spacer).append(this.isArray ? ']' : '}').append(isFinal ? "" : ",").append(ARKJsonParser.LINE_SEPARATOR);

        return str.toString();
    }
}
