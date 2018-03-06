package controllers.task;

import com.google.common.base.Strings;
import com.pezooworks.framework.log.LogHelper;
import com.pezooworks.framework.taskqueue.Task;
import controllers.*;
import controllers.common.Helper;
import org.json.simple.JSONObject;

import java.util.List;

/**
 * Created by LAP11313-local on 12/15/2017.
 */
public class DeleteSentinelTask extends Task {
    private SentinelProfile sentinelProfile;

    public DeleteSentinelTask(SentinelProfile sentinelProfile) {
        this.sentinelProfile = sentinelProfile;
        LogHelper.Log("DeleteSentinelTask.. new delete async task for sentinel := " + sentinelProfile.getMachineName());
    }

    @Override
    protected void HandleTask() {
        try {
            String script = Compute.PATH_TO_DELETE_SENTINEL_SCRIPT + " " + sentinelProfile.getMachineName() + " " + sentinelProfile.getZoneName();
            LogHelper.Log("DeleteSentinel.. script := " + script);
            List<String> output = Helper.executeBashScript(script);

            boolean success = false;
            boolean removeFromList = false;
            for (String s : output) {
                if (!Strings.isNullOrEmpty(s)) {
                    if (s.contains("Delete Sentinel status:0")) {
                        success = true;
                        sentinelProfile.setDeleteTime(Helper.getCurrentDateTime());
                        sentinelProfile.setDeleteTimeMs(System.currentTimeMillis());
                        sentinelProfile.setDeleted(true);
                        Compute.saveSentinel(Application.getDbKeyValue(), sentinelProfile);
                        break;
                    }
                }
            }

            SiteController.getRunningSentinelMap().remove(sentinelProfile.getMachineName());
            Compute.saveSentinelMap(Application.getDbKeyValue(), SiteController.getRunningSentinelMap());

            /* remove from sentinel list by country */
//            String region = sentinelProfile.getRegionName();
//            List<SentinelProfile> list = SentinelManager.INSTANCE.getSentinelsByRegion().get(region);
//            int sentinelIndex = -1;
//            for (SentinelProfile sentinel : list) {
//                if (sentinel.getMachineName().equalsIgnoreCase(sentinelProfile.getMachineName())) {
//                    sentinelIndex = list.indexOf(sentinel);
//                    break;
//                }
//            }
//            if (sentinelIndex != -1) {
//                SentinelManager.INSTANCE.getSentinelsByRegion().get(region).remove(sentinelIndex);
//                removeFromList = true;
//            }

            /* log */
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("time_ms", System.currentTimeMillis());
            jsonObject.put("time", Helper.getCurrentDateTime());
            jsonObject.put("severity", success ? "green" : "red");
            jsonObject.put("message", success ? "Delete sentinel [" + sentinelProfile.getMachineName() + "] OK. Remove from zone list [" + removeFromList + "]." : "Failed to delete sentinel [" + sentinelProfile.getMachineName() + "].");
            SiteController.getWorksLog().add(jsonObject);
            LogHelper.Log("WORK", jsonObject.toString());
        } catch (Exception e) {
            LogHelper.LogException("CreateSentinelTask", e);

            /* log */
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("time_ms", System.currentTimeMillis());
            jsonObject.put("time", Helper.getCurrentDateTime());
            jsonObject.put("severity", "red");
            jsonObject.put("message", "Failed to delete sentinel [" + sentinelProfile.getMachineName() + "].");
            SiteController.getWorksLog().add(jsonObject);
            LogHelper.Log("WORK", jsonObject.toString());
        }
    }
}
