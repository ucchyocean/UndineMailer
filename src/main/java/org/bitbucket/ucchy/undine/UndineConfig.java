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

    /** 添付アイテム課金を、アイテム1個ごとにするかどうか */
    private boolean attachFeePerAmount;

    /** 着払い料金に対する着払い税(パーセンテージ) */
    private int codMoneyTax;

    /** 着払いアイテムに対する着払い税(アイテム1個に対する金額) */
    private double codItemTax;

    /** 自分自身に送信を可能とするか */
    private boolean enableSendSelf;

    /** 添付ボックス操作不可とするワールド */
    private List<String> disableWorldsToOpenAttachBox;

    /** 着払い料金を使用するかどうか */
    private boolean enableCODMoney;

    /** 着払いアイテムを使用するかどうか */
    private boolean enableCODItem;

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

    /** 特殊グループ PEX への送信権限 */
    private GroupPermissionMode specialGroupPexSendMode;

    /** メールの保存期間（日数） */
    private int mailStorageTermDays;

    /** メールスパム保護期間（秒） */
    private int mailSpamProtectionSeconds;

    /** プレイヤーがログインした時に、未読一覧を表示するまでの時間（秒） */
    private int loginNotificationDelaySeconds;

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
            if ( UndineMailer.getDefaultLocaleLanguage().equals("ja") ) {
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
        sendFee = conf.getDouble("sendFee", 10);
        attachFee = conf.getDouble("attachFee", 10);
        attachFeePerAmount = conf.getBoolean("attachFeePerAmount", false);
        codMoneyTax = conf.getInt("codMoneyTax", 0);
        codItemTax = conf.getDouble("codItemTax", 0);
        enableSendSelf = conf.getBoolean("enableSendSelf", false);
        disableWorldsToOpenAttachBox =
                conf.getStringList("disableWorldsToOpenAttachBox");
        enableCODMoney = conf.getBoolean("enableCODMoney", true);
        enableCODItem = conf.getBoolean("enableCODItem", true);
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
        specialGroupPexSendMode = GroupPermissionMode.getFromString(
                conf.getString("specialGroupPexSendMode"),
                GroupPermissionMode.OP);
        mailStorageTermDays = conf.getInt("mailStorageTermDays", 30);
        mailSpamProtectionSeconds = conf.getInt("mailSpamProtectionSeconds", 15);
        loginNotificationDelaySeconds = conf.getInt("loginNotificationDelaySeconds", 3);

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

        // mailStorageTermDays は、マイナスが指定されていたら0に変更する
        if ( mailStorageTermDays < 0 ) {
            mailStorageTermDays = 0;
        }

        // mailSpamProtectionSeconds は、マイナスが指定されていたら0に変更する
        if ( mailSpamProtectionSeconds < 0 ) {
            mailSpamProtectionSeconds = 0;
        }

        // loginNotificationDelaySeconds は、マイナスが指定されていたら0に変更する
        if ( loginNotificationDelaySeconds < 0 ) {
            loginNotificationDelaySeconds = 0;
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
     * @return attachFeePerAmount
     */
    public boolean isAttachFeePerAmount() {
        return attachFeePerAmount;
    }

    /**
     * @return codMoneyTax
     */
    public int getCodMoneyTax() {
        return codMoneyTax;
    }

    /**
     * @return codItemTax
     */
    public double getCodItemTax() {
        return codItemTax;
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
     * @return enableCODMoney
     */
    public boolean isEnableCODMoney() {
        return enableCODMoney;
    }

    /**
     * @return enableCODItem
     */
    public boolean isEnableCODItem() {
        return enableCODItem;
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

    /**
     * @return specialGroupPexSendMode
     */
    public GroupPermissionMode getSpecialGroupPexSendMode() {
        return specialGroupPexSendMode;
    }

    /**
     * @return mailStorageTermDays
     */
    public int getMailStorageTermDays() {
        return mailStorageTermDays;
    }

    /**
     * @return mailSpamProtectionSeconds
     */
    public int getMailSpamProtectionSeconds() {
        return mailSpamProtectionSeconds;
    }

    /**
     * @return loginNotificationDelaySeconds
     */
    public int getLoginNotificationDelaySeconds() {
        return loginNotificationDelaySeconds;
    }

}
