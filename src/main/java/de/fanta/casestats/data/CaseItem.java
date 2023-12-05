package de.fanta.casestats.data;

import net.minecraft.item.ItemStack;

import java.util.Objects;

public class CaseItem {

    private final String id;
    private final ItemStack stack;

    public CaseItem(String id, ItemStack stack) {
        this.id = id;
        this.stack = stack;
    }

    public ItemStack stack() {
        return stack;
    }

    public String id() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CaseItem caseItem)) return false;
        return Objects.equals(id, caseItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
