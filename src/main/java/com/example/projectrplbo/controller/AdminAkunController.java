package com.example.projectrplbo.controller;

import com.example.projectrplbo.MainApp;
import com.example.projectrplbo.db.Retrieval;
import com.example.projectrplbo.model.Admin;
import com.example.projectrplbo.model.PenggunaInfo;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class AdminAkunController {

    @FXML private Label            lblNamaAdmin;
    @FXML private TextField        tfCariAkun;
    @FXML private ComboBox<String> cbFilterStatus;
    @FXML private VBox             vboxAkunRows;
    @FXML private Label            lblPagingInfo;
    @FXML private Button           btnPrev;
    @FXML private Button           btnNext;
    @FXML private HBox             hboxPageNumbers;
    @FXML private ToggleButton     toggleAutoReply;

    private Admin admin;
    private final Retrieval retrieval = new Retrieval();

    private List<PenggunaInfo> semuaPengguna = List.of();
    private List<PenggunaInfo> penggunaTerfilter = List.of();

    private static final int PER_HALAMAN = 5;
    private static final double COL_CHECK = 36;
    private static final double COL_MIN = 110;
    private static final double COL_PREF = 120;
    private int halamanSaat = 0;

    private boolean autoReplyEnabled = true;

    public void initData(Admin admin) {
        this.admin = admin;
        lblNamaAdmin.setText(admin.getNamaAdmin());
        Platform.runLater(this::muatSemua);
    }

    @FXML
    public void initialize() {
        cbFilterStatus.getItems().addAll("Semua Status", "Aktif", "Terblokir");
        cbFilterStatus.getSelectionModel().selectFirst();
        cbFilterStatus.setOnAction(e -> terapkanFilter());

        if (toggleAutoReply != null) {
            toggleAutoReply.setSelected(autoReplyEnabled);
            updateToggleStyle();
        }
    }

    private void muatSemua() {
        semuaPengguna = retrieval.getAllPenggunaInfo();
        penggunaTerfilter = semuaPengguna;
        halamanSaat = 0;
        renderHalaman();
    }

    private void terapkanFilter() {
        String cari = tfCariAkun.getText().trim().toLowerCase();
        String status = cbFilterStatus.getValue();

        penggunaTerfilter = semuaPengguna.stream()
            .filter(p -> {
                boolean cocokStatus = "Semua Status".equals(status)
                    || status == null
                    || p.getStatus().equalsIgnoreCase(status);
                boolean cocokCari = cari.isEmpty()
                    || p.getNama().toLowerCase().contains(cari)
                    || p.getEmail().toLowerCase().contains(cari)
                    || p.getUsername().toLowerCase().contains(cari);
                return cocokStatus && cocokCari;
            })
            .collect(Collectors.toList());

        halamanSaat = 0;
        renderHalaman();
    }

    @FXML
    private void handleCari() {
        terapkanFilter();
    }

    private void renderHalaman() {
        vboxAkunRows.getChildren().clear();

        int total = penggunaTerfilter.size();
        int totalHalaman = Math.max(1, (int) Math.ceil((double) total / PER_HALAMAN));
        halamanSaat = Math.max(0, Math.min(halamanSaat, totalHalaman - 1));

        int dari = halamanSaat * PER_HALAMAN;
        int sampai = Math.min(dari + PER_HALAMAN, total);

        if (total == 0) {
            Label empty = new Label("Tidak ada pengguna yang ditemukan.");
            empty.getStyleClass().add("admin-table-empty");
            vboxAkunRows.getChildren().add(empty);
        } else {
            List<PenggunaInfo> halIni = penggunaTerfilter.subList(dari, sampai);
            for (PenggunaInfo p : halIni) {
                vboxAkunRows.getChildren().add(buatBarisPengguna(p));
            }
        }

        lblPagingInfo.setText("Menampilkan " + (total == 0 ? 0 : dari + 1) + "-" + sampai + " dari " + total + " pengguna");

        btnPrev.setDisable(halamanSaat == 0);
        btnNext.setDisable(halamanSaat >= totalHalaman - 1);

        hboxPageNumbers.getChildren().clear();
        for (int i = 0; i < totalHalaman; i++) {
            final int idx = i;
            Button btnPage = new Button(String.valueOf(i + 1));
            if (i == halamanSaat) {
                btnPage.getStyleClass().add("admin-btn-paging-active");
            } else {
                btnPage.getStyleClass().add("admin-btn-paging");
            }
            btnPage.setOnAction(e -> { halamanSaat = idx; renderHalaman(); });
            hboxPageNumbers.getChildren().add(btnPage);
        }
    }

    private HBox buatBarisPengguna(PenggunaInfo p) {
        HBox row = new HBox();
        row.getStyleClass().add("admin-table-row");
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        CheckBox cb = new CheckBox();
        setFixedWidth(cb, COL_CHECK);

        HBox fotoNama = new HBox(8);
        fotoNama.setAlignment(Pos.CENTER_LEFT);
        StackPane avatar = new StackPane();
        avatar.getStyleClass().add("akun-avatar-mini");
        Label avatarLbl = new Label("👤");
        avatarLbl.getStyleClass().add("akun-avatar-mini-icon");
        avatar.getChildren().add(avatarLbl);
        Label lblNama = new Label(p.getNama());
        lblNama.getStyleClass().add("admin-cell-text");
        lblNama.setWrapText(false);
        fotoNama.getChildren().addAll(avatar, lblNama);
        setFluidColumn(fotoNama);

        Label lblUsername = new Label(p.getUsername());
        lblUsername.getStyleClass().add("admin-cell-text");
        setFluidColumn(lblUsername);

        VBox kontak = new VBox(2);
        setFluidColumn(kontak);
        Label lblEmail = new Label(p.getEmail());
        lblEmail.getStyleClass().add("admin-cell-text");
        lblEmail.setWrapText(false);
        Label lblTelp = new Label(p.getNoTelepon());
        lblTelp.getStyleClass().add("admin-cell-sub");
        kontak.getChildren().addAll(lblEmail, lblTelp);

        Label lblTgl = new Label(p.getTanggalDaftar());
        lblTgl.getStyleClass().add("admin-cell-text");
        setFluidColumn(lblTgl);

        HBox statusCell = new HBox();
        statusCell.setAlignment(Pos.CENTER_LEFT);
        setFluidColumn(statusCell);
        Label lblStatus = new Label(p.getStatus());
        lblStatus.getStyleClass().add(
            "Aktif".equalsIgnoreCase(p.getStatus()) ? "admin-status-selesai" : "admin-status-warning");
        statusCell.getChildren().add(lblStatus);

        HBox aksi = new HBox(6);
        aksi.setAlignment(Pos.CENTER_LEFT);
        setFluidColumn(aksi);

        Button btnDetail = new Button("👁");
        btnDetail.getStyleClass().add("admin-btn-icon-edit");
        btnDetail.setTooltip(new Tooltip("Lihat Detail"));
        btnDetail.setOnAction(e -> handleLihatDetail(p));

        Button btnToggleStatus = new Button(
            "Aktif".equalsIgnoreCase(p.getStatus()) ? "🔒" : "🔓");
        btnToggleStatus.getStyleClass().add("admin-btn-icon-edit");
        btnToggleStatus.setTooltip(new Tooltip(
            "Aktif".equalsIgnoreCase(p.getStatus()) ? "Blokir Pengguna" : "Aktifkan Pengguna"));
        btnToggleStatus.setOnAction(e -> handleToggleStatus(p));

        Button btnHapus = new Button("🗑");
        btnHapus.getStyleClass().add("admin-btn-icon-hapus");
        btnHapus.setTooltip(new Tooltip("Hapus Pengguna"));
        btnHapus.setOnAction(e -> handleHapus(p));

        aksi.getChildren().addAll(btnDetail, btnToggleStatus, btnHapus);

        row.getChildren().addAll(cb, fotoNama, lblUsername, kontak, lblTgl, statusCell, aksi);
        return row;
    }

    private void setFixedWidth(Region region, double width) {
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private void setFluidColumn(Region region) {
        region.setMinWidth(COL_MIN);
        region.setPrefWidth(COL_PREF);
        region.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(region, Priority.ALWAYS);
    }

    private void handleLihatDetail(PenggunaInfo p) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detail Pengguna");
        alert.setHeaderText(p.getNama());
        alert.setContentText(
            "Username  : " + p.getUsername() + "\n" +
            "Email     : " + p.getEmail() + "\n" +
            "Telepon   : " + p.getNoTelepon() + "\n" +
            "Status    : " + p.getStatus() + "\n" +
            "Terdaftar : " + p.getTanggalDaftar()
        );
        alert.showAndWait();
    }

    private void handleToggleStatus(PenggunaInfo p) {
        String statusBaru = "Aktif".equalsIgnoreCase(p.getStatus()) ? "Terblokir" : "Aktif";
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi");
        konfirmasi.setHeaderText("Ubah status pengguna?");
        konfirmasi.setContentText("Ubah status " + p.getNama() + " menjadi " + statusBaru + "?");
        konfirmasi.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                boolean ok = retrieval.updateStatusAkun(p.getIdAkun(), statusBaru);
                if (ok) {
                    muatSemua();
                } else {
                    tampilkanGagal("Gagal mengubah status pengguna.");
                }
            }
        });
    }

    private void handleHapus(PenggunaInfo p) {
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Hapus Pengguna");
        konfirmasi.setHeaderText("Hapus akun " + p.getNama() + "?");
        konfirmasi.setContentText("Tindakan ini tidak dapat dibatalkan.");
        konfirmasi.showAndWait().ifPresent(resp -> {
            if (resp == ButtonType.OK) {
                boolean ok = retrieval.hapusAkun(p.getIdAkun());
                if (ok) {
                    muatSemua();
                } else {
                    tampilkanGagal("Gagal menghapus akun pengguna.");
                }
            }
        });
    }

    @FXML
    private void handleTambahPengguna() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Tambah Pengguna");
        info.setHeaderText("Fitur Tambah Pengguna");
        info.setContentText("Pengguna baru dapat mendaftarkan diri melalui halaman Registrasi.");
        info.showAndWait();
    }

    @FXML
    private void handleToggleAutoReply() {
        autoReplyEnabled = toggleAutoReply.isSelected();
        updateToggleStyle();
    }

    private void updateToggleStyle() {
        if (toggleAutoReply.isSelected()) {
            toggleAutoReply.getStyleClass().setAll("akun-toggle-btn", "akun-toggle-on");
            toggleAutoReply.setText("ON");
        } else {
            toggleAutoReply.getStyleClass().setAll("akun-toggle-btn", "akun-toggle-off");
            toggleAutoReply.setText("OFF");
        }
    }

    @FXML
    private void handlePrev() {
        if (halamanSaat > 0) { halamanSaat--; renderHalaman(); }
    }

    @FXML
    private void handleNext() {
        int totalHalaman = (int) Math.ceil((double) penggunaTerfilter.size() / PER_HALAMAN);
        if (halamanSaat < totalHalaman - 1) { halamanSaat++; renderHalaman(); }
    }

    private void tampilkanGagal(String pesan) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Gagal");
        alert.setHeaderText(null);
        alert.setContentText(pesan);
        alert.showAndWait();
    }

    @FXML
    private void handleNavDashboard() throws IOException {
        AdminDashboardController ctrl = MainApp.loadSceneWithController(
            "admin-dashboard-view.fxml", "QuickBite Admin - Dashboard", 1100, 700);
        ctrl.initData(admin);
    }

    @FXML
    private void handleNavProduk() throws IOException {
        AdminProdukController ctrl = MainApp.loadSceneWithController(
            "admin-produk-view.fxml", "QuickBite Admin - Manajemen Produk", 1100, 700);
        ctrl.initData(admin);
    }

    @FXML
    private void handleNavPenjualan() throws IOException {
        AdminPenjualanController ctrl = MainApp.loadSceneWithController(
            "admin-penjualan-view.fxml", "QuickBite Admin - Manajemen Penjualan", 1100, 700);
        ctrl.initData(admin);
    }

    @FXML
    private void handleNavJamOp() throws IOException {
        AdminJamOperasionalController ctrl = MainApp.loadSceneWithController(
            "admin-jam-operasional-view.fxml", "QuickBite Admin - Jam Operasional", 1100, 700);
        ctrl.initData(admin);
    }

    @FXML
    private void handleKeluar() throws IOException {
        MainApp.loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
    }
}
