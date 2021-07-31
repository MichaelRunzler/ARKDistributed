package RMan.Tests;

import RMan.Core.Managers.*;
import RMan.Core.Types.Card;
import RMan.Core.Types.Component;
import RMan.Core.Types.Measurement;

import java.io.File;
import java.io.IOException;
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

    private static final File storage = new File(System.getProperty("AppData") + "\\ARK\\RM");

    public static void main(String[] args)
    {
        sc = new Scanner(System.in);

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
            int res = menu("Main Menu", "Cards...", "Components...", "Lists...", "Grocery Lists...", "Exit");
            switch (res) {
                case 1:
                    cardMenu();
                    break;
                case 2:
                    componentMenu();
                    break;
                case 3:
                    listMenu();
                    break;
                case 4:
                    groceryMenu();
                    break;
                case 5:
                    exit();
                    break;
            }
        }
    }

    private static void cardMenu()
    {
        while(true)
        {
            int res = menu("Card Menu", "Card Search", "View/Edit Card List", "Save Card List", "Return");
            switch (res)
            {
                case 1:
                    System.out.print("Input search, then press Enter to search or add:");
                    String input = sc.next();
                    ArrayList<Card> results = carM.searchByName(input, true);

                    // Found a match
                    if(results.size() > 0)
                    {
                        String[] names = new String[results.size() + 1];
                        for (int i = 0; i < results.size(); i++) {
                            Card c = results.get(i);
                            names[i] = c.name;
                        }
                        names[names.length - 1] = "Return";

                        int sRes = menu("The following cards were found. Select a card to view/edit:", names);
                        editDeleteCard(sRes, results.size());
                    }else{
                        // Didn't find a match
                        addCard();
                    }
                case 2:
                    String[] names = new String[carM.index.size() + 1];
                    for (int i = 0; i < carM.index.size(); i++) {
                        Card c = carM.index.get(i);
                        names[i] = c.name;
                    }
                    names[names.length - 1] = "Return";

                    int sRes = menu("Select a card to view/edit:", names);
                    editDeleteCard(sRes, carM.index.size());
                    break;
                case 3:
                    break;
                case 4:
                    System.out.println();
                    return;
            }
        }
    }

    private static void addCard()
    {
        int res = menu("Cards with this name were not found. Would you like to add one?", "Yes", "No");
        if(res == 2) return;

        Card c = new Card();

        System.out.print("Enter the author name:");
        c.creator = sc.next();
        System.out.print("Enter the number of servings that this makes:");
        c.makesNum = sc.nextInt();
        System.out.print("Enter the approximate time that this takes to make:");
        c.prepTime = sc.nextDouble();
        System.out.print("Enter a short description:");
        c.description = sc.next();
        System.out.print("Enter the instructions for making this recipe:");
        c.instructions = sc.next();

        System.out.println("Now, ingredients.");
        boolean done = false;
        while(!done)
        {
            Component cp = new Component();
            System.out.println("Enter the name of the ingredient:");
            cp.name = sc.next();
            System.out.println("How much of this is needed (numerical)?");
            cp.quantity = sc.next();
            System.out.println("What unit is this quantity in?");
            ArrayList<Measurement> measures = new ArrayList<>();
            while(measures.size() == 0){
                System.out.println("No unit with this name was found, try again.");
                measures = mM.searchByName(sc.next(), false);
            }
            cp.quantityMeasure = measures.get(0);

            c.components.add(cp);
            cmpM.index.add(cp);
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
                carM.index.get(index).fullToString();
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
                System.out.println();
        }
    }

    private static void editCard(int index)
    {
        int res = menu("Which field would you like to edit?",
                "Name", "Author", "Serving Count", "Prep Time", "Description", "Instructions", "Ingredient List");

        Card c = carM.index.get(index);

        //todo finish
        switch(res)
        {
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                break;
            case 5:
                break;
            case 6:
                break;
            case 7:
                break;
        }
    }

    private static void componentMenu()
    {

    }

    private static void listMenu()
    {

    }

    private static void groceryMenu()
    {

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
            System.out.println(title + "\n");
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
