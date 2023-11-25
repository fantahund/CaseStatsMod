package de.fanta.casestats.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Stats {

    private final Map<String, CaseStat> caseStats;

    public Stats() {
        this.caseStats = new HashMap<>();
    }

    public Optional<CaseStat> caseStatOf(String id) {
        return Optional.ofNullable(caseStats.get(id));
    }

    public void add(CaseStat stat) {
        caseStats.put(stat.id(), stat);
    }

    public Collection<CaseStat> caseStats() {
        return caseStats.values();
    }

}
