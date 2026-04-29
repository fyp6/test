package com.mycompany.project.util;

import java.sql.Connection;

public class TestDBConnection {
    public static void main(String[] args) {
        try (Connection conn = DBUtil.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("OK: Connected to database");
            } else {
                System.err.println("FAIL: Connection is null or closed");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("ERROR: Could not connect");
            e.printStackTrace();
            System.exit(1);
        }
    }
}
