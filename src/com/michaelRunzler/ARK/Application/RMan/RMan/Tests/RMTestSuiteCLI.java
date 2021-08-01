package RMan.Tests;

import RMan.Core.ListGenerator;
import RMan.Core.Managers.*;
import RMan.Core.Types.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class RMTestSuiteCLI
{
    private static Scanner sc;

    private static CardManager carM;
    private static ComponentManager cmpM;
    private static ListManager lM;
    private static ShoppingListManager sM;
    private static CategoryManager catM;
    private static MeasurementManager mM;

    private static final File storage = new File(System.getenv("AppData") + "\\ARK\\RM");

    public static void main(String[] args)
    {
        sc = new Scanner(System.in);
        sc.useDelimiter("\n");

        carM = new CardManager();
        cmpM = new ComponentManager();
        lM = new ListManager();
        sM = new ShoppingListManager();
        catM = new CategoryManager();
        mM = new MeasurementManager();

        carM.importIndex(new File(storage, "cards.idx"), false);
        cmpM.importIndex(new File(storage, "components.idx"), false);
        lM.importIndex(new File(storage, "lists.idx"), false);
        sM.importIndex(new File(storage, "shopping.idx"), false);
        catM.importIndex(new File(storage, "categories.idx"), false);
        mM.importIndex(new File(storage, "measurements.idx"), false);

        if(!storage.exists()) storage.mkdirs();

        while(true)
        {
            int res = menu("Main Menu", "Cards...", "Components...", "Measurements...", "Categories...", "Lists...", "Shopping Lists...", "Exit");
            switch (res) {
                case 1:
                    cardMenu();
                    break;
                case 2:
                    componentMenu();
                    break;
                case 3:
                    measurementMenu();
                    break;
                case 4:
                    categoryMenu();
                    break;
                case 5:
                    listMenu();
                    break;
                case 6:
                    groceryMenu();
                    break;
                case 7:
                    exit();
                    break;
            }
        }
    }

    private static void cardMenu()
    {
        while(true)
        {
            int res = menu("Card Menu", "Card Search", "View/Edit Card List", "Return");
            switch (res)
            {
                case 1:
                    System.out.print("Input search, then press Enter to search or add: ");
                    String input = sc.next();
                    ArrayList<Card> results = carM.searchByName(input, true);

                    // Found a match
                    if(results.size() > 0)
                    {
                        String[] names = new String[results.size() + 1];
                        for (int i = 0; i < results.size(); i++) names[i] = results.get(i).name;
                        names[names.length - 1] = "Return";

                        int sRes = menu("The following cards were found. Select a card to view/edit:", names);
                        editDeleteCard(sRes - 1, results.size());
                    }else{
                        // Didn't find a match
                        addCard(input);
                    }
                    break;
                case 2:
                    String[] names = new String[carM.index.size() + 1];
                    for (int i = 0; i < carM.index.size(); i++) names[i] = carM.index.get(i).name;
                    names[names.length - 1] = "Return";

                    int sRes = menu("Select a card to view/edit:", names);
                    if(sRes == names.length) break;
                    editDeleteCard(sRes - 1, carM.index.size());
                    break;
                case 3:
                    System.out.println();
                    return;
            }
        }
    }

    private static void addCard(String name)
    {
        int res = menu("Cards with this name were not found. Would you like to add one?", "Yes", "No");
        if(res == 2) return;

        Card c = new Card();
        c.name = name;

        System.out.print("Enter the author name: ");
        c.creator = sc.next();
        System.out.print("Set a category for this recipe: ");
        String query = sc.next();
        ArrayList<Category> cResults = catM.searchByName(query, true);

        if(cResults.size() > 0) {
            String[] options = new String[cResults.size()];
            int sRes = menu("Select the category you want:", options);
            c.category = cResults.get(sRes - 1);
            System.out.println("Category set.");
        }else{
            System.out.println("No matching category was found. This recipe will be uncategorized for now, change this later in the Edit Card menu.");
            c.category = new Category();
        }

        System.out.print("Enter the number of servings that this makes: ");
        c.makesNum = sc.nextInt();
        System.out.print("Enter the approximate time that this takes to make: ");
        c.prepTime = sc.nextDouble();
        System.out.print("Enter a short description: ");
        c.description = sc.next();
        System.out.print("Enter the instructions for making this recipe: ");
        c.instructions = sc.next();

        System.out.println("Now, ingredients.");
        boolean done = false;
        while(!done)
        {
            System.out.print("Search for an ingredient: ");
            String search  = sc.next();
            ArrayList<Component> results = cmpM.searchByName(search, true);
            Component cp;
            if(results.size() == 0) {
                System.out.println("No results found; adding \"" + search + "\"...");
                cp = addComponent(search, true);
            }else{
                String[] options = new String[results.size() + 1];
                for(int i = 0; i < results.size(); i++) options[i] = results.get(i).name;
                options[options.length - 1] = "None of these...";
                int sRes = menu("Select an ingredient", options);
                if(sRes == options.length) cp = addComponent(null, true);
                else cp = addComponent(results.get(sRes - 1).name, true);
            }

            c.components.add(cp);
            if(cmpM.searchByName(cp.name, false).size() == 0) cmpM.index.add(cp);
            res = menu("Do you want to add more ingredients?", "Yes", "No");
            done = (res == 2);
        }

        carM.index.add(c);
    }

    private static void editDeleteCard(int index, int sz)
    {
        if(index == sz + 1) return;

        int res = menu("What would you like to do?", "View", "Edit", "Delete", "Cancel/Return");
        switch(res)
        {
            case 1:
                System.out.println(carM.index.get(index).fullToString());
                break;
            case 2:
                editCard(index);
                break;
            case 3:
                int sRes = menu("Are you sure you want to delete " + carM.index.get(index) + "?", "Yes", "No");
                if(sRes == 1) carM.index.remove(index);
                System.out.println("Deleted card.");
                break;
            case 4:
                break;
        }
    }

    private static void editCard(int index)
    {
        while(true)
        {
            int res = menu("Which field would you like to edit?",
                    "Name", "Author", "Category", "Serving Count", "Prep Time", "Description", "Instructions", "Ingredient List", "Save & Return");

            Card c = carM.index.get(index);

            switch (res) {
                case 1:
                    System.out.print("Enter a new name for this card: ");
                    c.name = sc.next();
                    break;
                case 2:
                    System.out.print("Enter a new author name for this card: ");
                    c.creator = sc.next();
                    break;
                case 3:
                    System.out.print("Search for a new category: ");
                    ArrayList<Category> cResults = new ArrayList<>();

                    while(cResults.size() == 0)
                    {
                        String query = sc.next();
                        cResults = catM.searchByName(query, true);

                        if(cResults.size() == 0){
                            System.out.println("No results found; try again, or enter \"Uncategorized\".");
                        }

                        String[] options = new String[cResults.size()];
                        for(int i = 0; i < cResults.size(); i++) options[i] = cResults.get(i).name;
                        int sRes = menu("Select the category you want:", options);
                        c.category = cResults.get(sRes - 1);
                        System.out.println("Category set.");
                    }
                    break;
                case 4:
                    System.out.print("How many servings does this recipe make? Whole numbers only: ");
                    c.makesNum = sc.nextInt();
                    break;
                case 5:
                    System.out.print("How long does this recipe take to make (in minutes)? Decimals are allowed: ");
                    c.prepTime = sc.nextDouble();
                    break;
                case 6:
                    System.out.print("Enter a short description for this recipe: ");
                    c.description = sc.next();
                    break;
                case 7:
                    System.out.print("How do you make this recipe? Be as detailed as you like: ");
                    c.instructions = sc.next();
                    break;
                case 8:
                    editComponents(c.components, true);
                    break;
                case 9:
                    return;
            }
        }
    }

    private static void componentMenu() {
        editComponents(cmpM.index, false);
    }

    private static void editComponents(ArrayList<Component> components, boolean qtyEdit)
    {
        while(true)
        {
            String[] options = new String[components.size() + 2];
            for(int i = 1; i <= components.size(); i++) options[i] = components.get(i - 1).name;
            options[0] = "Add a New Ingredient";
            options[options.length - 1] = "Save & Return";

            int res = menu("Select an ingredient to edit:", options);

            if(res == 1) components.add(addComponent(null, false));
            else if(res == options.length) return;
            else
            {
                Component c = components.get(res - 2);
                int sRes = menu("What would you like to do with this ingredient?", (qtyEdit ? "Edit" : "Rename"),
                        "Delete", "Cancel/Return");
                switch (sRes)
                {
                    case 1:
                        if(qtyEdit){
                            editComponentQty(c);
                        }else {
                            System.out.print("Enter a new name for this ingredient: ");
                            c.name = sc.next();
                        }
                        break;
                    case 2:
                        components.remove(c);
                        System.out.println("Ingredient \"" + c.name + "\" removed.");
                        break;
                    case 3:
                        break;
                }
            }
        }
    }

    private static void editComponentQty(Component c)
    {
        while(true)
        {
            int res = menu("What field would you like to edit?", "Name", "Quantity", "Quantity Units", "Save & Return");
            switch (res) {
                case 1:
                    System.out.print("Enter a new name for this ingredient: ");
                    c.name = sc.next();
                    break;
                case 2:
                    System.out.print("How much of this ingredient is needed? Enter a whole or fractional number: ");
                    c.quantity = sc.next();
                    break;
                case 3:
                    System.out.println("What units does the quantity of this ingredient use? Enter the short name of the measurement unit: ");
                    ArrayList<Measurement> found = new ArrayList<>();
                    while(found.size() == 0)
                    {
                        String search = sc.next();
                        found = mM.searchByShortName(search, false);
                        if(found.size() > 0) break;
                        System.out.print("No measurement unit with this name was found, try again: ");
                    }
                    c.quantityMeasure = found.get(0);
                    break;
                case 4:
                    return;
            }
        }
    }

    private static Component addComponent(String name, boolean qty)
    {
        Component cp = new Component();

        if(name == null)
        {
            System.out.print("Enter the name of the ingredient: ");
            cp.name = sc.next();
        }else cp.name = name;

        if(qty)
        {
            System.out.print("How much of this is needed (numerical)? ");
            cp.quantity = sc.next();
            System.out.print("What unit is this quantity in? Use the short name of the unit (ex. g instead of Grams): ");
            ArrayList<Measurement> measures = new ArrayList<>();

            while (measures.size() == 0) {
                measures = mM.searchByShortName(sc.next(), false);
                if (measures.size() > 0) break;
                System.out.println("No unit with this name was found, try again.");
            }
            cp.quantityMeasure = measures.get(0);
        }

        return cp;
    }

    private static void measurementMenu()
    {
        while(true)
        {
            int res = menu("Measurement Menu", "Add new Measurement Unit", "Delete Measurement Unit", "View/Edit Measurement Units", "Return");
            switch (res)
            {
                case 1:
                    Measurement m = new Measurement();
                    System.out.print("Enter the full name of the unit: ");
                    m.name = sc.next();
                    System.out.print("Enter the short name (or symbol) of the unit: ");
                    m.shortName = sc.next();
                    int sRes = menu("Can this unit be fractionalized?", "Yes", "No");
                    m.allowFractional = sRes == 1;
                    m.userAdded = true;
                    mM.index.add(m);
                    System.out.println("New unit added.");
                    break;
                case 2:
                    ArrayList<String> options = new ArrayList<>();
                    for(Measurement M : mM.index) if(M.userAdded) options.add(M.name);
                    options.add("Cancel");
                    sRes = menu("Select a unit to delete (system measurement units are not shown): ", options.toArray(new String[0]));
                    if(sRes == options.size()) break;
                    ArrayList<Measurement> results = mM.searchByName(options.get(sRes - 1), false);
                    mM.index.remove(results.get(0));
                    System.out.println("Measurement unit deleted.");
                    break;
                case 3:
                    options = new ArrayList<>();
                    for(Measurement M : mM.index) if(M.userAdded) options.add(M.name);
                    options.add("Cancel");
                    sRes = menu("Select a unit to edit (system measurement units are not shown): ", options.toArray(new String[0]));
                    if(sRes == options.size()) break;
                    editMeasurement(mM.index.get(sRes - 1));
                    break;
                case 4:
                    return;
            }
        }
    }

    private static void editMeasurement(Measurement m)
    {
        while(true)
        {
            int res = menu("Select a field to edit:", "Name", "Short Name", "Fractionalizable", "Save & Return");
            switch(res)
            {
                case 1:
                    System.out.print("Enter a new full name for this unit: ");
                    m.name = sc.next();
                    break;
                case 2:
                    System.out.println("Enter a new short name for this unit: ");
                    m.shortName = sc.next();
                    break;
                case 3:
                    int sRes = menu("Is this unit fractionalizable?", "Yes", "No");
                    m.allowFractional = (sRes == 1);
                    break;
                case 4:
                    return;
            }
        }
    }

    private static void categoryMenu()
    {
        while(true)
        {
            int res = menu("Category Menu", "Add new category", "Delete category", "View/edit categories", "Return");
            switch (res)
            {
                case 1:
                    Category c = new Category();
                    System.out.print("Enter a name for this category: ");
                    c.name = sc.next();
                    c.userAdded = true;
                    catM.index.add(c);
                    System.out.println("New category added.");
                    break;
                case 2:
                    ArrayList<String> options = new ArrayList<>();
                    for(Category C : catM.index) if(C.userAdded) options.add(C.name);
                    options.add("Cancel");
                    int sRes = menu("Select a category to delete (system categories are not shown): ", options.toArray(new String[0]));
                    if(sRes == options.size()) break;
                    ArrayList<Category> results = catM.searchByName(options.get(sRes - 1), false);
                    catM.index.remove(results.get(0));
                    System.out.println("Category deleted.");
                    break;
                case 3:
                    options = new ArrayList<>();
                    for(Category C : catM.index) if(C.userAdded) options.add(C.name);
                    options.add("Cancel");
                    sRes = menu("Select a category to rename (system categories are not shown): ", options.toArray(new String[0]));
                    if(sRes == options.size()) break;
                    System.out.print("Enter a new name for this category: ");
                    catM.index.get(sRes - 1).name = sc.next();
                    System.out.println("Rename complete.");
                    break;
                case 4:
                    return;
            }
        }
    }

    private static void listMenu()
    {
        while(true)
        {
            int res = menu("Card List Menu", "New Card List", "View/Edit Existing Lists", "Generate Grocery List", "Return");
            switch (res) {
                case 1:
                    ListGenerator l = new ListGenerator();
                    System.out.print("Enter a name for the new list: ");
                    String name = sc.next();
                    lM.index.add(editList(new List(name, new ArrayList<>())));
                    break;
                case 2:
                    String[] options = new String[lM.index.size() + 1];
                    for(int i = 0; i < lM.index.size(); i++) options[i] = lM.index.get(i).name;
                    options[options.length - 1] = "Cancel";
                    int sRes = menu("Which list would you like to edit?", options);
                    if(sRes == options.length) break;
                    lM.index.set(sRes - 1, editList(lM.index.get(sRes - 1)));
                    break;
                case 3:
                    generateGroceryList();
                    break;
                case 4:
                    return;
            }
        }
    }

    private static List editList(List list)
    {
        ListGenerator l = new ListGenerator();
        l.importList(list);
        while(true)
        {
            int res = menu("What would you like to do?", "Search for a Card", "Remove a Card", "Generate Cards...", "Regenerate a Card", "Save & Finish");
            switch (res)
            {
                case 1:
                    System.out.print("Enter search query: ");
                    ArrayList<Card> results = carM.searchByName(sc.next(), true);
                    if(results.size() == 0) System.out.println("No results found.");
                    else
                    {
                        String[] options = new String[results.size() + 1];
                        for(int i = 0; i < results.size(); i++) options[i] = results.get(i).name;
                        options[options.length - 1] = "Cancel";
                        int sRes = menu("Which card would you like to add?", options);
                        if(sRes == options.length) break;
                        l.addCard(results.get(sRes - 1));
                        System.out.println("Card added.");
                    }
                    break;
                case 2:
                    if(l.list.size() == 0) System.out.println("No cards to remove.");
                    else{
                        String[] options = new String[l.list.size() + 1];
                        for(int i = 0; i < l.list.size(); i++) options[i] = l.list.get(i).name;
                        options[options.length - 1] = "Cancel";

                        int sRes = menu("Which card would you like to remove?", options);
                        if(sRes == options.length) break;
                        l.removeCard(sRes - 1);
                        System.out.println("Card removed.");
                    }
                    break;
                case 3:
                    System.out.print("How many cards would you like to generate? Enter a whole number: ");
                    int gen = sc.nextInt();
                    if(gen <= 0) break;
                    System.out.print("What category would you like to generate these cards from? Enter * for all categories: ");
                    String cat = sc.next();
                    ArrayList<Category> cats = new ArrayList<>();
                    if(cat.equals("*")) cats = catM.index;
                    else{
                        ArrayList<Category> results2 = catM.searchByName(cat, false);
                        if(results2.size() == 0) System.out.println("No results found for this category name.");
                        else cats = results2;
                    }

                    l.updateCardSet(carM, cats.toArray(new Category[0]));
                    boolean success = l.generateList(gen);
                    if(success) System.out.println("Generation successful; " + gen + " card(s) added.");
                    else System.out.println("Unable to generate: not enough cards available!");
                    break;
                case 4:
                    String[] options = new String[l.list.size() + 1];
                    for(int i = 0; i < l.list.size(); i++) options[i] = l.list.get(i).name;
                    options[options.length - 1] = "Cancel";

                    int sRes = menu("Which card would you like to regenerate?", options);
                    if(sRes == options.length) break;
                    success = l.regenerateElement(sRes);
                    if(success) System.out.println("Regenerated card.");
                    else System.out.println("Unable to regenerate: not enough cards available!");
                    break;
                case 5:
                    return l.exportList(list.name);
            }
        }
    }

    private static void groceryMenu()
    {
        while(true)
        {
            int res = menu("Grocery List Menu", "New Grocery List", "View/Check Off Existing Lists", "Return");
            switch (res) {
                case 1:
                    generateGroceryList();
                    break;
                case 2:
                    String[] options = new String[sM.index.size() + 1];
                    for(int i = 0; i < sM.index.size(); i++) options[i] = sM.index.get(i).name;
                    options[options.length - 1] = "Cancel";
                    int sRes = menu("Which list would you like to view?", options);
                    if(sRes == options.length) break;
                    checkGroceryList(sM.index.get(sRes - 1));
                    break;
                case 3:
                    return;
            }
        }
    }

    private static void checkGroceryList(ShoppingList sl)
    {
        while(true)
        {
            String[] options = new String[sl.entries.size() + 1];
            options[options.length - 1] = "Return";
            for (int i = 0; i < sl.entries.size(); i++)
                options[i] = (sl.entries.get(i).isChecked ? "[X] " : "[ ] ") + sl.entries.get(i).name;
            int res = menu("Select an entry to check/uncheck:", options);
            if (res == options.length) return;
            sl.entries.get(res).isChecked = !sl.entries.get(res).isChecked;
        }
    }

    private static void generateGroceryList()
    {
        String[] options = new String[lM.index.size() + 1];
        for(int i = 0; i < lM.index.size(); i++) options[i] = lM.index.get(i).name;
        options[options.length - 1] = "Cancel";
        int sRes = menu("Which list would you like to generate a Shopping List for?", options);
        if(sRes == options.length) return;
        sM.index.add(new ShoppingList(lM.index.get(sRes - 1)));
        System.out.println("Shopping List generated. Use the Shopping List Menu to view and edit it.");
    }

    private static void exit()
    {
        carM.exportIndex(new File(storage, "cards.idx"));
        cmpM.exportIndex(new File(storage, "components.idx"));
        lM.exportIndex(new File(storage, "lists.idx"));
        sM.exportIndex(new File(storage, "shopping.idx"));
        catM.exportIndex(new File(storage, "categories.idx"));
        mM.exportIndex(new File(storage, "measurements.idx"));
        
        System.exit(0);
    }

    private static int menu(String title, String... options)
    {
        while(true)
        {
            System.out.println("\n" + title + "\n");
            for (int i = 0; i < options.length; i++)
                System.out.printf("%d. %s\n", i + 1, options[i]);

            System.out.print("Enter an option: ");
            int res = sc.nextInt();
            if (res >= 1 && res <= options.length)
                return res;
            else
                System.out.println("Invalid input; please try again.");
        }
    }
}
