package com.example.projectrplbo.controller;

import com.example.projectrplbo.MainApp;
import com.example.projectrplbo.chatbot.Chatbot;
import com.example.projectrplbo.db.Retrieval;
import com.example.projectrplbo.model.JadwalOperasional;
import com.example.projectrplbo.model.Pengguna;
import com.example.projectrplbo.model.Pesanan;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ChatbotController {

    @FXML private Label lblNamaPengguna;
    @FXML private Label lblRolePengguna;
    @FXML private VBox vboxRiwayat;

    @FXML private Region statusDot;
    @FXML private Label lblStatusSystem;
    @FXML private Label lblJamOperasional;
    @FXML private Label lblServerStatus;

    @FXML private VBox vboxChat;
    @FXML private ScrollPane scrollChat;
    @FXML private TextField tfInput;

    private Pengguna pengguna;
    private Chatbot chatbot;
    private final Retrieval retrieval = new Retrieval();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        vboxChat.heightProperty().addListener((obs, oldVal, newVal) ->
            scrollChat.setVvalue(1.0));
    }

    public void initData(Pengguna pengguna) {
        this.pengguna = pengguna;
        this.chatbot = new Chatbot();

        lblNamaPengguna.setText(pengguna.getNama());
        lblRolePengguna.setText("Pelanggan Setia");

        JadwalOperasional jadwal = chatbot.getJadwalAktif();
        String jamInfo = jadwal.getJamBukaFormatted() + " - " + jadwal.getJamTutupFormatted() + " WIB";
        lblJamOperasional.setText("Jam Operasional: " + jamInfo);
        boolean operational = jadwal.isOperasional();
        lblStatusSystem.setText(operational ? "Sistem Aktif" : "Sistem Tutup");
        lblStatusSystem.getStyleClass().removeAll("status-aktif", "status-tutup");
        lblStatusSystem.getStyleClass().add(operational ? "status-aktif" : "status-tutup");

        if (statusDot != null) {
            statusDot.getStyleClass().removeAll("status-dot-green", "status-dot-gray");
            statusDot.getStyleClass().add(operational ? "status-dot-green" : "status-dot-gray");
        }

        lblServerStatus.setText(operational ? "Online" : "Offline");
        lblServerStatus.getStyleClass().removeAll("header-server-online", "header-server-offline");
        lblServerStatus.getStyleClass().add(operational ? "header-server-online" : "header-server-offline");

        loadRiwayatSidebar();

        String welcome = "Selamat datang di QuickBite!\n" +
            "Saya adalah asisten otomatis yang siap membantu Anda memesan makanan favorit Anda.";
        addBotMessage(welcome);

        String guide = "Untuk memulai, gunakan perintah berikut:\n" +
            "> Ketik MENU untuk melihat daftar menu lengkap\n" +
            "> Ketik CARI [Kategori] untuk mencari menu berdasarkan kategori\n" +
            "> Ketik PESAN [ID Menu] untuk memesan makanan";
        addBotMessage(guide);

        addQuickCommands();
    }

    @FXML
    private void handleKirimPesan() {
        String input = tfInput.getText().trim();
        if (input.isEmpty()) return;

        addUserMessage(input);
        tfInput.clear();

        Thread t = new Thread(() -> {
            String response = chatbot.prosesPesan(input, pengguna);
            Platform.runLater(() -> {
                addBotMessage(response);
                if (response.contains("Pesanan berhasil dikonfirmasi")) {
                    loadRiwayatSidebar();
                }
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void handleKeluar() throws IOException {
        chatbot.resetSesi();
        MainApp.loadScene("login-view.fxml", "QuickBite - Login", 900, 600);
    }

    @FXML
    private void handleBukaProfilDialog() {
        try {
            ProfilController ctrl = MainApp.loadSceneWithController(
                "profil-view.fxml", "QuickBite - Edit Profil", 900, 620);
            ctrl.initData(pengguna, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onProfilUpdated(Pengguna updated) {
        this.pengguna = updated;
        lblNamaPengguna.setText(updated.getNama());
    }

    private void addQuickCommands() {
        HBox hbox = new HBox(8);
        hbox.setPadding(new Insets(6, 12, 6, 12));
        hbox.setAlignment(Pos.CENTER_LEFT);

        String[][] commands = {
            {"MENU", "Lihat daftar menu"},
            {"PESAN [ID]", "Pesan makanan"},
            {"STATUS", "Cek status pesanan"},
            {"BATAL", "Batalkan pesanan"}
        };

        for (String[] cmd : commands) {
            VBox box = new VBox(2);
            box.getStyleClass().add("quick-cmd-box");
            Label lblCmd = new Label(cmd[0]);
            lblCmd.getStyleClass().add("quick-cmd-title");
            Label lblDesc = new Label(cmd[1]);
            lblDesc.getStyleClass().add("quick-cmd-desc");
            box.getChildren().addAll(lblCmd, lblDesc);
            final String command = cmd[0].replace("[ID]", "").trim();
            box.setOnMouseClicked(e -> tfInput.setText(command));
            hbox.getChildren().add(box);
        }

        VBox wrapper = new VBox();
        wrapper.getStyleClass().add("quick-cmd-panel");
        Label title = new Label("⚡ Perintah Cepat");
        title.getStyleClass().add("quick-cmd-header");
        wrapper.getChildren().addAll(title, hbox);
        vboxChat.getChildren().add(wrapper);
    }

    private void addBotMessage(String text) {
        HBox row = new HBox(8);
        row.setPadding(new Insets(4, 16, 4, 16));
        row.setAlignment(Pos.TOP_LEFT);

        Label avatar = new Label("🤖");
        avatar.getStyleClass().add("bot-avatar");

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("bot-bubble");
        bubble.setMaxWidth(580);

        Text txt = new Text(text);
        txt.setWrappingWidth(540);
        txt.getStyleClass().add("bubble-text");
        TextFlow tf = new TextFlow(txt);

        Label time = new Label(java.time.LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("msg-time");

        bubble.getChildren().addAll(tf, time);
        row.getChildren().addAll(avatar, bubble);
        vboxChat.getChildren().add(row);
    }

    private void addUserMessage(String text) {
        HBox row = new HBox(8);
        row.setPadding(new Insets(4, 16, 4, 16));
        row.setAlignment(Pos.TOP_RIGHT);

        VBox bubble = new VBox(4);
        bubble.getStyleClass().add("user-bubble");
        bubble.setMaxWidth(400);

        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.getStyleClass().add("bubble-text-user");

        Label time = new Label(java.time.LocalTime.now().format(TIME_FMT));
        time.getStyleClass().add("msg-time-user");

        bubble.getChildren().addAll(lbl, time);

        Label avatar = new Label("👤");
        avatar.getStyleClass().add("user-avatar");

        row.getChildren().addAll(bubble, avatar);
        vboxChat.getChildren().add(row);
    }

    private void loadRiwayatSidebar() {
        vboxRiwayat.getChildren().clear();
        List<Pesanan> list = retrieval.getPesananByUser(pengguna.getIdPengguna());
        if (list.isEmpty()) {
            Label empty = new Label("Belum ada pesanan");
            empty.getStyleClass().add("riwayat-empty");
            vboxRiwayat.getChildren().add(empty);
            return;
        }
        for (Pesanan p : list) {
            VBox card = new VBox(0);
            card.getStyleClass().add("riwayat-card-white");

            HBox topRow = new HBox();
            topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label noOrder = new Label(p.getIdPesanan());
            noOrder.getStyleClass().add("riwayat-id-dark");
            noOrder.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(noOrder, Priority.ALWAYS);

            Label harga = new Label(String.format("Rp %,.0f", p.getTotalHarga()));
            harga.getStyleClass().add("riwayat-harga-dark");

            topRow.getChildren().addAll(noOrder, harga);

            String tgl = p.getTanggal() != null ? p.getTanggal().format(FMT) : "-";
            Label tanggal = new Label(tgl);
            tanggal.getStyleClass().add("riwayat-tanggal-dark");

            card.getChildren().addAll(topRow, tanggal);
            vboxRiwayat.getChildren().add(card);
        }
    }

    @FXML
    private void handleInputKeyPress(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
            handleKirimPesan();
        }
    }
}
