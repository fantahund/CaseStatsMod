package de.fanta.casestats.entries;

import net.minecraft.item.Item;

public record ItemEntry(Item item, String name, int amount) {
}
