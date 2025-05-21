package com.example.chatty_be.model;

import com.example.chatty_be.EncryptedMessage;
import com.google.firebase.Timestamp;

public class ChatMessageModel {
    private String message;

    private EncryptedMessage encryptedMessage;
    private String sendeerId;
    private Timestamp timestamp;

    public ChatMessageModel() {
    }

    public ChatMessageModel(String message, String sendeerId, Timestamp timestamp) {
        this.message = message;
        this.sendeerId = sendeerId;
        this.timestamp = timestamp;
    }

    public ChatMessageModel(EncryptedMessage encryptedMessage, String sendeerId, Timestamp timestamp) {
        this.encryptedMessage = encryptedMessage;
        this.sendeerId = sendeerId;
        this.timestamp = timestamp;
    }

    public ChatMessageModel(String message, EncryptedMessage encryptedMessage, String sendeerId, Timestamp timestamp) {
        this.message = message;
        this.encryptedMessage = encryptedMessage;
        this.sendeerId = sendeerId;
        this.timestamp = timestamp;
    }

    public EncryptedMessage getEncryptedMessage() {
        return encryptedMessage;
    }

    public void setEncryptedMessage(EncryptedMessage encryptedMessage) {
        this.encryptedMessage = encryptedMessage;
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


