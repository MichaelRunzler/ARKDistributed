package core.CoreUtil.ARKXMLParser;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * Stores XML element attributes and names in a paired key wrapper for ease of use.
 */
public class Element
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
