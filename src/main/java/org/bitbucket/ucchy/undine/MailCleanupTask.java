/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * 古いメールを削除するタスク
 * @author ucchy
 */
public class MailCleanupTask extends BukkitRunnable {

    private MailManager manager;

    public MailCleanupTask(MailManager manager) {
        this.manager = manager;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        manager.cleanup();
    }

    protected void startTask() {
        runTaskTimerAsynchronously(
                UndineMailer.getInstance(), 0, 20 * 60 * 60);
    }
}
