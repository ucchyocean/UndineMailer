/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.mc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * ユーティリティクラス
 * @author ucchy
 */
public class Utility {

    /**
     * jarファイルの中に格納されているファイルを、jarファイルの外にコピーするメソッド
     * @param jarFile jarファイル
     * @param targetFile コピー先
     * @param sourceFilePath コピー元
     * @param isBinary バイナリファイルかどうか
     */
    public static void copyFileFromJar(
            File jarFile, File targetFile, String sourceFilePath, boolean isBinary) {

        InputStream is = null;
        FileOutputStream fos = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        File parent = targetFile.getParentFile();
        if ( !parent.exists() ) {
            parent.mkdirs();
        }

        try {
            JarFile jar = new JarFile(jarFile);
            ZipEntry zipEntry = jar.getEntry(sourceFilePath);
            is = jar.getInputStream(zipEntry);

            fos = new FileOutputStream(targetFile);

            if ( isBinary ) {
                byte[] buf = new byte[8192];
                int len;
                while ( (len = is.read(buf)) != -1 ) {
                    fos.write(buf, 0, len);
                }
                fos.flush();
                fos.close();
                is.close();

            } else {
                reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                writer = new BufferedWriter(new OutputStreamWriter(fos));

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if ( writer != null ) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( reader != null ) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( fos != null ) {
                try {
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
            if ( is != null ) {
                try {
                    is.close();
                } catch (IOException e) {
                    // do nothing.
                }
            }
        }
    }

    /**
     * クリックするとコマンドを実行するメッセージを表示する
     * @param player 表示対象のプレイヤー
     * @param text メッセージ
     * @param command 埋め込むコマンド
     */
    public static void sendCommandLinkMessage(Player player, String text, String command) {
        String com = String.format(
                "tellraw %s "
                + "{\"text\":\"%s\",\"underlined\":\"true\",\"clickEvent\":{"
                    + "\"action\":\"run_command\",\"value\":\"%s\"}}",
                player.getName(), text, command);
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), com);
    }

    /**
     * 指定された名前のオフラインプレイヤーを取得する
     * @param name プレイヤー名
     * @return プレイヤー
     */
    @SuppressWarnings("deprecation")
    public static OfflinePlayer getOfflinePlayer(String name) {
        return Bukkit.getOfflinePlayer(name);
    }
}
