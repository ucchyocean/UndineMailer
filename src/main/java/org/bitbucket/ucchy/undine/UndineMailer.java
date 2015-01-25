/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.util.List;

import org.bitbucket.ucchy.undine.group.GroupCommand;
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

    protected static final String COMMAND = "/umail";

    private static final String MAIL_FOLDER = "mail";
    private static final String GROUP_FOLDER = "group";

    private MailManager mailManager;
    private AttachmentBoxManager boxManager;
    private GroupManager groupManager;

    private UndineCommand undineCommand;
    private ListCommand listCommand;
    private GroupCommand groupCommand;

    private UndineConfig config;

    private VaultEcoBridge vaulteco;

    /**
     * プラグインが有効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // マネージャを生成し、データをロードする
        mailManager = new MailManager(this);
        boxManager = new AttachmentBoxManager(this);
        groupManager = new GroupManager(this);

        // コンフィグをロードする
        config = new UndineConfig(this);

        // VaultEcoをロード
        if ( getServer().getPluginManager().isPluginEnabled("Vault") ) {
            vaulteco = VaultEcoBridge.load(
                    getServer().getPluginManager().getPlugin("Vault"));
        }

        // メッセージをロードする
        Messages.initialize(getFile(), getDataFolder());
        Messages.reload(config.getLang());

        // コマンドクラスを作成する
        undineCommand = new UndineCommand(this);
        listCommand = new ListCommand(this);
        groupCommand = new GroupCommand(this);

        // リスナーの登録
        getServer().getPluginManager().registerEvents(new UndineListener(this), this);
    }

    /**
     * プラグインが無効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onDisable()
     */
    @Override
    public void onDisable() {

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
        if ( command.getName().equals("undine") )
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
        if ( command.getName().equals("undine") )
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
    protected AttachmentBoxManager getBoxManager() {
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
    protected VaultEcoBridge getVaultEco() {
        return vaulteco;
    }

    /**
     * このプラグインのJarファイルを返す
     * @return Jarファイル
     */
    protected File getJarFile() {
        return getFile();
    }

    /**
     * このプラグインのリリース先を返す
     * @return en または ja (pom.xml の release.lang の内容が返される)
     */
    protected String getReleaseLang() {
        String[] descs = getDescription().getDescription().split(" ");
        if ( descs.length <= 0 ) return "en";
        return descs[descs.length - 1];
    }

    /**
     * このプラグインのインスタンスを返す
     * @return プラグインのインスタンス
     */
    public static UndineMailer getInstance() {
        return (UndineMailer)Bukkit.getPluginManager().getPlugin("UndineMailer");
    }
}
