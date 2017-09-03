import system.ARKGlobalConstants;

import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

public class RPlanner
{
    private static ArrayList<Plan> index = new ArrayList<>();
    private static final File MASTER_INDEX = new File(ARKGlobalConstants.DESKTOP_DATA_ROOT.getAbsolutePath() + "\\Config", "RPlannerConfig.vcss");
    private static final File REFERENCE_INDEX = new File(ARKGlobalConstants.DESKTOP_DATA_ROOT.getAbsolutePath() + "\\Config", "RPLannerReference.vcss");
    private static final Scanner INPUT = new Scanner(System.in);
    private static final Random GENERATOR = new Random();

    public static void main(String[] args)
    {
        // Read from master index file if it exists.
        if(MASTER_INDEX.exists()){
            try {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(MASTER_INDEX));
                Object tmp = in.readObject();
                index = tmp instanceof ArrayList ? ((ArrayList<Plan>) tmp) : new ArrayList<>();
            } catch (IOException | ClassNotFoundException e) {
                index = new ArrayList<>();
                System.out.println("Failed to load from the index file!");
            }
        }

        // Display main menu.
        boolean doExit = false;
        do {
            System.out.println();
            System.out.println("-- MAIN MENU --");
            System.out.println();
            System.out.println("1. Generate List");
            System.out.println("2. Manage Plan/Component Index");
            System.out.println("3. Exit");
            System.out.println();
            System.out.print("Enter an option to continue: ");

            if (INPUT.hasNextInt()) {
                switch (INPUT.nextInt()) {
                    case 1:
                        generate();
                        break;
                    case 2:
                        manageIndex(index);
                        break;
                    case 3:
                        System.out.println("Exiting...");
                        doExit = true;
                        break;
                }
            }
        }while(!doExit);

        // Refresh the on-disk index.
        if(!MASTER_INDEX.exists()) MASTER_INDEX.getParentFile().mkdirs(); else MASTER_INDEX.delete();

        // Write index changes to disk.
        System.out.println("Saving index changes to disk...");
        try {
            MASTER_INDEX.createNewFile();
            ObjectOutputStream output = new ObjectOutputStream(new FileOutputStream(MASTER_INDEX));
            output.writeObject(index);
            System.out.println("Saved!");
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Goodbye.");
    }

    private static void manageIndex(ArrayList<Plan> index)
    {
        boolean doExit = false;
        do {
            System.out.println();
            System.out.println("-- INDEX MANAGEMENT --");
            System.out.println();
            System.out.println("Choose an option to continue:");
            System.out.println("");
            System.out.println("1. View Index Contents");
            System.out.println("2. Add Entry to Index");
            System.out.println("3. Remove Entry from Index");
            System.out.println("4. Manage an entry's Recipe Components");
            System.out.println("5. Load Index Entries from File");
            System.out.println("6. Clear Index");
            System.out.println("7. Write index to CSV");
            System.out.println("8. Return to Main Menu");
            System.out.println();
            System.out.print("Enter selection: ");

            if (INPUT.hasNextInt()) {
                System.out.println();
                switch (INPUT.nextInt()) {
                    case 1:
                        if (index.size() <= 0) {
                            System.out.println("No entries to display!");
                            break;
                        }
                        System.out.println("Entries are as follows:");
                        System.out.println();

                        for (int i = 0; i < index.size(); i++) {
                            System.out.println((i + 1) + ". " + index.get(i).getName());
                        }

                        System.out.println();
                        System.out.println("End of entries.");
                        break;
                    case 2:
                        String value = "";
                        do {
                            INPUT.useDelimiter(System.getProperty("line.separator"));
                            System.out.print("Enter the name of the new entry: ");
                            if(INPUT.hasNext()){
                                value = INPUT.next();
                            }
                        }while(value.equals(""));

                        ArrayList<String> values = new ArrayList<>();
                        do {
                            System.out.println("Enter the recipe components of this entry. Enter 'done' when done: ");
                            System.out.print("> ");
                            while(INPUT.hasNext()){
                                String tmp = INPUT.next();
                                if(tmp.equals("done")){
                                    break;
                                }
                                values.add(tmp);
                                System.out.print("> ");
                            }
                        }while(values.size() == 0);

                        String classValue = "";
                        do {
                            INPUT.useDelimiter(System.getProperty("line.separator"));
                            System.out.print("Enter the item class of the new entry: ");
                            if(INPUT.hasNext()){
                                classValue = INPUT.next();
                            }
                        }while(classValue.equals(""));

                        String[] AValues = new String[values.size()];
                        for(int j = 0; j < values.size(); j++) AValues[j] = values.get(j);
                        index.add(new Plan(value, classValue, AValues));
                        INPUT.reset();
                        System.out.println("Entry added to index.");
                        break;
                    case 3:
                        if(index.size() == 0){
                            System.out.println("No entries to remove!");
                            break;
                        }
                        Plan resultD = null;
                        do {
                            System.out.print("Enter the position or name of the item you wish to remove: ");
                            if(INPUT.hasNextInt()){
                                int i = INPUT.nextInt();
                                if(i >= 1 && i < index.size() + 1)
                                    resultD = index.get(i - 1);
                                else System.out.println("Value must be positive and within the size of the index!");
                            }else if(INPUT.hasNext()){
                                String s = INPUT.next();
                                for (Plan p : index) {
                                    if (p.getName().toLowerCase().equals(s.toLowerCase())) {
                                        resultD = p;
                                    }
                                }
                                if(resultD == null){
                                    System.out.println("No plan matched that name! Check the name and try again.");
                                }
                            }
                        }while(resultD == null);
                        index.remove(resultD);
                        System.out.println("Entry \'" + resultD.getName() + "\' removed.");
                        break;
                    case 4:
                        if(index.size() == 0){
                            System.out.println("No plans are available to query!");
                            break;
                        }
                        Plan result = null;
                        INPUT.useDelimiter(System.getProperty("line.separator"));
                        do {
                            System.out.print("Enter the position or name of the item you wish to view: ");
                            if(INPUT.hasNextInt()){
                                int i = INPUT.nextInt();
                                if(i >= 1 && i < index.size() + 1)
                                result = index.get(i - 1);
                                else System.out.println("Value must be positive and within the size of the index!");
                            }else if(INPUT.hasNext()){
                                String s = INPUT.next();
                                for (Plan p : index) {
                                    if (p.getName().toLowerCase().equals(s.toLowerCase())) {
                                        result = p;
                                    }
                                }
                                if(result == null){
                                    System.out.println("No plan matched that name! Check the name and try again.");
                                }
                            }
                        }while(result == null);
                        INPUT.reset();
                        managePlan(result);
                        break;
                    case 5:
                        File target = null;
                        System.out.println("This utility loads large numbers of index entries from a CSV or plaintext file.");
                        System.out.println("The file must be formatted in standard CSV notation, with item declarations");
                        System.out.println("separated by newline characters.");
                        System.out.println("Files may contain up to " + Integer.MAX_VALUE + " entries.");
                        INPUT.useDelimiter(System.getProperty("line.separator"));
                        do {
                            System.out.print("Enter the path to the file to load: ");
                            if(INPUT.hasNext()){
                                String literal = INPUT.next();
                                if(new File(literal).exists()){
                                    target = new File(literal);
                                }else{
                                    System.out.println("Invalid file path.");
                                }
                            }
                        }while(target == null);
                        INPUT.reset();
                        System.out.println("Loading entries from file...");
                        try {
                            ArrayList<Plan> added = loadEntriesFromFile(target);
                            index.addAll(added);
                            System.out.println("Successfully loaded " + added.size() + " entries to index.");
                        } catch (IOException e){
                            System.out.println("An error occurred while loading from the file.");
                            System.out.println("Error details are as follows:");
                            e.printStackTrace();
                        }
                        break;
                    case 6:
                        System.out.println("Are you SURE you want to clear the index? Enter 'y' if you wish to proceed.");
                        if(INPUT.hasNext() && INPUT.next().equals("y")){
                            index.clear();
                            System.out.println("Index cleared.");
                        }else{
                            System.out.println("Clear cancelled.");
                        }
                        break;
                    case 7:
                        try {
                            writeIndexToFile(index);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        break;
                    case 8:
                        doExit = true;
                }
            }
        }while(!doExit);
    }

    private static void managePlan(Plan target)
    {
        boolean doExit = false;
        do {
            System.out.println();
            System.out.println("-- RECIPE MANAGEMENT --");
            System.out.println();
            System.out.println("Choose an option to continue:");
            System.out.println("");
            System.out.println("1. View Recipe Components");
            System.out.println("2. Revise Recipe Components");
            System.out.println("3. Remove Recipe Components");
            System.out.println("4. Show Item Class");
            System.out.println("5. Return to previous menu");
            System.out.println();
            System.out.print("Enter selection: ");

            if (INPUT.hasNextInt()) {
                System.out.println();
                switch (INPUT.nextInt()) {
                    case 1:
                        if(target.getComponents() == null || target.getComponents().length == 0){
                            System.out.println("No components present. Add some!");
                            break;
                        }
                        System.out.println("The following are the recipe components of the selected plan:");
                        for(String s : target.getComponents()){
                            System.out.println(s);
                        }
                        break;
                    case 2:
                        INPUT.useDelimiter(System.getProperty("line.separator"));
                        ArrayList<String> values = new ArrayList<>();
                        System.out.println("Current component list cleared.");
                        do {
                            System.out.println("Enter the recipe components of this entry. Enter 'done' when done: ");
                            System.out.print("> ");
                            while(INPUT.hasNext()){
                                String tmp = INPUT.next();
                                if(tmp.toLowerCase().equals("done")){
                                    break;
                                }
                                values.add(tmp);
                                System.out.print("> ");
                            }
                        }while(values.size() == 0);

                        String[] AValues = new String[values.size()];
                        for(int j = 0; j < values.size(); j++) AValues[j] = values.get(j);
                        target.setComponents(AValues);
                        INPUT.reset();
                        System.out.println("Components revised.");
                        break;
                    case 3:
                        System.out.println("Removing components...");
                        target.setComponents(null);
                        System.out.println("Components removed.");
                        break;
                    case 4:
                        if(target.getClassIdentifier() != null){
                            System.out.println("Item Class: " + target.getClassIdentifier());
                        }
                        break;
                    case 5:
                        doExit = true;
                }
            }
        }while(!doExit);
    }

    private static ArrayList<Plan> loadEntriesFromFile(File src) throws IOException
    {
        if(!src.exists()){
            throw new IOException("Target file does not exist!");
        }

        Scanner reader = new Scanner(src);
        reader.useDelimiter("\n");

        ArrayList<String> buffer = new ArrayList<>();

        while(reader.hasNext()){
           buffer.add(reader.next());
        }

        ArrayList<Plan> temp = new ArrayList<>();
        for(String s : buffer){
            ArrayList<String> componentList = new ArrayList<>();
            reader = new Scanner(s);
            reader.useDelimiter(",");

            while(reader.hasNext()){
                String r = reader.next();
                if(!r.equals("\r") && !r.equals("\n") && !r.isEmpty()) componentList.add(r.replace("\r", "").replace("\n", ""));
            }

            if(componentList.size() == 0) continue;

            String name = componentList.get(0).replace("\"", "");
            componentList.remove(0);

            String itemClass = componentList.get(0).replace("\"", "");
            componentList.remove(0);

            String[] cmpnts = new String[componentList.size()];
            for(int i = 0; i < componentList.size(); i++) cmpnts[i] = componentList.get(i).replace("\"", "");
            temp.add(new Plan(name, itemClass, cmpnts));
        }

        return temp;
    }

    private static void generate()
    {
        int max = 0;
        do{
            System.out.println();
            System.out.println("Generate how many days' worth of plans?");
            if(INPUT.hasNextInt()) {
                max = INPUT.nextInt();
            }
        } while (max <= 0);

        System.out.println("Checking plan index...");

        if(index.size() < max){
            System.out.println("Not enough plans in list to generate! Add more plans and try again.");
            return;
        }

        ArrayList<Plan> generated = new ArrayList<>();

        int totalTries = 0;
        for(int i = 0; i < max; i++)
        {
            if(totalTries > index.size() * 2){
                System.out.println("Unable to generate! Try adding more entries to the index.");
                return;
            }

            int gen = GENERATOR.nextInt(index.size());

            if(generated.contains(index.get(gen))){
                i--;
            }else{
                int present = 0;
                for(Plan p : generated){
                    if(p.getClassIdentifier().equals(index.get(gen).getClassIdentifier())){
                        present ++;
                    }
                }
                if(present < 2) {
                    generated.add(index.get(gen));
                }else{
                    i --;
                }
            }
            totalTries ++;
        }



        System.out.println("Generated list is as follows:");
        System.out.println();

        ArrayList<String> genBuffer = new ArrayList<>();
        ArrayList<String> componentBuffer = null;

        for(int i = 0; i < max; i++){
            String temp = (i + 1) + ". " + generated.get(i).getName();
            System.out.println(temp);
            genBuffer.add(temp);
        }
        System.out.println();
        System.out.println("Do you wish to view the list of components necessary for these plans (y/n)?");
        if(INPUT.hasNext() && INPUT.next().equals("y")){
            System.out.println("Components are listed in 'name: quantity' format.");
            System.out.println("Listing...");
            System.out.println();

            HashMap<String, Integer> componentList = new HashMap<>();
            for(Plan p : generated){
                for(String c : p.getComponents()){
                    if(componentList.containsKey(c)){
                        componentList.replace(c, componentList.get(c) + 1);
                    }else{
                        componentList.put(c, 1);
                    }
                }
            }

            componentBuffer = new ArrayList<>();

            for(String s : componentList.keySet()){
                if(s.isEmpty() || s.equals(" ")) continue;
                String temp = s + ": " + componentList.get(s);
                System.out.println(temp);
                componentBuffer.add(temp);
            }

            System.out.println("Done.");
        }else{
            System.out.println("Fine, be that way then.");
        }

        System.out.println();
        System.out.println("Do you wish to output the results of this query to a file (y/n)?");
        if(INPUT.hasNext() && INPUT.next().equals("y"))
        {
            System.out.println("Writing temporary file...");
            File cache = new File(ARKGlobalConstants.DESKTOP_CACHE_ROOT.getAbsolutePath() + "\\RPC", "RPOutputCache" + System.currentTimeMillis() + ".txt");
            try {
                cache.getParentFile().delete();
                cache.getParentFile().mkdirs();
                cache.createNewFile();
                BufferedWriter wr = new BufferedWriter(new FileWriter(cache));
                wr.write("List is as follows:");
                wr.newLine();
                wr.newLine();
                for(String s : genBuffer){
                    wr.write(s);
                    wr.newLine();
                }
                wr.newLine();

                if(componentBuffer != null){
                    wr.write("Components are as follows.");
                    wr.newLine();
                    wr.write("Components are listed in 'name: quantity' format.");
                    wr.newLine();
                    wr.newLine();
                    for(String s : componentBuffer){
                        wr.write(s);
                        wr.newLine();
                    }
                }

                wr.flush();
                wr.close();

                System.out.println("Done! Opening file for viewing...");
                Desktop.getDesktop().open(cache);
            } catch (IOException e) {
                System.out.println("Unable to write temporary output file!");
            }
        }
        System.out.println("Done!");
        System.out.println();
    }

    private static void writeIndexToFile(ArrayList<Plan> index) throws IOException
    {
        if(index == null || index.size() == 0){
            return;
        }

        File f = null;

        INPUT.useDelimiter(System.getProperty("line.separator"));
        do {
            System.out.print("Enter the path to the output file: ");
            if(INPUT.hasNext()){
                String literal = INPUT.next();
                if(new File(literal).getParentFile().exists()){
                    f = new File(literal);
                }else{
                    System.out.println("Invalid file path.");
                }
            }
        }while(f == null);
        INPUT.reset();

        System.out.println("Writing...");

        if(f.exists()){
            System.out.println("Output file already exists! Delete it and try again.");
            return;
        }

        f.createNewFile();

        BufferedWriter out = new BufferedWriter(new FileWriter(f));

        for(int i = 0; i < index.size(); i++){
            Plan p = index.get(i);
            out.write(p.getName() + ",");

            for(String s : p.getComponents()){
                out.write(s + ",");
            }
            if(i < index.size() - 1) {
                out.write('\n');
            }
        }

        out.flush();
        out.close();

        System.out.println("Done! File written to:");
        System.out.println(f.getAbsolutePath());
    }
}
