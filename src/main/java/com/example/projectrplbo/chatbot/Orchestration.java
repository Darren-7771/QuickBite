package com.example.projectrplbo.chatbot;

import com.example.projectrplbo.db.Retrieval;
import com.example.projectrplbo.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Orchestration {

    private String intent;

    private final Retrieval retrieval;

    private ConversationState state = ConversationState.AWAL;

    private final Map<String, int[]> keranjang = new LinkedHashMap<>();
    private final Map<String, Menu> menuCache = new HashMap<>();

    private static final Pattern PATTERN_ID_MENU =
        Pattern.compile("\\b([A-Z]{3}\\d{5})\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_JUMLAH =
        Pattern.compile("\\b(\\d+)\\b");
    private static final Pattern PATTERN_PESAN =
        Pattern.compile("(?i)^PESAN\\s+(\\d+\\s+)?([A-Z]{3}\\d{5})$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_CARI =
        Pattern.compile("(?i)^CARI\\s+(.+)$", Pattern.CASE_INSENSITIVE);

    private static final List<String> KEYWORDS_MENU = List.of("menu", "daftar", "makanan", "apa saja", "lihat menu", "tampilkan menu");
    private static final List<String> KEYWORDS_STATUS = List.of("status", "riwayat", "pesanan saya", "history", "cek pesanan");
    private static final List<String> KEYWORDS_KONFIRMASI = List.of("ya", "iya", "yep", "oke", "ok", "benar", "konfirmasi", "pesan", "lanjut");
    private static final List<String> KEYWORDS_BATAL = List.of("batal", "cancel", "tidak", "ga", "gak", "jangan", "hapus");
    private static final List<String> KEYWORDS_SALAM = List.of("halo", "hai", "hello", "hi", "selamat", "pagi", "siang", "malam", "sore");
    private static final List<String> KEYWORDS_BANTUAN = List.of("bantuan", "help", "cara", "bisa apa", "panduan", "?");

    public Orchestration() {
        this.retrieval = new Retrieval();
        loadMenuCache();
    }

    private void loadMenuCache() {
        retrieval.getAllMenu().forEach(m -> menuCache.put(m.getIdMenu().toUpperCase(), m));
    }

    public void analyzeIntent(String input) {
        String normalized = input.trim().toUpperCase();
        String lower = input.trim().toLowerCase();

        if (normalized.equals("MENU"))                        { intent = Intent.LIHAT_MENU.name(); return; }
        if (normalized.startsWith("CARI "))                   { intent = Intent.CARI_MENU_KATEGORI.name(); return; }
        if (normalized.equals("STATUS"))                      { intent = Intent.RIWAYAT_PESANAN.name(); return; }
        if (normalized.equals("BATAL"))                       { intent = Intent.BATAL_PESANAN.name(); return; }
        if (PATTERN_PESAN.matcher(normalized).matches())      { intent = Intent.PESAN_MENU.name(); return; }

        if (state == ConversationState.PILIH_MENU && PATTERN_ID_MENU.matcher(normalized).find()) {
            intent = Intent.TAMBAH_ITEM.name(); return;
        }

        if (KEYWORDS_SALAM.stream().anyMatch(lower::contains))     { intent = Intent.SALAM.name(); return; }
        if (KEYWORDS_BANTUAN.stream().anyMatch(lower::contains))   { intent = Intent.BANTUAN.name(); return; }
        if (KEYWORDS_MENU.stream().anyMatch(lower::contains))      { intent = Intent.LIHAT_MENU.name(); return; }
        if (KEYWORDS_STATUS.stream().anyMatch(lower::contains))    { intent = Intent.RIWAYAT_PESANAN.name(); return; }
        if (KEYWORDS_BATAL.stream().anyMatch(lower::contains))     { intent = Intent.BATAL_PESANAN.name(); return; }
        if (KEYWORDS_KONFIRMASI.stream().anyMatch(lower::contains)
                && (state == ConversationState.KONFIRMASI || state == ConversationState.PILIH_MENU)) {
            intent = Intent.KONFIRMASI_PESANAN.name(); return;
        }

        if (lower.contains("stok") && PATTERN_ID_MENU.matcher(normalized).find()) {
            intent = Intent.TANYA_STOK.name(); return;
        }

        intent = Intent.TIDAK_DIKENALI.name();
    }

    public Entity extractEntity(String input) {
        Entity entity = new Entity();
        entity.setRawInput(input);
        String upper = input.trim().toUpperCase();

        Matcher mId = PATTERN_ID_MENU.matcher(upper);
        if (mId.find()) {
            entity.setIdMenu(mId.group(1).toUpperCase());
        }

        String withoutId = upper.replaceAll("[A-Z]{3}\\d{5}", "").replaceAll("[A-Z]+", "").trim();
        Matcher mJml = PATTERN_JUMLAH.matcher(withoutId);
        if (mJml.find()) {
            try { entity.setJumlah(Integer.parseInt(mJml.group(1))); } catch (NumberFormatException e) { }
        }

        Matcher mCari = PATTERN_CARI.matcher(input.trim());
        if (mCari.matches()) {
            String kat = mCari.group(1).trim();
            entity.setKategori(kat.substring(0, 1).toUpperCase() + kat.substring(1).toLowerCase());
        }

        return entity;
    }

    public Response executeBusinessLogic(String input, Akun akun) {
        if (!(akun instanceof Pengguna pengguna)) {
            return Response.badRequest("Fitur ini hanya untuk Pengguna.");
        }

        analyzeIntent(input);
        Entity entity = extractEntity(input);
        Intent intentEnum = Intent.valueOf(intent);

        return switch (intentEnum) {
            case SALAM          -> handleSalam(pengguna);
            case BANTUAN        -> handleBantuan();
            case LIHAT_MENU     -> handleLihatMenu();
            case CARI_MENU_KATEGORI -> handleCariMenu(entity);
            case TANYA_STOK     -> handleTanyaStok(entity);
            case PESAN_MENU, TAMBAH_ITEM -> handlePesanMenu(entity, pengguna);
            case KONFIRMASI_PESANAN -> handleKonfirmasiPesanan(pengguna);
            case BATAL_PESANAN  -> handleBatalPesanan();
            case RIWAYAT_PESANAN -> handleRiwayatPesanan(pengguna);
            default             -> handleTidakDikenali();
        };
    }

    public String formatFinalResponse(Response res) {
        return res.getMessage();
    }

    private Response handleSalam(Pengguna p) {
        String msg = "👋 Halo, " + p.getNama() + "! Selamat datang di QuickBite!\n\n"
            + "Saya siap membantu Anda memesan makanan.\n"
            + "Ketik *MENU* untuk melihat daftar menu lengkap, atau gunakan perintah di bawah ini.";
        return Response.ok(msg, null);
    }

    private Response handleBantuan() {
        String msg = """
            📖 *Panduan Perintah QuickBite:*

            > Ketik *MENU* untuk melihat daftar menu lengkap
            > Ketik *CARI [Kategori]* untuk mencari menu (contoh: CARI Ayam)
            > Ketik *PESAN [ID Menu]* untuk memesan (contoh: PESAN MKN00001)
            > Ketik *PESAN [Jumlah] [ID Menu]* untuk memesan banyak (contoh: PESAN 2 MKN00001)
            > Ketik *STATUS* untuk melihat riwayat pesanan
            > Ketik *BATAL* untuk membatalkan pesanan sementara
            """;
        return Response.ok(msg, null);
    }

    private Response handleLihatMenu() {
        List<Menu> menus = retrieval.getAllMenu();
        if (menus.isEmpty()) {
            return Response.notFound("Maaf, belum ada menu yang tersedia saat ini.");
        }
        StringBuilder sb = new StringBuilder("📋 *Daftar Menu QuickBite:*\n\n");
        String currentKat = "";
        for (Menu m : menus) {
            if (!m.getKategori().equals(currentKat)) {
                currentKat = m.getKategori();
                sb.append("— ").append(currentKat.toUpperCase()).append(" —\n");
            }
            sb.append(String.format("• *%s* - %s%n  Rp %,.0f | Stok: %d%n",
                m.getIdMenu(), m.getNamaMenu(), m.getHarga(), m.getStok()));
        }
        sb.append("\nKetik *PESAN [ID Menu]* untuk memesan.");
        menuCache.clear();
        menus.forEach(m -> menuCache.put(m.getIdMenu().toUpperCase(), m));
        return Response.ok(sb.toString(), menus);
    }

    private Response handleCariMenu(Entity entity) {
        if (entity.getKategori() == null) {
            return Response.badRequest("Format: CARI [Kategori]. Contoh: CARI Ayam");
        }
        List<Menu> menus = retrieval.getMenuByKategori(entity.getKategori());
        if (menus.isEmpty()) {
            return Response.notFound("😕 Tidak ada menu untuk kategori *" + entity.getKategori() + "*.\n"
                + "Coba kategori lain: Ayam, Burger, Minuman, Snack.");
        }
        StringBuilder sb = new StringBuilder("🔍 *Hasil pencarian: " + entity.getKategori() + "*\n\n");
        for (Menu m : menus) {
            sb.append(String.format("• *%s* - %s%n  Rp %,.0f | Stok: %d%n",
                m.getIdMenu(), m.getNamaMenu(), m.getHarga(), m.getStok()));
        }
        sb.append("\nKetik *PESAN [ID Menu]* untuk memesan.");
        return Response.ok(sb.toString(), menus);
    }

    private Response handleTanyaStok(Entity entity) {
        if (entity.getIdMenu() == null) {
            return Response.badRequest("Silakan masukkan ID menu. Contoh: stok MKN00001");
        }
        int stok = retrieval.checkStok(entity.getIdMenu());
        if (stok < 0) return Response.notFound("❌ Menu dengan ID *" + entity.getIdMenu() + "* tidak ditemukan.");
        Menu m = menuCache.get(entity.getIdMenu().toUpperCase());
        String nama = (m != null) ? m.getNamaMenu() : entity.getIdMenu();
        if (stok == 0) {
            return Response.ok("⚠️ Maaf, stok *" + nama + "* saat ini *habis*.", stok);
        }
        return Response.ok("✅ Stok *" + nama + "*: " + stok + " porsi tersedia.", stok);
    }

    private Response handlePesanMenu(Entity entity, Pengguna pengguna) {
        if (entity.getIdMenu() == null) {
            return Response.badRequest("Format salah. Gunakan: PESAN [ID Menu] atau PESAN [Jumlah] [ID Menu]\nContoh: PESAN MKN00001 atau PESAN 2 MKN00001");
        }
        String id = entity.getIdMenu().toUpperCase();
        Menu menu = menuCache.get(id);
        if (menu == null) {
            menu = retrieval.getMenuById(id);
            if (menu == null) return Response.notFound("❌ Menu *" + id + "* tidak ditemukan. Ketik MENU untuk melihat daftar.");
            menuCache.put(id, menu);
        }
        int stok = retrieval.checkStok(id);
        int req = entity.getJumlah();
        if (stok <= 0) {
            return Response.badRequest("⚠️ Maaf, *" + menu.getNamaMenu() + "* stoknya habis!");
        }
        if (req > stok) {
            return Response.badRequest("⚠️ Stok *" + menu.getNamaMenu() + "* tidak mencukupi. Tersedia: " + stok + " porsi.");
        }
        if (keranjang.containsKey(id)) {
            keranjang.get(id)[0] += req;
        } else {
            keranjang.put(id, new int[]{req});
        }
        state = ConversationState.KONFIRMASI;

        StringBuilder sb = new StringBuilder("🛒 *" + menu.getNamaMenu() + "* x" + req + " ditambahkan!\n\n");
        sb.append("*Pesanan sementara:*\n");
        sb.append(formatKeranjang());
        sb.append("\n\nKetik *PESAN [ID]* untuk tambah item lagi, atau ketik *YA* / *OK* / *LANJUT* untuk konfirmasi pesanan, atau *BATAL* untuk membatalkan.");
        return Response.ok(sb.toString(), keranjang);
    }

    private Response handleKonfirmasiPesanan(Pengguna pengguna) {
        if (keranjang.isEmpty()) {
            state = ConversationState.AWAL;
            return Response.badRequest("Keranjang Anda kosong. Ketik MENU untuk mulai memesan.");
        }
        Pesanan pesanan = new Pesanan();
        pesanan.setTanggal(LocalDateTime.now());
        pesanan.setStatus("MENUNGGU");
        pesanan.setIdPengguna(pengguna.getIdPengguna());

        double total = 0;
        for (Map.Entry<String, int[]> entry : keranjang.entrySet()) {
            Menu m = menuCache.get(entry.getKey());
            if (m == null) continue;
            int jml = entry.getValue()[0];
            double subtotal = m.getHarga() * jml;
            total += subtotal;
            Pesanan.DetailPesanan detail = new Pesanan.DetailPesanan(
                pesanan.getIdPesanan(), entry.getKey(), jml, subtotal
            );
            detail.setNamaMenu(m.getNamaMenu());
            detail.setHargaSatuan(m.getHarga());
            pesanan.addDetail(detail);
        }
        pesanan.setTotalHarga(total);

        boolean saved = retrieval.savePesanan(pesanan);
        if (!saved) {
            return Response.serverError("❌ Terjadi kesalahan saat menyimpan pesanan. Silakan coba lagi.");
        }
        for (Pesanan.DetailPesanan d : pesanan.getDetails()) {
            d.setIdPesanan(pesanan.getIdPesanan());
        }

        keranjang.clear();
        state = ConversationState.AWAL;

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        StringBuilder sb = new StringBuilder();
        sb.append("✅ *Pesanan berhasil dikonfirmasi!*\n\n");
        sb.append("📋 *Nomor Pesanan: ").append(pesanan.getIdPesanan()).append("*\n");
        sb.append("📅 Tanggal: ").append(pesanan.getTanggal().format(fmt)).append("\n\n");
        sb.append("*Detail Pesanan:*\n");
        for (Pesanan.DetailPesanan d : pesanan.getDetails()) {
            sb.append(String.format("• %s x%d = Rp %,.0f%n", d.getNamaMenu(), d.getJumlah(), d.getSubtotal()));
        }
        sb.append(String.format("%n💰 *Total: Rp %,.0f*%n", total));
        sb.append("\nTerima kasih telah memesan di QuickBite! 🎉");
        return Response.ok(sb.toString(), pesanan);
    }

    private Response handleBatalPesanan() {
        if (keranjang.isEmpty() && state == ConversationState.AWAL) {
            return Response.ok("Tidak ada pesanan aktif untuk dibatalkan.", null);
        }
        keranjang.clear();
        state = ConversationState.AWAL;
        return Response.ok("🚫 Pesanan dibatalkan. Keranjang dikosongkan.\nKetik MENU untuk mulai pesan kembali.", null);
    }

    private Response handleRiwayatPesanan(Pengguna pengguna) {
        List<Pesanan> list = retrieval.getPesananByUser(pengguna.getIdPengguna());
        if (list.isEmpty()) {
            return Response.ok("📭 Anda belum memiliki riwayat pesanan.", list);
        }
        StringBuilder sb = new StringBuilder("📦 *Riwayat 10 Pesanan Terakhir:*\n\n");
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        for (Pesanan p : list) {
            sb.append(String.format("• *%s* - %s%n  Rp %,.0f | Status: %s%n",
                p.getIdPesanan(), p.getTanggal().format(fmt), p.getTotalHarga(), p.getStatus()));
        }
        return Response.ok(sb.toString(), list);
    }

    private Response handleTidakDikenali() {
        if (state == ConversationState.PILIH_MENU) {
            return Response.badRequest("Saya tidak mengerti. Apakah Anda ingin:\n"
                + "• Tambah item? Ketik *PESAN [ID Menu]*\n"
                + "• Konfirmasi? Ketik *YA*\n"
                + "• Batalkan? Ketik *BATAL*");
        }
        return Response.badRequest("🤔 Maaf, saya tidak mengerti perintah itu.\n"
            + "Ketik *BANTUAN* atau *HELP* untuk melihat daftar perintah.");
    }

    private String formatKeranjang() {
        if (keranjang.isEmpty()) return "_(kosong)_";
        StringBuilder sb = new StringBuilder();
        double total = 0;
        for (Map.Entry<String, int[]> entry : keranjang.entrySet()) {
            Menu m = menuCache.get(entry.getKey());
            if (m == null) continue;
            int jml = entry.getValue()[0];
            double subtotal = m.getHarga() * jml;
            total += subtotal;
            sb.append(String.format("  • %s x%d = Rp %,.0f%n", m.getNamaMenu(), jml, subtotal));
        }
        sb.append(String.format("  ━━━━━━━━━━━━%n  *Total: Rp %,.0f*", total));
        return sb.toString();
    }

    public ConversationState getState() { return state; }
    public void setState(ConversationState state) { this.state = state; }
    public Map<String, int[]> getKeranjang() { return keranjang; }
    public String getIntent() { return intent; }
    public Map<String, Menu> getMenuCache() { return menuCache; }
}
