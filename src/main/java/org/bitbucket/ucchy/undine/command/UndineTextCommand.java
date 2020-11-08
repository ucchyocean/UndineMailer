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
import org.bitbucket.ucchy.undine.group.GroupManager;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

/**
 * undine text コマンド
 * @author ucchy
 */
public class UndineTextCommand implements SubCommand {

    private static final String NAME = "text";
    private static final String NODE = "undine." + NAME;

    private MailManager manager;
    private GroupManager groupManager;
    private UndineConfig config;
    private UndineMailer parent;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineTextCommand(UndineMailer parent) {
        this.parent = parent;
        this.manager = parent.getMailManager();
        this.groupManager = parent.getGroupManager();
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

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Destination"));
            return;
        }

        if ( args.length < 3 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Message"));
            return;
        }

        // スパム保護に引っかかるなら、エラーを表示して終了
        MailSender ms = MailSender.getMailSender(sender);
        long gap = getGapWithSpamProtectionMilliSeconds(ms);
        if ( !(ms instanceof MailSenderConsole) && gap > 0 ) {
            int remain = (int)(gap / 1000) + 1;
            sender.sendMessage(Messages.get("ErrorCannotSendSpamMail", "%remain", remain));
            return;
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
        ArrayList<GroupData> targetGroups = new ArrayList<GroupData>();

        for ( String d : dests ) {

            MailSender target = MailSender.getMailSenderFromString(d);
            if ( target == null || !target.isValidDestination() ) {
                // 宛先が見つからない場合は、グループを確認
                GroupData group = groupManager.getGroup(d);
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
            return;
        }

        // メール生成
        MailData mail = manager.makeEditmodeMail(ms);
        mail.addTo(targets);
        mail.addMessage(message.toString());
        for ( GroupData g : targetGroups ) {
            mail.setToGroup(mail.getToGroups().size(), g.getName());
        }

        // 送信にお金がかかる場合
        double fee = getSendFee(mail);
        if ( (ms instanceof MailSenderPlayer) && fee > 0 ) {

            VaultEcoBridge eco = parent.getVaultEco();
            String feeDisplay = eco.format(fee);

            // 残金が足りないならエラーで終了
            if ( !eco.has(ms.getPlayer(), fee) ) {
                sender.sendMessage(Messages.get("ErrorYouDontHaveEnoughMoney"));
                return;
            }

            // 引き落としの実行、ただし、エラーが返されたらエラーで終了
            if ( !eco.withdrawPlayer(ms.getPlayer(), fee) ) {
                sender.sendMessage(Messages.get("ErrorFailToWithdraw"));
                return;
            }

            // 引き落としたことを通知
            double balance = parent.getVaultEco().getBalance(ms.getPlayer());
            sender.sendMessage(Messages.get("EditmodeFeeResult",
                    new String[]{"%fee", "%remain"},
                    new String[]{feeDisplay, eco.format(balance)}));
        }

        // メールを送信する
        manager.sendNewMail(mail);

        // 送信したことを送信元に知らせる
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

        if ( args.length == 2 ) {
            // textコマンドの2つ目と、toコマンドの3つ目は、有効な宛先で補完する

            // プレイヤー名簿が利用不可の場合は、nullを返す
            //（オンラインプレイヤー名で補完される）
            if ( !parent.getUndineConfig().isEnablePlayerList() ) {
                return null;
            }

            // オフラインプレイヤー名で補完する
            String arg = args[1].toLowerCase();
            ArrayList<String> candidates = new ArrayList<String>();
            for ( String name : parent.getPlayerNames() ) {
                if ( name.toLowerCase().startsWith(arg)
                        && !name.equals(sender.getName())) {
                    candidates.add(name);
                }
            }
            return candidates;
        }

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
        double total = 0;
        total += mail.getTo().size() * config.getSendFee();
        if ( config.isAttachFeePerAmount() ) {
            total += getItemAmount(mail.getAttachments()) * config.getAttachFee();
        } else {
            total += mail.getAttachments().size() * config.getAttachFee();
        }
        return total;
    }

    /**
     * 次のメールを送信するまでに必要なミリ秒を返す
     * @param sender 送信者
     * @return ミリ秒
     */
    private long getGapWithSpamProtectionMilliSeconds(MailSender sender) {
        String value = sender.getStringMetadata(MailManager.SENDTIME_METAKEY);
        if ( value == null ) return -1;
        long prev = Long.parseLong(value);
        long next = prev + config.getMailSpamProtectionSeconds() * 1000;
        return next - System.currentTimeMillis();
    }

    /**
     * アイテムの個数総数を返す
     * @param items カウントする対象
     * @return 個数総数
     */
    private int getItemAmount(List<ItemStack> items) {
        int total = 0;
        for ( ItemStack item : items ) {
            if ( item != null ) {
                total += item.getAmount();
            }
        }
        return total;
    }
}
