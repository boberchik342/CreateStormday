package org.boberchik342.CreateStormday.all;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.boberchik342.CreateStormday.CreateStormday;
import org.boberchik342.CreateStormday.pinwheel.Pinwheel;

public class AllItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CreateStormday.MODID);
    public static final DeferredItem<Item> PINWHEEL = ITEMS.registerItem(
            "pinwheel",
            Pinwheel::new, // The factory that the properties will be passed into.
            new Item.Properties() // A unary operator of the properties to use.
    );
}
