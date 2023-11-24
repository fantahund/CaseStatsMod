package de.fanta.casestats.entries;

import net.minecraft.item.Item;

import java.util.Collection;

public class CaseEntry {
    private final Item item;
    private final String name;
    private final int slots;
    private Collection<Integer> itemEntries;

    public CaseEntry(Item item, String name, int slots, Collection<Integer> itemEntries) {
        this.item = item;
        this.name = name;
        this.slots = slots;
        this.itemEntries = itemEntries;
    }

    public String getName() {
        return name;
    }

    public Item getItem() {
        return item;
    }

    public Collection<Integer> getItemEntries() {
        return itemEntries;
    }

    public int getSlots() {
        return slots;
    }

    public void setItemEntries(Collection<Integer> itemEntries) {
        this.itemEntries = itemEntries;
    }

    public void addItemEntry(int itemID) {
        this.itemEntries.add(itemID);
    }
}
