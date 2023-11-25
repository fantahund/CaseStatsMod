package de.fanta.casestats.data;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.util.Identifier;

import java.util.Objects;

public class CaseItem {

    private final ItemStack stack;
    private final Item item;
    private final int amount;
    private final MutableText name;
    private final boolean enchanted;

    public CaseItem(ItemStack stack, Item item, int amount, MutableText name, boolean enchanted) {
        this.stack = stack;
        this.item = item;
        this.amount = amount;
        this.name = name;
        this.enchanted = enchanted;
    }

    public ItemStack stack() {
        return stack;
    }

    public Item item() {
        return item;
    }

    public int amount() {
        return amount;
    }

    public MutableText name() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CaseItem caseItem)) return false;
        return Objects.equals(item, caseItem.item) && amount == caseItem.amount && Objects.equals(name, caseItem.name) && enchanted == caseItem.enchanted;
    }

    @Override
    public int hashCode() {
        return Objects.hash(item, amount, name, enchanted);
    }
}
