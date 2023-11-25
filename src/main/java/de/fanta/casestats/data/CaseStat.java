package de.fanta.casestats.data;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class CaseStat {

    private final String id;
    private final ItemStack icon;
    private String name;

    private Set<ItemStat> occurrences;
    private Map<UUID, PlayerStat> playerStats;

    public CaseStat(String id, ItemStack icon) {
        this.id = id;
        this.icon = icon;
        this.occurrences = new HashSet<>();
    }

    public void addItemOccurrence(CaseItem caseItem, UUID uuid) {
        playerStats.computeIfAbsent(uuid, PlayerStat::new).addOccurrence(caseItem);
    }

    public static class PlayerStat {

        private UUID uuid;
        private Map<CaseItem, Integer> occurrences;

        public PlayerStat(UUID uuid) {
            this.uuid = uuid;
            this.occurrences = new HashMap<>();
        }

        public void addOccurrence(CaseItem caseItem) {
            occurrences.compute(caseItem, (uuid, curr) -> curr == null ? 1 : ++curr);
        }

        public void setOccurrence(CaseItem caseItem, int occurrence) {
            occurrences.put(caseItem, occurrence);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PlayerStat playerStat)) return false;
            return Objects.equals(uuid, );
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(uuid);
        }

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
