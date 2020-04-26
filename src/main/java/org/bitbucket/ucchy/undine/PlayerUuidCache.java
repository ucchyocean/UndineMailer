package org.bitbucket.ucchy.undine;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bitbucket.ucchy.undine.bridge.PCGFPluginLibBridge;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * プレイヤーのUUIDのキャッシュを管理するクラス
 * @author ucchy
 */
public class PlayerUuidCache {

    private HashMap<String, PlayerUuidCacheData> caches;
    private boolean isPlayerCacheLoaded;

    private PlayerUuidCache() {
        caches = new HashMap<String, PlayerUuidCacheData>();
        isPlayerCacheLoaded = false;
    }

    protected static PlayerUuidCache load() {

        PlayerUuidCache puc = new PlayerUuidCache();
        long start = System.currentTimeMillis();

        File folder = UndineMailer.getInstance().getCacheFolder();
        File[] children =  folder.listFiles();
        if ( children != null ) {
            for ( File file : children ) {
                if ( !file.getName().endsWith(".yml") ) {
                    continue;
                }
                PlayerUuidCacheData cache = PlayerUuidCacheData.load(file);
                puc.caches.put(cache.getName(), cache);
            }
        }

        UndineMailer.getInstance().getLogger().info("Load offline player data from cache... Done. Time: "
                + (System.currentTimeMillis() - start) + "ms, Data: " + puc.caches.size() + ".");

        return puc;
    }

    protected void refresh() {

        final long start = System.currentTimeMillis();
        isPlayerCacheLoaded = false;

        new BukkitRunnable() {
            public void run() {

                boolean onlineMode = UndineMailer.getInstance().getUndineConfig().isUuidOnlineMode();

                HashMap<String, PlayerUuidCacheData> temp = new HashMap<String, PlayerUuidCacheData>();
                for ( OfflinePlayer player : Bukkit.getOfflinePlayers() ) {

                    String name = player.getName();
                    if ( name == null ) continue;

                    String uuid = null;
                    PlayerUuidCacheData data = null;
                    if ( caches.containsKey(name) ) {
                        data = caches.get(name);
                        if ( isBefore30Days(data.getLastKnownDate()) ) {
                            uuid = PCGFPluginLibBridge.getUUIDFromName(name, onlineMode, true, new Date(0));
                            if ( uuid == null ) continue;
                            data = new PlayerUuidCacheData(name, uuid, new Date());
                            data.save();
                        }
                    } else {
                        uuid = PCGFPluginLibBridge.getUUIDFromName(name, onlineMode, true, new Date(0));
                        if ( uuid == null ) continue;
                        data = new PlayerUuidCacheData(name, uuid, new Date());
                        data.save();
                    }

                    temp.put(name, data);
                }

                UndineMailer.getInstance().getLogger().info("Async refresh offline player data... Done. Time: "
                        + (System.currentTimeMillis() - start) + "ms, Data: " + temp.size() + ".");
                caches = temp;
                isPlayerCacheLoaded = true;
            }
        }.runTaskAsynchronously(UndineMailer.getInstance());
    }

    /**
     * プレイヤーキャッシュがロードされているかどうかを返す
     * @return プレイヤーキャッシュがロードされているかどうか
     */
    protected boolean isPlayerCacheLoaded() {
        return isPlayerCacheLoaded;
    }

    /**
     * キャッシュしているプレイヤー名の一覧を返す
     * @return プレイヤー名一覧
     */
    protected  Set<String> getPlayerNames() {
        return caches.keySet();
    }

    /**
     * キャッシュされているすべてのUUIDを取得する
     * @return すべてのUUID
     */
    protected HashSet<String> getPlayerUuids() {
        HashSet<String> uuids = new HashSet<>();
        for ( PlayerUuidCacheData d : caches.values() ) {
            uuids.add(d.getUuid());
        }
        return uuids;
    }

    /**
     * 指定されたプレイヤー名のUUIDをキャッシュから取得する
     * @param name プレイヤー名
     * @return UUID
     */
    protected String getUUID(String name) {
        if ( caches.containsKey(name) ) {
            return caches.get(name).getUuid();
        }
        return refreshPlayerUuid(name);
    }

    /**
     * 指定されたプレイヤー名のUUIDを更新する
     * @param name プレイヤー名
     */
    private String refreshPlayerUuid(String name) {

        boolean onlineMode = UndineMailer.getInstance().getUndineConfig().isUuidOnlineMode();
        String uuid = null;
        PlayerUuidCacheData data = null;
        if ( caches.containsKey(name) ) {
            data = caches.get(name);
            if ( isBefore30Days(data.getLastKnownDate()) ) {
                uuid = PCGFPluginLibBridge.getUUIDFromName(name, onlineMode, true, new Date(0));
                if ( uuid == null ) return null;
                data = new PlayerUuidCacheData(name, uuid, new Date());
                caches.put(name, data);
                data.save();
            }
        } else {
            uuid = PCGFPluginLibBridge.getUUIDFromName(name, onlineMode, true, new Date(0));
            if ( uuid == null ) return null;
            data = new PlayerUuidCacheData(name, uuid, new Date());
            caches.put(name, data);
            data.save();
        }

        return uuid;
    }

    /**
     * 指定されたプレイヤー名のUUIDを、非同期スレッドで更新する
     * @param name プレイヤー名
     */
    protected void asyncRefreshPlayerUuid(String name) {

        new BukkitRunnable() {
            public void run() {
                refreshPlayerUuid(name);
            }
        }.runTaskAsynchronously(UndineMailer.getInstance());
    }

    private static boolean isBefore30Days(Date date) {
        return date.before(new Date(System.currentTimeMillis() - 1000L*24*3600* 30));
    }
}
