package com.example.projectrplbo.controller;

import com.example.projectrplbo.MainApp;
import com.example.projectrplbo.db.Retrieval;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class LupaPasswordController {

    @FXML private TextField tfEmail;
    @FXML private Label lblStatus;
    @FXML private VBox boxNewPassword;
    @FXML private PasswordField pfPasswordBaru;
    @FXML private PasswordField pfKonfirmasiPassword;

    private final Retrieval retrieval = new Retrieval();
    private boolean emailVerified = false;
    private String verifiedEmail = "";

    @FXML
    public void initialize() {
        lblStatus.setVisible(false);
        boxNewPassword.setVisible(false);
        boxNewPassword.setManaged(false);
    }

    @FXML
    private void handleKirim() {
        if (!emailVerified) {
            String email = tfEmail.getText().trim();
            if (email.isEmpty()) {
                showStatus("error", "Email tidak boleh kosong.");
                return;
            }
            if (!retrieval.isEmailTaken(email)) {
                showStatus("error", "Email tidak terdaftar dalam sistem.");
                return;
            }
            verifiedEmail = email;
            emailVerified = true;
            tfEmail.setDisable(true);
            boxNewPassword.setVisible(true);
            boxNewPassword.setManaged(true);
            showStatus("info", "Email diverifikasi. Masukkan password baru Anda.");
        } else {
            String pwBaru = pfPasswordBaru.getText().trim();
            String konfirmasi = pfKonfirmasiPassword.getText().trim();
            if (pwBaru.length() < 6) {
                showStatus("error", "Password minimal 6 karakter.");
                return;
            }
            if (!pwBaru.equals(konfirmasi)) {
                showStatus("error", "Konfirmasi password tidak cocok.");
                return;
            }
            boolean ok = retrieval.resetPasswordByEmail(verifiedEmail, pwBaru);
            if (ok) {
                showStatus("success", "Password berhasil direset! Mengarahkan ke halaman login...");
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                    javafx.util.Duration.seconds(2));
                pause.setOnFinished(e -> {
                    try {
                        MainApp.loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
                    } catch (IOException ex) { ex.printStackTrace(); }
                });
                pause.play();
            } else {
                showStatus("error", "Gagal reset password. Coba lagi.");
            }
        }
    }

    @FXML
    private void handleKembaliLogin() throws IOException {
        MainApp.loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
    }

    private void showStatus(String type, String msg) {
        lblStatus.setText(msg);
        lblStatus.setVisible(true);
        lblStatus.getStyleClass().removeAll("error-label", "success-label", "info-label");
        lblStatus.getStyleClass().add(type + "-label");
    }
}
