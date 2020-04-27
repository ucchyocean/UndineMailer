/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2020
 * This file was copied from at.pcgamingfreaks.UUIDConverter class.
 */
package org.bitbucket.ucchy.undine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    private static Map<String, String> UUID_CACHE = new HashMap<String, String>();

    public static String getNameFromUUID(String uuid) {
        NameChange[] names = getNamesFromUUID(uuid);
        if ( names == null ) return null;
        return names[names.length - 1].name;
    }

    public static NameChange[] getNamesFromUUID(String uuid) {
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

    public static String getUUIDFromName(String name, Date lastKnownDate) {
        String uuid;
        uuid = getOnlineUUID(name, lastKnownDate);
        if (uuid == null) return null;

        if (!uuid.contains("-")) {
            uuid = uuid.replaceAll(UUID_FORMAT_REGEX, UUID_FORMAT_REPLACE_TO);
        }
        return uuid;
    }

    private static String getOnlineUUID(String name, Date at) {
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

    public static Map<String, String> getUUIDsFromNames(Collection<String> names) {
        Map<String, String> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, UUID> entry : getUUIDsFromNamesAsUUIDs(names).entrySet()) {
            result.put(entry.getKey(), entry.getValue().toString());
        }
        return result;
    }

    public static Map<String, UUID> getUUIDsFromNamesAsUUIDs(Collection<String> names) {
        List<String> batch = new ArrayList<>(BATCH_SIZE);
        Iterator<String> players = names.iterator();
        Map<String, UUID> result = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
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
                                //TODO: better fail handling
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

    /**
     * A helper class to store the name changes and dates
     */
    public static class NameChange {

        /**
         * The name to which the name was changed
         */
        public String name;

        /**
         * DateTime of the name change in UNIX time (without milliseconds)
         */
        public long changedToAt;

        /**
         * Gets the date of a name change
         *
         * @return Date of the name change
         */
        public Date getChangeDate() {
            return new Date(changedToAt);
        }
    }

    private static class Profile {
        public String id;
        public String name;

        public UUID getUUID() {
            return UUID.fromString(id.replaceAll(UUID_FORMAT_REGEX, UUID_FORMAT_REPLACE_TO));
        }
    }
}
