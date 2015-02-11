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
import org.bitbucket.ucchy.undine.item.TradableMaterial;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

/**
 * undine costitem コマンド
 * @author ucchy
 */
public class UndineCostItemCommand implements SubCommand {

    private static final String NAME = "costitem";
    private static final String NODE = "undine." + NAME;

    private MailManager manager;
    private UndineConfig config;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineCostItemCommand(UndineMailer parent) {
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
     * @param args 実行時の引数
     * @see org.bitbucket.ucchy.undine.command.SubCommand#runCommand(org.bukkit.command.CommandSender, java.lang.String[])
     */
    @Override
    public void runCommand(CommandSender sender, String[] args) {

        // 着払いアイテムが使用不可の設定になっているなら、エラーを表示して終了
        if ( !config.isEnableCODItem() ) {
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
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "Item"));
            return;
        }

        // 2番めのパラメータがremoveだったら、着払いアイテムを解除する
        if ( args[1].equalsIgnoreCase("remove") ) {
            mail.setCostItem(null);

            // 編集画面を表示して終了する
            manager.displayEditmode(MailSender.getMailSender(sender));
            return;
        }

        // 指定値がアイテム表現形式ではない場合はエラーを表示して終了
        ItemStack item = UndineCommandUtil.getItemStackFromDescription(args[1]);
        if ( item == null ) {
            sender.sendMessage(Messages.get("ErrorInvalidCostItem", "%item", args[1]));
            return;
        }

        // 取引可能な種類のアイテムでないなら、エラーを表示して終了
        if ( !TradableMaterial.isTradable(item.getType()) ) {
            sender.sendMessage(Messages.get("ErrorInvalidCostItem", "%item", args[1]));
            return;
        }

        // アイテムと料金を両方設定しようとしたら、エラーを表示して終了
        if ( mail.getCostMoney() > 0 ) {
            sender.sendMessage(Messages.get("ErrorCannotSetMoneyAndItem"));
            return;
        }

        // 個数も指定されている場合は、個数を反映する
        if ( args.length >= 3 && args[2].matches("[0-9]{1,9}") ) {
            item.setAmount(Integer.parseInt(args[2]));
        }

        // 設定する
        mail.setCostItem(item);

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

        if ( ( args.length == 2 ) ) {
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
}
