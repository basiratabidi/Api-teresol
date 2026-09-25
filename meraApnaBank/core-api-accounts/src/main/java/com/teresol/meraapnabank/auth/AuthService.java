package com.teresol.meraapnabank.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Registers users and issues/validates signed session tokens.
 * Passwords are stored as PBKDF2 hashes; users are kept in a small JSON file.
 */
@ApplicationScoped
public class AuthService {

    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();

    @ConfigProperty(name = "auth.token-ttl-seconds", defaultValue = "28800")
    long tokenTtlSeconds;

    @ConfigProperty(name = "auth.users-file", defaultValue = "users.json")
    String usersFile;

    @ConfigProperty(name = "auth.secret")
    java.util.Optional<String> configuredSecret;

    private final ObjectMapper mapper = new ObjectMapper();
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Map<String, String>> users = new ConcurrentHashMap<>();
    private byte[] secret;

    @jakarta.annotation.PostConstruct
    void init() {
        if (configuredSecret.isPresent() && !configuredSecret.get().isBlank()) {
            secret = configuredSecret.get().getBytes(StandardCharsets.UTF_8);
        } else {
            // no secret configured: tokens are valid until the service restarts
            secret = new byte[32];
            random.nextBytes(secret);
        }
        Path file = Path.of(usersFile);
        if (Files.exists(file)) {
            try {
                users.putAll(mapper.readValue(file.toFile(), new TypeReference<Map<String, Map<String, String>>>() {}));
            } catch (IOException e) {
                throw new IllegalStateException("Cannot read " + usersFile, e);
            }
        }
    }

    public synchronized Map<String, Object> signup(String fullName, String email, String password) {
        fullName = fullName == null ? "" : fullName.trim();
        email = normalize(email);
        if (fullName.length() < 2) throw bad(400, "Please enter your full name.");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) throw bad(400, "Please enter a valid email address.");
        if (password == null || password.length() < 8) throw bad(400, "Password must be at least 8 characters.");
        if (users.containsKey(email)) throw bad(409, "An account with this email already exists.");

        byte[] salt = new byte[16];
        random.nextBytes(salt);
        Map<String, String> user = new LinkedHashMap<>();
        user.put("fullName", fullName);
        user.put("email", email);
        user.put("salt", B64.encodeToString(salt));
        user.put("hash", B64.encodeToString(hash(password, salt)));
        users.put(email, user);
        persist();
        return session(user);
    }

    public Map<String, Object> login(String email, String password) {
        Map<String, String> user = users.get(normalize(email));
        // compare even when the user is unknown so timing doesn't reveal which emails exist
        byte[] salt = user != null ? B64D.decode(user.get("salt")) : new byte[16];
        byte[] expected = user != null ? B64D.decode(user.get("hash")) : new byte[32];
        byte[] actual = hash(password == null ? "" : password, salt);
        if (user == null || !MessageDigest.isEqual(expected, actual)) {
            throw bad(401, "Incorrect email or password.");
        }
        return session(user);
    }

    /** Returns the user's email if the token is valid and unexpired, otherwise null. */
    public String verify(String token) {
        try {
            int dot = token.indexOf('.');
            if (dot < 1) return null;
            String payload = token.substring(0, dot);
            if (!MessageDigest.isEqual(B64D.decode(token.substring(dot + 1)), sign(payload))) return null;
            String[] parts = new String(B64D.decode(payload), StandardCharsets.UTF_8).split("\\|", 2);
            if (parts.length != 2 || Long.parseLong(parts[1]) < System.currentTimeMillis() / 1000) return null;
            return users.containsKey(parts[0]) ? parts[0] : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    public Map<String, Object> profile(String email) {
        Map<String, String> user = users.get(email);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("fullName", user.get("fullName"));
        out.put("email", user.get("email"));
        return out;
    }

    private Map<String, Object> session(Map<String, String> user) {
        long exp = System.currentTimeMillis() / 1000 + tokenTtlSeconds;
        String payload = B64.encodeToString((user.get("email") + "|" + exp).getBytes(StandardCharsets.UTF_8));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", payload + "." + B64.encodeToString(sign(payload)));
        out.put("expiresAt", exp);
        out.put("user", profile(user.get("email")));
        return out;
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private byte[] hash(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private void persist() {
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(Path.of(usersFile).toFile(), users);
        } catch (IOException e) {
            throw new IllegalStateException("Cannot write " + usersFile, e);
        }
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private static WebApplicationException bad(int status, String message) {
        return new WebApplicationException(message, Response.status(status).build());
    }
}
