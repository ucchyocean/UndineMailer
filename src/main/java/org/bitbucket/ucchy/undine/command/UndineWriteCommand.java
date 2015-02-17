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
import org.bitbucket.ucchy.undine.UndineConfig;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bitbucket.ucchy.undine.group.GroupData;
import org.bitbucket.ucchy.undine.group.GroupManager;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.command.CommandSender;

/**
 * undine write コマンド
 * @author ucchy
 */
public class UndineWriteCommand implements SubCommand {

    private static final String NAME = "write";
    private static final String NODE = "undine." + NAME;

    private MailManager manager;
    private GroupManager groupManager;
    private UndineConfig config;

    /**
     * コンストラクタ
     * @param parent
     */
    public UndineWriteCommand(UndineMailer parent) {
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

        MailSender ms = MailSender.getMailSender(sender);

        // 引数に何かあるなら、宛先として設定する
        if ( args.length >= 2 ) {

            MailData mail = manager.makeEditmodeMail(ms);

            ArrayList<String> dests = new ArrayList<String>();
            for ( int i=1; i<args.length; i++ ) {
                dests.add(args[i]);
            }

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
                        }
                    }
                } else if ( !config.isEnableSendSelf() && target.equals(sender) ) {
                    // 自分自身が指定不可の設定の場合は、自分自身が指定されたら無視する
                } else if ( !targets.contains(target) ) {
                    targets.add(target);
                }
            }

            for ( MailSender target : targets ) {
                if ( !mail.getTo().contains(target) ) {
                    mail.setTo(mail.getTo().size(), target);
                }
            }
            for ( GroupData g : targetGroups ) {
                if ( !mail.getToGroups().contains(g.getName()) ) {
                    mail.setToGroup(mail.getToGroups().size(), g.getName());
                }
            }
        }

        // 編集画面を表示する。
        manager.displayEditmode(ms);
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
