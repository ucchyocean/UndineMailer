/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.tellraw;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * tellrawメッセージコンポーネント
 * @author ucchy
 */
public class MessageComponent {

    private ArrayList<MessageParts> parts;

    /**
     * コンストラクタ
     */
    public MessageComponent() {
        parts = new ArrayList<MessageParts>();
    }

    /**
     * コンストラクタ
     * @param parts メッセージパーツ
     */
    public MessageComponent(ArrayList<MessageParts> parts) {
        this.parts = parts;
    }

    /**
     * テキストパーツを追加する
     * @param text テキスト
     */
    public void addText(String text) {
        this.parts.add(new MessageParts(text));
    }

    /**
     * テキストパーツを追加する
     * @param text テキスト
     * @param color テキスト色
     */
    public void addText(String text, ChatColor color) {
        this.parts.add(new MessageParts(text, color));
    }

    /**
     * テキストパーツを追加する
     * @param parts テキストパーツ
     */
    public void addParts(MessageParts parts) {
        this.parts.add(parts);
    }

    /**
     * 指定されたsenderに、このコンポーネントを送信する。
     * 相手がプレイヤーならtellrawコマンドで、コンソールならプレーンなテキストデータで送る。
     * @param sender 送信先
     */
    public void send(CommandSender sender) {
        if ( sender instanceof Player ) {
            Player player = (Player)sender;
            if ( player.isOnline() ) {
                sendCommand(player);
            } else {
                // do nothing.
            }
        } else {
            sender.sendMessage(buildPlain());
        }
    }

    /**
     * このコンポーネントが含んでいるパーツ数を返す
     * @return パーツ数
     */
    public int getPartsSize() {
        return parts.size();
    }

    /**
     * このコンポーネントが含んでいるパーツを表示したときの、文字列の文字数トータルを返す
     * @return 文字数
     */
    public int getTextLength() {
        int total = 0;
        for ( MessageParts part : parts ) {
            total += part.buildPlain().length();
        }
        return total;
    }

    /**
     * このコンポーネントをビルドして、tellrawのコマンド文字列を作成して返す
     * @param name 実行先のプレイヤー名
     * @return ビルドされたコマンド文字列
     */
    private String build(String name) {
        return String.format(
                "tellraw %s {\"text\":\"\",\"extra\":[%s]}",
                name, buildJoin(parts));
    }

    private void sendCommand(Player player) {
        final String commandLine = build(player.getName());
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
    }

    private String buildPlain() {
        StringBuffer buffer = new StringBuffer();
        for ( MessageParts part : parts ) {
            buffer.append(part.buildPlain());
        }
        return buffer.toString();
    }

    private static String buildJoin(ArrayList<MessageParts> arr) {
        StringBuffer buffer = new StringBuffer();
        for ( MessageParts s : arr ) {
            if ( buffer.length() > 0 ) {
                buffer.append(",");
            }
            buffer.append(s.build());
        }
        return buffer.toString();
    }

    /**
     * デバッグ用のエントリポイント
     * @param args
     */
    public static void main(String[] args) {

        // MessageComponentの使用例：相手に自殺ボタン付きメッセージを送る
        MessageComponent msg = new MessageComponent();
        msg.addText("自殺ボタンはこちら→");
        MessageParts button = new MessageParts("[あぼーん]", ChatColor.BLUE);
        button.setClickEvent(ClickEventType.RUN_COMMAND, "/kill");
        button.addHoverText("押しても後悔してはならない\n");
        button.addHoverText("絶対に後悔してはならない", ChatColor.GOLD);
        msg.addParts(button);
        msg.addText(" 押すなよ！絶対に押すなよ！！", ChatColor.RED);

        // 送信
        // msg.send(player);

        // デバッグ出力
        System.out.println(msg.build("ucchy"));
    }
}
