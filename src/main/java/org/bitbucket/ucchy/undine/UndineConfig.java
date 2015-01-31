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

    /** 着払い料金の入金後の残金が一致しない場合に、エラーとするかどうか */
    private boolean depositErrorOnUnmatch;

    /** 添付ボックスのサイズ */
    private int attachBoxSize;

    /** 同時に使用可能な添付ボックスの個数を指定します。 */
    private int maxAttachmentBoxCount;

    /** UIの前に挿入する空行の行数 */
    private int uiEmptyLines;

    /** プレイヤーリストを利用可能にするかどうか */
    private boolean enablePlayerList;

    /** 1プレイヤーが作成可能なグループの最大数 */
    private int maxCreateGroup;

    /** 1グループに追加できる最大プレイヤー数 */
    private int maxGroupMember;

    /** グループへのメール送信権限のデフォルト */
    private GroupPermissionMode sendModeDefault;

    /** グループのメンバー変更権限のデフォルト */
    private GroupPermissionMode modifyModeDefault;

    /** グループの解散権限のデフォルト */
    private GroupPermissionMode dissolutionModeDefault;

    /** 指定できる宛先の最大数 */
    private int maxDestination;

    /** 指定できる宛先グループの最大数 */
    private int maxDestinationGroup;

    /** 特殊グループ All への送信権限 */
    private GroupPermissionMode specialGroupAllSendMode;

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
        depositErrorOnUnmatch = conf.getBoolean("depositErrorOnUnmatch", true);
        attachBoxSize = conf.getInt("attachBoxSize", 1);
        maxAttachmentBoxCount = conf.getInt("maxAttachmentBoxCount", 3);
        uiEmptyLines = conf.getInt("uiEmptyLines", 2);
        enablePlayerList = conf.getBoolean("enablePlayerList", false);
        maxCreateGroup = conf.getInt("maxCreateGroup", 5);
        maxGroupMember = conf.getInt("maxGroupMember", 15);
        sendModeDefault = GroupPermissionMode.getFromString(
                conf.getString("sendModeDefault"),
                GroupPermissionMode.MEMBER);
        modifyModeDefault = GroupPermissionMode.getFromString(
                conf.getString("modifyModeDefault"),
                GroupPermissionMode.OWNER);
        dissolutionModeDefault = GroupPermissionMode.getFromString(
                conf.getString("dissolutionModeDefault"),
                GroupPermissionMode.OWNER);
        maxDestination = conf.getInt("maxDestination", 10);
        maxDestinationGroup = conf.getInt("maxDestinationGroup", 3);
        specialGroupAllSendMode = GroupPermissionMode.getFromString(
                conf.getString("specialGroupAllSendMode"),
                GroupPermissionMode.OP);

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

        // 添付ボックスの個数制限は、1以上の数値に制限する
        if ( maxAttachmentBoxCount < 1 ) {
            maxAttachmentBoxCount = 1;
        }

        // 挿入する空行の行数は、0から9までの数値に制限する
        if ( uiEmptyLines < 0 ) {
            uiEmptyLines = 0;
        } else if ( uiEmptyLines > 9 ) {
            uiEmptyLines = 9;
        }

        // maxDestinationは、1以上の数値に制限する
        if ( maxDestination < 1 ) {
            maxDestination = 1;
        }

        // maxDestinationGroupは、マイナスが指定されていたら0に変更する
        if ( maxDestinationGroup < 0 ) {
            maxDestinationGroup = 0;
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
     * @return depositErrorOnUnmatch
     */
    public boolean getDepositErrorOnUnmatch() {
        return depositErrorOnUnmatch;
    }

    /**
     * @return attachBoxSize
     */
    public int getAttachBoxSize() {
        return attachBoxSize;
    }

    /**
     * @return maxAttachmentBoxCount
     */
    public int getMaxAttachmentBoxCount() {
        return maxAttachmentBoxCount;
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
     * @return sendModeDefault
     */
    public GroupPermissionMode getSendModeDefault() {
        return sendModeDefault;
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

    /**
     * @return maxDestination
     */
    public int getMaxDestination() {
        return maxDestination;
    }

    /**
     * @return maxDestinationGroup
     */
    public int getMaxDestinationGroup() {
        return maxDestinationGroup;
    }

    /**
     * @return specialGroupAllSendMode
     */
    public GroupPermissionMode getSpecialGroupAllSendMode() {
        return specialGroupAllSendMode;
    }


}
