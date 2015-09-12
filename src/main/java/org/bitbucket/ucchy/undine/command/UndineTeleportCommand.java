package org.bitbucket.ucchy.undine.command;

import java.util.List;

import org.bitbucket.ucchy.undine.MailData;
import org.bitbucket.ucchy.undine.MailManager;
import org.bitbucket.ucchy.undine.Messages;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

/**
 * undine teleport コマンド
 * @author ucchy
 */
public class UndineTeleportCommand implements SubCommand {

    private static final String NAME = "teleport";
    private static final String NODE = "undine." + NAME;

    private MailManager manager;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineTeleportCommand(UndineMailer parent) {
        this.manager = parent.getMailManager();
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

        // このコマンドは、ゲーム内からの実行でない場合はエラーを表示して終了する
        if ( !(sender instanceof Player) ) {
            sender.sendMessage(Messages.get("ErrorInGameCommand"));
            return;
        }

        Player player = (Player)sender;

        // パラメータが足りない場合はエラーを表示して終了
        if ( args.length < 2 ) {
            sender.sendMessage(Messages.get("ErrorRequireArgument", "%param", "MailIndex"));
            return;
        }

        // 指定されたパラメータが数字(正の整数)でない場合はエラーを表示して終了
        if ( !args[1].matches("[0-9]{1,9}") ) {
            sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[1]));
            return;
        }

        int index = Integer.parseInt(args[1]);
        MailData mail = manager.getMail(index);

        // メールが見つからない場合はエラーを表示して終了
        if ( mail == null ) {
            sender.sendMessage(Messages.get("ErrorInvalidIndex", "%index", args[1]));
            return;
        }

        // 送信地点が見つからない場合はエラーを表示して終了
        if ( mail.getLocation() == null ) {
            sender.sendMessage(Messages.get("ErrorCannotFoundLocation"));
            return;
        }

        // 送信地点へテレポート
        player.teleport(mail.getLocation(), TeleportCause.PLUGIN);
        player.sendMessage(Messages.get("InformationTeleported", "%index", mail.getIndex()));
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
