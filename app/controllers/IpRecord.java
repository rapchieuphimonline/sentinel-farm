package controllers;

import com.google.api.client.repackaged.com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by LAP11313-local on 12/18/2017.
 */
public class IpRecord {
    private long id, lastUsedTimeMs, createdTimeMs;
    private String ip, createdTime;
    private List<String> zones = new ArrayList<>();
    private int count = -1;

    public IpRecord(long id) {
        this.id = id;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void addZone(String zone) {
        zones.add(zone);
    }

    public void increaseCount() {
        count = count + 1;
    }

    public long getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public List<String> getZones() {
        Set<String> unique = new HashSet<>();
        unique.addAll(zones);
        return new ArrayList<>(unique);
    }

    public int getCount() {
        return count;
    }

    public long getLastUsedTimeMs() {
        return lastUsedTimeMs;
    }

    public void setLastUsedTimeMs(long lastUsedTimeMs) {
        this.lastUsedTimeMs = lastUsedTimeMs;
    }

    public long getCreatedTimeMs() {
        return createdTimeMs;
    }

    public void setCreatedTimeMs(long createdTimeMs) {
        this.createdTimeMs = createdTimeMs;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public boolean inZone(String filter) {
        if (Strings.isNullOrEmpty(filter)) {
            return true;
        }

        for (String zone : zones) {
            if (zone.contains(filter)) {
                return true;
            }
        }
        return false;
    }

    public boolean inDate(String date) {
        try {
            if (Strings.isNullOrEmpty(date)) {
                return true;
            }
            if (Strings.isNullOrEmpty(createdTime)) {
                return false;
            }
            return createdTime.contains(date);
        } catch (Exception e) {
            return false;
        }
    }
}
