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
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.ChatColor;
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
     * 指定されたsenderが、グループのメンバーかどうかを返す
     * @param sender
     * @return メンバーかどうか
     */
    public boolean isMember(MailSender sender) {
        return members.contains(sender);
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
        return permissionCheck(sender,
                sendMode, "undine.group.send-all");
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
        return permissionCheck(sender,
                modifyMode, "undine.group.modify-all");
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
        return permissionCheck(sender,
                dissolutionMode, "undine.group.dissolution-all");
    }

    /**
     * ホバー用のテキストを作成して返す
     * @return ホバー用のテキスト
     */
    public String getHoverText() {
        StringBuffer hover = new StringBuffer();
        hover.append(ChatColor.GOLD + owner.getName() + ChatColor.WHITE);
        ArrayList<MailSender> members = new ArrayList<MailSender>(this.members);
        members.remove(owner);
        for ( int j=0; j<5; j++ ) {
            if ( members.size() <= j ) {
                break;
            }
            hover.append("\n" + members.get(j).getName());
        }
        if ( members.size() - 5 > 0 ) {
            hover.append("\n ... and " + (members.size() - 5) + " more ...");
        }
        return hover.toString();
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

    /**
     * パーミッションのチェックを行う
     * @param sender
     * @param mode
     * @param specialNode
     * @return パーミッションの有無
     */
    private boolean permissionCheck(
            MailSender sender, GroupPermissionMode mode, String specialNode) {
        if ( mode == GroupPermissionMode.NEVER ) return false;
        if ( sender.hasPermission(specialNode) ) {
            return true;
        }
        switch ( mode ) {
        case EVERYONE:
            return true;
        case MEMBER:
            return isMember(sender);
        case OWNER:
            return owner.equals(sender);
        case OP:
            return sender.isOp();
        default:
            return false;
        }
    }

    /**
     * データのアップグレードを行う。
     * @return アップグレードを実行したかどうか
     */
    protected boolean upgrade() {
        boolean upgraded = false;
        if ( owner instanceof MailSenderPlayer ) {
            if ( ((MailSenderPlayer) owner).upgrade() ) {
                upgraded = true;
            }
        }
        for ( MailSender ms : members ) {
            if ( ms instanceof MailSenderPlayer ) {
                if ( ((MailSenderPlayer) ms).upgrade() ) {
                    upgraded = true;
                }
            }
        }
        return upgraded;
    }
}
