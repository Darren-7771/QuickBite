package com.example.projectrplbo.controller;

import com.example.projectrplbo.MainApp;
import com.example.projectrplbo.db.Retrieval;
import com.example.projectrplbo.model.Admin;
import com.example.projectrplbo.model.Pengguna;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.io.IOException;

public class LoginController {

    @FXML private ToggleButton btnCustomer;
    @FXML private ToggleButton btnAdmin;
    @FXML private TextField tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private Label lblError;

    private final Retrieval retrieval = new Retrieval();
    private boolean isAdminMode = false;
    private ToggleGroup roleGroup;

    @FXML
    public void initialize() {
        lblError.setVisible(false);

        roleGroup = new ToggleGroup();
        btnCustomer.setToggleGroup(roleGroup);
        btnAdmin.setToggleGroup(roleGroup);

        btnCustomer.setSelected(true);
        isAdminMode = false;

        roleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == btnAdmin) {
                isAdminMode = true;
            } else if (newToggle == btnCustomer) {
                isAdminMode = false;
            } else if (newToggle == null) {
                oldToggle.setSelected(true);
            }
        });
    }

    @FXML
    private void handleMasuk() {
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Username dan password tidak boleh kosong.");
            return;
        }

        if (isAdminMode) {
            Admin admin = retrieval.loginAdmin(username, password);
            if (admin == null) {
                showError("Kredensial Admin tidak valid.");
            } else {
                try {
                    AdminDashboardController ctrl = MainApp.loadSceneWithController(
                            "admin-dashboard-view.fxml", "QuickBite - Admin Dashboard", 1200, 700);
                    ctrl.initData(admin);
                } catch (IOException e) {
                    showError("Gagal membuka halaman dashboard admin.");
                    e.printStackTrace();
                }
            }
        } else {
            if (retrieval.isPenggunaTerblokir(username, password)) {
                showBlockedAlert();
                return;
            }

            Pengguna pengguna = retrieval.loginPengguna(username, password);
            if (pengguna == null) {
                showError("Username atau password salah.");
            } else {
                try {
                    ChatbotController ctrl = MainApp.loadSceneWithController(
                            "chatbot-view.fxml", "QuickBite - Chatbot", 1100, 700);
                    ctrl.initData(pengguna);
                } catch (IOException e) {
                    showError("Gagal membuka halaman chatbot.");
                    e.printStackTrace();
                }
            }
        }
    }

    @FXML
    private void handleDaftarAkunBaru() throws IOException {
        MainApp.loadScene("register-view.fxml", "QuickBite - Register", 750, 600);
    }

    @FXML
    private void handleLupaPassword() throws IOException {
        MainApp.loadScene("lupa-password-view.fxml", "QuickBite - Lupa Password", 650, 480);
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
    }

    private void showBlockedAlert() {
        showError("Akun Anda telah terblokir.");

        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Akun Terblokir");
        alert.setHeaderText("Akun Anda telah terblokir");
        alert.setContentText("Silakan hubungi admin QuickBite untuk membuka kembali akun Anda.");
        alert.showAndWait();
    }
}
