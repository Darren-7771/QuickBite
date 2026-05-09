package com.example.projectrplbo.controller;

import com.example.projectrplbo.MainApp;
import com.example.projectrplbo.db.Retrieval;
import com.example.projectrplbo.model.Admin;
import com.example.projectrplbo.model.Menu;
import com.example.projectrplbo.model.Pesanan;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

public class AdminDashboardController {

    @FXML private Label lblNamaAdmin;
    @FXML private Label lblTotalPenjualan;
    @FXML private Label lblStokMenipis;
    @FXML private Label lblMenuTerlaris;
    @FXML private VBox vboxPesananRows;
    @FXML private VBox vboxStokRendah;
    @FXML private Label lblJmlStokRendah;

    private Admin admin;
    private final Retrieval retrieval = new Retrieval();

    private static final int STOK_RENDAH_THRESHOLD = 5;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public void initData(Admin admin) {
        this.admin = admin;
        lblNamaAdmin.setText(admin.getNamaAdmin());
        Platform.runLater(this::loadDashboard);
    }

    @FXML
    public void initialize() {}

    private void loadDashboard() {
        List<Menu> semuaMenu    = retrieval.getAllMenu();
        List<Pesanan> pesananList = retrieval.getAllPesananTerbaru(10);

        muatStatCards(semuaMenu, pesananList);
        muatTabelPesanan(pesananList);
        muatPanelStokRendah(semuaMenu);
    }

    private void muatStatCards(List<Menu> menus, List<Pesanan> pesananList) {

        double totalHariIni = pesananList.stream()
            .filter(p -> p.getTanggal() != null
                && p.getTanggal().toLocalDate().equals(java.time.LocalDate.now()))
            .mapToDouble(Pesanan::getTotalHarga).sum();
        lblTotalPenjualan.setText(String.format("Rp %,.0f", totalHariIni));

        long jmlMenipis = menus.stream()
            .filter(m -> m.getStok() <= STOK_RENDAH_THRESHOLD)
            .count();
        lblStokMenipis.setText(jmlMenipis + " Menu");

        String terlaris = retrieval.getMenuTerlaris();
        lblMenuTerlaris.setText(terlaris != null ? terlaris : "-");
    }

    private void muatTabelPesanan(List<Pesanan> list) {
        vboxPesananRows.getChildren().clear();
        if (list.isEmpty()) {
            Label empty = new Label("Belum ada pesanan.");
            empty.getStyleClass().add("admin-table-empty");
            vboxPesananRows.getChildren().add(empty);
            return;
        }
        for (Pesanan p : list) {
            HBox row = buatBarisPesanan(p);
            vboxPesananRows.getChildren().add(row);
        }
    }

    private HBox buatBarisPesanan(Pesanan p) {
        HBox row = new HBox();
        row.getStyleClass().add("admin-table-row");
        row.setPadding(new Insets(12, 16, 12, 16));
        row.setAlignment(Pos.CENTER_LEFT);

        Label lblId = buatCelLabel(p.getIdPesanan(), "admin-cell-id", 120);

        String nama = retrieval.getNamaPelangganByPesanan(p.getIdPengguna());
        Label lblNama = buatCelLabel(shortenName(nama), "admin-cell-text", 110);

        Label lblHarga = buatCelLabel(
            String.format("Rp %,.0f", p.getTotalHarga()), "admin-cell-text", 110);

        String tglStr = p.getTanggal() != null ? p.getTanggal().format(FMT) : "-";
        Label lblTgl = buatCelLabel(tglStr, "admin-cell-text", 110);

        Label lblStatus = new Label(p.getStatus());
        lblStatus.getStyleClass().add("admin-status-" + statusCssKey(p.getStatus()));
        lblStatus.setMinWidth(80);

        HBox.setHgrow(lblNama, Priority.ALWAYS);
        row.getChildren().addAll(lblId, lblNama, lblHarga, lblTgl, lblStatus);
        return row;
    }

    private void muatPanelStokRendah(List<Menu> menus) {
        vboxStokRendah.getChildren().clear();
        List<Menu> rendah = menus.stream()
            .filter(m -> m.getStok() <= STOK_RENDAH_THRESHOLD)
            .sorted(Comparator.comparingInt(Menu::getStok))
            .toList();

        lblJmlStokRendah.setText(rendah.size() + " produk memiliki stok rendah atau habis");

        for (Menu m : rendah) {
            String info = m.getStok() == 0
                ? m.getNamaMenu() + " (stok habis)"
                : m.getNamaMenu() + " (" + m.getStok() + " tersisa)";
            Label lbl = new Label("• " + info);
            lbl.getStyleClass().add("admin-stok-item");
            lbl.setWrapText(true);
            vboxStokRendah.getChildren().add(lbl);
        }
    }

    @FXML
    private void handleNavProduk() throws IOException {
        AdminProdukController ctrl = MainApp.loadSceneWithController(
            "admin-produk-view.fxml", "QuickBite Admin - Manajemen Produk", 1100, 700);
        ctrl.initData(admin);
    }

    @FXML
    private void handleNavPenjualan() {

    }

    @FXML
    private void handleNavJamOp() {

    }

    @FXML
    private void handleNavAkun() {

    }

    @FXML
    private void handleKeluar() throws IOException {
        MainApp.loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
    }

    private Label buatCelLabel(String teks, String styleClass, double minWidth) {
        Label lbl = new Label(teks);
        lbl.getStyleClass().add(styleClass);
        lbl.setMinWidth(minWidth);
        lbl.setMaxWidth(minWidth);
        lbl.setWrapText(false);
        return lbl;
    }

    private String statusCssKey(String status) {
        if (status == null) return "proses";
        return switch (status.toUpperCase()) {
            case "SELESAI"   -> "selesai";
            case "PROSES"    -> "proses";
            case "DIBATALKAN"-> "batal";
            default          -> "proses";
        };
    }

    private String shortenName(String nama) {
        if (nama == null) return "-";
        String[] parts = nama.trim().split(" ");
        if (parts.length == 1) return nama;
        return parts[0] + " " + parts[parts.length - 1].charAt(0) + ".";
    }
}
