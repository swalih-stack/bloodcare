package com.bloodcare.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * BloodInventory - the units in stock for each of the eight blood groups.
 * Built from the stock ledger by InventoryDao. ENCAPSULATION: the map is private and read-only to callers.
 */
public class BloodInventory {
    /** A group with fewer units than this is "low" (shown red). */
    public static final int LOW_STOCK = 20;

    private final Map<String, Integer> units = new LinkedHashMap<>();

    /** All eight groups start at 0; groups missing from the given map stay 0. */
    public BloodInventory(Map<String, Integer> stock) {
        for (String group : BloodGroups.ALL) units.put(group, Math.max(0, stock.getOrDefault(group, 0)));
    }

    public int getAvailableUnits(String bloodGroup) {
        return units.getOrDefault(bloodGroup, 0);
    }

    public int getTotalUnits() {
        int total = 0;
        for (int v : units.values()) total += v;
        return total;
    }

    public boolean isLow(String bloodGroup) {
        return getAvailableUnits(bloodGroup) < LOW_STOCK;
    }

    public Map<String, Integer> asMap() {
        return Collections.unmodifiableMap(units);
    }
}
