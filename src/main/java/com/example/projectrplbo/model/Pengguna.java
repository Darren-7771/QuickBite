package com.example.projectrplbo.model;

public class Pengguna extends Akun {

    private String idPengguna;

    private String idAkun;

    private String nama;

    private String alamat;

    private String email;

    private String noTelepon;

    private final String role = "Pengguna";

    public Pengguna() {}

    public Pengguna(String idAkun, String username, String password,

                    String idPengguna, String nama, String alamat,

                    String email, String noTelepon) {

        super(username, password);

        this.idAkun = idAkun;

        this.idPengguna = idPengguna;

        this.nama = nama;

        this.alamat = alamat;

        this.email = email;

        this.noTelepon = noTelepon;

    }

    @Override

    public boolean login() {

        return false;

    }

    @Override

    public void logout() {

    }

    public void registrasi() {

    }

    public void updateProfil() {

    }

    public String getIdPengguna() { return idPengguna; }

    public void setIdPengguna(String idPengguna) { this.idPengguna = idPengguna; }

    public String getIdAkun() { return idAkun; }

    public void setIdAkun(String idAkun) { this.idAkun = idAkun; }

    public String getNama() { return nama; }

    public void setNama(String nama) { this.nama = nama; }

    public String getAlamat() { return alamat; }

    public void setAlamat(String alamat) { this.alamat = alamat; }

    public String getEmail() { return email; }

    public void setEmail(String email) { this.email = email; }

    public String getNoTelepon() { return noTelepon; }

    public void setNoTelepon(String noTelepon) { this.noTelepon = noTelepon; }

    public String getRole() { return role; }

}