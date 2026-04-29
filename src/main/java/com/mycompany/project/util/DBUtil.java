package com.mycompany.project.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DBUtil {
    private static final String[] DB_NAMES = {"cchc_clinic", "cchc_clinic_db"};
    private static final String URL_PREFIX = "jdbc:mysql://localhost:3306/";
    private static final String URL_SUFFIX = "?useSSL=false&serverTimezone=Asia/Hong_Kong";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        SQLException last = null;
        for (String dbName : DB_NAMES) {
            String url = URL_PREFIX + dbName + URL_SUFFIX;
            try {
                return DriverManager.getConnection(url, USER, PASSWORD);
            } catch (SQLException ex) {
                last = ex;
            }
        }
        throw last;
    }
}
