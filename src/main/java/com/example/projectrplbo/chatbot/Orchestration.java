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
    private List<String> kategoriTersedia = new ArrayList<>();

    private final Map<String, List<String>> intentPatterns = new HashMap<>();

    private static final Pattern PATTERN_ID_MENU =
        Pattern.compile("\\b([A-Za-z]{3}\\d{5})\\b", Pattern.CASE_INSENSITIVE);

    private static final Pattern PATTERN_JUMLAH =
        Pattern.compile("\\b(\\d+)\\b");

    private static final Pattern PATTERN_CARI_EXPLICIT =
        Pattern.compile("(?i)^cari\\s+(.+)$");

    private static final Map<String, String> KATA_KE_KATEGORI = Map.of(
        "ayam",    "Ayam",
        "chicken", "Ayam",
        "burger",  "Burger",
        "minuman", "Minuman",
        "minum",   "Minuman",
        "drink",   "Minuman",
        "snack",   "Snack",
        "camilan", "Snack",
        "makanan ringan", "Snack"
    );

    public Orchestration() {
        this.retrieval = new Retrieval();
        loadMenuCache();
        loadKategori();
        loadIntentPatterns();
    }

    private void loadMenuCache() {
        retrieval.getAllMenu().forEach(m -> menuCache.put(m.getIdMenu().toUpperCase(), m));
    }

    private void loadKategori() {
        kategoriTersedia = retrieval.getAllKategori();
    }

    private void loadIntentPatterns() {
        for (Intent i : Intent.values()) {
            List<String> patterns = retrieval.getPatternsByIntent(i.name());
            patterns.sort((a, b) -> Integer.compare(b.length(), a.length()));
            intentPatterns.put(i.name(), patterns);
        }
    }

    public void analyzeIntent(String input) {
        String lower = input.trim().toLowerCase();

        if (state == ConversationState.PILIH_MENU
                && PATTERN_ID_MENU.matcher(input).find()) {
            intent = Intent.TAMBAH_ITEM.name();
            return;
        }

        if (matchesAnyPattern(lower, Intent.BATAL_PESANAN.name())) {
            intent = Intent.BATAL_PESANAN.name();
            return;
        }

        if (state == ConversationState.KONFIRMASI
                || state == ConversationState.PILIH_MENU) {
            if (matchesAnyPattern(lower, Intent.KONFIRMASI_PESANAN.name())) {
                intent = Intent.KONFIRMASI_PESANAN.name();
                return;
            }
        }

        String[] intentOrder = {
            Intent.LIHAT_PESANAN_SEMENTARA.name(),
            Intent.RIWAYAT_PESANAN.name(),
            Intent.TANYA_STOK.name(),
            Intent.PESAN_MENU.name(),
            Intent.CARI_MENU_KATEGORI.name(),
            Intent.LIHAT_MENU.name(),
            Intent.BANTUAN.name(),
            Intent.SALAM.name()
        };

        for (String intentName : intentOrder) {
            if (matchesAnyPattern(lower, intentName)) {
                intent = intentName;
                return;
            }
        }

        intent = Intent.TIDAK_DIKENALI.name();
    }

    private boolean matchesAnyPattern(String lowerInput, String intentName) {
        List<String> patterns = intentPatterns.getOrDefault(intentName, List.of());
        return patterns.stream().anyMatch(p -> matchesPattern(lowerInput, p));
    }

    private boolean matchesPattern(String lowerInput, String pattern) {

        if (pattern.contains(" ")) {
            return lowerInput.contains(pattern);
        }

        Pattern wp = Pattern.compile(
            "(?<![a-z])" + Pattern.quote(pattern) + "(?![a-z])"
        );
        return wp.matcher(lowerInput).find();
    }

    public Entity extractEntity(String input) {
        Entity entity = new Entity();
        entity.setRawInput(input);
        String upper = input.trim().toUpperCase();
        String lower = input.trim().toLowerCase();

        Matcher mId = PATTERN_ID_MENU.matcher(upper);
        if (mId.find()) {
            entity.setIdMenu(mId.group(1).toUpperCase());
        }

        String tanpaId = upper.replaceAll("[A-Z]{3}\\d{5}", "")
                              .replaceAll("[A-Z]+", "").trim();
        Matcher mJml = PATTERN_JUMLAH.matcher(tanpaId);
        if (mJml.find()) {
            try { entity.setJumlah(Integer.parseInt(mJml.group(1))); }
            catch (NumberFormatException ignored) { }
        }

        Matcher mCari = PATTERN_CARI_EXPLICIT.matcher(input.trim());
        if (mCari.matches()) {
            String kat = mCari.group(1).trim();
            entity.setKategori(capitalize(kat));
        } else {
            for (Map.Entry<String, String> entry : KATA_KE_KATEGORI.entrySet()) {
                if (lower.contains(entry.getKey())) {
                    entity.setKategori(entry.getValue());
                    break;
                }
            }
            if (entity.getKategori() == null) {
                for (String kat : kategoriTersedia) {
                    if (lower.contains(kat.toLowerCase())) {
                        entity.setKategori(kat);
                        break;
                    }
                }
            }
        }

        return entity;
    }

    public Response executeBusinessLogic(String input, Akun akun) {
        if (!(akun instanceof Pengguna pengguna)) {
            return Response.badRequest("Fitur ini hanya untuk Pengguna.");
        }

        analyzeIntent(input);
        Entity entity = extractEntity(input);
        Intent intentEnum;
        try {
            intentEnum = Intent.valueOf(intent);
        } catch (IllegalArgumentException e) {
            intentEnum = Intent.TIDAK_DIKENALI;
        }

        return switch (intentEnum) {
            case SALAM                   -> handleSalam(pengguna);
            case BANTUAN                 -> handleBantuan();
            case LIHAT_MENU              -> handleLihatMenu();
            case CARI_MENU_KATEGORI      -> handleCariMenu(entity, input);
            case TANYA_STOK              -> handleTanyaStok(entity);
            case PESAN_MENU, TAMBAH_ITEM -> handlePesanMenu(entity, pengguna);
            case KONFIRMASI_PESANAN      -> handleKonfirmasiPesanan(pengguna);
            case BATAL_PESANAN           -> handleBatalPesanan();
            case RIWAYAT_PESANAN         -> handleRiwayatPesanan(pengguna);
            case LIHAT_PESANAN_SEMENTARA -> handleLihatKeranjang();
            default                      -> handleTidakDikenali();
        };
    }

    public String formatFinalResponse(Response res) {
        return res.getMessage();
    }

    private Response handleSalam(Pengguna p) {
        String msg = "👋 Halo, " + p.getNama() + "! Selamat datang di QuickBite!\n\n"
            + "Saya siap membantu Anda memesan makanan favorit Anda.\n"
            + "Tanyakan menu, cari berdasarkan kategori, atau langsung pesan!";
        return Response.ok(msg, null);
    }

    private Response handleBantuan() {
        String kategoriStr = kategoriTersedia.isEmpty() ? "Ayam, Burger, Minuman, Snack"
            : String.join(", ", kategoriTersedia);
        String msg = """
            📖 *Panduan QuickBite:*

            > Tanya menu  - "Menu apa saja yang tersedia?"
            > Cari kategori - "Ada menu ayam apa?" / "Tampilkan burger"
            > Pesan - "Saya mau pesan MKN00001" / "Pesan 2 MKN00001"
            > Cek stok - "Masih ada MKN00001?" / "Cek stok MKN00001"
            > Keranjang - "Lihat keranjang saya" / "Berapa totalnya?"
            > Riwayat - "Pesanan saya" / "Lihat history order"
            > Batalkan - "Batalkan pesanan" / "Tidak jadi"
            > Konfirmasi - "Ya, lanjut" / "Konfirmasi pesanan"

            Kategori tersedia: """ + kategoriStr;
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
                sb.append("- ").append(currentKat.toUpperCase()).append(" -\n");
            }
            sb.append(String.format("• *%s* - %s%n  Rp %,.0f | Stok: %d%n",
                m.getIdMenu(), m.getNamaMenu(), m.getHarga(), m.getStok()));
        }
        sb.append("\nTanyakan: \"Saya mau pesan [ID Menu]\"");
        menuCache.clear();
        menus.forEach(m -> menuCache.put(m.getIdMenu().toUpperCase(), m));
        return Response.ok(sb.toString(), menus);
    }

    private Response handleCariMenu(Entity entity, String rawInput) {
        if (entity.getKategori() == null) {
            String lower = rawInput.toLowerCase();
            for (String kat : kategoriTersedia) {
                if (lower.contains(kat.toLowerCase())) {
                    entity.setKategori(kat);
                    break;
                }
            }
        }
        if (entity.getKategori() == null) {
            String daftar = kategoriTersedia.isEmpty() ? "Ayam, Burger, Minuman, Snack"
                : String.join(", ", kategoriTersedia);
            return Response.badRequest(
                "Sebutkan kategori yang ingin dicari.\n"
                + "Contoh: \"Ada menu ayam apa?\" atau \"Tampilkan burger\"\n"
                + "Kategori tersedia: " + daftar);
        }
        List<Menu> menus = retrieval.getMenuByKategori(entity.getKategori());
        if (menus.isEmpty()) {
            String daftar = String.join(", ", kategoriTersedia);
            return Response.notFound("😕 Tidak ada menu untuk kategori *" + entity.getKategori() + "*.\n"
                + "Kategori tersedia: " + daftar);
        }
        StringBuilder sb = new StringBuilder("🔍 *Hasil: " + entity.getKategori() + "*\n\n");
        for (Menu m : menus) {
            sb.append(String.format("• *%s* - %s%n  Rp %,.0f | Stok: %d%n",
                m.getIdMenu(), m.getNamaMenu(), m.getHarga(), m.getStok()));
        }
        sb.append("\nTanyakan: \"Saya mau pesan [ID Menu]\"");
        return Response.ok(sb.toString(), menus);
    }

    private Response handleTanyaStok(Entity entity) {
        if (entity.getIdMenu() == null) {
            return Response.badRequest(
                "Sebutkan ID menu yang ingin dicek stoknya.\n"
                + "Contoh: \"Masih ada MKN00001?\" atau \"Cek stok MKN00001\"");
        }
        int stok = retrieval.checkStok(entity.getIdMenu());
        if (stok < 0) {
            return Response.notFound("❌ Menu dengan ID *" + entity.getIdMenu() + "* tidak ditemukan.");
        }
        Menu m = menuCache.get(entity.getIdMenu().toUpperCase());
        String nama = (m != null) ? m.getNamaMenu() : entity.getIdMenu();
        if (stok == 0) {
            return Response.ok("⚠ Maaf, stok *" + nama + "* saat ini *habis*.", stok);
        }
        return Response.ok("✅ Stok *" + nama + "*: " + stok + " porsi tersedia.", stok);
    }

    private Response handlePesanMenu(Entity entity, Pengguna pengguna) {
        if (entity.getIdMenu() == null) {
            return Response.badRequest(
                "Sebutkan ID menu yang ingin dipesan.\n"
                + "Contoh: \"Saya mau pesan MKN00001\" atau \"Pesan 2 MKN00001\"\n"
                + "Ketik menu untuk melihat daftar ID menu.");
        }
        String id = entity.getIdMenu().toUpperCase();
        Menu menu = menuCache.get(id);
        if (menu == null) {
            menu = retrieval.getMenuById(id);
            if (menu == null) {
                return Response.notFound("❌ Menu *" + id + "* tidak ditemukan. Tanyakan \"menu apa saja\" untuk melihat daftar.");
            }
            menuCache.put(id, menu);
        }
        int stok = retrieval.checkStok(id);
        int req  = entity.getJumlah();
        if (stok <= 0) {
            return Response.badRequest("⚠ Maaf, *" + menu.getNamaMenu() + "* stoknya habis!");
        }
        if (req > stok) {
            return Response.badRequest("⚠ Stok *" + menu.getNamaMenu()
                + "* tidak mencukupi. Tersedia: " + stok + " porsi.");
        }
        if (keranjang.containsKey(id)) {
            keranjang.get(id)[0] += req;
        } else {
            keranjang.put(id, new int[]{req});
        }
        state = ConversationState.KONFIRMASI;

        StringBuilder sb = new StringBuilder("🛒 *" + menu.getNamaMenu() + "* x" + req + " ditambahkan!\n\n");
        sb.append(formatKeranjang());
        sb.append("\n\nIngin tambah lagi? Katakan \"Saya mau pesan [ID Menu]\" lagi.\n"
            + "Atau katakan \"Ya, lanjut\" untuk konfirmasi, \"Batalkan\" untuk membatalkan.");
        return Response.ok(sb.toString(), keranjang);
    }

    private Response handleLihatKeranjang() {
        if (keranjang.isEmpty()) {
            return Response.ok("Keranjang Anda kosong.\nTanyakan \"menu apa saja\" untuk mulai memilih.", null);
        }
        return Response.ok("🛒 *Pesanan sementara Anda:*\n\n" + formatKeranjang()
            + "\n\nKatakan \"Ya, konfirmasi\" untuk memesan atau \"Batalkan\" untuk membatalkan.", keranjang);
    }

    private Response handleKonfirmasiPesanan(Pengguna pengguna) {
        if (keranjang.isEmpty()) {
            state = ConversationState.AWAL;
            return Response.badRequest("Keranjang Anda kosong. Tanyakan \"menu apa saja\" untuk mulai memesan.");
        }
        Pesanan pesanan = new Pesanan();
        pesanan.setTanggal(LocalDateTime.now());
        pesanan.setStatus("MENUNGGU");
        pesanan.setIdPengguna(pengguna.getIdPengguna());

        double total = 0;
        for (Map.Entry<String, int[]> entry : keranjang.entrySet()) {
            Menu m = menuCache.get(entry.getKey());
            if (m == null) continue;
            int jml       = entry.getValue()[0];
            double sub    = m.getHarga() * jml;
            total        += sub;
            Pesanan.DetailPesanan detail = new Pesanan.DetailPesanan(
                pesanan.getIdPesanan(), entry.getKey(), jml, sub);
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
            return Response.ok("Tidak ada pesanan aktif untuk dibatalkan.\nTanyakan \"menu apa saja\" untuk mulai memesan.", null);
        }
        keranjang.clear();
        state = ConversationState.AWAL;
        return Response.ok("🚫 Pesanan dibatalkan. Keranjang dikosongkan.\nTanyakan \"menu apa saja\" untuk mulai pesan kembali.", null);
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
        if (state == ConversationState.PILIH_MENU || state == ConversationState.KONFIRMASI) {
            return Response.badRequest(
                "Saya tidak mengerti. Apakah maksud Anda:\n"
                + "• Tambah item? - \"Saya mau pesan [ID Menu]\"\n"
                + "• Konfirmasi? - \"Ya, lanjutkan pesanan\"\n"
                + "• Batalkan? - \"Batalkan pesanan saya\"");
        }
        return Response.badRequest(
            "🤔 Maaf, saya belum memahami pertanyaan Anda.\n"
            + "Coba tanyakan: \"Bantuan\" atau \"Panduan\" untuk melihat cara penggunaan.");
    }

    private String formatKeranjang() {
        if (keranjang.isEmpty()) return "_(kosong)_";
        StringBuilder sb = new StringBuilder("*Pesanan sementara:*\n");
        double total = 0;
        for (Map.Entry<String, int[]> entry : keranjang.entrySet()) {
            Menu m = menuCache.get(entry.getKey());
            if (m == null) continue;
            int jml       = entry.getValue()[0];
            double sub    = m.getHarga() * jml;
            total        += sub;
            sb.append(String.format("  • %s x%d = Rp %,.0f%n", m.getNamaMenu(), jml, sub));
        }
        sb.append(String.format("  ━━━━━━━━━━━━%n  *Total: Rp %,.0f*", total));
        return sb.toString();
    }

    private String capitalize(String s) {
        if (s == null || s.isBlank()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    public ConversationState getState()                    { return state; }
    public void setState(ConversationState state)          { this.state = state; }
    public Map<String, int[]> getKeranjang()               { return keranjang; }
    public String getIntent()                              { return intent; }
    public Map<String, Menu> getMenuCache()                { return menuCache; }
    public Map<String, List<String>> getIntentPatterns()   { return intentPatterns; }
}
