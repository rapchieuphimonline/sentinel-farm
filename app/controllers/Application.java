package controllers;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.pezooworks.db.gaedatastore.GAEDataStore;
import com.pezooworks.db.redis.DBRedis;
import com.pezooworks.framework.common.dbkeyvalue.DBKeyValue;
import com.pezooworks.framework.log.LogHelper;
import com.pezooworks.framework.taskqueue.Task;
import com.pezooworks.framework.taskqueue.TaskQueue;
import org.json.simple.JSONObject;
import pezooworks.db.filestore.DBFileStore;
import play.mvc.Controller;
import play.mvc.Result;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Application extends Controller {
    public static String machineName;
    public static TaskQueue taskQueue;
    public static ConcurrentHashMap<String, Task> delayedTask;

    public static String DATABASE_TYPE = "file_local";

    private static DBKeyValue dbKeyValue = null;
    private static DBKeyValue dbMemcache = null;

    public static String USE_MEMCACHE = "no";
    public static String MEMCACHE_ADDRESS = "localhost";
    public static String MEMCACHE_PORT = "9999";
    public static String MEMCACHE_PASSWORD = "no";

    public static String FARM_NAME = "Unnamed Farm";

    public static void startServer() {
        try {
            System.out.println("### user.dir: " + System.getProperty("user.dir"));
            System.out.println("### java.library.path: " + System.getProperty("java.library.path"));
            machineName = System.getProperty("user.name") + "@" + InetAddress.getLocalHost().getHostName();
            taskQueue = new TaskQueue(9999, 2, 3);
            delayedTask = new ConcurrentHashMap<String, Task>();

            /* load log config */
            LogHelper.setUseDefaultLog(false);
            LogHelper.setUseConsoleOutput(false);
            LogHelper.LoadConfig("log4j.properties");

            /* read app config */
            readConfig();

            /* init database */
            initDatabase();

            /* start schedule tasks */
            startScheduleTask();

            /* start sentinel manager */
            SentinelManager.INSTANCE.startExecute();
            LogHelper.Log("on startServer... OK! System property := " + System.getProperty("user.dir"));
        } catch (Exception e) {
            LogHelper.LogException("Exception in start server. System exit", e);
            System.exit(1);
        }
    }


    private static void startScheduleTask() {
        LogHelper.Log("on startScheduleTask... ");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    handleDelayedTask();
                } catch (Exception e) {
                    LogHelper.LogException("scheduledTask", e);
                }
            }
        }, 5, 5, TimeUnit.SECONDS);
    }

    private static void initDatabase() {
        /* init dbKeyValue */
        if (DATABASE_TYPE.equalsIgnoreCase("file_local")) {
            HashMap<String, Object> MBServerConfig = new HashMap<String, Object>();
            MBServerConfig.put("TimeOut", new Integer(5000));
            MBServerConfig.put("folder", "/database_mb/");
            DBKeyValue db = new DBFileStore();
            db.SetConfigs(MBServerConfig);
            db.Connect();
            dbKeyValue = db;
        } else if (DATABASE_TYPE.equalsIgnoreCase("gae_datastore")) {
            dbKeyValue = new GAEDataStore();
            dbKeyValue.Connect();
        }

        /* init dbKeyValue memcache */
        if (USE_MEMCACHE.equalsIgnoreCase("yes") || USE_MEMCACHE.equalsIgnoreCase("enable")) {
            dbMemcache = new DBRedis();
            ((DBRedis)dbMemcache).flushAllDatabase();
            boolean connectRedis = ((DBRedis)dbMemcache).Connect(MEMCACHE_ADDRESS, Integer.parseInt(MEMCACHE_PORT), MEMCACHE_PASSWORD);
            LogHelper.Log("Connect redis result... " + (connectRedis ? "success" : "failed"));
        }
    }

    private static void readConfig() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("app_config.cfg"), Charsets.UTF_8);
            for (String line : lines) {
                if (Strings.isNullOrEmpty(line) || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                String key = line.split("=")[0];
                String value = line.split("=")[1];
                switch (key) {
                    case "DATABASE_TYPE":
                        DATABASE_TYPE = value;
                        break;
                    case "USE_MEMCACHE": {
                        USE_MEMCACHE = value;
                        break;
                    }
                    case "MEMCACHE_ADDRESS": {
                        MEMCACHE_ADDRESS = value;
                        break;
                    }
                    case "MEMCACHE_PORT": {
                        MEMCACHE_PORT = value;
                        break;
                    }
                    case "MEMCACHE_PASSWORD": {
                        MEMCACHE_PASSWORD = value;
                        break;
                    }
                    case "FARM_NAME": {
                        FARM_NAME = value;
                        break;
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Err! Can not find the app_config.cfg file. Application will run with default config");
            LogHelper.LogException("readConfig", e);
        }
    }

    public static String getHeaderRequest() {
        try {
            Map<String, String[]> headers = request().headers();
            JSONObject jsonObject = new JSONObject();
            if (headers != null) {
                for (Map.Entry<String, String[]> entry : headers.entrySet()) {
                    jsonObject.put(entry.getKey(), entry.getValue()[0]);
                }
            }
            return jsonObject.toString();
        } catch (Exception e) {
            return "Empty";
        }
    }

    private static void handleDelayedTask() {
        try {
            /* move from delay task to queue task */
            for (Map.Entry<String, Task> entry : delayedTask.entrySet()) {
                String taskName = entry.getKey();
                Task task = entry.getValue();
                taskQueue.AddTask(task);
                delayedTask.remove(taskName);
                LogHelper.Log("handleDelayedTask.. moved task [" + taskName + "] to task queue, remain delayed task := " + delayedTask.size());
                break;
            }
        } catch (Exception e) {
            LogHelper.LogException("handleDelayedTask", e);
        }
    }

    public static Result index() {
        return ok(getHeaderRequest());
    }

    public static DBKeyValue getDbKeyValue() {
        return dbKeyValue;
    }

    public static boolean useMemcache() {
        return "yes".equalsIgnoreCase(USE_MEMCACHE);
    }

    public static DBKeyValue getDbMemcache() {
        return dbMemcache;
    }
}
