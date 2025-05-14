package com.example.chatty_be.crypto;

import java.security.KeyPair;
import java.security.KeyStore;

/** Creates or fetches the long-term identity key from AndroidKeyStore. */
public final class KeyManager {

    private static final String ALIAS = "identityKey";   // same alias you used earlier

    public static KeyPair getOrCreateIdentityKey() throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);

        if (ks.containsAlias(ALIAS)) {
            return new KeyPair(
                    ks.getCertificate(ALIAS).getPublicKey(),
                    (java.security.PrivateKey) ks.getKey(ALIAS, null));
        }
        return KeyGenerator.generateIdentityKey();        // the method you wrote
    }

    private KeyManager() {}
}

