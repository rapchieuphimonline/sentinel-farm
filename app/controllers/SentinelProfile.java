package controllers;

/**
 * Created by LAP11313-local on 12/15/2017.
 */
public class SentinelProfile {
    private long id;
    private String ip, machineName, regionName, zoneName, templateName, createTime, startTime, serviceStartTime, finishedTime, deleteTime;
    private long createTimeMs, deleteTimeMs, lastHeartBeatMs, startTimeMs, finishedTimeMs, serviceStartTimeMs, useCount;
    private boolean createSuccess, deleted, running, finished, isIdleCleanUp;

    public SentinelProfile(long id) {
        this.id = id;
        machineName = "sentinel-" + id;
    }

    public long getId() {
        return id;
    }

    public String getMachineName() {
        return machineName;
    }

    public String getCreateTime() {
        return createTime;
    }

    public String getDeleteTime() {
        return deleteTime;
    }

    public long getCreateTimeMs() {
        return createTimeMs;
    }

    public long getDeleteTimeMs() {
        return deleteTimeMs;
    }

    public boolean isCreateSuccess() {
        return createSuccess;
    }

    public void setMachineName(String machineName) {
        this.machineName = machineName;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public void setDeleteTime(String deleteTime) {
        this.deleteTime = deleteTime;
    }

    public void setCreateTimeMs(long createTimeMs) {
        this.createTimeMs = createTimeMs;
    }

    public void setDeleteTimeMs(long deleteTimeMs) {
        this.deleteTimeMs = deleteTimeMs;
    }

    public void setCreateSuccess(boolean createSuccess) {
        this.createSuccess = createSuccess;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public long getLastHeartBeatMs() {
        return lastHeartBeatMs;
    }

    public void setLastHeartBeatMs(long lastHeartBeatMs) {
        this.lastHeartBeatMs = lastHeartBeatMs;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public void setStartTimeMs(long startTimeMs) {
        this.startTimeMs = startTimeMs;
    }

    public String getFinishedTime() {
        return finishedTime;
    }

    public void setFinishedTime(String finishedTime) {
        this.finishedTime = finishedTime;
    }

    public long getFinishedTimeMs() {
        return finishedTimeMs;
    }

    public void setFinishedTimeMs(long finishedTimeMs) {
        this.finishedTimeMs = finishedTimeMs;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public String getZoneName() {
        return zoneName;
    }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public boolean isWorking() {
        if (lastHeartBeatMs > 0 && (System.currentTimeMillis() - lastHeartBeatMs) < 10000) {
            return true;
        }
        return false;
    }

    public String getServiceStartTime() {
        return serviceStartTime;
    }

    public void setServiceStartTime(String serviceStartTime) {
        this.serviceStartTime = serviceStartTime;
    }

    public long getServiceStartTimeMs() {
        return serviceStartTimeMs;
    }

    public void setServiceStartTimeMs(long serviceStartTimeMs) {
        this.serviceStartTimeMs = serviceStartTimeMs;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getUseCount() {
        return useCount;
    }

    public void setUseCount(long useCount) {
        this.useCount = useCount;
    }

    public boolean isIdleCleanUp() {
        return isIdleCleanUp;
    }

    public void setIdleCleanUp(boolean idleCleanUp) {
        isIdleCleanUp = idleCleanUp;
    }

    public String getRegionName() {
        return regionName;
    }

    public void setRegionName(String regionName) {
        this.regionName = regionName;
    }
}
