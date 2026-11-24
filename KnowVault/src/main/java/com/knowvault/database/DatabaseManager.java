package com.knowvault.database;

import com.knowvault.exception.KnowVaultException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

public final class DatabaseManager implements AutoCloseable {
    private final String url;

    public DatabaseManager() {
        this(defaultDatabasePath());
    }

    public DatabaseManager(Path databaseFile) {
        try {
            Path parent = databaseFile.toAbsolutePath().getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (IOException e) {
            throw new KnowVaultException("Cannot create database directory", e);
        }
        this.url = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
        initialize();
    }

    private static Path defaultDatabasePath() {
        return Path.of(System.getProperty("user.home"), ".knowvault", "knowvault.db");
    }

    public Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private void initialize() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS categories(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    color TEXT NOT NULL DEFAULT '#D86491'
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS notes(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL CHECK(length(trim(title)) > 0),
                    content TEXT NOT NULL DEFAULT '',
                    category_id INTEGER REFERENCES categories(id) ON DELETE SET NULL,
                    favorite INTEGER NOT NULL DEFAULT 0 CHECK(favorite IN (0,1)),
                    view_count INTEGER NOT NULL DEFAULT 0 CHECK(view_count >= 0),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS tags(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE COLLATE NOCASE CHECK(length(trim(name)) > 0)
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS note_tags(
                    note_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                    tag_id INTEGER NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
                    PRIMARY KEY(note_id, tag_id)
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS note_history(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    note_id INTEGER NOT NULL REFERENCES notes(id) ON DELETE CASCADE,
                    title TEXT NOT NULL, content TEXT NOT NULL, category_id INTEGER,
                    favorite INTEGER NOT NULL, tags TEXT NOT NULL DEFAULT '',
                    saved_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS search_history(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    query TEXT NOT NULL, result_count INTEGER NOT NULL,
                    searched_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""");
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS activity_logs(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    action TEXT NOT NULL, note_id INTEGER,
                    details TEXT NOT NULL DEFAULT '', occurred_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )""");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notes_updated ON notes(updated_at DESC)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_notes_category ON notes(category_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_note_tags_tag ON note_tags(tag_id)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_history_note ON note_history(note_id, saved_at DESC)");
            insertSamplesIfEmpty(connection);
        } catch (SQLException e) {
            throw new KnowVaultException("Database initialization failed", e);
        }
    }

    private void insertSamplesIfEmpty(Connection connection) throws SQLException {
        try (Statement check = connection.createStatement(); ResultSet rs = check.executeQuery("SELECT COUNT(*) FROM notes")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            long javaId = insertCategory(connection, "Java", "#D86491");
            long designId = insertCategory(connection, "Architecture", "#B94F7C");
            insertSample(connection, "Java Collections at a Glance",
                    "List preserves order, Set keeps unique values, and Map stores key/value pairs. Prefer interfaces in APIs and choose implementations from access and ordering needs.", javaId,
                    "java", "collections", "study");
            insertSample(connection, "Modern Java Concurrency",
                    "ExecutorService manages worker threads. Callable returns a value through Future. JavaFX Task moves expensive work away from the application thread.", javaId,
                    "java", "concurrency", "threads");
            insertSample(connection, "Layered Desktop Architecture",
                    "Views render state, controllers coordinate interaction, services implement use cases, repositories persist models, and the database layer owns connections.", designId,
                    "architecture", "javafx", "patterns");
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private long insertCategory(Connection c, String name, String color) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO categories(name,color) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name); ps.setString(2, color); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
        }
    }

    private void insertSample(Connection c, String title, String content, long category, String... tagNames) throws SQLException {
        long noteId;
        try (PreparedStatement ps = c.prepareStatement("INSERT INTO notes(title,content,category_id) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title); ps.setString(2, content); ps.setLong(3, category); ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) { keys.next(); noteId = keys.getLong(1); }
        }
        for (String tag : tagNames) {
            try (PreparedStatement add = c.prepareStatement("INSERT OR IGNORE INTO tags(name) VALUES(?)")) { add.setString(1, tag); add.executeUpdate(); }
            try (PreparedStatement link = c.prepareStatement("INSERT INTO note_tags(note_id,tag_id) SELECT ?,id FROM tags WHERE name=?")) {
                link.setLong(1, noteId); link.setString(2, tag); link.executeUpdate();
            }
        }
    }

    @Override public void close() { /* Connections are intentionally short lived. */ }
}
