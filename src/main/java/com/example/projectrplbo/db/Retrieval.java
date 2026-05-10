package com.example.projectrplbo.db;

import com.example.projectrplbo.model.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Retrieval {

    private final Connection conn;

    public Retrieval() {
        this.conn = DatabaseConnector.getInstance().getConnection();
    }

    public List<Menu> getAllMenu() {
        List<Menu> list = new ArrayList<>();
        String sql = "SELECT id_menu, nama_menu, kategori, harga, stok FROM Menu ORDER BY kategori, nama_menu";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapMenu(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllMenu: " + e.getMessage());
        }
        return list;
    }

    public List<Menu> getMenuByKategori(String kategori) {
        List<Menu> list = new ArrayList<>();
        String sql = "SELECT id_menu, nama_menu, kategori, harga, stok FROM Menu WHERE LOWER(kategori) = LOWER(?) AND stok > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, kategori);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapMenu(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getMenuByKategori: " + e.getMessage());
        }
        return list;
    }

    public Menu getMenuById(String idMenu) {
        String sql = "SELECT id_menu, nama_menu, kategori, harga, stok FROM Menu WHERE id_menu = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idMenu.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return mapMenu(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getMenuById: " + e.getMessage());
        }
        return null;
    }

    public int checkStok(String idMenu) {
        String sql = "SELECT stok FROM Menu WHERE id_menu = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idMenu.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt("stok");
        } catch (SQLException e) {
            System.err.println("Error checkStok: " + e.getMessage());
        }
        return -1;
    }

    public boolean kurangiStok(String idMenu, int jumlah) {
        String sql = "UPDATE Menu SET stok = stok - ? WHERE id_menu = ? AND stok >= ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, jumlah);
            ps.setString(2, idMenu.toUpperCase());
            ps.setInt(3, jumlah);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error kurangiStok: " + e.getMessage());
        }
        return false;
    }

    public JadwalOperasional getJadwalAktif() {
        String sql = "SELECT id_jadwal, jam_buka, jam_tutup, hari FROM Jadwal_Operasional ORDER BY id_jadwal LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return mapJadwal(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getJadwalAktif: " + e.getMessage());
        }
        return new JadwalOperasional(0, LocalTime.of(9, 0), LocalTime.of(21, 0), "Setiap Hari");
    }

    public boolean savePesanan(Pesanan pesanan) {
        String sqlPesanan = "INSERT INTO Pesanan (id_pesanan, tgl_pesanan, total_harga, status_pesanan, id_pengguna) VALUES (?,?,?,?,?)";
        String sqlDetail  = "INSERT INTO Detail_Pesanan (id_pesanan, id_menu, jumlah, subtotal) VALUES (?,?,?,?)";
        try {
            conn.setAutoCommit(false);
            String newId = generatePesananId();
            pesanan.setIdPesanan(newId);
            try (PreparedStatement ps = conn.prepareStatement(sqlPesanan)) {
                ps.setString(1, pesanan.getIdPesanan());
                ps.setString(2, pesanan.getTanggal().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                ps.setDouble(3, pesanan.getTotalHarga());
                ps.setString(4, pesanan.getStatus());
                ps.setString(5, pesanan.getIdPengguna());
                ps.executeUpdate();
            }
            for (Pesanan.DetailPesanan detail : pesanan.getDetails()) {
                try (PreparedStatement pd = conn.prepareStatement(sqlDetail)) {
                    pd.setString(1, pesanan.getIdPesanan());
                    pd.setString(2, detail.getIdMenu());
                    pd.setInt(3, detail.getJumlah());
                    pd.setDouble(4, detail.getSubtotal());
                    pd.executeUpdate();
                }
                kurangiStok(detail.getIdMenu(), detail.getJumlah());
            }
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            System.err.println("Error savePesanan: " + e.getMessage());
            try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) {  }
        }
        return false;
    }

    public List<Pesanan> getPesananByUser(String idPengguna) {
        List<Pesanan> list = new ArrayList<>();
        String sql = """
            SELECT id_pesanan, tgl_pesanan, total_harga, status_pesanan, id_pengguna
            FROM Pesanan WHERE id_pengguna = ?
            ORDER BY tgl_pesanan DESC LIMIT 10
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPengguna);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(mapPesanan(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error getPesananByUser: " + e.getMessage());
        }
        return list;
    }

    public List<Pesanan.DetailPesanan> getDetailPesanan(String idPesanan) {
        List<Pesanan.DetailPesanan> list = new ArrayList<>();
        String sql = """
            SELECT dp.id_pesanan, dp.id_menu, m.nama_menu, dp.jumlah, m.harga, dp.subtotal
            FROM Detail_Pesanan dp
            JOIN Menu m ON dp.id_menu = m.id_menu
            WHERE dp.id_pesanan = ?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPesanan);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Pesanan.DetailPesanan d = new Pesanan.DetailPesanan(
                    rs.getString("id_pesanan"),
                    rs.getString("id_menu"),
                    rs.getInt("jumlah"),
                    rs.getDouble("subtotal")
                );
                d.setNamaMenu(rs.getString("nama_menu"));
                d.setHargaSatuan(rs.getDouble("harga"));
                list.add(d);
            }
        } catch (SQLException e) {
            System.err.println("Error getDetailPesanan: " + e.getMessage());
        }
        return list;
    }

    public Pengguna loginPengguna(String username, String password) {
        ensureStatusColumn();
        String passwordHash = java.util.Base64.getEncoder().encodeToString(password.getBytes());
        String sql = """
            SELECT a.id_akun, a.username, a.password, p.id_pengguna, p.nama_lengkap,
                   p.jalan, p.kota, p.provinsi, p.kode_pos, p.no_telepon, p.email
            FROM Akun a JOIN Pengguna p ON a.id_akun = p.id_akun
            WHERE a.username = ? AND a.password = ? AND a.role = 'Pengguna'
              AND COALESCE(a.status, 'Aktif') <> 'Terblokir'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String alamat = rs.getString("jalan") + ", " + rs.getString("kota") + ", "
                    + rs.getString("provinsi") + " " + rs.getString("kode_pos");
                return new Pengguna(
                    rs.getString("id_akun"), rs.getString("username"), rs.getString("password"),
                    rs.getString("id_pengguna"), rs.getString("nama_lengkap"),
                    alamat, rs.getString("email"), rs.getString("no_telepon")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error loginPengguna: " + e.getMessage());
        }
        return null;
    }

    public boolean isPenggunaTerblokir(String username, String password) {
        ensureStatusColumn();
        String passwordHash = java.util.Base64.getEncoder().encodeToString(password.getBytes());
        String sql = """
            SELECT COALESCE(a.status, 'Aktif') AS status
            FROM Akun a
            JOIN Pengguna p ON a.id_akun = p.id_akun
            WHERE a.username = ? AND a.password = ? AND a.role = 'Pengguna'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ResultSet rs = ps.executeQuery();
            return rs.next() && "Terblokir".equalsIgnoreCase(rs.getString("status"));
        } catch (SQLException e) {
            System.err.println("Error isPenggunaTerblokir: " + e.getMessage());
        }
        return false;
    }

    public Admin loginAdmin(String username, String password) {
        String passwordHash = java.util.Base64.getEncoder().encodeToString(password.getBytes());
        String sql = """
            SELECT a.id_akun, a.username, a.password, ad.id_admin, ad.nama_admin
            FROM Akun a JOIN Admin ad ON a.id_akun = ad.id_akun
            WHERE a.username = ? AND a.password = ? AND a.role = 'Admin'
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, passwordHash);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Admin(
                    rs.getString("id_akun"), rs.getString("username"), rs.getString("password"),
                    rs.getString("id_admin"), rs.getString("nama_admin")
                );
            }
        } catch (SQLException e) {
            System.err.println("Error loginAdmin: " + e.getMessage());
        }
        return null;
    }

    public boolean registerPengguna(String nama, String jalan, String kota, String provinsi,
                                    String kodePos, String noTelepon, String email,
                                    String username, String password) {
        String idAkun = generateAkunId();
        String idPengguna = generatePenggunaId();
        String passwordHash = java.util.Base64.getEncoder().encodeToString(password.getBytes());
        try {
            conn.setAutoCommit(false);
            String sqlAkun = "INSERT INTO Akun (id_akun, username, password, role) VALUES (?,?,?,'Pengguna')";
            try (PreparedStatement ps = conn.prepareStatement(sqlAkun)) {
                ps.setString(1, idAkun);
                ps.setString(2, username);
                ps.setString(3, passwordHash);
                ps.executeUpdate();
            }
            String sqlPengguna = "INSERT INTO Pengguna (id_pengguna, nama_lengkap, jalan, kota, provinsi, kode_pos, no_telepon, email, id_akun) VALUES (?,?,?,?,?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sqlPengguna)) {
                ps.setString(1, idPengguna);
                ps.setString(2, nama);
                ps.setString(3, jalan);
                ps.setString(4, kota);
                ps.setString(5, provinsi);
                ps.setString(6, kodePos);
                ps.setString(7, noTelepon);
                ps.setString(8, email);
                ps.setString(9, idAkun);
                ps.executeUpdate();
            }
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            System.err.println("Error registerPengguna: " + e.getMessage());
            try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) {  }
        }
        return false;
    }

    public boolean updateProfil(String idAkun, String idPengguna, String nama, String jalan,
                                String kota, String provinsi, String kodePos,
                                String noTelepon, String email, String passwordBaru) {
        try {
            conn.setAutoCommit(false);
            String sqlP = "UPDATE Pengguna SET nama_lengkap=?, jalan=?, kota=?, provinsi=?, kode_pos=?, no_telepon=?, email=? WHERE id_pengguna=?";
            try (PreparedStatement ps = conn.prepareStatement(sqlP)) {
                ps.setString(1, nama); ps.setString(2, jalan); ps.setString(3, kota);
                ps.setString(4, provinsi); ps.setString(5, kodePos);
                ps.setString(6, noTelepon); ps.setString(7, email);
                ps.setString(8, idPengguna);
                ps.executeUpdate();
            }
            if (passwordBaru != null && !passwordBaru.isBlank()) {
                String hash = java.util.Base64.getEncoder().encodeToString(passwordBaru.getBytes());
                String sqlA = "UPDATE Akun SET password=? WHERE id_akun=?";
                try (PreparedStatement ps = conn.prepareStatement(sqlA)) {
                    ps.setString(1, hash); ps.setString(2, idAkun);
                    ps.executeUpdate();
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            System.err.println("Error updateProfil: " + e.getMessage());
            try { conn.rollback(); conn.setAutoCommit(true); } catch (SQLException ex) {  }
        }
        return false;
    }

    public boolean isUsernameTaken(String username) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM Akun WHERE username=?")) {
            ps.setString(1, username);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public boolean isEmailTaken(String email) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM Pengguna WHERE email=?")) {
            ps.setString(1, email);
            return ps.executeQuery().next();
        } catch (SQLException e) { return false; }
    }

    public boolean verifikasiPassword(String idAkun, String passwordPlain) {
        String passwordHash = java.util.Base64.getEncoder().encodeToString(passwordPlain.getBytes());
        String sql = "SELECT 1 FROM Akun WHERE id_akun = ? AND password = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idAkun);
            ps.setString(2, passwordHash);
            return ps.executeQuery().next();
        } catch (SQLException e) {
            System.err.println("Error verifikasiPassword: " + e.getMessage());
        }
        return false;
    }

        public boolean resetPasswordByEmail(String email, String newPassword) {
        String sql = """
            UPDATE Akun SET password=?
            WHERE id_akun = (SELECT id_akun FROM Pengguna WHERE email=?)
        """;
        String hash = java.util.Base64.getEncoder().encodeToString(newPassword.getBytes());
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, hash); ps.setString(2, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error resetPassword: " + e.getMessage());
        }
        return false;
    }

    public List<String> getPatternsByIntent(String intent) {
        List<String> list = new ArrayList<>();
        String sql = "SELECT pola FROM Pattern_Intent WHERE intent = ? ORDER BY id_pattern";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, intent);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("pola").toLowerCase().trim());
            }
        } catch (SQLException e) {
            System.err.println("Error getPatternsByIntent: " + e.getMessage());
        }
        return list;
    }

    public List<String> getAllIntents() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT intent FROM Pattern_Intent";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("intent"));
        } catch (SQLException e) {
            System.err.println("Error getAllIntents: " + e.getMessage());
        }
        return list;
    }

    public List<String> getAllKategori() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT DISTINCT kategori FROM Menu ORDER BY kategori";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(rs.getString("kategori"));
        } catch (SQLException e) {
            System.err.println("Error getAllKategori: " + e.getMessage());
        }
        return list;
    }

    public List<Pesanan> getAllPesananTerbaru(int limit) {
        List<Pesanan> list = new ArrayList<>();
        String sql = "SELECT id_pesanan, tgl_pesanan, total_harga, status_pesanan, id_pengguna "
            + "FROM Pesanan ORDER BY tgl_pesanan DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapPesanan(rs));
        } catch (SQLException e) {
            System.err.println("Error getAllPesananTerbaru: " + e.getMessage());
        }
        return list;
    }

    public String getNamaPelangganByPesanan(String idPengguna) {
        String sql = "SELECT nama_lengkap FROM Pengguna WHERE id_pengguna = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idPengguna);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("nama_lengkap");
        } catch (SQLException e) {
            System.err.println("Error getNamaPelanggan: " + e.getMessage());
        }
        return "-";
    }

    public String getMenuTerlaris() {
        String sql = """
            SELECT m.nama_menu, SUM(dp.jumlah) AS total_terjual
            FROM Detail_Pesanan dp
            JOIN Menu m ON dp.id_menu = m.id_menu
            GROUP BY dp.id_menu ORDER BY total_terjual DESC LIMIT 1
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getString("nama_menu");
        } catch (SQLException e) {
            System.err.println("Error getMenuTerlaris: " + e.getMessage());
        }
        return "-";
    }

    public boolean tambahMenu(String idMenu, String namaMenu, String kategori,
                              double harga, int stok) {
        String sql = "INSERT INTO Menu (id_menu, nama_menu, kategori, harga, stok) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idMenu);
            ps.setString(2, namaMenu);
            ps.setString(3, kategori);
            ps.setDouble(4, harga);
            ps.setInt(5, stok);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error tambahMenu: " + e.getMessage());
        }
        return false;
    }

    public boolean updateMenu(String idMenu, String namaMenu, String kategori,
                              double harga, int stok) {
        String sql = "UPDATE Menu SET nama_menu=?, kategori=?, harga=?, stok=? WHERE id_menu=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, namaMenu);
            ps.setString(2, kategori);
            ps.setDouble(3, harga);
            ps.setInt(4, stok);
            ps.setString(5, idMenu);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updateMenu: " + e.getMessage());
        }
        return false;
    }

    public boolean hapusMenu(String idMenu) {
        String sql = "DELETE FROM Menu WHERE id_menu = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idMenu);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error hapusMenu: " + e.getMessage());
        }
        return false;
    }

    public boolean setStok(String idMenu, int stokBaru) {
        String sql = "UPDATE Menu SET stok = ? WHERE id_menu = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stokBaru);
            ps.setString(2, idMenu);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error setStok: " + e.getMessage());
        }
        return false;
    }


    public boolean updateJadwalOperasional(LocalTime jamBuka, LocalTime jamTutup, String hari) {
        String sqlCheck = "SELECT id_jadwal FROM Jadwal_Operasional ORDER BY id_jadwal LIMIT 1";
        try (PreparedStatement psCheck = conn.prepareStatement(sqlCheck)) {
            ResultSet rs = psCheck.executeQuery();
            if (rs.next()) {
                int id = rs.getInt("id_jadwal");
                String sqlUp = "UPDATE Jadwal_Operasional SET jam_buka=?, jam_tutup=?, hari=? WHERE id_jadwal=?";
                try (PreparedStatement ps = conn.prepareStatement(sqlUp)) {
                    ps.setString(1, jamBuka.toString());
                    ps.setString(2, jamTutup.toString());
                    ps.setString(3, hari);
                    ps.setInt(4, id);
                    return ps.executeUpdate() > 0;
                }
            } else {
                String sqlIns = "INSERT INTO Jadwal_Operasional (jam_buka, jam_tutup, hari) VALUES (?,?,?)";
                try (PreparedStatement ps = conn.prepareStatement(sqlIns)) {
                    ps.setString(1, jamBuka.toString());
                    ps.setString(2, jamTutup.toString());
                    ps.setString(3, hari);
                    return ps.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updateJadwalOperasional: " + e.getMessage());
        }
        return false;
    }

    public List<PenggunaInfo> getAllPenggunaInfo() {
        List<PenggunaInfo> list = new ArrayList<>();
        String sql = """
            SELECT a.id_akun, p.id_pengguna, p.nama_lengkap, a.username,
                   p.email, p.no_telepon,
                   COALESCE(a.status, 'Aktif') AS status
            FROM Akun a
            JOIN Pengguna p ON a.id_akun = p.id_akun
            WHERE a.role = 'Pengguna'
            ORDER BY p.id_pengguna DESC
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            int urut = 1;
            while (rs.next()) {
                String tgl = "01 Jan 2024";
                list.add(new PenggunaInfo(
                    rs.getString("id_akun"),
                    rs.getString("id_pengguna"),
                    rs.getString("nama_lengkap"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("no_telepon"),
                    tgl,
                    rs.getString("status")
                ));
                urut++;
            }
        } catch (SQLException e) {
            System.err.println("Error getAllPenggunaInfo (retrying without status): " + e.getMessage());
            list = getAllPenggunaInfoFallback();
        }
        return list;
    }

    private List<PenggunaInfo> getAllPenggunaInfoFallback() {
        List<PenggunaInfo> list = new ArrayList<>();
        String sql = """
            SELECT a.id_akun, p.id_pengguna, p.nama_lengkap, a.username,
                   p.email, p.no_telepon
            FROM Akun a
            JOIN Pengguna p ON a.id_akun = p.id_akun
            WHERE a.role = 'Pengguna'
            ORDER BY p.id_pengguna DESC
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new PenggunaInfo(
                    rs.getString("id_akun"),
                    rs.getString("id_pengguna"),
                    rs.getString("nama_lengkap"),
                    rs.getString("username"),
                    rs.getString("email"),
                    rs.getString("no_telepon"),
                    "01 Jan 2024",
                    "Aktif"
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllPenggunaInfoFallback: " + e.getMessage());
        }
        return list;
    }

    public boolean updateStatusAkun(String idAkun, String status) {
        ensureStatusColumn();
        String sql = "UPDATE Akun SET status = ? WHERE id_akun = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, idAkun);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updateStatusAkun: " + e.getMessage());
        }
        return false;
    }

    public boolean hapusAkun(String idAkun) {
        String sql = "DELETE FROM Akun WHERE id_akun = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idAkun);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error hapusAkun: " + e.getMessage());
        }
        return false;
    }
    private void ensureStatusColumn() {
        try (PreparedStatement ps = conn.prepareStatement(
                "ALTER TABLE Akun ADD COLUMN status VARCHAR(20) DEFAULT 'Aktif'")) {
            ps.executeUpdate();
        } catch (SQLException ignored) {
        }
    }

        public Response createResponse(int status, String msg, Object data) {
        return new Response(status, msg, data);
    }

    private Menu mapMenu(ResultSet rs) throws SQLException {
        return new Menu(
            rs.getString("id_menu"),
            rs.getString("nama_menu"),
            rs.getString("kategori"),
            rs.getDouble("harga"),
            rs.getInt("stok")
        );
    }

    private Pesanan mapPesanan(ResultSet rs) throws SQLException {
        String tglStr = rs.getString("tgl_pesanan");
        LocalDateTime tgl;
        try {
            tgl = LocalDateTime.parse(tglStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            tgl = LocalDateTime.now();
        }
        return new Pesanan(
            rs.getString("id_pesanan"), tgl,
            rs.getDouble("total_harga"), rs.getString("status_pesanan"),
            rs.getString("id_pengguna")
        );
    }

    private JadwalOperasional mapJadwal(ResultSet rs) throws SQLException {
        LocalTime buka = LocalTime.parse(rs.getString("jam_buka"));
        LocalTime tutup = LocalTime.parse(rs.getString("jam_tutup"));
        return new JadwalOperasional(rs.getInt("id_jadwal"), buka, tutup, rs.getString("hari"));
    }

    public String generateAkunId() {
        String sql = "SELECT MAX(id_akun) FROM Akun WHERE id_akun LIKE 'AKN%'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String maxId = rs.getString(1);
                if (maxId != null && maxId.matches("AKN\\d{5}")) {
                    int nextNum = Integer.parseInt(maxId.substring(3)) + 1;
                    return String.format("AKN%05d", nextNum);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generateAkunId: " + e.getMessage());
        }
        return "AKN00002";
    }

    public String generatePenggunaId() {
        String sql = "SELECT MAX(id_pengguna) FROM Pengguna WHERE id_pengguna LIKE 'PNG%'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String maxId = rs.getString(1);
                if (maxId != null && maxId.matches("PNG\\d{5}")) {
                    int nextNum = Integer.parseInt(maxId.substring(3)) + 1;
                    return String.format("PNG%05d", nextNum);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generatePenggunaId: " + e.getMessage());
        }
        return "PNG00001";
    }

    public static String generateId(String prefix) {
        int rand = (int)(Math.random() * 90000) + 10000;
        return prefix + rand;
    }

    public boolean updateStatusPesanan(String idPesanan, String statusBaru) {
        String sql = "UPDATE Pesanan SET status_pesanan = ? WHERE id_pesanan = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, statusBaru);
            ps.setString(2, idPesanan);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updateStatusPesanan: " + e.getMessage());
        }
        return false;
    }

    public String generatePesananId() {
        String sql = "SELECT MAX(id_pesanan) FROM Pesanan";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String maxId = rs.getString(1);
                if (maxId != null && maxId.matches("P\\d{7}")) {
                    int nextNum = Integer.parseInt(maxId.substring(1)) + 1;
                    return String.format("P%07d", nextNum);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generatePesananId: " + e.getMessage());
        }
        return "P0000001";
    }
}
