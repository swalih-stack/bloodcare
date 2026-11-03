package com.bloodcare.db;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Passwords - never store a plain password. We store "salt:hash" where
 * hash = SHA-256(salt + password). The random salt makes equal passwords
 * produce different hashes.
 */
public final class Passwords {
    private Passwords() {}

    public static String hash(String password) {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String saltText = Base64.getEncoder().encodeToString(salt);
        return saltText + ":" + digest(saltText, password);
    }

    /** True if the typed password matches the stored "salt:hash" value. */
    public static boolean matches(String password, String stored) {
        String[] parts = stored.split(":", 2);
        return parts.length == 2 && digest(parts[0], password).equals(parts[1]);
    }

    private static String digest(String salt, String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(out);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 is always present in Java
        }
    }
}
