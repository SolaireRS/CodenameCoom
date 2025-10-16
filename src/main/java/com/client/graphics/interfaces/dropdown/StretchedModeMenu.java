package com.client.graphics.interfaces.dropdown;

import com.client.Client;
import com.client.ClientWindow;
import com.client.features.gameframe.ScreenMode;
import com.client.graphics.interfaces.MenuItem;
import com.client.graphics.interfaces.RSInterface;

import java.awt.*;

public class StretchedModeMenu implements MenuItem {


    public static void updateStretchedMode(boolean stretched) {
        if (Client.currentScreenMode == ScreenMode.FIXED) {
            if (Client.getUserSettings().getStretchedModeDimensions() == null)
                Client.getUserSettings().setStretchedModeDimensions(ScreenMode.FIXED.getDimensions());

            Dimension dimensions = stretched ? Client.getUserSettings().getStretchedModeDimensions() : ScreenMode.FIXED.getDimensions();
            if (Client.stretched != stretched) {
                Client.instance.refreshMode(ScreenMode.FIXED, dimensions);
                Client.stretched = stretched;
            }
        }
    }

    @Override
    public void select(int optionSelected, RSInterface rsInterface) {
        boolean bool = optionSelected == 0;
        if (Client.getUserSettings().isStretchedMode() == bool)
            return;
        Client.getUserSettings().setStretchedMode(bool);
        updateStretchedMode(bool);
    }
}
