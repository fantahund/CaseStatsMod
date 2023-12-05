package de.fanta.casestats.data;

import net.minecraft.item.ItemStack;

import java.util.*;

public class CaseStat {

    private final String id;
    private final ItemStack icon;

    private final Map<UUID, PlayerStat> playerStats;

    public CaseStat(String id, ItemStack icon) {
        this.id = id;
        this.icon = icon;
        this.playerStats = new HashMap<>();
    }

    public String id() {
        return id;
    }

    public ItemStack icon() {
        return icon;
    }

    public void setItemOccurrence(UUID uuid, CaseItem caseItem, int count) {
        playerStats.computeIfAbsent(uuid, PlayerStat::new).setOccurrence(caseItem, count);
    }

    public Collection<PlayerStat> playerStats() {
        return playerStats.values();
    }

    public static class PlayerStat {

        private final UUID uuid;
        private final Map<CaseItem, Integer> occurrences;

        public PlayerStat(UUID uuid) {
            this.uuid = uuid;
            this.occurrences = new HashMap<>();
        }

        public void setOccurrence(CaseItem caseItem, int occurrence) {
            occurrences.put(caseItem, occurrence);
        }

        public int getOccurrence(CaseItem caseItem) {
            return occurrences.getOrDefault(caseItem, 0);
        }

        public Set<Map.Entry<CaseItem, Integer>> occurrences() {
            return occurrences.entrySet();
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
