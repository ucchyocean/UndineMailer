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
        this.owner = owner;
        members = new ArrayList<MailSender>();
        members.add(owner);
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
