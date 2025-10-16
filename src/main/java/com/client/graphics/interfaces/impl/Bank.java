package com.client.graphics.interfaces.impl;

import java.util.Arrays;

import com.client.Client;
import com.client.Configuration;
import com.client.DrawingArea;
import com.client.Sprite;
import com.client.TextDrawingArea;
import com.client.definitions.ItemDefinition;
import com.client.graphics.interfaces.Configs;
import com.client.graphics.interfaces.RSInterface;

public class Bank extends RSInterface {

    public static final int EMPTY_CHILD = 41582;
    public static final int BANK_INTERFACE_ID = 5292;
    public static final int SEARCH_CONTAINER = 41583;
    public static final int SEARCH_BUTTON = 59000;
    public static final int TITLE_INTERFACE_ID = 58064;
    public static final int BANK_CONTAINER_HEIGHT = 130;

    /**
     * The main container for each tab and the top container in the main tab.
     */
    public static final int OLD_CONTAINER_INTERFACE_ID = 5382;

    /**
     * Displays the x/350 item count at the top of the bank.
     */
    public static final int ITEM_COUNT_INTERFACE_ID = 58061;

    /**
     * Item containers for the items. The first one is not display but used to contain the main tab items.
     */
    public static final int[] ITEM_CONTAINERS = {41573, 41574, 41575, 41576, 41577, 41578, 41579, 41580, 41581};

    /**
     * Item containers that display the first items in each tab.
     */
    public static final int[] BANK_TAB_ITEM_DISPLAYS = {58040, 58041, 58042, 58043, 58044, 58045, 58046, 58047, 58048};

    /**
     * The components that display the tab sprite.
     */
    public static final int[] TAB_INTERFACE_IDS = {58050, 58051, 58052, 58053, 58054, 58055, 58056, 58057, 58058};

    /**
     * Buttons to collapse tabs into the main tab.
     */
    public static final int[] COLLAPSE_BUTTONS = {-1, 58053, 58064, 58075, 58086, 58097, 58108, 58119, 58130};

    /**
     * Buttons to open the tabs.
     */
    public static final int[] OPEN_TAB_BUTTONS = {58042, 58054, 58065, 58076, 58087, 58098, 58109, 58120, 58131};

    /**
     * Components that display the sprite button
     */
    public static final int[] TAB_SPRITE_DISPLAYS = {58031, 58032, 58033, 58034, 58035, 58036, 58037, 58038, 58039, };

    public static final Sprite TAB_WITH_PLUS = new Sprite("BankTab/TAB 4");
    public static final Sprite TAB_WITHOUT_PLUS = new Sprite("BankTab/TAB 3");

    /**
     * The children inside the bank scrollable that are dynamically replaced with the contaienrs to
     * display the items inside other tabs. Does not include the default bank item container;
     * the main container is static so isn't tracked.
     */
    public static int[] mainTabChildren = new int[ITEM_CONTAINERS.length];

    /**
     * Scrollable that contains the item containers.
     */
    public static RSInterface bankScrollable = null;

    /**
     * String used to search the bank.
     */
    public static String searchingBankString = "";

    public static int getCurrentBankTab() {
        return Client.instance.variousSettings[Configs.BANK_TAB_CONFIG];
    }

    public static boolean isBankContainer(RSInterface rsInterface) {
        return rsInterface.id == SEARCH_CONTAINER || Arrays.stream(ITEM_CONTAINERS).anyMatch(id -> rsInterface.id == id);
    }

    private static boolean moreTabsBelow(int index) {
        return index + 1 < ITEM_CONTAINERS.length && interfaceCache[ITEM_CONTAINERS[index + 1]].getItemContainerRows() > 0;
    }

    public static boolean isSearchingBank() {
        return Client.inputDialogState == 9;
    }

    public static void openBankSearch() {
        bankScrollable.children[mainTabChildren[0]] = SEARCH_CONTAINER;
        Client.inputDialogState = 9;
        interfaceCache[SEARCH_CONTAINER].resetItems();
    }

    public static void closeBankSearch() {
        interfaceCache[SEARCH_CONTAINER].resetItems();
        Client.inputDialogState = 0;
        searchingBankString = "";
    }

    public static void handleButton(int buttonId) {
        for (int index = 1; index < COLLAPSE_BUTTONS.length; index++) {
            if (buttonId == COLLAPSE_BUTTONS[index]) {
                RSInterface tab = interfaceCache[ITEM_CONTAINERS[index]];
                RSInterface main = interfaceCache[ITEM_CONTAINERS[0]];
                if (main.getInventoryContainerFreeSlots() >= tab.inventoryItemId.length - tab.getInventoryContainerFreeSlots()) {
                    RSInterface.addAllItems(tab, main);
                    shiftTabs();
                    openBankTab(0);
                }
            }
        }

        for (int buttonIndex = 0; buttonIndex < OPEN_TAB_BUTTONS.length; buttonIndex++) {
            if (buttonId == OPEN_TAB_BUTTONS[buttonIndex]) {
                openBankTab(buttonIndex);
            }
        }

        if (buttonId == SEARCH_BUTTON) {
            if (bankScrollable.children[mainTabChildren[0]] == SEARCH_CONTAINER) {
                closeBankSearch();
                openBankTab(getCurrentBankTab());
            } else {
                openBankSearch();
            }
        }
    }

    /**
     * Shift items in each tab and shift the tabs left if needed.
     */
    public static void shiftTabs() {
        for (int tabIndex = 1; tabIndex < ITEM_CONTAINERS.length; tabIndex++) {
            RSInterface container = interfaceCache[ITEM_CONTAINERS[tabIndex]];
            // Shift items left
            container.shiftItems();

            // Shift tabs left
            if (container.getItemContainerRows() == 0) {
                for (int shiftTabIndex = tabIndex; shiftTabIndex < ITEM_CONTAINERS.length; shiftTabIndex++) {
                    // shift from shiftTabIndex + 1 to shiftTabIndex
                    if (shiftTabIndex + 1 < ITEM_CONTAINERS.length) {
                        container = interfaceCache[ITEM_CONTAINERS[shiftTabIndex]];
                        RSInterface shiftLeft = interfaceCache[ITEM_CONTAINERS[shiftTabIndex + 1]];
                        container.inventoryItemId = shiftLeft.inventoryItemId.clone();
                        container.inventoryAmounts = shiftLeft.inventoryAmounts.clone();
                    }
                }
            }
        }
    }

    public static void bankUpdates() {
        // Shift tabs and items left
        shiftTabs();

        // Show/hide tabs based on whether they have items in them
        boolean newTabDisplayed = false;
        for (int tabIndex = 1; tabIndex < TAB_INTERFACE_IDS.length; tabIndex++) {
            if (interfaceCache[ITEM_CONTAINERS[tabIndex]].getInventoryContainerFreeSlots() != interfaceCache[ITEM_CONTAINERS[tabIndex]].inventoryItemId.length) {
                RSInterface.interfaceCache[TAB_INTERFACE_IDS[tabIndex]].isMouseoverTriggered = false;
                RSInterface.interfaceCache[TAB_SPRITE_DISPLAYS[tabIndex]].sprite1 = TAB_WITHOUT_PLUS;
            } else if (!newTabDisplayed) {
                RSInterface.interfaceCache[TAB_INTERFACE_IDS[tabIndex]].isMouseoverTriggered = false;
                RSInterface.interfaceCache[TAB_SPRITE_DISPLAYS[tabIndex]].sprite1 = TAB_WITH_PLUS;
                newTabDisplayed = true;
            } else {
                RSInterface.interfaceCache[TAB_INTERFACE_IDS[tabIndex]].isMouseoverTriggered = true;
            }
        }

        // Update the items displayed at the top of the bank
        for (int index = 1; index < BANK_TAB_ITEM_DISPLAYS.length; index++) {
            interfaceCache[BANK_TAB_ITEM_DISPLAYS[index]].inventoryItemId[0] = interfaceCache[ITEM_CONTAINERS[index]].inventoryItemId[0];
            interfaceCache[BANK_TAB_ITEM_DISPLAYS[index]].inventoryAmounts[0] = interfaceCache[ITEM_CONTAINERS[index]].inventoryAmounts[0];
        }
    }

    public static void drawOnBank(RSInterface rsInterface, int x, int y) {
        if (rsInterface.id == BANK_INTERFACE_ID) {
            DrawingArea.drawPixels(1, 20, 29, 0xE68A00, 16);
        }
    }

    public static void setupMainTab(RSInterface rsInterface, int x, int y) {
        if (rsInterface.id == SEARCH_CONTAINER) {
            bankUpdates();

            // Hide other bank container
            for (int child : mainTabChildren) {
                bankScrollable.children[child] = EMPTY_CHILD;
            }

            // Set the search container inside the bank
            bankScrollable.children[mainTabChildren[0]] = SEARCH_CONTAINER;

            // Update title
            interfaceCache[TITLE_INTERFACE_ID].message = "Results for '" + searchingBankString + "'";

            // Update search container items
            RSInterface searchContainer = interfaceCache[SEARCH_CONTAINER];
            searchContainer.resetItems();
            if (searchingBankString.length() > 0) {
                for (int index = 0; index < ITEM_CONTAINERS.length; index++) {
                    RSInterface container = interfaceCache[ITEM_CONTAINERS[index]];
                    for (int itemIndex = 0; itemIndex < container.inventoryItemId.length; itemIndex++) {
                        if (container.inventoryItemId[itemIndex] > 0) {
                            ItemDefinition definition = ItemDefinition.forID(container.inventoryItemId[itemIndex] - 1);
                            if (definition != null && definition.name != null && definition.name.toLowerCase().contains(searchingBankString.toLowerCase())) {
                                searchContainer.addItem(container.inventoryItemId[itemIndex], container.inventoryAmounts[itemIndex]);
                            }
                        }
                    }
                }
            }
        } else if (rsInterface.id == ITEM_CONTAINERS[getCurrentBankTab()]) {
            bankUpdates();

            // Reset title
            interfaceCache[TITLE_INTERFACE_ID].message = "The Bank of " + Configuration.CLIENT_TITLE;

            if (getCurrentBankTab() == 0) {
                // Init the main tab view
                for (int index = 0; index < mainTabChildren.length; index++)
                    bankScrollable.children[mainTabChildren[index]] = EMPTY_CHILD;
                int itemContainerIndex = 0;
                for (int index = 0; index < mainTabChildren.length; index++) {
                    RSInterface container = interfaceCache[ITEM_CONTAINERS[index]];
                    if (index == 0 || container.getInventoryContainerFreeSlots() != container.inventoryItemId.length) {
                        bankScrollable.children[mainTabChildren[itemContainerIndex++]] = ITEM_CONTAINERS[index];
                    }
                }
            } else if (interfaceCache[ITEM_CONTAINERS[getCurrentBankTab()]].getInventoryContainerFreeSlots() ==
                    interfaceCache[ITEM_CONTAINERS[getCurrentBankTab()]].inventoryItemId.length) {
                openBankTab(0);
            }

            // Update the item count
            int size = 0;
            for (int container : ITEM_CONTAINERS) {
                size += interfaceCache[container].inventoryItemId.length - interfaceCache[container].getInventoryContainerFreeSlots();
            }
            interfaceCache[ITEM_COUNT_INTERFACE_ID].message = "<ul>" + size + "</ul>";

            // Hide/display the main tab containers
            if (getCurrentBankTab() == 0) {
                int height = 4;
                for (int index = 0; index < ITEM_CONTAINERS.length; index++) {
                    RSInterface container = interfaceCache[ITEM_CONTAINERS[index]];
                    if (index != 0 && moreTabsBelow(index - 1)) {
                        Client.instance.bankDivider.drawSprite(x, height + y);
                        height += 10; // buffer for tab separator
                    }
                    bankScrollable.childY[mainTabChildren[index]] = height;
                    height += container.getItemContainerHeight();
                }
                bankScrollable.scrollMax = height > bankScrollable.height + 1 ? height : bankScrollable.height + 1;
            } else {
                int height = rsInterface.getItemContainerHeight();
                bankScrollable.scrollMax = height > bankScrollable.height + 1 ? height : bankScrollable.height + 1;
            }
        }
    }

    public static void openBankTab(int tab) {
        // Comprehensive null and bounds checking
        if (interfaceCache == null) {
            System.err.println("interfaceCache is null - cannot open bank tab");
            return;
        }
        
        if (ITEM_CONTAINERS == null) {
            System.err.println("ITEM_CONTAINERS is null - cannot open bank tab");
            return;
        }
        
        if (tab < 0 || tab >= ITEM_CONTAINERS.length) {
            System.err.println("Invalid bank tab index: " + tab + " (max: " + (ITEM_CONTAINERS.length - 1) + ")");
            return;
        }
        
        int containerIndex = ITEM_CONTAINERS[tab];
        if (containerIndex < 0 || containerIndex >= interfaceCache.length) {
            System.err.println("Invalid container index: " + containerIndex + " for tab: " + tab);
            return;
        }
        
        if (interfaceCache[containerIndex] == null) {
            System.err.println("Interface at container index " + containerIndex + " is null - cannot open bank tab " + tab);
            return;
        }
        
        // Additional safety checks
        if (interfaceCache[containerIndex].inventoryItemId == null) {
            System.err.println("inventoryItemId is null for tab " + tab);
            return;
        }
        
        if (Client.instance == null || Client.instance.variousSettings == null) {
            System.err.println("Client instance or variousSettings is null - cannot update bank tab");
            return;
        }
        
        if (bankScrollable == null) {
            System.err.println("bankScrollable is null - cannot set bank container");
            return;
        }

        // Don't open if tab is empty (only for non-main tabs)
        if (tab != 0) {
            try {
                int freeSlots = (int) interfaceCache[containerIndex].getInventoryContainerFreeSlots();
                int totalSlots = interfaceCache[containerIndex].inventoryItemId.length;
                if (freeSlots == totalSlots) {
                    return; // Tab is empty, don't open
                }
            } catch (Exception e) {
                System.err.println("Error checking if tab is empty: " + e.getMessage());
                return;
            }
        }

        closeBankSearch();

        // Update config safely
        try {
            Client.instance.variousSettings[Configs.BANK_TAB_CONFIG] = tab;
        } catch (Exception e) {
            System.err.println("Error setting bank tab config: " + e.getMessage());
            return;
        }

        // Reset the lines sprites that show the tab as closed
        try {
            for (int tabIndex = 0; tabIndex < ITEM_CONTAINERS.length; tabIndex++) {
                if (34 + tabIndex >= Client.instance.variousSettings.length) {
                    continue; // Skip if index would be out of bounds
                }
                
                if (tabIndex == 0) {
                    Client.instance.variousSettings[34 + tabIndex] = 1;
                } else {
                    Client.instance.variousSettings[34 + tabIndex] = 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Error resetting tab sprites: " + e.getMessage());
        }

        // Set the sprite to show the current tab as open
        try {
            if (34 + tab < Client.instance.variousSettings.length) {
                if (tab == 0) {
                    Client.instance.variousSettings[34 + tab] = 0;
                } else {
                    Client.instance.variousSettings[34 + tab] = 1;
                }
            }
        } catch (Exception e) {
            System.err.println("Error setting current tab sprite: " + e.getMessage());
        }

        // Reset scroll position safely
        try {
            bankScrollable.scrollPosition = 0;
        } catch (Exception e) {
            System.err.println("Error resetting scroll position: " + e.getMessage());
        }

        // Handle main tab children safely
        if (tab != 0) {
            try {
                if (mainTabChildren != null && bankScrollable.children != null) {
                    for (int child : mainTabChildren) {
                        if (child >= 0 && child < bankScrollable.children.length) {
                            bankScrollable.children[child] = EMPTY_CHILD;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error hiding main tab view: " + e.getMessage());
            }
        }

        // Set the tab container safely
        try {
            if (mainTabChildren != null && mainTabChildren.length > 0 && 
                bankScrollable.children != null && 
                mainTabChildren[0] >= 0 && mainTabChildren[0] < bankScrollable.children.length) {
                
                bankScrollable.children[mainTabChildren[0]] = ITEM_CONTAINERS[tab];
            }
        } catch (Exception e) {
            System.err.println("Error setting tab container: " + e.getMessage());
        }
    }
    public static void onConfigChanged(int config, int value) {
        if (config == Configs.BANK_TAB_CONFIG) {
            closeBankSearch();
            if (value >= 0 && value < 10) { // Assuming max 10 bank tabs
                openBankTab(value);
            }
        }
    }

    private void editBank(TextDrawingArea[] tda) {
        // Store the bank scrollable
        bankScrollable = interfaceCache[5385];

        // Fixing container placement on interface
        bankScrollable.height -= 2;
        RSInterface bank = interfaceCache[BANK_INTERFACE_ID];
        for (int index = 0; index < bank.children.length; index++) {
            if (bank.children[index] == bankScrollable.id) {
                bank.childY[index] += 2;
            }
        }

        // Remove old container
        if (!RSInterface.deleteChild(OLD_CONTAINER_INTERFACE_ID, bankScrollable)) {
            System.out.println("Skipping bank reload.");
            return;
        }

        // Empty text to hide other containers while not in main tab
        addText(EMPTY_CHILD, "", tda, 1, 0xE68A00, true, true);

        // Adding new container for main tab
        int newContainersStartIndex = expandChildren(ITEM_CONTAINERS.length, bankScrollable);
        for (int index = 0; index < ITEM_CONTAINERS.length; index++) {
            RSInterface container = addInventoryContainer(ITEM_CONTAINERS[index], 10, BANK_CONTAINER_HEIGHT,12, 0, true);
            container.actions = new String[] {"Withdraw 1", "Withdraw 5", "Withdraw 10", "Withdraw All", "Withdraw X", "Withdraw All but one"};
            container.contentType = 206;
            mainTabChildren[index] = newContainersStartIndex;
            bankScrollable.child(newContainersStartIndex++, ITEM_CONTAINERS[index], 38, 0);
        }

        // Turn on dragging to other item containers
        for (int container : ITEM_CONTAINERS) {
            interfaceCache[container].allowInvDraggingToOtherContainers = true;
        }

        RSInterface container = addInventoryContainer(SEARCH_CONTAINER, 10, BANK_CONTAINER_HEIGHT,12, 0, true);
        container.actions = new String[] {"Withdraw 1", "Withdraw 5", "Withdraw 10", "Withdraw All", "Withdraw X", "Withdraw All but one"};


    }

    public void bank(TextDrawingArea[] tda) {
        RSInterface rs = addInterface(5292);
        rs.message = "";
        rs.textChangeListener = ((w, s) -> {
            for (int i = 0; i < 9; i++)
                (interfaceCache[58031 + i]).active = false;
            int tabId = Integer.parseInt(s);
            (interfaceCache[58031 + tabId]).active = true;
        });
        rs.alignmentPolicy = AlignPolicy.CENTER;
        addSprite(58001, 0, "banktab/07/bank");
        addSprite(59003, 0, "banktab/07/BREAKER");

        configHoverButton2(59000, "Search", "banktab/07/bank", 13, 14, 13, 14, false, 59004);
        addHoveredButton(59004, "banktab/07/bank", 14, 36, 36, 59005);
        addHoverButton(5384, "BankTab/updated/CLOSE", 97, 21, 21, "Close Window", 250, 5380, 3);
        addHoveredButton(5380, "BankTab/updated/CLOSE", 98, 21, 21, 5379);
        addHoverButton(5294, "banktab/07/bank", 7, 37, 29, "Set/edit your bank pin", 250, 5295, 4);
        addHoveredButton(5295, "banktab/bank", 4, 100, 33, 5296);

        //Item Movement
        addBankHover(58002, 4, 58003, 0, 1,
                "BankTab/updated/FLOW", 50, 22, 304, 1,
                "Swap Item Movement Mode", 58004, 7, 6,
                "BankTab/BANK", 58005,
                "Switch to insert items \nmode", "Switch to swap items \nmode.", 12, 20); //7

        //Noted
        addBankHover(58010, 4, 58011, 0, 1,
                "BankTab/updated/FLOW", 50, 22, 116, 1, "Enable/Disable Noted Withdrawal", 58012,
                10, 12, "BankTab/BANK", 58013,
                "Switch to note withdrawal \nmode", "Switch to item withdrawal \nmode",
                12, 20); //9

        addClickableSprites(58014, "Enable/disable placeholders", "banktab/07/bank",
                5, 6, 5);
        configHoverButton2(58018, "Deposit carried items", "banktab/07/bank",
                1, 2, 1, 2, false, 58018);
        configHoverButton2(58026, "Deposit worn items", "banktab/07/bank",
                3, 4, 3, 4, false, 58026);
        for (int i = 0; i < 9; i++) {
            addInterface(58050 + i);
            if (i == 0) {
                addConfigButton(58031, 5292, 0, 1, "banktab/tab",
                        48, 38, new String[] { "Price Check", "View" }, 1, 1111);
                (interfaceCache[58031]).disabledHover = imageLoader(1, "banktab/tab");
            } else {
                addConfigButton(58031 + i, 5292, 4, 3, "banktab/tab",
                        48, 38, new String[] { "Price check", "Collapse", "View" }, 1, 1111 + i);
                (interfaceCache[58031 + i]).disabledHover = imageLoader(2, "banktab/tab");
            }
            addToItemGroup(58040 + i, 1, 1, 0, 0, false, "", "", "");
        }
        addHoverButton(58060, "banktab/07/bank", 12, 25, 25, "Open Presets",
                250, 5295, 4);
        addText(58061, "-", tda, 0, 15108608, true, true);
        addText(58062, "-", tda, 0, 15108608, true, true);
        addInputField(58063, 50, 15108608, "", 100, 23, false, true);
        addText(58064, "Bank Of " + Configuration.CLIENT_TITLE, tda, 2, 15108608, true, true);

        addBankHover(18929, 4, 18930, 0, 1, "BankTab/updated/FLOW",
                50, 22, 305, 1, "Swap Item Movement Mode", 18931, 7, 6,
                "BankTab/BANK", 18932, "Switch to insert items \nmode",
                "Switch to swap items \nmode.", 12, 20); //7

        addBankHover(18933, 4, 18934, 0, 1, "BankTab/updated/FLOW",
                50, 22, 117, 1, "Enable/Disable Noted Withdrawal", 18935,
                10, 12, "BankTab/BANK", 18936,
                "Switch to note withdrawal \nmode", "Switch to item withdrawal \nmode",
                12, 20); //9

        addText(59018, "Rearrange mode:", tda, 1, 16750623, false, true);
        addText(59019, "Withdraw as:", tda, 1, 16750623, false, true);
        addText(59114, "Swap", tda, 1, 16750623, true, true);
        addText(59115, "Insert", tda, 1, 16750623, true, true);
        addText(59116, "Item", tda, 1, 16750623, true, true);
        addText(59117, "Note", tda, 1, 16750623, true, true);

        addText(58065, "Quantity:", tda, 1, 16750623, false, true);
        addConfigButton(58066, 5292, 0, 1, "Interfaces/Bank X/SPRITE", 25, 2, "", 0, 0, 222);
        addTextButton2(58067, "1", "Default quantity: 1", 16750623, 16777215, true, true, tda, 1, 25, 22, 0, 3);
        addConfigButton(58068, 5292, 0, 1, "Interfaces/Bank X/SPRITE", 25, 2, "", 1, 0, 222);
        addTextButton2(58069, "5", "Default quantity: 5", 16750623, 16777215, true, true, tda, 1, 25, 22, 0, 3);
        addConfigButton(58070, 5292, 0, 1, "Interfaces/Bank X/SPRITE", 25, 2, "", 2, 0, 222);
        addTextButton2(58071, "10", "Default quantity: 10", 16750623, 16777215, true, true, tda, 1, 25, 22, 0, 3);
        addConfigButton(58072, 5292, 0, 1, "Interfaces/Bank X/SPRITE", 25, 2, "", 3, 0, 222);
        addTextButton2(58073, "X", "Default quantity: X", 16750623, 16777215, true, true, tda, 1, 25, 22, 0, 3);
        addConfigButton(58074, 5292, 0, 1, "Interfaces/Bank X/SPRITE", 25, 2, "", 4, 0, 222);
        addTextButton2(58075, "All", "Default quantity: All", 16750623, 16777215, true, true, tda, 1, 25, 22, 0, 3);
        (interfaceCache[59000]).contentType = 243;
        RSInterface Interface = interfaceCache[5385];
        Interface.height = 202;
        Interface.width = 481;
        Interface = interfaceCache[5382];
        Interface.contentType = 206;
        Interface.width = 10;
        Interface.invSpritePadX = 12;
        Interface.height = 80;
        Interface.inventoryItemId = new int[800];
        Interface.inventoryAmounts = new int[800];
        Interface.actions = new String[] { "Withdraw 1", "Withdraw 5", "Withdraw 10", "Withdraw All", "Withdraw X", "Withdraw All-but-1" };
        setChildren(48, rs);
        setBounds(58001, 16, 7, 0, rs);
        int close_x = 476;
        int close_y = 14;
        setBounds(5384, close_x, close_y, 1, rs);
        setBounds(5380, close_x, close_y, 2, rs);
        setBounds(59018, 22, 288, 3, rs);
        setBounds(59019, 122, 288, 4, rs);
        int spacer_x = 4;
        setBounds(59000, 382 + spacer_x, 290, 5, rs);
        setBounds(59004, 382 + spacer_x, 290, 6, rs);
        setBounds(58018, 420 + spacer_x, 290, 7, rs);
        setBounds(58026, 458 + spacer_x, 290, 8, rs);
        setBounds(58014, 344 + spacer_x, 290, 9, rs);
        setBounds(5385, 0, 82, 10, rs);
        (interfaceCache[5385]).height = 203;
        int x = 60;
        for (int j = 0; j < 9; j++) {
            setBounds(58050 + j, 0, 0, 11 + j, rs);
            RSInterface rsi = interfaceCache[58050 + j];
            setChildren(2, rsi);
            setBounds(58031 + j, x, 42, 0, rsi);
            setBounds(58040 + j, x + 5, 45, 1, rsi);
            x += 41;
        }
        int quantity_y = 305;
/*        setBounds(59014, 22, quantity_y, 20, rs);
        setBounds(59015, 72, quantity_y, 21, rs);
        setBounds(59016, 122, quantity_y, 22, rs);
        setBounds(59017, 172, quantity_y, 23, rs);*/
        setBounds(58060, 21, 12, 24, rs);
        setBounds(59003, 53, 24, 25, rs);
        setBounds(58061, 59, 13, 26, rs);
        setBounds(58062, 59, 26, 27, rs);
        setBounds(58064, 253, 17, 28, rs);
        setBounds(58065, 255, 288, 29, rs);
        setBounds(18929, 70, 306, 40, rs); // Swap items
        setBounds(18930, 70, 306, 41, rs);
        setBounds(18933, 170, 306, 42, rs); // Noting
        setBounds(18934, 170, 306, 43, rs);
        int subtract_x = 19;
        setBounds(58066, 242 - subtract_x, quantity_y, 30, rs);
        setBounds(58067, 242 - subtract_x, quantity_y+3, 31, rs);
        setBounds(58068, 267 - subtract_x, quantity_y, 32, rs);
        setBounds(58069, 267 - subtract_x, quantity_y+3, 33, rs);
        setBounds(58070, 292 - subtract_x, quantity_y, 34, rs);
        setBounds(58071, 292 - subtract_x, quantity_y+3, 35, rs);
        setBounds(58072, 317 - subtract_x, quantity_y, 36, rs);
        setBounds(58073, 317 - subtract_x, quantity_y+3, 37, rs);
        setBounds(58074, 342 - subtract_x, quantity_y, 38, rs);
        setBounds(58075, 342 - subtract_x, quantity_y+3, 39, rs);
        setBounds(58002, 20, 306, 20, rs); // Rearrange mode
        setBounds(58003, 20, 306, 21, rs);
        setBounds(58010, 120, 306, 22, rs); // Noting
        setBounds(58011, 120, 306, 23, rs);
        setBounds(59114, 45, quantity_y + 3, 44, rs);
        setBounds(59115, 95, quantity_y + 3, 45, rs);
        setBounds(59116, 145, quantity_y + 3, 46, rs);
        setBounds(59117, 195, quantity_y + 3, 47, rs);
        editBank(tda);
    }

}
