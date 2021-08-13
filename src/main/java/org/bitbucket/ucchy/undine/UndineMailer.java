/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.bitbucket.ucchy.undine.bridge.LuckPermsBridge;
import org.bitbucket.ucchy.undine.bridge.VaultEcoBridge;
import org.bitbucket.ucchy.undine.command.GroupCommand;
import org.bitbucket.ucchy.undine.command.ListCommand;
import org.bitbucket.ucchy.undine.command.UndineCommand;
import org.bitbucket.ucchy.undine.group.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * UndineMailer メール送受信システムプラグイン
 * @author ucchy
 */
public class UndineMailer extends JavaPlugin {

    private static final String MAIL_FOLDER = "mail";
    private static final String GROUP_FOLDER = "group";
    private static final String CACHE_FOLDER = "cache";

    private MailManager mailManager;
    private AttachmentBoxManager boxManager;
    private GroupManager groupManager;
    private MailCleanupTask cleanupTask;
    private PlayerUuidCache playerUuidCache;

    private UndineCommand undineCommand;
    private ListCommand listCommand;
    private GroupCommand groupCommand;

    private UndineConfig config;

    private VaultEcoBridge vaulteco;
    private LuckPermsBridge lp;

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

        // LuckPermsをロード
        if ( getServer().getPluginManager().isPluginEnabled("LuckPerms") ) {
            lp = LuckPermsBridge.load(
                    getServer().getPluginManager().getPlugin("LuckPerms"));
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
        playerUuidCache = PlayerUuidCache.load();

        // プレイヤーキャッシュのリロード
        playerUuidCache.refresh();
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
     * キャッシュデータを格納するフォルダを返す
     * @return キャッシュデータ格納フォルダ
     */
    public File getCacheFolder() {
        File folder = new File(getDataFolder(), CACHE_FOLDER);
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
     * LuckPermsへのアクセスブリッジを取得する
     * @return LuckPermsBridge、ロードされていなければnullになる
     */
    public LuckPermsBridge getLp() {
        return lp;
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
     * 指定されたプレイヤー名のUUIDをキャッシュから取得する
     * @param name プレイヤー名
     * @return UUID
     */
    public String getUUID(String name) {
        return playerUuidCache.getUUID(name);
    }

    /**
     * 指定されたUUIDのプレイヤー名をキャッシュから取得する
     * @param uuid UUID
     * @return プレイヤー名
     */
    public String getName(String uuid) {
        return playerUuidCache.getName(uuid);
    }

    /**
     * 指定されたプレイヤー名のUUIDを、非同期スレッドで更新する
     * @param name プレイヤー名
     */
    public void asyncRefreshPlayerUuid(String name) {
        playerUuidCache.asyncRefreshPlayerUuid(name);
    }

    /**
     * キャッシュしているプレイヤー名の一覧を返す
     * @return プレイヤー名一覧
     */
    public Set<String> getPlayerNames() {
        return playerUuidCache.getPlayerNames();
    }

    /**
     * キャッシュされているすべてのUUIDを取得する
     * @return すべてのUUID
     */
    public HashSet<String> getPlayerUuids() {
        return playerUuidCache.getPlayerUuids();
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

        playerUuidCache.refresh();
    }

    /**
     * プレイヤーキャッシュがロードされているかどうかを返す
     * @return プレイヤーキャッシュがロードされているかどうか
     */
    public boolean isPlayerCacheLoaded() {
        return playerUuidCache.isPlayerCacheLoaded();
    }

    /**
     * このプラグインのインスタンスを返す
     * @return プラグインのインスタンス
     */
    public static UndineMailer getInstance() {
        return (UndineMailer)Bukkit.getPluginManager().getPlugin("UndineMailer");
    }
}
