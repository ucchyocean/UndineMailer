package org.bitbucket.ucchy.undine;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

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

    protected void load() {

        long start = System.currentTimeMillis();

        File folder = UndineMailer.getInstance().getCacheFolder();

        for ( File file : folder.listFiles() ) {

            if ( !file.getName().endsWith(".yml") ) {
                continue;
            }

            PlayerUuidCacheData cache = PlayerUuidCacheData.load(file);
            caches.put(cache.getName(), cache);
        }

        UndineMailer.getInstance().getLogger().info("Load offline player data from cache... Done. Time: "
                + (System.currentTimeMillis() - start) + "ms, Data: " + caches.size() + ".");
    }

    protected void refresh() {

        final long start = System.currentTimeMillis();
        isPlayerCacheLoaded = false;

        new BukkitRunnable() {
            public void run() {

                boolean onlineMode = UndineMailer.getInstance().getUndineConfig().isUuidOnlineMode();

                HashMap<String, PlayerUuidCacheData> temp = new HashMap<String, PlayerUuidCacheData>();
                for ( OfflinePlayer player : Bukkit.getOfflinePlayers() ) {

                    String uuid = null;
                    if ( caches.containsKey(player.getName()) ) {
                        PlayerUuidCacheData data = caches.get(player.getName());
                        uuid = PCGFPluginLibBridge.getUUIDFromName(data.getName(), onlineMode, data.getLastKnownDate());
                    } else {
                        uuid = PCGFPluginLibBridge.getUUIDFromName(player.getName(), onlineMode, null);
                    }
                    PlayerUuidCacheData newData = new PlayerUuidCacheData(player.getName(), uuid, new Date());
                    temp.put(player.getName(), newData);
                    newData.save();
                }

                UndineMailer.getInstance().getLogger().info("Async refresh offline player data... Done. Time: "
                        + (System.currentTimeMillis() - start) + "ms, Data: " + temp.size() + ".");
                caches = temp;
                isPlayerCacheLoaded = true;
            }
        }.runTaskAsynchronously(UndineMailer.getInstance());
    }

    protected HashMap<String, PlayerUuidCacheData> getCache() {
        return caches;
    }

    /**
     * プレイヤーキャッシュがロードされているかどうかを返す
     * @return プレイヤーキャッシュがロードされているかどうか
     */
    protected boolean isPlayerCacheLoaded() {
        return isPlayerCacheLoaded;
    }
}
