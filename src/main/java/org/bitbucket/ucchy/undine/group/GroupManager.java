/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.command.GroupCommand;
import org.bitbucket.ucchy.undine.command.ListCommand;
import org.bitbucket.ucchy.undine.command.UndineCommand;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.tellraw.ClickEventType;
import org.bitbucket.ucchy.undine.tellraw.MessageComponent;
import org.bitbucket.ucchy.undine.tellraw.MessageParts;
import org.bukkit.ChatColor;

/**
 * メールグループ管理クラス
 * @author ucchy
 */
public class GroupManager {

    private static final int PAGE_SIZE = 10;

    private static final String COMMAND = GroupCommand.COMMAND;
    private static final String PERMISSION = GroupCommand.PERMISSION;

    private UndineMailer parent;
    private HashMap<String, GroupData> groups;

    /**
     * コンストラクタ
     * @param parent
     */
    public GroupManager(UndineMailer parent) {
        this.parent = parent;
        reload();
    }

    /**
     * 全データを再読み込みする
     */
    public void reload() {

        UndineMailer.getInstance().getLogger().info("Loading group data... Start.");
        long start = System.currentTimeMillis();

        File folder = parent.getGroupFolder();
        File[] files = folder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        groups = new HashMap<String, GroupData>();
        for ( File f : files ) {
            GroupData group = GroupData.loadFromFile(f);
            groups.put(group.getName().toLowerCase(), group);
        }

        // 特殊グループを追加する
        GroupData all = new SpecialGroupAll();
        groups.put(all.getName().toLowerCase(), all);

        UndineMailer.getInstance().getLogger().info("Loading group data... Done.  Time: "
                + (System.currentTimeMillis() - start) + "ms, Data: " + groups.size() + ".");
    }

    /**
     * グループを追加する。
     * 重複するグループ名が既に追加されている場合は、
     * 古いグループが上書きされてしまうことに注意する。
     * @param group グループ
     */
    public void addGroup(GroupData group) {
        String name = group.getName().toLowerCase();
        groups.put(name, group);
        saveGroupData(group);
    }

    /**
     * 指定したグループ名のグループを取得する
     * @param name グループ名
     * @return グループ
     */
    public GroupData getGroup(String name) {
        name = name.toLowerCase();
        if ( groups.containsKey(name) ) {
            return groups.get(name);
        }
        return null;
    }

    /**
     * 指定したグループ名のグループを削除する
     * @param name グループ名
     */
    public void removeGroup(String name) {
        name = name.toLowerCase();
        if ( groups.containsKey(name) ) {
            groups.remove(name);
            File folder = parent.getGroupFolder();
            File file = new File(folder, name + ".yml");
            file.delete();
        }
    }

    /**
     * 全てのグループ名を取得する
     * @return 全てのグループ名
     */
    public ArrayList<String> getAllGroupNames() {
        ArrayList<String> names = new ArrayList<String>();
        for ( GroupData group : groups.values() ) {
            names.add(group.getName());
        }
        return names;
    }

    /**
     * 全てのグループを取得する
     * @return 全てのグループ
     */
    public ArrayList<GroupData> getAllGroups() {
        return new ArrayList<GroupData>(groups.values());
    }

    /**
     * 指定されたグループ名は既に存在するかどうかを確認する
     * @return 存在するかどうか
     */
    public boolean existGroupName(String name) {
        return groups.keySet().contains(name.toLowerCase());
    }

    /**
     * 指定したグループを実データファイルに保存する
     * @param group グループ
     */
    public void saveGroupData(GroupData group) {
        File folder = parent.getGroupFolder();
        File file = new File(folder, group.getName().toLowerCase() + ".yml");
        group.saveToFile(file);
    }

    /**
     * グループ名として使用できる名前かどうかを確認する
     * @param name グループ名
     * @return 使用可能かどうか
     */
    public static boolean canUseNameFromGroup(String name) {
        return name.matches("[^\\\\/\\?\\*:\\|\\\"<>\\.]{1,15}");
    }

    /**
     * 指定したsenderがオーナーのグループの個数を返す
     * @param sender
     * @return オーナーのグループの個数
     */
    public int getOwnerGroupCount(MailSender sender) {
        int total = 0;
        for ( GroupData group : groups.values() ) {
            if ( group.getOwner().equals(sender) ) total++;
        }
        return total;
    }

    /**
     * 指定したsenderは新規にグループを作成できるかどうかを返す
     * @param sender
     * @return 新規にグループを作成できるかどうか
     */
    public boolean canMakeNewGroup(MailSender sender) {
        if ( sender.hasPermission(PERMISSION + ".infinite-create") ) return true;
        if ( !sender.hasPermission(PERMISSION + ".create") ) return false;
        int num = getOwnerGroupCount(sender);
        int limit = parent.getUndineConfig().getMaxCreateGroup();
        return ( num < limit );
    }

    /**
     * グループ一覧画面で表示する項目として、指定されたsenderが表示可能なグループを返す
     * @param sender 取得対象のsender
     * @return senderが宛先として送信可能なグループと、senderがメンバーのグループ、の和。
     */
    public ArrayList<GroupData> getGroupsForList(MailSender sender) {

        ArrayList<GroupData> results = new ArrayList<GroupData>();

        for ( GroupData group : groups.values() ) {
            if ( group.isMember(sender) || group.canSend(sender) ) {
                results.add(group);
            }
        }

        Collections.sort(results, new Comparator<GroupData>() {
            public int compare(GroupData o1, GroupData o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        return results;
    }

    /**
     * メール宛先で表示する項目として、指定されたsenderが送信可能なグループを返す
     * @param sender 取得対象のsender
     * @return senderが宛先として送信可能なグループ
     */
    public ArrayList<GroupData> getGroupsForSelection(MailSender sender) {

        ArrayList<GroupData> results = new ArrayList<GroupData>();

        for ( GroupData group : groups.values() ) {
            if ( group.canSend(sender) ) {
                results.add(group);
            }
        }

        Collections.sort(results, new Comparator<GroupData>() {
            public int compare(GroupData o1, GroupData o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        return results;
    }

    /**
     * 指定されたsenderに、Groupリストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ(1から始まることに注意)
     */
    public void displayGroupList(MailSender sender, int page) {

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String pre = Messages.get("ListVerticalParts");
        String parts = Messages.get("ListHorizontalParts");

        ArrayList<GroupData> list = getGroupsForList(sender);
        int max = (int)((list.size() - 1) / PAGE_SIZE) + 1;

        String title = Messages.get("GroupListTitle", "%num", list.size());
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        for ( int i=0; i<10; i++ ) {

            int index = (page - 1) * 10 + i;
            if ( index < 0 || list.size() <= index ) {
                continue;
            }

            GroupData group = list.get(index);

            MessageComponent msg = new MessageComponent();
            msg.addText(pre);

            MessageParts button =
                    new MessageParts("[" + group.getName() + "]", ChatColor.AQUA);
            button.setClickEvent(ClickEventType.RUN_COMMAND,
                    COMMAND + " detail " + group.getName());
            button.addHoverText(group.getHoverText());
            msg.addParts(button);

            if ( group instanceof SpecialGroupAll ) {
                msg.addText(" " + Messages.get("GroupSpecialAllSummary"));
            } else {
                msg.addText(" " + Messages.get("GroupListSummayLine",
                        new String[]{"%owner", "%num"},
                        new String[]{group.getOwner().getName(), group.getMembers().size() + ""}));
            }

            msg.send(sender);
        }

        if ( canMakeNewGroup(sender) ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);

            MessageParts button =
                    new MessageParts(Messages.get("GroupMakeNewGroup"), ChatColor.AQUA);
            button.setClickEvent(ClickEventType.SUGGEST_COMMAND,
                    COMMAND + " create ");
            button.addHoverText(Messages.get("GroupMakeNewGroupToolTip"));
            msg.addParts(button);

            msg.send(sender);
        }

        sendPager(sender, COMMAND + " list", "", page, max,
                Messages.get("ListHorizontalParts"), null);
    }

    /**
     * 指定されたsenderに、Group選択リストを表示する。
     * @param sender 表示対象のsender
     * @param page 表示するページ(1から始まることに注意)
     * @param next 選択したときに実行するコマンド
     */
    public void displayGroupSelection(MailSender sender, int page, String next) {

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String pre = Messages.get("ListVerticalParts");
        String parts = Messages.get("ListHorizontalParts");

        ArrayList<GroupData> list = getGroupsForSelection(sender);
        int max = (int)((list.size() - 1) / PAGE_SIZE) + 1;

        String title = Messages.get("GroupListTitle", "%num", list.size());
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        for ( int i=0; i<10; i++ ) {

            int index = (page - 1) * 10 + i;
            if ( index < 0 || list.size() <= index ) {
                continue;
            }

            GroupData group = list.get(index);

            MessageComponent msg = new MessageComponent();
            msg.addText(pre);

            MessageParts button =
                    new MessageParts("[" + group.getName() + "]", ChatColor.AQUA);
            button.setClickEvent(ClickEventType.RUN_COMMAND,
                    next + " " + group.getName());
            button.addHoverText(group.getHoverText());
            msg.addParts(button);

            if ( group instanceof SpecialGroupAll ) {
                msg.addText(" " + Messages.get("GroupSpecialAllSummary"));
            } else {
                msg.addText(" " + Messages.get("GroupListSummayLine",
                        new String[]{"%owner", "%num"},
                        new String[]{group.getOwner().getName(), group.getMembers().size() + ""}));
            }

            msg.send(sender);
        }

        if ( canMakeNewGroup(sender) ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);

            MessageParts button =
                    new MessageParts(Messages.get("GroupMakeNewGroup"), ChatColor.WHITE);
            button.addHoverText(Messages.get("GroupMakeNewGroupToolTipForSelection"));
            msg.addParts(button);

            msg.send(sender);
        }

        sendPager(sender, COMMAND + " list", " " + next, page, max,
                Messages.get("ListHorizontalParts"),
                UndineCommand.COMMAND + " write");
    }

    /**
     * 指定されたsenderに、groupの詳細(ReadOnly)を表示する
     * @param sender 表示対象のsender
     * @param group 表示するグループ
     */
    public void displayGroupDetailReadOnly(MailSender sender, GroupData group) {

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String pre = Messages.get("DetailVerticalParts");
        String parts = Messages.get("DetailHorizontalParts");

        String title = Messages.get("GroupDetailTitle", "%name", group.getName());
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        sender.sendMessage(pre + Messages.get(
                "GroupOwnerLine", "%owner", group.getOwner().getName()));
        sender.sendMessage(pre + Messages.get("GroupMemberLine"));

        if ( group instanceof SpecialGroupAll ) {
            sender.sendMessage(pre + "  "
                    + ChatColor.WHITE + Messages.get("GroupSpecialAllMembers"));

        } else {

            // メンバーを5人ごとに区切って表示する
            ArrayList<MailSender> members = group.getMembers();
            Collections.sort(members, new Comparator<MailSender>() {
                public int compare(MailSender o1, MailSender o2) {
                    return o1.getName().compareToIgnoreCase(o2.getName());
                }
            });
            int size = members.size();
            int max = (int)((size - 1) / 5) + 1;
            for ( int i=0; i<max; i++ ) {
                StringBuffer buffer = new StringBuffer();
                buffer.append(pre + "  " + ChatColor.WHITE);
                for ( int j=0; j<5; j++ ) {
                    int index = i * 5 + j;
                    if ( index < size ) {
                        buffer.append(members.get(index).getName() + ", ");
                    }
                }
                sender.sendMessage(buffer.toString());
            }
        }

        sender.sendMessage(Messages.get("DetailLastLine"));
    }

    /**
     * 指定されたsenderに、groupの詳細(ModifyMode)を表示する
     * @param sender 表示対象のsender
     * @param group 表示するグループ
     * @param page 表示するページ(1から始まることに注意)
     */
    public void displayGroupDetailModifyMode(MailSender sender, GroupData group, int page) {

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String pre = Messages.get("DetailVerticalParts");
        String parts = Messages.get("DetailHorizontalParts");

        String title = Messages.get("GroupDetailTitle", "%name", group.getName());
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        sender.sendMessage(pre + Messages.get(
                "GroupOwnerLine", "%owner", group.getOwner().getName()));
        sender.sendMessage(pre + Messages.get("GroupMemberLine"));

        ArrayList<MailSender> members = group.getMembers();
        Collections.sort(members, new Comparator<MailSender>() {
            public int compare(MailSender o1, MailSender o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        int size = members.size();
        int max = (int)((size - 1) / PAGE_SIZE) + 1;

        // メンバーの一覧を表示する
        for ( int i=0; i<PAGE_SIZE; i++ ) {

            int index = (page - 1) * PAGE_SIZE + i;
            if( index < 0 || index >= members.size() ) continue;

            MailSender member = members.get(index);

            MessageComponent msg = new MessageComponent();
            msg.addText(pre + "  ");

            if ( !group.getOwner().equals(member) ) {
                MessageParts delete = new MessageParts(
                        Messages.get("GroupDeleteMemberButton"), ChatColor.AQUA);
                delete.setClickEvent(
                        ClickEventType.RUN_COMMAND,
                        COMMAND + " remove " + group.getName() + " " + member.getName());
                delete.addHoverText(Messages.get("GroupDeleteMemberToolTip"));
                msg.addParts(delete);

            } else {
                MessageParts delete = new MessageParts(
                        Messages.get("GroupDeleteMemberButton"), ChatColor.WHITE);
                delete.addHoverText(Messages.get("GroupDeleteMemberOwnerToolTip"));
                msg.addParts(delete);

            }

            msg.addText(member.getName());

            msg.send(sender);
        }

        // メンバー追加ボタンと、設定ボタンを置く
        if ( group.getMembers().size() < parent.getUndineConfig().getMaxGroupMember() ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);

            if ( !parent.getUndineConfig().isEnablePlayerList() ) {
                MessageParts add = new MessageParts(Messages.get("GroupAddMember"), ChatColor.AQUA);
                add.setClickEvent(
                        ClickEventType.SUGGEST_COMMAND,
                        COMMAND + " add " + group.getName() + " ");
                msg.addParts(add);

            } else {
                MessageParts addAddress = new MessageParts(
                        Messages.get("GroupAddMemberAddress"), ChatColor.AQUA);
                addAddress.setClickEvent(
                        ClickEventType.RUN_COMMAND,
                        ListCommand.COMMAND_INDEX + " "
                        + COMMAND + " add " + group.getName() + " ");
                msg.addParts(addAddress);
            }

            msg.send(sender);
        }

        MessageComponent msg = new MessageComponent();
        msg.addText(pre);
        MessageParts setting = new MessageParts(
                Messages.get("GroupChangeSetting"), ChatColor.AQUA);
        setting.setClickEvent(
                ClickEventType.RUN_COMMAND,
                COMMAND + " detail " + group.getName() + " setting");
        msg.addParts(setting);

        msg.send(sender);

        sendPager(sender, COMMAND + " detail " + group.getName(), "",
                page, max, Messages.get("DetailHorizontalParts"),
                COMMAND + " list");
    }

    /**
     * 指定されたsenderに、グループの設定変更画面を表示する
     * @param sender
     * @param group
     */
    public void displayGroupSetting(MailSender sender, GroupData group) {

        // 空行を挿入する
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            sender.sendMessage("");
        }

        String pre = Messages.get("DetailVerticalParts");
        String parts = Messages.get("DetailHorizontalParts");

        String title = Messages.get("GroupSettingTitle", "%name", group.getName());
        sender.sendMessage(parts + parts + " " + title + " " + parts + parts);

        GroupPermissionMode mode = group.getSendMode();
        sender.sendMessage(pre + Messages.get("GroupSendPerm")
                + ChatColor.WHITE + mode.getDisplayString() );

        MessageComponent msgSend = new MessageComponent();
        msgSend.addText(pre + " ");
        addChangeButtons(msgSend,
                COMMAND + " perm " + group.getName() + " send",
                (mode != GroupPermissionMode.OWNER),
                (mode != GroupPermissionMode.MEMBER),
                (mode != GroupPermissionMode.EVERYONE), true);
        msgSend.send(sender);

        mode = group.getModifyMode();
        sender.sendMessage(pre + Messages.get("GroupModifyPerm")
                + ChatColor.WHITE + mode.getDisplayString() );

        MessageComponent msgMod = new MessageComponent();
        msgMod.addText(pre + " ");
        addChangeButtons(msgMod,
                COMMAND + " perm " + group.getName() + " modify",
                (mode != GroupPermissionMode.OWNER),
                (mode != GroupPermissionMode.MEMBER),
                false, false);
        msgMod.send(sender);

        mode = group.getDissolutionMode();
        sender.sendMessage(pre + Messages.get("GroupDissolutionPerm")
                + ChatColor.WHITE + mode.getDisplayString() );

        MessageComponent msgDis = new MessageComponent();
        msgDis.addText(pre + " ");
        addChangeButtons(msgDis,
                COMMAND + " perm " + group.getName() + " dissolution",
                (mode != GroupPermissionMode.OWNER),
                (mode != GroupPermissionMode.MEMBER),
                false, false);
        msgDis.send(sender);

        if ( group.canBreakup(sender) ) {
            MessageComponent msg = new MessageComponent();
            msg.addText(pre);
            MessageParts breakup = new MessageParts(
                    Messages.get("GroupDeleteGroup"), ChatColor.AQUA);
            breakup.setClickEvent(
                    ClickEventType.RUN_COMMAND,
                    COMMAND + " delete " + group.getName());
            msg.addParts(breakup);
            msg.send(sender);
        }

        MessageComponent msg = new MessageComponent();
        msg.addText(pre);
        MessageParts breakup = new MessageParts(
                Messages.get("GroupReturnToMemberList"), ChatColor.AQUA);
        breakup.setClickEvent(
                ClickEventType.RUN_COMMAND,
                COMMAND + " detail " + group.getName());
        msg.addParts(breakup);
        msg.send(sender);

        sender.sendMessage(Messages.get("DetailLastLine"));
    }

    /**
     * 権限設定のメッセージコンポーネントにボタンを加える
     * @param msg
     * @param base
     * @param owner
     * @param member
     * @param everyone
     * @param evisible
     */
    private void addChangeButtons(MessageComponent msg, String base,
            boolean owner, boolean member, boolean everyone, boolean evisible) {

        msg.addText(" ");
        MessageParts buttonOwner = new MessageParts(
                Messages.get("GroupPermChangeButton", "%perm",
                        GroupPermissionMode.OWNER.getDisplayString()));
        if ( owner ) {
            buttonOwner.setColor(ChatColor.AQUA);
            buttonOwner.setClickEvent(ClickEventType.RUN_COMMAND,
                    base + " " + GroupPermissionMode.OWNER);
        }
        msg.addParts(buttonOwner);

        msg.addText(" ");
        MessageParts buttonMember = new MessageParts(
                Messages.get("GroupPermChangeButton", "%perm",
                        GroupPermissionMode.MEMBER.getDisplayString()));
        if ( member ) {
            buttonMember.setColor(ChatColor.AQUA);
            buttonMember.setClickEvent(ClickEventType.RUN_COMMAND,
                    base + " " + GroupPermissionMode.MEMBER);
        }
        msg.addParts(buttonMember);

        if ( evisible ) {
            msg.addText(" ");
            MessageParts buttonEveryone = new MessageParts(
                    Messages.get("GroupPermChangeButton", "%perm",
                            GroupPermissionMode.EVERYONE.getDisplayString()));
            if ( everyone ) {
                buttonEveryone.setColor(ChatColor.AQUA);
                buttonEveryone.setClickEvent(ClickEventType.RUN_COMMAND,
                        base + " " + GroupPermissionMode.EVERYONE);
            }
            msg.addParts(buttonEveryone);
        }
    }

    /**
     * ページャーを対象プレイヤーに表示する
     * @param sender 表示対象
     * @param commandPre コマンドのプレフィックス
     * @param commandSuf コマンドのサフィックス
     * @param page 現在のページ
     * @param max 最終ページ
     * @param parts ボタンの前後に表示する枠パーツ
     * @param returnCommand 戻るボタンに設定するコマンド、nullを指定したら戻るボタンは表示しない
     */
    private void sendPager(MailSender sender, String commandPre, String commandSuf,
            int page, int max,
            String parts, String returnCommand) {

        String firstLabel = Messages.get("FirstPage");
        String prevLabel = Messages.get("PrevPage");
        String nextLabel = Messages.get("NextPage");
        String lastLabel = Messages.get("LastPage");
        String firstToolTip = Messages.get("FirstPageToolTip");
        String prevToolTip = Messages.get("PrevPageToolTip");
        String nextToolTip = Messages.get("NextPageToolTip");
        String lastToolTip = Messages.get("LastPageToolTip");

        MessageComponent msg = new MessageComponent();

        msg.addText(parts + " ");

        if ( returnCommand != null ) {
            MessageParts returnButton = new MessageParts(
                    Messages.get("Return"), ChatColor.AQUA);
            returnButton.setClickEvent(ClickEventType.RUN_COMMAND, returnCommand);
            returnButton.addHoverText(Messages.get("ReturnToolTip"));
            msg.addParts(returnButton);

            msg.addText(" ");
        }

        if ( page > 1 ) {
            MessageParts firstButton = new MessageParts(
                    firstLabel, ChatColor.AQUA);
            firstButton.setClickEvent(ClickEventType.RUN_COMMAND,
                    commandPre + " 1" + commandSuf);
            firstButton.addHoverText(firstToolTip);
            msg.addParts(firstButton);

            msg.addText(" ");

            MessageParts prevButton = new MessageParts(
                    prevLabel, ChatColor.AQUA);
            prevButton.setClickEvent(ClickEventType.RUN_COMMAND,
                    commandPre + " " + (page - 1) + commandSuf);
            prevButton.addHoverText(prevToolTip);
            msg.addParts(prevButton);

        } else {
            msg.addText(firstLabel + " " + prevLabel, ChatColor.WHITE);

        }

        msg.addText(" (" + page + "/" + max + ") ");

        if ( page < max ) {
            MessageParts nextButton = new MessageParts(
                    nextLabel, ChatColor.AQUA);
            nextButton.setClickEvent(ClickEventType.RUN_COMMAND,
                    commandPre + " " + (page + 1) + commandSuf);
            nextButton.addHoverText(nextToolTip);
            msg.addParts(nextButton);

            msg.addText(" ");

            MessageParts lastButton = new MessageParts(
                    lastLabel, ChatColor.AQUA);
            lastButton.setClickEvent(ClickEventType.RUN_COMMAND,
                    commandPre + " " + max + commandSuf);
            lastButton.addHoverText(lastToolTip);
            msg.addParts(lastButton);

        } else {
            msg.addText(nextLabel + " " + lastLabel, ChatColor.WHITE);
        }

        msg.addText(" " + parts);

        msg.send(sender);
    }
}
