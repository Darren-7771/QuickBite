package com.example.projectrplbo.db;

import java.sql.*;
import java.time.LocalTime;

public class DatabaseConnector {

    private static final String DB_PATH = "QuickBite.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    private static DatabaseConnector instance;
    private Connection connection;

    private DatabaseConnector() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            initializeDatabase();
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Gagal menginisialisasi database: " + e.getMessage(), e);
        }
    }

    public static synchronized DatabaseConnector getInstance() {
        if (instance == null) {
            instance = new DatabaseConnector();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("PRAGMA foreign_keys = ON;");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Gagal mendapatkan koneksi: " + e.getMessage(), e);
        }
        return connection;
    }

    private void initializeDatabase() throws SQLException {
        try (Statement stmt = connection.createStatement()) {

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Akun (
                    id_akun   CHAR(8)      PRIMARY KEY,
                    username  VARCHAR(100) NOT NULL UNIQUE,
                    password  VARCHAR(100) NOT NULL,
                    role      VARCHAR(20)  NOT NULL DEFAULT 'Pengguna'
                              CHECK(role IN ('Pengguna', 'Admin'))
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Pengguna (
                    id_pengguna  CHAR(8)      PRIMARY KEY,
                    nama_lengkap VARCHAR(150) NOT NULL,
                    jalan        VARCHAR(150) NOT NULL,
                    kota         VARCHAR(100) NOT NULL,
                    provinsi     VARCHAR(100) NOT NULL,
                    kode_pos     CHAR(5),
                    no_telepon   VARCHAR(15)  NOT NULL UNIQUE,
                    email        VARCHAR(100) NOT NULL UNIQUE,
                    id_akun      CHAR(8),
                    FOREIGN KEY (id_akun) REFERENCES Akun(id_akun) ON DELETE CASCADE
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Admin (
                    id_admin    CHAR(8)      PRIMARY KEY,
                    nama_admin  VARCHAR(150) NOT NULL,
                    id_akun     CHAR(8),
                    FOREIGN KEY (id_akun) REFERENCES Akun(id_akun) ON DELETE CASCADE
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Menu (
                    id_menu    CHAR(8)      PRIMARY KEY,
                    nama_menu  VARCHAR(100) NOT NULL,
                    kategori   VARCHAR(50)  NOT NULL,
                    harga      DOUBLE       NOT NULL,
                    stok       INT          NOT NULL DEFAULT 0
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Kategori (
                    id_kategori CHAR(8)     PRIMARY KEY,
                    kategori    VARCHAR(50) NOT NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Kategori_Menu (
                    id_menu      CHAR(8) NOT NULL,
                    id_kategori  CHAR(8) NOT NULL,
                    PRIMARY KEY (id_menu, id_kategori),
                    FOREIGN KEY (id_menu)     REFERENCES Menu(id_menu)         ON DELETE CASCADE,
                    FOREIGN KEY (id_kategori) REFERENCES Kategori(id_kategori) ON DELETE CASCADE
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Pesanan (
                    id_pesanan     CHAR(8)     PRIMARY KEY,
                    tgl_pesanan    DATETIME    NOT NULL DEFAULT (datetime('now', 'localtime')),
                    total_harga    DOUBLE      NOT NULL DEFAULT 0,
                    status_pesanan VARCHAR(20) NOT NULL DEFAULT 'MENUNGGU',
                    id_pengguna    CHAR(8),
                    FOREIGN KEY (id_pengguna) REFERENCES Pengguna(id_pengguna) ON DELETE SET NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Detail_Pesanan (
                    id_pesanan CHAR(8) NOT NULL,
                    id_menu    CHAR(8) NOT NULL,
                    jumlah     INT    NOT NULL DEFAULT 1,
                    subtotal   DOUBLE NOT NULL DEFAULT 0,
                    PRIMARY KEY (id_pesanan, id_menu),
                    FOREIGN KEY (id_pesanan) REFERENCES Pesanan(id_pesanan) ON DELETE CASCADE,
                    FOREIGN KEY (id_menu)    REFERENCES Menu(id_menu)       ON DELETE RESTRICT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Jadwal_Operasional (
                    id_jadwal  INTEGER     PRIMARY KEY AUTOINCREMENT,
                    jam_buka   TIME        NOT NULL,
                    jam_tutup  TIME        NOT NULL,
                    hari       VARCHAR(20) NOT NULL DEFAULT 'Setiap Hari'
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS Detail_Jadwal (
                    id_admin   CHAR(8) NOT NULL,
                    id_jadwal  INTEGER NOT NULL,
                    PRIMARY KEY (id_admin, id_jadwal),
                    FOREIGN KEY (id_admin)  REFERENCES Admin(id_admin)                    ON DELETE CASCADE,
                    FOREIGN KEY (id_jadwal) REFERENCES Jadwal_Operasional(id_jadwal)      ON DELETE CASCADE
                );
            """);

            seedInitialData(stmt);
        }
    }

    private void seedInitialData(Statement stmt) throws SQLException {
        ResultSet rsJadwal = stmt.executeQuery("SELECT COUNT(*) FROM Jadwal_Operasional");
        if (rsJadwal.next() && rsJadwal.getInt(1) == 0) {
            stmt.execute("INSERT INTO Jadwal_Operasional (jam_buka, jam_tutup, hari) VALUES ('09:00', '21:00', 'Setiap Hari');");
        }

        ResultSet rsKat = stmt.executeQuery("SELECT COUNT(*) FROM Kategori");
        if (rsKat.next() && rsKat.getInt(1) == 0) {
            stmt.execute("INSERT INTO Kategori VALUES ('KAT00001', 'Ayam');");
            stmt.execute("INSERT INTO Kategori VALUES ('KAT00002', 'Burger');");
            stmt.execute("INSERT INTO Kategori VALUES ('KAT00003', 'Minuman');");
            stmt.execute("INSERT INTO Kategori VALUES ('KAT00004', 'Snack');");
        }

        ResultSet rsMenu = stmt.executeQuery("SELECT COUNT(*) FROM Menu");
        if (rsMenu.next() && rsMenu.getInt(1) == 0) {
            stmt.execute("INSERT INTO Menu VALUES ('MKN00001', 'Fried Chicken', 'Ayam', 35000, 50);");
            stmt.execute("INSERT INTO Menu VALUES ('MKN00002', 'Burger Beef Deluxe', 'Burger', 28000, 30);");
            stmt.execute("INSERT INTO Menu VALUES ('MKN00003', 'Chicken Burger', 'Burger', 25000, 25);");
            stmt.execute("INSERT INTO Menu VALUES ('MKN00004', 'Spicy Chicken', 'Ayam', 38000, 40);");
            stmt.execute("INSERT INTO Menu VALUES ('MNM00001', 'Coca Cola', 'Minuman', 12000, 100);");
            stmt.execute("INSERT INTO Menu VALUES ('MNM00002', 'Es Teh Manis', 'Minuman', 8000, 80);");
            stmt.execute("INSERT INTO Menu VALUES ('SNK00001', 'French Fries', 'Snack', 15000, 60);");
            stmt.execute("INSERT INTO Menu VALUES ('SNK00002', 'Onion Rings', 'Snack', 18000, 45);");
            stmt.execute("INSERT INTO Kategori_Menu VALUES ('MKN00001', 'KAT00001');");
            stmt.execute("INSERT INTO Kategori_Menu VALUES ('MKN00002', 'KAT00002');");
            stmt.execute("INSERT INTO Kategori_Menu VALUES ('MKN00003', 'KAT00002');");
            stmt.execute("INSERT INTO Kategori_Menu VALUES ('MKN00004', 'KAT00001');");
            stmt.execute("INSERT INTO Kategori_Menu VALUES ('MNM00001', 'KAT00003');");
            stmt.execute("INSERT INTO Kategori_Menu VALUES ('MNM00002', 'KAT00003');");
            stmt.execute("INSERT INTO Kategori_Menu VALUES ('SNK00001', 'KAT00004');");
            stmt.execute("INSERT INTO Kategori_Menu VALUES ('SNK00002', 'KAT00004');");
        }

        ResultSet rsAdmin = stmt.executeQuery("SELECT COUNT(*) FROM Akun WHERE role='Admin'");
        if (rsAdmin.next() && rsAdmin.getInt(1) == 0) {
            String passwordHash = java.util.Base64.getEncoder().encodeToString("admin123".getBytes());
            stmt.execute("INSERT INTO Akun VALUES ('AKN00001', 'admin', '" + passwordHash + "', 'Admin');");
            stmt.execute("INSERT INTO Admin VALUES ('ADM00001', 'Administrator', 'AKN00001');");
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error menutup koneksi: " + e.getMessage());
        }
    }
}