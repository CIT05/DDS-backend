package com.example.chatty_be.crypto;

import static com.example.chatty_be.utils.KdfUtils.hmacSha256;

import com.example.chatty_be.utils.KdfUtils;

import java.security.GeneralSecurityException;
import java.util.Arrays;

public class ChainKey {

    private byte[] ck;

    public ChainKey(byte[] sharedSecret) throws GeneralSecurityException {
        this.ck = KdfUtils.hmacSha256(sharedSecret, "init");
    }

    public byte[] nextMessageKey() throws GeneralSecurityException {
        byte[] mk = KdfUtils.hmacSha256(ck, "message");
        ck        = KdfUtils.hmacSha256(ck, "ratchet");
        return mk;
    }

    public void wipe() {
        Arrays.fill(ck, (byte) 0);
    }

}
