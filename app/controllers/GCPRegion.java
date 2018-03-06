package controllers;

import controllers.common.Helper;

/**
 * Created by LAP11313-local on 12/21/2017.
 */
public final class GCPRegion {
    private final String name;
    private final String[] zones;

    public GCPRegion(String name, String[] zones) {
        this.name = name;
        this.zones = zones;
    }

    public String getName() {
        return name;
    }

    public String[] getZones() {
        return zones;
    }

    public String randomZone() {
        if (zones != null && zones.length > 0) {
            return zones[Helper.RANDOM_RANGE(0, zones.length - 1)];
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}
