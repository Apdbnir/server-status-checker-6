package com.example.serverstatuschecker.dto;

import lombok.Data;

@Data
public class ServerStatusDto {
    private String url;
    private boolean isAvailable;
    private String message;

    public void setIsAvailable(boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

}