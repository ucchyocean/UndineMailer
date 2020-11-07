package org.bitbucket.ucchy.undine.database;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bitbucket.ucchy.undine.database.Database.DatabaseType;
import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bitbucket.ucchy.undine.sender.MailSenderBlock;
import org.bitbucket.ucchy.undine.sender.MailSenderConsole;
import org.bitbucket.ucchy.undine.sender.MailSenderDummy;
import org.bitbucket.ucchy.undine.sender.MailSenderPlayer;
import org.bukkit.Location;

public class MailSenderTable {

    public static final String NAME = "undine_mailsenders";
    
    private final Database database;

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
     * NOTE: こいつが-1のとき何を返すかで初期値が変わる。注意
     * @param id
     * @return
     */
    public MailSender getById(int id) {
        if (id <= 0) {
            return null;
        }

        return database.query("SELECT type, name, uuidMost, uuidLeast FROM " + NAME + " WHERE id = " + id, rs -> {
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
    }

    public int getId(MailSender mailSender) {
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

    public List<Integer> getIds(Collection<MailSender> mailSenders) {
        if (mailSenders.isEmpty()) {
            return new ArrayList<>();
        }
        addAll(mailSenders);
        List<Integer> result = new ArrayList<>();

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
        return result;
    }

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

    private int getIdWhere(String where) {
        List<Integer> ids = getIdsWhere(where);
        return ids.isEmpty() ? -1 : ids.get(0);
    }

    public Map<Integer, MailSender> getByIds(Collection<Integer> ids) {
        if (ids.isEmpty()) {
            return new HashMap<>();
        }
        return database.query("SELECT id, type, name, uuidMost, uuidLeast FROM " + NAME + " WHERE id " + Database.createIn(ids), rs -> {
            try {
                Map<Integer, MailSender> senders = new HashMap<>();
                
                while (rs.next()) {
                    int type = (int)rs.getByte("type");
                    if (type == 0) {
                        senders.put(rs.getInt("id"), new MailSenderBlock(rs.getString("name"), Database.fromDBLocationString(rs.getString("location"))));
                    } else if (type == 1) {
                        senders.put(rs.getInt("id"), MailSenderConsole.getMailSenderConsole());
                    } else if (type == 2) {
                        senders.put(rs.getInt("id"), MailSender.getMailSenderFromString("$" + new UUID(rs.getLong("uuidMost"), rs.getLong("uuidLeast"))));
                    } else if (type == 3 || type == 4) {
                        senders.put(rs.getInt("id"), new MailSenderDummy(rs.getString("name")));
                    }
                }
                return senders;
            } catch (SQLException e) {
                e.printStackTrace();
                return new HashMap<>();
            }
        });
    }

    public void add(MailSender mailSender) {
        addAll(Arrays.asList(mailSender));
    }

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
                ? " ON DUPLICATE KEY UPDATE name = VALUES(name) WHERE type = 2"
                : " ON CONFLICT(uuidMost, uuidLeast) DO UPDATE SET name = excluded.name WHERE type = 2";
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
                ? " ON DUPLICATE KEY UPDATE name = VALUES(name) WHERE type = 2"
                : " ON CONFLICT(uuidMost, uuidLeast) DO UPDATE SET name = excluded.name WHERE type = 2";
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
                ? " ON DUPLICATE KEY UPDATE name = VALUES(name) WHERE type = 2"
                : " ON CONFLICT(uuidMost, uuidLeast) DO UPDATE SET name = excluded.name WHERE type = 2";
            database.execute(insert + valuesBuilder.toString() + onDuplicateKey);
        }
    }

    public String updateName(MailSender mailSender, String newName) {
        int id = getId(mailSender);
        database.execute("UPDATE " + NAME + " SET name = '" + newName + "' WHERE id = " + id);
        return null;
    }

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
    
    public boolean exists(MailSender mailSender) {
        return getId(mailSender) != -1;
    }

    public boolean delete(MailSender mailSender) {
        return database.execute("DELETE FROM " + NAME + " WHERE id = " + getId(mailSender));
    }
}
