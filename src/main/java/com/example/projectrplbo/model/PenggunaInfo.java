package com.example.projectrplbo.model;

public class PenggunaInfo {

    private final String idAkun;
    private final String idPengguna;
    private final String nama;
    private final String username;
    private final String email;
    private final String noTelepon;
    private final String tanggalDaftar;
    private String status;

    public PenggunaInfo(String idAkun, String idPengguna, String nama, String username,
                        String email, String noTelepon, String tanggalDaftar, String status) {
        this.idAkun       = idAkun;
        this.idPengguna   = idPengguna;
        this.nama         = nama;
        this.username     = username;
        this.email        = email;
        this.noTelepon    = noTelepon;
        this.tanggalDaftar = tanggalDaftar;
        this.status       = status;
    }

    public String getIdAkun()       { return idAkun; }
    public String getIdPengguna()   { return idPengguna; }
    public String getNama()         { return nama; }
    public String getUsername()     { return username; }
    public String getEmail()        { return email; }
    public String getNoTelepon()    { return noTelepon; }
    public String getTanggalDaftar(){ return tanggalDaftar; }
    public String getStatus()       { return status; }
    public void   setStatus(String s){ this.status = s; }
}
