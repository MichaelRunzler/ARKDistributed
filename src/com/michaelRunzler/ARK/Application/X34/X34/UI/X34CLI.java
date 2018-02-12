package X34.UI;

import X34.Core.*;
import X34.Processors.X34ProcessorRegistry;
import core.AUNIL.XLoggerInterpreter;
import core.CoreUtil.ARKArrayUtil;
import core.CoreUtil.CMLUtils;
import core.system.ARKGlobalConstants;

import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class X34CLI
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
            "* (optional)[void]confirmdl: if present, user confirmation will be required before downloading any found images.\n"+
            "* \n" +
            "* \n" +
            "* Debug/info commands:\n" +
            "* \n" +
            "* (optional)[void]help: displays this help block\n" +
            "* (optional)[void]processors: displays the list of current retrieval processor IDs and their names\n" +
            "* \n" +
            "* \n" +
            "* Example command-line call:\n" +
            "* java -jar X34CLI.jar \"tag=elite_dangerous\" \"repo=R34X\" \"dest=\\images\" overwrite mkdirs";

    private static XLoggerInterpreter log;
    private static final File autoConfig = new File(ARKGlobalConstants.DESKTOP_DATA_ROOT.getAbsolutePath() + "\\X34", "CMLAutoCfg.x34c");
    private static final File generalConfig = new File(ARKGlobalConstants.DESKTOP_DATA_ROOT.getAbsolutePath() + "\\X34", "CMLGeneralCfg.x34c");

    private static X34Rule[] autoRuleList = null;
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
     *         <li>(optional)[void]confirmdl: if present, user confirmation will be required before downloading any found images.</li>
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
            displayAvailableProcessors();
            System.exit(0);
        }

        log = new XLoggerInterpreter("X34 CLI");

        // delegate to submethod based on whether batch job keys are in place
        if(CMLUtils.getArgument(args, "tag") == null && CMLUtils.getArgument(args, "repo") == null) CLI();
        else{
            X34Rule rule = new X34Rule(CMLUtils.getArgument(args, "tag"), null, CMLUtils.getArgument(args, "repo"));
            batch(args, rule);
        }

        log.disassociate();
        System.exit(0);
    }

    /**
     * Runs the non-interactive batch job version of this application.
     * Typically called if any of the command-line arguments are in place.
     * Also called from the CLI for its retrieval branch.
     * @param args the argument list form {@link X34CLI#main(String[])}
     * @param rule a valid {@link X34Schema} to be used for query and processor data, as well as metadata tags
     */
    private static void batch(String[] args, X34Rule rule)
    {
        //
        // check main args
        //

        if(!rule.validate()){
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
        boolean confirmDownload = CMLUtils.getArgument(args, "confirmdl") != null;

        //
        // init core and initiate retrieval
        //

        log.logEvent("Initialization complete.");
        log.logEvent("Schema validation passed.");

        X34Core xCore = new X34Core();

        log.logEvent("Retrieving...");

        ArrayList<X34Image> retrieved = null;
        try {
            retrieved = xCore.retrieve(rule);
            log.logEvent("Retrieval successful.");
        } catch (ValidationException e) {
            log.logEvent("Retrieval failed with the following exception:");
            log.logEvent(e);
        }

        if(retrieved != null && retrieved.size() > 0)
        {
            if(confirmDownload) System.out.println("Download " + retrieved.size() + " new image" + (retrieved.size() == 1 ? "" : "s") + "?");

            if(!confirmDownload || getBooleanFromScanner(new Scanner(System.in), System.out, "y", "n", "Enter y for yes or n for no.")) {
                try {
                    xCore.writeImagesToFile(retrieved, root, overwrite, mkdirs);
                    log.logEvent("Download complete.");
                } catch (IOException e) {
                    log.logEvent("Image download failed with the following exception:");
                    log.logEvent(e);
                }
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
                new CLIMenuOption("List Current Rules", ()->{
                    // self-explanatory
                    if(autoRuleList == null) autoRuleList = getAutoModeSchemas(autoConfig);
                    if(autoRuleList == null || autoRuleList.length == 0){
                        System.out.println("Rule list is empty. Add some, you perv~!");
                    }else{
                        System.out.println("Current auto rule list is as follows.");
                        System.out.println("RID is the index of the rule, SID is the ID of the schema within the rule.");
                        System.out.println();
                        System.out.println("RID-SID. PID : Query/Tag");
                        System.out.println();
                        for(int i = 0; i < autoRuleList.length; i++) {
                            X34Rule r = autoRuleList[i];
                            X34Schema[] schemas = r.getSchemas();
                            for (int j = 0; j < schemas.length; j++) {
                                System.out.println((i + 1) + "-" + (j + 1) + ". " + schemas[j].type + " : " + schemas[j].query);
                            }
                        }
                    }

                    System.out.println();
                    System.out.println("Enter any input to continue...");
                    // block until user enters something
                    getDataFromScanner(new Scanner(System.in), System.out, "");
                    return true;
                }),

                new CLIMenuOption("View Available Processor IDs", ()->{
                    displayAvailableProcessors();
                   return true;
                }),

                new CLIMenuOption("Add New Rule", ()->{
                    if(autoRuleList == null) autoRuleList = getAutoModeSchemas(autoConfig);
                    if(autoRuleList != null) {
                        // if the list is not null, lengthen it by one
                        X34Rule[] temp = new X34Rule[autoRuleList.length + 1];
                        System.arraycopy(autoRuleList, 0, temp, 0, autoRuleList.length);
                        autoRuleList = temp;
                    }else{
                        // otherwise, initialize it to a length of one
                        autoRuleList = new X34Rule[1];
                    }
                    // get and add the new schema
                    autoRuleList[autoRuleList.length - 1] = getRuleDetails(System.in, System.out);
                    System.out.println("Schema added!");
                    return true;
                }),

                new CLIMenuOption("Remove Rule", ()->{
                    if(autoRuleList == null) autoRuleList = getAutoModeSchemas(autoConfig);
                    if(autoRuleList == null || autoRuleList.length == 0){
                        System.out.println("Rule list is empty! Add some, you perv~!");
                        return true;
                    }

                    System.out.println("Enter the index of a rule in the list.");
                    System.out.println("Please note that this will remove all sub-schemas within this rule.");
                    System.out.print("Enter the index of the rule to be removed.");
                    int removed = getIntFromScanner(new Scanner(System.in), System.out, "Invalid index! Please try again.", autoRuleList.length);
                    System.out.println();

                    if(autoRuleList.length == 1){
                        autoRuleList = new X34Rule[0];
                    }else {
                        // bisect the array at the entry to be removed, and copy the two halves (not including the removed index) to the new array
                        int before = removed - 1;
                        int after = autoRuleList.length - removed;
                        X34Rule[] temp = new X34Rule[autoRuleList.length - 1];
                        if(before > 0) System.arraycopy(autoRuleList, 0, temp, 0, before);
                        if(after > 0) System.arraycopy(autoRuleList, removed, temp, removed - 1, after);
                        autoRuleList = temp;
                    }

                    System.out.println("Rule removed!");
                    return true;
                }),

                new CLIMenuOption("Clear Rule List", ()->{
                    if(autoRuleList == null) autoRuleList = getAutoModeSchemas(autoConfig);
                    if(autoRuleList == null || autoRuleList.length == 0){
                        System.out.println("Rule list is already empty!");
                        return true;
                    }

                    System.out.print("Really clear rule list completely (y/n)?");
                    if(getBooleanFromScanner(new Scanner(System.in), System.out, "y", "n", "Please enter y for yes or n for no.")){
                        autoRuleList = new X34Rule[0];
                        System.out.println("Rule list cleared.");
                    }else{
                        System.out.println("Clear operation aborted.");
                    }
                    return true;
                }),

                new CLIMenuOption("Return to Main Menu", () ->{
                    if(autoRuleList != null){
                        System.out.print("Save changes? (y/n)");

                        if(getBooleanFromScanner(new Scanner(System.in), System.out, "y", "n", "Please enter y for yes or n for no.")){
                            if(saveAutoModeSchemas(autoConfig, autoRuleList)){
                                System.out.println("Settings changes saved.");
                                // set the schema list to null to be sure that a current version is loaded when the menu is restarted
                                autoRuleList = null;
                            }
                            else System.out.println("Error saving settings! Changes made may be lost."); // skip invalidating list, we'll try to use it as it is until the program shuts down
                        }else{
                            System.out.println("Discarding changes.");
                            autoRuleList = null;
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
                        if(!s.contains("mkdirs") && !s.contains("overwrite") && !s.contains("dest=") && !s.contains("confirmdl")){
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
                        int after = generalArgList.length - removed - 1;
                        String[] temp = new String[generalArgList.length - 1];
                        if(removed > 0) System.arraycopy(generalArgList, 0, temp, 0, removed);
                        if(after > 0) System.arraycopy(generalArgList, removed + 1, temp, removed, after);
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
                    batch(getGeneralSettingsArgumentEquivalent(generalConfig), getRuleDetails(System.in, System.out));
                    return true;
                }),

                new CLIMenuOption("Retrieve (automatic)", ()->{
                    X34Rule[] autoRules = getAutoModeSchemas(autoConfig);
                    if(autoRules != null && autoRules.length > 0){
                        for(int i = 0; i < autoRules.length; i++){
                            System.out.println("Running retrieval process " + (i + 1) + " of " + autoRules.length + "...");
                            batch(getGeneralSettingsArgumentEquivalent(generalConfig), autoRules[i]);
                        }
                    }else{
                        System.out.println("No rules available from automatic rule list!");
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

    private static X34Rule getRuleDetails(InputStream in, PrintStream out)
    {
        Scanner input = new Scanner(in);
        input.useDelimiter(System.getProperty("line.separator"));
        String failText = "Invalid input! Please try again.";

        out.print("Enter tag/query: ");
        String query = getDataFromScanner(input, out, failText);
        out.println("Enter processor ID(s). When done, enter an invalid processor ID, or 'done' (no quotes). ");
        out.println("At least one valid processor ID must be entered to continue.");
        out.println("Enter ID(s):");
        ArrayList<String> ids = new ArrayList<>();
        String recent;
        boolean done = false;
        do{
            recent = getDataFromScanner(input, out, "Please enter a processor ID or 'done'.");
            if(!recent.toLowerCase().equals("done") && X34ProcessorRegistry.getProcessorForID(recent) != null) ids.add(recent);
            else if(ids.size() > 0) done = true;
            else out.println("Please enter at least one valid processor ID.");
        }while (!done);
        out.println();

        return new X34Rule(query, null, ids.toArray(new String[ids.size()]));
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

    private static X34Rule[] getAutoModeSchemas(File source)
    {
        if(source == null || !source.exists() || source.length() == 0) return null;

        try{
            ObjectInputStream is = new ObjectInputStream(new FileInputStream(source));
            X34Rule[] temp = (X34Rule[])is.readObject();
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

    private static boolean saveAutoModeSchemas(File dest, X34Rule[] schemas)
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
            // check for the DL confirmation setting, if it isn't there, add it.
            if(ARKArrayUtil.containsString(temp, "confirmdl") < 0) {
                String[] temp2 = new String[temp.length + 1];
                System.arraycopy(temp, 0, temp2, 0, temp.length);
                temp2[temp2.length - 1] = "confirmdl";
                is.close();
                return temp2;
            }else{
                is.close();
                return temp;
            }
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

    private static void displayAvailableProcessors()
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
            System.out.println();
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
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

        return X34CLI.getIntFromScanner(in, out, INVALID_INPUT_PROMPT, list.length);
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