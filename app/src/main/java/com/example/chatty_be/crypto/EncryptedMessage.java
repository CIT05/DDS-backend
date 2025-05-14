package com.example.chatty_be.crypto;

public class EncryptedMessage {
    public final byte[] cipher;
    public final byte[] iv;
    public final int ratchetIndex;

    public EncryptedMessage(byte[] cipher, byte[] iv, int ratchetIndex) {
        this.cipher = cipher;
        this.iv = iv;
        this.ratchetIndex = ratchetIndex;
    }
}

