package com.server;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class WarningMessage {

    private String nickname;
    private String dangertype;
    private Double latitude;
    private Double longitude;
    private LocalDateTime sent;
    private String areacode;
    private String phonenumber;

    public WarningMessage(String nickname, String dangertype, Double latitude, Double longitude, LocalDateTime sent,
            String areacode, String phonenumber) {
        this.nickname = nickname;
        this.dangertype = dangertype;
        this.latitude = latitude;
        this.longitude = longitude;
        this.sent = sent;
        this.areacode = areacode;
        this.phonenumber = phonenumber;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getNickname() {
        return nickname;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setDangertype(String dangertype) {
        this.dangertype = dangertype;
    }

    public String getDangertype() {
        return dangertype;
    }

    public String getAreacode() {
        return areacode;
    }

    public void setAreacode(String areacode) {
        this.areacode = areacode;
    }

    public String getPhonenumber() {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber) {
        this.phonenumber = phonenumber;
    }

    public void setSent(long epoch) {
        sent = LocalDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    }

    public LocalDateTime getSent() {
        return sent;
    }

    public long dateAsInt() {
        return sent.toInstant(ZoneOffset.UTC).toEpochMilli();
    }
}