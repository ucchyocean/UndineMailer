package org.bitbucket.ucchy.undine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Database {

    public static final String MAILDATA_TABLE = "undine_maildata";

    public static final String MAIL_RECIPIENTS_TABLE = "undine_mailrecipients";

    public static final String MAIL_RECIPIENT_GROUPS_TABLE = "undine_mailrecipientgroups";

    public static final String MAIL_ATTACHMENT_BOX_TABLE = "undine_mailattachmentbox";

    public static final String MAIL_ATTACHMENT_BOX_SNAPSHOT_TABLE = "undine_mailattachmentsnapshotbox";

    public static final String GROUPDATA_TABLE = "undine_group";

    public static final String GROUP_MEMBERS_TABLE = "undine_groupmembers";

    public static final String MAILSENDER_TABLE = "undine_mailsenders";

    /** データベースへの接続。 */
    private final Connection connection;

    private final DatabaseType databaseType;

    /**
     * 初期設定でSQLiteに接続する。
     * 
     * @param dbPath SQLiteのデータファイルのパス
     * @throws SQLException {@code Connection}の生成中に例外が発生した場合
     */
    public Database(Path dbPath) throws IOException, SQLException {
        Path dbFile = UndineMailer.getInstance().getDataFolder().toPath().resolve("maildata.db");
        if (!Files.exists(dbPath)) {
            Files.createFile(dbPath);
        }

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException | LinkageError e) {
            throw new SQLException("Error occurred on loading SQLite JDBC driver.", e);
        }

        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
        databaseType = DatabaseType.SQLITE;
        init();
    }

    /**
     * 推奨設定でMySQLに接続する。
     * 参照: https://github.com/brettwooldridge/HikariCP/wiki/MySQL-Configuration
     * 
     * @param host     ホスト
     * @param port     ポート
     * @param user     ユーザー
     * @param password パスワード
     * @param dbName   データベースの名前
     * @throws SQLException {@code Connection}の生成中に例外が発生した場合
     */
    public Database(String host, int port, String user, String password, String dbName) throws SQLException {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException | LinkageError e) {
            throw new SQLException("Error occurred on loading MySQL connector.", e);
        }

        Properties prop = new Properties();
        prop.put("user", user);
        prop.put("password", password);
        connection = DriverManager.getConnection(
            "jdbc:mysql://" + host + ":" + port + "/" + dbName + "?autoReconnect=true&useSSL=false",
            prop
        );
        databaseType = DatabaseType.MYSQL;
        init();
    }

    private void init() {
        initMailSenderTable();
        initMailDataTable();
        initMailRecipientsTable();
        initGroupDataTable();
        initMailRecipientGroupsTable();
        initMailAttachmentBoxTable();
        initMailAttachmentBoxSnapshotTable();
        initGroupMembersTable();

        //allGroupId = addOrGetAllGroup();
        //allConnectedGroupId = addOrGetAllConnectedGroup();
        //allLoginGroupId = addOrGetAllLoginGroup();
        //pexGroupId = addOrGetPexGroupId();

    }

    private void initMailSenderTable() {
        execute(
            "CREATE TABLE IF NOT EXISTS " + MAILSENDER_TABLE + " (" +
                "id INTEGER PRIMARY KEY " + databaseType.autoIncrement + ", " +
                "uuidMost BIGINT NOT NULL, " +
                "uuidLeast BIGINT NOT NULL, " +
                "type TINYINT NOT NULL, " +
                "name VARCHAR(128) NOT NULL DEFAULT '', " +
                "location VARCHAR(128) UNIQUE, " +
                "UNIQUE(uuidMost, uuidLeast)" +
            ")"
        );
    }

    private void initMailDataTable() {
        execute(
            "CREATE TABLE IF NOT EXISTS " + MAILDATA_TABLE + " (" +
                "id INTEGER PRIMARY KEY " + databaseType.autoIncrement + ", " +
                "sender INTEGER REFERENCES " + MAILSENDER_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "message TEXT(4096) NOT NULL DEFAULT '', " +
                "costMoney DOUBLE NOT NULL DEFAULT 0, " +
                "costItem TEXT(8192), " +
                "isEditmode TINYINT NOT NULL DEFAULT 0, " +
                "isBulk TINYINT NOT NULL DEFAULT 0, " +
                "isAttachmentsOpened TINYINT NOT NULL DEFAULT 0, " +
                "isAttachmentsCancelled TINYINT NOT NULL DEFAULT 0, " +
                "isAttachmentsRefused TINYINT NOT NULL DEFAULT 0, " +
                "attachmentsRefusedReason TEXT(512) NOT NULL DEFAULT '', " +
                "date BIGINT NOT NULL, " +
                "location VARCHAR(128), " +
                "UNIQUE(id, sender)" +
            ")"
        );
    }

    private void initMailRecipientsTable() {
        execute(
            "CREATE TABLE IF NOT EXISTS " + MAIL_RECIPIENTS_TABLE + " (" +
                "mailId INTEGER REFERENCES " + MAILDATA_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "recipient INTEGER REFERENCES " + MAILSENDER_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "isRead TINYINT NOT NULL DEFAULT 0, " +
                "isTrash TINYINT NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(mailId, recipient)" +
            ")"
        );
    }

    private void initGroupDataTable() {
        UndineConfig config = UndineMailer.getInstance().getUndineConfig();
        execute(
            "CREATE TABLE IF NOT EXISTS " + GROUPDATA_TABLE + " (" +
                "id INTEGER PRIMARY KEY " + databaseType.autoIncrement + ", " +
                "name VAECHAR(64) UNIQUE NOT NULL" +
                "owner INTEGER REFERENCES " + MAILSENDER_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "sendMode TINYINT NOT NULL DEFAULT " + config.getSendModeDefault().ordinal() + ", " +
                "modifyMode TINYINT NOT NULL DEFAULT " + config.getModifyModeDefault().ordinal() + ", " +
                "dissolutionMode TINYINT NOT NULL DEFAULT " + config.getDissolutionModeDefault().ordinal() +
            ")"
        );
    }

    private void initMailRecipientGroupsTable() {
        execute(
            "CREATE TABLE IF NOT EXISTS " + MAIL_RECIPIENT_GROUPS_TABLE + " (" +
                "mailId INTEGER REFERENCES " + MAILDATA_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "recipientGroup INTEGER REFERENCES " + GROUPDATA_TABLE + "(id) ON DELETE CASCADE ON UPDATE CASCADE, " +
                "PRIMARY KEY(mailId, recipientGroup)" +
            ")"
        );
    }

    private void initMailAttachmentBoxTable() {
        execute(
            "CREATE TABLE IF NOT EXISTS " + MAIL_ATTACHMENT_BOX_TABLE + " (" +
                "id INTEGER PRIMARY KEY " + databaseType.autoIncrement + ", " +
                "mailId INTEGER REFERENCES " + MAILDATA_TABLE + "(id) " + ", " +
                "item TEXT(8192) NOT NULL" +
            ")"
        );
        execute("CREATE INDEX IF NOT EXISTS attachmentmailid ON " + MAIL_ATTACHMENT_BOX_TABLE + "(id, mailId)");
    }

    private void initMailAttachmentBoxSnapshotTable() {
        execute(
            "CREATE TABLE IF NOT EXISTS " + MAIL_ATTACHMENT_BOX_SNAPSHOT_TABLE + " (" +
                "id INTEGER PRIMARY KEY " + databaseType.autoIncrement + ", " +
                "mailId INTEGER REFERENCES " + MAILDATA_TABLE + "(id) " + ", " +
                "item TEXT(8192) NOT NULL" +
            ")"
        );
        execute("CREATE INDEX IF NOT EXISTS snapshotattachmentmailid ON " + MAIL_ATTACHMENT_BOX_SNAPSHOT_TABLE + "(id, mailId)");
    }

    private void initGroupMembersTable() {
        execute(
            "CREATE TABLE IF NOT EXISTS " + GROUP_MEMBERS_TABLE + " (" +
                "groupId INTEGER REFERENCES " + GROUPDATA_TABLE + "(id) " + ", " +
                "member INTEGER REFERENCES " + MAILSENDER_TABLE + "(id), " +
                "PRIMARY KEY(groupId, member)" +
            ")"
        );
    }

    public DatabaseType getDatabaseType() {
        return databaseType;
    }

    public static Location fromDBLocationString(String dbLocationStr) {
        String[] part = dbLocationStr.split(",");
        World world = Bukkit.getWorld(part[0]);
        if (world == null) {
            world = Bukkit.getWorlds().get(0);
        }
        try {
            return new Location(
                world,
                Double.parseDouble(part[1]),
                Double.parseDouble(part[2]),
                Double.parseDouble(part[3]),
                (float) Double.parseDouble(part[4]),
                (float) Double.parseDouble(part[5])
            );
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            e.printStackTrace();
            return new Location(world, 0, 0, 0, 0, 0);
        }
    }

    public static String createDBLocationString(Location location) {
        return location.getWorld().getName() + "," +
                location.getX() + "," +
                location.getY() + "," +
                location.getZ() + "," +
                location.getYaw() + "," +
                location.getPitch();
    }

    /**
     * 指定した {@code SQL}を実行する。
     * 
     * @param statement 実行するSQL文。メソッド内でPreparedStatementに変換される。
     * @return SQL文の実行に成功したかどうか
     */
    public boolean execute(String SQL) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL)) {
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            System.err.println("Error occurred on executing SQL: " + SQL);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 指定した {@code SQL}を実行し、結果を第二引数で処理する。第二引数の処理が終わった後に、ResultSetはクローズされる。
     * 
     * @param queryState 実行するSQL文。メソッド内でPreparedStatementに変換される。
     * @param function 実行結果を処理する関数。
     * @return fuctionの処理結果
     */
    public <T> T query(String SQL, Function<ResultSet, T> function) {
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL)) {
            return function.apply(preparedStatement.executeQuery());
        } catch (SQLException e) {
            UndineMailer.getInstance().getLogger().log(Level.SEVERE, "Error occurred on executing SQL: " + SQL, e);
            return null;
        }
    }

    public enum DatabaseType {
        FLAT_FILE(""),
        SQLITE("AUTOINCREMENT"),
        MYSQL("AUTO_INCREMENT");

        public final String autoIncrement;

        private DatabaseType(String autoIncrement) {
            this.autoIncrement = autoIncrement;
        }

        public static DatabaseType getByName(String name) {
            for (DatabaseType type : values()) {
                if (type.name().toLowerCase(Locale.ROOT).replace("_", "").equals(name)) {
                    return type;
                }
            }

            return FLAT_FILE;
        }
    }
}
