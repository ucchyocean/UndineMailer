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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * メールグループ
 * @author ucchy
 */
public class GroupDataFlatFile extends GroupData {

    private MailSender owner;
    private ArrayList<MailSender> members;
    private GroupPermissionMode sendMode;
    private GroupPermissionMode modifyMode;
    private GroupPermissionMode dissolutionMode;

    /**
     * コンストラクタ(データロード用)
     */
    private GroupDataFlatFile(UndineMailer parent) {
        super(parent, null);
        members = new ArrayList<MailSender>();
    }

    /**
     * コンストラクタ(継承クラス用)
     * 
     * @param name グループ名
     */
    protected GroupDataFlatFile(UndineMailer parent, String name) {
        this(parent);
        this.name = name;
        sendMode = UndineMailer.getInstance().getUndineConfig().getSendModeDefault();
        modifyMode = UndineMailer.getInstance().getUndineConfig().getModifyModeDefault();
        dissolutionMode = UndineMailer.getInstance().getUndineConfig().getDissolutionModeDefault();
    }

    /**
     * コンストラクタ(新規作成用)
     * 
     * @param name  グループ名
     * @param owner オーナー
     */
    public GroupDataFlatFile(UndineMailer parent, String name, MailSender owner) {
        this(parent, name);
        setOwner(owner);
        addMember(owner);
    }

    /**
     * グループにメンバーを追加する
     * 
     * @param member メンバー
     */
    @Override
    public void addMember(MailSender member) {
        if (!members.contains(member)) {
            members.add(member);
        }
    }

    /**
     * グループからメンバーを削除する
     * 
     * @param member メンバー
     */
    @Override
    public void removeMember(MailSender member) {
        if (members.contains(member)) {
            members.remove(member);
        }
    }

    /**
     * グループのオーナーを取得する
     * 
     * @return オーナー
     */
    @Override
    public MailSender getOwner() {
        return owner;
    }

    /**
     * グループのオーナーを設定する
     * 
     * @param owner
     */
    @Override
    public void setOwner(MailSender owner) {
        this.owner = owner;
    }

    /**
     * グループのメンバーを取得する
     * 
     * @return メンバー
     */
    @Override
    public ArrayList<MailSender> getMembers() {
        return members;
    }

    /**
     * 指定されたsenderが、グループのメンバーかどうかを返す
     * 
     * @param sender
     * @return メンバーかどうか
     */
    @Override
    public boolean isMember(MailSender sender) {
        return members.contains(sender);
    }

    /**
     * 指定されたsenderがオーナーかどうかを返す
     * 
     * @param sender
     * @return オーナーかどうか
     */
    @Override
    public boolean isOwner(MailSender sender) {
        return owner.equals(sender);
    }

    /**
     * 送信権限モードを取得する
     * 
     * @return sendMode
     */
    @Override
    public GroupPermissionMode getSendMode() {
        return sendMode;
    }

    /**
     * 送信権限モードを設定する
     * 
     * @param sendMode sendMode
     */
    @Override
    public void setSendMode(GroupPermissionMode sendMode) {
        this.sendMode = sendMode;
    }

    /**
     * 変更権限モードを取得する
     * 
     * @return modifyMode
     */
    @Override
    public GroupPermissionMode getModifyMode() {
        return modifyMode;
    }

    /**
     * 変更権限モードを設定する
     * 
     * @param modifyMode modifyMode
     */
    @Override
    public void setModifyMode(GroupPermissionMode modifyMode) {
        this.modifyMode = modifyMode;
    }

    /**
     * 解散権限モードを取得する
     * 
     * @return dissolutionMode
     */
    @Override
    public GroupPermissionMode getDissolutionMode() {
        return dissolutionMode;
    }

    /**
     * 解散権限モードを設定する
     * 
     * @param dissolutionMode dissolutionMode
     */
    @Override
    public void setDissolutionMode(GroupPermissionMode dissolutionMode) {
        this.dissolutionMode = dissolutionMode;
    }

    /**
     * コンフィグセクションにグループを保存する
     * 
     * @param section コンフィグセクション
     */
    private void saveToSection(ConfigurationSection section) {
        section.set("name", name);
        section.set("owner", owner.toString());

        List<String> array = new ArrayList<String>();
        for (MailSender mem : members) {
            array.add(mem.toString());
        }
        section.set("members", array);

        section.set("sendMode", sendMode.toString());
        section.set("modifyMode", modifyMode.toString());
        section.set("dissolutionMode", dissolutionMode.toString());
    }

    /**
     * ファイルにグループを保存する
     */
    public void save() {
        File folder = parent.getGroupFolder();
        File file = new File(folder, name.toLowerCase() + ".yml");
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
     * 
     * @param section コンフィグセクション
     * @return グループ
     */
    private static GroupDataFlatFile loadFromSection(UndineMailer parent, ConfigurationSection section) {
        GroupDataFlatFile data = new GroupDataFlatFile(parent);
        data.name = section.getString("name");
        data.owner = MailSender.getMailSenderFromString(section.getString("owner"));
        for (String mem : section.getStringList("members")) {
            data.members.add(MailSender.getMailSenderFromString(mem));
        }
        data.sendMode = GroupPermissionMode.getFromString(section.getString("sendMode"), GroupPermissionMode.MEMBER);
        data.modifyMode = GroupPermissionMode.getFromString(section.getString("modifyMode"), GroupPermissionMode.OWNER);
        data.dissolutionMode = GroupPermissionMode.getFromString(section.getString("dissolutionMode"),
                GroupPermissionMode.OWNER);
        return data;
    }

    /**
     * ファイルからグループをロードする
     * 
     * @param file ファイル
     * @return グループ
     */
    protected static GroupDataFlatFile loadFromFile(UndineMailer parent, File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return loadFromSection(parent, config);
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
