package X34.UI;

import X34.Core.X34Core;
import X34.Core.X34Image;
import X34.Core.X34Schema;
import X34.Processors.X34ProcessorRegistry;
import core.AUNIL.XLoggerInterpreter;
import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.CMLUtils;
import core.system.ARKGlobalConstants;

import javax.xml.bind.ValidationException;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class X34CML
{
    private static final String helpText =
            "* Command-line argument detail (arguments are in '[input type]arg name: description' format):\n" +
            "* \n" +
            "* \n" +
            "* Retrieval commands:\n" +
            "* \n" +
            "* [String]tag: the tag to pull from the designated repo.\n" +
            "* [String]repo: the repo ID of the processor to be used when pulling images.\n" +
            "* [File]dest: the destination folder to write downloaded images to. If not present, will default to the local user desktop.\n" +
            "*             Incomplete file paths (such as '\\images\\download' will be appended to said desktop directory.\n" +
            "* (optional)[void]overwrite: if present, any existing files in the download directory\n" +
            "* (optional)[void]mkdirs: if present, the path to the download directory will be created if it does not exist.\n" +
            "* \n" +
            "* \n" +
            "* Debug/info commands:\n" +
            "* \n" +
            "* (optional)[void]help: displays this help block\n" +
            "* (optional)[void]processors: displays the list of current retrieval processor IDs and their names\n" +
            "* \n" +
            "* \n" +
            "* Example command-line call:\n" +
            "* java -jar X34CML.jar \"tag=elite_dangerous\" \"repo=R34X\" \"dest=\\images\" overwrite mkdirs";

    private static XLoggerInterpreter log;
    private static final File autoConfig = new File(ARKGlobalConstants.DESKTOP_DATA_ROOT.getAbsolutePath() + "\\X34", "CMLAutoCfg.x34c");
    private static final File generalConfig = new File(ARKGlobalConstants.DESKTOP_DATA_ROOT.getAbsolutePath() + "\\X34", "CMLGeneralCfg.x34c");

    private static X34Schema[] autoSchemaList = null;
    private static String[] generalArgList = null;

    /**
     * Command-line argument detail:
     * <ul>
     *     <li>
     *         Retrieval commands:
     *         <ul>
     *         <li>[String]tag: the tag to pull from the designated repo.</li>
     *         <li>[String]repo: the repo ID of the processor to be used when pulling images. If not present, will default to the local user desktop.
     *                           Incomplete file paths (such as '\images\download' will be appended to said desktop directory.</li>
     *         <li>[File]dest: the destination folder to write downloaded images to.</li>
     *         <li>(optional)[void]overwrite: if present, any existing files in the download directory</li>
     *         <li>(optional)[void]mkdirs: if present, the path to the download directory will be created if it does not exist.</li>
     *         </ul>
     *     </li>
     *     <li>
     *         Debug/info commands:
     *         <ul>
     *         <li>(optional)[void]help: displays this help block</li>
     *         <li>(optional)[void]processors: displays the list of current retrieval processor IDs and their names</li>
     *         </ul>
     *     </li>
     * </ul>
     */
    public static void main(String[] args)
    {
        //
        // check for debug/info args
        //

        if(CMLUtils.getArgument(args, "help") != null){
            System.out.println(helpText);
            System.exit(0);
        }else if(CMLUtils.getArgument(args, "processors") != null)
        {
            try {
                String[] uids = X34ProcessorRegistry.getAvailableProcessorIDs();
                String[] unames = X34ProcessorRegistry.getAvailableProcessorNames();
                System.out.println("Processor IDs and names are as follows:\n");
                for(int i = 0; i < uids.length; i++){
                    String id = uids[i];
                    String name = unames[i];
                    System.out.println("- " + id + " : " + name);
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            }
            System.exit(0);
        }

        log = new XLoggerInterpreter("X34 CLI");

        // delegate to submethod based on whether batch job keys are in place
        if(CMLUtils.getArgument(args, "tag") == null && CMLUtils.getArgument(args, "repo") == null) CLI();
        else{
            X34Schema schema = new X34Schema(CMLUtils.getArgument(args, "tag"), CMLUtils.getArgument(args, "repo"), null);
            batch(args, schema);
        }

        log.disassociate();
        System.exit(0);
    }

    /**
     * Runs the non-interactive batch job version of this application.
     * Typically called if any of the command-line arguments are in place.
     * Also called from the CLI for its retrieval branch.
     * @param args the argument list form {@link X34CML#main(String[])}
     * @param schema a valid {@link X34Schema} to be used for query and processor data, as well as metadata tags
     */
    private static void batch(String[] args, X34Schema schema)
    {
        //
        // check main args
        //

        if(!schema.validate()){
            System.out.println("One or more arguments are missing!");
            return;
        }

        // compute download root directory location
        String temp = CMLUtils.getArgument(args, "dest");
        File root;
        if(temp == null || temp.isEmpty()) root = ARKGlobalConstants.getOSSpecficDesktopRoot();
        else if(temp.startsWith("\\")) root = new File(ARKGlobalConstants.getOSSpecficDesktopRoot().getAbsolutePath() + temp);
        else root = new File(temp);

        // set directory creation and overwrite flags
        boolean overwrite = CMLUtils.getArgument(args, "overwrite") != null;
        boolean mkdirs = CMLUtils.getArgument(args, "mkdirs") != null;

        //
        // init core and initiate retrieval
        //

        log.logEvent("Initialization complete.");
        log.logEvent("Schema validation passed.");

        X34Core xCore = new X34Core();

        log.logEvent("Retrieving...");

        ArrayList<X34Image> retrieved = null;
        try {
            retrieved = xCore.retrieve(schema);
            log.logEvent("Retrieval successful" + (retrieved.size() == 0 ? "." : ", downloading..."));
        } catch (ValidationException | IOException e) {
            log.logEvent("Retrieval failed with the following exception:");
            log.logEvent(e);
        }

        if(retrieved != null && retrieved.size() > 0) {
            try {
                xCore.writeImagesToFile(retrieved, root, overwrite, mkdirs);
                log.logEvent("Download complete.");
            } catch (IOException e) {
                log.logEvent("Image download failed with the following exception:");
                log.logEvent(e);
            }
        }
    }

    /**
     * Runs the interactive CLI version of this application.
     * Typically called if the program is started with no arguments.
     */
    private static void CLI()
    {
        CLIMenuOption[] autoOptions = new CLIMenuOption[]{
                new CLIMenuOption("List Current Schemas", ()->{
                    // self-explanatory
                    if(autoSchemaList == null) autoSchemaList = getAutoModeSchemas(autoConfig);
                    if(autoSchemaList == null || autoSchemaList.length == 0){
                        System.out.println("Schema list is empty. Add some, you perv~!");
                    }else{
                        System.out.println("Current auto schema list is as follows:");
                        System.out.println();
                        System.out.println("#. PUID : Query/Tag");
                        System.out.println();
                        for(int i = 0; i < autoSchemaList.length; i++){
                            System.out.println((i + 1) + ". " + autoSchemaList[i].type + " : " + autoSchemaList[i].query);
                        }
                    }

                    System.out.println();
                    System.out.println("Enter any input to continue...");
                    // block until user enters something
                    getDataFromScanner(new Scanner(System.in), System.out, "");
                    return true;
                }),

                new CLIMenuOption("Add New Schema", ()->{
                    if(autoSchemaList == null) autoSchemaList = getAutoModeSchemas(autoConfig);
                    if(autoSchemaList != null) {
                        // if the list is not null, lengthen it by one
                        X34Schema[] temp = new X34Schema[autoSchemaList.length + 1];
                        System.arraycopy(autoSchemaList, 0, temp, 0, autoSchemaList.length);
                        autoSchemaList = temp;
                    }else{
                        // otherwise, initialize it to a length of one
                        autoSchemaList = new X34Schema[1];
                    }
                    // get and add the new schema
                    autoSchemaList[autoSchemaList.length - 1] = getSchemaDetails(System.in, System.out);
                    System.out.println("Schema added!");
                    return true;
                }),

                new CLIMenuOption("Remove Schema", ()->{
                    if(autoSchemaList == null) autoSchemaList = getAutoModeSchemas(autoConfig);
                    if(autoSchemaList == null || autoSchemaList.length == 0){
                        System.out.println("Schema list is empty! Add some, you perv~!");
                        return true;
                    }

                    System.out.print("Enter the index of the entry to be removed:");
                    int removed = getIntFromScanner(new Scanner(System.in), System.out, "Invalid index! Please try again.", autoSchemaList.length);
                    System.out.println();

                    if(autoSchemaList.length == 1){
                        autoSchemaList = new X34Schema[0];
                    }else {
                        // bisect the array at the entry to be removed, and copy the two halves (not including the removed index) to the new array
                        int before = removed - 1;
                        int after = autoSchemaList.length - removed;
                        X34Schema[] temp = new X34Schema[autoSchemaList.length - 1];
                        if(before > 0) System.arraycopy(autoSchemaList, 0, temp, 0, before);
                        if(after > 0) System.arraycopy(autoSchemaList, removed, temp, removed - 1, after);
                        autoSchemaList = temp;
                    }

                    System.out.println("Schema removed!");
                    return true;
                }),

                new CLIMenuOption("Clear Schema List", ()->{
                    if(autoSchemaList == null) autoSchemaList = getAutoModeSchemas(autoConfig);
                    if(autoSchemaList == null || autoSchemaList.length == 0){
                        System.out.println("Schema list is already empty!");
                        return true;
                    }

                    System.out.print("Really clear schema list completely (y/n)?");
                    if(getBooleanFromScanner(new Scanner(System.in), System.out, "y", "n", "Please enter y for yes or n for no.")){
                        autoSchemaList = new X34Schema[0];
                        System.out.println("Schema list cleared.");
                    }else{
                        System.out.println("Clear operation aborted.");
                    }
                    return true;
                }),

                new CLIMenuOption("Return to Main Menu", () ->{
                    if(autoSchemaList != null){
                        System.out.print("Save changes? (y/n)");

                        if(getBooleanFromScanner(new Scanner(System.in), System.out, "y", "n", "Please enter y for yes or n for no.")){
                            if(saveAutoModeSchemas(autoConfig, autoSchemaList)){
                                System.out.println("Settings changes saved.");
                                // set the schema list to null to be sure that a current version is loaded when the menu is restarted
                                autoSchemaList = null;
                            }
                            else System.out.println("Error saving settings! Changes made may be lost."); // skip invalidating list, we'll try to use it as it is until the program shuts down
                        }else{
                            System.out.println("Discarding changes.");
                            autoSchemaList = null;
                        }
                    }
                    return false;
                })
        };

        CLIMenu auto = new CLIMenu(
                "This menu allows the addition, removal, and modification of\n" +
                        "automatic retrieval schemas. " +
                        "Please select an option from the list below:", "Enter an option: ", autoOptions);

        CLIMenuOption[] generalSettingsOptions = new CLIMenuOption[]{
                new CLIMenuOption("Show Current Settings", ()->{
                    // self-explanatory
                    if(generalArgList == null) generalArgList = getGeneralSettingsArgumentEquivalent(generalConfig);
                    if(generalArgList == null || generalArgList.length == 0){
                        generalArgList = new String[]{"mkdirs"};
                        System.out.println("No general config entries present, default settings loaded instead.");
                    }

                    System.out.println("General configuration settings are as follows:");
                    System.out.println();

                    System.out.println("Download Directory Creation: " + (ARKArrayUtil.containsString(generalArgList, "mkdirs") >= 0 ? "On" : "Off"));
                    System.out.println("Existing Image File Overwrite: " + (ARKArrayUtil.containsString(generalArgList, "overwrite") >= 0 ? "On" : "Off"));

                    if(ARKArrayUtil.containsString(generalArgList, "dest") >= 0){
                        System.out.println("Image Download Directory: " + CMLUtils.getArgument(generalArgList, "dest"));
                        System.out.println("(if listed directory path is incomplete, it is relative to the system desktop folder equivalent)");
                    }

                    // print other settings if there are any
                    boolean hasMoreSettings = false;
                    for(String s : generalArgList){
                        if(!s.contains("mkdirs") && !s.contains("overwrite") && !s.contains("dest=")){
                            if(!hasMoreSettings){
                                // only print the 'more settings' header if there actually ARE any more settings besides default ones
                                System.out.println();
                                System.out.println("Other settings are as follows:");
                                hasMoreSettings = true;
                            }
                            System.out.println(s);
                        }
                    }

                    System.out.println();
                    System.out.println("Enter any input to continue...");
                    // block until user enters something
                    getDataFromScanner(new Scanner(System.in), System.out, "");
                    return true;
                }),

                new CLIMenuOption("Edit/Add Setting Key", ()->{
                    if(generalArgList == null) generalArgList = getGeneralSettingsArgumentEquivalent(generalConfig);
                    if(generalArgList == null || generalArgList.length == 0){
                        generalArgList = new String[]{"mkdirs"};
                        System.out.println("No general config entries present, default settings loaded instead.");
                    }

                    System.out.print("Enter the key name to edit: ");
                    String key = getDataFromScanner(new Scanner(System.in), System.out, "");
                    int i = ARKArrayUtil.containsString(generalArgList, key);

                    if(i >= 0){
                        System.out.println("Key already exists, and will be overwritten with the new value.");
                        System.out.println("Current key value is: " + generalArgList[i]);
                    }else{
                        System.out.println("Key does not exist, and will be created.");
                    }

                    System.out.print("Enter the value for the key: ");
                    String value = getDataFromScanner(new Scanner(System.in), System.out, "");

                    if(i >= 0){
                        generalArgList[i] = key + "=" + value;
                    }else{
                        String[] temp = new String[generalArgList.length + 1];
                        System.arraycopy(generalArgList, 0, temp, 0, generalArgList.length);
                        temp[temp.length - 1] = key + "=" + value;
                        generalArgList = temp;
                    }

                    System.out.println("Settings updated.");

                    return true;
                }),

                new CLIMenuOption("Remove Setting Key", ()->{
                    if(generalArgList == null) generalArgList = getGeneralSettingsArgumentEquivalent(generalConfig);
                    if(generalArgList == null || generalArgList.length == 0){
                        generalArgList = new String[]{"mkdirs"};
                        System.out.println("No general config entries present, default settings loaded instead.");
                    }

                    System.out.print("Enter the key of the settings value to be removed: ");
                    String key = getDataFromScanner(new Scanner(System.in), System.out, "");
                    int removed = ARKArrayUtil.containsString(generalArgList, key);

                    if(removed < 0){
                        System.out.println("Specified settings value does not exist!");
                        return true;
                    }

                    System.out.println();

                    if(generalArgList.length == 1){
                        generalArgList = new String[0];
                    }else {
                        // bisect the array at the entry to be removed, and copy the two halves (not including the removed index) to the new array
                        int before = removed - 1;
                        int after = generalArgList.length - removed;
                        String[] temp = new String[generalArgList.length - 1];
                        if(before > 0) System.arraycopy(generalArgList, 0, temp, 0, before);
                        if(after > 0) System.arraycopy(generalArgList, removed, temp, removed - 1, after);
                        generalArgList = temp;
                    }

                    System.out.println("Settings value removed!");
                    return true;
                }),

                new CLIMenuOption("Clear Settings Index", ()->{
                    if(generalArgList == null) generalArgList = getGeneralSettingsArgumentEquivalent(generalConfig);
                    if(generalArgList == null || generalArgList.length == 0){
                        System.out.println("Settings index is already empty!");
                        return true;
                    }

                    System.out.print("Really clear settings index completely (y/n)?");
                    if(getBooleanFromScanner(new Scanner(System.in), System.out, "y", "n", "Please enter y for yes or n for no.")){
                        generalArgList = new String[0];
                        System.out.println("Settings index cleared.");
                    }else{
                        System.out.println("Clear operation aborted.");
                    }
                    return true;
                }),

                new CLIMenuOption("Return to Main Menu", ()->{
                    if(generalArgList != null && generalArgList.length > 0){
                        System.out.print("Save changes? (y/n)");

                        if(getBooleanFromScanner(new Scanner(System.in), System.out, "y", "n", "Please enter y for yes or n for no.")){
                            if(saveGeneralSettings(generalConfig, generalArgList)){
                                System.out.println("Settings changes saved.");
                                // set the schema list to null to be sure that a current version is loaded when the menu is restarted
                                generalArgList = null;
                            }
                            else System.out.println("Error saving settings! Changes made may be lost."); // skip invalidating list, we'll try to use it as it is until the program shuts down
                        }else{
                            System.out.println("Discarding changes.");
                            generalArgList = null;
                        }
                    }
                    return false;
                })
        };

        CLIMenu general = new CLIMenu(
                "This menu allows access to general program settings and configuration.\n" +
                        "Please select an option from the list below:", "Enter an option: ", generalSettingsOptions);

        CLIMenuOption[] mainOptions = new CLIMenuOption[]{
                new CLIMenuOption("Retrieve (manual)", ()-> {
                    batch(getGeneralSettingsArgumentEquivalent(generalConfig), getSchemaDetails(System.in, System.out));
                    return true;
                }),

                new CLIMenuOption("Retrieve (automatic)", ()->{
                    X34Schema[] autoSchemas = getAutoModeSchemas(autoConfig);
                    if(autoSchemas != null && autoSchemas.length > 0){
                        for(int i = 0; i < autoSchemas.length; i++){
                            System.out.println("Running retrieval process " + (i + 1) + " of " + autoSchemas.length + "...");
                            batch(getGeneralSettingsArgumentEquivalent(generalConfig), autoSchemas[i]);
                        }
                    }else{
                        System.out.println("No schemas available from automatic schema list!");
                        System.out.println("Add some in the auto-retrieval settings menu!");
                    }
                    return true;
                }),

                new CLIMenuOption("Configure Auto-Retrieval Settings", ()-> {
                    auto.display(System.out);
                    return true;
                }),

                new CLIMenuOption("General Settings", ()-> {
                    general.display(System.out);
                    return true;
                }),

                new CLIMenuOption("Exit Program", ()->{
                    log.disassociate();
                    System.exit(0);
                    return false;
                })
        };

        CLIMenu main = new CLIMenu(
                "Welcome to the ARK X34 Retrieval Utility.\n" +
                "Please select an option from the list below:", "Enter an option: ", mainOptions);

        main.display(System.out);
    }

    private static X34Schema getSchemaDetails(InputStream in, PrintStream out){
        Scanner input = new Scanner(in);
        input.useDelimiter(System.getProperty("line.separator"));
        String failText = "Invalid input! Please try again.";

        out.print("Enter tag/query: ");
        String query = getDataFromScanner(input, out, failText);
        out.print("Enter processor ID: ");
        String id = getDataFromScanner(input, out, failText);
        out.println();

        return new X34Schema(query, id, null);
    }

    static String getDataFromScanner(Scanner in, PrintStream out, String failText){
        String curr;
        do{
            if(in.hasNext()){
                curr = in.nextLine();
                if(curr.isEmpty()){
                    curr = null;
                    out.println(failText);
                }
            }else{
                curr = null;
            }
        }while (curr == null);
        return curr;
    }

    static int getIntFromScanner(Scanner in, PrintStream out, String failText, int max)
    {
        String curr;
        int res = -1;
        // cycle until a valid input has been received
        do{
            // if the input has a valid next field:
            if(in.hasNext()){
                curr = in.next();
                // if it's empty, tell the user and continue
                if(curr.isEmpty()){
                    out.println(failText);
                }else{
                    try{
                        // parse the input and see if it's valid and within the proper range
                        res = Integer.parseInt(curr);
                        if(res > max|| res < 0) throw new NumberFormatException("Outside of valid range");
                    }catch (NumberFormatException e){
                        // if the input wasn't a valid number, or was outside of the valid range, tell the user and continue
                        out.println(failText);
                        res = -1;
                    }
                }
            }
        }while (res < 0);

        return res;
    }

    static boolean getBooleanFromScanner(Scanner in, PrintStream out, String trueText, String falseText, String failText)
    {
        String curr;
        boolean res = false;
        // cycle until a valid input has been received
        do{
            // if the input has a valid next field:
            if(in.hasNext()){
                curr = in.next();
                // if it's empty, tell the user and continue
                if(curr.isEmpty()){
                    out.println(failText);
                }else{
                    // check if the input is equal to either valid input, break loop with appropriate result if it is
                    if(curr.equals(trueText)) res = true;
                    else if(curr.equals(falseText)) break;
                }
            }
        }while (!res);

        return res;
    }

    private static X34Schema[] getAutoModeSchemas(File source)
    {
        if(source == null || !source.exists() || source.length() == 0) return null;

        try{
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(source));
            X34Schema[] temp = (X34Schema[])is.readObject();
            is.close();
            return temp;
        } catch (IOException | ClassCastException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("WHAT DID YOU DO TO YOUR CLASSPATH?!");
            return null;
        }
    }

    private static boolean saveAutoModeSchemas(File dest, X34Schema[] schemas)
    {
        if(schemas == null) return true;
        try {
            if(dest == null || (dest.exists() && !dest.delete()) || (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) || !dest.createNewFile()) return false;

            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(dest));
            os.writeObject(schemas);
            os.flush();
            os.close();
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    private static String[] getGeneralSettingsArgumentEquivalent(File source)
    {
        if(source == null || !source.exists() || source.length() == 0) return null;

        try{
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(source));
            String[] temp = (String[])is.readObject();
            is.close();
            return temp;
        } catch (IOException | ClassCastException e) {
            e.printStackTrace();
            return null;
        } catch (ClassNotFoundException e) {
            System.out.println("WHAT DID YOU DO TO YOUR CLASSPATH?!");
            return null;
        }
    }

    private static boolean saveGeneralSettings(File dest, String[] args)
    {
        if(args == null) return true;
        try {
            if(dest == null || (dest.exists() && !dest.delete()) || (!dest.getParentFile().exists() && !dest.getParentFile().mkdirs()) || !dest.createNewFile()) return false;

            ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(dest));
            os.writeObject(args);
            os.flush();
            os.close();
            return true;
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
}

class CLIMenu
{
    private CLIMenuOption[] list;
    private Scanner in;
    private String header;
    private String prompt;
    private final String INVALID_INPUT_PROMPT = "Invalid input! Please try again.";

    public CLIMenu(String header, String prompt, CLIMenuOption... options)
    {
        this.list = options == null ? new CLIMenuOption[0] : options;
        this.in = new Scanner(System.in);
        this.header = header == null || header.isEmpty() ? "Select an option to continue:" : header;
        this.prompt = prompt == null || prompt.isEmpty() ? "Enter selection: " : prompt;
    }

    // blocks until user inputs valid response, returns response index
    private int displayListInternal(PrintStream out)
    {
        out.println(header);
        out.println();
        for(int i = 0; i < list.length; i++){
            out.println((i + 1) + ": " + list[i].name);
        }

        out.println();
        out.print(prompt);

        return X34CML.getIntFromScanner(in, out, INVALID_INPUT_PROMPT, list.length);
    }

    void display(PrintStream out)
    {
        if(out == null) throw new IllegalArgumentException("Output stream cannot be null");

        int selected = displayListInternal(out);

        // cycle back to the menu if submethod returns true
        if(list[selected - 1].action.action()){
            display(out);
        }
    }
}

class CLIMenuOption
{
    String name;
    CLIMenuAction action;

    CLIMenuOption(String name, CLIMenuAction action)
    {
        this.name = name == null ? "Option" : name;
        this.action = action;
    }
}

interface CLIMenuAction
{
    // returns true if the menu that called it should cycle around again instead of returning to previous menu
    boolean action();
}