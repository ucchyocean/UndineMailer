/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.Utility;
import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.group.GroupManager;
import org.bitbucket.ucchy.undine.group.GroupPermissionMode;
import org.bitbucket.ucchy.undine.group.SpecialGroupPex;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.github.ucchyocean.messaging.tellraw.ClickEventType;
import com.github.ucchyocean.messaging.tellraw.MessageComponent;
import com.github.ucchyocean.messaging.tellraw.MessageParts;

/**
 * groupコマンド
 * @author ucchy
 */
public class GroupCommand implements TabExecutor {

    public static final String COMMAND = "/ugroup";
    public static final String PERMISSION = "undine.group";
    public static final String PERMISSION_INFINITE_CREATE = PERMISSION + ".infinite-create";
    public static final String PERMISSION_INFINITE_ADD_MEMBER = PERMISSION + ".infinite-add-member";

    private static final String[] COMMANDS = new String[]{
        "create", "delete", "list", "detail", "add", "addalllogin", "remove", "perm"
    };

    private UndineMailer parent;

    /**
     * コンストラクタ
     * @param parent
     */
    public GroupCommand(UndineMailer parent) {
        this.parent = parent;
    }

    /**
     * コマンドが実行された時に呼び出されるメソッド
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 引数なしの場合は、listを開く
        if ( args.length == 0 ) {
            return doListCommand(sender, command, label, new String[]{"list"});
        }

        // 後は、そのままそれぞれのサブコマンドを実行するようにする。
        if ( args[0].equalsIgnoreCase("create") ) {
            return doCreateCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("delete") ) {
            return doDeleteCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("list") ) {
            return doListCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("detail") ) {
            return doDetailCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("add") ) {
            return doAddCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("addalllogin") ) {
            return doAddAllLoginCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("remove") ) {
            return doRemoveCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("perm") ) {
            return doPermCommand(sender, command, label, args);
        }

        return false;
    }

    /**
     * タブキーで補完された時に呼び出されるメソッド
     * @see org.bukkit.command.TabCompleter#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if ( args.length == 1 ) {
            // コマンド名で補完する
            String arg = args[0].toLowerCase();
            ArrayList<String> coms = new ArrayList<String>();
            for ( String c : COMMANDS ) {
                if ( c.startsWith(arg) &&
                        sender.hasPermission(PERMISSION + "." + c) ) {
                    coms.add(c);
                }
            }
            return coms;

        } else if ( args.length == 2 &&
                ( args[0].equalsIgnoreCase("delete")
                        || args[0].equalsIgnoreCase("add")
                        || args[0].equalsIgnoreCase("addalllogin")
                        || args[0].equalsIgnoreCase("remove") ) ) {
            // delete、add、removeコマンドの2つ目は、
            // 変更権限を持っているグループ名で補完する

            if ( !sender.hasPermission(PERMISSION + "." + args[0].toLowerCase()) ) {
                return new ArrayList<String>();
            }

            MailSender ms = MailSender.getMailSender(sender);
            boolean isAdd = args[0].equalsIgnoreCase("add")
                    || args[0].equalsIgnoreCase("addalllogin");

            // グループ名で補完する
            String arg = args[1].toLowerCase();
            ArrayList<GroupData> groups = parent.getGroupManager().getAllGroups();
            ArrayList<String> candidates = new ArrayList<String>();
            for ( GroupData group : groups ) {
                String name = group.getName().toLowerCase();
                if ( name.startsWith(arg) ) {
                    if ( isAdd && group.canModify(ms) ) {
                        candidates.add(group.getName());
                    } else if ( !isAdd && group.canBreakup(ms) ) {
                        candidates.add(group.getName());
                    }
                }
            }
            return candidates;

        } else if ( ( args.length == 2 && args[0].equalsIgnoreCase("detail") ) ) {
            // detailコマンドの2つ目は、全てのグループ名で補完する

            if ( !sender.hasPermission(PERMISSION + ".detail") ) {
                return new ArrayList<String>();
            }

            // グループ名で補完する
            String arg = args[1].toLowerCase();
            ArrayList<String> candidates = new ArrayList<String>();
            for ( String name : parent.getGroupManager().getAllGroupNames() ) {
                if ( name.toLowerCase().startsWith(arg) ) {
                    candidates.add(name);
                }
            }
            return candidates;

        }

        return null;
    }

    private boolean doCreateCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".create") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "GroupName"));
            return true;
        }

        String name = args[1];
        GroupManager manager = parent.getGroupManager();
        MailSender ms = MailSender.getMailSender(sender);

        // 作成可能数を超えた場合はエラーを表示して終了
        if ( !sender.hasPermission(PERMISSION_INFINITE_CREATE) ) {
            int num = manager.getOwnerGroupCount(ms);
            int limit = parent.getUndineConfig().getMaxCreateGroup();
            if ( num >= limit ) {
                sender.sendMessage(Messages.get("ErrorGroupCreateLimitExceed", "%num", limit));
                return true;
            }
        }

        // グループ名として使用できない場合はエラーを表示して終了
        if ( !GroupManager.canUseNameFromGroup(name) ) {
            sender.sendMessage(Messages.get("ErrorInvalidGroupName", "%name", name));
            return true;
        }
        if ( name.toLowerCase().startsWith(SpecialGroupPex.NAME_PREFIX) ) {
            sender.sendMessage(Messages.get("ErrorInvalidGroupName", "%name", name));
            return true;
        }

        // 既に存在するグループ名が指定された場合はエラーを表示して終了
        if ( manager.existGroupName(name) ) {
            sender.sendMessage(Messages.get("ErrorGroupIsAlreadyExist", "%name", name));
            return true;
        }
        
        // グループ作成　保存もこのメソッドが行ってくれる
        GroupData.create(parent, name, ms);

        // グループリスト表示
        manager.displayGroupList(ms, 1);

        sender.sendMessage(Messages.get("InformationMakeGroup", "%name", name));

        return true;
    }

    private boolean doDeleteCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".delete") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "GroupName"));
            return true;
        }

        String name = args[1];
        GroupManager manager = parent.getGroupManager();

        // 存在しないグループ名が指定された場合はエラーを表示して終了
        if ( !manager.existGroupName(name) ) {
            sender.sendMessage(Messages.get("ErrorGroupNotExist", "%name", name));
            return true;
        }

        GroupData group = manager.getGroup(name);
        MailSender ms = MailSender.getMailSender(sender);

        // グループを削除する権限が無い場合はエラーを表示して終了
        if ( !group.canBreakup(ms) ) {
            sender.sendMessage(Messages.get("ErrorGroupDeleteNotPermission", "%name", name));
            return true;
        }

        if ( args.length >= 3 && args[2].equals("confirm") ) {
            // グループ削除
            manager.removeGroup(name);

            // グループリスト表示
            manager.displayGroupList(MailSender.getMailSender(sender), 1);

            sender.sendMessage(Messages.get("InformationDeleteGroup", "%name", name));

            return true;

        }

        // 削除の確認メッセージを表示する
        sender.sendMessage(Messages.get("InformationDeleteGroupConfirm", "%name", name));
        showOKCancelButton(ms, COMMAND + " delete " + name + " confirm", COMMAND);

        return true;
    }

    private boolean doListCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".list") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        int page = 1;
        String next = "";
        if ( args.length >= 2 && args[1].matches("[0-9]{1,5}") ) {
            page = Integer.parseInt(args[1]);

            for ( int i=2; i<args.length; i++ ) {
                next += " " + args[i];
            }
        }

        // リスト表示
        if ( next.length() > 0 ) {
            parent.getGroupManager().displayGroupSelection(
                    MailSender.getMailSender(sender), page, next.trim());
        } else {
            parent.getGroupManager().displayGroupList(
                    MailSender.getMailSender(sender), page);
        }

        return true;
    }

    private boolean doDetailCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".detail") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "GroupName"));
            return true;
        }

        String name = args[1];
        GroupManager manager = parent.getGroupManager();

        // 存在しないグループ名が指定された場合はエラーを表示して終了
        if ( !manager.existGroupName(name) ) {
            sender.sendMessage(Messages.get("ErrorGroupNotExist", "%name", name));
            return true;
        }

        GroupData group = manager.getGroup(name);
        MailSender ms = MailSender.getMailSender(sender);

        // グループの詳細表示（リードオンリー）
        if ( !group.canModify(ms) ) {
            manager.displayGroupDetailReadOnly(ms, group);
            return true;
        }

        // グループの設定表示
        if ( args.length >= 3 && args[2].equals("setting") ) {
            manager.displayGroupSetting(ms, group);
            return true;
        }

        // グループのメンバー表示
        int page = 1;
        if ( args.length >= 3 && args[2].matches("[0-9]{1,5}") ) {
            page = Integer.parseInt(args[2]);
        }
        manager.displayGroupDetailModifyMode(ms, group, page);

        return true;
    }

    private boolean doAddCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".add") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "GroupName"));
            return true;
        }

        if ( args.length < 3 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "PlayerName"));
            return true;
        }

        String gname = args[1];
        String pname = args[2];
        GroupManager manager = parent.getGroupManager();

        // 存在しないグループ名が指定された場合はエラーを表示して終了
        if ( !manager.existGroupName(gname) ) {
            sender.sendMessage(Messages.get("ErrorGroupNotExist", "%name", gname));
            return true;
        }

        GroupData group = manager.getGroup(gname);
        MailSender ms = MailSender.getMailSender(sender);
        MailSender target = MailSender.getMailSenderFromString(pname);

        // 追加可能数を超えた場合はエラーを表示して終了
        if ( !sender.hasPermission(PERMISSION_INFINITE_ADD_MEMBER) ) {
            int num = group.getMembers().size();
            int limit = parent.getUndineConfig().getMaxGroupMember();
            if ( num >= limit ) {
                sender.sendMessage(Messages.get("ErrorGroupMemberLimitExceed", "%num", limit));
                return true;
            }
        }

        // グループを編集する権限が無い場合はエラーを表示して終了
        if ( !group.canModify(ms) ) {
            sender.sendMessage(Messages.get("ErrorGroupModifyNotPermission", "%name", gname));
            return true;
        }

        // 追加するプレイヤーが存在しないならエラーを表示して終了
        if ( !target.isValidDestination() ) {
            sender.sendMessage(Messages.get("ErrorNotFoundPlayer", "%player", pname));
            return true;
        }

        // 既に追加されているならエラーを表示して終了
        if ( group.getMembers().contains(target) ) {
            sender.sendMessage(Messages.get("ErrorPlayerIsAlreadyMember", "%player", pname));
            return true;
        }

        // メンバーを追加する
        group.addMember(target);
        manager.saveGroupData(group);

        // グループ編集画面を表示
        manager.displayGroupDetailModifyMode(ms, group, 1);

        sender.sendMessage(
                Messages.get("InformationGroupMemberAdd",
                        new String[]{"%player", "%group"},
                        new String[]{pname, gname}));

        return true;
    }

    private boolean doAddAllLoginCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".addalllogin") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "GroupName"));
            return true;
        }

        String gname = args[1];
        GroupManager manager = parent.getGroupManager();

        // 存在しないグループ名が指定された場合はエラーを表示して終了
        if ( !manager.existGroupName(gname) ) {
            sender.sendMessage(Messages.get("ErrorGroupNotExist", "%name", gname));
            return true;
        }

        GroupData group = manager.getGroup(gname);
        MailSender ms = MailSender.getMailSender(sender);

        // グループを編集する権限が無い場合はエラーを表示して終了
        if ( !group.canModify(ms) ) {
            sender.sendMessage(Messages.get("ErrorGroupModifyNotPermission", "%name", gname));
            return true;
        }

        int count = 0;

        for ( Player player : Utility.getOnlinePlayers() ) {

            // 追加可能数を超えた場合はエラーを表示して終了
            if ( !sender.hasPermission(PERMISSION_INFINITE_ADD_MEMBER) ) {
                int num = group.getMembers().size();
                int limit = parent.getUndineConfig().getMaxGroupMember();
                if ( num >= limit ) {
                    sender.sendMessage(Messages.get("ErrorGroupMemberLimitExceed", "%num", limit));
                    break;
                }
            }

            MailSender target = MailSender.getMailSender(player);

            // 既に追加されているなら次へ
            if ( group.getMembers().contains(target) ) {
                continue;
            }

            // メンバーを追加する
            group.addMember(target);
            count++;
        }

        // 1人以上追加されたなら、グループを保存する
        if ( count > 0 ) {
            manager.saveGroupData(group);
        }

        // グループ編集画面を表示
        manager.displayGroupDetailModifyMode(ms, group, 1);

        sender.sendMessage(
                Messages.get("InformationGroupMemberAddAllLogin",
                        new String[]{"%num", "%group"},
                        new String[]{count + "", gname}));

        return true;
    }

    private boolean doRemoveCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".remove") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "GroupName"));
            return true;
        }

        if ( args.length < 3 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "PlayerName"));
            return true;
        }

        String gname = args[1];
        String pname = args[2];
        GroupManager manager = parent.getGroupManager();

        // 存在しないグループ名が指定された場合はエラーを表示して終了
        if ( !manager.existGroupName(gname) ) {
            sender.sendMessage(Messages.get("ErrorGroupNotExist", "%name", gname));
            return true;
        }

        GroupData group = manager.getGroup(gname);
        MailSender ms = MailSender.getMailSender(sender);
        MailSender target = MailSender.getMailSenderFromString(pname);

        // グループを編集する権限が無い場合はエラーを表示して終了
        if ( !group.canModify(ms) ) {
            sender.sendMessage(Messages.get("ErrorGroupModifyNotPermission", "%name", gname));
            return true;
        }

        // 削除するプレイヤーがメンバーでないならエラーを表示して終了
        if ( !group.getMembers().contains(target) ) {
            sender.sendMessage(Messages.get("ErrorPlayerIsNotMember", "%player", pname));
            return true;
        }

        // 削除するプレイヤーがグループのオーナーならエラーを表示して終了
        if ( group.getOwner().equals(target) ) {
            sender.sendMessage(Messages.get("ErrorPlayerIsOwner", "%player", pname));
            return true;
        }

        // メンバーを削除する
        group.removeMember(target);
        manager.saveGroupData(group);

        // グループ編集画面を表示
        manager.displayGroupDetailModifyMode(ms, group, 1);

        sender.sendMessage(
                Messages.get("InformationGroupMemberRemove",
                        new String[]{"%player", "%group"},
                        new String[]{pname, gname}));

        return true;
    }

    private boolean doPermCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".remove") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "GroupName"));
            return true;
        }

        if ( args.length < 3 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "PermType"));
            return true;
        }

        if ( args.length < 4 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "PermValue"));
            return true;
        }

        String gname = args[1];
        GroupManager manager = parent.getGroupManager();

        // 存在しないグループ名が指定された場合はエラーを表示して終了
        if ( !manager.existGroupName(gname) ) {
            sender.sendMessage(Messages.get("ErrorGroupNotExist", "%name", gname));
            return true;
        }

        GroupData group = manager.getGroup(gname);
        MailSender ms = MailSender.getMailSender(sender);

        String ptype = args[2];

        // 正しくない権限タイプが指定された場合はエラーを表示して終了
        if ( !ptype.equalsIgnoreCase("send") && !ptype.equalsIgnoreCase("modify")
                && !ptype.equalsIgnoreCase("dissolution") ) {
            sender.sendMessage(Messages.get("ErrorInvalidPermissionType", "%type", args[2]));
            return true;
        }

        String pvalue = args[3];
        GroupPermissionMode mode = GroupPermissionMode.getFromString(pvalue);

        // 正しくない権限設定値が指定された場合はエラーを表示して終了
        if ( mode == null ) {
            sender.sendMessage(Messages.get("ErrorInvalidPermissionValue", "%value", args[3]));
            return true;
        }

        // 該当グループの権限を持っていない場合はエラーを表示して終了
        if ( ptype.equalsIgnoreCase("send") && !group.canModify(ms) ) {
            // 送信権限の変更は、変更権限に従う。ちょっとややこしい
            sender.sendMessage(Messages.get("ErrorGroupModifyNotPermission", "%name", gname));
            return true;
        } else if ( ptype.equalsIgnoreCase("modify") && !group.canModify(ms) ) {
            sender.sendMessage(Messages.get("ErrorGroupModifyNotPermission", "%name", gname));
            return true;
        } else if ( ptype.equalsIgnoreCase("dissolution")
                && (!group.canBreakup(ms) || !group.canModify(ms)) ) {
            // 解散権限の変更は、変更権限と解散権限の両方が必要。ちょっとややこしい
            sender.sendMessage(Messages.get("ErrorGroupModifyNotPermission", "%name", gname));
            return true;
        }

        if ( (ptype.equalsIgnoreCase("modify") || ptype.equalsIgnoreCase("dissolution"))
                && mode == GroupPermissionMode.EVERYONE ) {
            sender.sendMessage(Messages.get("ErrorEveryonePermissionInvalid"));
            return true;
        }

        // 権限を設定する
        if ( ptype.equalsIgnoreCase("send") ) {
            group.setSendMode(mode);
        } else if ( ptype.equalsIgnoreCase("modify") ) {
            group.setModifyMode(mode);
        } else if ( ptype.equalsIgnoreCase("dissolution") ) {
            group.setDissolutionMode(mode);
        }

        manager.saveGroupData(group);

        if ( group.canModify(ms) ) {
            // グループ設定画面を表示する
            manager.displayGroupSetting(ms, group);
        } else {
            // 権限を変更することで、編集権限を失ってしまった場合は、
            // 一旦グループリストまで戻る
            manager.displayGroupList(ms, 1);
        }

        // メッセージを表示
        sender.sendMessage(Messages.get(
                "InformationGroupSetPermission",
                new String[]{"%name", "%type", "%value"},
                new String[]{group.getName(), ptype, mode.getDisplayString()}));

        return true;
    }

    /**
     * OKボタンとキャンセルボタンを表示して、確認をとるtellrawメッセージを表示する。
     * @param ms メッセージ送信先
     * @param okCommand OKボタンを押した時に実行するコマンド
     * @param cancelCommand キャンセルボタンを押した時に実行するコマンド
     */
    private void showOKCancelButton(
            MailSender ms, String okCommand, String cancelCommand) {

        MessageComponent msg = new MessageComponent();
        msg.addText("     ");
        MessageParts buttonOK = new MessageParts(
                Messages.get("ButtonOK"), ChatColor.AQUA);
        buttonOK.setClickEvent(ClickEventType.RUN_COMMAND, okCommand);
        msg.addParts(buttonOK);
        msg.addText("     ");
        MessageParts buttonCancel = new MessageParts(
                Messages.get("ButtonCancel"), ChatColor.AQUA);
        buttonCancel.setClickEvent(ClickEventType.RUN_COMMAND, cancelCommand);
        msg.addParts(buttonCancel);

        ms.sendMessageComponent(msg);
    }
}
