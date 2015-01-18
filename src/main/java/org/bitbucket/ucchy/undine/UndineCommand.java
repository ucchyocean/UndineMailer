/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.util.ArrayList;
import java.util.List;

import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bitbucket.ucchy.undine.tellraw.ClickEventType;
import org.bitbucket.ucchy.undine.tellraw.MessageComponent;
import org.bitbucket.ucchy.undine.tellraw.MessageParts;
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

    private static final String COMMAND = Undine.COMMAND;

    private static final String PERMISSION = "undine";

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
        } else if ( args[0].equalsIgnoreCase("costmoney") ) {
            return doCostmoneyCommand(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("costitem") ) {
            return doCostitemCommand(sender, command, label, args);
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
        if  ( !sender.hasPermission(PERMISSION + ".text") ) {
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

        // メッセージ本文
        StringBuffer message = new StringBuffer();
        for ( int i=2; i<args.length; i++ ) {
            if ( message.length() > 0 ) {
                message.append(" ");
            }
            message.append(args[i]);
        }

        // 宛先
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

        // メール生成
        MailSender ms = MailSender.getMailSender(sender);
        MailData mail = new MailData(
                targets, ms, message.toString());

        // 送信にお金がかかる場合
        int fee = manager.getSendFee(mail);
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
            int balance = parent.getVaultEco().getBalance(ms.getPlayer());
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

            if ( mail.getCostMoney() > 0 ) {
                // 着払い料金が設定がされている場合

                MailSender ms = MailSender.getMailSender(sender);
                VaultEcoBridge eco = parent.getVaultEco();
                int fee = mail.getCostMoney();
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
                    int balance = eco.getBalance(ms.getPlayer());
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

                sender.sendMessage(Messages.get("BoxOpenCostConfirm"));

                MessageComponent msg = new MessageComponent();
                msg.addText("     ");
                MessageParts buttonOK = new MessageParts(
                        Messages.get("BoxOpenCostOK"), ChatColor.AQUA);
                buttonOK.setClickEvent(
                        ClickEventType.RUN_COMMAND,
                        COMMAND + " attach " + index + " confirm");
                msg.addParts(buttonOK);
                msg.addText("     ");
                MessageParts buttonCancel = new MessageParts(
                        Messages.get("BoxOpenCostCancel"), ChatColor.AQUA);
                buttonCancel.setClickEvent(
                        ClickEventType.RUN_COMMAND, COMMAND + " read " + index);
                msg.addParts(buttonCancel);

                msg.send(ms);
                return true;

            } else if ( mail.getCostItem() != null ) {
                // 着払いアイテムが設定されている場合

                MailSender ms = MailSender.getMailSender(sender);
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

                sender.sendMessage(Messages.get("BoxOpenCostConfirm"));

                MessageComponent msg = new MessageComponent();
                msg.addText("     ");
                MessageParts buttonOK = new MessageParts(
                        Messages.get("BoxOpenCostOK"), ChatColor.AQUA);
                buttonOK.setClickEvent(
                        ClickEventType.RUN_COMMAND,
                        COMMAND + " attach " + index + " confirm");
                msg.addParts(buttonOK);
                msg.addText("     ");
                MessageParts buttonCancel = new MessageParts(
                        Messages.get("BoxOpenCostCancel"), ChatColor.AQUA);
                buttonCancel.setClickEvent(
                        ClickEventType.RUN_COMMAND, COMMAND + " read " + index);
                msg.addParts(buttonCancel);

                msg.send(ms);
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
        if ( !args[1].matches("[0-9]{1,9}") ) {
            sender.sendMessage(Messages.get("ErrorInvalidCostMoney", "%fee", args[1]));
            return true;
        }

        int amount = Integer.parseInt(args[1]);

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

        // 指定値がアイテム表現形式ではない場合はエラーを表示して終了
        ItemStack item = getItemFromDescription(args[1]);
        if ( item == null && !args[1].equals("remove") ) {
            sender.sendMessage(Messages.get("ErrorInvalidCostItem", "%item", args[1]));
            return true;
        }

        // アイテムと料金を両方設定しようとしたら、エラーを表示して終了
        if ( item != null && mail.getCostMoney() > 0 ) {
            sender.sendMessage(Messages.get("ErrorCannotSetMoneyAndItem"));
            return true;
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
        if ( mail.getTo().size() == 0 ) {
            sender.sendMessage(Messages.get("ErrorEmptyTo"));
            return true;
        }

        // 添付ファイル付きのメールを複数の宛先に出そうとしたなら、エラーを表示して終了
        if ( mail.getAttachments().size() > 0 && mail.getTo().size() > 1 ) {
            if ( !sender.hasPermission(PERMISSION + ".multi-attach") ) {
                sender.sendMessage(Messages.get("ErrorCannotSendMultiAttach"));
                return true;
            }
        }

        // 送信にお金がかかる場合
        int fee = manager.getSendFee(mail);
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
                int balance = parent.getVaultEco().getBalance(ms.getPlayer());
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

            sender.sendMessage(Messages.get("EditmodeFeeConfirm"));

            MessageComponent msg = new MessageComponent();
            msg.addText("     ");
            MessageParts buttonOK = new MessageParts(
                    Messages.get("EditmodeFeeOK"), ChatColor.AQUA);
            buttonOK.setClickEvent(ClickEventType.RUN_COMMAND, COMMAND + " send confirm");
            msg.addParts(buttonOK);
            msg.addText("     ");
            MessageParts buttonCancel = new MessageParts(
                    Messages.get("EditmodeFeeCancel"), ChatColor.AQUA);
            buttonCancel.setClickEvent(ClickEventType.RUN_COMMAND, COMMAND + " write");
            msg.addParts(buttonCancel);

            msg.send(ms);

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
     * アイテム表現形式（DIAMOND または DIAMOND:5）からItemStackを生成して返す
     * @param description アイテム表現形式
     * @return アイテム
     */
    private ItemStack getItemFromDescription(String description) {

        String mat = description;
        int amount = 1;
        if ( description.contains(":") ) {
            String[] temp = description.split(":");
            mat = temp[0];
            if ( temp.length >= 2 && temp[1].matches("[0-9]{1,9}") ) {
                amount = Integer.parseInt(temp[1]);
            }
        }

        Material material = Material.getMaterial(mat);
        if ( material == null ) {
            return null;
        }

        return new ItemStack(material, amount);
    }

    /**
     * 指定したプレイヤーが指定したアイテムを十分な個数持っているかどうか確認する
     * @param player プレイヤー
     * @param item アイテム
     * @return 持っているかどうか
     */
    private boolean hasItem(Player player, ItemStack item) {
        return player.getInventory().contains(item.getType(), item.getAmount());
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
            if ( i == null || i.getType() != item.getType() ) {
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
}
