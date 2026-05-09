package com.example.projectrplbo.controller;

import com.example.projectrplbo.MainApp;
import com.example.projectrplbo.db.Retrieval;
import com.example.projectrplbo.model.Admin;
import com.example.projectrplbo.model.Pesanan;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AdminPenjualanController {

    @FXML private Label  lblNamaAdmin;
    @FXML private Label  lblTotalPendapatan;
    @FXML private Label  lblUpdateTerakhir;
    @FXML private Label  lblPersenPendapatan;
    @FXML private Label  lblJumlahPesananSukses;
    @FXML private Label  lblDariTotal;
    @FXML private Label  lblPersenPesanan;
    @FXML private TextField tfCariPesanan;
    @FXML private VBox      vboxPesananRows;
    @FXML private Label     lblPagingInfo;
    @FXML private Button    btnPrev;
    @FXML private Button    btnNext;
    @FXML private HBox      hboxPageNumbers;
    @FXML private StackPane modalOverlay;
    @FXML private BorderPane mainPane;           // untuk efek blur
    @FXML private Label lblModalIdTransaksi;
    @FXML private Label lblModalNama;
    @FXML private Label lblModalWaktu;
    @FXML private Label lblModalStatus;
    @FXML private VBox  vboxDetailItems;
    @FXML private Label lblModalTotal;
    @FXML private Label lblModalBayar;
    @FXML private Label lblModalKembali;
    private Admin   admin;
    private final Retrieval retrieval = new Retrieval();

    private List<PesananRow> semuaRow   = new ArrayList<>();
    private List<PesananRow> filtered   = new ArrayList<>();
    private static final int PER_PAGE   = 5;
    private int currentPage             = 0;

    private static final DateTimeFormatter FMT_DISPLAY =
            DateTimeFormatter.ofPattern("dd/MM/yyyy\nHH:mm:ss");
    private static final DateTimeFormatter FMT_MODAL =
            DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm:ss 'WIB'");
    private record PesananRow(Pesanan pesanan, String namaPelanggan) {}
    public void initData(Admin admin) {
        this.admin = admin;
        lblNamaAdmin.setText(admin.getNamaAdmin());
        Platform.runLater(this::loadData);
    }

    @FXML
    public void initialize() {
        if (modalOverlay != null) {
            modalOverlay.setVisible(false);
            modalOverlay.setManaged(false);
        }
    }

    private void loadData() {
        List<Pesanan> pesananList = retrieval.getAllPesananTerbaru(10);
        semuaRow = pesananList.stream()
                .map(p -> new PesananRow(p, retrieval.getNamaPelangganByPesanan(p.getIdPengguna())))
                .collect(Collectors.toList());
        filtered = new ArrayList<>(semuaRow);
        currentPage = 0;

        muatStatCards(pesananList);
        renderPage();
    }

    private void muatStatCards(List<Pesanan> list) {
        double totalHariIni = list.stream()
                .filter(p -> p.getTanggal() != null
                        && p.getTanggal().toLocalDate().equals(java.time.LocalDate.now()))
                .mapToDouble(Pesanan::getTotalHarga)
                .sum();
        lblTotalPendapatan.setText(String.format("Rp %,.0f", totalHariIni));

        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        lblUpdateTerakhir.setText("Update terakhir: " + now);
        long sukses = list.stream()
                .filter(p -> "SELESAI".equalsIgnoreCase(p.getStatus()))
                .count();
        lblJumlahPesananSukses.setText(String.valueOf(sukses));
        lblDariTotal.setText("Dari " + list.size() + " total pesanan");
    }

    private void renderPage() {
        vboxPesananRows.getChildren().clear();

        int total        = filtered.size();
        int totalPages   = Math.max(1, (int) Math.ceil((double) total / PER_PAGE));
        int from         = currentPage * PER_PAGE;
        int to           = Math.min(from + PER_PAGE, total);

        List<PesananRow> slice = filtered.subList(from, to);
        for (PesananRow row : slice) {
            vboxPesananRows.getChildren().add(buatBarisRow(row));
        }

        lblPagingInfo.setText("Menampilkan " + (total == 0 ? 0 : from + 1)
                + "-" + to + " dari " + total + " produk");
        btnPrev.setDisable(currentPage == 0);
        btnNext.setDisable(currentPage >= totalPages - 1);

        hboxPageNumbers.getChildren().clear();
        for (int i = 0; i < totalPages; i++) {
            final int page = i;
            Button btn = new Button(String.valueOf(i + 1));
            btn.getStyleClass().add(i == currentPage
                    ? "admin-btn-paging-active" : "admin-btn-paging");
            btn.setOnAction(e -> { currentPage = page; renderPage(); });
            hboxPageNumbers.getChildren().add(btn);
        }
    }

    private HBox buatBarisRow(PesananRow row) {
        Pesanan p    = row.pesanan();
        String  nama = row.namaPelanggan();

        HBox hbox = new HBox(0);
        hbox.getStyleClass().add("admin-table-row");
        hbox.setPadding(new Insets(12, 16, 12, 16));
        hbox.setAlignment(Pos.CENTER_LEFT);
        Label lblId = new Label(p.getIdPesanan());
        lblId.getStyleClass().add("admin-penjualan-id-link");
        lblId.setMinWidth(110);
        lblId.setMaxWidth(110);
        Label lblNama = new Label(nama);
        lblNama.getStyleClass().add("admin-penjualan-nama-bold");
        lblNama.setMinWidth(130);
        HBox.setHgrow(lblNama, Priority.ALWAYS);
        String waktu = p.getTanggal() != null
                ? p.getTanggal().format(FMT_DISPLAY) : "-";
        Label lblWaktu = new Label(waktu);
        lblWaktu.getStyleClass().add("admin-cell-text");
        lblWaktu.setWrapText(true);
        lblWaktu.setMinWidth(120);
        lblWaktu.setMaxWidth(120);
        String ringkasanMenu = buatRingkasanMenu(p.getIdPesanan());
        Label lblMenu = new Label(ringkasanMenu);
        lblMenu.getStyleClass().add("admin-cell-text");
        lblMenu.setWrapText(true);
        lblMenu.setMinWidth(210);
        lblMenu.setMaxWidth(210);
        Label lblHarga = new Label(String.format("Rp %,.0f", p.getTotalHarga()));
        lblHarga.getStyleClass().add("admin-cell-text");
        lblHarga.setMinWidth(100);
        lblHarga.setMaxWidth(100);
        Label lblStatus = new Label(capitalize(p.getStatus()));
        lblStatus.getStyleClass().add("admin-status-" + statusCssKey(p.getStatus()));
        lblStatus.setMinWidth(90);
        Button btnDetail = new Button("👁  Detail");
        btnDetail.getStyleClass().add("admin-penjualan-btn-detail");
        btnDetail.setOnAction(e -> bukaModal(p, nama));

        HBox aksiBox = new HBox(btnDetail);
        aksiBox.setAlignment(Pos.CENTER_LEFT);
        aksiBox.setMinWidth(70);

        hbox.getChildren().addAll(lblId, lblNama, lblWaktu, lblMenu, lblHarga, lblStatus, aksiBox);
        return hbox;
    }

    private String buatRingkasanMenu(String idPesanan) {
        List<Pesanan.DetailPesanan> details = retrieval.getDetailPesanan(idPesanan);
        if (details.isEmpty()) return "-";
        return details.stream()
                .map(d -> d.getJumlah() + "x " + d.getNamaMenu())
                .collect(Collectors.joining(", "));
    }

    private void bukaModal(Pesanan p, String nama) {
        lblModalIdTransaksi.setText(p.getIdPesanan());
        lblModalNama.setText(nama);
        lblModalWaktu.setText(p.getTanggal() != null
                ? p.getTanggal().format(FMT_MODAL) : "-");
        lblModalStatus.setText(capitalize(p.getStatus()));

        vboxDetailItems.getChildren().clear();
        List<Pesanan.DetailPesanan> details = retrieval.getDetailPesanan(p.getIdPesanan());
        for (Pesanan.DetailPesanan d : details) {
            HBox row = new HBox(8);
            Label key = new Label(d.getNamaMenu() + " x " + d.getJumlah());
            key.getStyleClass().add("admin-modal-detail-key");
            HBox.setHgrow(key, Priority.ALWAYS);
            Label val = new Label(String.format("Rp %,.0f", d.getSubtotal()));
            val.getStyleClass().add("admin-modal-detail-val");
            row.getChildren().addAll(key, val);
            vboxDetailItems.getChildren().add(row);
        }

        double total = p.getTotalHarga();
        lblModalTotal.setText(String.format("Rp %,.0f", total));
        lblModalBayar.setText(String.format("Rp %,.0f", total));   // uang bayar = total (assumption)
        lblModalKembali.setText("Rp 0");

        mainPane.setEffect(new GaussianBlur(6));
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    @FXML
    private void handleTutupModal() {
        mainPane.setEffect(null);
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
    }

    @FXML
    private void handleCariPesanan() {
        String kata = tfCariPesanan.getText().trim().toLowerCase();
        if (kata.isEmpty()) {
            filtered = new ArrayList<>(semuaRow);
        } else {
            filtered = semuaRow.stream()
                    .filter(r -> r.pesanan().getIdPesanan().toLowerCase().contains(kata)
                            || r.namaPelanggan().toLowerCase().contains(kata))
                    .collect(Collectors.toList());
        }
        currentPage = 0;
        renderPage();
    }

    @FXML
    private void handleCetakLaporan() {
        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Cetak Laporan");
        info.setHeaderText(null);
        info.setContentText("Fitur cetak laporan belum diimplementasikan.");
        info.showAndWait();
    }

    @FXML private void handlePrev() { if (currentPage > 0) { currentPage--; renderPage(); } }

    @FXML private void handleNext() {
        int total = (int) Math.ceil((double) filtered.size() / PER_PAGE);
        if (currentPage < total - 1) { currentPage++; renderPage(); }
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

    @FXML private void handleNavJamOp()  {}
    @FXML private void handleNavAkun()   {}

    @FXML
    private void handleKeluar() throws IOException {
        MainApp.loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
    }

    private String statusCssKey(String status) {
        if (status == null) return "proses";
        return switch (status.toUpperCase()) {
            case "SELESAI"    -> "selesai";
            case "PROSES"     -> "proses";
            case "DIBATALKAN" -> "batal";
            default           -> "proses";
        };
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return "-";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }
}
