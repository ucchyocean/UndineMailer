/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.tellraw;

import java.util.ArrayList;
import java.util.HashSet;

import org.bukkit.ChatColor;

/**
 * メッセージパーツ
 * @author ucchy
 */
public class MessageParts {

    private String text;
    private ChatColor color;
    private ClickEventType ctype;
    private String cvalue;
    private HashSet<String> flags;
    private ArrayList<MessageParts> hover;

    /**
     * コンストラクタ
     * @param text テキスト
     */
    public MessageParts(String text) {
        this.text = text;
        this.flags = new HashSet<String>();
        this.hover = new ArrayList<MessageParts>();
    }

    /**
     * コンストラクタ
     * @param text テキスト
     * @param color テキスト色
     */
    public MessageParts(String text, ChatColor color) {
        this(text);
        setColor(color);
    }

    /**
     * コンストラクタ
     * @param text テキスト
     * @param color テキスト色その1
     * @param color2 テキスト色その2
     */
    public MessageParts(String text, ChatColor color, ChatColor color2) {
        this(text);
        setColor(color);
        setColor(color2);
    }

    /**
     * パーツに色を設定する
     * @param color 色。装飾系も可。
     */
    public void setColor(ChatColor color) {
        if ( color.isColor() ) {
            this.color = color;
        } else if ( color == ChatColor.BOLD ) {
            flags.add("bold");
        } else if ( color == ChatColor.ITALIC ) {
            flags.add("italic");
        } else if ( color == ChatColor.UNDERLINE ) {
            flags.add("underlined");
        } else if ( color == ChatColor.STRIKETHROUGH ) {
            flags.add("strikethrough");
        } else if ( color == ChatColor.MAGIC ) {
            flags.add("obfuscated");
        }
    }

    /**
     * パーツにクリック動作を設定する
     * @param type 動作の種類
     * @param value 設定値
     */
    public void setClickEvent(ClickEventType type, String value) {
        ctype = type;
        cvalue = value;
    }

    /**
     * ホバーテキストを追加する
     * @param text ホバーテキスト
     */
    public void addHoverText(String text) {
        this.hover.add(new MessageParts(text));
    }

    /**
     * ホバーテキストを1行追加する
     * @param text ホバーテキスト
     * @param color テキスト色
     */
    public void addHoverText(String text, ChatColor color) {
        this.hover.add(new MessageParts(text, color));
    }

    /**
     * このパーツをビルドして、tellrawのパラメータ文字列に変換する
     * @return パラメータ文字列
     */
    public String build() {

        ArrayList<String> items = new ArrayList<String>();
        items.add("\"text\":\"" + text.replace("\"", "\\\"") + "\"");
        if ( color != null ) {
            items.add("\"color\":\"" + color.name().toLowerCase() + "\"");
        }
        for ( String flag : flags ) {
            items.add("\"" + flag + "\":\"true\"");
        }
        if ( ctype != null ) {
            items.add("\"clickEvent\":"
                    + "{\"action\":\"" + ctype.toString() + "\","
                            + "\"value\":\"" + cvalue + "\"}");
        }
        if ( hover.size() > 0 ) {
            items.add("\"hoverEvent\":"
                    + "{\"action\":\"show_text\","
                            + "\"value\":{\"text\":\"\",\"extra\":["
                            + joinHover() + "]}}");
        }

        return "{" + join(items) + "}";
    }

    /**
     * このパーツを、tellrawを受け取れない人（コンソールなど）のために、
     * プレーンな文字列に変換して返す。
     * @return プレーンな文字列
     */
    public String buildPlain() {
        if ( color != null ) return color + text;
        return text;
    }

    /**
     * ホバーテキストを結合する
     * @return 結合された文字列
     */
    private String joinHover() {
        ArrayList<String> items = new ArrayList<String>();
        for ( MessageParts msg : hover ) {
            if ( msg.color == null ) {
                items.add("{\"text\":\"" + msg.text.replace("\"", "\\\"") + "\"}");
            } else {
                items.add("{\"text\":\"" + msg.text.replace("\"", "\\\"") + "\","
                        + "\"color\":\"" + msg.color.name().toLowerCase() + "\"}");
            }
        }
        return join(items);
    }

    /**
     * 文字列をコンマで結合する
     * @param arr 文字列の配列
     * @return 結合された文字列
     */
    private static String join(ArrayList<String> arr) {
        StringBuffer buffer = new StringBuffer();
        for ( String s : arr ) {
            if ( buffer.length() > 0 ) {
                buffer.append(",");
            }
            buffer.append(s);
        }
        return buffer.toString();
    }
}
