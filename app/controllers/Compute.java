package controllers;

import com.google.api.client.repackaged.com.google.common.base.Strings;
import com.google.gson.reflect.TypeToken;
import com.pezooworks.framework.common.dbkeyvalue.DBKeyValue;
import com.pezooworks.framework.log.LogHelper;
import controllers.common.Helper;
import controllers.shared.Cookies;
import controllers.shared.HumanProfile;
import controllers.shared.Target;
import controllers.task.CreateSentinelTask;
import controllers.task.DeleteSentinelTask;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static controllers.common.HttpHelper.USER_AGENTS;

/**
 * Created by LAP11313-local on 12/15/2017.
 */
public class Compute {
    /* execution script */
    public static final String PATH_TO_CREATE_SENTINEL_SCRIPT = "/usr/local/scripts/create_sentinel.sh";
    public static final String PATH_TO_DELETE_SENTINEL_SCRIPT = "/usr/local/scripts/delete_sentinel.sh";
    public static final GCPRegion[] GCP_REGIONS = new GCPRegion[] {
            new GCPRegion("us-central1", new String[] {"us-central1-a", "us-central1-b", "us-central1-c", "us-central1-f"}),
            new GCPRegion("us-west1", new String[] {"us-west1-a", "us-west1-b", "us-west1-c"}),
            new GCPRegion("us-east1", new String[] {"us-east1-b", "us-east1-c", "us-east1-d"}),
            new GCPRegion("us-east4", new String[] {"us-east4-a", "us-east4-b", "us-east4-c"}),
            new GCPRegion("southamerica-east1", new String[] {"southamerica-east1-a", "southamerica-east1-b", "southamerica-east1-c"}),
            new GCPRegion("europe-west1", new String[] {"europe-west1-b", "europe-west1-c", "europe-west1-d"}),
            new GCPRegion("europe-west2", new String[] {"europe-west2-a", "europe-west2-b", "europe-west2-c"}),
            new GCPRegion("europe-west3", new String[] {"europe-west3-a", "europe-west3-b", "europe-west3-c"}),
            new GCPRegion("asia-southeast1", new String[] {"asia-southeast1-a", "asia-southeast1-b"}),
            new GCPRegion("asia-northeast1", new String[] {"asia-northeast1-a", "asia-northeast1-b", "asia-northeast1-c"}),
            new GCPRegion("asia-east1", new String[] {"asia-east1-a", "asia-east1-b", "asia-east1-c"}),
            new GCPRegion("asia-south1", new String[] {"asia-south1-a", "asia-south1-b", "asia-south1-c"}),
            new GCPRegion("australia-southeast1", new String[] {"australia-southeast1-a", "australia-southeast1-b", "australia-southeast1-c"})
    };

    /* human profile config */
    public static final String NEW_SENTINEL_KEY = "new_sentinel_id";
    public static final int DISPLAY_SENTINEL_PER_PAGE = 36;
//    public static final String[] TARGET_URLS = new String[] {"http://phimcity.com"};
    public static String[] ANCHOR_IDS = new String[]{"cat_body","cat_group","item_img","item_name","item_description"
            ,"footer_tag","nav_bar_logo_top","nav_bar_logo","nav_bar","quick_search","quick_view"
            ,"quick_view_item","right_side_menu","right_side_slide_item_img","right_side_slide_item_nameVi"
            ,"right_side_slide_item_nameEn","right_side_small_group_item_img","right_side_small_group_item_nameVi"
            ,"right_side_small_group_item_nameEn","right_side_tag","search_result","suggestion_item"
            ,"suggestion_group","suggestion_group_btn","top_bar","video_description"};
    public static String[] ADS_IDS = new String[] {"banner_ads"};
    public static List<String> HUMAN_ACTION_LIST = Arrays.asList("SCROLL", "MOUSE_MOVE");

    /* moved to config file */
    public static final String SENTINEL_TEMPLATE_NAME = "sentinel-template-v2"; // moved to config file
    public static final int MIN_NUM_PAGE_VIEWS = 2;
    public static final int MAX_NUM_PAGE_VIEWS = 4;
    public static final int MIN_NUM_HUMAN_ACTION = 1;
    public static final int MAX_NUM_HUMAN_ACTION = 5;
    public static final int MIN_TIME_DELAY_SCHEDULE_MS = 1000;
    public static final int MAX_TIME_DELAY_SCHEDULE_MS = 3000;
    public static final int MIN_TIME_INTERVAL_SCHEDULE_MS = 2000;
    public static final int MAX_TIME_INTERVAL_SCHEDULE_MS = 5000;
    /* moved to config file */

    public static Map<String, IpRecord> ipRecordMap = null;

    public static SentinelProfile createSentinel(String region, String zone) {
        try {
            long newSentinelId = Helper.simpleGenerateIDForKey(Application.getDbKeyValue(), NEW_SENTINEL_KEY);
            if (newSentinelId == -1) {
                return null;
            }

            SentinelProfile sentinelProfile = new SentinelProfile(newSentinelId);
            sentinelProfile.setTemplateName(SiteController.getConfig().get("SENTINEL_TEMPLATE_NAME"));
            sentinelProfile.setZoneName(zone);
            sentinelProfile.setRegionName(region);
            sentinelProfile.setCreateTime(Helper.getCurrentDateTime());
            sentinelProfile.setCreateTimeMs(System.currentTimeMillis());
            saveSentinel(Application.getDbKeyValue(), sentinelProfile);

            CreateSentinelTask createSentinelTask = new CreateSentinelTask(sentinelProfile);
            Application.taskQueue.AddTask(createSentinelTask);
            return sentinelProfile;
        } catch (Exception e) {
            LogHelper.LogException("createSentinel", e);
        }
        return null;
    }

    public static boolean deleteSentinel(SentinelProfile sentinelProfile) {
        try {
            DeleteSentinelTask deleteSentinelTask = new DeleteSentinelTask(sentinelProfile);
            Application.delayedTask.put("delete-sentinel-" + sentinelProfile.getMachineName(), deleteSentinelTask);
            return true;
        } catch (Exception e) {
            LogHelper.LogException("deleteSentinel", e);
        }
        return false;
    }

    public static String since(IpRecord ipRecord) {
        String since = Helper.since(System.currentTimeMillis() - ipRecord.getLastUsedTimeMs());
        return since + " ago.";
    }

    public static String since(SentinelProfile sentinelProfile) {
        String since = Helper.since(System.currentTimeMillis() - sentinelProfile.getCreateTimeMs());
        return since + " ago.";
    }

    public static boolean heartbeat(SentinelProfile sentinelProfile) {
        long duration = System.currentTimeMillis() - sentinelProfile.getLastHeartBeatMs();
        return duration < 10000;
    }

    public static void saveSentinel(DBKeyValue dbKeyValue, SentinelProfile sentinelProfile) {
        String asJson = Helper.gson.toJson(sentinelProfile);
        dbKeyValue.Set("sentinel_profile" + "_" + sentinelProfile.getId(), asJson);
        if (Application.useMemcache()) {
            Application.getDbMemcache().Set("sentinel_profile" + "_" + sentinelProfile.getId(), asJson);
        }
        LogHelper.Log("SENTINEL_PROFILE", asJson);
    }

    public static SentinelProfile loadSentinel(DBKeyValue dbKeyValue, long id) {
        if (Application.useMemcache()) {
            try {
                Object obj = Application.getDbMemcache().Get("sentinel_profile" + "_" + id);
                if (obj != null) {
                    return Helper.gson.fromJson((String)obj, SentinelProfile.class);
                }
            } catch (Exception e) {
                LogHelper.LogException("loadSentinel", e);
            }
        }

        try {
            if (dbKeyValue == null) {
                return null;
            }
            Object obj = dbKeyValue.Get("sentinel_profile" + "_" + id);
            if (obj != null) {
                if (Application.useMemcache()) {
                    Application.getDbMemcache().Set("sentinel_profile" + "_" + id, (String)obj);
                }
                return Helper.gson.fromJson((String) obj, SentinelProfile.class);
            }
        } catch (Exception e) {
            LogHelper.LogException("loadSentinel", e);
        }
        return null;
    }

    public static void saveFarmRegion(DBKeyValue dbKeyValue, FarmRegion farmRegion) {
        String asJson = Helper.gson.toJson(farmRegion);
        dbKeyValue.Set("farm_region" + "_" + farmRegion.getRegionName(), asJson);
        if (Application.useMemcache()) {
            Application.getDbMemcache().Set("farm_region" + "_" + farmRegion.getRegionName(), asJson);
        }
    }

    public static FarmRegion loadFarmRegion(DBKeyValue dbKeyValue, String regionName) {
        if (Application.useMemcache()) {
            try {
                Object obj = Application.getDbMemcache().Get("farm_region" + "_" + regionName);
                if (obj != null) {
                    return Helper.gson.fromJson((String)obj, FarmRegion.class);
                }
            } catch (Exception e) {
                LogHelper.LogException("loadFarmRegion", e);
            }
        }

        try {
            if (dbKeyValue == null) {
                return null;
            }
            Object obj = dbKeyValue.Get("farm_region" + "_" + regionName);
            if (obj != null) {
                if (Application.useMemcache()) {
                    Application.getDbMemcache().Set("farm_region" + "_" + regionName, (String)obj);
                }
                return Helper.gson.fromJson((String) obj, FarmRegion.class);
            }
        } catch (Exception e) {
            LogHelper.LogException("loadFarmRegion", e);
        }
        return null;
    }

    /**
     * Generate human profile base on sentinel's IP
     * @param ip
     * @return HumanProfile
     */
    public static HumanProfile generateHumanProfile(String ip) {
        HumanProfile humanProfile = new HumanProfile(ip);
        humanProfile.setUserAgent(USER_AGENTS[Helper.RANDOM_RANGE(0, USER_AGENTS.length - 1)]);
        humanProfile.setWebview_width(Helper.RANDOM_RANGE(800, 1023));
        humanProfile.setWebview_height(Helper.RANDOM_RANGE(600, 767));
        humanProfile.setNum_pageviews(Helper.RANDOM_RANGE(Integer.parseInt(SiteController.getConfig().get("MIN_NUM_PAGE_VIEWS"))
                , Integer.parseInt(SiteController.getConfig().get("MAX_NUM_PAGE_VIEWS"))));
        humanProfile.setHumanActionList(Compute.getConfigValueAsList("HUMAN_ACTION_LIST"));
        humanProfile.setMin_num_action(Integer.parseInt(SiteController.getConfig().get("MIN_NUM_HUMAN_ACTION")));
        humanProfile.setMax_num_action(Integer.parseInt(SiteController.getConfig().get("MAX_NUM_HUMAN_ACTION")));
        humanProfile.setMin_time_delay_schedule_ms(Integer.parseInt(SiteController.getConfig().get("MIN_TIME_DELAY_SCHEDULE_MS")));
        humanProfile.setMax_time_delay_schedule_ms(Integer.parseInt(SiteController.getConfig().get("MAX_TIME_DELAY_SCHEDULE_MS")));
        humanProfile.setMin_time_interval_schedule_ms(Integer.parseInt(SiteController.getConfig().get("MIN_TIME_INTERVAL_SCHEDULE_MS")));
        humanProfile.setMax_time_interval_schedule_ms(Integer.parseInt(SiteController.getConfig().get("MAX_TIME_INTERVAL_SCHEDULE_MS")));

        /* add default cookies */
        List<Cookies> cookiesList = generateDefaultCookies();
        for (Cookies cookies : cookiesList) {
            humanProfile.getCookies().add(cookies);
        }

        /* add target list */
        List<Target> targetList = generateTargetList(humanProfile);
        for (Target target : targetList) {
            humanProfile.getTargets().add(target);
        }

        saveHumanProfile(Application.getDbKeyValue(), humanProfile);
        return humanProfile;
    }

    public static List<Cookies> generateDefaultCookies() {
        List<Cookies> cookiesList = new ArrayList<>();
        List<String> targetsUrl = getConfigValueAsList("TARGET_URLS");
        for (String url : targetsUrl) {
            Cookies cookies = new Cookies();
            cookies.setCookies_domain(url);
            cookies.setCookies_info(buildDefaultCookiesInfo());
            cookiesList.add(cookies);
        }
        return cookiesList;
    }

    public static List<Target> generateTargetList(HumanProfile humanProfile) {
        List<Target> ret = new ArrayList<>();
        List<String> targetsUrl = getConfigValueAsList("TARGET_URLS");
        List<String> targetCustomParameter = getConfigValueAsList("TARGET_URLS_PARAMS");
        for (String url : targetsUrl) {
            for (String param : targetCustomParameter) {
                if (param.contains(":")) {
                    String targetUrl = param.split(":")[0];
                    String realParam = param.split(":")[1];
                    if (url.contains(targetUrl)) {
                        Target target = new Target(UUID.randomUUID() + "-" + Long.toString(System.currentTimeMillis()));
                        target.setUrl(buildUtmForURL(url, humanProfile.getIp(), realParam));
                        target.setAnchor_ids(Arrays.asList(ANCHOR_IDS));
                        ret.add(target);
                    }
                }
            }
        }
        return ret;
    }

    private static final String[] UTM_CAMPAIGN_LIST = new String[]{"google.com.vn", "facebook.com", "google.com",
    "youtube.com", "zing.vn", "vnexpress.net", "xnxx.com", "xvideos.com", " 24h.com.vn", "kenh14.vn",
    "news.zing.vn", "lazada.vn", "yahoo.com", "baomoi.com", "dantri.com.vn", "phimmoi.net", "vietnamnet.vn",
    "thegioididong.com", "mp3.zing.vn", " nhaccuatui.com", "shopee.vn", "vlxx.tv", "chotot.com", "xoso.me",
    "soha.vn", "tinhte.vn", "sendo.vn", "tuoitre.vn", "phimbathu.com"};

    private static String buildUtmForURL(String url, String ip, String customParameter) {
        StringBuilder sb = new StringBuilder();
        sb.append(url).append("/xem-phim/").append(Helper.RANDOM_RANGE(0, 700));
        sb.append("?").append("utm_campaign").append("=").append(UTM_CAMPAIGN_LIST[Helper.RANDOM_RANGE(0, UTM_CAMPAIGN_LIST.length - 1)]);
        sb.append("&").append("utm_source").append("=").append(ip);
        sb.append("&").append("utm_medium").append("=").append(Helper.getCurrentDateTime("yyyy-MM-dd"));
        sb.append("&").append("utm_term").append("=").append(customParameter);
        return sb.toString();
    }

    private static String buildDefaultCookiesInfo() {
        StringBuilder sb = new StringBuilder();
        try {
            if (SiteController.getConfig().containsKey("URLENCODED_DEFAULT_COOKIES")) {
                sb.append(URLDecoder.decode(SiteController.getConfig().get("URLENCODED_DEFAULT_COOKIES"), "UTF-8"));
            }
            sb.append(";").append("day=").append(Helper.getCurrentDateTime("yyyy-MM-dd")).append(";");
        } catch (UnsupportedEncodingException e) {
            LogHelper.LogException("buildDefaultCookiesInfo", e);
        }
        return sb.toString();
    }

    public static void saveHumanProfile(DBKeyValue dbKeyValue, HumanProfile humanProfile) {
        String asJson = Helper.gson.toJson(humanProfile);
        dbKeyValue.Set("human_profile" + "_" + humanProfile.getIp(), asJson);
        if (Application.useMemcache()) {
            Application.getDbMemcache().Set("human_profile" + "_" + humanProfile.getIp(), asJson);
        }
        LogHelper.Log("HUMAN_PROFILE", asJson);
    }

    public static HumanProfile loadHumanProfile(DBKeyValue dbKeyValue, String ip) {
        if (Application.useMemcache()) {
            try {
                Object obj = Application.getDbMemcache().Get("human_profile" + "_" + ip);
                if (obj != null) {
                    return Helper.gson.fromJson((String)obj, HumanProfile.class);
                }
            } catch (Exception e) {
                LogHelper.LogException("loadHumanProfile", e);
            }
        }

        try {
            if (dbKeyValue == null) {
                return null;
            }
            Object obj = dbKeyValue.Get("human_profile" + "_" + ip);
            if (obj != null) {
                if (Application.useMemcache()) {
                    Application.getDbMemcache().Set("human_profile" + "_" + ip, (String)obj);
                }
                return Helper.gson.fromJson((String) obj, HumanProfile.class);
            }
        } catch (Exception e) {
            LogHelper.LogException("loadHumanProfile", e);
        }
        return null;
    }

    public static String getLastHeartBeat(SentinelProfile sentinelProfile) {
        try {
            if (sentinelProfile.getLastHeartBeatMs() == 0) {
                return "Never";
            }

            long duration = System.currentTimeMillis() - sentinelProfile.getLastHeartBeatMs();
            String since = Helper.since(duration);
            return since + " ago";
        } catch (Exception e) {
            return "Never";
        }
    }



    public static void recordIP(String ip, String zone) {
        IpRecord ipRecord = null;
        if (getIpRecordMap().containsKey(ip)) {
            ipRecord = getIpRecordMap().get(ip);
        } else {
            long newID = Helper.simpleGenerateIDForKey(Application.getDbKeyValue(), "new_iprecord_id");
            ipRecord = new IpRecord(newID);
            ipRecord.setCreatedTime(Helper.getCurrentDateTime());
            ipRecord.setCreatedTimeMs(System.currentTimeMillis());
        }

        ipRecord.setIp(ip);
        ipRecord.addZone(zone);
        ipRecord.increaseCount();
        ipRecord.setLastUsedTimeMs(System.currentTimeMillis());
        saveIpRecord(Application.getDbKeyValue(), ipRecord);
        getIpRecordMap().put(ip, ipRecord);
    }

    public static Map<String, IpRecord> getIpRecordMap() {
        if (ipRecordMap == null) {
            ipRecordMap = loadIpRecords(Application.getDbKeyValue());
        }
        return ipRecordMap;
    }

    private static void saveIpRecord(DBKeyValue dbKeyValue, IpRecord ipRecord) {
        try {
            String asJson = Helper.gson.toJson(ipRecord);
            dbKeyValue.Set("ip_record" + "_" + ipRecord.getId(), asJson);
            if (Application.useMemcache()) {
                Application.getDbMemcache().Set("ip_record" + "_" + ipRecord.getId(), asJson);
            }
        } catch (Exception e) {
            LogHelper.LogException("saveIpRecord", e);
        }
    }

    public static IpRecord loadIpRecord(DBKeyValue dbKeyValue, long id) {
        if (Application.useMemcache()) {
            try {
                Object obj = Application.getDbMemcache().Get("ip_record" + "_" + id);
                if (obj != null) {
                    return Helper.gson.fromJson((String)obj, IpRecord.class);
                }
            } catch (Exception e) {
                LogHelper.LogException("loadIpRecord", e);
            }
        }

        try {
            if (dbKeyValue == null) {
                return null;
            }
            Object obj = dbKeyValue.Get("ip_record" + "_" + id);
            if (obj != null) {
                if (Application.useMemcache()) {
                    Application.getDbMemcache().Set("ip_record" + "_" + id, (String)obj);
                }
                return Helper.gson.fromJson((String) obj, IpRecord.class);
            }
        } catch (Exception e) {
            LogHelper.LogException("loadIpRecord", e);
        }
        return null;
    }

    private static Map<String, IpRecord> loadIpRecords(DBKeyValue dbKeyValue) {
        try {
            if (dbKeyValue == null) {
                return null;
            }
            Map<String, IpRecord> ipRecordMap = new ConcurrentHashMap<String, IpRecord>();
            long lastID = Helper.getLastIDForKey(dbKeyValue, "new_iprecord_id");
            for (long i = lastID; i >= 0; i--) {
                IpRecord ipRecord = loadIpRecord(dbKeyValue, i);
                if (ipRecord != null) {
                    if (ipRecord.getIp() != null) {
                        ipRecordMap.put(ipRecord.getIp(), ipRecord);
                    }
                }
            }

            return ipRecordMap;
        } catch (Exception e) {
            LogHelper.LogException("loadIpRecords", e);
        }
        return new ConcurrentHashMap<String, IpRecord>();
    }

    public static long getIpUseCount(String ip) {
        try {
            if (Strings.isNullOrEmpty(ip)) {
                return -1;
            }

            if (getIpRecordMap().containsKey(ip)) {
                IpRecord ipRecord = getIpRecordMap().get(ip);
                return ipRecord.getCount();
            }
        } catch (Exception e) {
            LogHelper.LogException("getIpUseCount", e);
        }
        return -1;
    }

    public static String getZoneInRegion(String key) {
        return null;
    }

    public static GCPRegion getRegion(String region) {
        for (GCPRegion gcpRegion : GCP_REGIONS) {
            if (gcpRegion.getName().equalsIgnoreCase(region)) {
                return gcpRegion;
            }
        }
        return null;
    }

    public static void saveSentinelMap(DBKeyValue dbKeyValue, Map<String, SentinelProfile> sentinelProfileMap) {
        try {
            String asJson = Helper.gson.toJson(sentinelProfileMap);
            dbKeyValue.Set("current_sentinel_map", asJson);
            if (Application.useMemcache()) {
                Application.getDbMemcache().Set("current_sentinel_map", asJson);
            }
        } catch (Exception e) {
            LogHelper.LogException("saveSentinelMap", e);
        }
    }

    public static Map<String, SentinelProfile> loadSentinelMap(DBKeyValue dbKeyValue) {
        if (Application.useMemcache()) {
            try {
                Object obj = Application.getDbMemcache().Get("current_sentinel_map");
                if (obj != null) {
                    Type type = new TypeToken<ConcurrentHashMap<String, SentinelProfile>>(){}.getType();
                    return Helper.gson.fromJson((String)obj, type);
                }
            } catch (Exception e) {
                LogHelper.LogException("loadSentinelMap", e);
            }
        }

        try {
            if (dbKeyValue == null) {
                return null;
            }
            Object obj = dbKeyValue.Get("current_sentinel_map");
            if (obj != null) {
                if (Application.useMemcache()) {
                    Application.getDbMemcache().Set("current_sentinel_map", (String)obj);
                }
                Type type = new TypeToken<ConcurrentHashMap<String, SentinelProfile>>(){}.getType();
                return Helper.gson.fromJson((String) obj, type);
            }
        } catch (Exception e) {
            LogHelper.LogException("loadSentinelMap", e);
        }
        return new ConcurrentHashMap<String, SentinelProfile>();
    }

    public static List<String> getConfigValueAsList(String configKey) {
        List<String> ret = new ArrayList<>();
        String adsIds = SiteController.getConfig().get(configKey);
        if (!Strings.isNullOrEmpty(adsIds)) {
            String[] tmp = adsIds.split(";");
            for (String id : tmp) {
                if (!Strings.isNullOrEmpty(id)) {
                    ret.add(id);
                }
            }
        }
        return ret;
    }

    public static int loadDailyCounter(DBKeyValue dbKeyValue) {
        if (Application.useMemcache()) {
            try {
                Object obj = Application.getDbMemcache().Get("daily_count_sentinel");
                if (obj != null) {
                    return Integer.parseInt((String)obj);
                }
            } catch (Exception e) {
                LogHelper.LogException("loadDailyCounter", e);
            }
        }

        try {
            if (dbKeyValue == null) {
                return 0;
            }
            Object obj = dbKeyValue.Get("daily_count_sentinel");
            if (obj != null) {
                if (Application.useMemcache()) {
                    Application.getDbMemcache().Set("daily_count_sentinel", (String)obj);
                }
                return Integer.parseInt((String)obj);
            }
        } catch (Exception e) {
            LogHelper.LogException("loadDailyCounter", e);
        }
        return 0;
    }

    public static void saveDailyCounter(DBKeyValue dbKeyValue, int counter) {
        String asJson = Integer.toString(counter);
        dbKeyValue.Set("daily_count_sentinel", asJson);
        if (Application.useMemcache()) {
            Application.getDbMemcache().Set("daily_count_sentinel", asJson);
        }
    }
}
