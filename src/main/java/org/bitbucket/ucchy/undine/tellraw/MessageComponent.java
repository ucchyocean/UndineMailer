/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.tellraw;

import java.util.ArrayList;

import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * なんちゃってtellrawメッセージコンポーネント
 * @author ucchy
 */
public class MessageComponent {

    private ArrayList<MessageParts> parts;

    public MessageComponent() {
        parts = new ArrayList<MessageParts>();
    }

    public MessageComponent(ArrayList<MessageParts> parts) {
        this.parts = parts;
    }

    public void addText(String text) {
        this.parts.add(new MessageParts(text));
    }

    public void addText(String text, ChatColor color) {
        this.parts.add(new MessageParts(text, color));
    }

    public void addParts(MessageParts parts) {
        this.parts.add(parts);
    }

    private String build(String name) {
        return "tellraw "
                + name
                + " {\"text\":\"\",\"extra\":["
                + buildJoin(parts)
                + "]}";
    }

    public void send(MailSender sender) {
        if ( sender instanceof MailSenderPlayer && sender.isOnline() ) {
            sendCommand(sender.getPlayer());
        }
        sender.sendMessage(buildPlain());
    }

    private boolean sendCommand(Player player) {
        String commandLine = build(player.getName());
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
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
}
