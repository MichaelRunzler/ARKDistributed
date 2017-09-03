package util;

import org.xml.sax.*;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

/**
 * Provides methods for reading the contents of XML documents.
 */
public class ARKXMLParser extends DefaultHandler
{
    //to prevent access to internal super methods - make private and wrap in a public object container class?
    private ArrayList<Element> elements;
    private SAXParser saxParser;
    private XMLReader xmlReader;
    private ARKXMLParserStateWrapper state = new ARKXMLParserStateWrapper(ARKXMLParserState.NULL);

    /**
     * Represents the state of the parser engine.
     * State list is as follows:
     *
     * NULL: The object has not been initialized, and is currently 'null'.
     * INIT: The object's constructor has been called, and is initializing.
     * IDLE: The constructor has finished, and the object is ready for use.
     * PARSING: The parser engine has been instructed to parse a document, and is currently busy.
     * COMPLETE: The parser has finished parsing its current document, and is ready for further use.
     * WARNING: The parser has logged a minor problem, and will continue to operate normally.
     * ERROR: The parser has logged an error. Operation may be affected, but the parser will attempt to finish its task.
     * FATAL: The parser has encountered an unrecoverable exception during operation, and must abort its current task.
     */
    enum ARKXMLParserState{
        NULL,INIT,IDLE,PARSING,COMPLETE,WARNING,ERROR,FATAL
    }

    /**
     * Creates a new ARK XML Parser object with associated readers and utility objects.
     */
    public ARKXMLParser()
    {
        state.state = ARKXMLParserState.INIT;
        elements = new ArrayList<>();

        try {
            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            saxParser = spf.newSAXParser();

            xmlReader = saxParser.getXMLReader();
            xmlReader.setContentHandler(this);
            xmlReader.setErrorHandler(new ARKXMLParserErrorHandler(System.err, state));
            state.state = ARKXMLParserState.IDLE;
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    //methods from super
    public void startDocument() throws SAXException {
        state.state = ARKXMLParserState.PARSING;
    }

    public void endDocument() throws SAXException {
        state.state = ARKXMLParserState.COMPLETE;
    }

    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        elements.add(new Element(localName, atts));
    }
    //end methods from super

    /**
     * Gets the current state of the parser object.
     * @return the state of the parser engine. Will return 'ARKXMLParserState.NULL' if the object has not been initialized
     */
    public ARKXMLParserState getState() {
        return state.state;
    }

    /**
     * Parses an XML document file and reads its contents to the internal element list.
     * @param source a File object representing the XML file to be parsed
     * @throws IOException if the source file is unreadable or does not exist
     * @throws SAXException if the parser encounters an error while parsing the file
     */
    public void parseXMLDocument(File source) throws IOException, SAXException
    {
        xmlReader.parse(source.getAbsolutePath());
    }

    /**
     * Parses an XML document file and reads its contents to the internal element list.
     * @param source a String URI representing the XML file to be parsed
     * @throws IOException if the source file is unreadable or does not exist
     * @throws SAXException if the parser encounters an error while parsing the file
     */
    public void parseXMLDocument(String source) throws IOException, SAXException
    {
        xmlReader.parse(source);
    }

    /**
     * Gets a copy of the element list that is currently being stored by this object.
     * @return a copied version of the list of elements stored by the parser object, or null if there are none
     */
    public ArrayList<Element> getAllElements()
    {
        if(elements != null && elements.size() > 0)
            return new ArrayList<>(this.elements);
        else
            return null;
    }

    /**
     * Gets an element with a specific name if it exists.
     * @param name the name of the element to get
     * @return the first element with the specified name, or null if no elements with that name exist
     */
    public Element getElementByName(String name)
    {
        for(Element e : elements){
            if(e.getName().equals(name)){
                try {
                    return Element.cloneObject(e);
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * Gets all elements with the specified name.
     * @param name the name of the elements to get
     * @return all elements with the specified name, or null if no elements with that name exist
     */
    public ArrayList<Element> getElementsByName(String name)
    {
        ArrayList<Element> value = new ArrayList<>();

        for(Element e : elements){
            if(e.getName().equals(name)){
                try {
                    value.add(Element.cloneObject(e));
                } catch (IllegalAccessException e1) {
                    e1.printStackTrace();
                }
            }
        }

        if(value.size() > 0)
            return value;
        else
            return null;
    }

    /*
    /**
     * Method to allow direct object execution by the JVM for testing purposes.
     * @param args arguments from the JVM console
     */
    /*
    public static void main(String[] args)
    {
        ARKXMLParser parser = new ARKXMLParser();

        System.out.println(parser.getState());

        try {
            parser.parseXMLDocument(new File(System.getProperty("user.home"), "\\Desktop\\test.xml"));
        } catch (IOException | SAXException e) {
            e.printStackTrace();
        }

        System.out.println(parser.getState());

        if(parser.getState() == ARKXMLParserState.COMPLETE)
        {
            ArrayList<Element> elements = parser.getAllElements();

            System.out.println("Element count: " + elements.size() + "\n");

            for (int i = 0; i < elements.size(); i++)
            {
                Element e = elements.get(i);
                System.out.println("Element " + i + " - " + e.getName() + ":");

                if(e.hasAttributes()){
                    Attributes attrs = e.getAttributes();
                    for(int j = 0; j < attrs.getLength(); j++) {
                        System.out.println("Attribute " + j + ": " + attrs.getQName(j) + " = " + attrs.getValue(j));
                    }
                    System.out.println();
                }else{
                    System.out.println("Element has no valid attributes.\n");
                }
            }
        }
    }
    */
}

/**
 * Handles error events from the ARK XML Parser system.
 */
class ARKXMLParserErrorHandler implements ErrorHandler
{
    private PrintStream out;
    private ARKXMLParserStateWrapper state;

    ARKXMLParserErrorHandler(PrintStream out, ARKXMLParserStateWrapper state) {
        this.out = out;
        this.state = state;
    }

    private String getParseExceptionInfo(SAXParseException spe) {
        String systemId = spe.getSystemId();

        if (systemId == null) {
            systemId = "null";
        }

        String info = "URI=" + systemId + " Line="
                + spe.getLineNumber() + ": " + spe.getMessage();

        return info;
    }

    public void warning(SAXParseException spe) throws SAXException {
        state.state = ARKXMLParser.ARKXMLParserState.WARNING;
        out.println("Warning: " + getParseExceptionInfo(spe));
    }

    public void error(SAXParseException spe) throws SAXException {
        state.state = ARKXMLParser.ARKXMLParserState.ERROR;
        String message = "Error: " + getParseExceptionInfo(spe);
        throw new SAXException(message);
    }

    public void fatalError(SAXParseException spe) throws SAXException {
        state.state = ARKXMLParser.ARKXMLParserState.FATAL;
        String message = "Fatal Error: " + getParseExceptionInfo(spe);
        throw new SAXException(message);
    }
}

/**
 * Stores XML element attributes and names in a paired key wrapper for ease of use.
 */
class Element
{
    private String name;
    private Attributes attrs;

    /**
     * Contstructs a new XML element object.
     * @param name the name of the XML node to store
     * @param attrs the Attributes object containing the attributes of the XML node to store
     */
    public Element(String name, Attributes attrs)
    {
        this.name = name;
        this.attrs = new AttributesImpl(attrs);
    }

    /**
     * Gets the Attributes object associated with this Element.
     * @return a mutable Attributes object containing all of this Element's attribute data
     */
    public Attributes getAttributes() {
        return attrs;
    }

    /**
     * Gets the name of this Element.
     * @return the name of this XML Element
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if this Element has any attribute data.
     * @return true if this Element has attributes, false if otherwise
     */
    public boolean hasAttributes() {
        return this.attrs != null && this.attrs.getLength() > 0;
    }

    /**
     * Clones an object by generating a new instance and copying its declared fields over to the new instance.
     * Sourced from Stack Overflow user "WillingLearner".
     * @param obj the object to clone
     * @param <T> the type of object to clone. Implicitly declared if the object is being assigned to a statically typed object
     * @return an exact clone of the source object
     * @throws IllegalAccessException if the source object's constructor is declared private or protected
     */
    public static <T> T cloneObject(T obj) throws IllegalAccessException
    {
        try{
            Object clone = obj.getClass().newInstance();
            for (Field field : obj.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                if(field.get(obj) == null || Modifier.isFinal(field.getModifiers())){
                    continue;
                }
                if(field.getType().isPrimitive() || field.getType().equals(String.class)
                        || field.getType().getSuperclass().equals(Number.class)
                        || field.getType().equals(Boolean.class)){
                    field.set(clone, field.get(obj));
                }else{
                    Object childObj = field.get(obj);
                    if(childObj == obj){
                        field.set(clone, clone);
                    }else{
                        field.set(clone, cloneObject(field.get(obj)));
                    }
                }
            }
            return (T)clone;
        }catch(IllegalAccessException e){
            throw e;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }
}

/**
 * Wraps an ARKXMLParserState enum value in an object to facilitate inter-class state updates.
 */
class ARKXMLParserStateWrapper
{
    public ARKXMLParser.ARKXMLParserState state;

    /**
     * Constructs a new ARK XML Parser state wrapper object.
     * @param state the initial state of the object
     */
    ARKXMLParserStateWrapper(ARKXMLParser.ARKXMLParserState state) {
        this.state = state;
    }
}
