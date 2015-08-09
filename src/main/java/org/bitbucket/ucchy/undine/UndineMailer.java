/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.bitbucket.ucchy.undine.bridge.PermissionsExBridge;
import org.bitbucket.ucchy.undine.bridge.VaultEcoBridge;
import org.bitbucket.ucchy.undine.command.GroupCommand;
import org.bitbucket.ucchy.undine.command.ListCommand;
import org.bitbucket.ucchy.undine.command.UndineCommand;
import org.bitbucket.ucchy.undine.group.GroupManager;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * UndineMailer メール送受信システムプラグイン
 * @author ucchy
 */
public class UndineMailer extends JavaPlugin {

    private static final String MAIL_FOLDER = "mail";
    private static final String GROUP_FOLDER = "group";

    private MailManager mailManager;
    private AttachmentBoxManager boxManager;
    private GroupManager groupManager;
    private MailCleanupTask cleanupTask;
    private HashMap<String, MailSender> playerCache;
    private boolean isPlayerCacheLoaded;

    private UndineCommand undineCommand;
    private ListCommand listCommand;
    private GroupCommand groupCommand;

    private UndineConfig config;

    private VaultEcoBridge vaulteco;
    private PermissionsExBridge pex;

    /**
     * プラグインが有効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // コンフィグをロードする
        config = new UndineConfig(this);

        // VaultEcoをロード
        if ( getServer().getPluginManager().isPluginEnabled("Vault") ) {
            vaulteco = VaultEcoBridge.load(
                    getServer().getPluginManager().getPlugin("Vault"));
        }

        // PermissionsExをロード
        if ( getServer().getPluginManager().isPluginEnabled("PermissionsEx") ) {
            pex = PermissionsExBridge.load(
                    getServer().getPluginManager().getPlugin("PermissionsEx"));
        }

        // マネージャを生成し、データをロードする
        groupManager = new GroupManager(this);
        mailManager = new MailManager(this);
        boxManager = new AttachmentBoxManager(this);

        // メッセージをロードする
        File langFolder = new File(getDataFolder(), "lang");
        Messages.initialize(getFile(), langFolder, getDefaultLocaleLanguage());
        Messages.reload(config.getLang());

        // コマンドクラスを作成する
        undineCommand = new UndineCommand(this);
        listCommand = new ListCommand(this);
        groupCommand = new GroupCommand(this);

        // メールクリーンアップタスクを起動する
        cleanupTask = new MailCleanupTask(mailManager);
        cleanupTask.startTask();

        // リスナーの登録
        getServer().getPluginManager().registerEvents(new UndineListener(this), this);

        // プレイヤーキャッシュの作成
        checkAllValidPlayerNamesAsync();
    }

    /**
     * プラグインが無効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {

        // タスクを停止する
        cleanupTask.cancel();

        // 添付ボックスを開いたままにしているプレイヤーの
        // インベントリを強制的に閉じる
        boxManager.closeAllBox();

        // 編集中メールの保存
        mailManager.storeEditmodeMail();
    }

    /**
     * コマンドが実行された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ( command.getName().equals("mail") )
            return undineCommand.onCommand(sender, command, label, args);
        else if ( command.getName().equals("undinelist") )
            return listCommand.onCommand(sender, command, label, args);
        else if ( command.getName().equals("undinegroup") )
            return groupCommand.onCommand(sender, command, label, args);
        return false;
    }

    /**
     * タブキーで補完された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if ( command.getName().equals("mail") )
            return undineCommand.onTabComplete(sender, command, alias, args);
        else if ( command.getName().equals("undinelist") )
            return listCommand.onTabComplete(sender, command, alias, args);
        else if ( command.getName().equals("undinegroup") )
            return groupCommand.onTabComplete(sender, command, alias, args);
        return null;
    }

    /**
     * メールデータを格納するフォルダを返す
     * @return メールデータ格納フォルダ
     */
    public File getMailFolder() {
        File folder = new File(getDataFolder(), MAIL_FOLDER);
        if ( !folder.exists() ) {
            folder.mkdirs();
        }
        return folder;
    }

    /**
     * グループデータを格納するフォルダを返す
     * @return グループデータ格納フォルダ
     */
    public File getGroupFolder() {
        File folder = new File(getDataFolder(), GROUP_FOLDER);
        if ( !folder.exists() ) {
            folder.mkdirs();
        }
        return folder;
    }

    /**
     * メールマネージャを取得する
     * @return メールマネージャ
     */
    public MailManager getMailManager() {
        return mailManager;
    }

    /**
     * 添付ボックスマネージャを取得する
     * @return 添付ボックスマネージャ
     */
    public AttachmentBoxManager getBoxManager() {
        return boxManager;
    }

    /**
     * グループマネージャを取得する
     * @return グループマネージャ
     */
    public GroupManager getGroupManager() {
        return groupManager;
    }

    /**
     * コンフィグを取得する
     * @return コンフィグ
     */
    public UndineConfig getUndineConfig() {
        return config;
    }

    /**
     * 経済プラグインへのアクセスブリッジを取得する
     * @return VaultEcoBridge、ロードされていなければnullになる
     */
    public VaultEcoBridge getVaultEco() {
        return vaulteco;
    }

    /**
     * PermissionsExへのアクセスブリッジを取得する
     * @return PermissionsExBridge、ロードされていなければnullになる
     */
    public PermissionsExBridge getPex() {
        return pex;
    }

    /**
     * このプラグインのJarファイルを返す
     * @return Jarファイル
     */
    protected File getJarFile() {
        return getFile();
    }

    /**
     * 動作環境の言語設定を取得する。日本語環境なら ja、英語環境なら en が返される。
     * @return 動作環境の言語
     */
    protected static String getDefaultLocaleLanguage() {
        Locale locale = Locale.getDefault();
        if ( locale == null ) return "en";
        return locale.getLanguage();
    }

    /**
     * プレイヤーキャッシュを取得する
     * @return プレイヤーキャッシュ
     */
    public HashMap<String, MailSender> getPlayerCache() {
        return playerCache;
    }

    /**
     * このプラグインの関連データをリロードする
     * @param sender リロードが完了した時に、通知する先。通知が不要なら、nullでよい。
     */
    public void reloadAll(CommandSender sender) {
        groupManager.reload();
        if ( mailManager.isLoaded() ) {
            mailManager.reload(sender);
        }
        config.reloadConfig();
        Messages.reload(config.getLang());

        checkAllValidPlayerNamesAsync();
    }

    /**
     * 宛先として有効な全てのプレイヤー名を取得する
     * @return 有効な宛先
     */
    private void checkAllValidPlayerNamesAsync() {

        final long start = System.currentTimeMillis();
        isPlayerCacheLoaded = false;

        new BukkitRunnable() {
            public void run() {
                playerCache = new HashMap<String, MailSender>();
                for ( OfflinePlayer player : Bukkit.getOfflinePlayers() ) {
                    MailSender ms = MailSenderPlayer.getMailSenderFromString(player.getName());
                    if ( ms != null && ms.isValidDestination() ) {
                        playerCache.put(player.getName(), ms);
                    }
                }
                UndineMailer.getInstance().getLogger().info("Async load offline player data... Done. Time: "
                        + (System.currentTimeMillis() - start) + "ms, Data: " + playerCache.size() + ".");
                isPlayerCacheLoaded = true;
            }
        }.runTaskAsynchronously(this);
    }

    /**
     * プレイヤーキャッシュがロードされているかどうかを返す
     * @return プレイヤーキャッシュがロードされているかどうか
     */
    public boolean isPlayerCacheLoaded() {
        return isPlayerCacheLoaded;
    }

    /**
     * このプラグインのインスタンスを返す
     * @return プラグインのインスタンス
     */
    public static UndineMailer getInstance() {
        return (UndineMailer)Bukkit.getPluginManager().getPlugin("UndineMailer");
    }
}
