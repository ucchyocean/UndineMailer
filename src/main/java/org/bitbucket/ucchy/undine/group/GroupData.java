/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * メールグループ
 * @author ucchy
 */
public class GroupData {

    private String name;
    private MailSender owner;
    private ArrayList<MailSender> members;
    private GroupPermissionMode sendMode;
    private GroupPermissionMode modifyMode;
    private GroupPermissionMode dissolutionMode;

    /**
     * コンストラクタ(引数なしは外から利用不可)
     */
    private GroupData() {
        members = new ArrayList<MailSender>();
    }

    /**
     * コンストラクタ
     * @param name グループ名
     * @param owner オーナー
     */
    public GroupData(String name, MailSender owner) {
        this.name = name;
        this.owner = owner;
        members = new ArrayList<MailSender>();
        members.add(owner);
        sendMode = UndineMailer.getInstance().getUndineConfig().getSendModeDefault();
        modifyMode = UndineMailer.getInstance().getUndineConfig().getModifyModeDefault();
        dissolutionMode =
                UndineMailer.getInstance().getUndineConfig().getDissolutionModeDefault();
    }

    /**
     * グループにメンバーを追加する
     * @param member メンバー
     */
    public void addMember(MailSender member) {
        if ( !members.contains(member) ) {
            members.add(member);
        }
    }

    /**
     * グループからメンバーを削除する
     * @param member メンバー
     */
    public void removeMember(MailSender member) {
        if ( members.contains(member) ) {
            members.remove(member);
        }
    }

    /**
     * グループ名を取得する
     * @return グループ名
     */
    public String getName() {
        return name;
    }

    /**
     * グループのオーナーを取得する
     * @return オーナー
     */
    public MailSender getOwner() {
        return owner;
    }

    /**
     * グループのメンバーを取得する
     * @return メンバー
     */
    public ArrayList<MailSender> getMembers() {
        return members;
    }

    /**
     * 指定されたsenderがオーナーかどうかを返す
     * @param sender
     * @return オーナーかどうか
     */
    public boolean isOwner(MailSender sender) {
        return owner.equals(sender);
    }

    /**
     * 送信権限モードを取得する
     * @return sendMode
     */
    public GroupPermissionMode getSendMode() {
        return sendMode;
    }

    /**
     * 送信権限モードを設定する
     * @param sendMode sendMode
     */
    public void setSendMode(GroupPermissionMode sendMode) {
        this.sendMode = sendMode;
    }

    /**
     * 指定されたsenderは、送信権限を持っているかどうかを調べる
     * @param sender
     * @return 送信権限を持っているかどうか
     */
    public boolean canSend(MailSender sender) {
        if ( sender.hasPermission("undine.group.send-all") ) {
            return true;
        }
        switch ( sendMode ) {
        case EVERYONE:
            return true;
        case MEMBER:
            return members.contains(sender);
        case OWNER:
            return owner.equals(sender);
        case OP:
            return sender.isOp();
        default:
            return false;
        }
    }

    /**
     * 変更権限モードを取得する
     * @return modifyMode
     */
    public GroupPermissionMode getModifyMode() {
        return modifyMode;
    }

    /**
     * 変更権限モードを設定する
     * @param modifyMode modifyMode
     */
    public void setModifyMode(GroupPermissionMode modifyMode) {
        this.modifyMode = modifyMode;
    }

    /**
     * 指定されたsenderは、変更権限を持っているかどうかを調べる
     * @param sender
     * @return 変更権限を持っているかどうか
     */
    public boolean canModify(MailSender sender) {
        if ( sender.hasPermission("undine.group.modify-all") ) {
            return true;
        }
        switch ( modifyMode ) {
        case EVERYONE:
            return true;
        case MEMBER:
            return members.contains(sender);
        case OWNER:
            return owner.equals(sender);
        case OP:
            return sender.isOp();
        default:
            return false;
        }
    }

    /**
     * 解散権限モードを取得する
     * @return dissolutionMode
     */
    public GroupPermissionMode getDissolutionMode() {
        return dissolutionMode;
    }

    /**
     * 解散権限モードを設定する
     * @param dissolutionMode dissolutionMode
     */
    public void setDissolutionMode(GroupPermissionMode dissolutionMode) {
        this.dissolutionMode = dissolutionMode;
    }

    /**
     * 指定されたsenderは、解散権限を持っているかどうかを調べる
     * @param sender
     * @return 解散権限を持っているかどうか
     */
    public boolean canBreakup(MailSender sender) {
        if ( sender.hasPermission("undine.group.dissolution-all") ) {
            return true;
        }
        switch ( dissolutionMode ) {
        case EVERYONE:
            return true;
        case MEMBER:
            return members.contains(sender);
        case OWNER:
            return owner.equals(sender);
        case OP:
            return sender.isOp();
        default:
            return false;
        }
    }

    /**
     * コンフィグセクションにグループを保存する
     * @param section コンフィグセクション
     */
    private void saveToSection(ConfigurationSection section) {
        section.set("name", name);
        section.set("owner", owner.toString());

        List<String> array = new ArrayList<String>();
        for ( MailSender mem : members ) {
            array.add(mem.toString());
        }
        section.set("members", array);

        section.set("sendMode", sendMode.toString());
        section.set("modifyMode", modifyMode.toString());
        section.set("dissolutionMode", dissolutionMode.toString());
    }

    /**
     * ファイルにグループを保存する
     * @param file ファイル
     */
    protected void saveToFile(File file) {
        YamlConfiguration config = new YamlConfiguration();
        saveToSection(config);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * コンフィグセクションからグループをロードする
     * @param section コンフィグセクション
     * @return グループ
     */
    private static GroupData loadFromSection(ConfigurationSection section) {
        GroupData data = new GroupData();
        data.name = section.getString("name");
        data.owner = MailSender.getMailSenderFromString(section.getString("owner"));
        for ( String mem : section.getStringList("members") ) {
            data.members.add(MailSender.getMailSenderFromString(mem));
        }
        data.sendMode = GroupPermissionMode.getFromString(
                section.getString("sendMode"), GroupPermissionMode.MEMBER);
        data.modifyMode = GroupPermissionMode.getFromString(
                section.getString("modifyMode"), GroupPermissionMode.OWNER);
        data.dissolutionMode = GroupPermissionMode.getFromString(
                section.getString("dissolutionMode"), GroupPermissionMode.OWNER);
        return data;
    }

    /**
     * ファイルからグループをロードする
     * @param file ファイル
     * @return グループ
     */
    protected static GroupData loadFromFile(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return loadFromSection(config);
    }
}
