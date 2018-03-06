package controllers;

/**
 * Created by Administrator on 23/12/2017.
 */
public class FarmRegion {
    private final String regionName;
    private int currentCycle;
    private long lastUpdateTime;

    public FarmRegion(String regionName) {
        this.regionName = regionName;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public String getRegionName() {
        return regionName;
    }

    public int getCurrentCycle() {
        return currentCycle;
    }

    public void setCurrentCycle(int currentCycle) {
        this.currentCycle = currentCycle;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public void setLastUpdateTime(long lastUpdateTime) {
        this.lastUpdateTime = lastUpdateTime;
    }
}
