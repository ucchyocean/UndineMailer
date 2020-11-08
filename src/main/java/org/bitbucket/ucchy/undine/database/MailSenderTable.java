/*
 * @author     LazyGon
 * @license    LGPLv3
 * @copyright  Copyright OKOCRAFT 2020
 */
package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderBlock;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bitbucket.ucchy.undine.sender.MailSenderDummy;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Location;

/**
 * MailSenderを保存するテーブルにアクセスするクラス。。Bukkitからのみだと他のサーバーからのメール送信に対応できないため、このテーブルを実装している。
 * @author LazyGon
 */
public class MailSenderTable {

    public static final String NAME = "undine_mailsenders";
    
    private final Database database;

    private final BiMap<Integer, MailSender> mailSenderCache = HashBiMap.create();

    MailSenderTable(Database database) {
        this.database = database;
        createTable();
    }
    
    void createTable() {
        database.execute(
            "CREATE TABLE IF NOT EXISTS " + NAME + " (" +
                "id INTEGER PRIMARY KEY " + database.getDatabaseType().autoIncrement + ", " +
                "uuidMost BIGINT NOT NULL, " +
                "uuidLeast BIGINT NOT NULL, " +
                "type TINYINT NOT NULL, " +
                "name VARCHAR(128) NOT NULL DEFAULT '', " +
                "location VARCHAR(128) UNIQUE, " +
                "UNIQUE(uuidMost, uuidLeast)" +
            ")"
        );
    }

    /**
     * idからMailSenderを取得する。
     * @param id MailSenderのデータベース内でのid
     * @return MailSender
     */
    public MailSender getById(int id) {
        if (id <= 0) {
            return null;
        }

        if (mailSenderCache.containsKey(id)) {
            return mailSenderCache.get(id);
        }

        MailSender result = database.query("SELECT type, name, uuidMost, uuidLeast FROM " + NAME + " WHERE id = " + id, rs -> {
            try {
                if (rs.next()) {
                    switch ((int)rs.getByte("type")) {
                        case 0: return new MailSenderBlock(rs.getString("name"), Database.fromDBLocationString(rs.getString("location")));
                        case 1: return MailSenderConsole.getMailSenderConsole();
                        case 2: return MailSender.getMailSenderFromString("$" + new UUID(rs.getLong("uuidMost"), rs.getLong("uuidLeast")));
                        case 3:
                        case 4: return new MailSenderDummy(rs.getString("name"));
                        default: return null;
                    }
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });

        mailSenderCache.put(id, result);

        return result;
    }

    /**
     * 指定されたMailSenderのデータベース内でのidを取得する。MailSenderConsoleについては見つからなかったときはinsertしてそのとき生成されたidを返す。
     * @param mailSender idを取得するMailSender
     * @return MailSenderのid
     */
    public int getId(MailSender mailSender) {
        if (mailSenderCache.containsValue(mailSender)) {
            return mailSenderCache.inverse().get(mailSender);
        }

        if (mailSender instanceof MailSenderBlock) {
            return getIdWhere(
                "WHERE " +
                    "type = 0 AND " +
                    "name = '" + mailSender.getName() + "' AND " +
                    "location = '" + Database.createDBLocationString(((MailSenderBlock) mailSender).getLocation()) + "'"
            );
        } else if (mailSender instanceof MailSenderConsole) {
            int id = getIdWhere("WHERE type = 1");
            if (id != -1) {
                return id;
            }
            return database.insert("INSERT INTO " + NAME + " (uuidMost, uuidLeast, type, name) VALUES (0, 0, 1, 'CONSOLE')").get(0);
        } else if (mailSender instanceof MailSenderPlayer) {
            UUID uuid = UUID.fromString(mailSender.toString().replace("$", ""));
            return getIdWhere("WHERE type = 2 AND (uuidMost = " + uuid.getMostSignificantBits() + " AND uuidLeast = " + uuid.getLeastSignificantBits() + ")");
        } else if (mailSender instanceof MailSenderDummy) {
            return getIdWhere("WHERE (type = 3 OR type = 4) AND name = '" + mailSender.getName() + "'");
        }
        return -1;
    }

    /**
     * 指定されたすべてのMailSenderのidを取得する。
     * そのidとMailSenderのマッピングが欲しい場合はここから更にgetByIdsを使う必要がある。(複雑すぎて1メソッドでは実装できなかったため)
     * @param mailSenders idを取得するMailSender
     * @return 取得されたidのリスト
     */
    public List<Integer> getIds(Collection<MailSender> mailSenders) {
        mailSenders = new ArrayList<>(mailSenders);
        List<Integer> result = new ArrayList<>();
        for (MailSender mailSender : mailSenders) {
            if (mailSenderCache.containsValue(mailSender)) {
                result.add(mailSenderCache.inverse().get(mailSender));
            }
        }
        mailSenders.removeAll(mailSenderCache.values());

        if (mailSenders.isEmpty()) {
            return result;
        }

        addAll(mailSenders);

        List<MailSender> blocks = new ArrayList<>();
        MailSender console = null;
        List<MailSender> players = new ArrayList<>();
        List<MailSender> dummies = new ArrayList<>();
        for (MailSender sender : mailSenders) {
            if (sender instanceof MailSenderBlock) {
                blocks.add(sender);
            } else if (sender instanceof MailSenderConsole && console == null) {
                console = sender;
            } else if (sender instanceof MailSenderPlayer) {
                players.add(sender);
            } else if (sender instanceof MailSenderDummy) {
                dummies.add(sender);
            }
        }


        if (!blocks.isEmpty()) {
            StringBuilder whereBuilder = new StringBuilder();
            for (MailSender block : blocks) {
                whereBuilder.append("(").append("name = '").append(block.getName()).append("' AND location = '")
                        .append(Database.createDBLocationString(block.getLocation())).append("') OR");
            }
            whereBuilder.delete(whereBuilder.length() - 3, whereBuilder.length());
            result.addAll(getIdsWhere("WHERE type = 0 AND " + whereBuilder.toString()));
        }
        if (console != null) {
            int id = getIdWhere("WHERE type = 1");
            if (id == -1) {
                id = database.insert("INSERT INTO " + NAME + " (uuidMost, uuidLeast, type, name) VALUES (0, 0, 1, 'CONSOLE')").get(0);
            }
            result.add(id);
        }
        if (!players.isEmpty()) {
            StringBuilder whereBuilder = new StringBuilder();
            for (MailSender player : players) {
                UUID uuid = UUID.fromString(player.toString().replace("$", ""));
                whereBuilder.append("(").append("uuidMost = ").append(uuid.getMostSignificantBits()).append(" AND uuidLeast = ")
                        .append(uuid.getLeastSignificantBits()).append(") OR");
            }
            whereBuilder.delete(whereBuilder.length() - 3, whereBuilder.length());
            result.addAll(getIdsWhere("WHERE type = 2 AND " + whereBuilder.toString()));
        }
        if (!dummies.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (MailSender dummy : dummies) {
                names.add(dummy.getName());
            }
            result.addAll(getIdsWhere("WHERE (type = 3 OR type = 4) AND name " + Database.createIn(names)));
        }

        getByIds(result).forEach((id, sender) -> {
            if (!mailSenderCache.containsKey(id)) {
                mailSenderCache.put(id, sender);
            }
        });

        return result;
    }

    /**
     * WHERE句を指定してMailSenderのidを取得する。
     * @param where SQL文のWHERE句部分
     * @return 条件に当てはまったMailSenderのリスト
     */
    private List<Integer> getIdsWhere(String where) {
        return database.query("SELECT id FROM " + NAME + (where == null || where.isBlank() ? "" : " " + where), rs -> {
            try {
                List<Integer> ids = new ArrayList<>();
                if (rs.next()) {
                    ids.add(rs.getInt("id"));
                }
                return ids;
            } catch (SQLException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    /**
     * WHERE句を指定してMailSenderを取得する。結果が一つになるようなWHERE句を書くことが推奨される。
     * @param where SQL文のWHERE句部分
     * @return 取得されたMailSender
     */
    private int getIdWhere(String where) {
        List<Integer> ids = getIdsWhere(where);
        return ids.isEmpty() ? -1 : ids.get(0);
    }

    /**
     * idのリストからMailSenderをすべて取得する。
     * @param ids idのリスト
     * @return 指定されたidと取得されたMailSenderのMap
     */
    public Map<Integer, MailSender> getByIds(Collection<Integer> ids) {
        ids = new ArrayList<>(ids);
        Map<Integer, MailSender> result = new HashMap<>();
        for (int id : ids) {
            if (mailSenderCache.containsKey(id)) {
                result.put(id, mailSenderCache.get(id));
            }
        }
        ids.removeAll(result.keySet());
        if (ids.isEmpty()) {
            return result;
        }
        database.query("SELECT id, type, name, uuidMost, uuidLeast FROM " + NAME + " WHERE id " + Database.createIn(ids), rs -> {
            try {
                while (rs.next()) {
                    int type = (int)rs.getByte("type");
                    if (type == 0) {
                        result.put(rs.getInt("id"), new MailSenderBlock(rs.getString("name"), Database.fromDBLocationString(rs.getString("location"))));
                    } else if (type == 1) {
                        result.put(rs.getInt("id"), MailSenderConsole.getMailSenderConsole());
                    } else if (type == 2) {
                        result.put(rs.getInt("id"), MailSender.getMailSenderFromString("$" + new UUID(rs.getLong("uuidMost"), rs.getLong("uuidLeast"))));
                    } else if (type == 3 || type == 4) {
                        result.put(rs.getInt("id"), new MailSenderDummy(rs.getString("name")));
                    }
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
        return result;
    }

    /**
     * 指定されたMailSenderをデータベースに追加する。
     * @param mailSender
     */
    public void add(MailSender mailSender) {
        addAll(Arrays.asList(mailSender));
    }

    /**
     * 指定されたすべてのMailSenderをデータベースに追加する。
     * @param mailSenders MailSenderのリスト
     */
    public void addAll(Collection<MailSender> mailSenders) {
        if (mailSenders.isEmpty()) {
            return;
        }
        List<MailSender> blocks = new ArrayList<>();
        MailSender console = null;
        List<MailSender> players = new ArrayList<>();
        List<MailSender> dummies = new ArrayList<>();
        List<MailSender> others = new ArrayList<>();
        for (MailSender sender : mailSenders) {
            if (sender instanceof MailSenderBlock) {
                blocks.add(sender);
            } else if (sender instanceof MailSenderConsole && console == null) {
                console = sender;
            } else if (sender instanceof MailSenderPlayer) {
                players.add(sender);
            } else if (sender instanceof MailSenderDummy) {
                dummies.add(sender);
            } else {
                others.add(sender);
            }
        }

        String insert = "INSERT INTO " + NAME + " (uuidMost, uuidLeast, type, name, location) VALUES ";

        if (!blocks.isEmpty()) {
            StringBuilder valuesBuilder = new StringBuilder();
            for (MailSender block : blocks) {
                UUID uid = UUID.randomUUID();
                String loc = Database.createDBLocationString(block.getLocation());
                valuesBuilder.append("(")
                    .append(uid.getMostSignificantBits()).append(", ")
                    .append(uid.getLeastSignificantBits()).append(", ")
                    .append(0).append(", ")
                    .append("'").append(block.getName()).append("'").append(", ")
                    .append(loc != null ? loc : "NULL")
                .append("), ");
            }
            valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length());
            String onDuplicateKey = database.getDatabaseType() == DatabaseType.MYSQL
                ? " ON DUPLICATE KEY UPDATE name = VALUES(name), location = VALUES(location)"
                : " ON CONFLICT(uuidMost, uuidLeast) DO UPDATE SET name = excluded.name, location = excluded.location" + // UUIDはほぼ衝突しない。
                        " ON CONFLICT(location) DO UPDATE SET name = excluded.name, uuidMost = excluded.uuidMost, uuidLeast = excluded.uuidLeast";
            database.execute(insert + valuesBuilder.toString() + onDuplicateKey);
        }
        if (console != null) {
            String onDuplicateKey = database.getDatabaseType() == DatabaseType.MYSQL
                ? " ON DUPLICATE KEY UPDATE type = type"
                : " ON CONFLICT(uuidMost, uuidLeast) DO NOTHING";
            database.execute(insert + "(" + 0L + ", " + 0L + ", 1, 'CONSOLE', NULL)" + onDuplicateKey);
        }
        if (!players.isEmpty()) {
            StringBuilder valuesBuilder = new StringBuilder();
            for (MailSender player : players) {
                UUID uid = player.getOfflinePlayer().getUniqueId();
                valuesBuilder.append("(")
                    .append(uid.getMostSignificantBits()).append(", ")
                    .append(uid.getLeastSignificantBits()).append(", ")
                    .append(2).append(", ")
                    .append("'").append(player.getName()).append("'").append(", ")
                    .append("NULL")
                .append("), ");
            }
            valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length());
            String onDuplicateKey = database.getDatabaseType() == DatabaseType.MYSQL
                ? " ON DUPLICATE KEY UPDATE name = VALUES(name)"
                : " ON CONFLICT(uuidMost, uuidLeast) DO UPDATE SET name = excluded.name";
            database.execute(insert + valuesBuilder.toString() + onDuplicateKey);
        }
        if (!dummies.isEmpty()) {
            StringBuilder valuesBuilder = new StringBuilder();
            for (MailSender dummy : dummies) {
                UUID uid = UUID.randomUUID();
                valuesBuilder.append("(")
                    .append(uid.getMostSignificantBits()).append(", ")
                    .append(uid.getLeastSignificantBits()).append(", ")
                    .append(3).append(", ")
                    .append("'").append(dummy.getName()).append("'").append(", ")
                    .append("NULL")
                .append("), ");
            }
            valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length());
            String onDuplicateKey = database.getDatabaseType() == DatabaseType.MYSQL
                ? " ON DUPLICATE KEY UPDATE name = VALUES(name)"
                : " ON CONFLICT(uuidMost, uuidLeast) DO UPDATE SET name = excluded.name";
            database.execute(insert + valuesBuilder.toString() + onDuplicateKey);
        }
        if (!others.isEmpty()) {
            // CommandSenderを実装するEntityをここに入れたいが
            // UndineMailderでは残念ながら実装されていないため使われることはないと思われる。
            StringBuilder valuesBuilder = new StringBuilder();
            for (MailSender other : others) {
                UUID uid = UUID.randomUUID();
                valuesBuilder.append("(")
                    .append(uid.getMostSignificantBits()).append(", ")
                    .append(uid.getLeastSignificantBits()).append(", ")
                    .append(4).append(", ")
                    .append("'").append(other.getName()).append("'").append(", ")
                    .append("NULL")
                .append("), ");
            }
            valuesBuilder.delete(valuesBuilder.length() - 2, valuesBuilder.length());
            String onDuplicateKey = database.getDatabaseType() == DatabaseType.MYSQL
                ? " ON DUPLICATE KEY UPDATE name = VALUES(name)"
                : " ON CONFLICT(uuidMost, uuidLeast) DO UPDATE SET name = excluded.name";
            database.execute(insert + valuesBuilder.toString() + onDuplicateKey);
        }
    }

    /**
     * データベースに登録されている名前を変更する。
     * @param mailSender 名前を変更するMailSender
     * @param newName 新しい名前
     */
    public void updateName(MailSender mailSender, String newName) {
        database.execute("UPDATE " + NAME + " SET name = '" + newName + "' WHERE id = " + getId(mailSender));
    }

    /**
     * かつて指定された位置にあったBlockのMailSenderをデータベースから取得する。過去にそうだったとしても現在はそのブロックはMailSenderにはなりえない可能性がある。
     * @param location ブロックの位置
     * @return MailSenderBlock
     */
    public MailSenderBlock getMailSenderBlockAt(Location location) {
        return database.query("SELECT name, location FROM " + NAME + " " +
                "WHERE type = 0 AND location = '" + Database.createDBLocationString(location) + "'", rs -> {
            try {
                if (rs.next()) {
                    return new MailSenderBlock(rs.getString("name"), Database.fromDBLocationString(rs.getString("location")));
                }
                return null;
            } catch (SQLException e) {
                e.printStackTrace();
                return null;
            }
        });
    }
    
    /**
     * 指定されたMailSenderがデータベースに存在するかを調べる。
     * @param mailSender 調べるMailSender
     * @return データベースに存在したらtrue
     */
    public boolean exists(MailSender mailSender) {
        return getId(mailSender) != -1;
    }

    /**
     * データベースから指定されたMailSenderを削除する。
     * @param mailSender 削除するMailSender
     * @return 削除したかどうか
     */
    public boolean delete(MailSender mailSender) {
        return database.execute("DELETE FROM " + NAME + " WHERE id = " + getId(mailSender));
    }
}
