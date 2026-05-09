package com.example.projectrplbo.chatbot;

import com.example.projectrplbo.db.Retrieval;
import com.example.projectrplbo.model.Akun;
import com.example.projectrplbo.model.JadwalOperasional;
import com.example.projectrplbo.model.Pengguna;

public class Chatbot {

    private boolean statusAktif;

    private final Orchestration orchestration;

    private final Retrieval retrieval;

    public Chatbot() {
        this.orchestration = new Orchestration();
        this.retrieval = new Retrieval();
        this.statusAktif = true;
    }

    public String prosesPesan(String input, Akun user) {
        if (!statusAktif) {
            return "⚙️ Sistem chatbot sedang dalam pemeliharaan. Silakan coba lagi nanti.";
        }

        JadwalOperasional jadwal = retrieval.getJadwalAktif();
        if (!jadwal.isOperasional()) {
            return String.format(
                "🕐 Maaf, QuickBite sedang tutup.%n" +
                "Jam operasional kami: %s - %s WIB.%n" +
                "Silakan kembali pada jam tersebut!",
                jadwal.getJamBukaFormatted(),
                jadwal.getJamTutupFormatted()
            );
        }

        try {
            var response = orchestration.executeBusinessLogic(input, user);
            return orchestration.formatFinalResponse(response);
        } catch (Exception e) {
            System.err.println("Chatbot error: " + e.getMessage());
            return "❌ Terjadi kesalahan internal. Silakan coba lagi.";
        }
    }

    public JadwalOperasional getJadwalAktif() {
        return retrieval.getJadwalAktif();
    }

    public void resetSesi() {
        orchestration.getKeranjang().clear();
        orchestration.setState(ConversationState.AWAL);
    }

    public boolean isStatusAktif() { return statusAktif; }
    public void setStatusAktif(boolean statusAktif) { this.statusAktif = statusAktif; }
    public Orchestration getOrchestration() { return orchestration; }
    public ConversationState getConversationState() { return orchestration.getState(); }
}
