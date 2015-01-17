/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.undine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * メッセージリソース管理クラス
 * @author ucchy
 */
public class Messages {

    private static final String DEFAULT_FILE_NAME = "messages_en.yml";

    private static YamlConfiguration defaultMessages;
    private static File configFolder;
    private static File jar;

    private static Messages instance;

    private YamlConfiguration resources;

    /**
     * コンストラクタ
     * @param filename メッセージファイル
     */
    private Messages(String filename) {

        // メッセージファイルをロード
        if ( filename == null ) {
            filename = DEFAULT_FILE_NAME;
        }
        File file = new File(configFolder, filename);
        if ( !file.exists() ) {
            file = new File(configFolder, DEFAULT_FILE_NAME);
        }
        resources = YamlConfiguration.loadConfiguration(file);

        // デフォルトメッセージをデフォルトとして足す。
        resources.addDefaults(defaultMessages);
    }

    /**
     * リソースを取得する
     * @param key リソースキー
     * @return リソース
     */
    public static String get(String key) {
        String message = instance.resources.getString(key);
        if ( message == null ) return "";
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * リソースを、キーワード置き換えつつ取得する
     * @param key リソースキー
     * @param keyword キーワード
     * @param value キーワードの置き換え値
     * @return リソース
     */
    public static String get(String key, String keyword, String value) {
        String message = get(key).replace(keyword, value);
        return message;
    }

    /**
     * リソースを、キーワード置き換えつつ取得する
     * @param key リソースキー
     * @param keyword キーワード
     * @param value キーワードの置き換え値
     * @return リソース
     */
    public static String get(String key, String keyword, int value) {
        String message = get(key).replace(keyword, value + "");
        return message;
    }

    /**
     * リソースを、キーワード置き換えつつ取得する
     * @param key リソースキー
     * @param keys キーワード
     * @param values キーワードの置き換え値
     * @return リソース
     */
    public static String get(String key, String[] keys, String[] values) {

        String message = get(key);

        for ( int index=0; index<keys.length; index++ ) {
            if ( values.length < (index + 1) ) continue;
            message = message.replace(keys[index], values[index]);
        }

        return message;
    }

    /**
     * Jarファイル内から直接 messages_en.yml を読み込み、
     * defaultMessagesとしてロードする。
     * @param _jar jarファイル
     * @param _configFolder コンフィグフォルダ
     */
    protected static void initialize(File _jar, File _configFolder) {

        jar = _jar;
        configFolder = _configFolder;

        // コンフィグフォルダにメッセージファイルがまだ無いなら、コピーしておく
        for ( String filename : new String[]{"messages_en.yml", "messages_ja.yml"} ) {
            File file = new File(configFolder, filename);
            if ( !file.exists() ) {
                Utility.copyFileFromJar(jar, file, filename, false);
            }
        }

        // デフォルトメッセージを、jarファイル内からロードする
        defaultMessages = new YamlConfiguration();
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jar);
            ZipEntry zipEntry = jarFile.getEntry(DEFAULT_FILE_NAME);
            InputStream inputStream = jarFile.getInputStream(zipEntry);
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line;
            while ( (line = reader.readLine()) != null ) {
                if ( line.contains(":") && !line.startsWith("#") ) {
                    String key = line.substring(0, line.indexOf(":")).trim();
                    String value = line.substring(line.indexOf(":") + 1).trim();
                    if ( value.startsWith("'") && value.endsWith("'") )
                        value = value.substring(1, value.length()-1);
                    defaultMessages.set(key, value);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( jarFile != null ) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }
    }

    /**
     * 指定された言語でリロードを行う。
     * @param lang 言語
     */
    protected static void reload(String lang) {
        instance = new Messages(String.format("messages_%s.yml", lang));
    }
}
