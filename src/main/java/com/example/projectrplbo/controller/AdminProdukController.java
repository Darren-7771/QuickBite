package com.example.projectrplbo.controller;

import com.example.projectrplbo.MainApp;
import com.example.projectrplbo.db.Retrieval;
import com.example.projectrplbo.model.Admin;
import com.example.projectrplbo.model.Menu;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AdminProdukController {

    @FXML private Label lblNamaAdmin;
    @FXML private Label lblTotalProduk;
    @FXML private Label lblJmlStokRendah;
    @FXML private VBox  vboxStokRendahList;
    @FXML private ComboBox<String> cbPilihProduk;
    @FXML private TextField tfJumlahStok;
    @FXML private TextField tfCariProduk;
    @FXML private VBox vboxProdukRows;
    @FXML private Label lblPagingInfo;
    @FXML private Button btnPrev;
    @FXML private Button btnNext;
    @FXML private HBox hboxPageNumbers;
    @FXML private VBox panelFormMenu;
    @FXML private Label lblFormJudul;
    @FXML private TextField tfFormIdMenu;
    @FXML private TextField tfFormNama;
    @FXML private TextField tfFormKategori;
    @FXML private TextField tfFormHarga;
    @FXML private TextField tfFormStok;
    @FXML private Label lblFormError;

    private Admin admin;
    private final Retrieval retrieval = new Retrieval();

    private List<Menu> semuaMenu = List.of();
    private List<Menu> menuTerfilter = List.of();
    private static final int PER_HALAMAN = 5;
    private static final int STOK_RENDAH_THRESHOLD = 5;
    private int halamanSaat = 0;

    private Menu menuSedangDiedit = null;

    public void initData(Admin admin) {
        this.admin = admin;
        lblNamaAdmin.setText(admin.getNamaAdmin());
        Platform.runLater(this::muatSemua);
    }

    @FXML
    public void initialize() {
        if (panelFormMenu != null) {
            panelFormMenu.setVisible(false);
            panelFormMenu.setManaged(false);
        }
        if (lblFormError != null) {
            lblFormError.setVisible(false);
            lblFormError.setManaged(false);
        }
    }

    private void muatSemua() {
        semuaMenu = retrieval.getAllMenu();
        menuTerfilter = semuaMenu;
        halamanSaat = 0;
        muatComboBox();
        muatPanelStokRendah();
        renderHalaman();
    }

    private void muatComboBox() {
        cbPilihProduk.getItems().clear();
        for (Menu m : semuaMenu) {
            cbPilihProduk.getItems().add(m.getIdMenu() + " - " + m.getNamaMenu());
        }
    }

    private void muatPanelStokRendah() {
        List<Menu> rendah = semuaMenu.stream()
                .filter(m -> m.getStok() <= STOK_RENDAH_THRESHOLD)
                .sorted(Comparator.comparingInt(Menu::getStok))
                .collect(Collectors.toList());

        lblJmlStokRendah.setText(rendah.size() + " produk memiliki stok rendah atau habis");
        lblTotalProduk.setText(String.valueOf(semuaMenu.size()));

        vboxStokRendahList.getChildren().clear();
        for (Menu m : rendah) {
            String teks = m.getStok() == 0
                    ? m.getNamaMenu() + " (stok habis)"
                    : m.getNamaMenu() + " (" + m.getStok() + " tersisa)";
            Label lbl = new Label("• " + teks);
            lbl.getStyleClass().add("admin-stok-item");
            lbl.setWrapText(true);
            vboxStokRendahList.getChildren().add(lbl);
        }
    }

    private void renderHalaman() {
        vboxProdukRows.getChildren().clear();
        int total = menuTerfilter.size();
        int totalHalaman = (int) Math.ceil((double) total / PER_HALAMAN);

        int dari = halamanSaat * PER_HALAMAN;
        int sampai = Math.min(dari + PER_HALAMAN, total);
        List<Menu> slice = menuTerfilter.subList(dari, sampai);

        for (Menu m : slice) {
            vboxProdukRows.getChildren().add(buatBarisProduk(m));
        }

        lblPagingInfo.setText("Menampilkan " + (dari + 1) + "-" + sampai + " dari " + total + " produk");
        btnPrev.setDisable(halamanSaat == 0);
        btnNext.setDisable(halamanSaat >= totalHalaman - 1);


        if (hboxPageNumbers != null) {
            hboxPageNumbers.getChildren().clear();
            for (int i = 0; i < totalHalaman; i++) {
                final int page = i;
                Button btn = new Button(String.valueOf(i + 1));
                btn.getStyleClass().add(i == halamanSaat ? "admin-btn-paging-active" : "admin-btn-paging");
                btn.setOnAction(e -> { halamanSaat = page; renderHalaman(); });
                hboxPageNumbers.getChildren().add(btn);
            }
        }
    }

    private HBox buatBarisProduk(Menu m) {
        HBox row = new HBox(0);
        row.getStyleClass().add("admin-table-row");
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblId    = buatCel(m.getIdMenu(), "admin-cell-id", 110);
        Label lblNama  = buatCel(m.getNamaMenu(), "admin-cell-text", 160);
        HBox.setHgrow(lblNama, Priority.ALWAYS);
        Label lblKat   = buatCel(m.getKategori(), "admin-cell-text", 90);
        Label lblHarga = buatCel(String.format("Rp %,.0f", m.getHarga()), "admin-cell-text", 100);

        Label lblStok = new Label(String.valueOf(m.getStok()));
        lblStok.setMinWidth(60);
        if (m.getStok() == 0)          lblStok.getStyleClass().add("admin-stok-habis");
        else if (m.getStok() <= STOK_RENDAH_THRESHOLD) lblStok.getStyleClass().add("admin-stok-rendah-val");
        else                            lblStok.getStyleClass().add("admin-cell-text");

        String statusTeks = m.getStok() == 0 ? "Stok Habis"
                : m.getStok() <= STOK_RENDAH_THRESHOLD ? "Stok Rendah" : "Tersedia";
        Label lblStatus = new Label(statusTeks);
        lblStatus.getStyleClass().add("admin-status-" + statusCssKey(statusTeks));
        lblStatus.setMinWidth(100);

        Button btnEdit = new Button("✏");
        btnEdit.getStyleClass().add("admin-btn-icon-edit");
        btnEdit.setOnAction(e -> bukaFormEdit(m));

        Button btnHapus = new Button("🗑");
        btnHapus.getStyleClass().add("admin-btn-icon-hapus");
        btnHapus.setOnAction(e -> konfirmasiHapus(m));

        HBox aksiBox = new HBox(6, btnEdit, btnHapus);
        aksiBox.setAlignment(Pos.CENTER_LEFT);
        aksiBox.setMinWidth(70);

        row.getChildren().addAll(lblId, lblNama, lblKat, lblHarga, lblStok, lblStatus, aksiBox);
        return row;
    }

    @FXML
    private void handleTambahMenuBaru() {
        menuSedangDiedit = null;
        lblFormJudul.setText("Tambah Menu Baru");
        tfFormIdMenu.setEditable(true);
        bersihkanForm();
        tampilkanForm(true);
    }

    private void bukaFormEdit(Menu m) {
        menuSedangDiedit = m;
        lblFormJudul.setText("Edit Menu");
        tfFormIdMenu.setEditable(false);
        tfFormIdMenu.setText(m.getIdMenu());
        tfFormNama.setText(m.getNamaMenu());
        tfFormKategori.setText(m.getKategori());
        tfFormHarga.setText(String.valueOf((long) m.getHarga()));
        tfFormStok.setText(String.valueOf(m.getStok()));
        tampilkanForm(true);
    }

    @FXML
    private void handleSimpanForm() {
        String id      = tfFormIdMenu.getText().trim().toUpperCase();
        String nama    = tfFormNama.getText().trim();
        String kat     = tfFormKategori.getText().trim();
        String hargaStr= tfFormHarga.getText().trim();
        String stokStr = tfFormStok.getText().trim();

        if (id.isEmpty() || nama.isEmpty() || kat.isEmpty() || hargaStr.isEmpty() || stokStr.isEmpty()) {
            showFormError("Semua field wajib diisi.");
            return;
        }
        double harga; int stok;
        try { harga = Double.parseDouble(hargaStr); }
        catch (NumberFormatException e) { showFormError("Harga harus berupa angka."); return; }
        try { stok = Integer.parseInt(stokStr); }
        catch (NumberFormatException e) { showFormError("Stok harus berupa angka bulat."); return; }
        if (harga <= 0 || stok < 0) { showFormError("Harga > 0 dan stok >= 0."); return; }

        boolean ok;
        if (menuSedangDiedit == null) {
            ok = retrieval.tambahMenu(id, nama, kat, harga, stok);
        } else {
            ok = retrieval.updateMenu(id, nama, kat, harga, stok);
        }

        if (ok) {
            tampilkanForm(false);
            muatSemua();
        } else {
            showFormError("Gagal menyimpan. ID mungkin sudah dipakai.");
        }
    }

    @FXML
    private void handleBatalForm() {
        tampilkanForm(false);
    }

    private void tampilkanForm(boolean tampil) {
        panelFormMenu.setVisible(tampil);
        panelFormMenu.setManaged(tampil);
        if (!tampil) bersihkanForm();
    }

    private void bersihkanForm() {
        tfFormIdMenu.clear(); tfFormNama.clear();
        tfFormKategori.clear(); tfFormHarga.clear(); tfFormStok.clear();
        lblFormError.setVisible(false); lblFormError.setManaged(false);
    }

    private void showFormError(String msg) {
        lblFormError.setText(msg);
        lblFormError.setVisible(true);
        lblFormError.setManaged(true);
    }

    private void konfirmasiHapus(Menu m) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Konfirmasi Hapus");
        alert.setHeaderText("Hapus menu: " + m.getNamaMenu() + "?");
        alert.setContentText("Tindakan ini tidak bisa dibatalkan.");
        alert.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                boolean ok = retrieval.hapusMenu(m.getIdMenu());
                if (ok) muatSemua();
                else {
                    Alert err = new Alert(Alert.AlertType.ERROR,
                            "Gagal menghapus. Menu mungkin masih direferensikan oleh data pesanan.");
                    err.showAndWait();
                }
            }
        });
    }

    @FXML
    private void handleUpdateStok() {
        String pilihan = cbPilihProduk.getValue();
        String jumlahStr = tfJumlahStok.getText().trim();
        if (pilihan == null || jumlahStr.isEmpty()) return;

        String idMenu = pilihan.split(" - ")[0].trim();
        int jumlah;
        try { jumlah = Integer.parseInt(jumlahStr); }
        catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Jumlah stok harus berupa angka.").showAndWait();
            return;
        }
        if (jumlah < 0) {
            new Alert(Alert.AlertType.WARNING, "Jumlah stok tidak boleh negatif.").showAndWait();
            return;
        }

        boolean ok = retrieval.setStok(idMenu, jumlah);
        if (ok) {
            tfJumlahStok.clear();
            cbPilihProduk.setValue(null);
            muatSemua();
        } else {
            new Alert(Alert.AlertType.ERROR, "Gagal mengupdate stok.").showAndWait();
        }
    }

    @FXML
    private void handleCariProduk() {
        String kata = tfCariProduk.getText().trim().toLowerCase();
        if (kata.isEmpty()) {
            menuTerfilter = semuaMenu;
        } else {
            menuTerfilter = semuaMenu.stream()
                    .filter(m -> m.getNamaMenu().toLowerCase().contains(kata)
                            || m.getKategori().toLowerCase().contains(kata)
                            || m.getIdMenu().toLowerCase().contains(kata))
                    .collect(Collectors.toList());
        }
        halamanSaat = 0;
        renderHalaman();
    }

    @FXML private void handlePrev() { if (halamanSaat > 0) { halamanSaat--; renderHalaman(); } }

    @FXML private void handleNext() {
        int total = (int) Math.ceil((double) menuTerfilter.size() / PER_HALAMAN);
        if (halamanSaat < total - 1) { halamanSaat++; renderHalaman(); }
    }

    @FXML
    private void handleNavDashboard() throws IOException {
        AdminDashboardController ctrl = MainApp.loadSceneWithController(
                "admin-dashboard-view.fxml", "QuickBite Admin - Dashboard", 1100, 700);
        ctrl.initData(admin);
    }

    @FXML private void handleNavPenjualan() {}
    @FXML private void handleNavJamOp()     {}
    @FXML private void handleNavAkun()      {}

    @FXML
    private void handleKeluar() throws IOException {
        MainApp.loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
    }

    private Label buatCel(String teks, String css, double minW) {
        Label l = new Label(teks);
        l.getStyleClass().add(css);
        l.setMinWidth(minW);
        l.setMaxWidth(minW);
        return l;
    }

    private String statusCssKey(String s) {
        if (s == null) return "proses";
        return switch (s) {
            case "Tersedia"    -> "selesai";
            case "Stok Rendah" -> "warning";
            case "Stok Habis"  -> "batal";
            default            -> "proses";
        };
    }
}
