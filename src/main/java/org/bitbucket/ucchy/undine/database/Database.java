package org.bitbucket.ucchy.undine.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Function;
import java.util.logging.Level;

import org.bitbucket.ucchy.undine.UndineConfig;
import org.bitbucket.ucchy.undine.UndineMailer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Database {

    /** データベースへの接続。 */
    
    private final DatabaseType databaseType;
    
    private String mySQLHost;
    private String mySQLUser;
    private String mySQLPass;
    private int mySQLPort;
    private String mySQLDBName;
    
    private Path sqliteDBFile;

    private Connection connection;
    
    final UndineMailer parent;

    public final MailSenderTable mailSenderTable;
    public final GroupDataTable groupDataTable;
    public final GroupMembersTable groupMembersTable;
    public final MailDataTable mailDataTable;
    public final MailRecipientsTable mailRecipientsTable;
    public final MailRecipientGroupsTable mailRecipientGroupsTable;
    public final MailAttachmentBoxTable mailAttachmentBoxTable;
    public final MailAttachmentBoxSnapshotTable mailAttachmentBoxSnapshotTable;
    public final DraftMailDataTable draftMailDataTable;
    public final DraftMailRecipientsTable draftMailRecipientsTable;
    public final DraftMailRecipientGroupsTable draftMailRecipientGroupsTable;
    public final DraftMailAttachmentBoxTable draftMailAttachmentBoxTable;

    /**
     * SQLiteに接続する。
     * 
     * @param dbPath SQLiteのデータファイルのパス
     * @throws SQLException {@code Connection}の生成中に例外が発生した場合
     */
    public Database(UndineMailer parent, DatabaseType type) throws IOException, SQLException {
        this.parent = parent;
        this.databaseType = type;

        UndineConfig config = parent.getUndineConfig();

        if (databaseType == DatabaseType.SQLITE) {
            this.sqliteDBFile = UndineMailer.getInstance().getDataFolder().toPath().resolve("maildata.db");
            if (!Files.exists(sqliteDBFile)) {
                Files.createFile(sqliteDBFile);
            }
            
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException | LinkageError e) {
                throw new SQLException("Error occurred on loading SQLite JDBC driver.", e);
            }
        } else if (databaseType == DatabaseType.MYSQL) {
            this.mySQLHost = config.getMysqlHost();
            this.mySQLUser = config.getMysqlUser();
            this.mySQLDBName = config.getMysqlDBName();
            this.mySQLPass = config.getMysqlPass();
            this.mySQLPort = config.getMysqlPort();
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException | LinkageError e) {
                throw new SQLException("Error occurred on loading MySQL connector.", e);
            }
        } else {
            throw new IllegalStateException("DatabaseType must be sqlite or mysql");
        }

        reloadConnection();
        if (databaseType == DatabaseType.SQLITE) {
            execute("PRAGMA foreign_keys = ON");
        }
        
        mailSenderTable = new MailSenderTable(this);
        groupDataTable = new GroupDataTable(this, mailSenderTable);
        groupMembersTable = new GroupMembersTable(this, mailSenderTable, groupDataTable);
        mailDataTable = new MailDataTable(this, mailSenderTable);
        mailRecipientsTable = new MailRecipientsTable(this, mailSenderTable, mailDataTable);
        mailRecipientGroupsTable = new MailRecipientGroupsTable(this, mailDataTable, groupDataTable);
        mailAttachmentBoxTable = new MailAttachmentBoxTable(this, mailDataTable);
        mailAttachmentBoxSnapshotTable = new MailAttachmentBoxSnapshotTable(this, mailDataTable);
        draftMailDataTable = new DraftMailDataTable(this, mailSenderTable);
        draftMailRecipientsTable = new DraftMailRecipientsTable(this, mailSenderTable, draftMailDataTable);
        draftMailRecipientGroupsTable = new DraftMailRecipientGroupsTable(this, draftMailDataTable, groupDataTable);
        draftMailAttachmentBoxTable = new DraftMailAttachmentBoxTable(this, draftMailDataTable);

    }

    private Connection reloadConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return connection;
        }
        if (databaseType == DatabaseType.MYSQL) {
            Properties prop = new Properties();
            prop.put("user", mySQLUser);
            prop.put("password", mySQLPass);
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + mySQLHost + ":" + mySQLPort + "/" + mySQLDBName + "?autoReconnect=true&useSSL=false",
                prop
            );
        } else if (databaseType == DatabaseType.SQLITE) {
            connection = DriverManager.getConnection("jdbc:sqlite:" + sqliteDBFile);
        }

        return connection;
    }

    public void dispose() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
                Integer.parseInt(part[1]),
                Integer.parseInt(part[2]),
                Integer.parseInt(part[3]),
                (float) Double.parseDouble(part[4]),
                (float) Double.parseDouble(part[5])
            );
        } catch (NumberFormatException | IndexOutOfBoundsException e) {
            e.printStackTrace();
            return new Location(world, 0, 0, 0, 0, 0);
        }
    }

    public static String createDBLocationString(Location location) {
        if (location == null || location.getWorld() == null) return null;
        return location.getWorld().getName() + "," +
                location.getBlockX() + "," +
                location.getBlockY() + "," +
                location.getBlockZ() + "," +
                (Math.floor(location.getYaw() * 10) / 10) + "," +
                (Math.floor(location.getPitch()) * 10) / 10;
    }

    /**
     * 指定した {@code SQL}を実行する。
     * 
     * @param SQL 実行するSQL文。メソッド内でPreparedStatementに変換される。
     * @return SQL文の実行に成功したかどうか
     */
    boolean execute(String SQL) {
        System.out.println(SQL);
        try (PreparedStatement preparedStatement = reloadConnection().prepareStatement(SQL)) {
            preparedStatement.execute();
            return true;
        } catch (SQLException e) {
            System.err.println("Error occurred on executing SQL: " + SQL);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 指定したINSERT文を実行する。AUTOINCREMENTによって新たに生成された数値を返す。
     * 値は挿入されたレコードの数だけ生成される。主キーがAUTOINCREMENTでない場合は値が生成されない。
     * INSERTではないSQLを実行する場合は {@link org.bitbucket.ucchy.undine.database.Database#execute(String)} を使うこと。
     * 
     * @param insert 実行するSQL文。メソッド内でPreparedStatementに変換される。
     * @return AUTOINCREMENTで生成された数値のリスト
     */
    List<Integer> insert(String insert) {
        System.out.println(insert);
        try (PreparedStatement preparedStatement = reloadConnection().prepareStatement(insert, Statement.RETURN_GENERATED_KEYS)) {
            if (preparedStatement.executeUpdate() == 0) {
                return new ArrayList<>();
            }
            ResultSet rs = preparedStatement.getGeneratedKeys();
            List<Integer> newIds = new ArrayList<>();
            while (rs.next()) {
                newIds.add(rs.getInt(1));
            }
            return newIds;
        } catch (SQLException e) {
            System.err.println("Error occurred on executing SQL: " + insert);
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * 指定した {@code SQL}を実行し、結果を第二引数で処理する。第二引数の処理が終わった後に、ResultSetはクローズされる。
     * 
     * @param SQL 実行するSQL文。メソッド内でPreparedStatementに変換される。
     * @param function 実行結果を処理する関数。
     * @return fuctionの処理結果
     */
    <T> T query(String SQL, Function<ResultSet, T> function) {
        System.out.println(SQL);
        try (PreparedStatement preparedStatement = reloadConnection().prepareStatement(SQL)) {
            return function.apply(preparedStatement.executeQuery());
        } catch (SQLException e) {
            UndineMailer.getInstance().getLogger().log(Level.SEVERE, "Error occurred on executing SQL: " + SQL, e);
            return null;
        }
    }
    
    /**
     * 渡されたコレクションから、WHERE句の中で使えるIN(element, element, ...)という文字列を生成する。
     * @param <T> コレクションの型
     * @param collection コレクション
     * @return IN(element, element, ...) という文字列
     */
    static <T> String createIn(Collection<T> collection) {
        if (collection == null || collection.isEmpty()) {
            return "IN()";
        }
        StringBuilder inBuilder = new StringBuilder("IN(");
        for (T element : collection) {
            if (element instanceof Number) {
                inBuilder.append(element.toString()).append(", ");
            } else {
                inBuilder.append("'").append(element.toString()).append("'").append(", ");
            }
        }
        inBuilder.delete(inBuilder.length() - 2, inBuilder.length()).append(")");
        return inBuilder.toString();
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
