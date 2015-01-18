/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/**
 * Undineコマンドクラス
 * @author ucchy
 */
public class UndineCommand implements TabExecutor {

    private static final String PERMISSION = "undine.command";

    private Undine parent;
    private MailManager manager;
    private UndineConfig config;

    public UndineCommand(Undine parent) {
        this.parent = parent;
        manager = parent.getMailManager();
        config = parent.getUndineConfig();
    }

    /**
     * コマンドが実行された時に呼び出されるメソッド
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 引数なしの場合は、inboxコマンドを実行
        if ( args.length == 0 ) {
            return doInboxCommand(sender, command, label, new String[]{"inbox"});
        }

        // 後は、そのままそれぞれのサブコマンドを実行するようにする。
        if ( args[0].equalsIgnoreCase("inbox") ) {
            return doInboxCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("outbox") ) {
            return doOutboxCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("read") ) {
            return doReadCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("text") ) {
            return doTextCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("write") ) {
            return doWriteCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("to") ) {
            return doToCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("message") ) {
            return doMessageCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("attach") ) {
            return doAttachCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("send") ) {
            return doSendCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("cancel") ) {
            return doCancelCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("reload") ) {
            return doReloadCommand(sender, command, label, args);
        }

        return false;
    }

    /**
     * タブキーで補完された時に呼び出されるメソッド
     * @see org.bukkit.command.TabCompleter#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        return null;
    }

    private boolean doInboxCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".inbox") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        int page = 1;
        if ( args.length >= 2 && args[1].matches("[0-9]{1,9}") ) {
            page = Integer.parseInt(args[1]);
        }

        parent.getMailManager().displayInboxList(
                MailSender.getMailSender(sender), page);

        return true;
    }

    private boolean doOutboxCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".outbox") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        int page = 1;
        if ( args.length >= 2 && args[1].matches("[0-9]{1,9}") ) {
            page = Integer.parseInt(args[1]);
        }

        parent.getMailManager().displayOutboxList(
                MailSender.getMailSender(sender), page);

        return true;
    }

    private boolean doReadCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".read") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "MailIndex"));
            return true;
        }

        // 指定されたパラメータが数字(正の整数)でない場合はエラーを表示して終了
        if ( !args[1].matches("[0-9]{1,9}") ) {
            sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[1]));
            return true;
        }

        int index = Integer.parseInt(args[1]);
        MailData mail = manager.getMail(index);

        // メールが見つからない場合はエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[1]));
            return true;
        }

        // 他人のメールだった場合はエラーを表示して終了
        if ( !mail.isRelatedWith(MailSender.getMailSender(sender)) &&
                !sender.hasPermission(PERMISSION + ".read-all") ) {
            sender.sendMessage(Messages.get("ErrorNoneReadPermission"));
            return true;
        }

        // 該当のメールを表示
        manager.displayMail(MailSender.getMailSender(sender), mail);

        return true;
    }

    private boolean doTextCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".create") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "To"));
            return true;
        }

        if ( args.length < 3 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Message"));
            return true;
        }

        String[] dests = args[1].split(",");
        ArrayList<MailSender> targets = new ArrayList<MailSender>();

        for ( String d : dests ) {
            MailSender target = MailSender.getMailSenderFromString(d);
            if ( target == null || !target.isValidDestination() ) {
                // 宛先が見つからない場合はエラーを表示
                sender.sendMessage(Messages.get("ErrorNotFoundDestination", "%dest", d));
            } else if ( !config.isEnableSendSelf() && target.equals(sender) ) {
                // 自分自身が指定不可の設定の場合は、自分自身が指定されたらエラーを表示
                sender.sendMessage(Messages.get("ErrorCannotSendSelf"));
            } else if ( !targets.contains(target) ) {
                targets.add(target);
            }
        }

        // 結果として宛先が一つもないなら、メールを送信せずにそのまま終了
        if ( targets.size() == 0 ) {
            return true;
        }

        // メールを送信する
        manager.sendNewMail(MailSender.getMailSender(sender), targets, args[2]);

        return true;
    }

    private boolean doWriteCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".write") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // 編集メールを取得して、編集画面を表示する。
        MailData mail = manager.makeEditmodeMail(MailSender.getMailSender(sender));
        mail.displayEditmode(MailSender.getMailSender(sender), config);

        return true;
    }

    private boolean doToCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".to") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        MailData mail = manager.getEditmodeMail(MailSender.getMailSender(sender));

        // 編集中でないならエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorNotInEditmode"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Index Number"));
            return true;
        }

        if ( args.length < 3 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Destination"));
            return true;
        }

        if ( args[1].matches("[0-9]{1,2}") ) {
            // 2番めの引数に数値が来た場合は、追加/再設定
            int line = Integer.parseInt(args[1]) - 1;
            if ( line < 0 ) {
                line = 0;
            } else if ( line > mail.getTo().size() ) {
                line = mail.getTo().size();
            }
            if ( line >= MailData.TO_MAX_SIZE ) {
                sender.sendMessage(
                        Messages.get("ErrorTooManyDestination", "%num", MailData.TO_MAX_SIZE));
                return true;
            }

            MailSender target = MailSender.getMailSenderFromString(args[2]);

            if ( target == null || !target.isValidDestination() ) {
                // 宛先が見つからない場合はエラーを表示
                sender.sendMessage(Messages.get("ErrorNotFoundDestination", "%dest", args[2]));
                return true;
            } else if ( !config.isEnableSendSelf() && target.equals(sender) ) {
                // 自分自身が指定不可の設定の場合は、自分自身が指定されたらエラーを表示
                sender.sendMessage(Messages.get("ErrorCannotSendSelf"));
                return true;
            } else if ( mail.getTo().contains(target) ) {
                // 既に指定済みの宛先が再度指定された場合は、エラーを表示
                sender.sendMessage(Messages.get("ErrorAlreadyExistTo"));
                return true;
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

        }

        // 編集画面を表示する。
        mail.displayEditmode(MailSender.getMailSender(sender), config);

        return true;
    }

    private boolean doMessageCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".message") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        MailData mail = manager.getEditmodeMail(MailSender.getMailSender(sender));

        // 編集中でないならエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorNotInEditmode"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Index Number"));
            return true;
        }

        if ( args[1].matches("[0-9]{1,2}") ) {
            // 2番めの引数に数値が来た場合は、追加/再設定
            int line = Integer.parseInt(args[1]) - 1;
            if ( line < 0 ) {
                line = 0;
            } else if ( line >= MailData.MESSAGE_MAX_SIZE ) {
                line = MailData.MESSAGE_MAX_SIZE - 1;
            }

            StringBuffer message = new StringBuffer();
            for ( int i=2; i<args.length; i++ ) {
                if ( message.length() > 0 ) {
                    message.append(" ");
                }
                message.append(args[i]);
            }

            mail.setMessage(line, message.toString());

        } else if ( args[1].equalsIgnoreCase("delete")
                && args.length >= 3
                && args[2].matches("[0-9]{1,2}") ) {
            // 2番めの引数にdeleteが来た場合は、削除
            int line = Integer.parseInt(args[2]) - 1;
            if ( line < 0 ) {
                line = 0;
            } else if ( line >= mail.getMessage().size() ) {
                line = mail.getMessage().size() - 1;
            }
            mail.deleteMessage(line);

        }

        // 編集画面を表示する。
        mail.displayEditmode(MailSender.getMailSender(sender), config);

        return true;
    }

    private boolean doAttachCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".attach") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // このコマンドは、ゲーム内からの実行でない場合はエラーを表示して終了する
        if ( !(sender instanceof Player) ) {
            sender.sendMessage(Messages.get("ErrorInGameCommand"));
            return true;
        }

        Player player = (Player)sender;

        if ( args.length == 1 ) {
            MailData mail = manager.getEditmodeMail(MailSender.getMailSender(sender));

            // 編集中でないならエラーを表示して終了
            if ( mail == null ) {
                sender.sendMessage(Messages.get("ErrorNotInEditmode"));
                return true;
            }

            // 添付ボックスを表示する
            parent.getBoxManager().displayAttachBox(player, mail);

        } else if (args[1].matches("[0-9]{1,9}") ) {
            int index = Integer.parseInt(args[1]);
            MailData mail = manager.getMail(index);

            // 他人のメールだった場合はエラーを表示して終了
            if ( !mail.getTo().contains(MailSender.getMailSender(sender) ) &&
                    !sender.hasPermission(PERMISSION + ".read-all") ) {
                sender.sendMessage(Messages.get("ErrorNoneReadPermission"));
                return true;
            }

            // 添付ボックスを表示する
            parent.getBoxManager().displayAttachBox(player, mail);

        }

        return true;
    }

    private boolean doSendCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".send") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        MailData mail = manager.getEditmodeMail(MailSender.getMailSender(sender));

        // 編集中でないならエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorNotInEditmode"));
            return true;
        }

        // 宛先が設定されていないならエラーを表示して終了
        if ( mail.getTo().size() == 0 ) {

        }

        // 送信
        manager.sendNewMail(mail);
        manager.clearEditmodeMail(MailSender.getMailSender(sender));

        return true;
    }

    private boolean doCancelCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".cancel") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        MailData mail = manager.getEditmodeMail(MailSender.getMailSender(sender));

        // 編集中でないならエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorNotInEditmode"));
            return true;
        }

        // キャンセル
        manager.clearEditmodeMail(MailSender.getMailSender(sender));
        sender.sendMessage(Messages.get("InformationEditCancelled"));

        return true;
    }

    private boolean doReloadCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".reload") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // データをリロードする
        manager.reload();
        config.reloadConfig();
        Messages.reload(config.getLang());
        sender.sendMessage(Messages.get("InformationReload"));
        return true;
    }
}
