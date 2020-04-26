package org.bitbucket.ucchy.undine;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * プレイヤーのUUIDキャッシュデータ
 * @author ucchy
 */
public class PlayerUuidCacheData {

    private String name;
    private String uuid;
    private Date lastKnownDate;

    /**
     * コンストラクタ
     * @param name
     * @param uuid
     * @param lastKnownDate
     */
    protected PlayerUuidCacheData(String name, String uuid, Date lastKnownDate) {
        this.name = name;
        this.uuid = uuid;
        this.lastKnownDate = lastKnownDate;
    }

    public static PlayerUuidCacheData load(File file) {

        YamlConfiguration conf = YamlConfiguration.loadConfiguration(file);
        String name = conf.getString("name");
        String uuid = conf.getString("uuid");
        Date date = parseDate(conf.getString("lastKnownDate"));

        return new PlayerUuidCacheData(name, uuid, date);
    }

    public void save() {

        File folder = UndineMailer.getInstance().getCacheFolder();
        YamlConfiguration conf = new YamlConfiguration();
        conf.set("name", name);
        conf.set("uuid", uuid);
        conf.set("lastKnownDate", formatDate(lastKnownDate));

        try {
            conf.save(new File(folder, uuid + ".yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Date parseDate(String src) {

        if ( src == null ) return null;

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");

        try {
            return format.parse(src);
        } catch (ParseException e) {
            // do nothing.
        }
        return null;
    }

    private String formatDate(Date date) {

        if ( date == null ) return null;

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        return format.format(date);
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @param uuid uuid
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @return lastKnownDate
     */
    public Date getLastKnownDate() {
        return lastKnownDate;
    }

    /**
     * @param lastKnownDate lastKnownDate
     */
    public void setLastKnownDate(Date lastKnownDate) {
        this.lastKnownDate = lastKnownDate;
    }
}
