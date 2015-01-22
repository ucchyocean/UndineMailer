/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.util.List;

import org.bitbucket.ucchy.undine.group.GroupPermissionMode;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Undineコンフィグ管理クラス
 * @author ucchy
 */
public class UndineConfig {

    /** メッセージ言語 */
    private String lang;

    /** メールにアイテムの添付を可能にするかどうか */
    private boolean enableAttachment;

    /** 送信に料金を必要とするか */
    private boolean enableSendFee;

    /** 送信に必要な料金、送信先が複数ある場合は、送信先ごとに課金される */
    private double sendFee;

    /** 添付アイテムを1件付けるのに必要な料金 */
    private double attachFee;

    /** 自分自身に送信を可能とするか */
    private boolean enableSendSelf;

    /** 添付ボックス操作不可とするワールド */
    private List<String> disableWorldsToOpenAttachBox;

    /** 添付ボックスのサイズ */
    private int attachBoxSize;

    /** UIの前に挿入する空行の行数 */
    private int uiEmptyLines;

    /** プレイヤーリストを利用可能にするかどうか */
    private boolean enablePlayerList;

    /** 1プレイヤーが作成可能なグループの最大数 */
    private int maxCreateGroup;

    /** 1グループに追加できる最大プレイヤー数 */
    private int maxGroupMember;

    /** グループのメンバー変更は、オーナーのみ可能とするかどうか */
    private GroupPermissionMode modifyModeDefault;

    /** グループの解散は、オーナーのみ可能とするかどうか */
    private GroupPermissionMode dissolutionModeDefault;

    private UndineMailer parent;

    /**
     * コンストラクタ
     * @param parent プラグイン
     */
    public UndineConfig(UndineMailer parent) {
        this.parent = parent;
        reloadConfig();
    }

    /**
     * コンフィグを読み込む
     */
    protected void reloadConfig() {

        if ( !parent.getDataFolder().exists() ) {
            parent.getDataFolder().mkdirs();
        }

        File file = new File(parent.getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            if ( parent.getReleaseLang().equals("ja") ) {
                Utility.copyFileFromJar(
                        parent.getJarFile(), file, "config_ja.yml", false);
            } else {
                Utility.copyFileFromJar(
                        parent.getJarFile(), file, "config.yml", false);
            }
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        // 読み込み
        lang = conf.getString("lang", "ja");
        enableAttachment = conf.getBoolean("enableAttachment", true);
        enableSendFee = conf.getBoolean("enableSendFee", false);
        sendFee = conf.getInt("sendFee", 10);
        attachFee = conf.getInt("attachFee", 10);
        enableSendSelf = conf.getBoolean("enableSendSelf", false);
        disableWorldsToOpenAttachBox =
                conf.getStringList("disableWorldsToOpenAttachBox");
        attachBoxSize = conf.getInt("attachBoxSize", 1);
        uiEmptyLines = conf.getInt("uiEmptyLines", 3);
        enablePlayerList = conf.getBoolean("enablePlayerList", true);
        maxCreateGroup = conf.getInt("maxCreateGroup", 5);
        maxGroupMember = conf.getInt("maxGroupMember", 15);
        modifyModeDefault = GroupPermissionMode.getFromString(
                conf.getString("modifyModeDefault"),
                GroupPermissionMode.OWNER);
        dissolutionModeDefault = GroupPermissionMode.getFromString(
                conf.getString("dissolutionModeDefault"),
                GroupPermissionMode.OWNER);

        // sendFeeは、マイナスが指定されていたら0に変更する
        if ( sendFee < 0 ) {
            sendFee = 0;
        }

        // 添付ボックスは、1から6までの数値に制限する
        if ( attachBoxSize < 1 ) {
            attachBoxSize = 1;
        } else if ( attachBoxSize > 6 ) {
            attachBoxSize = 6;
        }

        // 挿入する空行の行数は、0から9までの数値に制限する
        if ( uiEmptyLines < 0 ) {
            uiEmptyLines = 0;
        } else if ( uiEmptyLines > 9 ) {
            uiEmptyLines = 9;
        }
    }

    /**
     * @return lang
     */
    public String getLang() {
        return lang;
    }

    /**
     * @return enableAttachment
     */
    public boolean isEnableAttachment() {
        return enableAttachment;
    }

    /**
     * @return enableSendFee
     */
    public boolean isEnableSendFee() {
        return enableSendFee;
    }

    /**
     * @return sendFee
     */
    public double getSendFee() {
        return sendFee;
    }

    /**
     * @return attachFee
     */
    public double getAttachFee() {
        return attachFee;
    }

    /**
     * @return enableSendSelf
     */
    public boolean isEnableSendSelf() {
        return enableSendSelf;
    }

    /**
     * @return disableWorldsToOpenAttachment
     */
    public List<String> getDisableWorldsToOpenAttachBox() {
        return disableWorldsToOpenAttachBox;
    }

    /**
     * @return attachBoxSize
     */
    public int getAttachBoxSize() {
        return attachBoxSize;
    }

    /**
     * @return uiEmptyLines
     */
    public int getUiEmptyLines() {
        return uiEmptyLines;
    }

    /**
     * @return enablePlayerList
     */
    public boolean isEnablePlayerList() {
        return enablePlayerList;
    }

    /**
     * @return maxCreateGroup
     */
    public int getMaxCreateGroup() {
        return maxCreateGroup;
    }

    /**
     * @return maxGroupMember
     */
    public int getMaxGroupMember() {
        return maxGroupMember;
    }

    /**
     * @return modifyModeDefault
     */
    public GroupPermissionMode getModifyModeDefault() {
        return modifyModeDefault;
    }

    /**
     * @return dissolutionMode
     */
    public GroupPermissionMode getDissolutionModeDefault() {
        return dissolutionModeDefault;
    }


}
