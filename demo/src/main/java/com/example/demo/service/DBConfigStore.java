package com.example.demo.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import java.util.prefs.Preferences;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DBConfigStore {

    private static final Logger logger =
            Logger.getLogger(DBConfigStore.class.getName());

    private static final Preferences prefs =
            Preferences.userNodeForPackage(DBConfigStore.class);

    private static final String KEY_HOST = "db.host";
    private static final String KEY_PORT = "db.port";
    private static final String KEY_NAME = "db.name";
    private static final String KEY_USER = "db.user";
    private static final String KEY_PASS = "db.pass";

    /* =======================
       CIFRATURA PASSWORD (AES)
       ======================= */

    /**
     * Seed locale per derivare la chiave AES.
     * Non è la chiave finale → viene hashata.
     */
    private static final String SECRET_SEED = "DB_CONFIG_LOCAL_SECRET";

    /**
     * Genera sempre una chiave AES-128 valida
     */
    private static SecretKeySpec getKey() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(SECRET_SEED.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(Arrays.copyOf(key, 16), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Errore generazione chiave AES", e);
        }
    }

    private static String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, getKey());

            return Base64.getEncoder().encodeToString(
                    cipher.doFinal(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore cifratura password DB", e);
            return "";
        }
    }

    private static String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, getKey());

            return new String(
                    cipher.doFinal(Base64.getDecoder().decode(encrypted)),
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "Errore decifratura password DB: config invalidata", e);
            return "";
        }
    }

    /* =======================
       SALVATAGGIO / LETTURA
       ======================= */

    public static void save(String host,
                            String port,
                            String db,
                            String user,
                            String pass) {

        prefs.put(KEY_HOST, host);
        prefs.put(KEY_PORT, port);
        prefs.put(KEY_NAME, db);
        prefs.put(KEY_USER, user);

        if (pass != null && !pass.isBlank()) {
            prefs.put(KEY_PASS, encrypt(pass));
        }
    }

    public static String getHost() {
        return prefs.get(KEY_HOST, "");
    }

    public static String getPort() {
        return prefs.get(KEY_PORT, "");
    }

    public static String getDbName() {
        return prefs.get(KEY_NAME, "");
    }

    public static String getUser() {
        return prefs.get(KEY_USER, "");
    }

    /**
     * Non lancia MAI eccezioni.
     * In caso di errore → password considerata non configurata.
     */
    public static String getPassword() {
        String enc = prefs.get(KEY_PASS, "");
        if (enc.isBlank()) return "";
        return decrypt(enc);
    }

    /**
     * Metodo di utilità per reset totale (debug / recovery)
     */
    public static void clearAll() {
        try {
            prefs.clear();
            logger.info("Configurazione DB cancellata");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore reset configurazione DB", e);
        }
    }

    private DBConfigStore() {
        throw new AssertionError("Utility class");
    }
}
