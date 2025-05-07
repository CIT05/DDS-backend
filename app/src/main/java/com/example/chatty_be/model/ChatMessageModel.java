package com.example.chatty_be.model;

import com.google.firebase.Timestamp;

public class ChatMessageModel {
    private String message;
    private String sendeerId;
    private Timestamp timestamp;

    public ChatMessageModel() {
    }

    public ChatMessageModel(String message, String sendeerId, Timestamp timestamp) {
        this.message = message;
        this.sendeerId = sendeerId;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSendeerId() {
        return sendeerId;
    }

    public void setSendeerId(String sendeerId) {
        this.sendeerId = sendeerId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}


