package de.fanta.casestats.data;

import net.minecraft.item.ItemStack;

import java.util.*;

public class CaseStat {

    private final String id;
    private final ItemStack icon;

    private final Map<UUID, PlayerStat> playerStats;
    private final Map<CaseItem, Integer> totals;
    private int total;

    public CaseStat(String id, ItemStack icon) {
        this.id = id;
        this.icon = icon;
        this.playerStats = new HashMap<>();
        this.totals = new HashMap<>();
        this.total = 0;
    }

    public String id() {
        return id;
    }

    public ItemStack icon() {
        return icon;
    }

    public void reset() {
        totals.clear();
        playerStats.clear();
        total = 0;
    }

    public void setItemOccurrence(UUID uuid, CaseItem caseItem, int count) {
        total += count;
        totals.compute(caseItem, (caseItem1, value) -> value == null ? count : value + count);
        playerStats.computeIfAbsent(uuid, PlayerStat::new).setOccurrence(caseItem, count);
    }

    public int total() {
        return total;
    }

    public Map<CaseItem, Integer> totals() {
        return totals;
    }

    public Map<UUID, PlayerStat> playerStats() {
        return playerStats;
    }

    public static class PlayerStat {

        private final UUID uuid;
        private final Map<CaseItem, Integer> occurrences;
        private int total = 0;

        public PlayerStat(UUID uuid) {
            this.uuid = uuid;
            this.occurrences = new HashMap<>();
        }

        public void setOccurrence(CaseItem caseItem, int occurrence) {
            total += occurrence;
            occurrences.put(caseItem, occurrence);
        }

        public int getOccurrence(CaseItem caseItem) {
            return occurrences.getOrDefault(caseItem, 0);
        }

        public Set<Map.Entry<CaseItem, Integer>> occurrences() {
            return occurrences.entrySet();
        }

        public int total() {
            return total;
        }

        public void reset() {
            total = 0;
            occurrences.clear();
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PlayerStat playerStat)) return false;
            return Objects.equals(uuid, playerStat.uuid);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(uuid);
        }

        public UUID uuid() {
            return uuid;
        }
    }

}
