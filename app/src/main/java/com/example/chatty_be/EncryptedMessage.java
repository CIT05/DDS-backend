package com.example.chatty_be;

public class EncryptedMessage {
    public final String ciphertext;
    public final String iv;
    public final String ephemeralPublicKey;

    public EncryptedMessage(String ciphertext, String iv, String ephemeralPublicKey) {
        this.ciphertext = ciphertext;
        this.iv = iv;
        this.ephemeralPublicKey = ephemeralPublicKey;
    }
}