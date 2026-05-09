package com.example.projectrplbo.controller;

import com.example.projectrplbo.MainApp;
import com.example.projectrplbo.db.Retrieval;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

public class RegisterController {

    @FXML private TextField tfNama;
    @FXML private TextField tfUsername;
    @FXML private TextField tfEmail;
    @FXML private PasswordField pfPassword;
    @FXML private TextField tfTelepon;
    @FXML private TextField tfJalan;
    @FXML private TextField tfKota;
    @FXML private TextField tfProvinsi;
    @FXML private TextField tfKodePos;
    @FXML private Label lblError;

    private final Retrieval retrieval = new Retrieval();

    @FXML
    public void initialize() {
        lblError.setVisible(false);
    }

    @FXML
    private void handleDaftar() throws IOException {
        String nama     = tfNama.getText().trim();
        String username = tfUsername.getText().trim();
        String email    = tfEmail.getText().trim();
        String password = pfPassword.getText().trim();
        String telepon  = tfTelepon.getText().trim();
        String jalan    = tfJalan.getText().trim();
        String kota     = tfKota.getText().trim();
        String provinsi = tfProvinsi.getText().trim();
        String kodePos  = tfKodePos.getText().trim();

        if (nama.isEmpty() || username.isEmpty() || email.isEmpty() ||
            password.isEmpty() || telepon.isEmpty() || jalan.isEmpty()) {
            showError("Semua field wajib diisi.");
            return;
        }
        if (password.length() < 6) {
            showError("Password minimal 6 karakter.");
            return;
        }
        if (!email.contains("@")) {
            showError("Format email tidak valid.");
            return;
        }
        if (retrieval.isUsernameTaken(username)) {
            showError("Username sudah digunakan.");
            return;
        }
        if (retrieval.isEmailTaken(email)) {
            showError("Email sudah terdaftar.");
            return;
        }

        boolean ok = retrieval.registerPengguna(
            nama, jalan,
            kota.isEmpty() ? "-" : kota,
            provinsi.isEmpty() ? "-" : provinsi,
            kodePos.isEmpty() ? "00000" : kodePos,
            telepon, email, username, password
        );

        if (ok) {
            MainApp.loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
        } else {
            showError("Registrasi gagal. Silakan coba lagi.");
        }
    }

    @FXML
    private void handleKembaliLogin() throws IOException {
        MainApp.loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
    }
}
