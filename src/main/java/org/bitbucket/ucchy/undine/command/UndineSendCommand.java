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
import org.bitbucket.ucchy.undine.bridge.VaultEcoBridge;
import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

/**
 * undine send コマンド
 * @author ucchy
 */
public class UndineSendCommand implements SubCommand {

    private static final String NAME = "send";
    private static final String NODE = "undine." + NAME;
    private static final String NODE_ATTACH_INFINITY = "undine.attach-infinity";
    private static final String NODE_MULTIPLE_ATTACH = "undine.multiple-attach";
    private static final String COMMAND = UndineCommand.COMMAND;

    private UndineMailer parent;
    private MailManager manager;
    private UndineConfig config;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineSendCommand(UndineMailer parent) {
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

        // MailManagerのロードが完了していないなら、エラーを表示して終了
        if ( !manager.isLoaded() ) {
            sender.sendMessage(Messages.get("ErrorCannotSendInitializingYet"));
            return;
        }

        MailSender ms = MailSender.getMailSender(sender);
        MailData mail = manager.getEditmodeMail(ms);

        // 編集中でないならエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorNotInEditmode"));
            return;
        }

        // 宛先が設定されていないならエラーを表示して終了
        if ( (mail.getTo().size() + mail.getToGroups().size()) == 0 ) {
            sender.sendMessage(Messages.get("ErrorEmptyTo"));
            return;
        }

        // 添付ボックスの使用制限を超える場合は、エラーを表示して終了
        if ( mail.getAttachments().size() > 0
                && !sender.hasPermission(NODE_ATTACH_INFINITY)
                && manager.getAttachBoxUsageCount(ms) >= config.getMaxAttachmentBoxCount() ) {
            sender.sendMessage(Messages.get("ErrorAttachBoxCountExceed",
                    new String[]{"%num", "%limit"},
                    new String[]{manager.getAttachBoxUsageCount(ms) + "", config.getMaxAttachmentBoxCount() + ""}));
            return;
        }

        // 複数の宛先に、添付付きメールを送信しようとしたときの処理
        if ( mail.getAttachments().size() > 0
                && (mail.getTo().size() > 1 || mail.getToGroups().size() > 0) ) {

            // undine.multiple-attach 権限が無いプレイヤーが
            // 添付ファイル付きのメールを複数の宛先に出そうとしたなら、エラーを表示して終了。
            if ( !sender.hasPermission(NODE_MULTIPLE_ATTACH) ) {
                sender.sendMessage(Messages.get("ErrorCannotSendMultiAttach"));
                return;
            }

            // All宛てのメールで添付付きは、権限があっても送信できないので、エラーを表示する
            if ( mail.isAllMail() ) {
                sender.sendMessage(Messages.get("ErrorCannotSendAttachMailToAll"));
                return;
            }

            // 宛先を調べる
            ArrayList<MailSender> to_total = new ArrayList<MailSender>();
            for ( MailSender t : mail.getTo() ) {
                if ( !to_total.contains(t) ) {
                    to_total.add(t);
                }
            }
            for ( GroupData group : mail.getToGroupsConv() ) {
                for ( MailSender t : group.getMembers() ) {
                    if ( !to_total.contains(t) ) {
                        to_total.add(t);
                    }
                }
            }

            if ( args.length >= 2 && args[1].equals("attachconfirm") ) {
                // 複製して送信

                for ( MailSender t : to_total ) {
                    MailData copy = mail.clone();
                    copy.deleteAllTo();
                    copy.setTo(0, t);
                    manager.sendNewMail(copy);
                }

                manager.clearEditmodeMail(ms);
                if ( sender instanceof Player ) {
                    parent.getBoxManager().clearEditmodeBox((Player)sender);
                }

                // 送信したことを送信元に知らせる
                for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
                    mail.getFrom().sendMessage("");
                }
                mail.getFrom().sendMessage(Messages.get("InformationYouSentMail"));

                return;
            }

            // メールが複製されることについて確認する
            sender.sendMessage(
                    Messages.get("InformationMultiAttachConfirm",
                            "%num", to_total.size()));

            if ( sender instanceof ConsoleCommandSender ) {
                // コンソールの場合は、OK Cancel ボタンの代わりに、コマンドガイドを出す。
                // see issue #17.
                sender.sendMessage(Messages.get("InformationMultiAttachConfirmConsole"));
            } else {
                UndineCommandUtil.showOKCancelButton(ms,
                        COMMAND + " send attachconfirm",
                        COMMAND + " write");
            }

            return;
        }

        // 送信にお金がかかる場合
        double fee = getSendFee(mail);
        if ( (ms instanceof MailSenderPlayer) && fee > 0 ) {

            VaultEcoBridge eco = parent.getVaultEco();
            String feeDisplay = eco.format(fee);

            if ( args.length > 1 && args[1].equals("confirm") ) {
                // 課金して送信

                if ( !eco.has(ms.getPlayer(), fee) ) {
                    sender.sendMessage(Messages.get("ErrorYouDontHaveEnoughMoney"));
                    return;
                }
                if ( !eco.withdrawPlayer(ms.getPlayer(), fee) ) {
                    sender.sendMessage(Messages.get("ErrorFailToWithdraw"));
                    return;
                }
                double balance = eco.getBalance(ms.getPlayer());
                sender.sendMessage(Messages.get("EditmodeFeeResult",
                        new String[]{"%fee", "%remain"},
                        new String[]{feeDisplay, eco.format(balance)}));

                manager.sendNewMail(mail);
                manager.clearEditmodeMail(ms);
                if ( sender instanceof Player ) {
                    parent.getBoxManager().clearEditmodeBox((Player)sender);
                }

                // 送信したことを送信元に知らせる
                for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
                    mail.getFrom().sendMessage("");
                }
                mail.getFrom().sendMessage(Messages.get("InformationYouSentMail"));

                return;
            }

            // かかる金額を表示
            sender.sendMessage(Messages.get(
                    "EditmodeFeeInformation", "%fee", feeDisplay));
            String sendfee = eco.format(mail.getTo().size() * config.getSendFee());
            String itemfee = eco.format(mail.getAttachments().size() * config.getAttachFee());
            String codfee = eco.format(0);
            boolean needCodFee = false;
            if ( mail.getCostMoney() > 0 ) {
                codfee = eco.format(mail.getCostMoney() * config.getCodMoneyTax() / 100);
                needCodFee = true;
            } else if ( mail.getCostItem() != null ) {
                codfee = eco.format(mail.getCostItem().getAmount() * config.getCodItemTax());
                needCodFee = true;
            }

            if ( !needCodFee ) {
                sender.sendMessage(Messages.get("EditmodeFeeDetail",
                        new String[]{"%mail", "%item"},
                        new String[]{sendfee, itemfee}));
            } else {
                sender.sendMessage(Messages.get("EditmodeFeeDetailWithCODTax",
                        new String[]{"%mail", "%item", "%cod"},
                        new String[]{sendfee, itemfee, codfee}));
            }

            if ( !eco.has(ms.getPlayer(), fee) ) {
                // 残金が足りない
                sender.sendMessage(Messages.get("ErrorYouDontHaveEnoughMoney"));
                return;
            }

            // 確認メッセージを表示
            sender.sendMessage(Messages.get("EditmodeFeeConfirm"));
            UndineCommandUtil.showOKCancelButton(ms,
                    COMMAND + " send confirm",
                    COMMAND + " write");

            return;
        }

        // 送信
        manager.sendNewMail(mail);
        manager.clearEditmodeMail(ms);
        if ( sender instanceof Player ) {
            parent.getBoxManager().clearEditmodeBox((Player)sender);
        }

        // 送信したことを送信元に知らせる
        for ( int i=0; i<parent.getUndineConfig().getUiEmptyLines(); i++ ) {
            mail.getFrom().sendMessage("");
        }
        mail.getFrom().sendMessage(Messages.get("InformationYouSentMail"));
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
        return null;
    }

    /**
     * 指定されたメールを送信するのにかかる金額を返す
     * @param mail メール
     * @return 送信にかかる金額
     */
    private double getSendFee(MailData mail) {
        if ( parent.getVaultEco() == null ) return 0;
        UndineConfig config = parent.getUndineConfig();
        if ( !config.isEnableSendFee() ) return 0;

        // 添付がないなら、着払い設定をクリアする。
        if ( mail.getAttachments().size() == 0 ) {
            mail.setCostMoney(0);
            mail.setCostItem(null);
        }

        double total = 0;
        total += mail.getTo().size() * config.getSendFee();
        total += mail.getAttachments().size() * config.getAttachFee();
        if ( mail.getCostMoney() > 0 ) {
            total += (mail.getCostMoney() * config.getCodMoneyTax() / 100);
        } else if ( mail.getCostItem() != null ) {
            total += (mail.getCostItem().getAmount() * config.getCodItemTax());
        }
        return total;
    }
}
