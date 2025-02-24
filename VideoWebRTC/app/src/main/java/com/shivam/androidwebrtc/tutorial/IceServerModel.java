package com.shivam.androidwebrtc.tutorial;

import java.util.ArrayList;

public class IceServerModel {

    private ArrayList<String> urls;
    private String username;
    private String credential;

    public ArrayList<String> getUrls() {
        return urls;
    }

    public void setUrls(ArrayList<String> urls) {
        this.urls = urls;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    @Override
    public String toString() {
        return "IceServerModel{" +
                "urls=" + urls +
                ", username='" + username + '\'' +
                ", password='" + credential + '\'' +
                '}';
    }
}
