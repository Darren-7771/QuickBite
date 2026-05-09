package com.example.projectrplbo.model;

public class Menu {
    private String idMenu;
    private String namaMenu;
    private String kategori;
    private double harga;
    private int stok;

    public Menu() {}

    public Menu(String idMenu, String namaMenu, String kategori, double harga, int stok) {
        this.idMenu = idMenu;
        this.namaMenu = namaMenu;
        this.kategori = kategori;
        this.harga = harga;
        this.stok = stok;
    }

    public void updateStok(int jumlah) {
        this.stok += jumlah;
    }

    public String getIdMenu() { return idMenu; }
    public void setIdMenu(String idMenu) { this.idMenu = idMenu; }
    public String getNamaMenu() { return namaMenu; }
    public void setNamaMenu(String namaMenu) { this.namaMenu = namaMenu; }
    public String getKategori() { return kategori; }
    public void setKategori(String kategori) { this.kategori = kategori; }
    public double getHarga() { return harga; }
    public void setHarga(double harga) { this.harga = harga; }
    public int getStok() { return stok; }
    public void setStok(int stok) { this.stok = stok; }

    @Override
    public String toString() {
        return idMenu + " - " + namaMenu + " | " + kategori + " | Rp " + String.format("%.0f", harga) + " | Stok: " + stok;
    }
}
