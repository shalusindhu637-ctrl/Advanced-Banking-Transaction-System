package com.banking.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transaction PIN hashing/verification. Separate from login password.
 * Uses PBKDF2WithHmacSHA256 with per-PIN random salt (suitable KDF).
 * Never log PINs or hashes.
 */
public class PinUtil {
    private static final Logger LOG = Logger.getLogger(PinUtil.class.getName());
    private static final int ITERATIONS = 120000;
    private static final int KEY_LENGTH = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final SecureRandom RNG = new SecureRandom();

    private PinUtil() {}

    /** Normalize user-entered PIN: trim surrounding whitespace; preserves all digits (incl. leading zeros). */
    private static String normalize(String pin) {
        if (pin == null) return null;
        String t = pin.trim();
        // remove common invisible/format chars that can be introduced by copy-paste/IME
        t = t.replace("\u00A0", "").replace("\u200B", "").replace("\u200C", "").replace("\u200D", "").replace("\uFEFF", "").trim();
        return t;
    }

    public static boolean isValidPinFormat(String pin) {
        String n = normalize(pin);
        return n != null && n.matches("\\d{4}|\\d{6}");
    }

    public static String hashPin(String pin) {
        String n = normalize(pin);
        if (!isValidPinFormat(n)) throw new IllegalArgumentException("Invalid PIN format");
        try {
            byte[] salt = new byte[16];
            RNG.nextBytes(salt);
            PBEKeySpec spec = new PBEKeySpec(n.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOG.log(Level.SEVERE, "PIN hash failure", e);
            throw new RuntimeException("PIN processing error", e);
        } finally {
            // best-effort: caller controls String lifetime; avoid logging
        }
    }

    public static boolean verifyPin(String pin, String stored) {
        String n = normalize(pin);
        if (n == null || stored == null || stored.isEmpty()) return false;
        try {
            String[] parts = stored.split(":");
            if (parts.length != 2) return false;
            byte[] salt = Base64.getDecoder().decode(parts[0]);
            byte[] hash = Base64.getDecoder().decode(parts[1]);
            PBEKeySpec spec = new PBEKeySpec(n.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] test = skf.generateSecret(spec).getEncoded();
            if (hash.length != test.length) return false;
            int diff = 0;
            for (int i = 0; i < hash.length; i++) diff |= (hash[i] ^ test[i]);
            return diff == 0;
        } catch (Exception e) {
            LOG.log(Level.FINE, "PIN verify failure (non-sensitive)", e);
            return false;
        }
    }
}