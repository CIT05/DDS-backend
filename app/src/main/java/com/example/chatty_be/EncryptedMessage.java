package com.example.chatty_be;

public class EncryptedMessage {
    private String ciphertext;
    private String iv;
    private String ephemeralPublicKey;

    public String getCiphertext() {
        return ciphertext;
    }

    public void setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getEphemeralPublicKey() {
        return ephemeralPublicKey;
    }

    public void setEphemeralPublicKey(String ephemeralPublicKey) {
        this.ephemeralPublicKey = ephemeralPublicKey;
    }

    public EncryptedMessage(String ciphertext, String iv, String ephemeralPublicKey) {
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.ephemeralPublicKey = ephemeralPublicKey;
    }

    public EncryptedMessage() {
    }
}