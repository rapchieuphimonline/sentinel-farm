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
public class CreateSentinelTask extends Task {
    private SentinelProfile sentinelProfile;

    public CreateSentinelTask(SentinelProfile sentinelProfile) {
        this.sentinelProfile = sentinelProfile;
        LogHelper.Log("CreateSentinelTask.. new create async task for sentinel := " + sentinelProfile.getMachineName());
    }

    @Override
    protected void HandleTask() {
        try {
            String script = Compute.PATH_TO_CREATE_SENTINEL_SCRIPT + " " + sentinelProfile.getMachineName() + " " + sentinelProfile.getTemplateName() + " " + sentinelProfile.getZoneName();
            LogHelper.Log("CreateSentinel.. script := " + script);
            List<String> output = Helper.executeBashScript(script);

            boolean success = false;
            for (String s : output) {
                if (!Strings.isNullOrEmpty(s)) {
                    if (s.contains("Create Sentinel status:0")) {
                        success = true;
                        sentinelProfile.setCreateSuccess(true);
                        sentinelProfile.setStartTime(Helper.getCurrentDateTime());
                        sentinelProfile.setStartTimeMs(System.currentTimeMillis());
                        Compute.saveSentinel(Application.getDbKeyValue(), sentinelProfile);
                        break;
                    }
                }
            }

            if (!success) {
                SiteController.getRunningSentinelMap().remove(sentinelProfile.getMachineName());
                Compute.saveSentinelMap(Application.getDbKeyValue(), SiteController.getRunningSentinelMap());
                /* remove from sentinel list by country */
//                String region = sentinelProfile.getRegionName();
//                List<SentinelProfile> list = SentinelManager.INSTANCE.getSentinelsByRegion().get(region);
//                int sentinelIndex = -1;
//                for (SentinelProfile sentinel : list) {
//                    if (sentinel.getMachineName().equalsIgnoreCase(sentinelProfile.getMachineName())) {
//                        sentinelIndex = list.indexOf(sentinel);
//                        break;
//                    }
//                }
//                if (sentinelIndex != -1) {
//                    SentinelManager.INSTANCE.getSentinelsByRegion().get(region).remove(sentinelIndex);
//                }

                 /* log */
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("time_ms", System.currentTimeMillis());
                jsonObject.put("time", Helper.getCurrentDateTime());
                jsonObject.put("severity", "red");
                jsonObject.put("message", "Failed to create sentinel [" + sentinelProfile.getMachineName() + "].");
                SiteController.getWorksLog().add(jsonObject);
                LogHelper.Log("WORK", jsonObject.toString());
            } else {
                /* log */
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("time_ms", System.currentTimeMillis());
                jsonObject.put("time", Helper.getCurrentDateTime());
                jsonObject.put("severity", "green");
                jsonObject.put("message", "Create sentinel [" + sentinelProfile.getMachineName() + "] success.");
                SiteController.getWorksLog().add(jsonObject);
                LogHelper.Log("WORK", jsonObject.toString());
            }
        } catch (Exception e) {
            LogHelper.LogException("CreateSentinelTask", e);

            /* log */
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("time_ms", System.currentTimeMillis());
            jsonObject.put("time", Helper.getCurrentDateTime());
            jsonObject.put("severity", "red");
            jsonObject.put("message", "Failed to create sentinel [" + sentinelProfile.getMachineName() + "]. Exception := " + e.getMessage());
            SiteController.getWorksLog().add(jsonObject);
            LogHelper.Log("WORK", jsonObject.toString());
        }
    }
}
