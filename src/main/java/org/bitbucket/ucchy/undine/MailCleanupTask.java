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

    /**
     * コンストラクタ
     * @param manager MailManager
     */
    public MailCleanupTask(MailManager manager) {
        this.manager = manager;
    }

    /**
     * 定期的に実行されるメソッド
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        manager.cleanup();
    }

    /**
     * タスクを開始する。処理は、開始された瞬間に1度実行され、以降は1時間ごとに実行される。
     */
    protected void startTask() {
        runTaskTimerAsynchronously(
                UndineMailer.getInstance(), 0, 20 * 60 * 60);
    }
}
