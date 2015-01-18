/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.io.File;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Undine メール送受信システムプラグイン
 * @author ucchy
 */
public class Undine extends JavaPlugin {

    private static final String MAIL_FOLDER = "mail";

    private MailManager mailManager;
    private AttachmentBoxManager boxManager;
    private UndineConfig config;
    private UndineCommand command;

    /**
     * プラグインが有効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // マネージャを生成し、データをロードする
        mailManager = new MailManager(this);
        boxManager = new AttachmentBoxManager(this);

        // コンフィグをロードする
        config = new UndineConfig(this);

        // メッセージをロードする
        Messages.initialize(getFile(), getDataFolder());
        Messages.reload(config.getLang());

        // コマンドクラスを作成する
        command = new UndineCommand(this);

        // リスナーの登録
        getServer().getPluginManager().registerEvents(new UndineListener(this), this);
    }

    /**
     * コマンドが実行された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return this.command.onCommand(sender, command, label, args);
    }

    /**
     * タブキーで補完された時に呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.command.onTabComplete(sender, command, alias, args);
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
     * コンフィグを取得する
     * @return コンフィグ
     */
    public UndineConfig getUndineConfig() {
        return config;
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
}
