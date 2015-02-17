/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.List;

import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.MailManager;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineConfig;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.command.CommandSender;

/**
 * undine costmoney コマンド
 * @author ucchy
 */
public class UndineCostMoneyCommand implements SubCommand {

    private static final String NAME = "costmoney";
    private static final String NODE = "undine." + NAME;

    private MailManager manager;
    private UndineConfig config;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineCostMoneyCommand(UndineMailer parent) {
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

        // 着払いアイテムが使用不可の設定になっているなら、エラーを表示して終了
        if ( !config.isEnableCODMoney() ) {
            sender.sendMessage(Messages.get("ErrorInvalidCommand"));
            return;
        }

        MailData mail = manager.getEditmodeMail(MailSender.getMailSender(sender));

        // 編集中でないならエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorNotInEditmode"));
            return;
        }

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Amount"));
            return;
        }

        // 指定値が数値ではない場合はエラーを表示して終了
        double amount = UndineCommandUtil.tryParseDouble(args[1]);
        if ( amount < 0 ) {
            sender.sendMessage(Messages.get("ErrorInvalidCostMoney", "%fee", args[1]));
            return;
        }

        // アイテムと料金を両方設定しようとしたら、エラーを表示して終了
        if ( amount > 0 && mail.getCostItem() != null ) {
            sender.sendMessage(Messages.get("ErrorCannotSetMoneyAndItem"));
            return;
        }

        // 設定する
        mail.setCostMoney(amount);

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
        return null;
    }
}
