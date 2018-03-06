package controllers.common;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.gson.Gson;
import com.pezooworks.framework.common.dbkeyvalue.DBKeyValue;
import com.pezooworks.framework.log.LogHelper;
import controllers.SentinelProfile;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by LAP11313-local on 7/3/2017.
 */
public class Helper {
    public static Gson gson = new Gson();
    public static Random random = new Random();

    public static String getCurrentDateTime() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    public static String getCurrentDateTime(String format) {
        return new SimpleDateFormat(format).format(Calendar.getInstance().getTime());
    }

    public static long MILLISECONDS_OF_1_1_2010() {
        return 1262325600000L;
    }

    public static long MILLISECONDS() {
        return System.currentTimeMillis() - MILLISECONDS_OF_1_1_2010();
    }

    public static int SECONDS() {
        return (int)(MILLISECONDS() / 1000L);
    }

    public static int RANDOM_RANGE(int min, int max) {
        try {
            if (min == max) return min;
            return random.nextInt(max - min + 1) + min;
        } catch (Exception e) {
            throw e;
        }
    }

    public static String simpleHashMD5(String s) {
        return Hash(s, "MD5");
    }

    public static String Hash(String str, String algorithm) {
        try {
            MessageDigest digester = MessageDigest.getInstance(algorithm);
            byte[] bytes = digester.digest(str.getBytes());
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < bytes.length; i++) {
                int val = 0xff & bytes[i];
                if (val <= 0x0f) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(val));
            }
            return sb.toString();
        } catch (Exception e) {
        }
        return null;
    }

    public static int MurmurHash(String key)
    {
        byte[] data = key.getBytes(StandardCharsets.US_ASCII);

        int len = data.length;

        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;

        int h1 = 123456;
        int roundedEnd = (len & 0xfffffffc);  // round down to 4 byte block

        for (int i = 0; i < roundedEnd; i += 4)
        {
            // little endian load order
            int k1 = (data[i] & 0xff) | ((data[i+1] & 0xff) << 8) | ((data[i+2] & 0xff) << 16) | (data[i+3] << 24);
            k1 *= c1;
            k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
            k1 *= c2;

            h1 ^= k1;
            h1 = (h1 << 13) | (h1 >>> 19);  // ROTL32(h1,13);
            h1 = h1*5+0xe6546b64;
        }

        // tail
        int k1 = 0;

        switch (len & 0x03)
        {
            case 3:
                k1 = (data[roundedEnd + 2] & 0xff) << 16;
                // fallthrough
            case 2:
                k1 |= (data[roundedEnd + 1] & 0xff) << 8;
                // fallthrough
            case 1:
                k1 |= (data[roundedEnd] & 0xff);
                k1 *= c1;
                k1 = (k1 << 15) | (k1 >>> 17);  // ROTL32(k1,15);
                k1 *= c2;
                h1 ^= k1;
        }

        // finalization
        h1 ^= len;

        // fmix(h1);
        h1 ^= h1 >>> 16;
        h1 *= 0x85ebca6b;
        h1 ^= h1 >>> 13;
        h1 *= 0xc2b2ae35;
        h1 ^= h1 >>> 16;

        return h1;
    }

    public static List<String> executeBashScript(String script) {
        try {
            String command = script;
            CommandLine commandLine = CommandLine.parse(command);
            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
            ExecuteWatchdog watchdog = new ExecuteWatchdog(60000);
            DefaultExecutor defaultExecutor = new DefaultExecutor();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
            defaultExecutor.setStreamHandler(streamHandler);
            defaultExecutor.execute(commandLine);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray())));
            String line = null;
            List<String> content = new ArrayList<String>();
            while ((line = bufferedReader.readLine()) != null) {
                LogHelper.Log("executeBashScript output: " + line);
                content.add(line);
            }
            return content;
        } catch (Exception e) {
            LogHelper.LogException("executeBashScript", e);
        }
        return null;
    }

    /**
     * Read single line file, return first line if file has multiple lines
     * @param pathToFile path to file
     * @return String
     */
    public static String readFirstLine(String pathToFile) {
        try {
            List<String> lines = com.google.common.io.Files.readLines(new File(pathToFile), Charsets.UTF_8);
            if (lines.size() > 0) {
                return lines.iterator().next();
            }
        } catch (IOException e) {
            LogHelper.LogException("readFirstLine", e);
        }
        return null;
    }

    public static File simpleDownloadFile(String httpLink, String pathToOutputFile) {
        try {
            URL url=new URL(httpLink);
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:31.0) Gecko/20100101 Firefox/31.0");
            conn.connect();

            File result = new File(pathToOutputFile);
            FileUtils.copyInputStreamToFile(conn.getInputStream(), result);
            return result;
        } catch (Exception e) {
            LogHelper.LogException("simpleDownloadFile", e);
        }
        return null;
    }

    public static String removeAccent(String s) {
        try {
            return VNCharacterUtils.removeAccent(s);
        } catch (Exception e) {
            LogHelper.LogException("Helper", e);
        }
        return s;
    }
    /**
     *  Get seconds left to the specific time (hour:minute)
     *  if current time passed the specific time, get the specific time of tomorrow
     * @param hour 0 -> 24
     * @param minute 0 -> 60
     * @return seconds to the specific time
     */
    public static long secondsToSpecificHourOfDay(int hour, int minute, String timezone) {
        Date currentTime = Calendar.getInstance(TimeZone.getTimeZone(timezone)).getTime();
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone(timezone));
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        Date specificTime = calendar.getTime();
        long timeDiff = (specificTime.getTime() - currentTime.getTime()) / 1000;
        if (timeDiff < 0) { // if current time has passed the specific time, get the next day
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            specificTime = calendar.getTime();
            timeDiff = (specificTime.getTime() - currentTime.getTime()) / 1000;
        }
        LogHelper.Log("secondsToSpecificHourOfDay.. to next " + hour + ":" + minute + " := " + timeDiff + " (timezone " + timezone + ").");
        return timeDiff;
    }

    /**
     * Generate new id for key
     * Caution: not check duplicate yet
     *
     * @param dbKeyValue
     * @return new generate film id
     */
    public static long simpleGenerateIDForKey(DBKeyValue dbKeyValue, String key) {
        try {
            if (dbKeyValue == null) {
                return -1;
            }

            /* get current */
            Object obj = dbKeyValue.Get(key);
            long ret = 0L;
            if (obj != null) {
                String current = (String) obj;
                ret = Long.parseLong(current) + 1;
            }

            /* save */
            dbKeyValue.Set(key, Long.toString(ret));
            return ret;
        } catch (Exception e) {
            LogHelper.LogException("simpleGenerateIDForKey", e);
        }
        return -1;
    }

    /**
     * Get last id for key
     * @param dbKeyValue
     * @param key
     * @return
     */
    public static long getLastIDForKey(DBKeyValue dbKeyValue, String key) {
        try {
            if (dbKeyValue == null) {
                return -1;
            }

            /* get current */
            Object obj = dbKeyValue.Get(key);
            long ret = 0L;
            if (obj != null) {
                String current = (String)obj;
                ret = Long.parseLong(current);
            }
            return ret;
        } catch (Exception e) {
            LogHelper.LogException("getLastIDForKey", e);
        }
        return -1;
    }

    /**
     * Sort a map follow its key
     * @param map map to sort
     * @param ascendant order to sort, true is ascendant, false is descendant
     * @param <K>
     * @param <V>
     * @return a order sorted map
     */
    public static <K extends Comparable<? super K>, V> Map<K, V> sortMapByKey(Map<K, V> map, final boolean ascendant) {
        List<K> keys = new LinkedList<K>(map.keySet());
        Ordering<K> ordering = Ordering.from(new Comparator<K>() {
            @Override
            public int compare(K o1, K o2) {
                return -(o1.compareTo(o2));
            }
        });
        List<K> sortedKeys = ascendant ? ordering.immutableSortedCopy(keys).reverse() : ordering.immutableSortedCopy(keys);
        Map<K, V> ret = new LinkedHashMap<K, V>();
        for (K k : sortedKeys) {
            ret.put(k, map.get(k));
        }
        return ret;
    }

    public static List<SentinelProfile> sort(List<SentinelProfile> list, final boolean descendant) {
        Collections.sort(list, new Comparator<SentinelProfile>() {
            @Override
            public int compare(SentinelProfile o1, SentinelProfile o2) {
                if (descendant) {
                    return (o1.getId() > o2.getId()) ? 1 : -1;
                } else {
                    return (o1.getId() > o2.getId()) ? -1 : 1;
                }
            }
        });
        return list;
    }

    public static String sinceMs(long millis) {
        long elapsed = System.currentTimeMillis() - millis;
        return since(elapsed);
    }


    /*
    Get time passed since the a specific time
     */
    public static String since(long millis)
    {
        if (millis < 0 ) {
            return "";
        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder();
        sb.append(days);
        sb.append(" Days ");
        sb.append(hours);
        sb.append(" Hours ");
        sb.append(minutes);
        sb.append(" Minutes ");
        sb.append(seconds);
        sb.append(" Seconds");
        return sb.toString();
    }

    public static String shortenString(String input, int len) {
        if (Strings.isNullOrEmpty(input)) {
            return input;
        }
        try {
            if (input.length() < len) {
                return input;
            } else {
                return input.substring(0, len);
            }
        } catch (Exception e) {
            LogHelper.LogException("Helper", e);
        }
        return input;
    }

    public static Map<String, String> readConfigFile(String pathToConfigFile) {
        Map<String, String> ret = new HashMap<>();
        try {
            List<String> lines = FileUtils.readLines(new File(pathToConfigFile), Charsets.UTF_8);
            for (String line : lines) {
                if (line.startsWith("#") || line.startsWith("//")) {
                    continue;
                } else {
                    if (line.contains("=")) {
                        String key = line.split("=")[0];
                        String value = line.split("=")[1];
                        ret.put(key, value);
                    }
                }
            }
        } catch (Exception e) {
            LogHelper.LogException("readConfigFile", e);
        }
        return ret;
    }

    public static boolean writeConfigFile(Map<String, String> configs, String pathToFile) {
        try {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> config : configs.entrySet()) {
                sb.append(config.getKey()).append("=").append(config.getValue()).append("\n");
            }
            FileUtils.write(new File(pathToFile), sb.toString(), Charsets.UTF_8);
            return true;
        } catch (Exception e) {
            LogHelper.LogException("writeConfigFile", e);
        }
        return false;
    }
}
