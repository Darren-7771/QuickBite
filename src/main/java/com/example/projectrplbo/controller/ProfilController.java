package com.example.projectrplbo.controller;

import com.example.projectrplbo.MainApp;
import com.example.projectrplbo.db.Retrieval;
import com.example.projectrplbo.model.Pengguna;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;

public class ProfilController {

    @FXML private TextField lblUsername;
    @FXML private TextField tfNama;
    @FXML private TextField tfEmail;
    @FXML private TextField tfTelepon;
    @FXML private TextField tfJalan;
    @FXML private TextField tfKota;
    @FXML private TextField tfProvinsi;
    @FXML private TextField tfKodePos;
    @FXML private PasswordField pfPasswordLama;
    @FXML private PasswordField pfPasswordBaru;
    @FXML private Label lblError;

    private Pengguna pengguna;
    private ChatbotController chatbotController;
    private final Retrieval retrieval = new Retrieval();

    @FXML
    public void initialize() {
        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    public void initData(Pengguna pengguna, ChatbotController chatbotController) {
        this.pengguna = pengguna;
        this.chatbotController = chatbotController;

        lblUsername.setText(pengguna.getUsername());
        tfNama.setText(pengguna.getNama() != null ? pengguna.getNama() : "");
        tfEmail.setText(pengguna.getEmail() != null ? pengguna.getEmail() : "");
        tfTelepon.setText(pengguna.getNoTelepon() != null ? pengguna.getNoTelepon() : "");
        String alamat = pengguna.getAlamat() != null ? pengguna.getAlamat() : "";
        if (alamat.contains(",")) {
            String[] parts = alamat.split(",", 3);
            tfJalan.setText(parts[0].trim());
            if (parts.length > 1) tfKota.setText(parts[1].trim());
            if (parts.length > 2) {
                String provKode = parts[2].trim();
                int lastSpace = provKode.lastIndexOf(" ");
                if (lastSpace > 0) {
                    tfProvinsi.setText(provKode.substring(0, lastSpace).trim());
                    tfKodePos.setText(provKode.substring(lastSpace + 1).trim());
                } else {
                    tfProvinsi.setText(provKode);
                }
            }
        } else {
            tfJalan.setText(alamat);
        }
    }

    @FXML
    private void handleSimpanPerubahan() {
        String nama     = tfNama.getText().trim();
        String email    = tfEmail.getText().trim();
        String telepon  = tfTelepon.getText().trim();
        String jalan    = tfJalan.getText().trim();
        String kota     = tfKota.getText().trim();
        String provinsi = tfProvinsi.getText().trim();
        String kodePos  = tfKodePos.getText().trim();
        String pwLama   = pfPasswordLama.getText().trim();
        String pwBaru   = pfPasswordBaru.getText().trim();

        if (nama.isEmpty() || email.isEmpty() || telepon.isEmpty()) {
            showError("Nama, email, dan nomor telepon tidak boleh kosong.");
            return;
        }
        if (!email.contains("@")) {
            showError("Format email tidak valid.");
            return;
        }
        if (!pwBaru.isEmpty()) {
            if (pwLama.isEmpty()) {
                showError("Masukkan password lama terlebih dahulu untuk mengganti password.");
                return;
            }
            if (!retrieval.verifikasiPassword(pengguna.getIdAkun(), pwLama)) {
                showError("Password lama tidak sesuai.");
                return;
            }
            if (pwBaru.length() < 6) {
                showError("Password baru minimal 6 karakter.");
                return;
            }
        }

        boolean ok = retrieval.updateProfil(
            pengguna.getIdAkun(), pengguna.getIdPengguna(),
            nama,
            jalan.isEmpty() ? "-" : jalan,
            kota.isEmpty() ? "-" : kota,
            provinsi.isEmpty() ? "-" : provinsi,
            kodePos.isEmpty() ? "00000" : kodePos,
            telepon, email,
            pwBaru.isEmpty() ? null : pwBaru
        );

        if (ok) {
            pengguna.setNama(nama);
            pengguna.setEmail(email);
            pengguna.setNoTelepon(telepon);
            pengguna.setAlamat(jalan + ", " + kota + ", " + provinsi + " " + kodePos);

            try {
                ChatbotController ctrl = MainApp.loadSceneWithController(
                    "chatbot-view.fxml", "QuickBite - Chatbot", 1100, 720);
                ctrl.initData(pengguna);
            } catch (IOException e) {
                e.printStackTrace();
                showError("Berhasil disimpan, tetapi gagal kembali ke chatbot.");
            }
        } else {
            showError("Gagal menyimpan. Email atau nomor telepon mungkin sudah digunakan akun lain.");
        }
    }

    @FXML
    private void handleBatal() {
        try {
            ChatbotController ctrl = MainApp.loadSceneWithController(
                "chatbot-view.fxml", "QuickBite - Chatbot", 1100, 720);
            ctrl.initData(pengguna);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }
}
