package com.client.graphics.interfaces.builder.impl;

import com.client.Configuration;
import com.client.Sprite;
import com.client.graphics.interfaces.RSInterface;
import com.client.graphics.interfaces.builder.InterfaceBuilder;

public class GroupIronmanBank extends InterfaceBuilder {

    public GroupIronmanBank() {
        super(48670);
    }

    @Override
    public void build() {
        addSprite(nextInterface(), new Sprite("banktab/07/bg 0"));
        child(14, 3);

        addText(nextInterface(), "Group Ironman Bank", 2, 0xE68A00, true, true);
        child(250, 13);

        addText(nextInterface(), "0", 0, 0xE68A00, true, true);
        child(36, 9);

        addText(nextInterface(), "350", 0, 0xE68A00, true, true);
        child(36, 21);

        // Inventory and container for inventory
        addInventoryContainer(nextInterface(), 10, 130,12, 4, true, "Withdraw 1", "Withdraw 5", "Withdraw 10", "Withdraw All", "Withdraw X", "Withdraw All but one");
        RSInterface container = addInterface(nextInterface());
        container.width = 455;
        container.height = 248;
        container.scrollMax = 6000;
        setChildren(1, container);

        RSInterface itemContainer = RSInterface.get(getCurrentInterfaceId() - 2);
        itemContainer.contentType = 206;
        container.child(0, itemContainer.id, 12, 0);
        child(24, 42);

        child(5384, 474, 10); // Close
        child(5380, 474, 10);

        child(18929, 70, 296); // Swap items
        child(18930, 70, 296);

        child(18933, 170, 296); // Noting
        child(18934, 170, 296);

        child(58002, 20, 296); // Rearrange mode
        child(58003, 20, 296);

        child(58010, 120, 296); // Noting
        child(58011, 120, 296);

        child(58018, 386, 282); // Deposit backpack
//        child(58019, 423, 292);

        child(58026, 423, 282); // Deposit worn items

        child(59018, 22, 276); // Rearrange mode:
        child(59019, 140, 276); // Withdraw as:

        child(59114, 45, 299); // Swap
        child(59115, 95, 299); // Insert
        child(59116, 145, 299); // Item
        child(59117, 195, 299); // Note

        child(58065, 255,276);
        int subtract_x = 19;
        int quantity_y = 296;
        child(58066, 242 - subtract_x, quantity_y);
        child(58067, 242 - subtract_x, quantity_y+3);
        child(58068, 267 - subtract_x, quantity_y);
        child(58069, 267 - subtract_x, quantity_y+3);
        child(58070, 292 - subtract_x, quantity_y);
        child(58071, 292 - subtract_x, quantity_y+3);
        child(58072, 317 - subtract_x, quantity_y);
        child(58073, 317 - subtract_x, quantity_y+3);
        child(58074, 342 - subtract_x, quantity_y);
        child(58075, 342 - subtract_x, quantity_y+3);
    }
}
