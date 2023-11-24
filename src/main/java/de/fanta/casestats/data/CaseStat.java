package de.fanta.casestats.data;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class CaseStat {

    private final String id;
    private final ItemStack icon;
    private String name;

    private Set<ItemStat> occurrences;
    private Map<CaseItem, ItemStat> itemStats;

    public CaseStat(String id, ItemStack icon) {
        this.id = id;
        this.icon = icon;
        this.occurrences = new HashSet<>();
    }

    public void addItemOccurrence(CaseItem caseItem, UUID uuid) {
        itemStats.computeIfAbsent(caseItem, caseItem1 -> new ItemStat(caseItem1)).addOccurrence(uuid);
    }

    public static class ItemStat {

        private final CaseItem caseItem;

        private final Map<UUID, Integer> perPlayerOccurrences;

        public ItemStat(CaseItem caseItem) {
            this.caseItem = caseItem;
            this.perPlayerOccurrences = new HashMap<>();
        }

        public CaseItem caseItem() {
            return caseItem;
        }

        public void addOccurrence(UUID player) {
            perPlayerOccurrences.compute(player, (uuid, curr) -> (curr == null) ? 1 : (curr + 1));
        }

        public void setOccurrence(UUID player, int occurrences) {
            perPlayerOccurrences.put(player, occurrences);
        }

        public int getOccurrence(UUID player) {
            return perPlayerOccurrences.getOrDefault(player, 0);
        }

        public void setOccurrences(Map<UUID, Integer> occurrences) {
            perPlayerOccurrences.putAll(occurrences);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ItemStat itemStat)) return false;
            return Objects.equals(caseItem, itemStat.caseItem);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(caseItem);
        }
    }





}
