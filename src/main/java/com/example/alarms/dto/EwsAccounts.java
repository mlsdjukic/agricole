package com.example.alarms.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EwsAccounts {
    private String type;
    private String url;
    private String username;

    public EwsAccounts(String type, String url, String username) {
        this.type = type;
        this.url = url;
        this.username = username;    }
}
