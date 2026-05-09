package com.example.projectrplbo.chatbot;

public class Entity {
    private String idMenu;
    private String kategori;
    private int jumlah;
    private String rawInput;

    public Entity() {
        this.jumlah = 1;
    }

    public String getIdMenu() { return idMenu; }
    public void setIdMenu(String idMenu) { this.idMenu = idMenu; }
    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }
    public int getJumlah() { return jumlah; }
    public void setJumlah(int jumlah) { this.jumlah = jumlah; }
    public String getRawInput() { return rawInput; }
    public void setRawInput(String rawInput) { this.rawInput = rawInput; }
}
