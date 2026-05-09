package com.example.projectrplbo.model;

public class Admin extends Akun {
    private String idAdmin;
    private String idAkun;
    private String namaAdmin;
    private final String role = "Admin";

    public Admin() {}

    public Admin(String idAkun, String username, String password,
                 String idAdmin, String namaAdmin) {
        super(username, password);
        this.idAkun = idAkun;
        this.idAdmin = idAdmin;
        this.namaAdmin = namaAdmin;
    }

    @Override
    public boolean login() {
        return false;
    }

    @Override
    public void logout() {}

    public void kelolaMenu() {}
    public void kelolaUser() {}

    public String getIdAdmin() { return idAdmin; }
    public void setIdAdmin(String idAdmin) { this.idAdmin = idAdmin; }
    public String getIdAkun() { return idAkun; }
    public void setIdAkun(String idAkun) { this.idAkun = idAkun; }
    public String getNamaAdmin() { return namaAdmin; }
    public void setNamaAdmin(String namaAdmin) { this.namaAdmin = namaAdmin; }
    public String getRole() { return role; }
}
