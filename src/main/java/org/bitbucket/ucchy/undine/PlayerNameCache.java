/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * プレイヤー名のキャッシュ管理クラス
 * @author ucchy
 */
public class PlayerNameCache {

    private static final String FILENAME = "playercache.yml";
    private HashMap<String, MailSender> playerCache;
    private boolean isPlayerCacheLoaded;

    private PlayerNameCache() {
        playerCache = new HashMap<String, MailSender>();
        isPlayerCacheLoaded = false;
    }

    protected static PlayerNameCache load() {

        long start = System.currentTimeMillis();

        File file = new File(UndineMailer.getInstance().getDataFolder(), FILENAME);
        if ( !file.exists() ) {
            // ファイルがまだ存在しないなら、ここで空ファイルを作成する。
            YamlConfiguration temp = new YamlConfiguration();
            try {
                temp.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        PlayerNameCache cache = new PlayerNameCache();
        YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);

        for ( String name : conf.getKeys(false) ) {
            MailSender sender = MailSender.getMailSenderFromString(conf.getString(name));
            cache.playerCache.put(name, sender);
        }

        UndineMailer.getInstance().getLogger().info("Load offline player data from cache... Done. Time: "
                + (System.currentTimeMillis() - start) + "ms, Data: " + cache.playerCache.size() + ".");

        return cache;
    }

    protected void save() {

        File file = new File(UndineMailer.getInstance().getDataFolder(), FILENAME);

        YamlConfiguration conf = new YamlConfiguration();
        for ( String name : playerCache.keySet() ) {
            conf.set(name, playerCache.get(name).toString());
        }

        try {
            conf.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void refresh() {

        final long start = System.currentTimeMillis();
        isPlayerCacheLoaded = false;

        new BukkitRunnable() {
            public void run() {
                HashMap<String, MailSender> temp = new HashMap<String, MailSender>();
                for ( OfflinePlayer player : Bukkit.getOfflinePlayers() ) {
                    MailSender ms = MailSenderPlayer.getMailSenderFromString(player.getName());
                    if ( ms != null && ms.isValidDestination() ) {
                        temp.put(player.getName(), ms);
                    }
                }
                UndineMailer.getInstance().getLogger().info("Async refresh offline player data... Done. Time: "
                        + (System.currentTimeMillis() - start) + "ms, Data: " + temp.size() + ".");
                playerCache = temp;
                isPlayerCacheLoaded = true;

                save();
            }
        }.runTaskAsynchronously(UndineMailer.getInstance());
    }

    protected HashMap<String, MailSender> getCache() {
        return playerCache;
    }

    /**
     * プレイヤーキャッシュがロードされているかどうかを返す
     * @return プレイヤーキャッシュがロードされているかどうか
     */
    protected boolean isPlayerCacheLoaded() {
        return isPlayerCacheLoaded;
    }
}
