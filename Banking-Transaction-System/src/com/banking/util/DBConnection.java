package com.banking.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DBConnection {
    private static final Logger LOG = Logger.getLogger(DBConnection.class.getName());
    private static String URL;
    private static String USERNAME;
    private static String PASSWORD;
    private static volatile String configError;

    static {
        try {
            loadProperties();
            Class.forName("com.mysql.cj.jdbc.Driver");
            LOG.info("DBConnection initialized (driver loaded).");
        } catch (Exception e) {
            configError = "Driver/config init failed: " + e.getClass().getSimpleName();
            LOG.log(Level.SEVERE, "DBConnection initialization failed (no secrets logged)", e);
        }
    }

    private static void loadProperties() {
        Properties props = new Properties();
        String envUrl = trim(System.getenv("DB_URL"));
        String envUser = trim(System.getenv("DB_USERNAME"));
        String envPass = System.getenv("DB_PASSWORD");
        try (java.io.InputStreamReader reader = new java.io.InputStreamReader(
                new FileInputStream("C:/Banking-Transaction-System/config/db.properties"), java.nio.charset.StandardCharsets.UTF_8)) {
            props.load(reader);
            URL = firstNonBlank(envUrl, trim(props.getProperty("db.url")));
            USERNAME = firstNonBlank(envUser, trim(props.getProperty("db.username")));
            PASSWORD = envPass != null ? envPass : props.getProperty("db.password"); // never log
        } catch (IOException e) {
            configError = "Could not read config/db.properties; using environment/defaults: " + e.getClass().getSimpleName();
            LOG.log(Level.WARNING, configError, e);
            URL = envUrl;
            USERNAME = envUser;
            PASSWORD = envPass;
        }
        if (URL == null || URL.isEmpty()) {
            URL = "jdbc:mysql://localhost:3306/banking?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }
        if (USERNAME == null || USERNAME.isEmpty()) {
            USERNAME = "root";
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isEmpty()) return a;
        if (b != null && !b.isEmpty()) return b;
        return null;
    }

    private static String trim(String v) { return v == null ? null : v.trim(); }

    public static Connection getConnection() throws SQLException {
        try {
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (SQLException e) {
            LOG.log(Level.WARNING, "DB connection failed (SQLState=" + e.getSQLState() + ", code=" + e.getErrorCode()
                    + ", urlHostDb=" + safeUrl(URL) + ", user=" + USERNAME + ")", e);
            throw e;
        }
    }

    /** URL without credentials/query secrets for logging. */
    private static String safeUrl(String url) {
        if (url == null) return "null";
        int q = url.indexOf('?');
        return q >= 0 ? url.substring(0, q) : url;
    }

    public static String getConfigError() { return configError; }
}
