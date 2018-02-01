package X34.UI;

import X34.Core.X34Core;
import X34.Core.X34Image;
import X34.Core.X34Schema;
import X34.Processors.X34ProcessorRegistry;
import core.AUNIL.XLoggerInterpreter;
import core.CoreUtil.CMLUtils;
import core.system.ARKGlobalConstants;

import javax.xml.bind.ValidationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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

        // delegate to submethod based on whether batch job keys are in place
        if(CMLUtils.getArgument(args, "tag") == null && CMLUtils.getArgument(args, "repo") == null) CML(args);
        else batch(args);
    }

    /**
     * Runs the non-interactive batch job version of this application.
     * Typically called if any of the command-line arguments are in place.
     * @param args the argument list form {@link X34CML#main(String[])}
     */
    private static void batch(String[] args)
    {
        //
        // check main args
        //

        X34Schema schema = new X34Schema(CMLUtils.getArgument(args, "tag"), CMLUtils.getArgument(args, "repo"), null);

        if(!schema.validate()){
            System.out.println("One or more arguments are missing!");
            System.exit(0);
        }

        // compute download root directory location
        String temp = CMLUtils.getArgument(args, "dest");
        File root;
        if(temp.isEmpty()) root = ARKGlobalConstants.getOSSpecficDesktopRoot();
        else if(temp.startsWith("\\")) root = new File(ARKGlobalConstants.getOSSpecficDesktopRoot().getAbsolutePath() + temp);
        else root = new File(temp);

        // set directory creation and overwrite flags
        boolean overwrite = CMLUtils.getArgument(args, "overwrite") != null;
        boolean mkdirs = CMLUtils.getArgument(args, "mkdirs") != null;

        //
        // init core and initiate retrieval
        //

        XLoggerInterpreter log = new XLoggerInterpreter("X34 CML Interface");

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

        log.logEvent("Job complete, shutting down.");
        log.disassociate();
        System.exit(0);
    }

    /**
     * Runs the interactive CLI version of this application.
     * Typically called if the program is started with no arguments.
     * @param args the argument list from {@link X34CML#main(String[])}
     */
    private static void CML(String[] args)
    {
        //todo complete
    }
}