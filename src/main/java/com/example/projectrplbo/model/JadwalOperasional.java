package com.example.projectrplbo.model;

import java.time.LocalTime;

public class JadwalOperasional {
    private int idJadwal;
    private LocalTime jamBuka;
    private LocalTime jamTutup;
    private String hari;

    public JadwalOperasional() {}

    public JadwalOperasional(int idJadwal, LocalTime jamBuka, LocalTime jamTutup, String hari) {
        this.idJadwal = idJadwal;
        this.jamBuka = jamBuka;
        this.jamTutup = jamTutup;
        this.hari = hari;
    }

    public boolean isOperasional() {
        LocalTime sekarang = LocalTime.now();
        return !sekarang.isBefore(jamBuka) && !sekarang.isAfter(jamTutup);
    }

    public String getJamBukaFormatted() {
        return jamBuka != null ? String.format("%02d:%02d", jamBuka.getHour(), jamBuka.getMinute()) : "--:--";
    }

    public String getJamTutupFormatted() {
        return jamTutup != null ? String.format("%02d:%02d", jamTutup.getHour(), jamTutup.getMinute()) : "--:--";
    }

    public int getIdJadwal() { return idJadwal; }
    public void setIdJadwal(int idJadwal) { this.idJadwal = idJadwal; }
    public LocalTime getJamBuka() { return jamBuka; }
    public void setJamBuka(LocalTime jamBuka) { this.jamBuka = jamBuka; }
    public LocalTime getJamTutup() { return jamTutup; }
    public void setJamTutup(LocalTime jamTutup) { this.jamTutup = jamTutup; }
    public String getHari() { return hari; }
    public void setHari(String hari) { this.hari = hari; }
}
