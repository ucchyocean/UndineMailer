/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.command;

import java.util.List;

import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.item.TradableMaterial;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * undine item コマンド
 * @author ucchy
 */
public class UndineItemCommand implements SubCommand {

    private static final String NAME = "item";
    private static final String NODE = "undine." + NAME;

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

        // このコマンドは、ゲーム内からの実行でない場合はエラーを表示して終了する
        if ( !(sender instanceof Player) ) {
            sender.sendMessage(Messages.get("ErrorInGameCommand"));
            return;
        }

        ItemStack hand = ((Player)sender).getItemInHand();
        if ( hand == null ) return;

        // 情報表示
        String description = UndineCommandUtil.getItemDesc(hand);
        String isTradable = TradableMaterial.isTradable(hand.getType()) ? "yes" : "no";
        sender.sendMessage(Messages.get("InformationItemDetail",
                new String[]{"%desc", "%tradable"},
                new String[]{description, isTradable}));
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
