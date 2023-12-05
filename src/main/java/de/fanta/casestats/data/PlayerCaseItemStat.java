package de.fanta.casestats.data;

import java.util.UUID;

public record PlayerCaseItemStat(UUID player, CaseItem caseItem, int count) {
}
