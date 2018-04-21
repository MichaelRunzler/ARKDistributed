package core.UI.ModeLocal;

import javafx.scene.Node;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Controls mode-switch operations for a JFX UI class, changing the visibility and state of {@link Node}s inside of said class
 * when told to. Uses the {@link ModeLocal} annotation to control which nodes are visible under which conditions.
 */
public class ModeSwitchController
{
    private Map<Node, ModeLocal[]> annotatedNodes;
    private ArrayList<ModeSwitchHook> modeSwitchHooks;
    private Class associatedClass;
    private Object assocClassInstance;
    private String[] identifiers;
    private int currentMode;

    /**
     * Constructs a new instance of this object. Uses the default (empty) annotation-identifier string. These cannot be changed once
     * set, and this object must be re-initialized to change or clear its set identifiers.
     * Automatically collects information about its calling class and stores it for later use. If the calling class has fields
     * which are non-static and annotated with {@link ModeLocal} annotations, use a constructor variant that takes a class
     * instance instead, such as {@link #ModeSwitchController(Object)}.
     * @throws ClassNotFoundException if a reference to the calling class cannot be obtained through reflection or exception-tracing
     */
    public ModeSwitchController() throws ClassNotFoundException
    {
        annotatedNodes = null;
        modeSwitchHooks = new ArrayList<>();
        currentMode = 0;
        this.identifiers = new String[]{""};

        // Get caller class so that we can directly reference its fields without a passed object reference from it
        StackTraceElement str = new Exception().getStackTrace()[2];
        associatedClass = Class.forName(str.getClassName());
        assocClassInstance = null;
    }

    /**
     * Constructs a new instance of this object. Uses the default (empty) annotation-identifier string. These cannot be changed once
     * set, and this object must be re-initialized to change or clear its set identifiers.
     * Automatically collects information about its calling class and stores it for later use. Uses the provided class instance
     * to retrieval field values. This (or any other constructor variant that takes a class instance as an argument) must be
     * used if the calling class has non-static fields that are annotated with {@link ModeLocal} annotations.
     * @throws ClassNotFoundException if a reference to the calling class cannot be obtained through reflection or exception-tracing
     */
    public ModeSwitchController(Object classInstance) throws ClassNotFoundException
    {
        this();
        assocClassInstance = classInstance;
    }

    /**
     * Constructs a new instance of this object. Uses the provided annotation-identifier string(s). These cannot be changed once
     * set, and this object must be re-initialized to change or clear its set identifiers.
     * Automatically collects information about its calling class and stores it for later use. If the calling class has fields
     *  which are non-static and annotated with {@link ModeLocal} annotations, use a constructor variant that takes a class
     *  instance instead, such as {@link #ModeSwitchController(Object)}
     * @param identifiers the annotation-identifier strings to associate with. Only {@link Node}s annotated with {@link ModeLocal} annotations that
     *                    have identifiers set which match one of these strings will be subject to mode-changes instated by this object.
     *                    All other identifiers (as well as annotations with no or default identifiers, unless the default identifier is also included in the list)
     *                    will be ignored by this object during mode changes. Providing {@code null}, a zero-length array, or no
     *                    arguments at all will result in identical behavior to {@link #ModeSwitchController()} being used.
     * @throws ClassNotFoundException if a reference to the calling class cannot be obtained through reflection or exception-tracing
     */
    public ModeSwitchController(String... identifiers) throws ClassNotFoundException
    {
        this();
        this.identifiers = identifiers == null ? new String[]{""} : identifiers;
    }

    /**
     * Constructs a new instance of this object. Uses the provided annotation-identifier string(s). These cannot be changed once
     * set, and this object must be re-initialized to change or clear its set identifiers.
     * Automatically collects information about its calling class and stores it for later use. Uses the provided class instance
     * to retrieval field values. This (or any other constructor variant that takes a class instance as an argument) must be
     * used if the calling class has non-static fields that are annotated with {@link ModeLocal} annotations.
     * @param identifiers the annotation-identifier strings to associate with. Only {@link Node}s annotated with {@link ModeLocal} annotations that
     *                    have identifiers set which match one of these strings will be subject to mode-changes instated by this object.
     *                    All other identifiers (as well as annotations with no or default identifiers, unless the default identifier is also included in the list)
     *                    will be ignored by this object during mode changes. Providing {@code null}, a zero-length array, or no
     *                    arguments at all will result in identical behavior to {@link #ModeSwitchController()} being used.
     * @throws ClassNotFoundException if a reference to the calling class cannot be obtained through reflection or exception-tracing
     */
    public ModeSwitchController(Object classInstance, String... identifiers) throws ClassNotFoundException
    {
        this(classInstance);
        this.identifiers = identifiers == null ? new String[]{""} : identifiers;
    }

    /**
     * Switches the state of all valid and annotated {@link Node}s in the associated class, and calls all hooks in the internal
     * mode-switch hook registry. Caches all valid nodes to an internal cache map if none are already loaded in order to reduce
     * CPU and memory access times. There is no limit on mode IDs, IDs which are effectively out-of-bounds (i.e there are no nodes
     * that are annotated with said ID) will simply result in none of the annotated nodes being visible until the mode is changed
     * back to a mode which is in-bounds.
     * @param mode the mode to switch to
     */
    public void switchMode(int mode)
    {
        currentMode = mode;

        // If a cached copy of the node list is present, use that instead of the full reflection routine.
        if(annotatedNodes != null)
        {
            // Iterate through the list of annotated nodes, setting each one's visibility to the correct state.
            for(Node n : annotatedNodes.keySet()){
                ModeLocal[] mla = annotatedNodes.get(n);
                for(ModeLocal ml : mla)
                {
                    boolean visible = ml.invert();
                    for (int i : ml.value()) {
                        if (i == mode) {
                            visible = !visible;
                            break;
                        }
                    }

                    n.setVisible(visible);
                }
            }
        }else{
            Field[] fields = associatedClass.getDeclaredFields();

            Map<Node, ModeLocal[]> elements = new HashMap<>();

            // Iterate through the list of this class's declared fields, checking each one for annotations.
            // Check to see if the field is annotated with ModeLocal or its container. If it is, check its
            // parameters to see if they match the mode provided. If they do, check that the field is an instance of a Node.
            // If it is, set the node's visibility to the correct visibility for the invert settings and the mode ID,
            // then add all of the ModeLocal annotations that match the current identifier(s) to the map.
            for (Field f : fields)
            {
                // Force the field's access modifier to public to allow attribute querying and modification
                f.setAccessible(true);
                ModeLocal[] mla;
                ModeLocalContainer mlc = f.getAnnotation(ModeLocalContainer.class);

                // Check for mode-local container annotations first, since they will be present instead of mode-local annotations
                // if the field has multiple annotations. If we don't find any, check for standard mode-local annotations instead.
                if(mlc != null) mla = mlc.value();
                else{
                    ModeLocal mlt = f.getAnnotation(ModeLocal.class);
                    if(mlt != null)
                        mla = new ModeLocal[]{mlt};
                    else mla = new ModeLocal[0];
                }

                // If the field does not have an annotation of the proper type, stop forcing its access modifier to prevent odd behavior from other classes
                if(mla.length == 0){
                    f.setAccessible(false);
                    continue;
                }

                Object o;
                try {
                    o = f.get(assocClassInstance);
                    // If this field is not a Node, fall through to the catch block
                    if (!(o instanceof Node)) throw new IllegalArgumentException();
                } catch (IllegalAccessException | IllegalArgumentException e) {
                    // If the field is not of the correct type, stop forcing its access modifier to prevent odd behavior from other classes
                    f.setAccessible(false);
                    continue;
                }

                ArrayList<ModeLocal> valid = new ArrayList<>();
                // Otherwise, iterate through all of the found annotations, altering state when necessary.
                for(ModeLocal ml : mla)
                {
                    // Check if the identifier of this annotation matches any of this object's set identifiers.
                    // If it does not, skip to the next entry in the annotation stack.
                    boolean validIdentifier = false;
                    for(String s : identifiers){
                        if(s.equals(ml.identifier())){
                            validIdentifier = true;
                            break;
                        }
                    }

                    if(!validIdentifier) continue;

                    boolean visible = ml.invert();
                    for (int i : ml.value()) {
                        if (i == mode) {
                            visible = !visible;
                            break;
                        }
                    }

                    ((Node) o).setVisible(visible);
                    valid.add(ml);
                }

                // Add entry to map if any of the found annotations were valid
                if(valid.size() > 0) elements.put((Node) o, valid.toArray(new ModeLocal[0]));
            }

            // Cache the node list for future access, since this is a fairly CPU-intensive process, and the results
            // will always be the same within any given run, due to annotations being a compile-time feature.
            annotatedNodes = elements;
        }

        // Execute any mode-switch hooks that are present.
        if(modeSwitchHooks != null && modeSwitchHooks.size() > 0){
            for(ModeSwitchHook m : modeSwitchHooks){
                m.onModeSwitch(mode);
            }
        }
    }

    /**
     * Gets the current mode that this controller is in. If {@link #switchMode(int)} has not yet been called,
     * the result will be {@code 0}.
     * @return the current mode-state of this controller object
     */
    public int getCurrentMode(){
        return currentMode;
    }

    /**
     * Adds a mode-switch hook to this object's internal list. All hooks in said list will be activated when {@link #switchMode(int)}
     * is called, regardless of mode.
     * @param m the {@link ModeSwitchHook} to add to the list
     */
    public void addModeSwitchHook(ModeSwitchHook m){
        if(m != null) modeSwitchHooks.add(m);
    }

    /**
     * Removes the specified mode-switch hook from the internal list. This hook will no longer be called when the {@link #switchMode(int)}
     * method is called.
     * @param m the {@link ModeSwitchHook} to remove from the list
     */
    public void removeModeSwitchHook(ModeSwitchHook m){
        if(m != null) modeSwitchHooks.remove(m);
    }

    /**
     * Clears the internal mode-switch hook list.
     */
    public void clearModeSwitchHooks(){
        modeSwitchHooks.clear();
    }

    /**
     * Purges the currently stored list of nodes. This will force this object to repopulate the list when the {@link #switchMode(int)}
     * method is next called. Useful if the cached list is invalid, inaccurate, or corrupt in some way.
     * @param repopulate set to {@code true} if this object should immediately repopulate its cached node list instead of
     *                   waiting for the next call to {@link #switchMode(int)} to do so.
     */
    public void purgeCachedNodes(boolean repopulate)
    {
        annotatedNodes = null;
        if(repopulate) switchMode(currentMode);
    }
}
