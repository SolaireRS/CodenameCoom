package com.client.graphics.interfaces.dropdown;

import com.client.Client;
import com.client.WorldController;
import com.client.graphics.interfaces.MenuItem;
import com.client.graphics.interfaces.RSInterface;

public class DrawDistanceMenu implements MenuItem {
    @Override
    public void select(int optionSelected, RSInterface rsInterface) {
        switch (optionSelected) {
            case 0:
                Client.getUserSettings().setDrawDistance(30);
                WorldController.farZ = 30;
                break;
            case 1:
                Client.getUserSettings().setDrawDistance(40);
                WorldController.farZ = 40;
                break;
            case 2:
                Client.getUserSettings().setDrawDistance(50);
                WorldController.farZ = 50;
                break;
            case 3:
                Client.getUserSettings().setDrawDistance(60);
                WorldController.farZ = 60;
                break;
            case 4:
                Client.getUserSettings().setDrawDistance(70);
                WorldController.farZ = 70;
                break;
        }

    }
}
