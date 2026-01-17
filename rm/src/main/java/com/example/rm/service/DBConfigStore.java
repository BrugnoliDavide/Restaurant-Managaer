package com.example.rm.service;

import com.example.rm.preference.DemoModeManager;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.prefs.Preferences;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Gestisce il salvataggio sicuro delle credenziali di connessione al database
 * utilizzando cifratura AES-GCM per la password.
 */
public final class DBConfigStore {

    private static final Logger logger = Logger.getLogger(DBConfigStore.class.getName());
    private static final Preferences prefs = Preferences.userNodeForPackage(DBConfigStore.class);

    private static final String KEY_HOST = "db.host";
    private static final String KEY_PORT = "db.port";
    private static final String KEY_NAME = "db.name";
    private static final String KEY_USER = "db.user";
    private static final String KEY_PASS = "db.pass";

    private static final String SECRET_SEED = "DB_CONFIG_LOCAL_SECRET";
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;

    private DBConfigStore() {
        throw new AssertionError("Utility class");
    }

    private static SecretKeySpec getKey() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] key = sha.digest(SECRET_SEED.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Errore generazione chiave AES", e);
        }
    }

    private static String encrypt(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), gcmSpec);

            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante la cifratura della password", e);
            return "";
        }
    }

    private static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return "";
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);

            if (decoded.length < GCM_IV_LENGTH + 1) {
                logger.log(Level.WARNING, "Dati cifrati corrotti: lunghezza insufficiente");
                return "";
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), gcmSpec);

            byte[] decrypted = cipher.doFinal(cipherText);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Errore durante la decifratura della password", e);
            return "";
        }
    }

    public static void save(String host, String port, String db, String user, String pass) {

        if (DemoModeManager.isDemoMode()) {
            logger.log(Level.INFO,"modalità DEMO attiva: i dati inseririti saranno eliminati alla chiusura del sistema");
        }
        else{

            prefs.put(KEY_HOST, host);
            prefs.put(KEY_PORT, port);
            prefs.put(KEY_NAME, db);
            prefs.put(KEY_USER, user);

            if (pass != null && !pass.isBlank()) {
                String encryptedPass = encrypt(pass);
                if (!encryptedPass.isEmpty()) {
                    prefs.put(KEY_PASS, encryptedPass);
                } else {
                    logger.log(Level.WARNING, "Impossibile cifrare la password: non verrà salvata");
                }
            }
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

    public static String getPassword() {
        String encrypted = prefs.get(KEY_PASS, "");
        if (encrypted.isBlank()) {
            return "";
        }

        String decrypted = decrypt(encrypted);

        if (decrypted.isEmpty() && !encrypted.isEmpty()) {
            logger.log(Level.WARNING,
                    "Password cifrata con vecchio algoritmo non sicuro. " +
                            "Sarà necessario reinserire le credenziali.");
            prefs.remove(KEY_PASS);
        }

        return decrypted;
    }

    public static void clearAll() {
        try {
            prefs.clear();
            logger.info("Configurazione DB cancellata con successo");
        } catch (Exception e) {
            logger.log(Level.WARNING, "Errore durante la cancellazione della configurazione DB", e);
        }
    }

    public static boolean hasConfiguration() {
        return !getHost().isEmpty()
                && !getPort().isEmpty()
                && !getDbName().isEmpty()
                && !getUser().isEmpty();
    }

    public static boolean hasValidPassword() {
        String encrypted = prefs.get(KEY_PASS, "");
        if (encrypted.isEmpty()) {
            return false;
        }

        String decrypted = decrypt(encrypted);
        return !decrypted.isEmpty();
    }
}