package controllers;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.pezooworks.framework.common.dbkeyvalue.DBKeyValue;
import com.pezooworks.framework.log.LogHelper;
import controllers.common.Helper;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by LAP11313-local on 12/18/2017.
 */
public enum SentinelManager {
    INSTANCE;

    private FarmRegion currentFarmRegion;
    private int sentinelCount = 0;
    private int dailySentinelCount = 0;

    public void startExecute() {
        LogHelper.Log("SentinelManager.. start execute");

        currentFarmRegion = loadCurrentFarm();
        LogHelper.Log("SentinelManager.. load current farm := " + Helper.gson.toJson(currentFarmRegion));

        /* load old logs */
        SiteController.readWorkLogHistory();

        /* load counter */
        dailySentinelCount = Compute.loadDailyCounter(Application.getDbKeyValue());

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        /* Reset cycle daily */
        long secondsToNext0AM = Helper.secondsToSpecificHourOfDay(0, 10, "GMT+0");
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                setDailySentinelCount(0);
                resetFarmCycle();
            }
        }, secondsToNext0AM, 86400, TimeUnit.SECONDS);

        /* handle farm region */
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                LogHelper.Log("on handle farm region...");
                /* check if the current farm reach max cycle per day */
                int maxFarmCyclePerDay = Integer.parseInt(SiteController.getConfig().get("MAX_FARM_CYCLE_PER_DAY"));
                if (currentFarmRegion != null && currentFarmRegion.getCurrentCycle() >= maxFarmCyclePerDay) {
                    LogHelper.Log("on handle farm region... current farm reached max cycle per day => Move to next farm");
                    /* clean current farm and move to next farm */
                    cleanCurrentFarm();
                    moveToNextFarm();
                    return;
                }

                long elapsedS = (System.currentTimeMillis() - currentFarmRegion.getLastUpdateTime())/1000;
                LogHelper.Log("on handle farm region... current farm elapsed second := " + elapsedS);
                if (elapsedS >= Integer.parseInt(SiteController.getConfig().get("DURATION_SECOND_PER_REGION"))) {
                    LogHelper.Log("on handle farm region... current farm reached max existed time -> Move to next farm");
                    currentFarmRegion.setCurrentCycle(currentFarmRegion.getCurrentCycle() + 1);
                    Compute.saveFarmRegion(Application.getDbKeyValue(), currentFarmRegion);
                    /* clean current farm and move to next farm */
                    cleanCurrentFarm();
                    moveToNextFarm();
                }
            }
        }, 10, 60, TimeUnit.SECONDS);

        /* create new sentinel */
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                boolean shouldCreateNewSentinel = SiteController.getConfig().get("SCHEDULE_TASK_CREATE_SENTINEL_STATUS").equalsIgnoreCase("play");
                if (shouldCreateNewSentinel) {
                    try {
                        int maxFarmCyclePerDay = Integer.parseInt(SiteController.getConfig().get("MAX_FARM_CYCLE_PER_DAY"));
                        if (currentFarmRegion == null || currentFarmRegion.getCurrentCycle() >= maxFarmCyclePerDay) {
                            return;
                        }

                        int maxSentinelPerFarm = Integer.parseInt(SiteController.getConfig().get("CONCURRENT_SENTINELS_PER_REGION"));
                        if (sentinelCount < maxSentinelPerFarm) {
                            /* increase sentinel count */
                            sentinelCount++;
                            setDailySentinelCount(getDailySentinelCount() + 1);

                            LogHelper.Log("scheduledTask.. create sentinel, region := " + currentFarmRegion.getRegionName() + ", size := " + sentinelCount + ", limit := " + maxSentinelPerFarm);
                            String zone = Compute.getRegion(currentFarmRegion.getRegionName()).randomZone();
                            createSentinel(currentFarmRegion.getRegionName(), zone);
                        }
                    } catch (Exception e) {
                        LogHelper.LogException("scheduledTask", e);
                    }
                }
            }
        }, 10, 30, TimeUnit.SECONDS);

        /* remove idle sentinel */
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if (SiteController.getConfig().get("SCHEDULE_TASK_DELETE_SENTINEL_STATUS").equalsIgnoreCase("play")) {
                    try {
                        cleanOldFarm();
                        removeIdleSentinel();
                    } catch (Exception e) {
                        LogHelper.LogException("scheduledTask", e);
                    }
                }
            }
        }, 10, 60, TimeUnit.SECONDS);
    }

    private Map<String, List<SentinelProfile>> loadSentinelList() {
        return null;
    }

    private void removeIdleSentinel() {
        for (Map.Entry<String, SentinelProfile> entry : SiteController.getRunningSentinelMap().entrySet()) {
            SentinelProfile sentinelProfile = entry.getValue();
            if (sentinelProfile.isIdleCleanUp()) {
                continue;
            }
            long elapsedTime = (System.currentTimeMillis() - sentinelProfile.getCreateTimeMs()) / 1000;
            long expiredTime = Long.parseLong(SiteController.getConfig().get("SENTINEL_EXPIRED_TIME_SECOND"));
            if (elapsedTime >= expiredTime) {
                deleteIdleSentinel(sentinelProfile);
            }
        }
    }

    private void resetFarmCycle() {
        LogHelper.Log("resetFarmCycle.. start");
        try {
            for (GCPRegion gcpRegion : Compute.GCP_REGIONS) {
                FarmRegion farmRegion = Compute.loadFarmRegion(Application.getDbKeyValue(), gcpRegion.getName());
                if (farmRegion != null) {
                    farmRegion.setCurrentCycle(0);
                    Compute.saveFarmRegion(Application.getDbKeyValue(), farmRegion);
                }
            }
            currentFarmRegion.setCurrentCycle(0);
            Compute.saveFarmRegion(Application.getDbKeyValue(), currentFarmRegion);
        } catch (Exception e) {
            LogHelper.LogException("resetFarmCycle", e);
        }
    }

    private FarmRegion loadCurrentFarm() {
        try {
            Object obj = Application.getDbKeyValue().Get("current_farm_name");
            LogHelper.Log("loadCurrentFarm.. farm := " + (String)obj);
            if (obj != null) {
                FarmRegion farmRegion = Compute.loadFarmRegion(Application.getDbKeyValue(), (String)obj);
                if (farmRegion != null) {
                    return farmRegion;
                }
            }
        } catch (Exception e) {
            LogHelper.LogException("loadCurrentFarm", e);
        }
        return new FarmRegion(Compute.GCP_REGIONS[0].getName());
    }

    private FarmRegion nextFarm(FarmRegion currentFarmRegion) {
        int currentID = -1;
        if (currentFarmRegion == null) {
            return new FarmRegion(Compute.GCP_REGIONS[0].getName());
        } else {
            for (int i = 0; i < Compute.GCP_REGIONS.length - 1; i++) {
                GCPRegion gcpRegion = Compute.GCP_REGIONS[i];
                if (gcpRegion.getName().equalsIgnoreCase(currentFarmRegion.getRegionName())) {
                    currentID = i;
                    break;
                }
            }
        }
        int nextID = currentID + 1;
        if (nextID >= Compute.GCP_REGIONS.length - 1) {
            nextID = 0;
        }
        FarmRegion ret = Compute.loadFarmRegion(Application.getDbKeyValue(), Compute.GCP_REGIONS[nextID].getName());
        return ret != null ? ret : new FarmRegion(Compute.GCP_REGIONS[nextID].getName());
    }

    public SentinelProfile createSentinel(String region, String zone) {
        LogHelper.Log("SentinelManager.. create new sentinel for region := " + region + ", zone := " + zone);
        try {
            if (Strings.isNullOrEmpty(region) || Strings.isNullOrEmpty(zone)) {
                /* log */
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("time_ms", System.currentTimeMillis());
                jsonObject.put("time", Helper.getCurrentDateTime());
                jsonObject.put("severity", "red");
                jsonObject.put("message", "[SentinelManager] Create new sentinel failed. Region info: [" + region + "/" + zone +"]");
                SiteController.getWorksLog().add(jsonObject);
                LogHelper.Log("WORK", jsonObject.toString());
                return null;
            }

            SentinelProfile sentinelProfile = Compute.createSentinel(region, zone);
            if (sentinelProfile != null) {
                SiteController.getRunningSentinelMap().put(sentinelProfile.getMachineName(), sentinelProfile);
                Compute.saveSentinelMap(Application.getDbKeyValue(), SiteController.getRunningSentinelMap());
                Compute.saveSentinel(Application.getDbKeyValue(), sentinelProfile);

                /* log */
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("time_ms", System.currentTimeMillis());
                jsonObject.put("time", Helper.getCurrentDateTime());
                jsonObject.put("severity", "green");
                jsonObject.put("message", "[SentinelManager] Create new sentinel, ID [" + sentinelProfile.getId() + "] at zone [" + region + "/" + zone +"], total sentinel := " + sentinelCount);
                SiteController.getWorksLog().add(jsonObject);
                LogHelper.Log("WORK", jsonObject.toString());

                return sentinelProfile;
            }
        } catch (Exception e) {
            LogHelper.LogException("SentinelManager", e);
        }
        return null;
    }

    private void deleteIdleSentinel(SentinelProfile sentinelProfile) {
        LogHelper.Log("deleteIdleSentinel.. delete idle sentinel [" + sentinelProfile.getMachineName() + "]");
        try {
            sentinelProfile.setIdleCleanUp(true);
            Compute.saveSentinel(Application.getDbKeyValue(), sentinelProfile);
            Compute.deleteSentinel(sentinelProfile);

            /* log */
            long elapsedTime = (System.currentTimeMillis() - sentinelProfile.getCreateTimeMs()) / 1000;
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("time_ms", System.currentTimeMillis());
            jsonObject.put("time", Helper.getCurrentDateTime());
            jsonObject.put("severity", "yellow");
            jsonObject.put("message", "[SentinelManager] Delete idle sentinel [" + sentinelProfile.getMachineName() + "]. Idle time : " + elapsedTime + " seconds.");
            SiteController.getWorksLog().add(jsonObject);
            LogHelper.Log("WORK", jsonObject.toString());
        } catch (Exception e) {
            LogHelper.LogException("SentinelManager", e);
        }
    }

    public void moveToNextFarm() {
        FarmRegion newFarm = nextFarm(currentFarmRegion);
        newFarm.setLastUpdateTime(System.currentTimeMillis());
        LogHelper.Log("### moveToNextFarm.. farm  [" + currentFarmRegion.getRegionName() + "] is full or reaches max cycle per day => Go to next farm := " + newFarm.getRegionName());
        Application.getDbKeyValue().Set("current_farm_name", newFarm.getRegionName());

        currentFarmRegion = newFarm;
        Compute.saveFarmRegion(Application.getDbKeyValue(), currentFarmRegion);
        /* reset sentinel count */
        sentinelCount = 0;
    }

    public void cleanCurrentFarm() {
        LogHelper.Log("### cleanCurrentFarm.. start ###");
        for (Map.Entry<String, SentinelProfile> entry : SiteController.getRunningSentinelMap().entrySet()) {
            SentinelProfile sentinelProfile = entry.getValue();
            if (sentinelProfile.getRegionName().equalsIgnoreCase(currentFarmRegion.getRegionName()) && sentinelProfile.isFinished()) {
                Compute.deleteSentinel(sentinelProfile);
            }
        }
        LogHelper.Log("### cleanCurrentFarm.. finished ###");
    }

    public void cleanOldFarm() {
        LogHelper.Log("### cleanOldFarm.. start ###");
        for (Map.Entry<String, SentinelProfile> entry : SiteController.getRunningSentinelMap().entrySet()) {
            SentinelProfile sentinelProfile = entry.getValue();
            if (!sentinelProfile.getRegionName().equalsIgnoreCase(currentFarmRegion.getRegionName()) && sentinelProfile.isFinished()) {
                Compute.deleteSentinel(sentinelProfile);
            }
        }
        LogHelper.Log("### cleanOldFarm.. finished ###");
    }

    public FarmRegion getCurrentFarmRegion() {
        return currentFarmRegion;
    }

    public int getSentinelCount() {
        return sentinelCount;
    }

    public int getDailySentinelCount() {
        return dailySentinelCount;
    }

    public int getLimitSentinelPerRegion() {
        return Integer.parseInt(SiteController.getConfig().get("CONCURRENT_SENTINELS_PER_REGION"));
    }

    public int getDailyLimitFarmCycle() {
        return Integer.parseInt(SiteController.getConfig().get("MAX_FARM_CYCLE_PER_DAY"));
    }

    public void setDailySentinelCount(int dailySentinelCount) {
        this.dailySentinelCount = dailySentinelCount;
        Compute.saveDailyCounter(Application.getDbKeyValue(), this.dailySentinelCount);
    }
}
