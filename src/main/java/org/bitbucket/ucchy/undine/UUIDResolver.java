/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 * This file was copied from at.pcgamingfreaks.UUIDConverter class.
 */
package org.bitbucket.ucchy.undine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * UUIDとプレイヤー名の相互変換を行うためのクラス
 * @author ucchy
 */
public class UUIDResolver {

    private static final Pattern API_MAX_PROFILE_BATCH_SIZE_PATTERN = Pattern.compile(".*Not more that (?<batchSize>\\d+) profile name per call is allowed.*");
    private static final String UUID_FORMAT_REGEX = "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})";
    private static final String UUID_FORMAT_REPLACE_TO = "$1-$2-$3-$4-$5";
    private static final long MOJANG_QUERY_RETRY_TIME = 600000L;

    private static final Gson GSON = new Gson();
    private static final Map<String, String> UUID_CACHE = new HashMap<String, String>();

    private boolean onlineMode = false;

    /**
     * コンストラクタ
     */
    public UUIDResolver() {
        this(false);
    }

    /**
     * コンストラクタ
     * @param useUserCacheJson usercache.jsonからキャッシュの初期値を取得するかどうか
     */
    public UUIDResolver(boolean onlineMode) {
        this.onlineMode = onlineMode;
        if ( !onlineMode ) loadUserCache();
    }

    // usercache.jsonを、uuidCacheの初期値としてロードする
    private void loadUserCache() {

        // 既にロード済みなら何もしない
        if ( UUID_CACHE.size() > 0 ) return;

        int loaded = 0;
        File uuidCache = new File("usercache.json");
        if ( !uuidCache.exists() ) return;

        try (JsonReader reader = new JsonReader(new FileReader(uuidCache))) {
            CacheData[] dat = new Gson().fromJson(reader, CacheData[].class);
            Date now = new Date();
            for (CacheData d : dat) {
                if (now.before(d.getExpiresDate())) {
                    loaded++;
                    UUID_CACHE.put(d.name, d.uuid);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        System.out.println("Loaded " + loaded + " UUIDs from local cache.");
    }

    /**
     * UUIDから現在のプレイヤー名を取得する
     * @param uuid UUID
     * @return プレイヤー名（存在しないUUIDが指定された場合はnullになる）
     */
    protected String getNameFromUUID(String uuid) {
        if ( uuid == null ) return null;
        if ( onlineMode ) {
            NameChange[] names = getOnlineNamesFromUUID(uuid);
            if ( names == null ) return null;
            return names[names.length - 1].name;
        } else {
            // Bukkit pass-throgh mode.
            OfflinePlayer p = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
            if ( p == null ) return null;
            return p.getName();
        }
    }

    /**
     * UUIDからプレイヤー名履歴を取得する
     * @param uuid UUID
     * @return プレイヤー名の履歴（存在しないUUIDが指定された場合はnullになる）
     */
    private NameChange[] getOnlineNamesFromUUID(String uuid) {
        NameChange[] names = null;
        try {
            Scanner jsonScanner = new Scanner((new URL("https://api.mojang.com/user/profiles/" + uuid.replaceAll("-", "") + "/names")).openConnection().getInputStream(), "UTF-8");
            names = GSON.fromJson(jsonScanner.next(), NameChange[].class);
            jsonScanner.close();
        } catch(IOException e) {
            if (e.getMessage().contains("HTTP response code: 429")) {
                System.out.println("You have reached the request limit of the Mojang api! Please retry later!");
            } else {
                System.out.println("Looks like there is a problem with the connection with Mojang. Please retry later.");
            }
        } catch(Exception e) {
            System.out.println("Looks like there is no player with this uuid!\n UUID: \"" + uuid + "\"");
        }
        return names;
    }

    /**
     * プレイヤー名からUUIDを取得する。
     * 例えば、name=ucchy、lastKnownDate=2020/04/28を指定した場合は、2020/04/28の時点でucchyというプレイヤー名を使っていた人のUUIDを取得する。
     * @param name プレイヤー名
     * @param lastKnownDate 時点
     * @return UUID
     */
    protected String getUUIDFromName(String name, Date lastKnownDate) {
        if ( name == null ) return null;
        if ( onlineMode ) {
            String uuid = getOnlineUUID(name, lastKnownDate);
            if (uuid == null) return null;
            if (!uuid.contains("-")) {
                uuid = uuid.replaceAll(UUID_FORMAT_REGEX, UUID_FORMAT_REPLACE_TO);
            }
            return uuid;
        } else {
            // Bukkit pass-throgh mode.
            @SuppressWarnings("deprecation")
            OfflinePlayer p = Bukkit.getOfflinePlayer(name);
            if ( p == null ) return null;
            return p.getUniqueId().toString();
        }
    }

    // ネットワーク経由でUUIDを取得する
    private String getOnlineUUID(String name, Date at) {
        if ((at == null || at.after(new Date(System.currentTimeMillis() - 1000L*24*3600* 30))) && UUID_CACHE.containsKey(name)) {
            return UUID_CACHE.get(name);
        }

        String uuid = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                new URL("https://api.mojang.com/users/profiles/minecraft/" + name + ((at != null) ? "?at=" + (at.getTime()/1000L) : "")).openStream(), StandardCharsets.UTF_8))) {
            uuid = (((JsonObject) new JsonParser().parse(in)).get("id")).getAsString();
            if (uuid != null && (at == null || at.after(new Date(System.currentTimeMillis() - 1000L*24*3600* 30)))) {
                UUID_CACHE.put(name, uuid);
            }
        } catch(MalformedURLException e) {
            System.out.println("Failed to get uuid cause of a malformed url!\n Name: \"" + name + "\" Date: " + ((at != null) ? "?at=" + at.getTime()/1000L : "null"));
        } catch(IOException e) {
            if (e.getMessage().contains("HTTP response code: 429")) {
                System.out.println("You have reached the request limit of the mojang api! Please retry later!");
            } else {
                System.out.println("Looks like there is a problem with the connection with mojang. Please retry later.");
            }
        } catch(Exception e) {
            if(at == null) {
                // We can't resolve the uuid for the player
                System.out.println("Unable to get UUID for: " + name + "!");
            } else if(at.getTime() == 0) {
                // If it's not his first name maybe it's his current name
                System.out.println("Unable to get UUID for: " + name + " at 0! Trying without date!");
                uuid = getOnlineUUID(name, null);
            } else {
                // If we cant get the player with the date he was here last time it's likely that it is his first name
                System.out.println("Unable to get UUID for: " + name + " at " + at.getTime()/1000L + "! Trying at=0!");
                uuid = getOnlineUUID(name, new Date(0));
            }
        }
        return uuid;
    }

    private static int BATCH_SIZE = 10; // Limit from Mojang

    /**
     * 複数のプレイヤー名からUUIDをまとめて取得する
     * @param names プレイヤー名
     * @return UUID
     */
    protected Map<String, String> getUUIDsFromNames(Collection<String> names) {
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, UUID> entry : getUUIDsFromNamesAsUUIDs(names).entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

    /**
     * 複数のプレイヤー名からUUIDをまとめて取得する
     * @param names プレイヤー名
     * @return UUID
     */
    protected Map<String, UUID> getUUIDsFromNamesAsUUIDs(Collection<String> names) {

        Map<String, UUID> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        if ( !onlineMode ) {
            // Bukkit pass-throgh mode.
            for ( String name : names ) {
                @SuppressWarnings("deprecation")
                OfflinePlayer p = Bukkit.getOfflinePlayer(name);
                if ( p == null ) continue;
                result.put(name, p.getUniqueId());
            }
            return result;
        }

        List<String> batch = new ArrayList<>(BATCH_SIZE);
        Iterator<String> players = names.iterator();
        boolean success;
        int fromCache = 0, fromWeb = 0;
        while (players.hasNext()) {
            while (players.hasNext() && batch.size() < BATCH_SIZE) {
                String name = players.next();
                if (UUID_CACHE.containsKey(name)) {
                    result.put(name, UUID.fromString(UUID_CACHE.get(name).replaceAll(UUID_FORMAT_REGEX, UUID_FORMAT_REPLACE_TO)));
                    fromCache++;
                } else {
                    batch.add(name);
                }
            }

            do {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL("https://api.mojang.com/profiles/minecraft").openConnection();
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; encoding=UTF-8");
                    connection.setUseCaches(false);
                    connection.setDoInput(true);
                    connection.setDoOutput(true);
                    try (OutputStream out = connection.getOutputStream()) {
                        out.write(GSON.toJson(batch).getBytes(Charsets.UTF_8));
                    }
                    Profile[] profiles;
                    try (Reader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        profiles = GSON.fromJson(in, Profile[].class);
                    }
                    for (Profile profile : profiles) {
                        result.put(profile.name, profile.getUUID());
                        UUID_CACHE.put(profile.name, profile.getUUID().toString());
                        fromWeb++;
                    }
                } catch(IOException e) {
                    try {
                        if(connection != null) {
                            if(connection.getResponseCode() == 429) {
                                System.out.println("Reached the request limit of the mojang api!\nConverting will be paused for 10 minutes and then continue!");
                                Thread.sleep(MOJANG_QUERY_RETRY_TIME);
                                success = false;
                                continue;
                            } else {
                                InputStream errorStream = connection.getErrorStream();
                                StringBuilder errorBuilder = new StringBuilder();
                                int c;
                                while ((c = errorStream.read()) != -1) {
                                    errorBuilder.append((char) c);
                                }
                                String errorMessage = errorBuilder.toString();
                                System.out.println("Mojang responded with status code: " + connection.getResponseCode() + " Message: " + errorMessage);
                                Matcher matcher = API_MAX_PROFILE_BATCH_SIZE_PATTERN.matcher(errorMessage);
                                if (connection.getResponseCode() == 400 && matcher.matches()) {
                                    BATCH_SIZE = Integer.parseInt(matcher.group("batchSize"));
                                    System.out.println("Reducing batch size to " + BATCH_SIZE + " and try again ...");
                                    return getUUIDsFromNamesAsUUIDs(names);
                                } else {
                                    e.printStackTrace();
                                }
                            }
                        } else {
                            e.printStackTrace();
                        }
                    }
                    catch(InterruptedException | IOException ignore) {}

                    System.out.println("Could not convert all names to uuids because of an issue. Please check the log.");
                    return result;
                }
                batch.clear();
                success = true;
            } while(!success);
        }

        System.out.println("Converted " + (fromCache + fromWeb) + "/" + names.size() + " UUIDs (" + fromCache + " of them from the cache and " + fromWeb + " from Mojang).");
        return result;
    }

    public class NameChange {
        public String name;
        public long changedToAt;

        public Date getChangeDate() {
            return new Date(changedToAt);
        }
    }

    private class Profile {
        public String id;
        public String name;

        public UUID getUUID() {
            return UUID.fromString(id.replaceAll(UUID_FORMAT_REGEX, UUID_FORMAT_REPLACE_TO));
        }
    }

    private static class CacheData {
        public String name, uuid, expiresOn;

        public Date getExpiresDate() {
            try {
                return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z").parse(expiresOn);
            } catch(ParseException e) {
                e.printStackTrace();
            }
            return new Date(); // When we failed to parse the date we return the current time stamp
        }
    }
}
