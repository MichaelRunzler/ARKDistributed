import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

/**
 * Automatically checks a specified repository on the Rule 34 network and reports any changes.
 * Designed to be called from a command-line interface. Arguments are specified with the format "argumentName=value".
 * An example valid command line call is shown here:
 * java -jar R34.jar "tag=Tag Name" repo=0 "download=C:\Users\Username\Desktop\R34" pause summary
 *
 * Argument table is as follows:
 *
 * Arg 0: [String: tag] The tag to check for. The program will automatically correct for separator characters,
 * so just use spaces, i.e "Test Tag" instead of "Test_Tag" or "Test+Tag".
 * Arg 1 (Optional): [Int: repo default 0] The type of repository to check. 0 is all, 1 is just Paheal,
 * 2 is just the original Rule 34.
 * Arg 2 (Optional): [File/Dir: download default %UserHome%\Desktop] If you want to change the save directory for
 * downloaded images.
 * Arg 3 (Optional):  [null: pause]  If you want the program to wait for input before proceeding from the summary detail.
 * Arg 4 (Optional): [null: noIndex] If the program should only display results and not push changes to the index.
 * Arg 5 (Optional): [null: help] Displays the help text.
 * Arg 6 (Optional): [null: summary] Displays a summary of all stored index files after completing the pull.
 */
public class R34
{
    private static File index = new File(System.getenv("AppData") + "\\KAI\\ARK\\R34", "index.vcsi");
    private static final File database = new File(System.getenv("AppData") + "\\KAI\\ARK\\R34", "database.xml");
    private static File logFile;
    private static BufferedWriter logFileWriter;
    private static ArrayList<String> arguments;
    private static ArrayList<URL> loadedIndex;
    private static boolean doIndex = true;

    private static final int VERSION_NUMBER = 2261;

    public static void main(String[] args)
    {
        arguments = new ArrayList<>();

        String tag = "";
        int repoID = 0;
        File downloadDir = new File(System.getProperty("user.home") + "\\Desktop");
        String systemLocalizedTagID = "";
        boolean pause;
        boolean summary;

        if(args != null){
            if(args.length > 0){
                Collections.addAll(arguments, args);
            }else{
                System.out.println("No arguments!");
                exit();
            }
        }else{
            System.out.println("Argument list is null!");
            exit();
        }

        if(iterateArgumentListContentWildCard("help") > -1){
            System.out.print("Program Help Section:\n" +
                    "Automatically checks a specified repository on the Rule 34 network and reports any changes.\n" +
                    "Designed to be called from a command-line interface. Arguments are specified with the format \"argumentName=value\".\n" +
                    "An example valid command line call is shown here:\n" +
                    "\n" +
                    "java -jar R34.jar \"tag=Tag Name\" repo=0 \"download=C:\\Users\\Username\\Desktop\\R34\" pause summary\n" +
                    "\n" +
                    "Argument table is as follows:\n" +
                    "\n" +
                    " * Arg 0: [String: tag] The tag to check for. The program will automatically correct for separator characters, " +
                    "so just use spaces, i.e \"Test Tag\" instead of \"Test_Tag\" or \"Test+Tag\".\n" +
                    "\n" +
                    " * Arg 1 (Optional): [Integer: repo default 0] The type of repository to check. 0 is all, 1 is just Paheal, " +
                    "2 is just the original Rule 34.\n" +
                    "\n" +
                    " * Arg 2 (Optional): [Directory: download default %UserHome%\\Desktop] If you want to change the save directory for " +
                    "downloaded images.\n" +
                    "\n" +
                    " * Arg 3 (Optional):  [null: pause]  If you want the program to wait for input before proceeding from the summary detail.\n" +
                    "\n" +
                    " * Arg 4 (Optional): [null: noIndex] If the program should only display results and not push changes to the index.\n" +
                    "\n" +
                    " * Arg 5 (Optional): [null: help] Displays this help text.\n" +
                    "\n" +
                    " * Arg 6 (Optional): [null: summary] Displays a summary of all stored index files after completing the pull.\n" +
                    "\n");

            Scanner wait = new Scanner(System.in);
            logData("Input any character, then press 'Enter' to continue...");

            while (!wait.hasNext()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }

            exit();
        }

        if(iterateArgumentListContentWildCard("tag=") > -1){
            String str = arguments.get(iterateArgumentListContentWildCard("tag="));
            tag = str.substring(str.indexOf('=') + 1, str.length());
            tag = tag.replace(' ', '_');
            str = tag.replace('?', '_');
            str = str.replace('!', '_');
            str = str.replace('<', '_');
            str = str.replace('>', '_');
            str = str.replace('*', '_');
            str = str.replace('|', '_');
            str = str.replace('/', '_');
            str = str.replace('\\', '_');
            str = str.replace(':', '_');
            index = new File(index.getParentFile(), "index-" + str + ".vcsi");
            systemLocalizedTagID = str;
        }else{
            System.out.println("No tag argument!");
            exit();
        }

        if(iterateArgumentListContentWildCard("repo=") > -1){
            try{
                String str = arguments.get(iterateArgumentListContentWildCard("repo="));
                str = str.substring(str.length() - 1, str.length());
                repoID = Integer.parseInt(str);
                if(repoID > 2 || repoID < 0){
                    System.out.println("Repository argument must be either 0, 1, or 2!");
                    exit();
                }
            } catch(NumberFormatException e){
                System.out.println("Repository argument must be a number!");
                exit();
            }
        }

        logFile = new File(System.getProperty("user.home") + "\\.R34Logs", "R34Log-" + systemLocalizedTagID + "-" + repoID + ".vcsl");

        try {
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
            }else{
                logFile.delete();
            }

            logFile.createNewFile();
            logFileWriter = new BufferedWriter(new FileWriter(logFile));
        } catch(IOException e){
            e.printStackTrace();
            System.out.println("Couldn't create the system log file! The program will progress without logging data.");
        }

        logData("Starting...");

        if(iterateArgumentListContentWildCard("download=") > -1){
            String str = arguments.get(iterateArgumentListContentWildCard("download="));
            str = str.substring(str.indexOf('=') + 1, str.length());
            downloadDir = new File(str);
        }

        pause = iterateArgumentListContentWildCard("pause") > -1;

        doIndex = !(iterateArgumentListContentWildCard("noIndex") > -1);

        summary = iterateArgumentListContentWildCard("summary") > -1;

        logData("ARK Image Repository Retrieval Tool version " + VERSION_NUMBER + " started at UET+" + System.currentTimeMillis() + ".");
        logData("");
        logData("Current arguments:");
        logData("Tag: " + tag);
        logData("Repo ID: " + repoID);
        logData("Download Directory: " + downloadDir.getAbsolutePath());
        logData("Pause on Summary: " + pause);
        logData("Push Changes to Index: " + doIndex);
        logData("Display Index Summary: " + summary);
        logData("");

        logData("Checking a few things in AppData. Hold on a moment.");

        if(index.exists()){
            logData("Found an index file! Loading it now...");

            try {
                ObjectInputStream ois = new ObjectInputStream(new FileInputStream(index));
                loadedIndex = (ArrayList<URL>) ois.readObject();
                ois.close();

                logData("Loaded the index! *fist pump*");
            } catch (EOFException e){
                logData("Existing index file is empty! We'll write changes to it later.");
                loadedIndex = new ArrayList<>();
            } catch (IOException e) {
                if(e.getMessage() != null){
                    logData(e.getMessage());
                }else{
                    logData(e.toString());
                }
                logData("Couldn't load from the index file. Maybe it's corrupt? Try deleting it and try again!");
                exit();
            } catch (ClassNotFoundException e){
                if(e.getMessage() != null){
                    logData(e.getMessage());
                }else{
                    logData(e.toString());
                }
                logData("OK, what did you do to your Java class libraries? One of the core classes is missing!\n" +
                        "Try re-downloading this software or your JRE/JDK and try again!");
                exit();
            }
        }else{
            logData("Just letting you know, I didn't find an index file in the default directory.\n" +
                    "If that's okay, you don't need to worry about this message.\n" +
                    "If not, you may want to check your index storage directory.");
            index.getParentFile().mkdirs();
            try {
                index.createNewFile();
                loadedIndex = new ArrayList<>();
                logData("Created the new index file!");
            } catch (IOException e) {
                if(e.getMessage() != null){
                    logData(e.getMessage());
                }else{
                    logData(e.toString());
                }
                logData("Couldn't create the index file! Try checking your index directory.");
                exit();
            }
        }

        logData("\nStarting retrieval. Hang on tight, this might get rough...");

        ArrayList<Integer> newImageIDs;

        if(database.exists() && database.delete()){
            logData("Deleted old database file, just like I promised!");
        }
        try {
            database.createNewFile();
            logData("Wrote database file placeholder!");
        } catch (IOException e) {
            if(e.getMessage() != null){
                logData(e.getMessage());
            }else{
                logData(e.toString());
            }
            logData("Couldn't write the temporary database file! Restart the application and try again!");
            exit();
        }

        if(repoID == 0){
            ArrayList<Integer> ni1 = retrieveR34P(tag);
            ArrayList<Integer> ni2 = retrieveR34(tag);

            newImageIDs = new ArrayList<>();
            newImageIDs.addAll(ni1);
            newImageIDs.addAll(ni2);
        }else if(repoID == 1){
            newImageIDs = retrieveR34P(tag);
        }else{
            newImageIDs = retrieveR34(tag);
        }

        int newImageCount = newImageIDs.size();

        logData("OK, now that that's over with, here's the report:");
        logData("");
        logData("Tag searched: " + tag);
        logData("Search type: " + repoID);
        logData("Total images in index: " + loadedIndex.size());
        logData("New images scanned to index: " + newImageCount);
        logData("");

        if(pause)
        {
            Scanner wait = new Scanner(System.in);
            logData("Press any key, then press 'Enter' to continue...");

            while (!wait.hasNext()) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }
            }
        }

        if(newImageCount > 0)
        {
            logData("Looks like you're in luck! Download the new images? (Y/N)");
            Scanner in = new Scanner(System.in);

            if(in.hasNext() && in.next().toLowerCase().equals("y"))
            {
                logData("OK, downloading the images now. Just a minute (or more, if your internet is crap)...");

                downloadDir.mkdirs();
                for(int i= 0; i < newImageIDs.size(); i++)
                {
                    int j = newImageIDs.get(i);
                    logData("Downloading image " + (i + 1) + " of " + newImageCount + "...");
                    try {
                        String str = loadedIndex.get(j).getPath();
                        File imgF = new File(downloadDir, str.substring(str.lastIndexOf("/") + 1, str.length()));

                        if(imgF.getAbsolutePath().length() >= 255)
                        {
                            logData("The combined file descriptor length of your image is too long! Attempting to truncate...");
                            int pathLen = imgF.getParentFile().getAbsolutePath().length();

                            if(pathLen >= 250) {
                                logData("The path to your download directory is too long! Please choose another directory and restart.");
                                logData("Skipped image URL:" + loadedIndex.get(j).getPath());
                                exit();
                            }else{
                                imgF = new File(imgF.getParentFile(), imgF.getName().substring(imgF.getName().length() - (255 - pathLen), imgF.getName().length()));
                                logData("Truncated to " + imgF.getName() + " sucessfully!");
                            }
                        }

                        if(!imgF.exists()) {
                            BufferedImage img;
                            img = ImageIO.read(loadedIndex.get(j));
                            ImageIO.write(img, str.substring(str.lastIndexOf('.') + 1, str.length()), imgF);
                        }else{
                            logData("Image already exists! Skipping.");
                        }
                    } catch (IOException e) {
                        if(e.getMessage() != null){
                            logData(e.getMessage());
                        }else{
                            logData(e.toString());
                        }
                        logData("Failed to download image due to some sort of IO error! Trying the rest anyway...");
                        logData("Skipped image URL:" + loadedIndex.get(j).getPath());
                    } catch (IllegalArgumentException e){
                        if(e.getMessage() != null){
                            logData(e.getMessage());
                        }else{
                            logData(e.toString());
                        }
                        logData("JPEG ICC ColorSpace profile did not compute properly!");
                        logData("Skipped image URL:" + loadedIndex.get(j).getPath());
                    }
                }
                logData("Looks like that's all of 'em! Enjoy!");
            }
        }else{
            logData("Looks like you're out of luck (and fap material)! Better luck next time, eh?");
        }

        if(summary)
        {
            logData("The following is a summary of your stored indexes:");
            logData("");

            File[] indices = index.getParentFile().listFiles();

            if(indices != null && indices.length > 0) {
                File[] cpy = new File[indices.length];
                int j = 0;
                for(int i = 0; i < indices.length; i++){
                    if(indices[i].getName().contains(".vcsi")){
                        cpy[j] = indices[i];
                        j++;
                    }
                }

                indices = cpy;

                for (int i = 0; i < j; i++)
                {
                    File f = indices[i];

                    try {
                        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
                        ArrayList<URL> temp = (ArrayList<URL>) ois.readObject();
                        ois.close();

                        logData("Index " + (i + 1) + " of " + j + ":");
                        logData("Filename: " + f.getName());
                        logData("Tag: " + f.getName().substring(f.getName().indexOf('-') + 1, f.getName().lastIndexOf('.')));
                        logData("Size on Disk: " + (double)f.length() / 1000 + " KB");
                        logData("No. of Entries: " + temp.size());
                        logData("");

                        Scanner wait = new Scanner(System.in);
                        logData("Input any character, then press 'Enter' to continue...");

                        while (!wait.hasNext()) {
                            try {
                                Thread.sleep(50);
                            } catch (InterruptedException e) {
                            }
                        }

                        logData("");
                    } catch (IOException | ClassNotFoundException e) {
                        logData("Couldn't load an index! Trying the rest anyway.");
                    }
                }
            }else{
                logData("Found no indexes in the default directory! Skipping summary...");
                logData("");
            }
        }

        logData("Don't go yet, Senpai! Cleaning up a few things (just like you will have to later...)");

        logData("Updating local index file...");

        try {
            if(index.exists()){
                index.delete();
            }
            index.createNewFile();

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(index));
            oos.writeObject(loadedIndex);
            oos.flush();
            oos.close();
            logData("Updated!");
        } catch (IOException e) {
            if(e.getMessage() != null){
                logData(e.getMessage());
            }else{
                logData(e.toString());
            }
            logData("Couldn't write index to disk! Check your index directory and try again!");
        }

        logData("Deleting local database copy...");

        if(database.delete()){
            logData("Deleted!");
        }else{
            logData("Couldn't get it! You can try deleting it manually, or I'll do it on the next program run. Your choice!");
        }

        logData("All done! See you next time, ya pervert! \\(^w^)/");

        exit();
    }

    private static ArrayList<Integer> retrieveR34P(String tag)
    {
        try {
            logData("Starting retrieval from Paheal repository...");
            logData("Getting page count...");

            //initialization phase
            URL tagURL = new URL("http://rule34.paheal.net/post/list/" + tag + "/1");
            ReadableByteChannel rbc = Channels.newChannel(tagURL.openStream());
            FileOutputStream fos = new FileOutputStream(database);
            FileReader reader = new FileReader(database);
            char[] cbuf = new char[100000];
            ArrayList<URL> images = new ArrayList<>();
            ArrayList<Integer> indexes = new ArrayList<>();

            //initial transfer phase
            fos.getChannel().transferFrom(rbc, 0, Integer.MAX_VALUE);
            fos.flush();
            fos.close();

            //page locator phase
            logData(reader.read(cbuf) + " characters read into buffer...");
            String db = new String(cbuf);
            reader.close();

            int lastPageID = 1;

            if(db.contains("Last</a>")) {
                try {
                    lastPageID = Integer.parseInt(db.substring(db.indexOf("Last</a>") - 5, db.indexOf("Last</a>") - 2));
                }catch (NumberFormatException e) {
                    try{
                        lastPageID = Integer.parseInt(db.substring(db.indexOf("Last</a>") - 4, db.indexOf("Last</a>") - 2));
                    }catch (NumberFormatException e1){
                        lastPageID = Integer.parseInt(db.substring(db.indexOf("Last</a>") - 3, db.indexOf("Last</a>") - 2));
                    }
                }
            }
            int i = 1;
            logData("Got page count as " + lastPageID);

            //retrieval phase
            do{
                //parse subphase
                if(lastPageID >= 50 && (double)i % 50.0 == 0){
                    logData("Sleeping for 4s to avoid overloading the server...");
                    Thread.sleep(4000);
                    logData("Done, continuing.");
                }

                logData("Pulling image URLs from page " + i + " of " + lastPageID + "...");

                int lastChar = 0;
                int parityCheck = 0;
                int lc2 = 0;
                int newCount = 0;
                while(parityCheck > -1){
                    lastChar = db.indexOf("/_images/", lastChar);
                    parityCheck = lastChar;
                    lastChar = lastChar + 9;
                    lc2 = db.indexOf(">Image Only", lc2) + 11;
                    if(parityCheck > -1){
                        images.add(new URL("http://rule34-data-" + db.substring(lastChar - 23, lc2 - 12)));
                        newCount ++;
                    }
                }

                logData("Pull complete! Pulled " + newCount + " images.");

                i++;

                //cleanup subphase
                if(i <= lastPageID) {
                    database.delete();
                    database.createNewFile();

                    //reset subphase
                    tagURL = new URL("http://rule34.paheal.net/post/list/" + tag + "/" + i);
                    rbc = Channels.newChannel(tagURL.openStream());
                    fos = new FileOutputStream(database);
                    reader = new FileReader(database);
                    cbuf = new char[100000];

                    //transfer subphase
                    fos.getChannel().transferFrom(rbc, 0, Integer.MAX_VALUE);
                    fos.flush();
                    fos.close();

                    //read subphase
                    logData(reader.read(cbuf) + " characters read into buffer...");
                    db = new String(cbuf);
                    reader.close();
                }

            }while(i <= lastPageID);

            //check phase
            if(doIndex)
            {
                logData("Querying local image index...");
                for (URL url : images) {
                    if (!loadedIndex.contains(url)) {
                        loadedIndex.add(url);
                        indexes.add(loadedIndex.indexOf(url));
                    }
                }
            }else{
                logData("Index update skipped as per configuration settings.");
            }

            logData("Retrieval complete with " + indexes.size() + " new images!");

            return indexes;
        } catch (IOException e) {
            if(e.getMessage() != null){
                logData(e.getMessage());
            }else{
                logData(e.toString());
            }
            logData("IO exception while running initial database read!");
            exit();
        } catch (NumberFormatException e){
            if(e.getMessage() != null){
                logData(e.getMessage());
            }else{
                logData(e.toString());
            }
            logData("Couldn't obtain page number index!");
            exit();
        } catch (InterruptedException e) {
        }
        return null;
    }

    private static ArrayList<Integer> retrieveR34(String tag)
    {
        try {
            logData("Starting retrieval from legacy repository...");
            logData("Getting total post count...");

            //initialization phase
            URL tagURL = new URL("http://rule34.xxx/index.php?page=dapi&s=post&q=index&limit=100&tags=" + tag + "&pid=0");
            ReadableByteChannel rbc = Channels.newChannel(tagURL.openStream());
            FileOutputStream fos = new FileOutputStream(database);
            FileReader reader = new FileReader(database);
            char[] cbuf = new char[100000];
            ArrayList<URL> images = new ArrayList<>();
            ArrayList<Integer> indexes = new ArrayList<>();

            //initial transfer phase
            fos.getChannel().transferFrom(rbc, 0, Integer.MAX_VALUE);
            fos.flush();
            fos.close();

            //page locator phase
            logData(reader.read(cbuf) + " characters read into buffer...");
            String db = new String(cbuf);
            reader.close();

            int postCount = Integer.parseInt(db.substring(db.indexOf("<posts count=\"") + 14, db.indexOf("\" offset=")));
            int lastPageID = (int)Math.ceil((double)postCount / 100);
            int i = 1;
            logData("Got total post count as " + postCount);
            logData("Will query a total of " + lastPageID + " times.");

            //retrieval phase
            do{
                //parse subphase
                logData("Pulling image URLs from page " + i + " of " + lastPageID + "...");

                int lastChar = 0;
                int lc2 = 0;
                int parityCheck = 0;
                int newCount = 0;
                while(parityCheck > -1){
                    lastChar = db.indexOf("file_url=\"", lastChar);
                    parityCheck = lastChar;
                    lastChar = lastChar + 10;
                    lc2 = db.indexOf("\" parent_id=", lc2) + 12;
                    if(parityCheck > -1){
                        images.add(new URL("http:" + db.substring(lastChar, lc2 - 12)));
                        newCount ++;
                    }
                }

                logData("Pull complete! Pulled " + newCount + " images.");

                i++;

                //cleanup subphase
                if(i <= lastPageID) {
                    database.delete();
                    database.createNewFile();

                    //reset subphase
                    tagURL = new URL("http://rule34.xxx/index.php?page=dapi&s=post&q=index&limit=100&tags=" + tag + "&pid=" + (i - 1));
                    rbc = Channels.newChannel(tagURL.openStream());
                    fos = new FileOutputStream(database);
                    reader = new FileReader(database);
                    cbuf = new char[100000];

                    //transfer subphase
                    fos.getChannel().transferFrom(rbc, 0, Integer.MAX_VALUE);
                    fos.flush();
                    fos.close();

                    //read subphase
                    logData(reader.read(cbuf) + " characters read into buffer...");
                    db = new String(cbuf);
                    reader.close();
                }

            }while(i <= lastPageID);

            //check phase
            if(doIndex)
            {
                logData("Querying local image index...");
                for (URL url : images) {
                    if (!loadedIndex.contains(url)) {
                        loadedIndex.add(url);
                        indexes.add(loadedIndex.indexOf(url));
                    }
                }
            }else{
                logData("Index update skipped as per configuration settings.");
            }

            logData("Retrieval complete with " + indexes.size() + " new images!");

            return indexes;
        } catch (IOException e) {
            if(e.getMessage() != null){
                logData(e.getMessage());
            }else{
                logData(e.toString());
            }
            logData("IO exception while running initial database read!");
            exit();
        } catch (NumberFormatException e){
            if(e.getMessage() != null){
                logData(e.getMessage());
            }else{
                logData(e.toString());
            }
            logData("Couldn't obtain page number index!");
            exit();
        }
        return null;
    }

    /**
     * Logs the specified text to the console window and the log file simultaneously.
     * If the log file creation failed for some reason, it will only log to the console window.
     * @param data the text data to log and display
     */
    private static void logData(String data)
    {
        System.out.println(data);
        if(logFileWriter != null) {
            try {
                logFileWriter.write(data);
                logFileWriter.newLine();
                logFileWriter.flush();
            } catch (IOException e) {
                if(e.getMessage() != null){
                    System.out.println(e.getMessage());
                }else{
                    System.out.println(e.toString());
                }
            }
        }
    }

    /**
     * Iterates through the contents of the internal argument list, searching for an entry that contains the given string.
     * @param content a String to search for in the argument list. Note that any given array entry only has to <u>contain</u> the value,
     *                not equal it.
     * @return the index of the array cell containing the given string
     */
    private static int iterateArgumentListContentWildCard(String content)
    {
        for(int i = 0; i < arguments.size(); i++){
            String str = arguments.get(i);

            if(str.contains(content)){
                return i;
            }
        }
        return -1;
    }

    /**
     * Performs shutdown tasks and exits the program.
     */
    private static void exit()
    {
        logData("Exiting program...");
        logData("Closing logfile writer...");
        if(logFileWriter != null) {
            try {
                logFileWriter.close();
            } catch (IOException e) {
                if (e.getMessage() != null) {
                    logData(e.getMessage());
                } else {
                    logData(e.toString());
                }
            }
        }
        System.exit(0);
    }
}
