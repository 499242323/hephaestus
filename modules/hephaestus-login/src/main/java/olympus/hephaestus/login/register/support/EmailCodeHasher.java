package olympus.hephaestus.login.register.support;

import olympus.hephaestus.login.register.domain.EmailVerificationScene;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

@Component
public class EmailCodeHasher {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public String hash(String email, EmailVerificationScene scene, String code) {
        String source = normalize(email) + ":" + scene + ":" + code;
        byte[] digest = sha256(source);
        return toHex(digest);
    }

    public String normalize(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private byte[] sha256(String source) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return messageDigest.digest(source.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    private String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            chars[i * 2] = HEX[value >>> 4];
            chars[i * 2 + 1] = HEX[value & 0x0f];
        }
        return new String(chars);
    }
}
