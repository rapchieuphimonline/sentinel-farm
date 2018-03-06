package controllers;

import com.google.common.base.Strings;
import com.google.common.reflect.TypeToken;
import com.pezooworks.framework.log.LogHelper;
import controllers.common.Helper;
import controllers.shared.Cookies;
import controllers.shared.HumanProfile;
import controllers.shared.Target;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import play.mvc.Controller;
import play.mvc.Result;
//import views.html.*;
import views.html.*;

import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.List;

import static controllers.common.HttpHelper.USER_AGENTS;

/**
 * Created by Administrator on 19/12/2017.
 */
public class ControlCenter extends Controller {
    public static Result goRequest() {
        try {
            String action = request().getQueryString("action");
            switch (action) {
                case "view": {
                    String page = request().getQueryString("page");
                    String src = request().getQueryString("src");
                    return redirect("/" + src + "?page=" + page);
                }
                case "create-sentinel": {
                    GCPRegion gcpRegion = Compute.GCP_REGIONS[Helper.RANDOM_RANGE(0, Compute.GCP_REGIONS.length - 1)];
                    SentinelProfile sentinelProfile = Compute.createSentinel(gcpRegion.getName(), gcpRegion.randomZone());
                    if (sentinelProfile == null) {
                        return SiteController.goDashboard(null, "Create sentinel failed");
                    } else {
                        SiteController.getRunningSentinelMap().put(sentinelProfile.getMachineName(), sentinelProfile);
                        Compute.saveSentinel(Application.getDbKeyValue(), sentinelProfile);
                        Compute.saveSentinelMap(Application.getDbKeyValue(), SiteController.getRunningSentinelMap());
                        return SiteController.goDashboard("Create success! New sentinel name := " + sentinelProfile.getMachineName(), null);
                    }
                }
                case "delete-sentinel": {
                    String sentinelName = request().getQueryString("sentinel-name");
                    String src = request().getQueryString("src");

                    SentinelProfile sentinelProfile;
                    if (SiteController.getRunningSentinelMap().containsKey(sentinelName)) {
                        sentinelProfile = SiteController.getRunningSentinelMap().get(sentinelName);
                    } else {
                        String[] sa = sentinelName.split("-");
                        sentinelProfile = Compute.loadSentinel(Application.getDbKeyValue(), Long.parseLong(sa[sa.length - 1]));
                    }

                    boolean ret = Compute.deleteSentinel(sentinelProfile);
                    if (ret) {
                        if (src.equalsIgnoreCase("dashboard")) {
                            return SiteController.goDashboard("Delete success!", null);
                        } else {
                            return SiteController.goHistory("Delete success!", null);
                        }
                    } else {
                        if (src.equalsIgnoreCase("dashboard")) {
                            return SiteController.goDashboard(null, "Delete failed!");
                        } else {
                            return SiteController.goHistory(null, "Delete failed!");
                        }
                    }
                }
                case "set-config": {
                    String key = request().getQueryString("key");
                    String value = request().getQueryString("value");
                    SiteController.setConfig(key, value);
                    return SiteController.goSetting("Success! Set new config, key = " + key + ", value = " + value, null);
                }
                case "delete-ip-record": {
                    try {
                        String ipRecordID = request().getQueryString("ip-record-id");
                        if (!Strings.isNullOrEmpty(ipRecordID)) {
                            IpRecord ipRecord = Compute.loadIpRecord(Application.getDbKeyValue(), Long.parseLong(ipRecordID));
                            if (ipRecord != null) {
                                Application.getDbKeyValue().Delete("ip_record" + "_" + ipRecord.getId());
                                if (Compute.getIpRecordMap().containsKey(ipRecord.getIp())) {
                                    Compute.getIpRecordMap().remove(ipRecord.getIp());
                                }
                            }
                        }
                        return SiteController.goIp("Delete IP Success!", null);
                    } catch (Exception e) {
                        LogHelper.LogException("delete-ip-record", e);
                    }
                    return SiteController.goIp(null, "Delete IP Failed!");
                }
                case "sentinel-detail": {
                    String sentinelName = request().getQueryString("sentinel-name");
                    String[] sa = sentinelName.split("-");
                    SentinelProfile sentinelProfile = Compute.loadSentinel(Application.getDbKeyValue(), Long.parseLong(sa[sa.length - 1]));
                    if (sentinelProfile != null) {
                        return ok(detail.render("Detail", sentinelProfile));
                    }
                    return badRequest();
                }
                /* sentinels notify that the VM instance has started successfully */
                case "sentinel-service-start": {
                    try {
                        String ip = request().remoteAddress();
                        String sentinelName = request().getQueryString("sentinel-name");
                        LogHelper.Log("sentinelName := " + sentinelName + ", ip := " + ip);

                        SentinelProfile sentinelProfile;
                        if (SiteController.getRunningSentinelMap().containsKey(sentinelName)) {
                            sentinelProfile = SiteController.getRunningSentinelMap().get(sentinelName);
                        } else {
                            String[] sa = sentinelName.split("-");
                            sentinelProfile = Compute.loadSentinel(Application.getDbKeyValue(), Long.parseLong(sa[sa.length - 1]));
                        }
                        sentinelProfile.setServiceStartTime(Helper.getCurrentDateTime());
                        sentinelProfile.setServiceStartTimeMs(System.currentTimeMillis());
                        sentinelProfile.setIp(ip);
                        Compute.saveSentinel(Application.getDbKeyValue(), sentinelProfile);

                        /* record ip */
                        Compute.recordIP(ip, sentinelProfile.getZoneName());

                         /* log */
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("time_ms", System.currentTimeMillis());
                        jsonObject.put("time", Helper.getCurrentDateTime());
                        jsonObject.put("severity", "green");
                        jsonObject.put("message", "Sentinel [" + sentinelName + "] has started and started working. Sentinel IP := " + ip);
                        SiteController.getWorksLog().add(jsonObject);
                        LogHelper.Log("WORK", jsonObject.toString());
                        return ok();
                    } catch (Exception e) {
                        LogHelper.LogException("goRequest",e);
                    }
                    return badRequest();
                }

                /* sentinels send periodic heartbeat */
                case "sentinel-heartbeat": {
                    try {
                        String sentinelName = request().getQueryString("sentinel-name");
                        SentinelProfile sentinelProfile;
                        if (SiteController.getRunningSentinelMap().containsKey(sentinelName)) {
                            sentinelProfile = SiteController.getRunningSentinelMap().get(sentinelName);
                        } else {
                            String[] sa = sentinelName.split("-");
                            sentinelProfile = Compute.loadSentinel(Application.getDbKeyValue(), Long.parseLong(sa[sa.length - 1]));
                        }

                        sentinelProfile.setLastHeartBeatMs(System.currentTimeMillis());
                        Compute.saveSentinel(Application.getDbKeyValue(), sentinelProfile);

                        return ok();
                    } catch (Exception e) {
                        LogHelper.LogException("goRequest",e);
                    }
                    return badRequest();
                }
                /* sentinels request task !IMPORTANT */
                case "sentinel-request-task" : {
                    try {
                        String ip = request().remoteAddress();
                        String sentinelName = request().getQueryString("sentinel-name");
                        LogHelper.Log("sentinelName := " + sentinelName + ", ip := " + ip);

                        /* update sentinel status */
                        SentinelProfile sentinelProfile = null;
                        try {
                            if (SiteController.getRunningSentinelMap().containsKey(sentinelName)) {
                                sentinelProfile = SiteController.getRunningSentinelMap().get(sentinelName);
                            } else {
                                String[] sa = sentinelName.split("-");
                                sentinelProfile = Compute.loadSentinel(Application.getDbKeyValue(), Long.parseLong(sa[sa.length - 1]));
                            }
                        } catch (Exception e) {
                            LogHelper.LogException("goRequest", e);
                        }

                        boolean isNewHumanProfile;
                        HumanProfile humanProfile = Compute.loadHumanProfile(Application.getDbKeyValue(), ip);
                        if (humanProfile != null) {
                            isNewHumanProfile = false;
                        } else {
                            humanProfile = Compute.generateHumanProfile(ip);
                            isNewHumanProfile = true;
                            if (humanProfile == null) {
                                return badRequest();
                            }
                        }

                        if (sentinelProfile != null) {
                            if (isNewHumanProfile) {
                                sentinelProfile.setUseCount(0);
                            } else {
                                sentinelProfile.setUseCount(sentinelProfile.getUseCount() + 1);
                            }
                            Compute.saveSentinel(Application.getDbKeyValue(), sentinelProfile);
                        }

                        if (Strings.isNullOrEmpty(humanProfile.getUserAgent())) {
                            humanProfile.setUserAgent(USER_AGENTS[Helper.RANDOM_RANGE(0, USER_AGENTS.length - 1)]);
                            Compute.saveHumanProfile(Application.getDbKeyValue(), humanProfile);
                        }

                        /* set click ads rate */
                        humanProfile.setClickAdsRate(Double.parseDouble(SiteController.getConfig().get("CLICK_ADS_RATE")));
                        humanProfile.setAdsIdList(Compute.getConfigValueAsList("BANNER_ADS_ID"));

                        /* set direct ads list */
                        humanProfile.setDirectAdsList(Compute.getConfigValueAsList("DIRECT_ADS_ID"));
                        /* set shorten ads list */
                        humanProfile.setShortenAdsList(Compute.getConfigValueAsList("SHORTEN_ADS_ID"));
                        humanProfile.setShortenAdsSkipIdList(Compute.getConfigValueAsList("SHORTEN_ADS_SKIP_ID"));
                        /* reset some misc stuff */
                        humanProfile.setHumanActionList(Compute.getConfigValueAsList("HUMAN_ACTION_LIST"));
                        humanProfile.setNum_pageviews(Helper.RANDOM_RANGE(Integer.parseInt(SiteController.getConfig().get("MIN_NUM_PAGE_VIEWS")), Integer.parseInt(SiteController.getConfig().get("MAX_NUM_PAGE_VIEWS"))));
                        humanProfile.setMin_num_action(Integer.parseInt(SiteController.getConfig().get("MIN_NUM_HUMAN_ACTION")));
                        humanProfile.setMax_num_action(Integer.parseInt(SiteController.getConfig().get("MAX_NUM_HUMAN_ACTION")));
                        humanProfile.setMin_time_delay_schedule_ms(Integer.parseInt(SiteController.getConfig().get("MIN_TIME_DELAY_SCHEDULE_MS")));
                        humanProfile.setMax_time_delay_schedule_ms(Integer.parseInt(SiteController.getConfig().get("MAX_TIME_DELAY_SCHEDULE_MS")));
                        humanProfile.setMin_time_interval_schedule_ms(Integer.parseInt(SiteController.getConfig().get("MIN_TIME_INTERVAL_SCHEDULE_MS")));
                        humanProfile.setMax_time_interval_schedule_ms(Integer.parseInt(SiteController.getConfig().get("MAX_TIME_INTERVAL_SCHEDULE_MS")));

                        /* pop up misc (click skip ads) */
                        humanProfile.setMinSkipAdsAction(Integer.parseInt(SiteController.getConfig().get("SKIP_ADS_MIN_ACTION")));
                        humanProfile.setMaxSkipAdsAction(Integer.parseInt(SiteController.getConfig().get("SKIP_ADS_MAX_ACTION")));
                        humanProfile.setMinPercentX(Double.parseDouble(SiteController.getConfig().get("SKIP_ADS_MIN_PERCENT_X")));
                        humanProfile.setMaxPercentX(Double.parseDouble(SiteController.getConfig().get("SKIP_ADS_MAX_PERCENT_X")));
                        humanProfile.setMinPercentY(Double.parseDouble(SiteController.getConfig().get("SKIP_ADS_MIN_PERCENT_Y")));
                        humanProfile.setMaxPercentY(Double.parseDouble(SiteController.getConfig().get("SKIP_ADS_MAX_PERCENT_Y")));

                        /* add target list */
                        List<Target> targetList = Compute.generateTargetList(humanProfile);
                        humanProfile.getTargets().clear();
                        for (Target target : targetList) {
                            humanProfile.getTargets().add(target);
                        }

                        /* log */
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("time_ms", System.currentTimeMillis());
                        jsonObject.put("time", Helper.getCurrentDateTime());
                        jsonObject.put("severity", "green");
                        jsonObject.put("message", "Sentinel [" + sentinelName + "] requests task. Sentinel IP := " + ip);
                        SiteController.getWorksLog().add(jsonObject);
                        LogHelper.Log("WORK", jsonObject.toString());
                        return ok(Helper.gson.toJson(humanProfile));
                    } catch (Exception e) {
                        LogHelper.LogException("goRequest",e);
                    }
                    return badRequest();
                }
                case "sentinel-notify-cookies": {
                    try {
                        String ip = request().remoteAddress();
                        String sentinelName = request().getQueryString("sentinel-name");
                        String targetId = request().getQueryString("target-id");
                        String encodedCookies = request().getQueryString("cookies");
                        String cookies = URLDecoder.decode(encodedCookies, "UTF-8");
                        LogHelper.Log("sentinelName := " + sentinelName + ", ip := " + ip + ", cookies := " + cookies);

                        /* update human profile */
                        try {
                            HumanProfile humanProfile = Compute.loadHumanProfile(Application.getDbKeyValue(), ip);
                            Type type = new TypeToken<List<Cookies>>(){}.getType();
                            List<Cookies> cookiesList = Helper.gson.fromJson(cookies, type);
                            for (Cookies tmp : cookiesList) {
                                humanProfile.getCookies().add(tmp);
                            }
                            Compute.saveHumanProfile(Application.getDbKeyValue(), humanProfile);
                        } catch (Exception e) {
                        }

                        /* log */
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("time_ms", System.currentTimeMillis());
                        jsonObject.put("time", Helper.getCurrentDateTime());
                        jsonObject.put("severity", "green");
                        jsonObject.put("message", "Sentinel [" + sentinelName + "] sends cookies. Sentinel IP := " + ip + ". Cookies := " + cookies);
                        SiteController.getWorksLog().add(jsonObject);
                        LogHelper.Log("WORK", jsonObject.toString());

                        return ok();
                    } catch (Exception e) {
                        LogHelper.LogException("goRequest",e);
                    }
                    return badRequest();
                }
                case "sentinel-report": {
                    try {
                        String ip = request().remoteAddress();
                        String sentinelName = request().getQueryString("sentinel-name");
                        String encodedReport = request().getQueryString("report");
                        String report = URLDecoder.decode(encodedReport, "UTF-8");
                        LogHelper.Log("sentinelName := " + sentinelName + ", ip := " + ip + ", report := " + report);

                        /* log */
                        JSONObject jsonObject = new JSONObject();
                        jsonObject.put("time_ms", System.currentTimeMillis());
                        jsonObject.put("time", Helper.getCurrentDateTime());
                        jsonObject.put("severity", "green");
                        jsonObject.put("message", "Sentinel [" + sentinelName + "] report. Sentinel IP := " + ip + ". Report := " + report);
                        SiteController.getWorksLog().add(jsonObject);
                        LogHelper.Log("WORK", jsonObject.toString());

                        try {
                            int pageLoad = 0;
                            int popupLoad = 0;
                            int clickAds = 0;
                            JSONParser jsonParser = new JSONParser();
                            JSONObject jsonReport = (JSONObject)jsonParser.parse(report);
                            if (jsonReport.containsKey("pageLoad")) {
                                pageLoad = Integer.parseInt(jsonReport.get("pageLoad") + "");
                            }
                            if (jsonReport.containsKey("popupLoad")) {
                                popupLoad = Integer.parseInt(jsonReport.get("popupLoad") + "");
                            }
                            if (jsonReport.containsKey("clickAds")) {
                                clickAds = Integer.parseInt(jsonReport.get("clickAds") + "");
                            }

                            /* log */
                            JSONObject jsReport = new JSONObject();
                            jsReport.put("action", "sentinel_report");
                            jsReport.put("time_ms", System.currentTimeMillis());
                            jsReport.put("time", Helper.getCurrentDateTime());
                            jsReport.put("severity", "green");
                            jsReport.put("message", report);
                            jsReport.put("sentinelIp", ip);
                            jsReport.put("sentinelName", sentinelName);
                            jsReport.put("pageLoad", pageLoad);
                            jsReport.put("popupLoad", popupLoad);
                            jsReport.put("clickAds", clickAds);
                            LogHelper.Log("REPORT", jsReport.toString());
                        } catch (Exception e) {
                            LogHelper.LogException("readReport",e);
                        }
                        return ok();
                    } catch (Exception e) {
                        LogHelper.LogException("goRequest",e);
                    }
                    return badRequest();
                }
                /* sentinels report when all assigned tasks are completed */
                case "sentinel-finished": {
                    try {
                        String ip = request().remoteAddress();
                        String sentinelName = request().getQueryString("sentinel-name");
                        LogHelper.Log("sentinelName := " + sentinelName + ", ip := " + ip);

                        /* update sentinel status */
                        SentinelProfile sentinelProfile = null;
                        if (SiteController.getRunningSentinelMap().containsKey(sentinelName)) {
                            sentinelProfile = SiteController.getRunningSentinelMap().get(sentinelName);
                        } else {
                            String[] sa = sentinelName.split("-");
                            sentinelProfile = Compute.loadSentinel(Application.getDbKeyValue(), Long.parseLong(sa[sa.length - 1]));
                        }

                        if (sentinelProfile != null) {
                            sentinelProfile.setFinishedTime(Helper.getCurrentDateTime());
                            sentinelProfile.setFinishedTimeMs(System.currentTimeMillis());
                            sentinelProfile.setFinished(true);
                            Compute.saveSentinel(Application.getDbKeyValue(), sentinelProfile);
//                            Compute.deleteSentinel(sentinelProfile);

                            /* log */
                            JSONObject jsonObject = new JSONObject();
                            jsonObject.put("time_ms", System.currentTimeMillis());
                            jsonObject.put("time", Helper.getCurrentDateTime());
                            jsonObject.put("severity", "green");
                            jsonObject.put("message", "Sentinel [" + sentinelName + "] has finished all tasks. Sentinel IP := " + ip);
                            SiteController.getWorksLog().add(jsonObject);
                            LogHelper.Log("WORK", jsonObject.toString());

                            return ok();
                        }
                    } catch (Exception e) {
                        LogHelper.LogException("goRequest",e);
                    }
                    return badRequest();
                }
            }
        } catch (Exception e) {
            LogHelper.LogException("goRequest", e);
        }
        return redirect(routes.SiteController.goDashboard());
    }

    private static List<Cookies> readCookies(String cookies) {
        String[] aos = cookies.split(";");
        for (String s : aos) {
            if (s.contains("domain_name")) {
                String domainName = s.replace("domain_name=", "");
            }
        }
        return null;
    }

    public static Result doRequest() {
        try {
            String action = request().getQueryString("action");
//            LogHelper.Log("action := " + action);
            switch (action) {
            }
        } catch (Exception e) {
            LogHelper.LogException("doRequest", e);
        }
        return ok();
    }
}
