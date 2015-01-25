/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.group;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

/**
 * groupコマンド
 * @author ucchy
 */
public class GroupCommand implements TabExecutor {

    protected static final String COMMAND = "/ugroup";
    protected static final String PERMISSION = "undine.group";
    private static final String[] COMMANDS = new String[]{
        "create", "delete", "list", "detail", "add", "remove",
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
        } else if ( args[0].equalsIgnoreCase("remove") ) {
            return doRemoveCommand(sender, command, label, args);
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
                        || args[0].equalsIgnoreCase("remove") ) ) {
            // delete、add、removeコマンドの2つ目は、
            // 変更権限を持っているグループ名で補完する

            if ( !sender.hasPermission(PERMISSION + ".delete") ) {
                return new ArrayList<String>();
            }

            MailSender ms = MailSender.getMailSender(sender);

            // グループ名で補完する
            String arg = args[1].toLowerCase();
            ArrayList<GroupData> groups = parent.getGroupManager().getAllGroups();
            ArrayList<String> candidates = new ArrayList<String>();
            for ( GroupData group : groups ) {
                String name = group.getName().toLowerCase();
                if ( name.startsWith(arg) && group.canBreakup(ms) ) {
                    candidates.add(group.getName());
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

        // グループ名として使用できない場合はエラーを表示して終了
        if ( !GroupManager.canUseNameFromGroup(name) ) {
            sender.sendMessage(Messages.get("ErrorInvalidGroupName", "%name", name));
            return true;
        }

        // 既に存在するグループ名が指定された場合はエラーを表示して終了
        if ( manager.existGroupName(name) ) {
            sender.sendMessage(Messages.get("ErrorGroupIsAlreadyExist", "%name", name));
            return true;
        }

        // グループ作成
        manager.addGroup(new GroupData(name, MailSender.getMailSender(sender)));

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

        // グループ削除
        manager.removeGroup(name);

        return true;
    }

    private boolean doListCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".list") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        int page = 1;
        if ( args.length >= 2 && args[1].matches("[0-9]{1,5}") ) {
            page = Integer.parseInt(args[1]);
        }

        // リスト表示
        parent.getGroupManager().displayGroupList(MailSender.getMailSender(sender), page);

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

        int page = 1;
        if ( args.length >= 3 && args[2].matches("[0-9]{1,5}") ) {
            page = Integer.parseInt(args[2]);
        }

        // グループの詳細表示
        if ( group.canModify(ms) ) {
            manager.displayGroupDetailModifyMode(ms, group, page);
        } else {
            manager.displayGroupDetailReadOnly(ms, group);
        }

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
}
