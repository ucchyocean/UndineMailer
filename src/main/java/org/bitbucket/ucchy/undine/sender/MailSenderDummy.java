/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine.sender;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * ダミーのMailSender
 * @author ucchy
 */
public class MailSenderDummy extends MailSender {

    private String name;

    public MailSenderDummy(String name) {
        this.name = name;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#isOnline()
     */
    @Override
    public boolean isOnline() {
        return false;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#isValidDestination()
     */
    @Override
    public boolean isValidDestination() {
        return false;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getDisplayName()
     */
    @Override
    public String getDisplayName() {
        return name;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#sendMessage(java.lang.String)
     */
    @Override
    public void sendMessage(String message) {
        // do nothing.
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getOfflinePlayer()
     */
    @Override
    public OfflinePlayer getOfflinePlayer() {
        return null;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getPlayer()
     */
    @Override
    public Player getPlayer() {
        return null;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getWorldName()
     */
    @Override
    public String getWorldName() {
        return null;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#hasPermission(java.lang.String)
     */
    @Override
    public boolean hasPermission(String node) {
        return false;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#isOp()
     */
    @Override
    public boolean isOp() {
        return false;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#setStringMetadata(java.lang.String, java.lang.String)
     */
    @Override
    public void setStringMetadata(String key, String value) {
        // do nothing.
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getStringMetadata(java.lang.String)
     */
    @Override
    public String getStringMetadata(String key) {
        return null;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#setBooleanMetadata(java.lang.String, boolean)
     */
    @Override
    public void setBooleanMetadata(String key, boolean value) {
        // do nothing.
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#getBooleanMetadata(java.lang.String)
     */
    @Override
    public boolean getBooleanMetadata(String key) {
        return false;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#equals(org.bukkit.command.CommandSender)
     */
    @Override
    public boolean equals(CommandSender sender) {
        return false;
    }

    /**
     * @see org.bitbucket.ucchy.undine.sender.MailSender#toString()
     */
    @Override
    public String toString() {
        return name;
    }
}
