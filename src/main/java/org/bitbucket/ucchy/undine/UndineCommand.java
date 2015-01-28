/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.item.TradableMaterial;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bitbucket.ucchy.undine.tellraw.ClickEventType;
import org.bitbucket.ucchy.undine.tellraw.MessageComponent;
import org.bitbucket.ucchy.undine.tellraw.MessageParts;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Undineコマンドクラス
 * @author ucchy
 */
public class UndineCommand implements TabExecutor {

    private static final String COMMAND = UndineMailer.COMMAND;
    private static final String PERMISSION = "undine";
    private static final String[] COMMANDS = new String[]{
        "inbox", "outbox", "read", "text", "write", "to", "message",
        "attach", "costmoney", "costitem", "send", "cancel", "item", "reload",
    };

    private UndineMailer parent;
    private MailManager manager;
    private UndineConfig config;

    public UndineCommand(UndineMailer parent) {
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
        } else if ( args[0].equalsIgnoreCase("costmoney") ) {
            return doCostmoneyCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("costitem") ) {
            return doCostitemCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("send") ) {
            return doSendCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("cancel") ) {
            return doCancelCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("item") ) {
            return doItemCommand(sender, command, label, args);
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

        } else if ( ( args.length == 2 && args[0].equalsIgnoreCase("text")
                || args.length == 3 && args[0].equalsIgnoreCase("to") ) ) {
            // textコマンドの2つ目と、toコマンドの3つ目は、有効な宛先で補完する

            // プレイヤー名簿が利用不可の場合は、nullを返す
            //（オンラインプレイヤー名で補完される）
            if ( !parent.getUndineConfig().isEnablePlayerList() ) {
                return null;
            }

            // オフラインプレイヤー名で補完する
            String arg = args.length == 2 ?
                    args[1].toLowerCase() : args[2].toLowerCase();
            ArrayList<String> candidates = new ArrayList<String>();
            for ( OfflinePlayer player : getAllValidPlayers() ) {
                if ( player.getName().toLowerCase().startsWith(arg)
                        && !player.getName().equals(sender.getName())) {
                    candidates.add(player.getName());
                }
            }
            return candidates;

        } else if ( ( args.length == 2 && args[0].equalsIgnoreCase("costitem") ) ) {
            // costitemコマンドの2つ目は、マテリアル名で補完する
            String arg = args[1].toUpperCase();
            ArrayList<String> candidates = new ArrayList<String>();
            for ( TradableMaterial material : TradableMaterial.values() ) {
                if ( material.toString().startsWith(arg) ) {
                    candidates.add(material.toString());
                }
            }
            return candidates;

        }

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
        if  ( !sender.hasPermission(PERMISSION + ".text") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Destination"));
            return true;
        }

        if ( args.length < 3 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Message"));
            return true;
        }

        // メッセージ本文
        StringBuffer message = new StringBuffer();
        for ( int i=2; i<args.length; i++ ) {
            if ( message.length() > 0 ) {
                message.append(" ");
            }
            message.append(args[i]);
        }

        // 宛先
        MailSender ms = MailSender.getMailSender(sender);
        String[] dests = args[1].split(",");
        ArrayList<MailSender> targets = new ArrayList<MailSender>();
        ArrayList<GroupData> targetGroups = new ArrayList<GroupData>();

        for ( String d : dests ) {

            MailSender target = MailSender.getMailSenderFromString(d);
            if ( target == null || !target.isValidDestination() ) {
                // 宛先が見つからない場合は、グループを確認
                GroupData group = parent.getGroupManager().getGroup(d);
                if ( group != null ) {
                    if ( group.canSend(ms) ) {
                        targetGroups.add(group);
                    } else {
                        // 権限がなくて送れないグループは、エラーを表示
                        sender.sendMessage(Messages.get(
                                "ErrorGroupSendNotPermission", "%name", d));
                    }
                } else {
                    // グループも見つからないなら、エラーを表示
                    sender.sendMessage(Messages.get(
                            "ErrorNotFoundDestination", "%dest", d));
                }
            } else if ( !config.isEnableSendSelf() && target.equals(sender) ) {
                // 自分自身が指定不可の設定の場合は、自分自身が指定されたらエラーを表示
                sender.sendMessage(Messages.get("ErrorCannotSendSelf"));
            } else if ( !targets.contains(target) ) {
                targets.add(target);
            }
        }

        // 結果として宛先が一つもないなら、メールを送信せずにそのまま終了
        if ( (targets.size() + targetGroups.size()) == 0 ) {
            return true;
        }

        // メール生成
        MailData mail = new MailData(
                targets, ms, message.toString());
        for ( GroupData g : targetGroups ) {
            mail.setToGroup(mail.getToGroups().size(), g.getName());
        }

        // 送信にお金がかかる場合
        double fee = manager.getSendFee(mail);
        if ( (ms instanceof MailSenderPlayer) && fee > 0 ) {

            VaultEcoBridge eco = parent.getVaultEco();
            String feeDisplay = eco.format(fee);

            // 残金が足りないならエラーで終了
            if ( !eco.has(ms.getPlayer(), fee) ) {
                sender.sendMessage(Messages.get("ErrorYouDontHaveEnoughMoney"));
                return true;
            }

            // 引き落としの実行、ただし、エラーが返されたらエラーで終了
            if ( !eco.withdrawPlayer(ms.getPlayer(), fee) ) {
                sender.sendMessage(Messages.get("ErrorFailToWithdraw"));
                return true;
            }

            // 引き落としたことを通知
            double balance = parent.getVaultEco().getBalance(ms.getPlayer());
            sender.sendMessage(Messages.get("EditmodeFeeResult",
                    new String[]{"%fee", "%remain"},
                    new String[]{feeDisplay, eco.format(balance)}));
        }

        // メールを送信する
        manager.sendNewMail(mail);

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
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "IndexNumber"));
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
            if ( line >= config.getMaxDestination() ) {
                sender.sendMessage(
                        Messages.get("ErrorTooManyDestination", "%num",
                                config.getMaxDestination()));
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
                return true;
            }

            // パラメータが足りない場合はエラーを表示して終了
            if ( args.length < 4 ) {
                sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "GroupName"));
                return true;
            }

            // 指定されたグループが見つからないなら、エラーを表示して終了
            GroupData group = parent.getGroupManager().getGroup(args[3]);
            if ( group == null ) {
                sender.sendMessage(Messages.get("ErrorGroupNotExist", "%name", args[3]));
                return true;
            }

            // 既に指定済みの宛先が再度指定された場合は、エラーを表示して終了
            if ( mail.getToGroups().contains(group.getName()) ) {
                sender.sendMessage(Messages.get("ErrorAlreadyExistTo"));
                return true;
            }

            // 送信権限の無いグループが指定された場合は、エラーを表示して終了
            if ( !group.canSend(MailSender.getMailSender(sender)) ) {
                sender.sendMessage(Messages.get(
                        "ErrorGroupSendNotPermission", "%name", group.getName()));
                return true;
            }

            // 宛先グループ追加
            mail.setToGroup(line, group.getName());

        } else if ( args[1].equalsIgnoreCase("group")
                && args[2].equalsIgnoreCase("delete") ) {
            // 2番めの引数にgroup、3番目の引数にdeleteが来た場合は、グループ削除

            // パラメータが足りない場合はエラーを表示して終了
            if ( args.length < 4 ) {
                sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "IndexNumber"));
                return true;
            }

            // 指定されたパラメータが数字(正の整数)でない場合はエラーを表示して終了
            if ( !args[3].matches("[0-9]{1,9}") ) {
                sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[3]));
                return true;
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
        MailSender ms = MailSender.getMailSender(sender);

        if ( args.length == 1 ) {
            MailData mail = manager.getEditmodeMail(ms);

            // 編集中でないならエラーを表示して終了
            if ( mail == null ) {
                sender.sendMessage(Messages.get("ErrorNotInEditmode"));
                return true;
            }

            // 添付ボックス利用不可のワールドにいるなら、エラーを表示して終了
            if ( config.getDisableWorldsToOpenAttachBox().contains(ms.getWorldName()) ) {
                sender.sendMessage(Messages.get("ErrorInvalidAttachBoxWorld"));
                return true;
            }

            // 添付ボックスを表示する
            parent.getBoxManager().displayAttachBox(player, mail);

        } else if (args[1].matches("[0-9]{1,9}") ) {
            int index = Integer.parseInt(args[1]);
            MailData mail = manager.getMail(index);

            // 3番目のパラメータがcancelなら、添付のキャンセル処理
            if ( args.length >= 3 && args[2].equalsIgnoreCase("cancel") ) {

                // 既にキャンセル済みならエラーを表示して終了
                if ( mail.isAttachmentsCancelled() ) {
                    sender.sendMessage(Messages.get("ErrorAlreadyAttachCancelled"));
                    return true;
                }

                // 送信者ではないならエラーを表示して終了
                if ( !mail.getFrom().equals(ms) ) {
                    sender.sendMessage(Messages.get("ErrorNoneCancelAttachPermission"));
                    return true;
                }

                // 既に送信者がボックスを開いてしまったなら、エラーを表示して終了
                if ( mail.isAttachmentsOpened() ) {
                    sender.sendMessage(Messages.get("ErrorAlreadyRecipientOpened"));
                    return true;
                }

                // 添付をキャンセルする
                mail.cancelAttachments();
                manager.saveMail(mail);

                // 添付ボックスを表示する
                parent.getBoxManager().displayAttachBox(player, mail);

                return true;
            }

            // 以下、添付ボックスのオープン処理

            // 開く権限が無い場合はエラーを表示して終了
            if ( mail.getFrom().equals(ms) ) {
                if ( !mail.isAttachmentsCancelled() ) {
                    sender.sendMessage(Messages.get("ErrorNoneReadPermission"));
                    return true;
                }
            } else if ( mail.getTo().contains(ms) ) {
                if ( mail.isAttachmentsCancelled() ) {
                    sender.sendMessage(Messages.get("ErrorAlreadyAttachCancelled"));
                    return true;
                }
            } else if ( !sender.hasPermission(PERMISSION + ".attach-all") ) {
                sender.sendMessage(Messages.get("ErrorNoneReadPermission"));
                return true;
            }

            // 添付ボックス利用不可のワールドにいるなら、エラーを表示して終了
            if ( config.getDisableWorldsToOpenAttachBox().contains(ms.getWorldName()) ) {
                sender.sendMessage(Messages.get("ErrorInvalidAttachBoxWorld"));
                return true;
            }

            if ( mail.getCostMoney() > 0 ) {
                // 着払い料金が設定がされている場合

                VaultEcoBridge eco = parent.getVaultEco();
                double fee = mail.getCostMoney();
                String feeDisplay = eco.format(fee);

                if ( args.length >= 3 && args[2].equals("confirm") ) {

                    // 引き落とし
                    if ( !eco.has(ms.getPlayer(), fee) ) {
                        sender.sendMessage(Messages.get("ErrorYouDontHaveEnoughMoney"));
                        return true;
                    }
                    if ( !eco.withdrawPlayer(ms.getPlayer(), fee) ) {
                        sender.sendMessage(Messages.get("ErrorFailToWithdraw"));
                        return true;
                    }
                    double balance = eco.getBalance(ms.getPlayer());
                    sender.sendMessage(Messages.get("BoxOpenCostMoneyResult",
                            new String[]{"%fee", "%remain"},
                            new String[]{feeDisplay, eco.format(balance)}));

                    // メールの送信元に送金
                    OfflinePlayer from = mail.getFrom().getOfflinePlayer();
                    eco.depositPlayer(from, fee);
                    if ( mail.getFrom().isOnline() ) {
                        balance = eco.getBalance(from);
                        mail.getFrom().sendMessage(Messages.get("BoxOpenCostMoneySenderResult",
                                new String[]{"%to", "%fee", "%remain"},
                                new String[]{ms.getName(), feeDisplay, eco.format(balance)}));
                    }

                    // 着払いを0に設定して保存
                    mail.setCostMoney(0);
                    manager.saveMail(mail);

                    // 添付ボックスを表示する
                    parent.getBoxManager().displayAttachBox(player, mail);

                    return true;
                }

                // かかる金額を表示
                sender.sendMessage(Messages.get(
                        "BoxOpenCostMoneyInformation", "%fee", feeDisplay));

                if ( !eco.has(ms.getPlayer(), fee) ) {
                    // 残金が足りない
                    sender.sendMessage(Messages.get("ErrorYouDontHaveEnoughMoney"));
                    return true;
                }

                // 確認メッセージを表示
                sender.sendMessage(Messages.get("BoxOpenCostConfirm"));
                showOKCancelButton(ms,
                        COMMAND + " attach " + index + " confirm",
                        COMMAND + " read " + index);

                return true;

            } else if ( mail.getCostItem() != null ) {
                // 着払いアイテムが設定されている場合

                ItemStack fee = mail.getCostItem();

                if ( args.length >= 3 && args[2].equals("confirm") ) {

                    // 引き落とし
                    if ( !hasItem(player, fee) ) {
                        sender.sendMessage(Messages.get("ErrorYouDontHaveEnoughItem"));
                        return true;
                    }
                    consumeItem(player, fee);
                    sender.sendMessage(Messages.get("BoxOpenCostItemResult",
                            new String[]{"%material", "%amount"},
                            new String[]{fee.getType().toString(), fee.getAmount() + ""}));

                    // メールの送信元に送金
                    MailData reply = new MailData();
                    reply.setTo(0, mail.getFrom());
                    reply.setFrom(MailSenderConsole.getMailSenderConsole());
                    reply.setMessage(0, Messages.get(
                            "BoxOpenCostItemSenderResult",
                            new String[]{"%to", "%material", "%amount"},
                            new String[]{ms.getName(), fee.getType().toString(), fee.getAmount() + ""}));
                    reply.addAttachment(fee);
                    parent.getMailManager().sendNewMail(reply);

                    // 着払いをnullに設定して保存
                    mail.setCostItem(null);
                    manager.saveMail(mail);

                    // 添付ボックスを表示する
                    parent.getBoxManager().displayAttachBox(player, mail);

                    return true;
                }

                // かかる金額を表示
                sender.sendMessage(Messages.get(
                        "BoxOpenCostItemInformation",
                        new String[]{"%material", "%amount"},
                        new String[]{fee.getType().toString(), fee.getAmount() + ""}));

                if ( !hasItem(player, fee) ) {
                    // 残金が足りない
                    sender.sendMessage(Messages.get("ErrorYouDontHaveEnoughItem"));
                    return true;
                }

                // 確認メッセージを表示
                sender.sendMessage(Messages.get("BoxOpenCostConfirm"));
                showOKCancelButton(ms,
                        COMMAND + " attach " + index + " confirm",
                        COMMAND + " read " + index);

                return true;

            }

            // 添付ボックスを表示する
            parent.getBoxManager().displayAttachBox(player, mail);
        }

        return true;
    }

    private boolean doCostmoneyCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".costmoney") ) {
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
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Amount"));
            return true;
        }

        // 指定値が数値ではない場合はエラーを表示して終了
        double amount = tryParseDouble(args[1]);
        if ( amount < 0 ) {
            sender.sendMessage(Messages.get("ErrorInvalidCostMoney", "%fee", args[1]));
            return true;
        }

        // アイテムと料金を両方設定しようとしたら、エラーを表示して終了
        if ( amount > 0 && mail.getCostItem() != null ) {
            sender.sendMessage(Messages.get("ErrorCannotSetMoneyAndItem"));
            return true;
        }

        // 設定する
        mail.setCostMoney(amount);

        // 編集画面を表示する。
        mail.displayEditmode(MailSender.getMailSender(sender), config);
        return true;
    }

    private boolean doCostitemCommand(CommandSender sender, Command command2, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".costitem") ) {
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
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Item"));
            return true;
        }

        // 2番めのパラメータがremoveだったら、着払いアイテムを解除する
        if ( args[1].equalsIgnoreCase("remove") ) {
            mail.setCostItem(null);

            // 編集画面を表示して終了する
            mail.displayEditmode(MailSender.getMailSender(sender), config);
            return true;
        }

        // 指定値がアイテム表現形式ではない場合はエラーを表示して終了
        ItemStack item = getItemStackFromDescription(args[1]);
        if ( item == null ) {
            sender.sendMessage(Messages.get("ErrorInvalidCostItem", "%item", args[1]));
            return true;
        }

        // 取引可能な種類のアイテムでないなら、エラーを表示して終了
        if ( !TradableMaterial.isTradable(item.getType()) ) {
            sender.sendMessage(Messages.get("ErrorInvalidCostItem", "%item", args[1]));
            return true;
        }

        // アイテムと料金を両方設定しようとしたら、エラーを表示して終了
        if ( mail.getCostMoney() > 0 ) {
            sender.sendMessage(Messages.get("ErrorCannotSetMoneyAndItem"));
            return true;
        }

        // 個数も指定されている場合は、個数を反映する
        if ( args.length >= 3 && args[2].matches("[0-9]{1,9}") ) {
            item.setAmount(Integer.parseInt(args[2]));
        }

        // 設定する
        mail.setCostItem(item);

        // 編集画面を表示する。
        mail.displayEditmode(MailSender.getMailSender(sender), config);
        return true;
    }

    private boolean doSendCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".send") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        MailSender ms = MailSender.getMailSender(sender);
        MailData mail = manager.getEditmodeMail(ms);

        // 編集中でないならエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorNotInEditmode"));
            return true;
        }

        // 宛先が設定されていないならエラーを表示して終了
        if ( (mail.getTo().size() + mail.getToGroups().size()) == 0 ) {
            sender.sendMessage(Messages.get("ErrorEmptyTo"));
            return true;
        }

        // 添付ファイル付きのメールを複数の宛先に出そうとしたなら、エラーを表示して終了
        if ( mail.getAttachments().size() > 0
                && (mail.getTo().size() > 1 || mail.getToGroups().size() > 0) ) {
            sender.sendMessage(Messages.get("ErrorCannotSendMultiAttach"));
            return true;
        }

        // 添付ボックスの使用制限を超える場合は、エラーを表示して終了
        if ( mail.getAttachments().size() > 0
                && !sender.hasPermission(PERMISSION + ".attach-infinity")
                && manager.getAttachBoxUsageCount(ms) >= config.getMaxAttachmentBoxCount() ) {
            sender.sendMessage(Messages.get("ErrorAttachBoxCountExceed",
                    new String[]{"%num", "%limit"},
                    new String[]{manager.getAttachBoxUsageCount(ms) + "", config.getMaxAttachmentBoxCount() + ""}));
            return true;
        }

        // 送信にお金がかかる場合
        double fee = manager.getSendFee(mail);
        if ( (ms instanceof MailSenderPlayer) && fee > 0 ) {

            VaultEcoBridge eco = parent.getVaultEco();
            String feeDisplay = eco.format(fee);

            if ( args.length > 1 && args[1].equals("confirm") ) {
                // 課金して送信

                if ( !eco.has(ms.getPlayer(), fee) ) {
                    sender.sendMessage(Messages.get("ErrorYouDontHaveEnoughMoney"));
                    return true;
                }
                if ( !eco.withdrawPlayer(ms.getPlayer(), fee) ) {
                    sender.sendMessage(Messages.get("ErrorFailToWithdraw"));
                    return true;
                }
                double balance = parent.getVaultEco().getBalance(ms.getPlayer());
                sender.sendMessage(Messages.get("EditmodeFeeResult",
                        new String[]{"%fee", "%remain"},
                        new String[]{feeDisplay, eco.format(balance)}));

                manager.sendNewMail(mail);
                manager.clearEditmodeMail(ms);

                return true;
            }

            // かかる金額を表示
            sender.sendMessage(Messages.get(
                    "EditmodeFeeInformation", "%fee", feeDisplay));

            if ( !eco.has(ms.getPlayer(), fee) ) {
                // 残金が足りない
                sender.sendMessage(Messages.get("ErrorYouDontHaveEnoughMoney"));
                return true;
            }

            // 確認メッセージを表示
            sender.sendMessage(Messages.get("EditmodeFeeConfirm"));
            showOKCancelButton(ms,
                    COMMAND + " send confirm",
                    COMMAND + " write");

            return true;
        }

        // 送信
        manager.sendNewMail(mail);
        manager.clearEditmodeMail(ms);

        if ( sender instanceof Player ) {
            parent.getBoxManager().clearEditmodeBox((Player)sender);
        }

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

        // 添付ファイルが残っているなら、エラーを表示して終了
        if ( mail.getAttachments().size() > 0 ) {
            sender.sendMessage(Messages.get("ErrorItemAttachedYet"));
            return true;
        }

        // キャンセル
        manager.clearEditmodeMail(MailSender.getMailSender(sender));

        if ( sender instanceof Player ) {
            parent.getBoxManager().clearEditmodeBox((Player)sender);
        }

        sender.sendMessage(Messages.get("InformationEditCancelled"));

        return true;
    }

    private boolean doItemCommand(CommandSender sender, Command command, String label, String[] args) {

        // パーミッション確認
        if  ( !sender.hasPermission(PERMISSION + ".item") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // このコマンドは、ゲーム内からの実行でない場合はエラーを表示して終了する
        if ( !(sender instanceof Player) ) {
            sender.sendMessage(Messages.get("ErrorInGameCommand"));
            return true;
        }

        ItemStack hand = ((Player)sender).getItemInHand();
        if ( hand == null ) return true;

        // 情報表示
        String description = getItemDesc(hand);
        String isTradable = TradableMaterial.isTradable(hand.getType()) ? "yes" : "no";
        sender.sendMessage(Messages.get("InformationItemDetail",
                new String[]{"%desc", "%tradable"},
                new String[]{description, isTradable}));

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

    /**
     * アイテム表記から、ItemStackを作成して返す
     * @param desc アイテム表記
     * （マテリアル名、または、アイテムID。コロンを付けた後にデータ値を指定することも可能。
     * 　例：WOOL, WOOL:3, 35, 35:6 ）
     * @return ItemStack
     */
    private ItemStack getItemStackFromDescription(String desc) {
        String[] descs = desc.split(":");
        if ( descs.length <= 0 ) return null;
        Material material = Material.getMaterial(descs[0]);
        if ( material == null && descs[0].matches("[0-9]{1,5}") ) {
            @SuppressWarnings("deprecation")
            Material m = Material.getMaterial(Integer.parseInt(descs[0]));
            material = m;
        }
        if ( material == null ) return null;
        ItemStack item = new ItemStack(material);
        if ( descs.length >= 2 && descs[1].matches("[0-9]{1,5}") ) {
            short durability = Short.parseShort(descs[1]);
            item.setDurability(durability);
        }
        return item;
    }

    /**
     * 指定したプレイヤーが指定したアイテムを十分な個数持っているかどうか確認する
     * @param player プレイヤー
     * @param item アイテム
     * @return 持っているかどうか
     */
    private boolean hasItem(Player player, ItemStack item) {
        //return player.getInventory().contains(item.getType(), item.getAmount());
        // ↑のコードは、アイテムのデータ値を検査しないのでNG

        int total = 0;
        for ( ItemStack i : player.getInventory().getContents() ) {
            if ( i != null && i.getType() == item.getType()
                    && i.getDurability() == item.getDurability() ) {
                total += i.getAmount();
                if ( total >= item.getAmount() ) return true;
            }
        }
        return false;
    }

    /**
     * 指定したプレイヤーから指定したアイテムを回収する
     * @param player プレイヤー
     * @param item アイテム
     * @return 回収に成功したかどうか
     */
    @SuppressWarnings("deprecation")
    private boolean consumeItem(Player player, ItemStack item) {
        Inventory inv = player.getInventory();
        int remain = item.getAmount();
        for ( int index=0; index<inv.getSize(); index++ ) {
            ItemStack i = inv.getItem(index);
            if ( i == null || i.getType() != item.getType()
                    || i.getDurability() != item.getDurability() ) {
                continue;
            }

            if ( i.getAmount() >= remain ) {
                if ( i.getAmount() == remain ) {
                    inv.clear(index);
                } else {
                    i.setAmount(i.getAmount() - remain);
                    inv.setItem(index, i);
                }
                remain = 0;
                break;
            } else {
                remain -= i.getAmount();
                inv.clear(index);
            }
        }
        player.updateInventory();
        return (remain <= 0);
    }

    /**
     * 宛先として有効な全てのプレイヤー名を取得する
     * @return 有効な宛先
     */
    private ArrayList<OfflinePlayer> getAllValidPlayers() {
        ArrayList<OfflinePlayer> players = new ArrayList<OfflinePlayer>();
        for ( OfflinePlayer player : Bukkit.getOfflinePlayers() ) {
            if ( player.hasPlayedBefore() || player.isOnline() ) {
                players.add(player);
            }
        }
        return players;
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

        msg.send(ms);
    }

    /**
     * アイテムを簡単な文字列表現にして返す
     * @param item アイテム
     * @return 文字列表現
     */
    private String getItemDesc(ItemStack item) {
        return item.getDurability() == 0 ? item.getType().toString() :
                item.getType().toString() + ":" + item.getDurability();
    }

    /**
     * 正の小数付き数値を返す。Doubleにパース不可の場合は、-1が返される。
     * @param value 変換対象
     * @return 変換後の値。doubleでない場合は-1が返される
     */
    private double tryParseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch(NumberFormatException e) {
            return -1;
        }
    }
}
