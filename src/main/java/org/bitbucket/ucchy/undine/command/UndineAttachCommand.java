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
import org.bitbucket.ucchy.undine.item.TradableMaterial;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * undine attach コマンド
 * @author ucchy
 */
public class UndineAttachCommand implements SubCommand {

    private static final String NAME = "attach";
    private static final String NODE = "undine." + NAME;
    private static final String NODE_COMMAND_ATTACH = "undine.command-attach";
    private static final String COMMAND = UndineCommand.COMMAND;

    private UndineMailer parent;
    private MailManager manager;
    private UndineConfig config;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineAttachCommand(UndineMailer parent) {
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

        MailSender ms = MailSender.getMailSender(sender);

        if ( args.length == 1 ) {
            // 編集メールの添付ボックスを開く。

            // ゲーム内からの実行でない場合はエラーを表示して終了する
            if ( !(sender instanceof Player) ) {
                sender.sendMessage(Messages.get("ErrorInGameCommand"));
                return;
            }

            Player player = (Player)sender;
            MailData mail = manager.getEditmodeMail(ms);

            // 編集中でないならエラーを表示して終了
            if ( mail == null ) {
                sender.sendMessage(Messages.get("ErrorNotInEditmode"));
                return;
            }

            // 添付ボックス利用不可のワールドにいるなら、エラーを表示して終了
            if ( config.getDisableWorldsToOpenAttachBox().contains(ms.getWorldName()) ) {
                sender.sendMessage(Messages.get("ErrorInvalidAttachBoxWorld"));
                return;
            }

            // 添付ボックスを表示する
            parent.getBoxManager().displayAttachBox(player, mail);

            return;

        } else if ( args.length >= 3 && args[1].equalsIgnoreCase("add") ) {
            // 編集メールに、コマンドでアイテムを足す。

            // パーミッション確認
            if  ( !sender.hasPermission(NODE_COMMAND_ATTACH) ) {
                sender.sendMessage(Messages.get("PermissionDeniedCommand"));
                return;
            }

            MailData mail = manager.getEditmodeMail(ms);

            // 編集中でないならエラーを表示して終了
            if ( mail == null ) {
                sender.sendMessage(Messages.get("ErrorNotInEditmode"));
                return;
            }

            // 指定値がアイテム表現形式ではない場合はエラーを表示して終了
            ItemStack item = UndineCommandUtil.getItemStackFromDescription(args[2]);
            if ( item == null ) {
                sender.sendMessage(Messages.get("ErrorInvalidItem", "%item", args[2]));
                return;
            }

            // 取引可能な種類のアイテムでないなら、エラーを表示して終了
            if ( !TradableMaterial.isTradable(item.getType()) ) {
                sender.sendMessage(Messages.get("ErrorInvalidItem", "%item", args[2]));
                return;
            }

            // 個数も指定されている場合は、個数を反映する
            if ( args.length >= 4 && args[3].matches("[0-9]{1,9}") ) {
                item.setAmount(Integer.parseInt(args[3]));
            }

            // 添付ボックスに投入する
            mail.getAttachments().add(item);

            // 編集画面を表示する。
            manager.displayEditmode(MailSender.getMailSender(sender));

            return;

        } else if ( args.length >= 2 && args[1].equalsIgnoreCase("clear") ) {
            // 編集メールのアイテムを、コマンドで全て除去する。

            // パーミッション確認
            if  ( !sender.hasPermission(NODE_COMMAND_ATTACH) ) {
                sender.sendMessage(Messages.get("PermissionDeniedCommand"));
                return;
            }

            MailData mail = manager.getEditmodeMail(ms);

            // 編集中でないならエラーを表示して終了
            if ( mail == null ) {
                sender.sendMessage(Messages.get("ErrorNotInEditmode"));
                return;
            }

            // 添付ボックスのクリアする
            mail.getAttachments().clear();

            // 編集画面を表示する。
            manager.displayEditmode(MailSender.getMailSender(sender));

            return;

        } else if ( args[1].matches("[0-9]{1,9}") ) {
            // 指定されたインデクスのメールの添付ボックスを開く

            // ゲーム内からの実行でない場合はエラーを表示して終了する
            if ( !(sender instanceof Player) ) {
                sender.sendMessage(Messages.get("ErrorInGameCommand"));
                return;
            }

            Player player = (Player)sender;
            int index = Integer.parseInt(args[1]);
            MailData mail = manager.getMail(index);

            // 該当メールが存在しない場合、エラーを表示して終了
            if ( mail == null ) {
                sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[1]));
                return;
            }

            runCommandForSentMail(player, ms, args, mail);
        }
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
     * 既に送信したメールに対するattachコマンドの実行
     * @param player
     * @param ms
     * @param args
     * @param mail
     */
    public void runCommandForSentMail(Player player, MailSender ms,
            String[] args, MailData mail) {

        if ( args.length >= 3 && args[2].equalsIgnoreCase("cancel") ) {
            // 3番目のパラメータがcancelなら、添付のキャンセル処理

            // 既にキャンセル済みならエラーを表示して終了
            if ( mail.isAttachmentsCancelled() ) {
                player.sendMessage(Messages.get("ErrorAlreadyAttachCancelled"));
                return;
            }

            // 送信者ではないならエラーを表示して終了
            if ( !mail.getFrom().equals(ms) ) {
                player.sendMessage(Messages.get("ErrorNoneCancelAttachPermission"));
                return;
            }

            // 既に受信者がボックスを開いてしまったなら、エラーを表示して終了
            if ( mail.isAttachmentsOpened() ) {
                player.sendMessage(Messages.get("ErrorAlreadyRecipientOpened"));
                return;
            }

            // 添付ボックス利用不可のワールドにいるなら、エラーを表示して終了
            if ( config.getDisableWorldsToOpenAttachBox().contains(ms.getWorldName()) ) {
                player.sendMessage(Messages.get("ErrorInvalidAttachBoxWorld"));
                return;
            }

            // 添付をキャンセルする
            mail.cancelAttachments();
            manager.saveMail(mail);

            // 添付ボックスを表示する
            parent.getBoxManager().displayAttachBox(player, mail);

            // 受信者側にメッセージを表示する
            String message = Messages.get(
                    "InformationAttachWasCanceledBySender",
                    new String[]{"%num", "%sender"},
                    new String[]{mail.getIndex() + "", player.getName()});
            for ( MailSender to : mail.getToTotal() ) {
                if ( to.isOnline() ) {
                    to.sendMessage(message);
                }
            }

            return;

        } else if ( args.length >= 3 && args[2].equalsIgnoreCase("refuse") ) {
            // 3番目のパラメータがrefuseなら、添付の受取拒否の処理

            // 既にキャンセル済みならエラーを表示して終了
            if ( mail.isAttachmentsCancelled() ) {
                player.sendMessage(Messages.get("ErrorAlreadyAttachCancelled"));
                return;
            }

            // 受信者ではないならエラーを表示して終了
            if ( !mail.getToTotal().contains(ms) ) {
                player.sendMessage(Messages.get("ErrorNoneRefuseAttachPermission"));
                return;
            }

            // 既に受信者がボックスを開いてしまったなら、エラーを表示して終了
            if ( mail.isAttachmentsOpened() ) {
                player.sendMessage(Messages.get("ErrorAlreadyRecipientOpened"));
                return;
            }

            // 拒否理由を取得する
            StringBuffer reason = new StringBuffer();
            for ( int i=3; i<args.length; i++ ) {
                reason.append(args[i] + " ");
            }

            // 添付のコピーを取る
            ArrayList<ItemStack> attachments =
                    new ArrayList<ItemStack>(mail.getAttachments());

            // 添付を拒否し、添付をクリアして、メールを保存する。
            mail.refuseAttachments(reason.toString().trim());
            mail.getAttachments().clear();
            manager.saveMail(mail);

            // 送信者側に新規メールで、アイテムを差し戻す
            MailData reply = new MailData();
            reply.setTo(0, mail.getFrom());
            reply.setFrom(MailSenderConsole.getMailSenderConsole());
            reply.addMessage(Messages.get(
                    "BoxRefuseSenderResult",
                    new String[]{"%to", "%num"},
                    new String[]{ms.getName(), mail.getIndex() + ""}));
            if ( reason.toString().trim().length() > 0 ) {
                reply.addMessage(Messages.get("BoxRefuseSenderResultReason")
                        + reason.toString().trim());
            }
            reply.setAttachments(attachments);
            parent.getMailManager().sendNewMail(reply);

            // 受信者側に、拒否した該当メールの詳細画面を開く
            manager.displayMailDescription(ms, mail);

            return;
        }

        // 以下、添付ボックスのオープン処理

        // 開く権限が無い場合はエラーを表示して終了
        if ( mail.isRecipient(ms) ) {
            if ( mail.isAttachmentsCancelled() ) {
                player.sendMessage(Messages.get("ErrorAlreadyAttachCancelled"));
                return;
            }
        } else if ( mail.getFrom().equals(ms) ) {
            if ( !mail.isAttachmentsCancelled() ) {
                player.sendMessage(Messages.get("ErrorNoneReadPermission"));
                return;
            }
        }

        // 添付ボックス利用不可のワールドにいるなら、エラーを表示して終了
        if ( config.getDisableWorldsToOpenAttachBox().contains(ms.getWorldName()) ) {
            player.sendMessage(Messages.get("ErrorInvalidAttachBoxWorld"));
            return;
        }

        if ( mail.getCostMoney() > 0 ) {
            // 着払い料金が設定がされている場合
            if ( !checkForCostMoney(player, ms, args, mail) ) {
                return;
            }

        } else if ( mail.getCostItem() != null ) {
            // 着払いアイテムが設定されている場合
            if ( !checkForCostItem(player, ms, args, mail) ) {
                return;
            }
        }

        // 添付ボックスを表示する
        parent.getBoxManager().displayAttachBox(player, mail);
    }

    /**
     * 着払い料金の支払いをチェックする
     * @param player
     * @param ms
     * @param args
     * @param mail
     * @return 支払ったかどうか
     */
    private boolean checkForCostMoney(Player player, MailSender ms,
            String[] args, MailData mail) {

        VaultEcoBridge eco = parent.getVaultEco();
        double fee = mail.getCostMoney();
        String feeDisplay = eco.format(fee);

        if ( args.length >= 3 && args[2].equals("confirm") ) {

            OfflinePlayer from = mail.getFrom().getOfflinePlayer();
            double preTo = eco.getBalance(ms.getOfflinePlayer());
            double preFrom = eco.getBalance(from);

            // 引き落とし
            if ( !eco.has(ms.getOfflinePlayer(), fee) ) {
                player.sendMessage(Messages.get("ErrorYouDontHaveEnoughMoney"));
                return false;
            }
            if ( !eco.withdrawPlayer(ms.getOfflinePlayer(), fee) ) {
                player.sendMessage(Messages.get("ErrorFailToWithdraw"));
                return false;
            }

            // メールの送信元に送金
            boolean depositResult = eco.depositPlayer(from, fee);

            // NOTE: EssentialsEcoでは、max-money以上になる入金は失敗する
            // (しかし、困ったことに、transactionSuccess が返される) ので、
            // 入金後が (pre + fee) > now なら、入金失敗として
            // 取り引きをキャンセルさせる。
            if ( !depositResult || ( config.getDepositErrorOnUnmatch()
                    && ((preFrom + fee) > eco.getBalance(from)) ) ) {
                // 返金
                eco.setPlayer(ms.getOfflinePlayer(), preTo);
                eco.setPlayer(from, preFrom);
                player.sendMessage(Messages.get("ErrorFailToDeposit"));
                return false;
            }

            // 成功メッセージを両者に表示する
            double balance = eco.getBalance(ms.getPlayer());
            player.sendMessage(Messages.get("BoxOpenCostMoneyResult",
                    new String[]{"%fee", "%remain"},
                    new String[]{feeDisplay, eco.format(balance)}));

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
            return true;
        }

        // かかる金額を表示
        player.sendMessage(Messages.get(
                "BoxOpenCostMoneyInformation", "%fee", feeDisplay));

        if ( !eco.has(ms.getPlayer(), fee) ) {
            // 残金が足りない
            player.sendMessage(Messages.get("ErrorYouDontHaveEnoughMoney"));
            return false;
        }

        // 確認メッセージを表示
        int index = mail.getIndex();
        player.sendMessage(Messages.get("BoxOpenCostConfirm"));
        UndineCommandUtil.showOKCancelButton(ms,
                COMMAND + " attach " + index + " confirm",
                COMMAND + " read " + index);

        return false;
    }

    /**
     * 着払いアイテムの支払いをチェックする
     * @param player
     * @param ms
     * @param args
     * @param mail
     * @return 支払ったかどうか
     */
    private boolean checkForCostItem(Player player, MailSender ms,
            String[] args, MailData mail) {

        ItemStack fee = mail.getCostItem();

        if ( args.length >= 3 && args[2].equals("confirm") ) {

            // 引き落とし
            if ( !hasItem(player, fee) ) {
                player.sendMessage(Messages.get("ErrorYouDontHaveEnoughItem"));
                return false;
            }
            consumeItem(player, fee);
            player.sendMessage(Messages.get("BoxOpenCostItemResult",
                    new String[]{"%material", "%amount"},
                    new String[]{fee.getType().toString(), fee.getAmount() + ""}));

            // メールの送信元に送金
            MailData reply = new MailData();
            reply.setTo(0, mail.getFrom());
            reply.setFrom(ms);
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
            return true;
        }

        // かかる金額を表示
        player.sendMessage(Messages.get(
                "BoxOpenCostItemInformation",
                new String[]{"%material", "%amount"},
                new String[]{fee.getType().toString(), fee.getAmount() + ""}));

        if ( !hasItem(player, fee) ) {
            // 残金が足りない
            player.sendMessage(Messages.get("ErrorYouDontHaveEnoughItem"));
            return false;
        }

        // 確認メッセージを表示
        int index = mail.getIndex();
        player.sendMessage(Messages.get("BoxOpenCostConfirm"));
        UndineCommandUtil.showOKCancelButton(ms,
                COMMAND + " attach " + index + " confirm",
                COMMAND + " read " + index);

        return false;
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
}