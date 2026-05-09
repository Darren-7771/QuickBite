package com.example.projectrplbo.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Pesanan {

    private String idPesanan;

    private LocalDateTime tanggal;

    private double totalHarga;

    private String status;

    private String idPengguna;

    private List<DetailPesanan> details = new ArrayList<>();

    public Pesanan() {}

    public Pesanan(String idPesanan, LocalDateTime tanggal, double totalHarga,
                   String status, String idPengguna) {

        this.idPesanan = idPesanan;

        this.tanggal = tanggal;

        this.totalHarga = totalHarga;

        this.status = status;

        this.idPengguna = idPengguna;

    }

    public double hitungTotal() {

        double total = 0;

        for (DetailPesanan d : details) {

            total += d.getSubtotal();

        }

        this.totalHarga = total;

        return total;

    }

    public static class DetailPesanan {

        private String idPesanan;

        private String idMenu;

        private int jumlah;

        private double subtotal;

        private String namaMenu;

        private double hargaSatuan;

        public DetailPesanan() {}

        public DetailPesanan(String idPesanan, String idMenu, int jumlah, double subtotal) {

            this.idPesanan = idPesanan;

            this.idMenu = idMenu;

            this.jumlah = jumlah;

            this.subtotal = subtotal;

        }

        public String getIdPesanan() { return idPesanan; }

        public void setIdPesanan(String idPesanan) { this.idPesanan = idPesanan; }

        public String getIdMenu() { return idMenu; }

        public void setIdMenu(String idMenu) { this.idMenu = idMenu; }

        public int getJumlah() { return jumlah; }

        public void setJumlah(int jumlah) { this.jumlah = jumlah; }

        public double getSubtotal() { return subtotal; }

        public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

        public String getNamaMenu() { return namaMenu; }

        public void setNamaMenu(String namaMenu) { this.namaMenu = namaMenu; }

        public double getHargaSatuan() { return hargaSatuan; }

        public void setHargaSatuan(double hargaSatuan) { this.hargaSatuan = hargaSatuan; }

    }

    public String getIdPesanan() { return idPesanan; }

    public void setIdPesanan(String idPesanan) { this.idPesanan = idPesanan; }

    public LocalDateTime getTanggal() { return tanggal; }

    public void setTanggal(LocalDateTime tanggal) { this.tanggal = tanggal; }

    public double getTotalHarga() { return totalHarga; }

    public void setTotalHarga(double totalHarga) { this.totalHarga = totalHarga; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getIdPengguna() { return idPengguna; }

    public void setIdPengguna(String idPengguna) { this.idPengguna = idPengguna; }

    public List<DetailPesanan> getDetails() { return details; }

    public void setDetails(List<DetailPesanan> details) { this.details = details; }

    public void addDetail(DetailPesanan detail) { this.details.add(detail); }

}