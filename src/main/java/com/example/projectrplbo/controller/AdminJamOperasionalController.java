package com.example.projectrplbo.controller;

import com.example.projectrplbo.MainApp;
import com.example.projectrplbo.db.Retrieval;
import com.example.projectrplbo.model.Admin;
import com.example.projectrplbo.model.JadwalOperasional;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public class AdminJamOperasionalController {

    @FXML private Label lblNamaAdmin;
    @FXML private TextField tfJamBuka;
    @FXML private TextField tfJamTutup;
    @FXML private CheckBox chkTerapkanSemua;
    @FXML private Label lblStatusSistem;
    @FXML private Label lblStatusDesc;
    @FXML private Label lblStatusOpenClose;
    @FXML private HBox hboxStatusBanner;
    @FXML private Label lblError;

    private Admin admin;
    private final Retrieval retrieval = new Retrieval();

    public void initData(Admin admin) {
        this.admin = admin;
        lblNamaAdmin.setText(admin.getNamaAdmin());
        Platform.runLater(this::muatData);
    }

    @FXML
    public void initialize() {
        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    private void muatData() {
        JadwalOperasional jadwal = retrieval.getJadwalAktif();
        tfJamBuka.setText(jadwal.getJamBukaFormatted());
        tfJamTutup.setText(jadwal.getJamTutupFormatted());

        chkTerapkanSemua.setSelected("Setiap Hari".equalsIgnoreCase(jadwal.getHari()));

        updateStatusBanner(jadwal);
    }

    private void updateStatusBanner(JadwalOperasional jadwal) {
        boolean open = jadwal.isOperasional();
        if (open) {
            hboxStatusBanner.getStyleClass().setAll("jamop-status-banner-open");
            lblStatusSistem.setText("SISTEM AKTIF");
            lblStatusDesc.setText("Chatbot sedang beroperasi");
            lblStatusOpenClose.setText("OPEN");
            lblStatusOpenClose.getStyleClass().setAll("jamop-status-open");
        } else {
            hboxStatusBanner.getStyleClass().setAll("jamop-status-banner-closed");
            lblStatusSistem.setText("SISTEM TIDAK AKTIF");
            lblStatusDesc.setText("Chatbot tidak beroperasi saat ini");
            lblStatusOpenClose.setText("CLOSED");
            lblStatusOpenClose.getStyleClass().setAll("jamop-status-closed");
        }
    }

    @FXML
    private void handleSimpan() {
        String bukaStr  = tfJamBuka.getText().trim();
        String tutupStr = tfJamTutup.getText().trim();

        LocalTime jamBuka, jamTutup;
        try {
            jamBuka = LocalTime.parse(bukaStr);
        } catch (DateTimeParseException e) {
            tampilkanError("Format jam buka tidak valid. Gunakan format HH:mm (contoh: 08:00).");
            return;
        }
        try {
            jamTutup = LocalTime.parse(tutupStr);
        } catch (DateTimeParseException e) {
            tampilkanError("Format jam tutup tidak valid. Gunakan format HH:mm (contoh: 22:00).");
            return;
        }
        if (!jamBuka.isBefore(jamTutup)) {
            tampilkanError("Jam buka harus lebih awal dari jam tutup.");
            return;
        }

        sembunyikanError();

        String hari = chkTerapkanSemua.isSelected() ? "Setiap Hari" : "Setiap Hari";
        boolean berhasil = retrieval.updateJadwalOperasional(jamBuka, jamTutup, hari);
        if (berhasil) {
            JadwalOperasional updated = retrieval.getJadwalAktif();
            updateStatusBanner(updated);
            tampilkanInfo("Jam operasional berhasil disimpan.");
        } else {
            tampilkanError("Gagal menyimpan jam operasional. Coba lagi.");
        }
    }

    private void tampilkanError(String pesan) {
        lblError.setText(pesan);
        lblError.getStyleClass().setAll("admin-form-error");
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void tampilkanInfo(String pesan) {
        lblError.setText(pesan);
        lblError.getStyleClass().setAll("jamop-info-label");
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void sembunyikanError() {
        lblError.setVisible(false);
        lblError.setManaged(false);
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
    private void handleNavAkun() throws IOException {
        AdminAkunController ctrl = MainApp.loadSceneWithController(
                "admin-akun-view.fxml", "QuickBite Admin - Manajemen Akun", 1100, 700);
        ctrl.initData(admin);
    }

    @FXML
    private void handleKeluar() throws IOException {
        MainApp.loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
    }
}
