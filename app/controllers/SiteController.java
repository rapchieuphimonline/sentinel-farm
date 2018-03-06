package controllers;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.common.base.Charsets;
import com.pezooworks.framework.log.LogHelper;
import controllers.authentication.Secured;
import controllers.common.Helper;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

//import views.html.*;
import views.html.*;

/**
 * Created by Administrator on 14/12/2017.
 */
@Security.Authenticated(Secured.class)
public class SiteController extends Controller {
    public static String notifyMessage, notifyError;
    public static Map<String, SentinelProfile> runningSentinelMap = null;
    private static Map<String, String> appConfigs;
    private static List<JSONObject> workLog = new LinkedList<>();

    public static void clearNotifyMessage() {
        notifyMessage = null;
        notifyError = null;
        LogHelper.Log("clearNotifyMessage... OK!");
    }

    public static Result goSetting() {
        return ok(setting.render("Setting"));
    }

    public static Result goSetting(String message, String error) {
        notifyMessage = message;
        notifyError = error;
        return redirect(routes.SiteController.goSetting());
    }

    public static Result goDashboard() {
        try {
            String page = request().getQueryString("page");
            if (!Strings.isNullOrEmpty(page)) {
                return goDashboard(Integer.parseInt(page));
            }
        } catch (Exception e) {
        }
        return ok(dashboard.render("Dashboard", "1"));
    }

    public static Result goDashboard(int page) {
        return ok(dashboard.render("Dashboard", Integer.toString(page)));
    }

    public static Result goDashboard(String message, String error) {
        notifyMessage = message;
        notifyError = error;
        return redirect(routes.SiteController.goDashboard());
    }

    public static Result goHistory() {
        try {
            String page = request().getQueryString("page");
            if (!Strings.isNullOrEmpty(page)) {
                return goHistory(Integer.parseInt(page));
            }
        } catch (Exception e) {
        }
        return ok(history.render("History", "1"));
    }

    public static Result goHistory(int page) {
        return ok(history.render("History", Integer.toString(page)));
    }

    public static Result goHistory(String message, String error) {
        notifyMessage = message;
        notifyError = error;
        return redirect(routes.SiteController.goHistory());
    }

    public static Result goLog() {
        return ok(log.render("Log"));
    }

    public static Result goLog(String message, String error) {
        notifyMessage = message;
        notifyError = error;
        return redirect(routes.SiteController.goLog());
    }

    public static Result goIp() {
        String zone = request().getQueryString("zone");
        String date = request().getQueryString("date");
        return ok(ip.render("Ip", "1", zone, date));
    }

    public static Result goIp(String message, String error) {
        notifyMessage = message;
        notifyError = error;
        return redirect(routes.SiteController.goIp());
    }

    public static Result goReport() {
        return ok(report.render("Report"));
    }

    public static Result goReport(String message, String error) {
        notifyMessage = message;
        notifyError = error;
        return redirect(routes.SiteController.goReport());
    }

    public static Result requestAjax() {
        String action = request().getQueryString("action");
        switch (action) {
            case "current-log":
                return ok(ajaxLogTable.render("current-log"));
            case "current-dashboard":
                String page = request().getQueryString("page");
                if (Strings.isNullOrEmpty(page)) {
                    page = "1";
                }
                return ok(ajaxDashboardTable.render(page));
        }
        return ok();
    }

    public static Result doDashboard() {
        return TODO;
    }

    public static List<JSONObject> getReport() {
        List<JSONObject> ret = new ArrayList<>();
        try {
            List<String> lines = FileUtils.readLines(new File("log/report.log"), Charsets.UTF_8);
            JSONParser jsonParser = new JSONParser();
            for (String line : lines) {
                ret.add((JSONObject)jsonParser.parse(line));
            }
            Collections.reverse(ret);
        } catch (Exception e) {
            LogHelper.LogException("getReport", e);
        }
        return ret;
    }

    public static Map<String, SentinelProfile> getRunningSentinelMap() {
        if (runningSentinelMap == null) {
            runningSentinelMap = Compute.loadSentinelMap(Application.getDbKeyValue());
        }
        return runningSentinelMap;
    }

    public static List<SentinelProfile> getRunningList(int page) {
        List<SentinelProfile> sorted = new ArrayList<>(getRunningSentinelMap().values());
        sorted = Helper.sort(sorted, false);
        int from = page * Compute.DISPLAY_SENTINEL_PER_PAGE;
        List<SentinelProfile> ret = new ArrayList<>();
        for (int i = from; i < sorted.size(); i++) {
            SentinelProfile sentinelProfile = sorted.get(i);
            if (sentinelProfile != null) {
                ret.add(sentinelProfile);
                if (ret.size() >= Compute.DISPLAY_SENTINEL_PER_PAGE) {
                    break;
                }
            }
        }
        return ret;
    }

    public static List<SentinelProfile> getHistoryList(int page) {
        long lastSentinelID = Helper.getLastIDForKey(Application.getDbKeyValue(), Compute.NEW_SENTINEL_KEY);
        long from = lastSentinelID - (page * Compute.DISPLAY_SENTINEL_PER_PAGE);
        List<SentinelProfile> ret = new ArrayList<>();
        for (long i = from; i >= 0; i--) {
            SentinelProfile sentinelProfile = Compute.loadSentinel(Application.getDbKeyValue(), i);
            if (sentinelProfile != null && !getRunningSentinelMap().containsKey(sentinelProfile.getMachineName())) {
                if (sentinelProfile != null) {
                    ret.add(sentinelProfile);
                    if (ret.size() >= Compute.DISPLAY_SENTINEL_PER_PAGE) {
                        break;
                    }
                }
            }
        }
        return ret;
    }

    public static List<JSONObject> getWorksLog() {
        if (workLog != null && workLog.size() > 300) {
            List<JSONObject> truncate = workLog.subList(workLog.size() - 100, workLog.size() - 1);
            workLog = new LinkedList<JSONObject>(truncate);
        }
        return workLog;
    }

    public static List<JSONObject> getLogs() {
        List<org.json.simple.JSONObject> ret = new LinkedList<JSONObject>(getWorksLog());
        Collections.reverse(ret);
        return ret;
    }

    public static void readWorkLogHistory() {
        try {
            List<String> lines = FileUtils.readLines(new File("log/work.log"), Charsets.UTF_8);
            JSONParser jsonParser = new JSONParser();
            for (String line : lines) {
                getWorksLog().add((JSONObject)jsonParser.parse(line));
            }
        } catch (Exception e) {
            LogHelper.LogException("readWorkLogHistory", e);
        }
    }

    public static List<IpRecord> getIpList(String filterZone, String filterDate) {
        try {
            List<IpRecord> sorted = new ArrayList<>(Compute.getIpRecordMap().values());

            /* filtered */
            List<IpRecord> filtered = new ArrayList<>(sorted);
            for (IpRecord ipRecord : sorted) {
                if (!Strings.isNullOrEmpty(filterZone)) {
                    if (!ipRecord.inZone(filterZone)) {
                        filtered.remove(ipRecord);
                    }
                }
                if (!Strings.isNullOrEmpty(filterDate)) {
                    if (filterDate.contains(" ")) {
                        filterDate = filterDate.substring(0, filterDate.indexOf(" ")).trim();
                    }

                    if (!ipRecord.inDate(filterDate)) {
                        filtered.remove(ipRecord);
                    }
                }
            }

//            /* sort */
//            Collections.sort(filtered, new Comparator<IpRecord>() {
//                @Override
//                public int compare(IpRecord o1, IpRecord o2) {
//                    if (o1.getCreatedTimeMs() > o2.getCreatedTimeMs()) {
//                        return -1;
//                    } else if (o1.getCreatedTimeMs() < o2.getCreatedTimeMs()) {
//                        return 1;
//                    } else {
//                        return 1;
//                    }
////                    return o1.getCount() > o2.getCount() ? 1 : -1;
//                }
//            });

            return filtered;
        } catch (Exception e) {
            LogHelper.LogException("getIpList", e);
        }
        return null;
    }

    private static final String PATH_TO_CONFIG_FILE = "appConfig.bat";
    public static Map<String, String> getConfig() {
        if (appConfigs == null) {
            appConfigs = Helper.readConfigFile(PATH_TO_CONFIG_FILE);
        }
        return appConfigs;
    }

    public static Map<String, String> setConfig(String key, String value) {
        Map<String, String> configs = Helper.readConfigFile(PATH_TO_CONFIG_FILE);
        configs.put(key, value);
        Helper.writeConfigFile(configs, PATH_TO_CONFIG_FILE);

        appConfigs = Helper.readConfigFile(PATH_TO_CONFIG_FILE);
        return appConfigs;
    }
}
