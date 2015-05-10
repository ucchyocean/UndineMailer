/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.undine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * メッセージリソース管理クラス
 * @author ucchy
 */
public class Messages {

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
        File file = new File(configFolder, filename);
        if ( !file.exists() ) {
            try {
                defaultMessages.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        resources = loadUTF8YamlFile(file);

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
        message = message.replace("\\n", "\n");
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
     * @param lang デフォルト言語
     */
    protected static void initialize(File _jar, File _configFolder, String lang) {

        jar = _jar;
        configFolder = _configFolder;

        if ( !configFolder.exists() ) {
            configFolder.mkdirs();
        }

        // コンフィグフォルダにメッセージファイルがまだ無いなら、コピーしておく
        for ( String filename : new String[]{
                "messages_en.yml", "messages_ja.yml", "messages_de.yml"} ) {
            File file = new File(configFolder, filename);
            if ( !file.exists() ) {
                Utility.copyFileFromJar(jar, file, filename, true);
            }
        }

        // デフォルトメッセージを、jarファイル内からロードする
        defaultMessages = new YamlConfiguration();
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jar);
            ZipEntry zipEntry = jarFile.getEntry(String.format("messages_%s.yml", lang));
            if ( zipEntry == null ) {
                zipEntry = jarFile.getEntry("messages_en.yml");
            }
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
     * UTF8エンコードのYamlファイルから、内容をロードする。
     * @param file ファイル
     * @return ロードされたYamlデータ
     */
    private static YamlConfiguration loadUTF8YamlFile(File file) {

        YamlConfiguration config = new YamlConfiguration();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file),"UTF-8"));
            String line;
            while ( (line = reader.readLine()) != null ) {
                if ( line.trim().startsWith("#") || !line.contains(":")) {
                    continue;
                }
                String[] temp = line.split(":");
                if ( temp.length < 2 ) {
                    continue;
                }
                String key = temp[0].trim();
                StringBuffer buffer = new StringBuffer();
                for ( int i=1; i<temp.length; i++ ) {
                    if ( buffer.length() > 0 ) {
                        buffer.append(":");
                    }
                    buffer.append(temp[i]);
                }
                String value = buffer.toString().trim();
                if ( value.startsWith("'") && value.endsWith("'") ) {
                    value = value.substring(1, value.length() - 1);
                }
                value = value.replace("''", "'");
                config.set(key, value);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( reader != null ) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }
        return config;
    }

    /**
     * 指定された言語でリロードを行う。
     * @param lang 言語
     */
    protected static void reload(String lang) {
        instance = new Messages(String.format("messages_%s.yml", lang));
    }
}
