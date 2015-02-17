/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.MailManager;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineConfig;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

/**
 * undine to コマンド
 * @author ucchy
 */
public class UndineToCommand implements SubCommand {

    private static final String NAME = "to";
    private static final String NODE = "undine." + NAME;

    private UndineMailer parent;
    private MailManager manager;
    private UndineConfig config;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineToCommand(UndineMailer parent) {
        this.parent = parent;
        this.manager = parent.getMailManager();
        this.config = parent.getUndineConfig();
    }

    /**
     * コマンドを取得します。
     * @return コマンド
     * @see org.bitbucket.ucchy.undine.command.SubCommand#getCommandName()
     */
    @Override
    public String getCommandName() {
        return NAME;
    }

    /**
     * パーミッションノードを取得します。
     * @return パーミッションノード
     * @see org.bitbucket.ucchy.undine.command.SubCommand#getPermissionNode()
     */
    @Override
    public String getPermissionNode() {
        return NODE;
    }

    /**
     * コマンドを実行します。
     * @param sender コマンド実行者
     * @param label 実行時のラベル
     * @param args 実行時の引数
     * @see org.bitbucket.ucchy.undine.command.SubCommand#runCommand(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public void runCommand(CommandSender sender, String label, String[] args) {

        MailData mail = manager.getEditmodeMail(MailSender.getMailSender(sender));

        // 編集中でないならエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorNotInEditmode"));
            return;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "IndexNumber"));
            return;
        }

        if ( args.length < 3 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Destination"));
            return;
        }

        if ( args[1].matches("[0-9]{1,2}") ) {
            // 2番めの引数に数値が来た場合は、追加/再設定
            int line = Integer.parseInt(args[1]) - 1;
            if ( line < 0 ) {
                line = 0;
            } else if ( line > mail.getTo().size() ) {
                line = mail.getTo().size();
            }
            if ( line >= config.getMaxDestination() ) {
                sender.sendMessage(
                        Messages.get("ErrorTooManyDestination", "%num",
                                config.getMaxDestination()));
                return;
            }

            MailSender target = MailSender.getMailSenderFromString(args[2]);

            if ( target == null || !target.isValidDestination() ) {
                // 宛先が見つからない場合はエラーを表示
                sender.sendMessage(Messages.get("ErrorNotFoundDestination", "%dest", args[2]));
                return;
            } else if ( !config.isEnableSendSelf() && target.equals(sender) ) {
                // 自分自身が指定不可の設定の場合は、自分自身が指定されたらエラーを表示
                sender.sendMessage(Messages.get("ErrorCannotSendSelf"));
                return;
            } else if ( mail.getTo().contains(target) ) {
                // 既に指定済みの宛先が再度指定された場合は、エラーを表示
                sender.sendMessage(Messages.get("ErrorAlreadyExistTo"));
                return;
            }

            mail.setTo(line, target);

        } else if ( args[1].equalsIgnoreCase("delete") && args[2].matches("[0-9]{1,2}") ) {
            // 2番めの引数にdeleteが来た場合は、削除
            int line = Integer.parseInt(args[2]) - 1;
            if ( line < 0 ) {
                line = 0;
            } else if ( line >= mail.getTo().size() ) {
                line = mail.getTo().size() - 1;
            }
            mail.deleteTo(line);

        } else if ( args[1].equalsIgnoreCase("group") && args[2].matches("[0-9]{1,2}") ) {
            // 2番めの引数にgroupが来た場合は、グループ追加
            int line = Integer.parseInt(args[2]) - 1;
            if ( line < 0 ) {
                line = 0;
            } else if ( line > mail.getToGroups().size() ) {
                line = mail.getToGroups().size();
            }
            if ( line >= config.getMaxDestinationGroup() ) {
                sender.sendMessage(
                        Messages.get("ErrorTooManyDestination", "%num",
                                config.getMaxDestinationGroup()));
                return;
            }

            // パラメータが足りない場合はエラーを表示して終了
            if ( args.length < 4 ) {
                sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "GroupName"));
                return;
            }

            // 指定されたグループが見つからないなら、エラーを表示して終了
            GroupData group = parent.getGroupManager().getGroup(args[3]);
            if ( group == null ) {
                sender.sendMessage(Messages.get("ErrorGroupNotExist", "%name", args[3]));
                return;
            }

            // 既に指定済みの宛先が再度指定された場合は、エラーを表示して終了
            if ( mail.getToGroups().contains(group.getName()) ) {
                sender.sendMessage(Messages.get("ErrorAlreadyExistTo"));
                return;
            }

            // 送信権限の無いグループが指定された場合は、エラーを表示して終了
            if ( !group.canSend(MailSender.getMailSender(sender)) ) {
                sender.sendMessage(Messages.get(
                        "ErrorGroupSendNotPermission", "%name", group.getName()));
                return;
            }

            // 宛先グループ追加
            mail.setToGroup(line, group.getName());

        } else if ( args[1].equalsIgnoreCase("group")
                && args[2].equalsIgnoreCase("delete") ) {
            // 2番めの引数にgroup、3番目の引数にdeleteが来た場合は、グループ削除

            // パラメータが足りない場合はエラーを表示して終了
            if ( args.length < 4 ) {
                sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "IndexNumber"));
                return;
            }

            // 指定されたパラメータが数字(正の整数)でない場合はエラーを表示して終了
            if ( !args[3].matches("[0-9]{1,9}") ) {
                sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[3]));
                return;
            }

            int line = Integer.parseInt(args[3]) - 1;
            if ( line < 0 ) {
                line = 0;
            } else if ( line >= mail.getToGroups().size() ) {
                line = mail.getToGroups().size() - 1;
            }

            // 宛先グループ削除
            mail.deleteToGroup(line);

        }

        // 編集画面を表示する。
        manager.displayEditmode(MailSender.getMailSender(sender));
    }

    /**
     * TABキー補完を実行します。
     * @param sender コマンド実行者
     * @param args 補完時の引数
     * @return 補完候補
     * @see org.bitbucket.ucchy.undine.command.SubCommand#tabComplete(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {

        if ( args.length == 3 ) {
            // textコマンドの2つ目と、toコマンドの3つ目は、有効な宛先で補完する

            // プレイヤー名簿が利用不可の場合は、nullを返す
            //（オンラインプレイヤー名で補完される）
            if ( !parent.getUndineConfig().isEnablePlayerList() ) {
                return null;
            }

            // オフラインプレイヤー名で補完する
            String arg = args[2].toLowerCase();
            ArrayList<String> candidates = new ArrayList<String>();
            for ( OfflinePlayer player : UndineCommandUtil.getAllValidPlayers() ) {
                if ( player.getName().toLowerCase().startsWith(arg)
                        && !player.getName().equals(sender.getName())) {
                    candidates.add(player.getName());
                }
            }
            return candidates;
        }

        return null;
    }
}
